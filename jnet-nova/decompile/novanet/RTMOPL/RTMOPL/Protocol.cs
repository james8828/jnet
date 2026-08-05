namespace RTMOPL;

public abstract class Protocol
{
	public string theConnection;

	public bool m_isShutDown;

	public bool m_isShuttingDown;

	public bool m_stopping;

	public abstract void ProcessMessage();

	public abstract bool IsAlive();

	public abstract bool ProcessNotify(int cd);
}
