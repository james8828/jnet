using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Xml.Serialization;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness.Bus;

public class NovaSyncBus
{
	private readonly NovaDbContext DbContext;

	public NovaSyncBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public void SaveDeviceConnect(DeviceModel model)
	{
		Device device = DbContext.Devices.FirstOrDefault((Device e) => e.SerialNo == model.SerialNo && e.DeviceId == model.DeviceId);
		if (device == null)
		{
			device = new Device
			{
				SerialNo = model.SerialNo,
				DeviceId = model.DeviceId,
				Name = model.Name,
				Hospital = model.Hospital,
				Depart = model.Depart,
				LastTime = DateTime.Now
			};
			DbContext.Devices.Add(device);
		}
		else
		{
			device.SerialNo = model.SerialNo;
			device.DeviceId = model.DeviceId;
			device.Name = model.Name;
			device.Hospital = model.Hospital;
			device.Depart = model.Depart;
			device.LastTime = DateTime.Now;
		}
		DbContext.SaveChanges();
	}

	public void AddSamples(List<SampleDataModel> models)
	{
		models.ForEach(delegate(SampleDataModel m)
		{
			if (m.ObsType == 1)
			{
				if (DbContext.Set<SampleData>().Count((SampleData e) => e.ObsTime == m.ObsTime && e.SerialNo == m.SerialNo && e.PatientId == m.PatientId) > 0)
				{
					m.Exist = true;
				}
			}
			else if (DbContext.Set<SampleData>().Count((SampleData e) => e.ObsTime == m.ObsTime && e.SerialNo == m.SerialNo) > 0)
			{
				m.Exist = true;
			}
		});
		IEnumerable<SampleData> entities = from m in models
			where !m.Exist
			select new SampleData
			{
				PatientId = m.PatientId,
				NurseCode = m.NurseCode,
				Hospital = m.Hospital,
				Depart = m.Depart,
				Diagcode = m.Diagcode,
				Reuslt = m.Reuslt,
				Unit = m.Unit,
				ObsStatus = m.ObsStatus,
				Interpretation = m.Interpretation,
				NormalLimit = m.NormalLimit,
				CriticalLimit = m.CriticalLimit,
				RgtLot = m.RgtLot,
				ObsTime = m.ObsTime,
				SerialNo = m.SerialNo,
				DeviceId = m.DeviceId,
				ObsType = m.ObsType,
				QcLot = m.QcLot,
				QcLevel = m.QcLevel
			};
		DbContext.Set<SampleData>().AddRange(entities);
		DbContext.SaveChanges();
	}

	public async Task SaveDeviceStatus(DeviceModel model)
	{
		Device device = DbContext.Devices.FirstOrDefault((Device e) => e.SerialNo == model.SerialNo);
		if (device == null)
		{
			device = new Device
			{
				SerialNo = model.SerialNo,
				Hospital = model.Hospital,
				Depart = model.Depart,
				Name = model.Name
			};
			Location location = DbContext.Locations.FirstOrDefault((Location e) => e.Name == model.Depart && e.ParentId.HasValue && e.Parent.Name == model.Hospital && !e.IsDeleted);
			if (location != null)
			{
				device.LocationId = location.Id;
			}
			DbContext.Devices.Add(device);
		}
		else
		{
			device.LastTime = DateTime.Now;
			device.ObservationsUpdateDttm = model.ObservationsUpdateDttm;
			device.OperatorsUpdateDttm = model.OperatorsUpdateDttm;
			device.EventsUpdateDttm = model.EventsUpdateDttm;
			device.PatientsUpdateDttm = model.PatientsUpdateDttm;
			device.SetupUpdateDttm = model.SetupUpdateDttm;
			device.PhysUpdateDttm = model.PhysUpdateDttm;
			device.ReagUpdateDttm = model.ReagUpdateDttm;
			device.LocListUpdateDttm = model.LocListUpdateDttm;
		}
		await DbContext.SaveChangesAsync();
	}

	public List<LocationModel> GetLocations()
	{
		List<LocationModel> list = new List<LocationModel>();
		foreach (Location item in from e in DbContext.Set<Location>().AsNoTracking().Include("Childs")
			where !e.IsDeleted && !e.ParentId.HasValue
			select e)
		{
			LocationModel locationModel = new LocationModel
			{
				Id = item.Id,
				Name = item.Name
			};
			if (item.Childs != null)
			{
				locationModel.Childs = (from c in item.Childs
					where !c.IsDeleted
					select new LocationModel
					{
						Id = c.Id,
						Name = c.Name
					}).ToList();
			}
			list.Add(locationModel);
		}
		return list;
	}

	public NovaSetupModel GetNovaSetup(string hosp, string depart)
	{
		NovaSetup novaSetup = DbContext.Set<NovaSetup>().AsNoTracking().Include("Location.TestRange")
			.Include("Location.LocationDiagcodes")
			.FirstOrDefault((NovaSetup e) => e.Location.Level == 1 && e.Location.Parent.Name == hosp && e.Location.Name == depart && !e.Location.IsDeleted);
		if (novaSetup == null)
		{
			return null;
		}
		NovaSetupModel novaSetupModel = new NovaSetupModel
		{
			Id = novaSetup.Id,
			SaveTime = novaSetup.SaveTime,
			AccnId2DsEnableCd = novaSetup.AccnId2DsEnableCd,
			AccnIdAlphaEnable = novaSetup.AccnIdAlphaEnable,
			AccnIdListEnable = novaSetup.AccnIdListEnable,
			AccnIdNonBarcodeCommentReq = novaSetup.AccnIdNonBarcodeCommentReq,
			AccnIdScanEnable2D = novaSetup.AccnIdScanEnable2D,
			AccnIdScanEnableC128 = novaSetup.AccnIdScanEnableC128,
			AccnIdScanEnableC2o5 = novaSetup.AccnIdScanEnableC2o5,
			AccnIdScanEnableC39 = novaSetup.AccnIdScanEnableC39,
			AccnIdScanEnableC93 = novaSetup.AccnIdScanEnableC93,
			AccnIdScanEnableCbar = novaSetup.AccnIdScanEnableCbar,
			AccnIdScanEnableCd = novaSetup.AccnIdScanEnableCd,
			AccnIdScanMaskFailRejC128 = novaSetup.AccnIdScanMaskFailRejC128,
			AccnIdScanMaskFailRejC2o5 = novaSetup.AccnIdScanMaskFailRejC2o5,
			AccnIdScanMaskFailRejC39 = novaSetup.AccnIdScanMaskFailRejC39,
			AccnIdScanMaskFailRejC93 = novaSetup.AccnIdScanMaskFailRejC93,
			AccnIdScanMaskFailRejCbar = novaSetup.AccnIdScanMaskFailRejCbar,
			AccnIdScanRequireAccept = novaSetup.AccnIdScanRequireAccept,
			AccnIdSm2D = novaSetup.AccnIdSm2D,
			AccnIdSmC128 = novaSetup.AccnIdSmC128,
			AccnIdSmC2o5 = novaSetup.AccnIdSmC2o5,
			AccnIdSmC39 = novaSetup.AccnIdSmC39,
			AccnIdSmC93 = novaSetup.AccnIdSmC93,
			AccnIdSmCbar = novaSetup.AccnIdSmCbar,
			AccnIdSupOverrideEnable = novaSetup.AccnIdSupOverrideEnable,
			AccnIdValidation = novaSetup.AccnIdValidation,
			DxId2dSEnableCd = novaSetup.DxId2dSEnableCd,
			DxIdAlphaEnable = novaSetup.DxIdAlphaEnable,
			DxIdListEnable = novaSetup.DxIdListEnable,
			DxIdNonBarcodeCommentReq = novaSetup.DxIdNonBarcodeCommentReq,
			DxIdPromptEnable = novaSetup.DxIdPromptEnable,
			DxIdScanEnable2D = novaSetup.DxIdScanEnable2D,
			DxIdScanEnableC128 = novaSetup.DxIdScanEnableC128,
			DxIdScanEnableC2o5 = novaSetup.DxIdScanEnableC2o5,
			DxIdScanEnableC39 = novaSetup.DxIdScanEnableC39,
			DxIdScanEnableC93 = novaSetup.DxIdScanEnableC93,
			DxIdScanEnableCbar = novaSetup.DxIdScanEnableCbar,
			DxIdScanEnableCd = novaSetup.DxIdScanEnableCd,
			DxIdScanMaskFailRejC128 = novaSetup.DxIdScanMaskFailRejC128,
			DxIdScanMaskFailRejC2o5 = novaSetup.DxIdScanMaskFailRejC2o5,
			DxIdScanMaskFailRejC39 = novaSetup.DxIdScanMaskFailRejC39,
			DxIdScanMaskFailRejC93 = novaSetup.DxIdScanMaskFailRejC93,
			DxIdScanMaskFailRejCbar = novaSetup.DxIdScanMaskFailRejCbar,
			DxIdScanRequireAccept = novaSetup.DxIdScanRequireAccept,
			DxIdSm2D = novaSetup.DxIdSm2D,
			DxIdSmC128 = novaSetup.DxIdSmC128,
			DxIdSmC2o5 = novaSetup.DxIdSmC2o5,
			DxIdSmC39 = novaSetup.DxIdSmC39,
			DxIdSmC93 = novaSetup.DxIdSmC93,
			DxIdSmCbar = novaSetup.DxIdSmCbar,
			DxIdSupOverrideEnable = novaSetup.DxIdSupOverrideEnable,
			DxIdValidation = novaSetup.DxIdValidation,
			ArchivedObsRetainDays = novaSetup.ArchivedObsRetainDays,
			ArchivedOvrwDisregardArchBit = novaSetup.ArchivedOvrwDisregardArchBit,
			DockLockAlertMins = novaSetup.DockLockAlertMins,
			DockLockElapsedHrs = novaSetup.DockLockElapsedHrs,
			DockLockInterval = novaSetup.DockLockInterval,
			DockLockModeCd = novaSetup.DockLockModeCd,
			DockLockShiftTimes = novaSetup.DockLockShiftTimes,
			DockLockSupOverrideEnable = novaSetup.DockLockSupOverrideEnable,
			AccnIdMaxLength = novaSetup.AccnIdMaxLength,
			AccnIdMinLength = novaSetup.AccnIdMinLength,
			DxIdMaxLength = novaSetup.DxIdMaxLength,
			DxIdMinLength = novaSetup.DxIdMinLength,
			OpLoginMaxLength = novaSetup.OpLoginMaxLength,
			OpLoginMinLength = novaSetup.OpLoginMinLength,
			PatIdMaxLength = novaSetup.PatIdMaxLength,
			PatIdMinLength = novaSetup.PatIdMinLength,
			PhysIdMaxLength = novaSetup.PhysIdMaxLength,
			PhysIdMinLength = novaSetup.PhysIdMinLength,
			LinLot2dSEnableCd = novaSetup.LinLot2dSEnableCd,
			LinLotListEnable = novaSetup.LinLotListEnable,
			LinLotNonBarcodeCmntReq = novaSetup.LinLotNonBarcodeCmntReq,
			LinLotScanEnableCd = novaSetup.LinLotScanEnableCd,
			LinLotScanRequireAccept = novaSetup.LinLotScanRequireAccept,
			LinLotSupOverrideEnable = novaSetup.LinLotSupOverrideEnable,
			LinLotValidation = novaSetup.LinLotValidation,
			PatIdTypeCd = novaSetup.PatIdTypeCd,
			OpLogoffElapsedSecs = novaSetup.OpLogoffElapsedSecs,
			OpLogoffModeCd = novaSetup.OpLogoffModeCd,
			DateFormat = novaSetup.DateFormat,
			MeterMaxLinRec = novaSetup.MeterMaxLinRec,
			MeterMaxPatRec = novaSetup.MeterMaxPatRec,
			MeterMaxProfRec = novaSetup.MeterMaxProfRec,
			MeterMaxQCRec = novaSetup.MeterMaxQCRec,
			TimeFormat = novaSetup.TimeFormat,
			OpLogin2dSEnableCd = novaSetup.OpLogin2dSEnableCd,
			OpLoginAlphaEnable = novaSetup.OpLoginAlphaEnable,
			OpLoginDisplayCd = novaSetup.OpLoginDisplayCd,
			OpLoginNonBarcodeCommentReq = novaSetup.OpLoginNonBarcodeCommentReq,
			OpLoginScanEnable2D = novaSetup.OpLoginScanEnable2D,
			OpLoginScanEnableC128 = novaSetup.OpLoginScanEnableC128,
			OpLoginScanEnableC2o5 = novaSetup.OpLoginScanEnableC2o5,
			OpLoginScanEnableC39 = novaSetup.OpLoginScanEnableC39,
			OpLoginScanEnableC93 = novaSetup.OpLoginScanEnableC93,
			OpLoginScanEnableCbar = novaSetup.OpLoginScanEnableCbar,
			OpLoginScanEnableCd = novaSetup.OpLoginScanEnableCd,
			OpLoginScanMaskFailRejC128 = novaSetup.OpLoginScanMaskFailRejC128,
			OpLoginScanMaskFailRejC2o5 = novaSetup.OpLoginScanMaskFailRejC2o5,
			OpLoginScanMaskFailRejC39 = novaSetup.OpLoginScanMaskFailRejC39,
			OpLoginScanMaskFailRejC93 = novaSetup.OpLoginScanMaskFailRejC93,
			OpLoginScanMaskFailRejCbar = novaSetup.OpLoginScanMaskFailRejCbar,
			OpLoginScanRequireAccept = novaSetup.OpLoginScanRequireAccept,
			OpLoginSm2D = novaSetup.OpLoginSm2D,
			OpLoginSmC128 = novaSetup.OpLoginSmC128,
			OpLoginSmC2o5 = novaSetup.OpLoginSmC2o5,
			OpLoginSmC39 = novaSetup.OpLoginSmC39,
			OpLoginSmC93 = novaSetup.OpLoginSmC93,
			OpLoginSmCbar = novaSetup.OpLoginSmCbar,
			OpLoginSupOverrideEnable = novaSetup.OpLoginSupOverrideEnable,
			OpLoginValidation = novaSetup.OpLoginValidation,
			SupOverrideSupOverrideEnable = novaSetup.SupOverrideSupOverrideEnable,
			SupOvScanEnableCd = novaSetup.SupOvScanEnableCd,
			SupOvScanRequireAccept = novaSetup.SupOvScanRequireAccept,
			PatId2dSEnableCd = novaSetup.PatId2dSEnableCd,
			PatIdAlphaEnable = novaSetup.PatIdAlphaEnable,
			PatIdAutoEnabled = novaSetup.PatIdAutoEnabled,
			PatIdFailCommentReq = novaSetup.PatIdFailCommentReq,
			PatIdFailDowntimeEnable = novaSetup.PatIdFailDowntimeEnable,
			PatIdFailNewPtEnable = novaSetup.PatIdFailNewPtEnable,
			PatIdListEnable = novaSetup.PatIdListEnable,
			PatIdNonBarcodeCommentReq = novaSetup.PatIdNonBarcodeCommentReq,
			PatIdScanEnable2D = novaSetup.PatIdScanEnable2D,
			PatIdScanEnableC128 = novaSetup.PatIdScanEnableC128,
			PatIdScanEnableC2o5 = novaSetup.PatIdScanEnableC2o5,
			PatIdScanEnableC39 = novaSetup.PatIdScanEnableC39,
			PatIdScanEnableC93 = novaSetup.PatIdScanEnableC93,
			PatIdScanEnableCbar = novaSetup.PatIdScanEnableCbar,
			PatIdScanEnableCd = novaSetup.PatIdScanEnableCd,
			PatIdScanMaskFailRejC128 = novaSetup.PatIdScanMaskFailRejC128,
			PatIdScanMaskFailRejC2o5 = novaSetup.PatIdScanMaskFailRejC2o5,
			PatIdScanMaskFailRejC39 = novaSetup.PatIdScanMaskFailRejC39,
			PatIdScanMaskFailRejC93 = novaSetup.PatIdScanMaskFailRejC93,
			PatIdScanMaskFailRejCbar = novaSetup.PatIdScanMaskFailRejCbar,
			PatIdScanRequireAccept = novaSetup.PatIdScanRequireAccept,
			PatIdSm2D = novaSetup.PatIdSm2D,
			PatIdSmC128 = novaSetup.PatIdSmC128,
			PatIdSmC2o5 = novaSetup.PatIdSmC2o5,
			PatIdSmC39 = novaSetup.PatIdSmC39,
			PatIdSmC93 = novaSetup.PatIdSmC93,
			PatIdSmCbar = novaSetup.PatIdSmCbar,
			PatIdSupOverrideEnable = novaSetup.PatIdSupOverrideEnable,
			PatIdTgcEnable = novaSetup.PatIdTgcEnable,
			PatIdValidation = novaSetup.PatIdValidation,
			CommentsFreeTextChartableEnable = novaSetup.CommentsFreeTextChartableEnable,
			CommentsFreeTextEnable = novaSetup.CommentsFreeTextEnable,
			CommentsFreeTextFlaggedEnable = novaSetup.CommentsFreeTextFlaggedEnable,
			LinObsFailCommentReqCd = novaSetup.LinObsFailCommentReqCd,
			LinObsPassCommentReqCd = novaSetup.LinObsPassCommentReqCd,
			LinObsValueDisplay = novaSetup.LinObsValueDisplay,
			ObsAbnormalRangeCommentReq = novaSetup.ObsAbnormalRangeCommentReq,
			ObsCriticalRangeCommentReq = novaSetup.ObsCriticalRangeCommentReq,
			ObsNormalRangeCommentReq = novaSetup.ObsNormalRangeCommentReq,
			ObsRejectEnable = novaSetup.ObsRejectEnable,
			ObsRejectResultCommentReq = novaSetup.ObsRejectResultCommentReq,
			ObsRejectSupOverrideReq = novaSetup.ObsRejectSupOverrideReq,
			ObsReviewNoLogin = novaSetup.ObsReviewNoLogin,
			ObsTechnicalRangeCommentReq = novaSetup.ObsTechnicalRangeCommentReq,
			QcObsFailCommentReqCd = novaSetup.QcObsFailCommentReqCd,
			QcObsPassCommentReqCd = novaSetup.QcObsPassCommentReqCd,
			QcObsValueDisplay = novaSetup.QcObsValueDisplay,
			PhysId2dSEnableCd = novaSetup.PhysId2dSEnableCd,
			PhysIdAlphaEnable = novaSetup.PhysIdAlphaEnable,
			PhysIdListEnable = novaSetup.PhysIdListEnable,
			PhysIdNonBarcodeCommentReq = novaSetup.PhysIdNonBarcodeCommentReq,
			PhysIdPromptEnable = novaSetup.PhysIdPromptEnable,
			PhysIdScanEnable2D = novaSetup.PhysIdScanEnable2D,
			PhysIdScanEnableC128 = novaSetup.PhysIdScanEnableC128,
			PhysIdScanEnableC2o5 = novaSetup.PhysIdScanEnableC2o5,
			PhysIdScanEnableC39 = novaSetup.PhysIdScanEnableC39,
			PhysIdScanEnableC93 = novaSetup.PhysIdScanEnableC93,
			PhysIdScanEnableCbar = novaSetup.PhysIdScanEnableCbar,
			PhysIdScanEnableCd = novaSetup.PhysIdScanEnableCd,
			PhysIdScanMaskFailRejC128 = novaSetup.PhysIdScanMaskFailRejC128,
			PhysIdScanMaskFailRejC2o5 = novaSetup.PhysIdScanMaskFailRejC2o5,
			PhysIdScanMaskFailRejC39 = novaSetup.PhysIdScanMaskFailRejC39,
			PhysIdScanMaskFailRejC93 = novaSetup.PhysIdScanMaskFailRejC93,
			PhysIdScanMaskFailRejCbar = novaSetup.PhysIdScanMaskFailRejCbar,
			PhysIdScanRequireAccept = novaSetup.PhysIdScanRequireAccept,
			PhysIdSm2D = novaSetup.PhysIdSm2D,
			PhysIdSmC128 = novaSetup.PhysIdSmC128,
			PhysIdSmC2o5 = novaSetup.PhysIdSmC2o5,
			PhysIdSmC39 = novaSetup.PhysIdSmC39,
			PhysIdSmC93 = novaSetup.PhysIdSmC93,
			PhysIdSmCbar = novaSetup.PhysIdSmCbar,
			PhysIdSupOverrideEnable = novaSetup.PhysIdSupOverrideEnable,
			PhysIdValidation = novaSetup.PhysIdValidation,
			PrivLevelAdminRenameMeterCd = novaSetup.PrivLevelAdminRenameMeterCd,
			PrivLevelAdminResetFacilityCd = novaSetup.PrivLevelAdminResetFacilityCd,
			PrivLevelAdminSetNetworkCd = novaSetup.PrivLevelAdminSetNetworkCd,
			PrivLevelAdminUnarchiveBitCd = novaSetup.PrivLevelAdminUnarchiveBitCd,
			PrivLevelDockLockOvCd = novaSetup.PrivLevelDockLockOvCd,
			PrivLevelSetDateTimeCd = novaSetup.PrivLevelSetDateTimeCd,
			PrivLevelTesttypeCorrelationCd = novaSetup.PrivLevelTesttypeCorrelationCd,
			PrivLevelTesttypeLinearityCd = novaSetup.PrivLevelTesttypeLinearityCd,
			PrivLevelTesttypeMaintCd = novaSetup.PrivLevelTesttypeMaintCd,
			PrivLevelTesttypeProficiencyCd = novaSetup.PrivLevelTesttypeProficiencyCd,
			PrivLevelTesttypeTrainingCd = novaSetup.PrivLevelTesttypeTrainingCd,
			ProfLot2dSEnableCd = novaSetup.ProfLot2dSEnableCd,
			ProfLotAlphaEnable = novaSetup.ProfLotAlphaEnable,
			ProfLotListEnable = novaSetup.ProfLotListEnable,
			ProfLotMaxLength = novaSetup.ProfLotMaxLength,
			ProfLotMinLength = novaSetup.ProfLotMinLength,
			ProfLotNonBarcodeCommentReq = novaSetup.ProfLotNonBarcodeCommentReq,
			ProfLotScanEnableCd = novaSetup.ProfLotScanEnableCd,
			ProfLotScanRequireAccept = novaSetup.ProfLotScanRequireAccept,
			ProfLotSupOverrideEnable = novaSetup.ProfLotSupOverrideEnable,
			ProfLotValidation = novaSetup.ProfLotValidation,
			ProfRejectEnable = novaSetup.ProfRejectEnable,
			QcLockAlertMins = novaSetup.QcLockAlertMins,
			QcLockElapsedHrs = novaSetup.QcLockElapsedHrs,
			QcLockInterval = novaSetup.QcLockInterval,
			QcLockKetAlertMins = novaSetup.QcLockKetAlertMins,
			QcLockKetElapsedHrs = novaSetup.QcLockKetElapsedHrs,
			QcLockKetInterval = novaSetup.QcLockKetInterval,
			QcLockKetLevel1Req = novaSetup.QcLockKetLevel1Req,
			QcLockKetLevel2Req = novaSetup.QcLockKetLevel2Req,
			QcLockKetLevel3Req = novaSetup.QcLockKetLevel3Req,
			QcLockKetModeCd = novaSetup.QcLockKetModeCd,
			QcLockKetShiftTimes = novaSetup.QcLockKetShiftTimes,
			QcLockLevel1Req = novaSetup.QcLockLevel1Req,
			QcLockLevel2Req = novaSetup.QcLockLevel2Req,
			QcLockLevel3Req = novaSetup.QcLockLevel3Req,
			QcLockLevel4Req = novaSetup.QcLockLevel4Req,
			QcLockModeCd = novaSetup.QcLockModeCd,
			QcLockShiftTimes = novaSetup.QcLockShiftTimes,
			QcLot2dSEnableCd = novaSetup.QcLot2dSEnableCd,
			QcLotListEnable = novaSetup.QcLotListEnable,
			QcLotNonBarcodeCommentReq = novaSetup.QcLotNonBarcodeCommentReq,
			QcLotScanEnableCd = novaSetup.QcLotScanEnableCd,
			QcLotScanRequireAccept = novaSetup.QcLotScanRequireAccept,
			QcLotSupOverrideEnable = novaSetup.QcLotSupOverrideEnable,
			QcLotValidation = novaSetup.QcLotValidation,
			AccnIdPromptText = novaSetup.AccnIdPromptText,
			ObsIdMethodCd = novaSetup.ObsIdMethodCd,
			PatIdPromptText = novaSetup.PatIdPromptText,
			SampleTypeSelectEnable = novaSetup.SampleTypeSelectEnable,
			StripId2dSEnableCd = novaSetup.StripId2dSEnableCd,
			StripIdAutoEnabled = novaSetup.StripIdAutoEnabled,
			StripIdDefaultLastStripId = novaSetup.StripIdDefaultLastStripId,
			StripIdListEnable = novaSetup.StripIdListEnable,
			StripIdNonBarcodeCommentReq = novaSetup.StripIdNonBarcodeCommentReq,
			StripIdScanEnableCd = novaSetup.StripIdScanEnableCd,
			StripIdScanRequireAccept = novaSetup.StripIdScanRequireAccept,
			StripIdSupOverrideEnable = novaSetup.StripIdSupOverrideEnable,
			StripIdValidation = novaSetup.StripIdValidation
		};
		if (novaSetup.Location.TestRange != null)
		{
			novaSetupModel.TestConfig = new TestRangeModel
			{
				Id = novaSetup.Location.TestRange.Id,
				HighCricital = novaSetup.Location.TestRange.HighCricital,
				HighNormal = novaSetup.Location.TestRange.HighNormal,
				LowNormal = novaSetup.Location.TestRange.LowNormal,
				LowCricital = novaSetup.Location.TestRange.LowCricital,
				IC = novaSetup.Location.TestRange.IC,
				SL = novaSetup.Location.TestRange.SL
			};
		}
		if (novaSetup.Location.LocationDiagcodes != null)
		{
			novaSetupModel.DiagCodes = (from ld in novaSetup.Location.LocationDiagcodes
				select ld.Diagcode into c
				select new DiagcodeModel
				{
					Id = c.Id,
					Code = c.Code,
					Description = c.Description
				}).ToList();
		}
		return novaSetupModel;
	}

	public async Task<NovaSetupKVModel> GetDefaultNovaSetupKV()
	{
		string path = AppDomain.CurrentDomain.BaseDirectory + "\\setup_default.xml";
		NovaSetupKVModel result = null;
		if (File.Exists(path))
		{
			using FileStream stream = new FileStream(path, FileMode.Open, FileAccess.Read);
			result = (NovaSetupKVModel)new XmlSerializer(typeof(NovaSetupKVModel)).Deserialize(stream);
		}
		return result;
	}

	public List<NurseModel> GetNurses(string hosp, string depart, int sid = 0, int ps = 20)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().Include("LocationNurses")
			.Include("LocationNurses.Nurse")
			.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null || location.LocationNurses == null)
		{
			return null;
		}
		return (from n in (from ln in location.LocationNurses
				select ln.Nurse into e
				where e.Id > sid && !e.IsDelete
				orderby e.Id
				select e).Take(ps)
			select new NurseModel
			{
				Id = n.Id,
				Code = n.Code,
				Name = n.Name
			}).ToList();
	}

	public List<NurseModel> GetNurses(string hosp, string depart, DateTime last, int sid = 0, int ps = 20)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().Include("LocationNurses")
			.Include("LocationNurses.Nurse")
			.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null || location.LocationNurses == null)
		{
			return null;
		}
		return (from n in (from ln in location.LocationNurses
				select ln.Nurse into e
				where ((e.UpdateTime.HasValue && e.UpdateTime.Value > last) || (e.DeleteTime.HasValue && e.DeleteTime.Value > last) || e.CreateTime > last) && e.Id > sid
				orderby e.Id
				select e).Take(ps)
			orderby n.Id
			select new NurseModel
			{
				Id = n.Id,
				Code = n.Code,
				Name = n.Name,
				IsDeleted = n.IsDelete
			}).ToList();
	}

	public List<PatientModel> GetPatients(string hosp, string depart, int sid = 0, int ps = 20)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().Include("Patients")
			.Include("Preference")
			.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null || location.Patients == null)
		{
			return null;
		}
		int? patId = location.Preference.PatientID;
		return (from p in (from p in location.Patients
				where p.Status == 0 && p.Id > sid
				orderby p.Id
				select p).Take(ps)
			select new PatientModel
			{
				Id = p.Id,
				PatID = patId.Value,
				Account = p.Account,
				MedicalRecord = p.MedicalRecord,
				PatientId = p.PatientId,
				WardNo = p.WardNo,
				BedNo = p.BedNo,
				Gender = (int)p.Gender,
				Name = p.Name,
				Birthday = p.Birthday
			}).ToList();
	}

	public List<ReagentModel> GetReagents(string hosp, string depart, int sid = 0, int ps = 50)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().Include("LocationReagents")
			.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null || location.LocationReagents == null)
		{
			return null;
		}
		return (from n in (from ln in location.LocationReagents
				select ln.Reagent into r
				where r.Expiration > DateTime.Now && r.Id > sid && (r.LotType == 1 || (r.LotType == 2 && r.High.HasValue && r.Low.HasValue))
				orderby r.Id
				select r).Take(ps)
			select new ReagentModel
			{
				Id = n.Id,
				LotNum = n.LotNum,
				LotType = n.LotType,
				Expiration = n.Expiration,
				Low = n.Low,
				High = n.High
			}).ToList();
	}

	public bool ExistLocation(string hosp, string depart)
	{
		return DbContext.Locations.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted) != null;
	}

	public NovaSTModel GetNovaST(string hosp, string depart)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null)
		{
			return null;
		}
		return new NovaSTModel
		{
			ST_Location = location.ST_Location,
			ST_Setup = location.ST_Setup,
			ST_Nurse = location.ST_Nurse,
			ST_Patient = location.ST_Patient,
			ST_Reagent = location.ST_Reagent
		};
	}

	public PreferenceModel GetPreference(string hosp, string depart)
	{
		Location location = DbContext.Set<Location>().AsNoTracking().Include("Preference")
			.FirstOrDefault((Location e) => e.Level == 1 && e.Parent.Name == hosp && e.Name == depart && !e.IsDeleted);
		if (location == null)
		{
			return null;
		}
		return new PreferenceModel
		{
			Id = location.Id,
			AutoReConnect = location.Preference.AutoReConnect,
			CycleMinutes = location.Preference.CycleMinutes,
			PatientID = location.Preference.PatientID
		};
	}
}
