using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Xml;
using System.Xml.Serialization;
using Autofac;
using NLog;
using Yzkj.Novanet.Bussiness.Bus;
using Yzkj.Novanet.Bussiness.Model;

namespace Yzkj.Novanet.WinService;

public class NovaMessageHandler
{
	private readonly LoggerWrap Logger;

	private readonly NovaSyncBus NovaSyncBus;

	private byte[] result = new byte[1024];

	private int cmd_id = 4000;

	private string control_id;

	private Socket ClientSocket;

	private bool RECEIVABLE = true;

	private int MAXCOUNT = 20;

	private DMLSTATE TSTATE;

	private string status_dttm;

	private string new_observations_qty;

	private string new_events_qty;

	private string condition_cd;

	private string observations_update_dttm;

	private string operators_update_dttm;

	private string events_update_dttm;

	private string patients_update_dttm;

	private string setup_update_dttm;

	private string phys_update_dttm;

	private string reag_update_dttm;

	private string loc_list_update_dttm;

	private string device_id;

	private string vendor_id;

	private string model_id;

	private string serial_id;

	private string manufacturer_name;

	private string hw_version;

	private string sw_version;

	private string device_name;

	private string application_timeout;

	private string max_message_sz;

	private string connection_profile_cd;

	private string m_facility;

	private string m_location;

	private List<string> topics_supported_cd;

	private List<string> directives_supported_cd;

	private NovaSTModel NSTModel;

	private List<byte> tempPackage = new List<byte>();

	private Timer timeoutExit;

	private int ack_control_id;

	private int pi;

	private bool page_over;

	private bool? is_initialized;

	public string ClientIp => ((IPEndPoint)ClientSocket.RemoteEndPoint).Address.ToString();

	public bool SetupRefresh
	{
		get
		{
			if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00")
			{
				return true;
			}
			if (NSTModel == null || !NSTModel.ST_Setup.HasValue || string.IsNullOrEmpty(setup_update_dttm))
			{
				return false;
			}
			DateTime dateTime = DML2DateTime(setup_update_dttm);
			return NSTModel.ST_Setup.Value > dateTime;
		}
	}

	public bool LocationRefresh
	{
		get
		{
			if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00")
			{
				return true;
			}
			if (NSTModel == null || !NSTModel.ST_Location.HasValue || string.IsNullOrEmpty(loc_list_update_dttm))
			{
				return false;
			}
			DateTime dateTime = DML2DateTime(loc_list_update_dttm);
			return NSTModel.ST_Location.Value > dateTime;
		}
	}

	public bool NurseRefresh
	{
		get
		{
			if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00")
			{
				return true;
			}
			if (NSTModel == null || !NSTModel.ST_Nurse.HasValue || string.IsNullOrEmpty(operators_update_dttm))
			{
				return false;
			}
			DateTime dateTime = DML2DateTime(operators_update_dttm);
			return NSTModel.ST_Nurse.Value > dateTime;
		}
	}

	public bool PatientRefresh
	{
		get
		{
			if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00")
			{
				return true;
			}
			if (NSTModel == null || !NSTModel.ST_Patient.HasValue || string.IsNullOrEmpty(patients_update_dttm))
			{
				return false;
			}
			DateTime dateTime = DML2DateTime(patients_update_dttm);
			return NSTModel.ST_Patient.Value > dateTime;
		}
	}

	public bool ReagentRefresh
	{
		get
		{
			if (setup_update_dttm == "2000-01-01T00:00:00.00+08:00")
			{
				return true;
			}
			if (NSTModel == null || !NSTModel.ST_Reagent.HasValue || string.IsNullOrEmpty(reag_update_dttm))
			{
				return false;
			}
			DateTime dateTime = DML2DateTime(reag_update_dttm);
			return NSTModel.ST_Reagent.Value > dateTime;
		}
	}

	public NovaMessageHandler(Socket cSocket)
	{
		ClientSocket = cSocket;
		Logger = new LoggerWrap(LogManager.GetCurrentClassLogger());
		NovaSyncBus = AutoFacConfig.container.Resolve<NovaSyncBus>();
		SetCloseClock();
	}

	private void SetCloseClock()
	{
		timeoutExit = new Timer(delegate
		{
			Logger.Info("线程执行超时退出");
			timeoutExit.Dispose();
			CloseConnection();
		}, null, 1800000, -1);
	}

	public void ReceiveMessage()
	{
		while (RECEIVABLE)
		{
			try
			{
				DateTime now = DateTime.Now;
				DateTime2DML(now);
				result = new byte[ClientSocket.ReceiveBufferSize];
				if (!ClientSocket.Poll(-1, SelectMode.SelectRead))
				{
					continue;
				}
				int num = ClientSocket.Receive(result);
				if (num == 0)
				{
					RECEIVABLE = false;
					CloseConnection();
					break;
				}
				tempPackage.AddRange(result.Take(num));
				string text = Encoding.UTF8.GetString(tempPackage.ToArray(), 0, tempPackage.Count);
				if (!string.IsNullOrWhiteSpace(text))
				{
					XmlDocument xmlDocument = new XmlDocument();
					xmlDocument.LoadXml(text);
					XmlNodeReader xmlNodeReader = new XmlNodeReader(xmlDocument);
					xmlNodeReader.Read();
					string localName = xmlNodeReader.LocalName;
					tempPackage.Clear();
					Logger.Debug(text);
					switch (localName)
					{
					case "ACK.R01":
						HandleACK_R01(xmlDocument);
						break;
					case "HEL.R01":
						HandleHEL_R01(xmlNodeReader);
						break;
					case "DST.R01":
						HandleDST_R01(xmlNodeReader);
						break;
					case "END.R01":
						HandleEND_R01();
						break;
					case "EOT.R01":
						HandleEOT_R01(xmlDocument);
						break;
					case "ESC.R01":
						Thread.CurrentThread.Abort();
						break;
					case "EVS.R01":
						ProcessEvents(xmlDocument.DocumentElement);
						break;
					case "KPA.R01":
						ProcessKeepAlive(xmlNodeReader);
						break;
					case "OBS.R01":
						HandleOBS_R01(xmlDocument);
						break;
					case "OBS.R02":
						HandleOBS_R02(xmlDocument);
						break;
					default:
					{
						string note = "Unexepected message type " + localName;
						SendEscape(note);
						CloseConnection();
						Thread.CurrentThread.Abort();
						break;
					}
					case "DTV.R01":
					case "DTV.R02":
					case "DTV.VENDOR":
						break;
					}
					StatusWork();
				}
			}
			catch (XmlException e)
			{
				Logger.Error(e, "Xml数据解析异常：" + Encoding.UTF8.GetString(tempPackage.ToArray(), 0, tempPackage.Count));
			}
			catch (Exception ex)
			{
				Logger.Error(ex, "ReceiveMessage发生错误" + ex.Message);
				CloseConnection();
				break;
			}
		}
	}

	private void HandleACK_R01(XmlDocument doc)
	{
		XmlNode xmlNode = doc.SelectSingleNode("/ACK.R01/ACK/ACK.ack_control_id");
		ack_control_id = int.Parse(xmlNode.Attributes["V"].Value);
		if (TSTATE == DMLSTATE.SET_TIME)
		{
			TSTATE = DMLSTATE.SET_TIME_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.LOC_SEND_EOT)
		{
			TSTATE = DMLSTATE.LOC_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.SETUP_SEND_EOT)
		{
			TSTATE = DMLSTATE.SETUP_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.OPR_SEND_EOT)
		{
			TSTATE = DMLSTATE.OPR_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.PAT_SEND_EOT)
		{
			TSTATE = DMLSTATE.PAT_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.REAG_SEND_EOT)
		{
			TSTATE = DMLSTATE.REAG_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.CONTINUOUS)
		{
			TSTATE = DMLSTATE.CONTINUOUS_RCV_ACK;
		}
		else if (TSTATE == DMLSTATE.END)
		{
			Thread.CurrentThread.Abort();
		}
	}

	private void HandleHEL_R01(XmlNodeReader reader)
	{
		ProcessHello(reader);
		NovaSyncBus.SaveDeviceConnect(new DeviceModel
		{
			SerialNo = serial_id,
			Hospital = m_facility,
			Depart = m_location,
			Name = device_name,
			DeviceId = device_id
		});
		NSTModel = NovaSyncBus.GetNovaST(m_facility, m_location);
		Logger.Info(serial_id, "建立连接", ClientIp, reader.ReadOuterXml());
		TSTATE = DMLSTATE.WAIT_STATUS;
	}

	private void HandleDST_R01(XmlNodeReader reader)
	{
		ProcessDeviceStatus(reader);
		Logger.Info(serial_id, "发送设备状态", ClientIp, reader.ReadOuterXml());
		TSTATE = DMLSTATE.ACK_STATUS;
	}

	private void HandleOBS_R01(XmlDocument doc)
	{
		Logger.Info(serial_id, "同步血糖结果", ClientIp, doc.OuterXml);
		ProcessObservation(doc.DocumentElement);
	}

	private void HandleOBS_R02(XmlDocument doc)
	{
		Logger.Info(serial_id, "同步质控结果", ClientIp, doc.OuterXml);
		ProcessObservation2(doc.DocumentElement);
	}

	private void HandleEOT_R01(XmlDocument doc)
	{
		string value = doc.SelectSingleNode("/EOT.R01/EOT/EOT.topic_cd").Attributes["V"].Value;
		if (value == "OBS")
		{
			TSTATE = DMLSTATE.OBS_EOT;
			new_observations_qty = "0";
			Logger.Info(serial_id, "血糖结果同步完成", ClientIp, "");
		}
		else if (value == "EVS")
		{
			TSTATE = DMLSTATE.EVS_EOT;
			new_events_qty = "0";
			Logger.Info(serial_id, "设备事件同步完成", ClientIp, "");
		}
	}

	private void HandleEND_R01()
	{
		SendAcknowledgeMessage(control_id, isError: false);
		CloseConnection();
	}

	private void StatusWork()
	{
		switch (TSTATE)
		{
		case DMLSTATE.ACK_STATUS:
		{
			int num = int.Parse(new_observations_qty);
			if (num > 0)
			{
				Logger.Info(serial_id, $"有{num}条新血糖结果待同步", ClientIp, "");
				TSTATE = DMLSTATE.REQ_OBS;
				RequestFromDevice("ROBS");
			}
			else
			{
				Logger.Info(serial_id, "无新血糖结果待同步", ClientIp, "");
				TSTATE = DMLSTATE.OBS_EOT;
				StatusWork();
			}
			break;
		}
		case DMLSTATE.OBS_EOT:
			if (int.Parse(new_events_qty) > 0)
			{
				TSTATE = DMLSTATE.REQ_EVS;
				RequestFromDevice("RDEV");
			}
			else
			{
				TSTATE = DMLSTATE.EVS_EOT;
				StatusWork();
			}
			break;
		case DMLSTATE.EVS_EOT:
			SendDateTime();
			TSTATE = DMLSTATE.SET_TIME;
			break;
		case DMLSTATE.SET_TIME_RCV_ACK:
			if (!IsInitialized() && SetupRefresh)
			{
				SendSetup();
				TSTATE = DMLSTATE.SETUP_SEND_EOT;
			}
			else
			{
				TSTATE = DMLSTATE.SETUP_EOT;
				StatusWork();
			}
			break;
		case DMLSTATE.SETUP_RCV_ACK:
			if (!IsInitialized() && SetupRefresh)
			{
				SendEotMessage("NOVA.STATSTRIP.SETUP", DateTime.Now);
			}
			Logger.Info(serial_id, "Setup配置完成", ClientIp, "");
			TSTATE = DMLSTATE.SETUP_EOT;
			StatusWork();
			break;
		case DMLSTATE.SETUP_EOT:
			if (IsInitialized())
			{
				SendLocationList();
				TSTATE = DMLSTATE.LOC_SEND_EOT;
			}
			else if (LocationRefresh)
			{
				SendLocationList();
				TSTATE = DMLSTATE.LOC_SEND_EOT;
			}
			else
			{
				TSTATE = DMLSTATE.LOC_RCV_ACK;
				StatusWork();
			}
			break;
		case DMLSTATE.LOC_RCV_ACK:
			if (IsInitialized() || LocationRefresh)
			{
				SendEotMessage("NOVA.LOC", DateTime.Now);
			}
			Logger.Info(serial_id, "医院科室信息同步完成", ClientIp, "");
			TSTATE = DMLSTATE.LOC_EOT;
			page_over = false;
			StatusWork();
			break;
		case DMLSTATE.LOC_EOT:
		case DMLSTATE.OPR_RCV_ACK:
			if (!IsInitialized() && NurseRefresh && !page_over)
			{
				if (operators_update_dttm.StartsWith("2000-01-01"))
				{
					TSTATE = DMLSTATE.OPR_SEND_EOT;
					if (SendOperatorList() == 0)
					{
						pi = 0;
						TSTATE = DMLSTATE.OPR_EOT;
						page_over = false;
						StatusWork();
					}
				}
				else
				{
					TSTATE = DMLSTATE.OPR_SEND_EOT;
					if (SendOperator2List() == 0)
					{
						pi = 0;
						TSTATE = DMLSTATE.OPR_EOT;
						page_over = false;
						StatusWork();
					}
				}
			}
			else
			{
				if (page_over)
				{
					SendEotMessage("OPL", DateTime.Now);
				}
				Logger.Info(serial_id, "护士信息同步完成", ClientIp, "");
				pi = 0;
				TSTATE = DMLSTATE.OPR_EOT;
				page_over = false;
				StatusWork();
			}
			break;
		case DMLSTATE.OPR_EOT:
		case DMLSTATE.PAT_RCV_ACK:
			if (!IsInitialized() && PatientRefresh && !page_over)
			{
				TSTATE = DMLSTATE.PAT_SEND_EOT;
				if (SendPatientList() == 0)
				{
					pi = 0;
					TSTATE = DMLSTATE.PAT_EOT;
					page_over = false;
					StatusWork();
				}
				break;
			}
			if (page_over)
			{
				SendEotMessage("PTL", DateTime.Now);
			}
			Logger.Info(serial_id, "患者信息同步完成", ClientIp, "");
			pi = 0;
			TSTATE = DMLSTATE.PAT_EOT;
			page_over = false;
			StatusWork();
			break;
		case DMLSTATE.PAT_EOT:
		case DMLSTATE.REAG_RCV_ACK:
			if (!IsInitialized() && ReagentRefresh && !page_over)
			{
				TSTATE = DMLSTATE.REAG_SEND_EOT;
				if (SendReagents() == 0)
				{
					pi = 0;
					TSTATE = DMLSTATE.REAG_EOT;
					page_over = false;
					StatusWork();
				}
				break;
			}
			if (page_over)
			{
				SendEotMessage("NOVA.REAG", DateTime.Now);
			}
			Logger.Info(serial_id, "试剂信息同步完成", ClientIp, "");
			pi = 0;
			TSTATE = DMLSTATE.REAG_EOT;
			page_over = false;
			StatusWork();
			break;
		case DMLSTATE.REAG_EOT:
		{
			PreferenceModel preference = NovaSyncBus.GetPreference(m_facility, m_location);
			if (IsInitialized() || preference == null)
			{
				TSTATE = DMLSTATE.END;
				SendTerminate("NRM", "");
				Logger.Info(serial_id, "通信结束", ClientIp, "");
			}
			else if (preference.AutoReConnect)
			{
				TSTATE = DMLSTATE.END;
				SendTerminate("NRM", "RECONNECT:" + preference.CycleMinutes.Value);
			}
			else
			{
				SendContinuous();
				TSTATE = DMLSTATE.CONTINUOUS;
			}
			break;
		}
		case DMLSTATE.REQ_OBS:
		case DMLSTATE.REQ_EVS:
		case DMLSTATE.SET_TIME:
		case DMLSTATE.SETUP_SEND_EOT:
		case DMLSTATE.LOC_SEND_EOT:
		case DMLSTATE.OPR_SEND_EOT:
		case DMLSTATE.PAT_SEND_EOT:
		case DMLSTATE.PHYS_SEND_EOT:
		case DMLSTATE.PHYS_RCV_ACK:
		case DMLSTATE.PHYS_EOT:
		case DMLSTATE.FIRM_SEND_EOT:
		case DMLSTATE.FIRM_RCV_ACK:
		case DMLSTATE.FIRM_EOT:
		case DMLSTATE.REAG_SEND_EOT:
		case DMLSTATE.CONTINUOUS_RCV_ACK:
			break;
		}
	}

	private bool IsInitialized()
	{
		if (!is_initialized.HasValue)
		{
			is_initialized = !NovaSyncBus.ExistLocation(m_facility, m_location);
		}
		return is_initialized.Value;
	}

	private void ProcessHello(XmlNodeReader reader)
	{
		string text = "";
		char[] separator = new char[1] { '^' };
		while (reader.Read())
		{
			reader.MoveToContent();
			if (reader.NodeType != XmlNodeType.Element || !reader.IsStartElement())
			{
				continue;
			}
			switch (reader.LocalName)
			{
			case "HDR.control_id":
				control_id = reader.GetAttribute("V");
				break;
			case "DEV.vendor_id":
				vendor_id = reader.GetAttribute("V");
				break;
			case "DEV.device_id":
				device_id = reader.GetAttribute("V");
				text = reader.ReadString();
				if (text.Length > 0)
				{
					string[] array = text.Split(separator, 2);
					m_facility = array[0];
					if (array.Length > 1)
					{
						m_location = array[1];
					}
				}
				break;
			case "DEV.facility":
				m_facility = reader.GetAttribute("V");
				break;
			case "DEV.model_id":
				model_id = reader.GetAttribute("V");
				break;
			case "DEV.location":
				m_location = reader.GetAttribute("V");
				break;
			case "DEV.serial_id":
				serial_id = reader.GetAttribute("V");
				break;
			case "DEV.manufacturer_name":
				manufacturer_name = reader.GetAttribute("V");
				break;
			case "DEV.device_name":
				device_name = reader.GetAttribute("V");
				break;
			case "DEV.hw_version":
				hw_version = reader.GetAttribute("V");
				break;
			case "DEV.sw_version":
				sw_version = reader.GetAttribute("V");
				break;
			case "DSC.max_message_sz":
				max_message_sz = reader.GetAttribute("V");
				if (max_message_sz.Length == 0)
				{
					max_message_sz = "4096";
				}
				break;
			case "DSC.topics_supported_cd":
				if (topics_supported_cd == null)
				{
					topics_supported_cd = new List<string>();
				}
				topics_supported_cd.Add(reader.GetAttribute("V"));
				break;
			case "DSC.directives_supported_cd":
				if (directives_supported_cd == null)
				{
					directives_supported_cd = new List<string>();
				}
				directives_supported_cd.Add(reader.GetAttribute("V"));
				break;
			default:
				reader.GetAttribute("V");
				text = reader.Value;
				break;
			}
		}
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private void ProcessDeviceStatus(XmlNodeReader reader)
	{
		new_observations_qty = "0";
		new_events_qty = "0";
		condition_cd = "";
		operators_update_dttm = "2000-01-01T00:00:00.00-04:00";
		patients_update_dttm = "2000-01-01T00:00:00.00-04:00";
		setup_update_dttm = "2000-01-01T00:00:00.00-04:00";
		loc_list_update_dttm = "2000-01-01T00:00:00.00-04:00";
		phys_update_dttm = "2000-01-01T00:00:00.00-04:00";
		reag_update_dttm = "2000-01-01T00:00:00.00-04:00";
		while (reader.Read())
		{
			reader.MoveToContent();
			if (reader.NodeType == XmlNodeType.Element && reader.IsStartElement())
			{
				switch (reader.LocalName)
				{
				case "HDR.control_id":
					control_id = reader.GetAttribute("V");
					break;
				case "DST.new_observations_qty":
					new_observations_qty = reader.GetAttribute("V");
					break;
				case "DST.new_events_qty":
					new_events_qty = reader.GetAttribute("V");
					break;
				case "DST.condition_cd":
					condition_cd = reader.GetAttribute("V");
					break;
				case "DST.operators_update_dttm":
					operators_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.patients_update_dttm":
					patients_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.setup_update_dttm":
					setup_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.loc_list_update_dttm":
					loc_list_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.phys_update_dttm":
					phys_update_dttm = reader.GetAttribute("V");
					break;
				case "DST.reag_update_dttm":
					reag_update_dttm = reader.GetAttribute("V");
					break;
				}
			}
		}
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private void ProcessObservation(XmlElement documentElement)
	{
		XmlElement xmlElement = (XmlElement)documentElement.SelectSingleNode("HDR/HDR.control_id");
		if (xmlElement != null)
		{
			control_id = xmlElement.GetAttribute("V");
		}
		List<SampleDataModel> list = new List<SampleDataModel>();
		foreach (XmlNode item in documentElement.SelectNodes("SVC"))
		{
			_ = (XmlElement)item;
			string hospital = m_facility;
			string depart = m_location;
			DateTime minValue = DateTime.MinValue;
			xmlElement = (XmlElement)item.SelectSingleNode("SVC.role_cd");
			xmlElement.GetAttribute("V");
			xmlElement = (XmlElement)item.SelectSingleNode("SVC.observation_dttm");
			minValue = DMLDateTime(xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("PT/PT.patient_id");
			string patientId = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("PT/PT.location");
			string text = "";
			if (xmlElement != null)
			{
				text = xmlElement.GetAttribute("V");
			}
			if (!string.IsNullOrWhiteSpace(text))
			{
				string[] array = text.Split('^');
				if (!string.IsNullOrEmpty(array[0]))
				{
					hospital = array[0];
				}
				if (array.Length > 1 && !string.IsNullOrEmpty(array[1]))
				{
					depart = array[1];
				}
			}
			xmlElement = (XmlElement)item.SelectSingleNode("PT/OBS/OBS.value");
			string text2;
			string unit;
			if (xmlElement != null)
			{
				text2 = xmlElement.GetAttribute("V");
				unit = xmlElement.GetAttribute("U");
			}
			else
			{
				text2 = "";
				unit = "";
			}
			xmlElement = (XmlElement)item.SelectSingleNode("PT/OBS/OBS.status_cd");
			string obsStatus = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("PT/OBS/OBS.interpretation_cd");
			string interpretation = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			string criticalLimit;
			string normalLimit = (criticalLimit = "");
			xmlElement = (XmlElement)item.SelectSingleNode("PT/OBS/OBS.normal_lo-hi_limit");
			if (xmlElement != null)
			{
				normalLimit = xmlElement.GetAttribute("V");
			}
			xmlElement = (XmlElement)item.SelectSingleNode("PT/OBS/OBS.critical_lo-hi_limit");
			if (xmlElement != null)
			{
				criticalLimit = xmlElement.GetAttribute("V");
			}
			xmlElement = (XmlElement)item.SelectSingleNode("OPR/OPR.operator_id");
			string nurseCode = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("RGT/RGT.lot_number");
			string rgtLot = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			string diagcode = "";
			foreach (XmlNode item2 in item.SelectNodes("NTE"))
			{
				xmlElement = (XmlElement)item2.FirstChild;
				string attribute = xmlElement.GetAttribute("V");
				if (attribute == "ID FLAGS")
				{
					_ = xmlElement.InnerText;
					continue;
				}
				if (attribute == "TGC FLAG")
				{
					_ = xmlElement.InnerText;
				}
				if (attribute == "DIAGCODE")
				{
					diagcode = xmlElement.InnerText;
				}
			}
			list.Add(new SampleDataModel
			{
				Hospital = hospital,
				Depart = depart,
				Diagcode = diagcode,
				Unit = unit,
				PatientId = patientId,
				NurseCode = nurseCode,
				ObsTime = minValue,
				CriticalLimit = criticalLimit,
				NormalLimit = normalLimit,
				Interpretation = interpretation,
				Reuslt = ((text2 == "") ? (-1m) : decimal.Parse(text2)),
				RgtLot = rgtLot,
				ObsStatus = obsStatus,
				SerialNo = serial_id,
				DeviceId = device_id,
				ObsType = 1
			});
		}
		NovaSyncBus.AddSamples(list);
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private void ProcessObservation2(XmlElement documentElement)
	{
		XmlElement xmlElement = (XmlElement)documentElement.SelectSingleNode("HDR/HDR.control_id");
		if (xmlElement != null)
		{
			control_id = xmlElement.GetAttribute("V");
		}
		List<SampleDataModel> list = new List<SampleDataModel>();
		foreach (XmlNode item in documentElement.SelectNodes("SVC"))
		{
			_ = (XmlElement)item;
			string facility = m_facility;
			string location = m_location;
			DateTime minValue = DateTime.MinValue;
			xmlElement = (XmlElement)item.SelectSingleNode("SVC.role_cd");
			xmlElement.GetAttribute("V");
			xmlElement = (XmlElement)item.SelectSingleNode("SVC.observation_dttm");
			minValue = DMLDateTime(xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("CTC/CTC.level_cd");
			string text = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("CTC/OBS/OBS.value");
			string text2;
			string unit;
			if (xmlElement != null)
			{
				text2 = xmlElement.GetAttribute("V");
				unit = xmlElement.GetAttribute("U");
			}
			else
			{
				text2 = "";
				unit = "";
			}
			xmlElement = (XmlElement)item.SelectSingleNode("CTC/OBS/OBS.status_cd");
			string obsStatus = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			xmlElement = (XmlElement)item.SelectSingleNode("CTC/OBS/OBS.interpretation_cd");
			string interpretation = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			string normalLimit = "";
			xmlElement = (XmlElement)item.SelectSingleNode("CTC/OBS/OBS.normal_lo-hi_limit");
			if (xmlElement != null)
			{
				normalLimit = xmlElement.GetAttribute("V");
			}
			xmlElement = (XmlElement)item.SelectSingleNode("OPR/OPR.operator_id");
			string nurseCode = ((xmlElement == null) ? "" : xmlElement.GetAttribute("V"));
			string qcLot;
			string rgtLot = (qcLot = "");
			XmlNodeList xmlNodeList = item.SelectNodes("RGT");
			if (xmlNodeList != null && xmlNodeList.Count > 0)
			{
				foreach (XmlElement item2 in xmlNodeList)
				{
					if (((XmlElement)item2.SelectSingleNode("RGT.name")).InnerText == "TY=TS")
					{
						rgtLot = ((XmlElement)item2.SelectSingleNode("RGT.lot_number")).GetAttribute("V");
					}
					else
					{
						qcLot = ((XmlElement)item2.SelectSingleNode("RGT.lot_number")).GetAttribute("V");
					}
				}
			}
			list.Add(new SampleDataModel
			{
				Hospital = facility,
				Depart = location,
				Unit = unit,
				NurseCode = nurseCode,
				ObsTime = minValue,
				NormalLimit = normalLimit,
				Interpretation = interpretation,
				Reuslt = ((text2 == "") ? (-1m) : decimal.Parse(text2)),
				RgtLot = rgtLot,
				ObsStatus = obsStatus,
				SerialNo = serial_id,
				DeviceId = device_id,
				QcLevel = ((!(text == "")) ? int.Parse(text) : 0),
				ObsType = 2,
				QcLot = qcLot
			});
		}
		NovaSyncBus.AddSamples(list);
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private void ProcessEvents(XmlElement documentElement)
	{
		XmlElement xmlElement = (XmlElement)documentElement.SelectSingleNode("HDR/HDR.control_id");
		if (xmlElement != null)
		{
			control_id = xmlElement.GetAttribute("V");
		}
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private bool RequestFromDevice(string request_cd)
	{
		bool flag = true;
		try
		{
			string message = "<REQ.R01>" + GenDMLHeader() + "<REQ><REQ.request_cd V=\"" + request_cd + "\"/></REQ></REQ.R01>";
			SendMessage(message);
		}
		catch
		{
			flag = false;
		}
		return flag;
	}

	private bool SendAcknowledgeMessage(string control_id, bool isError)
	{
		bool flag = true;
		try
		{
			string message = "<ACK.R01>" + GenDMLHeader() + "<ACK><ACK.type_cd V=\"A" + (isError ? "E" : "A") + "\"/><ACK.ack_control_id V=\"" + control_id + "\"/></ACK></ACK.R01>";
			SendMessage(message);
		}
		catch
		{
			flag = false;
		}
		return flag;
	}

	private void SendContinuous()
	{
		string message = "<DTV.R01>" + GenDMLHeader() + "<DTV><DTV.command_cd V=\"START_CONTINUOUS\"/></DTV></DTV.R01>";
		SendMessage(message);
	}

	private void SendDateTime()
	{
		string message = "<DTV.R02>" + GenDMLHeader() + "<DTV><DTV.command_cd V=\"SET_TIME\"/></DTV><TM><TM.dttm V=\"" + DateTime2DML(DateTime.Now) + "\"/></TM></DTV.R02>";
		SendMessage(message);
	}

	private bool SendEotMessage(string topic, DateTime last_eot_time)
	{
		bool flag = true;
		try
		{
			string message = "<EOT.R01>" + GenDMLHeader() + "<EOT><EOT.topic_cd V=\"" + topic + "\"/><EOT.update_dttm V=\"" + DateTime2DML(last_eot_time) + "\"/></EOT></EOT.R01>";
			SendMessage(message);
		}
		catch
		{
			flag = false;
		}
		return flag;
	}

	private void ProcessKeepAlive(XmlNodeReader reader)
	{
		while (reader.Read())
		{
			reader.MoveToContent();
			string localName;
			if (reader.NodeType == XmlNodeType.Element && reader.IsStartElement() && (localName = reader.LocalName) != null && localName == "HDR.control_id")
			{
				control_id = reader.GetAttribute("V");
			}
		}
		SendAcknowledgeMessage(control_id, isError: false);
	}

	private void SendKeepAliveMessage()
	{
		string message = "<KPA.R01>" + GenDMLHeader() + "</KPA.R01>";
		SendMessage(message);
	}

	private bool SendTerminate(string reason, string note)
	{
		bool flag = true;
		try
		{
			string message = "<END.R01>" + GenDMLHeader() + "<TRM><TRM.reason_cd V=\"" + reason + "\"/>" + ((note.Length > 0) ? ("<TRM.note_txt V=\"" + note + "\"/>") : "") + "</TRM></END.R01>";
			SendMessage(message);
		}
		catch
		{
			flag = false;
		}
		return flag;
	}

	private int SendReagents()
	{
		XmlDocument xmlDocument = new XmlDocument();
		string xml = "<NOVA.REAG>" + GenDMLHeader() + "</NOVA.REAG>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		List<ReagentModel> reagents = NovaSyncBus.GetReagents(m_facility, m_location, pi);
		if (reagents != null && reagents.Count > 0)
		{
			int num = 0;
			foreach (ReagentModel item in reagents)
			{
				num++;
				XmlElement xmlElement = xmlDocument.CreateElement("LOT");
				XmlElement xmlElement2 = xmlDocument.CreateElement("LOT.lot_number");
				xmlElement2.SetAttribute("V", item.LotNum);
				xmlElement.AppendChild(xmlElement2);
				XmlElement xmlElement3 = xmlDocument.CreateElement("LOT.type");
				xmlElement3.SetAttribute("V", (item.LotType == 1) ? "TS" : "QC");
				xmlElement.AppendChild(xmlElement3);
				XmlElement xmlElement4 = xmlDocument.CreateElement("LOT.expiration_dttm");
				xmlElement4.SetAttribute("V", DateTime2DML(item.Expiration));
				xmlElement.AppendChild(xmlElement4);
				if (item.LotType == 2 && item.Low.HasValue && item.High.HasValue)
				{
					XmlElement xmlElement5 = xmlDocument.CreateElement("Level");
					XmlElement xmlElement6 = xmlDocument.CreateElement("Level.number");
					XmlElement xmlElement7 = xmlDocument.CreateElement("Level.type");
					xmlElement6.SetAttribute("V", item.LotNum.Last().ToString());
					xmlElement7.SetAttribute("V", "QC");
					XmlElement xmlElement8 = xmlDocument.CreateElement("TST");
					XmlElement xmlElement9 = xmlDocument.CreateElement("TST.observation_id");
					XmlElement xmlElement10 = xmlDocument.CreateElement("TST.lo-hi_limit");
					xmlElement9.SetAttribute("V", "Glu");
					xmlElement10.SetAttribute("V", $"[{item.Low.Value};{item.High.Value}]");
					xmlElement10.SetAttribute("U", "mmol/L");
					xmlElement8.AppendChild(xmlElement9);
					xmlElement8.AppendChild(xmlElement10);
					xmlElement5.AppendChild(xmlElement6);
					xmlElement5.AppendChild(xmlElement7);
					xmlElement5.AppendChild(xmlElement8);
					xmlElement.AppendChild(xmlElement5);
				}
				documentElement.AppendChild(xmlElement);
				if (num >= MAXCOUNT || xmlDocument.OuterXml.Length > int.Parse(max_message_sz) - 8192 - 32)
				{
					xml = xmlDocument.OuterXml;
					SendMessage(xml);
					Logger.Info(serial_id, "发送试剂信息", ClientIp, "");
					pi = reagents.Max((ReagentModel n) => n.Id);
					return 1;
				}
			}
			page_over = true;
			xml = xmlDocument.OuterXml;
			SendMessage(xml);
			pi = reagents.Max((ReagentModel n) => n.Id);
			Logger.Info(serial_id, "发送试剂信息", ClientIp, "");
			return 1;
		}
		xml = xmlDocument.OuterXml;
		SendMessage(xml);
		page_over = true;
		Logger.Info(serial_id, "发送试剂信息", ClientIp, "");
		return 1;
	}

	private bool SendLocationList()
	{
		DML2DateTime(loc_list_update_dttm).ToString("yyyy-MM-dd HH:mm:ss");
		XmlDocument xmlDocument = new XmlDocument();
		string xml = "<NOVA.LOC>" + GenDMLHeader() + "</NOVA.LOC>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		foreach (LocationModel location in NovaSyncBus.GetLocations())
		{
			XmlElement xmlElement = xmlDocument.CreateElement("LOC");
			XmlElement xmlElement2 = xmlDocument.CreateElement("LOC.facility");
			xmlElement2.SetAttribute("V", location.Name);
			if (location.Childs != null && location.Childs.Count > 0)
			{
				foreach (LocationModel child in location.Childs)
				{
					XmlElement xmlElement3 = xmlDocument.CreateElement("unit");
					xmlElement3.SetAttribute("V", child.Name);
					xmlElement3.SetAttribute("DF", "F");
					xmlElement2.AppendChild(xmlElement3);
				}
			}
			xmlElement.AppendChild(xmlElement2);
			documentElement.AppendChild(xmlElement);
		}
		SendMessage(xmlDocument.OuterXml);
		Logger.Info(serial_id, "发送医院科室信息", ClientIp, xmlDocument.OuterXml);
		return true;
	}

	private int SendPatientList()
	{
		XmlDocument xmlDocument = new XmlDocument();
		string xml = "<PTL.R01>" + GenDMLHeader() + "</PTL.R01>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		List<PatientModel> patients = NovaSyncBus.GetPatients(m_facility, m_location, pi, MAXCOUNT);
		if (patients != null && patients.Count > 0)
		{
			int num = 0;
			foreach (PatientModel item in patients)
			{
				num++;
				XmlElement xmlElement = xmlDocument.CreateElement("PT");
				string innerText = ((item.PatID == 1) ? "PATID" : ((item.PatID == 2) ? "MRN" : "ACCT"));
				string value = ((item.PatID == 1) ? item.PatientId : ((item.PatID == 2) ? item.MedicalRecord : item.Account));
				XmlElement xmlElement2 = xmlDocument.CreateElement("PT.patient_id");
				xmlElement2.SetAttribute("V", value);
				xmlElement2.InnerText = innerText;
				xmlElement.AppendChild(xmlElement2);
				XmlElement xmlElement3 = xmlDocument.CreateElement("PT.location");
				xmlElement3.SetAttribute("V", m_facility + "^" + m_location + "^" + item.WardNo + "^" + item.BedNo);
				xmlElement.AppendChild(xmlElement3);
				XmlElement xmlElement4 = xmlDocument.CreateElement("PT.name");
				XmlElement xmlElement5 = xmlDocument.CreateElement("FAM");
				xmlElement5.SetAttribute("V", item.Name);
				xmlElement4.SetAttribute("V", " " + item.Name);
				xmlElement4.AppendChild(xmlElement5);
				xmlElement.AppendChild(xmlElement4);
				XmlElement xmlElement6 = xmlDocument.CreateElement("PT.birth_date");
				xmlElement6.SetAttribute("V", item.Birthday.ToString("yyyy-MM-dd"));
				xmlElement.AppendChild(xmlElement6);
				XmlElement xmlElement7 = xmlDocument.CreateElement("PT.gender_cd");
				xmlElement7.SetAttribute("V", (item.Gender == 1) ? "M" : "F");
				xmlElement.AppendChild(xmlElement7);
				XmlElement xmlElement8 = xmlDocument.CreateElement("PT.room");
				xmlElement8.SetAttribute("V", item.WardNo);
				xmlElement.AppendChild(xmlElement8);
				XmlElement xmlElement9 = xmlDocument.CreateElement("PT.bed");
				xmlElement9.SetAttribute("V", item.BedNo);
				xmlElement.AppendChild(xmlElement9);
				documentElement.AppendChild(xmlElement);
				if (num >= MAXCOUNT || xmlDocument.OuterXml.Length > int.Parse(max_message_sz) - 8192 - 32)
				{
					xml = xmlDocument.OuterXml;
					SendMessage(xml);
					Logger.Info(serial_id, "发送患者信息", ClientIp, "");
					pi = patients.Max((PatientModel p) => p.Id);
					return 1;
				}
			}
			page_over = true;
			xml = xmlDocument.OuterXml;
			SendMessage(xml);
			Logger.Info(serial_id, "发送患者信息", ClientIp, "");
			pi = patients.Max((PatientModel p) => p.Id);
			return 1;
		}
		xml = xmlDocument.OuterXml;
		SendMessage(xml);
		page_over = true;
		Logger.Info(serial_id, "发送患者信息", ClientIp, "");
		return 1;
	}

	private bool SendSetup()
	{
		NovaSetupModel novaSetup = NovaSyncBus.GetNovaSetup(m_facility, m_location);
		XmlDocument xmlDocument = new XmlDocument();
		DML2DateTime(setup_update_dttm).ToString("yyyy-MM-dd HH:mm:ss");
		_ = DateTime.Now;
		string xml = "<NOVA.STATSTRIP.SETUP>" + GenDMLHeader() + "</NOVA.STATSTRIP.SETUP>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		StringBuilder stringBuilder = new StringBuilder();
		XmlWriterSettings xmlWriterSettings = new XmlWriterSettings();
		xmlWriterSettings.OmitXmlDeclaration = true;
		using (XmlWriter xmlWriter = XmlWriter.Create(stringBuilder, xmlWriterSettings))
		{
			NovaSetupKVModel novaSetupKVModel = new NovaSetupKVModel();
			novaSetupKVModel.FromNovaSetup(novaSetup);
			XmlSerializer xmlSerializer = new XmlSerializer(typeof(NovaSetupKVModel));
			XmlSerializerNamespaces xmlSerializerNamespaces = new XmlSerializerNamespaces();
			xmlSerializerNamespaces.Add(string.Empty, string.Empty);
			xmlSerializer.Serialize(xmlWriter, novaSetupKVModel, xmlSerializerNamespaces);
			xmlWriter.Close();
		}
		XmlDocument xmlDocument2 = new XmlDocument();
		xmlDocument2.LoadXml(stringBuilder.ToString());
		XmlNode newChild = xmlDocument.ImportNode(xmlDocument2.DocumentElement, deep: true);
		documentElement.AppendChild(newChild);
		XmlElement xmlElement = xmlDocument.CreateElement("TEST_CONFIG");
		documentElement.AppendChild(xmlElement);
		if (novaSetup.TestConfig != null)
		{
			TestRangeModel testConfig = novaSetup.TestConfig;
			string value = "";
			string value2 = "";
			if (testConfig.HighCricital.HasValue && testConfig.LowCricital.HasValue && testConfig.HighNormal.HasValue && testConfig.HighCricital.HasValue)
			{
				value = "[" + testConfig.LowCricital.Value.ToString("0.0#") + ";" + testConfig.HighCricital.Value.ToString("0.0#") + "]";
				value2 = "[" + testConfig.LowNormal.Value.ToString("0.0#") + ";" + testConfig.HighNormal.Value.ToString("0.0#") + "]";
			}
			XmlElement xmlElement2 = xmlDocument.CreateElement("TEST");
			xmlElement2.SetAttribute("TN", "Glu");
			xmlElement2.SetAttribute("RT", "M");
			xmlElement2.SetAttribute("U", "mmol/L");
			xmlElement2.SetAttribute("SL", testConfig.SL.HasValue ? testConfig.SL.Value.ToString() : "1.00");
			xmlElement2.SetAttribute("IC", testConfig.IC.HasValue ? testConfig.IC.Value.ToString() : "0");
			XmlElement xmlElement3 = xmlDocument.CreateElement("RANGE");
			xmlElement3.SetAttribute("CT", value);
			xmlElement3.SetAttribute("RF", value2);
			xmlElement3.SetAttribute("SEX", "U");
			xmlElement3.SetAttribute("AGE", "");
			xmlElement3.SetAttribute("LABEL", "");
			xmlElement3.SetAttribute("ABS", "[0.6;33.3]");
			xmlElement3.SetAttribute("EQ", "");
			xmlElement3.SetAttribute("EQ_CONST", "");
			xmlElement3.SetAttribute("CODE", "2341-6");
			xmlElement3.SetAttribute("CODE_SYS", "LN");
			xmlElement3.SetAttribute("AMR", "");
			xmlElement2.AppendChild(xmlElement3);
			xmlElement.AppendChild(xmlElement2);
		}
		if (!xmlElement.HasChildNodes)
		{
			xmlElement.InnerText = "none";
		}
		XmlElement xmlElement4 = xmlDocument.CreateElement("COMMENTS");
		documentElement.AppendChild(xmlElement4);
		xmlElement4.InnerText = "none";
		XmlElement xmlElement5 = xmlDocument.CreateElement("DIAGCODES");
		documentElement.AppendChild(xmlElement5);
		if (novaSetup.DiagCodes != null)
		{
			foreach (DiagcodeModel diagCode in novaSetup.DiagCodes)
			{
				XmlElement xmlElement6 = xmlDocument.CreateElement("DIAGCODE");
				XmlElement xmlElement7 = xmlDocument.CreateElement("DIAGCODE.code_id");
				XmlElement xmlElement8 = xmlDocument.CreateElement("DIAGCODE.code_desc");
				xmlElement7.SetAttribute("V", diagCode.Code);
				xmlElement8.SetAttribute("V", diagCode.Description);
				xmlElement6.AppendChild(xmlElement7);
				xmlElement6.AppendChild(xmlElement8);
				xmlElement5.AppendChild(xmlElement6);
			}
		}
		if (!xmlElement5.HasChildNodes)
		{
			xmlElement5.InnerText = "none";
		}
		xml = xmlDocument.OuterXml;
		SendMessage(xml);
		Logger.Info(serial_id, "发送Setup信息", ClientIp, xml);
		return true;
	}

	private int SendOperatorList()
	{
		XmlDocument xmlDocument = new XmlDocument();
		string xml = "<OPL.R01>" + GenDMLHeader() + "</OPL.R01>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		List<NurseModel> nurses = NovaSyncBus.GetNurses(m_facility, m_location, pi);
		if (nurses != null && nurses.Count > 0)
		{
			int num = 0;
			foreach (NurseModel item in nurses)
			{
				num++;
				XmlElement xmlElement = xmlDocument.CreateElement("OPR");
				XmlElement xmlElement2 = xmlDocument.CreateElement("OPR.operator_id");
				xmlElement2.SetAttribute("V", item.Code);
				xmlElement.AppendChild(xmlElement2);
				XmlElement xmlElement3 = xmlDocument.CreateElement("OPR.name");
				XmlElement xmlElement4 = xmlDocument.CreateElement("FAM");
				xmlElement4.SetAttribute("V", item.Name);
				xmlElement3.SetAttribute("V", " " + item.Name);
				xmlElement3.AppendChild(xmlElement4);
				xmlElement.AppendChild(xmlElement3);
				XmlElement xmlElement5 = xmlDocument.CreateElement("ACC");
				XmlElement xmlElement6 = xmlDocument.CreateElement("ACC.method_cd");
				XmlElement xmlElement7 = xmlDocument.CreateElement("ACC.permission_level_cd");
				xmlElement6.SetAttribute("V", item.Method);
				xmlElement7.SetAttribute("V", item.PermissionLevel);
				xmlElement5.AppendChild(xmlElement6);
				xmlElement5.AppendChild(xmlElement7);
				xmlElement.AppendChild(xmlElement5);
				documentElement.AppendChild(xmlElement);
				if (num >= MAXCOUNT || xmlDocument.OuterXml.Length > int.Parse(max_message_sz) - 8192 - 32)
				{
					xml = xmlDocument.OuterXml;
					SendMessage(xml);
					Logger.Info(serial_id, "发送护士信息", ClientIp, "");
					pi = item.Id.Value;
					return 1;
				}
			}
			page_over = true;
			xml = xmlDocument.OuterXml;
			SendMessage(xml);
			Logger.Info(serial_id, "发送护士信息", ClientIp, "");
			pi = nurses.Max((NurseModel n) => n.Id.Value);
			return 1;
		}
		xml = xmlDocument.OuterXml;
		SendMessage(xml);
		page_over = true;
		Logger.Info(serial_id, "发送护士信息", ClientIp, "");
		return 1;
	}

	private int SendOperator2List()
	{
		XmlDocument xmlDocument = new XmlDocument();
		string xml = "<OPL.R02>" + GenDMLHeader() + "</OPL.R02>";
		xmlDocument.LoadXml(xml);
		XmlElement documentElement = xmlDocument.DocumentElement;
		DateTime last = DML2DateTime(operators_update_dttm);
		List<NurseModel> nurses = NovaSyncBus.GetNurses(m_facility, m_location, last, 12);
		if (nurses != null && nurses.Count > 0)
		{
			XmlElement xmlElement = xmlDocument.CreateElement("UPD");
			XmlElement xmlElement2 = xmlDocument.CreateElement("UPD.action_cd");
			xmlElement2.SetAttribute("V", "D");
			xmlElement.AppendChild(xmlElement2);
			foreach (NurseModel item in nurses)
			{
				XmlElement xmlElement3 = xmlDocument.CreateElement("OPR");
				XmlElement xmlElement4 = xmlDocument.CreateElement("OPR.operator_id");
				xmlElement4.SetAttribute("V", item.Code);
				xmlElement3.AppendChild(xmlElement4);
				xmlElement.AppendChild(xmlElement3);
			}
			documentElement.AppendChild(xmlElement);
			XmlElement xmlElement5 = xmlDocument.CreateElement("UPD");
			XmlElement xmlElement6 = xmlDocument.CreateElement("UPD.action_cd");
			xmlElement6.SetAttribute("V", "I");
			xmlElement5.AppendChild(xmlElement6);
			foreach (NurseModel item2 in nurses.Where((NurseModel o) => !o.IsDeleted))
			{
				XmlElement xmlElement7 = xmlDocument.CreateElement("OPR");
				XmlElement xmlElement8 = xmlDocument.CreateElement("OPR.operator_id");
				xmlElement8.SetAttribute("V", item2.Code);
				xmlElement7.AppendChild(xmlElement8);
				XmlElement xmlElement9 = xmlDocument.CreateElement("OPR.name");
				XmlElement xmlElement10 = xmlDocument.CreateElement("FAM");
				xmlElement10.SetAttribute("V", item2.Name);
				xmlElement9.SetAttribute("V", " " + item2.Name);
				xmlElement9.AppendChild(xmlElement10);
				xmlElement7.AppendChild(xmlElement9);
				XmlElement xmlElement11 = xmlDocument.CreateElement("ACC");
				XmlElement xmlElement12 = xmlDocument.CreateElement("ACC.method_cd");
				XmlElement xmlElement13 = xmlDocument.CreateElement("ACC.permission_level_cd");
				xmlElement12.SetAttribute("V", item2.Method);
				xmlElement13.SetAttribute("V", item2.PermissionLevel);
				xmlElement11.AppendChild(xmlElement12);
				xmlElement11.AppendChild(xmlElement13);
				xmlElement7.AppendChild(xmlElement11);
				xmlElement5.AppendChild(xmlElement7);
				if (xmlDocument.OuterXml.Length > int.Parse(max_message_sz) - 8192 - 32)
				{
					documentElement.AppendChild(xmlElement5);
					xml = xmlDocument.OuterXml;
					SendMessage(xml);
					Logger.Info(serial_id, "发送护士信息", ClientIp, "");
					pi = nurses.Max((NurseModel n) => n.Id.Value);
					return 1;
				}
			}
			documentElement.AppendChild(xmlElement5);
			page_over = true;
			xml = xmlDocument.OuterXml;
			SendMessage(xml);
			Logger.Info(serial_id, "发送护士信息", ClientIp, "");
			pi = nurses.Max((NurseModel n) => n.Id.Value);
			return 1;
		}
		xml = xmlDocument.OuterXml;
		SendMessage(xml);
		page_over = true;
		Logger.Info(serial_id, "发送护士信息", ClientIp, "");
		return 1;
	}

	private string GenDMLHeader()
	{
		string text = "";
		try
		{
			text = "<HDR><HDR.control_id V=\"" + cmd_id + "\" /><HDR.version_id V=\"POCT1\" /><HDR.creation_dttm V=\"" + DateTime2DML(DateTime.Now) + "\" /></HDR>";
			cmd_id++;
		}
		catch
		{
		}
		return text;
	}

	private string DateTime2DML(DateTime dt)
	{
		DateTime dateTime = TimeZoneInfo.ConvertTime(dt, TimeZoneInfo.Local);
		DateTime dateTime2 = TimeZoneInfo.ConvertTime(dt, TimeZoneInfo.Utc);
		if (dateTime >= dateTime2)
		{
			TimeSpan timeSpan = dateTime - dateTime2;
			return dateTime.ToString("s") + "+" + ((timeSpan.Hours < 10) ? "0" : "") + timeSpan.Hours.ToString("d") + ":" + ((timeSpan.Minutes < 10) ? "0" : "") + timeSpan.Minutes.ToString("d");
		}
		TimeSpan timeSpan2 = dateTime2 - dateTime;
		return dateTime.ToString("s") + "-" + ((timeSpan2.Hours < 10) ? "0" : "") + timeSpan2.Hours.ToString("d") + ":" + ((timeSpan2.Minutes < 10) ? "0" : "") + timeSpan2.Minutes.ToString("d");
	}

	private DateTime DMLDateTime(string DMLtime)
	{
		DateTime minValue = DateTime.MinValue;
		if (DMLtime.Length >= 19)
		{
			return DateTime.Parse(DMLtime.Substring(0, 10) + " " + DMLtime.Substring(11, 8));
		}
		return minValue;
	}

	private DateTime DML2DateTime(string DMLtime)
	{
		DateTime minValue = DateTime.MinValue;
		if (DMLtime.Length >= 25)
		{
			int num = 1;
			int num2 = DMLtime.IndexOf('+');
			if (num2 < 0)
			{
				num2 = DMLtime.LastIndexOf('-');
				num = -1;
			}
			DateTime dateTime = DateTime.Parse(DMLtime.Substring(0, 10) + " " + DMLtime.Substring(11, num2 - 11));
			if (num2 >= 19)
			{
				int num3 = Convert.ToInt32(DMLtime.Substring(num2 + 1, 2));
				TimeSpan timeSpan = TimeSpan.FromMinutes(Convert.ToDouble((Convert.ToInt32(DMLtime.Substring(num2 + 4, 2)) + num3 * 60) * num));
				return TimeZoneInfo.ConvertTime(dateTime - timeSpan, TimeZoneInfo.Utc, TimeZoneInfo.Local);
			}
			return minValue;
		}
		return minValue;
	}

	private bool SendEscape(string note)
	{
		bool flag = true;
		try
		{
			string message = "<ESC.R01>" + GenDMLHeader() + "<ESC><ESC.esc_control_id V=\"" + control_id + "\"/><ESC.detail_cd V=\"OTH\"/>" + ((note.Length > 0) ? ("<ESC.note_txt V=\"" + note + "\"/>") : "") + "</ESC></ESC.R01>";
			SendMessage(message);
		}
		catch
		{
			flag = false;
		}
		return flag;
	}

	private void SendMessage(string message)
	{
		Logger.Debug(message);
		byte[] bytes = Encoding.UTF8.GetBytes(message);
		ClientSocket.Send(bytes);
	}

	private void CloseConnection()
	{
		try
		{
			SafeClose();
			ClientSocket.Dispose();
		}
		catch (Exception ex)
		{
			Logger.Error(ex, "Close ClientSocket => " + ex.Message);
		}
		try
		{
			Thread.CurrentThread.Abort();
			Logger.Info("线程终止完毕.");
		}
		catch (Exception ex2)
		{
			Logger.Error(ex2, "Abort Thread => " + ex2.Message);
		}
		RECEIVABLE = false;
	}

	private void SafeClose()
	{
		if (ClientSocket == null || !ClientSocket.Connected)
		{
			return;
		}
		try
		{
			ClientSocket.Shutdown(SocketShutdown.Both);
		}
		catch
		{
		}
		try
		{
			ClientSocket.Disconnect(reuseSocket: false);
		}
		catch
		{
		}
		try
		{
			ClientSocket.Close();
		}
		catch
		{
		}
	}
}
