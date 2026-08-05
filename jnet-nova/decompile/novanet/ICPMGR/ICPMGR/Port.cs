using System;
using System.Data.Odbc;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.ServiceProcess;
using System.Threading;
using FlexTimers;
using NNClass;

namespace ICPMGR;

public class Port
{
	public abstract class CommType
	{
		public abstract bool Run();

		public abstract void Notify(int cd, string message);

		public abstract bool IsAlive();
	}

	private class SocketCommType : CommType
	{
		public override bool Run()
		{
			throw new NotImplementedException();
		}

		public override bool IsAlive()
		{
			throw new NotImplementedException();
		}

		public override void Notify(int cd, string message)
		{
		}
	}

	private class SocketServerCommType : SocketCommType
	{
		public AsynchNetworkServer theServer;

		public SocketServerCommType(PortParams par)
		{
			theServer = new AsynchNetworkServer(par);
		}

		public override bool Run()
		{
			theServer.Run();
			return true;
		}

		public override bool IsAlive()
		{
			return theServer.IsAlive();
		}

		public override void Notify(int cd, string message)
		{
			theServer.Notify(cd, message);
		}
	}

	public class AsynchNetworkServer
	{
		public static class ServerCommon
		{
			public static ClientHandlerList m_handlers = new ClientHandlerList();

			public static int MaxConnections = 300;

			public static int MaxConnectWaitTime = 30000;

			public static FlexTimer m_ListCleanerTimer = new FlexTimer();

			public static NNBase m_ListCleanerNNBase = new NNBase();

			public static bool m_ShuttingPorts;

			private static bool bListCleanerRunning = false;

			private static object m_commonLock = new object();

			public static void GetProcessControl()
			{
				lock (m_commonLock)
				{
					if (!ICPMGR.m_bShuttingDown && !m_ShuttingPorts)
					{
						try
						{
							ICPMGR.m_NNBase.BeginProcessControl();
							ICPMGR.m_NNBase.GetProcessControlValue("ICP", "MaxICPMGRConnections", ref MaxConnections);
							ICPMGR.m_NNBase.GetProcessControlValue("ICP", "MaxICPMGRConnectWaitTime", ref MaxConnectWaitTime);
							ICPMGR.m_NNBase.EndProcessControl();
						}
						catch
						{
						}
						ICPMGR.m_ICPBytesBuffers = new Buffers(MaxConnections * 3 + 20, 32768);
					}
				}
			}

			public static void StartListCleaner()
			{
				lock (m_commonLock)
				{
					if (!ICPMGR.m_bShuttingDown && !m_ShuttingPorts && !bListCleanerRunning)
					{
						m_ListCleanerNNBase.m_db_ver = ICPMGR.db_ver;
						m_ListCleanerNNBase.NNBaseOpen(ICPMGR.m_NNBase.m_isLogging, "ListCleaner", "ICPMGR", "LC");
						m_ListCleanerTimer.Interval = 60000u;
						m_ListCleanerTimer.theCallBack = OnListCleanerTimedEvent;
						m_ListCleanerTimer.Start();
						bListCleanerRunning = true;
					}
				}
			}

			private static void OnListCleanerTimedEvent()
			{
				bool bOKToClean = false;
				lock (m_commonLock)
				{
					bOKToClean = !ICPMGR.m_bShuttingDown && !m_ShuttingPorts;
				}
				if (bOKToClean)
				{
					CleanupList();
				}
			}

			public static void StopListCleanerTimer()
			{
				lock (m_commonLock)
				{
					if (m_ListCleanerTimer != null && bListCleanerRunning)
					{
						m_ListCleanerTimer.Stop();
					}
					bListCleanerRunning = false;
				}
			}

			public static void ShutDownListCleanerTimer()
			{
				lock (m_commonLock)
				{
					if (m_ListCleanerTimer != null && bListCleanerRunning)
					{
						lock (m_ListCleanerTimer)
						{
							if (m_ListCleanerTimer != null)
							{
								m_ListCleanerTimer.Close();
							}
						}
						lock (m_ListCleanerTimer)
						{
							if (m_ListCleanerTimer != null)
							{
								m_ListCleanerTimer.Dispose();
								m_ListCleanerTimer = null;
							}
						}
					}
					bListCleanerRunning = false;
				}
			}

			private static void CleanupList()
			{
				int NumClientsRemoved = 0;
				bool ClientRemoved = false;
				string reason = "";
				int numstopped = 0;
				string myclientID = "";
				new IPAddress(0L);
				bool bKillThreadAfterTimeOut = true;
				long DeviceTimeOutTicks = 300000000L;
				int pClientHandlers = -1;
				if (m_ListCleanerNNBase.m_isLogging)
				{
					m_ListCleanerNNBase.log("Starting cleanup", isXml: false, "CleanupList");
				}
				lock (m_handlers)
				{
					try
					{
						pClientHandlers = m_handlers.First();
						while (pClientHandlers >= 0)
						{
							ClientRemoved = false;
							bool bRemoveClient = false;
							ClientHandler h = (ClientHandler)m_handlers.m_Array[pClientHandlers];
							if (h != null && h.m_theProtocol != null && h.m_theProtocol.IsAliveAndWell() && h.m_InstrumentID == null && bKillThreadAfterTimeOut && DateTime.Now.Ticks > h.StartTimeStamp + DeviceTimeOutTicks)
							{
								reason = " because the protocol has failed to obtain an instrument ID for more than 2 minutes";
								h.Stop(bSignal: true, bkill: false, reason);
								string myClientID = h.GetClientID();
								string sh = h.socket.Handle.ToString();
								if (m_ListCleanerNNBase.m_isLogging)
								{
									m_ListCleanerNNBase.log("Client (" + myClientID + ") disconnected from port " + sh + reason, isXml: false, "CleanupList");
								}
								numstopped++;
								bRemoveClient = true;
							}
							if (h != null && !bRemoveClient && (h.m_theProtocol == null || (h.m_theProtocol != null && h.m_theProtocol.IsDeadOrDying())))
							{
								myclientID = h.GetClientID();
								reason = ((h.m_theProtocol != null) ? "the client's protocol is shut down or shutting down" : "the client has no protocol object");
								bRemoveClient = true;
							}
							if (bRemoveClient)
							{
								try
								{
									pClientHandlers = m_handlers.Remove();
								}
								catch (Exception ex)
								{
									if (m_ListCleanerNNBase.m_isLogging)
									{
										m_ListCleanerNNBase.log("Attempt to remove client handler threw exception " + ex.Message, isXml: false, "CleanupList");
									}
								}
								if (m_ListCleanerNNBase.m_isLogging)
								{
									string sh2 = h.socket.Handle.ToString();
									m_ListCleanerNNBase.log("Client (" + myclientID + ") removed from list on port " + sh2 + " because " + reason, isXml: false, "CleanupList");
								}
								h = null;
								ClientRemoved = true;
								NumClientsRemoved++;
							}
							if (!ClientRemoved)
							{
								pClientHandlers = m_handlers.Next();
							}
						}
					}
					catch (Exception ex2)
					{
						if (m_ListCleanerNNBase.m_isLogging)
						{
							m_ListCleanerNNBase.log("List Cleaner threw exception " + ex2.Message, isXml: false, "CleanupList");
						}
					}
				}
				if (numstopped > 0 && m_ListCleanerNNBase.m_isLogging)
				{
					string ThreadCount = "Thread Count after stopping " + numstopped + " protocols:" + Process.GetCurrentProcess().Threads.Count;
					m_ListCleanerNNBase.log(ThreadCount, isXml: false, "CleanupList");
				}
				if (NumClientsRemoved > 0 && m_ListCleanerNNBase.m_isLogging)
				{
					m_ListCleanerNNBase.log(NumClientsRemoved + " clients removed from list", isXml: false, "CleanupList");
				}
				lock (m_handlers)
				{
					m_handlers.m_Array.TrimToSize();
				}
				GC.Collect();
				if (m_ListCleanerNNBase.m_isLogging)
				{
					m_ListCleanerNNBase.log("Cleanup complete", isXml: false, "CleanupList");
				}
			}
		}

		public class ClientHandler
		{
			public Socket socket;

			private NetworkStream networkStream;

			public string m_protocol;

			public DMLProtocol m_theProtocol;

			public RCProtocol m_RCProtocol;

			public AsynchNetworkServer m_parent;

			public DMLICPBase m_DMLICPBase;

			public IPAddress m_InstrumentIP;

			public string m_InstrumentID;

			public long StartTimeStamp;

			public void Init(AsynchNetworkServer parent, ref Socket socketForClient, string protocol, IPAddress InstrumentIP, ref DMLICPBase myDMLICPBase)
			{
				socket = socketForClient;
				networkStream = null;
				m_protocol = protocol;
				m_theProtocol = null;
				m_parent = parent;
				m_DMLICPBase = myDMLICPBase;
				m_InstrumentIP = InstrumentIP;
				m_InstrumentID = null;
				StartTimeStamp = DateTime.Now.Ticks;
			}

			public void Start(string portType, string from_inst_id, bool logging, ref NNBase myListenerNNBase)
			{
				if (!socket.Connected)
				{
					return;
				}
				try
				{
					networkStream = new NetworkStream(socket);
				}
				catch (Exception ex)
				{
					if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_parent.m_ShuttingPort)
					{
						myListenerNNBase.ForceLogging("PORTS_Exception");
						myListenerNNBase.ReportErrorNoDB("Exception", "E", "creating network stream", "CientHandler.Start", ex.Message);
					}
					return;
				}
				switch (m_protocol)
				{
				case "DML":
					m_theProtocol = new DMLProtocol();
					m_theProtocol.InitDMLProtocol(this, ref networkStream, socket.Handle.ToInt32(), portType, from_inst_id, logging, m_DMLICPBase, ICPMGR.db_ver);
					if (m_theProtocol != null)
					{
						m_theProtocol.ProcessNotify(3, "");
					}
					break;
				case "Command":
					m_RCProtocol = new RCProtocol();
					m_RCProtocol.InitRCProtocol(this, ref networkStream, socket.Handle.ToInt32(), portType, from_inst_id, logging, m_DMLICPBase, ICPMGR.db_ver);
					if (m_RCProtocol != null)
					{
						m_RCProtocol.ProcessNotify(3, "");
					}
					break;
				}
			}

			public string GetClientID()
			{
				IPAddress ZeroIP = new IPAddress(0L);
				string myclientID = "instrument ID='";
				if (m_InstrumentID != null && m_InstrumentID.Length > 0)
				{
					myclientID += m_InstrumentID;
				}
				myclientID += "' IPAddress='";
				if (m_InstrumentIP != null && m_InstrumentIP != ZeroIP)
				{
					myclientID += m_InstrumentIP.ToString();
				}
				return myclientID + "'";
			}

			public void Stop(bool bSignal, bool bkill, string reason)
			{
				if (m_theProtocol == null || m_theProtocol.m_isShutDown)
				{
					return;
				}
				m_theProtocol.m_ShutdownReason = reason;
				m_theProtocol.m_stopping = true;
				GetClientID();
				socket.Handle.ToString();
				if (bSignal)
				{
					try
					{
						m_theProtocol.ProcessNotify(-1, reason);
					}
					catch
					{
					}
				}
				try
				{
					socket.Close();
				}
				catch
				{
				}
				if (!bkill || m_theProtocol == null || m_theProtocol.m_isShutDown)
				{
					return;
				}
				if (bSignal)
				{
					int loopcount = 0;
					while (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_parent.m_ShuttingPort && m_theProtocol != null && !m_theProtocol.m_isShutDown && loopcount < 50)
					{
						loopcount++;
						Thread.Sleep(200);
					}
				}
				if (m_theProtocol != null && !m_theProtocol.m_isShutDown)
				{
					m_theProtocol.Kill();
				}
			}
		}

		public class ClientHandlerList : CTList
		{
			public int GetElement()
			{
				int pElem = IndexGetElement();
				if (IsNew(pElem))
				{
					ClientHandler NewElem = new ClientHandler();
					m_Array.Add(NewElem);
				}
				return pElem;
			}

			public void StopDuplicatePort(Socket socket, ref NNBase myListenerNNBase)
			{
				int pClientHandlers = First();
				ClientHandler h = null;
				while (pClientHandlers >= 0)
				{
					h = (ClientHandler)m_Array[pClientHandlers];
					if (h != null && h.socket != null && h.socket.Handle.ToInt32() == socket.Handle.ToInt32())
					{
						string reason = " because a more recent session is starting using this port";
						h.Stop(bSignal: true, bkill: false, reason);
						string myClientID = h.GetClientID();
						string sh = h.socket.Handle.ToString();
						myListenerNNBase.log("Client (" + myClientID + ") disconnected from port " + sh + reason, isXml: false, "StopDuplicatePort");
						pClientHandlers = Remove();
					}
					else
					{
						pClientHandlers = Next();
					}
				}
			}

			public void StopDuplicateInstrument(DMLProtocol myProtocol, string InstrumentID, ref NNBase myNNBase)
			{
				int pClientHandlers = First();
				ClientHandler h = null;
				while (pClientHandlers >= 0)
				{
					h = (ClientHandler)m_Array[pClientHandlers];
					if (h == null || h.m_InstrumentID == null || !(h.m_InstrumentID == InstrumentID) || h.m_theProtocol == null || object.Equals(h.m_theProtocol, myProtocol))
					{
						pClientHandlers = ((h == null || h.m_InstrumentID == null || !(h.m_InstrumentID == InstrumentID) || h.m_theProtocol == null || !object.Equals(h.m_theProtocol, myProtocol)) ? Next() : (-1));
						continue;
					}
					string reason = " because a more recent session is starting for this instrument ID";
					h.Stop(bSignal: true, bkill: true, reason);
					string myClientID = h.GetClientID();
					string sh = h.socket.Handle.ToString();
					myNNBase.log("Client (" + myClientID + ") disconnected from port " + sh + reason, isXml: false, "StopDuplicateInstrument");
					pClientHandlers = Remove();
				}
			}

			public void StopInstrument(string InstrumentID, ref NNBase myNNBase)
			{
				int pClientHandlers = First();
				ClientHandler h = null;
				while (pClientHandlers >= 0)
				{
					h = (ClientHandler)m_Array[pClientHandlers];
					if (h != null && h.m_InstrumentID != null && h.m_InstrumentID == InstrumentID)
					{
						string reason = " because sessions for this instrument ID have been asked to stop";
						h.Stop(bSignal: true, bkill: true, reason);
						string myClientID = h.GetClientID();
						string sh = h.socket.Handle.ToString();
						myNNBase.log("Client (" + myClientID + ") disconnected from port " + sh + reason, isXml: false, "StopInstrument");
						pClientHandlers = Remove();
					}
					else
					{
						pClientHandlers = Next();
					}
				}
			}

			public ClientHandler GetInstrument(string InstrumentID)
			{
				int pClientHandlers = Last();
				ClientHandler h = null;
				while (pClientHandlers >= 0)
				{
					h = (ClientHandler)m_Array[pClientHandlers];
					pClientHandlers = ((h == null || !(h.m_InstrumentID == InstrumentID)) ? Prev() : (-1));
				}
				return h;
			}
		}

		private string m_protocol;

		private string m_portType;

		private string m_from_inst_id;

		private int m_portNum;

		private Thread myListener;

		public NNBase m_ListenerNNBase = new NNBase();

		private TalkSocket ClientTalkSocket = new TalkSocket(lingerenable: true, 1, 30000uL, 1000uL);

		private Socket m_socketForClient;

		private TcpListener m_tcpListener;

		public bool m_logging;

		public bool m_bEnabled;

		public bool m_ShuttingPort;

		public OdbcConnection myCheckConnection;

		public OdbcCommand myCheckCommand;

		public AsynchNetworkServer(PortParams par)
		{
			m_protocol = par.protocol;
			m_portNum = par.portNum;
			m_logging = par.do_logging;
			m_portType = par.portType;
			m_from_inst_id = par.instrumentId;
			m_ListenerNNBase.m_db_ver = ICPMGR.db_ver;
			m_ListenerNNBase.NNBaseOpen(m_logging, m_protocol + "_Listener", "ICPMGR", par.instrumentId);
		}

		public bool IsDBServiceRunning(string serviceName)
		{
			bool serviceRunning = false;
			try
			{
				string DBHostName = m_ListenerNNBase.GetHostName();
				serviceName += DBHostName;
				ServiceController sc = new ServiceController(serviceName, DBHostName);
				if (sc.Status.Equals(ServiceControllerStatus.Running))
				{
					serviceRunning = true;
				}
			}
			catch
			{
			}
			return serviceRunning;
		}

		public bool IsDBConnectable(ref NNBase myNNBase)
		{
			bool bOK = false;
			myCheckCommand.CommandText = "Select Count(*) from dba.pop_info";
			try
			{
				int count = (int)myCheckCommand.ExecuteScalar();
				return count > 0;
			}
			catch
			{
				return m_ListenerNNBase.OpenDBConnection(ref myCheckConnection, ref myCheckCommand);
			}
		}

		public void Run()
		{
			m_bEnabled = true;
			myListener = new Thread(theListener);
			myListener.Name = "LISTENER";
			myListener.Start();
		}

		public bool IsAlive()
		{
			if (myListener != null)
			{
				return myListener.IsAlive;
			}
			return false;
		}

		public void Notify(int cd, string message)
		{
			bool bPassOnToClients = false;
			try
			{
				switch (cd)
				{
				case 1:
					m_logging = true;
					if (!ICPMGR.SuppressClientLogs)
					{
						bPassOnToClients = m_protocol.CompareTo("DML") == 0;
					}
					break;
				case 2:
					m_logging = false;
					bPassOnToClients = m_protocol.CompareTo("DML") == 0;
					break;
				case -1:
					m_ShuttingPort = true;
					if (m_tcpListener != null)
					{
						if (ICPMGR.m_NNBase.m_isLogging)
						{
							string ThreadCount = "Thread Count before stopping TCP listener:" + Process.GetCurrentProcess().Threads.Count;
							ICPMGR.m_NNBase.log(ThreadCount, isXml: false, "NOTIFY");
						}
						m_tcpListener.Stop();
						if (ICPMGR.m_NNBase.m_isLogging)
						{
							string ThreadCount2 = "Thread Count after stopping TCP listener:" + Process.GetCurrentProcess().Threads.Count;
							ICPMGR.m_NNBase.log(ThreadCount2, isXml: false, "NOTIFY");
						}
					}
					if (myListener != null)
					{
						if (ICPMGR.m_NNBase.m_isLogging)
						{
							string ThreadCount3 = "Thread Count before stopping listener thread:" + Process.GetCurrentProcess().Threads.Count;
							ICPMGR.m_NNBase.log(ThreadCount3, isXml: false, "NOTIFY");
						}
						myListener.Abort();
						myListener.Join();
						myListener = null;
						if (ICPMGR.m_NNBase.m_isLogging)
						{
							string ThreadCount4 = "Thread Count after stopping listener thread:" + Process.GetCurrentProcess().Threads.Count;
							ICPMGR.m_NNBase.log(ThreadCount4, isXml: false, "NOTIFY");
						}
					}
					m_ListenerNNBase.NNBaseClose();
					bPassOnToClients = true;
					break;
				case 4:
					bPassOnToClients = m_protocol.CompareTo("DML") == 0;
					break;
				case 5:
					bPassOnToClients = m_protocol.CompareTo("DML") == 0;
					break;
				case 6:
					if (ICPMGR.m_NNBase.m_isLogging)
					{
						ICPMGR.m_NNBase.log("Pausing Listener" + message, isXml: false, "NOTIFY");
					}
					m_bEnabled = false;
					break;
				case 7:
					if (ICPMGR.m_NNBase.m_isLogging)
					{
						ICPMGR.m_NNBase.log("Resuming Listener", isXml: false, "NOTIFY");
					}
					m_bEnabled = true;
					break;
				}
				if (!bPassOnToClients)
				{
					return;
				}
				int numStopping = 0;
				int numAlreadyStopping = 0;
				int numAskedToStop = 0;
				int numKilled = 0;
				if (cd == -1 && ICPMGR.m_NNBase.m_isLogging)
				{
					string ThreadCount5 = "Thread Count before stopping protocols: " + Process.GetCurrentProcess().Threads.Count;
					ICPMGR.m_NNBase.log(ThreadCount5, isXml: false, "NOTIFY");
				}
				lock (ServerCommon.m_handlers)
				{
					for (int pclienthandler = ServerCommon.m_handlers.First(); pclienthandler >= 0; pclienthandler = ServerCommon.m_handlers.Next())
					{
						try
						{
							ClientHandler h = (ClientHandler)ServerCommon.m_handlers.m_Array[pclienthandler];
							if (h != null && h.m_theProtocol != null)
							{
								if (cd == -1)
								{
									if (h.m_theProtocol.IsAliveAndWell())
									{
										string reason = ((message.Length > 0) ? message : " because notify is signaling protocols to shut down");
										h.Stop(bSignal: true, bkill: false, reason);
										string myClientID = h.GetClientID();
										string sh = h.socket.Handle.ToString();
										ICPMGR.m_NNBase.log("Client (" + myClientID + ") disconnected from port " + sh + reason, isXml: false, "Notify");
										numStopping++;
										numAskedToStop++;
									}
									else if (h.m_theProtocol.IsDying())
									{
										numStopping++;
										numAlreadyStopping++;
									}
								}
								else if (h.m_theProtocol.IsAliveAndWell())
								{
									switch (cd)
									{
									case 4:
										h.m_theProtocol.ProcessNotify(1, "");
										break;
									case 5:
										h.m_theProtocol.ProcessNotify(2, "");
										break;
									default:
										h.m_theProtocol.ProcessNotify(cd, "");
										break;
									}
								}
							}
						}
						catch (Exception ex)
						{
							ICPMGR.m_NNBase.log(ex.Message, isXml: false, "Port-NOTIFY(PassOnToClients)");
						}
					}
				}
				if (numStopping > 0)
				{
					int numactive = numStopping;
					int loopcount = 0;
					while (numactive > 0)
					{
						try
						{
							loopcount++;
							Thread.Sleep(200);
							lock (ServerCommon.m_handlers)
							{
								numactive = 0;
								for (int pclienthandler2 = ServerCommon.m_handlers.First(); pclienthandler2 >= 0; pclienthandler2 = ServerCommon.m_handlers.Next())
								{
									ClientHandler h2 = (ClientHandler)ServerCommon.m_handlers.m_Array[pclienthandler2];
									if (h2 != null && h2.m_theProtocol != null && !h2.m_theProtocol.m_isShutDown)
									{
										if (loopcount < 10)
										{
											numactive++;
										}
										else
										{
											string reason2 = " because notify has waited too long for protocol to shut down";
											h2.Stop(bSignal: false, bkill: true, reason2);
											string myClientID2 = h2.GetClientID();
											string sh2 = h2.socket.Handle.ToString();
											ICPMGR.m_NNBase.log("Client (" + myClientID2 + ") disconnected from port " + sh2 + reason2, isXml: false, "Notify");
											numKilled++;
										}
									}
								}
							}
						}
						catch (Exception ex2)
						{
							ICPMGR.m_NNBase.log(ex2.Message, isXml: false, "Port-NOTIFY(numStopping)");
						}
					}
				}
				if (cd != -1)
				{
					return;
				}
				if (ICPMGR.m_NNBase.m_isLogging)
				{
					ICPMGR.m_NNBase.log("Clearing client handler list", isXml: false, "NOTIFY");
				}
				lock (ServerCommon.m_handlers)
				{
					ServerCommon.m_handlers.PurgeList();
				}
				if (ICPMGR.m_NNBase.m_isLogging)
				{
					ICPMGR.m_NNBase.log("Cleared client handler list", isXml: false, "NOTIFY");
				}
				if (m_socketForClient != null)
				{
					if (ICPMGR.m_NNBase.m_isLogging)
					{
						ICPMGR.m_NNBase.log("Closing socket for client", isXml: false, "NOTIFY");
					}
					m_socketForClient.Close();
					if (ICPMGR.m_NNBase.m_isLogging)
					{
						ICPMGR.m_NNBase.log("Closed socket for client", isXml: false, "NOTIFY");
					}
				}
				if ((numStopping > 0 || numKilled > 0) && ICPMGR.m_NNBase.m_isLogging)
				{
					ICPMGR.m_NNBase.log("We waited for " + numAlreadyStopping + " protocols that were already stopping", isXml: false, "NOTIFY");
					ICPMGR.m_NNBase.log("We asked " + numAskedToStop + " protocols to stop", isXml: false, "NOTIFY");
					ICPMGR.m_NNBase.log("We had to brute-force kill " + numKilled + " protocols", isXml: false, "NOTIFY");
					string ThreadCount6 = "Thread Count is now " + Process.GetCurrentProcess().Threads.Count;
					ICPMGR.m_NNBase.log(ThreadCount6, isXml: false, "NOTIFY");
				}
			}
			catch (Exception ex3)
			{
				ICPMGR.m_NNBase.log(ex3.Message, isXml: false, "Port-NOTIFY");
			}
		}

		public void theListener()
		{
			bool bListening = false;
			try
			{
				IPHostEntry hostInfo = Dns.GetHostEntry(Dns.GetHostName());
				IPAddress[] address = hostInfo.AddressList;
				int addressIndex = -1;
				for (int n = 0; n < address.Length; n++)
				{
					if (address[n].AddressFamily == AddressFamily.InterNetwork)
					{
						addressIndex = n;
						break;
					}
				}
				IPEndPoint ipLocalEndPoint = new IPEndPoint(address[addressIndex], m_portNum);
				m_tcpListener = new TcpListener(ipLocalEndPoint);
				m_tcpListener.Start();
				bListening = true;
			}
			catch (Exception ex)
			{
				if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort)
				{
					m_ListenerNNBase.ForceLogging("PORTS_Exception");
					m_ListenerNNBase.ReportErrorNoDB("Exception", "C", "initializing listener", "theListener", ex.Message);
					if (m_tcpListener != null)
					{
						m_tcpListener.Stop();
					}
					m_ListenerNNBase.NNBaseClose();
					if (m_protocol.CompareTo("DML") == 0)
					{
						ICPMGR.m_Port.m_isRunning = false;
						ICPMGR.m_Port.m_CommType = null;
					}
					else
					{
						ICPMGR.m_RCPort.m_isRunning = false;
						ICPMGR.m_RCPort.m_CommType = null;
					}
				}
				return;
			}
			bool bFirstTime = true;
			string LastErrorMessage = "";
			bool bDBWasConnectable = m_ListenerNNBase.OpenDBConnection(ref myCheckConnection, ref myCheckCommand);
			bool bErrorCondition = !bDBWasConnectable;
			while (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort)
			{
				try
				{
					int iNumConnections;
					lock (ServerCommon.m_handlers)
					{
						iNumConnections = ServerCommon.m_handlers.GetNumUsedElements();
					}
					if (m_bEnabled && (m_protocol.CompareTo("Command") == 0 || (m_protocol.CompareTo("DML") == 0 && ICPMGR.m_DMLICPBase.m_bIsReady && iNumConnections < ServerCommon.MaxConnections)))
					{
						if (bErrorCondition)
						{
							bFirstTime = true;
						}
						if (m_logging && bFirstTime)
						{
							m_ListenerNNBase.log("Checking for DB connectability", isXml: false, "theListener");
						}
						bool bDBConnectable = IsDBConnectable(ref m_ListenerNNBase);
						if (m_logging && (bFirstTime || (bDBConnectable && !bDBWasConnectable) || (!bDBConnectable && bDBWasConnectable)))
						{
							m_ListenerNNBase.log("Database " + (bDBConnectable ? "is connectable" : "is not connectable"), isXml: false, "theListener");
						}
						bDBWasConnectable = bDBConnectable;
						if (bDBConnectable)
						{
							bErrorCondition = false;
						}
						if (bDBConnectable)
						{
							if (!bListening)
							{
								m_tcpListener.Start();
								bListening = true;
							}
							if (m_logging && bFirstTime)
							{
								m_ListenerNNBase.log("Waiting for " + ((m_protocol.CompareTo("DML") == 0) ? "device" : "remote control") + " connection", isXml: false, m_protocol + ".theListener");
							}
							m_socketForClient = (ClientTalkSocket.theSocket = m_tcpListener.AcceptSocket());
							if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort)
							{
								ClientTalkSocket.Init();
								if (m_socketForClient.Connected)
								{
									if (m_logging && bFirstTime)
									{
										m_ListenerNNBase.log("Client connected on " + m_portNum + ":" + m_socketForClient.Handle.ToString(), isXml: false, "theListener");
									}
									if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort)
									{
										IPAddress InstrumentIP = ((IPEndPoint)m_socketForClient.RemoteEndPoint).Address;
										ClientHandler handler = null;
										lock (ServerCommon.m_handlers)
										{
											int pnewclienthandler = ServerCommon.m_handlers.GetElement();
											handler = (ClientHandler)ServerCommon.m_handlers.m_Array[pnewclienthandler];
											handler.Init(this, ref m_socketForClient, m_protocol, InstrumentIP, ref ICPMGR.m_DMLICPBase);
											ServerCommon.m_handlers.StopDuplicatePort(m_socketForClient, ref m_ListenerNNBase);
											ServerCommon.m_handlers.Append(pnewclienthandler);
										}
										handler.Start(m_portType, m_from_inst_id, m_logging & !ICPMGR.SuppressClientLogs, ref m_ListenerNNBase);
										if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort)
										{
											if (handler != null && handler.m_theProtocol != null)
											{
												if (m_logging && bFirstTime)
												{
													string ThreadCount = "Thread Count after adding client handler:" + Process.GetCurrentProcess().Threads.Count;
													m_ListenerNNBase.log(ThreadCount, isXml: false, "theListener");
												}
												LastErrorMessage = "";
												bErrorCondition = false;
											}
											else if (m_logging && bFirstTime && LastErrorMessage != "Client handler or protocol creation failure")
											{
												m_ListenerNNBase.log("Client handler or protocol creation failure", isXml: false, "theListener");
												bErrorCondition = true;
												LastErrorMessage = "Client handler or protocol creation failure";
											}
										}
									}
								}
							}
							if (ICPMGR.m_bShuttingDown || ServerCommon.m_ShuttingPorts || m_ShuttingPort || !m_socketForClient.Connected)
							{
								m_socketForClient.Close();
							}
						}
						else
						{
							bErrorCondition = true;
							if (m_logging && LastErrorMessage != "Database Connection Failed")
							{
								m_ListenerNNBase.log("Database Connection Failed", isXml: false, "theListener");
								LastErrorMessage = "Database Connection Failed";
							}
							m_tcpListener.Stop();
							bListening = false;
							int i = 0;
							while (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort && i < ServerCommon.MaxConnectWaitTime)
							{
								Thread.Sleep(100);
								i += 100;
							}
						}
					}
					else
					{
						bErrorCondition = true;
						if (m_logging)
						{
							if (!ICPMGR.m_DMLICPBase.m_bIsReady)
							{
								if (LastErrorMessage != "Waiting for list builder")
								{
									m_ListenerNNBase.log("Waiting for list builder...", isXml: false, "theListener");
									LastErrorMessage = "Waiting for list builder";
								}
							}
							else if (!m_bEnabled)
							{
								if (LastErrorMessage != "Sleeping")
								{
									m_ListenerNNBase.log("Sleeping...", isXml: false, "theListener");
									LastErrorMessage = "Sleeping";
								}
							}
							else if (LastErrorMessage != "Max connections reached")
							{
								m_ListenerNNBase.log("Max connections (" + ServerCommon.MaxConnections + ") reached", isXml: false, "theListener");
								LastErrorMessage = "Max connections reached";
							}
						}
						m_tcpListener.Stop();
						bListening = false;
						int i2 = 0;
						while (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort && i2 < ServerCommon.MaxConnectWaitTime)
						{
							Thread.Sleep(100);
							i2 += 100;
						}
					}
				}
				catch (Exception ex2)
				{
					if (!ICPMGR.m_bShuttingDown && !ServerCommon.m_ShuttingPorts && !m_ShuttingPort && LastErrorMessage != "Client handler spawn exception")
					{
						m_ListenerNNBase.ForceLogging("PORTS_Exception");
						m_ListenerNNBase.ReportErrorNoDB("Exception", "C", "spawning client handler", "theListener", ex2.Message);
						LastErrorMessage = "Client handler spawn exception";
						bErrorCondition = true;
					}
				}
				bFirstTime = false;
			}
			m_ListenerNNBase.NNBaseClose();
		}
	}

	public PortParams m_par;

	public bool m_isInvalid = true;

	public CommType m_CommType;

	public bool m_isRunning;

	public string InstrumentId
	{
		get
		{
			return m_par.instrumentId;
		}
		set
		{
			m_par.instrumentId = value;
		}
	}

	public bool InitPort(PortParams par, out string error)
	{
		bool bOK = false;
		error = string.Empty;
		if (m_isInvalid && !m_isRunning && m_CommType == null)
		{
			try
			{
				m_par = par;
				string commProtocol;
				if ((commProtocol = m_par.commProtocol) != null && commProtocol == "TCPIP")
				{
					if (par.connectRemote == 0)
					{
						m_CommType = new SocketServerCommType(m_par);
						bOK = m_CommType != null;
						m_isInvalid = false;
					}
					else
					{
						error = "par.connectRemote != 0";
					}
				}
				else
				{
					error = "m_par.commProtocol is " + m_par.commProtocol;
				}
			}
			catch (Exception ex)
			{
				error = "InitPort exception - " + ex.Message;
			}
		}
		return bOK;
	}

	public bool Run()
	{
		if (!m_isInvalid && !m_isRunning && m_CommType != null)
		{
			try
			{
				m_isRunning = m_CommType.Run();
			}
			catch
			{
			}
		}
		return m_isRunning;
	}
}
