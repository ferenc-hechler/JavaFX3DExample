package org.openjfx;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjfx.Y23GUIOutput3D18.DDDObject;

/**
 * see: https://adventofcode.com/2023/day/22
 */
public class Y23Day22 {

	static Y23GUIOutput3D18 output;

	/*
	 * Example:
	 * 
	 * 1,0,1~1,2,1
	 * 0,0,2~2,0,2
	 * 0,2,3~2,2,3
	 * 0,0,4~0,2,4
	 * 2,0,5~2,2,5
	 * 0,1,6~2,1,6
	 * 1,1,8~1,1,9
	 * 
	 */

	private static final String INPUT_RX = "^([0-9]+),([0-9]+),([0-9]+)~([0-9]+),([0-9]+),([0-9]+)$";
	
	public static record InputData(Pos from, Pos to) {
		@Override public String toString() { return from+"->"+to; }
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
				int xFrom = Integer.parseInt(line.replaceFirst(INPUT_RX, "$1"));
				int yFrom = Integer.parseInt(line.replaceFirst(INPUT_RX, "$2"));
				int zFrom = Integer.parseInt(line.replaceFirst(INPUT_RX, "$3"));
				int xTo = Integer.parseInt(line.replaceFirst(INPUT_RX, "$4"));
				int yTo = Integer.parseInt(line.replaceFirst(INPUT_RX, "$5"));
				int zTo = Integer.parseInt(line.replaceFirst(INPUT_RX, "$6"));
				return new InputData(new Pos(xFrom,yFrom,zFrom), new Pos(xTo,yTo,zTo));
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}


	static record Pos(int x, int y, int z) {
		@Override public String toString() { return "("+x+","+y+","+z+")"; }
		public Pos add(int dx, int dy, int dz) {
			return new Pos(x+dx, y+dy, z+dz);
		}
		public Pos add(Pos other) {
			return new Pos(x+other.x, y+other.y, z+other.z);
		}
		public Pos subtract(Pos other) {
			return new Pos(x-other.x, y-other.y, z-other.z);
		}
		public int manhattenDist(Pos other) {
			return Math.abs(x-other.x) + Math.abs(y-other.y) + Math.abs(z-other.z);
		}
		public Pos div(int n) {
			return new Pos(x/n, y/n, z/n);
		}
	}

	static AtomicInteger brickID = new AtomicInteger();
	
	static record Brick(String id, Pos from, Pos to) {
		public Brick(Pos from, Pos to) {
			this("B"+brickID.incrementAndGet(), from, to);
			if ((from.x>to.x) || (from.y>to.y) && (from.z>to.z)) {
				throw new RuntimeException("from > to: "+this);
			}
			int cnt = 0;
			if (from.x!=to.x) {
				cnt++;
			}
			if (from.y!=to.y) {
				cnt++;
			}
			if (from.z!=to.z) {
				cnt++;
			}
			if (cnt>1) {
				throw new RuntimeException("more than two axis differ: "+this);
			}
		}
		public boolean overlaps(Brick other) {
			return  (from.x<=other.to.x) && 
					(from.y<=other.to.y) && 
					(from.z<=other.to.z) &&
				    (to.x>=other.from.x) && 
				    (to.y>=other.from.y) && 
				    (to.z>=other.from.z);
		}
		@Override public String toString() {
			String x = Integer.toString(from.x);
			String y = Integer.toString(from.y);
			String z = Integer.toString(from.z);
			if (to.x != from.x) {
				x = x+".."+to.x;
			}
			if (to.y != from.y) {
				y = y+".."+to.y;
			}
			if (to.z != from.z) {
				z = z+".."+to.z;
			}
			return id+"("+x+","+y+","+z+")";
		}
		public Brick down() {
			return new Brick(id, from.add(0,0,-1), to.add(0,0,-1));
		}
	}

	
	public static class World {
		List<Brick> bricks;
		Set<Brick> disintegrationCandidates;
		public World() {
			this.bricks = new ArrayList<>();
			this.disintegrationCandidates = new LinkedHashSet<>();
		}
		public World(List<Brick> bricks) {
			this.bricks = new ArrayList<>(bricks);
		}
		public World copy() {
			return new World(bricks);
		}
		public void addBrick(Brick brick) {
			this.bricks.add(brick);
		}
		public int bricksFall() {
			int numChanged = 0;
			List<Brick> bricksToFall = new ArrayList<>(bricks);
			List<Brick> newBricks = new ArrayList<>();
			bricksToFall.sort((b1, b2)->Integer.compare(b1.from.z, b2.from.z));
			while (!bricksToFall.isEmpty()) {
				Brick brickToFall = bricksToFall.remove(0);
				Brick fallenBrick = brickToFall;
				Brick brickDown = brickToFall.down();
				if (brickDown.from.z>0) {
					if (checkNoOverlap(brickDown, newBricks)) {
						if (checkNoOverlap(brickDown, newBricks)) {
							numChanged++;
							fallenBrick = brickDown;
						}
					}
				}
				newBricks.add(fallenBrick);
			}
			bricks = newBricks;
			return numChanged; 
		}
		private boolean checkNoOverlap(Brick brick, List<Brick> otherBricks) {
			for (Brick otherBrick:otherBricks) {
				if (brick.overlaps(otherBrick)) {
					return false;
				}
			}
			return true;
		}
		@Override public String toString() {
			return bricks.toString();
		}
		public Set<Brick> calcDisintegrationCandidates() {
			disintegrationCandidates = new LinkedHashSet<>();
			for (Brick brickToDisintegrate:bricks) {
				World testWorld = copy();
				testWorld.bricks.remove(brickToDisintegrate);
				if (testWorld.bricksFall() == 0) {
					disintegrationCandidates.add(brickToDisintegrate);
				}
			}
			return disintegrationCandidates;
		}
		public int calcSumFalling() {
			int result = 0;
			for (Brick brickToDisintegrate:bricks) {
				World testWorld = copy();
				testWorld.bricks.remove(brickToDisintegrate);
				result += testWorld.bricksFall();
			}
			return result;
		}
		public void show3D(String info) {
			Map<Pos, String> duplicateCheck = new LinkedHashMap<>();
			List<Y23GUIOutput3D18.DDDObject> lines = new ArrayList<>();
			for (Brick brick:bricks) {
				int col = 0;
				if (disintegrationCandidates.contains(brick)) {
					col = 3;
				}
				List<Y23GUIOutput3D18.DDDObject> blocks = createBlocks(brick.id, brick.from, brick.to, col);
//				Y23GUIOutput3D18.DDDObject line = new Y23GUIOutput3D18.DDDLineObject(brick.id, 
//						brick.from.x,brick.from.y,brick.from.z, 
//						brick.to.x,brick.to.y,brick.to.z, 
//						0.5, 30+col);
				lines.addAll(blocks);
				for (DDDObject block:blocks) {
					String conflict = duplicateCheck.put(new Pos((int)block.x, (int)block.y, (int)block.z), brick.id);
					if (conflict != null) {
						//  B692(9,7,1..3) conflict: B522 B522(9,7,4..6)
						throw new RuntimeException("duplicate pos "+brick+" conflict: "+conflict);
					}
				}
			}
			if (output.scale == 1) {
				output.adjustScale(lines);
			}
			output.addStep(info, lines);
		}
		private List<DDDObject> createBlocks(String id, Pos from, Pos to, int col) {
			List<DDDObject> result = new ArrayList<>();
			int dist = from.manhattenDist(to);
			Pos v = to.subtract(from);
			if (dist > 0) {
				v = v.div(dist);
			}
			Pos current = from;
			for (int i=0; i<=dist; i++) {
				DDDObject block = new DDDObject(id+"-"+i, 
						current.x, -current.z, current.y, 
						1, 0+col);
				
				result.add(block);
				current = current.add(v);
			}
			return result;
		}
		
	}

	public static void mainPart1(String inputFile) {
		output = new Y23GUIOutput3D18("Day 22 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
			world.addBrick(new Brick(data.from, data.to));
		}
		System.out.println(world);
		world.show3D("init");
		while (world.bricksFall() > 0) {
			System.out.println(world);
			world.show3D("falling");
		}
		System.out.println("--- STOPPED ---");
		Set<Brick> disintegrationCandidates = world.calcDisintegrationCandidates();
		world.show3D("disintegration candidates");
		System.out.println("DISINTEGRATION CANDIDATES: "+disintegrationCandidates);
		System.out.println("                           #"+disintegrationCandidates.size());
	}

	
	public static void mainPart2(String inputFile) {
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
			world.addBrick(new Brick(data.from, data.to));
		}
		while (world.bricksFall() > 0) {
			world.show3D("falling");
		}
		System.out.println("SUM FALLING: "+world.calcSumFalling());
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		URL url;
		System.out.println("--- PART I ---");
//		mainPart1("exercises/day22/Feri/input-example.txt");
//		mainPart1("exercises/day22/Feri/input.txt");           // < 763    
		
//		url = Y23Day22.class.getResource("/resources/input/aoc23day22/input-example.txt");
		url = Y23Day22.class.getResource("/resources/input/aoc23day22/input.txt");
		mainPart1(new File(url.toURI()).toString());
		
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
//		mainPart2("exercises/day22/Feri/input-example.txt");
//		mainPart2("exercises/day22/Feri/input.txt");
		url = Y23Day22.class.getResource("/resources/input/aoc23day22/input.txt");
		mainPart2(new File(url.toURI()).toString());
		System.out.println("---------------");    
	}
	
}
