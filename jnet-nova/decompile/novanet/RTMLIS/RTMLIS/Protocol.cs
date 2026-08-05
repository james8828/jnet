namespace RTMLIS;

public abstract class Protocol
{
	public bool m_isShutDown;

	public bool m_isShuttingDown;

	public bool m_stopping;

	public abstract bool IsAlive();

	public abstract bool ProcessNotify(int cd);
}
