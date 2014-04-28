/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.data;

import java.util.HashMap;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author icharala
 */
public class RivetInfo {

    public static interface AsyncTagsHandler {
        public void rivetTagsReceived( HashMap<String, String> tags );
    }
    public static interface AsyncTagMatrixHandler {
        public void rivetMatrixReceived( HashMap<String, Object> analysisData );
    }
    public static interface AsyncProgressHandler {
        public void rivetProgress( String message, int item, int max );
    }
    
    /**
     * The TRAC crawler class which is used for looking-up up-to-date
     * Rivet information.
     */
    private final TracCrawler rivetTrac;

    /**
     * Initialize the RivetInfo class
     */
    public RivetInfo() {
        rivetTrac = new TracCrawler("https://rivet.hepforge.org/trac/browser");
    }
    
    /**
     * Fetch the list of available Rivet tags from online.
     * This function runs in a separate thread.
     * 
     * @param handler The handler
     * @param progressHandler A handler that can receive interface updates
     */
    public void asyncGetTags( final AsyncTagsHandler handler, final AsyncProgressHandler progressHandler ) {
        if (progressHandler != null)
            progressHandler.rivetProgress("Looking-up rivet tags", 0, 1);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.rivetTagsReceived(rivetTrac.getTags());
                if (progressHandler != null)
                    progressHandler.rivetProgress("Received rivet tags", 1, 1);
                
            }
        });
        t.start();
    }
    
    /**
     * Return the configuration of all analyses for the given revision.
     * This takes a lot of time.
     * 
     * @param rev The revision
     * @param threads The number of concurrent threads
     * @param handler The handler for the asynchronous matrix receiver.
     * @param progressHandler  The progress event handler receiver.
     */
    public void asyncGetAnalysesMatrix( String rev, int threads, final AsyncTagMatrixHandler handler, final AsyncProgressHandler progressHandler ) {
        
    }
    
}
