#define TRACE
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.Odbc;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.ServiceProcess;
using System.Timers;
using System.Xml;
using Microsoft.Win32;
using NNClass;

namespace RTMADTQ;

public class RTMADTQ : ServiceBase
{
	private Container components;

	private Timer m_oneMinTimer = new Timer();

	private bool m_connectedToADTFeeder;

	private DateTime newConfigTime = new DateTime(1, 1, 1, 0, 0, 0);

	private string BinDir = "C:\\NovaBiomedical\\NovaNet\\Bin";

	public Port m_Port;

	public bool bWorkerThreadsWereStarted;

	public NNBase m_NNBase = new NNBase();

	private object m_objTimerLock = new object();

	public bool m_bShuttingDown;

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

	private List<string> m_facilityList;

	public List<string> AvailableFacility => m_facilityList;

	public bool ConnectedToADTFeeder
	{
		get
		{
			return m_connectedToADTFeeder;
		}
		set
		{
			m_connectedToADTFeeder = value;
		}
	}

	public RTMADTQ()
	{
		InitializeComponent();
		Assembly asm = Assembly.GetExecutingAssembly();
		version = asm.GetName().Version.ToString();
		m_NNBase.NNBaseOpen(bLogging: true, "RTMADTQ", "RTMADTQ", "ADTQ");
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
		try
		{
			BinDir = Registry.LocalMachine.OpenSubKey(m_NNBase.REGISTRY_SUBKEY_RTM).GetValue("BinDir").ToString() + "\\";
		}
		catch (Exception e)
		{
			m_NNBase.ReportException(e, "Getting registry entry for bin folder", "RTMADTQ");
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
				log("RTMADTQ version = " + version + "  Database version = " + db_ver, isXml: false, "RTMADTQ");
				m_NNBase.CheckIfAuthorized("RTMADTQ", ref myCommand, ShutDown);
				m_NNBase.CheckAssemblyVersion("RTMADTQ", version, ref myCommand, ShutDown);
				m_NNBase.RegisterStart(myCommand);
				bDBAvailable = m_NNBase.bDBAvailable;
				if (bDBAvailable)
				{
					sql = "select tot_messages_processed from dba.health_ping where process_name = 'RTMADTQ' and host = '" + m_NNBase.GetLocalPOP() + "'";
					myCommand.CommandText = sql;
					OdbcDataReader theReader = myCommand.ExecuteReader();
					if (theReader.Read() && !theReader.IsDBNull(0))
					{
						m_iTotMessages = theReader.GetInt32(0);
					}
					theReader.Close();
					bRet = true;
				}
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "initializing", "RTMADTQ");
		}
		catch (Exception e2)
		{
			handleException(e2, "initializing", "RTMADTQ");
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
				new RTMADTQ()
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
		base.ServiceName = "RTMADTQ";
	}

	protected override void OnStart(string[] args)
	{
		m_oneMinTimer.Interval = 1000.0;
		m_oneMinTimer.Elapsed += OnTimedEvent;
		m_oneMinTimer.Enabled = true;
		m_oneMinTimer.Start();
	}

	protected override void Dispose(bool disposing)
	{
		if (disposing)
		{
			if (m_NNBase.m_isLogging)
			{
				log("Disposing", isXml: false, "RTMADTQ");
			}
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
		lock (m_objTimerLock)
		{
			m_oneMinTimer.Stop();
			m_oneMinTimer.Interval = 60000.0;
			InitializePorts();
			if (!m_dbReady)
			{
				m_dbReady = GetDBDataForService();
			}
			if (m_dbReady)
			{
				UpdateHealthPing();
			}
			m_oneMinTimer.Start();
		}
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
				log("logging turned off", isXml: false, "RTMADTQ");
				m_NNBase.StopLogging();
			}
			bool bWorkerThreadsAreRunning = !m_bShuttingDown && m_Port != null && m_Port.m_CommType != null && m_Port.m_CommType.IsConnected() && m_Port.m_CommType.IsAlive();
			if (bWorkerThreadsAreRunning)
			{
				bWorkerThreadsWereStarted = true;
			}
			m_NNBase.RegisterIfOtherThreadsAreActive(m_dbCommByTimer, m_iNumMessages, m_iTotMessages, bWorkerThreadsAreRunning);
			if (bWorkerThreadsWereStarted && !m_bShuttingDown && (m_Port == null || m_Port.m_CommType == null || !m_Port.m_CommType.IsAlive()))
			{
				if (!m_NNBase.m_isLogging)
				{
					m_NNBase.ForceLogging("ThreadDeath");
				}
				m_NNBase.log("Shutting down due to the death of worker thread", isXml: false, "RTMADTQ");
				ShutDown(bExit: true);
			}
		}
		catch (OdbcException e)
		{
			handleDBException(e, "performing timed actions", "UpdateHealthPing");
		}
		catch (Exception e2)
		{
			handleException(e2, "performing timed actions", "UpdateHealthPing");
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
			PortParams par = default(PortParams);
			bool ret = GetPortInfoFromRTMADTConfig(ref par);
			par.do_logging = m_NNBase.m_isLogging;
			if (ret)
			{
				try
				{
					string filePath = BinDir + "RTMADT.xml";
					newConfigTime = File.GetCreationTime(filePath);
				}
				catch
				{
				}
				InitializePort(par);
			}
		}
		catch (Exception e)
		{
			handleException(e, "initializing ports", "InitializePorts");
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
					else if (DateTime.Compare(newConfigTime, pold.m_ConfigTime) > 0)
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
			if (found || par.portActive != 1)
			{
				return;
			}
			try
			{
				Port pnew = new Port(par, newConfigTime);
				m_portsList.Add(pnew);
				m_Port = pnew;
				if (m_NNBase.m_isLogging)
				{
					log("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType, isXml: false, "RTMADTQ");
				}
				Console.WriteLine("Add Port " + ((par.connectRemote > 0) ? par.remotePort : par.portNum) + " " + par.commProtocol + " C" + par.connectRemote + " M" + par.multiConnect + " T" + par.portType);
			}
			catch
			{
				ConnectedToADTFeeder = false;
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

	private bool PortHasChanged(PortParams oldpar, PortParams newpar)
	{
		return (oldpar.protocol != newpar.protocol) | (oldpar.portType != newpar.portType) | (oldpar.commProtocol != newpar.commProtocol) | (oldpar.portActive != newpar.portActive) | (oldpar.remoteHostName != newpar.remoteHostName);
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
			log("Shutting down", isXml: false, "RTMADTQ");
		}
		m_oneMinTimer.Stop();
		m_oneMinTimer.Close();
		if (m_NNBase.m_isLogging)
		{
			log("Timer closed", isXml: false, "RTMADTQ");
		}
		CloseListenerPorts();
		foreach (Port p in m_portsList)
		{
			if (p.m_par.connectRemote > 0)
			{
				string sport = p.m_par.remotePort.ToString();
				if (m_NNBase.m_isLogging)
				{
					log("Closing client port " + sport, isXml: false, "RTMADTQ");
				}
				p.m_CommType.Notify(-1);
				if (m_NNBase.m_isLogging)
				{
					log("Closed client port " + sport, isXml: false, "RTMADTQ");
				}
			}
		}
		m_portsList.Clear();
		if (m_NNBase.m_isLogging)
		{
			log("Shutdown", isXml: false, "RTMADTQ");
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
					log("Closing listener port " + sport, isXml: false, "RTMADTQ");
				}
				p.m_CommType.Notify(-1);
				if (m_NNBase.m_isLogging)
				{
					log("Closed listener port " + sport, isXml: false, "RTMADTQ");
				}
			}
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

	private bool GetPortInfoFromRTMADTConfig(ref PortParams par)
	{
		bool bOK_File = false;
		try
		{
			if (m_facilityList != null)
			{
				m_facilityList.Clear();
			}
			else
			{
				m_facilityList = new List<string>();
			}
			string filePath = BinDir + "RTMADT.xml";
			par.ipAddress = new byte[4];
			for (int n = 0; n < 4; n++)
			{
				par.ipAddress[n] = 0;
			}
			using FileStream fileStream = File.OpenRead(filePath);
			XmlReaderSettings settings = new XmlReaderSettings();
			settings.ConformanceLevel = ConformanceLevel.Document;
			using (XmlReader reader = XmlReader.Create(fileStream, settings))
			{
				while (reader.Read())
				{
					XmlNodeType nodeType = reader.NodeType;
					if (nodeType != XmlNodeType.Element)
					{
						continue;
					}
					string thisvar = reader.GetAttribute("Variable");
					string thisvalue = reader.GetAttribute("Value");
					if (thisvalue == null)
					{
						thisvalue = string.Empty;
					}
					if (!string.IsNullOrEmpty(thisvar))
					{
						switch (thisvar)
						{
						case "Port.protocol":
							par.protocol = thisvalue;
							break;
						case "Port.portType":
							par.portType = thisvalue;
							break;
						case "Port.commProtocol":
							par.commProtocol = thisvalue;
							break;
						case "Port.portNum":
							par.portNum = Convert.ToInt32(thisvalue);
							break;
						case "Port.connectRemote":
							par.connectRemote = Convert.ToInt32(thisvalue);
							break;
						case "Port.used":
							par.used = thisvalue;
							break;
						case "Port.multiConnect":
							par.multiConnect = thisvalue;
							break;
						case "Port.ipAddress0":
							par.ipAddress[0] = Convert.ToByte(thisvalue);
							break;
						case "Port.ipAddress1":
							par.ipAddress[1] = Convert.ToByte(thisvalue);
							break;
						case "Port.ipAddress2":
							par.ipAddress[2] = Convert.ToByte(thisvalue);
							break;
						case "Port.ipAddress3":
							par.ipAddress[3] = Convert.ToByte(thisvalue);
							break;
						case "Port.portActive":
							par.portActive = Convert.ToInt32(thisvalue);
							break;
						case "Port.remoteHostName":
							par.remoteHostName = thisvalue;
							break;
						case "Port.remotePort":
							par.remotePort = Convert.ToInt32(thisvalue);
							break;
						case "Facility":
							m_facilityList.Add(thisvalue.ToUpper());
							break;
						}
					}
				}
			}
			fileStream.Close();
			par.parent = this;
			bOK_File = true;
		}
		catch (FileNotFoundException ex)
		{
			m_NNBase.ReportErrorNoDB("No config file found", "E", "Loading Config file", "GetInfoFromRTMADTConfig", ex.Message);
		}
		catch (Exception ex2)
		{
			if (ex2.Message.IndexOf("Could not find file") >= 0)
			{
				m_NNBase.ReportErrorNoDB("No config file found", "E", "Loading Config file", "GetInfoFromRTMADTConfig", "");
			}
			else
			{
				m_NNBase.ReportException(ex2, "Opening Config file", "GetInfoFromRTMADTConfig");
			}
		}
		return bOK_File;
	}
}
