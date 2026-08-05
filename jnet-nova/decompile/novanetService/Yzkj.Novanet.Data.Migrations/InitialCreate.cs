using System.CodeDom.Compiler;
using System.Data.Entity.Migrations;
using System.Data.Entity.Migrations.Builders;
using System.Data.Entity.Migrations.Infrastructure;
using System.Resources;

namespace Yzkj.Novanet.Data.Migrations;

[GeneratedCode("EntityFramework.Migrations", "6.1.3-40302")]
public sealed class InitialCreate : DbMigration, IMigrationMetadata
{
	private readonly ResourceManager Resources = new ResourceManager(typeof(InitialCreate));

	string IMigrationMetadata.Id => "201705270245458_InitialCreate";

	string IMigrationMetadata.Source => null;

	string IMigrationMetadata.Target => Resources.GetString("Target");

	public override void Up()
	{
		CreateTable("dbo.Devices", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			SerialNo = c.String(),
			Name = c.String(),
			Hospital = c.String(),
			Depart = c.String(),
			LocationId = c.Int(false),
			LastTime = c.DateTime(false),
			ObservationsUpdateDttm = c.DateTime(),
			OperatorsUpdateDttm = c.DateTime(),
			EventsUpdateDttm = c.DateTime(),
			PatientsUpdateDttm = c.DateTime(),
			SetupUpdateDttm = c.DateTime(),
			PhysUpdateDttm = c.DateTime(),
			ReagUpdateDttm = c.DateTime(),
			LocListUpdateDttm = c.DateTime()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true).Index(t => t.LocationId);
		CreateTable("dbo.Locations", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Name = c.String(),
			Level = c.Int(false),
			ParentId = c.Int(),
			CreateTime = c.DateTime(false),
			UpdateTime = c.DateTime(),
			IsDeleted = c.Boolean(false),
			DeleteTime = c.DateTime()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.ParentId).Index(t => t.Level)
			.Index(t => t.ParentId);
		CreateTable("dbo.LocationDiagcodes", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			LocationId = c.Int(false),
			DiagcodeId = c.Int(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Diagcodes", t => t.DiagcodeId, cascadeDelete: true).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true)
			.Index(t => t.LocationId)
			.Index(t => t.DiagcodeId);
		CreateTable("dbo.Diagcodes", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Code = c.String(null, 16),
			Description = c.String()
		}).PrimaryKey(t => t.Id).Index(t => t.Code);
		CreateTable("dbo.DiagcodeGroups", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Name = c.String()
		}).PrimaryKey(t => t.Id);
		CreateTable("dbo.LocationNurses", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			LocationId = c.Int(false),
			NurseId = c.Int(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true).ForeignKey("dbo.Nurses", t => t.NurseId, cascadeDelete: true)
			.Index(t => t.LocationId)
			.Index(t => t.NurseId);
		CreateTable("dbo.Nurses", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Code = c.String(null, 16),
			Name = c.String(),
			SyncStatus = c.Int(false),
			SyncTime = c.DateTime(),
			IsDelete = c.Boolean(false),
			DeleteTime = c.DateTime(),
			CreateTime = c.DateTime(false)
		}).PrimaryKey(t => t.Id).Index(t => t.Code).Index(t => t.SyncStatus)
			.Index(t => t.SyncTime)
			.Index(t => t.IsDelete)
			.Index(t => t.CreateTime);
		CreateTable("dbo.Patients", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			PatientId = c.String(null, 64),
			MedicalRecord = c.String(null, 64),
			Account = c.String(null, 64),
			Name = c.String(null, 16),
			Gender = c.Int(false),
			Birthday = c.DateTime(false),
			IdCard = c.String(),
			WardNo = c.String(null, 16),
			BedNo = c.String(null, 16),
			AdmissionDate = c.DateTime(false),
			Status = c.Int(false),
			DischargeDate = c.DateTime(),
			SyncStatus = c.Int(false),
			SyncTime = c.DateTime(),
			Source = c.Int(false),
			IsDelete = c.Boolean(false),
			DeleteTime = c.DateTime(),
			CreateTime = c.DateTime(false),
			LocationId = c.Int(false),
			NurseId = c.Int()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true).ForeignKey("dbo.Nurses", t => t.NurseId)
			.Index(t => t.PatientId)
			.Index(t => t.MedicalRecord)
			.Index(t => t.Account)
			.Index(t => t.Name)
			.Index(t => t.Gender)
			.Index(t => t.Birthday)
			.Index(t => t.WardNo)
			.Index(t => t.BedNo)
			.Index(t => t.AdmissionDate)
			.Index(t => t.Status)
			.Index(t => t.DischargeDate)
			.Index(t => t.SyncStatus)
			.Index(t => t.SyncTime)
			.Index(t => t.Source)
			.Index(t => t.IsDelete)
			.Index(t => t.CreateTime)
			.Index(t => t.LocationId)
			.Index(t => t.NurseId);
		CreateTable("dbo.Preferences", (ColumnBuilder c) => new
		{
			Id = c.Int(false),
			AutoReConnect = c.Boolean(false),
			CycleMinutes = c.Int(),
			PatientID = c.Int(),
			CreateTime = c.DateTime(false),
			UpdateTime = c.DateTime()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.Id).Index(t => t.Id);
		CreateTable("dbo.TestRanges", (ColumnBuilder c) => new
		{
			Id = c.Int(false),
			LowCricital = c.Decimal(false, (byte)18, (byte)2),
			LowNormal = c.Decimal(false, (byte)18, (byte)2),
			HighNormal = c.Decimal(false, (byte)18, (byte)2),
			HighCricital = c.Decimal(false, (byte)18, (byte)2),
			Sex = c.Int(false),
			AgeLow = c.Int(false),
			AgeHigh = c.Int(false),
			Remark = c.String(),
			IsDeleted = c.Boolean(false),
			DeleteTime = c.DateTime(false),
			CreateTime = c.DateTime(false),
			UpdateTime = c.DateTime()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.Id).Index(t => t.Id)
			.Index(t => t.IsDeleted)
			.Index(t => t.CreateTime)
			.Index(t => t.UpdateTime);
		CreateTable("dbo.DischargeClocks", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			IsEnabled = c.Boolean(false),
			Hour = c.Int(false),
			Minute = c.Int(false),
			LocationId = c.Int(false),
			SaveTime = c.DateTime(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true).Index(t => t.LocationId);
		CreateTable("dbo.LocationReagents", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			LocationId = c.Int(false),
			ReagentId = c.Int(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.LocationId, cascadeDelete: true).ForeignKey("dbo.Reagents", t => t.ReagentId, cascadeDelete: true)
			.Index(t => t.LocationId)
			.Index(t => t.ReagentId);
		CreateTable("dbo.Reagents", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			LotNum = c.String(),
			LotType = c.Int(false),
			High = c.Decimal(null, (byte)18, (byte)2),
			Low = c.Decimal(null, (byte)18, (byte)2),
			Expiration = c.DateTime(false),
			CreateTime = c.DateTime(false)
		}).PrimaryKey(t => t.Id).Index(t => t.LotType).Index(t => t.Expiration)
			.Index(t => t.CreateTime);
		CreateTable("dbo.ReagentGroups", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			Name = c.String()
		}).PrimaryKey(t => t.Id);
		CreateTable("dbo.NovaLogs", (ColumnBuilder c) => new
		{
			log_id = c.Long(false, identity: true),
			log_date = c.DateTime(false),
			log_level = c.Int(false),
			log_source = c.String(),
			log_message = c.String(),
			log_machine_name = c.String(),
			log_user_name = c.String(),
			log_exception = c.String(),
			log_stacktrace = c.String()
		}).PrimaryKey(t => t.log_id);
		CreateTable("dbo.NovaSetups", (ColumnBuilder c) => new
		{
			Id = c.Int(false),
			SaveTime = c.DateTime(false),
			AccnId2DsEnableCd = c.String(),
			AccnIdAlphaEnable = c.String(),
			AccnIdListEnable = c.String(),
			AccnIdNonBarcodeCommentReq = c.String(),
			AccnIdScanEnable2D = c.String(),
			AccnIdScanEnableC128 = c.String(),
			AccnIdScanEnableC2o5 = c.String(),
			AccnIdScanEnableC39 = c.String(),
			AccnIdScanEnableC93 = c.String(),
			AccnIdScanEnableCbar = c.String(),
			AccnIdScanEnableCd = c.String(),
			AccnIdScanMaskFailRejC128 = c.String(),
			AccnIdScanMaskFailRejC2o5 = c.String(),
			AccnIdScanMaskFailRejC39 = c.String(),
			AccnIdScanMaskFailRejC93 = c.String(),
			AccnIdScanMaskFailRejCbar = c.String(),
			AccnIdScanRequireAccept = c.String(),
			AccnIdSm2D = c.String(),
			AccnIdSmC128 = c.String(),
			AccnIdSmC2o5 = c.String(),
			AccnIdSmC39 = c.String(),
			AccnIdSmC93 = c.String(),
			AccnIdSmCbar = c.String(),
			AccnIdSupOverrideEnable = c.String(),
			AccnIdValidation = c.String(),
			DxId2dSEnableCd = c.String(),
			DxIdAlphaEnable = c.String(),
			DxIdListEnable = c.String(),
			DxIdNonBarcodeCommentReq = c.String(),
			DxIdPromptEnable = c.String(),
			DxIdScanEnable2D = c.String(),
			DxIdScanEnableC128 = c.String(),
			DxIdScanEnableC2o5 = c.String(),
			DxIdScanEnableC39 = c.String(),
			DxIdScanEnableC93 = c.String(),
			DxIdScanEnableCbar = c.String(),
			DxIdScanEnableCd = c.String(),
			DxIdScanMaskFailRejC128 = c.String(),
			DxIdScanMaskFailRejC2o5 = c.String(),
			DxIdScanMaskFailRejC39 = c.String(),
			DxIdScanMaskFailRejC93 = c.String(),
			DxIdScanMaskFailRejCbar = c.String(),
			DxIdScanRequireAccept = c.String(),
			DxIdSm2D = c.String(),
			DxIdSmC128 = c.String(),
			DxIdSmC2o5 = c.String(),
			DxIdSmC39 = c.String(),
			DxIdSmC93 = c.String(),
			DxIdSmCbar = c.String(),
			DxIdSupOverrideEnable = c.String(),
			DxIdValidation = c.String(),
			ArchivedObsRetainDays = c.String(),
			ArchivedOvrwDisregardArchBit = c.String(),
			DockLockAlertMins = c.String(),
			DockLockElapsedHrs = c.String(),
			DockLockInterval = c.String(),
			DockLockModeCd = c.String(),
			DockLockShiftTimes = c.String(),
			DockLockSupOverrideEnable = c.String(),
			AccnIdMaxLength = c.String(),
			AccnIdMinLength = c.String(),
			DxIdMaxLength = c.String(),
			DxIdMinLength = c.String(),
			OpLoginMaxLength = c.String(),
			OpLoginMinLength = c.String(),
			PatIdMaxLength = c.String(),
			PatIdMinLength = c.String(),
			PhysIdMaxLength = c.String(),
			PhysIdMinLength = c.String(),
			LinLot2dSEnableCd = c.String(),
			LinLotListEnable = c.String(),
			LinLotNonBarcodeCmntReq = c.String(),
			LinLotScanEnableCd = c.String(),
			LinLotScanRequireAccept = c.String(),
			LinLotSupOverrideEnable = c.String(),
			LinLotValidation = c.String(),
			PatIdTypeCd = c.String(),
			OpLogoffElapsedSecs = c.String(),
			OpLogoffModeCd = c.String(),
			DateFormat = c.String(),
			MeterMaxLinRec = c.String(),
			MeterMaxPatRec = c.String(),
			MeterMaxProfRec = c.String(),
			MeterMaxQCRec = c.String(),
			TimeFormat = c.String(),
			OpLogin2dSEnableCd = c.String(),
			OpLoginAlphaEnable = c.String(),
			OpLoginDisplayCd = c.String(),
			OpLoginNonBarcodeCommentReq = c.String(),
			OpLoginScanEnable2D = c.String(),
			OpLoginScanEnableC128 = c.String(),
			OpLoginScanEnableC2o5 = c.String(),
			OpLoginScanEnableC39 = c.String(),
			OpLoginScanEnableC93 = c.String(),
			OpLoginScanEnableCbar = c.String(),
			OpLoginScanEnableCd = c.String(),
			OpLoginScanMaskFailRejC128 = c.String(),
			OpLoginScanMaskFailRejC2o5 = c.String(),
			OpLoginScanMaskFailRejC39 = c.String(),
			OpLoginScanMaskFailRejC93 = c.String(),
			OpLoginScanMaskFailRejCbar = c.String(),
			OpLoginScanRequireAccept = c.String(),
			OpLoginSm2D = c.String(),
			OpLoginSmC128 = c.String(),
			OpLoginSmC2o5 = c.String(),
			OpLoginSmC39 = c.String(),
			OpLoginSmC93 = c.String(),
			OpLoginSmCbar = c.String(),
			OpLoginSupOverrideEnable = c.String(),
			OpLoginValidation = c.String(),
			SupOverrideSupOverrideEnable = c.String(),
			SupOvScanEnableCd = c.String(),
			SupOvScanRequireAccept = c.String(),
			PatId2dSEnableCd = c.String(),
			PatIdAlphaEnable = c.String(),
			PatIdAutoEnabled = c.String(),
			PatIdFailCommentReq = c.String(),
			PatIdFailDowntimeEnable = c.String(),
			PatIdFailNewPtEnable = c.String(),
			PatIdListEnable = c.String(),
			PatIdNonBarcodeCommentReq = c.String(),
			PatIdScanEnable2D = c.String(),
			PatIdScanEnableC128 = c.String(),
			PatIdScanEnableC2o5 = c.String(),
			PatIdScanEnableC39 = c.String(),
			PatIdScanEnableC93 = c.String(),
			PatIdScanEnableCbar = c.String(),
			PatIdScanEnableCd = c.String(),
			PatIdScanMaskFailRejC128 = c.String(),
			PatIdScanMaskFailRejC2o5 = c.String(),
			PatIdScanMaskFailRejC39 = c.String(),
			PatIdScanMaskFailRejC93 = c.String(),
			PatIdScanMaskFailRejCbar = c.String(),
			PatIdScanRequireAccept = c.String(),
			PatIdSm2D = c.String(),
			PatIdSmC128 = c.String(),
			PatIdSmC2o5 = c.String(),
			PatIdSmC39 = c.String(),
			PatIdSmC93 = c.String(),
			PatIdSmCbar = c.String(),
			PatIdSupOverrideEnable = c.String(),
			PatIdTgcEnable = c.String(),
			PatIdValidation = c.String(),
			CommentsFreeTextChartableEnable = c.String(),
			CommentsFreeTextEnable = c.String(),
			CommentsFreeTextFlaggedEnable = c.String(),
			LinObsFailCommentReqCd = c.String(),
			LinObsPassCommentReqCd = c.String(),
			LinObsValueDisplay = c.String(),
			ObsAbnormalRangeCommentReq = c.String(),
			ObsCriticalRangeCommentReq = c.String(),
			ObsNormalRangeCommentReq = c.String(),
			ObsRejectEnable = c.String(),
			ObsRejectResultCommentReq = c.String(),
			ObsRejectSupOverrideReq = c.String(),
			ObsReviewNoLogin = c.String(),
			ObsTechnicalRangeCommentReq = c.String(),
			QcObsFailCommentReqCd = c.String(),
			QcObsPassCommentReqCd = c.String(),
			QcObsValueDisplay = c.String(),
			PhysId2dSEnableCd = c.String(),
			PhysIdAlphaEnable = c.String(),
			PhysIdListEnable = c.String(),
			PhysIdNonBarcodeCommentReq = c.String(),
			PhysIdPromptEnable = c.String(),
			PhysIdScanEnable2D = c.String(),
			PhysIdScanEnableC128 = c.String(),
			PhysIdScanEnableC2o5 = c.String(),
			PhysIdScanEnableC39 = c.String(),
			PhysIdScanEnableC93 = c.String(),
			PhysIdScanEnableCbar = c.String(),
			PhysIdScanEnableCd = c.String(),
			PhysIdScanMaskFailRejC128 = c.String(),
			PhysIdScanMaskFailRejC2o5 = c.String(),
			PhysIdScanMaskFailRejC39 = c.String(),
			PhysIdScanMaskFailRejC93 = c.String(),
			PhysIdScanMaskFailRejCbar = c.String(),
			PhysIdScanRequireAccept = c.String(),
			PhysIdSm2D = c.String(),
			PhysIdSmC128 = c.String(),
			PhysIdSmC2o5 = c.String(),
			PhysIdSmC39 = c.String(),
			PhysIdSmC93 = c.String(),
			PhysIdSmCbar = c.String(),
			PhysIdSupOverrideEnable = c.String(),
			PhysIdValidation = c.String(),
			PrivLevelAdminRenameMeterCd = c.String(),
			PrivLevelAdminResetFacilityCd = c.String(),
			PrivLevelAdminSetNetworkCd = c.String(),
			PrivLevelAdminUnarchiveBitCd = c.String(),
			PrivLevelDockLockOvCd = c.String(),
			PrivLevelSetDateTimeCd = c.String(),
			PrivLevelTesttypeCorrelationCd = c.String(),
			PrivLevelTesttypeLinearityCd = c.String(),
			PrivLevelTesttypeMaintCd = c.String(),
			PrivLevelTesttypeProficiencyCd = c.String(),
			PrivLevelTesttypeTrainingCd = c.String(),
			ProfLot2dSEnableCd = c.String(),
			ProfLotAlphaEnable = c.String(),
			ProfLotListEnable = c.String(),
			ProfLotMaxLength = c.String(),
			ProfLotMinLength = c.String(),
			ProfLotNonBarcodeCommentReq = c.String(),
			ProfLotScanEnableCd = c.String(),
			ProfLotScanRequireAccept = c.String(),
			ProfLotSupOverrideEnable = c.String(),
			ProfLotValidation = c.String(),
			ProfRejectEnable = c.String(),
			QcLockAlertMins = c.String(),
			QcLockElapsedHrs = c.String(),
			QcLockInterval = c.String(),
			QcLockKetAlertMins = c.String(),
			QcLockKetElapsedHrs = c.String(),
			QcLockKetInterval = c.String(),
			QcLockKetLevel1Req = c.String(),
			QcLockKetLevel2Req = c.String(),
			QcLockKetLevel3Req = c.String(),
			QcLockKetModeCd = c.String(),
			QcLockKetShiftTimes = c.String(),
			QcLockLevel1Req = c.String(),
			QcLockLevel2Req = c.String(),
			QcLockLevel3Req = c.String(),
			QcLockLevel4Req = c.String(),
			QcLockModeCd = c.String(),
			QcLockShiftTimes = c.String(),
			QcLot2dSEnableCd = c.String(),
			QcLotListEnable = c.String(),
			QcLotNonBarcodeCommentReq = c.String(),
			QcLotScanEnableCd = c.String(),
			QcLotScanRequireAccept = c.String(),
			QcLotSupOverrideEnable = c.String(),
			QcLotValidation = c.String(),
			AccnIdPromptText = c.String(),
			ObsIdMethodCd = c.String(),
			PatIdPromptText = c.String(),
			SampleTypeSelectEnable = c.String(),
			StripId2dSEnableCd = c.String(),
			StripIdAutoEnabled = c.String(),
			StripIdDefaultLastStripId = c.String(),
			StripIdListEnable = c.String(),
			StripIdNonBarcodeCommentReq = c.String(),
			StripIdScanEnableCd = c.String(),
			StripIdScanRequireAccept = c.String(),
			StripIdSupOverrideEnable = c.String(),
			StripIdValidation = c.String()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Locations", t => t.Id).Index(t => t.Id);
		CreateTable("dbo.AspNetRoles", (ColumnBuilder c) => new
		{
			Id = c.String(false, 128),
			Name = c.String(false, 256)
		}).PrimaryKey(t => t.Id).Index(t => t.Name, "RoleNameIndex", unique: true);
		CreateTable("dbo.AspNetUserRoles", (ColumnBuilder c) => new
		{
			UserId = c.String(false, 128),
			RoleId = c.String(false, 128)
		}).PrimaryKey(t => new { t.UserId, t.RoleId }).ForeignKey("dbo.AspNetRoles", t => t.RoleId, cascadeDelete: true).ForeignKey("dbo.AspNetUsers", t => t.UserId, cascadeDelete: true)
			.Index(t => t.UserId)
			.Index(t => t.RoleId);
		CreateTable("dbo.SampleDatas", (ColumnBuilder c) => new
		{
			Id = c.Long(false, identity: true),
			PatientName = c.String(),
			PatientId = c.Int(false),
			NurseName = c.String(),
			NurseId = c.Int(),
			Hospital = c.String(),
			Depart = c.String(),
			Diagcode = c.String(),
			Reuslt = c.Decimal(false, (byte)18, (byte)2),
			Unit = c.String(),
			ObsStatus = c.String(),
			Interpretation = c.String(),
			NormalLimit = c.String(),
			CriticalLimit = c.String(),
			RgtLot = c.String(),
			ObsTime = c.DateTime(false),
			CreateTime = c.DateTime(false)
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.Nurses", t => t.PatientId, cascadeDelete: true).ForeignKey("dbo.Patients", t => t.PatientId, cascadeDelete: true)
			.Index(t => t.PatientId);
		CreateTable("dbo.AspNetUsers", (ColumnBuilder c) => new
		{
			Id = c.String(false, 128),
			ClearPassword = c.String(),
			LastTime = c.DateTime(),
			Email = c.String(null, 256),
			EmailConfirmed = c.Boolean(false),
			PasswordHash = c.String(),
			SecurityStamp = c.String(),
			PhoneNumber = c.String(),
			PhoneNumberConfirmed = c.Boolean(false),
			TwoFactorEnabled = c.Boolean(false),
			LockoutEndDateUtc = c.DateTime(),
			LockoutEnabled = c.Boolean(false),
			AccessFailedCount = c.Int(false),
			UserName = c.String(false, 256)
		}).PrimaryKey(t => t.Id).Index(t => t.UserName, "UserNameIndex", unique: true);
		CreateTable("dbo.AspNetUserClaims", (ColumnBuilder c) => new
		{
			Id = c.Int(false, identity: true),
			UserId = c.String(false, 128),
			ClaimType = c.String(),
			ClaimValue = c.String()
		}).PrimaryKey(t => t.Id).ForeignKey("dbo.AspNetUsers", t => t.UserId, cascadeDelete: true).Index(t => t.UserId);
		CreateTable("dbo.AspNetUserLogins", (ColumnBuilder c) => new
		{
			LoginProvider = c.String(false, 128),
			ProviderKey = c.String(false, 128),
			UserId = c.String(false, 128)
		}).PrimaryKey(t => new { t.LoginProvider, t.ProviderKey, t.UserId }).ForeignKey("dbo.AspNetUsers", t => t.UserId, cascadeDelete: true).Index(t => t.UserId);
		CreateTable("dbo.DiagcodeGroupDiagcodes", (ColumnBuilder c) => new
		{
			DiagcodeGroup_Id = c.Int(false),
			Diagcode_Id = c.Int(false)
		}).PrimaryKey(t => new { t.DiagcodeGroup_Id, t.Diagcode_Id }).ForeignKey("dbo.DiagcodeGroups", t => t.DiagcodeGroup_Id, cascadeDelete: true).ForeignKey("dbo.Diagcodes", t => t.Diagcode_Id, cascadeDelete: true)
			.Index(t => t.DiagcodeGroup_Id)
			.Index(t => t.Diagcode_Id);
		CreateTable("dbo.ReagentGroupReagents", (ColumnBuilder c) => new
		{
			ReagentGroup_Id = c.Int(false),
			Reagent_Id = c.Int(false)
		}).PrimaryKey(t => new { t.ReagentGroup_Id, t.Reagent_Id }).ForeignKey("dbo.ReagentGroups", t => t.ReagentGroup_Id, cascadeDelete: true).ForeignKey("dbo.Reagents", t => t.Reagent_Id, cascadeDelete: true)
			.Index(t => t.ReagentGroup_Id)
			.Index(t => t.Reagent_Id);
	}

	public override void Down()
	{
		DropForeignKey("dbo.AspNetUserRoles", "UserId", "dbo.AspNetUsers");
		DropForeignKey("dbo.AspNetUserLogins", "UserId", "dbo.AspNetUsers");
		DropForeignKey("dbo.AspNetUserClaims", "UserId", "dbo.AspNetUsers");
		DropForeignKey("dbo.SampleDatas", "PatientId", "dbo.Patients");
		DropForeignKey("dbo.SampleDatas", "PatientId", "dbo.Nurses");
		DropForeignKey("dbo.AspNetUserRoles", "RoleId", "dbo.AspNetRoles");
		DropForeignKey("dbo.NovaSetups", "Id", "dbo.Locations");
		DropForeignKey("dbo.LocationReagents", "ReagentId", "dbo.Reagents");
		DropForeignKey("dbo.ReagentGroupReagents", "Reagent_Id", "dbo.Reagents");
		DropForeignKey("dbo.ReagentGroupReagents", "ReagentGroup_Id", "dbo.ReagentGroups");
		DropForeignKey("dbo.LocationReagents", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.DischargeClocks", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.Devices", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.TestRanges", "Id", "dbo.Locations");
		DropForeignKey("dbo.Preferences", "Id", "dbo.Locations");
		DropForeignKey("dbo.Patients", "NurseId", "dbo.Nurses");
		DropForeignKey("dbo.Patients", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.LocationNurses", "NurseId", "dbo.Nurses");
		DropForeignKey("dbo.LocationNurses", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.LocationDiagcodes", "LocationId", "dbo.Locations");
		DropForeignKey("dbo.LocationDiagcodes", "DiagcodeId", "dbo.Diagcodes");
		DropForeignKey("dbo.DiagcodeGroupDiagcodes", "Diagcode_Id", "dbo.Diagcodes");
		DropForeignKey("dbo.DiagcodeGroupDiagcodes", "DiagcodeGroup_Id", "dbo.DiagcodeGroups");
		DropForeignKey("dbo.Locations", "ParentId", "dbo.Locations");
		DropIndex("dbo.ReagentGroupReagents", new string[1] { "Reagent_Id" });
		DropIndex("dbo.ReagentGroupReagents", new string[1] { "ReagentGroup_Id" });
		DropIndex("dbo.DiagcodeGroupDiagcodes", new string[1] { "Diagcode_Id" });
		DropIndex("dbo.DiagcodeGroupDiagcodes", new string[1] { "DiagcodeGroup_Id" });
		DropIndex("dbo.AspNetUserLogins", new string[1] { "UserId" });
		DropIndex("dbo.AspNetUserClaims", new string[1] { "UserId" });
		DropIndex("dbo.AspNetUsers", "UserNameIndex");
		DropIndex("dbo.SampleDatas", new string[1] { "PatientId" });
		DropIndex("dbo.AspNetUserRoles", new string[1] { "RoleId" });
		DropIndex("dbo.AspNetUserRoles", new string[1] { "UserId" });
		DropIndex("dbo.AspNetRoles", "RoleNameIndex");
		DropIndex("dbo.NovaSetups", new string[1] { "Id" });
		DropIndex("dbo.Reagents", new string[1] { "CreateTime" });
		DropIndex("dbo.Reagents", new string[1] { "Expiration" });
		DropIndex("dbo.Reagents", new string[1] { "LotType" });
		DropIndex("dbo.LocationReagents", new string[1] { "ReagentId" });
		DropIndex("dbo.LocationReagents", new string[1] { "LocationId" });
		DropIndex("dbo.DischargeClocks", new string[1] { "LocationId" });
		DropIndex("dbo.TestRanges", new string[1] { "UpdateTime" });
		DropIndex("dbo.TestRanges", new string[1] { "CreateTime" });
		DropIndex("dbo.TestRanges", new string[1] { "IsDeleted" });
		DropIndex("dbo.TestRanges", new string[1] { "Id" });
		DropIndex("dbo.Preferences", new string[1] { "Id" });
		DropIndex("dbo.Patients", new string[1] { "NurseId" });
		DropIndex("dbo.Patients", new string[1] { "LocationId" });
		DropIndex("dbo.Patients", new string[1] { "CreateTime" });
		DropIndex("dbo.Patients", new string[1] { "IsDelete" });
		DropIndex("dbo.Patients", new string[1] { "Source" });
		DropIndex("dbo.Patients", new string[1] { "SyncTime" });
		DropIndex("dbo.Patients", new string[1] { "SyncStatus" });
		DropIndex("dbo.Patients", new string[1] { "DischargeDate" });
		DropIndex("dbo.Patients", new string[1] { "Status" });
		DropIndex("dbo.Patients", new string[1] { "AdmissionDate" });
		DropIndex("dbo.Patients", new string[1] { "BedNo" });
		DropIndex("dbo.Patients", new string[1] { "WardNo" });
		DropIndex("dbo.Patients", new string[1] { "Birthday" });
		DropIndex("dbo.Patients", new string[1] { "Gender" });
		DropIndex("dbo.Patients", new string[1] { "Name" });
		DropIndex("dbo.Patients", new string[1] { "Account" });
		DropIndex("dbo.Patients", new string[1] { "MedicalRecord" });
		DropIndex("dbo.Patients", new string[1] { "PatientId" });
		DropIndex("dbo.Nurses", new string[1] { "CreateTime" });
		DropIndex("dbo.Nurses", new string[1] { "IsDelete" });
		DropIndex("dbo.Nurses", new string[1] { "SyncTime" });
		DropIndex("dbo.Nurses", new string[1] { "SyncStatus" });
		DropIndex("dbo.Nurses", new string[1] { "Code" });
		DropIndex("dbo.LocationNurses", new string[1] { "NurseId" });
		DropIndex("dbo.LocationNurses", new string[1] { "LocationId" });
		DropIndex("dbo.Diagcodes", new string[1] { "Code" });
		DropIndex("dbo.LocationDiagcodes", new string[1] { "DiagcodeId" });
		DropIndex("dbo.LocationDiagcodes", new string[1] { "LocationId" });
		DropIndex("dbo.Locations", new string[1] { "ParentId" });
		DropIndex("dbo.Locations", new string[1] { "Level" });
		DropIndex("dbo.Devices", new string[1] { "LocationId" });
		DropTable("dbo.ReagentGroupReagents");
		DropTable("dbo.DiagcodeGroupDiagcodes");
		DropTable("dbo.AspNetUserLogins");
		DropTable("dbo.AspNetUserClaims");
		DropTable("dbo.AspNetUsers");
		DropTable("dbo.SampleDatas");
		DropTable("dbo.AspNetUserRoles");
		DropTable("dbo.AspNetRoles");
		DropTable("dbo.NovaSetups");
		DropTable("dbo.NovaLogs");
		DropTable("dbo.ReagentGroups");
		DropTable("dbo.Reagents");
		DropTable("dbo.LocationReagents");
		DropTable("dbo.DischargeClocks");
		DropTable("dbo.TestRanges");
		DropTable("dbo.Preferences");
		DropTable("dbo.Patients");
		DropTable("dbo.Nurses");
		DropTable("dbo.LocationNurses");
		DropTable("dbo.DiagcodeGroups");
		DropTable("dbo.Diagcodes");
		DropTable("dbo.LocationDiagcodes");
		DropTable("dbo.Locations");
		DropTable("dbo.Devices");
	}
}
