package tools.math;

/**
 * A set of custom PCA computations
 * 
 * @author Peter Bugaj
 */
public class PCATools {

    /**
     * Get the standard deviation for set A.
     */
    public static float getStdDev(float [] set_a) {
    	float [] new_a = getAdjustedSet(set_a);
    	for(int i = 0; i < new_a.length; i++) {
    		new_a[i] = new_a[i]*new_a[i];
    	}
    	return (float) Math.sqrt(getMean(new_a));
    }
	
    /**
     * Get the standard deviation for set A.
     */
    public static double getStdDev(short [] set_a) {
    	float [] new_a = getAdjustedSet(set_a);
    	for(int i = 0; i < new_a.length; i++) {
    		new_a[i] = new_a[i]*new_a[i];
    	}
    	return Math.sqrt(getMean(new_a));
    }

    /**
     * Get the mean value of the set.
     */
    public static float getMean(float [] set) {
        float sum = 0.0f;
        for(int i = 0; i < set.length; i++) {
            sum+=set[i];
        }
        if(set.length==0) {
            return 0;
        } else {
            return sum/(set.length+0.0f);
        }
    }
    
    /**
     * Subtract the set medium from the set itself.
     */
    private static float [] getAdjustedSet(float [] set) {
        float mean = getMean(set);

        float [] new_set = new float [set.length];
        for(int i =0; i < set.length; i++) {
            new_set[i] = set[i]-mean;
        }
        return new_set;
    }

    /**
     * Subtract the set medium from the set itself.
     */
    private static float [] getAdjustedSet(short [] set) {
        float mean = getMean(set);

        float [] new_set = new float [set.length];
        for(int i =0; i < set.length; i++) {
            new_set[i] = set[i]-mean;
        }
        return new_set;
    }
    
    /**
     * Get the mean value of the set.
     */
    private static float getMean(short [] set) {
        float sum = 0.0f;
        for(int i = 0; i < set.length; i++) {
            sum+=set[i];
        }
        if(set.length==0) {
            return 0;
        } else {
            return sum/(set.length+0.0f);
        }
    }
}