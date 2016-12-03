package name.herve.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataSchema implements Iterable<String> {
	private List<String> orderedFields;

	public DataSchema() {
		super();

		orderedFields = new ArrayList<String>();
	}

	public void addField(String df) {
		orderedFields.add(df);
	}

	@Override
	public Iterator<String> iterator() {
		return orderedFields.iterator();
	}
}
