namespace RTMADTQ;

public class HL7Parse
{
	public string remainder;

	public uint curfield;

	public int fieldlen;

	public uint curcomponent;

	public string FieldDelim;

	public string ComponentDelim;

	public string SubComponentDelim;

	public HL7Parse()
	{
		remainder = "";
		curfield = 0u;
		fieldlen = 0;
		curcomponent = 0u;
		FieldDelim = "|";
		ComponentDelim = "^";
		SubComponentDelim = "&";
	}
}
