package name.herve.data;

import java.io.IOException;

public interface DataProvider {
	void closeDataStream() throws IOException;

	Data nextData() throws IOException;

	void openDataStream() throws IOException;
}
