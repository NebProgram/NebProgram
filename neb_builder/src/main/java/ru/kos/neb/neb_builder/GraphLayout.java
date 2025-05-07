package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import static ru.kos.neb.neb_builder.Neb.logger;


public class GraphLayout {
    private static final int LOOP_CICLE = 100;
    private static final long GRAPH_LAYOUT_PERIOD = 30*60;
//    private static final int reposition_otgig_cicle = 50;
//    private static final int otgig_cicle = 100;
    private static final double WIDTH = 1000;
    private static final double HIGHT = 1000;
    private static final double RND_DISTANCE = 500000;
    
    
    public static boolean PROCESSING_ALL_NODES_LAYOUT = false;
    
    private static String cur_area = "";
    
//    private static Map INFORMATION = new HashMap<>(); 

    private ArrayList getCenterLinkedToNode(String node, ArrayList<String[]> nodes_info, ArrayList<ArrayList<String>> links_info) {
        ArrayList result = new ArrayList();
        
        for(ArrayList<String> link : links_info) {
            String neighbor_name="";
            if(link.get(0).equals(node)) neighbor_name=link.get(3);
            if(link.get(3).equals(node)) neighbor_name=link.get(0);
            if(!neighbor_name.isEmpty()) {
                for(String[] item1 : nodes_info) {
                    if(item1[0].equals(neighbor_name)) {
                        double[] center = new double[2];
                        center[0]=Double.parseDouble(item1[1])+WIDTH/2;
                        center[1]=Double.parseDouble(item1[2])-HIGHT/2;
                        result.add(center);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    private double[] getCenter(double x, double y, double width, double hight) {
        double[] result = new double[2];
        result[0] = x+width/2;
        result[1] = y-hight/2;      
        return result;
    }
    
    private double[] getAttraction(String[] node_info, ArrayList<String[]> nodes_info, ArrayList<ArrayList<String>> links_info, double koefficient)
    {
        double[] attraction = new double[2];
//        double koefficient = 0.001;
        double forceX=0.; double forceY=0.;
        
        double x=Double.parseDouble(node_info[1]);
        double y=Double.parseDouble(node_info[2]);

        double[] center=getCenter(x, y, WIDTH, HIGHT);
        ArrayList coord_linked_nodes = getCenterLinkedToNode(node_info[0], nodes_info, links_info);
        for (Object coordLinkedNode : coord_linked_nodes) {
            double[] center_neighbor = (double[]) coordLinkedNode;
//            double distance1 = Math.sqrt(Math.pow(center_neighbor[0]-center[0], 2));
            double distance1 = center_neighbor[0] - center[0];
//            double forceX_link = koefficient*(distance1-min_distance);
//            double forceX_link = koefficient*Math.pow(distance1-min_distance, 3);
            double forceX_link = koefficient * distance1;
            double distance2 = center_neighbor[1] - center[1];
//            double forceY_link = koefficient*(distance2-min_distance);
//            double forceY_link = koefficient*Math.pow(distance2-min_distance, 3);
            double forceY_link = koefficient * distance2;
            forceX = forceX + forceX_link;
            forceY = forceY + forceY_link;
        }
        attraction[0]=forceX; attraction[1]=forceY;
        return attraction;
    }
    
    private double[] getRepulsion(String[] node_info, ArrayList<String[]> nodes_info, double electrical_repulsion)
    {
        double[] repulsion = new double[2];
        double forceX=0.; double forceY=0.;
        
        double x = Double.parseDouble(node_info[1]);
        double y = Double.parseDouble(node_info[2]);

        double[] center=getCenter(x, y, WIDTH, HIGHT);
        for(String[] item : nodes_info)
        {
            if(item[0].equals(node_info[0])) continue;
            double x1 = Double.parseDouble(item[1]);
            double y1 = Double.parseDouble(item[2]);
            double[] center_sec=getCenter(x1, y1, WIDTH, HIGHT);            

            double deltaX = (center_sec[0]-center[0]);
            double deltaY = (center_sec[1]-center[1]);
            
            // antistick
            double len = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
            double forceX_node = 0;
            double forceY_node = 0;
            if(len < WIDTH) {
                if(deltaX != 0) {
                    double force = 1000000000*electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                    forceX_node=force*deltaX/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                }
                if(deltaY != 0) {
                    double force = 1000000000*electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                    forceY_node=force*deltaY/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                }                
            } else {
                if(deltaX != 0) {
                    double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                    forceX_node=force*deltaX/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                }
                if(deltaY != 0) {
                    double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                    forceY_node=force*deltaY/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                }                 
            }
            
//            double forceX_node = 0;
//            if(deltaX != 0) {
//                double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
//                forceX_node=force*deltaX/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
//            }
//            double forceY_node = 0;
//            if(deltaY != 0) {
//                double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
//                forceY_node=force*deltaY/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
//            }
            forceX = forceX + forceX_node;
            forceY = forceY + forceY_node;
        }
        
        repulsion[0]=(-1)*forceX; repulsion[1]=(-1)*forceY;

        return repulsion;
    }
    
    private double[] magicWind(String[] node_info, double[] center_point, double wind_force)
    {
        double[] wind = new double[2];
        double forceX=0.; double forceY=0.;
        
        double x=Double.parseDouble(node_info[1]);
        double y=Double.parseDouble(node_info[2]);

        double[] center=getCenter(x, y, WIDTH, HIGHT);

        double deltaX = (center[0]-center_point[0]);
        double deltaY = (center[1]-center_point[1]);

        if(deltaX != 0) {
            double xforce=wind_force*deltaX;
            forceX=forceX+xforce;
        }
        if(deltaY != 0) {
            double yforce=wind_force*deltaY;
            forceY=forceY+yforce;
        }

        wind[0]=forceX; wind[1]=forceY;

        return wind;
    } 
    
    private double[] edgeRadius(String[] node_info, double[] center_point,
                                double edge_radius_force)
    {
        double[] edge = new double[2];
        double forceX=0.; double forceY=0.;
        
        double x=Double.parseDouble(node_info[1]);
        double y=Double.parseDouble(node_info[2]);

        double[] center=getCenter(x, y, WIDTH, HIGHT);

        double deltaX = (center[0]-center_point[0]);
        double deltaY = (center[1]-center_point[1]);

        double xforce=edge_radius_force*deltaX;
        forceX=forceX-xforce;
        double yforce=edge_radius_force*deltaY;
        forceY=forceY-yforce;        
//        if(length > maxRadius) {
//            double xforce=edge_radius_force*deltaX;
//            forceX=forceX-xforce;
//            double yforce=edge_radius_force*deltaY;
//            forceY=forceY-yforce;
//        } else {
//            forceX = 0;
//            forceY = 0;
//        }

        edge[0]=forceX; edge[1]=forceY;

        return edge;
    } 
    
    
    private ArrayList<String[]> graphLayout(ArrayList<String[]> processing_nodes_position, 
            ArrayList<String[]> all_nodes_position, 
            ArrayList<ArrayList<String>> processing_links) {
        double[] center = getCenterNodes(all_nodes_position);
        double maxRadius = 10 * Math.sqrt(all_nodes_position.size())*1000;
        if(Neb.cfg.get("areas") != null && 
                ((Map)Neb.cfg.get("areas")).get(cur_area) != null &&
                ((Map)((Map)Neb.cfg.get("areas")).get(cur_area)).get("canvas_size") != null)
        {
            long canvas_size = (long)((Map)((Map)Neb.cfg.get("areas")).get(cur_area)).get("canvas_size");
            maxRadius = (double)canvas_size;
        }
        for(String[] node_position : all_nodes_position) {
            double x_double=Double.parseDouble(node_position[1]);
            double y_double=Double.parseDouble(node_position[2]);
            double[] center_node=getCenter(x_double, y_double, WIDTH, HIGHT);
            double deltaX = (center_node[0]-center[0]);
            double deltaY = (center_node[1]-center[1]);
            double length = Math.sqrt(deltaX*deltaX+deltaY*deltaY);
            if(length > maxRadius)
                processing_nodes_position.add(node_position);
        }          

//        double x_min = Double.MAX_VALUE;
//        double x_max = (-1)*Double.MAX_VALUE;
//        double y_min = Double.MAX_VALUE;
//        double y_max = (-1)*Double.MAX_VALUE;        
//        for(String[] node_position : all_nodes_position) {
//            if(Double.parseDouble(node_position[1]) < x_min) x_min = Double.parseDouble(node_position[1]);
//            if(Double.parseDouble(node_position[1]) > x_max) x_max = Double.parseDouble(node_position[1]);
//            if(Double.parseDouble(node_position[2]) < y_min) y_min = Double.parseDouble(node_position[2]);
//            if(Double.parseDouble(node_position[2]) > y_max) y_max = Double.parseDouble(node_position[2]);            
//        }
        
        double increment = 5;
        double deceleration = increment/LOOP_CICLE;
        long start_time = System.currentTimeMillis();
        for(int loop=0; loop<LOOP_CICLE; loop++) {
            long cur_time = System.currentTimeMillis();
            if((cur_time-start_time)/1000 > GRAPH_LAYOUT_PERIOD) break;
//            if(processing_nodes_position.isEmpty()) break;
            
            
            ArrayList<String[]> links_position = new ArrayList();
            for(ArrayList<String> link : processing_links) {
                String[] link_position = new String[10];
                for(String[] node_position : all_nodes_position) {
                    if(link_position[0] != null && link_position[5] != null)
                        break;
                    if(link.get(0).equals(node_position[0])) {
                        link_position[0] = link.get(0);
                        link_position[1] = link.get(1);
                        link_position[2] = link.get(2);
                        link_position[3] = node_position[1];
                        link_position[4] = node_position[2];
                    }
                    if(link.get(3).equals(node_position[0])) {
                        link_position[5] = link.get(3);
                        link_position[6] = link.get(4);
                        link_position[7] = link.get(5);
                        link_position[8] = node_position[1];
                        link_position[9] = node_position[2];
                    }                    
                }
                links_position.add(link_position);
            }
            ArrayList<ArrayList> links_length = new ArrayList();
            for(String[] link_position : links_position) {
                ArrayList link_length = new ArrayList();
                link_length.add(link_position[0]);
                link_length.add(link_position[1]);
                link_length.add(link_position[2]);
                link_length.add(link_position[5]);
                link_length.add(link_position[6]);
                link_length.add(link_position[7]);
                double len = 0;
                if(link_position[3] != null && link_position[4] != null && 
                        link_position[8] != null && link_position[9] != null) {
                    len = Math.abs(Double.parseDouble(link_position[8])-Double.parseDouble(link_position[3])) + Math.abs(Double.parseDouble(link_position[9])-Double.parseDouble(link_position[4]));
                }
                link_length.add(len);
                links_length.add(link_length);
            }
            
            Map<String, Double> node_max_length_link = new HashMap();
            for(ArrayList link_length : links_length) {
                String node1 = (String)link_length.get(0);
                String node2 = (String)link_length.get(3);
                double len = (Double)link_length.get(6);
                if(node_max_length_link.get(node1) != null) {
                    if(node_max_length_link.get(node1) < len)
                        node_max_length_link.put(node1, len);
                } else
                    node_max_length_link.put(node1, len);
                if(node_max_length_link.get(node2) != null) {
                    if(node_max_length_link.get(node2) < len)
                        node_max_length_link.put(node2, len);
                } else
                    node_max_length_link.put(node2, len);                
            }
            
//            System.out.println("\tprocessing_nodes_position size="+processing_nodes_position.size());

            for(String[] node_position : processing_nodes_position) {                                  
                double koef = 1;
                double[] attraction = getAttraction(node_position, all_nodes_position, processing_links, koef);

                double koef1 = 1;
                double[] repulsion = getRepulsion(node_position, all_nodes_position, koef1);

                double koef2 = 0.001;
                double[] wind = magicWind(node_position, center, koef2);

                double edge_radius_force = 0.1;
                double[] edge = edgeRadius(node_position, center, edge_radius_force);

                double xattr=attraction[0]; double yattr=attraction[1];
                double xrepuls=repulsion[0]; double yrepuls=repulsion[1];
                double xwind=wind[0]; double ywind=wind[1];
                double xedge=edge[0]; double yedge=edge[1];
                double xforce=xattr + xrepuls + xwind + xedge;
                double yforce=yattr + yrepuls + ywind + yedge;                
                double x_delta=xforce*increment; 
                double y_delta=yforce*increment;
                ////////////////////////////////
                if(node_max_length_link.get(node_position[0]) != null) {
                    double max_len = node_max_length_link.get(node_position[0]);
                    double max_delta = Math.abs(x_delta);
                    if(Math.abs(y_delta) > Math.abs(x_delta)) {
                        max_delta = Math.abs(y_delta);
                    }
                    if(max_delta > max_len/10) {
                        x_delta=(max_len/(10*max_delta))*x_delta;
                        y_delta=(max_len/(10*max_delta))*y_delta;
                    }
                } else {
                    double max_len = Math.sqrt(Math.pow((center[0]-Double.parseDouble(node_position[1])), 2) + Math.pow((center[1]-Double.parseDouble(node_position[2])), 2));
                    double max_delta = Math.abs(x_delta);
                    if(Math.abs(y_delta) > Math.abs(x_delta)) {
                        max_delta = Math.abs(y_delta);
                    }                    
                    if(max_delta > max_len/10) {
                        x_delta=(max_len/(10*max_delta))*x_delta;
                        y_delta=(max_len/(10*max_delta))*y_delta;
                    }
                    
                }
                String x = String.valueOf(Double.parseDouble(node_position[1])+x_delta);
                String y = String.valueOf(Double.parseDouble(node_position[2])+y_delta);
                
                node_position[1] = x;
                node_position[2] = y;

                for(String[] item1 : all_nodes_position) {
                    if(node_position[0].equals(item1[0])) {
                        item1[1] = node_position[1];
                        item1[2] = node_position[2];
                    }
                }                

            }
            increment = increment - deceleration;
            System.out.println("loop="+loop);
        }
        
        return processing_nodes_position;
    }
        
    ////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////
    public Map startGraphLayout(Map INFORMATION) {
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)INFORMATION).entrySet()) {
            String area = entry.getKey();
            cur_area = area;
            logger.Println("Graph layout: "+area, logger.DEBUG);
            System.out.println("Graph layout: "+area);
            Map area_info = entry.getValue();

            ArrayList<String[]> all_nodes_position = runGraphLayout(area_info);
            
            for(String[] item : all_nodes_position) {
                if(area_info.get("nodes_information") != null && ((Map)area_info.get("nodes_information")).get(item[0]) != null) {
                    String[] coord = new String[2];
                    coord[0] = item[1];
                    coord[1] = item[2];
                    ((Map)((Map)area_info.get("nodes_information")).get(item[0])).put("xy", coord);
                }
            }
        }
        return INFORMATION;
    }
    
    private ArrayList<String[]> runGraphLayout(Map area_info) {
        ArrayList<String[]> all_nodes_position = new ArrayList();
        Map<String, String> processing_nodes_map = new HashMap();
        
        if(area_info.get("nodes_information") != null && area_info.get("links") != null) {
            
            Map<String, Map> nodes_info = (Map)area_info.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList)area_info.get("links");
        
            ArrayList<String[]> processing_nodes_position = new ArrayList();
            ArrayList<ArrayList<String>> processing_links = new ArrayList();
//            Map<String, String> processing_nodes_map = new HashMap();
            for(Map.Entry<String, Map> item : nodes_info.entrySet()) {
                String node = item.getKey(); 
                Map val = item.getValue();
                if(!PROCESSING_ALL_NODES_LAYOUT) {
                    if(val.get("xy") == null) {
                        double x = RND_DISTANCE * new Random().nextDouble();
                        double y = RND_DISTANCE * new Random().nextDouble();
                        String[] mas_tmp = new String[3];
                        mas_tmp[0] = node;
                        mas_tmp[1] = String.valueOf(x);
                        mas_tmp[2] = String.valueOf(y);
                        processing_nodes_map.put(node, node);
                        processing_nodes_position.add(mas_tmp);
                        all_nodes_position.add(mas_tmp);
                        logger.Println("Processing node layout: "+node, logger.DEBUG);
                    } else {
                        String x = (String)((ArrayList)val.get("xy")).get(0);
                        String y = (String)((ArrayList)val.get("xy")).get(1);
                        String[] mas_tmp = new String[3];
                        mas_tmp[0] = node;
                        mas_tmp[1] = x;
                        mas_tmp[2] = y;
                        all_nodes_position.add(mas_tmp);
                    }
                } else {
                    double x = RND_DISTANCE * new Random().nextDouble();
                    double y = RND_DISTANCE * new Random().nextDouble();
                    String[] mas_tmp = new String[3];
                    mas_tmp[0] = node;
                    mas_tmp[1] = String.valueOf(x);
                    mas_tmp[2] = String.valueOf(y);
                    processing_nodes_map.put(node, node);
                    processing_nodes_position.add(mas_tmp);
                    all_nodes_position.add(mas_tmp);
                    logger.Println("Processing node layout: "+node, logger.DEBUG);                    
                }
            }
            
            for(ArrayList<String> link : links) {
                if(processing_nodes_map.get(link.get(0)) != null || processing_nodes_map.get(link.get(3)) != null) {
                    processing_links.add(link);
                }
            }

//            ArrayList<String[]> processing_nodes_position_new = new ArrayList();
//            for(String[] item1 : processing_nodes_position) {
//                String node1 = item1[0];
//                for(ArrayList<String> link : processing_links) {
//                    if(node1.equals(link.get(0)) || node1.equals(link.get(3))) {
//                        processing_nodes_position_new.add(item1);
//                        break;
//                    }
//                }
//            }
//            processing_nodes_position = processing_nodes_position_new;
            
            processing_nodes_position = graphLayout(processing_nodes_position, all_nodes_position, processing_links);
        
//            logger.Println("### Correct new nodes positions ###", logger.DEBUG);
//            all_nodes_position = correctNewNodesPosition(all_nodes_position, processing_nodes_map, center);
//            logger.Println("### Correct All nodes positions ###", logger.DEBUG);
//            all_nodes_position = correctAllNodesPosition(all_nodes_position, center);
        }
        
        return all_nodes_position;
    }
    
    private double[] getCenterNodes(ArrayList<String[]> nodes_position) {
        double x_sum = 0;
        double y_sum = 0;

        for(String[] iter : nodes_position) {
            x_sum = x_sum + Double.parseDouble(iter[1]);
            y_sum = y_sum + Double.parseDouble(iter[2]);
        }            
        double[] out = new double[2];
        out[0] = x_sum/nodes_position.size();
        out[1] = y_sum/nodes_position.size();
        return out;
    }

}

