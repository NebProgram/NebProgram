package ru.kos.neb.neb_lib;

import java.io.File;

public class SnmpUtils {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        File file = new File(Utils.LOG_FILE); 
        if(file.exists())
            file.delete();
        
    }

}
