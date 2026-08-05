using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_device_add_deviceid : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_device_add_deviceid));

	string IMigrationMetadata.Id => "201707030953016_update_device_add_deviceid";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.Devices", "DeviceId", (ColumnBuilder c) => c.String(null, 32));
		CreateIndex("dbo.Devices", "DeviceId");
	}

	public override void Down()
	{
		DropIndex("dbo.Devices", new string[1] { "DeviceId" });
		DropColumn("dbo.Devices", "DeviceId");
	}
}
