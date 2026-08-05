using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_sampledata_add_qc : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_sampledata_add_qc));

	string IMigrationMetadata.Id => "201708220950321_update_sampledata_add_qc";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.SampleDatas", "ObsType", (ColumnBuilder c) => c.Int(false));
		AddColumn("dbo.SampleDatas", "QcLot", (ColumnBuilder c) => c.String());
	}

	public override void Down()
	{
		DropColumn("dbo.SampleDatas", "QcLot");
		DropColumn("dbo.SampleDatas", "ObsType");
	}
}
