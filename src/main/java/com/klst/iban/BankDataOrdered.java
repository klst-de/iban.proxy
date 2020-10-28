package com.klst.iban;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/* eine Klasse mit

 public static String toOrderedJSONString(Map map){

 */
// wg. ordering
public class BankDataOrdered {
//public class BankDataJSONObject extends JSONObject {

	final static List<String> MANDATORY_KEYS = Arrays.asList
			(Bank_Data.ID, Bank_Data.SWIFT_CODE, Bank_Data.BANK_CODE, Bank_Data.BANK);
	
	public final static List<String> OPTIONAL_KEYS = Arrays.asList
			( Bank_Data.BRANCH_CODE, Bank_Data.BRANCH
			, Bank_Data.STATE, Bank_Data.ZIP, Bank_Data.CITY, Bank_Data.ADDRESS // location
			, Bank_Data.PHONE, Bank_Data.FAX, Bank_Data.WWW, Bank_Data.EMAIL	// contact
			, Bank_Data.SUPPORT_CODES);

//	BankDataJSONObject(JSONObject jo) {
//		super(); // HashMap(), andere ctoren: HashMap(Map<? extends K, ? extends V> m)
//		put(Bank_Data.ID, jo.get(Bank_Data.ID));
//		put(Bank_Data.SWIFT_CODE, jo.get(Bank_Data.SWIFT_CODE));
//		put(Bank_Data.BANK_CODE, jo.get(Bank_Data.BANK_CODE));
//		put(Bank_Data.BANK, jo.get(Bank_Data.BANK));
//	}
//	public String toString(){
//		// in super: return toJSONString();
//		return super.toJSONString();
////		return toOrderedJSONString();
//	}
//
//	public String toJSONString(){
//		// in super: return toJSONString(this);
//		return toOrderedJSONString(this);
//	}

//	public static String toJSONString(Map map) ...
	public static String toOrderedJSONString(Map map) {
		if(map == null) return "null";
		
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		sb.append('{');
		// all mandatory props
		Iterator iter = MANDATORY_KEYS.iterator();
		while(iter.hasNext()) {
			if(first) {
				first = false;
			} else {
				sb.append(',');
			}
			Object key = iter.next();
			Object value = map.get(key);
			toJSONString(String.valueOf(key),value, sb); // JSONObject.toJSONString ist private!
		}
		
		iter=OPTIONAL_KEYS.iterator();
        while(iter.hasNext()){
        	Object key = iter.next();
        	Object value = map.get(key);
        	if(value!=null && !value.toString().isEmpty()) {
    			if(first) {
    				first = false;
    			} else {
    				sb.append(',');
    			}
        		toJSONString(String.valueOf(key),value, sb);
        	}
        }

		sb.append('}');
		return sb.toString();
	}

	// wg. Sichtbarkeit aus JSONObject.toJSONString hierhin kopiert
	private static String toJSONString(String key, Object value, StringBuffer sb) {
		sb.append('\"');
		if (key == null)
			sb.append("null");
		else {
			//JSONValue.escape(key, sb); // The method escape(String, StringBuffer) from the type JSONValue is not visible
			escape(key, sb);
		}
		sb.append('\"').append(':');

		sb.append(JSONValue.toJSONString(value));

		return sb.toString();
	}
	
    /**
     * @param s - Must not be null.
     * @param sb
     */
	// wg. Sichtbarkeit aus JSONValue.escape hierhin kopiert
    static void escape(String s, StringBuffer sb) {
		for(int i=0;i<s.length();i++){
			char ch=s.charAt(i);
			switch(ch){
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
                //Reference: http://www.unicode.org/versions/Unicode5.1.0/
				if((ch>='\u0000' && ch<='\u001F') || (ch>='\u007F' && ch<='\u009F') || (ch>='\u2000' && ch<='\u20FF')){
					String ss=Integer.toHexString(ch);
					sb.append("\\u");
					for(int k=0;k<4-ss.length();k++){
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				}
				else{
					sb.append(ch);
				}
			}
		}//for
	}

}
