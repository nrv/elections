package name.herve.data;

import java.util.HashMap;
import java.util.Map;

public class Data {
	private Map<String, String> values;
	private long lineNumber;

	public Data() {
		super();
		values = new HashMap<String, String>();
	}

	public String get(String key) {
		return values.get(key);
	}

	public long getLineNumber() {
		return lineNumber;
	}

	public Data put(String key, String value) {
		values.put(key, value);
		return this;
	}

	public Data setLineNumber(long lineNumber) {
		this.lineNumber = lineNumber;
		return this;
	}

}
