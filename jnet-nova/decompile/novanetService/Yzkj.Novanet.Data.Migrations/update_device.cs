using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_device : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_device));

	string IMigrationMetadata.Id => "201706020635246_update_device";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropForeignKey("dbo.Devices", "LocationId", "dbo.Locations");
		DropIndex("dbo.Devices", new string[1] { "LocationId" });
		AlterColumn("dbo.Devices", "SerialNo", (ColumnBuilder c) => c.String(null, 32));
		AlterColumn("dbo.Devices", "LocationId", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.Locations", "Name", (ColumnBuilder c) => c.String(null, 32));
		CreateIndex("dbo.Devices", "SerialNo");
		CreateIndex("dbo.Devices", "LocationId");
		CreateIndex("dbo.Devices", "LastTime");
		CreateIndex("dbo.Locations", "Name");
		AddForeignKey("dbo.Devices", "LocationId", "dbo.Locations", "Id");
	}

	public override void Down()
	{
		DropForeignKey("dbo.Devices", "LocationId", "dbo.Locations");
		DropIndex("dbo.Locations", new string[1] { "Name" });
		DropIndex("dbo.Devices", new string[1] { "LastTime" });
		DropIndex("dbo.Devices", new string[1] { "LocationId" });
		DropIndex("dbo.Devices", new string[1] { "SerialNo" });
		AlterColumn("dbo.Locations", "Name", (ColumnBuilder c) => c.String());
		AlterColumn("dbo.Devices", "LocationId", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.Devices", "SerialNo", (ColumnBuilder c) => c.String());
		CreateIndex("dbo.Devices", "LocationId");
		AddForeignKey("dbo.Devices", "LocationId", "dbo.Locations", "Id", cascadeDelete: true);
	}
}
