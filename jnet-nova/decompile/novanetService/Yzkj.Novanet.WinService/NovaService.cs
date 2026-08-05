using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Threading;
using NLog;

namespace Yzkj.Novanet.WinService;

public class NovaService
{
	private readonly LoggerWrap Logger;

	private Socket serverSocket;

	private Thread listenThread;

	private volatile bool stoped;

	private volatile bool paused;

	private ManualResetEvent resumeEvent = new ManualResetEvent(initialState: false);

	public NovaService()
	{
		Logger = new LoggerWrap(LogManager.GetCurrentClassLogger());
	}

	public void Start(string _host, int _port)
	{
		try
		{
			IPAddress address = IPAddress.Parse(_host);
			serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
			serverSocket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, optionValue: true);
			serverSocket.Bind(new IPEndPoint(address, _port));
			serverSocket.Listen(10);
			Logger.Info($"v1.2.1 启动监听{serverSocket.LocalEndPoint}成功。");
			listenThread = new Thread(ListenClientConnect);
			listenThread.IsBackground = true;
			listenThread.Start();
		}
		catch (Exception ex)
		{
			Logger.Error(ex, ex.Message);
			throw ex;
		}
	}

	public void Pause()
	{
		resumeEvent.Reset();
		paused = true;
	}

	public void Resume()
	{
		paused = false;
		resumeEvent.Set();
	}

	public void Stop()
	{
		try
		{
			stoped = true;
			if (serverSocket != null)
			{
				serverSocket.Close();
			}
			if (listenThread != null)
			{
				listenThread.Join(1000);
				listenThread.Abort();
			}
		}
		catch (Exception ex)
		{
			Logger.Error(ex, ex.Message);
		}
	}

	private void ListenClientConnect()
	{
		while (!stoped)
		{
			if (paused)
			{
				resumeEvent.WaitOne();
			}
			try
			{
				Socket socket = serverSocket.Accept();
				Logger.Info($"设备[{socket.RemoteEndPoint}]接入。");
				socket.SendBufferSize = 32768;
				socket.ReceiveBufferSize = 32768;
				socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, optionValue: true);
				Thread thread = new Thread(new NovaMessageHandler(socket).ReceiveMessage);
				thread.IsBackground = true;
				thread.Start();
				Logger.Info($"当前线程数[{Process.GetCurrentProcess().Threads.Count}].");
			}
			catch (Exception ex)
			{
				Logger.Error(ex, "nova解析服务启动监听失败" + ex.Message);
			}
		}
	}

	private byte[] GetKeepAliveData()
	{
		uint structure = 0u;
		byte[] array = new byte[Marshal.SizeOf(structure) * 3];
		BitConverter.GetBytes(1u).CopyTo(array, 0);
		BitConverter.GetBytes(3000u).CopyTo(array, Marshal.SizeOf(structure));
		BitConverter.GetBytes(500u).CopyTo(array, Marshal.SizeOf(structure) * 2);
		return array;
	}
}
