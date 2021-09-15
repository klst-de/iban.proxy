package com.klst.merge

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.sql.Sql;
import java.sql.SQLException

class Ext_bankleitzahlen extends Script {

	static final URL_PREFIX = 'https://raw.githubusercontent.com/klst-de/de-mpiere/master/data/bundesbank/'
	static final SUPER_USER_ID = 100
	static final SYSTEM_CLIENT_ID = 0
	
	static final ISO_3166_1_DE = [ name: "Germany" , name_fr: "Allemagne (l')" , alpha2: "DE" , alpha3: "DEU" , numeric: 276 ]
	//{ name: "Germany" , name_fr: "Allemagne (l')" , alpha2: "DE" , alpha3: "DEU" , numeric: 276 }
	
	def CLASSNAME = this.getClass().getName()
//	def DEFAULT_FROM_SCHEMA = "adempiere"
//	static final DEFAULT_TO_SCHEMA = "adempiere"
//	def GERDENWORD_CLIENT_ID = 11
//	def DEFAULT_CLIENT_ID = 1000000
	Sql sqlInstance
	
	public Ext_bankleitzahlen() {
		println "${CLASSNAME}:ctor"
	}

	public Ext_bankleitzahlen(Binding binding) {
		super(binding);
		println "${CLASSNAME}:ctor binding for ${ISO_3166_1_DE}"
		// ACHTUNG : nonstd port
		def db = [url:'jdbc:postgresql://localhost:5433/ad393', user:'adempiere', password:'adempiereIstEsNicht', driver:'org.postgresql.Driver']
		try {
			sqlInstance = Sql.newInstance(db.url, db.user, db.password, db.driver)
		} catch (Exception e) {
			println "${CLASSNAME}:ctor ${e} Datenbank ${db.url} nicht erreichbar."
			//throw e
		}
	}

	// @see https://www.postgresql.org/docs/10/errcodes-appendix.html
	static final SUCCESSFUL_COMPLETION = "00000"
	static final UNDEFINED_TABLE = "42P01"
	static final DUPLICATE_TABLE = "42P07"
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

	static final TABLENAME = "ext_bankleitzahlen"
	
	def createTable = { tableName=TABLENAME ->
		def sql = """
CREATE TABLE ${tableName}
(
  ext_bankleitzahlen_id numeric(10,0) NOT NULL,        -- fld 10 Konflikt mit c_bank_id 100 und 50000 GW, daher + ISO_3166_1_DE.numeric*1000000 
  routingno character varying(20) NOT NULL,            -- fld  1 BLZ , len  8
  merkmal character(1) NOT NULL DEFAULT '1'::bpchar,   -- fld  2
  name character varying(100) NOT NULL,                -- fld  3     , len 58
  postal character varying(10),                        -- fld  4     , len  5
  city character varying(60),                          -- fld  5     , len 35
                                                       -- fld  6     , len 27 (omit)
  pan character varying(5),                            -- fld  7
  swiftcode character varying(20),                     -- fld  8              , https://de.wikipedia.org/wiki/ISO_9362 ==bic , len 8 oder 11 (3optional
  checkdigitmethod character(2),                       -- fld  9     , len  2 , https://de.wikipedia.org/wiki/Pr%C3%BCfziffer
  chg character(1) NOT NULL DEFAULT 'U'::bpchar,       -- fld 11
  del character(1) NOT NULL DEFAULT '0'::bpchar,       -- fld 12
  newroutingno character varying(8),                   -- fld 13 BLZ , len  8
  ibanrule character varying(6),                       -- fld 14
  
  CONSTRAINT ext_bankleitzahlen_pkey PRIMARY KEY (ext_bankleitzahlen_id),
  CONSTRAINT ext_bankleitzahlen_merkmal_check CHECK (merkmal = ANY (ARRAY['1'::bpchar, '2'::bpchar])),
  CONSTRAINT ext_bankleitzahlen_chg_check CHECK (chg = ANY (ARRAY['A'::bpchar, 'D'::bpchar, 'U'::bpchar, 'M'::bpchar])),
  CONSTRAINT ext_bankleitzahlen_del_check CHECK (del = ANY (ARRAY['0'::bpchar, '1'::bpchar]))
);
"""
		def res = doSql(sql)
		println "${CLASSNAME}:createTable res=${res}" // 0 or catched exception
		if(res==null) {
			throw new SQLException("CREATE TABLE ${tableName}", lastSQLState)
		}
	}
	
/*
Satzaufbau der Bankleitzahlendatei 
Quelle https://www.bundesbank.de/Redaktion/DE/Standardartikel/Aufgaben/Unbarer_Zahlungsverkehr/bankleitzahlen_download.html 
neu:   https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/bankleitzahlen/download---bankleitzahlen-602592
(en):  https://www.bundesbank.de/en/tasks/payment-systems/services/bank-sort-codes/ 

Feld- Inhalt                                                                      Anzahl der Stellen
Nr.                                                                                  Nummerierung der Stellen
 1 Bankleitzahl                                                                    8   1 - 8
 2 Merkmal, ob bankleitzahlführender Zahlungsdienstleister („1“) oder nicht („2“)  1   9
 3 Bezeichnung des Zahlungsdienstleisters (ohne Rechtsform)                       58  10 - 67
 4 Postleitzahl                                                                    5  68 - 72
 5 Ort                                                                            35  73 - 107
 6 Kurzbezeichnung des Zahlungsdienstleisters mit Ort (ohne Rechtsform)           27 108 - 134
 7 Institutsnummer für PAN                                                         5 135 - 139
 8 Business Identifier Code – BIC                                                 11 140 - 150
 9 Kennzeichen für Prüfzifferberechnungsmethode                                    2 151 - 152
10 Nummer des Datensatzes                                                          6 153 - 158
11 Änderungskennzeichen                                                            1 159
   „A“ (Addition) für neue, 
   „D“ (Deletion) für gelöschte,
   „U“(Unchanged) für unveränderte und 
   „M“ (Modified) für veränderte Datensätze
12 Hinweis auf eine beabsichtigteBankleitzahllöschung                              1 160
   "0", sofern keine Angabe
   "1", sofern BLZ im Feld 1 zur Löschung vorgesehen ist
13 Hinweis auf Nachfolge-Bankleitzahl                                              8 161 - 168
14 Kennzeichen für die IBAN-Regel (nur erweiterte Bankleitzahlendatei)             6 169 - 174

key: 10/Nummer des Datensatzes

unique für merkmal='1' -- bankleitzahlführender Zahlungsdienstleister : COALESCE(pan,'00000')||routingno
siehe https://de.wikipedia.org/wiki/Primary_Account_Number

mapping to TABLE c_bank
  c_bank_id                               ext_bankleitzahlen_id + ISO_3166_1_DE.numeric*1000000
  ad_client_id                            0
  ad_org_id                               0
  isactive                                aus merkmal! 'Y' <= '1' ?
  created                                 '2018-09-03'  aus BLZ_20180903.txt  
  createdby                               SUPER_USER_ID = 100
  updated                                 "2001-05-11 18:19:36"
  updatedby  
  name                                    name , city 
  routingno                               routingno
  c_location_id                            -> postal,city
  swiftcode                               swiftcode
  isownbank                               'N' // default
  description                             merkmal,panin,checkdigitmethod,chg,del,newroutingno als map in JSON Format
  uuid  
 */
	static final int FIRST_ID = 276000000 // ISO_3166_1_DE.numeric*1000000
	def cols = [ext_bankleitzahlen_id: [6,153,"int"]
		, routingno        : [ 8,  1,"utf"]
		, merkmal          : [ 1,  9,"utf"]
		, name             : [58, 10,""]
		, postal           : [ 5, 68,"utf"]
		, city             : [35, 73,""]
		, pan              : [ 5,135,"utf"]
		, swiftcode        : [11,140,"utf"]
		, checkdigitmethod : [ 2,151,"utf"]
		, chg              : [ 1,159,"utf"]
		, del              : [ 1,160,"utf"]
		, newroutingno     : [ 8,161,"utf"]
		]
	Closure<String> colkeys = { -> cols.keySet().collect { it }.join(', ') }
	def data = [:]
	def processLine = { line , tableName=TABLENAME ->
		if (line.trim().size() == 0) {
			return 0
		} else {
			data = [:]
			cols.each {
				def name = it.key
				def from = it.value.get(1) - 1
				def to = it.value.get(0) + from
				if(it.value.get(2)=='int') {
					data.put(name, FIRST_ID + Integer.parseInt(line.substring(from,to)))
				} else {
					def str = line.substring(from,to).trim()
					data.put(name, (str.length()==0 ? null : "'"+str+"'"))
				} 
			}
			// Closure<String> values = { -> map.keySet().collect { ":${it}" }.join(', ') }
			Closure<String> values = { -> data.values().collect { it }.join(', ') }
		def sql = """
INSERT INTO ${tableName} 
( ${colkeys} )
VALUES ( ${values} )
"""
//			println "${CLASSNAME}:processLine '${data}'" // log.fine
//			println "${CLASSNAME}:processLine '${sql}" // log.fineer
			sqlInstance.execute(sql,[])
			def inserts = sqlInstance.getUpdateCount()
//			println "${CLASSNAME}:processLine inserts = ${inserts}." // log.fineer
			return inserts
		}
	}

	// see https://stackoverflow.com/questions/11863474/how-to-read-text-file-from-remote-system-and-then-write-it-into-array-of-string
	def	populate = { filename , charsetName="Cp1252" ->
		println "${CLASSNAME}:populate from ${filename}"
		File file = new File(filename)
		BufferedReader reader = null
		def done = 0
		if(file.exists()) {
			println "${CLASSNAME}:populate canRead: ${file.canRead()} file: ${file.getAbsolutePath()}"
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName))
		} else {
			println "${CLASSNAME}:populate canRead: File does not exist ${file.getAbsolutePath()} - try url"
			URL url = new URL(filename)
			reader = new BufferedReader(new InputStreamReader(url.openStream())) // , "UTF-8"
		}
		def input = 0
		reader.eachLine { line ->
			input++
			def l = processLine(line)
			done = done+l
		}
		println "${CLASSNAME}:populate done ${done}/${input}"
	}

	def	deletefrom = { tablename=TABLENAME ->
		def sql = """
DELETE FROM ${tablename} 
"""
		def res = doSql(sql)			
		println "${CLASSNAME}:deletefrom ${res}" // 0 or catched exception
		if(res==null) {
			throw new SQLException("${sql}", lastSQLState)
		}
	}
	
	def	updateNewroutingno = { tablename=TABLENAME ->
		def sql = """
UPDATE ${tablename} 
SET newroutingno = null
WHERE newroutingno='00000000'
"""
		def res = doSql(sql)			
		println "${CLASSNAME}:updateNewroutingno ${res}" // n or catched exception
	}

	static final BLZ_FUEHRENDE_ZDL_ONLY = "AND merkmal='1'" // only merge (aka load to) bankleitzahlführender Zahlungsdienstleister
	static final TO_TABLENAME = "c_bank"
	
	/**
	 * merge from to tablename / "ext_bankleitzahlen" to "c_bank"
	 */
	def mergeInto = { tablename , updated , initialload=false , from=TABLENAME ->
		def targetid = "${tablename}_id"
		// to be updated
		def fromsql = """
SELECT ${colkeys} FROM ${from} 
WHERE ext_bankleitzahlen_id = ?
${BLZ_FUEHRENDE_ZDL_ONLY}
"""
		// to be inserted (all on initialload)
		def newfromsql = """
SELECT ${colkeys} FROM ${from} 
WHERE ext_bankleitzahlen_id not in( select ${targetid} from ${tablename} where ad_client_id = ${SYSTEM_CLIENT_ID} )
AND chg <> 'D' -- „D“ (Deletion)
${BLZ_FUEHRENDE_ZDL_ONLY}
--and ext_bankleitzahlen_id <= 100 -- test only
"""
		def isactivesql = """
SELECT ${targetid} FROM ${tablename} 
WHERE isactive ='Y'
--and ${targetid} <=5000 -- test only
"""
		def updsql = """
UPDATE ${tablename}
SET ad_client_id = ${SYSTEM_CLIENT_ID} --  , ad_org_id , created , createdby // is OK
  , isactive = :isactive
  , updated = '${updated}'
  , updatedby = ${SUPER_USER_ID}
  , name = :name
  , routingno = :routingno
  , swiftcode = :swiftcode
  , description = :description
WHERE ${tablename}_id = :id
"""
		def newinssql = """
INSERT INTO ${tablename}
 ( c_bank_id , ad_client_id , ad_org_id , created , createdby , updated , updatedby
 , name , routingno , swiftcode , description )
VALUES ( :id , ${SYSTEM_CLIENT_ID} , 0 , '${updated}' , ${SUPER_USER_ID} , '${updated}' , ${SUPER_USER_ID}
       , :name , :routingno , :swiftcode , :description )
"""
		int id = -1
		def nums = [ A:0 , D:0 , M:0 , U:0 ]
		sqlInstance.eachRow(isactivesql,[]) { row ->
			id = row[targetid]
			//println "${CLASSNAME}:mergeInto ${row}"
			sqlInstance.eachRow(fromsql,[id]) { fromrow ->
				//println "${CLASSNAME}:mergeInto ${fromrow}"
				String name = fromrow.name + " , " + fromrow.city 
				String routingno = fromrow.routingno
				String swiftcode = fromrow.swiftcode
				String description = [merkmal:fromrow.merkmal,panin:fromrow.pan,checkdigitmethod:fromrow.checkdigitmethod,chg:fromrow.chg,del:fromrow.del,newroutingno:fromrow.newroutingno]
				def chg = fromrow.chg
				nums[chg]++
				if(initialload) {
					//println "${CLASSNAME}:mergeInto modify!"
					chg = 'M'
				}
				switch (chg) { // Änderungskennzeichen
					case 'A' : // „A“ (Addition) für neue : add to target
						println "${CLASSNAME}:mergeInto Addition ${fromrow}"
						break
					case 'D' : // „D“ (Deletion) für gelöschte : set target to inactive
						println "${CLASSNAME}:mergeInto Deletion ${fromrow}"
						doSql(updsql,[isactive:'N', name:name,routingno:routingno,swiftcode:swiftcode,description:description, id:id])
						break
					case 'M' : // „M“ (Modified) für veränderte Datensätze : update
						println "${CLASSNAME}:mergeInto Modified ${fromrow}"
						//break
					case 'U' : // „U“(Unchanged) für unveränderte : do nothing? : doch ein update!
						doSql(updsql,[isactive:'Y', name:name,routingno:routingno,swiftcode:swiftcode,description:description, id:id])
						break
					default:
						println "${CLASSNAME}:mergeInto default ${fromrow}"
						break
				}
			}
		}
		println "${CLASSNAME}:mergeInto nums: ${nums} Addition not processed yet"

//		println "${CLASSNAME}:mergeInto \n${newfromsql}"
		int i = 0
		sqlInstance.eachRow(newfromsql,[]) { fromrow ->
			i++
//			println "${CLASSNAME}:mergeInto $i: ${fromrow}" // nur kurzinfo: id, name, city, branch (evtl), swiftcode 
			nums[fromrow.chg]++
			id = fromrow.ext_bankleitzahlen_id
			String name = fromrow.name + " , " + fromrow.city 
			String routingno = fromrow.routingno
			String swiftcode = fromrow.swiftcode
			if(swiftcode==null) {
				println "${CLASSNAME}:mergeInto $i: ${fromrow}"
			} else {
				String branch = swiftcode.length()>8 ? '"'+swiftcode.substring(8)+'"' : "null"
				println "${CLASSNAME}:mergeInto $i: { \"id\": ${id}, \"bank\": \"${fromrow.name}\", \"city\": \"${fromrow.city}\", \"branch\": ${branch}, \"swift_code\": \"${swiftcode}\" },"
			}
			String description = [merkmal:fromrow.merkmal,panin:fromrow.pan,checkdigitmethod:fromrow.checkdigitmethod,chg:fromrow.chg,del:fromrow.del,newroutingno:fromrow.newroutingno]
			doSql(newinssql,[name:name,routingno:routingno,swiftcode:swiftcode,description:description, id:id])
		}
//		println "${CLASSNAME}:mergeInto nums: ${nums} \n${newinssql}"	
		println "${CLASSNAME}:mergeInto Addition, Deletion, Modified, Unchanged: ${nums} inserted=${i}"
	}

	void initialLoad(String blzfilepathname, String ymd) {
		sqlInstance.connection.autoCommit = false
		populate( blzfilepathname ) // #16741
		sqlInstance.commit()
		
		updateNewroutingno() // #16688/16741 bzw #16483/16533

		mergeInto(TO_TABLENAME, ymd, true) // ( tablename , updated , initialload=false
	}
	
	void deleteAndLoad(String blzfilepathname, String ymd) {
		deletefrom()
		
		sqlInstance.connection.autoCommit = false
		populate( blzfilepathname ) // #16741
		sqlInstance.commit()
		
		updateNewroutingno() // #16688/16741 bzw #16483/16533

		mergeInto(TO_TABLENAME, ymd) // ( tablename , updated
	}
	
	void loadOrInitialLoad(createTableAndInitialLoad=false) {
		// in ad390 ist "name character varying(60) NOT NULL" zu kurz ==> https://github.com/adempiere/adempiere/issues/2266
		
		def urlprefix = URL_PREFIX
		try {
/*
select created,count(*),min(c_bank_id)-276000000 from c_bank
where c_bank_id between 276000000 and 276999999
and isactive='Y'
group by 1
--having count(*)>1
order by 1
2018-03-05 00:00:00	3467	1 deaktiviert: 114	1166
2018-09-03 00:00:00	27	57147                1	57242
2018-12-03 00:00:00	7	57280
2019-03-04 00:00:00	6	57325
2019-06-03 00:00:00	11	57365
2019-09-09 00:00:00	2	57378
2019-12-09 00:00:00	1	57432
2020-03-09 00:00:00	7	57461
2020-06-08 00:00:00	1	57478
2020-09-07 00:00:00	4	57480
2020-12-07 00:00:00	83	57525
 nach 		deleteAndLoad(urlprefix + 'BLZ_20210308.txt', '2021-03-08')
com.klst.merge.Ext_bankleitzahlen:mergeInto Addition, Deletion, Modified, Unchanged: [A:1, D:13, M:33, U:3522] inserted=1
2021-03-08 00:00:00	1	57644
 nach		deleteAndLoad(urlprefix + 'BLZ_20210607.txt', '2021-06-07')
com.klst.merge.Ext_bankleitzahlen:mergeInto Addition, Deletion, Modified, Unchanged: [A:3, D:3, M:10, U:3543] inserted=3
2021-06-07 00:00:00	3	57645
 nach		deleteAndLoad(urlprefix + 'BLZ_20210906.txt', '2021-09-06')
com.klst.merge.Ext_bankleitzahlen:mergeInto Addition, Deletion, Modified, Unchanged: [A:8, D:1, M:27, U:3528] inserted=8
2021-09-06 00:00:00	8	57650
 */			
			deleteAndLoad(urlprefix + 'BLZ_20210906.txt', '2021-09-06')
//			deleteAndLoad(urlprefix + 'BLZ_20210607.txt', '2021-06-07')
//			deleteAndLoad(urlprefix + 'BLZ_20210308.txt', '2021-03-08')
			// ... TODO die Aufrufe automatisieren
//			deleteAndLoad(urlprefix + 'BLZ_20181203.txt', '2018-12-03')
//			deleteAndLoad(urlprefix + 'BLZ_20190304.txt', '2019-03-04')
		} catch(SQLException ex) {
			if(ex.getSQLState()==UNDEFINED_TABLE) {
				println "${CLASSNAME}:run table ${TABLENAME} does not exist - try to create ..."
				createTableAndInitialLoad = true
			}
		}
		if(createTableAndInitialLoad) try {
			createTable()
			initialLoad(urlprefix + 'BLZ_20180305.txt', '2018-03-05')
		} catch(SQLException ex) {
			if(ex.getSQLState()==DUPLICATE_TABLE) {
				println "${CLASSNAME}:run table ${TABLENAME} exists"
			} else {
				throw ex
			}
		}
		
		// TODO inaktive löschen : ca #360 und updateSequence


	}
	@Override
	public Object run() {  // nur Test
		println "${CLASSNAME}:run"
		println "${CLASSNAME}:run sqlInstance:${this.sqlInstance}"
		if(this.sqlInstance) {
			loadOrInitialLoad()
		}
		
		return this;
	}

  // wird in eclipse benötigt, damit ein "Run As Groovy Script" möglich ist (ohne Inhalt)
  // nach dem Instanzieren wird run() ausgeführt
  static main(args) {
  }

}
