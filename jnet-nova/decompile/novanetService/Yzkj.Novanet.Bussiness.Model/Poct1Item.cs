using System.Xml.Serialization;

namespace Yzkj.Novanet.Bussiness.Model;

public class Poct1Item
{
	[XmlIgnore]
	public string _v = "";

	[XmlAttribute("V")]
	public string V
	{
		get
		{
			return _v ?? "";
		}
		set
		{
			_v = value;
		}
	}
}
