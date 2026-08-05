using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace Yzkj.Novanet.Utility;

public class MD5Encrypt
{
	public static string Encrypt(string str)
	{
		byte[] result = ((HashAlgorithm)CryptoConfig.CreateFromName("MD5")).ComputeHash(Encoding.UTF8.GetBytes(str));
		StringBuilder output = new StringBuilder(16);
		for (int i = 0; i < result.Length; i++)
		{
			output.Append(result[i].ToString("x2", CultureInfo.InvariantCulture));
		}
		return output.ToString();
	}
}
