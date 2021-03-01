
package CandidatesList;

/***********************************************************************************
 * Class Sun is the Sun. The main method is to calculate position at a specified 
 * date-time. * 
 * @author Tony Evans
 */
public class Sun {
    
    public SphCoordinate ecliptic = new SphCoordinate();         // Ecliptic coordinates of Sun
    private SphCoordinate position = new SphCoordinate();         // Equatorial coordinates of the Sun
    private static final double pi2 = Math.PI*2;                  // 2 pi
    
    public double r =0.0;         // distance and ecliptic rectangular coords from Earth (used for candidate positions)
    public double x =0.0;
    public double y =0.0;
    public double L =0.0;         // mean longitude
    
   /**
    * Constructor.
    */
    public Sun() { 
    } 
    
   /**
    * Return the position of the Sun at the specified DateTime.
    * @param d The specified date-time.
     * @return Position of Sun at date
    */
    public SphCoordinate getPosition(DateTime d) {
        doCoords(d);
        return position;
    }
   
    /**
     * Calculate current position at DateTime d. 
     * Taken from http://www.stjarnhimlen.se/comp/tutorial.html.
     * @param dt DateTime of the required coordinates.
     */
    private void doCoords(DateTime dt) {
        double d = dt.julian - 2451543.5;
        double w  = Math.toRadians(282.9404 + 4.70935E-5 * d);          //longitude of perihelion
        double e = 0.016709 - 1.151E-9 * d;                             //eccentricity
        double M = Math.toRadians((356.0470 + 0.9856002585 * d));       //mean anomaly adjust values to 0-2pi
        while (M<0.0) {M+=pi2;}
        while (M>pi2) {M-=pi2;}
               L = (w + M)%pi2;                                         //mean longitude
       
        // calculate eccentric anomaly 
        double E = M + e*Math.sin(M) * (1 + e * Math.cos(M));
      
        // calculate rectangular coordinates in plane of ecliptic */
               x = Math.cos(E) - e;
               y = Math.sin(E) * Math.sqrt(1 - e*e);
        
        // calculate distance and true anomaly 
               r = Math.sqrt(x*x + y*y);
        double v = Math.atan2( y, x );
        
        // calc longitude of Sun and coordinates
        double lon = (v+w)%pi2;
        x = r * Math.cos(lon);
        y = r * Math.sin(lon);
        
        // calculate ecliptic coordinates 
        ecliptic.coord[0] = lon;    
        ecliptic.coord[1] = 0.0;  
          
        // RA and Dec position 
        position = ecliptic.getEquatorial();
    }
    
}
