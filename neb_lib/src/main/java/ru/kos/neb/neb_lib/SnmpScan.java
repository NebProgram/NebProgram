package ru.kos.neb.neb_lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.TSM;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpScan {

    public static Snmp snmp = new Snmp();
    Utils utils = new Utils();
    public static Logger logger;

    public SnmpScan() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread, int port) {
        int timeout = 3; // in sec
        int retries = 1;
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        return scan(network, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        return scan(network, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread, Map<String, String[]> snmp_accounts_priority) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return scan(network, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(String network, Map<String, String> exclude_list_ip,
            ArrayList<String> community_list, String oid, int timeout_thread, int port, int timeout,
            int retries, Map<String, String[]> snmp_accounts_priority) {
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
        
        TransportMapping transport;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException ex) {
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//        try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
            long[] interval = utils.intervalNetworkAddress(network);
            CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip = utils.networkToIPAddress(addr);
                    if (exclude_list_ip.get(ip) == null) {
                        if(snmp_accounts_priority != null)
                            callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, snmp_accounts_priority.get(ip)));
                        else
                            callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, null));
                    }
                }
            } else {
                long addr = interval[0];
                String ip = utils.networkToIPAddress(addr);
                if (exclude_list_ip.get(ip) == null) {
                    if(snmp_accounts_priority != null)
                        callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, snmp_accounts_priority.get(ip)));
                    else
                        callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, null));
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
        if(err_original != null) 
            System.setErr(err_original);
        try { snmp.close(); } catch (IOException _) {}
        return result;
    }

    private String[] getOids(String node, String oid, ArrayList<String> community_list, int port, int timeout, int retries, String[] snmp_accounts_priority) {
        String[] result = new String[3];
        try {
            if (snmp_accounts_priority != null && snmp_accounts_priority.length == 2) {
                String community = snmp_accounts_priority[0];
                String version = snmp_accounts_priority[1];
                if (snmpRequest(node, oid, community, version, port, timeout, retries)) {
                    String[] mas = new String[3];
                    mas[0] = node;
                    mas[1] = community;
                    mas[2] = "2";
                    return mas;
                }
            }
            for (String community : community_list) {
                if(community.split("\\|").length == 1) {
                    if (snmpRequest(node, oid, community, "2", port, timeout, retries)) {
                        String[] mas = new String[3];
                        mas[0] = node;
                        mas[1] = community;
                        mas[2] = "2";
                        return mas;
                    }
                }
//                try { Thread.sleep(100); } catch (InterruptedException ex) {}
            }
            for (String community : community_list) {
                if(community.split("\\|").length == 1) {
                    if (snmpRequest(node, oid, community, "1", port, timeout, retries)) {
                        String[] mas = new String[3];
                        mas[0] = node;
                        mas[1] = community;
                        mas[2] = "1";                    
                        return mas;
                    }
                }
//                try { Thread.sleep(100); } catch (InterruptedException ex) {}
            }
            for (String community : community_list) {
                if(community.split("\\|").length > 1) {
                    if (snmpRequest(node, oid, community, "3", port, timeout, retries)) {
                        String[] mas = new String[3];
                        mas[0] = node;
                        mas[1] = community;
                        mas[2] = "3";                    
                        return mas;
                    }
                }
//                try { Thread.sleep(100); } catch (InterruptedException ex) {}
            }            
        } catch (Exception ex) {
            logger.println(node+" --- "+ex.getMessage(), logger.DEBUG);
//            System.out.println(SnmpScan.class.getName());
        }
        return result;
    }

    private boolean snmpRequest(String node, String oid, String community, String version, int port, int timeout, int retries) {
        boolean result = false;
        try {
            if(version.equals("1") || version.equals("2")) {
                community = utils.decrypt(Utils.master_key, community);
                if (community != null) {
                    Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                    CommunityTarget target = new CommunityTarget();
                    target.setCommunity(new OctetString(community));
                    target.setAddress(targetAddress);
                    target.setRetries(retries-1);
                    target.setTimeout(timeout * 1000L);
                    if (version.equals("2")) {
                        target.setVersion(SnmpConstants.version2c);
                    } else {
                        target.setVersion(SnmpConstants.version1);
                    }

                    PDU pdu = new PDU();
                    //            OID startOid = new OID(oid);
                    pdu.add(new VariableBinding(new OID(oid)));
                    pdu.setType(PDU.GET);

                    ResponseEvent event = SnmpScan.snmp.send(pdu, target);
                    if (event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
//                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            result = switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" ->
                                    false;
                                default ->
                                    true;
                            };
                        }
                    }
                }
            } else if(version.equals("3")) {
                String[] mas = community.split("\\|");
                if(mas.length == 5) {
                    String user = mas[0];
                    String authProtocol_type = mas[1];
                    String auth_paswd = utils.decrypt(Utils.master_key, mas[2]);
                    String privProtocol_type = mas[3];
                    String priv_paswd = utils.decrypt(Utils.master_key, mas[4]);

                    if (user != null && 
                            (auth_paswd != null || priv_paswd != null) && 
                            (authProtocol_type != null || privProtocol_type != null)) {

                        OctetString localEngineId = new OctetString(MPv3.createLocalEngineID());

                        USM usm = new USM(SecurityProtocols.getInstance(), localEngineId, 0);
                        SecurityModels.getInstance().addSecurityModel(usm);

                        OctetString securityName = new OctetString(user);
                        OID authProtocol = AuthSHA.ID;
                        if(authProtocol_type != null && authProtocol_type.equalsIgnoreCase("SHA")) {
                            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
                            authProtocol = AuthSHA.ID;
                        }
                        else if(authProtocol_type != null && authProtocol_type.equalsIgnoreCase("MD5")) {
                            SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());
                            authProtocol = AuthMD5.ID;
                        }

                        OID privProtocol = PrivDES.ID;
                        if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("DES")) {
                            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivDES());
                            privProtocol = PrivDES.ID;
                        }
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("3DES")) {
                            SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
                            privProtocol = Priv3DES.ID; 
                        }
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES128")) {
                            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
                            privProtocol = PrivAES128.ID;  
                        }
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES192")) {
                            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
                            privProtocol = PrivAES192.ID;
                        }
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES256")) {
                            SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());
                            privProtocol = PrivAES256.ID;
                        }

                        OctetString authPassphrase = new OctetString(auth_paswd);
                        OctetString privPassphrase = new OctetString(priv_paswd);

                        snmp.getUSM().addUser(securityName, new UsmUser(securityName, authProtocol, authPassphrase, privProtocol, privPassphrase));
                        SecurityModels.getInstance().addSecurityModel(new TSM(localEngineId, false));

                        UserTarget target = new UserTarget();
                        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                        target.setSecurityName(securityName);

            //            target.setAddress(GenericAddress.parse(String.format("udp:%s/%s", ip, port_number)));
                        Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                        target.setAddress(targetAddress);
                        target.setVersion(SnmpConstants.version3);
                        target.setRetries(retries-1);
                        target.setTimeout(timeout * 1000L);

                        PDU pdu = new ScopedPDU();
                        pdu.add(new VariableBinding(new OID(oid)));
                        pdu.setType(PDU.GET);

                        ResponseEvent event = snmp.send(pdu, target);
                        if (event != null) {
                            PDU response = event.getResponse();
                            if (response != null) {
//                                String oidstr = response.get(0).getOid().toString();
                                String val = response.get(0).getVariable().toString();
                                result = switch (val) {
                                    case "Null", "noSuchObject", "noSuchInstance" ->
                                        false;
                                    default ->
                                        true;
                                };
                            }
                        }
                    }
                }                
            }
        } catch (IOException ex) {
            logger.println(node+" - "+ex.getMessage(), logger.DEBUG);
//            System.out.println(SnmpScan.class.getName());
        }
        return result;
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, int timeout_thread, String oid, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        return scan(list_ip, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 1;
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        return scan(list_ip, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread, int timeout, int retries) {
        int port = 161;
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        return scan(list_ip, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout_thread, int timeout, int retries, Map<String, String[]> snmp_accounts_priority) {
        int port = 161;
        return scan(list_ip, exclude_list_ip, community, oid, timeout_thread, port, timeout, retries, snmp_accounts_priority);
    }

    public ArrayList<String[]> scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip,
            ArrayList<String> community_list, String oid, int timeout_thread, int port, int timeout, int retries,
            Map<String, String[]> snmp_accounts_priority) {
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

        TransportMapping transport;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException ex) {
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        if (!list_ip.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String ip : list_ip) {
                    if(snmp_accounts_priority != null)
                        callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, snmp_accounts_priority.get(ip)));
                    else
                        callables.add(() -> getOids(ip, oid, community_list, port, timeout, retries, null));
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
        if(err_original != null) 
            System.setErr(err_original);
        try { snmp.close(); } catch (IOException _) {}
        return result;
    }
////////////////////////////////////////////////
    public ArrayList<String[]> scan_test(ArrayList<String> list_ip, Map<String, String> exclude_list_ip,
            ArrayList<String> community_list, String oid, int timeout_thread, int port, int timeout, int retries,
            Map<String, String[]> snmp_accounts_priority) {
        ArrayList<String[]> result = new ArrayList();

        TransportMapping transport;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException ex) {
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        if (!list_ip.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                @SuppressWarnings("unused")
                CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
                for (String ip : list_ip) {
                    service.execute(() -> {
//                        String[] result1 = new String[3];
                        try {

                            for (String community : community_list) {
                                if (snmpRequest(ip, oid, community, "2", port, timeout, retries)) {
                                    String[] mas = new String[3];
                                    mas[0] = ip;
                                    mas[1] = community;
                                    mas[2] = "2";
                                    result.add(mas);
                                }
                            }
                            for (String community : community_list) {
                                if (snmpRequest(ip, oid, community, "1", port, timeout, retries)) {
                                    String[] mas = new String[3];
                                    mas[0] = ip;
                                    mas[1] = community;
                                    mas[2] = "1";
                                    result.add(mas);
                                }
                            }
                        } catch (Exception ex) {
                            logger.println(ex.getMessage(), logger.DEBUG);
                    //            System.out.println(SnmpScan.class.getName());
                        }
                    });                            
                }
            }
        }

        try { snmp.close(); } catch (IOException _) {}
        return result;
    } 

}
