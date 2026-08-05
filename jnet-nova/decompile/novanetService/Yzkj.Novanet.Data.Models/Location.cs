using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Location
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	[MaxLength(32)]
	public string Name { get; set; }

	[Index]
	public int Level { get; set; }

	public int? ParentId { get; set; }

	[ForeignKey("ParentId")]
	public virtual Location Parent { get; set; }

	public virtual IList<Location> Childs { get; set; }

	public DateTime CreateTime { get; set; }

	public DateTime? UpdateTime { get; set; }

	public bool IsDeleted { get; set; }

	public DateTime? DeleteTime { get; set; }

	public virtual IList<Patient> Patients { get; set; }

	public virtual IList<LocationNurse> LocationNurses { get; set; }

	public virtual IList<LocationDiagcode> LocationDiagcodes { get; set; }

	public virtual IList<LocationReagent> LocationReagents { get; set; }

	public virtual TestRange TestRange { get; set; }

	public virtual Preference Preference { get; set; }

	public DateTime? ST_Location { get; set; }

	public DateTime? ST_Setup { get; set; }

	public DateTime? ST_Nurse { get; set; }

	public DateTime? ST_Patient { get; set; }

	public DateTime? ST_Reagent { get; set; }

	public int? DmsId { get; set; }

	public Location()
	{
		CreateTime = DateTime.Now;
	}
}
