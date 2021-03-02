
package CandidatesList;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Class CandidatesFile is the file that gives persistence to the candidates list. It is implemented 
 * using serialisation. 
 * 
 * @author Tony Evans
 **/
public class CandidatesFile implements Serializable {

    protected ArrayList<Candidate> cList;          // Array containing  the candidates
    private File dataFile;                         // File for the data
    public  String fileName = " ";                 // Name of file containing the candidates
    private final CandidatesList5GUI GUI;          // Parent GUI
    private IniFile ini;                           // ini file for persistence of file path

   /**
    * Constructor saves pointer to parent GUI for direct access to the form.
    * @param g The GUI
    */
    public CandidatesFile(CandidatesList5GUI g) {
        GUI = g;
    }
    
    /**
     * Initialisation looks for an ini file to get the name of the candidates file. If a valid file
     * is found the candidates are loaded from it. Missing ini file implies start a new candidates list.
     */
    public void initialise() { 
        cList = new ArrayList<>(200);
        clear();
        ini = new IniFile();
        if (ini.exists()) {
            fileName = ini.getProperty("filename");
            if (!(fileName == null)) {
                dataFile = new File(fileName);
                   if (dataFile.exists()) {
                      read();
                   }
             } 
        }   
    }
    
    /**
     * Remove all data and prepare for a new candidates list.
     */
    public void clear() {
        cList.clear();
        dataFile = null;
        fileName=" ";
    }
   
    /**
     * Open uses the file chooser to select a file and read the candidates list.
     */
    public void open() {
        clear();
        
        // set up and show file chooser dialog
        Frame parent = new Frame();
        JFileChooser fc = new JFileChooser(ini.getProperty("directory"));
        FileNameExtensionFilter ff = new FileNameExtensionFilter("Candidates List", "lst");
        fc.addChoosableFileFilter(ff);
        int r = fc.showOpenDialog(parent);

        // if chooser completes OK then save path in ini file and read the file
        if (r == JFileChooser.APPROVE_OPTION) {
            dataFile = fc.getSelectedFile();
            fileName = dataFile.getPath();
            ini.putProperty("filename", dataFile.getPath());
            ini.putProperty("directory", dataFile.getParent());
            read();
        }
    }
    
    /**
     * saveAs uses the file chooser to set up a new file and save data to it.
     */
    public void saveAs() {
        // set up and show file saveAs dialog 
        Frame parent = new Frame();
        JFileChooser fc = new JFileChooser(ini.getProperty("directory"));
        FileNameExtensionFilter ff = new FileNameExtensionFilter("lst", "lst");
        fc.addChoosableFileFilter(ff);
        int r = fc.showSaveDialog(parent);

        // if chooser closes OK, ensure proper file type suffix
        if (r == JFileChooser.APPROVE_OPTION) {
            dataFile = fc.getSelectedFile();
            String path = dataFile.getPath();
            if ((path.endsWith(".lst"))) {
                dataFile = new File(path);
            } else {
                dataFile = new File(path + ".lst");
            }
            
           // check if file of that name already exists and ask if ok to overwrite */
            if (dataFile.exists()) {
                Toolkit.getDefaultToolkit().beep();
                int opt = JOptionPane.showConfirmDialog(null, "File already exists. Overwrite it?",
                        "File Exists", JOptionPane.YES_NO_OPTION);
                if (opt != 0) {
                    return;
                }
            }
            
            // if we got here it is OK to proceed with writing the data */
            write();

            // save filename and directory path in the ini file */
            fileName = dataFile.getPath();
            ini.putProperty("filename", dataFile.getPath());
            ini.putProperty("directory", dataFile.getParent());
        }
    }

    /**
     * Save the existing file with the same name. If it does not exist then use saveAs.
     */
    public void save() {
        if (dataFile == null) {
            saveAs();
        }
        if (dataFile.exists()) {
            write();
        } else {
            saveAs();
        }
    }

   /**
    * Ask the user if you want to save.
    */
    public void askToSave() {
        Toolkit.getDefaultToolkit().beep();
        int opt = JOptionPane.showConfirmDialog(null, "Do you want to save the current data?",
                "Save Changes?", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.NO_OPTION) {
        } else {
            save();
        }
    }
    
    /**
     * Writes data from array to the file as serialised objects.
     */
    private void write() {
        try (FileOutputStream fos = new FileOutputStream(dataFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(cList);
            oos.close();
            fos.close();
        } catch (IOException ex) {
            GUI.commentary.append("IO Error writing file.");
        }
    }
    
    /**
     * Reads the data and de-serialises into the array.
     */
    private void read() {
        try (FileInputStream fos = new FileInputStream(dataFile);
            ObjectInputStream oos = new ObjectInputStream(fos)) {
            cList = (ArrayList<Candidate>) oos.readObject();
            oos.close();
            fos.close();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(CandidatesFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
        }
    }
        
    /**
     * Find the candidate with this name if it already exists otherwise provide a new candidate record.
     * @param n Name of candidate.
     * @param add True= if candidate not already in list, add it.
     * @return The requested candidate or a new empty candidate.
     */
    public Candidate find(String n, Boolean add) {
        Iterator<Candidate> it = cList.iterator();
        while (it.hasNext()) {
            Candidate c = it.next();
            if (c.name.equals(n)) {return c;}
        }
        if (add) {
            Candidate c2=new Candidate();
            c2.name = n;
            cList.add(c2);
        return c2;
        }
        return null;
    }  
    /**
     * Find the candidate with this number.
     * @param n Number of candidate.
     * @return The requested candidate or null if does not exist.
     */
    public Candidate findN(String n) {
        Iterator<Candidate> it = cList.iterator();
        while (it.hasNext()) {
            Candidate c = it.next();
            if (c.number.equals(n)) {return c;}
        }
        return null;
    }  

}