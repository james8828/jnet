using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class TestRangeBus
{
	private readonly NovaDbContext DbContext;

	public TestRangeBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddTestRange(TestRangeModel model)
	{
		TestRange entity = new TestRange
		{
			Id = model.Id,
			LowCricital = model.LowCricital,
			LowNormal = model.LowNormal,
			HighNormal = model.HighNormal,
			HighCricital = model.HighCricital
		};
		DbContext.TestRanges.Add(entity);
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdateTestRange(TestRangeModel model)
	{
		TestRange testRange = DbContext.TestRanges.FirstOrDefault((TestRange e) => e.Id == model.Id);
		if (testRange != null)
		{
			testRange.LowCricital = model.LowCricital;
			testRange.LowNormal = model.LowNormal;
			testRange.HighNormal = model.HighNormal;
			testRange.HighCricital = model.HighCricital;
			testRange.UpdateTime = DateTime.Now;
			await DbContext.SaveChangesAsync();
		}
	}

	public Task<List<TestRangeModel>> GetTestRanges(int? id = null)
	{
		return Task.FromResult((from l in DbContext.Set<TestRange>().AsNoTracking()
			where (int?)l.Id == id && !l.IsDeleted
			select new TestRangeModel
			{
				Id = l.Id,
				LowCricital = l.LowCricital,
				LowNormal = l.LowNormal,
				HighNormal = l.HighNormal,
				HighCricital = l.HighCricital
			}).ToList());
	}

	public async Task<TestRangeModel> GetTestRangeById(int id)
	{
		TestRange testRange = DbContext.Set<TestRange>().AsNoTracking().FirstOrDefault((TestRange e) => e.Id == id);
		if (testRange == null)
		{
			return null;
		}
		return new TestRangeModel
		{
			Id = testRange.Id,
			LowCricital = testRange.LowCricital,
			LowNormal = testRange.LowNormal,
			HighNormal = testRange.HighNormal,
			HighCricital = testRange.HighCricital,
			SL = testRange.SL,
			IC = testRange.IC
		};
	}
}
