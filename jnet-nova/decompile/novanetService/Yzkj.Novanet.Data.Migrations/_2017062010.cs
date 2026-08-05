using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062010 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062010));

	string IMigrationMetadata.Id => "201706201005044_2017062010";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropPrimaryKey("dbo.NovaSetups");
		DropColumn("dbo.NovaSetups", "sId");
		AddColumn("dbo.NovaSetups", "Id", (ColumnBuilder c) => c.Int(false, identity: true));
		AddPrimaryKey("dbo.NovaSetups", "Id");
	}

	public override void Down()
	{
		AddColumn("dbo.NovaSetups", "sId", (ColumnBuilder c) => c.Int(false, identity: true));
		DropPrimaryKey("dbo.NovaSetups");
		DropColumn("dbo.NovaSetups", "Id");
		AddPrimaryKey("dbo.NovaSetups", "sId");
	}
}
