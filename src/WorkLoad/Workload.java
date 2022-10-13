package WorkLoad;

public class Workload {
	String type;
	String URL;
	String parameter;
	String needResponse;
	Workload(String type, String URL, String parameter,String needResponse) {
		this.type = type;
		this.URL = URL;
		this.parameter = parameter;
		this.needResponse=needResponse;
	}
}
