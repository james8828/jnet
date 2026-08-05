using System;
using NLog;

namespace Yzkj.Novanet.WinService;

public class LoggerWrap
{
	private Logger Logger;

	public LoggerWrap(Logger _logger)
	{
		Logger = _logger;
	}

	public void Info(string userName, string operatorName, string userhost, string data)
	{
		string message = "设备：" + userName + "，操作：" + operatorName + ".";
		LogEventInfo logEventInfo = new LogEventInfo(LogLevel.Info, "NOVA_LOGGER", message);
		logEventInfo.Properties["source"] = "ws";
		logEventInfo.Properties["userhost"] = userhost;
		logEventInfo.Properties["username"] = userName;
		logEventInfo.Properties["actiondata"] = data;
		Logger.Info(logEventInfo);
	}

	public void Info(string message)
	{
		Logger.Info(message);
	}

	public void Debug(string message)
	{
		Logger.Debug(message);
	}

	public void Error(Exception e, string message)
	{
		Logger.Error(e, message);
	}
}
