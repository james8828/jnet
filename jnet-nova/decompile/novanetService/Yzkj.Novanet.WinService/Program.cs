using System;
using System.Configuration;
using System.Net;
using System.Net.Sockets;
using System.ServiceProcess;
using Topshelf;
using Topshelf.HostConfigurators;
using Topshelf.Runtime;
using Topshelf.ServiceConfigurators;

namespace Yzkj.Novanet.WinService;

internal class Program
{
	private static void Main(string[] args)
	{
		string text = ConfigurationManager.AppSettings["host"];
		string text2 = ConfigurationManager.AppSettings["port"];
		string host = (string.IsNullOrEmpty(text) ? GetIpAddress() : text);
		int port = ((!string.IsNullOrEmpty(text2)) ? int.Parse(text2) : 57380);
		HostFactory.Run(delegate(HostConfigurator x)
		{
			x.Service(delegate(ServiceConfigurator<NovaService> s)
			{
				s.ConstructUsing((HostSettings name) => new NovaService());
				s.WhenStarted(delegate(NovaService ns)
				{
					ns.Start(host, port);
				});
				s.WhenPaused(delegate(NovaService ns)
				{
					ns.Pause();
				});
				s.WhenContinued(delegate(NovaService ns)
				{
					ns.Resume();
				});
				s.WhenStopped(delegate(NovaService ns)
				{
					ns.Stop();
				});
			});
			x.RunAsLocalSystem();
			x.StartAutomaticallyDelayed();
			x.AfterInstall((Action)delegate
			{
				using ServiceController serviceController = new ServiceController("Nova Protocol Service");
				serviceController.Start();
			});
			x.SetDescription("血糖数据协议解析服务");
			x.SetDisplayName("血糖数据协议解析服务");
			x.SetServiceName("Nova Protocol Service");
		});
	}

	private static string GetIpAddress()
	{
		string result = "";
		IPAddress[] addressList = Dns.GetHostEntry(Dns.GetHostName()).AddressList;
		foreach (IPAddress iPAddress in addressList)
		{
			if (iPAddress.AddressFamily == AddressFamily.InterNetwork)
			{
				result = iPAddress.ToString();
			}
		}
		return result;
	}
}
