namespace Yzkj.Novanet.Bussiness.Model;

public class DischargeClockModel
{
	public int Id { get; set; }

	public bool IsEnabled { get; set; }

	public int Hour { get; set; }

	public int Minute { get; set; }

	public string HospitalName { get; set; }

	public string DepartName { get; set; }

	public int LocationId { get; set; }
}
