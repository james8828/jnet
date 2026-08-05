namespace Yzkj.Novanet.Bussiness.Model;

public class StleSetupModel
{
	public int Id { get; set; }

	public bool StripIdAutoEnabled { get; set; }

	public bool StripIdDefaultLastStripId { get; set; }

	public bool StripIdListEnable { get; set; }

	public string StripId2dSEnableCd { get; set; }

	public string StripIdScanEnableCd { get; set; }

	public bool StripIdScanRequireAccept { get; set; }

	public bool StripIdSupOverrideEnable { get; set; }

	public bool StripIdValidation { get; set; }
}
