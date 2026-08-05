using System;
using NNClass;

namespace RTMOPL;

public class OperatorToUnitRec : DBRecStatus
{
	public string m_OperatorNum;

	public string m_locnum;

	public DateTime m_lastupdatedate;

	public string m_isactive;

	public DateTime m_isactivelastupdatedate;

	public bool m_bUnitRead;

	public void Clear()
	{
		m_OperatorNum = "";
		m_locnum = "";
		m_lastupdatedate = DateTime.MinValue;
		m_isactive = "";
		m_isactivelastupdatedate = DateTime.MinValue;
		m_bUnitRead = false;
	}
}
