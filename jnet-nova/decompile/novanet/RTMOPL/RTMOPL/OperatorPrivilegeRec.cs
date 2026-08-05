using System;
using NNClass;

namespace RTMOPL;

public class OperatorPrivilegeRec : DBRecStatus
{
	public string m_OperatorNum;

	public string m_insttype;

	public string m_pswd;

	public DateTime m_certstartdate;

	public DateTime m_certenddate;

	public int m_privilege;

	public DateTime m_lastupdatedate;

	public string m_isactive;

	public DateTime m_isactivelastupdatedate;

	public string m_testname;

	public bool m_bPrivRead;

	public void Clear()
	{
		m_OperatorNum = "";
		m_insttype = "";
		m_pswd = "";
		m_certstartdate = DateTime.MinValue;
		m_certenddate = DateTime.MinValue;
		m_privilege = 0;
		m_lastupdatedate = DateTime.MinValue;
		m_isactive = "";
		m_isactivelastupdatedate = DateTime.MinValue;
		m_testname = "";
		m_bPrivRead = false;
	}
}
