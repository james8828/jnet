using System;
using Patient;

namespace ICPMGR;

public class PatientListsRec
{
	protected PatientList CompletePatientList = new PatientList();

	protected DateTime AsOf;

	public void GetCompletePatientList(ref DateTime AsOfOut, ref PatientList myList)
	{
		AsOfOut = AsOf;
		myList = new PatientList();
		myList.Copy(CompletePatientList);
	}

	public void SetCompletePatientList(PatientList CompleteList, DateTime AsOfIn)
	{
		CompletePatientList.Copy(CompleteList);
		AsOf = AsOfIn;
	}
}
