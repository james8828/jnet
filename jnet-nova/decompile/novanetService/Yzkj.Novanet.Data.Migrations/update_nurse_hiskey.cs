using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_nurse_hiskey : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_nurse_hiskey));

	string IMigrationMetadata.Id => "201706120151264_update_nurse_hiskey";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.Nurses", "hiskey", (ColumnBuilder c) => c.String(null, 64));
		CreateIndex("dbo.Nurses", "hiskey");
	}

	public override void Down()
	{
		DropIndex("dbo.Nurses", new string[1] { "hiskey" });
		DropColumn("dbo.Nurses", "hiskey");
	}
}
