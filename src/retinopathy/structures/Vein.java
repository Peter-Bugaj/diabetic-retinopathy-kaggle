package retinopathy.structures;

import java.util.Vector;

/**
 * Data structure for defining patches within an image.
 *
 * @author Peter Bugaj
 */
public class Vein {

	/**
	 * ID generator for every new vein created.
	 */
	private static int id_incrementor = 1;
	
	/**
	 * The ID of this vein
	 */
	private int ID = 1;
	
	/**
	 * A mark for this vein.
	 */
	private int MARK = 0;
	
	/**
	 * The width intensity of this vein, as
	 * computed during vein skeletonization.
	 */
	private float width_intensity = 0;
	
	/**
	 * Strength of the vein.
	 */
	private int vein_strength;
	
	/**
	 * Starting point of the vein.
	 */
	private short [] pointA;
	
	/**
	 * Ending point of the vein.
	 */
	private short [] pointB;
	
	/**
	 * Number of pixels making up this vein.
	 */
	private int size;
	
	/**
	 * Array for storig the points making up the vein.
	 */
	private Vector<short[]> vein_points = null;
	
	/**
	 * The veins connecting to this vein.
	 */
	private VeinFork connectionsA = null;

	/**
	 * The veins connecting to this vein.
	 */
	private VeinFork connectionsB = null;

	/**
	 * Create a new instance of the Vein class.
	 */
	public Vein() {
		id_incrementor++;
		this.ID = id_incrementor;
	}
	
	/**
	 * Create a new instance of the Vein class.
	 */
	public Vein(int vein_strength, short [] pointA, short [] pointB) {
		this.vein_strength = vein_strength;
		
		this.pointA = pointA;
		this.pointB = pointB;
		
		id_incrementor++;
		this.ID = id_incrementor;
	}

	/**
	 * Get or set the vein intensity.
	 */
	public float getIntensity() {
		return this.width_intensity;
	}
	
	/**
	 * Get or set the vein intensity.
	 */
	public void appendIntensity(float width_intensity) {
		this.width_intensity += width_intensity;
	}

	/**
	 * Return the ID of this vein.
	 */
	public int getId() {
		return this.ID;
	}
	
	/**
	 * Return the mark of this vein.
	 */
	public int getMark() {
		return this.MARK;
	}
	
	/**
	 * Set the mark of this vein.
	 */
	public void setMark(int mark) {
		this.MARK = mark;
	}
	
	/**
	 * Set or get the size of the vein.
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * Set or get the size of the vein.
	 */
	public void setSize(int size) {
		this.size = size;
	}
	
	/**
	 * Get or set the vein strength.
	 */
	public int getVeinStrength() {
		return this.vein_strength;
	}
	
	/**
	 * Get or set the vein strength.
	 */
	public void setVeinStrength(int vein_strength) {
		this.vein_strength = vein_strength;
	}

	/**
	 * Get or set the starting point.
	 */
	public short[] getPointA() {
		return this.pointA;
	}
	
	/**
	 * Get or set the starting point.
	 */
	public void setPointA(short[] pointA) {
		this.pointA = pointA;
	}

	/**
	 * Get or set the ending point.
	 */
	public short[] getPointB() {
		return this.pointB;
	}
	
	/**
	 * Get or set the ending point.
	 */
	public void setPointB(short[] pointB) {
		this.pointB = pointB;
	}
	
	/**
	 * Get or set the vein points.
	 */
	public void addPoint(short [] point) {
		if(this.vein_points == null) {
			this.vein_points = new Vector<short[]>();
		}
		
		this.vein_points.add(point);
	}
	
	/**
	 * Get or set the vein points.
	 */
	public Vector<short[]> getPoints() {
		if(this.vein_points == null) {
			this.vein_points = new Vector<short[]>();
		}
		
		return this.vein_points;
	}
	
	/**
	 * Add or get the vein connections.
	 */
	public void setConnectionA(VeinFork vein_fork) {
		this.connectionsA = vein_fork;
	}
	
	/**
	 * Add or get the vein connections.
	 */
	public VeinFork getConnectionA() {
		return this.connectionsA;
	}	
	
	/**
	 * Add or get the vein connections.
	 */
	public void setConnectionB(VeinFork vein_fork) {
		this.connectionsB = vein_fork;
	}
	
	/**
	 * Add or get the vein connections.
	 */
	public VeinFork getConnectionB() {
		return this.connectionsB;
	}
	
	/**
	 * Remove the connections.
	 */
	public void removeConnectionB() {
		this.connectionsB = null;
	}
	
	/**
	 * Remove the connections.
	 */
	public void removeConnectionA() {
		this.connectionsA = null;
	}

	/**
	 * Append an existing vein to this vein.
	 */
	public void appendVein(VeinFork toDelete, Vein vein) {
		if(this.connectionsA.ID == this.connectionsB.ID) {
			int[]u=new int[0];u[8989]=0;
		}
		if(vein.getConnectionA().ID == vein.getConnectionB().ID) {
			int[]u=new int[0];u[8989]=0;
		}

		
		if(this.connectionsA.ID == toDelete.ID) {

			if(vein.getConnectionA().ID != toDelete.ID) {
				this.connectionsA = vein.getConnectionA();
				this.pointA = vein.pointA;
			} else if(vein.getConnectionB().ID != toDelete.ID) {
				this.connectionsA = vein.getConnectionB();
				this.pointA = vein.pointB;
			} else {
				int[]u=new int[0];u[8989]=0;
			}
	
		} else if(this.connectionsB.ID == toDelete.ID) {

			if(vein.getConnectionA().ID != toDelete.ID) {
				this.connectionsB = vein.getConnectionA();
				this.pointB = vein.pointA;
			} else if(vein.getConnectionB().ID != toDelete.ID) {
				this.connectionsB = vein.getConnectionB();
				this.pointB = vein.pointB;
			} else {
				int[]u=new int[0];u[8989]=0;
			}

		} else {
			int[]u=new int[0];u[8989]=0;
		}
		
		if(this.connectionsA.ID == this.connectionsB.ID) {
			int[]u=new int[0];u[8989]=0;
		}
		
		this.vein_points.addAll(vein.getPoints());
		this.setSize(this.getSize() + vein.getPoints().size() - 1);
	}
	
	/**
	 * Duplicate the info from the passed in vein into this vein.
	 */
	public void duplicate(Vein vein) {
		this.connectionsA = vein.connectionsA;
		this.connectionsB = vein.connectionsB;
		
		this.pointA = vein.pointA;
		this.pointB = vein.pointB;
		
		this.ID = vein.ID;
		this.MARK = vein.MARK;
		this.width_intensity = vein.width_intensity;
		this.vein_strength = vein.vein_strength;
		
		this.size = vein.size;
		this.vein_points = vein.vein_points;
	}
}
