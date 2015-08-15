package detectors;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import retinopathy.structures.Constants;

/**
 * Data structure for holding image feature information.
 *
 * @author Peter Bugaj
 */
public class ImageFeatures {
	
	/**
	 * The radius of the eye.
	 */
	private float eyeRadius;
	
	/**
	 * The area of the eye.
	 */
	private float eyeArea;
	
	/**
	 * The file name to write the feature logs to.
	 */
	private String outputFile = null;

	/**
	 * A vector of analyzed patches.
	 */
	private Vector<PatchAnalysis> analyzedPatches = new Vector<PatchAnalysis>();
	
	/**
	 * The logged feature statistics.
	 */
	private Vector<String> log= new Vector<String>();

	
	/**
	 * Create a new instance of the ImageFeatures class.
	 * 
	 * @param outputFile
	 * The output file to log the features to.
	 */
	public ImageFeatures(String outputFile) {
		this.outputFile = outputFile;
	}
	
	/**
	 * Set the eye radius.
	 * 
	 * @param eyeRadius
	 * The eye radius to set.
	 */
	public void setEyeRadius(int eyeRadius) {
		this.eyeRadius = eyeRadius;
		this.eyeArea = (float) (Math.PI * Math.pow(this.eyeRadius, 2));
	}

	/**
	 * Add a feature statistic to a log file.
	 * 
	 * @param stat
	 * The statistic to log.
	 */
	public void addToFeatureLog(String stat) {
		log.add(stat);
	}
	
	/**
	 * Set the list of analyzed patches.
	 */
	public void setAnalyzedPatches(Vector<PatchAnalysis> analyzed_patches) {
		this.analyzedPatches = analyzed_patches;
	}
	
	/**
	 * Write the logs to file.
	 */
	public void writeLogToFile() {

		PrintWriter writer = null;

		try {
			writer = new PrintWriter(outputFile.replace(".jpeg", ".txt"), "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(0);
		}

		for(int i = 0; i < log.size(); i++) {
			writer.println(log.get(i));
		}

		writer.close();
	}
	
	/**
	 * Write the analyzed patches to a log file.
	 */
	public void writePatchesLogToFile() {
		
		String curvature_string = "CURVATURE_STRONG";
		int[][] logs = new int[2][40];

		double patch_count = 0;
		for(int k = 0; k < analyzedPatches.size(); k++) {
			PatchAnalysis next_analyzed_patch = analyzedPatches.get(k);
			if(next_analyzed_patch.isNoise()) continue;
			
			patch_count++;

			int average_intensity_level = next_analyzed_patch.getAverageIntensity() == Constants.PATCH_intensity_weak ? 0 : 1;
			
			int index_offset =
					next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_weak ? 0 :
					next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_medium ? 1 :
					next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_strong ? 2 :
					next_analyzed_patch.getPatchStrength() == Constants.PATCH_strength_xstrong ? 3 : 4;
			
			// Draw and log the XXXX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxxxlarge) {
				logs[average_intensity_level][35 + index_offset]++;
			}
			
			// Draw and log the XXXX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxxlarge) {
				logs[average_intensity_level][30 + index_offset]++;
			}
			
			// Draw and log the XXX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxxlarge) {
				logs[average_intensity_level][25 + index_offset]++;
			}
			
			// Draw and log the XX-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xxlarge) {
				logs[average_intensity_level][20 + index_offset]++;
			}
			
			// Draw and log the X-Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_xlarge) {
				logs[average_intensity_level][15 + index_offset]++;
			}
			
			// Draw and log the Large patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_large) {
				logs[average_intensity_level][10 + index_offset]++;
			}
			
			// Draw and log the Medium patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_medium) {
				logs[average_intensity_level][5 + index_offset]++;
			}
			
			// Draw and log the Small patches
			if(next_analyzed_patch.getPatchSize() == Constants.PATCH_size_small) {
				logs[average_intensity_level][0 + index_offset]++;
			}
		}

		patch_count = Math.max(patch_count, 1);
		
		// Write the data to logs with prepended labels.
		for(int i = 0; i < logs.length; i++) {
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_WEAK#" + ((logs[i][0]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_WEAK#" + ((logs[i][0]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_MEDIUM#" + ((logs[i][1]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_MEDIUM#" + ((logs[i][1]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_STRONG#" + ((logs[i][2]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_STRONG#" + ((logs[i][2]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_XSTRONG#" + ((logs[i][3]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_XSTRONG#" + ((logs[i][3]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_XXSTRONG#" + ((logs[i][4]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|SMALL_XXSTRONG#" + ((logs[i][4]/this.eyeArea)*100) );
			this.addToFeatureLog("");
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_WEAK#" + ((logs[i][5]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_WEAK#" + ((logs[i][5]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_MEDIUM#" + ((logs[i][6]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_MEDIUM#" + ((logs[i][6]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_STRONG#" + ((logs[i][7]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_STRONG#" + ((logs[i][7]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_XSTRONG#" + ((logs[i][8]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_XSTRONG#" + ((logs[i][8]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_XXSTRONG#" + ((logs[i][9]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|MEDIUM_XXSTRONG#" + ((logs[i][9]/this.eyeArea)*100) );
			this.addToFeatureLog("");		
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_WEAK#" + ((logs[i][10]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_WEAK#" + ((logs[i][10]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_MEDIUM#" + ((logs[i][11]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_MEDIUM#" + ((logs[i][11]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_STRONG#" + ((logs[i][12]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_STRONG#" + ((logs[i][12]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_XSTRONG#" + ((logs[i][13]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_XSTRONG#" + ((logs[i][13]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_XXSTRONG#" + ((logs[i][14]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|LARGE_XXSTRONG#" + ((logs[i][14]/this.eyeArea)*100) );
			this.addToFeatureLog("");	
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_WEAK#" + ((logs[i][15]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_WEAK#" + ((logs[i][15]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_MEDIUM#" + ((logs[i][16]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_MEDIUM#" + ((logs[i][16]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_STRONG#" + ((logs[i][17]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_STRONG#" + ((logs[i][17]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_XSTRONG#" + ((logs[i][18]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_XSTRONG#" + ((logs[i][18]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_XXSTRONG#" + ((logs[i][19]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XLARGE_XXSTRONG#" + ((logs[i][19]/this.eyeArea)*100) );
			this.addToFeatureLog("");	
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_WEAK#" + ((logs[i][20]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_WEAK#" + ((logs[i][20]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_MEDIUM#" + ((logs[i][21]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_MEDIUM#" + ((logs[i][21]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_STRONG#" + ((logs[i][22]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_STRONG#" + ((logs[i][22]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_XSTRONG#" + ((logs[i][23]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_XSTRONG#" + ((logs[i][23]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_XXSTRONG#" + ((logs[i][24]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXLARGE_XXSTRONG#" + ((logs[i][24]/this.eyeArea)*100) );
			this.addToFeatureLog("");			
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_WEAK#" + ((logs[i][25]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_WEAK#" + ((logs[i][25]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_MEDIUM#" + ((logs[i][26]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_MEDIUM#" + ((logs[i][26]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_STRONG#" + ((logs[i][27]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_STRONG#" + ((logs[i][27]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_XSTRONG#" + ((logs[i][28]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_XSTRONG#" + ((logs[i][28]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_XXSTRONG#" + ((logs[i][29]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXLARGE_XXSTRONG#" + ((logs[i][29]/this.eyeArea)*100) );
			this.addToFeatureLog("");
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_WEAK#" + ((logs[i][30]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_WEAK#" + ((logs[i][30]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_MEDIUM#" + ((logs[i][31]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_MEDIUM#" + ((logs[i][31]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_STRONG#" + ((logs[i][32]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_STRONG#" + ((logs[i][32]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_XSTRONG#" + ((logs[i][33]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_XSTRONG#" + ((logs[i][33]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_XXSTRONG#" + ((logs[i][34]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXLARGE_XXSTRONG#" + ((logs[i][34]/this.eyeArea)*100) );
			this.addToFeatureLog("");
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_WEAK#" + ((logs[i][35]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_WEAK#" + ((logs[i][35]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_MEDIUM#" + ((logs[i][36]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_MEDIUM#" + ((logs[i][36]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_STRONG#" + ((logs[i][37]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_STRONG#" + ((logs[i][37]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_XSTRONG#" + ((logs[i][38]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_XSTRONG#" + ((logs[i][38]/this.eyeArea)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_XXSTRONG#" + ((logs[i][39]/patch_count)*100) );
			this.addToFeatureLog("MICROANEURISM|" + curvature_string + "|XXXXXLARGE_XXSTRONG#" + ((logs[i][39]/this.eyeArea)*100) );
			this.addToFeatureLog("");
		}
	}
}
