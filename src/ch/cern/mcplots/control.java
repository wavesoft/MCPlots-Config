/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots;

import ch.cern.mcplots.data.CustomAnalyses;
import ch.cern.mcplots.data.RivetHistograms;
import ch.cern.mcplots.data.TracCrawler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author icharala
 */
public class control {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            
            // Load RivetHistograms class 
            RivetHistograms rh = new RivetHistograms("rivet-histograms.map");
            rh.load();

            // Load custom analyses information
            CustomAnalyses ca = new CustomAnalyses("C:\\Users\\icharala\\Local\\Develop\\Work\\mcplots\\trunk\\scripts\\mcprod\\analyses");
            
            // Load Rivet tags
            TracCrawler.trustAllSSL();
            TracCrawler tc = new TracCrawler("https://rivet.hepforge.org/trac/browser");
            //HashMap<String, String> tags = tc.getTags();
            ArrayList<String> files = tc.getFiles("src/Analyses", "");
            
        } catch (IOException ex) {
            Logger.getLogger(control.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
