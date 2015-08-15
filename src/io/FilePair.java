package io;

/**
 * Data structure for storing image file names of the eye as pairs,
 * for left and right eyes of one person.
 * 
 * @author Peter Bugaj
 */
public class FilePair {

	/**
	 * The file name of the image for the left eye.
	 */
	private String leftImage = null;
	
	/**
	 * The file name of the image for the right eye.
	 */
	private String rightImage = null;
	
	/**
	 * The rating for the image of the left eye.
	 */
	private String leftRating = null;
	
	/**
	 * The rating for the image of the right eye.
	 */
	private String rightRating = null;	
	
	/**
	 * Create a new instance of the FilePair class.
	 * 
	 * @param leftImage
	 * The left image.
	 * @param rightImage
	 * The right image.
	 * @param leftRating
	 * The left rating.
	 * @param rightRating
	 * The right rating.
	 */
	public FilePair(
			String leftImage,
			String rightImage,
			String leftRating,
			String rightRating) {
		
		this.leftImage = leftImage;
		this.rightImage = rightImage;
		
		this.leftRating = leftRating;
		this.rightRating = rightRating;
	}
	
	/**
	 * Get the left image name.
	 */
	public String getLeftName() {
		return this.leftImage;
	}
	
	/**
	 * Get the right image name.
	 */
	public String getRightName() {
		return this.rightImage;
	}
	
	/**
	 * Get the left image rating.
	 */
	public String getLeftRating() {
		return this.leftRating;
	}
	
	/**
	 * Get the right image rating.
	 */
	public String getRightRating() {
		return this.rightRating;
	}
}
