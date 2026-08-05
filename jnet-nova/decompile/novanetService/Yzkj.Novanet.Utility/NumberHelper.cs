namespace Yzkj.Novanet.Utility;

public class NumberHelper
{
	public static int GetDecimalLength(decimal value)
	{
		string[] parts = value.ToString().Split('.');
		return (parts.Length != 1) ? parts[1].Length : 0;
	}
}
