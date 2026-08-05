using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Diagcode
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	[MaxLength(16)]
	public string Code { get; set; }

	public string Description { get; set; }

	public virtual IList<DiagcodeGroup> Groups { get; set; }

	public virtual IList<LocationDiagcode> LocationDiagcodes { get; set; }

	public int? DmsId { get; set; }
}
