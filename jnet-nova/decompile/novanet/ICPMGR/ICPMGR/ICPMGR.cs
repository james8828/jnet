#define TRACE
using System;
using System.Collections;
using System.ComponentModel;
using System.Data;
using System.Data.Odbc;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.ServiceProcess;
using System.Threading;
using FlexTimers;
using Microsoft.Win32;
using NNClass;

namespace ICPMGR;

public class ICPMGR : ServiceBase
{
	public const int MaxReadBuffSize = 32768;

	private Container components;

	public static NNBase m_NNBase = new NNBase();

	private FlexTimer m_OneMinTimer = new FlexTimer();

	private object m_objTimerLock = new object();

	public static bool m_bShuttingDown = false;

	public static Port m_Port = new Port();

	public static Port m_RCPort = new Port();

	public static ArrayList m_ListenerPorts = new ArrayList();

	public static DMLICPBase m_DMLICPBase = new DMLICPBase();

	private static DateTime m_prevPatListBuild_dttm = DateTime.MinValue;

	private static DateTime m_lastPatListBuild_dttm = DateTime.MinValue;

	private static object m_PatListLock = new object();

	private static object m_dmlSessionLock = new object();

	private bool m_dbReady;

	private OdbcConnection m_dbConnByTimer;

	private OdbcCommand m_dbCommByTimer;

	private bool bDBAvailable = true;

	public bool bApacheIsActive;

	public bool bIISIsActive;

	public bool bEventNotifyerIsLocal;

	public bool bLocalEventNotifierWorks;

	public string DBHostName;

	protected ServiceController myServiceController;

	public string version = "";

	public static string db_ver = "";

	public static bool SuppressClientLogs = false;

	private bool ClientLogsWereSuppressed;

	private bool ClientsAreLogging;

	private bool InstrumentListenerThreadWasStarted;

	private bool RCListenerPortExists;

	private bool RCListenerThreadWasStarted;

	private bool ListCleanerThreadWasStarted;

	private bool ListBuilderThreadWasStarted;

	private bool InstrumentListenerThreadIsRunning;

	private bool RCListenerThreadIsRunning;

	private bool ListCleanerThreadIsRunning;

	private bool ListBuilderThreadIsRunning;

	private bool bFirstTimeCallDBService = true;

	public static Buffers m_ICPBytesBuffers = null;

	public static object m_PrimePlusSetupLock = new object();

	public static bool m_b_loc_to_config_inst_type_column = false;

	public static bool m_b_test_offsets_inst_class_column = false;

	public static bool m_b_test_offsets_inst_type_column = false;

	public static bool m_b_instruments_tests_inst_class_column = false;

	public static bool m_b_loc_last_update_inst_class_column = false;

	public static bool m_b_loc_last_update_inst_type_column = false;

	public static bool m_b_bga_setup_to_location_busy_column = false;

	public static bool m_b_operators_pw_expire_date_column = false;

	public static DateTime PrevPatListBuildDttm
	{
		get
		{
			lock (m_PatListLock)
			{
				return m_prevPatListBuild_dttm;
			}
		}
		set
		{
			lock (m_PatListLock)
			{
				m_prevPatListBuild_dttm = value;
			}
		}
	}

	public static DateTime LastPatListBuildDttm
	{
		get
		{
			lock (m_PatListLock)
			{
				return m_lastPatListBuild_dttm;
			}
		}
		set
		{
			lock (m_PatListLock)
			{
				m_lastPatListBuild_dttm = value;
			}
		}
	}

	public ICPMGR()
	{
		InitializeComponent();
		m_ListenerPorts.Add(m_Port);
		m_ListenerPorts.Add(m_RCPort);
		Assembly asm = Assembly.GetExecutingAssembly();
		version = asm.GetName().Version.ToString();
		m_NNBase.NNBaseOpen(bLogging: true, "ICPMGR", "ICPMGR", "ICP");
		ClientsAreLogging = !SuppressClientLogs;
		string DBFile = "";
		m_NNBase.GetDBServer(m_NNBase.DATASOURCE, ref DBFile, ref DBHostName);
		myServiceController = new ServiceController();
		bApacheIsActive = m_NNBase.IsApacheActive();
		bIISIsActive = m_NNBase.IsIISActive();
		string UIDir = "";
		try
		{
			UIDir = Registry.LocalMachine.OpenSubKey(m_NNBase.REGISTRY_SUBKEY_RTM).GetValue("UIDir").ToString();
		}
		catch
		{
			UIDir = "C:\\NovaBiomedical\\NovaNet\\UI";
		}
		bEventNotifyerIsLocal = UIDir.Length > 0 && File.Exists(UIDir + "\\event_notifier.php");
		bLocalEventNotifierWorks = (bApacheIsActive || bIISIsActive) && bEventNotifyerIsLocal;
	}

	protected SafeHandle SelectService(string ServiceName, string HostName, ref ServiceController myServiceController)
	{
		SafeHandle myServiceHandle = null;
		try
		{
			myServiceController.ServiceName = ServiceName;
			myServiceController.MachineName = HostName;
			myServiceHandle = myServiceController.ServiceHandle;
		}
		catch
		{
		}
		return myServiceHandle;
	}

	private bool GetDBDataForService()
	{
		OdbcConnection myConnection = null;
		OdbcCommand myCommand = null;
		bool bRet = false;
		try
		{
			if (bDBAvailable = m_NNBase.OpenDBConnection(ref myConnection, ref myCommand, 7))
			{
				if (bFirstTimeCallDBService)
				{
					try
					{
						myCommand.CommandText = "SELECT Version from DBA.version_info where Object_Name = 'runtime_db'";
						OdbcDataReader theReader = myCommand.ExecuteReader();
						if (theReader.Read())
						{
							db_ver = theReader.GetString(0);
						}
						theReader.Close();
					}
					catch
					{
					}
					m_NNBase.log("ICPMGR version = " + version + "  Database version = " + db_ver, isXml: false, "ICPMGR");
					m_NNBase.CheckIfAuthorized("ICPMGR", ref myCommand, ShutDown);
					m_NNBase.CheckAssemblyVersion("ICPMGR", version, ref myCommand, ShutDown);
					m_NNBase.RegisterStart(myCommand);
					bDBAvailable = m_NNBase.bDBAvailable;
					if (bDBAvailable)
					{
						myCommand.CommandText = $"delete from DBA.communications where Computer_Name = '{m_NNBase.GetHostName()}' and (from_ui = 'F' or from_ui is null) and multi_connect = 0 and from_inst_id = 'StatStrip'";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myCommand.CommandText, isXml: false, "SQL");
						}
						myCommand.ExecuteNonQuery();
						myCommand.CommandText = "update DBA.instruments set total_patients = -1";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myCommand.CommandText, isXml: false, "SQL");
						}
						myCommand.ExecuteNonQuery();
						string CurrentMachineName = Environment.MachineName;
						myCommand.CommandText = "update DBA.instruments set last_disconnect_dttm = now(*) where ((last_disconnect_dttm < last_connect_dttm or last_disconnect_dttm is null) and computer_name ='" + CurrentMachineName + "')";
						if (m_NNBase.m_isLogging)
						{
							m_NNBase.log(myCommand.CommandText, isXml: false, "SQL");
						}
						myCommand.ExecuteNonQuery();
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'bga_setup_to_location') order by column_id";
						OdbcDataReader theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field = theReader.GetValue(0).ToString();
								if (string.Compare(field, "busy", ignoreCase: true) == 0)
								{
									m_b_bga_setup_to_location_busy_column = true;
								}
							}
						}
						theReader.Close();
						if (m_b_bga_setup_to_location_busy_column)
						{
							myCommand.CommandText = "UPDATE DBA.bga_setup_to_location SET busy='F' WHERE busy='T'";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myCommand.CommandText, isXml: false, "SQL");
							}
							myCommand.ExecuteNonQuery();
							myCommand.CommandText = $"update DBA.instruments set total_patients = -1";
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log(myCommand.CommandText, isXml: false, "SQL");
							}
							myCommand.ExecuteNonQuery();
						}
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'loc_to_config') order by column_id";
						theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field2 = theReader.GetValue(0).ToString();
								if (string.Compare(field2, "inst_type", ignoreCase: true) == 0)
								{
									m_b_loc_to_config_inst_type_column = true;
								}
							}
						}
						theReader.Close();
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'test_offsets') order by column_id";
						theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field3 = theReader.GetValue(0).ToString();
								if (string.Compare(field3, "inst_class", ignoreCase: true) == 0)
								{
									m_b_test_offsets_inst_class_column = true;
								}
								else if (string.Compare(field3, "inst_type", ignoreCase: true) == 0)
								{
									m_b_test_offsets_inst_type_column = true;
								}
							}
						}
						theReader.Close();
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'instruments_tests') order by column_id";
						theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field4 = theReader.GetValue(0).ToString();
								if (string.Compare(field4, "inst_class", ignoreCase: true) == 0)
								{
									m_b_instruments_tests_inst_class_column = true;
								}
							}
						}
						theReader.Close();
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'loc_last_update') order by column_id";
						theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field5 = theReader.GetValue(0).ToString();
								if (string.Compare(field5, "inst_class", ignoreCase: true) == 0)
								{
									m_b_loc_last_update_inst_class_column = true;
								}
								else if (string.Compare(field5, "inst_type", ignoreCase: true) == 0)
								{
									m_b_loc_last_update_inst_type_column = true;
								}
							}
						}
						theReader.Close();
						myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'operators') order by column_id";
						theReader = myCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field6 = theReader.GetValue(0).ToString();
								if (string.Compare(field6, "pw_expire_date", ignoreCase: true) == 0)
								{
									m_b_operators_pw_expire_date_column = true;
								}
							}
						}
						theReader.Close();
						bFirstTimeCallDBService = false;
					}
				}
				bRet = true;
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "initializing", "ICPMGR");
		}
		catch (Exception e2)
		{
			handleException(e2, "initializing", "ICPMGR");
		}
		finally
		{
			myCommand?.Dispose();
			myConnection?.Close();
		}
		return bRet;
	}

	private bool GetDBConnByTimer()
	{
		if (m_NNBase.bDBAvailable && m_dbConnByTimer != null && m_dbConnByTimer.State.Equals(ConnectionState.Open))
		{
			return true;
		}
		if (m_dbCommByTimer != null)
		{
			m_dbCommByTimer.Dispose();
		}
		if (m_dbConnByTimer != null)
		{
			m_dbConnByTimer.Close();
		}
		return bDBAvailable = m_NNBase.OpenDBConnection(ref m_dbConnByTimer, ref m_dbCommByTimer, 7);
	}

	private static void Main(string[] args)
	{
		try
		{
			ServiceBase[] ServicesToRun = new ServiceBase[1]
			{
				new ICPMGR()
			};
			ServiceBase.Run(ServicesToRun);
		}
		catch (Exception ex)
		{
			Trace.Write("Service1.Main", ex.Message);
		}
	}

	private void InitializeComponent()
	{
		base.ServiceName = "RTM ICPMGR";
	}

	protected override void OnStart(string[] args)
	{
		m_OneMinTimer.Interval = 1000u;
		m_OneMinTimer.theCallBack = OnTimedEvent;
		m_OneMinTimer.Start();
	}

	protected override void Dispose(bool disposing)
	{
		if (disposing)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing", isXml: false, "ICPMGR");
			}
			m_OneMinTimer.Stop();
			m_OneMinTimer.Close();
			m_OneMinTimer.Dispose();
			ShutDown(bExit: false);
			if (components != null)
			{
				components.Dispose();
			}
		}
		base.Dispose(disposing);
	}

	protected override void OnStop()
	{
		try
		{
			ShutDown(bExit: false);
		}
		catch
		{
			LibWrap.ExitProcess(0u);
		}
	}

	private void OnTimedEvent()
	{
		m_OneMinTimer.Interval = 60000u;
		lock (m_objTimerLock)
		{
			if (!m_dbReady || !bDBAvailable)
			{
				m_dbReady = GetDBDataForService();
			}
			if (m_dbReady)
			{
				UpdateHealthPing();
			}
		}
		GC.Collect();
	}

	private void UpdateHealthPing()
	{
		bool bStartLogging = false;
		bool bStopLogging = false;
		ClientLogsWereSuppressed = SuppressClientLogs;
		m_NNBase.BeginProcessControl();
		m_NNBase.GetProcessControlValue("ICP", "SuppressClientLogs", ref SuppressClientLogs);
		m_NNBase.EndProcessControl();
		Port.AsynchNetworkServer.ServerCommon.GetProcessControl();
		try
		{
			if (!GetDBConnByTimer())
			{
				return;
			}
			m_NNBase.GetDoLog(m_dbCommByTimer);
			bDBAvailable = m_NNBase.bDBAvailable;
			bStartLogging = m_NNBase.m_bLogging & !m_NNBase.m_isLogging;
			bStopLogging = !m_NNBase.m_bLogging & m_NNBase.m_isLogging;
			InitializePorts();
			if (m_NNBase.m_Status >= 5)
			{
				return;
			}
			if (bStartLogging)
			{
				if (!m_bShuttingDown)
				{
					m_NNBase.StartLogging();
					foreach (Port myPort in m_ListenerPorts)
					{
						if (myPort.m_CommType != null)
						{
							try
							{
								myPort.m_CommType.Notify(1, "");
							}
							catch
							{
							}
						}
					}
					if (!SuppressClientLogs)
					{
						ClientsAreLogging = true;
					}
				}
			}
			else if (bStopLogging)
			{
				if (!m_bShuttingDown)
				{
					foreach (Port myPort2 in m_ListenerPorts)
					{
						if (myPort2.m_CommType != null)
						{
							try
							{
								myPort2.m_CommType.Notify(2, "");
							}
							catch
							{
							}
						}
					}
				}
				ClientsAreLogging = false;
				m_NNBase.log("logging turned off", isXml: false, "ICPMGR");
				m_NNBase.StopLogging();
			}
			else if (m_NNBase.m_bLogging && ClientLogsWereSuppressed && !SuppressClientLogs)
			{
				foreach (Port myPort3 in m_ListenerPorts)
				{
					if (myPort3.m_CommType != null)
					{
						try
						{
							myPort3.m_CommType.Notify(4, "");
						}
						catch
						{
						}
					}
				}
				ClientsAreLogging = true;
			}
			else if (SuppressClientLogs && ClientsAreLogging)
			{
				foreach (Port myPort4 in m_ListenerPorts)
				{
					if (myPort4.m_CommType != null)
					{
						try
						{
							myPort4.m_CommType.Notify(5, "");
						}
						catch
						{
						}
					}
				}
				ClientsAreLogging = false;
			}
			InstrumentListenerThreadIsRunning = m_Port != null && m_Port.m_CommType != null && m_Port.m_CommType.IsAlive();
			if (InstrumentListenerThreadIsRunning)
			{
				InstrumentListenerThreadWasStarted = true;
			}
			if (m_RCPort != null && m_RCPort.m_CommType != null)
			{
				RCListenerPortExists = true;
				RCListenerThreadIsRunning = m_RCPort.m_CommType.IsAlive();
				if (RCListenerThreadIsRunning)
				{
					RCListenerThreadWasStarted = true;
				}
			}
			ListCleanerThreadIsRunning = Port.AsynchNetworkServer.ServerCommon.m_ListCleanerTimer.myTimerIsAlive();
			if (ListCleanerThreadIsRunning)
			{
				ListCleanerThreadWasStarted = true;
			}
			ListBuilderThreadIsRunning = m_DMLICPBase.m_ListCreatorProtocol.m_ProtocolThread.IsAlive;
			if (ListBuilderThreadIsRunning)
			{
				ListBuilderThreadWasStarted = true;
			}
			bool bAllWorkerThreadsAreRunning = InstrumentListenerThreadIsRunning && ((RCListenerPortExists && RCListenerThreadIsRunning) || !RCListenerPortExists) && ListCleanerThreadIsRunning && ListBuilderThreadIsRunning;
			m_NNBase.RegisterIfOtherThreadsAreActive(m_dbCommByTimer, 0, 0, bAllWorkerThreadsAreRunning);
			if (!m_bShuttingDown && ((InstrumentListenerThreadWasStarted && !InstrumentListenerThreadIsRunning) || (RCListenerPortExists && RCListenerThreadWasStarted && !RCListenerThreadIsRunning) || (ListCleanerThreadWasStarted && !ListCleanerThreadIsRunning) || (ListBuilderThreadWasStarted && !ListBuilderThreadIsRunning)))
			{
				if (!m_NNBase.m_isLogging)
				{
					m_NNBase.ForceLogging("ThreadDeath");
				}
				m_NNBase.log("Shutting down due to the death of one or more worker threads", isXml: false, "ICPMGR");
				ShutDown(bExit: true);
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "performing timed actions", "OnTimedEvent");
		}
		catch (Exception e2)
		{
			handleException(e2, "performing timed actions", "OnTimedEvent");
		}
		finally
		{
			if (!bDBAvailable)
			{
				if (m_dbCommByTimer != null)
				{
					m_dbCommByTimer.Dispose();
				}
				if (m_dbConnByTimer != null)
				{
					m_dbConnByTimer.Close();
				}
				m_dbReady = false;
			}
		}
	}

	private void InitializePorts()
	{
		if (!m_bShuttingDown)
		{
			m_NNBase.m_Status = 0;
			m_DMLICPBase.DMLICPBaseInit(m_NNBase.m_isLogging, ref m_NNBase);
			if (m_NNBase.m_Status >= 5)
			{
				m_NNBase.log("Error loading location or location message list", isXml: false, "InitializePorts");
				return;
			}
			Port.AsynchNetworkServer.ServerCommon.StartListCleaner();
			try
			{
				if (m_Port != null)
				{
					m_Port.m_isInvalid = true;
				}
				if (m_RCPort != null)
				{
					m_RCPort.m_isInvalid = true;
				}
				if (GetDBConnByTimer())
				{
					m_dbCommByTimer.CommandText = "SELECT Instrument_ID, Protocol, Port_type, Comm_Protocol, Port_Num, Baud, Data_Bits, Stop_Bits, Parity, Flow_Control, Run_Mode, Connect_Remote, used, Multi_connect, IP_address, Rcv_Application, Rcv_Facility, Port_Active, Remote_Host_Name, remote_port, now(*) FROM DBA.Communications where from_ui = 'T' and computer_name = '" + m_NNBase.GetHostName() + "' and (Port_type = 'StatStrip' OR Port_type = 'ICPMgrAPI') and connect_remote = 0 and multi_connect = '1'";
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log(m_dbCommByTimer.CommandText, isXml: false, "SQL");
					}
					OdbcDataReader theReader = m_dbCommByTimer.ExecuteReader();
					PortParams par = new PortParams
					{
						ipAddress = new byte[4],
						parent = this
					};
					while (theReader.Read())
					{
						if (!theReader.IsDBNull(0))
						{
							par.instrumentId = theReader.GetString(0);
						}
						if (!theReader.IsDBNull(1))
						{
							par.protocol = theReader.GetString(1);
						}
						if (!theReader.IsDBNull(2))
						{
							par.portType = theReader.GetString(2);
						}
						if (!theReader.IsDBNull(3))
						{
							par.commProtocol = theReader.GetString(3);
						}
						if (!theReader.IsDBNull(4))
						{
							par.portNum = theReader.GetInt32(4);
						}
						if (!theReader.IsDBNull(5))
						{
							par.baud = theReader.GetString(5);
						}
						if (!theReader.IsDBNull(6))
						{
							par.dataBits = theReader.GetString(6);
						}
						if (!theReader.IsDBNull(7))
						{
							par.stopBits = theReader.GetString(7);
						}
						if (!theReader.IsDBNull(8))
						{
							par.parity = theReader.GetString(8);
						}
						if (!theReader.IsDBNull(9))
						{
							par.flowControl = theReader.GetInt32(9);
						}
						if (!theReader.IsDBNull(10))
						{
							par.runMode = theReader.GetInt32(10);
						}
						if (!theReader.IsDBNull(11))
						{
							par.connectRemote = theReader.GetInt32(11);
						}
						if (!theReader.IsDBNull(12))
						{
							par.used = theReader.GetString(12);
						}
						else
						{
							par.used = "T";
						}
						if (!theReader.IsDBNull(13))
						{
							par.multiConnect = theReader.GetString(13);
						}
						if (!theReader.IsDBNull(14))
						{
							uint i = (uint)theReader.GetInt32(14);
							byte[] b = new byte[4];
							par.ipAddress[0] = (b[3] = (byte)((i >> 24) & 0xFF));
							par.ipAddress[1] = (b[2] = (byte)((i >> 16) & 0xFF));
							par.ipAddress[2] = (b[1] = (byte)((i >> 8) & 0xFF));
							par.ipAddress[3] = (b[0] = (byte)(i & 0xFF));
						}
						if (!theReader.IsDBNull(15))
						{
							par.rcvApplication = theReader.GetString(15);
						}
						if (!theReader.IsDBNull(16))
						{
							par.rcvFacility = theReader.GetString(16);
						}
						if (!theReader.IsDBNull(17))
						{
							par.portActive = theReader.GetInt32(17);
						}
						if (!theReader.IsDBNull(18))
						{
							par.remoteHostName = theReader.GetString(18);
						}
						if (!theReader.IsDBNull(19))
						{
							par.remotePort = theReader.GetInt32(19);
						}
						par.do_logging = m_NNBase.m_bLogging;
						if (!theReader.IsDBNull(20))
						{
							DateTime dbNow = theReader.GetDateTime(20);
							if ((DateTime.Now - dbNow).TotalSeconds > 60.0)
							{
								m_NNBase.ReportErrorDB("System times of Database host and ICPMGR host exceed one minue", "E", "initializing ports", "InitializePorts", "");
								m_NNBase.m_Status = 0;
							}
						}
						if (par.used == "T")
						{
							switch (par.protocol)
							{
							case "DML":
								if (m_Port == null)
								{
									m_Port = new Port();
									m_Port.m_isInvalid = true;
								}
								InitializePort(par);
								break;
							case "Command":
								if (m_RCPort == null)
								{
									m_RCPort = new Port();
									m_RCPort.m_isInvalid = true;
								}
								InitializePort_RC(par);
								break;
							}
						}
						Thread.Sleep(100);
					}
					theReader.Close();
				}
			}
			catch (OdbcException e)
			{
				handleDBException(e, "initializing ports", "InitializePorts");
			}
			catch (Exception e2)
			{
				handleException(e2, "initializing ports", "InitializePorts");
			}
		}
		if (!m_bShuttingDown)
		{
			if (m_Port != null && m_Port.m_isInvalid)
			{
				if (m_Port.m_CommType != null)
				{
					try
					{
						m_Port.m_CommType.Notify(-1, " because InitializePort failed to validate the listener port");
					}
					catch
					{
					}
				}
				m_Port.m_CommType = null;
				m_Port.m_isRunning = false;
			}
			if (m_RCPort != null && m_RCPort.m_isInvalid)
			{
				if (m_RCPort.m_CommType != null)
				{
					try
					{
						m_RCPort.m_CommType.Notify(-1, " because InitializePort failed to validate the listener port");
					}
					catch
					{
					}
				}
				m_RCPort.m_CommType = null;
				m_RCPort.m_isRunning = false;
			}
		}
		if (m_bShuttingDown)
		{
			return;
		}
		if (m_Port != null && !m_Port.m_isInvalid && !m_Port.m_isRunning)
		{
			try
			{
				m_Port.Run();
			}
			catch (Exception e3)
			{
				handleException(e3, "starting port", "InitializePorts");
			}
		}
		if (m_RCPort != null && !m_RCPort.m_isInvalid && !m_RCPort.m_isRunning)
		{
			try
			{
				m_RCPort.Run();
			}
			catch (Exception e4)
			{
				handleException(e4, "starting port", "InitializePorts -- RCPort");
			}
		}
	}

	private bool InitializePort(PortParams par)
	{
		try
		{
			if (par.used == "T")
			{
				if (!m_bShuttingDown)
				{
					try
					{
						if (m_Port.m_CommType != null)
						{
							if (PortHasChanged(m_Port.m_par, par))
							{
								m_Port.m_CommType.Notify(-1, " because the listener port has changed");
								m_Port.m_isInvalid = true;
								m_Port.m_isRunning = false;
							}
							else if (PortHasBeenDeactivated(m_Port.m_par, par))
							{
								m_Port.m_par.portActive = 0;
								m_Port.m_CommType.Notify(6, " because the listener port has been deactivated");
								m_Port.m_isInvalid = false;
							}
							else if (PortHasBeenActivated(m_Port.m_par, par))
							{
								m_Port.m_par.portActive = 1;
								m_Port.m_CommType.Notify(7, "");
								m_Port.m_isInvalid = false;
							}
							else
							{
								m_Port.m_isInvalid = false;
							}
						}
						else
						{
							m_Port.m_isInvalid = true;
						}
					}
					catch (Exception ex)
					{
						m_NNBase.log("Exception " + ex.Message, isXml: false, "ICPMGR");
						handleException(ex, "initializing port", "InitializePort");
					}
				}
				if (!m_bShuttingDown)
				{
					try
					{
						if (m_Port.m_isInvalid && par.portActive == 1)
						{
							if (!m_Port.InitPort(par, out var error))
							{
								m_NNBase.log("failed to InitPort -- " + error, isXml: false, "ICPMGR");
								return false;
							}
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType, isXml: false, "ICPMGR");
							}
							if (m_NNBase.m_isLogging)
							{
								string ThreadCount = "Thread Count after adding listener port:" + Process.GetCurrentProcess().Threads.Count;
								m_NNBase.log(ThreadCount, isXml: false, "ICPMGR");
							}
						}
					}
					catch (Exception e)
					{
						handleException(e, "initializing port", "InitializePort");
						return false;
					}
				}
			}
		}
		catch (Exception e2)
		{
			handleException(e2, "initializing port", "InitializePort");
			return false;
		}
		return true;
	}

	private bool InitializePort_RC(PortParams par)
	{
		try
		{
			if (!m_bShuttingDown)
			{
				try
				{
					if (m_RCPort.m_CommType != null)
					{
						if (PortHasChanged(m_RCPort.m_par, par))
						{
							m_RCPort.m_CommType.Notify(-1, " because the listener port has changed");
							m_RCPort.m_isInvalid = true;
							m_RCPort.m_isRunning = false;
						}
						else if (PortHasBeenDeactivated(m_RCPort.m_par, par))
						{
							m_RCPort.m_par.portActive = 0;
							m_RCPort.m_CommType.Notify(6, " because the listener port has been deactivated");
							m_RCPort.m_isInvalid = false;
						}
						else if (PortHasBeenActivated(m_RCPort.m_par, par))
						{
							m_RCPort.m_par.portActive = 1;
							m_RCPort.m_CommType.Notify(7, "");
							m_RCPort.m_isInvalid = false;
						}
						else
						{
							m_RCPort.m_isInvalid = false;
						}
					}
					else
					{
						m_RCPort.m_isInvalid = true;
					}
				}
				catch (Exception e)
				{
					handleException(e, "initializing port", "InitializePort_RC");
				}
			}
			if (!m_bShuttingDown)
			{
				try
				{
					if (m_RCPort.m_isInvalid && par.portActive == 1)
					{
						if (m_RCPort.InitPort(par, out var error))
						{
							if (m_NNBase.m_isLogging)
							{
								m_NNBase.log("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType, isXml: false, "ICPMGR");
							}
							if (m_NNBase.m_isLogging)
							{
								string ThreadCount = "Thread Count after adding listener port:" + Process.GetCurrentProcess().Threads.Count;
								m_NNBase.log(ThreadCount, isXml: false, "ICPMGR");
							}
						}
						else
						{
							m_NNBase.log("failed to InitPort" + error, isXml: false, "ICPMGR");
						}
					}
				}
				catch (Exception e2)
				{
					handleException(e2, "initializing port", "InitializePort_RC");
				}
			}
		}
		catch (Exception e3)
		{
			handleException(e3, "initializing port", "InitializePort_RC");
		}
		return true;
	}

	private bool PortHasChanged(PortParams oldpar, PortParams newpar)
	{
		if (oldpar.instrumentId.CompareTo(newpar.instrumentId) == 0)
		{
			return (oldpar.protocol != newpar.protocol) | (oldpar.portType != newpar.portType) | (oldpar.portNum != newpar.portNum);
		}
		if (newpar.protocol.Length > 0)
		{
			return true;
		}
		return false;
	}

	private bool PortHasBeenDeactivated(PortParams oldpar, PortParams newpar)
	{
		if (oldpar.instrumentId.CompareTo(newpar.instrumentId) == 0)
		{
			return (oldpar.portActive == 1) & (newpar.portActive == 0);
		}
		if (newpar.protocol.Length > 0)
		{
			return true;
		}
		return false;
	}

	private bool PortHasBeenActivated(PortParams oldpar, PortParams newpar)
	{
		if (oldpar.instrumentId.CompareTo(newpar.instrumentId) == 0)
		{
			return (oldpar.portActive == 0) & (newpar.portActive == 1);
		}
		if (newpar.protocol.Length > 0)
		{
			return true;
		}
		return false;
	}

	public void ShutDown(string reason, string whofrom, bool bExit)
	{
		m_NNBase.log(reason, isXml: false, whofrom);
		ShutDown(bExit: false);
	}

	private void ShutDown(bool bExit)
	{
		try
		{
			m_bShuttingDown = true;
			m_OneMinTimer.Stop();
			m_OneMinTimer.Close();
			m_OneMinTimer.Dispose();
			CloseListenerPorts();
			if (m_DMLICPBase != null)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Stopping ListBuilder", isXml: false, "ShutDown");
				}
				m_DMLICPBase.DMLICPBaseClose();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Stopped ListBuilder", isXml: false, "ShutDown");
				}
			}
			m_NNBase.log("Stopping ListCleanerTimer", isXml: false, "ShutDown");
			Port.AsynchNetworkServer.ServerCommon.StopListCleanerTimer();
			m_NNBase.log("Stopped ListCleanerTimer", isXml: false, "ShutDown");
			m_NNBase.log("Closing and disposing ListCleanerTimer", isXml: false, "ShutDown");
			Port.AsynchNetworkServer.ServerCommon.ShutDownListCleanerTimer();
			m_NNBase.log("Closed and disposed ListCleanerTimer", isXml: false, "ShutDown");
			Port.AsynchNetworkServer.ServerCommon.m_ListCleanerNNBase.NNBaseClose();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Shutdown", isXml: false, "ICPMGR");
				m_NNBase.StopLogging();
			}
		}
		catch
		{
		}
		finally
		{
			if (bExit)
			{
				LibWrap.ExitProcess(1066u);
			}
		}
	}

	private void CloseListenerPorts()
	{
		Port.AsynchNetworkServer.ServerCommon.m_ShuttingPorts = true;
		try
		{
			if (m_Port != null && m_Port.m_CommType != null && m_Port.m_par.connectRemote == 0)
			{
				string sport = m_Port.m_par.portNum.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing DML listener port " + sport, isXml: false, "ICPMGR");
				}
				if (m_Port.m_CommType != null)
				{
					m_Port.m_CommType.Notify(-1, " because the service is shutting down");
				}
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("DML listener port " + sport + " closed", isXml: false, "ICPMGR");
				}
				m_Port.m_CommType = null;
				m_Port.m_isRunning = false;
			}
			if (m_RCPort != null && m_RCPort.m_CommType != null && m_RCPort.m_par.connectRemote == 0)
			{
				string sport2 = m_RCPort.m_par.portNum.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing RC listener port " + sport2, isXml: false, "ICPMGR");
				}
				if (m_RCPort.m_CommType != null)
				{
					m_RCPort.m_CommType.Notify(-1, " because the service is shutting down");
				}
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("RC listener port " + sport2 + " closed", isXml: false, "ICPMGR");
				}
				m_RCPort.m_CommType = null;
				m_RCPort.m_isRunning = false;
			}
		}
		catch (Exception ex)
		{
			m_NNBase.log("Exception (CloseListenerPorts) --" + ex.Message, isXml: false, "ICPMGR");
		}
	}

	private void handleException(Exception e, string when, string from)
	{
		if (!m_bShuttingDown)
		{
			bool bDBWasAvailable = bDBAvailable;
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
			if (!bDBDisconnect)
			{
				ShutDown(bExit: true);
			}
			else if (bDBWasAvailable)
			{
				m_OneMinTimer.Stop();
				m_Port.m_par.portActive = 0;
				m_Port.m_CommType.Notify(6, " because a service database connection has been lost");
				m_RCPort.m_par.portActive = 0;
				m_RCPort.m_CommType.Notify(6, " because a service database connection has been lost");
				m_OneMinTimer.Start();
			}
		}
	}

	private void handleDBException(OdbcException e, string when, string from)
	{
		if (m_bShuttingDown)
		{
			return;
		}
		bool bDBWasAvailable = bDBAvailable;
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
		if (!bDBDisconnect)
		{
			ShutDown(bExit: true);
		}
		else if (bDBWasAvailable)
		{
			m_OneMinTimer.Stop();
			m_Port.m_par.portActive = 0;
			m_Port.m_CommType.Notify(6, " because a service database connection has been lost");
			m_RCPort.m_par.portActive = 0;
			m_RCPort.m_CommType.Notify(6, " because a service database connection has been lost");
			m_OneMinTimer.Start();
		}
	}
}
