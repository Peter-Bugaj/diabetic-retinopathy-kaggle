package trainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import tools.math.KappaCalculator;
import tools.math.VectorTools;

/**
 * An experimental class used for training a meta-classifier
 * consisting of a network and KNN classifier.
 * 
 * @author Peter Bugaj
 */
public class ClassTrainerNet {

	/**
	 * The maximum number of features to read in per class.
	 */
	public static final int MAX_FEATURES_PER_CLASS = 708;
	
	/**
	 * Main entry function.
	 * 
	 * @param args
	 * [0] = Directory containing the training feature data.
	 */
	public static void main(String[] args) {
		
		// Read the input parameters.
		String inputDir = null;
		if(args.length > 0) {
			inputDir = args[0];
		} else {
			inputDir = "./Ratings";
		}

		// Read in the feature value pairs from the input directory specified.
		Vector<FeatureValuePair> featurePairs = readInFeatures(inputDir);

		
		// Shuffle the input values and train the
		// classifier on the subset of those values.
		FeatureValuePair[]values_array = shuffleFeatureValues(featurePairs);
		
		// Group the features by class
		double best_kappa = 0;

		int iterations = 40;
		int non_increasing_iterations = 0;
		int dim = values_array[0].getFeatureData().length;
		
		double[]weights = null;
		double[]best_weights = copyArray(weights);

		while(true) {
						
			double computed_kappa = trainWeightedKnn(values_array, weights);	
			System.out.println("Finished kNN with value: " + computed_kappa);
			if(computed_kappa > best_kappa) {

				non_increasing_iterations = 0;
				
				// Print the files used for training to log if they are
				// the best files so far for training the classifier.
				Vector<Vector<FeatureValuePair>> train_data_set_per_class =
					getGroupedFeatures(values_array, true);
				writeFeaturesToFile(train_data_set_per_class, "train_best");
				best_kappa = computed_kappa;
				
				best_weights = copyArray(weights);
				
				System.out.println("New best classifier found with Kappa value: " + best_kappa);
				System.out.println("Iteration: " + iterations);
				System.out.print("new double[]{");
				for(int i = 0; i < best_weights.length; i++) {
					System.out.print(best_weights[i] + (i != best_weights.length-1 ? "," : ""));
				}
				System.out.print("};\n");
			}

			non_increasing_iterations++;
			if(non_increasing_iterations > 25) {
				iterations -= 25;
				iterations = Math.max(0, iterations);
				non_increasing_iterations = 0;
			}
			iterations++;
			weights = getWeights(dim, (int)(iterations/30), best_weights);
		}
	}

	/**
	 * Run kNN classification with weights.
	 *
	 * @param valuesArray
	 * The features to evaluate the weighted KNN classifier on.
	 * @param weights
	 * The weights used on the KNN classifier when computing
	 * distance functions.
	 * @return
	 * The output Kappa value of the data predicted by the weighted
	 * KNN Classifier.
	 */
	private static double trainWeightedKnn(FeatureValuePair[]valuesArray, double[]weights) {
		
		Vector<Vector<FeatureValuePair>> train_data_set_per_class = getGroupedFeatures(valuesArray, true);
		Vector<Vector<FeatureValuePair>> test_data_set_per_class = getGroupedFeatures(valuesArray, true);
				
		double [][] kappa_o_values = new double[5][5];
		for (int i = 0; i < test_data_set_per_class.size(); i++) {
			Vector<FeatureValuePair> data_for_class = test_data_set_per_class.get(i);
			for (int j = 0; j < data_for_class.size(); j++) {
				int actual_label = data_for_class.get(j).getValue();
				int predicted_label = getKnnValue(train_data_set_per_class, data_for_class.get(j), weights);
				kappa_o_values[actual_label][predicted_label]++;
			}			
			
			System.out.println(i);
		}

		return KappaCalculator.findKappa(kappa_o_values);
	}
	
	/**
	 * Get the distances between clusters and a feature vector and
	 * output the best matching cluster class value from the average
	 * of distances of the 3 closest training points (k = 3).
	 * 
	 * @param clusteredDataPerClass
	 * The clustered training data.
	 * @param feature
	 * The feature vector to classify
	 */
	private static int getKnnValue(
			Vector<Vector<FeatureValuePair>> clusteredDataPerClass,
			FeatureValuePair feature,
			double[]weights) {
	
		float k = 3;
		
		int num_classes = clusteredDataPerClass.size();
		int class_size = clusteredDataPerClass.get(0).size();
		
		// Compute the distqnces.
		double[][]distances = new double[num_classes * class_size][2];
		int dist_count = 0;
		for (int i = 0; i < clusteredDataPerClass.size(); i++) {

			Vector<FeatureValuePair> data_for_class = clusteredDataPerClass.get(i);
			for (int j = 0; j < data_for_class.size(); j++) {
				
				FeatureValuePair neighbour = data_for_class.get(j);
				double neighbour_dist = VectorTools.distance(feature.getFeatureData(), neighbour.getFeatureData(), weights);
				distances[dist_count++] = new double[]{neighbour_dist, neighbour.getValue()};
			}	
		}
		
		// Sort the distances.
		Arrays.sort(distances, new Comparator<double[]>(){
			public int compare(double[]a, double[]b) {
				return a[0] > b[0] ? 1 : a[0] < b[0] ? -1 : 0;
			}});
		
		// Get the k-mean average.
		float kSum = 0;
		for(int i = 1; i < k + 1; i++) {
			kSum += distances[i][1];
		}
		
		return (int) (kSum / k);
	}
	
	/**
	 * Generate a set of random weights using an existing vector of
	 * weights. The algorithm takes these existing weights and
	 * gitters the values randomly, producing a new set of weights.
	 *
	 * @param dim
	 * The number of vector dimensions to generate the weights for.
	 * @param deltaIndicator
	 * How small should the jittering be on the weights.
	 * @param existingWeights
	 * The set of existing weights provided.
	 * @return
	 */
	private static double[]getWeights(
			int dim,
			int deltaIndicator,
			double[]existingWeights) {
		
		if(deltaIndicator == 0) {
			double[]weights = new double[dim];
			for(int i = 0; i < weights.length; i++) {
				weights[i] = Math.random();
			}

			return weights;
		} else {
			double[]weights = new double[dim];
			double epsilon = Math.pow(0.5, deltaIndicator);
			
			for(int i = 0; i < weights.length; i++) {
				double delta = epsilon * (-1 + 2*Math.random());
				weights[i] = existingWeights[i] + delta;
			}
			
			return weights;
		}
	}

	/**
	 * Helper function for writing a set of grouped features to a file.
	 * 
	 * @param featuresPerClass
	 * The grouped features to wrtie.
	 * @param fileName
	 * The name of the file to write the features to.
	 */
	private static void writeFeaturesToFile(
			Vector<Vector<FeatureValuePair>> featuresPerClass,
			String fileName) {

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(fileName + ".arff", "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(0);
		}
		writer.println("% Retinopathy feature data extracted for the Kaggle competition.");
		writer.println("@relation retinopathy_feaure_data");
		writer.println("@attribute class {c0, c1, c2, c3, c4}");

		int feature_dimension = featuresPerClass.get(0).get(0).getFeatureData().length;
		for(int i = 0; i < feature_dimension; i++) {
			writer.println("@attribute a" + (i + 1) + " real");
		}
		writer.print("@data");
		
		for(int t = 0; t < featuresPerClass.size(); t++) {
			Vector<FeatureValuePair> class_data = featuresPerClass.get(t);
			for (int i = 0; i < class_data.size(); i++) {

				writer.println("");
				writer.print("c" + class_data.get(i).getValue() + ",");

				double [] data = class_data.get(i).getFeatureData();
				for(int j = 0; j < data.length; j++) {
					writer.print((j > 0 ? "," : "") + data[j]);
				}
			}			
		}
		
		writer.close();	
	}

	/**
	 * Helper function for reading in input features.
	 * 
	 * @param inputDirectory
	 * The input directory to read the input features from.
	 */
	private static Vector<FeatureValuePair> readInFeatures(
			String inputDirectory) {
		
		/**The features to collect.**/
		Vector<FeatureValuePair> featureValuePairs = new Vector<FeatureValuePair>();
		
		// Read each file in the directory provided.
		int prev_size = -1;
		File folder = new File(inputDirectory);
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
		    if (file.isFile()) {

		    	// Parse the file line by line for feature data.
		    	Vector<String> data = new Vector<String>();
		        BufferedReader br = null;
				FileInputStream fstream = null;
				
				try {
					
					fstream = new FileInputStream(inputDirectory + "/" + file.getName());
					br = new BufferedReader(new InputStreamReader(fstream));
					
					String line;
					while ((line = br.readLine()) != null) {
						String [] lineSplit = line.split("#");
						if(lineSplit.length < 2) {
							continue;
						}

						data.add(lineSplit[1]);
					}
					
					br.close();
				} catch(Exception  e) {
					System.out.println("Failed feature data.");
				}
				
				
				// Store the parsed data in a feature.
				if(data.size() == 0) continue;
				double [] feature_data = new double[data.size() - 1];
				for(int i = 1; i < data.size(); i++) {
					feature_data[i-1] = Double.parseDouble(data.get(i));
					if(Double.isInfinite(feature_data[i-1]) || Double.isNaN(feature_data[i-1])) {
						System.out.println("BOO BAD DATA!");
						feature_data[i-1] = 0;
					}
				}

				if(prev_size == -1) {
					prev_size = feature_data.length;
				} else {
					if(prev_size != feature_data.length) {
						System.out.println("Uneven dimensions!!");
						System.exit(0);
					}
				}

				// if(Integer.parseInt(data.get(0)) != 0 && Integer.parseInt(data.get(0)) != 4) continue;
				FeatureValuePair featurePair = new FeatureValuePair(file.getName(), Integer.parseInt(data.get(0)), feature_data);
				featureValuePairs.add(featurePair);
		    }
		}
		
		return featureValuePairs;
	}

	/**
	 * Helper function for returning the list of features grouped by class.
	 * 
	 * @param valuesArray
	 * The features to group.
	 * @param limit
	 * The limit of how much features to add to each group, if specified.
	 */
	private static Vector<Vector<FeatureValuePair>> getGroupedFeatures(
			FeatureValuePair[]valuesArray,
			boolean limit) {
		
		int cur_0_count = 0;int cur_1_count = 0;int cur_2_count = 0;int cur_3_count = 0;int cur_4_count = 0;
		Vector<FeatureValuePair> class_0_data = new Vector<FeatureValuePair>();
		Vector<FeatureValuePair> class_1_data = new Vector<FeatureValuePair>();
		Vector<FeatureValuePair> class_2_data = new Vector<FeatureValuePair>();
		Vector<FeatureValuePair> class_3_data = new Vector<FeatureValuePair>();
		Vector<FeatureValuePair> class_4_data = new Vector<FeatureValuePair>();
		
		// Pick two hundred vectors from each class
		for(int i = 0; i < valuesArray.length; i++) {
			if (valuesArray[i].getValue() == 0) {
				if (!limit || cur_0_count < MAX_FEATURES_PER_CLASS) {
					class_0_data.add(valuesArray[i]);
					cur_0_count++;
				}
			}
			if (valuesArray[i].getValue() == 1) {
				if (!limit || cur_1_count < MAX_FEATURES_PER_CLASS) {
					class_1_data.add(valuesArray[i]);
					cur_1_count++;
				}
			}
			if (valuesArray[i].getValue() == 2) {
				if (!limit || cur_2_count < MAX_FEATURES_PER_CLASS) {
					class_2_data.add(valuesArray[i]);
					cur_2_count++;
				}
			}
			if (valuesArray[i].getValue() == 3) {
				if (!limit || cur_3_count < MAX_FEATURES_PER_CLASS) {
					class_3_data.add(valuesArray[i]);
					cur_3_count++;
				}
			}
			if (valuesArray[i].getValue() == 4) {
				if (!limit || cur_4_count < MAX_FEATURES_PER_CLASS) {
					class_4_data.add(valuesArray[i]);
					cur_4_count++;
				}
			}
		}
		
		Vector<Vector<FeatureValuePair>> class_data_set = new Vector<Vector<FeatureValuePair>>();
		class_data_set.add(class_0_data);
		class_data_set.add(class_1_data);
		class_data_set.add(class_2_data);
		class_data_set.add(class_3_data);
		class_data_set.add(class_4_data);
		
		return class_data_set;
	}

	/**
	 * Helper function for shuffeling the list of feature value pairs.
	 * 
	 * @param featurePairs
	 * The feature values to shuffle.
	 */
	private static FeatureValuePair [] shuffleFeatureValues(Vector<FeatureValuePair> featurePairs) {
		FeatureValuePair[]values_array = new FeatureValuePair[featurePairs.size()];
		featurePairs.toArray(values_array);
		shuffleArray(values_array);
		
		return values_array;
	}

	/**
	 * Implement the FisherÐYates shuffle.
	 * 
	 * @param ar
	 * The array to shuffle.
	 */
	private static void shuffleArray(FeatureValuePair [] ar)
	{
		Random rnd = new Random();
		for (int i = ar.length - 1; i > 0; i--)
		{
			int index = rnd.nextInt(i + 1);
			FeatureValuePair a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	/**
	 * Deep copy an array.
	 * 
	 * @param vals
	 * The array to copy.
	 */
	private static double[] copyArray(double[]vals) {
		double[]new_vals = new double[vals.length];
		for(int i = 0; i < vals.length; i++) {
			new_vals[i] = vals[i];
		}
		return new_vals;
	}
}
