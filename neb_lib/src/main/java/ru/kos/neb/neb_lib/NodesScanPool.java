package ru.kos.neb.neb_lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NodesScanPool {
    private final int timeout = 3;
    private final int retries = 2;
//    private final int timeout_thread = 1; // min
    public static Logger logger;
//    private static final ConcurrentHashMap<String, String> collector = new ConcurrentHashMap();

    Utils utils = new Utils();

    public NodesScanPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public Map<String, String> get(ArrayList<String> list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread) {
        return NodesScanPool.this.getProc(list_ip, included_port, excluded_port, timeout_thread, this.timeout, this.retries);
    }

    public Map<String, String> get(ArrayList<String> list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout) {
        return NodesScanPool.this.getProc(list_ip, included_port, excluded_port, timeout_thread, timeout, this.retries);
    }

    public Map<String, String> get(ArrayList<String> list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout, int retries) {
        return NodesScanPool.this.getProc(list_ip, included_port, excluded_port, timeout_thread, timeout, retries);
    }

    private Map<String, String> getProc(ArrayList<String> list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout, int retries) {
        Map<String, String> result = new HashMap();
        
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
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String node : list_ip) {
                    callables.add(() -> node_scan_ports(node, included_port, excluded_port, timeout, retries));
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
                            result.put(res[0], res[1]);             
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

    private String[] node_scan_ports(String node, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout, int retries) {
        String[] result = new String[2];
        
        boolean include_flag = false;
        for (int port : included_port) {
            if (TCPportIsOpen(node, port, timeout)) {
                include_flag = true;
                break;
            }
        }
        boolean exclude_flag = false;
        if(include_flag) {
            for (int port : excluded_port) {
                if (TCPportIsOpen(node, port, timeout)) {
                    exclude_flag = true;
                    break;
                }
            }
        }
        result[0] = node;
        if(include_flag && !exclude_flag) {
            result[1] = "ok";
        } else {
            result[1] = "err";
        }
        return result;
    }

    private boolean TCPportIsOpen(String node, int port, int timeout) {
        try {
            InetAddress IP = InetAddress.getByName(node);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(IP, port), timeout * 1000);
                return true;
            }
        } catch (IOException _) {
        }
        return false;
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread) {
        return getProc(network, exclude_list_ip, included_port, excluded_port, timeout_thread, this.timeout, this.retries);
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout) {
        return getProc(network, exclude_list_ip, included_port, excluded_port, timeout_thread, timeout, this.retries);
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout, int retries) {
        return getProc(network, exclude_list_ip, included_port, excluded_port, timeout_thread, timeout, retries);
    }

    private Map<String, String> getProc(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> included_port, ArrayList<Integer> excluded_port, int timeout_thread, int timeout, int retries) {
        Map<String, String> result = new HashMap();
        
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
            CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip = utils.networkToIPAddress(addr);
                    if (exclude_list_ip.get(ip) == null) {
                        callables.add(() -> node_scan_ports(ip, included_port, excluded_port, timeout, retries));
                    }
                }
            } else {
                long addr = interval[0];
                String ip = utils.networkToIPAddress(addr);
                if (exclude_list_ip.get(ip) == null) {
                    callables.add(() -> node_scan_ports(ip, included_port, excluded_port, timeout, retries));
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
                        result.put(res[0], res[1]);
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

}
