package tools.math;

/**
 * Tool for generating Gaussian matrixes.
 * 
 * @author Peter Bugaj
 */
public class GaussianGen {

	/**
	 * Compute a Gaussian matrix for a given size.
	 */
	public static void main(String[] args) {
		float gaussian_size = 11;
		float [][] gauss = new float [(int) gaussian_size][(int) gaussian_size];
		
		double gauss_sum = 0;
		for(int i = 0; i < gaussian_size; i++) {
			float x = ( (i / (gaussian_size - 1.0f) ) * 6.0f ) - 3f;

			for(int j = 0; j < gaussian_size; j++) {
				float y = ( (j / (gaussian_size - 1.0f) ) * 6.0f ) - 3f;

				gauss[i][j] = G(x, y);
				gauss_sum += gauss[i][j];
			}
		}
		
		for(int i = 0; i < gauss.length; i++) {
			System.out.print("new float[]{");
			for(int j = 0; j < gauss[0].length; j++) {
				gauss[i][j] /= gauss_sum;
				System.out.print(gauss[i][j] + "f,\t");
			}
			System.out.print("},");
			System.out.print("\n");
		}
	}

	/**
	 * Compute the 2D Gaussian function.
	 * 
	 * @param x
	 * The x value.
	 * @param y
	 * The y value.
	 */
	public static float G(float x, float y) {
		return (float) (0.5 * Math.PI * Math.pow(Math.E, -0.5 * (x*x + y*y)));
	}
}
