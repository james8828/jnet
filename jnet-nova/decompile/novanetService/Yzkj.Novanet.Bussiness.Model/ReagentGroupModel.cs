using System.Collections.Generic;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Model;

public class ReagentGroupModel
{
	public int Id { get; set; }

	public string Name { get; set; }

	public virtual IList<Reagent> Reagents { get; set; }
}
