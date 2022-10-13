package Experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import MainFBFI.Graph;
import MainFBFI.IO;
import MainFBFI.Layer;
import MainFBFI.Node;
import WorkLoad.ExperimentSubject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class Path_Trace {
	ExperimentSubject subject;
	public Graph graph;
	Set<Trace> graphTraces = new TreeSet<>();

	Path_Trace(ExperimentSubject subject) {
		this.subject = subject;
	}

	Set<Trace> Jaeger_Query(String service_name, String operation) throws Exception {
		long end = System.currentTimeMillis();
		long start = end - 60 * 60 * 1000;
		String URL = ("http://localhost:" + subject.jaeger_port + "/api/traces?end=" + (end * 1000)
				+ "&limit=1&lookback=1h&maxDuration&minDuration"
				+ (operation.equals("") ? "" : ("&operation=" + operation)) + "&service=" + service_name + "&start="
				+ (start * 1000)).replaceAll(" ", "%20");
		System.out.println("ServiceQuery: " + URL);
		JSONObject response = new Http("GET", URL, "", "application/json", "").response;
		if (response == null)
			throw new Experiment_Exception(
					"< Error: Jaeger responded null when querying " + service_name + "." + operation + " ! >");
		JSONArray trace_data = response.getJSONArray("data");
		Set<Trace> traces = new HashSet<>();
		Set<String> tracesID = new HashSet<>();
		int limit_number = operation.equals("") ? trace_data.size() : 1;
		for (int trace_index = 0; trace_index < limit_number; trace_index++) {
			String traceID = trace_data.getJSONObject(trace_index).getString("traceID");
			if (tracesID.contains(traceID))
				continue;
			tracesID.add(traceID);
			// 解析processes
			JSONObject json_processes = trace_data.getJSONObject(trace_index).getJSONObject("processes");
			Map<String, String> processMap = new HashMap<>();
			for (int i = 1; i <= json_processes.size(); i++) {
				String processID = "p" + Integer.toString(i);
				JSONObject p = json_processes.getJSONObject(processID);
				processMap.put(processID,
						p.getString("serviceName") + "~~" + p.getJSONArray("tags").getJSONObject(0).getString("value"));
			}

			// 解析spans
			JSONArray json_spans = trace_data.getJSONObject(trace_index).getJSONArray("spans");
			List<Span> spans = new ArrayList<>();
			Set<String> spanIDs = new HashSet<>();
			for (int i = 0; i < json_spans.size(); i++) {
				JSONObject json_span = json_spans.getJSONObject(i);
				String spanID = json_span.getString("spanID");
				if (spanIDs.contains(spanID))
					continue;
				spanIDs.add(spanID);
				String processID = json_span.getString("processID");
				String reference_spanID = "/";
				String operationName = "/";
				// 不是源头
				if (json_span.getJSONArray("references").size() > 0)
					reference_spanID = json_span.getJSONArray("references").getJSONObject(0).getString("spanID");
				// 是源头
				else
					operationName = json_span.getString("operationName");
				long startTime = json_span.getLong("startTime");
				spans.add(new Span(traceID, spanID, reference_spanID, operationName, processMap.get(processID),
						startTime));
			}
			// spans按startTime排序
			Collections.sort(spans);
			if (spans.get(0).operationName.equals("/"))
				continue;
			traces.add(new Trace(spans, processMap));
		}
		return traces;
	}
	void InitGraphs_Simulate() {
		System.out.println("Initing: SUT runs without fault.");
		System.out.println("Please input a successful execution path (e.g. A_1-B_1-C_1):");
		Scanner scanner = new Scanner(System.in);
		List<String> path = new ArrayList<String>(Arrays.asList(scanner.nextLine().split("-")));
		System.out.println(path);
		Map<String, Node> nodes = new HashMap<>();
		Map<String, Layer> layers = new HashMap<>();
		List<String> layersName = new ArrayList<>();
		int layerID = 0;
		for(String nodeID:path) {
			String serviceName = nodeID.split("_")[0];
			layers.put(serviceName, new Layer(++layerID, serviceName, new HashSet<String>() {
				{
					add(nodeID);
				}
			}));
			nodes.put(nodeID, new Node(nodeID, layerID, serviceName));
			layersName.add(serviceName);
		}
		nodes.put("start", new Node("start", 0, "START LAYER"));
		nodes.put("end", new Node("end", ++layerID, "END LAYER"));
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		graph = new Graph(nodes, layers, layersName, -1);
		graph.PrintGraph(false);
	}
	void InitGraphs() throws Exception {
		System.out.println("~ ~ ~ ~ ~ ~ ~ ~ ~ ~ Generating Workload ~ ~ ~ ~ ~ ~ ~ ~ ~ ~");
		if (subject.GenerateWorkload() == false)
			throw new Experiment_Exception("< Error: System is unavailable! >");
		// 给Jaeger一定的缓冲时间
		Thread.sleep(20 * 1000);
		String URL = "http://localhost:" + subject.jaeger_port + "/api/services";
		JSONObject response = new Http("GET", URL, "", "application/json", "").response;
		if (response == null)
			throw new Experiment_Exception("< Error: Jaeger's response is null when init graphs! >");
		JSONArray service_name_data;
		try {
			service_name_data = JSONObject.fromObject(response).getJSONArray("data");
		} catch (Exception e) {
			throw new Experiment_Exception("< Error: Jaeger could not find any services! >");
		}

		IO.Output("\n[ Covered " + service_name_data.size() + " Services ]");
		for (int service_index = 0; service_index < service_name_data.size(); service_index++) {
			String service_name = service_name_data.getString(service_index);
			IO.Output(service_name);
			graphTraces.addAll(Jaeger_Query(service_name, ""));
		}
		Map<String, Node> nodes = new HashMap<>();
		Map<String, Layer> layers = new HashMap<>();
		List<String> layersName = new ArrayList<>();
		List<String> path = new ArrayList<String>();
		int layerID = 0;
		IO.Output("\n[ Trace List ]");
		for (Trace trace : graphTraces) {
			IO.Output("[ " + trace.headServiceName + "." + trace.operation + " ]");
			trace.PrintTrace();
			for (Span span : trace.spans) {
				String serviceName = span.serviceName_nodeID.split("~~")[0];
				if (!layers.containsKey(serviceName)) {
					String nodeID = span.serviceName_nodeID;
					layers.put(serviceName, new Layer(++layerID, serviceName, new HashSet<String>() {
						{
							add(nodeID);
						}
					}));
					nodes.put(nodeID, new Node(nodeID, layerID, serviceName));
					layersName.add(serviceName);
					path.add(nodeID);
				}
			}
		}
		nodes.put("start", new Node("start", 0, "START LAYER"));
		nodes.put("end", new Node("end", ++layerID, "END LAYER"));
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		graph = new Graph(nodes, layers, layersName, -1);
		graph.PrintGraph(false);
	}

	List<String> TracePath() throws Exception {
		Map<Integer, String> nodeIDs = new TreeMap<>();// 自动按key从小到大排序
		Set<String> tracesID = new HashSet<>();
		for (Trace graphTrace : graphTraces) {
			Set<Trace> traces = Jaeger_Query(graphTrace.headServiceName, graphTrace.operation);
			for (Trace trace : traces) {
				if (tracesID.contains(trace.traceID))
					continue;
				tracesID.add(trace.traceID);
				trace.PrintTrace();
				for (String nodeID : trace.processMap.values()) {
					String serviceName = nodeID.split("~~")[0];
					// 出现未见过的serviceName
					if (!graph.layers.containsKey(serviceName)) {
						IO.Output("< Warning: serviceName \" " + serviceName + " \" is unseen! >");
						return new ArrayList<String>();
					}
					int layerID = graph.layers.get(serviceName).layerID;
					if (!nodeIDs.containsKey(layerID)) {
						graph.layers.get(serviceName).layerNodes.add(nodeID);
						graph.nodes.putIfAbsent(nodeID, new Node(nodeID, layerID, serviceName));
						nodeIDs.put(layerID, nodeID);
					}
				}
			}
		}
		List<String> path = new ArrayList<String>(nodeIDs.values());
		return path;
	}
	public static void main(String[] args) {
		
	
	}
}
