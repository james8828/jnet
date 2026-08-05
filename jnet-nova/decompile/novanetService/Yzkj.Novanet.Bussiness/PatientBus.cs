using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class PatientBus
{
	private readonly NovaDbContext DbContext;

	public PatientBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddPatient(PatientModel model)
	{
		Patient entity = new Patient
		{
			PatientId = model.PatientId,
			MedicalRecord = model.MedicalRecord,
			Account = model.Account,
			Name = model.Name,
			Gender = (Gender)model.Gender,
			Birthday = model.Birthday,
			AdmissionDate = model.AdmissionDate,
			BedNo = model.BedNo,
			LocationId = model.LocationId,
			Source = model.Source,
			Status = model.Status,
			SyncStatus = model.SyncStatus,
			WardNo = model.WardNo
		};
		DbContext.Patients.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdatePatient(PatientModel model)
	{
		Patient patient = DbContext.Patients.FirstOrDefault((Patient e) => e.Id == model.Id);
		patient.PatientId = model.PatientId;
		patient.MedicalRecord = model.MedicalRecord;
		patient.Account = model.Account;
		patient.Name = model.Name;
		patient.Gender = (Gender)model.Gender;
		patient.Birthday = model.Birthday;
		patient.AdmissionDate = model.AdmissionDate;
		patient.BedNo = model.BedNo;
		patient.WardNo = model.WardNo;
		await DbContext.SaveChangesAsync();
	}

	public async Task<PatientModel> GetPatientById(int id)
	{
		Patient patient = DbContext.Set<Patient>().AsNoTracking().FirstOrDefault((Patient e) => e.Id == id);
		if (patient == null)
		{
			return null;
		}
		return new PatientModel
		{
			Id = patient.Id,
			PatientId = patient.PatientId,
			MedicalRecord = patient.MedicalRecord,
			Account = patient.Account,
			Name = patient.Name,
			Gender = (int)patient.Gender,
			Birthday = patient.Birthday,
			AdmissionDate = patient.AdmissionDate,
			BedNo = patient.BedNo,
			LocationId = patient.LocationId,
			Source = patient.Source,
			Status = patient.Status,
			SyncStatus = patient.SyncStatus,
			WardNo = patient.WardNo
		};
	}

	public Task<List<PatientModel>> GetPatients(int locationId, List<OrderFieldModel> sorts, int ps, int pi, out int total)
	{
		IQueryable<Patient> source = from e in DbContext.Set<Patient>().Include("Location").Include("Location.Parent")
				.AsNoTracking()
			where !e.IsDelete && e.LocationId == locationId
			select e;
		if (sorts != null)
		{
			string text = "";
			for (int num = 0; num < sorts.Count; num++)
			{
				OrderFieldModel orderFieldModel = sorts.ElementAt(num);
				text = text + orderFieldModel.PropertyName + (orderFieldModel.IsDesc ? " desc" : "") + ",";
			}
			if (!string.IsNullOrEmpty(text))
			{
				text = text.Trim(',');
				source = source.OrderBy(text);
			}
		}
		else
		{
			source = source.OrderByDescending((Patient e) => e.CreateTime);
		}
		total = source.Count();
		return Task.FromResult((from p in source.Skip((pi - 1) * ps).Take(ps)
			select new PatientModel
			{
				Id = p.Id,
				PatientId = p.PatientId,
				Account = p.Account,
				MedicalRecord = p.MedicalRecord,
				Name = p.Name,
				Gender = (int)p.Gender,
				BedNo = p.BedNo,
				Birthday = p.Birthday,
				AdmissionDate = p.AdmissionDate,
				LocationId = p.LocationId,
				DischargeDate = p.DischargeDate,
				Status = p.Status,
				WardNo = p.WardNo
			}).ToList());
	}

	public Task<List<PatientModel>> GetPatientsByPage(int locationId, int status, List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		IQueryable<Patient> source = from e in DbContext.Set<Patient>().Include("Location").Include("Location.Parent")
				.AsNoTracking()
			where !e.IsDelete && e.LocationId == locationId && e.Status == status
			select e;
		if (sorts != null)
		{
			string text = "";
			for (int num = 0; num < sorts.Count; num++)
			{
				OrderFieldModel orderFieldModel = sorts.ElementAt(num);
				text = text + orderFieldModel.PropertyName + (orderFieldModel.IsDesc ? " desc" : "") + ",";
			}
			if (!string.IsNullOrEmpty(text))
			{
				text = text.Trim(',');
				source = source.OrderBy(text);
			}
		}
		else
		{
			source = source.OrderByDescending((Patient e) => e.CreateTime);
		}
		total = source.Count();
		return Task.FromResult((from p in source.Skip(start).Take(length)
			select new PatientModel
			{
				Id = p.Id,
				PatientId = p.PatientId,
				Account = p.Account,
				MedicalRecord = p.MedicalRecord,
				Name = p.Name,
				Gender = (int)p.Gender,
				BedNo = p.BedNo,
				Birthday = p.Birthday,
				AdmissionDate = p.AdmissionDate,
				LocationId = p.LocationId,
				DischargeDate = p.DischargeDate,
				Status = p.Status,
				WardNo = p.WardNo
			}).ToList());
	}

	public Task<List<PatientModel>> GetPatientsAll()
	{
		return Task.FromResult((from l in DbContext.Set<Patient>()
			where !l.IsDelete
			select new PatientModel
			{
				Id = l.Id,
				PatientId = l.PatientId,
				Account = l.Account,
				MedicalRecord = l.MedicalRecord,
				Name = l.Name,
				Gender = (int)l.Gender,
				BedNo = l.BedNo,
				Birthday = l.Birthday,
				AdmissionDate = l.AdmissionDate,
				LocationId = l.LocationId,
				DischargeDate = l.DischargeDate,
				Status = l.Status,
				WardNo = l.WardNo
			}).ToList());
	}

	public async Task DeletePatients(List<int> ids)
	{
		if (ids == null || ids.Count == 0)
		{
			return;
		}
		foreach (Patient item in DbContext.Patients.Where((Patient e) => ids.Contains(e.Id)))
		{
			item.IsDelete = true;
			item.DeleteTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task DischargePatients(List<int> ids)
	{
		if (ids == null || ids.Count == 0)
		{
			return;
		}
		foreach (Patient item in DbContext.Patients.Where((Patient e) => ids.Contains(e.Id)))
		{
			item.DischargeDate = DateTime.Now;
			item.Status = 1;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task DischargePatientsByDepart(int id)
	{
		foreach (Patient item in DbContext.Patients.Where((Patient e) => e.LocationId == id))
		{
			item.DischargeDate = DateTime.Now;
			item.Status = 1;
		}
		await DbContext.SaveChangesAsync();
	}

	public Task<List<PatientModel>> GetSyncPatients(int ps, int pi, out int total)
	{
		IQueryable<Patient> source = from e in DbContext.Set<Patient>().Include("Location").Include("Location.Parent")
				.Include("Location.Preference")
				.AsNoTracking()
			where !e.IsDelete && e.Location.Preference != null && e.Location.Preference.PatientID.HasValue && e.SyncStatus == 0
			select e;
		total = source.Count();
		source = source.Skip((pi - 1) * ps).Take(ps);
		return Task.FromResult(source.Select((Patient p) => new PatientModel
		{
			Id = p.Id,
			PatientId = p.PatientId,
			Account = p.Account,
			MedicalRecord = p.MedicalRecord,
			Name = p.Name,
			Gender = (int)p.Gender,
			BedNo = p.BedNo,
			WardNo = p.WardNo,
			Birthday = p.Birthday,
			LocationName = p.Location.Name,
			HospitalName = p.Location.Parent.Name,
			PatID = p.Location.Preference.PatientID.Value
		}).ToList());
	}

	public async Task UpdatePatientSyncStatus(List<int> ids)
	{
		foreach (Patient item in DbContext.Patients.Where((Patient e) => ids.Contains(e.Id)))
		{
			item.SyncStatus = 1;
			item.SyncTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}
}
