using System;
using System.Collections.Generic;
using System.Data.Odbc;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Xml;
using NNClass;

namespace ICPMGR;

public class RCProtocol : Protocol
{
	protected enum RCSTATE
	{
		SEND_REMOTE_COMMAND,
		MAX
	}

	protected NNBase m_NNBase = new NNBase();

	protected Port.AsynchNetworkServer.ClientHandler m_parent;

	private byte[] m_readbuffer;

	private byte[] m_writebuffer;

	private byte[] m_asyncwritebuffer;

	protected string m_message = "";

	protected bool m_isPartial;

	private string m_ReadString = string.Empty;

	private IPAddress m_IP_Address;

	protected string m_serial_id = "";

	protected string m_max_message_sz = "4096";

	private NetworkStream m_networkStream;

	private AsyncCallback callbackWrite;

	private int m_port_num;

	private string m_portType;

	private List<string> m_commandList = new List<string>();

	public override void ProcessNotify(int cd, string rcMessage)
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
			m_pleaseShutDown = true;
			break;
		case 8:
		{
			string[] rcMsg = rcMessage.Split('|');
			string id = rcMsg[0];
			lock (Port.AsynchNetworkServer.ServerCommon.m_handlers)
			{
				Port.AsynchNetworkServer.ClientHandler h = Port.AsynchNetworkServer.ServerCommon.m_handlers.GetInstrument(id);
				if (h != null && h.m_theProtocol != null && h.m_theProtocol.SupportRemoteControl() && h.m_theProtocol.IsAliveAndWell())
				{
					h.m_theProtocol.ProcessNotify(cd, rcMsg[1]);
				}
				break;
			}
		}
		}
	}

	public void InitRCProtocol(Port.AsynchNetworkServer.ClientHandler parent, ref NetworkStream networkStream, int port_num, string portType, string from_inst_id, bool logging, DMLICPBase myDMLICPBase, string db_ver)
	{
		m_parent = parent;
		m_networkStream = networkStream;
		m_port_num = port_num;
		m_portType = portType;
		m_NNBase.m_bLogging = logging;
		m_NNBase.m_db_ver = db_ver;
		callbackWrite = OnWriteComplete;
		m_IP_Address = m_parent.m_InstrumentIP;
		m_NNBase.m_LogName = Guid.NewGuid().ToString("N");
		FillRCCommandList();
		m_readbuffer = ICPMGR.m_ICPBytesBuffers.GetBigBuffer(32768);
	}

	private void Start()
	{
		m_ProtocolThread = new Thread(ProtocolThread);
		m_ProtocolThread.Start();
	}

	private void OnReadComplete()
	{
		string reason = string.Empty;
		int MessageCount = 0;
		try
		{
			while (!m_isShutDown && !m_isShuttingDown && !m_pleaseShutDown && MessageCount == 0)
			{
				int bytesRead = m_networkStream.Read(m_readbuffer, 0, m_readbuffer.Length);
				if (bytesRead > 0)
				{
					m_ReadString += Encoding.UTF8.GetString(m_readbuffer, 0, bytesRead);
					ProcessMessage();
				}
				else
				{
					reason = "Connection Dropped";
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
			reason = ((!m_pleaseShutDown && !m_stopping) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			ShutDown(reason, "Protocol", bExit: true);
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				bool bret = m_pleaseShutDown || m_stopping;
				m_NNBase.log(bret ? "Was asked to stop" : ex3.Message, isXml: false, "DML");
			}
			if (!m_pleaseShutDown && !m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(ex3, "Reading message(s)", "OnReadComplete", "Protocol");
			}
		}
	}

	private void OnWriteComplete(IAsyncResult ar)
	{
		try
		{
			m_networkStream.EndWrite(ar);
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("OnWriteComplete");
		}
		catch (IOException ex2)
		{
			string reason = ((!m_pleaseShutDown && !m_stopping) ? ("Connection Dropped - IOException: " + ex2.Message) : "Shutdown requested");
			ShutDown(reason, "OnWriteComplete", bExit: false);
		}
		catch (Exception ex3)
		{
			if (m_NNBase.m_isLogging)
			{
				bool bret = m_pleaseShutDown || m_stopping;
				m_NNBase.log(bret ? "Was asked to stop" : ex3.Message, isXml: false, "DML");
			}
			if (!m_pleaseShutDown && !m_stopping && !m_isShutDown && !m_isShuttingDown)
			{
				handleException(ex3, "writing message(s)", "OnWriteComplete", "OnWriteComplete");
			}
		}
	}

	private void ProcessMessage()
	{
		string retMsg = string.Empty;
		bool isInvalid = true;
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log(m_ReadString, isXml: false, "RCClient");
		}
		if (!string.IsNullOrEmpty(m_ReadString))
		{
			string[] rcMsg = m_ReadString.Split('^');
			if (rcMsg.Length == 6 && rcMsg[0].CompareTo(Date_code_pw()) == 0 && rcMsg[1].ToUpper().CompareTo("RC") == 0 && m_commandList.Contains(rcMsg[3]) && Port.AsynchNetworkServer.ServerCommon.m_handlers.GetInstrument(rcMsg[2]) != null)
			{
				string rcMessage = rcMsg[2] + "|" + m_ReadString;
				StartRC(rcMessage);
				isInvalid = false;
			}
		}
		if (isInvalid)
		{
			retMsg = "Invalid RC command: " + m_ReadString;
			SendString(retMsg);
		}
		else
		{
			retMsg = "OK";
			SendString(retMsg);
		}
		m_ReadString = string.Empty;
	}

	private string Date_code_pw()
	{
		DateTime today = DateTime.UtcNow;
		int currentDay = today.Day;
		int currentMonth = today.Month;
		int currentYear = today.Year;
		return ((currentMonth * 31 + currentDay * currentDay) * 137 + ((currentYear - 2000) * 16 + 5987)).ToString();
	}

	private void StartRC(string rcCommand)
	{
		ProcessNotify(8, rcCommand);
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
					m_NNBase.log("ShutDown called because " + reason, isXml: false, whoFrom);
					string ThreadCount = "Thread Count before shutdown:" + Process.GetCurrentProcess().Threads.Count;
					m_NNBase.log(ThreadCount, isXml: false, "Command-RC");
				}
				try
				{
					string sport = m_parent.socket.Handle.ToString();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closing client port " + sport + " for remote control " + m_serial_id, isXml: false, "Command-RC");
					}
					m_parent.socket.Close();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closed client port " + sport + " for remote control " + m_serial_id, isXml: false, "Command-RC");
					}
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closing networkStream", isXml: false, "Command-RC");
					}
					m_networkStream.Close();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Closed networkStream", isXml: false, "Command-RC");
					}
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Disposing networkStream", isXml: false, "Command-RC");
					}
					m_networkStream.Dispose();
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.log("Disposed networkStream", isXml: false, "Command-RC");
					}
				}
				catch
				{
				}
				shutdownstep = "UIConnection";
				if (m_ProtocolThread != null && m_ProtocolThread.IsAlive && Thread.CurrentThread.ManagedThreadId != m_ProtocolThread.ManagedThreadId)
				{
					try
					{
						ShutDownProtocol();
					}
					catch
					{
					}
				}
				shutdownstep = "protocol";
				try
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
				}
				catch
				{
				}
				shutdownstep = "releasebuffer";
				try
				{
					if (m_NNBase.m_isLogging)
					{
						string ThreadCount2 = "Thread Count before completing shutdown:" + Process.GetCurrentProcess().Threads.Count;
						m_NNBase.log(ThreadCount2, isXml: false, "Command-RC");
					}
					if (m_NNBase.m_isLogging)
					{
						m_NNBase.StopLogging();
					}
				}
				catch
				{
				}
				shutdownstep = "logging";
				try
				{
					GC.Collect();
				}
				catch
				{
				}
				shutdownstep = "garbagecollection";
			}
			catch (ThreadAbortException)
			{
				m_NNBase.ForceLogging("ShutdownException");
				m_NNBase.log("Shutdown aborted. Last completed step = " + shutdownstep, isXml: false, "Command-RC");
				try
				{
					ICPMGR.m_ICPBytesBuffers.ReleaseBigBuffer(ref m_readbuffer);
					m_readbuffer = null;
					m_NNBase.log("Released read buffer", isXml: false, "dml");
				}
				catch
				{
				}
				m_NNBase.StopLogging();
			}
			catch (Exception)
			{
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

	private bool OpenDBConnection(ref OdbcConnection myConnection, ref OdbcCommand myReadCommand, ref OdbcCommand myWriteCommand, int iTries, ref bool bDBAvailable, string whoFrom)
	{
		bDBAvailable = m_NNBase.OpenDBConnection(ref myConnection, ref myReadCommand, ref myWriteCommand, iTries);
		if (!bDBAvailable)
		{
			ShutDown("Cannot connect to database", whoFrom, whoFrom == "Protocol");
		}
		return bDBAvailable;
	}

	private void ShutDownProtocol()
	{
		if (m_ProtocolThread != null)
		{
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closing Protocol", isXml: false, "Command-RC");
			}
			m_ProtocolThread.Abort();
			m_ProtocolThread.Join();
			if (m_NNBase.m_isLogging)
			{
				m_NNBase.log("Closed Protocol", isXml: false, "Command-RC");
			}
		}
		else if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("m_ProtocolThread is null", isXml: false, "Command-RC");
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
				if (!bDBDisconnect)
				{
					details = details + " " + e.StackTrace.ToString();
				}
				if (!bDBDisconnect)
				{
					m_NNBase.ForceLogging("Exception");
				}
				m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : ("Exception " + e.GetType().ToString()), bDBDisconnect ? "E" : "C", when, from, details);
			}
			exceptionStr = "Exception";
		}
		if (whoFrom == "Protocol")
		{
			ShutDown(exceptionStr, "Protocol", bExit: true);
		}
	}

	private void handleXMLException(XmlException e, string when, string from)
	{
		if (!m_isShuttingDown)
		{
			string details = e.Message.ToString() + " at line: " + e.LineNumber + " " + e.StackTrace.ToString();
			m_NNBase.ForceLogging("XMLException");
			m_NNBase.ReportErrorDB("XML Exception " + e.GetType().ToString(), "C", when, from, details);
			ShutDown("XML Exception", "Protocol", bExit: true);
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
		}
		m_NNBase.ReportErrorDB(bDBDisconnect ? "The database connection has been lost" : "DB Exception", bDBDisconnect ? "E" : "C", when, from, details);
		if (whoFrom == "Protocol")
		{
			ShutDown(bDBDisconnect ? "The database connection has been lost" : "DB Exception", "Protocol", bExit: true);
		}
	}

	public int SendString(string input, bool isPartial, bool trunc)
	{
		bool getout = false;
		int i = 0;
		lock (this)
		{
		}
		if (!getout)
		{
			m_writebuffer = Encoding.UTF8.GetBytes(input);
			i = m_writebuffer.Length;
			if (trunc && input.Length > 256)
			{
				if (m_NNBase.m_isLogging)
				{
					m_NNBase.log(input.Substring(0, 256) + "..." + input.Substring(input.Length - 64), isXml: true, isPartial ? "ICPMGR..." : "ICPMGR   ");
				}
			}
			else if (m_NNBase.m_isLogging)
			{
				m_NNBase.log(input, isXml: true, isPartial ? "ICPMGR..." : "ICPMGR   ");
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
			lock (this)
			{
			}
		}
		return i;
	}

	public int SendString(string input)
	{
		m_asyncwritebuffer = Encoding.UTF8.GetBytes(input);
		int i = m_asyncwritebuffer.Length;
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
				ShutDown("Cannot write", "Protocol", bExit: false);
			}
		}
		catch (ThreadAbortException)
		{
			handleThreadAbortException("Protocol");
		}
		catch (Exception ex2)
		{
			ShutDown("BeginWrite failed(" + ex2.Message + ")", "Protocol", bExit: true);
		}
		return i;
	}

	private void ProtocolThread()
	{
		m_NNBase.m_LogName = "RC_";
		m_NNBase.NNBaseOpen(m_NNBase.m_bLogging, m_NNBase.m_LogName, "ICPMGR", "ICP");
		if (m_NNBase.m_isLogging)
		{
			m_NNBase.log("Connection established via local port " + m_port_num, isXml: false, "RCProtocol");
		}
		try
		{
			while (!m_isShutDown && !m_isShuttingDown && !m_pleaseShutDown)
			{
				OnReadComplete();
			}
			if (m_pleaseShutDown && !m_isShutDown && !m_isShuttingDown)
			{
				ShutDown("Shutdown requested", "RCProtocol", bExit: true);
			}
		}
		catch (ThreadAbortException)
		{
			if (!m_isShutDown && !m_isShuttingDown)
			{
				ShutDown("Protocol thread aborted", "Protocol", bExit: true);
			}
		}
	}

	private void FillRCCommandList()
	{
		m_commandList.Add("EXEC_SEQ_ABG_2PT_CAL");
		m_commandList.Add("EXEC_SEQ_COOX_CAL");
		m_commandList.Add("EXEC_SEQ_COOX_DEPRO");
		m_commandList.Add("EXEC_SEQ_SO2_CAL");
		m_commandList.Add("EXEC_SEQ_QC");
		m_commandList.Add("EXEC_SEQ_FOR_PRIME");
	}
}
