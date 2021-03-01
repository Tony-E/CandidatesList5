
package CandidatesList;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;

/**
 * Class MPC QueryDetail constructs a GET for the default browser to show the detailed ephemeris of 
 * selected objects.
 * 
 * @author Tony Evans
 */

public class MPCQueryDetail {
    // web address of the MPC ephemeris form and the NEOCP page.
    private static final String MPCEphemeris = "http://www.minorplanetcenter.net/cgi-bin/mpeph2.cgi";
    private static final String NEOCPpage = "http://www.minorplanetcenter.net/iau/NEO/toconfirm_tabular.html";
    
    // base structure of the quey block parts 1 and 2.
    private static final String queryPart1 = "?ty=e&TextArea=";
    private static final String separator ="%0D%0A";
    private static final String queryPart2="&long=&lat=&alt=&raty=a&s=t&m=m&adir=S&oed=&e=-2&resoc=&tit=&bu=&ch=c&ce=f&js=f";
   
    // pointer to the candidates table */
    private final JTable cTable;   
 
    /**
     * Constructor saves pointer to the displayed table in the GUI.
     * @param t Table of objects.
     **/
    public MPCQueryDetail(JTable t) {
        cTable = t;
    }
    
    /**
     * Build and submit an ephemeris query for designated objects. 
     * @param tFrom Start time.
     * @param tTo End time.
     * @param obs Observatory.
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public void doQuery(DateTime tFrom,DateTime tTo, String obs) throws IOException {
        
        // First part is url and initial common query text
        String query = MPCEphemeris+queryPart1;
        
        // followed by object list, maximum of 15 objects
        int count = 0;
        for (int i=0; i<cTable.getRowCount(); i++) {
            Candidate c = (Candidate) cTable.getValueAt(i, 0);
            if ((!c.NEOCP) && ((Boolean)cTable.getValueAt(i,1))) {
                 query+=c.MPCORBid.trim()+separator;
                 if (15<count++) {break;}
            }
        }
        
        // followed by the date, number of dates and interval
        query+="&d=JD+"+tFrom.getJdate();
        query+="&l=10&i=30&u=m&uto=0";
        query+="&c="+obs;
        
        // followed by the rest of the query text
        query+=queryPart2;
        
        // now try to run the query using the default browser
        try {
            URI uri = new URI(query);
            Desktop.getDesktop().browse(uri);
        } catch (URISyntaxException ex) {
            Logger.getLogger(MPCQueryDetail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
}
    
