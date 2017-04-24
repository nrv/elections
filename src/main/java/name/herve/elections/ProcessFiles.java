package name.herve.elections;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import name.herve.data.CSVDataFileParser;
import name.herve.data.Data;
import name.herve.data.DataSchema;

public class ProcessFiles {

	public static void main(String[] args) {
		Map<String, String> circoLabel = new HashMap<String, String>();
		
		try {
			String dataDir = args[0];

			File circoCommunes = new File(dataDir, "circo_communes.csv");
			DataSchema s1 = new DataSchema();
			s1.addField("code-canton");
			s1.addField("dep");
			s1.addField("dep-lbl");
			s1.addField("canton");
			s1.addField("canton-lbl");
			s1.addField("code-commune");
			s1.addField("commune-lbl");
			s1.addField("1986");
			s1.addField("2012");
			CSVDataFileParser p1 = new CSVDataFileParser(s1, circoCommunes, ',');
			p1.openDataStream();
			Data data = null;
			Map<String, Set<String>> circoCanton = new HashMap<String, Set<String>>();
			while ((data = p1.nextData()) != null) {
				String circo = ElectionUtils.getCircoCode(data.get("dep"), data.get("2012"));
				if (!circoCanton.containsKey(circo)) {
					circoCanton.put(circo, new HashSet<String>());
				}
				String cantonlbl = data.get("dep").equals("75") ? data.get("commune-lbl") : data.get("canton-lbl");
				circoCanton.get(circo).add(cantonlbl);
			}
			
			for (Entry<String, Set<String>> e : circoCanton.entrySet()) {
				StringBuilder sb = new StringBuilder();
				for (String canton : e.getValue()) { 
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(canton);
				}
				circoLabel.put(e.getKey(), sb.toString());
			}
			p1.closeDataStream();
			
			
			DataSchema s2 = new DataSchema();
			s2.addField("region");
			s2.addField("dep-code");
			s2.addField("dep");
			s2.addField("Circo");
			s2.addField("Député-2012");
			s2.addField("EURO-14");
			s2.addField("REG-15");
			s2.addField("PRES-12");
			s2.addField("2012");
			s2.addField("Strategie");
			s2.addField("Candidat");
			s2.addField("RISQUE");
			File tableau = new File(dataDir, "TableauCirco-Regions.csv");
			CSVDataFileParser p2 = new CSVDataFileParser(s2, tableau, ';');
			p2.openDataStream();
			List<Data> datas = new ArrayList<Data>();
			while ((data = p2.nextData()) != null) {
				String circo = ElectionUtils.getCircoCode(data.get("dep-code"), data.get("Circo"));
				data.put("circo-code", circo);
				data.put("circo-cantons", circoLabel.get(circo));
				
				String depute = data.get("Député-2012");
				int idx = depute.lastIndexOf(" ");
				String groupe = depute.substring(idx).trim();
				depute = depute.substring(0, idx);
				data.put("Député-2012", depute);
				data.put("Groupe-2012", groupe);
				
				datas.add(data);
			}
			p2.closeDataStream();
			
			DataSchema s3 = new DataSchema();
			s3.addField("region");
			s3.addField("dep-code");
			s3.addField("dep");
			s3.addField("Circo");
			s3.addField("Député-2012");
			s3.addField("Groupe-2012");
			s3.addField("EURO-14");
			s3.addField("REG-15");
			s3.addField("PRES-12");
			s3.addField("2012");
			s3.addField("Strategie");
			s3.addField("Candidat");
			s3.addField("RISQUE");
			s3.addField("circo-code");
			s3.addField("circo-cantons");
			
			FileWriter w = new FileWriter(new File(dataDir, "output.csv"));
			for (String f : s3) {
				w.write(f);
				w.write("; "); 
			}
			w.write("\n");
			for (Data d : datas) {
				for (String f : s3) {
					String s = d.get(f);
					w.write(s != null ? s : "");
					w.write("; "); 
				}
				w.write("\n");
			}
			
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
