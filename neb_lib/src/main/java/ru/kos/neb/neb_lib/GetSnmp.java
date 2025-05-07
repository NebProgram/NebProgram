package ru.kos.neb.neb_lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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

public class GetSnmp {

    private Snmp snmp = new Snmp();
    public static Logger logger;
    private final Utils utils = new Utils();

    public GetSnmp() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if (Utils.DEBUG) {
            logger.setLevel(logger.DEBUG);
        } else {
            logger.setLevel(logger.INFO);
        }
    }
   
    public String[] get(String node, String community, String oid, int version, int port, int timeout) {
        int retries = 3;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public String[] get(String node, String community, String oid, int version, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public String[] get(String node, String community, String oid, int version) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return get(node, community, oid, version, port, timeout, retries);
    }

    public String[] get(String node, String community, String oid, int version, int port, int timeout, int retries) {
        return getValue(node, community, oid, version, port, timeout, retries);
    }

    @SuppressWarnings("ConvertToTryWithResources")
    private String[] getValue(String node, String community, String oid, int version, int port, int timeout, int retries) {
        String[] result = new String[2];
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
        try {
            if(version == 1 || version == 2) {
                community = utils.decrypt(Utils.master_key, community);
                if (community != null) {
                    result[0] = oid;
                    result[1] = null;
                    TransportMapping transport = new DefaultUdpTransportMapping();
                    snmp = new Snmp(transport);
                    transport.listen();
                    Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                    CommunityTarget target = new CommunityTarget();
                    target.setCommunity(new OctetString(community));
                    target.setAddress(targetAddress);
                    target.setRetries(retries-1);
                    target.setTimeout(timeout * 1000L);
                    target.setVersion(SnmpConstants.version1);
                    if (version == 2) {
                        target.setVersion(SnmpConstants.version2c);
                    }

                    PDU pdu = new PDU();
                    pdu.add(new VariableBinding(new OID(oid)));
                    pdu.setType(PDU.GET);

                    ResponseEvent event = snmp.send(pdu, target);
                    if (event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" -> {
                                    result[0] = oidstr;
                                    result[1] = "";
                                }
                                default -> {
                                    result[0] = oidstr;
                                    result[1] = val;
                                }
                            }
                        }
                    }

                    // disconnect
                    if (snmp != null) {
                        snmp.close();
                    }
                    transport.close();
                }
            } else if(version == 3) {
                String[] mas = community.split("\\|");
                String user = mas[0];
                String authProtocol_type = mas[1];
                String auth_paswd = utils.decrypt(Utils.master_key, mas[2]);
                String privProtocol_type = mas[3];
                String priv_paswd = utils.decrypt(Utils.master_key, mas[4]);

                if (user != null && 
                        (auth_paswd != null || priv_paswd != null) && 
                        (authProtocol_type != null || privProtocol_type != null)) {
                    result[0] = oid;
                    result[1] = null;
                    TransportMapping transport = new DefaultUdpTransportMapping();
                    snmp = new Snmp(transport);
                    transport.listen();

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
                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" -> {
                                    result[0] = oidstr;
                                    result[1] = "";
                                }
                                default -> {
                                    result[0] = oidstr;
                                    result[1] = val;
                                }
                            }
                        }
                    }

                    // disconnect
                    if (snmp != null) {
                        snmp.close();
                    }
                    transport.close();
                }                
            }
        } catch (IOException ex) {
            logger.println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(GetSnmp.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (snmp != null) {
                    snmp.close();
                }
            } catch (IOException _) {
            }
        }
        if(err_original != null) 
            System.setErr(err_original);        
        return result;
    }

    public String[] getLite(Snmp snmp, String node, String community, String oid, int version, int port, int timeout, int retries) {
        String[] result = new String[2];
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
        try {
            if(version == 1 || version == 2) {
                community = utils.decrypt(Utils.master_key, community);
                if (community != null) {
                    result[0] = oid;
                    result[1] = null;
                    Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
                    CommunityTarget target = new CommunityTarget();
                    target.setCommunity(new OctetString(community));
                    target.setAddress(targetAddress);
                    target.setRetries(retries-1);
                    target.setTimeout(timeout * 1000L);
                    target.setVersion(SnmpConstants.version1);
                    if (version == 2) {
                        target.setVersion(SnmpConstants.version2c);
                    }

                    PDU pdu = new PDU();
                    pdu.add(new VariableBinding(new OID(oid)));
                    pdu.setType(PDU.GET);

                    ResponseEvent event = snmp.send(pdu, target);
                    if (event != null) {
                        PDU response = event.getResponse();
                        if (response != null) {
                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" -> {
                                    result[0] = oidstr;
                                    result[1] = "";
                                }
                                default -> {
                                    result[0] = oidstr;
                                    result[1] = val;
                                }
                            }
                        }
                    }
                }
            } else if(version == 3) {
                String[] mas = community.split("\\|");
                String user = mas[0];
                String authProtocol_type = mas[1];
                String auth_paswd = utils.decrypt(Utils.master_key, mas[2]);
                String privProtocol_type = mas[3];
                String priv_paswd = utils.decrypt(Utils.master_key, mas[4]);

                if (user != null && 
                        (auth_paswd != null || priv_paswd != null) && 
                        (authProtocol_type != null || privProtocol_type != null)) {
                    result[0] = oid;
                    result[1] = null;

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
                            String oidstr = response.get(0).getOid().toString();
                            String val = response.get(0).getVariable().toString();
                            switch (val) {
                                case "Null", "noSuchObject", "noSuchInstance" -> {
                                    result[0] = oidstr;
                                    result[1] = "";
                                }
                                default -> {
                                    result[0] = oidstr;
                                    result[1] = val;
                                }
                            }
                        }
                    }

                }                
            }
        } catch (IOException ex) {
            logger.println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(GetSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(err_original != null) 
            System.setErr(err_original);        
        return result;
    } 

}
