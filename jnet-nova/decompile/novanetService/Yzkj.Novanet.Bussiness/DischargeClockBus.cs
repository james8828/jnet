using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class DischargeClockBus
{
	private readonly NovaDbContext DbContext;

	public DischargeClockBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddDischargeClock(DischargeClockModel model)
	{
		DischargeClock entity = new DischargeClock
		{
			LocationId = model.LocationId,
			IsEnabled = model.IsEnabled,
			Hour = model.Hour,
			Minute = model.Minute
		};
		DbContext.DischargeClocks.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdateDischargeClock(DischargeClockModel model)
	{
		DischargeClock dischargeClock = DbContext.DischargeClocks.FirstOrDefault((DischargeClock e) => e.LocationId == model.LocationId);
		if (dischargeClock != null)
		{
			dischargeClock.IsEnabled = model.IsEnabled;
			dischargeClock.Hour = model.Hour;
			dischargeClock.Minute = model.Minute;
			dischargeClock.SaveTime = DateTime.Now;
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task<DischargeClockModel> GetDischargeClockById(int id)
	{
		DischargeClock dischargeClock = DbContext.Set<DischargeClock>().AsNoTracking().FirstOrDefault((DischargeClock e) => e.LocationId == id);
		if (dischargeClock == null)
		{
			return null;
		}
		return new DischargeClockModel
		{
			Id = dischargeClock.Id,
			LocationId = dischargeClock.LocationId,
			IsEnabled = dischargeClock.IsEnabled,
			Hour = dischargeClock.Hour,
			Minute = dischargeClock.Minute
		};
	}

	public async Task<List<DischargeClockModel>> GetDischargeClocks()
	{
		return (from e in DbContext.Set<DischargeClock>().Include("Location").AsNoTracking()
			where e.IsEnabled
			select e)?.Select((DischargeClock c) => new DischargeClockModel
		{
			Id = c.Id,
			LocationId = c.LocationId,
			Hour = c.Hour,
			Minute = c.Minute,
			DepartName = c.Location.Name
		}).ToList();
	}
}
