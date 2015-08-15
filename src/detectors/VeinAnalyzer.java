package detectors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import retinopathy.structures.Constants;
import retinopathy.structures.Vein;
import retinopathy.structures.VeinFork;

import tools.math.Kernels;
import tools.math.PCATools;
import tools.math.VectorTools;

/**
 * Class for analyzing an image of an eye for a network of veins.
 * 
 * @author Peter Bugaj
 */
public class VeinAnalyzer {

	public static final int VEIN_MAP_FACTOR = 2;
	
	/**
	 * Analyze an image of an eye for a network of veins.
	 *
	 * @param features
	 * The features structure to output the information about
	 * the detected set of veins.
	 * @param filtered_image
	 * The filtered image source matrix to run the vein detector on.
	 * @param noiseRemovalIterations
	 * Number o iterations to clean the detected veins for noise.
	 * @param minimal_vein_length
	 * The minimal vein length to accept for detection.
	 * @param nonEyeImageSize
	 * The size of the image not displaying the eye.
	 * @param scalingFac
	 * Scaling factor used for drawing the veins for a visual representation.
	 * @return
	 * A map of the image containing the visual
	 * representation of the veins detected.
	 */
	public static boolean [][] Analyze(
			ImageFeatures features,
			short [][][] filteredImage,
			int noiseRemovalIterations,
			int minimalVeinLength,
			float nonEyeImageSize,
			float scalingFac) {

		// Prepare the data structures.
		Vector<VeinFork> vein_forks = new Vector<VeinFork>();
		int MARKER = 1;
		int [][] vein_map = new int[filteredImage.length][filteredImage[0].length];
		float eye_pixel_size = Math.max((filteredImage.length * filteredImage[0].length) - nonEyeImageSize, 1);
		
		// Find the vein forks.
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				if(filteredImage[i][j][0] == 0) continue;

				byte num_connections = forkCount(filteredImage, i, j);
				if(num_connections > 2 || num_connections == 1) {
					VeinFork new_fork = new VeinFork(i, j, num_connections);
					vein_forks.add(new_fork);
					vein_map[i][j] = MARKER++;
				}
			}
		}

		// Find the connecting veins between each detected vein fork.
		Vector<Vein> retina_veins = new Vector<Vein>();
		int FORK_OFFSET = MARKER;
		for(int i = 0; i < vein_forks.size(); i++) {
			VeinFork next_fork = vein_forks.get(i);
			MARKER = markConnectingVeins(
					filteredImage, vein_map, 
					retina_veins, vein_forks, next_fork,
					FORK_OFFSET, MARKER);
		}

		// Optimize the connections between each fork
		// in case they are expecting more veins than received.
		for(int i = 0; i < vein_forks.size(); i++) {
			VeinFork next_fork = vein_forks.get(i);
			next_fork.optimizeConnections();
		}


		// Remove any small veins from the vein network as noise.
		removeShortVeins(retina_veins, noiseRemovalIterations, minimalVeinLength);

		// Clear the image.
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				filteredImage[i][j] = new short[]{0,0,0};
			}
		}

		// Draw the veins for visualization.
		return analyzeVein(features, filteredImage, retina_veins, eye_pixel_size, scalingFac);
	}

	/**
	 * Remove any short veins as noise.
	 * 
	 * @param retinaVeins
	 * The set of veins to process for noise.
	 * @param noiseRemovalIterations
	 * Number of times to iterate over the set of veins for noise detection.
	 * @param minimalLength
	 * The minimal vein of vein before it should be marked as noise.
	 */
	private static void removeShortVeins(
		Vector<Vein> retinaVeins,
		int noiseRemovalIterations,
		int minimalLength
		) {

		for(int h = 0; h < noiseRemovalIterations; h++) {
			for(int i = 0; i < retinaVeins.size(); i++) {
				Vein temp = retinaVeins.get(i);

				if (temp.getMark() == Constants.VEIN_CYCLE_MARK || temp.getMark() == Constants.VEIN_SHORT_MARK) {
					continue;
				}

				// Case where vein is a cycle
				if(temp.getConnectionA() == null) {
					temp.setMark(Constants.VEIN_CYCLE_MARK);

					VeinFork connectionsB = temp.getConnectionB();
					connectionsB.removeVein(temp);					
					temp.removeConnectionB();
				}
				else if(temp.getConnectionB() == null) {
					temp.setMark(Constants.VEIN_CYCLE_MARK);

					VeinFork connectionsA = temp.getConnectionA();
					connectionsA.removeVein(temp);
					temp.removeConnectionA();
				}
				else if(temp.getConnectionA().getVeins().length == 1) {
					if(temp.getConnectionB().getVeins().length == 2) continue; 
					if(temp.getSize() < minimalLength) {
						temp.setMark(Constants.VEIN_SHORT_MARK);

						VeinFork connectionsA = temp.getConnectionA();
						connectionsA.removeVein(temp);
						temp.removeConnectionA();

						VeinFork connectionsB = temp.getConnectionB();
						connectionsB.removeVein(temp);
						temp.removeConnectionB();
					}
				}
				else if(temp.getConnectionB().getVeins().length == 1) {
					if(temp.getConnectionA().getVeins().length == 2) continue; 
					if(temp.getSize() < minimalLength) {
						temp.setMark(Constants.VEIN_SHORT_MARK);

						VeinFork connectionsA = temp.getConnectionA();
						connectionsA.removeVein(temp);
						temp.removeConnectionA();

						VeinFork connectionsB = temp.getConnectionB();
						connectionsB.removeVein(temp);
						temp.removeConnectionB();
					}
				}
			}	
		}
	}

	/**
	 * Check whether or not a vein forks at this point.
	 * 
	 * @param filtered_image
	 * The image matrix containing the source data.
	 * @param x
	 * The x location to check the vein at.
	 * @param y
	 * The y location to check the vein at.
	 */
	private static byte forkCount(short[][][]filtered_image, int x, int y) {
		byte num_connections = 0;
		for(byte i = 0; i < Kernels.neighourhoodSmall.length; i++) {
			byte m = Kernels.neighourhoodSmall[i][0];
			byte n = Kernels.neighourhoodSmall[i][1];

			if(x-1+m < 0 || y-1+n < 0 || x-1+m >= filtered_image.length || y-1+n >= filtered_image[0].length) continue;
			if(filtered_image[x-1+m][y-1+n][0] != 0) {
				num_connections++;
			}
		}

		return num_connections;
	}

	/**
	 * Mark the connecting veins expanding from the vein fork.
	 * 
	 * @param filtered_image
	 * The image matrix containing the source data.
	 * @param veinMap
	 * The matrix marking the different veins detected within the image.
	 * @param retinaVeins
	 * The current detected veins stored in a vector structure.
	 * @param veinForks
	 * The vein forks detected within the image.
	 * @param fork_A
	 * The fork to analyze for connecting veins.
	 * @param FORK_OFFSET
	 * The fork offset value used for labellig in the vein map.
	 * @param VEIN_MARKER
	 * The current vein marker used for labelling in the vein map.
	 * @return
	 * The newly incremented vein marker.
	 */
	private static int markConnectingVeins(
			short[][][] filtered_image,
			int[][] veinMap,
			Vector<Vein> retinaVeins,
			Vector<VeinFork> veinForks,
			VeinFork fork_A,
			int FORK_OFFSET,
			int VEIN_MARKER) {

		short x = fork_A.getCoordx();
		short y = fork_A.getCoordy();
		int fork_A_label = veinMap[x][y];

		for(short i = 0; i < Kernels.neighourhoodSmall.length; i++) {
			byte m = Kernels.neighourhoodSmall[i][0];
			byte n = Kernels.neighourhoodSmall[i][1];

			if(!isInBounds(filtered_image, (short)(x-1+m), (short)(y-1+n))) continue;

			// Case where another forking vein is encountered.
			// In this case construct a vein object storig just those two points.
			if(isVeinFork(veinMap, x-1+m, y-1+n, FORK_OFFSET)) {

				int fork_id = veinMap[x-1+m][y-1+n];
				VeinFork fork_B = veinForks.get(fork_id - 1);

				if (fork_B.isMarked() || fork_B.getVeins().length > 0) continue;

				Vein connecting_vein = new Vein();

				fork_A.addVein(connecting_vein);
				connecting_vein.setConnectionA(fork_A);
				connecting_vein.setPointA(new short[]{x, y});
				connecting_vein.appendIntensity(filtered_image[x][y][0]);

				fork_B.addVein(connecting_vein);
				connecting_vein.setConnectionB(fork_B);
				connecting_vein.setPointB(new short[]{(short) (x-1+m), (short) (y-1+n)});
				connecting_vein.appendIntensity(filtered_image[x-1+m][y-1+n][0]);

				connecting_vein.setSize(2);

				retinaVeins.add(connecting_vein);
				VEIN_MARKER++;
				
				continue;
			}

			// Case where another forking vein is encountered that has already
			// been iterated by the algorithm. In this case ignore the point.
			// Or the case where the vein has already been iterated over.
			if(veinMap[x-1+m][y-1+n] > 0) {
				continue;
			}

			// Case were a vein does not ven exist.
			// In this case ignore the point.
			if(filtered_image[x-1+m][y-1+n][0] == 0) {
				continue;
			}

			// Otherwise we have a valid vein to track. Track the vein.
			Vein connecting_vein = new Vein();
			retinaVeins.add(connecting_vein);

			fork_A.addVein(connecting_vein);
			connecting_vein.setConnectionA(fork_A);	
			connecting_vein.setPointA(new short[]{x, y});
			connecting_vein.appendIntensity(filtered_image[x][y][0]);
			traceVein(
					filtered_image, veinMap,
					veinForks, fork_A_label, connecting_vein,
					(short)(x-1+m), (short)(y-1+n),
					FORK_OFFSET, VEIN_MARKER);
			VEIN_MARKER++;
		}

		fork_A.mark();	
		return VEIN_MARKER;
	}

	/**
	 * Helper function for tracing a vein within an imaage.
	 *
	 * @param filteredImage
	 * The image in whcih to trace the vein.
	 * @param veinMap
	 * Map keeping track of the different veins labelled within the image.
	 * @param veinForks
	 * The set of vein forks detected within the image with a
	 * label equal to their index within the vector plus one.
	 * @param forkAlabel
	 * The label of the fork from which the current
	 * vein being traced is starting at from.
	 * @param connectingVein
	 * The vein in focus that is being traced.
	 * @param s_x
	 * The x location from where the vein is to be traced from.
	 * @param s_y
	 * The y location from where the vein is to be traced from.
	 * @param FORK_OFFSET
	 * The fork offset value used for labellig in the vein map.
	 * @param VEIN_MARKER
	 * The current vein marker used for labelling in the vein map.
	 */
	private static void traceVein(
			short[][][] filteredImage,
			int[][] veinMap,
			Vector<VeinFork> veinForks,
			int forkAlabel,
			Vein connectingVein,
			short s_x,
			short s_y,
			int FORK_OFFSET,
			int VEIN_MARKER) {

		short nx = s_x;
		short ny = s_y;
		veinMap[nx][ny] = VEIN_MARKER;
		connectingVein.addPoint(new short[]{nx, ny});
		connectingVein.appendIntensity(filteredImage[nx][ny][0]);

		int size_counter = 1;

		top: while(true) {

			// Search for the next extension to track the vein.
			boolean extension_found = false;
			for(short i = 0; i < Kernels.neighourhoodSmall.length; i++) {
				byte m = Kernels.neighourhoodSmall[i][0];
				byte n = Kernels.neighourhoodSmall[i][1];

				if(!isInBounds(filteredImage, (short)(nx-1+m), (short)(ny-1+n))) continue;

				// Case where the starting point of the vein is hit. Ignore.
				if(veinMap[nx-1+m][ny-1+n] == forkAlabel) continue; 

				// Case where the next forking point is encountered.
				if(isVeinFork(veinMap, nx-1+m, ny-1+n, FORK_OFFSET)) {
					int fork_B_label = veinMap[nx-1+m][ny-1+n];
					VeinFork fork_B = veinForks.get(fork_B_label - 1);

					fork_B.addVein(connectingVein);
					connectingVein.setConnectionB(fork_B);
					connectingVein.setPointB(new short[]{(short) (nx-1+m), (short) (ny-1+n)});
					connectingVein.appendIntensity(filteredImage[nx-1+m][ny-1+n][0]);

					break top;
				}

				// Case were the vein point is already marked.
				if(veinMap[nx-1+m][ny-1+n] > 0) {
					continue;					
				}

				// Case where no vein exists on the point.
				if(filteredImage[nx-1+m][ny-1+n][0] == 0) {
					continue;
				}

				if(filteredImage[nx-1+m][ny-1+n][0] != 0) {
					veinMap[nx-1+m][ny-1+n] = VEIN_MARKER;
					connectingVein.addPoint(new short[]{(short) (nx-1+m), (short) (ny-1+n)});
					connectingVein.appendIntensity(filteredImage[nx-1+m][ny-1+n][0]);
					nx = (short) (nx-1+m);
					ny = (short) (ny-1+n);
					extension_found = true;
					size_counter++;
				}
			}

			if(!extension_found) {
				veinMap[nx][ny] = VEIN_MARKER;
				connectingVein.setPointB(new short[]{nx, ny});
				connectingVein.appendIntensity(filteredImage[nx][ny][0]);
				break;
			}
		}

		connectingVein.setSize(size_counter+2);
	}

	/**
	 * Helper function to check whether a point is in the bounds of an image.
	 * 
	 * @param image
	 * The image associated to the bounds to check.
	 * @param x
	 * The x location to check.
	 * @param y
	 * The y location to check.
	 */
	private static boolean isInBounds(short [][][] image, short x, short y) {
		return x >= 0 && y >= 0 && x < image.length && y  < image[0].length;
	}

	/**
	 * Helper function to determine when a labelled point is a vein fork.
	 * 
	 * @param veinMap
	 * The vein map containing the labelled vein data.
	 * @param x
	 * The x location of the vein being checked.
	 * @param y
	 * The y location of the vein being checked.
	 * @param FORK_OFFSET
	 * The fork label offset to use in comparison for the labels.
	 */
	private static boolean isVeinFork(int [][] veinMap, int x, int y, int FORK_OFFSET) {
		return veinMap[x][y] > 0 && veinMap[x][y] < FORK_OFFSET;
	}
	
	/**
	 * Helper function for analyzing the veins.
	 *
	 * @param features
	 * The feature structure for storing the set of analyzed veins to.
	 * @param filteredImage
	 * The image source matrix containing the eye image data.
	 * @param retinaVeins
	 * The current detected veins stored in a vector structure.
	 * @param eyePixelSize
	 * The size of the eye being analyzed within the image,
	 * in number of pixels.
	 * @param scalingFac
	 * Scaling factor used for drawing the veins for a visual representation.
	 */
	private static boolean[][] analyzeVein(
			ImageFeatures features,
			short[][][]filteredImage,
			Vector<Vein> retina_veins,
			float eyePixelSize,
			float scalingFac) {
		
		boolean [][] vein_map = new boolean
				[(filteredImage.length/VEIN_MAP_FACTOR) + VEIN_MAP_FACTOR]
				[(filteredImage[0].length/VEIN_MAP_FACTOR) + VEIN_MAP_FACTOR];
		
		// Compute the average and standard deviation of vein values.
		int valid_vein_count = 0;
		for(int i = 0; i < retina_veins.size(); i++) {
			Vein temp = retina_veins.get(i);
			if (temp.getMark() == 0) {
				valid_vein_count++;
			}
		}
		float [] valid_vein_values = new float[valid_vein_count];
		valid_vein_count = 0;
		for(int i = 0; i < retina_veins.size(); i++) {
			Vein temp = retina_veins.get(i);
			if (temp.getMark() == 0) {
				valid_vein_values[valid_vein_count++] = (temp.getIntensity() / (temp.getSize()+0.0f));
			}
		}
		float vein_mean = PCATools.getMean(valid_vein_values);
		float vein_std = PCATools.getStdDev(valid_vein_values);
		
		int strong_vein_pixel_count = 0;
		int medium_vein_pixel_count = 0;
		int weak_vein_pixel_count = 0;

		Vector<Vein> strong_veins = new Vector<Vein>();
		Vector<Vein> medium_veins = new Vector<Vein>();
		Vector<Vein> weak_veins = new Vector<Vein>();
		for(int i = 0; i < retina_veins.size(); i++) {

			Vein temp = retina_veins.get(i);
			if (temp.getMark() == 0) {
				float val = (temp.getIntensity() / (temp.getSize()+0.0f));
				
				if (val > vein_mean + 0.5*vein_std) {
					strong_veins.add(temp);
					strong_vein_pixel_count += temp.getSize();
					temp.setVeinStrength(Constants.VEIN_STRENGTH_STRONG);
				} else if (val > vein_mean - 0.5*vein_std) {
					medium_veins.add(temp);
					medium_vein_pixel_count += temp.getSize();
					temp.setVeinStrength(Constants.VEIN_STRENGTH_MEDIUM);
				} else if (val > vein_mean - 1.5*vein_std) {
					weak_veins.add(temp);
					weak_vein_pixel_count += temp.getSize();
					temp.setVeinStrength(Constants.VEIN_STRENGTH_WEAK);
				}
			}
		}
		
		float total_vein_count = Math.max(1, strong_veins.size() + medium_veins.size() + weak_veins.size());
		features.addToFeatureLog("STRONG_VEIN_RATIO#" +
				(strong_vein_pixel_count / eyePixelSize) );
		features.addToFeatureLog("STRONG_VEIN_RATIO#" +
				(strong_veins.size() / total_vein_count) );
		
		features.addToFeatureLog("MEDIUM_VEIN_RATIO#" +
				(medium_vein_pixel_count / eyePixelSize) );
		features.addToFeatureLog("MEDIUM_VEIN_RATIO#" +
				(medium_veins.size() / total_vein_count) );
		
		features.addToFeatureLog("WEAK_VEIN_RATIO#" +
				(weak_vein_pixel_count / eyePixelSize) );
		features.addToFeatureLog("WEAK_VEIN_RATIO#" +
				(weak_veins.size() / total_vein_count) );
		
		features.addToFeatureLog("");

		analyzeSubsetVeins(features, filteredImage, strong_veins, null, Constants.VEIN_STRENGTH_STRONG, scalingFac, eyePixelSize);
		return vein_map;
	}

	/**
	 * Helper function for analyzing a subset of veins.
	 * 
	 * @param features
	 * The feature structure for storing the set of analyzed veins to.
	 * @param filteredImage
	 * The image source matrix containing the eye image data.
	 * @param subsetVeins
	 * The subset of veins that are to be analyzed.
	 * @param veinMap
	 * The matrix map to keep track of the veins analyzed within the image.
	 * @param veinStrength
	 * The strength of the veins that are being analyzed
	 * @param scalingFac
	 * Scaling factor used for drawing the veins for a visual representation.
	 * @param eyePixelSize
	 * The size of the eye being analyzed within the image,
	 * in number of pixels.
	 */
	private static void analyzeSubsetVeins(
			ImageFeatures features,
			short[][][]filteredImage,
			Vector<Vein> subsetVeins,
			boolean[][]veinMap,
			int veinStrength,
			float scalingFac,
			float eyePixelSize) {

		int fork_marker = VeinFork.getNextMarkerValue();
		int fork_count = 0;
		
		// Combines the veins together.
		appendVeins(subsetVeins);
		
		// Remove duplications.
		subsetVeins = removeDuplicateVeins(subsetVeins);
		

		// Analyze the curvatures.
		int [] vein_curve_sums = new int [15];
		Vector<short[]>fork_points = new Vector<short[]>();
		float fork_distance_from_center_sum = 0;
		short[]eye_center = new short[]{(short) (filteredImage.length/2), (short) (filteredImage[0].length/2)};
		
		for(int i = 0; i < subsetVeins.size(); i++) {
			
			// Grab the data about the forks
			Vein temp = subsetVeins.get(i);
			if(validForkAnalysis(temp.getConnectionA(), fork_marker)) {
				fork_distance_from_center_sum += VectorTools.distance(eye_center, temp.getConnectionA().getCoord());
				temp.getConnectionA().setMark(fork_marker);
				fork_count++;
				fork_points.add(temp.getConnectionA().getCoord());
			}
			if(validForkAnalysis(temp.getConnectionB(), fork_marker)) {
				fork_distance_from_center_sum += VectorTools.distance(eye_center, temp.getConnectionB().getCoord());
				temp.getConnectionB().setMark(fork_marker);
				fork_count++;
				fork_points.add(temp.getConnectionB().getCoord());
			}

			// Grab the data about the veins
			float curvature = computeCurvature(temp);
			int curve_index_offset = 0;
			if (1 < curvature && curvature <= 3) curve_index_offset = 0;
			if (3 < curvature && curvature <= 6) curve_index_offset = 1;
			if (6 < curvature && curvature <= 9) curve_index_offset = 2;
			if (9 < curvature && curvature <= 12) curve_index_offset = 3;
			if (12 < curvature) curve_index_offset = 4;
			
			if(temp.getSize() > 75 * scalingFac) {
				vein_curve_sums[0 + curve_index_offset] += temp.getSize();
			} else if(temp.getSize() > 50 * scalingFac) {
				vein_curve_sums[5 + curve_index_offset] += temp.getSize();
			} else {
				vein_curve_sums[10 + curve_index_offset] += temp.getSize();
			}
			
			drawVein(temp, filteredImage, new short[]{255, 0, 0},
				new short[]{(short) (i*11), (short) (i*33), (short) (i*22)}, veinMap);	
		}
		
		// Compute the vein statistics
		for(int i = 0; i < vein_curve_sums.length; i++) {
			features.addToFeatureLog("VEIN_CURVATURE|" +  "#" + (vein_curve_sums[i] / eyePixelSize));
		}
		features.addToFeatureLog("");

		// Compute the fork statistics
		double std_x = 0;
		double std_y = 0;
		if(fork_points.size() > 0) {
			short[][]fork_points_array = new short[fork_points.size()][2];
			fork_points.toArray(fork_points_array);
			fork_points_array = VectorTools.getTranspose(fork_points_array);
			std_x = PCATools.getStdDev(fork_points_array[0]);
			std_y = PCATools.getStdDev(fork_points_array[1]);	
		}
		
		String strength = veinStrength == Constants.VEIN_STRENGTH_STRONG ?
			"STRONG" : veinStrength == Constants.VEIN_STRENGTH_MEDIUM ?
			"MEDIUM" : "WEAK";
		features.addToFeatureLog("FORK_COUNT|" + strength+ "#" + fork_count);
		features.addToFeatureLog("STANDARD_DEVIATION_FORK_X|" + strength+ "#" + std_x);
		features.addToFeatureLog("STANDARD_DEVIATION_FORK_Y|" + strength+ "#" + std_y);

		features.addToFeatureLog("FROM_CENTER_FORK|" + strength + "#" +
			((fork_distance_from_center_sum * scalingFac)/Math.max(1, fork_count)));

		features.addToFeatureLog("");
	}
	
	/**
	 * Helper function for joining two or more veins together
	 * into a large vein, if no vein fork is separating them.
	 * 
	 * @param veins
	 * The set of veins to iterate through
	 * and two join together if possible.
	 */
	private static void appendVeins(Vector<Vein> veins) {
		for(int i = 0; i < veins.size(); i++) {
			Vein temp = veins.get(i);

			for (int s = 0; s < 2; s++) {
				
				VeinFork forkToDelete = s == 0 ? temp.getConnectionA() : temp.getConnectionB();
				VeinFork forkToKeep = s == 0 ? temp.getConnectionB() : temp.getConnectionA();
				if (forkToDelete == null) {
					continue;
				}
				
				int a_count = 0;
				Vein connecting_vein = null;
				for(int j = 0; j < forkToDelete.getVeins().length; j++) {
					if(
							forkToDelete.getVeins()[j].getId() == temp.getId() ||
							forkToDelete.getVeins()[j].getVeinStrength() != temp.getVeinStrength() ||
							forkToDelete.getVeins()[j].getConnectionA().ID == forkToKeep.ID ||
							forkToDelete.getVeins()[j].getConnectionB().ID == forkToKeep.ID) {
						continue;
					}
					connecting_vein = forkToDelete.getVeins()[j];
					a_count++;
				}
				
				if(a_count == 1) {
					//connecting_vein.MARK = Vein.IGNORE_MARK;
					temp.appendVein(forkToDelete, connecting_vein);
					connecting_vein.duplicate(temp);
				}	
			}
		}
	}

	/**
	 * Helper function for removing duplicate veins
	 * from a set of veins, using vein ID comparison.
	 * 
	 * @param veins
	 * The set of veins to analyze for duplication.
	 */
	private static Vector<Vein> removeDuplicateVeins(Vector<Vein> veins) {
		Vein[]strong_veins_array = new Vein[veins.size()];
		veins.toArray(strong_veins_array);
		Arrays.sort(strong_veins_array, new Comparator<Vein>(){
			public int compare(Vein a, Vein b) {
				return a.getId() > b.getId() ? 1 : a.getId() < b.getId() ? -1 : 0;
			}});
		
		Vector<Vein> new_veins = new Vector<Vein>();
		int prev_idx = -1;
		for (int i = 0; i < strong_veins_array.length; i++) {
			if(strong_veins_array[i].getId() == prev_idx) continue;
			
			new_veins.add(strong_veins_array[i]);
			prev_idx = strong_veins_array[i].getId();
		}
		
		return new_veins;
	}
	
	/**
	 * Helper function for drawing a vein for
	 * visualization of the detected veins.
	 * 
	 * @param vein
	 * The vein to draw.
	 * @param image
	 * The image source matrix to draw the vein on.
	 * @param forkColor
	 * The color to use for draw the vein forks of the vein.
	 * @param veinColor
	 * The color to use for drawing the vein.
	 * @param veinMap
	 * The map to keep track of where the veins
	 * had been drawn within the image.
	 */
	private static void drawVein(
			Vein vein,
			short[][][]image,
			short[]forkColor,
			short[]veinColor,
			boolean[][]veinMap) {

		for(int i = 0; i < vein.getPoints().size(); i++) {
			short[]next_point = vein.getPoints().get(i);
			image[next_point[0]][next_point[1]] = veinColor;
			if(veinMap != null) {
				veinMap[next_point[0]/VEIN_MAP_FACTOR][next_point[1]/VEIN_MAP_FACTOR] = true;
			}
		}

		image[vein.getPointA()[0]][vein.getPointA()[1]] = forkColor;
		image[vein.getPointB()[0]][vein.getPointB()[1]] = forkColor;
		if(veinMap != null) {
			veinMap[vein.getPointA()[0]/VEIN_MAP_FACTOR]
					[vein.getPointA()[1]/VEIN_MAP_FACTOR] = true;
			veinMap[vein.getPointB()[0]/VEIN_MAP_FACTOR]
					[vein.getPointB()[1]/VEIN_MAP_FACTOR] = true;
		}
	}

	/**
	 * Helper function for determining whether a vein
	 * fork is valid for analysis or not.
	 * 
	 * @param fork
	 * The fork to inspect for validation.
	 * @param forkMarker
	 * The fork marker used as part of validation.
	 */
	private static boolean validForkAnalysis(VeinFork fork, int forkMarker) {
		return fork != null && fork.getMark() != forkMarker && fork.getVeins().length > 2;
	}
	
	/**
	 * Helper function for computing the curvature of a vein.
	 * 
	 * @param vein
	 * The vein to compute the curvature for.
	 */
	private static float computeCurvature(Vein vein) {
		return vein.getSize() /
			VectorTools.distance(
				vein.getConnectionA().getCoord(), vein.getConnectionB().getCoord());
	}
}
