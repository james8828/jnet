using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062706 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062706));

	string IMigrationMetadata.Id => "201706270557552_2017062706";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.Locations", "ST_Location", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Setup", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Nurse", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Patient", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Reagent", (ColumnBuilder c) => c.DateTime());
	}

	public override void Down()
	{
		DropColumn("dbo.Locations", "ST_Reagent");
		DropColumn("dbo.Locations", "ST_Patient");
		DropColumn("dbo.Locations", "ST_Nurse");
		DropColumn("dbo.Locations", "ST_Setup");
		DropColumn("dbo.Locations", "ST_Location");
	}
}
