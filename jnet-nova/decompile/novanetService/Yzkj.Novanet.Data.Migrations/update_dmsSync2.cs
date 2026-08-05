using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_dmsSync2 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_dmsSync2));

	string IMigrationMetadata.Id => "201708210937404_update_dmsSync2";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AlterColumn("dbo.Locations", "DmsId", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.LocationDiagcodes", "DmsId", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.Diagcodes", "DmsId", (ColumnBuilder c) => c.Int());
		AlterColumn("dbo.LocationNurses", "DmsId", (ColumnBuilder c) => c.Guid());
		AlterColumn("dbo.Nurses", "DmsId", (ColumnBuilder c) => c.Guid());
		AlterColumn("dbo.Patients", "DmsId", (ColumnBuilder c) => c.Int());
	}

	public override void Down()
	{
		AlterColumn("dbo.Patients", "DmsId", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.Nurses", "DmsId", (ColumnBuilder c) => c.Guid(false));
		AlterColumn("dbo.LocationNurses", "DmsId", (ColumnBuilder c) => c.Guid(false));
		AlterColumn("dbo.Diagcodes", "DmsId", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.LocationDiagcodes", "DmsId", (ColumnBuilder c) => c.Int(false));
		AlterColumn("dbo.Locations", "DmsId", (ColumnBuilder c) => c.Int(false));
	}
}
