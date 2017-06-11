package name.herve.results;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DataGrabber {
	public final static String SEP = "; ";
	public final static String EXPRIME = "---exprimes";
	public final static String DIR_RESULTATS_T1 = "resultatsT1/";
	public final static String[] DEPARTEMENTS = { "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011", "012", "013", "014", "015", "016", "017", "018", "019", "02A", "02B", "021", "022", "023", "024", "025", "026", "027", "028", "029", "030", "031", "032", "033", "034", "035", "036", "037", "038", "039", "040", "041", "042", "043", "044", "045", "046", "047", "048", "049", "050", "051", "052", "053", "054", "055", "056", "057", "058", "059", "060", "061", "062", "063", "064", "065", "066", "067", "068", "069", "070", "071", "072", "073", "074", "075", "076", "077", "078", "079", "080", "081", "082", "083", "084", "085", "086", "087", "088", "089", "090", "091", "092", "093", "094", "095", "099", "971", "972", "973", "974", "975", "976", "977", "986", "987", "988" };

	private final static Logger logger = Logger.getLogger(DataGrabber.class.getName());
	
	private final static DecimalFormat DF = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRENCH));

	private String cache;
	private String root;
	private String candidatsFile;
	private HttpClient httpclient;
	private Map<String, String> mesCandidats;
	private LevenshteinDistance d = new LevenshteinDistance();
	private FileWriter fw2;

	public DataGrabber() {
		super();
		cache = "/rex/local/elections";
		root = "http://elections.interieur.gouv.fr/telechargements/LG2017/";
		candidatsFile = "/rex/local/elections/candidats.csv";

		initHTTPClient();
	}

	private File getCacheFile(URI url) {
		String host = url.getHost();
		String path = url.getPath();

		return new File(cache, host + "/" + path + "fff---" + url.toString().hashCode() + ".cache");
	}

	public String getCandidatsFile() {
		return candidatsFile;
	}

	private String getContent(URI url, boolean force) {
		HttpResponse response = launchRequest(url, force);

		if (response != null) {
			HttpEntity entity = response.getEntity();

			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode != HttpStatus.SC_OK) {
				logger.warning("Status code " + responseCode + " for " + url);
				return null;
			} else {
				logger.info("Status code " + responseCode + " for " + url);
			}

			try {
				if (entity == null) {
					throw new RuntimeException("Passed argument entity is " + entity);
				}
				if (url.getPath().endsWith(".gz")) {
					try {
						return EntityUtils.toString(new InputStreamEntity(new GZIPInputStream(entity.getContent())));
					} catch (ZipException e) {
						return EntityUtils.toString(entity, "UTF-8");
					}
				} else {
					return EntityUtils.toString(entity, "UTF-8");
				}
			} catch (ParseException | IOException e) {
				throw new RuntimeException(e);
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}

		return null;
	}

	private void initHTTPClient() {
		HttpClientBuilder builder = HttpClientBuilder.create();

		builder.setMaxConnTotal(200);
		builder.setMaxConnPerRoute(50);

		try {
			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {

				@Override
				public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
					return true;
				}
			}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
			builder.setSSLSocketFactory(sslsf);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.severe(e.getClass().getName() + " : " + e.getMessage());
		}

		httpclient = builder.build();
	}
	
	private HttpResponse launchRequest(URI url, boolean force) {
		if (cache != null && !force) {
			File cacheFile = getCacheFile(url);
			if (cacheFile.exists()) {
				logger.finest("Getting from cache " + url);
				BasicHttpResponse r = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), HttpStatus.SC_OK, "OK"));
				BasicHttpEntity e = new BasicHttpEntity();
				try {
					e.setContent(new FileInputStream(cacheFile));
				} catch (FileNotFoundException e1) {
					logger.severe("Cache bad path : " + e1.getMessage());
				}
				r.setEntity(e);
				return r;
			}
		}

		HttpGet httpget = null;
		try {
			httpget = new HttpGet(url);
		} catch (IllegalArgumentException e) {
			logger.severe("Bad path : " + e.getMessage());
			return null;
		}
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			if (cache != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				logger.info("Writing to cache " + url);
				File cacheFile = getCacheFile(url);
				cacheFile.getParentFile().mkdirs();
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				FileUtils.copyInputStreamToFile(is, cacheFile);
				is.close();

				return launchRequest(url, false);
			}
		} catch (IOException e) {
			logger.severe("Error in request : " + e.getMessage());
			return null;
		}
		return response;
	}
	
	public String getMonCandidat(String circo, Collection<String> candidats) {
		String ref = mesCandidats.get(circo);
		if (ref == null) {
			return null;
		}
		int min = Integer.MAX_VALUE;
		String res = null;
		for (String potential : candidats) {
			int dist = d.apply(ref, potential);
			if (dist < min) {
				min = dist;
				res = potential;
			}
		}
		
		return res;
	}

	public void loadCandidats() {
		mesCandidats = new HashMap<String, String>();
		try {
			Iterator<CSVRecord> it = CSVFormat.EXCEL.withDelimiter(',').parse(new FileReader(candidatsFile)).iterator();
			while (it.hasNext()) {
				CSVRecord record = it.next();
				String circo = record.get(0);
				String nom = record.get(1);
				mesCandidats.put(circo, nom);
			}
			
			fw2 = new FileWriter(new File("/tmp/all.csv"));
			fw2.write("circoCode" + SEP + "exprimes" + SEP + "voix" + SEP + "nom" + SEP + "monCandidat\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void unloadCandidats() {
		try {
			fw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void processDepartement(String dep) {
		String resultatsURL = root + DIR_RESULTATS_T1 + dep + "/" + dep + "CIR.xml";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			XPathFactory xpf = XPathFactory.newInstance();
			XPath path = xpf.newXPath();

			String resultats = getContent(new URI(resultatsURL), false);
			Document xml = builder.parse(new ByteArrayInputStream(resultats.getBytes()));

			String ELEC_DEP = "/Election/Departement/";
			String codeDep = path.evaluate(ELEC_DEP + "CodDpt", xml);
			String nomDep = path.evaluate(ELEC_DEP + "LibDpt", xml);
			logger.info("Working on " + codeDep + " - " + nomDep);

			String ELEC_DEP_CIRC = ELEC_DEP + "Circonscriptions/Circonscription";
			NodeList circos = (NodeList) path.evaluate(ELEC_DEP_CIRC + "/CodCirLg", xml, XPathConstants.NODESET);
			for (int i = 0; i < circos.getLength(); i++) {
				Node circo = circos.item(i);
				String circoCode = circo.getTextContent();
				String TOUR = ELEC_DEP_CIRC + "[CodCirLg='" + circoCode + "']/Tours/Tour[NumTour=1]/";
				String MENTIONS = TOUR + "Mentions/";
				Number inscrits = (Number) path.evaluate(MENTIONS + "Inscrits/Nombre", xml, XPathConstants.NUMBER);
				Number abstentions = (Number) path.evaluate(MENTIONS + "Abstentions/Nombre", xml, XPathConstants.NUMBER);
				Number votants = (Number) path.evaluate(MENTIONS + "Votants/Nombre", xml, XPathConstants.NUMBER);
				Number blancs = (Number) path.evaluate(MENTIONS + "Blancs/Nombre", xml, XPathConstants.NUMBER);
				Number nuls = (Number) path.evaluate(MENTIONS + "Nuls/Nombre", xml, XPathConstants.NUMBER);
				Number exprimes = (Number) path.evaluate(MENTIONS + "Exprimes/Nombre", xml, XPathConstants.NUMBER);

				String CANDIDAT = TOUR + "Resultats/Candidats/Candidat";
				NodeList panneaux = (NodeList) path.evaluate(CANDIDAT + "/NumPanneauCand", xml, XPathConstants.NODESET);
				for (int j = 0; j < panneaux.getLength(); j++) {
					Node panneau = panneaux.item(j);
					String panneauCode = panneau.getTextContent();
					String CE_CANDIDAT = CANDIDAT + "[NumPanneauCand='" + panneauCode + "']/";

					String prenom = (String) path.evaluate(CE_CANDIDAT + "PrenomPsn", xml, XPathConstants.STRING);
					String nom = (String) path.evaluate(CE_CANDIDAT + "NomPsn", xml, XPathConstants.STRING);
					String civilite = (String) path.evaluate(CE_CANDIDAT + "CivilitePsn", xml, XPathConstants.STRING);
					String nuance = (String) path.evaluate(CE_CANDIDAT + "LibNua", xml, XPathConstants.STRING);
					Number voix = (Number) path.evaluate(CE_CANDIDAT + "NbVoix", xml, XPathConstants.NUMBER);
					String pctExprime = (String) path.evaluate(CE_CANDIDAT + "RapportExprime", xml, XPathConstants.STRING);
					String pcttInscrit = (String) path.evaluate(CE_CANDIDAT + "RapportInscrit", xml, XPathConstants.STRING);
					String elu = (String) path.evaluate(CE_CANDIDAT + "Elu", xml, XPathConstants.STRING);

					double pe = Double.parseDouble(pctExprime.trim().replace(",", "."));
					double pi = Double.parseDouble(pcttInscrit.trim().replace(",", "."));

					System.out.println(codeDep + SEP + circoCode + SEP + inscrits + SEP + exprimes + SEP + voix + SEP + pe + SEP + pi + SEP + elu + SEP + civilite + " " + prenom + " " + nom + SEP + nuance);
				}

			}
		} catch (URISyntaxException e) {
			logger.severe("Bad URL : " + e.getMessage());
		} catch (ParserConfigurationException | IOException | SAXException | XPathException e) {
			logger.severe("Bad XML : " + e.getMessage());
		}
	}

	public void processParCommune(String dep) {
		try {
			FileWriter fw = new FileWriter(new File("/tmp/" + dep + ".csv"));
			fw.write("circoCode" + SEP + "nomCommune" + SEP + "inscrits" + SEP + "exprimes" + SEP + "voix" + SEP + "pe" + SEP + "pi" + SEP + "elu" + SEP + "nom" + SEP + "nuance\n");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			XPathFactory xpf = XPathFactory.newInstance();
			XPath path = xpf.newXPath();

			String resultatsURL = root + DIR_RESULTATS_T1 + dep + "/";
			String resultats = getContent(new URI(resultatsURL), true);

			Map<String, Map<String, Integer>> res = new HashMap<>();
			
			org.jsoup.nodes.Document doc = Jsoup.parse(resultats, "UTF-8");
			Elements elements = doc.select("a");
			for (Element element : elements) {
				String href = element.attr("href");
				if (href.toLowerCase().endsWith(".xml")) {
					String data = getContent(new URI(resultatsURL + href), false);
					Document xml = builder.parse(new ByteArrayInputStream(data.getBytes()));

					String ELEC_DEP = "/Election/Departement/";
					String codeDep = path.evaluate(ELEC_DEP + "CodDpt", xml);
					String nomDep = path.evaluate(ELEC_DEP + "LibDpt", xml);

					String ELEC_DEP_COM = ELEC_DEP + "Commune/";
					String nomCommune = path.evaluate(ELEC_DEP_COM + "LibSubCom", xml);
					String circoCode = path.evaluate(ELEC_DEP_COM + "CodCirLg", xml);
					

					if (circoCode != null && !circoCode.isEmpty()) {
						String fullCode = codeDep + "-" + circoCode;
						
						if (!res.containsKey(fullCode)) {
							res.put(fullCode, new HashMap<String, Integer>());
						}
						
						Map<String, Integer> circoRes = res.get(fullCode);

						logger.info("Working on " + codeDep + " - " + nomDep + " - " + fullCode + " - " + nomCommune);

						String TOUR = ELEC_DEP_COM + "/Tours/Tour[NumTour=1]/";
						String MENTIONS = TOUR + "Mentions/";
						Number inscrits = (Number) path.evaluate(MENTIONS + "Inscrits/Nombre", xml, XPathConstants.NUMBER);
						Number abstentions = (Number) path.evaluate(MENTIONS + "Abstentions/Nombre", xml, XPathConstants.NUMBER);
						Number votants = (Number) path.evaluate(MENTIONS + "Votants/Nombre", xml, XPathConstants.NUMBER);
						Number blancs = (Number) path.evaluate(MENTIONS + "Blancs/Nombre", xml, XPathConstants.NUMBER);
						Number nuls = (Number) path.evaluate(MENTIONS + "Nuls/Nombre", xml, XPathConstants.NUMBER);
						Number exprimes = (Number) path.evaluate(MENTIONS + "Exprimes/Nombre", xml, XPathConstants.NUMBER);
						
						if (!circoRes.containsKey(EXPRIME)) {
							circoRes.put(EXPRIME, 0);
						}
						circoRes.put(EXPRIME, circoRes.get(EXPRIME) + exprimes.intValue());

						String CANDIDAT = TOUR + "Resultats/Candidats/Candidat";
						NodeList panneaux = (NodeList) path.evaluate(CANDIDAT + "/NumPanneauCand", xml, XPathConstants.NODESET);
						for (int j = 0; j < panneaux.getLength(); j++) {
							Node panneau = panneaux.item(j);
							String panneauCode = panneau.getTextContent();
							String CE_CANDIDAT = CANDIDAT + "[NumPanneauCand='" + panneauCode + "']/";

							String prenom = (String) path.evaluate(CE_CANDIDAT + "PrenomPsn", xml, XPathConstants.STRING);
							String nom = (String) path.evaluate(CE_CANDIDAT + "NomPsn", xml, XPathConstants.STRING);
							String fullNom = prenom + " " + nom;
							String civilite = (String) path.evaluate(CE_CANDIDAT + "CivilitePsn", xml, XPathConstants.STRING);
							String nuance = (String) path.evaluate(CE_CANDIDAT + "LibNua", xml, XPathConstants.STRING);
							Number voix = (Number) path.evaluate(CE_CANDIDAT + "NbVoix", xml, XPathConstants.NUMBER);
							
							if (!circoRes.containsKey(fullNom)) {
								circoRes.put(fullNom, 0);
							}
							circoRes.put(fullNom, circoRes.get(fullNom) + voix.intValue());
							
							String pctExprime = (String) path.evaluate(CE_CANDIDAT + "RapportExprime", xml, XPathConstants.STRING);
							String pcttInscrit = (String) path.evaluate(CE_CANDIDAT + "RapportInscrit", xml, XPathConstants.STRING);
							String elu = (String) path.evaluate(CE_CANDIDAT + "Elu", xml, XPathConstants.STRING);

							double pe = Double.parseDouble(pctExprime.trim().replace(",", "."));
							double pi = Double.parseDouble(pcttInscrit.trim().replace(",", "."));

//							System.out.println(codeDep + SEP + circoCode + SEP + nomCommune + SEP + inscrits + SEP + exprimes + SEP + voix + SEP + pe + SEP + pi + SEP + elu + SEP + civilite + " " + prenom + " " + nom + SEP + nuance);
							fw.write(fullCode + SEP + nomCommune + SEP + inscrits.intValue() + SEP + exprimes.intValue() + SEP + voix.intValue() + SEP + DF.format(pe) + SEP + DF.format(pi) + SEP + elu + SEP + fullNom + SEP + nuance + "\n");
						}
					}
				}
			}
			fw.close();
			
			
			for (Entry<String, Map<String, Integer>> e : res.entrySet()) {
				String circo = e.getKey();
				Map<String, Integer> resc = e.getValue();
				String monCandidat = getMonCandidat(circo, resc.keySet());
				int exp = resc.get(EXPRIME);
				for (Entry<String, Integer> e2 : resc.entrySet()) {
					if (!e2.getKey().equals(EXPRIME)) {
						fw2.write(circo + SEP + exp + SEP + e2.getValue() + SEP + e2.getKey() + SEP  + e2.getKey().equals(monCandidat)+ "\n");
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Error : " + e.getMessage());
		}
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public void setCandidatsFile(String candidatsFile) {
		this.candidatsFile = candidatsFile;
	}

	public void setRoot(String root) {
		this.root = root;
	}
}
