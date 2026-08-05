using System;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class SampleData
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
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

	[Index]
	[MaxLength(32)]
	public string SerialNo { get; set; }

	[Index]
	[MaxLength(32)]
	public string DeviceId { get; set; }

	[DefaultValue(0)]
	public int state { get; set; }

	[DefaultValue(1)]
	public int ObsType { get; set; }

	public int QcLevel { get; set; }

	public string QcLot { get; set; }

	public SampleData()
	{
		CreateTime = DateTime.Now;
	}
}
