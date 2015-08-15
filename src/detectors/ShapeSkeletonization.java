package detectors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import tools.math.Kernels;
import tools.structures.CoordinateList;

/**
 * A class for running the skeletonization algorithm
 * on foregrounds within an image.
 * 
 * @author Peter Bugaj
 */
public class ShapeSkeletonization {
	
	/**
	 * Run the skeletonization algorithm on foregrounds in the image.
	 *
	 * @param filteredImage
	 * The imag source matrix containing the foreground objects to
	 * be skeletonized.
	 * @param nonEyeImage
	 * The matrix marking parts of the image not belonging to the eye.
	 */
	public static void produceSkeleton(short [][][] filteredImage, boolean [][] nonEyeImage) {

		short [][] map = new short[filteredImage.length][filteredImage[0].length];		
		short boundaryCountIncrementor = 1;
		
		// Find the initial set of boundary points for the foreground
		Vector<CoordinateList> boundaries = new Vector<CoordinateList>();
		
		CoordinateList boundaryPoints = new CoordinateList();
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				if(map[i][j] != 0 || filteredImage[i][j][0] != 0) continue;
				findBoundary(filteredImage, map, i, j, boundaryPoints, boundaryCountIncrementor, nonEyeImage);
			}
		}	
		
		// Expand from the initial boundary found above towards
		// the center of each foreground shape.
		boundaryCountIncrementor++;
		boundaryIncrementor: while(true) {

			CoordinateList nextBoundaryPoints = new CoordinateList();
			boundaries.add(nextBoundaryPoints);
			Iterator<short[]> boundaryIterator = boundaryPoints.getCoords();

			while(boundaryIterator.hasNext()) {
				expandBoundary(
						filteredImage,
						map,
						boundaryIterator.next(),
						nextBoundaryPoints,
						boundaryCountIncrementor,
						nonEyeImage);
			}
			
			if(nextBoundaryPoints.getSize() == 0) break boundaryIncrementor;
			boundaryPoints = nextBoundaryPoints;
			boundaryCountIncrementor++;
		}
		
		// Label the boundaries created above for visual inspection.
		for(short i = 0; i < filteredImage.length; i++) {
			for(short j = 0; j < filteredImage[0].length; j++) {
				short val = (short) (map[i][j] - 1);
				if(nonEyeImage[i][j]) val = 0;

				filteredImage[i][j] = new short[]{
					(short) (Math.min(val*50, 250)),
					(short) (Math.min(val*25, 250)),
					(short) (Math.min(val*15, 255))
				};
			}
		}
		
		
		// Truncate the boundaries again to find the central
		// points making up the skeleton.
		for(int i = 0; i < boundaries.size(); i++) {
			CoordinateList nextBoundary = boundaries.get(i);
			Iterator<short[]> points = nextBoundary.getCoords();
			while(points.hasNext()) {
				short[]next_point = points.next();
				truncateBoundary(filteredImage, map, next_point);
			}
		}
		
		map =  null;
	}
	
	/**
	 * Helper function for finding the original boundary.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the foreground objects to
	 * be skeletonized.
	 * @param map
	 * Matrix keeping track of the boundaries created within the image.
	 * @param i
	 * The x coordinate to start marking the boundary from.
	 * @param j
	 * The y coordinate to start marking the boundary from.
	 * @param boundaryPoints
	 * The boundary points found so far for the new
	 * boundary that is being created.
	 * @param boundaryCountIncrementor
	 * The boundary counter incremented each time the boundary is expanded
	 * further and used for labelling the boundaries within the map.
	 * @param nonEyeImage
	 * The matrix marking parts of the image not belonging to the eye.
	 */
	private static void findBoundary(
			short [][][] filteredImage,
			short [][] map,
			short i, short j,
			CoordinateList boundaryPoints,
			short boundaryCountIncrementor,
			boolean [][] nonEyeImage) {

		LinkedList<short[]> stack = new LinkedList<short[]>();
		stack.push(new short[]{i, j});
		
		while(stack.size() > 0) {
			
			short [] next_coord = stack.removeLast();
			short nx = next_coord[0];
			short ny = next_coord[1];
			
			if (map[nx][ny] > 0) continue;
			if(nonEyeImage[nx][ny]) continue;
			map[nx][ny] = boundaryCountIncrementor;
					
			boolean boundary_hit = false;
			for(byte d = 0; d < Kernels.neighMap.length; d++) {
				byte m = Kernels.neighMap[d][0];
				byte n = Kernels.neighMap[d][1];

				if(nx-1+m < 0 || nx-1+m >= filteredImage.length) continue;
				if(ny-1+n < 0 || ny-1+n >= filteredImage[0].length) continue;

				if (map[nx-1+m][ny-1+n] > 0) continue;
				if(nonEyeImage[nx-1+m][ny-1+n]) continue;
				
				if (filteredImage[nx-1+m][ny-1+n][0] != 0) {
					boundary_hit = true;
					continue;
				}

				stack.push(new short []{(short) (nx-1+m), (short) (ny-1+n)});
			}
			
			if (boundary_hit) {
				boundaryPoints.addCoord(new short[]{next_coord[0], next_coord[1]});
			}
		}
	}

	/**
	 * Helper function for expanding the boundary given a start boundary
	 * point to expand from. Only expands the boundary from the
	 * neighbourhood of the starting point provided.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the foreground objects to
	 * be skeletonized.
	 * @param map:
	 * Matrix keeping track of the boundaries created within the image.
	 * @param startCoord
	 * The starting coordinate of the previous boundary to expand the
	 * new boundary from.
	 * @param boundaryPoints
	 * The boundary points found so far for the new
	 * boundary that is being created from the expansion.
	 * @param boundaryCountIncrementor
	 * The boundary counter incremented each time the boundary is expanded
	 * further and used for labelling the boundaries within the map.
	 * @param nonEyeImage
	 * The matrix marking parts of the image not belonging to the eye.
	 */
	private static void expandBoundary(
			short [][][] filteredImage,
			short [][] map,
			short [] startCoord,
			CoordinateList boundaryPoints,
			short boundaryCountIncrementor,
			boolean [][]nonEyeImage) {

		short nx = startCoord[0];
		short ny = startCoord[1];

		for(byte i = 0; i < Kernels.neighMap.length; i++) {
			byte m = Kernels.neighMap[i][0];
			byte n = Kernels.neighMap[i][1];

			if(nx-1+m < 0 || nx-1+m >= filteredImage.length) continue;
			if(ny-1+n < 0 || ny-1+n >= filteredImage[0].length) continue;

			if(nonEyeImage[nx-1+m][ny-1+n]) continue;
			if (map[nx-1+m][ny-1+n] != 0) {
				continue;
			}
			map[nx-1+m][ny-1+n] = boundaryCountIncrementor;

			if (filteredImage[nx-1+m][ny-1+n][0] != 0) {
				boundaryPoints.addCoord(new short []{(short) (nx-1+m), (short) (ny-1+n)});
			}
		}
	}

	/**
	 * Truncate the boundary as part of the skeletonization process.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the foreground objects to
	 * be skeletonized.
	 * @param map
	 * Matrix keeping track of the truncated boundaries within the image.
	 * @param startPoint
	 * Starting point to truncate the boundary from. From this point,
	 * the algorithm walks across the boundary and truncates it
	 * pixel by pixel.
	 */
	private static void truncateBoundary(
			short [][][] filtered_image,
			short [][] map,
			short [] startPoint) {
		
		int start_color = map[startPoint[0]][startPoint[1]];
		
		LinkedList<short[]> stack = new LinkedList<short[]>();
		stack.push(new short[]{startPoint[0], startPoint[1]});
		
		while(stack.size() > 0) {
			
			short [] next_coord = stack.removeLast();
			short nx = next_coord[0];
			short ny = next_coord[1];
			
			if(map[nx][ny] != start_color || map[nx][ny] == 0) {
				continue;
			}
			map[nx][ny] = 0;
			checkTruncation(filtered_image, nx, ny);

			for(byte d = 0; d < Kernels.neighourhoodSmall.length; d++) {
				byte m = Kernels.neighourhoodSmall[d][0];
				byte n = Kernels.neighourhoodSmall[d][1];

				if(nx-1+m < 0 || nx-1+m >= filtered_image.length) continue;
				if(ny-1+n < 0 || ny-1+n >= filtered_image[0].length) continue;

				stack.push(new short []{(short) (nx-1+m), (short) (ny-1+n)});
			}
		}
	}
	
	/**
	 * Check if the pixel at the boundary can be
	 * truncated further , and do so if possible.
	 * 
	 * @param filteredImage
	 * The image source matrix containing the foreground objects to
	 * be skeletonized.
	 * @param nx
	 * The x location of the boundary point to truncate.
	 * @param ny
	 * The y location of the boundary point to truncate.
	 */
	private static void checkTruncation(short[][][]filteredImage, short nx, short ny) {
		
		// Check if this boundary point can be truncated.
		short prev_val = -1;
		short first_valid_index = -1;
		
		short neg = 0;
		
		for(byte i = 0; i <= Kernels.neighMap.length; i++) {

			byte m;
			byte n;
			if(i == Kernels.neighMap.length) {
				m = Kernels.neighMap[first_valid_index][0];
				n = Kernels.neighMap[first_valid_index][1];				
			} else {
				m = Kernels.neighMap[i][0];
				n = Kernels.neighMap[i][1];
			}
			
			if(nx-1+m < 0 || nx-1+m >= filteredImage.length) continue;
			if(ny-1+n < 0 || ny-1+n >= filteredImage[0].length) continue;
			
			short val = filteredImage[nx-1+m][ny-1+n][0];
			if (prev_val == -1) {
				prev_val = val;
				first_valid_index = i;
			} else {
				if(val == 0) {
					if(val != prev_val) {
						neg++;
					}
				} else {
					val = 1;
				}
				
				prev_val = val;
			}
		}
		
		if(neg < 2) {
			
			// Expand a high intensity boundary inwards.
			for(byte i = 0; i <= Kernels.neighMap.length; i++) {

				byte m;
				byte n;
				if(i == Kernels.neighMap.length) {
					m = Kernels.neighMap[first_valid_index][0];
					n = Kernels.neighMap[first_valid_index][1];				
				} else {
					m = Kernels.neighMap[i][0];
					n = Kernels.neighMap[i][1];
				}
				if(nx-1+m < 0 || nx-1+m >= filteredImage.length) continue;
				if(ny-1+n < 0 || ny-1+n >= filteredImage[0].length) continue;
				
				if(filteredImage[nx-1+m][ny-1+n][0] != 0 && filteredImage[nx-1+m][ny-1+n][0] < filteredImage[nx][ny][0]) {
					filteredImage[nx-1+m][ny-1+n] = filteredImage[nx][ny];
				}
			}
			
			// Truncate the current pixels
			filteredImage[nx][ny] = new short[]{0,0,0};
		}
	}
}
