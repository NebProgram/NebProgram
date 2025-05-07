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
//import java.util.logging.Level;
//import java.util.logging.Logger;
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

public class GetPool {

    public static Snmp snmp = new Snmp();
    private final int port = 161;
    private final int timeout = 3;
    private final int retries = 2;
    public static Logger logger;
    private final Utils utils = new Utils();

    public GetPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> node_community_version_list, ArrayList<String> oid_list, int timeout_thread) {
        return getProc(node_community_version_list, oid_list, timeout_thread, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> node_community_version_list, ArrayList<String> oid_list, int timeout_thread, int port) {
        return getProc(node_community_version_list, oid_list, timeout_thread, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> node_community_version_list, ArrayList<String> oid_list, int timeout_thread, int timeout, int retries) {
        return getProc(node_community_version_list, oid_list, timeout_thread, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> get(ArrayList<String[]> node_community_version_list, ArrayList<String> oid_list, int timeout_thread, int port, int timeout, int retries) {
        return getProc(node_community_version_list, oid_list, timeout_thread, port, timeout, retries);
    }

    private Map<String, ArrayList<String[]>> getProc(ArrayList<String[]> node_community_version_list, ArrayList<String> oid_list, int timeout_thread, int port, int timeout, int retries) {
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

        if (!node_community_version_list.isEmpty()) {
            try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
//            try (ExecutorService service = Executors.newFixedThreadPool(Utils.MAXPOOLTHREADS)) {
                CopyOnWriteArrayList<Callable<Map<String, ArrayList<String[]>>>> callables = new CopyOnWriteArrayList<>();
                for (String[] node_community_ver : node_community_version_list) {
                    callables.add(() -> getOids(node_community_ver, oid_list, port, timeout, retries));
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

    private Map<String, ArrayList<String[]>> getOids(String[] node_community_ver, ArrayList<String> oid_list, int port, int timeout, int retries) {
        Map<String, ArrayList<String[]>> result_out = new HashMap();
        ArrayList<String[]> result = new ArrayList();
        String node = node_community_ver[0];
        String community = node_community_ver[1];
        String version = node_community_ver[2];
        for (String oid : oid_list) {
            oid = oid.replaceAll("^\\.", "");
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
                    pdu.setType(PDU.GET);

                    ResponseEvent event;
                    try {
                        event = GetPool.snmp.send(pdu, target);
                    } catch (IOException ex) {
                        return result_out;
                    }

                    PDU response = event.getResponse();
                    if (response != null) {
                        if (event.getPeerAddress().toString() != null) {
                            String[] mas = new String[2];
                            mas[0] = event.getPeerAddress().toString().split("/")[0];
                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" -> {
                                    mas[0] = oidstr;
                                    mas[1] = "";
                                }
                                default -> {
                                    mas[0] = oidstr;
                                    mas[1] = val;
                                }
                            }
                            result.add(mas);
    //                        Utils.current_value = Utils.current_value + 1;
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

                        ResponseEvent event;
                        try {
                            event = GetPool.snmp.send(pdu, target);
                        } catch (IOException ex) {
                            return result_out;
                        }

                        PDU response = event.getResponse();
                        if (response != null) {
                            if (event.getPeerAddress().toString() != null) {
                                mas = new String[2];
                                mas[0] = event.getPeerAddress().toString().split("/")[0];
                                String oidstr = response.get(0).getOid().toString();
                                String val = response.get(0).getVariable().toString();
                                switch (val) {
                                    case "Null", "noSuchObject", "noSuchInstance" -> {
                                        mas[0] = oidstr;
                                        mas[1] = "";
                                    }
                                    default -> {
                                        mas[0] = oidstr;
                                        mas[1] = val;
                                    }
                                }
                                result.add(mas);
        //                        Utils.current_value = Utils.current_value + 1;
                            }
                        }
                    }
                }                
            }
        }

        result_out.put(node, result);
        return result_out;
    }

}
