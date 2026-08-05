using System;
using System.Collections;
using System.Configuration;
using System.Data.Odbc;
using System.Globalization;
using System.IO;
using System.Messaging;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Xml;
using Microsoft.Win32;
using NNClass;
using Patient;

namespace RTMADTP;

public class HL7Protocol : Protocol
{
	internal class MQMessageAPI
	{
		[DllImport("mqrt.dll", CharSet = CharSet.Unicode)]
		internal static extern int MQOpenQueue(string formatName, int access, int shareMode, ref IntPtr hQueue);

		[DllImport("mqrt.dll", CharSet = CharSet.Unicode)]
		internal static extern int MQMoveMessage(IntPtr sourceQueue, IntPtr targetQueue, long lookupID, IntPtr pTransaction);
	}

	private const char ASCII_VT = '\v';

	private const char ASCII_FS = '\u001c';

	private const char ASCII_CR = '\r';

	private const char ASCII_STX = '\u0002';

	private const char ASCII_ETX = '\u0003';

	private const string MaxDateTimeHL7 = "20371231000000";

	private const int MaxHL7Year = 2037;

	private const int MinHL7Year = 1800;

	private NNBase m_NNBase = new NNBase();

	private DateTime MaxHL7DateTime = new DateTime(2037, 12, 31, 0, 0, 0);

	private string m_message = "";

	private string AppRejectMsg = "";

	private string AppErrorMsg = "";

	private string AppWarningMsg = "";

	private string m_facility = "";

	private string m_facility_alias = "";

	private string m_facility_source = "";

	private string m_PrevFacility = "";

	private string m_prev_facility_alias = "";

	private string m_prev_facility_source = "";

	private string m_location = "";

	private string m_location_alias = "";

	private string m_facil_num = "";

	private string m_prev_facil_num = "";

	private string m_loc_num = "";

	private string m_new_visit_UUID = "";

	private RTMADTP m_parent;

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

	private string Race_HL7 = "";

	private string Race_DML = "";

	private string AccountNumber = "";

	private string AccountAssigningAuthority = "";

	private string AccountAssigningFacility = "";

	private string EventFacility = "";

	private string PatientClass = "";

	private string Location_PV1_3_1 = "";

	private string Room = "";

	private string Bed = "";

	private string Weight_DML_value = "";

	private string Weight_DML_units = "";

	private string Weight_HL7_value = "";

	private string Weight_HL7_units = "";

	private string Height_DML_value = "";

	private string Height_DML_units = "";

	private string Height_HL7_value = "";

	private string Height_HL7_units = "";

	private string Diagnosis = "";

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

	private string AttendingPhysician = "";

	private string ReportingPhysician = "";

	private string ConsultingPhysician = "";

	private string PatientType = "";

	private string VisitNumToAdd = "";

	private string VisitNumToFind = "";

	private string VisitNumFromADT = "";

	private string VisitNumFound = "";

	private string VisitUUID = "";

	private bool bUseVisitNumbersToFind = true;

	private bool bUseAccountNumbersToFind = true;

	private int iAccountCount;

	private int iPrevAccountCount;

	private int iVisitCount;

	private int iPrevVisitCount;

	private DateTime DischargeTime;

	private bool bDischargeFacilityTime;

	private string DischargeDateTime = "20371231000000";

	protected DateTime AdmitTime;

	protected string AdmitDateTime = "";

	private string PreviousMedicalRecordNumber = "";

	private string PreviousPatientAccount = "";

	private string PreviousPatientID = "";

	private string PreviousVisitNumToFind = "";

	private string PreviousVisitNumFromADT = "";

	private string PreviousVisitNumFound = "";

	private string PreviousVisitUUID = "";

	private string PreviousMRNAssigningAuthority = "";

	private string PreviousMRNAssigningFacility = "";

	private string PreviousAccountAssigningAuthority = "";

	private string PreviousAccountAssigningFacility = "";

	private string PreviousVisitNumAssigningAuthority = "";

	private string PreviousVisitNumAssigningFacility = "";

	private bool bNoVisitInfo;

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	private CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private DBPatient m_DBPatient;

	private PatientQuery m_PatientQuery;

	private PatientList m_PatientList;

	private PatientVisitRec m_PatientVisitRec;

	private PatientAccountRec m_PatientAccountRec;

	private PatientQuery m_PrevIDs_PatientQuery;

	private PatientList m_PrevIDs_PatientList;

	private PatientVisitRec m_PrevIDs_PatientVisitRec;

	private PatientAccountRec m_PrevIDs_PatientAccountRec;

	private DBPatient m_PrevIDs_DBPatient;

	private DBPatient m_newDBPatient;

	private PatientAccountRec m_newPatientAccountRec;

	private PatientVisitRec m_newPatientVisitRec;

	private bool bAccountExists;

	private bool bPrevAccountExists;

	private bool bAddPatient;

	private bool bUpdatePatient;

	private bool bAddAccount;

	private bool bUpdateAccount;

	private bool bAddVisit;

	private bool bVisitAdded;

	private bool bVisitMoved;

	private bool bAccountAdded;

	private bool bAccountMoved;

	private bool bPrevVisitDeleted;

	private bool bPrevAccountDeleted;

	private bool bPrevPatientDeleted;

	private bool bPatientAdded;

	private bool bUpdateVisit;

	private bool bAddVisitOK;

	private bool bAddAccountOK;

	private bool bMRNGiven;

	private bool bPatIDGiven;

	private bool bPatientKeysGiven;

	private bool bAccountKeysGiven;

	private bool bAccountRequired;

	private bool bVisitRequired;

	private bool bVisitKeysGiven;

	private bool bAccountGiven;

	private bool bVisitNumToAddGiven;

	private bool bVisitNumToFindGiven;

	private bool bVisitNumToFindExact;

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

	private bool bCrossFacilityTransfer;

	private bool bCrossFacilityTransferSameAccount;

	private bool bCrossFacilityTransferSameVisit;

	private bool bCrossFacilityMergePatient;

	private bool bCrossFacilityMergeAccount;

	private bool bCrossFacilityMergeSameAccount;

	private bool bCrossFacilityMergeVisit;

	private bool bCrossFacilityMergeSameVisit;

	private bool bCrossFacilityMoveAccount;

	private bool bCrossFacilityMoveSameAcccount;

	private bool bCrossFacilityMoveVisit;

	private bool bCrossFacilityMoveSameVisit;

	private bool bSingleFacility;

	private bool bFacilityFromList;

	private bool bNoAdmit;

	private string theoneFacilityNum;

	private string theoneFacility;

	private bool bPatIDToFind;

	private bool bMRNToFind;

	private bool bAccountToFind;

	private bool bVisitToFind;

	private bool bPrevIDsToFind;

	private bool bLocationRequired;

	private string lastFacilityNum;

	private string lastLocationNum;

	private bool bPrevAcctGiven;

	private bool bPrevOrNewAcctGiven;

	private bool bPrevVisitNumToFindGiven;

	private bool bPrevVisitNumToFindExact;

	private bool bPrevOrNewVisitNumGiven;

	private bool bPrevOrNewVisitNumGivenExact;

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

	private bool bInvalidLoc;

	private string oldlocnum;

	private string newlocnum;

	private bool bLocChange;

	private bool bPatientIDExists;

	private bool bPrevPatientIDExists;

	private bool bMRNExists;

	private bool bPrevMRNExists;

	private bool bPatientExists;

	private bool bPrevPatientExists;

	private bool bVisitExists;

	private bool bPrevVisitExists;

	private bool bVisitNumToFindDifferent;

	private bool bVisitNumChanging;

	private bool bVisitNumTheSame;

	private bool bPatientChanged;

	private bool bPatientVisitChanged;

	private bool bVisitUpdated;

	private bool bChangesMade;

	private bool bVisitIsActive;

	private bool bLatestActiveVisit;

	private bool bVisitIsFuture;

	private bool bMergePatient;

	private bool bMergeAccount;

	private bool bMergeVisit;

	private bool bMoveAccount;

	private bool bMoveVisit;

	private bool bDeactAccount;

	private bool bDeactVisit;

	private bool bRemoveMessageWhenDone = true;

	private DateTime newDischargeTime;

	private DateTime oldDischargeTime;

	public Thread m_ProtocolThread;

	private OdbcConnection myDBReadConnection;

	private OdbcCommand myDBReadCommand;

	private OdbcDataReader myDBReadReader;

	private OdbcConnection myDBWriteConnection;

	private OdbcCommand myDBWriteCommand;

	private OdbcTransaction myTransaction;

	private OdbcConnection myPTDBWriteConnection;

	private OdbcCommand myPTDBWriteCommand;

	private string myPTConnectString = "";

	private bool bMyPTDBAvailable;

	private PatientTrackingRec myPatientTrackingRec;

	protected string BinDir = "C:\\NovaBiomedical\\NovaNet\\Bin";

	private FileStream ConfigWriter;

	private byte[] writebuff;

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

	protected string sMale = "M";

	protected string sFemale = "F";

	protected bool bCurrentConfigIsOverall = true;

	protected bool bCurrentConfigIsFacility;

	protected bool bCurrentConfigIsLocation;

	protected bool bMultipleVisitsPerAccount = true;

	protected bool bAccountNumsCrossFacilities;

	protected bool bVisitNumsCrossPatients;

	protected bool bVisitNumsCrossFacilities;

	protected bool bCrossFacilityPatientTransfers;

	private bool m_b_loc_last_update_inst_class_column;

	private bool m_b_loc_last_update_inst_type_column;

	private bool m_b_patient_tracking_pt_uuid_column;

	private string m_TimeZoneName;

	private TimeZoneInfo m_TimeZoneInfo;

	protected FacilityList m_FacilityList;

	protected LocationList m_LocationList;

	protected PortParams m_Par;

	protected bool bFirstConfigLoad = true;

	private MessageQueue m_Queue;

	private Message m_qMsg;

	private IntPtr m_rejectQueneHandle = IntPtr.Zero;

	private bool bOK_File = true;

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
			ShutDown("Notify", "RTMADTP", bExit: false);
			break;
		}
		return false;
	}

	public HL7Protocol(bool logging, RTMADTP parent)
	{
		try
		{
			segmentparse = new HL7Parse();
			m_parent = parent;
			m_FacilityList = new FacilityList();
			m_LocationList = new LocationList();
			m_b_loc_last_update_inst_class_column = RTMADTP.m_b_loc_last_update_inst_class_column;
			m_b_loc_last_update_inst_type_column = RTMADTP.m_b_loc_last_update_inst_type_column;
			m_b_patient_tracking_pt_uuid_column = RTMADTP.m_b_patient_tracking_pt_uuid_column;
			m_NNBase.NNBaseOpen(logging, "HL7", "RTMADTP", "ADTP");
			m_NNBase.OpenDBConnection(ref myDBReadConnection, ref myDBReadCommand, 7);
			if (!m_NNBase.bDBAvailable)
			{
				return;
			}
			m_NNBase.OpenDBConnection(ref myDBWriteConnection, ref myDBWriteCommand, 7);
			if (!m_NNBase.bDBAvailable)
			{
				return;
			}
			myPTConnectString = "DSN=" + m_NNBase.PROFILETRACKDATASOURCE + ";UID=" + m_NNBase.PROFILETRACKUAUTHORITY + ";PWD=" + m_NNBase.PROFILETRACKPAUTHORITY;
			m_NNBase.OpenDBConnection(ref myPTDBWriteConnection, ref myPTDBWriteCommand, 7, myPTConnectString, ref bMyPTDBAvailable, "Profile_Track DB");
			if (!bMyPTDBAvailable)
			{
				return;
			}
			myPatientTrackingRec = new PatientTrackingRec();
			myPatientTrackingRec.Init(myPTDBWriteConnection, myPTDBWriteCommand, m_b_patient_tracking_pt_uuid_column);
			m_NNBase.CommAudit(10, "Connect", "");
			if (!m_NNBase.bDBAvailable)
			{
				return;
			}
			string sCommand = "update DBA.health_ping set update_time = now(*), last_connect_dttm = now(*) where process_name = 'RTMADTP' and host = '" + m_NNBase.GetLocalPOP() + "'";
			myDBWriteCommand.CommandText = sCommand;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myDBWriteCommand.ExecuteNonQuery();
			if (!m_NNBase.bDBAvailable)
			{
				return;
			}
			bSingleFacility = false;
			m_NNBase.bLocationAliasProcessingEnabled = true;
			if (m_NNBase.bDBAvailable)
			{
				if (m_FacilityList == null)
				{
					m_FacilityList = new FacilityList();
				}
				m_FacilityList.LoadFacilityList(m_NNBase);
				int iNumFacilities = m_FacilityList.GetNumFacilities();
				if (iNumFacilities == 0)
				{
					m_NNBase.ReportErrorDB("Facility list is empty", "E", "loading facility list", "HL7Protocol", "");
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
					m_FacilityList.GetFirstFacility(ref theoneFacility, ref theoneFacilityNum);
				}
			}
			if (m_NNBase.bDBAvailable)
			{
				if (m_LocationList == null)
				{
					m_LocationList = new LocationList();
				}
				m_LocationList.LoadLocationList(m_NNBase);
				if (m_LocationList.GetNumLocations() == 0)
				{
					m_NNBase.ReportErrorDB("Location list is empty", "E", "loading location list", "HL7Protocol", "");
					m_NNBase.bDBAvailable = false;
					if (!m_NNBase.m_isLogging)
					{
						m_NNBase.ForceLogging("NoLocationsInList");
						m_NNBase.log("There are no locations in the location list!", isXml: false, "HL7Protocol");
					}
				}
			}
			if (m_NNBase.bDBAvailable)
			{
				LoadConfigFile(ref bFirstConfigLoad);
			}
			if (m_NNBase.bDBAvailable)
			{
				LoadAutoDischargeTables();
			}
			if (m_NNBase.bDBAvailable)
			{
				m_PatientQuery = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
				m_PrevIDs_PatientQuery = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
				m_PrevIDs_PatientList = new PatientList();
				m_DBPatient = new DBPatient(m_b_loc_last_update_inst_class_column, m_b_loc_last_update_inst_type_column);
				m_PatientList = new PatientList();
				m_PatientVisitRec = new PatientVisitRec();
				m_PatientAccountRec = new PatientAccountRec();
				m_PrevIDs_PatientVisitRec = new PatientVisitRec();
				m_PrevIDs_PatientAccountRec = new PatientAccountRec();
				m_PrevIDs_DBPatient = new DBPatient(m_b_loc_last_update_inst_class_column, m_b_loc_last_update_inst_type_column);
				m_newDBPatient = new DBPatient(m_b_loc_last_update_inst_class_column, m_b_loc_last_update_inst_type_column);
				m_newPatientVisitRec = new PatientVisitRec();
				m_newPatientAccountRec = new PatientAccountRec();
				m_ProtocolThread = new Thread(ProtocolThread);
				m_ProtocolThread.Start();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("HL7Protocol - constructor");
		}
		catch (Exception e)
		{
			handleException(e, "start protocol", "HL7Protocol", bMoveMessage: false);
		}
	}

	public void AddSetting(ref XmlDocument configdoc, string Variable, string Value, bool addNew = false)
	{
		XmlElement elemParent = (XmlElement)configdoc.SelectSingleNode("root");
		XmlElement elemChild;
		if (!addNew)
		{
			elemChild = FindOrAddNodeByAttribute(ref configdoc, ref elemParent, "Setting", "Variable", Variable, "");
		}
		else
		{
			elemChild = configdoc.CreateElement("Setting");
			elemChild.SetAttribute("Variable", Variable);
		}
		elemChild.SetAttribute("Value", Value);
		elemParent.AppendChild(elemChild);
	}

	public XmlElement FindOrAddNodeByAttribute(ref XmlDocument doc, ref XmlElement root, string name, string attribute, string aValue, string text)
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

	private bool LoadConfigFile(ref bool bFirstTime)
	{
		bool bOK_Reg = true;
		bool bOK_DB = true;
		bool bOK = true;
		bool bPortHasChanged = false;
		OdbcDataReader myDBReadReader = null;
		bool bReaderOpen = false;
		bool bConfigReadFromDB = false;
		PortParams par = default(PortParams);
		if (bFirstTime)
		{
			configdoc = new XmlDocument();
			InitializeConfig();
			try
			{
				BinDir = Registry.LocalMachine.OpenSubKey(m_NNBase.REGISTRY_SUBKEY_RTM).GetValue("BinDir").ToString() + "\\";
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("LoadConfigFile");
			}
			catch (Exception e)
			{
				m_NNBase.ReportException(e, "Getting registry entry for bin folder", "LoadConfigFile");
				bOK = (bOK_Reg = false);
			}
		}
		if (bOK_Reg && m_NNBase.bDBAvailable)
		{
			bOK = GetPortInfo(ref par, ref bOK_DB);
			if (bOK && (bFirstTime || (bPortHasChanged = PortHasChanged(m_Par, par))))
			{
				configdoc.LoadXml("<root></root>");
				AddSetting(ref configdoc, "Port.protocol", par.protocol);
				AddSetting(ref configdoc, "Port.portType", par.portType);
				AddSetting(ref configdoc, "Port.commProtocol", par.commProtocol);
				AddSetting(ref configdoc, "Port.portNum", par.portNum.ToString());
				AddSetting(ref configdoc, "Port.connectRemote", par.connectRemote.ToString());
				AddSetting(ref configdoc, "Port.used", par.used);
				AddSetting(ref configdoc, "Port.multiConnect", par.multiConnect);
				AddSetting(ref configdoc, "Port.ipAddress0", par.ipAddress[0].ToString());
				AddSetting(ref configdoc, "Port.ipAddress1", par.ipAddress[1].ToString());
				AddSetting(ref configdoc, "Port.ipAddress2", par.ipAddress[2].ToString());
				AddSetting(ref configdoc, "Port.ipAddress3", par.ipAddress[3].ToString());
				AddSetting(ref configdoc, "Port.portActive", par.portActive.ToString());
				AddSetting(ref configdoc, "Port.remoteHostName", par.remoteHostName);
				AddSetting(ref configdoc, "Port.remotePort", par.remotePort.ToString());
			}
			if (bOK && (bFirstTime || bPortHasChanged))
			{
				string m_FacilityAlias = "";
				string dummy = "";
				m_FacilityList.GetFirstFacilityAlias(ref m_FacilityAlias, ref dummy);
				if (m_FacilityAlias.Length > 0)
				{
					while (m_FacilityAlias.Length > 0)
					{
						AddSetting(ref configdoc, "Facility", m_FacilityAlias, addNew: true);
						m_FacilityList.GetNextFacilityAlias(ref m_FacilityAlias, ref dummy);
					}
				}
			}
			if (bOK)
			{
				if (bFirstTime || bPortHasChanged)
				{
					try
					{
						myDBReadCommand.CommandText = "select pc_key, pc_value, fac_uuid, loc_uuid from DBA.process_control where pc_process = 'ADT'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
						}
						myDBReadReader = myDBReadCommand.ExecuteReader();
						bReaderOpen = true;
						string variable = "";
						string value = "";
						string fac_uuid = "";
						string loc_uuid = "";
						while (myDBReadReader.Read())
						{
							int i = 0;
							variable = (myDBReadReader.IsDBNull(i) ? "" : myDBReadReader.GetString(i));
							i++;
							value = (myDBReadReader.IsDBNull(i) ? "" : myDBReadReader.GetString(i));
							i++;
							fac_uuid = (myDBReadReader.IsDBNull(i) ? "" : myDBReadReader.GetString(i));
							i++;
							loc_uuid = (myDBReadReader.IsDBNull(i) ? "" : myDBReadReader.GetString(i));
							i++;
							if (loc_uuid.Length > 0)
							{
								variable = variable + "_" + loc_uuid;
							}
							else if (fac_uuid.Length > 0)
							{
								variable = variable + "_" + fac_uuid;
							}
							XmlElement elemParent = (XmlElement)configdoc.SelectSingleNode("root");
							XmlElement elemChild = configdoc.CreateElement("Setting");
							elemChild.SetAttribute("Variable", variable);
							elemChild.SetAttribute("Value", value);
							elemParent.AppendChild(elemChild);
						}
						bConfigReadFromDB = true;
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("LoadConfigFile");
					}
					catch (XmlException e2)
					{
						m_NNBase.ReportXMLException(e2, "loading configuration from database", "LoadConfigFile");
						bOK = (bOK_DB = false);
					}
					catch (OdbcException e3)
					{
						m_NNBase.ReportDBException(e3, "loading configuration from database", "LoadConfigFile");
						bOK = (bOK_DB = false);
					}
					catch (Exception e4)
					{
						m_NNBase.ReportException(e4, "loading configuration from database", "LoadConfigFile");
						bOK = (bOK_DB = false);
					}
				}
				if (bReaderOpen)
				{
					myDBReadReader.Close();
				}
				if (bOK_DB && bConfigReadFromDB && (bFirstTime || bPortHasChanged))
				{
					string NewConfigFileName = "";
					try
					{
						try
						{
							NewConfigFileName = BinDir + "RTMADT_" + DateTime.Now.ToString("MMddHHmmssff") + ".xml";
							ConfigWriter = new FileStream(NewConfigFileName, FileMode.CreateNew);
							bOK = (bOK_File = true);
						}
						catch (ThreadAbortException)
						{
							handleThreadAbortException("LoadConfigFile");
						}
						catch (Exception e5)
						{
							if (ConfigWriter != null)
							{
								ConfigWriter.Close();
							}
							ConfigWriter = null;
							m_NNBase.ReportException(e5, "creating file " + NewConfigFileName, "LoadConfigFile");
							bOK = (bOK_File = false);
						}
						if (bOK_File)
						{
							bOK = (bOK_File = false);
							string input = configdoc.OuterXml;
							writebuff = Encoding.UTF8.GetBytes(input);
							int i2 = writebuff.Length;
							ConfigWriter.Write(writebuff, 0, i2);
							ConfigWriter.Flush();
							bOK = (bOK_File = true);
						}
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("LoadConfigFile");
					}
					catch (XmlException e6)
					{
						m_NNBase.ReportXMLException(e6, "writing config file", "LoadConfigFile");
						bOK = (bOK_File = false);
					}
					catch (Exception e7)
					{
						m_NNBase.ReportException(e7, "writing config file", "LoadConfigFile");
						bOK = (bOK_File = false);
					}
					if (ConfigWriter != null)
					{
						ConfigWriter.Close();
					}
					if (bOK_File)
					{
						try
						{
							if (File.Exists(BinDir + "RTMADT.XML"))
							{
								DateTime ConfigFileCreationDate = File.GetCreationTime(BinDir + "RTMADT.XML");
								string removedFileName = BinDir + "RTMADT_" + ConfigFileCreationDate.ToString("MMddHHmmssff") + "_Removed.XML";
								if (File.Exists(removedFileName))
								{
									File.Delete(removedFileName);
								}
								File.Move(BinDir + "RTMADT.XML", removedFileName);
							}
						}
						catch (ThreadAbortException)
						{
							handleThreadAbortException("LoadConfigFile");
						}
						catch (Exception e8)
						{
							m_NNBase.ReportException(e8, "renaming existing config file", "LoadConfigFile");
							bOK = (bOK_File = false);
						}
						if (bOK_File)
						{
							try
							{
								File.Move(NewConfigFileName, BinDir + "RTMADT.XML");
							}
							catch (ThreadAbortException)
							{
								handleThreadAbortException("LoadConfigFile");
							}
							catch (Exception e9)
							{
								m_NNBase.ReportException(e9, "renaming new config file", "LoadConfigFile");
								bOK = (bOK_File = false);
							}
						}
					}
				}
			}
		}
		if (bOK)
		{
			if (bFirstTime)
			{
				bOK = GetOverAllConfig();
			}
			if (bOK && (bFirstTime || bPortHasChanged))
			{
				m_Par = par;
				bFirstTime = false;
			}
		}
		else if (m_NNBase.bDBAvailable)
		{
			ShutDown("Error loading configuration", "LoadConfigFile", bExit: true);
		}
		return bOK;
	}

	private bool GetPortInfo(ref PortParams par, ref bool bOK_DB)
	{
		bool bOK = false;
		string selectlist = "SELECT Protocol, Port_type, Comm_Protocol, Port_Num, Connect_Remote, used, Multi_connect, IP_address, Port_Active, Remote_Host_Name, remote_port, comm_record_num FROM DBA.Communications";
		OdbcDataReader myDBReadReader = null;
		bool bReaderOpen = false;
		try
		{
			myDBReadCommand.CommandText = selectlist + " where from_ui = 'T' and Port_type = 'ADT' and computer_name = '" + m_NNBase.GetHostName() + "'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			bReaderOpen = true;
			par.ipAddress = new byte[4];
			if (myDBReadReader.Read())
			{
				if (!myDBReadReader.IsDBNull(0))
				{
					par.protocol = myDBReadReader.GetString(0);
				}
				if (!myDBReadReader.IsDBNull(1))
				{
					par.portType = myDBReadReader.GetString(1);
				}
				if (!myDBReadReader.IsDBNull(2))
				{
					par.commProtocol = myDBReadReader.GetString(2);
				}
				if (!myDBReadReader.IsDBNull(3))
				{
					par.portNum = myDBReadReader.GetInt32(3);
				}
				if (!myDBReadReader.IsDBNull(4))
				{
					par.connectRemote = myDBReadReader.GetInt32(4);
				}
				if (!myDBReadReader.IsDBNull(5))
				{
					par.used = myDBReadReader.GetString(5);
				}
				else
				{
					par.used = "T";
				}
				if (!myDBReadReader.IsDBNull(6))
				{
					par.multiConnect = myDBReadReader.GetString(6);
				}
				if (!myDBReadReader.IsDBNull(7))
				{
					uint i = (uint)myDBReadReader.GetInt32(7);
					byte[] b = new byte[4];
					par.ipAddress[0] = (b[3] = (byte)((i >> 24) & 0xFF));
					par.ipAddress[1] = (b[2] = (byte)((i >> 16) & 0xFF));
					par.ipAddress[2] = (b[1] = (byte)((i >> 8) & 0xFF));
					par.ipAddress[3] = (b[0] = (byte)(i & 0xFF));
				}
				if (!myDBReadReader.IsDBNull(8))
				{
					par.portActive = myDBReadReader.GetInt32(8);
				}
				if (!myDBReadReader.IsDBNull(9))
				{
					par.remoteHostName = myDBReadReader.GetString(9);
				}
				if (!myDBReadReader.IsDBNull(10))
				{
					par.remotePort = myDBReadReader.GetInt32(10);
				}
				bOK = true;
			}
			else
			{
				m_NNBase.ReportErrorDB("There is no ADT port defined", "E", "loading ADT Port configuration from database", "GetPortInfo", "");
				bOK = false;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetPortInfo");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "loading ADT Port configuration from database", "GetPortInfo", bMoveMessage: false);
			bOK = (bOK_DB = false);
		}
		catch (Exception e2)
		{
			handleException(e2, "loading ADT Port configuration from database", "GetPortInfo", bMoveMessage: false);
			bOK = (bOK_DB = false);
		}
		if (bReaderOpen)
		{
			myDBReadReader.Close();
		}
		return bOK;
	}

	private bool PortHasChanged(PortParams oldpar, PortParams newpar)
	{
		if (oldpar.portNum == newpar.portNum && oldpar.remotePort == newpar.remotePort && oldpar.connectRemote == newpar.connectRemote && !(oldpar.protocol != newpar.protocol) && !(oldpar.portType != newpar.portType) && !(oldpar.commProtocol != newpar.commProtocol) && !((oldpar.portActive != newpar.portActive) | (oldpar.remoteHostName != newpar.remoteHostName) | (oldpar.ipAddress[0] != newpar.ipAddress[0])) && oldpar.ipAddress[1] == newpar.ipAddress[1] && oldpar.ipAddress[2] == newpar.ipAddress[2])
		{
			return oldpar.ipAddress[3] != newpar.ipAddress[3];
		}
		return true;
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
						m_NNBase.ReportErrorDB("Invalid value for MRNsCrossFacilities", "E", "parsing MRNsCrossFacilities", "GetOverAllConfig", "");
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
							m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "GetOverAllConfig", "");
							bOK = false;
						}
						if (bOK && numvalueparts > iAcctFldPart + 1)
						{
							if (isNumeric(valueparts[iAcctFldPart + 1], NumberStyles.Integer))
							{
								iAccountComponent = (uint)Convert.ToInt32(valueparts[iAcctFldPart + 1]);
								continue;
							}
							m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "GetOverAllConfig", "");
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
						sSupportedTransactions = value;
					}
					else if (Comp.Compare(variable, "Male", CompOpt) == 0)
					{
						sMale = value;
					}
					else if (Comp.Compare(variable, "Female", CompOpt) == 0)
					{
						sFemale = value;
					}
					else if (Comp.Compare(variable, "CrossFacilityPatientTransfers", CompOpt) == 0 && Comp.Compare(value, "T", CompOpt) == 0)
					{
						bCrossFacilityPatientTransfers = true;
					}
				}
				SaveOverallConfig();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetOverAllConfig");
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
			ShutDown("Error loading configuration", "LoadConfigFile", bExit: true);
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
		bMultipleVisitsPerAccount = true;
		bAccountNumsCrossFacilities = false;
		bVisitNumsCrossPatients = false;
		bVisitNumsCrossFacilities = false;
		bCrossFacilityPatientTransfers = false;
		m_ActiveHours = 24;
		m_NNBase.bLocationAliasProcessingEnabled = true;
	}

	private void SaveConfig(ref ADTConfiguration myConfig)
	{
		myConfig.sAccountSegment = sAccountSegment;
		myConfig.iAccountField = iAccountField;
		myConfig.iAccountComponent = iAccountComponent;
		myConfig.sAdmitOnUpdateTypes = sAdmitOnUpdateTypes;
		myConfig.sSupportedTransactions = sSupportedTransactions;
		myConfig.sMale = sMale;
		myConfig.sFemale = sFemale;
		myConfig.bMultipleVisitsPerAccount = bMultipleVisitsPerAccount;
		myConfig.bVisitNumsCrossPatients = bVisitNumsCrossPatients;
		myConfig.m_ActiveHours = m_ActiveHours;
	}

	private void RestoreConfig(ADTConfiguration myConfig)
	{
		sAccountSegment = myConfig.sAccountSegment;
		iAccountField = myConfig.iAccountField;
		iAccountComponent = myConfig.iAccountComponent;
		sAdmitOnUpdateTypes = myConfig.sAdmitOnUpdateTypes;
		sSupportedTransactions = myConfig.sSupportedTransactions;
		sMale = myConfig.sMale;
		sFemale = myConfig.sFemale;
		bMultipleVisitsPerAccount = myConfig.bMultipleVisitsPerAccount;
		bVisitNumsCrossPatients = myConfig.bVisitNumsCrossPatients;
		m_ActiveHours = myConfig.m_ActiveHours;
	}

	private void LoadAutoDischargeTables()
	{
		sDischargeOPClassOrTypeByFacil = "";
		sDischargeOPClassOrTypeByLoc = "";
		try
		{
			myDBReadCommand.CommandText = "select loc_num, class_type, adt_id, retain_hours from DBA.auto_discharge_fac";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			while (myDBReadReader.Read())
			{
				string fac_num = myDBReadReader.GetString(0);
				string class_type = myDBReadReader.GetString(1);
				string adt_id = myDBReadReader.GetString(2);
				string retain_hours = myDBReadReader.GetString(3);
				if (sDischargeOPClassOrTypeByFacil.Length > 0)
				{
					sDischargeOPClassOrTypeByFacil += "|";
				}
				sDischargeOPClassOrTypeByFacil += fac_num;
				sDischargeOPClassOrTypeByFacil = sDischargeOPClassOrTypeByFacil + "^" + class_type;
				sDischargeOPClassOrTypeByFacil = sDischargeOPClassOrTypeByFacil + "^" + adt_id;
				sDischargeOPClassOrTypeByFacil = sDischargeOPClassOrTypeByFacil + "^" + retain_hours;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("LoadAutoDischargeTables");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "loading auto discharge by facility list", "LoadAutoDischargeTables", bMoveMessage: false);
		}
		catch (Exception e2)
		{
			handleException(e2, "loading auto discharge by facility list", "LoadAutoDischargeTables", bMoveMessage: false);
		}
		try
		{
			myDBReadCommand.CommandText = "select l.loc_num, f.class_type, f.adt_id, l.retain_hours from DBA.auto_discharge_fac f join DBA.auto_discharge_loc l on l.parent_uuid = f.uuid";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			while (myDBReadReader.Read())
			{
				string loc_num = myDBReadReader.GetString(0);
				string class_type2 = myDBReadReader.GetString(1);
				string adt_id2 = myDBReadReader.GetString(2);
				string retain_hours2 = myDBReadReader.GetString(3);
				if (sDischargeOPClassOrTypeByLoc.Length > 0)
				{
					sDischargeOPClassOrTypeByLoc += "|";
				}
				sDischargeOPClassOrTypeByLoc += loc_num;
				sDischargeOPClassOrTypeByLoc = sDischargeOPClassOrTypeByLoc + "^" + class_type2;
				sDischargeOPClassOrTypeByLoc = sDischargeOPClassOrTypeByLoc + "^" + adt_id2;
				sDischargeOPClassOrTypeByLoc = sDischargeOPClassOrTypeByLoc + "^" + retain_hours2;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("LoadAutoDischargeTables");
		}
		catch (OdbcException e3)
		{
			handleDBException(e3, "loading auto discharge by location list", "LoadAutoDischargeTables", bMoveMessage: false);
		}
		catch (Exception e4)
		{
			handleException(e4, "loading auto discharge by location list", "LoadAutoDischargeTables", bMoveMessage: false);
		}
	}

	public bool GetFacilityConfig()
	{
		bool bOK = true;
		try
		{
			if (m_facil_num.Length == 0 && m_facility.Length > 0)
			{
				m_FacilityList.LookupFacilNum(m_facility, ref m_facil_num);
			}
			if (m_facil_num.Length > 0 && (Comp.Compare(m_facil_num, lastFacilityNum, CompOpt) != 0 || !bCurrentConfigIsFacility) && configdoc != null)
			{
				RestoreOverallConfig();
				XmlElement root = configdoc.DocumentElement;
				XmlNodeList nodeList = root.SelectNodes("Setting");
				foreach (XmlNode varval in nodeList)
				{
					XmlElement elem = (XmlElement)varval;
					string variable = elem.GetAttribute("Variable");
					string value = elem.GetAttribute("Value");
					if (Comp.Compare(variable, "AccountField_" + m_facil_num, CompOpt) == 0)
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
							m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "GetFacilityConfig", "");
							bOK = false;
						}
						if (bOK && numvalueparts > iAcctFldPart + 1)
						{
							if (isNumeric(valueparts[iAcctFldPart + 1], NumberStyles.Integer))
							{
								iAccountComponent = (uint)Convert.ToInt32(valueparts[iAcctFldPart + 1]);
								continue;
							}
							m_NNBase.ReportErrorDB("Invalid account field format", "E", "parsing AccountField", "GetFacilityConfig", "");
							bOK = false;
						}
					}
					else if (Comp.Compare(variable, "AdmitOnUpdateTypes_" + m_facil_num, CompOpt) == 0)
					{
						sAdmitOnUpdateTypes = value;
					}
					else if (Comp.Compare(variable, "MultipleVisitsPerAccount_" + m_facil_num, CompOpt) == 0)
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
					else if (Comp.Compare(variable, "VisitNumbersCrossPatients_" + m_facil_num, CompOpt) == 0 && Comp.Compare(value, "T", CompOpt) == 0)
					{
						bVisitNumsCrossPatients = true;
					}
					else if (Comp.Compare(variable, "ActiveHours_" + m_facil_num, CompOpt) == 0)
					{
						if (isNumeric(value, NumberStyles.Integer))
						{
							m_ActiveHours = Convert.ToInt32(value);
						}
					}
					else if (Comp.Compare(variable, "SupportedTransactions_" + m_facil_num, CompOpt) == 0)
					{
						sSupportedTransactions = value;
					}
					else if (Comp.Compare(variable, "Male_" + m_facil_num, CompOpt) == 0)
					{
						sMale = value;
					}
					else if (Comp.Compare(variable, "Female_" + m_facil_num, CompOpt) == 0)
					{
						sFemale = value;
					}
				}
				SaveFacilityConfig();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetFacilityConfig");
		}
		catch (XmlException e)
		{
			m_NNBase.ReportXMLException(e, "Getting Facility Configuration", "GetFacilityConfig");
			bOK = false;
		}
		catch (Exception e2)
		{
			m_NNBase.ReportException(e2, "Getting Facility Configuration", "GetFacilityConfig");
			bOK = false;
		}
		if (!bOK)
		{
			ShutDown("Error loading configuration", "GetFacilityConfig", bExit: true);
		}
		return bOK;
	}

	public void SaveOverallConfig()
	{
		SaveConfig(ref myOverAllConfig);
		bCurrentConfigIsOverall = true;
		bCurrentConfigIsFacility = false;
		bCurrentConfigIsLocation = false;
	}

	public void RestoreOverallConfig()
	{
		RestoreConfig(myOverAllConfig);
		bCurrentConfigIsOverall = true;
		bCurrentConfigIsFacility = false;
		bCurrentConfigIsLocation = false;
	}

	public void SaveFacilityConfig()
	{
		SaveConfig(ref myFacilityConfig);
		bCurrentConfigIsOverall = false;
		bCurrentConfigIsFacility = true;
		bCurrentConfigIsLocation = false;
		lastFacilityNum = m_facil_num;
	}

	public void RestoreFacilityConfig()
	{
		RestoreConfig(myFacilityConfig);
		bCurrentConfigIsOverall = false;
		bCurrentConfigIsFacility = true;
		bCurrentConfigIsLocation = false;
	}

	public bool GetLocationConfig()
	{
		bool bOK = true;
		try
		{
			if (m_loc_num.Length == 0 && m_location.Length > 0 && m_facil_num.Length > 0)
			{
				m_LocationList.LookupLocNum(m_location, m_facil_num, ref m_loc_num);
			}
			if (m_loc_num.Length > 0 && Comp.Compare(m_loc_num, lastLocationNum, CompOpt) != 0 && configdoc != null)
			{
				if (lastFacilityNum == m_facil_num)
				{
					RestoreFacilityConfig();
				}
				else
				{
					RestoreOverallConfig();
				}
				XmlElement root = configdoc.DocumentElement;
				XmlNodeList nodeList = root.SelectNodes("Setting");
				foreach (XmlNode varval in nodeList)
				{
					XmlElement elem = (XmlElement)varval;
					string variable = elem.GetAttribute("Variable");
					string value = elem.GetAttribute("Value");
					if (Comp.Compare(variable, "ActiveHours_" + m_loc_num, CompOpt) == 0 && isNumeric(value, NumberStyles.Integer))
					{
						m_ActiveHours = Convert.ToInt32(value);
					}
				}
				lastLocationNum = m_loc_num;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetLocationConfig");
		}
		catch (XmlException e)
		{
			m_NNBase.ReportXMLException(e, "Getting Location Configuration", "GetLocationConfig");
			bOK = false;
		}
		catch (Exception e2)
		{
			m_NNBase.ReportException(e2, "Getting Location Configuration", "GetLocationConfig");
			bOK = false;
		}
		if (!bOK)
		{
			ShutDown("Error loading configuration", "GetFacilityConfig", bExit: true);
		}
		return bOK;
	}

	public override void ProcessMessage()
	{
		bool bOK = true;
		bool bSplitMessage = false;
		bRemoveMessageWhenDone = true;
		string MSHSegment = "";
		string PIDSegment = "";
		int iCountPID = 0;
		int iCountMRG = 0;
		int iPIDNum = -1;
		int iMRGNum = -1;
		int iSubMessage = -1;
		ArrayList SubMessage = new ArrayList();
		AppRejectMsg = "";
		AppErrorMsg = "";
		AppWarningMsg = "";
		try
		{
			bool bDone = false;
			int iPass = 0;
			while (!bDone && iPass < 2)
			{
				InitMessageFields();
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
							if (iPass == 0 && m_NNBase.m_isLogging)
							{
								m_NNBase.log(MessageSubType + " - " + MessageSubTypeDescription(MessageSubType), isXml: false, "ProcessMessage");
							}
							bOK &= MessageSubTypeSupported(MessageSubType);
							if (!bOK)
							{
								bDone = true;
							}
							MSHSegment = segment;
							if (MessageSubType == "A17")
							{
								MSHSegment.Replace(segmentparse.FieldDelim + "ADT" + segmentparse.ComponentDelim + "A17", segmentparse.FieldDelim + "ADT" + segmentparse.ComponentDelim + "A02");
							}
							break;
						case "EVN":
							ProcessEventSegment(segment);
							break;
						case "PID":
							if (iPass == 0)
							{
								iCountPID++;
								if (iCountPID > 1)
								{
									bSplitMessage = true;
								}
							}
							else if (bSplitMessage)
							{
								iPIDNum++;
								if (iSubMessage < iPIDNum)
								{
									iSubMessage = iPIDNum;
								}
								PIDSegment = segment;
								if (iSubMessage > SubMessage.Count - 1)
								{
									SubMessage.Add("");
								}
								SubMessage[iSubMessage] = MSHSegment + '\r' + PIDSegment + '\r';
							}
							else
							{
								ProcessPatientIdentificationSegment(segment);
							}
							break;
						case "PV1":
							if (iPass == 1)
							{
								if (bSplitMessage)
								{
									ArrayList arrayList4;
									int index4;
									(arrayList4 = SubMessage)[index4 = iSubMessage] = string.Concat(arrayList4[index4], segment, '\r');
									break;
								}
								bNoVisitInfo = false;
								ProcessPatientVisitSegment(segment);
							}
							break;
						case "OBX":
							if (iPass == 1)
							{
								if (bSplitMessage)
								{
									ArrayList arrayList2;
									int index2;
									(arrayList2 = SubMessage)[index2 = iSubMessage] = string.Concat(arrayList2[index2], segment, '\r');
								}
								else
								{
									ProcessResultSegment(segment);
								}
							}
							break;
						case "DG1":
							if (iPass == 1)
							{
								if (bSplitMessage)
								{
									ArrayList arrayList3;
									int index3;
									(arrayList3 = SubMessage)[index3 = iSubMessage] = string.Concat(arrayList3[index3], segment, '\r');
								}
								else
								{
									ProcessDiagnosisSegment(segment);
								}
							}
							break;
						case "MRG":
							if (iPass == 0)
							{
								iCountMRG++;
								if (iCountMRG > 1)
								{
									bSplitMessage = true;
								}
							}
							else if (bSplitMessage)
							{
								iMRGNum++;
								if (iSubMessage < iMRGNum)
								{
									iSubMessage = iMRGNum;
								}
								if (iSubMessage > SubMessage.Count - 1)
								{
									SubMessage.Add("");
								}
								SubMessage[iSubMessage] = MSHSegment + '\r' + PIDSegment + '\r';
								ArrayList arrayList;
								int index;
								(arrayList = SubMessage)[index = iSubMessage] = string.Concat(arrayList[index], segment, '\r');
							}
							else
							{
								bOK &= ProcessMergeSegment(segment);
								if (!bOK)
								{
									bDone = true;
								}
							}
							break;
						}
					}
					if (i > 0 && i < m_message.Length)
					{
						iLast = i + 1;
						i = m_message.IndexOf('\r', iLast);
						if (i < 0 && iPass > 0)
						{
							bDone = true;
						}
					}
				}
				iPass++;
			}
			if (bOK)
			{
				if (bSplitMessage)
				{
					for (int j = 0; j <= iSubMessage; j++)
					{
						ProcessSubMessage((string)SubMessage[j]);
					}
				}
				else
				{
					ProcessParsedMessage();
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessMessage");
		}
		catch (Exception e)
		{
			handleException(e, "processing ADT message", "ProcessMessage", bMoveMessage: true);
		}
		if (bRemoveMessageWhenDone)
		{
			RemoveMessageFromQueue();
		}
		m_message = "";
		m_parent.m_iNumMessages++;
		m_parent.m_iTotMessages++;
	}

	private void ProcessParsedMessage()
	{
		try
		{
			myPatientTrackingRec.Begin(m_NNBase, PatientID, MedicalRecordNumber, AccountNumber, MessageSubType, PreviousPatientID, PreviousMedicalRecordNumber, PreviousPatientAccount);
			if (ADTMessageOK("", ""))
			{
				if (bSpansFacilities)
				{
					myPatientTrackingRec.Commit(m_NNBase);
					bNoAdmit = true;
					ArrayList myFacilities = null;
					ArrayList myFacilityNums = null;
					ArrayList myMedrecNums = null;
					string where = "";
					if (bPrevPatIDGiven)
					{
						where = "p.Patient_ID = '" + PreviousPatientID + "'";
					}
					if (bPrevMRNGiven)
					{
						if (where.Length > 0)
						{
							where += " and ";
						}
						where = where + "p.medrec_num = '" + PreviousMedicalRecordNumber + "'";
					}
					bool bMRNWasGiven = bMRNGiven;
					bool bPrevMRNWasGiven = bPrevMRNGiven;
					m_PatientQuery.GetPatientFacilityList(m_NNBase, ref myFacilities, ref myFacilityNums, ref myMedrecNums, where, ref myDBReadCommand);
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("The previous patient ID and/or MRN was found in " + myFacilities.Count + " facilities.", isXml: false, "ProcessParsedMessage");
					}
					if (!bMRNsCrossFacilities && bPrevMRNWasGiven && myFacilities.Count > 1)
					{
						m_NNBase.ReportErrorDB(AppErrorMsg = "Unable to determine the facility for the MRN merge transaction", "E", "searching for patients with the previous MRN", "ProcessParsedMessage", "The previous MRN was found in more than one facility and MRNs do not cross facilities");
						return;
					}
					for (int i = 0; i < myFacilities.Count; i++)
					{
						m_facility = (string)myFacilities[i];
						m_facil_num = (string)myFacilityNums[i];
						bFacilityFromList = true;
						if (!bMRNsCrossFacilities)
						{
							if (!bPrevMRNWasGiven)
							{
								PreviousMedicalRecordNumber = (string)myMedrecNums[i];
							}
							if (!bMRNWasGiven)
							{
								MedicalRecordNumber = (string)myMedrecNums[i];
							}
						}
						ProcessMessageForFacility(m_facility, m_facil_num);
					}
				}
				else if (OKToAddOrUpdatePatient())
				{
					ProcessADTMessage();
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessParsedMessage");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "Processing parsed message", "ProcessParsedMessage", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing parsed message", "ProcessParsedMessage", bMoveMessage: true);
		}
		myPatientTrackingRec.Commit(m_NNBase);
	}

	private bool bPrevAndNewFacilitiesDifferent()
	{
		return (m_facil_num.Length > 0) & (m_prev_facil_num.Length > 0) & (m_facil_num != m_prev_facil_num);
	}

	private bool DetermineCrossFacilityActions()
	{
		bool bOK = true;
		bCrossFacilityTransfer = false;
		bCrossFacilityMergePatient = false;
		bCrossFacilityMergeAccount = false;
		bCrossFacilityMergeVisit = false;
		bCrossFacilityMoveAccount = false;
		bCrossFacilityMoveVisit = false;
		bCrossFacilityTransferSameAccount = false;
		bCrossFacilityTransferSameVisit = false;
		bCrossFacilityMergeSameAccount = false;
		bCrossFacilityMergeSameVisit = false;
		bCrossFacilityMoveSameAcccount = false;
		bCrossFacilityMoveSameVisit = false;
		if (bPrevAndNewFacilitiesDifferent())
		{
			switch (MessageSubType)
			{
			case "A02":
			case "A06":
			case "A07":
			case "A08":
			case "A31":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityTransfer = (bMRNDifferent | bMRNsCrossFacilities) & (bAcctDifferent | bAccountNumsCrossFacilities) & (bVisitNumChanging | bVisitNumsCrossFacilities));
					if (!bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityTransferFailure("A transfer between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent && !bAccountNumsCrossFacilities)
						{
							CrossFacilityTransferFailure("A transfer between facilities cannot be made without an Account change where Accounts don't cross facilities");
						}
						if (!bVisitNumChanging && !bVisitNumsCrossFacilities)
						{
							CrossFacilityTransferFailure("A transfer between facilities cannot be made without a Visit number change where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityTransferFailure("Cross-facility patient transfers are not enabled");
				}
				break;
			case "A18":
			case "A36":
				if (bCrossFacilityPatientTransfers)
				{
					bCrossFacilityMergePatient = (bMRNDifferent | (!bMRNDifferent & bMRNsCrossFacilities & bPatIDDifferent)) & (bAcctDifferent | bAccountNumsCrossFacilities) & bVisitNumsCrossFacilities;
					bCrossFacilityMergeAccount = bVisitNumsCrossFacilities & (bAcctDifferent | ((((MessageSubType == "A36") & bCrossFacilityMergePatient) | (MessageSubType == "A18")) & bAccountNumsCrossFacilities));
					bCrossFacilityMergeVisit = bVisitNumToFindExact & bPrevVisitNumToFindExact & bVisitNumsCrossFacilities;
					if (!bMRNDifferent && !bPatIDDifferent && !bAcctDifferent)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without a PatientID, MRN or Account change");
					}
					if (!bMRNDifferent && !bMRNsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without an MRN change where MRNs don't cross facilities");
					}
					if (!bAcctDifferent && !bAccountNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without an Account change where Accounts don't cross facilities");
					}
					if (!bVisitNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made where Visit numbers don't cross facilities");
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility merges are not enabled");
				}
				break;
			case "A39":
			case "A46":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergePatient = bPatIDDifferent && (bMRNsCrossFacilities & bAccountNumsCrossFacilities & bVisitNumsCrossFacilities));
					if (!bOK)
					{
						if (!bPatIDDifferent)
						{
							CrossFacilityMergeFailure("A patientID merge between facilities cannot be made without a patientID change");
						}
						if (!bMRNsCrossFacilities)
						{
							CrossFacilityMergeFailure("A patientID merge between facilities cannot be made where MRNs don't cross facilities");
						}
						if (!bAccountNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("A patientID merge between facilities cannot be made where Accounts don't cross facilities");
						}
						if (!bVisitNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("A patientID merge between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility patient merges are not enabled");
				}
				break;
			case "A34":
			case "A40":
				if (bCrossFacilityPatientTransfers)
				{
					bCrossFacilityMergePatient = (bMRNDifferent | (!bMRNDifferent & bMRNsCrossFacilities & bPatIDDifferent)) & bAccountNumsCrossFacilities & bVisitNumsCrossFacilities;
					if (!bMRNDifferent && !bPatIDDifferent)
					{
						bOK = false;
						CrossFacilityMergeFailure("A patient merge between facilities cannot be made without a PatientID or MRN change");
					}
					if (!bMRNDifferent && !bMRNsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A patient merge between facilities cannot be made without an MRN change where MRNs don't cross facilities");
					}
					if (!bAccountNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A patient merge between facilities cannot be made where Accounts don't cross facilities");
					}
					if (!bVisitNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A patient merge between facilities cannot be made where Visit numbers don't cross facilities");
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility patient merges are not enabled");
				}
				break;
			case "A47":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergePatient = bMRNDifferent & bAccountNumsCrossFacilities & bVisitNumsCrossFacilities);
					if (!bOK)
					{
						if (!bMRNDifferent)
						{
							bOK = false;
							CrossFacilityMergeFailure("An MRN change between facilities cannot be made if the previous and new MRN are the same");
						}
						if (!bAccountNumsCrossFacilities)
						{
							bOK = false;
							CrossFacilityMergeFailure("A MRN change between facilities cannot be made where Accounts don't cross facilities");
						}
						if (!bVisitNumsCrossFacilities)
						{
							bOK = false;
							CrossFacilityMergeFailure("An MRN change between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility patient merges are not enabled");
				}
				break;
			case "A35":
			case "A41":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergeAccount = (bAcctDifferent | bAccountNumsCrossFacilities) & (bMRNDifferent | bMRNsCrossFacilities) & bVisitNumsCrossFacilities);
					bCrossFacilityMergeVisit = bVisitNumToFindExact & bPrevVisitNumToFindExact & bVisitNumsCrossFacilities;
					if (!bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityMergeFailure("An account merge between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent && !bAccountNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("An account merge between facilities cannot be made without an Account change where Accounts don't cross facilities");
						}
						if (!bVisitNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("An account merge between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility account merges are not enabled");
				}
				break;
			case "A49":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergeAccount = bAcctDifferent & (bMRNDifferent | bMRNsCrossFacilities) & bVisitNumsCrossFacilities);
					if (!bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityMergeFailure("An account change between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent)
						{
							CrossFacilityMergeFailure("An account change between facilities cannot be made if the account numbers are the same");
						}
						if (!bVisitNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("An account change between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility account merges are not enabled");
				}
				break;
			case "A42":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergeVisit = (bMRNDifferent | bMRNsCrossFacilities) & (bAcctDifferent | bAccountNumsCrossFacilities) & (bVisitNumChanging | bVisitNumsCrossFacilities));
					if (bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityMergeFailure("A merge visit between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent && !bAccountNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("A merge visit between facilities cannot be made without an account change where accounts don't cross facilities");
						}
						if (!bVisitNumChanging && !bVisitNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("A merge visit between facilities cannot be made without a visit number change where visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility visit merges are not enabled");
				}
				break;
			case "A50":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMergeVisit = bVisitNumChanging & bMRNsCrossFacilities & bAccountNumsCrossFacilities);
					if (!bOK)
					{
						if (!bVisitNumChanging)
						{
							CrossFacilityMergeFailure("A visit number change between facilities cannot be made if the visit numbers are the same");
						}
						if (!bMRNsCrossFacilities)
						{
							CrossFacilityMergeFailure("A visit number change between facilities cannot be made where MRNs don't cross facilities");
						}
						if (!bAccountNumsCrossFacilities)
						{
							CrossFacilityMergeFailure("A visit number change between facilities cannot be made where account numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMergeFailure("Cross-facility visit merges are not enabled");
				}
				break;
			case "A44":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMoveAccount = (bMRNDifferent | bMRNsCrossFacilities) & (bAcctDifferent | bAccountNumsCrossFacilities) & bVisitNumsCrossFacilities);
					if (!bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityMoveFailure("An account move between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent && !bAccountNumsCrossFacilities)
						{
							CrossFacilityMoveFailure("An account move between facilities cannot be made without an Account change where Accounts don't cross facilities");
						}
						if (!bVisitNumsCrossFacilities)
						{
							CrossFacilityMoveFailure("An account move between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMoveFailure("Cross-facility account moves are not enabled");
				}
				break;
			case "A45":
				if (bCrossFacilityPatientTransfers)
				{
					bOK = (bCrossFacilityMoveVisit = (bMRNDifferent | bMRNsCrossFacilities) & (bAcctDifferent | bAccountNumsCrossFacilities) & (bVisitNumChanging | bVisitNumsCrossFacilities));
					if (!bOK)
					{
						if (!bMRNDifferent && !bMRNsCrossFacilities)
						{
							CrossFacilityMoveFailure("A visit move between facilities cannot be made without an MRN change where MRNs don't cross facilities");
						}
						if (!bAcctDifferent && !bAccountNumsCrossFacilities)
						{
							CrossFacilityMoveFailure("A visit move between facilities cannot be made without an Account change where Accounts don't cross facilities");
						}
						if (!bVisitNumChanging && !bVisitNumsCrossFacilities)
						{
							CrossFacilityMoveFailure("A visit move between facilities cannot be made where Visit numbers don't cross facilities");
						}
					}
				}
				else
				{
					CrossFacilityMoveFailure("Cross-facility visit moves are not enabled");
				}
				break;
			default:
				if (bCrossFacilityPatientTransfers)
				{
					bCrossFacilityMergePatient = (bMRNDifferent | (!bMRNDifferent & bMRNsCrossFacilities & bPatIDDifferent)) & bAccountNumsCrossFacilities & bVisitNumsCrossFacilities;
					bCrossFacilityMergeAccount = bVisitNumsCrossFacilities & (bAcctDifferent | (bCrossFacilityMergePatient & bAccountNumsCrossFacilities));
					bCrossFacilityMergeVisit = bVisitNumToFindExact & bPrevVisitNumToFindExact & bVisitNumsCrossFacilities;
					if (!bMRNDifferent && !bPatIDDifferent && !bAcctDifferent)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without a PatientID, MRN or Account change");
					}
					if (!bMRNDifferent && !bMRNsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without an MRN change where MRNs don't cross facilities");
					}
					if (!bAcctDifferent && !bAccountNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made without an Account change where Accounts don't cross facilities");
					}
					if (!bVisitNumsCrossFacilities)
					{
						bOK = false;
						CrossFacilityMergeFailure("A merge patient info between facilities cannot be made where Visit numbers don't cross facilities");
					}
				}
				else
				{
					CrossFacilityTransferFailure("Cross-facility merges are not enabled");
				}
				break;
			}
			bCrossFacilityTransferSameAccount = bCrossFacilityTransfer & bAccountNumsCrossFacilities & !bAcctDifferent;
			bCrossFacilityTransferSameVisit = bCrossFacilityTransfer & bVisitNumsCrossFacilities & !bVisitNumChanging;
			bCrossFacilityMergeSameAccount = bCrossFacilityMergeAccount & bAccountNumsCrossFacilities & !bAcctDifferent;
			bCrossFacilityMergeSameVisit = bCrossFacilityMergeVisit & bVisitNumsCrossFacilities & !bVisitNumChanging;
			bCrossFacilityMoveSameAcccount = bCrossFacilityMoveAccount & bAccountNumsCrossFacilities & !bAcctDifferent;
			bCrossFacilityMoveSameVisit = bCrossFacilityMoveVisit & bVisitNumsCrossFacilities & !bVisitNumChanging;
		}
		return bOK;
	}

	private void CrossFacilityTransferFailure(string details)
	{
		AppErrorMsg = "Attempted transfer between facilities failed.";
		m_NNBase.ReportErrorDB(AppErrorMsg, "E", "checking ADT message", "CrossFacilityTransferFailure", details);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Cross-facility transfer failed", bError: true, ref myPTDBWriteCommand);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Cross-facility transfer failed", bError: true, ref myPTDBWriteCommand);
	}

	private void CrossFacilityMergeFailure(string details)
	{
		AppErrorMsg = "Attempted merge between facilities failed.";
		m_NNBase.ReportErrorDB(AppErrorMsg, "E", "checking ADT message", "CrossFacilityMergeFailure", details);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Cross-facility merge failed", bError: true, ref myPTDBWriteCommand);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Cross-facility merge failed", bError: true, ref myPTDBWriteCommand);
	}

	private void CrossFacilityMoveFailure(string details)
	{
		AppErrorMsg = "Attempted move between facilities failed.";
		m_NNBase.ReportErrorDB(AppErrorMsg, "E", "checking ADT message", "CrossFacilityMoveFailure", details);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Cross-facility move failed", bError: true, ref myPTDBWriteCommand);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Cross-facility move failed", bError: true, ref myPTDBWriteCommand);
	}

	private void ProcessMessageForFacility(string myFacility, string myFacilNum)
	{
		try
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Processing message for facility " + myFacility, isXml: false, "ProcessMessageForFacility");
			}
			myPatientTrackingRec.Begin(m_NNBase, PatientID, MedicalRecordNumber, AccountNumber, MessageSubType, PreviousPatientID, PreviousMedicalRecordNumber, PreviousPatientAccount);
			if (ADTMessageOK(myFacility, myFacilNum) && OKToAddOrUpdatePatient())
			{
				ProcessADTMessage();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessMessageForFacility");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "Processing message for a given facility", "ProcessMessageForFacility", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing message for a given facility", "ProcessMessageForFacility", bMoveMessage: true);
		}
		myPatientTrackingRec.Commit(m_NNBase);
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

	private void ProcessSubMessage(string SubMessage)
	{
		bool bOK = true;
		try
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(". . . Sub-message . . . . . . . . . . . . . . . . . . . . ", isXml: false, "ProcessSubMessage");
				m_NNBase.log(SubMessage, isXml: false, "ProcessSubMessage");
			}
			InitMessageFields();
			int iLast = 0;
			int i = SubMessage.IndexOf('\r', iLast);
			bool bDone = false;
			while (!bDone && i > 0 && i < SubMessage.Length)
			{
				if (i > iLast)
				{
					string segment = SubMessage.Substring(iLast, i - iLast);
					switch (segment.Substring(0, 3))
					{
					case "MSH":
						ProcessMessageHeaderSegment(segment);
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(MessageSubType + " - " + MessageSubTypeDescription(MessageSubType), isXml: false, "ProcessSubMessage");
						}
						bOK &= MessageSubTypeSupported(MessageSubType);
						if (!bOK)
						{
							bDone = true;
						}
						break;
					case "PID":
						ProcessPatientIdentificationSegment(segment);
						break;
					case "PV1":
						bNoVisitInfo = false;
						ProcessPatientVisitSegment(segment);
						break;
					case "OBX":
						ProcessResultSegment(segment);
						break;
					case "DG1":
						ProcessDiagnosisSegment(segment);
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
				if (i > 0 && i < SubMessage.Length)
				{
					iLast = i + 1;
					i = SubMessage.IndexOf('\r', iLast);
				}
			}
			if (bOK)
			{
				ProcessParsedMessage();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("ProcessSubMessage");
		}
		catch (Exception e)
		{
			handleException(e, "processing ADT message", "ProcessSubMessage", bMoveMessage: true);
		}
	}

	private void InitMessageFields()
	{
		bNoVisitInfo = true;
		bLocChange = false;
		bPatientIDExists = false;
		bPrevPatientIDExists = false;
		bMRNExists = false;
		bPrevMRNExists = false;
		bPatientExists = false;
		bPrevPatientExists = false;
		bVisitExists = false;
		bPrevVisitExists = false;
		bInvalidLoc = false;
		m_facility = "";
		m_facility_source = "";
		m_facility_alias = "";
		m_PrevFacility = "";
		m_prev_facility_source = "";
		m_prev_facility_alias = "";
		m_location = "";
		m_location_alias = "";
		m_facil_num = "";
		m_prev_facil_num = "";
		m_loc_num = "";
		m_new_visit_UUID = "";
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
		Race_HL7 = "";
		Race_DML = "";
		AccountNumber = "";
		AccountAssigningAuthority = "";
		AccountAssigningFacility = "";
		EventFacility = "";
		PreviousMRNAssigningAuthority = "";
		PreviousMRNAssigningFacility = "";
		PreviousAccountAssigningAuthority = "";
		PreviousAccountAssigningFacility = "";
		PreviousVisitNumAssigningAuthority = "";
		PreviousVisitNumAssigningFacility = "";
		PatientClass = "";
		Location_PV1_3_1 = "";
		Room = "";
		Bed = "";
		Weight_HL7_value = "";
		Weight_HL7_units = "";
		Weight_DML_value = "";
		Weight_DML_units = "";
		Height_HL7_value = "";
		Height_HL7_units = "";
		Height_DML_value = "";
		Height_DML_units = "";
		Diagnosis = "";
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
		AttendingPhysician = "";
		ReportingPhysician = "";
		ConsultingPhysician = "";
		PatientType = "";
		DischargeDateTime = "20371231000000";
		PreviousMedicalRecordNumber = "";
		PreviousPatientAccount = "";
		PreviousPatientID = "";
		VisitNumFound = "";
		VisitNumToFind = "";
		VisitNumToAdd = "";
		VisitNumFromADT = "";
		VisitUUID = "";
		bUseVisitNumbersToFind = true;
		bUseAccountNumbersToFind = true;
		PreviousVisitNumToFind = "";
		PreviousVisitNumFromADT = "";
		PreviousVisitNumFound = "";
		PreviousVisitUUID = "";
		iVisitCount = 0;
		iPrevVisitCount = 0;
		bNoAdmit = false;
		bFacilityFromList = false;
		bPatIDToFind = false;
		bMRNToFind = false;
		bAccountToFind = false;
		bVisitToFind = false;
		bPrevIDsToFind = false;
		bLocationRequired = false;
		bAddAccount = false;
		bAddPatient = false;
		bAddVisit = false;
		bMergePatient = false;
		bMergeAccount = false;
		bMergeVisit = false;
		bMoveAccount = false;
		bMoveVisit = false;
		bDeactAccount = false;
		bDeactVisit = false;
		bPatientAdded = false;
		bAccountAdded = false;
		bVisitAdded = false;
		bUpdatePatient = false;
		bUpdateAccount = false;
		bUpdateVisit = false;
		bAddVisitOK = false;
		bAddAccountOK = false;
		bAccountRequired = true;
		bVisitRequired = true;
		bPatientVisitChanged = false;
		bVisitUpdated = false;
		bVisitMoved = false;
		bAccountMoved = false;
		bPrevVisitDeleted = false;
		bPrevAccountDeleted = false;
		bPrevPatientDeleted = false;
		m_PatientQuery.ClearSearchParams();
		m_PrevIDs_PatientQuery.ClearSearchParams();
		m_PrevIDs_PatientList.ClearList();
		m_DBPatient.ClearStatus();
		m_DBPatient.ClearAffectedLocationList();
		m_DBPatient.Clear();
		m_PatientList.ClearList();
		m_PatientVisitRec.ClearStatus();
		m_PatientVisitRec.Clear();
		m_PatientAccountRec.ClearStatus();
		m_PatientAccountRec.Clear();
		m_PrevIDs_PatientVisitRec.ClearStatus();
		m_PrevIDs_PatientVisitRec.Clear();
		m_PrevIDs_PatientAccountRec.ClearStatus();
		m_PrevIDs_PatientAccountRec.Clear();
		m_PrevIDs_DBPatient.ClearStatus();
		m_PrevIDs_DBPatient.ClearAffectedLocationList();
		m_PrevIDs_DBPatient.Clear();
		m_newDBPatient.ClearStatus();
		m_newDBPatient.ClearAffectedLocationList();
		m_newDBPatient.Clear();
		m_newPatientAccountRec.ClearStatus();
		m_newPatientAccountRec.Clear();
		m_newPatientVisitRec.ClearStatus();
		m_newPatientVisitRec.Clear();
	}

	private bool ADTMessageOK(string myFacility, string myFacilityNum)
	{
		bool bOK = false;
		m_loc_num = "";
		m_facil_num = myFacilityNum;
		m_facility = myFacility;
		m_facility_source = "";
		try
		{
			bool bFacilNumPassedIn = myFacilityNum.Length > 0;
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
			bVisitToFind = bPrevOrNewVisitNumGiven;
			bPrevIDsToFind = false;
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
				m_prev_facility_alias = (m_facility_alias = (m_PrevFacility = (m_facility = theoneFacility)));
				m_prev_facil_num = (m_facil_num = theoneFacilityNum);
				LookupTimeZone(ref m_TimeZoneName, ref m_TimeZoneInfo);
				myPatientTrackingRec.m_facilnum = m_facil_num;
				myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
				myPatientTrackingRec.m_facilname = m_facility;
				myPatientTrackingRec.m_facilalias = m_facility_alias;
				myPatientTrackingRec.m_PrevFacilName = m_PrevFacility;
				myPatientTrackingRec.m_PrevFacilAlias = m_prev_facility_alias;
			}
			else
			{
				if (!bFacilNumPassedIn)
				{
					if (!TryFacility(Facility_PV1_3_4, "Assigned patient location facility - PV1.3.4") && !TryFacility(Facility_PV1_3_11, "Location assigning authority - PV1.3.11") && !TryFacility(Facility_PV1_11_4, "Temporary patient location facility - PV1.11.4") && !TryFacility(Facility_PV1_11_11, "Temporary patient location assigning authority - PV1.11.1") && !TryFacility(Facility_PV1_19_4, "Visit number assigning authority - PV1.19.4") && !TryFacility(Facility_PV1_19_6, "Visit number assigning facility - PV1.19.6") && !TryFacility(ServicingFacility_PV1_39_1, "Servicing facility - PV1.39.1") && !TryFacility(Facility_PV1_42_4, "Pending location facility - PV1.42.4") && !TryFacility(Facility_PV1_42_11, "Pending location Assigning Authority - PV1.42.11") && !TryFacility(MRNAssigningAuthority, "MRN Assigning Authority - PID.3.4") && !TryFacility(MRNAssigningFacility, "MRN Assigning Facility - PID.3.6") && !TryFacility(AccountAssigningAuthority, "Account Assigning Authority - PID.18.4") && !TryFacility(AccountAssigningFacility, "Account Assigning Facility - PID.18.6") && !TryFacility(EventFacility, "Event Facility - EVN.7"))
					{
						TryFacility(SendingFacility, "SendingFacility - MSH.4");
					}
				}
				else
				{
					m_prev_facility_alias = (m_PrevFacility = (m_facility_alias = m_facility));
					m_prev_facil_num = m_facil_num;
					LookupTimeZone(ref m_TimeZoneName, ref m_TimeZoneInfo);
					myPatientTrackingRec.m_facilnum = m_facil_num;
					myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
					myPatientTrackingRec.m_facilname = m_facility;
					myPatientTrackingRec.m_facilalias = m_facility_alias;
					myPatientTrackingRec.m_PrevFacilName = m_PrevFacility;
					myPatientTrackingRec.m_PrevFacilAlias = m_prev_facility_alias;
				}
				if (!TryPrevFacility(Facility_PV1_6_4, "Prior patient location facility - PV1.6.4") && !TryPrevFacility(Facility_PV1_6_11, "Prior patient location assigning authority - PV1.6.11") && !TryPrevFacility(Facility_PV1_43_4, "Prior temporary location facility - PV1.43.4") && !TryPrevFacility(Facility_PV1_43_11, "Prior temporary location Assigning Authority - PV1.43.11") && !TryPrevFacility(PreviousMRNAssigningAuthority, "Prior MRN Assigning Authority - MRG.1.4") && !TryPrevFacility(PreviousMRNAssigningFacility, "Prior MRN Assigning Facility - MRG.1.6") && !TryPrevFacility(PreviousAccountAssigningAuthority, "Prior Account Assigning Authority - MRG.3.4") && !TryPrevFacility(PreviousAccountAssigningFacility, "Prior Account Assigning Facility - MRG.3.6") && !TryPrevFacility(PreviousVisitNumAssigningAuthority, "Prior Visit Number Assigning Authority - MRG.5.4"))
				{
					TryPrevFacility(PreviousVisitNumAssigningFacility, "Prior Visit Number Assigning Facility - MRG.5.6");
				}
				if (m_facility.Length == 0 && m_PrevFacility.Length > 0)
				{
					m_facility = m_PrevFacility;
					m_facil_num = m_prev_facil_num;
					m_facility_source = m_prev_facility_source;
					m_facility_alias = m_prev_facility_alias;
				}
				if (m_facility.Length > 0)
				{
					if (m_facil_num.Length == 0)
					{
						m_FacilityList.LookupFacilNum(m_facility, ref m_facil_num);
					}
					if (m_facil_num.Length == 0)
					{
						string ErrMsg = "Facility '" + m_facility + "' (" + m_facility_source + ") not found";
						if (AppErrorMsg.Length == 0)
						{
							AppErrorMsg = (bNoVisitInfo ? "" : ErrMsg);
						}
						m_NNBase.ReportErrorNoDB(ErrMsg, bNoVisitInfo ? "W" : "E", "checking ADT message", "ADTMessageOK", "");
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Facility not found", bError: true, ref myPTDBWriteCommand);
						if (!bNoVisitInfo)
						{
							bOK = false;
							return bOK;
						}
					}
					else
					{
						LookupTimeZone(ref m_TimeZoneName, ref m_TimeZoneInfo);
						myPatientTrackingRec.m_facilnum = m_facil_num;
						myPatientTrackingRec.m_facilname = m_facility;
						myPatientTrackingRec.m_facilalias = m_facility_alias;
						if (m_prev_facil_num.Length == 0)
						{
							m_PrevFacility = m_facility;
							m_prev_facil_num = m_facil_num;
							m_prev_facility_source = m_facility_source;
							m_prev_facility_alias = m_facility_alias;
						}
						myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
						myPatientTrackingRec.m_PrevFacilName = m_PrevFacility;
						myPatientTrackingRec.m_PrevFacilAlias = m_prev_facility_alias;
					}
				}
			}
			bFacilGiven = m_facil_num.Length > 0;
			bPrevFacilGiven = m_prev_facil_num.Length > 0;
			bSpansFacilities = ((!bFacilGiven & !bPrevFacilGiven) | bFacilNumPassedIn) & bNoVisitInfo & bPatIDChanging & !bSingleFacility;
			if (bMRNsCrossFacilities)
			{
				bSpansFacilities |= ((!bFacilGiven & !bPrevFacilGiven) | bFacilNumPassedIn) & bNoVisitInfo & bMRNChanging & !bSingleFacility;
			}
			if ((!bFacilGiven & !bPrevFacilGiven) && bSpansFacilities)
			{
				m_NNBase.log("This transaction spans facilities", isXml: false, "ADTMessageOK");
				bOK = true;
				return bOK;
			}
			if (!bNoVisitInfo)
			{
				if ((!bFacilGiven & !bPrevFacilGiven) && !bSpansFacilities)
				{
					if ((!bFacilGiven && m_facility_alias.Length > 0) || (!bPrevFacilGiven && m_prev_facility_alias.Length > 0))
					{
						AppErrorMsg = "Facility not found";
					}
					else
					{
						AppErrorMsg = "No facility provided";
					}
					m_NNBase.ReportErrorNoDB(AppErrorMsg, "E", "checking ADT message", "ADTMessageOK", "");
					bOK = false;
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, AppErrorMsg, bError: true, ref myPTDBWriteCommand);
					return bOK;
				}
				bOK = GetFacilityConfig();
				if (!bOK)
				{
					return bOK;
				}
				bUseVisitNumbersToFind = UseVisitNumbersToFind();
				bUseAccountNumbersToFind = UseAccountNumbersToFind();
				bOK = GetLocationAndBuildVisitNumsWithPV1();
				if (!bOK)
				{
					return bOK;
				}
			}
			else
			{
				bUseVisitNumbersToFind = UseVisitNumbersToFind();
				bUseAccountNumbersToFind = UseAccountNumbersToFind();
				GetVisitNumToUse(bNew: false);
				GetVisitNumberFlags();
			}
			bOK = DetermineCrossFacilityActions();
			if (!bOK)
			{
				return bOK;
			}
			bOK = DetermineMergesAndMoves(ref bPrevIDsToFind, ref bLocationRequired);
			if (!bOK)
			{
				return bOK;
			}
			if (bNoVisitInfo)
			{
				bOK = GetFacilityAndLocationAndBuildVisitNumsWithoutPV1();
				if (!bOK)
				{
					return bOK;
				}
			}
			bOK = FinalMessageChecks();
		}
		catch (OdbcException e)
		{
			handleDBException(e, "Checking ADT message", "ADTMessageOK", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "Checking ADT message", "ADTMessageOK", bMoveMessage: true);
		}
		return bOK;
	}

	private bool GetLocationAndBuildVisitNumsWithPV1()
	{
		bool bOK = true;
		if (Location_PV1_3_1.Length > 0)
		{
			myPatientTrackingRec.m_localias = (m_location_alias = Location_PV1_3_1);
		}
		else if (Location_PV1_11_1.Length > 0)
		{
			myPatientTrackingRec.m_localias = (m_location_alias = Location_PV1_11_1);
		}
		else if (Location_PV1_42_1.Length > 0)
		{
			myPatientTrackingRec.m_localias = (m_location_alias = Location_PV1_42_1);
		}
		if (m_location_alias.Length == 0)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "No location provided", "E", "checking ADT message", "GetLocationAndBuildVisitNumsWithPV1", "");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "No location provided", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		m_location = m_LocationList.LookupLocation(m_location_alias, m_facil_num);
		if (m_location.Length == 0)
		{
			bInvalidLoc = true;
		}
		else
		{
			myPatientTrackingRec.m_locname = m_location;
			bOK = GetLocationConfig();
			if (!bOK)
			{
				return bOK;
			}
		}
		GetVisitNumToUse(bNew: true);
		GetVisitNumToUse(bNew: false);
		GetVisitNumberFlags();
		return bOK;
	}

	private bool GetFacilityAndLocationAndBuildVisitNumsWithoutPV1()
	{
		bool bOK = true;
		int iNewPatientStat = 0;
		int iPrevPatientStat = 0;
		int iNewAccountStat = 0;
		int iPrevAccountStat = 0;
		int iNewVisitStat = 0;
		int iPrevVisitStat = 0;
		bool bNoNewPatients = false;
		bool bNoPrevPatients = false;
		bool bMoreThanOneNewPatient = false;
		bool bMoreThanOnePrevPatient = false;
		bool bNoNewAccounts = false;
		bool bNoPrevAccounts = false;
		bool bMoreThanOneNewAccount = false;
		bool bMoreThanOnePrevAccount = false;
		bool bNoNewVisits = false;
		bool bNoPrevVisits = false;
		bool bMoreThanOneNewVisit = false;
		bool bMoreThanOnePrevVisit = false;
		bool bNewVisitCanBeFound = false;
		bool bPrevVisitCanBeFound = false;
		bool bNewAccountCanBeFound = false;
		bool bPrevAccountCanBeFound = false;
		bool bNewPatientCanBeFound = false;
		bool bPrevPatientCanBeFound = false;
		bNewVisitCanBeFound = (bNewAccountCanBeFound = (bNewPatientCanBeFound = CanWeGetPatientVisitInfo(bNew: true)));
		bPrevVisitCanBeFound = (bPrevAccountCanBeFound = (bPrevPatientCanBeFound = CanWeGetPatientVisitInfo(bNew: false)));
		if (!bNewVisitCanBeFound && !bPrevVisitCanBeFound)
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "Insufficient patient and facility identification provided", "E", "checking demographics", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "(the necessary combination of facility and/or new and/or previous (account and/or MRN and/or PatientID) were missing on an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit not found without PV1", bError: true, ref myPTDBWriteCommand);
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Visit not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		bool bNotMoreThanOneNewAccountShouldBeFound = (!bMergePatient | bMergeAccount) & (!bMoveAccount | (bMoveAccount & bAccountGiven));
		bool bNotMoreThanOnePrevAccountShouldBeFound = (!bMergePatient | bMergeAccount | bDeactAccount) & (!bMoveAccount | (bMoveAccount & bPrevAcctGiven));
		bool bNotMoreThanOnePrevVisitShouldBeFound = !bMergePatient & !bMergeAccount & !bDeactAccount & !bMoveAccount & (!bMoveVisit | (bMoveVisit & bPrevVisitNumToFindExact));
		bool bAtLeastOneAccountShouldBeFound = (!bMergePatient | bMergeAccount) & (!bMoveAccount | (bMoveAccount & (bAccountGiven | bPrevAcctGiven)));
		bool bAtLeastOneVisitShouldBeFound = !bMergePatient & !bMergeAccount & !bMoveAccount & (!bMoveVisit | (bMoveVisit & bPrevVisitNumToFindGiven));
		if (bNewVisitCanBeFound)
		{
			GetPatientVisitInfo(bNew: true, ref iNewPatientStat, ref iNewAccountStat, ref iNewVisitStat);
			bMoreThanOneNewPatient = iNewPatientStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			bMoreThanOneNewAccount = iNewAccountStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			bMoreThanOneNewVisit = iNewVisitStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			Convert.ToInt32(errortypes.AOK);
			Convert.ToInt32(errortypes.AOK);
			Convert.ToInt32(errortypes.AOK);
			bNoNewPatients = iNewPatientStat == Convert.ToInt32(errortypes.NoMatch);
			bNoNewAccounts = iNewAccountStat == Convert.ToInt32(errortypes.NoMatch);
			bNoNewVisits = iNewVisitStat == Convert.ToInt32(errortypes.NoMatch);
		}
		if (bMoreThanOneNewPatient)
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "More than one matching patient record was found for given Patient IDs", "E", "searching for patient records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "more than one patients record was found in the database to match the new/given patient IDs from an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if (bMoreThanOneNewAccount && bNotMoreThanOneNewAccountShouldBeFound)
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "More than one matching account record was found for given Patient IDs", "E", "searching for patient records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "more than one patient_accounts record was found in the database to match the new/given patient IDs from an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Account not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if (bPrevVisitCanBeFound)
		{
			GetPatientVisitInfo(bNew: false, ref iPrevPatientStat, ref iPrevAccountStat, ref iPrevVisitStat);
			bMoreThanOnePrevPatient = iPrevPatientStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			bMoreThanOnePrevAccount = iPrevAccountStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			bMoreThanOnePrevVisit = iPrevVisitStat == Convert.ToInt32(errortypes.MoreThanOneMatch);
			Convert.ToInt32(errortypes.AOK);
			Convert.ToInt32(errortypes.AOK);
			Convert.ToInt32(errortypes.AOK);
			bNoPrevPatients = iPrevPatientStat == Convert.ToInt32(errortypes.NoMatch);
			bNoPrevAccounts = iPrevAccountStat == Convert.ToInt32(errortypes.NoMatch);
			bNoPrevVisits = iPrevVisitStat == Convert.ToInt32(errortypes.NoMatch);
		}
		if (bMoreThanOnePrevPatient)
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "More than one matching patient record was found for previous Patient IDs", "E", "searching for patient records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "more than one patients record was found in the database to match the previous patient IDs from an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if (bMoreThanOnePrevAccount && bNotMoreThanOnePrevAccountShouldBeFound)
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "More than one matching account record was found for previous Patient IDs", "E", "searching for patient records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "more than one patient_accounts record was found in the database to match the previous patient IDs from an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Account not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if (bMoreThanOnePrevVisit && bNotMoreThanOnePrevVisitShouldBeFound)
		{
			m_NNBase.ReportErrorDB("AppErrorMsg = More than one matching visit record was found for previous Patient IDs", "E", "searching for patient records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "more than one patient_visits record was found in the database to match the previous patient IDs from an ADT message with no PV1 segment");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Visit not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if ((bNoNewPatients || !bNewPatientCanBeFound) && (bNoPrevPatients || !bPrevPatientCanBeFound))
		{
			bOK = false;
			if (!bFacilityFromList)
			{
				m_NNBase.ReportErrorDB(AppErrorMsg = "No matching patient records for Patient IDs", "E", "searching for patients records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "no patients records were found in the database to match the given and/or previous patient IDs from an ADT message with no PV1 segment");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient not found without PV1", bError: true, ref myPTDBWriteCommand);
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient not found without PV1", bError: true, ref myPTDBWriteCommand);
			}
			return bOK;
		}
		if ((bNoNewAccounts || !bNewAccountCanBeFound) && (bNoPrevAccounts || !bPrevAccountCanBeFound) && bAtLeastOneAccountShouldBeFound)
		{
			bOK = false;
			m_NNBase.ReportErrorDB(AppErrorMsg = "No matching account records for Patient IDs", "E", "searching for patient_accounts records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "no patient_accounts records were found in the database to match the given and/or previous patient IDs from an ADT message with no PV1 segment");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Account not found without PV1", bError: true, ref myPTDBWriteCommand);
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Account not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		if ((bNoNewVisits || !bNewVisitCanBeFound) && (bNoPrevVisits || !bPrevVisitCanBeFound) && bAtLeastOneVisitShouldBeFound)
		{
			bOK = false;
			m_NNBase.ReportErrorDB(AppErrorMsg = "No matching visit records for Patient IDs", "E", "searching for patient_visits records", "GetFacilityAndLocationAndBuildVisitNumsWithoutPV1", "no patient_visits records were found in the database to match the given and/or previous patient IDs from an ADT message with no PV1 segment");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit not found without PV1", bError: true, ref myPTDBWriteCommand);
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Visit not found without PV1", bError: true, ref myPTDBWriteCommand);
			return bOK;
		}
		bOK = true;
		if (m_facil_num.Length == 0 && m_facility.Length == 0 && (m_prev_facil_num.Length > 0 || m_PrevFacility.Length > 0))
		{
			if (m_prev_facil_num.Length == 0)
			{
				m_FacilityList.LookupFacilNum(m_PrevFacility, ref m_prev_facil_num);
			}
			else if (m_PrevFacility.Length == 0)
			{
				LookupPrevFacilName();
			}
			if (bOK)
			{
				m_facility = m_PrevFacility;
				m_facil_num = m_prev_facil_num;
				m_facility_source = m_prev_facility_source;
				if (m_prev_facility_source.Length == 0)
				{
					m_prev_facility_alias = m_PrevFacility;
				}
				m_facility_alias = m_prev_facility_alias;
				myPatientTrackingRec.m_facilname = m_facility;
				myPatientTrackingRec.m_facilalias = m_facility_alias;
				myPatientTrackingRec.m_facilnum = m_facil_num;
			}
		}
		if (m_facil_num.Length > 0 || m_facility.Length > 0)
		{
			if (m_facil_num.Length == 0)
			{
				m_FacilityList.LookupFacilNum(m_facility, ref m_facil_num);
			}
			else if (m_facility.Length == 0)
			{
				LookupFacilName();
			}
			if (m_facility_source.Length == 0)
			{
				m_facility_alias = m_facility;
			}
			myPatientTrackingRec.m_facilname = m_facility;
			myPatientTrackingRec.m_facilalias = m_facility_alias;
			myPatientTrackingRec.m_facilnum = m_facil_num;
			bOK = GetFacilityConfig();
			if (!bOK)
			{
				return bOK;
			}
			if (m_PrevFacility.Length == 0 && m_facility.Length > 0)
			{
				m_PrevFacility = m_facility;
				m_prev_facil_num = m_facil_num;
				m_prev_facility_source = m_facility_source;
				m_prev_facility_alias = m_facility_alias;
				myPatientTrackingRec.m_PrevFacilName = m_PrevFacility;
				myPatientTrackingRec.m_PrevFacilAlias = m_prev_facility_alias;
				myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
			}
			if (m_location.Length > 0)
			{
				myPatientTrackingRec.m_locname = m_location;
				bOK = GetLocationConfig();
				if (!bOK)
				{
					return bOK;
				}
				GetVisitNumToUse(bNew: true);
				GetVisitNumberFlags();
			}
		}
		return bOK;
	}

	private bool FinalMessageChecks()
	{
		bool bOK = true;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("facility=\"" + m_facility + "\"" + ((m_facility_alias == m_facility || m_facility_alias.Length == 0) ? "" : ("(" + m_facility_alias + ")")) + " location=\"" + m_location + "\"" + ((m_location_alias == m_location || m_location_alias.Length == 0) ? "" : ("(" + m_location_alias + ")")) + " MRN=\"" + MedicalRecordNumber + "\" PreviousMRN=\"" + PreviousMedicalRecordNumber + "\" PatientID=\"" + PatientID + "\" PreviousPatientID=\"" + PreviousPatientID + "\" AccountNumber=\"" + AccountNumber + "\" PreviousAccountNumber=\"" + PreviousPatientAccount + "\" VisitNumToFind=\"" + VisitNumToFind + "\" VisitNumToAdd=\"" + VisitNumToAdd + "\" PreviousVisitNumToFind=\"" + PreviousVisitNumToFind + "\"", isXml: false, "FinalMessageChecks");
		}
		if (m_facility.Length == 0 && !bSpansFacilities && !bPrevOrNewPatIDGiven && !bMRNCanSpanFacilities)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "No facility provided", "E", "checking ADT message", "FinalMessageChecks", "");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "No facility provided", bError: true, ref myPTDBWriteCommand);
		}
		if (m_location.Length == 0 && bLocationRequired && !bPrevOrNewPatIDGiven && !bMRNCanSpanFacilities)
		{
			if (m_location_alias.Length > 0)
			{
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "Location not found", "E", "checking ADT message", "FinalMessageChecks", "");
			}
			else
			{
				m_NNBase.ReportErrorNoDB(AppErrorMsg = "No location provided", "E", "checking ADT message", "FinalMessageChecks", "");
			}
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, AppErrorMsg, bError: true, ref myPTDBWriteCommand);
		}
		if (bOK)
		{
			bFacilGiven = m_facil_num.Length > 0;
			bPrevFacilGiven = m_prev_facil_num.Length > 0;
			if (!bMRNGiven && !bAccountGiven && !bPatIDGiven)
			{
				m_NNBase.ReportErrorDB(AppErrorMsg = "No Patient IDs provided", "E", "checking ADT message", "FinalMessageChecks", "one or more of (MRN, Account number or Patient ID expected)");
				bOK = false;
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "No Patient IDs provided", bError: true, ref myPTDBWriteCommand);
			}
			if (bPrevPatIDGiven && !bPatIDGiven && (bMRNChanging || bAcctDifferent || bVisitNumChanging))
			{
				PatientID = PreviousPatientID;
				bPatIDGiven = true;
			}
			if (bPrevMRNGiven && !bMRNGiven && (bPatIDChanging || bAcctDifferent || bVisitNumChanging))
			{
				MedicalRecordNumber = PreviousMedicalRecordNumber.Trim();
				bMRNGiven = true;
			}
			if (bPrevAcctGiven && !bAccountGiven && (bPatIDDifferent || bMRNDifferent || bVisitNumChanging))
			{
				AccountNumber = PreviousPatientAccount.Trim();
				bAccountGiven = true;
			}
			if (bPrevVisitNumToFindGiven && !bVisitNumToFindGiven && (bPatIDDifferent || bMRNDifferent || bAcctDifferent))
			{
				VisitNumToFind = PreviousVisitNumToFind;
				bVisitNumToFindGiven = true;
				if (bPrevVisitNumToFindExact)
				{
					VisitNumToAdd = PreviousVisitNumToFind;
					bVisitNumToAddGiven = true;
					bVisitNumToFindExact = true;
				}
			}
			if ((bPrevPatIDGiven && !bPatIDGiven) || (bPrevMRNGiven && !bMRNGiven) || (bPrevAcctGiven && !bAccountGiven) || (bPrevVisitNumToFindGiven && !bVisitNumToFindGiven))
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
				if (bPrevVisitNumToFindGiven && !bVisitNumToFindGiven)
				{
					if (sPatIDs.Length > 0)
					{
						sPatIDs += " and ";
					}
					sPatIDs += "visit number";
				}
				m_NNBase.ReportErrorDB(AppErrorMsg = "No " + sPatIDs + " provided where previous " + sPatIDs + " provided", "E", "checking ADT message", "FinalMessageChecks", "");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing new patient IDs", bError: true, ref myPTDBWriteCommand);
				bOK = false;
			}
		}
		return bOK;
	}

	private void GetVisitNumberFlags()
	{
		bPrevOrNewVisitNumGiven = bVisitNumToFindGiven | bPrevVisitNumToFindGiven;
		bPrevOrNewVisitNumGivenExact = (bVisitNumToFindGiven & bVisitNumToFindExact) | (bPrevVisitNumToFindGiven & bPrevVisitNumToFindExact);
		bVisitNumToFindDifferent = Comp.Compare(VisitNumToFind, PreviousVisitNumToFind, CompOpt) != 0;
		bVisitNumChanging = bVisitNumToFindGiven & bPrevVisitNumToFindGiven & bVisitNumToFindExact & bPrevVisitNumToFindExact & bVisitNumToFindDifferent;
		bVisitNumTheSame = bVisitNumToFindGiven & bPrevVisitNumToFindGiven & bVisitNumToFindExact & bPrevVisitNumToFindExact & !bVisitNumToFindDifferent;
	}

	private void LookupTimeZone(ref string myTimeZoneName, ref TimeZoneInfo myTimeZoneInfo)
	{
		myDBReadCommand.CommandText = $"select time_zone from dba.facility_prefs where facility_uuid = '{m_facil_num}'";
		myDBReadReader = myDBReadCommand.ExecuteReader();
		if (myDBReadReader.Read())
		{
			string TimeZoneName = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
			try
			{
				myTimeZoneInfo = TimeZoneInfo.FindSystemTimeZoneById(TimeZoneName);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("LookupTimeZone");
			}
			catch (TimeZoneNotFoundException)
			{
				bool bWasLogging = m_NNBase.m_isLogging;
				m_NNBase.ForceLogging("TimeZoneNotFound");
				m_NNBase.log("Time zone not found " + myTimeZoneName, isXml: false, "HL7");
				m_NNBase.ReportErrorDB("Time zone " + myTimeZoneName + " was not found", "E", "looking up time zone", "LookupTimeZone", "");
				if (!bWasLogging)
				{
					m_NNBase.StopLogging();
				}
			}
			catch (Exception e)
			{
				handleException(e, "looking up time zone", "LookupTimeZone", bMoveMessage: false);
			}
		}
		myDBReadReader.Close();
	}

	protected bool MessageSubTypeSupported(string MessageSubType)
	{
		bool bOK = false;
		string[] sTransTypeArray = sSupportedTransactions.Split(',');
		string[] array = sTransTypeArray;
		foreach (string sTransType in array)
		{
			if (Comp.Compare(sTransType, MessageSubType, CompOpt) == 0)
			{
				bOK = true;
			}
		}
		if (!bOK)
		{
			m_NNBase.ReportErrorDB(AppRejectMsg = "Transaction type not supported: " + MessageSubType + " - " + MessageSubTypeDescription(MessageSubType), "E", "checking ADT message", "MessageSubTypeSupported", "");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Transaction not supported", bError: true, ref myPTDBWriteCommand);
		}
		return bOK;
	}

	private bool TryFacility(string myFacility, string myFacilitySource)
	{
		bool bRet = false;
		string FacilityFound = "";
		if (myFacility.Length > 0)
		{
			FacilityFound = m_FacilityList.LookupFacility(myFacility);
			if (FacilityFound.Length > 0)
			{
				myPatientTrackingRec.m_facilname = (m_facility = FacilityFound);
				myPatientTrackingRec.m_facilalias = (m_facility_alias = myFacility);
				m_facility_source = myFacilitySource;
				m_FacilityList.LookupFacilNum(FacilityFound, ref m_facil_num);
				myPatientTrackingRec.m_facilnum = m_facil_num;
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
		string PrevFacilityFound = "";
		if (myPrevFacility.Length > 0 && Comp.Compare(m_facility, myPrevFacility, CompOpt) != 0)
		{
			PrevFacilityFound = m_FacilityList.LookupFacility(myPrevFacility);
			if (PrevFacilityFound.Length > 0)
			{
				myPatientTrackingRec.m_PrevFacilName = (m_PrevFacility = PrevFacilityFound);
				myPatientTrackingRec.m_PrevFacilAlias = (m_prev_facility_alias = myPrevFacility);
				m_prev_facility_source = myPrevFacilitySource;
				m_FacilityList.LookupFacilNum(PrevFacilityFound, ref m_prev_facil_num);
				myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
				bRet = true;
			}
			else
			{
				m_NNBase.ReportErrorNoDB("Facility '" + myPrevFacility + "' (" + myPrevFacilitySource + ") not found", "W", "looking up previous facility", "TryPrevFacility", "");
			}
		}
		return bRet;
	}

	private bool OKToAddOrUpdatePatient()
	{
		bool bOK = true;
		bPatientIDExists = false;
		bMRNExists = false;
		bAccountExists = false;
		bPatientExists = false;
		bVisitExists = false;
		bPrevPatientIDExists = false;
		bPrevMRNExists = false;
		bPrevPatientExists = false;
		bPrevAccountExists = false;
		bPrevVisitExists = false;
		bAddPatient = false;
		bUpdatePatient = false;
		bAddAccount = false;
		bUpdateAccount = false;
		bAccountAdded = false;
		bPatientAdded = false;
		bAddVisit = false;
		bVisitAdded = false;
		bUpdateVisit = false;
		bAddVisitOK = false;
		bAddAccountOK = false;
		bPatientKeysGiven = false;
		bAccountKeysGiven = false;
		bVisitKeysGiven = false;
		if ((bVisitNumToFindGiven && bVisitNumToFindExact) || bAccountGiven || bMRNGiven || bPatIDGiven)
		{
			m_PatientQuery.ClearSearchParams();
			m_PatientQuery.FacilNum = m_facil_num;
			m_PatientQuery.EnterpriseID = PatientID;
			m_PatientQuery.MedRecNum = MedicalRecordNumber;
			m_PatientQuery.AccountNum = AccountNumber;
			m_PatientQuery.AdmitDate = AdmitTime;
			m_PatientQuery.VisitNum = VisitNumToFind;
			m_PatientQuery.bVisitExact = bVisitNumToFindExact;
			m_PatientQuery.bSpansFacilities = bSpansFacilities;
			m_PatientQuery.RecordsExist(m_NNBase, ref myDBReadCommand);
			bPatientIDExists = m_PatientQuery.bEnterpriseIDExists;
			bMRNExists = m_PatientQuery.bMedRecExists;
			bAccountExists = m_PatientQuery.bAccountExists;
			bPatientExists = m_PatientQuery.bPatientExists;
			bVisitExists = m_PatientQuery.bVisitExists;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("PatientIDExists: " + PatientID + " " + (bPatientIDExists ? "true" : "false") + ", MRNExists: " + MedicalRecordNumber + " " + (bMRNExists ? "true" : "false") + ", AccountExists: " + AccountNumber + " " + (bAccountExists ? "true" : "false") + ", VisitExists: " + VisitNumToFind + " " + (bVisitExists ? "true" : "false"), isXml: false, "OKToAddOrUpdatePatient");
			}
			if (bAccountGiven && bAccountExists && bPatientExists && bMRNGiven && !bMRNExists && !bMRNChanging && (!bPatIDGiven || (bPatIDGiven && !bPatientIDExists && !bPatIDChanging)))
			{
				m_NNBase.ReportErrorDB("MRN and account conflict", "E", "searching for MRN and account", "OKToAddOrUpdatePatient", "Given account was found, but given MRN was not found");
				bOK = false;
			}
			if (bAccountGiven && bAccountExists && bPatientExists && bPatIDGiven && !bPatientIDExists && !bPatIDChanging && (!bMRNGiven || (bMRNGiven && !bMRNExists && !bMRNChanging)))
			{
				m_NNBase.ReportErrorDB("PatientID and account conflict", "E", "searching for Patient ID and account", "OKToAddOrUpdatePatient", "Given account was found, but given Patient ID was not found");
				bOK = false;
			}
			if (!bOK)
			{
				return bOK;
			}
			bool bUseVisitNum = bVisitNumToFindGiven & bUseVisitNumbersToFind & bVisitExists;
			bool bUseAcctNum = bAccountGiven & bUseAccountNumbersToFind & bAccountExists;
			bool bUseMRN = bMRNGiven & bMRNExists;
			bool bUsePID = bPatIDGiven & bPatientIDExists;
			bool bVisitNumError = bVisitNumToFindGiven & bUseVisitNumbersToFind & !bVisitExists;
			bool bAcctNumError = bAccountGiven & bUseAccountNumbersToFind & !bAccountExists;
			if (bPatientExists && (bUseVisitNum || bUseAcctNum || bUseMRN || bUsePID))
			{
				string ReadWhere = m_PatientQuery.BuildReadWhere(bUseVisitNum, bUseAcctNum, bUseMRN, bUsePID);
				iVisitCount = 0;
				iAccountCount = 0;
				bOK = m_PatientList.ReadTheOne(m_NNBase, ReadWhere, ref myDBReadCommand, ref m_DBPatient);
				if (!bOK && m_PatientList.m_Status.m_errortype == Convert.ToInt32(errortypes.NoMatch))
				{
					bOK = true;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("patient, account or visit not found for all ids given", isXml: false, "OKToAddOrUpdatePatient");
					}
					if ((bPatientIDExists || bMRNExists) && bAccountExists)
					{
						ReadWhere = m_PatientQuery.BuildReadWhere(bVisit: false, bAccountGiven & bAccountExists & bUseAccountNumbersToFind, bMRNGiven & bMRNExists, bPatIDGiven & bPatientIDExists);
						bOK = m_PatientList.ReadTheOne(m_NNBase, ReadWhere, ref myDBReadCommand, ref m_DBPatient);
						if (bOK)
						{
							bPatientExists = true;
							bAccountExists = true;
						}
						else
						{
							m_NNBase.log("account not found for the given patient ID, MRN and account num", isXml: false, "OKToAddOrUpdatePatient");
						}
					}
					if (m_PatientList.m_Status.m_errortype == Convert.ToInt32(errortypes.NoMatch))
					{
						bOK = true;
						if (bPatientIDExists || bMRNExists)
						{
							ReadWhere = m_PatientQuery.BuildReadWhere(bVisit: false, bAccount: false, bMRNGiven & bMRNExists, bPatIDGiven & bPatientIDExists);
							bOK = m_PatientList.ReadTheOne(m_NNBase, ReadWhere, ref myDBReadCommand, ref m_DBPatient);
							if (bOK)
							{
								bPatientExists = true;
								if (bAccountExists && !bMoveAccount)
								{
									m_NNBase.ReportErrorDB("Patient and account conflict", "E", "searching for patient and account", "OKToAddOrUpdatePatient", "Patient and account found, but account is for a different patient");
									return false;
								}
							}
							else
							{
								m_NNBase.log("patient not found for both the given patient ID and MRN", isXml: false, "OKToAddOrUpdatePatient");
							}
						}
					}
				}
				iAccountCount = m_DBPatient.m_PatientAccountList.GetNumUsedElements();
				if (bOK && iAccountCount == 1 && bAccountExists)
				{
					int pAccount = m_DBPatient.m_PatientAccountList.First();
					if (pAccount >= 0)
					{
						m_PatientAccountRec = (PatientAccountRec)m_DBPatient.m_PatientAccountList.m_Array[pAccount];
						iVisitCount = m_PatientAccountRec.m_PatientVisitList.GetNumUsedElements();
						if (iVisitCount == 1 && bVisitExists)
						{
							int pVisit = m_PatientAccountRec.m_PatientVisitList.First();
							if (pVisit >= 0)
							{
								m_PatientVisitRec = (PatientVisitRec)m_PatientAccountRec.m_PatientVisitList.m_Array[pVisit];
								VisitNumFound = m_PatientVisitRec.m_visitnum;
								myPatientTrackingRec.m_visituuid = (VisitUUID = (m_newPatientVisitRec.m_visit_UUID = m_PatientVisitRec.m_visit_UUID));
								if (myPatientTrackingRec.m_locname.Length == 0)
								{
									myPatientTrackingRec.m_locname = m_LocationList.LookupLocation(m_PatientVisitRec.m_locnum);
								}
								if (myPatientTrackingRec.m_dischargetime.Year <= 1800 || myPatientTrackingRec.m_dischargetime.Year == 2037)
								{
									myPatientTrackingRec.m_dischargetime = m_PatientVisitRec.m_dischargetime;
								}
							}
						}
					}
				}
				if (bOK && (iAccountCount > 1 || iVisitCount > 1))
				{
					m_DBPatient.LatestNonFutureActiveOrCurrentPatientVisitRec(m_DBPatient, ref m_PatientVisitRec);
					if (bVisitExists && m_PatientVisitRec != null)
					{
						m_PatientAccountRec = m_PatientVisitRec.m_PatientAccountRec;
						VisitNumFound = m_PatientVisitRec.m_visitnum;
						myPatientTrackingRec.m_visituuid = (VisitUUID = (m_newPatientVisitRec.m_visit_UUID = m_PatientVisitRec.m_visit_UUID));
						if (myPatientTrackingRec.m_locname.Length == 0)
						{
							myPatientTrackingRec.m_locname = m_LocationList.LookupLocation(m_PatientVisitRec.m_locnum);
						}
						if (myPatientTrackingRec.m_dischargetime.Year <= 1800 || myPatientTrackingRec.m_dischargetime.Year == 2037)
						{
							myPatientTrackingRec.m_dischargetime = m_PatientVisitRec.m_dischargetime;
						}
					}
				}
			}
			if (bPatientExists && (bVisitNumError || bAcctNumError) && m_NNBase.m_isLogging)
			{
				m_NNBase.log("patient ID and/or MRN found, but relevant account and/or visit number not found", isXml: false, "OKToAddOrUpdatePatient");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Patient " + (bPatientExists ? "found" : "not found") + ", PID \"" + PatientID + "\" " + (bPatientIDExists ? "found" : "not found") + ", MRN \"" + MedicalRecordNumber + "\" " + (bMRNExists ? "found" : "not found") + ", Acct \"" + AccountNumber + "\" " + (bAccountExists ? "found" : "not found") + ", Visit " + (bVisitExists ? "found" : "not found") + ", VisitToFind=\"" + VisitNumToFind + "\" VisitFound=\"" + VisitNumFound + "\"", isXml: false, "OKToAddOrUpdatePatient");
			}
			if (bOK && bPatientExists && !MatchingIDs(m_DBPatient, bNew: true))
			{
				bOK = false;
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
			}
		}
		else
		{
			bOK = false;
			m_NNBase.ReportErrorDB(AppErrorMsg = "Insufficient patient identification", "E", "checking validity of message", "OKToAddOrUpdatePatient", "ADT messages must contain at least one of (visit number, Account number, Medrec number or Patient ID)");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "No patient IDs provided", bError: true, ref myPTDBWriteCommand);
		}
		if (bOK && bPrevIDsToFind)
		{
			bool bPrevPatIDIsNewPatID = false;
			m_PrevIDs_PatientQuery.ClearSearchParams();
			m_PrevIDs_PatientQuery.FacilNum = m_prev_facil_num;
			if (bPrevPatIDGiven)
			{
				m_PrevIDs_PatientQuery.EnterpriseID = PreviousPatientID;
			}
			else if (MessageSubType != "A44")
			{
				bPrevPatIDIsNewPatID = true;
				m_PrevIDs_PatientQuery.EnterpriseID = PatientID;
			}
			if (bPrevMRNGiven)
			{
				m_PrevIDs_PatientQuery.MedRecNum = PreviousMedicalRecordNumber;
			}
			else if (MessageSubType != "A44")
			{
				m_PrevIDs_PatientQuery.MedRecNum = MedicalRecordNumber;
			}
			if (bPrevAcctGiven)
			{
				m_PrevIDs_PatientQuery.AccountNum = PreviousPatientAccount;
			}
			else if (MessageSubType != "A45")
			{
				m_PrevIDs_PatientQuery.AccountNum = AccountNumber;
			}
			if (bPrevVisitNumToFindGiven)
			{
				m_PrevIDs_PatientQuery.VisitNum = PreviousVisitNumToFind;
				m_PrevIDs_PatientQuery.bVisitExact = bPrevVisitNumToFindExact;
			}
			else
			{
				m_PrevIDs_PatientQuery.VisitNum = VisitNumToFind;
				m_PrevIDs_PatientQuery.bVisitExact = bVisitNumToFindExact;
			}
			m_PrevIDs_PatientQuery.bSpansFacilities = bSpansFacilities;
			m_PrevIDs_PatientQuery.PreviousRecordsExist(m_NNBase, ref myDBReadCommand, m_PatientQuery, bPrevPatIDIsNewPatID);
			bPrevPatientIDExists = m_PrevIDs_PatientQuery.bEnterpriseIDExists;
			bPrevMRNExists = m_PrevIDs_PatientQuery.bMedRecExists;
			bPrevAccountExists = m_PrevIDs_PatientQuery.bAccountExists;
			bPrevPatientExists = m_PrevIDs_PatientQuery.bPatientExists;
			bPrevVisitExists = m_PrevIDs_PatientQuery.bVisitExists;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log((bPatIDChanging ? ("PrevPatientIDExists: " + PreviousPatientID + " " + (bPrevPatientIDExists ? "true" : "false")) : "") + (bMRNChanging ? (", PrevMRNExists: " + PreviousMedicalRecordNumber + " " + (bPrevMRNExists ? "true" : "false")) : "") + (bAcctChanging ? (", PrevAccountExists: " + PreviousPatientAccount + " " + (bPrevAccountExists ? "true" : "false")) : "") + (bVisitNumChanging ? (", PrevVisitNumExists: " + PreviousVisitNumToFind + " " + (bPrevVisitExists ? "true" : "false")) : ""), isXml: false, "OKToAddOrUpdatePatient");
			}
			if (bPrevAcctGiven && bPrevAccountExists && bPrevPatientExists && bPrevMRNGiven && !bPrevMRNExists && (!bPrevPatIDGiven || (bPrevPatIDGiven && !bPrevPatientIDExists)))
			{
				m_NNBase.ReportErrorDB("MRN and account conflict", "E", "searching for prior MRN and account", "OKToAddOrUpdatePatient", "Given prior account was found, but given prior MRN was not found");
				bOK = false;
			}
			if (bPrevAcctGiven && bPrevAccountExists && bPrevPatientExists && bPrevPatIDGiven && !bPrevPatientIDExists && (!bPrevMRNGiven || (bPrevMRNGiven && !bPrevMRNExists)))
			{
				m_NNBase.ReportErrorDB("PatientID and account conflict", "E", "searching for prior Patient ID and account", "OKToAddOrUpdatePatient", "Given prior account was found, but given prior Patient ID was not found");
				bOK = false;
			}
			if (!bOK)
			{
				return bOK;
			}
			bool bUseVisitNum2 = (m_PrevIDs_PatientQuery.VisitNum.Length > 0) & bUseVisitNumbersToFind & bPrevVisitExists;
			bool bUseAcctNum2 = (m_PrevIDs_PatientQuery.AccountNum.Length > 0) & bUseAccountNumbersToFind & bPrevAccountExists;
			bool bUseMRN2 = (m_PrevIDs_PatientQuery.MedRecNum.Length > 0) & bPrevMRNExists;
			bool bUsePID2 = (m_PrevIDs_PatientQuery.EnterpriseID.Length > 0) & bPrevPatientIDExists;
			bool bVisitNumError2 = (m_PrevIDs_PatientQuery.VisitNum.Length > 0) & bUseVisitNumbersToFind & !bPrevVisitExists;
			bool bAcctNumError2 = (m_PrevIDs_PatientQuery.AccountNum.Length > 0) & bUseAccountNumbersToFind & !bPrevAccountExists;
			if (bPrevPatientExists && (bUseVisitNum2 || bUseAcctNum2 || bUseMRN2 || bUsePID2))
			{
				string ReadWhere2 = m_PrevIDs_PatientQuery.BuildReadWhere(bUseVisitNum2, bUseAcctNum2, bUseMRN2, bUsePID2);
				iPrevVisitCount = 0;
				iPrevAccountCount = 0;
				bOK = m_PrevIDs_PatientList.ReadTheOne(m_NNBase, ReadWhere2, ref myDBReadCommand, ref m_PrevIDs_DBPatient);
				if (!bOK && m_PrevIDs_PatientList.m_Status.m_errortype == Convert.ToInt32(errortypes.NoMatch))
				{
					bOK = true;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("patient, account or visit not found for all previous ids given", isXml: false, "OKToAddOrUpdatePatient");
					}
					if ((bPrevPatientIDExists || bPrevMRNExists) && bPrevAccountExists)
					{
						ReadWhere2 = m_PrevIDs_PatientQuery.BuildReadWhere(bVisit: false, bAccountToFind & bPrevAccountExists & bUseAccountNumbersToFind, bMRNToFind & bPrevMRNExists, bPatIDToFind & bPrevPatientIDExists);
						bOK = m_PrevIDs_PatientList.ReadTheOne(m_NNBase, ReadWhere2, ref myDBReadCommand, ref m_PrevIDs_DBPatient);
						if (bOK)
						{
							bPrevPatientExists = true;
							bPrevAccountExists = true;
						}
						else
						{
							m_NNBase.log("account not found for the given previous patient ID, MRN and account", isXml: false, "OKToAddOrUpdatePatient");
						}
					}
					if (m_PrevIDs_PatientList.m_Status.m_errortype == Convert.ToInt32(errortypes.NoMatch))
					{
						bOK = true;
						if (bPrevPatientIDExists || bPrevMRNExists)
						{
							ReadWhere2 = m_PrevIDs_PatientQuery.BuildReadWhere(bVisit: false, bAccount: false, bMRNToFind & bPrevMRNExists, bPatIDToFind & bPrevPatientIDExists);
							bOK = m_PrevIDs_PatientList.ReadTheOne(m_NNBase, ReadWhere2, ref myDBReadCommand, ref m_PrevIDs_DBPatient);
							if (bOK)
							{
								bPrevPatientExists = true;
								if (bPrevAccountExists && !bMoveAccount && !bMergeAccount && !bDeactAccount)
								{
									m_NNBase.ReportErrorDB("Previous IDs Patient and account conflict", "E", "searching for previous IDs patient and account", "OKToAddOrUpdatePatient", "Previous IDs Patient and account found, but account is for a different patient");
									return false;
								}
							}
							else
							{
								m_NNBase.log("patient not found for both the given previous patient ID and MRN", isXml: false, "OKToAddOrUpdatePatient");
							}
						}
					}
				}
				iPrevAccountCount = m_PrevIDs_DBPatient.m_PatientAccountList.GetNumUsedElements();
				if (bOK && iPrevAccountCount == 1 && bPrevAccountExists)
				{
					int pAccount2 = m_PrevIDs_DBPatient.m_PatientAccountList.First();
					if (pAccount2 >= 0)
					{
						m_PrevIDs_PatientAccountRec = (PatientAccountRec)m_PrevIDs_DBPatient.m_PatientAccountList.m_Array[pAccount2];
						iPrevVisitCount = m_PrevIDs_PatientAccountRec.m_PatientVisitList.GetNumUsedElements();
						if (iPrevVisitCount == 1 && bPrevVisitExists)
						{
							int pVisit2 = m_PrevIDs_PatientAccountRec.m_PatientVisitList.First();
							if (pVisit2 >= 0)
							{
								m_PrevIDs_PatientVisitRec = (PatientVisitRec)m_PrevIDs_PatientAccountRec.m_PatientVisitList.m_Array[pVisit2];
								PreviousVisitNumFound = m_PrevIDs_PatientVisitRec.m_visitnum;
								myPatientTrackingRec.m_PrevVisitUUID = (PreviousVisitUUID = m_PrevIDs_PatientVisitRec.m_visit_UUID);
								m_DBPatient.FillInPrevPatientTrackingLocationInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, m_PrevIDs_PatientVisitRec.m_locnum);
							}
						}
					}
				}
				if (bOK && (iPrevAccountCount > 1 || iPrevVisitCount > 1))
				{
					m_PrevIDs_DBPatient.LatestNonFutureActiveOrCurrentPatientVisitRec(m_PrevIDs_DBPatient, ref m_PrevIDs_PatientVisitRec);
					if (bPrevVisitExists && m_PrevIDs_PatientVisitRec != null)
					{
						m_PrevIDs_PatientAccountRec = m_PrevIDs_PatientVisitRec.m_PatientAccountRec;
						PreviousVisitNumFound = m_PrevIDs_PatientVisitRec.m_visitnum;
						myPatientTrackingRec.m_PrevVisitUUID = (PreviousVisitUUID = m_PrevIDs_PatientVisitRec.m_visit_UUID);
						m_DBPatient.FillInPrevPatientTrackingLocationInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, m_PrevIDs_PatientVisitRec.m_locnum);
					}
				}
			}
			if (bPrevPatientExists && (bVisitNumError2 || bAcctNumError2) && m_NNBase.m_isLogging)
			{
				m_NNBase.log("Previous patient ID and/or MRN found, but relevant account and/or visit number not found", isXml: false, "OKToAddOrUpdatePatient");
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("PreviousPatient " + (bPrevPatientExists ? "found" : "not found") + ", PreviousPID \"" + m_PrevIDs_PatientQuery.EnterpriseID + "\" " + (bPrevPatientIDExists ? "found" : "not found") + ", PreviousMRN \"" + m_PrevIDs_PatientQuery.MedRecNum + "\" " + (bPrevMRNExists ? "found" : "not found") + (bUseAcctNum2 ? (", PreviousAcct \"" + m_PrevIDs_PatientQuery.AccountNum + "\" " + (bPrevAccountExists ? "found" : "not found")) : "") + (bUseVisitNum2 ? (", PreviousVisit " + (bPrevVisitExists ? "found" : "not found") + ", PreviousVisitToFind=\"" + PreviousVisitNumToFind + "\" PreviousVistFound=\"" + PreviousVisitNumFound + "\"") : ""), isXml: false, "OKToAddOrUpdatePatient");
			}
			if (bOK && bPrevPatientExists && !MatchingIDs(m_PrevIDs_DBPatient, bNew: false))
			{
				bOK = false;
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
			}
		}
		if (bOK)
		{
			bPatientKeysGiven = (bMRNGiven | bPatIDGiven) & bFacilGiven;
			if (!bPatientKeysGiven && !bPatientExists && !bPrevPatientExists)
			{
				bOK = false;
				m_NNBase.ReportErrorDB(AppErrorMsg = "Insufficient patient identification", "E", "checking ADT message", "OKToAddOrUpdatePatient", "Message is missing either the MRN and Patient ID or the facility");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing patient IDs", bError: true, ref myPTDBWriteCommand);
			}
			if (bPatientKeysGiven && !bPatientExists && !bPrevPatientExists && !bNoAdmit)
			{
				bAddPatient = true;
				if (m_new_visit_UUID.Length == 0)
				{
					m_new_visit_UUID = Guid.NewGuid().ToString("N");
				}
			}
			if (bPatientKeysGiven && (bPatientExists || bPrevPatientExists) && (bPatientIDExists || bPrevPatientIDExists || bMRNExists || bPrevMRNExists))
			{
				bUpdatePatient = true;
			}
			bAccountKeysGiven = (bPatientKeysGiven | bPatientExists | bPrevPatientExists) & bAccountGiven;
			bAccountRequired = AccountIsRequired();
			if (!bAccountKeysGiven && !bAccountExists && !bPrevAccountExists && bAccountRequired)
			{
				bOK = false;
				m_NNBase.ReportErrorDB(AppErrorMsg = "Insufficient account identification", "E", "checking ADT message", "OKToAddOrUpdatePatient", "Message is missing the Account number");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing Account number", bError: true, ref myPTDBWriteCommand);
			}
			if (bAccountKeysGiven && !bAccountExists && !bPrevAccountExists && !bNoAdmit)
			{
				bAddAccount = true;
				if (m_new_visit_UUID.Length == 0)
				{
					m_new_visit_UUID = Guid.NewGuid().ToString("N");
				}
			}
			bool bImpliedAccountMerge = !bMergeAccount && bPrevOrNewAcctGiven && bAccountExists && bPrevAccountExists && m_PatientAccountRec != null && m_PatientAccountRec.m_accountnum != null && m_PatientAccountRec.m_accountnum.Length > 0 && m_PrevIDs_PatientAccountRec != null && m_PrevIDs_PatientAccountRec.m_accountnum != null && m_PrevIDs_PatientAccountRec.m_accountnum.Length > 0 && Comp.Compare(m_PatientAccountRec.m_accountnum, m_PrevIDs_PatientAccountRec.m_accountnum, CompOpt) != 0;
			bool bUpdateAccountViaImpliedMerge = bMergePatient & bImpliedAccountMerge;
			if (UseAccountNumbersToFind() && bAccountKeysGiven && (bAccountExists || bPrevAccountExists) && !bUpdateAccountViaImpliedMerge && bAcctChanging)
			{
				bUpdateAccount = true;
			}
			bVisitKeysGiven = (bPatientKeysGiven | bPatientExists | bPrevPatientExists) & (bAccountGiven | bAccountExists | bPrevAccountExists) & bVisitNumToAddGiven;
			bVisitRequired = VisitIsRequired();
			if (!bVisitKeysGiven && !bVisitExists && !bPrevVisitExists && bVisitRequired)
			{
				bOK = false;
				m_NNBase.ReportErrorDB(AppErrorMsg = "Insufficient visit identification", "E", "checking ADT message", "OKToAddOrUpdatePatient", "Message is missing either the Account number or the visit number");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing visit number", bError: true, ref myPTDBWriteCommand);
			}
			if (bVisitKeysGiven && (!bVisitExists || (!bVisitNumToFindExact && bAddAccount)) && !bNoAdmit)
			{
				bAddVisit = OKToAddVisit(ref bOK);
				if (bOK && !bAddVisit)
				{
					bOK = (bMergePatient & (bPatientExists | bPrevPatientExists)) | ((bMergeAccount | bMoveAccount | bDeactAccount) & (bAccountExists | bPrevAccountExists));
				}
				if (!bOK)
				{
					AppErrorMsg = "Visit not found, visit will not be added for " + MessageSubType;
					m_NNBase.ReportErrorDB(AppErrorMsg + " - " + MessageSubTypeDescription(MessageSubType), "E", "checking ADT message", "OKToAddOrUpdatePatient", "for this transaction type, visit records are not added if they are not found in the database");
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit not added for " + MessageSubType, bError: true, ref myPTDBWriteCommand);
					return bOK;
				}
			}
			else
			{
				if (!bMergeVisit && (bPrevVisitNumToFindExact || bVisitNumToFindExact) && bVisitExists && bPrevVisitExists && m_PatientVisitRec != null && m_PatientVisitRec.m_visitnum != null && m_PatientVisitRec.m_visitnum.Length > 0 && m_PrevIDs_PatientVisitRec != null && m_PrevIDs_PatientVisitRec.m_visitnum != null && m_PrevIDs_PatientVisitRec.m_visitnum.Length > 0)
				{
					Comp.Compare(m_PatientVisitRec.m_visitnum, m_PrevIDs_PatientVisitRec.m_visitnum, CompOpt);
				}
				bool bUpdateVisitViaImpliedMerge = bMergePatient & bImpliedAccountMerge;
				if (UseVisitNumbersToFind() && bVisitKeysGiven && bVisitExists && bPrevOrNewVisitNumGivenExact && !bUpdateVisitViaImpliedMerge)
				{
					bUpdateVisit = true;
				}
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("AddPatient: " + (bAddPatient ? "true" : "false") + ", UpdatePatient: " + (bUpdatePatient ? "true" : "false") + ", AddAccount: " + (bAddAccount ? "true" : "false") + ", UpdateAccount: " + (bUpdateAccount ? "true" : "false") + ", AddVisit: " + (bAddVisit ? "true" : "false") + ", UpdateVisit: " + (bUpdateVisit ? "true" : "false"), isXml: false, "OKToAddOrUpdatePatient");
			}
			if (bMRNGiven && bPatIDGiven)
			{
				string otherPID = "";
				string otherMRN = "";
				if ((bAddPatient || (bPatientKeysGiven && bMRNChanging && !bPatIDChanging)) && (otherPID = OtherPIDforMRN(MedicalRecordNumber, PatientID)).Length > 0)
				{
					m_NNBase.ReportErrorDB(AppErrorMsg = "Patient ID mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different patient ID (" + otherPID + ", not " + PatientID + ") exists for this MRN (" + MedicalRecordNumber + ")");
					bOK = false;
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
				}
				if ((bAddPatient || (bPatientKeysGiven && bPatIDChanging && !bMRNChanging)) && (otherMRN = OtherMRNforPID(MedicalRecordNumber, PatientID)).Length > 0)
				{
					m_NNBase.ReportErrorDB(AppErrorMsg = "MRN mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different MRN (" + otherMRN + ", not " + MedicalRecordNumber + ") exists for this patient ID (" + PatientID + ")");
					bOK = false;
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
				}
				if (bPatientKeysGiven && bMRNChanging && !bPatIDChanging)
				{
					if ((otherPID = OtherPIDforMRN(PreviousMedicalRecordNumber, PatientID)).Length > 0)
					{
						m_NNBase.ReportErrorDB(AppErrorMsg = "Patient ID mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different patient ID (" + otherPID + ", not " + PatientID + ") exists for the previous MRN (" + PreviousMedicalRecordNumber + ")");
						bOK = false;
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
					}
					if ((otherMRN = OtherThanOldOrNewMRNforPID()).Length > 0)
					{
						m_NNBase.ReportErrorDB(AppErrorMsg = "MRN mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different MRN (" + otherMRN + ", not " + MedicalRecordNumber + " or " + PreviousMedicalRecordNumber + ") exists for this patient ID (" + PatientID + ")");
						bOK = false;
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
					}
				}
				if (bPatientKeysGiven && bPatIDChanging && !bMRNChanging)
				{
					if ((otherMRN = OtherMRNforPID(MedicalRecordNumber, PreviousPatientID)).Length > 0)
					{
						m_NNBase.ReportErrorDB(AppErrorMsg = "MRN mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different MRN (" + otherMRN + ", not " + MedicalRecordNumber + ") exists for the previous patient ID (" + PreviousPatientID + ")");
						bOK = false;
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "MRNs mismatch", bError: true, ref myPTDBWriteCommand);
					}
					if ((otherPID = OtherThanOldOrNewPIDforMRN()).Length > 0)
					{
						m_NNBase.ReportErrorDB(AppErrorMsg = "Patient ID mismatch", "E", "checking ADT message", "OKToAddOrUpdatePatient", "a different patient ID (" + otherPID + ", not " + PatientID + " or " + PreviousPatientID + ") exists for this MRN (" + MedicalRecordNumber + ")");
						bOK = false;
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Patient IDs mismatch", bError: true, ref myPTDBWriteCommand);
					}
				}
			}
		}
		if (bOK && (!bVisitExists || m_PatientVisitRec == null) && bInvalidLoc)
		{
			m_NNBase.ReportErrorNoDB(AppErrorMsg = "Invalid location, visit will not be added", "E", "checking ADT message", "OKToAddOrUpdatePatient", "");
			bOK = false;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Location not found", bError: true, ref myPTDBWriteCommand);
		}
		if (bOK)
		{
			bool bVisitReallyExists = bVisitExists & (m_PatientVisitRec != null);
			bool bVisitWillBeMoved = bMoveVisit & bPrevVisitExists & (m_PrevIDs_PatientVisitRec != null);
			bool bVisitNumWillBeChanged = bVisitNumChanging & bPrevVisitExists & (m_PrevIDs_PatientVisitRec != null);
			if (!bVisitReallyExists && !bAddVisit && !bUpdateVisit && !bVisitWillBeMoved && !bVisitNumWillBeChanged && !bAcctChanging && !bMRNChanging && !bPatIDChanging && bVisitRequired)
			{
				bOK = false;
				AppErrorMsg = "The new visit does not exist and will not be created for the " + MessageSubType;
				m_NNBase.ReportErrorDB(AppErrorMsg + " - " + MessageSubTypeDescription(MessageSubType) + " message ", "E", "checking for visit add or update", "OKToAddOrUpdatePatient", "The new visit does not exist " + ((bPrevVisitExists & (m_PrevIDs_PatientVisitRec != null)) ? "but the prior visit exists. However" : "and the prior visit does not exist. Also") + ", neither the visit number nor the account number nor the MRN nor the Patient ID are changing. Therefore, no merge or move can be performed");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit error for " + MessageSubType, bError: true, ref myPTDBWriteCommand);
			}
			else if (bMoveVisit && !bVisitWillBeMoved)
			{
				bOK = false;
				AppErrorMsg = "The prior visit does not exist and will not be moved for the " + MessageSubType;
				m_NNBase.ReportErrorDB(AppErrorMsg + " - " + MessageSubTypeDescription(MessageSubType) + " message ", "E", "checking for prior visit to move", "OKToAddOrUpdatePatient", "The prior visit does not exist");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit error for " + MessageSubType, bError: true, ref myPTDBWriteCommand);
			}
		}
		return bOK;
	}

	private bool UseVisitNumbersToFind()
	{
		bool bUseFindVisitNums = true;
		switch (MessageSubType)
		{
		case "A18":
		case "A34":
		case "A35":
		case "A36":
		case "A39":
		case "A46":
		case "A40":
		case "A47":
		case "A41":
		case "A49":
		case "A44":
			bUseFindVisitNums = false;
			break;
		}
		return bUseFindVisitNums;
	}

	private bool UseAccountNumbersToFind()
	{
		bool bUseFindAcctNums = true;
		switch (MessageSubType)
		{
		case "A18":
			bUseFindAcctNums = bAcctDifferent;
			break;
		case "A34":
		case "A39":
		case "A46":
		case "A40":
		case "A47":
			bUseFindAcctNums = false;
			break;
		}
		return bUseFindAcctNums;
	}

	private bool DetermineMergesAndMoves(ref bool bPrevIDsToFind, ref bool bLocationRequired)
	{
		bool bOK = true;
		string sExpected = "";
		string sExpectedDifference = "";
		string sUnexpectedDifference = "";
		bPrevIDsToFind = bPatIDChanging | bMRNChanging | bAcctChanging | bVisitNumChanging | bCrossFacilityMergePatient | bCrossFacilityMergeAccount | bCrossFacilityMergeVisit | bCrossFacilityMoveAccount | bCrossFacilityMoveVisit | (MessageSubType == "A44") | (MessageSubType == "A45");
		if (bPrevIDsToFind && m_NNBase.m_isLogging)
		{
			m_NNBase.log((bPatIDChanging ? ("PrevPatientID: " + PreviousPatientID) : "") + (bMRNChanging ? (" PrevMRN: " + PreviousMedicalRecordNumber) : "") + (bAcctChanging ? (" PrevAccount: " + PreviousPatientAccount) : "") + (bVisitNumChanging ? (" PrevVisitNum: " + PreviousVisitNumToFind) : ""), isXml: false, "DetermineMergesAndMoves");
		}
		switch (MessageSubType)
		{
		case "A02":
		case "A06":
		case "A07":
		case "A08":
		case "A31":
			if ((MessageSubType == "A02" || MessageSubType == "A08" || MessageSubType == "A31") && (bSpansFacilities || bCrossFacilityTransfer))
			{
				sExpected = "Patient ID or MRN";
				if (!bPrevOrNewPatIDGiven && !bPrevOrNewMRNGiven)
				{
					MissingIDs(sExpected);
					bOK = false;
				}
			}
			else if ((MessageSubType == "A02" || MessageSubType == "A08" || MessageSubType == "A31") && !bSpansFacilities && !bCrossFacilityTransfer)
			{
				sExpected = "Patient ID or MRN or Account number";
				if (!bPrevOrNewPatIDGiven && !bPrevOrNewMRNGiven && !bPrevOrNewAcctGiven)
				{
					MissingIDs(sExpected);
					bOK = false;
				}
				goto default;
			}
			if (bPrevOrNewAcctGiven)
			{
				bDeactAccount = bAcctChanging || bCrossFacilityTransferSameAccount;
				bDeactVisit = bVisitNumChanging || bCrossFacilityTransferSameVisit;
			}
			else
			{
				sExpected = "Account number";
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bPatIDChanging || (bMRNChanging && !bCrossFacilityTransfer))
			{
				sUnexpectedDifference = "Patient ID or MRN";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityTransferSameVisit)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: false);
			}
			break;
		case "A18":
			sExpected = "Patient ID or MRN or Account number";
			if (!bPrevOrNewPatIDGiven && !bPrevOrNewMRNGiven && !bPrevOrNewAcctGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bPatIDChanging || bMRNChanging || bAcctChanging || bCrossFacilityMergeSameAccount || bCrossFacilityMergeSameVisit)
			{
				bMergePatient = bPatIDChanging | bMRNChanging;
				bMergeAccount = bAcctChanging | bCrossFacilityMergeSameAccount;
				bMergeVisit = bVisitNumChanging | bCrossFacilityMergeSameVisit;
			}
			else if (!bCrossFacilityMergePatient && !bCrossFacilityMergeAccount && !bCrossFacilityMergeVisit)
			{
				sExpectedDifference = "Patient ID or MRN or Account number or Visit number";
				SameIDs(sExpectedDifference);
			}
			if (bAcctChanging && bVisitNumTheSame && bCrossFacilityMergeSameVisit)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: true);
				bOK = false;
			}
			break;
		case "A34":
			sExpected = "Patient ID or MRN";
			if (!bPrevOrNewPatIDGiven && !bPrevOrNewMRNGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bMRNChanging || bPatIDChanging)
			{
				bMergePatient = true;
			}
			else if (!bCrossFacilityMergePatient)
			{
				sExpectedDifference = "Patient ID or MRN";
				SameIDs(sExpectedDifference);
			}
			if (bAcctChanging || bVisitNumChanging)
			{
				sUnexpectedDifference = "Account number or Visit number";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			break;
		case "A35":
			sExpected = "Account number";
			if (!bPrevOrNewAcctGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bAcctChanging || bCrossFacilityMergeSameAccount)
			{
				bMergeAccount = true;
			}
			else if (!bCrossFacilityMergeAccount)
			{
				sExpectedDifference = "Account number";
				SameIDs(sExpectedDifference);
			}
			if ((bPatIDChanging || bMRNChanging) && !bCrossFacilityMergeAccount)
			{
				sUnexpectedDifference = "Patient ID or MRN";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			if (bAcctChanging && bVisitNumTheSame)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: false);
			}
			break;
		case "A36":
			sExpected = "(Patient ID or MRN) and Account number";
			if ((!bPrevOrNewPatIDGiven && !bPrevOrNewMRNGiven) || !bPrevOrNewAcctGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if ((bPatIDChanging || bMRNChanging) && (bAcctChanging || bCrossFacilityMergeSameAccount))
			{
				bMergePatient = true;
				bMergeAccount = true;
				bMergeVisit = bVisitNumChanging | bCrossFacilityMergeSameVisit;
			}
			else if (!bCrossFacilityMergePatient && !bCrossFacilityMergeAccount)
			{
				sExpectedDifference = "Patient ID or MRN or Account number";
				SameIDs(sExpectedDifference);
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityMergeAccount && bCrossFacilityMergeSameVisit)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: true);
				bOK = false;
			}
			break;
		case "A39":
		case "A46":
			sExpected = "Patient ID";
			if (!bPrevOrNewPatIDGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bPatIDChanging)
			{
				bMergePatient = true;
			}
			else if (bPatIDTheSame)
			{
				sExpectedDifference = "Patient ID";
				SameIDs(sExpectedDifference);
			}
			if (bMRNChanging || bAcctChanging || bVisitNumChanging)
			{
				sUnexpectedDifference = "Medical Record number or Account number or Visit Number";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			break;
		case "A40":
		case "A47":
			sExpected = "MRN";
			if (!bPrevOrNewMRNGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bMRNChanging || bCrossFacilityMergePatient)
			{
				bMergePatient = true;
			}
			else if (!bCrossFacilityMergePatient)
			{
				sExpectedDifference = "MRN";
				SameIDs(sExpectedDifference);
			}
			if (bPatIDChanging || bAcctChanging)
			{
				sUnexpectedDifference = "Patient ID or Account number";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			break;
		case "A41":
		case "A49":
			sExpected = "Account number";
			if (!bPrevOrNewAcctGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bAcctChanging || bCrossFacilityMergeSameAccount)
			{
				bMergeAccount = true;
			}
			else if (!bCrossFacilityMergeAccount)
			{
				sExpectedDifference = "Account number";
				SameIDs(sExpectedDifference);
			}
			if (bPatIDChanging || (bMRNChanging && !bCrossFacilityMergeAccount))
			{
				sUnexpectedDifference = "Patient ID or MRN";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityMergeAccount)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: true);
				bOK = false;
			}
			break;
		case "A42":
		case "A50":
			sExpected = "Exact visit number";
			if (!bPrevOrNewVisitNumGivenExact)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bVisitNumChanging || bCrossFacilityMergeSameVisit)
			{
				bMergeVisit = true;
			}
			else if (!bCrossFacilityMergeVisit)
			{
				sExpectedDifference = "Visit number";
				SameIDs(sExpectedDifference);
			}
			if (bPatIDChanging || (bMRNChanging && !bCrossFacilityMergeVisit))
			{
				sUnexpectedDifference = "Patient ID or MRN";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: false, bError: true);
				bOK = false;
			}
			break;
		case "A44":
			sExpected = "MRN and Account number";
			if (!bPrevOrNewMRNGiven || !bPrevOrNewAcctGiven)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if ((bMRNDifferent || bCrossFacilityMoveAccount) && bPrevOrNewAcctGiven)
			{
				bMoveAccount = true;
			}
			else if (bMRNTheSame && bPrevOrNewAcctGiven && !bCrossFacilityMoveAccount)
			{
				sExpectedDifference = "MRN";
				SameIDs(sExpectedDifference);
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityMoveAccount)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: false);
			}
			break;
		case "A45":
			sExpected = "MRN and Account number and Exact Visit number";
			if (!bPrevOrNewMRNGiven || !bPrevOrNewAcctGiven || !bPrevOrNewVisitNumGivenExact)
			{
				MissingIDs(sExpected);
				bOK = false;
			}
			if (bPrevOrNewMRNGiven && (bAcctDifferent || bCrossFacilityMoveSameVisit) && bPrevOrNewVisitNumGivenExact)
			{
				bMoveVisit = true;
			}
			else if (bPrevOrNewMRNGiven && bAcctTheSame && bPrevOrNewVisitNumGivenExact && !bCrossFacilityMoveVisit)
			{
				sExpectedDifference = "Account number";
				SameIDs(sExpectedDifference);
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityMoveSameVisit)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: false);
			}
			break;
		default:
			bMergePatient = bPatIDChanging || bMRNChanging || bCrossFacilityMergePatient;
			bMergeAccount = bAcctChanging || bCrossFacilityMergeAccount;
			bMergeVisit = bVisitNumChanging || bCrossFacilityMergeVisit;
			if (((bPatIDChanging || bMRNChanging) && !bCrossFacilityMergePatient) || (bAcctChanging && !bCrossFacilityMergePatient && !bCrossFacilityMergeAccount) || (bVisitNumChanging && !bCrossFacilityMergeAccount && !bCrossFacilityMergeVisit))
			{
				sUnexpectedDifference = "Patient ID or MRN or Account number or Visit number";
				UnexpectedIDChange(sUnexpectedDifference, bMergeOK: true, bError: false);
			}
			if (bAcctChanging && bVisitNumTheSame && !bCrossFacilityTransferSameVisit && !bCrossFacilityMergeSameVisit)
			{
				sExpectedDifference = "Visit number";
				AccountChangeWithSameVisitNums(bError: true);
				bOK = false;
			}
			break;
		}
		bLocationRequired = !bMergePatient & !bMergeAccount & !bMoveAccount & (!bMoveVisit | (bMoveVisit & (bVisitNumToFindGiven | bPrevVisitNumToFindGiven)));
		return bOK;
	}

	private void SameIDs(string sExpectedDifference)
	{
		AppWarningMsg = "Current and prior " + sExpectedDifference + " are the same for " + MessageSubType;
		m_NNBase.ReportErrorNoDB(AppWarningMsg + " - " + MessageSubTypeDescription(MessageSubType), "W", "checking ADT message", "SameIDs", "Current and prior " + sExpectedDifference + " have the same value so no merge or move will be performed");
	}

	private void AccountChangeWithSameVisitNums(bool bError)
	{
		string sMessage = "The current and prior account numbers were different but the current and prior visit numbers have the same value for the " + MessageSubType + " - " + MessageSubTypeDescription(MessageSubType) + " message";
		if (bError)
		{
			AppErrorMsg = "The account number is changing but the visit number is not";
			m_NNBase.ReportErrorDB(sMessage, "E", "checking ADT message", "AccountChangeWithSameVisitNums", "");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Account/Visit numbers mismatch", bError: true, ref myPTDBWriteCommand);
		}
		else
		{
			m_NNBase.ReportErrorNoDB(sMessage, "I", "checking ADT message", "AccountChangeWithSameVisitNums", "");
		}
	}

	private void MissingIDs(string sExpected)
	{
		m_NNBase.ReportErrorDB((AppErrorMsg = "Missing or incomplete current or prior identifiers required for the " + MessageSubType) + " - " + MessageSubTypeDescription(MessageSubType), "I", "checking ADT message", "MissingIDs", "Current or prior " + sExpected + " expected");
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing Patient IDs", bError: true, ref myPTDBWriteCommand);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Missing Patient IDs", bError: true, ref myPTDBWriteCommand);
	}

	private void UnexpectedIDChange(string sUnexpectedDifference, bool bMergeOK, bool bError)
	{
		string sMessage = "Merge segment with unexpected prior " + sUnexpectedDifference + " for the " + MessageSubType + " - " + MessageSubTypeDescription(MessageSubType) + " message";
		if (bError)
		{
			AppErrorMsg = "Merge segment with unexpected prior " + sUnexpectedDifference + " for the " + MessageSubType;
			m_NNBase.ReportErrorDB(sMessage, "E", "checking ADT message", "UnexpectedIDChange", bMergeOK ? "" : "the implied merge cannot be performed with this message type");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Can't do implied merge", bError: true, ref myPTDBWriteCommand);
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Can't do implied merge", bError: true, ref myPTDBWriteCommand);
		}
		else
		{
			m_NNBase.ReportErrorNoDB(sMessage, "W", "checking ADT message", "UnexpectedIDChange", bMergeOK ? "" : "the implied merge cannot be performed with this message type");
		}
	}

	private bool OKToAddVisit(ref bool bOK)
	{
		bOK = false;
		if (bAddVisitOK)
		{
			bOK = true;
		}
		else
		{
			if (sAdmitOnUpdateTypes.Length > 0)
			{
				string[] sAdmitTypeArray = sAdmitOnUpdateTypes.Split(',');
				string[] array = sAdmitTypeArray;
				foreach (string sAdmitType in array)
				{
					if (MessageSubType == sAdmitType)
					{
						bAddVisitOK = true;
						bOK = true;
					}
				}
			}
			if (!bAddVisitOK && (MessageSubType == "A01" || MessageSubType == "A04" || MessageSubType == "A05" || MessageSubType == "A06" || MessageSubType == "A07" || MessageSubType == "A13" || MessageSubType == "A14" || MessageSubType == "A22" || MessageSubType == "A52"))
			{
				bAddVisitOK = true;
				bOK = true;
			}
			if (!bAddVisitOK)
			{
				if (!VisitIsRequired())
				{
					bOK = true;
				}
				else if (MessageSubType == "A42" || MessageSubType == "A50")
				{
					bAddVisitOK = true;
					bOK = true;
				}
				else if (MessageSubType == "A45")
				{
					if (!bPrevVisitExists)
					{
						bAddVisitOK = true;
					}
					bOK = true;
				}
			}
		}
		return bAddVisitOK;
	}

	private bool OKToAddAccount(ref bool bOK)
	{
		bOK = false;
		if (bAddAccountOK)
		{
			bOK = true;
		}
		else
		{
			if (sAdmitOnUpdateTypes.Length > 0)
			{
				string[] sAdmitTypeArray = sAdmitOnUpdateTypes.Split(',');
				string[] array = sAdmitTypeArray;
				foreach (string sAdmitType in array)
				{
					if (MessageSubType == sAdmitType)
					{
						bAddAccountOK = true;
						bOK = true;
					}
				}
			}
			if (!bAddAccountOK && (MessageSubType == "A01" || MessageSubType == "A04" || MessageSubType == "A05" || MessageSubType == "A06" || MessageSubType == "A07" || MessageSubType == "A13" || MessageSubType == "A14" || MessageSubType == "A22" || MessageSubType == "A52"))
			{
				bAddAccountOK = true;
				bOK = true;
			}
			if (!bAddAccountOK && (MessageSubType == "A18" || MessageSubType == "A34" || MessageSubType == "A35" || MessageSubType == "A36" || MessageSubType == "A39" || MessageSubType == "A40" || MessageSubType == "A41" || MessageSubType == "A44" || MessageSubType == "A46" || MessageSubType == "A47" || MessageSubType == "A49"))
			{
				bAddAccountOK = true;
				bOK = true;
			}
		}
		return bAddAccountOK;
	}

	private bool AccountIsRequired()
	{
		bool bAccountIsRequired = true;
		if (MessageSubType == "A18" || MessageSubType == "A34" || MessageSubType == "A39" || MessageSubType == "A40" || MessageSubType == "A46" || MessageSubType == "A47")
		{
			bAccountIsRequired = false;
		}
		return bAccountIsRequired;
	}

	private bool VisitIsRequired()
	{
		bool bVisitIsRequired = true;
		if (MessageSubType == "A18" || MessageSubType == "A34" || MessageSubType == "A35" || MessageSubType == "A36" || MessageSubType == "A39" || MessageSubType == "A40" || MessageSubType == "A41" || MessageSubType == "A44" || MessageSubType == "A46" || MessageSubType == "A47" || MessageSubType == "A49")
		{
			bVisitIsRequired = false;
		}
		return bVisitIsRequired;
	}

	private void GetVisitNumToUse(bool bNew)
	{
		DateTime AdmitTimeToUse = DateTime.MinValue;
		AdmitTimeToUse = ((AdmitDateTime.Length <= 0) ? DateTime.Now : AdmitTime);
		if (bNew)
		{
			string AcctNumToUse = "";
			string VisitNumToUse = "";
			bool bFromADT = true;
			AcctNumToUse = ((AccountNumber.Length <= 0) ? PreviousPatientAccount : AccountNumber);
			if (VisitNumFromADT.Length > 0)
			{
				VisitNumToUse = VisitNumFromADT;
			}
			else if (PreviousVisitNumFromADT.Length > 0)
			{
				VisitNumToUse = PreviousVisitNumFromADT;
			}
			else if (!bMultipleVisitsPerAccount)
			{
				VisitNumToUse = AcctNumToUse;
				bFromADT = false;
			}
			string[] AccountNums = new string[2] { AccountNumber, PreviousPatientAccount };
			VisitNumToAdd = m_DBPatient.GetVisitNum(VisitNumToUse, bFromADT, AccountNums, AdmitTimeToUse, ref VisitNumToFind, ref bVisitNumToFindExact);
			bVisitNumToFindGiven = VisitNumToFind.Length > 0;
			bVisitNumToAddGiven = VisitNumToAdd.Length > 0;
		}
		else
		{
			string PreviousAcctNumToUse = "";
			string PreviousVisitNumToUse = "";
			bool bFromADT2 = true;
			PreviousAcctNumToUse = ((PreviousPatientAccount.Length <= 0) ? AccountNumber : PreviousPatientAccount);
			if (PreviousVisitNumFromADT.Length > 0)
			{
				PreviousVisitNumToUse = PreviousVisitNumFromADT;
			}
			else if (VisitNumFromADT.Length > 0)
			{
				PreviousVisitNumToUse = VisitNumFromADT;
			}
			else if (!bMultipleVisitsPerAccount)
			{
				PreviousVisitNumToUse = PreviousAcctNumToUse;
				bFromADT2 = false;
			}
			string[] AccountNums2 = new string[2] { AccountNumber, PreviousAcctNumToUse };
			m_DBPatient.GetVisitNum(PreviousVisitNumToUse, bFromADT2, AccountNums2, AdmitTimeToUse, ref PreviousVisitNumToFind, ref bPrevVisitNumToFindExact);
			bPrevVisitNumToFindGiven = PreviousVisitNumToFind.Length > 0;
		}
	}

	private bool MatchingIDs(PatientRec myPatient, bool bNew)
	{
		bool bOK = true;
		if (myPatient.m_PatientID.Length > 0 || myPatient.m_medrecnum.Length > 0)
		{
			string oldPatientID = myPatient.m_PatientID;
			string oldMedrecNum = myPatient.m_medrecnum;
			string NewPrev = (bNew ? "new/current" : "previous");
			string NewPrevPatients = NewPrev + " patients ";
			string NewPrevPatAccounts = NewPrev + " patient_accounts ";
			string NewPrevPatVisits = NewPrev + " patient_visits ";
			string NewAndPrevMRNs = " MRN(s) (" + MedicalRecordNumber + "/" + PreviousMedicalRecordNumber + ")";
			string NewAndPrevPatIDs = " Patient ID(s) (" + PatientID + "/" + PreviousPatientID + ")";
			string NewAndPrevAccts = " Account(s) (" + AccountNumber + "/" + PreviousPatientAccount + ")";
			string NewAndPrevVisitNums = " Visit Number(s) (" + VisitNumToFind + "/" + PreviousVisitNumToFind + ")";
			bool bPatientIDMatch = (PatientID.Length > 0 && oldPatientID.Length > 0 && Comp.Compare(oldPatientID, PatientID, CompOpt) == 0) || (bNew && (PatientID.Length == 0 || oldPatientID.Length == 0));
			bool bPrevPatientIDMatch = (PreviousPatientID.Length > 0 && oldPatientID.Length > 0 && Comp.Compare(oldPatientID, PreviousPatientID, CompOpt) == 0) || (!bNew && (PreviousPatientID.Length == 0 || oldPatientID.Length == 0));
			if (!bPatientIDMatch && !bPrevPatientIDMatch)
			{
				m_NNBase.ReportErrorDB(AppErrorMsg = "Patient ID mismatch", "E", "checking ADT message", "MatchingIDs", "the patient ID (" + oldPatientID + ") for the " + NewPrevPatients + "record retrieved by the " + NewAndPrevMRNs + " and/or the " + NewAndPrevAccts + " and/or the " + NewAndPrevVisitNums + " does not match the previous patient ID (" + PreviousPatientID + ") or new patient ID (" + PatientID + ") in the message");
				bOK = false;
			}
			bool bMedrecMatch = (MedicalRecordNumber.Length > 0 && oldMedrecNum.Length > 0 && Comp.Compare(oldMedrecNum, MedicalRecordNumber, CompOpt) == 0) || (bNew && (MedicalRecordNumber.Length == 0 || oldMedrecNum.Length == 0));
			bool bPrevMedrecMatch = (PreviousMedicalRecordNumber.Length > 0 && oldMedrecNum.Length > 0 && Comp.Compare(oldMedrecNum, PreviousMedicalRecordNumber, CompOpt) == 0) || (!bNew && (PreviousMedicalRecordNumber.Length == 0 || oldMedrecNum.Length == 0));
			if (!bMedrecMatch && !bPrevMedrecMatch)
			{
				m_NNBase.ReportErrorDB(AppErrorMsg = "Medrec number mismatch", "E", "checking ADT message", "MatchingIDs", "the MRN (" + oldMedrecNum + ") for the " + NewPrevPatients + "record retrieved by the " + NewAndPrevPatIDs + " and/or the " + NewAndPrevAccts + " and/or the " + NewAndPrevVisitNums + ") does not match the previous MRN (" + PreviousMedicalRecordNumber + ") or new MRN (" + MedicalRecordNumber + ") in the message");
				bOK = false;
			}
			if (bOK)
			{
				bool bAccountToMatch = AccountNumber.Length > 0 || PreviousPatientAccount.Length > 0;
				bool bAccountMatchFound = false;
				bool bVisitMatchFound = false;
				bool bVisitToMatch = false;
				string oldAccountNums = "";
				string oldVisitNums = "";
				int VisitNumLen = VisitNumToFind.Length;
				if (VisitNumLen > 0 && !bVisitNumToFindExact)
				{
					VisitNumLen = VisitNumToFind.IndexOf('%');
				}
				int PrevVisitNumLen = PreviousVisitNumToFind.Length;
				if (PrevVisitNumLen > 0 && !bPrevVisitNumToFindExact)
				{
					PrevVisitNumLen = PreviousVisitNumToFind.IndexOf('%');
				}
				if (VisitNumLen > 0 || PrevVisitNumLen > 0)
				{
					bVisitToMatch = true;
				}
				int pPatientAccount = myPatient.m_PatientAccountList.First();
				while (((bAccountToMatch && !bAccountMatchFound) || (bVisitToMatch && !bVisitMatchFound)) && pPatientAccount >= 0)
				{
					PatientAccountRec wrkPatientAccount = (PatientAccountRec)myPatient.m_PatientAccountList.m_Array[pPatientAccount];
					string oldAccountNum = wrkPatientAccount.m_accountnum;
					if (oldAccountNums.Length > 0)
					{
						oldAccountNums += ",";
					}
					oldAccountNums += oldAccountNum;
					bool bAccountMatch = (AccountNumber.Length > 0 && oldAccountNum.Length > 0 && Comp.Compare(oldAccountNum, AccountNumber, CompOpt) == 0) || (bNew && (AccountNumber.Length == 0 || oldAccountNum.Length == 0));
					bool bPrevAccountMatch = (PreviousPatientAccount.Length > 0 && oldAccountNum.Length > 0 && Comp.Compare(oldAccountNum, PreviousPatientAccount, CompOpt) == 0) || (!bNew && (PreviousPatientAccount.Length == 0 || oldAccountNum.Length == 0));
					if (bAccountMatch || bPrevAccountMatch)
					{
						bAccountMatchFound = true;
					}
					if (bVisitToMatch)
					{
						int pPatientVisit = wrkPatientAccount.m_PatientVisitList.First();
						while (!bVisitMatchFound && pPatientVisit >= 0)
						{
							PatientVisitRec wrkPatientVisitRec = (PatientVisitRec)wrkPatientAccount.m_PatientVisitList.m_Array[pPatientVisit];
							string oldVisitNum = wrkPatientVisitRec.m_visitnum;
							if (oldVisitNums.Length > 0)
							{
								oldVisitNums += ",";
							}
							oldVisitNums += oldVisitNum;
							bool bVisitMatch = (VisitNumLen > 0 && oldVisitNum.Length > 0 && oldVisitNum.Length >= VisitNumLen && Comp.Compare(oldVisitNum, 0, VisitNumLen, VisitNumToFind, 0, VisitNumLen, CompOpt) == 0) || (bNew && (VisitNumLen == 0 || oldVisitNum.Length == 0));
							bool bPrevVisitMatch = (PrevVisitNumLen > 0 && oldVisitNum.Length > 0 && oldVisitNum.Length >= PrevVisitNumLen && Comp.Compare(oldVisitNum, 0, PrevVisitNumLen, PreviousVisitNumToFind, 0, PrevVisitNumLen, CompOpt) == 0) || (!bNew && (PrevVisitNumLen == 0 || oldVisitNum.Length == 0));
							if (bVisitMatch || bPrevVisitMatch)
							{
								bVisitMatchFound = true;
							}
							if (!bVisitMatchFound)
							{
								pPatientVisit = wrkPatientAccount.m_PatientVisitList.Next();
							}
						}
					}
					if ((bAccountToMatch && !bAccountMatchFound) || (bVisitToMatch && !bVisitMatchFound))
					{
						pPatientAccount = myPatient.m_PatientAccountList.Next();
					}
				}
				if (bAccountToMatch && !bAccountMatchFound && !OKToAddAccount(ref bOK))
				{
					m_NNBase.ReportErrorDB(AppErrorMsg = "Account number mismatch", "E", "checking ADT message", "MatchingIDs", "the account number(s) (" + oldAccountNums + ") for the " + NewPrevPatAccounts + "record retrieved by the " + NewAndPrevPatIDs + " and/or the " + NewAndPrevMRNs + " and/or the " + NewAndPrevVisitNums + " do not match the previous account number (" + PreviousPatientAccount + ") or new account number (" + AccountNumber + ") in the message");
					bOK = false;
				}
				if (bOK && bVisitToMatch && !bVisitMatchFound)
				{
					bool bVisitCanBeAddedOrWillBeImplicitlyMoved = false;
					OKToAddVisit(ref bVisitCanBeAddedOrWillBeImplicitlyMoved);
					if (!bVisitCanBeAddedOrWillBeImplicitlyMoved)
					{
						AppErrorMsg = "Visit not found, visit will not be added for " + MessageSubType;
						m_NNBase.ReportErrorDB(AppErrorMsg + " - " + MessageSubTypeDescription(MessageSubType), "E", "checking ADT message", "MatchingIDs", "for this transaction type, visit records are not added if they are not found in the database. The visit number(s) (" + oldVisitNums + ") for the " + NewPrevPatVisits + " record(s) retrieved by the " + NewAndPrevPatIDs + " and/or the " + NewAndPrevMRNs + " and/or the " + NewAndPrevAccts + " do not match the previous visit number (" + PreviousVisitNumToFind + ") or new visit number (" + VisitNumToFind + ") in the message");
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit not added for " + MessageSubType, bError: true, ref myPTDBWriteCommand);
						bOK = false;
					}
				}
			}
		}
		return bOK;
	}

	private string OtherPIDforMRN(string MRN, string PatID)
	{
		string ValueFound = "";
		string where = "";
		PatientQuery loc_tst_Patient = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
		if (MRN.Length > 0 && PatID.Length > 0)
		{
			if (m_facil_num.Length > 0)
			{
				where = where + "(p.facil_num = '" + m_facil_num + "') and ";
			}
			where = where + "(p.medrec_num = '" + MRN + "')";
			where = where + " and (p.Patient_ID is not null) and (p.Patient_ID != '') and (p.Patient_ID != '" + PatID + "')";
			ValueFound = loc_tst_Patient.PatientValueExists(m_NNBase, "p.Patient_ID", where, ref myDBReadCommand);
		}
		return ValueFound;
	}

	private string OtherMRNforPID(string MRN, string PatID)
	{
		string ValueFound = "";
		string where = "";
		PatientQuery loc_tst_Patient = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
		if (MRN.Length > 0 && PatID.Length > 0)
		{
			if (m_facil_num.Length > 0)
			{
				where = where + "(p.facil_num = '" + m_facil_num + "') and ";
			}
			where = where + "(p.Patient_ID = '" + PatID + "')";
			where = where + " and (p.medrec_num is not null) and (p.medrec_num != '') and (p.medrec_num != '" + MRN + "')";
			ValueFound = loc_tst_Patient.PatientValueExists(m_NNBase, "p.medrec_num", where, ref myDBReadCommand);
		}
		return ValueFound;
	}

	private string OtherThanOldOrNewMRNforPID()
	{
		string ValueFound = "";
		string where = "";
		PatientQuery loc_tst_Patient = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
		if (MedicalRecordNumber.Length > 0 && PatientID.Length > 0 && PreviousMedicalRecordNumber.Length > 0)
		{
			if (m_facil_num.Length > 0)
			{
				where = where + "(p.facil_num = '" + m_facil_num + "') and ";
			}
			where = where + "(p.Patient_ID = '" + PatientID + "')";
			where = where + " and (p.medrec_num is not null) and (p.medrec_num != '') and (p.medrec_num != '" + MedicalRecordNumber + "')";
			where += " and (p.medrec_num != '";
			where += PreviousMedicalRecordNumber;
			where += "')";
			ValueFound = loc_tst_Patient.PatientValueExists(m_NNBase, "p.medrec_num", where, ref myDBReadCommand);
		}
		return ValueFound;
	}

	private string OtherThanOldOrNewPIDforMRN()
	{
		string ValueFound = "";
		string where = "";
		PatientQuery loc_tst_Patient = new PatientQuery(bMRNsCrossFacilities, bAccountNumsCrossFacilities, bVisitNumsCrossPatients, bVisitNumsCrossFacilities);
		if (MedicalRecordNumber.Length > 0 && PatientID.Length > 0 && PreviousPatientID.Length > 0)
		{
			if (m_facil_num.Length > 0)
			{
				where = where + "(p.facil_num = '" + m_facil_num + "') and ";
			}
			where = where + "(p.medrec_num = '" + MedicalRecordNumber + "')";
			where = where + " and (p.Patient_ID is not null) and (p.Patient_ID != '') and (p.Patient_ID != '" + PatientID + "')";
			where += " and (p.Patient_ID != '";
			where += PreviousPatientID;
			where += "')";
			ValueFound = loc_tst_Patient.PatientValueExists(m_NNBase, "p.Patient_ID", where, ref myDBReadCommand);
		}
		return ValueFound;
	}

	private bool CanWeGetPatientVisitInfo(bool bNew)
	{
		return (bNew & (bPatIDGiven | (bFacilGiven & (bMRNGiven | bAccountGiven)) | (bMRNGiven & bMRNCanSpanFacilities))) | (!bNew & (bPrevOrNewPatIDGiven | ((bFacilGiven | bPrevFacilGiven) & (bPrevOrNewMRNGiven | bPrevOrNewAcctGiven)) | (bPrevOrNewMRNGiven & bMRNCanSpanFacilities)));
	}

	private bool GetPatientVisitInfo(bool bNew, ref int iStatPatient, ref int iStatAccount, ref int iStatVisit)
	{
		bool bOK = false;
		bool bLatestVisitFound = false;
		iStatPatient = (iStatAccount = (iStatVisit = Convert.ToInt32(errortypes.AOK)));
		PatientList loc_tst_Patient = new PatientList();
		PatientVisitRec loc_test_visit = new PatientVisitRec();
		try
		{
			string where = "";
			bool bGotPatID = (bNew && bPatIDGiven) || (!bNew && (bPrevPatIDGiven || bPatIDGiven));
			bool bGotMRN = (bNew && bMRNGiven) || (!bNew && (bPrevMRNGiven || bMRNGiven));
			bool bGotAcct = (bNew && bAccountGiven) || (!bNew && (bPrevAcctGiven || bAccountGiven));
			if (!bGotPatID && !bGotMRN && !bGotAcct)
			{
				iStatPatient = (iStatAccount = (iStatVisit = Convert.ToInt32(errortypes.NoMatch)));
				return bOK = false;
			}
			if (bGotPatID && bGotMRN && bGotAcct)
			{
				where += "((";
			}
			else if ((bGotPatID || bGotMRN) && bGotAcct)
			{
				where += "(";
			}
			if (bGotPatID)
			{
				if (bGotMRN && bMRNsCrossFacilities)
				{
					where += "(";
				}
				where = ((!bNew && bPrevPatIDGiven) ? (where + "(p.Patient_ID = '" + PreviousPatientID.Replace("'", "''").Replace("\"", "") + "')") : (where + "(p.Patient_ID = '" + PatientID.Replace("'", "''").Replace("\"", "") + "')"));
			}
			if (bGotMRN)
			{
				if (bGotPatID)
				{
					where += " and ";
				}
				where = ((!bNew && bPrevMRNGiven) ? (where + "(p.medrec_num = '" + PreviousMedicalRecordNumber.Replace("'", "''").Replace("\"", "") + "')") : (where + "(p.medrec_num = '" + MedicalRecordNumber.Replace("'", "''").Replace("\"", "") + "')"));
				if (bGotPatID && bMRNsCrossFacilities)
				{
					where += ")";
				}
			}
			if (bGotAcct)
			{
				if (bGotPatID || bGotMRN)
				{
					if (bGotPatID && bGotMRN)
					{
						where += ")";
					}
					where += " and ";
				}
				where = ((!bNew && bPrevAcctGiven) ? (where + "(pa.account_num = '" + PreviousPatientAccount.Replace("'", "''").Replace("\"", "") + "')") : (where + "(pa.account_num = '" + AccountNumber.Replace("'", "''").Replace("\"", "") + "')"));
				if (bGotPatID || bGotMRN)
				{
					where += ")";
				}
			}
			if ((bNew && bFacilGiven) || (!bNew && (bFacilGiven || bPrevFacilGiven)))
			{
				if (bGotPatID || bGotMRN || bGotAcct)
				{
					where += " and ";
				}
				where = where + "(p.facil_num = '" + (bNew ? m_facil_num : (bPrevFacilGiven ? m_prev_facil_num : m_facil_num)) + "')";
			}
			int iAccCount = 0;
			int iVisCount = 0;
			if (bNew)
			{
				iAccountCount = (iVisitCount = 0);
			}
			else
			{
				iPrevAccountCount = (iPrevVisitCount = 0);
			}
			DBPatient myPatient = new DBPatient(m_b_loc_last_update_inst_class_column, m_b_loc_last_update_inst_type_column);
			bOK = loc_tst_Patient.ReadTheOne(m_NNBase, where, ref myDBReadCommand, ref myPatient);
			if (bOK)
			{
				int ploc_test_patient = loc_tst_Patient.First();
				if (ploc_test_patient >= 0)
				{
					if (bNew)
					{
						m_DBPatient.Copy(myPatient);
					}
					else
					{
						m_PrevIDs_DBPatient.Copy(myPatient);
					}
					if (bNew)
					{
						if (m_facil_num.Length == 0)
						{
							m_facil_num = myPatient.m_facilnum;
							bFacilGiven = m_facil_num.Length > 0;
							if (bFacilGiven)
							{
								LookupFacilName();
								myPatientTrackingRec.m_facilname = m_facility;
								myPatientTrackingRec.m_facilalias = m_facility;
								myPatientTrackingRec.m_facilnum = m_facil_num;
							}
						}
					}
					else if (m_prev_facil_num.Length == 0)
					{
						m_prev_facil_num = myPatient.m_facilnum;
						bPrevFacilGiven = m_prev_facil_num.Length > 0;
						if (bPrevFacilGiven)
						{
							LookupPrevFacilName();
							myPatientTrackingRec.m_PrevFacilName = m_PrevFacility;
							myPatientTrackingRec.m_PrevFacilAlias = m_PrevFacility;
							myPatientTrackingRec.m_PrevFacilNum = m_prev_facil_num;
						}
					}
					iAccCount = (bNew ? (iAccountCount = myPatient.m_PatientAccountList.GetNumUsedElements()) : (iPrevAccountCount = myPatient.m_PatientAccountList.GetNumUsedElements()));
					if (iAccCount == 1)
					{
						int ploc_test_account = myPatient.m_PatientAccountList.First();
						if (ploc_test_account >= 0)
						{
							PatientAccountRec loc_test_account = (PatientAccountRec)myPatient.m_PatientAccountList.m_Array[ploc_test_account];
							if (bNew)
							{
								m_PatientAccountRec.Copy(loc_test_account);
								m_newPatientAccountRec.Copy(loc_test_account);
							}
							else
							{
								m_PrevIDs_PatientAccountRec.Copy(loc_test_account);
							}
							iVisCount = (bNew ? (iVisitCount = loc_test_account.m_PatientVisitList.GetNumUsedElements()) : (iPrevVisitCount = loc_test_account.m_PatientVisitList.GetNumUsedElements()));
							if (iVisCount == 1)
							{
								int ploc_test_visit = loc_test_account.m_PatientVisitList.First();
								if (ploc_test_visit >= 0)
								{
									loc_test_visit = (PatientVisitRec)loc_test_account.m_PatientVisitList.m_Array[ploc_test_visit];
									bLatestVisitFound = true;
									if (bNew)
									{
										m_PatientVisitRec.Copy(loc_test_visit);
										m_newPatientVisitRec.Copy(loc_test_visit);
									}
									else
									{
										m_PrevIDs_PatientVisitRec.Copy(loc_test_visit);
									}
								}
								else
								{
									iStatVisit = Convert.ToInt32(errortypes.NoMatch);
								}
							}
							else if (iVisCount > 1)
							{
								iStatVisit = Convert.ToInt32(errortypes.MoreThanOneMatch);
							}
							else if (iVisCount < 1)
							{
								iStatVisit = Convert.ToInt32(errortypes.NoMatch);
							}
						}
						else
						{
							iStatVisit = (iStatAccount = Convert.ToInt32(errortypes.NoMatch));
						}
					}
					else if (iAccCount > 1)
					{
						iStatAccount = Convert.ToInt32(errortypes.MoreThanOneMatch);
					}
					else if (iAccCount < 1)
					{
						iStatVisit = (iStatAccount = Convert.ToInt32(errortypes.NoMatch));
					}
					if (iAccCount > 1 || iVisCount > 1)
					{
						if (bNew)
						{
							bOK = m_DBPatient.LatestNonFutureActiveOrCurrentPatientVisitRec(m_DBPatient, ref loc_test_visit);
							if (bOK)
							{
								m_PatientVisitRec.Copy(loc_test_visit);
								m_newPatientVisitRec.Copy(loc_test_visit);
								bLatestVisitFound = true;
							}
						}
						else
						{
							m_PrevIDs_DBPatient.LatestNonFutureActiveOrCurrentPatientVisitRec(m_PrevIDs_DBPatient, ref loc_test_visit);
							if (bOK)
							{
								m_PrevIDs_PatientVisitRec.Copy(loc_test_visit);
								bLatestVisitFound = true;
							}
						}
					}
					else if (iAccCount < 1)
					{
						iStatVisit = (iStatAccount = Convert.ToInt32(errortypes.NoMatch));
					}
					else if (iVisCount < 1)
					{
						iStatVisit = Convert.ToInt32(errortypes.NoMatch);
					}
					if (bOK && bLatestVisitFound)
					{
						if (bNew)
						{
							VisitNumFound = loc_test_visit.m_visitnum;
							myPatientTrackingRec.m_visituuid = (VisitUUID = loc_test_visit.m_visit_UUID);
							PatientClass = loc_test_visit.m_patientclass;
							PatientType = loc_test_visit.m_patienttype;
							AdmitTime = loc_test_visit.m_admittime;
							if (AdmitTime.Year <= 1800)
							{
								AdmitDateTime = "";
							}
							else
							{
								AdmitDateTime = AdmitTime.ToString("yyyyMMddHHmmss");
							}
							AttendingPhysician = loc_test_visit.m_AttendPhysician;
							ReportingPhysician = loc_test_visit.m_ReportPhysician;
							ConsultingPhysician = loc_test_visit.m_ConsultPhysician;
							myPatientTrackingRec.m_dischargetime = (DischargeTime = loc_test_visit.m_dischargetime);
							bDischargeFacilityTime = false;
							if (DischargeTime.Year <= 1800 || DischargeTime.Year == 2037)
							{
								DischargeDateTime = "20371231000000";
							}
							else
							{
								DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
							}
							m_loc_num = loc_test_visit.m_locnum;
							myPatientTrackingRec.m_locname = m_LocationList.LookupLocation(m_loc_num);
							Room = loc_test_visit.m_roomnum;
							Bed = loc_test_visit.m_bednum;
							Weight_DML_value = loc_test_visit.m_weight;
							Weight_DML_units = loc_test_visit.m_weight_units;
							Height_DML_value = loc_test_visit.m_height;
							Height_DML_units = loc_test_visit.m_height_units;
							Diagnosis = loc_test_visit.m_diagnosis;
						}
						else
						{
							PreviousVisitNumFound = loc_test_visit.m_visitnum;
							if (VisitNumFound.Length == 0)
							{
								VisitNumFound = PreviousVisitNumFound;
							}
							myPatientTrackingRec.m_PrevVisitUUID = (PreviousVisitUUID = loc_test_visit.m_visit_UUID);
							if (VisitUUID.Length == 0)
							{
								myPatientTrackingRec.m_visituuid = (VisitUUID = PreviousVisitUUID);
							}
							if (PatientClass.Length == 0)
							{
								PatientClass = loc_test_visit.m_patientclass;
							}
							if (PatientType.Length == 0)
							{
								PatientType = loc_test_visit.m_patienttype;
							}
							if (AdmitTime.Year <= 1800)
							{
								AdmitTime = loc_test_visit.m_admittime;
								if (AdmitTime.Year <= 1800)
								{
									AdmitDateTime = "";
								}
								else
								{
									AdmitDateTime = AdmitTime.ToString("yyyyMMddHHmmss");
								}
							}
							if (AttendingPhysician.Length == 0)
							{
								AttendingPhysician = loc_test_visit.m_AttendPhysician;
							}
							if (ReportingPhysician.Length == 0)
							{
								ReportingPhysician = loc_test_visit.m_ReportPhysician;
							}
							if (ConsultingPhysician.Length == 0)
							{
								ConsultingPhysician = loc_test_visit.m_ConsultPhysician;
							}
							if (DischargeTime.Year <= 1800 || DischargeTime.Year == 2037)
							{
								myPatientTrackingRec.m_dischargetime = (DischargeTime = loc_test_visit.m_dischargetime);
								bDischargeFacilityTime = false;
								if (DischargeTime.Year <= 1800 || DischargeTime.Year == 2037)
								{
									DischargeDateTime = "20371231000000";
								}
								else
								{
									DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
								}
							}
							m_DBPatient.FillInPrevPatientTrackingLocationInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, loc_test_visit.m_locnum);
							if (m_loc_num.Length == 0)
							{
								m_loc_num = loc_test_visit.m_locnum;
								m_DBPatient.FillInNewPatientTrackingLocationInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, loc_test_visit.m_locnum);
							}
							if (Room.Length == 0)
							{
								Room = loc_test_visit.m_roomnum;
							}
							if (Bed.Length == 0)
							{
								Bed = loc_test_visit.m_bednum;
							}
							if (Weight_DML_value.Length == 0)
							{
								Weight_DML_value = loc_test_visit.m_weight;
							}
							if (Weight_DML_units.Length == 0)
							{
								Weight_DML_units = loc_test_visit.m_weight_units;
							}
							if (Height_DML_value.Length == 0)
							{
								Height_DML_value = loc_test_visit.m_height;
							}
							if (Height_DML_units.Length == 0)
							{
								Height_DML_units = loc_test_visit.m_height_units;
							}
							if (Diagnosis.Length == 0)
							{
								Diagnosis = loc_test_visit.m_diagnosis;
							}
						}
						bOK = true;
					}
				}
				else
				{
					iStatVisit = (iStatAccount = (iStatPatient = Convert.ToInt32(errortypes.NoMatch)));
				}
			}
			else if (loc_tst_Patient.m_Status.m_errortype == Convert.ToInt32(errortypes.NoMatch))
			{
				iStatVisit = (iStatAccount = (iStatPatient = Convert.ToInt32(errortypes.NoMatch)));
			}
			else if (loc_tst_Patient.m_Status.m_errortype == Convert.ToInt32(errortypes.MoreThanOneMatch))
			{
				iStatPatient = Convert.ToInt32(errortypes.MoreThanOneMatch);
			}
			if (m_loc_num != null && m_loc_num.Length > 0)
			{
				bOK = GetFacilityAndLocationByLocNum();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetPatientVisitInfo");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "searching for patient_visits record", "GetPatientVisitInfo", bMoveMessage: true);
			bOK = false;
		}
		catch (Exception e2)
		{
			handleException(e2, "searching for patient_visits record", "GetPatientVisitInfo", bMoveMessage: true);
			bOK = false;
		}
		return bOK;
	}

	private bool GetFacilityAndLocationByLocNum()
	{
		bool bOK = false;
		try
		{
			myDBReadCommand.CommandText = $"select loc_name, parent from DBA.inst_locations where loc_num = '{m_loc_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			m_location = "";
			m_facil_num = "";
			if (myDBReadReader.Read())
			{
				m_location = myDBReadReader.GetString(0);
				m_facil_num = myDBReadReader.GetString(1);
			}
			myDBReadReader.Close();
			if (m_facil_num.Length > 0)
			{
				bOK = LookupFacilName();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetFacilityAndLocationByLocNum - current location");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up location number", "GetFacilityAndLocationByLocNum - current location", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up location number", "GetFacilityAndLocationByLocNum - current location", bMoveMessage: true);
		}
		return bOK;
	}

	private bool LookupFacilName()
	{
		bool bOK = false;
		try
		{
			myDBReadCommand.CommandText = $"select loc_name from DBA.inst_locations where loc_num = '{m_facil_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			m_facility = "";
			if (myDBReadReader.Read())
			{
				m_facility = myDBReadReader.GetString(0);
				bOK = true;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("LookupFacilName - current facility");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up facility name", "LookupFacilName - current facility", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up facility name", "LookupFacilName - current facility", bMoveMessage: true);
		}
		return bOK;
	}

	private bool LookupPrevFacilName()
	{
		bool bOK = false;
		try
		{
			myDBReadCommand.CommandText = $"select loc_name from DBA.inst_locations where loc_num = '{m_prev_facil_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myDBReadReader = myDBReadCommand.ExecuteReader();
			m_PrevFacility = "";
			if (myDBReadReader.Read())
			{
				m_PrevFacility = myDBReadReader.GetString(0);
				bOK = true;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("LookupPrevFacilName - previous facility");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up previous facility name", "LookupPrevFacilName - previous facility", bMoveMessage: true);
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up previous facility name", "LookupPrevFacilName - previous facility", bMoveMessage: true);
		}
		return bOK;
	}

	private void ProcessADTMessage()
	{
		bChangesMade = false;
		bool bOK = true;
		if (!bNoVisitInfo)
		{
			m_loc_num = "";
			m_LocationList.LookupLocNum(m_location, m_facil_num, ref m_loc_num);
		}
		bInvalidLoc = m_loc_num.Length == 0;
		if (MessageSubType == "A13" || MessageSubType == "A22" || MessageSubType == "A52")
		{
			DischargeTime = MaxHL7DateTime;
			bDischargeFacilityTime = false;
			DischargeDateTime = "20371231000000";
		}
		bool bDischPat = false;
		iActiveHours = m_ActiveHours;
		if (sDischargeOPClassOrTypeByFacil.Length > 0)
		{
			string[] sFacilPatClassTypeArray = sDischargeOPClassOrTypeByFacil.Split('|');
			string[] array = sFacilPatClassTypeArray;
			foreach (string sPatClassTypeAndHours in array)
			{
				string[] sPatClassTypeHoursArray = sPatClassTypeAndHours.Split('^');
				string fac_uuid = sPatClassTypeHoursArray[0];
				string ClassType = sPatClassTypeHoursArray[1];
				string ADT_ID = sPatClassTypeHoursArray[2];
				string sActiveHours = sPatClassTypeHoursArray[3];
				if (Comp.Compare(fac_uuid, m_facil_num, CompOpt) != 0)
				{
					continue;
				}
				if (ClassType == "C")
				{
					if (Comp.Compare(ADT_ID, PatientClass, CompOpt) == 0)
					{
						bDischPat = true;
						if (isNumeric(sActiveHours, NumberStyles.Integer))
						{
							iActiveHours = Convert.ToInt32(sActiveHours);
						}
					}
				}
				else if (Comp.Compare(ADT_ID, PatientType, CompOpt) == 0)
				{
					bDischPat = true;
					if (isNumeric(sActiveHours, NumberStyles.Integer))
					{
						iActiveHours = Convert.ToInt32(sActiveHours);
					}
				}
			}
		}
		if (sDischargeOPClassOrTypeByLoc.Length > 0)
		{
			string[] sLocPatClassTypeArray = sDischargeOPClassOrTypeByLoc.Split('|');
			string[] array2 = sLocPatClassTypeArray;
			foreach (string sPatClassTypeAndHours2 in array2)
			{
				string[] sPatClassTypeHoursArray2 = sPatClassTypeAndHours2.Split('^');
				string loc_uuid = sPatClassTypeHoursArray2[0];
				string ClassType2 = sPatClassTypeHoursArray2[1];
				string ADT_ID2 = sPatClassTypeHoursArray2[2];
				string sActiveHours2 = sPatClassTypeHoursArray2[3];
				if (Comp.Compare(loc_uuid, m_loc_num, CompOpt) != 0)
				{
					continue;
				}
				if (ClassType2 == "C")
				{
					if (Comp.Compare(ADT_ID2, PatientClass, CompOpt) == 0)
					{
						bDischPat = true;
						if (isNumeric(sActiveHours2, NumberStyles.Integer))
						{
							iActiveHours = Convert.ToInt32(sActiveHours2);
						}
					}
				}
				else if (Comp.Compare(ADT_ID2, PatientType, CompOpt) == 0)
				{
					bDischPat = true;
					if (isNumeric(sActiveHours2, NumberStyles.Integer))
					{
						iActiveHours = Convert.ToInt32(sActiveHours2);
					}
				}
			}
		}
		if (sDischargeOPClassOrTypeByFacil.Length == 0 && sDischargeOPClassOrTypeByLoc.Length == 0)
		{
			if (sDischargeOutPatientClasses.Length > 0)
			{
				string[] sPatClassArray = sDischargeOutPatientClasses.Split(',');
				string[] array3 = sPatClassArray;
				foreach (string sPatClassAndHours in array3)
				{
					string[] sPatClassHoursArray = sPatClassAndHours.Split('^');
					string sPatClass = sPatClassHoursArray[0];
					string sActiveHours3 = "";
					if (sPatClassHoursArray.Length > 1)
					{
						sActiveHours3 = sPatClassHoursArray[1];
					}
					if (PatientClass == sPatClass)
					{
						bDischPat = true;
						if (sActiveHours3.Length > 0 && isNumeric(sActiveHours3, NumberStyles.Integer))
						{
							iActiveHours = Convert.ToInt32(sActiveHours3);
						}
					}
				}
			}
			if (sDischargeOutPatientTypes.Length > 0)
			{
				string[] sPatTypeArray = sDischargeOutPatientTypes.Split(',');
				string[] array4 = sPatTypeArray;
				foreach (string sPatTypeAndHours in array4)
				{
					string[] sPatTypeHoursArray = sPatTypeAndHours.Split('^');
					string sPatType = sPatTypeHoursArray[0];
					string sActiveHours4 = "";
					if (sPatTypeHoursArray.Length > 1)
					{
						sActiveHours4 = sPatTypeHoursArray[1];
					}
					if (PatientType == sPatType)
					{
						bDischPat = true;
						if (sActiveHours4.Length > 0 && isNumeric(sActiveHours4, NumberStyles.Integer))
						{
							iActiveHours = Convert.ToInt32(sActiveHours4);
						}
					}
				}
			}
		}
		if (MessageSubType == "A03" || MessageSubType == "A07" || MessageSubType == "A11" || MessageSubType == "A21" || MessageSubType == "A23" || MessageSubType == "A27" || MessageSubType == "A38" || MessageSubType == "A53")
		{
			bDischPat = true;
		}
		if ((DischargeDateTime.Length == 0 || DischargeDateTime == "20371231000000") && (bDischPat || (bInvalidLoc && !bSpansFacilities)))
		{
			myPatientTrackingRec.m_dischargetime = (DischargeTime = DateTime.Now.AddHours(iActiveHours));
			bDischargeFacilityTime = false;
			DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
		}
		else if (DischargeDateTime.Length > 0 && DischargeDateTime != "20371231000000")
		{
			myPatientTrackingRec.m_dischargetime = DischargeTime;
			bDischargeFacilityTime = false;
			DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
		}
		m_newDBPatient.Clear();
		m_newPatientAccountRec.Clear();
		m_newPatientVisitRec.Clear();
		m_newDBPatient.m_PatientID = PatientID;
		m_newDBPatient.m_LastName = LastName;
		m_newDBPatient.m_FirstName = FirstName;
		m_newDBPatient.m_MiddleName = MiddleName;
		m_newDBPatient.m_Sex = Sex;
		m_newDBPatient.m_birthdate = YMDhms_To_DateTime(ref BirthDate, "BirthDate", bAllowMaxHL7Year: false, bAllowFuture: false);
		if (m_loc_num.Length > 0 && !bInvalidLoc)
		{
			m_newPatientVisitRec.m_locnum = m_loc_num;
			m_newPatientVisitRec.m_roomnum = Room;
			m_newPatientVisitRec.m_bednum = Bed;
		}
		m_newDBPatient.m_prefix = Prefix;
		m_newDBPatient.m_suffix = Suffix;
		m_newDBPatient.m_race = Race_DML;
		if (DischargeTime.Year <= 1800)
		{
			myPatientTrackingRec.m_dischargetime = (m_newPatientVisitRec.m_dischargetime = (DischargeTime = MaxHL7DateTime));
		}
		else if (bDischargeFacilityTime)
		{
			myPatientTrackingRec.m_dischargetime = (m_newPatientVisitRec.m_dischargetime = FacilityTimeToSystemTime(DischargeTime));
		}
		else
		{
			myPatientTrackingRec.m_dischargetime = (m_newPatientVisitRec.m_dischargetime = DischargeTime);
		}
		if (AdmitTime.Year > 1800)
		{
			m_newPatientVisitRec.m_admittime = FacilityTimeToSystemTime(AdmitTime);
		}
		else if (bUpdateVisit && m_PatientVisitRec != null && m_PatientVisitRec.m_admittime.Year > 1800)
		{
			m_newPatientVisitRec.m_admittime = m_PatientVisitRec.m_admittime;
		}
		else
		{
			m_newPatientVisitRec.m_admittime = DateTime.Now;
		}
		m_newPatientVisitRec.m_patientclass = PatientClass;
		m_newPatientVisitRec.m_patienttype = PatientType;
		m_newPatientVisitRec.m_visitnum = VisitNumToAdd;
		m_newPatientVisitRec.m_AttendPhysician = AttendingPhysician;
		m_newPatientVisitRec.m_ReportPhysician = ReportingPhysician;
		m_newPatientVisitRec.m_ConsultPhysician = ConsultingPhysician;
		m_newPatientVisitRec.m_weight = Weight_DML_value;
		m_newPatientVisitRec.m_weight_units = Weight_DML_units;
		m_newPatientVisitRec.m_height = Height_DML_value;
		m_newPatientVisitRec.m_height_units = Height_DML_units;
		m_newPatientVisitRec.m_diagnosis = Diagnosis;
		m_newDBPatient.m_medrecnum = MedicalRecordNumber;
		m_newPatientAccountRec.m_accountnum = AccountNumber;
		m_newDBPatient.m_facilnum = m_facil_num;
		if (bPatientExists)
		{
			m_newDBPatient.m_patientnum = m_DBPatient.m_patientnum;
			m_newPatientAccountRec.m_patientnum = m_DBPatient.m_patientnum;
			m_newPatientVisitRec.m_patientnum = m_DBPatient.m_patientnum;
			m_newDBPatient.m_PatientAccountList.Copy(m_DBPatient.m_PatientAccountList);
		}
		if (bAccountExists)
		{
			m_newPatientAccountRec.m_account_UUID = m_PatientAccountRec.m_account_UUID;
			m_newPatientVisitRec.m_account_UUID = m_PatientAccountRec.m_account_UUID;
		}
		m_newPatientAccountRec.m_PatientRec = m_newDBPatient;
		m_newPatientVisitRec.m_PatientRec = m_newDBPatient;
		m_newPatientVisitRec.m_PatientAccountRec = m_newPatientAccountRec;
		if ((bMRNOrPatIDChange && !bPrevAcctGiven && !bAccountGiven) || bAccountGiven)
		{
			if (m_NNBase.BeginTransaction(ref myDBWriteConnection, ref myDBWriteCommand, ref myTransaction, "Runtime DB"))
			{
				m_DBPatient.ClearAffectedLocationList();
				bOK = AddOrUpdateAndMergePatient();
				if (bOK)
				{
					if ((bAddAccount && !bAccountAdded) || bUpdateAccount || bMergeAccount || bMoveAccount)
					{
						bOK = AddOrUpdateAndMergePatientAccount();
					}
					if (bOK && ((bAddVisit && !bVisitAdded) || bUpdateVisit || bMergeVisit || bMoveVisit))
					{
						bOK = AddOrUpdateAndMergePatientVisit();
					}
					if (bOK)
					{
						if (bOK && (bVisitAdded || bVisitUpdated || bVisitMoved || bAccountAdded || bAccountMoved))
						{
							string where = "p.Patient_uuid = '" + m_newDBPatient.m_patientnum + "'";
							m_PatientList.PurgeList();
							bOK = m_PatientList.ReadTheOne(m_NNBase, where, ref myDBReadCommand, ref m_newDBPatient);
							if (bOK)
							{
								bOK = m_newDBPatient.CleanupPatient(m_NNBase, bNew: true, bPatientExists, m_LocationList, m_FacilityList, ref myDBReadCommand, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
							}
						}
						if (bOK && bPrevPatientExists && m_PrevIDs_DBPatient != null && !bPrevPatientDeleted && Comp.Compare(m_newDBPatient.m_patientnum, m_PrevIDs_DBPatient.m_patientnum, CompOpt) != 0 && (bPrevVisitDeleted || bVisitMoved || bPrevAccountDeleted || bAccountMoved))
						{
							string where2 = "p.patient_uuid = '" + m_PrevIDs_DBPatient.m_patientnum + "'";
							m_PrevIDs_PatientList.PurgeList();
							bOK = m_PrevIDs_PatientList.ReadTheOne(m_NNBase, where2, ref myDBReadCommand, ref m_PrevIDs_DBPatient);
							if (bOK)
							{
								bOK = m_PrevIDs_DBPatient.CleanupPatient(m_NNBase, bNew: false, bmyPatientExists: true, m_LocationList, m_FacilityList, ref myDBReadCommand, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
							}
						}
						if (bOK && bChangesMade)
						{
							bOK = m_DBPatient.PropogatePatientUpdate(m_NNBase, ref myDBWriteCommand);
							if (bOK)
							{
								m_NNBase.Commit(ref myTransaction, "Runtime DB");
							}
						}
					}
				}
				if (!bOK || !bChangesMade)
				{
					m_NNBase.Rollback(ref myTransaction, "Runtime DB");
					myPatientTrackingRec.Rollback(m_NNBase);
				}
			}
		}
		else
		{
			m_NNBase.ReportErrorDB(AppErrorMsg = "Missing account number. Message rejected.", "E", "processing ADT message", "ProcessADTMessage", "No account number was given and the MRN and PatID are not changing, so no action can be performed.");
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Missing account number", bError: true, ref myPTDBWriteCommand);
		}
		if (!bChangesMade)
		{
			AppWarningMsg = "No changes made";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("No changes made", isXml: false, "ProcessADTMessage");
			}
		}
	}

	private bool AddOrUpdateAndMergePatient()
	{
		bool bOK = false;
		if (bUpdatePatient)
		{
			DBPatient myOldPatient = null;
			DBPatient myNewPatient = new DBPatient(m_b_loc_last_update_inst_class_column, m_b_loc_last_update_inst_type_column);
			myNewPatient.Copy(m_newDBPatient);
			if (bPatientIDExists || bMRNExists)
			{
				myOldPatient = m_DBPatient;
			}
			else
			{
				if (!bPrevPatientIDExists && !bPrevMRNExists)
				{
					bOK = false;
					m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
					m_NNBase.ReportErrorDB(AppErrorMsg = "Internal error checking patients", "C", "checking for existing patients records", "AddOrUpdateAndMergePatient", "We cannot determine which existing patients record to update.");
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Can't update patient", bError: true, ref myPTDBWriteCommand);
					return bOK;
				}
				myOldPatient = m_PrevIDs_DBPatient;
			}
			myNewPatient.m_PatientAccountList.Copy(myOldPatient.m_PatientAccountList);
			m_DBPatient.FillInPatientPatientTrackingInfo(ref myPatientTrackingRec, myNewPatient, myOldPatient);
			bPatientChanged = m_DBPatient.PatientChange(myNewPatient, myOldPatient);
			if (bPatientChanged)
			{
				string where = " patient_uuid = '" + myOldPatient.m_patientnum + "'";
				bOK = m_newDBPatient.UpdatePatient(m_NNBase, where, ref myDBWriteCommand, myOldPatient.m_PatientID, myOldPatient.m_medrecnum);
				if (bOK)
				{
					bChangesMade = true;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Patient updated", isXml: false, "AddOrUpdateAndMergePatient");
					}
					m_DBPatient.UpdateAffectedLocationList(myOldPatient);
					m_newPatientAccountRec.m_patientnum = myOldPatient.m_patientnum;
					m_newPatientVisitRec.m_patientnum = myOldPatient.m_patientnum;
				}
				else if (m_newDBPatient.m_errortype == 11)
				{
					handleDBException(m_newDBPatient.m_SA_e, "updating patient", "AddOrUpdateAndMergePatient", bMoveMessage: true);
				}
				else if (m_newDBPatient.m_errortype == 12)
				{
					handleException(m_newDBPatient.m_e, "updating patient", "AddOrUpdateAndMergePatient", bMoveMessage: true);
				}
				myNewPatient.CreatePatientTrackingRecords(m_NNBase, ref myDBReadCommand, ref myPTDBWriteCommand, ref myPatientTrackingRec, "Update patient - new IDs", bPrev: false, !bOK);
				myOldPatient.CreatePatientTrackingRecords(m_NNBase, ref myDBReadCommand, ref myPTDBWriteCommand, ref myPatientTrackingRec, "Update patient - previous IDs", bPrev: true, !bOK);
			}
			else
			{
				bOK = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("No changes made to patient", isXml: false, "AddOrUpdateAndMergePatient");
				}
			}
		}
		else if (bAddPatient)
		{
			m_DBPatient.FillInPatientPatientTrackingInfo(ref myPatientTrackingRec, m_newDBPatient, null);
			if (MessageSubType == "A07")
			{
				myPatientTrackingRec.m_dischargetime = (DischargeTime = DateTime.Now.AddHours(iActiveHours));
				bDischargeFacilityTime = false;
				DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
				m_newPatientVisitRec.m_dischargetime = DischargeTime;
			}
			m_DBPatient.FillInPatientPatientTrackingInfo(ref myPatientTrackingRec, m_newDBPatient, null);
			bOK = m_newDBPatient.CreatePatient(m_NNBase, ref myDBWriteCommand);
			if (!bOK)
			{
				if (m_newDBPatient.m_errortype == 11)
				{
					handleDBException(m_newDBPatient.m_SA_e, "creating patient", "AddOrUpdateAndMergePatient", bMoveMessage: true);
				}
				else if (m_newDBPatient.m_errortype == 12)
				{
					handleException(m_newDBPatient.m_e, "creating patient", "AddOrUpdateAndMergePatient", bMoveMessage: true);
				}
			}
			if (m_new_visit_UUID.Length == 0)
			{
				m_new_visit_UUID = Guid.NewGuid().ToString("N");
			}
			myPatientTrackingRec.m_visituuid = m_new_visit_UUID;
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Create Patient", !bOK, ref myPTDBWriteCommand);
			if (bOK)
			{
				bChangesMade = true;
				bPatientAdded = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Patients record created", isXml: false, "AddOrUpdateAndMergePatient");
				}
				m_newPatientAccountRec.m_patientnum = m_newDBPatient.m_patientnum;
				m_newPatientVisitRec.m_patientnum = m_newDBPatient.m_patientnum;
				if (bAddAccount)
				{
					m_DBPatient.FillInAccountPatientTrackingInfo(ref myPatientTrackingRec, m_newPatientAccountRec, null);
					bOK = m_newPatientAccountRec.CreatePatientAccount(m_NNBase, ref myDBWriteCommand);
					if (!bOK)
					{
						if (m_newPatientAccountRec.m_errortype == 11)
						{
							handleDBException(m_newPatientAccountRec.m_SA_e, "creating patient account", "AddOrUpdateAndMergePatient", bMoveMessage: true);
						}
						else if (m_newDBPatient.m_errortype == 12)
						{
							handleException(m_newPatientAccountRec.m_e, "creating patient account", "AddOrUpdateAndMergePatient", bMoveMessage: true);
						}
					}
					if (m_new_visit_UUID.Length == 0)
					{
						m_new_visit_UUID = Guid.NewGuid().ToString("N");
					}
					myPatientTrackingRec.m_visituuid = m_new_visit_UUID;
					myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Create Patient Account", !bOK, ref myPTDBWriteCommand);
					if (bOK)
					{
						bAddAccount = false;
						bAccountAdded = true;
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Patient_Accounts record created", isXml: false, "AddOrUpdateAndMergePatient");
						}
					}
				}
			}
			if (bOK)
			{
				m_newPatientVisitRec.m_account_UUID = m_newPatientAccountRec.m_account_UUID;
				if (bAddVisit)
				{
					if (m_new_visit_UUID.Length > 0)
					{
						m_newPatientVisitRec.m_visit_UUID = m_new_visit_UUID;
					}
					bOK = CreatePatientVisit();
					if (bOK)
					{
						bAddVisit = false;
					}
				}
			}
		}
		else
		{
			bOK = true;
		}
		if (bOK && bPatIDChanging && bPrevPatientIDExists && (bPatientIDExists || bPatientAdded) && bMRNChanging && bPrevMRNExists && (bMRNExists || bPatientAdded) && bMergePatient)
		{
			bOK = m_PrevIDs_DBPatient.MergePatientByPatientIDAndMedRecNum(m_NNBase, m_newDBPatient, PreviousPatientID, PreviousMedicalRecordNumber, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
			if (bOK && bChangesMade)
			{
				bPrevPatientDeleted = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Patient merged by patient ID and MRN", isXml: false, "AddOrUpdateAndMergePatient");
				}
				m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
				m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
				m_PrevIDs_DBPatient.m_PatientID = PatientID;
				m_PrevIDs_DBPatient.m_medrecnum = MedicalRecordNumber;
				m_PrevIDs_DBPatient.m_patientnum = m_newDBPatient.m_patientnum;
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 11)
			{
				handleDBException(m_PrevIDs_DBPatient.m_SA_e, "merging patient by patient ID and MRN", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 12)
			{
				handleException(m_PrevIDs_DBPatient.m_e, "merging patient by patient ID and MRN", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
		}
		else if (bOK && bPatIDChanging && bPrevPatientIDExists && (bPatientIDExists || bPatientAdded) && bMergePatient)
		{
			bOK = m_PrevIDs_DBPatient.MergePatientByPatientID(m_NNBase, m_newDBPatient, PreviousPatientID, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
			if (bOK && bChangesMade)
			{
				bPrevPatientDeleted = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Patient merged by patient ID", isXml: false, "AddOrUpdateAndMergePatient");
				}
				m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
				m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
				m_PrevIDs_DBPatient.m_PatientID = PatientID;
				m_PrevIDs_DBPatient.m_patientnum = m_newDBPatient.m_patientnum;
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 11)
			{
				handleDBException(m_PrevIDs_DBPatient.m_SA_e, "merging patient by patient ID", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 12)
			{
				handleException(m_PrevIDs_DBPatient.m_e, "merging patient by patient ID", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
		}
		else if (bOK && (bMRNChanging || bCrossFacilityMergePatient) && bPrevMRNExists && (bMRNExists || bPatientAdded) && bMergePatient)
		{
			bOK = m_PrevIDs_DBPatient.MergePatientByMedRecNum(m_NNBase, m_newDBPatient, PreviousMedicalRecordNumber, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
			if (bOK && bChangesMade)
			{
				bPrevPatientDeleted = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Patient merged by MRN", isXml: false, "AddOrUpdateAndMergePatient");
				}
				m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
				m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
				m_PrevIDs_DBPatient.m_medrecnum = MedicalRecordNumber;
				m_PrevIDs_DBPatient.m_patientnum = m_newDBPatient.m_patientnum;
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 11)
			{
				handleDBException(m_PrevIDs_DBPatient.m_SA_e, "merging patient by MRN", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
			else if (m_PrevIDs_DBPatient.m_errortype == 12)
			{
				handleException(m_PrevIDs_DBPatient.m_e, "merging patient by MRN", "AddOrUpdateAndMergePatient", bMoveMessage: true);
			}
		}
		return bOK;
	}

	private DateTime FacilityTimeToSystemTime(DateTime FacilityLocalTime)
	{
		DateTime newDateTime = FacilityLocalTime;
		if (m_facil_num.Length > 0)
		{
			try
			{
				newDateTime = TimeZoneInfo.ConvertTime(FacilityLocalTime, m_TimeZoneInfo, TimeZoneInfo.Local);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("FacilityTimeToSystemTime");
			}
			catch (Exception)
			{
				newDateTime = FacilityLocalTime;
			}
		}
		return newDateTime;
	}

	private bool AddOrUpdateAndMergePatientAccount()
	{
		bool bOK = true;
		bool bAccountChanged = false;
		if (m_newPatientAccountRec.m_accountnum != null && m_newPatientAccountRec.m_accountnum.Length > 0)
		{
			if (bAccountExists && (bPatientExists || bPatientAdded || bPatientChanged))
			{
				PatientAccountRec myOldAccount = null;
				PatientAccountRec myNewAccount = new PatientAccountRec();
				myNewAccount.Copy(m_newPatientAccountRec);
				if (m_PatientAccountRec.m_accountnum.Length > 0)
				{
					myOldAccount = m_PatientAccountRec;
				}
				else
				{
					if (m_PrevIDs_PatientAccountRec.m_accountnum.Length <= 0)
					{
						bOK = false;
						m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
						m_NNBase.ReportErrorDB(AppErrorMsg = "Internal error checking accounts", "C", "checking for existing accounts records", "AddOrUpdateAndMergePatientAccount", "We cannot determine which existing patient_accounts record to update.");
						myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Can't update account", bError: true, ref myPTDBWriteCommand);
						return bOK;
					}
					myOldAccount = m_PrevIDs_PatientAccountRec;
				}
				myNewAccount.m_PatientVisitList.Copy(myOldAccount.m_PatientVisitList);
				if (m_DBPatient.PatientAccountChange(myNewAccount, myOldAccount))
				{
					string where = " account_uuid = '" + m_newPatientAccountRec.m_account_UUID + "'";
					m_DBPatient.FillInAccountPatientTrackingInfo(ref myPatientTrackingRec, myNewAccount, myOldAccount);
					bOK = m_newPatientAccountRec.UpdatePatientAccount(m_NNBase, where, ref myDBWriteCommand, myOldAccount.m_accountnum, myOldAccount.m_patientnum);
					if (!bOK)
					{
						if (m_newPatientAccountRec.m_errortype == 11)
						{
							handleDBException(m_newPatientAccountRec.m_SA_e, "updating patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
						}
						else if (m_newPatientAccountRec.m_errortype == 12)
						{
							handleException(m_newPatientAccountRec.m_e, "updating patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
						}
					}
					m_newDBPatient.CreatePatientTrackingRecords(m_NNBase, ref myDBReadCommand, ref myPTDBWriteCommand, ref myPatientTrackingRec, myNewAccount, "Update account - new IDs", bPrev: false, !bOK);
					m_DBPatient.CreatePatientTrackingRecords(m_NNBase, ref myDBReadCommand, ref myPTDBWriteCommand, ref myPatientTrackingRec, myOldAccount, "Update account - previous IDs", bPrev: true, !bOK);
					if (bOK)
					{
						bChangesMade = true;
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Patient account updated", isXml: false, "AddOrUpdateAndMergePatientAccount");
						}
						m_DBPatient.UpdateAffectedLocationList(myOldAccount);
					}
				}
				else if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("No changes made to patient account", isXml: false, "AddOrUpdateAndMergePatientAccount");
				}
			}
			else if (!bAccountExists)
			{
				m_DBPatient.FillInAccountPatientTrackingInfo(ref myPatientTrackingRec, m_newPatientAccountRec, null);
				bOK = m_newPatientAccountRec.CreatePatientAccount(m_NNBase, ref myDBWriteCommand);
				if (bOK)
				{
					bChangesMade = true;
					bAccountAdded = true;
					m_newPatientVisitRec.m_account_UUID = m_newPatientAccountRec.m_account_UUID;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Patient account created", isXml: false, "AddOrUpdateAndMergePatientAccount");
					}
				}
				else if (m_newPatientAccountRec.m_errortype == 11)
				{
					handleDBException(m_newPatientAccountRec.m_SA_e, "creating patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
				else if (m_newPatientAccountRec.m_errortype == 12)
				{
					handleException(m_newPatientAccountRec.m_e, "creating patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
				if (m_new_visit_UUID.Length == 0)
				{
					m_new_visit_UUID = Guid.NewGuid().ToString("N");
				}
				myPatientTrackingRec.m_visituuid = m_new_visit_UUID;
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Create Patient Account", !bOK, ref myPTDBWriteCommand);
			}
		}
		if (bOK)
		{
			if ((bAcctChanging || bCrossFacilityMergeSameAccount) && bPrevAccountExists && m_PrevIDs_PatientAccountRec.m_account_UUID.Length > 0 && (bAccountExists || bAccountAdded) && bMergeAccount)
			{
				bOK = m_PrevIDs_DBPatient.MergeAccount(m_NNBase, m_newDBPatient, m_newPatientAccountRec, PreviousPatientAccount, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
				if (bOK && bChangesMade)
				{
					bPrevAccountDeleted = true;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Patient account merged", isXml: false, "AddOrUpdateAndMergePatientVisit");
					}
					m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
					m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
					bOK = m_PrevIDs_DBPatient.UpdateAccountNum(PreviousPatientAccount, AccountNumber);
				}
				else if (m_PrevIDs_DBPatient.m_errortype == 11)
				{
					handleDBException(m_PrevIDs_DBPatient.m_SA_e, "merging patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
				else if (m_PrevIDs_DBPatient.m_errortype == 12)
				{
					handleException(m_PrevIDs_DBPatient.m_e, "merging patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
			}
			else if (bPrevAccountExists && m_PrevIDs_PatientAccountRec.m_account_UUID.Length > 0 && (bAccountExists || bAccountAdded) && bPrevMRNExists && bMRNExists && (bMRNDifferent || bCrossFacilityMoveAccount) && bMoveAccount)
			{
				bOK = m_PrevIDs_DBPatient.MoveAccount(m_NNBase, m_newDBPatient, m_PrevIDs_PatientAccountRec, m_newPatientAccountRec, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand, ref bChangesMade);
				if (bOK && bChangesMade)
				{
					bAccountMoved = true;
					m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
					m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
				}
				else if (m_PrevIDs_DBPatient.m_errortype == 11)
				{
					handleDBException(m_PrevIDs_DBPatient.m_SA_e, "moving patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
				else if (m_PrevIDs_DBPatient.m_errortype == 12)
				{
					handleException(m_PrevIDs_DBPatient.m_e, "moving patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
				}
			}
			else if (bPrevAccountExists && m_PrevIDs_PatientAccountRec.m_account_UUID.Length > 0 && (bAccountExists || bAccountAdded) && (bAcctChanging || bCrossFacilityTransferSameAccount) && bDeactAccount)
			{
				bOK = m_PrevIDs_DBPatient.DeleteAccount(m_NNBase, PreviousPatientAccount, ref myDBWriteCommand);
				if (!bOK)
				{
					if (m_PrevIDs_DBPatient.m_errortype == 11)
					{
						handleDBException(m_PrevIDs_DBPatient.m_SA_e, "deleting patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
					}
					else if (m_PrevIDs_DBPatient.m_errortype == 12)
					{
						handleException(m_PrevIDs_DBPatient.m_e, "deleting patient account", "AddOrUpdateAndMergePatientAccount", bMoveMessage: true);
					}
				}
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Delete Patient Account", !bOK, ref myPTDBWriteCommand);
				if (bOK)
				{
					bChangesMade = true;
					bPrevAccountDeleted = true;
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Previous account deactivated", isXml: false, "AddOrUpdateAndMergePatientVisit");
					}
					m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
				}
			}
		}
		return bOK;
	}

	private bool AddOrUpdateAndMergePatientVisit()
	{
		string VisitWhere = "";
		new PatientRec();
		bool bOK = false;
		newlocnum = m_newPatientVisitRec.m_locnum;
		oldlocnum = "";
		if (!bAddVisit && bVisitExists && m_PatientVisitRec != null && !bMergeVisit && !bMoveVisit && !bMergeAccount && !bMoveAccount)
		{
			oldlocnum = m_PatientVisitRec.m_locnum;
			bVisitIsActive = m_PatientVisitRec.Get_m_bVisitIsActive();
			bVisitIsFuture = m_PatientVisitRec.Get_m_bVisitIsFuture();
			bLatestActiveVisit = m_PatientVisitRec.Get_m_bLatestActiveVisitForPatient(m_NNBase, ref myDBReadCommand);
			if (m_NNBase.m_isLogging)
			{
				m_DBPatient.DisplayVisitStatus(m_NNBase, "existing given", m_PatientVisitRec.m_visitnum, "existing given", bFutureTense: true, bVisitIsFuture, bVisitIsActive, bLatestActiveVisit);
			}
			bOK = m_PatientVisitRec.m_bOK;
		}
		else if (bAddVisit && !bMergeVisit && !bMoveVisit && !bMergeAccount && !bMoveAccount)
		{
			bVisitIsActive = m_newPatientVisitRec.Get_m_bVisitIsActive();
			bVisitIsFuture = m_newPatientVisitRec.Get_m_bVisitIsFuture();
			bLatestActiveVisit = m_newPatientVisitRec.Get_m_bLatestActiveVisitForPatient(m_NNBase, ref myDBReadCommand);
			if (m_NNBase.m_isLogging)
			{
				m_DBPatient.DisplayVisitStatus(m_NNBase, "new given", m_newPatientVisitRec.m_visitnum, "existing given", bFutureTense: true, bVisitIsFuture, bVisitIsActive, bLatestActiveVisit);
			}
			bOK = m_newPatientVisitRec.m_bOK;
		}
		else if (bVisitAdded)
		{
			bVisitIsActive = m_newPatientVisitRec.Get_m_bVisitIsActive();
			bLatestActiveVisit = bVisitIsActive;
			if (m_NNBase.m_isLogging)
			{
				m_DBPatient.DisplayVisitStatus(m_NNBase, "new given", m_newPatientVisitRec.m_visitnum, "new given", bFutureTense: false, bVisitIsFuture, bVisitIsActive, bLatestActiveVisit);
			}
			bOK = m_newPatientVisitRec.m_bOK;
		}
		else
		{
			bVisitIsActive = false;
			bLatestActiveVisit = false;
			bVisitIsFuture = false;
			bOK = true;
		}
		if (!bOK)
		{
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Visit status error", bError: true, ref myPTDBWriteCommand);
		}
		if (bOK)
		{
			if (bVisitExists && iVisitCount > 0 && m_PatientVisitRec != null && !bMoveVisit)
			{
				VisitWhere = " visit_uuid = '" + m_PatientVisitRec.m_visit_UUID + "'";
			}
			else if (bPrevVisitExists && iPrevVisitCount > 0 && m_PrevIDs_PatientVisitRec != null)
			{
				VisitWhere = " visit_uuid = '" + m_PrevIDs_PatientVisitRec.m_visit_UUID + "'";
			}
			else if (!bAddVisit && !bVisitAdded)
			{
				bOK = false;
				m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
				m_NNBase.ReportErrorDB(AppErrorMsg = "Internal error checking patient_visits", "C", "checking for patient_visits records", "AddOrUpdateAndMergePatientVisit", "We have neither an existing patient_visits record nor are we adding one.");
				myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Can't add/update visit", bError: true, ref myPTDBWriteCommand);
			}
		}
		if (bOK)
		{
			if (bVisitExists && iVisitCount > 0 && m_PatientVisitRec != null && m_PatientVisitRec.m_visit_UUID != null && m_PatientVisitRec.m_visit_UUID.Length > 0 && !bVisitNumChanging && ((!bMoveVisit && !bMergePatient && !bMergeAccount && !bMoveAccount) || (!bNoVisitInfo && bVisitNumToFindGiven)))
			{
				bOK = UpdatePatientVisitIfChanged(VisitWhere);
			}
			else if (m_newPatientVisitRec.m_visitnum != null && m_newPatientVisitRec.m_visitnum.Length > 0)
			{
				if (MessageSubType == "A07")
				{
					myPatientTrackingRec.m_dischargetime = (DischargeTime = DateTime.Now.AddHours(iActiveHours));
					bDischargeFacilityTime = false;
					DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
					m_newPatientVisitRec.m_dischargetime = DischargeTime;
				}
				if (bAddVisit || bVisitNumChanging || bMoveVisit)
				{
					if (!bAddVisit && bVisitExists && !bMoveVisit)
					{
						bOK = UpdatePatientVisitIfChanged(VisitWhere);
					}
					else if (bMoveVisit && bPrevVisitExists && iPrevVisitCount > 0 && m_PrevIDs_PatientVisitRec != null)
					{
						bChangesMade |= (bVisitMoved = (bOK = m_PrevIDs_DBPatient.MoveVisitToAccount(m_NNBase, m_PrevIDs_PatientAccountRec, m_newPatientAccountRec, m_PrevIDs_PatientVisitRec, ref myDBWriteCommand, myPatientTrackingRec, ref myPTDBWriteCommand)));
						if (bOK && bChangesMade)
						{
							m_DBPatient.UpdateAffectedLocationList(m_newDBPatient);
							m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_DBPatient);
						}
						else if (m_PrevIDs_DBPatient.m_errortype == 11)
						{
							handleDBException(m_PrevIDs_DBPatient.m_SA_e, "moving visit to account", "AddOrUpdateAndMergePatientVisit", bMoveMessage: true);
						}
						else if (m_PrevIDs_DBPatient.m_errortype == 12)
						{
							handleException(m_PrevIDs_DBPatient.m_e, "moving visit to account", "AddOrUpdateAndMergePatientVisit", bMoveMessage: true);
						}
					}
					else if (bAddVisit)
					{
						bOK = CreatePatientVisit();
					}
				}
				else if (bVisitExists && ((!bMoveVisit && !bMergePatient && !bMergeAccount && !bMoveAccount) || (!bNoVisitInfo && bVisitNumToFindGiven)))
				{
					bOK = UpdatePatientVisitIfChanged(VisitWhere);
				}
			}
		}
		if (bOK && bPrevVisitExists && (bVisitExists || bVisitAdded) && (bVisitNumChanging || bCrossFacilityMergeSameVisit || bCrossFacilityMoveSameVisit) && (bDeactVisit || bMergeVisit))
		{
			bOK = DeletePreviousPatientVisit();
		}
		if (bOK)
		{
			bLocChange = (oldlocnum.Length > 0) & (oldlocnum != newlocnum) & !bInvalidLoc;
		}
		if (bOK && bLocChange && bChangesMade)
		{
			m_DBPatient.UpdateAffectedLocationList(m_PatientVisitRec);
		}
		return bOK;
	}

	private bool UpdatePatientVisitIfChanged(string VisitWhere)
	{
		bool bOK = true;
		UpdateDischargeTime(ref m_newPatientVisitRec, m_PatientVisitRec, iActiveHours);
		bPatientVisitChanged = m_DBPatient.PatientVisitChange(m_newPatientVisitRec, m_PatientVisitRec, PreviousVisitNumFromADT, bVisitNumToFindGiven, bVisitNumToFindExact, iVisitCount);
		if (bPatientVisitChanged)
		{
			if (DateTime.Compare(m_newPatientVisitRec.m_dischargetime, m_PatientVisitRec.m_dischargetime) != 0 || DateTime.Compare(m_newPatientVisitRec.m_adddate, m_PatientVisitRec.m_adddate) != 0)
			{
				m_newPatientVisitRec.m_DischargeOrAddDateChanged = true;
			}
			m_newPatientVisitRec.m_visit_UUID = m_PatientVisitRec.m_visit_UUID;
			m_DBPatient.FillInVisitPatientTrackingInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, m_newPatientVisitRec, m_PatientVisitRec);
			bOK = m_newPatientVisitRec.UpdatePatientVisit(m_NNBase, VisitWhere, ref myDBWriteCommand);
			if (bOK)
			{
				bChangesMade = true;
				bVisitUpdated = true;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Patient visit updated", isXml: false, "UpdatePatientVisitIfChanged");
				}
				m_DBPatient.UpdateAffectedLocationList(m_newPatientVisitRec);
			}
			else if (m_newPatientVisitRec.m_errortype == 11)
			{
				handleDBException(m_newPatientVisitRec.m_SA_e, "updating patient visit", "UpdatePatientVisitIfChanged", bMoveMessage: true);
			}
			else if (m_newPatientVisitRec.m_errortype == 12)
			{
				handleException(m_newPatientVisitRec.m_e, "updating patient visit", "UpdatePatientVisitIfChanged", bMoveMessage: true);
			}
			myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Update Patient Visit", !bOK, ref myPTDBWriteCommand);
		}
		else if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("No changes made to patient visit", isXml: false, "UpdatePatientVisitIfChanged");
		}
		return bOK;
	}

	private bool CreatePatientVisit()
	{
		if (m_new_visit_UUID.Length == 0)
		{
			m_new_visit_UUID = Guid.NewGuid().ToString("N");
		}
		m_newPatientVisitRec.m_visit_UUID = m_new_visit_UUID;
		bool bOK = m_newPatientVisitRec.CreatePatientVisit(m_NNBase, ref myDBWriteCommand, m_new_visit_UUID);
		if (bOK)
		{
			bChangesMade = true;
			bVisitAdded = true;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Patient visit created", isXml: false, "HL7.CreatePatientVisit");
			}
			m_DBPatient.UpdateAffectedLocationList(m_newPatientVisitRec);
		}
		else if (m_newPatientVisitRec.m_errortype == 11)
		{
			handleDBException(m_newPatientVisitRec.m_SA_e, "creating patient visit", "CreatePatientVisit", bMoveMessage: true);
		}
		else if (m_newPatientVisitRec.m_errortype == 12)
		{
			handleException(m_newPatientVisitRec.m_e, "creating patient visit", "CreatePatientVisit", bMoveMessage: true);
		}
		m_DBPatient.FillInVisitPatientTrackingInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, m_newPatientVisitRec, null);
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: false, "Create Patient Visit", !bOK, ref myPTDBWriteCommand);
		return bOK;
	}

	private bool DeletePreviousPatientVisit()
	{
		m_DBPatient.FillInVisitPatientTrackingInfo(m_NNBase, ref myDBReadCommand, ref myPatientTrackingRec, null, m_PrevIDs_PatientVisitRec);
		bool bOK = m_PrevIDs_PatientVisitRec.Delete(m_NNBase, ref myDBWriteCommand);
		if (bOK)
		{
			bChangesMade = true;
			bPrevVisitDeleted = true;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Previous IDs visit deleted", isXml: false, "HL7.DeletePreviousPatientVisit");
			}
			m_DBPatient.UpdateAffectedLocationList(m_PrevIDs_PatientVisitRec);
		}
		else if (m_PrevIDs_PatientVisitRec.m_errortype == 11)
		{
			handleDBException(m_PrevIDs_PatientVisitRec.m_SA_e, "deleting previous patient visit", "DeletePreviousPatientVisit", bMoveMessage: true);
		}
		else if (m_PrevIDs_PatientVisitRec.m_errortype == 12)
		{
			handleException(m_PrevIDs_PatientVisitRec.m_e, "deleting previous patient visit", "DeletePreviousPatientVisit", bMoveMessage: true);
		}
		myPatientTrackingRec.CreatePatientTracking(m_NNBase, bPrev: true, "Delete Previous Patient Visit", !bOK, ref myPTDBWriteCommand);
		return bOK;
	}

	private void UpdateDischargeTime(ref PatientVisitRec newPatVisit, PatientVisitRec oldPatVisit, int iActiveHours)
	{
		newDischargeTime = newPatVisit.m_dischargetime;
		oldDischargeTime = oldPatVisit.m_dischargetime;
		if (oldDischargeTime.Year > 1800 && oldDischargeTime.Year < 2037 && (newDischargeTime.Year <= 1800 || newDischargeTime.Year == 2037))
		{
			if (MessageSubType != "A01" && MessageSubType != "A04" && MessageSubType != "A05" && MessageSubType != "A06" && MessageSubType != "A07" && MessageSubType != "A13" && MessageSubType != "A22" && MessageSubType != "A52")
			{
				myPatientTrackingRec.m_dischargetime = (DischargeTime = oldDischargeTime);
				bDischargeFacilityTime = false;
				DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
				newDischargeTime = oldDischargeTime;
			}
			else if (MessageSubType == "A07")
			{
				myPatientTrackingRec.m_dischargetime = (DischargeTime = DateTime.Now.AddHours(iActiveHours));
				bDischargeFacilityTime = false;
				DischargeDateTime = DischargeTime.ToString("yyyyMMddHHmmss");
				newDischargeTime = DischargeTime;
			}
		}
		newPatVisit.m_dischargetime = newDischargeTime;
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
		PatientID = GetHL7Component(segmentparse, 2u, 1u).Replace("\"", "").Trim();
		MedicalRecordNumber = GetHL7Component(segmentparse, 3u, 1u).Replace("\"", "").Trim();
		MRNAssigningAuthority = GetHL7Component(segmentparse, 3u, 4u);
		MRNAssigningFacility = GetHL7Component(segmentparse, 3u, 6u);
		LastName = GetHL7Component(segmentparse, 5u, 1u);
		LastName = Regex.Replace(LastName, "[\0-\u001f]", string.Empty);
		LastName = LastName.Trim();
		FirstName = GetHL7Component(segmentparse, 5u, 2u);
		FirstName = Regex.Replace(FirstName, "[\0-\u001f]", string.Empty);
		FirstName = FirstName.Trim();
		MiddleName = GetHL7Component(segmentparse, 5u, 3u);
		MiddleName = Regex.Replace(MiddleName, "[\0-\u001f]", string.Empty);
		MiddleName = MiddleName.Trim();
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
		Race_HL7 = GetHL7Field(segmentparse, 10u);
		Race_DML = Race_HL7;
		AccountAssigningAuthority = GetHL7Component(segmentparse, 18u, 4u);
		AccountAssigningFacility = GetHL7Component(segmentparse, 18u, 6u);
		if (Comp.Compare(sAccountSegment, "PID") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			AccountNumber = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "").Trim();
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
		AttendingPhysician = GetHL7Field(segmentparse, 7u);
		ReportingPhysician = GetHL7Field(segmentparse, 8u);
		ConsultingPhysician = GetHL7Field(segmentparse, 9u);
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
		AdmitTime = YMDhms_To_DateTime(ref AdmitDateTime, "AdmitDateTime", bAllowMaxHL7Year: false, bAllowFuture: true);
		DischargeDateTime = GetHL7Field(segmentparse, 45u);
		if (DischargeDateTime.Length == 0 || DischargeDateTime == "\"\"")
		{
			DischargeDateTime = "20371231000000";
		}
		myPatientTrackingRec.m_dischargetime = (DischargeTime = YMDhms_To_DateTime(ref DischargeDateTime, "DischargeDateTime", bAllowMaxHL7Year: true, bAllowFuture: true));
		bDischargeFacilityTime = true;
		if (Comp.Compare(sAccountSegment, "PV1") == 0)
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			AccountNumber = GetHL7Component(segmentparse, iAccountField, iAccountComponent).Replace("\"", "").Trim();
		}
	}

	private void ProcessResultSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		string test_code = GetHL7Component(segmentparse, 3u, 1u);
		string szResultValue = GetHL7Component(segmentparse, 5u, 1u);
		string units = GetHL7Component(segmentparse, 6u, 1u);
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

	private void HeightHL7toDML()
	{
		Height_DML_value = Height_HL7_value;
		if (Height_HL7_units == "cm")
		{
			Height_DML_units = "CMS";
		}
		else if (Height_HL7_units == "in")
		{
			Height_DML_units = "INS";
		}
	}

	private void WeightHL7toDML()
	{
		Weight_DML_value = Weight_HL7_value;
		if (Weight_HL7_units == "Kg")
		{
			Weight_DML_units = "KGS";
		}
		else if (Weight_HL7_units == "lb")
		{
			Weight_DML_units = "LBS";
		}
	}

	private void ProcessDiagnosisSegment(string segment)
	{
		segmentparse.remainder = segment.Substring(4);
		segmentparse.curfield = 1u;
		segmentparse.curcomponent = 1u;
		Diagnosis += GetHL7Component(segmentparse, 4u, 1u);
	}

	private DateTime YMDhms_To_DateTime(ref string YMDhms, string FieldName, bool bAllowMaxHL7Year, bool bAllowFuture)
	{
		DateTime NoDateTime = new DateTime(1, 1, 1, 0, 0, 0);
		DateTime RetDateTime = NoDateTime;
		bool bAddOneDay = false;
		try
		{
			YMDhms = YMDhms.Trim();
			if (YMDhms.Length > 7 && isNumeric(YMDhms, NumberStyles.Integer))
			{
				int year = Convert.ToInt32(YMDhms.Substring(0, 4));
				if (year < 1 || (!bAllowMaxHL7Year && year > DateTime.Now.Year + 1))
				{
					string sError = ((year < 1) ? "Year cannot be zero" : ("Year cannot be more than next year for " + FieldName));
					m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError);
				}
				else
				{
					int month = Convert.ToInt32(YMDhms.Substring(4, 2));
					if (month < 1 || month > 12)
					{
						string sError2 = ((month < 1) ? "Month cannot be zero" : "Month cannot be more than 12");
						m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError2);
					}
					else
					{
						int day = Convert.ToInt32(YMDhms.Substring(6, 2));
						if (day < 1 || day > DateTime.DaysInMonth(year, month))
						{
							string sError3 = ((day < 1) ? "Day cannot be zero" : "Day cannot be more than days in month");
							m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError3);
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
								string sError4 = "Hour cannot be more than 23";
								m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError4);
							}
							else
							{
								int minute = ((YMDhms.Length > 11) ? Convert.ToInt32(YMDhms.Substring(10, 2)) : 0);
								if (minute > 59)
								{
									string sError5 = "Minute cannot be more than 59";
									m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError5);
								}
								else
								{
									int second = ((YMDhms.Length > 13) ? Convert.ToInt32(YMDhms.Substring(12, 2)) : 0);
									if (second > 59)
									{
										string sError6 = "Second cannot be more than 59";
										m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError6);
									}
									else
									{
										RetDateTime = new DateTime(year, month, day, hour, minute, second);
										if (bAddOneDay)
										{
											RetDateTime = RetDateTime.AddDays(1.0);
										}
										if (!bAllowFuture && DateTime.Compare(RetDateTime, DateTime.Now) > 0)
										{
											string sError7 = "DateTime cannot be in the future for " + FieldName;
											m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError7);
											RetDateTime = NoDateTime;
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
				string sError8 = ((YMDhms.Length < 8) ? "DateTime must be at least 8 digits" : "DateTime must be numeric");
				m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid date/time format", "E", "parsing " + FieldName, "YMDhms_To_DateTime", sError8);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("YMDhms_To_DateTime");
		}
		catch (Exception e)
		{
			handleException(e, "parsing " + FieldName, "YMDhms_To_DateTime", bMoveMessage: true);
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
			m_NNBase.ReportErrorDB(AppErrorMsg = "Invalid MRG segment format", "E", "parsing MRG Segment", "ProcessMergeSegment", "The MRG segment is too short");
		}
		else
		{
			segmentparse.remainder = segment.Substring(4);
			segmentparse.curfield = 1u;
			segmentparse.curcomponent = 1u;
			PreviousMedicalRecordNumber = GetHL7Component(segmentparse, 1u, 1u).Replace("\"", "").Trim();
			PreviousMRNAssigningAuthority = GetHL7Component(segmentparse, 1u, 4u);
			PreviousMRNAssigningFacility = GetHL7Component(segmentparse, 1u, 6u);
			PreviousPatientAccount = GetHL7Component(segmentparse, 3u, 1u).Replace("\"", "").Trim();
			PreviousAccountAssigningAuthority = GetHL7Component(segmentparse, 3u, 4u);
			PreviousAccountAssigningFacility = GetHL7Component(segmentparse, 3u, 6u);
			PreviousPatientID = GetHL7Component(segmentparse, 4u, 1u).Replace("\"", "").Trim();
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

	private void RemoveMessageFromQueue()
	{
		try
		{
			m_Queue.ReceiveById(m_qMsg.Id);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("RemoveMessageFromQueue");
		}
		catch (Exception ex2)
		{
			m_NNBase.log("RemoveMessageFromQueue failed: " + ex2.Message, isXml: false, m_NNBase.m_WhoAmI);
			m_NNBase.log(m_message, isXml: false, m_NNBase.m_WhoAmI);
		}
		finally
		{
			m_qMsg = null;
			m_Queue.BeginPeek();
		}
	}

	private void MoveMessageToSubQueue()
	{
		try
		{
			int result = MQMessageAPI.MQMoveMessage(m_Queue.ReadHandle, m_rejectQueneHandle, m_qMsg.LookupId, IntPtr.Zero);
			if (result < 0)
			{
				m_NNBase.log("MQMoveMessage failed: " + result, isXml: false, m_NNBase.m_WhoAmI);
				m_NNBase.log(m_message, isXml: false, m_NNBase.m_WhoAmI);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("MoveMessageToSubQueue");
		}
		catch (Exception ex2)
		{
			m_NNBase.log("MoveMessageToSubQueue failed: " + ex2.Message, isXml: false, m_NNBase.m_WhoAmI);
			m_NNBase.log(m_message, isXml: false, m_NNBase.m_WhoAmI);
		}
		finally
		{
			m_qMsg = null;
			bRemoveMessageWhenDone = false;
			m_Queue.BeginPeek();
		}
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
			m_Queue.Close();
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
				string sCommand = "update DBA.health_ping set update_time = now(*), last_disconnect_dttm = now(*) where process_name = 'RTMADTP' and host = '" + m_NNBase.GetLocalPOP() + "'";
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
		Console.WriteLine("Shutdown");
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

	private void handleException(Exception e, string when, string from, bool bMoveMessage)
	{
		if (m_isShuttingDown)
		{
			return;
		}
		bool bDBDisconnect = false;
		bool bDBAccessError = m_NNBase.ExceptionIsDBAccessError(e, ref bDBDisconnect);
		if (bDBAccessError)
		{
			m_NNBase.bDBAvailable &= !bDBDisconnect;
		}
		else
		{
			m_NNBase.ForceLogging("Exception");
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
		}
		AppErrorMsg = m_NNBase.ExceptionShutDownMessage(e);
		m_NNBase.ReportExceptionNoEvent(e, when, from);
		if (!bDBAccessError)
		{
			m_NNBase.Rollback(ref myTransaction, "Runtime DB");
			myPatientTrackingRec.Rollback(m_NNBase);
			if (bMoveMessage && m_qMsg != null)
			{
				MoveMessageToSubQueue();
			}
		}
		ShutDown(AppErrorMsg, from, bExit: true);
	}

	private void handleDBException(OdbcException e, string when, string from, bool bMoveMessage)
	{
		if (m_isShuttingDown)
		{
			return;
		}
		bool bDBDisconnect = false;
		bool bDBAccessError = m_NNBase.DBExceptionIsDBAccessError(e, ref bDBDisconnect);
		if (bDBAccessError)
		{
			m_NNBase.bDBAvailable &= !bDBDisconnect;
		}
		else
		{
			m_NNBase.ForceLogging("DatabaseException");
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
		}
		AppErrorMsg = m_NNBase.DBExceptionShutDownMessage(e);
		m_NNBase.ReportDBExceptionNoEvent(e, when, from);
		if (!bDBAccessError)
		{
			m_NNBase.Rollback(ref myTransaction, "Runtime DB");
			myPatientTrackingRec.Rollback(m_NNBase);
			if (bMoveMessage && m_qMsg != null)
			{
				MoveMessageToSubQueue();
			}
		}
		ShutDown(AppErrorMsg, from, bExit: true);
	}

	private void ProtocolThread()
	{
		InitializeMSMQ();
		if (bOK_File)
		{
		}
		while (!m_isShutDown && !m_isShuttingDown)
		{
			try
			{
				Thread.Sleep(1000);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("ProtocolThread");
			}
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
			CreateRejectQueue(ref m_rejectQueneHandle);
			m_Queue = new MessageQueue(ConfigurationManager.AppSettings["queueName"]);
			m_Queue.PeekCompleted += GetMSMQMessage;
			m_Queue.BeginPeek();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("InitializeMSMQ");
		}
		catch (MessageQueueException e)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				AppErrorMsg = m_NNBase.ExceptionShutDownMessage(e);
				m_NNBase.ReportException(e, "initializing queue", "InitializMSMQ");
				ShutDown(AppErrorMsg, "InitializeMSMQ", bExit: true);
			}
		}
		catch (Exception e2)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				AppErrorMsg = m_NNBase.ExceptionShutDownMessage(e2);
				m_NNBase.ReportException(e2, "initializing queue", "InitializMSMQ");
				ShutDown(AppErrorMsg, "InitializeMSMQ", bExit: true);
			}
		}
	}

	public void GetMSMQMessage(object source, PeekCompletedEventArgs asyncResult)
	{
		try
		{
			m_Queue = (MessageQueue)source;
			m_Queue.Formatter = new XmlMessageFormatter(new Type[1] { typeof(string) });
			m_qMsg = m_Queue.EndPeek(asyncResult.AsyncResult);
			if (!m_qMsg.Label.Contains("RTMADTQ"))
			{
				RemoveMessageFromQueue();
			}
			else if (m_NNBase.bDBAvailable)
			{
				m_message = (string)m_qMsg.Body;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("======Read=========================================================", isXml: false, m_NNBase.m_WhoAmI);
					string logMsg = RemoveAsciiControlChar(m_message);
					m_NNBase.log(logMsg, isXml: false, m_NNBase.m_WhoAmI);
				}
				ProcessMessage();
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("GetMSMQMessage");
		}
		catch (MessageQueueException e)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				AppErrorMsg = m_NNBase.ExceptionShutDownMessage(e);
				m_NNBase.ReportException(e, "getting message from queue", "GetMSMQMessage");
				ShutDown(AppErrorMsg, "GetMSMQMessage", bExit: true);
			}
		}
		catch (Exception e2)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				AppErrorMsg = m_NNBase.ExceptionShutDownMessage(e2);
				m_NNBase.ReportException(e2, "getting message from queue", "GetMSMQMessage");
				ShutDown(AppErrorMsg, "GetMSMQMessage", bExit: true);
			}
		}
	}

	private void CreateRejectQueue(ref IntPtr rejectHandle)
	{
		string rejectQueuePath = "DIRECT=OS:" + ConfigurationManager.AppSettings["queueName"] + ";reject_queue";
		int result = MQMessageAPI.MQOpenQueue(rejectQueuePath, 4, 0, ref rejectHandle);
		if (result < 0 && m_NNBase.m_isLogging)
		{
			m_NNBase.log("MQOpenQueue failed: " + result, isXml: false, m_NNBase.m_WhoAmI);
		}
	}

	private string RemoveAsciiControlChar(string inputstring)
	{
		char newChar = ' ';
		return inputstring.Replace('\v', newChar).Replace('\u001c', newChar).Replace('\u0003', newChar)
			.Replace('\u0002', newChar);
	}
}
