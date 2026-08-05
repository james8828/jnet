using System;
using System.Linq;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class PreferenceBus
{
	private readonly NovaDbContext DbContext;

	public PreferenceBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddPreference(PreferenceModel model)
	{
		Preference entity = new Preference
		{
			Id = model.Id,
			AutoReConnect = model.AutoReConnect,
			CycleMinutes = model.CycleMinutes,
			PatientID = model.PatientID
		};
		DbContext.Preferences.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdatePreferences(PreferenceModel model)
	{
		Preference preference = DbContext.Preferences.FirstOrDefault((Preference e) => e.Id == model.Id);
		if (preference != null)
		{
			preference.AutoReConnect = model.AutoReConnect;
			preference.CycleMinutes = model.CycleMinutes;
			preference.PatientID = model.PatientID;
			preference.UpdateTime = DateTime.Now;
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task<PreferenceModel> GetPreferenceById(int id)
	{
		Preference preference = DbContext.Set<Preference>().AsNoTracking().FirstOrDefault((Preference e) => e.Id == id);
		if (preference == null)
		{
			return null;
		}
		return new PreferenceModel
		{
			Id = preference.Id,
			AutoReConnect = preference.AutoReConnect,
			CycleMinutes = preference.CycleMinutes,
			PatientID = preference.PatientID
		};
	}
}
