using System.Collections.Generic;
using System.IO;
using System.Xml;

namespace Yzkj.Novanet.Utility;

public class NLangContainer
{
	public static readonly NLangContainer Langs;

	private static readonly Dictionary<string, string> LangDictionary;

	public string this[string key] => LangDictionary.ContainsKey(key) ? LangDictionary[key] : "";

	static NLangContainer()
	{
		LangDictionary = new Dictionary<string, string>();
		Langs = new NLangContainer();
	}

	private NLangContainer()
	{
	}

	public static void LoadLangs(string langPath)
	{
		if (string.IsNullOrEmpty(langPath) || !File.Exists(langPath))
		{
			return;
		}
		XmlDocument langDoc = new XmlDocument();
		langDoc.Load(langPath);
		XmlNodeList nodes = langDoc.SelectNodes("/trans/tran");
		foreach (XmlNode node in nodes)
		{
			string key = node.Attributes["k"].Value;
			string val = node.Attributes["v"].Value;
			LangDictionary[key] = val;
		}
	}
}
