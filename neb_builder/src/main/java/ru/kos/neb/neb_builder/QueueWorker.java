
package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import static ru.kos.neb.neb_builder.Neb.utils;

public class QueueWorker extends Thread {
    public static Map<String, ArrayList<String[]>> queue = new HashMap();
    public static boolean busy = false;
    public static long sleep_timeout = 10*1000;
 
    public QueueWorker() {

    } 
    
    @Override
    @SuppressWarnings({"SynchronizeOnNonFinalField", "SleepWhileInLoop"})
    public void run() {
        while(true) {
//            System.out.println("Queue size = "+queue.size());
            Map<String, ArrayList<String[]>> queue_tmp = new HashMap();
            if(!queue.isEmpty()) {
                try {
                    Neb.logger.Println("QueueWorker = True", Neb.logger.DEBUG);
                    busy = true;
//                    System.out.println("Start queue worker ...");
                    for(Map.Entry<String, ArrayList<String[]>> entry : queue.entrySet()) {
                        String file = entry.getKey();
                        ArrayList<String[]> list_tmp = new ArrayList();
                        for(String[] key_val : entry.getValue()) {
                            String[] mas = new String[3];
                            mas[0] = key_val[0];
                            mas[1] = key_val[1];
                            mas[2] = key_val[2];
//                            System.out.println("command="+mas[0]+" - "+mas[1]+" - "+mas[2]);
                            list_tmp.add(mas);
                        }
                        queue_tmp.put(file, list_tmp);
                    }

                    synchronized (queue) { queue = new HashMap(); }
//                    System.out.println("Start set key ...");
                    for(Map.Entry<String, ArrayList<String[]>> entry : queue_tmp.entrySet()) {
                        String file = entry.getKey();
                        Map<String, Map> INFO = Neb.utils.readJSONFile(file);
                        Map<String, Map> node_attribute = utils.readJSONFile(Neb.node_attribute_old_file);
                        for(String[] key_val : entry.getValue()) {
                            String command = key_val[0];
                            String key = key_val[1];
                            String val = key_val[2];
                            switch (command) {
                                case "SET" -> {
                                    Neb.logger.Println("Set key: " + key + " val: " + val, Neb.logger.DEBUG);
                                    Neb.utils.setValueToInfo(key, val, INFO);
//                                    System.out.println("Error set key: "+key+" val: "+val);
//                                    Neb.logger.Println("Error set key: "+key+" val: "+val, Neb.logger.DEBUG);
                                }
                                case "DELETE" -> {
                                    Neb.logger.Println("Delete key: " + key, Neb.logger.DEBUG);
                                    if (Neb.map_file.matches(".*" + file + "$") || Neb.map_file_pre.matches(".*" + file + "$")) {
//                                if(file.equals(Neb.map_file) || file.equals(Neb.map_file_pre)) {
                                        String end_symbol = key.substring(key.length() - 1);
                                        if (end_symbol.equals("/")) {
                                            key = key.substring(0, key.length() - 1);
                                        }
                                        key = key.strip();

                                        String[] mas = key.split("/");
                                        if (mas.length == 4 && mas[2].equals("nodes_information")) {
                                            String area = mas[1];
                                            String node = mas[3];
                                            if (INFO.get(area) != null &&
                                                    INFO.get(area).get("nodes_information") != null &&
                                                    ((Map) INFO.get(area).get("nodes_information")).get(node) != null &&
                                                    ((Map) ((Map) INFO.get(area).get("nodes_information")).get(node)).get("xy") != null
                                            ) {
                                                String image = (String) ((Map) ((Map) INFO.get(area).get("nodes_information")).get(node)).get("image");
                                                ArrayList<String> xy = (ArrayList<String>) ((Map) ((Map) INFO.get(area).get("nodes_information")).get(node)).get("xy");
                                                if (node_attribute.get(area) != null) {
                                                    Map tmp_map = new HashMap();
                                                    if (image != null) tmp_map.put("image", image);
                                                    if (xy != null) tmp_map.put("xy", xy);
                                                    node_attribute.get(area).put(node, tmp_map);
                                                } else {
                                                    Map tmp_node_map = new HashMap();
                                                    Map tmp_map = new HashMap();
                                                    if (image != null) tmp_map.put("image", image);
                                                    if (xy != null) tmp_map.put("xy", xy);
                                                    tmp_node_map.put(node, tmp_map);
                                                    node_attribute.put(area, tmp_node_map);
                                                }
                                            }
                                        }
                                    }
                                    Neb.utils.deleteKey(key, INFO);
                                }
                                case "DELETE_NODE" -> {
                                    String area = key_val[1];
                                    String node = key_val[2];
                                    utils.deleteNode(INFO, area, node);
                                    Neb.logger.Println("Delete node: " + area + "/" + node, Neb.logger.DEBUG);
//                                INFO = Neb.utils.deleteNode(INFO, area, node);
                                    Map nodes_delete_info = utils.readJSONFile(Neb.nodes_delete);
                                    utils.setKey("/" + area + "/" + node, node, nodes_delete_info);
                                    utils.mapToFile(nodes_delete_info, Neb.nodes_delete, Neb.DELAY_WRITE_FILE);
                                }
                                case "ADD_NODE" -> {
                                    String area = key_val[1];
                                    String node = key_val[2];
                                    String[] mas = node.split(";");
                                    if (mas.length == 4) {
                                        node = mas[0];
                                        String sysname = mas[1];
                                        String x = mas[2];
                                        String y = mas[3];
                                        String[] xy = new String[2];
                                        xy[0] = String.valueOf(x);
                                        xy[1] = String.valueOf(y);
                                        Map map_tmp = new HashMap();
                                        map_tmp.put("sysname", sysname);
                                        Map node_info = new HashMap();
                                        node_info.put("general", map_tmp);
                                        node_info.put("xy", xy);
                                        if (INFO.get(area) != null && INFO.get(area).get("nodes_information") != null) {
                                            Map<String, Map> nodes_info = (Map) INFO.get(area).get("nodes_information");
                                            nodes_info.put(node, node_info);
                                        }
                                        Neb.logger.Println("Add node: " + area + "/" + node, Neb.logger.DEBUG);
                                        Map nodes_add_info = utils.readJSONFile(Neb.nodes_adding);
                                        utils.setKey("/" + area + "/" + node, node_info, nodes_add_info);
                                        utils.mapToFile(nodes_add_info, Neb.nodes_adding, Neb.DELAY_WRITE_FILE);
                                    }
                                }
                                case "DELETE_LINK" -> {
                                    String area = key_val[1];
                                    String link_str = key_val[2];
                                    String[] mas = link_str.split(";");
                                    ArrayList<String> link = new ArrayList(Arrays.asList(mas));
                                    if (INFO.get(area) != null) {
                                        Neb.logger.Println("Delete link: " + area + ": " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), Neb.logger.DEBUG);
                                        utils.delLink(link, (ArrayList) INFO.get(area).get("links"));
                                        if (link.size() == 7 && link.get(6).equals("custom")) {
                                            Map links_delete_info = utils.readJSONFile(Neb.links_delete);
                                            ArrayList<ArrayList<String>> links_delete_area = (ArrayList) links_delete_info.get(area);
                                            if (links_delete_area != null) {
                                                //                                        links_delete_area.add(link);
                                                utils.addLink(link, links_delete_area);
                                            } else {
                                                ArrayList<ArrayList<String>> list_tmp = new ArrayList();
                                                list_tmp.add(link);
                                                links_delete_info.put(area, list_tmp);
                                            }
                                            utils.mapToFile(links_delete_info, Neb.links_delete, Neb.DELAY_WRITE_FILE);
                                        }
                                    } else {
                                        Neb.logger.Println("Error delete link: " + area + ": " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), Neb.logger.DEBUG);
                                    }
                                }
                                case "ADD_LINK" -> {
                                    String area = key_val[1];
                                    String link_str = key_val[2];
                                    String[] mas = link_str.split(";");
                                    ArrayList<String> link = new ArrayList(Arrays.asList(mas));
                                    if (INFO.get(area) != null) {
                                        Neb.logger.Println("Adding link: " + area + ": " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), Neb.logger.DEBUG);
                                        utils.addLink(link, (ArrayList) INFO.get(area).get("links"));
                                        Map links_add_info = utils.readJSONFile(Neb.links_adding);
                                        ArrayList<ArrayList<String>> links_add_area = (ArrayList) links_add_info.get(area);
                                        if (links_add_area != null) {
                                            links_add_area.add(link);
                                        } else {
                                            ArrayList<ArrayList<String>> list_tmp = new ArrayList();
                                            list_tmp.add(link);
                                            links_add_info.put(area, list_tmp);
                                        }
                                        utils.mapToFile(links_add_info, Neb.links_adding, Neb.DELAY_WRITE_FILE);
                                    } else {
                                        Neb.logger.Println("Error delete link: " + area + ": " + link.get(0) + " " + link.get(2) + " <---> " + link.get(3) + " " + link.get(5), Neb.logger.DEBUG);
                                    }
                                }
                                case "ADD_TO_LIST" -> {
                                    Neb.logger.Println("Adding to list: " + key + " value:" + val, Neb.logger.DEBUG);
                                    Neb.utils.addToList(key, val, INFO);
                                }
                                case "DEL_FROM_LIST" -> {
                                    Neb.logger.Println("Delete from list: " + key + " value:" + val, Neb.logger.DEBUG);
                                    Neb.utils.delFromList(key, val, INFO);
                                }
                            }
                        }
//                        Neb.logger.Println("Start write info to file: "+file+" ...", Neb.logger.DEBUG);
                        Neb.utils.mapToFile(INFO, file, Neb.DELAY_WRITE_FILE);
                        utils.mapToFile(node_attribute, Neb.node_attribute_old_file, Neb.DELAY_WRITE_FILE);
                      
//                        Neb.logger.Println("Stop write info to file: "+file, Neb.logger.DEBUG);
                    }
//                    System.out.println("Stop queue worker.");
                } catch(ConcurrentModificationException ex) { System.out.println("ConcurrentModificationException"); }
                finally {
                    busy = false;
                    Neb.logger.Println("QueueWorker = False", Neb.logger.DEBUG);
                }
            
            }
            try { Thread.sleep(sleep_timeout); } catch (InterruptedException e) { }
        }
    }
}
