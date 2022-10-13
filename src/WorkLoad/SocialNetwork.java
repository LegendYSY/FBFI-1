package WorkLoad;

import java.util.Set;

import Experiment.Http;

public class SocialNetwork extends ExperimentSubject {
	static String dashboard_port = "8080";
	static String prefix = "http://localhost:" + dashboard_port + "/wrk2-api/";
	Workload[] workloads = { 
			new Workload("GET", prefix + "home-timeline/read?user_id=001&start=33&stop=43", "", ""),
			new Workload("GET", prefix + "user-timeline/read?user_id=001&start=33&stop=43", "", ""),
			new Workload("POST", prefix + "post/compose",
					"username=username_001&user_id=001&text=hello&media_ids=[\"300099915097524593\",\"097428690469598587\"]&media_types=[\"png\",\"png\"]&post_type=0",
					"") };

	public SocialNetwork() {
		this.jaeger_port = "16687";
		this.jaeger_service = "hotel_reserv_jaeger";
	}

	@Override
	public boolean GenerateWorkload() throws Exception {
		for (Workload workload : workloads) {
			Http http = new Http(workload.type, workload.URL, workload.parameter, "application/x-www-form-urlencoded", "");
			if (http.success == false)
				return false;
		}
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		int maxScale = Integer.valueOf(args[0]);
		ExperimentSubject subject = new SocialNetwork();
		if (maxScale == 0)
			System.out.println(subject.GenerateWorkload());
		else
			System.out.println(subject.GenerateScale(maxScale));
	}
}
