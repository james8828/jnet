using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _201706201111 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_201706201111));

	string IMigrationMetadata.Id => "201706201015471_201706201111";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.NovaSetupGroups", "novaSetupId", (ColumnBuilder c) => c.Int(false));
		CreateIndex("dbo.NovaSetupGroups", "novaSetupId");
		AddForeignKey("dbo.NovaSetupGroups", "novaSetupId", "dbo.NovaSetups", "Id", cascadeDelete: true);
	}

	public override void Down()
	{
		DropForeignKey("dbo.NovaSetupGroups", "novaSetupId", "dbo.NovaSetups");
		DropIndex("dbo.NovaSetupGroups", new string[1] { "novaSetupId" });
		DropColumn("dbo.NovaSetupGroups", "novaSetupId");
	}
}
