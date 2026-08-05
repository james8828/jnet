using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Dynamic;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class ReagentBus
{
	private readonly NovaDbContext DbContext;

	public ReagentBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddReagent(string LotNum, int LotType, decimal? High, decimal? Low, DateTime Expiration)
	{
		Reagent entity = new Reagent
		{
			LotNum = LotNum,
			LotType = LotType,
			High = High,
			Low = Low,
			Expiration = Expiration
		};
		DbContext.Reagents.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdateReagent(int id, decimal? High, decimal? Low)
	{
		Reagent reagent = DbContext.Reagents.FirstOrDefault((Reagent e) => e.Id == id);
		reagent.High = High;
		reagent.Low = Low;
		await DbContext.SaveChangesAsync();
	}

	public async Task AddLocationReagent(int locatId, List<int> ids)
	{
		List<LocationReagent> list = DbContext.LocationReagents.Where((LocationReagent e) => e.LocationId == locatId).ToList();
		if (list == null)
		{
			return;
		}
		DbContext.Set<LocationReagent>().RemoveRange(list);
		DbContext.SaveChanges();
		foreach (int id in ids)
		{
			LocationReagent entity = new LocationReagent
			{
				LocationId = locatId,
				ReagentId = id
			};
			DbContext.LocationReagents.Add(entity);
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddReagentGroup(int groupId, string name, List<int> ids)
	{
		List<Reagent> list = (from e in DbContext.Set<Reagent>()
			where ids.Contains(e.Id)
			select e).ToList();
		if (groupId == 0)
		{
			ReagentGroup entity = new ReagentGroup
			{
				Name = name,
				Reagents = list
			};
			DbContext.ReagentGroups.Add(entity);
		}
		else
		{
			ReagentGroup reagentGroup = DbContext.Set<ReagentGroup>().FirstOrDefault((ReagentGroup e) => e.Id == groupId);
			if (reagentGroup == null)
			{
				return;
			}
			reagentGroup.Name = name;
			reagentGroup.Reagents.Clear();
			foreach (Reagent item in list)
			{
				reagentGroup.Reagents.Add(item);
			}
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task DeleteReagent(int id)
	{
		Reagent reagent = DbContext.Reagents.FirstOrDefault((Reagent e) => e.Id == id);
		if (reagent != null)
		{
			DbContext.Reagents.Remove(reagent);
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task DeleteReagentGroup(int id)
	{
		ReagentGroup reagentGroup = DbContext.ReagentGroups.FirstOrDefault((ReagentGroup e) => e.Id == id);
		if (reagentGroup != null)
		{
			DbContext.ReagentGroups.Remove(reagentGroup);
			await DbContext.SaveChangesAsync();
		}
	}

	public Task<List<ReagentGroupModel>> GetReagentGroups()
	{
		return Task.FromResult((from l in DbContext.Set<ReagentGroup>().AsNoTracking()
			select new ReagentGroupModel
			{
				Id = l.Id,
				Name = l.Name
			}).ToList());
	}

	public int GeReagentsAll()
	{
		int result = 0;
		DateTime nowDate = DateTime.Parse(DateTime.Now.ToString("yyyy-MM-dd 00:00:00.000"));
		IQueryable<Reagent> source = from e in DbContext.Set<Reagent>()
			where e.Expiration.CompareTo(nowDate) >= 0
			select e;
		if (source.Count() > 0)
		{
			result = source.Count();
		}
		return result;
	}

	public Task<List<ReagentModel>> GetReagents()
	{
		return Task.FromResult((from l in DbContext.Set<Reagent>().AsNoTracking()
			select new ReagentModel
			{
				Id = l.Id,
				LotNum = l.LotNum,
				LotType = l.LotType,
				High = l.High,
				Low = l.Low,
				Expiration = l.Expiration
			}).ToList());
	}

	public async Task<ReagentModel> GetReagent(int id)
	{
		Reagent reagent = DbContext.Set<Reagent>().AsNoTracking().FirstOrDefault((Reagent e) => e.Id == id);
		return new ReagentModel
		{
			Id = reagent.Id,
			LotNum = reagent.LotNum,
			LotType = reagent.LotType,
			High = reagent.High,
			Low = reagent.Low,
			Expiration = reagent.Expiration
		};
	}

	public Task<List<ReagentModel>> GetReagentsByPage(int lotType, int lid, int gid, List<OrderFieldModel> sorts)
	{
		IQueryable<Reagent> source = from e in DbContext.Set<Reagent>().Include("LocationReagent").Include("Location")
				.AsNoTracking()
			where e.Id > 0 && e.LotType == lotType
			select e;
		if (lid > 0 && gid == 0)
		{
			source = source.Where((Reagent e) => e.LocationReagents.Where((LocationReagent l) => l.LocationId == lid).Count() > 0);
		}
		if (gid > 0)
		{
			source = source.Where((Reagent e) => e.Groups.Where((ReagentGroup g) => g.Id == gid).Count() > 0);
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
				DateTime nowDate = DateTime.Parse(DateTime.Now.ToString("yyyy-MM-dd 00:00:00.000"));
				IQueryable<Reagent> source2 = source.Where((Reagent e) => e.Expiration.CompareTo(nowDate) >= 0);
				source2 = source2.OrderBy(text).Take(10000);
				IQueryable<Reagent> source3 = source.Where((Reagent e) => e.Expiration.CompareTo(nowDate) < 0);
				source3 = source3.OrderBy(text).Take(10000);
				source = source2.Concat(source3);
			}
		}
		else
		{
			source = from e in source
				orderby e.Expiration descending
				orderby e.CreateTime descending
				select e;
		}
		return Task.FromResult(source.Select((Reagent p) => new ReagentModel
		{
			Id = p.Id,
			LotNum = p.LotNum,
			LotType = p.LotType,
			High = p.High,
			Low = p.Low,
			Expiration = p.Expiration
		}).ToList());
	}
}
