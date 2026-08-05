using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_nurse_add_updatetime : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_nurse_add_updatetime));

	string IMigrationMetadata.Id => "201707260257553_update_nurse_add_updatetime";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.Nurses", "UpdateTime", (ColumnBuilder c) => c.DateTime());
		CreateIndex("dbo.Nurses", "UpdateTime");
	}

	public override void Down()
	{
		DropIndex("dbo.Nurses", new string[1] { "UpdateTime" });
		DropColumn("dbo.Nurses", "UpdateTime");
	}
}
