using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class DiagcodeGroup
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	public string Name { get; set; }

	public virtual IList<Diagcode> Diagcodes { get; set; }
}
