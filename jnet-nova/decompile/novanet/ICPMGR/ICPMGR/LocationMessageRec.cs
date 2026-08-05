using System;
using Patient;

namespace ICPMGR;

public class LocationMessageRec
{
	public string m_loc_num;

	public int m_status;

	protected PatientListsRec PatientLists1;

	protected PatientListsRec PatientLists2;

	protected int CurrentPatientLists;

	protected object CurrentPatientListsLock = new object();

	public int GetCurrentListNum()
	{
		return CurrentPatientLists;
	}

	public void Clear()
	{
		m_loc_num = "";
		m_status = 0;
		PatientLists1 = new PatientListsRec();
		PatientLists2 = new PatientListsRec();
		CurrentPatientLists = 0;
	}

	public void GetPatientList(ref PatientList CompletePatientList, bool bPrev, ref DateTime AsOf)
	{
		CompletePatientList = null;
		if ((CurrentPatientLists == 1 && !bPrev) || (CurrentPatientLists == 2 && bPrev))
		{
			PatientLists1.GetCompletePatientList(ref AsOf, ref CompletePatientList);
		}
		else if ((CurrentPatientLists == 2 && !bPrev) || (CurrentPatientLists == 1 && bPrev))
		{
			PatientLists2.GetCompletePatientList(ref AsOf, ref CompletePatientList);
		}
	}

	public void SetPatientList(PatientList CompletePatientList, DateTime AsOf)
	{
		switch (CurrentPatientLists)
		{
		case 0:
		case 2:
			PatientLists1.SetCompletePatientList(CompletePatientList, AsOf);
			CurrentPatientLists = 1;
			break;
		case 1:
			PatientLists2.SetCompletePatientList(CompletePatientList, AsOf);
			CurrentPatientLists = 2;
			break;
		}
	}
}
