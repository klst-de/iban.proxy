package com.klst.iban.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/*
groovy:
//		def db = [url:'jdbc:h2:~/data/H2/bankdata', user:'SA', password:'', driver:'org.h2.Driver']
		def db = [url:'jdbc:postgresql://localhost:5433/ad393', user:'adempiere', password:'adempiereIstEsNicht', driver:'org.postgresql.Driver']
		try {
			sqlInstance = Sql.newInstance(db.url, db.user, db.password, db.driver)
		} catch (Exception e) {
			println "${CLASSNAME}:ctor ${e} Datenbank ${db.url} nicht erreichbar."
			//throw e
		}

jdbc:subprotocol:subname // url Allgemein
subprotocol ::= postgresql
subname ::= //localhost:5432/ad393
subname ::= //[user[:password]@][netloc][:port][/dbname][?param1=value1&...]
subname ::= //{host}[:{port}]/[{database}]


 */
public class SqlInstance {

	// http://hsqldb.org/doc/ beschreibt HyperSQL Database Engine 2.5.1
	public static final String HSQLDB_25_DRIVER = "org.hsqldb.jdbc.JDBCDriver";
	// in LibreOffice ist HSQL Database Engine 1.8.0
	public static final String HSQLDB_18_DRIVER = "org.hsqldb.jdbcDriver";

	public static final String H2_DRIVER = "org.h2.Driver"; // h2-1.4.200

	public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

	private static Properties info(String user, String password) {
        Properties info = new java.util.Properties();
        if (user != null) {
            info.put("user", user);
        }
        if (password != null) {
            info.put("password", password);
        }
        return info;
	}

	Connection connection;

	public SqlInstance(String url, Properties info, String driver) {
		try {
			Class.forName(driver);
		} catch (Exception e) {
			System.err.println("ERROR: failed to load JDBC driver.");
			e.printStackTrace();
			return;
		}
		
		connection = null;
		try {
			connection = DriverManager.getConnection(url, info);
		} catch (SQLException e) {
			System.err.println("ERROR: failed to get connection to "+url+(info==null ? "." : (" with properties "+info)));
			e.printStackTrace();
			return;
		}
	}
	public SqlInstance(String url, String user, String password, String driver) {
        this(url, info(user, password), driver);
	}
	public SqlInstance(String url, String driver) {
		this(url, null, null, driver);
	}
}
