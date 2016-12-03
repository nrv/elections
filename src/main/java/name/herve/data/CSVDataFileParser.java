package name.herve.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class CSVDataFileParser implements DataProvider {
	private File f;
	private Reader in;
	private DataSchema schema;
	private Iterator<CSVRecord> records;
	private char delimiter;
	
	public CSVDataFileParser(DataSchema schema, File f, char delimiter) {
		super();
		this.schema = schema;
		this.f = f;
		this.delimiter = delimiter;
	}

	public CSVDataFileParser(DataSchema schema, File f) {
		this(schema, f, ';');
	}
	
	@Override
	public void closeDataStream() throws IOException {
		in.close();
	}

	@Override
	public Data nextData() throws IOException {
		if (records.hasNext()) {
			CSVRecord record = records.next();
			
			Data data = new Data();
			data.setLineNumber(record.getRecordNumber());
			for (String field : schema) {
				data.put(field, record.get(field));
			}
//			if (data.getLineNumber() % 10000 == 0) {
//				System.out.println(" ~ " + data.getLineNumber());
//			}
			return data;
		}
		
		return null;
	}

	@Override
	public void openDataStream() throws IOException {
		in = new FileReader(f);
		records = CSVFormat.EXCEL.withHeader().withDelimiter(delimiter).parse(in).iterator();
	}


}
