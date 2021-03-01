package CandidatesList;

/**********************************************************************************************************************
 * Class Earth is responsible to obtain the position of Earth at a specified moment of time.
 * It employs VSOP87 version A - Heliocentric, equatorial, J2000.
 * 
 * @author Tony Evans
 **/
public class Earth {
   
    // x,y,and z are Earth's cartesian equatorial heliocentric J2000 coordinates.
    public  double x,y,z;
    private double t;
    
    public void setPosition(DateTime jd){
         t = (jd.julian-2451545.0)/365250.0;
         x=getX();
         y=getY();
         z=getZ();
    }
    
    /**
     * X, Y and Z coordinates are obtained from VSOP87 using the formulae explained at 
     *               http://neoprogrammics.com/vsop87/index.html 
     */
    public double getX() {
        double r = Vsop87Earth.Earth_A_X0(t)+Vsop87Earth.Earth_A_X1(t)+Vsop87Earth.Earth_A_X2(t)+
                   Vsop87Earth.Earth_A_X3(t)+Vsop87Earth.Earth_A_X4(t)+Vsop87Earth.Earth_A_X5(t) ;
        return r;
    }
    public double getY() {
        double r = Vsop87Earth.Earth_A_Y0(t)+Vsop87Earth.Earth_A_Y1(t)+Vsop87Earth.Earth_A_Y2(t)+
                   Vsop87Earth.Earth_A_Y3(t)+Vsop87Earth.Earth_A_Y4(t)+Vsop87Earth.Earth_A_Y5(t) ;
        return r;
    }
    public double getZ() {
     
        double r = Vsop87Earth.Earth_A_Z0(t)+Vsop87Earth.Earth_A_Z1(t)+Vsop87Earth.Earth_A_Z2(t)+
                   Vsop87Earth.Earth_A_Z3(t)+Vsop87Earth.Earth_A_Z4(t)+Vsop87Earth.Earth_A_Z5(t) ;
        return r;
    
    }
}
