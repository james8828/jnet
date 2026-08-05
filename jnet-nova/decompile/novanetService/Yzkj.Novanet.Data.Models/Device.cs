using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Yzkj.Novanet.Data.Models;

public class Device
{
	[DatabaseGenerated(DatabaseGeneratedOption.Identity)]
	public int Id { get; set; }

	[Index]
	[MaxLength(32)]
	public string SerialNo { get; set; }

	[Index]
	[MaxLength(32)]
	public string DeviceId { get; set; }

	public string Name { get; set; }

	public string Hospital { get; set; }

	public string Depart { get; set; }

	[Index]
	public int? LocationId { get; set; }

	[ForeignKey("LocationId")]
	public virtual Location Location { get; set; }

	[Index]
	public DateTime LastTime { get; set; }

	public DateTime? ObservationsUpdateDttm { get; set; }

	public DateTime? OperatorsUpdateDttm { get; set; }

	public DateTime? EventsUpdateDttm { get; set; }

	public DateTime? PatientsUpdateDttm { get; set; }

	public DateTime? SetupUpdateDttm { get; set; }

	public DateTime? PhysUpdateDttm { get; set; }

	public DateTime? ReagUpdateDttm { get; set; }

	public DateTime? LocListUpdateDttm { get; set; }

	public Device()
	{
		LastTime = DateTime.Now;
	}
}
