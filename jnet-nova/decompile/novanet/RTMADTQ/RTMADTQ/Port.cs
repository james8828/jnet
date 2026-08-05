using System;
using System.Collections;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Threading;

namespace RTMADTQ;

public class Port
{
	public abstract class CommType
	{
		public abstract bool Run();

		public abstract bool Notify(int cd);

		public abstract bool IsAlive();

		public abstract bool IsConnected();
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

		public override bool IsConnected()
		{
			throw new NotImplementedException();
		}

		public override bool Notify(int cd)
		{
			return false;
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

		public override bool IsConnected()
		{
			return theServer.IsConnected();
		}

		public override bool Notify(int cd)
		{
			return theServer.Notify(cd);
		}
	}

	private class SocketClientCommType : SocketCommType
	{
		public AsynchNetworkClient theClient;

		public SocketClientCommType(PortParams par)
		{
			theClient = new AsynchNetworkClient(par);
		}

		public override bool IsAlive()
		{
			return theClient.IsAlive();
		}

		public override bool IsConnected()
		{
			return theClient.IsConnected();
		}

		public override bool Run()
		{
			theClient.Run();
			return true;
		}

		public override bool Notify(int cd)
		{
			return theClient.Notify(cd);
		}
	}

	public class AsynchNetworkServer
	{
		public class ClientHandler
		{
			public Socket socket;

			private NetworkStream networkStream;

			private string m_protocol;

			public Protocol m_theProtocol;

			public AsynchNetworkServer m_parent;

			public ClientHandler(AsynchNetworkServer parent, Socket socketForClient, string protocol, int portNum, string portType, string from_inst_id, bool logging)
			{
				m_parent = parent;
				socket = socketForClient;
				m_protocol = protocol;
				networkStream = new NetworkStream(socketForClient);
				string protocol2;
				if ((protocol2 = m_protocol) != null && protocol2 == "HL7")
				{
					m_theProtocol = new HL7Protocol(ref networkStream, socket.Handle.ToInt32(), portType, logging, m_parent.m_parent, this, portNum);
				}
			}

			public void RemoveFromList(string caller)
			{
				int handlercount = m_parent.m_handlers.Count;
				if (handlercount > 0)
				{
					m_parent.RemoveFromList(this);
				}
				else if (m_parent.m_parent.m_NNBase.m_isLogging)
				{
					m_parent.m_parent.log("Attempt to remove client handler when handler count is zero", isXml: false, caller);
				}
			}
		}

		private string m_protocol;

		private string m_portType;

		private string m_from_inst_id;

		private int m_portNum;

		private Thread myListener;

		private TalkSocket ClientTalkSocket = new TalkSocket(lingerenable: true, 1, 30000uL, 1000uL);

		private Socket m_socketForClient;

		private TcpListener m_tcpListener;

		public bool m_logging;

		public bool m_ShuttingPort;

		public RTMADTQ m_parent;

		public ArrayList m_handlers = new ArrayList();

		public AsynchNetworkServer(PortParams par)
		{
			m_parent = par.parent;
			m_protocol = par.protocol;
			m_portNum = par.portNum;
			m_logging = par.do_logging;
			m_portType = par.portType;
			m_from_inst_id = par.instrumentId;
			m_ShuttingPort = false;
		}

		public void Run()
		{
			myListener = new Thread(theListener);
			myListener.Name = "LISTENER";
			myListener.Start();
		}

		public bool IsAlive()
		{
			bool bIsAlive = myListener != null && myListener.IsAlive;
			bool bClientHandlerIsAlive = false;
			if (bIsAlive && m_socketForClient != null)
			{
				foreach (ClientHandler h in m_handlers)
				{
					if (h.socket != null && h.m_theProtocol != null && !h.m_theProtocol.m_isShutDown && !h.m_theProtocol.m_isShuttingDown && h.socket == m_socketForClient && h.m_theProtocol != null && h.m_theProtocol.IsAlive())
					{
						bClientHandlerIsAlive = true;
						break;
					}
				}
				bIsAlive &= bClientHandlerIsAlive;
			}
			return bIsAlive;
		}

		public bool IsConnected()
		{
			if (m_socketForClient != null)
			{
				return m_socketForClient.Connected;
			}
			return false;
		}

		public void RemoveFromList(ClientHandler him)
		{
			string sh = him.socket.Handle.ToString();
			him.socket.Close();
			lock (m_handlers)
			{
				m_handlers.Remove(him);
			}
			if (m_parent.m_NNBase.m_isLogging)
			{
				m_parent.log("Client disconnected from " + m_portNum + ":" + sh, isXml: false, "RTMADTQ");
				string ThreadCount = "Thread Count after removing client handler:" + Process.GetCurrentProcess().Threads.Count;
				m_parent.log(ThreadCount, isXml: false, "RTMADTQ");
			}
		}

		public bool Notify(int cd)
		{
			switch (cd)
			{
			case 1:
				m_logging = true;
				break;
			case 2:
				m_logging = false;
				break;
			case -1:
				m_ShuttingPort = true;
				if (m_tcpListener != null)
				{
					if (m_parent.m_NNBase.m_isLogging)
					{
						string ThreadCount = "Thread Count before stopping TCP listener:" + Process.GetCurrentProcess().Threads.Count;
						m_parent.log(ThreadCount, isXml: false, "RTMADTQ");
					}
					m_tcpListener.Stop();
					if (m_parent.m_NNBase.m_isLogging)
					{
						string ThreadCount2 = "Thread Count after stopping TCP listener:" + Process.GetCurrentProcess().Threads.Count;
						m_parent.log(ThreadCount2, isXml: false, "RTMADTQ");
					}
				}
				break;
			}
			ArrayList handlers;
			lock (m_handlers)
			{
				handlers = new ArrayList(m_handlers);
			}
			foreach (ClientHandler h in handlers)
			{
				if (h.m_theProtocol != null && !h.m_theProtocol.m_isShutDown && !h.m_theProtocol.m_isShuttingDown)
				{
					string sport = h.socket.Handle.ToString();
					if (cd == -1 && m_parent.m_NNBase.m_isLogging)
					{
						string ThreadCount3 = "Thread Count before shutting down protocol for socket " + sport + ":" + Process.GetCurrentProcess().Threads.Count;
						m_parent.log(ThreadCount3, isXml: false, "RTMADTQ");
					}
					h.m_theProtocol.ProcessNotify(cd);
					if (cd == -1 && m_parent.m_NNBase.m_isLogging)
					{
						string ThreadCount4 = "Thread Count after shutting down protocol for socket " + sport + ":" + Process.GetCurrentProcess().Threads.Count;
						m_parent.log(ThreadCount4, isXml: false, "RTMADTQ");
					}
				}
			}
			handlers.Clear();
			if (cd == -1)
			{
				if (m_parent.m_NNBase.m_isLogging)
				{
					m_parent.log("Clearing client handler list", isXml: false, "RTMADTQ");
				}
				lock (m_handlers)
				{
					m_handlers.Clear();
				}
				if (m_parent.m_NNBase.m_isLogging)
				{
					m_parent.log("Cleared client handler list", isXml: false, "RTMADTQ");
				}
				if (m_socketForClient != null)
				{
					if (m_parent.m_NNBase.m_isLogging)
					{
						m_parent.log("Closing socket for client", isXml: false, "RTMADTQ");
					}
					m_socketForClient.Close();
					if (m_parent.m_NNBase.m_isLogging)
					{
						m_parent.log("Closed socket for client", isXml: false, "RTMADTQ");
					}
				}
				if (m_parent.m_NNBase.m_isLogging)
				{
					string ThreadCount5 = "Thread Count before stopping listener thread:" + Process.GetCurrentProcess().Threads.Count;
					m_parent.log(ThreadCount5, isXml: false, "RTMADTQ");
				}
				myListener.Abort();
				myListener.Join();
				if (m_parent.m_NNBase.m_isLogging)
				{
					string ThreadCount6 = "Thread Count after stopping listener thread:" + Process.GetCurrentProcess().Threads.Count;
					m_parent.log(ThreadCount6, isXml: false, "RTMADTQ");
				}
			}
			return false;
		}

		public void theListener()
		{
			int MaxConnections = 1;
			int MaxConnectWaitTime = 30000;
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
				while (!m_parent.m_bShuttingDown && !m_ShuttingPort)
				{
					try
					{
						if (m_handlers.Count < MaxConnections)
						{
							m_socketForClient = (ClientTalkSocket.theSocket = m_tcpListener.AcceptSocket());
							ClientTalkSocket.Init();
							if (m_socketForClient.Connected)
							{
								if (m_parent.m_NNBase.m_isLogging)
								{
									m_parent.log("Client connected on " + m_portNum + ":" + m_socketForClient.Handle.ToString(), isXml: false, "RTMADTQ");
								}
								ClientHandler handler = new ClientHandler(this, m_socketForClient, m_protocol, m_portNum, m_portType, m_from_inst_id, m_logging);
								lock (m_handlers)
								{
									m_handlers.Add(handler);
								}
								if (m_parent.m_NNBase.m_isLogging)
								{
									string ThreadCount = "Thread Count after adding client handler:" + Process.GetCurrentProcess().Threads.Count;
									m_parent.log(ThreadCount, isXml: false, "RTMADTQ");
								}
							}
							else
							{
								m_socketForClient.Close();
							}
						}
						else
						{
							Thread.Sleep(MaxConnectWaitTime);
						}
					}
					catch
					{
						if (!m_parent.m_bShuttingDown && !m_ShuttingPort && m_parent.m_NNBase.m_isLogging)
						{
							m_parent.log("Listener port spawn exception", isXml: false, "RTMADTQ");
						}
					}
				}
			}
			catch (InvalidOperationException ex)
			{
				if (m_parent.m_NNBase.m_isLogging)
				{
					m_parent.log("Listener port accept exception: " + ex.Message, isXml: false, "RTMADTQ");
				}
			}
		}
	}

	public class AsynchNetworkClient
	{
		private NetworkStream streamToServer;

		public Protocol m_theProtocol;

		private string serverName;

		private IPAddress ipa;

		public AsynchNetworkClient(PortParams par)
		{
			try
			{
				TalkSocket ClientTalkSocket = new TalkSocket(lingerenable: true, 1, 30000uL, 1000uL);
				serverName = par.remoteHostName;
				TcpClient tcpSocket;
				if (serverName != null && serverName.Length > 0)
				{
					string remoteport = serverName + ":" + par.remotePort;
					Console.WriteLine("Connecting to " + remoteport);
					if (par.parent.m_NNBase.m_isLogging)
					{
						par.parent.log("Connecting to " + remoteport, isXml: false, "RTMADTQ");
					}
					tcpSocket = new TcpClient(serverName, par.remotePort);
					par.parent.ConnectedToADTFeeder = true;
				}
				else
				{
					ipa = new IPAddress(par.ipAddress);
					string sipa = ipa.ToString();
					IPHostEntry ipHostInfo = Dns.GetHostEntry(sipa);
					serverName = ipHostInfo.HostName;
					if (serverName != null && serverName.Length > 0 && serverName != sipa)
					{
						string remoteport2 = serverName + ":" + par.remotePort;
						Console.WriteLine("Connecting to " + remoteport2);
						if (par.parent.m_NNBase.m_isLogging)
						{
							par.parent.log("Connecting to " + remoteport2, isXml: false, "RTMADTQ");
						}
						tcpSocket = new TcpClient(serverName, par.remotePort);
					}
					else
					{
						tcpSocket = new TcpClient();
						IPEndPoint ipRemoteEndPoint = new IPEndPoint(ipa, par.remotePort);
						string remoteport3 = sipa + ":" + par.remotePort;
						Console.WriteLine("Connecting to " + remoteport3);
						if (par.parent.m_NNBase.m_isLogging)
						{
							par.parent.log("Connecting to " + remoteport3, isXml: false, "RTMADTQ");
						}
						tcpSocket.Connect(ipRemoteEndPoint);
					}
				}
				ClientTalkSocket.theSocket = tcpSocket.Client;
				ClientTalkSocket.Init();
				streamToServer = tcpSocket.GetStream();
				IPEndPoint iLocEndPoint = (IPEndPoint)tcpSocket.Client.LocalEndPoint;
				par.portNum = iLocEndPoint.Port;
				string protocol;
				if ((protocol = par.protocol) != null && protocol == "HL7")
				{
					m_theProtocol = new HL7Protocol(ref streamToServer, par.remotePort, par.portType, par.do_logging, par.parent, null, par.portNum);
				}
			}
			catch (Exception ex)
			{
				string sError = "Connect to server port " + par.remotePort + " failed";
				par.parent.m_NNBase.ReportErrorNoDB(sError, "E", "attempting to connect to ADT server", "AsynchNetworkClient", ex.Message);
				par.parent.m_NNBase.SendEventReport("Connection Refused", "ADT Feed");
			}
		}

		public void Run()
		{
		}

		public bool IsAlive()
		{
			return m_theProtocol != null && m_theProtocol.IsAlive();
		}

		public bool IsConnected()
		{
			return streamToServer != null && streamToServer.CanWrite;
		}

		public bool Notify(int cd)
		{
			if (m_theProtocol != null)
			{
				m_theProtocol.ProcessNotify(cd);
			}
			bool retVal = false;
			switch (cd)
			{
			case -1:
				if (streamToServer != null)
				{
					streamToServer.Close();
				}
				break;
			case 0:
				if (streamToServer == null || !streamToServer.CanWrite)
				{
					retVal = true;
				}
				break;
			}
			return retVal;
		}
	}

	public PortParams m_par;

	private bool m_isSpawned;

	private bool m_isInvalid;

	public CommType m_CommType;

	public bool m_isRunning;

	public DateTime m_ConfigTime;

	public bool IsInvalid
	{
		get
		{
			return m_isInvalid;
		}
		set
		{
			m_isInvalid = value;
		}
	}

	public bool IsSpawned
	{
		get
		{
			return m_isSpawned;
		}
		set
		{
			m_isSpawned = value;
		}
	}

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

	public Port(PortParams par, DateTime ConfigTime)
	{
		m_par = par;
		m_ConfigTime = ConfigTime;
		m_isSpawned = false;
		m_isInvalid = false;
		m_isRunning = false;
		string commProtocol;
		if ((commProtocol = m_par.commProtocol) != null && commProtocol == "TCPIP")
		{
			if (par.connectRemote == 0)
			{
				m_CommType = new SocketServerCommType(m_par);
			}
			else
			{
				m_CommType = new SocketClientCommType(m_par);
			}
		}
	}

	public bool Run()
	{
		return m_isRunning = m_CommType.Run();
	}
}
