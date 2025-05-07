package ru.kos.neb.neb_lib;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

public class GetName {

    public static Logger logger;

    public GetName() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public Map<String, String> get(ArrayList<String> nodes, int timeout_thread) {
        Map<String, String> result = new HashMap();

        if (!nodes.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String node : nodes) {
                    callables.add(() -> getName(node));
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
        return result;

    }

    private String[] getName(String node) {
        String[] result = new String[2];
        try {
            InetAddress ia = InetAddress.getByName(node);
            if (!ia.getCanonicalHostName().equals(node)) {
                result[0] = node;
                result[1] = ia.getCanonicalHostName().split("\\.")[0].toLowerCase();
                return result;
            }
        } catch (UnknownHostException _) {

        }
        return result;
    }

}
