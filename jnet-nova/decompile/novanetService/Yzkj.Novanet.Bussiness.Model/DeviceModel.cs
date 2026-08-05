using System;

namespace Yzkj.Novanet.Bussiness.Model;

public class DeviceModel
{
	public int Id { get; set; }

	public string SerialNo { get; set; }

	public string DeviceId { get; set; }

	public string Name { get; set; }

	public string Hospital { get; set; }

	public string Depart { get; set; }

	public int LocationId { get; set; }

	public string LocationName { get; set; }

	public DateTime LastTime { get; set; }

	public DateTime? ObservationsUpdateDttm { get; set; }

	public DateTime? OperatorsUpdateDttm { get; set; }

	public DateTime? EventsUpdateDttm { get; set; }

	public DateTime? PatientsUpdateDttm { get; set; }

	public DateTime? SetupUpdateDttm { get; set; }

	public DateTime? PhysUpdateDttm { get; set; }

	public DateTime? ReagUpdateDttm { get; set; }

	public DateTime? LocListUpdateDttm { get; set; }
}
