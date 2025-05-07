package ru.kos.neb.neb_lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kos
 */
public class RunScriptsPool {
//    public static StringBuffer result = new StringBuffer();
    
    private int MAXPOOLTHREADS = 16;
    private long MAX_OUTPUT_TIMEOUT = 10*60*1000; // 10 min
    private long MAX_PROCESS_TIMEOUT = 30*60*1000; // 10 min
    private final long SCRIPTS_POOL_TIMEOUT = 4*60*60*1000; // 4 hours
//    public final long min_memory = 1073741824; // 1 Gbyte.

    private int timeout = 10;
    private int retries = 1;  
    
    public Map<String, ArrayList> threads_pool = new HashMap();
    public ThreadPoolExecutor service = null;
    
    public static Utils utils = new Utils();
    public static Logger logger;
    
    
    public RunScriptsPool() {
        File file = new File(Utils.LOG_FILE);
        if(file.exists())
            file.delete();       
        // start logging
        logger = new ru.kos.neb.neb_lib.Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.setLevel(logger.DEBUG);
        else
            logger.setLevel(logger.INFO);
    }    

    public RunScriptsPool(long max_process_timeout, long max_output_timeout, int max_pool_threads) {
        MAX_PROCESS_TIMEOUT = max_process_timeout;
        MAX_OUTPUT_TIMEOUT = max_output_timeout;
        MAXPOOLTHREADS = max_pool_threads;
        
        File file = new File(Utils.LOG_FILE);
        if(file.exists())
            file.delete();         
        // start logging
        logger = new ru.kos.neb.neb_lib.Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.setLevel(logger.DEBUG);
        else
            logger.setLevel(logger.INFO);        
    }
    
    public ArrayList<String> Get(Map<String, ArrayList<String[]>> node_protocol_accounts, Map<String, ArrayList<String>> scripts)
    {
        return GetProc(node_protocol_accounts, scripts);
    }   

    public ArrayList<String> Get(Map<String, ArrayList<String[]>> node_protocol_accounts, Map<String, ArrayList<String>> scripts, int timeout, int retries)
    {
        this.timeout=timeout;
        this.retries=retries;
        return GetProc(node_protocol_accounts, scripts);
    }      
    
    private ArrayList<String> GetProc(Map<String, ArrayList<String[]>> node_protocol_accounts, Map<String, ArrayList<String>> scripts) {
        ArrayList<String> result = new ArrayList();
        Utils.current_value = 0;
        Utils.max_value = node_protocol_accounts.size();

//            result.setLength(0);
        if(!node_protocol_accounts.isEmpty()) {
            PrintStream oldError = System.err;
            System.setErr(new PrintStream(new OutputStream() { 
                @Override
                public void write(int b) {} }
            ));

            Map<String, Map<String, ArrayList<String[]>>> protocol___node___accounts = new HashMap();
            for(Map.Entry<String, ArrayList<String[]>> entry : node_protocol_accounts.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> accounts = entry.getValue();
                for(String[] account : accounts) {
                    String protocol = account[0];
                    String[] mas = new String[account.length+1];
                    for(int i=1; i<account.length; i++) {
                        mas[i-1]=account[i];
                    }
                    mas[account.length-1]=String.valueOf(this.timeout);
                    mas[account.length]=String.valueOf(this.retries);
                    if(protocol___node___accounts.get(protocol) != null)
                        if(protocol___node___accounts.get(protocol).get(node) != null) 
                            protocol___node___accounts.get(protocol).get(node).add(mas);
                        else {
    //                            Map<String, ArrayList<String[]>> tmp_map = new HashMap();
                            ArrayList<String[]> list = new ArrayList();
                            list.add(mas);
    //                            tmp_map.put(node, list);
                            protocol___node___accounts.get(protocol).put(node, list);
    //                            protocol___node___accounts_list.put(protocol, tmp_map);
                        }
                    else {
                        Map<String, ArrayList<String[]>> tmp_map = new HashMap();
                        ArrayList<String[]> list = new ArrayList();
                        list.add(mas);
                        tmp_map.put(node, list);
                        protocol___node___accounts.put(protocol, tmp_map);
                    }
                }
            }

            logger.println("Start ssh running scripts ...", logger.DEBUG);
            ArrayList res = RunningScripts(scripts, protocol___node___accounts, "ssh");
            result.addAll(res);
            logger.println("Stop ssh running scripts.", logger.DEBUG);
            logger.println("Start telnet running scripts ...", logger.DEBUG);
            res = RunningScripts(scripts, protocol___node___accounts, "telnet");
            result.addAll(res);
            logger.println("Stop telnet running scripts.", logger.DEBUG);
            logger.println("Start snmp running scripts ...", logger.DEBUG);
            res = RunningScripts(scripts, protocol___node___accounts, "snmp");
            result.addAll(res);
            logger.println("Stop snmp running scripts.", logger.DEBUG);

            System.setErr(oldError);
        }
//        System.out.println("Finished all threads");
        return result;
    }
    
    @SuppressWarnings("SleepWhileInLoop")
    private ArrayList<String> RunningScripts(Map<String, ArrayList<String>> scripts, Map<String, Map<String, ArrayList<String[]>>> protocol___node___protocol_accounts, String protocol) {
        ArrayList<String> result = new ArrayList();
        try {
            ArrayList<String> remove_node_list = new ArrayList();
            if(scripts.get(protocol) != null && protocol___node___protocol_accounts.get(protocol) != null && 
                !scripts.get(protocol).isEmpty() && !protocol___node___protocol_accounts.get(protocol).isEmpty()) {
                Map<String, ArrayList<String[]>> node___accounts = protocol___node___protocol_accounts.get(protocol);
//                ThreadPoolExecutor service = null;
                for(String script : scripts.get(protocol)) {  
                    WatchTimeout watchTimeout = new WatchTimeout(threads_pool, SCRIPTS_POOL_TIMEOUT);
                    service = StartScriptPool(script, node___accounts.size());
                    Map<String, String> response = RunCommands(node___accounts, service, script, protocol);
                    for(Map.Entry<String, String> entry1 : response.entrySet()) {
                        String cmd = entry1.getKey();
                        String out = entry1.getValue();
                        if(out !=null && !out.equals("") && !out.equals("out: {}out: {}")) {
                            result.add(out);
                            String node1 = cmd.split("\\s+")[0];
                            remove_node_list.add(node1);
                        }
                    }
                    DestroyPoolProcesses();
                    watchTimeout.stop = true;
                }

//                service.shutdown();
                while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }

                // remove node from protocol___node_protocol_accounts_list
//                if(protocol___node___protocol_accounts.get(protocol) != null) protocol___node___protocol_accounts.remove(protocol);
                for(Map.Entry<String, Map<String, ArrayList<String[]>>> entry : protocol___node___protocol_accounts.entrySet()) {
//                    String protocol1 = entry.getKey();
                    Map<String, ArrayList<String[]>> node___accounts1 = entry.getValue();
                    for(String remove_node : remove_node_list)
                        if(node___accounts1.get(remove_node) != null) node___accounts1.remove(remove_node);
                }
            }
        } catch (Exception ex) { 
            DestroyPoolProcesses();
            logger.println("Error RunScriptsPool - "+ex.getMessage(), logger.DEBUG);
        }        
        return result;
    }

    public ThreadPoolExecutor StartScriptPool(String script, int max_requests) {
//        DestroyPoolProcesses();
//        threads_pool.clear();
        // check free memory
//        long process_memory = 67108864;
//        long free = GetFreeMemory();
//        long max_used_memory = free*2/3;
//        MAXPOOLTHREADS = (int)(max_used_memory/process_memory);
        
//        int num_adding_process = 10;
        int maxpoolthreads = MAXPOOLTHREADS;
        if(max_requests < maxpoolthreads) maxpoolthreads=max_requests;
        
        ThreadPoolExecutor service = (ThreadPoolExecutor)Executors.newFixedThreadPool(maxpoolthreads);
        
        for(int i=0; i<maxpoolthreads; i++) {
            WorkerRun worker = new WorkerRun(script, threads_pool);
            service.execute(worker);
//            if(Utils.DEBUG_SCRIPT) System.out.println("Start new process.");
        }
        logger.println("Number started process = "+maxpoolthreads, logger.DEBUG);

//        if(Utils.DEBUG_SCRIPT) System.out.println("Start waiting running poll.");
        while(threads_pool.size() < maxpoolthreads) {
//            if(Utils.DEBUG_SCRIPT) System.out.println("threads_pool.size()="+threads_pool.size()+"  MAXPOOLTHREADS="+MAXPOOLTHREADS);
            try { Thread.sleep(3000); } catch (InterruptedException e) {  } 
        }
//        if(Utils.DEBUG_SCRIPT) System.out.println("Stop waiting running poll.");

        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            String name = entry1.getKey();
            ArrayList process_streams = entry1.getValue(); 
            process_streams.add(System.currentTimeMillis());
            process_streams.add("free");
            process_streams.add("");
            process_streams.add(System.currentTimeMillis());
        }
//            // check free memory
//            long free = GetFreeMemory();
//            if(Utils.DEBUG_SCRIPT) System.out.println("Free memory: "+free);
//            if(free < min_memory) break;            
//        }
        
        return service;
    }
    
    private Map<String, String> RunCommands(Map<String, ArrayList<String[]>> node___accounts, ThreadPoolExecutor service, String script, String protocol) {
        Map<String, String> result = new HashMap();
        logger.println("Start RunCommands.", logger.DEBUG);
        try {
            Queue<String> requests = new LinkedList();
            for(Map.Entry<String, ArrayList<String[]>> entry : node___accounts.entrySet()) {
                String node = entry.getKey();
//                String str = node+" "+protocol;
                ArrayList<String[]> accounts_list = entry.getValue();
                String acc_str = "";
                for(String[] account : accounts_list) {
                    if(account[0].matches("^AES:.+"))
                        account[0] = utils.decrypt(Utils.master_key, account[0]);
                    if(account[1].matches("^AES:.+"))
                        account[1] = utils.decrypt(Utils.master_key, account[1]);
                    
                    if(account[0] != null && account[1] != null) {
                        String acc_str1 = "";
                        for(String acc : account)
                            acc_str1=acc_str1+":"+acc;
                        acc_str1 = acc_str1.substring(1);
                        acc_str=acc_str+";"+acc_str1;
                    }
                }
                acc_str = acc_str.substring(1);
                
                requests.add(node+" "+protocol+" "+acc_str);
            }
            
            while(!IsEnd(threads_pool, requests)) {
                for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
                    String name = entry1.getKey();
                    ArrayList process_streams = entry1.getValue();
                    if(process_streams.size() == 7) {
                        Process process = (Process)process_streams.get(0);
                        BufferedWriter bw = (BufferedWriter)process_streams.get(1);
                        BufferedReader br = (BufferedReader)process_streams.get(2);
                        long timestamp = (Long)process_streams.get(3);
                        String state = (String)process_streams.get(4);
                        String request = (String)process_streams.get(5);
                        long timestamp_process = (Long)process_streams.get(6);
                        WatchCommand watchCommand = null;
                        if(state.equals("free")) {
                            String cmd = "";
                            synchronized(requests) { cmd = requests.poll(); }
                            if(cmd != null) {
                                bw.write(cmd+"\n");
                                bw.flush();
                                process_streams.set(3, System.currentTimeMillis());
                                process_streams.set(4, "busy");
                                process_streams.set(5, cmd);
                                process_streams.set(6, System.currentTimeMillis());
                                String[] mas = cmd.split("\\s");
                                logger.println("cmd: "+mas[0]+" "+mas[1]+" - Start.", logger.DEBUG);
                                watchCommand = new WatchCommand(process, MAX_PROCESS_TIMEOUT);
                            }
                        } else if(state.equals("busy")) {
                            if(br.ready()) {
                                String out = "";
                                while(br.ready()) {
                                    process_streams.set(3, System.currentTimeMillis());
                                    String res = br.readLine();
//                                    logger.Println(res, logger.DEBUG);
                                    out=out+res;
                                }
                                if(out != null && !out.equals("")) {
                                    Pattern p = Pattern.compile(".*<result>(.*)<\\/result>.*");
                                    Matcher m = p.matcher(out);
                                    if(m.find()){  
                                        String res=m.group(1);   
                                        result.put(request, res);
                                        Utils.current_value = Utils.current_value+1;
                                        String[] mas = request.split("\\s");
                                        logger.println("request: "+mas[0]+" "+mas[1]+" - End!", logger.DEBUG);
                                        process_streams.set(3, System.currentTimeMillis());
                                        process_streams.set(4, "free");
                                        process_streams.set(5, ""); 
                                        process_streams.set(6, System.currentTimeMillis());
                                    }
                                }
                                if(watchCommand != null)
                                    watchCommand.stop = true;
                            } else if(System.currentTimeMillis()-timestamp > MAX_OUTPUT_TIMEOUT ||
                                    System.currentTimeMillis()-timestamp_process > MAX_PROCESS_TIMEOUT) {
                                String[] mas = request.split("\\s");
                                logger.println("Process timeout: "+mas[0]+" "+mas[1], logger.DEBUG);
                                process.destroy();
                                try { Thread.sleep(1000*2); } catch (InterruptedException e) {  }
                                process.destroyForcibly();

                                logger.println("Destroy Process: "+mas[0]+" "+mas[1], logger.DEBUG);
                                process_streams.set(3, System.currentTimeMillis());
                                process_streams.set(4, "free");
                                process_streams.set(5, ""); 
                                process_streams.set(6, System.currentTimeMillis());
                                
                                
//                                WorkerRun worker = new WorkerRun(script, threads_pool);
//                                service.execute(worker);    
//                                process_streams.set(3, System.currentTimeMillis());
//                                process_streams.set(4, "free");
//                                process_streams.set(5, "");
//                                process_streams.set(6, System.currentTimeMillis());
//                                if(Utils.DEBUG_SCRIPT) System.out.println("Start new Process: "+request);
                            }
                        }
                    }
                }

                // Starting new processes instead process timeounts
//                for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
//                    String name = entry1.getKey();
//                    ArrayList process_streams = entry1.getValue(); 
//                    if(process_streams.size() == 6) {
//                        String state = (String)process_streams.get(4);
//                        if(state.equals("timeout")) {
//                            synchronized(threads_pool) { threads_pool.remove(name); }
//                            WorkerRun worker = new WorkerRun(script);
//                            service.submit(worker);
//                            if(Utils.DEBUG_SCRIPT) System.out.println("Start new Process after timeout.");                        
//                        }
//                    }
//                }                 
                try { Thread.sleep(1000L); } catch (InterruptedException e) {  }

            } 
        } catch (IOException ex) {
            ex.printStackTrace();
            logger.println("DestroyPoolProcesses - "+ex.getMessage(), logger.DEBUG);
        }
        logger.println("Stop RunCommands.", logger.DEBUG);
        
        return result;
    }
    
    private boolean IsEnd(Map<String, ArrayList> threads_pool, Queue<String> requests) {
        boolean pool_free=true;
        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            String name = entry1.getKey();
            ArrayList process_streams = entry1.getValue();
            if(process_streams.size() == 7) {
                String state = (String)process_streams.get(4);
                if(!state.equals("free")) {
                    pool_free=false;
                    break;
                }
            }
        }
        if(pool_free && requests.size() == 0) return true;
        return false;
    }
    
    public void DestroyPoolProcesses() {
        if(service != null)
            service.shutdown();
        
        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            try {
                String name = entry1.getKey();
                ArrayList process_streams = entry1.getValue();
                Process process = (Process)process_streams.get(0);
                BufferedWriter bw = (BufferedWriter)process_streams.get(1);
                bw.write("QUIT\n");
                bw.flush(); 
            } catch (IOException ex) {
                if(logger != null)
                    logger.println("DestroyPoolProcesses(QUIT) - "+ex.getMessage(), logger.DEBUG);
            }                
        }
        try { Thread.sleep(2*1000); } catch (InterruptedException e) {  }
        // lite destroy process
        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            try {
                String name = entry1.getKey();
                ArrayList process_streams = entry1.getValue();
                BufferedWriter bw = (BufferedWriter)process_streams.get(1);
                bw.close();
                BufferedReader br = (BufferedReader)process_streams.get(2);
                br.close(); 
                Process process = (Process)process_streams.get(0);
                if(process != null) process.destroy();
            } catch (IOException ex) {
                if(logger != null)
                    logger.println("DestroyPoolProcesses(destroy) - "+ex.getMessage(), logger.DEBUG);
            }                
        }
        
        try { Thread.sleep(2*1000); } catch (InterruptedException e) {  }
        // hard destroy process        
        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            try {
                String name = entry1.getKey();
                ArrayList process_streams = entry1.getValue();
                BufferedWriter bw = (BufferedWriter)process_streams.get(1);
                bw.close();
                BufferedReader br = (BufferedReader)process_streams.get(2);
                br.close(); 
                Process process = (Process)process_streams.get(0);
                if(process != null) process.destroyForcibly();                    
            } catch (IOException ex) {
                if(logger != null)
                    logger.println("DestroyPoolProcesses(destroyForcibly) - "+ex.getMessage(), logger.DEBUG);
            }                
        }
        threads_pool.clear();
        if(logger != null)
            logger.println("DestroyPoolProcesses.", logger.DEBUG);
    }
    
    private long GetFreeMemory() {
        com.sun.management.OperatingSystemMXBean bean =
         (com.sun.management.OperatingSystemMXBean)
           java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        return bean.getFreePhysicalMemorySize(); 
    }  
    
}

class WorkerRun implements Runnable {   
    private String script;
    private ArrayList<String> protocols_list;
    private Map<String, ArrayList> threads_pool;
    public boolean ready=true;
    
    public WorkerRun(String  script, Map<String, ArrayList> threads_pool) {
        this.script = script;
        this.threads_pool = threads_pool;
    }
    
    @Override
    public void run() {
        try {
            ready=false;
            String name = Thread.currentThread().getName();
//            if(Utils.DEBUG_SCRIPT) System.out.println(name);
            ArrayList process_streams = RunScriptsPool.utils.runScripts(script);
            synchronized(threads_pool) {
                threads_pool.put(name, process_streams);
            }
            ready=true;
        } catch (Exception ex) {
            ex.printStackTrace();
            RunScriptsPool.logger.println("RunScripts run - "+ex.getMessage(), RunScriptsPool.logger.DEBUG);
            ready=true;
        }        
    }    
    

    private Map<String, String> GetIpNode(Map<String, Object> nodes_information) {
        // Get Ip => node
        Map<String, String> ip_node = new HashMap();
        for(Map.Entry<String, Object> entry : nodes_information.entrySet()) {
            String node=entry.getKey();
            if(entry.getValue() instanceof Map) {
                Map val=(Map)entry.getValue();
                Map<String, Map> interfaces=(Map)val.get("interfaces");
                if(interfaces != null && interfaces.size() > 0) {
                    for(Map.Entry<String, Map> entry1 : interfaces.entrySet()) {
                        String iface_name=entry1.getKey();
                        ArrayList<String> ip_list = (ArrayList<String>)entry1.getValue().get("ip");
                        if(ip_list != null && ip_list.size() > 0) {
                            for(String ip : ip_list) {
                                ip_node.put(ip.split("[/\\s]")[0], node);
                            }
                        }
                    }
                }
                ip_node.put(node, node);
            }
        }
        
        return ip_node;
    } 
}

class WatchTimeout implements Runnable { 
    private long timeout = 0;
    private Map<String, ArrayList> threads_pool = new HashMap();
    public boolean stop = false;
    
    public WatchTimeout(Map<String, ArrayList> threads_pool, long timeout) {
        this.threads_pool = threads_pool;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis()-start < timeout || !stop) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {  }
        }
        if(!stop) DestroyPoolProcesses();
    }
    
    public void DestroyPoolProcesses() {
        // hard destroy process        
        for(Map.Entry<String, ArrayList> entry1 : threads_pool.entrySet()) {
            try {
                String name = entry1.getKey();
                ArrayList process_streams = entry1.getValue();
                BufferedWriter bw = (BufferedWriter)process_streams.get(1);
                bw.close();
                BufferedReader br = (BufferedReader)process_streams.get(2);
                br.close(); 
                Process process = (Process)process_streams.get(0);
                if(process != null) process.destroyForcibly();                    
            } catch (IOException ex) {
                if(RunScriptsPool.logger != null)
                    RunScriptsPool.logger.println("DestroyPoolProcesses(destroyForcibly) - "+ex.getMessage(), RunScriptsPool.logger.DEBUG);
            }                
        }
        threads_pool.clear();
        if(RunScriptsPool.logger != null)
            RunScriptsPool.logger.println("DestroyPoolProcesses timeout pool!!!", RunScriptsPool.logger.DEBUG);
    }
    
}

class WatchCommand implements Runnable { 
    private long timeout = 0;
    private Process process;
    public boolean stop = false;
    
    public WatchCommand(Process process, long timeout) {
        this.process = process;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis()-start < timeout || !stop) {
            try { Thread.sleep(1000); } catch (InterruptedException e) {  }
        }
        if(!stop) {
            process.destroy();
            try { Thread.sleep(1000*2); } catch (InterruptedException e) {  }
            process.destroyForcibly();

            RunScriptsPool.logger.println("Destroy Process WatchCommand: ", RunScriptsPool.logger.DEBUG);            
        }
    }
        
}
