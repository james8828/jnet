using System;

namespace Yzkj.Novanet.Utility;

public static class GuidHelper
{
	public static Guid NewComb()
	{
		byte[] guidArray = Guid.NewGuid().ToByteArray();
		DateTime dtBase = new DateTime(1900, 1, 1);
		DateTime dtNow = DateTime.Now;
		TimeSpan days = new TimeSpan(dtNow.Ticks - dtBase.Ticks);
		TimeSpan msecs = new TimeSpan(dtNow.Ticks - new DateTime(dtNow.Year, dtNow.Month, dtNow.Day).Ticks);
		byte[] daysArray = BitConverter.GetBytes(days.Days);
		byte[] msecsArray = BitConverter.GetBytes((long)(msecs.TotalMilliseconds / 3.333333));
		Array.Reverse(daysArray);
		Array.Reverse(msecsArray);
		Array.Copy(daysArray, daysArray.Length - 2, guidArray, guidArray.Length - 6, 2);
		Array.Copy(msecsArray, msecsArray.Length - 4, guidArray, guidArray.Length - 4, 4);
		return new Guid(guidArray);
	}

	public static string NewCombGuidString()
	{
		return NewComb().ToString();
	}

	public static DateTime GetDateFromComb(Guid id)
	{
		DateTime baseDate = new DateTime(1900, 1, 1);
		byte[] daysArray = new byte[4];
		byte[] msecsArray = new byte[4];
		byte[] guidArray = id.ToByteArray();
		Array.Copy(guidArray, guidArray.Length - 6, daysArray, 2, 2);
		Array.Copy(guidArray, guidArray.Length - 4, msecsArray, 0, 4);
		Array.Reverse(daysArray);
		Array.Reverse(msecsArray);
		int days = BitConverter.ToInt32(daysArray, 0);
		int msecs = BitConverter.ToInt32(msecsArray, 0);
		return baseDate.AddDays(days).AddMilliseconds((double)msecs * 3.333333);
	}
}
