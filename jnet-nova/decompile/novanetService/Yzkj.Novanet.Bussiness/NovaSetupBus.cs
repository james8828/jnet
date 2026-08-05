using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Xml.Serialization;
using Yzkj.Novanet.Bussiness.Model;
using Yzkj.Novanet.Data;
using Yzkj.Novanet.Data.Models;

namespace Yzkj.Novanet.Bussiness;

public class NovaSetupBus
{
	private readonly NovaDbContext DbContext;

	public NovaSetupBus(NovaDbContext dbContext)
	{
		DbContext = dbContext;
	}

	public async Task AddAcneSetup(AcneSetupModel acneModel)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == acneModel.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = acneModel.Id,
				AccnIdAlphaEnable = (acneModel.AccnIdAlphaEnable ? "1" : "0"),
				AccnIdScanRequireAccept = (acneModel.AccnIdScanRequireAccept ? "1" : "0"),
				AccnId2DsEnableCd = acneModel.AccnId2DsEnableCd,
				AccnIdScanEnableCd = acneModel.AccnId2DsEnableCd
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.AccnIdAlphaEnable = (acneModel.AccnIdAlphaEnable ? "1" : "0");
			novaSetup.AccnIdScanRequireAccept = (acneModel.AccnIdScanRequireAccept ? "1" : "0");
			novaSetup.AccnId2DsEnableCd = acneModel.AccnId2DsEnableCd;
			novaSetup.AccnIdScanEnableCd = acneModel.AccnId2DsEnableCd;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddDiceSetup(DiceSetupModel dice)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == dice.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = dice.Id,
				DxIdListEnable = (dice.DxIdListEnable ? "1" : "0"),
				DxIdPromptEnable = (dice.DxIdPromptEnable ? "1" : "0"),
				DxIdScanRequireAccept = (dice.DxIdScanRequireAccept ? "1" : "0"),
				DxIdSupOverrideEnable = (dice.DxIdSupOverrideEnable ? "1" : "0"),
				DxIdValidation = (dice.DxIdValidation ? "1" : "0"),
				DxId2dSEnableCd = dice.DxId2dSEnableCd,
				DxIdScanEnableCd = dice.DxId2dSEnableCd
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.DxIdListEnable = (dice.DxIdListEnable ? "1" : "0");
			novaSetup.DxIdPromptEnable = (dice.DxIdPromptEnable ? "1" : "0");
			novaSetup.DxIdScanRequireAccept = (dice.DxIdScanRequireAccept ? "1" : "0");
			novaSetup.DxIdSupOverrideEnable = (dice.DxIdSupOverrideEnable ? "1" : "0");
			novaSetup.DxIdValidation = (dice.DxIdValidation ? "1" : "0");
			novaSetup.DxId2dSEnableCd = dice.DxId2dSEnableCd;
			novaSetup.DxIdScanEnableCd = dice.DxId2dSEnableCd;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddDoloSetup(DoloSetupModel dolo)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == dolo.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = dolo.Id,
				DockLockSupOverrideEnable = (dolo.DockLockSupOverrideEnable ? "1" : "0"),
				ArchivedObsRetainDays = dolo.ArchivedObsRetainDays,
				ArchivedOvrwDisregardArchBit = (dolo.ArchivedOvrwDisregardArchBit ? "1" : "0"),
				DockLockAlertMins = dolo.DockLockAlertMins,
				DockLockModeCd = dolo.DockLockModeCd,
				DockLockInterval = dolo.DockLockInterval,
				DockLockShiftTimes = dolo.DockLockShiftTimes,
				DockLockElapsedHrs = dolo.DockLockElapsedHrs
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.DockLockSupOverrideEnable = (dolo.DockLockSupOverrideEnable ? "1" : "0");
			novaSetup.ArchivedObsRetainDays = dolo.ArchivedObsRetainDays;
			novaSetup.ArchivedOvrwDisregardArchBit = (dolo.ArchivedOvrwDisregardArchBit ? "1" : "0");
			novaSetup.DockLockAlertMins = dolo.DockLockAlertMins;
			novaSetup.DockLockModeCd = dolo.DockLockModeCd;
			novaSetup.DockLockInterval = dolo.DockLockInterval;
			novaSetup.DockLockShiftTimes = dolo.DockLockShiftTimes;
			novaSetup.DockLockElapsedHrs = dolo.DockLockElapsedHrs;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddLileSetup(LileSetupModel lile)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == lile.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = lile.Id,
				LinLotListEnable = (lile.LinLotListEnable ? "1" : "0"),
				LinLot2dSEnableCd = lile.LinLot2dSEnableCd,
				LinLotScanEnableCd = lile.LinLot2dSEnableCd,
				LinLotScanRequireAccept = (lile.LinLotScanRequireAccept ? "1" : "0"),
				LinLotSupOverrideEnable = (lile.LinLotSupOverrideEnable ? "1" : "0"),
				LinLotValidation = (lile.LinLotValidation ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.LinLotListEnable = (lile.LinLotListEnable ? "1" : "0");
			novaSetup.LinLot2dSEnableCd = lile.LinLot2dSEnableCd;
			novaSetup.LinLotScanEnableCd = lile.LinLot2dSEnableCd;
			novaSetup.LinLotScanRequireAccept = (lile.LinLotScanRequireAccept ? "1" : "0");
			novaSetup.LinLotSupOverrideEnable = (lile.LinLotSupOverrideEnable ? "1" : "0");
			novaSetup.LinLotValidation = (lile.LinLotValidation ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddLogoffSetup(LogoffSetupModel logoff)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == logoff.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = logoff.Id,
				OpLogoffElapsedSecs = logoff.OpLogoffElapsedSecs,
				OpLogoffModeCd = logoff.OpLogoffModeCd
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.OpLogoffElapsedSecs = logoff.OpLogoffElapsedSecs;
			novaSetup.OpLogoffModeCd = logoff.OpLogoffModeCd;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddOffsetsSetup(OffsetsSetupModel offsets)
	{
		TestRange testRange = DbContext.TestRanges.FirstOrDefault((TestRange l) => l.Id == offsets.Id);
		if (testRange == null)
		{
			TestRange entity = new TestRange
			{
				Id = offsets.Id,
				SL = offsets.SL,
				IC = offsets.IC
			};
			DbContext.TestRanges.Add(entity);
		}
		else
		{
			testRange.SL = offsets.SL;
			testRange.IC = offsets.IC;
			testRange.UpdateTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddOploSetup(OploSetupModel oplo)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == oplo.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = oplo.Id,
				OpLoginScanRequireAccept = (oplo.OpLoginScanRequireAccept ? "1" : "0"),
				OpLoginAlphaEnable = (oplo.OpLoginAlphaEnable ? "1" : "0"),
				OpLogin2dSEnableCd = oplo.OpLogin2dSEnableCd,
				OpLoginScanEnableCd = oplo.OpLogin2dSEnableCd,
				OpLoginSupOverrideEnable = (oplo.OpLoginSupOverrideEnable ? "1" : "0"),
				OpLoginValidation = (oplo.OpLoginValidation ? "1" : "0"),
				OpLoginDisplayCd = oplo.OpLoginDisplayCd,
				SupOvScanRequireAccept = (oplo.SupOvScanRequireAccept ? "1" : "0"),
				SupOvScanEnableCd = oplo.SupOvScanEnableCd
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.OpLoginScanRequireAccept = (oplo.OpLoginScanRequireAccept ? "1" : "0");
			novaSetup.OpLoginAlphaEnable = (oplo.OpLoginAlphaEnable ? "1" : "0");
			novaSetup.OpLogin2dSEnableCd = oplo.OpLogin2dSEnableCd;
			novaSetup.OpLoginScanEnableCd = oplo.OpLogin2dSEnableCd;
			novaSetup.OpLoginSupOverrideEnable = (oplo.OpLoginSupOverrideEnable ? "1" : "0");
			novaSetup.OpLoginValidation = (oplo.OpLoginValidation ? "1" : "0");
			novaSetup.OpLoginDisplayCd = oplo.OpLoginDisplayCd;
			novaSetup.SupOvScanRequireAccept = (oplo.SupOvScanRequireAccept ? "1" : "0");
			novaSetup.SupOvScanEnableCd = oplo.SupOvScanEnableCd;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddPaieSetup(PaieSetupModel paie)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == paie.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = paie.Id,
				PatIdAutoEnabled = (paie.PatIdAutoEnabled ? "1" : "0"),
				PatIdAlphaEnable = (paie.PatIdAlphaEnable ? "1" : "0"),
				PatIdFailDowntimeEnable = (paie.PatIdFailDowntimeEnable ? "1" : "0"),
				PatIdFailNewPtEnable = (paie.PatIdFailNewPtEnable ? "1" : "0"),
				PatIdListEnable = (paie.PatIdListEnable ? "1" : "0"),
				PatId2dSEnableCd = paie.PatId2dSEnableCd,
				PatIdScanEnableCd = paie.PatId2dSEnableCd,
				PatIdScanRequireAccept = (paie.PatIdScanRequireAccept ? "1" : "0"),
				PatIdSupOverrideEnable = (paie.PatIdSupOverrideEnable ? "1" : "0"),
				PatIdTgcEnable = (paie.PatIdTgcEnable ? "1" : "0"),
				PatIdValidation = (paie.PatIdValidation ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.PatIdAutoEnabled = (paie.PatIdAutoEnabled ? "1" : "0");
			novaSetup.PatIdAlphaEnable = (paie.PatIdAlphaEnable ? "1" : "0");
			novaSetup.PatIdFailDowntimeEnable = (paie.PatIdFailDowntimeEnable ? "1" : "0");
			novaSetup.PatIdFailNewPtEnable = (paie.PatIdFailNewPtEnable ? "1" : "0");
			novaSetup.PatIdListEnable = (paie.PatIdListEnable ? "1" : "0");
			novaSetup.PatId2dSEnableCd = paie.PatId2dSEnableCd;
			novaSetup.PatIdScanEnableCd = paie.PatId2dSEnableCd;
			novaSetup.PatIdScanRequireAccept = (paie.PatIdScanRequireAccept ? "1" : "0");
			novaSetup.PatIdSupOverrideEnable = (paie.PatIdSupOverrideEnable ? "1" : "0");
			novaSetup.PatIdTgcEnable = (paie.PatIdTgcEnable ? "1" : "0");
			novaSetup.PatIdValidation = (paie.PatIdValidation ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddResultsSetup(ResultsSetupModel results)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == results.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = results.Id,
				CommentsFreeTextChartableEnable = (results.CommentsFreeTextChartableEnable ? "1" : "0"),
				CommentsFreeTextFlaggedEnable = (results.CommentsFreeTextFlaggedEnable ? "1" : "0"),
				ObsReviewNoLogin = (results.ObsReviewNoLogin ? "1" : "0"),
				LinObsValueDisplay = (results.LinObsValueDisplay ? "1" : "0"),
				CommentsFreeTextEnable = (results.CommentsFreeTextEnable ? "1" : "0"),
				ObsRejectEnable = (results.ObsRejectEnable ? "1" : "0"),
				QcObsValueDisplay = (results.QcObsValueDisplay ? "1" : "0"),
				QcObsFailCommentReqCd = (results.QcObsFailCommentReqCd ? "ALLOW" : "REQ"),
				LinObsFailCommentReqCd = (results.LinObsFailCommentReqCd ? "ALLOW" : "REQ"),
				ObsRejectResultCommentReq = (results.ObsRejectResultCommentReq ? "1" : "0"),
				ObsCriticalRangeCommentReq = (results.ObsCriticalRangeCommentReq ? "1" : "0"),
				ObsTechnicalRangeCommentReq = (results.ObsTechnicalRangeCommentReq ? "1" : "0"),
				QcObsPassCommentReqCd = (results.QcObsPassCommentReqCd ? "ALLOW" : "REQ"),
				LinObsPassCommentReqCd = (results.LinObsPassCommentReqCd ? "ALLOW" : "REQ"),
				ObsNormalRangeCommentReq = (results.ObsNormalRangeCommentReq ? "1" : "0"),
				ObsAbnormalRangeCommentReq = (results.ObsAbnormalRangeCommentReq ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.CommentsFreeTextChartableEnable = (results.CommentsFreeTextChartableEnable ? "1" : "0");
			novaSetup.CommentsFreeTextFlaggedEnable = (results.CommentsFreeTextFlaggedEnable ? "1" : "0");
			novaSetup.ObsReviewNoLogin = (results.ObsReviewNoLogin ? "1" : "0");
			novaSetup.LinObsValueDisplay = (results.LinObsValueDisplay ? "1" : "0");
			novaSetup.CommentsFreeTextEnable = (results.CommentsFreeTextEnable ? "1" : "0");
			novaSetup.ObsRejectEnable = (results.ObsRejectEnable ? "1" : "0");
			novaSetup.QcObsValueDisplay = (results.QcObsValueDisplay ? "1" : "0");
			novaSetup.QcObsFailCommentReqCd = (results.QcObsFailCommentReqCd ? "ALLOW" : "REQ");
			novaSetup.LinObsFailCommentReqCd = (results.LinObsFailCommentReqCd ? "ALLOW" : "REQ");
			novaSetup.ObsRejectResultCommentReq = (results.ObsRejectResultCommentReq ? "1" : "0");
			novaSetup.ObsCriticalRangeCommentReq = (results.ObsCriticalRangeCommentReq ? "1" : "0");
			novaSetup.ObsTechnicalRangeCommentReq = (results.ObsTechnicalRangeCommentReq ? "1" : "0");
			novaSetup.QcObsPassCommentReqCd = (results.QcObsPassCommentReqCd ? "ALLOW" : "REQ");
			novaSetup.LinObsPassCommentReqCd = (results.LinObsPassCommentReqCd ? "ALLOW" : "REQ");
			novaSetup.ObsNormalRangeCommentReq = (results.ObsNormalRangeCommentReq ? "1" : "0");
			novaSetup.ObsAbnormalRangeCommentReq = (results.ObsAbnormalRangeCommentReq ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddPhieSetup(PhieSetupModel phie)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == phie.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = phie.Id,
				PhysIdAlphaEnable = (phie.PhysIdAlphaEnable ? "1" : "0"),
				PhysIdListEnable = (phie.PhysIdListEnable ? "1" : "0"),
				PhysIdPromptEnable = (phie.PhysIdPromptEnable ? "1" : "0"),
				PhysId2dSEnableCd = phie.PhysId2dSEnableCd,
				PhysIdScanEnableCd = phie.PhysId2dSEnableCd,
				PhysIdScanRequireAccept = (phie.PhysIdScanRequireAccept ? "1" : "0"),
				PhysIdSupOverrideEnable = (phie.PhysIdSupOverrideEnable ? "1" : "0"),
				PhysIdValidation = (phie.PhysIdValidation ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.PhysIdAlphaEnable = (phie.PhysIdAlphaEnable ? "1" : "0");
			novaSetup.PhysIdListEnable = (phie.PhysIdListEnable ? "1" : "0");
			novaSetup.PhysIdPromptEnable = (phie.PhysIdPromptEnable ? "1" : "0");
			novaSetup.PhysId2dSEnableCd = phie.PhysId2dSEnableCd;
			novaSetup.PhysIdScanEnableCd = phie.PhysId2dSEnableCd;
			novaSetup.PhysIdScanRequireAccept = (phie.PhysIdScanRequireAccept ? "1" : "0");
			novaSetup.PhysIdSupOverrideEnable = (phie.PhysIdSupOverrideEnable ? "1" : "0");
			novaSetup.PhysIdValidation = (phie.PhysIdValidation ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddPrssSetup(PrssSetupModel prss)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == prss.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = prss.Id,
				PrivLevelAdminRenameMeterCd = prss.PrivLevelAdminRenameMeterCd,
				PrivLevelAdminResetFacilityCd = prss.PrivLevelAdminResetFacilityCd,
				PrivLevelAdminSetNetworkCd = prss.PrivLevelAdminSetNetworkCd,
				PrivLevelAdminUnarchiveBitCd = prss.PrivLevelAdminUnarchiveBitCd,
				PrivLevelTesttypeLinearityCd = prss.PrivLevelTesttypeLinearityCd,
				PrivLevelTesttypeProficiencyCd = prss.PrivLevelTesttypeProficiencyCd,
				PrivLevelDockLockOvCd = prss.PrivLevelDockLockOvCd,
				PrivLevelSetDateTimeCd = prss.PrivLevelSetDateTimeCd
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.PrivLevelAdminRenameMeterCd = prss.PrivLevelAdminRenameMeterCd;
			novaSetup.PrivLevelAdminResetFacilityCd = prss.PrivLevelAdminResetFacilityCd;
			novaSetup.PrivLevelAdminSetNetworkCd = prss.PrivLevelAdminSetNetworkCd;
			novaSetup.PrivLevelAdminUnarchiveBitCd = prss.PrivLevelAdminUnarchiveBitCd;
			novaSetup.PrivLevelTesttypeLinearityCd = prss.PrivLevelTesttypeLinearityCd;
			novaSetup.PrivLevelTesttypeProficiencyCd = prss.PrivLevelTesttypeProficiencyCd;
			novaSetup.PrivLevelDockLockOvCd = prss.PrivLevelDockLockOvCd;
			novaSetup.PrivLevelSetDateTimeCd = prss.PrivLevelSetDateTimeCd;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddGqclSetup(GqclSetupModel gqcl)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == gqcl.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = gqcl.Id,
				QcLockAlertMins = gqcl.QcLockAlertMins,
				QcLockLevel1Req = (gqcl.QcLockLevel1Req ? "1" : "0"),
				QcLockLevel2Req = (gqcl.QcLockLevel2Req ? "1" : "0"),
				QcLockLevel3Req = (gqcl.QcLockLevel3Req ? "1" : "0"),
				QcLockModeCd = gqcl.QcLockModeCd,
				QcLockInterval = gqcl.QcLockInterval,
				QcLockElapsedHrs = gqcl.QcLockElapsedHrs,
				QcLockShiftTimes = gqcl.QcLockShiftTimes
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.QcLockAlertMins = gqcl.QcLockAlertMins;
			novaSetup.QcLockLevel1Req = (gqcl.QcLockLevel1Req ? "1" : "0");
			novaSetup.QcLockLevel2Req = (gqcl.QcLockLevel2Req ? "1" : "0");
			novaSetup.QcLockLevel3Req = (gqcl.QcLockLevel3Req ? "1" : "0");
			novaSetup.QcLockModeCd = gqcl.QcLockModeCd;
			novaSetup.QcLockInterval = gqcl.QcLockInterval;
			novaSetup.QcLockElapsedHrs = gqcl.QcLockElapsedHrs;
			novaSetup.QcLockShiftTimes = gqcl.QcLockShiftTimes;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddQCleSetup(QCleSetupModel qcle)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == qcle.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = qcle.Id,
				QcLotListEnable = (qcle.QcLotListEnable ? "1" : "0"),
				QcLot2dSEnableCd = qcle.QcLot2dSEnableCd,
				QcLotScanEnableCd = qcle.QcLot2dSEnableCd,
				QcLotScanRequireAccept = (qcle.QcLotScanRequireAccept ? "1" : "0"),
				QcLotSupOverrideEnable = (qcle.QcLotSupOverrideEnable ? "1" : "0"),
				QcLotValidation = (qcle.QcLotValidation ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.QcLotListEnable = (qcle.QcLotListEnable ? "1" : "0");
			novaSetup.QcLot2dSEnableCd = qcle.QcLot2dSEnableCd;
			novaSetup.QcLotScanEnableCd = qcle.QcLot2dSEnableCd;
			novaSetup.QcLotScanRequireAccept = (qcle.QcLotScanRequireAccept ? "1" : "0");
			novaSetup.QcLotSupOverrideEnable = (qcle.QcLotSupOverrideEnable ? "1" : "0");
			novaSetup.QcLotValidation = (qcle.QcLotValidation ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddSideSetup(SideSetupModel side)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == side.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = side.Id,
				ObsIdMethodCd = side.ObsIdMethodCd,
				AccnIdPromptText = side.AccnIdPromptText,
				PatIdPromptText = side.PatIdPromptText
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.ObsIdMethodCd = side.ObsIdMethodCd;
			novaSetup.AccnIdPromptText = side.AccnIdPromptText;
			novaSetup.PatIdPromptText = side.PatIdPromptText;
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task AddStleSetup(StleSetupModel stle)
	{
		NovaSetup novaSetup = DbContext.NovaSetups.FirstOrDefault((NovaSetup e) => e.LocationId == stle.Id);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = stle.Id,
				StripIdAutoEnabled = (stle.StripIdAutoEnabled ? "1" : "0"),
				StripIdDefaultLastStripId = (stle.StripIdDefaultLastStripId ? "1" : "0"),
				StripIdListEnable = (stle.StripIdListEnable ? "1" : "0"),
				StripId2dSEnableCd = stle.StripId2dSEnableCd,
				StripIdScanEnableCd = stle.StripId2dSEnableCd,
				StripIdScanRequireAccept = (stle.StripIdScanRequireAccept ? "1" : "0"),
				StripIdSupOverrideEnable = (stle.StripIdSupOverrideEnable ? "1" : "0"),
				StripIdValidation = (stle.StripIdValidation ? "1" : "0")
			};
			DbContext.NovaSetups.Add(entity);
		}
		else
		{
			novaSetup.StripIdAutoEnabled = (stle.StripIdAutoEnabled ? "1" : "0");
			novaSetup.StripIdDefaultLastStripId = (stle.StripIdDefaultLastStripId ? "1" : "0");
			novaSetup.StripIdListEnable = (stle.StripIdListEnable ? "1" : "0");
			novaSetup.StripId2dSEnableCd = stle.StripId2dSEnableCd;
			novaSetup.StripIdScanEnableCd = stle.StripId2dSEnableCd;
			novaSetup.StripIdScanRequireAccept = (stle.StripIdScanRequireAccept ? "1" : "0");
			novaSetup.StripIdSupOverrideEnable = (stle.StripIdSupOverrideEnable ? "1" : "0");
			novaSetup.StripIdValidation = (stle.StripIdValidation ? "1" : "0");
			novaSetup.SaveTime = DateTime.Now;
		}
		await DbContext.SaveChangesAsync();
	}

	public async Task<NovaSetupModel> GetNovaSetup(int id)
	{
		NovaSetup novaSetup = DbContext.Set<NovaSetup>().Include("Location.TestRange").AsNoTracking()
			.FirstOrDefault((NovaSetup e) => e.LocationId == id);
		if (novaSetup == null)
		{
			return null;
		}
		NovaSetupModel novaSetupModel = new NovaSetupModel
		{
			Id = novaSetup.Id,
			LocationId = novaSetup.LocationId,
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
		return novaSetupModel;
	}

	public async Task AddNovaGroup(NovaSetupGroupModel group)
	{
		if (group.Id == 0)
		{
			NovaSetupGroup entity = new NovaSetupGroup
			{
				Name = group.Name,
				AccnId2DsEnableCd = group.AccnId2DsEnableCd,
				AccnIdAlphaEnable = group.AccnIdAlphaEnable,
				AccnIdListEnable = group.AccnIdListEnable,
				AccnIdNonBarcodeCommentReq = group.AccnIdNonBarcodeCommentReq,
				AccnIdScanEnable2D = group.AccnIdScanEnable2D,
				AccnIdScanEnableC128 = group.AccnIdScanEnableC128,
				AccnIdScanEnableC2o5 = group.AccnIdScanEnableC2o5,
				AccnIdScanEnableC39 = group.AccnIdScanEnableC39,
				AccnIdScanEnableC93 = group.AccnIdScanEnableC93,
				AccnIdScanEnableCbar = group.AccnIdScanEnableCbar,
				AccnIdScanEnableCd = group.AccnIdScanEnableCd,
				AccnIdScanMaskFailRejC128 = group.AccnIdScanMaskFailRejC128,
				AccnIdScanMaskFailRejC2o5 = group.AccnIdScanMaskFailRejC2o5,
				AccnIdScanMaskFailRejC39 = group.AccnIdScanMaskFailRejC39,
				AccnIdScanMaskFailRejC93 = group.AccnIdScanMaskFailRejC93,
				AccnIdScanMaskFailRejCbar = group.AccnIdScanMaskFailRejCbar,
				AccnIdScanRequireAccept = group.AccnIdScanRequireAccept,
				AccnIdSm2D = group.AccnIdSm2D,
				AccnIdSmC128 = group.AccnIdSmC128,
				AccnIdSmC2o5 = group.AccnIdSmC2o5,
				AccnIdSmC39 = group.AccnIdSmC39,
				AccnIdSmC93 = group.AccnIdSmC93,
				AccnIdSmCbar = group.AccnIdSmCbar,
				AccnIdSupOverrideEnable = group.AccnIdSupOverrideEnable,
				AccnIdValidation = group.AccnIdValidation,
				DxId2dSEnableCd = group.DxId2dSEnableCd,
				DxIdAlphaEnable = group.DxIdAlphaEnable,
				DxIdListEnable = group.DxIdListEnable,
				DxIdNonBarcodeCommentReq = group.DxIdNonBarcodeCommentReq,
				DxIdPromptEnable = group.DxIdPromptEnable,
				DxIdScanEnable2D = group.DxIdScanEnable2D,
				DxIdScanEnableC128 = group.DxIdScanEnableC128,
				DxIdScanEnableC2o5 = group.DxIdScanEnableC2o5,
				DxIdScanEnableC39 = group.DxIdScanEnableC39,
				DxIdScanEnableC93 = group.DxIdScanEnableC93,
				DxIdScanEnableCbar = group.DxIdScanEnableCbar,
				DxIdScanEnableCd = group.DxIdScanEnableCd,
				DxIdScanMaskFailRejC128 = group.DxIdScanMaskFailRejC128,
				DxIdScanMaskFailRejC2o5 = group.DxIdScanMaskFailRejC2o5,
				DxIdScanMaskFailRejC39 = group.DxIdScanMaskFailRejC39,
				DxIdScanMaskFailRejC93 = group.DxIdScanMaskFailRejC93,
				DxIdScanMaskFailRejCbar = group.DxIdScanMaskFailRejCbar,
				DxIdScanRequireAccept = group.DxIdScanRequireAccept,
				DxIdSm2D = group.DxIdSm2D,
				DxIdSmC128 = group.DxIdSmC128,
				DxIdSmC2o5 = group.DxIdSmC2o5,
				DxIdSmC39 = group.DxIdSmC39,
				DxIdSmC93 = group.DxIdSmC93,
				DxIdSmCbar = group.DxIdSmCbar,
				DxIdSupOverrideEnable = group.DxIdSupOverrideEnable,
				DxIdValidation = group.DxIdValidation,
				ArchivedObsRetainDays = group.ArchivedObsRetainDays,
				ArchivedOvrwDisregardArchBit = group.ArchivedOvrwDisregardArchBit,
				DockLockAlertMins = group.DockLockAlertMins,
				DockLockElapsedHrs = group.DockLockElapsedHrs,
				DockLockInterval = group.DockLockInterval,
				DockLockModeCd = group.DockLockModeCd,
				DockLockShiftTimes = group.DockLockShiftTimes,
				DockLockSupOverrideEnable = group.DockLockSupOverrideEnable,
				AccnIdMaxLength = group.AccnIdMaxLength,
				AccnIdMinLength = group.AccnIdMinLength,
				DxIdMaxLength = group.DxIdMaxLength,
				DxIdMinLength = group.DxIdMinLength,
				OpLoginMaxLength = group.OpLoginMaxLength,
				OpLoginMinLength = group.OpLoginMinLength,
				PatIdMaxLength = group.PatIdMaxLength,
				PatIdMinLength = group.PatIdMinLength,
				PhysIdMaxLength = group.PhysIdMaxLength,
				PhysIdMinLength = group.PhysIdMinLength,
				LinLot2dSEnableCd = group.LinLot2dSEnableCd,
				LinLotListEnable = group.LinLotListEnable,
				LinLotNonBarcodeCmntReq = group.LinLotNonBarcodeCmntReq,
				LinLotScanEnableCd = group.LinLotScanEnableCd,
				LinLotScanRequireAccept = group.LinLotScanRequireAccept,
				LinLotSupOverrideEnable = group.LinLotSupOverrideEnable,
				LinLotValidation = group.LinLotValidation,
				PatIdTypeCd = group.PatIdTypeCd,
				OpLogoffElapsedSecs = group.OpLogoffElapsedSecs,
				OpLogoffModeCd = group.OpLogoffModeCd,
				DateFormat = group.DateFormat,
				MeterMaxLinRec = group.MeterMaxLinRec,
				MeterMaxPatRec = group.MeterMaxPatRec,
				MeterMaxProfRec = group.MeterMaxProfRec,
				MeterMaxQCRec = group.MeterMaxQCRec,
				TimeFormat = group.TimeFormat,
				OpLogin2dSEnableCd = group.OpLogin2dSEnableCd,
				OpLoginAlphaEnable = group.OpLoginAlphaEnable,
				OpLoginDisplayCd = group.OpLoginDisplayCd,
				OpLoginNonBarcodeCommentReq = group.OpLoginNonBarcodeCommentReq,
				OpLoginScanEnable2D = group.OpLoginScanEnable2D,
				OpLoginScanEnableC128 = group.OpLoginScanEnableC128,
				OpLoginScanEnableC2o5 = group.OpLoginScanEnableC2o5,
				OpLoginScanEnableC39 = group.OpLoginScanEnableC39,
				OpLoginScanEnableC93 = group.OpLoginScanEnableC93,
				OpLoginScanEnableCbar = group.OpLoginScanEnableCbar,
				OpLoginScanEnableCd = group.OpLoginScanEnableCd,
				OpLoginScanMaskFailRejC128 = group.OpLoginScanMaskFailRejC128,
				OpLoginScanMaskFailRejC2o5 = group.OpLoginScanMaskFailRejC2o5,
				OpLoginScanMaskFailRejC39 = group.OpLoginScanMaskFailRejC39,
				OpLoginScanMaskFailRejC93 = group.OpLoginScanMaskFailRejC93,
				OpLoginScanMaskFailRejCbar = group.OpLoginScanMaskFailRejCbar,
				OpLoginScanRequireAccept = group.OpLoginScanRequireAccept,
				OpLoginSm2D = group.OpLoginSm2D,
				OpLoginSmC128 = group.OpLoginSmC128,
				OpLoginSmC2o5 = group.OpLoginSmC2o5,
				OpLoginSmC39 = group.OpLoginSmC39,
				OpLoginSmC93 = group.OpLoginSmC93,
				OpLoginSmCbar = group.OpLoginSmCbar,
				OpLoginSupOverrideEnable = group.OpLoginSupOverrideEnable,
				OpLoginValidation = group.OpLoginValidation,
				SupOverrideSupOverrideEnable = group.SupOverrideSupOverrideEnable,
				SupOvScanEnableCd = group.SupOvScanEnableCd,
				SupOvScanRequireAccept = group.SupOvScanRequireAccept,
				PatId2dSEnableCd = group.PatId2dSEnableCd,
				PatIdAlphaEnable = group.PatIdAlphaEnable,
				PatIdAutoEnabled = group.PatIdAutoEnabled,
				PatIdFailCommentReq = group.PatIdFailCommentReq,
				PatIdFailDowntimeEnable = group.PatIdFailDowntimeEnable,
				PatIdFailNewPtEnable = group.PatIdFailNewPtEnable,
				PatIdListEnable = group.PatIdListEnable,
				PatIdNonBarcodeCommentReq = group.PatIdNonBarcodeCommentReq,
				PatIdScanEnable2D = group.PatIdScanEnable2D,
				PatIdScanEnableC128 = group.PatIdScanEnableC128,
				PatIdScanEnableC2o5 = group.PatIdScanEnableC2o5,
				PatIdScanEnableC39 = group.PatIdScanEnableC39,
				PatIdScanEnableC93 = group.PatIdScanEnableC93,
				PatIdScanEnableCbar = group.PatIdScanEnableCbar,
				PatIdScanEnableCd = group.PatIdScanEnableCd,
				PatIdScanMaskFailRejC128 = group.PatIdScanMaskFailRejC128,
				PatIdScanMaskFailRejC2o5 = group.PatIdScanMaskFailRejC2o5,
				PatIdScanMaskFailRejC39 = group.PatIdScanMaskFailRejC39,
				PatIdScanMaskFailRejC93 = group.PatIdScanMaskFailRejC93,
				PatIdScanMaskFailRejCbar = group.PatIdScanMaskFailRejCbar,
				PatIdScanRequireAccept = group.PatIdScanRequireAccept,
				PatIdSm2D = group.PatIdSm2D,
				PatIdSmC128 = group.PatIdSmC128,
				PatIdSmC2o5 = group.PatIdSmC2o5,
				PatIdSmC39 = group.PatIdSmC39,
				PatIdSmC93 = group.PatIdSmC93,
				PatIdSmCbar = group.PatIdSmCbar,
				PatIdSupOverrideEnable = group.PatIdSupOverrideEnable,
				PatIdTgcEnable = group.PatIdTgcEnable,
				PatIdValidation = group.PatIdValidation,
				CommentsFreeTextChartableEnable = group.CommentsFreeTextChartableEnable,
				CommentsFreeTextEnable = group.CommentsFreeTextEnable,
				CommentsFreeTextFlaggedEnable = group.CommentsFreeTextFlaggedEnable,
				LinObsFailCommentReqCd = group.LinObsFailCommentReqCd,
				LinObsPassCommentReqCd = group.LinObsPassCommentReqCd,
				LinObsValueDisplay = group.LinObsValueDisplay,
				ObsAbnormalRangeCommentReq = group.ObsAbnormalRangeCommentReq,
				ObsCriticalRangeCommentReq = group.ObsCriticalRangeCommentReq,
				ObsNormalRangeCommentReq = group.ObsNormalRangeCommentReq,
				ObsRejectEnable = group.ObsRejectEnable,
				ObsRejectResultCommentReq = group.ObsRejectResultCommentReq,
				ObsRejectSupOverrideReq = group.ObsRejectSupOverrideReq,
				ObsReviewNoLogin = group.ObsReviewNoLogin,
				ObsTechnicalRangeCommentReq = group.ObsTechnicalRangeCommentReq,
				QcObsFailCommentReqCd = group.QcObsFailCommentReqCd,
				QcObsPassCommentReqCd = group.QcObsPassCommentReqCd,
				QcObsValueDisplay = group.QcObsValueDisplay,
				PhysId2dSEnableCd = group.PhysId2dSEnableCd,
				PhysIdAlphaEnable = group.PhysIdAlphaEnable,
				PhysIdListEnable = group.PhysIdListEnable,
				PhysIdNonBarcodeCommentReq = group.PhysIdNonBarcodeCommentReq,
				PhysIdPromptEnable = group.PhysIdPromptEnable,
				PhysIdScanEnable2D = group.PhysIdScanEnable2D,
				PhysIdScanEnableC128 = group.PhysIdScanEnableC128,
				PhysIdScanEnableC2o5 = group.PhysIdScanEnableC2o5,
				PhysIdScanEnableC39 = group.PhysIdScanEnableC39,
				PhysIdScanEnableC93 = group.PhysIdScanEnableC93,
				PhysIdScanEnableCbar = group.PhysIdScanEnableCbar,
				PhysIdScanEnableCd = group.PhysIdScanEnableCd,
				PhysIdScanMaskFailRejC128 = group.PhysIdScanMaskFailRejC128,
				PhysIdScanMaskFailRejC2o5 = group.PhysIdScanMaskFailRejC2o5,
				PhysIdScanMaskFailRejC39 = group.PhysIdScanMaskFailRejC39,
				PhysIdScanMaskFailRejC93 = group.PhysIdScanMaskFailRejC93,
				PhysIdScanMaskFailRejCbar = group.PhysIdScanMaskFailRejCbar,
				PhysIdScanRequireAccept = group.PhysIdScanRequireAccept,
				PhysIdSm2D = group.PhysIdSm2D,
				PhysIdSmC128 = group.PhysIdSmC128,
				PhysIdSmC2o5 = group.PhysIdSmC2o5,
				PhysIdSmC39 = group.PhysIdSmC39,
				PhysIdSmC93 = group.PhysIdSmC93,
				PhysIdSmCbar = group.PhysIdSmCbar,
				PhysIdSupOverrideEnable = group.PhysIdSupOverrideEnable,
				PhysIdValidation = group.PhysIdValidation,
				PrivLevelAdminRenameMeterCd = group.PrivLevelAdminRenameMeterCd,
				PrivLevelAdminResetFacilityCd = group.PrivLevelAdminResetFacilityCd,
				PrivLevelAdminSetNetworkCd = group.PrivLevelAdminSetNetworkCd,
				PrivLevelAdminUnarchiveBitCd = group.PrivLevelAdminUnarchiveBitCd,
				PrivLevelDockLockOvCd = group.PrivLevelDockLockOvCd,
				PrivLevelSetDateTimeCd = group.PrivLevelSetDateTimeCd,
				PrivLevelTesttypeCorrelationCd = group.PrivLevelTesttypeCorrelationCd,
				PrivLevelTesttypeLinearityCd = group.PrivLevelTesttypeLinearityCd,
				PrivLevelTesttypeMaintCd = group.PrivLevelTesttypeMaintCd,
				PrivLevelTesttypeProficiencyCd = group.PrivLevelTesttypeProficiencyCd,
				PrivLevelTesttypeTrainingCd = group.PrivLevelTesttypeTrainingCd,
				ProfLot2dSEnableCd = group.ProfLot2dSEnableCd,
				ProfLotAlphaEnable = group.ProfLotAlphaEnable,
				ProfLotListEnable = group.ProfLotListEnable,
				ProfLotMaxLength = group.ProfLotMaxLength,
				ProfLotMinLength = group.ProfLotMinLength,
				ProfLotNonBarcodeCommentReq = group.ProfLotNonBarcodeCommentReq,
				ProfLotScanEnableCd = group.ProfLotScanEnableCd,
				ProfLotScanRequireAccept = group.ProfLotScanRequireAccept,
				ProfLotSupOverrideEnable = group.ProfLotSupOverrideEnable,
				ProfLotValidation = group.ProfLotValidation,
				ProfRejectEnable = group.ProfRejectEnable,
				QcLockAlertMins = group.QcLockAlertMins,
				QcLockElapsedHrs = group.QcLockElapsedHrs,
				QcLockInterval = group.QcLockInterval,
				QcLockKetAlertMins = group.QcLockKetAlertMins,
				QcLockKetElapsedHrs = group.QcLockKetElapsedHrs,
				QcLockKetInterval = group.QcLockKetInterval,
				QcLockKetLevel1Req = group.QcLockKetLevel1Req,
				QcLockKetLevel2Req = group.QcLockKetLevel2Req,
				QcLockKetLevel3Req = group.QcLockKetLevel3Req,
				QcLockKetModeCd = group.QcLockKetModeCd,
				QcLockKetShiftTimes = group.QcLockKetShiftTimes,
				QcLockLevel1Req = group.QcLockLevel1Req,
				QcLockLevel2Req = group.QcLockLevel2Req,
				QcLockLevel3Req = group.QcLockLevel3Req,
				QcLockLevel4Req = group.QcLockLevel4Req,
				QcLockModeCd = group.QcLockModeCd,
				QcLockShiftTimes = group.QcLockShiftTimes,
				QcLot2dSEnableCd = group.QcLot2dSEnableCd,
				QcLotListEnable = group.QcLotListEnable,
				QcLotNonBarcodeCommentReq = group.QcLotNonBarcodeCommentReq,
				QcLotScanEnableCd = group.QcLotScanEnableCd,
				QcLotScanRequireAccept = group.QcLotScanRequireAccept,
				QcLotSupOverrideEnable = group.QcLotSupOverrideEnable,
				QcLotValidation = group.QcLotValidation,
				AccnIdPromptText = group.AccnIdPromptText,
				ObsIdMethodCd = group.ObsIdMethodCd,
				PatIdPromptText = group.PatIdPromptText,
				SampleTypeSelectEnable = group.SampleTypeSelectEnable,
				StripId2dSEnableCd = group.StripId2dSEnableCd,
				StripIdAutoEnabled = group.StripIdAutoEnabled,
				StripIdDefaultLastStripId = group.StripIdDefaultLastStripId,
				StripIdListEnable = group.StripIdListEnable,
				StripIdNonBarcodeCommentReq = group.StripIdNonBarcodeCommentReq,
				StripIdScanEnableCd = group.StripIdScanEnableCd,
				StripIdScanRequireAccept = group.StripIdScanRequireAccept,
				StripIdSupOverrideEnable = group.StripIdSupOverrideEnable,
				StripIdValidation = group.StripIdValidation,
				SL = group.SL,
				IC = group.IC
			};
			DbContext.NovaSetupGroup.Add(entity);
		}
		else
		{
			NovaSetupGroup novaSetupGroup = DbContext.Set<NovaSetupGroup>().FirstOrDefault((NovaSetupGroup e) => e.Id == group.Id);
			if (novaSetupGroup == null)
			{
				return;
			}
			novaSetupGroup.Name = group.Name;
			novaSetupGroup.AccnIdAlphaEnable = group.AccnIdAlphaEnable;
			novaSetupGroup.AccnIdScanRequireAccept = group.AccnIdScanRequireAccept;
			novaSetupGroup.AccnId2DsEnableCd = group.AccnId2DsEnableCd;
			novaSetupGroup.AccnIdScanEnableCd = group.AccnIdScanEnableCd;
			novaSetupGroup.DxIdListEnable = group.DxIdListEnable;
			novaSetupGroup.DxIdPromptEnable = group.DxIdPromptEnable;
			novaSetupGroup.DxIdScanRequireAccept = group.DxIdScanRequireAccept;
			novaSetupGroup.DxIdSupOverrideEnable = group.DxIdSupOverrideEnable;
			novaSetupGroup.DxIdValidation = group.DxIdValidation;
			novaSetupGroup.DxId2dSEnableCd = group.DxId2dSEnableCd;
			novaSetupGroup.DxIdScanEnableCd = group.DxIdScanEnableCd;
			novaSetupGroup.DockLockSupOverrideEnable = group.DockLockSupOverrideEnable;
			novaSetupGroup.ArchivedObsRetainDays = group.ArchivedObsRetainDays;
			novaSetupGroup.ArchivedOvrwDisregardArchBit = group.ArchivedOvrwDisregardArchBit;
			novaSetupGroup.DockLockAlertMins = group.DockLockAlertMins;
			novaSetupGroup.DockLockModeCd = group.DockLockModeCd;
			novaSetupGroup.DockLockInterval = group.DockLockInterval;
			novaSetupGroup.DockLockShiftTimes = group.DockLockShiftTimes;
			novaSetupGroup.DockLockElapsedHrs = group.DockLockElapsedHrs;
			novaSetupGroup.LinLotListEnable = group.LinLotListEnable;
			novaSetupGroup.LinLot2dSEnableCd = group.LinLot2dSEnableCd;
			novaSetupGroup.LinLotScanEnableCd = group.LinLotScanEnableCd;
			novaSetupGroup.LinLotScanRequireAccept = group.LinLotScanRequireAccept;
			novaSetupGroup.LinLotSupOverrideEnable = group.LinLotSupOverrideEnable;
			novaSetupGroup.LinLotValidation = group.LinLotValidation;
			novaSetupGroup.OpLogoffElapsedSecs = group.OpLogoffElapsedSecs;
			novaSetupGroup.OpLogoffModeCd = group.OpLogoffModeCd;
			novaSetupGroup.OpLoginScanRequireAccept = group.OpLoginScanRequireAccept;
			novaSetupGroup.OpLoginAlphaEnable = group.OpLoginAlphaEnable;
			novaSetupGroup.OpLogin2dSEnableCd = group.OpLogin2dSEnableCd;
			novaSetupGroup.OpLoginScanEnableCd = group.OpLoginScanEnableCd;
			novaSetupGroup.OpLoginSupOverrideEnable = group.OpLoginSupOverrideEnable;
			novaSetupGroup.OpLoginValidation = group.OpLoginValidation;
			novaSetupGroup.OpLoginDisplayCd = group.OpLoginDisplayCd;
			novaSetupGroup.SupOvScanRequireAccept = group.SupOvScanRequireAccept;
			novaSetupGroup.SupOvScanEnableCd = group.SupOvScanEnableCd;
			novaSetupGroup.PatIdAutoEnabled = group.PatIdAutoEnabled;
			novaSetupGroup.PatIdAlphaEnable = group.PatIdAlphaEnable;
			novaSetupGroup.PatIdFailDowntimeEnable = group.PatIdFailDowntimeEnable;
			novaSetupGroup.PatIdFailNewPtEnable = group.PatIdFailNewPtEnable;
			novaSetupGroup.PatIdListEnable = group.PatIdListEnable;
			novaSetupGroup.PatId2dSEnableCd = group.PatId2dSEnableCd;
			novaSetupGroup.PatIdScanEnableCd = group.PatIdScanEnableCd;
			novaSetupGroup.PatIdScanRequireAccept = group.PatIdScanRequireAccept;
			novaSetupGroup.PatIdSupOverrideEnable = group.PatIdSupOverrideEnable;
			novaSetupGroup.PatIdTgcEnable = group.PatIdTgcEnable;
			novaSetupGroup.PatIdValidation = group.PatIdValidation;
			novaSetupGroup.CommentsFreeTextChartableEnable = group.CommentsFreeTextChartableEnable;
			novaSetupGroup.CommentsFreeTextFlaggedEnable = group.CommentsFreeTextFlaggedEnable;
			novaSetupGroup.ObsReviewNoLogin = group.ObsReviewNoLogin;
			novaSetupGroup.LinObsValueDisplay = group.LinObsValueDisplay;
			novaSetupGroup.CommentsFreeTextEnable = group.CommentsFreeTextEnable;
			novaSetupGroup.ObsRejectEnable = group.ObsRejectEnable;
			novaSetupGroup.QcObsValueDisplay = group.QcObsValueDisplay;
			novaSetupGroup.QcObsFailCommentReqCd = group.QcObsFailCommentReqCd;
			novaSetupGroup.LinObsFailCommentReqCd = group.LinObsFailCommentReqCd;
			novaSetupGroup.ObsRejectResultCommentReq = group.ObsRejectResultCommentReq;
			novaSetupGroup.ObsCriticalRangeCommentReq = group.ObsCriticalRangeCommentReq;
			novaSetupGroup.ObsTechnicalRangeCommentReq = group.ObsTechnicalRangeCommentReq;
			novaSetupGroup.QcObsPassCommentReqCd = group.QcObsPassCommentReqCd;
			novaSetupGroup.LinObsPassCommentReqCd = group.LinObsPassCommentReqCd;
			novaSetupGroup.ObsNormalRangeCommentReq = group.ObsNormalRangeCommentReq;
			novaSetupGroup.ObsAbnormalRangeCommentReq = group.ObsAbnormalRangeCommentReq;
			novaSetupGroup.PhysIdAlphaEnable = group.PhysIdAlphaEnable;
			novaSetupGroup.PhysIdListEnable = group.PhysIdListEnable;
			novaSetupGroup.PhysIdPromptEnable = group.PhysIdPromptEnable;
			novaSetupGroup.PhysId2dSEnableCd = group.PhysId2dSEnableCd;
			novaSetupGroup.PhysIdScanEnableCd = group.PhysIdScanEnableCd;
			novaSetupGroup.PhysIdScanRequireAccept = group.PhysIdScanRequireAccept;
			novaSetupGroup.PhysIdSupOverrideEnable = group.PhysIdSupOverrideEnable;
			novaSetupGroup.PhysIdValidation = group.PhysIdValidation;
			novaSetupGroup.PrivLevelAdminRenameMeterCd = group.PrivLevelAdminRenameMeterCd;
			novaSetupGroup.PrivLevelAdminResetFacilityCd = group.PrivLevelAdminResetFacilityCd;
			novaSetupGroup.PrivLevelAdminSetNetworkCd = group.PrivLevelAdminSetNetworkCd;
			novaSetupGroup.PrivLevelAdminUnarchiveBitCd = group.PrivLevelAdminUnarchiveBitCd;
			novaSetupGroup.PrivLevelTesttypeLinearityCd = group.PrivLevelTesttypeLinearityCd;
			novaSetupGroup.PrivLevelTesttypeProficiencyCd = group.PrivLevelTesttypeProficiencyCd;
			novaSetupGroup.PrivLevelDockLockOvCd = group.PrivLevelDockLockOvCd;
			novaSetupGroup.PrivLevelSetDateTimeCd = group.PrivLevelSetDateTimeCd;
			novaSetupGroup.QcLockAlertMins = group.QcLockAlertMins;
			novaSetupGroup.QcLockLevel1Req = group.QcLockLevel1Req;
			novaSetupGroup.QcLockLevel2Req = group.QcLockLevel2Req;
			novaSetupGroup.QcLockLevel3Req = group.QcLockLevel3Req;
			novaSetupGroup.QcLockModeCd = group.QcLockModeCd;
			novaSetupGroup.QcLockInterval = group.QcLockInterval;
			novaSetupGroup.QcLockElapsedHrs = group.QcLockElapsedHrs;
			novaSetupGroup.QcLockShiftTimes = group.QcLockShiftTimes;
			novaSetupGroup.QcLotListEnable = group.QcLotListEnable;
			novaSetupGroup.QcLot2dSEnableCd = group.QcLot2dSEnableCd;
			novaSetupGroup.QcLotScanEnableCd = group.QcLotScanEnableCd;
			novaSetupGroup.QcLotScanRequireAccept = group.QcLotScanRequireAccept;
			novaSetupGroup.QcLotSupOverrideEnable = group.QcLotSupOverrideEnable;
			novaSetupGroup.QcLotValidation = group.QcLotValidation;
			novaSetupGroup.ObsIdMethodCd = group.ObsIdMethodCd;
			novaSetupGroup.AccnIdPromptText = group.AccnIdPromptText;
			novaSetupGroup.PatIdPromptText = group.PatIdPromptText;
			novaSetupGroup.StripIdAutoEnabled = group.StripIdAutoEnabled;
			novaSetupGroup.StripIdDefaultLastStripId = group.StripIdDefaultLastStripId;
			novaSetupGroup.StripIdListEnable = group.StripIdListEnable;
			novaSetupGroup.StripId2dSEnableCd = group.StripId2dSEnableCd;
			novaSetupGroup.StripIdScanEnableCd = group.StripIdScanEnableCd;
			novaSetupGroup.StripIdScanRequireAccept = group.StripIdScanRequireAccept;
			novaSetupGroup.StripIdSupOverrideEnable = group.StripIdSupOverrideEnable;
			novaSetupGroup.StripIdValidation = group.StripIdValidation;
			novaSetupGroup.SL = group.SL;
			novaSetupGroup.IC = group.IC;
		}
		await DbContext.SaveChangesAsync();
	}

	public Task<List<NovaSetupGroupModel>> GetNovaGroups()
	{
		return Task.FromResult((from l in DbContext.Set<NovaSetupGroup>().AsNoTracking()
			select new NovaSetupGroupModel
			{
				Id = l.Id,
				Name = l.Name,
				AccnIdAlphaEnable = l.AccnIdAlphaEnable,
				AccnIdScanRequireAccept = l.AccnIdScanRequireAccept,
				AccnId2DsEnableCd = l.AccnId2DsEnableCd,
				AccnIdScanEnableCd = l.AccnIdScanEnableCd,
				DxIdListEnable = l.DxIdListEnable,
				DxIdPromptEnable = l.DxIdPromptEnable,
				DxIdScanRequireAccept = l.DxIdScanRequireAccept,
				DxIdSupOverrideEnable = l.DxIdSupOverrideEnable,
				DxIdValidation = l.DxIdValidation,
				DxId2dSEnableCd = l.DxId2dSEnableCd,
				DxIdScanEnableCd = l.DxIdScanEnableCd,
				DockLockSupOverrideEnable = l.DockLockSupOverrideEnable,
				ArchivedObsRetainDays = l.ArchivedObsRetainDays,
				ArchivedOvrwDisregardArchBit = l.ArchivedOvrwDisregardArchBit,
				DockLockAlertMins = l.DockLockAlertMins,
				DockLockModeCd = l.DockLockModeCd,
				DockLockInterval = l.DockLockInterval,
				DockLockShiftTimes = l.DockLockShiftTimes,
				DockLockElapsedHrs = l.DockLockElapsedHrs,
				LinLotListEnable = l.LinLotListEnable,
				LinLot2dSEnableCd = l.LinLot2dSEnableCd,
				LinLotScanEnableCd = l.LinLotScanEnableCd,
				LinLotScanRequireAccept = l.LinLotScanRequireAccept,
				LinLotSupOverrideEnable = l.LinLotSupOverrideEnable,
				LinLotValidation = l.LinLotValidation,
				OpLogoffElapsedSecs = l.OpLogoffElapsedSecs,
				OpLogoffModeCd = l.OpLogoffModeCd,
				OpLoginScanRequireAccept = l.OpLoginScanRequireAccept,
				OpLoginAlphaEnable = l.OpLoginAlphaEnable,
				OpLogin2dSEnableCd = l.OpLogin2dSEnableCd,
				OpLoginScanEnableCd = l.OpLoginScanEnableCd,
				OpLoginSupOverrideEnable = l.OpLoginSupOverrideEnable,
				OpLoginValidation = l.OpLoginValidation,
				OpLoginDisplayCd = l.OpLoginDisplayCd,
				SupOvScanRequireAccept = l.SupOvScanRequireAccept,
				SupOvScanEnableCd = l.SupOvScanEnableCd,
				PatIdAutoEnabled = l.PatIdAutoEnabled,
				PatIdAlphaEnable = l.PatIdAlphaEnable,
				PatIdFailDowntimeEnable = l.PatIdFailDowntimeEnable,
				PatIdFailNewPtEnable = l.PatIdFailNewPtEnable,
				PatIdListEnable = l.PatIdListEnable,
				PatId2dSEnableCd = l.PatId2dSEnableCd,
				PatIdScanEnableCd = l.PatIdScanEnableCd,
				PatIdScanRequireAccept = l.PatIdScanRequireAccept,
				PatIdSupOverrideEnable = l.PatIdSupOverrideEnable,
				PatIdTgcEnable = l.PatIdTgcEnable,
				PatIdValidation = l.PatIdValidation,
				CommentsFreeTextChartableEnable = l.CommentsFreeTextChartableEnable,
				CommentsFreeTextFlaggedEnable = l.CommentsFreeTextFlaggedEnable,
				ObsReviewNoLogin = l.ObsReviewNoLogin,
				LinObsValueDisplay = l.LinObsValueDisplay,
				CommentsFreeTextEnable = l.CommentsFreeTextEnable,
				ObsRejectEnable = l.ObsRejectEnable,
				QcObsValueDisplay = l.QcObsValueDisplay,
				QcObsFailCommentReqCd = l.QcObsFailCommentReqCd,
				LinObsFailCommentReqCd = l.LinObsFailCommentReqCd,
				ObsRejectResultCommentReq = l.ObsRejectResultCommentReq,
				ObsCriticalRangeCommentReq = l.ObsCriticalRangeCommentReq,
				ObsTechnicalRangeCommentReq = l.ObsTechnicalRangeCommentReq,
				QcObsPassCommentReqCd = l.QcObsPassCommentReqCd,
				LinObsPassCommentReqCd = l.LinObsPassCommentReqCd,
				ObsNormalRangeCommentReq = l.ObsNormalRangeCommentReq,
				ObsAbnormalRangeCommentReq = l.ObsAbnormalRangeCommentReq,
				PhysIdAlphaEnable = l.PhysIdAlphaEnable,
				PhysIdListEnable = l.PhysIdListEnable,
				PhysIdPromptEnable = l.PhysIdPromptEnable,
				PhysId2dSEnableCd = l.PhysId2dSEnableCd,
				PhysIdScanEnableCd = l.PhysIdScanEnableCd,
				PhysIdScanRequireAccept = l.PhysIdScanRequireAccept,
				PhysIdSupOverrideEnable = l.PhysIdSupOverrideEnable,
				PhysIdValidation = l.PhysIdValidation,
				PrivLevelAdminRenameMeterCd = l.PrivLevelAdminRenameMeterCd,
				PrivLevelAdminResetFacilityCd = l.PrivLevelAdminResetFacilityCd,
				PrivLevelAdminSetNetworkCd = l.PrivLevelAdminSetNetworkCd,
				PrivLevelAdminUnarchiveBitCd = l.PrivLevelAdminUnarchiveBitCd,
				PrivLevelTesttypeLinearityCd = l.PrivLevelTesttypeLinearityCd,
				PrivLevelTesttypeProficiencyCd = l.PrivLevelTesttypeProficiencyCd,
				PrivLevelDockLockOvCd = l.PrivLevelDockLockOvCd,
				PrivLevelSetDateTimeCd = l.PrivLevelSetDateTimeCd,
				QcLockAlertMins = l.QcLockAlertMins,
				QcLockLevel1Req = l.QcLockLevel1Req,
				QcLockLevel2Req = l.QcLockLevel2Req,
				QcLockLevel3Req = l.QcLockLevel3Req,
				QcLockModeCd = l.QcLockModeCd,
				QcLockInterval = l.QcLockInterval,
				QcLockElapsedHrs = l.QcLockElapsedHrs,
				QcLockShiftTimes = l.QcLockShiftTimes,
				QcLotListEnable = l.QcLotListEnable,
				QcLot2dSEnableCd = l.QcLot2dSEnableCd,
				QcLotScanEnableCd = l.QcLotScanEnableCd,
				QcLotScanRequireAccept = l.QcLotScanRequireAccept,
				QcLotSupOverrideEnable = l.QcLotSupOverrideEnable,
				QcLotValidation = l.QcLotValidation,
				ObsIdMethodCd = l.ObsIdMethodCd,
				AccnIdPromptText = l.AccnIdPromptText,
				PatIdPromptText = l.PatIdPromptText,
				StripIdAutoEnabled = l.StripIdAutoEnabled,
				StripIdDefaultLastStripId = l.StripIdDefaultLastStripId,
				StripIdListEnable = l.StripIdListEnable,
				StripId2dSEnableCd = l.StripId2dSEnableCd,
				StripIdScanEnableCd = l.StripIdScanEnableCd,
				StripIdScanRequireAccept = l.StripIdScanRequireAccept,
				StripIdSupOverrideEnable = l.StripIdSupOverrideEnable,
				StripIdValidation = l.StripIdValidation,
				SL = l.SL,
				IC = l.IC
			}).ToList());
	}

	public async Task DeleteNovaGroup(int id)
	{
		NovaSetupGroup novaSetupGroup = DbContext.NovaSetupGroup.FirstOrDefault((NovaSetupGroup e) => e.Id == id);
		if (novaSetupGroup != null)
		{
			DbContext.NovaSetupGroup.Remove(novaSetupGroup);
			await DbContext.SaveChangesAsync();
		}
	}

	public async Task ApplyNovaGroup(int gid, int lid)
	{
		NovaSetupGroup novaSetupGroup = DbContext.Set<NovaSetupGroup>().FirstOrDefault((NovaSetupGroup e) => e.Id == gid);
		if (novaSetupGroup == null)
		{
			return;
		}
		NovaSetup novaSetup = DbContext.Set<NovaSetup>().FirstOrDefault((NovaSetup e) => e.LocationId == lid);
		if (novaSetup == null)
		{
			NovaSetup entity = new NovaSetup
			{
				LocationId = lid,
				AccnId2DsEnableCd = novaSetupGroup.AccnId2DsEnableCd,
				AccnIdAlphaEnable = novaSetupGroup.AccnIdAlphaEnable,
				AccnIdListEnable = novaSetupGroup.AccnIdListEnable,
				AccnIdNonBarcodeCommentReq = novaSetupGroup.AccnIdNonBarcodeCommentReq,
				AccnIdScanEnable2D = novaSetupGroup.AccnIdScanEnable2D,
				AccnIdScanEnableC128 = novaSetupGroup.AccnIdScanEnableC128,
				AccnIdScanEnableC2o5 = novaSetupGroup.AccnIdScanEnableC2o5,
				AccnIdScanEnableC39 = novaSetupGroup.AccnIdScanEnableC39,
				AccnIdScanEnableC93 = novaSetupGroup.AccnIdScanEnableC93,
				AccnIdScanEnableCbar = novaSetupGroup.AccnIdScanEnableCbar,
				AccnIdScanEnableCd = novaSetupGroup.AccnIdScanEnableCd,
				AccnIdScanMaskFailRejC128 = novaSetupGroup.AccnIdScanMaskFailRejC128,
				AccnIdScanMaskFailRejC2o5 = novaSetupGroup.AccnIdScanMaskFailRejC2o5,
				AccnIdScanMaskFailRejC39 = novaSetupGroup.AccnIdScanMaskFailRejC39,
				AccnIdScanMaskFailRejC93 = novaSetupGroup.AccnIdScanMaskFailRejC93,
				AccnIdScanMaskFailRejCbar = novaSetupGroup.AccnIdScanMaskFailRejCbar,
				AccnIdScanRequireAccept = novaSetupGroup.AccnIdScanRequireAccept,
				AccnIdSm2D = novaSetupGroup.AccnIdSm2D,
				AccnIdSmC128 = novaSetupGroup.AccnIdSmC128,
				AccnIdSmC2o5 = novaSetupGroup.AccnIdSmC2o5,
				AccnIdSmC39 = novaSetupGroup.AccnIdSmC39,
				AccnIdSmC93 = novaSetupGroup.AccnIdSmC93,
				AccnIdSmCbar = novaSetupGroup.AccnIdSmCbar,
				AccnIdSupOverrideEnable = novaSetupGroup.AccnIdSupOverrideEnable,
				AccnIdValidation = novaSetupGroup.AccnIdValidation,
				DxId2dSEnableCd = novaSetupGroup.DxId2dSEnableCd,
				DxIdAlphaEnable = novaSetupGroup.DxIdAlphaEnable,
				DxIdListEnable = novaSetupGroup.DxIdListEnable,
				DxIdNonBarcodeCommentReq = novaSetupGroup.DxIdNonBarcodeCommentReq,
				DxIdPromptEnable = novaSetupGroup.DxIdPromptEnable,
				DxIdScanEnable2D = novaSetupGroup.DxIdScanEnable2D,
				DxIdScanEnableC128 = novaSetupGroup.DxIdScanEnableC128,
				DxIdScanEnableC2o5 = novaSetupGroup.DxIdScanEnableC2o5,
				DxIdScanEnableC39 = novaSetupGroup.DxIdScanEnableC39,
				DxIdScanEnableC93 = novaSetupGroup.DxIdScanEnableC93,
				DxIdScanEnableCbar = novaSetupGroup.DxIdScanEnableCbar,
				DxIdScanEnableCd = novaSetupGroup.DxIdScanEnableCd,
				DxIdScanMaskFailRejC128 = novaSetupGroup.DxIdScanMaskFailRejC128,
				DxIdScanMaskFailRejC2o5 = novaSetupGroup.DxIdScanMaskFailRejC2o5,
				DxIdScanMaskFailRejC39 = novaSetupGroup.DxIdScanMaskFailRejC39,
				DxIdScanMaskFailRejC93 = novaSetupGroup.DxIdScanMaskFailRejC93,
				DxIdScanMaskFailRejCbar = novaSetupGroup.DxIdScanMaskFailRejCbar,
				DxIdScanRequireAccept = novaSetupGroup.DxIdScanRequireAccept,
				DxIdSm2D = novaSetupGroup.DxIdSm2D,
				DxIdSmC128 = novaSetupGroup.DxIdSmC128,
				DxIdSmC2o5 = novaSetupGroup.DxIdSmC2o5,
				DxIdSmC39 = novaSetupGroup.DxIdSmC39,
				DxIdSmC93 = novaSetupGroup.DxIdSmC93,
				DxIdSmCbar = novaSetupGroup.DxIdSmCbar,
				DxIdSupOverrideEnable = novaSetupGroup.DxIdSupOverrideEnable,
				DxIdValidation = novaSetupGroup.DxIdValidation,
				ArchivedObsRetainDays = novaSetupGroup.ArchivedObsRetainDays,
				ArchivedOvrwDisregardArchBit = novaSetupGroup.ArchivedOvrwDisregardArchBit,
				DockLockAlertMins = novaSetupGroup.DockLockAlertMins,
				DockLockElapsedHrs = novaSetupGroup.DockLockElapsedHrs,
				DockLockInterval = novaSetupGroup.DockLockInterval,
				DockLockModeCd = novaSetupGroup.DockLockModeCd,
				DockLockShiftTimes = novaSetupGroup.DockLockShiftTimes,
				DockLockSupOverrideEnable = novaSetupGroup.DockLockSupOverrideEnable,
				AccnIdMaxLength = novaSetupGroup.AccnIdMaxLength,
				AccnIdMinLength = novaSetupGroup.AccnIdMinLength,
				DxIdMaxLength = novaSetupGroup.DxIdMaxLength,
				DxIdMinLength = novaSetupGroup.DxIdMinLength,
				OpLoginMaxLength = novaSetupGroup.OpLoginMaxLength,
				OpLoginMinLength = novaSetupGroup.OpLoginMinLength,
				PatIdMaxLength = novaSetupGroup.PatIdMaxLength,
				PatIdMinLength = novaSetupGroup.PatIdMinLength,
				PhysIdMaxLength = novaSetupGroup.PhysIdMaxLength,
				PhysIdMinLength = novaSetupGroup.PhysIdMinLength,
				LinLot2dSEnableCd = novaSetupGroup.LinLot2dSEnableCd,
				LinLotListEnable = novaSetupGroup.LinLotListEnable,
				LinLotNonBarcodeCmntReq = novaSetupGroup.LinLotNonBarcodeCmntReq,
				LinLotScanEnableCd = novaSetupGroup.LinLotScanEnableCd,
				LinLotScanRequireAccept = novaSetupGroup.LinLotScanRequireAccept,
				LinLotSupOverrideEnable = novaSetupGroup.LinLotSupOverrideEnable,
				LinLotValidation = novaSetupGroup.LinLotValidation,
				PatIdTypeCd = novaSetupGroup.PatIdTypeCd,
				OpLogoffElapsedSecs = novaSetupGroup.OpLogoffElapsedSecs,
				OpLogoffModeCd = novaSetupGroup.OpLogoffModeCd,
				DateFormat = novaSetupGroup.DateFormat,
				MeterMaxLinRec = novaSetupGroup.MeterMaxLinRec,
				MeterMaxPatRec = novaSetupGroup.MeterMaxPatRec,
				MeterMaxProfRec = novaSetupGroup.MeterMaxProfRec,
				MeterMaxQCRec = novaSetupGroup.MeterMaxQCRec,
				TimeFormat = novaSetupGroup.TimeFormat,
				OpLogin2dSEnableCd = novaSetupGroup.OpLogin2dSEnableCd,
				OpLoginAlphaEnable = novaSetupGroup.OpLoginAlphaEnable,
				OpLoginDisplayCd = novaSetupGroup.OpLoginDisplayCd,
				OpLoginNonBarcodeCommentReq = novaSetupGroup.OpLoginNonBarcodeCommentReq,
				OpLoginScanEnable2D = novaSetupGroup.OpLoginScanEnable2D,
				OpLoginScanEnableC128 = novaSetupGroup.OpLoginScanEnableC128,
				OpLoginScanEnableC2o5 = novaSetupGroup.OpLoginScanEnableC2o5,
				OpLoginScanEnableC39 = novaSetupGroup.OpLoginScanEnableC39,
				OpLoginScanEnableC93 = novaSetupGroup.OpLoginScanEnableC93,
				OpLoginScanEnableCbar = novaSetupGroup.OpLoginScanEnableCbar,
				OpLoginScanEnableCd = novaSetupGroup.OpLoginScanEnableCd,
				OpLoginScanMaskFailRejC128 = novaSetupGroup.OpLoginScanMaskFailRejC128,
				OpLoginScanMaskFailRejC2o5 = novaSetupGroup.OpLoginScanMaskFailRejC2o5,
				OpLoginScanMaskFailRejC39 = novaSetupGroup.OpLoginScanMaskFailRejC39,
				OpLoginScanMaskFailRejC93 = novaSetupGroup.OpLoginScanMaskFailRejC93,
				OpLoginScanMaskFailRejCbar = novaSetupGroup.OpLoginScanMaskFailRejCbar,
				OpLoginScanRequireAccept = novaSetupGroup.OpLoginScanRequireAccept,
				OpLoginSm2D = novaSetupGroup.OpLoginSm2D,
				OpLoginSmC128 = novaSetupGroup.OpLoginSmC128,
				OpLoginSmC2o5 = novaSetupGroup.OpLoginSmC2o5,
				OpLoginSmC39 = novaSetupGroup.OpLoginSmC39,
				OpLoginSmC93 = novaSetupGroup.OpLoginSmC93,
				OpLoginSmCbar = novaSetupGroup.OpLoginSmCbar,
				OpLoginSupOverrideEnable = novaSetupGroup.OpLoginSupOverrideEnable,
				OpLoginValidation = novaSetupGroup.OpLoginValidation,
				SupOverrideSupOverrideEnable = novaSetupGroup.SupOverrideSupOverrideEnable,
				SupOvScanEnableCd = novaSetupGroup.SupOvScanEnableCd,
				SupOvScanRequireAccept = novaSetupGroup.SupOvScanRequireAccept,
				PatId2dSEnableCd = novaSetupGroup.PatId2dSEnableCd,
				PatIdAlphaEnable = novaSetupGroup.PatIdAlphaEnable,
				PatIdAutoEnabled = novaSetupGroup.PatIdAutoEnabled,
				PatIdFailCommentReq = novaSetupGroup.PatIdFailCommentReq,
				PatIdFailDowntimeEnable = novaSetupGroup.PatIdFailDowntimeEnable,
				PatIdFailNewPtEnable = novaSetupGroup.PatIdFailNewPtEnable,
				PatIdListEnable = novaSetupGroup.PatIdListEnable,
				PatIdNonBarcodeCommentReq = novaSetupGroup.PatIdNonBarcodeCommentReq,
				PatIdScanEnable2D = novaSetupGroup.PatIdScanEnable2D,
				PatIdScanEnableC128 = novaSetupGroup.PatIdScanEnableC128,
				PatIdScanEnableC2o5 = novaSetupGroup.PatIdScanEnableC2o5,
				PatIdScanEnableC39 = novaSetupGroup.PatIdScanEnableC39,
				PatIdScanEnableC93 = novaSetupGroup.PatIdScanEnableC93,
				PatIdScanEnableCbar = novaSetupGroup.PatIdScanEnableCbar,
				PatIdScanEnableCd = novaSetupGroup.PatIdScanEnableCd,
				PatIdScanMaskFailRejC128 = novaSetupGroup.PatIdScanMaskFailRejC128,
				PatIdScanMaskFailRejC2o5 = novaSetupGroup.PatIdScanMaskFailRejC2o5,
				PatIdScanMaskFailRejC39 = novaSetupGroup.PatIdScanMaskFailRejC39,
				PatIdScanMaskFailRejC93 = novaSetupGroup.PatIdScanMaskFailRejC93,
				PatIdScanMaskFailRejCbar = novaSetupGroup.PatIdScanMaskFailRejCbar,
				PatIdScanRequireAccept = novaSetupGroup.PatIdScanRequireAccept,
				PatIdSm2D = novaSetupGroup.PatIdSm2D,
				PatIdSmC128 = novaSetupGroup.PatIdSmC128,
				PatIdSmC2o5 = novaSetupGroup.PatIdSmC2o5,
				PatIdSmC39 = novaSetupGroup.PatIdSmC39,
				PatIdSmC93 = novaSetupGroup.PatIdSmC93,
				PatIdSmCbar = novaSetupGroup.PatIdSmCbar,
				PatIdSupOverrideEnable = novaSetupGroup.PatIdSupOverrideEnable,
				PatIdTgcEnable = novaSetupGroup.PatIdTgcEnable,
				PatIdValidation = novaSetupGroup.PatIdValidation,
				CommentsFreeTextChartableEnable = novaSetupGroup.CommentsFreeTextChartableEnable,
				CommentsFreeTextEnable = novaSetupGroup.CommentsFreeTextEnable,
				CommentsFreeTextFlaggedEnable = novaSetupGroup.CommentsFreeTextFlaggedEnable,
				LinObsFailCommentReqCd = novaSetupGroup.LinObsFailCommentReqCd,
				LinObsPassCommentReqCd = novaSetupGroup.LinObsPassCommentReqCd,
				LinObsValueDisplay = novaSetupGroup.LinObsValueDisplay,
				ObsAbnormalRangeCommentReq = novaSetupGroup.ObsAbnormalRangeCommentReq,
				ObsCriticalRangeCommentReq = novaSetupGroup.ObsCriticalRangeCommentReq,
				ObsNormalRangeCommentReq = novaSetupGroup.ObsNormalRangeCommentReq,
				ObsRejectEnable = novaSetupGroup.ObsRejectEnable,
				ObsRejectResultCommentReq = novaSetupGroup.ObsRejectResultCommentReq,
				ObsRejectSupOverrideReq = novaSetupGroup.ObsRejectSupOverrideReq,
				ObsReviewNoLogin = novaSetupGroup.ObsReviewNoLogin,
				ObsTechnicalRangeCommentReq = novaSetupGroup.ObsTechnicalRangeCommentReq,
				QcObsFailCommentReqCd = novaSetupGroup.QcObsFailCommentReqCd,
				QcObsPassCommentReqCd = novaSetupGroup.QcObsPassCommentReqCd,
				QcObsValueDisplay = novaSetupGroup.QcObsValueDisplay,
				PhysId2dSEnableCd = novaSetupGroup.PhysId2dSEnableCd,
				PhysIdAlphaEnable = novaSetupGroup.PhysIdAlphaEnable,
				PhysIdListEnable = novaSetupGroup.PhysIdListEnable,
				PhysIdNonBarcodeCommentReq = novaSetupGroup.PhysIdNonBarcodeCommentReq,
				PhysIdPromptEnable = novaSetupGroup.PhysIdPromptEnable,
				PhysIdScanEnable2D = novaSetupGroup.PhysIdScanEnable2D,
				PhysIdScanEnableC128 = novaSetupGroup.PhysIdScanEnableC128,
				PhysIdScanEnableC2o5 = novaSetupGroup.PhysIdScanEnableC2o5,
				PhysIdScanEnableC39 = novaSetupGroup.PhysIdScanEnableC39,
				PhysIdScanEnableC93 = novaSetupGroup.PhysIdScanEnableC93,
				PhysIdScanEnableCbar = novaSetupGroup.PhysIdScanEnableCbar,
				PhysIdScanEnableCd = novaSetupGroup.PhysIdScanEnableCd,
				PhysIdScanMaskFailRejC128 = novaSetupGroup.PhysIdScanMaskFailRejC128,
				PhysIdScanMaskFailRejC2o5 = novaSetupGroup.PhysIdScanMaskFailRejC2o5,
				PhysIdScanMaskFailRejC39 = novaSetupGroup.PhysIdScanMaskFailRejC39,
				PhysIdScanMaskFailRejC93 = novaSetupGroup.PhysIdScanMaskFailRejC93,
				PhysIdScanMaskFailRejCbar = novaSetupGroup.PhysIdScanMaskFailRejCbar,
				PhysIdScanRequireAccept = novaSetupGroup.PhysIdScanRequireAccept,
				PhysIdSm2D = novaSetupGroup.PhysIdSm2D,
				PhysIdSmC128 = novaSetupGroup.PhysIdSmC128,
				PhysIdSmC2o5 = novaSetupGroup.PhysIdSmC2o5,
				PhysIdSmC39 = novaSetupGroup.PhysIdSmC39,
				PhysIdSmC93 = novaSetupGroup.PhysIdSmC93,
				PhysIdSmCbar = novaSetupGroup.PhysIdSmCbar,
				PhysIdSupOverrideEnable = novaSetupGroup.PhysIdSupOverrideEnable,
				PhysIdValidation = novaSetupGroup.PhysIdValidation,
				PrivLevelAdminRenameMeterCd = novaSetupGroup.PrivLevelAdminRenameMeterCd,
				PrivLevelAdminResetFacilityCd = novaSetupGroup.PrivLevelAdminResetFacilityCd,
				PrivLevelAdminSetNetworkCd = novaSetupGroup.PrivLevelAdminSetNetworkCd,
				PrivLevelAdminUnarchiveBitCd = novaSetupGroup.PrivLevelAdminUnarchiveBitCd,
				PrivLevelDockLockOvCd = novaSetupGroup.PrivLevelDockLockOvCd,
				PrivLevelSetDateTimeCd = novaSetupGroup.PrivLevelSetDateTimeCd,
				PrivLevelTesttypeCorrelationCd = novaSetupGroup.PrivLevelTesttypeCorrelationCd,
				PrivLevelTesttypeLinearityCd = novaSetupGroup.PrivLevelTesttypeLinearityCd,
				PrivLevelTesttypeMaintCd = novaSetupGroup.PrivLevelTesttypeMaintCd,
				PrivLevelTesttypeProficiencyCd = novaSetupGroup.PrivLevelTesttypeProficiencyCd,
				PrivLevelTesttypeTrainingCd = novaSetupGroup.PrivLevelTesttypeTrainingCd,
				ProfLot2dSEnableCd = novaSetupGroup.ProfLot2dSEnableCd,
				ProfLotAlphaEnable = novaSetupGroup.ProfLotAlphaEnable,
				ProfLotListEnable = novaSetupGroup.ProfLotListEnable,
				ProfLotMaxLength = novaSetupGroup.ProfLotMaxLength,
				ProfLotMinLength = novaSetupGroup.ProfLotMinLength,
				ProfLotNonBarcodeCommentReq = novaSetupGroup.ProfLotNonBarcodeCommentReq,
				ProfLotScanEnableCd = novaSetupGroup.ProfLotScanEnableCd,
				ProfLotScanRequireAccept = novaSetupGroup.ProfLotScanRequireAccept,
				ProfLotSupOverrideEnable = novaSetupGroup.ProfLotSupOverrideEnable,
				ProfLotValidation = novaSetupGroup.ProfLotValidation,
				ProfRejectEnable = novaSetupGroup.ProfRejectEnable,
				QcLockAlertMins = novaSetupGroup.QcLockAlertMins,
				QcLockElapsedHrs = novaSetupGroup.QcLockElapsedHrs,
				QcLockInterval = novaSetupGroup.QcLockInterval,
				QcLockKetAlertMins = novaSetupGroup.QcLockKetAlertMins,
				QcLockKetElapsedHrs = novaSetupGroup.QcLockKetElapsedHrs,
				QcLockKetInterval = novaSetupGroup.QcLockKetInterval,
				QcLockKetLevel1Req = novaSetupGroup.QcLockKetLevel1Req,
				QcLockKetLevel2Req = novaSetupGroup.QcLockKetLevel2Req,
				QcLockKetLevel3Req = novaSetupGroup.QcLockKetLevel3Req,
				QcLockKetModeCd = novaSetupGroup.QcLockKetModeCd,
				QcLockKetShiftTimes = novaSetupGroup.QcLockKetShiftTimes,
				QcLockLevel1Req = novaSetupGroup.QcLockLevel1Req,
				QcLockLevel2Req = novaSetupGroup.QcLockLevel2Req,
				QcLockLevel3Req = novaSetupGroup.QcLockLevel3Req,
				QcLockLevel4Req = novaSetupGroup.QcLockLevel4Req,
				QcLockModeCd = novaSetupGroup.QcLockModeCd,
				QcLockShiftTimes = novaSetupGroup.QcLockShiftTimes,
				QcLot2dSEnableCd = novaSetupGroup.QcLot2dSEnableCd,
				QcLotListEnable = novaSetupGroup.QcLotListEnable,
				QcLotNonBarcodeCommentReq = novaSetupGroup.QcLotNonBarcodeCommentReq,
				QcLotScanEnableCd = novaSetupGroup.QcLotScanEnableCd,
				QcLotScanRequireAccept = novaSetupGroup.QcLotScanRequireAccept,
				QcLotSupOverrideEnable = novaSetupGroup.QcLotSupOverrideEnable,
				QcLotValidation = novaSetupGroup.QcLotValidation,
				AccnIdPromptText = novaSetupGroup.AccnIdPromptText,
				ObsIdMethodCd = novaSetupGroup.ObsIdMethodCd,
				PatIdPromptText = novaSetupGroup.PatIdPromptText,
				SampleTypeSelectEnable = novaSetupGroup.SampleTypeSelectEnable,
				StripId2dSEnableCd = novaSetupGroup.StripId2dSEnableCd,
				StripIdAutoEnabled = novaSetupGroup.StripIdAutoEnabled,
				StripIdDefaultLastStripId = novaSetupGroup.StripIdDefaultLastStripId,
				StripIdListEnable = novaSetupGroup.StripIdListEnable,
				StripIdNonBarcodeCommentReq = novaSetupGroup.StripIdNonBarcodeCommentReq,
				StripIdScanEnableCd = novaSetupGroup.StripIdScanEnableCd,
				StripIdScanRequireAccept = novaSetupGroup.StripIdScanRequireAccept,
				StripIdSupOverrideEnable = novaSetupGroup.StripIdSupOverrideEnable,
				StripIdValidation = novaSetupGroup.StripIdValidation
			};
			DbContext.NovaSetups.Add(entity);
			TestRange testRange = DbContext.TestRanges.FirstOrDefault((TestRange l) => l.Id == lid);
			if (testRange == null)
			{
				TestRange entity2 = new TestRange
				{
					Id = lid,
					SL = novaSetupGroup.SL,
					IC = novaSetupGroup.IC
				};
				DbContext.TestRanges.Add(entity2);
			}
			else
			{
				testRange.SL = novaSetupGroup.SL;
				testRange.IC = novaSetupGroup.IC;
				testRange.UpdateTime = DateTime.Now;
			}
			DbContext.SaveChanges();
		}
		else
		{
			novaSetup.AccnIdAlphaEnable = novaSetupGroup.AccnIdAlphaEnable;
			novaSetup.AccnIdScanRequireAccept = novaSetupGroup.AccnIdScanRequireAccept;
			novaSetup.AccnId2DsEnableCd = novaSetupGroup.AccnId2DsEnableCd;
			novaSetup.AccnIdScanEnableCd = novaSetupGroup.AccnIdScanEnableCd;
			novaSetup.DxIdListEnable = novaSetupGroup.DxIdListEnable;
			novaSetup.DxIdPromptEnable = novaSetupGroup.DxIdPromptEnable;
			novaSetup.DxIdScanRequireAccept = novaSetupGroup.DxIdScanRequireAccept;
			novaSetup.DxIdSupOverrideEnable = novaSetupGroup.DxIdSupOverrideEnable;
			novaSetup.DxIdValidation = novaSetupGroup.DxIdValidation;
			novaSetup.DxId2dSEnableCd = novaSetupGroup.DxId2dSEnableCd;
			novaSetup.DxIdScanEnableCd = novaSetupGroup.DxIdScanEnableCd;
			novaSetup.DockLockSupOverrideEnable = novaSetupGroup.DockLockSupOverrideEnable;
			novaSetup.ArchivedObsRetainDays = novaSetupGroup.ArchivedObsRetainDays;
			novaSetup.ArchivedOvrwDisregardArchBit = novaSetupGroup.ArchivedOvrwDisregardArchBit;
			novaSetup.DockLockAlertMins = novaSetupGroup.DockLockAlertMins;
			novaSetup.DockLockModeCd = novaSetupGroup.DockLockModeCd;
			novaSetup.DockLockInterval = novaSetupGroup.DockLockInterval;
			novaSetup.DockLockShiftTimes = novaSetupGroup.DockLockShiftTimes;
			novaSetup.DockLockElapsedHrs = novaSetupGroup.DockLockElapsedHrs;
			novaSetup.LinLotListEnable = novaSetupGroup.LinLotListEnable;
			novaSetup.LinLot2dSEnableCd = novaSetupGroup.LinLot2dSEnableCd;
			novaSetup.LinLotScanEnableCd = novaSetupGroup.LinLotScanEnableCd;
			novaSetup.LinLotScanRequireAccept = novaSetupGroup.LinLotScanRequireAccept;
			novaSetup.LinLotSupOverrideEnable = novaSetupGroup.LinLotSupOverrideEnable;
			novaSetup.LinLotValidation = novaSetupGroup.LinLotValidation;
			novaSetup.OpLogoffElapsedSecs = novaSetupGroup.OpLogoffElapsedSecs;
			novaSetup.OpLogoffModeCd = novaSetupGroup.OpLogoffModeCd;
			novaSetup.OpLoginScanRequireAccept = novaSetupGroup.OpLoginScanRequireAccept;
			novaSetup.OpLoginAlphaEnable = novaSetupGroup.OpLoginAlphaEnable;
			novaSetup.OpLogin2dSEnableCd = novaSetupGroup.OpLogin2dSEnableCd;
			novaSetup.OpLoginScanEnableCd = novaSetupGroup.OpLoginScanEnableCd;
			novaSetup.OpLoginSupOverrideEnable = novaSetupGroup.OpLoginSupOverrideEnable;
			novaSetup.OpLoginValidation = novaSetupGroup.OpLoginValidation;
			novaSetup.OpLoginDisplayCd = novaSetupGroup.OpLoginDisplayCd;
			novaSetup.SupOvScanRequireAccept = novaSetupGroup.SupOvScanRequireAccept;
			novaSetup.SupOvScanEnableCd = novaSetupGroup.SupOvScanEnableCd;
			novaSetup.PatIdAutoEnabled = novaSetupGroup.PatIdAutoEnabled;
			novaSetup.PatIdAlphaEnable = novaSetupGroup.PatIdAlphaEnable;
			novaSetup.PatIdFailDowntimeEnable = novaSetupGroup.PatIdFailDowntimeEnable;
			novaSetup.PatIdFailNewPtEnable = novaSetupGroup.PatIdFailNewPtEnable;
			novaSetup.PatIdListEnable = novaSetupGroup.PatIdListEnable;
			novaSetup.PatId2dSEnableCd = novaSetupGroup.PatId2dSEnableCd;
			novaSetup.PatIdScanEnableCd = novaSetupGroup.PatIdScanEnableCd;
			novaSetup.PatIdScanRequireAccept = novaSetupGroup.PatIdScanRequireAccept;
			novaSetup.PatIdSupOverrideEnable = novaSetupGroup.PatIdSupOverrideEnable;
			novaSetup.PatIdTgcEnable = novaSetupGroup.PatIdTgcEnable;
			novaSetup.PatIdValidation = novaSetupGroup.PatIdValidation;
			novaSetup.CommentsFreeTextChartableEnable = novaSetupGroup.CommentsFreeTextChartableEnable;
			novaSetup.CommentsFreeTextFlaggedEnable = novaSetupGroup.CommentsFreeTextFlaggedEnable;
			novaSetup.ObsReviewNoLogin = novaSetupGroup.ObsReviewNoLogin;
			novaSetup.LinObsValueDisplay = novaSetupGroup.LinObsValueDisplay;
			novaSetup.CommentsFreeTextEnable = novaSetupGroup.CommentsFreeTextEnable;
			novaSetup.ObsRejectEnable = novaSetupGroup.ObsRejectEnable;
			novaSetup.QcObsValueDisplay = novaSetupGroup.QcObsValueDisplay;
			novaSetup.QcObsFailCommentReqCd = novaSetupGroup.QcObsFailCommentReqCd;
			novaSetup.LinObsFailCommentReqCd = novaSetupGroup.LinObsFailCommentReqCd;
			novaSetup.ObsRejectResultCommentReq = novaSetupGroup.ObsRejectResultCommentReq;
			novaSetup.ObsCriticalRangeCommentReq = novaSetupGroup.ObsCriticalRangeCommentReq;
			novaSetup.ObsTechnicalRangeCommentReq = novaSetupGroup.ObsTechnicalRangeCommentReq;
			novaSetup.QcObsPassCommentReqCd = novaSetupGroup.QcObsPassCommentReqCd;
			novaSetup.LinObsPassCommentReqCd = novaSetupGroup.LinObsPassCommentReqCd;
			novaSetup.ObsNormalRangeCommentReq = novaSetupGroup.ObsNormalRangeCommentReq;
			novaSetup.ObsAbnormalRangeCommentReq = novaSetupGroup.ObsAbnormalRangeCommentReq;
			novaSetup.PhysIdAlphaEnable = novaSetupGroup.PhysIdAlphaEnable;
			novaSetup.PhysIdListEnable = novaSetupGroup.PhysIdListEnable;
			novaSetup.PhysIdPromptEnable = novaSetupGroup.PhysIdPromptEnable;
			novaSetup.PhysId2dSEnableCd = novaSetupGroup.PhysId2dSEnableCd;
			novaSetup.PhysIdScanEnableCd = novaSetupGroup.PhysIdScanEnableCd;
			novaSetup.PhysIdScanRequireAccept = novaSetupGroup.PhysIdScanRequireAccept;
			novaSetup.PhysIdSupOverrideEnable = novaSetupGroup.PhysIdSupOverrideEnable;
			novaSetup.PhysIdValidation = novaSetupGroup.PhysIdValidation;
			novaSetup.PrivLevelAdminRenameMeterCd = novaSetupGroup.PrivLevelAdminRenameMeterCd;
			novaSetup.PrivLevelAdminResetFacilityCd = novaSetupGroup.PrivLevelAdminResetFacilityCd;
			novaSetup.PrivLevelAdminSetNetworkCd = novaSetupGroup.PrivLevelAdminSetNetworkCd;
			novaSetup.PrivLevelAdminUnarchiveBitCd = novaSetupGroup.PrivLevelAdminUnarchiveBitCd;
			novaSetup.PrivLevelTesttypeLinearityCd = novaSetupGroup.PrivLevelTesttypeLinearityCd;
			novaSetup.PrivLevelTesttypeProficiencyCd = novaSetupGroup.PrivLevelTesttypeProficiencyCd;
			novaSetup.PrivLevelDockLockOvCd = novaSetupGroup.PrivLevelDockLockOvCd;
			novaSetup.PrivLevelSetDateTimeCd = novaSetupGroup.PrivLevelSetDateTimeCd;
			novaSetup.QcLockAlertMins = novaSetupGroup.QcLockAlertMins;
			novaSetup.QcLockLevel1Req = novaSetupGroup.QcLockLevel1Req;
			novaSetup.QcLockLevel2Req = novaSetupGroup.QcLockLevel2Req;
			novaSetup.QcLockLevel3Req = novaSetupGroup.QcLockLevel3Req;
			novaSetup.QcLockModeCd = novaSetupGroup.QcLockModeCd;
			novaSetup.QcLockInterval = novaSetupGroup.QcLockInterval;
			novaSetup.QcLockElapsedHrs = novaSetupGroup.QcLockElapsedHrs;
			novaSetup.QcLockShiftTimes = novaSetupGroup.QcLockShiftTimes;
			novaSetup.QcLotListEnable = novaSetupGroup.QcLotListEnable;
			novaSetup.QcLot2dSEnableCd = novaSetupGroup.QcLot2dSEnableCd;
			novaSetup.QcLotScanEnableCd = novaSetupGroup.QcLotScanEnableCd;
			novaSetup.QcLotScanRequireAccept = novaSetupGroup.QcLotScanRequireAccept;
			novaSetup.QcLotSupOverrideEnable = novaSetupGroup.QcLotSupOverrideEnable;
			novaSetup.QcLotValidation = novaSetupGroup.QcLotValidation;
			novaSetup.ObsIdMethodCd = novaSetupGroup.ObsIdMethodCd;
			novaSetup.AccnIdPromptText = novaSetupGroup.AccnIdPromptText;
			novaSetup.PatIdPromptText = novaSetupGroup.PatIdPromptText;
			novaSetup.StripIdAutoEnabled = novaSetupGroup.StripIdAutoEnabled;
			novaSetup.StripIdDefaultLastStripId = novaSetupGroup.StripIdDefaultLastStripId;
			novaSetup.StripIdListEnable = novaSetupGroup.StripIdListEnable;
			novaSetup.StripId2dSEnableCd = novaSetupGroup.StripId2dSEnableCd;
			novaSetup.StripIdScanEnableCd = novaSetupGroup.StripIdScanEnableCd;
			novaSetup.StripIdScanRequireAccept = novaSetupGroup.StripIdScanRequireAccept;
			novaSetup.StripIdSupOverrideEnable = novaSetupGroup.StripIdSupOverrideEnable;
			novaSetup.StripIdValidation = novaSetupGroup.StripIdValidation;
			novaSetup.Location.TestRange.SL = novaSetupGroup.SL;
			novaSetup.Location.TestRange.IC = novaSetupGroup.IC;
		}
		await DbContext.SaveChangesAsync();
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
}
