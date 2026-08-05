namespace Yzkj.Novanet.Bussiness.Model;

public class OploSetupModel
{
	public int Id { get; set; }

	public bool OpLoginScanRequireAccept { get; set; }

	public bool OpLoginAlphaEnable { get; set; }

	public string OpLogin2dSEnableCd { get; set; }

	public string OpLoginScanEnableCd { get; set; }

	public bool OpLoginSupOverrideEnable { get; set; }

	public bool OpLoginValidation { get; set; }

	public string OpLoginDisplayCd { get; set; }

	public bool SupOvScanRequireAccept { get; set; }

	public string SupOvScanEnableCd { get; set; }
}
