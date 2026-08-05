using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class LocationBus
{
	private readonly NovaDbContext DbContext;

	private NovaSetupBus NovaSetupBus;

	public LocationBus(NovaSetupBus novaSetupBus, NovaDbContext dbContext)
	{
		NovaSetupBus = novaSetupBus;
		DbContext = dbContext;
	}

	public async Task AddLocation(string name, int level, int? parentid = null)
	{
		Location entity = new Location
		{
			Name = name,
			ParentId = parentid,
			Level = level
		};
		DbContext.Locations.Add(entity);
		if (entity.Level == 1 && entity.ParentId.HasValue)
		{
			Preference entity2 = new Preference
			{
				Id = entity.Id,
				AutoReConnect = false,
				CycleMinutes = null,
				PatientID = 2
			};
			DbContext.Preferences.Add(entity2);
			NovaSetupModel novaSetupModel = (await NovaSetupBus.GetDefaultNovaSetupKV()).ToNovaSetup();
			NovaSetup entity3 = new NovaSetup
			{
				LocationId = entity.Id,
				AccnId2DsEnableCd = novaSetupModel.AccnId2DsEnableCd,
				AccnIdAlphaEnable = novaSetupModel.AccnIdAlphaEnable,
				AccnIdListEnable = novaSetupModel.AccnIdListEnable,
				AccnIdNonBarcodeCommentReq = novaSetupModel.AccnIdNonBarcodeCommentReq,
				AccnIdScanEnable2D = novaSetupModel.AccnIdScanEnable2D,
				AccnIdScanEnableC128 = novaSetupModel.AccnIdScanEnableC128,
				AccnIdScanEnableC2o5 = novaSetupModel.AccnIdScanEnableC2o5,
				AccnIdScanEnableC39 = novaSetupModel.AccnIdScanEnableC39,
				AccnIdScanEnableC93 = novaSetupModel.AccnIdScanEnableC93,
				AccnIdScanEnableCbar = novaSetupModel.AccnIdScanEnableCbar,
				AccnIdScanEnableCd = novaSetupModel.AccnIdScanEnableCd,
				AccnIdScanMaskFailRejC128 = novaSetupModel.AccnIdScanMaskFailRejC128,
				AccnIdScanMaskFailRejC2o5 = novaSetupModel.AccnIdScanMaskFailRejC2o5,
				AccnIdScanMaskFailRejC39 = novaSetupModel.AccnIdScanMaskFailRejC39,
				AccnIdScanMaskFailRejC93 = novaSetupModel.AccnIdScanMaskFailRejC93,
				AccnIdScanMaskFailRejCbar = novaSetupModel.AccnIdScanMaskFailRejCbar,
				AccnIdScanRequireAccept = novaSetupModel.AccnIdScanRequireAccept,
				AccnIdSm2D = novaSetupModel.AccnIdSm2D,
				AccnIdSmC128 = novaSetupModel.AccnIdSmC128,
				AccnIdSmC2o5 = novaSetupModel.AccnIdSmC2o5,
				AccnIdSmC39 = novaSetupModel.AccnIdSmC39,
				AccnIdSmC93 = novaSetupModel.AccnIdSmC93,
				AccnIdSmCbar = novaSetupModel.AccnIdSmCbar,
				AccnIdSupOverrideEnable = novaSetupModel.AccnIdSupOverrideEnable,
				AccnIdValidation = novaSetupModel.AccnIdValidation,
				DxId2dSEnableCd = novaSetupModel.DxId2dSEnableCd,
				DxIdAlphaEnable = novaSetupModel.DxIdAlphaEnable,
				DxIdListEnable = novaSetupModel.DxIdListEnable,
				DxIdNonBarcodeCommentReq = novaSetupModel.DxIdNonBarcodeCommentReq,
				DxIdPromptEnable = novaSetupModel.DxIdPromptEnable,
				DxIdScanEnable2D = novaSetupModel.DxIdScanEnable2D,
				DxIdScanEnableC128 = novaSetupModel.DxIdScanEnableC128,
				DxIdScanEnableC2o5 = novaSetupModel.DxIdScanEnableC2o5,
				DxIdScanEnableC39 = novaSetupModel.DxIdScanEnableC39,
				DxIdScanEnableC93 = novaSetupModel.DxIdScanEnableC93,
				DxIdScanEnableCbar = novaSetupModel.DxIdScanEnableCbar,
				DxIdScanEnableCd = novaSetupModel.DxIdScanEnableCd,
				DxIdScanMaskFailRejC128 = novaSetupModel.DxIdScanMaskFailRejC128,
				DxIdScanMaskFailRejC2o5 = novaSetupModel.DxIdScanMaskFailRejC2o5,
				DxIdScanMaskFailRejC39 = novaSetupModel.DxIdScanMaskFailRejC39,
				DxIdScanMaskFailRejC93 = novaSetupModel.DxIdScanMaskFailRejC93,
				DxIdScanMaskFailRejCbar = novaSetupModel.DxIdScanMaskFailRejCbar,
				DxIdScanRequireAccept = novaSetupModel.DxIdScanRequireAccept,
				DxIdSm2D = novaSetupModel.DxIdSm2D,
				DxIdSmC128 = novaSetupModel.DxIdSmC128,
				DxIdSmC2o5 = novaSetupModel.DxIdSmC2o5,
				DxIdSmC39 = novaSetupModel.DxIdSmC39,
				DxIdSmC93 = novaSetupModel.DxIdSmC93,
				DxIdSmCbar = novaSetupModel.DxIdSmCbar,
				DxIdSupOverrideEnable = novaSetupModel.DxIdSupOverrideEnable,
				DxIdValidation = novaSetupModel.DxIdValidation,
				ArchivedObsRetainDays = novaSetupModel.ArchivedObsRetainDays,
				ArchivedOvrwDisregardArchBit = novaSetupModel.ArchivedOvrwDisregardArchBit,
				DockLockAlertMins = novaSetupModel.DockLockAlertMins,
				DockLockElapsedHrs = novaSetupModel.DockLockElapsedHrs,
				DockLockInterval = novaSetupModel.DockLockInterval,
				DockLockModeCd = novaSetupModel.DockLockModeCd,
				DockLockShiftTimes = novaSetupModel.DockLockShiftTimes,
				DockLockSupOverrideEnable = novaSetupModel.DockLockSupOverrideEnable,
				AccnIdMaxLength = novaSetupModel.AccnIdMaxLength,
				AccnIdMinLength = novaSetupModel.AccnIdMinLength,
				DxIdMaxLength = novaSetupModel.DxIdMaxLength,
				DxIdMinLength = novaSetupModel.DxIdMinLength,
				OpLoginMaxLength = novaSetupModel.OpLoginMaxLength,
				OpLoginMinLength = novaSetupModel.OpLoginMinLength,
				PatIdMaxLength = novaSetupModel.PatIdMaxLength,
				PatIdMinLength = novaSetupModel.PatIdMinLength,
				PhysIdMaxLength = novaSetupModel.PhysIdMaxLength,
				PhysIdMinLength = novaSetupModel.PhysIdMinLength,
				LinLot2dSEnableCd = novaSetupModel.LinLot2dSEnableCd,
				LinLotListEnable = novaSetupModel.LinLotListEnable,
				LinLotNonBarcodeCmntReq = novaSetupModel.LinLotNonBarcodeCmntReq,
				LinLotScanEnableCd = novaSetupModel.LinLotScanEnableCd,
				LinLotScanRequireAccept = novaSetupModel.LinLotScanRequireAccept,
				LinLotSupOverrideEnable = novaSetupModel.LinLotSupOverrideEnable,
				LinLotValidation = novaSetupModel.LinLotValidation,
				PatIdTypeCd = novaSetupModel.PatIdTypeCd,
				OpLogoffElapsedSecs = novaSetupModel.OpLogoffElapsedSecs,
				OpLogoffModeCd = novaSetupModel.OpLogoffModeCd,
				DateFormat = novaSetupModel.DateFormat,
				MeterMaxLinRec = novaSetupModel.MeterMaxLinRec,
				MeterMaxPatRec = novaSetupModel.MeterMaxPatRec,
				MeterMaxProfRec = novaSetupModel.MeterMaxProfRec,
				MeterMaxQCRec = novaSetupModel.MeterMaxQCRec,
				TimeFormat = novaSetupModel.TimeFormat,
				OpLogin2dSEnableCd = novaSetupModel.OpLogin2dSEnableCd,
				OpLoginAlphaEnable = novaSetupModel.OpLoginAlphaEnable,
				OpLoginDisplayCd = novaSetupModel.OpLoginDisplayCd,
				OpLoginNonBarcodeCommentReq = novaSetupModel.OpLoginNonBarcodeCommentReq,
				OpLoginScanEnable2D = novaSetupModel.OpLoginScanEnable2D,
				OpLoginScanEnableC128 = novaSetupModel.OpLoginScanEnableC128,
				OpLoginScanEnableC2o5 = novaSetupModel.OpLoginScanEnableC2o5,
				OpLoginScanEnableC39 = novaSetupModel.OpLoginScanEnableC39,
				OpLoginScanEnableC93 = novaSetupModel.OpLoginScanEnableC93,
				OpLoginScanEnableCbar = novaSetupModel.OpLoginScanEnableCbar,
				OpLoginScanEnableCd = novaSetupModel.OpLoginScanEnableCd,
				OpLoginScanMaskFailRejC128 = novaSetupModel.OpLoginScanMaskFailRejC128,
				OpLoginScanMaskFailRejC2o5 = novaSetupModel.OpLoginScanMaskFailRejC2o5,
				OpLoginScanMaskFailRejC39 = novaSetupModel.OpLoginScanMaskFailRejC39,
				OpLoginScanMaskFailRejC93 = novaSetupModel.OpLoginScanMaskFailRejC93,
				OpLoginScanMaskFailRejCbar = novaSetupModel.OpLoginScanMaskFailRejCbar,
				OpLoginScanRequireAccept = novaSetupModel.OpLoginScanRequireAccept,
				OpLoginSm2D = novaSetupModel.OpLoginSm2D,
				OpLoginSmC128 = novaSetupModel.OpLoginSmC128,
				OpLoginSmC2o5 = novaSetupModel.OpLoginSmC2o5,
				OpLoginSmC39 = novaSetupModel.OpLoginSmC39,
				OpLoginSmC93 = novaSetupModel.OpLoginSmC93,
				OpLoginSmCbar = novaSetupModel.OpLoginSmCbar,
				OpLoginSupOverrideEnable = novaSetupModel.OpLoginSupOverrideEnable,
				OpLoginValidation = novaSetupModel.OpLoginValidation,
				SupOverrideSupOverrideEnable = novaSetupModel.SupOverrideSupOverrideEnable,
				SupOvScanEnableCd = novaSetupModel.SupOvScanEnableCd,
				SupOvScanRequireAccept = novaSetupModel.SupOvScanRequireAccept,
				PatId2dSEnableCd = novaSetupModel.PatId2dSEnableCd,
				PatIdAlphaEnable = novaSetupModel.PatIdAlphaEnable,
				PatIdAutoEnabled = novaSetupModel.PatIdAutoEnabled,
				PatIdFailCommentReq = novaSetupModel.PatIdFailCommentReq,
				PatIdFailDowntimeEnable = novaSetupModel.PatIdFailDowntimeEnable,
				PatIdFailNewPtEnable = novaSetupModel.PatIdFailNewPtEnable,
				PatIdListEnable = novaSetupModel.PatIdListEnable,
				PatIdNonBarcodeCommentReq = novaSetupModel.PatIdNonBarcodeCommentReq,
				PatIdScanEnable2D = novaSetupModel.PatIdScanEnable2D,
				PatIdScanEnableC128 = novaSetupModel.PatIdScanEnableC128,
				PatIdScanEnableC2o5 = novaSetupModel.PatIdScanEnableC2o5,
				PatIdScanEnableC39 = novaSetupModel.PatIdScanEnableC39,
				PatIdScanEnableC93 = novaSetupModel.PatIdScanEnableC93,
				PatIdScanEnableCbar = novaSetupModel.PatIdScanEnableCbar,
				PatIdScanEnableCd = novaSetupModel.PatIdScanEnableCd,
				PatIdScanMaskFailRejC128 = novaSetupModel.PatIdScanMaskFailRejC128,
				PatIdScanMaskFailRejC2o5 = novaSetupModel.PatIdScanMaskFailRejC2o5,
				PatIdScanMaskFailRejC39 = novaSetupModel.PatIdScanMaskFailRejC39,
				PatIdScanMaskFailRejC93 = novaSetupModel.PatIdScanMaskFailRejC93,
				PatIdScanMaskFailRejCbar = novaSetupModel.PatIdScanMaskFailRejCbar,
				PatIdScanRequireAccept = novaSetupModel.PatIdScanRequireAccept,
				PatIdSm2D = novaSetupModel.PatIdSm2D,
				PatIdSmC128 = novaSetupModel.PatIdSmC128,
				PatIdSmC2o5 = novaSetupModel.PatIdSmC2o5,
				PatIdSmC39 = novaSetupModel.PatIdSmC39,
				PatIdSmC93 = novaSetupModel.PatIdSmC93,
				PatIdSmCbar = novaSetupModel.PatIdSmCbar,
				PatIdSupOverrideEnable = novaSetupModel.PatIdSupOverrideEnable,
				PatIdTgcEnable = novaSetupModel.PatIdTgcEnable,
				PatIdValidation = novaSetupModel.PatIdValidation,
				CommentsFreeTextChartableEnable = novaSetupModel.CommentsFreeTextChartableEnable,
				CommentsFreeTextEnable = novaSetupModel.CommentsFreeTextEnable,
				CommentsFreeTextFlaggedEnable = novaSetupModel.CommentsFreeTextFlaggedEnable,
				LinObsFailCommentReqCd = novaSetupModel.LinObsFailCommentReqCd,
				LinObsPassCommentReqCd = novaSetupModel.LinObsPassCommentReqCd,
				LinObsValueDisplay = novaSetupModel.LinObsValueDisplay,
				ObsAbnormalRangeCommentReq = novaSetupModel.ObsAbnormalRangeCommentReq,
				ObsCriticalRangeCommentReq = novaSetupModel.ObsCriticalRangeCommentReq,
				ObsNormalRangeCommentReq = novaSetupModel.ObsNormalRangeCommentReq,
				ObsRejectEnable = novaSetupModel.ObsRejectEnable,
				ObsRejectResultCommentReq = novaSetupModel.ObsRejectResultCommentReq,
				ObsRejectSupOverrideReq = novaSetupModel.ObsRejectSupOverrideReq,
				ObsReviewNoLogin = novaSetupModel.ObsReviewNoLogin,
				ObsTechnicalRangeCommentReq = novaSetupModel.ObsTechnicalRangeCommentReq,
				QcObsFailCommentReqCd = novaSetupModel.QcObsFailCommentReqCd,
				QcObsPassCommentReqCd = novaSetupModel.QcObsPassCommentReqCd,
				QcObsValueDisplay = novaSetupModel.QcObsValueDisplay,
				PhysId2dSEnableCd = novaSetupModel.PhysId2dSEnableCd,
				PhysIdAlphaEnable = novaSetupModel.PhysIdAlphaEnable,
				PhysIdListEnable = novaSetupModel.PhysIdListEnable,
				PhysIdNonBarcodeCommentReq = novaSetupModel.PhysIdNonBarcodeCommentReq,
				PhysIdPromptEnable = novaSetupModel.PhysIdPromptEnable,
				PhysIdScanEnable2D = novaSetupModel.PhysIdScanEnable2D,
				PhysIdScanEnableC128 = novaSetupModel.PhysIdScanEnableC128,
				PhysIdScanEnableC2o5 = novaSetupModel.PhysIdScanEnableC2o5,
				PhysIdScanEnableC39 = novaSetupModel.PhysIdScanEnableC39,
				PhysIdScanEnableC93 = novaSetupModel.PhysIdScanEnableC93,
				PhysIdScanEnableCbar = novaSetupModel.PhysIdScanEnableCbar,
				PhysIdScanEnableCd = novaSetupModel.PhysIdScanEnableCd,
				PhysIdScanMaskFailRejC128 = novaSetupModel.PhysIdScanMaskFailRejC128,
				PhysIdScanMaskFailRejC2o5 = novaSetupModel.PhysIdScanMaskFailRejC2o5,
				PhysIdScanMaskFailRejC39 = novaSetupModel.PhysIdScanMaskFailRejC39,
				PhysIdScanMaskFailRejC93 = novaSetupModel.PhysIdScanMaskFailRejC93,
				PhysIdScanMaskFailRejCbar = novaSetupModel.PhysIdScanMaskFailRejCbar,
				PhysIdScanRequireAccept = novaSetupModel.PhysIdScanRequireAccept,
				PhysIdSm2D = novaSetupModel.PhysIdSm2D,
				PhysIdSmC128 = novaSetupModel.PhysIdSmC128,
				PhysIdSmC2o5 = novaSetupModel.PhysIdSmC2o5,
				PhysIdSmC39 = novaSetupModel.PhysIdSmC39,
				PhysIdSmC93 = novaSetupModel.PhysIdSmC93,
				PhysIdSmCbar = novaSetupModel.PhysIdSmCbar,
				PhysIdSupOverrideEnable = novaSetupModel.PhysIdSupOverrideEnable,
				PhysIdValidation = novaSetupModel.PhysIdValidation,
				PrivLevelAdminRenameMeterCd = novaSetupModel.PrivLevelAdminRenameMeterCd,
				PrivLevelAdminResetFacilityCd = novaSetupModel.PrivLevelAdminResetFacilityCd,
				PrivLevelAdminSetNetworkCd = novaSetupModel.PrivLevelAdminSetNetworkCd,
				PrivLevelAdminUnarchiveBitCd = novaSetupModel.PrivLevelAdminUnarchiveBitCd,
				PrivLevelDockLockOvCd = novaSetupModel.PrivLevelDockLockOvCd,
				PrivLevelSetDateTimeCd = novaSetupModel.PrivLevelSetDateTimeCd,
				PrivLevelTesttypeCorrelationCd = novaSetupModel.PrivLevelTesttypeCorrelationCd,
				PrivLevelTesttypeLinearityCd = novaSetupModel.PrivLevelTesttypeLinearityCd,
				PrivLevelTesttypeMaintCd = novaSetupModel.PrivLevelTesttypeMaintCd,
				PrivLevelTesttypeProficiencyCd = novaSetupModel.PrivLevelTesttypeProficiencyCd,
				PrivLevelTesttypeTrainingCd = novaSetupModel.PrivLevelTesttypeTrainingCd,
				ProfLot2dSEnableCd = novaSetupModel.ProfLot2dSEnableCd,
				ProfLotAlphaEnable = novaSetupModel.ProfLotAlphaEnable,
				ProfLotListEnable = novaSetupModel.ProfLotListEnable,
				ProfLotMaxLength = novaSetupModel.ProfLotMaxLength,
				ProfLotMinLength = novaSetupModel.ProfLotMinLength,
				ProfLotNonBarcodeCommentReq = novaSetupModel.ProfLotNonBarcodeCommentReq,
				ProfLotScanEnableCd = novaSetupModel.ProfLotScanEnableCd,
				ProfLotScanRequireAccept = novaSetupModel.ProfLotScanRequireAccept,
				ProfLotSupOverrideEnable = novaSetupModel.ProfLotSupOverrideEnable,
				ProfLotValidation = novaSetupModel.ProfLotValidation,
				ProfRejectEnable = novaSetupModel.ProfRejectEnable,
				QcLockAlertMins = novaSetupModel.QcLockAlertMins,
				QcLockElapsedHrs = novaSetupModel.QcLockElapsedHrs,
				QcLockInterval = novaSetupModel.QcLockInterval,
				QcLockKetAlertMins = novaSetupModel.QcLockKetAlertMins,
				QcLockKetElapsedHrs = novaSetupModel.QcLockKetElapsedHrs,
				QcLockKetInterval = novaSetupModel.QcLockKetInterval,
				QcLockKetLevel1Req = novaSetupModel.QcLockKetLevel1Req,
				QcLockKetLevel2Req = novaSetupModel.QcLockKetLevel2Req,
				QcLockKetLevel3Req = novaSetupModel.QcLockKetLevel3Req,
				QcLockKetModeCd = novaSetupModel.QcLockKetModeCd,
				QcLockKetShiftTimes = novaSetupModel.QcLockKetShiftTimes,
				QcLockLevel1Req = novaSetupModel.QcLockLevel1Req,
				QcLockLevel2Req = novaSetupModel.QcLockLevel2Req,
				QcLockLevel3Req = novaSetupModel.QcLockLevel3Req,
				QcLockLevel4Req = novaSetupModel.QcLockLevel4Req,
				QcLockModeCd = novaSetupModel.QcLockModeCd,
				QcLockShiftTimes = novaSetupModel.QcLockShiftTimes,
				QcLot2dSEnableCd = novaSetupModel.QcLot2dSEnableCd,
				QcLotListEnable = novaSetupModel.QcLotListEnable,
				QcLotNonBarcodeCommentReq = novaSetupModel.QcLotNonBarcodeCommentReq,
				QcLotScanEnableCd = novaSetupModel.QcLotScanEnableCd,
				QcLotScanRequireAccept = novaSetupModel.QcLotScanRequireAccept,
				QcLotSupOverrideEnable = novaSetupModel.QcLotSupOverrideEnable,
				QcLotValidation = novaSetupModel.QcLotValidation,
				AccnIdPromptText = novaSetupModel.AccnIdPromptText,
				ObsIdMethodCd = novaSetupModel.ObsIdMethodCd,
				PatIdPromptText = novaSetupModel.PatIdPromptText,
				SampleTypeSelectEnable = novaSetupModel.SampleTypeSelectEnable,
				StripId2dSEnableCd = novaSetupModel.StripId2dSEnableCd,
				StripIdAutoEnabled = novaSetupModel.StripIdAutoEnabled,
				StripIdDefaultLastStripId = novaSetupModel.StripIdDefaultLastStripId,
				StripIdListEnable = novaSetupModel.StripIdListEnable,
				StripIdNonBarcodeCommentReq = novaSetupModel.StripIdNonBarcodeCommentReq,
				StripIdScanEnableCd = novaSetupModel.StripIdScanEnableCd,
				StripIdScanRequireAccept = novaSetupModel.StripIdScanRequireAccept,
				StripIdSupOverrideEnable = novaSetupModel.StripIdSupOverrideEnable,
				StripIdValidation = novaSetupModel.StripIdValidation
			};
			DbContext.NovaSetups.Add(entity3);
			TestRange entity4 = new TestRange
			{
				Id = entity.Id,
				SL = 1,
				IC = 0m
			};
			DbContext.TestRanges.Add(entity4);
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task UpdateLocation(int id, string name, int? parentid = null)
	{
		Location location = DbContext.Locations.FirstOrDefault((Location e) => e.Id == id);
		if (location != null)
		{
			location.Name = name;
			location.ParentId = parentid;
			location.UpdateTime = DateTime.Now;
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task DeleteLocation(int id)
	{
		Location location = DbContext.Locations.FirstOrDefault((Location e) => e.Id == id);
		if (location != null)
		{
			location.IsDeleted = true;
			location.DeleteTime = DateTime.Now;
			await DbContext.SaveChangesAsync();
		}
	}

	public Task<List<LocationModel>> GetLocationsByPid(int? parentid = null)
	{
		return Task.FromResult((from l in DbContext.Set<Location>().AsNoTracking()
			where l.ParentId == parentid && !l.IsDeleted
			select new LocationModel
			{
				Id = l.Id,
				Name = l.Name,
				ParentId = l.ParentId,
				Level = l.Level
			}).ToList());
	}

	public Task<List<LocationModel>> GetLocations()
	{
		return Task.FromResult((from l in DbContext.Set<Location>().Include("Preference").AsNoTracking()
			where !l.IsDeleted
			select new LocationModel
			{
				Id = l.Id,
				Name = l.Name,
				ParentId = l.ParentId,
				Level = l.Level,
				LocationNurses = l.LocationNurses,
				PatientID = l.Preference.PatientID,
				DiagsId = l.LocationDiagcodes.Select((LocationDiagcode e) => e.DiagcodeId).ToList(),
				DiagsName = l.LocationDiagcodes.Select((LocationDiagcode e) => e.Diagcode.Description).ToList(),
				LocationDiagcodes = l.LocationDiagcodes
			}).ToList());
	}
}
