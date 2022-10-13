package MainFBFI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;

@SuppressWarnings("serial")
public class FBFI_Algorithm {
	int layer_number;
	public Map<String, Node> nodes = new HashMap<>();
	public static Set<String> nodes_explored = new LinkedHashSet<>();
	public Map<String, Set<Node>> layers = new HashMap<>();
	int changed_layer;
	Map<String, Set<Set<String>>> counted = new HashMap<>();
	Map<String, Boolean> valid = new HashMap<>();
	Set<Set<String>> equivalent_set = new LinkedHashSet<>();
	public List<Set<String>> bottlenecks = new ArrayList<>();
	public List<Set<String>> defects = new ArrayList<>();
	public List<Set<String>> redundancies = new ArrayList<>();
	static List<Set<String>> failedCases = new ArrayList<>();
	int iterator_number = 0;
	int node_number = 0;
	int path_number = 0;
	int counted_times = 0;
	int judge_times = 0;

	public Set<String> LDFI_Result = new LinkedHashSet<>();

	public FBFI_Algorithm(int layer_size[],String layer_name[]) {
		Init(layer_size,layer_name);
		this.layer_number = layer_size.length-2;
		Init_BrokeEdge();
	}

	public FBFI_Algorithm(Map<String, Node> nodes) {
		this.nodes = nodes;
	}

	public FBFI_Algorithm(Map<String, Node> nodes, Map<String, Set<Node>> layers) {
		this.nodes = nodes;
		this.layers = layers;
	}

	public FBFI_Algorithm() {
	}

	void Init(int layer_size[],String layer_name[]) {
		int service_number=layer_size.length-2;
		for (int s = 0; s <= service_number; ++s) {
			Set<String> se = new LinkedHashSet<>();
			int layer_head = node_number;
			for (int i = 0; i < layer_size[s + 1]; ++i) {
				if (s == service_number)
					se.add("end");
				else
					se.add(String.valueOf(layer_head + layer_size[s] + i));
			}
			Set<Node> layer = new LinkedHashSet<>();
			for (int b = 0; b < layer_size[s]; ++b) {
				Node node;
				if (s == 0) {
					node = new Node("start", s, new LinkedHashSet<>(se));
					nodes.put("start", node);
				}

				else {
					node = new Node(String.valueOf(node_number), s, new LinkedHashSet<>(se));
					nodes.put(String.valueOf(node_number), node);
				}
				layer.add(node);
				node_number++;
			}
			layers.put("Layer" + Integer.toString(s), layer);
		}
		Node end = new Node("end", service_number + 1, new LinkedHashSet<>());
		nodes.put("end", end);
		node_number++;
		Set<Node> end_layer = new LinkedHashSet<>();
		end_layer.add(end);
		layers.put("Layer" + Integer.toString(service_number + 1), end_layer);
	}

	void Init_BrokeEdge() {
		nodes.get("2").subNodes.remove("4");
		nodes.get("5").subNodes.remove("6");
		nodes.get("1").subNodes.remove("4");
		nodes.get("1").subNodes.remove("5");
		nodes.get("2").subNodes.remove("4");
		nodes.get("8").subNodes.remove("9");
	}

	void GenES(String node) {
		for (Set<String> es : equivalent_set)
			for (String n : es)
				if (nodes.get(node).preNodes.equals(nodes.get(n).preNodes)) {
					es.add(node);
					return;
				}
		equivalent_set.add(new LinkedHashSet<String>() {
			{
				add(node);
			}
		});
	}

	public void PathProcessing(List<String> path) {
		changed_layer = -1;
		valid = new HashMap<>();
		equivalent_set = new LinkedHashSet<>();
		String preID = "start";
		path.add("end");
		for (String nodeID : path) {
			nodes_explored.add(nodeID);
			if (changed_layer < 0 && !nodes.get(nodeID).preNodes.contains(preID))
				changed_layer = nodes.get(preID).layerID;
			nodes.get(nodeID).preNodes.add(preID);
			nodes.get(preID).subNodes.add(nodeID);
			preID = nodeID;
		}
		for (String nodeID : nodes.keySet()) {
			if (nodes.get(nodeID).layerID <= changed_layer)
				valid.put(nodeID, true);
			if (nodes.get(nodeID).layerID > 1)
				GenES(nodeID);
		}

	}

	public List<String> Traverse() {
		List<String> path = new ArrayList<String>();
		Set<String> visited = new HashSet<>();
		Map<String, String> next = new HashMap<>();
		Stack<String> stack = new Stack<String>();
		stack.push("start");
		while (!stack.empty()) {
			boolean traceback = true;
			String current = stack.lastElement();
			ArrayList<String> subNodes = new ArrayList<String>(nodes.get(current).subNodes);
			while (!subNodes.isEmpty()) {
				int randomIndex = new Random().nextInt(subNodes.size());
				String subnode = subNodes.get(randomIndex);
				subNodes.remove(randomIndex);
				// 到达success节点
				if (subnode.equals("end")) {
					next.put(current, subnode);
					for (current = "start"; !current.equals("end"); current = subnode) {
						subnode = next.get(current);
						if (!current.equals("start"))
							path.add(current);
					}
					return path;
				}
				if (nodes.get(subnode).available && !visited.contains(subnode)) {
					visited.add(subnode);
					next.put(current, subnode);
					stack.push(subnode);
					traceback = false;
					break;
				}
			}
			if (traceback == true) {
				stack.pop();
			}
		}
		return new ArrayList<String>();
	}

	boolean redundant(Set<Set<String>> config_set_A, Set<Set<String>> config_set_B, Set<String> a, Set<String> b) {
		judge_times++;
		Set<String> aUb = new LinkedHashSet<>(a);
		aUb.addAll(b);
		Set<String> a_b = new LinkedHashSet<>(a);
		a_b.removeAll(b);
		Set<String> b_a = new LinkedHashSet<>(b);
		b_a.removeAll(a);
		for (Set<String> config_a : config_set_A)
			if (aUb.containsAll(config_a) && !(config_a.containsAll(a_b)))
				return true;
		for (Set<String> config_b : config_set_B)
			if (aUb.containsAll(config_b) && !(config_b.containsAll(b_a)))
				return true;
		return false;
	}

	public Set<Set<String>> conduct(Set<Set<String>> config_set_A, Set<Set<String>> config_set_B) {
		if (config_set_A.size() == 0)
			return config_set_B;
		Set<Set<String>> result = new LinkedHashSet<>();
		for (Set<String> config_a : config_set_A) {
			for (Set<String> config_b : config_set_B) {
				Set<String> aUb = new LinkedHashSet<>(config_a);
				aUb.addAll(config_b);
				// System.out.println("@@redundant( A:" + config_set_A+", B:"+config_set_B+",
				// a:"+config_a+", b:"+config_b+")");
				if (!redundant(config_set_A, config_set_B, config_a, config_b)) {
					result.add(aUb);
					// System.out.println("! redundant, add"+aUb+" to result");
				} else {
					// System.out.println("redundant");
				}
			}
		}
		// System.out.println("Conduct Result: \n"+result);
		return result;
	}

	void sift(Set<Set<String>> configs) {
		Iterator<Set<String>> it = configs.iterator();
		while (it.hasNext()) {
			Set<String> config = it.next();
			for (Set<String> c : configs) {
				if (config.equals(c))
					continue;
				if (config.containsAll(c)) {
					it.remove();
					// System.out.println("漏网之鱼! config: " + config);
					break;
				}
			}
		}
	}

	public Set<Set<String>> bottleneck(int start_layer, String node) {
		// System.out.println("##In bottleneck " + node);
		Set<Set<String>> config_set = new LinkedHashSet<>();
		if (nodes.get(node).layerID == start_layer) {
			// System.out.println("bottleneck(" + node + ")=" + config_set);
			return config_set;
		}
		// 已计算过
		if (valid.containsKey(node) && counted.containsKey(node)) {
			counted_times++;
			// System.out.println("bottleneck(" + node + ")=" + counted.get(node));
			return new LinkedHashSet<>(counted.get(node));
		}
		for (String preNode : nodes.get(node).preNodes) {
			Set<Set<String>> c = bottleneck(start_layer, preNode);
			c.add(new LinkedHashSet<String>() {
				{
					add(preNode);
				}
			});
			// System.out.println("conduct(" + config_set + " and "+c+")");
			config_set = conduct(config_set, c);
		}
		sift(config_set);
		counted.put(node, config_set);
		valid.put(node, true);
		Set<String> set = new LinkedHashSet<>();
		for (Set<String> es : equivalent_set)
			if (es.contains(node)) {
				set = es;
				break;
			}
		for (String n : set) {
			counted.put(n, config_set);
			valid.put(n, true);
		}
		// System.out.println("bottleneck(" + node + ")=" + counted.get(node));
		return new LinkedHashSet<>(counted.get(node));
	}

	public static Set<Set<String>> bottleneck_split(FBFI_Algorithm handler, int start_layer, int span) {
		Set<Node> nodes = handler.layers.get("Layer" + Integer.toString(start_layer + span - 1));
		Set<Set<String>> C = new LinkedHashSet<>();
		for (Node node : nodes) {
			if (!nodes_explored.contains(node.nodeID))
				continue;
			Set<Set<String>> c = handler.bottleneck(start_layer, node.nodeID);
			c.add(new LinkedHashSet<String>() {
				{
					add(node.nodeID);
				}
			});
			C = handler.conduct(C, c);
		}
		return C;
	}

	public Set<Set<String>> CNF(String node) {
		Set<Set<String>> cnf = new HashSet<>();
		if (node.equals("start"))
			cnf.add(new HashSet<>());
		else
			for (Iterator<String> i = nodes.get(node).preNodes.iterator(); i.hasNext();) {
				Set<Set<String>> c = CNF(i.next());
				if (!node.equals("end")) {
					for (Set<String> s : c)
						s.add(node);
				}
				cnf.addAll(c);
			}
		return cnf;
	}

	@SuppressWarnings("resource")
	public boolean SolveLDFI(Set<Set<String>> cnf, List<Set<String>> FaultyCases, int most) {
		IO.Output("< Most: " + most + " >");

		LDFI_Result = new LinkedHashSet<>();
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		Context context = new Context(cfg);
		Solver solver = context.mkSolver();
		Map<String, BoolExpr> map = new HashMap<>();

		BoolExpr[] x = new BoolExpr[nodes.size() - 2];
		int i = 0;
		for (String node : nodes.keySet()) {
			if (!node.equals("start") && !node.equals("end")) {
				x[i] = context.mkBoolConst("x" + i);
				map.put(node, x[i++]);
			}
		}
		solver.add(context.mkAtMost(x, most));

		for (Set<String> cn : cnf) {
			BoolExpr OR = context.mkBool(false);
			for (String c : cn) {
				if (c.length() > 0)
					OR = context.mkOr(OR, map.get(c));
			}
			solver.add(OR);
		}

		for (Set<String> fc : FaultyCases) {
			BoolExpr hasTest = context.mkBool(true);
			for (String c : fc)
				hasTest = context.mkAnd(hasTest, map.get(c));
			hasTest = context.mkNot(hasTest);
			solver.add(hasTest);
		}
		// System.out.println(solver.toString());
		switch (solver.check()) {
		case SATISFIABLE:
			final Model model = solver.getModel();
			map.forEach((node, xi) -> {
				if (model.getConstInterp(xi).toString().equals("true")) {
					LDFI_Result.add(node);
				}
			});
			return true;
		case UNSATISFIABLE:
			return false;
		default:
			System.out.println("unknow");
			return false;
		}
	}

	void Inject_Restore(Set<String> config, boolean sign) {
		for (String node : config)
			nodes.get(node).available = sign;
	}

	public int minLayer(Set<String> config) {
		int minLayer = -1;
		for (String nodeID : config) {
			int currentLayer = nodes.get(nodeID).layerID;
			if (minLayer < 0 || currentLayer < minLayer)
				minLayer = currentLayer;
		}
		return minLayer;
	}



	public void Find_Defect(List<Set<String>> failedCases) {
		System.out.println(failedCases);
		for (Set<String> failed_case : failedCases) {
			System.out.println(failed_case);
			Inject_Restore(failed_case, false);
			if (Traverse().size() > 0) {
				System.out.println("defects");
				defects.add(failed_case);
			}
			else {
				for (String config : failed_case) {
					nodes.get(config).available = true;
					if (Traverse().size() == 0) {
						System.out.println("redundancies");
						redundancies.add(failed_case);
						break;
					}
					nodes.get(config).available = false;
				}
				System.out.println("bottlenecks");
				bottlenecks.add(failed_case);
			}
			Inject_Restore(failed_case, true);
		}
		bottlenecks.removeAll(redundancies);
	}

	void FBFI_Execute() {
		Set<Set<String>> tested = new LinkedHashSet<>();
		Set<String> config = new LinkedHashSet<>();
		while (true) {
			iterator_number++;
			System.out.println("\nround " + iterator_number + "\ninject " + config);
			Inject_Restore(config, false);
			List<String> path = Traverse();
			System.out.println("\npath: " + path);
			Inject_Restore(config, true);
			if (path.size() > 0) {
				path_number++;
				PathProcessing(path);
			} else
				failedCases.add(config);
			Set<Set<String>> C = bottleneck(1, "end");
			System.out.println("bottleneck: " + C);
			boolean finish = true;
			for (Set<String> c : C) {
				// System.out.println(c);
				if (!tested.contains(c)) {
					if (finish) {
						config = c;
						finish = false;
					} else {
						if (minLayer(c) < minLayer(config))
							config = c;
					}
				}
			}
			if (!config.isEmpty())
				tested.add(config);
			if (finish)
				break;
		}
	}

	void FBFI_Execute_v2() {
		Set<Set<String>> tested = new LinkedHashSet<>();
		Set<String> config = new LinkedHashSet<>();
		List<String> path = Traverse();
		System.out.println("\npath: " + path);
		path_number++;
		PathProcessing(path);
		for (int span = 1; span <= layer_number; span++) {
			IO.Output("\n[ FBFI Span: " + span + " ]");
			for (int start_layer = 1; start_layer + span - 1 <= layer_number; start_layer++) {
				System.out.println("< Layer: " + Integer.toString(start_layer) + "->"
						+ Integer.toString(start_layer + span - 1) + " >");
				boolean updated = true;
				Set<Set<String>> C = new LinkedHashSet<>();
				while (true) {
					IO.Output("\nFBFI Round " + ++iterator_number);
					FBFI_Algorithm split_handler = new FBFI_Algorithm(nodes, layers);
					// 如果路径更新，则重新计算瓶颈
					if (updated) {
						IO.Output("Node Number: " + (nodes_explored.size() - 2));
						C = bottleneck_split(split_handler, start_layer, span);
						IO.Output("Bottleneck result: " + C);
					} else
						IO.Output("Graph Unchanged");
					IO.Output("Bottleneck Size: " + C.size());
					boolean hasTestedAll = true;
					for (Set<String> c : C) {
						if (!tested.contains(c)) {
							config = c;
							hasTestedAll = false;
						}
					}
					if (hasTestedAll)
						break;
					tested.add(config);
					Inject_Restore(config, false);
					System.out.println("Inject: " + config);
					path = Traverse();
					System.out.println("path: " + path);
					Inject_Restore(config, true);
					if (path.size() > 0) {
						path_number++;
						PathProcessing(path);
						updated = true;
					} else {
						failedCases.add(config);
						updated = false;
					}
				}
			}
		}
	}

	void RunLDFI() {

		int round_number = 0;
		Set<String> config = new LinkedHashSet<>();
		failedCases = new ArrayList<>();
		boolean satis = true;
		int mostTrue = 20;
		List<String> path = new ArrayList<>();

		for (int most = 1; most < nodes.size();) {
			IO.Output("\nLDFI Round " + ++round_number);
			Set<Set<String>> cnf = CNF("end");
			System.out.println("CNF:\n" + cnf);
			if (SolveLDFI(cnf, failedCases, most) == false) {
				most++;
				continue;
			}
			config = LDFI_Result;
			Inject_Restore(config, false);
			System.out.println("Inject: " + config);
			path = Traverse();
			System.out.println("path: " + path);
			Inject_Restore(config, true);
			if (path.size() > 0) {
				path_number++;
				PathProcessing(path);

			} else {
				List<Set<String>> tempFailedCases = new ArrayList<>();
				for (Set<String> failedCase : failedCases) {
					if (!failedCase.containsAll(config))
						tempFailedCases.add(failedCase);
				}
				tempFailedCases.add(config);
				failedCases = tempFailedCases;
			}
		}

	}

	public static void main(String[] args) {

		int layer_size[] = { 1, 3, 4, 3, 4, 3, 4, 1 };
		String layer_name[]= {"start","A","B","C","D","E","F","end"};
		FBFI_Algorithm fbfi = new FBFI_Algorithm(layer_size, layer_name);
		long startTime = System.currentTimeMillis();
		//fbfi.FBFI_Execute();
		fbfi.FBFI_Execute_v2();
		//fbfi.RunLDFI();
		long endTime = System.currentTimeMillis();
		fbfi.Find_Defect(failedCases);

		System.out.println("B: \n" + fbfi.bottlenecks);
		System.out.println("B size: \n" + fbfi.bottlenecks.size());
		System.out.println("R: \n" + fbfi.redundancies);
		System.out.println("D: \n" + fbfi.defects);
		System.out.println("F: \n" + fbfi.failedCases);
		System.out.println("RunTime: " + (double) (endTime - startTime) / 1000 + "s");
		System.out.println("judge_times: " + fbfi.judge_times);
		System.out.println("counted_times: " + fbfi.counted_times);
		System.out.println("path_number: " + fbfi.path_number);
		System.out.println("iterator_number: " + fbfi.iterator_number);
	}
}
