package ru.kos.neb.neb_lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
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

public class PingPool {

    private final int timeout = 3;
    private final int retries = 2;
//    private final int timeout_thread = 1; // min
    public static Logger logger;

    Utils utils = new Utils();

    public PingPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public Map<String, String> get(ArrayList<String> list_ip, int timeout_thread) {
        return PingPool.this.getProc(list_ip, timeout_thread, this.timeout, this.retries);
    }

    public Map<String, String> get(ArrayList<String> list_ip, int timeout_thread, int timeout) {
        return PingPool.this.getProc(list_ip, timeout_thread, timeout, this.retries);
    }

    public Map<String, String> get(ArrayList<String> list_ip, int timeout_thread, int timeout, int retries) {
        return PingPool.this.getProc(list_ip, timeout_thread, timeout, retries);
    }

    private Map<String, String> getProc(ArrayList<String> list_ip, int timeout_thread, int timeout, int retries) {
        Map<String, String> result = new HashMap();

        if (!list_ip.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String node : list_ip) {
                    callables.add(() -> getPing(node, timeout, retries));
                }

                try {
                    for (Future<String[]> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        String[] res = null;
                        try {
                            res = f.get(timeout* 2L, TimeUnit.SECONDS);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }                            
                        if(res != null)
                            result.put(res[0], res[1]);                                            
                    }                 
                } catch (InterruptedException | ExecutionException | CancellationException ex) {
                    System.out.println("Service timeout Exception!!!");
                }
            }
            for(String item : list_ip) {
                result.putIfAbsent(item, "err");
            }
        }
        return result;
    }

    @SuppressWarnings("SleepWhileInLoop")
    private String[] getPing(String node, int timeout, int retries) {
        String[] result = new String[2];
        try {
            // Get OS
            String os = getOsName();
            if (os.equals("windows")) {
                try {
                    ProcessBuilder theProcess1 = new ProcessBuilder("ping.exe", "-n", String.valueOf(retries), "-w", String.valueOf(timeout*1000), node);
                    theProcess1 = theProcess1.redirectErrorStream(true);
                    Process theProcess = theProcess1.start();
                    InputStream o = theProcess.getInputStream();

                    InputStreamReader ir = new InputStreamReader(o);
                    BufferedReader inStream2 = new BufferedReader(ir);
                    String line;
                    int ok_priznak = 0;
//                    boolean end = false;
                    while ((line = inStream2.readLine()) != null) {
                        //                    System.out.println("in-"+line);
                        if (!line.trim().isEmpty()) {
                            if (line.matches(".+(\\d+%\\s\\S+).+")) {
                                if (!line.matches(".+(100%\\s\\S+).+")) {
                                    ok_priznak++;
                                }
                            }
                            if (line.matches(".+" + node + ":.+TTL=\\d+$")) {
                                ok_priznak++;
                            }
                            //                             if(line.indexOf(address+":") >= 0 && line.indexOf("TTL=") >= 0) ok_priznak++;
                        }

                    }
                    result[0] = node;
                    if (ok_priznak >= 2) {
                        result[1] = "ok";
                    } else {
                        result[1] = "err";
                    }
                    try { Thread.sleep(1000); } catch (InterruptedException _) {}
                    return result;
                } catch (IOException e) {
                    result[0] = node;
                    result[1] = "err";
                    try { Thread.sleep(1000); } catch (InterruptedException _) {}
                    return result;
                }
            } else {
                try {
                    InetAddress aHost = InetAddress.getByName(node);
                    for (int i = 0; i < retries; i++) {
                        if (aHost.isReachable(timeout)) {
                            result[0] = node;
                            result[1] = "ok";
                            try { Thread.sleep(1000); } catch (InterruptedException _) {}
                            return result;
                        } else {
                            result[0] = node;
                            result[1] = "err";
                            try { Thread.sleep(1000); } catch (InterruptedException _) {}
                            return result;
                        }
                    }
                } catch (IOException ex) {
                    result[0] = node;
                    result[1] = "err";
                    try { Thread.sleep(1000); } catch (InterruptedException _) {}
                    return result;
                }
            }
        } catch (Exception ex) {
            result[0] = node;
            result[1] = "err";
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
            return result;
        }
        result[0] = node;
        result[1] = "err";
        try { Thread.sleep(1000); } catch (InterruptedException _) {}
        return result;
    }

    private static String getOsName() {
        String os = "";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            os = "windows";
        } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            os = "linux";
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            os = "mac";
        }

        return os;
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, int timeout_thread) {
        return getProc(network, exclude_list_ip, timeout_thread, this.timeout, this.retries);
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, int timeout_thread, int timeout) {
        return getProc(network, exclude_list_ip, timeout_thread, timeout, this.retries);
    }

    public Map<String, String> get(String network, Map<String, String> exclude_list_ip, int timeout_thread, int timeout, int retries) {
        return getProc(network, exclude_list_ip, timeout_thread, timeout, retries);
    }

    private Map<String, String> getProc(String network, Map<String, String> exclude_list_ip, int timeout_thread, int timeout, int retries) {
        Map<String, String> result = new HashMap();
        long[] interval = utils.intervalNetworkAddress(network);

        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//        try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
            CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip = utils.networkToIPAddress(addr);
                    if (exclude_list_ip.get(ip) == null) {
                        callables.add(() -> getPing(ip, timeout, retries));
                    }
                }
            } else {
                long addr = interval[0];
                String ip = utils.networkToIPAddress(addr);
                if (exclude_list_ip.get(ip) == null) {
                    callables.add(() -> getPing(ip, timeout, retries));
                }
            }

            try {
                for (Future<String[]> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                    String[] res = null;
                    try {
                        res = f.get(timeout* 2L, TimeUnit.SECONDS);
                    } catch (java.util.concurrent.TimeoutException ex) {
                        f.cancel(true);
                        System.out.println("Future Exception CancellationException!!!");
                    }
                    if(res != null)
                        result.put(res[0], res[1]);
                }            
            } catch (InterruptedException | ExecutionException | CancellationException ex) {
                System.out.println("Service timeout Exception!!!");
            }
        }

        return result;
    }
}
