using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062009 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062009));

	string IMigrationMetadata.Id => "201706201004117_2017062009";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropForeignKey("dbo.NovaSetupGroups", "novaSetupId", "dbo.NovaSetups");
		DropIndex("dbo.NovaSetupGroups", new string[1] { "novaSetupId" });
		DropPrimaryKey("dbo.NovaSetups");
		AddColumn("dbo.NovaSetups", "sId", (ColumnBuilder c) => c.Int(false, identity: true));
		AddPrimaryKey("dbo.NovaSetups", "sId");
		DropColumn("dbo.NovaSetupGroups", "novaSetupId");
		DropColumn("dbo.NovaSetups", "Id");
	}

	public override void Down()
	{
		AddColumn("dbo.NovaSetups", "Id", (ColumnBuilder c) => c.Int(false, identity: true));
		AddColumn("dbo.NovaSetupGroups", "novaSetupId", (ColumnBuilder c) => c.Int(false));
		DropPrimaryKey("dbo.NovaSetups");
		DropColumn("dbo.NovaSetups", "sId");
		AddPrimaryKey("dbo.NovaSetups", "Id");
		CreateIndex("dbo.NovaSetupGroups", "novaSetupId");
		AddForeignKey("dbo.NovaSetupGroups", "novaSetupId", "dbo.NovaSetups", "Id", cascadeDelete: true);
	}
}
