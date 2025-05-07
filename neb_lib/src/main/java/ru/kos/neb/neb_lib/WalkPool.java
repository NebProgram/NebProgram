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

public class WalkPool {

    public static Snmp snmp = new Snmp();
    public static Logger logger;

    private final int port = 161;
    private final int timeout = 3;
    private final int timeout_thread = 1; // min
    private final int retries = 2;
//    public int bulk_size = 10;

    private final Utils utils = new Utils();
    
    private final Map<String, String> out = new HashMap();

    public WalkPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_ip, String oid, int timeout_thread) {
        return WalkPool.this.getProc(list_ip, oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_ip, String oid, int timeout_thread, int port) {
        return WalkPool.this.getProc(list_ip, oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_ip, String oid, int timeout_thread, int port, int timeout) {
        return WalkPool.this.getProc(list_ip, oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_ip, String oid, int timeout_thread, int port, int timeout, int retries) {
        return WalkPool.this.getProc(list_ip, oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_ip, String oid, int timeout_thread, int port, int timeout, int retries, int bulk_size) {
        return WalkPool.this.getProc(list_ip, oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, Boolean> test(ArrayList<String[]> node_community_version_list, String oid, int timeout_thread, int port, int timeout, int retries, int bulk_size) {
        Map<String, Boolean> out_test = new HashMap();
        Map<String, ArrayList<String[]>> result = getProc(node_community_version_list, oid, timeout_thread, port, timeout, retries, bulk_size, true);
        if (result != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : result.entrySet()) {
                String node = entry.getKey();
                ArrayList val = entry.getValue();
                if (val != null && val.size() > 1) {
                    out_test.put(node, Boolean.TRUE);
                } else {
                    out_test.put(node, Boolean.FALSE);
                }
            }
        }
        for(String[] item : node_community_version_list) {
            String node = item[0];
            if(result != null && result.get(node) == null) {
                out_test.put(node, Boolean.FALSE);
            }
        }
        return out_test;
    }

    private Map<String, ArrayList<String[]>> getProc(ArrayList<String[]> node_community_version_list, String oid, int timeout_thread, int port, int timeout, int retries, int bulk_size, boolean test) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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
            System.out.println("IOException transport.listen()");
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        if (!node_community_version_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {               
                 CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (String[] node_community_version : node_community_version_list) {
                    callables.add(() -> snmpWalk(node_community_version, oid, port, timeout, retries, bulk_size, test));
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }
                        if(res1 != null && !res1.isEmpty())
                            result.putAll(res1);
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

    private Map<String, ArrayList<String[]>> snmpWalk(String[] node_community_version, String oid, int port, int timeout, int retries, int bulk_size, boolean test) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        Map<String, String> oids_map = new HashMap();
        String start_oid = oid;

        try {
            String node = node_community_version[0];

            if(node_community_version[2].equals("1") || node_community_version[2].equals("2")) {
                String community = utils.decrypt(Utils.master_key, node_community_version[1]);

                Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(community));
                target.setAddress(targetAddress);
                target.setRetries(retries-1);
                target.setTimeout(timeout * 1000L);
                target.setVersion(SnmpConstants.version1);
                if (node_community_version[2].equals("2")) {
                    target.setVersion(SnmpConstants.version2c);
                }

                PDU pdu = new PDU();
    //            OID startOid = new OID(oid);
                pdu.add(new VariableBinding(new OID(oid)));
                if (node_community_version[2].equals("2")) {
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);
                } else {
                    pdu.setType(PDU.GETNEXT);
                }

                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                if(event != null) {
                    PDU response = event.getResponse();
                    if (response != null) {
                        String cur_OID = "";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) {
                            ArrayList<String[]> res = new ArrayList();
                            for (VariableBinding var : response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[2];
                                    mas[0] = var.getOid().toString();
                                    mas[1] = var.getVariable().toString();

                                    if (oids_map.get(mas[0]) == null) {
                                        oids_map.put(mas[0], mas[0]);
                                        res.add(mas);
                                    }
                                }
                            }
                            if(!res.isEmpty()) {
                                Map<String, String> uniqal_oid = new HashMap();
                                ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                boolean fail = (boolean)res_list.get(1);
                                if (!test && !fail) {
                                    while(true) {
                                        Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_community_version, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                        if(!res1.isEmpty()) {
                                            res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                            fail = (boolean)res_list.get(1); 
                                            if(fail) {
//                                                System.out.println("Cycling for node: "+node+" - "+oid);
                                                break;
                                            }
                                            cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                        }
                                        else
                                            break;
                                    }
                                }
                            }
                        }
                    } else {
                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                        return result;
                    }
                }
            } else if(node_community_version[2].equals("3")) {
                String[] mas = node_community_version[1].split("\\|");
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
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);                    
                    

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    if(event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String cur_OID = "";
                            String ip_port = event.getPeerAddress().toString();
                            if (ip_port != null) {
                                ArrayList<String[]> res = new ArrayList();
                                for (VariableBinding var : response.toArray()) {
                                    cur_OID = var.getOid().toString();
                                    if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                        mas = new String[2];
                                        mas[0] = var.getOid().toString();
                                        mas[1] = var.getVariable().toString();

                                        if (oids_map.get(mas[0]) == null) {
                                            oids_map.put(mas[0], mas[0]);
                                            res.add(mas);
                                        }
                                    }
                                }
                                if(!res.isEmpty()) {
                                    Map<String, String> uniqal_oid = new HashMap();
                                    ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                    result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                    boolean fail = (boolean)res_list.get(1);
                                    if (!test && !fail) {
                                        while(true) {
                                            Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_community_version, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                            if(!res1.isEmpty()) {
                                                res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                fail = (boolean)res_list.get(1); 
                                                if(fail) {
//                                                    System.out.println("Cycling for node: "+node+" - "+oid);
                                                    break;
                                                }
                                                cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                            }
                                            else
                                                break;
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.println(node+": "+node_community_version[1]+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                            return result;
                        }
                    }

                }                
            }
            
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
            
        } catch (IOException ex) {
//            ex.printStackTrace();
            return result;
        }
        return result;
    }
    
    private Map<String, ArrayList<String[]>> snmpWalkNext(String[] node_community_version, String oid, int port, int timeout, int retries, int bulk_size, String start_oid) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        
        Map<String, String> oids_map = new HashMap();

        try {
            String node = node_community_version[0];
            if(node_community_version[2].equals("1") || node_community_version[2].equals("2")) {
                String community = utils.decrypt(Utils.master_key, node_community_version[1]);

                Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(community));
                target.setAddress(targetAddress);
                target.setRetries(retries-1);
                target.setTimeout(timeout * 1000L);
                target.setVersion(SnmpConstants.version1);
                if (node_community_version[2].equals("2")) {
                    target.setVersion(SnmpConstants.version2c);
                }

                PDU pdu = new PDU();
    //            OID startOid = new OID(oid);
                pdu.add(new VariableBinding(new OID(oid)));
                if (node_community_version[2].equals("2")) {
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);
                } else {
                    pdu.setType(PDU.GETNEXT);
                }

                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                if (event != null ) {
                    PDU response = event.getResponse();
                    if (response != null) {
                        String cur_OID = "";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) {
                            ArrayList<String[]> res = new ArrayList();
                            for (VariableBinding var : response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[2];
                                    mas[0] = var.getOid().toString();
                                    mas[1] = var.getVariable().toString();
                                    logger.println("\t--- Next "+node+": "+community+"|"+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                    if (oids_map.get(mas[0]) == null) {
                                        oids_map.put(mas[0], mas[0]);
                                        res.add(mas);
                                    }
                                }
                            }
                            if(!res.isEmpty())
                                result.put(node, res);
                        }
                    } else {
                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                        return result;                   
                    }
                }
            } else if(node_community_version[2].equals("3")) {
                String[] mas = node_community_version[1].split("\\|");
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
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);                    

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    if (event != null ) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String cur_OID = "";
                            String ip_port = event.getPeerAddress().toString();
                            if (ip_port != null) {
                                ArrayList<String[]> res = new ArrayList();
                                for (VariableBinding var : response.toArray()) {
                                    cur_OID = var.getOid().toString();
                                    if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                        mas = new String[2];
                                        mas[0] = var.getOid().toString();
                                        mas[1] = var.getVariable().toString();
                                        logger.println("\t--- Next "+node+": "+node_community_version[1]+"|"+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                        if (oids_map.get(mas[0]) == null) {
                                            oids_map.put(mas[0], mas[0]);
                                            res.add(mas);
                                        }
                                    }
                                }
                                if(!res.isEmpty())
                                    result.put(node, res);
                            }
                        } else {
                            logger.println(node+": "+node_community_version[1]+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                            return result;                   
                        }
                    }

                }                
                
            }
        } catch (IOException ex) {
//            ex.printStackTrace();
            return result;
        }
        return result;
    }    
    
    private Map<String, ArrayList<String[]>> snmpWalkNotBulkNext(String[] node_community_version, String oid, int port, int timeout, int retries, String start_oid) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        
        Map<String, String> oids_map = new HashMap();

        try {
            String node = node_community_version[0];
            if(node_community_version[2].equals("1") || node_community_version[2].equals("2")) {
                String community = utils.decrypt(Utils.master_key, node_community_version[1]);

                Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(community));
                target.setAddress(targetAddress);
                target.setRetries(retries-1);
                target.setTimeout(timeout * 1000L);
                target.setVersion(SnmpConstants.version1);
                if (node_community_version[2].equals("2")) {
                    target.setVersion(SnmpConstants.version2c);
                }

                PDU pdu = new PDU();
    //            OID startOid = new OID(oid);
                pdu.add(new VariableBinding(new OID(oid)));
                pdu.setType(PDU.GETNEXT);

                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                if (event != null) {
                    PDU response = event.getResponse();
                    if (response != null) {
                        String cur_OID = "";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) {
                            ArrayList<String[]> res = new ArrayList();
                            for (VariableBinding var : response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[2];
                                    mas[0] = var.getOid().toString();
                                    mas[1] = var.getVariable().toString();
                                    logger.println("\t--- Next "+node+": "+community+"|"+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                    if (oids_map.get(mas[0]) == null) {
                                        oids_map.put(mas[0], mas[0]);
                                        res.add(mas);
                                    }
                                }
                            }
                            if(!res.isEmpty())
                                result.put(node, res);
                        }
                    } else {
                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                        return result;                    
                    }
                }
            } else if(node_community_version[2].equals("3")) {
                String[] mas = node_community_version[1].split("\\|");
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
                    pdu.setType(PDU.GETNEXT);

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    if (event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String cur_OID = "";
                            String ip_port = event.getPeerAddress().toString();
                            if (ip_port != null) {
                                ArrayList<String[]> res = new ArrayList();
                                for (VariableBinding var : response.toArray()) {
                                    cur_OID = var.getOid().toString();
                                    if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                        mas = new String[2];
                                        mas[0] = var.getOid().toString();
                                        mas[1] = var.getVariable().toString();
                                        logger.println("\t--- Next "+node+": "+node_community_version[1]+"|"+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                        if (oids_map.get(mas[0]) == null) {
                                            oids_map.put(mas[0], mas[0]);
                                            res.add(mas);
                                        }
                                    }
                                }
                                if(!res.isEmpty())
                                    result.put(node, res);
                            }
                        } else {
                            logger.println(node+": "+node_community_version[1]+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                            return result;                    
                        }
                    }
                }                
            }
        } catch (IOException ex) {
//            ex.printStackTrace();
            return result;
        }
        return result;
    }    

////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList<String[]>> getNodeMultiCommunityVersion(ArrayList<ArrayList> node_multicommunity_version_list, String oid, int timeout_thread, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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
            System.out.println("IOException transport.listen()");
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        if (!node_multicommunity_version_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (ArrayList node_community_ver : node_multicommunity_version_list) {
                    callables.add(() -> snmpWalkMultiComminity(node_community_ver, oid, port, timeout, retries, Utils.BULKSIZE));
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }
                        if(res1 != null && !res1.isEmpty())
                            result.putAll(res1);                         
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

    private Map<String, ArrayList<String[]>> snmpWalkMultiComminity(ArrayList node_multicommunity_version, String oid, int port, int timeout, int retries, int bulk_size) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        String start_oid = oid;

        try {
            String node = (String) node_multicommunity_version.get(0);
            ArrayList<String> community_list = (ArrayList<String>) node_multicommunity_version.get(1);
            String version = (String) node_multicommunity_version.get(2);
            for (String community : community_list) {
//                logger.println("Start "+node+"|"+community+"|"+version+"|"+oid, logger.DEBUG);
                Map<String, String> oids_map = new HashMap();
                if (community != null && !community.isEmpty()) {
                    if(version.equals("1") || version.equals("2")) {
                        community = utils.decrypt(Utils.master_key, community);
                        if (community != null) {
                            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                            CommunityTarget target = new CommunityTarget();
                            target.setCommunity(new OctetString(community));
                            target.setAddress(targetAddress);
                            target.setRetries(retries-1);
                            target.setTimeout(timeout * 1000L);
                            target.setVersion(SnmpConstants.version1);
                            if (version.equals("2")) {
                                target.setVersion(SnmpConstants.version2c);
                            }

                            PDU pdu = new PDU();
                            pdu.add(new VariableBinding(new OID(oid)));

                            if (version.equals("2")) {
                                pdu.setType(PDU.GETBULK);
                                pdu.setMaxRepetitions(Utils.BULKSIZE);
                            } else {
                                pdu.setType(PDU.GETNEXT);
                            }

                            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                            if (event != null) {
                                PDU response = event.getResponse();
                                if (response != null) {
                                    boolean next_send = false;
                                    String cur_OID = "";
                                    String ip_port = event.getPeerAddress().toString();
                                    if (ip_port != null) {
                                        ArrayList<String[]> res = new ArrayList();
                                        for (VariableBinding var : response.toArray()) {
                                            cur_OID = var.getOid().toString();
                                            if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                String[] mas = new String[2];
                                                mas[0] = var.getOid().toString();
                                                mas[1] = var.getVariable().toString();
                                                if (oids_map.get(mas[0]) == null) {
                                                    oids_map.put(mas[0], mas[0]);
                                                    res.add(mas);
                                                    next_send = true;
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            } else {
                                                next_send = false;
                                                break;
                                            }
                                        }
                                        if(!res.isEmpty()) {
                                            Map<String, String> uniqal_oid = new HashMap();
                                            ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                            boolean fail = (boolean)res_list.get(1);
                                            if (next_send && !fail) {
                                                while(true) {
                                                    String[] node_comm_ver = new String[3];
                                                    node_comm_ver[0] = node;
                                                    node_comm_ver[1] = community;
                                                    node_comm_ver[2] = version;                                        
                                                    Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_comm_ver, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                                    if(!res1.isEmpty()) {
                                                        res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                        result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                        fail = (boolean)res_list.get(1); 
                                                        if(fail) {
//                                                            System.out.println("Cycling for node: "+node+" - "+oid);
                                                            break;
                                                        }
                                                        cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                                    }
                                                    else {
                                                        break;
                                                    }
                                                } 
                                            }
                                        }
                                    }
                                } else {
                                    logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                    return result;                                
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
                                pdu.setType(PDU.GETBULK);
                                pdu.setMaxRepetitions(Utils.BULKSIZE);

                                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                                if (event != null) {
                                    PDU response = event.getResponse();
                                    if (response != null) {
                                        boolean next_send = false;
                                        String cur_OID = "";
                                        String ip_port = event.getPeerAddress().toString();
                                        if (ip_port != null) {
                                            ArrayList<String[]> res = new ArrayList();
                                            for (VariableBinding var : response.toArray()) {
                                                cur_OID = var.getOid().toString();
                                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                    mas = new String[2];
                                                    mas[0] = var.getOid().toString();
                                                    mas[1] = var.getVariable().toString();
                                                    if (oids_map.get(mas[0]) == null) {
                                                        oids_map.put(mas[0], mas[0]);
                                                        res.add(mas);
                                                        next_send = true;
                                                    } else {
                                                        next_send = false;
                                                        break;
                                                    }
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            }
                                            if(!res.isEmpty()) {
                                                Map<String, String> uniqal_oid = new HashMap();
                                                ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                boolean fail = (boolean)res_list.get(1);
                                                if (next_send && !fail) {
                                                    while(true) {
                                                        String[] node_comm_ver = new String[3];
                                                        node_comm_ver[0] = node;
                                                        node_comm_ver[1] = community;
                                                        node_comm_ver[2] = version;                                        
                                                        Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_comm_ver, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                                        if(!res1.isEmpty()) {
                                                            res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                            fail = (boolean)res_list.get(1); 
                                                            if(fail) {
//                                                                System.out.println("Cycling for node: "+node+" - "+oid);
                                                                break;
                                                            }
                                                            cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                                        }
                                                        else {
                                                            break;
                                                        }
                                                    } 
                                                }
                                            }
                                        }
                                    } else {
                                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                        return result;                                
                                    }
                                }
                            }
                        }                        
                    }
                }
            }
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
        } catch (IOException ex) {
            return result;
        }
        return result;
    }

//////////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList<String[]>> getNodeMultiCommunityVersionOid(ArrayList<ArrayList> node_multicommunity_version_oid_list, int timeout_thread, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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
            System.out.println("IOException transport.listen()");
            try { snmp.close(); } catch (IOException _) {}
            return result;
        }

        if (!node_multicommunity_version_oid_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (ArrayList node_community_ver_oid : node_multicommunity_version_oid_list) {
//                    synchronized (callables) {
                        callables.add(() -> snmpWalkMultiComminityOid(node_community_ver_oid, port, timeout, retries, Utils.BULKSIZE, null));
//                    }
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }
                        if(res1 != null && !res1.isEmpty())
                            result.putAll(res1);                          
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
    
    private Map<String, ArrayList<String[]>> snmpWalkMultiComminityOid(ArrayList node_multicommunity_version_oid, int port, int timeout, int retries, int bulk_size, String start_oid) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        String oid = (String) node_multicommunity_version_oid.get(3);
        if(start_oid == null)
            start_oid = oid;

        try {
            String node = (String) node_multicommunity_version_oid.get(0);
            ArrayList<String> community_list = (ArrayList<String>) node_multicommunity_version_oid.get(1);
            String version = (String) node_multicommunity_version_oid.get(2);
//            String oid = (String) node_multicommunity_version_oid.get(3);
            logger.println("Start "+node+": "+oid, logger.DEBUG);
            for (String community : community_list) {
//                logger.println("Start "+node+": "+community+"|"+version+"|"+oid, logger.DEBUG);
                Map<String, String> oids_map = new HashMap();
                if (community != null && !community.isEmpty()) {
                    if(version.equals("1") || version.equals("2")) {
                        community = utils.decrypt(Utils.master_key, community);
                        if (community != null) {
    //                        logger.println("Start "+node+": "+community+"|"+version+"|"+oid, logger.DEBUG);
                            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                            CommunityTarget target = new CommunityTarget();
                            target.setCommunity(new OctetString(community));
                            target.setAddress(targetAddress);
                            target.setRetries(retries-1);
                            target.setTimeout(timeout * 1000L);
                            target.setVersion(SnmpConstants.version1);
                            if (version.equals("2")) {
                                target.setVersion(SnmpConstants.version2c);
                            }

                            PDU pdu = new PDU();
                            pdu.add(new VariableBinding(new OID(oid)));

                            if (version.equals("2")) {
                                pdu.setType(PDU.GETBULK);
                                pdu.setMaxRepetitions(Utils.BULKSIZE);
                            } else {
                                pdu.setType(PDU.GETNEXT);
                            }

                            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                            if (event != null) {
                                PDU response = event.getResponse();
                                if (response != null) {
                                    boolean next_send = false;
                                    String cur_OID = "";
                                    String ip_port = event.getPeerAddress().toString();
                                    if (ip_port != null) {
                                        ArrayList<String[]> res = new ArrayList();
                                        for (VariableBinding var : response.toArray()) {
                                            cur_OID = var.getOid().toString();
                                            if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                String[] mas = new String[2];
                                                mas[0] = var.getOid().toString();
                                                mas[1] = var.getVariable().toString();
                                                logger.println("\t--- Start "+node+": "+community+"|"+version+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                                if (oids_map.get(mas[0]) == null) {
                                                    oids_map.put(mas[0], mas[0]);
                                                    res.add(mas);
                                                    next_send = true;
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            } else {
                                                next_send = false;
                                                break;
                                            }
                                        }
                                        if(!res.isEmpty()) {
                                            Map<String, String> uniqal_oid = new HashMap();
                                            ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                            boolean fail = (boolean)res_list.get(1);
                                            if (next_send && !fail) {
                                                while(true) {
                                                    String[] node_comm_ver = new String[3];
                                                    node_comm_ver[0] = node;
                                                    node_comm_ver[1] = community;
                                                    node_comm_ver[2] = version;                                        
                                                    Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_comm_ver, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                                    if(!res1.isEmpty()) {
                                                        res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                        result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                        fail = (boolean)res_list.get(1); 
                                                        if(fail) {
//                                                            System.out.println("Cycling for node: "+node+" - "+oid);
                                                            break;
                                                        }
                                                        cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                                    }
                                                    else {
                                                        break;
                                                    }
                                                }                                    
                                            }
                                        }
                                    }
                                } else {
                                    logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                    return result;
                                }
                            }
    //                        logger.println("Stop "+node+": "+community+"|"+version+"|"+oid, logger.DEBUG);
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
                                pdu.setType(PDU.GETBULK);
                                pdu.setMaxRepetitions(Utils.BULKSIZE);                            

                                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                                if (event != null) {
                                    PDU response = event.getResponse();
                                    if (response != null) {
                                        boolean next_send = false;
                                        String cur_OID = "";
                                        String ip_port = event.getPeerAddress().toString();
                                        if (ip_port != null) {
                                            ArrayList<String[]> res = new ArrayList();
                                            for (VariableBinding var : response.toArray()) {
                                                cur_OID = var.getOid().toString();
                                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                    mas = new String[2];
                                                    mas[0] = var.getOid().toString();
                                                    mas[1] = var.getVariable().toString();
                                                    logger.println("\t--- Start "+node+": "+community+"|"+version+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                                    if (oids_map.get(mas[0]) == null) {
                                                        oids_map.put(mas[0], mas[0]);
                                                        res.add(mas);
                                                        next_send = true;
                                                    } else {
                                                        next_send = false;
                                                        break;
                                                    }
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            }
                                            if(!res.isEmpty()) {
                                                Map<String, String> uniqal_oid = new HashMap();
                                                ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                boolean fail = (boolean)res_list.get(1);
                                                if (next_send && !fail) {
                                                    while(true) {
                                                        String[] node_comm_ver = new String[3];
                                                        node_comm_ver[0] = node;
                                                        node_comm_ver[1] = community;
                                                        node_comm_ver[2] = version;                                        
                                                        Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_comm_ver, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                                        if(!res1.isEmpty()) {
                                                            res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                            fail = (boolean)res_list.get(1); 
                                                            if(fail) {
//                                                                System.out.println("Cycling for node: "+node+" - "+oid);
                                                                break;
                                                            }
                                                            cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                                        }
                                                        else {
                                                            break;
                                                        }
                                                    }                                    
                                                }
                                            }
                                        }
                                    } else {
                                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                        return result;
                                    }
                                }
                            }
                        }                        
                    }
                }
            }
            logger.println("Stop "+node+": "+oid, logger.DEBUG);
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
        } catch (IOException ex) {
            return result;
        }
        return result;
    }

//////////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList<String[]>> getNodeMultiCommunityVersionOidNotBulk(ArrayList<ArrayList> node_multicommunity_version_oid_list, int timeout_thread, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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

        if (!node_multicommunity_version_oid_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (ArrayList node_community_ver_oid : node_multicommunity_version_oid_list) {
                    callables.add(() -> snmpWalkMultiComminityOidNotBulk(node_community_ver_oid, port, timeout, retries));
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }
                        if(res1 != null && !res1.isEmpty())
                            result.putAll(res1);                           
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

    private Map<String, ArrayList<String[]>> snmpWalkMultiComminityOidNotBulk(ArrayList node_multicommunity_version_oid, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        String start_oid = (String) node_multicommunity_version_oid.get(3);

        try {
            String node = (String) node_multicommunity_version_oid.get(0);
            ArrayList<String> community_list = (ArrayList<String>) node_multicommunity_version_oid.get(1);
            String version = (String) node_multicommunity_version_oid.get(2);
            String oid = (String) node_multicommunity_version_oid.get(3);
            logger.println("Start "+node+": "+oid, logger.DEBUG);
            for (String community : community_list) {
                Map<String, String> oids_map = new HashMap();
                if (community != null && !community.isEmpty()) {
                    if(version.equals("1") || version.equals("2")) {
                        community = utils.decrypt(Utils.master_key, community);
                        if (community != null) {
                            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                            CommunityTarget target = new CommunityTarget();
                            target.setCommunity(new OctetString(community));
                            target.setAddress(targetAddress);
                            target.setRetries(retries-1);
                            target.setTimeout(timeout * 1000L);
                            target.setVersion(SnmpConstants.version1);
                            if (version.equals("2")) {
                                target.setVersion(SnmpConstants.version2c);
                            }

                            PDU pdu = new PDU();
                            pdu.add(new VariableBinding(new OID(oid)));

                            pdu.setType(PDU.GETNEXT);

                            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                            if (event != null) {
                                PDU response = event.getResponse();
                                if (response != null) {
                                    boolean next_send = false;
                                    String cur_OID = "";
                                    String ip_port = event.getPeerAddress().toString();
                                    if (ip_port != null) {
                                        ArrayList<String[]> res = new ArrayList();
                                        for (VariableBinding var : response.toArray()) {
                                            cur_OID = var.getOid().toString();
                                            if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                String[] mas = new String[2];
                                                mas[0] = var.getOid().toString();
                                                mas[1] = var.getVariable().toString();
                                                logger.println("\t--- Start "+node+": "+community+"|"+version+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                                if (oids_map.get(mas[0]) == null) {
                                                    oids_map.put(mas[0], mas[0]);
                                                    res.add(mas);
                                                    next_send = true;
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            } else {
                                                next_send = false;
                                                break;
                                            }
                                        }
                                        if(!res.isEmpty()) {
                                            if(result.get(node) != null)
                                               result.get(node).addAll(res);
                                            else
                                                result.put(node, res);
                                        }
                                        if (next_send) {
                                            while(true) {
                                                String[] node_comm_ver = new String[3];
                                                node_comm_ver[0] = node;
                                                node_comm_ver[1] = community;
                                                node_comm_ver[2] = version;                                        
                                                Map<String, ArrayList<String[]>> res1 = snmpWalkNotBulkNext(node_comm_ver, cur_OID, port, timeout, retries, start_oid);
                                                if(!res1.isEmpty()) {
                                                    ArrayList<String[]> res_list = res1.get(node);
                                                    for(String[] mas : res_list) {
                                                        if (oids_map.get(mas[0]) == null) {
                                                            oids_map.put(mas[0], mas[0]);
                                                            res.add(mas);
                                                        } else {
//                                                            System.out.println("Cycling for node: "+node+" - "+oid);
                                                            return result;
                                                        }
                                                    }
                                                    cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];

                                                }
                                                else {
                                                    break;
                                                }
                                            } 
                                        }                                 
                                    }
                                } else {
                                    logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                    return result;                                
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

                                pdu.setType(PDU.GETNEXT);

                                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                                if (event != null) {
                                    PDU response = event.getResponse();
                                    if (response != null) {
                                        boolean next_send = false;
                                        String cur_OID = "";
                                        String ip_port = event.getPeerAddress().toString();
                                        if (ip_port != null) {
                                            ArrayList<String[]> res = new ArrayList();
                                            for (VariableBinding var : response.toArray()) {
                                                cur_OID = var.getOid().toString();
                                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                    mas = new String[2];
                                                    mas[0] = var.getOid().toString();
                                                    mas[1] = var.getVariable().toString();
                                                    logger.println("\t--- Start "+node+": "+community+"|"+version+"|"+oid+" - "+mas[0]+" = "+mas[1], logger.DEBUG);
                                                    if (oids_map.get(mas[0]) == null) {
                                                        oids_map.put(mas[0], mas[0]);
                                                        res.add(mas);
                                                        next_send = true;
                                                    } else {
                                                        next_send = false;
                                                        break;
                                                    }
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            }
                                            if(!res.isEmpty()) {
                                                if(result.get(node) != null)
                                                   result.get(node).addAll(res);
                                                else
                                                    result.put(node, res);
                                            }
                                            if (next_send) {
                                                while(true) {
                                                    String[] node_comm_ver = new String[3];
                                                    node_comm_ver[0] = node;
                                                    node_comm_ver[1] = community;
                                                    node_comm_ver[2] = version;                                        
                                                    Map<String, ArrayList<String[]>> res1 = snmpWalkNotBulkNext(node_comm_ver, cur_OID, port, timeout, retries, start_oid);
                                                    if(!res1.isEmpty()) {
                                                        ArrayList<String[]> res_list = res1.get(node);
                                                        for(String[] mas1 : res_list) {
                                                            if (oids_map.get(mas1[0]) == null) {
                                                                oids_map.put(mas1[0], mas1[0]);
                                                                res.add(mas1);
                                                            } else {
//                                                                System.out.println("Cycling for node: "+node+" - "+oid);
                                                                return result;
                                                            }
                                                        }
                                                        cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];

                                                    }
                                                    else {
                                                        break;
                                                    }
                                                } 
                                            }                                 
                                        }
                                    } else {
                                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                        return result;                                
                                    }
                                }
                            }
                        }                        
                    }
                }
//                try { Thread.sleep(100); } catch (InterruptedException ex) {}
            }
            logger.println("Stop "+node+": "+oid, logger.DEBUG);
            try { Thread.sleep(1000); } catch (InterruptedException _) {}

        } catch (IOException ex) {
            return result;
        }
        return result;
    }

//////////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList<String[]>> getNodeCommunityVersionOidNotBulk(ArrayList<String[]> node_community_version_oid_list, int timeout_thread, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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

        if (!node_community_version_oid_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (String[] node_community_ver_oid : node_community_version_oid_list) {
                    callables.add(() -> snmpWalkComminityOidNotBulk(node_community_ver_oid, port, timeout, retries, Utils.BULKSIZE));
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }
                        if(res1 != null && !res1.isEmpty())
                            result.putAll(res1);                           
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

    private Map<String, ArrayList<String[]>> snmpWalkComminityOidNotBulk(String[] node_community_version_oid, int port, int timeout, int retries, int bulk_size) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        String start_oid = node_community_version_oid[3];

        try {
            String node = node_community_version_oid[0];
            String community = node_community_version_oid[1];
            String version = node_community_version_oid[2];
            String oid = node_community_version_oid[3];
            
            Map<String, String> oids_map = new HashMap();
            if (community != null && !community.isEmpty()) {
                if(version.equals("1") || version.equals("2")) {
                    community = utils.decrypt(Utils.master_key, community);
                    if (community != null) {
                        Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                        CommunityTarget target = new CommunityTarget();
                        target.setCommunity(new OctetString(community));
                        target.setAddress(targetAddress);
                        target.setRetries(retries-1);
                        target.setTimeout(timeout * 1000L);
                        target.setVersion(SnmpConstants.version1);
                        if (version.equals("2")) {
                            target.setVersion(SnmpConstants.version2c);
                        }

                        PDU pdu = new PDU();
                        pdu.add(new VariableBinding(new OID(oid)));

                        pdu.setType(PDU.GETNEXT);

                        ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                        if (event != null) {
                            PDU response = event.getResponse();
                            if (response != null) {
                                boolean next_send = false;
                                String cur_OID = "";
                                String ip_port = event.getPeerAddress().toString();
                                if (ip_port != null) {
                                    ArrayList<String[]> res = new ArrayList();
                                    for (VariableBinding var : response.toArray()) {
                                        cur_OID = var.getOid().toString();
                                        if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                            String[] mas = new String[2];
                                            mas[0] = var.getOid().toString();
                                            mas[1] = var.getVariable().toString();
                                            if (oids_map.get(mas[0]) == null) {
                                                oids_map.put(mas[0], mas[0]);
                                                res.add(mas);
                                                next_send = true;
                                            } else {
                                                next_send = false;
                                                break;
                                            }
                                        } else {
                                            next_send = false;
                                            break;
                                        }
                                    }
                                    if(!res.isEmpty()) {
                                        if(result.get(node) != null)
                                           result.get(node).addAll(res);
                                        else
                                            result.put(node, res);
                                    }
                                    if (next_send) {
                                        while(true) {
                                            String[] node_comm_ver = new String[3];
                                            node_comm_ver[0] = node;
                                            node_comm_ver[1] = community;
                                            node_comm_ver[2] = version;                                        
                                            Map<String, ArrayList<String[]>> res1 = snmpWalkNotBulkNext(node_comm_ver, cur_OID, port, timeout, retries, start_oid);
                                            if(!res1.isEmpty()) {
                                                res.addAll(res1.get(node));
                                                cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                            }
                                            else {
                                                break;
                                            }
                                        } 
                                    }                                 
                                }
                            } else {
                                logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                return result;                            
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

                            pdu.setType(PDU.GETNEXT);

                            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                            if (event != null) {
                                PDU response = event.getResponse();
                                if (response != null) {
                                    boolean next_send = false;
                                    String cur_OID = "";
                                    String ip_port = event.getPeerAddress().toString();
                                    if (ip_port != null) {
                                        ArrayList<String[]> res = new ArrayList();
                                        for (VariableBinding var : response.toArray()) {
                                            cur_OID = var.getOid().toString();
                                            if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                                mas = new String[2];
                                                mas[0] = var.getOid().toString();
                                                mas[1] = var.getVariable().toString();
                                                if (oids_map.get(mas[0]) == null) {
                                                    oids_map.put(mas[0], mas[0]);
                                                    res.add(mas);
                                                    next_send = true;
                                                } else {
                                                    next_send = false;
                                                    break;
                                                }
                                            } else {
                                                next_send = false;
                                                break;
                                            }
                                        }
                                        if(!res.isEmpty()) {
                                            if(result.get(node) != null)
                                               result.get(node).addAll(res);
                                            else
                                                result.put(node, res);
                                        }
                                        if (next_send) {
                                            while(true) {
                                                String[] node_comm_ver = new String[3];
                                                node_comm_ver[0] = node;
                                                node_comm_ver[1] = community;
                                                node_comm_ver[2] = version;                                        
                                                Map<String, ArrayList<String[]>> res1 = snmpWalkNotBulkNext(node_comm_ver, cur_OID, port, timeout, retries, start_oid);
                                                if(!res1.isEmpty()) {
                                                    res.addAll(res1.get(node));
                                                    cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                                }
                                                else {
                                                    break;
                                                }
                                            } 
                                        }                                 
                                    }
                                } else {
                                    logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                                    return result;                            
                                }
                            }
                        }
                    }                    
                }
            }
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
        } catch (IOException ex) {
            return result;
        }
        return result;
    }
    
////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_node_community_version_oid, int timeout_thread) {
        return getProc(list_node_community_version_oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_node_community_version_oid, int timeout_thread, int port) {
        return getProc(list_node_community_version_oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_node_community_version_oid, int timeout_thread, int timeout, int retries) {
        return getProc(list_node_community_version_oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_node_community_version_oid, int timeout_thread, int port, int timeout, int retries) {
        return getProc(list_node_community_version_oid, timeout_thread, port, timeout, retries, Utils.BULKSIZE, false);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> list_node_community_version_oid, int timeout_thread, int port, int timeout, int retries, int bulk_size) {
        return getProc(list_node_community_version_oid, timeout_thread, port, timeout, retries, bulk_size, false);
    }

    private Map<String, ArrayList<String[]>> getProc(ArrayList<String[]> node_community_version_oid_list, int timeout_thread, int port, int timeout, int retries, int bulk_size, boolean test) {
        Map<String, ArrayList<String[]>> result = new HashMap();
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

        if (!node_community_version_oid_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (String[] node_community_version_oid : node_community_version_oid_list) {
                    callables.add(() -> snmpWalkOid(node_community_version_oid, port, timeout, retries, bulk_size, test));
                }

                try {
                    for (Future<Map<String, ArrayList<String[]>>> f : service.invokeAll(callables, timeout_thread* 10L, TimeUnit.MINUTES)) {
                        Map res1 = null;
                        try {
                            res1 = f.get(timeout_thread, TimeUnit.MINUTES);
                        } catch (java.util.concurrent.TimeoutException ex) {
                            f.cancel(true);
                            System.out.println("Future Exception CancellationException!!!");
                        }                          
                        if(res1 != null && !res1.isEmpty()) {
                            String node = (String)res1.keySet().toArray()[0];
                            if(result.get(node) != null)
                                result.get(node).addAll((ArrayList<String[]>)res1.get(node));
                            else
                                result.putAll(res1);
                        }
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

    private Map<String, ArrayList<String[]>> snmpWalkOid(String[] node_community_version_oid, int port, int timeout, int retries, int bulk_size, boolean test) {
        Map<String, ArrayList<String[]>> result = new HashMap();

        Map<String, String> oids_map = new HashMap();
        String oid = node_community_version_oid[3];
        String start_oid = node_community_version_oid[3];
        try {
            String node = node_community_version_oid[0];
            if(node_community_version_oid[2].equals("1") || node_community_version_oid[2].equals("2")) {
                String community = utils.decrypt(Utils.master_key, node_community_version_oid[1]);

                Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(community));
                target.setAddress(targetAddress);
                target.setRetries(retries-1);
                target.setTimeout(timeout * 1000L);
                target.setVersion(SnmpConstants.version1);
                if (node_community_version_oid[2].equals("2")) {
                    target.setVersion(SnmpConstants.version2c);
                }

                PDU pdu = new PDU();
    //            OID startOid = new OID(oid);
                pdu.add(new VariableBinding(new OID(oid)));
                if (node_community_version_oid[2].equals("2")) {
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);
                } else {
                    pdu.setType(PDU.GETNEXT);
                }

                ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                if (event != null) {
                    PDU response = event.getResponse();
                    if (response != null) {
                        boolean next_send = false;
                        String cur_OID = "";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) {
                            ArrayList<String[]> res = new ArrayList();
                            Map<String, String> res_map = new HashMap();
                            for (VariableBinding var : response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[2];
                                    mas[0] = var.getOid().toString();
                                    mas[1] = var.getVariable().toString();

                                    if (oids_map.get(mas[0]) == null) {
                                        oids_map.put(mas[0], mas[0]);
                                        res.add(mas);
                                        next_send = true;
                                    } else {
                                        next_send = false;
                                        break;
                                    }
                                } else {
                                    next_send = false;
                                    break;
                                }
                            }
                            if(!res.isEmpty()) {
                                Map<String, String> uniqal_oid = new HashMap();
                                ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                boolean fail = (boolean)res_list.get(1);
                                if (next_send && !test && !fail) {
                                    while(true) {
                                        Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_community_version_oid, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                        if(!res1.isEmpty()) {
                                            res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                            result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                            fail = (boolean)res_list.get(1); 
                                            if(fail) {
//                                                System.out.println("Cycling for node: "+node+" - "+oid);
                                                break;
                                            }
                                            cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                        }
                                        else
                                            break;
                                    }
                                } 
                            }
                        }
                    } else {
                        logger.println(node+": "+community+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                        return result;                    
                    }
                }
            } else if(node_community_version_oid[2].equals("3")) {
                String[] mas = node_community_version_oid[1].split("\\|");
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
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(bulk_size);                    

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    if (event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            boolean next_send = false;
                            String cur_OID = "";
                            String ip_port = event.getPeerAddress().toString();
                            if (ip_port != null) {
                                ArrayList<String[]> res = new ArrayList();
                                Map<String, String> res_map = new HashMap();
                                for (VariableBinding var : response.toArray()) {
                                    cur_OID = var.getOid().toString();
                                    if (cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                        mas = new String[2];
                                        mas[0] = var.getOid().toString();
                                        mas[1] = var.getVariable().toString();

                                        if (oids_map.get(mas[0]) == null) {
                                            oids_map.put(mas[0], mas[0]);
                                            res.add(mas);
                                            next_send = true;
                                        } else {
                                            next_send = false;
                                            break;
                                        }
                                    } else {
                                        next_send = false;
                                        break;
                                    }
                                }
                                if(!res.isEmpty()) {
                                    Map<String, String> uniqal_oid = new HashMap();
                                    ArrayList res_list = utils.addSNMPResult(node, res, result, uniqal_oid);
                                    result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                    boolean fail = (boolean)res_list.get(1);
                                    if (next_send && !test && !fail) {
                                        while(true) {
                                            Map<String, ArrayList<String[]>> res1 = snmpWalkNext(node_community_version_oid, cur_OID, port, timeout, retries, bulk_size, start_oid);
                                            if(!res1.isEmpty()) {
                                                res_list = utils.addSNMPResult(node, res1.get(node), result, uniqal_oid);
                                                result = (Map<String, ArrayList<String[]>>)res_list.get(0);
                                                fail = (boolean)res_list.get(1); 
                                                if(fail) {
//                                                    System.out.println("Cycling for node: "+node+" - "+oid);
                                                    break;
                                                }
                                                cur_OID = res1.get(node).get(res1.get(node).size()-1)[0];
                                            }
                                            else
                                                break;
                                        }
                                    } 
                                }
                            }
                        } else {
                            logger.println(node+": "+node_community_version_oid[1]+"|"+"|"+oid+" - timeout!!!", logger.DEBUG);
                            return result;                    
                        }
                    }

                }                
            }
            try { Thread.sleep(1000); } catch (InterruptedException _) {}
        } catch (IOException ex) {
            return result;
        }
        return result;
    }

}
