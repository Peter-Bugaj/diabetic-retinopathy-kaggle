package detectors;

import tools.math.VectorTools;

/**
 *Class for processing image data of an eye for detecting the optic nerve.
 * 
 * @author peterbugaj
 */
public class OpticNerveDetection {

	/**
	 * The number of times to divide the complete image by so that
	 * the detection is only performed on a small version of the image.
	 */
	private static final int divFac = 10;
	
	/**
	 * The chosen eye radius to optic nerve radius ratio.
	 */
	private static final float eyeToNerveRatio = 6.3f;
	
	/**
	 * Find the optic nerve, given the source image matrix, the radius of
	 * the eye, and the non eye image map to map where the nerve is located.
	 * 
	 * @param image_matrix
	 * The image source matrix.
	 * @param eyeRadius
	 * The input radius of the eye.
	 * @param nonEyeImage
	 * The map containing the part of the image not belonging to the eye.
	 */
	public static void findNerve(
			short[][][]imageMatrix,
			int eyeRadius,
			boolean [][] nonEyeImage) {

		// Create the mini-image.
		short[][][]mini_image = new short[imageMatrix.length/divFac][imageMatrix[0].length/divFac][3];
		boolean[][]mini_map = new boolean[imageMatrix.length/divFac][imageMatrix[0].length/divFac];

		for(int i = 0; i < imageMatrix.length - divFac; i += divFac) { 
			for(int j = 0; j < imageMatrix[0].length - divFac; j+= divFac) { 
				mini_image[i/divFac][j/divFac] = imageMatrix[i][j];
				mini_map[i/divFac][j/divFac] = nonEyeImage[i][j];
			}			
		}

		// Find the best circle marking the optic
		// nerve using color data analysis.
		float best_ratio = -99;
		int [] best_center = new int[]{0, 0};
		
		int mini_radius = (int) (eyeRadius / (eyeToNerveRatio * divFac));
		for(int i = (int) (mini_radius*1.1); i < mini_image.length - mini_radius*1.1; i+=2) {
			for(int j = (int) (mini_radius*2.1); j < mini_image[0].length - mini_radius*2.1; j+=2) {
				float ratio = evaluateCircle(i , j, mini_radius, mini_image, mini_map);
				if(ratio > best_ratio) {
					best_ratio = ratio;
					best_center = new int[]{i, j};
				}
			}			
		}
		
		// Mark the best results within the image source matrix.
		best_center[0] *= divFac;
		best_center[1] *= divFac;
		int radius = (int) (eyeRadius / eyeToNerveRatio);
		markBestCircle(imageMatrix, nonEyeImage, radius, best_center);
	}
	
	/**
	 * Mark the location of optic nerve wihin the image source matrix.
	 * 
	 * @param img
	 * The image source matrix.
	 * @param nonEyeMap
	 * The map indicating the part of the image not
	 * belonging to the eye.
	 * @param radius
	 * Radius of the optic nerce to mark.
	 * @param center
	 * The location of the optic nerve to mark.
	 */
	private static void markBestCircle(
			short[][][]img,
			boolean [][] nonEyeMap,
			int radius,
			int [] center) {
		
		float ext_radius = radius * 1.2f;

		int bx = (int) (center[0] - ext_radius);
		int by = (int) (center[1] - ext_radius);
		
		for(int i = bx; i < bx + (2*ext_radius); i++) {
			for(int j = by; j < by + (2*ext_radius); j++) {
				
				if (i < 0 || j < 0 || i >= img.length || j >= img[0].length) continue;

				float dist = VectorTools.distance(center, new int[]{i, j});
				if(dist <= ext_radius) {
					nonEyeMap[i][j] = true;
				}
			}			
		}
	}
	
	/**Evaluate the possibility of the optic nerve
	 * existing within the x, y location of the image
	 * with the given radius. Uses colour information
	 * for evaluation.
	 * 
	 * @param x
	 * The predicted x location of the optic nerve.
	 * @param y
	 * The predicted y location of the optic nerve.
	 * @param radius
	 * The predicted radius of the optic nerve.
	 * @param img
	 * The img to evaluate against.
	 * @param nonEyeMap
	 * The map indicating the part of the image not
	 * belonging to the eye, important for the
	 * evaluation to avoid.
	 */
	private static float evaluateCircle(
			int x,
			int y,
			float radius,
			short[][][]img,
			boolean[][]nonEyeMap) {

		float int_radius = radius;
		float ext_radius = radius * 1.5f;
		
		int bx = (int) (x - ext_radius);
		int by = (int) (y - ext_radius);
		
		float int_sum = 0;
		float int_count = 0;
		
		float ext_sum = 0;
		float ext_count = 0;
		
		int [] cent = new int[]{x, y};
		
		for(int i = bx; i < bx + (2*ext_radius); i++) {
			for(int j = by; j < by + (2*ext_radius); j++) {
				
				if (i < 0 || j < 0 || i >= img.length || j >= img[0].length) continue;

				float dist = VectorTools.distance(cent, new int[]{i, j});
				if(dist <= int_radius) {
					if(nonEyeMap[i][j]) return 0;
					int_sum += combineCones(img[i][j]);
					int_count++;
				}
				else if(dist <= ext_radius) {
					if(nonEyeMap[i][j]) continue;
					ext_sum += combineCones(img[i][j]);
					ext_count++;					
				}
			}			
		}
		
		float int_avg = int_sum / Math.max(1, int_count);
		float ext_avg = ext_sum / Math.max(1, ext_count);

		return int_avg / ext_avg;
	}

	/**
	 * Combine the RGB values into one gray scale value.
	 *
	 * @param cones
	 * The RGB values.
	 * @return
	 * The gray scale value.
	 */
	private static short combineCones(short [] cones) {
		return (short)(cones[0]*0.5 + cones[1]*0.5 + cones[2]*0);
	}
}
