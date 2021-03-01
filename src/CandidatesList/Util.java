
package CandidatesList;

/******************************************************************************************
 * Class Util contains general utility functions and constants that can be used across 
 * various projects.
 * 
 * @author Tony Evans
 **/
public class Util {
    // Some constants 
    public static final double parabolic = 0.98;    // limit of eccentricity for elliptical orbit 
    public static final double k = 0.01720209895;   // Gaussian gravitational constant
    public static final double pi2 = Math.PI * 2;   // Value of 2xpi
      
    /**
     * General purpose string to float converter.
     * @param s String to be converted
     * @param dflt default if conversion fails
     * @return value or default
     */
     public static float s2f(String s, float dflt) {
        if (s == null) { 
            return dflt;
        } else {
            try {
                return Float.parseFloat(s);
            } catch (NumberFormatException e) {
                return dflt;
     }}}
     
     /**
      * General purpose string to double converter.
      * @param s String to be converted
      * @param dflt default if conversion fails
      * @return value or default
      */
     public static double s2d(String s, double dflt) {
        if (s == null) { 
            return dflt;
        } else {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return dflt;
     }}}
    
     /**
      * General purpose string to integer converter.
      * @param s String to be converted
      * @param dflt default if conversion fails
      * @return value or default
      */
     public static int s2i(String s, int dflt) {
        if (s == null) {
            return dflt;
        } else {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return dflt;
     }}} 
}
