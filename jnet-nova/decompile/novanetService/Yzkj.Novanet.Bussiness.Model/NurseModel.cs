using System.Collections.Generic;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Model;

public class NurseModel
{
	public int? Id { get; set; }

	public string Code { get; set; }

	public string Name { get; set; }

	public string hiskey { get; set; }

	public List<string> LocationsId { get; set; }

	public List<string> LocationsName { get; set; }

	public virtual IList<LocationNurse> LocationNurses { get; set; }

	public bool IsDeleted { get; set; }

	public string Method { get; set; }

	public string PermissionLevel { get; set; }

	public NurseModel()
	{
		Method = "Glu";
		PermissionLevel = "1";
	}
}
