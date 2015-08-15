package classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import trainer.FeatureValuePair;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Runs a forest classifier to classify the test features.
 * Uses the best training data found with combination of
 * the best seed.
 * 
 * Outputs a submission.csv file of the classified features.
 * 
 * @author Peter Bugaj
 */
public class KaggleClassifier {

	/**
	 * The main function.
	 * 
	 * @param args
	 * [0] = Directory containing the test features.
	 * [1] = Location of the best training data set.
	 * [2] = The best seed used in classifying the training data.
	 * [3] = Location for outputting the labelled test data to.
	 */
	public static void main(String[] args) {
		
		// Read the input parameters.
		String testFeatureDirectory = null;
		String bestTrainingSetLocation = null;
		String outputLocation = null;
		int bestSeed = 0;

		if(args.length > 0) {
			testFeatureDirectory = args[0];
			bestTrainingSetLocation = args[1];
			bestSeed = Integer.parseInt(args[2]);
			outputLocation = args[3];
		} else {
			testFeatureDirectory = "./SubmissionData";
			bestTrainingSetLocation = "./train_best.arff";
			outputLocation = "./";
		}

		// Read the best train data used by the trained classifier.
		System.out.println("Reading best trained data.");
		Instances best_train_data = readArffFile(bestTrainingSetLocation);
		best_train_data.setClassIndex(0);

		// Read in the feature value pairs from the input directory specifying
		// the data to be submitted, and convert this data to an ARFF file
		// to the output directory.
		System.out.println("Creating submit data.");
		Vector<FeatureValuePair> featurePairs = readInFeatures(testFeatureDirectory);
		writeFeaturesToFile(featurePairs, outputLocation + "submit");
		
		// Read the ARFF file from the output directory created above and
		// convert it to a WEKA instance.
		System.out.println("Reading in submit data.");
		Instances submit_data = readArffFile(outputLocation + "./submit.arff");
		submit_data.setClassIndex(0);

		// Initiate and build the classifier with the best training data
		// specified as input, and with the seed used in the classifier
		// for labelling the training data.
		System.out.println("Creating classifier.");
		Classifier forestClassifier = buildForestClassifier(best_train_data, bestSeed);
		
		// Classify the test data to be submitted and output the labelled
		// features to a submission csv file into the specified output
		// directory.
		System.out.println("Classifying test data.");
		try {
			PrintWriter writer = new PrintWriter(outputLocation + "submission.csv", "UTF-8");
			writer.write("image,level\n");
			for (int i = 0; i < submit_data.numInstances(); i++) {
				int predicted_label = (int)forestClassifier.classifyInstance(submit_data.instance(i));
				writer.write(featurePairs.get(i).getFileName().replace(".txt", "") +
						"," + predicted_label + "\n");
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Finished classifying features.");
	}
	
	/**
	 * Building a forest classifier using the provided training data and seed.
	 * 
	 * @param trainingData
	 * The training data.
	 * @param bestSeed
	 * The seed used for creating the classifier on the training data.
	 */
	private static Classifier buildForestClassifier(Instances trainingData, int bestSeed) {

		Classifier forestClassifier = new RandomForest();
		String[] options = null;
		try {
			options = weka.core.Utils.splitOptions("-I 900 -K 30 -S " + bestSeed);
			forestClassifier.setOptions(options);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Build the classifer
		try {
			forestClassifier.buildClassifier(trainingData);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return forestClassifier;
	}
	
	/**
	 * Write features to a file.
	 * 
	 * @param featurePairs
	 * The features to write.
	 * @param fileName
	 * The name of the file to write the features to.
	 */
	private static void writeFeaturesToFile(
			Vector<FeatureValuePair> featurePairs,
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

		int feature_dimension = featurePairs.get(0).getFeatureData().length;
		for(int i = 0; i < feature_dimension; i++) {
			writer.println("@attribute a" + (i + 1) + " real");
		}
		writer.print("@data");
		
		for(int t = 0; t < featurePairs.size(); t++) {
			FeatureValuePair data = featurePairs.get(t);

			writer.println("");
			writer.print("c" + data.getValue() + ",");
			for (int i = 0; i < data.getFeatureData().length; i++) {
				writer.print((i > 0 ? "," : "") + data.getFeatureData()[i]);
			}			
		}
		
		writer.close();	
	}

	/**
	 * Read in feature data from the input directory.
	 * 
	 * @param inputDirectory
	 * The input directory to read the features from.
	 * @return
	 * The features returned in a vector structure.
	 */
	private static Vector<FeatureValuePair> readInFeatures(
			String inputDirectory) {
		
		// The features to collect.
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
						System.out.println("Bad numerical data found!");
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
				FeatureValuePair featurePair = new FeatureValuePair(file.getName(), 0, feature_data);
				featureValuePairs.add(featurePair);
		    }
		}
		
		return featureValuePairs;
	}
	
	/**
	 * Read in an ARFF data file.
	 * 
	 * @param arffFileName
	 * The name and location of the ARFF file to read.
	 * @return
	 * The ARFF file parsed and converted to a WEKA instance.
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
