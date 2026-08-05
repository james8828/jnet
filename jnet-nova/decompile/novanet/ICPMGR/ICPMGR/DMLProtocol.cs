using System;
using System.Collections;
using System.Collections.Generic;
using System.Data;
using System.Data.Odbc;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Security;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Xml;
using FlexTimers;
using LitJson;
using NNClass;
using Patient;

namespace ICPMGR;

public class DMLProtocol : Protocol
{
	protected enum DMLSTATE
	{
		WAIT_HELLO,
		ACK_HELLO,
		WAIT_STATUS,
		ACK_STATUS,
		REQ_OBS,
		OBS_EOT,
		REQ_EVS,
		EVS_EOT,
		SET_TIME_RCV_ACK,
		SET_TIME,
		SETUP_SEND_EOT,
		SETUP_RCV_ACK,
		SETUP_EOT,
		BGA_SCHEDULE_SEND,
		BGA_SCHEDULE_RCV_ACK,
		WIFI_SETUP_SEND_EOT,
		WIFI_SETUP_RCV_ACK,
		WIFI_SETUP_EOT,
		WIFI_CERT_SEND_EOT,
		WIFI_CERT_RCV_ACK,
		WIFI_CERT_EOT,
		LOC_SEND_EOT,
		LOC_RCV_ACK,
		LOC_EOT,
		OPR_SEND_EOT,
		OPR_RCV_ACK,
		OPR_EOT,
		PAT_SEND_EOT,
		PAT_RCV_ACK,
		PAT_EOT,
		PHYS_SEND_EOT,
		PHYS_RCV_ACK,
		PHYS_EOT,
		FIRM_SEND_EOT,
		FIRM_RCV_ACK,
		FIRM_EOT,
		REAG_SEND_EOT,
		REAG_RCV_ACK,
		REAG_EOT,
		SYSTEM_STATUS_RCV,
		QUERY_RCV,
		RC_COMMAND_SENT,
		CONTINUOUS_RCV_ACK,
		CONTINUOUS,
		TERMINATE_RCV_ACK,
		TERMINATE,
		END,
		MAX
	}

	private struct Sample_Table
	{
		public string sample_key_num;

		public string Accession_num;

		public DateTime sample_Date;

		public string transmitted_flag;

		public string control_type;

		public string control_lot_num;

		public string strip_lot_num;

		public string xml_text;

		public string patient_id;

		public string medrec_num;

		public string account_num;

		public string fac_name;

		public string loc_name;

		public string device_serial;

		public string saved_to_history_db_flag;

		public string device_type;

		public string device_sw_ver;

		public string device_name;

		public string lot_level;

		public string internal_external;
	}

	private struct Delete_Patient
	{
		public string patientID;

		public string mrn;

		public string accountNum;
	}

	private struct Insert_Patient
	{
		public string patientID;

		public string mrn;

		public string accountNum;

		public string lastName;

		public string firstName;

		public string middleName;

		public string sex;

		public string birthDate;

		public string prefix;

		public string suffix;

		public string race;

		public string diagnosis;

		public string height;

		public string hUnit;

		public string weight;

		public string wUnit;

		public string bedNum;

		public string roomNum;

		public string location;

		public string cPhysician;

		public string rPhysician;

		public string aPhysician;

		public string facility;

		public string Notes;
	}

	private struct ReagentRec
	{
		public string lot_number;

		public string lot_name;

		public string lot_type;

		public DateTime exp_date;

		public string level_number;

		public string level_type;

		public string observation_id;

		public string lo_limit;

		public string hi_limit;

		public string units;
	}

	private NNBase m_NNBase = new NNBase();

	private bool m_force_meter_default;

	private bool m_force_result_default;

	private bool m_lookup_location;

	private bool m_send_location;

	private Port.AsynchNetworkServer.ClientHandler m_parent;

	private byte[] m_readbuffer;

	private List<byte[]> m_readBufferList = new List<byte[]>();

	private byte[] m_writebuffer;

	private byte[] m_asyncwritebuffer;

	private string m_message = "";

	private bool m_isPartial;

	private XmlDocument m_doc;

	private IPAddress m_IP_Address;

	private string m_max_message_sz = "4096";

	private string m_control_internal_external = "";

	private NetworkStream m_networkStream;

	private AsyncCallback callbackWrite;

	private int m_port_num;

	protected bool m_isProcessing;

	private string m_serial_id = "";

	private string m_comm_record_num;

	private string m_manufacturer_name;

	private string m_device_name = "";

	private string m_hw_version;

	private string m_sw_version;

	private string m_sw_lang_version;

	private string m_language_short;

	private string m_language_long;

	private string m_facility = "";

	private string m_location = "";

	private string m_facil_num = "";

	private string m_control_id = "";

	private string m_vendor_id = "";

	private string m_loc_num = "";

	private string m_loc_def_pat_id = "";

	private string m_inst_num = "";

	private string m_from_inst_id = "";

	private string m_inst_type = "";

	private string m_inst_class = "";

	private Dictionary<string, bool> m_SupportedTopic = new Dictionary<string, bool>();

	private Dictionary<string, bool> m_SupportedDirective = new Dictionary<string, bool>();

	private bool m_isContinuous;

	private string m_portType = "";

	private int m_DMLState;

	private string m_new_observations_qty;

	private string m_new_events_qty;

	private string m_condition_cd;

	private string m_operators_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_patients_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_setup_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_wifi_setup_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_cert_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_loc_list_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_phys_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private string m_reag_update_dttm = "2000-01-01T00:00:00.00-04:00";

	private int m_imsgid = 4000;

	private int m_kpaTimeoutCount;

	private bool m_waiting;

	private bool m_busy;

	private bool m_TimerSending;

	private bool m_ProtocolSending;

	private bool m_kpaEnabled;

	private string m_last_incremental = "C";

	private long m_ContinuousMinuteCount;

	private long m_LastContinuousMinuteCount;

	private long m_StartContinuousMinute;

	private DateTime m_ContinuousNow;

	private long LastContinuousOperatorSend;

	private long LastContinuousPatientSend;

	private long m_ListsMinuteCount;

	private long m_LastListsMinuteCount;

	private DateTime m_ListsNow;

	private DateTime m_LastListsNow;

	private long LastListsPatientBuild;

	protected DateTime m_LastPatientListDateTime;

	protected DateTime m_PrevPatientListDateTime;

	private DateTime device_patient_update_datetime;

	private DateTime list_patient_update_datetime;

	private string s_msg_type_rcvd;

	private bool m_OprListIncrSupported;

	private bool m_OprListFullSupported;

	private bool m_PatListIncrSupported;

	private bool m_PatListFullSupported;

	private bool m_PhysListIncrSupported;

	private bool m_PhysListFullSupported;

	private bool m_FirmSupported;

	private bool m_LocListSupported;

	private bool m_SetupSupported;

	private bool m_tdomeSetupSupported;

	private bool m_bgaSetupSupported;

	private bool m_WifiSetupSupported;

	private bool m_WifiCertSupported;

	private bool m_ReagSupported;

	private string m_ReadString = "";

	private DateTime m_last_eot_update_time = DateTime.Now;

	private bool m_SetTimeSupported;

	private bool m_ContinuousSupported;

	private DateTime m_LastNow;

	private bool m_LastNowInit;

	private long OneMinute = 600000000L;

	private bool m_was_unassigned;

	private bool m_last_loc_used;

	private string m_disconnect_active;

	private string m_disconnect_minutes;

	private bool m_supportMTE;

	private bool m_AlwaysSend;

	private int OpListFreq = 10;

	private int PatListFreq = 10;

	private int MaxAddDelPerMsg = 20;

	private int MaxAddDelPerIncr = 200;

	private int m_maxVisitLocations = 1000;

	private object m_dmlStateLock = new object();

	private object m_sendStringLock = new object();

	private object m_notifyLock = new object();

	private bool bRuntimeDBAvailable = true;

	private OdbcConnection myRuntimeDBConnection;

	private OdbcCommand myRuntimeDBReadCommand;

	private OdbcDataReader myRuntimeDBReadReader;

	private OdbcCommand myRuntimeDBWriteCommand;

	private OdbcConnection myStringsDBConnection;

	private OdbcCommand myStringsDBReadCommand;

	private OdbcDataReader myStringsDBReadReader;

	private bool m_b_loc_to_config_inst_type_column;

	private bool m_b_test_offsets_inst_class_column;

	private bool m_b_test_offsets_inst_type_column;

	private bool m_b_instruments_tests_inst_class_column;

	private bool m_b_loc_last_update_inst_class_column;

	private bool m_b_loc_last_update_inst_type_column;

	private string m_TimeZoneName;

	private TimeZoneInfo m_TimeZoneInfo;

	private DMLICPBase m_DMLICPBase;

	private bool bIsDeviceProtocol;

	private bool bIsListCreator;

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	private CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private int recSize = 2048;

	private bool m_LastTimeFullList = true;

	private int m_maxDownloadOperator = 4000;

	private int deviceTotalPatients = -1;

	private PatientList CompletePatientList;

	private string m_MAC_Address = "00-00-00-00-00-00";

	private string m_Wifi_MAC_Address = "00-00-00-00-00-00";

	public bool IsAliveAndWell()
	{
		return !m_isShutDown && !m_isShuttingDown && !m_pleaseShutDown && !m_stopping;
	}

	public bool IsDeadOrDying()
	{
		return m_isShutDown || m_isShuttingDown || m_pleaseShutDown || m_stopping;
	}

	public bool IsDying()
	{
		return m_isShuttingDown || m_pleaseShutDown || m_stopping;
	}

	public bool WasAskedToStop()
	{
		return (m_pleaseShutDown || m_stopping) && !m_isShutDown && !m_isShuttingDown;
	}

	public override void ProcessNotify(int cd, string Message)
	{
		lock (m_notifyLock)
		{
			switch (cd)
			{
			case 1:
				if (!m_NNBase.m_isLogging)
				{
					if (m_serial_id.Length > 0)
					{
						m_NNBase.m_LogName = m_serial_id;
					}
					m_NNBase.StartLogging();
				}
				break;
			case 2:
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("logging turned off", isXml: false, "Ports");
					m_NNBase.StopLogging();
				}
				break;
			case 3:
				Start();
				break;
			case -1:
				m_ShutdownReason = Message;
				m_pleaseShutDown = true;
				try
				{
					cmTimer.CancelWait();
					break;
				}
				catch
				{
					break;
				}
			case 8:
				ProcessRC(Message);
				break;
			}
		}
	}

	private bool SendRCCommand(string command, string lot, string level)
	{
		bool ret = false;
		try
		{
			string test = string.Empty;
			MemoryStream stream = new MemoryStream();
			m_DMLState = 41;
			XmlWriterSettings setttings = new XmlWriterSettings();
			setttings.OmitXmlDeclaration = true;
			XmlWriter writer = XmlWriter.Create(stream, setttings);
			writer.WriteStartDocument();
			writer.WriteStartElement("DTV.NOVA.EXEC_SEQ");
			AddDMLHeader(ref writer);
			writer.WriteStartElement("DTV");
			writer.WriteStartElement("DTV.command_cd");
			writer.WriteAttributeString("V", command);
			writer.WriteEndElement();
			writer.WriteEndElement();
			if (!string.IsNullOrEmpty(lot) || !string.IsNullOrEmpty(level))
			{
				writer.WriteStartElement("LotInfo");
				if (!string.IsNullOrEmpty(lot))
				{
					writer.WriteStartElement("lot_number");
					writer.WriteAttributeString("V", lot);
					writer.WriteEndElement();
				}
				if (!string.IsNullOrEmpty(level))
				{
					writer.WriteStartElement("level_cd");
					writer.WriteAttributeString("V", level);
					writer.WriteEndElement();
				}
				writer.WriteEndElement();
			}
			writer.WriteEndElement();
			writer.WriteEndDocument();
			writer.Flush();
			StreamReader reader = new StreamReader(stream, Encoding.UTF8, detectEncodingFromByteOrderMarks: true);
			stream.Seek(0L, SeekOrigin.Begin);
			test = reader.ReadToEnd();
			SendString(test, isPartial: false, trunc: false);
			stream.Close();
			reader.Close();
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (XmlException ex2)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("XML Exception " + ex2.Message, isXml: false, "SendRCCommand " + m_serial_id);
			}
			ret = false;
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Exception " + ex3.Message, isXml: false, "SendRCCommand " + m_serial_id);
			}
			ret = false;
		}
		return ret;
	}

	private bool ProcessRC(string rcMessage)
	{
		bool ret = false;
		try
		{
			if (!string.IsNullOrEmpty(rcMessage))
			{
				string[] rcMsg = rcMessage.Split('^');
				if (rcMsg.Length == 6 && rcMsg[1].ToUpper().CompareTo("RC") == 0)
				{
					ret = SendRCCommand(rcMsg[3], rcMsg[4], rcMsg[5]);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception ex2)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Exception " + ex2.Message, isXml: false, "ProcessRC " + m_serial_id);
			}
		}
		return ret;
	}

	private void ProcessSystemStatus(string topicName)
	{
		try
		{
			bool hasError = false;
			XmlElement root = m_doc.DocumentElement;
			XmlElement elem = (XmlElement)root.SelectSingleNode("HDR/HDR.control_id");
			if (elem != null)
			{
				m_control_id = elem.GetAttribute("V");
			}
			switch (topicName)
			{
			case "NOVA.ANALYZER_STATE":
			{
				XmlNode analyzerState = root.SelectSingleNode("ANALYZER_STATE");
				XmlNodeReader reader = new XmlNodeReader(analyzerState);
				hasError = ProcessAnalyzerState(reader);
				reader.Close();
				break;
			}
			case "NOVA.CARTRIDGE_STATUS":
			{
				XmlNodeList nodeList = root.SelectNodes("CARTRIDGE_STATUS");
				hasError = ProcessCartridgeStatus(nodeList);
				break;
			}
			case "NOVA.TEST_STATUS":
			{
				XmlNodeList nodeList = root.SelectNodes("TEST_STATUS");
				hasError = ProcessTestStatus(nodeList);
				break;
			}
			}
			SendAcknowledgeMessage(m_control_id, hasError);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (XmlException ex2)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("XML Exception " + ex2.Message, isXml: false, "ProcessSystemStatus");
			}
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(ex3.Message, isXml: false, "dml-ProcessSystemStatus");
			}
		}
	}

	private bool ProcessAnalyzerState(XmlNodeReader reader)
	{
		reader.ReadToFollowing("state");
		string stateMajor = reader.GetAttribute("V");
		reader.ReadToFollowing("state_details");
		string stateDetails = reader.GetAttribute("V");
		bool hasError = true;
		if (GetRuntimeDBConnection())
		{
			OdbcCommand command = null;
			OdbcCommand command2 = null;
			try
			{
				command = myRuntimeDBConnection.CreateCommand();
				command2 = myRuntimeDBConnection.CreateCommand();
				if (m_inst_num.Length < 1)
				{
					command.CommandText = $"SELECT inst_num FROM DBA.instruments WHERE serial_no = '{m_serial_id}'";
					m_inst_num = (string)command.ExecuteScalar();
				}
				string sqlInsert = "INSERT INTO dba.bga_state (inst_num,state_major,state_details) ON EXISTING UPDATE values (?,?,?)";
				command2.CommandText = sqlInsert;
				command2.Parameters.AddWithValue("@inst_num", m_inst_num);
				command2.Parameters.AddWithValue("@state_major", stateMajor);
				command2.Parameters.AddWithValue("@state_details", stateDetails);
				command2.ExecuteNonQuery();
				command2.Parameters.Clear();
				hasError = false;
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (OdbcException e)
			{
				handleDBException(e, reader.ToString(), "ProcessAnalyzerState", "BGA");
			}
			catch (Exception e2)
			{
				handleException(e2, reader.ToString(), "ProcessAnalyzerState", "BGA");
			}
			finally
			{
				CleanCommand(command);
				CleanCommand(command2);
			}
		}
		return hasError;
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

	private bool GetRuntimeDBConnection()
	{
		if (myRuntimeDBConnection != null && myRuntimeDBConnection.State.Equals(ConnectionState.Open))
		{
			return true;
		}
		return OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
	}

	private bool ProcessTestStatus(XmlNodeList nodeList2)
	{
		bool hasError = true;
		XmlNodeList nodeList3 = nodeList2[0].ChildNodes;
		if (GetRuntimeDBConnection())
		{
			OdbcCommand command = null;
			OdbcCommand command2 = null;
			command = myRuntimeDBConnection.CreateCommand();
			command2 = myRuntimeDBConnection.CreateCommand();
			if (m_inst_num.Length < 1)
			{
				command.CommandText = $"SELECT inst_num FROM DBA.instruments WHERE serial_no = '{m_serial_id}'";
				m_inst_num = (string)command.ExecuteScalar();
			}
			string sqlDelete = "DELETE FROM DBA.bga_test_status WHERE inst_num='" + m_inst_num + "'";
			string sqlInsert = "INSERT INTO DBA.bga_test_status(inst_num,observation_id,is_calibrated,is_qc_lockout,test_issues) VALUES (?,?,?,?,?)";
			command.CommandText = sqlDelete;
			command.ExecuteNonQuery();
			try
			{
				foreach (XmlNode thisNode in nodeList3)
				{
					string is_calibrated = string.Empty;
					string is_qc_lockout = string.Empty;
					string jsonIssues = string.Empty;
					string lockoutDetails = string.Empty;
					string obs_id = string.Empty;
					XmlElement elm = (XmlElement)thisNode;
					elm.GetAttribute("test_code");
					obs_id = elm.GetAttribute("DN");
					obs_id = Regex.Unescape(obs_id);
					is_calibrated = elm.GetAttribute("is_calibrated");
					is_calibrated = ((is_calibrated.ToLower().CompareTo("true") != 0) ? "F" : "T");
					is_qc_lockout = elm.GetAttribute("is_QC_locked");
					is_qc_lockout = ((is_qc_lockout.ToLower().CompareTo("true") != 0) ? "F" : "T");
					if (is_calibrated.CompareTo("T") != 0 || is_qc_lockout.CompareTo("F") != 0)
					{
						if (is_calibrated.CompareTo("F") == 0)
						{
							jsonIssues = MakeJSONTestIssue("CAL", "NOT_CALIBRATED");
						}
						else
						{
							if (elm.GetAttribute("int_QC_locked_1").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = "INT_QC_LOCKED_1";
							}
							if (elm.GetAttribute("int_QC_locked_2").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = ((lockoutDetails.Length >= 1) ? (lockoutDetails + ",INT_QC_LOCKED_2") : "INT_QC_LOCKED_2");
							}
							if (elm.GetAttribute("int_QC_locked_3").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = ((lockoutDetails.Length >= 1) ? (lockoutDetails + ",INT_QC_LOCKED_3") : "INT_QC_LOCKED_3");
							}
							if (elm.GetAttribute("ext_QC_locked_1").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = ((lockoutDetails.Length >= 1) ? (lockoutDetails + ",EXT_QC_LOCKED_1") : "EXT_QC_LOCKED_1");
							}
							if (elm.GetAttribute("ext_QC_locked_2").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = ((lockoutDetails.Length >= 1) ? (lockoutDetails + ",EXT_QC_LOCKED_2") : "EXT_QC_LOCKED_2");
							}
							if (elm.GetAttribute("ext_QC_locked_3").ToLower().CompareTo("true") == 0)
							{
								lockoutDetails = ((lockoutDetails.Length >= 1) ? (lockoutDetails + ",EXT_QC_LOCKED_3") : "EXT_QC_LOCKED_3");
							}
							jsonIssues = MakeJSONTestIssue("QC", lockoutDetails);
						}
					}
					try
					{
						command2.CommandText = sqlInsert;
						command2.Parameters.AddWithValue("@vinst_num", m_inst_num);
						command2.Parameters.AddWithValue("@vOBS", obs_id);
						command2.Parameters.AddWithValue("@vis_calibrated", is_calibrated);
						command2.Parameters.AddWithValue("@vis_qc_lockout", is_qc_lockout);
						command2.Parameters.AddWithValue("@test_issues", jsonIssues);
						command2.ExecuteNonQuery();
						command2.Parameters.Clear();
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("Protocol");
					}
					catch (OdbcException e)
					{
						handleDBException(e, jsonIssues, "ProcessTestStatus", "BGA");
						hasError = true;
					}
					catch (Exception e2)
					{
						handleException(e2, jsonIssues, "ProcessTestStatus", "BGA");
						hasError = true;
					}
				}
				hasError = false;
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (OdbcException ex3)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("DBException " + ex3.Message, isXml: false, "ProcessTestStatus " + m_serial_id);
				}
			}
			catch (Exception ex4)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Exception " + ex4.Message, isXml: false, "ProcessTestStatus " + m_serial_id);
				}
			}
			finally
			{
				CleanCommand(command);
				CleanCommand(command2);
			}
		}
		return hasError;
	}

	private bool ProcessCartridgeStatus(XmlNodeList nodeList)
	{
		string cartType = string.Empty;
		string lotNum = string.Empty;
		string rFluidlevel = string.Empty;
		string json_status = string.Empty;
		bool hasError = true;
		_ = string.Empty;
		_ = DateTime.MaxValue;
		_ = string.Empty;
		if (GetRuntimeDBConnection())
		{
			OdbcCommand command = null;
			command = myRuntimeDBConnection.CreateCommand();
			OdbcCommand command2 = myRuntimeDBConnection.CreateCommand();
			try
			{
				if (string.IsNullOrEmpty(m_inst_num))
				{
					command.CommandText = $"SELECT inst_num FROM DBA.instruments WHERE serial_no = '{m_serial_id}'";
					m_inst_num = (string)command.ExecuteScalar();
				}
				string sqlInsertUpdate = "INSERT INTO dba.bga_cartridge_status (inst_num,cartridge_type,lot_num, remaining_volume,status) ON EXISTING UPDATE values (?,?,?,?,?)";
				command2.CommandText = sqlInsertUpdate;
				foreach (XmlNode thisNode in nodeList)
				{
					string name = string.Empty;
					string value = string.Empty;
					JsonWriter jsonW = new JsonWriter();
					jsonW.WriteObjectStart();
					foreach (XmlElement elm in thisNode)
					{
						name = elm.Name;
						value = elm.GetAttribute("V");
						jsonW.WritePropertyName(name);
						jsonW.Write(value);
						switch (name)
						{
						case "name":
							cartType = value;
							break;
						case "lot_number":
							lotNum = value;
							break;
						case "uselife_remaining":
							rFluidlevel = value;
							break;
						case "lot_expiration_date":
							try
							{
								Convert.ToDateTime(value);
							}
							catch
							{
							}
							break;
						}
					}
					jsonW.WriteObjectEnd();
					json_status = jsonW.ToString();
					command2.Parameters.AddWithValue("@inst_num", m_inst_num);
					command2.Parameters.AddWithValue("@cartridge_type", cartType);
					command2.Parameters.AddWithValue("@lot_num", lotNum);
					command2.Parameters.AddWithValue("@remaining_volume", rFluidlevel);
					command2.Parameters.AddWithValue("@status", json_status);
					command2.ExecuteNonQuery();
					command2.Parameters.Clear();
				}
				hasError = false;
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (JsonException ex2)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("JSON Exception " + ex2.Message, isXml: false, "ProcessCartridgeStatus " + json_status + " " + m_serial_id);
				}
			}
			catch (OdbcException ex3)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("DBException " + ex3.Message, isXml: false, "ProcessCartridgeStatus " + json_status + " " + m_serial_id);
				}
			}
			catch (Exception ex4)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Exception " + ex4.Message, isXml: false, "ProcessCartridgeStatus " + json_status + " " + m_serial_id);
				}
			}
			finally
			{
				CleanCommand(command);
				CleanCommand(command2);
			}
		}
		return hasError;
	}

	public void InitDMLLists(bool logging, DMLICPBase myDMLICPBase, string db_ver)
	{
		bIsListCreator = true;
		m_DMLICPBase = myDMLICPBase;
		m_NNBase.m_db_ver = db_ver;
		m_NNBase.m_bLogging = logging;
		m_TimeZoneInfo = TimeZoneInfo.Local;
	}

	public void InitDMLProtocol(Port.AsynchNetworkServer.ClientHandler parent, ref NetworkStream networkStream, int port_num, string portType, string from_inst_id, bool logging, DMLICPBase myDMLICPBase, string db_ver)
	{
		NovaNetTopics();
		NovaNetDirectives();
		if (from_inst_id.CompareTo("StatStrip") == 0)
		{
			bIsDeviceProtocol = true;
		}
		else
		{
			bIsDeviceProtocol = false;
		}
		m_parent = parent;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_DMLICPBase = myDMLICPBase;
		m_NNBase.m_bLogging = logging;
		m_NNBase.m_db_ver = db_ver;
		callbackWrite = OnWriteComplete;
		m_from_inst_id = from_inst_id;
		m_IP_Address = m_parent.m_InstrumentIP;
		m_b_loc_to_config_inst_type_column = ICPMGR.m_b_loc_to_config_inst_type_column;
		m_b_test_offsets_inst_class_column = ICPMGR.m_b_test_offsets_inst_class_column;
		m_b_test_offsets_inst_type_column = ICPMGR.m_b_test_offsets_inst_type_column;
		m_b_instruments_tests_inst_class_column = ICPMGR.m_b_instruments_tests_inst_class_column;
		m_b_loc_last_update_inst_class_column = ICPMGR.m_b_loc_last_update_inst_class_column;
		m_b_loc_last_update_inst_type_column = ICPMGR.m_b_loc_last_update_inst_type_column;
		m_NNBase.m_LogName = Guid.NewGuid().ToString("N");
		m_readbuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(32768);
		m_writebuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(32768);
		m_asyncwritebuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(1024);
	}

	private void Start()
	{
		if (!ICPMGR.m_bShuttingDown && m_ProtocolThread == null)
		{
			m_ProtocolThread = new Thread(ProtocolThread);
		}
		if (!ICPMGR.m_bShuttingDown && m_ProtocolThread.ThreadState == System.Threading.ThreadState.Unstarted)
		{
			try
			{
				m_ProtocolThread.Start();
			}
			catch
			{
			}
		}
	}

	private void OnCmTimedEvent()
	{
		try
		{
			cmTimer.Interval = 60000u;
			bool bgetout = false;
			lock (this)
			{
				bgetout = IsDeadOrDying() | m_ProtocolSending | m_TimerSending | m_isProcessing;
				if (bIsDeviceProtocol && !bgetout && m_isContinuous && !m_busy && !m_waiting)
				{
					m_TimerSending = true;
					m_busy = true;
				}
			}
			if (!bgetout)
			{
				if (bIsDeviceProtocol)
				{
					CalcContinuousMinuteCount();
					if (m_TimerSending)
					{
						bool bTimeSent = false;
						if (m_SetTimeSupported && m_TimeZoneInfo != null)
						{
							DateTime myNow = TimeZoneInfo.ConvertTime(DateTime.Now, m_TimeZoneInfo);
							if (m_LastNowInit)
							{
								long TimeDiff = myNow.Ticks - m_LastNow.Ticks;
								if (TimeDiff > 2 * OneMinute || TimeDiff < 0)
								{
									SendDateTime("Timer");
									bTimeSent = true;
								}
							}
							m_LastNow = myNow;
							m_LastNowInit = true;
						}
						if (m_kpaEnabled && !bTimeSent)
						{
							SendKeepAliveMessage();
						}
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
						ShutDown("Timeout", "Timer", bExit: false);
						return;
					}
				}
				if (bIsListCreator && m_NNBase.bDBAvailable && m_DMLICPBase.m_LocationList.GetNumUsedElements() > 0 && IsAliveAndWell())
				{
					CreateLists();
				}
			}
			if (WasAskedToStop())
			{
				ShutDown("Shutdown requested", "Timer", bExit: false);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
	}

	public void CreateLists()
	{
		m_ListsNow = DateTime.Now;
		m_ListsMinuteCount = m_ListsNow.Ticks / OneMinute;
		if (m_LastListsMinuteCount > m_ListsMinuteCount)
		{
			int adjustminutes = PatListFreq;
			m_LastListsMinuteCount = m_ListsMinuteCount - adjustminutes;
		}
		SendPatientList(out var _, out var _, out var _);
		m_LastListsMinuteCount = m_ListsMinuteCount;
		m_LastListsNow = m_ListsNow;
		m_DMLICPBase.m_bIsReady = true;
	}

	private long CalcContinuousMinuteCount()
	{
		lock (this)
		{
			m_LastContinuousMinuteCount = m_ContinuousMinuteCount;
			if (m_isContinuous)
			{
				m_ContinuousNow = DateTime.Now;
				m_ContinuousMinuteCount = m_ContinuousNow.Ticks / OneMinute - m_StartContinuousMinute;
			}
			else
			{
				m_ContinuousMinuteCount = 0L;
			}
		}
		return m_ContinuousMinuteCount;
	}

	private bool OnReadComplete()
	{
		string reason = "ReadError";
		bool bgetout = false;
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
							bOK = ICPMGR.m_ICPBytesBuffers.NetworkStreamRead(ref m_networkStream, ref m_readbuffer, ref iOneBufferDataLen, ref m_isShutDown, ref m_isShuttingDown, ref m_pleaseShutDown, ref m_stopping, ref bytesRead);
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
								long dIndex = bufferCount * 32768;
								Array.Copy(m_readbuffer, 0L, bigBuffer, dIndex, iOneBufferDataLen);
								int bigBufferSize = bigBuffer.Length;
								m_NNBase.log("start parser", isXml: false, "DML");
								bWholeMessageFound = ICPMGR.m_ICPBytesBuffers.GetFullDMLMessageFromBuffer(ref bigBuffer, ref iBegin, ref iEnd, bigBufferSize, ref iDataLen, ref bMessageBeginFound, ref bMessageEndFound, ref m_ReadString);
								ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref bigBuffer);
								bigBuffer = null;
							}
							else
							{
								bWholeMessageFound = ICPMGR.m_ICPBytesBuffers.GetFullDMLMessageFromBuffer(ref m_readbuffer, ref iBegin, ref iEnd, m_readbuffer.Length, ref iDataLen, ref bMessageBeginFound, ref bMessageEndFound, ref m_ReadString);
							}
							if (bWholeMessageFound)
							{
								CleanBufferList();
								m_message = m_ReadString;
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(m_message, isXml: true, (m_isPartial ? "..." : "") + m_portType);
								}
							}
							else
							{
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(bytesRead + " bytes read", isXml: false, m_portType);
								}
								if (iOneBufferDataLen + 1 > 32768)
								{
									byte[] addBuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(32768);
									Array.Copy(m_readbuffer, addBuffer, 32768);
									m_readBufferList.Add(addBuffer);
									Array.Clear(m_readbuffer, 0, 32768);
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
					lock (this)
					{
						if (!(bgetout = IsDeadOrDying()))
						{
							m_isProcessing = true;
						}
					}
					if (bgetout)
					{
						return false;
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
			reason = ((!m_pleaseShutDown) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			bOK = false;
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				bool bret = WasAskedToStop();
				m_NNBase.log(bret ? "Was asked to stop" : ex3.Message, isXml: false, "DML");
			}
			if (IsAliveAndWell())
			{
				handleException(ex3, "Reading message(s)", "OnReadComplete", "Protocol");
			}
			bOK = false;
		}
		return bOK;
	}

	private void OnWriteComplete(IAsyncResult ar)
	{
		string reason = "";
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
			handleThreadAbortException("OnWriteComplete");
		}
		catch (IOException ex2)
		{
			reason = ((!m_pleaseShutDown && !m_stopping) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			ShutDown(reason, "OnWriteComplete", bExit: false);
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				bool bret = WasAskedToStop();
				m_NNBase.log(bret ? "Was asked to stop" : ex3.Message, isXml: false, "DML");
			}
			if (IsAliveAndWell())
			{
				handleException(ex3, "writing message(s)", "OnWriteComplete", "OnWriteComplete");
			}
		}
	}

	private void ProcessMessage()
	{
		lock (this)
		{
			m_kpaTimeoutCount = 0;
			m_waiting = false;
		}
		try
		{
			m_doc = new XmlDocument();
			try
			{
				m_doc.LoadXml(m_message);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (XmlException ex2)
			{
				string details = ex2.Message.ToString() + " at line: " + ex2.LineNumber + " " + ex2.StackTrace.ToString();
				m_NNBase.ReportErrorDB("XML Exception " + ex2.GetType().ToString(), "C", "loading xml message", "ProcessMessage", details);
				m_message = FixXMLMessage(m_message);
				m_doc.LoadXml(m_message);
			}
			m_message = "";
			XmlNodeReader reader = new XmlNodeReader(m_doc);
			reader.Read();
			s_msg_type_rcvd = reader.LocalName;
			switch (s_msg_type_rcvd)
			{
			case "ACK.R01":
				if (m_isPartial)
				{
					m_isPartial = false;
				}
				break;
			case "DST.R01":
				ProcessDeviceStatus(reader);
				break;
			case "DTV.NOVA_REQ.R02":
				m_DMLState = 40;
				ProcessQuery();
				break;
			case "END.R01":
				SendAcknowledgeMessage(m_control_id, isError: false);
				ShutDown("END.R01", "Protocol", bExit: true);
				break;
			case "EOT.R01":
				switch (m_DMLState)
				{
				case 4:
					m_DMLState = 5;
					break;
				case 6:
					m_DMLState = 7;
					break;
				case 5:
					break;
				}
				break;
			case "ESC.R01":
				ShutDown("ESC.R01", "Protocol", bExit: true);
				break;
			case "EVS.R01":
				ProcessEvents(reader);
				break;
			case "HEL.R01":
				ProcessHello(reader);
				break;
			case "KPA.R01":
				ProcessKeepAlive(reader);
				break;
			case "OBS.R01":
			case "OBS.R02":
				ProcessObservation(s_msg_type_rcvd);
				break;
			case "NOVA.ANALYZER_STATE":
			case "NOVA.CARTRIDGE_STATUS":
			case "NOVA.TEST_STATUS":
				try
				{
					if (m_SupportedTopic[s_msg_type_rcvd])
					{
						m_DMLState = 39;
						ProcessSystemStatus(s_msg_type_rcvd);
					}
					break;
				}
				catch
				{
					string ErrMsg2 = "Unsupported message type " + s_msg_type_rcvd;
					m_NNBase.ReportErrorDB(ErrMsg2, "E", "processing message", "ProcessMessage", "");
					SendEscape(ErrMsg2);
					ShutDown(ErrMsg2, "Protocol", bExit: true);
					break;
				}
			default:
			{
				string ErrMsg = "Unexepected message type " + s_msg_type_rcvd;
				m_NNBase.ReportErrorDB(ErrMsg, "E", "processing message", "ProcessMessage", "");
				SendEscape(ErrMsg);
				ShutDown(ErrMsg, "Protocol", bExit: true);
				break;
			}
			case "DTV.R01":
			case "DTV.R02":
				break;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (XmlException e)
		{
			m_NNBase.ForceLogging("XMLException");
			if (m_message.Length > 0)
			{
				m_NNBase.log(m_message, isXml: false, "ProcessMessage");
			}
			else
			{
				m_NNBase.log(m_doc.OuterXml, isXml: true, "ProcessMessage");
			}
			handleXMLException(e, "processing " + s_msg_type_rcvd + " message", "ProcessMessage");
		}
		catch (Exception e2)
		{
			m_NNBase.ForceLogging("Exception");
			if (m_message.Length > 0)
			{
				m_NNBase.log(m_message, isXml: false, "ProcessMessage");
			}
			else
			{
				m_NNBase.log(m_doc.OuterXml, isXml: true, "ProcessMessage");
			}
			handleException(e2, "processing " + s_msg_type_rcvd + " message", "ProcessMessage", "Protocol");
		}
	}

	private string FixXMLMessage(string m_message)
	{
		char[] anyOf = new char[256];
		int i = 0;
		int j = 0;
		for (i = 0; i < 9; i++)
		{
			anyOf[j++] = (char)i;
		}
		for (i = 11; i < 13; i++)
		{
			anyOf[j++] = (char)i;
		}
		for (i = 14; i < 32; i++)
		{
			anyOf[j++] = (char)i;
		}
		for (i = 127; i < 133; i++)
		{
			anyOf[j++] = (char)i;
		}
		for (i = 134; i < 160; i++)
		{
			anyOf[j++] = (char)i;
		}
		string message = m_message;
		i = 0;
		j = message.IndexOfAny(anyOf, i);
		while (j >= 0 && j < message.Length)
		{
			while (j >= 0)
			{
				char[] charfound = message.Substring(j, 1).ToCharArray();
				message = message.Replace(charfound[0], '_');
				i = j + 1;
				j = message.IndexOfAny(anyOf, i);
			}
		}
		return message;
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

	private bool SendAcknowledgeMessage(string control_id, bool isError)
	{
		bool retVal = true;
		try
		{
			string sAck = "<ACK.R01>" + GenDMLHeader("Protocol") + "<ACK><ACK.type_cd V=\"A" + (isError ? "E" : "A") + "\"/><ACK.ack_control_id V=\"" + control_id + "\"/></ACK></ACK.R01>";
			SendString(sAck, isPartial: false, trunc: false);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending acknowledge message", "SendAcknowledgeMessage", "Protocol");
		}
		return retVal;
	}

	private bool SendKeepAliveMessage()
	{
		bool retVal = true;
		try
		{
			string sKpa = "<KPA.R01>" + GenDMLHeader("Timer") + "</KPA.R01>";
			m_waiting = true;
			SendString(sKpa);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Timer");
		}
		catch (Exception e)
		{
			handleException(e, "sending keep alive message", "SendKeepAliveMessage", "Timer");
		}
		return retVal;
	}

	private void StepProtocolState()
	{
		lock (m_dmlStateLock)
		{
			try
			{
				int i = 0;
				switch (m_DMLState)
				{
				case 0:
				case 2:
					m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
					m_NNBase.ReportErrorDB("Unexepected Protocol Step", "C", "awaiting hello", "StepProtocolState", "");
					break;
				case 1:
					m_DMLState = 3;
					break;
				case 3:
					m_NNBase.CommAudit(10, "Connect", m_new_observations_qty);
					m_DMLState = 4;
					if (Convert.ToInt32(m_new_observations_qty) > 0)
					{
						RequestFromDevice("ROBS");
						break;
					}
					goto case 5;
				case 5:
					if (Convert.ToInt32(m_new_observations_qty) > 0)
					{
						m_NNBase.CommAudit(1, "Results received", m_new_observations_qty);
					}
					m_DMLState = 6;
					if (Convert.ToInt32(m_new_events_qty) > 0)
					{
						RequestFromDevice("RDEV");
						break;
					}
					goto case 7;
				case 7:
					if (Convert.ToInt32(m_new_events_qty) > 0)
					{
						m_NNBase.CommAudit(2, "Device Events Received", m_new_events_qty);
					}
					m_DMLState = 8;
					if (m_SetTimeSupported)
					{
						SendDateTime("Protocol");
						break;
					}
					goto case 8;
				case 8:
				case 9:
					m_DMLState = 11;
					lock (this)
					{
						m_kpaEnabled = false;
						m_kpaTimeoutCount = 0;
						m_busy = true;
					}
					if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
					{
						OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
					}
					if (SendSetup())
					{
						m_NNBase.CommAudit(8, "Setup sent", "");
						break;
					}
					goto case 12;
				case 10:
				case 11:
					m_DMLState = 12;
					SendEotMessage("NOVA.STATSTRIP.SETUP");
					goto case 12;
				case 12:
					m_DMLState = 16;
					lock (this)
					{
						m_kpaEnabled = false;
						m_kpaTimeoutCount = 0;
						m_busy = true;
					}
					if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
					{
						OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
					}
					if (SendWifiSetup())
					{
						m_NNBase.CommAudit(12, "WIFI Setup sent", "");
						break;
					}
					goto case 17;
				case 15:
				case 16:
					m_DMLState = 17;
					SendEotMessage("NOVA.WIFI_SETUP");
					goto case 17;
				case 17:
					m_DMLState = 19;
					lock (this)
					{
						m_kpaEnabled = false;
						m_kpaTimeoutCount = 0;
						m_busy = true;
					}
					if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
					{
						OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
					}
					if (SendWifiCert())
					{
						m_NNBase.CommAudit(13, "WIFI certificate sent", "");
						break;
					}
					goto case 20;
				case 18:
				case 19:
					m_DMLState = 20;
					SendEotMessage("NOVA.WIFI_CERT");
					goto case 20;
				case 20:
					m_DMLState = 22;
					if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
					{
						OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
					}
					if (SendLocationList())
					{
						m_NNBase.CommAudit(5, "Location list sent", "");
						break;
					}
					goto case 23;
				case 21:
				case 22:
					m_DMLState = 23;
					SendEotMessage("NOVA.LOC");
					goto case 23;
				case 23:
				{
					m_DMLState = 25;
					i = SendOperatorList(out var isIncremantl, out var opCount, out var opDcount);
					if (i != 0)
					{
						if (isIncremantl)
						{
							m_NNBase.CommAudit(3, "Incremental Operator List Sent(" + opDcount + "," + opCount + ")", m_last_incremental);
						}
						else
						{
							m_NNBase.CommAudit(3, "Complete Operator List Sent(" + opCount + ")", m_last_incremental);
						}
						if (i != -1)
						{
							break;
						}
						goto case 24;
					}
					goto case 26;
				}
				case 24:
				case 25:
					m_DMLState = 26;
					SendEotMessage("OPL");
					goto case 26;
				case 26:
				{
					m_DMLState = 28;
					i = SendPatientList(out var isIncremental, out var patCount, out var patDCount);
					if (i != 0)
					{
						if (isIncremental)
						{
							m_NNBase.CommAudit(4, "Incremental Patient list sent (" + patDCount + "," + patCount + ")", m_last_incremental);
						}
						else
						{
							m_NNBase.CommAudit(4, "Complete Patient list sent (" + patCount + ")", m_last_incremental);
						}
						if (i != -1)
						{
							break;
						}
						goto case 27;
					}
					goto case 29;
				}
				case 27:
				case 28:
					m_DMLState = 29;
					SendEotMessage("PTL");
					goto case 29;
				case 29:
					m_DMLState = 31;
					i = SendPhysicianList();
					if (i != 0)
					{
						m_NNBase.CommAudit(6, "Physician List Sent", m_last_incremental);
						if (i != -1)
						{
							break;
						}
						goto case 30;
					}
					goto case 32;
				case 30:
				case 31:
					m_DMLState = 32;
					SendEotMessage("NOVA.PHYS");
					goto case 32;
				case 32:
					m_DMLState = 34;
					if (SendFirmware())
					{
						m_NNBase.CommAudit(9, "Firmware update sent", "");
						break;
					}
					goto case 35;
				case 33:
				case 34:
					m_DMLState = 45;
					m_last_eot_update_time = DateTime.Now;
					SendEotMessage("NOVA.FRM");
					goto case 35;
				case 35:
					m_DMLState = 37;
					if (SendReagents())
					{
						m_NNBase.CommAudit(7, "Reagent list sent", "");
						break;
					}
					goto case 38;
				case 36:
				case 37:
					m_DMLState = 38;
					SendEotMessage("NOVA.REAG");
					goto case 38;
				case 38:
					if (m_isContinuous)
					{
						myRuntimeDBConnection.Close();
						goto case 42;
					}
					if (m_disconnect_active == "T" && m_inst_class.CompareTo("StatStrip") == 0 && m_SupportedTopic["NOVA.STATSTRIP.SETUP"])
					{
						m_DMLState = 44;
						SendTerminate("NRM", "RECONNECT:" + m_disconnect_minutes);
					}
					else if (m_ContinuousSupported && m_loc_num.Length > 0)
					{
						m_DMLState = 42;
						SendContinuous();
						myRuntimeDBConnection.Close();
						m_ContinuousMinuteCount = 0L;
						m_LastContinuousMinuteCount = 0L;
						m_ContinuousNow = DateTime.Now;
						m_StartContinuousMinute = m_ContinuousNow.Ticks / OneMinute;
						LastContinuousOperatorSend = 0L;
						LastContinuousPatientSend = 0L;
					}
					else
					{
						m_DMLState = 44;
						SendTerminate("NRM", "");
					}
					break;
				case 42:
				case 43:
					m_DMLState = 8;
					m_isContinuous = true;
					m_kpaTimeoutCount = 0;
					m_kpaEnabled = true;
					m_busy = false;
					break;
				case 44:
					ShutDown("Terminate", "Protocol", bExit: true);
					break;
				case 39:
					m_DMLState = 43;
					break;
				case 4:
				case 6:
				case 13:
				case 14:
				case 40:
				case 41:
				case 45:
					break;
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (Exception e)
			{
				handleException(e, "stepping to the next protocol state", "StepProtocolState", "Protocol");
			}
		}
	}

	private bool SendEotMessage(string topic)
	{
		bool retVal = true;
		try
		{
			string sAck = "<EOT.R01>" + GenDMLHeader("Protocol") + "<EOT><EOT.topic_cd V=\"" + topic + "\"/><EOT.update_dttm V=\"" + DateTime2DML(m_last_eot_update_time) + "\"/></EOT></EOT.R01>";
			SendString(sAck, isPartial: false, trunc: false);
			switch (topic)
			{
			case "NOVA.STATSTRIP.SETUP":
				m_setup_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "NOVA.WIFI_SETUP":
				m_wifi_setup_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "NOVA.WIFI_CERT":
				m_cert_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "NOVA.LOC":
				m_loc_list_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "OPL":
				m_operators_update_dttm = DateTime2DML(m_last_eot_update_time);
				UpdateLastDload("last_op_dload");
				break;
			case "PTL":
				m_patients_update_dttm = DateTime2DML(m_last_eot_update_time);
				device_patient_update_datetime = m_last_eot_update_time;
				UpdateLastDload("last_pat_dload");
				break;
			case "NOVA.PHYS":
				m_phys_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "NOVA.REAG":
				m_reag_update_dttm = DateTime2DML(m_last_eot_update_time);
				break;
			case "NOVA.FRM":
				break;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending end of topic message", "SendEOTMessage", "Protocol");
		}
		return retVal;
	}

	private void UpdateLastDload(string last_dload_field)
	{
		try
		{
			myRuntimeDBWriteCommand.CommandText = $"update DBA.instruments set {last_dload_field} = now(*) where inst_num = '{m_inst_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBWriteCommand.ExecuteNonQuery();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "updating last download", "UpdateLastDload", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "updating last download", "UpdateLastDload", "Protocol");
		}
	}

	private bool SendTerminate(string reason, string note)
	{
		bool retVal = true;
		try
		{
			string sAck = "<END.R01>" + GenDMLHeader("Protocol") + "<TRM><TRM.reason_cd V=\"" + reason + "\"/>" + ((note.Length > 0) ? ("<TRM.note_txt V=\"" + note + "\"/>") : "") + "</TRM></END.R01>";
			m_waiting = true;
			SendString(sAck, isPartial: false, trunc: false);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending terminate message", "SendTerminate", "Protocol");
		}
		return retVal;
	}

	private bool SendEscape(string note)
	{
		bool retVal = true;
		try
		{
			string sAck = "<ESC.R01>" + GenDMLHeader("Protocol") + "<ESC><ESC.esc_control_id V=\"" + m_control_id + "\"/><ESC.detail_cd V=\"OTH\"/>" + ((note.Length > 0) ? ("<ESC.note_txt V=\"" + note + "\"/>") : "") + "</ESC></ESC.R01>";
			SendString(sAck, isPartial: false, trunc: false);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending escape message", "SendEscape", "Protocol");
		}
		return retVal;
	}

	private bool RequestFromDevice(string request_cd)
	{
		bool retVal = true;
		try
		{
			string sReq = "<REQ.R01>" + GenDMLHeader("Protocol") + "<REQ><REQ.request_cd V=\"" + request_cd + "\"/></REQ></REQ.R01>";
			m_waiting = true;
			SendString(sReq, isPartial: false, trunc: false);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending request from device message", "RequestFromDevice", "Protocol");
		}
		return retVal;
	}

	private string GenDMLHeader(string whoFrom)
	{
		string sHeader = "";
		try
		{
			sHeader = "<HDR><HDR.control_id V=\"" + m_imsgid + "\" /><HDR.version_id V=\"POCT1\" /><HDR.creation_dttm V=\"" + DateTime2DML(DateTime.Now) + "\" /></HDR>";
			m_imsgid++;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (Exception e)
		{
			handleException(e, "formatting a general DML header", "GenDMLHeader", whoFrom);
		}
		return sHeader;
	}

	private void AddDMLHeader(ref XmlWriter writeHeader)
	{
		writeHeader.WriteStartElement("HDR");
		writeHeader.WriteStartElement("HDR.control_id");
		writeHeader.WriteAttributeString("V", m_imsgid.ToString());
		m_imsgid++;
		writeHeader.WriteEndElement();
		writeHeader.WriteStartElement("HDR.version_id");
		writeHeader.WriteAttributeString("V", "POCT1");
		writeHeader.WriteEndElement();
		writeHeader.WriteStartElement("HDR.creation_dttm");
		writeHeader.WriteAttributeString("V", DateTime2DML(DateTime.Now));
		writeHeader.WriteEndElement();
		writeHeader.WriteEndElement();
	}

	private void ProcessHello(XmlNodeReader reader)
	{
		try
		{
			string txt = "";
			char[] delim = new char[1] { '^' };
			m_location = "Unassigned";
			m_facility = "Unassigned";
			m_loc_num = "";
			m_last_loc_used = false;
			m_disconnect_active = "";
			m_disconnect_minutes = "";
			m_inst_class = "";
			m_inst_type = "";
			m_device_name = "";
			m_sw_version = "";
			m_supportMTE = false;
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
				case "HDR.control_id":
					m_control_id = reader.GetAttribute("V");
					break;
				case "DEV.vendor_id":
					m_vendor_id = reader.GetAttribute("V");
					break;
				case "DEV.device_id":
					m_serial_id = reader.GetAttribute("V");
					m_parent.m_InstrumentID = m_serial_id;
					txt = reader.ReadString();
					if (txt.Length > 0)
					{
						string[] locations = txt.Split(delim, 2);
						m_facility = locations[0];
						if (locations.Length > 1)
						{
							m_location = locations[1];
						}
					}
					break;
				case "DEV.facility":
					m_facility = reader.GetAttribute("V");
					break;
				case "DEV.model_id":
					m_inst_type = reader.GetAttribute("V");
					break;
				case "DEV.location":
					m_location = reader.GetAttribute("V");
					break;
				case "DEV.serial_id":
					m_serial_id = reader.GetAttribute("V");
					m_parent.m_InstrumentID = m_serial_id;
					break;
				case "DEV.manufacturer_name":
					m_manufacturer_name = reader.GetAttribute("V");
					break;
				case "DEV.device_name":
					m_device_name = reader.GetAttribute("V");
					if (m_inst_type.CompareTo("pHOx Ultra") == 0 && m_serial_id.Length < 1)
					{
						m_serial_id = m_device_name + "_PATOP";
					}
					break;
				case "DEV.hw_version":
					m_hw_version = reader.GetAttribute("V");
					break;
				case "DEV.sw_version":
				{
					m_sw_version = reader.GetAttribute("V");
					m_sw_lang_version = reader.ReadString();
					if (m_sw_lang_version.Length < m_sw_version.Length)
					{
						m_sw_lang_version = m_sw_version + "_en";
					}
					int il = m_sw_lang_version.LastIndexOf("_");
					if (il >= 0)
					{
						m_language_short = (m_language_long = m_sw_lang_version.Substring(il + 1));
						int isl = m_language_long.LastIndexOf("-");
						if (isl > 0)
						{
							m_language_short = m_language_long.Substring(0, isl);
						}
					}
					else
					{
						m_language_short = (m_language_long = "en");
					}
					break;
				}
				case "DSC.max_message_sz":
					m_max_message_sz = reader.GetAttribute("V");
					if (m_max_message_sz.Length == 0)
					{
						m_max_message_sz = "4096";
					}
					break;
				case "DSC.topics_supported_cd":
				{
					string thisTopic = reader.GetAttribute("V");
					if (!string.IsNullOrEmpty(thisTopic) && m_SupportedTopic.ContainsKey(thisTopic))
					{
						m_SupportedTopic[thisTopic] = true;
					}
					break;
				}
				case "DSC.directives_supported_cd":
				{
					string thisDirective = reader.GetAttribute("V");
					if (!string.IsNullOrEmpty(thisDirective) && m_SupportedDirective.ContainsKey(thisDirective))
					{
						m_SupportedDirective[thisDirective] = true;
					}
					break;
				}
				case "DSC.max_op_list_sz":
				{
					string sOpListSize = reader.GetAttribute("V");
					if (!string.IsNullOrEmpty(sOpListSize) && isNumeric(sOpListSize, NumberStyles.Integer))
					{
						m_maxDownloadOperator = Convert.ToInt32(sOpListSize);
					}
					break;
				}
				case "DSC.max_pat_list_sz":
				{
					string sPatListSize = reader.GetAttribute("V");
					if (!string.IsNullOrEmpty(sPatListSize) && isNumeric(sPatListSize, NumberStyles.Integer))
					{
						m_maxVisitLocations = Convert.ToInt32(sPatListSize);
					}
					break;
				}
				case "DCP.vendor_specific":
				{
					string vendordata = reader.ReadString();
					char[] delims = new char[2] { '^', '=' };
					string[] vendordataparts = vendordata.Split(delims);
					for (int i = 0; i < vendordataparts.Length - 1; i += 2)
					{
						string myvar = vendordataparts[i];
						string myval = vendordataparts[i + 1];
						if (string.Compare(myvar, "max_op_list_sz", ignoreCase: true) == 0)
						{
							if (!string.IsNullOrEmpty(myval) && isNumeric(myval, NumberStyles.Integer))
							{
								m_maxDownloadOperator = Convert.ToInt32(myval);
							}
						}
						else if (string.Compare(myvar, "max_pat_list_sz", ignoreCase: true) == 0)
						{
							if (!string.IsNullOrEmpty(myval) && isNumeric(myval, NumberStyles.Integer))
							{
								m_maxVisitLocations = Convert.ToInt32(myval);
							}
						}
						else if (string.Compare(myvar, "MAC_Address", ignoreCase: true) == 0)
						{
							if (!string.IsNullOrEmpty(myval) && myval.Length == 17)
							{
								m_MAC_Address = myval;
							}
						}
						else if (string.Compare(myvar, "Wifi_MAC_Address", ignoreCase: true) == 0 && !string.IsNullOrEmpty(myval) && myval.Length == 17)
						{
							m_Wifi_MAC_Address = myval;
						}
					}
					break;
				}
				default:
					reader.GetAttribute("V");
					txt = reader.Value;
					break;
				case "DCP.application_timeout":
					break;
				}
			}
			m_NNBase.m_IFInstID = m_serial_id;
			if (m_NNBase.m_bLogging)
			{
				m_NNBase.m_LogName = m_serial_id;
				m_NNBase.m_IFInstID = m_serial_id;
				if (!m_NNBase.m_isLogging)
				{
					m_NNBase.StartLogging();
				}
				else
				{
					m_NNBase.RenameLogFile();
				}
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("max download OP " + m_maxDownloadOperator, isXml: false, "DML");
					m_NNBase.log("max download PAT " + m_maxVisitLocations, isXml: false, "DML");
				}
			}
			if (m_serial_id.Length > 0)
			{
				CheckDMLSessionList();
			}
			m_inst_class = "";
			m_control_internal_external = "";
			if (m_inst_type.Length > 0)
			{
				myRuntimeDBReadCommand.CommandText = $"select inst_class from DBA.instrument_types where inst_type = '{m_inst_type}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					m_inst_class = myRuntimeDBReadReader.GetString(0);
				}
				myRuntimeDBReadReader.Close();
				if (m_inst_class.Length <= 0)
				{
					string ErrMsg = "Unknown device " + m_inst_type;
					m_NNBase.ReportErrorDB(ErrMsg, "E", "processing Hello", "ProcessHello", "");
					SendEscape(ErrMsg);
					ShutDown(ErrMsg, "Protocol", bExit: true);
					return;
				}
				if (Comp.Compare(m_inst_class, "StatStrip", CompOpt) == 0)
				{
					m_control_internal_external = "External";
				}
			}
			LookupLocNum();
			m_was_unassigned = (m_loc_num.Length == 0) | (m_location == "Unassigned") | (m_facility == "Unassigned");
			if (!m_was_unassigned)
			{
				LookupTimeZone(m_facility, ref m_TimeZoneName, ref m_TimeZoneInfo);
			}
			else
			{
				m_TimeZoneName = TimeZone.CurrentTimeZone.StandardName;
				m_TimeZoneInfo = TimeZoneInfo.Local;
			}
			m_inst_num = "";
			string myLocNum = "";
			if (m_inst_class.Length > 0)
			{
				myRuntimeDBReadCommand.CommandText = "select inst_num, inst_type, inst_name, sw_version, loc_num from DBA.instruments where serial_no = '" + m_serial_id + "'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					m_inst_num = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					if (m_inst_type.Length == 0)
					{
						m_inst_type = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
					}
					if (!myRuntimeDBReadReader.IsDBNull(2))
					{
						myRuntimeDBReadReader.GetString(2);
					}
					if (!myRuntimeDBReadReader.IsDBNull(3))
					{
						myRuntimeDBReadReader.GetString(3);
					}
					if (m_loc_num.Length == 0)
					{
						myLocNum = (myRuntimeDBReadReader.IsDBNull(4) ? "" : myRuntimeDBReadReader.GetString(4));
						m_last_loc_used = myLocNum.Length > 0;
						if (m_lookup_location)
						{
							m_loc_num = myLocNum;
						}
					}
				}
				myRuntimeDBReadReader.Close();
			}
			if (m_was_unassigned && m_last_loc_used)
			{
				string myFacility = "";
				string myLocation = "";
				myRuntimeDBReadCommand.CommandText = $"select l.loc_name, p.loc_name from DBA.inst_locations l join DBA.inst_locations p on p.loc_num = l.parent where l.loc_num = '{myLocNum}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					myLocation = myRuntimeDBReadReader.GetString(0);
					myFacility = myRuntimeDBReadReader.GetString(1);
				}
				myRuntimeDBReadReader.Close();
				if (myFacility.Length > 0)
				{
					LookupTimeZone(myFacility, ref m_TimeZoneName, ref m_TimeZoneInfo);
				}
				if (m_lookup_location)
				{
					m_facility = myFacility;
					m_location = myLocation;
				}
			}
			else if (m_was_unassigned && m_force_meter_default)
			{
				myRuntimeDBReadCommand.CommandText = $"select l.loc_num, l.loc_name, p.loc_name from DBA.inst_locations l join DBA.inst_locations p on p.loc_num = l.parent where l.is_default = 'T' and p.is_default = 'T'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					m_loc_num = (myRuntimeDBReadReader.IsDBNull(0) ? m_loc_num : myRuntimeDBReadReader.GetString(0));
					m_location = (myRuntimeDBReadReader.IsDBNull(1) ? m_location : myRuntimeDBReadReader.GetString(1));
					m_facility = (myRuntimeDBReadReader.IsDBNull(2) ? m_facility : myRuntimeDBReadReader.GetString(2));
				}
				myRuntimeDBReadReader.Close();
			}
			if (m_loc_num.Length > 0)
			{
				myRuntimeDBReadCommand.CommandText = "select active, minutes from DBA.discon_by_loc_profiles where loc_num = '" + m_loc_num + "'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					m_disconnect_active = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					m_disconnect_minutes = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
				}
				myRuntimeDBReadReader.Close();
			}
			m_loc_def_pat_id = "";
			if (m_loc_num.Length > 0)
			{
				myRuntimeDBReadCommand.CommandText = $"SELECT _value FROM DBA.config_data c join DBA.loc_to_config l2c on c.config_num = l2c.config_num where _key = 'PatIdTypeCd*V' and l2c.loc_num = '{m_loc_num}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				if (myRuntimeDBReadReader.Read())
				{
					m_loc_def_pat_id = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
				}
				myRuntimeDBReadReader.Close();
			}
			if (m_inst_num.Length == 0 && m_inst_class.Length > 0)
			{
				string patients_update_date = device_patient_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_inst_num = Guid.NewGuid().ToString("N");
				string sql = "insert into DBA.instruments ";
				string columnlist = "(inst_num, inst_type, inst_name, inst_id, serial_no, loc_num, port_num, inst_active, sw_version, inst_condition, last_connect_dttm, last_pat_dload, ip_address, computer_name, mac_address, wifi_mac_address";
				string valuelist = $" values ('{m_inst_num}','{m_inst_type}','{m_device_name}','{m_serial_id}','{m_serial_id}','{m_loc_num}','{m_port_num.ToString()}','1','{m_sw_version}','R',Now(*),'{patients_update_date}','{m_IP_Address.ToString()}','{m_NNBase.GetHostName()}','{m_MAC_Address}','{m_Wifi_MAC_Address}'";
				columnlist += ")";
				valuelist += ")";
				sql = sql + columnlist + valuelist;
				myRuntimeDBWriteCommand.CommandText = sql;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBWriteCommand.ExecuteNonQuery();
			}
			else
			{
				myRuntimeDBWriteCommand.CommandText = "update DBA.instruments set ";
				myRuntimeDBWriteCommand.CommandText += string.Format("inst_type = '{1}', inst_name = '{2}', inst_id = '{3}', serial_no = '{4}', loc_num = '{5}', port_num = '{6}', inst_active = '1', sw_version = '{7}', inst_condition = 'R', last_connect_dttm = now(*), ip_address = '{8}', computer_name = '{9}', mac_address = '{10}', wifi_mac_address = '{11}'where inst_num = '{0}'", m_inst_num, m_inst_type, m_device_name, m_serial_id, m_serial_id, m_loc_num, m_port_num.ToString(), m_sw_version, m_IP_Address.ToString(), m_NNBase.GetHostName(), m_MAC_Address, m_Wifi_MAC_Address);
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBWriteCommand.ExecuteNonQuery();
			}
			myRuntimeDBWriteCommand.CommandText = $"update DBA.communications set Port_Active= '0', Used = 'T', port_num = '{m_port_num}' where Computer_Name = '{m_NNBase.GetHostName()}' and Instrument_ID = '{m_serial_id}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			if (myRuntimeDBWriteCommand.ExecuteNonQuery() < 1 && m_portType == "StatStrip")
			{
				AddPortToDb();
			}
			m_SetTimeSupported = m_SupportedDirective["SET_TIME"];
			m_ContinuousSupported = m_SupportedDirective["START_CONTINUOUS"];
			m_OprListIncrSupported = m_SupportedTopic["OP_LST_I"];
			if (m_inst_class.CompareTo("StatStrip") == 0 && m_OprListIncrSupported)
			{
				m_SupportedTopic["OP_LST"] = true;
			}
			m_OprListFullSupported = m_SupportedTopic["OP_LST"];
			m_PatListIncrSupported = m_SupportedTopic["PT_LST_I"];
			if (m_inst_class.CompareTo("StatStrip") == 0 && m_PatListIncrSupported)
			{
				m_SupportedTopic["PT_LST"] = true;
			}
			m_PatListFullSupported = m_SupportedTopic["PT_LST"];
			m_PhysListIncrSupported = m_SupportedTopic["NOVA.PHYS_I"];
			m_PhysListFullSupported = m_SupportedTopic["NOVA.PHYS"];
			m_LocListSupported = m_SupportedTopic["NOVA.LOC"];
			m_SetupSupported = m_SupportedTopic["NOVA.STATSTRIP.SETUP"];
			m_tdomeSetupSupported = m_SupportedTopic["NOVA.PHOENIX.SETUP"];
			m_bgaSetupSupported = m_SupportedTopic["NOVA.BLOODGAS.SETUP"];
			m_WifiSetupSupported = m_SupportedTopic["NOVA.WIFI_SETUP"];
			m_WifiCertSupported = m_SupportedTopic["NOVA.WIFI_CERT"];
			m_ReagSupported = m_SupportedTopic["NOVA.REAG"];
			m_supportMTE = m_SupportedTopic["NOVA.MANUAL_TEST"];
			m_FirmSupported = m_SupportedTopic["NOVA.FRM"];
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			m_NNBase.ForceLogging("DBException");
			m_NNBase.log(m_doc.OuterXml, isXml: true, "ProcessHello");
			handleDBException(e, "processing hello message", "ProcessHello", "Protocol");
		}
		catch (Exception e2)
		{
			m_NNBase.ForceLogging("Exception");
			m_NNBase.log(m_doc.OuterXml, isXml: true, "ProcessHello");
			handleException(e2, "processing hello message", "ProcessHello", "Protocol");
		}
		m_isContinuous = false;
		m_DMLState = 1;
		m_waiting = true;
		SendAcknowledgeMessage(m_control_id, isError: false);
	}

	private bool isNumeric(string val, NumberStyles NumberStyle)
	{
		double result;
		return double.TryParse(val, NumberStyle, CultureInfo.CurrentCulture, out result);
	}

	private void ProcessEvents(XmlNodeReader reader)
	{
		try
		{
			string description = "";
			string event_dttm = "";
			string severity_cd = "";
			string operator_id = "";
			string event_type = "";
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
				case "HDR.control_id":
					m_control_id = reader.GetAttribute("V");
					break;
				case "EVT":
					description = "";
					event_dttm = "";
					severity_cd = "";
					operator_id = "";
					event_type = "";
					break;
				case "EVT.description":
					description = reader.GetAttribute("V");
					event_type = reader.ReadString();
					break;
				case "EVT.event_dttm":
					event_dttm = reader.GetAttribute("V");
					break;
				case "EVT.severity_cd":
					severity_cd = reader.GetAttribute("V");
					break;
				case "OPR.operator_id":
					operator_id = reader.GetAttribute("V");
					if (description == "OP MSG READ")
					{
						if (event_dttm.Length <= 10 || event_type.Length <= 10)
						{
							break;
						}
						string sMessageReadSystemTime = DMLToSystemDateTime(event_dttm).ToString("yyyy-MM-dd HH:mm:ss");
						string sMessageCreatedSystemTime = DMLToSystemDateTime(event_type).ToString("yyyy-MM-dd HH:mm:ss.ff");
						myRuntimeDBWriteCommand.CommandText = string.Format("update DBA.operator_message set msg_read_dttm = '{0}' where msg_create_dttm = '{1}' and operator_num in (select operator_num from DBA.operators where operator_id = '{2}')", sMessageReadSystemTime, sMessageCreatedSystemTime, operator_id.Replace("'", "''"));
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBWriteCommand.ExecuteNonQuery();
						myRuntimeDBWriteCommand.CommandText = string.Format("update DBA.operator_message set current_msg = if msg_create_dttm in (select first msg_create_dttm from DBA.operator_message where operator_num in (select operator_num from DBA.operators where operator_id = '{0}' and msg_read_dttm is null) order by msg_priority desc, msg_create_dttm asc) then 'T' else 'F' endif where operator_num in (select operator_num from DBA.operators where operator_id = '{0}')", operator_id.Replace("'", "''"));
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						if (myRuntimeDBWriteCommand.ExecuteNonQuery() == 0)
						{
							myRuntimeDBWriteCommand.CommandText = string.Format("update DBA.operator_message set current_msg = 'T' where operator_num in (select operator_num from DBA.operators where operator_id = '{0}') and msg_create_dttm in (select first msg_create_dttm from DBA.operator_message where operator_num in (select operator_num from DBA.operators where operator_id = '{0}'))", operator_id.Replace("'", "''"));
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBWriteCommand.ExecuteNonQuery();
						}
						myRuntimeDBWriteCommand.CommandText = string.Format("update DBA.operators set datetime_stamp = now(*) where operator_num in (select operator_num from DBA.operators where operator_id = '{0}')", operator_id.Replace("'", "''"));
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBWriteCommand.ExecuteNonQuery();
						myRuntimeDBWriteCommand.CommandText = $"update DBA.loc_last_update set last_update_time = now(*) where loc_num = '{m_loc_num}' and data_type = 'OPERATORS'";
						if (m_b_loc_last_update_inst_class_column)
						{
							myRuntimeDBWriteCommand.CommandText += $" and inst_class = '{m_inst_class}'";
						}
						else if (m_b_loc_last_update_inst_type_column)
						{
							myRuntimeDBWriteCommand.CommandText += $" and inst_type = '{m_inst_type}'";
						}
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBWriteCommand.ExecuteNonQuery();
					}
					else if (event_dttm.Length > 10)
					{
						string event_date = event_dttm.Substring(0, 10) + " " + event_dttm.Substring(11, 8);
						myRuntimeDBWriteCommand.CommandText = "insert into DBA.device_events (event_type, date_done, inst_num, operator_num, arch, event_desc";
						myRuntimeDBWriteCommand.CommandText += ", uuid";
						myRuntimeDBWriteCommand.CommandText += string.Format(") values ('{0}','{1}','{2}',(select operator_num from DBA.operators where operator_id = '{3}'),'F','{4}'", (event_type == "TY=MT") ? "M" : ((event_type == "TY=SE") ? "E" : "O"), event_date, m_inst_num, operator_id.Replace("'", "''"), severity_cd + ": " + description.Replace("'", "''"));
						myRuntimeDBWriteCommand.CommandText += string.Format(",'{0}'", Guid.NewGuid().ToString("N"));
						myRuntimeDBWriteCommand.CommandText += ")";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBWriteCommand.ExecuteNonQuery();
					}
					break;
				default:
					reader.GetAttribute("V");
					_ = reader.Value;
					break;
				case "OPR":
					break;
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing events", "ProcessEvents", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing events", "ProcessEvents", "Protocol");
		}
		finally
		{
			m_waiting = true;
			SendAcknowledgeMessage(m_control_id, isError: false);
		}
	}

	private void AddPortToDb()
	{
		try
		{
			myRuntimeDBReadCommand.CommandText = $"select comm_record_num from DBA.communications where Computer_Name = '{m_NNBase.GetHostName()}' and Instrument_ID = '{m_serial_id}' and port_num = '{m_port_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			if (myRuntimeDBReadReader.Read())
			{
				m_comm_record_num = myRuntimeDBReadReader.GetString(0);
				myRuntimeDBReadReader.Close();
				return;
			}
			myRuntimeDBReadReader.Close();
			m_comm_record_num = Guid.NewGuid().ToString("N");
			myRuntimeDBWriteCommand.CommandText = string.Format("insert into DBA.communications (Computer_Name, Instrument_ID, Protocol, Port_Type, Comm_Protocol, Port_num, Flow_Control, Connect_Remote, Port_Active, Last_Activity, comm_record_num, from_ui, Used, Multi_Connect, from_inst_id) values ('{0}','{1}','DML','{4}','TCPIP','{3}','0','0','0','0','{2}','F','F','0','{5}')", m_NNBase.GetHostName(), m_serial_id, m_comm_record_num, m_port_num, m_inst_type, m_from_inst_id);
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBWriteCommand.ExecuteNonQuery();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "adding port to database", "AddPortToDb", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "adding port to database", "AddPortToDb", "Protocol");
		}
	}

	private void ProcessDeviceStatus(XmlNodeReader reader)
	{
		m_new_observations_qty = "0";
		m_new_events_qty = "0";
		m_condition_cd = "";
		m_operators_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_patients_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_setup_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_wifi_setup_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_cert_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_loc_list_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_phys_update_dttm = "2000-01-01T00:00:00.00-04:00";
		m_reag_update_dttm = "2000-01-01T00:00:00.00-04:00";
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
				case "DST.new_observations_qty":
					m_new_observations_qty = reader.GetAttribute("V");
					break;
				case "DST.new_events_qty":
					m_new_events_qty = reader.GetAttribute("V");
					break;
				case "DST.condition_cd":
					m_condition_cd = reader.GetAttribute("V");
					break;
				case "DST.operators_update_dttm":
					m_operators_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.patients_update_dttm":
					m_patients_update_dttm = reader.GetAttribute("V");
					device_patient_update_datetime = DMLToSystemDateTime(m_patients_update_dttm);
					break;
				case "DST.setup_update_dttm":
					m_setup_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.wifi_setup_update_dttm":
					m_wifi_setup_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.wifi_cert_update_dttm":
					m_cert_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.loc_list_update_dttm":
					m_loc_list_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.phys_update_dttm":
					m_phys_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.reag_update_dttm":
					m_reag_update_dttm = reader.GetAttribute("V");
					break;
				}
			}
		}
		try
		{
			myRuntimeDBWriteCommand.CommandText = string.Format("UPDATE DBA.instruments SET inst_condition = '{1}' WHERE inst_num = '{0}'", m_inst_num, m_condition_cd);
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBWriteCommand.ExecuteNonQuery();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "updating instruments", "ProcessDeviceStatus", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "updating instruments", "ProcessDeviceStatus", "Protocol");
		}
		SendAcknowledgeMessage(m_control_id, isError: false);
	}

	private void ProcessKeepAlive(XmlNodeReader reader)
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
		SendAcknowledgeMessage(m_control_id, isError: false);
	}

	private void ProcessObservation(string typeOfOBS)
	{
		try
		{
			XmlElement root = m_doc.DocumentElement;
			XmlElement elem = (XmlElement)root.SelectSingleNode("HDR/HDR.control_id");
			if (elem != null)
			{
				m_control_id = elem.GetAttribute("V");
			}
			bool isQC = false;
			if (typeOfOBS.CompareTo("OBS.R02") == 0)
			{
				isQC = true;
			}
			XmlNodeList nodeList = root.SelectNodes("SVC");
			if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
			{
				OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
			}
			foreach (XmlNode svc in nodeList)
			{
				Sample_Table newRecord = GetANewRecord();
				XmlNodeList nodeListOBS = svc.SelectNodes(isQC ? "CTC/OBS" : "PT/OBS");
				int nodecount = nodeListOBS.Count;
				if (nodecount < 1)
				{
					m_NNBase.ReportErrorDB("No " + (isQC ? "CTC/OBS" : "PT/OBS") + " nodes in sample", "E", "parsing xml_text in sample", "ProcessObservation", "");
					break;
				}
				XmlElement elemSvc = (XmlElement)svc;
				string loc_num = m_loc_num;
				string facility = m_facility;
				string location = m_location;
				string enterprise_id = "";
				string medrec_num = "";
				string account_num = "";
				string sample_id_type = FindTextByChildNode(ref elemSvc, "NTE", "NTE.text", "V", "SAMPLE ID TYPE");
				elem = (XmlElement)svc.SelectSingleNode("SVC.role_cd");
				newRecord.control_type = elem.GetAttribute("V");
				elem = (XmlElement)svc.SelectSingleNode("SVC.observation_dttm");
				newRecord.sample_Date = DMLFacilityLocalDateTime(elem.GetAttribute("V"));
				if (isQC)
				{
					elem = (XmlElement)svc.SelectSingleNode("CTC/CTC.lot_number");
					if (elem != null)
					{
						newRecord.control_lot_num = elem.GetAttribute("V");
					}
					elem = (XmlElement)svc.SelectSingleNode("CTC/CTC.level_cd");
					if (elem != null)
					{
						newRecord.lot_level = elem.GetAttribute("V");
					}
					if (m_inst_class.CompareTo("StatStrip") == 0)
					{
						newRecord.internal_external = m_control_internal_external;
					}
					else if (m_inst_class.CompareTo("pHOx Ultra") == 0)
					{
						XmlNodeList noteList = svc.SelectNodes("NTE");
						foreach (XmlNode nte in noteList)
						{
							elem = (XmlElement)nte.FirstChild;
							string attribute = elem.GetAttribute("V");
							string text;
							if ((text = attribute) == null || !(text == "Analysis cycle flags"))
							{
								continue;
							}
							string info = elem.InnerText;
							string[] arrayOfInfo = info.Split('^');
							if (arrayOfInfo.Length > 1 && !string.IsNullOrEmpty(arrayOfInfo[1]))
							{
								if (arrayOfInfo[1].Substring(0, 3).CompareTo("EXT") == 0)
								{
									newRecord.internal_external = "External";
								}
								else if (arrayOfInfo[1].Substring(0, 3).CompareTo("INT") == 0)
								{
									newRecord.internal_external = "Internal";
								}
							}
						}
					}
					else if (m_inst_class.CompareTo("Prime") == 0 || m_inst_class.CompareTo("Prime+") == 0)
					{
						elem = (XmlElement)svc.SelectSingleNode("CTC/CTC.name");
						if (elem != null)
						{
							string ctcName = elem.GetAttribute("V");
							if (ctcName.IndexOf("Internal") > 0)
							{
								newRecord.internal_external = "Internal";
							}
							else if (ctcName.IndexOf("External") > 0)
							{
								newRecord.internal_external = "External";
							}
							else
							{
								newRecord.internal_external = m_control_internal_external;
							}
						}
					}
					else
					{
						newRecord.internal_external = m_control_internal_external;
					}
				}
				else
				{
					string patient_id = string.Empty;
					elem = (XmlElement)svc.SelectSingleNode("PT/PT.patient_id");
					if (elem != null)
					{
						patient_id = elem.GetAttribute("V");
					}
					string patient_id_field = string.Empty;
					if (m_inst_class.CompareTo("StatStrip") == 0)
					{
						switch (sample_id_type)
						{
						case "PATID":
							patient_id_field = "patient_id";
							if (patient_id.Length > 0 && patient_id != "UNKNOWN")
							{
								enterprise_id = patient_id;
							}
							break;
						case "MRN":
							patient_id_field = "medrec_num";
							if (patient_id.Length > 0 && patient_id != "UNKNOWN")
							{
								medrec_num = patient_id;
							}
							break;
						case "ACCT":
							patient_id_field = "account_num";
							if (patient_id.Length > 0 && patient_id != "UNKNOWN")
							{
								account_num = patient_id;
							}
							break;
						}
					}
					else if (elem != null && !string.IsNullOrEmpty(elem.InnerText))
					{
						patient_id_field = GetPatientIDTypeForBGA(patient_id, elem.InnerText, out medrec_num, out enterprise_id, out account_num);
					}
					elem = (XmlElement)svc.SelectSingleNode("PT/PT.location");
					if (elem != null)
					{
						string loc = elem.GetAttribute("V");
						char[] hat = new char[1] { '^' };
						string[] locs = loc.Split(hat);
						bool newLoc = false;
						if (locs.GetLength(0) > 0 && locs[0].Length > 0)
						{
							if (facility != locs[0])
							{
								newLoc = true;
							}
							facility = locs[0];
						}
						if (locs.GetLength(0) > 1 && locs[1].Length > 0)
						{
							if (location != locs[1])
							{
								newLoc = true;
							}
							location = locs[1];
						}
						if (newLoc)
						{
							loc_num = "";
							myRuntimeDBReadCommand.CommandText = string.Format("select loc_num from DBA.inst_locations where loc_name = '{0}' and parent = (select loc_num from DBA.inst_locations where loc_name = '{1}' and level_num = 1)", location.Replace("'", "''"), facility.Replace("'", "''"));
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							if (myRuntimeDBReadReader.Read())
							{
								loc_num = (myRuntimeDBReadReader.IsDBNull(0) ? m_loc_num : myRuntimeDBReadReader.GetString(0));
							}
							else if (m_force_result_default)
							{
								myRuntimeDBReadReader.Close();
								myRuntimeDBReadCommand.CommandText = $"select l.loc_num, l.loc_name, p.loc_name from DBA.inst_locations l join DBA.inst_locations p on p.loc_num = l.parent where l.is_default = 'T' and p.is_default = 'T'";
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
								}
								myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
								if (myRuntimeDBReadReader.Read())
								{
									loc_num = (myRuntimeDBReadReader.IsDBNull(0) ? m_loc_num : myRuntimeDBReadReader.GetString(0));
									location = (myRuntimeDBReadReader.IsDBNull(1) ? m_location : myRuntimeDBReadReader.GetString(1));
									facility = (myRuntimeDBReadReader.IsDBNull(2) ? m_facility : myRuntimeDBReadReader.GetString(2));
								}
							}
							else
							{
								loc_num = m_loc_num;
							}
							myRuntimeDBReadReader.Close();
						}
					}
					PatientRec m_Patient = null;
					if (string.IsNullOrEmpty(enterprise_id) || string.IsNullOrEmpty(medrec_num) || string.IsNullOrEmpty(account_num))
					{
						PatientList m_PatientList = new PatientList();
						m_facil_num = GetFacilNum(facility);
						m_PatientList.GetPatientIDs(m_NNBase, ref myRuntimeDBReadCommand, m_facil_num, bByFacility: false, loc_num, patient_id_field, ref enterprise_id, ref medrec_num, ref account_num, bRetrieveDetails: false, ref m_Patient);
					}
					newRecord.patient_id = enterprise_id;
					newRecord.medrec_num = medrec_num;
					newRecord.account_num = account_num;
				}
				newRecord.fac_name = facility;
				newRecord.loc_name = location;
				elem = (XmlElement)svc.SelectSingleNode("ORD/ORD.order_id");
				if (elem != null)
				{
					newRecord.Accession_num = elem.GetAttribute("V");
				}
				XmlNodeList nodeListRgt = svc.SelectNodes("RGT");
				foreach (XmlNode rgt in nodeListRgt)
				{
					string name = "";
					string lot_number = "";
					string lot_type = "";
					string expiration_date = "";
					GetReagentLotInfo(rgt, ref name, ref lot_number, ref lot_type, ref expiration_date);
					if (lot_type == "TestStrip" || lot_type == "MT_TS")
					{
						newRecord.strip_lot_num = lot_number;
					}
				}
				newRecord.xml_text = svc.OuterXml;
				myRuntimeDBWriteCommand.CommandText = string.Format("select count(*) from DBA.samples where sample_Date = '{0}' and device_serial = '{1}'", newRecord.sample_Date.ToString("yyyy-MM-dd HH:mm:ss"), m_serial_id);
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				int icount = (int)myRuntimeDBWriteCommand.ExecuteScalar();
				if (icount == 0)
				{
					newRecord.sample_key_num = Guid.NewGuid().ToString("N");
					myRuntimeDBWriteCommand.CommandText = "insert into DBA.samples ( device_serial, sample_Date, transmitted_flag, saved_to_history_db_flag, xml_text, patient_id, medrec_num, account_num, loc_name, fac_name, control_type, accession_num, sample_key_num, control_lot_num, strip_lot_num, device_type, device_name, device_sw_ver, lot_level, internal_external";
					string ValuesText = string.Format(") values ('{0}','{1}','F','F','{2}',{3},{4},{5},{6},{7},{8},{9},'{10}','{11}','{12}','{13}','{14}','{15}','{16}','{17}'", newRecord.device_serial, newRecord.sample_Date.ToString("yyyy-MM-dd HH:mm:ss"), newRecord.xml_text.Replace("'", "''"), (newRecord.patient_id.Length > 0) ? ("'" + newRecord.patient_id.Replace("'", "''") + "'") : "null", (newRecord.medrec_num.Length > 0) ? ("'" + newRecord.medrec_num.Replace("'", "''") + "'") : "null", (newRecord.account_num.Length > 0) ? ("'" + newRecord.account_num.Replace("'", "''") + "'") : "null", (newRecord.loc_name.Length > 0) ? ("'" + newRecord.loc_name.Replace("'", "''") + "'") : "null", (newRecord.fac_name.Length > 0) ? ("'" + newRecord.fac_name.Replace("'", "''") + "'") : "null", (newRecord.control_type.Length > 0) ? ("'" + newRecord.control_type + "'") : "null", (newRecord.Accession_num.Length > 0) ? ("'" + newRecord.Accession_num + "'") : "null", newRecord.sample_key_num, newRecord.control_lot_num, newRecord.strip_lot_num, newRecord.device_type, newRecord.device_name, newRecord.device_sw_ver, newRecord.lot_level, newRecord.internal_external);
					OdbcCommand odbcCommand = myRuntimeDBWriteCommand;
					odbcCommand.CommandText = odbcCommand.CommandText + ValuesText + ")";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
					}
					icount = myRuntimeDBWriteCommand.ExecuteNonQuery();
				}
				if (icount == 1)
				{
					if (m_inst_class.CompareTo("StatStrip") == 0)
					{
						XmlNodeList nodeList2 = svc.SelectNodes("RGT");
						foreach (XmlNode rgt2 in nodeList2)
						{
							string name2 = "";
							string lot_number2 = "";
							string lot_type2 = "";
							string expiration_date2 = "";
							GetReagentLotInfo(rgt2, ref name2, ref lot_number2, ref lot_type2, ref expiration_date2);
							if (lot_number2.Length <= 0 || lot_type2.CompareTo("PRO") == 0)
							{
								continue;
							}
							bool isFound = false;
							myRuntimeDBReadCommand.CommandText = $"select distinct lot_level, level_type, generic_test_name, LR, HR, Units from DBA.lots l left outer join DBA.lot_chem lc on lc.lots_key_num = l.lots_key_num  where lot = '{lot_number2}'";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							if (myRuntimeDBReadReader.Read())
							{
								isFound = true;
							}
							myRuntimeDBReadReader.Close();
							if (!isFound)
							{
								string lots_key_num = Guid.NewGuid().ToString("N");
								string expDate = ((expiration_date2.Length > 9) ? expiration_date2.Substring(0, 10) : "2007-01-01");
								myRuntimeDBWriteCommand.CommandText = "insert into DBA.lots ( lots_key_num, lot, expDate, lot_type,";
								myRuntimeDBWriteCommand.CommandText += " datetime_stamp, lot_name, in_use, usedCount, retired, is_validated) values (";
								myRuntimeDBWriteCommand.CommandText += $" '{lots_key_num}','{lot_number2}','{expDate}','{lot_type2}'";
								myRuntimeDBWriteCommand.CommandText += string.Format(",'{0}','{1}','T',1,'F','F')", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"), name2);
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
								}
								myRuntimeDBWriteCommand.ExecuteNonQuery();
								myRuntimeDBWriteCommand.CommandText = $"insert into DBA.device_to_lot ( lots_key_num, inst_type) values ( '{lots_key_num}','{m_inst_type}')";
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
								}
								myRuntimeDBWriteCommand.ExecuteNonQuery();
							}
							else
							{
								myRuntimeDBWriteCommand.CommandText = "Update dba.lots set usedCount = usedCount + 1 where lot = '" + lot_number2 + "'";
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
								}
								myRuntimeDBWriteCommand.ExecuteNonQuery();
							}
						}
					}
				}
				else
				{
					m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
					m_NNBase.ReportErrorDB("Unable to insert sample into database", "C", "processing observation", "ProcessObservation", "");
				}
				if (icount != 1 || newRecord.Accession_num.Length <= 0)
				{
					continue;
				}
				try
				{
					myRuntimeDBWriteCommand.CommandText = $"delete from DBA.orders where accession_num = '{newRecord.Accession_num}'";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
					}
					icount = myRuntimeDBWriteCommand.ExecuteNonQuery();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e)
				{
					handleDBException(e, "deleting order", "ProcessObservation", "Protocol");
				}
				catch (Exception e2)
				{
					handleException(e2, "deleting order", "ProcessObservation", "Protocol");
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e3)
		{
			handleDBException(e3, "processing observation", "ProcessObservation", "Protocol");
		}
		catch (Exception e4)
		{
			handleException(e4, "processing observation", "ProcessObservation", "Protocol");
		}
		m_waiting = !m_isContinuous;
		SendAcknowledgeMessage(m_control_id, isError: false);
	}

	private void ProcessQuery()
	{
		XmlDocument DmlDoc = new XmlDocument();
		XmlElement root = m_doc.DocumentElement;
		string order_id = "";
		string enterprise_id = "";
		string medrec_num = "";
		string account_num = "";
		string operator_id = "";
		string sex = "";
		string facility = "";
		string facil_num = "";
		string location = "";
		string loc_num = "";
		string room_num = "";
		string bed_num = "";
		string first_name = "";
		string last_name = "";
		string middle_name = "";
		string prefix = "";
		string suffix = "";
		DateTime birthdate = DateTime.MinValue;
		DateTime orderDateTime = DateTime.Now;
		string Weight_value = "";
		string Weight_units = "";
		string Height_value = "";
		string Height_units = "";
		string Race = "";
		string Diagnosis = "";
		string panel = "";
		string patient_id_field = "";
		string sample_type = "";
		string AttendingPhysician = "";
		string Notes = "";
		string QueryType = "";
		string QueryID = "";
		string QueryIDType = "";
		PatientRec m_Patient = null;
		int SleepCount = 0;
		bool order_readok = false;
		string sAckResponse = "";
		string sNakResponse = "";
		string effective_patient_id = "";
		bool bOK = false;
		bool bQueryResponseSent = false;
		string method_cd = "";
		DateTime cert_start_date = DateTime.MinValue;
		DateTime cert_end_date = DateTime.MaxValue;
		int privilege = 0;
		string opr_message = "";
		DateTime msg_create_dttm = DateTime.MinValue;
		string opr_first_name = "";
		string opr_last_name = "";
		string title = "";
		bool ignore_location = false;
		try
		{
			sNakResponse = QueryNakString("", "", "", "", "Invalid query message format or decoding error");
			XmlElement elem = (XmlElement)root.SelectSingleNode("HDR/HDR.control_id");
			if (elem != null)
			{
				m_control_id = elem.GetAttribute("V");
			}
			((XmlElement)root.SelectSingleNode("DTV/DTV.command_cd"))?.GetAttribute("V");
			elem = (XmlElement)root.SelectSingleNode("PARAM/PARAM.query_type");
			if (elem != null)
			{
				QueryType = elem.GetAttribute("V");
			}
			elem = (XmlElement)root.SelectSingleNode("PARAM/PARAM.serial_id");
			if (elem != null)
			{
				m_serial_id = elem.GetAttribute("V");
			}
			elem = (XmlElement)root.SelectSingleNode("PARAM/PARAM.query_id");
			if (elem != null)
			{
				QueryID = elem.GetAttribute("V");
				QueryIDType = elem.InnerXml;
			}
			elem = (XmlElement)root.SelectSingleNode("PARAM/PARAM.ignore_location");
			if (elem != null)
			{
				string signoreLocation = elem.GetAttribute("V");
				if (!string.IsNullOrEmpty(signoreLocation) && signoreLocation.CompareTo("T") == 0)
				{
					ignore_location = true;
				}
			}
			switch (QueryIDType)
			{
			case "PATID":
				enterprise_id = QueryID;
				break;
			case "MRN":
				medrec_num = QueryID;
				break;
			case "ACCT":
				account_num = QueryID;
				break;
			case "ACCN":
				order_id = QueryID;
				break;
			case "OPID":
				operator_id = QueryID;
				break;
			}
			if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
			{
				OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Protocol");
			}
			if (m_loc_num.Length > 0)
			{
				bOK = true;
				if (m_facility.Length == 0 || m_location.Length == 0)
				{
					GetFacilityAndLocationByLocNum();
				}
			}
			else
			{
				sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Meter location is unknown");
				bOK = false;
			}
			if (bOK && QueryType != "ORDER" && QueryType != "PATIENT" && QueryType != "OPERATOR")
			{
				sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Query type is not supported");
				bOK = false;
			}
			if (bOK)
			{
				if (m_serial_id.Length > 0 && ((QueryType == "ORDER" && order_id.Length > 0) || (QueryType == "PATIENT" && (medrec_num.Length > 0 || enterprise_id.Length > 0 || account_num.Length > 0)) || (QueryType == "OPERATOR" && operator_id.Length > 0)))
				{
					bOK = true;
				}
				else
				{
					sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Query is missing one or more parameters");
					bOK = false;
				}
			}
			if (bOK)
			{
				switch (QueryType)
				{
				case "PATIENT":
					sAckResponse = QueryAckString(QueryType, m_control_id, m_serial_id, QueryID, "<PT></PT>");
					break;
				case "ORDER":
					sAckResponse = QueryAckString(QueryType, m_control_id, m_serial_id, QueryID, "<PT></PT><SPC></SPC><ORD></ORD>");
					break;
				case "OPERATOR":
					sAckResponse = QueryAckString(QueryType, m_control_id, m_serial_id, QueryID, "<OPR></OPR>");
					break;
				}
				DmlDoc.LoadXml(sAckResponse);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (XmlException e)
		{
			m_NNBase.ReportXMLException(e, "processing query message", "ProcessQuery");
			bOK = false;
			bQueryResponseSent = SendQueryResponse(DmlDoc, sNakResponse, bOK);
		}
		catch (Exception e2)
		{
			m_NNBase.ReportException(e2, "processing query message", "ProcessQuery");
			bOK = false;
			bQueryResponseSent = SendQueryResponse(DmlDoc, sNakResponse, bOK);
		}
		try
		{
			if (bOK && (QueryType == "PATIENT" || QueryType == "ORDER") && (medrec_num.Length > 0 || enterprise_id.Length > 0 || account_num.Length > 0))
			{
				if (QueryType == "PATIENT")
				{
					m_loc_def_pat_id = QueryIDType;
				}
				if ((m_loc_def_pat_id.Length == 0 || m_loc_def_pat_id == "PATID") && enterprise_id.Length > 0)
				{
					patient_id_field = "patient_id";
					m_loc_def_pat_id = "PATID";
					effective_patient_id = enterprise_id;
				}
				else if ((m_loc_def_pat_id.Length == 0 || m_loc_def_pat_id == "MRN") && medrec_num.Length > 0)
				{
					patient_id_field = "medrec_num";
					m_loc_def_pat_id = "MRN";
					effective_patient_id = medrec_num;
				}
				else if ((m_loc_def_pat_id.Length == 0 || m_loc_def_pat_id == "ACCT") && account_num.Length > 0)
				{
					patient_id_field = "account_num";
					m_loc_def_pat_id = "ACCT";
					effective_patient_id = account_num;
				}
				PatientList m_PatientList = new PatientList();
				if (m_facil_num.Length == 0 && m_facility.Length > 0)
				{
					m_facil_num = GetFacilNum(m_facility);
				}
				bOK = m_PatientList.GetPatientIDs(m_NNBase, ref myRuntimeDBReadCommand, m_facil_num, ignore_location, m_loc_num, patient_id_field, ref enterprise_id, ref medrec_num, ref account_num, bRetrieveDetails: true, ref m_Patient);
				if (bOK && m_Patient != null)
				{
					first_name = m_Patient.m_FirstName;
					first_name = first_name.Substring(0, Math.Min(first_name.Length, 16));
					last_name = m_Patient.m_LastName;
					last_name = last_name.Substring(0, Math.Min(last_name.Length, 16));
					middle_name = m_Patient.m_MiddleName;
					middle_name = middle_name.Substring(0, Math.Min(middle_name.Length, 16));
					prefix = m_Patient.m_prefix;
					suffix = m_Patient.m_suffix;
					birthdate = m_Patient.m_birthdate;
					sex = m_Patient.m_Sex;
					Race = m_Patient.m_race;
					int pAccount = m_Patient.m_PatientAccountList.First();
					if (pAccount >= 0)
					{
						PatientAccountRec m_PatientAccount = (PatientAccountRec)m_Patient.m_PatientAccountList.m_Array[pAccount];
						int pVisit = m_PatientAccount.m_PatientVisitList.First();
						if (pVisit >= 0)
						{
							PatientVisitRec m_PatientVisit = (PatientVisitRec)m_PatientAccount.m_PatientVisitList.m_Array[pVisit];
							loc_num = m_PatientVisit.m_locnum;
							room_num = m_PatientVisit.m_roomnum;
							bed_num = m_PatientVisit.m_bednum;
							Weight_value = m_PatientVisit.m_weight;
							Weight_units = m_PatientVisit.m_weight_units;
							Height_value = m_PatientVisit.m_height;
							Height_units = m_PatientVisit.m_height_units;
							Diagnosis = m_PatientVisit.m_diagnosis;
							AttendingPhysician = m_PatientVisit.m_AttendPhysician;
							Notes = m_PatientVisit.m_Physician_note;
						}
					}
					if (m_loc_num != loc_num)
					{
						GetFacilityAndLocationByLocNum(loc_num, ref facility, ref facil_num, ref location);
						if (string.Compare(m_facility, facility, ignoreCase: true) != 0)
						{
							sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Patient is not in the meter's facility");
							bOK = false;
						}
						else if (QueryType == "PATIENT" && !ignore_location)
						{
							sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Patient is not in the meter's location");
							bOK = false;
						}
					}
				}
				else
				{
					sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Patient not found");
					bOK = false;
				}
				if (bOK)
				{
					GetFacilityAndLocationByLocNum(loc_num, ref facility, ref facil_num, ref location);
				}
			}
			if (bOK && QueryType == "ORDER" && order_id.Length > 0)
			{
				bool bExit = false;
				while (!bExit && panel.Length == 0 && SleepCount < 120)
				{
					order_readok = false;
					try
					{
						myRuntimeDBReadCommand.CommandText = "select order_key_num, order_date, patient_id, medrec_num, account_num, loc_name, fac_name, weight, weight_units, height, height_units, race, diagnosis, panel, sample_type";
						myRuntimeDBReadCommand.CommandText += $" from DBA.orders where accession_num = '{order_id}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
						order_readok = myRuntimeDBReadReader.Read();
						if (order_readok)
						{
							if (!myRuntimeDBReadReader.IsDBNull(0))
							{
								myRuntimeDBReadReader.GetString(0);
							}
							orderDateTime = (myRuntimeDBReadReader.IsDBNull(1) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(1));
							enterprise_id = (myRuntimeDBReadReader.IsDBNull(2) ? "" : myRuntimeDBReadReader.GetString(2));
							medrec_num = (myRuntimeDBReadReader.IsDBNull(3) ? "" : myRuntimeDBReadReader.GetString(3));
							account_num = (myRuntimeDBReadReader.IsDBNull(4) ? "" : myRuntimeDBReadReader.GetString(4));
							location = (myRuntimeDBReadReader.IsDBNull(5) ? location : myRuntimeDBReadReader.GetString(5));
							facility = (myRuntimeDBReadReader.IsDBNull(6) ? facility : myRuntimeDBReadReader.GetString(6));
							Weight_value = (myRuntimeDBReadReader.IsDBNull(7) ? Weight_value : myRuntimeDBReadReader.GetString(7));
							Weight_units = (myRuntimeDBReadReader.IsDBNull(8) ? Weight_units : myRuntimeDBReadReader.GetString(8));
							Height_value = (myRuntimeDBReadReader.IsDBNull(9) ? Height_value : myRuntimeDBReadReader.GetString(9));
							Height_units = (myRuntimeDBReadReader.IsDBNull(10) ? Height_units : myRuntimeDBReadReader.GetString(10));
							Race = (myRuntimeDBReadReader.IsDBNull(11) ? Race : myRuntimeDBReadReader.GetString(11));
							Diagnosis = (myRuntimeDBReadReader.IsDBNull(12) ? Diagnosis : myRuntimeDBReadReader.GetString(12));
							panel = (myRuntimeDBReadReader.IsDBNull(13) ? panel : myRuntimeDBReadReader.GetString(13));
							sample_type = (myRuntimeDBReadReader.IsDBNull(14) ? sample_type : myRuntimeDBReadReader.GetString(14));
						}
						myRuntimeDBReadReader.Close();
					}
					catch (OdbcException e3)
					{
						bExit = true;
						sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "DB exception reading order record");
						bOK = false;
						handleDBException(e3, "reading order", "ProcessQuery", "Protocol");
					}
					catch (Exception e4)
					{
						bExit = true;
						sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Exception reading order record");
						bOK = false;
						handleException(e4, "reading order", "ProcessQuery", "Protocol");
					}
					if (order_readok && string.Compare(m_facility, facility, ignoreCase: true) != 0)
					{
						sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Patient is not in the meter's facility");
						bOK = false;
						bExit = true;
					}
					if (order_readok && panel.Length > 0)
					{
						bExit = true;
					}
					if (!bExit && bOK && !order_readok)
					{
						try
						{
							myRuntimeDBWriteCommand.CommandText = string.Format("insert into DBA.orders ( order_key_num, accession_num, order_date, patient_id, medrec_num, account_num, loc_name, fac_name, weight, weight_units, height, height_units, race, diagnosis, sample_type, transmitted_flag, instrument_id) values ('{0}','{1}',datetime('{2}'),'{3}','{4}','{5}','{6}','{7}','{8}','{9}','{10}','{11}','{12}','{13}','{14}','{15}','{16}')", Guid.NewGuid().ToString("N"), order_id, orderDateTime.ToString("yyyy-MM-dd HH:mm:ss"), enterprise_id, medrec_num, account_num, m_location, m_facility, Weight_value, Weight_units, Height_value, Height_units, Race, Diagnosis, sample_type, "F", m_serial_id);
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBWriteCommand.ExecuteNonQuery();
						}
						catch (OdbcException e5)
						{
							bExit = true;
							sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "DB exception creating order record");
							bOK = false;
							handleDBException(e5, "creating order query", "ProcessQuery", "Protocol");
						}
						catch (Exception e6)
						{
							bExit = true;
							sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Exception creating order record");
							bOK = false;
							handleException(e6, "creating order query", "ProcessOrderQuery", "Protocol");
						}
					}
					if (order_readok && !bExit)
					{
						Thread.Sleep(1000);
						SleepCount++;
					}
				}
			}
			if (bOK && (QueryType == "PATIENT" || (QueryType == "ORDER" && order_readok && panel.Length > 0)) && (enterprise_id.Length > 0 || medrec_num.Length > 0 || account_num.Length > 0))
			{
				root = DmlDoc.DocumentElement;
				XmlElement elemORD;
				XmlElement elemSPC;
				if (QueryType == "ORDER")
				{
					elemORD = (XmlElement)root.SelectSingleNode("ACK/ACK.note_txt/ORD");
					elemSPC = (XmlElement)root.SelectSingleNode("ACK/ACK.note_txt/SPC");
				}
				else
				{
					elemORD = null;
					elemSPC = null;
				}
				XmlElement elemPAT = (XmlElement)root.SelectSingleNode("ACK/ACK.note_txt/PT");
				if (QueryType == "ORDER")
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemORD, "ORD.order_id", "V", order_id, "");
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemORD, "ORD.universal_service_id", "V", panel, "");
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemSPC, "SPC.type_cd", "V", sample_type, "");
				}
				if (Comp.Compare(m_inst_class, "StatStrip", CompOpt) == 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.patient_id", "V", effective_patient_id, m_loc_def_pat_id);
				}
				else
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.patient_id", "V", effective_patient_id, medrec_num + "^" + enterprise_id + "^" + account_num);
				}
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.location", "V", facility + "^" + location + "^" + room_num + "^" + bed_num, "");
				XmlElement elemName = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.name", "V", first_name + "  " + last_name, "");
				if (last_name.Length > 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "FAM", "V", last_name, "");
				}
				if (first_name.Length > 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "GIV", "V", first_name, "");
				}
				if (middle_name.Length > 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "MID", "V", middle_name, "");
				}
				if (prefix.Length > 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "PFX", "V", prefix, "");
				}
				if (suffix.Length > 0)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "SFX", "V", suffix, "");
				}
				if (birthdate.Year > 1)
				{
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.birth_date", "V", birthdate.ToString("yyyy-MM-dd"), "");
				}
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.gender_cd", "V", sex.ToUpper(), "");
				XmlElement tElem = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.weight", "V", Weight_value, "");
				tElem.SetAttribute("U", Weight_units);
				tElem = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.height", "V", Height_value, "");
				tElem.SetAttribute("U", Height_units);
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.race_cd", "V", Race, "");
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.ethnic_cd", "V", Race, "");
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.diagnosis", "V", Diagnosis, "");
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPAT, "PT.physician", "V", AttendingPhysician, "");
				if (Notes.Length > 0)
				{
					XmlElement elemNTE = (XmlElement)elemPAT.SelectSingleNode("NTE");
					if (elemNTE == null)
					{
						elemNTE = DmlDoc.CreateElement("NTE");
						elemPAT.AppendChild(elemNTE);
					}
					FindOrAddNodeByAttribute(ref DmlDoc, ref elemNTE, "NTE.text", "V", Notes, DateTime2DML(DateTime.Now));
				}
			}
			if (bOK && QueryType == "OPERATOR" && operator_id.Length > 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = "SELECT DISTINCT  operator_id,";
					myRuntimeDBReadCommand.CommandText += "  test_name,";
					myRuntimeDBReadCommand.CommandText += "  cert_start_date,  cert_end_date,  privilege,  IFNULL(msg_read_dttm, opr_message, null),  msg_create_dttm,  o2u.is_active,  op.is_active,  first_name,  last_name,  title FROM";
					myRuntimeDBReadCommand.CommandText += "  DBA.operator_privilege op";
					myRuntimeDBReadCommand.CommandText += $"  join DBA.operators o on op.operator_num = o.operator_num  join DBA.operator_to_unit o2u on o2u.operator_num = o.operator_num  key join DBA.inst_locations il  join DBA.contact_info ci on ci.contact_num = o.operator_num  left outer join DBA.operator_message om on (om.operator_num = o.operator_num and om.current_msg = 'T') WHERE  (op.privilege != 0) AND  (op.inst_type = '{m_inst_type}' or op.inst_type = 'MTE') AND  (o2u.loc_num = '{m_loc_num}') AND  (o2u.is_active = 'T') AND  (o.operator_id = '{operator_id}') AND  (op.is_active = 'T')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool boper_read_ok = false;
					while (myRuntimeDBReadReader.Read())
					{
						boper_read_ok = true;
						operator_id = myRuntimeDBReadReader.GetString(0);
						method_cd = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
						cert_start_date = (myRuntimeDBReadReader.IsDBNull(2) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(2));
						cert_end_date = (myRuntimeDBReadReader.IsDBNull(3) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(3));
						privilege = ((!myRuntimeDBReadReader.IsDBNull(4)) ? myRuntimeDBReadReader.GetInt32(4) : 0);
						opr_message = (myRuntimeDBReadReader.IsDBNull(5) ? "" : myRuntimeDBReadReader.GetString(5));
						msg_create_dttm = (myRuntimeDBReadReader.IsDBNull(6) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(6));
						if (!myRuntimeDBReadReader.IsDBNull(7))
						{
							myRuntimeDBReadReader.GetString(7);
						}
						if (!myRuntimeDBReadReader.IsDBNull(8))
						{
							myRuntimeDBReadReader.GetString(8);
						}
						opr_first_name = (myRuntimeDBReadReader.IsDBNull(9) ? "" : myRuntimeDBReadReader.GetString(9));
						opr_first_name = opr_first_name.Substring(0, Math.Min(opr_first_name.Length, 16));
						opr_last_name = (myRuntimeDBReadReader.IsDBNull(10) ? "" : myRuntimeDBReadReader.GetString(10));
						opr_last_name = opr_last_name.Substring(0, Math.Min(opr_last_name.Length, 16));
						title = (myRuntimeDBReadReader.IsDBNull(11) ? "" : myRuntimeDBReadReader.GetString(11));
						root = DmlDoc.DocumentElement;
						XmlElement elemOPR = (XmlElement)root.SelectSingleNode("ACK/ACK.note_txt/OPR");
						FindOrAddNodeByAttribute(ref DmlDoc, ref elemOPR, "OPR.operator_id", "V", operator_id, "");
						XmlElement elemName2 = FindOrAddNodeByAttribute(ref DmlDoc, ref elemOPR, "OPR.name", "V", opr_first_name + "  " + opr_last_name, "");
						if (opr_last_name.Length > 0)
						{
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "FAM", "V", opr_last_name, "");
						}
						if (opr_first_name.Length > 0)
						{
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "GIV", "V", opr_first_name, "");
						}
						if (title.Length > 0)
						{
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "PFX", "V", title, "");
						}
						XmlElement elemACC = (XmlElement)elemOPR.SelectSingleNode("ACC");
						if (elemACC == null)
						{
							elemACC = DmlDoc.CreateElement("ACC");
							elemOPR.AppendChild(elemACC);
						}
						FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.method_cd", "V", method_cd, "");
						FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.permission_level_cd", "V", privilege.ToString(), "");
						if (m_inst_class.CompareTo("StatStrip") != 0)
						{
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.password", "V", "", "");
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.active_date", "V", DateTime2DML(cert_start_date), "");
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.expiration_date", "V", DateTime2DML(cert_end_date), "");
						}
						if (msg_create_dttm != DateTime.MinValue)
						{
							XmlElement elemNTE2 = (XmlElement)elemOPR.SelectSingleNode("NTE");
							if (elemNTE2 == null)
							{
								elemNTE2 = DmlDoc.CreateElement("NTE");
								elemOPR.AppendChild(elemNTE2);
							}
							FindOrAddNodeByAttribute(ref DmlDoc, ref elemNTE2, "NTE.text", "V", opr_message, DateTime2DMLCenti(msg_create_dttm));
						}
					}
					myRuntimeDBReadReader.Close();
					if (!boper_read_ok)
					{
						bOK = false;
						sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "No operator records found for operator ID in meter's location");
					}
				}
				catch (OdbcException e7)
				{
					sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "DB exception reading operator records");
					bOK = false;
					handleDBException(e7, "reading operator records", "ProcessQuery", "Protocol");
				}
				catch (Exception e8)
				{
					sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Exception reading operator records");
					bOK = false;
					handleException(e8, "reading operator records", "ProcessQuery", "Protocol");
				}
			}
		}
		catch (OdbcException e9)
		{
			sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Database exception processing query");
			bOK = false;
			handleDBException(e9, "processing query", "ProcessQuery", "Protocol");
		}
		catch (XmlException e10)
		{
			sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "XML exception processing query");
			bOK = false;
			handleXMLException(e10, "processing query message", "ProcessQuery");
		}
		catch (Exception e11)
		{
			sNakResponse = QueryNakString(QueryType, m_control_id, m_serial_id, QueryID, "Exception processing query");
			bOK = false;
			handleException(e11, "processing query", "ProcessQuery", "Protocol");
		}
		if (!bQueryResponseSent)
		{
			SendQueryResponse(DmlDoc, sNakResponse, bOK);
		}
	}

	private bool SendQueryResponse(XmlDocument DmlDoc, string sNakResponse, bool bOK)
	{
		m_waiting = true;
		bool bSent = false;
		try
		{
			string sAck = "";
			sAck = ((!bOK) ? sNakResponse : DmlDoc.OuterXml);
			SendString(sAck, isPartial: false, trunc: false);
			bSent = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "responding to query", "ProcessQuery", "Protocol");
		}
		return bSent;
	}

	private string QueryAckString(string Command, string control_id, string serial_id, string QueryID, string otherstuff)
	{
		string response = "<ACK.R01>";
		response += GenDMLHeader("Protocol");
		response = response + "<ACK><ACK.type_cd V=\"AA\" /><ACK.ack_control_id V=\"" + control_id + "\" />";
		response = response + "<ACK.note_txt V=\"" + Command + "_QUERY\" >";
		response = response + "<DEV><DEV.serial_id V=\"" + serial_id + "\" /></DEV>";
		response = response + "<PARAM><PARAM.query_id V=\"" + FixXMLString(QueryID) + "\" /></PARAM>";
		response += otherstuff;
		return response + "</ACK.note_txt></ACK></ACK.R01>";
	}

	private string QueryNakString(string Command, string control_id, string serial_id, string QueryID, string error)
	{
		string response = "<ACK.R01>";
		response += GenDMLHeader("Protocol");
		response = response + "<ACK><ACK.type_cd V=\"AE\" /><ACK.ack_control_id V=\"" + control_id + "\" />";
		response = response + "<ACK.note_txt V=\"" + Command + "\" >";
		response = response + "<DEV><DEV.serial_id V=\"" + serial_id + "\" /></DEV>";
		response = response + "<PARAM><PARAM.query_id V=\"" + FixXMLString(QueryID) + "\" /></PARAM>";
		string text = response;
		response = text + "<NTE><NTE.text V=\"" + error + "\">" + DateTime2DML(DateTime.Now) + "</NTE.text></NTE>";
		return response + "</ACK.note_txt></ACK></ACK.R01>";
	}

	private string RestrictByLocation(string loc_num)
	{
		string restrict = "T";
		try
		{
			myRuntimeDBReadCommand.CommandText = "select restrict_to_local_queries";
			myRuntimeDBReadCommand.CommandText += $" from DBA.inst_locations where loc_num = '{loc_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			if (myRuntimeDBReadReader.Read())
			{
				restrict = (myRuntimeDBReadReader.IsDBNull(0) ? "T" : myRuntimeDBReadReader.GetString(0));
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "checking if location is a lab", "RestrictByLocation", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "checking if location is a lab", "RestrictByLocation", "Protocol");
		}
		return restrict;
	}

	private void GetReagentLotInfo(XmlNode rgt, ref string name, ref string lot_number, ref string lot_type, ref string expiration_date)
	{
		name = "";
		lot_number = "";
		lot_type = "";
		expiration_date = "";
		XmlElement elem = (XmlElement)rgt.SelectSingleNode("RGT.name");
		if (elem != null)
		{
			string regType = elem.InnerText;
			if (regType.Length > 0)
			{
				switch (regType)
				{
				case "TY=TS":
					lot_type = "TestStrip";
					break;
				case "TY=LN":
				case "TY=LIN":
					lot_type = "Linearity";
					break;
				case "TY=QC":
					lot_type = "Control";
					break;
				case "TY=RG":
					lot_type = "Reagent";
					break;
				default:
				{
					string[] typeArray = regType.Split('=');
					lot_type = typeArray[1];
					break;
				}
				}
			}
			name = elem.GetAttribute("V");
			if (name.Length == 0)
			{
				name = m_inst_type + "_" + ((lot_type == "TestStrip") ? "STRIP" : ((lot_type == "Control") ? " QC" : ((lot_type == "Linearity") ? " LIN" : "?")));
			}
		}
		elem = (XmlElement)rgt.SelectSingleNode("RGT.lot_number");
		if (elem != null)
		{
			lot_number = elem.GetAttribute("V");
		}
		elem = (XmlElement)rgt.SelectSingleNode("RGT.expiration_date");
		if (elem != null)
		{
			expiration_date = elem.GetAttribute("V");
		}
	}

	private string GetFacilNum(string facility)
	{
		m_facil_num = "";
		try
		{
			myRuntimeDBReadCommand.CommandText = string.Format("select loc_num from DBA.inst_locations where loc_name = '{0}' and level_num = 1", facility.Replace("'", "''"));
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			if (myRuntimeDBReadReader.Read() && !myRuntimeDBReadReader.IsDBNull(0))
			{
				m_facil_num = myRuntimeDBReadReader.GetString(0);
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up facility number", "GetFacilNum", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up facility number", "GetFacilNum", "Protocol");
		}
		return m_facil_num;
	}

	private bool GetFacilityAndLocationByLocNum()
	{
		return GetFacilityAndLocationByLocNum(m_loc_num, ref m_facility, ref m_facil_num, ref m_location);
	}

	private bool GetFacilityAndLocationByLocNum(string loc_num, ref string facility, ref string facil_num, ref string location)
	{
		bool bOK = false;
		try
		{
			myRuntimeDBReadCommand.CommandText = $"select loc_name, parent from DBA.inst_locations where loc_num = '{loc_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			location = "";
			facil_num = "";
			if (myRuntimeDBReadReader.Read())
			{
				location = myRuntimeDBReadReader.GetString(0);
				facil_num = myRuntimeDBReadReader.GetString(1);
			}
			myRuntimeDBReadReader.Close();
			if (facil_num.Length > 0)
			{
				bOK = LookupFacilName(facil_num, ref facility);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up location number", "GetFacilityAndLocationByLocNum", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up location number", "GetFacilityAndLocationByLocNum", "Protocol");
		}
		return bOK;
	}

	private bool LookupFacilName(string facil_num, ref string facility)
	{
		bool bOK = false;
		try
		{
			myRuntimeDBReadCommand.CommandText = $"select loc_name from DBA.inst_locations where loc_num = '{facil_num}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			facility = "";
			if (myRuntimeDBReadReader.Read())
			{
				facility = myRuntimeDBReadReader.GetString(0);
				bOK = true;
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "looking up facility name", "LookupFacilName", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "looking up facility name", "LookupFacilName", "Protocol");
		}
		return bOK;
	}

	private bool LookupLocNum()
	{
		bool bOK = false;
		try
		{
			myRuntimeDBReadCommand.CommandText = string.Format("SELECT loc_num FROM DBA.inst_locations WHERE loc_name = '{0}' AND parent = ( select loc_num from DBA.inst_locations where loc_name = '{1}' and level_num = 1 )", m_location.Replace("'", "''"), m_facility.Replace("'", "''"));
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			if (myRuntimeDBReadReader.Read())
			{
				m_loc_num = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
				bOK = true;
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "finding location", "LookupLocNum", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "finding location", "LookupLocNum", "Protocol");
		}
		return bOK;
	}

	private int SendOperatorList(out bool Incremental, out int count, out int dCount)
	{
		int totalDelete = 0;
		int totalAdd = 0;
		int delCount = 0;
		int addCount = 0;
		int totalCount = 0;
		XmlDocument DmlDoc = new XmlDocument();
		bool isIncremental = false;
		string pswd = "";
		Incremental = false;
		count = 0;
		dCount = 0;
		try
		{
			if ((m_OprListFullSupported || m_OprListIncrSupported) && m_loc_num.Length > 0 && (!m_isContinuous || CalcContinuousMinuteCount() >= LastContinuousOperatorSend + OpListFreq || m_ContinuousMinuteCount < m_LastContinuousMinuteCount))
			{
				DateTime operator_update_datetime = DMLToSystemDateTime(m_operators_update_dttm);
				string operators_update_date = operator_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - operator_update_datetime;
				myRuntimeDBReadCommand.CommandText = string.Format("SELECT count(*) from DBA.loc_last_update where ((loc_num = '{0}') or (loc_num = '{2}')) and data_type = 'OPERATORS' and last_update_time >= '{1}'", m_loc_num, operators_update_date, m_facil_num);
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				int opCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				myRuntimeDBReadCommand.CommandText = $"SELECT total_operators FROM DBA.instruments WHERE (inst_id = '{m_serial_id}')";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				object getObj = myRuntimeDBReadCommand.ExecuteScalar();
				int deviceTotalOperators = 0;
				if (!DBNull.Value.Equals(getObj))
				{
					deviceTotalOperators = Convert.ToInt32(getObj);
				}
				if (m_AlwaysSend || opCount > 0 || (opCount == 0 && ts.TotalDays > 365.0) || deviceTotalOperators < 0)
				{
					XmlElement root = null;
					XmlElement delNode = null;
					XmlElement addNode = null;
					isIncremental = true;
					if (m_OprListIncrSupported)
					{
						if (deviceTotalOperators > 0)
						{
							myRuntimeDBReadCommand.CommandText = "SELECT count(distinct o.operator_id) FROM";
							myRuntimeDBReadCommand.CommandText += "  DBA.operator_privilege op";
							myRuntimeDBReadCommand.CommandText += $"  join DBA.operators o on op.operator_num = o.operator_num  join DBA.operator_to_unit o2u on o2u.operator_num = o.operator_num  key join DBA.inst_locations il  join DBA.contact_info ci on ci.contact_num = o.operator_num  left outer join DBA.operator_message om on (om.operator_num = o.operator_num and om.current_msg = 'T') WHERE  (op.privilege != 0) AND  (op.inst_type = '{m_inst_type}' or op.inst_type = 'MTE') AND  ((o2u.loc_num = '{m_loc_num}') or (o2u.loc_num = '{m_facil_num}')) AND (  o2u.is_active = 'T' )";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							int totOperCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(totOperCount + " operators...", isXml: false, "ICPMGR");
							}
							if ((totOperCount > m_maxDownloadOperator || ts.TotalDays > 365.0) && m_OprListFullSupported)
							{
								isIncremental = false;
							}
							if (isIncremental)
							{
								myRuntimeDBReadCommand.CommandText = "SELECT count(distinct o.operator_id) FROM";
								myRuntimeDBReadCommand.CommandText += "  DBA.operator_privilege op";
								myRuntimeDBReadCommand.CommandText += string.Format("  join DBA.operators o on op.operator_num = o.operator_num  join DBA.operator_to_unit o2u on o2u.operator_num = o.operator_num  key join DBA.inst_locations il  join DBA.contact_info ci on ci.contact_num = o.operator_num  left outer join DBA.operator_message om on (om.operator_num = o.operator_num and om.current_msg = 'T') WHERE  (op.privilege != 0) AND  (op.inst_type = '{0}' or op.inst_type = 'MTE') AND  ((o2u.loc_num = '{2}') or (o2u.loc_num = '{3}')) AND (  o2u.is_active = 'T'  OR  o2u.is_active_last_update_date > '{1}' ) AND (  o2u.is_active_last_update_date > '{1}'  OR  op.is_active_last_update_date > '{1}'  OR  o.last_update_date > '{1}'  OR  op.last_update_date > '{1}'", m_inst_type, operators_update_date, m_loc_num, m_facil_num);
								myRuntimeDBReadCommand.CommandText += string.Format("  OR  ci.datetime_stamp > '{0}'  OR  om.datetime_stamp > '{0}'  OR  o.datetime_stamp > '{0}' )", operators_update_date);
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
								}
								int updateCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(updateCount + " new operators...", isXml: false, "ICPMGR");
								}
								if (updateCount > 0)
								{
									if (updateCount > MaxAddDelPerIncr && m_OprListFullSupported)
									{
										isIncremental = false;
									}
									else
									{
										isIncremental = true;
										m_last_incremental = "I";
										myRuntimeDBReadCommand.CommandText = "SELECT DISTINCT  operator_id,";
										myRuntimeDBReadCommand.CommandText += "  test_name,";
										myRuntimeDBReadCommand.CommandText += "  cert_start_date,  cert_end_date,  privilege,  IFNULL(msg_read_dttm, opr_message, null),  msg_create_dttm,  o2u.is_active,  op.is_active,  first_name,  last_name,  title,  add_date FROM";
										myRuntimeDBReadCommand.CommandText += "  DBA.operator_privilege op";
										myRuntimeDBReadCommand.CommandText += string.Format("  join DBA.operators o on op.operator_num = o.operator_num  join DBA.operator_to_unit o2u on o2u.operator_num = o.operator_num  key join DBA.inst_locations il  join DBA.contact_info ci on ci.contact_num = o.operator_num  left outer join DBA.operator_message om on (om.operator_num = o.operator_num and om.current_msg = 'T') WHERE  (op.privilege != 0) AND  (op.inst_type = '{0}' or op.inst_type = 'MTE') AND  ((o2u.loc_num = '{2}') or (o2u.loc_num = '{3}')) AND (  o2u.is_active = 'T'  OR  o2u.is_active_last_update_date > '{1}' ) AND (  o2u.is_active_last_update_date > '{1}'  OR  op.is_active_last_update_date > '{1}'  OR  o.last_update_date > '{1}'  OR  op.last_update_date > '{1}'", m_inst_type, operators_update_date, m_loc_num, m_facil_num);
										myRuntimeDBReadCommand.CommandText += string.Format("  OR  ci.datetime_stamp > '{0}'  OR  om.datetime_stamp > '{0}'  OR  o.datetime_stamp > '{0}' )", operators_update_date);
										myRuntimeDBReadCommand.CommandText += " ORDER BY operator_id ASC";
										if (m_NNBase.m_isLogging)
										{
											m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
										}
										myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
										bool firstTime = true;
										string preDelOpID = string.Empty;
										string preOpID = string.Empty;
										while (myRuntimeDBReadReader.Read())
										{
											string operator_id = myRuntimeDBReadReader.GetString(0);
											string method_cd = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
											DateTime cert_start_date = (myRuntimeDBReadReader.IsDBNull(2) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(2));
											DateTime cert_end_date = (myRuntimeDBReadReader.IsDBNull(3) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(3));
											int privilege = ((!myRuntimeDBReadReader.IsDBNull(4)) ? myRuntimeDBReadReader.GetInt32(4) : 0);
											string opr_message = (myRuntimeDBReadReader.IsDBNull(5) ? "" : myRuntimeDBReadReader.GetString(5));
											DateTime msg_create_dttm = (myRuntimeDBReadReader.IsDBNull(6) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(6));
											string is_active = (myRuntimeDBReadReader.IsDBNull(7) ? "" : myRuntimeDBReadReader.GetString(7));
											string p_is_active = (myRuntimeDBReadReader.IsDBNull(8) ? "" : myRuntimeDBReadReader.GetString(8));
											string first_name = (myRuntimeDBReadReader.IsDBNull(9) ? "" : myRuntimeDBReadReader.GetString(9));
											first_name = first_name.Substring(0, Math.Min(first_name.Length, 16));
											string last_name = (myRuntimeDBReadReader.IsDBNull(10) ? "" : myRuntimeDBReadReader.GetString(10));
											last_name = last_name.Substring(0, Math.Min(last_name.Length, 16));
											string title = (myRuntimeDBReadReader.IsDBNull(11) ? "" : myRuntimeDBReadReader.GetString(11));
											DateTime addData = (myRuntimeDBReadReader.IsDBNull(12) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(12));
											pswd = operator_id;
											_ = DateTime.Now;
											if (m_inst_class.CompareTo("StatStrip") != 0)
											{
												if (method_cd.CompareTo("MTE") == 0)
												{
													continue;
												}
												switch (privilege)
												{
												case 4:
													privilege = 2;
													break;
												default:
													privilege = 2;
													break;
												case 1:
													break;
												}
											}
											bool AddToDelete = false;
											if (addData < operator_update_datetime)
											{
												AddToDelete = true;
											}
											if (operator_id.CompareTo(preOpID) != 0 && (firstTime || addCount + delCount >= MaxAddDelPerMsg))
											{
												string opList;
												if (!firstTime)
												{
													if (delCount == 0)
													{
														root.RemoveChild(delNode);
													}
													if (addCount == 0)
													{
														root.RemoveChild(addNode);
													}
													opList = DmlDoc.OuterXml;
													m_waiting = true;
													SendString(opList, isPartial: true, trunc: false);
													LastContinuousOperatorSend = CalcContinuousMinuteCount();
													OnReadComplete();
												}
												totalDelete += delCount;
												totalAdd += addCount;
												delCount = 0;
												addCount = 0;
												opList = "<OPL.R02>" + GenDMLHeader("Protocol") + "<UPD></UPD><UPD></UPD></OPL.R02>";
												DmlDoc.LoadXml(opList);
												root = DmlDoc.DocumentElement;
												XmlNodeList UPDnodeList = root.SelectNodes("UPD");
												delNode = (XmlElement)UPDnodeList.Item(0);
												XmlElement updElemD = DmlDoc.CreateElement("UPD.action_cd");
												updElemD.SetAttribute("V", "D");
												delNode.AppendChild(updElemD);
												addNode = (XmlElement)UPDnodeList.Item(1);
												XmlElement updElemI = DmlDoc.CreateElement("UPD.action_cd");
												updElemI.SetAttribute("V", "I");
												addNode.AppendChild(updElemI);
											}
											if (AddToDelete)
											{
												FindOrCreateChildNode(ref DmlDoc, ref delNode, "OPR", "OPR.operator_id", "V", operator_id, "");
												if (preDelOpID.CompareTo(operator_id) != 0)
												{
													preDelOpID = operator_id;
													delCount++;
													totalCount++;
												}
											}
											if (!(is_active == "F") && !(p_is_active == "F"))
											{
												XmlElement elemOPR = FindOrCreateChildNode(ref DmlDoc, ref addNode, "OPR", "OPR.operator_id", "V", operator_id, "");
												XmlElement elemName = FindOrAddNodeByAttribute(ref DmlDoc, ref elemOPR, "OPR.name", "V", first_name + "  " + last_name, "");
												if (last_name.Length > 0)
												{
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "FAM", "V", last_name, "");
												}
												if (first_name.Length > 0)
												{
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "GIV", "V", first_name, "");
												}
												if (title.Length > 0)
												{
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "PFX", "V", title, "");
												}
												XmlElement elemACC = (XmlElement)elemOPR.SelectSingleNode("ACC");
												if (elemACC == null)
												{
													elemACC = DmlDoc.CreateElement("ACC");
													elemOPR.AppendChild(elemACC);
												}
												FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.method_cd", "V", method_cd, "");
												FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.permission_level_cd", "V", privilege.ToString(), "");
												if (m_inst_class.CompareTo("StatStrip") != 0)
												{
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.password", "V", pswd, "");
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.active_date", "V", DateTime2DML(cert_start_date), "");
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC, "ACC.expiration_date", "V", DateTime2DML(cert_end_date), "");
												}
												if (msg_create_dttm != DateTime.MinValue)
												{
													XmlElement elemNTE = (XmlElement)elemOPR.SelectSingleNode("NTE");
													if (elemNTE == null)
													{
														elemNTE = DmlDoc.CreateElement("NTE");
														elemOPR.AppendChild(elemNTE);
													}
													FindOrAddNodeByAttribute(ref DmlDoc, ref elemNTE, "NTE.text", "V", opr_message, DateTime2DMLCenti(msg_create_dttm));
												}
												if (elemACC.GetElementsByTagName("ACC.method_cd").Count == 1)
												{
													addCount++;
													totalCount++;
												}
											}
											firstTime = false;
											preOpID = operator_id;
										}
										myRuntimeDBReadReader.Close();
									}
								}
							}
						}
						else
						{
							isIncremental = false;
						}
					}
					if (!isIncremental)
					{
						addCount = 0;
						int operatorCount = 0;
						if (m_OprListIncrSupported || m_OprListFullSupported)
						{
							m_last_incremental = "C";
							myRuntimeDBReadCommand.CommandText = "SELECT DISTINCT  operator_id,";
							myRuntimeDBReadCommand.CommandText += "  test_name,";
							myRuntimeDBReadCommand.CommandText += " cert_start_date, cert_end_date, privilege, IFNULL(msg_read_dttm, opr_message, null), msg_create_dttm, first_name, last_name, title, o.last_update_date FROM";
							myRuntimeDBReadCommand.CommandText += "  DBA.operator_privilege op";
							myRuntimeDBReadCommand.CommandText += $" join DBA.operators o on op.operator_num = o.operator_num join DBA.operator_to_unit o2u on o2u.operator_num = o.operator_num key join DBA.inst_locations il join DBA.contact_info ci on ci.contact_num = o.operator_num left outer join DBA.operator_message om on (om.operator_num = o.operator_num and om.current_msg = 'T') WHERE (op.privilege != 0) AND (op.inst_type = '{m_inst_type}' or op.inst_type = 'MTE') AND o2u.is_active = 'T' AND op.is_active = 'T' AND ((o2u.loc_num = '{m_loc_num}') or (o2u.loc_num = '{m_facil_num}'))";
							myRuntimeDBReadCommand.CommandText += " ORDER BY o.last_update_date DESC, operator_id ASC";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							string opList = "<OPL.R01>" + GenDMLHeader("Protocol") + "</OPL.R01>";
							DmlDoc.LoadXml(opList);
							root = DmlDoc.DocumentElement;
							string preOpID2 = string.Empty;
							while (operatorCount <= m_maxDownloadOperator && myRuntimeDBReadReader.Read())
							{
								string operator_id2 = myRuntimeDBReadReader.GetString(0);
								string method_cd2 = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
								DateTime cert_start_date2 = (myRuntimeDBReadReader.IsDBNull(2) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(2));
								DateTime cert_end_date2 = (myRuntimeDBReadReader.IsDBNull(3) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(3));
								int privilege2 = ((!myRuntimeDBReadReader.IsDBNull(4)) ? myRuntimeDBReadReader.GetInt32(4) : 0);
								string opr_message2 = (myRuntimeDBReadReader.IsDBNull(5) ? "" : myRuntimeDBReadReader.GetString(5));
								DateTime msg_create_dttm2 = (myRuntimeDBReadReader.IsDBNull(6) ? DateTime.MinValue : myRuntimeDBReadReader.GetDateTime(6));
								string first_name2 = (myRuntimeDBReadReader.IsDBNull(7) ? "" : myRuntimeDBReadReader.GetString(7));
								first_name2 = first_name2.Substring(0, Math.Min(first_name2.Length, 16));
								string last_name2 = (myRuntimeDBReadReader.IsDBNull(8) ? "" : myRuntimeDBReadReader.GetString(8));
								last_name2 = last_name2.Substring(0, Math.Min(last_name2.Length, 16));
								string title2 = (myRuntimeDBReadReader.IsDBNull(9) ? "" : myRuntimeDBReadReader.GetString(9));
								pswd = operator_id2;
								_ = DateTime.Now;
								if (m_inst_class.CompareTo("StatStrip") != 0)
								{
									if (method_cd2.CompareTo("MTE") == 0)
									{
										continue;
									}
									switch (privilege2)
									{
									case 4:
										privilege2 = 2;
										break;
									default:
										privilege2 = 2;
										break;
									case 1:
										break;
									}
								}
								if (addCount >= MaxAddDelPerMsg && operator_id2.CompareTo(preOpID2) != 0)
								{
									opList = DmlDoc.OuterXml;
									m_waiting = true;
									SendString(opList, isPartial: true, trunc: false);
									LastContinuousOperatorSend = CalcContinuousMinuteCount();
									OnReadComplete();
									addCount = 0;
									opList = "<OPL.R01>" + GenDMLHeader("Protocol") + "</OPL.R01>";
									DmlDoc.LoadXml(opList);
									root = DmlDoc.DocumentElement;
								}
								if (operator_id2.CompareTo(preOpID2) != 0)
								{
									operatorCount++;
								}
								if (operatorCount > m_maxDownloadOperator)
								{
									continue;
								}
								XmlElement elemOPR2 = FindOrCreateChildNode(ref DmlDoc, ref root, "OPR", "OPR.operator_id", "V", operator_id2, "");
								XmlElement elemName2 = FindOrAddNodeByAttribute(ref DmlDoc, ref elemOPR2, "OPR.name", "V", first_name2 + "  " + last_name2, "");
								if (last_name2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "FAM", "V", last_name2, "");
								}
								if (first_name2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "GIV", "V", first_name2, "");
								}
								if (title2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "PFX", "V", title2, "");
								}
								XmlElement elemACC2 = (XmlElement)elemOPR2.SelectSingleNode("ACC");
								if (elemACC2 == null)
								{
									elemACC2 = DmlDoc.CreateElement("ACC");
									elemOPR2.AppendChild(elemACC2);
								}
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC2, "ACC.method_cd", "V", method_cd2, "");
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC2, "ACC.permission_level_cd", "V", privilege2.ToString(), "");
								if (m_inst_class.CompareTo("StatStrip") != 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC2, "ACC.password", "V", pswd, "");
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC2, "ACC.active_date", "V", DateTime2DML(cert_start_date2), "");
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemACC2, "ACC.expiration_date", "V", DateTime2DML(cert_end_date2), "");
								}
								if (msg_create_dttm2 != DateTime.MinValue)
								{
									XmlElement elemNTE2 = (XmlElement)elemOPR2.SelectSingleNode("NTE");
									if (elemNTE2 == null)
									{
										elemNTE2 = DmlDoc.CreateElement("NTE");
										elemOPR2.AppendChild(elemNTE2);
									}
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemNTE2, "NTE.text", "V", opr_message2, DateTime2DMLCenti(msg_create_dttm2));
								}
								if (elemACC2.GetElementsByTagName("ACC.method_cd").Count == 1)
								{
									addCount++;
									totalCount++;
								}
								preOpID2 = operator_id2;
							}
							myRuntimeDBReadReader.Close();
						}
					}
					if (delCount > 0 || addCount > 0)
					{
						if (delCount == 0 && delNode != null)
						{
							root.RemoveChild(delNode);
						}
						if (addCount == 0 && addNode != null)
						{
							root.RemoveChild(addNode);
						}
						string opList = DmlDoc.OuterXml;
						m_waiting = true;
						SendString(opList, isPartial: false, trunc: false);
						LastContinuousOperatorSend = CalcContinuousMinuteCount();
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
			handleDBException(e, "processing operator list", "SendOperatorList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing operator list", "SendOperatorList", "Protocol");
		}
		int iRet = ((totalCount > 0) ? ((delCount > 0 || addCount > 0) ? 1 : (-1)) : 0);
		if (totalCount > 0 || delCount > 0 || addCount > 0)
		{
			if (isIncremental)
			{
				UpdateOperatorNumInDevice(totalDelete + delCount, totalAdd + addCount, isCompleteList: false);
				Incremental = true;
				count = totalAdd + addCount;
				dCount = totalDelete + delCount;
			}
			else
			{
				UpdateOperatorNumInDevice(0, totalCount, isCompleteList: true);
				Incremental = false;
				count = totalCount;
				dCount = 0;
			}
		}
		return iRet;
	}

	private int SendPatientList(out bool Incremental, out int count, out int delCount)
	{
		int totalCount = 0;
		bool bProceed = false;
		CompletePatientList = null;
		int patCount = 0;
		Incremental = false;
		count = 0;
		delCount = 0;
		try
		{
			if ((!bIsListCreator) ? ((m_PatListFullSupported || m_PatListIncrSupported) && m_loc_num.Length > 0 && (!m_isContinuous || CalcContinuousMinuteCount() >= LastContinuousPatientSend + PatListFreq || m_ContinuousMinuteCount < m_LastContinuousMinuteCount || m_AlwaysSend)) : (LastListsPatientBuild == 0 || m_ListsMinuteCount >= LastListsPatientBuild + PatListFreq || m_AlwaysSend))
			{
				string patients_update_date;
				if (bIsListCreator)
				{
					if (LastListsPatientBuild == 0)
					{
						m_LastPatientListDateTime = DateTime.Parse("2007-01-01 00:00:00");
						m_PrevPatientListDateTime = DateTime.Parse("2007-01-01 00:00:00");
						patCount = 1;
					}
					list_patient_update_datetime = m_LastPatientListDateTime;
					patients_update_date = m_LastPatientListDateTime.ToString("yyyy-MM-dd HH:mm:ss");
					if (LastListsPatientBuild > 0)
					{
						myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where data_type = 'PATIENTS' and last_update_time >= '{patients_update_date}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						patCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
					}
				}
				else
				{
					patients_update_date = device_patient_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
					m_last_eot_update_time = DateTime.Now;
					DateTime AsOf = DateTime.MinValue;
					if (m_DMLICPBase.m_LocationMessageList.GetPatientList(m_loc_num, ref CompletePatientList, bPrev: false, ref AsOf))
					{
						myRuntimeDBReadCommand.CommandText = $"SELECT total_patients FROM DBA.instruments WHERE (inst_id = '{m_serial_id}')";
						object getObj = myRuntimeDBReadCommand.ExecuteScalar();
						if (!DBNull.Value.Equals(getObj))
						{
							deviceTotalPatients = Convert.ToInt32(getObj);
						}
						if (deviceTotalPatients < 0 || DateTime.Compare(AsOf, device_patient_update_datetime) > 0)
						{
							patCount = 1;
						}
					}
				}
				if (patCount > 0)
				{
					if (bIsListCreator)
					{
						if (m_DMLICPBase.m_firstTime)
						{
							m_DMLICPBase.m_firstTime = false;
							m_loc_list_update_dttm = DateTime2DML(DateTime.Now);
						}
						else
						{
							string locations_update_date = DMLToSystemDateTime(m_loc_list_update_dttm).ToString("yyyy-MM-dd HH:mm:ss");
							myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where data_type = 'LOCATIONS' and last_update_time >= '{locations_update_date}'";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							int locCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
							if (locCount > 0)
							{
								m_DMLICPBase.ReloadLocations(ref m_NNBase);
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log("Reload locations", isXml: false, "SendPatientList");
								}
							}
							m_loc_list_update_dttm = DateTime2DML(DateTime.Now);
						}
						LocationRec plocation = null;
						for (plocation = m_DMLICPBase.m_LocationList.FirstLocation(); plocation != null; plocation = m_DMLICPBase.m_LocationList.NextLocation())
						{
							m_loc_num = plocation.get_m_loc_num();
							if (LastListsPatientBuild > 0)
							{
								myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}' and data_type = 'PATIENTS' and last_update_time >= '{patients_update_date}'";
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
								}
								patCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
							}
							if (patCount > 0)
							{
								GetFacilityAndLocationByLocNum();
								DateTime AsOf = m_ListsNow;
								CompletePatientList = BuildCompletePatientList(AsOf);
								LocationMessageRec locmsgrec = m_DMLICPBase.m_LocationMessageList.SetCurrentPatientList(m_loc_num, CompletePatientList, AsOf);
								if (locmsgrec != null && m_NNBase.m_isLogging)
								{
									int ListNum = locmsgrec.GetCurrentListNum();
									m_NNBase.log("patient list for location " + m_facility + "." + m_location + " as of " + AsOf.ToString("yyyy-MM-dd HH:mm:ss") + " stored in list " + ListNum, isXml: false, "SendPatientList");
								}
							}
						}
						LastListsPatientBuild = m_ListsMinuteCount;
						m_PrevPatientListDateTime = m_LastPatientListDateTime;
						m_LastPatientListDateTime = m_ListsNow;
						ICPMGR.PrevPatListBuildDttm = m_PrevPatientListDateTime;
						ICPMGR.LastPatListBuildDttm = m_LastPatientListDateTime;
					}
					else
					{
						m_PrevPatientListDateTime = ICPMGR.PrevPatListBuildDttm;
						m_LastPatientListDateTime = ICPMGR.LastPatListBuildDttm;
						if (m_LastTimeFullList && m_PatListFullSupported && DateTime.Compare(device_patient_update_datetime, m_PrevPatientListDateTime) > 0)
						{
							if (DateTime.Compare(device_patient_update_datetime, m_LastPatientListDateTime) > 0)
							{
								device_patient_update_datetime = m_LastPatientListDateTime;
							}
							else
							{
								device_patient_update_datetime = m_PrevPatientListDateTime;
							}
						}
						if (ShallUsePatIncrementalList(out var reqPatInFacility))
						{
							totalCount = SendPatIncrementalList(reqPatInFacility, out var subCount, out var subdelCount);
							if (totalCount < 0)
							{
								totalCount = SendCompletePatientList();
								m_LastTimeFullList = true;
								count = totalCount;
								delCount = 0;
							}
							else
							{
								m_LastTimeFullList = false;
								count = subCount;
								delCount = subdelCount;
								Incremental = true;
							}
						}
						else
						{
							totalCount = (count = SendCompletePatientList());
							delCount = 0;
							m_LastTimeFullList = true;
						}
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
			handleDBException(e, "processing patient list", "SendPatientList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing patient list", "SendPatientList", "Protocol");
		}
		if (CompletePatientList != null)
		{
			CompletePatientList.ClearList();
			CompletePatientList = null;
		}
		try
		{
			GC.Collect();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e3)
		{
			handleException(e3, "Disposing patient list", "SendPatientList", "Protocol");
		}
		return totalCount;
	}

	private bool ShallUsePatIncrementalList(out bool reqPatInFacility)
	{
		reqPatInFacility = false;
		if (!m_PatListFullSupported)
		{
			return true;
		}
		if (deviceTotalPatients < 0)
		{
			return false;
		}
		try
		{
			myRuntimeDBReadCommand.CommandText = $"SELECT last_pat_dload FROM DBA.instruments  WHERE inst_id = '{m_serial_id}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			object patDownload = myRuntimeDBReadCommand.ExecuteScalar();
			if (DBNull.Value.Equals(patDownload))
			{
				return false;
			}
			DateTime patDownloadTime = Convert.ToDateTime(patDownload);
			if (patDownloadTime < new DateTime(2001, 1, 1, 0, 59, 59))
			{
				return false;
			}
			if ((DateTime.Now - device_patient_update_datetime).TotalDays > 14.0)
			{
				return false;
			}
			string patients_update_date = device_patient_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
			myRuntimeDBReadCommand.CommandText = $"SELECT _value FROM DBA.config_data c join DBA.loc_to_config l2c on c.config_num = l2c.config_num where _key = 'PatIdTypeCd*V' and l2c.loc_num = '{m_loc_num}' and c.datetime_stamp > '{patients_update_date}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			string newPatIdType = string.Empty;
			if (myRuntimeDBReadReader.Read())
			{
				newPatIdType = myRuntimeDBReadReader.GetString(0);
			}
			myRuntimeDBReadReader.Close();
			if (newPatIdType.Length > 0)
			{
				m_loc_def_pat_id = newPatIdType;
				return false;
			}
			string restrict = RestrictByLocation(m_loc_num);
			if (restrict == "F")
			{
				reqPatInFacility = true;
				myRuntimeDBReadCommand.CommandText = $"SELECT COUNT(patient_uuid) FROM DBA.PATIENT_INCREMENTAL_D WHERE (facil_num = '{m_facil_num}' AND last_update_dttm > '{patients_update_date}') ";
			}
			else
			{
				myRuntimeDBReadCommand.CommandText = $"SELECT COUNT(patient_uuid) FROM DBA.PATIENT_INCREMENTAL_D WHERE (loc_num = '{m_loc_num}' AND last_update_dttm > '{patients_update_date}') ";
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			int dCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
			if (restrict == "F")
			{
				myRuntimeDBReadCommand.CommandText = string.Format("SELECT COUNT(*) FROM DBA.patients_view WHERE ((patient_last_activity_date > '{0}' OR account_last_activity_date > '{0}' OR visit_last_activity_date > '{0}') AND facil_num = '{1}' AND dListFlag != 'T')", patients_update_date, m_facil_num);
			}
			else
			{
				myRuntimeDBReadCommand.CommandText = string.Format("SELECT COUNT(*) FROM DBA.patients_view WHERE ((patient_last_activity_date > '{0}' OR account_last_activity_date > '{0}' OR visit_last_activity_date > '{0}') AND loc_num = '{1}' AND dListFlag != 'T')", patients_update_date, m_loc_num);
			}
			int iCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
			if (dCount + iCount > MaxAddDelPerIncr)
			{
				return false;
			}
			if (deviceTotalPatients - dCount + iCount > m_maxVisitLocations)
			{
				return false;
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "determining if incremental list can be sent", "ShallUsePatIncrementalList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "determining if incremental list can be sent", "ShallUsePatIncrementalList", "Protocol");
		}
		return true;
	}

	private int SendPatIncrementalList(bool reqPatInFacility, out int count, out int delCount)
	{
		int iret = -1;
		count = 0;
		delCount = 0;
		try
		{
			Dictionary<string, Delete_Patient> dList = new Dictionary<string, Delete_Patient>();
			string patIDType = string.Empty;
			if (m_loc_def_pat_id.Length == 0 || m_loc_def_pat_id == "PATID")
			{
				patIDType = "patient_id";
			}
			else if (m_loc_def_pat_id == "MRN")
			{
				patIDType = "medrec_num";
			}
			else if (m_loc_def_pat_id == "ACCT")
			{
				patIDType = "account_num";
			}
			string patients_update_date = device_patient_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
			if (!reqPatInFacility)
			{
				myRuntimeDBReadCommand.CommandText = $"SELECT patient_uuid, patient_id, medrec_num, account_num FROM DBA.PATIENT_INCREMENTAL_D WHERE (last_update_dttm > '{patients_update_date}' AND loc_num = '{m_loc_num}') ORDER BY last_update_dttm ASC";
			}
			else
			{
				myRuntimeDBReadCommand.CommandText = $"SELECT patient_uuid, patient_id, medrec_num, account_num FROM DBA.PATIENT_INCREMENTAL_D WHERE (last_update_dttm > '{patients_update_date}' AND facil_num = '{m_facil_num}') ORDER BY last_update_dttm ASC";
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			while (myRuntimeDBReadReader.Read())
			{
				string patient_uuid = myRuntimeDBReadReader.GetString(0);
				if (!dList.ContainsKey(patient_uuid))
				{
					Delete_Patient oneRec = ANewDelPatRecord();
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(1)))
					{
						oneRec.patientID = myRuntimeDBReadReader.GetString(1);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(2)))
					{
						oneRec.mrn = myRuntimeDBReadReader.GetString(2);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(3)))
					{
						oneRec.accountNum = myRuntimeDBReadReader.GetString(3);
					}
					dList.Add(patient_uuid, oneRec);
				}
			}
			myRuntimeDBReadReader.Close();
			string sqlSel = "SELECT Patient_ID,medrec_num,account_num,Last_Name,First_Name,Middle_Name,Sex,birthdate,prefix, suffix,race, diagnosis,height,height_units,weight,weight_units,bed_num,room_num,loc_name, Consult_Physician,Report_Physician,Attend_Physician, fac_name, Notes FROM DBA.patients_view ";
			if (!reqPatInFacility)
			{
				myRuntimeDBReadCommand.CommandText = string.Format("{0} WHERE (loc_num = '{1}' AND ((patient_last_activity_date > '{2}') OR (account_last_activity_date > '{2}')  OR (visit_last_activity_date > '{2}')) AND (dListFlag IS NULL OR dListFlag = 'F'))", sqlSel, m_loc_num, patients_update_date);
			}
			else
			{
				myRuntimeDBReadCommand.CommandText = string.Format("{0} WHERE (facil_num = '{1}' AND (patient_last_activity_date > '{2}' OR account_last_activity_date > '{2}'  OR (visit_last_activity_date > '{2}')) AND (dListFlag IS NULL OR dListFlag = 'F'))", sqlSel, m_facil_num, patients_update_date);
			}
			myRuntimeDBReadCommand.CommandText += $" AND (deprecated_for_{patIDType} = 'F')";
			List<Insert_Patient> iList = new List<Insert_Patient>();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			while (myRuntimeDBReadReader.Read())
			{
				Insert_Patient newOne = ANewInsertPatRecord();
				try
				{
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(0)))
					{
						newOne.patientID = myRuntimeDBReadReader.GetString(0);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(1)))
					{
						newOne.mrn = myRuntimeDBReadReader.GetString(1);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(2)))
					{
						newOne.accountNum = myRuntimeDBReadReader.GetString(2);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(3)))
					{
						newOne.lastName = myRuntimeDBReadReader.GetString(3);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(4)))
					{
						newOne.firstName = myRuntimeDBReadReader.GetString(4);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(5)))
					{
						newOne.middleName = myRuntimeDBReadReader.GetString(5);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(6)))
					{
						newOne.sex = myRuntimeDBReadReader.GetString(6).ToUpper();
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(7)))
					{
						newOne.birthDate = myRuntimeDBReadReader.GetDateTime(7).ToString("yyyy-MM-dd");
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(8)))
					{
						newOne.prefix = myRuntimeDBReadReader.GetString(8);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(9)))
					{
						newOne.suffix = myRuntimeDBReadReader.GetString(9);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(10)))
					{
						newOne.race = myRuntimeDBReadReader.GetString(10);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(11)))
					{
						newOne.diagnosis = myRuntimeDBReadReader.GetString(11);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(12)))
					{
						newOne.height = myRuntimeDBReadReader.GetString(12);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(13)))
					{
						newOne.hUnit = myRuntimeDBReadReader.GetString(13);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(14)))
					{
						newOne.weight = myRuntimeDBReadReader.GetString(14);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(15)))
					{
						newOne.wUnit = myRuntimeDBReadReader.GetString(15);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(16)))
					{
						newOne.bedNum = myRuntimeDBReadReader.GetString(16);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(17)))
					{
						newOne.roomNum = myRuntimeDBReadReader.GetString(17);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(18)))
					{
						newOne.location = myRuntimeDBReadReader.GetString(18);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(19)))
					{
						newOne.cPhysician = myRuntimeDBReadReader.GetString(19);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(20)))
					{
						newOne.rPhysician = myRuntimeDBReadReader.GetString(20);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(21)))
					{
						newOne.aPhysician = myRuntimeDBReadReader.GetString(21);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(22)))
					{
						newOne.facility = myRuntimeDBReadReader.GetString(22);
					}
					if (!DBNull.Value.Equals(myRuntimeDBReadReader.GetValue(23)))
					{
						newOne.Notes = myRuntimeDBReadReader.GetString(23);
					}
					iList.Add(newOne);
				}
				catch (ThreadAbortException)
				{
					myRuntimeDBReadReader.Close();
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e)
				{
					m_NNBase.ReportDBException(e, "building patient incremental list", "SendPatientIncrementalList");
				}
				catch (Exception e2)
				{
					m_NNBase.ReportException(e2, "building patient incremental list", "SendPatientIncrementalList");
				}
			}
			myRuntimeDBReadReader.Close();
			int dCount = dList.Count;
			int iCount = iList.Count;
			if (dCount + iCount == 0)
			{
				return 0;
			}
			if (deviceTotalPatients - dCount + iCount > m_maxVisitLocations)
			{
				return -1;
			}
			try
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("incremental patient list for location " + m_facility + "." + m_location + " as of " + DateTime.Now, isXml: false, "SendPatientList");
				}
				string patList = string.Empty;
				MemoryStream stream = new MemoryStream();
				XmlWriterSettings setttings = new XmlWriterSettings();
				setttings.OmitXmlDeclaration = true;
				XmlWriter writer = XmlWriter.Create(stream, setttings);
				writer.WriteStartDocument();
				writer.WriteStartElement("PTL.R02");
				AddDMLHeader(ref writer);
				if (dList.Count > 0)
				{
					writer.WriteStartElement("UPD");
					writer.WriteStartElement("UPD.action_cd");
					writer.WriteAttributeString("V", "D");
					writer.WriteEndElement();
					foreach (Delete_Patient oneRecord in dList.Values)
					{
						string effectiveID = string.Empty;
						switch (m_loc_def_pat_id)
						{
						case "PATID":
							effectiveID = oneRecord.patientID;
							break;
						case "MRN":
							effectiveID = oneRecord.mrn;
							break;
						case "ACCT":
							effectiveID = oneRecord.accountNum;
							break;
						}
						if (m_inst_class.CompareTo("StatStrip") != 0 || !string.IsNullOrEmpty(effectiveID))
						{
							writer.WriteStartElement("PT");
							writer.WriteStartElement("PT.patient_id");
							if (m_inst_class.CompareTo("StatStrip") == 0)
							{
								writer.WriteAttributeString("V", effectiveID);
								writer.WriteString(m_loc_def_pat_id);
							}
							else
							{
								writer.WriteAttributeString("V", effectiveID);
								writer.WriteString(oneRecord.mrn + "^" + oneRecord.patientID + "^" + oneRecord.accountNum);
							}
							writer.WriteEndElement();
							writer.WriteEndElement();
						}
					}
					writer.WriteEndElement();
				}
				if (iList.Count > 0)
				{
					int indexCount = 0;
					bool firstTime = true;
					foreach (Insert_Patient oneRecord2 in iList)
					{
						if (indexCount > 4)
						{
							writer.Flush();
							if (stream.Length + recSize * 5 > Convert.ToInt32(m_max_message_sz))
							{
								writer.WriteEndElement();
								writer.WriteEndElement();
								writer.WriteEndDocument();
								writer.Flush();
								writer.Close();
								StreamReader reader1 = new StreamReader(stream, Encoding.UTF8, detectEncodingFromByteOrderMarks: true);
								stream.Seek(0L, SeekOrigin.Begin);
								patList = reader1.ReadToEnd();
								m_waiting = true;
								SendString(patList, isPartial: true, trunc: false);
								LastContinuousPatientSend = CalcContinuousMinuteCount();
								OnReadComplete();
								indexCount = 0;
								stream.SetLength(0L);
								writer = XmlWriter.Create(stream, setttings);
								writer.WriteStartDocument();
								writer.WriteStartElement("PTL.R02");
								AddDMLHeader(ref writer);
								writer.WriteStartElement("UPD");
								writer.WriteStartElement("UPD.action_cd");
								writer.WriteAttributeString("V", "I");
								writer.WriteEndElement();
							}
						}
						else
						{
							if (indexCount == 0 && firstTime)
							{
								writer.WriteStartElement("UPD");
								writer.WriteStartElement("UPD.action_cd");
								writer.WriteAttributeString("V", "I");
								writer.WriteEndElement();
								firstTime = false;
							}
							indexCount++;
						}
						string effectiveID2 = string.Empty;
						switch (m_loc_def_pat_id)
						{
						case "PATID":
							effectiveID2 = oneRecord2.patientID;
							break;
						case "MRN":
							effectiveID2 = oneRecord2.mrn;
							break;
						case "ACCT":
							effectiveID2 = oneRecord2.accountNum;
							break;
						}
						if (string.IsNullOrEmpty(effectiveID2) && m_inst_class.CompareTo("StatStrip") == 0)
						{
							continue;
						}
						writer.WriteStartElement("PT");
						writer.WriteStartElement("PT.patient_id");
						if (m_inst_class.CompareTo("StatStrip") == 0)
						{
							writer.WriteAttributeString("V", effectiveID2);
							writer.WriteString(m_loc_def_pat_id);
						}
						else
						{
							writer.WriteAttributeString("V", effectiveID2);
							writer.WriteString(oneRecord2.mrn + "^" + oneRecord2.patientID + "^" + oneRecord2.accountNum);
						}
						writer.WriteEndElement();
						writer.WriteStartElement("PT.location");
						string detailsLoc = oneRecord2.facility + "^" + oneRecord2.location + "^" + oneRecord2.roomNum + "^" + oneRecord2.bedNum;
						writer.WriteAttributeString("V", detailsLoc);
						writer.WriteEndElement();
						writer.WriteStartElement("PT.name");
						writer.WriteAttributeString("V", oneRecord2.firstName + " " + oneRecord2.lastName);
						writer.WriteStartElement("FAM");
						writer.WriteAttributeString("V", oneRecord2.lastName);
						writer.WriteEndElement();
						writer.WriteStartElement("GIV");
						writer.WriteAttributeString("V", oneRecord2.firstName);
						writer.WriteEndElement();
						if (!string.IsNullOrEmpty(oneRecord2.prefix))
						{
							writer.WriteStartElement("PFX");
							writer.WriteAttributeString("V", oneRecord2.prefix);
							writer.WriteEndElement();
						}
						if (!string.IsNullOrEmpty(oneRecord2.suffix))
						{
							writer.WriteStartElement("SFX");
							writer.WriteAttributeString("V", oneRecord2.suffix);
							writer.WriteEndElement();
						}
						if (!string.IsNullOrEmpty(oneRecord2.middleName))
						{
							writer.WriteStartElement("MID");
							writer.WriteAttributeString("V", oneRecord2.middleName);
							writer.WriteEndElement();
						}
						writer.WriteEndElement();
						if (!string.IsNullOrEmpty(oneRecord2.birthDate))
						{
							writer.WriteStartElement("PT.birth_date");
							writer.WriteAttributeString("V", oneRecord2.birthDate);
							writer.WriteEndElement();
						}
						writer.WriteStartElement("PT.gender_cd");
						writer.WriteAttributeString("V", oneRecord2.sex);
						writer.WriteEndElement();
						string unit = oneRecord2.wUnit;
						if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime") == 0)
						{
							if (oneRecord2.wUnit.CompareTo("LBS") == 0)
							{
								unit = "lbs";
							}
							else if (oneRecord2.wUnit.CompareTo("KG") == 0)
							{
								unit = "kg";
							}
						}
						writer.WriteStartElement("PT.weight");
						writer.WriteAttributeString("V", oneRecord2.weight);
						writer.WriteAttributeString("U", unit);
						writer.WriteEndElement();
						unit = oneRecord2.hUnit;
						if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime+") == 0)
						{
							if (oneRecord2.hUnit.CompareTo("INS") == 0)
							{
								unit = "inches";
							}
							else if (oneRecord2.hUnit.CompareTo("CM") == 0)
							{
								unit = "centimeters";
							}
						}
						writer.WriteStartElement("PT.height");
						writer.WriteAttributeString("V", oneRecord2.height);
						writer.WriteAttributeString("U", unit);
						writer.WriteEndElement();
						string race = oneRecord2.race;
						if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime+") == 0)
						{
							race = ((oneRecord2.race.CompareTo("JP") == 0) ? "J" : ((oneRecord2.race.CompareTo("B") == 0) ? oneRecord2.race : ((oneRecord2.race.CompareTo("U") != 0) ? "O" : oneRecord2.race)));
						}
						writer.WriteStartElement("PT.ethnic_cd");
						writer.WriteAttributeString("V", race);
						writer.WriteEndElement();
						writer.WriteStartElement("PT.diagnosis");
						writer.WriteAttributeString("V", oneRecord2.diagnosis);
						writer.WriteEndElement();
						writer.WriteStartElement("PT.physician");
						if (m_inst_class.CompareTo("StatStrip") == 0)
						{
							writer.WriteAttributeString("V", "");
						}
						else
						{
							writer.WriteAttributeString("V", oneRecord2.cPhysician + "^" + oneRecord2.rPhysician + "^" + oneRecord2.aPhysician);
						}
						writer.WriteEndElement();
						writer.WriteStartElement("PT.room");
						writer.WriteAttributeString("V", oneRecord2.roomNum);
						writer.WriteEndElement();
						writer.WriteStartElement("PT.bed");
						writer.WriteAttributeString("V", oneRecord2.bedNum);
						writer.WriteEndElement();
						if (oneRecord2.Notes.Length > 0)
						{
							writer.WriteStartElement("NTE");
							writer.WriteStartElement("NTE.text");
							writer.WriteAttributeString("V", oneRecord2.Notes);
							writer.WriteString(DateTime2DML(DateTime.Now));
							writer.WriteEndElement();
							writer.WriteEndElement();
						}
						writer.WriteEndElement();
					}
					writer.WriteEndElement();
				}
				writer.WriteEndElement();
				writer.WriteEndDocument();
				writer.Flush();
				writer.Close();
				StreamReader reader2 = new StreamReader(stream, Encoding.UTF8, detectEncodingFromByteOrderMarks: true);
				stream.Seek(0L, SeekOrigin.Begin);
				patList = reader2.ReadToEnd();
				m_waiting = true;
				SendString(patList, isPartial: false, trunc: false);
				LastContinuousPatientSend = CalcContinuousMinuteCount();
				UpdatePatientNumInDevice(dCount, iCount, isCompleteList: false);
				count = iCount;
				delCount = dCount;
				return dCount + iCount;
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (Exception e3)
			{
				handleException(e3, "building or sending incremental patient list", "SendPatIncrementalList", "Protocol");
				return -1;
			}
		}
		catch (ThreadAbortException)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e4)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleDBException(e4, "building or sending patient incremental list", "SendPatientIncrementalList", "Protocol");
			return -1;
		}
		catch (Exception e5)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleException(e5, "building or sending patient incremental list", "SendPatientIncrementalList", "Protocol");
			return -1;
		}
		return iret;
	}

	private int BuildPTL_R02DList(Dictionary<string, Delete_Patient> dlist)
	{
		return 0;
	}

	private Delete_Patient ANewDelPatRecord()
	{
		Delete_Patient newOne = default(Delete_Patient);
		newOne.accountNum = string.Empty;
		newOne.mrn = string.Empty;
		newOne.patientID = string.Empty;
		return newOne;
	}

	private Insert_Patient ANewInsertPatRecord()
	{
		Insert_Patient newOne = default(Insert_Patient);
		newOne.accountNum = string.Empty;
		newOne.mrn = string.Empty;
		newOne.patientID = string.Empty;
		newOne.lastName = string.Empty;
		newOne.firstName = string.Empty;
		newOne.middleName = string.Empty;
		newOne.sex = string.Empty;
		newOne.birthDate = string.Empty;
		newOne.prefix = string.Empty;
		newOne.suffix = string.Empty;
		newOne.race = string.Empty;
		newOne.diagnosis = string.Empty;
		newOne.height = string.Empty;
		newOne.hUnit = string.Empty;
		newOne.weight = string.Empty;
		newOne.wUnit = string.Empty;
		newOne.bedNum = string.Empty;
		newOne.roomNum = string.Empty;
		newOne.location = string.Empty;
		newOne.cPhysician = string.Empty;
		newOne.rPhysician = string.Empty;
		newOne.aPhysician = string.Empty;
		newOne.facility = string.Empty;
		newOne.Notes = string.Empty;
		return newOne;
	}

	private void UpdatePatientNumInDevice(int dCount, int iCount, bool isCompleteList)
	{
		if (isCompleteList)
		{
			myRuntimeDBReadCommand.CommandText = $"UPDATE DBA.instruments SET total_patients = {iCount} WHERE inst_id = '{m_serial_id}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadCommand.ExecuteNonQuery();
			return;
		}
		myRuntimeDBReadCommand.CommandText = $"SELECT total_patients FROM DBA.instruments WHERE inst_id = '{m_serial_id}'";
		int curCount = 0;
		object getObj = myRuntimeDBReadCommand.ExecuteScalar();
		if (!DBNull.Value.Equals(getObj))
		{
			curCount = Convert.ToInt32(getObj);
		}
		int updatedCount = curCount - dCount + iCount;
		myRuntimeDBReadCommand.CommandText = $"UPDATE DBA.instruments SET total_patients = {updatedCount} WHERE inst_id = '{m_serial_id}'";
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
		}
		myRuntimeDBReadCommand.ExecuteNonQuery();
	}

	private void UpdateOperatorNumInDevice(int dCount, int iCount, bool isCompleteList)
	{
		if (isCompleteList)
		{
			myRuntimeDBReadCommand.CommandText = $"UPDATE DBA.instruments SET total_operators = {iCount} WHERE inst_id = '{m_serial_id}'";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadCommand.ExecuteNonQuery();
			return;
		}
		myRuntimeDBReadCommand.CommandText = $"SELECT total_operators FROM DBA.instruments WHERE inst_id = '{m_serial_id}'";
		int curCount = 0;
		object getObj = myRuntimeDBReadCommand.ExecuteScalar();
		if (!DBNull.Value.Equals(getObj))
		{
			curCount = Convert.ToInt32(getObj);
		}
		int updatedCount = curCount - dCount + iCount;
		myRuntimeDBReadCommand.CommandText = $"UPDATE DBA.instruments SET total_operators = {updatedCount} WHERE inst_id = '{m_serial_id}'";
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
		}
		myRuntimeDBReadCommand.ExecuteNonQuery();
	}

	private PatientList BuildCompletePatientList(DateTime AsOf)
	{
		PatientList m_PatientList = new PatientList();
		try
		{
			string where = $"(loc_num = '{m_loc_num}')";
			string patient_id_field = "";
			if (m_loc_def_pat_id == "PATID")
			{
				patient_id_field = "patient_id";
			}
			else if (m_loc_def_pat_id.Length == 0 || m_loc_def_pat_id == "MRN")
			{
				patient_id_field = "medrec_num";
			}
			else if (m_loc_def_pat_id == "ACCT")
			{
				patient_id_field = "account_num";
			}
			string TopView = m_PatientList.latest_active_visit_by_patient_id(bSinglePatient: false, bRetrieveDetails: true, patient_id_field, "", m_facil_num, bByFacility: false, m_loc_num);
			string VisitOrderBy = "discharge_time desc, pv.last_update_date desc, visit_uuid";
			string TopOrderBy = "discharge_time desc, last_update_date desc, visit_uuid";
			string PatientOrderBy = "patient_id, medrec_num";
			string AccountOrderBy = "account_num";
			m_PatientList.ReadMany(m_NNBase, where, bMustHaveVisit: true, bIncludePatientDetails: true, bIncludeVisitDetails: true, bTop: true, m_maxVisitLocations, TopView, TopOrderBy, PatientOrderBy, AccountOrderBy, VisitOrderBy, ref myRuntimeDBReadCommand);
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("patient list built for location " + m_facility + "." + m_location + " as of " + AsOf.ToString("yyyy-MM-dd HH:mm:ss") + " containing " + m_PatientList.GetNumUsedElements() + " patients", isXml: false, "BuildCompletePatientList");
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing patient list", "BuildCompletePatientList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing patient list", "BuildCompletePatientList", "Protocol");
		}
		return m_PatientList;
	}

	private int SendCompletePatientList()
	{
		int addCount = 0;
		int totalCount = 0;
		XmlDocument DmlDoc = new XmlDocument();
		XmlElement root = null;
		PatientRec m_Patient = null;
		string patList = "";
		DateTime AsOf = DateTime.MinValue;
		bool firstTime = true;
		int pAccount = -1;
		try
		{
			if (CompletePatientList != null && m_NNBase.m_isLogging)
			{
				m_NNBase.log("patient list for location " + m_facility + "." + m_location + " as of " + AsOf.ToString("yyyy-MM-dd HH:mm:ss"), isXml: false, "SendPatientList");
			}
			if (CompletePatientList != null)
			{
				int numpatients = CompletePatientList.GetNumUsedElements();
				myRuntimeDBWriteCommand.CommandText = string.Format("update DBA.instruments set total_patients = -1 where inst_id = '" + m_serial_id + "'");
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBWriteCommand.ExecuteNonQuery();
				int pPatient = CompletePatientList.First();
				while (pPatient >= 0 && totalCount < m_maxVisitLocations)
				{
					m_Patient = (PatientRec)CompletePatientList.m_Array[pPatient];
					pAccount = m_Patient.m_PatientAccountList.First();
					if (m_loc_def_pat_id == "ACCT")
					{
						while (pAccount >= 0 && totalCount < m_maxVisitLocations)
						{
							AddToPatientListForAccount(ref DmlDoc, ref root, ref m_Patient, pAccount, ref firstTime, ref addCount, ref totalCount);
							pAccount = m_Patient.m_PatientAccountList.Next();
						}
					}
					else if (pAccount >= 0)
					{
						AddToPatientListForAccount(ref DmlDoc, ref root, ref m_Patient, pAccount, ref firstTime, ref addCount, ref totalCount);
					}
					pPatient = CompletePatientList.Next();
				}
				if (addCount > 0)
				{
					patList = DmlDoc.OuterXml;
					m_waiting = true;
					SendString(patList, isPartial: false, trunc: false);
					LastContinuousPatientSend = CalcContinuousMinuteCount();
				}
				if (totalCount >= numpatients)
				{
					UpdatePatientNumInDevice(0, totalCount, isCompleteList: true);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing patient list", "SendCompletePatientList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing patient list", "SendCompletePatientList", "Protocol");
		}
		return totalCount;
	}

	private void AddToPatientListForAccount(ref XmlDocument DmlDoc, ref XmlElement root, ref PatientRec m_Patient, int pAccount, ref bool firstTime, ref int addCount, ref int totalCount)
	{
		PatientAccountRec m_Account = null;
		PatientVisitRec m_Visit = null;
		string patList = "";
		string patient_id = "";
		string first_name = "";
		string last_name = "";
		string middle_name = "";
		string prefix = "";
		string suffix = "";
		DateTime birthdate = DateTime.MinValue;
		string sex = "";
		string Race = "";
		string medrec_num = "";
		int pVisit = -1;
		string account_num = "";
		_ = DateTime.MaxValue;
		string PatientNotes = "";
		string room_num = "";
		string bed_num = "";
		string Weight_value = "";
		string Weight_units = "";
		string Height_value = "";
		string Height_units = "";
		string Diagnosis = "";
		string AttendingPhysician = "";
		_ = DateTime.MinValue;
		_ = TimeSpan.MinValue;
		try
		{
			patient_id = m_Patient.m_PatientID;
			first_name = m_Patient.m_FirstName;
			first_name = first_name.Substring(0, Math.Min(first_name.Length, 16));
			last_name = m_Patient.m_LastName;
			last_name = last_name.Substring(0, Math.Min(last_name.Length, 16));
			middle_name = m_Patient.m_MiddleName;
			middle_name = middle_name.Substring(0, Math.Min(middle_name.Length, 16));
			prefix = m_Patient.m_prefix;
			suffix = m_Patient.m_suffix;
			birthdate = m_Patient.m_birthdate;
			sex = m_Patient.m_Sex.ToUpper();
			Race = m_Patient.m_race;
			medrec_num = m_Patient.m_medrecnum;
			m_Account = (PatientAccountRec)m_Patient.m_PatientAccountList.m_Array[pAccount];
			account_num = m_Account.m_accountnum;
			pVisit = m_Account.m_PatientVisitList.First();
			if (pVisit < 0)
			{
				return;
			}
			m_Visit = (PatientVisitRec)m_Account.m_PatientVisitList.m_Array[pVisit];
			_ = m_Visit.m_dischargetime;
			PatientNotes = m_Visit.m_Physician_note;
			room_num = m_Visit.m_roomnum;
			bed_num = m_Visit.m_bednum;
			_ = m_Visit.m_adddate;
			Weight_value = m_Visit.m_weight;
			Weight_units = m_Visit.m_weight_units;
			Height_value = m_Visit.m_height;
			Height_units = m_Visit.m_height_units;
			Diagnosis = m_Visit.m_diagnosis;
			AttendingPhysician = m_Visit.m_AttendPhysician;
			if (firstTime || addCount >= MaxAddDelPerMsg || DmlDoc.OuterXml.Length > int.Parse(m_max_message_sz) - addCount - 4 * recSize - 32)
			{
				if (!firstTime)
				{
					patList = DmlDoc.OuterXml;
					m_waiting = true;
					SendString(patList, isPartial: true, trunc: false);
					LastContinuousPatientSend = CalcContinuousMinuteCount();
					OnReadComplete();
				}
				addCount = 0;
				patList = "<PTL.R01>" + GenDMLHeader("Protocol") + "</PTL.R01>";
				DmlDoc.LoadXml(patList);
				root = DmlDoc.DocumentElement;
			}
			string effective_patient_id = "";
			effective_patient_id = ((m_loc_def_pat_id.Length <= 2) ? patient_id : ((m_loc_def_pat_id == "PATID" && patient_id.Length > 0) ? patient_id : ((m_loc_def_pat_id == "MRN" && medrec_num.Length > 0) ? medrec_num : ((!(m_loc_def_pat_id == "ACCT") || account_num.Length <= 0) ? patient_id : account_num))));
			XmlElement elemPT = null;
			if (m_inst_class.CompareTo("StatStrip") == 0)
			{
				if (string.IsNullOrEmpty(effective_patient_id))
				{
					return;
				}
				elemPT = FindOrCreateChildNode(ref DmlDoc, ref root, "PT", "PT.patient_id", "V", effective_patient_id, m_loc_def_pat_id);
			}
			else
			{
				string myText = medrec_num + "^" + patient_id + "^" + account_num;
				elemPT = FindOrCreateChildNode(ref DmlDoc, ref root, "PT", "PT.patient_id", "V", effective_patient_id, myText);
			}
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.location", "V", m_facility + "^" + m_location + "^" + room_num + "^" + bed_num, "");
			XmlElement elemName = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.name", "V", first_name + "  " + last_name, "");
			if (last_name.Length > 0)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "FAM", "V", last_name, "");
			}
			if (first_name.Length > 0)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "GIV", "V", first_name, "");
			}
			if (middle_name.Length > 0)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "MID", "V", middle_name, "");
			}
			if (prefix.Length > 0)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "PFX", "V", prefix, "");
			}
			if (suffix.Length > 0)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "SFX", "V", suffix, "");
			}
			if (birthdate.Year > 1)
			{
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.birth_date", "V", birthdate.ToString("yyyy-MM-dd"), "");
			}
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.gender_cd", "V", sex, "");
			XmlElement elmW = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.weight", "V", Weight_value, "");
			if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime+") == 0)
			{
				if (Weight_units.CompareTo("LBS") == 0)
				{
					Weight_units = "lbs";
				}
				else if (Weight_units.CompareTo("KG") == 0)
				{
					Weight_units = "kg";
				}
			}
			elmW.SetAttribute("U", Weight_units);
			XmlElement elmH = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.height", "V", Height_value, "");
			if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime+") == 0)
			{
				if (Height_units.CompareTo("INS") == 0)
				{
					Height_units = "inches";
				}
				else if (Height_units.CompareTo("CM") == 0)
				{
					Height_units = "centimeters";
				}
			}
			elmH.SetAttribute("U", Height_units);
			if (m_inst_class.CompareTo("pHOx Ultra") == 0 || m_inst_class.CompareTo("Prime+") == 0)
			{
				if (Race.CompareTo("JP") == 0)
				{
					Race = "J";
				}
				else if (Race.CompareTo("B") != 0 && Race.CompareTo("U") != 0)
				{
					Race = "O";
				}
			}
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.ethnic_cd", "V", Race, "");
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.diagnosis", "V", Diagnosis, "");
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.physician", "V", AttendingPhysician, "");
			if (PatientNotes.Length > 0)
			{
				XmlElement elemNTE = (XmlElement)elemPT.SelectSingleNode("NTE");
				if (elemNTE == null)
				{
					elemNTE = DmlDoc.CreateElement("NTE");
					elemPT.AppendChild(elemNTE);
				}
				FindOrAddNodeByAttribute(ref DmlDoc, ref elemNTE, "NTE.text", "V", PatientNotes, DateTime2DML(DateTime.Now));
			}
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.room", "V", room_num, "");
			FindOrAddNodeByAttribute(ref DmlDoc, ref elemPT, "PT.bed", "V", bed_num, "");
			addCount++;
			totalCount++;
			firstTime = false;
			pVisit = -1;
			pAccount = -1;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing patient list", "AddToPatientListForAccount", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing patient list", "AddToPatientListForAccount", "Protocol");
		}
	}

	private int SendPhysicianList()
	{
		int delCount = 0;
		int addCount = 0;
		int totalCount = 0;
		XmlDocument DmlDoc = new XmlDocument();
		try
		{
			if ((m_PhysListFullSupported || m_PhysListIncrSupported) && m_loc_num.Length > 0)
			{
				DateTime physician_update_datetime = DMLToSystemDateTime(m_phys_update_dttm);
				string physicians_update_date = physician_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - physician_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}' and data_type = 'PHYSICIANS' and last_update_time >= '{physicians_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				int phCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (m_AlwaysSend || phCount > 0 || (phCount == 0 && ts.TotalDays > 365.0))
				{
					XmlElement root = null;
					XmlElement delNode = null;
					XmlElement addNode = null;
					bool isIncremental = false;
					if (m_PhysListIncrSupported && ts.TotalDays < 365.0)
					{
						myRuntimeDBReadCommand.CommandText = string.Format("SELECT count(*) FROM DBA.physicians p  join DBA.physician_to_unit p2u on p.physician_id = p2u.physician_id WHERE loc_num = '{0}' AND ( p.datetime_stamp > '{1}' OR  p.last_update_date > '{1}' OR  p2u.datetime_stamp > '{1}' OR  p2u.last_update_date > '{1}' )", m_loc_num, physicians_update_date);
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						int updateCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(updateCount + " new physicians...", isXml: false, "ICPMGR");
						}
						if (updateCount > 0 && updateCount <= MaxAddDelPerIncr)
						{
							isIncremental = true;
							m_last_incremental = "I";
							myRuntimeDBReadCommand.CommandText = string.Format("SELECT DISTINCT p.physician_id, first_name, last_name, middle_name, prefix, suffix, is_active, add_date FROM DBA.physicians p  join DBA.physician_to_unit p2u on p.physician_id = p2u.physician_id WHERE loc_num = '{0}' AND ( p.datetime_stamp > '{1}' OR  p.last_update_date > '{1}' OR  p2u.datetime_stamp > '{1}' OR  p2u.last_update_date > '{1}' )", m_loc_num, physicians_update_date);
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							bool firstTime = true;
							int recSize = 2048;
							while (myRuntimeDBReadReader.Read())
							{
								string physician_id = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
								string first_name = (myRuntimeDBReadReader.IsDBNull(1) ? "U" : myRuntimeDBReadReader.GetString(1));
								first_name = first_name.Substring(0, Math.Min(first_name.Length, 16));
								string last_name = (myRuntimeDBReadReader.IsDBNull(2) ? "U" : myRuntimeDBReadReader.GetString(2));
								last_name = last_name.Substring(0, Math.Min(last_name.Length, 16));
								string middle_name = (myRuntimeDBReadReader.IsDBNull(3) ? "U" : myRuntimeDBReadReader.GetString(3));
								middle_name = middle_name.Substring(0, Math.Min(middle_name.Length, 16));
								string prefix = (myRuntimeDBReadReader.IsDBNull(4) ? "U" : myRuntimeDBReadReader.GetString(4));
								string suffix = (myRuntimeDBReadReader.IsDBNull(5) ? "U" : myRuntimeDBReadReader.GetString(5));
								string is_active = (myRuntimeDBReadReader.IsDBNull(6) ? "U" : myRuntimeDBReadReader.GetString(6));
								DateTime add_date = (myRuntimeDBReadReader.IsDBNull(7) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(7));
								_ = DateTime.Now;
								if (firstTime || addCount + delCount >= MaxAddDelPerMsg || DmlDoc.OuterXml.Length > int.Parse(m_max_message_sz) - delCount - addCount - 4 * recSize - 32)
								{
									string physList;
									if (!firstTime)
									{
										if (delCount == 0)
										{
											root.RemoveChild(delNode);
										}
										if (addCount == 0)
										{
											root.RemoveChild(addNode);
										}
										physList = DmlDoc.OuterXml;
										m_waiting = true;
										SendString(physList, isPartial: true, trunc: false);
										OnReadComplete();
									}
									delCount = 0;
									addCount = 0;
									physList = "<NOVA.PHYS.R02>" + GenDMLHeader("Protocol") + "<UPD></UPD><UPD></UPD></NOVA.PHYS.R02>";
									DmlDoc.LoadXml(physList);
									root = DmlDoc.DocumentElement;
									XmlNodeList UPDnodeList = root.SelectNodes("UPD");
									delNode = (XmlElement)UPDnodeList.Item(0);
									XmlElement updElemD = DmlDoc.CreateElement("UPD.action_cd");
									updElemD.SetAttribute("V", "D");
									delNode.AppendChild(updElemD);
									addNode = (XmlElement)UPDnodeList.Item(1);
									XmlElement updElemI = DmlDoc.CreateElement("UPD.action_cd");
									updElemI.SetAttribute("V", "I");
									addNode.AppendChild(updElemI);
								}
								XmlElement elemPHYS = FindOrCreateChildNode(ref DmlDoc, ref delNode, "PHYS", "PHYS.physician_id", "V", physician_id, "");
								delCount++;
								totalCount++;
								if ((is_active == "F" || add_date < physician_update_datetime) && delCount == 0 && addCount == 0)
								{
									recSize = elemPHYS.OuterXml.Length;
								}
								if (is_active != "F")
								{
									elemPHYS = FindOrCreateChildNode(ref DmlDoc, ref addNode, "PHYS", "PHYS.physician_id", "V", physician_id, "");
									XmlElement elemName = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPHYS, "PHYS.name", "V", first_name + "  " + last_name, "");
									if (last_name.Length > 0)
									{
										FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "FAM", "V", last_name, "");
									}
									if (first_name.Length > 0)
									{
										FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "GIV", "V", first_name, "");
									}
									if (middle_name.Length > 0)
									{
										FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "MID", "V", middle_name, "");
									}
									if (prefix.Length > 0)
									{
										FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "PFX", "V", prefix, "");
									}
									if (suffix.Length > 0)
									{
										FindOrAddNodeByAttribute(ref DmlDoc, ref elemName, "SFX", "V", suffix, "");
									}
									if (addCount == 0)
									{
										recSize = elemPHYS.OuterXml.Length;
									}
									addCount++;
									totalCount++;
								}
								firstTime = false;
							}
							myRuntimeDBReadReader.Close();
							if (totalCount == 0 && m_AlwaysSend)
							{
								isIncremental = false;
							}
						}
					}
					if (!isIncremental)
					{
						m_last_incremental = "C";
						if (m_PhysListIncrSupported || m_PhysListFullSupported)
						{
							myRuntimeDBReadCommand.CommandText = $"SELECT DISTINCT p.physician_id, first_name, last_name, middle_name,  prefix, suffix FROM DBA.physicians p join DBA.physician_to_unit p2u on p.physician_id = p2u.physician_id  WHERE loc_num = '{m_loc_num}' AND is_active = 'T' ";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							bool firstTime2 = true;
							int recSize2 = 2048;
							while (myRuntimeDBReadReader.Read())
							{
								string physician_id2 = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
								string first_name2 = (myRuntimeDBReadReader.IsDBNull(1) ? "U" : myRuntimeDBReadReader.GetString(1));
								first_name2 = first_name2.Substring(0, Math.Min(first_name2.Length, 16));
								string last_name2 = (myRuntimeDBReadReader.IsDBNull(2) ? "U" : myRuntimeDBReadReader.GetString(2));
								last_name2 = last_name2.Substring(0, Math.Min(last_name2.Length, 16));
								string middle_name2 = (myRuntimeDBReadReader.IsDBNull(3) ? "U" : myRuntimeDBReadReader.GetString(3));
								middle_name2 = middle_name2.Substring(0, Math.Min(middle_name2.Length, 16));
								string prefix2 = (myRuntimeDBReadReader.IsDBNull(4) ? "U" : myRuntimeDBReadReader.GetString(4));
								string suffix2 = (myRuntimeDBReadReader.IsDBNull(5) ? "U" : myRuntimeDBReadReader.GetString(5));
								_ = DateTime.Now;
								if (firstTime2 || addCount >= MaxAddDelPerMsg || DmlDoc.OuterXml.Length > int.Parse(m_max_message_sz) - addCount - 4 * recSize2 - 32)
								{
									string physList2;
									if (!firstTime2)
									{
										physList2 = DmlDoc.OuterXml;
										m_waiting = true;
										SendString(physList2, isPartial: true, trunc: false);
										OnReadComplete();
									}
									addCount = 0;
									physList2 = "<NOVA.PHYS.R01>" + GenDMLHeader("Protocol") + "</NOVA.PHYS.R01>";
									DmlDoc.LoadXml(physList2);
									root = DmlDoc.DocumentElement;
								}
								XmlElement elemPHYS2 = FindOrCreateChildNode(ref DmlDoc, ref root, "PHYS", "PHYS.physician_id", "V", physician_id2, "");
								XmlElement elemName2 = FindOrAddNodeByAttribute(ref DmlDoc, ref elemPHYS2, "PHYS.name", "V", first_name2 + "  " + last_name2, "");
								if (last_name2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "FAM", "V", last_name2, "");
								}
								if (first_name2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "GIV", "V", first_name2, "");
								}
								if (middle_name2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "MID", "V", middle_name2, "");
								}
								if (prefix2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "PFX", "V", prefix2, "");
								}
								if (suffix2.Length > 0)
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemName2, "SFX", "V", suffix2, "");
								}
								if (addCount == 0)
								{
									recSize2 = elemPHYS2.OuterXml.Length;
								}
								addCount++;
								totalCount++;
								firstTime2 = false;
							}
							myRuntimeDBReadReader.Close();
						}
					}
					if (delCount > 0 || addCount > 0)
					{
						if (delCount == 0 && delNode != null)
						{
							root.RemoveChild(delNode);
						}
						if (addCount == 0 && addNode != null)
						{
							root.RemoveChild(addNode);
						}
						string physList3 = DmlDoc.OuterXml;
						m_waiting = true;
						SendString(physList3, isPartial: false, trunc: false);
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
			handleDBException(e, "processing physician list", "SendPhysicianList", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing physician list", "SendPhysicianList", "Protocol");
		}
		if (totalCount <= 0)
		{
			return 0;
		}
		if (delCount <= 0 && addCount <= 0)
		{
			return -1;
		}
		return 1;
	}

	private bool SendLocationList()
	{
		int addCount = 0;
		int totalCount = 0;
		XmlDocument DmlDoc = new XmlDocument();
		try
		{
			if (m_LocListSupported)
			{
				DateTime loc_list_update_datetime = DMLToSystemDateTime(m_loc_list_update_dttm);
				string locations_update_date = loc_list_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - loc_list_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where data_type = 'LOCATIONS' and last_update_time >= '{locations_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				int locCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (m_AlwaysSend || locCount > 0 || (locCount == 0 && ts.TotalDays > 365.0) || m_was_unassigned)
				{
					XmlElement root = null;
					myRuntimeDBReadCommand.CommandText = $"SELECT loc_name FROM DBA.inst_locations WHERE level_num = 1";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					string[] facs = new string[200];
					int j = 0;
					while (myRuntimeDBReadReader.Read() && j < 200)
					{
						if (!myRuntimeDBReadReader.IsDBNull(0))
						{
							facs[j++] = myRuntimeDBReadReader.GetString(0);
						}
					}
					myRuntimeDBReadReader.Close();
					bool firstTime = true;
					int recSize = 2048;
					for (int i = 0; i < j; i++)
					{
						XmlElement elemLOC = DmlDoc.CreateElement("LOC");
						XmlElement elemLOCfac = DmlDoc.CreateElement("LOC.facility");
						elemLOCfac.SetAttribute("V", facs[i]);
						myRuntimeDBReadCommand.CommandText = string.Format("SELECT loc_name, is_default FROM DBA.inst_locations WHERE parent in (SELECT loc_num FROM DBA.inst_locations WHERE loc_name = '{0}')", facs[i].Replace("'", "''"));
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
						while (myRuntimeDBReadReader.Read())
						{
							if (!myRuntimeDBReadReader.IsDBNull(0))
							{
								XmlElement elemLOCunit = DmlDoc.CreateElement("unit");
								elemLOCunit.SetAttribute("V", myRuntimeDBReadReader.GetString(0));
								if (!myRuntimeDBReadReader.IsDBNull(1))
								{
									elemLOCunit.SetAttribute("DF", myRuntimeDBReadReader.GetString(1));
								}
								elemLOCfac.AppendChild(elemLOCunit);
							}
						}
						myRuntimeDBReadReader.Close();
						int iLen = int.Parse(m_max_message_sz) - addCount - 4 * recSize - 32;
						if (firstTime || DmlDoc.OuterXml.Length > iLen)
						{
							string locList;
							if (!firstTime)
							{
								locList = DmlDoc.OuterXml;
								m_waiting = true;
								SendString(locList, isPartial: true, trunc: false);
								OnReadComplete();
							}
							addCount = 0;
							locList = "<NOVA.LOC>" + GenDMLHeader("Protocol") + "</NOVA.LOC>";
							DmlDoc.LoadXml(locList);
							root = DmlDoc.DocumentElement;
						}
						elemLOC.AppendChild(elemLOCfac);
						root.AppendChild(elemLOC);
						addCount++;
						totalCount++;
						firstTime = false;
					}
					if (addCount > 0)
					{
						string locList = DmlDoc.OuterXml;
						m_waiting = true;
						SendString(locList, isPartial: false, trunc: false);
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
			handleDBException(e, "processing location list", "SendLocationList", "Protocol");
			addCount = 0;
		}
		catch (Exception e2)
		{
			handleException(e2, "Processing location list", "SendLocationList", "Protocol");
			addCount = 0;
		}
		if (totalCount <= 0)
		{
			return false;
		}
		return true;
	}

	private bool SendSetup()
	{
		bool ret = false;
		if (m_SetupSupported)
		{
			ret = SendSetup_meter();
		}
		else if (m_tdomeSetupSupported)
		{
			SendSetup_tdome();
		}
		else if (m_bgaSetupSupported)
		{
			SendSetup_bga();
		}
		return ret;
	}

	private bool SendSetup_meter()
	{
		bool ret = false;
		XmlDocument DmlDoc = new XmlDocument();
		try
		{
			if (m_SetupSupported && m_loc_num.Length > 0)
			{
				int kvCount = 0;
				int tcCount = 0;
				int comsCount = 0;
				int dcCount = 0;
				DateTime setup_update_datetime = DMLToSystemDateTime(m_setup_update_dttm);
				string setup_update_date = setup_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - setup_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}'";
				if (m_b_loc_last_update_inst_class_column)
				{
					myRuntimeDBWriteCommand.CommandText += $" and inst_class = '{m_inst_class}'";
				}
				else if (m_b_loc_last_update_inst_type_column)
				{
					myRuntimeDBWriteCommand.CommandText += $" and inst_type = '{m_inst_type}'";
				}
				myRuntimeDBReadCommand.CommandText += $" and data_type = 'SETUP' and last_update_time >= '{setup_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				kvCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (kvCount == 0 && ts.TotalDays > 365.0)
				{
					kvCount = 1;
				}
				if (m_AlwaysSend || kvCount + comsCount + tcCount + dcCount > 0)
				{
					string setup = "<NOVA.STATSTRIP.SETUP>" + GenDMLHeader("Protocol") + "</NOVA.STATSTRIP.SETUP>";
					DmlDoc.LoadXml(setup);
					XmlElement root = DmlDoc.DocumentElement;
					XmlElement kvElem = DmlDoc.CreateElement("KEY_VALUE");
					root.AppendChild(kvElem);
					myRuntimeDBReadCommand.CommandText = "SELECT _key, _value FROM DBA.config_data c join DBA.loc_to_config l2c on c.config_num = l2c.config_num WHERE ";
					if (m_b_loc_to_config_inst_type_column)
					{
						myRuntimeDBReadCommand.CommandText += $"inst_type = '{m_inst_type}' AND ";
					}
					myRuntimeDBReadCommand.CommandText += $"((loc_num is NULL) OR (loc_num = '{m_loc_num}')) ";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool noKV = true;
					while (myRuntimeDBReadReader.Read())
					{
						if (!myRuntimeDBReadReader.IsDBNull(0))
						{
							char[] star = new char[1] { '*' };
							string[] key_attr = myRuntimeDBReadReader.GetString(0).Split(star);
							XmlElement kElem = DmlDoc.CreateElement(key_attr[0]);
							if (!myRuntimeDBReadReader.IsDBNull(1) && key_attr.Length > 1)
							{
								kElem.SetAttribute(key_attr[1], myRuntimeDBReadReader.GetString(1));
							}
							kvElem.AppendChild(kElem);
							noKV = false;
						}
					}
					myRuntimeDBReadReader.Close();
					if (m_supportMTE)
					{
						AddKeyValueForMT(ref DmlDoc, ref kvElem, m_loc_num);
					}
					if (m_send_location)
					{
						XmlElement fElem = DmlDoc.CreateElement("Facility");
						fElem.SetAttribute("V", m_facility);
						kvElem.AppendChild(fElem);
						noKV = false;
						XmlElement lElem = DmlDoc.CreateElement("Location");
						lElem.SetAttribute("V", m_location);
						kvElem.AppendChild(lElem);
						noKV = false;
					}
					if (noKV)
					{
						kvElem.InnerText = "none";
					}
					XmlElement tcElem = DmlDoc.CreateElement("TEST_CONFIG");
					root.AppendChild(tcElem);
					OpenStringsDBConnection();
					string m_facil_num = "";
					myRuntimeDBReadCommand.CommandText = $"select parent from DBA.inst_locations il where loc_num = '{m_loc_num}'";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					if (myRuntimeDBReadReader.Read())
					{
						m_facil_num = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
					myRuntimeDBReadReader.Close();
					myRuntimeDBReadCommand.CommandText = "SELECT DISTINCT it.test_name, it.result_type_code, it.units, ftu.units_of_measure, it.lo_limit, it.hi_limit, o.slope, o.intercept, t.lo_panic_limit, t.hi_panic_limit, t.lo_normal_limit, t.hi_normal_limit, t.sex, t.age_type, t.ageLo, t.ageHi, t.enable_all_ages, t.range_label, t.equation, t.eq_const, o.enable_deselect, it.test_code, it.test_code_system, t.ui_order, fptr.lo_limit, fptr.hi_limit from DBA.instruments_tests it left outer join DBA.test_offsets o on it.generic_test_name = o.generic_test_name and it.units = o.units";
					if (m_b_test_offsets_inst_class_column && m_b_instruments_tests_inst_class_column)
					{
						myRuntimeDBReadCommand.CommandText += " and o.inst_class = it.inst_class";
					}
					else if (m_b_test_offsets_inst_type_column)
					{
						myRuntimeDBReadCommand.CommandText += " and o.inst_type = it.inst_type";
					}
					myRuntimeDBReadCommand.CommandText += string.Format(" and o.loc_num = '{0}' join DBA.test_range t on t.generic_test_name = it.generic_test_name and t.units = it.units and t.result_type_code = it.result_type_code and t.loc_num = '{0}' left outer join DBA.facility_test_units ftu on ftu.loc_num = '{1}' and it.generic_test_name = ftu.generic_test_name left outer join DBA.facility_patient_test_rails fptr on fptr.test_name = it.test_name and fptr.units = it.units and fptr.facility_id = '{1}' where it.sample_type_code = 'BLD'", m_loc_num, m_facil_num);
					myRuntimeDBReadCommand.CommandText += $"and it.inst_type = '{m_inst_type}'";
					myRuntimeDBReadCommand.CommandText += " order by t.ui_order";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool noTC = true;
					while (myRuntimeDBReadReader.Read())
					{
						string units = (myRuntimeDBReadReader.IsDBNull(2) ? "" : myRuntimeDBReadReader.GetString(2));
						string units_of_measure = (myRuntimeDBReadReader.IsDBNull(3) ? units : myRuntimeDBReadReader.GetString(3));
						if (units_of_measure != units)
						{
							continue;
						}
						XmlElement tElem = FindOrAddNodeByAttributeU(ref DmlDoc, ref tcElem, "TEST", "TN", myRuntimeDBReadReader.GetString(0), "", units);
						string result_type_code = "";
						if (!myRuntimeDBReadReader.IsDBNull(1))
						{
							result_type_code = myRuntimeDBReadReader.GetString(1);
							tElem.SetAttribute("RT", result_type_code);
						}
						tElem.SetAttribute("U", units_of_measure);
						if (result_type_code == "M")
						{
							tElem.SetAttribute("SL", myRuntimeDBReadReader.IsDBNull(6) ? "1.0" : myRuntimeDBReadReader.GetString(6));
							tElem.SetAttribute("IC", myRuntimeDBReadReader.IsDBNull(7) ? "0.0" : myRuntimeDBReadReader.GetString(7));
						}
						else if (!myRuntimeDBReadReader.IsDBNull(20))
						{
							tElem.SetAttribute("ED", (myRuntimeDBReadReader.GetString(20) == "F") ? "0" : "1");
						}
						XmlElement rElem = DmlDoc.CreateElement("RANGE");
						rElem.SetAttribute("CT", "[" + (myRuntimeDBReadReader.IsDBNull(8) ? "" : myRuntimeDBReadReader.GetString(8)) + ";" + (myRuntimeDBReadReader.IsDBNull(9) ? "" : myRuntimeDBReadReader.GetString(9)) + "]");
						if (rElem.GetAttribute("CT").Length == 3)
						{
							rElem.SetAttribute("CT", "");
						}
						rElem.SetAttribute("RF", "[" + (myRuntimeDBReadReader.IsDBNull(10) ? "" : myRuntimeDBReadReader.GetString(10)) + ";" + (myRuntimeDBReadReader.IsDBNull(11) ? "" : myRuntimeDBReadReader.GetString(11)) + "]");
						if (rElem.GetAttribute("RF").Length == 3)
						{
							rElem.SetAttribute("RF", "");
						}
						rElem.SetAttribute("SEX", myRuntimeDBReadReader.IsDBNull(12) ? "U" : myRuntimeDBReadReader.GetString(12));
						string age = "";
						string ageLo_type = "";
						if (!myRuntimeDBReadReader.IsDBNull(13))
						{
							ageLo_type = myRuntimeDBReadReader.GetString(13);
						}
						if (ageLo_type.Length > 0)
						{
							string age_lo = (myRuntimeDBReadReader.IsDBNull(14) ? "" : myRuntimeDBReadReader.GetString(14));
							int ageLo = ((age_lo.Length > 0) ? int.Parse(age_lo) : 0);
							string age_hi = (myRuntimeDBReadReader.IsDBNull(15) ? "" : myRuntimeDBReadReader.GetString(15));
							int ageHi = ((age_hi.Length > 0) ? int.Parse(age_hi) : 0);
							if (ageHi > 0)
							{
								if (ageLo_type == "Y")
								{
									ageLo = (int)((double)ageLo * 365.25);
									ageHi = (int)((double)ageHi * 365.25);
								}
								string enable_all_ages = (myRuntimeDBReadReader.IsDBNull(16) ? "" : myRuntimeDBReadReader.GetString(16));
								age = "[";
								age += ((enable_all_ages == "T") ? "0" : ageLo.ToString());
								age += ";";
								age += ((enable_all_ages == "T") ? "+inf[" : (ageHi + "]"));
							}
						}
						rElem.SetAttribute("AGE", age);
						if (rElem.GetAttribute("AGE").Length == 3)
						{
							rElem.SetAttribute("AGE", "");
						}
						string mylabel = (myRuntimeDBReadReader.IsDBNull(17) ? "" : myRuntimeDBReadReader.GetString(17));
						int il = mylabel.LastIndexOf('^');
						if (il >= 0)
						{
							string pagename = mylabel.Substring(0, il);
							string varname = mylabel.Substring(il + 1);
							bool bFound = false;
							myStringsDBReadCommand.CommandText = "select var_value from dba.ui_translations where page_name = '" + pagename + "' and var_name = '" + varname + "' and lang = '" + m_language_long + "'";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myStringsDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myStringsDBReadReader = myStringsDBReadCommand.ExecuteReader();
							if (myStringsDBReadReader.Read())
							{
								mylabel = (myStringsDBReadReader.IsDBNull(0) ? "" : myStringsDBReadReader.GetString(0));
								bFound = true;
							}
							myStringsDBReadReader.Close();
							if (!bFound)
							{
								myStringsDBReadCommand.CommandText = "select var_value from dba.ui_translations where page_name = '" + pagename + "' and var_name = '" + varname + "' and lang = '" + m_language_short + "'";
								if (m_NNBase.m_isLogging)
								{
									m_NNBase.log(myStringsDBReadCommand.CommandText, isXml: false, "SQL");
								}
								myStringsDBReadReader = myStringsDBReadCommand.ExecuteReader();
								if (myStringsDBReadReader.Read())
								{
									mylabel = (myStringsDBReadReader.IsDBNull(0) ? "" : myStringsDBReadReader.GetString(0));
								}
								myStringsDBReadReader.Close();
							}
						}
						rElem.SetAttribute("LABEL", mylabel);
						rElem.SetAttribute("ABS", "[" + (myRuntimeDBReadReader.IsDBNull(4) ? "" : myRuntimeDBReadReader.GetString(4)) + ";" + (myRuntimeDBReadReader.IsDBNull(5) ? "" : myRuntimeDBReadReader.GetString(5)) + "]");
						if (rElem.GetAttribute("ABS").Length == 3)
						{
							rElem.SetAttribute("ABS", "");
						}
						rElem.SetAttribute("EQ", myRuntimeDBReadReader.IsDBNull(18) ? "" : myRuntimeDBReadReader.GetString(18));
						rElem.SetAttribute("EQ_CONST", myRuntimeDBReadReader.IsDBNull(19) ? "" : myRuntimeDBReadReader.GetString(19));
						rElem.SetAttribute("CODE", myRuntimeDBReadReader.IsDBNull(21) ? "" : myRuntimeDBReadReader.GetString(21));
						rElem.SetAttribute("CODE_SYS", myRuntimeDBReadReader.IsDBNull(22) ? "" : myRuntimeDBReadReader.GetString(22));
						string sAMR_lo = (myRuntimeDBReadReader.IsDBNull(24) ? "" : myRuntimeDBReadReader.GetString(24));
						string sAMR_hi = (myRuntimeDBReadReader.IsDBNull(25) ? "" : myRuntimeDBReadReader.GetString(25));
						rElem.SetAttribute("AMR", "[" + sAMR_lo + ";" + sAMR_hi + "]");
						if (rElem.GetAttribute("AMR").Length == 3)
						{
							rElem.SetAttribute("AMR", "");
						}
						tElem.AppendChild(rElem);
						noTC = false;
					}
					myRuntimeDBReadReader.Close();
					myStringsDBConnection.Close();
					if (m_supportMTE)
					{
						try
						{
							myRuntimeDBReadCommand.CommandText = $"SELECT xml_data from DBA.manual_tests mt join DBA.loc_to_panel l2mt on mt.panel_name = l2mt.panel_name where loc_num = '{m_loc_num}'";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
							while (myRuntimeDBReadReader.Read())
							{
								tcElem.InnerXml += myRuntimeDBReadReader.GetString(0);
								noTC = false;
							}
						}
						catch (ThreadAbortException)
						{
							handleThreadAbortException("Protocol");
						}
						catch (OdbcException e)
						{
							handleDBException(e, "ManualTestConfig", "SendSetup_meter", "Protocol");
						}
						catch (Exception e2)
						{
							handleException(e2, "ManualTestConfig", "SendSetup_meter", "Protocol");
						}
						myRuntimeDBReadReader.Close();
					}
					if (noTC)
					{
						tcElem.InnerText = "none";
					}
					XmlElement comsElem = DmlDoc.CreateElement("COMMENTS");
					root.AppendChild(comsElem);
					myRuntimeDBReadCommand.CommandText = "SELECT distinct comment_desc, it.test_name, is_chartable, is_flagable, comment_type, display_order FROM";
					myRuntimeDBReadCommand.CommandText += " DBA.test_comment c join DBA.test_comment_to_loc";
					myRuntimeDBReadCommand.CommandText += " c2u on c.comment_num = c2u.comment_num join DBA.instruments_tests it on c.generic_test_name = it.generic_test_name WHERE";
					myRuntimeDBReadCommand.CommandText += $" c2u.loc_num = '{m_loc_num}'";
					myRuntimeDBReadCommand.CommandText += $" AND it.inst_type = '{m_inst_type}' order by c.display_order";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool noComments = true;
					while (myRuntimeDBReadReader.Read())
					{
						if (!myRuntimeDBReadReader.IsDBNull(0))
						{
							XmlElement cElem = DmlDoc.CreateElement("Comment");
							if (!myRuntimeDBReadReader.IsDBNull(0))
							{
								cElem.SetAttribute("V", myRuntimeDBReadReader.GetString(0));
							}
							if (!myRuntimeDBReadReader.IsDBNull(1))
							{
								cElem.SetAttribute("TN", myRuntimeDBReadReader.GetString(1));
							}
							if (!myRuntimeDBReadReader.IsDBNull(2))
							{
								cElem.SetAttribute("CH", (myRuntimeDBReadReader.GetString(2) == "T") ? "1" : "0");
							}
							if (!myRuntimeDBReadReader.IsDBNull(3))
							{
								cElem.SetAttribute("FL", (myRuntimeDBReadReader.GetString(3) == "T") ? "1" : "0");
							}
							if (!myRuntimeDBReadReader.IsDBNull(4))
							{
								cElem.SetAttribute("CT", myRuntimeDBReadReader.GetString(4));
							}
							comsElem.AppendChild(cElem);
							noComments = false;
						}
					}
					myRuntimeDBReadReader.Close();
					if (noComments)
					{
						comsElem.InnerText = "none";
					}
					root.AppendChild(comsElem);
					XmlElement dcsElem = DmlDoc.CreateElement("DIAGCODES");
					root.AppendChild(dcsElem);
					myRuntimeDBReadCommand.CommandText = $"SELECT DISTINCT d.diagnosis_code, diagnosis_text FROM DBA.diagnosis_codes d join DBA.diagnosis_to_unit d2u on d.diagnosis_code = d2u.diagnosis_code join DBA.instruments_tests it on d.generic_test_name = it.generic_test_name  WHERE  inst_type = '{m_inst_type}' AND loc_num = '{m_loc_num}'";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool noDC = true;
					while (myRuntimeDBReadReader.Read())
					{
						if (!myRuntimeDBReadReader.IsDBNull(0))
						{
							XmlElement dElem = DmlDoc.CreateElement("DIAGCODE");
							XmlElement dElemId = DmlDoc.CreateElement("DIAGCODE.code_id");
							if (!myRuntimeDBReadReader.IsDBNull(0))
							{
								dElemId.SetAttribute("V", myRuntimeDBReadReader.GetString(0));
							}
							dElem.AppendChild(dElemId);
							XmlElement dElemDesc = DmlDoc.CreateElement("DIAGCODE.code_desc");
							if (!myRuntimeDBReadReader.IsDBNull(1))
							{
								dElemDesc.SetAttribute("V", myRuntimeDBReadReader.GetString(1));
							}
							dElem.AppendChild(dElemDesc);
							dcsElem.AppendChild(dElem);
							noDC = false;
						}
					}
					myRuntimeDBReadReader.Close();
					if (noDC)
					{
						dcsElem.InnerText = "none";
					}
					root.AppendChild(dcsElem);
					ret = true;
					setup = DmlDoc.OuterXml;
					m_waiting = true;
					SendString(setup, isPartial: false, trunc: false);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e3)
		{
			handleDBException(e3, "processing setup", "SendSetup_meter", "Protocol");
			ret = false;
		}
		catch (Exception e4)
		{
			handleException(e4, "processing setup", "SendSetup_meter", "Protocol");
			ret = false;
		}
		return ret;
	}

	private bool SendSetup_tdome()
	{
		return false;
	}

	private bool SendSetup_bga()
	{
		return false;
	}

	private bool SendWifiSetup()
	{
		bool ret = false;
		string WifiSetupContent = "";
		m_facil_num = GetFacilNum(m_facility);
		try
		{
			if (m_WifiSetupSupported && m_loc_num.Length > 0)
			{
				int kvCount = 0;
				DateTime wifi_setup_update_datetime = DMLToSystemDateTime(m_wifi_setup_update_dttm);
				string wifi_setup_update_date = wifi_setup_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - wifi_setup_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}'";
				if (m_b_loc_last_update_inst_class_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_class = '{m_inst_class}'";
				}
				else if (m_b_loc_last_update_inst_type_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_type = '{m_inst_type}'";
				}
				myRuntimeDBReadCommand.CommandText += $" and data_type = 'WIFI_SETUP' and last_update_time >= '{wifi_setup_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				kvCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (kvCount == 0 && ts.TotalDays > 365.0)
				{
					kvCount = 1;
				}
				if (m_AlwaysSend || kvCount > 0)
				{
					string wifi_setup = "";
					string config_id = "";
					myRuntimeDBReadCommand.CommandText = $"SELECT config_id FROM DBA.loc_to_wifi_setup WHERE (inst_class = '{m_inst_class}') AND (loc_num = '{m_loc_num}') ";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					if (myRuntimeDBReadReader.Read() && !myRuntimeDBReadReader.IsDBNull(0))
					{
						config_id = myRuntimeDBReadReader.GetString(0);
					}
					myRuntimeDBReadReader.Close();
					string userName = string.Empty;
					string passWord = string.Empty;
					if (config_id.Length > 0)
					{
						myRuntimeDBReadCommand.CommandText = $"SELECT wifi_data FROM DBA.wifi_setup WHERE config_id = '{config_id}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
						if (myRuntimeDBReadReader.Read() && !myRuntimeDBReadReader.IsDBNull(0))
						{
							WifiSetupContent = myRuntimeDBReadReader.GetString(0);
						}
						myRuntimeDBReadReader.Close();
						if (WifiSetupContent.Length > 0)
						{
							bool continueQuery = true;
							bool upRet = GetWifiUsernameAndPassword("1FacAnd1Location", out userName, out passWord);
							if (upRet && (userName.Length > 0 || passWord.Length > 0))
							{
								continueQuery = false;
							}
							if (continueQuery)
							{
								upRet = GetWifiUsernameAndPassword("1FacAndAllLocation", out userName, out passWord);
							}
							if (upRet && (userName.Length > 0 || passWord.Length > 0))
							{
								continueQuery = false;
							}
							if (continueQuery)
							{
								upRet = GetWifiUsernameAndPassword("AllFac", out userName, out passWord);
							}
							if (userName.Length > 0)
							{
								WifiSetupContent = ReplaceXMLElement(WifiSetupContent, "<userName>", "</userName>", userName, obfuscateFlag: false);
							}
							if (passWord.Length > 0)
							{
								WifiSetupContent = ReplaceXMLElement(WifiSetupContent, "<password>", "</password>", passWord, obfuscateFlag: false);
							}
							wifi_setup = "<NOVA.WIFI.SETUP>" + GenDMLHeader("Protocol") + WifiSetupContent + "</NOVA.WIFI.SETUP>";
						}
					}
					if (wifi_setup.Length > 0)
					{
						m_waiting = true;
						ret = true;
						string logString = null;
						if (m_NNBase.m_isLogging)
						{
							logString = wifi_setup;
							logString = ReplaceXMLElement(logString, "<userName>", "</userName>", null, obfuscateFlag: true);
							logString = ReplaceXMLElement(logString, "<password>", "</password>", null, obfuscateFlag: true);
							logString = ReplaceXMLElement(logString, "<passPhrase>", "</passPhrase>", null, obfuscateFlag: true);
							logString = ReplaceXMLElement(logString, "<pacFileName>", "</pacFileName>", null, obfuscateFlag: true);
							logString = ReplaceXMLElement(logString, "<pacPassword>", "</pacPassword>", null, obfuscateFlag: true);
						}
						SendString(wifi_setup, isPartial: false, trunc: false, logString);
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
			handleDBException(e, "processing WIFI setup", "SendWifiSetup", "Protocol");
			ret = false;
		}
		catch (Exception e2)
		{
			handleException(e2, "processing WIFI setup", "SendWifiSetup", "Protocol");
			ret = false;
		}
		return ret;
	}

	private bool GetWifiUsernameAndPassword(string queryMethod, out string userName, out string passWord)
	{
		bool ret = true;
		userName = string.Empty;
		passWord = string.Empty;
		try
		{
			switch (queryMethod)
			{
			case "1FacAnd1Location":
				myRuntimeDBReadCommand.CommandText = $"SELECT wifi_user_name, wifi_password FROM DBA.wifi_credentials WHERE (fac_num = '{m_facil_num}') AND (loc_num = '{m_loc_num}') AND wifi_mac_address = '{m_Wifi_MAC_Address}'";
				break;
			case "1FacAndAllLocation":
				myRuntimeDBReadCommand.CommandText = $"SELECT wifi_user_name, wifi_password FROM DBA.wifi_credentials WHERE (fac_num = '{m_facil_num}') AND ((loc_num = 'All') or (loc_num = '') or (loc_num is null)) AND wifi_mac_address = '{m_Wifi_MAC_Address}'";
				break;
			case "AllFac":
				myRuntimeDBReadCommand.CommandText = $"SELECT wifi_user_name, wifi_password FROM DBA.wifi_credentials WHERE ((fac_num = 'All') or (fac_num = '') or (fac_num is null)) AND wifi_mac_address = '{m_Wifi_MAC_Address}'";
				break;
			}
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			if (myRuntimeDBReadReader.Read())
			{
				if (!myRuntimeDBReadReader.IsDBNull(0))
				{
					userName = myRuntimeDBReadReader.GetString(0);
				}
				if (!myRuntimeDBReadReader.IsDBNull(1))
				{
					passWord = myRuntimeDBReadReader.GetString(1);
				}
			}
			myRuntimeDBReadReader.Close();
		}
		catch (OdbcException e)
		{
			ret = false;
			handleDBException(e, "processing WIFI setup", "SendWifiSetup", "Protocol-WIFI-SETUP");
		}
		catch (Exception e2)
		{
			ret = false;
			handleException(e2, "processing WIFI setup", "SendWifiSetup", "Protocol-WIFI-SETUP");
		}
		return ret;
	}

	private string ReplaceXMLElement(string xmlString, string startElement, string endElement, string newValue, bool obfuscateFlag)
	{
		string[] WifiSetupContentParts = new string[3];
		newValue = SecurityElement.Escape(newValue);
		int i = xmlString.IndexOf(startElement, StringComparison.OrdinalIgnoreCase);
		int j = xmlString.IndexOf(endElement, StringComparison.OrdinalIgnoreCase);
		if (i > 0 && j > i)
		{
			WifiSetupContentParts[0] = xmlString.Substring(0, i + startElement.Length);
			WifiSetupContentParts[1] = xmlString.Substring(i + startElement.Length, j - i - startElement.Length);
			WifiSetupContentParts[2] = xmlString.Substring(j);
		}
		if (!string.IsNullOrEmpty(newValue))
		{
			WifiSetupContentParts[1] = newValue;
		}
		if (obfuscateFlag)
		{
			WifiSetupContentParts[1] = getStringForLog(WifiSetupContentParts[1]);
		}
		if (i > 0 && j > i)
		{
			return WifiSetupContentParts[0] + WifiSetupContentParts[1] + WifiSetupContentParts[2];
		}
		return xmlString;
	}

	private string getStringForLog(string inputString)
	{
		if (!string.IsNullOrEmpty(inputString))
		{
			int l = inputString.Length;
			if (l > 3)
			{
				inputString = inputString.Substring(l - 2);
				return "*" + inputString;
			}
			return "***";
		}
		return string.Empty;
	}

	private bool SendWifiCert()
	{
		bool ret = false;
		try
		{
			if (m_WifiCertSupported && m_loc_num.Length > 0)
			{
				int kvCount = 0;
				DateTime wifi_cert_update_datetime = DMLToSystemDateTime(m_cert_update_dttm);
				string wifi_cert_update_date = wifi_cert_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - wifi_cert_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}'";
				if (m_b_loc_last_update_inst_class_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_class = '{m_inst_class}'";
				}
				else if (m_b_loc_last_update_inst_type_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_type = '{m_inst_type}'";
				}
				myRuntimeDBReadCommand.CommandText += $" and data_type = 'WIFI_CERT' and last_update_time >= '{wifi_cert_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				kvCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (kvCount == 0 && ts.TotalDays > 365.0)
				{
					kvCount = 1;
				}
				if (kvCount == 0)
				{
					try
					{
						myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.wifi_certificate_to_device where  wifi_mac_address = '{m_Wifi_MAC_Address}' AND datetime_stamp >= '{wifi_cert_update_date}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
						}
						kvCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("Protocol");
					}
					catch (OdbcException e)
					{
						handleDBException(e, "Searching for any certificate changes for this device", "SendWifiCert", "Protocol");
					}
					catch (Exception e2)
					{
						handleException(e2, "Searching for any certificate changes for this device", "SendWifiCert", "Protocol");
					}
				}
				if (m_AlwaysSend || kvCount > 0)
				{
					bool bFirstTime = true;
					bool bMoreData = true;
					string cert = "";
					string CertCertnum = "";
					string CertUCertnum = "";
					string PVKCertnum = "";
					bool bCertToSend = false;
					bool bCertUToSend = false;
					bool bPVKToSend = false;
					bool bQueryLocation = true;
					PVKCertnum = GetCertNum("PVK", "Nova.PVK", 1);
					bPVKToSend = PVKCertnum.Length > 0;
					CertUCertnum = GetCertNum("CER", "NovaU.CER", 1);
					bCertUToSend = CertUCertnum.Length > 0;
					CertCertnum = GetCertNum("CER", "Nova.CER", 1);
					bCertToSend = CertCertnum.Length > 0;
					if (bPVKToSend || bCertUToSend || bCertToSend)
					{
						bQueryLocation = false;
					}
					if (bQueryLocation)
					{
						PVKCertnum = GetCertNum("PVK", "Nova.PVK", 2);
						bPVKToSend = PVKCertnum.Length > 0;
						CertUCertnum = GetCertNum("CER", "NovaU.CER", 2);
						bCertUToSend = CertUCertnum.Length > 0;
						CertCertnum = GetCertNum("CER", "Nova.CER", 2);
						bCertToSend = CertCertnum.Length > 0;
					}
					if (bCertToSend)
					{
						ret = true;
						cert = GetCertData("CER", "Nova.CER", CertCertnum, ref bFirstTime, ref bMoreData);
						while (cert.Length > 0)
						{
							m_waiting = true;
							SendString(cert, bMoreData | bCertUToSend | bPVKToSend, trunc: true);
							cert = "";
							if (bMoreData | bCertUToSend | bPVKToSend)
							{
								OnReadComplete();
							}
							if (bMoreData)
							{
								cert = GetCertData("CER", "Nova.CER", CertCertnum, ref bFirstTime, ref bMoreData);
							}
						}
					}
					if (bCertUToSend)
					{
						ret = true;
						bFirstTime = true;
						bMoreData = true;
						cert = "";
						cert = GetCertData("CER", "NovaU.CER", CertUCertnum, ref bFirstTime, ref bMoreData);
						while (cert.Length > 0)
						{
							m_waiting = true;
							SendString(cert, bMoreData | bPVKToSend, trunc: true);
							cert = "";
							if (bMoreData | bPVKToSend)
							{
								OnReadComplete();
							}
							if (bMoreData)
							{
								cert = GetCertData("CER", "NovaU.CER", CertUCertnum, ref bFirstTime, ref bMoreData);
							}
						}
					}
					if (bPVKToSend)
					{
						ret = true;
						bFirstTime = true;
						bMoreData = true;
						cert = "";
						cert = GetCertData("PVK", "Nova.PVK", PVKCertnum, ref bFirstTime, ref bMoreData);
						while (cert.Length > 0)
						{
							m_waiting = true;
							SendString(cert, bMoreData, trunc: true);
							cert = "";
							if (bMoreData)
							{
								OnReadComplete();
								cert = GetCertData("PVK", "Nova.PVK", PVKCertnum, ref bFirstTime, ref bMoreData);
							}
						}
					}
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e3)
		{
			handleDBException(e3, "processing WIFI certificate", "SendWifiCert", "Protocol");
			ret = false;
		}
		catch (Exception e4)
		{
			handleException(e4, "processing WIFI certificate", "SendWifiCert", "Protocol");
			ret = false;
		}
		return ret;
	}

	private string GetCertNum(string DataType, string CertName, int getCertFlag)
	{
		string certnum = "";
		bool bReaderOpen = false;
		GetFacilityAndLocationByLocNum();
		if (getCertFlag == 1)
		{
			if (certnum.Length == 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_device WHERE (fac_num = '{m_facil_num}') AND (loc_num = '{m_loc_num}') AND (data_type = '{DataType}') AND (certificate_name = '{CertName}') AND (wifi_mac_address = '{m_Wifi_MAC_Address}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e)
				{
					handleDBException(e, "Selecting certificate for device and location", "GetCertNum", "Protocol");
				}
				catch (Exception e2)
				{
					handleException(e2, "Selecting certificate for device and location", "GetCertNum", "Protocol");
				}
				if (bReaderOpen)
				{
					myRuntimeDBReadReader.Close();
					bReaderOpen = false;
				}
			}
			if (certnum.Length == 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_device WHERE (fac_num = '{m_facil_num}') AND ((loc_num = 'All') or (loc_num = '') or (loc_num is null)) AND (data_type = '{DataType}') AND (certificate_name = '{CertName}') AND (wifi_mac_address = '{m_Wifi_MAC_Address}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e3)
				{
					handleDBException(e3, "Selecting certificate for device and facility", "GetCertNum", "Protocol");
				}
				catch (Exception e4)
				{
					handleException(e4, "Selecting certificate for device and facility", "GetCertNum", "Protocol");
				}
				if (bReaderOpen)
				{
					myRuntimeDBReadReader.Close();
					bReaderOpen = false;
				}
			}
			if (certnum.Length == 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_device WHERE ((fac_num = 'All') or (fac_num = '') or (fac_num is null)) AND (data_type = '{DataType}') AND (certificate_name = '{CertName}') AND (wifi_mac_address = '{m_Wifi_MAC_Address}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e5)
				{
					handleDBException(e5, "Selecting certificate for device and enterprise", "GetCertNum", "Protocol");
				}
				catch (Exception e6)
				{
					handleException(e6, "Selecting certificate for device and enterprise", "GetCertNum", "Protocol");
				}
				if (bReaderOpen)
				{
					myRuntimeDBReadReader.Close();
					bReaderOpen = false;
				}
			}
		}
		if (getCertFlag == 2)
		{
			try
			{
				if (certnum.Length == 0)
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_location WHERE (inst_class = '{m_inst_class}') AND (fac_num = '{m_facil_num}') AND (loc_num = '{m_loc_num}') AND (data_type = '{DataType}') AND (certificate_name = '{CertName}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (OdbcException e7)
			{
				handleDBException(e7, "Selecting certificate for location", "GetCertNum", "Protocol");
			}
			catch (Exception e8)
			{
				handleException(e8, "Selecting certificate for location", "GetCertNum", "Protocol");
			}
			if (bReaderOpen)
			{
				myRuntimeDBReadReader.Close();
				bReaderOpen = false;
			}
			if (certnum.Length == 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_location WHERE (inst_class = '{m_inst_class}') AND (fac_num = '{m_facil_num}') AND ((loc_num = 'All') or (loc_num = '') or (loc_num is null)) AND (data_type = '{DataType}') AND (certificate_name = '{CertName}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e9)
				{
					handleDBException(e9, "Selecting certificate for facility", "GetCertNum", "Protocol");
				}
				catch (Exception e10)
				{
					handleException(e10, "Selecting certificate for facility", "GetCertNum", "Protocol");
				}
				if (bReaderOpen)
				{
					myRuntimeDBReadReader.Close();
					bReaderOpen = false;
				}
			}
			if (certnum.Length == 0)
			{
				try
				{
					myRuntimeDBReadCommand.CommandText = $"SELECT certificate_num from DBA.wifi_certificate_to_location WHERE (inst_class = '{m_inst_class}') AND ((fac_num = 'All') or (fac_num = '') or (fac_num is null)) AND (data_type = '{DataType}') AND (certificate_name = '{CertName}')";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bReaderOpen = true;
					if (myRuntimeDBReadReader.Read())
					{
						certnum = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortException("Protocol");
				}
				catch (OdbcException e11)
				{
					handleDBException(e11, "Selecting certificate for enterprise", "GetCertNum", "Protocol");
				}
				catch (Exception e12)
				{
					handleException(e12, "Selecting certificate for enterprise", "GetCertNum", "Protocol");
				}
				if (bReaderOpen)
				{
					myRuntimeDBReadReader.Close();
					bReaderOpen = false;
				}
			}
		}
		return certnum;
	}

	private string GetCertData(string DataType, string CertName, string certnum, ref bool firstTime, ref bool bMoreData)
	{
		string cert = "";
		XmlDocument DmlDoc = new XmlDocument();
		if (firstTime)
		{
			myRuntimeDBReadCommand.CommandText = $"SELECT certificate_data FROM DBA.wifi_certificate_data_mine WHERE certificate_num = '{certnum}' ORDER BY index_num";
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
		}
		XmlElement elemCERTdata = null;
		XmlElement elemCERTname = null;
		int recSize = 2048;
		cert = "<NOVA.WIFI.CERT>" + GenDMLHeader("Protocol") + "</NOVA.WIFI.CERT>";
		DmlDoc.LoadXml(cert);
		XmlElement root = DmlDoc.DocumentElement;
		XmlElement elemCERT = DmlDoc.CreateElement(DataType);
		root.AppendChild(elemCERT);
		elemCERTname = DmlDoc.CreateElement(DataType + ".name");
		elemCERTname.SetAttribute("V", CertName);
		elemCERT.AppendChild(elemCERTname);
		elemCERTdata = DmlDoc.CreateElement(DataType + ".data");
		elemCERTdata.SetAttribute("ENC", "B64");
		elemCERT.AppendChild(elemCERTdata);
		while (myRuntimeDBReadReader.Read())
		{
			if (DmlDoc.OuterXml.Length > int.Parse(m_max_message_sz) - 4 * recSize)
			{
				cert = DmlDoc.OuterXml;
				bMoreData = true;
				return cert;
			}
			elemCERTdata.InnerText += (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
			if (firstTime)
			{
				firstTime = false;
				recSize = elemCERTdata.OuterXml.Length;
			}
		}
		myRuntimeDBReadReader.Close();
		cert = DmlDoc.OuterXml;
		bMoreData = false;
		return cert;
	}

	private ReagentRec ANewReagentRecord()
	{
		ReagentRec newOne = default(ReagentRec);
		newOne.lot_number = "";
		newOne.lot_name = "";
		newOne.lot_type = "";
		newOne.exp_date = DateTime.MaxValue;
		newOne.level_number = "";
		newOne.level_type = "";
		newOne.observation_id = "";
		newOne.lo_limit = "";
		newOne.hi_limit = "";
		newOne.units = "";
		return newOne;
	}

	private bool SendReagents()
	{
		int LocLastUpdateLotsCount = 0;
		XmlDocument DmlDoc = new XmlDocument();
		ArrayList MyReagentRecs = new ArrayList();
		bool firstTime = true;
		string reagList = "";
		try
		{
			if (m_ReagSupported && m_loc_num.Length > 0)
			{
				DateTime reag_update_datetime = DMLToSystemDateTime(m_reag_update_dttm);
				string reag_update_date = reag_update_datetime.ToString("yyyy-MM-dd HH:mm:ss");
				m_last_eot_update_time = DateTime.Now;
				TimeSpan ts = m_last_eot_update_time - reag_update_datetime;
				myRuntimeDBReadCommand.CommandText = $"SELECT count(*) from DBA.loc_last_update where loc_num = '{m_loc_num}'";
				if (m_b_loc_last_update_inst_class_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_class = '{m_inst_class}'";
				}
				else if (m_b_loc_last_update_inst_type_column)
				{
					myRuntimeDBReadCommand.CommandText += $" and inst_type = '{m_inst_type}'";
				}
				myRuntimeDBReadCommand.CommandText += $" and data_type = 'LOTS' and last_update_time >= '{reag_update_date}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				LocLastUpdateLotsCount = (int)myRuntimeDBReadCommand.ExecuteScalar();
				if (m_AlwaysSend || LocLastUpdateLotsCount > 0 || (LocLastUpdateLotsCount == 0 && ts.TotalDays > 365.0))
				{
					XmlElement root = null;
					LocLastUpdateLotsCount = 1;
					myRuntimeDBReadCommand.CommandText = string.Format("SELECT DISTINCT  lot,  lot_type,  expDate,  lot_level,  level_type,  test_name,  LR,  HR,  lc.Units FROM  DBA.lots l left outer join DBA.lot_chem lc  ON   lc.lots_key_num = l.lots_key_num  AND   lc.units in (select units_of_measure from DBA.facility_test_units f where f.loc_num in   (select parent from DBA.inst_locations i where i.loc_num = '{1}')   and lc.generic_test_name = f.generic_test_name and lc.facility_num = f.loc_num)  left outer join DBA.instruments_tests it ON lc.generic_test_name = it.generic_test_name  AND it.inst_type = '{0}' AND ((l.lot_type = 'TestStrip') or (l.lot_type = 'Control') or (l.lot_type = 'Linearity'))", m_inst_type, m_loc_num);
					myRuntimeDBReadCommand.CommandText += " WHERE";
					myRuntimeDBReadCommand.CommandText += $" l.lots_key_num in ( SELECT d2l.lots_key_num from DBA.device_to_lot d2l WHERE d2l.inst_type = '{m_inst_type}' )";
					myRuntimeDBReadCommand.CommandText += $" AND  (retired = 'F' or retired is null) AND  l.lots_key_num in (  SELECT   l2u.lots_key_num from DBA.lots_to_unit l2u  WHERE   loc_num = '{m_loc_num}'  ) AND (test_name is not null or lot_type = 'TestStrip') ORDER BY lot";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					while (myRuntimeDBReadReader.Read())
					{
						ReagentRec myReagentRec = ANewReagentRecord();
						myReagentRec.lot_number = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
						myReagentRec.lot_type = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
						myReagentRec.exp_date = (myRuntimeDBReadReader.IsDBNull(2) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(2));
						myReagentRec.level_number = (myRuntimeDBReadReader.IsDBNull(3) ? "" : myRuntimeDBReadReader.GetString(3));
						myReagentRec.level_type = (myRuntimeDBReadReader.IsDBNull(4) ? "" : myRuntimeDBReadReader.GetString(4));
						myReagentRec.observation_id = (myRuntimeDBReadReader.IsDBNull(5) ? "" : myRuntimeDBReadReader.GetString(5));
						myReagentRec.lo_limit = (myRuntimeDBReadReader.IsDBNull(6) ? "" : myRuntimeDBReadReader.GetString(6));
						myReagentRec.hi_limit = (myRuntimeDBReadReader.IsDBNull(7) ? "" : myRuntimeDBReadReader.GetString(7));
						myReagentRec.units = (myRuntimeDBReadReader.IsDBNull(8) ? "" : myRuntimeDBReadReader.GetString(8));
						MyReagentRecs.Add(myReagentRec);
					}
					myRuntimeDBReadReader.Close();
					if (m_supportMTE)
					{
						AddReagentsForMT(ref MyReagentRecs, m_loc_num);
					}
					int count = 0;
					if (MyReagentRecs.Count > 0)
					{
						string preLotNumber = string.Empty;
						int lotCount = 0;
						foreach (ReagentRec myReagentRec2 in MyReagentRecs)
						{
							if (preLotNumber != myReagentRec2.lot_number)
							{
								lotCount++;
								preLotNumber = myReagentRec2.lot_number;
							}
							if (firstTime || lotCount > MaxAddDelPerMsg)
							{
								if (!firstTime)
								{
									reagList = DmlDoc.OuterXml;
									m_waiting = true;
									SendString(reagList, isPartial: true, trunc: false);
									OnReadComplete();
								}
								reagList = "<NOVA.REAG>" + GenDMLHeader("Protocol") + "</NOVA.REAG>";
								DmlDoc.LoadXml(reagList);
								root = DmlDoc.DocumentElement;
								count = 0;
								lotCount = 0;
							}
							if (myReagentRec2.lot_type.Length > 1 && string.Compare(myReagentRec2.lot_type.Substring(0, 2), "MT", ignoreCase: true) == 0)
							{
								XmlElement elemLOT = FindOrCreateChildNode(ref DmlDoc, ref root, "LOT", "LOT.lot_number", "V", myReagentRec2.lot_number, "");
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemLOT, "LOT.type", "V", myReagentRec2.lot_type, "");
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemLOT, "LOT.expiration_dttm", "V", ExpDate2DML(myReagentRec2.exp_date), "");
								if (!string.IsNullOrEmpty(myReagentRec2.lot_name))
								{
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemLOT, "LOT.lot_name", "V", myReagentRec2.lot_name, "");
								}
							}
							else
							{
								XmlElement elemLOT2 = FindOrCreateChildNode(ref DmlDoc, ref root, "LOT", "LOT.lot_number", "V", myReagentRec2.lot_number, "");
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemLOT2, "LOT.type", "V", (myReagentRec2.lot_type == "Reagent") ? "RG" : ((myReagentRec2.lot_type == "Linearity") ? "LN" : ((myReagentRec2.lot_type == "TestStrip") ? "TS" : "QC")), "");
								FindOrAddNodeByAttribute(ref DmlDoc, ref elemLOT2, "LOT.expiration_dttm", "V", ExpDate2DML(myReagentRec2.exp_date), "");
								if (myReagentRec2.level_number.Length > 0)
								{
									XmlElement elemLevel = FindOrCreateChildNode(ref DmlDoc, ref elemLOT2, "Level", "Level.number", "V", myReagentRec2.level_number, "");
									FindOrAddNodeByAttribute(ref DmlDoc, ref elemLevel, "Level.type", "V", (myReagentRec2.level_type == "Reagent") ? "RG" : ((myReagentRec2.level_type == "Linearity") ? "LN" : "QC"), "");
									if (myReagentRec2.observation_id.Length > 0)
									{
										XmlElement elemTest = FindOrCreateChildNode(ref DmlDoc, ref elemLevel, "TST", "TST.observation_id", "V", myReagentRec2.observation_id, "");
										XmlElement elemTestRange = FindOrAddNodeByAttribute(ref DmlDoc, ref elemTest, "TST.lo-hi_limit", "V", "[" + myReagentRec2.lo_limit + ";" + myReagentRec2.hi_limit + "]", "");
										elemTestRange.SetAttribute("U", myReagentRec2.units);
									}
								}
							}
							count++;
							firstTime = false;
						}
						reagList = DmlDoc.OuterXml;
						m_waiting = true;
						SendString(reagList, isPartial: false, trunc: false);
					}
					else
					{
						reagList = "<NOVA.REAG>" + GenDMLHeader("Protocol") + "</NOVA.REAG>";
						m_waiting = true;
						SendString(reagList, isPartial: false, trunc: false);
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
			handleDBException(e, "processing lots", "SendReagents", "Protocol");
		}
		catch (Exception e2)
		{
			handleException(e2, "processing lots", "SendReagents", "Protocol");
		}
		return LocLastUpdateLotsCount > 0;
	}

	private void AddReagentsForMT(ref ArrayList MyReagentRecs, string loc_num)
	{
		string sql = "SELECT DISTINCT lot, lot_type, expDate,lot_name FROM DBA.lots l ";
		sql += " WHERE (l.lots_key_num IN (SELECT lots_key_num FROM DBA.mt_lot_to_loc lc ";
		sql += " WHERE lc.loc_num = '";
		sql = sql + loc_num + "') AND (lot_type LIKE 'MT%') AND (retired = 'F' OR retired is null))";
		myRuntimeDBReadCommand.CommandText = sql;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
		}
		try
		{
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			while (myRuntimeDBReadReader.Read())
			{
				ReagentRec myReagentRec = ANewReagentRecord();
				myReagentRec.lot_number = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
				myReagentRec.lot_type = (myRuntimeDBReadReader.IsDBNull(1) ? "" : myRuntimeDBReadReader.GetString(1));
				myReagentRec.exp_date = (myRuntimeDBReadReader.IsDBNull(2) ? DateTime.MaxValue : myRuntimeDBReadReader.GetDateTime(2));
				myReagentRec.lot_name = (myRuntimeDBReadReader.IsDBNull(3) ? "" : myRuntimeDBReadReader.GetString(3));
				MyReagentRecs.Add(myReagentRec);
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleDBException(e, "processing manual lots", "AddReagentsForMT", "Protocol");
		}
		catch (Exception e2)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleException(e2, "processing manual lots", "AddReagentsForMT", "Protocol");
		}
	}

	private void AddKeyValueForMT(ref XmlDocument DmlDoc, ref XmlElement root, string loc_num)
	{
		string sql = "SELECT c._key,c._value FROM DBA.config_data c, DBA.inst_locations il";
		sql += " WHERE c.config_num = il.loc_num AND il.loc_num = '";
		sql = sql + loc_num + "'";
		myRuntimeDBReadCommand.CommandText = sql;
		try
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
			}
			myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
			while (myRuntimeDBReadReader.Read())
			{
				if (!myRuntimeDBReadReader.IsDBNull(0))
				{
					char[] star = new char[1] { '*' };
					string[] key_attr = myRuntimeDBReadReader.GetString(0).Split(star);
					XmlElement kElem = DmlDoc.CreateElement(key_attr[0]);
					if (!myRuntimeDBReadReader.IsDBNull(1) && key_attr.Length > 1)
					{
						kElem.SetAttribute(key_attr[1], myRuntimeDBReadReader.GetString(1));
					}
					root.AppendChild(kElem);
				}
			}
			myRuntimeDBReadReader.Close();
		}
		catch (ThreadAbortException)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleDBException(e, "processing manual key-value", "GetReagentForMT", "Protocol");
		}
		catch (Exception e2)
		{
			if (myRuntimeDBReadReader != null && !myRuntimeDBReadReader.IsClosed)
			{
				myRuntimeDBReadReader.Close();
			}
			handleException(e2, "processing manual key-value", "GetReagentForMT", "Protocol");
		}
	}

	private bool SendFirmware()
	{
		bool ret = false;
		try
		{
			if (m_FirmSupported && m_loc_num.Length > 0)
			{
				string sInstType = m_inst_type;
				string sHWVersion = m_hw_version;
				if (m_hw_version != null && m_hw_version.Length > 0)
				{
					int iCarrot = sHWVersion.IndexOf("^");
					if (iCarrot > 0)
					{
						sHWVersion = sHWVersion.Substring(0, iCarrot);
					}
					sInstType = sInstType + "-" + sHWVersion;
				}
				myRuntimeDBReadCommand.CommandText = $"SELECT firmware_version from DBA.firmware f join DBA.loc_to_firmware l2f on f.firmware_num = l2f.firmware_num WHERE f.inst_type = '{sInstType}' AND loc_num = '{m_loc_num}'";
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
				}
				myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
				string firmware_version = "";
				while (myRuntimeDBReadReader.Read())
				{
					firmware_version = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
				}
				myRuntimeDBReadReader.Close();
				bool doFirmware = false;
				if (firmware_version != "")
				{
					string firmware_lang_version = firmware_version;
					if (firmware_lang_version.IndexOf("_") < 0)
					{
						firmware_lang_version += "_en";
					}
					doFirmware = m_sw_lang_version != firmware_lang_version;
					if (m_NNBase.m_isLogging)
					{
						string firmwareversions = "device firmare version = " + m_sw_lang_version + ", database firmware version = " + firmware_lang_version;
						m_NNBase.log(firmwareversions, isXml: false, "ICPMGR");
					}
				}
				if (doFirmware)
				{
					ret = true;
					XmlDocument DmlDoc = new XmlDocument();
					myRuntimeDBReadCommand.CommandText = $"SELECT firmware_data FROM DBA.DownloadFirmware WHERE inst_type = '{sInstType}' AND firmware_version = '{firmware_version}' ORDER BY index_num";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "SQL");
					}
					myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
					bool firstTime = true;
					string firmware = "";
					XmlElement elemFRMdata = null;
					int recSize = 2048;
					while (myRuntimeDBReadReader.Read())
					{
						if (firstTime || DmlDoc.OuterXml.Length > int.Parse(m_max_message_sz) - 4 * recSize)
						{
							if (!firstTime)
							{
								firmware = DmlDoc.OuterXml;
								m_waiting = true;
								SendString(firmware, isPartial: true, trunc: true);
								OnReadComplete();
							}
							firmware = "<NOVA.FRM>" + GenDMLHeader("Protocol") + "</NOVA.FRM>";
							DmlDoc.LoadXml(firmware);
							XmlElement root = DmlDoc.DocumentElement;
							XmlElement elemFRM = DmlDoc.CreateElement("FRM");
							root.AppendChild(elemFRM);
							elemFRMdata = DmlDoc.CreateElement("FRM.data");
							elemFRMdata.SetAttribute("ENC", "B64");
							elemFRM.AppendChild(elemFRMdata);
						}
						elemFRMdata.InnerText += (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
						if (firstTime)
						{
							firstTime = false;
							recSize = elemFRMdata.OuterXml.Length;
						}
					}
					myRuntimeDBReadReader.Close();
					firmware = DmlDoc.OuterXml;
					m_waiting = true;
					SendString(firmware, isPartial: false, trunc: true);
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (OdbcException e)
		{
			handleDBException(e, "processing firmware", "SendFirmware", "Protocol");
			ret = false;
		}
		catch (Exception e2)
		{
			handleException(e2, "processing firmware", "SendFirmware", "Protocol");
			ret = false;
		}
		return ret;
	}

	private void SendDateTime(string whoFrom)
	{
		try
		{
			string sReq = "<DTV.R02>" + GenDMLHeader(whoFrom) + "<DTV><DTV.command_cd V=\"SET_TIME\"/></DTV><TM><TM.dttm V=\"" + DateTime2DML(DateTime.Now) + "\"/></TM></DTV.R02>";
			m_waiting = true;
			if (whoFrom == "Timer")
			{
				SendString(sReq);
			}
			else
			{
				SendString(sReq, isPartial: false, trunc: false);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException(whoFrom);
		}
		catch (OdbcException e)
		{
			handleDBException(e, "sending date and time", "SendDateTime", whoFrom);
		}
		catch (Exception e2)
		{
			handleException(e2, "sending date and time", "SendDateTime", whoFrom);
		}
	}

	private void SendContinuous()
	{
		string sReq = "<DTV.R01>" + GenDMLHeader("Protocol") + "<DTV><DTV.command_cd V=\"START_CONTINUOUS\"/></DTV></DTV.R01>";
		m_waiting = true;
		SendString(sReq, isPartial: false, trunc: false);
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

	private string GetPatientIDTypeForBGA(string keyValue, string typestring, out string mrn, out string patId, out string acct)
	{
		string patientIDType = string.Empty;
		mrn = string.Empty;
		patId = string.Empty;
		acct = string.Empty;
		try
		{
			string[] arrayType = typestring.Split('^');
			for (int n = 0; n < arrayType.Length; n++)
			{
				switch (n)
				{
				case 0:
					mrn = arrayType[n];
					if (mrn.CompareTo(keyValue) == 0)
					{
						patientIDType = "medrec_num";
					}
					break;
				case 1:
					patId = arrayType[n];
					if (patId.CompareTo(keyValue) == 0)
					{
						patientIDType = "patient_id";
					}
					break;
				case 2:
					acct = arrayType[n];
					if (acct.CompareTo(keyValue) == 0)
					{
						patientIDType = "account_num";
					}
					break;
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "getting the patient ID type", "GetPatientIDTypeForBGA", "BGA");
		}
		return patientIDType;
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

	private string DateTime2DML(DateTime dt)
	{
		string ret = "";
		DateTime FacilityLocalTime = TimeZoneInfo.ConvertTime(dt, m_TimeZoneInfo);
		DateTime UTCTime = TimeZoneInfo.ConvertTime(dt, TimeZoneInfo.Utc);
		if (FacilityLocalTime >= UTCTime)
		{
			TimeSpan tzDif = FacilityLocalTime - UTCTime;
			return FacilityLocalTime.ToString("s") + "+" + ((tzDif.Hours < 10) ? "0" : "") + tzDif.Hours.ToString("d") + ":" + ((tzDif.Minutes < 10) ? "0" : "") + tzDif.Minutes.ToString("d");
		}
		TimeSpan tzDif2 = UTCTime - FacilityLocalTime;
		return FacilityLocalTime.ToString("s") + "-" + ((tzDif2.Hours < 10) ? "0" : "") + tzDif2.Hours.ToString("d") + ":" + ((tzDif2.Minutes < 10) ? "0" : "") + tzDif2.Minutes.ToString("d");
	}

	private string DateTime2DMLCenti(DateTime dt)
	{
		string ret = "";
		DateTime FacilityLocalTime = TimeZoneInfo.ConvertTime(dt, m_TimeZoneInfo);
		DateTime UTCTime = TimeZoneInfo.ConvertTime(dt, TimeZoneInfo.Utc);
		string centiseconds = (FacilityLocalTime.Millisecond / 10).ToString("d2") + "0";
		if (FacilityLocalTime >= UTCTime)
		{
			TimeSpan tzDif = FacilityLocalTime - UTCTime;
			return FacilityLocalTime.ToString("s") + "." + centiseconds + "+" + ((tzDif.Hours < 10) ? "0" : "") + tzDif.Hours.ToString("d") + ":" + ((tzDif.Minutes < 10) ? "0" : "") + tzDif.Minutes.ToString("d");
		}
		TimeSpan tzDif2 = UTCTime - FacilityLocalTime;
		return FacilityLocalTime.ToString("s") + "." + centiseconds + "-" + ((tzDif2.Hours < 10) ? "0" : "") + tzDif2.Hours.ToString("d") + ":" + ((tzDif2.Minutes < 10) ? "0" : "") + tzDif2.Minutes.ToString("d");
	}

	private DateTime DMLToSystemDateTime(string DMLtime)
	{
		DateTime SystemTime = DateTime.MinValue;
		try
		{
			if (DMLtime.Length >= 25)
			{
				int iSign = 1;
				int pOffset = DMLtime.IndexOf('+');
				if (pOffset < 0)
				{
					pOffset = DMLtime.LastIndexOf('-');
					iSign = -1;
				}
				string sdatetime = DMLtime.Substring(0, 10) + " " + DMLtime.Substring(11, pOffset - 11);
				DateTime FacilityLocalTime = DateTime.Parse(sdatetime);
				if (pOffset >= 19)
				{
					int offsethours = Convert.ToInt32(DMLtime.Substring(pOffset + 1, 2));
					int offsetminutes = Convert.ToInt32(DMLtime.Substring(pOffset + 4, 2));
					offsetminutes += offsethours * 60;
					offsetminutes *= iSign;
					TimeSpan Offset = TimeSpan.FromMinutes(Convert.ToDouble(offsetminutes));
					DateTime UTCTime = FacilityLocalTime - Offset;
					SystemTime = TimeZoneInfo.ConvertTime(UTCTime, TimeZoneInfo.Utc, TimeZoneInfo.Local);
				}
				else
				{
					m_NNBase.ReportErrorDB("Error parsing DML time", "E", "parsing DML time", "DMLToSystemDateTime", DMLtime);
				}
			}
			else
			{
				m_NNBase.ReportErrorDB("Error parsing DML time", "E", "parsing DML time", "DMLToSystemDateTime", DMLtime);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception)
		{
			m_NNBase.ReportErrorDB("Error parsing DML time", "E", "parsing DML time", "DMLToSystemDateTime", DMLtime);
		}
		return SystemTime;
	}

	private DateTime DMLFacilityLocalDateTime(string DMLtime)
	{
		DateTime FacilityLocalTime = DateTime.MinValue;
		try
		{
			if (DMLtime.Length >= 19)
			{
				string sdatetime = DMLtime.Substring(0, 10) + " " + DMLtime.Substring(11, 8);
				FacilityLocalTime = DateTime.Parse(sdatetime);
			}
			else
			{
				m_NNBase.ReportErrorDB("Error parsing DML time", "E", "parsing DML time", "DMLFacilityLocalDateTime", DMLtime);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception)
		{
			m_NNBase.ReportErrorDB("Error parsing DML time", "E", "parsing DML time", "DMLFacilityLocalDateTime", DMLtime);
		}
		return FacilityLocalTime;
	}

	private string ExpDate2DML(DateTime FacilityLocalTime)
	{
		string ret = "";
		DateTime UTCTime = TimeZoneInfo.ConvertTime(FacilityLocalTime, m_TimeZoneInfo, TimeZoneInfo.Utc);
		if (FacilityLocalTime >= UTCTime)
		{
			TimeSpan tzDif = FacilityLocalTime - UTCTime;
			return FacilityLocalTime.Date.ToString("s").Substring(0, 10) + "T23:59:59+" + ((tzDif.Hours < 10) ? "0" : "") + tzDif.Hours.ToString("d") + ":" + ((tzDif.Minutes < 10) ? "0" : "") + tzDif.Minutes.ToString("d");
		}
		TimeSpan tzDif2 = UTCTime - FacilityLocalTime;
		return FacilityLocalTime.Date.ToString("s").Substring(0, 10) + "T23:59:59-" + ((tzDif2.Hours < 10) ? "0" : "") + tzDif2.Hours.ToString("d") + ":" + ((tzDif2.Minutes < 10) ? "0" : "") + tzDif2.Minutes.ToString("d");
	}

	private void ShutDown(string reason, string whoFrom, bool bExit)
	{
		string shutdownstep = "none";
		if (!m_isShutDown && !m_isShuttingDown)
		{
			m_isShuttingDown = true;
			try
			{
				if (m_NNBase.m_isLogging)
				{
					if ((m_pleaseShutDown || m_stopping) && m_ShutdownReason.Length > 0)
					{
						reason = reason + ". Shutdown also called " + m_ShutdownReason;
					}
					m_NNBase.log("ShutDown called because " + reason, isXml: false, whoFrom);
					string ThreadCount = "Thread Count before shutdown:" + Process.GetCurrentProcess().Threads.Count;
					m_NNBase.log(ThreadCount, isXml: false, "dml");
				}
				try
				{
					StopTimer();
					ShutDownTimer();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortExceptionWithinShutDown(shutdownstep);
				}
				catch (Exception ex2)
				{
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("ShutDown exception(" + ex2.Message + ")", isXml: false, "dml");
					}
				}
				shutdownstep = "timer";
				try
				{
					string sport = m_parent.socket.Handle.ToString();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closing client port " + sport + " for instument " + m_serial_id, isXml: false, "dml");
					}
					m_parent.socket.Close();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closed client port " + sport + " for instument " + m_serial_id, isXml: false, "dml");
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
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortExceptionWithinShutDown(shutdownstep);
				}
				catch (Exception ex4)
				{
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("ShutDown exception(" + ex4.Message + ")", isXml: false, "dml");
					}
				}
				shutdownstep = "deviceconnection";
				if (m_ProtocolThread != null && m_ProtocolThread.IsAlive && Thread.CurrentThread.ManagedThreadId != m_ProtocolThread.ManagedThreadId)
				{
					try
					{
						ShutDownProtocol();
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortExceptionWithinShutDown(shutdownstep);
					}
					catch (Exception ex6)
					{
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("ShutDown exception(" + ex6.Message + ")", isXml: false, "dml");
						}
					}
				}
				shutdownstep = "protocol";
				try
				{
					ReleaseBytesBuffer();
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortExceptionWithinShutDown(shutdownstep);
				}
				catch (Exception ex8)
				{
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("ShutDown exception(" + ex8.Message + ")", isXml: false, "dml");
					}
				}
				shutdownstep = "bufferrelease";
				if (bRuntimeDBAvailable)
				{
					try
					{
						if (myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open))
						{
							OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, whoFrom);
						}
						myRuntimeDBWriteCommand.CommandText = $"delete from DBA.communications where Computer_Name = '{m_NNBase.GetHostName()}' and Instrument_ID = '{m_serial_id}' and port_num = '{m_port_num}'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
						}
						if (myRuntimeDBWriteCommand.ExecuteNonQuery() > 0)
						{
							string sCommand = "update DBA.instruments set last_disconnect_dttm = now(*) where inst_id = '" + m_serial_id + "'";
							myRuntimeDBWriteCommand.CommandText = sCommand;
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myRuntimeDBWriteCommand.CommandText, isXml: false, "SQL");
							}
							myRuntimeDBWriteCommand.ExecuteNonQuery();
							m_NNBase.CommAudit(11, "Disconnect", reason);
						}
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortExceptionWithinShutDown(shutdownstep);
					}
					catch (Exception ex10)
					{
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("ShutDown exception(" + ex10.Message + ")", isXml: false, "dml");
						}
					}
				}
				shutdownstep = "tables";
				try
				{
					if (myRuntimeDBConnection != null)
					{
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Closing runtime database connection", isXml: false, "dml");
						}
						myRuntimeDBConnection.Close();
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Closed runtime database connection", isXml: false, "dml");
						}
					}
					if (myStringsDBConnection != null)
					{
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Closing strings database connection", isXml: false, "dml");
						}
						myStringsDBConnection.Close();
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log("Closed strings database connection", isXml: false, "dml");
						}
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortExceptionWithinShutDown(shutdownstep);
				}
				catch (Exception ex12)
				{
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("ShutDown exception(" + ex12.Message + ")", isXml: false, "dml");
					}
				}
				shutdownstep = "dbconnection";
				try
				{
					if (m_NNBase.m_isLogging)
					{
						string ThreadCount2 = "Thread Count before completing shutdown:" + Process.GetCurrentProcess().Threads.Count;
						m_NNBase.log(ThreadCount2, isXml: false, "dml");
					}
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.StopLogging();
					}
				}
				catch (ThreadAbortException)
				{
					handleThreadAbortExceptionWithinShutDown(shutdownstep);
				}
				catch (Exception ex14)
				{
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("ShutDown exception(" + ex14.Message + ")", isXml: false, "dml");
						m_NNBase.StopLogging();
					}
				}
				shutdownstep = "logging";
				try
				{
					GC.Collect();
				}
				catch (Exception)
				{
				}
				shutdownstep = "garbagecollection";
			}
			catch (ThreadAbortException)
			{
				m_NNBase.ForceLogging("ShutdownException");
				m_NNBase.log("Shutdown aborted. Last completed step = " + shutdownstep, isXml: false, "dml");
				try
				{
					ReleaseBytesBuffer();
				}
				catch (Exception)
				{
				}
				m_NNBase.StopLogging();
			}
			finally
			{
				m_isShutDown = true;
			}
		}
		if (bExit)
		{
			LibWrap.ExitThread(0u);
		}
	}

	private void handleThreadAbortExceptionWithinShutDown(string shutdownstep)
	{
		m_NNBase.ForceLogging("ShutdownException");
		m_NNBase.log("Shutdown aborted. Last completed step = " + shutdownstep, isXml: false, "dml");
		try
		{
			ReleaseBytesBuffer();
		}
		catch (Exception)
		{
		}
		m_NNBase.StopLogging();
		LibWrap.ExitThread(0u);
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

	private void ShutDownProtocol()
	{
		if (m_ProtocolThread != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing Protocol", isXml: false, "dml");
			}
			m_ProtocolThread.Abort();
			m_ProtocolThread.Join();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed Protocol", isXml: false, "dml");
			}
		}
		else if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("m_ProtocolThread is null", isXml: false, "dml");
		}
	}

	private void handleException(Exception e, string when, string from, string whoFrom)
	{
		if (m_isShuttingDown)
		{
			return;
		}
		string exceptionStr = "Unhandled exception";
		bool bDBDisconnect = false;
		if (e != null)
		{
			string details = e.Message.ToString();
			if (details.IndexOf("Thread was being aborted") < 0)
			{
				bDBDisconnect = m_NNBase.ExceptionIsDisconnect(e);
				if (bDBDisconnect)
				{
					bRuntimeDBAvailable = false;
					m_NNBase.bDBAvailable = false;
				}
				else
				{
					details = details + " " + e.StackTrace.ToString();
				}
				if (!bDBDisconnect)
				{
					m_NNBase.ForceLogging("Exception");
					m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
				}
				m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : ("Exception " + e.GetType().ToString()), bDBDisconnect ? "E" : "C", when, from, details);
			}
			exceptionStr = "Exception";
		}
		if (whoFrom == "Protocol" || whoFrom == "Timer")
		{
			ShutDown(exceptionStr, whoFrom, bExit: true);
		}
	}

	private void handleThreadAbortException(string whoFrom)
	{
		if (m_NNBase.m_isLogging)
		{
			bool bret = WasAskedToStop();
			m_NNBase.log("Thread aborted " + m_serial_id + (bret ? " - was asked to stop" : ""), isXml: false, whoFrom);
		}
		if (IsAliveAndWell())
		{
			ShutDown("Thread aborted", whoFrom, bExit: true);
		}
		else
		{
			LibWrap.ExitThread(0u);
		}
	}

	private void handleXMLException(XmlException e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString() + " at line: " + e.LineNumber + " " + e.StackTrace.ToString();
			m_NNBase.ForceLogging("XMLException");
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("XML Exception " + e.GetType().ToString(), "C", when, from, details);
			ShutDown("XML Exception", "Protocol", bExit: true);
		}
	}

	private void handleJSONException(JsonException e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString() + " at " + e.StackTrace.ToString();
			m_NNBase.ForceLogging("JSONException");
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
			m_NNBase.ReportErrorDB("JSON Exception " + e.GetType().ToString(), "C", when, from, details);
			ShutDown("JSON Exception", "Protocol", bExit: true);
		}
	}

	private void handleDBException(OdbcException e, string when, string from, string whoFrom)
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
			bRuntimeDBAvailable = false;
		}
		else
		{
			for (int i = 0; i < e.Errors.Count; i++)
			{
				details = details + e.Errors[i].Message + " ";
			}
		}
		if (!bDBDisconnect)
		{
			m_NNBase.ForceLogging("DatabaseException");
			m_NNBase.SendEventReport("Exception", m_NNBase.EventProcessName(m_NNBase.m_WhoAmI));
		}
		m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : "DB Exception", bDBDisconnect ? "E" : "C", when, from, details);
		if (whoFrom == "Protocol")
		{
			ShutDown(bDBDisconnect ? "The database connection has been lost" : "DB Exception", "Protocol", bExit: true);
		}
	}

	public int SendString(string input, bool isPartial, bool trunc, string forLog = null)
	{
		int i = 0;
		try
		{
			lock (m_sendStringLock)
			{
				bool getout = false;
				m_ProtocolSending = true;
				if (m_writebuffer != null)
				{
					Array.Clear(m_writebuffer, 0, m_writebuffer.Length);
				}
				if (!getout)
				{
					i = Encoding.UTF8.GetBytes(input, 0, input.Length, m_writebuffer, 0);
					string logString = string.Empty;
					if (trunc && input.Length > 256)
					{
						if (m_NNBase.m_isLogging)
						{
							logString = (string.IsNullOrEmpty(forLog) ? input : forLog);
							m_NNBase.log(logString.Substring(0, 256) + "..." + logString.Substring(logString.Length - 64), isXml: true, isPartial ? "ICPMGR..." : "ICPMGR   ");
						}
					}
					else if (m_NNBase.m_isLogging)
					{
						logString = (string.IsNullOrEmpty(forLog) ? input : forLog);
						m_NNBase.log(logString, isXml: true, isPartial ? "ICPMGR..." : "ICPMGR   ");
					}
					try
					{
						if (isPartial)
						{
							m_isPartial = true;
						}
						if (m_networkStream.CanWrite)
						{
							m_networkStream.Write(m_writebuffer, 0, i);
						}
						else
						{
							if (m_NNBase.m_isLogging)
							{
								string msg = "Cannot write";
								m_NNBase.log(msg, isXml: true, "ICPMGR");
							}
							ShutDown("Cannot write", "Protocol", bExit: true);
						}
					}
					catch (ThreadAbortException)
					{
						handleThreadAbortException("Protocol");
					}
					catch (Exception ex2)
					{
						ShutDown("Write failed(" + ex2.Message + ")", "Protocol", bExit: true);
					}
					m_ProtocolSending = false;
				}
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "sending message", "SendString", m_NNBase.m_WhoAmI);
		}
		return i;
	}

	public int SendString(string input)
	{
		int i = 0;
		if (m_asyncwritebuffer != null)
		{
			Array.Clear(m_asyncwritebuffer, 0, m_asyncwritebuffer.Length);
		}
		lock (m_sendStringLock)
		{
			i = Encoding.UTF8.GetBytes(input, 0, input.Length, m_asyncwritebuffer, 0);
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(input, isXml: true, "ICPMGR");
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
						m_NNBase.log(msg, isXml: true, "ICPMGR");
					}
					ShutDown("Cannot write", "Timer", bExit: false);
				}
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Timer");
			}
			catch (Exception ex2)
			{
				ShutDown("BeginWrite failed(" + ex2.Message + ")", "Timer", bExit: true);
			}
			m_ProtocolSending = false;
			return i;
		}
	}

	private bool OpenDBConnection(ref OdbcConnection myConnection, ref OdbcCommand myReadCommand, ref OdbcCommand myWriteCommand, int iTries, ref bool bDBAvailable, string whoFrom)
	{
		bDBAvailable = m_NNBase.OpenDBConnection(ref myConnection, ref myReadCommand, ref myWriteCommand, iTries);
		if (!bDBAvailable)
		{
			ShutDown("Cannot connect to database", whoFrom, whoFrom == "Protocol");
		}
		return bDBAvailable;
	}

	private void OpenStringsDBConnection()
	{
		if (!m_NNBase.OpenStringsDBConnection(ref myStringsDBConnection, ref myStringsDBReadCommand, 7))
		{
			throw new Exception();
		}
	}

	private void LookupTimeZone(string myFacility, ref string myTimeZoneName, ref TimeZoneInfo myTimeZoneInfo)
	{
		myRuntimeDBReadCommand.CommandText = string.Format("select time_zone from dba.facility_prefs where facility_uuid = (select loc_num from DBA.inst_locations where loc_name = '{0}' and level_num = 1)", myFacility.Replace("'", "''"));
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(myRuntimeDBReadCommand.CommandText, isXml: false, "dml");
		}
		myRuntimeDBReadReader = myRuntimeDBReadCommand.ExecuteReader();
		if (myRuntimeDBReadReader.Read())
		{
			myTimeZoneName = (myRuntimeDBReadReader.IsDBNull(0) ? "" : myRuntimeDBReadReader.GetString(0));
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Time zone is " + myTimeZoneName, isXml: false, "dml");
			}
			try
			{
				myTimeZoneInfo = TimeZoneInfo.FindSystemTimeZoneById(myTimeZoneName);
			}
			catch (ThreadAbortException)
			{
				handleThreadAbortException("Protocol");
			}
			catch (TimeZoneNotFoundException)
			{
				bool bWasLogging = m_NNBase.m_isLogging;
				m_NNBase.ForceLogging("TimeZoneNotFound");
				m_NNBase.log("Time zone not found " + myTimeZoneName, isXml: false, "dml");
				m_NNBase.ReportErrorDB("Time zone " + myTimeZoneName + " was not found", "E", "looking up time zone", "LookupTimeZone", "");
				if (!bWasLogging)
				{
					m_NNBase.StopLogging();
				}
			}
			catch (Exception e)
			{
				handleException(e, "looking up time zone", "LookupTimeZone", "dml");
			}
		}
		myRuntimeDBReadReader.Close();
		if (myTimeZoneName == null || myTimeZoneName.Length == 0)
		{
			myTimeZoneName = TimeZone.CurrentTimeZone.StandardName;
		}
		if (myTimeZoneInfo == null)
		{
			myTimeZoneInfo = TimeZoneInfo.Local;
		}
	}

	private void ProtocolThread()
	{
		if (bIsListCreator)
		{
			m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, "ListCreator", "ICPMGR", "ICP");
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("List creator initialized", isXml: false, "ICPMGR");
			}
		}
		if (bIsDeviceProtocol)
		{
			m_NNBase.m_LogName = Guid.NewGuid().ToString("N");
			m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, m_NNBase.m_LogName, "ICPMGR", "ICP");
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Connection established via local port " + m_port_num, isXml: false, "DML");
			}
		}
		if (m_NNBase.m_isLogging)
		{
			string ThreadCount = "Thread Count on Start:" + Process.GetCurrentProcess().Threads.Count;
			m_NNBase.log(ThreadCount, isXml: false, bIsListCreator ? "ListCreator" : "DML");
		}
		if ((myRuntimeDBConnection == null || !myRuntimeDBConnection.State.Equals(ConnectionState.Open)) && !OpenDBConnection(ref myRuntimeDBConnection, ref myRuntimeDBReadCommand, ref myRuntimeDBWriteCommand, 7, ref bRuntimeDBAvailable, "Listener"))
		{
			return;
		}
		m_NNBase.BeginProcessControl();
		m_NNBase.GetProcessControlValue("ICP", "AlwaysSend", ref m_AlwaysSend);
		m_NNBase.GetProcessControlValue("ICP", "OpListFreq", ref OpListFreq);
		m_NNBase.GetProcessControlValue("ICP", "PatListFreq", ref PatListFreq);
		m_NNBase.GetProcessControlValue("ICP", "MaxAddDelPerMsg", ref MaxAddDelPerMsg);
		m_NNBase.GetProcessControlValue("ICP", "MaxAddDelPerIncr", ref MaxAddDelPerIncr);
		if (bIsListCreator)
		{
			m_maxVisitLocations = 5000;
			m_NNBase.GetProcessControlValue("ICP", "MaxVisitLocations", ref m_maxVisitLocations);
		}
		m_NNBase.EndProcessControl();
		m_busy = true;
		m_waiting = true;
		cmTimer = new FlexTimer();
		cmTimer.theCallBack = OnCmTimedEvent;
		if (bIsDeviceProtocol)
		{
			cmTimer.Interval = 60000u;
		}
		else
		{
			cmTimer.Interval = 100u;
		}
		cmTimer.Start();
		try
		{
			while (IsAliveAndWell())
			{
				if (bIsDeviceProtocol)
				{
					if (OnReadComplete() && IsAliveAndWell())
					{
						StepProtocolState();
					}
				}
				else
				{
					Thread.Sleep(500);
				}
			}
			if (WasAskedToStop())
			{
				ShutDown("Shutdown requested", "Protocol", bExit: true);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
	}

	private bool CheckDMLSessionList()
	{
		lock (Port.AsynchNetworkServer.ServerCommon.m_handlers)
		{
			Port.AsynchNetworkServer.ServerCommon.m_handlers.StopDuplicateInstrument(this, m_serial_id, ref m_NNBase);
		}
		return true;
	}

	private void NovaNetTopics()
	{
		m_SupportedTopic.Add("D_EV", value: false);
		m_SupportedTopic.Add("DTV", value: false);
		m_SupportedTopic.Add("OP_LST", value: false);
		m_SupportedTopic.Add("OP_LST_I", value: false);
		m_SupportedTopic.Add("PT_LST", value: false);
		m_SupportedTopic.Add("PT_LST_I", value: false);
		m_SupportedTopic.Add("NOVA.PHYS", value: false);
		m_SupportedTopic.Add("NOVA.PHYS_I", value: false);
		m_SupportedTopic.Add("NOVA.LOC", value: false);
		m_SupportedTopic.Add("NOVA.STATSTRIP.SETUP", value: false);
		m_SupportedTopic.Add("NOVA.PHOENIX.SETUP", value: false);
		m_SupportedTopic.Add("NOVA.BLOODGAS.SETUP", value: false);
		m_SupportedTopic.Add("NOVA.WIFI_SETUP", value: false);
		m_SupportedTopic.Add("NOVA.WIFI_CERT", value: false);
		m_SupportedTopic.Add("NOVA.REAG", value: false);
		m_SupportedTopic.Add("NOVA.FRM", value: false);
		m_SupportedTopic.Add("NOVA.MANUAL_TEST", value: false);
		m_SupportedTopic.Add("NOVA.ANALYZER_STATE", value: false);
		m_SupportedTopic.Add("NOVA.CARTRIDGE_STATUS", value: false);
		m_SupportedTopic.Add("NOVA.TEST_STATUS", value: false);
		m_SupportedTopic.Add("NOVA.MAINT_ACTIVITY", value: false);
	}

	private void NovaNetDirectives()
	{
		m_SupportedDirective.Add("SET_TIME", value: false);
		m_SupportedDirective.Add("START_CONTINUOUS", value: false);
		m_SupportedDirective.Add("ORDER_QUERY", value: false);
		m_SupportedDirective.Add("PATIENT_QUERY", value: false);
		m_SupportedDirective.Add("OPERATOR_QUERY", value: false);
		m_SupportedDirective.Add("DTV.NOVA.EXEC_SEQ", value: false);
		m_SupportedDirective.Add("DTV.NOVA.PRIME_PLUS_SETUP", value: false);
	}

	public bool SupportRemoteControl()
	{
		if (m_SupportedDirective.ContainsKey("EXEC_SEQ_ABG_2PT_CAL") || m_SupportedDirective.ContainsKey("EXEC_SEQ_COOX_CAL") || m_SupportedDirective.ContainsKey("EXEC_SEQ_COOX_DEPRO") || m_SupportedDirective.ContainsKey("EXEC_SEQ_QC"))
		{
			return true;
		}
		return false;
	}

	private Sample_Table GetANewRecord()
	{
		Sample_Table empty = default(Sample_Table);
		empty.sample_key_num = string.Empty;
		empty.Accession_num = string.Empty;
		empty.sample_Date = DateTime.MinValue;
		empty.transmitted_flag = "F";
		empty.control_type = string.Empty;
		empty.control_lot_num = string.Empty;
		empty.strip_lot_num = string.Empty;
		empty.xml_text = string.Empty;
		empty.patient_id = string.Empty;
		empty.medrec_num = string.Empty;
		empty.account_num = string.Empty;
		empty.fac_name = string.Empty;
		empty.loc_name = string.Empty;
		empty.device_serial = m_serial_id;
		empty.saved_to_history_db_flag = "F";
		empty.device_type = m_inst_type;
		empty.device_sw_ver = m_sw_version;
		empty.device_name = m_device_name;
		empty.lot_level = string.Empty;
		empty.internal_external = string.Empty;
		return empty;
	}

	private string MakeJSONTestIssue(string issueType, string issueString)
	{
		string jsonString = string.Empty;
		try
		{
			JsonWriter jsonW = new JsonWriter();
			jsonW.WriteObjectStart();
			jsonW.WritePropertyName("issues");
			string[] issues = issueString.Split(',');
			jsonW.WriteArrayStart();
			for (int n = 0; n < issues.Length; n++)
			{
				jsonW.WriteObjectStart();
				jsonW.WritePropertyName("issue");
				jsonW.Write(issues[n]);
				jsonW.WriteObjectEnd();
			}
			jsonW.WriteArrayEnd();
			jsonW.WriteObjectEnd();
			jsonString = jsonW.ToString();
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (JsonException ex2)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("JSON Exception " + ex2.Message, isXml: false, "MakeJSONTestIssue " + m_serial_id);
			}
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Exception " + ex3.Message, isXml: false, "MakeJSONTestIssue " + m_serial_id);
			}
		}
		return jsonString;
	}

	private void ReleaseBytesBuffer()
	{
		if (m_readbuffer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Releasing read buffer", isXml: false, "dml");
			}
			ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref m_readbuffer);
			m_readbuffer = null;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Read buffer released", isXml: false, "dml");
			}
		}
		if (m_writebuffer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Releasing write buffer", isXml: false, "dml");
			}
			ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref m_writebuffer);
			m_writebuffer = null;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Write buffer released", isXml: false, "dml");
			}
		}
		if (m_asyncwritebuffer != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Releasing asyncwrite buffer", isXml: false, "dml");
			}
			ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref m_asyncwritebuffer);
			m_asyncwritebuffer = null;
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("asyncwrite buffer released", isXml: false, "dml");
			}
		}
	}

	private void CleanBufferList()
	{
		for (int n = 0; n < m_readBufferList.Count; n++)
		{
			_ = m_readBufferList[n];
			byte[] thisBuffer = m_readBufferList[n];
			ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref thisBuffer);
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
			bigBuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(32768 * (bufferCount + 1));
			long desIndex = 0L;
			for (int n = 0; n < bufferCount; n++)
			{
				desIndex = n * 32768;
				byte[] thisBuffer = m_readBufferList[n];
				Array.Copy(thisBuffer, 0L, bigBuffer, desIndex, 32768L);
			}
			ret = true;
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception e)
		{
			handleException(e, "getting buffer list", "GetBufferList", "DML");
		}
		return ret;
	}
}
