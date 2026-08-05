using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class LocationNurse
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	public int LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	[Index]
	public int NurseId { get; set; }

	[ForeignKey("NurseId")]
	public virtual Nurse Nurse { get; set; }

	public Guid? DmsId { get; set; }
}
