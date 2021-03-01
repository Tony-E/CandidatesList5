
package CandidatesList;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;

/*****************************************************************************************
 * Class NEOCPQuery constructs and executes a GET to show selected NEOCPs the default browser.
 *
 * @author Tony Evans 
 */
public class NEOCPQuery {
    // This is the POST address of the NEOCP ephemeris form 
    private static final String NEOCPEphemeris = "http://cgi.minorplanetcenter.net/cgi-bin/confirmeph2.cgi";
    // This is the base structure of the quey block parts 1 and 2. 
    private static final String queryPart1 = "?mb=-30&mf=30&dl=-90&du=%2B90&nl=0&nu=100&sort=d&W=j";
    private static final String queryPart2 = "&Parallax=1&long=&lat=&alt=&int=1&raty=a&mot=m&dmot=p&out=f&sun=x&oalt=20";
    // This is the format of the inserted parameters:  &obj=XGB176E&obscode=G40&start=13
    
    // pointer to the candidates table */
    private final JTable cTable;  
    
   /**
    * Constructor saves pointer to the candidates table showing in the GUI.
    * @param t Table of candidates.
    */
    public NEOCPQuery(JTable t) {
        cTable = t;
    }
    
   /**
    * Build and submit a query. 
    * @param tFrom Start time.
    * @param tTo End time.
    * @param Observatory code.
    */
    public void doQuery(DateTime tFrom,DateTime tTo, String obs) throws IOException {
        
        // First part is url and initial common query text
        String query = NEOCPEphemeris+queryPart1;
        
        // followed by object list, maximum of 15 objects
        int count = 0;
        for (int i=0; i<cTable.getRowCount(); i++) {
            Candidate c = (Candidate) cTable.getValueAt(i, 0);
            if ((c.NEOCP) && ((Boolean)cTable.getValueAt(i,1))) {
                 query+="&obj="+c.name;
                 if (15<count++) {break;}
            }
        }
         
        // set up string with number of hours from now to start of missions
        DateTime now = new DateTime();
        now.setNow();
        String shrs = String.valueOf( Math.round((tFrom.julian - now.julian)*24));
        
        // append start time and obs code
        query+="&start="+shrs;
        query+="&obscode="+obs;
        // followed by the rest of the query text */
        query+=queryPart2;
        
        // now try to run the query using the default browser */
        try {
            URI uri = new URI(query);
            Desktop.getDesktop().browse(uri);
        } catch (URISyntaxException ex) {
            Logger.getLogger(MPCQueryDetail.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
   
}
