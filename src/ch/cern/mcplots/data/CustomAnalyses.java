/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.data;

import java.io.File;
import java.util.HashMap;

/**
 *
 * @author icharala
 */
public class CustomAnalyses {
    
    private final File analysisFolder;

    /**
     * Custom analysis data entry
     */
    public static class Data {
        
        /**
         * The name of the analysis
         */
        String analysisName;

        /**
         * The base name (path+filename without extension) of the file
         */
        private final String baseName;
        
        /**
         * Check if the given extension exists for this analysis
         * 
         * @param extension The filename extension (ex. 'cc', 'aida', 'plot', ...)
         * @return Returns TRUE if such file exists
         */
        Boolean has(String extension) {
            File f = new File(baseName + "." + extension);
            return f.exists();
        }

        /**
         * Constructor of the data class 
         * @param analysisName The name of the analysis
         * @param baseName The file path+filename without extension 
         */
        public Data(String analysisName, String baseName) {
            this.analysisName = analysisName;
            this.baseName = baseName;
        }
        
    }
    
    /**
     * Map of data entries indexed by the analysis name
     */
    HashMap<String, Data> analyses;
    
    /**
     * Initialize custom analysis class
     * @param analysisFolder The full path to the folder to use for custom analysis
     */
    public CustomAnalyses(String analysisFolder) {
        
        this.analysisFolder = new File(analysisFolder);
        this.analyses = new HashMap<>();
        
        // Scan alayses in folder & populate analyses array
        File[] files = this.analysisFolder.listFiles();
        for (File f: files) {
            if (f.isFile()) {
                
                // Get filename
                String n = f.getName();
                
                // get extension
                String[] parts = n.split("\\.");
                String ext = parts[parts.length-1];
                
                // Strip extension
                n = n.substring(0, n.length()-ext.length()-1);
                
                // Create entry
                if (!analyses.containsKey(n)) {
                    File subPath = new File(this.analysisFolder, n);
                    analyses.put(n, new Data(n, subPath.getPath()) );
                }
            }
        }
        
    }
    
}
