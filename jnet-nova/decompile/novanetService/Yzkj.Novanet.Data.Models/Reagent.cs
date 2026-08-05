using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Reagent
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	public string LotNum { get; set; }

	[Index]
	public int LotType { get; set; }

	public decimal? High { get; set; }

	public decimal? Low { get; set; }

	[Index]
	public DateTime Expiration { get; set; }

	[Index]
	public DateTime CreateTime { get; set; }

	public virtual IList<ReagentGroup> Groups { get; set; }

	public virtual IList<LocationReagent> LocationReagents { get; set; }

	public Reagent()
	{
		CreateTime = DateTime.Now;
	}
}
