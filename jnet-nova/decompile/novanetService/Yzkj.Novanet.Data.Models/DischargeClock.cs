using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class DischargeClock
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	public bool IsEnabled { get; set; }

	public int Hour { get; set; }

	public int Minute { get; set; }

	public int LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	public DateTime SaveTime { get; set; }

	public DischargeClock()
	{
		SaveTime = DateTime.Now;
	}
}
