
package CandidatesList;
/************************************************************************************
 * Class CandiParameters saves and retrieves user options and parameters in an 
 * ini file.
 * 
 * @author Tony Evans
 */
public class CandiParameters {
    private final CandidatesList5GUI gui;

   /**
    * Constructor stores pointer to the GUI for direct access to the form content.
    * @param gui The calling GUI.
    */ 
    public CandiParameters(CandidatesList5GUI gui) {
        this.gui=gui; 
    }
    
    /**
     * Save all the parameters in an ini file. 
     */
    public void putParms() {
        IniFile ini = new IniFile();
        ini.putProperty("twilight", ""+gui.twilight.getSelectedIndex());
        ini.putProperty("observatory", ""+gui.observe.getSelectedIndex());
        ini.putProperty("dlmag", ""+gui.magLimit.getValue());
        ini.putProperty("dispmag", ""+gui.vFilter.getValue());
        ini.putProperty("minalt", ""+gui.altFilter.getValue());
        ini.putProperty("minga", ""+gui.galFilter.getValue());
        ini.putProperty("uncert", ""+gui.uncert.getValue());
    }
    
    /**
     * Fetch all the user parameters from an ini file.
     */
    public void getParms() {
        IniFile ini = new IniFile();
        if (ini.exists()) {
            try {
            gui.twilight.setSelectedIndex(Integer.parseInt(ini.getProperty("twilight")));
            gui.observe.setSelectedIndex(Integer.parseInt(ini.getProperty("observatory")));
            gui.magLimit.setValue(Float.parseFloat(ini.getProperty("dlmag")));
            gui.vFilter.setValue(Float.parseFloat(ini.getProperty("dispmag")));
            gui.altFilter.setValue(Integer.parseInt(ini.getProperty("minalt")));
            gui.galFilter.setValue(Integer.parseInt(ini.getProperty("minga")));
            gui.uncert.setValue(Integer.parseInt(ini.getProperty("uncert")));
            } catch (NumberFormatException e) {
            gui.commentary.append("Exception reading parameters " + e.getMessage());}
        }   
    }
}
    

