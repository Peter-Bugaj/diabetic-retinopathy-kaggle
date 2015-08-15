package trainer;

/**
 * Data structure for storing a feature vector and
 * its associated classification.
 *
 * @author Peter Bugaj
 */
public class FeatureValuePair {

	/**
	 * The classification value assigned to this feature.
	 */
	private int featureValue;
	
	/**
	 * The individual values of the feature vector.
	 */
	private double[] feature;
	
	/**
	 * The name of the file from which this feature was read from.
	 */
	private String fileName;
	
	/**
	 * Create a new instance of the FeatureValuePair class.
	 * 
	 * @param fileName
	 * The file name containing the feature data.
	 * @param featureValue
	 * The classification value of the feature.
	 * @param feature
	 * The feature vector.
	 */
	public FeatureValuePair(String fileName, int featureValue, double[] feature) {
		this.featureValue = featureValue;
		this.feature = feature;
		this.fileName = fileName;
	}
	
	/**
	 * Get the file name.
	 */
	public String getFileName() {
		return this.fileName;
	}
	
	/**
	 * Get the feature value.
	 */
	public int getValue() {
		return this.featureValue;
	}
	
	/**
	 * Get the feature data.
	 */
	public double[] getFeatureData() {
		return this.feature;
	}
}
