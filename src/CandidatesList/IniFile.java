
package CandidatesList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Class IniFile is an ".ini" properties file that stores file and directory names
 * most recently used.
 *
 * @author Tony Evans
 */

public class IniFile {

    private static Properties ini;         // properties object
    private static String iniFileName;     // path and name of ini file

    /**
     * Constructor creates an ini filename in the user home directory.
     */
    public IniFile() {
        ini = new Properties();                        // create properties list
        iniFileName = System.getProperty("user.home"); // get location for ini file
        iniFileName+="\\candy5.ini";                   // construct ini file name
    }
   
    /**
     * Get a property value from the ini file.
     * @param prop Name of the property to be retrieved.
     * @return Value of the property from the ini file or null if the property does not exist.
     */
    public String getProperty(String prop) {
        String r ="";
            try (FileInputStream in = new FileInputStream(iniFileName)) {
                ini.load(in);
                r = ini.getProperty(prop);
            }
            catch (Exception e){}
        return r;
    }
    
    /**
     * Put a property value into the ini file.
     * @param property Name of the property to be saved.
     * @param value Value of the property to be saved.
     */
    public void putProperty(String property, String value) {
        try (FileInputStream in = new FileInputStream(iniFileName)) {
            ini.load(in);
        }
        catch (Exception e) {
        }
        ini.setProperty(property, value);
        try (FileOutputStream out = new FileOutputStream(iniFileName)) {
            ini.store(out, "CandidatesIni");
        }
        catch (Exception e) {
        }
    }
    
    /**
     * Check if the ini file exists.
     * @return true if the ini file already exists.
     */
    public boolean exists() {
        File fini = new File(iniFileName);
        return fini.exists();

    }
}
