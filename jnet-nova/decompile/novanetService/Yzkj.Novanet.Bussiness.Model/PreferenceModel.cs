namespace Yzkj.Novanet.Bussiness.Model;

public class PreferenceModel
{
	public int Id { get; set; }

	public bool AutoReConnect { get; set; }

	public int? CycleMinutes { get; set; }

	public int? PatientID { get; set; }
}
