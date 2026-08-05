using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class SampleDataBus
{
	private readonly NovaDbContext DbContext;

	public SampleDataBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
		dbContext.Database.CommandTimeout = 180;
	}

	public int GetSampleDatasAll()
	{
		int result = 0;
		IQueryable<SampleData> source = from e in DbContext.Set<SampleData>().AsNoTracking()
			where e.Id > 0 && e.ObsType == 1
			select e;
		if (source.Count() > 0)
		{
			result = source.Count();
		}
		return result;
	}

	public int GetQCDatasAll()
	{
		int result = 0;
		IQueryable<SampleData> source = from e in DbContext.Set<SampleData>().AsNoTracking()
			where e.Id > 0 && e.ObsType == 2
			select e;
		if (source.Count() > 0)
		{
			result = source.Count();
		}
		return result;
	}

	public Task<List<object>> GetSampleDatasByPage(string patientId, string lname, string dname, string startDate, string endDate, string bedNo, string diagName, string serialNo, List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		var source = from p in DbContext.Set<SampleData>().AsNoTracking()
			join d in DbContext.Diagcodes on p.Diagcode equals d.Code into left4
			from d1 in left4.DefaultIfEmpty()
			join n in DbContext.Nurses on p.NurseCode equals n.Code into left5
			from n1 in left5.DefaultIfEmpty()
			join j in DbContext.Patients on p.PatientId equals j.PatientId into left1
			from b1 in left1.DefaultIfEmpty()
			join k in DbContext.Patients on p.PatientId equals k.MedicalRecord into left2
			from b2 in left2.DefaultIfEmpty()
			join l in DbContext.Patients on p.PatientId equals l.Account into left3
			from b3 in left3.DefaultIfEmpty()
			where p.ObsType == 1
			select new
			{
				Id = p.Id,
				PatientId = p.PatientId,
				NurseCode = p.NurseCode,
				Hospital = p.Hospital,
				Depart = p.Depart,
				DiagcodeName = ((d1 == null) ? "" : d1.Description),
				Reuslt = p.Reuslt,
				ObsStatus = p.ObsStatus,
				PatientName = ((b1 == null) ? ((b2 == null) ? ((b3 == null) ? "" : b3.Name) : b2.Name) : b1.Name),
				NurseName = ((n1 == null) ? "" : n1.Name),
				BedNo = ((b1 == null) ? ((b2 == null) ? ((b3 == null) ? "" : b3.BedNo) : b2.BedNo) : b1.BedNo),
				ObsTime = p.ObsTime,
				SerialNo = ((p.SerialNo == null) ? "" : p.SerialNo),
				Interpretation = p.Interpretation
			};
		if (patientId.Trim(' ') != string.Empty && patientId != null)
		{
			string patId = patientId.Trim(' ');
			source = source.Where(e => e.PatientId == patId);
		}
		if (!string.IsNullOrWhiteSpace(lname))
		{
			source = source.Where(e => e.Hospital == lname);
		}
		if (!string.IsNullOrWhiteSpace(dname))
		{
			source = source.Where(e => e.Depart == dname);
		}
		if (!string.IsNullOrWhiteSpace(bedNo))
		{
			source = source.Where(e => e.BedNo == bedNo);
		}
		if (startDate != string.Empty && startDate != null)
		{
			DateTime sdate = DateTime.Parse(startDate + " 00:00");
			source = source.Where(e => e.ObsTime.CompareTo(sdate) >= 0);
		}
		if (endDate != string.Empty && endDate != null)
		{
			DateTime edate = DateTime.Parse(endDate + " 23:59");
			source = source.Where(e => e.ObsTime.CompareTo(edate) <= 0);
		}
		if (!string.IsNullOrWhiteSpace(diagName))
		{
			source = source.Where(e => e.DiagcodeName == diagName);
		}
		if (!string.IsNullOrWhiteSpace(serialNo))
		{
			source = source.Where(e => e.SerialNo == serialNo);
		}
		if (sorts != null)
		{
			string text = "";
			for (int num = 0; num < sorts.Count; num++)
			{
				OrderFieldModel orderFieldModel = sorts.ElementAt(num);
				text = text + orderFieldModel.PropertyName + (orderFieldModel.IsDesc ? " desc" : "") + ",";
			}
			if (!string.IsNullOrWhiteSpace(text))
			{
				text = text.Trim(',');
				source = source.OrderBy(text);
			}
		}
		else
		{
			source = source.OrderByDescending(e => e.ObsTime);
		}
		total = source.Count();
		return Task.FromResult(((IEnumerable<object>)(from p in source.Skip(start).Take(length).ToList()
			select new
			{
				Id = p.Id,
				PatientId = p.PatientId,
				NurseCode = p.NurseCode,
				Hospital = p.Hospital,
				Depart = p.Depart,
				DiagcodeName = p.DiagcodeName,
				Reuslt = ((p.Reuslt < 0m) ? "" : ((p.Interpretation == "N") ? ("<label  style = 'color:blue;'>" + p.Reuslt + "</label>") : ((p.Interpretation == "HH" || p.Interpretation == "H" || p.Interpretation == ">") ? ("<label style = 'color:red;'>" + p.Reuslt + "</label>") : ((p.Interpretation == "LL" || p.Interpretation == "L" || p.Interpretation == "<") ? ("<label style = 'color:GREEN;'>" + p.Reuslt + "</label>") : ("<label style = ''>" + p.Reuslt + "</label>"))))),
				ObsStatus = p.ObsStatus,
				PatientName = p.PatientName,
				NurseName = p.NurseName,
				BedNo = p.BedNo,
				ObsTime = p.ObsTime.ToString("yyyy-MM-dd HH:mm:ss"),
				SerialNo = p.SerialNo,
				Interpretation = p.Interpretation
			})).ToList());
	}

	public Task<List<object>> GetQCDatasByPage(string lname, string dname, string startDate, string endDate, string serialNo, List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		var source = from p in DbContext.Set<SampleData>().AsNoTracking()
			where p.Id > 0 && p.ObsType == 2
			join n in DbContext.Nurses on p.NurseCode equals n.Code into left1
			from e in left1.DefaultIfEmpty()
			select new
			{
				Id = p.Id,
				NurseCode = p.NurseCode,
				Hospital = p.Hospital,
				Depart = p.Depart,
				Reuslt = p.Reuslt,
				NurseName = ((e == null) ? "" : e.Name),
				ObsTime = p.ObsTime,
				SerialNo = ((p.SerialNo == null) ? "" : p.SerialNo),
				RgtLot = p.RgtLot,
				QcLot = p.QcLot,
				ObsStatus = p.ObsStatus,
				Interpretation = p.Interpretation,
				IsPass = ((p.Interpretation == null || p.Interpretation == "null") ? "NA" : ((p.Interpretation == "N") ? ("通过" + p.NormalLimit) : ("不通过" + p.NormalLimit)))
			};
		if (!string.IsNullOrWhiteSpace(lname))
		{
			source = source.Where(e => e.Hospital == lname);
		}
		if (!string.IsNullOrWhiteSpace(dname))
		{
			source = source.Where(e => e.Depart == dname);
		}
		if (startDate != string.Empty && startDate != null)
		{
			DateTime sdate = DateTime.Parse(startDate + " 00:00");
			source = source.Where(e => e.ObsTime.CompareTo(sdate) >= 0);
		}
		if (endDate != string.Empty && endDate != null)
		{
			DateTime edate = DateTime.Parse(endDate + " 23:59");
			source = source.Where(e => e.ObsTime.CompareTo(edate) <= 0);
		}
		if (!string.IsNullOrWhiteSpace(serialNo))
		{
			source = source.Where(e => e.SerialNo == serialNo);
		}
		if (sorts != null)
		{
			string text = "";
			for (int num = 0; num < sorts.Count; num++)
			{
				OrderFieldModel orderFieldModel = sorts.ElementAt(num);
				text = text + orderFieldModel.PropertyName + (orderFieldModel.IsDesc ? " desc" : "") + ",";
			}
			if (!string.IsNullOrWhiteSpace(text))
			{
				text = text.Trim(',');
				source = source.OrderBy(text);
			}
		}
		else
		{
			source = source.OrderByDescending(e => e.ObsTime);
		}
		total = source.Count();
		return Task.FromResult(((IEnumerable<object>)(from p in source.Skip(start).Take(length).ToList()
			select new
			{
				Id = p.Id,
				NurseCode = p.NurseCode,
				Hospital = p.Hospital,
				Depart = p.Depart,
				Reuslt = ((p.Reuslt < 0m) ? "" : ((p.Interpretation == "N") ? ("<label  style = 'color:blue;'>" + p.Reuslt + "</label>") : ((p.Interpretation == "H" || p.Interpretation == "HH" || p.Interpretation == ">") ? ("<label style = 'color:red;'>" + p.Reuslt + "</label>") : ((p.Interpretation == "L" || p.Interpretation == "LL" || p.Interpretation == "<") ? ("<label style = 'color:GREEN;'>" + p.Reuslt + "</label>") : ("<label style = ''>" + p.Reuslt + "</label>"))))),
				NurseName = p.NurseName,
				ObsTime = p.ObsTime.ToString("yyyy-MM-dd HH:mm:ss"),
				SerialNo = p.SerialNo,
				RgtLot = p.RgtLot,
				QcLot = p.QcLot,
				IsPass = p.IsPass,
				ObsStatus = p.ObsStatus
			})).ToList());
	}
}
