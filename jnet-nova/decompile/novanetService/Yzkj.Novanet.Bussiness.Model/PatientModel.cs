using System;
using Yzkj.Novanet.Utility;

namespace Yzkj.Novanet.Bussiness.Model;

public class PatientModel
{
	public int Id { get; set; }

	public string PatientId { get; set; }

	public string MedicalRecord { get; set; }

	public string Account { get; set; }

	public string Name { get; set; }

	public int Gender { get; set; }

	public DateTime Birthday { get; set; }

	public string IdCard { get; set; }

	public string WardNo { get; set; }

	public string BedNo { get; set; }

	public DateTime AdmissionDate { get; set; }

	public int Status { get; set; }

	public DateTime? DischargeDate { get; set; }

	public int SyncStatus { get; set; }

	public DateTime? SyncTime { get; set; }

	public int Source { get; set; }

	public bool IsDelete { get; set; }

	public DateTime? DeleteTime { get; set; }

	public DateTime CreateTime { get; set; }

	public int LocationId { get; set; }

	public string LocationName { get; set; }

	public string HospitalName { get; set; }

	public int PatID { get; set; }

	public int Age => AgeHelper.CalcAge(Birthday, DateTime.Today);
}
