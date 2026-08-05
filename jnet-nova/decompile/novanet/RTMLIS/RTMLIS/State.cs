using System.Collections;

namespace RTMLIS;

internal class State
{
	public bool bGotChar;

	public byte b;

	public int state;

	public ArrayList queryMessage = new ArrayList();

	public ArrayList resultMessage = new ArrayList();

	public string m_sCurMsg = "";

	public string[] recordList;

	public string queryTestList = "";

	public bool bFullMessage;

	public bool bSendingMessage;

	public bool bWaitingForQueryResponse;

	public bool bRetryLastQueryMessage;

	public bool bRetryLastResultMessage;
}
