package CandidatesList;
import java.io.Serializable;
import java.text.DecimalFormat;

/**************************************************************************************
 * Class Candidate represents an individual asteroid, a potential candidate
 * for observation. Includes methods to calculate RA/Dec position and rise, meridian 
 * and set time.
 *
 * @author Tony Evans
 **/
public class Candidate implements Serializable,Comparable {
   //Identities of various kinds. 
    public String name="";                      // Name - Proper name if it has one else provId or NEOCP id
    public String number="";                    // Number - Number if it has one
    public String provId="";                    // Provisional designation
    public String MPCORBid = "";                // Identity in cols 1-8 of MPCORB
    public String NEOCPid="";                   // Identity on NEOCP/PCCP page
   // Other properties 
    public String NEOCPdate="";                 // Date-time added/updated
    public String NEOCPScore="";                // Score from NEOCPP
    public DateTime lastObs = new DateTime();   // Date of last observation
    public String ops="";                       // Code for number of oppositions
    public String orbitName="";                 // Name of this type of orbit (Apollo, Aten etc)
    public String spgPri="";                    // ESA Spaceguard priority code
    public String closeDate;                    // ESA Date of close approach
    public double closeDist = 0.0;              // ESA Close approach distance
    public double closeMag = 0.0;               // Magnitude at close approach
    public double diameter = 0.0;               // Estimated diameter
    public Boolean NEOCP = false;               // is it a NEOCP/PCCP?
    public Boolean comet = false;               // is it a comet (or potential comet)?
    public Boolean VI=false;                    // Is it a virtual impactor?
    public Boolean NEO=false;                   // Is it a Near Earth Object?
    public Boolean PMD=false;                   // Is it a potential mission destination
    public Boolean PHA=false;                   // Is it a Potentially Hazardous Object?
    public Boolean kmplus=false;                // Is it 1km+ in size?
    public SphCoordinate position = new SphCoordinate();  // RA and Dec
    public float motion = 0;                    // Current angular motion rate
    public double Hmag = 0;                     // Absolute magnitude
    public double Vmag = 0;                     // Current apparent magnitude 
    public String dVmag=" ";                    // Magnitude rising or falling indicator or N/T indicator for a comet
    public double bestAlt = 0;                  // Current highest altitude (across meridian tonight)
    public DateTime ctMeridian = new DateTime();// Time across meridian 
    public double HAHrs = 0;                    // Hours either side of meridian object is visible 
    public DateTime ctRise = new DateTime();    // Time rises above altitude limit
    public DateTime ctSet = new DateTime();     // Time sets below altitude limit
    public int moonAngle =0;                    // Angular separation from moon
    public int arc = 0;                         // Nmber of days of arc observed if 1-Op object
    public String uncertainty = " ";            // Orbital uncertainty  parameter
    public float  punc=0;                       // Positional uncerainty
    public Boolean select = false;              // Is this selected in the table(SEL)
    public String packNo = " ";                 // MPC Packed number
    public String packDes = "  ";               // MPC Packed designation

    public String SAO ="xx";                    // Sormano list type (Priority or Encounter)
    public Elements elements = new Elements();  // Orbital elements  
    
    /** Orbital Elements are defined as an inner class of Candidate */
    public class Elements implements Serializable  {
         DateTime Epoch = new DateTime();        // Epoch
         DateTime TT = new DateTime();           // Time of perihelion passage 
         Double M = 0.0;                         // Mean anomaly
         Double i = 0.0;                         // Inclination
         Double N = 0.0;                         // Longitude of ascending node
         Double w = 0.0;                         // Argument of perihelion
         Double a = 0.0;                         // Semimajor axis
         Double e = 0.0;                         // Eccentricity
         Double q = 0.0;                         // Perihelion disance
         Double n = 0.0;                         // Mean motion (rads per day)
      }

    /* decimal formating patterns for the display of data elements */
    private transient static final DecimalFormat dddd  = new DecimalFormat("0000");
    private transient static final DecimalFormat ddddd = new DecimalFormat("00000");
    
    /* strings used to translate MPC packed formats */
    private transient static final char[] 
       codes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private transient static final char[] yrcodes = "IJK".toCharArray();

    /**
     * Calculate position, ctMeridian, ctRise and ctSet times, altitude and motion of 
     * this candidate at observatory ob at the specified date-time.
     * @param minAlt Minimum altitude above horizon at meridian (degrees).
     * @param ob The observatory from which the object is viewed.
     * @param sun The Sun object.
     * @param moon The Moon object.
     * @param earth The Earth object.
     */
    public void setTimes(int minAlt, Observatories ob, Sun sun, Moon moon, Earth earth) { 
        
        // get the sidereal time offset (LMST-UT) for this observatory at tMidnight.
        double dTObs = ob.getLMSTOffset(ob.tMidnight);
       
        // Objects with available orbital elements and eccentricity below (Util.parabolic) have their positions
        // recalculated itteratively. Meridian crossing time is estimated first from the position at midnight
        // then at the position at the estimated meridian crossing time. This should get within a few minutes. 
        // exclude objects we can't calc positions for like NEOCPs
        
        SphCoordinate pMid = new SphCoordinate();
        if (!NEOCP) { 
            // establish position at midnight
            pMid = doPosition(ob, ob.tMidnight, sun, earth);   
            
            // establish what time that position passes meridian
            double merid = (24+position.getHours()-dTObs)%24;                   // meridian time (hours) 
            ctMeridian.setDate(ob.tNow.julian);                                 // meridian date
            ctMeridian.setTime(merid/24);                                       // meridial time
            
            // crossing time must <12hrs before sunset else add a day 
            if ((ob.tSet.julian-ctMeridian.julian)>0.5) {ctMeridian.add(1.0);} 
            
            // establish RA/Decl position at the time of crossing the meridian
            position = doPosition(ob, ctMeridian, sun, earth); 
            
            // derive motion from distance moved between midnight and crossing meridian 
            double dt = Math.abs(ctMeridian.julian - ob.tMidnight.julian);      // time from midniht to meridian
            double da = position.getAngle(pMid);                                // movement during time dt
            motion = (float) ((float) 143.2*da/dt);                             // rate of motion
        }
       
        // meridian time is (re) estimated based on the (new) position and must be no more than 
        //12 hrs before sunset
        double merid = (24+position.getHours()-dTObs)%24;
        ctMeridian.setDate(ob.tNow.julian);
        ctMeridian.setTime(merid/24);
        if ((ob.tSet.julian-ctMeridian.julian)>0.5) {ctMeridian.add(1.0);}
        
        // get angular separation of Moon at Meridian passage time (or from given position for comets and NEOCPs)
        if (NEOCP || comet) {moonAngle = (int) moon.getAngle(position, ctMeridian, ob);} 
        else {moonAngle = (int) moon.getAngle(pMid, ctMeridian, ob);}
        
        // Altitude is max at meridian and is fuction of object Dec and observatory latitude
        double alt = Math.toDegrees((Math.PI/2 - ob.position.coord[1]) + this.position.coord[1]);
        if (alt>90) {alt = 180-alt;}
        bestAlt = alt;
        
        // get hour angle either side of meridian the object is visible. Set 12h for circumpolar
        HAHrs = ob.position.riseTime(position, minAlt);
        if (Double.isNaN(HAHrs)) {HAHrs = 12;}
        
        // establish ctSet and ctRise times HA hours either side of meridian. Set time must be in future.
        ctSet.setDate(ctMeridian.julian + HAHrs/24);
        ctRise.setDate(ctSet.julian - HAHrs/12);
       
    } 
    
    /**
     * The position of the this candidate is calculated from its orbital elements using a simplified
     * algorithm based on work by Paul Schylter at http://www.stjarnhimlen.se/comp/ppcomp.html. 
     * 
     * Positions are calculated at "equinox of date" as it is assumed the user will use MPC or JPL to get 
     * precise coordinates before setting up the mission. J2000 topocentric positions could be calculated if justified.
     */
     private SphCoordinate doPosition(Observatories ob, DateTime dt, Sun sun, Earth earth) {
        double v,r;
        double N = elements.N;  
        double w = elements.w;   
        double i = elements.i;  
        
        // different calcs depending on whether the orbit is considered elliptical or hyperbolic */
        if (elements.e<Util.parabolic) {
        
            /* prepare all the elements*/
            double d = dt.julian - elements.Epoch.julian;        // time since epoch                
            double a = elements.a;                          
            double e = elements.e;                          
            double M = elements.M + d*elements.n;                // mean anomaly at time dt  
                                 
            // obtain Eccentric anomaly using Newton Raphson method to get E
            double E = calcE(e,M);
       
            // calculate rectangular coordinates in plane of the objects orbit
            double x = a*(Math.cos(E) - e);
            double y = a* Math.sin(E) * Math.sqrt(1 - e*e);
        
            // calculate heliocentric distance and true anomaly i.e. position in the plane of the orbit
            r = Math.sqrt(x*x + y*y);
            v = Math.atan2(y, x );
            
        } else {
            
            double d = dt.julian - elements.TT.julian;          // time since perihelion
            double e = elements.e;
            double q = elements.q;           
            
            double a = 0.75 * d * Util.k * Math.sqrt( (1 + e) / (q*q*q) );
            double b = Math.sqrt( 1 + a*a );
            double W = Math.pow(b+a,1/3.0) - Math.pow(b - a, 1/3.0);
            double f = (1 - e) / (1 + e);

            double a1 = (2/3) + (2/5) * W*W;
            double a2 = (7/5) + (33/35) * W*W + (37/175) * W*W*W*W;
            double a3 = W*W * ( (432/175) + (956/1125) * W*W + (84/1575) * W*W*W*W);
             
            double C = W*W / (1 + W*W);
            double g = f * C*C;
            double w1 = W * ( 1 + f * C * ( a1 + a2*g + a3*g*g ) );

            v = 2 * Math.atan(w1);
            r = q * ( 1 + w1*w1 ) / ( 1 + w1*w1 * f );
            
        }
        
        // calculate heliocentric ecliptic coordinates */
        double xeclip = r * ( Math.cos(N) * Math.cos(v+w) - Math.sin(N) * Math.sin(v+w) * Math.cos(i) );
        double yeclip = r * ( Math.sin(N) * Math.cos(v+w) + Math.cos(N) * Math.sin(v+w) * Math.cos(i) );
        double zeclip = r * Math.sin(v+w) * Math.sin(i);
       
        // get ecliptic long and lat
        double lonecl = Math.toDegrees(Math.atan2(yeclip, xeclip ));
        double latecl = Math.toDegrees(Math.atan2( zeclip, Math.sqrt(xeclip*xeclip+yeclip*yeclip) ));
        
        // calculate phase angle (needed in magnitude calc if implemented) */
        SphCoordinate s = sun.getPosition(dt);  // this is just to force the Sun to recalculate its position at dt
        double pv = Math.acos(Math.cos(sun.ecliptic.coord[0] - lonecl)*Math.cos(latecl));
        
        // calculate geocentric ecliptic coordinates and distance from Earth (delta)
        earth.setPosition(dt);
        double xgeo = xeclip-earth.x;
        double ygeo = yeclip-earth.y;
        double zgeo = zeclip-earth.z;
        
        // distance from Earth (for use in magnitude calc if implemented)
        //double delta = Math.sqrt(xgeo*xgeo+ygeo*ygeo+zgeo*zgeo);
        
        // calculate geocentric longitude and latitude 
        double longeo = Math.atan2(ygeo, xgeo);
        double latgeo = Math.atan2(zgeo,Math.sqrt(xgeo*xgeo+ygeo*ygeo));
        double rgeo = Math.sqrt(xgeo*xgeo+ygeo*ygeo+zgeo*zgeo);   
        
        // establish ecliptic coordinates and convert to topocentric equatorial */ 
        SphCoordinate ecliptic = new SphCoordinate(longeo, latgeo);
        SphCoordinate equitorial = ecliptic.getEquatorial();
        SphCoordinate topocentric = ob.getTopocentric(equitorial, dt, rgeo);
  
        return  topocentric;      
    }
     
     /**
      * Calculate the eccentric anomaly E from the mean anomaly M using Newton-Raphson method 
      * (see Boulet 1991)
      * @param de Eccentricity.
      * @param dM Mean anomaly.
      * @return  Eccentric anomaly
      */
     public double calcE(double de,double dM) {
        double dE = dM;
        while (Math.abs(fnf(de, dE, dM))>1e-8) {dE=dE-fnf(de,dE,dM)/fndf(de,dE);}
        return dE;
     }
        private double fnf(double de, double dE, double dM ) {return dE-de*Math.sin(dE)-dM;}
        private double fndf(double de, double dE) {return 1.0- de*Math.cos(dE);} 
     
     /**
      * calculate the Phi1 and Phi2 functions associate with magnitude phase curve (Bowell 1989) 
      * These are included here but are not currently used.
      */
     private double phi1(double pv) {
         double sina = Math.sin(pv);
         double tana2= Math.tan(pv/2);
         double p1s = 1- (0.986*sina)/(0.119 + 1.341*sina - 0.754*sina*sina);
         double p1l = Math.exp(-3.332*Math.pow(tana2, 0.631));
         double W = Math.exp(-90.56*tana2*tana2);
         return W*p1s+(1-W)*p1l ;
     }
     private double phi2(double pv) {
         double sina = Math.sin(pv);
         double tana2= Math.tan(pv/2);
         double p2s = 1- (0.238*sina)/(0.119 + 1.341*sina - 0.754*sina*sina);
         double p2l = Math.exp(-1.862*Math.pow(tana2, 1.218));
         double W = Math.exp(-90.56*tana2*tana2);
         return W*p2s+(1-W)*p2l ;
     }
     
   /**
    * Overrides toString to provide number=designation or NEOCPId.
    **/
    @Override
    public String toString() {
        if (number.isEmpty()) {
            return name;
        } else {
            return number; 
        }
    }
    
    /**
     * Make MPC-style packed formats for a number.
     * @return MPC-style packed format number.
     */
    public String packNumber() {
        // if there is no number return empty string
        if (number.isEmpty()) {
            return "";
        } else {
            // if there is a number strip off the brackets and if length>5 pack it 
            String n = number.replace("(","");
            n=n.replace(")","");
            int i = Integer.parseInt(n);
            if (i>99999) {
                n = ""+codes[-10+(int) i/10000];
                n = n+dddd.format(i%10000)+"  ";
            } else {
                n = ddddd.format(i)+"  ";
            }
            return n;
        }
    }
    
    /***
     * Make MPC-style packed formats for a provisional designation.
     * @return MPC-style packed designation.
     */ 
    public String packName() {
        // if name is empty return empty sring
        if (name.isEmpty()) {return ""; }
        if (name.startsWith("(")) {return "";}
        
        // get century. Must be 19 or 20
        String cent = name.substring(0,2);
        if (!(cent.equals("19") || cent.equals("20"))) {return "";}
        
        // get century code and year number
        int i = Integer.parseInt(cent);              
        String n = yrcodes[i-18]+name.substring(2,4)+ name.substring(5,6)+"xx"+name.substring(6,7);
        
        // assemble remainder of coded designation
        String n2=name.substring(7);
        switch (n2.length()) {
            case 0: n=n.replace("xx","00");
                    break;
            case 1: n=n.replace("xx", "0"+n2);
                    break;
            case 2: n=n.replace("xx", n2);
                    break;
            case 3: i = Integer.parseInt(n2.substring(0,2))-10;
                    n=n.replace("xx",codes[i]+n2.substring(2));
        }
        return n;
    }
    
    /**
     * Make an identity that will match the id in MPCORB. This does not account for some old
     * survey designation schemes.
     */
    public void makeMPCOrbid() {
        if (number.isEmpty()) {
            MPCORBid = packDes;
        } else {
            MPCORBid = packNo;
        }
    }
    
    /**
     * Comparable is implemented by comparing MPCORBid. Sort to MPCORB sub section sequence.
     * @param c Object to be compared to.
     **/
    @Override
    public int compareTo(Object c) {
        Candidate c1 = (Candidate) c;
        return MPCORBid.compareTo(c1.MPCORBid);
    }
}