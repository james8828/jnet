using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class LocationDiagcode
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	public int LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	[Index]
	public int DiagcodeId { get; set; }

	[ForeignKey("DiagcodeId")]
	public virtual Diagcode Diagcode { get; set; }

	public int? DmsId { get; set; }
}
