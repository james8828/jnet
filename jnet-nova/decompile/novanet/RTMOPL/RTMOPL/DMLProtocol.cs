using System;
using System.Collections;
using System.Collections.Generic;
using System.Configuration;
using System.Data.Odbc;
using System.Globalization;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Timers;
using System.Xml;
using InstLocations;
using LocLastUpdate;
using NNClass;

namespace RTMOPL;

public class DMLProtocol : Protocol
{
	private const int DML_MAXTOPICSSUPPORTED = 16;

	private const int DML_MAXDIRECTIVESSUPPORTED = 16;

	public NNBase m_NNBase = new NNBase();

	private int m_loc_port;

	private byte[] m_readbuffer;

	private List<byte[]> m_readBufferList = new List<byte[]>();

	private byte[] m_outbuffer;

	private byte[] m_asyncwritebuffer;

	private string m_message = "";

	private string m_OplistMsg = "";

	private XmlDocument m_doc;

	private string m_facility = "";

	private string m_location = "";

	private string m_loc_num = "";

	private string m_insttype = "";

	private string m_inst_class = "";

	private string m_method = "";

	private string m_OperatorID = "";

	private string m_OperatorLastName = "";

	private string m_OperatorFirstName = "";

	private string m_active_date = "";

	private string m_expiration_date = "";

	private string m_permission_level = "";

	private string m_password = "";

	private ArrayList m_method_list = new ArrayList();

	private ArrayList m_unused_method_list = new ArrayList();

	private ArrayList m_instrument_list = new ArrayList();

	private ArrayList m_location_list = new ArrayList();

	private string m_action_cd = "";

	private string m_operator_num = "";

	private string m_portType = "";

	private NetworkStream m_networkStream;

	private AsyncCallback callbackWrite;

	private RTMOPL m_parent;

	private Port.AsynchNetworkServer.ClientHandler m_clienthandler;

	private int m_port_num;

	private string m_ReadString = "";

	private string m_control_id = "";

	private int m_imsgid = 4000;

	private int MaxMsgId = 9999;

	private DateTime m_last_oper_upd_time;

	private DateTime m_last_operpriv_upd_time;

	private DateTime m_last_operunit_upd_time;

	private DateTime m_last_operall_upd_time;

	private string s_msg_type_rcvd;

	private string m_LastMessageSent = "";

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	private CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private OperatorRec m_Operator;

	private DBOperator m_DBOperator;

	private OperatorPrivilegeRec m_OperatorPrivilege;

	private OperatorToUnitRec m_OperatorToUnit;

	private DBOperatorPrivilege m_DBOperatorPrivilege;

	private DBOperatorToUnit m_DBOperatorToUnit;

	private MethodRec m_Method;

	private DBMethod m_DBMethod;

	private LocLastUpdateRec m_LocLastUpdate;

	private DBLocLastUpdate m_DBLocLastUpdate;

	private InstLocationRec m_InstLocation;

	private DBInstLocation m_DBInstLocation;

	private DateTime m_last_eot_update_time = DateTime.Now;

	private bool m_bCertByTest = true;

	private Thread m_ProtocolThread;

	private bool bDBAvailable = true;

	private OdbcConnection myDBReadConnection;

	private OdbcCommand myDBReadCommand;

	private OdbcDataReader myDBReadReader;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

	private System.Timers.Timer cmTimer;

	private int m_kpaTimeoutCount;

	private bool m_waiting;

	private bool m_TimerSending;

	private bool m_ProtocolSending;

	private bool m_inTimedEvent;

	private bool m_isProcessing;

	private bool m_UseFacilityUUID;

	protected FacilityList m_FacilityList;

	protected LocationList m_LocationList;

	private bool m_b_loc_last_update_inst_class_column;

	private bool m_b_loc_last_update_inst_type_column;

	private ArrayList instTypeList = new ArrayList();

	private List<string> locLastUpdateList = new List<string>();

	private Dictionary<string, string> ValidTestWithInstType = new Dictionary<string, string>();

	private Dictionary<string, string> comLocNumList = new Dictionary<string, string>();

	public override bool IsAlive()
	{
		if (!m_isShutDown && !m_isShuttingDown && m_ProtocolThread != null)
		{
			return m_ProtocolThread.IsAlive;
		}
		return false;
	}

	private void GetCompleteLocNumList()
	{
		OdbcCommand command1 = myDBReadConnection.CreateCommand();
		try
		{
			string sqlLocNum = " SELECT l.loc_num, l.loc_name as loc, f.loc_name as fac FROM DBA.inst_locations l join dba.inst_locations f on(l.parent = f.loc_num)";
			command1.CommandText = sqlLocNum;
			myDBReadReader = command1.ExecuteReader();
			_ = string.Empty;
			_ = string.Empty;
			while (myDBReadReader.Read())
			{
				string locNum = myDBReadReader.GetString(0);
				string loc = myDBReadReader.GetString(1);
				string fac = myDBReadReader.GetString(2);
				string key = fac.ToLower() + "^" + loc.ToLower();
				comLocNumList.Add(key, locNum);
			}
			myDBReadReader.Close();
		}
		catch
		{
		}
		finally
		{
			CleanCommand(command1);
		}
	}

	private void AddCompleteFacNumToLocList()
	{
		OdbcCommand command1 = myDBReadConnection.CreateCommand();
		try
		{
			string sqlLocNum = " SELECT loc_name, loc_num FROM DBA.inst_locations where level_num =1";
			command1.CommandText = sqlLocNum;
			myDBReadReader = command1.ExecuteReader();
			_ = string.Empty;
			_ = string.Empty;
			while (myDBReadReader.Read())
			{
				string facName = myDBReadReader.GetString(0);
				string facNum = myDBReadReader.GetString(1);
				comLocNumList.Add(facName, facNum);
			}
			myDBReadReader.Close();
		}
		catch
		{
		}
		finally
		{
			CleanCommand(command1);
		}
	}

	private void MakeInstrumentTypeList()
	{
		myDBReadCommand.CommandText = "select inst_type from dba.instrument_types it JOIN dba.ui_instruments uii ON(it.inst_type = uii.name)";
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
		}
		myDBReadReader = myDBReadCommand.ExecuteReader();
		while (myDBReadReader.Read())
		{
			if (!myDBReadReader.IsDBNull(0))
			{
				instTypeList.Add(myDBReadReader.GetString(0).ToLower());
			}
		}
		myDBReadReader.Close();
	}

	private void FillTestWithInstTypeList()
	{
		OdbcCommand command1 = myDBReadConnection.CreateCommand();
		try
		{
			string sqlLocNum = "SELECT distinct test_name, inst_type FROM DBA.instruments_tests it JOIN dba.ui_instruments uii  ON(it.inst_type = uii.name) order by test_name";
			command1.CommandText = sqlLocNum;
			myDBReadReader = command1.ExecuteReader();
			string cmpTestName = string.Empty;
			string compelteInstType = string.Empty;
			while (myDBReadReader.Read())
			{
				string testName = myDBReadReader.GetString(0);
				string instType = myDBReadReader.GetString(1);
				if (cmpTestName.Length < 1 && compelteInstType.Length < 1)
				{
					cmpTestName = testName;
					compelteInstType = instType;
				}
				else if (testName.CompareTo(cmpTestName) == 0)
				{
					compelteInstType = compelteInstType + "," + instType;
				}
				else
				{
					ValidTestWithInstType.Add(cmpTestName.ToLower(), compelteInstType);
					cmpTestName = testName;
					compelteInstType = instType;
				}
			}
			ValidTestWithInstType.Add(cmpTestName.ToLower(), compelteInstType);
			myDBReadReader.Close();
		}
		catch
		{
		}
		finally
		{
			CleanCommand(command1);
		}
	}

	private void AddMTEToStatStripDevice()
	{
		OdbcCommand command1 = myDBReadConnection.CreateCommand();
		try
		{
			string sqlLocNum = "SELECT it.inst_type FROM DBA.INSTRUMENT_TYPES it  JOIN dba.ui_instruments uii ON (it.inst_type = uii.name)  where it.inst_class = 'statstrip'";
			command1.CommandText = sqlLocNum;
			myDBReadReader = command1.ExecuteReader();
			_ = string.Empty;
			string compelteInstType = string.Empty;
			while (myDBReadReader.Read())
			{
				compelteInstType = ((compelteInstType.Length >= 1) ? (compelteInstType + "," + myDBReadReader.GetString(0)) : myDBReadReader.GetString(0));
			}
			ValidTestWithInstType.Add("mte", compelteInstType);
			myDBReadReader.Close();
		}
		catch
		{
		}
		finally
		{
			CleanCommand(command1);
		}
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
		case -1:
			ShutDown("Notify", "Ports", bExit: false);
			break;
		}
		return false;
	}

	public DMLProtocol(ref NetworkStream networkStream, int port_num, string portType, bool logging, RTMOPL parent, Port.AsynchNetworkServer.ClientHandler clienthandler, int loc_port)
	{
		RTMOPL.m_OPLBytesBuffers = new Buffers(21, RTMOPL.MaxReadBuffSize);
		m_readbuffer = RTMOPL.m_OPLBytesBuffers.GetBigBuffer(RTMOPL.MaxReadBuffSize);
		m_Operator = new OperatorRec();
		m_DBOperator = new DBOperator();
		m_OperatorPrivilege = new OperatorPrivilegeRec();
		m_DBOperatorPrivilege = new DBOperatorPrivilege();
		m_OperatorToUnit = new OperatorToUnitRec();
		m_DBOperatorToUnit = new DBOperatorToUnit();
		m_Method = new MethodRec();
		m_DBMethod = new DBMethod();
		m_LocLastUpdate = new LocLastUpdateRec();
		m_DBLocLastUpdate = new DBLocLastUpdate();
		m_InstLocation = new InstLocationRec();
		m_DBInstLocation = new DBInstLocation();
		m_FacilityList = new FacilityList();
		m_LocationList = new LocationList();
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_parent = parent;
		m_clienthandler = clienthandler;
		m_loc_port = loc_port;
		m_isShutDown = false;
		m_isShuttingDown = false;
		callbackWrite = OnWriteComplete;
		m_b_loc_last_update_inst_class_column = RTMOPL.m_b_loc_last_update_inst_class_column;
		m_b_loc_last_update_inst_type_column = RTMOPL.m_b_loc_last_update_inst_type_column;
		theConnection = "DSN=" + m_NNBase.DATASOURCE + ";UID=" + m_NNBase.UAUTHORITY + ";PWD=" + m_NNBase.PAUTHORITY;
		m_NNBase.NNBaseOpen(logging, "OPL", "RTMOPL", "OPL");
		bDBAvailable = OpenDBConnection(ref myDBReadConnection, ref myDBReadCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = OpenDBConnection(ref myDBWriteConnection, ref myDBWriteCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		m_NNBase.CommAudit(10, "Connect", "");
		string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMOPL' and host = '" + m_NNBase.GetLocalPOP() + "'";
		myDBWriteCommand.CommandText = sCommand;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
		}
		myDBWriteCommand.ExecuteNonQuery();
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Connection established via local port " + m_loc_port, isXml: false, "RTMOPL");
		}
		m_NNBase.bLocationAliasProcessingEnabled = true;
		if (bDBAvailable)
		{
			GetCompleteLocNumList();
			if (comLocNumList.Count < 1)
			{
				m_NNBase.ReportErrorDB("Facility and location list is empty", "E", "loading facility list", "DMLOPLBaseInit", "");
				bDBAvailable = false;
			}
			MakeInstrumentTypeList();
			FillTestWithInstTypeList();
			AddMTEToStatStripDevice();
		}
		string configValue = ConfigurationManager.AppSettings["UseFacilityUUID"];
		if (!string.IsNullOrEmpty(configValue))
		{
			m_UseFacilityUUID = configValue == "T";
		}
		if (m_UseFacilityUUID)
		{
			AddCompleteFacNumToLocList();
		}
		m_ProtocolThread = new Thread(ProtocolThread);
		m_ProtocolThread.Start();
		cmTimer = new System.Timers.Timer();
		cmTimer.AutoReset = true;
		cmTimer.Elapsed += OnCmTimedEvent;
		cmTimer.Interval = 60000.0;
		cmTimer.Enabled = true;
		m_LastMessageSent = "Hello";
		SendHello();
	}

	private void OnCmTimedEvent(object source, ElapsedEventArgs ev)
	{
		bool bgetout = false;
		lock (this)
		{
			bgetout = m_isShutDown | m_isShuttingDown | m_ProtocolSending | m_TimerSending | m_isProcessing | m_inTimedEvent;
			if (!bgetout)
			{
				m_inTimedEvent = true;
				cmTimer.Stop();
				if (!m_waiting)
				{
					m_TimerSending = true;
				}
			}
			else if (!m_inTimedEvent && !m_isShutDown && !m_isShuttingDown && !cmTimer.Enabled)
			{
				cmTimer.Start();
			}
		}
		if (bgetout)
		{
			return;
		}
		if (m_TimerSending && !m_isProcessing)
		{
			SendKeepAliveMessage();
		}
		if (m_kpaTimeoutCount < 4)
		{
			lock (this)
			{
				m_kpaTimeoutCount++;
			}
		}
		else if (m_waiting)
		{
			ShutDown("Timeout", "Timer", bExit: true);
			return;
		}
		lock (this)
		{
			m_inTimedEvent = false;
			cmTimer.Start();
		}
	}

	private void OnReadComplete()
	{
		string reason = "ReadError";
		bool bWholeMessageFound = false;
		bool bOK = true;
		int bytesRead = 0;
		int iDataLen = 0;
		int iOneBufferDataLen = 0;
		int iBegin = 0;
		int iEnd = 0;
		bool bMessageBeginFound = false;
		bool bMessageEndFound = false;
		bool bResetReaderBuffer = false;
		if (m_readbuffer != null)
		{
			Array.Clear(m_readbuffer, 0, m_readbuffer.Length);
		}
		try
		{
			while (bOK && IsAliveAndWell() && !bWholeMessageFound)
			{
				bOK = IsAliveAndWell() && m_networkStream.CanRead;
				if (bOK)
				{
					do
					{
						lock (this)
						{
							m_isProcessing = false;
						}
						if (bResetReaderBuffer)
						{
							bResetReaderBuffer = false;
							bytesRead = 0;
							iOneBufferDataLen = 0;
							iBegin = 0;
							iEnd = 0;
						}
						if (iOneBufferDataLen == 0 || !bWholeMessageFound)
						{
							bOK = RTMOPL.m_OPLBytesBuffers.NetworkStreamRead(ref m_networkStream, ref m_readbuffer, ref iOneBufferDataLen, ref m_isShutDown, ref m_isShuttingDown, ref bytesRead);
							iDataLen += bytesRead;
							bOK = IsAliveAndWell() && bytesRead > 0;
						}
						else
						{
							bOK = IsAliveAndWell();
						}
						if (bOK)
						{
							int bufferCount = m_readBufferList.Count;
							if (bufferCount > 0)
							{
								GetBufferList(bufferCount, out var bigBuffer);
								long dIndex = bufferCount * RTMOPL.MaxReadBuffSize;
								Array.Copy(m_readbuffer, 0L, bigBuffer, dIndex, iOneBufferDataLen);
								int bigBufferSize = bigBuffer.Length;
								m_NNBase.log("start parser", isXml: false, "DML");
								bWholeMessageFound = RTMOPL.m_OPLBytesBuffers.GetFullDMLMessageFromBuffer(ref bigBuffer, ref iBegin, ref iEnd, bigBufferSize, ref iDataLen, ref bMessageBeginFound, ref bMessageEndFound, ref m_ReadString);
								RTMOPL.m_OPLBytesBuffers.ReleaseBigBuffer(ref bigBuffer);
								bigBuffer = null;
							}
							else
							{
								bWholeMessageFound = RTMOPL.m_OPLBytesBuffers.GetFullDMLMessageFromBuffer(ref m_readbuffer, ref iBegin, ref iEnd, m_readbuffer.Length, ref iDataLen, ref bMessageBeginFound, ref bMessageEndFound, ref m_ReadString);
							}
							if (bWholeMessageFound)
							{
								CleanBufferList();
								m_message = m_ReadString;
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(m_message, isXml: true, m_portType);
								}
							}
							else
							{
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(bytesRead + " bytes read", isXml: false, m_portType);
								}
								if (iOneBufferDataLen + 1 > RTMOPL.MaxReadBuffSize)
								{
									byte[] addBuffer = RTMOPL.m_OPLBytesBuffers.GetBigBuffer(RTMOPL.MaxReadBuffSize);
									Array.Copy(m_readbuffer, addBuffer, RTMOPL.MaxReadBuffSize);
									m_readBufferList.Add(addBuffer);
									Array.Clear(m_readbuffer, 0, RTMOPL.MaxReadBuffSize);
									bResetReaderBuffer = true;
								}
							}
						}
						bOK = IsAliveAndWell() && bytesRead > 0;
					}
					while (bOK && !bWholeMessageFound);
				}
				if (bOK && bWholeMessageFound)
				{
					bool bgetout = false;
					lock (this)
					{
						if (IsAliveAndWell())
						{
							m_isProcessing = true;
						}
					}
					if (bgetout)
					{
						break;
					}
					ProcessMessage();
				}
				else if (bytesRead == 0)
				{
					reason = "Connection dropped";
					bOK = false;
					ShutDown(reason, "Protocol", bExit: true);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (IOException ex2)
		{
			reason = ((!m_stopping) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (IsAliveAndWell())
			{
				handleException(e, "Reading message(s)", "OnReadComplete");
			}
		}
	}

	private void OnWriteComplete(IAsyncResult ar)
	{
		try
		{
			m_networkStream.EndWrite(ar);
			lock (this)
			{
				m_TimerSending = false;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (IOException ex2)
		{
			string reason = ((!m_stopping) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (IsAliveAndWell())
			{
				handleException(e, "EndWrite failed", "OnWriteComplete");
			}
		}
	}

	public override void ProcessMessage()
	{
		lock (this)
		{
			m_kpaTimeoutCount = 0;
			m_waiting = false;
		}
		try
		{
			m_doc = new XmlDocument();
			m_doc.LoadXml(m_message);
			XmlNodeReader reader = new XmlNodeReader(m_doc);
			reader.Read();
			s_msg_type_rcvd = reader.LocalName;
			switch (s_msg_type_rcvd)
			{
			case "ACK.R01":
				ProcessAck(reader);
				break;
			case "END.R01":
				GetControlID(reader);
				SendAcknowledgeMessage(m_control_id, isError: false);
				ShutDown("END.R01", "Protocol", bExit: true);
				break;
			case "ESC.R01":
				ShutDown("ESC.R01", "Protocol", bExit: true);
				break;
			case "OPL.R01":
			case "OPL.R02":
				m_OplistMsg = m_message;
				GetControlID(reader);
				ProcessOperatorList();
				SendAcknowledgeMessage(m_control_id, isError: false);
				m_parent.m_iNumMessages++;
				m_parent.m_iTotMessages++;
				break;
			case "KPA.R01":
				GetControlID(reader);
				SendAcknowledgeMessage(m_control_id, isError: false);
				break;
			}
			m_message = "";
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

	private void GetControlID(XmlNodeReader reader)
	{
		while (reader.Read())
		{
			reader.MoveToContent();
			XmlNodeType nodeType = reader.NodeType;
			if (nodeType == XmlNodeType.Element && reader.IsStartElement())
			{
				string se = reader.LocalName;
				string text;
				if ((text = se) != null && text == "HDR.control_id")
				{
					m_control_id = reader.GetAttribute("V");
				}
			}
		}
	}

	private void ProcessAck(XmlNodeReader reader)
	{
		string m_type_cd = "";
		while (reader.Read())
		{
			reader.MoveToContent();
			XmlNodeType nodeType = reader.NodeType;
			if (nodeType == XmlNodeType.Element && reader.IsStartElement())
			{
				string se = reader.LocalName;
				string text;
				if ((text = se) != null && text == "ACK.type_cd")
				{
					m_type_cd = reader.GetAttribute("V");
				}
			}
		}
		if (m_type_cd == "AA")
		{
			switch (m_LastMessageSent)
			{
			case "Hello":
				m_LastMessageSent = "Status";
				SendStatus();
				break;
			case "Status":
				break;
			case "KPA":
				break;
			}
		}
		else if (m_LastMessageSent != "KPA")
		{
			ShutDown("ACK.type_cd V=\"" + m_type_cd + "\"", "Protocol", bExit: true);
		}
	}

	private void InitOperatorFields()
	{
		m_facility = "";
		m_location = "";
		m_loc_num = "";
		m_insttype = "";
		m_inst_class = "";
		m_OperatorID = "";
		m_OperatorLastName = "";
		m_OperatorFirstName = "";
		m_active_date = "";
		m_expiration_date = "";
		m_permission_level = "";
		m_password = "";
		m_method_list.Clear();
		m_unused_method_list.Clear();
		m_instrument_list.Clear();
		m_operator_num = "";
		m_Operator.Clear();
		m_OperatorPrivilege.Clear();
		m_OperatorToUnit.Clear();
		m_Method.Clear();
	}

	private void ProcessOperatorList()
	{
		m_action_cd = "";
		try
		{
			m_doc = new XmlDocument();
			m_doc.LoadXml(m_OplistMsg);
			XmlElement root = m_doc.DocumentElement;
			if (root.OuterXml.Length > 8 && root.OuterXml.Substring(0, 8) == "<OPL.R02")
			{
				XmlNodeList nodeList = root.SelectNodes("UPD");
				foreach (XmlNode upd in nodeList)
				{
					XmlElement elem = (XmlElement)upd.SelectSingleNode("UPD.action_cd");
					m_action_cd = elem.GetAttribute("V");
					XmlNodeList operatornodeList = upd.SelectNodes("OPR");
					if (m_action_cd == "D")
					{
						m_NNBase.log("Start Delete", isXml: false, "ProcessOperatorList");
						foreach (XmlNode opr in operatornodeList)
						{
							ProcessOperatorDelete(opr);
						}
						continue;
					}
					m_NNBase.log("Start insert or update", isXml: false, "ProcessOperatorList");
					foreach (XmlNode opr2 in operatornodeList)
					{
						ProcessOperatorInsert(opr2);
					}
				}
			}
			else
			{
				m_NNBase.log("Start insert or update", isXml: false, "ProcessOperatorList");
				XmlNodeList operatornodeList = root.SelectNodes("OPR");
				foreach (XmlNode opr3 in operatornodeList)
				{
					ProcessOperatorInsert(opr3);
				}
			}
			Update_loc_last_opdate_OPERATOR();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing operator list", "ProcessOperatorList");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing operator list", "ProcessOperatorList");
		}
	}

	private void ProcessOperatorDelete(XmlNode opr)
	{
		try
		{
			InitOperatorFields();
			if (!GetOperatorFields(opr) || !LookupOperator(ToUpdate: false))
			{
				return;
			}
			MakeLocationList();
			if (m_location_list.Count <= 0)
			{
				return;
			}
			foreach (string item2 in m_location_list)
			{
				string loc_num = (m_loc_num = item2);
				m_DBOperatorToUnit.Read(this, ref m_OperatorToUnit, m_operator_num, m_loc_num, ref myDBReadCommand);
				if (m_OperatorToUnit.m_bOK && m_OperatorToUnit.m_bUnitRead)
				{
					m_OperatorToUnit.Clear();
					m_OperatorToUnit.m_OperatorNum = m_operator_num;
					m_OperatorToUnit.m_isactive = "F";
					m_OperatorToUnit.m_isactivelastupdatedate = DateTime.Now;
					m_OperatorToUnit.m_locnum = m_loc_num;
					m_DBOperatorToUnit.UpdateOperatorToUnit(this, ref m_OperatorToUnit, ref myDBWriteCommand);
					string item = loc_num + "^" + m_insttype;
					if (!locLastUpdateList.Contains(item))
					{
						locLastUpdateList.Add(item);
					}
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing delete operator", "ProcessOperatorDelete");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing delete operator", "ProcessOperatorDelete");
		}
	}

	private void ProcessOperatorInsert(XmlNode opr)
	{
		try
		{
			InitOperatorFields();
			bool bOK = GetOperatorFields(opr);
			if (m_active_date.Length == 0)
			{
				bOK = false;
				m_NNBase.ReportErrorDB("Missing active date", "E", "processing insert operator", "ProcessOperatorInsert", "");
			}
			if (m_expiration_date.Length == 0)
			{
				bOK = false;
				m_NNBase.ReportErrorDB("Missing expiration date", "E", "processing insert operator", "ProcessOperatorInsert", "");
			}
			if (!bOK)
			{
				return;
			}
			LookupOperator(ToUpdate: true);
			if (m_Operator.m_errortype >= 10)
			{
				return;
			}
			m_Operator.m_OperatorID = m_OperatorID;
			m_Operator.m_Firstname = m_OperatorFirstName;
			m_Operator.m_Lastname = m_OperatorLastName;
			m_Operator.m_lastupdatedate = DateTime.Now;
			m_Operator.m_adddate = m_Operator.m_lastupdatedate;
			m_DBOperator.CreateorUpdate(this, ref m_Operator, ref myDBWriteCommand);
			if (!m_Operator.m_bOK)
			{
				return;
			}
			m_operator_num = m_Operator.m_OperatorNum;
			MakeLocationList();
			if (m_location_list.Count <= 0)
			{
				return;
			}
			foreach (string item2 in m_location_list)
			{
				string loc_num = (m_loc_num = item2);
				m_DBOperatorToUnit.Read(this, ref m_OperatorToUnit, m_operator_num, m_loc_num, ref myDBReadCommand);
				if (m_parent.m_bTestNameColumn)
				{
					foreach (string item3 in m_method_list)
					{
						string s = (m_method = item3);
						string instTypes = string.Empty;
						if (ValidTestWithInstType.ContainsKey(s.ToLower()))
						{
							instTypes = ValidTestWithInstType[s.ToLower()];
						}
						string[] instTypeList = null;
						if (instTypes.Length > 1)
						{
							instTypeList = instTypes.Split(',');
						}
						string[] array = instTypeList;
						foreach (string sInstType in array)
						{
							if (m_bCertByTest)
							{
								ReadAndUpdateOperatorPrivilege(sInstType);
							}
							else if (Comp.Compare(sInstType, m_insttype, CompOpt) == 0)
							{
								ReadAndUpdateOperatorPrivilege(sInstType);
							}
							else
							{
								ReadOrCreateEmptyOperatorPrivilege(sInstType);
							}
							string item = loc_num + "^" + sInstType;
							if (!locLastUpdateList.Contains(item))
							{
								locLastUpdateList.Add(item);
							}
						}
					}
				}
				else
				{
					m_method = "";
					ReadAndUpdateOperatorPrivilege(m_insttype);
				}
				if (!m_OperatorPrivilege.m_bOK)
				{
					continue;
				}
				m_OperatorToUnit.m_OperatorNum = m_operator_num;
				m_OperatorToUnit.m_locnum = m_loc_num;
				m_OperatorToUnit.m_isactivelastupdatedate = DateTime.Now;
				m_OperatorToUnit.m_isactive = "T";
				m_DBOperatorToUnit.CreateorUpdate(this, ref m_OperatorToUnit, ref myDBWriteCommand);
				if (!m_OperatorToUnit.m_bOK || !m_parent.m_bMethodsTable)
				{
					continue;
				}
				foreach (string s2 in m_method_list)
				{
					m_Method.m_OperatorNum = m_operator_num;
					m_Method.m_insttype = m_insttype;
					m_Method.m_methodcd = s2;
					m_DBMethod.Create(this, ref m_Method, ref myDBWriteCommand);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing insert operator", "ProcessOperatorInsert");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing insert operator", "ProcessOperatorInsert");
		}
	}

	private void Update_loc_last_opdate_OPERATOR()
	{
		OdbcCommand cmdLocLastUpdate = myDBWriteConnection.CreateCommand();
		string updateByFacNLoc = "INSERT INTO dba.loc_last_update(loc_num, data_type, inst_type) ON EXISTING  UPDATE VALUES (?,'OPERATORS',?)";
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(updateByFacNLoc, isXml: false, "SQL");
		}
		try
		{
			cmdLocLastUpdate.CommandText = updateByFacNLoc;
			for (int n = 0; n < locLastUpdateList.Count; n++)
			{
				string oneItem = locLastUpdateList[n];
				string[] arrayItem = oneItem.Split('^');
				cmdLocLastUpdate.Parameters.AddWithValue("@loc_num", arrayItem[0]);
				cmdLocLastUpdate.Parameters.AddWithValue("@inst_type", arrayItem[1]);
				cmdLocLastUpdate.ExecuteNonQuery();
				cmdLocLastUpdate.Parameters.Clear();
				if (m_NNBase.m_isLogging)
				{
					string msg = "loc_num (" + arrayItem[0] + "), inst_type (" + arrayItem[1] + ")";
					m_NNBase.log(msg, isXml: false, "Update_loc_last_opdate_OPERATOR");
				}
			}
		}
		catch
		{
		}
		finally
		{
			CleanCommand(cmdLocLastUpdate);
			locLastUpdateList.Clear();
		}
	}

	private void ReadAndUpdateOperatorPrivilege(string sInstType)
	{
		if (Comp.Compare(m_method, "MTE", CompOpt) == 0)
		{
			sInstType = "MTE";
		}
		m_DBOperatorPrivilege.Read(this, ref m_OperatorPrivilege, m_operator_num, sInstType, m_method, ref myDBReadCommand);
		UpdateOperatorPrivilegeFields(sInstType);
		m_DBOperatorPrivilege.CreateorUpdate(this, ref m_OperatorPrivilege, ref myDBWriteCommand);
	}

	private void ReadOrCreateEmptyOperatorPrivilege(string sInstType)
	{
		if (Comp.Compare(m_method, "MTE", CompOpt) == 0)
		{
			sInstType = "MTE";
		}
		m_DBOperatorPrivilege.Read(this, ref m_OperatorPrivilege, m_operator_num, sInstType, m_method, ref myDBReadCommand);
		if (!m_OperatorPrivilege.m_bPrivRead)
		{
			m_OperatorPrivilege.m_OperatorNum = m_operator_num;
			m_OperatorPrivilege.m_insttype = sInstType;
			m_OperatorPrivilege.m_pswd = "";
			m_OperatorPrivilege.m_certstartdate = DateTime.MinValue;
			m_OperatorPrivilege.m_certenddate = DateTime.MinValue;
			m_OperatorPrivilege.m_privilege = 0;
			m_OperatorPrivilege.m_lastupdatedate = DateTime.Now;
			m_OperatorPrivilege.m_isactivelastupdatedate = DateTime.MinValue;
			m_OperatorPrivilege.m_isactive = "";
			m_OperatorPrivilege.m_testname = m_method;
			m_DBOperatorPrivilege.CreateOperatorPrivilege(this, ref m_OperatorPrivilege, ref myDBWriteCommand);
		}
	}

	private bool GetOperatorFields(XmlNode opr)
	{
		bool bOK = true;
		try
		{
			XmlElement elem = (XmlElement)opr.SelectSingleNode("OPR.operator_id");
			if (elem != null)
			{
				m_OperatorID = elem.GetAttribute("V").Trim();
				if (m_OperatorID == null || m_OperatorID.Length == 0)
				{
					m_NNBase.ReportErrorDB("Missing Operator ID", "E", "getting operator fields", "GetOperatorFields", "");
					bOK = false;
				}
				string nt = elem.InnerText;
				char[] hat = new char[1] { '^' };
				string[] nts = nt.Split(hat);
				bool bFacilityFound = false;
				if (nts.GetLength(0) > 0)
				{
					if (Comp.Compare(nts[0], "ALL", CompOpt) != 0)
					{
						m_facility = nts[0];
						if (m_facility.Length > 0)
						{
							foreach (KeyValuePair<string, string> comLocNum in comLocNumList)
							{
								if (comLocNum.Key.Contains(m_facility.ToLower() + "^"))
								{
									bFacilityFound = true;
									break;
								}
							}
							if (!bFacilityFound)
							{
								m_NNBase.ReportErrorDB("Invalid facility", "E", "getting operator fields", "GetOperatorFields", "");
								bOK = false;
							}
						}
					}
					else
					{
						m_facility = "ALL";
						bFacilityFound = true;
					}
					if (nts.GetLength(0) > 1)
					{
						m_location = nts[1];
						if (bFacilityFound && m_location.Length > 0)
						{
							if (Comp.Compare(m_facility, "ALL", CompOpt) != 0 && Comp.Compare(m_location, "ALL", CompOpt) != 0)
							{
								string key = m_facility.ToLower() + "^" + m_location.ToLower();
								if (!comLocNumList.ContainsKey(key))
								{
									m_NNBase.ReportErrorDB("Invalid location", "E", "getting operator fields", "GetOperatorFields", "");
									bOK = false;
								}
							}
							else if (Comp.Compare(m_facility, "ALL", CompOpt) == 0 && Comp.Compare(m_location, "ALL", CompOpt) != 0)
							{
								m_NNBase.ReportErrorDB("Invalid location", "E", "getting operator fields", "GetOperatorFields", "");
								bOK = false;
							}
							else
							{
								m_location = "ALL";
							}
						}
						if (nts.GetLength(0) > 2)
						{
							m_insttype = nts[2];
							if (m_insttype.Length > 0 && !instTypeList.Contains(m_insttype.ToLower()))
							{
								m_NNBase.ReportErrorDB("Invalid instrument type", "E", "getting operator fields", "GetOperatorFields", "");
								bOK = false;
							}
						}
					}
				}
				if (m_insttype.Length == 0)
				{
					m_NNBase.ReportErrorDB("Missing instrument type", "E", "getting operator fields", "GetOperatorFields", "");
					bOK = false;
				}
				if (m_location.Length == 0)
				{
					m_NNBase.ReportErrorDB("Missing location", "E", "getting operator fields", "GetOperatorFields", "");
					bOK = false;
				}
				if (m_facility.Length == 0)
				{
					m_NNBase.ReportErrorDB("Missing facility", "E", "getting operator fields", "GetOperatorFields", "");
					bOK = false;
				}
				XmlNode opername = opr.SelectSingleNode("OPR.name");
				if (opername != null)
				{
					elem = (XmlElement)opername.SelectSingleNode("GIV");
					if (elem != null)
					{
						m_OperatorFirstName = elem.GetAttribute("V");
						m_OperatorFirstName = Regex.Replace(m_OperatorFirstName, "[\0-\u001f]", string.Empty);
						m_OperatorFirstName = m_OperatorFirstName.Trim();
					}
					elem = (XmlElement)opername.SelectSingleNode("FAM");
					if (elem != null)
					{
						m_OperatorLastName = elem.GetAttribute("V");
						m_OperatorLastName = Regex.Replace(m_OperatorLastName, "[\0-\u001f]", string.Empty);
						m_OperatorLastName = m_OperatorLastName.Trim();
					}
				}
				XmlNode operaccess = opr.SelectSingleNode("ACC");
				if (operaccess != null)
				{
					elem = (XmlElement)operaccess.SelectSingleNode("ACC.active_date");
					if (elem != null)
					{
						m_active_date = elem.GetAttribute("V");
					}
					elem = (XmlElement)operaccess.SelectSingleNode("ACC.expiration_date");
					if (elem != null)
					{
						m_expiration_date = elem.GetAttribute("V");
					}
					elem = (XmlElement)operaccess.SelectSingleNode("ACC.permission_level_cd");
					if (elem != null)
					{
						m_permission_level = elem.GetAttribute("V");
						if (m_permission_level == "")
						{
							m_permission_level = "4";
						}
					}
					else
					{
						m_permission_level = "4";
					}
					if (m_permission_level != "1" && m_permission_level != "4" && m_permission_level != "2")
					{
						m_NNBase.ReportErrorDB("Invalid permission level " + m_permission_level, "E", "getting operator fields", "GetOperatorFields", "");
						bOK = false;
					}
					XmlNodeList methodlist = operaccess.SelectNodes("ACC.method_cd");
					foreach (XmlNode method in methodlist)
					{
						elem = (XmlElement)method;
						string pnew = elem.GetAttribute("V");
						if (ValidTestWithInstType.ContainsKey(pnew.ToLower()))
						{
							m_method_list.Add(pnew);
							continue;
						}
						m_NNBase.ReportErrorDB("Invalid method code " + pnew, "E", "getting operator fields", "GetOperatorFields", "");
						bOK = false;
					}
				}
			}
			else
			{
				m_NNBase.ReportErrorDB("Missing Operator ID", "E", "getting operator fields", "GetOperatorFields", "");
				bOK = false;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "getting operator fields", "GetOperatorFields");
			bOK = false;
		}
		catch (Exception e2)
		{
			handleException(e2, "getting operator fields", "GetOperatorFields");
			bOK = false;
		}
		return bOK;
	}

	private void MakeLocationList()
	{
		m_location_list.Clear();
		string text;
		if ((text = m_facility.ToLower()) != null && text == "all")
		{
			if (m_UseFacilityUUID)
			{
				foreach (KeyValuePair<string, string> kvp in comLocNumList)
				{
					if (!kvp.Key.Contains("^"))
					{
						m_location_list.Add(kvp.Value);
					}
				}
				return;
			}
			{
				foreach (KeyValuePair<string, string> kvp2 in comLocNumList)
				{
					m_location_list.Add(kvp2.Value);
				}
				return;
			}
		}
		if (Comp.Compare(m_location, "ALL", CompOpt) == 0)
		{
			foreach (KeyValuePair<string, string> kvp3 in comLocNumList)
			{
				if (kvp3.Key.Contains(m_facility.ToLower() + "^"))
				{
					m_location_list.Add(kvp3.Value);
				}
			}
			return;
		}
		string key = m_facility.ToLower() + "^" + m_location.ToLower();
		if (comLocNumList.ContainsKey(key))
		{
			m_location_list.Add(comLocNumList[key]);
		}
	}

	private bool LookupOperator(bool ToUpdate)
	{
		string where = "operator_id = '" + m_OperatorID.Replace("'", "''") + "'";
		m_DBOperator.Read(this, ref m_Operator, where, ref myDBReadCommand, ToUpdate);
		if (m_Operator.m_bOK)
		{
			m_operator_num = m_Operator.m_OperatorNum;
		}
		else
		{
			m_operator_num = "";
		}
		return m_operator_num.Length > 0;
	}

	private void UpdateOperatorPrivilegeFields(string sInstType)
	{
		DateTime Now = DateTime.Now;
		DateTime Start = DML2Date(m_active_date);
		DateTime End = DML2ExpDate(m_expiration_date);
		m_OperatorPrivilege.m_OperatorNum = m_operator_num;
		m_OperatorPrivilege.m_insttype = sInstType;
		m_OperatorPrivilege.m_pswd = m_password;
		m_OperatorPrivilege.m_certstartdate = Start;
		m_OperatorPrivilege.m_certenddate = End;
		m_OperatorPrivilege.m_privilege = Convert.ToInt32(m_permission_level, 10);
		m_OperatorPrivilege.m_lastupdatedate = Now;
		if (DateTime.Compare(Now, Start) >= 0 && DateTime.Compare(Now, End) <= 0)
		{
			if (m_OperatorPrivilege.m_isactive == null || m_OperatorPrivilege.m_isactive != "T")
			{
				m_OperatorPrivilege.m_isactivelastupdatedate = Now;
			}
			m_OperatorPrivilege.m_isactive = "T";
		}
		else
		{
			if (m_OperatorPrivilege.m_isactive == null || m_OperatorPrivilege.m_isactive != "F")
			{
				m_OperatorPrivilege.m_isactivelastupdatedate = Now;
			}
			m_OperatorPrivilege.m_isactive = "F";
		}
		if (m_parent.m_bTestNameColumn)
		{
			m_OperatorPrivilege.m_testname = m_method;
		}
	}

	private bool SendAcknowledgeMessage(string control_id, bool isError)
	{
		bool retVal = true;
		try
		{
			string sAck = "<ACK.R01>" + GenDMLHeader("Protocol") + "<ACK><ACK.type_cd V=\"A" + (isError ? "E" : "A") + "\"/><ACK.ack_control_id V=\"" + control_id + "\"/></ACK></ACK.R01>";
			lock (this)
			{
				m_LastMessageSent = "ACK";
			}
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

	private bool SendKeepAliveMessage()
	{
		bool retVal = true;
		try
		{
			string sKpa = "<KPA.R01>" + GenDMLHeader("Timer") + "</KPA.R01>";
			lock (this)
			{
				m_waiting = true;
				m_LastMessageSent = "KPA";
			}
			AsyncSendString(sKpa);
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
				handleException(e, "sending keepalive message", "SendKeepAliveMessage");
			}
		}
		return retVal;
	}

	private bool SendTerminate(string reason, string note)
	{
		bool retVal = true;
		try
		{
			string sTrm = "<END.R01>" + GenDMLHeader("Protocol") + "<TRM><TRM.reason_cd V=\"" + reason + "\"/>" + ((note.Length > 0) ? ("<TRM.note_txt V=\"" + note + "\"/>") : "") + "</TRM></END.R01>";
			lock (this)
			{
				m_waiting = true;
				m_LastMessageSent = "Terminate";
			}
			SendString(sTrm, "Protocol");
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
				handleException(e, "sending terminate message", "SendTerminate");
			}
		}
		return retVal;
	}

	private bool SendEscape(string note)
	{
		bool retVal = true;
		try
		{
			string sEsc = "<ESC.R01>" + GenDMLHeader("Protocol") + "<ESC><ESC.esc_control_id V=\"" + m_control_id + "\"/><ESC.detail_cd V=\"OTH\"/>" + ((note.Length > 0) ? ("<ESC.note_txt V=\"" + note + "\"/>") : "") + "</ESC></ESC.R01>";
			lock (this)
			{
				m_LastMessageSent = "Escape";
			}
			SendString(sEsc, "Protocol");
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
				handleException(e, "sending escape message", "SendEscape");
			}
		}
		return retVal;
	}

	private string GenDMLHeader(string whoFrom)
	{
		string sHeader = "";
		try
		{
			lock (this)
			{
				sHeader = "<HDR><HDR.control_id V=\"" + m_imsgid + "\" /><HDR.version_id V=\"POCT1\" /><HDR.creation_dttm V=\"" + DateTime2DML(DateTime.Now, whoFrom) + "\" /></HDR>";
				m_imsgid++;
				if (m_imsgid > MaxMsgId)
				{
					m_imsgid = 4000;
				}
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
			string helloMessage = string.Format("<HEL.R01>{0}<DEV><DEV.device_id V=\"OPLIST CLIENT\"/><DSC><DSC.connection_profile_cd V=\"SA\"/><DSC.topics_supported_cd V=\"OP_LST\"/><DSC.topics_supported_cd V=\"OP_LST_I\"/><DSC.directives_supported_cd V=\"START_CONTINUOUS\"/></DSC></DEV></HEL.R01>", GenDMLHeader("RTMOPL"));
			lock (this)
			{
				m_waiting = true;
				m_LastMessageSent = "Hello";
			}
			SendString(helloMessage, "RTMOPL");
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RTMOPL");
		}
		catch (Exception e)
		{
			handleException(e, "Sending hello", "SendHello");
		}
		return ret;
	}

	private bool SendStatus()
	{
		bool ret = false;
		try
		{
			m_last_oper_upd_time = DateTime.MinValue;
			myDBReadCommand.CommandText = "SELECT MAX(last_update_date) from DBA.OPERATORS";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			if (myDBReadReader.Read() && !myDBReadReader.IsDBNull(0))
			{
				m_last_oper_upd_time = myDBReadReader.GetDateTime(0);
			}
			myDBReadReader.Close();
			m_last_operpriv_upd_time = DateTime.MinValue;
			myDBReadCommand.CommandText = "SELECT MAX(last_update_date) from DBA.OPERATOR_PRIVILEGE";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			if (myDBReadReader.Read() && !myDBReadReader.IsDBNull(0))
			{
				m_last_operpriv_upd_time = myDBReadReader.GetDateTime(0);
			}
			myDBReadReader.Close();
			m_last_operunit_upd_time = DateTime.MinValue;
			myDBReadCommand.CommandText = "SELECT MAX(is_active_last_update_date) from DBA.OPERATOR_TO_UNIT";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			if (myDBReadReader.Read() && !myDBReadReader.IsDBNull(0))
			{
				m_last_operunit_upd_time = myDBReadReader.GetDateTime(0);
			}
			myDBReadReader.Close();
			m_last_operall_upd_time = m_last_oper_upd_time;
			if (m_last_operpriv_upd_time.Ticks > m_last_operall_upd_time.Ticks)
			{
				m_last_operall_upd_time = m_last_operpriv_upd_time;
			}
			if (m_last_operunit_upd_time.Ticks > m_last_operall_upd_time.Ticks)
			{
				m_last_operall_upd_time = m_last_operunit_upd_time;
			}
			string statusMessage = string.Format("<DST.R01>{0}<DST><DST.status_dttm V=\"{1}\"/><DST.new_observations_qty V=\"0\"/><DST.condition_cd V=\"R\"/><DST.patients_update_dttm V=\"{1}\"/><DST.operators_update_dttm V=\"{2}\"/></DST></DST.R01>", GenDMLHeader("Protocol"), DateTime2DML(DateTime.Now, "Protocol"), DateTime2DML(m_last_operall_upd_time, "Protocol"));
			lock (this)
			{
				m_waiting = true;
				m_LastMessageSent = "Status";
			}
			SendString(statusMessage, "Protocol");
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "Sending status", "SendStatus");
		}
		catch (Exception e2)
		{
			handleException(e2, "Sending status", "SendStatus");
		}
		return ret;
	}

	private string FindTextByChildNode(ref XmlElement root, string parent, string name, string attribute, string aValue)
	{
		string ret = "";
		string xpath = parent + "/" + name;
		XmlNodeList nodeList = root.SelectNodes(xpath);
		foreach (XmlNode node in nodeList)
		{
			XmlElement enode = (XmlElement)node;
			string v = enode.GetAttribute(attribute);
			if (v == aValue)
			{
				ret = enode.InnerText;
				break;
			}
		}
		return ret;
	}

	private XmlElement FindNodeByChildNode(ref XmlElement root, string parent, string name, string attribute, string aValue)
	{
		XmlElement elem = null;
		string xpath = parent + "/" + name;
		XmlNodeList nodeList = root.SelectNodes(xpath);
		foreach (XmlNode node in nodeList)
		{
			XmlElement enode = (XmlElement)node;
			string v = enode.GetAttribute(attribute);
			if (v == aValue)
			{
				elem = (XmlElement)enode.ParentNode;
				break;
			}
		}
		return elem;
	}

	private XmlElement FindOrCreateChildNode(ref XmlDocument doc, ref XmlElement root, string parent, string name, string attribute, string aValue, string text)
	{
		XmlElement elemParent = FindNodeByChildNode(ref root, parent, name, attribute, aValue);
		if (elemParent == null)
		{
			elemParent = doc.CreateElement(parent);
			XmlElement elemChild = doc.CreateElement(name);
			doc.CreateAttribute(attribute);
			elemChild.SetAttribute(attribute, aValue);
			if (text.Length > 0)
			{
				XmlText t = doc.CreateTextNode(text);
				elemChild.AppendChild(t);
			}
			elemParent.AppendChild(elemChild);
			root.AppendChild(elemParent);
		}
		return elemParent;
	}

	private XmlElement FindOrAddToChildNode(ref XmlDocument doc, ref XmlElement root, string parent, string name, string attribute, string aValue, string text)
	{
		XmlElement elem = null;
		string xpath = parent + "/" + name + "/" + attribute;
		XmlNodeList nodeList = root.SelectNodes(xpath);
		foreach (XmlNode node in nodeList)
		{
			if (node.Value == aValue)
			{
				elem = (XmlElement)node.ParentNode.ParentNode;
				break;
			}
		}
		if (elem == null)
		{
			elem = (XmlElement)root.SelectSingleNode(parent);
			if (elem == null)
			{
				elem = doc.CreateElement(parent);
			}
			XmlElement elem2 = doc.CreateElement(name);
			elem2.SetAttribute(attribute, aValue);
			if (text.Length > 0)
			{
				XmlText t = doc.CreateTextNode(text);
				elem2.AppendChild(t);
			}
			elem.AppendChild(elem2);
			root.AppendChild(elem);
		}
		return elem;
	}

	private XmlElement FindOrAddNodeByAttribute(ref XmlDocument doc, ref XmlElement root, string name, string attribute, string aValue, string text)
	{
		XmlElement elem = null;
		XmlNodeList nodeList = root.SelectNodes(name);
		foreach (XmlNode node in nodeList)
		{
			elem = (XmlElement)node;
			if (!(elem.GetAttribute(attribute) == aValue))
			{
				elem = null;
				continue;
			}
			break;
		}
		if (elem == null)
		{
			elem = doc.CreateElement(name);
			doc.CreateAttribute(attribute);
			elem.SetAttribute(attribute, aValue);
			if (text.Length > 0)
			{
				XmlText t = doc.CreateTextNode(text);
				elem.AppendChild(t);
			}
			root.AppendChild(elem);
		}
		return elem;
	}

	private XmlElement FindOrAddNodeByAttributeU(ref XmlDocument doc, ref XmlElement root, string name, string attribute, string aValue, string text, string units)
	{
		XmlElement elem = null;
		XmlNodeList nodeList = root.SelectNodes(name);
		foreach (XmlNode node in nodeList)
		{
			elem = (XmlElement)node;
			if (!(elem.GetAttribute(attribute) == aValue) || !(elem.GetAttribute("U") == units))
			{
				elem = null;
				continue;
			}
			break;
		}
		if (elem == null)
		{
			elem = doc.CreateElement(name);
			doc.CreateAttribute(attribute);
			elem.SetAttribute(attribute, aValue);
			if (text.Length > 0)
			{
				XmlText t = doc.CreateTextNode(text);
				elem.AppendChild(t);
			}
			root.AppendChild(elem);
		}
		return elem;
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

	private DateTime DML2Date(string dmldt)
	{
		string sdate = ((dmldt.Length < 10) ? "0001-01-01 00:00:00" : (dmldt.Substring(0, 10) + " 00:00:00"));
		return DateTime.Parse(sdate);
	}

	private DateTime DML2ExpDate(string dmldt)
	{
		string sdate = ((dmldt.Length < 10) ? DateTime.MaxValue.ToString("yyyy-MM-dd HH:mm:ss") : (dmldt.Substring(0, 10) + " 23:59:59"));
		return DateTime.Parse(sdate);
	}

	public void ShutDown(string reason, string whoFrom, bool bExit)
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
			StopTimer();
			ShutDownTimer();
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
					m_NNBase.log("Closing client port " + sport + " for OPL interface", isXml: false, "dml");
				}
				m_clienthandler.socket.Close();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport + " for OPL interface", isXml: false, "dml");
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
			if (m_ProtocolThread != null && m_ProtocolThread.IsAlive && Thread.CurrentThread.ManagedThreadId != m_ProtocolThread.ManagedThreadId)
			{
				m_ProtocolThread.Abort();
			}
			Thread.Sleep(1000);
			if (bDBAvailable)
			{
				m_NNBase.CommAudit(11, "Disconnect", reason);
				string sCommand = "update DBA.health_ping set update_time = now(*), last_disconnect_dttm = now(*) where process_name = 'RTMOPL' and host = '" + m_NNBase.GetLocalPOP() + "'";
				myDBWriteCommand.CommandText = sCommand;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myDBWriteCommand.ExecuteNonQuery();
				myDBReadConnection.Close();
				myDBWriteConnection.Close();
			}
		}
		catch
		{
		}
		if (m_NNBase.m_isLogging)
		{
			try
			{
				m_NNBase.StopLogging();
			}
			catch
			{
				Console.WriteLine("Error closing log file");
			}
		}
		try
		{
			ReleaseBytesBuffer();
		}
		catch (ThreadAbortException)
		{
			Console.WriteLine("Thread abort exception releasing read buffer");
		}
		catch (Exception ex2)
		{
			Console.WriteLine("Exception releasing read buffer: " + ex2.Message);
		}
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Shutdown " + m_port_num, isXml: false, "DML");
		}
		m_isShutDown = true;
		if (bExit)
		{
			LibWrap.ExitThread(0u);
		}
	}

	private void StopTimer()
	{
		if (cmTimer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Stoping timer", isXml: false, "dml");
			}
			lock (this)
			{
				cmTimer.Stop();
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Stoped timer", isXml: false, "dml");
			}
		}
		else if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("cmTimer is null", isXml: false, "dml");
		}
	}

	private void ShutDownTimer()
	{
		if (cmTimer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing timer", isXml: false, "dml");
			}
			lock (this)
			{
				cmTimer.Close();
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed timer", isXml: false, "dml");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing timer", isXml: false, "dml");
			}
			lock (this)
			{
				cmTimer.Dispose();
				cmTimer = null;
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposed timer", isXml: false, "dml");
			}
		}
		else if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("cmTimer is null", isXml: false, "dml");
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

	public void handleException(Exception e, string when, string from)
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

	public void handleXMLException(XmlException e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString() + " at line: " + e.LineNumber + " " + e.StackTrace.ToString();
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("XML Exception " + e.GetType().ToString(), "C", when, from, details);
			ShutDown("XML Exception", "Protocol", bExit: true);
		}
	}

	public void handleDBException(OdbcException e, string when, string from)
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
		m_outbuffer = Encoding.UTF8.GetBytes(input);
		int i = m_outbuffer.Length;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(input, isXml: true, "RTMOPL");
		}
		try
		{
			m_networkStream.Write(m_outbuffer, 0, i);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (IOException)
		{
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown requested");
			ShutDown(reason, whoFrom, bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Writing message", "SendString");
			}
		}
		lock (this)
		{
			m_ProtocolSending = false;
			return i;
		}
	}

	public int AsyncSendString(string input)
	{
		m_asyncwritebuffer = Encoding.UTF8.GetBytes(input);
		int i = m_asyncwritebuffer.Length;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(input, isXml: true, "Timer");
		}
		try
		{
			if (m_networkStream.CanWrite)
			{
				m_networkStream.BeginWrite(m_asyncwritebuffer, 0, i, callbackWrite, m_networkStream);
			}
			else
			{
				if (m_NNBase.m_isLogging)
				{
					string msg = "Cannot write";
					m_NNBase.log(msg, isXml: true, "Timer");
				}
				ShutDown("Cannot write", "Timer", bExit: true);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (IOException)
		{
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown requested");
			ShutDown(reason, "Timer", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Writing message", "AsyncSendString");
			}
		}
		return i;
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

	private void ProtocolThread()
	{
		while (!m_isShutDown && !m_isShuttingDown)
		{
			OnReadComplete();
		}
	}

	public bool IsAliveAndWell()
	{
		return !m_isShutDown && !m_isShuttingDown;
	}

	private void ReleaseBytesBuffer()
	{
		if (m_readbuffer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Releasing read buffer", isXml: false, "dml");
			}
			RTMOPL.m_OPLBytesBuffers.ReleaseBigBuffer(ref m_readbuffer);
			m_readbuffer = null;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Read buffer released", isXml: false, "dml");
			}
		}
	}

	private void CleanBufferList()
	{
		for (int n = 0; n < m_readBufferList.Count; n++)
		{
			_ = m_readBufferList[n];
			byte[] thisBuffer = m_readBufferList[n];
			RTMOPL.m_OPLBytesBuffers.ReleaseBigBuffer(ref thisBuffer);
			thisBuffer = null;
		}
		m_readBufferList.Clear();
	}

	private bool GetBufferList(int bufferCount, out byte[] bigBuffer)
	{
		bool ret = false;
		bigBuffer = null;
		try
		{
			bigBuffer = RTMOPL.m_OPLBytesBuffers.GetBigBuffer(RTMOPL.MaxReadBuffSize * (bufferCount + 1));
			long desIndex = 0L;
			for (int n = 0; n < bufferCount; n++)
			{
				desIndex = n * RTMOPL.MaxReadBuffSize;
				byte[] thisBuffer = m_readBufferList[n];
				Array.Copy(thisBuffer, 0L, bigBuffer, desIndex, RTMOPL.MaxReadBuffSize);
			}
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "getting buffer list", "GetBufferList");
		}
		return ret;
	}

	private void CleanCommand(OdbcCommand command)
	{
		if (command != null)
		{
			command.Cancel();
			command.Dispose();
			command = null;
		}
	}
}
