package retinopathy.structures;

/**
 * Data structure for defining a vein fork within an image of an eye.
 *
 * @author Peter Bugaj
 */
public class VeinFork {
	
	/**
	 * ID generator for every new patch created.
	 */
	private static int id_incrementor = 1;
	
	/**
	 * Auto marker incrementor.
	 */
	private static int marker_incrementor = 99;
	
	/**
	 * The ID of this vein fork.
	 */
	public int ID = 1;
	
	/**
	 * Indication of whether or not this fork is marked.
	 */
	private boolean MARKED = false;
	
	/**
	 * A mark for this vein fork.
	 */
	private int MARK = 0;

	/**
	 * The veins connected to this vein fork.
	 */
	private Vein[] veins = null;

	/**
	 * The x coordinate of the vein fork.
	 */
	private short coordx = -1;

	/**
	 * The y coordinate of the vein fork.
	 */
	private short coordy = -1;
	
	/**
	 * The index the last vein was stored at
	 * in the array for this vein fork.
	 */
	private byte store_index;
	
	/**
	 * Create a new instance o the vein fork class.
	 * 
	 * @param x
	 * The x coordinate of the vein fork.
	 * @param y
	 * The y coordinate of the vein fork.
	 * @param size
	 * The size of the fork in pixels.
	 */
	public VeinFork(short x, short y, byte size) {
		this.coordx = x;
		this.coordy = y;
		
		this.veins = new Vein[size];
		store_index = 0;
		
		id_incrementor++;
		this.ID = id_incrementor;
	}
	
	/**
	 * Add a new vein that is connected to the vein fork.
	 * 
	 * @param vein
	 * The new vein to add.
	 */
	public void addVein(Vein vein) {
		this.veins[store_index++] = vein;
	}
	
	/**
	 * Get the veins connected to this vein fork.
	 */
	public Vein[] getVeins() {
		return this.veins;
	}
	
	/**
	 * Optimize the memory usage for storing veins for this fork.
	 */
	public void optimizeConnections() {
		Vein [] new_veins = new Vein[store_index];
		for(int i = 0; i < new_veins.length; i++) {
			new_veins[i] = this.veins[i];
		}
		
		this.veins = new_veins;
	}
	
	/**
	 * Remov a vein from the vein fork.
	 * 
	 * @param vein
	 * The vein to remove.
	 */
	public void removeVein(Vein vein) {
		Vein [] new_veins = new Vein[this.veins.length - 1];
		int counter = 0;

		for(int i = 0; i < this.veins.length; i++) {
			if(this.veins[i].getId() != vein.getId()) {
				new_veins[counter++] = this.veins[i];
			}
		}

		this.veins = new_veins;
	}

	/**
	 * Get the indication of whether or not this vein is marked.
	 */
	public boolean isMarked() {
		return this.MARKED;
	}
	
	/**
	 * Indicate that this vein is marked.
	 */
	public void mark() {
		this.MARKED = true;
	}
	
	/**
	 * Get the mark for the fork.
	 */
	public int getMark() {
		return this.MARK;
	}

	/**
	 * Set the mark for the fork.
	 */
	public void setMark(int mark) {
		this.MARK = mark;
	}

	/**
	 * Get the x coordinate of the fork.
	 */
	public short getCoordx() {
		return this.coordx;
	}

	/**
	 * Get the y coordinate of the fork.
	 */
	public short getCoordy() {
		return this.coordy;
	}
	
	/**
	 * Get the coordinate of the fork.
	 */
	public short []getCoord() {
		return new short[]{this.coordx, this.coordy};
	}
	
	/**
	 * Get next incremented marker value.
	 */
	public static int getNextMarkerValue() {
		return marker_incrementor++;
	}
}
