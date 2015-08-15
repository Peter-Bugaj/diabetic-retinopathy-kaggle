package retinopathy.structures;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import tools.math.VectorTools;

/**
 * Data structure for defining patches within an image.
 *
 * @author Peter Bugaj
 */
public class Patch {

	/**
	 * ID generator for every new patch created.
	 */
	private static int id_gen = 1;

	/**
	 * The ID of the current patch.
	 */
	private int id;
	
	/**
	 * The area of the patch.
	 */
	private float pixelCount;
	
	/**
	 * The stacked area of the patch.
	 */
	private float stackPixelCount;
	
	/**
	 * Sum of the pixel levels used for calculatig the average light intensity.
	 */
	private long levelSum;
	
	/**
	 * Pixel sum x for caculating centroid of patch.
	 */
	private long sumX;
	
	/**
	 * Pixel sum y for caculating centroid of patch.
	 */
	private long sumY;	
	
	/**
	 * The curvature value of the boundary of this patch.
	 */
	private float curvature;
	
	/**
	 * The boundary of the patch.
	 */
	private LinkedList<short[]> boundary = new LinkedList<short[]>();

	/**
	 * The level this patch is at is at.
	 */
	private int level = -1;

	/**
	 * Whether or not this patch has been analyzed by an algorithm.
	 */
	private boolean isAnalyzed = false;
	
	/**
	 * The parent of this patch
	 */
	private Patch parent;

	
	/**
	 * Create a new instance of the Patch class.
	 */
	public Patch() {
		this.id = id_gen++;
	}

	/**
	 * Set the level for this patch.
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	
	/**
	 * Get the level for this patch.
	 */
	public int getLevel() {
		return this.level;
	}
	
	/**
	 * Get the analysis status of the patch.
	 */
	public boolean getAnalysisStatus() {
		return this.isAnalyzed;
	}
	
	/**
	 * Return the curvature of this patch.
	 */
	public float getCurvature() {
		return this.curvature;
	}
	
	/**
	 * Set the curvature of this patch.
	 */
	public void setCurvature(float curv) {
		this.curvature = curv;
	}
	
	/**
	 * Return the ID of this patch.
	 */
	public int getId() {
		return this.id;
	}
	
	/**
	 * Set the parent patch.
	 */
	public void setParentPatch(Patch parent) {
		this.parent = parent;
	}
	
	/**
	 * Get the parent patch.
	 */
	public Patch getParentPatch() {
		return this.parent;
	}
	
	/**
	 * Get the boundary of the patch.
	 */
	public LinkedList<short[]> getBoundary() {
		return this.boundary;
	}
	
	/**
	 * Get an estimated curvature of the boundary.
	 */
	public static float getCurvature_T5(
			LinkedList<short[]> boundary,
			long sum_x, long sum_y, float pixel_count) {

		if (pixel_count <= 8) return 0;
		
		// Store the centroid.
		float cent_x = sum_x/(pixel_count + 0f);
		float cent_y = sum_y/(pixel_count + 0f);
		float [] cent = new float[]{cent_x, cent_y};
		
		// Get the diffs
		float [] diffs = new float[boundary.size()];
		Iterator<short[]> iter = boundary.iterator();
		int count = 0;
		while(iter.hasNext()) {
			short[]next = iter.next();
			
			diffs[count] = VectorTools.distance(cent, new float[]{next[0], next[1]});
			count++;
		}
		
		
		Arrays.sort(diffs);
		int sample_size = Math.max(1, diffs.length / 8);
		float min_sum = 0;
		float max_sum = 0;

		for(int i = 0; i < sample_size && i < diffs.length; i++) {
			min_sum += diffs[i];
		}
		min_sum /= sample_size;

		for(int i = diffs.length - 1; i >=  diffs.length - sample_size && i >= 0; i--) {
			max_sum += diffs[i];
		}
		max_sum /= sample_size;
		
		return min_sum / max_sum;
	}
	
	/**
	 * Return the area of the patch.
	 */
	public float getArea() {
		return this.pixelCount;
	}
	
	/**
	 * Return the stack area of the patch.
	 */
	public float getStackArea() {
		return this.stackPixelCount;
	}
	
	/**
	 * Set the area of the patch.
	 */
	public void setArea(float pixelCount) {
		this.pixelCount = pixelCount;
	}
	
	/**
	 * Increment the area of the patch.
	 */
	public void incrementArea(float pixelCount) {
		this.pixelCount += pixelCount;
	}
	
	/**
	 * Increment the stack area of the patch.
	 */
	public void incrementStackArea(float pixelCount) {
		this.stackPixelCount += pixelCount;
	}
	
	/**
	 * Get the level sum
	 */
	public long getLevelSum() {
		return this.levelSum;
	}

	/**
	 * Increment the level sum
	 */
	public void incrementLevelSum(long val) {
		this.levelSum += val;
	}
	
	/**
	 * Get the sum of x coordinates.
	 */
	public long getSumX() {
		return this.sumX;
	}
	
	/**
	 * Get the sum of y coordinates.
	 */
	public long getSumY() {
		return this.sumY;
	}
	
	/**
	 * Increment the sum of x coordinates.
	 */
	public void incrementSumX(long val) {
		this.sumX += val;
	}
	
	/**
	 * Increment the sum of y coordinates.
	 */
	public void incrementSumY(long val) {
		this.sumY += val;
	}
	
	
	/**
	 * Return the centroid of this patch.
	 */
	public float[] getCentroid() {
		return new float[]{
				this.sumX/(this.pixelCount + 0f),
				this.sumY/(this.pixelCount + 0f)
				};
	}
	
	/**
	 * Return the average light intensity for this patch.
	 */
	public float getAverageIntensity() {
		return this.levelSum/(this.pixelCount + 0f);
	}
}
