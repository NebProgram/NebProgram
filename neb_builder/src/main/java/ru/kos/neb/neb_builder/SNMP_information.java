package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static ru.kos.neb.neb_builder.Neb.logger;
import ru.kos.neb.neb_lib.GetPool;
import ru.kos.neb.neb_lib.WalkPool;

public class SNMP_information {

    private final Map<String, String> oids = new HashMap<>();
    private int port = 161;
    private boolean debug = Neb.DEBUG;

    private static final Utils utils = new Utils();

    public SNMP_information() {

    }

    public SNMP_information(int timeout, int retries, int port, boolean debug) {
        this.port = port;
        this.debug = debug;
    }

    public Map getInformationFromNodes(ArrayList<String[]> node_community_version) {
        Map result = new HashMap<>();

        if (debug) {
            logger.Println("Start GetCommonInformationFromNodes ...", logger.DEBUG);
        }
        Map<String, Map<String, String>> commonInformationFromNodes = getCommonInformationFromNodes(node_community_version);
        if (!commonInformationFromNodes.isEmpty()) {
            result.put("general", commonInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetCommonInformationFromNodes.", logger.DEBUG);
            }
            if (debug) {
                logger.Println("Start GetWalkInformationFromNodes ...", logger.DEBUG);
            }
            Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes = getWalkInformationFromNodes(node_community_version);
//            Map<String, ArrayList> ifaceMaping = GetIfaceMaping(node_community_version, walkInformationFromNodes);
//            walkInformationFromNodes.put("IfaceMaping", ifaceMaping);
            if (debug) {
                logger.Println("Stop GetWalkInformationFromNodes.", logger.DEBUG);
            }
            Map<String, Map> serialnumber_map = getSerialNumberFromNodes(walkInformationFromNodes);
            if (!serialnumber_map.isEmpty()) {
                Map<String, Map> general_map = (Map) result.get("general");
                for (Map.Entry<String, Map> entry : general_map.entrySet()) {
                    String node = entry.getKey();
                    if (serialnumber_map.get(node) != null) {
                        general_map.get(node).put("serialnumber", serialnumber_map.get(node));
                    }

                }
            }
            if (debug) {
                logger.Println("Start GetDuplexMode ...", logger.DEBUG);
            }
            Map<String, Map<String, String>> duplex_mode = getDuplexMode(walkInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetDuplexMode.", logger.DEBUG);
            }
            if (debug) {
                logger.Println("Start GetIpAddressFromInterface ...", logger.DEBUG);
            }
            Map<String, Map<String, ArrayList<String>>> interface_ipaddress = getIpAddressFromInterface(walkInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetIpAddressFromInterface.", logger.DEBUG);
            }
            if (debug) {
                logger.Println("Start GetVlanInform ...", logger.DEBUG);
            }
            Map<String, Map<String, String>> getVlanInform = getVlanInform(walkInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetVlanInform.", logger.DEBUG);
            }
            if (debug) {
                logger.Println("Start GetVlanPortUntagTag ...", logger.DEBUG);
            }
            Map<String, Map<String, String[]>> getVlanPortUntagTag = getVlanPortUntagTag(walkInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetVlanPortUntagTag.", logger.DEBUG);
            }

            if (debug) {
                logger.Println("Start GetInterfacesInformationFromNodes ...", logger.DEBUG);
            }
            Map<String, Map<String, Map<String, String>>> getInterfacesInformationFromNodes = getInterfacesInformationFromNodes(
                    walkInformationFromNodes, duplex_mode, interface_ipaddress, getVlanPortUntagTag);
            result.put("interfaces", getInterfacesInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetInterfacesInformationFromNodes.", logger.DEBUG);
            }
            
//            Map<String, String[]> mac_node_iface = new HashMap();
//            for (Map.Entry<String, Map<String, Map<String, String>>> entry : getInterfacesInformationFromNodes.entrySet()) {
//                String node = entry.getKey();
//                Map<String, Map<String, String>> ifaces_information = entry.getValue();
//                Map<String, String[]> mac_node_iface_tmp = utils.getMacNode_Iface(node, ifaces_information);
//                mac_node_iface.putAll(mac_node_iface_tmp);
//            }
            
//            if (debug) {
//                logger.Println("Start GetRoutesInformationFromNodes ...", logger.DEBUG);
//            }
//            Map<String, ArrayList<String[]>> getRoutesInformationFromNodes = getRoutesInformationFromNodes(walkInformationFromNodes);
//
//            result.put("routes", getRoutesInformationFromNodes);
//            if (debug) {
//                logger.Println("Stop GetRoutesInformationFromNodes.", logger.DEBUG);
//            }
            if (debug) {
                logger.Println("Start GetVlansInformationFromNodes ...", logger.DEBUG);
            }
            Map<String, Map<String, String>> getVlansInformationFromNodes = getVlansInformationFromNodes(getVlanInform);
            result.put("vlans", getVlansInformationFromNodes);
            if (debug) {
                logger.Println("Stop GetVlansInformationFromNodes.", logger.DEBUG);
            }

            if (debug) {
                logger.Println("Start get discoverer protocol...", logger.DEBUG);
            }
            Map<String, Map<String, ArrayList<Map<String, String>>>> dplinks = getDP(walkInformationFromNodes);
            if (!dplinks.isEmpty()) {
                if (result.get("advanced") != null) {
                    ((Map) result.get("advanced")).put("links", dplinks);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("links", dplinks);
                    result.put("advanced", tmp);
                }
            }
            if (debug) {
                logger.Println("Stop get discoverer protocol.", logger.DEBUG);
            }

            if (debug) {
                logger.Println("Start GetARP...", logger.DEBUG);
            }
            Map<String, Map<String, String>> arp_table = getARP(node_community_version);
            if (!arp_table.isEmpty()) {
                if (result.get("advanced") != null) {
                    ((Map) result.get("advanced")).put("arp", arp_table);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("arp", arp_table);
                    result.put("advanced", tmp);
                }
            }
            if (debug) {
                logger.Println("Stop GetARP.", logger.DEBUG);
            }

        }

        // transform result
        Map result_new = new HashMap();
        Map<String, Map> general = (Map<String, Map>) result.get("general");
        Map<String, Map<String, String>> interfaces = (Map<String, Map<String, String>>) result.get("interfaces");
//        Map<String, ArrayList<String[]>> routes = (Map<String, ArrayList<String[]>>) result.get("routes");
        Map<String, Map<String, String>> vlans = (Map<String, Map<String, String>>) result.get("vlans");
        Map<String, Map<String, Map<String, String>>> advanced = (Map<String, Map<String, Map<String, String>>>) result.get("advanced");
        if(general != null) {
            for (Map.Entry<String, Map> entry : general.entrySet()) {
                String node = entry.getKey();
                Map map_tmp = new HashMap();
                if (general.get(node) != null) {
                    map_tmp.put("general", general.get(node));
                }
                if (interfaces != null && interfaces.get(node) != null) {
                    map_tmp.put("interfaces", interfaces.get(node));
                }
    //            if (routes!= null && routes.get(node) != null) {
    //                map_tmp.put("routes", routes.get(node));
    //            }
                if (vlans != null && vlans.get(node) != null) {
                    map_tmp.put("vlans", vlans.get(node));
                }
                if (advanced != null && advanced.get("links") != null && advanced.get("arp") != null 
                        && advanced.get("links").get(node) != null) {
                    Map<String, Map> map_tmp1 = new HashMap();
                    map_tmp1.put("links", advanced.get("links").get(node));
                    map_tmp1.put("arp", advanced.get("arp").get(node));
                    map_tmp.put("advanced", map_tmp1);
                }
                result_new.put(node, map_tmp);
            }
        }

        return result_new;
    }

    // output: node ---> ArrayList(String,String)
    private Map<String, Map<String, String>> getCommonInformationFromNodes(ArrayList<String[]> node_community_version) {
        String sysDescription = "1.3.6.1.2.1.1.1.0";
        String sysUptime = "1.3.6.1.2.1.1.3.0";
        String sysContact = "1.3.6.1.2.1.1.4.0";
        String sysName = "1.3.6.1.2.1.1.5.0";
        String sysLocation = "1.3.6.1.2.1.1.6.0";
        String defaultTTL = "1.3.6.1.2.1.4.2.0";
        String dot1dBaseBridgeAddress = "1.3.6.1.2.1.17.1.1.0";
        ArrayList oid_list = new ArrayList();
        oid_list.add(sysDescription);
        oid_list.add(sysUptime);
        oid_list.add(sysContact);
        oid_list.add(sysName);
        oid_list.add(sysLocation);
        oid_list.add(defaultTTL);
        oid_list.add(dot1dBaseBridgeAddress);

        Map<String, Map<String, String>> result = new HashMap<>();

        GetPool getPool = new GetPool();
        Map<String, ArrayList> res = getPool.get(node_community_version, oid_list, Neb.timeout_thread, port, Neb.timeout, Neb.retries);
        for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> map_tmp = new HashMap<>();
            for (String[] val : val_list) {
                if (val[0].equals(sysDescription)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("sysDescription", utils.hexStringToUTF8(val[1]));
                    }
                } else if (val[0].equals(sysUptime)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("uptime", val[1]);
                    }
                } else if (val[0].equals(sysContact)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("contact", val[1]);
                    }
                } else if (val[0].equals(sysName)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("sysname", val[1]);
                    }
                } else if (val[0].equals(sysLocation)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("syslocation", utils.hexStringToUTF8(val[1]));
                    }
                } else if (val[0].equals(defaultTTL)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("ttl", val[1]);
                    }
                } else if (val[0].equals(dot1dBaseBridgeAddress)) {
                    val[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(val[1]));
                    if (!val[1].isEmpty()) {
                        map_tmp.put("base_address", val[1]);
                    }
                }
            }
            if (!map_tmp.isEmpty()) {
                result.put(node, map_tmp);
            }
        }

        return result;
    }

    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList<String[]>>> getWalkInformationFromNodes(ArrayList<String[]> node_community_version) {
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        oids.put("ifMTU", "1.3.6.1.2.1.2.2.1.4");
        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");
        oids.put("ifMAC", "1.3.6.1.2.1.2.2.1.6");
        oids.put("ifAdminStatus", "1.3.6.1.2.1.2.2.1.7");
        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
        oids.put("ifIpAddress", "1.3.6.1.2.1.4.20.1.2");
        oids.put("ifIpNetMask", "1.3.6.1.2.1.4.20.1.3");
        oids.put("NetRoute", "1.3.6.1.2.1.4.24.4.1.1");
        oids.put("RouteMetric", "1.3.6.1.2.1.4.24.4.1.11");
        oids.put("RouteDestination", "1.3.6.1.2.1.4.24.4.1.4");
        oids.put("RouteType", "1.3.6.1.2.1.4.24.4.1.6");
        oids.put("RouteProto", "1.3.6.1.2.1.4.24.4.1.7");
        oids.put("RouteAge", "1.3.6.1.2.1.4.24.4.1.8");
        oids.put("RouteMask", "1.3.6.1.2.1.4.24.4.1.2");
        oids.put("IdNameVlan", "1.3.6.1.2.1.17.7.1.4.3.1.1");
        oids.put("IdVlanToNumberInterface", "1.3.6.1.2.1.16.22.1.1.1.1.4.1.3.6.1.2.1.16.22.1.4.1");
        oids.put("TaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.4");
        oids.put("UnTaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.5");
        oids.put("IdNameVlanCisco", "1.3.6.1.4.1.9.9.46.1.3.1.1.4.1");
        oids.put("IdVlanToNumberInterfaceCisco", "1.3.6.1.4.1.9.9.128.1.1.1.1.3");
        oids.put("VlanType", "1.3.6.1.4.1.9.9.46.1.6.1.1.3");
        oids.put("VlanPortAccessModeCisco", "1.3.6.1.4.1.9.9.68.1.2.1.1.2");

        oids.put("PortTrunkNativeVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.5");
        oids.put("PortTrunkVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.11");
        oids.put("vlanTrunkPortDynamicStatus", "1.3.6.1.4.1.9.9.46.1.6.1.1.14");
        oids.put("VlanNameHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.2");
        oids.put("VlanIdHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.5");
        oids.put("VlanPortStateHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.8.1.1");
        oids.put("VlanNameTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("Duplex_Allied", "1.3.6.1.4.1.207.8.10.3.1.1.5");
        oids.put("Duplex_Asante", "1.3.6.1.4.1.298.1.5.1.2.6.1.7");
        oids.put("Duplex_Dell", "1.3.6.1.4.1.89.43.1.1.4");
        oids.put("Duplex_Foundry", "1.3.6.1.4.1.1991.1.1.3.3.1.1.4");
        oids.put("Duplex_Cisco2900", "1.3.6.1.4.1.9.9.87.1.4.1.1.32");
        oids.put("Duplex_HP", "1.3.6.1.2.1.26.2.1.1.3");
        oids.put("Duplex_Cisco", "1.3.6.1.2.1.10.7.2.1.19");
        oids.put("IpAddress", "1.3.6.1.2.1.4.20.1.1");
        oids.put("lldpRemManAddrIfId", "1.0.8802.1.1.2.1.4.2.1.4");
        oids.put("lldpRemPortId", "1.0.8802.1.1.2.1.4.1.1.7");
        oids.put("lldpRemChassisId", "1.0.8802.1.1.2.1.4.1.1.5");
        oids.put("lldpLocChassisId", "1.0.8802.1.1.2.1.3.2");
        oids.put("lldpRemManAddrIfSubtype", "1.0.8802.1.1.2.1.4.2.1.3");
        oids.put("lldpRemSysName", "1.0.8802.1.1.2.1.4.1.1.9");
        oids.put("lldpRemSysDesc", "1.0.8802.1.1.2.1.4.1.1.10");
        oids.put("ldpLocPortId", "1.0.8802.1.1.2.1.3.7.1.3");
        oids.put("cdpCacheAddress", "1.3.6.1.4.1.9.9.23.1.2.1.1.4");
        oids.put("cdpCacheDevicePort", "1.3.6.1.4.1.9.9.23.1.2.1.1.7");
        oids.put("cdpRemSysName", "1.3.6.1.4.1.9.9.23.1.2.1.1.6");
        oids.put("cdpRemSysDesc", "1.3.6.1.4.1.9.9.23.1.2.1.1.8");
        oids.put("IfaceMaping", "1.3.6.1.2.1.17.1.4.1.2");
        oids.put("entPhysicalDescr", "1.3.6.1.2.1.47.1.1.1.1.2");
        oids.put("entPhysicalSerialNumber", "1.3.6.1.2.1.47.1.1.1.1.11");
        oids.put("cisco-vlan-membership", "1.3.6.1.4.1.9.9.68.1.2.2.1.2");

        Map<String, Map<String, ArrayList<String[]>>> result = new HashMap<>();

        WalkPool walkPool = new WalkPool();
        ArrayList<String[]> node_community_version_oid = new ArrayList();
        for (Map.Entry<String, String> entry : oids.entrySet()) {
//            String key = entry.getKey();
            String val = entry.getValue();
            for(String[] item : node_community_version) {
                String[] mas = new String[4];
                mas[0] = item[0];
//                mas[1] = Neb.neb_lib_utils.decrypt(ru.kos.neb.neb_lib.Utils.master_key, item[1]);
                mas[1] = item[1];
                mas[2] = item[2];
                mas[3] = val;
                node_community_version_oid.add(mas);
            }
        }
                  
        Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version_oid, Neb.timeout_thread, port, Neb.timeout, Neb.retries);
        ArrayList oid_node_keyval = new ArrayList();
        for (Map.Entry<String, ArrayList<String[]>> entry : res.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            if(val != null) {
                for(String[] item : val) {
                    String key = null;
                    if(item != null) {
                        for(Map.Entry<String, String> entry1 : oids.entrySet()) {
                            String oidname = entry1.getKey();
                            String oid = entry1.getValue();
                            if(item[0].startsWith(oid)) {
                                key = oidname;
                                break;
                            }
                        }
                        if(key != null) {
                            ArrayList list = new ArrayList();
                            list.add(key);
                            list.add(node);
                            String[] mas1 = new String[2];
                            mas1[0] = item[0];
                            mas1[1] = item[1];
                            list.add(mas1);
                            oid_node_keyval.add(list);
                        }
                    }
                }
            }
        }
        for(ArrayList item : (ArrayList<ArrayList>)oid_node_keyval) {
            String oid = (String)item.get(0);
            String node = (String)item.get(1);
            String[] mas = (String[])item.get(2);
//            String k = mas[0];
//            String v = mas[1];
            if(result.get(oid) == null) {
                ArrayList<String[]> list_tmp = new ArrayList();
                list_tmp.add(mas);
                Map<String, ArrayList<String[]>> map_tmp = new HashMap();
                map_tmp.put(node, list_tmp);
                result.put(oid, map_tmp);
            } else {
                if(result.get(oid).get(node) == null) {
                    ArrayList<String[]> list_tmp = new ArrayList();
                    list_tmp.add(mas);
                    result.get(oid).put(node, list_tmp);
                } else {
                    result.get(oid).get(node).add(mas);
                }
            }
        }

        return result;
    }

    // Output format: ArrayList(String)
    private ArrayList getVlanCommunity(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
        ArrayList result = new ArrayList();

        if(walkInformationFromNodes.get("VlanCommunity") != null) {
            ArrayList<String[]> res = walkInformationFromNodes.get("VlanCommunity").get(node);
            if (res != null) {
                res = utils.setUniqueList(res, 1);
                for (String[] item : res) {
                    String[] mas = item[1].split(":");
                    StringBuilder community = new StringBuilder();
                    if (mas.length > 1) {
                        for (String item1 : mas) {
                            String ch = utils.convertHexToString(item1);
                            community.append(ch);
                        }
                    } else {
                        community = new StringBuilder(item[1]);
                    }
                    if (!community.toString().isEmpty() && !community.toString().startsWith("@")) {
                        result.add(community.toString());
                    }
                }
            }
        }
        return result;
    }

    //  output: node ---> id_iface ---> duplex_mode
    private Map<String, Map<String, String>> getDuplexMode(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        if (walkInformationFromNodes.get("Duplex_Allied") != null && !walkInformationFromNodes.get("Duplex_Allied").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Allied").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "1":
                            val[1] = "full-duplex";
                            break;
                        case "2":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Asante") != null && !walkInformationFromNodes.get("Duplex_Asante").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Asante").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "3":
                            val[1] = "full-duplex";
                            break;
                        case "2":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Dell") != null && !walkInformationFromNodes.get("Duplex_Dell").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Dell").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "2":
                            val[1] = "full-duplex";
                            break;
                        case "1":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Foundry") != null && !walkInformationFromNodes.get("Duplex_Foundry").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Foundry").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "3":
                            val[1] = "full-duplex";
                            break;
                        case "2":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Cisco2900") != null && !walkInformationFromNodes.get("Duplex_Cisco2900").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Cisco2900").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "1":
                            val[1] = "full-duplex";
                            break;
                        case "2":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_HP") != null && !walkInformationFromNodes.get("Duplex_HP").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_HP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 2];
                    String var = val[1].split("\\.")[val[1].split("\\.").length - 1];
                    switch (var) {
                        case "11":
                        case "13":
                        case "16":
                        case "18":
                        case "20":
                            val[1] = "full-duplex";
                            break;
                        case "10":
                        case "12":
                        case "15":
                        case "17":
                        case "19":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Cisco") != null && !walkInformationFromNodes.get("Duplex_Cisco").isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("Duplex_Cisco").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    switch (val[1]) {
                        case "3":
                            val[1] = "full-duplex";
                            break;
                        case "2":
                            val[1] = "half-duplex";
                            break;
                        default:
                            val[1] = "unknown";
                            break;
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        return result;
    }

    // Output format node ---> id_iface ---> ip, mask    
    private Map<String, Map<String, ArrayList<String>>> getIpAddressFromInterface(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, ArrayList<String>>> result = new HashMap<>();
        Map<String, ArrayList<String[]>> id_ip = new HashMap<>();
        Map<String, ArrayList<String[]>> ip_mask = new HashMap<>();

        // output: node ---> ArrayList(id, ip)
        if(walkInformationFromNodes.get("ifIndex") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("ifIndex").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList<>();
                if(walkInformationFromNodes.get("ifIpAddress") != null) {
                    ArrayList<String[]> val_list_sec = walkInformationFromNodes.get("ifIpAddress").get(node);
                    for (String[] item1 : val_list) {
                        if (val_list_sec != null) {
//                            boolean find = false;
                            for (String[] item2 : val_list_sec) {
                                if (item1[1].equals(item2[1])) {
                                    String[] tmp = item2[0].split("\\.");
                                    String[] mas = new String[2];
                                    mas[0] = item1[1];
                                    mas[1] = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                                    tmp_list.add(mas);
//                                    find = true;
        //                            break;
                                }
                            }
                        }
                    }
                }

                id_ip.put(node, tmp_list);
            }
        }

        // output: node ---> ArrayList(ip, mask)
        if(walkInformationFromNodes.get("ifIpNetMask") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("ifIpNetMask").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList<>();
                for (String[] item1 : val_list) {
                    String[] mas = new String[2];
                    String[] tmp = item1[0].split("\\.");
                    mas[0] = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                    mas[1] = item1[1];
                    tmp_list.add(mas);
                }
                ip_mask.put(node, tmp_list);

            }
        }

        // output: node --> id ---> ip, mask
        for (Map.Entry<String, ArrayList<String[]>> entry : id_ip.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> val_list_sec = ip_mask.get(node);
            ArrayList<String[]> tmp_list = new ArrayList();
            for (String[] item1 : val_list) {
                if (val_list_sec != null) {
                    for (String[] item2 : val_list_sec) {
                        if (item1[1].equals(item2[0])) {
                            String[] mas = new String[3];
                            mas[0] = item1[0];
                            mas[1] = item1[1];
                            mas[2] = item2[1];
                            tmp_list.add(mas);
//                            break;
                        }
                    }
                }
            }

            Map<String, ArrayList<String>> tmp_map1 = new HashMap<>();
            for (int i = 0; i < tmp_list.size(); i++) {
                ArrayList<String> tmp_tmp_list = new ArrayList();
                String[] mas1 = tmp_list.get(i);
                tmp_tmp_list.add(mas1[1] + " " + mas1[2]);
                for (int j = i + 1; j < tmp_list.size(); j++) {
                    String[] mas2 = tmp_list.get(j);
                    if (mas1[0].equals(mas2[0])) {
//                        String[] mas_tmp1 = new String[2];
//                        mas_tmp1[0]=mas2[1]; mas_tmp1[1]=mas2[2];
                        tmp_tmp_list.add(mas2[1] + " " + mas2[2]);
                        tmp_list.remove(j);
                        j = j - 1;
                    }
                }
                tmp_map1.put(mas1[0], tmp_tmp_list);
            }

            result.put(node, tmp_map1);
        }

        return result;
    }

    private Map<String, Map<String, String>> getVlanInform(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        Map<String, Map<String, String>> getVlanInformCisco = getVlanInformCisco(walkInformationFromNodes);
        Map<String, Map<String, String>> getVlanInformHP = getVlanInformHP(walkInformationFromNodes);
        Map<String, Map<String, String>> getVlanInformRFC2674 = getVlanInformRFC2674(walkInformationFromNodes);

        result.putAll(getVlanInformCisco);
        result.putAll(getVlanInformHP);
        result.putAll(getVlanInformRFC2674);
        return result;
    }

    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> getVlanInformCisco(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // get node ---> id ---> namevlan
        if(walkInformationFromNodes.get("IdNameVlanCisco") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("IdNameVlanCisco").entrySet()) {
                Map<String, String> id_namevlan = new HashMap<>();
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
//                ArrayList<String[]> tmp_list = new ArrayList<>();
                for (String[] item1 : val_list) {
                    String[] tmp = item1[0].split("\\.");
                    String id = tmp[tmp.length - 1];
                    String namevlan = item1[1];
                    id_namevlan.put(id, namevlan);
                }
                result.put(node, id_namevlan);
            }
        }

        return result;
    }

    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> getVlanInformHP(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // output: node ---> id_vlan ---> name_vlan)
        if(walkInformationFromNodes.get("VlanIdHP") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("VlanIdHP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> val_list_sec = walkInformationFromNodes.get("VlanNameHP").get(node);
                Map<String, String> id_namevlan = new HashMap<>();
                for (String[] item1 : val_list) {
                    String num1 = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                    if (val_list_sec != null) {
                        for (String[] item2 : val_list_sec) {
                            String num2 = item2[0].split("\\.")[item2[0].split("\\.").length - 1];
                            if (num1.equals(num2)) {
                                id_namevlan.put(item1[1], item2[1]);
                                break;
                            }
                        }
                    }
                }
                result.put(node, id_namevlan);
            }
        }

        return result;
    }

    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> getVlanInformRFC2674(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // output: node ---> ArrayList(id_vlan, name_vlan)
        if(walkInformationFromNodes.get("IdNameVlan") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("IdNameVlan").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> id_namevlan = new HashMap<>();
                for (String[] item1 : val_list) {
                    String id_vlan = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                    id_namevlan.put(id_vlan, item1[1]);
                }
                result.put(node, id_namevlan);
            }
        }

        return result;
    }

    // Output format node ---> id_iface ---> untag, tag(vlan1:vlan2...)
    private Map<String, Map<String, String[]>> getVlanPortUntagTag(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();

        Map<String, Map<String, String[]>> getVlanCisco = getVlanCisco(walkInformationFromNodes);
        Map getVlanHP = new HashMap();
        Map getVlanRFC2674 = new HashMap();
        if (getVlanCisco.isEmpty()) {
            getVlanHP = getVlanHP(walkInformationFromNodes);
        }
        if (getVlanHP.isEmpty()) {
            getVlanRFC2674 = getVlanRFC2674(walkInformationFromNodes);
        }

        result.putAll(getVlanCisco);
        result.putAll(getVlanHP);
        result.putAll(getVlanRFC2674);
        return result;
    }

    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> getVlanCisco(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
//        Map<String, Map<String, String>> node_id_untag = new HashMap<>();
//        Map<String, Map<String, String>> node_id_tag = new HashMap<>();

        // output: node ---> id_iface ---> trunk_access
        Map<String, Map<String, String>> node_id_trunk_access_mode = new HashMap<>();
        if(walkInformationFromNodes.get("vlanTrunkPortDynamicStatus") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("vlanTrunkPortDynamicStatus").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> trunk_access = new HashMap<>();
                for (String[] item : val_list) {
                    if (item[1].equals("1")) {
                        trunk_access.put(item[0].split("\\.")[item[0].split("\\.").length - 1], "trunk");
                    } else {
                        trunk_access.put(item[0].split("\\.")[item[0].split("\\.").length - 1], "access");
                    }
                }
                node_id_trunk_access_mode.put(node, trunk_access);
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : node_id_trunk_access_mode.entrySet()) {
            String node = entry.getKey();
//            System.out.println(node);
            Map<String, String> val_list = entry.getValue();
            Map<String, String[]> tmp_map = new HashMap<>();
            for (Map.Entry<String, String> entry1 : val_list.entrySet()) {
                String id_iface = entry1.getKey();
                String trunk_access_mode = entry1.getValue();
//                System.out.println(id_iface+" - "+trunk_access_mode);
                String native_vlan = "";
                String tag_vlans = "";
                String access_vlan = "";
                if (trunk_access_mode.equals("trunk")) {
                    if (walkInformationFromNodes.get("PortTrunkNativeVlanCisco") != null && 
                            walkInformationFromNodes.get("PortTrunkNativeVlanCisco").get(node) != null) {
                        for (String[] item : walkInformationFromNodes.get("PortTrunkNativeVlanCisco").get(node)) {
                            if (id_iface.equals(item[0].split("\\.")[item[0].split("\\.").length - 1])) {
                                native_vlan = item[1] + ":trunk";
                                break;
                            }

                        }
                    }

                    if (walkInformationFromNodes.get("PortTrunkVlanCisco") != null && 
                            walkInformationFromNodes.get("PortTrunkVlanCisco").get(node) != null) {
                        for (String[] item : walkInformationFromNodes.get("PortTrunkVlanCisco").get(node)) {
                            if (id_iface.equals(item[0].split("\\.")[item[0].split("\\.").length - 1])) {
                                tag_vlans = utils.hexMapToVlans(item[1]);
                                break;
                            }
                        }
                    }
                } else {
                    if (walkInformationFromNodes.get("cisco-vlan-membership") != null && 
                            walkInformationFromNodes.get("cisco-vlan-membership").get(node) != null) {
                        for (String[] item : walkInformationFromNodes.get("cisco-vlan-membership").get(node)) {
                            String id_iface1 = item[0].split("\\.")[item[0].split("\\.").length - 1];
                            String id_vlan = item[1];

                            if (id_iface.equals(id_iface1)) {
                                access_vlan = id_vlan + ":access";
                                break;
                            }

                        }
                    }
                }

                String[] mas = new String[2];
                if (trunk_access_mode.equals("trunk")) {
                    mas[0] = native_vlan;
                    mas[1] = tag_vlans;
                } else {
                    mas[0] = access_vlan;
                    mas[1] = "";
                }
                tmp_map.put(id_iface, mas);
            }
            result.put(node, tmp_map);
        }

        return result;
    }

    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> getVlanHP(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, Map<String, String>> index_idvlan = new HashMap<>();
        Map<String, ArrayList> index_idifaceUntag = new HashMap<>();
        Map<String, ArrayList> index_idifaceTag = new HashMap<>();
        Map<String, ArrayList> idiface_idvlanUntag = new HashMap<>();
        Map<String, ArrayList> idiface_idvlanTag = new HashMap<>();

        // output: node ---> index ---> id_vlan
        if(walkInformationFromNodes.get("VlanIdHP") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("VlanIdHP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> map_tmp = new HashMap<>();
                for (String[] item1 : val_list) {
                    String index = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                    map_tmp.put(index, item1[1]);
                }
                index_idvlan.put(node, map_tmp);
            }
        }

        // output: node ---> ArrayList(index, id_iface)
        if(walkInformationFromNodes.get("VlanPortStateHP") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("VlanPortStateHP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> list_tmp = new ArrayList();
                for (String[] item1 : val_list) {
                    if (item1[1].equals("2")) {
                        String[] mas = new String[2];
                        mas[0] = item1[0].split("\\.")[item1[0].split("\\.").length - 2];
                        mas[1] = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                        list_tmp.add(mas);
                    }
                }
                index_idifaceUntag.put(node, list_tmp);
            }
        }

        // output: node ---> ArrayList(index, id_iface)
        if(walkInformationFromNodes.get("VlanPortStateHP") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("VlanPortStateHP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> list_tmp = new ArrayList();
                for (String[] item1 : val_list) {
                    if (item1[1].equals("1")) {
                        String[] mas = new String[2];
                        mas[0] = item1[0].split("\\.")[item1[0].split("\\.").length - 2];
                        mas[1] = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                        list_tmp.add(mas);
                    }
                }
                index_idifaceTag.put(node, list_tmp);
            }
        }

        // output: node ---> ArrayList(id_iface, id_vlanUntag)
        for (Map.Entry<String, ArrayList> entry : index_idifaceUntag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                String[] mas = new String[2];
                mas[0] = item1[1];
                if (index_idvlan.get(node) != null) {
                    mas[1] = index_idvlan.get(node).get(item1[0]);
                } else {
                    mas[1] = "";
                }
                list_tmp.add(mas);
            }
            idiface_idvlanUntag.put(node, list_tmp);
        }

        // output: node ---> ArrayList(id_iface, id_vlanTag)
        for (Map.Entry<String, ArrayList> entry : index_idifaceTag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                String[] mas = new String[2];
                mas[0] = item1[1];
                if (index_idvlan.get(node) != null) {
                    mas[1] = index_idvlan.get(node).get(item1[0]);
                } else {
                    mas[1] = "";
                }
                list_tmp.add(mas);
            }
            idiface_idvlanTag.put(node, list_tmp);
        }

        for (Map.Entry<String, ArrayList> entry : idiface_idvlanUntag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String[]> map_tmp = new HashMap<>();
            for (String[] item1 : val_list) {
                String id_iface = item1[0];
                String untag = item1[1];
                ArrayList<String[]> tmp = idiface_idvlanTag.get(node);
                String tag = "";
                if (tmp != null) {
                    for (String[] item2 : tmp) {
                        if (item2[0].equals(id_iface)) {
                            tag = item2[1];
                            break;
                        }
                    }
                }
                String[] mas = new String[2];
                if (tag.isEmpty()) {
                    mas[0] = untag + ":access";
                    mas[1] = "";
                } else {
                    mas[0] = untag + ":trunk";
                    mas[1] = tag;
                }
                map_tmp.put(id_iface, mas);
            }
            result.put(node, map_tmp);
        }

        return result;
    }

    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> getVlanRFC2674(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, ArrayList<String[]>> idvlan_untagport = new HashMap<>();
        Map<String, ArrayList<String[]>> idvlan_tagport = new HashMap<>();

        // output: node ---> list(id_vlan, Untag_ports)
        if(walkInformationFromNodes.get("UnTaggedVlan") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("UnTaggedVlan").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                for (String[] item : val_list) {
                    String[] tmp = new String[2];
                    tmp[0] = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    tmp[1] = utils.hexMapToVlans(item[1]);
                    if (!tmp[1].isEmpty()) {
                        String[] fields = tmp[1].split(":");
                        tmp[1] = "";
                        for (String field : fields) {
                            if (tmp[1].isEmpty()) {
                                tmp[1] = String.valueOf(Integer.parseInt(field) + 1);
                            } else {
                                tmp[1] = tmp[1] + ":" + (Integer.parseInt(field) + 1);
                            }
                        }
                        boolean found = false;
                        for (String[] item1 : tmp_list) {
                            if (item1[0].equals(tmp[0])) {
                                found = true;
                                String[] mas = new String[2];
                                mas[0] = item1[0];
                                mas[1] = item1[1] + ":" + tmp[1];
                                tmp_list.remove(item1);
                                tmp_list.add(mas);
                                break;
                            }
                        }
                        if (!found) {
                            tmp_list.add(tmp);
                        }
                    }
                }
                idvlan_untagport.put(node, tmp_list);
            }
        }

        // output: node ---> list(id_vlan, tag_ports)
        if(walkInformationFromNodes.get("TaggedVlan") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("TaggedVlan").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                for (String[] item : val_list) {
                    String[] tmp = new String[2];
                    tmp[0] = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    tmp[1] = utils.hexMapToVlans(item[1]);
                    if (!tmp[1].isEmpty()) {
                        String[] fields = tmp[1].split(":");
                        tmp[1] = "";
                        for (String field : fields) {
                            if (tmp[1].isEmpty()) {
                                tmp[1] = String.valueOf(Integer.parseInt(field) + 1);
                            } else {
                                tmp[1] = tmp[1] + ":" + (Integer.parseInt(field) + 1);
                            }
                        }
                        boolean found = false;
                        for (String[] item1 : tmp_list) {
                            if (item1[0].equals(tmp[0])) {
                                found = true;
                                String[] mas = new String[2];
                                mas[0] = item1[0];
                                mas[1] = item1[1] + ":" + tmp[1];
                                tmp_list.remove(item1);
                                tmp_list.add(mas);
                                break;
                            }
                        }
                        if (!found) {
                            tmp_list.add(tmp);
                        }
                    }
                }
                idvlan_tagport.put(node, tmp_list);
            }
        }

        for (Map.Entry<String, ArrayList<String[]>> entry : idvlan_untagport.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list_untag = entry.getValue();
            ArrayList<String[]> val_list_tag = idvlan_tagport.get(node);
            ArrayList<String[]> port_vlan_untag = new ArrayList();
            for (String[] item : val_list_untag) {
                for (String id_port : item[1].split(":")) {
                    String[] mas = new String[2];
                    mas[0] = id_port;
                    mas[1] = item[0];
                    port_vlan_untag.add(mas);
                }
            }
            ArrayList<String[]> port_vlan_tag = new ArrayList();
            if (val_list_tag != null) {
                for (String[] item : val_list_tag) {
                    for (String id_port : item[1].split(":")) {
                        String[] mas = new String[2];
                        mas[0] = id_port;
                        mas[1] = item[0];
                        port_vlan_tag.add(mas);
                    }
                }
            }

            Map<String, String> idport_vlan_untag = new HashMap<>();
            for (int i = 0; i < port_vlan_untag.size(); i++) {
                String[] item1 = port_vlan_untag.get(i);
                String id_port = item1[0];
                StringBuilder vlan = new StringBuilder(item1[1]);
                for (int j = i + 1; j < port_vlan_untag.size(); j++) {
                    String[] item2 = port_vlan_untag.get(j);
                    if (item1[0].equals(item2[0])) {
                        boolean found = false;
                        for (String it : vlan.toString().split(":")) {
                            if (it.equals(item2[1])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            vlan.append(":").append(item2[1]);
                        }
                        port_vlan_untag.remove(j);
                        j--;
                    }
                }
                idport_vlan_untag.put(id_port, vlan.toString());
            }

            Map<String, String> idport_vlan_tag = new HashMap<>();
            for (int i = 0; i < port_vlan_tag.size(); i++) {
                String[] item1 = port_vlan_tag.get(i);
                String id_port = item1[0];
                StringBuilder vlan = new StringBuilder();
                if (!item1[1].equals(idport_vlan_untag.get(id_port))) {
                    vlan = new StringBuilder(item1[1]);
                }
                for (int j = i + 1; j < port_vlan_tag.size(); j++) {
                    String[] item2 = port_vlan_tag.get(j);
                    if (item1[0].equals(item2[0])) {
                        if (!item2[1].equals(idport_vlan_untag.get(item2[0]))) {
                            if (vlan.isEmpty()) {
                                vlan = new StringBuilder(item2[1]);
                            } else {
                                boolean found = false;
                                for (String it : vlan.toString().split(":")) {
                                    if (it.equals(item2[1])) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    vlan.append(":").append(item2[1]);
                                }
//                                
//                                
//                                
//                                vlan = vlan + ":" + item2[1];
                            }
                        }
                        port_vlan_tag.remove(j);
                        j--;
                    }
                }
                idport_vlan_tag.put(id_port, vlan.toString());
            }

            Map<String, String[]> idport_untag_tag = new HashMap<>();
            for (Map.Entry<String, String> entry1 : idport_vlan_untag.entrySet()) {
                String ip_port = entry1.getKey();
                String untag = entry1.getValue();
                String tag = idport_vlan_tag.get(ip_port);
                String[] mas = new String[2];
                mas[0] = untag;
                mas[1] = tag;
                idport_untag_tag.put(ip_port, mas);
            }
            result.put(node, idport_untag_tag);
        }

        return result;
    }

    private String[] getIfaceName(String node, String id_iface, Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes, boolean with_interface_maping) {
        String[] result = new String[2];
        result[0] = "";
        result[1] = "";

        boolean found_IfaceMaping = false;
        if (with_interface_maping) {
            if (walkInformationFromNodes.get("IfaceMaping") != null && 
                    !walkInformationFromNodes.get("IfaceMaping").isEmpty() && 
                    walkInformationFromNodes.get("IfaceMaping").get(node) != null) {
                for (String[] item0 : walkInformationFromNodes.get("IfaceMaping").get(node)) {
                    if (item0[0].split("\\.")[item0[0].split("\\.").length - 1].equals(id_iface)) {
                        id_iface = item0[1];
                        found_IfaceMaping = true;
                        break;
                    }
                }
            }
        }

        if (!with_interface_maping || found_IfaceMaping) {
            boolean uniqal_all_interface = true;
            if (walkInformationFromNodes.get("ifDescr") != null && 
                    walkInformationFromNodes.get("ifDescr").get(node) != null) {
                ArrayList<String[]> list = walkInformationFromNodes.get("ifDescr").get(node);

                if (list != null) {
                    int i = 0;
                    for (String[] mas : list) {
                        boolean found = false;
                        for (int j = i + 1; j < list.size(); j++) {
                            String[] mas1 = list.get(j);
                            if (mas1[1].equals(mas[1])) {
                                found = true;
                                uniqal_all_interface = false;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                        i++;
                    }
                }

                if (list != null) {
                    for (String[] mas : list) {
                        if (mas[0].split("\\.").length > 0 && mas[0].split("\\.")[mas[0].split("\\.").length - 1].equals(id_iface)) {
                            result[0] = id_iface;
                            result[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(mas[1]));
                            break;
                        }
                    }
                }
                if (!uniqal_all_interface) {
                    if (walkInformationFromNodes.get("IfNameExtendedIfName") != null && 
                            walkInformationFromNodes.get("IfNameExtendedIfName").get(node) != null) {
                        ArrayList<String[]> list_ext = walkInformationFromNodes.get("IfNameExtendedIfName").get(node);
                        if (list_ext != null) {
                            for (String[] mas : list_ext) {
                                if (mas[0].split("\\.").length > 0 && mas[0].split("\\.")[mas[0].split("\\.").length - 1].equals(id_iface)) {
                                    result[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(mas[1]));
                                    break;
                                }
                            }
                        }

                    }

                }
            }
        }

        return result;
    }

    //  output: PhysicalDescr ---> serialnumber
    private Map<String, Map> getSerialNumberFromNodes(
            Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, Map> result = new HashMap<>();

        if (walkInformationFromNodes.get("entPhysicalDescr") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("entPhysicalDescr").entrySet()) {
                String node = entry.getKey();
                //            System.out.println(node);
                ArrayList<String[]> physicalDescr_list = entry.getValue();
                Map<String, String> res = new HashMap<>();
                if(walkInformationFromNodes.get("entPhysicalSerialNumber") != null) {
                    ArrayList<String[]> serialnumber_list = walkInformationFromNodes.get("entPhysicalSerialNumber").get(node);
                    if (serialnumber_list != null) {
                        for (int i = 0; i < physicalDescr_list.size(); i++) {
                            if (physicalDescr_list.get(i) != null && serialnumber_list.size() > i && serialnumber_list.get(i) != null) {
                                String physicalDescr = physicalDescr_list.get(i)[1];
                                String serialnumber = serialnumber_list.get(i)[1];
                                if (!serialnumber.isEmpty()) {
                                    res.put(physicalDescr, serialnumber);
                                }
                            }
                        }
                    }
                }
                result.put(node, res);
            }
        }
        return result;
    }

    //  output: node ---> id_iface ---> interface_information
    private Map<String, Map<String, Map<String, String>>> getInterfacesInformationFromNodes(
            Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes,
            Map<String, Map<String, String>> duplex_mode, Map<String, Map<String, ArrayList<String>>> interface_ipaddress,
            Map<String, Map<String, String[]>> getVlanPortUntagTag) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        if(walkInformationFromNodes.get("ifIndex") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("ifIndex").entrySet()) {
                String node = entry.getKey();
    //            System.out.println(node);
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, Map<String, String>> res_map = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[1];
                    String[] res = getIfaceName(node, id_iface, walkInformationFromNodes, false);
                    String iface_name = res[1];
                    id_iface = res[0];

                    if (!iface_name.isEmpty()) {
                        Map tmp_map = new HashMap<>();
                        if(walkInformationFromNodes.get("ifMTU") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifMTU").get(node);
                            if (list1 != null) {
                                for (String[] mas1 : list1) {
                                    if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                        tmp_map.put("mtu", mas1[1]);
                                        break;
                                    }
                                }
                            }
                        }

                        if(walkInformationFromNodes.get("ifSpeed") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifSpeed").get(node);
                            if (list1 != null) {
                                for (String[] mas1 : list1) {
                                    if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                        switch (mas1[1]) {
                                            case "10000000" ->
                                                tmp_map.put("speed", "10 Mbps");
                                            case "100000000" ->
                                                tmp_map.put("speed", "100 Mbps");
                                            case "1000000000" ->
                                                tmp_map.put("speed", "1 Gbps");
                                            case "10000000000" ->
                                                tmp_map.put("speed", "10 Gbps");
                                            case "100000000000" ->
                                                tmp_map.put("speed", "100 Gbps");
                                            default ->
                                                tmp_map.put("speed", mas1[1]);
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        if (duplex_mode.get(node) != null && duplex_mode.get(node).get(id_iface) != null) {
                            tmp_map.put("duplex", duplex_mode.get(node).get(id_iface));
                        }

                        if(walkInformationFromNodes.get("ifMAC") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifMAC").get(node);
                            if (list1 != null) {
                                for (String[] mas1 : list1) {
                                    if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                        tmp_map.put("mac", utils.replaceDelimiter(utils.translateMAC(mas1[1])));
                                        break;
                                    }
                                }
                            }
                        }

                        if (interface_ipaddress.get(node) != null && interface_ipaddress.get(node).get(id_iface) != null) {
                            tmp_map.put("ip", interface_ipaddress.get(node).get(id_iface));
                        }

                        if(walkInformationFromNodes.get("ifAdminStatus") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifAdminStatus").get(node);
                            if (list1 != null) {
                                for (String[] mas1 : list1) {
                                    if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                        String status;
                                        if (mas1[1].equals("1")) {
                                            status = "up";
                                        } else {
                                            status = "down";
                                        }
                                        tmp_map.put("admin_status", status);
                                        break;
                                    }
                                }
                            }
                        }

                        if(walkInformationFromNodes.get("ifOperStatus") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifOperStatus").get(node);
                            if (list1 != null) {
                                for (String[] mas1 : list1) {
                                    if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                        String status;
                                        if (mas1[1].equals("1")) {
                                            status = "up";
                                        } else {
                                            status = "down";
                                        }
                                        tmp_map.put("operation_status", status);
                                        break;
                                    }
                                }
                            }
                        }

                        if (getVlanPortUntagTag.get(node) != null && getVlanPortUntagTag.get(node).get(id_iface) != null && getVlanPortUntagTag.get(node).get(id_iface).length == 2) {
                            String[] mas1 = getVlanPortUntagTag.get(node).get(id_iface)[0].split(":", -1);
                            if (mas1.length == 2) {
                                if (mas1[1].equals("access")) {
                                    tmp_map.put("mode", "access");
                                    tmp_map.put("access_vlan", mas1[0]);
                                } else if (mas1[1].equals("trunk")) {
                                    tmp_map.put("mode", "trunk");
                                    tmp_map.put("trunk_vlan", getVlanPortUntagTag.get(node).get(id_iface)[1].replace(":", ","));
                                    tmp_map.put("native_vlan", mas1[0]);
                                }
                            }
                        }
                        res_map.put(iface_name, tmp_map);
                    }
                }
                result.put(node, res_map);
            }
        }

        return result;
    }

    //  output: node ---> route_information
//    private Map<String, ArrayList<String[]>> getRoutesInformationFromNodes(
//            Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
//        Map<String, ArrayList<String[]>> result = new HashMap();
//        
//        Map<String, Map<String, String>> node_net_RouteDestination = new HashMap();
//        if(walkInformationFromNodes.get("RouteDestination") != null) {
//            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("RouteDestination").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> route_destination_list = entry.getValue();
//                Map<String, String> res = new HashMap();
//                for (String[] val : route_destination_list) {
//                    String[] mas = val[0].split("\\.");
//                    if (mas.length >= 13) {
//                        String network = mas[mas.length - 13] + "." + mas[mas.length - 12] + "." + mas[mas.length - 11] + "." + mas[mas.length - 10];
//                        String destination = val[1];
//                        res.put(network, destination);
//                    }                    
//                }
//                node_net_RouteDestination.put(node, res);
//            }
//        }
//
//        Map<String, Map<String, String>> node_net_RouteMask = new HashMap();
//        if(walkInformationFromNodes.get("RouteMask") != null) {
//            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("RouteMask").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> route_destination_list = entry.getValue();
//                Map<String, String> res = new HashMap();
//                for (String[] val : route_destination_list) {
//                    String[] mas = val[0].split("\\.");
//                    if (mas.length >= 13) {
//                        String network = mas[mas.length - 13] + "." + mas[mas.length - 12] + "." + mas[mas.length - 11] + "." + mas[mas.length - 10];
//                        String mask = val[1];
//                        res.put(network, mask);
//                    }                    
//                }
//                node_net_RouteMask.put(node, res);
//            }
//        }        
//
//        Map<String, Map<String, String>> node_net_RouteType = new HashMap();
//        if(walkInformationFromNodes.get("RouteType") != null) {
//            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("RouteType").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> route_destination_list = entry.getValue();
//                Map<String, String> res = new HashMap();
//                for (String[] val : route_destination_list) {
//                    String[] mas = val[0].split("\\.");
//                    if (mas.length >= 13) {
//                        String network = mas[mas.length - 13] + "." + mas[mas.length - 12] + "." + mas[mas.length - 11] + "." + mas[mas.length - 10];
//                        String type = val[1];
//                        res.put(network, type);
//                    }                    
//                }
//                node_net_RouteType.put(node, res);
//            }
//        } 
//
//        Map<String, Map<String, String>> node_net_RouteProto = new HashMap();
//        if(walkInformationFromNodes.get("RouteProto") != null) {
//            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("RouteProto").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> route_destination_list = entry.getValue();
//                Map<String, String> res = new HashMap();
//                for (String[] val : route_destination_list) {
//                    String[] mas = val[0].split("\\.");
//                    if (mas.length >= 13) {
//                        String network = mas[mas.length - 13] + "." + mas[mas.length - 12] + "." + mas[mas.length - 11] + "." + mas[mas.length - 10];
//                        String proto = val[1];
//                        res.put(network, proto);
//                    }                    
//                }
//                node_net_RouteProto.put(node, res);
//            }
//        } 
//
//        Map<String, Map<String, String>> node_net_RouteAge = new HashMap();
//        if(walkInformationFromNodes.get("RouteAge") != null) {
//            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("RouteAge").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> route_destination_list = entry.getValue();
//                Map<String, String> res = new HashMap();
//                for (String[] val : route_destination_list) {
//                    String[] mas = val[0].split("\\.");
//                    if (mas.length >= 13) {
//                        String network = mas[mas.length - 13] + "." + mas[mas.length - 12] + "." + mas[mas.length - 11] + "." + mas[mas.length - 10];
//                        String age = val[1];
//                        res.put(network, age);
//                    }                    
//                }
//                node_net_RouteAge.put(node, res);
//            }
//        }         
//        //////////////////////////////////////////////
//        for (Map.Entry<String, Map<String, String>> entry : node_net_RouteDestination.entrySet()) {
//            String node = entry.getKey();
//            Map<String, String> route_destination_map = entry.getValue();
//            ArrayList<String[]> res = new ArrayList();
//            for (Map.Entry<String, String> entry1 : route_destination_map.entrySet()) {
//                String network = entry1.getKey();
//                String destination = entry1.getValue();
//                String mask = "";
//                String routeType = "";
//                String routeProto = "";
//                String routeAge = "";
//
//                if(node_net_RouteMask.get(node) != null && 
//                        node_net_RouteMask.get(node).get(network) != null) {
//                    mask = node_net_RouteMask.get(node).get(network);
//                }
//                if(node_net_RouteType.get(node) != null && 
//                        node_net_RouteType.get(node).get(network) != null) {
//                    routeType = node_net_RouteType.get(node).get(network);
//                }                    
//                if(node_net_RouteProto.get(node) != null && 
//                        node_net_RouteProto.get(node).get(network) != null) {
//                    routeProto = node_net_RouteProto.get(node).get(network);
//                }  
//                if(node_net_RouteAge.get(node) != null && 
//                        node_net_RouteAge.get(node).get(network) != null) {
//                    routeAge = node_net_RouteAge.get(node).get(network);
//                }                    
//
//                if (!(network.isEmpty() || destination.isEmpty() || mask.isEmpty() || routeType.isEmpty() || routeProto.isEmpty() || routeAge.isEmpty())) {
//                    String[] mas_tmp = {network, mask, destination, routeType, routeProto, routeAge};
//                    res.add(mas_tmp);
//                }
//            }
//            result.put(node, res);
//        }
//
//        return result;
//    }

    //  output: node ---> vlans_information
    private Map<String, Map<String, String>> getVlansInformationFromNodes(Map<String, Map<String, String>> getVlanInform) {
        Map<String, Map<String, String>> result = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : getVlanInform.entrySet()) {
//            String vlan_info = "";
            String node = entry.getKey();
            Map<String, String> res = new HashMap();
            for (Map.Entry<String, String> entry1 : entry.getValue().entrySet()) {
                String id_vlan = entry1.getKey();
                String name_vlan = entry1.getValue();
                res.put(id_vlan, name_vlan);
            }
            result.put(node, res);
        }
        return result;
    }

    // Output format: ArrayList(node, id_iface, name_iface, remote_node, id_iface_remote, name_iface_remote)
    private Map<String, Map<String, ArrayList<Map<String, String>>>> getDP(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, ArrayList> getLLDP = getLLDP(walkInformationFromNodes);
        Map<String, ArrayList> getCDP = getCDP(walkInformationFromNodes);

        Map<String, Map<String, ArrayList<Map<String, String>>>> result = new HashMap<>();

        for (Map.Entry<String, ArrayList> entry : getCDP.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, ArrayList<Map<String, String>>> res = new HashMap<>();
            for (String[] mas : val_list) {
                Map<String, String> map_tmp1 = new HashMap<>();
                map_tmp1.put("remote_ip", mas[2]);
                map_tmp1.put("remote_id", mas[5]);
                map_tmp1.put("remote_port_id", mas[4]);
                map_tmp1.put("remote_version", mas[6]);
                map_tmp1.put("type", "cdp");

                if (res.get(mas[1]) != null) {
                    res.get(mas[1]).add(map_tmp1);
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map_tmp1);
                    res.put(mas[1], list_tmp);
                }
            }
            result.put(node, res);
        }

        for (Map.Entry<String, ArrayList> entry : getLLDP.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, ArrayList<Map<String, String>>> res = new HashMap<>();
            for (String[] mas : val_list) {
//                String local_port = mas[1];
                Map<String, String> map_tmp1 = new HashMap<>();
                map_tmp1.put("remote_ip", mas[2]);
                map_tmp1.put("remote_id", mas[5]);
                map_tmp1.put("remote_port_id", mas[4]);
                map_tmp1.put("remote_version", mas[6]);
                map_tmp1.put("type", "lldp");
                
                if (result.get(node) != null) {
                    if (result.get(node).get(mas[1]) == null) {
                        ArrayList<Map<String, String>> list_tmp = new ArrayList();
                        list_tmp.add(map_tmp1);
                        result.get(node).put(mas[1], list_tmp);
                    }
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map_tmp1);
                    res.put(mas[1], list_tmp);
                    result.put(node, res);
                }                
            }
        }

        return result;
    }

//    private boolean equalsIfaceName(String str1, String str2) {
//        String short_iface;
//        String full_iface;
//        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
//            return false;
//        }
//        str1 = str1.toLowerCase();
//        str2 = str2.toLowerCase();
//        String[] mas = str1.split("\\s");
//        str1 = mas[mas.length - 1];
//        str1 = str1.replaceAll("\\W", "");
//        mas = str2.split("\\s");
//        str2 = mas[mas.length - 1];
//        str2 = str2.replaceAll("\\W", "");
//        if (str1.length() > str2.length()) {
//            short_iface = str2;
//            full_iface = str1;
//        } else {
//            short_iface = str1;
//            full_iface = str2;
//        }
//
//        if (short_iface.equals(full_iface)) {
//            return true;
//        }
//
//        Pattern p = Pattern.compile("^(.*?)(\\d+?)$");
//        Matcher m = p.matcher(short_iface);
//        String[] short_iface_mas = new String[2];
//        if (m.matches()) {
//            short_iface_mas[0] = m.group(1);
//            short_iface_mas[1] = m.group(2);
//        } else {
//            return false;
//        }
//
//        Matcher m1 = p.matcher(full_iface);
//        String[] full_iface_mas = new String[2];
//        if (m1.matches()) {
//            full_iface_mas[0] = m1.group(1);
//            full_iface_mas[1] = m1.group(2);
//        } else {
//            return false;
//        }
//
//        return (!short_iface_mas[0].isEmpty() && !full_iface_mas[0].isEmpty())
//                && full_iface_mas[0].startsWith(short_iface_mas[0])
//                && short_iface_mas[1].equals(full_iface_mas[1]);
//    }

    // Output format: node ---> ArrayList(id_port, remote_node, id_port_remote, name_port_remote)
    private Map<String, ArrayList> getLLDP(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, ArrayList> result = new HashMap<>();

        Map<String, ArrayList<String>> hash_ip = getIpAddress(walkInformationFromNodes);

        Map<String, ArrayList<String[]>> tmp_map = walkInformationFromNodes.get("lldpRemManAddrIfSubtype");
        if (tmp_map != null && !tmp_map.isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : tmp_map.entrySet()) {
                String node = entry.getKey();
//                System.out.println("lldpRemManAddrIfSubtype node="+node);
//                if(result.get(node) == null) {
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                for (int i = 0; i < val_list.size(); i++) {
                    String[] item = val_list.get(i);

                    String[] buf = item[0].split("\\.");
                    if (buf.length == 20) {

                        String ip = buf[16] + "." + buf[17] + "." + buf[18] + "." + buf[19];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                            System.out.println(buf[12]+" --- "+ip);
                            String id_iface = buf[12];
                            String name_iface = "";
                            String node_remote = ip;
                            String id_iface_remote = buf[13];
                            String name_iface_remote = "";
                            String sysname_remote = "";
                            String sysdescr_remote = "";
                            
//                            if(node_remote.equals("10.97.3.90"))
//                                System.out.println("11111111111");

                            if(walkInformationFromNodes.get("lldpRemSysName") != null) {
                                ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysName").get(node);
                                if (tmp_list1 != null && tmp_list1.size() > i) {
                                    for(String[] it : tmp_list1) {
                                        String[] mas = it[0].split("\\.");
                                        if(mas[12].equals(id_iface) && mas[13].equals(id_iface_remote)) {
                                            sysname_remote = it[1];
                                            break;
                                        }
                                    }
//                                    sysname_remote = tmp_list1.get(i)[1];
                                }
                            }

                            if(walkInformationFromNodes.get("lldpRemSysDesc") != null) {
                                ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysDesc").get(node);
                                if (tmp_list1 != null && tmp_list1.size() > i) {
                                    for(String[] it : tmp_list1) {
                                        String[] mas = it[0].split("\\.");
                                        if(mas[12].equals(id_iface) && mas[13].equals(id_iface_remote)) {
                                            sysdescr_remote = it[1];
                                            break;
                                        }
                                    }                                    
//                                    sysdescr_remote = tmp_list1.get(i)[1];
                                }
                            }

                            if (!node_remote.isEmpty() && walkInformationFromNodes.get("lldpRemPortId") != null) {
                                ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemPortId").get(node);
                                if (tmp_list1 != null) {
                                    for(String[] it : tmp_list1) {
                                        String[] mas = it[0].split("\\.");
                                        if(mas[12].equals(id_iface) && mas[13].equals(id_iface_remote)) {
                                            String[] mas1 = getRemotePort(node_remote, id_iface_remote, it[1], walkInformationFromNodes, hash_ip);
                                            id_iface_remote = mas1[0];
                                            name_iface_remote = mas1[1];
                                            break;
                                        }
                                    }                                    
                                    
//                                    String[] mas = getRemotePort(node_remote, id_iface_remote, tmp_list1.get(i)[1], walkInformationFromNodes, hash_ip);
//                                    id_iface_remote = mas[0];
//                                    name_iface_remote = mas[1];

                                }
                            }
// --------------------------------------------
                            if (!id_iface.isEmpty() && !node_remote.isEmpty() &&
                                    !name_iface_remote.isEmpty() &&
                                    walkInformationFromNodes.get("ldpLocPortId") != null) {
                                ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("ldpLocPortId").get(node);
                                if (tmp_list1 != null) {
                                    for (String[] item1 : tmp_list1) {
                                        String last = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                                        if (last.equals(id_iface)) {
                                            name_iface = item1[1];
                                            break;
                                        }
                                    }
                                }
                            }
                            if (name_iface.isEmpty()) {
                                String[] mas1 = getIfaceName(node, id_iface, walkInformationFromNodes, true);
                                if (!mas1[0].isEmpty() && !mas1[1].isEmpty()) {
                                    String[] mas = new String[7];
                                    mas[0] = utils.replaceDelimiter(mas1[0]);
                                    mas[1] = utils.replaceDelimiter(mas1[1]);
                                    mas[2] = node_remote;
                                    mas[3] = utils.replaceDelimiter(id_iface_remote);
                                    mas[4] = utils.replaceDelimiter(name_iface_remote);
                                    mas[5] = utils.replaceDelimiter(sysname_remote);
                                    mas[6] = utils.replaceDelimiter(sysdescr_remote);
                                    tmp_list.add(mas);
//                                        System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                } else {
                                    if(walkInformationFromNodes.get("ifDescr") != null) {
                                        mas1 = searchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                        if (mas1[0] != null && mas1[1] != null) {
                                            String[] mas = new String[7];
                                            mas[0] = utils.replaceDelimiter(mas1[0]);
                                            mas[1] = utils.replaceDelimiter(mas1[1]);
                                            mas[2] = node_remote;
                                            mas[3] = utils.replaceDelimiter(id_iface_remote);
                                            mas[4] = utils.replaceDelimiter(name_iface_remote);
                                            mas[5] = utils.replaceDelimiter(sysname_remote);
                                            mas[6] = utils.replaceDelimiter(sysdescr_remote);
                                            tmp_list.add(mas);
//                                            System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                        }
                                    }
                                }
                            } else {
                                String[] mas = new String[7];
                                mas[0] = utils.replaceDelimiter(id_iface);
                                mas[1] = utils.replaceDelimiter(name_iface);
                                mas[2] = node_remote;
                                mas[3] = utils.replaceDelimiter(id_iface_remote);
                                mas[4] = utils.replaceDelimiter(name_iface_remote);
                                mas[5] = utils.replaceDelimiter(sysname_remote);
                                mas[6] = utils.replaceDelimiter(sysdescr_remote);
                                tmp_list.add(mas);                                
                            }
                        }
                    }
                }

                result.put(node, tmp_list);
            }

        }

        if (walkInformationFromNodes.get("lldpRemChassisId") != null) {
            Map<String, String> chassisId_node = new HashMap();
            if(walkInformationFromNodes.get("lldpLocChassisId") != null) {
                for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("lldpLocChassisId").entrySet()) {
                    String node = entry.getKey();
                    ArrayList<String[]> val_list = entry.getValue();
                    for(String[] iter : val_list) {
                        chassisId_node.put(iter[1], node);
                    }
                }
            }             
            
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("lldpRemChassisId").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();

                for (int i = 0; i < val_list.size(); i++) {
                    String[] item = val_list.get(i);
                    String id_iface = item[0].split("\\.")[item[0].split("\\.").length - 2];
                    String name_iface = "";
                    String node_remote = chassisId_node.get(item[1]);
                    if(node_remote == null) {
                        node_remote = item[1];
                    }                          
                    
//                    String mac = utils.extractMACFromName(item[1]).replace(":", "").replace(".", "").replace("-", "");
//                    String[] node_iface = mac_node_iface.get(mac);
//                    if(node_iface != null) {
//                        node_remote = node_iface[0];
//                    }
                    
                    String id_iface_remote = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    String name_iface_remote = "";
                    String sysname_remote = "";
                    String sysdescr_remote = "";

                    if(walkInformationFromNodes.get("lldpRemSysName") != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysName").get(node);
                        if (tmp_list1 != null && tmp_list1.size() > i) {
                            if (!node_remote.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                node_remote = tmp_list1.get(i)[1] + "(" + node_remote + ")";
                            }
                            sysname_remote = tmp_list1.get(i)[1];
                        }
                    }

                    if(walkInformationFromNodes.get("lldpRemSysDesc") != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysDesc").get(node);
                        if (tmp_list1 != null && tmp_list1.size() > i) {
                            sysdescr_remote = tmp_list1.get(i)[1];
                        }
                    }

//                    String last=item[0].split("\\.")[item[0].split("\\.").length - 1];
//                    String mac_remote = utils.translateMAC(item[1]);
//
//                    if (mac_remote.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
//                        if (mac_node.get(mac_remote) != null) {
//                            node_remote = mac_node.get(mac_remote);
//                        }
//                    } else {
//                        node_remote = "";
//                    }

                    if (!node_remote.isEmpty() && walkInformationFromNodes.get("lldpRemPortId") != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemPortId").get(node);
                        if (tmp_list1 != null && tmp_list1.size() > i) {
                            String[] mas = getRemotePort(node_remote, id_iface_remote, tmp_list1.get(i)[1], walkInformationFromNodes, hash_ip);
                            id_iface_remote = mas[0];
                            name_iface_remote = mas[1];
                        }
                    }

                    if (!id_iface.isEmpty() && !node_remote.isEmpty() && !name_iface_remote.isEmpty()) {
                        if(walkInformationFromNodes.get("ldpLocPortId") != null) {
                            ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("ldpLocPortId").get(node);
                            if (tmp_list1 != null && tmp_list1.size() > i) {
                                for (String[] item1 : tmp_list1) {
                                    String last = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                                    if (last.equals(id_iface)) {
                                        name_iface = item1[1];
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!id_iface.isEmpty()) {
                        String[] mas1 = getIfaceName(node, id_iface, walkInformationFromNodes, true);
                        if (!mas1[0].isEmpty() && !mas1[1].isEmpty()) {
                            String[] mas = new String[7];
                            mas[0] = mas1[0];
                            mas[1] = mas1[1];
                            mas[2] = node_remote;
                            mas[3] = id_iface_remote;
                            mas[4] = name_iface_remote;
                            mas[5] = sysname_remote;
                            mas[6] = sysdescr_remote;
                            tmp_list.add(mas);
//                                System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                        } else {
                            if(walkInformationFromNodes.get("ifDescr") != null) {
                                mas1 = searchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                if (mas1[0] != null && mas1[1] != null) {
                                    String[] mas = new String[7];
                                    mas[0] = mas1[0];
                                    mas[1] = mas1[1];
                                    mas[2] = node_remote;
                                    mas[3] = id_iface_remote;
                                    mas[4] = name_iface_remote;
                                    mas[5] = sysname_remote;
                                    mas[6] = sysdescr_remote;
                                    tmp_list.add(mas);
//                                    System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                }
                            }
                        }
                    }
                }

                if(result.get(node) == null) {
                    result.put(node, tmp_list);
                } else {
                    Map<String, String[]> id_info = new HashMap();
                    ArrayList<String[]> info_list = result.get(node);
                    for(String[] iter : info_list) {
                        id_info.put(iter[0], iter);
                    }
                    
                    for(String[] iter : tmp_list) {
                        if(id_info.get(iter[0]) == null) {
                            id_info.put(iter[0], iter);
                        } else {
                            boolean replace = false;
                            String node_rem = iter[2].replace(":", "");
                            String node_rem_prev = id_info.get(iter[0])[2].replace(":", "");
                            String iface_rem = iter[4].replace(":", "");
                            String iface_rem_prev = id_info.get(iter[0])[4].replace(":", "");
                            
                            if(!node_rem_prev.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && 
                                    node_rem.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
                                replace = true;
                            else {
                                if(node_rem_prev.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && 
                                        node_rem.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {                              
                                    if(iface_rem_prev.matches("[0-9a-f]{12}") && 
                                            !iface_rem.matches("[0-9a-f]{12}"))
                                        replace = true;
                                    if(!iface_rem_prev.matches("[0-9a-f]{12}") && 
                                            !iface_rem.matches("[0-9a-f]{12}") && 
                                            iface_rem.length() > iface_rem_prev.length())
                                        replace = true; 
                                }
                            }
                            if(replace)
                                id_info.put(iter[0], iter);
                            
                        }
                    }
                    ArrayList<String[]> tmp_list_new = new ArrayList();
                    for (Map.Entry<String, String[]> entry1 : id_info.entrySet()) {
                        String[] mas = entry1.getValue();
                        tmp_list_new.add(mas);
                    }
                    result.put(node, tmp_list_new);
                }
            }

        }

        return result;
    }

    // Output format: node ---> ArrayList(id_port, remote_node, id_port_remote, name_port_remote)
    private Map<String, ArrayList> getCDP(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, ArrayList> result = new HashMap<>();

        Map<String, ArrayList<String>> hash_ip = getIpAddress(walkInformationFromNodes);
        if(walkInformationFromNodes.get("cdpCacheAddress") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("cdpCacheAddress").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                for (String[] item : val_list) {
                    String[] buf = item[0].split("\\.");
                    String ip = utils.translateIP(item[1]);
                    if (ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && buf.length >= 16) {
                        String id_iface = buf[14];
                        String name_iface = "";
                        String node_remote = ip;
                        String id_iface_remote = buf[15];
                        String name_iface_remote = "";
                        String sysname_remote = "";
                        String sysdescr_remote = "";

                        if (walkInformationFromNodes.get("cdpCacheDevicePort") != null && 
                                walkInformationFromNodes.get("cdpCacheDevicePort").get(node) != null) {
                            ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpCacheDevicePort").get(node);
                            for (String[] item1 : tmp_list1) {
                                if (item1[0].equals(oids.get("cdpCacheDevicePort") + "." + id_iface + "." + id_iface_remote)) {
                                    name_iface_remote = item1[1];
                                    String[] res1 = getIfaceName(node, buf[14], walkInformationFromNodes, false);
                                    id_iface = res1[0];
                                    name_iface = res1[1];
                                    break;
                                }
                            }
                        }

                        if (walkInformationFromNodes.get("cdpRemSysName") != null && 
                                walkInformationFromNodes.get("cdpRemSysName").get(node) != null) {
                            ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpRemSysName").get(node);
                            for (String[] item1 : tmp_list1) {
                                if (item1[0].equals(oids.get("cdpRemSysName") + "." + id_iface + "." + id_iface_remote)) {
                                    sysname_remote = item1[1];
                                    break;
                                }
                            }
                        }

                        if (walkInformationFromNodes.get("cdpRemSysDesc") != null && 
                                walkInformationFromNodes.get("cdpRemSysDesc").get(node) != null) {
                            ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpRemSysDesc").get(node);
                            for (String[] item1 : tmp_list1) {
                                if (item1[0].equals(oids.get("cdpRemSysDesc") + "." + id_iface + "." + id_iface_remote)) {
                                    sysdescr_remote = item1[1];
                                    break;
                                }
                            }
                        }

                        if (!id_iface.isEmpty() && !node_remote.isEmpty() && !name_iface_remote.isEmpty()) {
                            String[] res1 = getIfaceName(node, id_iface, walkInformationFromNodes, false);
                            if (!res1[0].isEmpty() && !res1[1].isEmpty()) {
                                String[] mas = new String[7];
                                mas[0] = res1[0];
                                mas[1] = res1[1];
                                if (node_remote.equals("0.0.0.0")) {
                                    mas[2] = sysname_remote + "(" + sysdescr_remote + ")";
                                } else {
                                    mas[2] = node_remote;
                                }
                                String[] mas1 = getRemotePort(node_remote, id_iface_remote, name_iface_remote, walkInformationFromNodes, hash_ip);
                                mas[3] = mas1[0];
                                mas[4] = mas1[1];
                                mas[5] = utils.replaceDelimiter(sysname_remote);
                                mas[6] = utils.replaceDelimiter(sysdescr_remote);
                                tmp_list.add(mas);
    //                            System.out.println("Link adding CDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                            } else {
                                if(walkInformationFromNodes.get("ifDescr") != null) {
                                    res1 = searchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                    if (res1[0] != null && res1[1] != null) {
                                        String[] mas = new String[7];
                                        mas[0] = res1[0];
                                        mas[1] = res1[1];
                                        if (node_remote.equals("0.0.0.0")) {
                                            mas[2] = sysname_remote + "(" + sysdescr_remote + ")";
                                        } else {
                                            mas[2] = node_remote;
                                        }
                                        mas[3] = id_iface_remote;
                                        mas[4] = name_iface_remote;
                                        mas[5] = sysname_remote;
                                        mas[6] = sysdescr_remote;
                                        tmp_list.add(mas);
        //                                    System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                    }
                                }
                            }
                        }

                    }

                }
                result.put(node, tmp_list);
            }
        }
        return result;
    }

    // Output format: mac ---> ip
    private Map<String, Map<String, String>> getARP(ArrayList<String[]> node_community_version) {
        String ArpTable = "1.3.6.1.2.1.3.1.1.2";
        String ArpTable1 = "1.3.6.1.2.1.4.22.1.2";

        Map<String, Map<String, String>> result = new HashMap<>();

        if (!node_community_version.isEmpty()) {
            WalkPool walkPool = new WalkPool();
            Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version, ArpTable, Neb.timeout_thread);
            ArrayList<String[]> node_community_version_error = new ArrayList();
            for (String[] it : node_community_version) {
                if (res.get(it[0]) == null) {
                    node_community_version_error.add(it);
                }
            }
            Map<String, ArrayList> map = new HashMap<>(res);
            if (!node_community_version_error.isEmpty()) {
                res = walkPool.get(node_community_version_error, ArpTable1, Neb.timeout_thread);
                map.putAll(res);
            }

            for (Map.Entry<String, ArrayList> entry : map.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> res1 = new HashMap<>();
                for (String[] item : val_list) {
                    String[] buf = item[0].split("\\.");
                    if (buf.length == 16) {
                        String ip = buf[12] + "." + buf[13] + "." + buf[14] + "." + buf[15];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac = item[1];
                            if (mac != null) {
                                res1.put(mac, ip);
                            }
                        }
                    } else if (buf.length == 15) {
                        String ip = buf[11] + "." + buf[12] + "." + buf[13] + "." + buf[14];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac = item[1];
                            if (mac != null) {
                                res1.put(mac, ip);
                            }
                        }
                    }
                }
                result.put(node, res1);
            }
        }

        return result;
    }

    // Output format: node ---> id_iface ---> list(mac)
//    private Map<String, ArrayList> getMAC(ArrayList<String[]> node_community_version, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        int retries_mac = 10;
//        ArrayList<String> macTable = new ArrayList();
//        macTable.add("1.3.6.1.2.1.17.4.3.1.2");
//        macTable.add("1.3.6.1.2.1.17.7.1.2.2.1.2");
//
//        Map<String, Map<String, ArrayList>> result = new HashMap<>();
//
//        WalkPool walkPool = new WalkPool();
//
//        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
//        for (String oid : macTable) {
//            Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version, oid, Neb.timeout_thread);
//            for (Map.Entry<String, ArrayList<String[]>> entry : res.entrySet()) {
//                String node = entry.getKey();
//                if (!entry.getValue().isEmpty()) {
//                    for (String[] item1 : node_community_version) {
//                        if (node.equals(item1[0])) {
//                            String[] mas = new String[4];
//                            mas[0] = item1[0];
//                            mas[1] = item1[1];
//                            mas[2] = item1[2];
//                            mas[3] = oid;
//                            list_node_community_version_oid.add(mas);
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//
//        ArrayList<String[]> tmp_node_community_version = new ArrayList();
//        for (String[] item : node_community_version) {
//            ArrayList<String> getVlanCommunity = getVlanCommunity(walkInformationFromNodes, item[0]);
//            if (!getVlanCommunity.isEmpty()) {
//                for (String item1 : getVlanCommunity) {
//                    for (String[] item2 : list_node_community_version_oid) {
//                        if (item2[0].equals(item[0])) {
//                            String[] mas = new String[4];
//                            mas[0] = item[0];
//                            mas[1] = item1;
//                            mas[2] = item[2];
//                            mas[3] = item2[3];
//                            tmp_node_community_version.add(mas);
//                            break;
//                        }
//                    }
//                }
//            } else {
//                for (String[] item2 : list_node_community_version_oid) {
//                    if (item2[0].equals(item[0])) {
//                        String[] mas = new String[4];
//                        mas[0] = item[0];
//                        mas[1] = item[1];
//                        mas[2] = item[2];
//                        mas[3] = item2[3];
//                        tmp_node_community_version.add(mas);
//                        break;
//                    }
//                }
//            }
//        }
//
//        for (int i = 0; i < retries_mac; i++) {
//            int num_mac_records = 0;
//            Map<String, ArrayList<String[]>> res = walkPool.get(tmp_node_community_version, Neb.timeout_thread);
//            for (Map.Entry<String, ArrayList<String[]>> entry : res.entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> val_list = entry.getValue();
//                for (String[] item : val_list) {
//                    String mac = "";
//                    String[] buf = item[0].split("\\.");
//                    if (buf.length == 17) {
//                        mac = utils.decToHex(Integer.parseInt(buf[11]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[12]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[13]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[14]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[15]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[16]));
//                    }
//                    if (buf.length == 20) {
//                        mac = utils.decToHex(Integer.parseInt(buf[14]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[15]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[16]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[17]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[18]));
//                        mac = mac + ":" + utils.decToHex(Integer.parseInt(buf[19]));
//                    }
//                    String id_iface = item[1];
//
//                    if (!(mac.isEmpty() || id_iface.isEmpty())) {
//                        if (!result.isEmpty() && result.get(node) != null && result.get(node).get(id_iface) != null) {
//                            boolean find = false;
//                            ArrayList<String> tmp_list = result.get(node).get(id_iface);
//                            for (String item1 : tmp_list) {
//                                if (item1.equals(mac)) {
//                                    find = true;
//                                    break;
//                                }
//                            }
//                            if (!find) {
//                                if(result.get(node) != null) {
//                                    result.get(node).get(id_iface).add(mac);
//                                    num_mac_records++;
//                                }
//                            }
//                        } else if (!result.isEmpty() && result.get(node) != null && result.get(node).get(id_iface) == null) {
//                            ArrayList tmp_list = new ArrayList();
//                            tmp_list.add(mac);
//                            result.get(node).put(id_iface, tmp_list);
//                            num_mac_records++;
//                        } else {
//                            ArrayList tmp_list = new ArrayList();
//                            tmp_list.add(mac);
//                            Map<String, ArrayList> tmp_map = new HashMap<>();
//                            tmp_map.put(id_iface, tmp_list);
//                            result.put(node, tmp_map);
//                            num_mac_records++;
//                        }
//                    }
//                }
//            }
//            System.out.println("retries=" + i + " - " + "num_mac_records=" + num_mac_records);
//        }
//
//        // output: node ---> id ---> id_translate
//        Map<String, Map<String, String>> tmp_map = new HashMap<>();
//        if(walkInformationFromNodes.get("IfaceMaping") != null) {
//            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("IfaceMaping").entrySet()) {
//                String node = entry.getKey();
//                ArrayList<String[]> val_list = entry.getValue();
//                Map<String, String> tmp_map1 = new HashMap<>();
//                for (String[] item : val_list) {
//                    tmp_map1.put(item[0].split("\\.")[item[0].split("\\.").length - 1], item[1]);
//                }
//                tmp_map.put(node, tmp_map1);
//            }
//        }
//
//        Map<String, ArrayList> map = new HashMap<>();
//        for (Map.Entry<String, Map<String, ArrayList>> entry : result.entrySet()) {
//            String node = entry.getKey();
//            Map<String, ArrayList> val_list = entry.getValue();
//            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
//                if (tmp_map.get(node) != null && tmp_map.get(node).get(entry1.getKey()) != null) {
//                    String iface_id = tmp_map.get(node).get(entry1.getKey());
//                    ArrayList<String> val_list1 = entry1.getValue();
//
//                    if (walkInformationFromNodes.get("ifDescr") != null && walkInformationFromNodes.get("ifDescr").get(node) != null) {
//                        for (String[] mas : (ArrayList<String[]>) walkInformationFromNodes.get("ifDescr").get(node)) {
//                            String id_if = mas[0].split("\\.")[mas[0].split("\\.").length - 1];
//                            if (id_if.equals(iface_id)) {
//                                map.put(mas[1], val_list1);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return map;
//    }

    public boolean checkInterfaceName(String interface_name, String interface_name_sec) {
        try {
            if (interface_name != null) {
                if (interface_name.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
                    String[] fields = interface_name.split(":");

                    StringBuilder out = new StringBuilder();
                    for (String octet : fields) {
                        int dec = Integer.parseInt(octet, 16);
                        if (dec != 0) {
                            out.append((char) dec);
                        }

                    }
                    interface_name = out.toString();
                }

                if (interface_name_sec.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
                    String[] fields = interface_name.split(":");

                    StringBuilder out = new StringBuilder();
                    for (String octet : fields) {
                        int dec = Integer.parseInt(octet, 16);
                        if (dec != 0) {
                            out.append((char) dec);
                        }

                    }
                    interface_name_sec = out.toString();
                }

                String iface;
                String iface_long;

                if (!interface_name.equals(interface_name_sec)) {
                    if (interface_name.length() < interface_name_sec.length()) {
                        iface = interface_name;
                        iface_long = interface_name_sec;
                    } else {
                        iface = interface_name_sec;
                        iface_long = interface_name;
                    }

                    iface = iface.toLowerCase();
                    iface_long = iface_long.toLowerCase();
                    Pattern p = Pattern.compile("^([A-Za-z\\s]+)[\\s_-]*(\\d+[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*)$");
                    Matcher m = p.matcher(iface);
                    if (m.find()) {
                        String start = m.group(1);
                        String stop = m.group(2);
                        Pattern p1 = Pattern.compile("^" + start + "([\\w\\s]*)" + stop + "$");
                        Matcher m1 = p1.matcher(iface_long);
                        if (m1.find()) {
                            String diff = m1.group(1);
                            return diff.length() > 3;
                        }

                    }
                } else {
                    return true;
                }
            }

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //  output: id_iface, iface_name
    private String[] searchInterfaceName(String interface_name, ArrayList<String[]> list_interface) {
        String[] result = new String[2];

        if (interface_name != null) {
            interface_name = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(interface_name));

            Pattern p = Pattern.compile("^([A-Za-z\\s]+)[\\s_-]*(\\d+[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*)$");
            Matcher m = p.matcher(interface_name.toLowerCase());
            if (m.find()) {
                String start = m.group(1);
                String stop = m.group(2);
                Pattern p1 = Pattern.compile("^" + start + "[\\w\\s]*" + stop + "$");
                if (list_interface != null) {
                    for (String[] iter : list_interface) {
                        Matcher m1 = p1.matcher(iter[1].toLowerCase());
                        if (m1.matches()) {
                            result[0] = iter[0].split("\\.")[iter[0].split("\\.").length - 1];
                            result[1] = iter[1];
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    private Map<String, ArrayList<String>> getIpAddress(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes) {
        Map<String, ArrayList<String>> result = new HashMap();

        if(walkInformationFromNodes.get("ifIpAddress") != null) {
            for (Map.Entry<String, ArrayList<String[]>> entry : walkInformationFromNodes.get("ifIpAddress").entrySet()) {
                String node = entry.getKey();
                ArrayList<String> tmp_list = getIpAddressFromNode(walkInformationFromNodes, node);
                result.put(node, tmp_list);
            }
        }

        return result;
    }

    private ArrayList<String> getIpAddressFromNode(Map<String, Map<String, ArrayList<String[]>>> walkInformationFromNodes, String node) {
        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";
        ArrayList<String> result = new ArrayList();
        if(walkInformationFromNodes.get("ifIpAddress") != null) {
            if(walkInformationFromNodes.get("ifIpAddress") != null) {
                ArrayList<String[]> list = walkInformationFromNodes.get("ifIpAddress").get(node);
                result.add(node);
                if (list != null) {
                    for (String[] item : list) {
                        String[] tmp = item[0].split("\\.");
                        String ip = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                        String id_iface = item[1];
                        if(walkInformationFromNodes.get("ifOperStatus") != null) {
                            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifOperStatus").get(node);
                            if (list1 != null) {
                                for (String[] item1 : list1) {
                                    if (item1[0].equals(ifOperStatus + "." + id_iface)) {
                                        if (item1[1].equals("1")) {
                                            if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                                                result.add(ip);
                                            } else {
                                                System.out.println("node=" + node + " ip=" + ip + " is not up.");
                                            }
                                        }
            //                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                result.add(node);
            }
        }

//        ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
        ArrayList<String> result_sort = new ArrayList();
//        for(String ip : result) {
//            boolean find=false;
//            for(String network : Neb.networks) {
//                if(lib_utils.InsideInterval(ip, network)) {
//                    result_sort.add(ip);
//                    find=true; 
//                    break;
//                }
//            }
//            if(find) break;
//        }
        result_sort.addAll(result);

        return result_sort;
    }

    private String[] getRemotePort(String node_remote, String id_iface_remote, 
            String name_iface_remote, Map<String, Map<String, 
                    ArrayList<String[]>>> walkInformationFromNodes, 
            Map<String, ArrayList<String>> hash_ip) {
        String[] result = new String[2];
        result[0] = id_iface_remote;
        result[1] = utils.replaceDelimiter(utils.translateHexString_to_SymbolString(name_iface_remote));
        
//        String mac = utils.extractMACFromName(result[1]).replace(":", "").replace(".", "").replace("-", "");
//        String[] node_iface = mac_node_iface.get(mac);
//        if(node_iface != null) {
//            result[1] = node_iface[1];
//        }        

//        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
        node_remote = getRealIpAddress(hash_ip, node_remote);

        if (walkInformationFromNodes.get("ifDescr") != null && 
                walkInformationFromNodes.get("ifDescr").get(node_remote) != null) {
            String[] mas1 = searchInterfaceName(name_iface_remote, walkInformationFromNodes.get("ifDescr").get(node_remote));
            if (mas1[0] != null && mas1[1] != null) {
                result[0] = mas1[0];
                result[1] = mas1[1];
            } else {
                if (name_iface_remote.matches("\\d+")) {
                    String[] res = getIfaceName(node_remote, name_iface_remote, walkInformationFromNodes, true);
                    if (!res[0].isEmpty()) {
                        result[0] = res[0];
                        result[1] = res[1];
                    } else {
                        res = getIfaceName(node_remote, id_iface_remote, walkInformationFromNodes, true);
                        if (!res[0].isEmpty()) {
                            result[0] = res[0];
                            result[1] = res[1];
                        }
                    }
                }
            }
        }

        return result;
    }

    private String getRealIpAddress(Map<String, ArrayList<String>> hash_ip, String ip_search) {
        String result = ip_search;

        for (Map.Entry<String, ArrayList<String>> entry : hash_ip.entrySet()) {
            boolean find = false;
            for (String ip : entry.getValue()) {
                if (ip.equals(ip_search)) {
                    result = entry.getKey();
                    find = true;
                    break;
                }
            }
            if (find) {
                break;
            }
        }
        return result;
    }

}
