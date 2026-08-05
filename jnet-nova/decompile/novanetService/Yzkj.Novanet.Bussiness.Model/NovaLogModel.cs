using System;

namespace Yzkj.Novanet.Bussiness.Model;

public class NovaLogModel
{
	public long log_id { get; set; }

	public DateTime log_date { get; set; }

	public string log_level { get; set; }

	public string log_source { get; set; }

	public string log_message { get; set; }

	public string log_machine_name { get; set; }

	public string log_user_name { get; set; }

	public string log_exception { get; set; }

	public string log_stacktrace { get; set; }
}
