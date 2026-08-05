#define TRACE
using System;
using System.Collections;
using System.ComponentModel;
using System.Data;
using System.Data.Odbc;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.ServiceProcess;
using System.Timers;
using Microsoft.Win32;
using NNClass;

namespace RTMLIS;

public class RTMLIS : ServiceBase
{
	private Container components;

	public NNBase m_NNBase = new NNBase();

	private Timer m_OneMinTimer;

	private object m_objTimerLock = new object();

	public bool m_bShuttingDown;

	private bool m_bOnTimedEvent;

	private ArrayList m_portsList = new ArrayList();

	public int m_iNumMessages;

	public int m_iTotMessages;

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

	public string db_ver = "";

	private CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	private CompareOptions CompOpt = CompareOptions.IgnoreCase;

	public bool m_bSamplesDeviceNameColumn;

	public bool m_bInstrumentsTestsLisTestAliasColumn;

	public bool m_bPatientVisitsTable;

	public Port m_Port;

	public bool bWorkerThreadsWereStarted;

	public RTMLIS()
	{
		InitializeComponent();
		Assembly asm = Assembly.GetExecutingAssembly();
		version = asm.GetName().Version.ToString();
		m_NNBase.NNBaseOpen(bLogging: true, "RTMLIS", "RTMLIS", "LIS");
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
				string sql = "";
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
				m_NNBase.log("RTMLIS version = " + version + "  Database version = " + db_ver, isXml: false, "RTMLIS");
				m_NNBase.CheckIfAuthorized("RTMLIS", ref myCommand, ShutDown);
				m_NNBase.CheckAssemblyVersion("RTMLIS", version, ref myCommand, ShutDown);
				m_NNBase.RegisterStart(myCommand);
				bDBAvailable = m_NNBase.bDBAvailable;
				if (bDBAvailable)
				{
					sql = "select tot_messages_processed from dba.health_ping where process_name = 'RTMLIS' and host = '" + m_NNBase.GetLocalPOP() + "'";
					myCommand.CommandText = sql;
					OdbcDataReader theReader = myCommand.ExecuteReader();
					if (theReader.Read() && !theReader.IsDBNull(0))
					{
						m_iTotMessages = theReader.GetInt32(0);
					}
					theReader.Close();
					sql = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'samples') order by column_id";
					myCommand.CommandText = sql;
					theReader = myCommand.ExecuteReader();
					while (theReader.Read())
					{
						if (!theReader.IsDBNull(0))
						{
							string field = theReader.GetValue(0).ToString();
							if (Comp.Compare(field, "device_name", CompOpt) == 0)
							{
								m_bSamplesDeviceNameColumn = true;
							}
						}
					}
					theReader.Close();
					sql = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'instruments_tests') order by column_id";
					myCommand.CommandText = sql;
					theReader = myCommand.ExecuteReader();
					while (theReader.Read())
					{
						if (!theReader.IsDBNull(0))
						{
							string field2 = theReader.GetValue(0).ToString();
							if (Comp.Compare(field2, "lis_test_alias", CompOpt) == 0)
							{
								m_bInstrumentsTestsLisTestAliasColumn = true;
							}
						}
					}
					theReader.Close();
					sql = "select table_name from sys.systable order by table_name";
					myCommand.CommandText = sql;
					theReader = myCommand.ExecuteReader();
					while (theReader.Read())
					{
						if (!theReader.IsDBNull(0))
						{
							string table = theReader.GetValue(0).ToString();
							if (Comp.Compare(table, "patient_visits", CompOpt) == 0)
							{
								m_bPatientVisitsTable = true;
							}
						}
					}
					theReader.Close();
					bRet = true;
				}
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "initializing", "RTMLIS");
		}
		catch (Exception e2)
		{
			handleException(e2, "initializing", "RTMLIS");
		}
		finally
		{
			myCommand?.Dispose();
			myConnection?.Close();
			if (!bDBAvailable)
			{
				m_dbReady = false;
			}
		}
		return bRet;
	}

	private bool GetDBConnByTimer()
	{
		if (m_dbConnByTimer != null && m_dbConnByTimer.State.Equals(ConnectionState.Open))
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

	private static void Main()
	{
		try
		{
			ServiceBase[] ServicesToRun = new ServiceBase[1]
			{
				new RTMLIS()
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
		m_OneMinTimer = new Timer();
		((ISupportInitialize)m_OneMinTimer).BeginInit();
		m_OneMinTimer.Enabled = true;
		base.ServiceName = "RTMLIS";
		((ISupportInitialize)m_OneMinTimer).EndInit();
	}

	protected override void OnStart(string[] args)
	{
		m_OneMinTimer.Interval = 1000.0;
		m_OneMinTimer.AutoReset = true;
		m_OneMinTimer.Elapsed += OnTimedEvent;
	}

	protected override void Dispose(bool disposing)
	{
		if (disposing)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Disposing", isXml: false, "RTMLIS");
			}
			m_OneMinTimer.Close();
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
		ShutDown(bExit: false);
	}

	private void OnTimedEvent(object source, ElapsedEventArgs e)
	{
		if (m_bOnTimedEvent)
		{
			return;
		}
		m_bOnTimedEvent = true;
		m_OneMinTimer.Stop();
		m_OneMinTimer.Interval = 60000.0;
		lock (m_objTimerLock)
		{
			if (!m_dbReady)
			{
				m_dbReady = GetDBDataForService();
			}
			if (m_dbReady)
			{
				UpdateHealthPing();
			}
		}
		m_bOnTimedEvent = false;
		m_OneMinTimer.Start();
	}

	private void UpdateHealthPing()
	{
		try
		{
			if (GetDBConnByTimer())
			{
				m_NNBase.GetDoLog(m_dbCommByTimer);
				bDBAvailable = m_NNBase.bDBAvailable;
				bool bStartLogging = m_NNBase.m_bLogging & !m_NNBase.m_isLogging;
				bool bStopLogging = !m_NNBase.m_bLogging & m_NNBase.m_isLogging;
				InitializePorts();
				if (bStartLogging)
				{
					m_NNBase.StartLogging();
					foreach (Port p in m_portsList)
					{
						p.m_CommType.Notify(1);
					}
				}
				else if (bStopLogging)
				{
					foreach (Port p2 in m_portsList)
					{
						p2.m_CommType.Notify(2);
					}
					m_NNBase.log("logging turned off", isXml: false, "RTMLIS");
					m_NNBase.StopLogging();
				}
			}
			bool bWorkerThreadsAreRunning = !m_bShuttingDown && ((m_Port != null && m_Port.m_CommType != null && m_Port.m_CommType.IsConnected() && m_Port.m_CommType.IsAlive()) || !SomethingToSend());
			if (bWorkerThreadsAreRunning)
			{
				bWorkerThreadsWereStarted = true;
			}
			m_NNBase.RegisterIfOtherThreadsAreActive(m_dbCommByTimer, m_iNumMessages, m_iTotMessages, bWorkerThreadsAreRunning);
			if (bWorkerThreadsWereStarted && !m_bShuttingDown && (m_Port == null || m_Port.m_CommType == null || !m_Port.m_CommType.IsAlive()) && SomethingToSend())
			{
				if (!m_NNBase.m_isLogging)
				{
					m_NNBase.ForceLogging("ThreadDeath");
				}
				m_NNBase.log("Shutting down due to the death of worker thread", isXml: false, "RTMLIS");
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
		try
		{
			foreach (Port p in m_portsList)
			{
				if (!p.IsSpawned)
				{
					p.IsInvalid = true;
				}
			}
			if (GetDBConnByTimer())
			{
				m_dbCommByTimer.CommandText = "SELECT Instrument_ID, Protocol, Port_type, Comm_Protocol, Port_Num, Baud, Data_Bits, Stop_Bits, Parity, Flow_Control, Run_Mode, Connect_Remote, used, Multi_connect, IP_address, Rcv_Application, Rcv_Facility, Port_Active, Remote_Host_Name, remote_port, now(*) FROM DBA.Communications where from_ui = 'T' and Port_type = 'LIS' and computer_name = '" + m_NNBase.GetHostName() + "'";
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
				if (theReader.Read())
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
							m_NNBase.ReportErrorDB("System times of Database host and RTMLIS host exceed one minue", "E", "initializing ports", "InitializePorts", "");
							m_NNBase.m_Status = 0;
						}
					}
				}
				theReader.Close();
				InitializePort(par);
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
		bool removed = false;
		do
		{
			removed = false;
			foreach (Port p2 in m_portsList)
			{
				if (p2.IsInvalid)
				{
					p2.m_CommType.Notify(-1);
					m_portsList.Remove(p2);
					removed = true;
					break;
				}
				if (!p2.m_isRunning)
				{
					p2.Run();
				}
			}
		}
		while (removed);
	}

	private void InitializePort(PortParams par)
	{
		try
		{
			if (!(par.used == "T"))
			{
				return;
			}
			bool found = false;
			foreach (Port pold in m_portsList)
			{
				if ((pold.m_par.portNum <= 0 || pold.m_par.portNum != par.portNum) && (pold.m_par.remotePort <= 0 || pold.m_par.remotePort != par.remotePort))
				{
					continue;
				}
				found = true;
				if (!pold.m_CommType.Notify(0))
				{
					if (PortHasChanged(pold.m_par, par))
					{
						pold.m_CommType.Notify(-1);
						found = false;
					}
					else
					{
						pold.IsInvalid = false;
					}
				}
				else
				{
					pold.m_CommType.Notify(-1);
					pold.IsInvalid = true;
					found = false;
				}
			}
			if (!found && par.portActive == 1 && SomethingToSend())
			{
				Port pnew = new Port(par);
				m_portsList.Add(pnew);
				m_Port = pnew;
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType, isXml: false, "RTMLIS");
				}
				Console.WriteLine("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType);
			}
		}
		catch (Exception e)
		{
			if (!m_bShuttingDown)
			{
				handleException(e, "initializing port", "InitializePort");
			}
		}
	}

	private bool SomethingToSend()
	{
		bool bAddPort = false;
		m_dbCommByTimer.CommandText = "select count(*) from DBA.ORDERS where transmitted_flag = 'F'";
		int queryOrderToSend = (int)m_dbCommByTimer.ExecuteScalar();
		if (queryOrderToSend > 0)
		{
			bAddPort = true;
		}
		else
		{
			m_dbCommByTimer.CommandText = "select count(*) from DBA.samples where transmitted_flag = 'F' and xml_text like '%<SVC>%</SVC>%' and control_type is not null and control_type != ''";
			int ResultsToSend = (int)m_dbCommByTimer.ExecuteScalar();
			if (ResultsToSend > 0)
			{
				bAddPort = true;
			}
		}
		return bAddPort;
	}

	private bool PortHasChanged(PortParams oldpar, PortParams newpar)
	{
		return (oldpar.protocol != newpar.protocol) | (oldpar.portType != newpar.portType) | (oldpar.commProtocol != newpar.commProtocol) | (oldpar.portActive != newpar.portActive) | (oldpar.remoteHostName != newpar.remoteHostName);
	}

	public void ShutDown(string reason, string whofrom, bool bExit)
	{
		m_NNBase.log(reason, isXml: false, whofrom);
		ShutDown(bExit: false);
	}

	private void ShutDown(bool bExit)
	{
		m_bShuttingDown = true;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Shutting down", isXml: false, "RTMLIS");
		}
		m_OneMinTimer.Stop();
		m_OneMinTimer.Close();
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Timer closed", isXml: false, "RTMLIS");
		}
		CloseListenerPorts();
		foreach (Port p in m_portsList)
		{
			if (p.m_par.connectRemote > 0)
			{
				string sport = p.m_par.remotePort.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing client port " + sport, isXml: false, "RTMLIS");
				}
				p.m_CommType.Notify(-1);
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed client port " + sport, isXml: false, "RTMLIS");
				}
			}
		}
		m_portsList.Clear();
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Shutdown", isXml: false, "RTMLIS");
			m_NNBase.StopLogging();
		}
		Console.WriteLine("Shutdown");
		if (bExit)
		{
			LibWrap.ExitProcess(1066u);
		}
	}

	private void CloseListenerPorts()
	{
		foreach (Port p in m_portsList)
		{
			if (p.m_par.connectRemote == 0)
			{
				string sport = p.m_par.portNum.ToString();
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closing listener port " + sport, isXml: false, "RTMLIS");
				}
				p.m_CommType.Notify(-1);
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log("Closed listener port " + sport, isXml: false, "RTMLIS");
				}
			}
		}
	}

	private void handleException(Exception e, string when, string from)
	{
		if (!m_bShuttingDown)
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
			if (!bDBDisconnect)
			{
				ShutDown(bExit: true);
			}
		}
	}

	private void handleDBException(OdbcException e, string when, string from)
	{
		if (m_bShuttingDown)
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
		if (!bDBDisconnect)
		{
			ShutDown(bExit: true);
		}
	}
}
