using System;
using System.Data.Entity.Migrations;

namespace Yzkj.Novanet.Data.Migrations;

internal sealed class Configuration : DbMigrationsConfiguration<NovaDbContext>
{
	public Configuration()
	{
		base.AutomaticMigrationsEnabled = true;
		base.AutomaticMigrationDataLossAllowed = true;
		base.ContextKey = "Yzkj.Novanet.Data.NovaDbContext";
	}

	protected override void Seed(NovaDbContext context)
	{
		if (!string.IsNullOrEmpty(DbResource.TriggerGenerateScript))
		{
			string[] array = DbResource.TriggerGenerateScript.Split(new string[1] { "GO" }, StringSplitOptions.RemoveEmptyEntries);
			foreach (string sqlBatch in array)
			{
				context.Database.ExecuteSqlCommand(sqlBatch);
			}
		}
	}
}
