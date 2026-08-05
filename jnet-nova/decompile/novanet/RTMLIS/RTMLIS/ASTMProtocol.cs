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
using Patient;

namespace RTMLIS;

internal class ASTMProtocol : Protocol
{
	private NNBase m_NNBase = new NNBase();

	protected CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	protected CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private int m_loc_port;

	private byte[] m_inbuffer = new byte[32768];

	private byte[] m_outbuffer;

	private byte[] m_workbuffer;

	private XmlDocument m_doc;

	private XmlElement svc;

	private XmlElement elem;

	private int m_nCurFrameSend;

	private int m_nCurFrame;

	private byte m_FrameChecksum;

	private byte[] m_szChecksum = new byte[3];

	private bool m_bLastFrame;

	private string m_device_id;

	private string m_serial_id;

	private string m_facility;

	private string m_location;

	private string m_loc_num = "";

	private string m_facil_num = "";

	private string m_inst_type = "";

	private string m_inst_name = "";

	private string m_control_type = "";

	private string m_svc_node = "";

	private DateTime sampleDateTime = DateTime.MinValue;

	private string m_inst_ver = "";

	private string operator_id = "";

	private string releaser_id = "";

	private string operator_last_name = "";

	private string operator_first_name = "";

	private string strip_lot_num = "";

	private string order_id = "";

	private string enterprise_id = "";

	private string medrec_num = "";

	private string account_num = "";

	private string patient_id = "";

	private string first_name = "";

	private string last_name = "";

	private string middle_name = "";

	private string prefix = "";

	private string suffix = "";

	private DateTime BirthDate = DateTime.MinValue;

	private string Sex = "";

	private string Physician = "";

	private string room_num = "";

	private string bed_num = "";

	private string Race_DML = "";

	private string Race_ASTM = "";

	private string AgeRange_DML = "";

	private string AgeRange_ASTM = "";

	private string Weight_DML = "";

	private string Weight_ASTM = "";

	private string Height_DML = "";

	private string Height_ASTM = "";

	private string Diagnosis = "";

	private string PatientClass = "";

	private string PatientType = "";

	private string control_lot_num = "";

	private string control_lot_level = "";

	private string control_internal_external = "";

	private string tgc_flag = "";

	private string sample_type_DML = "";

	private string sample_type_ASTM = "";

	private string role_cd = "";

	private string m_loc_def_pat_id = "";

	private string status_DML = "A";

	private string status_ASTM = "";

	private string normal_limit_lo = "";

	private string normal_limit_hi = "";

	private string critical_limit_lo = "";

	private string critical_limit_hi = "";

	private string test_code = "";

	private string test_transmit_name = "";

	private string test_code_system = "";

	private string method_cd_DML = "";

	private string method_cd_ASTM = "";

	private string interpretation_DML = "";

	private string interpretation_ASTM = "";

	private string result_str_value = "";

	private string units = "";

	private string comment_text = "";

	private string[] response_patid = new string[3] { "", "", "" };

	private string[] sent_patid = new string[3] { "", "", "" };

	private string response_patlastname = "";

	private string response_patfirstname = "";

	private string response_birthdate = "";

	private bool isQC;

	private bool bSampleError;

	public string inst_class;

	private State myState = new State();

	private long m_dwSendTimeoutStart;

	private long m_dwTimeoutStart;

	private int m_sendRetries;

	private bool bPrevBad;

	private string m_portType = "";

	private NetworkStream m_networkStream;

	private RTMLIS m_parent;

	private Port.AsynchNetworkServer.ClientHandler m_clienthandler;

	private int m_port_num;

	private System.Timers.Timer cmTimer;

	private string m_sample_key_num;

	private bool m_inTimedEvent;

	private bool m_isReading;

	private DateTime m_last_eot_update_time = DateTime.Now;

	private bool m_bConnected;

	private string[] m_LastMessageSent = new string[10];

	private string m_SelectSampleKeyNum = "";

	private Thread m_ProtocolThread;

	private bool bDBAvailable = true;

	private OdbcConnection myTimerConnection;

	private OdbcCommand myTimerCommand;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

	private OdbcConnection myDBReadConnection;

	private OdbcCommand myDBReadCommand;

	private OdbcConnection myStringsDBConnection;

	private OdbcCommand myStringsDBReadCommand;

	private OdbcDataReader myStringsDBReadReader;

	private bool m_more_samples;

	private OdbcDataReader myTimerReader;

	protected string BinDir = "C:\\NovaBiomedical\\NovaNet\\Bin";

	private bool bASTM_QueryForOrders;

	private bool m_bSamplesDeviceNameColumn;

	private bool m_bInstrumentsTestsLisTestAliasColumn;

	public bool m_bPatientVisitsTable;

	private bool m_needQuery;

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

	public ASTMProtocol(ref NetworkStream networkStream, int port_num, string portType, bool logging, RTMLIS parent, Port.AsynchNetworkServer.ClientHandler clienthandler, int loc_port)
	{
		m_bConnected = true;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_parent = parent;
		m_bSamplesDeviceNameColumn = m_parent.m_bSamplesDeviceNameColumn;
		m_bInstrumentsTestsLisTestAliasColumn = m_parent.m_bInstrumentsTestsLisTestAliasColumn;
		m_bPatientVisitsTable = m_parent.m_bPatientVisitsTable;
		m_clienthandler = clienthandler;
		m_NNBase.m_bLogging = logging;
		m_loc_port = loc_port;
		m_isShutDown = false;
		m_isShuttingDown = false;
	}

	private void Start()
	{
		m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, "ASTM", "RTMLIS", "LIS");
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
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = m_NNBase.OpenStringsDBConnection(ref myStringsDBConnection, ref myStringsDBReadCommand, 7);
		if (bDBAvailable)
		{
			m_NNBase.CommAudit(10, "Connect", "");
			string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
			myDBWriteCommand.CommandText = sCommand;
			if (m_NNBase.m_isLogging)
			{
				log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
			}
			myDBWriteCommand.ExecuteNonQuery();
			if (m_NNBase.m_isLogging)
			{
				log("Connection established via local port " + m_loc_port, isXml: false, "RTMLIS_ASTM");
			}
			m_NNBase.GetProcessControlValue("LIS", "ASTMQueryOrder", ref bASTM_QueryForOrders);
			m_SelectSampleKeyNum = "select top 1 sample_key_num from DBA.samples where transmitted_flag = 'F' and xml_text like '%<SVC>%</SVC>%' and control_type is not null and control_type != '' order by sample_Date";
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
			bgetout = m_isShutDown | m_isShuttingDown | m_isReading | m_inTimedEvent;
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
		if (m_bConnected)
		{
			bool bReadyToSend = false;
			bool bResendQuery = false;
			bool bResendResult = false;
			lock (myState)
			{
				if (myState.resultMessage.Count == 0 && myState.queryMessage.Count == 0 && myState.state == 0 && !myState.bSendingMessage && !myState.bWaitingForQueryResponse)
				{
					bReadyToSend = true;
					bResendQuery = myState.bRetryLastQueryMessage;
					myState.bRetryLastQueryMessage = false;
					bResendResult = myState.bRetryLastResultMessage;
					myState.bRetryLastResultMessage = false;
					myState.bSendingMessage = true;
				}
			}
			if (!bReadyToSend)
			{
				lock (myState)
				{
					ProcessState();
				}
			}
			else
			{
				try
				{
					if (!bResendResult && !bResendQuery)
					{
						myTimerCommand.CommandText = m_SelectSampleKeyNum;
						if (cmTimer.Interval == 100.0 && m_NNBase.m_isLogging)
						{
							log(m_SelectSampleKeyNum, isXml: false, "RTMLIS_ASTM_SQL");
						}
						myTimerReader = myTimerCommand.ExecuteReader();
						m_sample_key_num = "";
						if (m_more_samples = myTimerReader.Read())
						{
							m_sample_key_num = myTimerReader.GetString(0);
						}
						myTimerReader.Close();
					}
					if (m_sample_key_num.Length > 0)
					{
						Console.WriteLine("Next sample is {0}", m_sample_key_num);
						if (ProcessSample())
						{
							if (ProcessSVCNode())
							{
								lock (myState)
								{
									if (!m_needQuery)
									{
										myState.resultMessage.Add(GenASTMTerminator() + "\r");
									}
									else
									{
										myState.queryMessage.Add(GenASTMQuery() + "\r");
										myState.queryMessage.Add(GenASTMTerminator() + "\r");
									}
									myState.bFullMessage = true;
									ProcessState();
								}
							}
							else
							{
								if (bDBAvailable)
								{
									MarkSampleAsTransmitted("Timer");
								}
								lock (myState)
								{
									myState.resultMessage.Clear();
									myState.bSendingMessage = false;
								}
								cmTimer.Interval = 100.0;
							}
						}
						else
						{
							if (bDBAvailable)
							{
								MarkSampleAsTransmitted("Timer");
							}
							lock (myState)
							{
								myState.resultMessage.Clear();
								myState.bSendingMessage = false;
							}
							cmTimer.Interval = 100.0;
						}
					}
					else
					{
						lock (myState)
						{
							myState.bSendingMessage = false;
						}
						Console.WriteLine("No samples to send");
						if (cmTimer.Interval == 100.0 && m_NNBase.m_isLogging)
						{
							log("No samples to send", isXml: false, "RTMLIS_ASTM");
						}
						cmTimer.Interval = 500.0;
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
		}
		m_inTimedEvent = false;
		cmTimer.Start();
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
		m_inst_type = "";
		m_inst_ver = "";
		m_device_id = "";
		m_serial_id = "";
		m_location = "Unassigned";
		m_facility = "Unassigned";
		m_control_type = "";
		m_svc_node = "";
		m_loc_num = "";
		m_facil_num = "";
		sampleDateTime = DateTime.MinValue;
		operator_id = "";
		releaser_id = "";
		operator_last_name = "";
		operator_first_name = "";
		strip_lot_num = "";
		order_id = "";
		enterprise_id = "";
		medrec_num = "";
		account_num = "";
		patient_id = "";
		first_name = "";
		last_name = "";
		middle_name = "";
		prefix = "";
		suffix = "";
		BirthDate = DateTime.MinValue;
		Sex = "";
		Race_DML = "";
		Race_ASTM = "";
		AgeRange_DML = "";
		AgeRange_ASTM = "";
		Weight_DML = "";
		Weight_ASTM = "";
		Height_DML = "";
		Height_ASTM = "";
		Diagnosis = "";
		PatientClass = "";
		PatientType = "";
		Physician = "";
		room_num = "";
		bed_num = "";
		control_lot_num = "";
		tgc_flag = "";
		sample_type_DML = "";
		sample_type_ASTM = "";
		role_cd = "";
		m_loc_def_pat_id = "";
		status_DML = "A";
		status_ASTM = "";
		bSampleError = false;
		if (!m_bSamplesDeviceNameColumn)
		{
			try
			{
				myDBReadCommand.CommandText = $"select sw_version, inst_type, inst_id, serial_no, il.loc_name, ip.loc_name from DBA.instruments i left outer join DBA.inst_locations il on i.loc_num = il.loc_num left outer join DBA.inst_locations ip on ip.loc_num = il.parent where inst_id in (select device_serial from DBA.samples where sample_key_num = '{m_sample_key_num}')";
				if (m_NNBase.m_isLogging)
				{
					log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
				}
				OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
				readok = myReader.Read();
				if (readok)
				{
					m_inst_ver = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					m_inst_type = (myReader.IsDBNull(1) ? "" : myReader.GetString(1));
					m_device_id = (myReader.IsDBNull(2) ? "" : myReader.GetString(2));
					m_serial_id = (myReader.IsDBNull(3) ? "" : myReader.GetString(3));
					m_location = (myReader.IsDBNull(4) ? "Unassigned" : myReader.GetString(4));
					m_facility = (myReader.IsDBNull(5) ? "Unassigned" : myReader.GetString(5));
				}
				myReader.Close();
				myDBReadCommand.CommandText = "SELECT inst_class FROM DBA.INSTRUMENT_TYPES WHERE inst_type ='" + m_inst_type + "'";
				inst_class = (string)myDBReadCommand.ExecuteScalar();
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "reading instrument table", "ProcessSample");
			}
			catch (Exception e2)
			{
				handleException(e2, "reading instrument table", "ProcessSample");
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
				myDBReadCommand.CommandText = "select control_type, xml_text, accession_num, sample_date, control_lot_num, strip_lot_num, patient_id, medrec_num, account_num, loc_name, fac_name, device_name, device_type, device_serial, device_sw_ver, lot_level, internal_external";
				myDBReadCommand.CommandText += $" from DBA.samples where sample_key_num = '{m_sample_key_num}'";
				if (m_NNBase.m_isLogging)
				{
					log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
				}
				OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
				readok = myReader.Read();
				if (readok)
				{
					m_control_type = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					xml_text = (myReader.IsDBNull(1) ? "" : myReader.GetString(1));
					order_id = (myReader.IsDBNull(2) ? "" : myReader.GetString(2));
					sampleDateTime = (myReader.IsDBNull(3) ? DateTime.MinValue : myReader.GetDateTime(3));
					control_lot_num = (myReader.IsDBNull(4) ? "" : myReader.GetString(4));
					strip_lot_num = (myReader.IsDBNull(5) ? "" : myReader.GetString(5));
					enterprise_id = (myReader.IsDBNull(6) ? "" : myReader.GetString(6));
					medrec_num = (myReader.IsDBNull(7) ? "" : myReader.GetString(7));
					account_num = (myReader.IsDBNull(8) ? "" : myReader.GetString(8));
					m_location = (myReader.IsDBNull(9) ? m_location : myReader.GetString(9));
					m_facility = (myReader.IsDBNull(10) ? m_facility : myReader.GetString(10));
					m_inst_name = (myReader.IsDBNull(11) ? "" : myReader.GetString(11));
					m_inst_type = (myReader.IsDBNull(12) ? "" : myReader.GetString(12));
					m_serial_id = (m_device_id = (myReader.IsDBNull(13) ? "" : myReader.GetString(13)));
					m_inst_ver = (myReader.IsDBNull(14) ? "" : myReader.GetString(14));
					control_lot_level = (myReader.IsDBNull(15) ? "" : myReader.GetString(15));
					control_internal_external = (myReader.IsDBNull(16) ? "" : myReader.GetString(16));
				}
				myReader.Close();
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e3)
			{
				handleDBException(e3, "reading sample table", "ProcessSample");
			}
			catch (Exception e4)
			{
				handleException(e4, "reading sample table", "ProcessSample");
			}
			if (!readok)
			{
				m_NNBase.ReportErrorDB("Sample not found", "E", "selecting sample", "ProcessSample", "");
				ShutDown("Sample not found", "Protocol", bExit: true);
			}
			else if (m_control_type.Length > 0 && xml_text.Length > 0)
			{
				ret = true;
				isQC = m_control_type != "OBS";
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
			if (m_facility.Length > 0 && m_location.Length > 0)
			{
				m_loc_num = "";
				LookupLocNum();
				m_loc_def_pat_id = "";
				if (m_loc_num.Length > 0)
				{
					try
					{
						myDBReadCommand.CommandText = $"SELECT _value FROM DBA.config_data c join DBA.loc_to_config l2c on c.config_num = l2c.config_num where _key = 'PatIdTypeCd*V' and l2c.loc_num = '{m_loc_num}'";
						if (m_NNBase.m_isLogging)
						{
							log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
						}
						OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
						while (myReader.Read())
						{
							m_loc_def_pat_id = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
						}
						myReader.Close();
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("Timer");
					}
					catch (OdbcException e5)
					{
						handleDBException(e5, "getting default patient ID type", "ProcessSample");
					}
					catch (Exception e6)
					{
						handleException(e6, "getting default patient ID type", "ProcessSample");
					}
				}
			}
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
			catch (Exception e7)
			{
				handleException(e7, "parsing xml_text", "ProcessSample");
			}
		}
		return ret;
	}

	private bool ProcessSVCNode()
	{
		bool bOK = true;
		XmlNodeList nodeList2 = null;
		m_doc = new XmlDocument();
		try
		{
			m_doc.LoadXml(m_svc_node);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (XmlException e)
		{
			handleXMLException(e, "loading xml text for sample " + m_sample_key_num, "ProcessSample");
			bOK = false;
		}
		if (bOK)
		{
			svc = m_doc.DocumentElement;
			nodeList2 = svc.SelectNodes(isQC ? "CTC/OBS" : "PT/OBS");
			int nodecount = nodeList2.Count;
			if (nodecount < 1)
			{
				m_NNBase.ReportErrorDB("No " + (isQC ? "CTC/OBS" : "PT/OBS") + " nodes in sample", "E", "parsing xml_text in sample " + m_sample_key_num, "ProcessSample", "");
				bOK = false;
			}
		}
		m_needQuery = false;
		if (bOK)
		{
			lock (myState)
			{
				if (bASTM_QueryForOrders && !isQC && order_id.Length < 1)
				{
					m_needQuery = true;
				}
				if (m_needQuery)
				{
					myState.queryMessage.Add(GenASTMHeader() + "\r");
				}
				else
				{
					myState.resultMessage.Add(GenASTMHeader() + "\r");
				}
			}
			patient_id = "";
			if (!isQC)
			{
				string patient_id_type = m_loc_def_pat_id;
				string sample_id_type = FindTextByChildNode(svc, "NTE", "NTE.text", "V", "SAMPLE ID TYPE");
				Diagnosis = FindTextByChildNode(svc, "NTE", "NTE.text", "V", "DIAGCODE");
				if (sample_id_type != null && sample_id_type.Length > 0 && (sample_id_type == "PATID" || (sample_id_type == "MRN" && sample_id_type != "ACCT")))
				{
					patient_id_type = sample_id_type;
				}
				switch (patient_id_type)
				{
				case "PATID":
					if (enterprise_id.Length > 0 && enterprise_id != "UNKNOWN")
					{
						patient_id = enterprise_id;
					}
					break;
				case "MRN":
					if (medrec_num.Length > 0 && medrec_num != "UNKNOWN")
					{
						patient_id = medrec_num;
					}
					break;
				case "ACCT":
					if (account_num.Length > 0 && account_num != "UNKNOWN")
					{
						patient_id = account_num;
					}
					break;
				}
				elem = (XmlElement)svc.SelectSingleNode("PT/PT.birth_date");
				if (elem != null)
				{
					BirthDate = DateTime.Parse(elem.GetAttribute("V").Substring(0, 10));
				}
				elem = (XmlElement)svc.SelectSingleNode("PT/PT.gender_cd");
				if (elem != null)
				{
					Sex = elem.GetAttribute("V");
				}
				elem = (XmlElement)svc.SelectSingleNode("PT/PT.location");
				if (elem != null)
				{
					string loc = elem.GetAttribute("V");
					char[] hat = new char[1] { '^' };
					string[] locs = loc.Split(hat);
					if (locs.GetLength(0) > 2)
					{
						room_num = locs[2];
					}
					if (locs.GetLength(0) > 3)
					{
						bed_num = locs[3];
					}
				}
				if (patient_id.Length > 0)
				{
					GetPatientInfo(patient_id_type);
				}
			}
			else
			{
				patient_id = control_lot_num;
				elem = (XmlElement)svc.SelectSingleNode("RGT/RGT.expiration_date");
				if (elem != null)
				{
					string expdate = elem.GetAttribute("V");
					if (expdate.Length > 9)
					{
						BirthDate = DateTime.Parse(expdate.Substring(0, 10));
					}
					else
					{
						BirthDate = DateTime.Parse("2007-01-01");
					}
				}
			}
			lock (myState)
			{
				if (!m_needQuery)
				{
					myState.resultMessage.Add(GenASTMPatient() + "\r");
				}
			}
			elem = (XmlElement)svc.SelectSingleNode("SPC/SPC.type_cd");
			if (elem != null)
			{
				sample_type_DML = elem.GetAttribute("V");
			}
			elem = (XmlElement)svc.SelectSingleNode("SVC.role_cd");
			role_cd = elem.GetAttribute("V");
			if (sample_type_DML.Length == 0)
			{
				if (isQC)
				{
					sample_type_DML = role_cd;
				}
				else
				{
					sample_type_DML = "BLD";
				}
			}
			SampleTypeDMLtoASTM();
			elem = (XmlElement)svc.SelectSingleNode("OPR/OPR.operator_id");
			if (elem != null)
			{
				string operator_supervisor_id = elem.GetAttribute("V");
				int i = operator_supervisor_id.IndexOf("^");
				if (i >= 0)
				{
					operator_id = operator_supervisor_id.Substring(0, i);
					releaser_id = operator_supervisor_id.Substring(i + 1);
				}
				else
				{
					releaser_id = (operator_id = operator_supervisor_id);
				}
			}
			if (operator_id.Length > 0)
			{
				try
				{
					myDBReadCommand.CommandText = "select c.last_name, c.first_name from dba.contact_info c join dba.operators o on c.contact_num = o.operator_num where operator_id = '" + operator_id + "'";
					if (m_NNBase.m_isLogging)
					{
						log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
					}
					OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
					if (myReader.Read())
					{
						if (!myReader.IsDBNull(0))
						{
							operator_last_name = myReader.GetString(0);
						}
						if (!myReader.IsDBNull(1))
						{
							operator_first_name = myReader.GetString(1);
						}
					}
					myReader.Close();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Timer");
				}
				catch (OdbcException e2)
				{
					handleDBException(e2, "getting operator name", "ProcessSVCNode");
					bOK = false;
				}
				catch (Exception e3)
				{
					handleException(e3, "getting operator name", "ProcessSVCNode");
					bOK = false;
				}
			}
			if (bOK)
			{
				XmlNodeList noteList = svc.SelectNodes("NTE");
				foreach (XmlNode nte in noteList)
				{
					elem = (XmlElement)nte.FirstChild;
					string attribute = elem.GetAttribute("V");
					if (attribute == "TGC FLAG")
					{
						tgc_flag = elem.InnerText;
					}
				}
				if (!m_needQuery)
				{
					ProcessComments(noteList, bGenComments: false);
				}
				ProcessObservations(nodeList2, bGenResults: false);
				lock (myState)
				{
					if (!m_needQuery)
					{
						myState.resultMessage.Add(GenASTMOrder() + "\r");
					}
				}
				if (!m_needQuery)
				{
					ProcessComments(noteList, bGenComments: true);
				}
				bOK = ProcessObservations(nodeList2, bGenResults: true);
			}
		}
		return bOK;
	}

	private void ProcessComments(XmlNodeList noteList, bool bGenComments)
	{
		int nParm = 1;
		foreach (XmlNode nte in noteList)
		{
			comment_text = "";
			bool bError = false;
			elem = (XmlElement)nte.FirstChild;
			string attribute = elem.GetAttribute("V");
			if (!(attribute != "ID FLAGS") || !(attribute != "TGC FLAG") || !(attribute != "DIAGCODE") || !(attribute != "SAMPLE ID TYPE"))
			{
				continue;
			}
			comment_text = attribute;
			string comment_flags = elem.InnerText;
			if (comment_flags.IndexOf("TY=E") >= 0)
			{
				bError = (bSampleError = true);
			}
			else
			{
				comment_flags.IndexOf("CH=1");
				comment_flags.IndexOf("FL=1");
			}
			if (bGenComments)
			{
				lock (myState)
				{
					myState.resultMessage.Add(GenASTMComment(nParm++, bError) + "\r");
				}
			}
		}
	}

	private bool ProcessObservations(XmlNodeList nodeList2, bool bGenResults)
	{
		int nResultParm = 1;
		bool bOK = true;
		foreach (XmlNode obs in nodeList2)
		{
			test_code = "";
			units = "";
			result_str_value = "null";
			method_cd_DML = "";
			method_cd_ASTM = "";
			interpretation_DML = "";
			interpretation_ASTM = "";
			string normal_limits = "";
			string critical_limits = "";
			normal_limit_lo = "";
			normal_limit_hi = "";
			critical_limit_lo = "";
			critical_limit_hi = "";
			string display_name = "";
			test_transmit_name = "";
			if (bGenResults)
			{
				elem = (XmlElement)obs.SelectSingleNode("OBS.observation_id");
				if (elem != null)
				{
					test_code = elem.GetAttribute("V");
					display_name = elem.GetAttribute("DN");
					if (display_name.Length == 0)
					{
						display_name = test_code;
					}
				}
				elem = (XmlElement)obs.SelectSingleNode("OBS.value");
				if (elem != null)
				{
					result_str_value = elem.GetAttribute("V");
					units = elem.GetAttribute("U");
				}
				else
				{
					elem = (XmlElement)obs.SelectSingleNode("OBS.qualitative_value");
					if (elem != null)
					{
						result_str_value = elem.GetAttribute("V");
					}
				}
				elem = (XmlElement)obs.SelectSingleNode("OBS.method_cd");
				if (elem != null)
				{
					method_cd_DML = elem.GetAttribute("V");
				}
				elem = (XmlElement)obs.SelectSingleNode("OBS.interpretation_cd");
				if (elem != null)
				{
					interpretation_DML = elem.GetAttribute("V").Trim();
				}
			}
			elem = (XmlElement)obs.SelectSingleNode("OBS.status_cd");
			if (elem != null)
			{
				status_DML = elem.GetAttribute("V");
			}
			else if (result_str_value.Length < 1)
			{
				status_DML = "D";
			}
			else
			{
				status_DML = "A";
			}
			StatusDMLtoASTM();
			InterpretationDMLtoASTM();
			if (status_ASTM.Length > 0 && bGenResults)
			{
				elem = (XmlElement)obs.SelectSingleNode("OBS.normal_lo-hi_limit");
				if (elem != null)
				{
					normal_limits = elem.GetAttribute("V");
					normal_limits.Trim();
					int iHat = normal_limits.IndexOfAny(new char[1] { ';' });
					if (iHat > 0)
					{
						normal_limit_lo = normal_limits.Substring(1, iHat - 1);
						normal_limit_hi = normal_limits.Substring(iHat + 1, normal_limits.Length - iHat - 2);
					}
				}
				elem = (XmlElement)obs.SelectSingleNode("OBS.critical_lo-hi_limit");
				if (elem != null)
				{
					critical_limits = elem.GetAttribute("V");
					critical_limits.Trim();
					int iHat2 = critical_limits.IndexOfAny(new char[1] { ';' });
					if (iHat2 > 0)
					{
						critical_limit_lo = critical_limits.Substring(1, iHat2 - 1);
						critical_limit_hi = critical_limits.Substring(iHat2 + 1, critical_limits.Length - iHat2 - 2);
					}
				}
				test_code_system = "";
				test_transmit_name = "";
				try
				{
					myDBReadCommand.CommandText = " select first test_code_system, test_transmit_name from DBA.instruments_tests where inst_type = '" + m_inst_type + "' and result_type_code = '" + method_cd_DML + "' and test_code = '" + test_code + "'";
					if (units.Length > 0)
					{
						OdbcCommand odbcCommand = myDBReadCommand;
						odbcCommand.CommandText = odbcCommand.CommandText + " and units = '" + units + "'";
					}
					if (m_NNBase.m_isLogging)
					{
						log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
					}
					OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
					if (myReader.Read())
					{
						test_code_system = myReader.GetString(0);
						test_transmit_name = myReader.GetString(1);
					}
					else
					{
						test_code_system = "NOVABIO";
						test_transmit_name = display_name;
					}
					myReader.Close();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Timer");
				}
				catch (OdbcException e)
				{
					handleDBException(e, "getting generic test name", "ProcessObservations");
					bOK = false;
					break;
				}
				catch (Exception e2)
				{
					handleException(e2, "getting generic test name", "ProcessObservations");
					bOK = false;
					break;
				}
				MethodCdDMLtoASTM();
				if (method_cd_ASTM.Length > 0)
				{
					lock (myState)
					{
						if (!m_needQuery)
						{
							myState.resultMessage.Add(GenASTMResult(nResultParm++) + "\r");
						}
						else
						{
							AddTestToList(ref myState.queryTestList);
						}
					}
					if (!m_needQuery)
					{
						XmlNodeList noteList = obs.SelectNodes("NTE");
						comment_text = "";
						int nCommentParm = 1;
						foreach (XmlNode nte in noteList)
						{
							elem = (XmlElement)nte.FirstChild;
							elem.GetAttribute("V");
							comment_text = elem.InnerText;
							lock (myState)
							{
								myState.resultMessage.Add(GenASTMComment(nCommentParm++, bError: false) + "\r");
							}
						}
					}
				}
				else
				{
					m_NNBase.ReportErrorDB("No translation for method code " + method_cd_DML, "E", "generating result records", "ProcessObservations", "");
					bOK = false;
				}
			}
			if (status_ASTM.Length == 0 && bGenResults)
			{
				m_NNBase.ReportErrorDB("No translation for status code " + status_DML, "E", "generating result records", "ProcessObservations", "");
				bOK = false;
			}
			XmlNodeList noteList2 = obs.SelectNodes("NTE");
			foreach (XmlNode nte2 in noteList2)
			{
				elem = (XmlElement)nte2.FirstChild;
				string attribute = elem.GetAttribute("V");
				int iagerng = attribute.IndexOf("PAT_AGE_RANGE");
				int iethn = attribute.IndexOf("PAT_ETHNICITY");
				int iwght = attribute.IndexOf("PAT_WEIGHT");
				int ihght = attribute.IndexOf("PAT_HEIGHT");
				if (iagerng < 0 && iethn < 0 && iwght < 0 && ihght < 0)
				{
					continue;
				}
				char[] sep = new char[2] { '^', '=' };
				string[] parts = attribute.Split(sep);
				for (int i = 0; i < parts.Length - 1; i += 2)
				{
					if (parts[i] == "PAT_AGE_RANGE")
					{
						AgeRange_DML = parts[i + 1];
						AgeRangeDMLtoASTM();
					}
					else if (parts[i] == "PAT_ETHNICITY")
					{
						Race_DML = parts[i + 1];
						RaceDMLtoASTM();
					}
					else if (parts[i] == "PAT_WEIGHT")
					{
						Weight_DML = parts[i + 1];
						WeightDMLtoASTM();
					}
					else if (parts[i] == "PAT_HEIGHT")
					{
						Height_DML = parts[i + 1];
						HeightDMLtoASTM();
					}
				}
			}
		}
		return bOK;
	}

	private void StatusDMLtoASTM()
	{
		if (status_DML == "A")
		{
			status_ASTM = "F";
		}
		else if (status_DML == "D")
		{
			status_ASTM = "X";
		}
		else if (status_DML == "U")
		{
			status_ASTM = "P";
		}
		else if (status_DML == "X")
		{
			status_ASTM = "X";
		}
		else
		{
			status_ASTM = "";
		}
	}

	private void MethodCdDMLtoASTM()
	{
		if (method_cd_DML == "C")
		{
			method_cd_ASTM = "C";
		}
		else if (method_cd_DML == "D")
		{
			method_cd_ASTM = "D";
		}
		else if (method_cd_DML == "E")
		{
			method_cd_ASTM = "";
		}
		else if (method_cd_DML == "I")
		{
			method_cd_ASTM = "E";
		}
		else if (method_cd_DML == "M")
		{
			method_cd_ASTM = "M";
		}
		else if (method_cd_DML == "U")
		{
			method_cd_ASTM = "";
		}
		else
		{
			method_cd_ASTM = "";
		}
	}

	private void InterpretationDMLtoASTM()
	{
		if (interpretation_DML == "L")
		{
			interpretation_ASTM = "L";
		}
		else if (interpretation_DML == "H")
		{
			interpretation_ASTM = "H";
		}
		else if (interpretation_DML == "LL")
		{
			interpretation_ASTM = "LL";
		}
		else if (interpretation_DML == "HH")
		{
			interpretation_ASTM = "HH";
		}
		else if (interpretation_DML == "<")
		{
			interpretation_ASTM = "<";
		}
		else if (interpretation_DML == ">")
		{
			interpretation_ASTM = ">";
		}
		else if (interpretation_DML == "N")
		{
			interpretation_ASTM = "N";
		}
		else if (interpretation_DML == "A")
		{
			interpretation_ASTM = "A";
		}
		else if (interpretation_DML == "AA")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "null")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "U")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "D")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "B")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "W")
		{
			interpretation_ASTM = "";
		}
		else if (interpretation_DML == "UC")
		{
			interpretation_ASTM = "UC";
		}
		else if (interpretation_DML == "PC")
		{
			interpretation_ASTM = "PC";
		}
		else if (interpretation_DML == "QC")
		{
			interpretation_ASTM = "QC";
		}
		else if (interpretation_DML == "X")
		{
			interpretation_ASTM = "X";
		}
		else if (interpretation_DML == "PASS")
		{
			interpretation_ASTM = "PASS";
		}
		else if (interpretation_DML == "FAIL")
		{
			interpretation_ASTM = "FAIL";
		}
		else
		{
			interpretation_ASTM = "";
		}
	}

	private void SampleTypeDMLtoASTM()
	{
		sample_type_ASTM = sample_type_DML;
	}

	private void RaceDMLtoASTM()
	{
		if (Race_DML == "NB")
		{
			Race_ASTM = "W";
		}
		else if (Race_DML == "B")
		{
			Race_ASTM = "B";
		}
		else if (Race_DML == "JP")
		{
			Race_ASTM = "O";
		}
		else if (Race_DML.Length == 0)
		{
			Race_ASTM = "U";
		}
		else
		{
			Race_ASTM = "U";
		}
	}

	private void AgeRangeDMLtoASTM()
	{
		if (AgeRange_DML == "NA")
		{
			AgeRange_ASTM = "N";
		}
		else
		{
			if (AgeRange_DML.Length <= 0)
			{
				return;
			}
			myStringsDBReadCommand.CommandText = "select var_name from dba.ui_translations where page_name = 'egfr_strings' and var_value = '" + AgeRange_DML + "'";
			if (m_NNBase.m_isLogging)
			{
				log(myStringsDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myStringsDBReadReader = myStringsDBReadCommand.ExecuteReader();
			if (myStringsDBReadReader.Read())
			{
				string var_name = (myStringsDBReadReader.IsDBNull(0) ? "" : myStringsDBReadReader.GetString(0));
				if (var_name == "pre_term_infant")
				{
					AgeRange_ASTM = "P";
				}
				else if (var_name == "term_infant")
				{
					AgeRange_ASTM = "F";
				}
			}
			myStringsDBReadReader.Close();
		}
	}

	private void HeightDMLtoASTM()
	{
		string[] HeightParts = Height_DML.Split(',');
		Height_ASTM = HeightParts[0] + "^";
		if (HeightParts.Length > 1)
		{
			if (HeightParts[1] == "CMS")
			{
				HeightParts[1] = "cm";
			}
			else if (HeightParts[1] == "INS")
			{
				HeightParts[1] = "in";
			}
			Height_ASTM += HeightParts[1];
		}
	}

	private void WeightDMLtoASTM()
	{
		string[] WeightParts = Weight_DML.Split(',');
		Weight_ASTM = WeightParts[0] + "^";
		if (WeightParts.Length > 1)
		{
			if (WeightParts[1] == "KGS")
			{
				WeightParts[1] = "Kg";
			}
			else if (WeightParts[1] == "LBS")
			{
				WeightParts[1] = "lb";
			}
			Weight_ASTM += WeightParts[1];
		}
	}

	private string FindTextByChildNode(XmlElement root, string parent, string name, string attribute, string aValue)
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

	private void GetFacilNum()
	{
		m_facil_num = "";
		try
		{
			myDBReadCommand.CommandText = string.Format("select loc_num from DBA.inst_locations where loc_name = '{0}' and level_num = 1", m_facility.Replace("'", "''"));
			if (m_NNBase.m_isLogging)
			{
				log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
			}
			OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
			if (myReader.Read() && !myReader.IsDBNull(0))
			{
				m_facil_num = myReader.GetString(0);
			}
			myReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up facility", "GetFacilNum");
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up facility", "GetFacilNum");
		}
	}

	private bool GetPatientInfo(string patient_id_field)
	{
		myDBReadCommand.CommandText = "";
		PatientRec m_Patient = null;
		bool bOK = false;
		PatientList m_PatientList = new PatientList();
		GetFacilNum();
		if (m_facil_num.Length > 0)
		{
			if (patient_id_field == "PATID" && enterprise_id.Length > 0)
			{
				patient_id_field = "patient_id";
			}
			else if (patient_id_field == "MRN" && medrec_num.Length > 0)
			{
				patient_id_field = "medrec_num";
			}
			else
			{
				if (!(patient_id_field == "ACCT") || account_num.Length <= 0)
				{
					return false;
				}
				patient_id_field = "account_num";
			}
			bool bByFacility = string.IsNullOrEmpty(m_loc_num);
			m_PatientList.GetPatientIDs(m_NNBase, ref myDBReadCommand, m_facil_num, bByFacility, m_loc_num, patient_id_field, ref enterprise_id, ref medrec_num, ref account_num, bRetrieveDetails: true, ref m_Patient);
			if (m_Patient != null)
			{
				int pPatient = m_PatientList.First();
				if (pPatient >= 0)
				{
					m_Patient = (PatientRec)m_PatientList.m_Array[pPatient];
					if (m_Patient.m_PatientID.Length > 0 && enterprise_id.Length == 0)
					{
						enterprise_id = m_Patient.m_PatientID;
					}
					if (m_Patient.m_medrecnum.Length > 0 && medrec_num.Length == 0)
					{
						medrec_num = m_Patient.m_medrecnum;
					}
					if (m_Patient.m_FirstName.Length > 0 && first_name.Length == 0)
					{
						first_name = m_Patient.m_FirstName.Substring(0, Math.Min(m_Patient.m_FirstName.Length, 16));
					}
					if (m_Patient.m_LastName.Length > 0 && last_name.Length == 0)
					{
						last_name = m_Patient.m_LastName.Substring(0, Math.Min(m_Patient.m_LastName.Length, 16));
					}
					if (m_Patient.m_MiddleName.Length > 0 && middle_name.Length == 0)
					{
						middle_name = m_Patient.m_MiddleName.Substring(0, Math.Min(m_Patient.m_MiddleName.Length, 16));
					}
					if (m_Patient.m_prefix.Length > 0 && prefix.Length == 0)
					{
						prefix = m_Patient.m_prefix;
					}
					if (m_Patient.m_suffix.Length > 0 && suffix.Length == 0)
					{
						suffix = m_Patient.m_suffix;
					}
					if (m_Patient.m_birthdate.Year > 1800 && BirthDate.Year < 1800)
					{
						BirthDate = m_Patient.m_birthdate;
					}
					if (m_Patient.m_Sex.Length > 0 && Sex.Length == 0)
					{
						Sex = m_Patient.m_Sex;
					}
				}
				bOK = true;
			}
		}
		return bOK;
	}

	private bool LookupLocNum()
	{
		bool bOK = false;
		OdbcDataReader myDBReadReader = null;
		try
		{
			myDBReadCommand.CommandText = string.Format("SELECT loc_num FROM DBA.inst_locations WHERE loc_name = '{0}' AND parent = ( select loc_num from DBA.inst_locations where loc_name = '{1}' and level_num = 1 )", m_location.Replace("'", "''"), m_facility.Replace("'", "''"));
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			if (myDBReadReader.Read())
			{
				m_loc_num = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
				bOK = true;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "finding location", "LookupLocNum");
		}
		catch (Exception e2)
		{
			handleException(e2, "finding location", "LookupLocNum");
		}
		return bOK;
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
					m_isReading = false;
				}
				int bytesRead = m_networkStream.Read(m_inbuffer, 0, m_inbuffer.Length);
				if (bytesRead > 0)
				{
					lock (this)
					{
						if (!(bgetout = m_isShutDown | m_isShuttingDown))
						{
							m_isReading = true;
						}
					}
					if (bgetout)
					{
						break;
					}
					if (m_NNBase.m_isLogging)
					{
						string sRead = BytesToLogString(m_inbuffer, bytesRead);
						log(sRead, isXml: false, "RTMLIS_ASTM_ReadPort");
					}
					lock (myState)
					{
						myState.m_sCurMsg += Encoding.UTF8.GetString(m_inbuffer, 0, bytesRead);
						for (int i = 0; i < bytesRead; i++)
						{
							myState.b = m_inbuffer[i];
							myState.bGotChar = true;
							ProcessState();
						}
						myState.bGotChar = false;
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
		finally
		{
			lock (this)
			{
				m_isReading = false;
			}
		}
	}

	private string ByteToLogString(byte m_byte)
	{
		return BytesToLogString(new byte[2] { m_byte, 0 }, 1);
	}

	private string BytesToLogString(byte[] m_buffer, int buflen)
	{
		string sIn = Encoding.UTF8.GetString(m_buffer, 0, buflen);
		string sOut = "";
		char c = '\0';
		for (int i = 0; i < sIn.Length; i++)
		{
			c = sIn[i];
			sOut = ((c >= ' ') ? (sOut + c) : (c switch
			{
				'\0' => sOut + "{NUL}", 
				'\u0001' => sOut + "{SOH}", 
				'\u0002' => sOut + "{STX}", 
				'\u0003' => sOut + "{ETX}", 
				'\u0004' => sOut + "{EOT}", 
				'\u0005' => sOut + "{ENQ}", 
				'\u0006' => sOut + "{ACK}", 
				'\t' => sOut + "{HT}", 
				'\n' => sOut + "{LF}", 
				'\r' => sOut + "{CR}", 
				'\u0015' => sOut + "{NAK}", 
				_ => sOut + "{" + ((byte)c).ToString("D2") + "}", 
			}));
		}
		return sOut;
	}

	private void ProcessState()
	{
		try
		{
			if (myState.state < 8)
			{
				ProcessReadState();
				return;
			}
			if (myState.state > 8)
			{
				ProcessWriteState();
				return;
			}
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("ASTM state machine: invalid state", "C", "processing ASTM state", "ProcessState", "the current state is " + Convert.ToString(myState.state));
			ShutDown("Invalid state", "Protocol", bExit: true);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessState");
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "processing current state", "ProcessState");
			}
		}
	}

	private void ProcessReadState()
	{
		switch (myState.state)
		{
		case 0:
			if (myState.bGotChar)
			{
				if (myState.b == 5)
				{
					bPrevBad = false;
					SendByte(6);
					myState.state = 1;
					myState.m_sCurMsg = "";
					m_nCurFrame = 1;
					m_dwTimeoutStart = dwCurTime();
				}
				else if (myState.b != 4)
				{
					bPrevBad = true;
					string logstring2 = ByteToLogString(myState.b);
					m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
					m_NNBase.ReportErrorDB("ASTM state machine: received unexpected character " + logstring2 + " in idle state", "C", "processing ASTM state", "ProcessState", "");
					ShutDown("Unexpected character in idle state", "Protocol", bExit: true);
				}
			}
			else if (myState.bFullMessage && (m_dwSendTimeoutStart == 0 || m_dwSendTimeoutStart + 20000 <= dwCurTime()))
			{
				SendByte(5);
				m_dwSendTimeoutStart = dwCurTime();
				myState.state = 9;
			}
			break;
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		{
			if (!myState.bGotChar)
			{
				if (m_dwTimeoutStart + 30000 <= dwCurTime())
				{
					m_NNBase.ReportErrorDB("Timeout waiting for message frame", "E", "waiting for message frame", "ProcessReadState", "");
					myState.state = 0;
				}
				break;
			}
			string pszExpecting = "";
			switch (myState.state)
			{
			case 1:
				m_FrameChecksum = 0;
				break;
			default:
				m_FrameChecksum += myState.b;
				break;
			case 4:
			case 5:
			case 6:
			case 7:
				break;
			}
			switch (myState.state)
			{
			case 1:
				if (myState.b == 2)
				{
					bPrevBad = false;
					myState.state = 2;
				}
				else if (myState.b == 4)
				{
					ParseCurrentMessage();
					myState.state = 0;
				}
				else
				{
					pszExpecting = "{STX}";
				}
				break;
			case 2:
				if (myState.b >= 48 && myState.b <= 55)
				{
					byte nFrame = (byte)(myState.b - Convert.ToByte('0'));
					if ((m_nCurFrame + 1) % 8 == nFrame)
					{
						bPrevBad = false;
						m_nCurFrame = nFrame;
						myState.state = 3;
					}
					else if (m_nCurFrame == nFrame)
					{
						bPrevBad = false;
						myState.state = 3;
					}
					else
					{
						pszExpecting = "new frame";
						myState.state = 1;
						SendByte(21);
						m_dwTimeoutStart = dwCurTime();
					}
				}
				else
				{
					pszExpecting = "frame number";
				}
				break;
			case 3:
				bPrevBad = false;
				switch (myState.b)
				{
				case 23:
					myState.state = 4;
					m_bLastFrame = false;
					break;
				case 3:
					myState.state = 4;
					m_bLastFrame = true;
					break;
				}
				break;
			case 4:
				bPrevBad = false;
				m_szChecksum[0] = myState.b;
				myState.state = 5;
				break;
			case 5:
				bPrevBad = false;
				m_szChecksum[1] = myState.b;
				m_szChecksum[2] = 0;
				myState.state = 6;
				break;
			case 6:
				if (myState.b == 13)
				{
					bPrevBad = false;
					myState.state = 7;
					break;
				}
				pszExpecting = "{CR}";
				SendByte(21);
				myState.state = 1;
				m_dwTimeoutStart = dwCurTime();
				break;
			case 7:
				if (myState.b == 10)
				{
					bPrevBad = false;
					string s = m_FrameChecksum.ToString("X2");
					if ((byte)s[0] != m_szChecksum[0] || (byte)s[1] != m_szChecksum[1])
					{
						SendByte(21);
						m_dwTimeoutStart = dwCurTime();
						myState.state = 1;
						m_NNBase.ReportErrorDB("Frame checksum error", "E", "processing message frame", "ProcessReadState", "");
						break;
					}
					SendByte(6);
					m_dwTimeoutStart = dwCurTime();
					if (m_bLastFrame)
					{
						myState.state = 1;
					}
					else
					{
						myState.state = 1;
					}
				}
				else
				{
					pszExpecting = "{LF}";
					myState.state = 1;
					SendByte(21);
					m_dwTimeoutStart = dwCurTime();
				}
				break;
			}
			if (pszExpecting.Length > 0)
			{
				if (!bPrevBad)
				{
					bPrevBad = true;
				}
				string logstring = ByteToLogString(myState.b);
				m_NNBase.ReportErrorDB("Received " + logstring + ", was expecting " + pszExpecting, "E", "processing message frame", "ProcessReadState", "");
			}
			break;
		}
		default:
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("ASTM state machine: invalid state", "C", "processing ASTM state", "ProcessState", "the current state is " + Convert.ToString(myState.state));
			ShutDown("Invalid state", "Protocol", bExit: true);
			break;
		}
	}

	private void ProcessWriteState()
	{
		try
		{
			switch (myState.state)
			{
			case 9:
				if (myState.bGotChar)
				{
					switch (myState.b)
					{
					case 6:
						myState.state = 10;
						m_nCurFrameSend = 1;
						m_sendRetries = 0;
						break;
					case 21:
						m_dwSendTimeoutStart = dwCurTime() - 10000;
						myState.state = 0;
						break;
					case 5:
						m_dwSendTimeoutStart = dwCurTime();
						myState.state = 0;
						break;
					default:
						if (!bPrevBad)
						{
							bPrevBad = true;
							m_NNBase.ReportErrorDB("received unexpected data", "E", "processing input", "ProcessWriteState", "");
						}
						break;
					}
				}
				else if (m_dwSendTimeoutStart + 15000 < dwCurTime())
				{
					m_NNBase.ReportErrorDB("send failure: timeout on waiting-to-send acknowledgement", "E", "waiting for ack", "ProcessWriteState", "");
					if (!m_needQuery)
					{
						TerminateResultSend(bRetryLastMessage: true);
					}
					else
					{
						TerminateQuerySend(bRetryLastMessage: true);
					}
				}
				break;
			case 10:
			{
				if (!m_needQuery && myState.resultMessage.Count == 0)
				{
					TerminateResultSend(bRetryLastMessage: false);
					break;
				}
				if (m_needQuery && myState.queryMessage.Count == 0)
				{
					TerminateQuerySend(bRetryLastMessage: false);
					break;
				}
				string sSegment = "";
				sSegment = (m_needQuery ? ((string)myState.queryMessage[0]) : ((string)myState.resultMessage[0]));
				int inlen = StringToUTF8(sSegment, ref m_workbuffer);
				int packetlen = AddMessageFraming(m_workbuffer, inlen, ref m_outbuffer);
				SendBytes(m_outbuffer, packetlen);
				myState.state = 11;
				m_dwSendTimeoutStart = dwCurTime();
				break;
			}
			case 11:
				if (myState.bGotChar)
				{
					switch (myState.b)
					{
					case 4:
					case 6:
						myState.state = 10;
						m_sendRetries = 0;
						if (!m_needQuery)
						{
							myState.resultMessage.RemoveAt(0);
						}
						else
						{
							myState.queryMessage.RemoveAt(0);
						}
						m_nCurFrameSend = (m_nCurFrameSend + 1) % 8;
						myState.state = 10;
						break;
					case 21:
						m_sendRetries++;
						if (m_sendRetries >= 6)
						{
							m_NNBase.ReportErrorDB("send failure: receiver refused data", "E", "receiving NAK", "ProcessWriteState", "");
							if (!m_needQuery)
							{
								TerminateResultSend(bRetryLastMessage: true);
							}
							else
							{
								TerminateQuerySend(bRetryLastMessage: true);
							}
						}
						else
						{
							myState.state = 10;
							m_dwSendTimeoutStart = 0L;
						}
						break;
					default:
						if (!bPrevBad)
						{
							bPrevBad = true;
							m_NNBase.ReportErrorDB("received unexpected data", "E", "waiting for ACK", "ProcessWriteState", "");
						}
						break;
					}
				}
				else if (m_dwSendTimeoutStart + 15000 <= dwCurTime())
				{
					m_NNBase.ReportErrorDB("send failure: timeout on packet acknowledgement", "E", "waiting for ACK", "ProcessWriteState", "");
					if (!m_needQuery)
					{
						TerminateResultSend(bRetryLastMessage: true);
					}
					else
					{
						TerminateQuerySend(bRetryLastMessage: true);
					}
				}
				break;
			default:
				m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
				m_NNBase.ReportErrorDB("ASTM state machine: invalid state", "C", "processing ASTM state", "ProcessState", "the current state is " + Convert.ToString(myState.state));
				ShutDown("Invalid state", "Protocol", bExit: true);
				break;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessWriteState");
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "processing current state", "ProcessWriteState");
			}
		}
	}

	private void ParseCurrentMessage()
	{
		if (myState.m_sCurMsg.Length < 3)
		{
			return;
		}
		myState.recordList = myState.m_sCurMsg.Split('\n');
		for (int i = 0; i < myState.recordList.Length; i++)
		{
			string msg = myState.recordList[i];
			int j = msg.IndexOf('\r');
			if (j > 0)
			{
				msg = msg.Substring(0, j);
			}
			if (msg.Length > 3)
			{
				switch (msg.Substring(2, 1))
				{
				case "H":
					ProcessidHeader(msg);
					break;
				case "P":
					ProcessidPatient(msg);
					break;
				case "O":
					ProcessidTestOrder(msg);
					break;
				case "L":
					ProcessidTerminator(msg);
					break;
				}
			}
		}
	}

	private void ProcessidHeader(string myrecord)
	{
	}

	private void ProcessidPatient(string myrecord)
	{
		string[] myfield = myrecord.Split('|');
		response_patid[0] = myfield[2];
		if (myfield.Length <= 3)
		{
			return;
		}
		response_patid[1] = myfield[3];
		if (myfield.Length <= 4)
		{
			return;
		}
		response_patid[2] = myfield[4];
		if (myfield.Length <= 5)
		{
			return;
		}
		string[] patname = myfield[5].Split('^');
		response_patlastname = "";
		response_patfirstname = "";
		if (patname.Length > 0)
		{
			response_patlastname = patname[0];
			if (patname.Length > 1)
			{
				response_patfirstname = patname[1];
			}
		}
		if (myfield.Length > 7)
		{
			response_birthdate = myfield[7];
		}
	}

	private void ProcessidTestOrder(string myrecord)
	{
		try
		{
			if (m_sample_key_num.Length > 0 && order_id.Length == 0)
			{
				sent_patid[0] = medrec_num;
				sent_patid[1] = enterprise_id;
				sent_patid[2] = account_num;
				int i = 0;
				int j = 0;
				bool bPatIDFound = false;
				for (i = 0; i < 3; i++)
				{
					if (bPatIDFound)
					{
						break;
					}
					for (j = 0; j < 3; j++)
					{
						if (bPatIDFound)
						{
							break;
						}
						if (sent_patid[i].Length > 0 && response_patid[j].Length > 0 && sent_patid[i] == response_patid[j])
						{
							bPatIDFound = true;
						}
					}
				}
				bool bNameFound = false;
				if (last_name.Length > 0 && Comp.Compare(last_name, response_patlastname, CompOpt) == 0 && first_name.Length > 0 && Comp.Compare(first_name, response_patfirstname, CompOpt) == 0)
				{
					bNameFound = true;
				}
				bool bBirthDateFound = true;
				string sent_birthdate = "";
				if (BirthDate.Year > 1800)
				{
					sent_birthdate = BirthDate.Year.ToString("D4") + BirthDate.Month.ToString("D2") + BirthDate.Day.ToString("D2");
					if (response_birthdate.Length >= 8 && sent_birthdate != response_birthdate.Substring(0, 8))
					{
						bBirthDateFound = false;
					}
				}
				if ((bPatIDFound || bNameFound) && bBirthDateFound)
				{
					string[] myfield = myrecord.Split('|');
					myDBWriteCommand.CommandText = "update dba.samples set accession_num = '" + myfield[2] + "' where sample_key_num = '" + m_sample_key_num + "'";
					if (m_NNBase.m_isLogging)
					{
						log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
					}
					myDBWriteCommand.ExecuteNonQuery();
				}
				else
				{
					if (m_NNBase.m_isLogging)
					{
						log("Demographics of query and received order do not match", isXml: false, "RTMLIS_ASTM");
					}
					MarkSampleAsTransmitted("Protocol");
				}
			}
			else if (m_NNBase.m_isLogging)
			{
				log("Unsolicited order received and ignored", isXml: false, "RTMLIS_ASTM");
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "updating sample with accession number", "ProcessidOrder");
		}
		catch (Exception e2)
		{
			handleException(e2, "updating sample with accession number", "ProcessidOrder");
		}
	}

	private void ProcessidTerminator(string myrecord)
	{
		myState.bWaitingForQueryResponse = false;
	}

	private void TerminateQuerySend(bool bRetryLastMessage)
	{
		SendByte(4);
		myState.queryMessage.Clear();
		myState.queryTestList = "";
		myState.bFullMessage = false;
		myState.bRetryLastQueryMessage = bRetryLastMessage;
		myState.bSendingMessage = false;
		myState.bWaitingForQueryResponse = !bRetryLastMessage;
		myState.state = 0;
		m_dwSendTimeoutStart = 0L;
	}

	private void TerminateResultSend(bool bRetryLastMessage)
	{
		SendByte(4);
		myState.resultMessage.Clear();
		myState.bFullMessage = false;
		myState.bRetryLastResultMessage = bRetryLastMessage;
		myState.bSendingMessage = false;
		myState.state = 0;
		m_dwSendTimeoutStart = 0L;
		if (!bRetryLastMessage)
		{
			MarkSampleAsTransmitted("Timer");
		}
	}

	private long dwCurTime()
	{
		return DateTime.Now.Ticks / 10000;
	}

	private string GenASTMHeader()
	{
		string sHeader = "";
		try
		{
			DateTime myNow = DateTime.Now;
			string SendingApplication = GetSenderStringToLIS(inst_class, m_inst_ver, m_serial_id, m_inst_type);
			string SendingTime = myNow.Year.ToString("D4") + myNow.Month.ToString("D2") + myNow.Day.ToString("D2") + myNow.Hour.ToString("D2") + myNow.Minute.ToString("D2") + myNow.Second.ToString("D2");
			sHeader = $"H|\\^&|||{SendingApplication}||||||||LIS2-A2|{SendingTime}";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Header", "GenASTMHeader");
		}
		return sHeader;
	}

	private string GenASTMQuery()
	{
		string sQuery = "";
		try
		{
			string szTimeDrawn = "";
			szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
			sQuery = $"Q|1|^^{medrec_num}^{enterprise_id}^{account_num}||{myState.queryTestList}||||||{operator_id}^{szTimeDrawn}|||||||||||||||O";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Query Record", "GenASTMQuery");
		}
		return sQuery;
	}

	private string GenASTMPatient()
	{
		string sPatient = "";
		try
		{
			sPatient = $"P|1|{medrec_num}|{enterprise_id}|{account_num}|";
			if (last_name.Length > 0)
			{
				sPatient += last_name;
			}
			sPatient += "^";
			if (first_name.Length > 0)
			{
				sPatient += first_name;
			}
			sPatient += "^";
			if (middle_name.Length > 0)
			{
				sPatient += middle_name;
			}
			sPatient += "^";
			if (suffix.Length > 0)
			{
				sPatient += suffix;
			}
			sPatient += "^";
			if (prefix.Length > 0)
			{
				sPatient += prefix;
			}
			sPatient += "||";
			if (BirthDate.Year > 1800)
			{
				sPatient = sPatient + BirthDate.Year.ToString("D4") + BirthDate.Month.ToString("D2") + BirthDate.Day.ToString("D2");
			}
			sPatient += "|";
			if (Sex.Length > 0)
			{
				sPatient += Sex;
			}
			sPatient += "|";
			if (Race_ASTM.Length > 0)
			{
				sPatient += Race_ASTM;
			}
			sPatient += "||||";
			if (Physician.Length > 0)
			{
				sPatient += Physician;
			}
			sPatient += "||";
			if (AgeRange_ASTM.Length > 0)
			{
				sPatient += AgeRange_ASTM;
			}
			sPatient += "|";
			if (Height_ASTM.Length > 0)
			{
				sPatient += Height_ASTM;
			}
			sPatient += "|";
			if (Weight_ASTM.Length > 0)
			{
				sPatient += Weight_ASTM;
			}
			sPatient += "|";
			if (Diagnosis.Length > 0)
			{
				sPatient += Diagnosis;
			}
			sPatient += "||||||";
			if (PatientClass.Length > 0)
			{
				sPatient += PatientClass;
			}
			sPatient += "^";
			if (PatientType.Length > 0)
			{
				sPatient += PatientType;
			}
			sPatient += "|";
			if (m_location.Length > 0)
			{
				sPatient += m_location;
				sPatient += "^";
				if (room_num.Length > 0)
				{
					sPatient += room_num;
				}
				sPatient += "^";
				if (bed_num.Length > 0)
				{
					sPatient += bed_num;
				}
				sPatient += "^";
				if (m_facility.Length > 0)
				{
					sPatient += m_facility;
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Patient Record", "GenASTMPatient");
		}
		return sPatient;
	}

	private string GenASTMOrder()
	{
		string sOrder = "";
		string szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
		string sampNum = szTimeDrawn;
		string specid = "";
		if (isQC)
		{
			_ = control_lot_num.Length;
			specid = control_lot_num + "^" + control_lot_level + "^" + control_internal_external;
		}
		else
		{
			specid = order_id;
		}
		try
		{
			sOrder = $"O|1|{specid}|{sampNum}||||{szTimeDrawn}||||||||{sample_type_ASTM}|||||{strip_lot_num}|||||{status_ASTM}";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Order Record", "GenASTMOrder");
		}
		return sOrder;
	}

	private string GenASTMResult(int nParm)
	{
		string sResult = "";
		try
		{
			string szRange = normal_limit_lo + " to " + normal_limit_hi;
			string szTimeDrawn = "";
			szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
			string szResultValue = "";
			szResultValue = ((result_str_value.Length == 0 || result_str_value == "null") ? "ERR" : ((!bSampleError) ? result_str_value : ("?" + result_str_value)));
			sResult = string.Format("R|{0}|{1}^{2}^{3}^{4}^{5}|{6}|{7}|{8}|{9}||{10}||{11}^{12}|{13}||{14}", nParm.ToString("D1"), test_code, "", test_code_system, test_transmit_name, method_cd_ASTM, szResultValue, units, szRange, interpretation_ASTM, status_ASTM, operator_id, releaser_id, szTimeDrawn, m_serial_id);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Result Record", "GenASTMResult");
		}
		return sResult;
	}

	private void AddTestToList(ref string InOutStr)
	{
		try
		{
			if (InOutStr.Length > 0)
			{
				InOutStr += "/";
			}
			string teststr = string.Format("{0}^{1}^{2}^{3}^{4}", test_code, "", test_code_system, test_transmit_name, method_cd_ASTM);
			InOutStr += teststr;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Test Code List", "AddTestToList");
		}
	}

	private string GenASTMComment(int nParm, bool bError)
	{
		string sComment = "";
		try
		{
			string sSource = "";
			string sType = "";
			if (bError)
			{
				sSource = "I";
				sType = "I";
			}
			else
			{
				sSource = "P";
				sType = "G";
			}
			sComment = string.Format("C|{0}|{1}|{2}|{3}", nParm.ToString("D1"), sSource, comment_text, sType);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Comment Record", "GenASTMComment");
		}
		return sComment;
	}

	private string GenASTMTerminator()
	{
		string sTerminator = "";
		try
		{
			sTerminator = "L|1|N";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building ASTM Terminator Record", "GenASTMTerminator");
		}
		return sTerminator;
	}

	private void MarkSampleAsTransmitted(string whoFrom)
	{
		try
		{
			myDBWriteCommand.CommandText = $"UPDATE DBA.samples set transmitted_flag = 'T' where sample_key_num = '{m_sample_key_num}'";
			if (m_NNBase.m_isLogging)
			{
				log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
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
					m_NNBase.log("Closing client port " + sport + " for LIS interface", isXml: false, "astm");
				}
				m_clienthandler.socket.Close();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport + " for LIS interface", isXml: false, "astm");
				}
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing networkStream", isXml: false, "astm");
			}
			m_networkStream.Close();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed networkStream", isXml: false, "astm");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing networkStream", isXml: false, "astm");
			}
			m_networkStream.Dispose();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposed networkStream", isXml: false, "astm");
			}
			if (m_clienthandler != null)
			{
				m_clienthandler.RemoveFromList("astm.ShutDown");
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
					log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_ASTM_SQL");
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

	public int StringToUTF8(string input, ref byte[] workbuf)
	{
		int i = 0;
		try
		{
			workbuf = Encoding.UTF8.GetBytes(input);
			i = workbuf.Length;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("StringToUTF8");
		}
		catch (Exception e)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Converting string to UTF8", "StringToUTF8");
			}
		}
		return i;
	}

	public int AddMessageFraming(byte[] workbuf, int inlen, ref byte[] m_outbuf)
	{
		int outlen = 0;
		m_outbuf = new byte[inlen + 64];
		m_outbuf[outlen++] = 2;
		byte cCheckSum = (m_outbuf[outlen++] = (byte)m_nCurFrameSend.ToString("D1")[0]);
		for (int i = 0; i < inlen; i++)
		{
			cCheckSum += (m_outbuf[outlen++] = workbuf[i]);
		}
		string s = ((byte)(cCheckSum + (m_outbuf[outlen++] = 3))).ToString("X2");
		m_outbuf[outlen++] = (byte)s[0];
		m_outbuf[outlen++] = (byte)s[1];
		m_outbuf[outlen++] = 13;
		m_outbuf[outlen++] = 10;
		m_outbuf[outlen] = 0;
		return outlen;
	}

	public int SendBytes(byte[] input, int len)
	{
		int i = 0;
		try
		{
			if (m_NNBase.m_isLogging)
			{
				string LogString = BytesToLogString(input, len);
				log(LogString, isXml: false, "RTMLIS_ASTM_Send   ");
			}
			m_networkStream.Write(input, 0, len);
			i = len;
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
				handleException(e, "sending bytes", "SendBytes");
			}
		}
		return i;
	}

	public int SendByte(byte input)
	{
		int i = 0;
		byte[] inbytes = new byte[2] { input, 0 };
		try
		{
			if (m_NNBase.m_isLogging)
			{
				string LogString = BytesToLogString(inbytes, 1);
				log(LogString, isXml: false, "RTMLIS_ASTM_Send   ");
			}
			m_networkStream.Write(inbytes, 0, 1);
			i = 1;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("SendByte");
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
				handleException(e, "sending byte", "SendByte");
			}
		}
		return i;
	}

	private void ProtocolThread()
	{
		while (!m_isShutDown && !m_isShuttingDown)
		{
			OnReadComplete();
		}
	}

	private string GetSenderStringToLIS(string instClass, string sfVersion, string serialNum, string modelName)
	{
		return "NOVANET^" + instClass + "^" + sfVersion + "^" + serialNum + "^" + modelName;
	}
}
