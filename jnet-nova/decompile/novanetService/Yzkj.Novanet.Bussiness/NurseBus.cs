using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using System.Transactions;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class NurseBus
{
	private readonly NovaDbContext DbContext;

	public NurseBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddNurse(NurseModel model)
	{
		using TransactionScope transactionScope = new TransactionScope();
		try
		{
			Nurse nurse = new Nurse
			{
				Name = model.Name,
				Code = model.Code,
				hiskey = model.hiskey
			};
			DbContext.Nurses.Add(nurse);
			DbContext.SaveChanges();
			foreach (string item in model.LocationsId)
			{
				LocationNurse entity = new LocationNurse
				{
					LocationId = Convert.ToInt32(item),
					NurseId = nurse.Id
				};
				DbContext.LocationNurses.Add(entity);
			}
			DbContext.SaveChanges();
			transactionScope.Complete();
		}
		catch (Exception ex)
		{
			throw ex;
		}
		finally
		{
			transactionScope.Dispose();
		}
	}

	public async Task UpdateNurse(NurseModel model)
	{
		using TransactionScope transactionScope = new TransactionScope();
		try
		{
			Nurse nurse = DbContext.Set<Nurse>().Include("LocationNurses").FirstOrDefault((Nurse e) => (int?)e.Id == model.Id);
			nurse.Code = model.Code;
			nurse.Name = model.Name;
			nurse.hiskey = model.hiskey;
			nurse.UpdateTime = DateTime.Now;
			DbContext.Set<LocationNurse>().RemoveRange(nurse.LocationNurses);
			foreach (string item in model.LocationsId)
			{
				LocationNurse entity = new LocationNurse
				{
					LocationId = Convert.ToInt32(item),
					NurseId = nurse.Id
				};
				DbContext.LocationNurses.Add(entity);
			}
			DbContext.SaveChanges();
			transactionScope.Complete();
		}
		catch (Exception ex)
		{
			throw ex;
		}
		finally
		{
			transactionScope.Dispose();
		}
	}

	public async Task DeleteNurses(List<int> ids)
	{
		if (ids == null || ids.Count == 0)
		{
			return;
		}
		foreach (Nurse item in DbContext.Nurses.Where((Nurse e) => ids.Contains(e.Id)))
		{
			item.IsDelete = true;
			item.DeleteTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public Task<List<NurseModel>> GetNurses()
	{
		return Task.FromResult((from l in DbContext.Set<Nurse>().Include("LocationNurses").Include("LocationNurses.Location")
				.AsNoTracking()
			where !l.IsDelete
			select new NurseModel
			{
				Id = l.Id,
				Name = l.Name,
				Code = l.Code,
				hiskey = l.hiskey,
				LocationsId = l.LocationNurses.Select((LocationNurse e) => e.LocationId.ToString()).ToList(),
				LocationsName = l.LocationNurses.Select((LocationNurse e) => e.Location.Name).ToList(),
				LocationNurses = l.LocationNurses
			}).ToList());
	}

	public async Task<List<NurseModel>> GetNursesByLocationId(int id)
	{
		return (from n in DbContext.Set<Nurse>().Include("LocationNurses").Include("LocationNurses.Location")
				.AsNoTracking()
			where !n.IsDelete && n.LocationNurses.Count((LocationNurse l) => l.LocationId == id) > 0
			select new NurseModel
			{
				Id = n.Id,
				Name = n.Name,
				Code = n.Code,
				hiskey = n.hiskey,
				LocationsId = n.LocationNurses.Select((LocationNurse e) => e.LocationId.ToString()).ToList(),
				LocationsName = n.LocationNurses.Select((LocationNurse e) => e.Location.Name).ToList(),
				LocationNurses = n.LocationNurses
			}).ToList();
	}

	public Task<List<NurseModel>> GetNursesByPage(int locationId, List<OrderFieldModel> sorts, int start, int length, out int total)
	{
		IQueryable<Nurse> source = from e in DbContext.Set<Nurse>().Include("LocationNurses").Include("LocationNurses.Location")
				.AsNoTracking()
			where !e.IsDelete && e.LocationNurses.Count((LocationNurse l) => l.LocationId == locationId) > 0
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
			source = source.OrderByDescending((Nurse e) => e.CreateTime);
		}
		total = source.Count();
		return Task.FromResult((from n in source.Skip(start).Take(length)
			select new NurseModel
			{
				Id = n.Id,
				Name = n.Name,
				Code = n.Code,
				hiskey = n.hiskey,
				LocationsId = n.LocationNurses.Select((LocationNurse e) => e.LocationId.ToString()).ToList(),
				LocationsName = n.LocationNurses.Select((LocationNurse e) => e.Location.Name).ToList(),
				LocationNurses = n.LocationNurses
			}).ToList());
	}

	public List<NurseModel> GetSyncNurses(string name, int ps, int pi, out int total)
	{
		IQueryable<Nurse> source = from e in DbContext.Set<Nurse>().AsNoTracking()
			where !e.IsDelete && e.LocationNurses.Count((LocationNurse l) => l.Location.Name == name) > 0
			select e;
		total = source.Count();
		source = source.Skip((pi - 1) * ps).Take(ps);
		return source.Select((Nurse n) => new NurseModel
		{
			Id = n.Id,
			Name = n.Name,
			Code = n.Code,
			hiskey = n.hiskey
		}).ToList();
	}

	public async Task UpdateNurseSyncStatus(List<int> ids)
	{
		foreach (Nurse item in DbContext.Nurses.Where((Nurse e) => ids.Contains(e.Id)))
		{
			item.SyncStatus = 1;
			item.SyncTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}
}
