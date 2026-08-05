using System.Security.Claims;
using System.Threading.Tasks;
using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.Owin;
using Microsoft.Owin;
using Microsoft.Owin.Security;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class AppSignInManager : SignInManager<AppUser, string>
{
	public AppSignInManager(AppUserManager userManager, IAuthenticationManager authenticationManager)
		: base((UserManager<AppUser, string>)userManager, authenticationManager)
	{
	}

	public override Task<ClaimsIdentity> CreateUserIdentityAsync(AppUser user)
	{
		return user.GenerateUserIdentityAsync((AppUserManager)base.UserManager);
	}

	public static AppSignInManager Create(IdentityFactoryOptions<AppSignInManager> options, IOwinContext context)
	{
		return new AppSignInManager(context.GetUserManager<AppUserManager>(), context.Authentication);
	}
}
