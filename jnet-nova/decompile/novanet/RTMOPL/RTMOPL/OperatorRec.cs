using System;
using NNClass;

namespace RTMOPL;

public class OperatorRec : DBRecStatus
{
	public string m_OperatorNum;

	public string m_SupervisorNum;

	public string m_OperatorID;

	public string m_IsSupervisor;

	public DateTime m_lastupdatedate;

	public DateTime m_adddate;

	public bool m_bOperRead;

	public string m_ContactNum;

	public string m_Lastname;

	public string m_Firstname;

	public string m_Initials;

	public string m_email;

	public bool m_bContRead;

	public void Clear()
	{
		m_OperatorNum = "";
		m_SupervisorNum = "";
		m_OperatorID = "";
		m_IsSupervisor = "";
		m_lastupdatedate = DateTime.MinValue;
		m_bOperRead = false;
		m_ContactNum = "";
		m_Lastname = "";
		m_Firstname = "";
		m_Initials = "";
		m_email = "";
		m_bContRead = false;
	}
}
