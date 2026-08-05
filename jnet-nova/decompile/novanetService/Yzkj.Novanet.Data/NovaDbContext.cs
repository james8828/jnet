using System.Data.Entity;
using Microsoft.AspNet.Identity.EntityFramework;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Data;

public class NovaDbContext : IdentityDbContext<AppUser>
{
	public IDbSet<Location> Locations { get; set; }

	public IDbSet<Preference> Preferences { get; set; }

	public IDbSet<TestRange> TestRanges { get; set; }

	public IDbSet<Patient> Patients { get; set; }

	public IDbSet<Nurse> Nurses { get; set; }

	public IDbSet<LocationNurse> LocationNurses { get; set; }

	public IDbSet<Diagcode> Diagcodes { get; set; }

	public IDbSet<DiagcodeGroup> DiagcodeGroups { get; set; }

	public IDbSet<LocationDiagcode> LocationDiagcodes { get; set; }

	public IDbSet<Reagent> Reagents { get; set; }

	public IDbSet<ReagentGroup> ReagentGroups { get; set; }

	public IDbSet<LocationReagent> LocationReagents { get; set; }

	public IDbSet<DischargeClock> DischargeClocks { get; set; }

	public IDbSet<SampleData> SampleDatas { get; set; }

	public IDbSet<Device> Devices { get; set; }

	public IDbSet<NovaSetup> NovaSetups { get; set; }

	public IDbSet<NovaLog> NovaLog { get; set; }

	public IDbSet<NovaSetupGroup> NovaSetupGroup { get; set; }

	static NovaDbContext()
	{
		Database.SetInitializer(new CreateDatabaseIfNotExists<NovaDbContext>());
		Database.SetInitializer(new DbInitializer());
	}

	public NovaDbContext()
		: base("DefaultConnection", false)
	{
	}

	protected override void OnModelCreating(DbModelBuilder modelBuilder)
	{
		base.OnModelCreating(modelBuilder);
	}

	public static NovaDbContext Create()
	{
		return new NovaDbContext();
	}
}
