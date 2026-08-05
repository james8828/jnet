using System;
using System.Data.Odbc;
using System.Threading;
using NNClass;

namespace RTMOPL;

public class DBOperatorPrivilege
{
	public bool CreateOperatorPrivilege(DMLProtocol myProtocol, ref OperatorPrivilegeRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szParams = "";
		string szInsert = "";
		try
		{
			szInsert = "insert into dba.Operator_Privilege(";
			szParams = "(";
			szInsert += "operator_num,";
			szParams = szParams + "'" + Opr_in.m_OperatorNum + "',";
			szInsert += "inst_type,";
			szParams = szParams + "'" + Opr_in.m_insttype + "',";
			szInsert += "privilege,";
			szParams = szParams + Opr_in.m_privilege + ",";
			szInsert += "pswd,";
			szParams = ((Opr_in.m_pswd.Length <= 0) ? (szParams + "NULL,") : (szParams + "'" + Opr_in.m_pswd + "',"));
			szInsert += "last_update_date,";
			if (Opr_in.m_lastupdatedate.Year > 1800)
			{
				szParams += "datetime('";
				szParams += Opr_in.m_lastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szParams += "'),";
			}
			else
			{
				szParams += "NULL,";
			}
			szInsert += "cert_start_date,";
			if (Opr_in.m_certstartdate.Year > 1800)
			{
				szParams += "datetime('";
				szParams += Opr_in.m_certstartdate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szParams += "'),";
			}
			else
			{
				szParams += "NULL,";
			}
			szInsert += "cert_end_date,";
			if (Opr_in.m_certenddate.Year > 1800)
			{
				szParams += "datetime('";
				szParams += Opr_in.m_certenddate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szParams += "'),";
			}
			else
			{
				szParams += "NULL,";
			}
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
			if (Opr_in.m_testname.Length > 0)
			{
				szInsert += ", test_name";
				szParams = szParams + ",'" + Opr_in.m_testname + "'";
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
			Opr_in.m_while = "creating Operator_Privilege record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "creating Operator_Privilege record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorPrivilege.CreateOperatorPrivilege");
		return Opr_in.m_bOK;
	}

	private bool UpdateOperatorPrivilege(DMLProtocol myProtocol, ref OperatorPrivilegeRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szSQL = "";
		try
		{
			szSQL = "update dba.Operator_Privilege set locked_by = NULL";
			szSQL = szSQL + ", privilege = " + Opr_in.m_privilege;
			if (Opr_in.m_pswd.Length > 0)
			{
				szSQL = szSQL + ", pswd = '" + Opr_in.m_pswd + "'";
			}
			if (Opr_in.m_lastupdatedate.Year > 1800)
			{
				szSQL += ", last_update_date = datetime('";
				szSQL += Opr_in.m_lastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
			if (Opr_in.m_certstartdate.Year > 1800)
			{
				szSQL += ", cert_start_date = datetime('";
				szSQL += Opr_in.m_certstartdate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
			if (Opr_in.m_certenddate.Year > 1800)
			{
				szSQL += ", cert_end_date = datetime('";
				szSQL += Opr_in.m_certenddate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
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
			szSQL = text + " where operator_num = '" + Opr_in.m_OperatorNum + "' and inst_type = '" + Opr_in.m_insttype + "'";
			if (Opr_in.m_testname.Length > 0)
			{
				szSQL = szSQL + " and test_name = '" + Opr_in.m_testname + "'";
			}
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
			Opr_in.m_while = "updating Operator_Privilege record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "updating Operator_Privilege record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorPrivilege.UpdateOperatorPrivilege");
		return Opr_in.m_bOK;
	}

	public bool DeactivateAll(DMLProtocol myProtocol, ref OperatorPrivilegeRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szSQL = "";
		try
		{
			szSQL = "update dba.Operator_Privilege set locked_by = NULL";
			if (Opr_in.m_lastupdatedate.Year > 1800)
			{
				szSQL += ", last_update_date = datetime('";
				szSQL += Opr_in.m_lastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
			szSQL += ", is_active = 'F'";
			if (Opr_in.m_isactivelastupdatedate.Year > 1800)
			{
				szSQL += ", is_active_last_update_date = datetime('";
				szSQL += Opr_in.m_isactivelastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0");
				szSQL += "')";
			}
			szSQL = szSQL + " where operator_num = '" + Opr_in.m_OperatorNum + "' and is_active = 'T'";
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
			Opr_in.m_while = "deactivating Operator_Privilege records";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "deactivating Operator_Privilege records";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorPrivilege.DeactivateAll");
		return Opr_in.m_bOK;
	}

	public bool Read(DMLProtocol myProtocol, ref OperatorPrivilegeRec Opr_in, string operator_num, string inst_type, string test_name, ref OdbcCommand myCommand)
	{
		Opr_in.Clear();
		Opr_in.ClearStatus();
		try
		{
			string where = "operator_num = '" + operator_num + "' and inst_type = '" + inst_type + "'";
			if (test_name.Length > 0)
			{
				where = where + " and test_name = '" + test_name + "'";
			}
			myCommand.CommandText = "select operator_num, inst_type, pswd, cert_start_date, cert_end_date, privilege, last_update_date";
			if (test_name.Length > 0)
			{
				myCommand.CommandText += ", test_name";
			}
			OdbcCommand obj = myCommand;
			obj.CommandText = obj.CommandText + " from dba.operator_privilege where " + where;
			Opr_in.m_SQL = myCommand.CommandText;
			OdbcDataReader myDBReadReader = myCommand.ExecuteReader();
			if (myDBReadReader.Read())
			{
				Opr_in.m_OperatorNum = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
				Opr_in.m_insttype = (myDBReadReader.IsDBNull(1) ? "" : myDBReadReader.GetString(1));
				Opr_in.m_pswd = (myDBReadReader.IsDBNull(2) ? "" : myDBReadReader.GetString(2));
				Opr_in.m_certstartdate = (myDBReadReader.IsDBNull(3) ? DateTime.MinValue : myDBReadReader.GetDateTime(3));
				Opr_in.m_certenddate = (myDBReadReader.IsDBNull(4) ? DateTime.MinValue : myDBReadReader.GetDateTime(4));
				Opr_in.m_privilege = myDBReadReader.GetInt32(5);
				Opr_in.m_lastupdatedate = (myDBReadReader.IsDBNull(6) ? DateTime.MinValue : myDBReadReader.GetDateTime(6));
				if (test_name.Length > 0)
				{
					Opr_in.m_testname = myDBReadReader.GetString(7);
				}
				Opr_in.m_bPrivRead = true;
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
			Opr_in.m_while = "reading Operator_Privilege record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "reading Operator_Privilege record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperatorPrivilege.Read");
		return Opr_in.m_bOK;
	}

	public bool CreateorUpdate(DMLProtocol myProtocol, ref OperatorPrivilegeRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		if (!Opr_in.m_bPrivRead)
		{
			CreateOperatorPrivilege(myProtocol, ref Opr_in, ref myCommand);
		}
		else
		{
			UpdateOperatorPrivilege(myProtocol, ref Opr_in, ref myCommand);
		}
		return Opr_in.m_bOK;
	}
}
