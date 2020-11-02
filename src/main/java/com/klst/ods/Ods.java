package com.klst.ods;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

public class Ods {

	private static final Logger LOG = Logger.getLogger(Ods.class.getName());

	public static List<Sheet> getSheets(String odsFilePath) {
        SpreadSheet spreadSheet = null;
		try {
			spreadSheet = new SpreadSheet(new File(odsFilePath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.info("Number of sheets: " + spreadSheet.getNumSheets());
        List<Sheet> sheets = spreadSheet.getSheets();
		return sheets;
	}
	
	public static int getMaxRows(Range range) {
        int maxRows = 0;
		Object[][] values = range.getValues();
		for (int r = 0; r < range.getNumRows(); r++) {
			for (int c = 0; c < range.getNumColumns(); c++) {
				Object v = values[r][c];
				if(v!=null) maxRows = 1+r;
				//LOG.info("r("+r+"),c:"+v);
			}
		}
		if(maxRows>0) {
			LOG.info("Columns " + range.getNumColumns() + " , Rows " + range.getNumRows() + " maxRows="+maxRows);
		}
		return maxRows;
	}

    public static Sheet getNonEmptySheet(List<Sheet> sheets, Map<String,Integer> nonEmptySheets, int numColumns) {
    	Sheet nonEmptySheet = null;
        for (Sheet sheet : sheets) {
            Range range = sheet.getDataRange();
            int totalRows = 0;
            int maxRows = 0;
            int rangeNumColumns = range.getNumColumns();
            int lastColumn = range.getLastColumn();
            int numRows = range.getNumRows();
            int lastRow = range.getLastRow();
            int numValues = range.getNumValues();
            //                             1002/1003 
    		LOG.info("Columns " + lastColumn +"/" +rangeNumColumns+ " , Rows " + lastRow +"/"+numRows + " in sheet " + sheet.getName() + " numValues="+numValues);
//            LOG.info(range.toString()); // too long for print, MAX_PRINTABLE = 1024
    		
    		for (int c = 0; c < numColumns ; c++) {
        		range = sheet.getRange(0, c, numRows);
        		maxRows = Ods.getMaxRows(range);
        		if(maxRows>totalRows) totalRows = maxRows;
    		}
    		if(totalRows==0) {
        		LOG.info("empty sheet " + sheet.getName());
    		} else {
        		LOG.info("sheet " + sheet.getName() + " totalRows="+totalRows);
        		nonEmptySheet = sheet;
        		nonEmptySheets.put(nonEmptySheet.getName(), totalRows);
    		}
    	}
    	return nonEmptySheet;
    }
    
/*

dokumentiert ist es ( Range.getValues() ) so:
	The (cell)values could be String, Float, Integer, OfficeCurrency, OfficePercentage or 
	a DateEmpty cells returns a null object
	
Aber numerische Werte werden als Double gespeichert
 */
    public static Integer getInteger(Object cellValue) {
		Integer integer = null;
		if(cellValue instanceof String) {
			try {
				integer = Integer.parseInt((String)cellValue);
			} catch (NumberFormatException e) {
			}
		}
		if(cellValue instanceof Double) {
//			TODO check: MIN_VALUE <= res <= MAX_VALUE
			integer = (int)(double)((Double)cellValue);
		}
		return integer;
    }
}
