using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Nurse
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	[MaxLength(16)]
	public string Code { get; set; }

	public string Name { get; set; }

	[Index]
	[MaxLength(64)]
	public string hiskey { get; set; }

	[Index]
	public int SyncStatus { get; set; }

	[Index]
	public DateTime? SyncTime { get; set; }

	public virtual IList<LocationNurse> LocationNurses { get; set; }

	public virtual IList<Patient> Patients { get; set; }

	[Index]
	public bool IsDelete { get; set; }

	public DateTime? DeleteTime { get; set; }

	[Index]
	public DateTime CreateTime { get; set; }

	[Index]
	public DateTime? UpdateTime { get; set; }

	public Guid? DmsId { get; set; }

	public Nurse()
	{
		CreateTime = DateTime.Now;
	}
}
