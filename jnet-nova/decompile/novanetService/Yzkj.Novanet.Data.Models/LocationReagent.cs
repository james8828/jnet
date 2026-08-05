using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class LocationReagent
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	public int LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	[Index]
	public int ReagentId { get; set; }

	[ForeignKey("ReagentId")]
	public virtual Reagent Reagent { get; set; }
}
