using System;
using System.Data.Odbc;
using System.Threading;
using NNClass;

namespace RTMOPL;

public class DBOperatorToUnit
{
	private bool CreateOperatorToUnit(DMLProtocol myProtocol, ref OperatorToUnitRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szParams = "";
		string szInsert = "";
		try
		{
			szInsert = "insert into dba.Operator_To_Unit(";
			szParams = "(";
			szInsert += "operator_num,";
			szParams = szParams + "'" + Opr_in.m_OperatorNum + "',";
			szInsert += "loc_num,";
			szParams = szParams + "'" + Opr_in.m_locnum + "',";
			szInsert += "is_active,";
			szParams = ((Opr_in.m_isactive.Length <= 0) ? (szParams + "NULL,") : (szParams + "'" + Opr_in.m_isactive + "',"));
			szInsert += "is_active_last_update_date";
			if (Opr_in.m_isactivelastupdatedate.Year > 1800)
			{
				szParams += "datetime('";
				szParams += Opr_in.m_isactivelastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szParams += "')";
			}
			else
			{
				szParams += "NULL";
			}
			szParams += ")";
			szInsert += ") values";
			myCommand.CommandText = szInsert + szParams;
			Opr_in.m_SQL = myCommand.CommandText;
			myCommand.ExecuteNonQuery();
		}
		catch (ThreadAbortException ex)
		{
			throw new Exception(ex.Message, ex.InnerException);
		}
		catch (OdbcException sA_e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
			Opr_in.m_while = "creating Operator_To_Unit record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "creating Operator_To_Unit record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorToUnit.CreateOperatorToUnit");
		return Opr_in.m_bOK;
	}

	public bool UpdateOperatorToUnit(DMLProtocol myProtocol, ref OperatorToUnitRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szSQL = "";
		try
		{
			szSQL = "update dba.Operator_To_Unit set locked_by = NULL";
			if (Opr_in.m_isactive.Length > 0)
			{
				szSQL = szSQL + ", is_active = '" + Opr_in.m_isactive + "'";
			}
			if (Opr_in.m_isactivelastupdatedate.Year > 1800)
			{
				szSQL += ", is_active_last_update_date = datetime('";
				szSQL += Opr_in.m_isactivelastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
			string text = szSQL;
			szSQL = text + " where operator_num = '" + Opr_in.m_OperatorNum + "' and loc_num = '" + Opr_in.m_locnum + "'";
			myCommand.CommandText = szSQL;
			Opr_in.m_SQL = myCommand.CommandText;
			myCommand.ExecuteNonQuery();
		}
		catch (ThreadAbortException ex)
		{
			throw new Exception(ex.Message, ex.InnerException);
		}
		catch (OdbcException sA_e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
			Opr_in.m_while = "updating Operator_To_Unit record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "updating Operator_To_Unit record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorToUnit.UpdateOperatorToUnit");
		return Opr_in.m_bOK;
	}

	public int GetOperatorToUnitCount(DMLProtocol myProtocol, ref OperatorToUnitRec Opr_in, string operator_num, ref OdbcCommand myCommand)
	{
		int Count = 0;
		Opr_in.Clear();
		Opr_in.ClearStatus();
		try
		{
			string where = "operator_num = '" + operator_num + "' and is_active = 'T'";
			myCommand.CommandText = "select count(*) from dba.operator_to_unit where " + where;
			Opr_in.m_SQL = myCommand.CommandText;
			Count = (int)myCommand.ExecuteScalar();
		}
		catch (ThreadAbortException ex)
		{
			throw new Exception(ex.Message, ex.InnerException);
		}
		catch (OdbcException sA_e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
			Opr_in.m_while = "reading Operator_To_Unit record count";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "reading Operator_To_Unit record count";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorToUnit.GetOperatorToUnitCount");
		return Count;
	}

	public bool Read(DMLProtocol myProtocol, ref OperatorToUnitRec Opr_in, string operator_num, string loc_num, ref OdbcCommand myCommand)
	{
		Opr_in.Clear();
		Opr_in.ClearStatus();
		try
		{
			string where = "operator_num = '" + operator_num + "' and loc_num = '" + loc_num + "'";
			myCommand.CommandText = "select loc_num, is_active, is_active_last_update_date from dba.operator_to_unit where " + where;
			Opr_in.m_SQL = myCommand.CommandText;
			OdbcDataReader myDBReadReader = myCommand.ExecuteReader();
			if (myDBReadReader.Read())
			{
				Opr_in.m_locnum = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
				Opr_in.m_isactive = (myDBReadReader.IsDBNull(1) ? "" : myDBReadReader.GetString(1));
				Opr_in.m_isactivelastupdatedate = myDBReadReader.GetDateTime(2);
				Opr_in.m_bUnitRead = true;
			}
			myDBReadReader.Close();
		}
		catch (ThreadAbortException ex)
		{
			throw new Exception(ex.Message, ex.InnerException);
		}
		catch (OdbcException sA_e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
			Opr_in.m_while = "reading Operator_To_Unit record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "reading Operator_To_Unit record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorToUnit.Read");
		return Opr_in.m_bOK;
	}

	public bool CreateorUpdate(DMLProtocol myProtocol, ref OperatorToUnitRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		if (!Opr_in.m_bUnitRead)
		{
			CreateOperatorToUnit(myProtocol, ref Opr_in, ref myCommand);
		}
		else
		{
			UpdateOperatorToUnit(myProtocol, ref Opr_in, ref myCommand);
		}
		return Opr_in.m_bOK;
	}
}
