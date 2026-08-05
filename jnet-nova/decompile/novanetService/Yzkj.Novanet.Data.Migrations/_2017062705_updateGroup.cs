using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062705_updateGroup : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062705_updateGroup));

	string IMigrationMetadata.Id => "201706270549512_2017062705_updateGroup";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.NovaSetupGroups", "SL", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AddColumn("dbo.NovaSetupGroups", "IC", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		DropColumn("dbo.Locations", "ST_Location");
		DropColumn("dbo.Locations", "ST_Setup");
		DropColumn("dbo.Locations", "ST_Nurse");
		DropColumn("dbo.Locations", "ST_Patient");
		DropColumn("dbo.Locations", "ST_Reagent");
	}

	public override void Down()
	{
		AddColumn("dbo.Locations", "ST_Reagent", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Patient", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Nurse", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Setup", (ColumnBuilder c) => c.DateTime());
		AddColumn("dbo.Locations", "ST_Location", (ColumnBuilder c) => c.DateTime());
		DropColumn("dbo.NovaSetupGroups", "IC");
		DropColumn("dbo.NovaSetupGroups", "SL");
	}
}
