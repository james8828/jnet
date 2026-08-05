using System;
using System.Data.Odbc;
using System.Threading;
using NNClass;

namespace RTMOPL;

public class DBMethod
{
	public bool Create(DMLProtocol myProtocol, ref MethodRec Method_in, ref OdbcCommand myCommand)
	{
		MethodRec TmpMethod = new MethodRec();
		Method_in.ClearStatus();
		string szParams = "";
		string szInsert = "";
		Read(myProtocol, ref TmpMethod, Method_in.m_OperatorNum, Method_in.m_insttype, Method_in.m_methodcd, ref myCommand);
		if (TmpMethod.m_methodcd.Length == 0)
		{
			try
			{
				szInsert = "insert into dba.Methods(";
				szParams = "(";
				szInsert += "operator_num,";
				szParams = szParams + "'" + Method_in.m_OperatorNum + "',";
				szInsert += "inst_type,";
				szParams = szParams + "'" + Method_in.m_insttype + "',";
				szInsert += "method_cd";
				szParams = szParams + "'" + Method_in.m_methodcd + "'";
				szParams += ")";
				szInsert += ") values";
				myCommand.CommandText = szInsert + szParams;
				Method_in.m_SQL = myCommand.CommandText;
				myCommand.ExecuteNonQuery();
			}
			catch (ThreadAbortException ex)
			{
				throw new Exception(ex.Message, ex.InnerException);
			}
			catch (OdbcException sA_e)
			{
				Method_in.m_bOK = false;
				Method_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
				Method_in.m_while = "creating Method record";
				Method_in.m_SA_e = sA_e;
			}
			catch (Exception e)
			{
				Method_in.m_bOK = false;
				Method_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
				Method_in.m_while = "creating Method record";
				Method_in.m_e = e;
			}
			myProtocol.m_NNBase.LogActionAndError(Method_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBMethod.Create");
		}
		return Method_in.m_bOK;
	}

	public bool Read(DMLProtocol myProtocol, ref MethodRec Method_in, string operator_num, string inst_type, string methodcd, ref OdbcCommand myCommand)
	{
		Method_in.Clear();
		Method_in.ClearStatus();
		try
		{
			string where = "operator_num = '" + operator_num + "' and inst_type = '" + inst_type + "' and method_cd = '" + methodcd + "'";
			myCommand.CommandText = "select operator_num, inst_type, method_cd from dba.methods where " + where;
			Method_in.m_SQL = myCommand.CommandText;
			OdbcDataReader myDBReadReader = myCommand.ExecuteReader();
			if (myDBReadReader.Read())
			{
				Method_in.m_OperatorNum = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
				Method_in.m_insttype = (myDBReadReader.IsDBNull(1) ? "" : myDBReadReader.GetString(1));
				Method_in.m_methodcd = (myDBReadReader.IsDBNull(2) ? "" : myDBReadReader.GetString(2));
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException ex)
		{
			throw new Exception(ex.Message, ex.InnerException);
		}
		catch (OdbcException sA_e)
		{
			Method_in.m_bOK = false;
			Method_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
			Method_in.m_while = "reading Methods record";
			Method_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Method_in.m_bOK = false;
			Method_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Method_in.m_while = "reading Methods record";
			Method_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Method_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBMethod.Read");
		return Method_in.m_bOK;
	}
}
