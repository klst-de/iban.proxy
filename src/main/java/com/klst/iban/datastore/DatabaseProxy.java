package com.klst.iban.datastore;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.klst.iban.BankId;
import com.klst.iban.InternationalBankAccountNumber;
import com.klst.iban.Result.BankData;

public class DatabaseProxy {

	private static final Logger LOG = Logger.getLogger(DatabaseProxy.class.getName());
	
/* db-URL 
 
jdbc:subprotocol:subname // url Aufbau Allgemein
subprotocol ::= postgresql
subname ::= //localhost:5432/ad393
subname ::= //[user[:password]@][netloc][:port][/dbname][?param1=value1&...]
subname ::= //{host}[:{port}]/[{database}]

 */
	public static final String JDBC = "jdbc:";
	@Deprecated
	public static final String HSQLDB_SUBPROTOCOL = "hsqldb:"; // wird nicht weiter verfolgt ==> H2
	public static final String H2_SUBPROTOCOL = "h2:";
	public static final String POSTGRESQL_SUBPROTOCOL = "postgresql:";
	// Datastore : jdbc:subprotocol:subname
	/*

	org.h2.jdbc.JdbcSQLNonTransientConnectionException: 
	Ein implizit relativer Pfad zum Arbeitsverzeichnis ist nicht erlaubt in der Datenbank URL "jdbc:h2:data/H2". 
	Bitte absolute Pfade, ~/name, ./name, oder baseDir verwenden.
	
	A file path that is implicitly relative to the current working directory is not allowed in the database URL "jdbc:h2:data/H2". 
	Use an absolute path, ~/name, ./name, or the baseDir setting instead. [90011-200]

	 */
	public static final String H2_DATASTORE = JDBC+H2_SUBPROTOCOL+"~/data/H2/bankdata";
	public static final String POSTGRESQL_DATASTORE = JDBC+POSTGRESQL_SUBPROTOCOL+"//localhost:5432/ad393";
		

	SqlInstance sql;
	DatabaseMetaData dbmd;
	
	public DatabaseProxy(SqlInstance sql) {
		if(sql.connection!=null) try {
			this.sql = sql;
			dbmd = sql.connection.getMetaData();
			LOG.info(dbmd.getDatabaseProductName()+" "+dbmd.getDatabaseProductVersion() + 
					" - "+dbmd.getDatabaseMajorVersion()+"."+dbmd.getDatabaseMinorVersion() +
					" , SchemaTerm="+ dbmd.getSchemaTerm() + " , getCatalogTerm="+ dbmd.getCatalogTerm()
					);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	// default datastore
	public DatabaseProxy() {
		this(new SqlInstance(DatabaseProxy.H2_DATASTORE, "SA", "", SqlInstance.H2_DRIVER));
	}
	
/*
	(postgres) The schema columns are: 
	1.TABLE_SCHEM String => schema name 
	2.TABLE_CATALOG String => catalog name (may be null) 
				
 */
	public void logSchemas() {
		try {
			ResultSet rs = dbmd.getSchemas();
			ResultSetMetaData rsMeta = rs.getMetaData();
			Map<String,Class<?>> map = new HashMap<String,Class<?>>();
			for(int column=1; column<=rsMeta.getColumnCount(); column++) {
				LOG.info(""+column + ": name="+rsMeta.getColumnName(column) + ", label="+rsMeta.getColumnLabel(column) +
						// BIT             =  -7 ,  ...
						", Type="+rsMeta.getColumnType(column) + ", TypeName="+rsMeta.getColumnTypeName(column) +
						", ClassName="+rsMeta.getColumnClassName(column) + ", DisplaySize="+rsMeta.getColumnDisplaySize(column) +
						", SchemaName="+rsMeta.getSchemaName(column) + ", TableName="+rsMeta.getTableName(column));
				map.put(rsMeta.getColumnTypeName(column), Class.forName(rsMeta.getColumnClassName(column)));
			}
			int row = 0;
			while(rs.next()) {
				row++;
				rs.getRow(); // Retrieves the current row number. The first row is number 1, thesecond number 2, and so on. 
//				LOG.info(rs.getString("TABLE_SCHEM2") + ";" +rs.getString("TABLE_CATALOG"));
//				LOG.info(rs.getString(1) + ";" +rs.getString(2));
				for(int column=1; column<=rsMeta.getColumnCount(); column++) {
					Object columnObj = null;
//					rs.getObject(column, map); // Dieses Feature wird nicht unterstützt: "map" , daher:
					switch(rsMeta.getColumnType(column)) { 
					case Types.VARCHAR:
						columnObj = rs.getString(column);
			            break; 
					case Types.BOOLEAN:
						columnObj = rs.getBoolean(column);
			            break; 
					default:
						LOG.warning("col:"+column + " : nicht IMPLEMENTIERT type " +rsMeta.getColumnType(column));
					}
					LOG.info("row "+row + " " + rsMeta.getColumnName(column)+"\tcol:"+column + " : " +columnObj);
				}
			}
			rs.close();
		} catch (SQLException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void logRs(ResultSet rs) {
		try {
			ResultSetMetaData rsMeta = rs.getMetaData();
			for(int column=1; column<=rsMeta.getColumnCount(); column++) {
				LOG.info(""+column + ": name="+rsMeta.getColumnName(column) + ", label="+rsMeta.getColumnLabel(column) +
						// BIT             =  -7 ,  ...
						", Type="+rsMeta.getColumnType(column) + ", TypeName="+rsMeta.getColumnTypeName(column) +
						", ClassName="+rsMeta.getColumnClassName(column) + ", DisplaySize="+rsMeta.getColumnDisplaySize(column) +
						", SchemaName="+rsMeta.getSchemaName(column) + ", TableName="+rsMeta.getTableName(column));
			}
			int row = 0;
			while(rs.next()) {
				row++;
				rs.getRow(); // Retrieves the current row number. The first row is number 1, thesecond number 2, and so on. 
				for(int column=1; column<=rsMeta.getColumnCount(); column++) {
					Object columnObj = null;
					switch(rsMeta.getColumnType(column)) { 
					case Types.TINYINT:
						columnObj = rs.getByte(column);
			            break;             
					case Types.CHAR:
					case Types.VARCHAR:
						columnObj = rs.getString(column);
			            break; 
					case Types.NUMERIC:
					case Types.DECIMAL:
						columnObj = rs.getBigDecimal(column);
			            break; 
					case Types.INTEGER:
					case Types.SMALLINT:
						columnObj = rs.getInt(column);
			            break;             
					case Types.BOOLEAN:
						columnObj = rs.getBoolean(column);
			            break; 
					default:
						LOG.warning("col:"+column + " : nicht IMPLEMENTIERT type " +rsMeta.getColumnType(column));
					}
					LOG.info("row "+row + " " + rsMeta.getColumnName(column)+"\tcol:"+column + " : " +columnObj);
				}
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void logCountry() {
		logCountry(null);
	}
	public void logCountry(String alpha2) {
		String tablename = "country_code";
		try {
//			Map<String,Class<?>> map = sql.connection.getTypeMap();
//			LOG.info("getTypeMap:" + map); // wird nicht unterstützt! ==null
			Statement stmt = sql.connection.createStatement();
			String query = "select * from "+tablename;
			// true if the first result is a ResultSetobject; false if it is an update count or there are no results
			stmt.execute(alpha2==null ? query : query + " where alpha2='"+alpha2+"'");
			ResultSet rs = stmt.getResultSet();
			logRs(rs);
		} catch (SQLException e) {
			LOG.warning("ErrorCode:"+e.getErrorCode() + " SQLState:"+e.getSQLState() + " " + e.getMessage());
			if("42P01".equals(e.getSQLState())) {
				// (PostgreSQL) SQLState: 42P01 : Relation »xxx« existiert nicht
			} else {
				e.printStackTrace();
			}
		}
	}

	public BankData XXgetBankdata(String alpha2, String bank_id, Object branch_code) {
		BankData bankData = null;
		return bankData;
	}
	public BankData getBankdata(InternationalBankAccountNumber iban) {
		BankData bankData = null;
		BankData ibanBankData = iban.getBankData();
		LOG.info("TEST:"+ibanBankData);
		String alpha2 = iban.getCountryCode();
		String bank_id = iban.getBankData().getBankIdentifier();
		Object branch_code = iban.getBankData().getBranchCode();
		
		String tablename = "bankdata";
		try {
			Statement stmt = sql.connection.createStatement();
			StringBuffer query = new StringBuffer("select * from ").append(tablename);
			query.append(" where country_code='").append(alpha2).append("'");
			if(bank_id==null) {
				LOG.severe("bank_id is null");
			} else if(BankId.getInstance().isALPHA_BANKCODE(alpha2)
					||BankId.getInstance().isSORTCODE_LIKE(alpha2) ) {
				query.append(" and bank_id='").append(bank_id).append("'");
			} else {
				query.append(" and bank_id='").append(Long.parseLong(bank_id)).append("'");
			}
			if(branch_code==null) {
				// nix weiter: query sollte 1 exemplat liefern
			} else if(BankId.getInstance().isBANKCODE_WITH_ZERO_BRANCHCODE(alpha2)){
				//query.append(" and branch_code='").append(branch_code.toString()).append("'");
			} else if(BankId.getInstance().isBANKCODE_AND_BRANCHCODE_NUMERIC(alpha2)
					||BankId.getInstance().isSORTCODE_LIKE(alpha2) ) {
				query.append(" and branch_code='").append(branch_code.toString()).append("'");
			}
			LOG.info("query:"+query);
			stmt.execute(query.toString());
			ResultSet rs = stmt.getResultSet();
			bankData = getBankdata(ibanBankData, rs);
		} catch (SQLException e) {
			LOG.warning(e.getSQLState() + " " + e.getMessage());
			if("42P01".equals(e.getSQLState())) {
				// (PostgreSQL) SQLState: 42P01 : Relation »xxx« existiert nicht
			} else {
				e.printStackTrace();
			}
		}
		LOG.info("TEST bankData:"+bankData);
		return bankData;
	}
	
	private BankData getBankdata(BankData bd, ResultSet rs) {
		try {
			ResultSetMetaData rsMeta = rs.getMetaData();
//			for(int column=1; column<=rsMeta.getColumnCount(); column++) {
//				LOG.info(""+column + ": name="+rsMeta.getColumnName(column) + ", label="+rsMeta.getColumnLabel(column) +
//						// BIT             =  -7 ,  ...
//						", Type="+rsMeta.getColumnType(column) + ", TypeName="+rsMeta.getColumnTypeName(column) +
//						", ClassName="+rsMeta.getColumnClassName(column) + ", DisplaySize="+rsMeta.getColumnDisplaySize(column) +
//						", SchemaName="+rsMeta.getSchemaName(column) + ", TableName="+rsMeta.getTableName(column));
//			}
			int row = 0;
			while(rs.next()) {
				row++;
				rs.getRow(); // Retrieves the current row number. The first row is number 1, thesecond number 2, and so on. 
				for(int column=1; column<=rsMeta.getColumnCount(); column++) {
					Object columnValue = null;
					switch(rsMeta.getColumnType(column)) { 
					case Types.TINYINT:
						columnValue = rs.getByte(column);
			            break;             
					case Types.CHAR:
					case Types.VARCHAR:
						columnValue = rs.getString(column);
			            break; 
					case Types.NUMERIC:
					case Types.DECIMAL:
						columnValue = rs.getBigDecimal(column);
			            break; 
					case Types.INTEGER: 
					case Types.SMALLINT:
						columnValue = rs.getInt(column);
			            break;             
					case Types.BOOLEAN:
						columnValue = rs.getBoolean(column);
			            break; 
					default:
						LOG.warning("col:"+column + " : nicht IMPLEMENTIERT type " +rsMeta.getColumnType(column));
					}
					LOG.info("row "+row + " " + rsMeta.getColumnName(column)+"\tcol:"+column + " : " +columnValue);
					if(columnValue==null) {
						// kein set
//					} else if("BANK_CODE".equals(rsMeta.getColumnName(column).toUpperCase())) {
//						bd.setBankCode((int)columnValue);
//					} else if("ID".equals(rsMeta.getColumnName(column).toUpperCase())) {
//						bd.setXXX((BigDecimal)columnValue);
					} else if("SUPPORT_CODES".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setBankSupports(Byte.parseByte(columnValue.toString()));
					} else if("COUNTRY_CODE".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setCountryIso((String)columnValue);
					} else if("SWIFT_CODE".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setBic((String)columnValue);
//					} else if("BANK_ID".equals(rsMeta.getColumnName(column).toUpperCase())) {
//						bd.setBankIdentifier((String)columnValue);
					} else if("BANK".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setBank((String)columnValue);
					} else if("BRANCH".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setBranch(columnValue);
					} else if("ZIP".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setZipString((String)columnValue);
					} else if("CITY".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setCity((String)columnValue);
					} else if("ADDRESS".equals(rsMeta.getColumnName(column).toUpperCase())) {
						bd.setAddress(columnValue);
					}
				}
			}
			rs.close();
			if(row==0) return null;
			if(row>1) return null; // TODO msg nicht eindeutig
			return bd;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void logBankdata(String alpha2) {
		String tablename = "bankdata";
		try {
			Statement stmt = sql.connection.createStatement();
			String query = "select * from "+tablename;
			// true if the first result is a ResultSetobject; false if it is an update count or there are no results
			stmt.execute(alpha2==null ? query : query + " where country_code='"+alpha2+"'");
			ResultSet rs = stmt.getResultSet();
			logRs(rs);
		} catch (SQLException e) {
			LOG.warning(e.getSQLState() + " " + e.getMessage());
			if("42P01".equals(e.getSQLState())) {
				// (PostgreSQL) SQLState: 42P01 : Relation »xxx« existiert nicht
			} else {
				e.printStackTrace();
			}
		}
	}

	void close() {
		try {
			if(sql.connection.isClosed()) return;
			sql.connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
