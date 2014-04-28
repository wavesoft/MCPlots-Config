/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for accessing rivet-histograms.map
 * @author icharala
 */
public class RivetHistograms {

    /**
     * Regular expression for parsing the configuration lines.
     * It scans patterns according to the following pattern:
     * 
     * [#][beam] [process] [energy] [params] [analysis_histogram] [observable] [cuts]
     *  1    2       3         4        5             6                 7        8
     */
    private static final Pattern rxLineParser = 
            Pattern.compile("^(#?)\\s*([a-z\\-]+)\\s+([\\w\\-]+)\\s+([0-9\\.]+)\\s+([\\w\\-\\.',]+)\\s+([\\w\\-\\.]+)\\s+([\\w\\-\\.]+)\\s+([\\w\\-\\.]+)$");
    
    /**
     * Smaller class that expands the [params] configuration
     */
    public static class DataParams {
        String pTmin;
        String pTmax;
        String mHatMin;
        String mHatMax;
        Boolean defined;
        
        private final int numDefined;

        /**
         * Crate a parameters class from parameters string
         * @param params 
         */
        public DataParams(String params) {
            if (params.isEmpty() || params.equals("-")) {
                defined = false;
                numDefined = 0;
            } else {
                String[] parts = params.split(",");
                pTmin = parts[0];
                numDefined = parts.length;
                if (parts.length > 1)
                    pTmax = parts[1];
                if (parts.length > 2)
                    mHatMin = parts[2];
                if (parts.length > 3)
                    mHatMax = parts[3];
            }
        }

        /**
         * Rebuild the parameters string
         * @return 
         */
        @Override
        public String toString() {
            if (!defined) {
                return "-";
            } else {
                String ans = pTmin;
                if (numDefined > 1)
                    ans += ","+pTmax;
                if (numDefined > 2)
                    ans += ","+mHatMin;
                if (numDefined > 3)
                    ans += ","+mHatMax;
                return ans;
            }
        }
        
    }
    
    /**
     * Representation of data entries in the RivetHistograms file
     */
    public static class Data {
        String beam;
        String process;
        String energy;
        DataParams params;
        String analysis;
        String histogram;
        String observable;
        String cuts;
        Boolean enabled;

        public Data(Matcher lineMatcher) {
                           
            // Check if the line is enabled
            String hashLine = lineMatcher.group(1).trim();
            enabled = hashLine.isEmpty();

            // Get straight-forward parts
            beam = lineMatcher.group(2);
            process = lineMatcher.group(3);
            energy = lineMatcher.group(4);
            params = new DataParams(lineMatcher.group(5));
            observable = lineMatcher.group(7);
            cuts = lineMatcher.group(8);
            
            // Split/parse [analysis_histogram] group, that follows
            // the Rivet configuration.
            String analysis_histogram = lineMatcher.group(6);
            String parts[] = analysis_histogram.split("_");
            histogram = parts[parts.length-1];
            analysis = analysis_histogram.substring(0, analysis_histogram.length()-histogram.length()-1);
            
        }

        @Override
        public String toString() {
            String ans = "";
            
            // Start with disabled hash
            if (!enabled)
                ans = "#";
            
            // Continue with elements
            ans += String.format("%-9s", beam) + " ";
            ans += String.format("%-13s", process) + " ";
            ans += String.format("%-10s", energy) + " ";
            ans += String.format("%-11s", params.toString()) + " ";
            String histo_analysis = analysis + "_" + histogram;
            ans += String.format("%-33s", histo_analysis) + " ";
            ans += String.format("%-15s", observable) + " ";
            ans += cuts;
            
            // Return formatted string
            return ans;
        }
        
    }
    
    /**
     * Grouping of data classes with comments
     */
    public static class DataGroup {
        String comment;
        ArrayList<Data> values;

        public DataGroup() {
            comment = "";
            values = new ArrayList<>();
        }
        
    }

    /**
     * Data groups in the file
     */
    public ArrayList<DataGroup> dataGroups;
    
    /**
     * Heading comment
     */
    private String headingComment;
    
    /**
     * Configuration file
     */
    private final File configFile;

    /**
     * Constructor for RivetHistograms class
     */
    public RivetHistograms() {
        configFile = null;
        dataGroups = new ArrayList<>();
    }
    
    public void load() throws IOException {
        
        // Do I/O Operations in safe context
        FileReader fReader = null;
        try {
            
            fReader = new FileReader(configFile);
            BufferedReader bReader = new BufferedReader(fReader);
            
            // Possition/indexing
            Boolean pReadingHeadingComment = true;
            DataGroup pCurrentGroup = null;
            
            // Reset buffers
            headingComment = "";
            dataGroups.clear();
            
            // Start reading source file
            String line;
            while ( (line = bReader.readLine()) != null ) {
                
                // Trim excess whitespace
                line = line.trim();
                
                // Run regex matcher for this line
                Matcher matcher = rxLineParser.matcher(line);
                
                // Process comment cases
                if (line.startsWith("#")) {
                    
                    // Update heading comment
                    if (pReadingHeadingComment) {
                        headingComment += line + "\n";
                    }
                    
                    // Check if the line is actually a commented-out data line
                    else if (matcher.matches()) {
                        
                        // Create new section if we don't have any active
                        if (pCurrentGroup == null)
                            pCurrentGroup = new DataGroup();
                        
                        // Add data value
                        pCurrentGroup.values.add( new Data(matcher) );
                        
                    }
                    
                    // Update section comment
                    else {
                        
                        // Commit active section (assuming no space was in between.)
                        if ((pCurrentGroup != null) && (pCurrentGroup.values.size() > 0)) {
                            dataGroups.add(pCurrentGroup);
                            pCurrentGroup = null;
                        }
                        
                        // Create new section if we don't have any active
                        if (pCurrentGroup == null)
                            pCurrentGroup = new DataGroup();
                        
                        // Update current group sections
                        pCurrentGroup.comment += line + "\n";
                        
                    }
                }
                
                // Process whitespace cases
                else if (line.isEmpty()) {
                    
                    // Quit heading comment on the first empty line
                    if (pReadingHeadingComment) {
                        pReadingHeadingComment = false;
                    }
                    
                    // Commit active section (split on space)
                    if ((pCurrentGroup != null) && (pCurrentGroup.values.size() > 0)) {
                        dataGroups.add(pCurrentGroup);
                        pCurrentGroup = null;
                    }
                    
                }
                
                // Process data cases
                else {
                    
                    // Check if line matches
                    if (matcher.matches()) {

                        // Create new section if we don't have any active
                        if (pCurrentGroup == null)
                            pCurrentGroup = new DataGroup();

                        // Add data value
                        pCurrentGroup.values.add( new Data(matcher) );

                    } else {
                        Logger.getLogger(RivetHistograms.class.getName()).log(Level.WARNING, "Unparsable line: ", line);
                    }
                    
                }
                
            }

            // Commit active section (without end-line space)
            if ((pCurrentGroup != null) && (pCurrentGroup.values.size() > 0))
                dataGroups.add(pCurrentGroup);
            
            // Close reader
            fReader.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RivetHistograms.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
            
        } finally {
            try {
                fReader.close();
            } catch (IOException ex) {
                Logger.getLogger(RivetHistograms.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    public void save() throws IOException {
        PrintWriter fWriter = new PrintWriter(configFile);
        
        // Write heading comment
        if (!headingComment.isEmpty()) {
            fWriter.println(headingComment);
        }
        
        // Process data groups
        for (DataGroup g: dataGroups) {
            fWriter.print(g.comment);
            for (Data d: g.values) {
                fWriter.println(d.toString());
            }
            fWriter.println();
        }
        
        // Close and flush
        fWriter.close();
    }
    
    public RivetHistograms(String filename) {
        configFile = new File(filename);
        dataGroups = new ArrayList<>();
    }
    
}
