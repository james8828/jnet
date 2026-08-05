using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_novasetup_group : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_novasetup_group));

	string IMigrationMetadata.Id => "201706200821564_update_novasetup_group";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropForeignKey("dbo.NovaSetups", "Id", "dbo.Locations");
		DropIndex("dbo.NovaSetups", new string[1] { "Id" });
		DropPrimaryKey("dbo.NovaSetups");
		AddColumn("dbo.NovaSetups", "LocationId", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.NovaSetups", "Id", (ColumnBuilder c) => c.Int(false, identity: true));
		AddPrimaryKey("dbo.NovaSetups", "Id");
		CreateIndex("dbo.NovaSetups", "LocationId");
		AddForeignKey("dbo.NovaSetups", "LocationId", "dbo.Locations", "Id", cascadeDelete: true);
	}

	public override void Down()
	{
		DropForeignKey("dbo.NovaSetups", "LocationId", "dbo.Locations");
		DropIndex("dbo.NovaSetups", new string[1] { "LocationId" });
		DropPrimaryKey("dbo.NovaSetups");
		AlterColumn("dbo.NovaSetups", "Id", (ColumnBuilder c) => c.Int(false));
		DropColumn("dbo.NovaSetups", "LocationId");
		AddPrimaryKey("dbo.NovaSetups", "Id");
		CreateIndex("dbo.NovaSetups", "Id");
		AddForeignKey("dbo.NovaSetups", "Id", "dbo.Locations", "Id");
	}
}
