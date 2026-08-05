using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class NovaLogBus
{
	private readonly NovaDbContext DbContext;

	public NovaLogBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public Task<List<NovaLogModel>> GetNovaLogsByPage(List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		IQueryable<NovaLog> source = from e in DbContext.Set<NovaLog>().AsNoTracking()
			where e.log_id > 0
			select e;
		if (sorts != null)
		{
			string text = "";
			for (int num = 0; num < sorts.Count; num++)
			{
				OrderFieldModel orderFieldModel = sorts.ElementAt(num);
				text = text + orderFieldModel.PropertyName + (orderFieldModel.IsDesc ? " desc" : "") + ",";
			}
			if (!string.IsNullOrEmpty(text))
			{
				text = text.Trim(',');
				source = source.OrderBy(text);
			}
		}
		else
		{
			source = source.OrderByDescending((NovaLog e) => e.log_date);
		}
		total = source.Count();
		return Task.FromResult((from p in source.Skip(start).Take(length)
			select new NovaLogModel
			{
				log_id = p.log_id,
				log_date = p.log_date,
				log_level = p.log_level,
				log_source = p.log_source,
				log_message = p.log_message,
				log_machine_name = p.log_machine_name,
				log_user_name = p.log_user_name,
				log_exception = p.log_exception,
				log_stacktrace = p.log_stacktrace
			}).ToList());
	}
}
