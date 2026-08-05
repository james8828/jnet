using System.Collections.Generic;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Model;

public class DiagcodeModel
{
	public int Id { get; set; }

	public string Code { get; set; }

	public string Description { get; set; }

	public virtual IList<DiagcodeGroup> Groups { get; set; }

	public virtual IList<LocationDiagcode> LocationDiagcodes { get; set; }
}
