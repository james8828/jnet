using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_sampledatas_add_qclevel : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_sampledatas_add_qclevel));

	string IMigrationMetadata.Id => "201708230932152_update_sampledatas_add_qclevel";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.SampleDatas", "QcLevel", (ColumnBuilder c) => c.Int(false));
	}

	public override void Down()
	{
		DropColumn("dbo.SampleDatas", "QcLevel");
	}
}
