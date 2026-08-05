namespace Yzkj.Novanet.Bussiness.Model;

public class DiceSetupModel
{
	public int Id { get; set; }

	public bool DxIdListEnable { get; set; }

	public bool DxIdPromptEnable { get; set; }

	public bool DxIdScanRequireAccept { get; set; }

	public bool DxIdSupOverrideEnable { get; set; }

	public bool DxIdValidation { get; set; }

	public string DxId2dSEnableCd { get; set; }

	public string DxIdScanEnableCd { get; set; }
}
