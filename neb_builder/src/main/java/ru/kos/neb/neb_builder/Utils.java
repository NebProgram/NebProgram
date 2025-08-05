package ru.kos.neb.neb_builder;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Console;
import static java.lang.Math.abs;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.*;
import org.json.simple.parser.*;
import com.google.gson.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static ru.kos.neb.neb_builder.Neb.DEBUG;
import static ru.kos.neb.neb_builder.Neb.history_dir;
import static ru.kos.neb.neb_builder.Neb.map_file;

import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import static ru.kos.neb.neb_builder.Neb.logger;
import static ru.kos.neb.neb_builder.Neb.utils;

import ru.kos.neb.neb_lib.*;

@SuppressWarnings("ALL")
public class Utils {
//    private final int MAXPOOLTHREADS = 128;
//    private final int NODES_INFO_TIMEOUT = 2*60*60; // 2 hour

    public String getHomePath() {
        StringBuilder result = new StringBuilder();
        try {
            String path = Neb.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String[] mas = path.split("/");
            for (int j = 1; j < mas.length - 1; j++) {
                result.append(mas[j]).append("/");
            }
        } catch (URISyntaxException ex) {
            System.out.println(Utils.class.getName() + " - " + ex);
            System.exit(1);
        }
        return result.toString();
    }

    public Map reloadCfg(String cfg_file) {
        System.out.println("Reload neb.cfg file.");
        Neb.logger.Println("Reload " + Neb.neb_cfg + " file.", Neb.logger.INFO);
        Map cfg = readConfig(cfg_file);
//        Neb.history_map = (String) cfg.get("history_map");
//        Neb.history_dir = Neb.history_map;
        String level_log = (String) cfg.get("level_log");
        if (level_log.equals("INFO")) {
            ru.kos.neb.neb_lib.Utils.DEBUG = false;
            logger.SetLevel(logger.INFO);
            Neb.logger_user.SetLevel(Neb.logger_user.INFO);
        } else if (level_log.equals("DEBUG")) {
            ru.kos.neb.neb_lib.Utils.DEBUG = true;
            logger.SetLevel(logger.DEBUG);
            Neb.logger_user.SetLevel(Neb.logger_user.DEBUG);
        }
        Neb.history_num_days = ((Long) cfg.get("history_num_days")).intValue();
        Neb.log_num_days = ((Long) cfg.get("log_num_days")).intValue();
//        Neb.pause_fast_and_carefully_mac_scanning = ((Long) cfg.get("pause_fast_and_carefully_mac_scanning")).intValue();
//        Neb.run_post_scripts = (String) cfg.get("postscript");
        ArrayList<Long> included_port_tmp = (ArrayList) cfg.get("included_port");
        for (Long port : included_port_tmp) {
            Neb.included_port.add(port.intValue());
        }
        ArrayList<Long> excluded_port_tmp = (ArrayList) cfg.get("excluded_port");
        for (Long port : excluded_port_tmp) {
            Neb.excluded_port.add(port.intValue());
        }
//        Neb.ifaces_counters_extended = (ArrayList)cfg.get("ifaces_counters_extended");
        return cfg;
    }

    public Map readConfig(String filename) {
        Map result = new HashMap();
        JSONParser parser = new JSONParser();

        try {
            /* Get the file content into the JSONObject */
            String str = readFileToStringWithoutComments(filename);
            JSONObject jsonObject = (JSONObject) parser.parse(str);
            result = toMap(jsonObject);
            if (result.get("build_network_time") == null) {
                System.out.println("Not set build_network_time!");
//                System.exit(1);
            }
            if (result.get("areas") == null) {
                System.out.println("Not set areas!");
//                System.exit(1);
            }
            if (((Map) result.get("areas")).isEmpty()) {
                System.out.println("Not set none area!");
//                System.exit(1);
            }
            for (Map.Entry<String, Map> entry : ((Map<String, Map>) result.get("areas")).entrySet()) {
                String area_name = entry.getKey();
                Map map_area = entry.getValue();
                if (map_area.get("networks") == null || ((ArrayList) map_area.get("networks")).isEmpty()) {
                    System.out.println("Not set networks in area " + area_name + "!");
                    ((Map) result.get("areas")).remove(area_name);
                    if (((Map) result.get("areas")).isEmpty()) {
                        System.out.println("Not set none area!");
//                        System.exit(1);
                    }
                } else {
                    ArrayList<String> list = new ArrayList();
                    for (String network : (ArrayList<String>) map_area.get("networks")) {
                        if (!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$")
                                || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$")
                                || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))) {
                            System.out.println("In networks section " + network + " is not correct format!");
                        } else {
                            list.add(network);
                        }
                    }
                    if (!list.isEmpty()) {
                        map_area.put("networks", list);
                    } else {
                        System.out.println("Not set networks in area!");
//                        System.exit(1);                         
                    }

                }

                ArrayList<String> list1 = new ArrayList();
                for (String network : (ArrayList<String>) map_area.get("include")) {
                    if (!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$")
                            || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$")
                            || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))) {
                        System.out.println("In include section " + network + " is not correct format!");
                    } else {
                        list1.add(network);
                    }
                }
                map_area.put("include", list1);

                Map map1 = new HashMap();
                for (String network : (ArrayList<String>) map_area.get("exclude")) {
                    if (!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))) {
                        System.out.println("In exclude section " + network + " is not correct format!");
                    } else {
                        map1.put(network, network);
                    }
                }
                map_area.put("exclude", map1);

                if (map_area.get("snmp_community") == null || ((ArrayList) map_area.get("snmp_community")).isEmpty()) {
                    ArrayList list = new ArrayList();
                    list.add("public");
                    map_area.put("snmp_community", list);
                }

                map_area.putIfAbsent("discovery_networks", "yes");
                map_area.putIfAbsent("calculate_links_from_counters", "yes");
            }
            if (result.get("map_file") == null) {
                result.put("map_file", Neb.history_dir + "neb.map");
            } else {
                result.put("map_file", getAbsolutePath((String) result.get("map_file"), Neb.home_dir));
            }
            result.putIfAbsent("level_log", "INFO");
            result.putIfAbsent("scan_mode", "normal");
//            if (result.get("host_time_live") == null) {
//                result.put("host_time_live", 30);
//            }
//            if (result.get("history_map") == null) {
//                result.put("history_map", Neb.history_dir + "history");
//            } else {
//                result.put("history_map", getAbsolutePath((String) result.get("history_map"), Neb.home_dir));
//            }
            result.putIfAbsent("history_num_days", 365);
            result.putIfAbsent("log_num_days", 41);
            result.putIfAbsent("canvas_color", "255,255,255");
            result.putIfAbsent("term_font_size", 22);
            result.putIfAbsent("term_font_style", "bold");

//            if (result.get("scripts") != null) {
//                if (((Map) result.get("scripts")).get("get_info_node") != null) {
//                    Map<String, ArrayList<String>> val = (Map) ((Map) result.get("scripts")).get("get_info_node");
//                    for (Map.Entry<String, ArrayList<String>> entry1 : val.entrySet()) {
//                        String protocol = entry1.getKey();
//                        ArrayList<String> lst = entry1.getValue();
//                        int i = 0;
//                        for (String command : lst) {
//                            String[] mas = splitCommandString(command);
//                            if (mas[0].matches("^portable.+")) {
//                                mas[0] = (Neb.home_dir + mas[0]).replace("\\", "/");
//                            } else {
//                                mas[0] = mas[0].replace("\\", "/");
//                            }
//                            mas[mas.length - 1] = getAbsolutePath(mas[mas.length - 1], Neb.home_dir);
//                            command = "";
//                            for (String str1 : mas) {
//                                command = command + " " + str1;
//                            }
//                            command = command.trim();
//                            ((ArrayList<String>) ((Map) ((Map) result.get("scripts")).get("get_info_node")).get(protocol)).set(i, command);
//                            i = i + 1;
//                        }
//                    }
//                }
//                if (((Map) result.get("scripts")).get("cli-test") != null) {
//                    ArrayList<String> val = (ArrayList) ((Map) result.get("scripts")).get("cli-test");
//                    int i = 0;
//                    for (String command : val) {
//                        String[] mas = splitCommandString(command);
//                        if (mas[0].matches("^portable.+")) {
//                            mas[0] = (Neb.home_dir + mas[0]).replace("\\", "/");
//                        } else {
//                            mas[0] = mas[0].replace("\\", "/");
//                        }
//                        mas[mas.length - 1] = getAbsolutePath(mas[mas.length - 1], Neb.home_dir);
//                        command = "";
//                        for (String str1 : mas) {
//                            command = command + " " + str1;
//                        }
//                        command = command.trim();
//                        ((ArrayList<String>) ((Map) result.get("scripts")).get("cli-test")).set(i, command);
//                        i = i + 1;
//                    }
//                }
//            }
//            CheckConfig(result);

        } catch (ParseException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            System.out.println(Utils.class.getName() + " - " + ex);
            System.exit(1);
        }

        return result;
    }

    public String getAbsolutePath(String file, String home_dir) {
        if (!file.contains(":") && !file.startsWith("/")) {
            return (home_dir + file).replace("\\", "/");
        } else {
            return file.replace("\\", "/");
        }
    }

//    private void CheckConfig(Map cfg) {
//        if(cfg.get("areas") == null || ((Map)cfg.get("areas")).size() == 0) {
//            System.out.println("Not set section areas in cfg file.");
//            System.exit(1);
//        }
//        if(cfg.get("scripts") == null) {
//            System.out.println("Not set section scripts in cfg file.");
//            System.exit(1);
//        }
//
//        if(((Map)cfg.get("scripts")).get("get_info_node") == null || ((Map)((Map)cfg.get("scripts")).get("get_info_node")).size() == 0) {
//            System.out.println("Not set section scripts->get_info_node in cfg file.");
//            System.exit(1);
//        }
//        if(((Map)cfg.get("scripts")).get("cli-test") != null) {
//            String cli_test = (String)((ArrayList)((Map)cfg.get("scripts")).get("cli-test")).get(0);
//            String[] mas = splitCommandString(cli_test);
//            File file1_s = new File(mas[0].replace("\"", ""));            
//            File file1 = new File((Neb.home_dir+mas[0]).replace("\"", ""));
//            File file2_s = new File(mas[mas.length-1].replace("\"", ""));
//            File file2 = new File((Neb.home_dir+mas[mas.length-1]).replace("\"", ""));
//            if(!file1_s.exists() && !file1.exists()) {
//                if(!(mas[0].indexOf("/") >= 0 || mas[0].indexOf("\\") >= 0) && !Find_Path_Env(mas[0])) {
//                    System.out.println("Not find java interpreter scripts->cli-test in cfg file.");
//                    System.exit(1);  
//                }
//            }
//            if(!file2_s.exists() && !file2.exists()) {
//                System.out.println("Not find java program scripts->cli-test in cfg file.");
//                System.exit(1);            
//            }
//        }
//
//        Map<String, ArrayList<String>> get_info_node = (Map)((Map)cfg.get("scripts")).get("get_info_node");
//        for (Map.Entry<String, ArrayList<String>> entry : get_info_node.entrySet()) {
//            String key = entry.getKey();
//            String val = entry.getValue().get(0);
//            String[] mas = splitCommandString(val);
//            File file1_s = new File(mas[0].replace("\"", ""));
//            File file1 = new File((Neb.home_dir+mas[0]).replace("\"", ""));
//            File file2_s = new File(mas[mas.length-1].replace("\"", ""));
//            File file2 = new File((Neb.home_dir+mas[mas.length-1]).replace("\"", ""));
//            if(!file1_s.exists() && !file1.exists()) {
//                if(!(mas[0].indexOf("/") >= 0 || mas[0].indexOf("\\") >= 0) && !Find_Path_Env(mas[0])) {
//                    System.out.println("Not find java interpreter scripts->get_info_node->"+key+" in cfg file.");
//                    System.exit(1);  
//                }
//            }
//            if(!file2_s.exists() && !file2.exists()) {
//                System.out.println("Not find java program scripts->get_info_node->"+key+" in cfg file.");
//                System.exit(1);            
//            }
//            
//        }
//    }
//    private boolean Find_Path_Env(String prog) {
//        String path = System.getenv("PATH");
//        String[] mas = path.split("\\;");
//        for(String s : mas) {
//            File file = new File(s+"/"+prog);
//            if(file.isFile()) return true;
//            file = new File(s+"/"+prog+".bat");
//            if(file.isFile()) return true;
//            file = new File(s+"/"+prog+".com");
//            if(file.isFile()) return true;
//            file = new File(s+"/"+prog+".exe");
//            if(file.isFile()) return true;            
//        }
//        return false;
//    }
//    public Map Read_NebMapFile(String filename) {
//        Map result = new HashMap<>();
//        JSONParser parser = new JSONParser();
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(filename));
//            
//            char[] buff = new char[65535];
////            char[] buff = new char[1024];
//            int size;
//            StringBuilder out = new StringBuilder();
//            while ((size=reader.read(buff))>=0) {
//                out.append(String.valueOf(buff, 0, size));
//            }
//            reader.close();
//            
//            String[] mas = out.toString().split("\n*#+ .+ #+\n");
//            for(String str : mas) {
//                if(!str.equals("")) {
////                    System.out.println(str);
//                    JSONObject jsonObject = (JSONObject)parser.parse(str);
//                    Map map_tmp = toMap(jsonObject);
//                    result.putAll(map_tmp);
//                }
//            }
//        } catch (Exception ex) {
//            System.out.println(Utils.class.getName() + " - " + ex);
//        }        
//        
//        return result;
//    }    

    public Map readNebMapFile(String map_file) {
        Map<String, Map> INFO = readJSONFile(map_file);
        for (Map.Entry<String, Map> entry : INFO.entrySet()) {
//            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, ArrayList> node_protocol_accounts = val.get("node_protocol_accounts");
            if (node_protocol_accounts != null && !node_protocol_accounts.isEmpty()) {
                Map<String, String[]> node_protocol_accounts_new = new HashMap();
                for (Map.Entry<String, ArrayList> entry1 : node_protocol_accounts.entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<String> item = entry1.getValue();
                    String[] mas = new String[3];
                    mas[0] = item.get(0);
                    mas[1] = item.get(1);
                    mas[2] = item.get(2);
                    node_protocol_accounts_new.put(node, mas);
                }
                val.put("node_protocol_accounts", node_protocol_accounts_new);
            }
        }
        return INFO;
    }

    public Map readJSONFile(String filename) {
        Map result = new HashMap<>();
        File file = new File(filename);
        if (file.exists()) {
            if (filename.equals(Neb.neb_cfg)) {
                result = readConfig(filename);
            } else {
                JSONParser parser = new JSONParser();
                FileReader fr = null;
                try {
                    /* Get the file content into the JSONObject */
                    fr = new FileReader(filename);
                    JSONObject jsonObject = (JSONObject) parser.parse(fr);
                    fr.close();
                    result = toMap(jsonObject);
                } catch (IOException | ParseException ex) {
                    if (fr != null) try {
                        fr.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    System.out.println(Utils.class.getName() + " - " + ex);
                }
            }
        }

        return result;
    }

    public ArrayList readJSONFileToList(String filename) {
        ArrayList result = new ArrayList();
        File file = new File(filename);
        if (file.exists()) {
            JSONParser parser = new JSONParser();
            FileReader fr = null;
            try {
                /* Get the file content into the JSONObject */
                fr = new FileReader(filename);
                JSONArray jsonObject = (JSONArray) parser.parse(fr);
                fr.close();
                result = (ArrayList) toList(jsonObject);
            } catch (IOException | ParseException ex) {
                if (fr != null) try {
                    fr.close();
                } catch (IOException ex1) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex1);
                }
                System.out.println(Utils.class.getName() + " - " + ex);
            }
        }

        return result;
    }

    public Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<>();

        for (String key : (Iterable<String>) object.keySet()) {
            Object value = object.get(key);
            if (value != null) {
                switch (value) {
                    case JSONArray jSONArray -> value = toList(jSONArray);
                    case JSONObject jSONObject -> value = toMap(jSONObject);
                    default -> {
                    }
                }
                map.put(key, value);
            }
        }
        return map;
    }

    private List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList();
        for (Object value : array) {
            if (value != null) {
                switch (value) {
                    case JSONArray jSONArray -> value = toList(jSONArray);
                    case JSONObject jSONObject -> value = toMap(jSONObject);
                    default -> {
                    }
                }
                list.add(value);
            }
        }
        return list;
    }

    private ArrayList rescanSNMPVersion(ArrayList<String[]> node_community_version, int timeout, int retries) {
        ArrayList<String[]> result = new ArrayList();

        PrintStream oldError = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));

        ArrayList<String[]> node_community_version_oid = new ArrayList();
        if (!node_community_version.isEmpty()) {
            for (String[] it : node_community_version) {
                String[] mas = new String[4];
                mas[0] = it[0];
                mas[1] = it[1];
                mas[2] = it[2];
                mas[3] = "1.3.6.1.2.1.31.1.1.1.6";
                node_community_version_oid.add(mas);
            }
        }

        WalkPool walkPool = new WalkPool();
        Map<String, ArrayList<String[]>> res1 = walkPool.get(node_community_version_oid, Neb.timeout_thread, timeout, retries);

        ArrayList<String[]> node_community_version_oid1 = new ArrayList();
        for (String[] iter : node_community_version_oid) {
            if (res1.get(iter[0]) != null) {
                String[] mas = new String[3];
                mas[0] = iter[0];
                mas[1] = iter[1];
                mas[2] = iter[2];
                result.add(mas);
            } else {
                String[] mas = new String[4];
                mas[0] = iter[0];
                mas[1] = iter[1];
                mas[2] = iter[2];
                mas[3] = "1.3.6.1.2.1.2.2.1.10";
                node_community_version_oid1.add(mas);
            }
        }
        if (!node_community_version_oid1.isEmpty()) {
            Map<String, ArrayList<String[]>> res2 = walkPool.get(node_community_version_oid1, Neb.timeout_thread, timeout, retries);
            for (String[] iter : node_community_version_oid1) {
                if (res2.get(iter[0]) != null) {
                    String[] mas = new String[3];
                    mas[0] = iter[0];
                    mas[1] = iter[1];
                    mas[2] = "1";
                    result.add(mas);
                }
            }
        }

        // get sysDescription information 
        GetPool getPool = new GetPool();
        ArrayList<String> oid_list = new ArrayList();
        String sysDescr = ".1.3.6.1.2.1.1.1.0";
        oid_list.add(sysDescr);
        Map<String, ArrayList<String[]>> res = getPool.get(result, oid_list, Neb.timeout_thread, timeout, retries);

        // adding sysDescr to node_commenity_version
        ArrayList<String[]> result1 = new ArrayList();
        for (String[] it : result) {
            if (res.get(it[0]) != null && res.get(it[0]).size() == 1) {
                String[] mas = new String[4];
                mas[0] = it[0];
                mas[1] = it[1];
                mas[2] = it[2];
                mas[3] = res.get(it[0]).get(0)[1];
                result1.add(mas);
            }
        }
        System.setErr(oldError);
        return result1;
    }

    public ArrayList setUniqueList(ArrayList<String[]> list, int num_field) {
        for (int i = 0; i < list.size(); i++) {
            String node1 = list.get(i)[num_field];
            if (!node1.isEmpty()) {
                for (int j = i + 1; j < list.size(); j++) {
                    String node2 = list.get(j)[num_field];
                    if (!node2.isEmpty()) {
                        if (node1.equals(node2)) {
                            String[] mas = new String[3];
                            mas[0] = "";
                            mas[1] = "";
                            mas[2] = "";
                            list.set(j, mas);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[0].isEmpty()) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

//        //  output: node ---> id_iface ---> name_iface,in,out
//    public Map<String, Map<String, String[]>> GetTestCountersFromNodes(String filename) {
//        Map<String, Map<String, String[]>> result = new HashMap<>();
//
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(filename));
//            try {
//                String s;
//                ArrayList<String[]> list = new ArrayList();
//                while ((s = in.readLine()) != null) {
//                    String[] buf = s.split(",");
//                    list.add(buf);
//                }
//                
//                for(int i=0; i<list.size(); i++) {
//                   String[] item = list.get(i);
//                   Map<String, String[]> tmp = new HashMap<>();
//                   String[] mas = new String[3];
//                   mas[0]=item[2]; mas[1]=item[3]; mas[2]=item[4];
//                   tmp.put(item[1], mas);
//                   
//                   for(int j=i+1; j<list.size(); j++) { 
//                       String[] item1 = list.get(j);
//                       if(item[0].equals(item1[0])) {
//                           String[] mas1 = new String[3];
//                           mas1[0]=item1[2]; mas1[1]=item1[3]; mas1[2]=item1[4];
//                           tmp.put(item1[1], mas1);
//                           list.remove(j);
//                           j--;
//                       }
//                       
//                   }
//                   result.put(item[0], tmp);
//                }
//                
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        
//        
//        return result;
//    }

    private Map<String, Map<String, Map<String, String>>> indexingInterfacesFromNodes(Map<String, Map<String, ArrayList>> interfacesFromNodes) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap();

        Map<String, Boolean> node_IfNameExtended = new HashMap();
        for (Map.Entry<String, ArrayList> entry1 : interfacesFromNodes.get("ifDescr").entrySet()) {
            String node = entry1.getKey();
            ArrayList<String[]> val = entry1.getValue();
            boolean interface_name_extended = false;
            int i = 0;
            for (String[] mas : val) {
                boolean found = false;
                for (int j = i + 1; j < val.size(); j++) {
                    String[] mas1 = val.get(j);
                    if (mas1[1].equals(mas[1])) {
                        found = true;
                        interface_name_extended = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
                i++;
            }
            node_IfNameExtended.put(node, interface_name_extended);
        }

        Map<String, Map<String, String>> node_id_ifDescr = new HashMap();
        for (Map.Entry<String, ArrayList> entry1 : interfacesFromNodes.get("ifDescr").entrySet()) {
            String node = entry1.getKey();
            ArrayList<String[]> val = entry1.getValue();
            Map<String, String> map_tmp = new HashMap();
            for (String[] item : val) {
                String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                map_tmp.put(id, item[1]);
            }
            node_id_ifDescr.put(node, map_tmp);
        }
        result.put("ifDescr", node_id_ifDescr);

//        Map<String, Map<String, String>> node_id_IfNameExtendedIfName = new HashMap();
        for (Map.Entry<String, ArrayList> entry1 : interfacesFromNodes.get("IfNameExtendedIfName").entrySet()) {
            String node = entry1.getKey();
            ArrayList<String[]> val = entry1.getValue();
            if (node_IfNameExtended.get(node) != null && node_IfNameExtended.get(node)) {
                Map<String, String> map_tmp = new HashMap();
                for (String[] item : val) {
                    String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    map_tmp.put(id, item[1]);
                }
                node_id_ifDescr.put(node, map_tmp);
            }
        }

        Map<String, Map<String, String>> node_id_IfaceMaping = new HashMap();
        for (Map.Entry<String, ArrayList> entry1 : interfacesFromNodes.get("IfaceMaping").entrySet()) {
            String node = entry1.getKey();
            ArrayList<String[]> val = entry1.getValue();
            Map<String, String> map_tmp = new HashMap();
            for (String[] item : val) {
                String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                map_tmp.put(id, item[1]);
            }
            node_id_IfaceMaping.put(node, map_tmp);
        }
        result.put("IfaceMaping", node_id_IfaceMaping);
        return result;
    }

    //  output: node ---> iface_name ---> iface_id
    private Map<String, Map<String, String>> getNode_Ifaceid_Ifacename(ArrayList<String[]> node_community_version) {
        Map<String, Map<String, String>> result = new HashMap<>();

        logger.Println("\tStart getWalkInterfacesFromNodes ...", logger.DEBUG);
        Map<String, Map<String, ArrayList>> interfacesFromNodes = getWalkInterfacesFromNodes(node_community_version);
        logger.Println("\tStop getWalkInterfacesFromNodes.", logger.DEBUG);
        logger.Println("\tStart indexingInterfacesFromNodes ...", logger.DEBUG);
        Map<String, Map<String, Map<String, String>>> indexingInterfacesFromNodes = indexingInterfacesFromNodes(interfacesFromNodes);
        logger.Println("\tStop indexingInterfacesFromNodes.", logger.DEBUG);

        for (Map.Entry<String, ArrayList> entry : interfacesFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> map_tmp = new HashMap();
            for (String[] val : val_list) {
                String id_iface = val[1];
                String[] res = getIfaceName(node, id_iface, indexingInterfacesFromNodes, false);
                if (!res[1].isEmpty()) {
                    map_tmp.put(res[0], res[1]);
                }
            }
            result.put(node, map_tmp);
        }
        return result;
    }

    //  output: node ---> id_iface ---> name_iface,in,out
    public Map<String, Map<String, String[]>> getCountersFromNodes(Map<String, Map> nodes_information, ArrayList<String[]> node_community_version, ArrayList<String[]> dp_links) {
        Map<String, Map<String, String>> node_listiface_from_dplinks = new HashMap<>();
        for (String[] item : dp_links) {
            String node = item[0];
            String iface_id = item[1];
            String iface = item[2];
            if (!iface_id.isEmpty()) {
                if (node_listiface_from_dplinks.get(node) != null) {
                    node_listiface_from_dplinks.get(node).put(iface_id, iface);
                } else {
                    Map<String, String> map_tmp = new HashMap();
                    map_tmp.put(iface_id, iface_id);
                    node_listiface_from_dplinks.put(node, map_tmp);
                }
            }
            node = item[3];
            iface_id = item[4];
            iface = item[5];
            if (!iface_id.isEmpty()) {
                if (node_listiface_from_dplinks.get(node) != null) {
                    node_listiface_from_dplinks.get(node).put(iface_id, iface);
                } else {
                    Map<String, String> map_tmp = new HashMap();
                    map_tmp.put(iface_id, iface_id);
                    node_listiface_from_dplinks.put(node, map_tmp);
                }
            }
        }

        Map<String, Map<String, String[]>> result = new HashMap<>();
        long start_time = System.currentTimeMillis();
        Map<String, Map<String, ArrayList>> getWalkCountersFromNodes = getWalkCountersFromNodes(node_community_version);
        long stop_time = System.currentTimeMillis();
        long delta = (stop_time - start_time) / 1000;
//        System.out.println("WalkCounters time: "+delta);
        logger.Println("WalkCounters time: " + delta, logger.DEBUG);
        //////// arraylist to map /////////////
        Map<String, Map<String, Map<String, String>>> walkCounters = new HashMap();
        for (Map.Entry<String, Map<String, ArrayList>> entry0 : getWalkCountersFromNodes.entrySet()) {
            String name = entry0.getKey();
            Map<String, Map<String, String>> map_tmp1 = new HashMap();
            for (Map.Entry<String, ArrayList> entry : getWalkCountersFromNodes.get(name).entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> map_tmp = new HashMap();
                for (String[] mas : val_list) {
                    String[] mas1 = mas[0].split("\\.");
                    String id = mas1[mas1.length - 1];
                    map_tmp.put(id, mas[1]);
                }
                map_tmp1.put(node, map_tmp);
            }
            walkCounters.put(name, map_tmp1);
        }

        //////////////////////////////////
        for (Map.Entry<String, ArrayList> entry : getWalkCountersFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();

//            boolean iface_type_extended = false;
//            if(nodes_information.get(node) != null && 
//                    nodes_information.get(node).get("general") != null &&
//                    ((Map)nodes_information.get(node).get("general")).get("sysDescription") != null) {
//                String sysDescription = (String)((Map)nodes_information.get(node).get("general")).get("sysDescription");
//                for(String shablon : Neb.ifaces_counters_extended) {
//                    if(sysDescription.matches(".*"+shablon+".*")) {
//                        iface_type_extended = true;
//                        break;
//                    }
//                }
//            }            

            Map<String, String[]> tmp = new HashMap<>();
            if (val_list.size() >= 3) {
                for (String[] val : val_list) {
                    String id_iface = val[1];
                    String iface_name = "";
                    String id_iface_alt = null;
                    if (walkCounters.get("ifDescr") != null && walkCounters.get("ifDescr").get(node) != null) {
                        iface_name = walkCounters.get("ifDescr").get(node).get(id_iface);
                    }
                    if (iface_name == null)
                        iface_name = "";

                    if (walkCounters.get("ifName") != null && walkCounters.get("ifName").get(node) != null) {
                        for (Map.Entry<String, String> entry1 : walkCounters.get("ifName").get(node).entrySet()) {
                            String id = entry1.getKey();
                            String if_name = entry1.getValue();
                            if (iface_name.contains(if_name)) {
//                                if(if_name.equals(iface_name)) {
                                id_iface_alt = id;
                                break;
                            }
                        }
                    }

                    String[] out = new String[4];
                    out[0] = id_iface;
                    if (walkCounters.get("ifOperStatus") != null && walkCounters.get("ifOperStatus").get(node) != null) {
                        String iface_status = walkCounters.get("ifOperStatus").get(node).get(id_iface);
                        if (iface_status != null && iface_status.equals("1")) {
                            if (walkCounters.get("ifType") != null && walkCounters.get("ifType").get(node) != null) {
                                String iface_type = walkCounters.get("ifType").get(node).get(id_iface);
                                if (iface_type == null)
                                    iface_type = "";
                                if (iface_type.equals("6")) {
//                                if (!iface_type.equals("53")) {
                                    out[1] = getInCounters(walkCounters, node, id_iface, id_iface_alt);
                                    out[2] = getOutCounters(walkCounters, node, id_iface, id_iface_alt);
                                }
                            }
                        }
                    }
                    if (!(out[0] == null || out[1] == null || out[2] == null)) {
                        tmp.put(id_iface, out);
                    }
                    out[3] = id_iface_alt;
                }
            }
            if (!tmp.isEmpty()) {
                result.put(node, tmp);
            }
        }

        // filter counters
        Map<String, Map<String, String[]>> result_new = new HashMap();
        for (Map.Entry<String, Map<String, String[]>> entry : result.entrySet()) {
            String node = entry.getKey();
            Map<String, String[]> val = entry.getValue();
            if (node_listiface_from_dplinks.get(node) != null) {
                Map<String, String[]> val_new = new HashMap();
                for (Map.Entry<String, String[]> entry1 : val.entrySet()) {
                    String id_iface = entry1.getKey();
                    String[] val1 = entry1.getValue();
                    if (node_listiface_from_dplinks.get(node).get(id_iface) == null) {
                        val_new.put(id_iface, val1);
                    }
                }
                result_new.put(node, val_new);
            } else {
                result_new.put(node, val);
            }
        }
        result = result_new;

        return result;
    }

    String getInCounters(Map<String, Map<String, Map<String, String>>> walkCounters,
                         String node, String id_iface, String id_iface_alt) {
        String ifInUcastPkts = "0";
        if (walkCounters.get("ifHCInUcastPkts") != null &&
                walkCounters.get("ifHCInUcastPkts").get(node) != null &&
                walkCounters.get("ifHCInUcastPkts").get(node).get(id_iface) != null) {
            ifInUcastPkts = walkCounters.get("ifHCInUcastPkts").get(node).get(id_iface);
        } else {
            if (id_iface_alt != null && walkCounters.get("ifHCInUcastPkts") != null &&
                    walkCounters.get("ifHCInUcastPkts").get(node) != null &&
                    walkCounters.get("ifHCInUcastPkts").get(node).get(id_iface_alt) != null) {
                ifInUcastPkts = walkCounters.get("ifHCInUcastPkts").get(node).get(id_iface_alt);
            } else if (walkCounters.get("ifInUcastPkts") != null &&
                    walkCounters.get("ifInUcastPkts").get(node) != null &&
                    walkCounters.get("ifInUcastPkts").get(node).get(id_iface) != null) {
                ifInUcastPkts = walkCounters.get("ifInUcastPkts").get(node).get(id_iface);
            }
        }
        String ifInMulticastPkts = "0";
        if (walkCounters.get("ifHCInMulticastPkts") != null &&
                walkCounters.get("ifHCInMulticastPkts").get(node) != null &&
                walkCounters.get("ifHCInMulticastPkts").get(node).get(id_iface) != null) {
            ifInMulticastPkts = walkCounters.get("ifHCInMulticastPkts").get(node).get(id_iface);
        } else if (id_iface_alt != null && walkCounters.get("ifHCInMulticastPkts") != null &&
                walkCounters.get("ifHCInMulticastPkts").get(node) != null &&
                walkCounters.get("ifHCInMulticastPkts").get(node).get(id_iface_alt) != null) {
            ifInMulticastPkts = walkCounters.get("ifHCInMulticastPkts").get(node).get(id_iface_alt);
        }
        String ifInBroadcastPkts = "0";
        if (walkCounters.get("ifHCInBroadcastPkts") != null &&
                walkCounters.get("ifHCInBroadcastPkts").get(node) != null &&
                walkCounters.get("ifHCInBroadcastPkts").get(node).get(id_iface) != null) {
            ifInBroadcastPkts = walkCounters.get("ifHCInBroadcastPkts").get(node).get(id_iface);
        } else if (id_iface_alt != null && walkCounters.get("ifHCInBroadcastPkts") != null &&
                walkCounters.get("ifHCInBroadcastPkts").get(node) != null &&
                walkCounters.get("ifHCInBroadcastPkts").get(node).get(id_iface_alt) != null) {
            ifInBroadcastPkts = walkCounters.get("ifHCInBroadcastPkts").get(node).get(id_iface_alt);
        }
        String ifInNUcastPkts = "0";
        if (walkCounters.get("ifInNUcastPkts") != null &&
                walkCounters.get("ifInNUcastPkts").get(node) != null &&
                walkCounters.get("ifInNUcastPkts").get(node).get(id_iface) != null) {
            ifInNUcastPkts = walkCounters.get("ifInNUcastPkts").get(node).get(id_iface);
        }
        String ifInDiscards = "0";
        if (walkCounters.get("ifInDiscards") != null &&
                walkCounters.get("ifInDiscards").get(node) != null &&
                walkCounters.get("ifInDiscards").get(node).get(id_iface) != null) {
            ifInDiscards = walkCounters.get("ifInDiscards").get(node).get(id_iface);
        }
        String ifInErrors = "0";
        if (walkCounters.get("ifInErrors") != null &&
                walkCounters.get("ifInErrors").get(node) != null &&
                walkCounters.get("ifInErrors").get(node).get(id_iface) != null) {
            ifInErrors = walkCounters.get("ifInErrors").get(node).get(id_iface);
        }
        String res = ifInUcastPkts;
        res = res + "," + ifInMulticastPkts;
        res = res + "," + ifInBroadcastPkts;
        res = res + "," + ifInNUcastPkts;
        res = res + "," + ifInDiscards;
        res = res + "," + ifInErrors;
        return res;
    }

    String getOutCounters(Map<String, Map<String, Map<String, String>>> walkCounters,
                          String node, String id_iface, String id_iface_alt) {
        String ifOutUcastPkts = "0";
        if (walkCounters.get("ifHCOutUcastPkts") != null &&
                walkCounters.get("ifHCOutUcastPkts").get(node) != null &&
                walkCounters.get("ifHCOutUcastPkts").get(node).get(id_iface) != null) {
            ifOutUcastPkts = walkCounters.get("ifHCOutUcastPkts").get(node).get(id_iface);
        } else {
            if (id_iface_alt != null && walkCounters.get("ifHCOutUcastPkts") != null &&
                    walkCounters.get("ifHCOutUcastPkts").get(node) != null &&
                    walkCounters.get("ifHCOutUcastPkts").get(node).get(id_iface_alt) != null) {
                ifOutUcastPkts = walkCounters.get("ifHCOutUcastPkts").get(node).get(id_iface_alt);
            } else if (walkCounters.get("ifOutUcastPkts") != null &&
                    walkCounters.get("ifOutUcastPkts").get(node) != null &&
                    walkCounters.get("ifOutUcastPkts").get(node).get(id_iface) != null) {
                ifOutUcastPkts = walkCounters.get("ifOutUcastPkts").get(node).get(id_iface);
            }
        }
        String ifOutMulticastPkts = "0";
        if (walkCounters.get("ifHCOutMulticastPkts") != null &&
                walkCounters.get("ifHCOutMulticastPkts").get(node) != null &&
                walkCounters.get("ifHCOutMulticastPkts").get(node).get(id_iface) != null) {
            ifOutMulticastPkts = walkCounters.get("ifHCOutMulticastPkts").get(node).get(id_iface);
        } else if (id_iface_alt != null && walkCounters.get("ifHCOutMulticastPkts") != null &&
                walkCounters.get("ifHCOutMulticastPkts").get(node) != null &&
                walkCounters.get("ifHCOutMulticastPkts").get(node).get(id_iface_alt) != null) {
            ifOutMulticastPkts = walkCounters.get("ifHCOutMulticastPkts").get(node).get(id_iface_alt);
        }
        String ifOutBroadcastPkts = "0";
        if (walkCounters.get("ifHCOutBroadcastPkts") != null &&
                walkCounters.get("ifHCOutBroadcastPkts").get(node) != null &&
                walkCounters.get("ifHCOutBroadcastPkts").get(node).get(id_iface) != null) {
            ifOutBroadcastPkts = walkCounters.get("ifHCOutBroadcastPkts").get(node).get(id_iface);
        } else if (id_iface_alt != null && walkCounters.get("ifHCOutBroadcastPkts") != null &&
                walkCounters.get("ifHCOutBroadcastPkts").get(node) != null &&
                walkCounters.get("ifHCOutBroadcastPkts").get(node).get(id_iface_alt) != null) {
            ifOutBroadcastPkts = walkCounters.get("ifHCOutBroadcastPkts").get(node).get(id_iface_alt);
        }
        String ifOutNUcastPkts = "0";
        if (walkCounters.get("ifOutNUcastPkts") != null &&
                walkCounters.get("ifOutNUcastPkts").get(node) != null &&
                walkCounters.get("ifOutNUcastPkts").get(node).get(id_iface) != null) {
            ifOutNUcastPkts = walkCounters.get("ifOutNUcastPkts").get(node).get(id_iface);
        }
        String ifOutDiscards = "0";
        if (walkCounters.get("ifOutDiscards") != null &&
                walkCounters.get("ifOutDiscards").get(node) != null &&
                walkCounters.get("ifOutDiscards").get(node).get(id_iface) != null) {
            ifOutDiscards = walkCounters.get("ifOutDiscards").get(node).get(id_iface);
        }
        String ifOutErrors = "0";
        if (walkCounters.get("ifOutErrors") != null &&
                walkCounters.get("ifOutErrors").get(node) != null &&
                walkCounters.get("ifOutErrors").get(node).get(id_iface) != null) {
            ifOutErrors = walkCounters.get("ifOutErrors").get(node).get(id_iface);
        }
        String res = ifOutUcastPkts;
        res = res + "," + ifOutMulticastPkts;
        res = res + "," + ifOutBroadcastPkts;
        res = res + "," + ifOutNUcastPkts;
        res = res + "," + ifOutDiscards;
        res = res + "," + ifOutErrors;
        return res;
    }

    public double delta(double start, double stop) {
        double result = -1L;
//        double MAX_VALUE32 = Math.pow(2, 32);
//        double MAX_VALUE64 = Math.pow(2, 64);

//        if(start == 0 && stop == 0) {
//            double rnd = Math.random()*MAX_VALUE64;
////            System.out.println("start="+start+" - stop="+stop+" --- result="+rnd);
//            return rnd;
//        }
        if (start == 0 && stop == 0) {
            return 0;
        }
        double delta = stop - start;
        if (delta > 0) {
            result = delta;
        } else {
            result = 0;
//            if(rnd_flag)
//                result = Math.random()*MAX_VALUE64;
//            else
//                result = 0;
        }

        return result;
    }

    //  output: LinkedList(ArrayList(node, id_iface, name_iface, in, out))
    public ArrayList<ArrayList> deltaCounters(Map<String, Map<String, String[]>> start_counters, Map<String, Map<String, String[]>> stop_counters, ArrayList<String[]> node_community_version) {
        ArrayList<ArrayList> result = new ArrayList();
        // normalize start_counters
        Map<String, Map<String, ArrayList>> start_counters_new = new HashMap();
        for (Map.Entry<String, Map<String, String[]>> entry : start_counters.entrySet()) {
            String node = entry.getKey();
            Map<String, String[]> val_list = entry.getValue();
            Map<String, ArrayList> val_list_new = new HashMap();
            for (Map.Entry<String, String[]> entry1 : val_list.entrySet()) {
                String id_iface = entry1.getKey();
                String[] val = entry1.getValue();
                ArrayList list_counters = new ArrayList();
                list_counters.add(id_iface);
                String[] mas_in = val[1].split(",");
                double[] in = new double[mas_in.length];
                for (int i = 0; i < mas_in.length; i++) {
                    in[i] = Double.parseDouble(mas_in[i]);
                }
                list_counters.add(in);
                String[] mas_out = val[2].split(",");
                double[] out = new double[mas_out.length];
                for (int i = 0; i < mas_out.length; i++) {
                    out[i] = Double.parseDouble(mas_out[i]);
                }
                list_counters.add(out);
                list_counters.add(val[3]);
                val_list_new.put(id_iface, list_counters);
            }
            start_counters_new.put(node, val_list_new);
        }
        // normalize stop_counters
        Map<String, Map<String, ArrayList>> stop_counters_new = new HashMap();
        for (Map.Entry<String, Map<String, String[]>> entry : stop_counters.entrySet()) {
            String node = entry.getKey();
            Map<String, String[]> val_list = entry.getValue();
            Map<String, ArrayList> val_list_new = new HashMap();
            for (Map.Entry<String, String[]> entry1 : val_list.entrySet()) {
                String id_iface = entry1.getKey();
                String[] val = entry1.getValue();
                ArrayList list_counters = new ArrayList();
                list_counters.add(id_iface);
                String[] mas_in = val[1].split(",");
                double[] in = new double[mas_in.length];
                for (int i = 0; i < mas_in.length; i++) {
                    in[i] = Double.parseDouble(mas_in[i]);
                }
                list_counters.add(in);
                String[] mas_out = val[2].split(",");
                double[] out = new double[mas_out.length];
                for (int i = 0; i < mas_out.length; i++) {
                    out[i] = Double.parseDouble(mas_out[i]);
                }
                list_counters.add(out);
                list_counters.add(val[3]);
                val_list_new.put(id_iface, list_counters);
            }
            stop_counters_new.put(node, val_list_new);
        }


        for (Map.Entry<String, Map<String, ArrayList>> entry : start_counters_new.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                String id_iface = entry1.getKey();
                ArrayList val1 = entry1.getValue();
                double[] start_in = (double[]) val1.get(1);
                double[] start_out = (double[]) val1.get(2);
                if (stop_counters_new.get(node) != null) {
                    ArrayList val2 = stop_counters_new.get(node).get(id_iface);
                    if (val2 != null) {
                        double[] stop_in = (double[]) val2.get(1);
                        double[] stop_out = (double[]) val2.get(2);
                        double inUcast = delta(start_in[0], stop_in[0]);
                        double inMulticast = delta(start_in[1], stop_in[1]);
                        double inBroadcast = delta(start_in[2], stop_in[2]);
                        double inNUcast = delta(start_in[3], stop_in[3]);
                        double in_discards = delta(start_in[4], stop_in[4]);
                        double in_errors = delta(start_in[5], stop_in[5]);
                        double outUcast = delta(start_out[0], stop_out[0]);
                        double outMulticast = delta(start_out[1], stop_out[1]);
                        double outBroadcast = delta(start_out[2], stop_out[2]);
                        double outNUcast = delta(start_out[3], stop_out[3]);
                        double out_discards = delta(start_out[4], stop_out[4]);
                        double out_errors = delta(start_out[5], stop_out[5]);

//                        System.out.println(node+" - "+id_iface+":"+inUcast+","+inMulticast+","+inBroadcast+","+inNUcast+","+in_discards+","+in_errors+"|"+outUcast+","+outMulticast+","+outBroadcast+","+outNUcast+","+out_discards+","+out_errors);

                        double in = inUcast;
                        if (inMulticast > 0) {
                            in = in + inMulticast + inBroadcast;
                        } else {
                            in = in + inNUcast;
                        }
                        in = in + in_discards;
                        in = in + in_errors;
                        if (in <= 0)
                            in = Math.abs(Math.random() * Math.pow(2, 64));

                        double in1 = inUcast;
                        if (inMulticast > 0) {
                            in1 = in1 + inMulticast + inBroadcast;
                        } else {
                            in1 = in1 + inNUcast;
                        }
                        if (in1 <= 0)
                            in1 = Math.abs(Math.random() * Math.pow(2, 64));

                        double in2 = inUcast;
                        if (in2 <= 0)
                            in2 = Math.abs(Math.random() * Math.pow(2, 64));

                        double out = outUcast;
                        if (outMulticast > 0) {
                            out = out + outMulticast + outBroadcast;
                        } else {
                            out = out + outNUcast;
                        }
                        out = out - out_discards;
                        out = out - out_errors;
                        if (out <= 0)
                            out = Math.abs(Math.random() * Math.pow(2, 64));
                        double out1 = outUcast;
                        if (outMulticast > 0) {
                            out1 = out1 + outMulticast + outBroadcast;
                        } else {
                            out1 = out1 + outNUcast;
                        }
                        if (out1 <= 0)
                            out1 = Math.abs(Math.random() * Math.pow(2, 64));

                        double out2 = outUcast;
                        if (out2 <= 0)
                            out2 = Math.abs(Math.random() * Math.pow(2, 64));

                        ArrayList tmp = new ArrayList();
                        tmp.add(node);
                        tmp.add(id_iface);
                        tmp.add(val1.get(3));
                        tmp.add(in);
                        tmp.add(out);
                        result.add(tmp);

                        if (in_discards > 0 || in_errors > 0 || out_discards > 0 || out_errors > 0) {
                            ArrayList tmp1 = new ArrayList();
                            tmp1.add(node);
                            tmp1.add(id_iface);
                            tmp1.add(val1.get(3));
                            tmp1.add(in1);
                            tmp1.add(out1);
                            result.add(tmp1);
                        }

                        if (inMulticast > 0 || inBroadcast > 0 || inNUcast > 0 ||
                                outMulticast > 0 || outBroadcast > 0 || outNUcast > 0) {
                            ArrayList tmp2 = new ArrayList();
                            tmp2.add(node);
                            tmp2.add(id_iface);
                            tmp2.add(val1.get(3));
                            tmp2.add(in2);
                            tmp2.add(out2);
                            result.add(tmp2);
                        }
                    }
                }
            }
        }

        return result;
    }

    public Map<String, ArrayList<ArrayList<String>>> getCalculateLinks(Map<String, Map> informationFromNodesAllAreas,
                                                                       Map<String, ArrayList<String[]>> area_node_community_version_dp,
                                                                       int pause, int pause_test, double precession_limit, int retries_testing, int limit_retries_testing) {
        // check needed calculate links
        Map<String, Boolean> next_counters_links_areas = new HashMap();
        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map<String, Map> val = area.getValue();
//            Map<String, Map> nodes_information = val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
            Map<String, Map> node_protocol_accounts = (Map) val.get("node_protocol_accounts");

            if (links != null) {
                Map<String, String> node_in_links = new HashMap();
                for (ArrayList<String> item : links) {
                    if (node_protocol_accounts.get(item.get(0)) != null && node_protocol_accounts.get(item.get(3)) != null) {
                        node_in_links.put(item.get(0), item.get(0));
                        node_in_links.put(item.get(3), item.get(3));
                    }
                }

                boolean find = true;
                for (Map.Entry<String, Map> entry : node_protocol_accounts.entrySet()) {
                    String node = entry.getKey();
                    if (node_in_links.get(node) == null) {
                        find = false;
                        break;
                    }
                }
                if (!find) {
                    next_counters_links_areas.put(area_name, true);
                } else {
                    next_counters_links_areas.put(area_name, false);
                }

            }
        }

        Map<String, Map<String, Map<String, String[]>>> start_counters = new HashMap();
        Map<String, Map<String, Map<String, String[]>>> stop_counters = new HashMap();
        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            if (next_counters_links_areas.get(area_name) != null && next_counters_links_areas.get(area_name)) {
                Map<String, Map> nodes_information = (Map) val.get("nodes_information");
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (node_community_version_area != null && node_community_version_area.size() > 1) {
                    logger.Println("Start get counters area " + area_name + " ...", logger.INFO);
                    ArrayList<ArrayList<String>> links_tmp = (ArrayList) val.get("links");
                    ArrayList<String[]> links = new ArrayList();
                    if (links_tmp != null) {
                        for (ArrayList<String> it1 : links_tmp) {
                            String[] mas = new String[6];
                            mas[0] = it1.get(0);
                            mas[1] = it1.get(1);
                            mas[2] = it1.get(2);
                            mas[3] = it1.get(3);
                            mas[4] = it1.get(4);
                            mas[5] = it1.get(5);
                            links.add(mas);
                        }
                    }
                    Map<String, Map<String, String[]>> start_counters_area = getCountersFromNodes(nodes_information, node_community_version_area, links);
                    start_counters.put(area_name, start_counters_area);
                    logger.Println("Stop get counters area " + area_name + ".", logger.INFO);
                }
            }
        }
        if (DEBUG) mapToFile(start_counters, Neb.debug_folder + "/start_counters", Neb.DELAY_WRITE_FILE);
///////////////////////////////////////////////
        if (!start_counters.isEmpty()) {
            long start_time = System.currentTimeMillis();
            Map<String, ArrayList<String[]>> area_node_community_version = getAreaNodeCommunityVersion(informationFromNodesAllAreas);

            Neb.area_arp_mac_table = getArpMac(area_node_community_version);
            if (DEBUG)
                utils.mapToFile(Neb.area_arp_mac_table, Neb.debug_folder + "/area_arp_mac_table", Neb.DELAY_WRITE_FILE);
            long stop_time = System.currentTimeMillis();
            long arp_mac_run_time = (stop_time - start_time) / 1000;
            if (pause - arp_mac_run_time > 0) {
                waiting(pause - arp_mac_run_time);
            }
        }
////////////////////////////////////////////////

        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            if (next_counters_links_areas.get(area_name) != null && next_counters_links_areas.get(area_name)) {
                Map<String, Map> nodes_information = (Map) val.get("nodes_information");
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (node_community_version_area != null && node_community_version_area.size() > 1) {
                    logger.Println("Start get counters area " + area_name + " ...", logger.INFO);
                    ArrayList<ArrayList<String>> links_tmp = (ArrayList) val.get("links");
                    ArrayList<String[]> links = new ArrayList();
                    if (links_tmp != null) {
                        for (ArrayList<String> it1 : links_tmp) {
                            String[] mas = new String[6];
                            mas[0] = it1.get(0);
                            mas[1] = it1.get(1);
                            mas[2] = it1.get(2);
                            mas[3] = it1.get(3);
                            mas[4] = it1.get(4);
                            mas[5] = it1.get(5);
                            links.add(mas);
                        }
                    }
                    Map<String, Map<String, String[]>> stop_counters_area = getCountersFromNodes(nodes_information, node_community_version_area, links);
                    stop_counters.put(area_name, stop_counters_area);
                    logger.Println("Stop get counters area " + area_name + ".", logger.INFO);
                }
            }
        }
        if (DEBUG) mapToFile(stop_counters, Neb.debug_folder + "/stop_counters", Neb.DELAY_WRITE_FILE);

        Map<String, ArrayList<ArrayList>> deltaCounters = new HashMap();
        for (Map.Entry<String, Map<String, Map<String, String[]>>> area : start_counters.entrySet()) {
            String area_name = area.getKey();
            Map<String, Map<String, String[]>> val_start = area.getValue();
            ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
            if (stop_counters.get(area_name) != null && node_community_version_area != null && !node_community_version_area.isEmpty()) {
                Map<String, Map<String, String[]>> val_stop = stop_counters.get(area_name);
                ArrayList<ArrayList> deltaCounters_area = deltaCounters(val_start, val_stop, node_community_version_area);
                deltaCounters.put(area_name, deltaCounters_area);
            }
        }
        // write to file deltaCounters
        if (DEBUG) mapToFile(deltaCounters, Neb.debug_folder + "/deltaCounters", Neb.DELAY_WRITE_FILE);

        Map<String, ArrayList<String[]>> links_calculate = new HashMap();
        for (Map.Entry<String, ArrayList<ArrayList>> area : deltaCounters.entrySet()) {
            String area_name = area.getKey();
            ArrayList<ArrayList> deltaCounters_list = area.getValue();
            logger.Println(area_name + " - start CalculateLinks ...", logger.INFO);
            double min_packets = (double) pause / 10;
            ArrayList<String[]> links_area = calculateLinks(deltaCounters_list, precession_limit, min_packets);
            links_calculate.put(area_name, links_area);
            logger.Println(area_name + " - stop CalculateLinks.", logger.INFO);
        }

        // replace "null" to null
        Map<String, ArrayList<String[]>> links_calculate_new = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : links_calculate.entrySet()) {
            String area = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            ArrayList<String[]> val_new = new ArrayList();
            for (String[] it : val) {
                String[] it_new = new String[it.length];
                int i = 0;
                for (String it1 : it) {
                    if (it1 != null && it1.equals("null")) {
//                        logger.Println("Replace null str: " + it[0] + ", " + it[1] + ", " + it[2] + " <--->" + it[3] + ", " + it[4] + ", " + it[5] , logger.DEBUG);
                        it1 = null;
                    }
                    it_new[i] = it1;
                    i += 1;
                }
                val_new.add(it_new);
            }
            links_calculate_new.put(area, val_new);
        }
        links_calculate = links_calculate_new;

        // write to file links_calculate
        if (DEBUG) mapToFile(links_calculate, Neb.debug_folder + "/links_calculate", Neb.DELAY_WRITE_FILE);

        // append Base Mac addres from ARP_MAC_Table
        logger.Println("Start append Base Mac addres from ARP_MAC_Table...", logger.DEBUG);
        informationFromNodesAllAreas = utils.appendMacFromArpMacTable(informationFromNodesAllAreas, Neb.area_arp_mac_table);
        logger.Println("Stop append Base Mac addres from ARP_MAC_Table.", logger.DEBUG);
//        if(DEBUG) mapToFile((Map) informationFromNodesAllAreas, Neb.debug_folder+"/Info_append_mac", Neb.DELAY_WRITE_FILE);

        Map<String, ArrayList<ArrayList<String>>> area_links_calculate = new HashMap();
        if (!links_calculate.isEmpty()) {
            Map<String, ArrayList<String[]>>[] testingLinks = new HashMap[retries_testing];
            for (int n = 0; n < retries_testing; n++) {
                testingLinks[n] = new HashMap();
            }

            for (int ii = 0; ii < retries_testing; ii++) {
                Map<String, ArrayList<String[]>> testingLinks_start_map = new HashMap();
                for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
                    String area_name = area.getKey();
                    ArrayList<String[]> links_calculate_area = links_calculate.get(area_name);
                    ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                    if (links_calculate_area != null && !links_calculate_area.isEmpty() && node_community_version_area != null && !node_community_version_area.isEmpty()) {
                        Map<String, String[]> node_community_version_map = new HashMap();
                        for (String[] item : node_community_version_area) {
                            node_community_version_map.put(item[0], item);
                        }
                        ArrayList<String[]> links = new ArrayList();
                        for (String[] list : links_calculate_area) {
                            String[] mas = new String[6];
                            mas[0] = list[0];
                            mas[1] = list[1];
                            mas[2] = list[2];
                            mas[3] = list[3];
                            mas[4] = list[4];
                            mas[5] = list[5];
                            //                        logger.Println("Prepare link for testing = " + mas[0] + ", " + mas[1] + ", " + mas[2] + " <--->" + mas[3] + ", " + mas[4] + ", " + mas[5], logger.DEBUG);
                            links.add(mas);
                        }
                        //                    logger.Println("Prepare link for testing size = " + links.size(), logger.DEBUG);

                        logger.Println("Start get counters for testing links area " + area_name + " size=" + links.size() + " ...", logger.INFO);
                        ArrayList<String[]> testingLinks_start = getCountersTestingLinks(links, node_community_version_map);
                        testingLinks_start_map.put(area_name, testingLinks_start);
                        logger.Println("Stop get counters for testing links area " + area_name + ".", logger.INFO);
                        writeArrayListToFile(Neb.debug_folder + "/" + "testingLinks_start_" + area_name + "_" + ii, testingLinks_start);
                    }
                }

                waiting(pause_test);

                Map<String, ArrayList<String[]>> testingLinks_stop_map = new HashMap();
                for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
                    String area_name = area.getKey();
                    ArrayList<String[]> links_calculate_area = links_calculate.get(area_name);
                    ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                    if (links_calculate_area != null && !links_calculate_area.isEmpty() && node_community_version_area != null && !node_community_version_area.isEmpty()) {
                        Map<String, String[]> node_community_version_map = new HashMap();
                        for (String[] item : node_community_version_area) {
                            node_community_version_map.put(item[0], item);
                        }
                        ArrayList<String[]> links = new ArrayList();
                        for (String[] list : links_calculate_area) {
                            String[] mas = new String[6];
                            mas[0] = list[0];
                            mas[1] = list[1];
                            mas[2] = list[2];
                            mas[3] = list[3];
                            mas[4] = list[4];
                            mas[5] = list[5];
                            links.add(mas);
                        }
                        //                    logger.Println("Prepare link for testing size = " + links.size(), logger.DEBUG);

                        logger.Println("Start get counters for testing links area " + area_name + " size=" + links.size() + " ...", logger.INFO);
                        ArrayList<String[]> testingLinks_stop = getCountersTestingLinks(links, node_community_version_map);
                        testingLinks_stop_map.put(area_name, testingLinks_stop);
                        logger.Println("Stop get counters for testing links area " + area_name + ".", logger.INFO);
                        writeArrayListToFile(Neb.debug_folder + "/" + "testingLinks_stop_" + area_name + "_" + ii, testingLinks_stop);
                    }
                }

                Map<String, ArrayList<String[]>> calculateTestingLinks = new HashMap();
                for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
                    String area_name = area.getKey();
                    ArrayList<String[]> testingLinks_start = testingLinks_start_map.get(area_name);
                    ArrayList<String[]> testingLinks_stop = testingLinks_stop_map.get(area_name);
//                    ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                    if (testingLinks_start != null
                            && testingLinks_stop != null
                            && !testingLinks_start.isEmpty()
                            && !testingLinks_stop.isEmpty()) {
                        logger.Println("Start calculate for testing links area " + area_name + " ...", logger.INFO);
                        ArrayList<String[]> calculateTestingLinks_area = calculateTestingLinks(testingLinks_start, testingLinks_stop, precession_limit);
                        logger.Println("Stop calculate for testing links area " + area_name + ".", logger.INFO);
                        calculateTestingLinks.put(area_name, calculateTestingLinks_area);
                    }
                }
                testingLinks[ii] = calculateTestingLinks;
            }
            ///////////////////////////////////////
            Map<String, Map<String, String[]>> area_links_tmp = new HashMap();
            for (int ii = 0; ii < retries_testing; ii++) {
                for (Map.Entry<String, ArrayList<String[]>> entry : testingLinks[ii].entrySet()) {
                    String area_name = entry.getKey();
                    if (area_links_tmp.get(area_name) == null) {
                        Map<String, String[]> links_map = new HashMap();
                        area_links_tmp.put(area_name, links_map);
                    }
                    ArrayList<String[]> val = entry.getValue();
                    for (String[] link : val) {
                        String key = link[0] + " " + link[1] + " " + link[3] + " " + link[4];
                        if (area_links_tmp.get(area_name).get(key) != null) {
                            String[] link1 = area_links_tmp.get(area_name).get(key);
                            link1[6] = String.valueOf(Double.parseDouble(link1[6]) + Double.parseDouble(link[6]));
                            link1[8] = String.valueOf(Integer.parseInt(link1[8]) + 1);
                        } else {
                            String[] mas = new String[9];
                            mas[0] = link[0];
                            mas[1] = link[1];
                            mas[2] = link[2];
                            mas[3] = link[3];
                            mas[4] = link[4];
                            mas[5] = link[5];
                            mas[6] = link[6];
                            mas[7] = link[7];
                            mas[8] = "1";
                            area_links_tmp.get(area_name).put(key, mas);
                        }
                    }

                }
            }

            Map<String, ArrayList<String[]>> area_calc_links = new HashMap();
            for (Map.Entry<String, Map<String, String[]>> entry : area_links_tmp.entrySet()) {
                String area_name = entry.getKey();
                Map<String, String[]> val = entry.getValue();
                ArrayList<String[]> links = new ArrayList();
                for (Map.Entry<String, String[]> entry1 : val.entrySet()) {
                    links.add(entry1.getValue());
                }
                if (!links.isEmpty())
                    area_calc_links.put(area_name, links);
            }

            for (Map.Entry<String, ArrayList<String[]>> entry : area_calc_links.entrySet()) {
//                String area_name = entry.getKey();
                ArrayList<String[]> val = entry.getValue();
                for (String[] link : val) {
                    link[6] = String.valueOf(Double.parseDouble(link[6]) / Integer.parseInt(link[8]));
                }
            }

            Map<String, ArrayList<String[]>> links_from_counters = new HashMap();
            for (Map.Entry<String, ArrayList<String[]>> entry : area_calc_links.entrySet()) {
                String area_name = entry.getKey();
                ArrayList<String[]> val = entry.getValue();
                ArrayList<String[]> list_tmp = new ArrayList();
                for (String[] link : val) {
                    if (link.length == 9) {
                        if (Integer.parseInt(link[8]) >= limit_retries_testing) {
                            String[] mas = new String[9];
                            mas[0] = link[0];
                            mas[1] = link[1];
                            mas[2] = link[2];
                            mas[3] = link[3];
                            mas[4] = link[4];
                            mas[5] = link[5];
                            mas[6] = link[6];
                            mas[7] = link[7];
                            mas[8] = link[8];
                            list_tmp.add(mas);
                        }
                    }

                }
                links_from_counters.put(area_name, list_tmp);
            }
            //////////////////////
            for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
                String area_name = area.getKey();
                //            Map val = area.getValue();
                ArrayList<String[]> links_calculate_area = links_from_counters.get(area_name);
                ArrayList<ArrayList<String>> links_calculate_list = new ArrayList();
                if (links_calculate_area != null) {
                    for (String[] link : links_calculate_area) {
                        ArrayList list = new ArrayList(Arrays.asList(link));
                        links_calculate_list.add(list);
                    }
                    if (!links_calculate_list.isEmpty()) {

                        area_links_calculate.put(area_name, links_calculate_list);
                    }
                }
            }

            for (Map.Entry<String, ArrayList<ArrayList<String>>> area : area_links_calculate.entrySet()) {
                String area_name = area.getKey();
                ArrayList<ArrayList<String>> val = area.getValue();
                for (ArrayList<String> link : val) {
                    StringBuilder str = new StringBuilder(area_name);
                    for (String item : link) {
                        str.append(", ").append(item);
                    }
                    logger.Println("area_links_calculate : " + str, logger.DEBUG);
                }
            }

            // insert iface name
            for (Map.Entry<String, ArrayList<ArrayList<String>>> area : area_links_calculate.entrySet()) {
                String area_name = area.getKey();
                ArrayList<ArrayList<String>> val = area.getValue();
                for (ArrayList<String> link : val) {
                    String node1 = link.get(0);
                    String id1 = link.get(1);
                    if (id1.matches("\\d+") &&
                            Neb.area_node_ifaceid_ifacename.get(area_name) != null &&
                            Neb.area_node_ifaceid_ifacename.get(area_name).get(node1) != null &&
                            Neb.area_node_ifaceid_ifacename.get(area_name).get(node1).get(id1) != null) {
                        String iface_name = Neb.area_node_ifaceid_ifacename.get(area_name).get(node1).get(id1);
                        link.set(2, iface_name);
                    } else {
                        link.set(2, "unknown");
                    }
                    String node2 = link.get(3);
                    String id2 = link.get(4);
                    if (id2.matches("\\d+") &&
                            Neb.area_node_ifaceid_ifacename.get(area_name) != null &&
                            Neb.area_node_ifaceid_ifacename.get(area_name).get(node2) != null &&
                            Neb.area_node_ifaceid_ifacename.get(area_name).get(node2).get(id2) != null) {
                        String iface_name = Neb.area_node_ifaceid_ifacename.get(area_name).get(node2).get(id2);
                        link.set(5, iface_name);
                    } else {
                        link.set(5, "unknown");
                    }
                }
            }
        }

        // write to file links_calculate
        if (DEBUG) mapToFile(area_links_calculate, Neb.debug_folder + "/area_links_calculate", Neb.DELAY_WRITE_FILE);

        // get trusted links
        Map<String, Map<String, ArrayList<String>>> links_calculate_prev_count = utils.readJSONFile(Neb.links_calculate_prev_count_file);
        Map<String, Map<String, ArrayList<String>>> map_links_calculate_prev_count = new HashMap();
        for (Map.Entry<String, Map<String, ArrayList<String>>> entry : links_calculate_prev_count.entrySet()) {
            String area_name = entry.getKey();
            Map<String, ArrayList<String>> val = entry.getValue();
            Map<String, ArrayList<String>> map_links_calculate_prev_count_area = new HashMap();
            for (Map.Entry<String, ArrayList<String>> entry1 : val.entrySet()) {
                String key = entry1.getKey();
                ArrayList<String> link = entry1.getValue();
                map_links_calculate_prev_count_area.put(key, link);
            }
            map_links_calculate_prev_count.put(area_name, map_links_calculate_prev_count_area);
        }

        for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : area_links_calculate.entrySet()) {
            String area_name = entry.getKey();
            ArrayList<ArrayList<String>> links = entry.getValue();
            for (ArrayList<String> link : links) {
                String key1 = link.get(0) + "," + link.get(1) + "," + link.get(2) + "," + link.get(3) + "," + link.get(4) + "," + link.get(5);
                String key2 = link.get(3) + "," + link.get(4) + "," + link.get(5) + "," + link.get(0) + "," + link.get(1) + "," + link.get(2);
                if (map_links_calculate_prev_count.get(area_name) != null &&
                        map_links_calculate_prev_count.get(area_name).get(key1) != null) {
                    int count = Integer.parseInt(map_links_calculate_prev_count.get(area_name).get(key1).get(8));
                    count += 1;
                    map_links_calculate_prev_count.get(area_name).get(key1).set(8, String.valueOf(count));
                } else if (map_links_calculate_prev_count.get(area_name) != null &&
                        map_links_calculate_prev_count.get(area_name).get(key2) != null) {
                    int count = Integer.parseInt(map_links_calculate_prev_count.get(area_name).get(key2).get(8));
                    count += 1;
                    map_links_calculate_prev_count.get(area_name).get(key2).set(8, String.valueOf(count));
                } else {
                    ArrayList<String> links1 = (ArrayList) link.clone();
                    links1.set(8, "1");
                    if (map_links_calculate_prev_count.get(area_name) != null) {
                        map_links_calculate_prev_count.get(area_name).put(key1, links1);
                    } else {
                        Map<String, ArrayList<String>> map_links_calculate_prev_count_area = new HashMap();
                        map_links_calculate_prev_count_area.put(key1, links1);
                        map_links_calculate_prev_count.put(area_name, map_links_calculate_prev_count_area);
                    }
                }
            }
        }

        // remove link 
        Map<String, Map<String, ArrayList<String>>> map_links_calculate = new HashMap();
        for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : area_links_calculate.entrySet()) {
            String area_name = entry.getKey();
            ArrayList<ArrayList<String>> links = entry.getValue();
            Map<String, ArrayList<String>> map_links_calculate_area = new HashMap();
            for (ArrayList<String> link : links) {
                String key1 = link.get(0) + "," + link.get(1) + "," + link.get(2) + "," + link.get(3) + "," + link.get(4) + "," + link.get(5);
                String key2 = link.get(3) + "," + link.get(4) + "," + link.get(5) + "," + link.get(0) + "," + link.get(1) + "," + link.get(2);
                map_links_calculate_area.put(key1, link);
                map_links_calculate_area.put(key2, link);
            }
            map_links_calculate.put(area_name, map_links_calculate_area);
        }

        for (Map.Entry<String, Map<String, ArrayList<String>>> entry : map_links_calculate_prev_count.entrySet()) {
            String area_name = entry.getKey();
            Map<String, ArrayList<String>> val = entry.getValue();
            Map<String, ArrayList<String>> val_new = new HashMap();
            for (Map.Entry<String, ArrayList<String>> entry1 : val.entrySet()) {
                String key = entry1.getKey();
                ArrayList<String> link = entry1.getValue();
                if (map_links_calculate.get(area_name) != null && map_links_calculate.get(area_name).get(key) != null) {
                    val_new.put(key, link);
                }
            }
            map_links_calculate_prev_count.put(area_name, val_new);
        }

        utils.mapToFile(map_links_calculate_prev_count, Neb.links_calculate_prev_count_file, Neb.DELAY_WRITE_FILE);

        Map<String, Map<String, String>> trust_calculate_links = new HashMap();
        for (Map.Entry<String, Map<String, ArrayList<String>>> entry : map_links_calculate_prev_count.entrySet()) {
            String area_name = entry.getKey();
            Map<String, ArrayList<String>> val = entry.getValue();
            Map<String, String> trust_calculate_links_area = new HashMap();
            for (Map.Entry<String, ArrayList<String>> entry1 : val.entrySet()) {
                String key = entry1.getKey();
                ArrayList<String> link = entry1.getValue();
                if (Integer.parseInt(link.get(8)) >= Neb.links_calculate_prev_history) {
                    trust_calculate_links_area.put(key, key);
                }
            }
            if (!trust_calculate_links_area.isEmpty())
                trust_calculate_links.put(area_name, trust_calculate_links_area);
        }

        // arp mac checking calculate links
        Map<String, Map> area_node_mac = utils.getAreaNodeMac(informationFromNodesAllAreas);
        logger.Println("Start get area_node_mac_connected_neighbours_mac ...", logger.DEBUG);
        Map<String, Map> topology = utils.getTopology(informationFromNodesAllAreas);
        Map<String, Map> area_node_mac_connected_neighbours_mac = getConnectedNeighboursMac(topology, Neb.area_arp_mac_table);
        logger.Println("Stop get area_node_mac_connected_neighbours_mac.", logger.DEBUG);

        for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : area_links_calculate.entrySet()) {
            String area_name = entry.getKey();
            Map<String, Map> node_mac_connected_neighbours = area_node_mac_connected_neighbours_mac.get(area_name);
            if (area_node_mac.get(area_name) != null && node_mac_connected_neighbours != null) {
                ArrayList<ArrayList<String>> links = entry.getValue();
//                // get concurent links
//                Map<String, ArrayList<ArrayList<String>>> concurent_links = new HashMap();
//                for(ArrayList<String> link : links) {
//                    String key1 = link.get(0)+","+link.get(1)+","+link.get(2);
//                    String key2 = link.get(3)+","+link.get(4)+","+link.get(5);
//                    if(concurent_links.get(key1) != null) {
//                        concurent_links.get(key1).add(link);
//                    } else {
//                        ArrayList<ArrayList<String>> tmp_list = new ArrayList();
//                        tmp_list.add(link);
//                        concurent_links.put(key1, tmp_list);
//                    }
//                    if(concurent_links.get(key2) != null) {
//                        concurent_links.get(key2).add(link);
//                    } else {
//                        ArrayList<ArrayList<String>> tmp_list = new ArrayList();
//                        tmp_list.add(link);
//                        concurent_links.put(key2, tmp_list);
//                    }                
//                }
//                
//                // checking arp mac for concurent links
//                Map<String, ArrayList<ArrayList<String>>> concurent_links_new = new HashMap();
//                for (Map.Entry<String, ArrayList<ArrayList<String>>> entry1 : concurent_links.entrySet()) {
//                    String key = entry1.getKey();
//                    boolean find_arp_mac = false;
//                    for(ArrayList<String> link : entry1.getValue()) {
//                        if(check_Link_through_node_mac_connected_neighbours_mac(link, node_mac_connected_neighbours, area_node_mac.get(area_name))) {
//                            find_arp_mac = true;
//                            break;
//                        }                    
//                    }
//                    if(find_arp_mac)
//                        concurent_links_new.put(key, entry1.getValue());
//                }
//                concurent_links = concurent_links_new;

                ArrayList<ArrayList<String>> links_new = new ArrayList();
                for (ArrayList<String> link : links) {
//                    String key1 = link.get(0)+","+link.get(1)+","+link.get(2);
//                    String key2 = link.get(3)+","+link.get(4)+","+link.get(5);
                    String key1_full = link.get(0) + "," + link.get(1) + "," + link.get(2) + "," + link.get(3) + "," + link.get(4) + "," + link.get(5);
                    String key2_full = link.get(3) + "," + link.get(4) + "," + link.get(5) + "," + link.get(0) + "," + link.get(1) + "," + link.get(2);
                    if (
                            (trust_calculate_links.get(area_name) != null &&
                                    trust_calculate_links.get(area_name).get(key1_full) != null) ||
                                    (trust_calculate_links.get(area_name) != null &&
                                            trust_calculate_links.get(area_name).get(key2_full) != null)
                    ) {
                        logger.Println(link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + " OK.", logger.DEBUG);
                        links_new.add(link);
                    } else if (check_Link_through_node_mac_connected_neighbours_mac(link, node_mac_connected_neighbours, area_node_mac.get(area_name))) {
                        logger.Println(link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + " OK.", logger.DEBUG);
                        links_new.add(link);
                    } else {
                        logger.Println(link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + " ERR!!!", logger.DEBUG);
                    }
                }
                area_links_calculate.put(area_name, links_new);
            }
        }
        mapToFile(area_links_calculate, Neb.debug_folder + "/area_links_calculate_check_arp_mac", Neb.DELAY_WRITE_FILE);

        // get uniqal calculate links
        Map<String, ArrayList<ArrayList<String>>> area_links_calculate_uniqal = new HashMap();
        for (Map.Entry<String, ArrayList<ArrayList<String>>> area : area_links_calculate.entrySet()) {
            String area_name = area.getKey();
//            System.out.println(area_name);
            ArrayList<ArrayList<String>> val = area.getValue();
            // sorting for precession
            val.sort((ArrayList<String> o1, ArrayList<String> o2) -> {
                if (Double.parseDouble(o1.get(6)) == Double.parseDouble(o2.get(6)))
                    return 0;
                return Double.parseDouble(o1.get(6)) < Double.parseDouble(o2.get(6)) ? -1 : 1;
            });
            ArrayList<ArrayList<String>> links_calculate_uniqal = new ArrayList();
            for (ArrayList<String> link : val) {
                boolean found = false;
                for (ArrayList<String> link_uniq : links_calculate_uniqal) {
                    if (link.get(0).equals(link_uniq.get(0)) && link.get(1).equals(link_uniq.get(1)) ||
                            link.get(3).equals(link_uniq.get(3)) && link.get(4).equals(link_uniq.get(4)) ||
                            link.get(0).equals(link_uniq.get(3)) && link.get(1).equals(link_uniq.get(4)) ||
                            link.get(3).equals(link_uniq.get(0)) && link.get(4).equals(link_uniq.get(1))
                    ) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    links_calculate_uniqal.add(link);
                    logger.Println("Add link: " + link.get(0) + ", " + link.get(1) + " <---> " + link.get(3) + ", " + link.get(4), logger.DEBUG);
                } else {
                    logger.Println("Delete link: " + link.get(0) + ", " + link.get(1) + " <---> " + link.get(3) + ", " + link.get(4), logger.DEBUG);
                }
            }

            if (!links_calculate_uniqal.isEmpty())
                area_links_calculate_uniqal.put(area_name, links_calculate_uniqal);

        }


        // write to file links_calculate
        if (DEBUG)
            mapToFile(area_links_calculate_uniqal, Neb.debug_folder + "/area_links_calculate_uniqal", Neb.DELAY_WRITE_FILE);


        return area_links_calculate_uniqal;
    }

    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))     pause in seconds
    public ArrayList<String[]> calculateLinks(ArrayList<ArrayList> deltaCounters, double precession_limit, double min_packets) {
        ArrayList<String[]> result = new ArrayList();

        RTree<String, Geometry> tree = RTree.create();
        for (ArrayList it : deltaCounters) {
            String item = it.get(0) + ";" + it.get(1) + ";" + it.get(2);
            if ((Double) it.get(3) > min_packets || (Double) it.get(4) > min_packets) {
                tree = tree.add(item, Geometries.point((Double) it.get(3), (Double) it.get(4)));
            }
        }
        Map<String, String> links_map_tmp = new HashMap();
        for (Entry<String, Geometry> it : tree.entries()) {
            String name = it.value();
            Point gm = (Point) it.geometry();
            double x = gm.x();
            double y = gm.y();
            double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) * precession_limit;
            Iterable<Entry<String, Geometry>> entries = tree.search(Geometries.point(y, x), distance);
            for (Entry<String, Geometry> it1 : entries) {
                String name1 = it1.value();
                Point gm1 = (Point) it1.geometry();
                double x1 = gm1.x();
                double y1 = gm1.y();
                double dist = Math.sqrt(Math.pow(x - y1, 2) + Math.pow(y - x1, 2));
                double proc = 2 * dist / (x + y);
                String[] mas = name.split(";");
                String[] mas1 = name1.split(";");
                String node1 = mas[0];
                String node2 = mas1[0];
                if (!node1.equals(node2)) {
                    if (links_map_tmp.get(name + ";" + name1) == null && links_map_tmp.get(name1 + ";" + name) == null) {
//                        System.out.println(name+" <---> "+name1+" --- "+dist+"/"+proc+"/"+x+","+y+";"+x1+","+y1);
                        links_map_tmp.put(name + ";" + name1, dist + "/" + proc);
                    }
                }
            }
        }
        for (Map.Entry<String, String> entry : links_map_tmp.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            String[] mas = key.split(";");
            result.add(mas);
            logger.Println("Links for testing: " + mas[0] + ", " + mas[1] + ", " + mas[2] + " <---> " + mas[3] + ", " + mas[4] + ", " + mas[5] + " --- " + val, logger.DEBUG);
        }

        return result;
    }

    //    public void TestCalculateLinks() {
//        
//        LinkedList<ArrayList> deltaCounters = new LinkedList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("delta"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    ArrayList tmp = new ArrayList();
//                    
//                    tmp.add(s.split(",")[0]);
//                    tmp.add(s.split(",")[1]);
//                    tmp.add(s.split(",")[2]);
//                    tmp.add(Double.parseDouble(s.split(",")[3]));
//                    tmp.add(Double.parseDouble(s.split(",")[4]));
//                    tmp.add(Double.parseDouble(s.split(",")[3])+Double.parseDouble(s.split(",")[4]));
//                    deltaCounters.add(tmp);
//
//                }
//            } finally {
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }    
//        
//        
//        ArrayList<String[]> result = new ArrayList();
//        double precession_limit = 0.02;
//
//        for(int num=4; num>=0; num--) {
//            double precession_limit_calc=precession_limit/Math.pow(2,num);
//            logger.Println("precession_limit_calc = "+precession_limit_calc, logger.DEBUG);
//            while(deltaCounters.size() >= 2) {
//                int pos1=-1; int pos2=-1;
//                double precession=0; 
//                double min = Math.pow(2, 64);
//                long start_time = System.currentTimeMillis();
//                for (int i = 0; i < deltaCounters.size(); i++) {
//                    ArrayList item1 = deltaCounters.get(i);
//                    for (int j = i + 1; j < deltaCounters.size(); j++) {
//                        ArrayList item2 = deltaCounters.get(j);
//                        double delta_sum = Math.abs((double)item1.get(5)-(double)item2.get(5));
//                        double sum = (double)item1.get(5)+(double)item2.get(5);
//                        if ( !item1.get(0).equals(item2.get(0)) ) {
//                            if (2*delta_sum/sum < precession_limit_calc) {
//                                double delta1=abs((double)item1.get(3)-(double)item2.get(4));
//                                double sum1=(double)item1.get(3)+(double)item2.get(4);
//                                double delta2=abs((double)item1.get(4)-(double)item2.get(3));
//                                double sum2=(double)item1.get(4)+(double)item2.get(3);
//                                double prec1 = 2*delta1/sum1;
//                                double prec2 = 2*delta2/sum2;
//                                double delta = (delta1+delta2)/2;
//                                precession = (prec1+prec2)/2;
//                                if(precession < min) { min=precession; pos1=i; pos2=j; }
//
//                            } else {
////                                if(pos1 == -1 && pos2 == -1) {
////                                    deltaCounters.remove(i);
////                                    System.out.println("Remove delta: "+(String)deltaCounters.get(i).get(0)+","+(String)deltaCounters.get(i).get(1)+","+(String)deltaCounters.get(i).get(2));
////                                    i--;
////                                    j--;
////                                }
//                                break;
//                            }
//                        } 
//
//                    }
//        //            System.out.println(i);
//                }
//                long stop_time = System.currentTimeMillis();
//                System.out.println( (stop_time-start_time)/1000+" sec.  records="+deltaCounters.size());
//
//                if( !(pos1 == -1 || pos2 == -1 || deltaCounters.size() <= 1) ) {
//                    String[] mas = new String[6];
//                    mas[0]=(String)deltaCounters.get(pos1).get(0);
//                    mas[1]=(String)deltaCounters.get(pos1).get(1);
//                    mas[2]=(String)deltaCounters.get(pos1).get(2);
//                    mas[3]=(String)deltaCounters.get(pos2).get(0);
//                    mas[4]=(String)deltaCounters.get(pos2).get(1);
//                    mas[5]=(String)deltaCounters.get(pos2).get(2);
//
//                    double delta1=abs((double)deltaCounters.get(pos1).get(3)-(double)deltaCounters.get(pos2).get(4));
//                    double sum1=(double)deltaCounters.get(pos1).get(3)+(double)deltaCounters.get(pos2).get(4);
//                    double delta2=abs((double)deltaCounters.get(pos1).get(4)-(double)deltaCounters.get(pos2).get(3));
//                    double sum2=(double)deltaCounters.get(pos1).get(4)+(double)deltaCounters.get(pos2).get(3);
//                    double prec1 = 2*delta1/sum1;
//                    double prec2 = 2*delta2/sum2;
//                    double delta = (delta1+delta2)/2;
//                    precession = (prec1+prec2)/2;
//
//                    if(precession < 5*precession_limit) {
////                        System.out.println("Calculate link: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" --- "+precession);
//                        result.add(mas);
//                        deltaCounters.remove(pos1);
//                        deltaCounters.remove(pos2-1);                        
//                    } else break;
//                } else break;
//
//            }
//        }
////        System.out.println("11111111111111");
//    }
    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList>> getWalkCountersFromNodes(ArrayList<String[]> node_community_version) {
        Map<String, String> oids = new HashMap<>();
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
//        oids.put("ifInOctets", "1.3.6.1.2.1.2.2.1.10");
//        oids.put("ifOutOctets", "1.3.6.1.2.1.2.2.1.16");
//        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");
        oids.put("ifName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("ifInUcastPkts", "1.3.6.1.2.1.2.2.1.11");
        oids.put("ifOutUcastPkts", "1.3.6.1.2.1.2.2.1.17");
        oids.put("ifInNUcastPkts", "1.3.6.1.2.1.2.2.1.12");
        oids.put("ifOutNUcastPkts", "1.3.6.1.2.1.2.2.1.18");
        oids.put("ifInDiscards", "1.3.6.1.2.1.2.2.1.13");
        oids.put("ifOutDiscards", "1.3.6.1.2.1.2.2.1.19");
        oids.put("ifInErrors", "1.3.6.1.2.1.2.2.1.14");
        oids.put("ifOutErrors", "1.3.6.1.2.1.2.2.1.20");

        Map<String, String> oidsV2 = new HashMap<>();
//        oidsV2.put("ifHCInOctets", "1.3.6.1.2.1.31.1.1.1.6");
//        oidsV2.put("ifHCOutOctets", "1.3.6.1.2.1.31.1.1.1.10");
        oidsV2.put("ifHCInUcastPkts", "1.3.6.1.2.1.31.1.1.1.7");
        oidsV2.put("ifHCOutUcastPkts", "1.3.6.1.2.1.31.1.1.1.11");
        oidsV2.put("ifHCInMulticastPkts", "1.3.6.1.2.1.31.1.1.1.8");
        oidsV2.put("ifHCOutMulticastPkts", "1.3.6.1.2.1.31.1.1.1.12");
        oidsV2.put("ifHCInBroadcastPkts", "1.3.6.1.2.1.31.1.1.1.9");
        oidsV2.put("ifHCOutBroadcastPkts", "1.3.6.1.2.1.31.1.1.1.13");

        Map<String, Map<String, ArrayList>> result = new HashMap<>();

        PrintStream oldError = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));

        WalkPool walkPool = new WalkPool();
        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool " + val);
//            watch_Telemetry_Lib.start();
            Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version, val, Neb.timeout_thread);
//            watch_Telemetry_Lib.exit = true;
            Map tmp = new HashMap(res);
            result.put(key, tmp);
        }

        ArrayList<String[]> node_community_V2 = new ArrayList();
        for (String[] item : node_community_version) {
            if (item[2].equals("2")) {
                node_community_V2.add(item);
            }
        }
        for (Map.Entry<String, String> entry : oidsV2.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool V2 " + val);
//            watch_Telemetry_Lib.start();
            Map<String, ArrayList<String[]>> res = walkPool.get(node_community_V2, val, Neb.timeout_thread);
//            watch_Telemetry_Lib.exit = true;
            Map tmp = new HashMap(res);
            result.put(key, tmp);
        }
        System.setErr(oldError);
        return result;
    }

    // output: oid_key ---> node ---> ArrayList(String,String) 
//    private Map<String, Map<String, ArrayList>> GetWalkInformationFromNodes(ArrayList<String[]> node_community_version) {
//        Map<String, String> oids = new HashMap<>();
//        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
//        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
//        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
//        oids.put("ifMTU", "1.3.6.1.2.1.2.2.1.4");
//        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");
//        oids.put("ifMAC", "1.3.6.1.2.1.2.2.1.6");
//        oids.put("ifAdminStatus", "1.3.6.1.2.1.2.2.1.7");
//        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
//        oids.put("ifIpAddress", "1.3.6.1.2.1.4.20.1.2");
//        oids.put("ifIpNetMask", "1.3.6.1.2.1.4.20.1.3");
//        oids.put("NetRoute", "1.3.6.1.2.1.4.21.1.1");
//        oids.put("RouteMetric", "1.3.6.1.2.1.4.21.1.3");
//        oids.put("RouteDestination", "1.3.6.1.2.1.4.21.1.7");
//        oids.put("RouteType", "1.3.6.1.2.1.4.21.1.8");
//        oids.put("RouteProto", "1.3.6.1.2.1.4.21.1.9");
//        oids.put("RouteAge", "1.3.6.1.2.1.4.21.1.10");
//        oids.put("RouteMask", "1.3.6.1.2.1.4.21.1.11");
//        oids.put("IdNameVlan", "1.3.6.1.2.1.17.7.1.4.3.1.1");
//        oids.put("IdVlanToNumberInterface", "1.3.6.1.2.1.16.22.1.1.1.1.4.1.3.6.1.2.1.16.22.1.4.1");
//        oids.put("TaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.4");
//        oids.put("UnTaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.5");
//        oids.put("IdNameVlanCisco", "1.3.6.1.4.1.9.9.46.1.3.1.1.4.1");
//        oids.put("IdVlanToNumberInterfaceCisco", "1.3.6.1.4.1.9.9.128.1.1.1.1.3");
//        oids.put("VlanType", "1.3.6.1.4.1.9.9.46.1.6.1.1.3");
//        oids.put("VlanPortAccessModeCisco", "1.3.6.1.4.1.9.9.68.1.2.1.1.2");
//        oids.put("IndexInterfaceCisco", "1.3.6.1.4.1.9.5.1.4.1.1.11");
//        oids.put("IndexInterfaceCiscoSec", "1.3.6.1.4.1.9.9.82.1.5.1.1.2");
//        oids.put("PortTrunkNativeVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.5");
//        oids.put("PortTrunkVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.11");
//        oids.put("vlanTrunkPortDynamicStatus","1.3.6.1.4.1.9.9.46.1.6.1.1.14");
//        oids.put("VlanNameHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.2");
//        oids.put("VlanIdHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.5");
//        oids.put("VlanPortStateHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.8.1.1");
//        oids.put("VlanNameTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.2");
//        oids.put("VlanIdTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.3");
//        oids.put("VlanIdTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.3");
//        oids.put("TagPortsTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.4");
//        oids.put("UnTagPortsTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.5");
//        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
//        oids.put("Duplex_Allied", "1.3.6.1.4.1.207.8.10.3.1.1.5");
//        oids.put("Duplex_Asante", "1.3.6.1.4.1.298.1.5.1.2.6.1.7");
//        oids.put("Duplex_Dell", "1.3.6.1.4.1.89.43.1.1.4");
//        oids.put("Duplex_Foundry", "1.3.6.1.4.1.1991.1.1.3.3.1.1.4");
//        oids.put("Duplex_Cisco2900", "1.3.6.1.4.1.9.9.87.1.4.1.1.32");
//        oids.put("Duplex_HP", "1.3.6.1.2.1.26.2.1.1.3");
//        oids.put("Duplex_Cisco", "1.3.6.1.2.1.10.7.2.1.19");
//        oids.put("IpAddress", "1.3.6.1.2.1.4.20.1.1");
//        oids.put("VlanCommunity", "1.3.6.1.2.1.47.1.2.1.1.4");
//        oids.put("NumSwitchPorts", "1.3.6.1.2.1.17.1.2.0");
//        oids.put("lldpRemManAddrIfId", "1.0.8802.1.1.2.1.4.2.1.4");
//        oids.put("lldpRemPortId", "1.0.8802.1.1.2.1.4.1.1.7");
//        oids.put("lldpRemChassisId","1.0.8802.1.1.2.1.4.1.1.5");
//        oids.put("lldpRemManAddrIfSubtype","1.0.8802.1.1.2.1.4.2.1.3");
//        oids.put("lldpRemSysName","1.0.8802.1.1.2.1.4.1.1.9");
//        oids.put("lldpRemSysDesc", "1.0.8802.1.1.2.1.4.1.1.10");
//        oids.put("ldpLocPortId", "1.0.8802.1.1.2.1.3.7.1.3");
//        oids.put("cdpCacheAddress", "1.3.6.1.4.1.9.9.23.1.2.1.1.4");
//        oids.put("cdpCacheDevicePort", "1.3.6.1.4.1.9.9.23.1.2.1.1.7");
//        oids.put("cdpRemSysName", "1.3.6.1.4.1.9.9.23.1.2.1.1.6");
//        oids.put("cdpRemSysDesc", "1.3.6.1.4.1.9.9.23.1.2.1.1.8");
//        oids.put("vmVlanType", "1.3.6.1.4.1.9.9.68.1.2.2.1.1");
//        oids.put("vmVlan", "1.3.6.1.4.1.9.9.68.1.2.2.1.2");
//        oids.put("vmVlans", "1.3.6.1.4.1.9.9.68.1.2.2.1.4");
//        oids.put("portMembersPortChannel", "1.2.840.10006.300.43.1.2.1.1.12");
//        oids.put("IfaceMaping", "1.3.6.1.2.1.17.1.4.1.2");
//        oids.put("entPhysicalDescr", "1.3.6.1.2.1.47.1.1.1.1.2");
//        oids.put("entPhysicalSerialNumber", "1.3.6.1.2.1.47.1.1.1.1.11");
//
//        Map<String, Map<String, ArrayList>> result = new HashMap<>();
//
//        WalkPool walkPool = new WalkPool();
//        for (Map.Entry<String, String> entry : oids.entrySet()) {
//            String key = entry.getKey();
//            String val = entry.getValue();
//            Map<String, ArrayList> res = walkPool.Get(node_community_version, val);
//            Map tmp = new HashMap(res);
//            result.put(key, tmp);
//        }
//        return result;
//    }     
    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList>> getWalkInterfacesFromNodes(ArrayList<String[]> node_community_version) {
        Map<String, String> oids = new HashMap<>();
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        String IfaceMaping = "1.3.6.1.2.1.17.1.4.1.2";
//        String vlan_community = "1.3.6.1.2.1.47.1.2.1.1.4";

        Map<String, Map<String, ArrayList>> result = new HashMap<>();
        WalkPool walkPool = new WalkPool();

        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
        for (String[] item : node_community_version) {
            String[] mas = new String[4];
            mas[0] = item[0];
            mas[1] = item[1];
            mas[2] = item[2];
            mas[3] = IfaceMaping;
            list_node_community_version_oid.add(mas);
        }

        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool " + val);
//            watch_Telemetry_Lib.start();
            Map<String, ArrayList<String[]>> res1 = walkPool.get(node_community_version, val, Neb.timeout_thread);
//            watch_Telemetry_Lib.exit = true;
            Map tmp = new HashMap(res1);
            result.put(key, tmp);
        }

        int snmp_port = 161;
//        Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool node_community_version_oid_list");
//        watch_Telemetry_Lib.start();       
        Map<String, ArrayList<String[]>> res_iface_maping = walkPool.get(list_node_community_version_oid, Neb.timeout_thread, snmp_port, Neb.timeout_mac, Neb.retries_mac);
//        watch_Telemetry_Lib.exit = true;
        Map<String, ArrayList> res_new = new HashMap();
        if (res_iface_maping != null && !res_iface_maping.isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry1 : res_iface_maping.entrySet()) {
                String key1 = entry1.getKey();
                ArrayList<String[]> val1 = entry1.getValue();
                ArrayList val1_new = new ArrayList(val1);
                res_new.put(key1, val1_new);
            }
        }
        result.put("IfaceMaping", res_new);

        return result;
    }

//    private ArrayList<String> GetMACAddressFromNode(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList> commonInformationFromNodes, String node) {
//        ArrayList<String> result = new ArrayList();
//        
//        ArrayList<String[]> list = walkInformationFromNodes.get("ifMAC").get(node);
//        if(list != null) {
//            if(commonInformationFromNodes.get(node) != null) result.add((String)commonInformationFromNodes.get(node).get(6));
//            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifMAC").get(node);
//            if(list1 != null) {
//                for (String[] item : list1) {
//                    result.add(ReplaceDelimiter(TranslateMAC(item[1])));
//                }
//            } 
//        } 
//        
//        return result;
//    }
//    
//    private Map<String, ArrayList<String>> GetNativeMACAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList> commonInformationFromNodes) {
//        Map<String, ArrayList<String>> result = new HashMap();
//        
//        for(Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIpAddress").entrySet()) {
//            String node = entry.getKey();
//            ArrayList<String> tmp_list = GetMACAddressFromNode(walkInformationFromNodes, commonInformationFromNodes, node);
//            result.put(node, tmp_list);
//        }
//        
//        return result;        
//    }
//    private ArrayList<String> getIpAddressFromNode(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
//        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";
//        ArrayList<String> result = new ArrayList();
//        ArrayList<String[]> list = walkInformationFromNodes.get("ifIpAddress").get(node);
//        if (list != null) {
//            result.add(node);
//            for (String[] item : list) {
//                String[] tmp = item[0].split("\\.");
//                String ip = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
//                String id_iface = item[1];
//                ArrayList<String[]> list1 = walkInformationFromNodes.get("ifOperStatus").get(node);
//                if (list1 != null) {
//                    for (String[] item1 : list1) {
//                        if (item1[0].equals(ifOperStatus + "." + id_iface)) {
//                            if (item1[1].equals("1")) {
//                                if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                                    result.add(ip);
//                                } else {
//                                    logger.Println("node=" + node + " ip=" + ip + " is not up.", logger.DEBUG);
//                                }
//                            }
////                            break;
//                        }
//                    }
//                }
//            }
//        } else {
//            result.add(node);
//        }
//
////        ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
//        ArrayList<String> result_sort = new ArrayList();
//        for (String ip : result) {
//            boolean find = false;
//            for (String network : Neb.networks_for_current_area) {
//                if (Neb.neb_lib_utils.insideInterval(ip, network)) {
//                    result_sort.add(ip);
//                    find = true;
//                    break;
//                }
//            }
//            if (find) {
//                break;
//            }
//        }
//        if (result_sort.size() == 1) {
//            for (String ip : result) {
//                if (!ip.equals(result_sort.get(0))) {
//                    result_sort.add(ip);
//                }
//            }
//        } else {
//            result_sort.addAll(result);
//        }
//
//        return result_sort;
//    }

//    public Map<String, ArrayList<String>> getIpAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        Map<String, ArrayList<String>> result = new HashMap();
//
//        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIpAddress").entrySet()) {
//            String node = entry.getKey();
//            ArrayList<String> tmp_list = getIpAddressFromNode(walkInformationFromNodes, node);
//            result.put(node, tmp_list);
//        }
//
//        return result;
//    }
//
//    public Map<String, ArrayList<String>> getIpAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, String> info_nodes) {
//        Map<String, ArrayList<String>> result = new HashMap();
//
//        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node = entry.getKey();
//            ArrayList<String> tmp_list = getIpAddressFromNode(walkInformationFromNodes, node);
//            result.put(node, tmp_list);
//        }
//
//        return result;
//    }

    //    private String GetRealIpAddress(Map<String, ArrayList<String>> hash_ip, String ip_search) {
//        String result=ip_search;
//        
//        for (Map.Entry<String, ArrayList<String>> entry : hash_ip.entrySet()) {
//            boolean find=false;
//            for(String ip : entry.getValue()) {
//                if(ip.equals(ip_search)) {
//                    result=entry.getKey();
//                    find=true;
//                    break;
//                }
//            }
//            if(find) break;
//        }  
//        return result;
//    }
    public String hexMapToVlans(String hex) {
        StringBuilder result = new StringBuilder();

        if (hex != null && !hex.isEmpty()) {
            String[] fields = hex.split(":");
            StringBuilder hash = new StringBuilder();
            for (String field : fields) {
                if (field.matches("^[0-9A-Fa-f]{1,2}$")) {
                    String octet = Integer.toBinaryString(Integer.parseInt(field, 16));
                    hash.append("0".repeat(Math.max(0, 8 - octet.length())));
                    hash.append(octet);
                } else {
                    for (int i = 0; i < field.length(); i++) {
                        String octet = Integer.toBinaryString(field.charAt(i));
                        hash.append("0".repeat(Math.max(0, 8 - octet.length())));
                        hash.append(octet);
                    }
                }
            }
            boolean first = true;
            for (int p = 0; p < hash.length(); p++) {
                if (String.valueOf(hash.charAt(p)).equals("1")) {
                    if (!first) {
                        result.append(":").append(p);
                    } else {
                        result.append(p);
                    }
                    first = false;
                }
            }
        }
        return result.toString();
    }

    public String replaceDelimiter(String str) {
        if (str != null) {
            str = str.replaceAll("\n", " ").replaceAll("\r", "");
            str = str.replaceAll(";", " ");
            str = str.replaceAll("\\|", " ");
            str = str.replaceAll(",", " ");
        }

        return str;
    }

    public String translateIP(String ip) {
        String out = null;

        if (ip != null) {
            if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                out = ip;
            } else {
                if (ip.length() == 4) {
                    String s = String.valueOf((int) ip.charAt(0));
                    out = s;
                    s = String.valueOf((int) ip.charAt(1));
                    out = out + "." + s;
                    s = String.valueOf((int) ip.charAt(2));
                    out = out + "." + s;
                    s = String.valueOf((int) ip.charAt(3));
                    out = out + "." + s;
                } else {
                    String[] buf = ip.split(":");
                    if (buf.length == 4) {
                        String s = String.valueOf(Integer.parseInt(buf[0], 16));
                        out = s;
                        s = String.valueOf(Integer.parseInt(buf[1], 16));
                        out = out + "." + s;
                        s = String.valueOf(Integer.parseInt(buf[2], 16));
                        out = out + "." + s;
                        s = String.valueOf(Integer.parseInt(buf[3], 16));
                        out = out + "." + s;
                    } else {
                        buf = ip.split(" ");
                        if (buf.length == 4) {
                            String s = String.valueOf(Integer.parseInt(buf[0], 16));
                            out = s;
                            s = String.valueOf(Integer.parseInt(buf[1], 16));
                            out = out + "." + s;
                            s = String.valueOf(Integer.parseInt(buf[2], 16));
                            out = out + "." + s;
                            s = String.valueOf(Integer.parseInt(buf[3], 16));
                            out = out + "." + s;
                        }
                        //                    else System.out.println("Error - "+ip);
                    }
                }
            }
        }

        return out;
    }

    public String translateMAC(String mac) {
        StringBuilder out = new StringBuilder();

        if (mac != null) {
            if (mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                out = new StringBuilder(mac);
            } else if (mac.matches("^([0-9A-Fa-f]{1,2}[:-]){5}([0-9A-Fa-f]{1,2})$")) {
                String[] mas = mac.split(":");
                for (String it : mas) {
                    if (it.length() == 1)
                        out.append(":0").append(it);
                    else
                        out.append(":").append(it);
                }
                out = new StringBuilder(out.substring(1));
            } else {
                if (mac.length() == 6) {
                    byte[] bb = mac.getBytes();
                    out = new StringBuilder(String.format("%02x", bb[0]));
                    for (int i = 1; i < bb.length; i++) {
                        out.append(":").append(String.format("%02x", bb[i]));
                    }
                }
            }
        }
        return out.toString().toLowerCase().replace("-", ":").replace(" ", ":");
    }

    // Output format: mac ---> ip
    public Map<String, String> getARP(ArrayList<String[]> node_community_version) {
        String ArpTable = "1.3.6.1.2.1.3.1.1.2";
        String ArpTable1 = "1.3.6.1.2.1.4.22.1.2";

        ArrayList<String[]> mac_ip_node = new ArrayList();
        if (!node_community_version.isEmpty()) {
            WalkPool walkPool = new WalkPool();
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool ArpTable");
//            watch_Telemetry_Lib.start();
            Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version, ArpTable, Neb.timeout_thread);
//            watch_Telemetry_Lib.exit = true;
            ArrayList<String[]> buff = new ArrayList();
            for (String[] item1 : node_community_version) {
                if (res.get(item1[0]) == null) {
                    String[] mas = new String[3];
                    mas[0] = item1[0];
                    mas[1] = item1[1];
                    mas[2] = item1[2];
                    buff.add(mas);
                }
            }
            Map<String, ArrayList> map = new HashMap<>(res);
            if (!buff.isEmpty()) {
//                watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool ArpTable1");
//                watch_Telemetry_Lib.start();
                res = walkPool.get(buff, ArpTable1, Neb.timeout_thread);
//                watch_Telemetry_Lib.exit = true;
                map.putAll(res);
            }

            for (Map.Entry<String, ArrayList> entry : map.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                for (String[] item : val_list) {
                    String[] buf = item[0].split("\\.");
                    if (buf.length == 16) {
                        String ip = buf[12] + "." + buf[13] + "." + buf[14] + "." + buf[15];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac = mac_Char_To_HEX(item[1]);
                            if (mac != null) {
                                String[] mas = new String[3];
                                mas[0] = mac;
                                mas[1] = ip;
                                mas[2] = node;
                                mac_ip_node.add(mas);
                                //                                logger.Println("ARP: "+mac+","+ip, logger.DEBUG);
                            }
                        }
                    } else if (buf.length == 15) {
                        String ip = buf[11] + "." + buf[12] + "." + buf[13] + "." + buf[14];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac = mac_Char_To_HEX(item[1]);
                            if (mac != null) {
                                String[] mas = new String[3];
                                mas[0] = mac;
                                mas[1] = ip;
                                mas[2] = node;
                                mac_ip_node.add(mas);
                                //                                logger.Println("ARP: "+mac+","+ip, logger.DEBUG);
                            }
                        }
                    }
                }
            }
        }
        Map<String, String> result_area = processingARP(mac_ip_node);

        return result_area;
    }

    public Map<String, String> processingARP(ArrayList<String[]> mac_ip_node) {
        Map<String, String> result = new HashMap();

        Map<String, ArrayList<String[]>> node_macip = new HashMap();
        for (String[] iter1 : mac_ip_node) {
            String mac = iter1[0];
            String ip = iter1[1];
            String node = iter1[2];
            if (node_macip.get(node) == null) {
                ArrayList<String[]> tmp_list = new ArrayList();
                String[] mas = new String[2];
                mas[0] = mac;
                mas[1] = ip;
                tmp_list.add(mas);
                node_macip.put(node, tmp_list);
            } else {
                String[] mas = new String[2];
                mas[0] = mac;
                mas[1] = ip;
                node_macip.get(node).add(mas);
            }
        }

        Map<String, Map<String, String[]>> node_mac_ipscore = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : node_macip.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, Integer> mac_score = new HashMap();
            for (String[] macip : val) {
                String mac = macip[0];
                if (mac_score.get(mac) == null) {
                    mac_score.put(mac, 1);
                } else {
                    int score = mac_score.get(mac) + 1;
                    mac_score.put(mac, score);
                }
            }

            Map<String, String[]> mac_ipscore = new HashMap();
            for (String[] macip : val) {
                String mac = macip[0];
                String ip = macip[1];
                if (mac_score.get(mac) != null) {
                    String[] mas = new String[2];
                    mas[0] = ip;
                    mas[1] = String.valueOf(mac_score.get(mac));
                    mac_ipscore.put(mac, mas);
                }
            }
            node_mac_ipscore.put(node, mac_ipscore);
        }

        Map<String, String[]> mac_ipscore = new HashMap();
        for (Map.Entry<String, Map<String, String[]>> entry : node_mac_ipscore.entrySet()) {
            Map<String, String[]> val = entry.getValue();
            for (Map.Entry<String, String[]> entry1 : val.entrySet()) {
                String mac = entry1.getKey();
                String[] val1 = entry1.getValue();
                String score = val1[1];
                if (mac_ipscore.get(mac) == null) {
                    mac_ipscore.put(mac, val1);
                } else if (mac_ipscore.get(mac) != null && Integer.parseInt(score) < Integer.parseInt(mac_ipscore.get(mac)[1])) {
                    mac_ipscore.put(mac, val1);
                }
            }
        }

        for (Map.Entry<String, String[]> entry : mac_ipscore.entrySet()) {
            String mac = entry.getKey();
            String[] val = entry.getValue();
            result.put(mac, val[0]);
        }

        return result;
    }

    // Output format: ArrayList(String)
//    private ArrayList GetVlanCommunity(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
//        ArrayList result = new ArrayList();
//        
//        ArrayList<String[]> res = walkInformationFromNodes.get("VlanCommunity").get(node);
//        if(res != null) {
//            res = SetUniqueList(res, 1);
//            for(String[] item : res) {
//                String[] mas = item[1].split(":");
//                String community = "";
//                if(mas.length > 1) {
//                    for(String item1 : mas) {
//                        String ch = convertHexToString(item1);
//                        community=community+ch;
//                    }
//                } else community=item[1];
//                if(!community.equals("") && !community.startsWith("@")) result.add(community);
//            }
//        }
//        return result;
//    }
    // Output format: node ---> id_iface ---> list(mac)
    public Map<String, Map<String, ArrayList>> getMAC(ArrayList<String[]> node_community_version) {
//        int retries = 1;
        ArrayList<String> macTable = new ArrayList();
        macTable.add("1.3.6.1.2.1.17.4.3.1.2");
        macTable.add("1.3.6.1.2.1.17.7.1.2.2.1.2");

        String vlan_community = "1.3.6.1.2.1.47.1.2.1.1.4";
        String vtpVlanName = "1.3.6.1.4.1.9.9.46.1.3.1.1.4";

        String IfaceMaping = "1.3.6.1.2.1.17.1.4.1.2";
//        String ifDescr = "1.3.6.1.2.1.2.2.1.2";

        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";
        int snmp_port = 161;
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;


        WalkPool walkPool = new WalkPool();

        // select only number iface > 2
//        Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool " + ifDescr);
//        watch_Telemetry_Lib.start();

        Map<String, Map<String, ArrayList>> result_area = new HashMap<>();

        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
        for (String[] it1 : node_community_version) {
            String[] mas = new String[4];
            mas[0] = it1[0];
            mas[1] = it1[1];
            mas[2] = it1[2];
            mas[3] = macTable.get(0);
            list_node_community_version_oid.add(mas);
        }

        ArrayList<ArrayList> node_multicommunity_version_list = new ArrayList();
//        watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool " + vlan_community);
//        watch_Telemetry_Lib.start();
        logger.Println("\tStart Walk vlan_community ...", logger.DEBUG);
        Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version, vlan_community, Neb.timeout_thread);
        logger.Println("\tStop Walk vlan_community ...", logger.DEBUG);
//        watch_Telemetry_Lib.exit = true;
        ArrayList<String[]> buff = new ArrayList();
        ArrayList<String[]> list_node_community_version_oid_new = new ArrayList();
        for (String[] item1 : list_node_community_version_oid) {
            if (res.get(item1[0]) == null) {
                String[] mas = new String[4];
                mas[0] = item1[0];
                mas[1] = item1[1];
                mas[2] = item1[2];
                mas[3] = item1[3];
                buff.add(mas);
            } else {
                list_node_community_version_oid_new.add(item1);
            }
        }

        if (res != null && !res.isEmpty()) {
            for (String[] item : list_node_community_version_oid_new) {
                if (res.get(item[0]) != null) {
                    ArrayList node_community_version_oid = new ArrayList();
                    node_community_version_oid.add(item[0]);
                    String[] mas = item[1].split("\\|");
                    if (mas.length != 5) {
                        ArrayList community_list = new ArrayList();
                        for (String[] item1 : res.get(item[0])) {
                            String community = translateHexString_to_SymbolString(item1[1]).replaceAll("NUL", "@");
                            if (!community.contains("@")) {
                                community = item[1];
                            }
                            if (!community_list.contains(community)) {
                                if (community != null && !community.isEmpty()) {
                                    community_list.add(community);
                                }
                            }
                        }
                        if (community_list.isEmpty()) {
                            community_list.add(item[1]);
                        }

                        node_community_version_oid.add(community_list);
                        node_community_version_oid.add(item[2]);
                        node_community_version_oid.add(item[3]);
                        node_multicommunity_version_list.add(node_community_version_oid);
                    }
                } else {
                    ArrayList node_community_version_oid = new ArrayList();
                    node_community_version_oid.add(item[0]);
                    ArrayList community_list = new ArrayList();
                    String community = translateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                    if (!community_list.contains(community)) {
                        if (community != null && !community.isEmpty()) {
                            community_list.add(community);
                        }
                    }
                    node_community_version_oid.add(community_list);
                    node_community_version_oid.add(item[2]);
                    node_community_version_oid.add(item[3]);
                    node_multicommunity_version_list.add(node_community_version_oid);
                }
            }
        }
        ///////////////////////////////

        if (!buff.isEmpty()) {
//            watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool " + vtpVlanName);
//            watch_Telemetry_Lib.start();
            logger.Println("\tStart Walk vtpVlanName ...", logger.DEBUG);
            res = walkPool.get(buff, vtpVlanName, Neb.timeout_thread);
            logger.Println("\tStop Walk vtpVlanName.", logger.DEBUG);
//            watch_Telemetry_Lib.exit = true;
            if (res != null && !res.isEmpty()) {
                for (String[] item : buff) {
                    boolean find = false;
                    for (Map.Entry<String, ArrayList<String[]>> entry : res.entrySet()) {
                        String node = entry.getKey();
                        ArrayList<String[]> val = entry.getValue();
                        if (item[0].equals(node)) {
                            ArrayList node_community_version_oid = new ArrayList();
                            node_community_version_oid.add(item[0]);
                            ArrayList community_list = new ArrayList();
                            for (String[] item1 : val) {
                                String[] tmp = item1[0].split("\\.");
                                String id_vlan = tmp[tmp.length - 1];

                                community_list.add(item[1] + "@" + id_vlan);
                            }
                            node_community_version_oid.add(community_list);
                            node_community_version_oid.add(item[2]);
                            node_community_version_oid.add(item[3]);
                            node_multicommunity_version_list.add(node_community_version_oid);
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        ArrayList node_community_version_oid = new ArrayList();
                        node_community_version_oid.add(item[0]);
                        ArrayList community_list = new ArrayList();
                        String community = translateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                        if (!community_list.contains(community)) {
                            if (community != null && !community.isEmpty()) {
                                community_list.add(community);
                            }
                        }

                        node_community_version_oid.add(community_list);
                        node_community_version_oid.add(item[2]);
                        node_community_version_oid.add(item[3]);
                        node_multicommunity_version_list.add(node_community_version_oid);
                    }
                }
            }

        }
        /////////////////

        if (!buff.isEmpty()) {
            for (String[] item : buff) {
                ArrayList node_community_version_oid = new ArrayList();
                node_community_version_oid.add(item[0]);
                ArrayList community_list = new ArrayList();
                //                community_list.add(TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@"));
                String community = translateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                if (community != null && !community.isEmpty()) {
                    community_list.add(community);
                }

                node_community_version_oid.add(community_list);
                node_community_version_oid.add(item[2]);
                node_community_version_oid.add(item[3]);
                node_multicommunity_version_list.add(node_community_version_oid);
            }
        }

        ////////////////////////////////////////////////////
        // remove not unical for node records
        Map<String, ArrayList<String>> map_tmp = new HashMap();
        for (ArrayList<String> item : node_multicommunity_version_list) {
            map_tmp.put(item.get(0), item);
        }
        node_multicommunity_version_list.clear();
        for (Map.Entry<String, ArrayList<String>> entry : map_tmp.entrySet()) {
            node_multicommunity_version_list.add(entry.getValue());
        }

        ArrayList<ArrayList> node_multicommunity_version_list_orig = new ArrayList(node_multicommunity_version_list);

        // fast mac address scaning. SNMP BULK for 1.3.6.1.2.1.17.4.3.1.2
        int num_mac_records = 0;
        logger.Println("\tStart Walk node_community_version_oid_list for MAC", logger.DEBUG);
        res = walkPool.getNodeMultiCommunityVersionOid(node_multicommunity_version_list, Neb.timeout_thread_mac, snmp_port, Neb.timeout_mac, Neb.retries_mac);
        logger.Println("\tStop Walk node_community_version_oid_list for MAC", logger.DEBUG);
//            ArrayList<ArrayList> error_node_community_version_oid_list = new ArrayList();

        // fast mac address scaning. SNMP BULK for 1.3.6.1.2.1.17.7.1.2.2.1.2
        ArrayList<ArrayList> node_community_version_oid_list_new = new ArrayList();
        for (ArrayList it : node_multicommunity_version_list) {
            String node = (String) it.get(0);
            if (res.get(node) == null) {
                it.set(3, macTable.get(1));
                node_community_version_oid_list_new.add(it);
                logger.Println("\tPrepare alternative MAC table for 1.3.6.1.2.1.17.7.1.2.2.1.2 node - " + node, logger.DEBUG);
            }
        }
        node_multicommunity_version_list = node_community_version_oid_list_new;
        if (!node_multicommunity_version_list.isEmpty()) {
            logger.Println("\tStart alternative Walk node_community_version_oid_list for MAC 1.3.6.1.2.1.17.7.1.2.2.1.2", logger.DEBUG);
            Map<String, ArrayList<String[]>> res1 = walkPool.getNodeMultiCommunityVersionOid(node_multicommunity_version_list, Neb.timeout_thread_mac, snmp_port, Neb.timeout_mac, Neb.retries_mac);
            logger.Println("\tStop alternative Walk node_community_version_oid_list for MAC 1.3.6.1.2.1.17.7.1.2.2.1.2", logger.DEBUG);
            for (Map.Entry<String, ArrayList<String[]>> entry : res1.entrySet()) {
                String node = entry.getKey();
                logger.Println("MAC address alternative 1.3.6.1.2.1.17.7.1.2.2.1.2 from  node - " + node, logger.DEBUG);
            }
            res.putAll(res1);
        }

//        // carefully mac address scaning. 1.3.6.1.2.1.17.4.3.1.2
//        node_community_version_oid_list_new = new ArrayList();
//        for(ArrayList it : node_multicommunity_version_list) {
//            String node = (String)it.get(0);
//            if(res.get(node) == null) {
//                it.set(3, macTable.get(0));
//                node_community_version_oid_list_new.add(it);
//                logger.Println("\tPrepare carefully MAC table for 1.3.6.1.2.1.17.4.3.1.2 node - "+node, logger.DEBUG);
//            }
//        }
//        node_multicommunity_version_list = node_community_version_oid_list_new;
//        if(!node_multicommunity_version_list.isEmpty()) {
//            Map<String, ArrayList<String[]>> res2 = get_Mac_retry_scanning(node_multicommunity_version_list); 
//            res.putAll(res2);
//        }

        for (ArrayList it : node_multicommunity_version_list) {
            String node = (String) it.get(0);
            if (res.get(node) == null) {
                logger.Println("Not MAC address from  node - " + node, logger.DEBUG);
            }
        }


        for (Map.Entry<String, ArrayList<String[]>> entry : res.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            for (String[] item : val_list) {
                String mac = "";
                String[] buf = item[0].split("\\.");
                if (buf.length == 17) {
                    mac = decToHex(Integer.parseInt(buf[11]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[12]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[13]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[14]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[15]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[16]));
                }
                if (buf.length == 20) {
                    mac = decToHex(Integer.parseInt(buf[14]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[15]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[16]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[17]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[18]));
                    mac = mac + ":" + decToHex(Integer.parseInt(buf[19]));
                }
                String id_iface = item[1];

                if (!(mac.isEmpty() || id_iface.isEmpty())) {
                    if (!result_area.isEmpty() && result_area.get(node) != null && result_area.get(node).get(id_iface) != null) {
                        boolean find = false;
                        ArrayList<String> tmp_list = result_area.get(node).get(id_iface);
                        for (String item1 : tmp_list) {
                            if (item1.equals(mac)) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            result_area.get(node).get(id_iface).add(mac);
//                                logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                            num_mac_records++;
                        }
                    } else if (!result_area.isEmpty() && result_area.get(node) != null && result_area.get(node).get(id_iface) == null) {
                        ArrayList tmp_list = new ArrayList();
                        tmp_list.add(mac);
//                            logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                        result_area.get(node).put(id_iface, tmp_list);
                        num_mac_records++;
                    } else {
                        ArrayList tmp_list = new ArrayList();
                        tmp_list.add(mac);
//                            logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                        Map<String, ArrayList> tmp_map = new HashMap<>();
                        tmp_map.put(id_iface, tmp_list);
                        result_area.put(node, tmp_map);
                        num_mac_records++;
                    }
                }
            }
        }


        /////////////////////////////////////////////////
        //////////////////////////////////////////////////
        node_multicommunity_version_list = node_multicommunity_version_list_orig;

        ArrayList<ArrayList> node_multicommunity_version_oid_list = new ArrayList();
        for (ArrayList node_community_version_oid : node_multicommunity_version_list) {
            ArrayList node_multucommunity_version_oid = new ArrayList();
            node_multucommunity_version_oid.add(node_community_version_oid.get(0));
            node_multucommunity_version_oid.add(node_community_version_oid.get(1));
            node_multucommunity_version_oid.add(node_community_version_oid.get(2));
            node_multucommunity_version_oid.add(IfaceMaping);
            node_multicommunity_version_oid_list.add(node_multucommunity_version_oid);
        }

        // output: node ---> id ---> id_translate
        Map<String, Map<String, String>> tmp_map = new HashMap<>();
//        watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool node_community_version_list IfaceMaping");
//        watch_Telemetry_Lib.start();
        Map<String, ArrayList<String[]>> res_iface_maping = walkPool.getNodeMultiCommunityVersionOid(node_multicommunity_version_oid_list, Neb.timeout_thread, snmp_port, Neb.timeout, Neb.retries);
//        watch_Telemetry_Lib.exit = true;
        if (res_iface_maping != null && !res_iface_maping.isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : res_iface_maping.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp_map1 = new HashMap<>();
                for (String[] item : val_list) {
                    String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    tmp_map1.computeIfAbsent(id, k -> item[1]);
                }
                tmp_map.put(node, tmp_map1);
            }
        } else {
            return result_area;
        }

        // translate in result id_iface to id_iface_translate
        Map<String, Map<String, ArrayList>> out = new HashMap<>();
        for (Map.Entry<String, Map<String, ArrayList>> entry : result_area.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            Map<String, ArrayList> map = new HashMap<>();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                String iface_id = entry1.getKey();
                ArrayList<String> val_list1 = entry1.getValue();
                if (tmp_map.get(node) != null && tmp_map.get(node).get(iface_id) != null) {
                    map.put(tmp_map.get(node).get(iface_id), val_list1);
                } else {
                    map.put(iface_id, val_list1);
                }
            }
            out.put(node, map);
        }

        ArrayList<String[]> node_community_version_list = new ArrayList();
        for (ArrayList node_community_version_oid : node_multicommunity_version_list) {
            String[] mas = new String[3];
            mas[0] = (String) node_community_version_oid.get(0);
            mas[1] = ((String) ((ArrayList) node_community_version_oid.get(1)).get(0)).split("@")[0];
            mas[2] = (String) node_community_version_oid.get(2);
            node_community_version_list.add(mas);
        }

        // select only ifaces if ifOperStatus is Up
        tmp_map = new HashMap<>();
//        watch_Telemetry_Lib = new Watch_Telemetry_Lib("WalkPool node_community_version_list ifOperStatus");
//        watch_Telemetry_Lib.start();
        Map<String, ArrayList<String[]>> res_iface_ifOperStatus = walkPool.get(node_community_version_list, ifOperStatus, Neb.timeout_thread);
//        watch_Telemetry_Lib.exit = true;
        if (res_iface_ifOperStatus != null && !res_iface_ifOperStatus.isEmpty()) {
            for (Map.Entry<String, ArrayList<String[]>> entry : res_iface_ifOperStatus.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp_map1 = new HashMap<>();
                for (String[] item : val_list) {
                    String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                    tmp_map1.computeIfAbsent(id, k -> item[1]);
                }
                tmp_map.put(node, tmp_map1);
            }
        } else {
            return result_area;
        }

        Map<String, Map<String, ArrayList>> out_map = new HashMap<>();
        for (Map.Entry<String, Map<String, ArrayList>> entry : out.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_map = entry.getValue();
            Map<String, ArrayList> val_map_new = new HashMap();
            for (Map.Entry<String, ArrayList> entry1 : val_map.entrySet()) {
                String iface_id = entry1.getKey();
                ArrayList val_list = entry1.getValue();
                if (tmp_map.get(node) != null && tmp_map.get(node).get(iface_id) != null) {
                    if (tmp_map.get(node).get(iface_id).equals("1")) {
                        val_map_new.put(iface_id, val_list);
                    }
                }
            }
            out_map.put(node, val_map_new);
        }

        return out_map;
    }

    public String decToHex(int dec) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};

        String hex = String.valueOf(hexDigits[dec / 16]) + hexDigits[dec % 16];

        return hex;
    }

    public String convertHexToString(String hex) {

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        try {
            for (int i = 0; i < hex.length() - 1; i += 3) {

                //grab the hex in pairs
                String output = hex.substring(i, (i + 2));
                //convert hex to decimal
                int decimal = Integer.parseInt(output, 16);
                if (decimal == 0) {
                    decimal = 64;
                }
                //convert the decimal to character
                sb.append((char) decimal);

                temp.append(decimal);
            }
            //              System.out.println("Decimal : " + temp.toString());

            return sb.toString();
        } catch (NumberFormatException e) {
            return hex;
        }

    }

    // Output format: list(node, iface_id, iface_name, ip, mac 
    public ArrayList<String[]> arpmac(Map<String, Map<String, ArrayList>> MAC,
                                      Map<String, String> ARP,
                                      ArrayList<String[]> node_community_version,
                                      Map<String, Map<String, String>> node_ifaceid_ifacename) {


        ArrayList<String[]> result_area = new ArrayList();

        Map<String, Map<String, ArrayList>> walkInterfacesFromNodes = getWalkInterfacesFromNodes(node_community_version);
        Map<String, Map<String, String>> node_id_iftype = new HashMap();
        for (Map.Entry<String, ArrayList> entry1 : walkInterfacesFromNodes.get("ifType").entrySet()) {
            String node = entry1.getKey();
            ArrayList<String[]> val = entry1.getValue();
            Map<String, String> map_tmp = new HashMap();
            for (String[] item : val) {
                String id = item[0].split("\\.")[item[0].split("\\.").length - 1];
                map_tmp.put(id, item[1]);
            }
            node_id_iftype.put(node, map_tmp);
        }

        // Output format: node ---> iface_id ---> list(iface_name, list(ip, mac))
        Map<String, Map<String, ArrayList>> node_iface_id_iface_name_ip_mac = new HashMap<>();
        if (MAC != null && !MAC.isEmpty()
                && ARP != null && !ARP.isEmpty()) {
            for (Map.Entry<String, Map<String, ArrayList>> entry : MAC.entrySet()) {
                String node = entry.getKey();
                Map<String, ArrayList> val_list = entry.getValue();
                Map<String, ArrayList> map = new HashMap<>();
                for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                    String iface_id = entry1.getKey();
                    ArrayList<String> val_list1 = entry1.getValue();
                    ArrayList<String[]> list1 = new ArrayList();
                    for (String mac : val_list1) {
                        //                        System.out.println(node+","+iface_id+","+mac);
                        String[] ip_mac = new String[2];
                        String ip = ARP.get(mac);
                        if (ip != null) {
                            ip_mac[0] = ip;
                            ip_mac[1] = mac;
                            //                            System.out.println(node+","+iface_id+","+ip+","+mac);
                        } else {
                            ip_mac[0] = "unknown_ip";
                            ip_mac[1] = mac;
                            //                            System.out.println(node+","+iface_id+","+"unknown_ip"+","+mac);
                        }
                        list1.add(ip_mac);
                    }

                    String iface_name = "";
                    String iface_type = "";
                    if (node_ifaceid_ifacename != null &&
                            node_ifaceid_ifacename.get(node) != null &&
                            node_ifaceid_ifacename.get(node).get(iface_id) != null) {
                        iface_name = node_ifaceid_ifacename.get(node).get(iface_id);

                        if (node_id_iftype.get(node) != null && node_id_iftype.get(node).get(iface_id) != null) {
                            String iftype = node_id_iftype.get(node).get(iface_id);
                            if (!iftype.equals("24") && Integer.parseInt(iftype) <= 32) {
                                iface_type = iftype;
                            }
                        }
                    }

                    if (!iface_name.isEmpty() && !iface_type.isEmpty()) {
                        ArrayList iface_name_list_ip_mac = new ArrayList();
                        iface_name_list_ip_mac.add(iface_name);
                        iface_name_list_ip_mac.add(list1);
                        map.put(iface_id, iface_name_list_ip_mac);
                    }
                }
                node_iface_id_iface_name_ip_mac.put(node, map);
            }
        }
        // Output format: ip ---> list(node, iface_id, iface_name, mac)
        /////////////////////////////////////////////////////////////////////////
        for (Map.Entry<String, Map<String, ArrayList>> entry : node_iface_id_iface_name_ip_mac.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                String iface_id = entry1.getKey();
                ArrayList val_list1 = entry1.getValue();
                String iface_name = (String) val_list1.get(0);
                ArrayList<String[]> ip_mac = (ArrayList) val_list1.get(1);

                for (String[] item : ip_mac) {
                    String ip = item[0];
                    String mac = item[1];
                    String[] mas = new String[5];
                    mas[0] = node;
                    mas[1] = iface_id;
                    mas[2] = iface_name;
                    mas[3] = ip;
                    mas[4] = mac;
                    result_area.add(mas);
                    //                    outFile.write("ARP_MAC: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+"\n");
                    //                    System.out.println("ARP_MAC: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]);
                }
            }

        }

        return result_area;
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public String getInCountersTesting(String node, String community, int version, String id_iface, String id_iface_alt) {
        String ifInUcastPkts = "1.3.6.1.2.1.2.2.1.11";
//        String ifOutUcastPkts = "1.3.6.1.2.1.2.2.1.17";
        String ifInNUcastPkts = "1.3.6.1.2.1.2.2.1.12";
//        String ifOutNUcastPkts = "1.3.6.1.2.1.2.2.1.18"; 
        String ifInDiscards = "1.3.6.1.2.1.2.2.1.13";
//        String ifOutDiscards = "1.3.6.1.2.1.2.2.1.19";
        String ifInErrors = "1.3.6.1.2.1.2.2.1.14";
//        String ifOutErrors = "1.3.6.1.2.1.2.2.1.20";        
        String ifHCInUcastPkts = "1.3.6.1.2.1.31.1.1.1.7";
//        String ifHCOutUcastPkts = "1.3.6.1.2.1.31.1.1.1.11";
        String ifHCInMulticastPkts = "1.3.6.1.2.1.31.1.1.1.8";
//        String ifHCOutMulticastPkts = "1.3.6.1.2.1.31.1.1.1.12";  
        String ifHCInBroadcastPkts = "1.3.6.1.2.1.31.1.1.1.9";
//        String ifHCOutBroadcastPkts = "1.3.6.1.2.1.31.1.1.1.13"; 

        try {
            GetSnmp getSnmp = new GetSnmp();
            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            String inUcastPkts = getSnmp.getLite(snmp, node, community, ifHCInUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (inUcastPkts == null) {
                if (id_iface_alt != null) {
                    inUcastPkts = getSnmp.getLite(snmp, node, community, ifHCInUcastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
                }
                if (inUcastPkts == null) {
                    inUcastPkts = getSnmp.getLite(snmp, node, community, ifInUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
                }
            }
            String inMulticastPkts = getSnmp.getLite(snmp, node, community, ifHCInMulticastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (inMulticastPkts == null && id_iface_alt != null) {
                inMulticastPkts = getSnmp.getLite(snmp, node, community, ifHCInMulticastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
            }
            String inBroadcastPkts = getSnmp.getLite(snmp, node, community, ifHCInBroadcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (inBroadcastPkts == null && id_iface_alt != null) {
                inBroadcastPkts = getSnmp.getLite(snmp, node, community, ifHCInBroadcastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
            }
            String inNUcastPkts = getSnmp.getLite(snmp, node, community, ifInNUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            String inDiscards = getSnmp.getLite(snmp, node, community, ifInDiscards + "." + id_iface, version, 161, 3, 2)[1];
            String inErrors = getSnmp.getLite(snmp, node, community, ifInErrors + "." + id_iface, version, 161, 3, 2)[1];

            if (inUcastPkts == null || inUcastPkts.isEmpty()) inUcastPkts = "0";
            if (inMulticastPkts == null || inMulticastPkts.isEmpty()) inMulticastPkts = "0";
            if (inBroadcastPkts == null || inBroadcastPkts.isEmpty()) inBroadcastPkts = "0";
            if (inNUcastPkts == null || inNUcastPkts.isEmpty()) inNUcastPkts = "0";
            if (inDiscards == null || inDiscards.isEmpty()) inDiscards = "0";
            if (inErrors == null || inErrors.isEmpty()) inErrors = "0";
            String out = inUcastPkts + "," + inMulticastPkts + "," + inBroadcastPkts + "," + inNUcastPkts + "," + inDiscards + "," + inErrors;

            // disconnect
            snmp.close();
            transport.close();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            return out;
        } catch (IOException ex) {
            System.out.println("Error SNMP!!!");
            double MAX_VALUE64 = Math.pow(2, 64);
            return String.valueOf(Math.abs(Math.random() * MAX_VALUE64));
        }
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public String getOutCountersTesting(String node, String community, int version, String id_iface, String id_iface_alt) {
//        String ifInUcastPkts = "1.3.6.1.2.1.2.2.1.11";
        String ifOutUcastPkts = "1.3.6.1.2.1.2.2.1.17";
//        String ifInNUcastPkts = "1.3.6.1.2.1.2.2.1.12";
        String ifOutNUcastPkts = "1.3.6.1.2.1.2.2.1.18";
//        String ifInDiscards = "1.3.6.1.2.1.2.2.1.13";
        String ifOutDiscards = "1.3.6.1.2.1.2.2.1.19";
//        String ifInErrors = "1.3.6.1.2.1.2.2.1.14";
        String ifOutErrors = "1.3.6.1.2.1.2.2.1.20";
//        String ifHCInUcastPkts = "1.3.6.1.2.1.31.1.1.1.7";
        String ifHCOutUcastPkts = "1.3.6.1.2.1.31.1.1.1.11";
//        String ifHCInMulticastPkts = "1.3.6.1.2.1.31.1.1.1.8";
        String ifHCOutMulticastPkts = "1.3.6.1.2.1.31.1.1.1.12";
//        String ifHCInBroadcastPkts = "1.3.6.1.2.1.31.1.1.1.9";
        String ifHCOutBroadcastPkts = "1.3.6.1.2.1.31.1.1.1.13";

        try {
            GetSnmp getSnmp = new GetSnmp();
            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            String outUcastPkts = getSnmp.getLite(snmp, node, community, ifHCOutUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (outUcastPkts == null) {
                if (id_iface_alt != null) {
                    outUcastPkts = getSnmp.getLite(snmp, node, community, ifHCOutUcastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
                }
                if (outUcastPkts == null) {
                    outUcastPkts = getSnmp.getLite(snmp, node, community, ifOutUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
                }
            }
            String outMulticastPkts = getSnmp.getLite(snmp, node, community, ifHCOutMulticastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (outMulticastPkts == null && id_iface_alt != null) {
                outMulticastPkts = getSnmp.getLite(snmp, node, community, ifHCOutMulticastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
            }
            String outBroadcastPkts = getSnmp.getLite(snmp, node, community, ifHCOutBroadcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            if (outBroadcastPkts == null && id_iface_alt != null) {
                outBroadcastPkts = getSnmp.getLite(snmp, node, community, ifHCOutBroadcastPkts + "." + id_iface_alt, version, 161, 3, 2)[1];
            }
            String outNUcastPkts = getSnmp.getLite(snmp, node, community, ifOutNUcastPkts + "." + id_iface, version, 161, 3, 2)[1];
            String outDiscards = getSnmp.getLite(snmp, node, community, ifOutDiscards + "." + id_iface, version, 161, 3, 2)[1];
            String outErrors = getSnmp.getLite(snmp, node, community, ifOutErrors + "." + id_iface, version, 161, 3, 2)[1];

            if (outUcastPkts == null || outUcastPkts.isEmpty()) outUcastPkts = "0";
            if (outMulticastPkts == null || outMulticastPkts.isEmpty()) outMulticastPkts = "0";
            if (outBroadcastPkts == null || outBroadcastPkts.isEmpty()) outBroadcastPkts = "0";
            if (outNUcastPkts == null || outNUcastPkts.isEmpty()) outNUcastPkts = "0";
            if (outDiscards == null || outDiscards.isEmpty()) outDiscards = "0";
            if (outErrors == null || outErrors.isEmpty()) outErrors = "0";

            String out = outUcastPkts + "," + outMulticastPkts + "," + outBroadcastPkts + "," + outNUcastPkts + "," + outDiscards + "," + outErrors;

            // disconnect
            snmp.close();
            transport.close();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            return out;
        } catch (IOException ex) {
            System.out.println("Error SNMP!!!");
            double MAX_VALUE64 = Math.pow(2, 64);
            return String.valueOf(Math.abs(Math.random() * MAX_VALUE64));
        }
    }

    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2, in1, ou1, in2, out2, time))
    public ArrayList<String[]> getCountersTestingLinks(ArrayList<String[]> links, Map<String, String[]> node_community_version_map) {
        ArrayList<String[]> result = new ArrayList();

//        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
        try (ExecutorService service = Executors.newFixedThreadPool(Neb.MAXPOOLTHREADS)) {
            CopyOnWriteArrayList<Callable<String[]>> callables = new CopyOnWriteArrayList<>();
            for (String[] link : links) {
                callables.add(() -> countersTestingLink(link, node_community_version_map));
            }

            try {
                for (Future<String[]> f : service.invokeAll(callables)) {
                    String[] res = null;
                    try {
                        res = f.get(Neb.timeout_thread, TimeUnit.MINUTES);
                    } catch (java.util.concurrent.TimeoutException ex) {
                        f.cancel(true);
                        System.out.println("Future Exception CancellationException!!!");
                    }
                    if (res != null)
                        result.add(res);
                }
            } catch (InterruptedException | ExecutionException ex) {
                System.out.println("Exception=" + ex);
            }
        }

        return result;
    }

    private String[] countersTestingLink(String[] link, Map<String, String[]> node_community_version_map) {
        String[] result = null;

//        logger.Println("Start counters testing link: " + link[0] + "," + link[1] + "," + link[2] + "," + link[3] + "," + link[4] + "," + link[5], logger.DEBUG);
        String node1 = link[0];
        String id1 = link[1];
        String id_alt1 = link[2];
        String node2 = link[3];
        String id2 = link[4];
        String id_alt2 = link[5];
        String[] node_comm_ver1 = node_community_version_map.get(node1);
        String[] node_comm_ver2 = node_community_version_map.get(node2);
        if (node_comm_ver1 != null && node_comm_ver2 != null) {
            String community1 = node_comm_ver1[1];
            int version1 = Integer.parseInt(node_comm_ver1[2]);
            String community2 = node_comm_ver2[1];
            int version2 = Integer.parseInt(node_comm_ver2[2]);

            long start_time = System.currentTimeMillis();
            // input
            String in1 = getInCountersTesting(node1, community1, version1, id1, id_alt1);
            String in2 = getInCountersTesting(node2, community2, version2, id2, id_alt2);
            // output
            String out1 = getOutCountersTesting(node1, community1, version1, id1, id_alt1);
            String out2 = getOutCountersTesting(node2, community2, version2, id2, id_alt2);
            long stop_time = System.currentTimeMillis();
            if (in1 != null && in2 != null && out1 != null && out2 != null) {
                result = new String[11];
                result[0] = link[0];
                result[1] = link[1];
                result[2] = link[2];
                result[3] = link[3];
                result[4] = link[4];
                result[5] = link[5];
                result[6] = in1;
                result[7] = out1;
                result[8] = in2;
                result[9] = out2;
                result[10] = String.valueOf(stop_time - start_time);
//                logger.Println("Stop counters testing link OK: " + link[0] + "," + link[1] + "," + link[2] + "," + link[3] + "," + link[4] + "," + link[5], logger.DEBUG);
            } else {
                logger.Println("Stop counters testing link ERR!!!: " + link[0] + "," + link[1] + "," + link[2] + "," + link[3] + "," + link[4] + "," + link[5] + "\nin1=" + in1 + ", out1=" + out1 + ", in2=" + in2 + ", out2=" + out2, logger.DEBUG);
            }
        } else {
            if (node_comm_ver1 == null)
                logger.Println("ERR!!! node_comm_ver: node1=" + node1 + " - null", logger.DEBUG);
            if (node_comm_ver2 == null)
                logger.Println("ERR!!! node_comm_ver: node2=" + node2 + " - null", logger.DEBUG);

        }
//        try { Thread.sleep(1000); } catch (InterruptedException ex) {}
        return result;
    }

    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))
    public ArrayList<String[]> calculateTestingLinks(ArrayList<String[]> testingLinks_start, ArrayList<String[]> testingLinks_stop, double precession_limit) {
        ArrayList<String[]> result = new ArrayList();

        Map<String, String[]> testingLinks_stop_map = new HashMap();
        for (String[] item : testingLinks_stop) {
            String key = item[0] + " " + item[1] + " " + item[3] + " " + item[4];
            testingLinks_stop_map.put(key, item);
        }

        for (String[] item : testingLinks_start) {
            String key = item[0] + " " + item[1] + " " + item[3] + " " + item[4];
            if (testingLinks_stop_map.get(key) != null) {
                String[] item1 = testingLinks_stop_map.get(key);
                String[] in1_start = item[6].split(",");
                String[] out1_start = item[7].split(",");
                String[] in2_start = item[8].split(",");
                String[] out2_start = item[9].split(",");
                String[] in1_stop = item1[6].split(",");
                String[] out1_stop = item1[7].split(",");
                String[] in2_stop = item1[8].split(",");
                String[] out2_stop = item1[9].split(",");
                double delta_inUcast1 = delta(Double.parseDouble(in1_start[0]), Double.parseDouble(in1_stop[0]));
                double delta_inMulticast1 = delta(Double.parseDouble(in1_start[1]), Double.parseDouble(in1_stop[1]));
                double delta_inBroadcast1 = delta(Double.parseDouble(in1_start[2]), Double.parseDouble(in1_stop[2]));
                double delta_inNUcast1 = delta(Double.parseDouble(in1_start[3]), Double.parseDouble(in1_stop[3]));
                double delta_in_discards1 = delta(Double.parseDouble(in1_start[4]), Double.parseDouble(in1_stop[4]));
                double delta_in_errors1 = delta(Double.parseDouble(in1_start[5]), Double.parseDouble(in1_stop[5]));

                double delta_outUcast1 = delta(Double.parseDouble(out1_start[0]), Double.parseDouble(out1_stop[0]));
                double delta_outMulticast1 = delta(Double.parseDouble(out1_start[1]), Double.parseDouble(out1_stop[1]));
                double delta_outBroadcast1 = delta(Double.parseDouble(out1_start[2]), Double.parseDouble(out1_stop[2]));
                double delta_outNUcast1 = delta(Double.parseDouble(out1_start[3]), Double.parseDouble(out1_stop[3]));
                double delta_out_discards1 = delta(Double.parseDouble(out1_start[4]), Double.parseDouble(out1_stop[4]));
                double delta_out_errors1 = delta(Double.parseDouble(out1_start[5]), Double.parseDouble(out1_stop[5]));

                double delta_inUcast2 = delta(Double.parseDouble(in2_start[0]), Double.parseDouble(in2_stop[0]));
                double delta_inMulticast2 = delta(Double.parseDouble(in2_start[1]), Double.parseDouble(in2_stop[1]));
                double delta_inBroadcast2 = delta(Double.parseDouble(in2_start[2]), Double.parseDouble(in2_stop[2]));
                double delta_inNUcast2 = delta(Double.parseDouble(in2_start[3]), Double.parseDouble(in2_stop[3]));
                double delta_in_discards2 = delta(Double.parseDouble(in2_start[4]), Double.parseDouble(in2_stop[4]));
                double delta_in_errors2 = delta(Double.parseDouble(in2_start[5]), Double.parseDouble(in2_stop[5]));

                double delta_outUcast2 = delta(Double.parseDouble(out2_start[0]), Double.parseDouble(out2_stop[0]));
                double delta_outMulticast2 = delta(Double.parseDouble(out2_start[1]), Double.parseDouble(out2_stop[1]));
                double delta_outBroadcast2 = delta(Double.parseDouble(out2_start[2]), Double.parseDouble(out2_stop[2]));
                double delta_outNUcast2 = delta(Double.parseDouble(out2_start[3]), Double.parseDouble(out2_stop[3]));
                double delta_out_discards2 = delta(Double.parseDouble(out2_start[4]), Double.parseDouble(out2_stop[4]));
                double delta_out_errors2 = delta(Double.parseDouble(out2_start[5]), Double.parseDouble(out2_stop[5]));

                double delta_in1;
                if (delta_inNUcast1 > 0)
                    delta_in1 = delta_inUcast1 + delta_inNUcast1 + delta_in_discards1 + delta_in_errors1;
                else
                    delta_in1 = delta_inUcast1 + delta_inMulticast1 + delta_inBroadcast1 + delta_in_discards1 + delta_in_errors1;

                double delta_out1;
                if (delta_outNUcast1 > 0)
                    delta_out1 = delta_outUcast1 + delta_outNUcast1 + delta_out_discards1 + delta_out_errors1;
                else
                    delta_out1 = delta_outUcast1 + delta_outMulticast1 + delta_outBroadcast1 + delta_out_discards1 + delta_out_errors1;

                if (delta_in1 == 0 && delta_out1 == 0) {
                    double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                    delta_in1 = rnd1;
                    double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                    delta_out1 = rnd2;
                }

                double delta_in2;
                if (delta_inNUcast2 > 0)
                    delta_in2 = delta_inUcast2 + delta_inNUcast2 + delta_in_discards2 + delta_in_errors2;
                else
                    delta_in2 = delta_inUcast2 + delta_inMulticast2 + delta_inBroadcast2 + delta_in_discards2 + delta_in_errors2;

                double delta_out2;
                if (delta_outNUcast2 > 0)
                    delta_out2 = delta_outUcast2 + delta_outNUcast2 + delta_out_discards2 + delta_out_errors2;
                else
                    delta_out2 = delta_outUcast2 + delta_outMulticast2 + delta_outBroadcast2 + delta_out_discards2 + delta_out_errors2;

                if (delta_in2 == 0 && delta_out2 == 0) {
                    double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                    delta_in2 = rnd1;
                    double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                    delta_out2 = rnd2;
                }

                if (delta_in1 > 0 && delta_out1 > 0 && delta_in2 > 0 && delta_out2 > 0) {
                    double epsilon1 = 0;
                    if (delta_in1 + delta_out2 > 0) {
                        epsilon1 = 2 * abs(delta_in1 - delta_out2) / (delta_in1 + delta_out2);
                    }
                    double epsilon2 = 0;
                    if (delta_in2 + delta_out1 > 0) {
                        epsilon2 = 2 * abs(delta_in2 - delta_out1) / (delta_in2 + delta_out1);
                    }
                    double epsilon = (epsilon1 + epsilon2) / 2;
                    if (epsilon == 0) {
                        epsilon = Math.abs(Math.random() * Math.pow(2, 32));
                    }

//                        float precession_limit=(float)((delta_time+time_lag)/wait_time);
                    if (epsilon < precession_limit / 2) {
                        String[] mas = new String[8];
                        mas[0] = item[0];
                        mas[1] = item[1];
                        mas[2] = item[2];
                        mas[3] = item[3];
                        mas[4] = item[4];
                        mas[5] = item[5];
                        mas[6] = String.valueOf(epsilon);
                        mas[7] = String.valueOf(precession_limit / 2);
                        logger.Println("CalculateTestingLink: " + mas[0] + "," + mas[1] + "," + mas[2] + "," + mas[3] + "," + mas[4] + "," + mas[5] + "," + mas[6] + "," + mas[7], logger.DEBUG);
                        result.add(mas);
                    } else {
                        // alternative calculete without discards and errors packets
                        ////////////////////////////////////////////////////////////
                        if (delta_inNUcast1 > 0)
                            delta_in1 = delta_inUcast1 + delta_inNUcast1;
                        else
                            delta_in1 = delta_inUcast1 + delta_inMulticast1 + delta_inBroadcast1;

                        if (delta_outNUcast1 > 0)
                            delta_out1 = delta_outUcast1 + delta_outNUcast1;
                        else
                            delta_out1 = delta_outUcast1 + delta_outMulticast1 + delta_outBroadcast1;

                        if (delta_in1 == 0 && delta_out1 == 0) {
                            double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                            delta_in1 = rnd1;
                            double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                            delta_out1 = rnd2;
                        }

                        if (delta_inNUcast2 > 0)
                            delta_in2 = delta_inUcast2 + delta_inNUcast2;
                        else
                            delta_in2 = delta_inUcast2 + delta_inMulticast2 + delta_inBroadcast2;

                        if (delta_outNUcast2 > 0)
                            delta_out2 = delta_outUcast2 + delta_outNUcast2;
                        else
                            delta_out2 = delta_outUcast2 + delta_outMulticast2 + delta_outBroadcast2;

                        if (delta_in2 == 0 && delta_out2 == 0) {
                            double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                            delta_in2 = rnd1;
                            double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                            delta_out2 = rnd2;
                        }

                        if (delta_in1 > 0 && delta_out1 > 0 && delta_in2 > 0 && delta_out2 > 0) {
                            epsilon1 = 0;
                            if (delta_in1 + delta_out2 > 0) {
                                epsilon1 = 2 * abs(delta_in1 - delta_out2) / (delta_in1 + delta_out2);
                            }
                            epsilon2 = 0;
                            if (delta_in2 + delta_out1 > 0) {
                                epsilon2 = 2 * abs(delta_in2 - delta_out1) / (delta_in2 + delta_out1);
                            }
                            epsilon = (epsilon1 + epsilon2) / 2;
                            if (epsilon == 0) {
                                epsilon = Math.abs(Math.random() * Math.pow(2, 32));
                            }

                            //                        float precession_limit=(float)((delta_time+time_lag)/wait_time);
                            if (epsilon < precession_limit / 2) {
                                String[] mas = new String[8];
                                mas[0] = item[0];
                                mas[1] = item[1];
                                mas[2] = item[2];
                                mas[3] = item[3];
                                mas[4] = item[4];
                                mas[5] = item[5];
                                mas[6] = String.valueOf(epsilon);
                                mas[7] = String.valueOf(precession_limit / 2);
                                logger.Println("CalculateTestingLink: " + mas[0] + "," + mas[1] + "," + mas[2] + "," + mas[3] + "," + mas[4] + "," + mas[5] + "," + mas[6] + "," + mas[7], logger.DEBUG);
                                result.add(mas);
                            } else {
                                // alternative calculete without discards and errors packets
                                ////////////////////////////////////////////////////////////
                                if (delta_inMulticast1 + delta_inBroadcast1 + delta_inNUcast1 == 0) {
                                    delta_in1 = delta_inUcast1;
                                    delta_out1 = delta_outUcast1;
                                    if (delta_in1 == 0 && delta_out1 == 0) {
                                        double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                                        delta_in1 = rnd1;
                                        double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                                        delta_out1 = rnd2;
                                    }
                                } else {
                                    double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                                    delta_in1 = rnd1;
                                    double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                                    delta_out1 = rnd2;
                                }
                                if (delta_inMulticast2 + delta_inBroadcast2 + delta_inNUcast2 == 0) {
                                    delta_in2 = delta_inUcast2;
                                    delta_out2 = delta_outUcast2;

                                    if (delta_in2 == 0 && delta_out2 == 0) {
                                        double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                                        delta_in2 = rnd1;
                                        double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                                        delta_out2 = rnd2;
                                    }
                                } else {
                                    double rnd1 = Math.abs(Math.random() * Math.pow(2, 64));
                                    delta_in2 = rnd1;
                                    double rnd2 = Math.abs(Math.random() * Math.pow(2, 64));
                                    delta_out2 = rnd2;
                                }

                                if (delta_in1 > 0 && delta_out1 > 0 && delta_in2 > 0 && delta_out2 > 0) {
                                    epsilon1 = 0;
                                    if (delta_in1 + delta_out2 > 0) {
                                        epsilon1 = 2 * abs(delta_in1 - delta_out2) / (delta_in1 + delta_out2);
                                    }
                                    epsilon2 = 0;
                                    if (delta_in2 + delta_out1 > 0) {
                                        epsilon2 = 2 * abs(delta_in2 - delta_out1) / (delta_in2 + delta_out1);
                                    }
                                    epsilon = (epsilon1 + epsilon2) / 2;
                                    if (epsilon == 0) {
                                        epsilon = Math.abs(Math.random() * Math.pow(2, 32));
                                    }

                                    //                        float precession_limit=(float)((delta_time+time_lag)/wait_time);
                                    if (epsilon < precession_limit / 2) {
                                        String[] mas = new String[8];
                                        mas[0] = item[0];
                                        mas[1] = item[1];
                                        mas[2] = item[2];
                                        mas[3] = item[3];
                                        mas[4] = item[4];
                                        mas[5] = item[5];
                                        mas[6] = String.valueOf(epsilon);
                                        mas[7] = String.valueOf(precession_limit / 2);
                                        logger.Println("CalculateTestingLink: " + mas[0] + "," + mas[1] + "," + mas[2] + "," + mas[3] + "," + mas[4] + "," + mas[5] + "," + mas[6] + "," + mas[7], logger.DEBUG);
                                        result.add(mas);
                                    } else {
                                        logger.Println("CalculateTestingLink exclude: " + item[0] + "," + item[1] + "," + item[2] + "," + item[3] + "," + item[4] + "," + item[5] + "," + epsilon + "," + precession_limit, logger.DEBUG);
                                    }
                                } else {
                                    logger.Println("CalculateTestingLink not streams packets: " + item[0] + "," + item[1] + "," + item[2] + "," + item[3] + "," + item[4] + "," + item[5], logger.DEBUG);
                                }
                                ////////////////////////////////////////////////////////////                                
                            }
                        } else {
                            logger.Println("CalculateTestingLink not streams packets: " + item[0] + "," + item[1] + "," + item[2] + "," + item[3] + "," + item[4] + "," + item[5], logger.DEBUG);
                        }
                        ////////////////////////////////////////////////////////////
                    }
                } else {
                    logger.Println("CalculateTestingLink not streams packets: " + item[0] + "," + item[1] + "," + item[2] + "," + item[3] + "," + item[4] + "," + item[5], logger.DEBUG);
                }

            }
        }

        return result;
    }

    @SuppressWarnings("SleepWhileInLoop")
    public void waiting(long wait_timeout) {
        long start_time = System.currentTimeMillis();
        StringBuilder backspace = new StringBuilder();
        if (wait_timeout > 0) {
            while (true) {
                long stop_time = System.currentTimeMillis();
                System.out.print(backspace);
                String out = "Elapsed time : " + (stop_time - start_time) / 1000 + " sec.          From : " + wait_timeout + " sec.";
                System.out.print(out);
                backspace = new StringBuilder();
                backspace.append("\b".repeat(out.length()));
                if ((stop_time - start_time) / 1000 > wait_timeout) {
                    System.out.println();
                    break;
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

//    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))
//    public ArrayList<String[]> summaryLinks(ArrayList<String[]> links, ArrayList<String[]> dplinks, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String[]> result = new ArrayList();
//
//        for (String[] item : dplinks) {
//            String[] mas = new String[7];
//            mas[0] = item[0];
//            mas[1] = item[1];
//            mas[2] = item[2];
//            mas[3] = item[3];
//            mas[4] = item[4];
//            mas[5] = item[5];
//            mas[6] = "dp_link";
//            result.add(mas);
//        }
//
//        Map<String, ArrayList<String>> nodes_ip_address = getIpAddress(walkInformationFromNodes);
//        for (String[] item : links) {
//            if (checkDuplicateLink(item, result, nodes_ip_address)) {
//                String[] mas = new String[7];
//                mas[0] = item[0];
//                mas[1] = item[1];
//                mas[2] = item[2];
//                mas[3] = item[3];
//                mas[4] = item[4];
//                mas[5] = item[5];
//                mas[6] = "calc_link";
//                result.add(mas);
////                System.out.println("Link adding from links: "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5]);
//            }
//        }
//
//        for (String[] item : result) {
//            logger.Println("Start merging dplinks ... " + item[0] + "," + item[1] + "," + item[2] + " <---> " + item[3] + "," + item[4] + "," + item[5] + " --- " + item[6], logger.DEBUG);
//        }
//
//        return result;
//    }

    public String translateHexString_to_SymbolString(String str) {
        String result = str;

        if (str.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
            String[] fields = str.split("[:-]");
            if (fields.length > 1) {
                StringBuilder out = new StringBuilder();
                for (String octet : fields) {
                    int dec = Integer.parseInt(octet, 16);
                    if (dec == 0) {
                        out.append("NUL");
                    } else if (dec == 63 || dec < 32 || dec > 126) {
                        out = new StringBuilder();
                        break;
                    } else {
                        out.append((char) dec);
                    }
                }
                if (out.isEmpty())
                    return str;
                else
                    result = out.toString();
            }
        }
        return result;
    }

    //  output: id_iface, iface_name
//    private String[] SearchInterfaceName(String interface_name, ArrayList<String[]> list_interface) {
//        String[] result = new String[2];
//
//        if(interface_name != null) {
//            interface_name = ReplaceDelimiter(TranslateHexString_to_SymbolString(interface_name));
//
//            Pattern p = Pattern.compile("^([A-Za-z\\s]+)[\\s_-]*(\\d+[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*)$");
//            Matcher m = p.matcher(interface_name.toLowerCase());
//            if(m.find()){  
//                String start=m.group(1);
//                String stop=m.group(2);
//                Pattern p1 = Pattern.compile("^"+start+"[\\w\\s]*"+stop+"$");
//                if(list_interface != null) {
//                    for(String[] iter : list_interface) {
//                        Matcher m1 = p1.matcher(iter[1].toLowerCase());
//                        if(m1.matches()) {
//                            result[0]=iter[0].split("\\.")[iter[0].split("\\.").length - 1];
//                            result[1]=iter[1];
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        
//        return result;
//    }
    public boolean checkInterfaceName(String interface_name, String interface_name_sec) {
        try {
            if (interface_name != null && interface_name_sec != null) {
                interface_name = interface_name.replace("_", " ");
                interface_name_sec = interface_name_sec.replace("_", " ");
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

                if (!interface_name.equals(interface_name_sec)) {
                    String iface;
                    String iface_long;
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

    // Output format: ArrayList
    //    mac --> ip, node, if_iface, name_iface
    public ArrayList<String[]> calculateARPMAC(ArrayList<String[]> ARP_MAC, ArrayList<ArrayList<String>> links, Map<String, Map> nodes_information, Map node_protocol_accounts) {
        ArrayList<String[]> result = new ArrayList();
//        ArrayList<String> nodes = new ArrayList();
        ArrayList<String> mac = new ArrayList();
        ArrayList<String> ip = new ArrayList();
        ArrayList<String[]> ARP_MAC_NEW = new ArrayList(ARP_MAC);

        // get nodes unknown
        ArrayList<String> nodes_unknown = new ArrayList();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            if (node_protocol_accounts.get(node) == null) {
                nodes_unknown.add(node);
            }
            Map<String, Map> val = entry.getValue();
            ArrayList<String> mac_list = getMACFromNode(val);
            mac.addAll(mac_list);
            ArrayList<String> ip_list = getIpListFromNode(val);
            ip.addAll(ip_list);
        }

        // get links to unknown nodes
        ArrayList<ArrayList<String>> links_to_unknown_nodes = new ArrayList();
        ArrayList<ArrayList<String>> links_to_known_nodes = new ArrayList();
        for (ArrayList<String> item : links) {
            boolean find = false;
            for (String node : nodes_unknown) {
                if (item.get(3).equals(node)) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                links_to_known_nodes.add(item);
            } else {
                links_to_unknown_nodes.add(item);
            }
        }

        // ARP_MAC to HashMap
        Map<String, String[]> node_ARP_MAC = new HashMap();
        for (String[] item : ARP_MAC) {
            node_ARP_MAC.put(item[0], item);
        }

        // get nodes without MAC address
        ArrayList<String> nodes_without_mac = new ArrayList();
        for (Map.Entry<String, ArrayList<String>> entry : ((Map<String, ArrayList<String>>) node_protocol_accounts).entrySet()) {
            String node = entry.getKey();
            if (node_ARP_MAC.get(node) == null) {
                nodes_without_mac.add(node);
            }
        }

        // get links to nodes without MAC
        Map<String, ArrayList<String>> node_iface_without_mac_nodes = new HashMap();
        for (String node : nodes_without_mac) {
            for (ArrayList<String> item : links) {
                if (item.get(0).equals(node) || item.get(3).equals(node)) {
                    String node_iface1 = (item.get(0) + " " + item.get(2)).replaceAll("_", " ");
                    String node_iface2 = (item.get(3) + " " + item.get(5)).replaceAll("_", " ");
                    node_iface_without_mac_nodes.put(node_iface1, item);
                    node_iface_without_mac_nodes.put(node_iface2, item);
                }
            }
        }

//        WriteArrayListToFile1("links_to_known_nodes", links_to_known_nodes);
//        WriteArrayListToFile1("links_to_unknown_nodes", links_to_unknown_nodes);
//////////////////////////////////////////////////////////////////////////////  
        // indexing node_iface
        Map<String, ArrayList<Integer>> node_iface_number = new HashMap();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = (item[0] + " " + item[2]).replaceAll("_", " ");
            if (node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            } else {
                node_iface_number.get(node_iface).add(i);
            }
        }

        Map<Integer, Integer> remove_numbers_links = new HashMap();
        for (ArrayList<String> item : links_to_known_nodes) {
            String node_iface = (item.get(0) + " " + item.get(2)).replaceAll("_", " ");
            if (node_iface_without_mac_nodes.get(node_iface) == null) {
                ArrayList<Integer> number_list = node_iface_number.get(node_iface);
                if (number_list != null) {
                    for (Integer num : number_list) {
                        remove_numbers_links.put(num, num);
                    }
                }
            }

            node_iface = (item.get(3) + " " + item.get(5)).replaceAll("_", " ");
            if (node_iface_without_mac_nodes.get(node_iface) == null) {
                ArrayList<Integer> number_list = node_iface_number.get(node_iface);
                if (number_list != null) {
                    for (Integer num : number_list) {
                        remove_numbers_links.put(num, num);
                    }
                }
            }

        }

        // removes ARP_mac records if is am link
        ArrayList<String[]> ARP_MAC_tmp = new ArrayList();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if (remove_numbers_links.get(i) == null) {
                ARP_MAC_tmp.add(item);
            }
        }

        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();

////////////////////////////////////////////////////////////////  
//         indexing node_iface
        node_iface_number = new HashMap();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = (item[0] + " " + item[2]).replaceAll("_", " ");
            if (node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            } else {
                node_iface_number.get(node_iface).add(i);
            }
        }

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        Map<Integer, ArrayList<String>> update_numbers_links_to_unknown_nodes = new HashMap();
        for (ArrayList<String> item : links_to_unknown_nodes) {
            String node_iface = (item.get(0) + " " + item.get(2)).replaceAll("_", " ");
            ArrayList<Integer> number_list = node_iface_number.get(node_iface);
            if (number_list != null) {
                for (Integer num : number_list) {
                    update_numbers_links_to_unknown_nodes.put(num, item);
                }
            }
        }

//        // moves mac ip address to CDP and LLDP nodes.
//        for (Map.Entry<Integer, ArrayList<String>> entry : update_numbers_links_to_unknown_nodes.entrySet()) {
//            String[] item = ARP_MAC_NEW.get(entry.getKey());
//            ArrayList<String> item1 = entry.getValue();
//            String[] mas = new String[5];
//            mas[0] = item1.get(3);
//            mas[1] = "unknown";
//            mas[2] = "unknown";
//            mas[3] = item[3];
//            mas[4] = item[4];
//            ARP_MAC_NEW.set(entry.getKey(), mas);
//        }
////////////////////////////////////////////////  

        // indexing mac
        Map<String, ArrayList<Integer>> MAC_number = new HashMap();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if (MAC_number.get(item[4]) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                MAC_number.put(item[4], number_list);
            } else {
                MAC_number.get(item[4]).add(i);
            }
        }

        Map<Integer, Integer> remove_numbers = new HashMap();
        for (String item : mac) {
            ArrayList<Integer> number_list = MAC_number.get(item);
            if (number_list != null) {
                for (Integer num : number_list) {
                    remove_numbers.put(num, num);
                }
            }
        }

        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if (remove_numbers.get(i) == null) {
                ARP_MAC_tmp.add(item);
            }
        }
        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();
//////////////////////////////////////////

        // indexing ip
        Map<String, ArrayList<Integer>> IP_number = new HashMap();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if (IP_number.get(item[3]) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                IP_number.put(item[3], number_list);
            } else {
                IP_number.get(item[3]).add(i);
            }
        }

        remove_numbers = new HashMap();
        for (String item : ip) {
            ArrayList<Integer> number_list = IP_number.get(item);
            if (number_list != null) {
                for (Integer num : number_list) {
                    remove_numbers.put(num, num);
                }
            }
        }

        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if (remove_numbers.get(i) == null) {
                ARP_MAC_tmp.add(item);
            }
        }
        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();

////////////////////////////////////////// 
        // indexing node_iface
        node_iface_number = new HashMap();
        for (int i = 0; i < ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = (item[0] + " " + item[2]).replaceAll("_", " ");
            if (node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            } else {
                node_iface_number.get(node_iface).add(i);
            }
        }

        ArrayList<String[]> mac_ip_node_ifaceid_ifacename_nummacforport = new ArrayList();
        for (String[] item : ARP_MAC_NEW) {
            String node_iface = (item[0] + " " + item[2]).replaceAll("_", " ");
            int num_mac = 0;
            if (node_iface_number.get(node_iface) != null) {
                num_mac = node_iface_number.get(node_iface).size();
            }
            String[] mas = new String[6];
            mas[0] = item[4];
            mas[1] = item[3];
            mas[2] = item[0];
            mas[3] = item[1];
            mas[4] = item[2];
            mas[5] = String.valueOf(num_mac);
            mac_ip_node_ifaceid_ifacename_nummacforport.add(mas);
//            System.out.println("+++ "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]);
        }

        // indexing mac
        Map<String, ArrayList<String[]>> MAC_ip_node_ifaceid_ifacename_num = new HashMap();
        for (String[] item : mac_ip_node_ifaceid_ifacename_nummacforport) {
            if (MAC_ip_node_ifaceid_ifacename_num.get(item[0]) == null) {
                ArrayList<String[]> list = new ArrayList();
                list.add(item);
                MAC_ip_node_ifaceid_ifacename_num.put(item[0], list);
            } else {
                MAC_ip_node_ifaceid_ifacename_num.get(item[0]).add(item);
            }
        }

        // if mac clients exist in node name - remove from result
        Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
        Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4}))");
        ArrayList<String> mackey_delete_list = new ArrayList();
        for (Map.Entry<String, ArrayList<String[]>> entry : MAC_ip_node_ifaceid_ifacename_num.entrySet()) {
            String mac_addr = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            for (String[] item : val) {
                Matcher m_mac1 = p1.matcher(item[2]);
                Matcher m_mac2 = p2.matcher(item[2]);
                String mac_address = null;
                if (m_mac1.find()) {
                    mac_address = m_mac1.group(1).replaceAll("[:.-]", "").toLowerCase();
                } else if (m_mac2.find()) {
                    mac_address = m_mac2.group(1).replaceAll("[:.-]", "").toLowerCase();
                }
                String mac_addr_short = mac_addr.replaceAll("[:.-]", "");
                if (mac_addr_short != null && mac_addr_short.equals(mac_address)) {
                    mackey_delete_list.add(mac_addr);
                }
            }
        }

        for (String item : mackey_delete_list) {
            MAC_ip_node_ifaceid_ifacename_num.remove(item);
        }

        for (Map.Entry<String, ArrayList<String[]>> entry : MAC_ip_node_ifaceid_ifacename_num.entrySet()) {
            String mac_addr = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            if (!mac_addr.equals("unknown_mac")) {
                String[] min_item = new String[6];
                min_item[5] = String.valueOf(2137483647);
                for (String[] item : val) {
                    if (Integer.parseInt(item[5]) < Integer.parseInt(min_item[5])) {
                        min_item[0] = item[0];
                        min_item[1] = item[1];
                        min_item[2] = item[2];
                        min_item[3] = item[3];
                        min_item[4] = item[4];
                        min_item[5] = item[5];
                    }
                }
                if (min_item[0] != null) {
                    result.add(min_item);
                }
            } else {
                result.addAll(val);
            }
        }

        return result;
    }

    //    // Output format: ArrayList
//    //    mac --> ip, node, if_iface, name_iface
//    public void CalculateARPMAC() {        
//        ArrayList<String[]> result = new ArrayList();
//        ArrayList<String> nodes = new ArrayList();
//        ArrayList<String> mac = new ArrayList();
//        ArrayList<String> ip = new ArrayList();  
//        
//      
//        ArrayList<String[]> ARP_MAC = new ArrayList();
//        ArrayList<String[]> added_from_exclude_nodes_links = new ArrayList();
//        ArrayList<String[]> links = new ArrayList();
//        Map<String, String> informationFromNodes = new HashMap<>();
//        
//
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("ARP_MAC"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    ARP_MAC.add(s.split(","));
////                    System.out.println(s);
//                }
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("added_from_exclude_nodes_links"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    added_from_exclude_nodes_links.add(s.split(","));
////                    System.out.println(s);
//                }
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("links"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    links.add(s.split(","));
//                }
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("info_nodes1"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] mas=s.split(";", -1);
//                    String str = mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5]+";"+mas[6]+";"+mas[7]+";"+mas[8]+";"+mas[9]+";"+mas[10];
//                    informationFromNodes.put(mas[0], str);
////                    System.out.println(s);
//                }
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        
///////////////////////////////////////////////////////////////////
//
//        // adding ARP_MAC table from added_from_exclude_nodes_links
//        for(String[] item : added_from_exclude_nodes_links) {
//            boolean find=false;
//            for(String[] item1 : ARP_MAC) {
//                if(item1[3].equals(item[3])) {
//                    find=true;
//                    break;
//                }
//            }
//            if(!find) ARP_MAC.add(item);
//        }
////        ARP_MAC.addAll(added_from_exclude_nodes_links);
//
//        ArrayList<String> nodes_unknown = new ArrayList();
//        for (Map.Entry<String, String> entry : informationFromNodes.entrySet()) {
//            String node = entry.getKey();
//            String val = entry.getValue();
//            
////            System.out.println("CalculateARPMAC: "+node+";"+val);
//            String[] mas=val.split(";", -1);
//            nodes.add(node);
//            if(mas[0].split("\\|").length != 3) {
//                nodes_unknown.add(node);
//            }
//            
//            
//            if(mas.length == 10) {
//                if(!mas[7].equals("")) {
//                    String[] mas1=mas[7].split("\\|");
//                    for(String item : mas1) {
//                        String[] mas2=item.split(",", -1);
//                        if(mas2.length == 11) {
//                            if(!mas2[5].equals("")) {
//                                mac.add(mas2[5]);
//                            }
//                            if(!mas2[6].equals("")) {
//                                String ipaddr=mas2[6].split("/")[0];
//                                if(!ipaddr.equals("127.0.0.1")) ip.add(ipaddr);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // get links to unknown nodes
//        ArrayList<String[]> links_to_unknown_nodes = new ArrayList();
//        for(String[] item : links) {
//            for(String node : nodes_unknown) {
//                if(item[3].equals(node.split(" ")[0])) {
//                    links_to_unknown_nodes.add(item);
//                    break;
//                }
//            }
//        }
//         
//        // moves mac ip address to CDP and LLDP nodes.
//        for(int i=0; i<ARP_MAC.size(); i++) {
//            String[] item = ARP_MAC.get(i);
//            for(String[] item1 : links_to_unknown_nodes) {
//                if(item1[0].equals(item[0]) && item1[2].equals(item[2])) {
//                    String[] mas = new String[5];
//                    mas[0]=item1[3]; mas[1]="unknown"; mas[2]="unknown";
//                    mas[3]=item[3]; mas[4]=item[4];
//                    if(mas[0].equals(mas[3])) {
//                        ARP_MAC.remove(i);
//                    } else {
//                        ARP_MAC.set(i, mas);
//                    }
//                    break;
//                }
//            }
//        }
//        
//        ArrayList<String[]> ARP_MAC_tmp = new ArrayList();
//        for(String[] item : ARP_MAC) {
//            boolean found=false;
//            for(String[] item1 : links) {
//                if( (item[0].equals(item1[0]) && CheckInterfaceName(item[2], item1[2])) || (item[0].equals(item1[3]) && CheckInterfaceName(item[2], item1[5])) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp.add(item);
//            }
//        }
//        
//        ArrayList<String[]> ARP_MAC_tmp1 = new ArrayList();
//        for(String[] item : ARP_MAC_tmp) {
//            boolean found=false;
//            for(String item1 : mac) {
//                if( item[4].equals(item1) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp1.add(item);
//            }
//        }        
//        
//        ArrayList<String[]> ARP_MAC_tmp2 = new ArrayList();
//        for(String[] item : ARP_MAC_tmp1) {
//            boolean found=false;
//            for(String item1 : ip) {
//                if( item[3].equals(item1) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp2.add(item);
//            }
//        } 
//        
//        ArrayList<String[]> ARP_MAC_userports = new ArrayList();
//        for(String[] item : ARP_MAC_tmp2) {
//            boolean found=false;
//            for(String node : nodes) {
//                if( item[3].equals(node)) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_userports.add(item);
////                System.out.println("### "+item[0]+","+item[1]+","+item[2]+","+item[3]+","+item[4]);
//            }
//        }
//        
//        
//        
//        ArrayList<String[]> mac_ip_node_ifaceid_ifacename_nummacforport = new ArrayList();
//        for(String[] item : ARP_MAC_userports) {
//            int num_mac=0;
//            for(String[] item1 : ARP_MAC_userports) {
//                if(item[0].equals(item1[0]) && item[2].equals(item1[2]) ) {
//                    num_mac++;
//                }
//            }
//            String[] mas = new String[6];
//            mas[0]=item[4];
//            mas[1]=item[3];
//            mas[2]=item[0];
//            mas[3]=item[1];
//            mas[4]=item[2];
//            mas[5]=String.valueOf(num_mac);
//            mac_ip_node_ifaceid_ifacename_nummacforport.add(mas);
////            System.out.println("+++ "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]);
//        }
//
////        ArrayList<String[]> result = new ArrayList();
//        for(int pos=0; pos<mac_ip_node_ifaceid_ifacename_nummacforport.size(); pos++) {
//            String[] item=mac_ip_node_ifaceid_ifacename_nummacforport.get(pos);
//            if(!item[0].equals("unknown_mac")) {
//                String[] item_min = item.clone();
//                int num_min=Integer.parseInt(item[5]);
//                for(int j=pos+1; j<mac_ip_node_ifaceid_ifacename_nummacforport.size(); j++) {
//                    String[] item1=mac_ip_node_ifaceid_ifacename_nummacforport.get(j);
//                    if( item[0].equals(item1[0])) {
//                        if(Integer.parseInt(item1[5]) < num_min) {
//                            item_min = item1.clone();
//                            num_min=Integer.parseInt(item1[5]);
//                        }
//                        mac_ip_node_ifaceid_ifacename_nummacforport.remove(j);
//                        j--;
//                    }
//                }
//                if( !item_min[1].equals(item_min[2]) ) {
////                    System.out.println("--- "+item_min[0]+","+item_min[1]+","+item_min[2]+","+item_min[3]+","+item_min[4]+","+item_min[5]);
//                    result.add(item_min);
//                }
//            } else {
////                System.out.println("--- "+item[0]+","+item[1]+","+item[2]+","+item[3]+","+item[4]+","+item[5]);
//                result.add(item);                
//            }       
//        }
//
//    }
    private String[] getIfaceName(String node, String id_iface, Map<String, Map<String, Map<String, String>>> indexingInterfacesFromNodes, boolean with_interface_maping) {
        String[] result = new String[2];
        result[0] = "";
        result[1] = "";

        if (with_interface_maping) {
            Map<String, Map<String, String>> ifaceMaping = indexingInterfacesFromNodes.get("IfaceMaping");
            if (ifaceMaping.get(node) != null && ifaceMaping.get(node).get(id_iface) != null) {
                id_iface = ifaceMaping.get(node).get(id_iface);
            }
        }

        Map<String, Map<String, String>> ifDescr = indexingInterfacesFromNodes.get("ifDescr");
        if (ifDescr != null) {
            if (ifDescr.get(node) != null && ifDescr.get(node).get(id_iface) != null) {
                result[0] = id_iface;
                result[1] = replaceDelimiter(translateHexString_to_SymbolString(ifDescr.get(node).get(id_iface)));
            }
        }
        return result;
    }

    //    public ArrayList<String[]> GetNodeCommunityVersion() {
//        ArrayList<String[]> result = new ArrayList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("nodes"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] mas=s.split(",");
//                    result.add(mas);
////                    System.out.println(s);
//                }
//            } finally {
//                //РўР°РєР¶Рµ РЅРµ Р·Р°Р±С‹РІР°РµРј Р·Р°РєСЂС‹С‚СЊ С„Р°Р№Р»
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        return result;
//    }
    public LinkedList<ArrayList> sortingMinPathDeltaCounters(LinkedList<ArrayList> deltaCounters) {
        LinkedList<ArrayList> result = new LinkedList();

        int pos = 0;
        while (deltaCounters.size() > 1) {
            int first_pos = pos;
            ArrayList item = deltaCounters.get(first_pos);
            double min = Math.pow(2, 64);
            for (int i = 1; i < deltaCounters.size(); i++) {
                ArrayList item1 = deltaCounters.get(i);
                if (!item.get(0).equals(item1.get(0))) {
                    double d1 = Math.abs(2 * ((double) item.get(3) - (double) item1.get(4)) / ((double) item.get(3) + (double) item1.get(4)));
                    double d2 = Math.abs(2 * ((double) item.get(4) - (double) item1.get(3)) / ((double) item.get(4) + (double) item1.get(3)));
//                    double path = Math.pow(d1, 2 ) + Math.pow(d2, 2 );
                    double path = (d1 + d2) / 2;
                    if (path < min) {
                        min = path;
                        pos = i;
                    }
                }
            }

            if (min < Math.pow(2, 64)) {
                ArrayList list_tmp = new ArrayList();
                list_tmp.add(item.get(0));
                list_tmp.add(item.get(1));
                list_tmp.add(item.get(2));
                list_tmp.add(item.get(3));
                list_tmp.add(item.get(4));
                list_tmp.add(min);
                result.add(list_tmp);
                deltaCounters.remove(first_pos);
                pos--;
            } else {
                deltaCounters.remove(first_pos);
                pos--;
            }

        }

        return result;
    }

    //    private String[] GetRemotePort(String node_remote, String id_iface_remote, String name_iface_remote, Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList<String>> hash_ip) {
//        String[] result = new String[2];
//        result[0]=id_iface_remote;
//        result[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(name_iface_remote));
//        
////        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
//        node_remote = GetRealIpAddress(hash_ip, node_remote);
//        
//        if(walkInformationFromNodes.get("ifDescr").get(node_remote) != null) {
//            String[] mas1 = SearchInterfaceName(name_iface_remote, walkInformationFromNodes.get("ifDescr").get(node_remote));
//            if(mas1[0] != null && mas1[1] != null) {
//                result[0]=mas1[0];
//                result[1]=mas1[1];
//            } else {
//                if(name_iface_remote.matches("\\d+")) {
//                    String[] res = GetIfaceName(node_remote, name_iface_remote, walkInformationFromNodes, true);
//                    if(!res[0].equals("")) {
//                        result[0]=res[0];
//                        result[1]=res[1];
//                    } else {
//                        res = GetIfaceName(node_remote, id_iface_remote, walkInformationFromNodes, true);
//                        if(!res[0].equals("")) {
//                            result[0]=res[0];
//                            result[1]=res[1];
//                        }                        
//                    }
//                } 
//            }
//        }
//        
//        return result;
//    }
    // Output format: ArrayList
    //    ArrayList<String[]> links
    //    Map<String, String> informationFromNodes
    public ArrayList<String[]> findHub(ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        logger.Println("Starting FindHub ...", logger.INFO);
        ArrayList<String[]> result = new ArrayList();
//        result.addAll(links);

        for (int i = 0; i < links.size(); i++) {
            ArrayList<Integer> find_positions_hub_tmp = findLinksFromSomeInterface(i, links, nodes_ip_address);
            if (find_positions_hub_tmp.size() > 1) {
                ArrayList<String[]> links_hub_tmp = new ArrayList();
                ArrayList<Integer> find_positions_hub = new ArrayList();
                for (int pos : find_positions_hub_tmp) {
                    if (!find_positions_hub.contains(pos)) {
                        find_positions_hub.add(pos);
                        links_hub_tmp.add(links.get(pos));
//                        System.out.println("links_hub: "+links_tmp.get(pos)[0]+","+links_tmp.get(pos)[2]+" <---> "+links_tmp.get(pos)[3]+","+links_tmp.get(pos)[5]);
                    }
                }
                for (int ii = find_positions_hub_tmp.size() - 1; ii >= 0; ii--) {
                    links.remove((int) find_positions_hub_tmp.get(ii));
                }

                if (links_hub_tmp.size() > 1) {
                    ArrayList<String[]> links_hub = new ArrayList();
//                    ArrayList<String> buff = new ArrayList();
                    String buff = "";
                    for (String[] it : links_hub_tmp) {
                        String[] mas = new String[6];
                        mas[0] = it[0];
                        mas[1] = it[1];
                        mas[2] = it[2];
                        mas[3] = "hub";
                        mas[4] = "unknown";
                        mas[5] = "unknown";
                        boolean find = false;
                        for (String[] it1 : links_hub) {
                            if (it1[0].equals(it[0]) && it1[2].equals(it[2])) {
                                find = true;
                                break;
                            }
                            if (it1[3].equals(it[0]) && it1[5].equals(it[2])) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            links_hub.add(mas);
                            buff = it[0] + "_" + it[2];
                        }

                        String[] mas1 = new String[6];
                        mas1[0] = it[3];
                        mas1[1] = it[4];
                        mas1[2] = it[5];
                        mas1[3] = "hub";
                        mas1[4] = "unknown";
                        mas1[5] = "unknown";
                        find = false;
                        for (String[] it1 : links_hub) {
                            if (it1[0].equals(it[3]) && it1[2].equals(it[5])) {
                                find = true;
                                break;
                            }
                            if (it1[3].equals(it[3]) && it1[5].equals(it[5])) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            links_hub.add(mas1);
                            buff = it[0] + "_" + it[2];
                        }
                    }

                    BitSet bs = BitSet.valueOf(new long[]{buff.hashCode()});
                    String hub = "hub_" + Arrays.toString(bs.toByteArray());

                    for (String[] it : links_hub) {
                        if (it[3].equals("hub")) {
                            it[3] = hub;
                        }
                    }

                    result.addAll(links_hub);
                }
                i--;
//                    for(String[] it1 : links) {
//                        boolean find=false;
//                        for(String[] it2 : links_hub) {
//                            if( (it1[0].equals(it2[0]) && it1[2].equals(it2[2])) ||
//                                (it1[3].equals(it2[0]) && it1[5].equals(it2[2])) ||  
//                                (it1[0].equals(it2[3]) && it1[2].equals(it2[5])) ||    
//                                (it1[3].equals(it2[3]) && it1[5].equals(it2[5])) ) {
//                                find=true;
//                                break;
//                            }
//                        }
//                        if(find) logger.Println("Deleted link (FindHub):"+it1[0]+" "+it1[2]+" <---> "+it1[3]+" "+it1[5], logger.DEBUG);
//                        else result.add(it1);
//                    }
//                result.addAll(links_hub);

            }
        }
        result.addAll(links);

        logger.Println("Stop FindHub.", logger.INFO);
        return result;
    }

//    private boolean CheckUniqalNode(String node, Map<String, String> info_nodes) {
//        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node1 = entry.getKey();
//            String value = entry.getValue();
//            String[] mas=value.split(";", -1)[7].split("\\|", -1);
//            boolean find=false;
//            for(String item : mas) {
//                String str=item.split(",", -1)[6];
//                if(!str.equals("")) {
//                    String ip = str.split("/",-1)[0];
//                    if(node.equals(ip)) {
//                        find=true;
//                        break;
//                    }
//                }
//            }
//            if(!find) return true;
//            else return false;
//        } 
//        return true;
//    }
//    public boolean checkUniqalNode(String node, Map<String, String> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node1 = entry.getKey();
//            ArrayList<String> list_ip = getIpAddressFromNode(walkInformationFromNodes, node1);
//            if (list_ip.contains(node)) {
//                return false;
//            }
//        }
//        return true;
//    }

//    public String duplicateNodeWithNode(String node, Map<String, String> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        String duplicate_with_node = "";
//        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node1 = entry.getKey();
//            ArrayList<String> list_ip = getIpAddressFromNode(walkInformationFromNodes, node1);
//            int pos = list_ip.indexOf(node);
//            if (pos >= 0) {
//                duplicate_with_node = list_ip.get(pos) + ";" + info_nodes.get(list_ip.get(pos));
//                break;
//            }
//        }
//        return duplicate_with_node;
//    }

//    public ArrayList<String[]> checkDuplicateLinkList(ArrayList<String[]> links_new, ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        Map<String, ArrayList<String>> nodes_ip_address = getIpAddress(walkInformationFromNodes);
//        for (String[] item : links_new) {
//            if (checkDuplicateLink(item, links, nodes_ip_address)) {
//                logger.Println("CheckDuplicateLinkList: adding link: " + item[0] + "," + item[1] + "," + item[2] + " <---> " + item[3] + "," + item[4] + "," + item[5], logger.DEBUG);
//                links.add(item);
//            }
//        }
//        return links;
//    }

//    public ArrayList<String> duplicateNodesList(Map<String, String[]> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String> result = new ArrayList();
//        Map<String, ArrayList<String>> nodes_ip_address = getIpAddress(walkInformationFromNodes);
//        ArrayList<String> nodes_list = new ArrayList();
//        for (Map.Entry<String, String[]> entry : info_nodes.entrySet()) {
//            nodes_list.add(entry.getKey());
//        }
//
//        for (int i = 0; i < nodes_list.size(); i++) {
//            String node1 = nodes_list.get(i);
//            ArrayList<String> list_ip = nodes_ip_address.get(node1);
//            if (list_ip == null) {
//                list_ip = new ArrayList();
//                list_ip.add(node1);
//            }
//            for (int j = i + 1; j < nodes_list.size(); j++) {
//                String node2 = nodes_list.get(j);
//                if (list_ip.contains(node2)) {
//                    result.add(node2);
//                    nodes_list.remove(j);
//                    logger.Println("DuplicateNodesList: duplicate node: " + node1 + " <---> " + node2, logger.DEBUG);
//                    j--;
//                }
//            }
//        }
//
//        return result;
//    }

    public boolean checkDuplicateLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        // get unique links(remove mirroring links)
        return checkUniqalLink(link, links, nodes_ip_address) == -1;

    }

    private int checkUniqalLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {

        // get unique links(remove mirroring links)
        if (links.isEmpty()) {
            return -1;
        } else {
            boolean found = false;
            int pos = 0;
            for (String[] item1 : links) {
                ArrayList<String> list_ip1 = nodes_ip_address.get(item1[0]);
                if (list_ip1 == null) {
                    list_ip1 = new ArrayList();
                    list_ip1.add(item1[0]);
                }
                ArrayList<String> list_ip2 = nodes_ip_address.get(item1[3]);
                if (list_ip2 == null) {
                    list_ip2 = new ArrayList();
                    list_ip2.add(item1[3]);
                }
                if ((list_ip1.contains(link[0]) && list_ip2.contains(link[3])) && (checkInterfaceName(item1[2], link[2]) || checkInterfaceName(item1[5], link[5]))
                        || (list_ip2.contains(link[0]) && list_ip1.contains(link[3])) && (checkInterfaceName(item1[2], link[5]) || checkInterfaceName(item1[5], link[2]))) {
                    found = true;
                    logger.Println("Link duplicate: " + link[0] + "," + link[1] + "," + link[2] + " <---> " + link[3] + "," + link[4] + "," + link[5] + "\t\t\t" + item1[0] + "," + item1[1] + "," + item1[2] + " <---> " + item1[3] + "," + item1[4] + "," + item1[5], logger.DEBUG);
                    break;
                }
                pos++;
            }
            if (found) {
                return pos;
            }
        }

        return -1;
    }

    private ArrayList<Integer> findLinksFromSomeInterface(int start_pos, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        ArrayList<Integer> result = new ArrayList();

        ArrayList<String[]> collection = new ArrayList();
        String[] mas = new String[2];
        mas[0] = links.get(start_pos)[0];
        mas[1] = links.get(start_pos)[2];
        collection.add(mas);
        String[] mas1 = new String[2];
        mas1[0] = links.get(start_pos)[3];
        mas1[1] = links.get(start_pos)[5];
        collection.add(mas1);
        result.add(start_pos);

        for (int pos = start_pos + 1; pos < links.size(); pos++) {
            String[] item = links.get(pos);
            ArrayList<String> list_ip1 = nodes_ip_address.get(item[0]);
            if (list_ip1 == null) {
                list_ip1 = new ArrayList();
                list_ip1.add(item[0]);
            }
            ArrayList<String> list_ip2 = nodes_ip_address.get(item[3]);
            if (list_ip2 == null) {
                list_ip2 = new ArrayList();
                list_ip2.add(item[3]);
            }
            ArrayList<String[]> collection_add = new ArrayList();
            for (String[] mas2 : collection) {
                if (!(mas2[1].matches("^Trk\\d*") || mas2[1].matches("^trk\\d*") || mas2[1].matches("channel") || mas2[1].matches("Channel"))
                        && (list_ip1.contains(mas2[0]) && checkInterfaceName(item[2], mas2[1])
                        || list_ip2.contains(mas2[0]) && checkInterfaceName(item[5], mas2[1]))) {
                    if (!result.contains(pos)) {
                        result.add(pos);
                    }
                    String[] mas3 = new String[2];
                    mas3[0] = item[0];
                    mas3[1] = item[2];
                    collection_add.add(mas3);
                    String[] mas4 = new String[2];
                    mas4[0] = item[3];
                    mas4[1] = item[5];
                    collection_add.add(mas4);
                }
            }
            for (String[] item1 : collection_add) {
                boolean find = false;
                for (String[] item2 : collection) {
                    if (item1[0].equals(item2[0]) && item1[1].equals(item2[1])) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    collection.add(item1);
                }
            }
        }

        return result;
    }

    //    public ArrayList<String[]> MergingLinks(ArrayList<String[]> links_new, ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String[]> result = new ArrayList();
//
//        if(links_new.size() == 0 && links.size() > 0) {
//            result.addAll(links);
//        } else if(links_new.size() > 0 && links.size() == 0) {
//            result.addAll(links_new);
//        } else if(links_new.size() > 0 && links.size() > 0) {
//            result.addAll(links);
//            for(String[] link : links_new) {
//                boolean find=false;
//                for(int pos=0; pos<result.size(); pos++ ) {
//                    String[] link1 = result.get(pos);
//                    ArrayList<String> list_ip1 = GetIpAddressFromNode(walkInformationFromNodes, link1[0]);
//                    ArrayList<String> list_ip2 = GetIpAddressFromNode(walkInformationFromNodes, link1[3]);
//                    if( (list_ip1.indexOf(link[0]) >= 0 && list_ip2.indexOf(link[3]) >= 0) && ( CheckInterfaceName(link1[2],link[2]) || CheckInterfaceName(link1[5],link[5]) ) ) {
//                        logger.Println("Link mirorring: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5]+"\t\t\t"+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//                        find=true;
//                        break;
//                    } else if( (list_ip2.indexOf(link[0]) >= 0 && list_ip1.indexOf(link[3]) >= 0) && ( CheckInterfaceName(link1[2],link[5]) || CheckInterfaceName(link1[5],link[2]) ) ) {
//                        find=true;
//                        result.remove(pos);
//                        pos--;
//                        String[] mas = new String[link.length];
//                        for(int i=0; i<link.length; i++) mas[i]=link[i];
//                        mas[3]=link1[0]; mas[4]=link1[1]; mas[5]=link1[2];
//                        if(link.length == 7) mas[6]=link[6];
//                        result.add(mas);
//                        logger.Println("Rename id or name link: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" from: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//                        break;
//                    }
//                }
//                if(!find) {
//                    String[] mas = new String[7];
//                    mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=link[3]; mas[4]=link[4]; mas[5]=link[5];
//                    if(link.length == 7) mas[6]=link[6];
//                    result.add(mas);
//                    if(link.length == 7) logger.Println("Link adding: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" ---"+mas[6], logger.DEBUG);
//                    else logger.Println("Link adding: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5], logger.DEBUG);
//                }
//            }
//        }
//        return result;
//    }
//    
//    public ArrayList<String[]> MergingLinksWithoutDuplicateNode(ArrayList<String[]> links_new, ArrayList<String[]> links, ArrayList<String> duplicate_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        for (String duplicate_node : duplicate_nodes) {
//            boolean find1=false; boolean find2=false;
//            for(int i=0; i<links_new.size(); i++) {
//                String[] link = links_new.get(i);
//                if(link[0].equals(duplicate_node)) find1=true;
//                if(link[3].equals(duplicate_node)) find2=true;
//                if(find1 && find2) {
//                    links_new.remove(i);
//                    i--;
//                }
//            }
//        }
//        return MergingLinks(links_new, links, walkInformationFromNodes);
//    }
    public int checkFullUniqalLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {

        // get unique links(remove mirroring links)
        if (links.isEmpty()) {
            return -1;
        } else {
            boolean found = false;
            int pos = 0;
            for (String[] item1 : links) {
                ArrayList<String> list_ip1 = nodes_ip_address.get(item1[0]);
                if (list_ip1 == null) {
                    list_ip1 = new ArrayList();
                    list_ip1.add(item1[0]);
                }
                ArrayList<String> list_ip2 = nodes_ip_address.get(item1[3]);
                if (list_ip2 == null) {
                    list_ip2 = new ArrayList();
                    list_ip2.add(item1[3]);
                }
                if ((list_ip1.contains(link[0]) && list_ip2.contains(link[3])) && checkInterfaceName(item1[2], link[2]) && checkInterfaceName(item1[5], link[5])
                        || (list_ip2.contains(link[0]) && list_ip1.contains(link[3])) && checkInterfaceName(item1[2], link[5]) && checkInterfaceName(item1[5], link[2])) {
                    found = true;
//                    logger.Println("Link mirorring full: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5]+"\t\t\t"+item1[0]+","+item1[1]+","+item1[2]+" <---> "+item1[3]+","+item1[4]+","+item1[5], logger.DEBUG);
                    break;
                }
                pos++;
            }
            if (found) {
                return pos;
            }
        }

        return -1;
    }

    //    public boolean WriteToMapFile(String filename, String buff, Map<String, String> info_nodes, ArrayList<String[]> links, ArrayList<String[]> ARP_MAC, Map<String, String> excluded_nodes, String host_time_live, String history_dir, int history_num_days) {
//        BufferedWriter outFile = null;
//        BufferedWriter outBuff = null;
//        File file = new File(filename);
//        try {
//            if(file.exists()) {
//                logger.Println("File: "+filename+" is exist.", logger.DEBUG);
//                ArrayList<String[]> nodes_in_mapfile = ReadFromMapFile(filename, "^:nodes:$");
//                ArrayList<String[]> links_in_mapfile = ReadFromMapFile(filename, "^:links:$");
//                ArrayList<String[]> hosts_in_mapfile = ReadFromMapFile(filename, "^:hosts:$");
//                ArrayList<String[]> custom_texts_in_mapfile = ReadFromMapFile(filename, "^:custom_texts:$");
////                ArrayList<String[]> extend_info_in_mapfile = ReadFromMapFile(filename, "^:extend_info:$");
//                ArrayList<String[]> text_in_mapfile = ReadFromMapFile(filename, "^:text:$");
//                
//                ArrayList<String[]> nodes_in_buff = ReadFromMapFile(buff, "^:nodes:$");
//                ArrayList<String[]> links_in_buff = ReadFromMapFile(buff, "^:links:$");
//                
//                //clear delete.buff file
//                for(int i=0; i<nodes_in_buff.size(); i++) {
//                    String[] item = nodes_in_buff.get(i);
//                    if(System.currentTimeMillis()-Long.valueOf(item[item.length-1]) > Long.valueOf(host_time_live)*86400*1000) {
//                        nodes_in_buff.remove(i);
//                        logger.Println("Node: "+item[0]+" remove from "+buff+" time exceed.", logger.DEBUG);
//                    }
//                }
//                for(int i=0; i<links_in_buff.size(); i++) {  
//                    String[] item = links_in_buff.get(i);
//                    if(System.currentTimeMillis()-Long.valueOf(item[item.length-1]) > Long.valueOf(host_time_live)*86400*1000) {
//                        links_in_buff.remove(i);
//                        logger.Println("link: "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5]+" remove from "+buff+" time exceed.", logger.DEBUG);
//                    }
//                }
//                
//                outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//                outBuff = new BufferedWriter(new FileWriter(buff));
//                
//                //nodes
//                for(String[] item : nodes_in_mapfile) {
////                    Map<String, String> info_nodes = (Map<String, String>) informationFromNodes.get(0);
//                    boolean find=false;
//                    for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                        if(entry.getKey().equals(item[0])) {
//                            find=true;
//                            break;                            
//                        }
//                    }
//                    if(!find) {
//                        if(nodes_in_buff.size() > 0) {
//                            for(int i=0; i<nodes_in_buff.size(); i++) {
//                                String[] item1 = nodes_in_buff.get(i);
//                                if(item[0].equals(item1[0])) {
//                                    nodes_in_buff.remove(i);
//                                    i--;
//                                }
//                            }
//                            if(!item[0].matches("^hub_\\d+")) {
//                                String[] mas = new String[4];
//                                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; mas[3]=String.valueOf(System.currentTimeMillis());
//                                nodes_in_buff.add(mas);
//                                logger.Println("Node: "+item[0]+" adding to nodes_in_buff", logger.DEBUG);
//                            }
//                        } else {
//                            if(!item[0].matches("^hub_\\d+")) {
//                                String[] mas = new String[4];
//                                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; mas[3]=String.valueOf(System.currentTimeMillis());
//                                nodes_in_buff.add(mas);
//                                logger.Println("Node: "+item[0]+" adding to nodes_in_buff", logger.DEBUG); 
//                            }
//                        }
//                    }
//                }    
//
//                outFile.write(":nodes:\n");
////                Map<String, String> info_nodes = (Map<String, String>) informationFromNodes.get(0);
//                for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                    
//                    boolean find=false;
//                    String coord = "";
//                    String node_image = "";
//                    for(String[] item : nodes_in_mapfile) {
//                        if(entry.getKey().equals(item[0])) {
//                            find=true;
//                            coord=item[1];
//                            node_image=item[2];
//                            break;
//                        }
//                    }
//                    if(find) {
//                        String line=entry.getKey()+";"+coord+";"+node_image+";"+entry.getValue();
//                        outFile.write(line+"\n");
//                        logger.Println("Node: "+line+" write to "+filename+".temp. Is exist node.", logger.DEBUG);
//                    } else {
//                        boolean find1=false;
//                        String coord1 = "";
//                        String node_image1 = "";                        
//                        for(String[] item : nodes_in_buff) {
//                            if(entry.getKey().equals(item[0])) {
//                                find1=true;
//                                coord1=item[1];
//                                node_image1=item[2]; 
//                                nodes_in_buff.remove(item);
//                                break;
//                            }
//                        }
//                        if(find1) {
//                            String line=entry.getKey()+";"+coord1+";"+node_image1+";"+entry.getValue();
//                            outFile.write(line+"\n");
//                            logger.Println("Node: "+line+" write to "+filename+".temp. Is new node. Exist in nodes_in_buff", logger.DEBUG);
//                        } else {
//                            String line=entry.getKey()+";;;"+entry.getValue();
//                            outFile.write(line+"\n");
//                            logger.Println("Node: "+line+" write to "+filename+".temp. Is new node.", logger.DEBUG);
//                        }
//                    }
//                    
//                }
//                outBuff.write(":nodes:\n");
//                for(String[] item : nodes_in_buff) {
////                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                        String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3];
//                        outBuff.write(line+"\n");
//                        logger.Println("Node: "+line+" write to "+buff, logger.DEBUG);
////                    }
//                }                
//                
//                
//                //links
//                for(String[] item : links_in_mapfile) {
//                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && item[3].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && 
//                            !(item[0].matches("^hub_\\d+") || item[3].matches("^hub_\\d+")) ) {
//                        boolean find=false;
//                        String[] link = new String[7];
//                        link[0]=item[0]; link[1]=item[1]; link[2]=item[2]; link[3]=item[3]; link[4]=item[4]; link[5]=item[5];
//                        link[6]=String.valueOf(System.currentTimeMillis());                        
//                        if(links.size() > 0) {
//                            for(String[] item1 : links) {
//                                if( (item[0].equals(item1[0]) && item[2].equals(item1[2]) && item[3].equals(item1[3]) && item[5].equals(item1[5])) ||
//                                    (item[0].equals(item1[3]) && item[2].equals(item1[5]) && item[3].equals(item1[0]) && item[5].equals(item1[2]))    
//                                        ) { 
//                                    find=true; break; 
//                                }
//                            }
//                            if(!find) {
//                                links_in_buff.add(link);
//                                logger.Println("Adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                            } else logger.Println("Not adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                        } else {
//                            links_in_buff.add(link);
//                            logger.Println("Adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                        }
//                    }
//                }
//                
//                for(int i=0; i<links_in_buff.size(); i++) {
//                    String[] item = links_in_buff.get(i);
//                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && item[3].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && 
//                            !(item[0].matches("^hub_\\d+") || item[3].matches("^hub_\\d+")) ) {
//                        int pos=-1;
//                        if(links.size() > 0) {
//                            for(String[] item1 : links) {
//                                if( (item[0].equals(item1[0]) && item[2].equals(item1[2]) && item[3].equals(item1[3]) && item[5].equals(item1[5])) ||
//                                    (item[0].equals(item1[3]) && item[2].equals(item1[5]) && item[3].equals(item1[0]) && item[5].equals(item1[2]))    
//                                        ) { 
//                                    pos=i;
//                                    break; 
//                                }
//                            }
//                            if(pos >= 0) {
//                                logger.Println("Remove link from: "+buff+" - "+links_in_buff.get(pos)[0]+";"+links_in_buff.get(pos)[1]+";"+links_in_buff.get(pos)[2]+";"+links_in_buff.get(pos)[3]+";"+links_in_buff.get(pos)[4]+";"+links_in_buff.get(pos)[5]+";"+links_in_buff.get(pos)[6]+" to links_in_buff", logger.DEBUG);
//                                links_in_buff.remove(pos);
//                                i--;
//                            }
//                        }
//                    }
//                } 
//                
//                outBuff.write(":links:\n");
//                for(String[] item : links_in_buff) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+";"+item[6];
//                    outBuff.write(line+"\n");
//                    logger.Println("links: "+line+" write to "+buff, logger.DEBUG);
//                }
//                
//                outFile.write(":links:\n");
//                for(String[] item : links) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5];
//                    outFile.write(line+"\n");
//                    logger.Println("links: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//
//                //hosts
//                outFile.write(":hosts:\n");
//                for(String[] item : ARP_MAC) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+System.currentTimeMillis();
//                    outFile.write(line+"\n");
//                    logger.Println("host: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//
//                //custom_texts information
//                outFile.write(":custom_texts:\n");
//                for(String[] item : custom_texts_in_mapfile) {
//                    String line=item[0]+";"+item[1]+";"+item[2];
//                    outFile.write(line+"\n");
//                    logger.Println("custom_texts: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }     
//                
//                //text information
//                outFile.write(":text:\n");
//                for(String[] item : text_in_mapfile) {
//                    String line=item[0]+";"+item[1]+";"+item[2];
//                    outFile.write(line+"\n");
//                    logger.Println("text: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }     
//                
//                //extend_info information
//                outFile.write(":extend_info:\n");
//                for(Map.Entry<String, String> entry : excluded_nodes.entrySet()) {
//                    boolean find=false;
//
//                    for(Map.Entry<String, String> entry1 : info_nodes.entrySet()) {
//                        if(entry.getKey().equals(entry1.getKey())) { find=true; break; }
//                    }                    
//                    if(!find) {
//                        String line=entry.getKey()+";"+entry.getValue();
//                        outFile.write(line+"\n");
//                        logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
//                    }
//                }                
////                for(String[] item : extend_info_in_mapfile) {
////                    boolean find=false;
////                    for(Map.Entry<String, String> entry1 : info_nodes.entrySet()) {
////                        if(item[0].equals(entry1.getKey())) { find=true; break; }
////                    }
////                    if(!find) {
////                        String line=item[0];
////                        for(int i=1; i<item.length; i++) line=line+";"+item[i];
////                        outFile.write(line+"\n");
////                        logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
////                    }
////                }                 
//            } else {
//                logger.Println("File: "+filename+" not exist.", logger.DEBUG);
//                outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//                outFile.write(":nodes:\n");
//                for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                    String line=entry.getKey()+";;;"+entry.getValue();
//                    outFile.write(line+"\n");
//                    logger.Println("Node: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                outFile.write(":links:\n");
//                for(String[] item : links) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5];
//                    outFile.write(line+"\n");
//                    logger.Println("link: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                outFile.write(":hosts:\n");
//                for(String[] item : ARP_MAC) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+System.currentTimeMillis();
//                    outFile.write(line+"\n");
//                    logger.Println("host: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                //extended information
//                outFile.write(":extend_info:\n");
//                for(Map.Entry<String, String> entry : excluded_nodes.entrySet()) {
//                    String line=entry.getKey()+";"+entry.getValue();
//                    outFile.write(line+"\n");
//                    logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }                       
//            }
//
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            
//            // remove old map files
//            RemoveOldFiles(history_dir, history_num_days);
//            // replace map file to history
//            File file_tmp = new File(filename+".temp");
//            if(file_tmp.exists()) {
//                Date d = new Date(file.lastModified());
//                SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
//                String file_history = history_dir+"/Neb_"+format1.format(d)+".map"; 
//                File folder_history = new File(history_dir);
//                File history = new File(file_history);
//                if (!folder_history.exists()) {
//                    folder_history.mkdir();
//                }                  
//                if(!history.exists()) file.renameTo(history);
//                if(file_tmp.exists()) {
//                    file.delete();
//                    if(!file.exists()) file_tmp.renameTo(file);
//                }
//            }
//            
//        } catch (Exception ex) {
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        } finally {
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            if(outBuff != null) try {
//                outBuff.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            
//        }
//
//        return true;
//    }
//    
//    public ArrayList ReadFromMapFile(String filename, String match) {
//        ArrayList<String[]> result = new ArrayList();
//
//        BufferedReader inFile = null;
//        File file = new File(filename);
//        if(file.exists()) {
//            try {
//                inFile = new BufferedReader(new FileReader(filename));
//                while(true) {
//                    String line = inFile.readLine().toString();
//                    if(line.matches(match)) break;
//                }
//                while(true) {
//                    String line = inFile.readLine().toString();
//                    if(line.matches("^:\\S+:$")) break;
//                    if(!line.equals("")) result.add(line.split(";", -1));
////                    System.out.println(line);
//                }                
//            } catch (Exception ex) {} 
//            finally {
//                if(inFile != null) try {
//                    inFile.close();
//                } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            }
//        }
//        
//        return result;
//    }
//    
//    private ArrayList<String[]> ReadFromMapFile(File file, String match) {
//        ArrayList<String[]> result = new ArrayList();
//
//        BufferedReader inFile = null;
//        if (file.exists()) {
//            try {
//                inFile = new BufferedReader(new FileReader(file));
//                while (true) {
//                    String line = inFile.readLine().toString();
//                    if (line.matches(match)) {
//                        break;
//                    }
//                }
//                while (true) {
//                    String line = inFile.readLine().toString();
//                    if (line.matches("^:\\S+:$")) {
//                        break;
//                    }
//                    if (!line.equals("")) {
//                        result.add(line.split(";", -1));
//                    }
////                    System.out.println(line);
//                }
//            } catch (Exception ex) {
//            } finally {
//                if (inFile != null) {
//                    try {
//                        inFile.close();
//                    } catch (IOException ex) {
//                    }
//                }
//            }
//        }
//
//        return result;
//    }    
    public Map rescanInformationFromNodes(Map informationFromNodes,
                                          ArrayList<String> community_list, ArrayList<String> include_list,
                                          Map<String, String[]> snmp_accounts_priority, ArrayList<String> net_list) {

        if (informationFromNodes.size() == 3) {
//            ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
            ArrayList<String> list_ip_test = new ArrayList();
            Map<String, String> exclude_list = (Map<String, String>) informationFromNodes.get("exclude_list");
            for (Map.Entry<String, Map> entry : ((Map<String, Map>) informationFromNodes.get("nodes_information")).entrySet()) {
//                logger.Println("Get ip addresses from node - "+entry.getKey()+" to list_ip_test", logger.DEBUG);
                // adding ip address from arp
                Map<String, Map> advanced = (Map<String, Map>) entry.getValue().get("advanced");
                if (advanced != null && !advanced.isEmpty()) {
                    Map<String, String> arp_list = (Map<String, String>) advanced.get("arp");
                    if (arp_list != null) {
                        for (Map.Entry<String, String> ip_mac : arp_list.entrySet()) {
                            String ip = ip_mac.getValue();
                            if (!list_ip_test.contains(ip) && exclude_list.get(ip) == null) {
                                boolean find = false;
                                for (String include_network : include_list) {
                                    if (Neb.neb_lib_utils.insideInterval(ip, include_network)) {
                                        find = true;
                                        break;
                                    }
                                }
                                if (find) {
                                    list_ip_test.add(ip);
                                } else {
                                    logger.Println("ip=" + ip + " not included.", logger.DEBUG);
                                }
                            }

                        }
                    }
                }

                // adding ip address from cdp, lldp
                if (entry.getValue().get("advanced") != null) {
                    Map<String, ArrayList<Map<String, String>>> links_list = (Map<String, ArrayList<Map<String, String>>>) ((Map<String, Map>) entry.getValue().get("advanced")).get("links");
                    if (links_list != null) {
                        for (Map.Entry<String, ArrayList<Map<String, String>>> entry1 : links_list.entrySet()) {
                            ArrayList<Map<String, String>> list = entry1.getValue();
                            for (Map<String, String> item : list) {
                                String remote_ip = item.get("remote_ip");
                                if (remote_ip != null && !remote_ip.matches("^0\\.0\\.0\\.\\d+$") && remote_ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                                    if (!list_ip_test.contains(remote_ip) && exclude_list.get(remote_ip) == null) {
                                        boolean find = false;
                                        for (String include_network : include_list) {
                                            if (Neb.neb_lib_utils.insideInterval(remote_ip, include_network)) {
                                                find = true;
                                                break;
                                            }
                                        }
                                        if (find) {
                                            list_ip_test.add(remote_ip);
                                            logger.Println("Adding from links remote_ip " + remote_ip, logger.DEBUG);
                                        } else {
                                            logger.Println("ip=" + remote_ip + " not included.", logger.DEBUG);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (String ip_test : list_ip_test) {
                logger.Println("list_ip_test=" + ip_test, logger.DEBUG);
            }
            logger.Println("list_ip_test size = " + list_ip_test.size(), logger.DEBUG);

            if (!list_ip_test.isEmpty()) {
                logger.Println("Scanning recursive list ...", logger.INFO);

                ArrayList<String> list_ip = new ArrayList();
//                Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("Rescan scanning ping ip list");
//                watch_Telemetry_Lib.start();
                PingPool pingPool = new PingPool();
                Map<String, String> res = pingPool.get(list_ip_test, Neb.timeout_thread, Neb.timeout_ping, Neb.retries);
//                watch_Telemetry_Lib.exit = true;
                for (Map.Entry<String, String> entry : res.entrySet()) {
                    if (entry.getValue().equals("ok")) {
                        list_ip.add(entry.getKey());
                        logger.Println("pingPool discovery: " + entry.getKey(), logger.DEBUG);
                    }
                }
                logger.Println("list_ip ping size = " + list_ip.size(), logger.DEBUG);

                // scan ports
//                watch_Telemetry_Lib = new Watch_Telemetry_Lib("Scan ports for nodes");
//                watch_Telemetry_Lib.start();
                ScanCli scanCli = new ScanCli();
                ArrayList<String[]> res0 = scanCli.scan(list_ip, exclude_list, Neb.included_port, Neb.excluded_port, Neb.timeout_thread, Neb.timeout);
//                watch_Telemetry_Lib.exit = true;
                list_ip.clear();
                for (String[] item : res0) {
                    if (item[1].equals("ok")) {
                        list_ip.add(item[0]);
                        logger.Println("Scan ports discovery: " + item[0], logger.DEBUG);
                    }
                }
                logger.Println("Scan ports list_ip size = " + list_ip.size(), logger.DEBUG);


                logger.Println("Start scanning snmp ip list ...", logger.DEBUG);
                ArrayList<String[]> node_community_version = new ArrayList();
//                watch_Telemetry_Lib = new Watch_Telemetry_Lib("Rescan scanning snmp ip list");
//                watch_Telemetry_Lib.start();
                SnmpScan snmpScan = new SnmpScan();
                ArrayList<String[]> res1 = snmpScan.scan(list_ip, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", Neb.timeout_thread, 161, Neb.timeout, Neb.retries, snmp_accounts_priority);
//                watch_Telemetry_Lib.exit = true;
                for (String[] mas : res1) {
                    node_community_version.add(mas);
                    logger.Println("snmpScan discovery: " + mas[0] + "," + mas[1] + "," + mas[2], logger.DEBUG);
                }
                logger.Println("node_community_version.size() = " + node_community_version.size(), logger.DEBUG);
                logger.Println("Stop scanning snmp ip list.", logger.DEBUG);

                logger.Println("Start Rescanning ...", logger.DEBUG);
                node_community_version = rescanSNMPVersion(node_community_version, Neb.timeout * 2, Neb.retries);
                logger.Println("Stop Rescanning.", logger.DEBUG);

                if (!node_community_version.isEmpty()) {
                    logger.Println("node_community_version.size() = " + node_community_version.size(), logger.DEBUG);
                    logger.Println("Start get information from nodes ...", logger.INFO);
                    SNMP_information snmp_information = new SNMP_information();
                    Map nodes_information_rescan = snmp_information.getInformationFromNodes(node_community_version);
                    logger.Println("snmp_information.size() = " + nodes_information_rescan.size(), logger.DEBUG);
                    logger.Println("Stop get information from nodes.", logger.INFO);

                    nodes_information_rescan = uniqal_Nodes_Information(nodes_information_rescan);

                    Gson gson = new Gson();
                    for (Map.Entry<String, Map> entry : ((Map<String, Map>) nodes_information_rescan).entrySet()) {
                        String str = gson.toJson(entry.getValue());
                        logger.Println("nodes_information discovery=" + entry.getKey() + " - " + str, logger.DEBUG);
                        //            System.out.println("nodes_information="+entry.getKey()+" - "+str);
                    }

                    // merging informationFromNodes and nodes_information
                    if (informationFromNodes.get("nodes_information") != null) {
                        ((Map) informationFromNodes.get("nodes_information")).putAll(nodes_information_rescan);
                    }


                    Map<String, String> ip_from_nodes = getIpFromNodes(nodes_information_rescan, net_list, Neb.not_correct_networks);
                    exclude_list.putAll(ip_from_nodes);
                    for (String ip : list_ip_test) {
                        exclude_list.put(ip, ip);
                    }
                    informationFromNodes.put("exclude_list", exclude_list);

                    // translate node_protocol_accounts Array to ArrayList
                    Map<String, String[]> node_protocol_accounts_new = translateNodeProtocolAccountsToList(node_community_version);

                    // merging node_protocol_accounts
                    ((Map<String, String[]>) informationFromNodes.get("node_protocol_accounts")).putAll(node_protocol_accounts_new);
                    informationFromNodes.put("node_protocol_accounts", informationFromNodes.get("node_protocol_accounts"));

                    // checking nodes not get information !!!
                    for (Map.Entry<String, String[]> entry : node_protocol_accounts_new.entrySet()) {
                        String node = entry.getKey();
                        String node_base_ip = ip_from_nodes.get(node);
                        if (node_base_ip == null) {
                            node_base_ip = node;
                        }
                        boolean find = false;
                        for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) nodes_information_rescan).entrySet()) {
                            String node1 = entry1.getKey();
                            String node_base_ip1 = ip_from_nodes.get(node1);
                            if (node_base_ip1 == null) {
                                node_base_ip = node1;
                            }
                            if (node_base_ip.equals(node_base_ip1)) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            logger.Println("Not access information from nodes - " + node_base_ip + " !!!", logger.DEBUG);
                        }
                    }
                }
            }
        }

        return informationFromNodes;
    }

//    public ArrayList merge_nodes_information(ArrayList new_list, ArrayList old_list, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList result = new ArrayList();
//
//        Map<String, String> information_nodes_res = new HashMap<>();
//        ArrayList<String> exclude_list = new ArrayList();
//
//        Map<String, String> information_nodes_new = (Map<String, String>) new_list.get(0);
//        Map<String, String> information_nodes_old = (Map<String, String>) old_list.get(0);
//
//        ArrayList<String[]> dplinks_new = (ArrayList<String[]>) new_list.get(1);
//        ArrayList<String[]> dplinks_old = (ArrayList<String[]>) old_list.get(1);
//
//        // merging node information
//        for (Map.Entry<String, String> entry : information_nodes_new.entrySet()) {
//            String node_new = entry.getKey();
//            String node_info_new = entry.getValue();
//
//            boolean find = false;
//            for (Map.Entry<String, String> entry1 : information_nodes_old.entrySet()) {
//                String node_old = entry1.getKey();
//                String node_info_old = entry1.getValue();
//
//                if (node_new.equals(node_old)) {
//                    String[] mas = node_info_old.split(";", -1)[0].split("\\|");
//                    if (mas.length != 3) {
//                        String[] mas1 = node_info_new.split(";", -1)[0].split("\\|");
//                        if (mas1.length != 3) {
//                            if (node_new.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                                exclude_list.add(node_new);
//                            }
//                        }
//                        information_nodes_res.put(node_new, node_info_new);
//                    } else {
//                        information_nodes_res.put(node_old, node_info_old);
//                    }
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) {
////                String[] mas = node_info_new.split(";", -1)[0].split("\\|");
////                if(mas.length != 3) {
////                    if(node_new.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
////                       exclude_list.add(node_new);
////                    }
////                }
//                information_nodes_res.put(node_new, node_info_new);
//            }
//        }
//
//        for (Map.Entry<String, String> entry : information_nodes_old.entrySet()) {
//            String node_old = entry.getKey();
//            String node_info_old = entry.getValue();
//            boolean find = false;
//            for (Map.Entry<String, String> entry1 : information_nodes_new.entrySet()) {
//                String node_new = entry1.getKey();
//                if (node_old.equals(node_new)) {
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) {
//                String[] mas = node_info_old.split(";", -1)[0].split("\\|");
//                if (mas.length != 3) {
//                    if (node_old.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                        exclude_list.add(node_old);
//                    }
//                }
//                information_nodes_res.put(node_old, node_info_old);
//            }
//        }
//
//        // merging links
//        Map<String, ArrayList<String>> nodes_ip_address = getIpAddress(walkInformationFromNodes);
//        for (String[] item : dplinks_new) {
//            if (checkDuplicateLink(item, result, nodes_ip_address)) {
//                dplinks_old.add(item);
//            }
//        }
//
//        // merging walk info
//        Map<String, Map<String, ArrayList>> walkInformation_new = (Map<String, Map<String, ArrayList>>) new_list.get(2);
//        Map<String, Map<String, ArrayList>> walkInformation_old = (Map<String, Map<String, ArrayList>>) old_list.get(2);
//
//        Map<String, Map<String, ArrayList>> walkInformation = mergingWalk(walkInformation_new, walkInformation_old);
//
////        for (Map.Entry<String, Map<String, ArrayList>> entry : walkInformation_new.entrySet()) {
////            String key = entry.getKey();
////            Map<String, ArrayList> val = entry.getValue();
////            Map<String, ArrayList> val_all = walkInformation.get(key);
////            val_all.putAll(val);
////            walkInformation.put(key, val_all);
////        }
//        // merging exclude list
//        ArrayList<String> exclude_old = (ArrayList<String>) old_list.get(3);
//        for (String item : exclude_list) {
//            if (!exclude_old.contains(item)) {
//                exclude_old.add(item);
//            }
//        }
//
//        // merging node_community_version
//        ArrayList<String[]> node_community_version_old = (ArrayList<String[]>) old_list.get(4);
//        for (String[] item : (ArrayList<String[]>) new_list.get(4)) {
//            boolean find = false;
//            for (String[] item_old : node_community_version_old) {
//                if (item_old[0].equals(item[0])) {
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) {
//                node_community_version_old.add(item);
//            }
//        }
//
//        result.add(information_nodes_res);
//        result.add(dplinks_old);
//        result.add(walkInformation);
//        result.add(exclude_old);
//        result.add(node_community_version_old);
//
//        return result;
//    }

    public Map<String, Map<String, ArrayList>> mergingWalk(Map<String, Map<String, ArrayList>> walkInformation_new, Map<String, Map<String, ArrayList>> walkInformation_old) {
        Map<String, Map<String, ArrayList>> result = new HashMap(walkInformation_old);

        for (Map.Entry<String, Map<String, ArrayList>> entry : walkInformation_new.entrySet()) {
            String key = entry.getKey();
            Map<String, ArrayList> val = entry.getValue();
            Map<String, ArrayList> val_all = walkInformation_old.get(key);
            if (val_all == null) {
                val_all = new HashMap();
            }
            val_all.putAll(val);
            result.put(key, val_all);
        }
        return result;
    }

    public Map<String, ArrayList<String[]>> getArpMac(Map<String, ArrayList<String[]>> area_node_community_version) {
        Map<String, ArrayList<String[]>> area_ARP_MAC = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : area_node_community_version.entrySet()) {
            String area = entry.getKey();
            ArrayList<String[]> node_community_version = entry.getValue();

            logger.Println("Start GetARP area " + area + "...", logger.DEBUG);
            Map<String, String> ARP = getARP(node_community_version);
            if (DEBUG) mapToFile(ARP, Neb.debug_folder + "/ARP_" + area, Neb.DELAY_WRITE_FILE);
            logger.Println("Stop GetARP area " + area + ". size=" + ARP.size(), logger.DEBUG);

            logger.Println("Start GetMAC area " + area + "...", logger.DEBUG);
            Map<String, Map<String, ArrayList>> MAC = getMAC(node_community_version);
            if (DEBUG) mapToFile(MAC, Neb.debug_folder + "/MAC_" + area, Neb.DELAY_WRITE_FILE);
            logger.Println("Stop GetMAC area " + area + ". size=" + MAC.size(), logger.DEBUG);

            if (Neb.area_node_ifaceid_ifacename.get(area) != null) {
                logger.Println("Start svodka ARP MAC area " + area + "...", logger.DEBUG);
                ArrayList<String[]> ARP_MAC = arpmac(MAC, ARP, node_community_version, Neb.area_node_ifaceid_ifacename.get(area));
                logger.Println("Stop svodka ARP MAC area " + area + ". size=" + ARP_MAC.size(), logger.DEBUG);
                area_ARP_MAC.put(area, ARP_MAC);
            }
//            if(DEBUG) mapToFile((Map)area_ARP_MAC, Neb.debug_folder+"/area_ARP_MAC", Neb.DELAY_WRITE_FILE);            
        }

        return area_ARP_MAC;
    }

    public Map scanInformationFromNodes(ArrayList<String> network_list,
                                        ArrayList<String> ip_list, ArrayList<String> community_list,
                                        Map<String, String> exclude_list,
//            ArrayList<String> include_sysDescr_cli_matching,
                                        Map<String, String[]> snmp_accounts_priority,
                                        ArrayList<String> net_list) {
        Map result = new HashMap();

        logger.Println("Scanning networks ...", logger.INFO);
        SnmpScan snmpScan = new SnmpScan();
        ScanCli scanCli = new ScanCli();
//        NodesScanPool nodesScanPool = new NodesScanPool();
        PingPool pingPool = new PingPool();
        ArrayList<String[]> node_community_version = new ArrayList();
//        ArrayList<String[]> cli_node_account = new ArrayList();
        ArrayList<String> list_ip_ping = new ArrayList(ip_list);
        for (String network : network_list) {
            logger.Println("Start scanning snmp networks:\t" + network + " ...", logger.DEBUG);

            ArrayList<String> list_ip = new ArrayList();
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("Scanning ping networks: " + network);
//            watch_Telemetry_Lib.start();
            Map<String, String> res = pingPool.get(network, exclude_list, Neb.timeout_thread, Neb.timeout_ping, Neb.retries);
//            watch_Telemetry_Lib.exit = true;
            for (Map.Entry<String, String> entry : res.entrySet()) {
                if (entry.getValue().equals("ok")) {
                    list_ip.add(entry.getKey());
                    logger.Println("pingPool: " + entry.getKey(), logger.DEBUG);
                }
            }
            logger.Println("ping list_ip size = " + list_ip.size(), logger.DEBUG);
            list_ip_ping.addAll(list_ip);

            // scan ports
//            watch_Telemetry_Lib = new Watch_Telemetry_Lib("Scan ports for nodes");
//            watch_Telemetry_Lib.start();
            ArrayList<String[]> res0 = scanCli.scan(list_ip, exclude_list, Neb.included_port, Neb.excluded_port, Neb.timeout_thread, Neb.timeout);
//            watch_Telemetry_Lib.exit = true;
            list_ip.clear();
            for (String[] item : res0) {
                if (item[1].equals("ok")) {
                    list_ip.add(item[0]);
                    logger.Println("Scan ports: " + item[0], logger.DEBUG);
                }
            }
            logger.Println("Scan ports list_ip size = " + list_ip.size(), logger.DEBUG);

            // output
//            watch_Telemetry_Lib = new Watch_Telemetry_Lib("SnmpScan for nodes");
//            watch_Telemetry_Lib.start();
            ArrayList<String[]> res1 = snmpScan.scan(list_ip, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", Neb.timeout_thread, Neb.timeout, Neb.retries, snmp_accounts_priority);
//            watch_Telemetry_Lib.exit = true;

            for (String[] mas : res1) {
                node_community_version.add(mas);
                logger.Println("snmpScan: " + mas[0] + "," + mas[1] + "," + mas[2], logger.DEBUG);
            }
            logger.Println("snmpScan.Scan size = " + res1.size(), logger.DEBUG);
            logger.Println("Stop scanning snmp networks:\t" + network, logger.DEBUG);
        }
        if (!ip_list.isEmpty()) {
            // scan ports
//            Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("Scan ports for nodes");
//            watch_Telemetry_Lib.start();
            ArrayList<String[]> res0 = scanCli.scan(ip_list, exclude_list, Neb.included_port, Neb.excluded_port, Neb.timeout_thread, Neb.timeout);
//            watch_Telemetry_Lib.exit = true;
            ip_list.clear();
            for (String[] item : res0) {
                if (item[1].equals("ok")) {
                    ip_list.add(item[0]);
                    logger.Println("Scan ports: " + item[0], logger.DEBUG);
                }
            }
            logger.Println("Scan ports list_ip size = " + ip_list.size(), logger.DEBUG);

            logger.Println("Start scanning snmp ip list ...", logger.DEBUG);
//            watch_Telemetry_Lib = new Watch_Telemetry_Lib("Scanning snmp ip list");
//            watch_Telemetry_Lib.start();           
            ArrayList<String[]> res1 = snmpScan.scan(ip_list, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", Neb.timeout_thread, 161, Neb.timeout, Neb.retries, snmp_accounts_priority);
//            watch_Telemetry_Lib.exit = true;
            for (String[] mas : res1) {
                node_community_version.add(mas);
                logger.Println("snmpScan: " + mas[0] + "," + mas[1] + "," + mas[2], logger.DEBUG);
//                exclude_list.put(mas[0], mas[0]);
            }
            logger.Println("snmpScan.Scan size = " + res1.size(), logger.DEBUG);
            logger.Println("Stop scanning snmp ip list.", logger.DEBUG);
        }
        logger.Println("node_community_version size = " + node_community_version.size(), logger.DEBUG);
        logger.Println("Start Rescanning ...", logger.DEBUG);
        node_community_version = rescanSNMPVersion(node_community_version, Neb.timeout * 2, Neb.retries);
        logger.Println("Stop Rescanning.", logger.DEBUG);
        logger.Println("node_community_version rescanSNMPVersion size = " + node_community_version.size(), logger.DEBUG);
        logger.Println("End scanning networks.", logger.INFO);

        if (!node_community_version.isEmpty()) {
            logger.Println("Start get information from nodes ...", logger.INFO);
            SNMP_information snmp_information = new SNMP_information();
            Map nodes_information = snmp_information.getInformationFromNodes(node_community_version);

            logger.Println("Stop get information from nodes.", logger.INFO);

            ArrayList<String> networks = new ArrayList();
            networks.addAll(network_list);
            networks.addAll(ip_list);
            nodes_information = uniqal_Nodes_Information(nodes_information, networks);

            Gson gson = new Gson();
            for (Map.Entry<String, Map> entry : ((Map<String, Map>) nodes_information).entrySet()) {
                String str = gson.toJson(entry.getValue());
                logger.Println("nodes_information=" + entry.getKey() + " - " + str, logger.DEBUG);
                //            System.out.println("nodes_information="+entry.getKey()+" - "+str);
            }
            logger.Println("nodes_information size = " + nodes_information.size(), logger.DEBUG);
            result.put("nodes_information", nodes_information);

            Map<String, String> ip_from_nodes = getIpFromNodes(nodes_information, net_list, Neb.not_correct_networks);
            exclude_list.putAll(ip_from_nodes);
            for (String ip : list_ip_ping) {
                exclude_list.put(ip, ip);
            }
            result.put("exclude_list", exclude_list);

            // checking nodes not get information !!!
            for (String[] item : node_community_version) {
                String node = item[0];
                String node_base_ip = ip_from_nodes.get(node);
                if (node_base_ip == null) {
                    node_base_ip = node;
                }
                boolean find = false;
                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) nodes_information).entrySet()) {
                    String node1 = entry1.getKey();
                    String node_base_ip1 = ip_from_nodes.get(node1);
                    if (node_base_ip1 == null) {
                        node_base_ip = node1;
                    }
                    if (node_base_ip.equals(node_base_ip1)) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    logger.Println("Not access information from nodes - " + node_base_ip + " !!!", logger.DEBUG);
                }
            }

            // translate node_protocol_accounts Array to ArrayList
            Map<String, String[]> node_protocol_accounts_new = translateNodeProtocolAccountsToList(node_community_version);

            result.put("node_protocol_accounts", node_protocol_accounts_new);
        }
        return result;
    }

//    private Map<String, ArrayList<String[]>> mergingCliAndSnmpAccounts(ArrayList<String[]> cli_node_account, ArrayList<String[]> node_community_version) {
//
//        // normalize node_community_version
//        ArrayList<String[]> node_community_version_norm = new ArrayList();
//        for (String[] mas : node_community_version) {
//            String[] mas1 = new String[4];
//            mas1[0] = mas[0];
//            mas1[1] = "snmp";
//            mas1[2] = mas[1];
//            mas1[3] = mas[2];
//            node_community_version_norm.add(mas1);
//        }
//
//        Map<String, ArrayList<String[]>> node_protocol_accounts = new HashMap();
//        for (String[] mas : cli_node_account) {
//            String[] mas_tmp = new String[4];
//            mas_tmp[0] = mas[1];
//            mas_tmp[1] = mas[2];
//            mas_tmp[2] = mas[3];
//            mas_tmp[3] = mas[4];
//            ArrayList<String[]> list = node_protocol_accounts.get(mas[0]);
//            if (list != null && !list.isEmpty()) {
//                boolean find_protocol = false;
//                for (String[] it : list) {
//                    if (it[0].equals(mas_tmp[0])) {
//                        find_protocol = true;
//                        break;
//                    }
//                }
//                if (!find_protocol) {
//                    node_protocol_accounts.get(mas[0]).add(mas_tmp);
//                }
//            } else {
//                ArrayList list_tmp = new ArrayList();
//                list_tmp.add(mas_tmp);
//                node_protocol_accounts.put(mas[0], list_tmp);
//            }
//        }
//        for (String[] mas : node_community_version_norm) {
//            String[] mas_tmp = new String[3];
//            mas_tmp[0] = mas[1];
//            mas_tmp[1] = mas[2];
//            mas_tmp[2] = mas[3];
//            ArrayList<String[]> list = node_protocol_accounts.get(mas[0]);
//            if (list != null && !list.isEmpty()) {
//                boolean find_protocol = false;
//                for (String[] it : list) {
//                    if (it[0].equals(mas_tmp[0])) {
//                        find_protocol = true;
//                        break;
//                    }
//                }
//                if (!find_protocol) {
//                    node_protocol_accounts.get(mas[0]).add(mas_tmp);
//                }
//            } else {
//                ArrayList list_tmp = new ArrayList();
//                list_tmp.add(mas_tmp);
//                node_protocol_accounts.put(mas[0], list_tmp);
//            }
//        }
//        return node_protocol_accounts;
//    }

//    public void DeleteUnlinkedNodes() {
//        Map<String, String[]> nodes_info_tmp = new HashMap();
//        nodes_info_tmp.putAll(Neb.nodes_info);
//        ArrayList<String[]> links_info_tmp = new ArrayList();
//        links_info_tmp.addAll(Neb.links_info);
//        
//        // delete unlinked nodes
//        for(Map.Entry<String, String[]> entry : nodes_info_tmp.entrySet()) {
//            String node = entry.getKey();
//            boolean find=false;
//            for(String[] item : links_info_tmp) {
//                if(node.equals(item[0]) || node.equals(item[3])) {
//                    find=true;
//                    break;
//                }
//            }
//            if(!find) DeletedNode(node, false);
//        } 
//    }
//    public ArrayList IntegrationCheckingOne(Map<String, String> info_nodes, ArrayList<String[]> links) {
//        ArrayList result = new ArrayList();
////        Map<String, String> info_nodes_out = new HashMap();
//        ArrayList<String[]> links_out = new ArrayList();
//        Map<String, String> excluded_nodes_out = new HashMap();
//        
//        
////        // delete unlinked nodes
////        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
////            String node = entry.getKey();
////            boolean find=false;
////            for(String[] item : links) {
////                if(node.equals(item[0]) || node.equals(item[3])) {
////                    find=true;
////                    break;
////                }
////            }
////            if(find) info_nodes_out.put(node, entry.getValue());
////            else { 
////                excluded_nodes_out.put(node, entry.getValue());
////                logger.Println("Excluded unlinked node: "+node+";"+entry.getValue(), logger.DEBUG);
////            }
////        }
//        
//        // delete unused links
//        for(String[] item : links) {
//            boolean find1=false;
//            for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                String node = entry.getKey();
//                if(node.equals(item[0])) {
//                    find1=true;
//                    break;
//                }
//            }
//            boolean find2=false;
//            for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                String node = entry.getKey();
//                if(node.equals(item[3])) {
//                    find2=true;
//                    break;
//                }
//            }            
//            if(find1 && find2) links_out.add(item);
//        }
//        
//        // check duplicate nodes am MAC address in node name
//        ArrayList<String> info_list = new ArrayList();
//        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            info_list.add(entry.getKey()+";"+entry.getValue());
//        }
//        ArrayList<String[]> node_replace = new ArrayList();
//        for(String item : info_list) {
//            String node = item.split(";", -1)[0];
//            Pattern p = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
//            Matcher m = p.matcher(node);
//            if(m.find()) {
//                String find_mac = node.substring(m.start(), m.end());
//                for(String item1 : info_list) {
//                    ArrayList<String> mac_list = GetMacAddressFromInfoNode(item1);
//                    boolean find=false;
//                    for(String mac : mac_list) {
//                        if(find_mac.equals(mac)) {
//                            find=true;
//                            String[] mas = new String[2];
//                            mas[0]=node; mas[1]=item1.split(";", -1)[0];
//                            if(!mas[1].equals("unknown_ip")) {
//                                logger.Println("Node: "+mas[0]+" need replaced to: "+mas[1], logger.DEBUG);
//                                node_replace.add(mas);
//                            }
//                            break;
//                        }
//                    }
//                    if(find) {
//                        break;
//                    }
//                }
//            }            
//        }
//        for(String[] item : node_replace) {
//            info_nodes.remove(item[0]);            
//            logger.Println("Remove duplicate node: "+item[0], logger.DEBUG);
//            for(int i=0; i<links_out.size(); i++) {
//                String[] link = links_out.get(i);
//                if(item[0].equals(link[0])) {
//                    String[] mas = new String[6];
//                    mas[0]=item[1]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=link[3]; mas[4]=link[4]; mas[5]=link[5];
//                    links_out.remove(i);
//                    i--;
//                    links_out.add(mas);
//                    logger.Println("Replace link from: "+link[0]+";"+link[1]+";"+link[2]+" <---> "+link[3]+";"+link[4]+";"+link[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//                }
//                if(item[0].equals(link[3])) {
//                    String[] mas = new String[6];
//                    mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=item[1]; mas[4]=link[4]; mas[5]=link[5];
//                    links_out.remove(i);
//                    i--;
//                    links_out.add(mas);
//                    logger.Println("Replace link from: "+link[0]+";"+link[1]+";"+link[2]+" <---> "+link[3]+";"+link[4]+";"+link[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//                }                
//            }
//            
//        }
//        
//        
//        result.add(info_nodes);
//        result.add(links_out);
//        result.add(excluded_nodes_out);
//        
//        return result;
//    }
//    public ArrayList IntegrationCheckingTwo(Map<String, String> info_nodes, ArrayList<String[]> links, ArrayList<String[]> arp_mac, Map<String, ArrayList<String>> nodes_ip_address) {
//        ArrayList result = new ArrayList();
//        
//        // replace info_nodes
//        Map<String, String> info_nodes_out = new HashMap();
//        Map<String, String> replace = new HashMap();
//        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node = entry.getKey();
//            Pattern p = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
//            Matcher m = p.matcher(node);
//            if(m.find()) {
//                String find_mac = node.substring(m.start(), m.end());
//                String ip = "";
//                for(String[] item : arp_mac) {
//                    if(find_mac.equals(item[0]) && !item[1].equals("unknown_ip")) {
//                        ip=item[1];
//                        break;
//                    }
//                }
//                if(!ip.equals("")) {
//                    replace.put(node, ip);
//                    info_nodes_out.put(ip, entry.getValue());
//                    logger.Println("Replace node from: "+node+" to: "+ip, logger.DEBUG);                    
//                } else {
//                    info_nodes_out.put(node, entry.getValue());
//                }
//            } else info_nodes_out.put(node, entry.getValue());
//        }
//        
//        // replace links
//        ArrayList<String[]> links_out = new ArrayList();
//        for(String[] item : links) {
//            if(replace.get(item[0]) != null) {
//                String[] mas = new String[6];
//                mas[0]=replace.get(item[0]); mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);
//                logger.Println("Replace link from: "+item[0]+";"+item[1]+";"+item[2]+" <---> "+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            if(replace.get(item[3]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=replace.get(item[3]); mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);
//                logger.Println("Replace link from: "+item[0]+";"+item[1]+";"+item[2]+" <---> "+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }   
//            if(replace.get(item[0]) == null && replace.get(item[3]) == null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);                
//            }
//        }
//        
//        // replace hosts
//        ArrayList<String[]> arp_mac_tmp = new ArrayList();
//        for(String[] item : arp_mac) {
//            if(replace.get(item[1]) != null && replace.get(item[2]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=replace.get(item[1]); mas[2]=replace.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            else if(replace.get(item[1]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=replace.get(item[1]); mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            else if(replace.get(item[2]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=replace.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            } else arp_mac_tmp.add(item);
//        }
//        
//        // delete taftology in hosts
//        ArrayList<String[]> arp_mac_out = new ArrayList();
//        for(String[] item : arp_mac_tmp) {
//            Pattern p = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
//            Pattern p1 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
//            
//            Matcher m = p.matcher(item[0]);
//            String mac="";
//            if(m.find()) mac=m.group(1);
//            
//            Matcher m1 = p.matcher(item[2]);
//            String mac1="";
//            if(m1.find()) mac1=m1.group(1);
//            
//            Matcher m2 = p1.matcher(item[1]);
//            String ip="";
//            if(m2.find()) ip=m2.group(1);    
//            
//            Matcher m3 = p1.matcher(item[2]);
//            String ip1="";
//            if(m3.find()) ip1=m3.group(1);              
//            
//            if( !(!mac.equals("") && !mac1.equals("") && mac.equals(mac1) &&
//               !ip.equals("") && !ip1.equals("") && ip.equals(ip1))    
//                    ) arp_mac_out.add(item);
//            else logger.Println("Delete taftology in hosts: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5], logger.DEBUG);
//        }
//        
//        //replace ip aliases in arp_mac_out
//        Map<String, String> replace1 = new HashMap();
//        for(Map.Entry<String, ArrayList<String>> entry : nodes_ip_address.entrySet()) {
//            for(String item : entry.getValue()) {
//                replace1.put(item, entry.getKey());
//                logger.Println("replace1: "+item+" ===> "+entry.getKey(), logger.DEBUG);
//            }
//        }
//        
//        for(int i=0; i<arp_mac_out.size(); i++) {
//            String[] item = arp_mac_out.get(i);
//            if(replace1.get(item[2]) != null && !replace1.get(item[2]).equals(item[2])) {
//                arp_mac_out.remove(i);
//                i--;
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=replace1.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_out.add(mas);
//                logger.Println("Replace arp_mac_out record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//        }
//        
//        result.add(info_nodes_out);
//        result.add(links_out);
//        result.add(arp_mac_out);
//        
//        return result;
//    }
//    private ArrayList<String> GetMacAddressFromInfoNode(String info_node) {
//        ArrayList<String> result = new ArrayList();
//        String[] mas = info_node.split(";", -1);
//        if(mas.length == 11) {
//            if(mas[7].matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) result.add(mas[7]);
//            String[] mas1 = mas[8].split("\\|", -1);
//            for(String item : mas1) {
//                String[] mas2 = item.split(",", -1);
//                if(mas2.length > 6) {
//                    String mac = mas2[5];
//                    if(mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) result.add(mac);
//                }
//            }
//        }
//        return result;
//    }
//    public ArrayList nodes_Links_Summary(ArrayList informationFromNodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes, ArrayList<String[]> arp_mac) {
//        ArrayList result = new ArrayList();
//
//        Map<String, String> nodes_info = (Map<String, String>) informationFromNodes.get(0);
//        ArrayList<String[]> links = (ArrayList<String[]>) informationFromNodes.get(1);
//        Map<String, Map<String, ArrayList>> walks_info = (Map<String, Map<String, ArrayList>>) informationFromNodes.get(2);
//        ArrayList<String> exclude_list = (ArrayList<String>) informationFromNodes.get(3);
////        ArrayList<String[]> node_community_version = (ArrayList<String[]>)informationFromNodes.get(4);
//
//        // node ip address to map
//        Map<String, String> ip_ip = new HashMap();
//        for (Map.Entry<String, String> entry : nodes_info.entrySet()) {
//            String node = entry.getKey();
//            ArrayList<String> list_ip = getIpAddressFromNode(walkInformationFromNodes, node);
//            for (String item : list_ip) {
//                if (item.equals("127.0.0.1")) {
//                    ip_ip.put("127.0.0.1", "127.0.0.1");
//                } else {
//                    ip_ip.put(item, list_ip.get(0));
//                }
//            }
//        }
//
//        // delete duplicate nodes and adding to ip_ip
//        Map<String, String> nodes_info_new = new HashMap();
//        for (Map.Entry<String, String> entry : nodes_info.entrySet()) {
//            String node = entry.getKey();
//            String tmp = ip_ip.get(entry.getKey());
//            if (tmp != null) {
//                node = tmp;
//            } else {
//                ip_ip.put(node, node);
//            }
//            if (!node.equals("127.0.0.1")) {
//                String str_node_info_new = nodes_info_new.get(node);
//                if (str_node_info_new != null) {
//                    if (str_node_info_new.split(";", -1)[0].split("\\|", -1).length == 1) {
//                        if (entry.getValue().split(";", -1)[0].split("\\|", -1).length > 1) {
//                            nodes_info_new.remove(node);
//                            nodes_info_new.put(node, entry.getValue());
//                            logger.Println("Rename duplicate node: " + node + ";" + str_node_info_new + " -  rename to node: " + entry.getKey() + ";" + entry.getValue(), logger.DEBUG);
//                        } else {
//                            logger.Println("Duplicate node: " + entry.getKey() + ";" + entry.getValue() + " - exist node: " + node + ";" + str_node_info_new, logger.DEBUG);
//                        }
//                    } else {
//                        logger.Println("Duplicate node: " + entry.getKey() + ";" + entry.getValue() + " - exist node: " + node + ";" + str_node_info_new, logger.DEBUG);
//                    }
//                } else {
//                    nodes_info_new.put(node, entry.getValue());
//                }
//            }
//        }
//
//        // rename and delete unused links
//        ArrayList<String[]> links_new = new ArrayList();
//        ArrayList<String[]> excluded_links = new ArrayList();
//        for (String[] item : links) {
//            if (item.length == 8) {
//                String aliase1 = ip_ip.get(item[0]);
//                String aliase2 = ip_ip.get(item[3]);
//
//                if (aliase1 != null && aliase2 != null) {
//                    if (!item[0].equals(aliase1) || !item[3].equals(aliase2)) {
//                        logger.Println("Rename link: " + item[0] + " " + item[2] + " <---> " + item[3] + " " + item[5] + " to: " + aliase1 + " " + item[2] + " <---> " + aliase2 + " " + item[5], logger.DEBUG);
//                    }
//
//                    String[] mas = new String[8];
//                    mas[0] = aliase1;
//                    mas[1] = item[1];
//                    mas[2] = item[2];
//                    mas[3] = aliase2;
//                    mas[4] = item[4];
//                    mas[5] = item[5];
//                    mas[6] = item[6];
//                    mas[7] = item[7];
//                    links_new.add(mas);
//                } else {
//                    excluded_links.add(item);
//                }
//            } else {
//                loggingArray(item, "Delete link from Nodes_Links_Summary", logger.DEBUG);
//            }
//        }
//
//        // rename and delete walks_info
//        Map<String, Map<String, ArrayList>> walks_info_new = new HashMap();
//        for (Map.Entry<String, Map<String, ArrayList>> entry : walks_info.entrySet()) {
//            String key = entry.getKey();
//            Map<String, ArrayList> val = entry.getValue();
//            Map<String, ArrayList> map_tmp = new HashMap();
//            for (Map.Entry<String, ArrayList> entry1 : val.entrySet()) {
//                String node = ip_ip.get(entry1.getKey());
//                ArrayList list = entry1.getValue();
//                if (node != null) {
//                    map_tmp.put(node, list);
//                }
//            }
//            walks_info_new.put(key, map_tmp);
//        }
//
//        // make node_community_version from nodes_info_new
//        ArrayList<String[]> node_community_version = new ArrayList();
//        for (Map.Entry<String, String> entry : nodes_info_new.entrySet()) {
//            String node = entry.getKey();
//            String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
//            if (mas.length == 3) {
//                String[] mas1 = new String[3];
//                mas1[0] = node;
//                mas1[1] = mas[1];
//                mas1[2] = mas[2];
//                node_community_version.add(mas1);
//            }
//        }
//
//        // rename arp_mac
//        ArrayList arp_mac_new = new ArrayList();
//        for (String[] item : arp_mac) {
//            String node = ip_ip.get(item[0]);
//            if (node != null) {
//                if (!item[0].equals(node)) {
//                    logger.Println("Rename arp_mac: " + item[0] + " --- " + node, logger.DEBUG);
//                }
//                String[] mas = new String[5];
//                mas[0] = node;
//                mas[1] = item[1];
//                mas[2] = item[2];
//                mas[3] = item[3];
//                mas[4] = item[4];
//                arp_mac_new.add(mas);
//            }
//        }
//
//        result.add(nodes_info_new);
//        result.add(links_new);
//        result.add(walks_info_new);
//        result.add(exclude_list);
//        result.add(node_community_version);
//        result.add(arp_mac_new);
//        result.add(excluded_links);
//
//        return result;
//    }

    public int num_Community_node(ArrayList informationFromNodes) {
        int community_nodes = 0;
        if (informationFromNodes != null && !informationFromNodes.isEmpty()) {
            Map<String, String> map = (Map<String, String>) informationFromNodes.get(0);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String node = entry.getKey();
                if (node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if (mas.length == 3) {
                        community_nodes++;
                    }
                }
            }
        }

        return community_nodes;
    }

    public int num_Community_node(Map<String, String> info_nodes) {
        int community_nodes = 0;
        if (info_nodes != null) {
            Map<String, String> map = info_nodes;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String node = entry.getKey();
                if (node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if (mas.length == 3) {
                        community_nodes++;
                    }
                }
            }
        }

        return community_nodes;
    }

    public ArrayList<String[]> get_Community_Nodes_Version(Map<String, String> info_nodes) {
        ArrayList<String[]> result = new ArrayList();
        if (info_nodes != null) {
            for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
                String node = entry.getKey();
//                System.out.println("Get_Community_Nodes_Version: "+node+";"+entry.getValue());
                if (node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if (mas.length == 3) {
                        String[] mas1 = new String[3];
                        mas1[0] = node;
                        mas1[1] = mas[1];
                        mas1[2] = mas[2];
                        result.add(mas1);
                    }
                }
            }
        }
        return result;
    }

    //    public ArrayList<String[]> InspectLinks(ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String[]> result = new ArrayList();
//        
//        logger.Println("Checking consistent link.", logger.DEBUG);
//        // rename id_iface and name _iface
//        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
//        for(String[] link : links) {
//            String[] mas1 = GetRemotePort(link[3], link[4], link[5], walkInformationFromNodes, hash_ip);
//            if(!mas1[0].equals(link[4]) || !mas1[1].equals(link[5])) {
//                logger.Println("Rename id or name link: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+mas1[0]+","+mas1[1]+" from: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5], logger.DEBUG);
//            }
//            String[] mas = new String[6];
//            mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//            mas[3]=link[3];
//            mas[4]=mas1[0];
//            mas[5]=mas1[1];
//            result.add(mas);
//        }
//        
//        //delete duplicate link
//        ArrayList<String[]> result1 = new ArrayList();
//        for(int i=0; i<result.size(); i++) {
//            String[] link1=result.get(i);
//            boolean find=false;
//            for(int j=i+1; j<result.size(); j++) {
//                String[] link2=result.get(j);
//                if(
//                    (link1[0].equals(link2[0]) && link1[3].equals(link2[3])) && ( CheckInterfaceName(link1[2],link2[2]) || CheckInterfaceName(link1[5],link2[5]) ) ||
//                    (link1[3].equals(link2[0]) && link1[0].equals(link2[3])) && ( CheckInterfaceName(link1[2],link2[5]) || CheckInterfaceName(link1[5],link2[2]) )    
//                  ) {
//                    find=true;
//                    break;
//                }
//            }
//            if(!find) {
//                result1.add(link1);
//                logger.Println("Link added: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//            }
//            else logger.Println("Link duplicate: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//        }
//        
//        return result1;
//    }
    public void loggingArray(String[] mas, String prefix, int level) {
        logger.Print(new Date() + ":\t" + prefix + ": ", level);
        logger.Print(mas[0], level);
        for (int i = 1; i < mas.length; i++) {
            logger.Print(", " + mas[i], level);
        }
        logger.Print("\n", level);
    }

    //    public void GetInformationFromMapFile(String mapfile) {
//        ArrayList<String[]> nodes = ReadFromMapFile(mapfile, "^:nodes:$");
//        synchronized(Neb.links_info) { Neb.links_info.clear(); Neb.links_info = ReadFromMapFile(mapfile, "^:links:$"); }
//        ArrayList<String[]> hosts = ReadFromMapFile(mapfile, "^:hosts:$");
//        ArrayList<String[]> texts = ReadFromMapFile(mapfile, "^:texts:$");
//        ArrayList<String[]> custom_texts = ReadFromMapFile(mapfile, "^:custom_texts:$"); 
//        ArrayList<String[]> extend_info = ReadFromMapFile(mapfile, "^:extend_info:$");
//
//        synchronized(Neb.nodes_info) { Neb.nodes_info.clear(); for(String[] item : nodes) Neb.nodes_info.put(item[0], item); }
//        synchronized(Neb.mac_ArpMacTable) { Neb.mac_ArpMacTable.clear(); for(String[] item : hosts) Neb.mac_ArpMacTable.put(item[0], item); }
//        synchronized(Neb.ip_ArpMacTable) { Neb.ip_ArpMacTable.clear(); for(String[] item : hosts) Neb.ip_ArpMacTable.put(item[1], item); }
//        synchronized(Neb.text_info) { Neb.text_info.clear(); for(String[] item : texts) Neb.text_info.put(item[0], item); }
//        synchronized(Neb.text_custom_info) { Neb.text_custom_info.clear(); for(String[] item : custom_texts) Neb.text_custom_info.put(item[0], item); }
//        synchronized(Neb.extended_info) { Neb.extended_info.clear(); for(String[] item : extend_info) Neb.extended_info.put(item[0], item); }
//    }
//    public boolean WriteInformationToMapFile(String filename, Map<String, String[]> info_nodes, ArrayList<String[]> links, Map<String, String[]> ARP_MAC, Map<String, String[]> text, Map<String, String[]> text_custom, Map<String, String[]> extended_info) {
//        BufferedWriter outFile = null;
//        BufferedReader inFile = null;
//        File file = new File(filename);
//        try {
//            outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//            outFile.write(":nodes:\n");
//            for(Map.Entry<String, String[]> entry : info_nodes.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }
//            outFile.write(":links:\n");
//            for(String[] item : links) {
//                outFile.write(item[0]);
//                for(int i=1; i<item.length; i++) outFile.write(";"+item[i]);
//                outFile.write("\n");
//            }
//            outFile.write(":hosts:\n");
//            for(Map.Entry<String, String[]> entry : ARP_MAC.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }            
//            //extended information
//            outFile.write(":extend_info:\n");
//            for(Map.Entry<String, String[]> entry : extended_info.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }              
//            outFile.write(":text:\n");
//            for(Map.Entry<String, String[]> entry : text.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }               
//            outFile.write(":custom_texts:\n");
//            for(Map.Entry<String, String[]> entry : text_custom.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }               
//
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { 
//                if(DEBUG) System.out.println(ex);
//                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); 
//            }
//            
//            File file_tmp = new File(filename+".temp");
//            if(file_tmp.exists()) {
//                file.delete();
//                file_tmp.renameTo(file);
//            }
//            
//        } catch (Exception ex) {
//            if(DEBUG) System.out.println(ex);
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        } finally {
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            if(inFile != null) try {
//                inFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }            
//        }
//
//        return true;
//    }    
//    public void RunScripts(String cmd) {
//        try {
//            if (new File(cmd.replace("\"", "")).exists()) {
//                Process process = new ProcessBuilder(cmd, ru.kos.neb.neb_lib.Utils.master_key).start();
//                InputStream is = process.getInputStream();
//                InputStreamReader isr = new InputStreamReader(is);
//                BufferedReader br = new BufferedReader(isr);
//                String line;
//                while ((line = br.readLine()) != null) {
//                    logger.Println("Scripts: "+line, logger.DEBUG);
//                }
//            } else logger.Println("Scripts "+cmd+" not found!!!", logger.DEBUG);
//        } catch (IOException ex) {
//            if(DEBUG) System.out.println(ex);
//            logger.Println("Scripts: "+ex, logger.DEBUG);
//            ex.printStackTrace();
////            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//        }        
//    }
    public Process runUserScript(String cmd) {
        Process process = null;
        try {
            String[] param = parseCommandLine(cmd);
            ProcessBuilder pb = new ProcessBuilder(param);
            // redirect error to output
            pb.redirectErrorStream(true);
            process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                logger.Println("Scripts: " + line, logger.DEBUG);
                System.out.println(line);
            }

        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            logger.Println("Scripts: " + ex, logger.DEBUG);
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return process;
    }

    public Process runUserScriptNoOutput(String cmd) {
        Process process = null;
        try {
            String[] param = parseCommandLine(cmd);
            ProcessBuilder pb = new ProcessBuilder(param);
            process = pb.start();

        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            logger.Println("Scripts: " + ex, logger.DEBUG);
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return process;
    }

    public String[] parseCommandLine(String str) {
        Pattern p = Pattern.compile("\"(.+?)\"");
        Matcher m = p.matcher(str);
        ArrayList<String> list = new ArrayList();
        while (m.find()) {
            list.add(m.group(1));
        }
        for (String item : list) {
            String item_new = item.replaceAll("\\s+", "<blank>");
            str = str.replace(item, item_new);
        }

        String[] arr = str.split("\\s+");
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].replace("<blank>", " ");
        }

        return arr;
    }

    @SuppressWarnings("SleepWhileInLoop")
    public boolean writeStrToFile(String filename, String str, long delay) {
        // delay sec
        Date d = new Date();
        long start_time = d.getTime();
        boolean is_ok = false;
        while (true) {
            try {
                try (Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
                    str = str.replace("\\u003d", "=");
                    outFile.write(str);
                }
                is_ok = true;
                break;
            } catch (IOException ex) {
                d = new Date();
                if (d.getTime() - start_time > delay * 1000) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (java.lang.InterruptedException e) {
                }
            }
        }
        return is_ok;
    }

    public boolean appendStrToFile(String filename, String str) {
        try {
//            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            try (Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), StandardCharsets.UTF_8))) {
                //            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
                outFile.write(str);
            }
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean writeArrayListToFile(String filename, ArrayList<String[]> list) {
        try {
            try (BufferedWriter outFile = new BufferedWriter(new FileWriter(filename))) {
                for (String[] item : list) {
                    outFile.write(item[0]);
                    for (int ii = 1; ii < item.length; ii++) {
                        outFile.write("," + item[ii]);
                    }
                    outFile.write("\n");
                }
            }
            return true;
        } catch (IOException ex) {
//            if(DEBUG) System.out.println(ex);
//            ex.printStackTrace();
//            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean writeArrayListToFile1(String filename, ArrayList<ArrayList<String>> list) {
        try {
            try (BufferedWriter outFile = new BufferedWriter(new FileWriter(filename))) {
                for (ArrayList<String> item : list) {
                    outFile.write(item.get(0));
                    for (int ii = 1; ii < item.size(); ii++) {
                        outFile.write("," + item.get(ii));
                    }
                    outFile.write("\n");
                }
            }
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean writeHashMapToFile(String filename, Map<String, String> map) {
        try {
            try (BufferedWriter outFile = new BufferedWriter(new FileWriter(filename))) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String node = entry.getKey();
                    String val = entry.getValue();
                    outFile.write(node + ";" + val + "\n");
                }
            }
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public Map normalizeMap2(Map<String, Map> info_map) {
        Gson gson = new Gson();

        // remove mac if am link to new node
        for (Map.Entry<String, Map> area : info_map.entrySet()) {
            Map area_info = area.getValue();
            ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
            ArrayList<String[]> mac_ip_port_new = new ArrayList();
            if (mac_ip_port != null) {
                for (String[] mip : mac_ip_port) {
                    String node = mip[2];
                    String iface = mip[4];
                    boolean find = false;
                    for (ArrayList<String> link : links) {
                        String node1 = link.get(0);
                        String iface1 = link.get(2);
                        String node2 = link.get(3);
                        String iface2 = link.get(5);
                        if (node1.equals(node) && equalsIfaceName(iface1, iface)) {
                            String[] mas = new String[6];
                            mas[0] = mip[0];
                            mas[1] = mip[1];
                            mas[2] = node2;
                            mas[3] = "";
                            mas[4] = "unknown";
                            mas[5] = "1";
                            mac_ip_port_new.add(mas);
                            logger.Println("Move mac/ip: " + mas[0] + "/" + mas[1] + " from: " + node + " " + iface + " to: " + node2 + " unknown", logger.DEBUG);
                            find = true;
                            break;
                        } else if (node2.equals(node) && equalsIfaceName(iface2, iface)) {
                            String[] mas = new String[6];
                            mas[0] = mip[0];
                            mas[1] = mip[1];
                            mas[2] = node1;
                            mas[3] = "";
                            mas[4] = "unknown";
                            mas[5] = "1";
                            mac_ip_port_new.add(mas);
                            logger.Println("Move mac/ip: " + mas[0] + "/" + mas[1] + " from: " + node + " " + iface + " to: " + node1 + " unknown", logger.DEBUG);
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        mac_ip_port_new.add(mip);
                    }
                }
            }
            area_info.put("mac_ip_port", mac_ip_port_new);
        }
/////////////////////////////////////////////////////////////////////////
        info_map = Neb.utils.removeDuplicateNodes(info_map, Neb.area_networks);

        return info_map;
    }

    public Map<String, ArrayList<String>> getNodeNeightboards(ArrayList<ArrayList<String>> links) {
        Map<String, ArrayList<String>> result = new HashMap();
        for (ArrayList<String> link : links) {
            String node1 = link.get(0);
            String node2 = link.get(3);

            if (result.get(node1) != null) {
                result.get(node1).add(node2);
            } else {
                ArrayList list = new ArrayList();
                list.add(node2);
                result.put(node1, list);
            }
            if (result.get(node2) != null) {
                result.get(node2).add(node1);
            } else {
                ArrayList list = new ArrayList();
                list.add(node1);
                result.put(node2, list);
            }

        }
        return result;
    }

    private Map<String, ArrayList<String>> getNodeNeightboards(ArrayList<ArrayList<String>> links, ArrayList<String> node_list) {
        Map<String, ArrayList<String>> result = new HashMap();
        for (ArrayList<String> link : links) {
            String node1 = link.get(0);
            String node2 = link.get(3);
            if (node_list.isEmpty()) {
                if (result.get(node1) != null) {
                    result.get(node1).add(node2);
                } else {
                    ArrayList list = new ArrayList();
                    list.add(node2);
                    result.put(node1, list);
                }
                if (result.get(node2) != null) {
                    result.get(node2).add(node1);
                } else {
                    ArrayList list = new ArrayList();
                    list.add(node1);
                    result.put(node2, list);
                }
            } else {
                if (node_list.contains(node1)) {
                    if (result.get(node1) != null) {
                        result.get(node1).add(node2);
                    } else {
                        ArrayList list = new ArrayList();
                        list.add(node2);
                        result.put(node1, list);
                    }
                }
                if (node_list.contains(node2)) {
                    if (result.get(node2) != null) {
                        result.get(node2).add(node1);
                    } else {
                        ArrayList list = new ArrayList();
                        list.add(node1);
                        result.put(node2, list);
                    }
                }
            }
        }
        return result;
    }

//    private ArrayList<String> getOneStepConnectivity(ArrayList<ArrayList<String>> links, ArrayList<String> node_list, ArrayList<String> nodes_exclude) {
//        ArrayList<String> node_list_out = new ArrayList();
//        Map<String, ArrayList<String>> node_neightboards = getNodeNeightboards(links, node_list);
//        for (Map.Entry<String, ArrayList<String>> item : node_neightboards.entrySet()) {
//            String node = item.getKey();
//            if (!nodes_exclude.contains(node)) {
//                nodes_exclude.add(node);
//                ArrayList<String> neightboards = item.getValue();
//                for (String neightboard : neightboards) {
//                    if (!node_list_out.contains(neightboard) && !nodes_exclude.contains(neightboard)) {
//                        node_list_out.add(neightboard);
//                    }
//                }
//            }
//            if (node_list.isEmpty()) {
//                break;
//            }
//        }
//        ArrayList result = new ArrayList();
//        if (!nodes_exclude.isEmpty()) {
//            result = node_list_out;
//        }
//        return result;
//    }

//    private ArrayList<ArrayList<String>> correctLinksExclude(ArrayList<ArrayList<String>> links, ArrayList<String> node_list) {
//        ArrayList<ArrayList<String>> result = new ArrayList();
//        for (ArrayList<String> link : links) {
//            String node1 = link.get(0);
//            String node2 = link.get(3);
//            if (!(node_list.contains(node1) || node_list.contains(node2))) {
//                result.add(link);
//            }
//        }
//        return result;
//    }

    //    private ArrayList<ArrayList<String>> CorrectLinksInclude(ArrayList<ArrayList<String>> links, ArrayList<String> node_list) {
//        ArrayList<ArrayList<String>> result = new ArrayList();
//        for(ArrayList<String> link : links) {
//            String node1 = link.get(0);
//            String node2 = link.get(3);    
//            if(node_list.contains(node1) || node_list.contains(node2))
//                result.add(link);
//        }
//        return result;
//    }    
    private ArrayList<ArrayList<String>> removeLinks(ArrayList<ArrayList<String>> links, ArrayList<String> link) {
        ArrayList<ArrayList<String>> result = new ArrayList();
        for (ArrayList<String> item : links) {
            if (!(item.get(0).equals(link.get(0)) && item.get(1).equals(link.get(1)) && item.get(2).equals(link.get(2))
                    && item.get(3).equals(link.get(3)) && item.get(4).equals(link.get(4)) && item.get(5).equals(link.get(5)))) {
                result.add(item);
            }
        }
        return result;
    }

//    private ArrayList<String> getConnectivity(ArrayList<ArrayList<String>> links) {
//        ArrayList<String> node_list = new ArrayList();
//        ArrayList<String> nodes_exclude = new ArrayList();
//        while (true) {
//            ArrayList res = getOneStepConnectivity(links, node_list, nodes_exclude);
//            node_list = (ArrayList<String>) res;
//            if (node_list.isEmpty()) {
//                break;
//            }
//        }
//        return nodes_exclude;
//    }

    public Integer getDistance(ArrayList<ArrayList<String>> links, String cel_node) {
        ArrayList<String> node_list = new ArrayList();
        ArrayList<String> nodes_exclude = new ArrayList();
        int i = 1;
        while (true) {
            ArrayList res = getOneStepDistance(links, node_list, nodes_exclude, cel_node);
            node_list = (ArrayList<String>) res;
            if (node_list.isEmpty()) {
                break;
            }
            i = i + 1;
        }
        return i;
    }

    private ArrayList<String> getOneStepDistance(ArrayList<ArrayList<String>> links, ArrayList<String> node_list, ArrayList<String> nodes_exclude, String cel_node) {
        ArrayList result = new ArrayList();
        ArrayList<String> node_list_out = new ArrayList();
        Map<String, ArrayList<String>> node_neightboards = getNodeNeightboards(links, node_list);
        for (Map.Entry<String, ArrayList<String>> item : node_neightboards.entrySet()) {
            String node = item.getKey();
            if (node.equals(cel_node)) {
                return result;
            }
            if (!nodes_exclude.contains(node)) {
                nodes_exclude.add(node);
                ArrayList<String> neightboards = item.getValue();
                for (String neightboard : neightboards) {
                    if (neightboard.equals(cel_node)) {
                        return result;
                    }
                    if (!node_list_out.contains(neightboard) && !nodes_exclude.contains(neightboard)) {
                        node_list_out.add(neightboard);
                    }
                }
            }
            if (node_list.isEmpty()) {
                break;
            }
        }

        if (!nodes_exclude.isEmpty()) {
            result = node_list_out;
        }
        return result;
    }

    public Map normalizeMap3(Map<String, Map> info_map) {
        for (Map.Entry<String, Map> area : info_map.entrySet()) {
            Map area_info = area.getValue();
            ArrayList<String[]> mac_ip_port = (ArrayList) area_info.get("mac_ip_port");
            if (mac_ip_port != null) {
                ArrayList<String[]> mac_ip_port_new = new ArrayList();
                for (String[] mip : mac_ip_port) {
                    boolean correct = true;
                    for (String it : mip) {
                        if (it == null) {
                            correct = false;
                            break;
                        }
                    }
                    if (correct) {
                        mac_ip_port_new.add(mip);
                    }
                }
                area_info.put("mac_ip_port", mac_ip_port_new);
            }
        }

        Map area_node_info_brief = getNodeInfoBrief(info_map);

        // if node not links. Adding link from mac_ip_ports
        for (Map.Entry<String, Map> item : info_map.entrySet()) {
            String area_name = item.getKey();
            Map area_info = item.getValue();
            ArrayList<ArrayList<String>> links = (ArrayList) area_info.get("links");
            Map<String, Map> nodes_information = (Map) area_info.get("nodes_information");
            ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
            Map<String, String[]> mac_node_iface = new HashMap();
            Map<String, String[]> ip_node_iface = new HashMap();
            ArrayList<String[]> selected_list = new ArrayList();
            int pos = 0;
            if(mac_ip_port != null) {
                for (String[] mip : mac_ip_port) {
                    String[] mas = new String[4];
                    mas[0] = mip[2];
                    mas[1] = mip[3];
                    mas[2] = mip[4];
                    mas[3] = String.valueOf(pos);
                    mac_node_iface.put(mip[0].replaceAll("[:.-]", "").toLowerCase(), mas);
                    pos++;
                }
                pos = 0;
                for (String[] mip : mac_ip_port) {
                    String[] mas = new String[4];
                    mas[0] = mip[2];
                    mas[1] = mip[3];
                    mas[2] = mip[4];
                    mas[3] = String.valueOf(pos);
                    ip_node_iface.put(mip[1], mas);
                    pos++;
                }


                Map<String, String> nodes_with_links = new HashMap();
                for (ArrayList<String> link : links) {
                    nodes_with_links.put(link.get(0), link.get(0));
                    nodes_with_links.put(link.get(3), link.get(3));
                }

                ArrayList<String> nodes_without_links = new ArrayList();
                for (Map.Entry<String, Map> item1 : nodes_information.entrySet()) {
                    String node = item1.getKey();
                    if (nodes_with_links.get(node) == null) {
                        nodes_without_links.add(node);
                    }
                }

                for (String node : nodes_without_links) {
                    if (area_node_info_brief.get(area_name) != null &&
                            ((Map) area_node_info_brief.get(area_name)).get(node) != null) {
                        String mac_base = (String) ((Map) ((Map) area_node_info_brief.get(area_name)).get(node)).get("mac_base");
                        ArrayList<String> mac_list = (ArrayList) ((Map) ((Map) area_node_info_brief.get(area_name)).get(node)).get("mac_list");
                        if (mac_list != null && mac_base != null) {
                            mac_list.add(mac_base);
                        }
                        ArrayList<String> ip_list = (ArrayList) ((Map) ((Map) area_node_info_brief.get(area_name)).get(node)).get("ip_list");
                        boolean find = false;
                        if (mac_list != null) {
                            for (String mac : mac_list) {
                                mac.replaceAll("[:.-]", "").toLowerCase();
                                if (mac_node_iface.get(mac) != null) {
                                    String[] mas = new String[5];
                                    mas[0] = node;
                                    mas[1] = mac_node_iface.get(mac)[0];
                                    mas[2] = mac_node_iface.get(mac)[1];
                                    mas[3] = mac_node_iface.get(mac)[2];
                                    mas[4] = mac_node_iface.get(mac)[3];
                                    selected_list.add(mas);
                                    find = true;
                                    break;
                                }
                            }
                        }
                        if (!find && ip_list != null) {
                            for (String ip : ip_list) {
                                if (ip_node_iface.get(ip) != null) {
                                    String[] mas = new String[5];
                                    mas[0] = node;
                                    mas[1] = ip_node_iface.get(ip)[0];
                                    mas[2] = ip_node_iface.get(ip)[1];
                                    mas[3] = ip_node_iface.get(ip)[2];
                                    mas[4] = ip_node_iface.get(ip)[3];
                                    selected_list.add(mas);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!selected_list.isEmpty()) {
                    class MaxToMinPosition implements Comparator {
                        public int compare(Object obj1, Object obj2) {
                            Integer pos1 = Integer.parseInt(((String[]) obj1)[4]);
                            Integer pos2 = Integer.parseInt(((String[]) obj2)[4]);
                            // Compare the objects
                            if (pos1 > pos2) return -1;
                            if (pos1 < pos2) return 1;
                            return 0;
                        }
                    }
                    Comparator maxToMinPosition = new MaxToMinPosition();

                    Collections.sort(selected_list, maxToMinPosition);

                    for (String[] it : selected_list) {
                        mac_ip_port.remove(Integer.parseInt(it[4]));
                        ArrayList<String> link = new ArrayList();
                        link.add(it[1]);
                        link.add(it[2]);
                        link.add(it[3]);
                        link.add(it[0]);
                        link.add("");
                        link.add("unknown");
                        link.add("mip");
                        links.add(link);
                    }
                }
            }
        }


        // remove nodes if not links
        for (Map.Entry<String, Map> item : info_map.entrySet()) {
            String area_name = item.getKey();
            System.out.println(area_name);
            Map area_info = item.getValue();
            ArrayList<ArrayList<String>> links = (ArrayList) area_info.get("links");
            Map<String, Map> nodes_information = (Map) area_info.get("nodes_information");
            Map<String, String> nodes_with_links = new HashMap();
            if(links != null && nodes_information != null) {
                for (ArrayList<String> link : links) {
                    nodes_with_links.put(link.get(0), link.get(0));
                    nodes_with_links.put(link.get(3), link.get(3));
                }

                ArrayList<String> remove_nodes_list = new ArrayList();
                for (Map.Entry<String, Map> item1 : nodes_information.entrySet()) {
                    String node = item1.getKey();
                    if (nodes_with_links.get(node) == null) {
                        remove_nodes_list.add(node);
                    }
                }
                for (String node : remove_nodes_list) {
                    nodes_information.remove(node);
                }
                area_info.put("nodes_information", nodes_information);
            }
        }

        return info_map;
    }

    public Map normalizeMapHub(Map<String, Map> info_map) {
        int iteration = 0;
        while (true) {
            if (!reorderAndHub(info_map)) {
                break;
            }
            System.out.println("iteration = " + iteration);
            iteration = iteration + 1;
        }
        return info_map;
    }

    private boolean reorderAndHub(Map<String, Map> info_map) {
        boolean result = false;
//        JSONParser parser = new JSONParser();

        /////////////////////////////////////////////////////////////////////////
        // fix errors cdp or lldp protocols
        Map<String, ArrayList<ArrayList<ArrayList<String>>>> area_links_debatables_list = new HashMap();
        for (Map.Entry<String, Map> area : info_map.entrySet()) {
            String area_name = area.getKey();
            Map area_info = area.getValue();
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
            ArrayList<ArrayList<ArrayList<String>>> links_debatables_list = new ArrayList();
            for (int i = 0; i < links.size(); i++) {
                ArrayList<String> link1 = links.get(i);
                String node2 = link1.get(3);
                String iface2 = link1.get(5);
                ArrayList<ArrayList<String>> links_debatables = new ArrayList();
                links_debatables.add(link1);
                for (int j = i + 1; j < links.size(); j++) {
                    ArrayList<String> link2 = links.get(j);
                    String node2_sec = link2.get(3);
                    String iface2_sec = link2.get(5);
                    if (node2.equals(node2_sec) && equalsIfaceName(iface2, iface2_sec)) {
                        links_debatables.add(link2);
                        logger.Print("Errors cdl or lldp: ", logger.DEBUG);
                        for (String item : link1) {
                            logger.Print(item + ", ", logger.DEBUG);
                        }
                        logger.Print(" <---> ", logger.DEBUG);
                        for (String item : link2) {
                            logger.Print(item + ", ", logger.DEBUG);
                        }
                        logger.Println("", logger.DEBUG);
                    }
                }
                if (links_debatables.size() > 1) {
                    links_debatables_list.add(links_debatables);
                }
            }
            if (!links_debatables_list.isEmpty()) {
                area_links_debatables_list.put(area_name, links_debatables_list);
            }
        }

        // remove debatables links
        for (Map.Entry<String, ArrayList<ArrayList<ArrayList<String>>>> area : area_links_debatables_list.entrySet()) {
            String area_name = area.getKey();
            ArrayList<ArrayList<ArrayList<String>>> links_debatables_list = area.getValue();
//            ArrayList<ArrayList<String>> links = new ArrayList();
            ArrayList<ArrayList<String>> links = new ArrayList();
            if (info_map.get(area_name) != null && info_map.get(area_name).get("links") != null) {
                links = (ArrayList<ArrayList<String>>) info_map.get(area_name).get("links");
            }
            for (ArrayList<ArrayList<String>> links_debatables : links_debatables_list) {
                result = true;
                int score = Integer.MAX_VALUE;
                int pos = -1;
                int i = 0;
                for (ArrayList<String> link : links_debatables) {
                    String node = link.get(0);
                    if (!links.isEmpty()) {
                        int distance = getDistance(links, node);
                        if (distance < score) {
                            score = distance;
                            pos = i;
                        }
                    }
                    i = i + 1;
                }
                if (pos != -1) {
//                    System.out.println("pos="+pos);
                    int ii = 0;
                    for (ArrayList<String> link : links_debatables) {
                        if (ii != pos) {
                            links = removeLinks(links, link);
                            logger.Println("Remove debatable link: " + link.get(0) + ", " + link.get(2) + ", " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                        ii = ii + 1;
                    }
                } else {
                    logger.Println("pos not select.", logger.DEBUG);
                    for (int ii = 0; ii < links_debatables.size() - 1; ii++) {
                        ArrayList<String> link = links_debatables.get(ii);
                        links = removeLinks(links, link);
                        logger.Println("Remove debatable link:: " + link.get(0) + ", " + link.get(2) + ", " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                    }
                }
            }
            if (info_map.get(area_name) != null && info_map.get(area_name).get("links") != null) {
                ((ArrayList<ArrayList<String>>) (info_map.get(area_name).get("links"))).clear();
                ((ArrayList<ArrayList<String>>) (info_map.get(area_name).get("links"))).addAll(links);
            }
        }

        //////// reorder links /////////////////////
        Gson gson = new Gson();
        for (Map.Entry<String, Map> area : info_map.entrySet()) {
            String area_name = area.getKey();
            System.out.println("Area: " + area_name);
            Map area_info = area.getValue();
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
            Map<String, Map> nodes_information = (Map<String, Map>) area_info.get("nodes_information");
            Map<String, ArrayList<String[]>> hubs = getHubStruct(links);
            if (!hubs.isEmpty()) {
                // find duplicate node;
                for (Map.Entry<String, ArrayList<String[]>> entry : hubs.entrySet()) {
                    String node_iface = entry.getKey();
                    ArrayList<String[]> children_list = entry.getValue();
                    if (children_list.size() == 2) {
                        String type1 = children_list.get(0)[3];
                        String type2 = children_list.get(1)[3];
                        String iface1 = children_list.get(0)[2];
                        String iface2 = children_list.get(1)[2];
                        String node1 = children_list.get(0)[0];
                        String node2 = children_list.get(1)[0];

                        if (((type1.equals("cdp") && type2.equals("lldp"))
                                || (type1.equals("lldp") && type2.equals("cdp"))
                                && equalsIfaceName(iface1, iface2)) || equalsNode(node1, node2)) {
                            result = true;
                            logger.Println("Duplicate node: " + node1 + ", " + iface1 + " --- " + node2 + ", " + iface2, logger.DEBUG);
                            if (node1.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !node2.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                if (nodes_information.get(node2) != null) {
                                    nodes_information.remove(node2);
                                    logger.Println("Remove duplicate node: " + node2, logger.DEBUG);
                                }
                                String[] mas = node_iface.split(";");
                                if (mas.length == 3) {
                                    ArrayList<String> link = new ArrayList();
                                    link.add(mas[0]);
                                    link.add(mas[1]);
                                    link.add(mas[2]);
                                    link.add(children_list.get(1)[0]);
                                    link.add(children_list.get(1)[1]);
                                    link.add(children_list.get(1)[2]);
                                    if (delLink(link, links)) {
                                        logger.Println("Delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    } else {
                                        logger.Println("Error delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    }
                                }
                            } else if (!node1.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && node2.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                if (nodes_information.get(node1) != null) {
                                    nodes_information.remove(node1);
                                    logger.Println("Remove duplicate node: " + node1, logger.DEBUG);
                                }
                                String[] mas = node_iface.split(";");
                                if (mas.length == 3) {
                                    ArrayList<String> link = new ArrayList();
                                    link.add(mas[0]);
                                    link.add(mas[1]);
                                    link.add(mas[2]);
                                    link.add(children_list.get(0)[0]);
                                    link.add(children_list.get(0)[1]);
                                    link.add(children_list.get(0)[2]);
                                    if (delLink(link, links)) {
                                        logger.Println("Delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    } else {
                                        logger.Println("Error delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    }
                                }
                            } else if (node1.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && node2.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")
                                    || !node1.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !node2.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String str1 = gson.toJson(nodes_information.get(node1));
                                String str2 = gson.toJson(nodes_information.get(node2));

                                if (str1.length() >= str2.length()) {
                                    if (nodes_information.get(node2) != null) {
                                        nodes_information.remove(node2);
                                        logger.Println("Remove duplicate node: " + node2, logger.DEBUG);
                                    }
                                    String[] mas = node_iface.split(";");
                                    if (mas.length == 3) {
                                        ArrayList<String> link = new ArrayList();
                                        link.add(mas[0]);
                                        link.add(mas[1]);
                                        link.add(mas[2]);
                                        link.add(children_list.get(1)[0]);
                                        link.add(children_list.get(1)[1]);
                                        link.add(children_list.get(1)[2]);
                                        if (delLink(link, links)) {
                                            logger.Println("Delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                        } else {
                                            logger.Println("Error delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                        }
                                    }
                                } else {
                                    if (nodes_information.get(node1) != null) {
                                        nodes_information.remove(node1);
                                        logger.Println("Remove duplicate node: " + node1, logger.DEBUG);
                                    }
                                    String[] mas = node_iface.split(";");
                                    if (mas.length == 3) {
                                        ArrayList<String> link = new ArrayList();
                                        link.add(mas[0]);
                                        link.add(mas[1]);
                                        link.add(mas[2]);
                                        link.add(children_list.get(0)[0]);
                                        link.add(children_list.get(0)[1]);
                                        link.add(children_list.get(0)[2]);
                                        if (delLink(link, links)) {
                                            logger.Println("Delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                        } else {
                                            logger.Println("Error delete link for duplicate node: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            hubs = getHubStruct(links);
            if (!hubs.isEmpty()) {
                // reorder link for leaving hub (one node cdp, lldp other not)
                for (Map.Entry<String, ArrayList<String[]>> entry : hubs.entrySet()) {
                    String node_iface = entry.getKey();
                    ArrayList<String[]> children_list = entry.getValue();
                    int count_dp = 0;
                    for (String[] children : children_list) {
                        if (children[3].equals("cdp") || children[3].equals("lldp")) {
                            count_dp = count_dp + 1;
                        }
                    }
                    if (count_dp == 1) {
                        result = true;
                        String[] key_node = new String[6];
                        ArrayList<String[]> reorder_node = new ArrayList();
                        for (String[] children : children_list) {
                            if (children[3].equals("cdp") || children[3].equals("lldp")) {
                                key_node = children;
                            } else {
                                reorder_node.add(children);
                            }
                        }
                        logger.Println("Hubs one cdp or lldp size = 2: " + node_iface, logger.DEBUG);
                        logger.Println("\tkey_node: " + key_node[0] + ", " + key_node[2], logger.DEBUG);
                        for (String[] iter : reorder_node) {
                            logger.Println("\treorder_node: " + iter[0] + ", " + iter[2], logger.DEBUG);
                        }
                        for (String[] iter : reorder_node) {
                            String[] mas = node_iface.split(";");
                            if (mas.length == 3) {
                                ArrayList<String> link = new ArrayList();
                                link.add(mas[0]);
                                link.add(mas[1]);
                                link.add(mas[2]);
                                link.add(iter[0]);
                                link.add(iter[1]);
                                link.add(iter[2]);
                                if (delLink(link, links)) {
                                    logger.Println("Delete link reorder cdp or lldp and not: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                } else {
                                    logger.Println("Error delete link reorder cdp or lldp and not: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                }
                            }
                            ArrayList<String> link = new ArrayList();
                            link.add(key_node[0]);
                            link.add("");
                            link.add("unknown");
                            link.add(iter[0]);
                            link.add(iter[1]);
                            link.add(iter[2]);
                            link.add(iter[5]);
                            addLink(link, links);
                            logger.Println("Add link reorder cdp or lldp and not: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                    }
                }
            }

            hubs = getHubStruct(links);
            if (!hubs.isEmpty()) {
                // reorder link for leaving hub (interface include 'radio', 'wan' or 'wlan')
                ArrayList<String> endpoint_iface_shablon = (ArrayList<String>) Neb.cfg.get("endpoint_iface_shablon");
                if (endpoint_iface_shablon != null && !endpoint_iface_shablon.isEmpty()) {
                    for (Map.Entry<String, ArrayList<String[]>> entry : hubs.entrySet()) {
                        String node_iface = entry.getKey();
                        ArrayList<String[]> children_list = entry.getValue();
                        int count_non_radio = 0;
                        for (String[] children : children_list) {
                            String iface = children[2].toLowerCase();
                            boolean found = false;
                            for (String shablon : endpoint_iface_shablon) {
                                if (iface.matches(shablon.toLowerCase())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                count_non_radio = count_non_radio + 1;
                            }
                        }

                        if (count_non_radio == 1) {
                            result = true;
                            String[] key_node = new String[6];
                            ArrayList<String[]> reorder_node = new ArrayList();
                            for (String[] children : children_list) {
                                String iface = children[2].toLowerCase();
                                boolean found = false;
                                for (String shablon : endpoint_iface_shablon) {
                                    if (iface.matches(shablon.toLowerCase())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    key_node = children;
                                } else {
                                    reorder_node.add(children);
                                }
                            }
                            logger.Println("Hubs name iface math wan(non wan 1): " + node_iface, logger.DEBUG);
                            logger.Println("\tkey_node: " + key_node[0] + ", " + key_node[2], logger.DEBUG);
                            for (String[] iter : reorder_node) {
                                logger.Println("\treorder_node: " + iter[0] + ", " + iter[2], logger.DEBUG);
                            }

                            for (String[] iter : reorder_node) {
                                String[] mas = node_iface.split(";");
                                if (mas.length == 3) {
                                    ArrayList<String> link = new ArrayList();
                                    link.add(mas[0]);
                                    link.add(mas[1]);
                                    link.add(mas[2]);
                                    link.add(iter[0]);
                                    link.add(iter[1]);
                                    link.add(iter[2]);
                                    if (delLink(link, links)) {
                                        logger.Println("Delete link reorder endpoint_iface_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    } else {
                                        logger.Println("Error delete link reorder endpoint_iface_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                    }
                                }
                                ArrayList<String> link = new ArrayList();
                                link.add(key_node[0]);
                                link.add("");
                                link.add("unknown");
                                link.add(iter[0]);
                                link.add(iter[1]);
                                link.add(iter[2]);
                                link.add(iter[5]);
                                addLink(link, links);
                                logger.Println("Add link reorder endpoint_iface_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                            }
                        } else if (count_non_radio > 1) {
                            int count_radio = 0;
                            for (String[] children : children_list) {
                                String iface = children[2].toLowerCase();
                                boolean found = false;
                                for (String shablon : endpoint_iface_shablon) {
                                    if (iface.matches(shablon.toLowerCase())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    count_radio = count_radio + 1;
                                }
                            }
                            if (count_radio > 0) {
                                result = true;
                                addHubsWlan(node_iface, children_list, nodes_information, links);
                            }
                        }
                    }
                }
            }

            hubs = getHubStruct(links);
            if (!hubs.isEmpty()) {
                // reorder link for leaving hub (info  include 'air', 'camera')
                ArrayList<String> endpoint_shablon = (ArrayList<String>) Neb.cfg.get("endpoint_shablon");
                for (Map.Entry<String, ArrayList<String[]>> entry : hubs.entrySet()) {
                    String node_iface = entry.getKey();
                    ArrayList<String[]> children_list = entry.getValue();
                    int count = 0;
                    for (String[] children : children_list) {
                        String info = children[5].toLowerCase();
                        boolean found = false;
                        for (String shablon : endpoint_shablon) {
                            if (info.matches(shablon.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            count = count + 1;
                        }
                    }
                    if (count == 1) {
                        result = true;
                        String[] key_node = new String[6];
                        ArrayList<String[]> reorder_node = new ArrayList();
                        for (String[] children : children_list) {
                            String info = children[5].toLowerCase();
                            boolean found = false;
                            for (String shablon : endpoint_shablon) {
                                if (info.matches(shablon.toLowerCase())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                key_node = children;
                            } else {
                                reorder_node.add(children);
                            }
                        }
                        logger.Println("Hubs sysname match endpoint(non endpoint 1): " + node_iface, logger.DEBUG);
                        logger.Println("\tkey_node: " + key_node[0] + ", " + key_node[2], logger.DEBUG);
                        for (String[] iter : reorder_node) {
                            logger.Println("\treorder_node: " + iter[0] + ", " + iter[2], logger.DEBUG);
                        }
                        for (String[] iter : reorder_node) {
                            String[] mas = node_iface.split(";");
                            if (mas.length == 3) {
                                ArrayList<String> link = new ArrayList();
                                link.add(mas[0]);
                                link.add(mas[1]);
                                link.add(mas[2]);
                                link.add(iter[0]);
                                link.add(iter[1]);
                                link.add(iter[2]);
                                if (delLink(link, links)) {
                                    logger.Println("Delete link reorder endpoint_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                } else {
                                    logger.Println("Error delete link reorder endpoint_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                                }
                            }
                            ArrayList<String> link = new ArrayList();
                            link.add(key_node[0]);
                            link.add("");
                            link.add("unknown");
                            link.add(iter[0]);
                            link.add(iter[1]);
                            link.add(iter[2]);
                            link.add(iter[5]);
                            addLink(link, links);
                            logger.Println("Add link reorder endpoint_shablon: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                    }
                }
            }

            ///////////// find hubs ///////////////
            hubs = getHubStruct(links);
            if (!hubs.isEmpty()) {
                result = true;
                addHubs(hubs, nodes_information, links);
            }

        }

        return result;
    }

    private boolean addHubs(Map<String, ArrayList<String[]>> hubs, Map<String, Map> nodes_information, ArrayList<ArrayList<String>> links) {
        boolean result = false;
        for (Map.Entry<String, ArrayList<String[]>> entry : hubs.entrySet()) {
            String node_iface = entry.getKey();
            ArrayList<String[]> children_list = entry.getValue();
            logger.Println("Hub: " + node_iface, logger.DEBUG);
            for (String[] children : children_list) {
                logger.Println("\t- " + children[0] + ", " + children[2] + ", " + children[5], logger.DEBUG);
            }

            String str = node_iface;
//            ArrayList<String> sibling_list = new ArrayList();
//            for(String[] children : children_list) {
//                sibling_list.add(children[0]);
//            }
//            Collections.sort(sibling_list);
//            for(String sibling : sibling_list) {
//                str = str + sibling;
//            }            
            String hash_code = String.valueOf(Math.abs(str.hashCode()));
            String hub_name = "hub_" + hash_code;
            if (hash_code.length() > 8) {
                hub_name = "hub_" + hash_code.substring(0, 8);
            }
            String[] parrent = node_iface.split(";");

            // add hub
            Map<String, String> general1 = new HashMap();
            general1.put("sysname", "");
            general1.put("model", "hub");
            Map node_map = new HashMap();
            node_map.put("general", general1);
            node_map.put("image", Neb.hub_image);
            nodes_information.put(hub_name, node_map);
            logger.Println("Add hub: " + hub_name, logger.DEBUG);

            // remove links
            for (String[] children : children_list) {
                ArrayList<String> link = new ArrayList();
                link.add(parrent[0]);
                link.add("");
                link.add(parrent[2]);
                link.add(children[0]);
                link.add("");
                link.add(children[2]);
                if (delLink(link, links)) {
                    logger.Println("Delete link find hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                } else {
                    logger.Println("Error delete link find hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                }

            }

            // add links parrent to hub
            ArrayList<String> link = new ArrayList();
            link.add(parrent[0]);
            link.add(parrent[1]);
            link.add(parrent[2]);
            link.add(hub_name);
            link.add("unknown");
            link.add("unknown");
            link.add("parent_hub");
            addLink(link, links);
            logger.Println("Add link find hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);

            // add links hub to neightors
            for (String[] children : children_list) {
                link = new ArrayList();
                link.add(hub_name);
                link.add("unknown");
                link.add("unknown");
                link.add(children[0]);
                link.add(children[1]);
                link.add(children[2]);
                link.add("hub_link");
                addLink(link, links);
                logger.Println("Add link find hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
            }
            result = true;
        }
        return result;
    }

    private boolean addHubsWlan(String node_iface, ArrayList<String[]> children_list, Map<String, Map> nodes_information, ArrayList<ArrayList<String>> links) {
        boolean result = false;
        ArrayList<String> endpoint_iface_shablon = (ArrayList<String>) Neb.cfg.get("endpoint_iface_shablon");
        if (endpoint_iface_shablon != null && !endpoint_iface_shablon.isEmpty()) {
            logger.Println("Wlan_hub: " + node_iface, logger.DEBUG);
            boolean found = false;
            for (String[] children : children_list) {
                logger.Println("\t- " + children[0] + ", " + children[2] + ", " + children[5], logger.DEBUG);
                String iface = children[2].toLowerCase();
                boolean found1 = false;
                for (String shablon : endpoint_iface_shablon) {
                    if (iface.matches(shablon.toLowerCase())) {
                        found1 = true;
                        break;
                    }
                }
                if (found1) {
                    found = true;
                }
            }
            if (found) {
                // add wan hub
                StringBuilder str = new StringBuilder(node_iface);
                ArrayList<String> sibling_list = new ArrayList();
                for (String[] children : children_list) {
                    sibling_list.add(children[0]);
                }
                Collections.sort(sibling_list);
                for (String sibling : sibling_list) {
                    str.append(sibling);
                }

                String hash_code = String.valueOf(Math.abs(str.toString().hashCode()));
                String hub_name = "wan_" + hash_code;
                if (hash_code.length() > 8) {
                    hub_name = "wan_" + hash_code.substring(0, 8);
                }

                String[] parrent = node_iface.split(";");

                Map<String, String> node_iface_lan = new HashMap();
                Map<String, String> node_iface_wan = new HashMap();
                for (String[] children : children_list) {
                    String iface = children[2].toLowerCase();
                    boolean found2 = false;
                    for (String shablon : endpoint_iface_shablon) {
                        if (iface.matches(shablon.toLowerCase())) {
                            found2 = true;
                            break;
                        }
                    }
                    if (found2) {
                        node_iface_wan.put(children[0], children[0]);
                    } else {
                        node_iface_lan.put(children[0], children[0]);
                    }

                }
                if (!node_iface_lan.isEmpty()) {
                    // add hub
//                    Map<String, String> general1 = new HashMap();
//                    general1.put("sysname", "");
//                    general1.put("model", "wan_hub");
                    Map<String, String> image = new HashMap();
                    image.put("image", Neb.wan_image);
                    nodes_information.put(hub_name, image);
                    logger.Println("Add wan_hub: " + hub_name, logger.DEBUG);

                    // remove links
                    for (String[] children : children_list) {
                        if (node_iface_wan.get(children[0]) != null) {
                            ArrayList<String> link = new ArrayList();
                            link.add(parrent[0]);
                            link.add("");
                            link.add(parrent[2]);
                            link.add(children[0]);
                            link.add("");
                            link.add(children[2]);
                            if (delLink(link, links)) {
                                logger.Println("Delete link wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                            } else {
                                logger.Println("Error delete link wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                            }

                        }
                    }
                    // add links lan to wan hub
                    for (String[] children : children_list) {
                        if (node_iface_lan.get(children[0]) != null) {
                            ArrayList<String> link = new ArrayList();
                            link.add(children[0]);
                            link.add("unknown");
                            link.add("unknown");
                            link.add(hub_name);
                            link.add("unknown");
                            link.add("unknown");
                            link.add("wan_link");
                            addLink(link, links);
                            logger.Println("Add link find wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                    }

                    // add links wlan to wan hub
                    for (String[] children : children_list) {
                        if (node_iface_wan.get(children[0]) != null) {
                            ArrayList<String> link = new ArrayList();
                            link.add(hub_name);
                            link.add("unknown");
                            link.add("unknown");
                            link.add(children[0]);
                            link.add(children[1]);
                            link.add(children[2]);
                            link.add("wan_link");
                            addLink(link, links);
                            logger.Println("Add link find wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                    }
                    result = true;
                } else {
                    // add hub
                    Map<String, String> general1 = new HashMap();
                    general1.put("sysname", "");
                    general1.put("model", "wan_hub");
                    Map<String, Map> general = new HashMap();
                    general.put("general", general1);
                    nodes_information.put(hub_name, general);
                    logger.Println("Add wan_hub: " + hub_name, logger.DEBUG);

                    // remove links
                    for (String[] children : children_list) {
                        if (node_iface_wan.get(children[0]) != null) {
                            ArrayList<String> link = new ArrayList();
                            link.add(parrent[0]);
                            link.add("");
                            link.add(parrent[2]);
                            link.add(children[0]);
                            link.add("");
                            link.add(children[2]);
                            if (delLink(link, links)) {
                                logger.Println("Delete link wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                            } else {
                                logger.Println("Error delete link wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                            }

                        }
                    }

                    // add link parrent to wan hub                               
                    ArrayList<String> link = new ArrayList();
                    link.add(parrent[0]);
                    link.add(parrent[1]);
                    link.add(parrent[2]);
                    link.add(hub_name);
                    link.add("unknown");
                    link.add("unknown");
                    link.add("wan_link");
                    addLink(link, links);
                    logger.Println("Add link find wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);

                    // add links wlan to wan hub
                    for (String[] children : children_list) {
                        if (node_iface_wan.get(children[0]) != null) {
                            link = new ArrayList();
                            link.add(hub_name);
                            link.add("unknown");
                            link.add("unknown");
                            link.add(children[0]);
                            link.add(children[1]);
                            link.add(children[2]);
                            link.add("wan_link");
                            addLink(link, links);
                            logger.Println("Add link find wan_hub: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
                        }
                    }
                    result = true;
                }
            }
        }
        return result;
    }

    private boolean equalsNode(String node1, String node2) {
        if (node1.equals(node2)) {
            return true;
        } else {
            String nd1;
            String nd2;
            if (node1.length() < node2.length()) {
                nd1 = node1;
                nd2 = node2;
            } else {
                nd1 = node2;
                nd2 = node1;
            }
            return nd2.matches("^" + nd1 + "\\(.+\\)");
        }
    }

    public boolean delLink(ArrayList<String> link, ArrayList<ArrayList<String>> links) {
        boolean res = false;
        ArrayList<Integer> del_list = new ArrayList();
        int i = 0;
        for (ArrayList<String> item : links) {
            if (item.get(0).equals(link.get(0)) && item.get(2).equals(link.get(2))
                    && item.get(3).equals(link.get(3)) && item.get(5).equals(link.get(5))) {
                del_list.add(i);
            } else if (item.get(0).equals(link.get(3)) && item.get(2).equals(link.get(5))
                    && item.get(3).equals(link.get(0)) && item.get(5).equals(link.get(2))) {
                del_list.add(i);
            }
            i = i + 1;
        }
        if (!del_list.isEmpty()) {
            //reverse list
            ArrayList<Integer> del_list_new = new ArrayList();
            for (int ii = del_list.size() - 1; ii >= 0; ii--) {
                del_list_new.add(del_list.get(ii));
            }
            del_list = del_list_new;

            for (Integer item : del_list) {
                links.remove((int) item);
                res = true;
            }
        }
        return res;
    }

    public boolean addLink(ArrayList<String> link, ArrayList<ArrayList<String>> links) {
        boolean found = false;
        for (ArrayList<String> item : links) {
            if (item.get(0).equals(link.get(0)) && item.get(2).equals(link.get(2))
                    && item.get(3).equals(link.get(3)) && item.get(5).equals(link.get(5))) {
                found = true;
                break;
            } else if (item.get(0).equals(link.get(3)) && item.get(2).equals(link.get(5))
                    && item.get(3).equals(link.get(0)) && item.get(5).equals(link.get(2))) {
                found = true;
                break;
            }
        }
        if (!found) {
            links.add(link);
            return true;
        } else {
            return false;
        }

    }

    private Map<String, ArrayList<String[]>> getHubStruct(ArrayList<ArrayList<String>> links) {
        Map hubs = new HashMap();

        JSONParser parser = new JSONParser();
        Map<Integer, Integer> used_links = new HashMap();
        for (int i = 0; i < links.size(); i++) {
            if (used_links.get(i) == null) {
                ArrayList<String> link1 = links.get(i);
                if (link1.size() == 7 && !link1.get(0).matches("^hub_.+") && !link1.get(0).matches("^wan_.+")
                        && !link1.get(3).matches("^hub_.+") && !link1.get(3).matches("^wan_.+")
                        && !link1.get(2).equals("unknown") && !link1.get(2).equals("Port")
                        && !link1.get(2).toLowerCase().matches(".*radio.*")
                        && !link1.get(2).toLowerCase().matches(".*wan.*")
                        && !link1.get(2).toLowerCase().matches(".*wlan.*")) {
                    ArrayList<String[]> children_list = new ArrayList();
                    for (int j = i + 1; j < links.size(); j++) {
                        if (used_links.get(j) == null) {
                            ArrayList<String> link2 = links.get(j);
                            if (link2.size() == 7 && !link2.get(0).matches("^hub_.+") && !link2.get(0).matches("^wan_.+")
                                    && !link2.get(3).matches("^hub_.+") && !link2.get(3).matches("^wan_.+")) {
                                if (link1.get(0).equals(link2.get(0)) && link1.get(2).equals(link2.get(2))) {
                                    String type = "";
                                    String remote_version = "";
                                    try {
                                        JSONObject jsonObject = (JSONObject) parser.parse(link2.get(6));
                                        Map json = toMap(jsonObject);
                                        if (json.get("type") != null) {
                                            type = (String) json.get("type");
                                        }
                                        if (json.get("remote_version") != null) {
                                            remote_version = (String) json.get("remote_version");
                                        }
                                    } catch (ParseException ex) {
                                    }
                                    children_list = add_list(link2.get(3), link2.get(4), link2.get(5), type, remote_version, link2.get(6), children_list);
                                    used_links.put(j, j);
                                }
                                if (link1.get(0).equals(link2.get(3)) && link1.get(2).equals(link2.get(5))) {
                                    String type = "";
                                    String remote_version = "";
                                    try {
                                        JSONObject jsonObject = (JSONObject) parser.parse(link2.get(6));
                                        Map json = toMap(jsonObject);
                                        if (json.get("type") != null) {
                                            type = (String) json.get("type");
                                        }
                                        if (json.get("remote_version") != null) {
                                            remote_version = (String) json.get("remote_version");
                                        }
                                    } catch (ParseException ex) {
                                    }
                                    children_list = add_list(link2.get(0), link2.get(1), link2.get(2), type, remote_version, link2.get(6), children_list);
                                    used_links.put(j, j);
                                }

                            }
                        }
                    }
                    if (!children_list.isEmpty()) {
                        String type = "";
                        String remote_version = "";
                        try {
                            JSONObject jsonObject = (JSONObject) parser.parse(link1.get(6));
                            Map json = toMap(jsonObject);
                            if (json.get("type") != null) {
                                type = (String) json.get("type");
                            }
                            if (json.get("remote_version") != null) {
                                remote_version = (String) json.get("remote_version");
                            }
                        } catch (ParseException ex) {
                        }
                        children_list = add_list(link1.get(3), link1.get(4), link1.get(5), type, remote_version, link1.get(6), children_list);
                        used_links.put(i, i);
                        hubs.put(link1.get(0) + ";" + link1.get(1) + ";" + link1.get(2), children_list);
                    }

                }
            }
        }

        return hubs;
    }

    private ArrayList<String[]> add_list(String node, String id, String port, String type, String remote_version, String info, ArrayList<String[]> node_port_list) {
        boolean found = false;
        for (String[] item : node_port_list) {
            if (item[0].equals(node) && item[2].equals(port)) {
                found = true;
                break;
            }
        }
        if (!found) {
            String[] mas = new String[6];
            mas[0] = node;
            mas[1] = id;
            mas[2] = port;
            mas[3] = type;
            mas[4] = remote_version;
            mas[5] = info;
            node_port_list.add(mas);
        }
        return node_port_list;
    }

    public Map getNodeInfoBrief(Map info_map) {
        Map area_node_links = new HashMap();

        for (Map.Entry<String, Map> area : ((Map<String, Map>) info_map).entrySet()) {
            String area_name = area.getKey();
            Map area_info = area.getValue();
            Map<String, Map> nodes_information = (Map<String, Map>) area_info.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList) area_info.get("links");
            for (Map.Entry<String, Map> item : nodes_information.entrySet()) {
                String node = item.getKey();
                Map val = item.getValue();
                Map<String, String> ip_map = getIpNode(val);
                ArrayList<String> ip_list = new ArrayList(ip_map.values());
                ArrayList<String> mac_list = getMACFromNode(val);
                ArrayList<String> mac_list_normalize = new ArrayList();
                for (String item1 : mac_list) {
                    mac_list_normalize.add(item1.toLowerCase().replace(":", "").replace("-", "").replace(".", ""));
                }
                String mac_base = null;
                String sysname = null;
//                ArrayList<String> mac_list = new ArrayList();
                if (val.get("general") != null) {
                    mac_base = (String) ((Map) val.get("general")).get("base_address");
                    if (mac_base != null) {
                        mac_base = mac_base.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
                        mac_list_normalize.add(mac_base.toLowerCase().replace(":", "").replace("-", "").replace(".", ""));
                    }
                    sysname = (String) ((Map) val.get("general")).get("sysname");
                }

                ArrayList<Pattern> p_list = new ArrayList();
                Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[ .:-]){5}([0-9A-Fa-f]{2}))");
                Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[ .:-]){2}([0-9A-Fa-f]{4}))");
                Pattern p3 = Pattern.compile("([0-9A-Fa-f]{6}[ .:-][0-9A-Fa-f]{6})");
                Pattern p4 = Pattern.compile("\\(([0-9A-Fa-f]{12})\\)");
                p_list.add(p1);
                p_list.add(p2);
                p_list.add(p3);
                p_list.add(p4);
                if (sysname != null) {
                    for (Pattern p : p_list) {
                        Matcher m = p.matcher(sysname);
                        if (m.find()) {
                            String mac = m.group(1);
                            mac = mac.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
                            if (!mac_list_normalize.contains(mac)) {
                                mac_list_normalize.add(mac);
                            }
                            break;
                        }
                    }
                }
                for (Pattern p : p_list) {
                    Matcher m = p.matcher(node);
                    if (m.find()) {
                        String mac = m.group(1);
                        mac = mac.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
                        if (!mac_list_normalize.contains(mac)) {
                            mac_list_normalize.add(mac);
                        }
                        break;
                    }
                }

                Pattern p = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                if (sysname != null) {
                    Matcher m = p.matcher(sysname);
                    if (m.find()) {
                        String ip = m.group(1);
                        if (!ip_list.contains(ip)) {
                            ip_list.add(ip);
                        }
                    }
                }
                Matcher m = p.matcher(node);
                if (m.find()) {
                    String ip = m.group(1);
                    if (!ip_list.contains(ip)) {
                        ip_list.add(ip);
                    }
                }

//                if (val.get("interfaces") != null) {
//                    Map<String, Map> interfaces = (Map) val.get("interfaces");
//                    for (Map.Entry<String, Map> item1 : interfaces.entrySet()) {
//                        Map<String, String> val1 = item1.getValue();
//                        if (val1.get("mac") != null && !val1.get("mac").isEmpty()) {
//                            String mac = val1.get("mac");
//                            mac = mac.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
//                            if (mac.matches("[0-9a-f]{12}") && !mac.equals("000000000000") && !mac.matches("000000[0-9a-f]{6}")) {
//                                mac_list.add(mac);
//                            }
//                        }
//                    }
//                }
                ArrayList<String[]> node_links = new ArrayList();
                if (links != null) {
                    for (ArrayList<String> item1 : links) {
                        String node1 = null;
                        String iface1 = null;
                        String node2 = null;
                        String iface2 = null;
                        if (item1.size() == 5) {
                            node1 = item1.get(0);
                            iface1 = item1.get(1);
                            node2 = item1.get(2);
                            iface2 = item1.get(3);
                        } else {
                            node1 = item1.get(0);
                            iface1 = item1.get(2);
                            node2 = item1.get(3);
                            iface2 = item1.get(5);
                        }
                        if (node1 != null && iface1 != null && node2 != null && iface2 != null) {
                            if (node1.equals(node)) {
                                String[] mas = new String[2];
                                mas[0] = node2;
                                mas[1] = iface2;
                                node_links.add(mas);
                            }
                            if (node2.equals(node)) {
                                String[] mas = new String[2];
                                mas[0] = node1;
                                mas[1] = iface1;
                                node_links.add(mas);
                            }
                        }
                    }
                }

                Map info = new HashMap();
                info.put("node", node);
                if (mac_base != null) {
                    info.put("mac_base", mac_base);
                }
                if (sysname != null) {
                    info.put("sysname", sysname);
                }
                if (!mac_list_normalize.isEmpty()) {
                    info.put("mac_list", mac_list_normalize);
                }
                if (!ip_list.isEmpty()) {
                    info.put("ip_list", ip_list);
                }
                if (!node_links.isEmpty()) {
                    info.put("links", node_links);
                }
                Map node_info = new HashMap();
                node_info.put(node, info);

                if (area_node_links.get(area_name) == null) {
                    area_node_links.put(area_name, node_info);
                } else {
                    ((Map) area_node_links.get(area_name)).putAll(node_info);
                }
            }
        }
        return area_node_links;
    }

    public boolean isEqualsNodes(Map info1, Map info2) {
        String node1 = (String) info1.get("node");
        String node2 = (String) info2.get("node");
        String mac_base1 = (String) info1.get("mac_base");
        String mac_base2 = (String) info2.get("mac_base");
        String sysname1 = (String) info1.get("sysname");
        String sysname2 = (String) info2.get("sysname");
        ArrayList<String> mac_list1 = (ArrayList) info1.get("mac_list");
        ArrayList<String> mac_list2 = (ArrayList) info2.get("mac_list");
        ArrayList<String> ip_list1 = (ArrayList) info1.get("ip_list");
        ArrayList<String> ip_list2 = (ArrayList) info2.get("ip_list");
        ArrayList<String[]> links1 = (ArrayList) info1.get("links");
        ArrayList<String[]> links2 = (ArrayList) info2.get("links");

        if (sysname1 != null && sysname2 != null && !sysname1.equals(sysname2)) {
//            System.out.println(" --- "+node1+"/"+sysname1+" - "+node2+"/"+sysname2+" non equals.");
            return false;
        }

        if (mac_list1 != null && mac_list2 != null) {
            for (String mac1 : mac_list1) {
                boolean find = false;
                for (String mac2 : mac_list2) {
                    if (mac1.equals(mac2)) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    return false;
                }
            }
            return true;
        }

        if (ip_list1 != null && ip_list2 != null && ip_list1.size() > 1 && ip_list2.size() > 1) {
            for (String ip1 : ip_list1) {
                boolean find = false;
                for (String ip2 : ip_list2) {
                    if (ip1.equals(ip2)) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    return false;
                }
            }
            return true;
        }

        if (mac_base1 != null && mac_base2 != null &&
                !mac_base1.startsWith("00000000") && !mac_base1.startsWith("01000000") &&
                !mac_base2.startsWith("00000000") && !mac_base2.startsWith("01000000")
        ) {
            if (mac_base1.equals(mac_base2)) {
                return true;
            }
        }
        boolean equal = false;
        if (sysname1 != null && !sysname1.equals("(none)") && sysname2 != null && !sysname2.equals("(none)")) {
            if (sysname1.equals(sysname2)) {
                equal = true;
            }
        }
        if (equal) {
            if (links1 != null && links2 != null) {
                for (String[] item1 : links1) {
                    for (String[] item2 : links2) {
                        if (item1[0].equals(item2[0]) && equalsIfaceName(item1[1], item2[1])) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public String nameToIp(String name, Map<String, String> mac_ip) {
        if (name.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return name;
        }

//        Pattern p0 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
//        Matcher m0 = p0.matcher(name);
//        if (m0.find()) {
//            String ip = m0.group(1);
//            if(ip != null)
//                return ip;
//        }        

        String name_new = name.replaceAll("[:.-]", "").toLowerCase();
        Pattern p = Pattern.compile("^.*([0-9a-f]{12}).*$");
        Matcher m = p.matcher(name_new);
        if (m.find()) {
            String mac = m.group(1);
            if (mac_ip.get(mac) != null)
                return mac_ip.get(mac);
            else
                return name;
        }
        return name;
    }

    public Map normalizeLinks(Map<String, Map> info_map) {
        Map<String, String> mac_ip = get_Mac_Ip(info_map);

        for (Map.Entry<String, Map> entry : info_map.entrySet()) {
//            String area = entry.getKey();
            Map val = entry.getValue();
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
            if (links != null) {
                ArrayList<ArrayList<String>> links_new = new ArrayList();
                for (ArrayList<String> link : links) {
                    String node = link.get(3);
                    if (!node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        String res = utils.nameToIp(node, mac_ip);
                        if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            link.set(3, res);
                            links_new.add(link);
                            logger.Println("normalizeLinks add link: " + link.get(0) + ", " + link.get(1) + " <---> " + link.get(2) + ", " + link.get(3) + " - " + link.get(4), logger.DEBUG);
                        } else {
                            logger.Println("normalizeLinks remove link: " + link.get(0) + ", " + link.get(1) + " <---> " + link.get(2) + ", " + link.get(3) + " - " + link.get(4), logger.DEBUG);
                        }
                    } else {
                        links_new.add(link);
                        logger.Println("normalizeLinks add link: " + link.get(0) + ", " + link.get(1) + " <---> " + link.get(2) + ", " + link.get(3) + " - " + link.get(4), logger.DEBUG);
                    }
                }
                val.put("links", links_new);
            }
        }
        return info_map;
    }

    public Map normalizeMap(Map<String, Map> info_map, Map<String, ArrayList<String[]>> area_arp_mac_table) {
        try {
            Map<String, String> mac_ip_from_nodes = get_Mac_Ip(info_map);
            Map<String, Map<String, String>> area_ip_mac_from_nodes = get_Area_Ip_Mac(info_map);
            Map<String, Map<String, Map<String, String>>> area_node_ip_mac = get_Area_Node_Ip_Mac(info_map);
            Map<String, Map> area_Node_NodePriority = getArea_Node_NodePriority(info_map, Neb.area_networks);

            // area_arp_mac_table to area_mac_ip and area_ip_mac
            Map<String, Map<String, String>> area_mac_ip_from_arp_mac_table = new HashMap();
            Map<String, Map<String, String>> area_ip_mac_from_arp_mac_table = new HashMap();
            for (Map.Entry<String, ArrayList<String[]>> area : area_arp_mac_table.entrySet()) {
                String area_name = area.getKey();
                ArrayList<String[]> area_info = area.getValue();
                Map<String, String> mac_map = new HashMap();
                Map<String, String> ip_map = new HashMap();
                for (String[] item : area_info) {
                    mac_map.put(item[4].replaceAll("[:.-]", "").toLowerCase(), item[3]);
                    ip_map.put(item[3], item[4]);
                }
                area_mac_ip_from_arp_mac_table.put(area_name, mac_map);
                area_ip_mac_from_arp_mac_table.put(area_name, ip_map);
            }

            for (Map.Entry<String, Map> area : info_map.entrySet()) {
                String area_name = area.getKey();
                Map area_info = area.getValue();

                Map<String, Map> nodes_info = (Map<String, Map>) area_info.get("nodes_information");
                if (nodes_info != null) {
                    ArrayList<String[]> replace_nodes = new ArrayList();

                    //remove node equiv "" and "unknown_ip"
                    Map<String, Map> nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if (!node.isEmpty() && !node.equals("unknown_ip")) {
                            nodes_info_new.put(node, node_info);
                        }
                    }
                    nodes_info = nodes_info_new;

                    //replace symbol "/" to "_" in node name
                    nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey().replace("/", "_");
                        Map<String, Map> node_info = node_it.getValue();
                        if (!node.isEmpty()) {
                            nodes_info_new.put(node, node_info);
                        }
                    }
                    nodes_info = nodes_info_new;

                    // correct node information
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        System.out.println("node1 - "+node);
                        Map<String, Map> node_info = node_it.getValue();
                        if (node_info.get("general") != null && node_info.get("general").get("base_address") != null) {
                            String base_address = (String) node_info.get("general").get("base_address");
//                            if(base_address == null || base_address.equals("0"))
//                                System.out.println("11111");
                            if (base_address == null || !(base_address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || base_address.matches("^([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4})$"))) {
                                if (node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                    if (area_ip_mac_from_arp_mac_table.get(area_name) != null && area_ip_mac_from_arp_mac_table.get(area_name).get(node) != null) {
                                        String base_address_old = base_address;
                                        base_address = area_ip_mac_from_arp_mac_table.get(area_name).get(node);
                                        node_info.get("general").put("base_address", base_address);
                                        logger.Println("Replace base_address for node: " + node + " - " + base_address_old + "/" + base_address, logger.DEBUG);
                                    } else {
                                        if (area_ip_mac_from_nodes.get(area_name) != null && area_ip_mac_from_nodes.get(area_name).get(node) != null) {
                                            String base_address_old = base_address;
                                            base_address = area_ip_mac_from_nodes.get(area_name).get(node);
                                            node_info.get("general").put("base_address", base_address);
                                            logger.Println("Replace base_address for node: " + node + " - " + base_address_old + "/" + base_address, logger.DEBUG);
                                        }
                                    }

                                }
                            }
                        }
                    }
                    //////////////////////////////////////////////////////
                    //remove node if node equals sysname other node
                    Map<String, ArrayList<String>> sysname_node = new HashMap();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        System.out.println("node2 - "+node);
                        Map<String, Map> node_info = node_it.getValue();
                        if (node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
                            String sysname = (String) (node_info.get("general").get("sysname"));
                            Pattern p = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
                            Matcher m = p.matcher(sysname);
                            if (!m.find()) {
                                sysname = sysname.split("\\.")[0];
                            }
                            if (sysname_node.get(sysname) == null) {
                                ArrayList<String> tmp_list = new ArrayList();
                                tmp_list.add(node);
                                sysname_node.put(sysname, tmp_list);
                            } else {
                                sysname_node.get(sysname).add(node);
                            }
                        }
                    }

//                    ArrayList<String[]> nodes_to_nodes_for_mac_ip_port = new ArrayList();
                    for (Map.Entry<String, ArrayList<String>> it : sysname_node.entrySet()) {
                        ArrayList<String> val = it.getValue();
                        if (val.size() == 2) {
                            if (val.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !val.get(1).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String[] tmp = new String[2];
                                tmp[0] = val.get(1);
                                tmp[1] = val.get(0);
                                replace_nodes.add(tmp);
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = val.get(1);
//                                tmp1[1] = val.get(0);
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
                            } else if (!val.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && val.get(1).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String[] tmp = new String[2];
                                tmp[0] = val.get(0);
                                tmp[1] = val.get(1);
                                replace_nodes.add(tmp);
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = val.get(0);
//                                tmp1[1] = val.get(1);
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
                            }
//                            else if (!val.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !val.get(1).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                                String[] tmp = new String[2];
//                                tmp[0] = val.get(0);
//                                replace_nodes.add(tmp);
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = val.get(0);
//                                tmp1[1] = val.get(1);
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
//                            }
                        }
                    }

                    ////////////////////////////////////
                    applyInfo(area_info, replace_nodes);

                    nodes_info = (Map<String, Map>) area_info.get("nodes_information");
                    replace_nodes.clear();
//                    nodes_to_nodes_for_mac_ip_port.clear();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        System.out.println("node3 - "+node);
                        // resolve nodes format CS2960...(12:34:56:78:0a:bc) and remove duplicate nodes
                        if (!node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String ip = null;
                            String res = nameToIp(node, mac_ip_from_nodes);
                            if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                ip = res;
                            } else {
                                if (area_mac_ip_from_arp_mac_table.get(area_name) != null) {
                                    res = nameToIp(node, area_mac_ip_from_arp_mac_table.get(area_name));
                                    if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                        ip = res;
                                    }
                                }
                            }
//                            if (ip == null) {
//                                InetAddress address;
//                                try {
//                                    node = node.split("\\.")[0];
//                                    if(!node.isEmpty()) {
//                                        address = InetAddress.getByName(node);
//                                        ip = address.getHostAddress();
//                                    }
//                                } catch (UnknownHostException ex) {
//                                }
//                            }                            
//                            if (ip == null) {
//                                Pattern p = get("^(.*)\\((.*)\\)$");
//                                Matcher m = p.matcher(node);
//                                if (m.find()) {
//                                    String sysname = m.group(1);
//                                    InetAddress address;
//                                    try {
//                                        sysname = sysname.split("\\.")[0];
//                                        if(!sysname.isEmpty()) {
//                                            address = InetAddress.getByName(sysname);
//                                            ip = address.getHostAddress();
//                                        }
//                                    } catch (UnknownHostException ex) {
//                                    }
//                                }
//                            }
                            if (ip != null) {
                                String[] tmp = new String[2];
                                tmp[0] = node;
                                tmp[1] = ip;
                                replace_nodes.add(tmp);
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = node;
//                                tmp1[1] = ip;
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
                            }
                        }

                    }
                    applyInfo(area_info, replace_nodes);

                    nodes_info = (Map<String, Map>) area_info.get("nodes_information");
                    replace_nodes.clear();
//                    nodes_to_nodes_for_mac_ip_port.clear();
                    // find in node and sysname name mac address.
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        System.out.println("node4 - "+node);
                        Map<String, Map> node_info = node_it.getValue();

                        if (!node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String res = nameToIp(node, mac_ip_from_nodes);
                            if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String[] tmp = new String[2];
                                tmp[0] = node;
                                tmp[1] = res;
                                replace_nodes.add(tmp);
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = node;
//                                tmp1[1] = res;
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
                            } else {
                                if (node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
                                    String sysname = (String) (node_info.get("general").get("sysname"));
                                    res = nameToIp(sysname, mac_ip_from_nodes);
                                    if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                        String[] tmp = new String[2];
                                        tmp[0] = node;
                                        tmp[1] = res;
                                        replace_nodes.add(tmp);
//                                        String[] tmp1 = new String[2];
//                                        tmp1[0] = node;
//                                        tmp1[1] = res;
//                                        nodes_to_nodes_for_mac_ip_port.add(tmp1);
                                    }
                                }
                            }
                        }
                    }
                    applyInfo(area_info, replace_nodes);

                    // remove duplicate nodes if node or sysname equivalent am splitter
                    Map<String, ArrayList<String>> key_node = new HashMap();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        System.out.println("node5 - "+node);
                        Map<String, Map> node_info = node_it.getValue();

                        Pattern p = Pattern.compile("(.*)\\((([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))\\)");
                        Matcher m = p.matcher(node);
                        String[] mas = new String[2];
                        if (m.find()) {
                            if (!(m.group(1).equals("none") || m.group(1).equals("(none)") ||
                                    m.group(1).equals("null") ||
                                    m.group(1).equals("not advertised")
                            ) &&
                                    !(m.group(2).startsWith("00:00:00:00") ||
                                            m.group(2).startsWith("01:00:00:00")
                                    )
                            ) {
                                mas[0] = m.group(1);
                                mas[1] = m.group(2);
                            }
                        }

                        for (String item : mas) {
                            if (item != null && !item.isEmpty()) {
//                                System.out.println("\t"+item+" - "+node);
                                if (key_node.get(item) == null) {
                                    ArrayList tmp = new ArrayList();
                                    tmp.add(node);
                                    key_node.put(item, tmp);
                                } else {
                                    if (!key_node.get(item).contains(node)) {
                                        key_node.get(item).add(node);
                                    }
                                }
                            }

                        }
                    }

                    Map<String, ArrayList<String>> key_node_new = new HashMap();
                    for (Map.Entry<String, ArrayList<String>> entry : key_node.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<String> nodes_list = entry.getValue();
                        if (nodes_list.size() > 1) {
                            key_node_new.put(key, nodes_list);
                        }
                    }
                    key_node = key_node_new;

                    ArrayList<String[]> nodes_duble_list = new ArrayList();
//                    Map<String, String> nodes_exclude = new HashMap();
                    for (Map.Entry<String, ArrayList<String>> entry : key_node.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<String> nodes_list = entry.getValue();
//                        String listString = nodes_list.stream().map(Object::toString).collect(Collectors.joining(", "));
//                        System.out.println(key+" - "+listString);
                        if (nodes_list.size() > 1) {
                            int node_ip = 0;
                            for (String it : nodes_list) {
                                if (it.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
                                    node_ip++;
                            }
                            if (node_ip <= 1) {
//                                System.out.print(key+": ");
//                                for(String it : nodes_list)
//                                    System.out.print(it+", ");
//                                System.out.println();

                                if (key.length() > 4 && !key.contains(" ")) {
                                    String node_priority = nodes_list.get(0);
                                    if (Neb.area_networks.get(area_name) != null && !Neb.area_networks.get(area_name).isEmpty()) {
                                        node_priority = getPriorityNode(nodes_list, nodes_info, area_Node_NodePriority.get(area_name));
                                        nodes_list.remove(node_priority);
                                    }
                                    ArrayList<String[]> tmp_list = new ArrayList();
                                    for (String node : nodes_list) {
                                        String[] tmp_mas = new String[2];
                                        tmp_mas[0] = node;
                                        tmp_mas[1] = node_priority;
                                        tmp_list.add(tmp_mas);
                                    }
                                    nodes_duble_list.addAll(tmp_list);
//                                    /////////////
//                                    for(String node : nodes_list) {
//                                        System.out.println(" --- Double - "+node);
//                                    }   
//                                    /////////////////                                    
                                } else {
                                    Map<String, ArrayList<String>> mac_node = new HashMap();
                                    for (String node : nodes_list) {
                                        String mac = extractMACFromName1(node);
                                        if (!mac.isEmpty()) {
                                            if (mac_node.get(mac) == null) {
                                                ArrayList<String> tmp_list = new ArrayList();
                                                tmp_list.add(node);
                                                mac_node.put(mac, tmp_list);
                                            } else {
                                                mac_node.get(mac).add(node);
                                            }
                                        }

                                    }
                                    for (Map.Entry<String, ArrayList<String>> entry1 : mac_node.entrySet()) {
                                        ArrayList<String> val_list = entry1.getValue();
                                        String node_priority = nodes_list.get(0);
                                        if (Neb.area_networks.get(area_name) != null && !Neb.area_networks.get(area_name).isEmpty()) {
                                            node_priority = getPriorityNode(val_list, nodes_info, area_Node_NodePriority.get(area_name));
                                            val_list.remove(node_priority);
                                        }
                                        ArrayList<String[]> tmp_list = new ArrayList();
                                        for (String node : val_list) {
                                            String[] tmp_mas = new String[2];
                                            tmp_mas[0] = node;
                                            tmp_mas[1] = node_priority;
                                            tmp_list.add(tmp_mas);
                                        }
                                        nodes_duble_list.addAll(tmp_list);
//                                        /////////////
//                                        for(String node : val_list) {
//                                            System.out.println(" --- Double - "+node);
//                                        }   
//                                        /////////////////
                                    }
                                }
                            }
                        }
                    }

                    // remove duplicate nodes if node equivalent ip address other node
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
//                        if(node.equals("10.96.115.254") || node.equals("10.97.7.254")) {
//                            System.out.println("node - "+node);
//                        }
//                        System.out.println("node6 - "+node);
                        if (area_node_ip_mac.get(area_name) != null) {
                            for (Map.Entry<String, Map<String, String>> entry1 : area_node_ip_mac.get(area_name).entrySet()) {
                                String node1 = entry1.getKey();
                                Map<String, String> val = entry1.getValue();
                                if (val.get(node) != null && !node.equals(node1)) {
                                    String[] tmp_mas = new String[2];
                                    tmp_mas[0] = node;
                                    tmp_mas[1] = node1;
                                    String node_priority = node;
                                    if (Neb.area_networks.get(area_name) != null && !Neb.area_networks.get(area_name).isEmpty()) {
                                        ArrayList<String> tmp_list = new ArrayList();
                                        tmp_list.add(node);
                                        tmp_list.add(node1);
                                        node_priority = getPriorityNode(tmp_list, nodes_info, area_Node_NodePriority.get(area_name));
                                    }
                                    if (node_priority.equals(node1)) {
                                        nodes_duble_list.add(tmp_mas);
                                    }
//                                    System.out.println("Node "+node+" is duplicate with "+node1);
                                }

                            }
                        }
                    }

                    if (!nodes_duble_list.isEmpty()) {
//                        applyInfo_RemoveDubleNodes(area_info, nodes_duble_list);
                        applyInfo(area_info, nodes_duble_list);
                    }
                }

            }

            // check nodes without links and adding link from mac_ip_port
            for (Map.Entry<String, Map> area : info_map.entrySet()) {
                Map area_info = area.getValue();
                Map<String, Map> nodes_info = (Map<String, Map>) area_info.get("nodes_information");
                ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
                ArrayList<ArrayList<String>> links_info = (ArrayList<ArrayList<String>>) area_info.get("links");
                if (nodes_info != null && links_info != null) {
                    //                Map<String, Map> nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        boolean found = false;
                        for (ArrayList<String> link : links_info) {
                            String node1 = link.get(0);
                            String node2 = link.get(3);
                            if (node.equals(node1) || node.equals(node2)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            logger.Println("Node " + node + " is not link.", logger.DEBUG);
                            //                        boolean found_ip = false;
                            if (mac_ip_port != null) {
                                for (String[] mac_ip_node_id_iface : mac_ip_port) {
                                    if (node.equals(mac_ip_node_id_iface[1])) {
                                        ArrayList<String> link_tmp = new ArrayList();
                                        link_tmp.add(node);
                                        link_tmp.add("");
                                        link_tmp.add("unknown");
                                        link_tmp.add(mac_ip_node_id_iface[2]);
                                        link_tmp.add(mac_ip_node_id_iface[3]);
                                        link_tmp.add(mac_ip_node_id_iface[4]);
                                        link_tmp.add("mac_ip_port");
                                        links_info.add(link_tmp);
                                        logger.Println("Adding new link from mac_ip_port: " + link_tmp.get(0) + ", " + link_tmp.get(1) + ", " + link_tmp.get(2) + " <---> " + link_tmp.get(3) + ", " + link_tmp.get(4) + ", " + link_tmp.get(5) + " --- " + link_tmp.get(6), logger.DEBUG);
                                        if (node_info != null && node_info.get("general") != null) {
                                            String base_address = (String) node_info.get("general").get("base_address");
                                            if ((mac_ip_node_id_iface[0].matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || mac_ip_node_id_iface[0].matches("^([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4})$"))
                                                    && (base_address == null || !(base_address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || base_address.matches("^([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4})$")))) {
                                                node_info.get("general").put("base_address", mac_ip_node_id_iface[0]);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Map> area : info_map.entrySet()) {
                //            System.out.println(area_name);
                Map area_info = area.getValue();
                Map<String, Map> nodes_info = (Map<String, Map>) area_info.get("nodes_information");

                // remove clients from mac_ip_port if ip contains ip address node
                ArrayList<String> ip_list_nodes = new ArrayList();
                ArrayList<String> mac_list_nodes = new ArrayList();
                if (nodes_info != null) {
                    for (Map.Entry<String, Map> node_it : nodes_info.entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if (node_info != null) {
                            ip_list_nodes.add(node);
                            ip_list_nodes.addAll(getIpListFromNode(node_info));
                            mac_list_nodes.addAll(getMACFromNode(node_info));
                        }
                    }
                }

                ArrayList<String[]> mac_ip_port_new = new ArrayList();
                Map<String, Map> nodes_information = (Map<String, Map>) area_info.get("nodes_information");
                ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
                ArrayList<ArrayList<String>> links_info = (ArrayList<ArrayList<String>>) area_info.get("links");
                // remove clients if mac is node mac
                if (mac_ip_port != null) {
                    for (String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        if (!mac_list_nodes.contains(mac) && !ip_list_nodes.contains(ip)) {
                            mac_ip_port_new.add(item);
                        } else {
                            logger.Println("Remove client is mac node - " + item[0] + ", " + item[1] + ", " + item[2] + ", " + item[3] + ", " + item[4], logger.DEBUG);
                        }
                    }
                    mac_ip_port = mac_ip_port_new;

                    // remove clients am links
                    Map<String, String> mac_ip_node_iface_delete = new HashMap();
                    for (String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        String node = item[2];
                        String iface = item[4];
                        for (ArrayList<String> link : links_info) {
                            boolean found1 = node.equals(link.get(0)) &&
                                    compareIfacename(iface, link.get(2)) &&
                                    nodes_information.get(link.get(0)) != null;
                            boolean found2 = node.equals(link.get(3)) &&
                                    compareIfacename(iface, link.get(5)) &&
                                    nodes_information.get(link.get(3)) != null;
                            if (found1 || found2) {
                                String str = mac + ":" + ip + ":" + node + ":" + iface;
                                mac_ip_node_iface_delete.put(str, str);
                            }
                        }
                    }

                    mac_ip_port_new = new ArrayList();
                    for (String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        String node = item[2];
                        String iface = item[4];
                        String str = mac + ":" + ip + ":" + node + ":" + iface;
                        if (mac_ip_node_iface_delete.get(str) == null) {
                            mac_ip_port_new.add(item);
                        } else {
                            logger.Println("Remove client am link: " + mac + ", " + ip + " - " + node + ", " + iface, logger.DEBUG);
                        }
                    }

                    mac_ip_port = mac_ip_port_new;
                    area_info.put("mac_ip_port", mac_ip_port);
                }
            }

            // checking and replace short interface to full name interface
            for (Map.Entry<String, Map> area : info_map.entrySet()) {
                String area_name = area.getKey();
                Map<String, Object> val = area.getValue();
                Map<String, Map<String, String>> node_ifacename_ifaceid = Neb.area_node_ifaceid_ifacename.get(area_name);
                if (node_ifacename_ifaceid != null) {
                    Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
                    ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                    for (ArrayList<String> mas : links) {
                        String node1 = mas.get(0);
                        String iface1 = mas.get(2);
                        if (nodes_information.get(node1) != null && nodes_information.get(node1).get("interfaces") != null) {
                            Map<String, Map> interfaces_map = (Map<String, Map>) nodes_information.get(node1).get("interfaces");
                            boolean find = false;
                            if (interfaces_map.get(iface1) == null) {
                                for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                                    String interfacename = entry.getKey();
                                    if (equalsIfaceName(iface1, interfacename)) {
                                        mas.set(2, interfacename);
                                        logger.Println("Replace: " + node1 + " " + iface1 + " to: " + node1 + " " + mas.get(2), logger.DEBUG);
                                        //                            System.out.println("Replace: "+node1+" "+iface1+" to: "+node1+" "+mas.get(1));
                                        find = true;
                                        break;
                                    }
                                }
                            } else {
                                find = true;
                            }
                            if (!find) {
                                logger.Println(node1 + " " + iface1 + " not finded interfece from node.", logger.DEBUG);
                                //                   System.out.println(node1+" "+mas.get(1)+" not finded interfece from node.");
                            }
                        }
                        String node2 = mas.get(3);
                        String iface2 = mas.get(5);
                        if (nodes_information.get(node2) != null && nodes_information.get(node2).get("interfaces") != null) {
                            Map<String, Map> interfaces_map = (Map<String, Map>) nodes_information.get(node2).get("interfaces");
                            boolean find = false;
                            if (interfaces_map.get(iface2) == null) {
                                for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                                    String interfacename = entry.getKey();
                                    if (equalsIfaceName(iface2, interfacename)) {
                                        mas.set(5, interfacename);
                                        logger.Println("Replace: " + node2 + " " + iface2 + " to: " + node2 + " " + mas.get(5), logger.DEBUG);
                                        //                            System.out.println("Replace: "+node1+" "+iface1+" to: "+node1+" "+mas.get(1));
                                        find = true;
                                        break;
                                    }
                                }
                            } else {
                                find = true;
                            }
                            if (!find) {
                                logger.Println(node2 + " " + iface2 + " not finded interfece from node.", logger.DEBUG);
                                //                   System.out.println(node1+" "+mas.get(1)+" not finded interfece from node.");
                            }
                        }
                    }
                }
            }

            // adding ifaceid to links
            for (Map.Entry<String, Map> area : info_map.entrySet()) {
                String area_name = area.getKey();
                Map<String, Object> val = area.getValue();
                Map<String, Map<String, String>> node_ifacename_ifaceid = Neb.area_node_ifaceid_ifacename.get(area_name);
                if (node_ifacename_ifaceid != null) {
                    ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                    ArrayList<ArrayList<String>> links_new = new ArrayList();
                    if (links != null) {
                        for (ArrayList<String> link : links) {
                            ArrayList<String> link_tmp = new ArrayList();
                            if (link.get(2).isEmpty()) {
                                Map<String, String> ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(0));
                                if (ifaceid_ifacename_list != null && !ifaceid_ifacename_list.isEmpty()) {
                                    String[] if_name_iface1 = findIfaceFromNode(link.get(2), ifaceid_ifacename_list);
                                    if (if_name_iface1[0] != null) {
                                        link_tmp.add(link.get(0));
                                        link_tmp.add(if_name_iface1[0]);
                                        link_tmp.add(if_name_iface1[1]);
                                    } else {
                                        link_tmp.add(link.get(0));
                                        link_tmp.add("");
                                        link_tmp.add(link.get(2));
                                        //                                System.out.println("ID iface not found: "+link.get(0)+", "+link.get(1));
                                    }
                                } else {
                                    link_tmp.add(link.get(0));
                                    link_tmp.add("");
                                    link_tmp.add(link.get(2));
                                }
                            } else {
                                link_tmp.add(link.get(0));
                                link_tmp.add(link.get(1));
                                link_tmp.add(link.get(2));
                            }

                            if (link.get(4).isEmpty()) {
                                Map<String, String> ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(3));
                                if (ifaceid_ifacename_list != null && !ifaceid_ifacename_list.isEmpty()) {
                                    String[] if_name_iface2 = findIfaceFromNode(link.get(5), ifaceid_ifacename_list);
                                    if (if_name_iface2[0] != null) {
                                        link_tmp.add(link.get(3));
                                        link_tmp.add(if_name_iface2[0]);
                                        link_tmp.add(if_name_iface2[1]);
                                    } else {
                                        link_tmp.add(link.get(3));
                                        link_tmp.add("");
                                        link_tmp.add(link.get(5));
                                        //                                System.out.println("ID iface not found: "+link.get(2)+", "+link.get(3));
                                    }
                                } else {
                                    link_tmp.add(link.get(3));
                                    link_tmp.add("");
                                    link_tmp.add(link.get(5));
                                }
                            } else {
                                link_tmp.add(link.get(3));
                                link_tmp.add(link.get(4));
                                link_tmp.add(link.get(5));
                            }
                            link_tmp.add(link.get(6));
                            links_new.add(link_tmp);
                        }
                    }
                    val.put("links", links_new);
                }
            }

            // find false DP links(is same ports delete CDP links)
            for (Map.Entry<String, Map> area : info_map.entrySet()) {
//                String area_name = area.getKey();
//                System.out.println(area_name);
                Map<String, Object> val = area.getValue();
                if (val != null) {
                    ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                    if (links != null) {
                        ArrayList<ArrayList<ArrayList<String>>> links_switces = new ArrayList();
                        for (ArrayList<String> link : links) {
                            if (!links_switces.isEmpty()) {
                                ArrayList<ArrayList<String>> link_sw_list = new ArrayList();
                                boolean found = false;
                                for (ArrayList<ArrayList<String>> linksSwitce : links_switces) {
                                    link_sw_list = linksSwitce;
                                    ArrayList<String> link_sw = link_sw_list.get(0);
                                    if (!link.get(2).equals("unknown") && link_sw.get(0).equals(link.get(0)) && equalsIfaceName(link_sw.get(2), link.get(2)) ||
                                            !link.get(5).equals("unknown") && link_sw.get(0).equals(link.get(3)) && equalsIfaceName(link_sw.get(2), link.get(5)) ||
                                            !link.get(2).equals("unknown") && link_sw.get(3).equals(link.get(0)) && equalsIfaceName(link_sw.get(5), link.get(2)) ||
                                            !link.get(5).equals("unknown") && link_sw.get(3).equals(link.get(3)) && equalsIfaceName(link_sw.get(5), link.get(5))
                                    ) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    link_sw_list.add(link);
                                } else {
                                    ArrayList<ArrayList<String>> tmp = new ArrayList();
                                    tmp.add(link);
                                    links_switces.add(tmp);
                                }

                            } else {
                                ArrayList<ArrayList<String>> tmp = new ArrayList();
                                tmp.add(link);
                                links_switces.add(tmp);
                            }
                        }

                        JSONParser parser = new JSONParser();
                        ArrayList<ArrayList<String>> links_delete = new ArrayList();
                        for (ArrayList<ArrayList<String>> item : links_switces) {
                            if (item.size() > 1) {
                                ArrayList<ArrayList<String>> links_tmp = new ArrayList();
                                for (ArrayList<String> link : item) {
                                    try {
                                        JSONObject jsonObject = (JSONObject) parser.parse(link.get(6));
                                        Map json = toMap(jsonObject);
                                        if (json.get("type") != null) {
                                            String type = (String) json.get("type");
                                            if (type.equals("cdp")) {
                                                links_tmp.add(link);
                                            }
                                        }
                                    } catch (ParseException ex) {
                                    }
                                }
                                if (links_tmp.size() != item.size()) {
                                    links_delete.addAll(links_tmp);
                                } else {
                                    for (int i = 1; i < links_tmp.size(); i++) {
                                        links_delete.add(links_tmp.get(i));
                                    }
                                }
                            }
                        }
                        ArrayList<ArrayList<String>> links_new = new ArrayList();
                        if (!links_delete.isEmpty()) {
                            for (ArrayList<String> link : links) {
                                boolean found = false;
                                for (ArrayList<String> item_del : links_delete) {
                                    if (link.get(0).equals(item_del.get(0)) &&
                                            link.get(1).equals(item_del.get(1)) &&
                                            link.get(2).equals(item_del.get(2)) &&
                                            link.get(3).equals(item_del.get(3)) &&
                                            link.get(4).equals(item_del.get(4)) &&
                                            link.get(5).equals(item_del.get(5))
                                    ) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    links_new.add(link);
                                } else {
                                    logger.Println("Delete switch link cdp: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
                                }
                            }
                            val.put("links", links_new);
                        }
                    }
                }
            }


        } catch (Exception ex) {
            System.out.println("ex=" + ex);
            ex.printStackTrace(System.out);
        }
        return info_map;
    }

    private boolean compareIfacename(String iface1, String iface2) {
        ArrayList<String> synonim1 = getIface_Synonim(iface1);
        ArrayList<String> synonim2 = getIface_Synonim(iface2);
        for (String it : synonim1) {
            if (synonim2.contains(it)) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<String> getIface_Synonim(String iface_name) {
        ArrayList<String> result = new ArrayList();
        iface_name = iface_name.toLowerCase();
        result.add(iface_name);
        Pattern p = Pattern.compile("^([a-z]+)(\\d+.*)$");
        Matcher m = p.matcher(iface_name);
        if (m.find()) {
//            if(m.group(1).length() == 0) 
//                System.out.println("111111");
            if (m.group(1).length() > 2) {
                String alias3 = m.group(1).substring(0, 3) + m.group(2);
                result.add(alias3);
            }
            if (m.group(1).length() > 1) {
                String alias2 = m.group(1).substring(0, 2) + m.group(2);
                result.add(alias2);
            }
            if (!m.group(1).isEmpty()) {
                String alias1 = m.group(1).substring(0, 1) + m.group(2);
                result.add(alias1);
            }

        }
        return result;
    }

    public boolean writeHashMapToFile1(String filename, Map<String, String[]> map) {
        try {
            try (BufferedWriter outFile = new BufferedWriter(new FileWriter(filename))) {
                for (Map.Entry<String, String[]> entry : map.entrySet()) {
                    String node = entry.getKey();
                    String[] val = entry.getValue();
                    outFile.write(node);
                    for (String item : val) {
                        outFile.write(";" + item);
                    }
                    outFile.write("\n");
                }
            }
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean writeHashMapToFile2(String filename, Map<String, ArrayList<String>> map) {
        try {
            try (BufferedWriter outFile = new BufferedWriter(new FileWriter(filename))) {
                for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                    String node = entry.getKey();
                    ArrayList<String> val = entry.getValue();
                    outFile.write(node);
                    for (String item : val) {
                        outFile.write(";" + item);
                    }
                    outFile.write("\n");
                }
            }
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                System.out.println(ex);
            }
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public ArrayList<String[]> selectOldLinks(ArrayList<String[]> links_in_buff, ArrayList<String[]> links, ArrayList<String[]> dp_links, Map<String, String> information_nodes) {
        ArrayList<String[]> result = new ArrayList();
        for (String[] item : links_in_buff) {
            boolean find = false;
            for (String[] item1 : links) {
                if (item[0].equals(item1[0]) && checkInterfaceName(item[2], item1[2])) {
                    find = true;
                    logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[0] + " iface=" + item1[2] + " is busy from links", logger.DEBUG);
                    break;
                } else if (item[0].equals(item1[3]) && checkInterfaceName(item[2], item1[5])) {
                    find = true;
                    logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[3] + " iface=" + item1[5] + " is busy from links", logger.DEBUG);
                    break;
                } else if (item[3].equals(item1[0]) && checkInterfaceName(item[5], item1[2])) {
                    find = true;
                    logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[0] + " iface=" + item1[2] + " is busy from links", logger.DEBUG);
                    break;
                } else if (item[3].equals(item1[3]) && checkInterfaceName(item[5], item1[5])) {
                    find = true;
                    logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[3] + " iface=" + item1[5] + " is busy from links", logger.DEBUG);
                    break;
                }
            }
            if (!find) {
                find = false;
                for (String[] item1 : dp_links) {
                    if (item[0].equals(item1[0]) && checkInterfaceName(item[2], item1[2])) {
                        find = true;
                        logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[0] + " iface=" + item1[2] + " is busy from dp_links", logger.DEBUG);
                        break;
                    } else if (item[0].equals(item1[3]) && checkInterfaceName(item[2], item1[5])) {
                        find = true;
                        logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[3] + " iface=" + item1[5] + " is busy from dp_links", logger.DEBUG);
                        break;
                    } else if (item[3].equals(item1[0]) && checkInterfaceName(item[5], item1[2])) {
                        find = true;
                        logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[0] + " iface=" + item1[2] + " is busy from dp_links", logger.DEBUG);
                        break;
                    } else if (item[3].equals(item1[3]) && checkInterfaceName(item[5], item1[5])) {
                        find = true;
                        logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node=" + item1[3] + " iface=" + item1[5] + " is busy from dp_links", logger.DEBUG);
                        break;
                    }
                }
                if (!find) {
                    boolean find1 = false;
                    boolean find2 = false;
                    for (Map.Entry<String, String> entry : information_nodes.entrySet()) {
                        String node = entry.getKey();
                        if (!find1 && item[0].equals(node)) {
                            find1 = true;
                        }
                        if (!find2 && item[3].equals(node)) {
                            find2 = true;
                        }
                        if (find1 && find2) {
                            break;
                        }
                    }
                    if (find1 && find2) {
                        String[] mas = new String[6];
                        mas[0] = item[0];
                        mas[1] = item[1];
                        mas[2] = item[2];
                        mas[3] = item[3];
                        mas[4] = item[4];
                        mas[5] = item[5];
                        logger.Println("Adding old links: " + mas[0] + ";" + mas[1] + ";" + mas[2] + " <---> " + mas[3] + ";" + mas[4] + ";" + mas[5], logger.DEBUG);
                        result.add(mas);
                    } else {
                        if (!find1) {
                            logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node_link=" + item[0] + " not exist in information_nodes.", logger.DEBUG);
                        }
                        if (!find2) {
                            logger.Println("Not added old links: " + item[0] + "," + item[2] + " <---> " + item[3] + "," + item[5] + " node_link=" + item[3] + " not exist in information_nodes.", logger.DEBUG);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Map<String, String> selectNodesCDPLLDPGroup(Map<String, String> informationFromNodes, Map<String, Map<String, ArrayList>> walkInformation) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : informationFromNodes.entrySet()) {
            String node = entry.getKey();

            if ((walkInformation.get("cdpCacheAddress") != null && walkInformation.get("cdpCacheAddress").get(node) != null)
                    && (walkInformation.get("lldpRemManAddrIfSubtype") != null && walkInformation.get("lldpRemManAddrIfSubtype").get(node) != null)
                    && (walkInformation.get("lldpRemChassisId") != null && walkInformation.get("lldpRemChassisId").get(node) != null)) {
                logger.Println("SelectNodesCDPLLDPGroup: node=" + node + " - cdp_lldp", logger.DEBUG);
                result.put(node, "cdp_lldp");
            } else if (walkInformation.get("cdpCacheAddress") != null && walkInformation.get("cdpCacheAddress").get(node) != null) {
                logger.Println("SelectNodesCDPLLDPGroup: node=" + node + " - cdp", logger.DEBUG);
                result.put(node, "cdp");
            } else if ((walkInformation.get("lldpRemManAddrIfSubtype") != null && walkInformation.get("lldpRemManAddrIfSubtype").get(node) != null)
                    || (walkInformation.get("lldpRemChassisId") != null && walkInformation.get("lldpRemChassisId").get(node) != null)) {
                logger.Println("SelectNodesCDPLLDPGroup: node=" + node + " - lldp", logger.DEBUG);
                result.put(node, "lldp");
            } else {
                logger.Println("SelectNodesCDPLLDPGroup: node=" + node + " - none", logger.DEBUG);
                result.put(node, "none");
            }
        }
        return result;
    }

    public boolean check_CDP_LDP_NONE_Neighbors(String priznak1, String priznak2) {
        return ((priznak1.equals("none") && priznak2.equals("none")
                || priznak1.equals("none") && priznak2.equals("cdp")
                || priznak1.equals("none") && priznak2.equals("lldp")
                || priznak1.equals("none") && priznak2.equals("cdp_lldp"))
                || (priznak1.equals("cdp") && priznak2.equals("none")
                || priznak1.equals("cdp") && priznak2.equals("lldp"))
                || (priznak1.equals("lldp") && priznak2.equals("none")
                || priznak1.equals("lldp") && priznak2.equals("cdp"))
                || priznak1.equals("cdp_lldp") && priznak2.equals("none"))
                && ((priznak2.equals("none") && priznak1.equals("none")
                || priznak2.equals("none") && priznak1.equals("cdp")
                || priznak2.equals("none") && priznak1.equals("lldp")
                || priznak2.equals("none") && priznak1.equals("cdp_lldp"))
                || (priznak2.equals("cdp") && priznak1.equals("none")
                || priznak2.equals("cdp") && priznak1.equals("lldp"))
                || (priznak2.equals("lldp") && priznak1.equals("none")
                || priznak2.equals("lldp") && priznak1.equals("cdp"))
                || priznak2.equals("cdp_lldp") && priznak1.equals("none"));
    }

    private void mapToIndexSubProcessor(Map<String, Object> map, String key, Object value, IndexWriter w, String area, String node) {
        if (value != null) {
            switch (value) {
                case String string -> {
                    //            System.out.println(value);
                    String[] mas = string.split("\n");
                    for (String item : mas) {
                        if (item.length() > 32765) {

                            int i = 0;
                            while (true) {
                                String val;
                                try {
                                    val = item.substring(32765 * i, 32765 * (i + 1));
                                } catch (Exception ex) {
                                    val = item.substring(32765 * i);
                                }
                                addDoc(w, val, area, node);
                                if (val.length() < 32765) {
                                    break;
                                }
                                i = i + 1;
                            }
                        } else {
                            addDoc(w, item, area, node);
                        }
                    }
                }
                case List list -> {
                    for (Object object : list) {
                        mapToIndexSubProcessor(map, key, object, w, area, node);
                    }
                }
                case @SuppressWarnings("unused")Map map1 -> {
                    try {
                        //noinspection unchecked
                        Map<String, Object> subMap = (Map<String, Object>) value;
                        mapToIndex(subMap, w, area, node);
                    } catch (ClassCastException e) {
                    }
                }
                case null, default -> throw new IllegalArgumentException(String.valueOf(value));
            }
        }
    }

    private void mapToIndex(Map<String, Object> map, IndexWriter w, String area, String node) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            mapToIndexSubProcessor(map, key, value, w, area, node);
        }
    }

    public void indexFullText(Map<String, Map> info, String index_directory) {
        try {
            final Directory index = FSDirectory.open(Paths.get(index_directory));
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            try (IndexWriter w = new IndexWriter(index, config)) {
                for (Map.Entry<String, Map> entry : info.entrySet()) {
                    String area = entry.getKey();
                    Map<String, Map> area_info = entry.getValue();
                    Map<String, Map> nodes_info = area_info.get("nodes_information");
                    if (nodes_info != null) {
                        for (Map.Entry<String, Map> entry1 : nodes_info.entrySet()) {
                            String node = entry1.getKey();
                            Map node_info = entry1.getValue();
                            mapToIndex(node_info, w, area, node);
                        }
                    }
                }
            }
        } catch (IOException ex) {
        }
    }

    private static void addDoc(IndexWriter w, String text, String area, String node) {
        Document doc = new Document();
        doc.add(new TextField("text", text, Field.Store.YES));
//        doc.add(new TextField("text", text.replaceAll("[\\.|/\\\\;:]", " "), Field.Store.YES));

        // use a string field for isbn because we don't want it tokenized
//        doc.add(new StringField("origin", text, Field.Store.YES));
        doc.add(new StringField("area", area, Field.Store.YES));
        doc.add(new StringField("node", node, Field.Store.YES));
        try {
            w.addDocument(doc);
        } catch (IOException ex) {
        }
    }

    public void clearIndex(String mapfile, Map<String, ArrayList<ArrayList<String>>> index) {
        try {
            for (Map.Entry<String, ArrayList<ArrayList<String>>> entry : index.entrySet()) {
                String key = entry.getKey();
                ArrayList<ArrayList<String>> val = entry.getValue();
                ArrayList<ArrayList<String>> val_new = new ArrayList();
                for (ArrayList<String> it : val) {
                    if (!it.get(1).endsWith(mapfile)) {
                        val_new.add(it);
                    }
                }
                index.put(key, val_new);
            }
        } catch (java.util.ConcurrentModificationException ex) {
        }
    }

    public void indexing(Map<String, ArrayList<ArrayList<String>>> index) {
        logger.Println("Indexing map file: " + map_file, logger.DEBUG);
        filesToIndexing(map_file, index);

        File history = new File(history_dir);
        if (history.isDirectory()) {
            File[] list_dir = history.listFiles();
            for (File file : list_dir) {
                String filename = file.getAbsoluteFile().toString();
                String short_filename = file.getAbsoluteFile().getName();
                logger.Println("Indexing map file: " + short_filename, logger.DEBUG);
                filesToIndexing(filename, index);
            }
        }

        // write full text index
        logger.Println("Start Indexing Full text search map file - " + map_file + "...", logger.DEBUG);
        Map<String, Map> info = utils.readJSONFile(map_file);
        indexFullText(info, Neb.index_dir);
        logger.Println("Stop Indexing Full text search map file - " + map_file + ".", logger.DEBUG);

    }

    public void filesToIndexing(String file, Map<String, ArrayList<ArrayList<String>>> index) {
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        String date_str;
        Matcher m = p.matcher(file);
        if (m.find()) {
            date_str = m.group(1).replace("-", " ");
        } else {
            date_str = getFileCreateTime(file);
        }

        Map<String, Map> info = utils.readJSONFile(file);
        for (Map.Entry<String, Map> entry : info.entrySet()) {
            String area = entry.getKey();
//            System.out.println("area="+area);
            Map<String, Map> area_info = entry.getValue();
            Map<String, Map> nodes_info = area_info.get("nodes_information");
            if (nodes_info != null) {
                for (Map.Entry<String, Map> entry1 : nodes_info.entrySet()) {
                    String node = entry1.getKey();
                    Map node_info = entry1.getValue();

                    String sysname = null;
                    if (node_info.get("general") != null && ((Map) node_info.get("general")).get("sysname") != null) {
                        sysname = (String) ((Map) node_info.get("general")).get("sysname");
                        addToIndexStruct(file, date_str, area, sysname, sysname + "(" + node + ")", node, "", index);
                    }
                    String name = node;
                    if (sysname != null) {
                        name = name + "(" + sysname + ")";
                    }
                    addToIndexStruct(file, date_str, area, node, name, node, "", index);

                    Map<String, Map> interfaces = (Map<String, Map>) node_info.get("interfaces");
                    if (interfaces != null) {
                        for (Map.Entry<String, Map> entry2 : interfaces.entrySet()) {
                            Map iface_info = entry2.getValue();
                            if (iface_info.get("ip") != null) {
                                ArrayList<String> ip_list = (ArrayList<String>) iface_info.get("ip");
                                for (String ip : ip_list) {
                                    ip = ip.split(" ")[0].split("/")[0];
                                    name = ip;
                                    if (sysname != null) {
                                        name = name + "(" + sysname + ")";
                                    }
                                    addToIndexStruct(file, date_str, area, ip, name, node, "", index);
                                }
                            }
                            if (iface_info.get("mac") != null) {
                                String mac = normalizeMAC((String) iface_info.get("mac"));
                                name = mac;
                                if (sysname != null) {
                                    name = name + "(" + sysname + ")";
                                }
                                addToIndexStruct(file, date_str, area, mac, name, node, "", index);
                            }
                        }
                    }
                }
            }

            ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>) area_info.get("mac_ip_port");
            if (mac_ip_port != null) {
                for (ArrayList<String> mip : mac_ip_port) {
                    if (mip.size() > 5) {
                        String mac = normalizeMAC(mip.get(0));
                        String ip = mip.get(1);
                        String node1 = mip.get(2);
                        String port = mip.get(4);
                        if (!mac.equals("unknown_mac") || !mac.isEmpty()) {
                            String name1 = mac;
                            if (!ip.equals("unknown_ip")) {
                                name1 = ip + "(" + mac + ")";
                            }
                            addToIndexStruct(file, date_str, area, mac, name1, node1, port, index);
                        }
                        if (!ip.equals("unknown_ip")) {
                            String name1 = ip;
                            if (!mac.equals("unknown_mac") || !mac.isEmpty()) {
                                name1 = ip + "(" + mac + ")";
                            }
                            addToIndexStruct(file, date_str, area, ip, name1, node1, port, index);
                        }
                    }
                }
            }
        }

    }

    private String normalizeMAC(String mac) {
        String out = "";
        mac = mac.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
        if (mac.length() == 12 && !mac.equals("unknown_mac")) {
            String[] mas = mac.split("");
            out = mas[0] + mas[1] + ":" + mas[2] + mas[3] + ":" + mas[4] + mas[5] + ":" + mas[6] + mas[7] + ":" + mas[8] + mas[9] + ":" + mas[10] + mas[11];
        }

        return out;
    }

    private void addToIndexStruct(String file, String data, String area, String mac_ip_sysname, String name, String node, String port, Map<String, ArrayList<ArrayList<String>>> index) {
        if (!mac_ip_sysname.isEmpty()) {
            if (index.get(mac_ip_sysname) != null) {
                ArrayList<ArrayList<String>> list_tmp = index.get(mac_ip_sysname);

                boolean find = false;
                for (ArrayList<String> item : list_tmp) {
                    if (item.get(0).equals(data) || item.get(1).equals(file)) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    ArrayList list_tmp1 = new ArrayList();
                    list_tmp1.add(data);
                    list_tmp1.add(file);
                    list_tmp1.add(area);
                    list_tmp1.add(name);
                    list_tmp1.add(node);
                    list_tmp1.add(port);
                    list_tmp.add(list_tmp1);
                }
            } else {
                ArrayList<ArrayList<String>> list_tmp = new ArrayList();
                ArrayList list_tmp1 = new ArrayList();
                list_tmp1.add(data);
                list_tmp1.add(file);
                list_tmp1.add(area);
                list_tmp1.add(name);
                list_tmp1.add(node);
                list_tmp1.add(port);
                list_tmp.add(list_tmp1);
                index.put(mac_ip_sysname, list_tmp);
            }
        }
    }

    //    public boolean DeletedNode(String node, boolean force) {
//        boolean is_changed=false;
//        if(Neb.nodes_info.get(node) != null) {
//            String[] link = new String[6];
//            ArrayList<Integer> pos = new ArrayList();
//            for(int i=0; i<Neb.links_info.size(); i++) {
//                String[] item = Neb.links_info.get(i);
//                if(item[0].equals(node)) {
//                    link[0]=item[0]; link[1]=item[1]; link[2]=item[2];
//                    link[3]=item[3]; link[4]=item[4]; link[5]=item[5];
//                    pos.add(i);
//                } else if(item[3].equals(node)) {
//                    link[0]=item[3]; link[1]=item[4]; link[2]=item[5];
//                    link[3]=item[0]; link[4]=item[1]; link[5]=item[2];
//                    pos.add(i);
//                }                        
//            }
//            if(pos.size() == 1 || force) {
//                is_changed=true;
//                Map<String, String[]> mac_ArpMacTable_tmp = new HashMap();
//                synchronized(Neb.mac_ArpMacTable) { mac_ArpMacTable_tmp.putAll(Neb.mac_ArpMacTable); }
//                for (Map.Entry<String, String[]> entry : mac_ArpMacTable_tmp.entrySet()) {
//                    String[] value = entry.getValue();
//                    String[] mas = new String[6];
//                    if(value[2].equals(node) || value[1].equals(node)) {
//                        mas[0]=value[0]; mas[1]=value[1]; mas[2]=link[3];
//                        mas[3]=link[4]; mas[4]=link[5]; mas[5]=value[5];
//                        synchronized(Neb.mac_ArpMacTable) { 
//                            Neb.mac_ArpMacTable.replace(entry.getKey(), mas);
//                            logger.Println("Replace mac_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
//                        }
//                    }
//                }
//
//                Map<String, String[]> ip_ArpMacTable_tmp = new HashMap();
//                synchronized(Neb.ip_ArpMacTable) { ip_ArpMacTable_tmp.putAll(Neb.ip_ArpMacTable); }
//                for (Map.Entry<String, String[]> entry : ip_ArpMacTable_tmp.entrySet()) {
//                    String[] value = entry.getValue();
//                    String[] mas = new String[6];
//                    if(value[2].equals(node) || value[1].equals(node)) {
//                        mas[0]=value[0]; mas[1]=value[1]; mas[2]=link[3];
//                        mas[3]=link[4]; mas[4]=link[5]; mas[5]=value[5];                                
//                        synchronized(Neb.ip_ArpMacTable) { 
//                            Neb.ip_ArpMacTable.replace(entry.getKey(), mas); 
//                            logger.Println("Replace ip_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
//                        }
//                    }
//                }
//                // adding deleted nodes to ARP_MAC table
//                String[] node_info = Neb.nodes_info.get(node);
//                if(node_info != null && node_info.length > 10) {
//                    String[] mas = new String[6];
//                    Pattern p = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
//                    String line=node_info[0];
//                    for(int i=1; i<node_info.length; i++) line=line+";"+node_info[i];
//                    Matcher m = p.matcher(line);
//                    String mac="";
//                    if(m.find()) mac=m.group(1);
//
//                    if(!mac.equals("")) mas[0]=mac; else mas[0]=node_info[0];
//                    mas[1]=node_info[0];
//                    mas[2]=link[3]; mas[3]=link[4]; mas[4]=link[5];
//                    mas[5]=String.valueOf(System.currentTimeMillis());
//
//                    Matcher m2 = p.matcher(mas[0]);
//                    String mac_find="";
//                    if(m2.find()) mac_find=m2.group(1);
//
//                    Pattern p1 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
//                    Matcher m1 = p1.matcher(mas[1]);
//                    String ip_find="";
//                    if(m1.find()) ip_find=m1.group(1);
//
//                    if(Neb.mac_ArpMacTable.get(mac_find) == null && Neb.ip_ArpMacTable.get(ip_find) == null) {
//
//                        logger.Println("Adding deleted node to arp_mac_table: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
//                        synchronized(Neb.mac_ArpMacTable) { Neb.mac_ArpMacTable.put(mas[0], mas); } 
//                        synchronized(Neb.ip_ArpMacTable) { Neb.ip_ArpMacTable.put(mas[1], mas); }
//                    } else {
//                        logger.Println("Not adding deleted node to arp_mac_table. Is exist: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
//                    }
//
//                }  
//                synchronized(Neb.extended_info) { 
//                    Neb.extended_info.put(node, Neb.nodes_info.get(node)); 
//                    logger.Println("Put deleted node: "+node+" to extended_info.", logger.DEBUG); 
//                }
//                synchronized(Neb.nodes_info) { 
//                    Neb.nodes_info.remove(node); 
//                    logger.Println("Remove deleted node: "+node+" from nodes_info.", logger.DEBUG); 
//                }
//                synchronized(Neb.links_info) {
//                    for(int p : pos) Neb.links_info.remove(p);
//                    logger.Println("Node: "+node+" remove from links_info."+Neb.links_info.get(pos.get(0))[0]+","+Neb.links_info.get(pos.get(0))[2]+" <===> "+Neb.links_info.get(pos.get(0))[3]+","+Neb.links_info.get(pos.get(0))[5], logger.DEBUG); 
//                }
//                synchronized(Neb.text_info) { 
//                    Neb.text_info.remove(node); 
//                    logger.Println("Remove deleted node: "+node+" from text_info.", logger.DEBUG); 
//                }
//            } else if(pos.size() == 0) {
//                is_changed=true;
//                
//                Map<String, String[]> mac_ArpMacTable_tmp = new HashMap();
//                synchronized(Neb.mac_ArpMacTable) { mac_ArpMacTable_tmp.putAll(Neb.mac_ArpMacTable); }
//                for (Map.Entry<String, String[]> entry : mac_ArpMacTable_tmp.entrySet()) {
//                    String[] value = entry.getValue();
//                    String[] mas = new String[6];
//                    if(value[2].equals(node)) {
//                        synchronized(Neb.mac_ArpMacTable) { 
//                            Neb.mac_ArpMacTable.remove(entry.getKey());
//                            logger.Println("Delete mac_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5], logger.DEBUG); 
//                        }
//                    }
//                }
//                
//                Map<String, String[]> ip_ArpMacTable_tmp = new HashMap();
//                synchronized(Neb.ip_ArpMacTable) { ip_ArpMacTable_tmp.putAll(Neb.ip_ArpMacTable); }
//                for (Map.Entry<String, String[]> entry : ip_ArpMacTable_tmp.entrySet()) {
//                    String[] value = entry.getValue();
//                    String[] mas = new String[6];
//                    if(value[2].equals(node)) {
//                        synchronized(Neb.ip_ArpMacTable) { 
//                            Neb.ip_ArpMacTable.remove(entry.getKey());
//                            logger.Println("Delete ip_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5], logger.DEBUG); 
//                        }
//                    }
//                }                
//                
//                synchronized(Neb.extended_info) { 
//                    Neb.extended_info.put(node, Neb.nodes_info.get(node));
//                    logger.Println("Put deleted node: "+node+" to extended_info.", logger.DEBUG); 
//                }
//                synchronized(Neb.nodes_info) { 
//                    Neb.nodes_info.remove(node); 
//                    logger.Println("Remove deleted node: "+node+" from nodes_info.", logger.DEBUG); 
//                }
//                synchronized(Neb.text_info) { 
//                    Neb.text_info.remove(node);
//                    logger.Println("Remove deleted node: "+node+" from text_info.", logger.DEBUG);
//                }
//            } else logger.Println("Not delete node: "+node+" num links = "+pos.size(), logger.DEBUG);
//        }
//        return is_changed;
//    }
    public void removeOldFiles(String folder, int history_day) {
        File f_folder = new File(folder);
        if (f_folder.exists() && f_folder.isDirectory()) {
            File[] folderEntries = f_folder.listFiles();
            for (File file : folderEntries) {
                if (file.isFile()) {
                    if ((System.currentTimeMillis() - getFileCreateTime_mSec(file.getPath())) > (long) history_day * 24 * 60 * 60 * 1000) {
                        file.delete();
                        //                    System.out.println("Delete file: "+file.getName());
                        logger.Println("Delete file: " + file.getName(), logger.DEBUG);
                    }
                }

            }
        }
    }

    public boolean mapToFile(Map map, String filename, long delay) {
        Gson gson = new Gson();
        String str = gson.toJson(map);
        return writeStrToFile(filename, prettyJSONOut(str), delay);
    }

    public boolean mapToFile(ArrayList list, String filename, long delay) {
        Gson gson = new Gson();
        String str = gson.toJson(list);
        return writeStrToFile(filename, prettyJSONOut(str), delay);
    }

    public boolean map_To_NebMapFile(Map map, String filename, long delay) {
        Gson gson = new Gson();

        writeStrToFile(filename, "", delay);
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) map).entrySet()) {
            String key = entry.getKey();
            Map value = entry.getValue();
            Map<String, Map> map_tmp = new HashMap();
            map_tmp.put(key, value);
            String str = prettyJSONOut(gson.toJson(map_tmp));
            str = "########################## " + key + " ####################################\n" + str + "\n";
            if (!appendStrToFile(filename, str)) {
                return false;
            }
        }

        return true;
    }

    public String prettyJSONOut(String str) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(str);
        String result = gson.toJson(je);

        return result;
    }

    public Map uniqal_Nodes_Information(Map<String, Map> nodes_information) {
        Map result = new HashMap();
        Map<String, String> ip_collection = new HashMap();

        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            Map val = entry.getValue();
            if (ip_collection.get(node) == null) {
                result.put(node, val);
            } else {
                logger.Println("Not uniqal node: " + node, logger.DEBUG);
            }
            ip_collection.putAll(getIpNode(val));
        }

        return result;
    }

    public Map uniqal_Nodes_Information(Map<String, Map> nodes_information, ArrayList<String> networks) {
        Map<String, Map> result = new HashMap();
        Map<String, String> ip_node = new HashMap();

        Map<String, Integer> node_priority = new HashMap();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            int pos = 0;
            boolean found = false;
            for (String network : networks) {
                if (inside_Network(node, network)) {
                    found = true;
                    break;
                }
                pos = pos + 1;
            }
            if (found) {
                node_priority.put(node, pos);
            }
        }

        Gson gson = new Gson();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            Map val = entry.getValue();
            if (ip_node.get(node) == null) {
                result.put(node, val);
            } else {
                String node_exist = ip_node.get(node);
                Map val_exist = nodes_information.get(node_exist);

                if ((node_priority.get(node) != null && node_priority.get(node_exist) != null &&
                        node_priority.get(node) < node_priority.get(node_exist))
                ) {
                    result.remove(node_exist);
                    logger.Println("Remove not uniqal node: " + node_exist, logger.DEBUG);
                    result.put(node, val);
                    logger.Println("Adding uniqal node: " + node, logger.DEBUG);
                } else if (node_priority.get(node) != null && node_priority.get(node_exist) == null) {
                    String str = gson.toJson(val);
                    String str_exist = "";
                    if (val_exist != null) {
                        str_exist = gson.toJson(val_exist);
                    }
                    if (str.length() > str_exist.length()) {
                        result.remove(node_exist);
                        logger.Println("Remove not uniqal node: " + node_exist, logger.DEBUG);
                        result.put(node, val);
                        logger.Println("Adding uniqal node: " + node, logger.DEBUG);
                    } else {
                        logger.Println("Not uniqal node: " + node, logger.DEBUG);
                    }
                } else {
                    logger.Println("Not uniqal node: " + node, logger.DEBUG);
                }

            }

            Map<String, String> ip_map = getIpNode(val);
            for (Map.Entry<String, String> entry1 : ip_map.entrySet()) {
                String ip = entry1.getKey();
                ip_node.put(ip, node);
            }

        }

        return result;
    }

    private Map<String, String> getIpNode(Map<String, Map> node_information) {
        // Get Ip => node
        Map<String, String> ip_node = new HashMap();
        Map<String, Map> interfaces = (Map) node_information.get("interfaces");
        if (interfaces != null && !interfaces.isEmpty()) {
            for (Map.Entry<String, Map> entry1 : interfaces.entrySet()) {
                ArrayList<String> ip_list = new ArrayList();
                if (entry1.getValue().get("ip") != null) {
                    if (entry1.getValue().get("ip") instanceof String string) {
                        ip_list.add(string);
                    } else if (entry1.getValue().get("ip") instanceof ArrayList) {
                        ip_list = (ArrayList<String>) entry1.getValue().get("ip");
                    }
                }
                if (!ip_list.isEmpty()) {
                    for (String ip : ip_list) {
                        ip = ip.split("[/\\s]")[0];
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            if (!inside_Networks(ip, Neb.not_correct_networks)) {
                                ip_node.put(ip, ip);
                            }
                        }
                    }
                }
            }
        }

        return ip_node;
    }

    //    private Map<String, String> GetIpFromNone(Map<String, Map> nodes_information) {
//        Map<String, String> result = new HashMap();
//
//        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
//            String node = entry.getKey();
//            result.put(node, node);
//            Map<String, Map> map_interf = ((Map<String, Map>)entry.getValue()).get("interfaces");
//            if(map_interf != null && map_interf.size() > 0) {
//                for(Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
//                    ArrayList<String> list = (ArrayList)entry1.getValue().get("ip");
//                    if(list != null && list.size() > 0) {
//                        for(String ip : list) {
//                            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
//                                ip=ip.split("/")[0];
//                                result.put(ip, ip);
//                            }
//                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                                ip=ip.split(" ")[0];
//                                result.put(ip, ip);
//                            }
//                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                                result.put(ip, ip);
//                            } else logger.Println("Bad format ip address - "+ip, logger.DEBUG);
//                        }
//                    }
//                }
//            }
//        }
//        
//        return result;
//    }
    public ArrayList<ArrayList<ArrayList<String>>> normalizationLinks(Map<String, Map> nodes_information,
                                                                      ArrayList<String> networks, ArrayList<String> not_correct_networks) {
        ArrayList<ArrayList<ArrayList<String>>> result = new ArrayList();

        // Get mac => [node, iface]
        Map<String, String[]> mac_node_port = getMacNode_Iface(nodes_information);
//        mapToFile(mac_node_port, "mac_node_port", Neb.DELAY_WRITE_FILE);


        // Get Ip => node
        Map<String, String> ip_node = getIpFromNodes(nodes_information, networks, not_correct_networks);

        ArrayList<ArrayList<String>> links = new ArrayList();
        ArrayList<ArrayList<String>> links_extended = new ArrayList();
        Gson gson = new Gson();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            Map val = entry.getValue();
//            if(node.equals("10.96.249.1"))
//                System.out.println("1111111111111111");
            if (val.get("advanced") != null) {
                Map<String, ArrayList<Map<String, String>>> links_map = (Map) ((Map) val.get("advanced")).get("links");
                if (links_map != null && !links_map.isEmpty()) {
                    for (Map.Entry<String, ArrayList<Map<String, String>>> entry1 : links_map.entrySet()) {
                        String iface_local = entry1.getKey();
                        ArrayList<Map<String, String>> val1 = entry1.getValue();
                        for (Map<String, String> link : val1) {
                            String node_remote = "unknown";
                            String str_tmp = link.get("remote_ip");
                            if (str_tmp != null && !str_tmp.isEmpty()) {
//                                if(str_tmp.equals("169.254.244.211"))
//                                    System.out.println("11111");
                                node_remote = nameToIpExt(str_tmp, mac_node_port);
                            }
                            if (node_remote.equals("unknown")) {
                                if (link.get("remote_id") != null) {
                                    node_remote = link.get("remote_id");
                                }
                            }

                            String port_remote = "unknown";
                            str_tmp = link.get("remote_port");
                            if (str_tmp != null && !str_tmp.isEmpty()) {
                                if (!checkMACFromName(str_tmp)) {
                                    port_remote = str_tmp;
                                } else {
                                    String mac = extractMACFromName(str_tmp);
                                    mac = mac.toLowerCase().replaceAll("[.:-]", "");
                                    if (mac_node_port.get(mac) != null) {
                                        logger.Println("Replace remote interface: " + port_remote + " to - " + mac_node_port.get(mac)[1], logger.DEBUG);
//                                        System.out.println("Replace remote interface: "+port_remote+" to - "+mac_node_port.get(mac)[1]);
                                        port_remote = mac_node_port.get(mac)[1];
                                    }
                                }
                            }
                            str_tmp = link.get("remote_port_id");
                            if (str_tmp != null && !str_tmp.isEmpty()) {
                                if (!checkMACFromName(str_tmp)) {
                                    port_remote = str_tmp;
                                } else {
                                    String mac = extractMACFromName(str_tmp);
                                    mac = mac.toLowerCase().replaceAll("[.:-]", "");
                                    if (mac_node_port.get(mac) != null) {
                                        //                                System.out.println("Replace remote interface: "+port_remote+" to - "+mac_node.get(mac)[1]);
                                        port_remote = mac_node_port.get(mac)[1];
                                    }
                                }
                            }

                            if (node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && !node.startsWith("127.")) {
                                if (!node.equals(ip_node.get(node)) && ip_node.get(node) != null) {
                                    logger.Println("Replace: " + node + " to: " + ip_node.get(node), logger.DEBUG);
                                    //                                System.out.println("Replace: "+node+" to: "+ip_node.get(node));
                                }
                                if (ip_node.get(node) != null) {
                                    node = ip_node.get(node);
                                }
                            }
//                            if (node_remote.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && !node_remote.startsWith("127.")) {
//                                if (!node_remote.equals(ip_node.get(node_remote)) && ip_node.get(node_remote) != null) {
//                                    logger.Println("Replace: " + node_remote + " to: " + ip_node.get(node_remote), logger.DEBUG);
//                                    //                                System.out.println("Replace: "+node_remote+" to: "+ip_node.get(node_remote));
//                                }
//                                if (ip_node.get(node_remote) != null) {
//                                    node_remote = ip_node.get(node_remote);
//                                }
//                            }
                            ArrayList<String> mas = new ArrayList();
                            mas.add(node);
                            mas.add(iface_local);
                            mas.add(node_remote);
                            mas.add(port_remote);
                            mas.add(gson.toJson(link));
//                            if(!mas.get(3).equals("unknown")) 
//                            {
                            if (!mas.get(0).equals(mas.get(2))) {
                                if (!inside_Networks(mas.get(2), not_correct_networks)) {
                                    links.add(mas);
                                } else {
                                    logger.Println("Remove link(not_correct_networks):" + mas.get(0) + ", " + mas.get(1) + " <---> " + mas.get(2) + ", " + mas.get(3), logger.DEBUG);
                                }
                            } else {
                                logger.Println("Remove link(source and dest equals):" + mas.get(0) + ", " + mas.get(1) + " <---> " + mas.get(2) + ", " + mas.get(3), logger.DEBUG);
                                //                                System.out.println("Remove link(source and dest equals):"+mas.get(0)+", "+mas.get(1)+" <---> "+mas.get(2)+", "+mas.get(3));
                            }
//                            }
//                            else links_extended.add(mas);
                        }
                    }
                }
            }
        }

        // checking and replace short interface to full name interface
        for (ArrayList<String> mas : links) {
            String node1 = mas.get(0);
            String iface1 = mas.get(1);
            if (nodes_information.get(node1) != null && nodes_information.get(node1).get("interfaces") != null) {
                Map<String, Map> interfaces_map = (Map<String, Map>) nodes_information.get(node1).get("interfaces");
                boolean find = false;
                if (interfaces_map.get(iface1) == null) {
                    for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                        String interfacename = entry.getKey();
                        if (equalsIfaceName(iface1, interfacename)) {
                            mas.set(1, interfacename);
                            logger.Println("Replace: " + node1 + " " + iface1 + " to: " + node1 + " " + mas.get(1), logger.DEBUG);
//                            System.out.println("Replace: "+node1+" "+iface1+" to: "+node1+" "+mas.get(1));
                            find = true;
                            break;
                        }
                    }
                } else {
                    find = true;
                }
                if (!find) {
                    logger.Println(node1 + " " + mas.get(1) + " not finded interfece from node.", logger.DEBUG);
//                   System.out.println(node1+" "+mas.get(1)+" not finded interfece from node.");
                }
            }

            String node2 = mas.get(2);
            String iface2 = mas.get(3);
            if (nodes_information.get(node2) != null && nodes_information.get(node2).get("interfaces") != null) {
                Map<String, Map> interfaces_map = (Map<String, Map>) nodes_information.get(node2).get("interfaces");
                boolean find = false;
                if (interfaces_map.get(iface2) == null) {
                    for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                        String interfacename = entry.getKey();
                        if (equalsIfaceName(iface2, interfacename)) {
                            mas.set(3, interfacename);
                            logger.Println("Replace: " + node2 + " " + iface2 + " to: " + node2 + " " + mas.get(3), logger.DEBUG);
//                            System.out.println("Replace: "+node2+" "+iface2+" to: "+node2+" "+mas.get(3));
                            find = true;
                            break;
                        }
                    }
                } else {
                    find = true;
                }
                if (!find) {
                    logger.Println(node2 + " " + mas.get(3) + " not finded interfece from node.", logger.DEBUG);
//                   System.out.println(node2+" "+mas.get(3)+" not finded interfece from node.");
                }
            }

        }

        // delete duplicate link
        ArrayList<ArrayList<String>> links_new = new ArrayList();
        for (int i = 0; i < links.size(); i++) {
            ArrayList<String> mas1 = links.get(i);
            ArrayList<ArrayList<String>> links_duplicate = new ArrayList();
            links_duplicate.add(mas1);
            for (int j = i + 1; j < links.size(); j++) {
                ArrayList<String> mas2 = links.get(j);
                if ((mas1.get(0).equals(mas2.get(0)) && equalsIfaceName(mas1.get(1), mas2.get(1)) && mas1.get(2).equals(mas2.get(2))) && equalsIfaceName(mas1.get(3), mas2.get(3))
                        || (mas1.get(2).equals(mas2.get(2)) && equalsIfaceName(mas1.get(3), mas2.get(3)) && mas1.get(0).equals(mas2.get(0))) && equalsIfaceName(mas1.get(1), mas2.get(1))
                        || (mas1.get(0).equals(mas2.get(2)) && equalsIfaceName(mas1.get(1), mas2.get(3)) && mas1.get(2).equals(mas2.get(0))) && equalsIfaceName(mas1.get(3), mas2.get(1))
                        || (mas1.get(2).equals(mas2.get(0)) && equalsIfaceName(mas1.get(3), mas2.get(1))) && mas1.get(0).equals(mas2.get(2)) && equalsIfaceName(mas1.get(1), mas2.get(3))) {
                    links_duplicate.add(mas2);
                    links.remove(j);
                    j--;
                    logger.Println("Link: " + mas1.get(0) + " " + mas1.get(1) + " <---> " + mas1.get(2) + " " + mas1.get(3) + " dublicate to link: " + mas2.get(0) + " " + mas2.get(1) + " <---> " + mas2.get(2) + " " + mas2.get(3), logger.DEBUG);
//                    System.out.println("Link: "+mas1.get(0)+" "+mas1.get(1)+" <---> "+mas1.get(2)+" "+mas1.get(3)+" dublicate to link: "+mas2.get(0)+" "+mas2.get(1)+" <---> "+mas2.get(2)+" "+mas2.get(3));                    
                }
            }

            if (links_duplicate.size() > 1) {
                ArrayList<String[]> links_score = new ArrayList();
                for (ArrayList<String> link : links_duplicate) {
                    String[] link_score = new String[6];
                    link_score[0] = link.get(0);
                    link_score[1] = link.get(1);
                    link_score[2] = link.get(2);
                    link_score[3] = link.get(3);
                    link_score[4] = link.get(4);
                    int score = 0;
                    if (link.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        score = score + 100;
                    }
                    score = score + link.get(1).length();
                    if (link.get(2).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        score = score + 100;
                    }
                    score = score + link.get(3).length();
                    link_score[5] = Integer.toString(score);
                    links_score.add(link_score);
                }
                ArrayList<String> link_normal = new ArrayList();
                int max_score = 0;
                for (String[] link : links_score) {
                    int score = Integer.parseInt(link[5]);
                    if (score > max_score) {
                        max_score = score;
                        link_normal = new ArrayList();
                        link_normal.add(link[0]);
                        link_normal.add(link[1]);
                        link_normal.add(link[2]);
                        link_normal.add(link[3]);
                        link_normal.add(link[4]);
                    }
                }
                links_new.add(link_normal);
                logger.Println("Adding normal link: " + link_normal.get(0) + " " + link_normal.get(1) + " <---> " + link_normal.get(2) + " " + link_normal.get(3), logger.DEBUG);
            } else {
                links_new.add(mas1);
            }

        }
        links = links_new;

        // delete link with node 127.0.0.1
        for (int i = 0; i < links.size(); i++) {
            ArrayList<String> mas = links.get(i);
            if (mas.get(0).startsWith("127.") && mas.get(2).startsWith("127.")) {
                logger.Println("Remove link: " + mas.get(0) + " " + mas.get(1) + " <---> " + mas.get(2) + " " + mas.get(3), logger.DEBUG);
//                System.out.println("Remove link: "+mas.get(0)+" "+mas.get(1)+" <---> "+mas.get(2)+" "+mas.get(3));                    
                links.remove(i);
                i--;
            }
        }

        // replace \n and space
        ArrayList<ArrayList<String>> new_list = new ArrayList();
        for (ArrayList<String> mas : links) {
            ArrayList<String> mas1 = new ArrayList();
            for (int i = 0; i < 4; i++) {
                String val = mas.get(i);
//                val = val.replace("\n", "").replaceAll("\\s+", "_").replace("\"", "_").replace("\'", "_").replace("\\;", "_").replace("\\:", "_");
                val = val.replace("\n", "").replace("\"", "_").replace("'", "_").replace("\\;", "_").replace("\\:", "_");
                mas1.add(val);
            }
            mas1.add(mas.get(4));
            new_list.add(mas1);
        }
        links = new_list;

//        // replace link node to format ip address and find in nodes list, if no to remove link
//        links_new = new ArrayList();
//        for(ArrayList<String> link : links) {
//            String node = link.get(3);
//            if (!node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                String res = utils.nameToIp(node, mac_ip);
//                if (res.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                    link.set(3, res);
//                    links_new.add(link);
//                    logger.Println("normalizeLinks add link: "+link.get(0)+", "+link.get(1)+" <---> "+link.get(2)+", "+link.get(3)+" - "+link.get(4), logger.DEBUG);
//                } else {
//                    logger.Println("normalizeLinks remove link: "+link.get(0)+", "+link.get(1)+" <---> "+link.get(2)+", "+link.get(3)+" - "+link.get(4), logger.DEBUG);
//                }
//            } else {
//                links_new.add(link);
//                logger.Println("normalizeLinks add link: "+link.get(0)+", "+link.get(1)+" <---> "+link.get(2)+", "+link.get(3)+" - "+link.get(4), logger.DEBUG);
//            }
//        }
//        val.put("links", links_new);        

        new_list = new ArrayList();
        for (ArrayList<String> mas : links_extended) {
            ArrayList<String> mas1 = new ArrayList();
            for (String val : mas) {
//                val = val.replace("\n", "").replaceAll("\\s+", "_").replace("\"", "_").replace("\'", "_").replace("\\;", "_").replace("\\:", "_");
                val = val.replace("\n", "").replace("\"", "_").replace("'", "_").replace("\\;", "_").replace("\\:", "_");
                mas1.add(val);
            }
            new_list.add(mas1);
        }
        links_extended = new_list;

        result.add(links);
        result.add(links_extended);

        return result;
    }

    public Map<String, String[]> getMacNode_Iface(Map<String, Map> nodes_information) {
        // Get mac => [node, iface]
        Map<String, String[]> mac_node_iface = new HashMap();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            Map val = entry.getValue();
            Map<String, Map<String, String>> interfaces = (Map) val.get("interfaces");
            Map<String, String[]> mac_node_iface_part = getMacNode_Iface(node, interfaces);
            mac_node_iface.putAll(mac_node_iface_part);
        }
        return mac_node_iface;
    }

    public Map<String, String[]> getMacNode_Iface(String node, Map<String, Map<String, String>> ifaces_information) {
        // Get mac => [node, iface]
        Map<String, String[]> mac_node_iface = new HashMap();

        if (ifaces_information != null && !ifaces_information.isEmpty()) {
            for (Map.Entry<String, Map<String, String>> entry1 : ifaces_information.entrySet()) {
                String iface_name = entry1.getKey();
                String mac = entry1.getValue().get("mac");
                if (mac != null) {
                    mac = mac.toLowerCase().replaceAll("[.:-]", "");
                    String[] node_iface = new String[2];
                    node_iface[0] = node;
                    node_iface[1] = iface_name;
//                    if(mac.equals("0090e8869839"))
//                        System.out.println("111111");
                    if (mac.matches("[0-9a-f]{12}") && !mac.equals("000000000000")) {
                        mac_node_iface.put(mac, node_iface);
                    }
                }
            }
        }


        if (mac_node_iface.size() > 1) {
            long min = Long.MAX_VALUE;
            String[] val_min = new String[2];
            for (Map.Entry<String, String[]> entry : mac_node_iface.entrySet()) {
                String mac = entry.getKey();
                String[] val = entry.getValue();
                if (mac.matches("[0-9a-f]{12}")) {
                    long l = Long.parseLong(mac, 16);
                    if (l < min && l > 1) {
                        min = l;
                        val_min = val;
                    }
                }
            }

            String mac_baze_tmp = String.format("%12x", min - 1).trim();
            String mac_baze = "0".repeat(Math.max(0, 12 - mac_baze_tmp.length())) +
                    mac_baze_tmp;
            String[] val_baze = new String[2];
            val_baze[0] = val_min[0];
            val_baze[1] = "baze";
            mac_node_iface.put(mac_baze, val_baze);

            // append mac+1
            Map<String, String[]> mac_node_iface_new = new HashMap();
            for (Map.Entry<String, String[]> entry : mac_node_iface.entrySet()) {
                String mac = entry.getKey();
                String[] val = entry.getValue();
                if (mac.matches("[0-9a-f]{12}")) {
                    long l = Long.parseLong(mac, 16);
                    String mac_plus1_tmp = String.format("%12x", l + 1).trim();
                    StringBuilder mac_plus1 = new StringBuilder();
                    mac_plus1.append("0".repeat(Math.max(0, 12 - mac_plus1_tmp.length())));
                    mac_plus1.append(mac_plus1_tmp);
                    if (mac_node_iface.get(mac_plus1.toString()) == null) {
                        String[] mas = new String[2];
                        mas[0] = val[0];
                        mas[1] = "unknown";
                        mac_node_iface_new.put(mac_plus1.toString(), mas);
                    } else {
                        mac_node_iface_new.put(mac, val);
                    }
                }
            }
            mac_node_iface.putAll(mac_node_iface_new);
        }

        return mac_node_iface;
    }

//    private Map<String, String> getIpFromNodes(Map<String, Map> nodes_information) {
//        Map<String, String> result = new HashMap();
//        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
//            String node = entry.getKey();
//            result.put(node, node);
//            ArrayList<String> ip_list = getIpListFromNode((Map<String, Map>) entry.getValue());
//            for (String ip : ip_list) {
//                result.put(ip, node);
//            }
//        }
//
//        return result;
//    }

    private Map<String, String> getIpFromNodes(Map<String, Map> nodes_information,
                                               ArrayList<String> networks, ArrayList<String> not_correct_networks) {
        Map<String, String> result = new HashMap();

        Map<String, Integer> node_priority = new HashMap();
        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            int pos = 0;
            boolean found = false;
            for (String network : networks) {
                if (inside_Network(node, network)) {
                    found = true;
                    break;
                }
                pos = pos + 1;
            }
            if (found) {
                node_priority.put(node, pos);
            }
        }

        for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            result.put(node, node);
            ArrayList<String> ip_list = getIpListFromNode((Map<String, Map>) entry.getValue());
            for (String ip : ip_list) {
                if (result.get(ip) == null) {
                    if (!inside_Networks(ip, not_correct_networks)) {
                        result.put(ip, node);
                    }
                } else {
                    String node_exist = result.get(ip);
                    if ((node_priority.get(node) != null && node_priority.get(node_exist) != null && node_priority.get(node) < node_priority.get(node_exist)) ||
                            (node_priority.get(node) != null && node_priority.get(node_exist) == null)
                    ) {
                        if (!inside_Networks(ip, not_correct_networks)) {
                            result.remove(ip);
                            result.put(ip, node);
                            logger.Println("Replace ip_node: " + ip + ", " + node_exist + " to:" + ip + ", " + node, logger.DEBUG);
                        }
                    }
                }
            }
        }

        return result;
    }

    public ArrayList<String> getIpListFromNode(Map<String, Map> node_information) {
        ArrayList<String> result = new ArrayList();

        if (node_information != null && !node_information.isEmpty()) {
            Map<String, Map> map_interf = (node_information).get("interfaces");
            if (map_interf != null && !map_interf.isEmpty()) {
                for (Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
                    ArrayList<String> list = (ArrayList) entry1.getValue().get("ip");
                    String operation_status = (String) entry1.getValue().get("operation_status");
                    if (list != null && !list.isEmpty() && operation_status != null && operation_status.equals("up")) {
                        for (String ip : list) {
                            if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
                                ip = ip.split("/")[0];
                                result.add(ip);
                            } else if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                ip = ip.split(" ")[0];
                                result.add(ip);
                            } else if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                result.add(ip);
                            } else {
                                System.out.println("Bad format ip address - ");
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<String> getMACFromNode(Map<String, Map> node_information) {
        ArrayList<String> result = new ArrayList();

        Map<String, Map> map_interf = (node_information).get("interfaces");
        if (map_interf != null && !map_interf.isEmpty()) {
            for (Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
                String mac = (String) entry1.getValue().get("mac");
                if (mac != null && !mac.startsWith("00:00:00:00")
                        && !mac.startsWith("01:00:00:00")
                        && (mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
                        || mac.matches("^[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}$"))) {
                    if (!result.contains(mac)) {
                        result.add(mac);
                    }
                }
            }
        }

        return result;
    }

    public Map<String, String> getIpMACFromNode(Map<String, Map> node_information) {
        Map<String, String> result = new HashMap();

        Map<String, Map> map_interf = (node_information).get("interfaces");
        if (map_interf != null && !map_interf.isEmpty()) {
            for (Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
                String mac_find = null;
                String mac = (String) entry1.getValue().get("mac");
                if (mac != null && !mac.equals("00:00:00:00:00:00")
                        && !mac.equals("0000.0000.0000")
                        && (mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
                        || mac.matches("^[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}$"))) {
                    mac_find = mac;
                }

                ArrayList<String> ip_find_list = new ArrayList();
                ArrayList<String> list = (ArrayList) entry1.getValue().get("ip");
                String operation_status = (String) entry1.getValue().get("operation_status");
                if (list != null && !list.isEmpty() && operation_status != null && operation_status.equals("up")) {
                    for (String ip : list) {
                        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
                            ip = ip.split("/")[0];
                            ip_find_list.add(ip);
                        } else if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            ip = ip.split(" ")[0];
                            ip_find_list.add(ip);
                        } else if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            ip_find_list.add(ip);
                        }
                    }
                }
                if (mac_find != null && !ip_find_list.isEmpty()) {
                    for (String ip_find : ip_find_list) {
                        result.put(ip_find, mac_find);
                    }
                }
            }
        }

        return result;
    }

    public String nameToIpExt(String name, Map<String, String[]> mac_node) {
        Pattern p0 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Matcher m0 = p0.matcher(name);
        if (m0.find()) {
            String ip = m0.group(1);
            if (ip != null)
                return ip;
        }
        String result = name;

        String mac_extract = extractMACFromName1(name);

        if (!mac_extract.isEmpty()) {
            String mac_extract1 = mac_extract.replaceAll("[.:-]", "");
            String[] mas = mac_node.get(mac_extract1);
            if (mas != null && mas.length == 2) {
                result = mas[0];
//                System.out.println("Replace - "+mac_extract+" to - "+result);
            }
        }
        return result;
    }

    public String extractMACFromName(String name) {
        String mac_extract = name;
        name = name.toLowerCase();
        Pattern p = Pattern.compile(".*(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}).*");
        Matcher m = p.matcher(name);
        Pattern p1 = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
        Matcher m1 = p1.matcher(name);
        Pattern p2 = Pattern.compile(".*([0-9A-Fa-f]{6}[:-][0-9A-Fa-f]{6}).*");
        Matcher m2 = p2.matcher(name);

        if (m.find()) {
            mac_extract = m.group(1);
        } else if (m1.find()) {
            mac_extract = m1.group(1);
        } else if (m2.find()) {
            mac_extract = m2.group(1);
        }
        return mac_extract;
    }

    private String extractMACFromName1(String name) {
        String mac_extract = "";
        name = name.toLowerCase();
        Pattern p = Pattern.compile(".*(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}).*");
        Matcher m = p.matcher(name);
        Pattern p1 = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
        Matcher m1 = p1.matcher(name);
        Pattern p2 = Pattern.compile(".*([0-9A-Fa-f]{6}[:-][0-9A-Fa-f]{6}).*");
        Matcher m2 = p2.matcher(name);

        if (m.find()) {
            mac_extract = m.group(1);
        } else if (m1.find()) {
            mac_extract = m1.group(1);
        } else if (m2.find()) {
            mac_extract = m2.group(1);
        }
        return mac_extract;
    }

    private boolean checkMACFromName(String name) {
        name = name.toLowerCase();
        Pattern p = Pattern.compile(".*(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}).*");
        Matcher m = p.matcher(name);
        Pattern p1 = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
        Matcher m1 = p1.matcher(name);
        Pattern p2 = Pattern.compile(".*([0-9A-Fa-f]{6}[:-][0-9A-Fa-f]{6}).*");
        Matcher m2 = p2.matcher(name);

        if (m.find()) {
            return true;
        } else if (m1.find()) {
            return true;
        } else return m2.find();
    }

    public Map addingNodesFromLinks(Map informationFromNodes) {
        Map nodes_information = (Map) informationFromNodes.get("nodes_information");
        ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) informationFromNodes.get("links");
        ArrayList<ArrayList<String>> links_new = new ArrayList();
        Map<String, Map> map_res = new HashMap();
//        Map<String, Integer> node_hash = new HashMap();
        for (ArrayList<String> list : links) {
            try {
                String node1 = list.get(0);
                String iface1 = list.get(1);
                String node2 = list.get(2);
                String iface2 = list.get(3);
                String info = list.get(4);
//                int hash = info.hashCode();

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(info);
                Map<String, String> info_map = (Map) toMap(jsonObject);

                Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
                Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4}))");
                String mac = "";
                for (Map.Entry<String, String> entry : info_map.entrySet()) {
                    String val = entry.getValue();
                    Matcher m1 = p1.matcher(val);
                    if (m1.find()) {
                        mac = m1.group(1);
                        break;
                    } else {
                        Matcher m2 = p2.matcher(val);
                        if (m2.find()) {
                            mac = m2.group(1);
                            break;
                        }
                    }
                }

                Map<String, String> map1 = new HashMap();
                String sysname = info_map.get("remote_id");
                if (sysname.equals("0")) {
                    sysname = null;
                }
                if (sysname != null && !sysname.isEmpty()) {
                    map1.put("sysname", sysname);
                }
                String model = info_map.get("remote_version");
                if (model != null && !model.isEmpty()) {
                    if (model.equals("0")) {
                        model = null;
                    }
                    if (model != null) {
                        map1.put("model", model);
                    }
                }
                String platform = info_map.get("remote_platform");
                if (platform != null && !platform.isEmpty()) {
                    map1.put("platform", platform);
                }
                if (!mac.isEmpty()) {
                    map1.put("base_address", mac);
                }
                Map<String, Map> map2 = new HashMap();
                map2.put("general", map1);

                Map<String, Map> map4 = new HashMap();
                map4.put(iface2, new HashMap());
                map2.put("interfaces", map4);

                if (map_res.get(node2) != null && nodes_information.get(node2) == null) {
                    String base_address = (String) ((Map) map_res.get(node2).get("general")).get("base_address");
                    String sysname_prev = (String) ((Map) map_res.get(node2).get("general")).get("sysname");
                    if (base_address != null && !mac.isEmpty()
                            && !(base_address.replace(":", "").replace(".", "").toLowerCase()).equals(mac.replace(":", "").replace(".", "").toLowerCase())) {
                        node2 = node2 + "(" + mac + ")";
                    } else if ((sysname != null && sysname_prev == null)
                            || (sysname == null && sysname_prev != null)
                            || (sysname != null && sysname_prev != null && !sysname.equals(sysname_prev))) {
                        if (sysname != null) {
                            node2 = node2 + "(" + sysname + ")";
                        }
                    }
                }
                ArrayList<String> link = new ArrayList();
                link.add(node1);
                link.add(iface1);
                link.add(node2);
                link.add(iface2);
                link.add(info);
                links_new.add(link);

                int score = 0;
                if (sysname != null) {
                    score = 4;
                }
                if (model != null) {
                    score = score + 2;
                }

                int score1 = 0;
                if (map_res.get(node2) != null) {
                    if (map_res.get(node2).get("general") != null) {
                        String sysname1 = (String) ((Map) map_res.get(node2).get("general")).get("sysname");
                        if (sysname1 != null) {
                            score1 = 4;
                        }
                        String model1 = (String) ((Map) map_res.get(node2).get("general")).get("model");
                        if (model1 != null) {
                            score1 = score1 + 2;
                        }
                    }
                }

                int score2 = 0;
                if (nodes_information.get(node2) != null) {
                    Map general = (Map) ((Map) nodes_information.get(node2)).get("general");
                    if (general != null) {
                        String sysname1 = (String) ((Map) ((Map) (nodes_information.get(node2))).get("general")).get("sysname");
                        if (sysname1 != null) {
                            score2 = 4;
                        }
                        String model1 = (String) ((Map) ((Map) (nodes_information.get(node2))).get("general")).get("model");
                        if (model1 != null) {
                            score2 = score2 + 2;
                        }
                        String sysDescription = (String) ((Map) ((Map) (nodes_information.get(node2))).get("general")).get("sysDescription");
                        if (sysDescription != null) {
                            score2 = score2 + 3;
                        }
                        score2 = score2 + general.size();
                    }
                }

                if ((nodes_information.get(node2) == null && map_res.get(node2) == null)
                        || (nodes_information.get(node2) == null && map_res.get(node2) != null && score > score1)
                        || (nodes_information.get(node2) != null && map_res.get(node2) == null && score > score2)
                        || (nodes_information.get(node2) != null && map_res.get(node2) != null && score > score1 && score > score2)) {
                    map_res.put(node2, map2);
//                    node_hash.put(node2, info.hashCode());
                    logger.Println("Adding node " + node2 + " from links.", logger.DEBUG);
                }
            } catch (ParseException ex) {
                if (DEBUG) {
                    System.out.println(ex);
                }
                System.out.println(Utils.class.getName() + " - " + ex);
            }
        }
        nodes_information.putAll(map_res);
        informationFromNodes.put("links", links_new);
//            System.out.println("1111111");        

        return informationFromNodes;
    }

    public String[] findIfaceFromNode(String iface, Map<String, String> ifaceid_ifacename) {
        String[] result = new String[2];

        if (ifaceid_ifacename != null) {
            boolean find = false;
            for (Map.Entry<String, String> entry : ifaceid_ifacename.entrySet()) {
                String iface_id = entry.getKey();
                String iface_name = entry.getValue();
                if (iface.matches("\\d+")) {
                    if (iface.equals(iface_id)) {
                        result[0] = iface_id;
                        result[1] = iface_name;
                        find = true;
                        break;
                    }
                } else if (iface.equals(iface_name)) {
                    result[0] = iface_id;
                    result[1] = iface_name;
                    find = true;
                    break;
                }
            }
            if (!find) {
                for (Map.Entry<String, String> entry : ifaceid_ifacename.entrySet()) {
                    String iface_id = entry.getKey();
                    String iface_name = entry.getValue();
                    if (equalsIfaceName(iface_name, iface)) {
                        result[0] = iface_id;
                        result[1] = iface_name;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public String[] get_prefix_num_iface(String iface) {
        String num_iface = null;
        String prefix = null;
        Pattern p = Pattern.compile("(\\D*)(\\d+(\\W\\d+)*)");
        Matcher m = p.matcher(iface);
        ArrayList<String> num_iface_list = new ArrayList();
        ArrayList<String> prefix_list = new ArrayList();
        while (m.find()) {
            if (m.group(1) != null && !m.group(1).isEmpty())
                prefix_list.add(m.group(1));
            if (m.group(2) != null && !m.group(2).isEmpty())
                num_iface_list.add(m.group(2));
        }
        if (!num_iface_list.isEmpty()) {
            num_iface = String.join(":", num_iface_list);
        }
        if (!prefix_list.isEmpty()) {
            prefix = String.join(":", prefix_list);
        }
        String[] res = new String[2];
        if (prefix != null)
            res[0] = prefix.toLowerCase().trim();
        if (num_iface != null)
            res[1] = num_iface;

        return res;
    }

    public boolean equalsIfaceName(String iface1, String iface2) {
        if (iface1.equals(iface2))
            return true;
        String[] prefix_num_iface1 = get_prefix_num_iface(iface1);
        String[] prefix_num_iface2 = get_prefix_num_iface(iface2);
//        System.out.println(num_iface1 + "  -  " + num_iface2);

        if (prefix_num_iface1[1] != null && prefix_num_iface2[1] != null && prefix_num_iface1[1].equals(prefix_num_iface2[1]) &&
                prefix_num_iface1[0] != null && prefix_num_iface2[0] != null &&
                (prefix_num_iface1[0].startsWith(prefix_num_iface2[0]) || prefix_num_iface2[0].startsWith(prefix_num_iface1[0]))
        ) {
            return true;
        }
        return false;
    }

//    public boolean equalsIfaceName(String str1, String str2) {
//        if(str1 != null && str2 != null) {
//            String short_iface;
//            String full_iface;
//            str1 = str1.toLowerCase();
//            str2 = str2.toLowerCase();
//            String[] mas = str1.split("\\s");
//            StringBuilder num_iface1 = new StringBuilder();
//            if (mas.length > 2) {
//                for (String item : mas) {
//                    if (item.matches("\\d+")) {
//                        num_iface1.append(":").append(item);
//                    }
//                }
//            }
//            str1 = mas[mas.length - 1];
//            str1 = str1.replaceAll("\\W", "");
//
//            mas = str2.split("\\s");
//            StringBuilder num_iface2 = new StringBuilder();
//            if (mas.length > 2) {
//                for (String item : mas) {
//                    if (item.matches("\\d+")) {
//                        num_iface2.append(":").append(item);
//                    }
//                }
//            }
//            str2 = mas[mas.length - 1];
//            str2 = str2.replaceAll("\\W", "");
//
//            if ((!num_iface1.isEmpty()) && (num_iface2.isEmpty())) {
//                return false;
//            } else if ((num_iface1.isEmpty()) && (!num_iface2.isEmpty())) {
//                return false;
//            } else if ((!num_iface1.isEmpty()) && (!num_iface2.isEmpty())) {
//                return num_iface1.toString().equals(num_iface2.toString());
//            }
//            if (str1.length() > str2.length()) {
//                short_iface = str2;
//                full_iface = str1;
//            } else {
//                short_iface = str1;
//                full_iface = str2;
//            }
//
//            if (short_iface.equals(full_iface)) {
//                return true;
//            }
//
//            Pattern p = Pattern.compile("^(.*?)(\\d+?)$");
//            Matcher m = p.matcher(short_iface);
//            String[] short_iface_mas = new String[2];
//            if (m.matches()) {
//                short_iface_mas[0] = m.group(1);
//                short_iface_mas[1] = m.group(2);
//            } else {
//                return false;
//            }
//
//            Matcher m1 = p.matcher(full_iface);
//            String[] full_iface_mas = new String[2];
//            if (m1.matches()) {
//                full_iface_mas[0] = m1.group(1);
//                full_iface_mas[1] = m1.group(2);
//            } else {
//                return false;
//            }
//
//            return (!short_iface_mas[0].isEmpty() && !full_iface_mas[0].isEmpty())
//                    && full_iface_mas[0].startsWith(short_iface_mas[0])
//                    && short_iface_mas[1].equals(full_iface_mas[1]);
//        } else {
//            return str1 == null && str2 == null;
//        }
//    }

    //    private String[] FindIfaceFromNodeLevenstein(String iface, ArrayList<ArrayList<String>> ifaceid_ifacename_list) {
//        ArrayList<ArrayList> list = new ArrayList();
//        ArrayList id_iface_distance_list = new ArrayList();
//        for(ArrayList<String> ifaceid_ifacename : ifaceid_ifacename_list) {
//            ArrayList id_iface_distance = new ArrayList();
//            id_iface_distance.add(ifaceid_ifacename.get(0));
//            id_iface_distance.add(ifaceid_ifacename.get(1));
//            id_iface_distance.add(LevensteinEquals(iface, (String)id_iface_distance.get(1)));
//            id_iface_distance_list.add(id_iface_distance);
//        }
//        int max_distance=0;
//        String[] id_iface = new String[2];
//        for(ArrayList id_iface_distance : (ArrayList<ArrayList>)id_iface_distance_list) {
//            int distance = (int)id_iface_distance.get(2);
//            if(distance > max_distance) {
//                max_distance=distance;
//                id_iface[0]=(String)id_iface_distance.get(0);
//                id_iface[1]=(String)id_iface_distance.get(1);
//            }
//        }
//        System.out.println(iface+" - "+id_iface[1]);
//        int count=0;
//        for(ArrayList id_iface_distance : (ArrayList<ArrayList>)id_iface_distance_list) {
//            int distance = (int)id_iface_distance.get(2);
//            if(max_distance == distance) count=count+1;
//        }
//        if(count > 1) {
//            id_iface[0]=null;
//            id_iface[1]=null;
//        }
//        
//        return id_iface;
//    }
//    
//    private int LevensteinEquals(String S1, String S2) {
//            S1=S1.replace("/", "").replace("\\", "").replace("-", "").replace(".", "").replace("_", "").toLowerCase();
//            S2=S2.replace("/", "").replace("\\", "").replace("-", "").replace(".", "").replace("_", "").toLowerCase();
//            if(S1.length() > S2.length()) {
//                String str_tmp=S1;
//                S1=S2;
//                S2=str_tmp;
//            }
//            int m = S1.length(), n = S2.length();
//            int[] D1;
//            int[] D2 = new int[n + 1];
//
//            for(int i = 0; i <= n; i ++)
//                    D2[i] = i;
//
//            for(int i = 1; i <= m; i ++) {
//                    D1 = D2;
//                    D2 = new int[n + 1];
//                    for(int j = 0; j <= n; j ++) {
//                            if(j == 0) D2[j] = i;
//                            else {
//                                    int cost = (S1.charAt(i - 1) != S2.charAt(j - 1)) ? 1 : 0;
//                                    if(D2[j - 1] < D1[j] && D2[j - 1] < D1[j - 1] + cost)
//                                            D2[j] = D2[j - 1] + 1;
//                                    else if(D1[j] < D1[j - 1] + cost)
//                                            D2[j] = D1[j] + 1;
//                                    else
//                                            D2[j] = D1[j - 1] + cost;
//                            }
//                    }
//            }
//            int result=n-D2[n];
//            return result;
//    }      
    // remove duplicate nodes in all areas am mac addresses, ip list and sysname
    public Map<String, Map> removeDuplicateNodes(Map<String, Map> informationFromNodesAllAreas, Map<String, ArrayList<String>> area_networks) {
        int count = 0;
        while(true) {
            Map<String, Map> area_Node_NodePriority = getArea_Node_NodePriority(informationFromNodesAllAreas, Neb.area_networks);
//        --------------------------------------

            Map area_node_info_brief = getNodeInfoBrief(informationFromNodesAllAreas);
            Map<String, Map> area_mainnode_nodelist = new HashMap();
            for (Map.Entry<String, Map> item : ((Map<String, Map>) area_node_info_brief).entrySet()) {
                String area_name = item.getKey();
                Map<String, Map> nodes_links = item.getValue();
                Map<String, ArrayList<String>> mainnode_nodelist = new HashMap();
                ArrayList<ArrayList> nodes_info_list = new ArrayList();
                for (Map.Entry<String, Map> item1 : nodes_links.entrySet()) {
                    String node = item1.getKey();
                    Map val = item1.getValue();
                    ArrayList tmp = new ArrayList();
                    tmp.add(node);
                    tmp.add(val);
                    nodes_info_list.add(tmp);
                }

                Map<String, String> exclude = new HashMap();
                for (int i = 0; i < nodes_info_list.size(); i++) {
                    ArrayList<String> duplicate_nodes_list = new ArrayList();
                    String node1 = (String) nodes_info_list.get(i).get(0);
                    Map info1 = (Map) nodes_info_list.get(i).get(1);
                    if (exclude.get(node1) == null) {
                        for (int j = i + 1; j < nodes_info_list.size(); j++) {
                            String node2 = (String) nodes_info_list.get(j).get(0);
                            Map info2 = (Map) nodes_info_list.get(j).get(1);
                            if (exclude.get(node2) == null) {
                                if (isEqualsNodes(info1, info2)) {
                                    if (!duplicate_nodes_list.contains(node2)) {
                                        duplicate_nodes_list.add(node2);
                                    }
                                    exclude.put(node2, node2);
                                }
                            }
                        }
                    }

                    if (!duplicate_nodes_list.isEmpty() && informationFromNodesAllAreas.get(area_name) != null &&
                            informationFromNodesAllAreas.get(area_name).get("nodes_information") != null) {
                        duplicate_nodes_list.add(node1);
                        exclude.put(node1, node1);
                        Map<String, Map> nodes_info = (Map<String, Map>) informationFromNodesAllAreas.get(area_name).get("nodes_information");
                        String main_node = getPriorityNode(duplicate_nodes_list, nodes_info, area_Node_NodePriority.get(area_name));

                        ArrayList<String> duplicate_nodes_list_new = new ArrayList();
                        if (main_node != null) {
                            for (String item1 : duplicate_nodes_list) {
                                if (!item1.equals(main_node)) {
                                    duplicate_nodes_list_new.add(item1);
                                }
                            }
                        }
                        mainnode_nodelist.put(main_node, duplicate_nodes_list_new);
                    }
                }
                if (!mainnode_nodelist.isEmpty()) {
                    area_mainnode_nodelist.put(area_name, mainnode_nodelist);
                }
            }
            if (area_mainnode_nodelist.isEmpty()) {
                break;
            }

            for (Map.Entry<String, Map> item : area_mainnode_nodelist.entrySet()) {
                String area_name = item.getKey();
                Map<String, ArrayList<String>> mainnode_nodelist = item.getValue();
                if (informationFromNodesAllAreas.get(area_name) != null) {
                    Map area_info = informationFromNodesAllAreas.get(area_name);

                    ArrayList<String[]> replace_nodes = new ArrayList();
                    for (Map.Entry<String, ArrayList<String>> item1 : mainnode_nodelist.entrySet()) {
                        String main_node = item1.getKey();
                        ArrayList<String> node_list = item1.getValue();
                        for (String node : node_list) {
                            String[] tmp = new String[2];
                            tmp[0] = node;
                            tmp[1] = main_node;
                            replace_nodes.add(tmp);
                        }
                    }
                    applyInfo(area_info, replace_nodes);
                }
            }

            count++;
        }

        return informationFromNodesAllAreas;
    }

    public Map addingIfaceIdToLinks(Map informationFromNodesAllAreas, Map<String, Map<String, Map<String, String>>> area_node_ifaceid_ifacename) {
        // adding ifacaid to links
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map<String, Object> val = area.getValue();
            Map<String, Map<String, String>> node_ifacename_ifaceid = area_node_ifaceid_ifacename.get(area_name);
            if (node_ifacename_ifaceid != null) {
                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                ArrayList<ArrayList<String>> links_new = new ArrayList();
                if (links != null) {
                    for (ArrayList<String> link : links) {
                        Map<String, String> ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(0));
                        ArrayList<String> link_tmp = new ArrayList();
                        if (ifaceid_ifacename_list != null && !ifaceid_ifacename_list.isEmpty()) {
                            String[] if_name_iface1 = findIfaceFromNode(link.get(1), ifaceid_ifacename_list);
                            if (if_name_iface1[0] != null) {
                                link_tmp.add(link.get(0));
                                link_tmp.add(if_name_iface1[0]);
                                link_tmp.add(if_name_iface1[1]);
                            } else {
                                link_tmp.add(link.get(0));
                                link_tmp.add("");
                                link_tmp.add(link.get(1));
//                                System.out.println("ID iface not found: "+link.get(0)+", "+link.get(1));                                
                            }
                        } else {
                            link_tmp.add(link.get(0));
                            link_tmp.add("");
                            link_tmp.add(link.get(1));
                        }

                        ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(2));
                        if (ifaceid_ifacename_list != null && !ifaceid_ifacename_list.isEmpty()) {
                            String[] if_name_iface2 = findIfaceFromNode(link.get(3), ifaceid_ifacename_list);
                            if (if_name_iface2[0] != null) {
                                link_tmp.add(link.get(2));
                                link_tmp.add(if_name_iface2[0]);
                                link_tmp.add(if_name_iface2[1]);
                            } else {
                                link_tmp.add(link.get(2));
                                link_tmp.add("");
                                link_tmp.add(link.get(3));
//                                System.out.println("ID iface not found: "+link.get(2)+", "+link.get(3));
                            }
                        } else {
                            link_tmp.add(link.get(2));
                            link_tmp.add("");
                            link_tmp.add(link.get(3));
                        }
                        link_tmp.add(link.get(4));
                        links_new.add(link_tmp);
                    }
                }
                val.put("links", links_new);
            }
        }
        return informationFromNodesAllAreas;
    }

    public Map<String, Map<String, Map<String, String>>> getAreaNodeIdIface(Map informationFromNodesAllAreas, Map<String, ArrayList<String[]>> node_community_version) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap();
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            logger.Println("area - " + area_name, logger.DEBUG);
            ArrayList<String[]> node_community_version_area = node_community_version.get(area_name);
            if (node_community_version_area != null && !node_community_version_area.isEmpty()) {
                Map<String, Map<String, String>> node_ifacename_ifaceid = getNode_Ifaceid_Ifacename(node_community_version_area);
                result.put(area_name, node_ifacename_ifaceid);
            }
        }
//        ru.kos.neb.neb_lib.Utils.DEBUG = false;
        return result;
    }

    public Map<String, ArrayList<String[]>> getAreaNodeCommunityVersion(Map informationFromNodesAllAreas) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();

            Map nodes_information = (Map) val.get("nodes_information");
            Map<String, String[]> node_protocol_accounts = (Map) val.get("node_protocol_accounts");
            if (nodes_information != null && !nodes_information.isEmpty()
                    && node_protocol_accounts != null && !node_protocol_accounts.isEmpty()) {
                ArrayList<String[]> node_community_version = new ArrayList();
                for (Map.Entry<String, Map> map_tmp : ((Map<String, Map>) nodes_information).entrySet()) {
                    String node = map_tmp.getKey();
                    if (node_protocol_accounts.get(node) != null) {
                        String[] it = node_protocol_accounts.get(node);
                        if (it[0].equals("snmp")) {
                            String[] mas = new String[3];
                            mas[0] = node;
                            mas[1] = it[1];
                            mas[2] = it[2];
                            node_community_version.add(mas);
                        }
                    }
                }
                result.put(area_name, node_community_version);
            } else {
                logger.Println(area_name + " - is not node_protocol_accounts!!!", logger.DEBUG);
//                   System.out.println(area_name+" - is not node_protocol_accounts!!!");
            }
        }
        return result;
    }

    public Map<String, ArrayList<String[]>> getAreaNodeCommunityVersionDP(Map informationFromNodesAllAreas) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();

            Map<String, ArrayList<String>> nodes_dp_tmp = new HashMap();
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
            if (links != null && !links.isEmpty()) {
                JSONParser parser = new JSONParser();
                for (ArrayList<String> list : links) {
//                    String ext = "";
                    try {
                        String node1 = null;
                        String node2 = null;
                        String ext = null;
                        if (list.size() == 5) {
                            node1 = list.get(0);
                            node2 = list.get(2);
                            ext = list.get(4);
                        } else if (list.size() == 7) {
                            node1 = list.get(0);
                            node2 = list.get(3);
                            ext = list.get(6);
                        }
                        if (node1 != null && node2 != null && ext != null) {
                            JSONObject jsonObject = (JSONObject) parser.parse(ext);
                            Map map_tmp = toMap(jsonObject);
                            String type = (String) map_tmp.get("type");
                            if (type != null) {
                                switch (type) {
                                    case "cdp" -> {
                                        ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                        if (list1 != null) {
                                            list1.add("cdp");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("cdp");
                                        }
                                        nodes_dp_tmp.put(node1, list1);
                                        list1 = nodes_dp_tmp.get(node2);
                                        if (list1 != null) {
                                            list1.add("cdp");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("cdp");
                                        }
                                        nodes_dp_tmp.put(node2, list1);
                                    }
                                    case "lldp" -> {
                                        ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                        if (list1 != null) {
                                            list1.add("lldp");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("lldp");
                                        }
                                        nodes_dp_tmp.put(node1, list1);
                                        list1 = nodes_dp_tmp.get(node2);
                                        if (list1 != null) {
                                            list1.add("lldp");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("lldp");
                                        }
                                        nodes_dp_tmp.put(node2, list1);
                                    }
                                    default -> {
                                        ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                        if (list1 != null) {
                                            list1.add("");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("");
                                        }
                                        nodes_dp_tmp.put(node1, list1);
                                        list1 = nodes_dp_tmp.get(node2);
                                        if (list1 != null) {
                                            list1.add("");
                                        } else {
                                            list1 = new ArrayList();
                                            list1.add("");
                                        }
                                        nodes_dp_tmp.put(node2, list1);
                                    }
                                }
                            } else {
                                ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                if (list1 != null) {
                                    list1.add("");
                                } else {
                                    list1 = new ArrayList();
                                    list1.add("");
                                }
                                nodes_dp_tmp.put(node1, list1);
                                list1 = nodes_dp_tmp.get(node2);
                                if (list1 != null) {
                                    list1.add("");
                                } else {
                                    list1 = new ArrayList();
                                    list1.add("");
                                }
                                nodes_dp_tmp.put(node2, list1);
                            }
                        }
                        //                    System.out.println("1111111111111");
                    } catch (ParseException ex) {
//                        logger.Println("Error translate to JSON string : "+ext+" ...", logger.DEBUG);
//                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            Map<String, String> nodes_dp = new HashMap();
            for (Map.Entry<String, ArrayList<String>> entry : nodes_dp_tmp.entrySet()) {
                String node = entry.getKey();
                ArrayList<String> list = entry.getValue();
                boolean cdp = false;
                boolean lldp = false;
                for (String str : list) {
                    if (str.equals("cdp")) {
                        cdp = true;
                    } else if (str.equals("lldp")) {
                        lldp = true;
                    }
                }
                String dp = "";
                if (cdp && lldp) {
                    dp = "cdp,lldp";
                } else if (cdp && !lldp) {
                    dp = "cdp";
                } else if (!cdp && lldp) {
                    dp = "lldp";
                }
                nodes_dp.put(node, dp);
            }

            Map nodes_information = (Map) val.get("nodes_information");
            Map<String, String[]> node_protocol_accounts = (Map) val.get("node_protocol_accounts");
            if (nodes_information != null && !nodes_information.isEmpty()
                    && node_protocol_accounts != null && !node_protocol_accounts.isEmpty()) {
                ArrayList<String[]> node_community_version_dp = new ArrayList();
                for (Map.Entry<String, Map> map_tmp : ((Map<String, Map>) nodes_information).entrySet()) {
                    String node = map_tmp.getKey();
//                    if(node.equals("10.96.12.4"))
//                        System.out.println("11111");
                    if (node_protocol_accounts.get(node) != null) {
                        String[] node_protocol_accounts_item = node_protocol_accounts.get(node);
                        if (node_protocol_accounts_item[0].equals("snmp")) {
                            String[] mas = new String[4];
                            mas[0] = node;
                            mas[1] = node_protocol_accounts_item[1];
                            mas[2] = node_protocol_accounts_item[2];
                            if (nodes_dp.get(node) != null) {
                                mas[3] = nodes_dp.get(node);
                            } else {
                                mas[3] = "";
                            }
                            node_community_version_dp.add(mas);
                        }

                    }
                }
                result.put(area_name, node_community_version_dp);
            } else {
                logger.Println(area_name + " - is not node_protocol_accounts!!!", logger.DEBUG);
//                   System.out.println(area_name+" - is not node_protocol_accounts!!!");
            }
        }
        return result;
    }

    private Map<String, String[]> translateNodeProtocolAccountsToList(ArrayList<String[]> node_community_version) {
        // translate node_protocol_accounts Array to ArrayList
        Map<String, String[]> node_protocol_accounts_new = new HashMap();
        for (String[] item : node_community_version) {
            String node = item[0];
            String[] mas = new String[3];
            mas[0] = "snmp";
            mas[1] = item[1];
            mas[2] = item[2];
            node_protocol_accounts_new.put(node, mas);
        }
        return node_protocol_accounts_new;
    }

    //    public ArrayList<String[]> ReadARP_Mac_FromNodes(String filename) {
//        ArrayList<String[]> result = new ArrayList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(filename));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] buf = s.split(",");
//                    if(buf.length == 5) {
//                        result.add(buf);
//                    }
//                }
//                
//
//            } finally {
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        return result;
//    } 
//    public String RunRequest(String str) {
//        String result = "";
//        
//        Object result_map = new HashMap();
//        String[] mas = str.split("\\s+");
//        if(mas.length > 0) {
//            String command = mas[0];
//            ArrayList<String> params = new ArrayList();
//            for(int i=1; i<mas.length; i++) params.add(mas[i]);
//            if(command.equals("GET")) {
//                if(params.size() == 0) result_map=Neb.INFORMATION;
//                else {
//                    String param1=params.get(0);
//                    result_map=GetKey(param1);
//                }
//                System.out.println("11111111111111");
//            }
//        }
//        return result;
//    }
    public Object getKey(String key_str, Map MAP_INFO) {
        Object result = null;

        String[] keys = key_str.split("/");
        if (keys.length == 0) {
            result = MAP_INFO;
        } else {
            if (keys[0].isEmpty()) {
                Map value_map = MAP_INFO;
                Object value = null;
                String key = "";
                for (int i = 1; i < keys.length; i++) {
                    key = keys[i];
                    if (value_map.get(key) instanceof Map map) {
                        value_map = map;
                    } else if (value_map.get(key) == null) {
                        return null;
                    } else {
                        value = value_map.get(key);
                        break;
                    }
                }
                if (key.equals(keys[keys.length - 1])) {
                    if (value == null) {
                        result = value_map;
                    } else {
                        result = value;
                    }
                }
            }

        }
        return result;
    }

    public String getKeyList(String key_str, Map MAP_INFO) {
        StringBuilder out = new StringBuilder();
        String[] keys = key_str.split("/", -1);
        if (keys.length > 0) {
            if (keys[0].isEmpty()) {
                Map value_map = MAP_INFO;
                String key = "";
                for (int i = 1; i < keys.length; i++) {
                    key = keys[i];
                    if (value_map.get(key) instanceof Map map) {
                        value_map = map;
                    } else {
                        break;
                    }
                }
                if (key.equals(keys[keys.length - 1])) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) value_map).entrySet()) {
                        key = entry.getKey();
                        out.append("\n").append(key);
                    }
                }
            }

        }
        return out.toString().trim();
    }

    public boolean setKey(String key_str, Object val, Map MAP_INFO) {
        if (key_str.isEmpty() || key_str.equals("/")) {
            return false;
        }
        String[] mas = key_str.split("/");
        if (!mas[0].isEmpty()) {
            mas = ("/" + key_str).split("/");
        }
        Map path = MAP_INFO;
        for (int i = 1; i < mas.length - 1; i++) {
            path.computeIfAbsent(mas[i], k -> new HashMap());
            path = (Map) path.get(mas[i]);
        }
        path.put(mas[mas.length - 1], val);
//        if(path.get(mas[mas.length - 1]) != null && (path.get(mas[mas.length - 1]) instanceof List)) {
//            ((ArrayList)path.get(mas[mas.length - 1])).add(val);
//        } else {
//            path.put(mas[mas.length - 1], val);
//        }

        return true;
    }

    public boolean deleteKey(String key_str, Map MAP_INFO) {
        if (key_str.isEmpty() || key_str.equals("/")) {
            return false;
        }
        String end_symbol = key_str.substring(key_str.length() - 1);
        if (end_symbol.equals("/")) {
            key_str = key_str.substring(0, key_str.length() - 1);
        }
        String[] mas = key_str.split("/");
        if (!mas[0].isEmpty()) {
            mas = ("/" + key_str).split("/");
        }
        Map path = MAP_INFO;
        for (int i = 1; i < mas.length - 1; i++) {
            if (path.get(mas[i]) == null) {
                return false;
            }
            path = (Map) path.get(mas[i]);
        }
        path.remove(mas[mas.length - 1]);

        return true;
    }

    public ArrayList deleteFromList(Object del, ArrayList list) {
        ArrayList list_new = new ArrayList();
        if (del instanceof String) {
            list.remove(del);
        } else if (del instanceof List) {
            if (list.get(0) instanceof List) {
                for (ArrayList<String> item : (ArrayList<ArrayList>) list) {
//                    if(item.size() == ((List) del).size()) {
//                        boolean is_equiv = true;
//                        for(int i=0; i<item.size(); i++) {
//                            String it1 = item.get(i);
//                            String it2 = (String)((List)del).get(i);
//                            if(!it1.equals(it2)) {
//                                is_equiv = false;
//                                break;
//                            }
//                        } if(!is_equiv) {
//                            list_new.add(item);
//                        }
//                    } else {
//                        list_new.add(item);
//                    }
                    if (!item.equals(del)) {
                        list_new.add(item);
                    }
                }
            }
        } else if (del instanceof String[]) {
            if (list.get(0) instanceof String[]) {
                for (String[] item : (ArrayList<String[]>) list) {
                    if (!Arrays.equals(item, (String[]) del)) {
                        list_new.add(item);
                    }
                }
            }
        }
        if (list_new.isEmpty())
            return list;
        else
            return list_new;
    }

    private Object strToObject(String val) {
        JSONParser parser = new JSONParser();
        try {
            Map map;
            List list;
            Object json = parser.parse(val);
            switch (json) {
                case JSONObject jSONObject -> {
                    map = toMap(jSONObject);
                    return map;
                }
                case JSONArray jSONArray -> {
                    list = toList(jSONArray);
                    return list;
                }
                default -> {
                }
            }
        } catch (ParseException excep) {
            return val;
        }
        return null;
    }

    public boolean addToList(String key_str, String val, Map MAP_INFO) {
        Object obj = strToObject(val);
        Object value = getValueKey(key_str, MAP_INFO);
        boolean found = false;
        if (value instanceof List) {
            for (Object item : (ArrayList) value) {
                if ((item instanceof List) && (obj instanceof List)) {
                    boolean found1 = true;
                    for (int i = 0; i < ((ArrayList) item).size(); i++) {
                        Object it = ((ArrayList) item).get(i);
                        Object it_shablon = ((ArrayList) obj).get(i);
                        if (it instanceof String && it_shablon instanceof String) {
                            if (!it.equals(it_shablon)) {
                                found1 = false;
                                break;
                            }
                        } else if (it instanceof Integer && it_shablon instanceof Integer) {
                            if (it != it_shablon) {
                                found1 = false;
                                break;
                            }
                        }
                    }
                    if (found1) {
                        found = true;
                        break;
                    }
                } else if (item instanceof String && obj instanceof String) {
                    if (item.equals(obj)) {
                        found = true;
                        break;
                    }
                } else if (item instanceof Integer && obj instanceof Integer) {
                    if (item == obj) {
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            if (value != null) {
                ((ArrayList) value).add(obj);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    public boolean delFromList(String key_str, String val, Map MAP_INFO) {
        boolean result;

        Object value = getValueKey(key_str, MAP_INFO);
        if (value instanceof List) {
            ArrayList<Integer> del_row = new ArrayList();
            for (int i = ((ArrayList) value).size() - 1; i >= 0; i--) {
                Object iter = ((ArrayList) value).get(i);
                if (iter instanceof List) {
                    String[] fields = val.split(";");
                    boolean found = true;
                    int num_field = 0;
                    for (String feild : fields) {
                        if (!feild.isEmpty() && !feild.equals(((ArrayList) iter).get(num_field))) {
                            found = false;
                            break;
                        }
                        num_field++;
                    }
                    if (found) {
                        del_row.add(i);
                    }
                } else if (iter instanceof String string) {
                    if (string.equals(val)) {
                        del_row.add(i);
                    }
                }

            }

            if (!del_row.isEmpty()) {
                for (int row : del_row) {
                    ((ArrayList) value).remove(row);
                }
                result = true;
            } else {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    //    private String[] GetParentKeyAndKey(String key_str, Map MAP_INFO) {
//        String[] result = new String[2];
//        
//        String parent = "";
//        String s = key_str.substring(key_str.length()-1);
//        if(s.equals("/"))
//            key_str = key_str.substring(0, key_str.length()-1);
//        String[] mas = key_str.split("/");
//        
//        if(mas[0].equals("") && mas.length > 1) {
//            for(int i = 1; i<mas.length-1; i++) {
//                String iter = mas[i];
//                parent = parent +"/"+iter;
//            }
//
//
//            if(parent.equals("")) parent = "/";
//
//            String key = mas[mas.length-1];
//
//            result[0] = parent;
//            result[1] = key;
//        } else 
//            result = null;
//        
//        return result;
//    }
    private Object getValueKey(String key_str, Map MAP_INFO) {
        Object result = null;

        String[] keys = key_str.split("/", -1);
        if (keys.length > 1) {
            if (keys[1].isEmpty()) {
                result = MAP_INFO;
            } else if (keys.length > 0) {
                if (keys[0].isEmpty()) {
                    Object value = MAP_INFO;
                    String key;
                    //                int depth = 0;
                    for (int i = 1; i < keys.length; i++) {
                        key = keys[i];
                        if (((Map) value).get(key) != null) {
                            if (((Map) value).get(key) instanceof Map) {
                                value = ((Map) value).get(key);
                                if (i == keys.length - 1) {
                                    result = value;
                                    break;
                                }
                            } else {
                                result = ((Map) value).get(key);
                                break;
                            }
                        } else {
                            result = null;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

//    private ArrayList<String[]> RunScriptCliTest(ArrayList<String> ip_list, Map<String, String> exclude_list, ArrayList<ArrayList<String>> accounts_list, int timeout, int retries, Map<String, String[]> cli_accounts_priority) {
//        ArrayList<String[]> cli_node_account = new ArrayList();
//
//        if (((Map) Neb.cfg.get("scripts")).get("cli-test") != null
//                && ((ArrayList) ((Map) Neb.cfg.get("scripts")).get("cli-test")).get(0) != null) {
//            Map<String, ArrayList<String[]>> cli_accounts_list_priority = new HashMap();
//            if (cli_accounts_priority != null) {
//                for (Map.Entry<String, String[]> entry : ((Map<String, String[]>) cli_accounts_priority).entrySet()) {
//                    String node = entry.getKey();
//                    if (ip_list.contains(node)) {
//                        ArrayList tmp_list = new ArrayList();
//                        tmp_list.add(entry.getValue());
//                        cli_accounts_list_priority.put(node, tmp_list);
//                    }
//                }
//            }
//
//            Map<String, ArrayList<String[]>> node_protocol_accounts = new HashMap();
//            for (String ip : ip_list) {
//                if (exclude_list == null || exclude_list.get(ip) == null) {
//                    ArrayList<String[]> list = new ArrayList();
//                    if (node_protocol_accounts.get(ip) != null) {
//                        list = node_protocol_accounts.get(ip);
//                    }
//
//                    for (ArrayList<String> acount : accounts_list) {
//                        if (acount.size() == 2) {
//                            String[] mas1 = new String[4];
//                            mas1[0] = "ssh";
//                            mas1[1] = acount.get(0);
//                            mas1[2] = acount.get(1);
//                            mas1[3] = "";
//                            list.add(mas1);
//                            mas1 = new String[4];
//                            mas1[0] = "telnet";
//                            mas1[1] = acount.get(0);
//                            mas1[2] = acount.get(1);
//                            mas1[3] = "";
//                            list.add(mas1);
//                        } else if (acount.size() == 3) {
//                            String[] mas1 = new String[4];
//                            mas1[0] = "ssh";
//                            mas1[1] = acount.get(0);
//                            mas1[2] = acount.get(1);
//                            mas1[3] = acount.get(2);
//                            list.add(mas1);
//                            mas1 = new String[4];
//                            mas1[0] = "telnet";
//                            mas1[1] = acount.get(0);
//                            mas1[2] = acount.get(1);
//                            mas1[3] = acount.get(2);
//                            list.add(mas1);
//                        }
//                    }
//                    node_protocol_accounts.put(ip, list);
//                }
//            }
//
//            Map<String, ArrayList<String>> scripts = new HashMap();
//            ArrayList<String> list_tmp = new ArrayList();
//            list_tmp.add((String) ((ArrayList) ((Map) Neb.cfg.get("scripts")).get("cli-test")).get(0));
//            scripts.put("ssh", list_tmp);
//            list_tmp = new ArrayList();
//            list_tmp.add((String) ((ArrayList) ((Map) Neb.cfg.get("scripts")).get("cli-test")).get(0));
//            scripts.put("telnet", list_tmp);
//
//            if (cli_accounts_priority != null) {
//                Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("RunScriptCliTest");
//                watch_Telemetry_Lib.start();
//                ArrayList<String> out1 = Neb.runScriptsPool.Get(cli_accounts_list_priority, scripts, timeout, retries);
//                watch_Telemetry_Lib.exit = true;
//                for (String str : out1) {
//                    try {
//                        JSONParser parser = new JSONParser();
////                        logger.Println(str, logger.DEBUG);
//                        JSONObject jsonObject = (JSONObject) parser.parse(str);
//                        Map<String, Object> map = toMap(jsonObject);
//                        if (map.containsKey("node") && map.containsKey("protocol")
//                                && map.containsKey("user") && map.containsKey("passwd")
//                                && map.containsKey("enable_passwd")) {
//                            String[] mas1 = new String[5];
//                            mas1[0] = (String) map.get("node");
//                            mas1[1] = (String) map.get("protocol");
//                            mas1[2] = (String) map.get("user");
//                            mas1[3] = Neb.neb_lib_utils.encrypt(ru.kos.neb.neb_lib.Utils.master_key, (String) map.get("passwd"));
//                            if (mas1[3] == null) {
//                                mas1[3] = (String) map.get("passwd");
//                            }
//                            mas1[4] = (String) map.get("enable_passwd");
//                            cli_node_account.add(mas1);
//                            logger.Println("node: " + mas1[0] + " protocol: " + mas1[1] + " user: " + mas1[2] + " passwd: " + mas1[3] + " enable_passwd: " + mas1[4], logger.DEBUG);
//                        }
//                    } catch (ParseException ex) {
//                        if (DEBUG) {
//                            System.out.println(ex);
//                        }
//                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                ////////////////////////////////////////////////////////
//                Map<String, ArrayList<String[]>> node_protocol_accounts_new = new HashMap();
//                for (Map.Entry<String, ArrayList<String[]>> entry : ((Map<String, ArrayList<String[]>>) node_protocol_accounts).entrySet()) {
//                    String node = entry.getKey();
//                    ArrayList<String[]> val = entry.getValue();
//                    boolean find = false;
//                    for (String[] item : cli_node_account) {
//                        if (item[0].equals(node)) {
//                            find = true;
//                            break;
//                        }
//                    }
//                    if (!find) {
//                        node_protocol_accounts_new.put(node, val);
//                    }
//                }
//                node_protocol_accounts = node_protocol_accounts_new;
//            }
//            /////////////////////////////////////////////////////////
//            if (!node_protocol_accounts.isEmpty()) {
//                Watch_Telemetry_Lib watch_Telemetry_Lib = new Watch_Telemetry_Lib("RunScriptCliTest");
//                watch_Telemetry_Lib.start();
//                ArrayList<String> out = Neb.runScriptsPool.Get(node_protocol_accounts, scripts, timeout, retries);
//                watch_Telemetry_Lib.exit = true;
//                for (String str : out) {
//                    try {
//                        JSONParser parser = new JSONParser();
////                        logger.Println(str, logger.DEBUG);
//                        JSONObject jsonObject = (JSONObject) parser.parse(str);
//                        Map<String, Object> map = toMap(jsonObject);
//                        if (map.containsKey("node") && map.containsKey("protocol")
//                                && map.containsKey("user") && map.containsKey("passwd")
//                                && map.containsKey("enable_passwd")) {
//                            String[] mas1 = new String[5];
//                            mas1[0] = (String) map.get("node");
//                            mas1[1] = (String) map.get("protocol");
//                            mas1[2] = (String) map.get("user");
//                            mas1[3] = Neb.neb_lib_utils.encrypt(ru.kos.neb.neb_lib.Utils.master_key, (String) map.get("passwd"));
//                            if (mas1[3] == null) {
//                                mas1[3] = (String) map.get("passwd");
//                            }
//                            mas1[4] = (String) map.get("enable_passwd");
//                            cli_node_account.add(mas1);
//                            logger.Println("node: " + mas1[0] + " protocol: " + mas1[1] + " user: " + mas1[2] + " passwd: " + mas1[3] + " enable_passwd: " + mas1[4], logger.DEBUG);
//                        }
//                    } catch (ParseException ex) {
//                        if (DEBUG) {
//                            System.out.println(ex);
//                        }
//                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }
//        return cli_node_account;
//    }

    public ArrayList<String> readFileToList(String filename) {
        ArrayList<String> result_list = new ArrayList();
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
                String s;
                while ((s = in.readLine()) != null) {
                    result_list.add(s);
//                    System.out.println(s);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result_list;
    }

    public String readFileToString(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
                sb.append(in.readLine());
                String s;
                while ((s = in.readLine()) != null) {
                    sb.append("\n").append(s);
                }
            }
        } catch (IOException e) {
            if (DEBUG) {
                System.out.println(e);
            }
//            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public String readFileToStringWithoutComments(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
                String str = in.readLine();
                str = str.replaceAll("#.*", "");
                sb.append(str);
                String s;
                while ((s = in.readLine()) != null) {
                    s = s.replaceAll("#.*", "");
                    sb.append("\n").append(s);
                }
            }
        } catch (IOException e) {
            if (DEBUG) {
                System.out.println(e);
            }
//            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public static String[] extended_spliter(String str, String delimiter) {
        str = str.replace("\\" + delimiter, "<delimiter>");
        String[] mas = str.split(";");
        int i = 0;
        for (String it : mas) {
            mas[i] = it.replace("<delimiter>", delimiter);
            i++;
        }
        return mas;
    }

    public Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
        }
        return result;
    }

    public Object[] stringToArray(String str) {
        str = str.replace("\\,", "<_QWERTY_>");
        Pattern p = Pattern.compile("^\\s*\\[(.*)]\\s*$");
        Matcher m = p.matcher(str);
        Object[] res = null;
        if (m.find()) {
            String array = m.group(1);
            String[] mas = array.split(",");
            res = new Object[mas.length];
            int i = 0;
            for (String it : mas) {
                it = it.replace("<_QWERTY_>", ",");
                Pattern p1 = Pattern.compile("^\\s*(.*?)\\s*$");
                Matcher m1 = p1.matcher(it);
                String val_str = "";
                if (m1.find()) {
                    val_str = m1.group(1);
                }
                Pattern p2 = Pattern.compile("^\"(.*)\"$");
                Matcher m2 = p2.matcher(val_str);
                if (m2.find()) {
                    res[i] = m2.group(1);
                } else {
                    if (val_str.matches("\\d+\\.\\d*")) {
                        res[i] = Float.valueOf(val_str);
                    } else if (val_str.matches("\\d+")) {
                        res[i] = Long.valueOf(val_str);
                    }
                }
                i++;
            }
        }
        Object[] result = null;
        if (res != null) {
            boolean ok = true;
            for (Object it : res) {
                if (it == null) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                result = res;
            }
        }
        return result;
    }

    //    public Object StringToValue(String str) {
//        Object result = null;
//        Pattern p1 = Pattern.compile("^\\s*(.*?)\\s*$");
//        Matcher m1 = p1.matcher(str);
//        String val_str = "";
//        if(m1.find()) 
//            val_str=m1.group(1);
//        Pattern p2 = Pattern.compile("^\\\"(.*)\\\"$");
//        Matcher m2 = p2.matcher(val_str);
//        if(m2.find()) {
//            result=m2.group(1);
//        } else {
//            if(val_str.matches("\\d+\\.\\d*")) result = Float.valueOf(val_str);
//            else if(val_str.matches("\\d+")) result = Long.valueOf(val_str);
//        }
//
//        return result;
//    }
    public boolean setValueToInfo(String key, String str, Map MAP_INFO) {
        boolean result = false;

        str = URLDecoder.decode(str, StandardCharsets.UTF_8);
        Object obj = strToObject(str);
        if (obj == null) {
            if (setKey(key, str, MAP_INFO)) {
                result = true;
            }
        } else {
            if (setKey(key, obj, MAP_INFO)) {
                result = true;
            }
        }
        return result;
    }

    public Map getNodesAttributes(Map INFO) {
        Map<String, Map> result = new HashMap();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) INFO).entrySet()) {
            String area = entry.getKey();
            Map val = (Map) entry.getValue().get("nodes_information");
            Map tmp_node_map = new HashMap();
            if (val != null) {
                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
                    String node = entry1.getKey();
                    String image = (String) entry1.getValue().get("image");
                    String image_auto = (String) entry1.getValue().get("image_auto");
                    ArrayList<String> xy = (ArrayList<String>) entry1.getValue().get("xy");
                    ArrayList<String> size = (ArrayList<String>) entry1.getValue().get("size");
                    String base_address = null;
                    if (entry1.getValue().get("general") != null) {
                        base_address = (String) ((Map) entry1.getValue().get("general")).get("base_address");
                    }
                    Map tmp_map = new HashMap();
                    if (image != null) {
                        tmp_map.put("image", image);
                    }
                    if (image_auto != null) {
                        tmp_map.put("image_auto", image_auto);
                    }
                    if (xy != null && xy.size() == 2) {
                        tmp_map.put("xy", xy);
                    }
                    if (size != null && size.size() == 2) {
                        tmp_map.put("size", size);
                    }
                    if (base_address != null) {
                        tmp_map.put("base_address", base_address);
                    }
                    tmp_node_map.put(node, tmp_map);
                }
            }
            result.put(area, tmp_node_map);
        }
        return result;
    }

    public Map getNewNodes(Map INFO, Map INFO_OLD) {
        Map<String, Map<String, String>> result = new HashMap();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) INFO).entrySet()) {
            String area = entry.getKey();
            Map val = (Map) entry.getValue().get("nodes_information");
            Map<String, String> res = new HashMap();
            if (val != null) {
                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
                    String node = entry1.getKey();
                    if (!(INFO_OLD.get(area) != null
                            && ((Map) INFO_OLD.get(area)).get("nodes_information") != null
                            && ((Map) ((Map) INFO_OLD.get(area)).get("nodes_information")).get(node) != null)) {
                        res.put(node, node);
                    }
                }
            }
            result.put(area, res);
        }
        return result;
    }

    public Map setNodesAttributes(Map<String, Map> node_attribute, Map INFO) {
        Map<String, Map> mac_attribute = new HashMap();
        for (Map.Entry<String, Map> entry : node_attribute.entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
//                String node = entry1.getKey();
                Map val1 = entry1.getValue();
                String mac = (String) val1.get("base_address");
                if (mac != null) {
                    if (mac_attribute.get(area) != null) {
                        mac_attribute.get(area).put(mac, val1);
                    } else {
                        Map<String, Map> map_tmp = new HashMap();
                        map_tmp.put(mac, val1);
                        mac_attribute.put(area, map_tmp);
                    }
                }
            }
        }

        for (Map.Entry<String, Map> entry : ((Map<String, Map>) INFO).entrySet()) {
            String area = entry.getKey();
            Map val = (Map) entry.getValue().get("nodes_information");
            if (val != null) {
                try {
                    for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
                        String node = entry1.getKey();
                        Map val1 = entry1.getValue();

                        if (val1 != null) {
                            boolean complete = false;
                            String mac;
                            if (val1.get("general") != null && ((Map) val1.get("general")).get("base_address") != null) {
                                mac = (String) ((Map) val1.get("general")).get("base_address");
                                Map value = (Map) getKey("/" + area + "/" + mac, mac_attribute);
                                if (value != null) {
                                    String image = (String) value.get("image");
                                    String image_auto = (String) value.get("image_auto");
                                    ArrayList<String> xy = (ArrayList<String>) value.get("xy");
                                    ArrayList<String> size = (ArrayList<String>) value.get("size");
                                    if (image != null) {
                                        val1.put("image", image);
                                    }
                                    if (image_auto != null) {
                                        val1.put("image_auto", image_auto);
                                    }
                                    if (xy != null && xy.size() == 2) {
                                        val1.put("xy", xy);
                                    }
                                    if (size != null && size.size() == 2) {
                                        val1.put("size", size);
                                    }
                                    if (val1.get("general") != null) {
                                        ((Map) val1.get("general")).put("base_address", mac);
                                    }
                                    complete = true;
                                }
                            }
                            if (!complete) {
                                ArrayList<String> list_ip = getIpListFromNode(val1);
                                list_ip.add(node);
                                if (!list_ip.isEmpty()) {
                                    for (String ip : list_ip) {
                                        if (!ip.startsWith("127.")) {
                                            Map value = (Map) getKey("/" + area + "/" + ip, node_attribute);
                                            if (value != null) {
                                                String image = (String) value.get("image");
                                                String image_auto = (String) value.get("image_auto");
                                                ArrayList<String> xy = (ArrayList<String>) value.get("xy");
                                                ArrayList<String> size = (ArrayList<String>) value.get("size");
                                                if (val1.get("image") == null && image != null) {
                                                    val1.put("image", image);
                                                }
                                                if (val1.get("image_auto") == null && image_auto != null) {
                                                    val1.put("image_auto", image_auto);
                                                }
                                                if (val1.get("xy") == null && xy != null && xy.size() == 2) {
                                                    val1.put("xy", xy);
                                                }
                                                if (val1.get("size") == null && size != null && size.size() == 2) {
                                                    val1.put("size", size);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    Map value = (Map) getKey("/" + area + "/" + node, node_attribute);
                                    if (value != null) {
                                        String image = (String) value.get("image");
                                        String image_auto = (String) value.get("image_auto");
                                        ArrayList<String> xy = (ArrayList<String>) value.get("xy");
                                        ArrayList<String> size = (ArrayList<String>) value.get("size");
                                        if (image != null) {
                                            val1.put("image", image);
                                        }
                                        if (image_auto != null) {
                                            val1.put("image_auto", image_auto);
                                        }
                                        if (xy != null && xy.size() == 2) {
                                            val1.put("xy", xy);
                                        }
                                        if (size != null && size.size() == 2) {
                                            val1.put("size", size);
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.Println("val1 is null node=" + node + " !!!", logger.DEBUG);
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }
        return INFO;
    }

//    public Map<String, Map> saveNodesAttributesOld(Map<String, Map> node_attribute, Map INFO) {
//        Map<String, Map> result = new HashMap();
//
//        for (Map.Entry<String, Map> entry : node_attribute.entrySet()) {
//            String area = entry.getKey();
//            Map val = entry.getValue();
//
//            Map<String, String> ip_node = new HashMap();
//            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("nodes_information") != null) {
//                ip_node = getIpFromNodes((Map) ((Map) INFO.get(area)).get("nodes_information"));
//            }
//
//            if (val != null) {
//                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
//                    String node = entry1.getKey();
//                    Map val1 = entry1.getValue();
//                    if (val1 != null && !node.startsWith("127.")) {
////                        Map value = (Map)GetKey("/"+area+"/nodes_information/"+node, INFO);
//                        if (ip_node.get(node) == null) {
//                            if (result.get(area) != null) {
//                                result.get(area).put(node, val1);
//                            } else {
//                                Map<String, Map> map_tmp = new HashMap();
//                                map_tmp.put(node, val1);
//                                result.put(area, map_tmp);
//                            }
//                        }
//                    }
//
//                }
//            }
//
//        }
//
//        return result;
//    }

    private String mac_Char_To_HEX(String str) {
        String result = str;

        if (str.length() == 6 && !str.matches("^[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}$")) {
            result = toHexString(str.getBytes());
        }
        return result;
    }

    private String toHexString(byte[] ba) {
        StringBuilder str = new StringBuilder();
        for (byte b : ba) {
            str.append(String.format("%x", b)).append(":");
        }
        String out = str.toString();
        out = out.substring(0, out.length() - 1);
        return out;
    }

    public Map<String, Map<String, String[]>> get_SNMP_accounts(Map neb_map_info) {
        Map<String, Map<String, String[]>> result = new HashMap();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) neb_map_info).entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, ArrayList> node_protocol_accounts = val.get("node_protocol_accounts");
            Map<String, String[]> map_node_tmp = new HashMap();
            if (node_protocol_accounts != null) {
                for (Map.Entry<String, ArrayList> entry1 : node_protocol_accounts.entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<ArrayList<String>> val1 = entry1.getValue();
                    ArrayList<String> item = val1.get(0);
                    if (item.get(0).equals("snmp")) {
                        String[] mas = new String[2];
                        mas[0] = item.get(1);
                        mas[1] = item.get(2);
                        map_node_tmp.put(node, mas);
                    }
                }
            }
            if (!map_node_tmp.isEmpty()) {
                result.put(area, map_node_tmp);
            }
        }
        return result;
    }

    public Map<String, Map<String, String[]>> get_CLI_accounts(Map neb_map_info) {
        Map<String, Map<String, String[]>> result = new HashMap();
        final Random random = new Random();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) neb_map_info).entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, ArrayList> node_protocol_accounts = val.get("node_protocol_accounts");
            Map<String, String[]> map_node_tmp = new HashMap();
            if (node_protocol_accounts != null) {
                for (Map.Entry<String, ArrayList> entry1 : node_protocol_accounts.entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<ArrayList<String>> val1 = entry1.getValue();
                    for (ArrayList<String> item : val1) {
                        if (item.get(0).equals("ssh") || item.get(0).equals("telnet")) {
                            String[] mas = new String[4];
                            mas[0] = item.get(0);
                            mas[1] = item.get(1);
                            mas[2] = item.get(2);
                            mas[3] = item.get(3);
                            if (random.nextInt(10) > 3) {
                                map_node_tmp.put(node, mas);
                            }
                            break;
                        }
                    }
                }
            }
            if (!map_node_tmp.isEmpty()) {
                result.put(area, map_node_tmp);
            }
        }
        return result;
    }

    public String getFileCreateTime(String file) {
        String result = null;
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        Matcher m = p.matcher(file);
        if (m.find()) {
            result = m.group(1).replace("-", " ");
        } else {
            try {
                File f = new File(file);
                if (f.exists()) {
                    BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                    ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
                    String day;
                    if (ct.getDayOfMonth() <= 9) {
                        day = "0" + ct.getDayOfMonth();
                    } else {
                        day = String.valueOf(ct.getDayOfMonth());
                    }
                    String month;
                    if (ct.getMonthValue() <= 9) {
                        month = "0" + ct.getMonthValue();
                    } else {
                        month = String.valueOf(ct.getMonthValue());
                    }
                    String year = String.valueOf(ct.getYear());
                    String hour;
                    if (ct.getHour() <= 9) {
                        hour = "0" + ct.getHour();
                    } else {
                        hour = String.valueOf(ct.getHour());
                    }
                    String min;
                    if (ct.getMinute() <= 9) {
                        min = "0" + ct.getMinute();
                    } else {
                        min = String.valueOf(ct.getMinute());
                    }
                    result = day + "." + month + "." + year + " " + hour + "." + min;
                }
            } catch (IOException ex) {
            }
        }
        return result;
    }

    public long getFileCreateTime_mSec(String file) {
        long result = 0;
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        Matcher m = p.matcher(file);
        if (m.find()) {
            String date_str = m.group(1).replace("-", " ");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm");
            try {
                result = sdf.parse(date_str).getTime();
            } catch (java.text.ParseException ex) {
            }
        } else {
            try {
                BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
                ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
                result = ct.toEpochSecond() * 1000;
            } catch (IOException ex) {
            }
        }
        return result;
    }

    @SuppressWarnings("SleepWhileInLoop")
    public boolean setFileCreationDateNow(String filePath, long delay) {
        BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis((new Date()).getTime());
        Date d = new Date();
        long start_time = d.getTime();
        boolean is_ok = false;
        while (true) {
            try {
                attributes.setTimes(time, time, time);
                is_ok = true;
                break;
            } catch (IOException ex) {
                d = new Date();
                if (d.getTime() - start_time > delay * 1000) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (java.lang.InterruptedException e) {
                }
            }
        }
        return is_ok;
    }

    public ArrayList findKey(String key) {
        ArrayList<ArrayList<String>> result = new ArrayList();

        if (Neb.INDEX.get(key) != null) {
            result = Neb.INDEX.get(key);

            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm");
            result.sort((var s1, var s2) -> {
                long time1 = 0;
                long time2 = 0;
                try {
                    time1 = sdf.parse(s1.get(0)).getTime();
                    time2 = sdf.parse(s2.get(0)).getTime();
                } catch (java.text.ParseException ex) {
                }
                if (time1 < time2) {
                    return 1;
                } else if (time1 >= time2) {
                    return -1;
                }
                return 0;
            });

        }

        return result;
    }

    public Map setInformations(Map scan_info, Map info) {
        Map result = new HashMap();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) scan_info).entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            if (info.get(area) != null) {
                Map val1 = (Map) val.get("nodes_information_extended");
                if (val1 != null) {
                    ((Map) info.get(area)).put("nodes_information_extended", val1);
                }
                ArrayList list1 = (ArrayList) val.get("links");
                if (list1 != null) {
                    ((Map) info.get(area)).put("links", list1);
                }
                val1 = (Map) val.get("node_protocol_accounts");
                if (val1 != null) {
                    ((Map) info.get(area)).put("node_protocol_accounts", val1);
                }
                val1 = (Map) val.get("nodes_information");
                if (val1 != null) {
                    ((Map) info.get(area)).put("nodes_information", val1);
                }
                list1 = (ArrayList) val.get("mac_ip_port");
                if (list1 != null) {
                    ((Map) info.get(area)).put("mac_ip_port", list1);
                }
                val1 = (Map) val.get("texts");
                if (val1 != null) {
                    ((Map) info.get(area)).put("texts", val1);
                }
            } else {
                Map map_tmp = new HashMap();
                Map val1 = (Map) val.get("nodes_information_extended");
                if (val1 != null) {
                    map_tmp.put("nodes_information_extended", val1);
                }
                ArrayList list1 = (ArrayList) val.get("links");
                if (list1 != null) {
                    map_tmp.put("links", list1);
                }
                val1 = (Map) val.get("node_protocol_accounts");
                if (val1 != null) {
                    map_tmp.put("node_protocol_accounts", val1);
                }
                val1 = (Map) val.get("nodes_information");
                if (val1 != null) {
                    map_tmp.put("nodes_information", val1);
                }
                list1 = (ArrayList) val.get("mac_ip_port");
                if (list1 != null) {
                    map_tmp.put("mac_ip_port", list1);
                }
                val1 = (Map) val.get("texts");
                if (val1 != null) {
                    map_tmp.put("texts", val1);
                }
                info.put(area, map_tmp);
            }
        }

        // remove areas not scanned
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) info).entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            if (scan_info.get(area) != null) {
                result.put(area, val);
            }
        }
        return result;
    }

    public boolean inside_Network(String ip, String network) {
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            if (ip.equals(network.split("[/\\s+]")[0]))
                return true;
            if (network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                String[] net_mask = network.split("\\s+");
                if (net_mask.length == 2) {
                    SubnetInfo subnet = (new SubnetUtils(net_mask[0], net_mask[1])).getInfo();
                    return subnet.isInRange(ip);
                } else {
                    return false;
                }
            } else if (network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+/32$")) {
                return network.split("/")[0].equals(ip);
            } else if (network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+$")) {
                SubnetInfo subnet = (new SubnetUtils(network)).getInfo();
                return subnet.isInRange(ip);
            } else if (network.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || network.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/32")) {
                return network.equals(ip);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean inside_Networks(String ip, ArrayList<String> networks) {
        boolean is_inside = false;
        for (String network : networks) {
            if (inside_Network(ip, network)) {
                is_inside = true;
                break;
            }
        }
        return is_inside;
    }


    public Map<String, ArrayList> getForkList(Map<String, Map> INFO) {
        Map<String, ArrayList> result = new HashMap();
        Map<String, ArrayList<String[]>> area_node_community_version_dp = utils.getAreaNodeCommunityVersionDP(INFO);
        for (Map.Entry<String, Map> area : INFO.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
//            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");

            if (area_node_community_version_dp.get(area_name) != null) {
                ArrayList<String[]> node_community_version_dp_list = area_node_community_version_dp.get(area_name);
                ArrayList fork_list = getFork(links, node_community_version_dp_list);
                if (!fork_list.isEmpty()) {
                    result.put(area_name, fork_list);
                }
            }
        }
        return result;
    }

    private ArrayList getFork(ArrayList<ArrayList<String>> links_base,
                              ArrayList<String[]> node_community_version_dp_list) {

        ArrayList<ArrayList<String>> links = new ArrayList();
        for (ArrayList<String> link : links_base) {
            if (link.get(0) != null && link.get(2) != null && link.get(3) != null && link.get(5) != null) {
                links.add(link);
            }
//            System.out.println(link);
        }

        ArrayList fork_list = new ArrayList();
        for (int i = 0; i < links.size(); i++) {
            ArrayList<String> link1 = links.get(i);
//            boolean flag = false;
//            if(
//                (link1.get(0).equals("10.96.115.140") && link1.get(2).equals("GigabitEthernet1/0/14")) ||
//                (link1.get(3).equals("10.96.115.140") && link1.get(5).equals("GigabitEthernet1/0/14"))
//            ) {
//                System.out.println("11111");
//                flag = true;
//            }

            ArrayList<String[]> neighbors_node_iface = new ArrayList();
            for (int j = i + 1; j < links.size(); j++) {
                ArrayList<String> link2 = links.get(j);

                if (link1.get(0).equals(link2.get(0)) && !link1.get(2).equals("unknown") && !link2.get(2).equals("unknown") && equalsIfaceName(link1.get(2), link2.get(2))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(3);
                    mas[1] = link2.get(4);
                    mas[2] = link2.get(5);
                    mas[3] = "";
                    for (String[] node_community_version_dp : node_community_version_dp_list) {
                        if (mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }
                    neighbors_node_iface.add(mas);
                    links.remove(j);
                    j--;
                }

                if (link1.get(0).equals(link2.get(3)) && !link1.get(2).equals("unknown") && !link2.get(5).equals("unknown") && equalsIfaceName(link1.get(2), link2.get(5))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(0);
                    mas[1] = link2.get(1);
                    mas[2] = link2.get(2);
                    mas[3] = "";
                    for (String[] node_community_version_dp : node_community_version_dp_list) {
                        if (mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }
                    neighbors_node_iface.add(mas);
                    links.remove(j);
                    j--;
                }
            }

            if (!neighbors_node_iface.isEmpty()) {
                String[] mas = new String[4];
                mas[0] = link1.get(3);
                mas[1] = link1.get(4);
                mas[2] = link1.get(5);
                mas[3] = "";
                for (String[] node_community_version_dp : node_community_version_dp_list) {
                    if (mas[0].equals(node_community_version_dp[0])) {
                        mas[3] = node_community_version_dp[3];
                        break;
                    }
                }
                neighbors_node_iface.add(mas);
                String[] mas1 = new String[3];
                mas1[0] = link1.get(0);
                mas1[1] = link1.get(1);
                mas1[2] = link1.get(2);

                ArrayList fork = new ArrayList();
                fork.add(mas1);
                fork.add(neighbors_node_iface);
                fork_list.add(fork);
            }

            ArrayList<String[]> neighbors_node_iface1 = new ArrayList();
            for (int j = i + 1; j < links.size(); j++) {
                ArrayList<String> link2 = links.get(j);
                if (link1.get(3).equals(link2.get(0)) && !link1.get(5).equals("unknown") && !link2.get(2).equals("unknown") && equalsIfaceName(link1.get(5), link2.get(2))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(3);
                    mas[1] = link2.get(4);
                    mas[2] = link2.get(5);
                    mas[3] = "";
                    for (String[] node_community_version_dp : node_community_version_dp_list) {
                        if (mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }
                    neighbors_node_iface1.add(mas);
                }

                if (link1.get(3).equals(link2.get(3)) && !link1.get(5).equals("unknown") && !link2.get(5).equals("unknown") && equalsIfaceName(link1.get(5), link2.get(5))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(0);
                    mas[1] = link2.get(1);
                    mas[2] = link2.get(2);
                    mas[3] = "";
                    for (String[] node_community_version_dp : node_community_version_dp_list) {
                        if (mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }
                    neighbors_node_iface1.add(mas);
                }
            }

            if (!neighbors_node_iface1.isEmpty()) {
                String[] mas = new String[4];
                mas[0] = link1.get(0);
                mas[1] = link1.get(1);
                mas[2] = link1.get(2);
                mas[3] = "";
                for (String[] node_community_version_dp : node_community_version_dp_list) {
                    if (mas[0].equals(node_community_version_dp[0])) {
                        mas[3] = node_community_version_dp[3];
                        break;
                    }
                }
                neighbors_node_iface1.add(mas);
                String[] mas1 = new String[3];
                mas1[0] = link1.get(3);
                mas1[1] = link1.get(4);
                mas1[2] = link1.get(5);

                ArrayList fork = new ArrayList();
                fork.add(mas1);
                fork.add(neighbors_node_iface1);
                fork_list.add(fork);
            }
        }
        return fork_list;
    }

    public Map<String, ArrayList> getForkLinks(Map<String, ArrayList> area_forks, Map<String, Map> INFO,
                                               Map<String, ArrayList<String[]>> area_arp_mac_table) {

        Map<String, ArrayList> area_add_del_links = new HashMap();
        for (Map.Entry<String, Map> area : INFO.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            ArrayList<String[]> arp_mac_table = area_arp_mac_table.get(area_name);

            ArrayList<ArrayList<String>> links = new ArrayList();
            if (val.get("links") != null) {
                links = (ArrayList<ArrayList<String>>) val.get("links");
            }
//            if(val.get("mac_ip_port") != null)
//                ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>)val.get("mac_ip_port");  

            ArrayList<ArrayList<String>> add_links = new ArrayList();
            ArrayList<ArrayList<String>> del_links = new ArrayList();
            if (area_forks.get(area_name) != null) {
                for (ArrayList forks : (ArrayList<ArrayList>) area_forks.get(area_name)) {
                    String[] parent_node_iface = (String[]) forks.get(0);
                    ArrayList<String[]> children_list = (ArrayList<String[]>) forks.get(1);
                    int count_lldp = 0;
                    String[] lldp_node = new String[3];
                    for (String[] child : children_list) {
                        if (child[3].equals("lldp")) {
                            count_lldp = count_lldp + 1;
                            lldp_node[0] = child[0];
                            lldp_node[1] = child[1];
                            lldp_node[2] = child[2];
                        }
                    }

                    if (count_lldp == 1) {
//                        ArrayList<String> link = new ArrayList();
//                        link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
//                        link.add(lldp_node[0]); link.add(lldp_node[1]); link.add(lldp_node[2]);
//                        link.add("{\"type\":\"lldp\"}");
//                        System.out.println("Adding fork link to middle "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
//                        add_links.add(link);

                        ArrayList<String[]> children_list_new = new ArrayList();
                        for (String[] child : children_list) {
                            if (!(child[0].equals(lldp_node[0]) && child[1].equals(lldp_node[1]) && child[2].equals(lldp_node[2]))) {
                                children_list_new.add(child);
                            }
                        }
                        children_list = children_list_new;

                        for (String[] child : children_list) {
                            boolean find = false;
                            for (ArrayList<String> iter : links) {
                                if (lldp_node[0].equals(iter.get(0)) && child[0].equals(iter.get(3))) {
//                                    ArrayList<String> link = new ArrayList();
//                                    link.add(iter.get(0)); link.add(iter.get(1)); link.add(iter.get(2));
//                                    link.add(iter.get(3)); link.add(iter.get(4)); link.add(iter.get(5));
//                                    link.add(iter.get(6));
//                                    add_links.add(link);
                                    find = true;
//                                    System.out.println("Adding fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    ArrayList<String> link = new ArrayList();
                                    link.add(parent_node_iface[0]);
                                    link.add(parent_node_iface[1]);
                                    link.add(parent_node_iface[2]);
                                    link.add(child[0]);
                                    link.add(child[1]);
                                    link.add(child[2]);
                                    del_links.add(link);
                                    logger.Println("Remove fork link from links " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
//                                    System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);                                    
                                    break;
                                }
                                if (lldp_node[0].equals(iter.get(3)) && child[0].equals(iter.get(0))) {
//                                    link = new ArrayList();
//                                    link.add(iter.get(3)); link.add(iter.get(4)); link.add(iter.get(5));
//                                    link.add(iter.get(0)); link.add(iter.get(1)); link.add(iter.get(2));
//                                    link.add(iter.get(6));
//                                    add_links.add(link);
                                    find = true;
//                                    System.out.println("Adding fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    ArrayList<String> link = new ArrayList();
                                    link.add(parent_node_iface[0]);
                                    link.add(parent_node_iface[1]);
                                    link.add(parent_node_iface[2]);
                                    link.add(child[0]);
                                    link.add(child[1]);
                                    link.add(child[2]);
                                    del_links.add(link);
                                    logger.Println("Remove fork link from links " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
//                                    System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    break;
                                }
                            }
                            if (!find) {
                                for (String[] child1 : children_list) {
                                    boolean find1 = false;
                                    if (arp_mac_table != null) {
                                        for (String[] nodeport_ip_mac : arp_mac_table) {
                                            if (nodeport_ip_mac[0].equals(lldp_node[0]) && nodeport_ip_mac[3].equals(child1[0])) {
                                                ArrayList<String> link = new ArrayList();
                                                link.add(nodeport_ip_mac[0]);
                                                link.add(nodeport_ip_mac[1]);
                                                link.add(nodeport_ip_mac[2]);
                                                link.add(child1[0]);
                                                link.add(child1[1]);
                                                link.add(child1[2]);
                                                link.add("mac_ip_port");
                                                boolean found = false;
                                                for (ArrayList<String> iter : add_links) {
                                                    if (iter.get(0).equals(link.get(0)) && iter.get(1).equals(link.get(1)) && iter.get(2).equals(link.get(2))
                                                            && iter.get(3).equals(link.get(3)) && iter.get(4).equals(link.get(4)) && iter.get(5).equals(link.get(5))) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                if (!found) {
                                                    add_links.add(link);
                                                    logger.Println("Adding fork link from mac_ip_port " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                                } else {
                                                    logger.Println("Not adding duplicate fork link from mac_ip_port " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                                }
                                                find1 = true;
                                                link = new ArrayList();
                                                link.add(parent_node_iface[0]);
                                                link.add(parent_node_iface[1]);
                                                link.add(parent_node_iface[2]);
                                                link.add(child[0]);
                                                link.add(child[1]);
                                                link.add(child[2]);
                                                del_links.add(link);
                                                //                                            System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                                logger.Println("Remove fork link from links " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                                break;
                                            }
                                        }
                                    }
                                    if (!find1) {
                                        ArrayList<String> link = new ArrayList();
                                        link.add(lldp_node[0]);
                                        link.add("");
                                        link.add("unknown");
                                        link.add(child1[0]);
                                        link.add(child1[1]);
                                        link.add(child1[2]);
                                        link.add("mac_ip_port");
                                        boolean found = false;
                                        for (ArrayList<String> iter : add_links) {
                                            if (iter.get(0).equals(link.get(0)) && iter.get(1).equals(link.get(1)) && iter.get(2).equals(link.get(2))
                                                    && iter.get(3).equals(link.get(3)) && iter.get(4).equals(link.get(4)) && iter.get(5).equals(link.get(5))) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            add_links.add(link);
                                            logger.Println("Adding fork link from mac_ip_port " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                        } else {
                                            logger.Println("Not adding fork link from mac_ip_port " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                        }
                                        logger.Println("Not link node " + child[0] + " " + child[2], logger.DEBUG);
                                        link = new ArrayList();
                                        link.add(parent_node_iface[0]);
                                        link.add(parent_node_iface[1]);
                                        link.add(parent_node_iface[2]);
                                        link.add(child[0]);
                                        link.add(child[1]);
                                        link.add(child[2]);
                                        del_links.add(link);
//                                        System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);                                            
                                        logger.Println("Remove fork link from links " + parent_node_iface[0] + " " + parent_node_iface[2] + ": " + link, logger.DEBUG);
                                    }
                                }

                            }
                        }

                    }
                }
            }
            if (!add_links.isEmpty() || !del_links.isEmpty()) {
                ArrayList item = new ArrayList();
                item.add(add_links);
                item.add(del_links);
                area_add_del_links.put(area_name, item);
            }
        }

        return area_add_del_links;

    }

    public void modifyLinks(Map<String, Map> INFO, Map<String, ArrayList> area_add_del_links) {
        for (Map.Entry<String, Map> area : INFO.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();

            ArrayList add_del_links = area_add_del_links.get(area_name);
            if (add_del_links != null) {
//                ArrayList<ArrayList<String>> links = new ArrayList();
//                if(val.get("links") != null)
//                    links = (ArrayList<ArrayList<String>>)val.get("links"); 

                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                if (links != null) {
                    ArrayList<ArrayList<String>> add_links = (ArrayList<ArrayList<String>>) add_del_links.get(0);
                    for (ArrayList<String> item : add_links) {
                        links.add(item);
//                        System.out.println("Adding link: "+item.get(0)+" "+item.get(2)+" <---> "+item.get(3)+" "+item.get(5)+" --- "+item.get(6));
                        logger.Println("Adding link: " + item.get(0) + " " + item.get(2) + " <---> " + item.get(3) + " " + item.get(5) + " --- " + item.get(6), logger.DEBUG);
                    }

                    ArrayList<ArrayList<String>> del_links = (ArrayList<ArrayList<String>>) add_del_links.get(1);
                    ArrayList<ArrayList<String>> links_new = new ArrayList();
                    for (ArrayList<String> link : links) {
                        boolean found = false;
                        for (ArrayList<String> del_link : del_links) {
                            if ((del_link.get(0).equals(link.get(0)) && del_link.get(1).equals(link.get(1)) && del_link.get(2).equals(link.get(2))
                                    && del_link.get(3).equals(link.get(3)) && del_link.get(4).equals(link.get(4)) && del_link.get(5).equals(link.get(5)))
                                    || (del_link.get(0).equals(link.get(3)) && del_link.get(1).equals(link.get(4)) && del_link.get(2).equals(link.get(5))
                                    && del_link.get(3).equals(link.get(0)) && del_link.get(4).equals(link.get(1)) && del_link.get(5).equals(link.get(2)))) {
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            logger.Println("Remove link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + " --- " + link.get(6), logger.DEBUG);
                        } else {
                            links_new.add(link);
                        }
                    }
                    links.clear();
                    links.addAll(links_new);
                }
            }
        }
    }

    public Map setAttributesToNodes(Map INFORMATION, String last_day_file, String attribute_old_file) {
        // set nodes attributes
        Map<String, Map> information_last_day = utils.readJSONFile(last_day_file);
        Map<String, Map> node_attribute_old_from_file = utils.readJSONFile(attribute_old_file);
        Map<String, Map> node_attribute = node_attribute_old_from_file;

        Map<String, Map> node_attribute_last_day = utils.getNodesAttributes(information_last_day);
        for (Map.Entry<String, Map> entry : node_attribute_last_day.entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            if (val != null) {
                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
                    String node = entry1.getKey();
                    Map val1 = entry1.getValue();
                    if (val1 != null) {
                        Map value = (Map) getKey("/" + area + "/" + node, node_attribute);
                        if (value != null) {
                            setKey("/" + area + "/" + node, val1, node_attribute);
                        } else {
                            if (node_attribute.get(area) != null) {
                                node_attribute.get(area).put(node, val1);
                            } else {
                                Map<String, Map> map_tmp = new HashMap();
                                map_tmp.put(node, val1);
                                node_attribute.put(area, map_tmp);
                            }
                        }
                    }
                }
            }
        }

//        // save nodes attribute old
//        Map<String, Map> nodes_attribute_old = utils.saveNodesAttributesOld(node_attribute, INFORMATION);
//
//        // write to file nodes_information
//        if(DEBUG) utils.mapToFile(nodes_attribute_old, attribute_old_file, Neb.DELAY_WRITE_FILE);

        INFORMATION = utils.setNodesAttributes(node_attribute, INFORMATION);
//        utils.MapToFile((Map) INFORMATION, map_file_pre); 

        return INFORMATION;
    }

    public Map getName(Map INFORMATION) {
        Map<String, String> name_ip = new HashMap();
        for (Map.Entry<String, Map> entry : ((Map<String, Map>) INFORMATION).entrySet()) {
//            String area = entry.getKey();
//            System.out.println(area);
            Map val = entry.getValue();
            ArrayList<String> nodes = new ArrayList();
            if (val.get("nodes_information") != null) {
                for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val.get("nodes_information")).entrySet()) {
                    String node = entry1.getKey();
                    if (node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        nodes.add(node);
//                        try {
//                            InetAddress ia = InetAddress.getByName(node);
//                            if(!ia.getCanonicalHostName().equals(node)) {
//                                String name = ia.getCanonicalHostName().split("\\.")[0].toLowerCase();
//                                name_ip.put(name, node);
////                                System.out.println(node+" - "+name);
//                            }
//                        } catch (Exception ex) {}

                    }
                }
            }

            ArrayList<String[]> mac_ip_port = (ArrayList) val.get("mac_ip_port");
            if (mac_ip_port != null) {
                for (String[] item : mac_ip_port) {
                    String host = item[1];
                    if (host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        nodes.add(host);
//                        try {
//                            InetAddress ia = InetAddress.getByName(host);
//                            if(!ia.getCanonicalHostName().equals(host)) {
//                                String name = ia.getCanonicalHostName().split("\\.")[0].toLowerCase();
//                                name_ip.put(name, host);
////                                System.out.println(host+" - "+name);
//                            }
//                        } catch (Exception ex) {}

                    }
                }
            }

            GetName getName = new GetName();
            Map<String, String> result = getName.get(nodes, Neb.timeout_thread);
            name_ip.putAll(result);
        }
        return name_ip;
    }

    public void setName(Map<String, Map> info, String names_file) {
//        Map<String, Map> info = readJSONFile(info_file);
        Map<String, String> name_ip_fromfile = readJSONFile(names_file);
        Map name_ip = getName(info);

        for (Map.Entry<String, String> entry : ((Map<String, String>) name_ip).entrySet()) {
            String name = entry.getKey();
            String ip = entry.getValue();
            name_ip_fromfile.put(name, ip);

        }

        mapToFile(name_ip_fromfile, names_file, Neb.DELAY_WRITE_FILE);
    }

    public Map<String, Map> setAttributeOld(Map<String, Map> info_old, String node_attribute_old_file) {
        Map<String, Map> area_nodes_attribute_old = readJSONFile(node_attribute_old_file);
        Map<String, Map> node_attribute_last_day = getNodesAttributes(info_old);
        Map<String, Map> area_mac_node = new HashMap();
        for (Map.Entry<String, Map> entry : area_nodes_attribute_old.entrySet()) {
            String area = entry.getKey();
            Map<String, Map> area_info = entry.getValue();
            Map<String, String> mac_node = new HashMap();
            for (Map.Entry<String, Map> entry1 : area_info.entrySet()) {
                String node = entry1.getKey();
                Map node_info = entry1.getValue();
                String mac = (String) node_info.get("base_address");
                if (mac != null && !mac.startsWith("00:00:00:00") && !mac.startsWith("01:00:00:00")) {
                    mac_node.put(mac, node);
                }
            }
            if (mac_node.size() > 0) {
                area_mac_node.put(area, mac_node);
            }
        }

        for (Map.Entry<String, Map> area : node_attribute_last_day.entrySet()) {
            String area_name = area.getKey();
            Map<String, Map> nodes_attr = area.getValue();
//            Map<String, Map> nodes_attribute_old = new HashMap();
//            if(area_node_attribute_old.get(area_name) != null) {
//                nodes_attribute_old = area_node_attribute_old.get(area_name);
//            }

            for (Map.Entry<String, Map> iter : nodes_attr.entrySet()) {
                String node = iter.getKey();
                Map val = iter.getValue();

                String base_address = (String) val.get("base_address");
                String image = (String) val.get("image");
                String image_auto = (String) val.get("image_auto");
                ArrayList<String> xy = (ArrayList) val.get("xy");
                Map attribute = new HashMap();
                if (image != null)
                    attribute.put("image", image);
                if (image_auto != null)
                    attribute.put("image_auto", image_auto);
                if (xy != null)
                    attribute.put("xy", xy);
                if (base_address != null)
                    attribute.put("base_address", base_address);
                String node_replaced = null;
                if (area_mac_node.get(area_name) != null) {
                    node_replaced = (String) area_mac_node.get(area_name).get(base_address);
                }
                if (area_nodes_attribute_old.get(area_name) == null) {
                    Map node_attribute = new HashMap();
                    node_attribute.put(node, attribute);
                    area_nodes_attribute_old.put(area_name, node_attribute);
                } else {
                    if (node_replaced != null && !node.equals(node_replaced)) {
                        area_nodes_attribute_old.get(area_name).remove(node_replaced);
                    }
                    area_nodes_attribute_old.get(area_name).put(node, attribute);
                }
            }
        }

        return area_nodes_attribute_old;
    }

    private ArrayList<ArrayList<String>> removeDuplicateLinks(ArrayList<ArrayList<String>> links) {
        // remove duplicate links
        ArrayList<String[]> remove_links_list_all = new ArrayList();
        Map<Integer, Integer> exclude_positions = new HashMap();
        for (int i = 0; i < links.size(); i++) {
//            System.out.println("i="+i);
            ArrayList<String[]> remove_links_list = new ArrayList();
            if (exclude_positions.get(i) == null) {
                String node1 = "";
                String id1 = "";
                String iface1 = "";
                String node2 = "";
                String id2 = "";
                String iface2 = "";
                if (links.get(i).size() == 5) {
                    node1 = links.get(i).get(0);
                    iface1 = links.get(i).get(1);
                    node2 = links.get(i).get(2);
                    iface2 = links.get(i).get(3);
                } else {
                    node1 = links.get(i).get(0);
                    id1 = links.get(i).get(1);
                    iface1 = links.get(i).get(2);
                    node2 = links.get(i).get(3);
                    id2 = links.get(i).get(4);
                    iface2 = links.get(i).get(5);
                }
                String[] mas = new String[5];
                mas[0] = node1;
                mas[1] = iface1;
                mas[2] = node2;
                mas[3] = iface2;
                mas[4] = String.valueOf(i);
                remove_links_list.add(mas);

                for (int j = i + 1; j < links.size(); j++) {
//                System.out.println("  j="+j);
                    if (exclude_positions.get(j) == null) {
                        String node3 = "";
                        String id3 = "";
                        String iface3 = "";
                        String node4 = "";
                        String id4 = "";
                        String iface4 = "";
                        if (links.get(j).size() == 5) {
                            node3 = links.get(j).get(0);
                            iface3 = links.get(j).get(1);
                            node4 = links.get(j).get(2);
                            iface4 = links.get(j).get(3);
                        } else {
                            node3 = links.get(j).get(0);
                            id3 = links.get(j).get(1);
                            iface3 = links.get(j).get(2);
                            node4 = links.get(j).get(3);
                            id4 = links.get(j).get(4);
                            iface4 = links.get(j).get(5);
                        }

                        if (
                                (id1.matches("\\d+") && id3.matches("\\d+")) ||
                                        (id1.matches("\\d+") && id4.matches("\\d+")) ||
                                        (id2.matches("\\d+") && id4.matches("\\d+")) ||
                                        (id2.matches("\\d+") && id3.matches("\\d+"))
                        ) {
                            if (
                                    (id1.matches("\\d+") && id3.matches("\\d+") && node1.equals(node3) && id1.equals(id3) && node2.equals(node4)) ||
                                            (id1.matches("\\d+") && id4.matches("\\d+") && node1.equals(node4) && id1.equals(id4) && node2.equals(node3)) ||
                                            (id2.matches("\\d+") && id4.matches("\\d+") && node1.equals(node3) && node2.equals(node4) && id2.equals(id4)) ||
                                            (id2.matches("\\d+") && id3.matches("\\d+") && node1.equals(node4) && node2.equals(node3) && id2.equals(id3))
                            ) {
                                String[] mas1 = new String[5];
                                mas1[0] = node3;
                                mas1[1] = iface3;
                                mas1[2] = node4;
                                mas1[3] = iface4;
                                mas1[4] = String.valueOf(j);
                                remove_links_list.add(mas1);
                                exclude_positions.put(j, j);
                            }

                        } else {
                            if ((node1.equals(node3) && node2.equals(node4) && equalsIfaceName(iface1, iface3))
                                    || (node1.equals(node4) && node2.equals(node3) && equalsIfaceName(iface1, iface4))
                                    || (node1.equals(node3) && node2.equals(node4) && equalsIfaceName(iface2, iface4))
                                    || (node1.equals(node4) && node2.equals(node3) && equalsIfaceName(iface2, iface3))) {
                                String[] mas1 = new String[5];
                                mas1[0] = node3;
                                mas1[1] = iface3;
                                mas1[2] = node4;
                                mas1[3] = iface4;
                                mas1[4] = String.valueOf(j);
                                remove_links_list.add(mas1);
                                exclude_positions.put(j, j);
                            }

                        }
                    }
                }
            }
            if (remove_links_list.size() > 1) {
                ArrayList<String[]> links_new = new ArrayList();
                logger.Println("------------------------------------------------------------------------", logger.DEBUG);
                for(String[] link : remove_links_list) {
                    logger.Println("Dublicate: " + link[0] + " " + link[1] + " <---> " + link[2] + " " + link[3], logger.DEBUG);
                    if(!link[3].equals("unknown")) {
                        links_new.add(link);
                    }
                }
                logger.Println("------------------------------------------------------------------------", logger.DEBUG);
                if(links_new.isEmpty()) {
                    remove_links_list.remove(0);
                } else {
                    int max_len = 0;
                    int pos = 0;
                    int ii = 0;
                    for(String[] link1 : links_new) {
                        if((link1[1]+";"+link1[3]).length() > max_len) {
                            max_len = (link1[1]+";"+link1[3]).length();
                            pos = ii;
                        }
                        ii++;
                    }
                    logger.Println("Main link: "+links_new.get(pos)[0]+" "+links_new.get(pos)[1]+" <---> "+links_new.get(pos)[2]+" "+links_new.get(pos)[3], logger.DEBUG);
                    logger.Println("===================================================================", logger.DEBUG);
                    links_new.remove(pos);
                    remove_links_list = links_new;
                }
                remove_links_list_all.addAll(remove_links_list);
            }
        }
        class MaxToMinPosition implements Comparator {
            public int compare(Object obj1, Object obj2) {
                Integer pos1 = Integer.parseInt(((String[])obj1)[4]);
                Integer pos2 = Integer.parseInt(((String[])obj2)[4]);
                // Compare the objects
                if (pos1 > pos2) return -1;
                if (pos1 < pos2) return 1;
                return 0;
            }
        }
        Comparator maxToMinPosition = new MaxToMinPosition();

        Collections.sort(remove_links_list_all, maxToMinPosition);
        for(String[] link : remove_links_list_all) {
            links.remove(Integer.parseInt(link[4]));
        }

//        for(int pos : position) {
//            ArrayList<String> link = links.get(pos);
//            if (link.size() == 5) {
//                logger.Println("Remove Link: " + link.get(0) + " " + link.get(1) + " <---> " + link.get(2) + " " + link.get(3), logger.DEBUG);
//            } else {
//                logger.Println("Remove Link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
//            }
//            links.remove(pos);
//

        return links;
    }

    private ArrayList<String[]> removeDuplicateMacIpPort(ArrayList<String[]> mac_ip_port) {
        // remove duplicate mac ip port
        Map<String, String[]> map_mip = new HashMap();
        for (String[] mip : mac_ip_port) {
            if (map_mip.get(mip[0] + "|" + mip[1]) == null) {
                map_mip.put(mip[0] + "|" + mip[1], mip);
            } else {
                logger.Println("Remove duplicate mac_ip_port: " + mip[0] + ", " + mip[1] + " - " + mip[2] + ", " + mip[3] + ", " + mip[4], logger.DEBUG);
            }
        }

        ArrayList<String[]> mac_ip_port_uniqal = new ArrayList();
        for (Map.Entry<String, String[]> entry : map_mip.entrySet()) {
            String[] val = entry.getValue();
            mac_ip_port_uniqal.add(val);
        }

        return mac_ip_port_uniqal;
    }

    public boolean authDC(String username, String password, String serverName, String domainName) {
        @SuppressWarnings("unused")
        LdapContext ctx = null;
        try {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.SECURITY_AUTHENTICATION, "Simple");
            String principalName = username + "@" + domainName;
            env.put(Context.SECURITY_PRINCIPAL, principalName);
            env.put(Context.SECURITY_CREDENTIALS, password);

            long cur_time = (new Date()).getTime();
            if (Neb.user_passwd_buffer.get(principalName) != null) {
                long t = Long.parseLong(Neb.user_passwd_buffer.get(principalName)[0]);
                String pass = Neb.user_passwd_buffer.get(principalName)[1];
                if (pass.equals(password) && cur_time - t < Neb.user_passwd_buffer_timeout) {
                    Neb.user_passwd_buffer.get(principalName)[0] = String.valueOf(cur_time);
                    Neb.user_passwd_buffer.get(principalName)[1] = password;
                    return true;
                } else {
                    Neb.user_passwd_buffer.remove(principalName);
                }
            }

            String protocol = (String) ((Map) Neb.cfg.get("AD_auth")).get("protocol");
            String port = (String) ((Map) Neb.cfg.get("AD_auth")).get("port");
            if (protocol.equals("ldaps")) {
                env.put("java.naming.ldap.factory.socket", "ru.kos.neb.neb_builder.MySSLSocketFactory");
                env.put(Context.PROVIDER_URL, "ldaps://" + serverName + "." + domainName + ":" + port + "/");
            } else {
                env.put(Context.PROVIDER_URL, "ldap://" + serverName + "." + domainName + ":" + port + "/");
            }
            ctx = new InitialLdapContext(env, null);
            if (Neb.user_passwd_buffer.get(principalName) == null) {
                String[] mas = new String[2];
                mas[0] = String.valueOf(cur_time);
                mas[1] = password;
                Neb.user_passwd_buffer.put(principalName, mas);
            }

            return true;
        } catch (NamingException nex) {
            return false;
        }

    }

    public Map<String, Map<String, Map<String, String>>> getNodeNeighbors(Map<String, Map> info_map) {
        Map<String, Map<String, Map<String, String>>> area_node_neighbors = new HashMap();
        for (Map.Entry<String, Map> area : info_map.entrySet()) {
            String area_name = area.getKey();
            Map<String, Map> area_info = area.getValue();
            Map<String, Map> nodes_information = area_info.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
            Map<String, Map<String, String>> node_neighbors = new HashMap();
            for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                String node = entry.getKey();
                Map<String, String> neighbors = new HashMap();
                for (ArrayList<String> link : links) {
                    if (node.equals(link.get(0))) {
                        neighbors.put(link.get(3), link.get(3));
                    }
                    if (node.equals(link.get(3))) {
                        neighbors.put(link.get(0), link.get(0));
                    }
                }
                node_neighbors.put(node, neighbors);
            }
            area_node_neighbors.put(area_name, node_neighbors);
        }
        return area_node_neighbors;
    }

    public Map<String, ArrayList<String>> differentNodeNeightbors(Map<String, Map<String, Map<String, String>>> area_node_neighbors_old,
                                                                  Map<String, Map<String, Map<String, String>>> area_node_neighbors_new) {
        Map<String, ArrayList<String>> area_node = new HashMap();
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : area_node_neighbors_new.entrySet()) {
            String area = entry.getKey();
            Map<String, Map<String, String>> node_neighbors_new = entry.getValue();
            ArrayList<String> diff_node_list = new ArrayList();
            if (area_node_neighbors_old.get(area) != null) {
                for (Map.Entry<String, Map<String, String>> entry1 : node_neighbors_new.entrySet()) {
                    String node = entry1.getKey();
//                    if(node.equals("10.32.69.208"))
//                        System.out.println("1111111111111111");
                    if (area_node_neighbors_old.get(area).get(node) != null) {
                        Map<String, String> neighbors_new = entry1.getValue();
                        if (!neighbors_new.isEmpty()) {
                            int count_diff = 0;
                            for (Map.Entry<String, String> entry2 : neighbors_new.entrySet()) {
                                String neighbor = entry2.getKey();
                                if (area_node_neighbors_old.get(area).get(node).get(neighbor) == null) {
                                    count_diff = count_diff + 1;
                                }
                            }
                            if (count_diff / neighbors_new.size() >= 0.3) {
                                diff_node_list.add(node);
                            }
                        }
                    }
                }
            }
            area_node.put(area, diff_node_list);
        }
        return area_node;
    }

    public Integer getNodeDiameterGraph(ArrayList<ArrayList<String>> links, String node) {
        ArrayList<String> nodes_list = new ArrayList();
        nodes_list.add(node);
        Map<String, String> nodes_exclude = new HashMap();
        Map<String, ArrayList<String>> node_neightboards = getNodeNeightboards(links);
        int i = 1;
        while (true) {
            ArrayList res = getOneStepDiameter(nodes_list, nodes_exclude, node_neightboards);
            nodes_list = (ArrayList<String>) res.get(0);
            nodes_exclude = (Map<String, String>) res.get(1);
            if (nodes_list.isEmpty()) {
                break;
            }
            i = i + 1;
        }
        return i;
    }

    private ArrayList getOneStepDiameter(ArrayList<String> node_list,
                                         Map<String, String> nodes_exclude,
                                         Map<String, ArrayList<String>> node_neightboards) {
        ArrayList result = new ArrayList();
        ArrayList node_list_new = new ArrayList();
        for (String node : node_list) {
            if (node_neightboards.get(node) != null) {
                ArrayList<String> neightboards = node_neightboards.get(node);
                for (String node_nei : neightboards) {
                    if (nodes_exclude.get(node_nei) == null) {
                        node_list_new.add(node_nei);
                    }
                }
            }
            nodes_exclude.put(node, node);
        }
        result.add(node_list_new);
        result.add(nodes_exclude);
        return result;
    }

//    private static String HTTPRequestGET(String url_str, String user, String passwd) {
//        String out = "";
//
//        try {
//            URL url = new URL(url_str);
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setRequestProperty("user", user);
//            con.setRequestProperty("passwd", passwd);
//            con.setRequestMethod("GET");
//            int status = con.getResponseCode();
//
//            if (status == 200) {
//                try ( BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
//                    String inputLine;
//                    StringBuilder content = new StringBuilder();
//                    while ((inputLine = in.readLine()) != null) {
//                        content.append(inputLine + "\n");
//                    }
//                    out = content.toString();
//                }
//            }
//            con.disconnect();
//        } catch (IOException e) {
////            e.printStackTrace();
//            logger.Println("HTTPRequestGET: "+url_str+" not connect to Neb server!", logger.DEBUG);
//        }
//        return out;
//    }
//    private static String HTTPRequestPOST(String url_str, String data, String user, String passwd) {
//        String result = "";
//        try {
//            URL url = new URL(url_str);
//            byte[] postData = data.getBytes(StandardCharsets.UTF_8);
//
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con = (HttpURLConnection) url.openConnection();
//
//            con.setDoOutput(true);
//            con.setRequestMethod("POST");
//            con.setRequestProperty("User-Agent", "Java client");
//            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            con.setRequestProperty("user", user);
//            con.setRequestProperty("passwd", passwd);
//
//            try ( DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
//                wr.write(postData);
//            }
//
//            StringBuilder content;
//
//            try ( BufferedReader in = new BufferedReader(
//                    new InputStreamReader(con.getInputStream()))) {
//
//                String line;
//                content = new StringBuilder();
//
//                while ((line = in.readLine()) != null) {
//                    content.append(line);
//                    content.append(System.lineSeparator());
//                }
//            }
//
//            int code = con.getResponseCode();
//            if (code == 200) {
//                result = content.toString();
//            }
//            con.disconnect();
////            System.out.println("response code: "+con.getResponseCode());
////            result = content.toString();
//        } catch (IOException ex) {
////            ex.printStackTrace();
//            logger.Println("HTTPRequestPOST: "+url_str+" not connect to Neb server!", logger.DEBUG);
//        }
//        return result;
//    }

//    private String[] splitCommandString(String command) {
//        Pattern p = Pattern.compile("(\"\\S+\\s+\\S+\")");
//        while (true) {
//            Matcher m = p.matcher(command);
//            if (m.find()) {
//                String find_str = m.group(1);
//                command = command.replace(find_str, find_str.replaceAll("\\s+", "%BLANK%"));
//            } else {
//                break;
//            }
//        }
//
//        String[] mas = command.split("\\s+");
//        int i = 0;
//        for (String s : mas) {
//            mas[i] = s.replace("%BLANK%", " ");
//            i++;
//        }
//        return mas;
//    }

    public ArrayList<String[]> define_Images(Map INFORMATION, Map area_nodes_images_buffer) {
        ArrayList<String[]> list = getNodesInfo(INFORMATION);
        ArrayList<String[]> list_buffer = getNodesInfo(area_nodes_images_buffer);
        // merging list and list_buffer
        for (String[] it_buf : list_buffer) {
            boolean found = false;
            int i = 0;
            for (String[] it : list) {
                if (it_buf[3].equals(it[3]) && it_buf[1].equals(it[1])) {
                    found = true;
                    break;
                }
                i += 1;
            }
            if (found) {
                list.set(i, it_buf);
            }
        }

        Map<String, ArrayList<String[]>> base_groups = split_Groups_from_images(list);

        Map<String, Map<String, double[]>> groups_node_vector = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : base_groups.entrySet()) {
            String group_image = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, double[]> node_vector = vectorization(val);
            groups_node_vector.put(group_image, node_vector);
        }

        Map<String, double[]> undefined_group = new HashMap();
        Map<String, Map<String, double[]>> groups_node_vector_new = new HashMap();
        for (Map.Entry<String, Map<String, double[]>> entry : groups_node_vector.entrySet()) {
            String group_image = entry.getKey();
            Map<String, double[]> node_vector = entry.getValue();
            if (group_image.matches(".*default.png$")) {
                undefined_group = node_vector;
            } else if (group_image.matches(".*Stack.*.png$")) {

            } else {
                groups_node_vector_new.put(group_image, node_vector);
            }
        }
        groups_node_vector = groups_node_vector_new;

//        Map<String, ArrayList<Map<String, double[]>>> group_image_cluster = clustering_from_image_groups(groups_node_vector);
        ArrayList<String[]> list1 = new ArrayList();
        for (Map.Entry<String, Map<String, double[]>> entry : groups_node_vector.entrySet()) {
            Map<String, double[]> val = entry.getValue();
            for (Map.Entry<String, double[]> entry1 : val.entrySet()) {
                String[] mas = entry1.getKey().split(",\\s+");
                list1.add(mas);
            }
        }
        for (Map.Entry<String, double[]> entry : undefined_group.entrySet()) {
            String[] mas = entry.getKey().split(",\\s+");
            list1.add(mas);
        }
        Map<String, double[]> node_vector = vectorization(list1);
        Map<String, ArrayList<double[]>> group_image_cluster_vector = new HashMap();
        for (Map.Entry<String, Map<String, double[]>> entry : groups_node_vector.entrySet()) {
            String group_image = entry.getKey();
            Map<String, double[]> val = entry.getValue();
            ArrayList<double[]> tmp_list = new ArrayList();
            Map<String, double[]> tmp_map = new HashMap();
            for (Map.Entry<String, double[]> entry1 : val.entrySet()) {
                String str = entry1.getKey();
                if (node_vector.get(str) != null) {
                    tmp_map.put(str, node_vector.get(str));
                }
            }
            if (!tmp_map.isEmpty()) {
                double[] center = getCenter(tmp_map);
                //                Map<String, double[]> tmp_map_center = new HashMap();
                //                tmp_map_center.put("center", center);
                tmp_list.add(center);
                group_image_cluster_vector.put(group_image, tmp_list);
            }
        }
        Map<String, double[]> undefined_group_vector = new HashMap();
        for (Map.Entry<String, double[]> entry : undefined_group.entrySet()) {
            String str = entry.getKey();
            if (node_vector.get(str) != null) {
                undefined_group_vector.put(str, node_vector.get(str));
            }
        }

        ArrayList<String[]> node_image = new ArrayList();
        for (Map.Entry<String, double[]> entry : undefined_group_vector.entrySet()) {
            String str_undef = entry.getKey();
            double[] vector_undef = entry.getValue();
            double min_distance = Double.MAX_VALUE;
            String min_group_image = null;
            for (Map.Entry<String, ArrayList<double[]>> entry1 : group_image_cluster_vector.entrySet()) {
                String group_image = entry1.getKey();
                ArrayList<double[]> val = entry1.getValue();
                double min_distance_cluster = Double.MAX_VALUE;
                for (double[] it : val) {
                    double dis = distance(vector_undef, it);
                    if (dis < min_distance_cluster) {
                        min_distance_cluster = dis;
                    }
                }
//                System.out.println("\t --- "+group_image+" min_distance_cluster: "+min_distance_cluster);
                if (min_distance_cluster < min_distance) {
                    min_distance = min_distance_cluster;
                    min_group_image = group_image;
                }
            }
//            System.out.println(str_undef+"; "+min_group_image+" min_distance; "+min_distance);   
            if (min_distance < 0.01) {
                String[] arr = str_undef.split(",\\s+");
                String node = arr[1];
                String[] mas = new String[4];
                mas[0] = node;
                mas[1] = min_group_image;
                mas[2] = arr[0];
                mas[3] = arr[3];
                node_image.add(mas);
//                System.out.println(mas[0]+" - "+mas[1]+", "+mas[2]+", "+mas[3]);
            }
        }
        return node_image;
    }

    public ArrayList<Map<String, String>> clusteringNodesDelete(Map INFORMATION) {
        ArrayList<String[]> list = getNodesInfo(INFORMATION);

        Map<String, ArrayList<String[]>> base_groups = split_Groups_from_images(list);

        Map<String, Map<String, double[]>> groups_node_vector = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : base_groups.entrySet()) {
            String group_image = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, double[]> node_vector = vectorization(val);
            groups_node_vector.put(group_image, node_vector);
        }

        Map<String, double[]> undefined_group = new HashMap();
        Map<String, Map<String, double[]>> groups_node_vector_new = new HashMap();
        for (Map.Entry<String, Map<String, double[]>> entry : groups_node_vector.entrySet()) {
            String group_image = entry.getKey();
            Map<String, double[]> node_vector = entry.getValue();
            if (group_image.matches(".*default.png$")) {
                undefined_group = node_vector;
            } else if (group_image.matches(".*Stack.*.png$")) {

            } else {
                groups_node_vector_new.put(group_image, node_vector);
            }
        }
        groups_node_vector = groups_node_vector_new;

        ArrayList<Map<String, String>> undefined_group_list = clustering_group(undefined_group);

        return undefined_group_list;
    }

    //    private Map<String, ArrayList<Map<String, double[]>>> clustering_from_image_groups(Map<String, Map<String, double[]>> groups_node_vector) {
//        Map<String, ArrayList<Map<String, double[]>>> group_image_claster = new HashMap();
//        for(Map.Entry<String, Map<String, double[]>> entry : groups_node_vector.entrySet()) {
//            String group_image = entry.getKey();
////            System.out.println(" --- "+group_image);
//            Map<String, double[]> node_vector = entry.getValue();
//            ArrayList<Map<String, double[]>> group_list = new ArrayList();
//            if(node_vector.size() > 1) {
//                Map<String, double[]> node_vector_prima = prima_algoritm(node_vector);
//                group_list.add(node_vector_prima);
//                double sum_diameters_prev = sum_diameters_groups(group_list);
////                System.out.println("iter=0   sum_diameters="+sum_diameters_prev);
//                int iter = 1;
//                while(iter <= 100) {
//                    ArrayList<Map<String, double[]>> group_list_new = splitGroups(group_list);
//                    if(group_list_new.size() == 0) break;
//                    if(group_list.size() == group_list_new.size()) break;
//                    double sum_diameters = sum_diameters_groups(group_list_new);
//                    sum_diameters = Math.abs(sum_diameters)/group_list_new.size();
//                    double delta = sum_diameters_prev - sum_diameters;
//                    if(delta < 0.01) {
//                        break;
//                    }
////                    System.out.println("iter="+iter+"   sum_diameters="+sum_diameters+"   delta="+delta);
//                    group_list = group_list_new;
//                    group_image_claster.put(group_image, group_list);                    
//                    sum_diameters_prev = sum_diameters;
//                    iter++;
//                }
//            } else {
//                group_list.add(node_vector);
//                group_image_claster.put(group_image, group_list);
//            }           
//        }
//        return group_image_claster;
//    }
    private ArrayList<Map<String, String>> clustering_group(Map<String, double[]> node_vector) {
        ArrayList<Map<String, double[]>> group_list = new ArrayList();
        if (node_vector.size() > 1) {
            Map<String, double[]> node_vector_prima = prima_algoritm(node_vector);
            group_list.add(node_vector_prima);
            double sum_diameters_prev = sum_diameters_groups(group_list);
//            System.out.println("iter=0   sum_diameters="+sum_diameters_prev);
            int iter = 1;
            while (iter <= 100) {
                ArrayList<Map<String, double[]>> group_list_new = splitGroups(group_list);
                if (group_list.size() == group_list_new.size()) {
                    break;
                }
                double sum_diameters = sum_diameters_groups(group_list_new);
                sum_diameters = Math.abs(sum_diameters) / group_list_new.size();
                double delta = sum_diameters_prev - sum_diameters;
                if (delta < 0.001) {
                    break;
                }
//                System.out.println("iter="+iter+"   sum_diameters="+sum_diameters+"   delta="+delta);
                group_list = group_list_new;
                sum_diameters_prev = sum_diameters;
                iter++;
            }
        } else {
            group_list.add(node_vector);
        }

        ArrayList<Map<String, String>> result = new ArrayList();
        for (Map<String, double[]> group : group_list) {
            Map<String, String> res = new LinkedHashMap();
            for (Map.Entry<String, double[]> entry : group.entrySet()) {
                String node_str = entry.getKey();
                String node = node_str.split(",")[1].trim();
                res.put(node, node_str);
            }
            result.add(res);
        }

        return result;
    }

    //    private double getMaxDistance(double[] vector, Map<String, double[]> node_vector) {
//        double distance_max = 0;
//        for(Map.Entry<String, double[]> entry : node_vector.entrySet()) {
//            String key = entry.getKey();
//            double[] vector1 = entry.getValue();
//            double dis = distance(vector, vector1);
//            if(dis > distance_max)
//                distance_max=dis;
//        }
//        return distance_max;
//    }
    private double[] getCenter(Map<String, double[]> node_vector) {
        String key = (String) node_vector.keySet().toArray()[0];
        double[] vector = node_vector.get(key);
        double[] vector_sum = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            for (Map.Entry<String, double[]> entry1 : node_vector.entrySet()) {
                vector = entry1.getValue();
                vector_sum[i] = vector_sum[i] + vector[i];
            }
        }
        double[] vector_center = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            vector_center[i] = vector_sum[i] / node_vector.size();
        }

        return vector_center;
    }

    private ArrayList<Map<String, double[]>> splitGroups(ArrayList<Map<String, double[]>> group_list) {
        ArrayList<Map<String, double[]>> group_list_out = new ArrayList();
        Map<String, double[]> out1 = new LinkedHashMap();
        Map<String, double[]> out2 = new LinkedHashMap();
        // find max distance
        double max_distance = 0;
        int position = -1;
        int num_group = -1;
        int count = 0;
        for (Map<String, double[]> group : group_list) {
            for (int i = 0; i < group.size() - 1; i++) {
                String key_cur = (String) group.keySet().toArray()[i];
                double[] vector_cur = group.get(key_cur);
                String key_next = (String) group.keySet().toArray()[i + 1];
                double[] vector_next = group.get(key_next);
                double dis = distance(vector_cur, vector_next);
                if (dis > max_distance) {
                    max_distance = dis;
                    position = i;
                    num_group = count;
                }
            }
            count++;
        }

        if (num_group >= 0) {
            // split 2 groups
            Map<String, double[]> group = group_list.get(num_group);

            for (int i = 0; i <= position; i++) {
                String key = (String) group.keySet().toArray()[i];
                double[] vector = group.get(key);
                out1.put(key, vector);
            }
            for (int i = position + 1; i < group.size(); i++) {
                String key = (String) group.keySet().toArray()[i];
                double[] vector = group.get(key);
                out2.put(key, vector);
            }

            group_list_out = (ArrayList<Map<String, double[]>>) group_list.clone();
            group_list_out.remove(num_group);
            group_list_out.add(out1);
            group_list_out.add(out2);
        }
        return group_list_out;
    }

    private double diameter_Group(Map<String, double[]> group) {
        double[] center = getCenter(group);
        double sum = 0;

        for (Map.Entry<String, double[]> entry : group.entrySet()) {
            double[] vector = entry.getValue();
            double dis = distance(vector, center);
            sum = sum + dis;
        }
        return sum;
    }

    private double sum_diameters_groups(ArrayList<Map<String, double[]>> group_list) {
        double sum_diameters = 0;
        for (Map<String, double[]> group : group_list) {
            sum_diameters = sum_diameters + diameter_Group(group);
        }
        return sum_diameters;
    }

    private Map<String, Map<String, Integer>> getTermsFromFile(ArrayList<String[]> list) {
        Map<String, Map<String, Integer>> string_line_words_count = new HashMap();
        for (String[] line : list) {
//            String[] words = line[0].toLowerCase().split("[\\s+,!@#$%^&\\*()-_+=<>\\?/\\.\\\\|{}:]");
            String[] words = line[0].toLowerCase().split("[\\s+\\W+_]");
            Map<String, Integer> line_words_count = new HashMap();
            for (String term : words) {
//                if(!stop_words.contains(term) && term.length() >= 3 && !term.matches("\\d+") && !term.matches("^[0-9abcdefABCDEF:-]+$")) {
                if (term.length() >= 3) {
                    line_words_count.merge(term, 1, Integer::sum);
                }
            }
            String str = line[0] + ", " + line[1] + ", " + line[2] + ", " + line[3];
            string_line_words_count.put(str.trim(), line_words_count);
        }
        return string_line_words_count;
    }

    private Map<String, ArrayList<String[]>> split_Groups_from_images(ArrayList<String[]> list) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        for (String[] mas : list) {
            if (result.get(mas[2]) != null) {
                result.get(mas[2]).add(mas);
            } else {
                ArrayList<String[]> tmp = new ArrayList();
                tmp.add(mas);
                result.put(mas[2], tmp);
            }
        }
        return result;
    }

    private Map<String, double[]> vectorization(ArrayList<String[]> list) {
        Map<String, Map<String, Integer>> string_line_words_count = getTermsFromFile(list);
        Map<String, Double> words_count_line = new HashMap();
        Map<String, Integer> words_position = new HashMap();
        Map<String, Double> words_in_data = new HashMap();
        double all_words_in_data = 0;
        int position = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : string_line_words_count.entrySet()) {
            Map<String, Integer> val = entry.getValue();
            for (Map.Entry<String, Integer> entry1 : val.entrySet()) {
                if (words_count_line.get(entry1.getKey()) != null) {
                    words_count_line.put(entry1.getKey(), words_count_line.get(entry1.getKey()) + 1);
                    words_in_data.put(entry1.getKey(), words_in_data.get(entry1.getKey()) + entry1.getValue());
                } else {
                    words_count_line.put(entry1.getKey(), 1.);
                    words_in_data.put(entry1.getKey(), Double.valueOf(entry1.getValue()));
                    words_position.put(entry1.getKey(), position);
                    position++;
                }
                all_words_in_data++;
            }
        }

        Map<String, double[]> words_vector_tfidf = new HashMap();
        for (Map.Entry<String, Map<String, Integer>> entry : string_line_words_count.entrySet()) {
            String str = entry.getKey();
            Map<String, Integer> line_words_count = entry.getValue();
            double[] vector = new double[words_position.size()];
            for (Map.Entry<String, Integer> entry1 : line_words_count.entrySet()) {
                String term = entry1.getKey();
                double count = entry1.getValue();
                double tf = count / line_words_count.size();
                double df = Math.pow(10, words_count_line.get(term) / list.size()) - 1;
                double tf_df = tf * df;
                if (words_position.get(term) != null) {
                    vector[words_position.get(term)] = tf_df;
                }
            }
            words_vector_tfidf.put(str, vector);
        }
        return words_vector_tfidf;
    }

    private ArrayList<String[]> getNodesInfo(Map INFORMATION) {
        ArrayList<String[]> list_image = new ArrayList();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) INFORMATION).entrySet()) {
            String area_name = area.getKey();
            Map<String, Map> val = area.getValue();
            Map<String, Map> nodes_information = val.get("nodes_information");
            if (nodes_information != null) {
                for (Map.Entry<String, Map> it : nodes_information.entrySet()) {
                    String node = it.getKey();
                    Map val1 = it.getValue();
                    if (val1 != null && val1.get("general") != null) {
                        String sysDescription = (String) ((Map) val1.get("general")).get("sysDescription");
                        String sysname = (String) ((Map) val1.get("general")).get("sysname");
                        String model = (String) ((Map) val1.get("general")).get("model");
                        String image = (String) val1.get("image");
                        String image_auto = (String) val1.get("image_auto");
                        String str = "";
                        if (sysDescription != null) {
                            str = sysDescription;
                        }
                        if (sysname != null) {
                            str = str + " " + sysname;
                        }
                        if (model != null) {
                            str = str + " " + model;
                        }
                        if (image == null || (image_auto != null && image_auto.equals("yes"))) {
                            image = "images/default.png";
                        }
                        if (!str.isEmpty()) {
                            String[] tmp = new String[4];
                            tmp[0] = str;
                            tmp[1] = node;
                            tmp[2] = image;
                            tmp[3] = area_name;
                            list_image.add(tmp);
                        }
                    }
                }
            }
        }
        return list_image;
    }

    public ArrayList<String> neighbors_node(Map INFO, String area, String node) {
        ArrayList<String> result = new ArrayList();
        if (INFO.get(area) != null && ((Map) INFO.get(area)).get("links") != null) {
            for (String[] link : (ArrayList<String[]>) ((Map) INFO.get(area)).get("links")) {
                if (link[0].equals(node)) {
                    result.add(link[3]);
                }
                if (link[3].equals(node)) {
                    result.add(link[0]);
                }
            }
        }
        return result;
    }

    private double distance(double[] v1, double[] v2) {
//        double distance = Double.MAX_VALUE;
        double distance = 0;
        if (v1 != null && v2 != null) {
            // check not null vector
//            boolean v1_null = true;
//            for(double d : v1) {
//                if(d != 0) {
//                    v1_null = false;
//                    break;
//                }
//            }
//            boolean v2_null = true;
//            for(double d : v2) {
//                if(d != 0) {
//                    v2_null = false;
//                    break;
//                }
//            }
//            if(!v1_null || !v2_null) {
            for (int i = 0; i < v1.length; i++) {
                distance = distance + Math.pow(Math.abs(v1[i] - v2[i]), 2);
//                distance = distance + Math.abs(v1[i]-v2[i]);
            }
            distance = Math.sqrt(distance);
//            } else
//                distance = 0;
        } else {
            distance = 20000000;
        }
        return distance;
    }

    private Map<String, double[]> prima_algoritm(Map<String, double[]> node_vector) {
        Map<String, double[]> base_node = new HashMap();
        Map<String, double[]> select = new LinkedHashMap();
        Map<String, double[]> unselect = new LinkedHashMap();
        int i = 0;
        for (Map.Entry<String, double[]> entry : node_vector.entrySet()) {
            if (i == 0) {
                base_node.put(entry.getKey(), entry.getValue());
                select.put(entry.getKey(), entry.getValue());
            } else {
                unselect.put(entry.getKey(), entry.getValue());
            }
            i++;
        }

        i = 0;
        while (true) {
            ArrayList out = selectVertex(base_node, select, unselect);
            base_node = (Map<String, double[]>) out.get(0);
            select = (Map<String, double[]>) out.get(1);
            unselect = (Map<String, double[]>) out.get(2);
//            if(unselect.size() % 100 == 0)
//                System.out.println(unselect.size());
            if (unselect != null && unselect.isEmpty()) {
                break;
            }
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            i++;
        }
        return select;
    }

    private ArrayList selectVertex(Map<String, double[]> base_node, Map<String, double[]> select, Map<String, double[]> unselect) {
        double min_distance = Double.MAX_VALUE;
        String min_key = "";
        String base_key = (String) base_node.keySet().toArray()[0];
        double[] base_vector = base_node.get(base_key);
        for (Map.Entry<String, double[]> entry : unselect.entrySet()) {
            double dis = distance(entry.getValue(), base_vector);
            if (dis < min_distance) {
                min_distance = dis;
                min_key = entry.getKey();
            }
        }
        Map<String, double[]> base_node_new = new HashMap();
        base_node_new.put(min_key, unselect.get(min_key));
        select.put(min_key, unselect.get(min_key));
        unselect.remove(min_key);
        ArrayList result = new ArrayList();
        result.add(base_node_new);
        result.add(select);
        result.add(unselect);
        return result;
    }

    public int calc_node_link(Map INFO, String area, String node) {
        int result = -1;
        if (((Map) INFO.get(area)).get("nodes_information") != null
                && ((Map) ((Map) INFO.get(area)).get("nodes_information")).get(node) != null) {
            result = 0;
            if (((Map) INFO.get(area)).get("links") != null) {
                ArrayList<ArrayList<String>> links = (ArrayList) ((Map) INFO.get(area)).get("links");
                for (ArrayList<String> link : links) {
                    if (link.get(0).equals(node) || link.get(3).equals(node)) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    public Map deleteNode(Map INFO, String area, String node) {
        if (INFO.get(area) != null
                && ((Map) INFO.get(area)).get("nodes_information") != null
                && ((Map) ((Map) INFO.get(area)).get("nodes_information")).get(node) != null) {
            // delete node
            logger.Println("Delete node: " + node + " from area: " + area, logger.DEBUG);
            ((Map) ((Map) INFO.get(area)).get("nodes_information")).remove(node);

            // delete node from node_protocol_accounts
            if (((Map) INFO.get(area)).get("node_protocol_accounts") != null
                    && ((Map) ((Map) INFO.get(area)).get("node_protocol_accounts")).get(node) != null) {
                ((Map) ((Map) INFO.get(area)).get("node_protocol_accounts")).remove(node);
            }

            // delete links from node
            ArrayList<String[]> neighbors_iface_list = new ArrayList();
            ArrayList<ArrayList<String>> links_new = new ArrayList();
//            ArrayList<ArrayList<String>> links_delete = new ArrayList();
            if (((Map) INFO.get(area)).get("links") != null) {
                ArrayList<ArrayList<String>> links = (ArrayList) ((Map) INFO.get(area)).get("links");
                for (ArrayList<String> link : links) {
                    if (link.get(0).equals(node) || link.get(3).equals(node)) {
                        logger.Println("Delete link: " + link.get(0) + ", " + link.get(2) + " <---> " + link.get(3) + ", " + link.get(5), logger.DEBUG);
//                        links_delete.add(link);
                        String[] neighbors_iface = new String[3];
                        if (link.get(0).equals(node)) {
                            neighbors_iface[0] = link.get(3);
                            neighbors_iface[1] = link.get(4);
                            neighbors_iface[2] = link.get(5);
                        }
                        if (link.get(3).equals(node)) {
                            neighbors_iface[0] = link.get(0);
                            neighbors_iface[1] = link.get(1);
                            neighbors_iface[2] = link.get(2);
                        }
                        neighbors_iface_list.add(neighbors_iface);
                    } else {
                        links_new.add(link);
                    }
                }
            }
            ((Map) INFO.get(area)).put("links", links_new);

            // delete mac_ip_port from node
            ArrayList<String[]> mip_delete = new ArrayList();
            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("mac_ip_port") != null &&
                    !((ArrayList) ((Map) INFO.get(area)).get("mac_ip_port")).isEmpty()) {

                if (((ArrayList) ((Map) INFO.get(area)).get("mac_ip_port")).get(0) instanceof String[]) {
                    for (String[] mip : (ArrayList<String[]>) ((Map) INFO.get(area)).get("mac_ip_port")) {
                        if (mip.length >= 5 && mip[0] != null && mip[1] != null && mip[2] != null && mip[3] != null && mip[4] != null) {
                            if (mip[2].equals(node)) {
                                logger.Println("Delete mac_ip_port: " + mip[0] + ", " + mip[1] + ", " + mip[2] + ", " + mip[3] + ", " + mip[4], logger.DEBUG);
                                mip_delete.add(mip);
                            }
                        }
                    }
                }
                if (((ArrayList) ((Map) INFO.get(area)).get("mac_ip_port")).get(0) instanceof ArrayList) {
                    for (ArrayList<String> mip : (ArrayList<ArrayList<String>>) ((Map) INFO.get(area)).get("mac_ip_port")) {
                        if (mip.get(2) != null && mip.get(2).equals(node)) {
                            String[] mas = new String[mip.size()];
                            int i = 0;
                            for (String it : mip) {
                                mas[i] = it;
                                i += 1;
                            }
                            logger.Println("Delete mac_ip_port: " + mas[0] + ", " + mas[1] + ", " + mas[2] + ", " + mas[3] + ", " + mas[4], logger.DEBUG);
                            mip_delete.add(mas);
                        }
                    }
                }

                // correct INFO
                ArrayList<String[]> mip_new = new ArrayList();
                if (((ArrayList) ((Map) INFO.get(area)).get("mac_ip_port")).get(0) instanceof String[]) {
                    for (String[] mip : (ArrayList<String[]>) ((Map) INFO.get(area)).get("mac_ip_port")) {
                        if (mip.length >= 5 && mip[0] != null && mip[1] != null && mip[2] != null && mip[3] != null && mip[4] != null) {
                            boolean found = false;
                            for (String[] mip_del : mip_delete) {
                                if (mip[0].equals(mip_del[0])
                                        && mip[1].equals(mip_del[1])
                                        && mip[2].equals(mip_del[2])
                                        && mip[3].equals(mip_del[3])
                                        && mip[4].equals(mip_del[4])) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                mip_new.add(mip);
                            }
                        }
                    }
                }
                if (((ArrayList) ((Map) INFO.get(area)).get("mac_ip_port")).get(0) instanceof ArrayList) {
                    for (ArrayList<String> mip : (ArrayList<ArrayList<String>>) ((Map) INFO.get(area)).get("mac_ip_port")) {
                        boolean found = false;
                        for (String[] mip_del : mip_delete) {
                            if (mip.get(0).equals(mip_del[0])
                                    && mip.get(1).equals(mip_del[1])
                                    && mip.get(2).equals(mip_del[2])
                                    && mip.get(3).equals(mip_del[3])
                                    && mip.get(4).equals(mip_del[4])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String[] mas = new String[mip.size()];
                            int i = 0;
                            for (String it : mip) {
                                mas[i] = it;
                                i += 1;
                            }
                            mip_new.add(mas);
                        }
                    }
                }
                ((Map) INFO.get(area)).put("mac_ip_port", mip_new);
            }

            // adding mac_ip_port to parent node port
            ArrayList<String[]> mip_adding = new ArrayList();
            int max_count = 0;
            String[] parent_node_iface = new String[3];
            if (neighbors_iface_list.size() > 1 && new File(Neb.debug_folder + "/arp_mac_table_" + area).exists()) {
//                Map<String, Map<String, Integer>> node_iface_count = new HashMap();
                Map<String, Integer> node_mac_count = new HashMap();
                ArrayList<String> line = readFileToList(Neb.debug_folder + "/arp_mac_table_" + area);
                for (String it : line) {
                    String[] mas = it.split(",");
                    for (int i = 0; i < mas.length; i++) {
                        mas[i] = mas[i].trim();
                    }
                    if (node_mac_count.get(mas[0]) == null) {
                        node_mac_count.put(mas[0], 1);
                    } else {
                        int count = node_mac_count.get(mas[0]);
                        count++;
                        node_mac_count.put(mas[0], count);
                    }
                }

                for (String[] neighbors_iface : neighbors_iface_list) {
                    if (node_mac_count.get(neighbors_iface[0]) != null) {
                        int count = node_mac_count.get(neighbors_iface[0]);
                        if (count > max_count) {
                            max_count = count;
                            parent_node_iface[0] = neighbors_iface[0];
                            parent_node_iface[1] = neighbors_iface[1];
                            parent_node_iface[2] = neighbors_iface[2];
                        }
                    }
                }
            } else if (neighbors_iface_list.size() == 1) {
                parent_node_iface[0] = neighbors_iface_list.get(0)[0];
                parent_node_iface[1] = neighbors_iface_list.get(0)[1];
                parent_node_iface[2] = neighbors_iface_list.get(0)[2];
            }

            for (String[] mip : mip_delete) {
                String[] mas = new String[6];
                mas[0] = mip[0];
                mas[1] = mip[1];
                mas[2] = parent_node_iface[0];
                mas[3] = parent_node_iface[1];
                mas[4] = parent_node_iface[2];
                mas[5] = "";
                mip_adding.add(mas);
                logger.Println("Add corrected mac_ip_port: " + mas[0] + ", " + mas[1] + ", " + mas[2] + ", " + mas[3] + ", " + mas[4], logger.DEBUG);
            }
            // correct INFO
            ArrayList<String[]> mip = (ArrayList<String[]>) ((Map) INFO.get(area)).get("mac_ip_port");
            mip.addAll(mip_adding);
            ((Map) INFO.get(area)).put("mac_ip_port", mip);
        }
        return INFO;
    }

    public Map restoreNode(Map INFO, String area, String node) {
        Map nodes_delete_info = utils.readJSONFile(Neb.nodes_delete);
        if (nodes_delete_info.get(area) != null && ((Map) nodes_delete_info.get(area)).get(node) != null) {
            //restore node
            Map node_info = (Map) ((Map) ((Map) nodes_delete_info.get(area)).get(node)).get("nodes_information");
            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("nodes_information") != null) {
                Map nodes_map = (Map) ((Map) INFO.get(area)).get("nodes_information");
                nodes_map.put(node, node_info);
            }
            // restore links
            ArrayList<ArrayList<String>> links_delete = (ArrayList) ((Map) ((Map) nodes_delete_info.get(area)).get(node)).get("links_delete");
            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("links") != null) {
                ArrayList<ArrayList<String>> links = (ArrayList) ((Map) INFO.get(area)).get("links");
                links.addAll(links_delete);
            }
            // correct mac_ip_port
            ArrayList<ArrayList<String>> mac_ip_port_adding = (ArrayList) ((Map) ((Map) nodes_delete_info.get(area)).get(node)).get("mac_ip_port_adding");
            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("mac_ip_port") != null) {
                ArrayList<ArrayList<String>> mac_ip_port = (ArrayList) ((Map) INFO.get(area)).get("mac_ip_port");
                ArrayList<ArrayList<String>> mac_ip_port_new = new ArrayList();
                for (ArrayList<String> it : mac_ip_port) {
                    boolean found = false;
                    for (ArrayList<String> it1 : mac_ip_port_adding) {
                        if (it.get(0).equals(it1.get(0))
                                && it.get(1).equals(it1.get(1))
                                && it.get(2).equals(it1.get(2))
                                && it.get(3).equals(it1.get(3))
                                && it.get(4).equals(it1.get(4))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        mac_ip_port_new.add(it);
                    }
                }
                ((Map) INFO.get(area)).put("mac_ip_port", mac_ip_port_new);
            }

            ArrayList<ArrayList<String>> mac_ip_port_delete = (ArrayList) ((Map) ((Map) nodes_delete_info.get(area)).get(node)).get("mac_ip_port_delete");
            if (INFO.get(area) != null && ((Map) INFO.get(area)).get("mac_ip_port") != null) {
                ArrayList<ArrayList<String>> mac_ip_port = (ArrayList) ((Map) INFO.get(area)).get("mac_ip_port");
                mac_ip_port.addAll(mac_ip_port_delete);
            }
        }
        return INFO;
    }

    public Map getInfo_sum(Map<String, Map> INFO, Map<String, Map> nodes_delete_info) {
        Map<String, Map> INFO_sum = new HashMap();
        for (Map.Entry<String, Map> entry : INFO.entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            INFO_sum.put(area, val);
        }
        for (Map.Entry<String, Map> entry : nodes_delete_info.entrySet()) {
            String area_delete = entry.getKey();
            Map val = entry.getValue();
            for (Map.Entry<String, Map> entry1 : ((Map<String, Map>) val).entrySet()) {
                String node_delete = entry1.getKey();
                Map nodes_delete_information = (Map) entry1.getValue().get("nodes_information");
                if (nodes_delete_information != null && INFO_sum.get(area_delete) != null
                        && INFO_sum.get(area_delete).get("nodes_information") != null
                        && ((Map) INFO_sum.get(area_delete).get("nodes_information")).get(node_delete) == null) {
                    ((Map) INFO_sum.get(area_delete).get("nodes_information")).put(node_delete, nodes_delete_information);
                }
            }
        }
        return INFO_sum;
    }

    public String getNodesImage(Map INFORMATION, String area, String node) {
        return (String) getKey("/" + area + "/nodes_information/" + node + "/image", INFORMATION);
    }

    public boolean setNodesImage(Map INFORMATION, String area, String node, String image) {
        return setKey("/" + area + "/nodes_information/" + node + "/image", image, INFORMATION);
    }

    public String colon_to_shablon(String str) {
        return str.replace(":", "{rep}");
    }

    public String shablon_to_colon(String str) {
        return str.replace("{rep}", ":");
    }

    public void get_passwd() {
        Console console = System.console();
        if (console != null) {
            char[] passwordArray = console.readPassword("Enter master key: ");
            ru.kos.neb.neb_lib.Utils.master_key = new String(passwordArray);
        } else {
            System.out.println("Enter master key: ");
            Scanner keyboard_input = new Scanner(System.in);
            String passwd = keyboard_input.nextLine();
            ru.kos.neb.neb_lib.Utils.master_key = passwd;
        }

    }

    public boolean check_master_key(Map cfg, String master_key) {
        Map<String, Map> areas = (Map<String, Map>) Neb.cfg.get("areas");
        if (areas != null && !areas.isEmpty()) {
            for (Map.Entry<String, Map> area : areas.entrySet()) {
                ArrayList<String> community_list = (ArrayList<String>) area.getValue().get("snmp_community");
                for (String community : community_list) {
                    if (community.startsWith("AES:")) {
                        if (Neb.neb_lib_utils.decrypt(master_key, community) != null) {
                            return true;
                        }
                    }
                }

                ArrayList<ArrayList<String>> cli_accounts = (ArrayList<ArrayList<String>>) area.getValue().get("cli_accounts");
                if (cli_accounts != null) {
                    for (ArrayList<String> cli_account : cli_accounts) {
                        if (cli_account.get(1) != null) {
                            if (cli_account.get(1).startsWith("AES:")) {
                                if (Neb.neb_lib_utils.decrypt(master_key, cli_account.get(1)) != null) {
                                    return true;
                                }
                            }
                        }
                        if (cli_account.size() > 2) {
                            if (cli_account.get(2) != null) {
                                if (cli_account.get(2).startsWith("AES:")) {
                                    if (Neb.neb_lib_utils.decrypt(master_key, cli_account.get(2)) != null) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Map> users = (Map<String, Map>) Neb.cfg.get("users");
        if (users != null && !users.isEmpty()) {
            for (Map.Entry<String, Map> user : users.entrySet()) {
                Map val = user.getValue();
                if (val.get("passwd") != null) {
                    if (((String) val.get("passwd")).startsWith("AES:")) {
                        if (Neb.neb_lib_utils.decrypt(master_key, (String) val.get("passwd")) != null) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;

    }

    public boolean is_not_blank_cfg(Map cfg) {
        boolean snmp_ok = false;
        boolean networks_ok = false;
        Map<String, Map> areas = (Map<String, Map>) Neb.cfg.get("areas");
        if (areas != null && !areas.isEmpty()) {
            for (Map.Entry<String, Map> area : areas.entrySet()) {
                ArrayList<String> community_list = (ArrayList<String>) area.getValue().get("snmp_community");
                if (!community_list.isEmpty()) {
                    snmp_ok = true;
                }
                ArrayList<String> networks = (ArrayList<String>) area.getValue().get("networks");
                if (!networks.isEmpty()) {
                    networks_ok = true;
                }
            }
        }
        return snmp_ok && networks_ok;
    }

    public boolean is_need_master_key(Map cfg) {
        Map<String, Map> areas = (Map<String, Map>) Neb.cfg.get("areas");
        if (areas != null && !areas.isEmpty()) {
            for (Map.Entry<String, Map> area : areas.entrySet()) {
                ArrayList<String> community_list = (ArrayList<String>) area.getValue().get("snmp_community");
                for (String community : community_list) {
                    if (community.startsWith("AES:")) {
                        return true;
                    }
                }

                ArrayList<ArrayList<String>> cli_accounts = (ArrayList<ArrayList<String>>) area.getValue().get("cli_accounts");
                if (cli_accounts != null) {
                    for (ArrayList<String> cli_account : cli_accounts) {
                        if (cli_account.get(1) != null) {
                            if (cli_account.get(1).startsWith("AES:")) {
                                return true;
                            }
                        }
                        if (cli_account.size() > 2) {
                            if (cli_account.get(2) != null) {
                                if (cli_account.get(2).startsWith("AES:")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<String, Map> users = (Map<String, Map>) Neb.cfg.get("users");
        if (users != null && !users.isEmpty()) {
            for (Map.Entry<String, Map> user : users.entrySet()) {
                Map val = user.getValue();
                if (val.get("passwd") != null) {
                    if (((String) val.get("passwd")).startsWith("AES:")) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    public void input_master_key() {
        if (is_need_master_key(Neb.cfg)) {
            // set master-key
            while (true) {
                utils.get_passwd();
                System.out.println("Post master key.");
                if (check_master_key(Neb.cfg, ru.kos.neb.neb_lib.Utils.master_key)) {
                    System.out.println("master key is ok.");
                    logger.Println("master key is ok.", logger.INFO);
                    break;
                } else {
                    ru.kos.neb.neb_lib.Utils.master_key = "";
                    System.out.println("master key is ERR !!!");
                    logger.Println("master key is ERR !!!", logger.INFO);
                }
            }
        }
    }

    public Map<String, String> readSecretsFile(String secrets_file) {
        Map<String, String> result = new HashMap();
        File f_secrets_file = new File(secrets_file);
        if (f_secrets_file.exists()) {
            // read secrets file
            result = readJSONFile(secrets_file);
            Map<String, String> secrets_new = new HashMap();
            for (Map.Entry<String, String> entry : result.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                val = Neb.neb_lib_utils.decrypt(ru.kos.neb.neb_lib.Utils.master_key, val);
                secrets_new.put(key, val);
            }
            result = secrets_new;
        }
        return result;
    }

    public String hexStringToUTF8(String hex) {
        if (hex.matches("([0-9A-Fa-f]{2}[:-])+([0-9A-Fa-f]{2})")) {
            hex = hex.replace(":", "").replace("-", "");
            ByteBuffer buff = ByteBuffer.allocate(hex.length() / 2);
            for (int i = 0; i < hex.length(); i += 2) {
                buff.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
            }
            ((Buffer) buff).rewind();
//            buff.rewind();
            Charset cs = StandardCharsets.UTF_8;
            CharBuffer cb = cs.decode(buff);
            return cb.toString();
        } else {
            return hex;
        }
    }

    public Map<String, Map> transform_node_protocol_accounts(Map<String, Map> INFO) {
        for (Map.Entry<String, Map> entry : INFO.entrySet()) {
//            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, String[]> node_protocol_accounts = val.get("node_protocol_accounts");
            if (node_protocol_accounts != null && !node_protocol_accounts.isEmpty()) {
                Map<String, ArrayList<String[]>> node_protocol_accounts_new = new HashMap();
                for (Map.Entry<String, String[]> entry1 : node_protocol_accounts.entrySet()) {
                    String node = entry1.getKey();
                    String[] mas = entry1.getValue();
                    ArrayList<String[]> list_mas = new ArrayList();
                    list_mas.add(mas);
                    node_protocol_accounts_new.put(node, list_mas);
                }
                val.put("node_protocol_accounts", node_protocol_accounts_new);
            }

        }

        return INFO;
    }

    public Map<String, String> get_Mac_Ip(Map<String, Map> informationFromNodesAllAreas) {
        Map<String, ArrayList<String>> area_networks = new HashMap();
        Map<String, Map> areas = (Map<String, Map>) Neb.cfg.get("areas");
        if (areas != null) {
            for (Map.Entry<String, Map> entry : areas.entrySet()) {
                String area = entry.getKey();
                Map val = entry.getValue();
                ArrayList<String> networks = (ArrayList<String>) val.get("networks");
                if (networks != null && !networks.isEmpty())
                    area_networks.put(area, networks);
            }
        }


        Map<String, String> mac_ip = new HashMap();
        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
            if (nodes_information != null) {
                for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                    String node = entry.getKey();
                    Map<String, Map> node_information = entry.getValue();
                    ArrayList<String> ip_list = utils.getIpListFromNode(node_information);
                    ArrayList<String> mac_list = utils.getMACFromNode(node_information);
                    ArrayList<String> mac_list_new = new ArrayList();
                    for (String mac : mac_list) {
                        mac_list_new.add(mac.replaceAll("[:.-]", "").toLowerCase());
                    }
                    mac_list = mac_list_new;
//                    ArrayList<BigInteger> l = new ArrayList();
//                    for(String mac : mac_list) {
//                        BigInteger num = new BigInteger(mac, 16);
//                        l.add(num);
//                    }
//
//                    if(!l.isEmpty()) {
//                        BigInteger min = new BigInteger("ffffffffffff", 16);
//                        for(BigInteger it : l) {
//                            if(it.compareTo(min) == -1)
//                                min = it;
//                        }
//                        min = min.subtract(BigInteger.valueOf(1));
//                        String new_mac = min.toString(16);
//                        StringBuilder prefix = new StringBuilder();
//                        prefix.append("0".repeat(Math.max(0, 12 - new_mac.length())));
//                        new_mac = prefix+new_mac;
//                        mac_list.add(new_mac);
//                        String new_mac2 = new_mac.substring(0, new_mac.length()-2);
//                        mac_list.add(new_mac2+"00");
//                    }

                    if (!ip_list.isEmpty() && !mac_list.isEmpty()) {
                        for (String mac : mac_list) {
                            ArrayList<String> networks = area_networks.get(area_name);
                            if (networks != null) {
                                for (String net : networks) {
                                    String ip_find = null;
                                    for (String ip : ip_list) {
                                        if (utils.inside_Network(ip, net)) {
                                            ip_find = ip;
                                            break;
                                        }
                                    }
                                    if (ip_find != null) {
                                        mac_ip.put(mac, ip_find);
                                    } else {
                                        mac_ip.put(mac, ip_list.get(0));
                                    }
                                }
                            } else {
                                mac_ip.put(mac, ip_list.get(0));
                            }

                        }
                    }

                }
            }
        }
        return mac_ip;
    }

    public Map<String, Map<String, String>> get_Area_Ip_Mac(Map<String, Map> informationFromNodesAllAreas) {
        Map<String, Map<String, String>> area_ip_mac = new HashMap();
        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
            if (nodes_information != null) {
                for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
//                    String node = entry.getKey();
                    Map<String, Map> node_information = entry.getValue();
                    Map<String, String> ip_mac = utils.getIpMACFromNode(node_information);
                    if (area_ip_mac.get(area_name) != null) {
                        area_ip_mac.get(area_name).putAll(ip_mac);
                    } else {
                        area_ip_mac.put(area_name, ip_mac);
                    }

                }
            }
        }
        return area_ip_mac;
    }

    public Map<String, Map<String, Map<String, String>>> get_Area_Node_Ip_Mac(Map<String, Map> informationFromNodesAllAreas) {
        Map<String, Map<String, Map<String, String>>> area_node_ip_mac = new HashMap();
        for (Map.Entry<String, Map> area : informationFromNodesAllAreas.entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
            if (nodes_information != null) {
                Map<String, Map<String, String>> node_ip_mac = new HashMap();
                for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                    String node = entry.getKey();
                    Map<String, Map> node_information = entry.getValue();
                    Map<String, String> ip_mac = utils.getIpMACFromNode(node_information);
                    if (ip_mac != null && !ip_mac.isEmpty()) {
                        if (node_ip_mac.get(node) != null) {
                            node_ip_mac.get(node).putAll(ip_mac);
                        } else {
                            node_ip_mac.put(node, ip_mac);
                        }
                    }
                }
                area_node_ip_mac.put(area_name, node_ip_mac);
            }
        }
        return area_node_ip_mac;
    }

    public Map<String, ArrayList<String[]>> get_Mac_retry_scanning(ArrayList<ArrayList> node_community_version_oid_list) {
        Map<String, ArrayList<String[]>> result = new HashMap();

        WalkPool walkPool = new WalkPool();
        int iter = 1;
        while (true) {
            Map<String, ArrayList<String[]>> res = walkPool.getNodeMultiCommunityVersionOidNotBulk(node_community_version_oid_list, Neb.timeout_thread_mac, 161, Neb.timeout_mac_retry, Neb.retries_mac);
            logger.Println("###### Iter = " + iter, logger.DEBUG);
            logger.Println("------ Size = " + res.size(), logger.DEBUG);
            if (res.isEmpty()) break;
            ArrayList<ArrayList> node_community_version_oid_list_new = new ArrayList();
            for (ArrayList it : node_community_version_oid_list) {
                String node = (String) it.get(0);
                if (res.get(node) == null) {
                    node_community_version_oid_list_new.add(it);
//                    logger.Println("\tNot MAC address node - "+node, logger.DEBUG);
                }
            }
            node_community_version_oid_list = node_community_version_oid_list_new;
            result.putAll(res);
            iter += 1;
        }

        return result;
    }

    private void applyInfo(Map area_info, ArrayList<String[]> replace_nodes) {
        Map<String, Map> nodes_info = (Map<String, Map>) area_info.get("nodes_information");
        if (nodes_info != null) {
            // replace and remove duplicate nodes
            for (String[] replace_node : replace_nodes) {
                Map<String, Map> node_info = nodes_info.get(replace_node[0]);
                Map<String, Map> node_info1 = nodes_info.get(replace_node[1]);
                if (replace_node[1] != null) {
                    nodes_info.remove(replace_node[0]);
                    if (node_info1 != null && nodes_info.get(replace_node[1]) == null) {
                        nodes_info.put(replace_node[1], node_info1);
                    } else if (node_info != null && nodes_info.get(replace_node[1]) == null) {
                        nodes_info.put(replace_node[1], node_info);
                    }
                    logger.Println("Replace node: " + replace_node[0] + " to: " + replace_node[1], logger.DEBUG);
                } else {
                    nodes_info.remove(replace_node[0]);
                    logger.Println("Remove duplicate node: " + replace_node[0], logger.DEBUG);
                }
            }

            // replace links from replace_nodes
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
            ArrayList<ArrayList<String>> links_new = new ArrayList();
            for (ArrayList<String> link : links) {
                String node1 = null;
                String node2 = null;
                if (link.size() == 5) {
                    node1 = link.get(0);
                    node2 = link.get(2);
                } else {
                    node1 = link.get(0);
                    node2 = link.get(3);
                }
                if (node1 != null && node2 != null) {
                    String replace1 = null;
                    String replace2 = null;
                    boolean remove_link = false;
                    for (String[] replace_node : replace_nodes) {
                        if (replace_node[0].equals(node1)) {
                            if (replace_node[1] != null) {
                                replace1 = replace_node[1];
                            } else {
                                remove_link = true;
                            }
                            break;
                        }

                    }
                    for (String[] replace_node : replace_nodes) {
                        if (replace_node[0].equals(node2)) {
                            if (replace_node[1] != null) {
                                replace2 = replace_node[1];
                            } else {
                                remove_link = true;
                            }
                            break;
                        }
                    }

                    if (!remove_link) {
                        if (replace1 != null) {
                            if (link.size() == 5) {
                                logger.Println("Replace link: " + link.get(0) + " " + link.get(1) + " <---> " + link.get(2) + " " + link.get(3) + "   to:   " + replace1 + " " + link.get(1) + " <---> " + link.get(2) + " " + link.get(3), logger.DEBUG);
                                link.set(0, replace1);
                            } else {
                                logger.Println("Replace link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + "   to:   " + replace1 + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
                                link.set(0, replace1);
                            }
                        }
                        if (replace2 != null) {
                            if (link.size() == 5) {
                                logger.Println("Replace link: " + link.get(0) + " " + link.get(1) + " <---> " + link.get(2) + " " + link.get(3) + "   to:   " + link.get(0) + " " + link.get(1) + " <---> " + replace2 + " " + link.get(3), logger.DEBUG);
                                link.set(3, replace2);
                            } else {
                                logger.Println("Replace link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + "   to:   " + link.get(0) + " " + link.get(2) + " <---> " + replace2 + " " + link.get(5), logger.DEBUG);
                                link.set(3, replace2);
                            }
                        }
                        links_new.add(link);
                    } else {
                        if (link.size() == 5) {
                            logger.Println("Remove link: " + link.get(0) + " " + link.get(1) + " <---> " + link.get(2) + " " + link.get(3), logger.DEBUG);
                        } else {
                            logger.Println("Remove link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
                        }
                    }
                }
            }
            links = links_new;

            // remove duplicate links
            links = removeDuplicateLinks(links);

            // clear link. Remove link if node not exist.
            ArrayList<ArrayList<String>> links_new1 = new ArrayList();
            for (ArrayList<String> link : links) {
                String node1 = null;
                String node2 = null;
                if (link.size() == 5) {
                    node1 = link.get(0);
                    node2 = link.get(2);
                } else {
                    node1 = link.get(0);
                    node2 = link.get(3);
                }
                if (node1 != null && node2 != null) {
                    if (nodes_info.get(node1) != null && nodes_info.get(node2) != null) {
                        links_new1.add(link);
                    } else {
                        if (link.size() == 5) {
                            logger.Println("Remove Link(not exist node): " + node1 + " " + link.get(1) + " <---> " + node2 + " " + link.get(3), logger.DEBUG);
                        } else {
                            logger.Println("Remove Link(not exist node): " + node1 + " " + link.get(2) + " <---> " + node2 + " " + link.get(5), logger.DEBUG);
                        }
                    }
                }
            }
            links = links_new1;

            // replace mac_ip_port from replace_nodes
            ArrayList<String[]> mac_ip_port_new = new ArrayList();
            ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
            if (mac_ip_port != null) {
                for (String[] it : mac_ip_port) {
                    String node = it[2];
                    String replace = null;
                    for (String[] replace_node : replace_nodes) {
                        if (replace_node[0].equals(node)) {
                            if (replace_node[1] != null) {
                                replace = replace_node[1];
                            }
                            break;
                        }

                    }

                    String node_prev = it[2];
                    if (replace != null) {
                        it[2] = replace;
                    }
                    mac_ip_port_new.add(it);
                    if (replace != null) {
                        logger.Println("Replace mac_ip_port: " + node_prev + "   to:   " + it[2], logger.DEBUG);
                    }

                }
            }
            mac_ip_port = mac_ip_port_new;

            // delete client if ip addres equals with node ip address
            ArrayList<String[]> mac_ip_port_new1 = new ArrayList();
            for (String[] it : mac_ip_port) {
                if (!it[1].equals(it[2])) {
                    mac_ip_port_new1.add(it);
                } else {
                    logger.Println("Remove mac_ip_port: " + it[0] + "," + it[1] + "," + it[2] + "," + it[3] + "," + it[4], logger.DEBUG);
                }
            }
            mac_ip_port = mac_ip_port_new1;

            if (nodes_info != null && !nodes_info.isEmpty()) {
                area_info.put("nodes_information", nodes_info);
            }
            if (links != null && !links.isEmpty()) {
                area_info.put("links", links);
            }
            if (mac_ip_port != null && !mac_ip_port.isEmpty()) {
                area_info.put("mac_ip_port", mac_ip_port);
            }
        }

    }

//    private void applyInfo_RemoveDubleNodes(Map area_info, ArrayList<String[]> replace_nodes) {
//        Map<String, Map> nodes_info = (Map<String, Map>) area_info.get("nodes_information");
//        if (nodes_info != null && !replace_nodes.isEmpty()) {
//
//            // remove duplicate nodes
//            for (String[] replace_node : replace_nodes) {
//                nodes_info.remove(replace_node[0]);
//                logger.Println("Remove duplicate node: " + replace_node[0], logger.DEBUG);
//            }
//
//
//            // Adding links from duble nodes to main node
//            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) area_info.get("links");
//            for (ArrayList<String> link : links) {
//                String node1 = link.get(0);
//                String node2 = link.get(3);
//                for (String[] replace_node : replace_nodes) {
//                    if (replace_node[0].equals(node1)) {
//                        logger.Println("Replace link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + "   to:   " + replace_node[1] + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
//                        link.set(0, replace_node[1]);
//                        break;
//                    }
//
//                }
//                for (String[] replace_node : replace_nodes) {
//                    if (replace_node[0].equals(node2)) {
//                        logger.Println("Replace link: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5) + "   to:   " + link.get(0) + " " + link.get(2) + " <---> " + replace_node[1] + " " + link.get(5), logger.DEBUG);
//                        link.set(3, replace_node[1]);
//                        break;
//                    }
//
//                }
//            }
//
//            // remove duplicate links
//            links = removeDuplicateLinks(links);
//
//            // clear link. Remove link if node not exist.
//            ArrayList<ArrayList<String>> links_new1 = new ArrayList();
//            for (ArrayList<String> link : links) {
//                String node1 = link.get(0);
//                String node2 = link.get(3);
//                if (nodes_info.get(node1) != null && nodes_info.get(node2) != null) {
//                    links_new1.add(link);
//                } else {
//                    logger.Println("Remove Link(not exist node): " + node1 + " " + link.get(2) + " <---> " + node2 + " " + link.get(5), logger.DEBUG);
//                }
//            }
//            links = links_new1;
//
//            // replace mac_ip_port from replace_nodes
//            ArrayList<String[]> mac_ip_port = (ArrayList<String[]>) area_info.get("mac_ip_port");
//            if (mac_ip_port != null) {
//                for (String[] it : mac_ip_port) {
//                    String node = it[2];
//                    for (String[] replace_node : replace_nodes) {
//                        if (replace_node[0].equals(node)) {
//                            it[2] = replace_node[1];
//                            logger.Println("Replace mac_ip_port: " + node + "   to:   " + it[2], logger.DEBUG);
//                        }
//                    }
//                }
//            }
//
//            // delete client if ip addres equals with node ip address
//            if (mac_ip_port != null) {
//                ArrayList<String[]> mac_ip_port_new1 = new ArrayList();
//                for (String[] it : mac_ip_port) {
//                    if (!it[1].equals(it[2])) {
//                        mac_ip_port_new1.add(it);
//                    } else {
//                        logger.Println("Remove mac_ip_port: " + it[0] + "," + it[1] + "," + it[2] + "," + it[3] + "," + it[4], logger.DEBUG);
//                    }
//                }
//                mac_ip_port = mac_ip_port_new1;
//                mac_ip_port = removeDuplicateMacIpPort(mac_ip_port);
//            }
//
//            area_info.put("nodes_information", nodes_info);
//            area_info.put("links", links);
//            area_info.put("mac_ip_port", mac_ip_port);
//        }
//    }

    public Map<String, Map> getArea_Node_NodePriority(Map<String, Map> Info, Map<String, ArrayList<String>> area_networks) {
        Map<String, Map> area_Node_NodePriority = new HashMap();
        for (Map.Entry<String, Map> entry : Info.entrySet()) {
            String area_name = entry.getKey();
            Map area_info = entry.getValue();
            Map<String, String> node_NodePriority = new HashMap();
            Map<String, Map> nodes_information = (Map<String, Map>) area_info.get("nodes_information");
            if (nodes_information != null) {
                Map<String, Integer> node_priority = new HashMap();
                for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                    String node = entry1.getKey();
                    Map node_information = entry1.getValue();
                    ArrayList<String> ip_list = getIpListFromNode(node_information);
                    if (ip_list != null && !ip_list.isEmpty()) {
                        String main_node = node;
                        ArrayList<String> networks = area_networks.get(area_name);
                        if (networks != null) {
                            int pos_min = Integer.MAX_VALUE;
                            for (String ip : ip_list) {
                                int pos = 0;
                                boolean found = false;
                                for (String network : networks) {
                                    if (inside_Network(ip, network)) {
                                        found = true;
                                        break;
                                    }
                                    pos = pos + 1;
                                }
                                if (found) {
                                    if (pos < pos_min) {
                                        main_node = ip;
                                        pos_min = pos;
                                    }
                                }
                            }
                        }
                        node_NodePriority.put(node, main_node);
                    }
                }
            }
            area_Node_NodePriority.put(area_name, node_NodePriority);
        }
        return area_Node_NodePriority;
    }

    public String getPriorityNode(ArrayList<String> nodes_list, Map<String, Map> nodes_info, Map<String, String> node_NodePriority) {
        String main_node = null;

        if (!nodes_list.isEmpty()) {
            main_node = nodes_list.get(0);
            boolean find = false;
            for (String node : nodes_list) {
                if (node_NodePriority != null && node_NodePriority.get(node) != null) {
                    main_node = node_NodePriority.get(node);
                    find = true;
                    break;
                }

            }
            if (!find) {
                ArrayList<String> nodes_list_new = new ArrayList();
                for (String node : nodes_list) {
                    if(node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        nodes_list_new.add(node);
                    }
                }
                if(!nodes_list_new.isEmpty()) {
                    nodes_list = nodes_list_new;
                }

                int max_info = 0;
                for (String node : nodes_list) {
                    Map node_info = nodes_info.get(node);
                    if (node_info != null) {
                        Gson gson = new Gson();
                        String str = gson.toJson(node_info);
                        if (str.length() > max_info) {
                            max_info = str.length();
                            main_node = node;
                        }
                    }
                }
            }
        }
        return main_node;
    }

    public Map getTopology(Map<String, Map> Info) {
        Map<String, Map> result = new HashMap();
        for (Map.Entry<String, Map> entry0 : Info.entrySet()) {
            String area = entry0.getKey();
            Map val = entry0.getValue();

            Map<String, ArrayList> res = new HashMap();
            Map<String, Map> nodes_information = (Map) val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList) val.get("links");
            if (nodes_information != null && !nodes_information.isEmpty()) {
                for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                    String node = entry.getKey();
                    ArrayList<String[]> neighbours = new ArrayList();
                    if (links != null) {
                        for (ArrayList<String> link : links) {
                            if (link.get(0).equals(node)) {
                                String[] mas = new String[3];
                                mas[0] = link.get(3);
                                mas[1] = link.get(4);
                                mas[2] = link.get(5);
                                neighbours.add(mas);
                            }
                            if (link.get(3).equals(node)) {
                                String[] mas = new String[3];
                                mas[0] = link.get(0);
                                mas[1] = link.get(1);
                                mas[2] = link.get(2);
                                neighbours.add(mas);
                            }
                        }
                    }
                    res.put(node, neighbours);
                }
            }
            result.put(area, res);
        }

        return result;
    }

    public Map getConnectedNeighboursMac(Map<String, Map> topology, Map<String, ArrayList<String[]>> area_arp_mac_table) {
        Map<String, Map> area_node_id_mac_list = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : area_arp_mac_table.entrySet()) {
            String area = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, Map<String, Map<String, String>>> node_id_mac = new HashMap();
            for (String[] iter : val) {
                String node = iter[0];
                String id = iter[1];
//                String iface = iter[2];
//                String ip = iter[3];
                String mac = iter[4].replace(":", "");
                if (node_id_mac.get(node) != null) {
                    if (node_id_mac.get(node).get(id) != null) {
                        node_id_mac.get(node).get(id).put(mac, mac);
                    } else {
                        Map<String, String> map_tmp = new HashMap();
                        map_tmp.put(mac, mac);
                        node_id_mac.get(node).put(id, map_tmp);
                    }
                } else {
                    Map<String, Map<String, String>> id_mac = new HashMap();
                    if (id_mac.get(id) != null) {
                        id_mac.get(id).put(mac, mac);
                    } else {
                        Map<String, String> map_tmp = new HashMap();
                        map_tmp.put(mac, mac);
                        id_mac.put(id, map_tmp);
                    }
                    node_id_mac.put(node, id_mac);
                }
            }
            area_node_id_mac_list.put(area, node_id_mac);
        }

        Map<String, Map> area_node_mac_connected_neighbours_mac = new HashMap();
        for (Map.Entry<String, Map> entry : topology.entrySet()) {
            String area = entry.getKey();
            Map<String, ArrayList> val = (Map) entry.getValue();
            Map<String, Map> node_mac_connected_neighbours_mac = new HashMap();
            if (area_node_id_mac_list.get(area) != null) {
                for (Map.Entry<String, ArrayList> entry1 : val.entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<String[]> val1 = entry1.getValue();
                    Map<String, Map> mac_connected_neighbours_mac = new HashMap();
                    if (area_node_id_mac_list.get(area).get(node) != null) {
                        Map map_tmp = new HashMap((Map) area_node_id_mac_list.get(area).get(node));
                        if (!map_tmp.isEmpty()) {
                            mac_connected_neighbours_mac.put("connected", map_tmp);
                        }
                    }

                    Map map_tmp = new HashMap();
                    for (String[] iter : val1) {
                        String node_neighbours = iter[0];
                        String id_neighbours = iter[1];
                        String iface_neighbours = iter[2];

                        if (!id_neighbours.isEmpty() && area_node_id_mac_list.get(area).get(node_neighbours) != null &&
                                ((Map) area_node_id_mac_list.get(area).get(node_neighbours)).get(id_neighbours) != null) {
                            map_tmp.put(node_neighbours, ((Map) area_node_id_mac_list.get(area).get(node_neighbours)).get(id_neighbours));
                        }
                    }
                    if (!map_tmp.isEmpty()) {
                        mac_connected_neighbours_mac.put("neighbours", map_tmp);
                    }
                    if (!mac_connected_neighbours_mac.isEmpty()) {
                        node_mac_connected_neighbours_mac.put(node, mac_connected_neighbours_mac);
                    }
                }
            }
            if (!node_mac_connected_neighbours_mac.isEmpty()) {
                area_node_mac_connected_neighbours_mac.put(area, node_mac_connected_neighbours_mac);
            }
        }
        return area_node_mac_connected_neighbours_mac;
    }

    public Map appendMacFromArpMacTable(Map<String, Map> INFO, Map<String, ArrayList<String[]>> area_arp_mac_table) {
        for (Map.Entry<String, Map> entry : INFO.entrySet()) {
            String area = entry.getKey();
            Map<String, Map> area_info = entry.getValue();
            Map<String, Map> nodes_information = area_info.get("nodes_information");
            Map<String, String> node_mac = new HashMap();
            if (area_arp_mac_table.get(area) != null) {
                for (String[] iter : area_arp_mac_table.get(area)) {
                    String ip = iter[3];
                    String mac = iter[4];
                    node_mac.put(ip, mac);
                }
            }
            if (nodes_information != null) {
                for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                    String node = entry1.getKey();
                    Map<String, Map> node_info = entry1.getValue();
                    if (node_info.get("general") != null) {
                        String base_address = (String) node_info.get("general").get("base_address");
                        if (base_address == null) {
                            String mac = node_mac.get(node);
                            node_info.get("general").put("base_address", mac);
                        }
                    }
                }
            }
        }
        return INFO;
    }

    public boolean check_Link_through_node_mac_connected_neighbours_mac(
            ArrayList<String> link,
            Map<String, Map> node_mac_connected_neighbours_mac,
            Map<String, ArrayList<String>> node_mac) {
//        if(link.get(0).equals("10.13.14.1") && link.get(2).equals("ethernet1/4"))
//            System.out.println("1111");
        ArrayList<String> mac_list = node_mac.get(link.get(0));
        boolean find1 = false;
        boolean find2 = false;
        boolean is_mac1 = false;
        boolean is_mac2 = false;
        if (mac_list != null) {
            for (String mac : mac_list) {
                String node = link.get(3);
                String id_iface = link.get(4);
                if (node_mac_connected_neighbours_mac.get(node) != null)
                    is_mac1 = true;
                if (find_mac_from_node_mac_connected_neighbours_mac(mac, link.get(0), node, id_iface, node_mac_connected_neighbours_mac)) {
                    find1 = true;
                }
            }
        }
        mac_list = node_mac.get(link.get(3));
        if (mac_list != null) {
            for (String mac : mac_list) {
                String node = link.get(0);
                if (node_mac_connected_neighbours_mac.get(node) != null)
                    is_mac2 = true;
                String id_iface = link.get(1);
                if (find_mac_from_node_mac_connected_neighbours_mac(mac, link.get(3), node, id_iface, node_mac_connected_neighbours_mac)) {
                    find2 = true;
                }
            }
        }
        if (find1 && find2)
            return true;
        else if ((!find1 && !is_mac1) && find2)
            return true;
        else return find1 && (!find2 && !is_mac2);
    }

    private boolean find_mac_from_node_mac_connected_neighbours_mac(String mac, String owner_mac,
                                                                    String node, String id_iface,
                                                                    Map<String, Map> node_mac_connected_neighbours_mac) {
        if (node_mac_connected_neighbours_mac.get(node) != null) {
            Map<String, Map> val = node_mac_connected_neighbours_mac.get(node);
            Map<String, Map> connected = val.get("connected");
            Map<String, Map> neighbours = val.get("neighbours");
            if (connected == null && neighbours == null) {
                return true;
            }
            if (connected != null) {
//                if(connected.get(id_iface) == null) {
//                    return true;
//                }
                if (connected.get(id_iface) != null && connected.get(id_iface).get(mac) != null) {
                    return true;
                }
            }
            if (neighbours != null) {
                for (Map.Entry<String, Map> entry : neighbours.entrySet()) {
                    String node_neighbours = entry.getKey();
                    Map<String, String> val1 = entry.getValue();
                    if (neighbours.size() == 1 && owner_mac.equals(node_neighbours)) {
                        return true;
                    } else {
                        if (val1.get(mac) != null) {
                            return true;
                        }
                    }
                }
            }
        } else {
            return false;
        }
        return false;
    }

    public Map<String, Map> getAreaNodeMac(Map<String, Map> Info) {
        Map<String, Map> area_node_mac = new HashMap();
        for (Map.Entry<String, Map> entry : Info.entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, Map> nodes_information = val.get("nodes_information");
            if (nodes_information != null) {
                Map<String, ArrayList<String>> node_mac = new HashMap();
                for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                    String node = entry1.getKey();
                    Map<String, Map> node_info = entry1.getValue();
                    if (node_info.get("general") != null && node_info.get("general").get("base_address") != null) {
                        String mac = ((String) node_info.get("general").get("base_address")).replace(":", "");
                        if (mac != null && !mac.isEmpty()) {
                            if (node_mac.get(node) != null) {
                                if (!node_mac.get(node).contains(mac)) {
                                    node_mac.get(node).add(mac);
                                }
                            } else {
                                ArrayList<String> tmp_list = new ArrayList();
                                tmp_list.add(mac);
                                node_mac.put(node, tmp_list);
                            }
                        }
                    }
                    Map<String, Map> interfaces = node_info.get("interfaces");
                    if (interfaces != null) {
                        for (Map.Entry<String, Map> entry2 : interfaces.entrySet()) {
//                            String iface = entry2.getKey();
                            Map<String, String> iface_info = entry2.getValue();
                            if (iface_info.get("mac") != null) {
                                String mac = iface_info.get("mac").replace(":", "");
                                if (mac != null && !mac.isEmpty() && !mac.equals("000000000000")) {
                                    if (node_mac.get(node) != null) {
                                        if (!node_mac.get(node).contains(mac)) {
                                            node_mac.get(node).add(mac);
                                        }
                                    } else {
                                        ArrayList<String> tmp_list = new ArrayList();
                                        tmp_list.add(mac);
                                        node_mac.put(node, tmp_list);
                                    }
                                }
                            }
                        }
                    }

                }
                area_node_mac.put(area, node_mac);
            }
        }
        return area_node_mac;
    }

    public Map<String, Map> getAreaNodeIfaceVlans(Map<String, Map> Info, Map<String, Map<String, Map<String, String>>> area_node_ifaceid_ifacename) {
        Map<String, Map> area_node_ifacename_ifaceid = new HashMap();
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : area_node_ifaceid_ifacename.entrySet()) {
            String area = entry.getKey();
            Map<String, Map<String, String>> node_ifaceid_ifacename = entry.getValue();
            Map<String, Map> node_ifacename_ifaceid = new HashMap();
            for (Map.Entry<String, Map<String, String>> entry1 : node_ifaceid_ifacename.entrySet()) {
                String node = entry1.getKey();
                Map<String, String> ifaceid_ifacename = entry1.getValue();
                Map<String, String> ifacename_ifaceid = new HashMap();
                for (Map.Entry<String, String> entry2 : ifaceid_ifacename.entrySet()) {
                    ifacename_ifaceid.put(entry2.getValue(), entry2.getKey());
                }
                node_ifacename_ifaceid.put(node, ifacename_ifaceid);
            }
            area_node_ifacename_ifaceid.put(area, node_ifacename_ifaceid);
        }

        Map<String, Map> area_node_idiface_vlans = new HashMap();
        for (Map.Entry<String, Map> entry : Info.entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, Map> nodes_information = val.get("nodes_information");
            if (nodes_information != null) {
                Map<String, Map> node_idiface_vlans = new HashMap();
                for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                    String node = entry1.getKey();
                    Map<String, Map> interfaces = (Map) entry1.getValue().get("interfaces");
                    if (interfaces != null) {
                        Map<String, ArrayList> idiface_vlans = new HashMap();
                        for (Map.Entry<String, Map> entry2 : interfaces.entrySet()) {
                            String iface = entry2.getKey();
                            String idiface = null;
                            if (area_node_ifacename_ifaceid.get(area) != null &&
                                    area_node_ifacename_ifaceid.get(area).get(node) != null &&
                                    ((Map) area_node_ifacename_ifaceid.get(area).get(node)).get(iface) != null
                            ) {
                                idiface = (String) ((Map) area_node_ifacename_ifaceid.get(area).get(node)).get(iface);
                            }
                            if (idiface != null) {
                                Map<String, String> iface_info = entry2.getValue();
                                ArrayList<String> vlans = new ArrayList();
                                String access_vlan = iface_info.get("access_vlan");
                                if (access_vlan != null) {
                                    if (access_vlan.isEmpty())
                                        access_vlan = "1";
                                    vlans.add(access_vlan);
                                }
                                String trunk_vlan = iface_info.get("trunk_vlan");
                                if (trunk_vlan != null) {
                                    if (trunk_vlan.isEmpty())
                                        trunk_vlan = "1";
                                    String[] mas = trunk_vlan.split(",");
                                    vlans.addAll(new ArrayList<>(Arrays.asList(mas)));
                                }
                                if (!vlans.isEmpty())
                                    idiface_vlans.put(idiface, vlans);
                            }
                        }
                        if (!idiface_vlans.isEmpty())
                            node_idiface_vlans.put(node, idiface_vlans);
                    }
                }
                if (!node_idiface_vlans.isEmpty())
                    area_node_idiface_vlans.put(area, node_idiface_vlans);

            }
        }

        return area_node_idiface_vlans;
    }

    public Map<String, Map> getAreaNodeArpMacExist(Map<String, ArrayList<String[]>> area_arp_mac_table) {
        Map<String, Map> area_node_arpmac_exist = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : area_arp_mac_table.entrySet()) {
            String area = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, Boolean> node_arpmac_exist = new HashMap();
            for (String[] item : val) {
                String node = item[0];
                node_arpmac_exist.put(node, true);
            }
            area_node_arpmac_exist.put(area, node_arpmac_exist);
        }
        return area_node_arpmac_exist;
    }

    public Map<String, Map> filterNodes(Map<String, Map> Info, String filters_file) {
        Map<String, Map> filters = Neb.utils.readJSONFile(filters_file);
        if (filters != null && !filters.isEmpty()) {
            Map<String, ArrayList> include = filters.get("include");
            Map<String, ArrayList> exclude = filters.get("exclude");

            Map area_node_info_brief = getNodeInfoBrief(Info);

            for (Map.Entry<String, Map> entry : Info.entrySet()) {
                String area = entry.getKey();
                Map val = entry.getValue();
                Map<String, Map> nodes_information = (Map) val.get("nodes_information");
                ArrayList<ArrayList<String>> links = (ArrayList) val.get("links");
                ArrayList<String[]> mac_ip_port = (ArrayList) val.get("mac_ip_port");
                // get node -> number_links
                Map<String, Integer> nodes_numLinks = new HashMap();
                if (links != null) {
                    for (ArrayList<String> link : links) {
                        if (nodes_numLinks.get(link.get(0)) != null) {
                            Integer numLinks = nodes_numLinks.get(link.get(0));
                            numLinks++;
                            nodes_numLinks.put(link.get(0), numLinks);
                        } else {
                            nodes_numLinks.put(link.get(0), 1);
                        }
                        if (nodes_numLinks.get(link.get(3)) != null) {
                            Integer numLinks = nodes_numLinks.get(link.get(3));
                            numLinks++;
                            nodes_numLinks.put(link.get(3), numLinks);
                        } else {
                            nodes_numLinks.put(link.get(3), 1);
                        }
                    }
                }
                // get nodes_clients
                Map<String, String> nodes_clients = new HashMap();
                if (mac_ip_port != null) {
                    for (String[] mip : mac_ip_port) {
                        nodes_clients.put(mip[2], mip[2]);
                    }
                }

                ArrayList<String> node_exclude_list = new ArrayList();
                if (nodes_information != null) {
                    for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                        String node = entry1.getKey();
//                        if (node.equals("(00:90:fa:a5:75:70)"))
//                            System.out.println("11111");
                        Map<String, Map> node_info = entry1.getValue();
                        if (node_info != null &&
                                (nodes_numLinks.get(node) == null || nodes_numLinks.get(node) == 1) &&
                                nodes_clients.get(node) == null) {
                            // check include
                            if (include != null) {
                                boolean node_include = false;
                                for (Map.Entry<String, ArrayList> entry2 : include.entrySet()) {
                                    String key = entry2.getKey();
                                    ArrayList<String> val_list = entry2.getValue();
                                    if (key.equals("key")) {
                                        boolean found_shablon = false;
                                        for (String shablon : val_list) {
                                            if (node.toLowerCase().matches(shablon.toLowerCase())) {
                                                found_shablon = true;
                                                break;
                                            }
                                        }
                                        if (found_shablon) {
                                            node_include = true;
                                            break;
                                        }
                                    } else {
                                        Object key_val = getKey(key, node_info);
                                        if (key_val instanceof String string) {
                                            boolean found_shablon = false;
                                            for (String shablon : val_list) {
                                                if (string.toLowerCase().matches(shablon.toLowerCase())) {
                                                    found_shablon = true;
                                                    break;
                                                }
                                            }
                                            if (found_shablon) {
                                                node_include = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (node_include) {
                                    continue;
                                }
                            }

                            // check exclude
                            if (exclude != null) {
                                for (Map.Entry<String, ArrayList> entry2 : exclude.entrySet()) {
                                    String key = entry2.getKey();
                                    ArrayList<String> val_list = entry2.getValue();
                                    if (key.equals("key")) {
                                        boolean found_shablon = false;
                                        for (String shablon : val_list) {
                                            if (node.toLowerCase().matches(shablon.toLowerCase())) {
                                                found_shablon = true;
                                                break;
                                            }
                                        }
                                        if (found_shablon) {
                                            node_exclude_list.add(node);
                                            break;
                                        }
                                    } else {
                                        Object key_val = getKey(key, node_info);
                                        if (key_val instanceof String string) {
                                            boolean found_shablon = false;
                                            for (String shablon : val_list) {
                                                if (string.toLowerCase().matches(shablon.toLowerCase())) {
                                                    found_shablon = true;
                                                    break;
                                                }
                                            }
                                            if (found_shablon) {
                                                node_exclude_list.add(node);
                                                break;
                                            }
                                        }
                                    }

                                }
                            }
                            // check empty
                            boolean find_key = false;
                            if (exclude != null) {
                                for (Map.Entry<String, ArrayList> entry2 : exclude.entrySet()) {
                                    String key = entry2.getKey();
                                    if (!key.equals("/general/base_address")) {
                                        Object key_val = getKey(key, node_info);
                                        if (key_val != null) {
                                            find_key = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!find_key) {
                                node_exclude_list.add(node);
                            }

                        }
                    }
                }

                if (!node_exclude_list.isEmpty()) {
                    // delete nodes from nodes_information
                    for (String node : node_exclude_list) {
                        if (nodes_information != null && nodes_information.get(node) != null) {
                            nodes_information.remove(node);
                            logger.Println("Filter node delete: " + node, logger.DEBUG);
                        }
                    }
                    // delete nodes from links
                    if (links != null) {
                        ArrayList<ArrayList<String>> links_new = new ArrayList();
                        for (ArrayList<String> link : links) {
                            boolean is_find = false;
                            for (String node : node_exclude_list) {
                                if (link.get(0).equals(node) || link.get(3).equals(node)) {
                                    is_find = true;
                                    break;
                                }
                            }
                            if (!is_find) {
                                links_new.add(link);
                            } else {
                                logger.Println("Filter link delete: " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), logger.DEBUG);
                            }
                        }
                        val.put("links", links_new);
                    }

                    // adding node info to mac_ip_port
                    Map node_info_brief = (Map) area_node_info_brief.get(area);
                    Map<String, String[]> mac_mip = new HashMap();
                    Map<String, String[]> ip_mip = new HashMap();
                    if (mac_ip_port != null) {
                        for (String[] mip : mac_ip_port) {
                            mac_mip.put(mip[0], mip);
                            ip_mip.put(mip[1], mip);
                        }
                    }
                    Map<String, ArrayList<String[]>> node_neightbour = new HashMap();
                    if (links != null) {
                        for (ArrayList<String> link : links) {
                            String[] mas1 = new String[3];
                            mas1[0] = link.get(3);
                            mas1[1] = link.get(4);
                            mas1[2] = link.get(5);
                            if (node_neightbour.get(link.get(0)) != null) {
                                node_neightbour.get(link.get(0)).add(mas1);
                            } else {
                                ArrayList<String[]> tmp_list = new ArrayList();
                                tmp_list.add(mas1);
                                node_neightbour.put(link.get(0), tmp_list);
                            }
                            String[] mas2 = new String[3];
                            mas2[0] = link.get(0);
                            mas2[1] = link.get(1);
                            mas2[2] = link.get(2);
                            if (node_neightbour.get(link.get(3)) != null) {
                                node_neightbour.get(link.get(3)).add(mas2);
                            } else {
                                ArrayList<String[]> tmp_list = new ArrayList();
                                tmp_list.add(mas2);
                                node_neightbour.put(link.get(3), tmp_list);
                            }
                        }
                    }
                    if (node_info_brief != null) {
                        for (String node : node_exclude_list) {
                            Map info_brief = (Map) ((Map) (Map) area_node_info_brief.get(area)).get(node);
                            if(info_brief != null) {
                                ArrayList<String> mac_list = (ArrayList) info_brief.get("mac_list");
                                ArrayList<String> ip_list = (ArrayList) info_brief.get("ip_list");
                                if (node_neightbour.get(node) != null && node_neightbour.get(node).size() == 1) {
                                    if (ip_list != null) {
                                        for (String ip : ip_list) {
                                            if (ip_mip.get(ip) == null) {
                                                String[] mas = new String[6];
                                                if (mac_list != null && !mac_list.isEmpty()) {
                                                    mas[0] = mac_list.get(0).substring(0, 2)+":"+
                                                            mac_list.get(0).substring(2, 4)+":"+
                                                            mac_list.get(0).substring(4, 6)+":"+
                                                            mac_list.get(0).substring(6, 8)+":"+
                                                            mac_list.get(0).substring(8, 10)+":"+
                                                            mac_list.get(0).substring(10, 12);
                                                } else {
                                                    mas[0] = "unknown";
                                                }
                                                mas[1] = ip;
                                                mas[2] = node_neightbour.get(node).get(0)[0];
                                                mas[3] = node_neightbour.get(node).get(0)[1];
                                                mas[4] = node_neightbour.get(node).get(0)[2];
                                                mas[5] = "";
                                                mac_ip_port.add(mas);
                                            }
                                        }
                                    } else {
                                        if (mac_list != null) {
                                            for (String mac : mac_list) {
                                                if (mac_mip.get(mac) == null) {
                                                    String[] mas = new String[6];
                                                    mas[0] = mac.substring(0, 2)+":"+
                                                            mac.substring(2, 4)+":"+
                                                            mac.substring(4, 6)+":"+
                                                            mac.substring(6, 8)+":"+
                                                            mac.substring(8, 10)+":"+
                                                            mac.substring(10, 12);
                                                    if (ip_list != null && !ip_list.isEmpty()) {
                                                        mas[1] = ip_list.get(0);
                                                    } else {
                                                        mas[1] = "unknown";
                                                    }
                                                    mas[2] = node_neightbour.get(node).get(0)[0];
                                                    mas[3] = node_neightbour.get(node).get(0)[1];
                                                    mas[4] = node_neightbour.get(node).get(0)[2];
                                                    mas[5] = "";
                                                    mac_ip_port.add(mas);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return Info;
    }

    public Map<String, Map> imageNodes(Map<String, Map> Info, String image_file) {
        Map<String, String> shablon_image = Neb.utils.readJSONFile(image_file);

        if (shablon_image != null && !shablon_image.isEmpty()) {
            for (Map.Entry<String, Map> entry : Info.entrySet()) {
//                String area = entry.getKey();
                Map val = entry.getValue();
                Map<String, Map> nodes_information = (Map) val.get("nodes_information");
                if (nodes_information != null) {
                    for (Map.Entry<String, Map> entry1 : nodes_information.entrySet()) {
                        String node = entry1.getKey();
                        Map node_info = entry1.getValue();
                        if (node_info.get("image") == null) {
                            Map<String, Map> interfaces = (Map) node_info.get("interfaces");
                            if (interfaces != null) {
                                ArrayList<String> iface_list = new ArrayList();
                                for (Map.Entry<String, Map> entry_ifaces : interfaces.entrySet()) {
                                    iface_list.add(entry_ifaces.getKey());
                                }
                                Pattern pattern = Pattern.compile("[A-Za-z]+\\s*(\\d+)/\\d+/\\d+");
                                Map<String, Integer> slot_numiface = new HashMap();
                                for (String iface : iface_list) {
                                    Matcher matcher = pattern.matcher(iface);
                                    if (matcher.find()) {
                                        String slot_digit = matcher.group(1);
                                        if (slot_numiface.get(slot_digit) == null) {
                                            slot_numiface.put(slot_digit, 1);
                                        } else {
                                            int num = slot_numiface.get(slot_digit);
                                            num = num + 1;
                                            slot_numiface.put(slot_digit, num);
                                        }
                                    }
                                }
                                if (slot_numiface.size() > 1) {
                                    Map<String, Integer> slot_numiface_new = new HashMap();
                                    for (Map.Entry<String, Integer> entry_slot : slot_numiface.entrySet()) {
                                        String slot = entry_slot.getKey();
                                        int num = entry_slot.getValue();
                                        if (num > 20) {
                                            slot_numiface_new.put(slot, num);
                                        }
                                    }
                                    String image_stack = null;
                                    if (slot_numiface_new.size() == 2) {
                                        image_stack = "images/Cisco/Stack-2.png";
                                    } else if (slot_numiface_new.size() == 3) {
                                        image_stack = "images/Cisco/Stack-3.png";
                                    } else if (slot_numiface_new.size() >= 4) {
                                        image_stack = "images/Cisco/Stack-4.png";
                                    }
                                    if (image_stack != null) {
                                        node_info.put("image", image_stack);
                                        logger.Println("Set node: " + node + " image: " + image_stack, logger.DEBUG);
                                        continue;
                                    }

                                }
                            }


                            ArrayList<String> ident_list = new ArrayList();
                            if (node_info.get("general") != null) {
                                String sysDescription = (String) ((Map) node_info.get("general")).get("sysDescription");
                                if (sysDescription != null)
                                    ident_list.add(sysDescription);
                                String model = (String) ((Map) node_info.get("general")).get("model");
                                if (model != null)
                                    ident_list.add(model);
                            }

                            String image_node = null;
                            for (String ident : ident_list) {
                                for (Map.Entry<String, String> entry2 : shablon_image.entrySet()) {
                                    String shablon = entry2.getKey();
                                    String image = entry2.getValue();
                                    if (ident.toLowerCase().matches(shablon.toLowerCase())) {
                                        image_node = image;
                                        break;
                                    }
                                }
                                if (image_node != null) {
                                    break;
                                }
                            }
                            if (image_node != null) {
                                node_info.put("image", image_node);
                                logger.Println("Set node: " + node + " image: " + image_node, logger.DEBUG);
                            }
                        }
                    }
                }
            }
        }

        return Info;
    }

    public Map<String, Map> applyImport(String import_file, Map<String, Map> Info) {
        ArrayList<Map> import_list = readJSONFileToList(import_file);
        for (Map task : import_list) {
            String command = (String) task.get("command");
            String key = (String) task.get("key");
            Object value = task.get("value");
            if (command != null && key != null && value != null && command.equals("add")) {
                setKey(key, value, Info);
            } else if (command != null && key != null && value != null && command.equals("add_list")) {
                Object val = getKey(key, Info);
                if (val instanceof List list) {
                    list.add(value);
                }
            } else if (command != null && key != null && command.equals("del")) {
                deleteKey(key, Info);
            } else if (command != null && key != null && value != null && command.equals("del_list")) {
                Object val = getKey(key, Info);
                if (val instanceof List list && !((List) val).isEmpty()) {
                    int prev_size = list.size();
                    val = deleteFromList(value, (ArrayList) val);
                    if (list.size() != prev_size) {
                        deleteKey(key, Info);
                        setKey(key, val, Info);
                    }
//                    deleteKey(key, Info);
//                    setKey(key, val, Info);
                }
            }
        }

        return Info;
    }

    public boolean is_cfg_empty(Map cfg) {
        Map<String, Map> areas = (Map) cfg.get("areas");
        if (areas != null) {
            for (Map.Entry<String, Map> entry : areas.entrySet()) {
                Map area = entry.getValue();
                ArrayList<String> networks = (ArrayList) area.get("networks");
                ArrayList<String> snmp_community = (ArrayList) area.get("snmp_community");
                if (!networks.isEmpty() && !snmp_community.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map input_base_cfg() {
        Scanner in = new Scanner(System.in);
        ArrayList<String> networks = new ArrayList();
        ArrayList<String> community = new ArrayList();
        while (true) {
            System.out.print("Input scanning networks(192.168.0.0/24, 10.1.1.1): ");
            String net_str = in.nextLine();
            String[] mas = net_str.split(",");
            boolean error = false;
            for (String net : mas) {
                net = net.trim();
                if (!(net.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+") ||
                        net.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    error = true;
                    break;
                } else {
                    networks.add(net);
                }
            }
            if (!error) break;
        }
        System.out.print("Input snmp community(community1, community2): ");
        String snmp_str = in.nextLine();
        String[] mas = snmp_str.split(",");
        for (String comm : mas) {
            community.add(comm.trim());
        }

        Map cfg = new LinkedHashMap();
        ArrayList<String> times = new ArrayList();
        times.add("00:00");
        cfg.put("build_network_time", times);

        Map areas = new LinkedHashMap();
        Map area = new LinkedHashMap();
        area.put("description", "area description");
        area.put("networks", networks);
        area.put("snmp_community", community);
        area.put("include", new ArrayList());
        area.put("exclude", new ArrayList());
        area.put("discovery_networks", "yes");
        areas.put("area", area);
        cfg.put("areas", areas);

        Map users = new LinkedHashMap();
        Map admin = new LinkedHashMap();
        admin.put("passwd", "admin");
        admin.put("access", "write");
        users.put("admin", admin);
        cfg.put("users", users);

        cfg.put("calculate_links_from_counters", "yes");

        ArrayList<Integer> included_port = new ArrayList();
        included_port.add(22);
        included_port.add(23);
        cfg.put("included_port", included_port);

        ArrayList<Integer> excluded_port = new ArrayList();
        excluded_port.add(135);
        cfg.put("excluded_port", excluded_port);

        cfg.put("map_file", "neb.map");
        cfg.put("http_port", 8080L);
        cfg.put("https_port", 9090L);
        cfg.put("level_log", "INFO");
        cfg.put("history_num_days", 41L);
        cfg.put("log_num_days", 41L);

        return cfg;
    }

}

//class Watch_Telemetry_Lib extends Thread {
//
//    public boolean exit = false;
//    private String descr = "";
//
//    public Watch_Telemetry_Lib(String descr) {
//        this.descr = descr;
//    }
//
//    @Override
//    public void run() {
//        String max = Long.toString(ru.kos.neb.neb_lib.Utils.max_value);
//        Utils.sendTelemetry(descr + "|0|" + max);
//        while (!exit) {
//            String val = Long.toString(ru.kos.neb.neb_lib.Utils.current_value);
//            Utils.sendTelemetry(descr + "|" + val + "|" + max);
//        }
//        Utils.sendTelemetry(descr + "|" + max + "|" + max);
//    }
//}
