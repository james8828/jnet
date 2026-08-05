using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class TestRange
{
	[Key]
	[DatabaseGenerated(DatabaseGeneratedOption.None)]
	[ForeignKey("Location")]
	public int Id { get; set; }

	public decimal? LowCricital { get; set; }

	public decimal? LowNormal { get; set; }

	public decimal? HighNormal { get; set; }

	public decimal? HighCricital { get; set; }

	public Gender Sex { get; set; }

	public int? AgeLow { get; set; }

	public int? AgeHigh { get; set; }

	public virtual Location Location { get; set; }

	public string Remark { get; set; }

	[Index]
	public bool IsDeleted { get; set; }

	public DateTime? DeleteTime { get; set; }

	[Index]
	public DateTime CreateTime { get; set; }

	[Index]
	public DateTime? UpdateTime { get; set; }

	public decimal? SL { get; set; }

	public decimal? IC { get; set; }

	public TestRange()
	{
		CreateTime = DateTime.Now;
		Sex = Gender.All;
	}
}
