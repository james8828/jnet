namespace Yzkj.Novanet.Bussiness.Model;

public class ResultsSetupModel
{
	public int Id { get; set; }

	public bool CommentsFreeTextChartableEnable { get; set; }

	public bool CommentsFreeTextFlaggedEnable { get; set; }

	public bool ObsReviewNoLogin { get; set; }

	public bool LinObsValueDisplay { get; set; }

	public bool CommentsFreeTextEnable { get; set; }

	public bool ObsRejectEnable { get; set; }

	public bool QcObsValueDisplay { get; set; }

	public bool QcObsFailCommentReqCd { get; set; }

	public bool LinObsFailCommentReqCd { get; set; }

	public bool ObsRejectResultCommentReq { get; set; }

	public bool ObsCriticalRangeCommentReq { get; set; }

	public bool ObsTechnicalRangeCommentReq { get; set; }

	public bool QcObsPassCommentReqCd { get; set; }

	public bool LinObsPassCommentReqCd { get; set; }

	public bool ObsNormalRangeCommentReq { get; set; }

	public bool ObsAbnormalRangeCommentReq { get; set; }
}
