package org.openjfx;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * see: https://adventofcode.com/2023/day/25
 */
public class Y23Day25 {

	static Y23GUIOutput3D18 output;

	/*
	 * Example:
	 * 
	 * jqt: rhn xhk nvd
	 * rsh: frs pzl lsr
	 * xhk: hfx
	 * cmg: qnr nvd lhk bvb
	 * rhn: xhk bvb hfx
	 * bvb: xhk hfx
	 * pzl: lsr hfx nvd
	 * qnr: nvd
	 * ntq: jqt hfx bvb xhk
	 * nvd: lhk
	 * lsr: lhk
	 * rzs: qnr cmg lsr rsh
	 * frs: qnr lhk lsr
	 * 
	 */

	private static final String INPUT_RX = "^([a-z]+): ([a-z ]+)$";
	
	public static record InputData(String nodeName, List<String> childNodeNames) {}
	
	public static class InputProcessor implements Iterable<InputData>, Iterator<InputData> {
		private Scanner scanner;
		public InputProcessor(String inputFile) {
			try {
				scanner = new Scanner(new File(inputFile));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		@Override public Iterator<InputData> iterator() { return this; }
		@Override public boolean hasNext() { return scanner.hasNext(); }
		@Override public InputData next() {
			String line = scanner.nextLine().trim();
			while (line.length() == 0) {
				line = scanner.nextLine();
			}
			if (line.matches(INPUT_RX)) {
				String nodeName = line.replaceFirst(INPUT_RX, "$1");
				List<String> childNodeNames = Arrays.asList(line.replaceFirst(INPUT_RX, "$2").trim().split(" +"));
				return new InputData(nodeName, childNodeNames);
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}


	static final long RAND_SEED = 4;
	static final double NET_DIST = 8.0;
	static final long NET_ITERATIONS = 200;
	static final double NET_SIZE_FACTOR = 0.25;

	static Random random = new Random(RAND_SEED);
	
	public static double rand(double from, double to) {
		return random.nextDouble()*(to-from)+from;
	}
	
    private static final DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
	public static String d(double d) {
		return df.format(d);
	}

	static record Pos3D(double x, double y, double z) {
		@Override public String toString() {
			return "("+d(x)+","+d(y)+","+d(z)+")";
		}
		public Pos3D add(Pos3D other) {
			return new Pos3D(x+other.x, y+other.y, z+other.z);  
		}
		public Pos3D subtract(Pos3D other) {
			return new Pos3D(x-other.x, y-other.y, z-other.z);  
		}
		public Pos3D multiply(double factor) {
			return new Pos3D(x*factor, y*factor, z*factor);  
		}
		public double magnitude() {
			return Math.sqrt(x*x+y*y+z*z);  
		}
		public Pos3D normalize() {
			double mag = magnitude();
			if (mag == 0) {
				return this;
			}
			return multiply(1/mag);  
		}
	}
	static Pos3D randomPos3D() {
		return new Pos3D(rand(-1000,1000), rand(-1000,1000), rand(-1000,1000));
	}

	static class Node3D {
		String name;
		Pos3D pos;
		Pos3D newPos;
		Set<Node3D> neighbours;
		public Node3D(String name, Pos3D pos) {
			this.name = name;
			this.pos = pos;
			neighbours = new LinkedHashSet<>();
		}
		@Override public String toString() {
			return name+"["+pos+"#"+neighbours.size()+"]";
		}
		public void addConnection(Node3D otherNode) {
			neighbours.add(otherNode);
		}
		public void removeChild(Node3D child) {
			neighbours.remove(child);
		}
	}
	
	static class Node {
		String name;
		List<Node> children; 
		public Node(String name) {
			this.name= name;
			this.children = new ArrayList<>();
		}
		public void addNode(Node node) {
			children.add(node);
		}
		@Override public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(name);
			String seperator = "[";
			for (Node child:children) {
				result.append(seperator).append(child.name);
				seperator = ",";
			}
			result.append("]");
			return result.toString();
		}
	}

	public static class World {
		Map<String, Node> nodes;
		Map<String, Node3D> nodes3D;
		public World() {
			this.nodes = new LinkedHashMap<>();
			this.nodes3D = new LinkedHashMap<>();
		}
		public void addNode(String nodeName, List<String> childNodeNames) {
			Node node = getOrCreateNode(nodeName);
			for (String childNodeName:childNodeNames) {
				node.addNode(getOrCreateNode(childNodeName));
			}
		}
		private Node getOrCreateNode(String nodeName) {
			return nodes.computeIfAbsent(nodeName, (k)->new Node(nodeName));
		}
		@Override
		public String toString() {
			return nodes.toString();
		}
		public void create3DTopology() {
			nodes3D = new HashMap<>();
			for (Node node:nodes.values()) {
				Node3D node3D = new Node3D(node.name, randomPos3D());
				nodes3D.put(node3D.name, node3D);
			}
			for (Node node:nodes.values()) {
				for (Node child:node.children) {
					addNode3DConnection(node.name, child.name);
				}
			}
		}
		private void addNode3DConnection(String nodeName1, String nodeName2) {
			Node3D node1 = nodes3D.get(nodeName1);
			Node3D node2 = nodes3D.get(nodeName2);
			node1.addConnection(node2);
			node2.addConnection(node1);
		}
		public void move3DNodes() {
			for (Node3D node:nodes3D.values()) {
				Pos3D sum = new Pos3D(0,0,0);
//				System.out.println("NODE "+node);
				int cntTargets = 0;
				for (Node3D neighbour:node.neighbours) {
//					System.out.println("  neighbout "+neighbour);
					Pos3D vect = neighbour.pos.subtract(node.pos);
					double dist = vect.magnitude();
//					System.out.println("  dist: "+dist);
					double move = dist-NET_DIST;
//					System.out.println("  move: "+move);
					Pos3D mVect = vect.normalize().multiply(move*0.5);
//					System.out.println("  mVect: "+mVect);
					Pos3D target = node.pos.add(mVect);
//					System.out.println("  target: "+target);
					sum = sum.add(target);
					cntTargets++;
//					System.out.println("  SUM: "+sum);
				}
				for (Node3D otherNode:nodes3D.values()) {
					if (otherNode == node) {
						continue;
					}
					Pos3D vect = otherNode.pos.subtract(node.pos);
					double dist = vect.magnitude();
					if (dist<NET_DIST/2) {
						double move = dist-NET_DIST/2;
						Pos3D mVect = vect.normalize().multiply(0.5*move);
						Pos3D target = node.pos.add(mVect);
						sum = sum.add(target);
						cntTargets++;
					}
				}
				Pos3D targetPos = sum.multiply(1.0/cntTargets);
//				System.out.println("  SUM/#: "+targetPos);
//				Pos3D halfWay = targetPos.subtract(node.pos)
				node.newPos = targetPos;
			}
//			System.out.println();
			for (Node3D node:nodes3D.values()) {
				node.pos = node.newPos;
			}
		}
		public void show3D(String info) {
			show3D(info, 0, -1, -1);
		}
		public void show3D(String info, int bitMask, int min, int max) {
			List<Y23GUIOutput3D18.DDDObject> points = new ArrayList<>();
			int cntLine = 0;
			for (Node3D node:nodes3D.values()) {
				int type = 3;
				double size = 1.0;
				double boxSize = size*NET_SIZE_FACTOR;
				Y23GUIOutput3D18.DDDObject point = new Y23GUIOutput3D18.DDDObject(node.name, node.pos.x, node.pos.y, node.pos.z, boxSize, type);
				points.add(point);
				for (Node3D neighbour:node.neighbours) {
					String lineName = node.name+"-"+neighbour.name;
					cntLine++;
					int lineType=3;
					double lineSize = 0.5*NET_SIZE_FACTOR;
					if ((cntLine >= min) && (cntLine <= max)) {
						System.out.println(cntLine+": "+lineName+" (>="+min+", <="+max+")");
						lineType=2;
						lineSize=2*lineSize;
					}
					if ((cntLine & bitMask) != 0) {
						System.out.println(cntLine+": "+lineName+" ("+(cntLine & bitMask)+")");
						lineType=0;
						lineSize=2*lineSize;
					}
//					if ((cntLine == 1533) || (cntLine == 3258) || (cntLine == 3029)) {
//					if ((cntLine == 1533)) {
//						System.out.println(cntLine+": "+lineName);
//						lineType=1;
//						lineSize=2*lineSize;
//						Y23GUIOutput3D18.DDDObject line = new Y23GUIOutput3D18.DDDLineObject(lineName, node.pos.x, node.pos.y, node.pos.z, neighbour.pos.x, neighbour.pos.y, neighbour.pos.z, lineSize, 30+lineType);
//						points.add(line);						
//					}
					Y23GUIOutput3D18.DDDObject line = new Y23GUIOutput3D18.DDDLineObject(lineName, node.pos.x, node.pos.y, node.pos.z, neighbour.pos.x, neighbour.pos.y, neighbour.pos.z, lineSize, 30+lineType);
					points.add(line);
				}
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.addStep(info, points);
		}
		public void remove(String nodeName1, String nodeName2) {
			Node3D node1 = nodes3D.get(nodeName1);
			Node3D node2 = nodes3D.get(nodeName2);
			node1.removeChild(node2);
			node2.removeChild(node1);
		}
		public int countCluster(String startNodeName) {
			Set<String> clusterNodes = new LinkedHashSet<>();
			Set<String> newNodes = new LinkedHashSet<>();
			newNodes.add(startNodeName);
			while (!newNodes.isEmpty()) {
				String nodeName = newNodes.iterator().next(); 
				Node3D node = nodes3D.get(nodeName);
				newNodes.remove(nodeName);
				clusterNodes.add(nodeName);
				for (Node3D child:node.neighbours) {
					if (clusterNodes.contains(child.name)) {
						continue;
					}
					newNodes.add(child.name);
				}
			}
			return clusterNodes.size();
		}
		public void remove(String lineName) {
			String[] nodeNames = lineName.split("-");
			remove(nodeNames[0], nodeNames[1]);
		}
		public String findLongestLine() {
			double maxDist = 0;
			String result = "?-?";
			for (Node3D node:nodes3D.values()) {
				for (Node3D child:node.neighbours) {
					double dist = child.pos.subtract(node.pos).magnitude();
					if (dist>maxDist) {
						maxDist = dist;
						result = node.name+"-"+child.name;
					}
				}
			}
			return result;
		}
		public void move3DNodes(long iterations, int show) {
			for (int n=0; n<iterations; n++) {
				move3DNodes();
				if ((show>0) && (n%show)==show-1) {
					show3D("iteration "+n);
				}
			}
		}
	}
	
	
	public static void mainPart1(String inputFile) {
		output = new Y23GUIOutput3D18("Day 25 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			world.addNode(data.nodeName, data.childNodeNames);
		}
//		System.out.println(world);
		world.create3DTopology();

		for (int l=0; l<3; l++) {
			world.move3DNodes(NET_ITERATIONS, 0);
			String lineName = world.findLongestLine();
			System.out.println("LONGEST LINE: "+lineName);
			world.show3D("move "+l+" longest line: "+lineName);
			world.remove(lineName);
			world.show3D("move "+l+" removed: "+lineName);
		}
		world.move3DNodes(NET_ITERATIONS, 0);
		world.show3D("move clusters");
		
//		world.remove("cmj","qhd");
//		world.remove("lnf","jll");
//		world.remove("vtv","kkp");
		
		int clusterSize1 = world.countCluster("cmj");
		int clusterSize2 = world.countCluster("qhd");

		System.out.println("ClusteSizes: "+clusterSize1+" x "+clusterSize2+" = "+clusterSize1*clusterSize2);

		if (true) {
			return;
		}
		
		for (int n=0; n<NET_ITERATIONS; n++) {
			world.move3DNodes();
			if ((n%50)==0) {
//				world.show3D("move "+n);
			}
		}

		show(world, 1701, 1701);
		show(world, 820, 820);
		show(world, 2525, 2525);
		show(world, 4194, 4194);
		show(world, 4873, 4873);
		show(world, 6482, 6482);
		
//		for (int i=6400; i<=6499; i+=1) {
//			show(world, i, i);
//		}
		
//		for (int i=0; i<=8000; i+=100) {
//			show(world, i, i+99);
//		}
		
//		for (int i=0; i<=20; i++) {
//			world.show3D("BIT "+i, 1<<i, -1, -1);
//		}
		
		world.show3D("init");
		
	}

	
	/**
	 *  820: cmj-qhd (>=820, <=820)
	 * 2525: qhd-cmj (>=2525, <=2525)
	 * 1701: lnf-jll (>=1701, <=1701)
	 * 4873: jll-lnf (>=4873, <=4873)
	 * 4194: vtv-kkp (>=4194, <=4194)
	 * 6482: kkp-vtv (>=6482, <=6482)
     *
	 * @param world
	 * @param from
	 * @param to
	 */
	


	private static void show(World world, int from, int to) {
		world.show3D(from+".."+to, 0, from, to);
	}




	public static void mainPart2(String inputFile) {
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		System.out.println("--- PART I ---");

//		URL url = Y23Day24.class.getResource("/resources/input/aoc23day25/input-example.txt");
		URL url = Y23Day24.class.getResource("/resources/input/aoc23day25/input.txt");    // 616225 too high (785x785)     
		mainPart1(new File(url.toURI()).toString());
		
//		mainPart1("exercises/day25/Feri/input-example.txt");
//		mainPart1("exercises/day25/Feri/input.txt");               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
//		mainPart2("exercises/day25/Feri/input-example.txt");
//		mainPart2("exercises/day25/Feri/input.txt");
		System.out.println("---------------");    
	}
	
}
