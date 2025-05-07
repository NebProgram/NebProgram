package ru.kos.neb_viewer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static ru.kos.neb_viewer.Main.history_list;
import static ru.kos.neb_viewer.Main.utils;

public class CheckerMapFile extends Thread {
    public static long time_start = 0;
    
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        time_start = System.currentTimeMillis();
        String prev_map_filename=history_list.get(TimeMachineForm.selector)[1];
        System.out.println("time_start="+time_start);
        while(true) {
            if(!Main.isBusy) {
                try {
                    BufferedReader inFile = new BufferedReader(new FileReader(Main.dump));
                    try {
                        String line1 = inFile.readLine();
                        long time_from_file = Long.parseLong(line1);
                        if(time_from_file > time_start) {
                            time_start = System.currentTimeMillis();
                            System.out.println("time_start="+time_start);
                            history_list = utils.GetListMapFiles();
                            for(int i=0; i<history_list.size(); i++) {
                                String[] item = history_list.get(i);
                                if(prev_map_filename.equals(item[1])) {
                                    TimeMachineForm.selector=i;
                                    break;
                                }
                            }
                            Utils.SetTimeMachine(history_list, TimeMachineForm.selector, ControlPanel.area_select);
                        }
                    } catch (IOException _) { }
                } catch (FileNotFoundException _) { }
            
            }
            prev_map_filename=history_list.get(TimeMachineForm.selector)[1];
            try { Thread.sleep(10000); } catch (InterruptedException _) { }

        }
    }
}
