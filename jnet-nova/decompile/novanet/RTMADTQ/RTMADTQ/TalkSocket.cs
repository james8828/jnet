using System;
using System.Net.Sockets;

namespace RTMADTQ;

public class TalkSocket
{
	private const int bytesperlong = 4;

	private const int bitsperbyte = 8;

	private byte[] SIO_KEEPALIVE_VALS = new byte[12];

	private LingerOption myLingerOption;

	public Socket theSocket;

	public TalkSocket(bool lingerenable, int lingerseconds, ulong NormFreq, ulong TimeoutRetryTime)
	{
		myLingerOption = new LingerOption(lingerenable, lingerseconds);
		ulong[] input = new ulong[3];
		try
		{
			if (NormFreq == 0 || TimeoutRetryTime == 0)
			{
				input[0] = 0uL;
			}
			else
			{
				input[0] = 1uL;
			}
			input[1] = NormFreq;
			input[2] = TimeoutRetryTime;
			for (int i = 0; i < input.Length; i++)
			{
				SIO_KEEPALIVE_VALS[i * 4 + 3] = (byte)((input[i] >> 24) & 0xFF);
				SIO_KEEPALIVE_VALS[i * 4 + 2] = (byte)((input[i] >> 16) & 0xFF);
				SIO_KEEPALIVE_VALS[i * 4 + 1] = (byte)((input[i] >> 8) & 0xFF);
				SIO_KEEPALIVE_VALS[i * 4] = (byte)(input[i] & 0xFF);
			}
		}
		catch
		{
		}
	}

	public bool Init()
	{
		try
		{
			theSocket.LingerState = myLingerOption;
			byte[] result = BitConverter.GetBytes(0);
			theSocket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, optionValue: true);
			theSocket.IOControl(IOControlCode.KeepAliveValues, SIO_KEEPALIVE_VALS, result);
		}
		catch (Exception)
		{
			return false;
		}
		return true;
	}
}
