package preprocessing;

import java.util.LinkedList;
import java.util.Vector;

import tools.math.Kernels;
import tools.math.VectorTools;

/**
 * Custom class for subtracting the background data from an image.
 *
 * @author peterbugaj
 */
public class BackgroundSubtraction {

	/**
	 * Find the background of the image not belonging to the eye.
	 *
	 * @param imageMatrix
	 * The image source matrix.
	 * @param map
	 * The map marking the part of the image not belonging to the eye.
	 * @param foreGroundStrength
	 * The foreground threshold to determine whether a part of the image
	 * belongs to the eye or not.
	 * @param boundaryThickness
	 * The boundary thickness to create around the part of the image
	 * that is part of the eye. This helps reduce noise where a pixel
	 * can belong partially within or outside the region of an eye
	 * due to the boundary dividing the eye from the rest of the image
	 * being too thin.
	 * @return
	 * An array
	 * [0] - The size of the background in number of pixels.
	 * [1] - The radius of the eye in number of pixels.
	 */
	public static int [] findBlackBackground(
			short[][][] imageMatrix,
			boolean [][] map,
			int foreGroundStrength,
			int boundaryThickness) {

		int counter = 0;

		Vector<short[]>boundary = new Vector<short[]>();

		// Find and expand the image boundary on the top
		// left corner of the image.
		for(int i = 0; i < 200; i++) {
			for(int j = 0; j < 200; j++) {
				if(map[i][j]) continue;
				if(pixel_sum(imageMatrix[i][j]) > foreGroundStrength) continue;
				counter += expandBlackBackground(imageMatrix, map, i, j, foreGroundStrength, boundary);
			}			
		}
		
		// Find and expand the image boundary on the top
		// right corner of the image.
		for(int i = imageMatrix.length - 200; i < imageMatrix.length; i++) {
			for(int j = 0; j < 200; j++) {
				if(map[i][j]) continue;
				if(pixel_sum(imageMatrix[i][j]) > foreGroundStrength) continue;
				counter += expandBlackBackground(imageMatrix, map, i, j, foreGroundStrength, boundary);
			}			
		}

		// Find and expand the image boundary on the bottom
		// left corner of the image.
		for(int i = 0; i < 200; i++) {
			for(int j = imageMatrix[0].length - 200; j < imageMatrix[0].length; j++) {
				if(map[i][j]) continue;
				if(pixel_sum(imageMatrix[i][j]) > foreGroundStrength) continue;
				counter += expandBlackBackground(imageMatrix, map, i, j, foreGroundStrength, boundary);
			}			
		}
		
		// Find and expand the image boundary on the bottom
		// right corner of the image.
		for(int i = imageMatrix.length - 200; i < imageMatrix.length; i++) {
			for(int j = imageMatrix[0].length - 200; j < imageMatrix[0].length; j++) {
				if(map[i][j]) continue;
				if(pixel_sum(imageMatrix[i][j]) > foreGroundStrength) continue;
				counter += expandBlackBackground(imageMatrix, map, i, j, foreGroundStrength, boundary);
			}			
		}
		
		// Expand the boundary between the background detected and the eye.
		// This is done by further labelling more pixels within the map as
		// belonging to the background.
		for(int k = 0; k < boundaryThickness; k++) {
			Vector<short[]> new_boundary = new Vector<short[]>();
			for(int i = 0; i < boundary.size(); i++) {
				counter += expandBlackBackgroundBorder(imageMatrix, map, new_boundary, boundary.get(i));
			}
			
			boundary = new_boundary;
		}

		// Estimate the radius of the eye within the image.
		int corner_aa = 0;
		int corner_bb = 0;
		while(true) {
			if(!map[corner_aa][corner_bb]) {
				break;
			}
			if(corner_aa == map.length - 1 || corner_bb == map[0].length - 1) {
				corner_aa = 0;
				corner_bb = 0;
				break;
			}
			corner_aa++;
			corner_bb++;
		}

		int corner_bb_x = map.length - 1;
		int corner_bb_y = map[0].length - 1;
		while(true) {
			if(!map[corner_bb_x][corner_bb_y]) {
				break;
			}
			if(corner_bb_x == 0 || corner_bb_y == 0) {
				corner_bb_x = map.length - 1;
				corner_bb_y = map[0].length - 1;
				break;
			}
			corner_bb_x--;
			corner_bb_y--;
		}

		int radius = (int) (VectorTools.distance(
			new int[]{corner_aa, corner_bb}, new int[]{corner_bb_x, corner_bb_y}) / 2f);

		// Finally return the size of the bakground and
		// the radius of the eye computed above.
		return new int[]{counter, radius};
	}

	/**
	 * Run a connected components algorithm for expanding the black
	 * background within the image.
	 * 
	 * @param image
	 * The image to run the connected component algorithm inside of.
	 * @param map:
	 * Map keeping track of the black background detected thus far.
	 * @param i
	 * The x location to start the connected component algorithm at.
	 * @param j
	 * The y location to start the connected component algorithm at.
	 * @param foreGroundStrength
	 * The foreground threshold to determine whether a part of the image
	 * belongs to the eye or not.
	 * @param boundary
	 * Data structure keeping track of the points that have hit the
	 * part of the image no longer considered as part of the background.
	 * @return
	 * The number of pixels found by the connected component algorithm
	 * to belong to the background.
	 */
	private static int expandBlackBackground(
			short[][][] image,
			boolean[][]map,
			int i,
			int j,
			int foreGroundStrength,
			Vector<short[]> boundary) {

		int counter = 0;
		
		LinkedList<int[]> stack = new LinkedList<int[]>();
		stack.push(new int[]{i, j});
		
		while(stack.size() > 0) {
			
			int [] next_coord = stack.removeLast();
			int nx = next_coord[0];
			int ny = next_coord[1];
			
			if (map[nx][ny]) continue;
			map[nx][ny] = true;
			counter++;

			boolean boundary_hit = false;
			for(byte d = 0; d < Kernels.neighourhoodSmall.length; d++) {
				byte m = Kernels.neighourhoodSmall[d][0];
				byte n = Kernels.neighourhoodSmall[d][1];

				if(nx-1+m < 0 || nx-1+m >= image.length) continue;
				if(ny-1+n < 0 || ny-1+n >= image[0].length) continue;

				if (map[nx-1+m][ny-1+n]) continue;
				if(pixel_sum(image[nx-1+m][ny-1+n]) > foreGroundStrength) {
					boundary_hit = true;
					continue;
				}

				stack.push(new int []{nx-1+m, ny-1+n});
			}
			
			if(boundary_hit) {
				boundary.add(new short[]{(short) nx, (short) ny});
			}
		}
		
		return counter;
	}
	
	/**
	 * Helper function for expanding the black border found between
	 * the background and the eye. This function is executed for reducing
	 * any noise caused by pixels accidentally belonging to the eye or
	 * background, but being labelled as otherwise due to the thin
	 * background.
	 *
	 * @param image
	 * The image source matrix containing the background and the eye.
	 * @param map
	 * The map keeping track which part of the image belongs to the
	 * background.
	 * @param newBoundary
	 * The new boundary being created from the expansion of the previous
	 * existing boundary.
	 * @param point
	 * The point to expand the current boundary point from.
	 * @return
	 * The number of pixels the background has increased by as a result
	 * of the expansion of the boundary.
	 */
	private static int expandBlackBackgroundBorder(
			short[][][]image,
			boolean[][]map,
			Vector<short[]>newBoundary,
			short[]point) {

		short nx = point[0];
		short ny = point[1];

		int counter = 0;
		
		for(byte i = 0; i < Kernels.neighMap.length; i++) {
			byte m = Kernels.neighMap[i][0];
			byte n = Kernels.neighMap[i][1];

			if(nx-1+m < 0 || nx-1+m >= image.length) continue;
			if(ny-1+n < 0 || ny-1+n >= image[0].length) continue;

			if (map[nx-1+m][ny-1+n]) continue;
			map[nx-1+m][ny-1+n] = true;
			counter++;
			newBoundary.add(new short[] {(short) (nx-1+m), (short) (ny-1+n)});
		}
		
		return counter;
	}
	
	/**
	 * Helper function to return the sum of pixel values.
	 *
	 * @param pixels
	 * The pixels to return the sum for.
	 * @return
	 * Sum of the pixel values.
	 */
	private static int pixel_sum(short[]pixels) {
		return pixels[0] + pixels[1] + pixels[2];
	}
	
	/**
	 * Run a background substraction algorithm on the provided image
	 * by equalizing the distribution of colours across the entire image.
	 * 
	 * @param map
	 * The map containing the imag with the subtracted background.
	 * @param image_matrix
	 * The image matrix to run the background subtraction algorithm on.
	 * @param mediumValue
	 * The value to equalize the colours within the image to.
	 * @param boxSize
	 * The size of the neighbourhood used when equalizing the pixel value
	 * within an image relative to the average colour intentisity of
	 * its neighbouring pixels.
	 */
	public static void substract(
			short[][][] map,
			short[][][] imageMatrix,
			short mediumValue,
			int boxSize) {

		int sum = 0;
		int counter = 0;
		
		double normalizer_min = 1000;
		double normalizer_max = 0;
		
		for(int j = 0; j < imageMatrix[0].length; j++) {
			boolean new_row = true;
			for(int i = 0; i < imageMatrix.length; i++) {
				if (new_row) {
					new_row = false;
					sum = 0;
					counter = 0;	
					
					for(int m = i - boxSize; m < i + boxSize + 1; m++) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (m < 0 || n < 0 || m >= imageMatrix.length || n >= imageMatrix[0].length) continue;
							sum += imageMatrix[m][n][1];
							counter++;
						}					
					}
				} else {
				
					// Update horizontal sides
					if (i - boxSize > 0) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (n < 0 || n >= imageMatrix[0].length) continue;
							sum -= imageMatrix[i-boxSize-1][n][1];
							counter--;
						}	
					}
	
					if (i + boxSize < imageMatrix.length) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (n < 0 || n >= imageMatrix[0].length) continue;
							sum += imageMatrix[i+boxSize][n][1];
							counter++;
						}	
					}
				}

				int diff =  mediumValue - (sum / counter);
				
				short mean_offset = (short)(imageMatrix[i][j][1] + diff);

				map[i][j] = new short[]{0, mean_offset, 0};
				normalizer_min = Math.min(normalizer_min, mean_offset);
				normalizer_max = Math.max(normalizer_max, mean_offset);
			}			
		}

		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[0].length; j++) {
				map[i][j] = new short[]{0, (short) (255 * (map[i][j][1]-normalizer_min) / (normalizer_max - normalizer_min)), 0};
			}			
		}
	}
	
	/**
	 * Create a covariance map for the given image. At each pixel, computes
	 * the covariance value, using the pixels in neighbourhood bounded by
	 * the size of the boxSize.
	 *
	 * @param map
	 * The map to store the covariance values.
	 * @param imageMatrix
	 * The image matrix to analyze for the computation
	 * of the covariance values.
	 * @param boxSize
	 * The size of the box bounding the neighbourhood of pixel for
	 * which the covariance value will be created for at each
	 * pixel in the image.
	 */
	public static void createVarianceMap(
			short[][][] map,
			short[][][] imageMatrix,
			int boxSize) {

		int sum2 = 0;
		
		double counter = 0;

		for(int j = 0; j < imageMatrix[0].length; j++) {
			boolean new_row = true;
			for(int i = 0; i < imageMatrix.length; i++) {
				if (new_row) {
					new_row = false;
					sum2 = 0;
					counter = 0;	
					
					for(int m = i - boxSize; m < i + boxSize + 1; m++) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (m < 0 || n < 0 || m >= imageMatrix.length || n >= imageMatrix[0].length) continue;
							sum2 += imageMatrix[m][n][1];
							counter++;
						}					
					}
				} else {
				
					// Update horizontal sides
					if (i - boxSize > 0) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (n < 0 || n >= imageMatrix[0].length) continue;
							sum2 -= imageMatrix[i-boxSize-1][n][1];
							counter--;
						}	
					}
	
					if (i + boxSize < imageMatrix.length) {
						for(int n = j - boxSize; n < j + boxSize + 1; n++) {
							if (n < 0 || n >= imageMatrix[0].length) continue;
							sum2 += imageMatrix[i+boxSize][n][1];
							counter++;
						}	
					}
				}

				double mean_2 = sum2 / counter;
				
				double variance_sum2 = 0;
				double variance_count = 0;
				for(int m = i - boxSize; m < i + boxSize + 1; m+=20) {
					for(int n = j - boxSize; n < j + boxSize + 1; n+=20) {
						if (m < 0 || n < 0 || m >= imageMatrix.length || n >= imageMatrix[0].length) continue;
						variance_sum2 += Math.pow(imageMatrix[m][n][1] - mean_2, 2);
						variance_count++;
					}					
				}

				short var_2 = (short) Math.sqrt(variance_sum2 / variance_count);
				map[i][j] = new short[]{0, var_2, 0};
			}			
		}
	}
}
