package name.herve.results;

public class GetAllData {
	
	public static void main(String[] args) {
		DataGrabber gb = new DataGrabber();
		gb.loadCandidats();
//		gb.setRoot("https://www.interieur.gouv.fr/avotreservice/elections/telechargements/EssaiLG2017/");
		for (String dep : DataGrabber.DEPARTEMENTS) {
//			gb.processDepartement(dep);
			gb.processParCommune(dep);
		}
		
//		gb.processParCommune("022");
//		gb.processParCommune("035");
		gb.unloadCandidats();
	}

}
