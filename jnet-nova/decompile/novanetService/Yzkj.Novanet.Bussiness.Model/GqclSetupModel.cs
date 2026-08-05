namespace Yzkj.Novanet.Bussiness.Model;

public class GqclSetupModel
{
	public int Id { get; set; }

	public string QcLockAlertMins { get; set; }

	public bool QcLockLevel1Req { get; set; }

	public bool QcLockLevel2Req { get; set; }

	public bool QcLockLevel3Req { get; set; }

	public string QcLockModeCd { get; set; }

	public string QcLockInterval { get; set; }

	public string QcLockElapsedHrs { get; set; }

	public string QcLockShiftTimes { get; set; }
}
