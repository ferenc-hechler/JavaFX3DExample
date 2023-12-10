package org.openjfx;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
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
import java.util.stream.Collectors;

/**
 * see: https://adventofcode.com/2023/day/08
 */
public class Y23Day08Animation3D {

	static final long RAND_SEED = 4;
	static final double NET_DIST = 8.0;
	static final long NET_ITERATIONS = 200;
	static final double NET_SIZE_FACTOR = 0.7;
	
	
	
	static GUIOutput3D output;
	/*
	 *
	 * LLR
	 * 
	 * AAA = (BBB, BBB)
	 * BBB = (AAA, ZZZ)
	 * ZZZ = (ZZZ, ZZZ)
	 * 
	 */

	private static final String INPUT_RX_MOVEMENTS = "^([LR]+)$";
	private static final String INPUT_RX_NODE      = "^([A-Z0-9]+) = [(]([A-Z0-9]+), ([A-Z0-9]+)[)]$";
	
	public static record InputData(String movements, Node node) {
		public boolean isMovements() { return movements != null; }
		public boolean isNode() { return node != null; }
		@Override public String toString() {
			if (isMovements()) {
				return movements;
			}
			return node.toString();
		}
	}
	
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
			if (line.matches(INPUT_RX_MOVEMENTS)) {
				String movements = line.replaceFirst(INPUT_RX_MOVEMENTS, "$1");
				return new InputData(movements, null);
			}
			else if (line.matches(INPUT_RX_NODE)) {
				String nodeName = line.replaceFirst(INPUT_RX_NODE, "$1");
				String childLeft = line.replaceFirst(INPUT_RX_NODE, "$2");
				String childRight = line.replaceFirst(INPUT_RX_NODE, "$3");
				return new InputData(null, new Node(nodeName, childLeft, childRight));
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}
	
	public static record Node(String nodeName, String childLeft, String childRight) {
		@Override public String toString() {
			return nodeName+" = ("+childLeft+", "+childRight+")";
		}
	}
	
    private static final DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
	
	public static String d(double d) {
		return df.format(d);
	}
	
	static Random random = new Random(RAND_SEED);
	
	public static double rand(double from, double to) {
		return random.nextDouble(from, to);
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
	}
	
	public static class World {
		String movements;
		Map<String, Node> nodes;
		Node currentNode;
		int nextMovementIdx;
		int ticks;
		Map<String, Node3D> nodes3D;
		public World() {
			this.nodes = new HashMap<>();
			this.ticks = 0;
		}
		public void setMovements(String movements) {
			this.movements = movements;
			this.nextMovementIdx = 0;
		}
		public void addNode(Node node) {
			nodes.put(node.nodeName, node);
			if (node.nodeName.equals("AAA")) {
				this.currentNode = node;
			}
		}
		public char nextMove() {
			char result = movements.charAt(nextMovementIdx);
			nextMovementIdx = (nextMovementIdx+1)%movements.length();
			return result;
		}
		public char peekNextDir() {
			return movements.charAt(nextMovementIdx);
		}
		public void tick() {
			ticks++;
			char dir = nextMove();
			String nextNodeName = dir == 'L' ? currentNode.childLeft : currentNode.childRight;
			currentNode = nodes.get(nextNodeName);
		}
		public int getTicks() {
			return ticks;
		}
		public String currentNodeName() {
			return currentNode.nodeName;
		}
		public void create3DTopology() {
			nodes3D = new HashMap<>();
			for (Node node:nodes.values()) {
				Node3D node3D = new Node3D(node.nodeName, randomPos3D());
				nodes3D.put(node3D.name, node3D);
			}
			for (Node node:nodes.values()) {
				addNode3DConnection(node.nodeName, node.childLeft);
				addNode3DConnection(node.nodeName, node.childRight);
			}
		}
		public void move3DNodes() {
			for (Node3D node:nodes3D.values()) {
				List<Pos3D> neighbourPos = node.neighbours.stream().map(n->n.pos).toList();
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
			System.out.println();
			for (Node3D node:nodes3D.values()) {
				node.pos = node.newPos;
			}
		}
		public void show3D() {
			List<GUIOutput3D.DDDObject> points = new ArrayList<>();
			for (Node3D node:nodes3D.values()) {
				String nodeName = node.name;
				int type = 3;
				double size = 1.0;
				if (currentNodeName().equals(nodeName)) {
					type = 0;
					size = 1.5;
				}
				else if (nodeName.equals("AAA")) {
					type = 2;
				}
				else if (nodeName.equals("ZZZ")) {
					type = 1;
				}
				double boxSize = size*NET_SIZE_FACTOR;
				double lineSize = 0.1*NET_SIZE_FACTOR;
				GUIOutput3D.DDDObject point = new GUIOutput3D.DDDObject(node.name, node.pos.x, node.pos.y, node.pos.z, boxSize, type);
				points.add(point);
				for (Node3D neighbour:node.neighbours) {
					GUIOutput3D.DDDObject line = new GUIOutput3D.DDDLineObject(node.pos.x, node.pos.y, node.pos.z, neighbour.pos.x, neighbour.pos.y, neighbour.pos.z, lineSize, 30);
					points.add(line);
				}
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.addStep(currentNode.toString()+" "+peekNextDir(), points);
		}
		private void addNode3DConnection(String nodeName1, String nodeName2) {
			Node3D node1 = nodes3D.get(nodeName1);
			Node3D node2 = nodes3D.get(nodeName2);
			node1.addConnection(node2);
			node2.addConnection(node1);
		}
	}

	public static record NodeAtMovementIndex(String nodeName, int movementIndex) {
		@Override public String toString() { return nodeName+"-"+movementIndex; }
	}
	
	public static class CircleDetector {
		int id;
		Map<NodeAtMovementIndex, Integer> previousNodesAtTick;
		int circleLength;
		int lastTick;
		public CircleDetector(int id, Node node) {
			this.id = id;
			this.previousNodesAtTick = new LinkedHashMap<>();
			this.circleLength = 0;
			detectCircle(node.nodeName, 0, 0);
		}
		public boolean circleDetected() {
			return circleLength != 0;
		}
		public boolean detectCircle(String nodeName, int moveIndex, int tick) {
			if (circleDetected()) {
				return true;
			}
			NodeAtMovementIndex nodeAtIdx = new NodeAtMovementIndex(nodeName, moveIndex);
			if (previousNodesAtTick.containsKey(nodeAtIdx)) {
				lastTick = tick;
				int oldTick = previousNodesAtTick.get(nodeAtIdx);
				circleLength = lastTick - oldTick;
				System.out.println("FOUND CIRCLE "+id+" at "+nodeName+",idx="+moveIndex+" "+": "+circleLength);
				return true;
			}
			previousNodesAtTick.put(nodeAtIdx, tick);
			return false;
		}
		public void printZTicks(Map<String, Node> nodes) {
			List<String> circleNodeNames = previousNodesAtTick.keySet().stream().map(nami->nami.nodeName).toList();
			int firstCircleTick = lastTick - circleLength;
			System.out.println();
			System.out.println("CIRCLE "+id+": Node: "+circleNodeNames.get(firstCircleTick)+", firstCircleTick: "+firstCircleTick+", lastCircleTick: "+(lastTick-1));
			System.out.print("  zIndex= n*"+circleLength+"+[");
			for (int i=firstCircleTick; i<lastTick; i++) {
				if (circleNodeNames.get(i).endsWith("Z")) {
					System.out.print((i%circleLength)+",");
				}
			}
			System.out.println("]");
			for (int i=0; i<Math.min(5, circleNodeNames.size()); i++) {
				System.out.println("    "+i+": "+nodes.get(circleNodeNames.get(i)));	
			}
			for (int i=Math.max(0, circleNodeNames.size()-5); i<circleNodeNames.size(); i++) {
				System.out.println(i+": "+nodes.get(circleNodeNames.get(i)));	
			}
		}
	}
	
	public static class World2 {
		String movements;
		Map<String, Node> nodes;
		List<Node> currentNodes;
		List<CircleDetector> circleDetectors;
		int nextMovementIdx;
		int ticks;
		Map<String, Node3D> nodes3D;
		public World2() {
			this.nodes = new HashMap<>();
			this.currentNodes = new ArrayList<>();
			this.circleDetectors = new ArrayList<>();
			this.ticks = 0;
		}
		public void setMovements(String movements) {
			this.movements = movements;
			this.nextMovementIdx = 0;
		}
		public void addNode(Node node) {
			nodes.put(node.nodeName, node);
			if (node.nodeName.endsWith("A")) {
				this.currentNodes.add(node);
				this.circleDetectors.add(new CircleDetector(currentNodes.size(), node));
			}
		}
		public char nextMove() {
			char result = movements.charAt(nextMovementIdx);
			nextMovementIdx = (nextMovementIdx+1)%movements.length();
			return result;
		}
		public char peekNextDir() {
			return movements.charAt(nextMovementIdx);
		}
		public void tick() {
			ticks++;
			char dir = nextMove();
			for (int i=0; i<circleDetectors.size(); i++) {
				CircleDetector circleDetector = circleDetectors.get(i);
				if (circleDetector.circleDetected()) {
					continue;
				}
				Node currentNode = currentNodes.get(i);
				String nextNodeName = dir == 'L' ? currentNode.childLeft : currentNode.childRight;
				currentNodes.set(i, nodes.get(nextNodeName));
				circleDetector.detectCircle(nextNodeName, nextMovementIdx, ticks);
			}
		}
		public int getTicks() {
			return ticks;
		}
		public boolean allCirclesDetected() {
			for (CircleDetector circleDetector:circleDetectors) {
				if (!circleDetector.circleDetected()) {
					return false;
				}
			}
			return true;
		}
		public long calcTicksForAllZ() {
			List<Long> circleLengths = new ArrayList<>(); 
			for (CircleDetector circleDetector:circleDetectors) {
				circleLengths.add((long)circleDetector.circleLength);
				circleDetector.printZTicks(nodes);
			}
			return kgv(circleLengths);
		}
		private long kgv(List<Long> values) {
			long result = values.get(0);
			for (long value:values) {
				result = kgV(result, value);
			}
			return result;
		}
		@Override public String toString() {
			StringBuilder result = new StringBuilder();
			for (Node currentNode:currentNodes) {
				result.append(currentNode.toString()).append("\n");
			}
			return result.toString();
		}
		public void create3DTopology() {
			nodes3D = new HashMap<>();
			for (Node node:nodes.values()) {
				Node3D node3D = new Node3D(node.nodeName, randomPos3D());
				nodes3D.put(node3D.name, node3D);
			}
			for (Node node:nodes.values()) {
				addNode3DConnection(node.nodeName, node.childLeft);
				addNode3DConnection(node.nodeName, node.childRight);
			}
		}
		public void move3DNodes() {
			for (Node3D node:nodes3D.values()) {
				List<Pos3D> neighbourPos = node.neighbours.stream().map(n->n.pos).toList();
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
			System.out.println();
			for (Node3D node:nodes3D.values()) {
				node.pos = node.newPos;
			}
		}
		public void show3D() {
			Set<String> currentNodeNames = currentNodes.stream().map(n->n.nodeName).collect(Collectors.toSet());
			List<GUIOutput3D.DDDObject> points = new ArrayList<>();
			for (Node3D node:nodes3D.values()) {
				String nodeName = node.name;
				int type = 3;
				double size = 1.0;
				if (currentNodeNames.contains(nodeName)) {
					type = 0;
					size = 1.5;
				}
				else if (nodeName.endsWith("A")) {
					type = 2;
				}
				else if (nodeName.endsWith("Z")) {
					type = 1;
				}
				double boxSize = size*NET_SIZE_FACTOR;
				double lineSize = 0.1*NET_SIZE_FACTOR;
				GUIOutput3D.DDDObject point = new GUIOutput3D.DDDObject(node.name, node.pos.x, node.pos.y, node.pos.z, boxSize, type);
				points.add(point);
				for (Node3D neighbour:node.neighbours) {
					GUIOutput3D.DDDObject line = new GUIOutput3D.DDDLineObject(node.name+neighbour.name, node.pos.x, node.pos.y, node.pos.z, neighbour.pos.x, neighbour.pos.y, neighbour.pos.z, lineSize, 30);
					if ("CDBSDB".equals(line.id)) {
						System.out.println("BREAK");
					}
					points.add(line);
				}
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.addStep(currentNodes.get(1).toString()+" "+peekNextDir(), points);
		}
		private void addNode3DConnection(String nodeName1, String nodeName2) {
			Node3D node1 = nodes3D.get(nodeName1);
			Node3D node2 = nodes3D.get(nodeName2);
			node1.addConnection(node2);
			node2.addConnection(node1);
		}
	}

	// https://www.programmieren-ist-einfach.de/Java/F009.html
    public static long ggT(long a, long b) {
        // Die Funktion ggT berechnet den größten gemeinsamen Teiler zweier Zahlen a und b.
        // Die Zwischenergebnisse und das Endergebnis der Funktion ggT werden in einer Variable gespeichert. Dafür wird die Variable resultat deklariert.
    	long resultat;
        
        // Im Fall, dass die erste Zahl a gleich 0 ist, ist das Ergebnis gleich b (der zweiten Zahl). Im Fall, dass a jedoch ungleich 0 ist, wird die ggT Funktion mit den geänderten beziehungsweise angepassten Argumenten ggT(b MOD a, a).
        if (a == 0) {
            resultat = b;
        } else {
            resultat = ggT(b % a, a);
        }
        return resultat;
    }
    
	// https://www.programmieren-ist-einfach.de/Java/F009.html
    public static long kgV(long a, long b) {
    	if ((a==0) || (b==0)) {
    		return 0;
    	}
        // Die Funktion kgV soll das kleinste gemeinsame Vielfach zweier Zahlen a und b berechnen. Die zwei Zahlen wurden als Argument an die kgV Funktion übergeben.
        // Um das Ergebnis zu speichern, wird in einer Variable gespeichert. Dafür wird die Variable resultat deklariert.
    	long resultat;
        
        // Um das kgV zu berechnen werden die zwei Zahlen a und b zuerst multipliziert und das Ergebnis wird dann durch den größten gemeinsamen Teiler der zwei Zahlen a und b geteilt. Das Ergebnis wird in der Variablen resultat gespeichert.
        resultat = (long) ((a * b) / ggT(a, b));
        
        return resultat;
    }

	
	public static void mainPart1(String inputFile) {
		output = new GUIOutput3D("Day 08 Part I");
		output.setUseCachedNodes(true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			if (data.isMovements()) {
				world.setMovements(data.movements);
			}
			else {
				world.addNode(data.node);
			}
		}
		world.create3DTopology();
		for (int n=0; n<NET_ITERATIONS; n++) {
			world.move3DNodes();
//			if ((n%10)==0) {
//				world.show3D();
//			}
		}
		world.show3D();
		int cnt = 0;
		while (!world.currentNodeName().equals("ZZZ")) {
			System.out.println(cnt+": "+world.currentNode+" "+world.peekNextDir());
			world.tick();
			world.show3D();
			if (cnt++ >= 200) {
				break;
			}
		}
		System.out.println("TICKS: "+world.getTicks());
	}
	
	
	public static void mainPart2(String inputFile) {
		output = new GUIOutput3D("Day 08 Part II");
		output.setUseCachedNodes(true);
		World2 world2 = new World2();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			if (data.isMovements()) {
				world2.setMovements(data.movements);
			}
			else {
				world2.addNode(data.node);
			}
		}
		
		world2.create3DTopology();
		for (int n=1; n<NET_ITERATIONS; n++) {
			world2.move3DNodes();
		}
		world2.show3D();
		
//		System.out.println(world2.getTicks()+" "+world2.peekNextDir());
//		System.out.println(world2.toString());
		int cnt = 0;
		while (!world2.allCirclesDetected()) {
			cnt++;
			world2.tick();
			if (cnt < 200) {
				world2.show3D();
			}
			else {
				break;
			}
//			System.out.println(world2.getTicks()+" "+world2.peekNextDir());
//			System.out.println(world2.toString());
		}
		System.out.println("TICKS: "+world2.calcTicksForAllZ());
	}

	

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("--- PART I ---");
//		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input-example.txt");
//		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input-example-2.txt");
//		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input.txt");               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
//		mainPart2("../advent-of-code-2023/exercises/day08/Feri/input-example-3.txt");
		mainPart2("../advent-of-code-2023/exercises/day08/Feri/input.txt");                
		System.out.println("---------------");    //
	}
	
}
