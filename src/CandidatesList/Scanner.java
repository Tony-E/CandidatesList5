package CandidatesList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/****************************************************************************************
 * Class Scanner executes as a swingWorker to download each of the sources of information
 *   using an execution thread separate from the GUI. It can obtain candidates from:
 *
 *             Dates of Last Observation of Unusual Minor Planets list(inc NEOs)
 *             Dates of Last Observation of Distant Objects list
 *             NEOCP and PCCP lists
 *             ESA Priority list and approach/departure lists
 *             Sormano Observatory list of interesting objects
 *
 *          The Scanner will also:
 *             Extract object and orbit type information from the local MPCORB.DAT file
 *             Download and extract object and orbit type information from the NEAp01.txt file.
 *
 *          Progress as a percentage of each download or scan is published
 *          as work continues and messages are published reporting on activities.
 *
 * @author Tony Evans
 */
 public class Scanner extends SwingWorker <Integer, String>  {

    // Declared int values for the names of the various sources
    public final static int NEOCP =      0;
    public final static int PCCP  =      1;
    public final static int Priority =   2;
    public final static int NEA   =      3;
    public final static int Unusual =    4;
    public final static int MPCORB =     5;
    public final static int Recover =    6;
    public final static int Distant =    8;
    public final static int Auto =      10;
    public final static int PMD =       11;
    public final static int Critlist =  12;
    public final static int Desire =    13;
    public final static int Sormano =   14;

      // web addresses of the sources
    private final static String NEOsList =    "https://www.minorplanetcenter.net/iau/NEO/LastObsNEO.txt";
    private final static String NEOCPList =   "https://www.minorplanetcenter.net/iau/NEO/neocp.txt";
    private final static String PCCPList =    "https://www.minorplanetcenter.net/iau/NEO/pccp.txt";
    private final static String UnusualsList ="https://www.minorplanetcenter.net/iau/lists/LastUnusual.html";
    private final static String DistantList = "https://www.minorplanetcenter.net/iau/TNO/LastObsTNO.html";
    private final static String NEOOrb =      "https://www.minorplanetcenter.net/iau/MPCORB/NEAp01.txt";
    private final static String SormanoTxt =  "http://www.brera.mi.astro.it/sormano/Observable.txt";
      
      // ESA file grabber addresses
    private final static String Baseurl =     "https://neo.ssa.esa.int/PSDB-portlet/download?file=";
    private final static String RiskList =    "esa_risk_list";
    private final static String PriList =     "esa_priority_neo_list";
    private final static String Recent =      "esa_recent_close_app";
    private final static String Upcoming =    "esa_upcoming_close_app";

    // orbit types encoded in MPCORB and NEAp01.txt records 
    private final static String orbTypes[] = {"MBA","Atira","Aten", "Apollo", "Amor","Mars Xer","Hungaria","Undef","Hilda"
        ,"J-Trojan","Distant","Undef.","Undef.","Undef.","Plutino","Other TNO","Cubewano","SDO"};

    // working variables 
    private int source = 0;                            // which souce to scan
    private final JTextArea msgText;                   // message area in GUI
    public int progress = 0;                           // progress indicator
    private String data = "";                          // input area for data downloaded
    private int fLength = 0;                           // length of file to download
    private float limit = 0.0f;                        // limiting magnitude for downloads
    private CandidatesFile candidatesFile;             // Pointer to candidates list file.
    private String MPCOrbFile = "";                    // Path and name of local MPCORB.DAT

    // Spaceguard priority codes
    private static final String[] SPGpricodes = {"Urg.", "Need", "Use.", "Low", "?"};
 
    /**
     * Constructor stores pointer to message area.
     * @param msg Text area in the GUI in which messages can be written.
     */
    public Scanner(final JTextArea msg) {
    this.msgText=msg;
    }

    /**
     * Set source, limiting magnitude, candidates list file handler, MPCORB file path and query .
     * @param s source
     */
    public void setSource(int s) {source = s;}
    public void setLimit(float l) {limit = l;}
    public void setFile(CandidatesFile f) {candidatesFile = f;}
    public void setMPC(String f) {MPCOrbFile = f;}

    /**
     * Execute the scanner. 
     */
    @Override
    public Integer doInBackground() {
        // Select the methods according to the requested source.
        try {
            progress = 0;
            switch (source) {
                case NEOCP:    doNEOCP();
                               doPCCP();
                               break;
                case Priority: doPriority(); 
                               doRisks();
                               doApproach();
                               break;
                case Unusual:  doUnusual(); 
                               break;
                case MPCORB:   doMPCOrb();
                               doNEAOrb();
                               break;
                case Distant:  doDistant();       
                               break;
                case Sormano:  doSormano();
                               break;
                case Auto: /* This is the sequence of actions for Refresh. */
                               doNEOCP();
                               doPCCP();
                               doUnusual();
                               doDistant();
                               doPriority(); 
                               doRisks();
                               doApproach();
                               doSormano();
                               doMPCOrb();
                               doNEAOrb();
            }
            return 0;    
        } catch (IOException ex) {
            publish("Error encountered: " + ex.getMessage() );
            return -1;
        }
    }
    
    /**
     * A swingWorker method to take the published messages and output them to the 
     * message area in the GUI.
     * @param chunks Chunks of text to send to message area.
     */
    @Override
    protected void process(List<String> chunks) {
        for (String str : chunks) {
            msgText.append(str);
            msgText.append("\n");
        }
    }
    
    /**
     * A swingWorker method at completion of progress. 999 signals progress timer to quit.
     */
    @Override
    protected void done() {
        progress = 999;
    }
    
    /**
     * Download the specified file and return number of bytes read.
     * @param from URL of source in String form.
     * @return Number of bytes downloaded.
     */
    private int download(String from) {
        int count = 0;
        InputStream in;
        try {
            /* Set up connection with http or https */
            URL url = new URL(from);
            if (from.startsWith("https")) {
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                in = con.getInputStream();
                fLength = con.getContentLength();           // get its length
            } else {
                URLConnection con = url.openConnection();
                in = con.getInputStream();    
                fLength = con.getContentLength();           // get its length
            }
            BufferedInputStream b = new BufferedInputStream(in);
            if (fLength<1) {fLength = 2500000;}         // some files do not reurn a length, make assumption

            /* download */
            byte[] buffer = new byte[2048];
            int charsRead;
            while((charsRead = b.read(buffer,0,2048)) != -1) {
                data+=new String(buffer,0,charsRead);
                count+=charsRead;
                progress = 100*count/fLength;
                if (progress>99) {progress=99;}
            }
        } catch(IOException e) {
            publish("Unable to connect to source of data "+source+".\n");
            return 0;
        }
        progress = 99;    // file has downloaded
        return count;
    }

    /**
     * Read NEOCP text page from the MPC. Add every object within magnitude limit.
     */
    private int doNEOCP() {
        data ="";
        int count = 0;
        int n = download(NEOCPList);
        publish(n +" Bytes downloaded from NEOCP page...");
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        while (st.hasMoreTokens()) {
            String tkn = st.nextToken();
           /* get V mag */
            float v = Util.s2f(tkn.substring(43,47),999);
           /* if V mag within limit: */
            if (!(v>limit)) {
                String nm = tkn.substring(0,8).trim();
                Candidate c = candidatesFile.find(nm,true);
                c.NEOCPScore = tkn.substring(8,12);
                c.NEOCPid = nm;
                c.ops = "";
                c.uncertainty = "~";
                c.orbitName = "NEOCP" + tkn.substring(79,82)+" obs in arc "+ tkn.substring(84,89) + "°";
                c.position.coord[0] = 15*Math.toRadians(Util.s2f(tkn.substring(26,33),999));
                c.position.coord[1] = Math.toRadians(Util.s2f(tkn.substring(34,42),999));
                
               /* extract and date added or updated */
                String dt = tkn.substring(48,70);
                dt=dt.replace("Added","A");
                dt=dt.replace("Updated", "U");
                dt=dt.replace("UT","");
                dt=dt.replace(" ", "");
                c.NEOCPdate=dt;
                
               /* extract other properties */
                c.Vmag = v;
                c.Hmag = Util.s2f(tkn.substring(90,94),99);
                c.NEOCP = true;
                c.MPCORBid="";
                count++;
            }
        }
        publish(count + " objects updated or added.\n");
        return count;
    }
    
    /**
     * Read PCCP text page from the MPC. The NEOCP text file includes the PCCP identifiers 
     * but needs to be updated from the PCCP list.
     */
    private int doPCCP() {
        data ="";
        int count = 0;
        int n = download(PCCPList);
        publish(n +" Bytes downloaded from PCCP page...");
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        while (st.hasMoreTokens()) {
            String tkn = st.nextToken();
            if (tkn.equals("\n")) {break;}     
            /* get V mag */
            float v = Util.s2f(tkn.substring(43,47),999);
            /* set PCCP flag */
            if (!(v>limit)) {
                String nm = tkn.substring(0,8).trim();
                Candidate c = candidatesFile.find(nm,true);
                String replace = c.orbitName.replace("NEOCP", "PCCP");
                c.comet=true;
                count++;
            }
        }
        publish(count + " objects updated or added.\n");
        return count;
    }
    
    /**
     * Unusual objects. This is an HTML page so we have to scrape out the useful data. 
     * "Unusual" includes NEOs.
     */
    private int doUnusual() {
        data ="";
        int count = 0;
        int n = download(UnusualsList);
        publish(n +" Bytes downloaded from Dates of Last Observation of Unusual Minor Planets...");
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        while (st.hasMoreTokens()) {
            String tkn = st.nextToken();
            /* look for lines that contain an object and work out where object data starts */
            if (tkn.startsWith("<input type=\"checkbox\" name=\"Obj\"")) {
                int k = tkn.indexOf(">");
                tkn=tkn.substring(k+1);
               /* get V mag */
                float v = Util.s2f(tkn.substring(43,47),999);
               /* if within mag limit, get name and candidate */
                if (!(v>limit)) {
                    String nm = tkn.substring(9,19).trim();

                    Candidate c = candidatesFile.find(nm,true);
                    c.Vmag = v;
                    if (nm.startsWith("(") && nm.endsWith(")")) {
                        c.number = nm;
                        c.packNo=c.packNumber();
                    }
                   /* get date of last ob and round to middle of day */
                    c.lastObs.setMPCTextDate(tkn.substring(60,72));
                    c.lastObs.julian+=0.5;
                   /* save packed formats */
                    c.packNo=c.packNumber();
                    c.packDes=c.packName();
                    c.makeMPCOrbid();
                    count++;
                }
            }
        }
        publish(count + " objects updated or added.\n");
        return count;
    }
   
    /**
     * Date of Last Observation of Distant Objects list. HTML file.
     */
    private int doDistant() {
        data ="";
        int count = 0;
        int n = download(DistantList);
        publish(n +" Bytes downloaded from Dates of Last Observation of Distant Objects..");
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        while (st.hasMoreTokens()) {
            /* search for line containing an object and work out where the object starts  */
            String tkn = st.nextToken();
            if (tkn.startsWith("<input type=\"checkbox\" name=\"Obj\"")) {
                int k = tkn.indexOf(">");
                tkn=tkn.substring(k+1);
               /* get V mag and if within limit get name and candidate */
                float v = Util.s2f(tkn.substring(43,47),999);
                if (!(v>limit)) {
                    String nm = tkn.substring(8,29).trim();
                    Candidate c = candidatesFile.find(nm,true);
                    count++;
                   /* get number */
                    c.number = tkn.substring(0,8).trim();

                   /* get rest of data items */
                    c.Vmag = v;
                    c.lastObs.setMPCTextDate(tkn.substring(54,  66));
                    c.lastObs.julian+=0.5;
                   /* save packed formats */
                    c.packNo=c.packNumber();
                    c.packDes=c.packName();
                    c.makeMPCOrbid();
                }
            }
        }
        publish(count + " objects updated or added.\n");
        return count;
    }

    /**
     * Download the ESA Priority table and update candidates with priority.
     */
    private int doPriority()  {
                 
        /* download priority list and check */
        publish("Downloading ESA Rriroity List.");
        data = "";
        int n = download(Baseurl + PriList);
        if  (n<50) {
           publish(" ESA Priority list not found!");
           return 0;
        }
        publish(n +" Bytes downloaded from ESA Priority List..");
        
        /* tokenize and extract data */
        StringTokenizer st = new StringTokenizer(data,"\n\r");  
        int count = 0;
        while (st.hasMoreTokens()) {
            String tkn = st.nextToken();
            if (tkn.length()<40) {tkn = st.nextToken();} // ignore short lines
            
            /* get V mag and process object only if not beyond limit */
            float v = Util.s2f(tkn.substring(36,40),999);
            if (!(v>limit)) {
             
               /* get name and strip out quote marks */
                String nm = tkn.substring(3,16).trim();
                nm=nm.replace("\"", "");
               /* find the candidate, quit if not available*/
                Candidate c = candidatesFile.find(nm,false);
                if(c == null) {break;}
               /* translate priority code number into words */
                c.spgPri = tkn.substring(0,1);
                int p = (int) Util.s2f(c.spgPri,4);
                c.spgPri=SPGpricodes[p];
                c.Vmag = v;
                c.packNo=c.packNumber();
                c.packDes=c.packName();
                c.makeMPCOrbid();
                count++;
            }
        }
        publish(count + " objects updated or added.\n");
        return 0;
    }
    
    /**
     * Download the ESA Risk table and update candidates with VI status.
     */
    private int doRisks() {
        /* download risk list and check */
        data="";
        publish("Downloading ESA Risk List.");
        int n = download(Baseurl + RiskList);
        if  (n<50) {
            publish(" ESA Risk list not found!");
            return 0;
        }
        publish(n + " Bytes downloaded from ESA Risk List. ");
      
        /* tokenize and extract data */
        StringTokenizer st = new StringTokenizer(data,"\n\r");  
        int count = 0;
        for (int i=0; i<4; i++) {st.nextToken();} // skip headings
        while (st.hasMoreTokens()) {
            Candidate c;
            String tkn = st.nextToken();
            if (tkn.length()<40) {continue;} // ignore short lines
            String nm = tkn.substring(0,9).trim();
            Character ch = nm.charAt(4);
            if (Character.isDigit(ch)) {
               /* it is a numbered object */
               c = candidatesFile.findN(nm);
            } else {
               /*  it is not numbered */
               nm = nm.substring(0,4)+" "+nm.substring(4);
               c = candidatesFile.find(nm,false);
            }
            /* if candidate found, add VI status */
            if (c == null) {continue;}
            c.VI = true;
            count++;
        }
        publish(count + " objects updated or added.\n");
        return 0;
     }
    
     /**
      * Fetch ESA close approach tables and extract data.
      */
     private int doApproach() {
        /* download approach list and check */
        data="";
        publish("Downloading ESA Approach & Depart Lists.");
        int n = download(Baseurl + Upcoming);
        String approach = data;
        n = n+ download(Baseurl + Recent);
        if  (n<50) {
           publish(" ESA Appproach lists not found!");
           return 0;
        }
        publish(n + " Bytes downloaded from ESA Upcoming & Recent List. ");
        approach += data;
        /* tokenize and extract data */
        StringTokenizer st = new StringTokenizer(approach,"\n\r");  
        int count = 0;
        while (st.hasMoreTokens()) {
            Candidate c;
            String tkn = st.nextToken();
            if (tkn.length()<50) continue;
            if (!Character.isDigit(tkn.charAt(0))) {continue;}
            String nm = tkn.substring(0,9);
            Character ch = nm.charAt(4);
            if (Character.isDigit(ch)) {
               /* it is a numbered object */
               c = candidatesFile.findN("("+nm.trim()+")");
            } else {
               /*  it is not numbered */
               nm=nm.trim();
               nm = nm.substring(0,4)+" "+nm.substring(4);
               c = candidatesFile.find(nm,false);
            }
            if (c == null) {continue;}
            c.closeDate = tkn.substring(29,39); 
            c.closeDist = Util.s2d(tkn.substring(63,69),99);
            c.closeMag = Util.s2d(tkn.substring(101,105),99);
            count++;
        }
        publish(count + " objects updated or added.\n");
        return 0;
    }
     
    /**
     * Scan the local MPCORB.DAT file and update object characteristics.
     * *** NOTE this does not catch all the objects unless MPCORB is correctly sorted 
     */
    private int doMPCOrb() throws FileNotFoundException, IOException {
       /* quit if nothing in the list or no MPCORB file */
        if (candidatesFile.cList.isEmpty()) {
            publish("MPCORB Scan failed - no objects to process.");
            return 0;}
        if (MPCOrbFile.isEmpty()) {
            publish("MPCORB Scan failed - no local MPCORB file defined");
            return 0;}

        int count = 0;
        int mCount = 0;
        progress = 0;
        publish("Scanning local MPCORB....");

       /* sort the candidates list based on MPCORBid */
        Collections.sort(candidatesFile.cList);

       /* define the MPCORB input file */
        BufferedReader br = new BufferedReader(new FileReader(MPCOrbFile));
        fLength = (int) 700000;

        /* Skip over front matter */
        String line = br.readLine();
        if (line == null) {return 0;}
        while (! line.startsWith("---------")) {
            line = br.readLine();
            if (line == null) {return 0;}
        }

        /* Get the first real MPC record and first candidate */
        if ((line = br.readLine()) == null) {return count;}
        Iterator<Candidate> it = candidatesFile.cList.iterator();
        Candidate c = null;
        if (it.hasNext()) {c = it.next();}

        /* run through MPCORB updating candidates where there is a match */
        while (line != null) {
            progress = (int) mCount/fLength;
            /* if there is a blank line reset the candidates to start at beginning and advance to next orbit.
             * This is because of the thee-part structure of MPCORB */
            if (line.length() == 0) {
                it = candidatesFile.cList.iterator();
                c=it.next();
                line = br.readLine();
                mCount++;
            }

            /* compare the candidate and mpcorb designations     */
            int comp = c.MPCORBid.compareTo(line.substring(0,7));

                /* if MPCORB>Candidate get next candidae. If no more candidates get next MPCORB. */
                if (comp <0) {
                    if (it.hasNext()) {c=it.next();} else {line = br.readLine(); mCount++;}
                }
                /* if MPCOB=Candidate update candidate and get next candidate and MPCORB. */
                if (comp==0){
                    doUpdate(line,c);
                    count++;
                    line = br.readLine();
                    mCount++;
                    if (it.hasNext()) {c=it.next();}
                }
                /* id MPCORB<Candidate get next MPCOrb.*/
                if (comp >0) {
                    line = br.readLine();
                    mCount++;
             }
         }
         publish(count + " objects updated from MPCORB.\n");
         return count;
    }
    
    /**
     * Download the NEAp01.txt file and update the latest NEOs with characteristics.
     * The main objective is to get current epoch orbital elements (etc) for recent
     * discoveries.
     */
    private int doNEAOrb() {
        int count =0;
        data = "";
        publish("Downloading NEAp01.txt...");
        int n = download(NEOOrb);
        publish(n+" Bytes downloaed from NEAp01.txt...");

       /* string tokenizer used to scan the NEOtom data */
        String neo;
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        if (st.hasMoreTokens()) {neo = st.nextToken();} else {return 0;}
       /* sort the candidates into designation order and get an iterator */
        Collections.sort(candidatesFile.cList);
        Iterator<Candidate> it = candidatesFile.cList.iterator();
        Candidate c = it.next();
        /* do the update as a classical master - update merge with no additions */
        while (st.hasMoreTokens()) {
            int comp = c.MPCORBid.compareTo(neo.substring(0,7));

                /* if NEOtom>Candidate get next candidate. If no more candidates quit. */
                if (comp <0) {
                    if (it.hasNext()) {c=it.next();} else {break;}
                }
                /* if NEOtom=Candidate update candidate and get next candidate and MPCORB. */
                if (comp==0){
                    doUpdate(neo,c);
                    count++;
                    if (it.hasNext()) {c=it.next();}
                    if (st.hasMoreTokens()) {neo = st.nextToken();} else {break;}
                }
                /* if NEOtom<Candidate get next NEOtom */
                if (comp >0) {
                    if (st.hasMoreTokens()) {neo = st.nextToken();} else {break;}
             }
        }
        publish(count + " objects updated.\n");
        progress = 99;
        return count;
    }
    
    /**
     * Process the priority observing list from Sormano Observatory.
     */
    private int doSormano() {
        data="";
        int count =0;
        int n=download(SormanoTxt);
        publish(n +" Bytes downloaded from Sormano... ");
        StringTokenizer st = new StringTokenizer(data,"\n\r");
        while (st.hasMoreTokens()) {
            String sorm = st.nextToken();
            if (sorm.length() >93.) {
                for (Candidate c : candidatesFile.cList) {
                    if ((sorm.substring(0,5).equals(c.MPCORBid)) || (sorm.substring(5,12).equals(c.packDes)) ) {
                        c.SAO = sorm.substring(92);
                        count++;
                    }
                }
            }
        }
        publish(count + " objects updated. \n");
        return 0;
    }
  
    /**
     * This routine extracts orbit information from the bit string in an MPCORB or 
     * NEAp01.txt record.
     * @param s String formatted according to the MPCORB format bit string.
     * @param c Candidate to which this orbit refers.
     */
    private void doUpdate(String s, Candidate c) {
        /* get H and estimated size */
        c.Hmag = Util.s2f(s.substring(8,13),99);
        c.diameter = 3551901.90501*Math.pow(10.0, -0.2*c.Hmag);
        /* get ucertainty if not already supplied */
        if (c.uncertainty.equals(" ")) {c.uncertainty = s.substring(105,106);}
        /* if still no uncertainty set default */
        if (c.uncertainty.equals(" ")) {c.uncertainty = "*";}
        /* Set 1, n or # Ops if not already supplied */
        if (c.ops.isEmpty()) {
            if (s.substring(123,126).equals("  1")) {
                c.ops="1Op";
            }else{
                if (c.number.isEmpty()) {c.ops="nOp";} else {c.ops="#Op";}
            }
        }
        /* manipulate the lower 6 bits of positions 162-165 to get an orbit type name */
        int bits = Integer.parseInt(s.substring(161,165), 16);
        int lowbits = bits & 63; // get the bottom 6 bits
        c.orbitName = orbTypes[(int)lowbits];

        /* check the upper bits for various flags */
        int b= bits & 2048;
        c.NEO = b==2048;
        b = bits & 4096;
        c.kmplus = b==4096;
        b = bits & 32768;
        c.PHA = b==32768;

        /* get orbital elements */
         c.elements.Epoch.setMPCDate(s.substring(20,25));
         c.elements.M = Math.toRadians(Util.s2d(s.substring(26,35),0));
         c.elements.w = Math.toRadians(Util.s2d(s.substring(37,46),0));
         c.elements.i = Math.toRadians(Util.s2d(s.substring(59,68),0));
         c.elements.e = Util.s2d(s.substring(70,79),0.5);
         c.elements.a = Util.s2d(s.substring(92,103),1);
         c.elements.N = Math.toRadians(Util.s2d(s.substring(48,57), 0));
         c.elements.n = Math.toRadians(Util.s2d(s.substring(80,91), 0));
    }
    
    /**
     * Publish a message.
     * @param s String text to be published-
     */
     public void doPublish(String s) {
         publish(s);
     }
     
     /**
      * Update the progress indicator.
      * @param p Progress %-
      */
     public void doProgress(int p) {
         progress = p;
     }
}
