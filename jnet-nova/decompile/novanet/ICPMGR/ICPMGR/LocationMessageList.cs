using System;
using System.Globalization;
using NNClass;
using Patient;

namespace ICPMGR;

public class LocationMessageList : CTList
{
	protected CompareInfo Comp = CompareInfo.GetCompareInfo("en-US");

	protected CompareOptions CompOpt = CompareOptions.IgnoreCase;

	private DateTime AsOf;

	private object LockObj = new object();

	protected int GetElement()
	{
		int pElem = IndexGetElement();
		if (IsNew(pElem))
		{
			LocationMessageRec NewElem = new LocationMessageRec();
			m_Array.Add(NewElem);
		}
		return pElem;
	}

	public void LoadLocationMesssageList(LocationList m_LocationList)
	{
		int numrecs = 0;
		string locnum = "";
		int pLocationMessages = -1;
		lock (this)
		{
			if (m_LocationList.GetNumUsedElements() <= 0)
			{
				return;
			}
			for (pLocationMessages = First(); pLocationMessages >= 0; pLocationMessages = Next())
			{
				LocationMessageRec plocationmessage = (LocationMessageRec)m_Array[pLocationMessages];
				plocationmessage.m_status = 2;
			}
			DateTime newAsOf = DateTime.Now;
			LocationRec plocation = null;
			for (plocation = m_LocationList.FirstLocation(); plocation != null; plocation = m_LocationList.NextLocation())
			{
				locnum = plocation.get_m_loc_num();
				LocationMessageRec plocationmessage;
				if ((pLocationMessages = LookupLocNum(locnum)) < 0)
				{
					int pnewlocationmessage = GetElement();
					plocationmessage = (LocationMessageRec)m_Array[pnewlocationmessage];
					plocationmessage.Clear();
					plocationmessage.m_loc_num = locnum;
					plocationmessage.m_status = plocation.GetStatus();
					Append(pnewlocationmessage);
				}
				else
				{
					plocationmessage = (LocationMessageRec)m_Array[pLocationMessages];
					plocationmessage.m_status = plocation.GetStatus();
				}
				if (plocationmessage.m_status == 1)
				{
					numrecs++;
				}
			}
			if (numrecs <= 0)
			{
				return;
			}
			for (pLocationMessages = First(); pLocationMessages >= 0; pLocationMessages = Next())
			{
				LocationMessageRec plocationmessage = (LocationMessageRec)m_Array[pLocationMessages];
				int istatus = plocationmessage.m_status;
				if (istatus == 2)
				{
					plocationmessage.m_status = 3;
				}
			}
			AsOf = newAsOf;
		}
	}

	private int LookupLocNum(string m_loc_num)
	{
		bool bFound = false;
		int pLocationMessages = -1;
		if (GetNumUsedElements() > 0)
		{
			pLocationMessages = First();
			while (pLocationMessages >= 0 && !bFound)
			{
				LocationMessageRec plocationmessage = (LocationMessageRec)m_Array[pLocationMessages];
				int iStatus = plocationmessage.m_status;
				if (iStatus != 3 && Comp.Compare(plocationmessage.m_loc_num, m_loc_num, CompOpt) == 0)
				{
					bFound = true;
				}
				else
				{
					pLocationMessages = Next();
				}
			}
		}
		return pLocationMessages;
	}

	public bool GetPatientList(string m_loc_num, ref PatientList CompletePatientList, bool bPrev, ref DateTime AsOf)
	{
		CompletePatientList = null;
		LocationMessageRec LocationMessage = null;
		int pLocationMessages = -1;
		bool bOK = false;
		lock (LockObj)
		{
			if ((pLocationMessages = LookupLocNum(m_loc_num)) >= 0)
			{
				LocationMessage = (LocationMessageRec)m_Array[pLocationMessages];
				LocationMessage.GetPatientList(ref CompletePatientList, bPrev, ref AsOf);
				bOK = CompletePatientList != null;
			}
		}
		return bOK;
	}

	public LocationMessageRec SetCurrentPatientList(string m_loc_num, PatientList CompletePatientList, DateTime AsOf)
	{
		LocationMessageRec LocationMessage = null;
		int pLocationMessages = -1;
		lock (LockObj)
		{
			if ((pLocationMessages = LookupLocNum(m_loc_num)) >= 0)
			{
				LocationMessage = (LocationMessageRec)m_Array[pLocationMessages];
				LocationMessage.SetPatientList(CompletePatientList, AsOf);
			}
		}
		return LocationMessage;
	}
}
