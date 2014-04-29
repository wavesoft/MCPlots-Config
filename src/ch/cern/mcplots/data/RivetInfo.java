/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.data;

import ch.cern.mcplots.ui.ProgressEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;
 
/**
 *
 * @author icharala
 */
public class RivetInfo {

    public static interface AsyncTagsHandler {
        public void rivetTagsReceived( HashMap<String, String> tags );
    }
    public static interface AsyncAnalysisMatrixHandler {
        public void rivetMatrixReceived( HashMap<String, Object> analysisData );
    }

    /**
     * A thread monitor that waits all threads to complete
     */
    private static class ThreadMonitor {

        AsyncAnalysisMatrixHandler handler;
        ProgressEvents progressHandler;
        HashMap<String, Object> output;
        
        /**
         * The counter of running threads
         */
        private int numThreads;

        /**
         * Initialize the thread monitor
         * @param handler
         * @param output 
         */
        private ThreadMonitor(ProgressEvents progressHandler, AsyncAnalysisMatrixHandler handler, HashMap<String, Object> output) {
            this.handler = handler;
            this.progressHandler = progressHandler;
            this.output = output;
            this.numThreads = 0;
        }
        
        /**
         * Handle start of thread
         */
        synchronized void threadStarted() {
            // Increment running threads counter
            numThreads++;
        }
        
        /**
         * Handle completion of thread execution
         */
        synchronized void threadCompleted() {
            // Wait until all threads are completed
            if (--numThreads == 0) {
                
                // Fire completion handler
                if (progressHandler != null)
                    progressHandler.progressCompleted();
                
                // Fire handler upon completion
                handler.rivetMatrixReceived(output);
                
            }
        }
        
    }
    
    /**
     * Internal class used for downloading analyses info
     */
    private class DownloadThread implements Runnable {
        
        ThreadMonitor monitor;
        LinkedList<String> input;
        int inputSize;
        HashMap<String, Object> output;
        ProgressEvents progressHandler;
        String rev;

        /**
         * Initialize a downloader thread.
         * 
         * @param parent The parent RivetInfo class
         * @param input The list to read the analysis names from
         * @param output The list to write the analysis data to
         * @param rev The TRAC revision we are in
         */
        public DownloadThread(ThreadMonitor monitor, ProgressEvents progressHandler, LinkedList<String> input, int inputSize, HashMap<String, Object> output, String rev) {
            this.progressHandler = progressHandler;
            this.monitor = monitor;
            this.input = input;
            this.inputSize = inputSize;
            this.output = output;
            this.rev = rev;
        }

        @Override
        public void run() {
            
            // Notify thread monitor that we started
            monitor.threadStarted();
            
            // Yaml parser
            Yaml yaml = new Yaml();

            // If no analyses left, quit
            while (!input.isEmpty()) {

                // Get analyses name
                String name = null;
                synchronized (this) {
                    
                    // Fetch name from input
                    name = input.removeFirst();

                    // Let people know that we are downloading
                    if (progressHandler != null)
                        progressHandler.progress("Analyzing "+name+"...", inputSize+1-input.size(), inputSize+1);        
                    
                }
                
                // Check for error
                if (name == null)
                    break;

                // Download buffer
                String buffer = rivetTrac.downloadFile("data/anainfo/"+name+".info", rev);
                if (!buffer.isEmpty()) {

                    // Parse with YAML
                    Object o = null;
                    try {
                        o = yaml.load(buffer);
                    }
                    catch (ScannerException ex) {
                        java.util.logging.Logger.getLogger(DownloadThread.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);                        
                    }
                    catch (Exception ex) {
                        java.util.logging.Logger.getLogger(DownloadThread.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);                        
                    }

                    // Store error (null) or value
                    synchronized (this) {
                        output.put(name, o);
                    }

                } else {

                    // Store error
                    synchronized (this) {
                        output.put(name, null);
                    }

                }

            }
            
            // Notify thread monitor that we are completed
            monitor.threadCompleted();
            
        }
        
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
    public void asyncGetTags( final AsyncTagsHandler handler, final ProgressEvents progressHandler ) {
        if (progressHandler != null) {
            progressHandler.progressStarted();
            progressHandler.progress("Looking-up rivet tags...", 0, 1);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                
                // Get rivet tags
                HashMap<String, String> tags = rivetTrac.getTags();
                
                // First fire progress completion handlers
                if (progressHandler != null) {
                    progressHandler.progress("Received rivet tags", 1, 1);
                    progressHandler.progressCompleted();
                }
                
                // Then fire callback handlers
                handler.rivetTagsReceived(tags);

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
    public void asyncGetAnalysesMatrix( final String rev, final int threads, final AsyncAnalysisMatrixHandler handler, final ProgressEvents progressHandler ) {
        if (progressHandler != null) {
            progressHandler.progressStarted();
            progressHandler.progress("Discovering available analyses...", 0, 1);
        }
        
        // Run all the time-consuming operations in another thread
        Thread tt = new Thread(new Runnable() {

            @Override
            public void run() {

                // Fetch array list
                ArrayList<String> analyses = rivetTrac.getFiles("src/Analyses", rev);
                if (analyses == null) {
                    return;
                }

                // Create a thread-safe input and output
                LinkedList<String> input = new LinkedList<>();
                HashMap<String, Object> output = new HashMap<>();

                // Populate input
                for (String ana: analyses) {
                    if (ana.endsWith(".cc")) {
                        input.add(ana.substring(0, ana.length()-3));
                    }
                }

                // Prepare thread monitor which is going to fire
                // the handler upon completion
                ThreadMonitor tm = new ThreadMonitor(progressHandler, handler, output);

                // Get number of elements to scan
                int inputSize = input.size();

                // Notify that we now know the number of analyses
                if (progressHandler != null)
                    progressHandler.progress("Starting download...", 1, inputSize+1);        

                // Create workers
                for (int i=0; i< threads; i++) {
                    Thread t = new Thread( new DownloadThread(tm, progressHandler, input, inputSize, output, rev) );
                    t.start();
                }

                // The callbacks will be fired by the DownloadThread and
                // the ThreadMonitor classes when needed.

            }
            
        });
        
        // Start thread
        tt.start();
        
    }
    
}
