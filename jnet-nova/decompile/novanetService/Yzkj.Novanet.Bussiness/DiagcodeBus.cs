using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class DiagcodeBus
{
	private readonly NovaDbContext DbContext;

	public DiagcodeBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddDiagcode(string code, string description)
	{
		Diagcode entity = new Diagcode
		{
			Code = code,
			Description = description
		};
		DbContext.Diagcodes.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task AddLocationDiag(int locatId, List<int> ids)
	{
		List<LocationDiagcode> list = (from e in DbContext.Set<LocationDiagcode>()
			where e.LocationId == locatId
			select e).ToList();
		if (list == null)
		{
			return;
		}
		DbContext.Set<LocationDiagcode>().RemoveRange(list);
		DbContext.SaveChanges();
		foreach (int id in ids)
		{
			LocationDiagcode entity = new LocationDiagcode
			{
				LocationId = locatId,
				DiagcodeId = id
			};
			DbContext.LocationDiagcodes.Add(entity);
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddDiagcodeGroup(int groupId, string name, List<int> ids)
	{
		List<Diagcode> list = (from e in DbContext.Set<Diagcode>()
			where ids.Contains(e.Id)
			select e).ToList();
		if (groupId == 0)
		{
			DiagcodeGroup entity = new DiagcodeGroup
			{
				Name = name,
				Diagcodes = list
			};
			DbContext.DiagcodeGroups.Add(entity);
		}
		else
		{
			DiagcodeGroup diagcodeGroup = DbContext.DiagcodeGroups.FirstOrDefault((DiagcodeGroup e) => e.Id == groupId);
			if (diagcodeGroup == null)
			{
				return;
			}
			diagcodeGroup.Name = name;
			diagcodeGroup.Diagcodes.Clear();
			foreach (Diagcode item in list)
			{
				diagcodeGroup.Diagcodes.Add(item);
			}
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task DeleteDiagcode(int id)
	{
		Diagcode diagcode = DbContext.Diagcodes.FirstOrDefault((Diagcode e) => e.Id == id);
		if (diagcode != null)
		{
			DbContext.Diagcodes.Remove(diagcode);
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task DeleteDiagGroup(int id)
	{
		DiagcodeGroup diagcodeGroup = DbContext.DiagcodeGroups.FirstOrDefault((DiagcodeGroup e) => e.Id == id);
		if (diagcodeGroup != null)
		{
			DbContext.DiagcodeGroups.Remove(diagcodeGroup);
			await DbContext.SaveChangesAsync();
		}
	}

	public Task<List<DiagcodeModel>> GetDiagcodes()
	{
		return Task.FromResult((from l in DbContext.Set<Diagcode>().AsNoTracking()
			select new DiagcodeModel
			{
				Id = l.Id,
				Code = l.Code,
				Description = l.Description,
				LocationDiagcodes = l.LocationDiagcodes,
				Groups = l.Groups
			}).ToList());
	}

	public Task<List<DiagcodeModel>> GetDiagcodesByPage(int lid, int gid, int start, int length, out int total)
	{
		IQueryable<Diagcode> source = from e in DbContext.Set<Diagcode>().Include("LocationDiagcode").Include("Location")
				.AsNoTracking()
			where e.Id > 0
			select e;
		if (lid > 0)
		{
			source = source.Where((Diagcode e) => e.LocationDiagcodes.Where((LocationDiagcode l) => l.LocationId == lid).Count() > 0);
		}
		if (gid > 0)
		{
			source = source.Where((Diagcode e) => e.Groups.Where((DiagcodeGroup g) => g.Id == gid).Count() > 0);
		}
		source = source.OrderBy((Diagcode e) => e.Id);
		total = source.Count();
		return Task.FromResult((from p in source.Skip(start).Take(length)
			select new DiagcodeModel
			{
				Id = p.Id,
				Code = p.Code,
				Description = p.Description,
				LocationDiagcodes = p.LocationDiagcodes,
				Groups = p.Groups
			}).ToList());
	}

	public Task<List<DiagcodeGroupModel>> GetDiagGroups()
	{
		return Task.FromResult((from l in DbContext.Set<DiagcodeGroup>().AsNoTracking()
			select new DiagcodeGroupModel
			{
				Id = l.Id,
				Name = l.Name
			}).ToList());
	}
}
