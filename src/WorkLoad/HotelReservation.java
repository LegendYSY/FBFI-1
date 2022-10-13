package WorkLoad;

import java.util.Set;

import Experiment.Http;

public class HotelReservation extends ExperimentSubject {
	static String dashboard_port = "5000";
	static String prefix = "http://localhost:" + dashboard_port + "/";
	Workload[] workloads = {
			new Workload("GET", prefix + "hotels?inDate=2015-04-20&outDate=2015-04-22&lat=37.831&lon=-121.932", "", ""),
			new Workload("GET", prefix + "recommendations?require=price&lat=38.246&lon=-122.186", "", ""),
			new Workload("GET", prefix + "user?username=Cornell_216&password=216216216216216216216216216216", "", ""),
			new Workload("GET", prefix + "reservation?inDate=2015-04-16&outDate=2015-04-20&lat=nil&lon=nil&hotelId=34&customerName=Cornell_170&username=Cornell_170&password=170170170170170170170170170170&number=1", "", ""), };

	public HotelReservation() {
		this.jaeger_port = "16686";
		this.jaeger_service = "hotel_reserv_jaeger";
		String[] services = { "geo","profile","rate","recommendation","reservation","search","user" };
		this.services = services;
	}

	@Override
	public boolean GenerateWorkload() throws Exception {
		for (Workload workload : workloads) {
			Http http = new Http(workload.type, workload.URL, workload.parameter, "application/x-www-form-urlencoded","");
			if (http.success == false)
				return false;
		}
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		int maxScale = Integer.valueOf(args[0]);
		ExperimentSubject subject = new HotelReservation();
		if (maxScale == 0)
			System.out.println(subject.GenerateWorkload());
		else
			System.out.println(subject.GenerateScale(maxScale));
	}
}
