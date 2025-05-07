package ru.kos.neb.neb_lib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Logger {
    public int INFO = 5;
    public int DEBUG = 10;
    private int level = 5;
    private BufferedWriter descLog = null;
    
    public Logger(String logfile) {
//        Clear(logfile);
        try {
            descLog = new BufferedWriter(new FileWriter(logfile, true));
        } catch (IOException ex) {
            System.out.println(Logger.class.getName()+"\t"+ex);
        }
    }
    
    public void clear(String logfile) {
        if(this.descLog != null) try {
            this.descLog.close();
        } catch (IOException _) {  }

        try {
            descLog = new BufferedWriter(new FileWriter(logfile));
            descLog.write("");
        } catch (IOException ex) {
            System.out.println(Logger.class.getName()+"\t"+ex);
        }
    }
    
    public void setLevel(int level) {
        this.level=level;
    }
    
    public int println(String msg, int level) {
        int result = 0;
        if(this.descLog != null ) {
            if(level <= this.level) {
                if(level == this.INFO) System.out.println(msg);
                try {
                    if(msg != null) {
                        this.descLog.write(new Date()+":\t"+msg+"\r\n");
                        this.descLog.flush();
                        result=msg.length();
                    }
                } catch (IOException ex) {
//                    System.out.println(Logger.class.getName()+"\t"+ex);
                }
            }
        }
        return result;
    }
    
    public int print(String msg, int level) {
        int result = 0;
        if(this.descLog != null ) {
            if(level <= this.level) {
                if(level == this.INFO) System.out.print(msg);
                try {
                    if(msg != null) {
                        this.descLog.write(msg);
                        this.descLog.flush();
                        result=msg.length();
                    }
                } catch (IOException ex) {
//                    System.out.println(Logger.class.getName()+"\t"+ex);
                }
            }
        }
        return result;
    }

}
