package CandidatesList;

import java.awt.Frame;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**********************************************************************************
 * Class  MPCORBFile provides ability for user to identify the local MPCORB File.
 *
 * @author Tony Evans
 */
public class MPCORBFile {

    public String fileName;
    public Boolean mpcorbOK;

   /**
    * Constructor tries to find the ini file and get the previously used file name.
    */
    public MPCORBFile() {
        IniFile ini = new IniFile();
        fileName = ini.getProperty("mpcorb");
        if (!(fileName == null)) {
            File f = new File(fileName);
            if (!f.exists()) {
                fileName = "Please browse to your MPCORB.DAT file.";
                mpcorbOK = false;
            }
            else {
                mpcorbOK = true;
            }
        } else {
            fileName = "Please browse to your MPCORB.DAT file.";
            mpcorbOK = false;
        }
    }
    
    /**
     * Open a file selection dialog for the user to select the MPCORB.dat file.
     */
    public void browse() {
       // set up and show file open dialog 
        IniFile ini = new IniFile();
        Frame parent = new Frame();
        JFileChooser fc;
        FileNameExtensionFilter ff;
        fc = new JFileChooser(ini.getProperty("mpcorb"));
        ff = new FileNameExtensionFilter("dat files","dat");
        fc.addChoosableFileFilter(ff);
        int r = fc.showOpenDialog(parent);

       //if a file is found save its path and name otherwise set a message saying no file.
        if (r == JFileChooser.APPROVE_OPTION) {
            File fMpc = fc.getSelectedFile();
            ini.putProperty("mpcorb", fMpc.getPath());
            ini.putProperty("mpcpath",fMpc.getParent());
            fileName = fMpc.getPath();
        } else {
            fileName ="MPCORB.dat file not found";
        }
    }
}
