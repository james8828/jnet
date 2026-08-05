using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class update_nova_log : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(update_nova_log));

	string IMigrationMetadata.Id => "201707040654575_update_nova_log";

	string IMigrationMetadata.Source => Resources.GetString("Source");

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
	}

	public override void Down()
	{
	}
}
