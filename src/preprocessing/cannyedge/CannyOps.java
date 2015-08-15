package preprocessing.cannyedge;

/**
 * Class for running the Canny Edge Detector.
 * 
 * @author Peter Bugaj
 */
public class CannyOps {

	/**
	 * The R pixel.
	 */
	public static final byte R = 0;

	/**
	 * The G pixel.
	 */
	public static final byte G = 1;
	
	/**
	 * The B pixel.
	 */
	public static final byte B = 2;

	/**
	 * Return the gray scale of an input image
	 * @param imageMatrix
	 * The input image.
	 * @return
	 * The gray scale version of the input image.
	 */
	public static short [][][] returnGreyScale(short [][][] imageMatrix) {

		/**--------------------------------------------------------------**/
		int height = imageMatrix[0].length;
		int width  =  imageMatrix.length;
		short [][][] new_image_matrix = new short[width][height][3];
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
				
				/**===================================================**/
				int r = (int) imageMatrix[i][j][0];
				int g = (int) imageMatrix[i][j][1];
				int b = (int) imageMatrix[i][j][2];
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				short brightness = (short)
						((int)(r*0.3f)+(int)(g*0.59f)+(int)(b*0.11f));
				new_image_matrix[i][j] = new short[]{
						brightness,
						brightness,
						brightness};
				/**===================================================**/
			}
		}
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		return new_image_matrix;
		/**--------------------------------------------------------------**/
	}

	/**
	 * Convolves a 2D filter across an image for a specific color index.
	 *
	 * @param imageMatrix
	 * The image source matrix to convolve the filter over.
	 * @param filter
	 * The filter to convovle.
	 * @param colorIndex
	 * The specific color index to convolve for.
	 * @param nonEyeImage
	 * The image pixels marked that are not belonging to the eye.
	 * @param fraction
	 * How heavily to apply the convolution effect to the resulting image.
	 * A small fraction will yield a smaller effect and vise versa.
	 * @return
	 * The resulting image with the filter convolved across it.
	 */
	public static short[][][] convolve(
			short [][][]imageMatrix,
			float [][] filter,
			byte colorIndex,
			boolean [][] nonEyeImage,
			double fraction) {
		
		int height = imageMatrix[0].length;
		int width  =  imageMatrix.length;
		
		float summation = 0;
		float summation_counter = 0;
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
				if(nonEyeImage[i][j]) continue;
				summation += imageMatrix[i][j][colorIndex];
				summation_counter++;
			}
		}
		
		summation /= Math.max(summation_counter, 1) * fraction;
		
		short [][][] new_image_matrix = new short[width][height][3];

		int half_filter = ((filter.length)-1)/2;

		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
					
				float fac_a = 0;
				float fac_b = 1;

				float min_v = summation;
				fac_a = (min_v - imageMatrix[i][j][colorIndex]) / min_v;
				fac_b = imageMatrix[i][j][colorIndex] / min_v;
				
				/**===================================================**/
				float sum_c = 0;
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				for(int m = 0; m < filter.length; m++) {
					for(int n = 0; n < filter.length; n++) {
						
						int conv_neighb_x = (i-half_filter)+m;
						if(conv_neighb_x <0) conv_neighb_x = 0;
						if(conv_neighb_x >width-1) conv_neighb_x = width-1;
						
						int conv_neighb_y = (j-half_filter)+n;
						if(conv_neighb_y <0) conv_neighb_y = 0;
						if(conv_neighb_y >height-1) conv_neighb_y = height-1;
						
						sum_c += filter[m][n]*imageMatrix
							[conv_neighb_x]
							[conv_neighb_y][colorIndex];
					}					
				}
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				new_image_matrix[i][j] = new short[]{
						(short)(fac_b*sum_c + fac_a*imageMatrix[i][j][colorIndex]),
						(short)(fac_b*sum_c + fac_a*imageMatrix[i][j][colorIndex]),
						(short)(fac_b*sum_c + fac_a*imageMatrix[i][j][colorIndex]),};
				/**===================================================**/
			}
		}
		return new_image_matrix;
	}
	
	/**
	 * Convolves a 2D filter across an image for a specific color index.
	 *
	 * @param imageMatrix
	 * The image source matrix to convolve the filter over.
	 * @param filter
	 * The filter to convovle.
	 * @param colorIndex
	 * The specific color index to convolve for.
	 * @return
	 * The resulting image with the filter convolved across it.
	 */
	public static short[][][] convolve(
			short [][][]image_matrix,
			float [][] filter,
			byte color_index) {
		
		
		/**--------------------------------------------------------------**/
		int height = image_matrix[0].length;
		int width  =  image_matrix.length;
		short [][][] new_image_matrix = new short[width][height][3];
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		int half_filter = ((filter.length)-1)/2;
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {

				/**===================================================**/
				float sum_c = 0;
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				for(int m = 0; m < filter.length; m++) {
					for(int n = 0; n < filter.length; n++) {
						
						int conv_neighb_x = (i-half_filter)+m;
						if(conv_neighb_x <0) conv_neighb_x = 0;
						if(conv_neighb_x >width-1) conv_neighb_x = width-1;
						
						int conv_neighb_y = (j-half_filter)+n;
						if(conv_neighb_y <0) conv_neighb_y = 0;
						if(conv_neighb_y >height-1) conv_neighb_y = height-1;
						
						sum_c += filter[m][n]*image_matrix
							[conv_neighb_x]
							[conv_neighb_y][color_index];
					}					
				}
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				new_image_matrix[i][j] = new short[]{
						(short)sum_c,
						(short)sum_c,
						(short)sum_c,};
				/**===================================================**/
			}
		}
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		return new_image_matrix;
		/**--------------------------------------------------------------**/
	}
		
	/**
	 * Return the image gradient, given a convolved image with a horizontal
	 * image gradient, and a convolved image with a vertical image gradient.
	 *
	 * @param grad_x
	 * The convolved image with a horizontal image gradient.
	 * @param grad_y
	 * The convolved image with a vertical image gradient.
	 * @param colour_gradient
	 * Whether or not to returned a colour labelled gradient.
	 * @return
	 * The image gradient.
	 */
	public static short[][][] returnEdges(
			short[][][]grad_x,
			short[][][]grad_y,
			boolean colour_gradient) {
		
		/**--------------------------------------------------------------**/
		int height = grad_x[0].length;
		int width  =  grad_x.length;
		short [][][] edges = new short[width][height][3];
		/**--------------------------------------------------------------**/
		/**==============================================================**/
		/**--------------------------------------------------------------**/
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
			
				float grad = (float) Math.sqrt(
						(grad_x[i][j][0]*grad_x[i][j][0]) + 
						(grad_y[i][j][0]*grad_y[i][j][0])
						);
				float angle = 0;
				if(grad > 0) {
					angle = 	(float) (Math.atan2(grad_x[i][j][0]/grad,
							grad_y[i][j][0]/grad)*180.0f/Math.PI);
				}
				if(angle < 0) {
					angle = 360+angle;
				}
				if(angle == 360) {
					angle = 0;
				}
				//System.out.println(angle);
				//else grad = 255;
				
				/**===================================================**/
				/**|||||||||||||||||||||||||||||||||||||||||||||||||||**/
				/**===================================================**/
				if(colour_gradient) {
					edges[i][j] = new short[]{
							(short)grad,
							(short) ((angle/360f)*255f),
							(short) (255f - ((angle/360f)*255f))};
				} else {
					edges[i][j] = new short[]{ (short) grad, (short) grad, (short) grad };
				}
				/**===================================================**/
			}
		}
		return edges;
		/**--------------------------------------------------------------**/
	}
}
