namespace Yzkj.Novanet.Bussiness.Model;

public class PaieSetupModel
{
	public int Id { get; set; }

	public bool PatIdAutoEnabled { get; set; }

	public bool PatIdAlphaEnable { get; set; }

	public bool PatIdFailDowntimeEnable { get; set; }

	public bool PatIdFailNewPtEnable { get; set; }

	public bool PatIdListEnable { get; set; }

	public string PatId2dSEnableCd { get; set; }

	public string PatIdScanEnableCd { get; set; }

	public bool PatIdScanRequireAccept { get; set; }

	public bool PatIdSupOverrideEnable { get; set; }

	public bool PatIdTgcEnable { get; set; }

	public bool PatIdValidation { get; set; }
}
