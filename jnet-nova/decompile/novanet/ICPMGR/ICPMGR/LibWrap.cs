using System.Runtime.InteropServices;

namespace ICPMGR;

public class LibWrap
{
	[DllImport("Kernel32.dll", CharSet = CharSet.Auto)]
	public static extern void ExitProcess([In] uint uExitCode);

	[DllImport("Kernel32.dll", CharSet = CharSet.Auto)]
	public static extern void ExitThread([In] uint uExitCode);
}
