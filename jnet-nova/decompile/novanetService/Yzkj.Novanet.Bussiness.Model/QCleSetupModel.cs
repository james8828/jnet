namespace Yzkj.Novanet.Bussiness.Model;

public class QCleSetupModel
{
	public int Id { get; set; }

	public bool QcLotListEnable { get; set; }

	public string QcLot2dSEnableCd { get; set; }

	public string QcLotScanEnableCd { get; set; }

	public bool QcLotScanRequireAccept { get; set; }

	public bool QcLotSupOverrideEnable { get; set; }

	public bool QcLotValidation { get; set; }
}
