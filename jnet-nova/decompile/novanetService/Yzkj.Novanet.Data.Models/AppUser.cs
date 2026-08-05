using System;
using System.Security.Claims;
using System.Threading.Tasks;
using Microsoft.AspNet.Identity;
using Microsoft.AspNet.Identity.EntityFramework;

namespace Yzkj.Novanet.Data.Models;

public class AppUser : IdentityUser
{
	public string ClearPassword { get; set; }

	public DateTime? LastTime { get; set; }

	public async Task<ClaimsIdentity> GenerateUserIdentityAsync(UserManager<AppUser> manager)
	{
		return await manager.CreateIdentityAsync(this, "ApplicationCookie");
	}
}
