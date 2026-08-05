using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_testrange : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_testrange));

	string IMigrationMetadata.Id => "201705310447284_update_testrange";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AlterColumn("dbo.TestRanges", "AgeLow", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.TestRanges", "AgeHigh", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.TestRanges", "DeleteTime", (ColumnBuilder c) => c.DateTime());
	}

	public override void Down()
	{
		AlterColumn("dbo.TestRanges", "DeleteTime", (ColumnBuilder c) => c.DateTime(false));
		AlterColumn("dbo.TestRanges", "AgeHigh", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.TestRanges", "AgeLow", (ColumnBuilder c) => c.Int(false));
	}
}
