using System;
using System.Data.Odbc;
using System.Globalization;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Timers;
using System.Xml;
using NNClass;

namespace RTMLIS;

internal class DMLProtocol : Protocol
{
	private const int DML_MAXTOPICSSUPPORTED = 16;

	private const int DML_MAXDIRECTIVESSUPPORTED = 16;

	private NNBase m_NNBase = new NNBase();

	private string theConnection;

	private int m_loc_port;

	private byte[] m_inbuffer = new byte[32768];

	private byte[] m_outbuffer;

	private string m_message = "";

	private XmlDocument m_doc;

	private string m_device_id;

	private string m_serial_id;

	private string m_facility;

	private string m_location;

	private string m_inst_type = "";

	private string m_inst_name = "";

	private string m_inst_ver = "";

	private string m_control_type = "";

	private string m_svc_node = "";

	private string[] m_Topics = new string[16];

	private string[] m_Directives = new string[16];

	private int m_angleCount;

	private char m_chLast;

	private bool m_inLisSession;

	private string m_portType = "";

	private int m_imsgid = 4000;

	private int MaxMsgId = 4009;

	private int[] MsgId = new int[10];

	private int m_ack_control_id;

	private NetworkStream m_networkStream;

	private RTMLIS m_parent;

	private Port.AsynchNetworkServer.ClientHandler m_clienthandler;

	private int m_port_num;

	private System.Timers.Timer cmTimer;

	private string m_sample_key_num;

	private bool m_ProtocolSending;

	private bool m_inTimedEvent;

	private bool m_isProcessing;

	private DateTime m_last_eot_update_time = DateTime.Now;

	private bool m_bConnected;

	private string[] m_LastMessageSent = new string[10];

	private string m_ReadString = "";

	private string m_SelectSampleKeyNum = "";

	private Thread m_ProtocolThread;

	private bool bDBAvailable = true;

	private OdbcConnection myTimerConnection;

	private OdbcCommand myTimerCommand;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

	private OdbcConnection myDBReadConnection;

	private OdbcCommand myDBReadCommand;

	private bool m_more_samples;

	private OdbcDataReader myTimerReader;

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	public bool m_bSamplesDeviceNameColumn;

	private bool m_bInstrumentsTestsLisTestAliasColumn;

	public override bool IsAlive()
	{
		if (!m_isShutDown && !m_isShuttingDown && m_ProtocolThread != null)
		{
			return m_ProtocolThread.IsAlive;
		}
		return false;
	}

	public override bool ProcessNotify(int cd)
	{
		switch (cd)
		{
		case 1:
			if (!m_NNBase.m_isLogging)
			{
				m_NNBase.StartLogging();
			}
			break;
		case 2:
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.StopLogging();
			}
			break;
		case 3:
			Start();
			break;
		case -1:
			ShutDown("Notify", "Ports", bExit: false);
			break;
		}
		return false;
	}

	public DMLProtocol(ref NetworkStream networkStream, int port_num, string portType, bool logging, RTMLIS parent, Port.AsynchNetworkServer.ClientHandler clienthandler, int loc_port)
	{
		m_bConnected = true;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_parent = parent;
		m_bSamplesDeviceNameColumn = m_parent.m_bSamplesDeviceNameColumn;
		m_bInstrumentsTestsLisTestAliasColumn = m_parent.m_bInstrumentsTestsLisTestAliasColumn;
		m_clienthandler = clienthandler;
		m_NNBase.m_bLogging = logging;
		m_loc_port = loc_port;
		m_isShutDown = false;
		m_isShuttingDown = false;
		theConnection = "DSN=" + m_NNBase.DATASOURCE + ";UID=" + m_NNBase.UAUTHORITY + ";PWD=" + m_NNBase.PAUTHORITY;
	}

	private void Start()
	{
		m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, "DML", "RTMLIS", "LIS");
		bDBAvailable = OpenDBConnection(ref myTimerConnection, ref myTimerCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = OpenDBConnection(ref myDBWriteConnection, ref myDBWriteCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = OpenDBConnection(ref myDBReadConnection, ref myDBReadCommand, 7);
		if (bDBAvailable)
		{
			m_NNBase.CommAudit(10, "Connect", "");
			string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
			myDBWriteCommand.CommandText = sCommand;
			if (m_NNBase.m_isLogging)
			{
				log(myDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myDBWriteCommand.ExecuteNonQuery();
			if (m_NNBase.m_isLogging)
			{
				log("Connection established via local port " + m_loc_port, isXml: false, "RTMLIS");
			}
			m_SelectSampleKeyNum = "select top 1 sample_key_num from DBA.samples where transmitted_flag = 'F' and xml_text like '%<SVC>%</SVC>%' and control_type is not null and control_type != '' order by sample_Date";
			if (m_NNBase.m_isLogging)
			{
				log(m_SelectSampleKeyNum, isXml: false, "SQL");
			}
			m_ProtocolThread = new Thread(ProtocolThread);
			m_ProtocolThread.Start();
			cmTimer = new System.Timers.Timer();
			cmTimer.AutoReset = true;
			cmTimer.Elapsed += OnCmTimedEvent;
			cmTimer.Interval = 100.0;
			cmTimer.Enabled = true;
		}
	}

	private bool OpenDBConnection(ref OdbcConnection myConnection, ref OdbcCommand myCommand, int iTries)
	{
		bDBAvailable = m_NNBase.OpenDBConnection(ref myConnection, ref myCommand, iTries);
		if (!bDBAvailable)
		{
			ShutDown("Cannot connect to database", "Protocol", bExit: true);
		}
		return bDBAvailable;
	}

	private void OnCmTimedEvent(object source, ElapsedEventArgs ev)
	{
		bool bgetout = false;
		lock (this)
		{
			bgetout = m_isShutDown | m_isShuttingDown | m_ProtocolSending | m_isProcessing | m_inTimedEvent;
			if (!bgetout)
			{
				m_inTimedEvent = true;
				cmTimer.Stop();
			}
		}
		if (bgetout)
		{
			return;
		}
		if (m_bConnected && !m_inLisSession)
		{
			try
			{
				myTimerCommand.CommandText = m_SelectSampleKeyNum;
				myTimerReader = myTimerCommand.ExecuteReader();
				m_sample_key_num = "";
				if (m_more_samples = myTimerReader.Read())
				{
					m_sample_key_num = myTimerReader.GetString(0);
				}
				myTimerReader.Close();
				if (m_sample_key_num.Length > 0)
				{
					Console.WriteLine("Next sample is {0}", m_sample_key_num);
					if (ProcessSample())
					{
						m_LastMessageSent[m_imsgid - 4000] = "Hello";
						if (!SendHello())
						{
							m_NNBase.ReportErrorDB("Unable to transmit sample " + m_sample_key_num, "C", "sending Hello", "OnCmTimedEvent", "");
							ShutDown("Unable to transmit hello", "Timer", bExit: true);
						}
						else
						{
							cmTimer.Interval = 200.0;
						}
					}
					else
					{
						MarkSampleAsTransmitted("Timer");
						cmTimer.Interval = 100.0;
						cmTimer.Start();
					}
				}
				else
				{
					Console.WriteLine("No samples to send");
					cmTimer.Interval = 500.0;
					cmTimer.Start();
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "Scanning for untransmitted results", "OnCmTimedEvent");
			}
			catch (Exception e2)
			{
				handleException(e2, "Scanning for untransmitted results", "OnCmTimedEvent");
			}
		}
		lock (this)
		{
			m_inTimedEvent = false;
		}
	}

	private bool ProcessSample()
	{
		bool ret = false;
		bool readok = false;
		string xml_text = "";
		string svc_plus = "";
		string obs_plus = "";
		int pSVC = -1;
		int pEndSVC = -1;
		int pOBS = -1;
		int pEndOBS = -1;
		m_inst_name = "";
		m_inst_type = "";
		m_device_id = "";
		m_serial_id = "";
		m_location = "Unassigned";
		m_facility = "Unassigned";
		m_control_type = "";
		m_svc_node = "";
		if (!m_bSamplesDeviceNameColumn)
		{
			try
			{
				myDBReadCommand.CommandText = $"select inst_name, inst_type, inst_id, serial_no, il.loc_name, ip.loc_name from DBA.instruments i left outer join DBA.inst_locations il on i.loc_num = il.loc_num left outer join DBA.inst_locations ip on ip.loc_num = il.parent where inst_id in (select device_serial from DBA.samples where sample_key_num = '{m_sample_key_num}')";
				if (m_NNBase.m_isLogging)
				{
					log(myDBReadCommand.CommandText, isXml: false, "SQL");
				}
				OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
				readok = myReader.Read();
				if (readok)
				{
					m_inst_name = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					m_inst_type = (myReader.IsDBNull(1) ? "" : myReader.GetString(1));
					m_device_id = (myReader.IsDBNull(2) ? "" : myReader.GetString(2));
					m_serial_id = (myReader.IsDBNull(3) ? "" : myReader.GetString(3));
					m_location = (myReader.IsDBNull(4) ? "Unassigned" : myReader.GetString(4));
					m_facility = (myReader.IsDBNull(5) ? "Unassigned" : myReader.GetString(5));
				}
				myReader.Close();
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "reading instrument", "ProcessSample");
			}
			catch (Exception e2)
			{
				handleException(e2, "reading instrument", "ProcessSample");
			}
			if (!readok)
			{
				m_NNBase.ReportErrorDB("Unable to obtain instrument information for sample " + m_sample_key_num, "E", "looking up instrument record", "ProcessSample", "");
			}
			else
			{
				ret = true;
			}
		}
		else
		{
			ret = true;
		}
		if (ret)
		{
			ret = false;
			try
			{
				myDBReadCommand.CommandText = "select control_type, xml_text";
				if (m_bSamplesDeviceNameColumn)
				{
					myDBReadCommand.CommandText += ", loc_name, fac_name, device_name, device_type, device_serial, device_sw_ver";
				}
				myDBReadCommand.CommandText += $" from DBA.samples  where sample_key_num = '{m_sample_key_num}'";
				if (m_NNBase.m_isLogging)
				{
					log(myDBReadCommand.CommandText, isXml: false, "SQL");
				}
				OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
				readok = myReader.Read();
				if (readok)
				{
					m_control_type = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					xml_text = (myReader.IsDBNull(1) ? "" : myReader.GetString(1));
					if (m_bSamplesDeviceNameColumn)
					{
						m_location = (myReader.IsDBNull(2) ? m_location : myReader.GetString(2));
						m_facility = (myReader.IsDBNull(3) ? m_facility : myReader.GetString(3));
						m_inst_name = (myReader.IsDBNull(4) ? "" : myReader.GetString(4));
						m_inst_type = (myReader.IsDBNull(5) ? "" : myReader.GetString(5));
						m_serial_id = (m_device_id = (myReader.IsDBNull(6) ? "" : myReader.GetString(6)));
						m_inst_ver = (myReader.IsDBNull(7) ? "" : myReader.GetString(7));
					}
				}
				myReader.Close();
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e3)
			{
				handleDBException(e3, "reading sample", "ProcessSample");
			}
			catch (Exception e4)
			{
				handleException(e4, "reading sample", "ProcessSample");
			}
			if (!readok)
			{
				m_NNBase.ReportErrorDB("Sample not found", "E", "selecting sample", "ProcessSample", "");
				ShutDown("Sample not found", "Protocol", bExit: true);
			}
			else if (m_control_type.Length > 0 && xml_text.Length > 0)
			{
				ret = true;
			}
			else
			{
				if (m_control_type.Length == 0)
				{
					m_NNBase.ReportErrorDB("Null or empty control type", "E", "reading sample", "ProcessSample", "");
				}
				if (xml_text.Length == 0)
				{
					m_NNBase.ReportErrorDB("Null or empty xml_text", "E", "reading sample", "ProcessSample", "");
				}
			}
		}
		if (ret)
		{
			ret = false;
			try
			{
				pSVC = xml_text.IndexOf("<SVC>");
				if (pSVC < 0)
				{
					m_NNBase.ReportErrorDB("<SVC> not found", "E", "parsing xml_text", "ProcessSample", "");
				}
				else
				{
					svc_plus = xml_text.Substring(pSVC);
					pEndSVC = svc_plus.IndexOf("</SVC>");
					if (pEndSVC < 0)
					{
						m_NNBase.ReportErrorDB("</SVC> not found", "E", "parsing xml_text", "ProcessSample", "");
					}
					else
					{
						m_svc_node = svc_plus.Substring(0, pEndSVC + 6);
						pOBS = m_svc_node.IndexOf("<OBS>");
						if (pOBS < 0)
						{
							m_NNBase.ReportErrorDB("<OBS> not found", "E", "parsing xml_text", "ProcessSample", "");
						}
						else
						{
							obs_plus = m_svc_node.Substring(pOBS);
							pEndOBS = obs_plus.IndexOf("</OBS>");
							if (pEndOBS < 0)
							{
								m_NNBase.ReportErrorDB("</OBS> not found", "E", "parsing xml_text", "ProcessSample", "");
							}
							else
							{
								ret = true;
							}
						}
					}
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (Exception e5)
			{
				if (!m_stopping)
				{
					handleException(e5, "parsing xml_text", "ProcessSample");
				}
			}
		}
		return ret;
	}

	private void OnReadComplete()
	{
		string reason = "ReadError";
		bool bgetout = false;
		int MessageCount = 0;
		try
		{
			while (!m_isShutDown && !m_isShuttingDown && MessageCount == 0)
			{
				lock (this)
				{
					m_isProcessing = false;
				}
				int bytesRead = m_networkStream.Read(m_inbuffer, 0, m_inbuffer.Length);
				if (bytesRead > 0)
				{
					lock (this)
					{
						if (!(bgetout = m_isShutDown | m_isShuttingDown))
						{
							m_isProcessing = true;
						}
					}
					if (bgetout)
					{
						break;
					}
					string sRead = Encoding.UTF8.GetString(m_inbuffer, 0, bytesRead);
					if (m_NNBase.m_isLogging)
					{
						log(sRead, isXml: false, m_portType);
					}
					m_ReadString += sRead;
					int iLast = 0;
					m_message = "";
					m_angleCount = 0;
					m_chLast = '\0';
					for (int i = iLast; i < m_ReadString.Length; i++)
					{
						switch (m_ReadString[i])
						{
						case '<':
							m_angleCount++;
							break;
						case '/':
							if (m_chLast == '<')
							{
								m_angleCount -= 2;
							}
							break;
						case '>':
							if (m_chLast == '?')
							{
								m_angleCount--;
								break;
							}
							if (m_chLast == '-')
							{
								m_angleCount--;
								break;
							}
							if (m_chLast == '/')
							{
								m_angleCount--;
							}
							if (m_angleCount == 0)
							{
								m_message = m_ReadString.Substring(iLast, i - iLast + 1);
								MessageCount++;
								Console.WriteLine("{0}Rcvd {1} bytes from {3}:\t{2}", "", bytesRead, m_message, m_portType);
								iLast = i + 1;
								m_angleCount = 0;
								m_chLast = '\0';
								ProcessMessage();
							}
							break;
						}
						if (m_ReadString[i] >= ' ')
						{
							m_chLast = m_ReadString[i];
						}
					}
					if (iLast < m_ReadString.Length)
					{
						m_ReadString = m_ReadString.Substring(iLast);
					}
					else
					{
						m_ReadString = "";
					}
				}
				else
				{
					reason = "Connection dropped";
					ShutDown(reason, "Protocol", bExit: true);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (IOException)
		{
			reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Reading message(s)", "OnReadComplete");
			}
		}
	}

	private void ProcessMessage()
	{
		try
		{
			m_doc = new XmlDocument();
			m_doc.LoadXml(m_message);
			m_message = "";
			XmlNodeReader reader = new XmlNodeReader(m_doc);
			reader.Read();
			switch (reader.LocalName)
			{
			case "ACK.R01":
				ProcessAck(reader);
				break;
			case "END.R01":
				ProcessEnd(reader);
				break;
			case "ESC.R01":
				ShutDown("ESC.R01", "Protocol", bExit: true);
				break;
			case "REQ.R01":
				ProcessRequestOBS();
				break;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (XmlException e)
		{
			handleXMLException(e, "processing message", "ProcessMessage");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing message", "ProcessMessage");
		}
	}

	private void ProcessAck(XmlNodeReader reader)
	{
		string m_type_cd = "";
		while (reader.Read())
		{
			reader.MoveToContent();
			XmlNodeType nodeType = reader.NodeType;
			if (nodeType != XmlNodeType.Element || !reader.IsStartElement())
			{
				continue;
			}
			switch (reader.LocalName)
			{
			case "ACK.type_cd":
				m_type_cd = reader.GetAttribute("V");
				break;
			case "ACK.ack_control_id":
			{
				string ack_control_id = reader.GetAttribute("V");
				if (ack_control_id.Length > 0)
				{
					m_ack_control_id = Convert.ToInt32(ack_control_id);
				}
				else
				{
					m_ack_control_id = 0;
				}
				break;
			}
			}
		}
		if (m_type_cd == "AA")
		{
			switch (m_LastMessageSent[m_ack_control_id - 4000])
			{
			case "Hello":
				m_LastMessageSent[m_imsgid - 4000] = "Status";
				SendStatus();
				break;
			case "Result":
				MarkSampleAsTransmitted("Protocol");
				m_last_eot_update_time = DateTime.Now;
				m_LastMessageSent[m_imsgid - 4000] = "EOT";
				SendEotMessage("OBS");
				lock (this)
				{
					m_inLisSession = false;
					cmTimer.Start();
					break;
				}
			case "Status":
				break;
			case "EOT":
				break;
			}
		}
		else if (m_ack_control_id >= 4000)
		{
			ShutDown("ACK.type_cd V=\"" + m_type_cd + "\"", "Protocol", bExit: true);
		}
	}

	private void ProcessRequestOBS()
	{
		if (m_sample_key_num.Length > 0)
		{
			m_LastMessageSent[m_imsgid - 4000] = "Result";
			SendResult();
			return;
		}
		m_last_eot_update_time = DateTime.Now;
		m_LastMessageSent[m_imsgid - 4000] = "EOT";
		SendEotMessage("OBS");
		lock (this)
		{
			m_inLisSession = false;
			cmTimer.Start();
		}
	}

	private void ProcessEnd(XmlNodeReader reader)
	{
		string m_control_id = "";
		string m_reason_cd = "";
		while (reader.Read())
		{
			reader.MoveToContent();
			XmlNodeType nodeType = reader.NodeType;
			if (nodeType == XmlNodeType.Element && reader.IsStartElement())
			{
				switch (reader.LocalName)
				{
				case "HDR.control_id":
					m_control_id = reader.GetAttribute("V");
					break;
				case "TRM.reason_cd":
					m_reason_cd = reader.GetAttribute("V");
					break;
				}
			}
		}
		SendAcknowledgeMessage(m_control_id, isError: false);
		if (m_reason_cd == "NRM")
		{
			if (m_inLisSession)
			{
				ShutDown("END.R01", "Protocol", bExit: true);
			}
		}
		else
		{
			ShutDown("END.R01", "Protocol", bExit: true);
		}
	}

	private bool SendAcknowledgeMessage(string control_id, bool isError)
	{
		bool retVal = true;
		try
		{
			string sAck = "<ACK.R01>" + GenDMLHeader("Protocol") + "<ACK><ACK.type_cd V=\"A" + (isError ? "E" : "A") + "\"/><ACK.ack_control_id V=\"" + control_id + "\"/></ACK></ACK.R01>";
			SendString(sAck, "Protocol");
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (IOException)
		{
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "sending acknowledge message", "SendAcknowledgeMessage");
			}
		}
		return retVal;
	}

	private bool SendEotMessage(string topic)
	{
		bool retVal = true;
		try
		{
			string sEOT = "<EOT.R01>" + GenDMLHeader("Timer") + "<EOT><EOT.topic_cd V=\"" + topic + "\"/><EOT.update_dttm V=\"" + DateTime2DML(m_last_eot_update_time, "Timer") + "\"/></EOT></EOT.R01>";
			SendString(sEOT, "Timer");
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (IOException)
		{
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "sending EOT message", "SendEOTMessage");
			}
		}
		return retVal;
	}

	private string GenDMLHeader(string whoFrom)
	{
		string sHeader = "";
		try
		{
			sHeader = "<HDR><HDR.control_id V=\"" + m_imsgid + "\"/><HDR.version_id V=\"POCT1\"/><HDR.creation_dttm V=\"" + DateTime2DML(DateTime.Now, whoFrom) + "\"/></HDR>";
			m_imsgid++;
			if (m_imsgid > MaxMsgId)
			{
				m_imsgid = 4000;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (Exception e)
		{
			handleException(e, "building DML Header", "GenDMLHeader");
		}
		return sHeader;
	}

	private bool SendHello()
	{
		bool ret = false;
		try
		{
			lock (this)
			{
				m_inLisSession = true;
			}
			string helloMessage = string.Format("<HEL.R01>{0}<DEV><DEV.vendor_id V=\"{1}^{2}^{3}\"/><DEV.device_id V=\"{4}\">{5}^{6}</DEV.device_id><DEV.model_id V=\"{2}\"></DEV.model_id><DEV.serial_id V=\"{4}\"></DEV.serial_id><DSC><DSC.connection_profile_cd V=\"SA\"/></DSC></DEV></HEL.R01>", GenDMLHeader("Timer"), FixXMLString(m_inst_name), m_inst_type, m_device_id, m_serial_id, m_facility, m_location);
			SendString(helloMessage, "Timer");
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "sending hello", "SendHello");
		}
		catch (Exception e2)
		{
			handleException(e2, "sending hello", "SendHello");
		}
		return ret;
	}

	public string FixXMLString(string inXML)
	{
		string FixedXML = "";
		if (Comp.IndexOf(inXML, "<") >= 0 || Comp.IndexOf(inXML, ">") >= 0 || Comp.IndexOf(inXML, "\"") >= 0 || Comp.IndexOf(inXML, "&") >= 0 || Comp.IndexOf(inXML, "'") >= 0)
		{
			int ilen = inXML.Length;
			for (int i = 0; i < ilen; i++)
			{
				FixedXML = ((!(inXML.Substring(i, 1) == "<")) ? ((!(inXML.Substring(i, 1) == ">")) ? ((!(inXML.Substring(i, 1) == "\"")) ? ((!(inXML.Substring(i, 1) == "&")) ? ((!(inXML.Substring(i, 1) == "'")) ? (FixedXML + inXML.Substring(i, 1)) : (FixedXML + "&apos;")) : (FixedXML + "&amp;")) : (FixedXML + "&quot;")) : (FixedXML + "&gt;")) : (FixedXML + "&lt;"));
			}
		}
		else
		{
			FixedXML = inXML;
		}
		return FixedXML;
	}

	private bool SendStatus()
	{
		bool ret = false;
		string sNumSamples = ((m_sample_key_num.Length > 0) ? "1" : "0");
		try
		{
			string statusMessage = string.Format("<DST.R01>{0}<DST><DST.status_dttm V=\"{1}\"/><DST.new_observations_qty V=\"" + sNumSamples + "\"/><DST.condition_cd V=\"R\"/><DST.patients_update_dttm V=\"{1}\"/><DST.operators_update_dttm V=\"{1}\"/></DST></DST.R01>", GenDMLHeader("Timer"), DateTime2DML(DateTime.Now, "Timer"));
			SendString(statusMessage, "Timer");
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "sending status", "SendStatus");
		}
		catch (Exception e2)
		{
			handleException(e2, "sending status", "SendStatus");
		}
		return ret;
	}

	private void MarkSampleAsTransmitted(string whoFrom)
	{
		try
		{
			myDBWriteCommand.CommandText = $"UPDATE DBA.samples set transmitted_flag = 'T' where sample_key_num = '{m_sample_key_num}'";
			if (m_NNBase.m_isLogging)
			{
				log(myDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myDBWriteCommand.ExecuteNonQuery();
			m_sample_key_num = "";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (OdbcException e)
		{
			handleDBException(e, "marking sample as transmitted", "MarkSampleAsTransmitted");
		}
		catch (Exception e2)
		{
			handleException(e2, "marking sample as transmitted", "MarkSampleAsTransmitted");
		}
	}

	private bool SendResult()
	{
		bool ret = false;
		try
		{
			string resultMessage = string.Format("<OBS.R0{0}>{1}{2}</OBS.R0{0}>", (m_control_type != "OBS") ? "2" : "1", GenDMLHeader("Timer"), m_svc_node);
			SendString(resultMessage, "Timer");
			m_parent.m_iNumMessages++;
			m_parent.m_iTotMessages++;
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "Sending result message", "SendResult");
		}
		return ret;
	}

	private string DateTime2DML(DateTime dt, string whoFrom)
	{
		string ret = "";
		try
		{
			if (DateTime.Now >= DateTime.UtcNow)
			{
				TimeSpan tzDif = DateTime.Now - DateTime.UtcNow;
				ret = dt.ToString("s") + "+" + ((tzDif.Hours < 10) ? "0" : "") + tzDif.Hours.ToString("d") + ":" + ((tzDif.Minutes < 10) ? "0" : "") + tzDif.Minutes.ToString("d");
			}
			else
			{
				TimeSpan tzDif2 = DateTime.UtcNow - DateTime.Now;
				ret = dt.ToString("s") + "-" + ((tzDif2.Hours < 10) ? "0" : "") + tzDif2.Hours.ToString("d") + ":" + ((tzDif2.Minutes < 10) ? "0" : "") + tzDif2.Minutes.ToString("d");
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (Exception e)
		{
			handleException(e, "converting datetime to DML", "DateTime2DML");
		}
		return ret;
	}

	private void log(string s, bool isXml, string whoFrom)
	{
		m_NNBase.log(s, isXml, whoFrom);
	}

	private void ShutDown(string reason, string whoFrom, bool bExit)
	{
		if (m_isShutDown || m_isShuttingDown)
		{
			return;
		}
		m_isShuttingDown = true;
		try
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Shutting down because " + reason, isXml: false, whoFrom);
			}
		}
		catch
		{
		}
		try
		{
			if (m_clienthandler != null)
			{
				string sport = m_clienthandler.socket.Handle.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing client port " + sport + " for LIS interface", isXml: false, "dml");
				}
				m_clienthandler.socket.Close();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport + " for LIS interface", isXml: false, "dml");
				}
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing networkStream", isXml: false, "dml");
			}
			m_networkStream.Close();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed networkStream", isXml: false, "dml");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing networkStream", isXml: false, "dml");
			}
			m_networkStream.Dispose();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposed networkStream", isXml: false, "dml");
			}
			if (m_clienthandler != null)
			{
				m_clienthandler.RemoveFromList("dml.ShutDown");
			}
		}
		catch
		{
		}
		try
		{
			m_bConnected = false;
			if (whoFrom != "Timer")
			{
				cmTimer.Close();
			}
			if (m_ProtocolThread != null && m_ProtocolThread.IsAlive && Thread.CurrentThread.ManagedThreadId != m_ProtocolThread.ManagedThreadId)
			{
				m_ProtocolThread.Abort();
			}
			Thread.Sleep(1000);
			if (bDBAvailable)
			{
				m_NNBase.CommAudit(11, "Disconnect", reason);
				string sCommand = "update DBA.health_ping set update_time = now(*), last_disconnect_dttm = now(*) where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
				myDBWriteCommand.CommandText = sCommand;
				if (m_NNBase.m_isLogging)
				{
					log(myDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myDBWriteCommand.ExecuteNonQuery();
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.StopLogging();
			}
		}
		catch
		{
		}
		finally
		{
			Console.WriteLine("Shutdown " + m_port_num);
			m_isShutDown = true;
			if (bExit)
			{
				LibWrap.ExitThread(0u);
			}
		}
	}

	private void handleThreadAbortException(string whoFrom)
	{
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Thread aborted " + (m_stopping ? "- was asked to stop" : ""), isXml: false, whoFrom);
		}
		if (!m_isShutDown && !m_isShuttingDown)
		{
			ShutDown("Thread aborted", whoFrom, bExit: true);
		}
		else
		{
			LibWrap.ExitThread(0u);
		}
	}

	private void handleException(Exception e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString();
			bool bDBDisconnect = m_NNBase.ExceptionIsDisconnect(e);
			if (bDBDisconnect)
			{
				bDBAvailable = false;
			}
			else
			{
				details = details + " " + e.StackTrace.ToString();
				m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			}
			m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : ("Exception " + e.GetType().ToString()), bDBDisconnect ? "E" : "C", when, from, details);
			ShutDown(bDBDisconnect ? "The database connection has been lost" : "Exception", "Protocol", bExit: true);
		}
	}

	private void handleXMLException(XmlException e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString() + " at line: " + e.LineNumber + " " + e.StackTrace.ToString();
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("XML Exception " + e.GetType().ToString(), "C", when, from, details);
			ShutDown("XML Exception", "Protocol", bExit: true);
		}
	}

	private void handleDBException(OdbcException e, string when, string from)
	{
		if (m_isShuttingDown)
		{
			return;
		}
		string details = "";
		string emessage = e.Message.ToString();
		bool bDBDisconnect = m_NNBase.DBExceptionIsDisconnect(e);
		if (bDBDisconnect)
		{
			details = emessage;
			bDBAvailable = false;
		}
		else
		{
			for (int i = 0; i < e.Errors.Count; i++)
			{
				details = details + e.Errors[i].Message + " ";
			}
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
		}
		m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : "DB Exception", bDBDisconnect ? "E" : "C", when, from, details);
		ShutDown(bDBDisconnect ? "The database connection has been lost" : "DB Exception", "Protocol", bExit: true);
	}

	public int SendString(string input, string whoFrom)
	{
		lock (this)
		{
			m_ProtocolSending = true;
		}
		int i = 0;
		try
		{
			m_outbuffer = Encoding.UTF8.GetBytes(input);
			i = m_outbuffer.Length;
			Console.WriteLine("Sent {0} bytes to {2}{3}:\t{1}", i, input, m_portType, "   ");
			if (m_NNBase.m_isLogging)
			{
				log(input, isXml: true, "RTMLIS   ");
			}
			m_networkStream.Write(m_outbuffer, 0, i);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (IOException)
		{
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown Requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "sending string", "SendString");
			}
		}
		lock (this)
		{
			m_ProtocolSending = false;
			return i;
		}
	}

	private void ProtocolThread()
	{
		while (!m_isShutDown && !m_isShuttingDown)
		{
			OnReadComplete();
		}
	}
}
