package CandidatesList;

import java.io.Serializable;
import java.text.*;
import java.util.Calendar;
import java.util.TimeZone;

/***********************************************************************************************************************
 * Class DateTime contains a date and time with transforms between Julian Dates and Gregorian formats. 
 * It was originally developed as part of the Jingo Package but has been copied and evolved here 
 * to avoid having to reference the Jingo library or overload Jingo with the time functions.
 *
 * Note that the master date/time is held in Julian format, other forms are generated when needed.
 * 
 * A method is included to set the date from a MPC packed date and to generate dates formatted as in
 * the MPC Further Observation? commentary. 
 *
 * @author Tony Evans
 */

public class DateTime implements Serializable {
    
    // master data is Julian date
    public double julian = 2451545.0;              
    
    // gregorian date and time calculated when needed
    private int jYear = 2000;                    
    private int jMonth = 1;                      
    private int jDay = 1;                      
    private int jHour = 12;
    private int jMinute = 0;
    private int jSecond = 0;
    
    // some format names */
    public static final int HOUR = 1;
    public static final int HHMM = 2;
    
    // working variables and constants 
    private boolean isSet;                           // true if gregorian date has been calculated
    public final static double J2000 = 2451545.0;    // Julian date of J2000 is midday on 1 Jan 2000
    private int jJyear = 2000;                       // working value
    private int jJmonth;                             // working value
    private static final int JGREG = 15 + 31 * (10 + 12 * 1582); // changeover date in Gregorian calendar
    
    // the following is used in decoding the MPC compressed format dates
    private static final String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcde"
            + "fghijklmnopqrstuvwxyz";
    
    // the following is used to construct dates as formated in MPC lists 
    private final String[] months ={"Jan.","Feb.","Mar.","Apr.","May","June","July","Aug.","Sep.","Oct.","Nov.","Dec."};
    
    // various formats for output
    private static final DecimalFormat jf = new DecimalFormat("#############0.0000");
    private static final DecimalFormat f2 = new DecimalFormat("00");
    private static final DecimalFormat f2a = new DecimalFormat("#0");
    private static final DecimalFormat f4 = new DecimalFormat("#######0000");

    /**
     * Constructor creates a date with default J2000 epoch. Gregorian data is not set.
     */
    public DateTime() {
        isSet = false;
    }
   
    /**
     * setDate(day, month, year) set the date according to Gregorian input. Default time is zero 0:0:0.
     */
    public void setDate(int d, int m, int y) {
        jDay = d;
        jMonth = m;
        jYear = y;      //load greg date as is
        jJyear = jYear;
        if (jJyear < 0) {
            jJyear++;
        }       // account for year zero is 1BC
        if (m > 2) {
            jJmonth = m + 1;
        } else {
            jJyear--;
            jJmonth = m + 13;
        }

        julian = (java.lang.Math.floor(365.25 * jJyear)
                + java.lang.Math.floor(30.6001 * jJmonth)
                + d + 1720995.0);
        if (d + 31 * (m + 12 * y) >= JGREG) {  // change over to Gregorian calendar
            int ja = (int) (0.01 * jJyear);
            julian += 2 - ja + (int) (0.25 * ja);
        }
        julian-=0.5; // set to zero hours UT on the specified day.
        isSet=false;
    }
    
     /**
      * set Time sets the time of day from hh mm ss or from fraction of a day. 
      */
     public void setTime( int h, int m, int s) {
         
         julian = 0.5+((int) (julian-0.5));                             // set to time zero of current day
         julian+= (((double) h)/24) + (((double) m)/1440) + (((double) s)/86400);          // add the time
         isSet=false;
         
     }
     public void setTime(double d) {
         julian = 0.5+((int) (julian-0.5)); 
         julian+=d;
         isSet=false;
     }
    
     /**
      * setDateTime from java Calendar object.
      */
      public void setDateTime(Calendar cal)           {
        int year = cal.get(Calendar.YEAR);
        int month = 1+cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
                
        setDate(day, month,year);
        int hour =  cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        setTime(hour, min, sec);
        isSet=false;
    }
    
    /**
     * setDate(Julian days) set date according to Julian days. This can contain time information.
     */
    public void setDate(double d) {
        julian = d;
        isSet=false;
    }
  
    /**
     * setDate(string) set date according to epoch name.
     * @para s Epoch name "J2000" or "today".
     */
    public void setDate(String s) {
        if (s.equals("J2000")) {
            setDate(J2000);
            isSet=false;
        }
        if (s.equals("today")) {
            setNow();                 
            isSet=false;
        }
    }
   
    /**
     * setMPCTextDate(string) sets the date according to a date in MPC text format.
     */
    public void setMPCTextDate(String s) {
        int MPCy = s2i(s.substring(0,4),1);
        int MPCm = 1;
        for (int i=0; i<12; i++) {if (s.contains(months[i])) {MPCm = i+1;break;}}
        int MPCd = s2i(s.substring(10,12),1);
        setDate(MPCd, MPCm, MPCy);
    }

    /**
     * getGdate() returns a Gregorian date in string format.
     */
    public String getGdate() {
        if (!isSet) {setGregorian();}
        return f4.format(jYear) + "-" + f2.format(jMonth) + "-" + f2.format(jDay);
    }
    
    /**
     * getTime returns time in one of several possible formats.
     */
    public String getTime(int format) {
        if (!isSet) {setGregorian();}
        if (format == HOUR) {
            return f2.format((int) (jHour/24 + jMinute/140 + jSecond/86400));
        }
        if (format == HHMM) {
            return f2.format(jHour)+":"+f2.format(jMinute);
        }
        return "fmt?";
    }
    
    /** getHours returns an integer number of hours.
     * 
     * @return Hours part of the time.
     */
    public int getHours() {
        if (!isSet) {setGregorian();}
        return (int) jHour;
    }
    
    
    /**
     * getMinutes returns an integer number of minutes.
     * @return Minutes part of the time.
     */
    public int getMinutes() {
        if (!isSet) {setGregorian();}
        return (int) jMinute;
        }
        
    /**
     * getMPCDate() return date in format used by MPC queries. Note the extra blank when the day is a single figure.
     * but this extra blank seems to be inconsistent so we have 2 flavours.
     */
    public String getMPCDate() {
        if (!isSet) {setGregorian();}
        String dy = f2a.format(Math.floor(jDay));
        return f4.format(jYear) + " " + months[jMonth-1] + " " + dy;
    }
    public String getMPCDate1() {
        if (!isSet) {setGregorian();}
        String dy = f2a.format(Math.floor(jDay));
        if (1==dy.length()) {dy=" "+dy;}
        return f4.format(jYear) + " " + months[jMonth-1] + " " + dy;
    }
   
    /**
     * getJdate() returns a Julian date in string format.
     */
    public String getJdate() {
        return jf.format(julian);
    }
   
    /**
     * add(double) adds the given number of days to the date.
     */
    public void add(double d) {
        julian += d;
        isSet=false;
    }
   
    /**
     * isJ2000() returns true if the date is J2000.
     */
    public boolean isJ2000() {
        return julian == J2000;
    }
   
    /**
     * setMPCdate sets the date from an MPC compressed date format.
     */
    public void setMPCDate(String mpc) {
        // the century is encoded into the first char
        int yy=0;
        if ("I".equals(mpc.substring(0,1))) {yy=1800;}
        if ("J".equals(mpc.substring(0,1))) {yy=1900;}
        if ("K".equals(mpc.substring(0,1))) {yy=2000;}
        yy+=Integer.parseInt(mpc.substring(1,3));           // year is numeric in 2nd and 3rd chars
        int mm = charset.indexOf(mpc.substring(3,4));       // month is 4th char encoded
        int dd = charset.indexOf(mpc.substring(4,5));       // day is 5th char encoded
        setDate(dd,mm,yy);
        isSet=false;
    }
    
    /**
     * setGregorian calculates the Gregorian date and time. Taken from Montenbruck & Pfledger p16.
     */
    private void setGregorian() {
        // establish date
        long  ja, jb, jc, jd, je, f;
        ja = (long) (julian+0.5);
        if (ja<2299161) {
            jb=0; jc=ja+1524;
        } else {
            jb=(long) ((ja-1867216.25)/36524.25);
            jc=ja+jb-(jb/4)+1525;                       
        }
        
        jd=(long) ((jc-122.1)/365.25);
        je=365*jd + jd/4; 
        f= (long) ((jc-je)/30.6001);
        jDay=(int) ((int) jc-je-(int) (30.6001*f));
        jMonth=(int) (f-1-12*(f/14));
        jYear=(int) (jd-4715 - ((7+jMonth)/10));
         
        // establish time
        double dd = ((julian%1)*24 +12)%24;                
        jHour = (int) dd;
        dd=(dd-jHour)*60;
        jMinute = (int) dd;
        dd=(dd-jMinute)*60;
        jSecond = (int) dd;
        isSet=true;
    }
   
    /**
     * setNow() sets the date and time to "now" U.T.
     */
    public void setNow() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int year = now.get(Calendar.YEAR);
        int month = 1+now.get(Calendar.MONTH);
        int day = now.get(Calendar.DAY_OF_MONTH);      
        setDate(day, month,year);
        int hour =  now.get(Calendar.HOUR_OF_DAY);
        int min = now.get(Calendar.MINUTE);
        int sec = now.get(Calendar.SECOND);
        setTime(hour, min, sec);
        isSet=false;
    }
    
    /**
     * Set date from ESA style date string.
     * @param s Date in form yyyy-mm-dd
     */
    public void setESADate(String s) {
        int y = s2i(s.substring(0,4), 1900);
        int m = s2i(s.substring(5,7),1);
        int d = s2i(s.substring(8),1);
        setDate(d,m,y);
    }
    
    /**
     * General purpose string to integer converter.
     */ 
     private static int s2i(String s, int dflt) {
        if (s == null) {
            return dflt;
        } else {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return dflt;
            }
        }
    }
}
