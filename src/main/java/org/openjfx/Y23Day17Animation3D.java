package org.openjfx;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

/**
 * see: https://adventofcode.com/2023/day/17
 */
public class Y23Day17Animation3D {

	
	static Y23GUIOutput3D17 output;
	
	/*
	 * Example:
	 * 
	 * 2413432311323
	 * 3215453535623
	 * 3255245654254
	 * 3446585845452
	 * 4546657867536
	 * 1438598798454
	 * 4457876987766
	 * 3637877979653
	 * 4654967986887
	 * 4564679986453
	 * 1224686865563
	 * 2546548887735
	 * 4322674655533
	 * 
	 */

	private static final String INPUT_RX = "^([0-9]*)$";
	
	public static record InputData(String row) {
		@Override public String toString() { return row; }
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
			if (line.matches(INPUT_RX)) {
				String row = line.replaceFirst(INPUT_RX, "$1");
				return new InputData(row);
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}

	static String DIRS            = ">v<^";
	static int[]  DIR_ADD_X 	  = { 1,   0,  -1,   0};
	static int[]  DIR_ADD_Y 	  = { 0,   1,   0,  -1};
	
	static int DIR_EAST = 0;
	static int DIR_SOUTH = 1;
	static int DIR_WEST = 2;
	static int DIR_NORTH = 3;
	
	static int DIR_ROT_LEFT = 3;
	static int DIR_ROT_RIGHT = 1;

	static int rot(int dir, int rot) { return (dir+rot+4)%4; }


	static final int Z_HORIZONTAL = 0;
	static final int Z_VERTICAL = 1;

	static record State(int sumHeatLoss, Pos pos, Pos fromPos) {
		@Override public String toString() { return "M["+fromPos+"->"+pos+"|"+sumHeatLoss+"]"; }
	}
	
	static record Pos(int x, int y, int z) {
		@Override public String toString() { return "("+x+","+y+","+z+")"; }
	}
	
	static record Move(Pos targetPos, int heatloss) {
		@Override public String toString() { return "M["+targetPos+"|"+heatloss+"]"; }
	}
	
	public static class World {
		Map<Pos, List<Move>> heatlossDirections;
		Map<Pos, Integer> minimalHeatLoss;
		PriorityQueue<State> searchPaths;
		List<String> rows;
		int[][] field;
		int maxX;
		int maxY;
		int ticks;
		public World() {
			this.rows = new ArrayList<>();
		}
		public void addRow(String row) {
			rows.add(row);
		}
		public void init() {
			ticks = 0;
			maxY = rows.size();
			maxX = rows.get(0).length();
			field = new int[maxY][maxX];
			for (int y=0; y<maxY; y++) {
				for (int x=0; x<maxX; x++) {
					field[y][x] = rows.get(y).charAt(x)-'0';
				}
			}
		}
		private int get(int x, int y) {
			if ((x<0) || (y<0) || (x>=maxX) || (y>=maxY)) {
				return '?';
			}
			return field[y][x];
		}		
		public void tick() {
			ticks++;
		}
		@Override public String toString() {
			StringBuilder result = new StringBuilder();
			for (int y=0; y<maxY; y++) {
				for (int x=0; x<maxX; x++) {
					result.append((char) ('0'+get(x,y)));
				}
				result.append("\n");
			}
			return result.toString();
		}
		boolean isValidPos(int x, int y) {
			return (x>=0)&&(x<maxX)&&(y>=0)&&(y<maxY);
		}
		private List<Move> createHorizontalMoves(int x, int y) {
			List<Move> result = new ArrayList<>();
			int heatLossForward = 0;
			int heatLossBackward = 0;
			for (int dx=1; dx<=3; dx++) {
				if (isValidPos(x+dx, y)) {
					Pos targetPos = new Pos(x+dx, y, Z_VERTICAL);
					heatLossForward += get(x+dx, y);
					result.add(new Move(targetPos, heatLossForward));
				}
				if (isValidPos(x-dx, y)) {
					Pos targetPos = new Pos(x-dx, y, Z_VERTICAL);
					heatLossBackward += get(x-dx, y);
					result.add(new Move(targetPos, heatLossBackward));
				}
			}
			return result;
		}
		private List<Move> createVerticalMoves(int x, int y) {
			List<Move> result = new ArrayList<>();
			int heatLossForward = 0;
			int heatLossBackward = 0;
			for (int dy=1; dy<=3; dy++) {
				if (isValidPos(x, y+dy)) {
					Pos targetPos = new Pos(x, y+dy, Z_HORIZONTAL);
					heatLossForward += get(x, y+dy);
					result.add(new Move(targetPos, heatLossForward));
				}
				if (isValidPos(x, y-dy)) {
					Pos targetPos = new Pos(x, y-dy, Z_HORIZONTAL);
					heatLossBackward += get(x, y-dy);
					result.add(new Move(targetPos, heatLossBackward));
				}
			}
			return result;
		}
		public void createHeatlossDirectionGraph() {
			heatlossDirections = new LinkedHashMap<>();
			for (int y=0; y<maxY; y++) {
				for (int x=0; x<maxX; x++) {
					heatlossDirections.put(new Pos(x,y,Z_HORIZONTAL), createHorizontalMoves(x,y));
					heatlossDirections.put(new Pos(x,y,Z_VERTICAL), createVerticalMoves(x,y));
				}
			}
		}
		private List<Move> createUltraHorizontalMoves(int x, int y) {
			List<Move> result = new ArrayList<>();
			int heatLossForward = 0;
			int heatLossBackward = 0;
			for (int dx=1; dx<=3; dx++) {
				if (isValidPos(x+dx, y)) {
					heatLossForward += get(x+dx, y);
				}
				if (isValidPos(x-dx, y)) {
					heatLossBackward += get(x-dx, y);
				}
			}
			for (int dx=4; dx<=10; dx++) {
				if (isValidPos(x+dx, y)) {
					Pos targetPos = new Pos(x+dx, y, Z_VERTICAL);
					heatLossForward += get(x+dx, y);
					result.add(new Move(targetPos, heatLossForward));
				}
				if (isValidPos(x-dx, y)) {
					Pos targetPos = new Pos(x-dx, y, Z_VERTICAL);
					heatLossBackward += get(x-dx, y);
					result.add(new Move(targetPos, heatLossBackward));
				}
			}
			return result;
		}
		private List<Move> createUltraVerticalMoves(int x, int y) {
			List<Move> result = new ArrayList<>();
			int heatLossForward = 0;
			int heatLossBackward = 0;
			for (int dy=1; dy<=3; dy++) {
				if (isValidPos(x, y+dy)) {
					heatLossForward += get(x, y+dy);
				}
				if (isValidPos(x, y-dy)) {
					heatLossBackward += get(x, y-dy);
				}
			}
			for (int dy=4; dy<=10; dy++) {
				if (isValidPos(x, y+dy)) {
					Pos targetPos = new Pos(x, y+dy, Z_HORIZONTAL);
					heatLossForward += get(x, y+dy);
					result.add(new Move(targetPos, heatLossForward));
				}
				if (isValidPos(x, y-dy)) {
					Pos targetPos = new Pos(x, y-dy, Z_HORIZONTAL);
					heatLossBackward += get(x, y-dy);
					result.add(new Move(targetPos, heatLossBackward));
				}
			}
			return result;
		}
		public void createUltraHeatlossDirectionGraph() {
			heatlossDirections = new LinkedHashMap<>();
			for (int y=0; y<maxY; y++) {
				for (int x=0; x<maxX; x++) {
					heatlossDirections.put(new Pos(x,y,Z_HORIZONTAL), createUltraHorizontalMoves(x,y));
					heatlossDirections.put(new Pos(x,y,Z_VERTICAL), createUltraVerticalMoves(x,y));
				}
			}
		}
		public String showMinimalMoves() {
			StringBuilder result = new StringBuilder();
			for (int y=0; y<maxY; y++) {
				for (int x=0; x<maxX; x++) {
					String minHLH = num3(minimalHeatLoss.get(new Pos(x,y,Z_HORIZONTAL)));
					String minHLV = num3(minimalHeatLoss.get(new Pos(x,y,Z_VERTICAL)));
					result.append(" "+minHLH+"/"+minHLV);
				}
				result.append("\n");
			}
			return result.toString();
		}
		private String num3(Integer n) {
			if (n==null) {
				return "***";
			}
			String result = Integer.toString(n);
			return "   ".substring(result.length(), 3)+result;
		}
		public int findMinimalHeatLoss() {
			minimalHeatLoss = new LinkedHashMap<>();
			searchPaths = new PriorityQueue<>((s1,s2)->Integer.compare(s1.sumHeatLoss,s2.sumHeatLoss));
			Pos startPosH = new Pos(0,0,Z_HORIZONTAL);
			Pos startPosV = new Pos(0,0,Z_VERTICAL);
			searchPaths.add(new State(0, startPosH, startPosH));
			searchPaths.add(new State(0, startPosV, startPosV));
			minimalHeatLoss.put(startPosH, 0);
			minimalHeatLoss.put(startPosV, 0);
			while (true) {
				State currentSearch = searchPaths.poll();
				show3D(currentSearch);
//				output.addStep(currentSearch+"\n"+showMinimalMoves());
//				System.out.println(currentSearch);
//				if (currentSearch.pos.toString().contains("2,1,0")) {
//					System.out.println("BREAK");
//				}
				if ((currentSearch.pos.x == maxX-1) && (currentSearch.pos.y == maxY-1)) {
					return currentSearch.sumHeatLoss;
				}
				List<Move> moves = heatlossDirections.get(currentSearch.pos);
				for (Move move:moves) {
					Integer minHL = minimalHeatLoss.get(move.targetPos);
					if ((minHL==null) || (minHL>currentSearch.sumHeatLoss+move.heatloss)) {
						minimalHeatLoss.put(move.targetPos, currentSearch.sumHeatLoss+move.heatloss);
						searchPaths.add(new State(currentSearch.sumHeatLoss+move.heatloss, move.targetPos, currentSearch.pos));
					}
				}
			}
		}
		static final double SCALEZ = 2.0;
		Map<Pos, Pos> shortestPathsList = new LinkedHashMap<>();
		void show3D(State state) {
			shortestPathsList.put(state.pos, state.fromPos);
			Map<Pos, Pos> currentShortestPathsList = recursiveGetShortestPath(state.pos, 0);
			List<Y23GUIOutput3D17.DDDObject> points = new ArrayList<>();
			for (Pos pos:heatlossDirections.keySet()) {
				List<Move> moves = heatlossDirections.get(pos);
				int type = 3;
				if (shortestPathsList.containsKey(pos)) {
					type = 1;
				}
				if (currentShortestPathsList.containsKey(pos)) {
					type = 0;
				}
				Y23GUIOutput3D17.DDDObject point = new Y23GUIOutput3D17.DDDObject(pos.toString(), pos.x, pos.y, SCALEZ*pos.z, 0.1, type);
				points.add(point);
				for (Move move:moves) {
					Pos shortestPath = shortestPathsList.get(move.targetPos);
					Pos currentShortestPath = currentShortestPathsList.get(move.targetPos);
					int lType = 33;
					double lSize = 0.005;
					if ((shortestPath != null) && pos.equals(shortestPath)) {
						lType = 31;
						lSize = 0.02;
					}
					if ((currentShortestPath != null) && pos.equals(currentShortestPath)) {
						lType = 30;
						lSize = 0.02;
					}
					Y23GUIOutput3D17.DDDObject line = new Y23GUIOutput3D17.DDDLineObject(pos.toString()+move.targetPos.toString(), pos.x, pos.y, SCALEZ*pos.z, move.targetPos.x, move.targetPos.y, SCALEZ*move.targetPos.z, lSize, lType);
					points.add(line);
				}
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.addStep(state.toString(), points);
		}
		private Map<Pos, Pos> recursiveGetShortestPath(Pos pos, int cnt) {
			Pos predecessor = shortestPathsList.get(pos);
			if ((predecessor==null) || predecessor.equals(pos)) {
				Map<Pos, Pos> result = new LinkedHashMap<>();
				result.put(pos, pos);
				return result;
			}
			Map<Pos, Pos> result = recursiveGetShortestPath(predecessor, cnt+1);
			result.put(pos, predecessor);
			return result;
		}
	}

	public static void mainPart1(String inputFile) {
		output = new Y23GUIOutput3D17("Day 08 Part II", true);

//		output = new Y23GUIOutput17("2023 day 17 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			world.addRow(data.row);
		}
		world.init();
//		System.out.println(world);
		world.createHeatlossDirectionGraph();
		int heatLoss = world.findMinimalHeatLoss();
		System.out.println("MINIMAL HEATLOSS: "+heatLoss);
	}

	public static void mainPart2(String inputFile) {
//		output = new Y23GUIOutput17("2023 day 17 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			world.addRow(data.row);
		}
		world.init();
//		System.out.println(world);
		world.createUltraHeatlossDirectionGraph();
		int heatLoss = world.findMinimalHeatLoss();
		System.out.println("MINIMAL HEATLOSS: "+heatLoss);		
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		URL url;
		System.out.println("--- PART I ---");
		url = Y23Day17Animation3D.class.getResource("/resources/input/aoc23day17/input-example.txt");
		mainPart1(new File(url.toURI()).toString());
//		mainPart1("../input-example.txt");
//		mainPart1("exercises/day17/Feri/input-example.txt");
//		mainPart1("exercises/day17/Feri/input.txt");               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
//		mainPart2("exercises/day17/Feri/input-example.txt");
//		mainPart2("exercises/day17/Feri/input-example-2.txt");
//		mainPart2("exercises/day17/Feri/input.txt");
		System.out.println("---------------");    
	}
	
}
