using System.Threading;
using FlexTimers;

namespace ICPMGR;

public abstract class Protocol
{
	public bool m_isShutDown;

	public bool m_isShuttingDown;

	public bool m_pleaseShutDown;

	public bool m_stopping;

	public string m_ShutdownReason = "";

	public FlexTimer cmTimer;

	public Thread m_ProtocolThread;

	public abstract void ProcessNotify(int cd, string message);

	public void Kill()
	{
		try
		{
			cmTimer.Abort();
		}
		catch
		{
		}
		try
		{
			m_ProtocolThread.Abort();
			m_ProtocolThread.Join();
			m_ProtocolThread = null;
		}
		catch
		{
		}
		try
		{
			m_isShutDown = true;
		}
		catch
		{
		}
	}
}
