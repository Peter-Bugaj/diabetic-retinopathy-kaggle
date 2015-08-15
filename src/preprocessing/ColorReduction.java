package preprocessing;

/**
 * Class for helping reduce the color variation within an image.
 *
 * @author Peter Bugaj
 */
public class ColorReduction {

	/**
	 * The number of colours to reduce an image to.
	 */
	public static final int NUM_COLORS = 32;
	
	/**
	 * Custom function for reducing the colours within an image.
	 * 
	 * @param image
	 * The image source matrix for which to reduce the colours for.
	 * @param inverse
	 * Whether to reverse the colours during reduction.
	 */
	public static void reduceColourMonotone(short [][][] image, boolean inverse) {
		
		short min_color = 1000;
		short max_color = 0;
		
		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[0].length; j++) {
				short val = combineCones(image[i][j]);
				if(min_color > val) {
					min_color = val;
				}
				if(max_color < val) {
					max_color = val;
				}
			}			
		}

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[0].length; j++) {
				float val = combineCones(image[i][j]);
				short index = (short) (NUM_COLORS * (val - min_color)
						/ (max_color - min_color));

				image[i][j] = createRGBLabel(index, inverse);
			}
		}
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
		return (short)(cones[0]*0 + cones[1]*1 + cones[2]*0);
	}
	
	/**
	 * Create an RGB label given an index.
	 *
	 * @param i
	 * The index to create the label for.
	 * @param inverse
	 * Whether or not to revert the label.
	 * @return
	 * The returned RGB label.
	 */
	private static short [] createRGBLabel(short i, boolean inverse) {
		if (inverse) {
			i = (short) (NUM_COLORS - i);
		}
		short r = (short) (i+1);
		
		// 200 and 240 are arbitrary numbers just for labelling.
		short g = (short) ((i+1.0) / NUM_COLORS * 200);
		short b = (short) ((i+1.0) / NUM_COLORS * 240);
		
		return new short[]{r, g, b};
	}	
}
