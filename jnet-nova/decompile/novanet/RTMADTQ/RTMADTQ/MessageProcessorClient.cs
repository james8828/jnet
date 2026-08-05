using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.MsmqIntegration;

namespace RTMADTQ;

public class MessageProcessorClient : ClientBase<IMessageProcessor>, IMessageProcessor
{
	public MessageProcessorClient()
	{
	}

	public MessageProcessorClient(string configurationName)
		: base(configurationName)
	{
	}

	public MessageProcessorClient(Binding binding, EndpointAddress address)
		: base(binding, address)
	{
	}

	public void SubmitStringMessage(MsmqMessage<string> msg)
	{
		base.Channel.SubmitStringMessage(msg);
	}
}
