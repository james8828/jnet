using System;
using System.Collections.Generic;
using System.Data.Odbc;
using System.Globalization;
using System.IO;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Timers;
using System.Xml;
using Microsoft.Win32;
using NNClass;
using Patient;

namespace RTMLIS;

internal class HL7Protocol : Protocol
{
	private const char ASCII_VT = '\v';

	private const char ASCII_FS = '\u001c';

	private const char ASCII_CR = '\r';

	private const char ASCII_STX = '\u0002';

	private const char ASCII_ETX = '\u0003';

	private NNBase m_NNBase = new NNBase();

	protected CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	protected CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private char MessageBeginChar = '\v';

	private char MessageEndChar = '\u001c';

	protected uint iAccountField = 18u;

	protected uint iAccountComponent = 1u;

	protected string sAccountSegment = "PID";

	private string sMale = "M";

	private string sFemale = "F";

	private int m_loc_port;

	private byte[] m_inbuffer = new byte[32768];

	private byte[] m_outbuffer;

	private string m_message = "";

	private XmlDocument m_doc;

	private XmlElement svc;

	private XmlElement elem;

	private char m_chLast;

	private string m_portType = "";

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

	private DateTime orderDateTime = DateTime.MinValue;

	private string m_inst_ver = "";

	private string operator_id = "";

	private string releaser_id = "";

	private string operator_last_name = "";

	private string operator_first_name = "";

	private string strip_lot_num = "";

	private string m_order_id = "";

	private string response_order_id = "";

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

	private DateTime ExpDate = DateTime.MaxValue;

	private string sBirthDate = "";

	private string Sex = "";

	private string Physician = "";

	private string room_num = "";

	private string bed_num = "";

	private string Race_DML = "";

	private string Race_HL7 = "";

	private string AgeRange_DML = "";

	private string Weight_DML = "";

	private string Weight_DML_value = "";

	private string Weight_DML_units = "";

	private string Weight_HL7_value = "";

	private string Weight_HL7_units = "";

	private string Height_DML = "";

	private string Height_DML_value = "";

	private string Height_DML_units = "";

	private string Height_HL7_value = "";

	private string Height_HL7_units = "";

	private string Diagnosis = "";

	private string PatientClass = "";

	private string PatientType = "";

	private string control_lot_num = "";

	private string control_lot_level = "";

	private string control_internal_external = "";

	private string tgc_flag = "";

	private string sample_type_DML = "";

	private string sample_type_HL7 = "";

	private string role_cd_HL7 = "";

	private string control_type_HL7 = "";

	private string role_cd = "";

	private string m_loc_def_pat_id = "";

	private string status_DML = "A";

	private string status_HL7 = "";

	private string normal_limit_lo = "";

	private string normal_limit_hi = "";

	private string critical_limit_lo = "";

	private string critical_limit_hi = "";

	private string test_code = "";

	private string test_transmit_name = "";

	private string test_code_system = "";

	private string method_cd_DML = "";

	private string method_cd_HL7 = "";

	private string interpretation_DML = "";

	private string interpretation_HL7 = "";

	private string result_str_value = "";

	private string units = "";

	private string comment_text = "";

	private string panel = "";

	private string test_and_panel_list = "";

	private string order_provider = string.Empty;

	private string TestList = "";

	private string m_type_cd = "";

	private bool isQC;

	private bool bSampleError;

	public string inst_class;

	private string m_ReadString = "";

	private HL7Parse segmentparse;

	private string SendingApplication = "";

	private string SendingFacility = "";

	private string ReceivingApplication = "";

	private string ReceivingFacility = "";

	private string MSHTimeStamp = "";

	private string MessageType = "";

	private string MessageSubType = "";

	private string MessageControlID = "";

	private string ReceivedMessageControlID = "";

	private string ProcessingID = "";

	private int m_imsgid;

	private int MaxMsgId = 9;

	private int[] MsgId = new int[10];

	private string MessageBeingRespondedTo = "";

	private string m_ack_MessageControlID = "";

	private NovaHL7UUID m_NovaHL7UUID = new NovaHL7UUID();

	private string resultMessage = "";

	private string queryMessage = "";

	private string sSupportedTransactions = "ACK^R01,ORF^R04, OSQ^Q06,OSR^Q06";

	private NetworkStream m_networkStream;

	private RTMLIS m_parent;

	private Port.AsynchNetworkServer.ClientHandler m_clienthandler;

	private int m_port_num;

	private System.Timers.Timer cmTimer;

	private string m_sample_key_num;

	private string m_order_key_num;

	private string[] order_key_num = new string[10];

	private string[] order_id = new string[10];

	private string[] facility = new string[10];

	private string MBRT_order_key_num = "";

	private string MBRT_order_id = "";

	private string MBRT_facility = "";

	private bool m_ProtocolSending;

	private bool m_inTimedEvent;

	private bool m_isProcessing;

	private DateTime m_last_eot_update_time = DateTime.Now;

	private bool m_bConnected;

	private string[] m_LastMessageSent = new string[10];

	private string[] m_LastMessageSentControlID = new string[10];

	private Dictionary<string, int> m_MessageControlList = new Dictionary<string, int>();

	private Dictionary<string, string> m_DeviceTestAliases = new Dictionary<string, string>();

	private string m_SelectSampleKeyNum = "";

	private string m_SelectOrderKeyNum = "";

	private Thread m_ProtocolThread;

	private bool bDBAvailable = true;

	private OdbcConnection myTimerConnection;

	private OdbcCommand myTimerCommand;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

	private OdbcConnection myDBReadConnection;

	private OdbcCommand myDBReadCommand;

	private bool m_more_samples;

	private bool m_more_order_queries;

	private OdbcDataReader myTimerReader;

	protected string BinDir = "C:\\NovaBiomedical\\NovaNet\\Bin";

	private FileStream ConfigReader;

	private byte[] readbuff = new byte[4096];

	private XmlDocument configdoc;

	private bool bHL7_QueryForOrders;

	private bool m_bSamplesDeviceNameColumn;

	private bool m_bInstrumentsTestsLisTestAliasColumn;

	private object m_lockObj = new object();

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

	private string RemoveAsciiControlChar(string inputstring)
	{
		char newChar = ' ';
		return inputstring.Replace('\v', newChar).Replace('\u001c', newChar).Replace('\u0003', newChar)
			.Replace('\u0002', newChar);
	}

	public HL7Protocol(ref NetworkStream networkStream, int port_num, string portType, bool logging, RTMLIS parent, Port.AsynchNetworkServer.ClientHandler clienthandler, int loc_port)
	{
		m_bConnected = true;
		segmentparse = new HL7Parse();
		m_parent = parent;
		m_bSamplesDeviceNameColumn = m_parent.m_bSamplesDeviceNameColumn;
		m_bInstrumentsTestsLisTestAliasColumn = m_parent.m_bInstrumentsTestsLisTestAliasColumn;
		m_clienthandler = clienthandler;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_loc_port = loc_port;
		m_NNBase.m_bLogging = logging;
		m_isShutDown = false;
		m_isShuttingDown = false;
	}

	private void Start()
	{
		m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, "HL7", "RTMLIS", "LIS");
		bDBAvailable = m_NNBase.OpenDBConnection(ref myTimerConnection, ref myTimerCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = m_NNBase.OpenDBConnection(ref myDBWriteConnection, ref myDBWriteCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		bDBAvailable = m_NNBase.OpenDBConnection(ref myDBReadConnection, ref myDBReadCommand, 7);
		if (!bDBAvailable)
		{
			return;
		}
		m_NNBase.CommAudit(10, "Connect", "");
		if (!m_NNBase.bDBAvailable)
		{
			return;
		}
		string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
		myDBWriteCommand.CommandText = sCommand;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
		}
		myDBWriteCommand.ExecuteNonQuery();
		if (m_NNBase.bDBAvailable)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Connection established via local port " + m_loc_port, isXml: false, "RTMLIS");
			}
			LoadConfigFile();
			LoadDeviceTestAliases();
			m_SelectSampleKeyNum = "select top 1 sample_key_num from DBA.samples where transmitted_flag = 'F' and xml_text like '%<SVC>%</SVC>%' and control_type is not null and control_type != '' order by sample_Date";
			m_SelectOrderKeyNum = "select top 1 order_key_num from DBA.orders where transmitted_flag = 'F' order by order_Date";
			m_ProtocolThread = new Thread(ProtocolThread);
			m_ProtocolThread.Start();
			cmTimer = new System.Timers.Timer();
			cmTimer.AutoReset = true;
			cmTimer.Elapsed += OnCmTimedEvent;
			cmTimer.Interval = 100.0;
			cmTimer.Enabled = true;
		}
	}

	private void LoadConfigFile()
	{
		bool bOK = true;
		try
		{
			BinDir = Registry.LocalMachine.OpenSubKey(m_NNBase.REGISTRY_SUBKEY_RTM).GetValue("BinDir").ToString() + "\\";
			ConfigReader = new FileStream(BinDir + "RTMLIS.XML", FileMode.Open);
			if (ConfigReader.CanRead)
			{
				int bytesRead = ConfigReader.Read(readbuff, 0, readbuff.Length);
				if (bytesRead > 0)
				{
					string sRead = Encoding.UTF8.GetString(readbuff, 0, bytesRead);
					configdoc = new XmlDocument();
					configdoc.LoadXml(sRead);
					XmlElement root = configdoc.DocumentElement;
					XmlNodeList nodeList = root.SelectNodes("Setting");
					foreach (XmlNode varval in nodeList)
					{
						XmlElement elem = (XmlElement)varval;
						string variable = elem.GetAttribute("Variable");
						string value = elem.GetAttribute("Value");
						if (Comp.Compare(variable, "HL7_QueryForOrders", CompOpt) == 0 && Comp.Compare(value, "true", CompOpt) == 0)
						{
							bHL7_QueryForOrders = true;
						}
						else if (Comp.Compare(variable, "AccountField", CompOpt) == 0)
						{
							sAccountSegment = "PID";
							iAccountField = 18u;
							iAccountComponent = 1u;
							string[] valueparts = value.Split('.');
							int numvalueparts = valueparts.Length;
							int iAcctFldPart = 0;
							if (numvalueparts > 1 && (Comp.Compare(valueparts[0], "PID") == 0 || Comp.Compare(valueparts[0], "PV1") == 0))
							{
								sAccountSegment = valueparts[0];
								iAcctFldPart = 1;
							}
							if (numvalueparts > iAcctFldPart && isNumeric(valueparts[iAcctFldPart], NumberStyles.Integer))
							{
								iAccountField = (uint)Convert.ToInt32(valueparts[iAcctFldPart]);
								bOK = true;
							}
							else
							{
								m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "LoadConfigFile", "");
								bOK = false;
							}
							if (bOK && numvalueparts > iAcctFldPart + 1)
							{
								if (isNumeric(valueparts[iAcctFldPart + 1], NumberStyles.Integer))
								{
									iAccountComponent = (uint)Convert.ToInt32(valueparts[iAcctFldPart + 1]);
									bOK = true;
								}
								else
								{
									m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "LoadConfigFile", "");
									bOK = false;
								}
							}
						}
						else if (Comp.Compare(variable, "Male", CompOpt) == 0)
						{
							sMale = value;
						}
						else if (Comp.Compare(variable, "Female", CompOpt) == 0)
						{
							sFemale = value;
						}
					}
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RTMLIS");
		}
		catch (XmlException e)
		{
			handleXMLException(e, "Loading Config file", "LoadConfigFile");
		}
		catch (FileNotFoundException ex2)
		{
			m_NNBase.ReportErrorDB("No config file found", "E", "Loading Config file", "LoadConfigFile", ex2.Message);
		}
		catch (Exception ex3)
		{
			if (ex3.Message.IndexOf("Could not find file") < 0)
			{
				handleException(ex3, "Loading Config file", "LoadConfigFile");
			}
		}
		if (ConfigReader != null)
		{
			ConfigReader.Close();
		}
	}

	private void LoadDeviceTestAliases()
	{
		string inst_type = "";
		string sample_type_code = "";
		string test_code = "";
		string test_name = "";
		string test_transmit_name = "";
		string lis_test_alias = "";
		string inst_sample_type = "";
		string inst_sample_type_test_alias = "";
		try
		{
			myDBReadCommand.CommandText = "select distinct inst_type, sample_type_code, test_code, test_name, test_transmit_name";
			if (m_bInstrumentsTestsLisTestAliasColumn)
			{
				myDBReadCommand.CommandText += ", lis_test_alias";
			}
			myDBReadCommand.CommandText += " from dba.INSTRUMENTS_TESTS order by inst_type, sample_type_code, test_code";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			OdbcDataReader myDBReadReader = myDBReadCommand.ExecuteReader();
			while (myDBReadReader.Read())
			{
				inst_type = myDBReadReader.GetString(0);
				sample_type_code = myDBReadReader.GetString(1);
				test_code = myDBReadReader.GetString(2);
				test_name = myDBReadReader.GetString(3);
				test_transmit_name = myDBReadReader.GetString(4);
				if (m_bInstrumentsTestsLisTestAliasColumn)
				{
					lis_test_alias = (myDBReadReader.IsDBNull(5) ? "" : myDBReadReader.GetString(5));
				}
				inst_sample_type = inst_type + "^" + sample_type_code + "^";
				inst_sample_type_test_alias = inst_sample_type + test_code;
				if (!m_DeviceTestAliases.ContainsKey(inst_sample_type_test_alias))
				{
					m_DeviceTestAliases.Add(inst_sample_type_test_alias, test_code);
				}
				inst_sample_type_test_alias = inst_sample_type + test_name;
				if (!m_DeviceTestAliases.ContainsKey(inst_sample_type_test_alias))
				{
					m_DeviceTestAliases.Add(inst_sample_type_test_alias, test_code);
				}
				inst_sample_type_test_alias = inst_sample_type + test_transmit_name;
				if (!m_DeviceTestAliases.ContainsKey(inst_sample_type_test_alias))
				{
					m_DeviceTestAliases.Add(inst_sample_type_test_alias, test_code);
				}
				if (lis_test_alias.Length > 0)
				{
					inst_sample_type_test_alias = inst_sample_type + lis_test_alias;
					if (!m_DeviceTestAliases.ContainsKey(inst_sample_type_test_alias))
					{
						m_DeviceTestAliases.Add(inst_sample_type_test_alias, test_code);
					}
				}
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RTMLIS");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "getting device test aliases", "LoadDeviceTestAliases");
		}
		catch (Exception e2)
		{
			handleException(e2, "getting device test aliases", "LoadDeviceTestAliases");
		}
	}

	private void OnCmTimedEvent(object source, ElapsedEventArgs ev)
	{
		bool bgetout = false;
		lock (m_lockObj)
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
		if (m_bConnected)
		{
			m_order_key_num = "";
			m_sample_key_num = "";
			try
			{
				myTimerCommand.CommandText = m_SelectOrderKeyNum;
				myTimerReader = myTimerCommand.ExecuteReader();
				if (m_more_order_queries = myTimerReader.Read())
				{
					m_order_key_num = myTimerReader.GetString(0);
				}
				myTimerReader.Close();
				if (m_order_key_num.Length > 0)
				{
					Console.WriteLine("Next order query is {0}", m_order_key_num);
					if (ProcessOrderQuery())
					{
						m_LastMessageSent[m_imsgid] = "OrderQuery";
						if (!SendOrderQuery())
						{
							m_NNBase.ReportErrorDB("Unable to transmit order query " + m_order_key_num, "C", "sending Order Query", "OnCmTimedEvent", "");
							ShutDown("Unable to transmit order query", "Timer", bExit: true);
						}
						else
						{
							cmTimer.Interval = 100.0;
						}
					}
					else
					{
						if (bDBAvailable)
						{
							MarkOrderQueryAsTransmitted("Timer");
						}
						cmTimer.Interval = 100.0;
						cmTimer.Start();
					}
				}
				else
				{
					Console.WriteLine("No order queries to send");
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "Scanning for untransmitted order queries", "OnCmTimedEvent");
			}
			catch (Exception e2)
			{
				handleException(e2, "Scanning for untransmitted order queries", "OnCmTimedEvent");
			}
			try
			{
				if (m_order_key_num.Length == 0)
				{
					myTimerCommand.CommandText = m_SelectSampleKeyNum;
					myTimerReader = myTimerCommand.ExecuteReader();
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
							if (ProcessSVCNode())
							{
								m_LastMessageSent[m_imsgid] = "Result";
								if (!SendResult())
								{
									m_NNBase.ReportErrorDB("Unable to transmit sample " + m_sample_key_num, "C", "sending Hello", "OnCmTimedEvent", "");
									ShutDown("Unable to transmit sample", "Timer", bExit: true);
								}
								else
								{
									cmTimer.Interval = 200.0;
								}
							}
							else
							{
								if (bDBAvailable)
								{
									MarkSampleAsTransmitted("Timer");
								}
								cmTimer.Interval = 100.0;
								cmTimer.Start();
							}
						}
						else
						{
							if (bDBAvailable)
							{
								MarkSampleAsTransmitted("Timer");
							}
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
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e3)
			{
				handleDBException(e3, "Scanning for untransmitted results", "OnCmTimedEvent");
			}
			catch (Exception e4)
			{
				handleException(e4, "Scanning for untransmitted results", "OnCmTimedEvent");
			}
		}
		lock (this)
		{
			m_inTimedEvent = false;
		}
	}

	private bool ProcessOrderQuery()
	{
		bool ret = false;
		bool readok = false;
		m_inst_type = "";
		m_inst_ver = "";
		m_device_id = "";
		m_serial_id = "";
		m_location = "Unassigned";
		m_facility = "Unassigned";
		m_control_type = "";
		m_loc_num = "";
		m_facil_num = "";
		orderDateTime = DateTime.MinValue;
		operator_id = "";
		releaser_id = "";
		operator_last_name = "";
		operator_first_name = "";
		strip_lot_num = "";
		m_order_id = "";
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
		Race_HL7 = "";
		AgeRange_DML = "";
		Weight_DML = "";
		Weight_HL7_value = "";
		Weight_HL7_units = "";
		Height_DML = "";
		Height_HL7_value = "";
		Height_HL7_units = "";
		Diagnosis = "";
		PatientClass = "";
		PatientType = "";
		Physician = "";
		room_num = "";
		bed_num = "";
		control_lot_num = "";
		control_lot_level = "";
		control_internal_external = "";
		tgc_flag = "";
		sample_type_DML = "";
		sample_type_HL7 = "";
		role_cd = "";
		m_loc_def_pat_id = "";
		status_DML = "A";
		status_HL7 = "";
		lock (m_lockObj)
		{
			try
			{
				myDBReadCommand.CommandText = "select accession_num, order_date, patient_id, medrec_num, account_num, loc_name, fac_name";
				myDBReadCommand.CommandText += $" from DBA.orders where order_key_num = '{m_order_key_num}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
				}
				OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
				readok = myReader.Read();
				if (readok)
				{
					m_order_id = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					orderDateTime = (myReader.IsDBNull(1) ? DateTime.MinValue : myReader.GetDateTime(1));
					enterprise_id = (myReader.IsDBNull(2) ? "" : myReader.GetString(2));
					medrec_num = (myReader.IsDBNull(3) ? "" : myReader.GetString(3));
					account_num = (myReader.IsDBNull(4) ? "" : myReader.GetString(4));
					m_location = (myReader.IsDBNull(5) ? m_location : myReader.GetString(5));
					m_facility = (myReader.IsDBNull(6) ? m_facility : myReader.GetString(6));
					ret = true;
				}
				myReader.Close();
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "reading order query", "ProcessOrderQuery");
			}
			catch (Exception e2)
			{
				handleException(e2, "reading order query", "ProcessOrderQuery");
			}
			if (!readok)
			{
				m_NNBase.ReportErrorDB("Order query not found", "E", "selecting order query", "ProcessOrderQuery", "");
				ShutDown("Order query not found", "Protocol", bExit: true);
			}
			if (ret && m_facility.Length > 0 && m_location.Length > 0)
			{
				m_loc_num = "";
				try
				{
					myDBReadCommand.CommandText = string.Format("SELECT loc_num FROM DBA.inst_locations WHERE loc_name = '{0}' AND parent = ( select loc_num from DBA.inst_locations where loc_name = '{1}' and level_num = 1 )", m_location.Replace("'", "''"), m_facility.Replace("'", "''"));
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
					}
					OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
					if (myReader.Read())
					{
						m_loc_num = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
					}
					myReader.Close();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Timer");
				}
				catch (OdbcException e3)
				{
					handleDBException(e3, "finding location", "ProcessOrderQuery");
				}
				catch (Exception e4)
				{
					handleException(e4, "finding location", "ProcessOrderQuery");
				}
				m_loc_def_pat_id = "";
				if (m_loc_num.Length > 0)
				{
					try
					{
						myDBReadCommand.CommandText = $"SELECT _value FROM DBA.config_data c join DBA.loc_to_config l2c on c.config_num = l2c.config_num where _key = 'PatIdTypeCd*V' and l2c.loc_num = '{m_loc_num}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
						handleDBException(e5, "getting default patient ID type", "ProcessOrderQuery");
					}
					catch (Exception e6)
					{
						handleException(e6, "getting default patient ID type", "ProcessQuery");
					}
				}
			}
		}
		return ret;
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
		m_order_id = "";
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
		Race_HL7 = "";
		AgeRange_DML = "";
		Weight_DML = "";
		Weight_HL7_units = "";
		Weight_HL7_value = "";
		Height_DML = "";
		Height_HL7_value = "";
		Height_HL7_units = "";
		Diagnosis = "";
		PatientClass = "";
		PatientType = "";
		Physician = "";
		room_num = "";
		bed_num = "";
		control_lot_num = "";
		control_lot_level = "";
		control_internal_external = "";
		tgc_flag = "";
		sample_type_DML = "";
		sample_type_HL7 = "";
		role_cd = "";
		m_loc_def_pat_id = "";
		status_DML = "A";
		status_HL7 = "";
		bSampleError = false;
		try
		{
			myDBReadCommand.CommandText = "select control_type, xml_text, accession_num, sample_date, control_lot_num, strip_lot_num, patient_id, medrec_num, account_num, loc_name, fac_name, device_name, device_type, device_serial, device_sw_ver, lot_level, internal_external";
			myDBReadCommand.CommandText += $" from DBA.samples where sample_key_num = '{m_sample_key_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
			}
			OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
			readok = myReader.Read();
			if (readok)
			{
				m_control_type = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
				xml_text = (myReader.IsDBNull(1) ? "" : myReader.GetString(1));
				m_order_id = (myReader.IsDBNull(2) ? "" : myReader.GetString(2));
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
			myDBReadCommand.CommandText = "SELECT inst_class FROM DBA.INSTRUMENT_TYPES WHERE inst_type ='" + m_inst_type + "'";
			inst_class = (string)myDBReadCommand.ExecuteScalar();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "reading sample", "ProcessSample");
		}
		catch (Exception e2)
		{
			handleException(e2, "reading sample", "ProcessSample");
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
			if (isQC)
			{
				ProcessingID = "Q";
			}
			else
			{
				ProcessingID = "P";
			}
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
							m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
					catch (OdbcException e3)
					{
						handleDBException(e3, "getting default patient ID type", "ProcessSample");
					}
					catch (Exception e4)
					{
						handleException(e4, "getting default patient ID type", "ProcessSample");
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
			catch (Exception e5)
			{
				if (!m_isShuttingDown)
				{
					handleException(e5, "parsing xml_text", "ProcessSample");
				}
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
		if (bOK)
		{
			MessageControlID = m_NovaHL7UUID.GetNovaHL7UUID();
			MessageType = "ORU";
			MessageSubType = "R01";
			resultMessage = '\v' + GenHL7Header("Timer");
			m_imsgid++;
			if (m_imsgid > MaxMsgId)
			{
				m_imsgid = 0;
			}
			m_LastMessageSentControlID[m_imsgid] = MessageControlID;
			m_MessageControlList.Add(MessageControlID, m_imsgid);
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
						ExpDate = DateTime.Parse(expdate.Substring(0, 10));
					}
					else
					{
						ExpDate = DateTime.Parse("2007-01-01");
					}
				}
			}
			if (!isQC)
			{
				resultMessage += GenHL7Patient();
				resultMessage += GenHL7PatientVisit();
			}
			else
			{
				resultMessage += GenHL7NonPatient();
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
			SampleTypeDMLtoHL7();
			SampleTypeDMLtoRoleCodeHL7();
			SampleTypeDMLtoControlTypeHL7();
			elem = (XmlElement)svc.SelectSingleNode("ORD/ORD.universal_service_id");
			if (elem != null)
			{
				panel = elem.GetAttribute("V");
			}
			elem = (XmlElement)svc.SelectSingleNode("ORD/ORD.ordering_provider_id");
			if (elem != null)
			{
				order_provider = elem.GetAttribute("V");
			}
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
						m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
				ProcessComments(noteList, bGenComments: false);
				resultMessage += GenHL7Order();
				if (m_order_id.Length > 0 || !bHL7_QueryForOrders)
				{
					ProcessComments(noteList, bGenComments: true);
				}
				bOK = ProcessObservations(nodeList2, bGenResults: true);
			}
			if (bOK)
			{
				resultMessage += GenHL7Specimen();
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
			if (attribute != "ID FLAGS" && attribute != "TGC FLAG" && attribute != "DIAGCODE" && attribute != "SAMPLE ID TYPE")
			{
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
					resultMessage += GenHL7Comment(nParm++, bError);
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
			method_cd_HL7 = "";
			interpretation_DML = "";
			interpretation_HL7 = "";
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
			StatusDMLtoHL7();
			InterpretationDMLtoHL7();
			if (status_HL7.Length > 0 && bGenResults)
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
						m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
				MethodCdDMLtoHL7();
				if (method_cd_HL7.Length > 0)
				{
					if (m_order_id.Length > 0 || !bHL7_QueryForOrders)
					{
						resultMessage += GenHL7Result(nResultParm++);
					}
					else
					{
						AddTestToList(ref TestList);
					}
					if (m_order_id.Length > 0 || !bHL7_QueryForOrders)
					{
						XmlNodeList noteList = obs.SelectNodes("NTE");
						comment_text = "";
						int nCommentParm = 1;
						foreach (XmlNode nte in noteList)
						{
							elem = (XmlElement)nte.FirstChild;
							elem.GetAttribute("V");
							comment_text = elem.InnerText;
							resultMessage += GenHL7Comment(nCommentParm++, bError: false);
						}
					}
				}
				else
				{
					m_NNBase.ReportErrorDB("No translation for method code " + method_cd_DML, "E", "generating result records", "ProcessObservations", "");
					bOK = false;
				}
			}
			if (status_HL7.Length == 0 && bGenResults)
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
					}
					else if (parts[i] == "PAT_ETHNICITY")
					{
						Race_DML = parts[i + 1];
						RaceDMLtoHL7();
					}
					else if (parts[i] == "PAT_WEIGHT")
					{
						Weight_DML = parts[i + 1];
						WeightDMLtoHL7();
					}
					else if (parts[i] == "PAT_HEIGHT")
					{
						Height_DML = parts[i + 1];
						HeightDMLtoHL7();
					}
				}
			}
		}
		if (!string.IsNullOrEmpty(Weight_HL7_value))
		{
			test_code = "3141-9";
			panel = "BODY WEIGHT";
			test_code_system = "LN";
			test_transmit_name = "";
			method_cd_HL7 = "";
			result_str_value = Weight_HL7_value;
			units = Weight_HL7_units;
			normal_limit_lo = "";
			normal_limit_hi = "";
			interpretation_HL7 = "";
			status_HL7 = "";
			resultMessage += GenHL7Result(nResultParm++);
		}
		if (!string.IsNullOrEmpty(Height_HL7_value))
		{
			test_code = "3137-7";
			panel = "BODY HEIGHT";
			test_code_system = "LN";
			test_transmit_name = "";
			method_cd_HL7 = "";
			result_str_value = Height_HL7_value;
			units = Height_HL7_units;
			normal_limit_lo = "";
			normal_limit_hi = "";
			interpretation_HL7 = "";
			status_HL7 = "";
			resultMessage += GenHL7Result(nResultParm++);
		}
		return bOK;
	}

	private void StatusDMLtoHL7()
	{
		if (status_DML == "A")
		{
			status_HL7 = "F";
		}
		else if (status_DML == "D")
		{
			status_HL7 = "X";
		}
		else if (status_DML == "U")
		{
			status_HL7 = "P";
		}
		else if (status_DML == "X")
		{
			status_HL7 = "X";
		}
		else
		{
			status_HL7 = "";
		}
	}

	private void MethodCdDMLtoHL7()
	{
		if (method_cd_DML == "C")
		{
			method_cd_HL7 = "C";
		}
		else if (method_cd_DML == "D")
		{
			method_cd_HL7 = "D";
		}
		else if (method_cd_DML == "E")
		{
			method_cd_HL7 = "";
		}
		else if (method_cd_DML == "I")
		{
			method_cd_HL7 = "E";
		}
		else if (method_cd_DML == "M")
		{
			method_cd_HL7 = "M";
		}
		else if (method_cd_DML == "U")
		{
			method_cd_HL7 = "";
		}
		else
		{
			method_cd_HL7 = "";
		}
	}

	private void InterpretationDMLtoHL7()
	{
		if (interpretation_DML == "L")
		{
			interpretation_HL7 = "L";
		}
		else if (interpretation_DML == "H")
		{
			interpretation_HL7 = "H";
		}
		else if (interpretation_DML == "LL")
		{
			interpretation_HL7 = "LL";
		}
		else if (interpretation_DML == "HH")
		{
			interpretation_HL7 = "HH";
		}
		else if (interpretation_DML == "<")
		{
			interpretation_HL7 = "<";
		}
		else if (interpretation_DML == ">")
		{
			interpretation_HL7 = ">";
		}
		else if (interpretation_DML == "N")
		{
			interpretation_HL7 = "N";
		}
		else if (interpretation_DML == "A")
		{
			interpretation_HL7 = "A";
		}
		else if (interpretation_DML == "AA")
		{
			interpretation_HL7 = "AA";
		}
		else if (interpretation_DML == "null")
		{
			interpretation_HL7 = "null";
		}
		else if (interpretation_DML == "U")
		{
			interpretation_HL7 = "U";
		}
		else if (interpretation_DML == "D")
		{
			interpretation_HL7 = "D";
		}
		else if (interpretation_DML == "B")
		{
			interpretation_HL7 = "B";
		}
		else if (interpretation_DML == "W")
		{
			interpretation_HL7 = "W";
		}
		else if (interpretation_DML == "UC")
		{
			interpretation_HL7 = "UC";
		}
		else if (interpretation_DML == "PC")
		{
			interpretation_HL7 = "PC";
		}
		else if (interpretation_DML == "QC")
		{
			interpretation_HL7 = "QC";
		}
		else if (interpretation_DML == "X")
		{
			interpretation_HL7 = "X";
		}
		else if (interpretation_DML == "PASS")
		{
			interpretation_HL7 = "PASS";
		}
		else if (interpretation_DML == "FAIL")
		{
			interpretation_HL7 = "FAIL";
		}
		else
		{
			interpretation_HL7 = "";
		}
	}

	private void SampleTypeDMLtoHL7()
	{
		sample_type_HL7 = sample_type_DML;
	}

	private void SampleTypeHL7toDML()
	{
		if (sample_type_HL7 == "BLDA")
		{
			sample_type_DML = "BLDA";
		}
		else if (sample_type_HL7 == "BLDC")
		{
			sample_type_DML = "BLDC";
		}
		else if (sample_type_HL7 == "BLDV")
		{
			sample_type_DML = "BLDV";
		}
		else if (sample_type_HL7 == "")
		{
			sample_type_DML = "BLD";
		}
		else
		{
			sample_type_DML = sample_type_HL7;
		}
	}

	private void SampleTypeDMLtoRoleCodeHL7()
	{
		if (sample_type_DML == "BLD")
		{
			role_cd_HL7 = "P";
		}
		else if (sample_type_DML == "BLDA")
		{
			role_cd_HL7 = "P";
		}
		else if (sample_type_DML == "BLDC")
		{
			role_cd_HL7 = "P";
		}
		else if (sample_type_DML == "BLDV")
		{
			role_cd_HL7 = "P";
		}
		else if (sample_type_DML == "Control")
		{
			role_cd_HL7 = "Q";
		}
		else if (sample_type_DML == "LQC")
		{
			role_cd_HL7 = "Q";
		}
		else if (sample_type_DML == "CVR")
		{
			role_cd_HL7 = "F";
		}
		else if (sample_type_DML == "PRF")
		{
			role_cd_HL7 = "F";
		}
		else
		{
			role_cd_HL7 = "";
		}
	}

	private void SampleTypeDMLtoControlTypeHL7()
	{
		if (sample_type_DML == "BLD")
		{
			control_type_HL7 = "";
		}
		else if (sample_type_DML == "BLDA")
		{
			control_type_HL7 = "";
		}
		else if (sample_type_DML == "BLDC")
		{
			control_type_HL7 = "";
		}
		else if (sample_type_DML == "BLDV")
		{
			control_type_HL7 = "";
		}
		else if (sample_type_DML == "Control")
		{
			if (control_internal_external == "Internal")
			{
				control_type_HL7 = "INTQC";
			}
			else if (control_internal_external == "External")
			{
				control_type_HL7 = "EXTQC";
			}
			else
			{
				control_type_HL7 = "";
			}
		}
		else if (sample_type_DML == "LQC")
		{
			if (control_internal_external == "Internal")
			{
				control_type_HL7 = "INTQC";
			}
			else if (control_internal_external == "External")
			{
				control_type_HL7 = "EXTQC";
			}
			else
			{
				control_type_HL7 = "";
			}
		}
		else if (sample_type_DML == "CVR")
		{
			control_type_HL7 = "LIN";
		}
		else if (sample_type_DML == "PRF")
		{
			control_type_HL7 = "PRF";
		}
		else
		{
			control_type_HL7 = "";
		}
	}

	private void RaceDMLtoHL7()
	{
		if (Race_DML == "NB")
		{
			Race_HL7 = "2106-3";
		}
		else if (Race_DML == "B")
		{
			Race_HL7 = "2054-5";
		}
		else if (Race_DML == "JP")
		{
			Race_HL7 = "2028-9";
		}
		else
		{
			Race_HL7 = "2131-1";
		}
	}

	private void RaceHL7toDML()
	{
		if (Race_HL7 == "2106-3")
		{
			Race_DML = "NB";
		}
		else if (Race_HL7 == "2054-5")
		{
			Race_DML = "B";
		}
		else
		{
			Race_DML = "";
		}
	}

	private void HeightDMLtoHL7()
	{
		string[] HeightParts = Height_DML.Split(',');
		Height_HL7_value = HeightParts[0];
		if (HeightParts.Length > 1)
		{
			if (HeightParts[1] == "CMS")
			{
				Height_HL7_units = "cm";
			}
			else if (HeightParts[1] == "INS")
			{
				Height_HL7_units = "in";
			}
		}
	}

	private void HeightHL7toDML()
	{
		Height_DML = (Height_DML_value = Height_HL7_value);
		Height_DML += ",";
		if (Height_HL7_units == "cm")
		{
			Height_DML += (Height_DML_units = "CMS");
		}
		else if (Height_HL7_units == "in")
		{
			Height_DML += (Height_DML_units = "INS");
		}
	}

	private void WeightDMLtoHL7()
	{
		string[] WeightParts = Weight_DML.Split(',');
		Weight_HL7_value = WeightParts[0];
		if (WeightParts.Length > 1)
		{
			if (WeightParts[1] == "KGS")
			{
				Weight_HL7_units = "Kg";
			}
			else if (WeightParts[1] == "LBS")
			{
				Weight_HL7_units = "lb";
			}
		}
	}

	private void WeightHL7toDML()
	{
		Weight_DML = (Weight_DML_value = Weight_HL7_value);
		Weight_DML += ",";
		if (Weight_HL7_units == "Kg")
		{
			Weight_DML += (Weight_DML_units = "KGS");
		}
		else if (Weight_HL7_units == "lb")
		{
			Weight_DML += (Weight_DML_units = "LBS");
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
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
		int MessageCount = 0;
		try
		{
			while (!m_isShutDown && !m_isShuttingDown && MessageCount == 0)
			{
				int bytesRead = m_networkStream.Read(m_inbuffer, 0, m_inbuffer.Length);
				if (bytesRead > 0)
				{
					lock (m_lockObj)
					{
						if (!(m_isShutDown | m_isShuttingDown))
						{
							m_isProcessing = true;
						}
					}
					string sRead = Encoding.UTF8.GetString(m_inbuffer, 0, bytesRead);
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("======Read=========================================================", isXml: false, m_portType);
						m_NNBase.log(sRead, isXml: false, m_portType);
					}
					m_ReadString += sRead;
					int iLast = 0;
					m_message = "";
					m_chLast = '\0';
					for (int i = iLast; i < m_ReadString.Length; i++)
					{
						char mychar = m_ReadString[i];
						if (mychar == MessageBeginChar)
						{
							iLast = i + 1;
						}
						else if (mychar == MessageEndChar)
						{
							m_message = m_ReadString.Substring(iLast, i - iLast);
							MessageCount++;
							Console.WriteLine("{0}Rcvd {1} bytes from {3}:\t{2}", "", bytesRead, m_message, m_portType);
							iLast = i + 1;
							m_chLast = '\0';
							ProcessMessage();
						}
						m_chLast = m_ReadString[i];
					}
					if (iLast < m_ReadString.Length)
					{
						m_ReadString = m_ReadString.Substring(iLast);
					}
					else
					{
						m_ReadString = "";
					}
					lock (m_lockObj)
					{
						if (!(m_isShutDown | m_isShuttingDown))
						{
							m_isProcessing = false;
						}
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

	public void ProcessMessage()
	{
		bool bOK = true;
		try
		{
			if (GetMessageBeingRespondedTo())
			{
				InitMessageFields();
				int iLast = 0;
				m_message = Regex.Replace(m_message, "\\n", "");
				int i = m_message.IndexOf('\r', iLast);
				bool bDone = false;
				while (!bDone && i > 0 && i < m_message.Length)
				{
					if (i > iLast)
					{
						string segment = m_message.Substring(iLast, i - iLast);
						switch (segment.Substring(0, 3))
						{
						case "PID":
							ProcessPatientIdentificationSegment(segment);
							break;
						case "PV1":
							ProcessPatientVisitSegment(segment);
							break;
						case "OBR":
							ProcessObservationReportingSegment(segment);
							break;
						case "OBX":
							ProcessResultSegment(segment);
							break;
						case "DG1":
							ProcessDiagnosisSegment(segment);
							break;
						case "QRD":
							ProcessQueryDefinitionSegment(segment);
							break;
						}
					}
					if (i > 0 && i < m_message.Length)
					{
						iLast = i + 1;
						i = m_message.IndexOf('\r', iLast);
					}
				}
				if (bOK)
				{
					ProcessResonseMessage();
					if (Comp.Compare(MessageType, "ACK", CompOpt) != 0)
					{
						SendAcknowledgeMessage();
					}
				}
			}
			m_message = "";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "processing HL7 message", "ProcessMessage");
		}
		m_parent.m_iNumMessages++;
		m_parent.m_iTotMessages++;
	}

	private string MessageTypeDescription(string msgtype, string msgsubtype)
	{
		string description = "unknown transaction type: " + msgtype + "^" + msgsubtype;
		switch (msgtype)
		{
		case "ACK":
			description = "acknowledgement";
			break;
		case "ORF":
			if (msgsubtype == "R04")
			{
				description = "observation report";
			}
			break;
		case "OSR":
			if (msgsubtype == "Q06")
			{
				description = "order query response";
			}
			break;
		case "OUL":
			if (msgsubtype == "R22")
			{
				description = "observation report";
			}
			break;
		case "ORU":
			if (msgsubtype == "R01")
			{
				description = "observation report";
			}
			break;
		}
		return description;
	}

	private bool GetMessageBeingRespondedTo()
	{
		MessageBeingRespondedTo = "";
		MBRT_order_key_num = "";
		MBRT_order_id = "";
		bool bOK = false;
		int iLast = 0;
		int i = m_message.IndexOf('\r', iLast);
		bool bDone = false;
		while (!bDone && i > 0 && i < m_message.Length)
		{
			if (i > iLast)
			{
				string segment = m_message.Substring(iLast, i - iLast);
				switch (segment.Substring(0, 3))
				{
				case "MSH":
					ProcessMessageHeaderSegment(segment);
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(MessageType + "^" + MessageSubType + " - " + MessageTypeDescription(MessageType, MessageSubType), isXml: false, "GetMessageBeingRespondedTo");
					}
					if (!MessageTypeSupported(MessageType + "^" + MessageSubType))
					{
						m_NNBase.log("Message type is not supported", isXml: false, "GetMessageBeingRespondedTo");
						bDone = true;
					}
					break;
				case "MSA":
					ProcessAcknowledgementSegment(segment);
					bDone = true;
					break;
				default:
					bDone = true;
					break;
				}
			}
			if (i > 0 && i < m_message.Length)
			{
				iLast = i + 1;
				i = m_message.IndexOf('\r', iLast);
			}
		}
		i = 0;
		if (m_ack_MessageControlID.Length > 0 && m_MessageControlList.ContainsKey(m_ack_MessageControlID))
		{
			int arrayIndex = m_MessageControlList[m_ack_MessageControlID];
			MBRT_order_key_num = order_key_num[arrayIndex];
			MessageBeingRespondedTo = m_LastMessageSent[arrayIndex];
			MBRT_order_key_num = order_key_num[arrayIndex];
			MBRT_order_id = order_id[arrayIndex];
			MBRT_facility = facility[arrayIndex];
			bOK = true;
			m_MessageControlList.Remove(m_ack_MessageControlID);
		}
		return bOK;
	}

	private void InitMessageFields()
	{
		if (MessageBeingRespondedTo == "DemogQuery")
		{
			InitPatientVisitFields();
		}
		if (MessageBeingRespondedTo == "DemogQuery" || MessageBeingRespondedTo == "OrderQuery")
		{
			InitPatientFields();
		}
		if (MessageBeingRespondedTo == "OrderQuery")
		{
			InitOrderFields();
		}
	}

	private void InitPatientVisitFields()
	{
		m_facility = "";
		m_location = "";
		m_loc_num = "";
		m_facil_num = "";
		Physician = "";
		room_num = "";
		bed_num = "";
		Weight_DML = "";
		Weight_HL7_value = "";
		Weight_HL7_units = "";
		Height_DML = "";
		Height_HL7_value = "";
		Height_HL7_units = "";
		Diagnosis = "";
		PatientClass = "";
		PatientType = "";
	}

	private void InitPatientFields()
	{
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
		sBirthDate = "";
		Sex = "";
		Race_DML = "";
		Race_HL7 = "";
	}

	private void InitOrderFields()
	{
		response_order_id = "";
		test_and_panel_list = "";
		sample_type_HL7 = "";
		sample_type_DML = "";
	}

	protected bool MessageTypeSupported(string messagetype)
	{
		bool bOK = false;
		string[] sTransTypeArray = sSupportedTransactions.Split(',');
		string[] array = sTransTypeArray;
		foreach (string sTransType in array)
		{
			if (Comp.Compare(sTransType, messagetype, CompOpt) == 0)
			{
				bOK = true;
			}
		}
		if (!bOK)
		{
			m_NNBase.ReportErrorDB("Transaction type not supported: " + messagetype + " - " + MessageTypeDescription(messagetype, MessageSubType), "E", "checking OBR message", "MessageSubTypeSupported", "");
		}
		return bOK;
	}

	private void ProcessResonseMessage()
	{
		switch (MessageType)
		{
		case "ACK":
			ProcessAck();
			break;
		case "ORF":
			if (MessageSubType == "R04")
			{
				ProcessOrderQueryResponse();
			}
			break;
		case "OSR":
			if (MessageSubType == "Q06")
			{
				ProcessOrderQueryResponse();
			}
			break;
		}
	}

	private void ProcessAck()
	{
		if (m_sample_key_num.Length > 0)
		{
			MarkSampleAsTransmitted("Protocol");
		}
		cmTimer.Start();
	}

	private void ProcessOrderQueryResponse()
	{
		lock (m_lockObj)
		{
			try
			{
				if (MBRT_order_key_num.Length > 0 && test_and_panel_list.Length > 0)
				{
					if (response_order_id.Length == 0 || Comp.Compare(response_order_id, MBRT_order_id, CompOpt) == 0)
					{
						string order_order_id = "";
						myDBReadCommand.CommandText = "select accession_num";
						myDBReadCommand.CommandText += $" from DBA.orders where order_key_num = '{MBRT_order_key_num}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
						}
						OdbcDataReader myReader = myDBReadCommand.ExecuteReader();
						if (myReader.Read())
						{
							order_order_id = (myReader.IsDBNull(0) ? "" : myReader.GetString(0));
						}
						myReader.Close();
						if (Comp.Compare(order_order_id, MBRT_order_id, CompOpt) == 0)
						{
							string sql = "update dba.orders set test_and_panel_list = '" + TranslateLISTestAndPanelList(test_and_panel_list) + "'";
							if (enterprise_id.Length > 0)
							{
								sql = sql + ", patient_id = '" + enterprise_id + "'";
							}
							if (medrec_num.Length > 0)
							{
								sql = sql + ", medrec_num = '" + medrec_num + "'";
							}
							if (account_num.Length > 0)
							{
								sql = sql + ", account_num = '" + account_num + "'";
							}
							if (m_location.Length > 0)
							{
								sql = sql + ", loc_name = '" + m_location + "'";
							}
							if (m_facility.Length > 0)
							{
								sql = sql + ", fac_name = '" + m_facility + "'";
							}
							if (Race_DML.Length > 0)
							{
								sql = sql + ", race = '" + Race_DML + "'";
							}
							if (Weight_DML_value.Length > 0)
							{
								sql = sql + ", weight = '" + Weight_DML_value + "'";
								sql = sql + ", weight_units = '" + Weight_DML_units + "'";
							}
							if (Height_DML_value.Length > 0)
							{
								sql = sql + ", height = '" + Height_DML_value + "'";
								sql = sql + ", height_units = '" + Height_DML_units + "'";
							}
							if (Diagnosis.Length > 0)
							{
								sql = sql + ", diagnosis = '" + Diagnosis + "'";
							}
							if (sample_type_DML.Length > 0)
							{
								sql = sql + ", sample_type = '" + sample_type_DML + "'";
							}
							if (Sex.Length > 0)
							{
								sql = sql + ", sex = '" + Sex + "'";
							}
							if (sBirthDate.Length > 0)
							{
								sql = sql + ", birthday = '" + sBirthDate + "'";
							}
							if (first_name.Length > 0)
							{
								sql = sql + ", first_name = '" + first_name + "'";
							}
							if (last_name.Length > 0)
							{
								sql = sql + ", last_name = '" + last_name + "'";
							}
							sql += ", transmitted_flag = 'T'";
							sql = sql + " where order_key_num = '" + MBRT_order_key_num + "'";
							myDBWriteCommand.CommandText = sql;
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
							}
							myDBWriteCommand.ExecuteNonQuery();
						}
						else if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("The order record's order number does not match the query order number. The message will be ignored.", isXml: false, "RTMLIS_HL7");
						}
					}
					else if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("The received order number does not match the query order number. The message will be ignored.", isXml: false, "RTMLIS_HL7");
					}
				}
				else if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("The received order is missing a panel number, and will be ignored.", isXml: false, "RTMLIS_HL7");
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (OdbcException e)
			{
				handleDBException(e, "updating order record", "ProcessOrderQueryResponse");
			}
			catch (Exception e2)
			{
				handleException(e2, "updating order record", "ProcessOrderQueryResponse");
			}
			cmTimer.Start();
		}
	}

	private string TranslateLISTestAndPanelList(string LIS_test_and_panel_list)
	{
		string DeviceTestAndPanelList = "";
		char[] HL7TestAndPanelDelims = new char[4] { '^', '~', '/', ',' };
		char DMLTestAndPanelDelim = '\\';
		try
		{
			string[] TestsAndPanels = LIS_test_and_panel_list.Split(HL7TestAndPanelDelims);
			int i = 0;
			int j = 0;
			for (; i < TestsAndPanels.Length; i++)
			{
				if (TestsAndPanels[i].Length <= 0)
				{
					continue;
				}
				string myTestOrPanelFromLIS = TestsAndPanels[i];
				string myTestOrPanelForDevice = "";
				string myDeviceSampleTypeTestAlias = m_inst_type + "^" + sample_type_DML + "^" + myTestOrPanelFromLIS;
				myTestOrPanelForDevice = ((!m_DeviceTestAliases.ContainsKey(myDeviceSampleTypeTestAlias)) ? TestsAndPanels[i] : m_DeviceTestAliases[myDeviceSampleTypeTestAlias]);
				if (myTestOrPanelForDevice.Length > 0)
				{
					if (j > 0)
					{
						DeviceTestAndPanelList += DMLTestAndPanelDelim;
					}
					DeviceTestAndPanelList += myTestOrPanelForDevice;
					j++;
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "processing test and panel list", "TranslateLISTestAndPanelList");
		}
		return DeviceTestAndPanelList;
	}

	private void ProcessMessageHeaderSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(9);
		segmentparse.curfield = 3u;
		segmentparse.curcomponent = 1u;
		segmentparse.FieldDelim = segment.Substring(3, 1);
		segmentparse.ComponentDelim = segment.Substring(4, 1);
		SendingApplication = GetHL7Field(segmentparse, 3u);
		SendingFacility = GetHL7Field(segmentparse, 4u);
		ReceivingApplication = GetHL7Field(segmentparse, 5u);
		ReceivingFacility = GetHL7Field(segmentparse, 6u);
		MSHTimeStamp = GetHL7Field(segmentparse, 7u);
		MessageType = GetHL7Component(segmentparse, 9u, 1u);
		MessageSubType = GetHL7Component(segmentparse, 9u, 2u);
		ReceivedMessageControlID = GetHL7Field(segmentparse, 10u);
		ProcessingID = GetHL7Component(segmentparse, 11u, 1u);
	}

	private void ProcessAcknowledgementSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		m_type_cd = GetHL7Field(segmentparse, 1u);
		m_ack_MessageControlID = GetHL7Field(segmentparse, 2u);
	}

	private void ProcessPatientIdentificationSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		enterprise_id = GetHL7Component(segmentparse, 2u, 1u).Replace("\"", "");
		medrec_num = GetHL7Component(segmentparse, 3u, 1u).Replace("\"", "");
		last_name = GetHL7Component(segmentparse, 5u, 1u);
		first_name = GetHL7Component(segmentparse, 5u, 2u);
		middle_name = GetHL7Component(segmentparse, 5u, 3u);
		suffix = GetHL7Component(segmentparse, 5u, 4u);
		prefix = GetHL7Component(segmentparse, 5u, 5u);
		sBirthDate = GetHL7Field(segmentparse, 7u);
		Sex = GetHL7Field(segmentparse, 8u).Replace("\"", "");
		if (sMale.Length > 0 && Comp.Compare(Sex, sMale, CompOpt) == 0)
		{
			Sex = "M";
		}
		if (sFemale.Length > 0 && Comp.Compare(Sex, sFemale, CompOpt) == 0)
		{
			Sex = "F";
		}
		Race_HL7 = GetHL7Field(segmentparse, 10u);
		RaceHL7toDML();
		if (Comp.Compare(sAccountSegment, "PID") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			account_num = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "");
		}
	}

	private void ProcessPatientVisitSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		PatientClass = GetHL7Field(segmentparse, 2u);
		m_location = GetHL7Component(segmentparse, 3u, 1u);
		room_num = GetHL7Component(segmentparse, 3u, 2u);
		bed_num = GetHL7Component(segmentparse, 3u, 3u);
		m_facility = GetHL7Component(segmentparse, 3u, 4u);
		Physician = GetHL7Field(segmentparse, 7u);
		PatientType = GetHL7Field(segmentparse, 18u);
		if (Comp.Compare(sAccountSegment, "PV1") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			account_num = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "");
		}
	}

	private void ProcessObservationReportingSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		response_order_id = GetHL7Component(segmentparse, 3u, 1u);
		test_and_panel_list = GetHL7Component(segmentparse, 4u, 1u);
		sample_type_HL7 = GetHL7Component(segmentparse, 15u, 1u);
		SampleTypeHL7toDML();
	}

	private void ProcessResultSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		test_code = GetHL7Component(segmentparse, 3u, 1u);
		string szResultValue = GetHL7Component(segmentparse, 5u, 1u);
		units = GetHL7Component(segmentparse, 6u, 1u);
		if (test_code == "3141-9")
		{
			Weight_HL7_value = szResultValue;
			Weight_HL7_units = units;
			WeightHL7toDML();
		}
		else if (test_code == "3137-7")
		{
			Height_HL7_value = szResultValue;
			Height_HL7_units = units;
			HeightHL7toDML();
		}
	}

	private void ProcessDiagnosisSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		Diagnosis += GetHL7Component(segmentparse, 4u, 1u);
	}

	private void ProcessQueryDefinitionSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		m_order_id = GetHL7Field(segmentparse, 4u);
	}

	private DateTime YMDhms_To_DateTime(ref string YMDhms, string FieldName)
	{
		DateTime RetDateTime = new DateTime(1, 1, 1, 0, 0, 0);
		bool bAddOneDay = false;
		try
		{
			YMDhms = YMDhms.Trim();
			if (YMDhms.Length > 7 && isNumeric(YMDhms, NumberStyles.Integer))
			{
				int year = Convert.ToInt32(YMDhms.Substring(0, 4));
				if (year < 1 || year > DateTime.Now.Year + 1)
				{
					m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
				}
				else
				{
					int month = Convert.ToInt32(YMDhms.Substring(4, 2));
					if (month < 1 || month > 12)
					{
						m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
					}
					else
					{
						int day = Convert.ToInt32(YMDhms.Substring(6, 2));
						if (day < 1 || day > DateTime.DaysInMonth(year, month))
						{
							m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
						}
						else
						{
							int hour = ((YMDhms.Length > 9) ? Convert.ToInt32(YMDhms.Substring(8, 2)) : 0);
							if (hour == 24 && (YMDhms.Length <= 11 || Convert.ToInt32(YMDhms.Substring(10, 2)) == 0))
							{
								hour = 0;
								bAddOneDay = true;
							}
							if (hour > 23)
							{
								m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
							}
							else
							{
								int minute = ((YMDhms.Length > 11) ? Convert.ToInt32(YMDhms.Substring(10, 2)) : 0);
								if (minute > 59)
								{
									m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
								}
								else
								{
									int second = ((YMDhms.Length > 13) ? Convert.ToInt32(YMDhms.Substring(12, 2)) : 0);
									if (second > 59)
									{
										m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
									}
									else
									{
										RetDateTime = new DateTime(year, month, day, hour, minute, second);
										if (bAddOneDay)
										{
											RetDateTime = RetDateTime.AddDays(1.0);
										}
									}
								}
							}
						}
					}
				}
			}
			else if (YMDhms == "\"\"")
			{
				YMDhms = "";
			}
			else if (YMDhms.Length > 0)
			{
				m_NNBase.ReportErrorDB("Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
			}
		}
		catch (Exception e)
		{
			handleException(e, "parsing " + FieldName, "YMDhms_To_DateTime");
		}
		return RetDateTime;
	}

	private bool isNumeric(string val, NumberStyles NumberStyle)
	{
		double result;
		return double.TryParse(val, NumberStyle, CultureInfo.CurrentCulture, out result);
	}

	private int FindHL7Field(HL7Parse myparse, uint fieldnum)
	{
		int iRet = 0;
		bool bGetOut = false;
		while (!bGetOut && myparse.curfield < fieldnum && myparse.remainder.Length > 0)
		{
			int i = myparse.remainder.IndexOf(myparse.FieldDelim);
			if (i >= 0)
			{
				myparse.remainder = myparse.remainder.Substring(i + 1);
				myparse.curfield++;
			}
			else
			{
				bGetOut = true;
			}
		}
		if (myparse.curfield == fieldnum)
		{
			iRet = GetHL7Length(myparse, myparse.FieldDelim, myparse.remainder.Length);
		}
		myparse.fieldlen = iRet;
		myparse.curcomponent = 1u;
		return iRet;
	}

	private int FindHL7Component(HL7Parse myparse, uint componentnum)
	{
		int iRet = 0;
		bool bGetOut = false;
		while (!bGetOut && myparse.curcomponent < componentnum && myparse.remainder.Length > 0)
		{
			int i = myparse.remainder.IndexOf(myparse.ComponentDelim);
			if (i >= 0 && i < myparse.fieldlen)
			{
				myparse.remainder = myparse.remainder.Substring(i + 1);
				myparse.fieldlen -= i + 1;
				myparse.curcomponent++;
			}
			else
			{
				bGetOut = true;
			}
		}
		if (myparse.curcomponent == componentnum)
		{
			iRet = GetHL7Length(myparse, myparse.ComponentDelim, myparse.fieldlen);
		}
		return iRet;
	}

	private int GetHL7Length(HL7Parse myparse, string Delim, int MaxLen)
	{
		int iRet = 0;
		int j = myparse.remainder.IndexOf(Delim);
		if (j >= 0 && j < MaxLen)
		{
			return j;
		}
		return MaxLen;
	}

	private string GetHL7Field(HL7Parse myparse, uint fieldnum)
	{
		string retstring = "";
		int retlen = 0;
		if ((retlen = FindHL7Field(myparse, fieldnum)) > 0)
		{
			return myparse.remainder.Substring(0, retlen);
		}
		return "";
	}

	private string GetHL7Component(HL7Parse myparse, uint fieldnum, uint componentnum)
	{
		string retstring = "";
		int retlen = 0;
		int fldlen = 0;
		fldlen = ((myparse.curfield >= fieldnum) ? (myparse.fieldlen = GetHL7Length(myparse, myparse.FieldDelim, myparse.remainder.Length)) : FindHL7Field(myparse, fieldnum));
		if (fldlen > 0)
		{
			retstring = (((retlen = FindHL7Component(myparse, componentnum)) <= 0) ? "" : myparse.remainder.Substring(0, retlen));
		}
		return retstring;
	}

	protected string DateTime2HL7(DateTime dt)
	{
		string ret = "";
		if (DateTime.Now >= DateTime.UtcNow)
		{
			TimeSpan tzDif = DateTime.Now - DateTime.UtcNow;
			return dt.ToString("s") + "+" + ((tzDif.Hours < 10) ? "0" : "") + tzDif.Hours.ToString("d") + ":" + ((tzDif.Minutes < 10) ? "0" : "") + tzDif.Minutes.ToString("d");
		}
		TimeSpan tzDif2 = DateTime.UtcNow - DateTime.Now;
		return dt.ToString("s") + "-" + ((tzDif2.Hours < 10) ? "0" : "") + tzDif2.Hours.ToString("d") + ":" + ((tzDif2.Minutes < 10) ? "0" : "") + tzDif2.Minutes.ToString("d");
	}

	private bool SendAcknowledgeMessage()
	{
		bool retVal = true;
		try
		{
			string MSHSegment = GenHL7Header("Protocol");
			string MSASegment = GenHL7Acknowledge();
			string OutMessage = '\v' + MSHSegment + MSASegment + '\u001c' + "\r";
			SendString(OutMessage, "Protocol");
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending acknowledgement", "SendAcknowledgeMessage");
			retVal = false;
		}
		return retVal;
	}

	private string GenHL7Header(string whoFrom)
	{
		string sMSHSegment = "";
		try
		{
			DateTime st = DateTime.Now;
			string SendingApplication = GetSenderStringToLIS(inst_class, m_inst_ver, m_serial_id, m_inst_type);
			sMSHSegment = string.Format("\vMSH|^~\\&|NOVANET|NOVANET|{0}|{1}|{2}||{3}|{4}|{5}|2.5|||NE|NE||UNICODE UTF-8|\r", SendingApplication, SendingFacility, st.ToString("yyyyMMddHHmmss"), MessageType + "^" + MessageSubType, MessageControlID, ProcessingID);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Header Segment", "GenHL7Header");
		}
		return sMSHSegment;
	}

	private string GenHL7Acknowledge()
	{
		string MSASegment = "";
		try
		{
			MSASegment = $"MSA|AA|{MessageControlID}|\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Acknowledgement Segment", "GenHL7Acknowledge");
		}
		return MSASegment;
	}

	private string GenHL7OrderQuery()
	{
		string sQuery = "";
		try
		{
			DateTime st = DateTime.Now;
			string sDateTime = st.Year.ToString("D4") + st.Month.ToString("D2") + st.Day.ToString("D2") + st.Hour.ToString("D2") + st.Minute.ToString("D2") + st.Second.ToString("D2");
			sQuery = string.Format("QRD|{0}|R|I|{1}|||RD|{1}|ORD|\r", sDateTime, m_order_id);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Query Segment", "GenHL7Query");
		}
		return sQuery;
	}

	private string GenHL7Patient()
	{
		string sPatient = "";
		try
		{
			sPatient = $"PID|1|{enterprise_id}|{medrec_num}||";
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
			sPatient += "||";
			if (Race_HL7.Length > 0)
			{
				sPatient += Race_HL7;
			}
			sPatient += "|";
			if (Comp.Compare(sAccountSegment, "PID") == 0)
			{
				int iField = 11;
				int iComponent = 1;
				for (; iField < iAccountField; iField++)
				{
					sPatient += "|";
				}
				for (; iComponent < iAccountComponent; iComponent++)
				{
					sPatient += "^";
				}
				sPatient += account_num;
			}
			sPatient += "\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Patient Segment", "GenHL7Patient");
		}
		return sPatient;
	}

	private string GenHL7NonPatient()
	{
		string sPatient = "";
		string MRN = "";
		try
		{
			MRN = control_lot_num + "^" + control_lot_level + "^" + control_internal_external;
			sPatient = $"PID|1||{MRN}||";
			sPatient += "|";
			sPatient += "\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 PID Segment for non-patient ", "GenHL7NonPatient");
		}
		return sPatient;
	}

	private string GenHL7PatientVisit()
	{
		string sPatientVisit = "";
		try
		{
			sPatientVisit = $"PV1|1||{m_location}^{room_num}^{bed_num}^{m_facility}||||";
			if (Physician.Length > 0)
			{
				sPatientVisit += Physician;
			}
			sPatientVisit += "|";
			if (Comp.Compare(sAccountSegment, "PV1") == 0)
			{
				int iField = 8;
				int iComponent = 1;
				for (; iField < iAccountField; iField++)
				{
					sPatientVisit += "|";
				}
				for (; iComponent < iAccountComponent; iComponent++)
				{
					sPatientVisit += "^";
				}
				sPatientVisit += account_num;
			}
			sPatientVisit += "\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Patient Visit Segment", "GenHL7Patient");
		}
		return sPatientVisit;
	}

	private string GenHL7Order()
	{
		string sOrder = "";
		string szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
		string specid = "";
		if (isQC)
		{
			if (sample_type_HL7.CompareTo("PRF") == 0)
			{
				panel = control_type_HL7 + "^^NOVABIO";
			}
			else
			{
				panel = control_type_HL7 + "_" + control_lot_level + "^^NOVABIO";
			}
		}
		else
		{
			specid = m_order_id;
		}
		try
		{
			sOrder = string.Format("OBR|1||{0}|{1}|||{2}||||||||{3}^^^^^^{4}|{5}||||{6}|||||{6}|\r", specid, panel, szTimeDrawn, sample_type_HL7, role_cd_HL7, Physician, strip_lot_num, status_HL7);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Order Segment", "GenHL7Order");
		}
		return sOrder;
	}

	private string GenHL7Specimen()
	{
		string sSample = "";
		string specid = "";
		string szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
		specid = ((!isQC) ? m_order_id : "");
		try
		{
			sSample = string.Format("SPM|1|^{0}||{1}|||||||{2}||||||{3}||{3}|\r", specid, sample_type_HL7, role_cd_HL7, szTimeDrawn);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Specimen Segment", "GenHL7Specimen");
		}
		return sSample;
	}

	private string GenHL7SpecimenContainer()
	{
		string sSample = "";
		string specid = "";
		string sContainerid = "";
		if (isQC)
		{
			sContainerid = strip_lot_num;
			specid = "";
		}
		else
		{
			specid = m_order_id;
			sContainerid = "";
		}
		try
		{
			sSample = $"SAC|1|{specid}|{sContainerid}|\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Specimen Container Segment", "GenHL7SpecimenContainer");
		}
		return sSample;
	}

	private string GenHL7Inventory()
	{
		string sSample = "";
		string sControlType = "";
		string specid = "";
		string sDateTime = "";
		DateTime myDateTime;
		if (isQC)
		{
			sControlType = control_type_HL7;
			specid = control_lot_num;
			myDateTime = ExpDate;
		}
		else
		{
			sControlType = "";
			specid = m_order_id;
			myDateTime = BirthDate;
		}
		sDateTime = myDateTime.Year.ToString("D4") + myDateTime.Month.ToString("D2") + myDateTime.Day.ToString("D2") + myDateTime.Hour.ToString("D2") + myDateTime.Minute.ToString("D2") + myDateTime.Second.ToString("D2");
		try
		{
			sSample = $"INV|{sControlType}^{control_lot_level}|OK||||||||||{sDateTime}||||{specid}|\r";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Inventory Segment", "GenHL7Inventory");
		}
		return sSample;
	}

	private string GenHL7Result(int nParm)
	{
		string sResult = "";
		try
		{
			string szRange = "";
			if (normal_limit_lo.Length > 0 || normal_limit_hi.Length > 0)
			{
				szRange = normal_limit_lo + " to " + normal_limit_hi;
			}
			string szTimeDrawn = "";
			szTimeDrawn = sampleDateTime.Year.ToString("D4") + sampleDateTime.Month.ToString("D2") + sampleDateTime.Day.ToString("D2") + sampleDateTime.Hour.ToString("D2") + sampleDateTime.Minute.ToString("D2") + sampleDateTime.Second.ToString("D2");
			string szResultValue = "";
			szResultValue = ((result_str_value.Length == 0 || result_str_value == "null") ? "ERR" : ((!bSampleError) ? result_str_value : ("?" + result_str_value)));
			sResult = string.Format("OBX|{0}|FT|{1}^{2}^{3}^{4}^{5}||{6}|{7}|{8}|{9}|||{10}|||{11}||||{12}||\r", nParm.ToString("D1"), test_code, panel, test_code_system, test_transmit_name, method_cd_HL7, szResultValue, units, szRange, interpretation_HL7, status_HL7, szTimeDrawn, m_serial_id);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Result Segment", "GenHL7Result");
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
			string teststr = string.Format("{0}^{1}^{2}^{3}^{4}", test_code, "", test_code_system, test_transmit_name, method_cd_HL7);
			InOutStr += teststr;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Test Code List", "AddTestToList");
		}
	}

	private string GenHL7Comment(int nParm, bool bError)
	{
		string sComment = "";
		try
		{
			string sType = "";
			sType = ((!bError) ? "G" : "I");
			sComment = string.Format("NTE|{0}||{1}|{2}|\r", nParm.ToString("D1"), comment_text, sType);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "building HL7 Comment Segment", "GenHL7Comment");
		}
		return sComment;
	}

	private void MarkOrderQueryAsTransmitted(string whoFrom)
	{
		try
		{
			myDBWriteCommand.CommandText = $"UPDATE DBA.orders set transmitted_flag = 'T' where order_key_num = '{m_order_key_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
			}
			myDBWriteCommand.ExecuteNonQuery();
			m_order_key_num = "";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (OdbcException e)
		{
			handleDBException(e, "marking order query as transmitted", "MarkOrderQueryAsTransmitted");
		}
		catch (Exception e2)
		{
			handleException(e2, "marking order query as transmitted", "MarkOrderQueryAsTransmitted");
		}
	}

	private void MarkSampleAsTransmitted(string whoFrom)
	{
		try
		{
			myDBWriteCommand.CommandText = $"UPDATE DBA.samples set transmitted_flag = 'T' where sample_key_num = '{m_sample_key_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "RTMLIS_HL7_SQL");
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
			resultMessage = resultMessage + '\u001c' + "\r";
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

	private bool SendOrderQuery()
	{
		bool ret = false;
		try
		{
			MessageControlID = m_NovaHL7UUID.GetNovaHL7UUID();
			m_LastMessageSentControlID[m_imsgid] = MessageControlID;
			order_key_num[m_imsgid] = m_order_key_num;
			order_id[m_imsgid] = m_order_id;
			facility[m_imsgid] = m_facility;
			m_MessageControlList.Add(MessageControlID, m_imsgid);
			MessageType = "OSQ";
			MessageSubType = "Q06";
			ProcessingID = "O";
			string MSHSegment = GenHL7Header("Timer");
			m_imsgid++;
			if (m_imsgid > MaxMsgId)
			{
				m_imsgid = 0;
			}
			string QRDSegment = GenHL7OrderQuery();
			queryMessage = '\v' + MSHSegment + QRDSegment + '\u001c' + "\r";
			SendString(queryMessage, "Timer");
			m_parent.m_iNumMessages++;
			m_parent.m_iTotMessages++;
			ret = true;
		}
		catch (Exception e)
		{
			handleException(e, "Sending order query message", "SendOrderQuery");
		}
		return ret;
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
					m_NNBase.log("Closing client port " + sport + " for LIS interface", isXml: false, "hl7");
				}
				m_clienthandler.socket.Close();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport + " for LIS interface", isXml: false, "hl7");
				}
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing networkStream", isXml: false, "hl7");
			}
			m_networkStream.Close();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed networkStream", isXml: false, "hl7");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing networkStream", isXml: false, "hl7");
			}
			m_networkStream.Dispose();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposed networkStream", isXml: false, "hl7");
			}
			if (m_clienthandler != null)
			{
				m_clienthandler.RemoveFromList("hl7.ShutDown");
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
			if (m_NNBase.bDBAvailable)
			{
				m_NNBase.CommAudit(11, "Disconnect", reason);
				string sCommand = "update DBA.health_ping set update_time = now(*), last_disconnect_dttm = now(*) where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
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
		Console.WriteLine("Shutdown " + m_port_num);
		m_isShutDown = true;
		if (bExit)
		{
			LibWrap.ExitThread(0u);
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
				m_NNBase.bDBAvailable = false;
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
			m_NNBase.bDBAvailable = false;
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
		lock (m_lockObj)
		{
			m_ProtocolSending = true;
		}
		m_outbuffer = Encoding.UTF8.GetBytes(input);
		int i = m_outbuffer.Length;
		Console.WriteLine("Sent {0} bytes to {2}{3}:\t{1}", i, input, m_portType, "   ");
		if (m_NNBase.m_isLogging)
		{
			string logMsg = RemoveAsciiControlChar(input);
			m_NNBase.log(logMsg, isXml: false, "RTMLIS");
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
			string reason = ((!m_stopping) ? "Connection Dropped - IOException" : "Shutdown Requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Writing message", "SendString");
			}
		}
		lock (m_lockObj)
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

	private string GetSenderStringToLIS(string instClass, string sfVersion, string serialNum, string modelName)
	{
		return "NOVANET^" + instClass + "^" + sfVersion + "^" + serialNum + "^" + modelName;
	}
}
