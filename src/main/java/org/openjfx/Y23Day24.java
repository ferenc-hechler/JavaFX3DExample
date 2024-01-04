package org.openjfx;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.MathContext;
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

	static MathContext mc  = new MathContext(20); 
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

	static boolean LT(BigDecimal bd1, BigDecimal bd2) {
		return bd1.compareTo(bd2)<0;
	}
	static boolean LE(BigDecimal bd1, BigDecimal bd2) {
		return bd1.compareTo(bd2)<=0;
	}
	static boolean GT(BigDecimal bd1, BigDecimal bd2) {
		return bd1.compareTo(bd2)>0;
	}
	static boolean GE(BigDecimal bd1, BigDecimal bd2) {
		return bd1.compareTo(bd2)>=0;
	}
	static BigDecimal MIN(BigDecimal bd1, BigDecimal bd2) {
		return bd1.min(bd2);
	}
	static BigDecimal MAX(BigDecimal bd1, BigDecimal bd2) {
		return bd1.max(bd2);
	}
	
	
	static record Pos(BigDecimal x, BigDecimal y, BigDecimal z) {
		public Pos(long x, long y, long z) {
			this(BigDecimal.valueOf(x), BigDecimal.valueOf(y), BigDecimal.valueOf(z));
		}
		public Pos(double x, double y, double z) {
			this(BigDecimal.valueOf(x), BigDecimal.valueOf(y), BigDecimal.valueOf(z));
		}
		@Override public String toString() { return "("+x+","+y+","+z+")"; }
		public Pos add(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
			return new Pos(x.add(dx), y.add(dy), z.add(dz));
		}
		public Pos add(Pos other) {
			return new Pos(x.add(other.x), y.add(other.y), z.add(other.z));
		}
		public Pos subtract(Pos other) {
			return new Pos(x.subtract(other.x), y.subtract(other.y), z.subtract(other.z));
		}
		public BigDecimal manhattenDist(Pos other) {
			return x.subtract(other.x).abs().add(y.subtract(other.y).abs().add(z.subtract(other.z).abs()));
		}
		public Pos multiply(BigDecimal k) {
			return new Pos(k.multiply(x), k.multiply(y), k.multiply(z));
		}
		public BigDecimal magnitude() {
			return x.multiply(x).add(y.multiply(y)).add(z.multiply(z)).sqrt(mc);  
		}
		public Pos normalize() {
			BigDecimal mag = magnitude();
			if (mag.equals(BigDecimal.ZERO)) {
				return this;
			}
			return multiply(BigDecimal.ONE.divide(mag, mc));  
		}
		// https://matheguru.com/lineare-algebra/kreuzprodukt-vektorprodukt.html?utm_content=cmp-true
		public Pos cross(Pos other) {
			return new Pos(y.multiply(other.z).subtract(z.multiply(other.y)), z.multiply(other.x).subtract(x.multiply(other.z)), x.multiply(other.y).subtract(y.multiply(other.x)));
		}
		public BigDecimal dot(Pos other) {
			return x.multiply(other.x).add(y.multiply(other.y)).add(z.multiply(other.z));
		}
		public Pos min(Pos pos) {
			if (LE(x, pos.x) && LE(y,pos.y) && LE(z,pos.z)) {
				return this;
			}
			return new Pos(MIN(x, pos.x), MIN(y, pos.y), MIN(z, pos.z));
		}
		public Pos max(Pos pos) {
			if (GE(x,pos.x) && GE(y,pos.y) && GE(z,pos.z)) {
				return this;
			}
			return new Pos(MAX(x, pos.x), MAX(y, pos.y), MAX(z, pos.z));
		}
		public Pos divide(BigDecimal deltaTick) {
			return multiply(BigDecimal.ONE.divide(deltaTick, mc));
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
			Pos vNorm = new Pos(v.x, v.y, BigDecimal.ZERO);
			BigDecimal vFactor = vNorm.magnitude();
			vNorm = vNorm.normalize();
			Pos vOtherNorm = new Pos(other.v.x, other.v.y, BigDecimal.ZERO).normalize();
			
			BigDecimal PQx = other.pos.x.subtract(pos.x);
			BigDecimal PQy = other.pos.y.subtract(pos.y);
			BigDecimal rx = vNorm.x;
			BigDecimal ry = vNorm.y;
			BigDecimal rxt = ry.negate();
			BigDecimal ryt = rx;
			BigDecimal qx = PQx.multiply(rx).add(PQy.multiply(ry));
			BigDecimal qy = PQx.multiply(rxt).add(PQy.multiply(ryt));
			BigDecimal s0 = vOtherNorm.x;
			BigDecimal s1 = vOtherNorm.y;
			BigDecimal sx = s0.multiply(rx).add(s1.multiply(ry));
			BigDecimal sy = s0.multiply(rxt).add(s1.multiply(ryt));
			if (sy.equals(BigDecimal.ZERO)) {
				// lines are parallel
				return null;
			}
			BigDecimal a = qx.subtract(qy.multiply(sx.divide(sy, mc)));
			if (LT(a,BigDecimal.ZERO)) {
				return null;
			}
			BigDecimal resultX = pos.x.add(a.multiply(v.x.divide(vFactor, mc)));
			BigDecimal resultY = pos.y.add(a.multiply(v.y.divide(vFactor, mc)));
			BigDecimal resultZ = pos.z.add(a.multiply(v.z.divide(vFactor, mc)));
			return new Pos(resultX, resultY, resultZ);
		}
		public Pos intersectXYZ(Y23Day24.Hail other) {
			// TODO: add delta-z check, switch x/z or y/z if x/y is parallel 
			Pos vNorm = new Pos(v.x, v.y, BigDecimal.ZERO);
			BigDecimal vFactor = vNorm.magnitude();
			vNorm = vNorm.normalize();
			Pos vOtherNorm = new Pos(other.v.x, other.v.y, BigDecimal.ZERO).normalize();
			
			BigDecimal PQx = other.pos.x.subtract(pos.x);
			BigDecimal PQy = other.pos.y.subtract(pos.y);
			BigDecimal rx = vNorm.x;
			BigDecimal ry = vNorm.y;
			BigDecimal rxt = ry.negate();
			BigDecimal ryt = rx;
			BigDecimal qx = PQx.multiply(rx).add(PQy.multiply(ry));
			BigDecimal qy = PQx.multiply(rxt).add(PQy.multiply(ryt));
			BigDecimal s0 = vOtherNorm.x;
			BigDecimal s1 = vOtherNorm.y;
			BigDecimal sx = s0.multiply(rx).add(s1.multiply(ry));
			BigDecimal sy = s0.multiply(rxt).add(s1.multiply(ryt));
			if (sy.equals(BigDecimal.ZERO)) {
				// lines are parallel
				return null;
			}
			BigDecimal a = qx.subtract(qy.multiply(sx.divide(sy, mc)));
			BigDecimal resultX = pos.x.add(a.multiply(v.x.divide(vFactor, mc)));
			BigDecimal resultY = pos.y.add(a.multiply(v.y.divide(vFactor, mc)));
			BigDecimal resultZ = pos.z.add(a.multiply(v.z.divide(vFactor, mc)));
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
			BigDecimal dist = n.dot(other.pos.subtract(pos));
			Pos movedOtherPos = other.pos.add(n.multiply(dist.negate()));
			Hail movedOther = new Hail(movedOtherPos, other.v); 
			Pos result = movedOther.intersectXYZ(this);
			return result;
		}
		
		public Hail move() {
			return new Hail(id, pos.add(v), v);
		}

		public Hail move(BigDecimal factor) {
			return new Hail(id, pos.add(v.multiply(factor)), v);
		}
		// https://www.mathematik-oberstufe.de/vektoren/a/abstand-punkt-gerade-formel.html
		public BigDecimal distance(Pos otherPos) {
			return otherPos.subtract(pos).cross(v).magnitude().divide(v.magnitude(), mc);
		}

	}
	
	static long FSIZE = 5000000000000L;

	static record Timeslot(BigDecimal from, BigDecimal to) {}
	
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
		public long countIntersectionsXY(BigDecimal minTargetArea, BigDecimal maxTargetArea) {
			long result = 0; 
			for (int i=0; i<hails.size(); i++) {
				Hail hail1 = hails.get(i);
				for (int j=i+1; j<hails.size(); j++) {
					Hail hail2 = hails.get(j);
					Pos pos1 = hail1.intersectXY(hail2);
					Pos pos2 = hail2.intersectXY(hail1);
					if ((pos1 != null) && (pos2 != null)
							&& GE(pos1.x,minTargetArea) && LE(pos1.x,maxTargetArea) 
							&& GE(pos1.y,minTargetArea) && LE(pos1.y,maxTargetArea)) {
//						System.out.println(hail1+" and "+hail2+" intersect at "+pos1);
						result++;
					}
				}
			}
			return result;
		}
		public void tick(BigDecimal factor) {
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
//		Pos stoneStartPos =  new Pos(30.42,52.92,39.41).multiply(BigDecimal.valueOf(FSIZE));                                    // id 77 tick 4
//		Pos stoneEndPos =  new Pos(90.6654552739864,56.5242249593568,75.71798919844001).multiply(BigDecimal.valueOf(FSIZE));    // id 92 tick 101
		
//		Pos stoneStartPos =  new Pos(152110428649347L,265036458756957L,192241731033158L);
//		Pos stoneEndPos = new Pos(452582497259420L,283217889660114L,380754461976420L);

		Pos stoneStartPos =  new Pos(152110428649347L,265036458756956L,192241731033157L);   // 0.0000002
		Pos stoneEndPos = new Pos(452582497259420L,283217889660114L,380754461976419L);

//		Pos stoneStartPos =  new Pos(1703619443970450L,358917577518425L,1165640699244168L);
//		Pos stoneEndPos = new Pos(452582497259420L,283217889660114L,380754461976419L);
		
		BigDecimal bestMaxDist;
		Pos bestStartPos;
		Pos bestEndPos;
		public void fillClosestPositions() {
			Hail stoneThrow = new Hail(stoneStartPos, stoneEndPos.subtract(stoneStartPos).normalize());
			closestPositions = findClosestPositions(stoneThrow);
			bestMaxDist = calcMaxDist(stoneThrow);
			System.out.println("MAX DIST: "+bestMaxDist);
			BigDecimal delta = bestMaxDist.multiply(BigDecimal.valueOf(0.3));
			bestStartPos = stoneStartPos;
			bestEndPos = stoneEndPos;
			checkAlternative(delta.negate(), BigDecimal.ZERO, BigDecimal.ZERO); 
			checkAlternative(delta, BigDecimal.ZERO, BigDecimal.ZERO); 
			checkAlternative(BigDecimal.ZERO, delta.negate(), BigDecimal.ZERO); 
			checkAlternative(BigDecimal.ZERO, delta, BigDecimal.ZERO); 
			checkAlternative(BigDecimal.ZERO, BigDecimal.ZERO, delta.negate()); 
			checkAlternative(BigDecimal.ZERO, BigDecimal.ZERO, delta);
			System.out.println("BEST MAX DIST "+bestMaxDist+"  "+bestStartPos+" -> "+bestEndPos);
			stoneStartPos = bestStartPos;
			stoneEndPos = bestEndPos;
		}
		private void checkAlternative(BigDecimal dx, BigDecimal dy, BigDecimal dz) {
			checkAlternative(stoneStartPos.add(dx, dy, dz), stoneEndPos);
			checkAlternative(stoneStartPos, stoneEndPos.add(dx, dy, dz));
		}
		private void checkAlternative(Pos startPos, Pos endPos) {
			Hail alternativeStoneThrow = new Hail(startPos, endPos.subtract(startPos).normalize());
			BigDecimal maxDist = calcMaxDist(alternativeStoneThrow);
			if (LT(maxDist,bestMaxDist)) {
				bestMaxDist = maxDist;
				bestStartPos = startPos;
				bestEndPos = endPos;
			}
		}
		private BigDecimal calcMaxDist(Hail stoneThrow) {
			BigDecimal result = BigDecimal.ZERO;
			for (Pos pos:closestPositions) {
				BigDecimal dist = stoneThrow.distance(pos);
				result = MAX(result, dist);
			}
			return result;
		}
		
		public void show3D(String info) {
//			System.out.println("TICKS: "+ticks);
			List<DDDObject> blocks = new ArrayList<>();
			Hail rock = hails.get(0);
			double baseSize = FSIZE/2;
			for (int i=0; i<closestPositions.size(); i++) {
				Pos pos = closestPositions.get(i);
				if (LE(pos.x,rock.pos.x)) {
					int col = 3;
					DDDObject block = new DDDObject("C"+i,
							pos.x.doubleValue(), pos.y.doubleValue(), pos.z.doubleValue(), baseSize/2, 0+col);
					blocks.add(block);
				}
			}
			for (Hail hail:hails) {
				int col = 0;
				double hSize = baseSize;
//				if (hail.id==ticks) {
//					col=3;
//				}
				if (hail.id==1) {
					col=1;
					hSize = hSize * 1.5;
//					System.out.println("ID:1, TICK:"+ticks+": "+hail.pos+"  FSIZE*"+hail.pos.multiply(BigDecimal.valueOf(1.0/FSIZE)));
				}
				DDDObject block = new DDDObject("H"+hail.id,
						hail.pos.x.doubleValue(), hail.pos.y.doubleValue(), hail.pos.z.doubleValue(), hSize, 0+col);
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
			
//			line = new DDDLineObject("STONE", startLinePos.x.doubleValue(), startLinePos.y.doubleValue(),startLinePos.z.doubleValue(), endLinePos.x.doubleValue(), endLinePos.y.doubleValue(),endLinePos.z.doubleValue(), FSIZE/5, 31);
//			blocks.add(line);

			
			
//			Pos testpos1 = new Pos(30.42*FSIZE, 52.92*FSIZE, 39.41*FSIZE);
////			System.out.println(testpos1+"  FSIZE*"+testpos1.multiply(1.0/FSIZE));
//			DDDObject block = new DDDObject("TEST1", testpos1.x.doubleValue(),testpos1.y.doubleValue(),testpos1.z.doubleValue(), FSIZE, 2);
//			blocks.add(block);
//
//			Pos testpos2 = new Pos(testpos1.x.doubleValue()+60*FSIZE, testpos1.y.doubleValue(), testpos1.z.doubleValue()+40*FSIZE);
////			System.out.println(testpos2+"  FSIZE*"+testpos2.multiply(1.0/FSIZE));
//			block = new DDDObject("TEST2", testpos2.x.doubleValue(),testpos2.y.doubleValue(),testpos2.z.doubleValue(), FSIZE, 3);
//			blocks.add(block);

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
			BigDecimal previousHitTick = BigDecimal.ZERO;
			for (Hail hail:hails) {
				Pos closestPos = hail.closestPos(rockThrow);
				BigDecimal hitTick = closestPos.subtract(hail.pos).magnitude().divide(hail.v.magnitude(), mc);
//				System.out.println("TICK: "+hitTick+" "+closestPos.multiply(1.0/FSIZE));
				if (previousPos == null) {
					previousPos = closestPos;
					previousHitTick = hitTick;
				}
				else {
					BigDecimal deltaTick = hitTick.subtract(previousHitTick);
					Pos deltaPos = previousPos.subtract(closestPos);
					Pos startPos = closestPos.add(deltaPos.multiply(hitTick.divide(deltaTick, mc)));
					System.out.println("ROCKSTART: SUM "+startPos.x.add(startPos.y).add(startPos.z)+" ("+startPos.x+","+startPos.y+","+startPos.z+")");
					System.out.println("ROCKSTART: ("+startPos.x+","+startPos.y+","+startPos.z+")");
					result = startPos;
					Pos rockV = closestPos.subtract(startPos).divide(hitTick);
					System.out.println("ROCKV: ("+rockV.x+","+rockV.y+","+rockV.z+")");
					minStartPos = minStartPos.min(startPos);
					maxStartPos = maxStartPos.max(startPos);
				}
			}
			System.out.println("SEARCH: "+maxStartPos.subtract(minStartPos));
			return result;
		}
		public void testSolution() {
			Pos oldRockStartPos = new Pos(1703619443970450L,358917577518425L,1165640699244168L);
			Pos rockV = new Pos(-314L,-19L,-197L);
//			Pos rockV = new Pos(314L,19L,197L);
			
			Pos rockStartPos = oldRockStartPos;
			rockStartPos = rockStartPos.add(rockV.multiply(BigDecimal.valueOf(4780877094720L+219122905280L)));
//			rockStartPos = rockStartPos.add(rockV.multiply(BigDecimal.valueOf(219122905280L-4780877094720L)));
			System.err.println("ROCKSTART: "+rockStartPos);
			System.err.println("SUM: "+rockStartPos.x.add(rockStartPos.y).add(rockStartPos.z));
			
			
			Hail rock = new Hail(rockStartPos, rockV);
			System.out.println(rock);
			for (Hail hail:hails) {
				Pos pos = hail.closestPos(rock);
				BigDecimal distR = pos.subtract(rockStartPos).magnitude();
				BigDecimal vDistR = distR.divide(rockV.magnitude(), mc);
				
				BigDecimal distH = pos.subtract(hail.pos).magnitude();
				BigDecimal vDistH = distH.divide(hail.v.magnitude(), mc);
				System.out.println("ID: "+hail.id+" vDISTR: "+vDistR+" vDISTH: "+vDistH);
				
			}
		}
		
	}

	public static void mainPart1(String inputFile, long minTargetArea, long maxTargetArea) {
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
//		System.out.println(world);
		long cnt = world.countIntersectionsXY(BigDecimal.valueOf(minTargetArea), BigDecimal.valueOf(maxTargetArea));
		System.out.println("INTERSECTIONS X/Y: "+cnt);
	}

	
	public static void mainPart2(String inputFile) {
		output = new Y23GUIOutput3D18("Day 22 Part I", true);
		World world = new World();
		for (InputData data:new InputProcessor(inputFile)) {
//			System.out.println(data);
			world.addHail(new Hail(data.pos, data.v));
		}
//		world.testSolution();
//		if (true)
//			return;
		world.show3D("init");
		world.fillClosestPositions();
		world.show3D("closest");
//		while (GT(world.bestMaxDist, BigDecimal.valueOf(0.0000002))) {    // 0.0000002
//			for (int i=0; i<=10; i++) {
//				world.fillClosestPositions();
//			}
//			world.show3D("closest");
//		}
		
		for (int i=0; i<1500; i++) {
			world.tick(BigDecimal.valueOf(FSIZE/5000));
			world.show3D("tick");
		}
		
		Pos rockStart = world.calcRockStartPosition();
		System.out.println(rockStart);
		System.out.println("ROCKSTART: ("+rockStart.x+","+rockStart.y+","+rockStart.z+")");
		System.out.println("SUM: "+rockStart.x.add(rockStart.y).add(rockStart.z));
		
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
		                                                                               // 3228177720733043 too high (1703619443970449.99999680434718937260951063437086419125581909068979953387125301512956023021018923762220937120831222001145825264749278205981354459918939118910774817195982453891910189568798923480107826360320659389228527078608857387263885586319391637276270828050612426453117566061555934374859593802045534494853751327202182707390239319631395528963900165964095448863641915087248581025000000000000000000000000000000000000000000000,358917577518425.000000259590180263264813519319292272470946093654708759682418541340017648617573381010472697017474040387465695988683843739976067826757847723095022318636191642670197875837938056749293134201340186269170178010979164624955447838101833271183218666934983841213416313932611103286708028114497745153865539788459286023393891047316430318820903708911197729142397015197875629081680000000000000000000000000000000000000000000,1165640699244168.00000199139193840853967713641460351827246762531060786669569340097231738056923567880040425380693889737323715832440527731799667971264828112747446816848753219791917446628274298135460351105568803474375237846581196588575717686039514361690448486600296753855171055160694958665320092269146676646961872016400409915894112870972691999994145894222396581152059369052143207438040000000000000000000000000000000000000000000)
																					   // 3228177720732513 too high
		/*
		 * 3228177720733043
		 * ROCKSTART: (1703619443970450,358917577518425,1165640699244168)
		 * ROCKV:     (-314,-19,-197)
		 */
		
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
