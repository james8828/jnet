using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Patient
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	[MaxLength(64)]
	public string PatientId { get; set; }

	[Index]
	[MaxLength(64)]
	public string MedicalRecord { get; set; }

	[Index]
	[MaxLength(64)]
	public string Account { get; set; }

	[Index]
	[MaxLength(16)]
	public string Name { get; set; }

	[Index]
	public Gender Gender { get; set; }

	[Index]
	public DateTime Birthday { get; set; }

	public string IdCard { get; set; }

	[Index]
	[MaxLength(16)]
	public string WardNo { get; set; }

	[Index]
	[MaxLength(16)]
	public string BedNo { get; set; }

	[Index]
	public DateTime AdmissionDate { get; set; }

	[Index]
	public int Status { get; set; }

	[Index]
	public DateTime? DischargeDate { get; set; }

	[Index]
	public int SyncStatus { get; set; }

	[Index]
	public DateTime? SyncTime { get; set; }

	[Index]
	public int Source { get; set; }

	[Index]
	public bool IsDelete { get; set; }

	public DateTime? DeleteTime { get; set; }

	[Index]
	public DateTime CreateTime { get; set; }

	public int LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	public int? NurseId { get; set; }

	[ForeignKey("NurseId")]
	public virtual Nurse Nurse { get; set; }

	public int? DmsId { get; set; }

	public Patient()
	{
		CreateTime = DateTime.Now;
	}
}
