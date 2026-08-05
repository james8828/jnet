using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class ReagentGroup
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	public string Name { get; set; }

	public virtual IList<Reagent> Reagents { get; set; }
}
