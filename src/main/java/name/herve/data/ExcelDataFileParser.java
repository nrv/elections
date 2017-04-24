package name.herve.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class ExcelDataFileParser implements DataProvider {
	private File f;
	private FileInputStream in;
	private DataSchema schema;
	private int sheet;
	private Iterator<Row> rowIterator;
	private List<String> header;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

	public ExcelDataFileParser(DataSchema schema, int sheet, File f) {
		super();
		this.schema = schema;
		this.sheet = sheet;
		this.f = f;
	}

	@Override
	public void closeDataStream() throws IOException {
		in.close();
	}

	@Override
	public Data nextData() throws IOException {
		if (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Data data = new Data();
			data.setLineNumber(row.getRowNum());

			int colNum = 0;
			for (String field : header) {
				Cell cell = row.getCell(colNum);
				if (cell == null) {
					System.err.println("l:" + row.getRowNum() + " c:" + colNum + " (" + field + ") -> null");
					continue;
				}

				switch (cell.getCellType()) {
				case Cell.CELL_TYPE_NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						data.put(field, dateFormat.format(cell.getDateCellValue()));
					} else {
						data.put(field, Double.toString(cell.getNumericCellValue()));
					}
					break;
				case Cell.CELL_TYPE_STRING:
					data.put(field, cell.getStringCellValue());
					break;
				default:
					System.err.println("l:" + row.getRowNum() + " c:" + colNum + " -> " + cell.getCellType());
					break;
				}

				colNum++;
			}

//			if ((data.getLineNumber() % 10000) == 0) {
//				System.out.println(" ~ " + data.getLineNumber());
//			}
			return data;
		}

		return null;
	}

	@Override
	public void openDataStream() throws IOException {
		try {
			in = new FileInputStream(f);
//		XSSFWorkbook workbook = new XSSFWorkbook(in);
			org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(in);
//		XSSFSheet sheet = workbook.getSheetAt(0);
			org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
			rowIterator = sheet.iterator();
			header = new ArrayList<String>();
			if (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				Iterator<Cell> cellIterator = row.cellIterator();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					header.add(cell.getStringCellValue());
				}
			} else {
				throw new IOException("Empty Excel file, unable to parse header line for " + f);
			}
		} catch (EncryptedDocumentException e) {
			throw new IOException(e);
		} catch (InvalidFormatException e) {
			throw new IOException(e);
		}
	}

}
