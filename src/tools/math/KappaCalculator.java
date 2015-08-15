package tools.math;

/**
 * Tool for computing the Kappa value.
 */
public class KappaCalculator {
	
	public static double findKappa(double [][]kappa){
		int kappa_dim = kappa.length;
		
        //we have the counts stored in an nxn matrix
        //convert "observed matrix" from counts to frequencies
        double sum = sumOfAllElements(kappa);
        for (int k = 0; k < kappa_dim; k++) {
            for (int k2 = 0; k2 < kappa_dim; k2++) {
                kappa[k][k2] = kappa[k][k2]/sum;
            }
        }
 
        //echo "observed matrix:".json_encode(matrix)."</p>";
        //now, let's find the "chance matrix"
        double chance[][] = new double[kappa_dim][kappa_dim];
        for (int k = 0; k < kappa_dim; k++) {
            for (int k2 = 0; k2 < kappa_dim; k2++) {
                chance[k][k2] =
                    array_sum(getRow(kappa, k))*array_sum(getColumn(kappa, k2, kappa_dim));
            }
        }
        //echo "chance matrix:".json_encode(chance)."</p>";
        //we will use a weight matrix
        //there are two main ways to calculate the weight matrix;
        //linear or quadratic
        //we will use the linear one
 
        double weight[][] = new double[kappa_dim][kappa_dim];
        double rowCount = kappa.length;
        //echo "each dimension:".rowCount;
        //rowCount = columnCount, because the observation matrix is an nxn matrix.
        for(int i=0;i<rowCount;i++){
            for(int j=0;j<rowCount;j++){
                weight[i][j]=1-(Math.abs(i-j)/(rowCount-1));
                //this would be the quadratic one:
                //weight[i][j]=1-pow((abs(i-j)/rowCount),2);
            }
        }
        //echo "weight matrix:".json_encode(weight)."</p>";
        //now, 1)multiply each element in the observed matrix
        //by corresponding weight element and sum it all
        //2)do the same thing with chance matrix
        double sumOfObserved = 0;
        double sumOfChance = 0;
        for(int i=0;i<rowCount;i++){
            for(int j=0;j<rowCount;j++){
                sumOfObserved += kappa[i][j]*weight[i][j];
                sumOfChance += chance[i][j]*weight[i][j];
            }
        }
        //the formula for kappa is this:
        double kappaValue = (sumOfObserved-sumOfChance)/(1-sumOfChance);
        return kappaValue;
    }

    private static double array_sum(double arr[]){
        double sum=0;
        for (int i=0; i < arr.length; i++)
            sum+=arr[i];
        return sum;
    }
    
    private static double sumOfAllElements(double[][] matrix){
        double sum = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                sum+=matrix[i][j];
            }
        }
        return sum;
    }
    
    private static double[] getRow(double matrix[][],int row){
        return matrix[row];
    }
    
    private static double[] getColumn(double matrix[][],int column, int kappa_dim){
        double col[]=new double[kappa_dim];
        for (int i = 0; i < matrix.length; i++) {
                col[i]=matrix[i][column];
        }
        return col;
    }
}
