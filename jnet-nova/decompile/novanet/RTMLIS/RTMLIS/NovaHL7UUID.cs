using System;

namespace RTMLIS;

public class NovaHL7UUID
{
	public string GetNovaHL7UUID()
	{
		return DateTime.Now.ToString("MMddHHmmss.fff");
	}
}
