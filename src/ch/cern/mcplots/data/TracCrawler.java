/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.cern.mcplots.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 *
 * @author icharala
 */
public class TracCrawler {

    String tracBrowserURL;

    /**
     * Call this function once in your program to install a "TrustAll" trust
     * manager that is going to accept all SSL certificates.
     */
    public static void trustAllSSL() {

        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };
            
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);   
            
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(TracCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(TracCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Initialize the TRAC Crawler class
     * @param tracBrowserURL The browse url of the trac (ex. http://<mydomain>/trac/browser)
     */
    public TracCrawler(String tracBrowserURL) {
        this.tracBrowserURL = tracBrowserURL;
        
        // Remove trailing slash
        if (this.tracBrowserURL.endsWith("/")) {
            this.tracBrowserURL = this.tracBrowserURL.substring(0,this.tracBrowserURL.length()-1);
        }
        
    }
    
    /**
     * Utility function to download string buffer from given URL
     * @param fromURL The URL to download from
     * @return The string buffer
     */
    private String wget(String fromURL) {
        try {
            
            // Send request
            URL url = new URL(fromURL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            // Collect response
            String ans = "";
            String s = null;
            while ((s = reader.readLine()) != null) {
                ans += s + "\n";
            }
            
            // Return answer
            return ans;
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(TracCrawler.class.getName()).log(Level.SEVERE, null, ex);
            return "";
            
        } catch (IOException ex) {
            Logger.getLogger(TracCrawler.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }
    
    /**
     * Download a file from TRAC from the given path
     * 
     * @param path The file name relative to root
     * @param rev The revision (ex. tag revision retrieved from getTagS()) or empty for trunk
     * @return The contents of the file
     */
    public String downloadFile(String path, String rev) {

        // Build URL
        String fullURL = tracBrowserURL + "/" + path.replace("\\", "/");
        if (!rev.isEmpty()) {
            fullURL += "?ref=" + rev + "&format=raw";
        } else {
            fullURL += "?format=raw";
        }
        
        // Download
        return wget(fullURL);
        
    }
    
    /**
     * Enumarate the files in the given path
     * 
     * @param path The path to list (relative to root, without beginning slash)
     * @param rev The revision (ex. tag revision retrieved from getTagS()) or empty for trunk
     * @return The list of files in the directory
     */
    public ArrayList<String> getFiles(String path, String rev) {
        
        // Build URL
        String fullURL = tracBrowserURL + "/" + path.replace("\\", "/");
        if (!rev.isEmpty()) {
            fullURL += "?ref=" + rev;
        }
        
        // Download TRAC Browser body
        String body = wget(fullURL);
        if (body.isEmpty()) return null;
        
        // Look for the list which contains the tags
        Document dom = Jsoup.parse(body);
        
        // Look for the directory listing element
        Element eJumpLoc = dom.getElementById("dirlist");
        if (eJumpLoc == null) return null;
        
        // Look for table rows
        ArrayList<String> ans = new ArrayList<>();
        Elements esRows = eJumpLoc.getElementsByTag("tr");
        for (Element row: esRows) {
            
            // Scal columns to find the name
            Elements esColumns = row.children();
            for (Element col: esColumns) {
                
                // It cas a 'class=name' attrib
                if (col.hasAttr("class") && col.attr("class").equals("name")) {
                    
                    // Inside it has a link
                    Elements esLinks = col.getElementsByTag("a");
                    if (!esLinks.isEmpty()) {
                        
                        // Collect ONLY files
                        Element e = esLinks.first();
                        if (e.hasAttr("class") && e.attr("class").equals("file")) {
                            ans.add(e.text());
                        }
                        
                    }
                    
                }
                
            }
            
        }
        
        // Return listing
        return ans;
        
    }
    
    /**
     * Crawl the TRAC website and locate the tags in the project
     * @return A HashMap in [tag] => [get_params] format
     */
    public HashMap<String, String> getTags() {
                
        // Download TRAC Browser body
        String body = wget(tracBrowserURL);
        if (body.isEmpty()) return null;
        
        // Look for the list which contains the tags
        Document dom = Jsoup.parse(body);
        
        // Look for the jumploc element
        Element eJumpLoc = dom.getElementById("jumploc");
        if (eJumpLoc == null) return null;
        
        // ... it contains a <select> with everything
        Elements esSelect = eJumpLoc.getElementsByTag("select");
        if (esSelect.isEmpty()) return null;
        
        // ... with <optgroup>s
        Elements esOptGroups = esSelect.first().getElementsByTag("optgroup");
        for (Element grp: esOptGroups) {
            // ... look for label=tags
            if (grp.hasAttr("label") && grp.attr("label").equals("tags")) {
                
                // Traverse optgroup & build tag list
                HashMap<String, String> ans = new HashMap<>();
                for (Element e: grp.children()) {
                    
                    // Get revision argument from URL
                    String linkURL = e.attr("value");
                    String[] parts = linkURL.split("\\?rev=");
                    if (parts.length > 1) {
                        linkURL = parts[1];
                    } else {
                        linkURL = "";
                    }
                   
                    // Store the tag -> param mapping
                    ans.put(e.text(), linkURL);
                }
                return ans;
                
            }
        }
        
        // Something went wrong
        return null;
    }
    
}
