using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class DeviceBus
{
	private readonly NovaDbContext DbContext;

	public DeviceBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public int GetDevicesAll()
	{
		int result = 0;
		IQueryable<Device> source = from e in DbContext.Set<Device>().AsNoTracking()
			where e.Id > 0
			select e;
		if (source.Count() > 0)
		{
			result = source.Count();
		}
		return result;
	}

	public Task<List<DeviceModel>> GetDevicesByPage(string startDate, string endDate, List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		IQueryable<Device> source = from e in DbContext.Set<Device>().AsNoTracking()
			where e.Id > 0
			select e;
		if (startDate != string.Empty && startDate != null)
		{
			DateTime sdate = DateTime.Parse(startDate + " 00:00");
			source = source.Where((Device e) => e.LastTime.CompareTo(sdate) >= 0);
		}
		if (endDate != string.Empty && endDate != null)
		{
			DateTime edate = DateTime.Parse(endDate + " 23:59");
			source = source.Where((Device e) => e.LastTime.CompareTo(edate) <= 0);
		}
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
			source = source.OrderByDescending((Device e) => e.SerialNo);
		}
		total = source.Count();
		return Task.FromResult((from p in source.Skip(start).Take(length)
			select new DeviceModel
			{
				Id = p.Id,
				SerialNo = p.SerialNo,
				Name = p.Name,
				Hospital = p.Hospital,
				Depart = p.Depart,
				LastTime = p.LastTime
			}).ToList());
	}
}
