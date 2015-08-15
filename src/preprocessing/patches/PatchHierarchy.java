package preprocessing.patches;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import preprocessing.ColorReduction;

import retinopathy.structures.Patch;

import tools.math.Kernels;
import tools.structures.CoordinateList;

/**
 * Class for running the patch hierarchy algorithm.
 *
 * @author Peter Bugaj
 */
public class PatchHierarchy {

	/**
	 * Takes in an image with RGB values [num_colors + 1, x, y], and
	 * outputs an image of RGB values [f1, f2, 0].
	 * 
	 * @param filteredImage
	 * The image source matrix to run the patch hierarchy algorithm on.
	 * @param flipped
	 * Whether or not the algorithm is to run on an image with
	 * the colour values flipped.
	 */
	public static Hashtable<String, Patch> constructPatches(short [][][] filteredImage, boolean flipped) {

		// Locate the different layers by pixel coordinates
		boolean [][] map = new boolean[filteredImage.length][filteredImage[0].length];
		CoordinateList[] locations_per_layer = new CoordinateList[(int) (ColorReduction.NUM_COLORS + 2)];
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				if(map[i][j]) continue;
				markRegion(filteredImage, map, i, j, locations_per_layer);
			}
		}

		// Store the boundary of each layer, starting
		// from the top layer and going down.
		Hashtable<String, Patch> id_to_patch = new Hashtable<String, Patch>();
		int[][] patch_marker = new int[filteredImage.length][filteredImage[0].length];

		for(short layer = 0; layer < locations_per_layer.length; layer++) {
			if (locations_per_layer[layer] == null) {
				continue;
			}

			Iterator<short[]> coords = locations_per_layer[layer].getCoords();
			while(coords.hasNext()) {
				short [] next_coord = coords.next();
				if(patch_marker[next_coord[0]][next_coord[1]] != 0) {
					continue;
				}

				findPatchAndStoreBoundary(filteredImage, id_to_patch, patch_marker, next_coord, layer, flipped);
			}
		}

		// Print the important patches onto the image for visualization
		drawPatches(filteredImage, patch_marker, id_to_patch, ColorReduction.NUM_COLORS);
		
		return id_to_patch;
	}
	
	/**
	 * Mark a certain region within an image that has uniform colour.
	 * Then mark that entire region of uniform colour to prevent any
	 * pixels belonging to it from being marked in that region again.
	 * Run this using the connected component algorithm.
	 * 
	 * @param filteredImage
	 * The image source matrix for which to mark the uniform region in.
	 * @param map
	 * The map keeping track of the different uniform regions so far,
	 * as well as the current region being marked by the connected
	 * component algorithm.
	 * @param x
	 * The x location within the image to start running the connected
	 * component algorithm at to mark the uniform colour region.
	 * @param y
	 * The y location within the image to start running the connected
	 * component algorithm at to mark the uniform colour region.
	 * @param locationsPerLayer
	 * Data structure for storing the various marked points within the
	 * image, grouped by the colour intensity values present within
	 * the image.
	 */
	private static void markRegion(
			short [][][] filteredImage,
			boolean [][] map,
			short x, short y,
			CoordinateList[] locationsPerLayer) {

		/**--------------------------------------------------------------**/
		short intensity = filteredImage[x][y][0];
		if(locationsPerLayer[intensity] == null) {
			locationsPerLayer[intensity] = new CoordinateList();
		}
		locationsPerLayer[intensity].addCoord(new short[]{x, y});
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/


		/**--------------------------------------------------------------**/
		LinkedList<int[]> stack = new LinkedList<int[]>();
		stack.push(new int[]{x,y});
		while(stack.size() > 0) {

			/**===================================================**/
			int [] next_pair = stack.removeLast();
			/**===================================================**/
			/**Get the next point to expand from**/
			int nx = next_pair[0];
			int ny = next_pair[1];
			/**===================================================**/
			if(map[nx][ny]) continue;
			map[nx][ny] = true;
			/**===================================================**/

			/**===================================================**/
			for(short i = 0; i < Kernels.neighMap.length; i++) {
				byte m = Kernels.neighMap[i][0];
				byte n = Kernels.neighMap[i][1];
			
				if(nx-1+m < 0 || ny-1+n < 0 || nx-1+m >= filteredImage.length || ny-1+n >= filteredImage[0].length) continue;

				if(map[nx-1+m][ny-1+n]) continue;
				if(filteredImage[nx-1+m][ny-1+n][0] != intensity) continue;

				stack.push(new int []{nx-1+m, ny-1+n});
			}
			/**===================================================**/
		}
	}

	/**
	 * Find a patch within the image and store its boundary points.
     *
	 * @param filteredImage
	 * The filtered image containing the pixel information needed
	 * for constructing the patch.
	 * @param idToPatch
	 * The data structure keeping track of the patches found and
	 * constructed so far, storing them using their IDs.
	 * @param patchMarker
	 * The map storing where all the patches are located within the
	 * image, marking them by their IDs.
	 * @param startCoord
	 * The startig coordinate to use by the connected component
	 * algoritm to iterate through the image of the uniform
	 * colour region and to mark the patch.
	 * @param newLayer
	 * The pixel value layer of the new patch being constructed.
	 * @param flipped
	 * Whether or not the patch is being created for an image
	 * with colours retreated as reversed.
	 */
	private static void findPatchAndStoreBoundary(
			short [][][] filteredImage,
			Hashtable<String, Patch> idToPatch,
			int[][] patchMarker,
			short [] startCoord,
			int newLayer,
			boolean flipped) {

		int max_area = 100000 * 10;
		boolean large_patch_detected = false;

		// Create a new patch
		Patch new_patch = new Patch();
		int new_id = new_patch.getId();
		new_patch.setLevel(newLayer);
		idToPatch.put(new_id + "", new_patch);

		// Start from the original coordinate, find all the connected
		// pixels that exist on the same layer and store a boundary.
		LinkedList<short[]> stack = new LinkedList<short[]>();
		stack.push(startCoord);

		// ============================================================== //
		// ======================== MAIN LOOP START ===================== //
		while(stack.size() > 0 && !large_patch_detected) { // =========== //
			// ============================================================== //

			short [] next_coord = stack.removeLast();
			short nx = next_coord[0];
			short ny = next_coord[1];

			// Coordinate already processed. Move on.
			if(patchMarker[nx][ny] == new_id) continue;

			// A higher layer. Move on.
			if(filteredImage[nx][ny][0] > newLayer) continue;

			// The boundary of a lower layer not yet marked by new_id.
			// Search around its boundary for point of equal intensity
			// to expend the new layer. Finally mark this coordinate
			// with the new_id to avoid paying attention to this lower
			// layer again. Also mark other boundary points of this
			// lower layer with the new_id for the same reason.
			// ==================================================== //
			// =============== BOUNDARY TRANSFER START ============ //
			if(filteredImage[nx][ny][0] < newLayer) { // ======== //
				// ==================================================== //

				int old_id = patchMarker[nx][ny];
				Patch old_patch = idToPatch.get(old_id + "");

				// Patches of lower intensity are only
				// removed in the case when they grew too large.
				if(old_patch == null) {
					large_patch_detected = true;
					continue;
				}

				new_patch.incrementArea(old_patch.getArea());			
				new_patch.incrementStackArea(
					(newLayer - filteredImage[nx][ny][0]) * old_patch.getStackArea());
				
				new_patch.incrementLevelSum(old_patch.getLevelSum());
				new_patch.incrementSumX(old_patch.getSumX());
				new_patch.incrementSumY(old_patch.getSumY());

				old_patch.setParentPatch(new_patch);
				
				Iterator<short[]> old_boundary = old_patch.getBoundary().iterator();
				while(old_boundary.hasNext()) {
					short [] old_next = old_boundary.next();
					short old_x = old_next[0];
					short old_y = old_next[1];
					patchMarker[old_x][old_y] = new_id;

					boolean boundary_hit = false;
					for(short i = 0; i < Kernels.neighMap.length; i++) {
						byte m = Kernels.neighMap[i][0];
						byte n = Kernels.neighMap[i][1];

						if(old_x-1+m < 0 || old_x-1+m >= filteredImage.length) continue;
						if(old_y-1+n < 0 || old_y-1+n >= filteredImage[0].length) continue;

						if(patchMarker[old_x-1+m][old_y-1+n] == new_id) {
							continue;
						}

						if (filteredImage[old_x-1+m][old_y-1+n][0] > newLayer) {
							boundary_hit = true;
							continue;
						}
						if (filteredImage[old_x-1+m][old_y-1+n][0] < newLayer) {
							continue;
						};

						stack.push(new short []{(short) (old_x-1+m), (short) (old_y-1+n)});
					}

					if (boundary_hit) {
						new_patch.getBoundary().add(new short[]{old_next[0], old_next[1]});
					}
				}

				//old_patch.boundary.clear();
				//old_patch.boundary = null;
				continue;

				// ==================================================== //				
			} // =============== BOUNDARY TRANSFER END ============ //
			// ==================================================== //

			patchMarker[nx][ny] = new_id;
			new_patch.incrementArea(1);
			new_patch.incrementLevelSum(newLayer);
			new_patch.incrementSumX(nx);
			new_patch.incrementSumY(ny);
			if(new_patch.getArea() > max_area) {
				large_patch_detected = true;
				continue;
			}

			// Extend search region
			boolean boundary_hit = false;
			for(short i = 0; i < Kernels.neighMap.length; i++) {
				byte m = Kernels.neighMap[i][0];
				byte n = Kernels.neighMap[i][1];

				if(nx-1+m < 0 || nx-1+m >= filteredImage.length) continue;
				if(ny-1+n < 0 || ny-1+n >= filteredImage[0].length) continue;

				if (patchMarker[nx-1+m][ny-1+n] == new_id) continue;

				if (filteredImage[nx-1+m][ny-1+n][0] > newLayer) {
					boundary_hit = true;
					continue;
				}

				stack.push(new short []{(short) (nx-1+m), (short) (ny-1+n)});
			}

			if (boundary_hit) {
				new_patch.getBoundary().add(new short[]{next_coord[0], next_coord[1]});
			}

			// ============================================================== //
		} // ======================= MAIN LOOP END ====================== //
		// ============================================================== //
		new_patch.incrementStackArea(new_patch.getArea());
		new_patch.setCurvature(
			Patch.getCurvature_T5(
				new_patch.getBoundary(), new_patch.getSumX(), new_patch.getSumY(), new_patch.getArea())
		);

		if(new_patch.getArea() > max_area || large_patch_detected) {
			idToPatch.remove(new_patch.getId() + "");
			return;
		}
	}

	/**
	 * Helper function for drawing the patches within the image
	 * for visualization.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the patches to be
	 * drawn the better emphasis.
	 * @param patchMarker
	 * The map storing where all the patches are located within the
	 * image, marking them by their IDs.
	 * @param idToPatch
	 * The data structure keeping track of the patches found and
	 * constructed so far, storing them using their IDs.
	 * @param numColours
	 * The total number of different colours present in the image matrix.
	 * Also equivalent to the number of layers the patch hierarchy is
	 * made up from.
	 */
	private static void drawPatches(
			short[][][]filteredImage,
			int[][] patchMarker,
			Hashtable<String, Patch> idToPatch,
			float numColours) {
		
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				if(patchMarker[i][j] == 0 || idToPatch.get(patchMarker[i][j] + "") == null) {
					filteredImage[i][j] = new short[]{0,0,0};
				} else {
					filteredImage[i][j] = new short[]{45, 30, 15};
				}
			}
		}
		
		// Draw the patches
		Enumeration<Patch> patches = idToPatch.elements();
		while(patches.hasMoreElements()) {
			Patch next_patch = patches.nextElement();

			float c = numColours;
			float ints = (c - Math.min(next_patch.getLevel(), c))/c;
			short[] color = null;
			
			float fac = next_patch.getStackArea() / (next_patch.getArea() +0.0f);
			
			
			float fac_ints = fac * ints;		
			if(next_patch.getStackArea() > 10) {
				color = new short[]{
						(short) (Math.min(fac_ints*150, 255)), 
						(short) (Math.min(fac_ints*100, 255)),
						(short) (Math.min(fac_ints*50, 255))};
			}
			else {
				continue;
			}

			/**Draw the boundary.**/
			drawBoundary(filteredImage, next_patch.getBoundary().iterator(), fac, color);
		}
	}
	
	/**
	 * Helper function for drawing the boundary of a patch.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the patches to be
	 * drawn the better emphasis.
	 * @param boundary
	 * The boundary of the patch to draw.
	 * @param strength
	 * The strength of the patch used for determing how thick
	 * the boundary should be drawn as.
	 * @param color
	 * The colour to use for drawing the boundary points.
	 */
	private static void drawBoundary(
			short[][][]filteredImage,
			Iterator<short[]> boundary,
			float strength,
			short[]color) {

		int b_m = 3;
		int b_h = 1;
		if(strength > 1.2) {
			b_m = 5; b_h = 2;
		}
		if (strength > 1.4) {
			b_m = 7; b_h = 3;
		}
		/**if (fac > 1.6) {
			b_m = 9; b_h = 4;
		}**/
		while(boundary.hasNext()) {
			short[]next_point = boundary.next();
			short cx = next_point[0];
			short cy = next_point[1];

			for(int m = 0; m < b_m; m++) {
				for(int n = 0; n < b_m; n++) {

					if(n==b_h && m==b_h) continue;
					if(cx-b_h+m < 0 || cy-b_h+n < 0 || cx-b_h+m >= filteredImage.length
						|| cy-b_h+n >= filteredImage[0].length) {
						continue;
					}

					if(cx-b_h+m < 0 || cx-b_h+m >= filteredImage.length) continue;
					if(cy-b_h+n < 0 || cy-b_h+n >= filteredImage[0].length) continue;
					filteredImage[cx-b_h+m][cy-b_h+n] = color;
				}
			}
		}
	}
}
