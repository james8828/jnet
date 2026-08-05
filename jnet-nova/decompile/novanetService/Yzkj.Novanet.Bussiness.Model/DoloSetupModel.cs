namespace Yzkj.Novanet.Bussiness.Model;

public class DoloSetupModel
{
	public int Id { get; set; }

	public bool DockLockSupOverrideEnable { get; set; }

	public string ArchivedObsRetainDays { get; set; }

	public bool ArchivedOvrwDisregardArchBit { get; set; }

	public string DockLockAlertMins { get; set; }

	public string DockLockModeCd { get; set; }

	public string DockLockInterval { get; set; }

	public string DockLockShiftTimes { get; set; }

	public string DockLockElapsedHrs { get; set; }
}
