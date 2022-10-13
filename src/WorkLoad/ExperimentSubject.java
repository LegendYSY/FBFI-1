package WorkLoad;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Experiment.Experiment_Exception;
import MainFBFI.IO;

public abstract class ExperimentSubject {

	public String jaeger_port;
	public String jaeger_service;

	public String[] services;
	public boolean firstTime = true;

	public static void Execute_Command(String command) throws Exception {
		System.out.println(command);
		Process process = Runtime.getRuntime().exec(new String[] { "sh", "-c", command });
		if (process.waitFor() == 1)
			throw new Experiment_Exception("< Error: Execute command [" + command + "] failed! >");
	}

	public String GenerateScale(int scale) {
		String scale_command = "docker-compose up";
		Random random = new Random();
		List<String> scale_list = new ArrayList<>();
		for (String service : services) {
			int random_scale = random.nextInt(2) + scale;
			scale_list.add(service + " = " + random_scale);
			scale_command += " --scale " + service + "=" + String.valueOf(random_scale);
		}
		IO.Write("./scale.txt", false, scale_list);
		return scale_command;
	}

	// δʹ�õķ���
	public void Deploy(int maxScale) throws Exception {
		System.out.println("\nCleaning Docker Environment");
		Execute_Command("docker-compose down --volumes");
		System.out.println("\nDocker Environment Cleaned");
		System.out.println("\nRandom Deploy Scale:");
		Runtime.getRuntime().exec(GenerateScale(maxScale));
		System.out.println("\nDeploy Start, please wait for about 200s ...\n");
		Thread.sleep(200 * 1000);
		System.out.println("\nDeploy Finish\n");
	}

	public void ResetJaeger() throws Exception {
		System.out.println("\nReset Jaeger");
		Execute_Command("docker restart " + jaeger_service);
		Thread.sleep(30 * 1000);
	}

	public void ResetDashboard() throws Exception {
		System.out.println("\nReset Dashboard");
		Execute_Command("docker restart train-ticket_ts-ui-dashboard_1");
		//Execute_Command("train-ticket_ts-preserve-service_1 train-ticket_ts-notification-service_1");
	}

	public void ResetSubject() throws Exception {
		System.out.println("\nReset Subject");
		Execute_Command("systemctl restart docker");
	}

	public void Inject(Set<String> config, String inject_type) throws Exception {
		if (config.isEmpty())
			return;
		switch (inject_type) {
		case "stop":
		case "pause":
			String inject_command = "docker " + inject_type;
			for (String node : config)
				inject_command += " " + node.split("~~")[1];
			Execute_Command(inject_command);
			break;
		case "net":
			for (String node : config)
				Execute_Command("nsenter -n -t $(docker inspect -f {{.State.Pid}} " + node.split("~~")[1]
						+ ") iptables -A INPUT -p tcp -j DROP");
			break;
		}
		Thread.sleep(10 * 1000);
	}

	public void Restore(Set<String> config, String inject_type) throws Exception {
		if (config.isEmpty())
			return;
		String inject_command = "";
		switch (inject_type) {
		case "stop":
			inject_command = "docker start";
			break;
		case "pause":
			inject_command = "docker unpause";
			break;
		case "restart":
			inject_command = "docker restart";
			break;
		case "net":
			for (String node : config)
				Execute_Command("nsenter -n -t $(docker inspect -f {{.State.Pid}} " + node.split("~~")[1]
						+ ") iptables -D INPUT 1");
			return;
		}
		for (String node : config)
			inject_command += " " + node.split("~~")[1];
		Execute_Command(inject_command);
	}

	public abstract boolean GenerateWorkload() throws Exception;
}
