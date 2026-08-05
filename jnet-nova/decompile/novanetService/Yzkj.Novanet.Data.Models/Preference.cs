using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Preference
{
	[Key]
	[DatabaseGenerated(DatabaseGeneratedOption.None)]
	[ForeignKey("Location")]
	public int Id { get; set; }

	public bool AutoReConnect { get; set; }

	public int? CycleMinutes { get; set; }

	public int? PatientID { get; set; }

	public virtual Location Location { get; set; }

	public DateTime CreateTime { get; set; }

	public DateTime? UpdateTime { get; set; }

	public bool Synced { get; set; }

	public Preference()
	{
		CreateTime = DateTime.Now;
		Synced = false;
	}
}
