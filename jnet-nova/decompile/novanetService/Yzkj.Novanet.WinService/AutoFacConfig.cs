using Autofac;
using Yzkj.Novanet.Bussiness.Bus;
using Yzkj.Novanet.Data;

namespace Yzkj.Novanet.WinService;

public class AutoFacConfig
{
	public static ContainerBuilder builder;

	public static IContainer container;

	static AutoFacConfig()
	{
		builder = new ContainerBuilder();
		container = null;
		RegisterTypes();
	}

	public static void RegisterTypes()
	{
		builder.RegisterType<NovaDbContext>();
		builder.RegisterType<NovaSyncBus>();
		container = builder.Build();
	}
}
