
package CandidatesList;

/************************************************************************************
 * Class Moon is the Moon. Methods are provided to get the Phase of the Moon and 
 * its angular distance from another object.
 * 
 * Based on MiniMoon in Astronomy on the Personal Computer,  O. Montenbruck and T. Pfleger,
 * Springer, 4th Ed. 2000.
 *
 * @author Tony Evans
 */
public class Moon {
    private final double mNew = 2456688.403472;                   // Julian date of new Moon on 2014 Jan 30 21:41
    private static final double eps = 23.43929111*(Math.PI/180);  // obliquity of ecliptic
    private static final double pi2 = Math.PI*2;                  // 2 pi
    private static final double lMonth = 29.53059;                // days in a month (lunar phase)
    private static final DateTime j2000 = new DateTime();         // date of J2000
    private static final double arcs = 206264.806;                // arcseconds per radian
    private SphCoordinate ecliptic = new SphCoordinate();         // Ecliptic position of Moon (Long, Lat)
    private SphCoordinate equatorial = new SphCoordinate();       // Equatorial position of Moon
    private SphCoordinate position = new SphCoordinate();         // Equatorial position of Moon (RA, DEC)
  
    /**
     * Constructor (empty)
     */
    public Moon() {
        
    }
    
    /**
     * Calculate RA and Dec at a specified time.
     * @param dt The specified date-time
     * @param The observatory from which the observation is made.
     */

    private void doCoords(DateTime dt, Observatories obs) {
        double d = dt.julian - 2451543.5 ;
        double N = Math.toRadians(125.1228 - 0.0529538083  * d);
        double i = Math.toRadians(5.1454);
        double w = Math.toRadians(318.0634 + 0.1643573223  * d);
        double a = 60.2666; 
        double e = 0.054900; 
        double M = Math.toRadians(115.3654 + 13.0649929509 * d);
        
        // obtain Eccentric anomaly using Newton Raphson method to get E 
        Double E = calcE(e,M);
        
        // calculate rectangular coordinates in plane of Moons orbit 
        double x = a*(Math.cos(E) - e);
        double y = a* Math.sin(E) * Math.sqrt(1 - e*e);
        
        // calculate distance and true anomaly 
        double r = Math.sqrt(x*x + y*y);
        double v = Math.atan2(y, x );
        
        // calculate ecliptic coordinates
        double xeclip = r * ( Math.cos(N) * Math.cos(v+w) - Math.sin(N) * Math.sin(v+w) * Math.cos(i) );
        double yeclip = r * ( Math.sin(N) * Math.cos(v+w) + Math.cos(N) * Math.sin(v+w) * Math.cos(i) );
        double zeclip = r * Math.sin(v+w) * Math.sin(i);
        
        // ecliptic geocentric long and lat 
        double lon = Math.atan2(yeclip, xeclip);
        double lat = Math.atan2(zeclip,Math.sqrt(xeclip*xeclip+yeclip*yeclip));
        
        // establish ecliptic coordinates and convert to topocentric 
        ecliptic = new SphCoordinate(lon, lat);
        equatorial = ecliptic.getEquatorial();
        position = obs.getTopocentric(equatorial, dt, r);
    }
    
    /**
     * Calculate the eccentric anomaly E from the mean anomaly M using Newton-Raphson 
     * method. (see Boulet 1991).
     */
    private double calcE(double de,double dM) {
       double dE = dM;
       while (Math.abs(fnf(de, dE, dM))>1e-8) {dE=dE-fnf(de,dE,dM)/fndf(de,dE,dM);}
       return dE;
    }
    private double fnf(double de, double dE, double dM ) {return dE-de*Math.sin(dE)-dM;}
    private double fndf(double de, double dE, double dM) {return 1.0- de*Math.cos(dE);} 
    
    /**
     * Calculate current phase (illumination) of the Moon. Assumes the phase increases 
     * and decreases in a sine curve which is an approximation.
     * @param d DateTime.
     * @return Percent phase of the Moon.
     */
    public double getPhase(DateTime d) { 
        double phase = ((d.julian-mNew)/lMonth) %1;     // fraction of time through the month
        phase = phase - 0.5;                            // range is -0.5   0    +0.5
        phase = phase * pi2;                            // range is -PI    0    +PI
        phase = Math.cos(phase);                        // range is -1    +1    -1
        phase = 100*(phase +1)/2;                       // range is  0%   100%   0%          
        return phase;
    }
    
    /**
     * Calculate the angular distance between the Moon and an object at p at date-time d.
     * @param p Coordinates of object.
     * @param d DateTime of observation.
     * @param obs Observatory.
     * @return Angular distance of object from Moon.
     */
    public double getAngle(SphCoordinate p, DateTime d, Observatories obs) {
        // first make sure the Moon position is correct for the time
        doCoords(d, obs); 
        return Math.toDegrees(Math.abs(position.getAngle(p)));
    }
}
