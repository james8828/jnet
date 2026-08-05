using System;
using System.Data.Odbc;
using System.Threading;
using NNClass;

namespace RTMOPL;

public class DBOperator
{
	private bool CreateOperator(DMLProtocol myProtocol, ref OperatorRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szParams = "";
		string szInsert = "";
		try
		{
			szInsert = "insert into dba.Operators(";
			szParams = "(";
			szInsert += "Operator_num,";
			szParams = szParams + "'" + (Opr_in.m_OperatorNum = Guid.NewGuid().ToString("N")) + "',";
			szInsert += "Supervisor_num,";
			szParams = ((Opr_in.m_SupervisorNum.Length <= 0) ? (szParams + "null,") : (szParams + "'" + Opr_in.m_SupervisorNum + "',"));
			szInsert += "Operator_ID,";
			szParams = szParams + "'" + Opr_in.m_OperatorID.Replace("'", "''") + "',";
			szInsert += "Is_Supervisor,";
			szParams = ((Opr_in.m_IsSupervisor.Length <= 0) ? (szParams + "null,") : (szParams + "'" + Opr_in.m_IsSupervisor + "',"));
			szInsert += "last_update_date,";
			szParams = ((Opr_in.m_lastupdatedate.Year <= 1800) ? (szParams + "NULL,") : (szParams + "date('" + Opr_in.m_lastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0") + "'),"));
			szInsert += "add_date";
			szParams = ((Opr_in.m_adddate.Year <= 1800) ? (szParams + "NULL") : (szParams + "date('" + Opr_in.m_adddate.ToString("yyyy-MM-dd HH:mm:ss.0") + "')"));
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
			Opr_in.m_while = "creating Operator record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "creating Operator record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.CreateOperator");
		return Opr_in.m_bOK;
	}

	private bool CreateContactInfo(DMLProtocol myProtocol, ref OperatorRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szParams = "";
		string szInsert = "";
		try
		{
			szInsert = "insert into dba.contact_info(";
			szParams = "(";
			szInsert += "ref_table,";
			szParams += "'OPERATORS',";
			szInsert += "contact_num,";
			szParams = szParams + "'" + Opr_in.m_OperatorNum + "',";
			szInsert += "Last_Name,";
			szParams = ((Opr_in.m_Lastname.Length <= 0) ? (szParams + "NULL,") : (szParams + "'" + Opr_in.m_Lastname.Replace("'", "''") + "',"));
			szInsert += "First_Name,";
			szParams = ((Opr_in.m_Firstname.Length <= 0) ? (szParams + "NULL,") : (szParams + "'" + Opr_in.m_Firstname.Replace("'", "''") + "',"));
			szInsert += "Initials,";
			szParams = ((Opr_in.m_Initials.Length <= 0) ? (szParams + "NULL,") : (szParams + "'" + Opr_in.m_Initials.Replace("'", "''") + "',"));
			szInsert += "Email";
			szParams = ((Opr_in.m_email.Length <= 0) ? (szParams + "null") : (szParams + "'" + Opr_in.m_email + "'"));
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
			Opr_in.m_while = "creating Contact_Info record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "creating Contact_Info record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.CreateContactInfo");
		return Opr_in.m_bOK;
	}

	public bool Read(DMLProtocol myProtocol, ref OperatorRec Opr_in, string where, ref OdbcCommand myCommand, bool toUpdate)
	{
		Opr_in.Clear();
		Opr_in.ClearStatus();
		try
		{
			myCommand.CommandText = "select operator_num, supervisor_num, operator_id, is_supervisor, last_update_date from dba.operators where " + where;
			Opr_in.m_SQL = myCommand.CommandText;
			OdbcDataReader myDBReadReader = myCommand.ExecuteReader();
			if (myDBReadReader.Read())
			{
				Opr_in.m_OperatorNum = (myDBReadReader.IsDBNull(0) ? "" : myDBReadReader.GetString(0));
				Opr_in.m_SupervisorNum = (myDBReadReader.IsDBNull(1) ? "" : myDBReadReader.GetString(1));
				Opr_in.m_OperatorID = (myDBReadReader.IsDBNull(2) ? "" : myDBReadReader.GetString(2));
				Opr_in.m_IsSupervisor = (myDBReadReader.IsDBNull(3) ? "" : myDBReadReader.GetString(3));
				Opr_in.m_lastupdatedate = myDBReadReader.GetDateTime(4);
				Opr_in.m_bOperRead = true;
			}
			else
			{
				Opr_in.m_bOK = false;
				Opr_in.m_errortype = Convert.ToInt32(errortypes.NoMatch);
				Opr_in.m_description = "- no matching operator record found";
				Opr_in.m_while = "reading Operator record";
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
			Opr_in.m_while = "reading Operator record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "reading Operator record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.Read");
		if (Opr_in.m_bOK && toUpdate)
		{
			try
			{
				myCommand.CommandText = "select contact_num, last_name, first_name, initials, email from dba.contact_info where contact_num = '" + Opr_in.m_OperatorNum + "'";
				Opr_in.m_SQL = myCommand.CommandText;
				OdbcDataReader myDBReadReader2 = myCommand.ExecuteReader();
				if (myDBReadReader2.Read())
				{
					Opr_in.m_ContactNum = (myDBReadReader2.IsDBNull(0) ? "" : myDBReadReader2.GetString(0));
					Opr_in.m_Lastname = (myDBReadReader2.IsDBNull(1) ? "" : myDBReadReader2.GetString(1));
					Opr_in.m_Firstname = (myDBReadReader2.IsDBNull(2) ? "" : myDBReadReader2.GetString(2));
					Opr_in.m_Initials = (myDBReadReader2.IsDBNull(3) ? "" : myDBReadReader2.GetString(3));
					Opr_in.m_email = (myDBReadReader2.IsDBNull(4) ? "" : myDBReadReader2.GetString(4));
					Opr_in.m_bContRead = true;
				}
				else
				{
					Opr_in.m_bOK = false;
					Opr_in.m_errortype = Convert.ToInt32(errortypes.NoMatch);
					Opr_in.m_description = "- no matching contact_info record found";
					Opr_in.m_while = "reading contact_info record";
				}
				myDBReadReader2.Close();
			}
			catch (ThreadAbortException ex2)
			{
				throw new Exception(ex2.Message, ex2.InnerException);
			}
			catch (OdbcException sA_e2)
			{
				Opr_in.m_bOK = false;
				Opr_in.m_errortype = Convert.ToInt32(errortypes.SA_Exception);
				Opr_in.m_while = "reading Conact_Info record";
				Opr_in.m_SA_e = sA_e2;
			}
			catch (Exception e2)
			{
				Opr_in.m_bOK = false;
				Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
				Opr_in.m_while = "reading Contact_Info record";
				Opr_in.m_e = e2;
			}
			myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.Read");
		}
		return Opr_in.m_bOK;
	}

	private bool UpdateOperator(DMLProtocol myProtocol, ref OperatorRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szSQL = "";
		try
		{
			szSQL = "update dba.Operators set locked_by = NULL";
			if (Opr_in.m_IsSupervisor.Length > 0)
			{
				szSQL = szSQL + ", Is_Supervisor = '" + Opr_in.m_IsSupervisor + "'";
			}
			if (Opr_in.m_lastupdatedate.Year > 1800)
			{
				szSQL = szSQL + ", last_update_date = date('" + Opr_in.m_lastupdatedate.ToString("yyyy-MM-dd HH:mm:ss.0") + "')";
			}
			szSQL = szSQL + " where operator_num = '" + Opr_in.m_OperatorNum + "'";
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
			Opr_in.m_while = "updating Operator record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "updating Operator record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.UpdateOperator");
		return Opr_in.m_bOK;
	}

	private bool UpdateContactInfo(DMLProtocol myProtocol, ref OperatorRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		string szSQL = "";
		try
		{
			szSQL = "update dba.Contact_Info set locked_by = NULL";
			if (Opr_in.m_Lastname.Length > 0)
			{
				szSQL = szSQL + ", Last_Name = '" + Opr_in.m_Lastname.Replace("'", "''") + "'";
			}
			if (Opr_in.m_Firstname.Length > 0)
			{
				szSQL = szSQL + ", First_Name = '" + Opr_in.m_Firstname.Replace("'", "''") + "'";
			}
			if (Opr_in.m_Initials.Length > 0)
			{
				szSQL = szSQL + ", Initials = '" + Opr_in.m_Initials.Replace("'", "''") + "'";
			}
			if (Opr_in.m_email.Length > 0)
			{
				szSQL = szSQL + ", Email = '" + Opr_in.m_email + "'";
			}
			szSQL = szSQL + " where contact_num = '" + Opr_in.m_ContactNum + "'";
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
			Opr_in.m_while = "updating contact_info record";
			Opr_in.m_SA_e = sA_e;
		}
		catch (Exception e)
		{
			Opr_in.m_bOK = false;
			Opr_in.m_errortype = Convert.ToInt32(errortypes.Other_Exception);
			Opr_in.m_while = "updating contact_info record";
			Opr_in.m_e = e;
		}
		myProtocol.m_NNBase.LogActionAndError(Opr_in, Convert.ToInt32(errortypes.Critical), myProtocol.ShutDown, "DBOperator.UpdateContactInfo");
		return Opr_in.m_bOK;
	}

	public bool CreateorUpdate(DMLProtocol myProtocol, ref OperatorRec Opr_in, ref OdbcCommand myCommand)
	{
		Opr_in.ClearStatus();
		if (!Opr_in.m_bOperRead)
		{
			CreateOperator(myProtocol, ref Opr_in, ref myCommand);
		}
		else
		{
			UpdateOperator(myProtocol, ref Opr_in, ref myCommand);
		}
		if (Opr_in.m_bOK)
		{
			if (!Opr_in.m_bContRead)
			{
				CreateContactInfo(myProtocol, ref Opr_in, ref myCommand);
			}
			else
			{
				UpdateContactInfo(myProtocol, ref Opr_in, ref myCommand);
			}
		}
		return Opr_in.m_bOK;
	}
}
