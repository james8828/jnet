using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062703 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062703));

	string IMigrationMetadata.Id => "201706270331154_2017062703";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropForeignKey("dbo.Diagcodes", "NovaSetup_Id", "dbo.NovaSetups");
		DropForeignKey("dbo.TestRanges", "NovaSetup_Id", "dbo.NovaSetups");
		DropIndex("dbo.Diagcodes", new string[1] { "NovaSetup_Id" });
		DropIndex("dbo.TestRanges", new string[1] { "NovaSetup_Id" });
		DropColumn("dbo.Diagcodes", "NovaSetup_Id");
		DropColumn("dbo.TestRanges", "NovaSetup_Id");
	}

	public override void Down()
	{
		AddColumn("dbo.TestRanges", "NovaSetup_Id", (ColumnBuilder c) => c.Int());
		AddColumn("dbo.Diagcodes", "NovaSetup_Id", (ColumnBuilder c) => c.Int());
		CreateIndex("dbo.TestRanges", "NovaSetup_Id");
		CreateIndex("dbo.Diagcodes", "NovaSetup_Id");
		AddForeignKey("dbo.TestRanges", "NovaSetup_Id", "dbo.NovaSetups", "Id");
		AddForeignKey("dbo.Diagcodes", "NovaSetup_Id", "dbo.NovaSetups", "Id");
	}
}
