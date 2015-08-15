package tools.structures;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Custom data structure for storing a list of coordinates.
 *
 * @author Peter Bugaj
 */
public class CoordinateList {

	/**
	 * List of coordinates stored for this list.
	 */
	private LinkedList<short[]> coordinates = new LinkedList<short[]>();
	
	/**
	 * Adds a coordinate to this list of coordinates.
	 */
	public void addCoord(short [] coordinate) {
		this.coordinates.push(coordinate);
	}

	/**
	 * Get the enumerator to the list of coordinates.
	 */
	public Iterator<short[]> getCoords() {
		return this.coordinates.iterator();
	}
	
	/**
	 * Get the number of coordinates.
	 */
	public int getSize() {
		return this.coordinates.size();
	}
}
