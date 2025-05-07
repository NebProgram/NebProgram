/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author kos
 */
public class ScanCli {
//    private final int LIMITS_INTERVAL = 2;
//    private final int MAXPOOLTHREADS = 2;    
//    private final int timeout_ScanCliWorker = 180; // seconds

    Utils utils = new Utils();

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread) {
        int timeout = 10; // in sec
        return ScanCli.this.scan(network, exclude_list_ip, included_port, excluded_port, timeout_thread, timeout);
    }

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout) {
        ArrayList<String[]> result = new ArrayList();
        
        PrintStream err_original = null;
        if(!Utils.DEBUG) {
            err_original = System.err;
            System.setErr(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        //DO NOTHING
                    }
            }));  
        }

        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//        try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
            long[] interval = utils.intervalNetworkAddress(network);
            Utils.max_value = interval[1] - interval[0];
            CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip=utils.networkToIPAddress(addr);
                    if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) { 
                        callables.add(() -> scanPorts(ip, included_port, excluded_port, timeout));
                    }
                }
                
            } else {
                long addr = interval[0];
                String ip=utils.networkToIPAddress(addr);
                if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) {
                    callables.add(() -> scanPorts(ip, included_port, excluded_port, timeout));
                }
            }
            
            try {
                for (Future<String[]> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                    String[] res = null;
                    try {
                        res = f.get(timeout_thread, TimeUnit.MINUTES);
                    } catch (java.util.concurrent.TimeoutException ex) {
                        f.cancel(true);
                        System.out.println("Future Exception CancellationException!!!");
                    }                           
                    if(res != null && res[0] != null)
                        result.add(res);
                }
            } catch (InterruptedException | ExecutionException | CancellationException ex) {
                System.out.println("Service timeout Exception!!!");
            }           
        }

        if(err_original != null) {
            System.setErr(err_original);
        }
     
        return result;
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread) {
        int timeout = 10; // in sec
        return scan(list_ip, exclude_list_ip, included_port, excluded_port, timeout_thread, timeout);
    }       
    
    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout) {
        ArrayList<String[]> result = new ArrayList();
        
        PrintStream err_original = null;
        if(!Utils.DEBUG) {
            err_original = System.err;
            System.setErr(new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        //DO NOTHING
                    }
            }));  
        }

	if (!list_ip.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
//            try (ExecutorService service = Executors.newFixedThreadPool(128)) { 
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String ip : list_ip) {
                    if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) {
                        callables.add(() -> scanPorts(ip, included_port, excluded_port, timeout));
                    }
                }
                try {
                    for (Future<String[]> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        String[] res = null;
                        try {
                            res = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }                           
                        if(res != null && res[0] != null)
                            result.add(res);
                    }
                } catch (InterruptedException | ExecutionException | CancellationException ex) {
                    System.out.println("Service timeout Exception!!!");
                }
            }
        }        
        
        if(err_original != null) {
            System.setErr(err_original);
        }
     
        return result;
    }
    
    private String[] scanPorts(String node, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout) {
        boolean result = false;

        boolean is_include = false;
        for(Integer port : included_port) {
            if(TCPportIsOpen(node, port, timeout)) {
                is_include = true;
                break;
            }
        }
        if(is_include) {
            boolean is_exclude = false;
            for(Integer port : excluded_port) {
                if(TCPportIsOpen(node, port, timeout)) {
                    is_exclude = true;
                    break;
                }
            }
            if(!is_exclude)
                result = true;
        }

        String[] mas = new String[2];
        mas[0] = node;
        if(result) {
            mas[1] = "ok";
        } else {
            mas[1] = "err";
        }
        return mas;
    }  
    
    private boolean TCPportIsOpen(String node, int port, int timeout) {
        try {
            InetAddress IP = InetAddress.getByName(node);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(IP, port), timeout*1000);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }      
}
