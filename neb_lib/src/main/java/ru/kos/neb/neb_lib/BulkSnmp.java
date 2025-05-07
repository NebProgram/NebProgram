package ru.kos.neb.neb_lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

public class BulkSnmp {

    private final static int BULK_SIZE = 10;
    private Snmp snmp = new Snmp();
    private String start_oid = null;
    public static Logger logger;
    private final Utils utils = new Utils();
    private final Map<String, String> uniqal_oid = new HashMap();
    
    public BulkSnmp() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.setLevel(logger.DEBUG);
        else
            logger.setLevel(logger.INFO);
    }    

    public ArrayList<String[]> get(String node, String community, String oid, String version, int port, int timeout) {
        int retries = 2;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> get(String node, String community, String oid, String version, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> get(String node, String community, String oid, String version) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> get(String node, String community, String oid, String version, int port, int timeout, int retries) {
        return getList(node, community, oid, version, port, timeout, retries);
    }
    
    private ArrayList<String[]> getList(String node, String community, String oid, String version, int port, int timeout, int retries) {
        ArrayList<String[]> result = new ArrayList();
        start_oid=oid;
        try {
//            ExecutorService service = Executors.newCachedThreadPool();
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();          

            result = getOids(node, community, oid, version, port, timeout, retries);
        }    
        catch (IOException ex) {
            logger.println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(SnmpScan.class.getName()).log(Level.SEVERE, null, ex);
        }  
        finally {
            if (snmp != null) {
                try { snmp.close(); } catch (IOException _) { }
//                snmp = null;
            }
        } // finalize        

        return result;
    }

    private ArrayList<String[]> getOids(String node, String community, String oid, String version, int port, int timeout, int retries) {
        ArrayList<String[]> result = new ArrayList();
        try {
            if(version.equals("1") || version.equals("2")) {
                community = utils.decrypt(Utils.master_key, community);
                if(community != null) {            
                    Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                    CommunityTarget target = new CommunityTarget();
                    target.setCommunity(new OctetString(community));
                    target.setAddress(targetAddress);
                    target.setRetries(retries-1);
                    target.setTimeout(timeout * 1000L);
                    if(version.equals("2")) target.setVersion(SnmpConstants.version2c);
                    else target.setVersion(SnmpConstants.version1);

                    PDU pdu = new PDU();
                    pdu.add(new VariableBinding(new OID(oid)));
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(BULK_SIZE);

                    ResponseEvent event = snmp.send(pdu, target);
                    if(event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String cur_OID="";
                            String ip_port = event.getPeerAddress().toString();
                            if (ip_port != null) { 
                                for( VariableBinding var: response.toArray()) {
                                    cur_OID = var.getOid().toString();
                                    if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                        String[] mas = new String[2];
                                        mas[0]=var.getOid().toString();
                                        mas[1]=var.getVariable().toString();
                                        if(uniqal_oid.get(mas[0]) == null) {
                                            uniqal_oid.put(mas[0], mas[0]);
                                            result.add(mas);
                                        } else {
                                            return result;                           
                                        }
                                    }
                                    else {
                                        return result;
                                    }
                                }
                                ArrayList<String[]> res = getOids(node, community, cur_OID, version, port, timeout, retries);
                                result.addAll(res);

                            }
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

                        SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());
                        SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());

                        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivDES());
                        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
                        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES128());
                        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES192());
                        SecurityProtocols.getInstance().addPrivacyProtocol(new PrivAES256());

                        OctetString localEngineId = new OctetString(MPv3.createLocalEngineID());

                        USM usm = new USM(SecurityProtocols.getInstance(), localEngineId, 0);
                        SecurityModels.getInstance().addSecurityModel(usm);

                        OctetString securityName = new OctetString(user);
                        OID authProtocol = AuthSHA.ID;
                        if(authProtocol_type != null && authProtocol_type.equalsIgnoreCase("SHA"))
                            authProtocol = AuthSHA.ID;
                        else if(authProtocol_type != null && authProtocol_type.equalsIgnoreCase("MD5"))
                            authProtocol = AuthMD5.ID;

                        OID privProtocol = PrivDES.ID;
                        if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("DES"))
                            privProtocol = PrivDES.ID;
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("3DES"))
                            privProtocol = Priv3DES.ID;
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES128"))
                            privProtocol = PrivAES128.ID;
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES192"))
                            privProtocol = PrivAES192.ID;
                        else if(privProtocol_type != null && privProtocol_type.equalsIgnoreCase("AES256"))
                            privProtocol = PrivAES256.ID;

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
                        pdu.setMaxRepetitions(BULK_SIZE);                

                        ResponseEvent event = snmp.send(pdu, target);
                        if(event != null) {
                            PDU response = event.getResponse();
                            if (response != null) {
                                String cur_OID="";
                                String ip_port = event.getPeerAddress().toString();
                                if (ip_port != null) { 
                                    for( VariableBinding var: response.toArray()) {
                                        cur_OID = var.getOid().toString();
                                        if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                            mas = new String[2];
                                            mas[0]=var.getOid().toString();
                                            mas[1]=var.getVariable().toString();
                                            if(uniqal_oid.get(mas[0]) == null) {
                                                uniqal_oid.put(mas[0], mas[0]);
                                                result.add(mas);
                                            } else {
                                                return result;                           
                                            }
                                        }
                                        else {
                                            return result;
                                        }
                                    }
                                    ArrayList<String[]> res = getOids(node, community, cur_OID, version, port, timeout, retries);
                                    result.addAll(res);

                                }
                            }
                        }
                    }
                }                
            }
        } catch (IOException _) {
        }
        return result;
    }

}
