using System;
using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.EntityFramework;
using Microsoft.AspNet.Identity.Owin;
using Microsoft.Owin;
using Microsoft.Owin.Security.DataProtection;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class AppUserManager : UserManager<AppUser>
{
	public AppUserManager(IUserStore<AppUser> store)
		: base(store)
	{
	}

	public static AppUserManager Create(IdentityFactoryOptions<AppUserManager> options, IOwinContext context)
	{
		AppUserManager appUserManager = new AppUserManager(new UserStore<AppUser>(context.Get<NovaDbContext>()));
		appUserManager.UserValidator = new UserValidator<AppUser>(appUserManager)
		{
			AllowOnlyAlphanumericUserNames = false,
			RequireUniqueEmail = true
		};
		appUserManager.PasswordValidator = new PasswordValidator
		{
			RequiredLength = 6,
			RequireNonLetterOrDigit = false,
			RequireDigit = false,
			RequireLowercase = false,
			RequireUppercase = false
		};
		appUserManager.UserLockoutEnabledByDefault = true;
		appUserManager.DefaultAccountLockoutTimeSpan = TimeSpan.FromMinutes(5.0);
		appUserManager.MaxFailedAccessAttemptsBeforeLockout = 5;
		IDataProtectionProvider dataProtectionProvider = options.DataProtectionProvider;
		if (dataProtectionProvider != null)
		{
			appUserManager.UserTokenProvider = new DataProtectorTokenProvider<AppUser>(dataProtectionProvider.Create("Novanet"));
		}
		return appUserManager;
	}
}
