using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Model;

public class ReagentModel
{
	public int Id { get; set; }

	public string LotNum { get; set; }

	[Index]
	public int LotType { get; set; }

	public decimal? High { get; set; }

	public decimal? Low { get; set; }

	public DateTime Expiration { get; set; }

	public virtual IList<ReagentGroup> Groups { get; set; }

	public virtual IList<LocationReagent> LocationReagents { get; set; }
}
