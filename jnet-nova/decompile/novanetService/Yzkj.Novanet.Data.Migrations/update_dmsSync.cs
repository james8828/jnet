using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_dmsSync : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_dmsSync));

	string IMigrationMetadata.Id => "201708210935017_update_dmsSync";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.Locations", "DmsId", (ColumnBuilder c) => c.Int(false));
		AddColumn("dbo.LocationDiagcodes", "DmsId", (ColumnBuilder c) => c.Int(false));
		AddColumn("dbo.Diagcodes", "DmsId", (ColumnBuilder c) => c.Int(false));
		AddColumn("dbo.LocationNurses", "DmsId", (ColumnBuilder c) => c.Guid(false));
		AddColumn("dbo.Nurses", "DmsId", (ColumnBuilder c) => c.Guid(false));
		AddColumn("dbo.Patients", "DmsId", (ColumnBuilder c) => c.Int(false));
	}

	public override void Down()
	{
		DropColumn("dbo.Patients", "DmsId");
		DropColumn("dbo.Nurses", "DmsId");
		DropColumn("dbo.LocationNurses", "DmsId");
		DropColumn("dbo.Diagcodes", "DmsId");
		DropColumn("dbo.LocationDiagcodes", "DmsId");
		DropColumn("dbo.Locations", "DmsId");
	}
}
