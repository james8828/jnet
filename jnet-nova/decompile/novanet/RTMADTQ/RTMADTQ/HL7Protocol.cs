using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data.Odbc;
using System.Globalization;
using System.IO;
using System.Messaging;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.ServiceModel.MsmqIntegration;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Transactions;
using System.Xml;
using Microsoft.Win32;
using NNClass;

namespace RTMADTQ;

internal class HL7Protocol : Protocol
{
	internal class MQMessageAPI
	{
		[DllImport("mqrt.dll", CharSet = CharSet.Unicode)]
		internal static extern int MQOpenQueue(string formatName, int access, int shareMode, ref IntPtr hQueue);

		[DllImport("mqrt.dll", CharSet = CharSet.Unicode)]
		internal static extern int MQMoveQueue(IntPtr sourceQueue, IntPtr targetQueue, long lookupID, IntPtr pTransaction);
	}

	private const char ASCII_VT = '\v';

	private const char ASCII_FS = '\u001c';

	private const char ASCII_CR = '\r';

	private const char ASCII_STX = '\u0002';

	private const char ASCII_ETX = '\u0003';

	private const int MaxReadBuffSize = 65536;

	private NNBase m_NNBase = new NNBase();

	private char MessageBeginChar = '\v';

	private char MessageEndChar = '\u001c';

	private int m_loc_port;

	private byte[] m_outbuffer;

	private byte[] m_inbuffer;

	private Buffers m_ReadBuffers;

	private string m_message = "";

	private string AppRejectMsg = "";

	private string AppErrorMsg = "";

	private string AppWarningMsg = "";

	private string m_facility_alias = "";

	private string m_facility_source = "";

	private string m_location_alias = "";

	private string m_prev_facility_source = "";

	private string m_prev_facility_alias = "";

	private string m_portType = "";

	private NetworkStream m_networkStream;

	private RTMADTQ m_parent;

	private Port.AsynchNetworkServer.ClientHandler m_clienthandler;

	private int m_port_num;

	private HL7Parse segmentparse;

	private string SendingApplication = "";

	private string SendingFacility = "";

	private string ReceivingApplication = "";

	private string ReceivingFacility = "";

	private string MSHTimeStamp = "";

	private string MessageType = "";

	private string MessageSubType = "";

	private string MessageControlID = "";

	private string ProcessingID = "";

	private string PatientID = "";

	private string MedicalRecordNumber = "";

	private string MRNAssigningAuthority = "";

	private string MRNAssigningFacility = "";

	private string FirstName = "";

	private string LastName = "";

	private string MiddleName = "";

	private string Prefix = "";

	private string Suffix = "";

	private string BirthDate = "";

	private string Sex = "";

	private string AccountNumber = "";

	private string AccountAssigningAuthority = "";

	private string AccountAssigningFacility = "";

	private string EventFacility = "";

	private string PatientClass = "";

	private string Location_PV1_3_1 = "";

	private string Room = "";

	private string Bed = "";

	private string Facility_PV1_3_4 = "";

	private string Facility_PV1_3_11 = "";

	private string Facility_PV1_6_4 = "";

	private string Facility_PV1_6_11 = "";

	private string Facility_PV1_11_4 = "";

	private string Facility_PV1_11_11 = "";

	private string Location_PV1_11_1 = "";

	private string Facility_PV1_42_4 = "";

	private string Facility_PV1_42_11 = "";

	private string Location_PV1_42_1 = "";

	private string Facility_PV1_19_4 = "";

	private string Facility_PV1_19_6 = "";

	private string ServicingFacility_PV1_39_1 = "";

	private string Facility_PV1_43_4 = "";

	private string Facility_PV1_43_11 = "";

	private string PatientType = "";

	private string VisitNumFromADT = "";

	private string DischargeDateTime = "";

	protected DateTime AdmitTime;

	protected string AdmitDateTime = "";

	private string PreviousMedicalRecordNumber = "";

	private string PreviousPatientAccount = "";

	private string PreviousPatientID = "";

	private string PreviousVisitNumFromADT = "";

	private string PreviousMRNAssigningAuthority = "";

	private string PreviousMRNAssigningFacility = "";

	private string PreviousAccountAssigningAuthority = "";

	private string PreviousAccountAssigningFacility = "";

	private string PreviousVisitNumAssigningAuthority = "";

	private string PreviousVisitNumAssigningFacility = "";

	private bool bNoVisitInfo;

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	private CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private bool bMRNGiven;

	private bool bPatIDGiven;

	private bool bAccountGiven;

	private bool bPrevMRNGiven;

	private bool bPrevOrNewMRNGiven;

	private bool bMRNDifferent;

	private bool bMRNChanging;

	private bool bMRNCanSpanFacilities;

	private bool bMRNTheSame;

	private bool bPrevPatIDGiven;

	private bool bPrevOrNewPatIDGiven;

	private bool bPatIDDifferent;

	private bool bPatIDChanging;

	private bool bPatIDTheSame;

	private bool bSpansFacilities;

	private bool bSingleFacility;

	private string theoneFacility;

	private bool bPatIDToFind;

	private bool bMRNToFind;

	private bool bAccountToFind;

	private bool bLocationRequired;

	private bool bPrevAcctGiven;

	private bool bPrevOrNewAcctGiven;

	private bool bAcctDifferent;

	private bool bAcctChanging;

	private bool bAcctTheSame;

	private bool bMRNAndPatIDChange;

	private bool bMRNOrPatIDChange;

	private bool bFacilGiven;

	private bool bPrevFacilGiven;

	private bool bLastGiven;

	private bool bFirstGiven;

	private bool bMiddleGiven;

	private bool bSexGiven;

	private bool bBirthGiven;

	private bool bPFXGiven;

	private bool bSFXGiven;

	private Thread m_ProtocolThread;

	protected string BinDir = "C:\\NovaBiomedical\\NovaNet\\Bin";

	private FileStream ConfigReader;

	private byte[] readbuff;

	private XmlDocument configdoc;

	private ADTConfiguration myOverAllConfig = new ADTConfiguration();

	private ADTConfiguration myFacilityConfig = new ADTConfiguration();

	protected bool bMRNsCrossFacilities;

	protected uint iAccountField = 18u;

	protected uint iAccountComponent = 1u;

	protected string sAccountSegment = "PID";

	protected string sDischargeOutPatientClasses = "O^24,E^24,R^24";

	protected string sDischargeOutPatientTypes = "O^24,E^24,R^24";

	protected string sDischargeOPClassOrTypeByFacil = "";

	protected string sDischargeOPClassOrTypeByLoc = "";

	protected string sAdmitOnUpdateTypes = "";

	protected int m_ActiveHours = 24;

	protected int iActiveHours;

	protected string sDefaultSupportedTransactions = "A01,A02,A03,A04,A05,A06,A07,A08,A09,A10,A11,A12,A13,A17,A18,A21,A22,A23,A31,A32,A33,A34,A35,A36,A38,A39,A40,A41,A42,A44,A45,A46,A47,A49,A50,A52,A53,A54,A55,A61,A62";

	protected string sSupportedTransactions = "";

	protected string[] arraySupportedMsgType;

	protected string sMale = "M";

	protected string sFemale = "F";

	protected bool bVisitNumIsVisitNum;

	protected bool bVisitNumIsAcctNum;

	protected bool bVisitNumIsAcctPlusAdmit = true;

	protected bool bOverallVisitNumIsSet;

	protected bool bFacilityVisitNumIsSet;

	protected bool bCurrentConfigIsOverall = true;

	protected bool bCurrentConfigIsFacility;

	protected bool bCurrentConfigIsLocation;

	protected bool bMultipleVisitsPerAccount = true;

	protected bool bAccountNumsCrossFacilities;

	protected bool bVisitNumsCrossPatients;

	protected bool bVisitNumsCrossFacilities;

	private List<string> m_FacilityList;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

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
		case -1:
			m_stopping = true;
			ShutDown("Notify", "Ports", bExit: false);
			break;
		}
		return false;
	}

	public HL7Protocol(ref NetworkStream networkStream, int port_num, string portType, bool logging, RTMADTQ parent, Port.AsynchNetworkServer.ClientHandler clienthandler, int loc_port)
	{
		segmentparse = new HL7Parse();
		m_parent = parent;
		m_clienthandler = clienthandler;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_loc_port = loc_port;
		m_NNBase.NNBaseOpen(logging, "HL7", "RTMADTQ", "ADTQ");
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Connection established via local port " + m_loc_port, isXml: false, "RTMADTQ");
		}
		m_NNBase.OpenDBConnection(ref myDBWriteConnection, ref myDBWriteCommand, 7);
		if (m_NNBase.bDBAvailable)
		{
			m_NNBase.CommAudit(10, "Connect", "");
			if (m_NNBase.bDBAvailable)
			{
				string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMADTQ' and host = '" + m_NNBase.GetLocalPOP() + "'";
				myDBWriteCommand.CommandText = sCommand;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myDBWriteCommand.ExecuteNonQuery();
			}
		}
		m_ReadBuffers = new Buffers(1, 65536);
		m_FacilityList = parent.AvailableFacility;
		LoadConfigFile();
		bSingleFacility = false;
		int iNumFacilities = m_FacilityList.Count;
		if (iNumFacilities == 0)
		{
			m_NNBase.ReportErrorNoDB("Facility list is empty", "E", "loading facility list", "HL7Protocol", "");
			m_NNBase.bDBAvailable = false;
			if (!m_NNBase.m_isLogging)
			{
				m_NNBase.ForceLogging("NoFacilitiesInList");
				m_NNBase.log("There are no facilities in the facility list!", isXml: false, "HL7Protocol");
			}
		}
		if (iNumFacilities == 1)
		{
			bSingleFacility = true;
			theoneFacility = m_FacilityList[0];
		}
		m_ProtocolThread = new Thread(ProtocolThread);
		m_ProtocolThread.Start();
	}

	private bool LoadConfigFile()
	{
		bool bOK_Reg = true;
		bool bOK_File = true;
		bool bOK = true;
		configdoc = new XmlDocument();
		InitializeConfig();
		try
		{
			BinDir = Registry.LocalMachine.OpenSubKey(m_NNBase.REGISTRY_SUBKEY_RTM).GetValue("BinDir").ToString() + "\\";
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RTMADTQ");
		}
		catch (Exception e)
		{
			m_NNBase.ReportException(e, "Getting registry entry for bin folder", "LoadConfigFile");
			bOK = (bOK_Reg = false);
		}
		if (bOK_Reg)
		{
			try
			{
				ConfigReader = new FileStream(BinDir + "RTMADT.XML", FileMode.Open);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("RTMADTQ");
			}
			catch (FileNotFoundException ex3)
			{
				m_NNBase.ReportErrorNoDB("No config file found", "E", "Loading Config file", "LoadConfigFile", ex3.Message);
				bOK_File = false;
			}
			catch (Exception ex4)
			{
				if (ex4.Message.IndexOf("Could not find file") >= 0)
				{
					m_NNBase.ReportErrorNoDB("No config file found", "E", "Loading Config file", "LoadConfigFile", "");
				}
				else
				{
					m_NNBase.ReportException(ex4, "Opening Config file", "LoadConfigFile");
					bOK = false;
				}
				bOK_File = false;
			}
			if (bOK_File)
			{
				try
				{
					if (ConfigReader != null && ConfigReader.CanRead)
					{
						readbuff = new byte[32768];
						int bytesRead = ConfigReader.Read(readbuff, 0, readbuff.Length);
						if (bytesRead > 0)
						{
							string sRead = Encoding.UTF8.GetString(readbuff, 0, bytesRead);
							configdoc.LoadXml(sRead);
						}
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("RTMADTQ");
				}
				catch (XmlException e2)
				{
					m_NNBase.ReportXMLException(e2, "Loading Config file", "LoadConfigFile");
					bOK = (bOK_File = false);
				}
				catch (Exception e3)
				{
					m_NNBase.ReportException(e3, "Loading Config file", "LoadConfigFile");
					bOK = (bOK_File = false);
				}
			}
			if (ConfigReader != null)
			{
				ConfigReader.Close();
			}
		}
		if (bOK)
		{
			bOK = GetOverAllConfig();
		}
		else
		{
			ShutDown("Error loading configuration", "LoadConfigFile", bExit: true);
		}
		return bOK;
	}

	private bool GetOverAllConfig()
	{
		bool bOK = true;
		InitializeConfig();
		try
		{
			if (configdoc != null)
			{
				XmlElement root = configdoc.DocumentElement;
				XmlNodeList nodeList = root.SelectNodes("Setting");
				foreach (XmlNode varval in nodeList)
				{
					XmlElement elem = (XmlElement)varval;
					string variable = elem.GetAttribute("Variable");
					string value = elem.GetAttribute("Value");
					if (Comp.Compare(variable, "MRNsCrossFacilities", CompOpt) == 0)
					{
						if (Comp.Compare(value, "T", CompOpt) == 0)
						{
							bMRNsCrossFacilities = true;
							continue;
						}
						if (Comp.Compare(value, "F", CompOpt) == 0)
						{
							bMRNsCrossFacilities = false;
							continue;
						}
						m_NNBase.ReportErrorNoDB("Invalid value for MRNsCrossFacilities", "E", "parsing MRNsCrossFacilities", "GetOverAllConfig", "");
						bOK = false;
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
						}
						else
						{
							m_NNBase.ReportErrorNoDB("Invalid account field format", "E", "parsing AccountField", "GetOverAllConfig", "");
							bOK = false;
						}
						if (bOK && numvalueparts > iAcctFldPart + 1)
						{
							if (isNumeric(valueparts[iAcctFldPart + 1], NumberStyles.Integer))
							{
								iAccountComponent = (uint)Convert.ToInt32(valueparts[iAcctFldPart + 1]);
								continue;
							}
							m_NNBase.ReportErrorNoDB("Invalid account field format", "E", "parsing AccountField", "GetOverAllConfig", "");
							bOK = false;
						}
					}
					else if (Comp.Compare(variable, "DischargeOutPatientClasses", CompOpt) == 0)
					{
						sDischargeOutPatientClasses = value;
					}
					else if (Comp.Compare(variable, "DischargeOutPatientTypes", CompOpt) == 0)
					{
						sDischargeOutPatientTypes = value;
					}
					else if (Comp.Compare(variable, "AdmitOnUpdateTypes", CompOpt) == 0)
					{
						sAdmitOnUpdateTypes = value;
					}
					else if (Comp.Compare(variable, "FramingCharacters", CompOpt) == 0)
					{
						string sFramingCharacters = value;
						string[] sFramingCharacterArray = sFramingCharacters.Split(',');
						if (sFramingCharacterArray.Length == 2)
						{
							if (Comp.Compare(sFramingCharacterArray[0], "STX", CompOpt) == 0)
							{
								MessageBeginChar = '\u0002';
							}
							if (Comp.Compare(sFramingCharacterArray[1], "ETX", CompOpt) == 0)
							{
								MessageEndChar = '\u0003';
							}
						}
					}
					else if (Comp.Compare(variable, "VisitNumberIs", CompOpt) == 0)
					{
						if (Comp.Compare(value, "AccountPlusAdmitDate", CompOpt) == 0)
						{
							bVisitNumIsAcctPlusAdmit = true;
							bVisitNumIsAcctNum = false;
							bVisitNumIsVisitNum = false;
							bOverallVisitNumIsSet = true;
						}
						else if (Comp.Compare(value, "AccountNumber", CompOpt) == 0)
						{
							bVisitNumIsAcctNum = true;
							bVisitNumIsAcctPlusAdmit = false;
							bVisitNumIsVisitNum = false;
							bOverallVisitNumIsSet = true;
						}
						else if (Comp.Compare(value, "VisitNumber", CompOpt) == 0)
						{
							bVisitNumIsVisitNum = true;
							bVisitNumIsAcctPlusAdmit = false;
							bVisitNumIsAcctNum = false;
							bOverallVisitNumIsSet = true;
						}
					}
					else if (Comp.Compare(variable, "MultipleVisitsPerAccount", CompOpt) == 0)
					{
						if (Comp.Compare(value, "T", CompOpt) == 0)
						{
							bMultipleVisitsPerAccount = true;
						}
						else
						{
							bMultipleVisitsPerAccount = false;
						}
					}
					else if (Comp.Compare(variable, "AccountNumbersCrossFacilities", CompOpt) == 0 && Comp.Compare(value, "T", CompOpt) == 0)
					{
						bAccountNumsCrossFacilities = true;
					}
					else if (Comp.Compare(variable, "VisitNumbersCrossPatients", CompOpt) == 0 && Comp.Compare(value, "T", CompOpt) == 0)
					{
						bVisitNumsCrossPatients = true;
					}
					else if (Comp.Compare(variable, "VisitNumbersCrossFacilities", CompOpt) == 0 && Comp.Compare(value, "T", CompOpt) == 0)
					{
						bVisitNumsCrossFacilities = true;
					}
					else if (Comp.Compare(variable, "ActiveHours", CompOpt) == 0)
					{
						if (isNumeric(value, NumberStyles.Integer))
						{
							m_ActiveHours = Convert.ToInt32(value);
						}
					}
					else if (Comp.Compare(variable, "SupportedTransactions", CompOpt) == 0)
					{
						if (string.IsNullOrEmpty(value))
						{
							arraySupportedMsgType = sSupportedTransactions.Split(',');
						}
						else
						{
							sSupportedTransactions = value;
							arraySupportedMsgType = sSupportedTransactions.Split(',');
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
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RTMADTQ");
		}
		catch (XmlException e)
		{
			m_NNBase.ReportXMLException(e, "Loading configuration variables", "GetOverallConfig");
			bOK = false;
		}
		catch (Exception e2)
		{
			m_NNBase.ReportException(e2, "Loading configuration variables", "GetOverallConfig");
			bOK = false;
		}
		if (!bOK)
		{
			ShutDown("Error loading configuration", "GetOverAllConfig", bExit: true);
		}
		return bOK;
	}

	private void InitializeConfig()
	{
		bMRNsCrossFacilities = false;
		sAccountSegment = "PID";
		iAccountField = 18u;
		iAccountComponent = 1u;
		sDischargeOutPatientClasses = "O^24,E^24,R^24";
		sDischargeOutPatientTypes = "O^24,E^24,R^24";
		sAdmitOnUpdateTypes = "";
		sSupportedTransactions = sDefaultSupportedTransactions;
		sMale = "M";
		sFemale = "F";
		bVisitNumIsVisitNum = false;
		bVisitNumIsAcctNum = false;
		bVisitNumIsAcctPlusAdmit = true;
		bMultipleVisitsPerAccount = true;
		bAccountNumsCrossFacilities = false;
		bVisitNumsCrossPatients = false;
		bVisitNumsCrossFacilities = false;
		m_ActiveHours = 24;
		m_NNBase.bLocationAliasProcessingEnabled = true;
	}

	private void OnReadComplete()
	{
		string reason = "ReadError";
		bool bgetout = false;
		bool bWholeMessageFound = false;
		bool bOK = true;
		int bytesRead = 0;
		int iDataLen = 0;
		int iBegin = 0;
		int iEnd = 0;
		bool bMessageBeginByteFound = false;
		bool bMessageEndByteFound = false;
		try
		{
			while (bOK && !m_isShutDown && !m_isShuttingDown)
			{
				bOK = !m_isShutDown && !m_isShuttingDown && m_networkStream.CanRead;
				if (bOK)
				{
					do
					{
						if (iDataLen == 0 || !bWholeMessageFound)
						{
							bOK = m_ReadBuffers.NetworkStreamRead(ref m_networkStream, ref m_inbuffer, ref iDataLen, ref m_isShutDown, ref m_isShuttingDown, ref bytesRead);
							bOK = !m_isShutDown && !m_isShuttingDown && bytesRead > 0;
						}
						else
						{
							bOK = !m_isShutDown && !m_isShuttingDown;
						}
						if (bOK)
						{
							bWholeMessageFound = m_ReadBuffers.GetFullHL7MessageFromBuffer(ref m_inbuffer, ref iBegin, ref iEnd, ref iDataLen, (byte)MessageBeginChar, (byte)MessageEndChar, ref bMessageBeginByteFound, ref bMessageEndByteFound, ref m_message);
							if (bWholeMessageFound)
							{
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log("....Read................................................", isXml: false, m_portType);
									string logMsg = RemoveAsciiControlChar(m_message);
									m_NNBase.log(logMsg, isXml: false, m_portType);
								}
							}
							else if (m_NNBase.m_isLogging)
							{
								m_NNBase.log("....Read................................................", isXml: false, m_portType);
								m_NNBase.log(bytesRead + " bytes read", isXml: false, m_portType);
							}
						}
						bOK = !m_isShutDown && !m_isShuttingDown && bytesRead > 0;
					}
					while (bOK && !bWholeMessageFound);
				}
				if (bOK && bWholeMessageFound)
				{
					if (m_isShutDown | m_isShuttingDown)
					{
						break;
					}
					ProcessMessage();
				}
				else if (bytesRead == 0)
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

	public override void ProcessMessage()
	{
		bool bOK = true;
		AppRejectMsg = "";
		AppErrorMsg = "";
		AppWarningMsg = "";
		try
		{
			bool bDone = false;
			InitMessageFields();
			m_message = Regex.Replace(m_message, "\\n", "");
			int iLast = 0;
			int i = m_message.IndexOf('\r', iLast);
			while (!bDone && i > 0 && i < m_message.Length)
			{
				if (i > iLast)
				{
					string segment = m_message.Substring(iLast, i - iLast);
					switch (segment.Substring(0, 3))
					{
					case "MSH":
						ProcessMessageHeaderSegment(segment);
						bOK &= MessageSubTypeSupported(MessageSubType);
						if (!bOK)
						{
							bDone = true;
						}
						break;
					case "EVN":
						ProcessEventSegment(segment);
						break;
					case "PID":
						ProcessPatientIdentificationSegment(segment);
						break;
					case "PV1":
						bNoVisitInfo = false;
						ProcessPatientVisitSegment(segment);
						break;
					case "MRG":
						bOK &= ProcessMergeSegment(segment);
						if (!bOK)
						{
							bDone = true;
						}
						break;
					}
				}
				if (i > 0 && i < m_message.Length)
				{
					iLast = i + 1;
					i = m_message.IndexOf('\r', iLast);
					if (i < 0)
					{
						bDone = true;
					}
				}
			}
			if (bOK)
			{
				ProcessParsedMessage();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "processing ADT message", "ProcessMessage");
		}
		SendAcknowledgeMessage();
		m_message = "";
		m_parent.m_iNumMessages++;
		m_parent.m_iTotMessages++;
	}

	private void ProcessParsedMessage()
	{
		if (ADTMessageOK())
		{
			MessageProcessorClient strClient = new MessageProcessorClient("MessageResponseEndpoint");
			MsmqMessage<string> strMsmqMsg = new MsmqMessage<string>(m_message);
			strMsmqMsg.Priority = MessagePriority.Highest;
			strMsmqMsg.Label = string.Concat("[RTMADTQ][", DateTime.Now, "]");
			using (TransactionScope scope = new TransactionScope(TransactionScopeOption.Required))
			{
				strClient.SubmitStringMessage(strMsmqMsg);
				scope.Complete();
			}
			strClient.Close();
		}
	}

	private string MessageSubTypeDescription(string msgsubtype)
	{
		string description = "unknown transaction type";
		switch (MessageSubType)
		{
		case "A01":
			description = "admit";
			break;
		case "A02":
			description = "transfer patient";
			break;
		case "A03":
			description = "Discharge/End Visit";
			break;
		case "A04":
			description = "register patient";
			break;
		case "A05":
			description = "pre-admit";
			break;
		case "A06":
			description = "change outpatient to inpatient";
			break;
		case "A07":
			description = "change inpatient to outpatient";
			break;
		case "A08":
			description = "update patient information";
			break;
		case "A09":
			description = "patient departing - tracking";
			break;
		case "A10":
			description = "patient arriving - tracking";
			break;
		case "A11":
			description = "cancel admit";
			break;
		case "A12":
			description = "cancel tranfer";
			break;
		case "A13":
			description = "cancel discharge";
			break;
		case "A14":
			description = "pending admit";
			break;
		case "A15":
			description = "pending transfer";
			break;
		case "A16":
			description = "pending discharge";
			break;
		case "A17":
			description = "swap patients (actually: patients swap beds)";
			break;
		case "A18":
			description = "merge patient information";
			break;
		case "A19":
			description = "patient query";
			break;
		case "A20":
			description = "bed status update";
			break;
		case "A21":
			description = "patient goes on leave of absence";
			break;
		case "A22":
			description = "patient returns from leave of absence";
			break;
		case "A23":
			description = "delete patient (actually: delete patient visit)";
			break;
		case "A24":
			description = "link patient information";
			break;
		case "A25":
			description = "cancel pending discharge";
			break;
		case "A26":
			description = "cancel pending transfer";
			break;
		case "A27":
			description = "cancel pending admit";
			break;
		case "A28":
			description = "add person or patient information";
			break;
		case "A29":
			description = "delete person information";
			break;
		case "A30":
			description = "merge person information";
			break;
		case "A31":
			description = "update person information";
			break;
		case "A32":
			description = "cancel patient arriving - tracking";
			break;
		case "A33":
			description = "cancel patient departing - tracking";
			break;
		case "A34":
			description = "merge patient information - patient ID only (actually: merge patient id list - MRN)";
			break;
		case "A35":
			description = "Merge Patient Information - Account Number Only";
			break;
		case "A36":
			description = "Merge Patient Information - Patient ID & Account Number(actually: merge patient id list - MRN & account)";
			break;
		case "A37":
			description = "unlink patient information";
			break;
		case "A38":
			description = "cancel preadmit";
			break;
		case "A39":
			description = "Merge Person - Patient ID";
			break;
		case "A40":
			description = "Merge Patient - Patient Identifier List (MRN)";
			break;
		case "A41":
			description = "Merge Account - Patient Account Number";
			break;
		case "A42":
			description = "Merge Visit - Visit Number";
			break;
		case "A43":
			description = "move patient information - patient identifier list (MRN)";
			break;
		case "A44":
			description = "Move Account Information - Patient Account Number";
			break;
		case "A45":
			description = "Move Visit Information - Visit Number";
			break;
		case "A46":
			description = "Change Patient ID";
			break;
		case "A47":
			description = "Change Patient Identifier List (MRN)";
			break;
		case "A48":
			description = "Change alternate patient ID";
			break;
		case "A49":
			description = "Change Patient Account Number";
			break;
		case "A50":
			description = "Change Visit Number";
			break;
		case "A51":
			description = "Change alternate visit ID";
			break;
		case "A52":
			description = "cancel leave of absence";
			break;
		case "A53":
			description = "cancel patient returns from leave of absence";
			break;
		case "A54":
			description = "change attending doctor";
			break;
		case "A55":
			description = "cancel change attending doctor";
			break;
		case "A56":
			description = "get person demographics and response";
			break;
		case "A57":
			description = "find candidates and response";
			break;
		case "A58":
			description = "get corresponding identifiers and response";
			break;
		case "A59":
			description = "allocate identifiers and response";
			break;
		case "A60":
			description = "update adverse reaction information";
			break;
		case "A61":
			description = "change consulting doctor";
			break;
		case "A62":
			description = "cancel change consulting doctor";
			break;
		}
		return description;
	}

	private void InitMessageFields()
	{
		bNoVisitInfo = true;
		m_facility_source = "";
		m_facility_alias = "";
		m_location_alias = "";
		m_prev_facility_alias = "";
		m_prev_facility_source = "";
		SendingApplication = "";
		SendingFacility = "";
		ReceivingApplication = "";
		ReceivingFacility = "";
		MSHTimeStamp = "";
		MessageType = "";
		MessageSubType = "";
		MessageControlID = "";
		ProcessingID = "";
		PatientID = "";
		MedicalRecordNumber = "";
		MRNAssigningAuthority = "";
		MRNAssigningFacility = "";
		FirstName = "";
		LastName = "";
		MiddleName = "";
		Prefix = "";
		Suffix = "";
		BirthDate = "";
		Sex = "";
		AccountNumber = "";
		AccountAssigningAuthority = "";
		AccountAssigningFacility = "";
		EventFacility = "";
		PatientClass = "";
		Location_PV1_3_1 = "";
		Room = "";
		Bed = "";
		Facility_PV1_3_4 = "";
		Facility_PV1_3_11 = "";
		Facility_PV1_6_4 = "";
		Facility_PV1_6_11 = "";
		Facility_PV1_11_4 = "";
		Facility_PV1_11_11 = "";
		Location_PV1_11_1 = "";
		Facility_PV1_42_4 = "";
		Facility_PV1_42_11 = "";
		Location_PV1_42_1 = "";
		Facility_PV1_19_4 = "";
		Facility_PV1_19_6 = "";
		ServicingFacility_PV1_39_1 = "";
		Facility_PV1_43_4 = "";
		Facility_PV1_43_11 = "";
		PatientType = "";
		DischargeDateTime = "";
		PreviousMedicalRecordNumber = "";
		PreviousPatientAccount = "";
		PreviousPatientID = "";
		VisitNumFromADT = "";
		PreviousVisitNumFromADT = "";
		PreviousMRNAssigningAuthority = "";
		PreviousMRNAssigningFacility = "";
		PreviousAccountAssigningAuthority = "";
		PreviousAccountAssigningFacility = "";
		PreviousVisitNumAssigningAuthority = "";
		PreviousVisitNumAssigningFacility = "";
		bPatIDToFind = false;
		bMRNToFind = false;
		bAccountToFind = false;
	}

	private bool ADTMessageOK()
	{
		bool bOK = false;
		m_facility_alias = "";
		m_facility_source = "";
		bMRNGiven = MedicalRecordNumber.Length > 0;
		bPrevMRNGiven = PreviousMedicalRecordNumber.Length > 0;
		bPrevOrNewMRNGiven = bMRNGiven | bPrevMRNGiven;
		bPatIDGiven = PatientID.Length > 0;
		bPrevPatIDGiven = PreviousPatientID.Length > 0;
		bPrevOrNewPatIDGiven = bPatIDGiven | bPrevPatIDGiven;
		bAccountGiven = AccountNumber.Length > 0;
		bPrevAcctGiven = PreviousPatientAccount.Length > 0;
		bPrevOrNewAcctGiven = bAccountGiven | bPrevAcctGiven;
		bLastGiven = LastName.Replace("\"", "").Length > 0;
		bFirstGiven = FirstName.Replace("\"", "").Length > 0;
		bMiddleGiven = MiddleName.Replace("\"", "").Length > 0;
		bSexGiven = Sex.Replace("\"", "").Length > 0 && Comp.Compare(Sex, "U", CompOpt) != 0;
		bBirthGiven = BirthDate.Replace("\"", "").Length > 0;
		bPFXGiven = Prefix.Replace("\"", "").Length > 0;
		bSFXGiven = Suffix.Replace("\"", "").Length > 0;
		bPatIDToFind = bPrevOrNewPatIDGiven;
		bMRNToFind = bPrevOrNewMRNGiven;
		bAccountToFind = bPrevOrNewAcctGiven;
		bLocationRequired = false;
		bMRNDifferent = bMRNGiven & bPrevMRNGiven & (Comp.Compare(MedicalRecordNumber, PreviousMedicalRecordNumber, CompOpt) != 0);
		bMRNChanging = bMRNDifferent & (MessageSubType != "A44") & (MessageSubType != "A45");
		bMRNCanSpanFacilities = bPrevOrNewMRNGiven & bMRNsCrossFacilities & (MessageSubType != "A44") & (MessageSubType != "A45");
		bMRNTheSame = bMRNGiven & bPrevMRNGiven & !bMRNDifferent;
		bPatIDDifferent = bPatIDGiven & bPrevPatIDGiven & (Comp.Compare(PatientID, PreviousPatientID, CompOpt) != 0);
		bPatIDChanging = bPatIDDifferent & (MessageSubType != "A44") & (MessageSubType != "A45");
		bPatIDTheSame = bPatIDGiven & bPrevPatIDGiven & !bPatIDDifferent;
		bAcctDifferent = bAccountGiven & bPrevAcctGiven & (Comp.Compare(AccountNumber, PreviousPatientAccount, CompOpt) != 0);
		bAcctChanging = bAcctDifferent & (MessageSubType != "A45");
		bAcctTheSame = bAccountGiven & bPrevAcctGiven & !bAcctDifferent;
		bMRNAndPatIDChange = bMRNChanging & bPatIDChanging;
		bMRNOrPatIDChange = bMRNChanging | bPatIDChanging;
		if (bSingleFacility)
		{
			m_facility_alias = theoneFacility;
		}
		else
		{
			if (!TryFacility(Facility_PV1_3_4, "Assigned patient location facility - PV1.3.4") && !TryFacility(Facility_PV1_3_11, "Location assigning authority - PV1.3.11") && !TryFacility(Facility_PV1_11_4, "Temporary patient location facility - PV1.11.4") && !TryFacility(Facility_PV1_11_11, "Temporary patient location assigning authority - PV1.11.1") && !TryFacility(Facility_PV1_19_4, "Visit number assigning authority - PV1.19.4") && !TryFacility(Facility_PV1_19_6, "Visit number assigning facility - PV1.19.6") && !TryFacility(ServicingFacility_PV1_39_1, "Servicing facility - PV1.39.1") && !TryFacility(Facility_PV1_42_4, "Pending location facility - PV1.42.4") && !TryFacility(Facility_PV1_42_11, "Pending location Assigning Authority - PV1.42.11") && !TryFacility(MRNAssigningAuthority, "MRN Assigning Authority - PID.3.4") && !TryFacility(MRNAssigningFacility, "MRN Assigning Facility - PID.3.6") && !TryFacility(AccountAssigningAuthority, "Account Assigning Authority - PID.18.4") && !TryFacility(AccountAssigningFacility, "Account Assigning Facility - PID.18.6") && !TryFacility(EventFacility, "Event Facility - EVN.7"))
			{
				TryFacility(SendingFacility, "SendingFacility - MSH.4");
			}
			if (!TryPrevFacility(Facility_PV1_6_4, "Prior patient location facility - PV1.6.4") && !TryPrevFacility(Facility_PV1_6_11, "Prior patient location assigning authority - PV1.6.11") && !TryPrevFacility(Facility_PV1_43_4, "Prior temporary location facility - PV1.43.4") && !TryPrevFacility(Facility_PV1_43_11, "Prior temporary location Assigning Authority - PV1.43.11") && !TryPrevFacility(PreviousMRNAssigningAuthority, "Prior MRN Assigning Authority - MRG.1.4") && !TryPrevFacility(PreviousMRNAssigningFacility, "Prior MRN Assigning Facility - MRG.1.6") && !TryPrevFacility(PreviousAccountAssigningAuthority, "Prior Account Assigning Authority - MRG.3.4") && !TryPrevFacility(PreviousAccountAssigningFacility, "Prior Account Assigning Facility - MRG.3.6") && !TryPrevFacility(PreviousVisitNumAssigningAuthority, "Prior Visit Number Assigning Authority - MRG.5.4"))
			{
				TryPrevFacility(PreviousVisitNumAssigningFacility, "Prior Visit Number Assigning Facility - MRG.5.6");
			}
			if (m_facility_alias.Length == 0 && m_prev_facility_alias.Length > 0)
			{
				m_facility_source = m_prev_facility_source;
				m_facility_alias = m_prev_facility_alias;
			}
			if (m_prev_facility_alias.Length == 0 && m_facility_alias.Length > 0)
			{
				m_prev_facility_source = m_facility_source;
				m_prev_facility_alias = m_facility_alias;
			}
		}
		bFacilGiven = m_facility_alias.Length > 0;
		bPrevFacilGiven = m_prev_facility_alias.Length > 0;
		bSpansFacilities = !bFacilGiven & bNoVisitInfo & bPatIDChanging & !bSingleFacility;
		if (bMRNsCrossFacilities)
		{
			bSpansFacilities |= !bFacilGiven & bNoVisitInfo & bMRNChanging & !bSingleFacility;
		}
		if (!bFacilGiven && bSpansFacilities)
		{
			m_NNBase.log("This transaction spans facilities", isXml: false, "ADTMessageOK");
			return true;
		}
		if (!bNoVisitInfo)
		{
			if (!bFacilGiven && !bSpansFacilities)
			{
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "No facility provided or facility not found", "E", "checking ADT message", "ADTMessageOK", "");
				return false;
			}
			bOK = GetLocationFromPV1();
			if (!bOK)
			{
				return bOK;
			}
		}
		if (bNoVisitInfo)
		{
			bOK = GetFacilityAndLocationWithoutPV1();
			if (!bOK)
			{
				return bOK;
			}
		}
		return FinalMessageChecks();
	}

	private bool GetLocationFromPV1()
	{
		bool bOK = true;
		if (Location_PV1_3_1.Length > 0)
		{
			m_location_alias = Location_PV1_3_1;
		}
		else if (Location_PV1_11_1.Length > 0)
		{
			m_location_alias = Location_PV1_11_1;
		}
		else if (Location_PV1_42_1.Length > 0)
		{
			m_location_alias = Location_PV1_42_1;
		}
		if (m_location_alias.Length == 0)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "No location provided", "E", "checking ADT message", "GetLocationFromPV1", "");
			return false;
		}
		return bOK;
	}

	private bool GetFacilityAndLocationWithoutPV1()
	{
		bool bOK = true;
		bool bNewVisitCanBeFound = false;
		bool bPrevVisitCanBeFound = false;
		if (bPrevOrNewPatIDGiven)
		{
			return true;
		}
		bNewVisitCanBeFound = CanWeGetPatientVisitInfo(bNew: true);
		bPrevVisitCanBeFound = CanWeGetPatientVisitInfo(bNew: false);
		if (!bNewVisitCanBeFound && !bPrevVisitCanBeFound)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "Insufficient patient and facility identification provided", "E", "checking demographics", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "(the necessary combination of facility and/or new and/or previous (account and/or MRN and/or PatientID) were missing on an ADT message with no PV1 segment");
			return false;
		}
		return true;
	}

	private bool FinalMessageChecks()
	{
		bool bOK = true;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("facility=\"" + m_facility_alias + " location=\"" + m_location_alias + " MRN=\"" + MedicalRecordNumber + "\" PreviousMRN=\"" + PreviousMedicalRecordNumber + "\" PatientID=\"" + PatientID + "\" PreviousPatientID=\"" + PreviousPatientID + "\" AccountNumber=\"" + AccountNumber + "\" PreviousAccountNumber=\"" + PreviousPatientAccount + "\" VisitNum=\"" + VisitNumFromADT + "\" PreviousVisitNum=\"" + PreviousVisitNumFromADT + "\"", isXml: false, "FinalMessageChecks");
		}
		if (m_facility_alias.Length == 0 && !bSpansFacilities && !bPrevOrNewPatIDGiven && !bMRNCanSpanFacilities)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "No facility provided", "E", "checking ADT message", "FinalMessageChecks", "");
			bOK = false;
		}
		if (m_location_alias.Length == 0 && bLocationRequired && !bPrevOrNewPatIDGiven && !bMRNCanSpanFacilities)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "No location provided", "E", "checking ADT message", "FinalMessageChecks", "");
			bOK = false;
		}
		if (bOK)
		{
			bFacilGiven = m_facility_alias.Length > 0;
			if (!bMRNGiven && !bAccountGiven && !bPatIDGiven)
			{
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "No Patient IDs provided", "E", "checking ADT message", "FinalMessageChecks", "one or more of (MRN, Account number or Patient ID expected)");
				bOK = false;
			}
			if (bPrevPatIDGiven && !bPatIDGiven && (bMRNChanging || bAcctDifferent))
			{
				PatientID = PreviousPatientID;
				bPatIDGiven = true;
			}
			if (bPrevMRNGiven && !bMRNGiven && (bPatIDChanging || bAcctDifferent))
			{
				MedicalRecordNumber = PreviousMedicalRecordNumber;
				bMRNGiven = true;
			}
			if (bPrevAcctGiven && !bAccountGiven && (bPatIDDifferent || bMRNDifferent))
			{
				AccountNumber = PreviousPatientAccount;
				bAccountGiven = true;
			}
			if ((bPrevPatIDGiven && !bPatIDGiven) || (bPrevMRNGiven && !bMRNGiven) || (bPrevAcctGiven && !bAccountGiven))
			{
				string sPatIDs = "";
				if (bPrevPatIDGiven && !bPatIDGiven)
				{
					sPatIDs += "Patient ID";
				}
				if (bPrevMRNGiven && !bMRNGiven)
				{
					if (sPatIDs.Length > 0)
					{
						sPatIDs += " and ";
					}
					sPatIDs += "MRN";
				}
				if (bPrevAcctGiven && !bAccountGiven)
				{
					if (sPatIDs.Length > 0)
					{
						sPatIDs += " and ";
					}
					sPatIDs += "account number";
				}
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "No " + sPatIDs + " provided where previous " + sPatIDs + " provided", "E", "checking ADT message", "FinalMessageChecks", "");
				bOK = false;
			}
		}
		return bOK;
	}

	protected bool MessageSubTypeSupported(string MessageSubType)
	{
		bool bOK = false;
		string[] array = arraySupportedMsgType;
		foreach (string sTransType in array)
		{
			if (Comp.Compare(sTransType, MessageSubType, CompOpt) == 0)
			{
				bOK = true;
				break;
			}
		}
		if (!bOK)
		{
			m_NNBase.ReportErrorNoDB(AppRejectMsg = "Transaction type not supported: " + MessageSubType + " - " + MessageSubTypeDescription(MessageSubType), "E", "checking ADT message", "MessageSubTypeSupported", "");
		}
		return bOK;
	}

	private bool TryFacility(string myFacility, string myFacilitySource)
	{
		bool bRet = false;
		if (myFacility.Length > 0)
		{
			if (m_FacilityList.Contains(myFacility.ToUpper()))
			{
				m_facility_alias = myFacility;
			}
			if (m_facility_alias.Length > 0)
			{
				m_facility_source = myFacilitySource;
				bRet = true;
			}
			else
			{
				m_NNBase.ReportErrorNoDB("Facility '" + myFacility + "' (" + myFacilitySource + ") not found", "W", "looking up facility", "TryFacility", "");
			}
		}
		return bRet;
	}

	private bool TryPrevFacility(string myPrevFacility, string myPrevFacilitySource)
	{
		bool bRet = false;
		if (myPrevFacility.Length > 0 && Comp.Compare(m_facility_alias, myPrevFacility, CompOpt) != 0)
		{
			if (m_FacilityList.Contains(myPrevFacility.ToUpper()))
			{
				m_prev_facility_alias = myPrevFacility;
				m_prev_facility_source = myPrevFacilitySource;
				bRet = true;
			}
			else
			{
				m_NNBase.ReportErrorNoDB("Facility '" + myPrevFacility + "' (" + myPrevFacilitySource + ") not found", "W", "looking up previous facility", "TryPrevFacility", "");
			}
		}
		return bRet;
	}

	private bool CanWeGetPatientVisitInfo(bool bNew)
	{
		return (bNew & (bPatIDGiven | (bFacilGiven & (bMRNGiven | bAccountGiven)) | (bMRNGiven & bMRNCanSpanFacilities))) | (!bNew & (bPrevOrNewPatIDGiven | ((bFacilGiven | bPrevFacilGiven) & (bPrevOrNewMRNGiven | bPrevOrNewAcctGiven)) | (bPrevOrNewMRNGiven & bMRNCanSpanFacilities)));
	}

	private void ProcessMessageHeaderSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(9);
		segmentparse.curfield = 3u;
		segmentparse.curcomponent = 1u;
		segmentparse.FieldDelim = segment.Substring(3, 1);
		segmentparse.ComponentDelim = segment.Substring(4, 1);
		segmentparse.SubComponentDelim = segment.Substring(7, 1);
		SendingApplication = GetHL7Field(segmentparse, 3u);
		SendingFacility = GetHL7Field(segmentparse, 4u);
		ReceivingApplication = GetHL7Field(segmentparse, 5u);
		ReceivingFacility = GetHL7Field(segmentparse, 6u);
		MSHTimeStamp = GetHL7Field(segmentparse, 7u);
		MessageType = GetHL7Component(segmentparse, 9u, 1u);
		MessageSubType = GetHL7Component(segmentparse, 9u, 2u);
		MessageControlID = GetHL7Field(segmentparse, 10u);
		ProcessingID = GetHL7Component(segmentparse, 11u, 1u);
	}

	private void ProcessEventSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		EventFacility = GetHL7Field(segmentparse, 7u);
	}

	private void ProcessPatientIdentificationSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		PatientID = GetHL7Component(segmentparse, 2u, 1u).Replace("\"", "");
		MedicalRecordNumber = GetHL7Component(segmentparse, 3u, 1u).Replace("\"", "");
		MRNAssigningAuthority = GetHL7Component(segmentparse, 3u, 4u);
		MRNAssigningFacility = GetHL7Component(segmentparse, 3u, 6u);
		LastName = GetHL7Component(segmentparse, 5u, 1u);
		FirstName = GetHL7Component(segmentparse, 5u, 2u);
		MiddleName = GetHL7Component(segmentparse, 5u, 3u);
		Suffix = GetHL7Component(segmentparse, 5u, 4u);
		Prefix = GetHL7Component(segmentparse, 5u, 5u);
		BirthDate = GetHL7Field(segmentparse, 7u);
		Sex = GetHL7Field(segmentparse, 8u).Replace("\"", "");
		if (sMale.Length > 0 && Comp.Compare(Sex, sMale, CompOpt) == 0)
		{
			Sex = "M";
		}
		if (sFemale.Length > 0 && Comp.Compare(Sex, sFemale, CompOpt) == 0)
		{
			Sex = "F";
		}
		AccountAssigningAuthority = GetHL7Component(segmentparse, 18u, 4u);
		AccountAssigningFacility = GetHL7Component(segmentparse, 18u, 6u);
		if (Comp.Compare(sAccountSegment, "PID") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			AccountNumber = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "");
		}
	}

	private void ProcessPatientVisitSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		PatientClass = GetHL7Field(segmentparse, 2u).Replace("\"", "");
		Location_PV1_3_1 = GetHL7Component(segmentparse, 3u, 1u);
		Room = GetHL7Component(segmentparse, 3u, 2u);
		Bed = GetHL7Component(segmentparse, 3u, 3u);
		Facility_PV1_3_4 = GetHL7Component(segmentparse, 3u, 4u);
		Facility_PV1_3_11 = GetHL7Component(segmentparse, 3u, 11u);
		Facility_PV1_6_4 = GetHL7Component(segmentparse, 6u, 4u);
		Facility_PV1_6_11 = GetHL7Component(segmentparse, 6u, 11u);
		Location_PV1_11_1 = GetHL7Component(segmentparse, 11u, 1u);
		Facility_PV1_11_4 = GetHL7Component(segmentparse, 11u, 4u);
		Facility_PV1_11_11 = GetHL7Component(segmentparse, 11u, 11u);
		PatientType = GetHL7Field(segmentparse, 18u).Replace("\"", "");
		VisitNumFromADT = GetHL7Component(segmentparse, 19u, 1u);
		Facility_PV1_19_4 = GetHL7Component(segmentparse, 19u, 4u);
		Facility_PV1_19_6 = GetHL7Component(segmentparse, 19u, 6u);
		ServicingFacility_PV1_39_1 = GetHL7Field(segmentparse, 39u);
		Location_PV1_42_1 = GetHL7Component(segmentparse, 42u, 1u);
		Facility_PV1_42_4 = GetHL7Component(segmentparse, 42u, 4u);
		Facility_PV1_42_11 = GetHL7Component(segmentparse, 42u, 11u);
		Facility_PV1_43_4 = GetHL7Component(segmentparse, 43u, 4u);
		Facility_PV1_43_11 = GetHL7Component(segmentparse, 43u, 11u);
		AdmitDateTime = GetHL7Field(segmentparse, 44u);
		AdmitTime = YMDhms_To_DateTime(ref AdmitDateTime, "AdmitDateTime");
		DischargeDateTime = GetHL7Field(segmentparse, 45u);
		if (Comp.Compare(sAccountSegment, "PV1") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			AccountNumber = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "");
		}
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
					m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
				}
				else
				{
					int month = Convert.ToInt32(YMDhms.Substring(4, 2));
					if (month < 1 || month > 12)
					{
						m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
					}
					else
					{
						int day = Convert.ToInt32(YMDhms.Substring(6, 2));
						if (day < 1 || day > DateTime.DaysInMonth(year, month))
						{
							m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
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
								m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
							}
							else
							{
								int minute = ((YMDhms.Length > 11) ? Convert.ToInt32(YMDhms.Substring(10, 2)) : 0);
								if (minute > 59)
								{
									m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
								}
								else
								{
									int second = ((YMDhms.Length > 13) ? Convert.ToInt32(YMDhms.Substring(12, 2)) : 0);
									if (second > 59)
									{
										m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
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
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", "");
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
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

	private bool ProcessMergeSegment(string segment)
	{
		bool bOK = false;
		if (segment.Length < 7)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid MRG segment format", "E", "parsing MRG Segment", "ProcessMergeSegment", "The MRG segment is too short");
		}
		else
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			PreviousMedicalRecordNumber = GetHL7Component(segmentparse, 1u, 1u);
			PreviousMRNAssigningAuthority = GetHL7Component(segmentparse, 1u, 4u);
			PreviousMRNAssigningFacility = GetHL7Component(segmentparse, 1u, 6u);
			PreviousPatientAccount = GetHL7Component(segmentparse, 3u, 1u);
			PreviousAccountAssigningAuthority = GetHL7Component(segmentparse, 3u, 4u);
			PreviousAccountAssigningFacility = GetHL7Component(segmentparse, 3u, 6u);
			PreviousPatientID = GetHL7Component(segmentparse, 4u, 1u);
			PreviousVisitNumFromADT = GetHL7Component(segmentparse, 5u, 1u);
			PreviousVisitNumAssigningAuthority = GetHL7Component(segmentparse, 5u, 4u);
			PreviousVisitNumAssigningFacility = GetHL7Component(segmentparse, 5u, 6u);
			bOK = true;
		}
		return bOK;
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
			if ((retlen = FindHL7Component(myparse, componentnum)) > 0)
			{
				retstring = myparse.remainder.Substring(0, retlen);
				int i = retstring.IndexOf(myparse.SubComponentDelim);
				if (i > 0)
				{
					retstring = retstring.Substring(0, i);
				}
			}
			else
			{
				retstring = "";
			}
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
			DateTime st = DateTime.Now;
			string MSHSegment = string.Format("\vMSH|^~\\&|NOVANET|NOVANET|{0}|{1}|{2}||ACK|{3}|{4}|2.5|||NE|NE|\r", SendingApplication, SendingFacility, st.ToString("yyyyMMddHHmmss"), MessageControlID, ProcessingID);
			string AckCode = ((AppRejectMsg.Length > 0) ? "AR" : ((AppErrorMsg.Length > 0) ? "AE" : "AA"));
			string AckMsg = ((AppRejectMsg.Length > 0) ? AppRejectMsg : ((AppErrorMsg.Length > 0) ? AppErrorMsg : ""));
			if (AckMsg.Length == 0 && AppWarningMsg.Length > 0)
			{
				AckMsg = AppWarningMsg;
			}
			string MSASegment = $"MSA|{AckCode}|{MessageControlID}|{AckMsg}|||\r";
			string OutMessage = MSHSegment + MSASegment + '\u001c' + "\r";
			SendString(OutMessage);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "formatting acknowledgement", "SendAcknowledgeMessage");
			retVal = false;
		}
		return retVal;
	}

	private void ShutDown(string reason, string whoFrom, bool bExit)
	{
		if (m_isShutDown || m_isShuttingDown)
		{
			return;
		}
		m_isShuttingDown = true;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Shutting down because " + reason, isXml: false, whoFrom);
		}
		try
		{
			if (m_clienthandler != null)
			{
				string sport = m_clienthandler.socket.Handle.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing client port " + sport + " for ADT interface", isXml: false, "hl7.ShutDown");
				}
				m_clienthandler.socket.Close();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport + " for ADT interface", isXml: false, "hl7.ShutDown");
				}
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing networkStream (reason : " + reason + ")(from : " + whoFrom + ")", isXml: false, "hl7.ShutDown");
			}
			m_networkStream.Close();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed networkStream", isXml: false, "hl7.ShutDown");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing networkStream", isXml: false, "hl7.ShutDown");
			}
			m_networkStream.Dispose();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposed networkStream", isXml: false, "hl7.ShutDown");
			}
			if (m_clienthandler != null)
			{
				m_clienthandler.RemoveFromList("hl7.ShutDown");
			}
			m_parent.ConnectedToADTFeeder = false;
		}
		catch (ThreadAbortException)
		{
		}
		catch (Exception)
		{
		}
		try
		{
			if (m_ProtocolThread != null && m_ProtocolThread.IsAlive && Thread.CurrentThread.ManagedThreadId != m_ProtocolThread.ManagedThreadId)
			{
				try
				{
					m_ProtocolThread.Abort();
				}
				catch (ThreadAbortException)
				{
				}
				catch (Exception)
				{
				}
			}
			Thread.Sleep(1000);
			if (m_NNBase.bDBAvailable)
			{
				m_NNBase.CommAudit(11, "Disconnect", reason);
				string sCommand = "update DBA.health_ping set update_time = now(*), last_disconnect_dttm = now(*) where process_name = 'RTMADTQ' and host = '" + m_NNBase.GetLocalPOP() + "'";
				myDBWriteCommand.CommandText = sCommand;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myDBWriteCommand.ExecuteNonQuery();
				myDBWriteConnection.Close();
			}
		}
		catch (ThreadAbortException)
		{
		}
		catch (Exception)
		{
		}
		if (m_NNBase.m_isLogging)
		{
			try
			{
				m_NNBase.StopLogging();
			}
			catch (ThreadAbortException)
			{
			}
			catch (Exception)
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
			AppErrorMsg = (bDBDisconnect ? "The database connection has been lost" : "Internal Error");
			m_NNBase.ReportErrorNoDB(bDBDisconnect ? "The database connection has been lost" : ("Exception " + e.GetType().ToString()), bDBDisconnect ? "E" : "C", when, from, details);
			ShutDown(bDBDisconnect ? "The database connection has been lost" : "Exception", "Protocol", bExit: true);
		}
	}

	public int SendString(string input)
	{
		m_outbuffer = Encoding.UTF8.GetBytes(input);
		int i = m_outbuffer.Length;
		Console.WriteLine("Sent {0} bytes to {2}{3}:\t{1}", i, input, m_portType, "   ");
		if (m_NNBase.m_isLogging)
		{
			string logMsg = RemoveAsciiControlChar(input);
			m_NNBase.log(logMsg, isXml: false, "HL7.SendString");
		}
		try
		{
			m_networkStream.Write(m_outbuffer, 0, i);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (IOException)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				string reason = "Connection Dropped - IOException";
				ShutDown(reason, "Protocol", bExit: true);
			}
		}
		catch (Exception e)
		{
			if (!m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(e, "Writing message", "SendString");
			}
		}
		return i;
	}

	private void ProtocolThread()
	{
		InitializeMSMQ();
		while (!m_isShutDown && !m_isShuttingDown)
		{
			OnReadComplete();
		}
	}

	private void InitializeMSMQ()
	{
		try
		{
			if (!MessageQueue.Exists(ConfigurationManager.AppSettings["queueName"]))
			{
				MessageQueue.Create(ConfigurationManager.AppSettings["queueName"], transactional: true);
				MessageQueue Queue = new MessageQueue(ConfigurationManager.AppSettings["queueName"]);
				Queue.SetPermissions("Administrators", MessageQueueAccessRights.FullControl, AccessControlEntryType.Allow);
			}
			CreateRejectQueue();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (MessageQueueException ex2)
		{
			ShutDown(ex2.Message, "Protocol", bExit: true);
		}
		catch (Exception ex3)
		{
			ShutDown(ex3.Message, "Protocol", bExit: true);
		}
	}

	private void CreateRejectQueue()
	{
		IntPtr targetHandle = IntPtr.Zero;
		string rejectQueuePath = "DIRECT=OS:" + ConfigurationManager.AppSettings["queueName"] + ";reject_queue";
		int result = MQMessageAPI.MQOpenQueue(rejectQueuePath, 4, 0, ref targetHandle);
		if (result < 0 && m_NNBase.m_isLogging)
		{
			m_NNBase.log("MQOpenQueue failed: " + result, isXml: false, m_NNBase.m_WhoAmI);
			m_NNBase.log(m_message, isXml: false, m_NNBase.m_WhoAmI);
		}
	}

	private string RemoveAsciiControlChar(string inputstring)
	{
		char newChar = ' ';
		return inputstring.Replace('\v', newChar).Replace('\u001c', newChar).Replace('\u0003', newChar)
			.Replace('\u0002', newChar);
	}
}
