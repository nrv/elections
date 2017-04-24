package name.herve.elections;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ProcessPres2017 {
	private final static int SHEET_NUMBER = 3;
	private final static int FIRST_DATA_LINE = 2;
	
	private final static int COL_DEP = 0;
	private final static int COL_CIRCO = 2;
	
	private final static int COL_INSCRITS = 4;
	private final static int COL_VOTANTS = 7;
	private final static int COL_BLANCS = 9;
	private final static int COL_NULS = 12;
	
	private final static int NB_CANDIDATS = 11;
	private final static int COL_PREMIER_CANDIDAT = 18;
	private final static int COL_OFFSET_NOM = 2;
	private final static int COL_OFFSET_VOTES = 4;
	private final static int COL_OFFSET_SUIVANT = 7;
	
	public static int getInt(Row row, int col) {
		return Integer.parseInt(get(row, col));
	}
	
	public static String get(Row row, int col) {
		Cell cell = row.getCell(col);
		if (cell == null) {
			return null;
		}

		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_NUMERIC:
			double d = cell.getNumericCellValue();
			return Integer.toString((int)d);
		case Cell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		default:
			return null;
		}
	}
	
	public static void main(String[] args) {
		Map<String, Map<String, Integer>> data = new HashMap<String, Map<String, Integer>>();
		
		try {
			File f = new File(args[0]);
			FileInputStream fis = new FileInputStream(f);
			Workbook workbook = WorkbookFactory.create(fis);
			Sheet sheet = workbook.getSheetAt(SHEET_NUMBER);
			
			Iterator<Row> rowIterator = sheet.rowIterator();
			for (int i = 0; i < FIRST_DATA_LINE; i++) {
				rowIterator.next();
			}
			
			Set<String> candidats = new TreeSet<String>();
			
			while (rowIterator.hasNext()) {
				Row line = rowIterator.next();
				String code = ElectionUtils.getCircoCode(get(line, COL_DEP), get(line, COL_CIRCO));
				System.out.println(code);
				Map<String, Integer> circoData = new HashMap<String, Integer>();
				circoData.put("INSCRITS", getInt(line, COL_INSCRITS));
				circoData.put("VOTANTS", getInt(line, COL_VOTANTS));
				circoData.put("BLANCS", getInt(line, COL_BLANCS));
				circoData.put("NULS", getInt(line, COL_NULS));
				for (int c = 0; c < NB_CANDIDATS; c++) {
					String nom = get(line, COL_PREMIER_CANDIDAT + c * COL_OFFSET_SUIVANT + COL_OFFSET_NOM);
					int votes = getInt(line, COL_PREMIER_CANDIDAT + c * COL_OFFSET_SUIVANT + COL_OFFSET_VOTES);
					circoData.put(nom, votes);
					candidats.add(nom);
				}
				
				data.put(code, circoData);
			}
			
			workbook.close();
			
			Workbook export = new HSSFWorkbook();
			Sheet exportSheet = export.createSheet("1er tour présidentielle 2017");
			
			Row header = exportSheet.createRow(0);
			header.createCell(0).setCellValue("Circo");
			header.createCell(1).setCellValue("Inscrits");
			header.createCell(2).setCellValue("Votants");
			header.createCell(3).setCellValue("Blancs");
			header.createCell(4).setCellValue("Nuls");
			int idx = 5;
			for (String candidat : candidats) {
				header.createCell(idx).setCellValue(candidat);
				idx++;
			}
			
			int rowIdx = 1;
			for (Entry<String, Map<String, Integer>> e : data.entrySet()) {
				Row dataRow = exportSheet.createRow(rowIdx);
				dataRow.createCell(0).setCellValue(e.getKey());
				dataRow.createCell(1).setCellValue(e.getValue().get("INSCRITS"));
				dataRow.createCell(2).setCellValue(e.getValue().get("VOTANTS"));
				dataRow.createCell(3).setCellValue(e.getValue().get("BLANCS"));
				dataRow.createCell(4).setCellValue(e.getValue().get("NULS"));
				int colIdx = 5;
				for (String candidat : candidats) {
					dataRow.createCell(colIdx).setCellValue(e.getValue().get(candidat));
					colIdx++;
				}
				rowIdx++;
			}
			
			File exportFile = new File(f.getParentFile(), "1er_tour_presidentielle_2017_par_circo.xls");
			export.write(new FileOutputStream(exportFile));
			export.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
