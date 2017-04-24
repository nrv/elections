package name.herve.elections;

import java.text.DecimalFormat;

public class ElectionUtils {
	public final static DecimalFormat D00 = new DecimalFormat("00");
	
	public static String getCircoCode(String dep, String circo) {
		try {
			int iDep = Integer.parseInt(dep);
			dep = D00.format(iDep);
		} catch (NumberFormatException e) {
		}
		
		try {
			int iCirco = Integer.parseInt(circo);
			circo = D00.format(iCirco);
		} catch (NumberFormatException e) {
		}
		
		return dep + "-" + circo;
	}
}
