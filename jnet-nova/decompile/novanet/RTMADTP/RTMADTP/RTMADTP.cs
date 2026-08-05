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
using System.Timers;
using Microsoft.Win32;
using NNClass;

namespace RTMADTP;

public class RTMADTP : ServiceBase
{
	private Container components;

	public NNBase m_NNBase = new NNBase();

	private Timer m_OneMinTimer;

	private object m_objTimerLock = new object();

	public bool m_bShuttingDown;

	private bool m_bOnTimedEvent;

	public bool bWorkerThreadsWereStarted;

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

	public HL7Protocol m_theProtocol;

	public static bool m_b_loc_last_update_inst_class_column;

	public static bool m_b_loc_last_update_inst_type_column;

	public static bool m_b_patient_tracking_pt_uuid_column;

	public RTMADTP()
	{
		InitializeComponent();
		Assembly asm = Assembly.GetExecutingAssembly();
		version = asm.GetName().Version.ToString();
		m_NNBase.NNBaseOpen(bLogging: true, "RTMADTP", "RTMADTP", "ADTP");
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
				log("RTMADTP version = " + version + "  Database version = " + db_ver, isXml: false, "RTMADTP");
				m_NNBase.CheckIfAuthorized("RTMADTP", ref myCommand, ShutDown);
				m_NNBase.CheckAssemblyVersion("RTMADTP", version, ref myCommand, ShutDown);
				m_NNBase.RegisterStart(myCommand);
				bDBAvailable = m_NNBase.bDBAvailable;
				if (bDBAvailable)
				{
					sql = "select tot_messages_processed from dba.health_ping where process_name = 'RTMADTP' and host = '" + m_NNBase.GetLocalPOP() + "'";
					myCommand.CommandText = sql;
					OdbcDataReader theReader = myCommand.ExecuteReader();
					if (theReader.Read() && !theReader.IsDBNull(0))
					{
						m_iTotMessages = theReader.GetInt32(0);
					}
					theReader.Close();
					myCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'loc_last_update') order by column_id";
					theReader = myCommand.ExecuteReader();
					while (theReader.Read())
					{
						if (!theReader.IsDBNull(0))
						{
							string field = theReader.GetValue(0).ToString();
							if (string.Compare(field, "inst_class", ignoreCase: true) == 0)
							{
								m_b_loc_last_update_inst_class_column = true;
							}
							else if (string.Compare(field, "inst_type", ignoreCase: true) == 0)
							{
								m_b_loc_last_update_inst_type_column = true;
							}
						}
					}
					theReader.Close();
					string myPTConnectString = "DSN=" + m_NNBase.PROFILETRACKDATASOURCE + ";UID=" + m_NNBase.PROFILETRACKUAUTHORITY + ";PWD=" + m_NNBase.PROFILETRACKPAUTHORITY;
					OdbcConnection myPTDBReadConnection = null;
					OdbcCommand myPTDBReadCommand = null;
					bool bMyPTDBAvailable = false;
					m_NNBase.OpenDBConnection(ref myPTDBReadConnection, ref myPTDBReadCommand, 7, myPTConnectString, ref bMyPTDBAvailable, "Profile_Track DB");
					if (bMyPTDBAvailable)
					{
						myPTDBReadCommand.CommandText = "select column_name from sys.syscolumn where table_id in (select table_id from sys.systable where table_name = 'patient_tracking') order by column_id";
						theReader = myPTDBReadCommand.ExecuteReader();
						while (theReader.Read())
						{
							if (!theReader.IsDBNull(0))
							{
								string field2 = theReader.GetValue(0).ToString();
								if (string.Compare(field2, "pt_uuid", ignoreCase: true) == 0)
								{
									m_b_patient_tracking_pt_uuid_column = true;
								}
							}
						}
						theReader.Close();
						m_NNBase.CloseDBConnection(ref myPTDBReadConnection, ref myPTDBReadCommand);
						bRet = true;
					}
				}
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "initializing", "RTMADTP");
		}
		catch (Exception e2)
		{
			handleException(e2, "initializing", "RTMADTP");
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
				new RTMADTP()
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
		base.ServiceName = "RTMADTP";
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
				log("Disposing", isXml: false, "RTMADTP");
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
				if (m_theProtocol == null)
				{
					m_theProtocol = new HL7Protocol(m_NNBase.m_isLogging, this);
					log("HL7 protocol created", isXml: false, "RTMADTP");
				}
				else if (m_theProtocol.m_isShutDown || m_theProtocol.m_isShuttingDown)
				{
					m_theProtocol = null;
				}
				UpdateHealthPing();
				log("UpdateHealthPing() returned " + m_dbReady, isXml: false, "RTMADTP");
			}
		}
		m_bOnTimedEvent = false;
		m_OneMinTimer.Start();
	}

	private void UpdateHealthPing()
	{
		try
		{
			if (!GetDBConnByTimer())
			{
				return;
			}
			m_NNBase.GetDoLog(m_dbCommByTimer);
			bDBAvailable = m_NNBase.bDBAvailable;
			bool bStartLogging = m_NNBase.m_bLogging & !m_NNBase.m_isLogging;
			bool bStopLogging = !m_NNBase.m_bLogging & m_NNBase.m_isLogging;
			if (bStartLogging)
			{
				m_NNBase.StartLogging();
				if (m_theProtocol != null)
				{
					m_theProtocol.ProcessNotify(1);
				}
			}
			else if (bStopLogging)
			{
				if (m_theProtocol != null)
				{
					m_theProtocol.ProcessNotify(2);
				}
				log("logging turned off", isXml: false, "RTMADTP");
				m_NNBase.StopLogging();
			}
			bool bWorkerThreadsAreRunning = !m_bShuttingDown && m_theProtocol != null && m_theProtocol.m_ProtocolThread != null && m_theProtocol.m_ProtocolThread.IsAlive && !m_theProtocol.m_isShutDown && !m_theProtocol.m_isShuttingDown;
			if (bWorkerThreadsAreRunning)
			{
				bWorkerThreadsWereStarted = true;
			}
			m_NNBase.RegisterIfOtherThreadsAreActive(m_dbCommByTimer, m_iNumMessages, m_iTotMessages, bWorkerThreadsAreRunning);
			if (bWorkerThreadsWereStarted && !m_bShuttingDown && (m_theProtocol == null || m_theProtocol.m_ProtocolThread == null || !m_theProtocol.m_ProtocolThread.IsAlive || m_theProtocol.m_isShutDown || m_theProtocol.m_isShuttingDown))
			{
				if (!m_NNBase.m_isLogging)
				{
					m_NNBase.ForceLogging("ThreadDeath");
				}
				m_NNBase.log("Shutting down due to the death of worker thread", isXml: false, "RTMADTP");
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

	public void ShutDown(string reason, string whofrom, bool bExit)
	{
		log(reason, isXml: false, whofrom);
		ShutDown(bExit: false);
	}

	private void ShutDown(bool bExit)
	{
		m_bShuttingDown = true;
		if (m_NNBase.m_isLogging)
		{
			log("Shutting down", isXml: false, "RTMADTP");
		}
		m_OneMinTimer.Close();
		if (m_NNBase.m_isLogging)
		{
			log("Timer closed", isXml: false, "RTMADTP");
		}
		if (m_theProtocol != null)
		{
			m_theProtocol.ProcessNotify(-1);
		}
		if (m_NNBase.m_isLogging)
		{
			log("Shutdown", isXml: false, "RTMADTP");
			m_NNBase.StopLogging();
		}
		m_OneMinTimer.Stop();
		m_OneMinTimer.Close();
		Console.WriteLine("Shutdown");
		if (bExit)
		{
			LibWrap.ExitProcess(1066u);
		}
	}

	public void log(string s, bool isXml, string whoFrom)
	{
		m_NNBase.log(s, isXml, whoFrom);
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
