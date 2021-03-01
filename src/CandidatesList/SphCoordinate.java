
package CandidatesList; 

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

/***********************************************************************************
 * Class SphCoordinate represents a 2D coordinate on the surface of a sphere 
 * (Spherical Coordinate). It may Longitude and Latitude, Ecliptic Longitude and 
 * Latitude or Equatorial RA and Dec.
 * 
 * Methods are provided to transform from ecliptic to equatorial coordinates and to 
 * calculate rise/set hour angles.
 * 
 * The coordinates are normally stored in radians but methods are provided to obtain 
 * degrees or hours for specific purposes and to input from typical string 
 * forms of RA and Dec.
 *
 * @author Tony Evans
 */
public class SphCoordinate implements Serializable  {
    /* Serializable so objects can be writtten to a file (as part of a Candidate). */
    
    public double[] coord = {0.0,0.0};                            // The coordinates
    
    private static final double toHours = 12/Math.PI;             // Radians to hours factor
    private static final double eps = 23.4373*(Math.PI/180);      // Obliquity of ecliptic
    private static final double gpRA = 3.366;                     // Galactic pole RA (radians)
    private static final double gpDec = 0.4734;                   // Galactic pole Dec(radians)
    private static final double pi2 = 2 * Math.PI;
    private static final DecimalFormat dd = new DecimalFormat("00");
    private static final DecimalFormat ddpd = new DecimalFormat("00.0");
    private static final DecimalFormat ddpdd = new DecimalFormat("00.00");
    
    /**
     * ´Constructor creates an empty coordinate.
     */
    public SphCoordinate() {                                
        coord[0]=0.0;
        coord[1]=0.0;
    }
    /**
     * Create a coordinate from RA and Dec.
     * @param x RA in radians.
     * @param y Dec in radians.
     */
    public SphCoordinate(double x, double y) {              
        coord[0]=x;
        coord[1]=y;
    }
    /**
     * Set the coordinate from a string.
     * @param s String in the form hh mm ss.s +dd mm ss.s
     */
    public SphCoordinate(String s) {                          
         StringTokenizer st = new StringTokenizer(s," ");
         coord[0] = Util.s2d(st.nextToken(),0);
         coord[0]+= Util.s2d(st.nextToken(),0)/60;
         coord[0]+= Util.s2d(st.nextToken(),0)/3600;
         coord[0]=Math.toRadians(15*coord[0]);
         String deg = st.nextToken();
         coord[1] = Math.abs(Util.s2d(deg, 0));
         coord[1]+= Util.s2d(st.nextToken(),0)/60;
         coord[1]+= Util.s2d(st.nextToken(),0)/3600;
         coord[1]=Math.toRadians(coord[1]);
         if (deg.startsWith("-")) {coord[1]=-coord[1];}
    }
    
    /**
     * Calculate the sky angle between this coordinate and an object at coordinate c. 
     * @param c The coordinates of the other position.
     * @return The sky angle in radians between this coordinate and coordinate c.
     */
    public double getAngle(SphCoordinate c) {
        return Math.acos(Math.sin(c.coord[1])*Math.sin(coord[1])
            + Math.cos(c.coord[1])*Math.cos(coord[1])*Math.cos(c.coord[0]-coord[0]));
    }
    
    /**
     * Get the offset in hours from the Longitude or RA.
     * @return The RA or Longitude expressed as hours.
     */
    public double getHours() {
        return coord[0]*toHours;
    }
    
    /**
     * Set the coordinates explicitly in radians. 
     * @param x Longitude or RA expressed in radians.
     * @param y Latitude or Decl expressed in radians.
     */
    public void setCoords(double x, double y) {
        coord[0] = x;
        coord[1] = y;
    }
    
    /**
     * Transform this coordinate from ecliptic to equatorial. From Fundamental Astronomy, 
     * Karttunen, etal 5th ed. 2003, Springer.
     */
    public SphCoordinate getEquatorial() {
        SphCoordinate eq = new SphCoordinate();
        eq.coord[1]=Math.asin(Math.sin(coord[1])*Math.cos(eps) + Math.cos(coord[1])*Math.sin(eps)*Math.sin(coord[0]));    
        double sinRA = (Math.cos(coord[1])*Math.cos(eps)*Math.sin(coord[0])-Math.sin(coord[1])*Math.sin(eps))/Math.cos(eq.coord[1]);
        double cosRA = (Math.cos(coord[0])*Math.cos(coord[1]))/Math.cos(eq.coord[1]);
        eq.coord[0] = Math.atan2(sinRA,cosRA);
        if (eq.coord[0]<0) {eq.coord[0]+=pi2;}
        return eq;
    }
    
    /**
     * If this coordinate is the geographic position of an observatory, calculate time either side of 
     * meridian an object with equatorial coordinates p remains above a horizon of altitude alt degrees.
     * @param p Coordinate of object.
     * @param alt Altitude above horizon the object becomes observable.
     * @return Number of hours object is above observable horizon either side of meridian.
     */
    public double riseTime(SphCoordinate p, double alt) {
        // calculate hours either side of meridian the object is visible above altitude  alt
        double zRad = Math.toRadians(alt);       
        double HARad = Math.acos((Math.sin(zRad)-Math.sin(p.coord[1])*Math.sin(coord[1]))/(Math.cos(p.coord[1])*Math.cos(coord[1])));
        return Math.toDegrees(HARad)/15;
        
    }
    
    /**
     * If this coordinate is the RA and Dec of a position then return the midpoint between it and point
     * p on the celestial sphere. From https://answers.yahoo.com/question/index?qid=20081211074044AA2G9aK
     * @param p Another coordinate.
     * @return Coordinate mid way between this coordinate and p.
     */
    public SphCoordinate getMiddle(SphCoordinate p) {
        SphCoordinate m = new SphCoordinate();
        double Bx = Math.cos(p.coord[1]) * Math.cos(p.coord[0] - coord[0]); 
        double By = Math.cos(p.coord[1]) * Math.sin(p.coord[0] - coord[0]);
        m.coord[1] = Math.atan2(Math.sin(coord[1]) + Math.sin(p.coord[1]),
          Math.sqrt((Math.cos(coord[1]) + Bx)*(Math.cos(coord[1]) + Bx)+ (By*By))); 
        m.coord[0] = coord[0] + Math.atan2(By, Math.cos(coord[1]) + Bx);
        return m;
    }
    
    /**
     * If this coordinate is the RA and Dec of an object, return the galactic latitude in degrees.
     * @return Galactic coordinates equivalent to this ecliptic coordinate.
     */
    public double galLat(){
        double d = Math.sin(gpDec)*Math.sin(coord[1])+Math.cos(gpDec)*Math.cos(coord[1])*Math.cos(coord[0]-gpRA);
        return Math.toDegrees(Math.asin(d));
    }
    
    /**
     * Returns the coordinates as RA and Dec in a String - mainly for testing.
     * @return This coordinate expressed as a String.
     */
    public String getRADec() {
        double ra = Math.toDegrees(coord[0]/15);
        int h = (int) ra;
        ra=(ra-h)*60;
        int m = (int) ra;
        double s = (ra-m)*60;
        String r = dd.format(h)+" "+dd.format(m)+" "+ddpdd.format(s)+" ";
        ra = Math.abs(Math.toDegrees(coord[1]));
         h = (int) ra;
        ra=(ra-h)*60;
         m = (int) ra;
         s = (ra-m)*60;
         if (coord[1]<0) {r+="-";} else {r+="+";}
         r+= dd.format(h)+" "+dd.format(m)+" "+ddpd.format(s);
        return r;
    }
    
    /**
     * Returns a SphCoordinate being the point distant d (radians) from this point along 
     * bearing b (radians clockwise from north).
     * Based on http://www.movable-type.co.uk/scripts/latlong.html
     * @param d Distance in radians across the sky.
     * @param b Bearing (direction) radians clockwise from 0=north.
     * @return Coordinates of object d radians from this coordinate in direction b.
     */
    public SphCoordinate findCoord(double d, double b) {
        SphCoordinate r = new SphCoordinate();
        r.coord[1]=Math.asin( Math.sin(coord[1])*Math.cos(d) + Math.cos(coord[1])*Math.sin(d)*Math.cos(b) );
        r.coord[0]=coord[0] - Math.atan2(Math.sin(b)*Math.sin(d)*Math.cos(coord[1]),
                Math.cos(d)-Math.sin(coord[1])*Math.sin(r.coord[1]));
        r.coord[0]=(r.coord[0]+2*Math.PI)%(2*Math.PI);
        return r;
    } 
}
