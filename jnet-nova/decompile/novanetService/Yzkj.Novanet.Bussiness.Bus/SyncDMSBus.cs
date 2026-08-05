using System.Data.SqlClient;
using Yzkj.Novanet.Data;

namespace Yzkj.Novanet.Bussiness.Bus;

public class SyncDMSBus
{
	private readonly NovaDbContext DbContext;

	public SyncDMSBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public int SyncSampleData(string date)
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand($"EXEC [dbo].[Proc_SyncSampleData] '{date}'", parameters);
	}

	public int SyncLocation()
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand("EXEC [dbo].[Proc_SyncLocations]", parameters);
	}

	public int SyncDepts()
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand("EXEC [dbo].[Proc_SyncDepts]", parameters);
	}

	public int SyncDiags()
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand("EXEC [dbo].[Proc_SyncDiags]", parameters);
	}

	public int SyncPatients()
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand("EXEC [dbo].[Proc_SyncPatients]", parameters);
	}

	public int SyncNurses()
	{
		SqlParameter[] parameters = new SqlParameter[0];
		return DbContext.Database.ExecuteSqlCommand("EXEC [dbo].[Proc_SyncNurses]", parameters);
	}
}
