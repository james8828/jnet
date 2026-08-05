using NLog;

namespace Yzkj.Novanet.Utility.Common;

public class ClearLogEventInfo : LogEventInfo
{
	public ClearLogEventInfo()
	{
	}

	public ClearLogEventInfo(LogLevel level, string loggerName, string message)
		: base(level, loggerName, message)
	{
	}

	public override string ToString()
	{
		return base.Message;
	}
}
