using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _20170630_updatelog : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_20170630_updatelog));

	string IMigrationMetadata.Id => "201706300822251_20170630_updatelog";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AlterColumn("dbo.NovaLogs", "log_level", (ColumnBuilder c) => c.String());
	}

	public override void Down()
	{
		AlterColumn("dbo.NovaLogs", "log_level", (ColumnBuilder c) => c.Int(false));
	}
}
