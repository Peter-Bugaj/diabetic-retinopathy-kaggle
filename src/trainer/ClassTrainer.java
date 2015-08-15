package trainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Vector;

import tools.math.KappaCalculator;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Class for running different types of classifier provided by WEKA
 * on a set of image features stored in text files.
 * 
 * @author Peter Bugaj
 */
public class ClassTrainer {

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
		
		int seed = 0;
		
		// Read in the feature value pairs from the input directory specified.
		Vector<FeatureValuePair> featurePairs = readInFeatures(inputDir);

		
		// Shuffle the input values and train the
		// classifier on the subset of those values.
		FeatureValuePair[]values_array = shuffleFeatureValues(featurePairs);
		
		// Group the features by class
		double best_kappa = 0;
		while(true) {

			double computed_kappa = trainClassifier(values_array, seed);		
			System.out.println("Finished training classifier with value: " + computed_kappa);
			if(computed_kappa > best_kappa) {

				// Print the files used for training to log if they are
				// the best files so far for training the classifier.
				Vector<Vector<FeatureValuePair>> train_data_set_per_class = getGroupedFeatures(values_array, true);
				writeFeaturesToFile(train_data_set_per_class, "train_best");
				best_kappa = computed_kappa;
				
				System.out.println("New best classifier found with Kappa value: " + best_kappa);
				System.out.println("Seed: " + seed);
			}
			seed++;
		}
	}

	/**
	 * Train the classifier on the provided input of feature value pairs.
     *
	 * @param valuesArray
	 * The set of feature value pairs.
	 * @param seed
	 * The seed to use with a classifier if required.
	 * @return
	 * The Kappa value of the predicted output created by the trained
	 * classifier.
	 */
	private static double trainClassifier(FeatureValuePair[]valuesArray, int seed) {

		Vector<Vector<FeatureValuePair>> train_data_set_per_class = getGroupedFeatures(valuesArray, true);
		Vector<Vector<FeatureValuePair>> test_data_set_per_class = getGroupedFeatures(valuesArray, false);
		
		// Write the features to an ARFF file.
		writeFeaturesToFile(train_data_set_per_class, "train");
		writeFeaturesToFile(test_data_set_per_class, "test");
		writeFeaturesToFile(test_data_set_per_class, "unlabeled");

		// Read the train data as a WEKA instance.
		Instances train_data = readArffFile("./train.arff");

		// Read the test data as a WEKA instance.
		Instances test_data = readArffFile("./test.arff");
		
		// Read the unlabeled data as a WEKA instance.
		Instances unlabeled_data = readArffFile("./unlabeled.arff");

		// Configure the WEKA instance.
		train_data.setClassIndex(0);
		test_data.setClassIndex(0);
		unlabeled_data.setClassIndex(0);

		// Initiate the classifier.
		Classifier forestClassifier = new RandomForest();
		String[] options = null;
		try {
			options = weka.core.Utils.splitOptions("-I 400 -K 30 -S " + seed);
			forestClassifier.setOptions(options);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Build the classifer
		// Evaluation evaluator = null;
		try {
			forestClassifier.buildClassifier(train_data);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Classify the unlabeled data.
		System.out.println("Finished building the classifier.");
		double [][] kappa_o_values = new double[5][5];
		try {
			for (int i = 0; i < unlabeled_data.numInstances(); i++) {
				int actual_label = (int)test_data.instance(i).classValue();
				int predicted_label = (int)forestClassifier.classifyInstance(unlabeled_data.instance(i));
				kappa_o_values[actual_label][predicted_label]++;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return KappaCalculator.findKappa(kappa_o_values);
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
	 * Helper function for reading an ARFF file.
	 * 
	 * @param arffFileName
	 * The name of the ARFF file to read.
	 */
	private static Instances readArffFile(String arffFileName) {
		Instances data = null;
		try {
			data = DataSource.read(arffFileName);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return data;
	}
}
