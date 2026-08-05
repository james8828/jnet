using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_sampleData : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_sampleData));

	string IMigrationMetadata.Id => "201706021002284_update_sampleData";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		DropForeignKey("dbo.SampleDatas", "PatientId", "dbo.Nurses");
		DropForeignKey("dbo.SampleDatas", "PatientId", "dbo.Patients");
		DropIndex("dbo.SampleDatas", new string[1] { "PatientId" });
		AddColumn("dbo.Preferences", "Synced", (ColumnBuilder c) => c.Boolean(false));
		AddColumn("dbo.SampleDatas", "NurseCode", (ColumnBuilder c) => c.String());
		AlterColumn("dbo.SampleDatas", "PatientId", (ColumnBuilder c) => c.String());
		DropColumn("dbo.SampleDatas", "PatientName");
		DropColumn("dbo.SampleDatas", "NurseName");
		DropColumn("dbo.SampleDatas", "NurseId");
	}

	public override void Down()
	{
		AddColumn("dbo.SampleDatas", "NurseId", (ColumnBuilder c) => c.Int());
		AddColumn("dbo.SampleDatas", "NurseName", (ColumnBuilder c) => c.String());
		AddColumn("dbo.SampleDatas", "PatientName", (ColumnBuilder c) => c.String());
		AlterColumn("dbo.SampleDatas", "PatientId", (ColumnBuilder c) => c.Int(false));
		DropColumn("dbo.SampleDatas", "NurseCode");
		DropColumn("dbo.Preferences", "Synced");
		CreateIndex("dbo.SampleDatas", "PatientId");
		AddForeignKey("dbo.SampleDatas", "PatientId", "dbo.Patients", "Id", cascadeDelete: true);
		AddForeignKey("dbo.SampleDatas", "PatientId", "dbo.Nurses", "Id", cascadeDelete: true);
	}
}
