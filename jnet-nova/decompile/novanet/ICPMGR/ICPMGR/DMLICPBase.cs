using System.Threading;
using NNClass;

namespace ICPMGR;

public class DMLICPBase
{
	public LocationList m_LocationList = new LocationList();

	public LocationMessageList m_LocationMessageList = new LocationMessageList();

	public DMLProtocol m_ListCreatorProtocol = new DMLProtocol();

	public bool m_bIsReady;

	private bool m_LocationListLoaded;

	private bool m_MessageListLoaded;

	public bool m_firstTime = true;

	public int DMLICPBaseInit(bool logging, ref NNBase myNNBase)
	{
		if (!m_LocationListLoaded && !ICPMGR.m_bShuttingDown)
		{
			int numrecs = m_LocationList.LoadLocationList(myNNBase);
			if (numrecs > 0)
			{
				m_LocationListLoaded = true;
			}
		}
		if (m_LocationListLoaded && !m_MessageListLoaded && !ICPMGR.m_bShuttingDown)
		{
			m_LocationMessageList.LoadLocationMesssageList(m_LocationList);
			if (!ICPMGR.m_bShuttingDown)
			{
				m_ListCreatorProtocol.InitDMLLists(logging, this, myNNBase.m_db_ver);
				m_MessageListLoaded = true;
			}
		}
		if (m_MessageListLoaded && !ICPMGR.m_bShuttingDown)
		{
			if (m_ListCreatorProtocol == null)
			{
				m_ListCreatorProtocol = new DMLProtocol();
			}
			if (m_ListCreatorProtocol != null && (m_ListCreatorProtocol.m_ProtocolThread == null || m_ListCreatorProtocol.m_ProtocolThread.ThreadState == ThreadState.Unstarted || m_ListCreatorProtocol.m_ProtocolThread.ThreadState == ThreadState.Aborted || m_ListCreatorProtocol.m_ProtocolThread.ThreadState == ThreadState.Stopped || m_ListCreatorProtocol.m_isShutDown) && !ICPMGR.m_bShuttingDown)
			{
				m_ListCreatorProtocol.ProcessNotify(3, "");
			}
		}
		return myNNBase.m_Status;
	}

	public void DMLICPBaseClose()
	{
		if (m_ListCreatorProtocol != null)
		{
			m_ListCreatorProtocol.ProcessNotify(-1, " because the service is shutting down");
		}
	}

	public void ReloadLocations(ref NNBase myNNBase)
	{
		int numrecs = m_LocationList.LoadLocationList(myNNBase);
		if (numrecs > 0)
		{
			m_LocationMessageList.LoadLocationMesssageList(m_LocationList);
		}
	}
}
