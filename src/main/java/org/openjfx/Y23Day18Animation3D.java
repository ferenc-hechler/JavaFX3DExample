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

import org.openjfx.Y23Day17Animation3D.Move;
import org.openjfx.Y23Day17Animation3D.Pos;

/**
 * see: https://adventofcode.com/2023/day/18
 */
public class Y23Day18Animation3D {

	static Y23GUIOutput3D18 output;
//	static Y23GUIOutput18 output;

	/*
	 * Example:
	 * 
	 * R 6 (#70c710)
	 * D 5 (#0dc571)
	 * L 2 (#5713f0)
	 * D 2 (#d2c081)
	 * R 2 (#59c680)
	 * D 2 (#411b91)
	 * L 5 (#8ceee2)
	 * U 2 (#caa173)
	 * L 1 (#1b58a2)
	 * U 2 (#caa171)
	 * R 2 (#7807d2)
	 * U 3 (#a77fa3)
	 * L 2 (#015232)
	 * U 2 (#7a21e3)
	 * 
	 */

	private static final String INPUT_RX = "^([RDLU]) ([0-9]+) [(]#([0-9a-f]{5})([0-3])[)]$";
	
	public static record InputData(char dir, int steps, int color, char cDir, long cSteps) {
		@Override public String toString() { return dir+" "+steps+"(#"+Integer.toHexString(color)+") / "+cDir+" "+cSteps; }
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
				char dir = line.replaceFirst(INPUT_RX, "$1").charAt(0);
				int steps = Integer.parseInt(line.replaceFirst(INPUT_RX, "$2"));
				String hex5 = line.replaceFirst(INPUT_RX, "$3");
				int dirNum = line.replaceFirst(INPUT_RX, "$4").charAt(0)-'0';
				char cDir = DIR_LETTERS.charAt(dirNum);
				long cSteps = Integer.parseInt(hex5, 16);
				int color = Integer.parseInt(hex5+dirNum, 16);
				return new InputData(dir, steps, color, cDir, cSteps);
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}

	static String DIRS            = ">v<^";
	static String DIR_LETTERS     = "RDLU";
	static int[]  DIR_ADD_X 	  = { 1,   0,  -1,   0};
	static int[]  DIR_ADD_Y 	  = { 0,   1,   0,  -1};
	
	static int DIR_EAST = 0;
	static int DIR_SOUTH = 1;
	static int DIR_WEST = 2;
	static int DIR_NORTH = 3;
	
	static int DIR_ROT_LEFT = 3;
	static int DIR_ROT_RIGHT = 1;
	
	static int rot(int dir, int rot) { return (dir+rot+4)%4; }

	
	static record Pos(long x, long y) {
		Pos move(int dir) {
			return new Pos(x+DIR_ADD_X[dir], y+DIR_ADD_Y[dir]);
		}		
		Pos move(int dir, long steps) {
			return new Pos(x+steps*DIR_ADD_X[dir], y+steps*DIR_ADD_Y[dir]);
		}		
		public Pos min(Pos other) {
			if ((x<=other.x) && (y<=other.y)) {
				return this;
			}
			if ((other.x<=x) && (other.y<=y)) {
				return other;
			}
			return new Pos(Math.min(x,  other.x), Math.min(y,  other.y));
		}
		public Pos max(Pos other) {
			if ((x>=other.x) && (y>=other.y)) {
				return this;
			}
			if ((other.x>=x) && (other.y>=y)) {
				return other;
			}
			return new Pos(Math.max(x,  other.x), Math.max(y,  other.y));
		}
		@Override public String toString() { return "("+x+","+y+")"; }
		public List<Pos> getNeighbours() {
			List<Pos> result = new ArrayList<>();
			result.add(move(DIR_EAST));
			result.add(move(DIR_SOUTH));
			result.add(move(DIR_WEST));
			result.add(move(DIR_NORTH));
			return result;
		}
	}
	
	static record Beam(Pos pos, int dir) {
		public Beam move() {
			return new Beam(pos.move(dir), dir);
		}
		@Override public String toString() { return "B["+pos+"|"+DIRS.charAt(dir)+"]"; }
	}
	
	public static class World {
		Map<Pos, Integer> field;
		Set<Pos> outside;
		int ticks;
		Pos startPos;
		Pos currentPos;
		Pos maxPos;
		Pos minPos;
		public World() {
			this.field = new LinkedHashMap<>();
			this.outside = new LinkedHashSet<>();
			this.ticks = 0;
			this.startPos = new Pos(0,0);
			this.currentPos = startPos;
			this.minPos = currentPos;
			this.maxPos = currentPos;
			this.currentPos = new Pos(0,0);
		}
		public void move(InputData move) {
			int dir = DIR_LETTERS.indexOf(move.dir);
			for (int i=0; i<move.steps; i++) {
				currentPos = currentPos.move(dir);
				field.put(currentPos, move.color);
			}
			minPos = minPos.min(currentPos);
			maxPos = maxPos.max(currentPos);
		}
		@Override public String toString() {
			StringBuilder result = new StringBuilder();
			for (long y=minPos.y; y<=maxPos.y; y++) {
				for (long x=minPos.x; x<=maxPos.x; x++) {
					Integer col = get(x, y);
					char c = (col == null) ? '.' : '#';
					result.append(c);
				}
				result.append("\n");
			}
			return result.toString();
		}
		public String showBorder() {
			StringBuilder result = new StringBuilder();
			for (long y=minPos.y-2; y<=maxPos.y+2; y++) {
				for (long x=minPos.x-2; x<=maxPos.x+2; x++) {
					result.append(getChar(x, y, 1));
				}
				result.append("\n");
			}
			return result.toString();
		}
		public void fillOutside() {
			outside.clear();
			Set<Pos> currentPositions = new LinkedHashSet<>();
			currentPositions.add(new Pos(minPos.x-1, minPos.y-1));
			while (!currentPositions.isEmpty()) {
				Pos pos = currentPositions.iterator().next();
				currentPositions.remove(pos);
				if (getChar(pos.x,  pos.y,  1) != '.') {
					continue;
				}
				if (outside.contains(pos)) {
					continue;
				}
				outside.add(pos);
				currentPositions.addAll(pos.getNeighbours());
			}
		}
		private Integer get(long x, long y) {
			return field.get(new Pos(x,y));
		}
		private char getChar(long x, long y) {
			return getChar(x,y,0);
		}
		private char getChar(long x, long y, int border) {
			if (outside.contains(new Pos(x,y))) {
				return 'o';
			}
			if ((x<minPos.x-border) || (x>maxPos.x+border) || (y<minPos.y-border) || (y>maxPos.y+border)) {
				return '~';
			}
			if ((x<minPos.x) || (x>maxPos.x) || (y<minPos.y) || (y>maxPos.y)) {
				return '.';
			}
			Integer col = field.get(new Pos(x,y));
			return (col == null) ? '.' : '#';
		}
		public int countCubicmeters() {
			int result = 0;
			for (long y=minPos.y; y<=maxPos.y; y++) {
				for (long x=minPos.x; x<=maxPos.x; x++) {
					char c = getChar(x, y);
					if ((c == '.') || (c == '#')) {
						result++;
					}
				}
			}
			return result;
		}
	}

	static record HLine(long fromX, long toX, long y) {
		public long size() {
			return toX-fromX+1;
		}
		@Override public String toString() {
			return "("+fromX+".."+toX+","+y+")";
		}
	}
	static record VLine(long x, long fromY, long toY) {
		public boolean overlaps(VLine other) {
			return (x == other.x) && (fromY<=other.toY) && (toY>=other.fromY); 
		}
		public long size() {
			return toY-fromY+1;
		}
		@Override public String toString() {
			return "("+x+","+fromY+".."+toY+")";
		}
	}
	
	static record Area(long fromX, long toX, long fromY, long toY) {
		@Override public String toString() {
			return "A["+fromX+".."+toX+","+fromY+".."+toY+"]";
		}
		public long calcSize() {
			return width()*height();
		}
		public long width() {
			return toX-fromX+1;
		}
		public long height() {
			return toY-fromY+1;
		}
	}
	
	public static class World2 {
		List<HLine> hLines;
		List<VLine> vLines;
		List<VLine> innerVLines;
		List<Area> fillAreas;
		
		Map<Pos, HLine> hLineEndpoints;
		Map<Pos, VLine> vLineEndpoints;
		Pos startPos;
		Pos currentPos;
		Pos maxPos;
		Pos minPos;
		public World2() {
			this.hLines = new ArrayList<>();
			this.vLines = new ArrayList<>();
			this.hLineEndpoints = new LinkedHashMap<>();
			this.vLineEndpoints = new LinkedHashMap<>();
			this.startPos = new Pos(0,0);
			this.currentPos = startPos;
			this.minPos = currentPos;
			this.maxPos = currentPos;
			this.fillAreas = new ArrayList<>();
			this.innerVLines = new ArrayList<>();
		}
		public void move(char cDir, long steps) {
			int dir = DIR_LETTERS.indexOf(cDir);
			Pos nextPos = currentPos.move(dir, steps);
			if ((dir == DIR_EAST) || (dir == DIR_WEST)) {
				HLine hLine = new HLine(nextPos.min(currentPos).x, nextPos.max(currentPos).x, nextPos.y);
				hLines.add(hLine);
				if (hLineEndpoints.put(currentPos, hLine) != null) {
					throw new RuntimeException("duplicate HLine Endpoint "+currentPos);
				};
				if (hLineEndpoints.put(nextPos, hLine) != null) {
					throw new RuntimeException("duplicate HLine Endpoint "+nextPos);
				};
			}
			else {
				VLine vLine = new VLine(nextPos.x, nextPos.min(currentPos).y, nextPos.max(currentPos).y);
				vLines.add(vLine);
				if (vLineEndpoints.put(currentPos, vLine) != null) {
					throw new RuntimeException("duplicate VLine Endpoint "+currentPos);
				};
				if (vLineEndpoints.put(nextPos, vLine) != null) {
					throw new RuntimeException("duplicate VLine Endpoint "+nextPos);
				};
			}
			currentPos = nextPos;
			minPos = minPos.min(currentPos);
			maxPos = maxPos.max(currentPos);
		}
		@Override public String toString() {
			return "SIZE: "+(hLines.size()+vLines.size())+", MIN: "+minPos+", MAX: "+maxPos;
		}
		public long countCubicmeters() {
			long result = 0;
			List<Long> segments = vLines.stream().map(vl->vl.x).sorted().distinct().toList();
			System.out.println(segments);
			long subResult = 0;
			for (int i=0; i<segments.size()-1; i++) {
				long fromX = segments.get(i);
				long toX = segments.get(i+1);
				List<HLine> crossingHLines = hLines.stream().filter(hl->hl.fromX<=fromX && hl.toX>=toX).sorted((hl1,hl2)->Long.compare(hl1.y, hl2.y)).toList();
				System.out.println("from:"+fromX+", to: "+toX+" HLines: "+crossingHLines);
				for (int j=0; j<crossingHLines.size(); j+=2) {
					HLine hLine1 = crossingHLines.get(j);
					HLine hLine2 = crossingHLines.get(j+1);
					Area area = new Area(fromX+1, toX-1, hLine1.y+1, hLine2.y-1);
					if ((area.fromX<=area.toX) && (area.fromY<=area.toY)) {
						subResult += area.calcSize();
						System.out.println(area+" -> "+area.calcSize());
						fillAreas.add(area);
					}
					if (area.fromY <= area.toY) {
						addInnerVLine(fromX, area.fromY, area.toY);
						addInnerVLine(toX, area.fromY, area.toY);
					}
				}
			}
			System.out.println("AREAS:" + subResult);
			result += subResult;
			subResult = 0;
			for (VLine innerVLine:innerVLines) {
				System.out.println(innerVLine+" -> "+innerVLine.size());
				subResult += innerVLine.size();
			}
			System.out.println("INNERVL:" + result);
			result += subResult;
			subResult = 0;
			for (VLine vLine:vLines) {
				System.out.println(vLine+" -> "+(vLine.size()-1));
				subResult += (vLine.size()-1);
			}
			System.out.println("VL:" + subResult);
			result += subResult;
			subResult = 0;
			for (HLine hLine:hLines) {
				System.out.println(hLine+" -> "+(hLine.size()-1));
				subResult += (hLine.size()-1);
			}
			System.out.println("HL:" + subResult);
			result += subResult;
			return result;
		}
		private void addInnerVLine(long x, long fromY, long toY) {
			VLine innerVL = new VLine(x, fromY, toY);
			List<VLine> splitInnerVLines = new ArrayList<>();
			splitInnerVLines.add(innerVL);
			boolean changed = true;
			while (changed) {
				changed = false;
				for (VLine outerVLine:vLines) {
					changed = removeOverlaps(splitInnerVLines, outerVLine);
					if (changed) {
						break;
					}
				}
				if (changed) {
					continue;
				}
				for (VLine otherInnerVLine:innerVLines) {
					changed = removeOverlaps(splitInnerVLines, otherInnerVLine);
					if (changed) {
						break;
					}
				}
			}
			innerVLines.addAll(splitInnerVLines);
		}
		private boolean removeOverlaps(List<VLine> newVLines, VLine vLineToCheck) {
			for (VLine newVLine:newVLines) {
				if (newVLine.overlaps(vLineToCheck)) {
					newVLines.remove(newVLine);
					long minY = Math.min(newVLine.fromY, vLineToCheck.fromY);
					long maxY = Math.max(newVLine.toY, vLineToCheck.toY);
					if (minY < vLineToCheck.fromY) {
						newVLines.add(new VLine(newVLine.x, minY, vLineToCheck.fromY-1));
					}
					if (maxY > vLineToCheck.toY) {
						newVLines.add(new VLine(newVLine.x, vLineToCheck.toY+1, maxY));
					}
					return true;
				}
			}
			return false;
		}
		
		public void show3D(String info) {
			long Z_OFFSET=Math.max(1,(maxPos.y-minPos.y)/1000);
			double LINE_WIDTH=Math.max(0.1,(maxPos.y-minPos.y)/1000.0);
			Y23GUIOutput3D18.DDDObject line;
			List<Y23GUIOutput3D18.DDDObject> lines = new ArrayList<>();
			for (HLine hLine:hLines) {
				double lSize = LINE_WIDTH;
				int lType = 30;
				line = new Y23GUIOutput3D18.DDDLineObject(hLine.toString(), hLine.fromX,hLine.y,0, hLine.toX,hLine.y,0, lSize, lType);
				lines.add(line);
			}
			for (VLine vLine:vLines) {
				double lSize = LINE_WIDTH;
				int lType = 30;
				line = new Y23GUIOutput3D18.DDDLineObject(vLine.toString(), vLine.x,vLine.fromY,0, vLine.x,vLine.toY,0, lSize, lType);
				lines.add(line);
			}
			for (Area area:fillAreas) {
				int lType = 51;
				double width = area.width();
				double height = area.height();
				double size = (float)height;
				double ratio = width/height;
				Y23GUIOutput3D18.DDDAreaObject dddao = new Y23GUIOutput3D18.DDDAreaObject(area.toString(), 0.5*(area.fromX+area.toX),0.5*(area.fromY+area.toY),Z_OFFSET, ratio, size, lType);
				dddao.setSizeFixed();
				lines.add(dddao);
			}
			for (VLine innerVLine:innerVLines) {
				double lSize = LINE_WIDTH;
				int lType = 32;
				line = new Y23GUIOutput3D18.DDDLineObject(innerVLine.toString(), innerVLine.x,innerVLine.fromY,2*Z_OFFSET, innerVLine.x,innerVLine.toY,2*Z_OFFSET, lSize, lType);
				lines.add(line);
			}
			if (output.scale == 1) {
				output.adjustScale(lines);
			}
			output.addStep(info, lines);
		}
	}

	public static void mainPart1(String inputFile) {
//		output = new Y23GUIOutput18("Day 18 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
			world.move(data);
		}
//		System.out.println(world);
//		output.addStep(world.toString());
//		System.out.println(world.showBorder());
//		output.addStep(world.showBorder());
		world.fillOutside();
//		output.addStep(world.showBorder());
		System.out.println(world.countCubicmeters());
	}

	


	public static void mainPart2(String inputFile) {
		output = new Y23GUIOutput3D18("day 18 part II", true);
		World2 world2 = new World2();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
//			world2.move(data.dir, data.steps);
			world2.move(data.cDir, data.cSteps);
		}
		world2.show3D("init");
		System.out.println(world2);
		System.out.println(world2.countCubicmeters());
		world2.show3D("fill areas");
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		System.out.println("--- PART I ---");
//		mainPart1("exercises/day18/Feri/input-example.txt");
//		mainPart1("exercises/day18/Feri/input.txt");               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
		URL url;
		System.out.println("--- PART I ---");
		url = Y23Day18Animation3D.class.getResource("/resources/input/aoc23day18/input-example.txt");
//		url = Y23Day18Animation.class.getResource("/resources/input/aoc23day18/input.txt");
		mainPart2(new File(url.toURI()).toString());
//		mainPart2("exercises/day18/Feri/input-example.txt");
//		mainPart2("exercises/day18/Feri/input.txt");
		System.out.println("---------------");    
	}
	
}
