using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class NovaLog
{
	[Key]
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public long log_id { get; set; }

	public DateTime log_date { get; set; }

	[Index]
	[MaxLength(16)]
	public string log_level { get; set; }

	[Index]
	[MaxLength(8)]
	public string log_source { get; set; }

	public string log_message { get; set; }

	public string log_machine_name { get; set; }

	[Index]
	[MaxLength(32)]
	public string log_user_name { get; set; }

	public string log_exception { get; set; }

	public string log_stacktrace { get; set; }

	public string log_actiondata { get; set; }

	public NovaLog()
	{
		log_date = DateTime.Now;
	}
}
