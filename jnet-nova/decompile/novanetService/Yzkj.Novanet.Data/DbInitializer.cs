using System;
using System.Data.Entity;
using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.EntityFramework;
using NLog;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Data;

public class DbInitializer : IDatabaseInitializer<NovaDbContext>
{
	public void InitializeDatabase(NovaDbContext context)
	{
		Seed(context);
	}

	protected void Seed(NovaDbContext context)
	{
		Logger logger = LogManager.GetCurrentClassLogger();
		try
		{
			using (RoleManager<IdentityRole> roleManager = new RoleManager<IdentityRole>(new RoleStore<IdentityRole>(context)))
			{
				roleManager.Create(new IdentityRole("admin"));
				roleManager.Create(new IdentityRole("reader"));
			}
			using UserManager<AppUser> userManager = new UserManager<AppUser>(new UserStore<AppUser>(context));
			AppUser adminUser = new AppUser
			{
				Id = Guid.NewGuid().ToString(),
				UserName = "admin",
				ClearPassword = "123456",
				Email = "admin@nova.net"
			};
			IdentityResult result = userManager.Create(adminUser, adminUser.ClearPassword);
			if (!result.Succeeded)
			{
				logger.Error(result.Errors);
			}
			result = userManager.AddToRole(adminUser.Id, "admin");
			if (!result.Succeeded)
			{
				logger.Error(result.Errors);
			}
			AppUser readonlyUser = new AppUser
			{
				Id = Guid.NewGuid().ToString(),
				UserName = "reader",
				ClearPassword = "123456",
				Email = "reader@nova.net"
			};
			result = userManager.Create(readonlyUser, readonlyUser.ClearPassword);
			if (!result.Succeeded)
			{
				logger.Error(result.Errors);
			}
			result = userManager.AddToRole(readonlyUser.Id, "reader");
			if (!result.Succeeded)
			{
				logger.Error(result.Errors);
			}
		}
		catch (Exception ex)
		{
			logger.Error(ex, ex.Message);
		}
	}
}
