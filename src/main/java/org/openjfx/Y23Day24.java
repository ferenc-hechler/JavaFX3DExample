package org.openjfx;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjfx.Y23GUIOutput3D18.DDDLineObject;
import org.openjfx.Y23GUIOutput3D18.DDDObject;


/**
 * see: https://adventofcode.com/2023/day/24
 */
public class Y23Day24 {

	static Y23GUIOutput3D18 output;

	/*
	 * Example:
	 * 
	 * 19, 13, 30 @ -2,  1, -2
	 * 18, 19, 22 @ -1, -1, -2
	 * 20, 25, 34 @ -2, -2, -4
	 * 12, 31, 28 @ -1, -2, -1
	 * 20, 19, 15 @  1, -5, -3
	 * 
	 */

	private static final String INPUT_RX = "^([-0-9]+), *([-0-9]+), *([-0-9]+) *@ *([-0-9]+), *([-0-9]+), *([-0-9]+)$";
	
	public static record InputData(Pos pos, Pos v) {
		@Override public String toString() { return pos+"->"+v; }
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
				long xFrom = Long.parseLong(line.replaceFirst(INPUT_RX, "$1"));
				long yFrom = Long.parseLong(line.replaceFirst(INPUT_RX, "$2"));
				long zFrom = Long.parseLong(line.replaceFirst(INPUT_RX, "$3"));
				long vxTo = Long.parseLong(line.replaceFirst(INPUT_RX, "$4"));
				long vyTo = Long.parseLong(line.replaceFirst(INPUT_RX, "$5"));
				long vzTo = Long.parseLong(line.replaceFirst(INPUT_RX, "$6"));
				return new InputData(new Pos(xFrom,yFrom,zFrom), new Pos(vxTo,vyTo,vzTo));
			}
			else {
				throw new RuntimeException("invalid line '"+line+"'");
			}
		}
	}


	static record Pos(double x, double y, double z) {
		@Override public String toString() { return "("+x+","+y+","+z+")"; }
		public Pos add(double dx, double dy, double dz) {
			return new Pos(x+dx, y+dy, z+dz);
		}
		public Pos add(Pos other) {
			return new Pos(x+other.x, y+other.y, z+other.z);
		}
		public Pos subtract(Pos other) {
			return new Pos(x-other.x, y-other.y, z-other.z);
		}
		public double manhattenDist(Pos other) {
			return Math.abs(x-other.x) + Math.abs(y-other.y) + Math.abs(z-other.z);
		}
		public Pos multiply(double k) {
			return new Pos(k*x, k*y, k*z);
		}
		public double magnitude() {
			return Math.sqrt(x*x+y*y+z*z);  
		}
		public Pos normalize() {
			double mag = magnitude();
			if (mag == 0) {
				return this;
			}
			return multiply(1/mag);  
		}
		// https://matheguru.com/lineare-algebra/kreuzprodukt-vektorprodukt.html?utm_content=cmp-true
		public Pos cross(Pos other) {
			return new Pos(y*other.z-z*other.y, z*other.x-x*other.z, x*other.y-y*other.x);
		}
		public double dot(Pos other) {
			return x*other.x + y*other.y + z*other.z;
		}
		public Pos min(Pos pos) {
			if ((x<=pos.x) && (y<=pos.y) && (z<=pos.z)) {
				return this;
			}
			return new Pos(Math.min(x, pos.x), Math.min(y, pos.y), Math.min(z, pos.z));
		}
		public Pos max(Pos pos) {
			if ((x>=pos.x) && (y>=pos.y) && (z>=pos.z)) {
				return this;
			}
			return new Pos(Math.max(x, pos.x), Math.max(y, pos.y), Math.max(z, pos.z));
		}
	}

	static AtomicInteger hailID = new AtomicInteger();
	
	static record Hail(int id, Pos pos, Pos v) {
		public Hail(Pos pos, Pos v) {
			this(hailID.incrementAndGet(), pos, v);
		}

		// http://walter.bislins.ch/blog/index.asp?page=Schnittpunkt+zweier+Geraden+berechnen+%28JavaScript%29
		// 
		//		function IntersectLines( P, r, Q, s ) {
		//		  // line1 = P + lambda1 * r
		//		  // line2 = Q + lambda2 * s
		//		  // r and s must be normalized (length = 1)
		//		  // returns intersection point O of line1 with line2 = [ Ox, Oy ] 
		//		  // returns null if lines do not intersect or are identical
		//		  var PQx = Q[0] - P[0];
		//		  var PQy = Q[1] - P[1];
		//		  var rx = r[0];
		//		  var ry = r[1];
		//		  var rxt = -ry;
		//		  var ryt = rx;
		//		  var qx = PQx * rx + PQy * ry;
		//		  var qy = PQx * rxt + PQy * ryt;
		//		  var sx = s[0] * rx + s[1] * ry;
		//		  var sy = s[0] * rxt + s[1] * ryt;
		//		  // if lines are identical or do not cross...
		//		  if (sy == 0) return null;
		//		  var a = qx - qy * sx / sy;
		//		  return [ P[0] + a * rx, P[1] + a * ry ];
		//		}
	
		public Pos intersectXY(Y23Day24.Hail other) {
			Pos vNorm = new Pos(v.x, v.y, 0);
			double vFactor = vNorm.magnitude();
			vNorm = vNorm.normalize();
			Pos vOtherNorm = new Pos(other.v.x, other.v.y, 0).normalize();
			
			double PQx = other.pos.x - pos.x;
			double PQy = other.pos.y - pos.y;
			double rx = vNorm.x;
			double ry = vNorm.y;
			double rxt = -ry;
			double ryt = rx;
			double qx = PQx * rx + PQy * ry;
			double qy = PQx * rxt + PQy * ryt;
			double s0 = vOtherNorm.x;
			double s1 = vOtherNorm.y;
			double sx = s0 * rx + s1 * ry;
			double sy = s0 * rxt + s1 * ryt;
			if (sy == 0) {
				// lines are parallel
				return null;
			}
			double a = qx - qy * sx / sy;
			if (a<0) {
				return null;
			}
			double resultX = pos.x + a*v.x/vFactor;
			double resultY = pos.y + a*v.y/vFactor;
			double resultZ = pos.z + a*v.z/vFactor;
			return new Pos(resultX, resultY, resultZ);
		}
		public Pos intersectXYZ(Y23Day24.Hail other) {
			// TODO: add delta-z check, switch x/z or y/z if x/y is parallel 
			Pos vNorm = new Pos(v.x, v.y, 0);
			double vFactor = vNorm.magnitude();
			vNorm = vNorm.normalize();
			Pos vOtherNorm = new Pos(other.v.x, other.v.y, 0).normalize();
			
			double PQx = other.pos.x - pos.x;
			double PQy = other.pos.y - pos.y;
			double rx = vNorm.x;
			double ry = vNorm.y;
			double rxt = -ry;
			double ryt = rx;
			double qx = PQx * rx + PQy * ry;
			double qy = PQx * rxt + PQy * ryt;
			double s0 = vOtherNorm.x;
			double s1 = vOtherNorm.y;
			double sx = s0 * rx + s1 * ry;
			double sy = s0 * rxt + s1 * ryt;
			if (sy == 0) {
				// lines are parallel
				return null;
			}
			double a = qx - qy * sx / sy;
			double resultX = pos.x + a*v.x/vFactor;
			double resultY = pos.y + a*v.y/vFactor;
			double resultZ = pos.z + a*v.z/vFactor;
			return new Pos(resultX, resultY, resultZ);
		}
		// https://math.stackexchange.com/questions/2213165/find-shortest-distance-between-lines-in-3d
		// 
		//		// def distance_from_two_lines(e1, e2, r1, r2):
		//	    # e1, e2 = Direction vector
		//	    	    # r1, r2 = Point where the line passes through
		//
		//	    	    # Find the unit vector perpendicular to both lines
		//	    	    n = np.cross(e1, e2)
		//	    	    n /= np.linalg.norm(n)
		//
		//	    	    # Calculate distance
		//	    	    d = np.dot(n, r1 - r2)
		//
		//	    	    return d
		public Pos closestPos(Hail other) {
			Pos e1 = v.normalize();
			Pos e2 = other.v.normalize();
			Pos n = e1.cross(e2);
			n = n.normalize();
			double dist = n.dot(other.pos.subtract(pos));
			Pos movedOtherPos = other.pos.add(n.multiply(-dist));
			Hail movedOther = new Hail(movedOtherPos, other.v); 
			Pos result = movedOther.intersectXYZ(this);
			return result;
		}
		
		public Hail move() {
			return new Hail(id, pos.add(v), v);
		}

		public Hail move(double factor) {
			return new Hail(id, pos.add(v.multiply(factor)), v);
		}
		// https://www.mathematik-oberstufe.de/vektoren/a/abstand-punkt-gerade-formel.html
		public double distance(Pos otherPos) {
			return otherPos.subtract(pos).cross(v).magnitude()/v.magnitude();
		}

	}
	
	static double FSIZE = 5000000000000.0;

	static record Timeslot(double from, double to) {}
	
	public static class World {
		List<Hail> hails;
		int ticks;
		public World() {
			this.hails= new ArrayList<>();
			this.ticks = 0;
		}
		public void addHail(Hail hail) {
			this.hails.add(hail);
		}
		@Override public String toString() {
			return hails.toString();
		}
		public long countIntersectionsXY(double minTargetArea, double maxTargetArea) {
			long result = 0; 
			for (int i=0; i<hails.size(); i++) {
				Hail hail1 = hails.get(i);
				for (int j=i+1; j<hails.size(); j++) {
					Hail hail2 = hails.get(j);
					Pos pos1 = hail1.intersectXY(hail2);
					Pos pos2 = hail2.intersectXY(hail1);
					if ((pos1 != null) && (pos2 != null)
							&& (pos1.x>=minTargetArea) && (pos1.x<=maxTargetArea) 
							&& (pos1.y>=minTargetArea) && (pos1.y<=maxTargetArea)) {
//						System.out.println(hail1+" and "+hail2+" intersect at "+pos1);
						result++;
					}
				}
			}
			return result;
		}
		public void tick(double factor) {
			ticks++;
			List<Hail> nextHails = new ArrayList<>();
			for (Hail hail:hails) {
				nextHails.add(hail.move(factor));
			}
			hails = nextHails;
		}
		List<Pos> closestPositions = new ArrayList<>();
		public List<Pos> findClosestPositions(Hail stoneHail) {
			List<Pos> result = new ArrayList<>(); 
			for (Hail hail:hails) {
				Pos closestPos = hail.closestPos(stoneHail);
				result.add(closestPos);
			}
			return result;
		}
		Pos stoneStartPos =  new Pos(30.42,52.92,39.41).multiply(FSIZE);                                    // id 77 tick 4
		Pos stoneEndPos =  new Pos(90.6654552739864,56.5242249593568,75.71798919844001).multiply(FSIZE);    // id 92 tick 101
		double bestMaxDist;
		Pos bestStartPos;
		Pos bestEndPos;
		public void fillClosestPositions() {
			Hail stoneThrow = new Hail(stoneStartPos, stoneEndPos.subtract(stoneStartPos).normalize());
			closestPositions = findClosestPositions(stoneThrow);
			bestMaxDist = calcMaxDist(stoneThrow);
			System.out.println("MAX DIST: "+bestMaxDist);
			double delta = bestMaxDist / 10;
			bestStartPos = stoneStartPos;
			bestEndPos = stoneEndPos;
			checkAlternative(-delta, 0, 0); 
			checkAlternative(delta, 0, 0); 
			checkAlternative(0, -delta, 0); 
			checkAlternative(0, delta, 0); 
			checkAlternative(0, 0, -delta); 
			checkAlternative(0, 0, delta);
			System.out.println("BEST MAX DIST "+bestMaxDist+"  "+bestStartPos+" -> "+bestEndPos);
			stoneStartPos = bestStartPos;
			stoneEndPos = bestEndPos;
		}
		private void checkAlternative(double dx, double dy, double dz) {
			checkAlternative(stoneStartPos.add(dx, dy, dz), stoneEndPos);
			checkAlternative(stoneStartPos, stoneEndPos.add(dx, dy, dz));
		}
		private void checkAlternative(Pos startPos, Pos endPos) {
			Hail alternativeStoneThrow = new Hail(startPos, endPos.subtract(startPos).normalize());
			double maxDist = calcMaxDist(alternativeStoneThrow);
			if (maxDist<bestMaxDist) {
				bestMaxDist = maxDist;
				bestStartPos = startPos;
				bestEndPos = endPos;
			}
		}
		private double calcMaxDist(Hail stoneThrow) {
			double result = 0;
			for (Pos pos:closestPositions) {
				double dist = stoneThrow.distance(pos);
				result = Math.max(result, dist);
			}
			return result;
		}
		
		public void show3D(String info) {
//			System.out.println("TICKS: "+ticks);
			List<DDDObject> blocks = new ArrayList<>();
			for (int i=0; i<closestPositions.size(); i++) {
				Pos pos = closestPositions.get(i);
				int col = 3;
				DDDObject block = new DDDObject("C"+i,
						pos.x, pos.y, pos.z, FSIZE/4, 0+col);
				blocks.add(block);
			}
			for (Hail hail:hails) {
				int col = 0;
				if (hail.id==ticks) {
					col=3;
				}
//				if (hail.id==92) {
//					col=1;
//					System.out.println("ID:92, TICK:"+ticks+": "+hail.pos+"  FSIZE*"+hail.pos.multiply(1.0/FSIZE));
//				}
				DDDObject block = new DDDObject("H"+hail.id,
						hail.pos.x, hail.pos.y, hail.pos.z, FSIZE, 0+col);
				blocks.add(block);
			}
			DDDLineObject line = new DDDLineObject("XAX", 0,0,0, 100*FSIZE,0,0, FSIZE/10, 31);
			blocks.add(line);
			line = new DDDLineObject("YAX", 0,0,0, 0,100*FSIZE,0, FSIZE/10, 32);
			blocks.add(line);
			line = new DDDLineObject("ZAX", 0,0,0, 0,0,100*FSIZE, FSIZE/10, 33);
			blocks.add(line);

//			Pos startLinePos = new Pos(30.42,52.92,39.41).multiply(FSIZE);                                    // id 77 tick 4
//			Pos endLinePos = new Pos(90.6654552739864,56.5242249593568,75.71798919844001).multiply(FSIZE);    // id 92 tick 101
			Pos startLinePos = stoneStartPos;
			Pos endLinePos = stoneEndPos;
			
			line = new DDDLineObject("STONE", startLinePos.x, startLinePos.y,startLinePos.z, endLinePos.x, endLinePos.y,endLinePos.z, FSIZE/5, 31);
			blocks.add(line);

			
			
			Pos testpos1 = new Pos(30.42*FSIZE, 52.92*FSIZE, 39.41*FSIZE);
//			System.out.println(testpos1+"  FSIZE*"+testpos1.multiply(1.0/FSIZE));
			DDDObject block = new DDDObject("TEST1", testpos1.x,testpos1.y,testpos1.z, FSIZE, 2);
			blocks.add(block);

			Pos testpos2 = new Pos(testpos1.x+60*FSIZE, testpos1.y, testpos1.z+40*FSIZE);
//			System.out.println(testpos2+"  FSIZE*"+testpos2.multiply(1.0/FSIZE));
			block = new DDDObject("TEST2", testpos2.x,testpos2.y,testpos2.z, FSIZE, 3);
			blocks.add(block);

//			Pos testpos3 = new Pos(-13*FSIZE, 55*FSIZE, 35*FSIZE);
//			System.out.println(testpos3+"  FSIZE*"+testpos3.multiply(1.0/FSIZE));
//			block = new DDDObject("TEST3", testpos3.x,testpos3.y,testpos3.z, FSIZE, 1);
//			blocks.add(block);

			if (output.scale == 1) {
				output.adjustScale(blocks);
			}
			output.addStep(info, blocks);
		}
		public Pos calcRockStartPosition() {
			Pos minStartPos = new Pos(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
			Pos maxStartPos = new Pos(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
			Hail rockThrow = new Hail(stoneStartPos, stoneEndPos.subtract(stoneStartPos).normalize());
			Pos result = null;
			Pos previousPos = null;
			double previousHitTick = 0;
			for (Hail hail:hails) {
				Pos closestPos = hail.closestPos(rockThrow);
				double hitTick = closestPos.subtract(hail.pos).magnitude()/hail.v.magnitude();
//				System.out.println("TICK: "+hitTick+" "+closestPos.multiply(1.0/FSIZE));
				if (previousPos == null) {
					previousPos = closestPos;
					previousHitTick = hitTick;
				}
				else {
					double deltaTick = hitTick-previousHitTick;
					Pos deltaPos = previousPos.subtract(closestPos);
					Pos startPos = closestPos.add(deltaPos.multiply(hitTick/deltaTick));
					System.out.println("ROCKSTART: SUM "+(long)(startPos.x+startPos.y+startPos.z)+" ("+(long)startPos.x+","+(long)startPos.y+","+(long)startPos.z+")");
					result = startPos;
					minStartPos = minStartPos.min(startPos);
					maxStartPos = maxStartPos.max(startPos);
				}
			}
			System.out.println("SEARCH: "+maxStartPos.subtract(minStartPos));
			return result;
		}
		
	}

	public static void mainPart1(String inputFile, long minTargetArea, long maxTargetArea) {
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
//		System.out.println(world);
		long cnt = world.countIntersectionsXY(minTargetArea, maxTargetArea);
		System.out.println("INTERSECTIONS X/Y: "+cnt);
	}

	
	public static void mainPart2(String inputFile) {
		output = new Y23GUIOutput3D18("Day 22 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
		world.show3D("init");
		world.fillClosestPositions();
		world.show3D("closest");
		while (world.bestMaxDist > 0.27) {
			for (int i=0; i<=10; i++) {
				world.fillClosestPositions();
			}
			world.show3D("closest");
		}
		
		for (int i=0; i<500; i++) {
			world.tick(FSIZE/500);
			world.show3D("tick");
		}
		
		Pos rockStart = world.calcRockStartPosition();
		System.out.println(rockStart);
		System.out.println("ROCKSTART: ("+(long)rockStart.x+","+(long)rockStart.y+","+(long)rockStart.z+")");
		System.out.println("SUM: "+(long)(rockStart.x+rockStart.y+rockStart.z));
		
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
//		testClosestPos();
//		if (true) return;
		URL url;
		System.out.println("--- PART I ---");
//		mainPart1("exercises/day24/Feri/input-example.txt", 7L, 27L);
//		mainPart1("exercises/day24/Feri/input.txt", 200000000000000L, 400000000000000L);               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
		
//		url = Y23Day24.class.getResource("/resources/input/aoc23day24/input-example.txt");
		url = Y23Day24.class.getResource("/resources/input/aoc23day24/input.txt");     // 3228177720733056 too high (1703619443970457,358917577518420,1165640699244178) 
		mainPart2(new File(url.toURI()).toString());

//		mainPart2("exercises/day24/Feri/input-example.txt");
//		mainPart2("exercises/day24/Feri/input.txt");
		System.out.println("---------------");    
	}


	private static void testClosestPos() {
		Hail rock = new Hail(new Pos(24, 13, 10), new Pos(-3, 1, 2));
//		Hail rock = new Hail(new Pos(26.56,19.4,8.08), new Pos(-3, 1, 2));
//		Hail rock = new Hail(new Pos(52.519999999999996,-3.7599999999999962,-8.799999999999995), new Pos(-3, 1, 2));
		Hail hail1 = new Hail(new Pos(19, 13, 30), new Pos(-2,  1, -2));
		Pos pos = rock.closestPos(hail1);
		System.out.println(pos);

		
		Hail hx = new Hail(new Pos(10,1,0.5), new Pos(0.5, 0, 0));
		Hail hy = new Hail(new Pos(1,20,1), new Pos(0, 0.5, 0));
		pos = hx.closestPos(hy);
		System.out.println(pos);

		Hail h1 = new Hail(new Pos(0,0,1), new Pos(1, 0, 0));
		Hail h2 = new Hail(new Pos(0,0,9), new Pos(0, 1, 0));
		pos = h1.closestPos(h2);
		System.out.println(pos);

	}
	
}
