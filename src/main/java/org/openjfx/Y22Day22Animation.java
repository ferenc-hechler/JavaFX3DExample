package org.openjfx;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * see: https://adventofcode.com/2022/day/22
 */
public class Y22Day22Animation {
 
	/*
	 * example input: 
	 *
	 *       ..#
	 *       #..
	 *       ...
	 * ..#.....#
	 * ......#..
	 * .#...#...
	 *       ..#...
	 *       ....#.
	 *       .#....
	 * 
	 * 10R5L5R10L4R5L5
	 * 
	 */

	private static final String INPUT_RX_ROW   = "^([ .#]+)$";
	private static final String INPUT_RX_MOVES = "^([0-9RL]+)$";
	
	private static GUIOutput3D output;
	

	
	public static class InputData {
		String row;
		String moves;
		public boolean isRow() {
			return row != null;
		}
		@Override
		public String toString() {
			if (isRow()) {
				return row;
			}
			return moves;
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
			String line = scanner.nextLine();
			while (line.trim().length() == 0) {
				line = scanner.nextLine();
			}
			InputData data = new InputData();
			if (line.matches(INPUT_RX_ROW)) {
				data.row = line.replaceFirst(INPUT_RX_ROW, "$1");
			}
			else if (line.matches(INPUT_RX_MOVES)) {
				data.moves = line.replaceFirst(INPUT_RX_MOVES, "$1");
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
			return data;
		}
	}

	
	public static class Position {
		int x;
		int y;
		public Position(int x, int y) {
			this.x = x;
			this.y = y;
		}
		public Position min(Position other) {
			return new Position(Math.min(x,  other.x), Math.min(y,  other.y));
		}
		public Position max(Position other) {
			return new Position(Math.max(x,  other.x), Math.max(y,  other.y));
		}
		public int dist(Position other) {
			return Math.abs(x-other.x)+Math.abs(y-other.y);
		}
		@Override public int hashCode() { return Objects.hash(x, y); }
		@Override public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Position other = (Position) obj;
			return x == other.x && y == other.y;
		}
		@Override public String toString() { return "("+x+","+y+")"; }
	}	
	
	private static final int DIR_RIGHT = 0;
	private static final int DIR_DOWN  = 1;
	private static final int DIR_LEFT  = 2;
	private static final int DIR_UP    = 3;
	
	private static final char[] DIR_CHARS = {'>', 'v', '<', '^'};
	private static final int[] DIR_MOVEX = { 1,  0, -1,  0 };
	private static final int[] DIR_MOVEY = { 0,  1,  0, -1 };

	private static final int ROT_0     = 0; 
	private static final int ROT_LEFT  = 1; 
	private static final int ROT_180   = 2; 
	private static final int ROT_RIGHT = 3; 

	
	private static final int[][][] ROT = {
			// ROT_0
			{{1,0},{0,1}},
			// ROT_LEFT
			{{0,-1},{1,0}},
			// ROT_180
			{{-1,0},{0,-1}},
			// ROT_RIGHT
			{{0,1},{-1,0}}
	};

	static class SurfacePosition {
		int surface;
		int x;
		int y;
		int dir;
		public SurfacePosition(int surface, int x, int y, int dir) {
			this.surface = surface;
			this.x = x;
			this.y = y;
			this.dir = dir;
		}
		@Override
		public String toString() {
			return "S"+surface+"("+x+","+y+"|"+DIR_CHARS[dir]+")";
		}
	}

	
	public static class Pos3D {
		double x;
		double y;
		double z;
		public Pos3D(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		@Override
		public String toString() {
			return "("+x+","+y+","+z+")";
		}
		public Pos3D sub(Pos3D other) {
			return new Pos3D(x-other.x, y-other.y, z-other.z);
		}
		public Pos3D mul(double factor) {
			return new Pos3D(x*factor, y*factor, z*factor);
		}
		public Pos3D add(double factor, Pos3D vector) {
			return new Pos3D(x+factor*vector.x, y+factor*vector.y, z+factor*vector.z);
		}
		
	}
	
	
	static List<Pos3D> cornersPos3D;
	static Map<Integer, SurfaceDef> surfaceDefs;

	/**
	 *
	 *     6======7
	 *    /.     /| 
	 *   / .    / | 
	 *  /  5.../..8 
	 * 2==+===3  /
	 * | .    | / 
	 * |.     |/ 
	 * 1======4
	 *  
	 */

	static int[][] SURFACE_CORNERS = { 
			// (front)
			{2,3,4,1},
			// (top)
			{6,7,3,2},
			// (back)
			{5,8,7,6},
			// (bottom)
			{1,4,8,5},
			// (left)
			{6,2,1,5},
			// (right)
			{3,7,8,4} 
	};
	
	
	static class SurfaceDef {
		int id;
		int offsX;
		int offsY;
		Pos3D topLeftPos;
		Pos3D xUnitVector;
		Pos3D yUnitVector;
		public SurfaceDef(int id, int offsX, int offsY, Pos3D topLeftPos, Pos3D xUnitVector, Pos3D yUnitVector) {
			this.id = id;
			this.offsX = offsX;
			this.offsY = offsY;
			this.topLeftPos = topLeftPos;
			this.xUnitVector = xUnitVector;
			this.yUnitVector = yUnitVector;
		}
		
	}
	
	public static class World {
		int cubeSize;
		int maxHSurface;
		List<char[]> rowList;
		char[][] field;
		int rows;
		int cols;
		String movements;
		int movementsPosition;
		
		int lastSteps;
		char lastTurn;
		
		int surface;
		Position pos;
		int dir;

		Map<Integer, Integer> orientationDefs;

		Map<Position, Character> path;

		

		public World(int cubeSize) {
			this.cubeSize = cubeSize;
			this.rowList = new ArrayList<>();
			this.path = new LinkedHashMap<>();
		}
		public void addRow(String row) {
			rowList.add(row.toCharArray());
		}
		public void setMovements(String movements) {
			this.movements = movements;
			this.movementsPosition = 0;
		}
		public void init() {
			rows = rowList.size();
			cols = 0;
			field = new char[rows][];
			for (int i=0; i<rows; i++) {
				field[i] = rowList.get(i);
				cols = Math.max(cols, field[i].length);
			}
			maxHSurface = cols / cubeSize;
			autoDefineOrientations();
			dir = 0;
			pos = findStartPos();
			surface = maxHSurface*(pos.y / cubeSize) + (pos.x / cubeSize);
			pos = new Position(pos.x%cubeSize, pos.y%cubeSize);
			rememberPosition();
			lastSteps = 0;
			lastTurn = '?';
			initCorners();
		}
		
		/**
		 *
		 *     6======7
		 *    /.     /| 
		 *   / .    / | 
		 *  /  5.../..8 
		 * 2==+===3  /
		 * | .    | / 
		 * |.     |/ 
		 * 1======4
		 *  
		 */
		public void initCorners() {
			double width = cubeSize+1.0;
			double d = width / 2;
			cornersPos3D = new ArrayList<>();
			cornersPos3D.add(new Pos3D(-d, -d,  d));
			cornersPos3D.add(new Pos3D(-d,  d,  d));
			cornersPos3D.add(new Pos3D( d,  d,  d));
			cornersPos3D.add(new Pos3D( d, -d,  d));
			cornersPos3D.add(new Pos3D(-d, -d, -d));
			cornersPos3D.add(new Pos3D(-d,  d, -d));
			cornersPos3D.add(new Pos3D( d,  d, -d));
			cornersPos3D.add(new Pos3D( d, -d, -d));
			
			surfaceDefs = new LinkedHashMap<>();
			surfaceDefs.put(surface, createSD(surface, SURFACE_CORNERS[0], ROT_0));
			int topSurface = getOrientationTo(surface, DIR_UP);
			int topRot = getOrientationRot(surface, DIR_UP);
			surfaceDefs.put(topSurface, createSD(topSurface, SURFACE_CORNERS[1], (4-topRot)%4));
			int bottomSurface = getOrientationTo(surface, DIR_DOWN);
			int bottomRot = getOrientationRot(surface, DIR_DOWN);
			surfaceDefs.put(bottomSurface, createSD(bottomSurface, SURFACE_CORNERS[3], (4-bottomRot)%4));
			int leftSurface = getOrientationTo(surface, DIR_LEFT);
			int leftRot = getOrientationRot(surface, DIR_LEFT);
			surfaceDefs.put(leftSurface, createSD(leftSurface, SURFACE_CORNERS[4], (4-leftRot)%4));
			int rightSurface = getOrientationTo(surface, DIR_RIGHT);
			int rightRot = getOrientationRot(surface, DIR_RIGHT);
			surfaceDefs.put(rightSurface, createSD(rightSurface, SURFACE_CORNERS[5], (4-rightRot)%4));
			
			int top2backDir = (DIR_UP+topRot)%4;
			int backSurface = getOrientationTo(topSurface, top2backDir);
			int backRot = getOrientationRot(topSurface, top2backDir);
			surfaceDefs.put(backSurface, createSD(backSurface, SURFACE_CORNERS[2], (8-backRot-topRot)%4));
		}
		

		private SurfaceDef createSD(int sf, int[] corners, int rot) {
			double width = cubeSize+1;
			Pos3D topLeft = cornersPos3D.get(corners[(0+rot)%4]-1);
			Pos3D topRight = cornersPos3D.get(corners[(1+rot)%4]-1);
			Pos3D bottomLeft = cornersPos3D.get(corners[(3+rot)%4]-1);
			Pos3D xUnitVec = topRight.sub(topLeft).mul(1.0/width);
			Pos3D yUnitVec = bottomLeft.sub(topLeft).mul(1.0/width);
			return new SurfaceDef(sf, cubeSize*(sf%maxHSurface), cubeSize*(sf/maxHSurface), topLeft, xUnitVec, yUnitVec); 
		}
		
		private void autoDefineOrientations() {
			orientationDefs = new HashMap<>();
			
			int maxVSurface = rows/cubeSize;
			int maxSurface = maxHSurface * maxVSurface;
			for (int h=0; h<maxHSurface; h++) {
				for (int v=0; v<maxVSurface; v++) {
					if (surfaceExists(h, v)) {
						if (surfaceExists(h+1, v)) {
							orientation(v*maxHSurface+h, DIR_RIGHT, v*maxHSurface+h+1, ROT_0);
							orientation(v*maxHSurface+h+1, DIR_LEFT, v*maxHSurface+h, ROT_0);
						}
						if (surfaceExists(h, v+1)) {
							orientation(v*maxHSurface+h, DIR_DOWN, (v+1)*maxHSurface+h, ROT_0);
							orientation((v+1)*maxHSurface+h, DIR_UP, v*maxHSurface+h, ROT_0);
						}
					}
				}
			}
			
			boolean changed = true;
			while (changed) {
				System.out.println("#"+orientationDefs.size());
				changed = false;
				for (int from=0; from<maxSurface; from++) {
					
					Integer targetRight = getOrientationTo(from, DIR_RIGHT);
					if (targetRight != null) {
						
						Integer targetDown = getOrientationTo(from, DIR_DOWN);
						if (targetDown != null) {
							// from -> tr
							//   |
							//   v 
							//  td
							int rotTD = getOrientationRot(from, DIR_DOWN);
							int rotTR = getOrientationRot(from, DIR_RIGHT);
							int rotDown2Right = (ROT_RIGHT + (4-rotTD) + rotTR)%4;  
							if (orientation(targetDown, (DIR_RIGHT+rotTD)%4, targetRight, rotDown2Right)) {
								changed = true;
							}
							int rotRight2Down = (ROT_LEFT + rotTD + (4-rotTR))%4;  
							if (orientation(targetRight, (DIR_DOWN+rotTR)%4, targetDown, rotRight2Down)) {
								changed = true;
							}
						}

						Integer targetUp = getOrientationTo(from, DIR_UP);
						if (targetUp != null) {
							//  tu
							//   ^ 
							//   |
							// from -> tr
							int rotTU = getOrientationRot(from, DIR_UP);
							int rotTR = getOrientationRot(from, DIR_RIGHT);
							int rotUp2Right = (ROT_LEFT + (4-rotTU) + rotTR)%4;  
							if (orientation(targetUp, (DIR_RIGHT+rotTU)%4, targetRight, rotUp2Right)) {
								changed = true;
							}
							int rotRight2Up = (ROT_RIGHT + rotTU + (4-rotTR))%4;  
							if (orientation(targetRight, (DIR_UP+rotTR)%4, targetUp, rotRight2Up)) {
								changed = true;
							}
						}
					
					}
					
					Integer targetLeft = getOrientationTo(from, DIR_LEFT);
					if (targetLeft != null) {
						
						Integer targetDown = getOrientationTo(from, DIR_DOWN);
						if (targetDown != null) {
							// tl <- from 
							//        |
							//        v 
							//        td
							int rotTD = getOrientationRot(from, DIR_DOWN);
							int rotTL = getOrientationRot(from, DIR_LEFT);
							int rotDown2Left = (ROT_LEFT + (4-rotTD) + rotTL)%4;  
							if (orientation(targetDown, (DIR_LEFT+rotTD)%4, targetLeft, rotDown2Left)) {
								changed = true;
							}
							int rotLeft2Down = (ROT_RIGHT + rotTD + (4-rotTL))%4;  
							if (orientation(targetLeft, (DIR_DOWN+rotTL)%4, targetDown, rotLeft2Down)) {
								changed = true;
							}
						}

						Integer targetUp = getOrientationTo(from, DIR_UP);
						if (targetUp != null) {
							//        tu
							//        ^ 
							//        |
							// tl <- from 
							int rotTU = getOrientationRot(from, DIR_UP);
							int rotTL = getOrientationRot(from, DIR_LEFT);
							int rotUp2Left = (ROT_RIGHT + (4-rotTU) + rotTL)%4;  
							if (orientation(targetUp, (DIR_LEFT+rotTU)%4, targetLeft, rotUp2Left)) {
								changed = true;
							}
							int rotLeft2Up = (ROT_LEFT + rotTU + (4-rotTL))%4;  
							if (orientation(targetLeft, (DIR_UP+rotTL)%4, targetUp, rotLeft2Up)) {
								changed = true;
							}
						}

					}
					
				}
			}
		}

		private boolean surfaceExists(int h, int v) {
			return get(h*cubeSize, v*cubeSize) != ' ';
		}

		private boolean orientation(int from, int dir, int to, int rot) {
			String text = "orientation("+from+"|"+DIR_CHARS[dir]+" : "+to+"|"+rot*90+")";
			int fromdir = 10*from+dir;
			int torot = 10*to+rot;
			Integer oldValue = orientationDefs.put(fromdir, torot);
			if (oldValue == null) {
				System.out.println(text);
				return true;
			}
			if (oldValue != torot) {
				System.out.println("WRONG: "+text);
				throw new RuntimeException("mismatch");
			}
			return false;		
		}
		private Integer getOrientationTo(int from, int dir) {
			Integer result = orientationDefs.get(from*10+dir);
			return result == null ? null : result/10;
		}
		private int getOrientationRot(int from, int dir) {
			Integer result = orientationDefs.get(from*10+dir);
			return result == null ? null : result%10;
		}

		private Position findStartPos() {
			for (int y=0; y<rows; y++) {
				for (int x=0; x<cols; x++) {
					if (isFree(x,y)) {
						return new Position(x,y);
					}
				}
			}
			return null;
		}
		private boolean isFree(int x, int y) {
			return get(x,y) == '.';
		}
		private boolean isWall(int x, int y) {
			return get(x,y) == '#';
		}
		private boolean isWrap(int x, int y) {
			char c = get(x,y);
			return c == ' ' || c == '?';
		}
		public char get(int x, int y) {
			if (x<0 || x>=cols || y<0 || y>=rows || (x >= field[y].length)) {
				return ' ';
			}
			return field[y][x];
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(""+lastSteps+lastTurn+"\n");
			for (int y=0; y<rows; y++) {
				for (int x=0; x<cols; x++) {
					char c = get(x,y);
					Character step = path.get(new Position(x, y));
					if (step != null) {
						c = step;
					}
					result.append(c);
				}
				result.append("\n");
			}
			return result.toString();
		}
		public boolean hasMoves() {
			return peekMovementChar() != '\0';
		}
		public void nextMove() {
			int steps = nextMovementInt();
			char turnDir = nextMovementTurn();
			lastSteps = steps;
			lastTurn = turnDir;
			walk(steps);
			turn(turnDir);
		}
		private void turn(char turnDir) {
			if (turnDir == '\0') {
				return;
			}
			int offs = turnDir == 'R' ? 1 : 3;
			dir = (dir + offs) % 4;
			rememberPosition();
		}
		
		private void rememberPosition() {
			path.put(new Position(pos.x+cubeSize*(surface%maxHSurface), pos.y+cubeSize*(surface/maxHSurface)), DIR_CHARS[dir]);
		}
		
		private void walk(int steps) {
			rememberPosition();
			SurfacePosition nextSP = new SurfacePosition(surface, pos.x, pos.y, dir);
			for (int i=0; i<steps; i++) {
				SurfacePosition oldSP = nextSP;
				nextSP = singleStep(nextSP);
				if (nextSP == null) {
					nextSP = oldSP;
					break;
				}
				pos = new Position(nextSP.x, nextSP.y);
				dir = nextSP.dir;
				surface = nextSP.surface;
				rememberPosition();
			}
			pos = new Position(nextSP.x, nextSP.y);
			dir = nextSP.dir;
			surface = nextSP.surface;
		}
		private SurfacePosition singleStep(SurfacePosition nextSP) {
			int nextX = nextSP.x + DIR_MOVEX[nextSP.dir];
			int nextY = nextSP.y + DIR_MOVEY[nextSP.dir];
			int nextSurface = nextSP.surface;
			int nextDir = nextSP.dir;
			if ((nextX<0) || (nextX>=cubeSize) || (nextY<0) || (nextY>=cubeSize)) {
				nextSurface = getOrientationTo(nextSP.surface, nextSP.dir);
				int rot = getOrientationRot(nextSP.surface, nextSP.dir);
				nextDir = (nextSP.dir + rot) % 4;
				double xRot = ((nextX+cubeSize) %cubeSize)-0.5*(cubeSize-1);
				double yRot = ((nextY+cubeSize) %cubeSize)-0.5*(cubeSize-1);
				nextX = (int) (xRot*ROT[rot][0][0] + yRot*ROT[rot][0][1] + 0.5*(cubeSize-1) + 0.001);
				nextY = (int) (xRot*ROT[rot][1][0] + yRot*ROT[rot][1][1] + 0.5*(cubeSize-1) + 0.001);
			}
			if (isWall(nextSurface, nextX, nextY)) {
				return null;
			}
			return new SurfacePosition(nextSurface, nextX, nextY, nextDir);
		}
		
		private boolean isWall(int sf, int x, int y) {
			return get(sf, x, y) == '#';
		}
		private char get(int sf, int x, int y) {
			return get(x+cubeSize*(sf%maxHSurface), y+cubeSize*(sf/maxHSurface));
		}
		private char peekMovementChar() {
			if (movementsPosition >= movements.length()) {
				return '\0';
			}
			return movements.charAt(movementsPosition);
			
		}
		private char nextMovementChar() {
			char result = peekMovementChar();
			movementsPosition++;
			return result;
		}
		private char nextMovementTurn() {
			char result = nextMovementChar();
			if ((result != 'L') && (result != 'R')  && (result != '\0')) {
				throw new RuntimeException("invalid turn '"+result+"'");
			}
			return result;
		}
		private int nextMovementInt() {
			int result = 0;
			char c=peekMovementChar();
			while (c >= '0' && c <= '9') {
				nextMovementChar();
				result = result * 10 + (c-'0');
				c=peekMovementChar();
			}
			return result;
		}
		
		public void output() {
			List<GUIOutput3D.DDDObject> points = new ArrayList<>();
			points.add(new GUIOutput3D.DDDObject(0, 0, 0, 0.5*cubeSize, 2));
			for (SurfaceDef sDef:surfaceDefs.values()) {
				for (int y=0; y<cubeSize; y++) {
					for (int x=0; x<cubeSize; x++) {
						char c = get(sDef.id, x, y);
						Character step = path.get(new Position(x+sDef.offsX, y+sDef.offsY));
						if (step != null) {
							c = '*';
						}
						if ((c == '#') || (c == '*')) {
							int type = (c == '#') ? 0 : 11;
							if ((type == 11) && (sDef.id == surface) && (x == pos.x) && (y == pos.y)) {
								type = 13;
							}
							Pos3D outPos = sDef.topLeftPos.add(x+1, sDef.xUnitVector).add(y+1, sDef.yUnitVector);
							points.add(new GUIOutput3D.DDDObject(outPos.x, outPos.y, outPos.z, 0.5, type));
						}
					}
				}
			}
			if (output.scale == 1) {
				output.adjustScale(points);
			}
			output.setText("", points);
		}

	}
	
	public static void mainPart2(String inputFile, int cubeSize) {
		output = GUIOutput3D.open("Day 22 Part I");

		output = GUIOutput3D.open("Day 22 Part II");

		World world = new World(cubeSize);
		for (InputData data : new InputProcessor(inputFile)) {
			if (data.isRow()) {
				world.addRow(data.row);
			}
			else {
				world.setMovements(data.moves);
				break;
			}
		}
		world.init();
		world.output();
		while (world.hasMoves()) {
			world.nextMove();
			world.output();
		}
		System.out.println("SURFACE="+world.surface+", POS="+world.pos+" DIR="+world.dir);
		System.out.println("CODE: " + ((world.pos.y+world.cubeSize*(world.surface/world.maxHSurface)+1)*1000 + (world.pos.x+world.cubeSize*(world.surface%world.maxHSurface)+1)*4+world.dir));
	}

	
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("--- PART II ---");
//			mainPart2("exercises/day22/Feri/input-example.txt", 4);
//			mainPart2("exercises/day22/Feri/input-example2.txt", 3);
//			mainPart2("exercises/day22/Feri/input-example3.txt", 4);
//			mainPart2("exercises/day22/Feri/input.txt", 50);        
			mainPart2("C:\\Users\\feri\\git\\AdvenrOfCode\\AdvenrOfCode2022\\rsrc\\de\\hechler\\patrick\\aoc2022\\d22\\realdata", 50);        
		System.out.println("---------------");    // 
	}
	
}
