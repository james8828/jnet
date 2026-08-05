using System;

namespace Yzkj.Novanet.Bussiness.Model;

public class SampleDataModel
{
	public long Id { get; set; }

	public string PatientId { get; set; }

	public string NurseCode { get; set; }

	public string Hospital { get; set; }

	public string Depart { get; set; }

	public string Diagcode { get; set; }

	public decimal Reuslt { get; set; }

	public string Unit { get; set; }

	public string ObsStatus { get; set; }

	public string Interpretation { get; set; }

	public string NormalLimit { get; set; }

	public string CriticalLimit { get; set; }

	public string RgtLot { get; set; }

	public DateTime ObsTime { get; set; }

	public DateTime CreateTime { get; set; }

	public string SerialNo { get; set; }

	public string DeviceId { get; set; }

	public int ObsType { get; set; }

	public int QcLevel { get; set; }

	public string QcLot { get; set; }

	public bool Exist { get; set; }
}
