package com.klst.merge

import groovy.json.JsonSlurper
import groovy.lang.Binding
import groovy.lang.Script
import groovy.sql.Sql

import java.sql.SQLException

import com.klst.iban.datastore.DatabaseProxy
import com.klst.iban.datastore.SqlInstance

/**
	load bankdata in database 
 */
class Bankdata extends Script {

	static final URL_PREFIX = 'https://raw.githubusercontent.com/homebeaver/bankdata/main/iban-countries/'
	static final JSON_EXT = ".json"
	static final TABLENAME = "bankdata"
	// ADempiere:
	static final SUPER_USER_ID = 100
	static final SYSTEM_CLIENT_ID = 0
	static final db = [url:DatabaseProxy.H2_DATASTORE, user:'SA', password:'', driver:SqlInstance.H2_DRIVER]
//	static final db = [url:DatabaseProxy.POSTGRESQL_DATASTORE, user:"adempiere", password:"??", driver:SqlInstance.POSTGRESQL_DRIVER]
	def CLASSNAME = this.getClass().getName()
	Sql sqlInstance
	
	public Bankdata() {
		println "${CLASSNAME}:ctor"
	}

	public Bankdata(Binding binding) {
		super(binding);
		println "${CLASSNAME}:ctor binding"
		try {
			sqlInstance = Sql.newInstance(db.url, db.user, db.password, db.driver)
		} catch (Exception e) {
			println "${CLASSNAME}:ctor ${e} Datenbank ${db.url} nicht erreichbar."
			//throw e
		}
	}

	// @see https://www.postgresql.org/docs/10/errcodes-appendix.html
	static final SUCCESSFUL_COMPLETION = "00000"
	static final UNDEFINED_TABLE = "42P01" // PG
	static final DUPLICATE_TABLE = "42P07"
	static final TABLE_NOT_FOUND = "42S02" // H2
	def lastSQLState = SUCCESSFUL_COMPLETION
	
	// Returns:true if the first result is a ResultSet object; or an update count
	def doSql = { sql , param=[] ->
		def current = sqlInstance.connection.autoCommit = false
		def res
		try {
			def isResultSet = sqlInstance.execute(sql,param)
			if(isResultSet) {
				println "${CLASSNAME}:doSql isQuery : ${sql}"
				res = isResultSet // true
			} else {
				res = sqlInstance.getUpdateCount()
//				println "${CLASSNAME}:doSql updates = ${res} : ${sql} param =  ${param}" // log.fine
				sqlInstance.commit();
			}
			lastSQLState = SUCCESSFUL_COMPLETION
		} catch(SQLException ex) {
			println "${CLASSNAME}:doSql ${ex} ErrorCode:${ex.getErrorCode()} SQLState:${ex.getSQLState()}"
			lastSQLState = ex.getSQLState()
			sqlInstance.rollback()
			println "${CLASSNAME}:doSql Transaction rollback."
		}
		sqlInstance.connection.autoCommit = current
		return res                  
	}

/*

Überlegungen zum Datenmodell
 - FK country_code : country_code( alpha2 )
 - PK candidate a: country_code, id oder
      candidate b: country_code, bank_code, branch_code
 dazu muss aber INTEGER bank_code aus bank_id errechnet werden
 und branch_code sollte INTEGER sein, alternativ:
      candidate c: country_code, bank_id, branch_code VARCHAR(10)

 */
	def createTable = { tableName=TABLENAME ->
		def sql = """
CREATE TABLE ${tableName}
(
	country_code  character(2) NOT NULL,                -- PK, mit FK in Tabelle country_code
	id            numeric(10,0),               -- PK a für AD
	swift_code    VARCHAR( 11),
	bank_id       VARCHAR( 10) NOT NULL,
	bank_code INTEGER,
	branch_code   VARCHAR(10) NOT NULL,                 -- PK c ==> NULL not allowed for column "BRANCH_CODE"
	bank          VARCHAR(120) NOT NULL,
	branch        VARCHAR(120),
	support_codes SMALLINT,     -- (Postgres)Typ »tinyint« existiert nicht
-- location:
	state         VARCHAR(120),
	zip           VARCHAR(120),
	city          VARCHAR(120),
	address       VARCHAR(120),
-- contact:
	PHONE         VARCHAR(120),
	FAX           VARCHAR(120),
	WWW           VARCHAR(120),
	EMAIL         VARCHAR(120),
--  CONSTRAINT ${tableName}_pkey PRIMARY KEY (country_code, id),
  CONSTRAINT ${tableName}_pkey PRIMARY KEY (country_code, bank_id, branch_code),
  CONSTRAINT ${tableName}_FK FOREIGN KEY (country_code) 
    REFERENCES country_code( alpha2 ) ON DELETE RESTRICT ON UPDATE RESTRICT  
);
"""
		def res = doSql(sql)
		println "${CLASSNAME}:createTable res=${res}" // 0 or catched exception
		if(res==null) {
			throw new SQLException("CREATE TABLE ${tableName}", lastSQLState)
		}
	}
	
	/*

Spalten des json objects

bank_code müsste demnach in zwei Spalten gemapped werden:
 - in bank_id varchar(10) immer
 - und in bank_code, nur wenn es numerisch ist (das lasse ich weg)

	 */
	def cols = [country_code: ["country_code"]
		, id                : ["id"]
		, swift_code        : ["swift_code"]
		, bank_code         : ["bank_id"] // numeric ==> int bank_code, oder String, z.B. BG ==> bank_id varchar(10)
		, branch_code       : ["branch_code"] // ==> branch_code varchar(10) 
		, bank              : ["bank"] // name
		, branch            : ["branch"] // name
		, state             : ["state"]
		, zip               : ["zip"]
		, city              : ["city"]
		, address           : ["address"]
//		, phone             : ["phone"]
		// ... TODO
		, support_codes     : ["support_codes"]
		]
//	Closure<String> colkeys = { -> cols.keySet().collect { it }.join(', ') }
		// name colkeys irreführend, da es das erste element von values ist
	Closure<String> colkeys = { -> cols.values().collect { it.get(0) }.join(', ') }
	def data = [:]
	def processLine = { line , tableName=TABLENAME ->
		
	}

	Integer countryLoad(String jsonString, tableName=TABLENAME) {
		def jsonSlurper = new JsonSlurper()
		object = jsonSlurper.parseText(jsonString)
		assert object instanceof Map
		// "country_code": "AD",
		// "ISO_3166_1": { "name": "Andorra" 
		// "list": [ ...
		println "${CLASSNAME}:countryLoad country_code: '${object.country_code}', no of objects:${object.list.size()}"
		println "${CLASSNAME}:countryLoad colkeys:${colkeys}"
		List oList = object.list
		oList.each { bank ->
//	uncomment to log bank record to be inserted into db	
//			println "${CLASSNAME}:countryLoad ${bank}"
			data = [:]
			cols.each {
				def name = it.key // name des json objekts
				def dbcol = it.value.get(0) // name der db spalte zum json objekt
				if(name=='country_code') {
					data.put(name, "'"+object.country_code+"'") // immer '<country_code>'
				} else if(name=='bank_code' || name=='branch_code') { // kann numerisch sein, wird nach varchar abgebildet 
					def code = bank.get(name)
					data.put(name, code ? "'"+code+"'" : "''")
				} else if(bank.get(name).is(null)) {
					data.put(name, null)
				} else if(name=='id' || name=='support_codes') {
					data.put(name, bank.get(name))
				} else {
					def str = bank.get(name).toString()
					data.put(name, (str.length()==0 ? null : "'"+str.replace("'","''")+"'"))
				}
			}
			Closure<String> values = { -> data.values().collect { it }.join(', ') }
			def sql = """
INSERT INTO ${tableName} 
( ${colkeys} )
VALUES ( ${values} )
"""
			sqlInstance.execute(sql,[])
			def inserts = sqlInstance.getUpdateCount()
			if(inserts!=1) println "${CLASSNAME}:countryLoad inserts=${inserts} : ${sql}"
		}
		return object.list.size()
	}
	
	Integer countBicCountry(String countryCode, from=TABLENAME) {
		def fromsql = """
SELECT SUBSTRING(swift_code,5,2) as cc , COUNT(*) as count FROM ${from}
WHERE country_code=?
GROUP BY SUBSTRING(swift_code,5,2)
"""
		sqlInstance.eachRow(fromsql,[countryCode]) { fromrow ->
			println "${CLASSNAME}:countBicCountry ${fromrow}"
			def count = fromrow.count
		}
	}
 
	def jsonString = new StringBuilder()
	// see https://stackoverflow.com/questions/11863474/how-to-read-text-file-from-remote-system-and-then-write-it-into-array-of-string
	def	populate = { countryCode , charsetName="UTF-8" ->
		def filename = countryCode+JSON_EXT
		println "${CLASSNAME}:populate from ${filename}"
		File file = new File("../iban.proxy/data/iban-countries/"+filename)
		BufferedReader reader = null
		def done = 0
		if(file.exists()) {
			println "${CLASSNAME}:populate canRead: ${file.canRead()} file: ${file.getAbsolutePath()}"
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName))
		} else {
			println "${CLASSNAME}:populate canRead: File does not exist ${file.getAbsolutePath()} - try url"
			URL url = new URL(URL_PREFIX +filename)
			reader = new BufferedReader(new InputStreamReader(url.openStream())) // , "UTF-8"
		}
		this.jsonString = new StringBuilder()
		reader.eachLine { line ->
			this.jsonString.append(line)
			done = done+1
		}
		int inserted = countryLoad(this.jsonString.toString())
		if(inserted>0) {
		    countBicCountry(countryCode)	   
		}
		println "${CLASSNAME}:populate done ${done} lines, ${inserted} inserted recs."
	}

	def	deletefrom = { String countryCode, tablename=TABLENAME ->
		def sql = """
DELETE FROM ${tablename} WHERE country_code = '${countryCode}'
"""
		def res = doSql(sql)			
		println "${CLASSNAME}:deletefrom ${res}" // 0 or catched exception
		if(res==null) {
			throw new SQLException("${sql}", lastSQLState)
		}
	}
	
	void initialLoad(String countryCode) {
		sqlInstance.connection.autoCommit = false
		populate( countryCode )
		sqlInstance.commit()
	}
	
	void deleteAndLoad(String countryCode) {
		deletefrom(countryCode)
		sqlInstance.connection.autoCommit = false
		populate( countryCode )
		sqlInstance.commit()
	}

/*
countryCode ist der name des files, z.B. AD.json, CH.json, DK.json, ... aus dem die daten geladen werden.
Es ist das Land aus dem die IBAN Clearingstelle stammt, z.B. Dänemark. Aus wikipedia: 
	"Die Bestimmung des Ländercodes nach dem Land der Clearingstelle wird nicht durchgängig eingehalten. 
	So befindet sich die Clearingstelle für Grönland (GL) in Dänemark (DK), die IBANs beginnen aber dennoch mit GL."
Die (dänische) bank bbbb=1601, Grønlandsbanken hat GL in der BIC: GRENGLGXXXX
countryCode ist nicht der Ländercode aus BIC! 
SQL zu Beispiel GL und FO (Färöer):
select * from bankdata where country_code='DK' and substring(swift_code,5,2)<>country_code -- wg. GL und FO :#7
Auch in CH.json gibt es viele Institute mit anderen Ländercode als CH.
 */	
	void loadOrInitialLoad(countryCode, createTableAndInitialLoad=false) {	
		try {
			deleteAndLoad(countryCode)
		} catch(SQLException ex) {
			println "${CLASSNAME}:loadOrInitialLoad SQLState ${ex.getSQLState()}"
			if(ex.getSQLState()==UNDEFINED_TABLE || ex.getSQLState()==TABLE_NOT_FOUND) {
				println "${CLASSNAME}:loadOrInitialLoad table ${TABLENAME} does not exist - try to create ..."
				createTableAndInitialLoad = true
			}
		}
		if(createTableAndInitialLoad) try {
			createTable()
			initialLoad(countryCode)
		} catch(SQLException ex) {
			if(ex.getSQLState()==DUPLICATE_TABLE) {
				println "${CLASSNAME}:loadOrInitialLoad table ${TABLENAME} exists"
			} else {
				throw ex
			}
		}
		
	}
	@Override
	public Object run() {  // nur Test
		println "${CLASSNAME}:run"
		println "${CLASSNAME}:run sqlInstance:${this.sqlInstance}"
		if(this.sqlInstance) {
			println "${CLASSNAME}:run Connection:${this.sqlInstance.getConnection()}"
			loadOrInitialLoad("AD")
			loadOrInitialLoad("AE")
			loadOrInitialLoad("AT")
//com.klst.merge.Bankdata:countBicCountry [CC:AT, COUNT:874]
//com.klst.merge.Bankdata:countBicCountry [CC:DE, COUNT:2]
			loadOrInitialLoad("BE")
//com.klst.merge.Bankdata:countBicCountry [CC:BE, COUNT:845]
//com.klst.merge.Bankdata:countBicCountry [CC:FR, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:LU, COUNT:1]
			loadOrInitialLoad("BG")
			loadOrInitialLoad("CH")
//com.klst.merge.Bankdata:countBicCountry [CC:AT, COUNT:21]
//com.klst.merge.Bankdata:countBicCountry [CC:CH, COUNT:1587]
//com.klst.merge.Bankdata:countBicCountry [CC:DE, COUNT:26]
//com.klst.merge.Bankdata:countBicCountry [CC:DK, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:FI, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:GB, COUNT:15]
//com.klst.merge.Bankdata:countBicCountry [CC:GR, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:IT, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:LI, COUNT:24]
//com.klst.merge.Bankdata:countBicCountry [CC:LT, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:LU, COUNT:8]
//com.klst.merge.Bankdata:countBicCountry [CC:NL, COUNT:5]
//com.klst.merge.Bankdata:countBicCountry [CC:SE, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:US, COUNT:1]
			loadOrInitialLoad("CR")
			loadOrInitialLoad("CZ")
			loadOrInitialLoad("DE")
			loadOrInitialLoad("DK")
//com.klst.merge.Bankdata:countBicCountry [CC:DK, COUNT:3883]
//com.klst.merge.Bankdata:countBicCountry [CC:FO, COUNT:4]
//com.klst.merge.Bankdata:countBicCountry [CC:GL, COUNT:3]
			loadOrInitialLoad("EE")
			loadOrInitialLoad("ES")
			loadOrInitialLoad("FI")
			loadOrInitialLoad("FR")
//com.klst.merge.Bankdata:countBicCountry [CC:FR, COUNT:569]
//com.klst.merge.Bankdata:countBicCountry [CC:GB, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:GF, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:GP, COUNT:9]
//com.klst.merge.Bankdata:countBicCountry [CC:LU, COUNT:1]
//com.klst.merge.Bankdata:countBicCountry [CC:MQ, COUNT:4]
//com.klst.merge.Bankdata:countBicCountry [CC:NC, COUNT:9]
//com.klst.merge.Bankdata:countBicCountry [CC:PF, COUNT:6]
//com.klst.merge.Bankdata:countBicCountry [CC:PM, COUNT:2]
//com.klst.merge.Bankdata:countBicCountry [CC:RE, COUNT:7]
//com.klst.merge.Bankdata:countBicCountry [CC:WF, COUNT:1]
			loadOrInitialLoad("GR")
			loadOrInitialLoad("HR")
			loadOrInitialLoad("IE")
//com.klst.merge.Bankdata:countBicCountry [CC:GB, COUNT:613]
//com.klst.merge.Bankdata:countBicCountry [CC:IE, COUNT:1505]
			loadOrInitialLoad("IS")
			loadOrInitialLoad("IT")
//com.klst.merge.Bankdata:countBicCountry [cc:CH, count:1]
//com.klst.merge.Bankdata:countBicCountry [cc:CZ, count:1]
//com.klst.merge.Bankdata:countBicCountry [cc:DE, count:3]
//com.klst.merge.Bankdata:countBicCountry [cc:FR, count:1]
//com.klst.merge.Bankdata:countBicCountry [cc:GB, count:1]
//com.klst.merge.Bankdata:countBicCountry [cc:IT, count:835]
//com.klst.merge.Bankdata:countBicCountry [cc:SM, count:10]
			loadOrInitialLoad("KZ")
//com.klst.merge.Bankdata:countBicCountry [cc:KZ, count:43]
//com.klst.merge.Bankdata:countBicCountry [cc:RU, count:2]
			loadOrInitialLoad("LB")
			loadOrInitialLoad("LI")
			loadOrInitialLoad("LT")
			loadOrInitialLoad("LU")
			loadOrInitialLoad("LV")
			loadOrInitialLoad("MC")
			loadOrInitialLoad("MT")
			loadOrInitialLoad("NL")
			loadOrInitialLoad("NO")
			loadOrInitialLoad("PL") // #38 bzw.#25 ohne BIC
//com.klst.merge.Bankdata:countBicCountry [CC:[null], COUNT:25]
//com.klst.merge.Bankdata:countBicCountry [CC:PL, COUNT:3109]
			loadOrInitialLoad("RS")
			loadOrInitialLoad("SA")
			loadOrInitialLoad("SE")
			loadOrInitialLoad("SI")
			loadOrInitialLoad("SK")
//com.klst.merge.Bankdata:countBicCountry [cc:CZ, count:3]
//com.klst.merge.Bankdata:countBicCountry [cc:SK, count:33]
			loadOrInitialLoad("SM")
			loadOrInitialLoad("VA")
			loadOrInitialLoad("XK")
		}
		
		return this;
	}

  // wird in eclipse benötigt, damit ein "Run As Groovy Script" möglich ist (ohne Inhalt)
  // nach dem Instanzieren wird run() ausgeführt
  static main(args) {
  }

}
