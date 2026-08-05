namespace Yzkj.Novanet.Bussiness.Model;

public class PhieSetupModel
{
	public int Id { get; set; }

	public bool PhysIdAlphaEnable { get; set; }

	public bool PhysIdListEnable { get; set; }

	public bool PhysIdPromptEnable { get; set; }

	public string PhysId2dSEnableCd { get; set; }

	public string PhysIdScanEnableCd { get; set; }

	public bool PhysIdScanRequireAccept { get; set; }

	public bool PhysIdSupOverrideEnable { get; set; }

	public bool PhysIdValidation { get; set; }
}
