namespace ICPMGR;

public struct PortParams
{
	public string instrumentId;

	public string protocol;

	public string portType;

	public string commProtocol;

	public int portNum;

	public int remotePort;

	public string baud;

	public string dataBits;

	public string stopBits;

	public string parity;

	public int flowControl;

	public int runMode;

	public int connectRemote;

	public string used;

	public string multiConnect;

	public byte[] ipAddress;

	public string rcvApplication;

	public string rcvFacility;

	public int portActive;

	public string remoteHostName;

	public bool do_logging;

	public ICPMGR parent;
}
