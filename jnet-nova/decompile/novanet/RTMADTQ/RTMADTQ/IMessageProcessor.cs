using System.ServiceModel;
using System.ServiceModel.MsmqIntegration;

namespace RTMADTQ;

[ServiceContract(Namespace = "http://RTMADTQ")]
public interface IMessageProcessor
{
	[OperationContract(IsOneWay = true, Action = "*")]
	void SubmitStringMessage(MsmqMessage<string> msg);
}
