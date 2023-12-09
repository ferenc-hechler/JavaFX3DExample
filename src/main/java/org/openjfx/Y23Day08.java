package org.openjfx;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.openjfx.Y22Day22Animation.Pos3D;
import org.openjfx.Y22Day22Animation.Position;
import org.openjfx.Y22Day22Animation.SurfaceDef;

/**
 * see: https://adventofcode.com/2023/day/08
 */
public class Y23Day08 {

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
	
	static Random random = new Random();
	
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
		return new Pos3D(rand(-100,100), rand(-100,100), rand(-100,100));
	}

	
	static class Node3D {
		String name;
		Pos3D pos;
		Pos3D newPos;
		List<Node3D> neighbours;
		public Node3D(String name, Pos3D pos) {
			this.name = name;
			this.pos = pos;
			neighbours = new ArrayList<>();
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
				System.out.println("NODE "+node);
				for (Node3D neighbour:node.neighbours) {
					System.out.println("  neighbout "+neighbour);
					Pos3D vect = neighbour.pos.subtract(node.pos);
					double dist = vect.magnitude();
					System.out.println("  dist: "+dist);
					double move = dist-500.0;
					System.out.println("  move: "+move);
					Pos3D mVect = vect.normalize().multiply(move);
					System.out.println("  mVect: "+mVect);
					Pos3D target = node.pos.add(mVect);
					System.out.println("  target: "+target);
					sum = sum.add(target);
					System.out.println("  SUM: "+sum);
					System.out.println(sum);
				}
				Pos3D targetPos = sum.multiply(1.0/node.neighbours.size());
				System.out.println("  SUM/#: "+targetPos);
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
				GUIOutput3D.DDDObject point = new GUIOutput3D.DDDObject(node.pos.x, node.pos.y, node.pos.z, 10.0, 1);
				points.add(point);
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.addStep("", points);
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
			for (int i=0; i<5; i++) {
				System.out.println("    "+i+": "+nodes.get(circleNodeNames.get(i)));	
			}
			for (int i=circleNodeNames.size()-5; i<circleNodeNames.size(); i++) {
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
        // Die Funktion kgV soll das kleinste gemeinsame Vielfach zweier Zahlen a und b berechnen. Die zwei Zahlen wurden als Argument an die kgV Funktion übergeben.
        // Um das Ergebnis zu speichern, wird in einer Variable gespeichert. Dafür wird die Variable resultat deklariert.
    	long resultat;
        
        // Um das kgV zu berechnen werden die zwei Zahlen a und b zuerst multipliziert und das Ergebnis wird dann durch den größten gemeinsamen Teiler der zwei Zahlen a und b geteilt. Das Ergebnis wird in der Variablen resultat gespeichert.
        resultat = (long) ((a * b) / ggT(a, b));
        
        return resultat;
    }

	
	public static void mainPart1(String inputFile) {
		output = new GUIOutput3D("Day 08 Part I");
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
		world.show3D();
		for (int n=1; n<50; n++) {
			world.move3DNodes();
			world.show3D();
		}
		while (!world.currentNodeName().equals("ZZZ")) {
			world.tick();
		}
		System.out.println("TICKS: "+world.getTicks());
	}
	
	
	public static void mainPart2(String inputFile) {
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
//		System.out.println(world2.getTicks()+" "+world2.peekNextDir());
//		System.out.println(world2.toString());
		while (!world2.allCirclesDetected()) {
			world2.tick();
//			System.out.println(world2.getTicks()+" "+world2.peekNextDir());
//			System.out.println(world2.toString());
		}
		System.out.println("TICKS: "+world2.calcTicksForAllZ());
	}

	

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("--- PART I ---");
//		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input-example.txt");
//		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input-example-2.txt");
		mainPart1("../advent-of-code-2023/exercises/day08/Feri/input.txt");               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
//		mainPart2("../advent-of-code-2023/exercises/day08/Feri/input-example-3.txt");
//		mainPart2("../advent-of-code-2023/exercises/day08/Feri/input.txt");                
		System.out.println("---------------");    //
	}
	
}
