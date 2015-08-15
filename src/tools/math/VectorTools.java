package tools.math;

/**
 * A set of custom vector computations.
 * 
 * @author Peter Bugaj
 */
public class VectorTools {
		
	/**
	 * Get the matrix transpose.
	 */
	public static short [][] getTranspose(short [][] m) {

		short [][] transpose = new short[m[0].length][m.length];
		
		for(int i = 0; i < m.length; i++) {
			for(int j = 0; j < m[i].length; j++) {
				transpose[j][i] = m [i][j];
			}
		}
		
		return transpose;
	}

	/**
	 * Calculate distance between two vectors.
	 */
	public static double distance(double [] v1, double []v2, double [] w) {
		return mag( mult( sub(v1, v2), w) );
	}
	
	/**
	 * Calculate distance between two vectors.
	 */
	public static double distance(double [] v1, double []v2) {
		return mag(sub(v1, v2));
	}
	
	/**
	 * Calculate distance between two vectors.
	 */
	public static float distance(float [] v1, float []v2) {
		return mag(sub(v1, v2));
	}
	
	/**
	 * Calculate distance between two vectors.
	 */
	public static float distance(short [] v1, short []v2) {
		return mag(sub(v1, v2));
	}
	
	/**
	 * Calculate distance between two vectors.
	 */
	public static float distance(int [] v1, int []v2) {
		return mag(sub(v1, v2));
	}

	/**
	 * Calculate magnitude of the vector.
	 */
	private static double mag(double [] v1) {
		double sum = 0.0f;
		for(int i =0; i < v1.length; i++) {
			sum = sum+Math.pow(v1[i], 2);
		}
		return Math.sqrt(sum);
	}
	
	/**
	 * Calculate magnitude of the vector.
	 */
	private static float mag(short [] v1) {
		double sum = 0.0f;
		for(int i =0; i < v1.length; i++) {
			sum = sum+Math.pow(v1[i], 2);
		}
		return (float) Math.sqrt(sum);
	}
	
	/**
	 * Calculate magnitude of the vector.
	 */
	private static float mag(int [] v1) {
		double sum = 0.0f;
		for(int i =0; i < v1.length; i++) {
			sum = sum+Math.pow(v1[i], 2);
		}
		return (float) Math.sqrt(sum);
	}

	/**
	 * Calculate magnitude of the vector.
	 */
	private static float mag(float [] v1) {
		double sum = 0.0f;
		for(int i =0; i < v1.length; i++) {
			sum = sum+Math.pow(v1[i], 2);
		}
		return (float) Math.sqrt(sum);
	}

	/**
	 * Subtract two vectors: v1 - v2
	 */
	private static double [] sub(double [] v1, double []v2) {
		double [] ans = new double[v1.length];
		for(int i = 0; i < ans.length; i++) {
			ans[i] = v1[i]-v2[i];
		}
		return ans;
	}
	
	/**
	 * Subtract two vectors: v1 - v2
	 */
	private static float [] sub(float [] v1, float []v2) {
		float [] ans = new float[v1.length];
		for(int i = 0; i < ans.length; i++) {
			ans[i] = v1[i]-v2[i];
		}
		return ans;
	}
	
	/**
	 * Subtract two vectors: v1 - v2
	 */
	private static short [] sub(short [] v1, short []v2) {
		short [] ans = new short[v1.length];
		for(int i = 0; i < ans.length; i++) {
			ans[i] = (short) (v1[i]-v2[i]);
		}
		return ans;
	}
	
	/**
	 * Subtract two vectors: v1 - v2
	 */
	private static int [] sub(int [] v1, int []v2) {
		int [] ans = new int[v1.length];
		for(int i = 0; i < ans.length; i++) {
			ans[i] = v1[i]-v2[i];
		}
		return ans;
	}

	/**
	 * Dot product.
	 */
	private static double[] mult(double[]a, double[]b) {
		double [] ans = new double[a.length];
		for(int i = 0; i < ans.length; i++) {
			ans[i] = a[i]*b[i];
		}
		return ans;
	}
}