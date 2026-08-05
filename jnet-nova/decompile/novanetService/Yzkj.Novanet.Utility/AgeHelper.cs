using System;

namespace Yzkj.Novanet.Utility;

public class AgeHelper
{
	public static int CalcAge(DateTime bday, DateTime today)
	{
		int age = 0;
		bday = bday.AddYears(1);
		while (bday < today)
		{
			age++;
			bday = bday.AddYears(1);
		}
		return age;
	}
}
