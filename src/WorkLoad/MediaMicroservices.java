package WorkLoad;

import java.util.Set;

import Experiment.Http;

public class MediaMicroservices extends ExperimentSubject {
	static String dashboard_port = "8080";
	static String prefix = "http://localhost:" + dashboard_port + "/wrk2-api/";
	Workload[] workloads = {
			//cast_info_id是key
			new Workload("POST", prefix + "cast-info/write", "{\"cast_info_id\":\"cast_info_id_1\", \"name\":\"Kamran Usluer\", \"gender\":\"True\",\"intro\":\"Born in 1937 in İzmir.\"}", ""),
			//movie_id是key
			new Workload("POST", prefix + "movie-info/write", "{\"movie_id\": \"movie_id_2\", \"title\": \"title_1\", \"plot_id\": 1, \"casts\": [{\"cast_id\": 1, \"character\": \"character_1\", \"cast_info_id\": 1}], \"thumbnail_ids\": [], \"photo_ids\": [], \"video_ids\": [], \"avg_rating\": 8.6, \"num_rating\": 4789}", ""),
			//plot_id是key
			new Workload("POST", prefix + "plot/write", "{\"plot_id\": 1, \"plot\": \"plot_1\"}", ""),
			//title和movie_id是key
			new Workload("POST", prefix + "movie/register", "title=title_1&movie_id=movie_id_2", ""),
			//username是key
			new Workload("POST", prefix + "user/register", "first_name=first_name_1&last_name=last_name_1&username=username_1&password=password_1", ""),
			//无key
			new Workload("POST", prefix + "review/compose", "text=text_1&username=username_1&password=password_1&rating=5&title=title_1", ""),
	};

	public MediaMicroservices() {
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
		ExperimentSubject subject = new MediaMicroservices();
		if (maxScale == 0)
			System.out.println(subject.GenerateWorkload());
		else
			System.out.println(subject.GenerateScale(maxScale));
	}
}
