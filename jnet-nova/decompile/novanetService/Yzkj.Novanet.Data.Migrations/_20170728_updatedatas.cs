using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _20170728_updatedatas : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_20170728_updatedatas));

	string IMigrationMetadata.Id => "201707281021465_20170728_updatedatas";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.SampleDatas", "SerialNo", (ColumnBuilder c) => c.String(null, 32));
		AddColumn("dbo.SampleDatas", "DeviceId", (ColumnBuilder c) => c.String(null, 32));
		CreateIndex("dbo.SampleDatas", "SerialNo");
		CreateIndex("dbo.SampleDatas", "DeviceId");
	}

	public override void Down()
	{
		DropIndex("dbo.SampleDatas", new string[1] { "DeviceId" });
		DropIndex("dbo.SampleDatas", new string[1] { "SerialNo" });
		DropColumn("dbo.SampleDatas", "DeviceId");
		DropColumn("dbo.SampleDatas", "SerialNo");
	}
}
