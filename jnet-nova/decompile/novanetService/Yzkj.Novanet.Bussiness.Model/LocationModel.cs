using System.Collections.Generic;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Model;

public class LocationModel
{
	public int Id { get; set; }

	public string Name { get; set; }

	public int? ParentId { get; set; }

	public int Level { get; set; }

	public List<LocationModel> Childs { get; set; }

	public virtual IList<LocationNurse> LocationNurses { get; set; }

	public List<int> DiagsId { get; set; }

	public List<string> DiagsName { get; set; }

	public virtual IList<LocationDiagcode> LocationDiagcodes { get; set; }

	public int? PatientID { get; set; }
}
