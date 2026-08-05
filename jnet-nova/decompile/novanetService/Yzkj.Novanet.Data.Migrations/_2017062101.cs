using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class _2017062101 : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(_2017062101));

	string IMigrationMetadata.Id => "201706211013482_2017062101";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		AddColumn("dbo.TestRanges", "SL", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AddColumn("dbo.TestRanges", "IC", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "LowCricital", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "LowNormal", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "HighNormal", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "HighCricital", (ColumnBuilder c) => c.Decimal(null, (byte)18, (byte)2));
	}

	public override void Down()
	{
		AlterColumn("dbo.TestRanges", "HighCricital", (ColumnBuilder c) => c.Decimal(false, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "HighNormal", (ColumnBuilder c) => c.Decimal(false, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "LowNormal", (ColumnBuilder c) => c.Decimal(false, (byte)18, (byte)2));
		AlterColumn("dbo.TestRanges", "LowCricital", (ColumnBuilder c) => c.Decimal(false, (byte)18, (byte)2));
		DropColumn("dbo.TestRanges", "IC");
		DropColumn("dbo.TestRanges", "SL");
	}
}
