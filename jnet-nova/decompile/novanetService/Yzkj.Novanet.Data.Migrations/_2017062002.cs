using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062002 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062002));

	string IMigrationMetadata.Id => "201706200838238_2017062002";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		CreateTable("dbo.NovaSetupGroups", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Name = c.String(),
			novaSetupId = c.Int(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.NovaSetups", t => t.novaSetupId, cascadeDelete: true).Index(t => t.novaSetupId);
	}

	public override void Down()
	{
		DropForeignKey("dbo.NovaSetupGroups", "novaSetupId", "dbo.NovaSetups");
		DropIndex("dbo.NovaSetupGroups", new string[1] { "novaSetupId" });
		DropTable("dbo.NovaSetupGroups");
	}
}
