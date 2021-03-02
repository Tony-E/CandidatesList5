package CandidatesList;

/**
 * Class Observatories contains positional information about observatories.
 * The observatory object maintains the time stamps that define the observing 
 * session being displayed.
 * <ul>
 * <li>tNow       is the current time </li>
 * <li>tSet       is sunset at the start of the session </li>
 * <li>tRise      is sunrise at the end of the session, which must be in the future </li>
 * <li>tMidnight  is half way between sunset and sunrise </li>
 * </ul>
 * 
 * @author Tony Evans
 */

public class Observatories {
   /**
    * Observatory constants: Longitudes and Geodetic sines and cosines are from ObsCodes.htm.
    */
    private static final String[] codes = {"G40", "W88"};
    private static final String[] names = {"Slooh Teide", "Slooh Chile"};
    private static final float[]  lats =  {28.3f, -33.26f};
    private static final float[]  longs = {343.491740f, 289.46570f};
    private static final double[] Gsin =  {+0.471441, -0.545574};
    private static final double[] Gcos =  {+0.881470,  0.837136};
    
    // the default filter altitudes and magnitudes for each bservatory
    private static final int[]    alts  = {35,35};
    private static final float[]  mags  = {19.5f, 19.5f};
   
    // the number of observatories 
    public int no;
    
    private static final double pi2 = Math.PI*2;
    
    // These are the values for the currently selected observatory   
    public SphCoordinate position = new SphCoordinate();     // Longiude & Latitude
    public String    name = "undefined";                     // name
    public String    code = "???";                           // code
    public double    gSin = 0.0;                             // Geodetic sine
    public double    gCos = 0.0;                             // Geodetic cosine
    public DateTime  tRise;                                  // sunrise is end of observing period
    public DateTime  tMidnight;                              // tMidnight at the observatory
    public DateTime  tSet;                                   // sunset is start of observing period
    public int       obsAlt = 0;                             // Default minimum altitude.
    public float     obsMag = 0;                             // Default maximum magnitude.
    public DateTime  tNow;                                   // Current time UT at the observaory

    /**
     * Constructor initialises dates and current time.
     */
    public Observatories() {
        tNow = new DateTime();
        tNow.setNow();
        tRise = new DateTime();
        tSet = new DateTime();
        tMidnight = new DateTime();
        no = codes.length;
    }
    
    /**
     * Set the observatory variables based on an index.
     * @param index Observatory number.
     */
    public void setObservatory(int index) {
        position.coord[0]=pi2*longs[index]/360;
        position.coord[1]=pi2*lats[index]/360;
        gSin = Gsin[index];
        gCos = Gcos[index];
        name = names[index];
        code = codes[index];  
        obsMag = mags[index];
        obsAlt = alts[index];
    }
    
    /**
     * Supply the code and name of an Observatory specified by an index.
     * @param i Observatory number-
     */
    public String getName(int i) {
        return codes[i]+"-"+names[i];
    }
    
    /**
     * Set up the times that govern the start and end of the next observing session. 
     * The objective is to establish sunset and sunrise as boundaries of the next observing session. 
     * Sunrise must be in the future. If sunset is in the past it is already night at the observatory
     * and the observing session is already started. 
     * 
     * Improvement - sunrise is today's sunrise + 24 hrs which is not quite accurate if 
     * tnow is daytime at the observatory.  
     * @param s The Sun.
     * @param addDay If true look 24hrs later.
     * @param horzn Twilight setting- 
     */
    public void setTime(Sun s, Boolean addDay, int horzn) {
        // set tNow to the current date/time UT (GMT) or +1 day if addday is specified
          tNow.setNow();
          if (addDay) {tNow.add(1.0);}
          
         // get position of Sun at tNow 
         SphCoordinate sunPosition = s.getPosition(tNow);   
         
         // get offset of siderial time to UT (hrs) 
         double dTObs = getLMSTOffset(tNow); 
         
         // get hour angle offset of rise and set at astronomical twilight 
         double dHA = position.riseTime(sunPosition, horzn);  
         
         // set sunrise and sunset times 
         tRise.setNow();
         if (addDay) {tRise.add(1.0);}
         tRise.setTime((sunPosition.getHours()-dTObs-dHA)/24);
         
         // its the next sunrise we are interested in 
         while (tRise.julian<tNow.julian) {tRise.add(1.0);}
         
         // sunset is the sunset immediately prior to the sunrise 
         tSet.setDate(tRise.julian-(24 - 2*dHA)/24);
         
         // tMidnight is half way through 
         tMidnight.setDate(0.5*(tSet.julian + tRise.julian));        
    }
    
    /**
     * Supply the Local Mean Sidereal Time (radians) at this observatory at a 
     * specified date-time dt. (from Boulet 2.4 but using radians). 
     */
    public double getLMST(DateTime dt) {
        double J0 =  0.5+Math.floor(dt.julian-0.5);
        double J = (J0 - 2451545.0)/36525;
        double GMST0 = (1.75336856 + 628.3319705*J + 6.77071E-06 * J*J);
        double GMST = (GMST0 +6.300388097*((dt.julian+0.5)%1));
        double LMST = (GMST + position.coord[0])%pi2;
        return LMST;
    }
    public double getLMSTOffset(DateTime dt) {
        double J0 =  0.5+Math.floor(dt.julian-0.5);
        double J = (J0 - 2451545.0)/36525;
        double GMST0 = (1.75336856 + 628.3319705*J + 6.77071E-06 * J*J);
        double GMST = (GMST0 +6.300388097*((dt.julian+0.5)%1));
        double LMST = (GMST + position.coord[0])%pi2;
        double LMSTOff = LMST - pi2*((dt.julian+0.5)%1);
        return (LMSTOff*24/pi2)%24;
    }
   
    /**
     * given the geocentric equatorial coordinates of an object and its distance, supply its 
     * topocentric coordinates as seen from this observatory at a specified time.
     * (from Boulet 2.8 but using radians).
     * @param equatorial Equatorial geocentric coordinates of object.
     * @param dt DateTime of observation.
     * @param distance Distance of object from geocentre.
     * @return Topocentric coordinates.
     */
    public SphCoordinate getTopocentric(SphCoordinate equatorial, DateTime dt, double distance) {
        SphCoordinate topocentric = new SphCoordinate();
        double parallax = (4.26345E-5)/distance;
        double ha = getLMST(dt) - equatorial.coord[0];
        topocentric.coord[0]=equatorial.coord[0]-parallax*distance*gCos*Math.sin(ha)/Math.cos(equatorial.coord[1]);
        topocentric.coord[1]=equatorial.coord[1]-parallax*distance*(gSin*Math.cos(equatorial.coord[1])
                -gCos*Math.cos(ha)*Math.sin(equatorial.coord[1]));
        return topocentric;
    }
}
