package ru.kos.neb.neb_builder;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.toIntExact;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import static ru.kos.neb.neb_builder.Neb.utils;

import ru.kos.neb.neb_lib.RunScriptsPool;


/**
 *
 * @author kos
 */
@SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
public class Neb {
//    public static String home = "";
    public static String delimiter = "<DELIMITER>";
    public static Thread main_thread = null;
    public static String map_file = "neb.map";
    public static String neb_cfg = "neb.cfg";
    public static String map_file_pre = "neb.map.pre";
//    private static String delete_file = "delete.buff";
    public static String history_map = "history";
    public static String home_dir = "";
    private static String log = "neb.log";
    private static String log_user = "neb_user.log";
    private static String log_history_dir = "Log";
    public static String history_dir = "history";
    public static String node_attribute_old_file = "node_attribute_old";
    public static String links_calculate_prev_count_file = "links_calculate_prev_count";
    public static String filter_nodes_file = "filter_nodes";
    public static String image_nodes_file = "image_nodes";
    public static String names_info = "names_info";
    public static String nodes_delete = "nodes_delete";
    public static String links_delete = "links_delete";
    public static String nodes_adding = "nodes_adding";
    public static String links_adding = "links_adding"; 
    public static String import_file = "import";
//    private static String dump = "dump.tmp";
    public static String debug_folder = "debug";
    public static String static_data_folder = "static_data";
    public static Map<String, ArrayList<String>> area_networks = new HashMap();
//    public static ArrayList<String> networks_for_current_area = new ArrayList();
    public static int history_num_days = 41;
    public static int log_num_days = 41;
    public static Map<String, String[]> user_passwd_buffer = new HashMap();
    public static Long user_passwd_buffer_timeout = 5*60*60*1000L; // 5 hour
    private static final int PAUSE = 60 * 60; // sec.
//    private static final int PAUSE = 15 * 60; // sec.
    private static final int PAUSE_TEST = 15 * 60; // sec.    
    private static final int RETRIES_TESTING = 3;
    private static final int LIMIT_RETRIES_TESTING = 3;
//    private static final int TIME_LAG = 20; // sec.
    private static final double PRECESSION_LIMIT = 0.02;
    public static int timeout = 5; // timeout network.
    public static int timeout_ping = 3;
    public static int retries = 1;
    public static int timeout_thread = 10; // 10 min
    public static int timeout_mac = 30; // mac timeout 3 sec.
    public static int timeout_mac_retry = 600; // mac timeout 3 sec.
    public static int retries_mac = 1; // mac timeout 1 sec.
    public static int timeout_thread_mac = 180; // 180 min
    public static final int MAXPOOLTHREADS = 256;
    public static ArrayList<Integer> included_port = new ArrayList();
    public static ArrayList<Integer> excluded_port = new ArrayList();
//    public static ArrayList<String> ifaces_counters_extended = new ArrayList();
    public static int links_calculate_prev_history = 3;
    public static long DELAY_WRITE_FILE = 60*3;

    public static Logger logger;
    public static Logger logger_user;
    public static ArrayList<String> node_scanning = new ArrayList();
//    public static String home = "";
    public static Map<String, String[]> nodes_info = new HashMap();
    public static ArrayList<String[]> links_info = new ArrayList();
    public static Map<String, String[]> mac_ArpMacTable = new HashMap();
    public static Map<String, String[]> ip_ArpMacTable = new HashMap();
    public static Map<String, String[]> extended_info = new HashMap();
    public static Map<String, String[]> text_info = new HashMap();
    public static Map<String, String[]> text_custom_info = new HashMap();
    public static Map<String, ArrayList<String[]>> area_arp_mac_table = new HashMap();
    public static Map<String, Map<String, Map<String, String>>> area_node_ifaceid_ifacename = new HashMap();

    public static Map cfg = new HashMap();
    public static Map<String, ArrayList<ArrayList<String>>> INDEX = new HashMap();
    public static String index_dir = "index";
    public static Utils utils = new Utils();
    public static ru.kos.neb.neb_lib.Utils neb_lib_utils;
    
    public static boolean RELOAD_PROGRESS = false;

    public static String run_post_scripts = "PostScripts/run.cmd";
//    private static String run_post_post_scripts = "PostScripts/run1.cmd";
    

    public static boolean DEBUG = true;
    
    public static RunScriptsPool runScriptsPool;
    
//    public static ArrayList<Map<String, String>> clusters_nodes_delete = new ArrayList();
    public static ArrayList<String[]> clusters_nodes_image = new ArrayList();
//    public static String clusters_nodes_file = "clusters_nodes";
    public static Map<Object, Object> area_nodes_images_buffer = new HashMap();

    public static String hub_image = "images/HUB.png";
    public static String wan_image = "images/WAN.png";
    
    public static ArrayList<String> not_correct_networks = new ArrayList();
    
    public static void main(String[] args) {
        main_thread = Thread.currentThread();
        if(args.length == 0) {           
            initNeb();
        }
        else {
            String mode = args[0];
            if ("start".equals(mode)) {  
                System.out.println("Running Neb.");
                initNeb();
            } else if("stop".equals(mode)) {
                System.out.println("Neb exit.");
                shutdown_Neb();
                System.exit(0);
            }
        }        
//        initNeb();
    }
    
    @SuppressWarnings("SleepWhileInLoop")
    private static void initNeb() {
        not_correct_networks.add("0.0.0.0/8");
        not_correct_networks.add("127.0.0.0/8");
        not_correct_networks.add("169.254.0.0/16");
        not_correct_networks.add("192.0.2.0/24");
        not_correct_networks.add("198.51.100.0/24");
        not_correct_networks.add("203.0.113.0/24");
        not_correct_networks.add("233.252.0.0/24");
                
        String folder = new File(Neb.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        folder = java.net.URLDecoder.decode(folder, StandardCharsets.UTF_8);
        while(true) {
            File cfg_file = new File(folder+"/cert.jks");
            if(cfg_file.exists()) {
                home_dir = folder+"/";
                home_dir = home_dir.replace("\\", "/");
                break;
            } else {
                folder = new File(folder).getParent();
            }            
        }

        static_data_folder = utils.getAbsolutePath(static_data_folder, home_dir);
        map_file = utils.getAbsolutePath(map_file, home_dir);
        neb_cfg = utils.getAbsolutePath(neb_cfg, home_dir);
        map_file_pre = utils.getAbsolutePath(map_file_pre, home_dir);
//        delete_file = utils.getAbsolutePath(delete_file, home_dir);
        history_map = utils.getAbsolutePath(history_map, home_dir);
        log = utils.getAbsolutePath(log, home_dir);
        log_user = utils.getAbsolutePath(log_user, home_dir);
//        dump = utils.getAbsolutePath(dump, home_dir);
        debug_folder = utils.getAbsolutePath(debug_folder, home_dir);
        
        index_dir = utils.getAbsolutePath(index_dir, home_dir);
        run_post_scripts = utils.getAbsolutePath(run_post_scripts, home_dir);
//        run_post_post_scripts = utils.GetAbsolutePath(run_post_post_scripts, home_dir);
        history_dir = utils.getAbsolutePath(history_dir, home_dir);
        log_history_dir = utils.getAbsolutePath(log_history_dir, home_dir);
//        secrets_file = utils.getAbsolutePath(secrets_file, home_dir);
        System.out.println("home = "+home_dir);
        
        node_attribute_old_file = static_data_folder+"/"+node_attribute_old_file;
        links_calculate_prev_count_file = static_data_folder+"/"+links_calculate_prev_count_file;
        names_info = static_data_folder+"/"+names_info;
        nodes_delete = static_data_folder+"/"+nodes_delete;
        links_delete = static_data_folder+"/"+links_delete;
        nodes_adding = static_data_folder+"/"+nodes_adding;
        links_adding = static_data_folder+"/"+links_adding;
        filter_nodes_file = static_data_folder+"/"+filter_nodes_file;
        image_nodes_file = static_data_folder+"/"+image_nodes_file;
        import_file = static_data_folder+"/"+import_file;

        // add shutdown hook
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        ru.kos.neb.neb_lib.Utils.LOG_FILE = utils.getAbsolutePath(ru.kos.neb.neb_lib.Utils.LOG_FILE, home_dir);
        neb_lib_utils = new ru.kos.neb.neb_lib.Utils();
        
        File file_log_lib = new File(ru.kos.neb.neb_lib.Utils.LOG_FILE);
        if(file_log_lib.exists()) {
            file_log_lib.delete();
        }
        
        // read config file
        File cfg_file = new File(neb_cfg);
        if(cfg_file.exists()) {
            cfg = utils.readConfig(neb_cfg);
            if(utils.is_cfg_empty(cfg)) {
                cfg = utils.input_base_cfg();
                utils.mapToFile(cfg, neb_cfg, DELAY_WRITE_FILE);
            }
        } else {
            cfg = utils.input_base_cfg();
            utils.mapToFile(cfg, neb_cfg, DELAY_WRITE_FILE);
        }

        int http_port = 8080;
        if(cfg.get("http_port") != null)
            http_port = ((Long) cfg.get("http_port")).intValue();
        
        int https_port = 9090;
        if(cfg.get("https_port") != null)
            https_port = ((Long) cfg.get("https_port")).intValue(); 


        // start logging
        System.out.println("log = "+log);
        System.out.println("log_user = "+log_user);
        logger = new Logger(log);
        logger_user = new Logger(log_user);
        String level_log = (String) cfg.get("level_log");
        if (level_log.equals("INFO")) {
            ru.kos.neb.neb_lib.Utils.DEBUG=false;
            DEBUG=false;
            logger.SetLevel(logger.INFO);
            logger_user.SetLevel(logger_user.INFO);
        } else if (level_log.equals("DEBUG")) {
            ru.kos.neb.neb_lib.Utils.DEBUG=true;
            DEBUG=true;
            logger.SetLevel(logger.DEBUG);
            logger_user.SetLevel(logger_user.DEBUG);
        }

        if(DEBUG) {
            File folder_debug = new File(debug_folder);
            if (!folder_debug.exists()) {
                folder_debug.mkdir();
            }
        }
        File folder_static_data = new File(static_data_folder);
        if (!folder_static_data.exists()) {
            folder_static_data.mkdir();
        }         
        // starting watching modify files
        WatchFile watchFile = new WatchFile();
        watchFile.start();

        logger.Println("Run HTTPS server.", logger.DEBUG);
        QueueWorker queueWorker = new QueueWorker();
        queueWorker.start();
        
        Server_HTTP server_HTTP = new Server_HTTP(http_port);
        server_HTTP.start();     
        
        Server_HTTPS server_HTTPS = new Server_HTTPS(https_port);
        server_HTTPS.start();   
        
//        // read JSON file for check input master key
//        secrets = utils.readJSONFile(secrets_file);
        
        // input master key
        utils.input_master_key();
        
//        //read secrets file with decryption
//        secrets = utils.readSecretsFile(secrets_file);        
        
//        // read clusters_nodes file
//        File file_clusters_nodes = new File(debug_folder+"/"+clusters_nodes_file);
//        if (file_clusters_nodes.exists()) {
//            clusters_nodes_delete = utils.readJSONFileToList(debug_folder+"/"+clusters_nodes_file);
//        }
        
        // full indexing
        logger.Println("Start indexing ...", logger.INFO);
        utils.indexing(INDEX);
        logger.Println("Stop indexing.", logger.INFO);

        while (true) {
            // get current time
            Date date = new Date(System.currentTimeMillis());
            String d = date.toString();
            String[] arr = d.split(" ");
            String[] time1 = arr[3].split(":");
            int hour = Integer.parseInt(time1[0]);
            int minute = Integer.parseInt(time1[1]);

            ArrayList<String> build_network_time = (ArrayList<String>) cfg.get("build_network_time");
            ArrayList<String> build_times = new ArrayList();
            if (build_network_time != null) {
                for (String item : build_network_time) {
                    String[] mas = item.split("\\s*,\\s*");
                    for (String str : mas) {
                        if (str.matches("^\\d{1,2}:\\d{1,2}$")) {
                            build_times.add(str);
                        }
                    }
                }
            }

            if( (utils.is_need_master_key(Neb.cfg) &&
                    !ru.kos.neb.neb_lib.Utils.master_key.isEmpty() &&
                utils.check_master_key(Neb.cfg, ru.kos.neb.neb_lib.Utils.master_key))
                    ||
                !utils.is_need_master_key(Neb.cfg) ) {
                if(utils.is_not_blank_cfg(Neb.cfg)) {
                    if(!(new File(map_file)).exists()) {
                        runNeb();
                    } else {
                        for (String build_time : build_times) {
                            String[] time_start = build_time.split(":");
                            //                System.out.println("Start time: "+time_start[0]+":"+time_start[1]+"\tCurrent: "+hour+":"+minute);
                            //                if(true) {
                            if (Integer.parseInt(time_start[0]) == hour && Integer.parseInt(time_start[1]) == minute) {
                                runNeb();
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(10000);
            } catch (java.lang.InterruptedException e) {
                if (DEBUG) {
                    System.out.println(e);
                }
            }
        }        
    }

    private static void runNeb() {
        utils.reloadCfg(neb_cfg);
       
        // delete old log files
        utils.removeOldFiles(log_history_dir, ((Long) cfg.get("log_num_days")).intValue());
        // copy neb.log file to Log directory
        File file_log = new File(log);
        if (file_log.exists()) {
            Date dd = new Date(file_log.lastModified());
            SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
            String file_log_history = log_history_dir+"/Neb_" + format1.format(dd) + ".log";
            File folder_log = new File(log_history_dir);
            File history_log = new File(file_log_history);
            if (!folder_log.exists()) {
                folder_log.mkdir();
            }
            try {
                if (!history_log.exists()) {
                    Files.copy(file_log.toPath(), history_log.toPath());
                }
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Neb.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // start new logging
        logger.Clear(log);
        
       // copy neb_user.log file to Log directory
        File file_log_user = new File(log_user);
        if (file_log_user.exists()) {
            Date dd = new Date(file_log_user.lastModified());
            SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
            String file_log_user_history = log_history_dir+"/Neb_user_" + format1.format(dd) + ".log";
            File folder_log = new File(log_history_dir);
            File history_user_log = new File(file_log_user_history);
            if (!folder_log.exists()) {
                folder_log.mkdir();
            }
            try {
                if (!history_user_log.exists()) {
                    Files.copy(file_log_user.toPath(), history_user_log.toPath());
                }
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Neb.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        

        // start new logging
        logger_user.Clear(log_user);

        ru.kos.neb.neb_lib.Utils.LOG_FILE = utils.getAbsolutePath(ru.kos.neb.neb_lib.Utils.LOG_FILE, home_dir);
        File file_log_lib = new File(ru.kos.neb.neb_lib.Utils.LOG_FILE);
        if(file_log_lib.exists()) {
            Logger logger_lib = new Logger(ru.kos.neb.neb_lib.Utils.LOG_FILE);
            logger_lib.Clear(ru.kos.neb.neb_lib.Utils.LOG_FILE);
        }
//        runScriptsPool = new RunScriptsPool(timeout_process, timeout_output, MAX_RUNSCRIPT_CLI_TEST);

        logger.Println("Runnung Neb program ...", logger.INFO);

        // read config file
        cfg = utils.readConfig(neb_cfg);
        System.out.println("Load neb.cfg file.");

        // read neb.map file
        Map INFORMATION = new HashMap();
        File file_map = new File(map_file);
        if (file_map.exists()) {
            INFORMATION = utils.readJSONFile(map_file);
        }
        
        Map<String, Map<String, String[]>> snmp_accounts = utils.get_SNMP_accounts(INFORMATION);

        Map<String, Map> areas = (Map<String, Map>) cfg.get("areas");
        if (areas != null && !areas.isEmpty()) {
            Map informationFromNodesAllAreas = new HashMap();

            node_scanning.clear();
            //////////////////////////////////////////////////////////////////////////////////////

//            Map<String, ArrayList<String>> area_networks = new HashMap();
            for (Map.Entry<String, Map> area : areas.entrySet()) {
                ArrayList<String> networks = new ArrayList();
                ArrayList<String> ip_list = new ArrayList();
//                networks.clear();
//                ip_list.clear();
                String area_name = area.getKey();
                logger.Println("Area: " + area_name, logger.INFO);
                ArrayList<String> community_list = (ArrayList<String>) area.getValue().get("snmp_community");
                area.getValue().get("cli_accounts");
                ArrayList<String> include_list = (ArrayList<String>) area.getValue().get("include");
                Map<String, String> exclude_list = (Map<String, String>) area.getValue().get("exclude");
                ArrayList<String> network_list = (ArrayList<String>) area.getValue().get("networks");

                for (String network : network_list) {
                    if (network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$") || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$")) {
                        networks.add(network);
                    } else if (network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$")) {
                        ip_list.add(network);
                    } else {
                        logger.Println(network + " is not correct format!", logger.INFO);
                    }
                }

                String discovery_networks = ((String) area.getValue().get("discovery_networks")).toLowerCase();
                if (cfg.get("areas") != null && ((Map) cfg.get("areas")).get(area_name) != null) {
                    Long timeout_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("timeout");
                    Long retries_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("retries");
                    if (timeout_options != null) {
                        toIntExact(timeout_options);
                    }
                    if (retries_options != null) {
                        toIntExact(retries_options);
                    }
                    Long timeout_process_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("timeout_process");
                    if (timeout_process_options != null) {
                        toIntExact(timeout_process_options);
                    }
//                                    if(retries_process_options != null) retries_process=toIntExact(retries_process_options);                                
                }

                ArrayList<String> net_list = new ArrayList();
                net_list.addAll(networks);
                net_list.addAll(ip_list);

                logger.Println("Start scanning information from nodes ...", logger.DEBUG);
                Map informationFromNodes = utils.scanInformationFromNodes(networks, ip_list, community_list, exclude_list, snmp_accounts.get(area_name), net_list);
                logger.Println("Stop scanning information from nodes.", logger.DEBUG);
                if(!informationFromNodes.isEmpty()) {
                    // recursive explorer networks
                    int iteration = 1;
                    logger.Println("Start rescanning information from nodes ...", logger.DEBUG);
                    int prev_size = 0;
                    while (discovery_networks.equals("yes")) {
                        informationFromNodes = utils.rescanInformationFromNodes(informationFromNodes, community_list, include_list, snmp_accounts.get(area_name), net_list);
                        int cur_size = ((Map)informationFromNodes.get("nodes_information")).size();
                        if(informationFromNodes.get("nodes_information") == null || cur_size == prev_size) {
                            break;
                        } else {
                            prev_size = cur_size;
                        }
                        logger.Println("Discoverer iterations = " + iteration, logger.DEBUG);
                        iteration++;
                    }
                    logger.Println("Stop rescanning information from nodes.", logger.DEBUG);
                }
                
                if(!informationFromNodes.isEmpty()) {
                    Map<String, Map> nodes_information = (Map<String, Map>) informationFromNodes.get("nodes_information");
                    area_networks.put(area_name, net_list);
//                    networks_for_current_area.addAll(net_list);
                    ArrayList<ArrayList<ArrayList<String>>> result = utils.normalizationLinks(nodes_information, net_list, not_correct_networks);
                    ArrayList<ArrayList<String>> links = result.get(0);
                    ArrayList<ArrayList<String>> links_extended = result.get(1);
                    informationFromNodes.put("links", links);
                    informationFromNodes.put("links_extended", links_extended);

                    // adding nodes from links
                    informationFromNodes = utils.addingNodesFromLinks(informationFromNodes);

                    // remove exclude_list
                    if (informationFromNodes.get("exclude_list") != null) {
                        informationFromNodes.remove("exclude_list");
                    }

                    informationFromNodesAllAreas.put(area_name, informationFromNodes);

                    // write to file nodes_information
    //                            utils.MapToFile((Map)informationFromNodes, "informationFromNodes"+area_name);
                    logger.Println("=====================================", logger.INFO);
                }
            }

            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info1", DELAY_WRITE_FILE);

            // remove duplicate nodes in all areas am mac addresses, ip list and sysname
            logger.Println("Start RemoveDuplicateNodes...", logger.DEBUG);
            informationFromNodesAllAreas = utils.removeDuplicateNodes(informationFromNodesAllAreas, area_networks);
            logger.Println("Stop RemoveDuplicateNodes.", logger.DEBUG); 
                        
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info2", DELAY_WRITE_FILE);
            logger.Println("Start GetAreaNodeCommunityVersionDP...", logger.DEBUG);
            Map<String, ArrayList<String[]>> area_node_community_version_dp = utils.getAreaNodeCommunityVersionDP(informationFromNodesAllAreas);
            logger.Println("Stop GetAreaNodeCommunityVersionDP.", logger.DEBUG);
            if(DEBUG) utils.mapToFile(area_node_community_version_dp, debug_folder+"/area_node_community_version_dp", DELAY_WRITE_FILE);

            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info3", DELAY_WRITE_FILE);
            // get nodes ifaceid ifacename 
            logger.Println("Start GetAreaNodeIdIface...", logger.DEBUG);
            area_node_ifaceid_ifacename = utils.getAreaNodeIdIface(informationFromNodesAllAreas, area_node_community_version_dp);
            logger.Println("Stop GetAreaNodeIdIface.", logger.DEBUG);
            if(DEBUG) utils.mapToFile(area_node_ifaceid_ifacename, debug_folder+"/area_node_ifaceid_ifacename", DELAY_WRITE_FILE);

            // adding ifaceid to links
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info4", DELAY_WRITE_FILE);
            logger.Println("Start AddingIfaceIdToLinks...", logger.DEBUG);
            informationFromNodesAllAreas = utils.addingIfaceIdToLinks(informationFromNodesAllAreas, area_node_ifaceid_ifacename);
            logger.Println("Stop AddingIfaceIdToLinks.", logger.DEBUG);
            
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info5", DELAY_WRITE_FILE);
            Map<String, ArrayList<ArrayList<String>>> area_links_calculate = new HashMap();
            if (cfg.containsKey("calculate_links_from_counters") && cfg.get("calculate_links_from_counters").equals("yes")) {
                area_links_calculate = utils.getCalculateLinks(informationFromNodesAllAreas, area_node_community_version_dp, PAUSE, PAUSE_TEST, PRECESSION_LIMIT, RETRIES_TESTING, LIMIT_RETRIES_TESTING);
            } else {
                logger.Println("Start GetAreaNodeCommunityVersion...", logger.DEBUG);
                Map<String, ArrayList<String[]>> area_node_community_version = utils.getAreaNodeCommunityVersion(informationFromNodesAllAreas);
                logger.Println("Stop GetAreaNodeCommunityVersion.", logger.DEBUG);

                area_arp_mac_table = utils.getArpMac(area_node_community_version);
                if(DEBUG) utils.mapToFile(area_arp_mac_table, debug_folder+"/area_arp_mac_table", DELAY_WRITE_FILE);
                informationFromNodesAllAreas = utils.appendMacFromArpMacTable(informationFromNodesAllAreas, area_arp_mac_table);
            }

            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info6", DELAY_WRITE_FILE);
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                String area_name = area.getKey();
                Map val = area.getValue();
                ArrayList<ArrayList<String>> links_result = new ArrayList();
                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                ArrayList<ArrayList<String>> links_calculate = area_links_calculate.get(area_name);
                if (links != null && !links.isEmpty()) {
                    for (ArrayList<String> link : links) {
                        if (link.size() > 5) {
                            ArrayList<String> mas = new ArrayList();
                            mas.add(link.get(0));
                            mas.add(link.get(1));
                            mas.add(link.get(2));
                            mas.add(link.get(3));
                            mas.add(link.get(4));
                            mas.add(link.get(5));
                            mas.add(link.get(6));
                            links_result.add(mas);
                        } else {
                            logger.Println("links size <= 5. Size =" + link.size(), logger.DEBUG);
                            for (String it : link) {
                                logger.Println(it, logger.DEBUG);
                            }
                            logger.Println("----------------------", logger.DEBUG);
                            ArrayList<String> mas = new ArrayList();
                            mas.add(link.get(0));
                            mas.add("");
                            mas.add(link.get(1));
                            mas.add(link.get(2));
                            mas.add("");
                            mas.add(link.get(3));
                            mas.add(link.get(4));
                            links_result.add(mas);
                        }
                    }
                    val.remove("links");
                }
                if (links_calculate != null && !links_calculate.isEmpty()) {
                    for (ArrayList<String> link : links_calculate) {
                        if (link.size() > 5) {
                            ArrayList<String> mas = new ArrayList();
                            mas.add(link.get(0));
                            mas.add(link.get(1));
                            mas.add(link.get(2));
                            mas.add(link.get(3));
                            mas.add(link.get(4));
                            mas.add(link.get(5));
                            mas.add("calc");
                            links_result.add(mas);
                        } else {
                            logger.Println("links size <= 5. Size =" + link.size(), logger.DEBUG);
                            for (String it : link) {
                                logger.Println(it, logger.DEBUG);
                            }
                            logger.Println("----------------------", logger.DEBUG);
                            ArrayList<String> mas = new ArrayList();
                            mas.add(link.get(0));
                            mas.add("");
                            mas.add(link.get(1));
                            mas.add(link.get(2));
                            mas.add("");
                            mas.add(link.get(3));
                            mas.add("calc");
                            links_result.add(mas);
                        }
                    }
//                                val.remove("links_calculate");
                }
                val.remove("links_extended");
                val.put("links", links_result);
            }

            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info7", DELAY_WRITE_FILE);

            // adding mac_ip_port
            Map<String, ArrayList<String[]>> calculate_ARP_MAC = new HashMap();
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                String area_name = area.getKey();
                Map val = area.getValue();
                Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                Map node_protocol_accounts = (Map) val.get("node_protocol_accounts");

                logger.Println("Calculate mac_ip_node_port for area: " + area_name, logger.DEBUG);
                ArrayList<String[]> arp_mac_table = area_arp_mac_table.get(area_name);
                calculate_ARP_MAC.put(area_name, arp_mac_table);
                if (arp_mac_table != null && !arp_mac_table.isEmpty()) {
                    ArrayList<String[]> mac_ip_node_port = utils.calculateARPMAC(arp_mac_table, links, nodes_information, node_protocol_accounts);
                    val.put("mac_ip_port", mac_ip_node_port);
                }
            }
            if(DEBUG) utils.mapToFile(calculate_ARP_MAC, debug_folder+"/calculate_ARP_MAC", DELAY_WRITE_FILE);
            
            // remove old map files
            logger.Println("Remove old map and log files", logger.DEBUG);
            utils.removeOldFiles(history_dir, ((Long) cfg.get("history_num_days")).intValue());

            // remove advanced information 
            logger.Println("Remove advanced information", logger.DEBUG);
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                Map<String, Map> val = area.getValue();
                // remove advanced information  from nodes
                Map<String, Map> info_nodes_tmp1 = (Map<String, Map>) val.get("nodes_information");
                for (Map.Entry<String, Map> entry : info_nodes_tmp1.entrySet()) {
                    Map val1 = entry.getValue();
                    if (val1.get("advanced") != null) {
                        val1.remove("advanced");
                    }
                }
            }

            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info8", DELAY_WRITE_FILE);

            // Normalization information map
            logger.Println("Normalization information map", logger.DEBUG);
            informationFromNodesAllAreas = utils.normalizeMap(informationFromNodesAllAreas, area_arp_mac_table);
            
            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info9", DELAY_WRITE_FILE);

            ////////////// Calculate forks links /////////////////////////////////////////
//            Map<String, ArrayList> area_forks = utils.getForkList(informationFromNodesAllAreas);
//            Map<String, ArrayList> area_add_del_links = utils.getForkLinks(area_forks, informationFromNodesAllAreas, area_arp_mac_table);
//            utils.modifyLinks(informationFromNodesAllAreas, area_add_del_links);
            /////////////////////////////////////////////////////                        

            // write to file nodes_information
//            if(DEBUG) utils.mapToFile((Map) informationFromNodesAllAreas, debug_folder+"/info10", DELAY_WRITE_FILE);

            // Normalization information map
            logger.Println("Normalization information map 2 cicle.", logger.DEBUG);
            informationFromNodesAllAreas = utils.normalizeMap2(informationFromNodesAllAreas);                        
            
            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info11", DELAY_WRITE_FILE);
            
            // remove node nodes_delete from informationFromNodesAllAreas
            logger.Println("Remove node nodes_delete from informationFromNodesAllAreas.", logger.DEBUG);
            Map nodes_delete_info = utils.readJSONFile(nodes_delete);
//            Map informationFromNodesAllAreas_new = new HashMap();
            for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_delete_info).entrySet()) {
                String area_delete = entry.getKey();
                Map val = entry.getValue();
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                    String node_delete = entry1.getKey();
                    informationFromNodesAllAreas = utils.deleteNode(informationFromNodesAllAreas, area_delete, node_delete);
                }
            }
            
            // remove link links_delete from informationFromNodesAllAreas
            logger.Println("Remove link links_delete from informationFromNodesAllAreas.", logger.DEBUG);
            Map links_delete_info = utils.readJSONFile(links_delete);
//            Map informationFromNodesAllAreas_new = new HashMap();
            for(Map.Entry<String, ArrayList> entry : ((Map<String, ArrayList>)links_delete_info).entrySet()) {
                String area_delete = entry.getKey();
                Map area_info = (Map)informationFromNodesAllAreas.get(area_delete);
                if(area_info != null) {
                    ArrayList<ArrayList<String>> links = (ArrayList)area_info.get("links");
                    ArrayList<ArrayList<String>> del_link_list = entry.getValue();
                    for(ArrayList<String> del_link : del_link_list) {
                        if(utils.delLink(del_link, links)) {
                            Neb.logger.Println("Delete link: "+area_delete+": "+del_link.get(0)+" "+del_link.get(2)+" <---> "+del_link.get(3)+" "+del_link.get(5), Neb.logger.DEBUG);
                        } else {
                            Neb.logger.Println("Error delete link: "+area_delete+": "+del_link.get(0)+" "+del_link.get(2)+" <---> "+del_link.get(3)+" "+del_link.get(5), Neb.logger.DEBUG);
                        }
                    }
                }
            }
            
            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info12", DELAY_WRITE_FILE);
            
            // adding custom nodes from nodes_adding
            Map<String, Map> nodes_add_info = utils.readJSONFile(Neb.nodes_adding);
            for (Map.Entry<String, Map> entry : nodes_add_info.entrySet()) {
                String area = entry.getKey();
                Map<String, Map> area_info = entry.getValue();
                if(informationFromNodesAllAreas.get(area) != null) {
                    for (Map.Entry<String, Map> entry1 : area_info.entrySet()) {
                        String node_add = entry1.getKey();
                        Map node_add_info = entry1.getValue();
                        Map<String, Map> nodes_information = (Map)((Map)informationFromNodesAllAreas.get(area)).get("nodes_information");
                        if(nodes_information != null) {
                            nodes_information.put(node_add, node_add_info);
                        }
                    }
                }
            }
            
            // adding custom links from nodes_adding
            Map<String, ArrayList> links_add_info = utils.readJSONFile(Neb.links_adding);
            for (Map.Entry<String, ArrayList> entry : links_add_info.entrySet()) {
                String area = entry.getKey();
                ArrayList<ArrayList<String>> area_info = entry.getValue();
                if(informationFromNodesAllAreas.get(area) != null) {
                    for(ArrayList<String> add_link : area_info) {
                        ArrayList<ArrayList<String>> links = (ArrayList)((Map)informationFromNodesAllAreas.get(area)).get("links");
                        if(links != null) {
                            links.add(add_link);
                        }
                    }
                }
            }
            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info13", DELAY_WRITE_FILE);
            
            
            // replace map file to history
            logger.Println("Replace map file to history", logger.DEBUG);
            File file = new File(map_file);
            if (file.exists()) {
                String file_history = history_dir + "/Neb_" + utils.getFileCreateTime(map_file).replace(" ", "-") + ".map";
                File folder_history = new File(history_dir);
                File history = new File(file_history);
                if (!folder_history.exists()) {
                    folder_history.mkdir();
                }
                if (!history.exists()) {
                    try {
                        Files.copy(file.toPath(), history.toPath());
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(Neb.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }

            // read neb.map file
            INFORMATION = new HashMap();
            if (file_map.exists()) {
                INFORMATION = utils.readJSONFile(map_file);
            }
            // save nodes attribute to node_attribute_old file
            Map<String, Map> area_node_attribute_old = Neb.utils.setAttributeOld(INFORMATION, node_attribute_old_file);
            Neb.utils.mapToFile(area_node_attribute_old, node_attribute_old_file, Neb.DELAY_WRITE_FILE);
            
            // write to file nodes_information
//            logger.Println("Write to "+map_file_pre+" nodes_information", logger.DEBUG);
            Map<String, Map> INFORMATION_PRE = utils.setInformations(informationFromNodesAllAreas, INFORMATION);

            logger.Println("Start apply import file ...", logger.DEBUG);
            INFORMATION_PRE = utils.applyImport(Neb.import_file, INFORMATION_PRE);
            logger.Println("Stop apply import file.", logger.DEBUG);
            
            // set nodes attributes
            INFORMATION_PRE = utils.setAttributesToNodes(INFORMATION_PRE, map_file, node_attribute_old_file);
            
            logger.Println("Start set names_info ...", logger.DEBUG);
            utils.setName(INFORMATION_PRE, names_info);
            logger.Println("Stop set names_info.", logger.DEBUG);

//            // set nodes attributes
//            INFORMATION = utils.readJSONFile(map_file_pre);
            
            // filter nodes
            logger.Println("Start filter nodes ...", logger.DEBUG);
            INFORMATION_PRE = utils.filterNodes(INFORMATION_PRE, filter_nodes_file);
            logger.Println("Stop filter nodes.", logger.DEBUG);
            // images nodes
            logger.Println("Start image nodes ...", logger.DEBUG);
            INFORMATION_PRE = utils.imageNodes(INFORMATION_PRE, image_nodes_file);
            logger.Println("Stop image nodes.", logger.DEBUG);            
            
            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info14", DELAY_WRITE_FILE);

            INFORMATION_PRE = utils.setAttributesToNodes(INFORMATION_PRE, map_file, node_attribute_old_file);

            // Normalization 3.
            logger.Println("Start normalizeMap3 ...", logger.DEBUG);
            INFORMATION_PRE = utils.normalizeMap3(INFORMATION_PRE);
            logger.Println("Stop normalizeMap3.", logger.DEBUG);

            // write to file nodes_information
            if(DEBUG) utils.mapToFile(informationFromNodesAllAreas, debug_folder+"/info15", DELAY_WRITE_FILE);

//            utils.mapToFile((Map) INFORMATION, map_file_pre, DELAY_WRITE_FILE);
//////////////////////////////////////////////////////////////////////////  

//            INFORMATION = utils.readJSONFile(map_file_pre);
//            Map INFORMATION_OLD = utils.readJSONFile(map_file);
            // Graph layout
            logger.Println("Starting GraphLayout ...", logger.INFO);
            GraphLayout graphLayout = new GraphLayout();
//            INFORMATION = graphLayout.startGraphLayout(INFORMATION, INFORMATION_OLD);
            INFORMATION_PRE = graphLayout.startGraphLayout(INFORMATION_PRE);
            logger.Println("Stop GraphLayout.", logger.INFO);

            // transform node_protocol_accounts
            INFORMATION_PRE = utils.transform_node_protocol_accounts(INFORMATION_PRE);
            INFORMATION = INFORMATION_PRE;
            utils.mapToFile(INFORMATION, map_file, DELAY_WRITE_FILE);

            // delete file map_file_pre
            File f_pre = new File(map_file_pre);
            if (f_pre.exists())
                f_pre.delete();

            utils.setFileCreationDateNow(map_file, DELAY_WRITE_FILE);

            // indexing
            logger.Println("Start indexing ...", logger.INFO);
            INDEX = new HashMap();
            utils.indexing(INDEX);
            logger.Println("Stop indexing.", logger.INFO);

            informationFromNodesAllAreas.clear();
        }

        logger.Println("Stop running Neb program ...", logger.INFO);
    }

    public static void shutdown_Neb() {
        System.out.println("Shutdown Neb ...");
        if(logger != null) logger.Println("Start destroy scripts process ...", logger.INFO);
        if(runScriptsPool != null) {
            if(runScriptsPool.service != null)
                runScriptsPool.service.shutdownNow();


            runScriptsPool.DestroyPoolProcesses();
        }
        
        if(logger != null) logger.Println("Stop destroy scripts process.", logger.INFO);
    }
}

class ShutdownHook extends Thread {

    @Override
    public void run() {
        Neb.shutdown_Neb();
    }
}

class WatchFile extends Thread {
    private static Map<String, String> lastmodify = new HashMap();
    
    public WatchFile() {
    }    
    @Override
    public void run() {
        while(true) {
            try {
                lastmodify = CheckModify(lastmodify);
                Thread.sleep(10000);
            } catch(java.lang.InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static Map CheckModify(Map<String, String> lastmodify) {
        if(!Neb.RELOAD_PROGRESS) {
            Neb.RELOAD_PROGRESS = true;
            if(lastmodify.get(Neb.map_file) != null) {
                long lastmodify_time_prev = Long.parseLong(lastmodify.get(Neb.map_file));
                File f_map = new File(Neb.map_file);
                if(f_map.exists()) {
                    if(lastmodify_time_prev < f_map.lastModified()) {
                        Neb.logger.Println("Start reload "+Neb.map_file+" file.", Neb.logger.INFO);
                        Server_HTTP.INFO = Neb.utils.readJSONFile(Neb.map_file);
                        Neb.logger.Println("Stop reload "+Neb.map_file+" file.", Neb.logger.INFO);
                        lastmodify.put(Neb.map_file, String.valueOf(f_map.lastModified()));
                        Neb.logger.Println("Start indexing "+Neb.map_file+" file.", Neb.logger.INFO);
                        Neb.utils.clearIndex(Neb.map_file, Neb.INDEX);
                        Neb.utils.filesToIndexing(Neb.map_file, Neb.INDEX);
                        Neb.logger.Println("Stop indexing "+Neb.map_file+" file.", Neb.logger.INFO);
                    }
                }
            } else {
                File f_map = new File(Neb.map_file);
                if(f_map.exists()) {
                    Neb.logger.Println("Load "+Neb.map_file+" file.", Neb.logger.INFO);
                    Server_HTTP.INFO = Neb.utils.readJSONFile(Neb.map_file);
                    Neb.logger.Println("Stop load "+Neb.map_file+" file.", Neb.logger.INFO);
                    lastmodify.put(Neb.map_file, String.valueOf(f_map.lastModified()));
                    Neb.logger.Println("Start indexing "+Neb.map_file+" file.", Neb.logger.INFO);
                    Neb.utils.clearIndex(Neb.map_file, Neb.INDEX);
                    Neb.utils.filesToIndexing(Neb.map_file, Neb.INDEX);
                    Neb.logger.Println("Stop indexing "+Neb.map_file+" file.", Neb.logger.INFO);
                }
            }

            if(lastmodify.get(Neb.neb_cfg) != null) {
                long lastmodify_time_prev = Long.parseLong(lastmodify.get(Neb.neb_cfg));
                File f_neb_cfg = new File(Neb.neb_cfg);
                if(f_neb_cfg.exists()) {
                    if(lastmodify_time_prev < f_neb_cfg.lastModified()) {
                        Neb.cfg = utils.reloadCfg(Neb.neb_cfg);
                        lastmodify.put(Neb.neb_cfg, String.valueOf(f_neb_cfg.lastModified()));
                    }
                }
            } 
            else {
                File f_neb_cfg = new File(Neb.neb_cfg);
                if(f_neb_cfg.exists()) {
                    utils.reloadCfg(Neb.neb_cfg);
                    lastmodify.put(Neb.neb_cfg, String.valueOf(f_neb_cfg.lastModified()));
                }            
            }
            
            if(lastmodify.get(Neb.names_info) != null) {
                long lastmodify_time_prev = Long.parseLong(lastmodify.get(Neb.names_info));
                File f_names_info = new File(Neb.names_info);
                if(f_names_info.exists()) {
                    if(lastmodify_time_prev < f_names_info.lastModified()) {
                        Neb.logger.Println("Reload "+Neb.names_info+" file.", Neb.logger.INFO);
                        Server_HTTP.INFO_NAMES = Neb.utils.readJSONFile(Neb.names_info);
                        Neb.logger.Println("Stop reload "+Neb.names_info+" file.", Neb.logger.INFO);
                        lastmodify.put(Neb.names_info, String.valueOf(f_names_info.lastModified()));
                    }
                }
            } 
            else {
                File f_names_info = new File(Neb.names_info);
                if(f_names_info.exists()) {
                    Neb.logger.Println("Load "+Neb.names_info+" file.", Neb.logger.INFO);
                    Server_HTTP.INFO_NAMES = Neb.utils.readJSONFile(Neb.names_info);
                    Neb.logger.Println("Stop load "+Neb.names_info+" file.", Neb.logger.INFO);
                    lastmodify.put(Neb.names_info, String.valueOf(f_names_info.lastModified()));
                }            
            }
                       
            Neb.RELOAD_PROGRESS = false;
        }
        return lastmodify;
    }
    
}