package WorkLoad;

import java.util.Set;

import Experiment.Http;

public class SimulateSubject extends ExperimentSubject {
	Workload[] workloads = {};
	public SimulateSubject() {
		String[] services = {};
		this.services = services;
	}
	@Override
	public boolean GenerateWorkload() throws Exception {
		return true;
	}
	public void ResetJaeger() throws Exception {
		return;
	}
	public static void main(String[] args) throws Exception {
	}
}
