using NNClass;

namespace RTMOPL;

public class MethodRec : DBRecStatus
{
	public string m_OperatorNum;

	public string m_insttype;

	public string m_methodcd;

	public void Clear()
	{
		m_OperatorNum = "";
		m_insttype = "";
		m_methodcd = "";
	}
}
