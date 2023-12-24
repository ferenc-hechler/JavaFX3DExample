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
		public Pos add(int dx, int dy, int dz) {
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
		public Hail move() {
			return new Hail(id, pos.add(v), v);
		}

		public Hail move(double factor) {
			return new Hail(id, pos.add(v.multiply(factor)), v);
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
						System.out.println(hail1+" and "+hail2+" intersect at "+pos1);
						result++;
					}
				}
			}
			return result;
		}
		public Timeslot findTimeslot() {
			double minDeltaZ = Long.MAX_VALUE;
			double minTime = 0;
			double maxTime = Long.MAX_VALUE; 
			for (int i=0; i<hails.size(); i++) {
				Hail hail1 = hails.get(i);
				for (int j=i+1; j<hails.size(); j++) {
					Hail hail2 = hails.get(j);
					Pos pos1 = hail1.intersectXY(hail2);
					Pos pos2 = hail2.intersectXY(hail1);
					if ((pos1 != null) && (pos2 != null)) {
						double deltaZ = Math.abs(pos2.z-pos1.z);
						System.out.println("DELTAZ: "+deltaZ);
						minDeltaZ = Math.min(minDeltaZ, deltaZ);
						Pos vTarget = pos1.subtract(hail1.pos);
						double time = vTarget.magnitude()/hail1.v.magnitude(); 
						System.out.println("TIME: "+time+" at "+pos1);
						minTime = Math.min(minTime, time);
						maxTime = Math.max(maxTime, time);
					}
				}
			}
			System.out.println("MIN DELTAZ: "+minDeltaZ);
			return new Timeslot(minTime, maxTime);
		}
		public void tick(double factor) {
			ticks++;
			List<Hail> nextHails = new ArrayList<>();
			for (Hail hail:hails) {
				nextHails.add(hail.move(factor));
			}
			hails = nextHails;
		}
		public void show3D(String info) {
			System.out.println("TICKS: "+ticks);
			List<DDDObject> blocks = new ArrayList<>();
			for (Hail hail:hails) {
				int col = 0;
				if (hail.id==ticks) {
					col=3;
				}
				if (hail.id==92) {
					col=1;
					System.out.println("ID:92, TICK:"+ticks+": "+hail.pos+"  FSIZE*"+hail.pos.multiply(1.0/FSIZE));
				}
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

			Pos startLinePos = new Pos(30.42,52.92,39.41).multiply(FSIZE);                                    // id 77 tick 4
			Pos endLinePos = new Pos(90.6654552739864,56.5242249593568,75.71798919844001).multiply(FSIZE);    // id 92 tick 101
			
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
		
	}

	public static void mainPart1(String inputFile, long minTargetArea, long maxTargetArea) {
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
		System.out.println(world);
		long cnt = world.countIntersectionsXY(minTargetArea, maxTargetArea);
		System.out.println("INTERSECTIONS X/Y: "+cnt);
	}

	
	public static void mainPart2(String inputFile) {
		output = new Y23GUIOutput3D18("Day 22 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
		world.show3D("init");
		Timeslot timeslot = world.findTimeslot();
		System.out.println(timeslot);
		for (int i=0; i<500; i++) {
			world.tick(FSIZE/500);
			world.show3D("tick");
		}
	}


	public static void main(String[] args) throws FileNotFoundException, URISyntaxException {
		URL url;
		System.out.println("--- PART I ---");
//		mainPart1("exercises/day24/Feri/input-example.txt", 7L, 27L);
//		mainPart1("exercises/day24/Feri/input.txt", 200000000000000L, 400000000000000L);               
		System.out.println("---------------");                           
		System.out.println("--- PART II ---");
		
//		url = Y23Day24.class.getResource("/resources/input/aoc23day24/input-example.txt");
		url = Y23Day24.class.getResource("/resources/input/aoc23day24/input.txt");
		mainPart2(new File(url.toURI()).toString());

//		mainPart2("exercises/day24/Feri/input-example.txt");
//		mainPart2("exercises/day24/Feri/input.txt");
		System.out.println("---------------");    
	}
	
}
