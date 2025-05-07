package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Random;


public class GraphLayoutAnnealing {
    /**
     * Whether or not the distance between edge and nodes will be calculated
     * as an energy cost function. This function is CPU intensive and is best
     * only used in the fine tuning phase.
     */
    private boolean isOptimizeEdgeDistance = true;

    /**
     * Whether or not edges crosses will be calculated as an energy cost
     * function. This function is CPU intensive, though if some iterations
     * without it are required, it is best to have a few cycles at the start
     * of the algorithm using it, then use it intermittantly through the rest
     * of the layout.
     */
    private boolean isOptimizeEdgeCrossing = true;

    /**
     * Whether or not edge lengths will be calculated as an energy cost
     * function. This function not CPU intensive.
     */
    private boolean isOptimizeEdgeLength = true;

    /**
     * Whether or not nodes will contribute an energy cost as they approach
     * the bound of the graph. The cost increases to a limit close to the
     * border and stays constant outside the bounds of the graph. This function
     * is not CPU intensive
     */
    private boolean isOptimizeBorderLine = true;

    /**
     * Whether or not node distribute will contribute an energy cost where
     * nodes are close together. The function is moderately CPU intensive.
     */
    private boolean isOptimizeNodeDistribution = true;
    /**
     * Whether or not fine tuning is on. The determines whether or not
     * node to edge distances are calculated in the total system energy.
     * This cost function , besides detecting line intersection, is a
     * performance intensive component of this algorithm and best left
     * to optimization phase. <code>isFineTuning</code> is switched to
     * <code>true</code> if and when the <code>fineTuningRadius</code>
     * radius is reached. Switching this variable to <code>true</code>
     * before the algorithm runs mean the node to edge cost function
     * is always calculated.
     */
    private boolean isFineTuning = false;
    
    
    /**
     * Limit to the number of iterations that may take place. This is only
     * reached if one of the termination conditions does not occur first.
     */
    private int maxIterations = 1000;
    /**
     * Size node image in pixels
     */
    private int sizeNode = 500;
    /**
     * prevents from dividing with zero and from creating excessive energy
     * values
     */
    private double minDistanceLimit = 2;
    /**
     * distance limit beyond which energy costs due to object repulsive is
     * not calculated as it would be too insignificant
     */
    private double maxDistanceLimit = 10000;
    
    /**
     * Cost factor applied to energy calculations involving the general node
     * distribution of the graph. Increasing this value tends to result in
     * a better distribution of nodes across the available space, at the
     * partial cost of other graph aesthetics.
     * <code>isOptimizeNodeDistribution</code> must be true for this general
     * distribution to be applied.
     */
    private double nodeDistributionCostFactor = 1;
    /**
     * Cost factor applied to energy calculations involving the distance
     * nodes and edges. Increasing this value tends to cause nodes to move away
     * from edges, at the partial cost of other graph aesthetics.
     * <code>isOptimizeEdgeDistance</code> must be true for edge to nodes
     * distances to be taken into account.
     */
    private double edgeDistanceCostFactor = 1;
    /**
     * Cost factor applied to energy calculations involving edges that cross
     * over one another. Increasing this value tends to result in fewer edge
     * crossings, at the partial cost of other graph aesthetics.
     * <code>isOptimizeEdgeCrossing</code> must be true for edge crossings
     * to be taken into account.
     */
//    private double edgeCrossingCostFactor = 6000;
//    private double edgeCrossingCostFactor = 5;
    private double edgeCrossingCostFactor = 1000000;
    /**
     * The x coordinate of the final graph
     */
    private double boundsX = 0.0;
    /**
     * The y coordinate of the final graph
     */
    private double boundsY = 0.0;
    /**
     * The width coordinate of the final graph
     */
    private double boundsWidth = 0.0;
    /**
     * The height coordinate of the final graph
     */
    private double boundsHeight = 0.0;
    /**
     * Cost factor applied to energy calculations for node promixity to the
     * notional border of the graph. Increasing this value results in
     * nodes tending towards the centre of the drawing space, at the
     * partial cost of other graph aesthetics.
     * <code>isOptimizeBorderLine</code> must be true for border
     * repulsion to be applied.
     */
    private double borderLineCostFactor = 5;
    /**
     * Cost factor applied to energy calculations for the edge lengths.
     * Increasing this value results in the layout attempting to shorten all
     * edges to the minimum edge length, at the partial cost of other graph
     * aesthetics.
     * <code>isOptimizeEdgeLength</code> must be true for edge length
     * shortening to be applied.
     */
    private double edgeLengthCostFactor = 0.02;
//    private double edgeLengthCostFactor = 0.000000002;
    /**
     * determines, in how many segments the circle around cells is divided, to
     * find a new position for the cell. Doubling this value doubles the CPU
     * load. Increasing it beyond 16 might mean a change to the
     * <code>performRound</code> method might further improve accuracy for a
     * small performance hit. The change is described in the method comment.
     */
    private int triesPerCell = 8;
    /**
     * The current radius around each node where the next position energy
     * values will be calculated for a possible move
     */
    private double moveRadiusDefault = 1000;
    /**
     * Keeps track of how many consecutive round have passed without any energy
     * changes 
     */
//    private int unchangedEnergyRoundCount = 0;
    /**
     * The number of round of no node moves taking placed that the layout
     * terminates
     */
//    private int unchangedEnergyRoundTermination = 5;
    /**
     * current iteration number of the layout
     */
    private int iteration;
    /**
     * The factor by which the <code>moveRadius</code> is multiplied by after
     * every iteration. A value of 0.75 is a good balance between performance
     * and aesthetics. Increasing the value provides more chances to find
     * minimum energy positions and decreasing it causes the minimum radius
     * termination condition to occur more quickly.
     */
    private double radiusScaleFactor = 0.75;
//    private double radiusScaleFactor = 0.9;
    /**
     * when {@link #moveRadius}reaches this value, the algorithm is terminated
     */
    private double minMoveRadius = sizeNode;
    /**
     * The radius below which fine tuning of the layout should start
     * This involves allowing the distance between nodes and edges to be
     * taken into account in the total energy calculation. If this is set to
     * zero, the layout will automatically determine a suitable value
     */
    private double fineTuningRadius = minMoveRadius*40;

    double energy_prev = Double.MAX_VALUE;
    double energyKoef = 1000000;
//    double energy_min = Double.MAX_VALUE;
//    Map<String, Double[]> node_coord_min = new HashMap();
    
    
    
    public GraphLayoutAnnealing() {
        
    } 

    public Map<String, Map> execute(Map<String, Map> INFO) {
        // Setup the normal vectors for the test points to move each vertex to
        double[] xNormTry = new double[triesPerCell];
        double[] yNormTry = new double[triesPerCell];
        for (int i = 0; i < triesPerCell; i++)
        {
            double angle = i*((2.0*Math.PI)/triesPerCell);
            xNormTry[i] = Math.cos(angle);
            yNormTry[i] = Math.sin(angle);
        }
        
        for (Map.Entry<String, Map> area : ((Map<String, Map>) INFO).entrySet()) {
            String area_name = area.getKey();
            System.out.println("area - "+area_name);
            Map val = area.getValue();
        
            Map<String, Map> nodes_information = (Map)val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList) val.get("links");
            int num_nodes = nodes_information.size();
            boundsX = -(2 * Math.sqrt(num_nodes)*500*10);
            boundsY = -(2 * Math.sqrt(num_nodes)*500*10);
            boundsWidth = 2 * Math.sqrt(num_nodes)*500*10;
            boundsHeight = 2 * Math.sqrt(num_nodes)*500*10;
            maxDistanceLimit = boundsWidth;
//            energy_min = Double.MAX_VALUE;
            
//            moveRadius = boundsWidth/20.0;
            
            Map<String, Double[]> node_coord = new HashMap();
            for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                String node = entry.getKey();
                ArrayList<String> xy = (ArrayList)entry.getValue().get("xy");
                if(xy != null && xy.size() == 2) {
                    Double[] mas = new Double[2];
                    mas[0] = Double.valueOf(xy.get(0));
                    mas[1] = Double.valueOf(xy.get(1));
                    node_coord.put(node, mas);
                } else {
                    Double[] mas = new Double[2];
                    mas[0] = boundsWidth * new Random().nextDouble();
                    mas[1] = boundsWidth * new Random().nextDouble();
                    node_coord.put(node, mas);                  
                }
            }
            
            Map<String, ArrayList<Double[]>> node_ConnectedLinksCoord = new HashMap();
            for(ArrayList<String> link : links) {
                String node1 = link.get(0);
                String node2 = link.get(3);
                Double[] mas = new Double[4];
                mas[0] = node_coord.get(node1)[0];
                mas[1] = node_coord.get(node1)[1];
                mas[2] = node_coord.get(node2)[0];
                mas[3] = node_coord.get(node2)[1];                
                    
                if(node_ConnectedLinksCoord.get(node1) == null) {
                    ArrayList<Double[]> list_tmp = new ArrayList();
                    list_tmp.add(mas);
                    node_ConnectedLinksCoord.put(node1, list_tmp);
                } else {
                    node_ConnectedLinksCoord.get(node1).add(mas);
                }
                if(node_ConnectedLinksCoord.get(node2) == null) {
                    ArrayList<Double[]> list_tmp = new ArrayList();
                    list_tmp.add(mas);
                    node_ConnectedLinksCoord.put(node2, list_tmp);
                } else {
                    node_ConnectedLinksCoord.get(node2).add(mas);
                }
            }
            
            // adding max length connected links
            for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
                String node = entry.getKey();
                Double[] coord = entry.getValue();
                double max_length = 0;
                if(node_ConnectedLinksCoord.get(node) != null) {
                    for(Double[] link_coord : node_ConnectedLinksCoord.get(node))  {
                        double edgeLength = Point2D.distance(link_coord[0], link_coord[1], 
                                link_coord[2], link_coord[3]);
                        if(edgeLength > max_length) {
                            max_length = edgeLength;
                        }
                            
                    }
                } else {
                    max_length = moveRadiusDefault;
                }
                Double[] coord_new = new Double[3];
                coord_new[0] = coord[0];
                coord_new[1] = coord[1];
                coord_new[2] = max_length;
                node_coord.put(node, coord_new);
            }
            
            Map<String, ArrayList<Double[]>> node_NotConnectedLinksCoord = new HashMap();
            for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                String node = entry.getKey();
                for(ArrayList<String> link : links) {
                    String node1 = link.get(0);
                    String node2 = link.get(3);  
                    if(!node.equals(node1) && !node.equals(node2)) {
                        Double[] mas = new Double[4];
                        mas[0] = node_coord.get(node1)[0];
                        mas[1] = node_coord.get(node1)[1];
                        mas[2] = node_coord.get(node2)[0];
                        mas[3] = node_coord.get(node2)[1];                         
                        if(node_NotConnectedLinksCoord.get(node) == null) {
                            ArrayList<Double[]> list_tmp = new ArrayList();
                            list_tmp.add(mas);
                            node_NotConnectedLinksCoord.put(node, list_tmp);
                        } else {
                            node_NotConnectedLinksCoord.get(node).add(mas);
                        }                        
                    }
                }
            }
        
//            node_coord = performRound(node_coord, node_ConnectedLinksCoord, 
//                    node_NotConnectedLinksCoord, links, xNormTry, yNormTry);            
            // The main layout loop
            for (iteration = 0; iteration < maxIterations; iteration++)
            {
                System.out.println("\tIteration - "+iteration);
                node_coord = performRound(node_coord, node_ConnectedLinksCoord, 
                        node_NotConnectedLinksCoord, links, xNormTry, yNormTry);
            }
            // update info
            for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                String node = entry.getKey();
                if(node_coord.get(node) != null) {
                    double x = node_coord.get(node)[0];
                    double y = node_coord.get(node)[1];
                    ArrayList<String> xy = (ArrayList)entry.getValue().get("xy");
                    if(xy != null) {
                        xy.set(0, String.valueOf(x));
                        xy.set(1, String.valueOf(y));
                    } else {
                        ArrayList list_tmp = new ArrayList();
                        list_tmp.add(String.valueOf(x));
                        list_tmp.add(String.valueOf(y));
                        entry.getValue().put("xy", list_tmp);
                    }
                }
            }
        }
        return INFO;
    }

    /**
     * The main round of the algorithm. Firstly, a permutation of nodes
     * is created and worked through in that random order. Then, for each node
     * a number of point of a circle of radius <code>moveRadius</code> are
     * selected and the total energy of the system calculated if that node
     * were moved to that new position. If a lower energy position is found
     * this is accepted and the algorithm moves onto the next node. There
     * may be a slightly lower energy value yet to be found, but forcing
     * the loop to check all possible positions adds nearly the current
     * processing time again, and for little benefit. Another possible
     * strategy would be to take account of the fact that the energy values
     * around the circle decrease for half the loop and increase for the
     * other, as a general rule. If part of the decrease were seen, then
     * when the energy of a node increased, the previous node position was
     * almost always the lowest energy position. This adds about two loop
     * iterations to the inner loop and only makes sense with 16 tries or more.
     */
    private Map<String, Double[]> performRound(Map<String, Double[]> node_coord, 
            Map<String, ArrayList<Double[]>> node_ConnectedLinksCoord, 
            Map<String, ArrayList<Double[]>> node_NotConnectedLinksCoord,
            ArrayList<ArrayList<String>> links, double[] xNormTry, double[] yNormTry) {
        // sequential order cells are computed (every round the same order)

        // boolean to keep track of whether any moves were made in this round
        boolean energyHasChanged = false;
        double energy = 0;
        for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
            String node = entry.getKey();

//            System.out.println("\t"+node);
//            double nodeX = node_coord.get(node)[0];
//            double nodeY = node_coord.get(node)[1];
//            double length = node_coord.get(node)[2];
//            if(node.equals("10.64.20.66") && length == 1324.9100670406444)
//                System.out.println("length="+String.valueOf(length));
            double moveRadius = node_coord.get(node)[2]/2;
            if (moveRadius <= minMoveRadius)
            {
                continue;
            }             
            // Obtain the energies for the node is its current position
            // TODO The energy could be stored from the last iteration
            // and used again, rather than re-calculate
            double oldNodeDistribution = getNodeDistribution(node, node_coord);
            double oldEdgeDistance = getEdgeDistanceFromNode(node, node_coord, node_NotConnectedLinksCoord);
            oldEdgeDistance += getEdgeDistanceAffectedNodes(node, node_coord, node_ConnectedLinksCoord);
            double oldEdgeCrossing = getEdgeCrossingAffectedEdges(node_coord, links);
            double oldBorderLine = getBorderline(node, node_coord);
            double oldEdgeLength = getEdgeLengthAffectedEdges(node, node_coord, node_ConnectedLinksCoord);
            double oldAdditionFactors = getAdditionFactorsEnergy();

            double minEnergyDelta = Double.MAX_VALUE;
            Double[] mas_min = new Double[3];
            while(moveRadius > minMoveRadius) {
                for (int j = 0; j < triesPerCell; j++) {
                    double movex = moveRadius * xNormTry[j];
                    double movey = moveRadius * yNormTry[j];

                    // applying new move
                    double oldx = node_coord.get(node)[0];
                    double oldy = node_coord.get(node)[1];
                    node_coord.get(node)[0] = node_coord.get(node)[0] + movex;
                    node_coord.get(node)[1] = node_coord.get(node)[1] + movey;

                    // calculate the energy delta from this move
                    double energyDelta = calcEnergyDelta(node, node_coord,
                        node_ConnectedLinksCoord, 
                        node_NotConnectedLinksCoord,
                        links,
                        oldNodeDistribution, oldEdgeDistance, oldEdgeCrossing,
                        oldBorderLine, oldEdgeLength, oldAdditionFactors);

                    if (energyDelta < 0)
                    {
                        // energy of moved node is lower, finish tries for this
                        // node
                        energyHasChanged = true;
                        if(energyDelta < minEnergyDelta) {
                            minEnergyDelta = energyDelta/energyKoef;
                            mas_min[0] = node_coord.get(node)[0];
                            mas_min[1] = node_coord.get(node)[1];
                            mas_min[2] = node_coord.get(node)[2];
                        }
    //                    break; // exits loop
                    }
                    node_coord.get(node)[0] = oldx;
                    node_coord.get(node)[1] = oldy;
                }
                moveRadius = moveRadius * radiusScaleFactor;
            }
            if(mas_min[0] != null) {
                mas_min[2] = node_coord.get(node)[2] * radiusScaleFactor;                
                node_coord.put(node, mas_min);
            } else {
                node_coord.get(node)[2] = node_coord.get(node)[2] * radiusScaleFactor;               
            }
            energy = energy + Math.abs(minEnergyDelta);
        }
        if (!energyHasChanged)
        {
            iteration = maxIterations;
        } else {
            System.out.println("Iteration - "+String.valueOf(iteration)+"  energy = "+String.valueOf(energy));
        }
//        if(energy > energy_prev) {
//            iteration = maxIterations;
//        }

//        if(energy < energy_min) {
//            energy_min = energy;
//            node_coord_min = new HashMap();
//            for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
//                String node = entry.getKey();
//                Double[] val = entry.getValue();
//                Double[] val_new = new Double[3];
//                val_new[0] = val[0];
//                val_new[1] = val[1];
//                val_new[2] = val[2];
//                node_coord_min.put(node, val_new);
//            }
//        } else {
//            System.out.println("11111111111");
//            for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
//                String node = entry.getKey();
//                Double[] val1 = entry.getValue();
//                Double[] val2 = node_coord_min.get(node);
//                if(val1[0] != val2[0] || val1[1] != val2[1]) {
//                    System.out.println("11111111111");
//                }
//            }
//        }
        energy_prev = energy;

        return node_coord;
    }

    
    /**
     * Calculates the energy cost of the specified node relative to all other
     * nodes. Basically produces a higher energy the closer nodes are together.
     * 
     */
    private double getNodeDistribution(String node, Map<String, Double[]> node_coord) {
        double energy = 0.0;
        if (isOptimizeNodeDistribution == true) {
            double nodeX = node_coord.get(node)[0];
            double nodeY = node_coord.get(node)[1];
            for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
                String vertex = entry.getKey();
                double vertexX = node_coord.get(vertex)[0];
                double vertexY = node_coord.get(vertex)[1];                
                if (!vertex.equals(node)) {
                    double vx = nodeX - vertexX;
                    double vy = nodeY - vertexY;
                    double distanceSquared = vx*vx + vy*vy;
                    distanceSquared = distanceSquared - 2*sizeNode*sizeNode;

                    // prevents from dividing with Zero.
                    if (distanceSquared < minDistanceLimit*minDistanceLimit)
                    {
                            distanceSquared = minDistanceLimit*minDistanceLimit;
                    }
                    energy += nodeDistributionCostFactor/distanceSquared;
                }
            }
        }
        return energy;
    }
    
    /**
     * This method calculates the energy of the distance between Cells and
     * Edges. This version of the edge distance cost calculates the energy
     * cost from a specified <strong>node</strong>. The distance cost to all
     * unconnected edges is calculated and the total returned.
     */
    private double getEdgeDistanceFromNode(String node, Map<String, Double[]> node_coord, 
            Map<String, ArrayList<Double[]>> node_NotConnectedLinksCoord) {
        double energy = 0.0;
        // This function is only performed during fine tuning for performance
        if (isOptimizeEdgeDistance && isFineTuning) {
            if(node_NotConnectedLinksCoord.get(node) != null) {
                for(Double[] links_not_connected : node_NotConnectedLinksCoord.get(node)) {
                    double distSquare = Line2D.ptSegDistSq(links_not_connected[0], links_not_connected[1], 
                            links_not_connected[2], links_not_connected[3], 
                            node_coord.get(node)[0], node_coord.get(node)[1]);
                    distSquare = distSquare - sizeNode*sizeNode;
                    // prevents from dividing with Zero. No Math.abs() call
                    // for performance
                    if (distSquare < minDistanceLimit*minDistanceLimit)
                    {
                            distSquare = minDistanceLimit*minDistanceLimit;
                    }

                    // Only bother with the divide if the node and edge are
                    // fairly close together
                    if (distSquare < maxDistanceLimit*maxDistanceLimit)
                    {
                            energy += edgeDistanceCostFactor/distSquare;
                    }                    
                    
                }
            }

        }
        return energy;
    }
    
    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeDistanceFromEdge</code> for all
     * edges connected to the specified node
     * 				the node whose connected edges cost functions are to be
     * 				calculated
     */
    private double getEdgeDistanceAffectedNodes(String node, Map<String, Double[]> node_coord, 
            Map<String, ArrayList<Double[]>> node_ConnectedLinksCoord)
    {
        double energy = 0.0;
        if(node_ConnectedLinksCoord.get(node) != null) {
            for(Double[] link_coord : node_ConnectedLinksCoord.get(node)) {
                energy += getEdgeDistanceFromEdge(node_coord, link_coord);
            }
        }

        return energy;
    }
    
    /**
     * This method calculates the energy of the distance between Cells and
     * Edges. This version of the edge distance cost calculates the energy
     * cost from a specified <strong>edge</strong>. The distance cost to all
     * unconnected nodes is calculated and the total returned.
     */
    private double getEdgeDistanceFromEdge(Map<String, Double[]> node_coord, Double[] link_coord) {
        double energy = 0.0;
        // This function is only performed during fine tuning for performance
        if (isOptimizeEdgeDistance && isFineTuning) {
            for (Map.Entry<String, Double[]> entry : node_coord.entrySet()) {
                String vertex = entry.getKey();
                double vertexX = node_coord.get(vertex)[0];
                double vertexY = node_coord.get(vertex)[1];
                double distSquare = Line2D.ptSegDistSq(link_coord[0], link_coord[1], 
                        link_coord[2], link_coord[3], vertexX, vertexY); 
                distSquare = distSquare - sizeNode*sizeNode;
                // prevents from dividing with Zero. No Math.abs() call
                // for performance
                if (distSquare < minDistanceLimit*minDistanceLimit)
                {
                        distSquare = minDistanceLimit*minDistanceLimit;
                }

                // Only bother with the divide if the node and edge are
                // fairly close together
                if (distSquare < maxDistanceLimit*maxDistanceLimit)
                {
                        energy += edgeDistanceCostFactor/distSquare;
                }                    
                
            }
        }
        return energy;
    }

    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeCrossing</code> for all
     * edges connected to the specified node
     */
    private double getEdgeCrossingAffectedEdges(Map<String, Double[]> node_coord,
            ArrayList<ArrayList<String>> links)
    {
        double energy = 0.0;
        ArrayList<ArrayList<String>> links_new = new ArrayList();
        for(ArrayList<String> link : links) {
            ArrayList<String> link_new = new ArrayList();
            for(String item : link) {
                link_new.add(item);
            }
            links_new.add(link_new);
        }
        for(ArrayList<String> link : links_new) {
            if(!link.get(0).equals("blank")) {
                Double[] link_coord = new Double[4];
                link_coord[0] = node_coord.get(link.get(0))[0];
                link_coord[1] = node_coord.get(link.get(0))[1];
                link_coord[2] = node_coord.get(link.get(3))[0];
                link_coord[3] = node_coord.get(link.get(3))[1];
                link.set(0, "blank");
                energy += getEdgeCrossing(node_coord, link_coord, links_new);
            }
        }
        return energy;
    }

    /**
     * This method calculates the energy of the distance from the specified
     * edge crossing any other edges. Each crossing add a constant factor
     * to the total energy
     */
    private double getEdgeCrossing(Map<String, Double[]> node_coord, 
            Double[] link_coord, ArrayList<ArrayList<String>> links) {
        // TODO Could have a cost function per edge
        int n = 0; // counts energy of edgecrossings through edge i

        // max and min variable for minimum bounding rectangles overlapping
        // checks
        double minjX, minjY, miniX, miniY, maxjX, maxjY, maxiX, maxiY;

        if (isOptimizeEdgeCrossing) {
            double iP1X = link_coord[0];
            double iP1Y = link_coord[1];
            double iP2X = link_coord[2];
            double iP2Y = link_coord[3];

            for(ArrayList<String> link : links) {
                if(!link.get(0).equals("blank")) {
                    double jP1X = node_coord.get(link.get(0))[0];
                    double jP1Y = node_coord.get(link.get(0))[1];
                    double jP2X = node_coord.get(link.get(3))[0];
                    double jP2Y = node_coord.get(link.get(3))[1];
                    link.set(0, "blank");
                    if(!(iP1X == jP1X && iP1Y == jP1Y && iP2X == jP2X && iP2Y == jP2Y)) {
                        // First check is to see if the minimum bounding rectangles
                        // of the edges overlap at all. Since the layout tries
                        // to separate nodes and shorten edges, the majority do not
                        // overlap and this is a cheap way to avoid most of the
                        // processing
                        // Some long code to avoid a Math.max call...
                        if (iP1X < iP2X)
                        {
                            miniX = iP1X;
                            maxiX = iP2X;
                        }
                        else
                        {
                            miniX = iP2X;
                            maxiX = iP1X;
                        }
                        if (jP1X < jP2X)
                        {
                            minjX = jP1X;
                            maxjX = jP2X;
                        }
                        else
                        {
                            minjX = jP2X;
                            maxjX = jP1X;
                        }
                        if (maxiX < minjX || miniX > maxjX)
                        {
                            continue;
                        }

                        if (iP1Y < iP2Y)
                        {
                            miniY = iP1Y;
                            maxiY = iP2Y;
                        }
                        else
                        {
                            miniY = iP2Y;
                            maxiY = iP1Y;
                        }
                        if (jP1Y < jP2Y)
                        {
                            minjY = jP1Y;
                            maxjY = jP2Y;
                        }
                        else
                        {
                            minjY = jP2Y;
                            maxjY = jP1Y;
                        }
                        if (maxiY < minjY || miniY > maxjY)
                        {
                            continue;
                        }

//                        if( !((maxiX < minjX || maxiY < minjY) || (maxjX < miniX || maxjY < miniY) ||
//                                (miniX > maxjX || miniY > maxjY) || (minjX > maxiX || minjY < maxiY)) ) {
                            // Ignore if any end points are coincident
                        if (((iP1X != jP1X) && (iP1Y != jP1Y))
                                        && ((iP1X != jP2X) && (iP1Y != jP2Y))
                                        && ((iP2X != jP1X) && (iP2Y != jP1Y))
                                        && ((iP2X != jP2X) && (iP2Y != jP2Y)))
                        {
                            boolean intersects = Line2D.linesIntersect(iP1X, iP1Y, iP2X, iP2Y, jP1X, jP1Y, jP2X, jP2Y);
                            if (intersects)
                            {
                                    n++;
                            }
                        }
//                        }
                    }
                }
            }
        }
        return edgeCrossingCostFactor * n;
    }
    
    /**
     * This method calculates the energy of the distance of the specified
     * node to the notional border of the graph. The energy increases up to
     * a limited maximum close to the border and stays at that maximum
     * up to and over the border.
     */
    private double getBorderline(String node, Map<String, Double[]> node_coord) {
        double energy = 0.0;
        if (isOptimizeBorderLine) {
            double nodeX = node_coord.get(node)[0];
            double nodeY = node_coord.get(node)[1];            
            // Avoid very small distances and convert negative distance (i.e
            // outside the border to small positive ones )
            double l = nodeX - boundsX;
            if (l < minDistanceLimit)
                    l = minDistanceLimit;
            double t = nodeY - boundsY;
            if (t < minDistanceLimit)
                    t = minDistanceLimit;
            double r = boundsX + boundsWidth - nodeX;
            if (r < minDistanceLimit)
                    r = minDistanceLimit;
            double b = boundsY + boundsHeight - nodeY;
            if (b < minDistanceLimit)
                    b = minDistanceLimit;
            energy += borderLineCostFactor
                            * ((1000000.0 / (t * t)) + (1000000.0 / (l * l))
                                            + (1000000.0 / (b * b)) + (1000000.0 / (r * r)));
        }
        return energy;
    }
    
    /**
     * Obtains the energy cost function for the specified node being moved.
     * This involves calling <code>getEdgeLength</code> for all
     * edges connected to the specified node
     */
    private double getEdgeLengthAffectedEdges(String node, Map<String, Double[]> node_coord, 
            Map<String, ArrayList<Double[]>> node_ConnectedLinksCoord) {
        double energy = 0.0;
        if(node_ConnectedLinksCoord.get(node) != null) {
            for(Double[] link_coord : node_ConnectedLinksCoord.get(node)) {
                energy += getEdgeLength(link_coord);
            }
        }
        return energy;
    }

    /**
     * This method calculates the energy due to the length of the specified
     * edge. The energy is proportional to the length of the edge, making
     * shorter edges preferable in the layout.
     */
    private double getEdgeLength(Double[] link_coord)
    {
        if (isOptimizeEdgeLength)
        {
            double edgeLength = Point2D.distance(link_coord[0],link_coord[1], 
                    link_coord[2], link_coord[3]);
            return (edgeLengthCostFactor * edgeLength * edgeLength);
        }
        else
        {
            return 0.0;
        }
    }
   
    /**
     * Hook method to adding additional energy factors into the layout.
     * Calculates the energy just for the specified node.
     * @param i the nodes whose energy is being calculated
     * @return the energy of this node caused by the additional factors
     */
    private double getAdditionFactorsEnergy()
    {
            return 0.0;
    }
    
    /**
     * Calculates the change in energy for the specified node. The new energy is
     * calculated from the cost function methods and the old energy values for
     * each cost function are passed in as parameters
     * 
     * @param index
     *            The index of the node in the <code>vertices</code> array
     * @param oldNodeDistribution
     *            The previous node distribution energy cost of this node
     * @param oldEdgeDistance
     *            The previous edge distance energy cost of this node
     * @param oldEdgeCrossing
     *            The previous edge crossing energy cost for edges connected to
     *            this node
     * @param oldBorderLine
     *            The previous border line energy cost for this node
     * @param oldEdgeLength
     *            The previous edge length energy cost for edges connected to
     *            this node
     * @param oldAdditionalFactorsEnergy
     *            The previous energy cost for additional factors from
     *            sub-classes
     * 
     * @return the delta of the new energy cost to the old energy cost
     * 
     */
    private double calcEnergyDelta(String node,
            Map<String, Double[]> node_coord, 
            Map<String, ArrayList<Double[]>> node_ConnectedLinksCoord, 
            Map<String, ArrayList<Double[]>> node_NotConnectedLinksCoord,
            ArrayList<ArrayList<String>> links,
            double oldNodeDistribution,
            double oldEdgeDistance, double oldEdgeCrossing,
            double oldBorderLine, double oldEdgeLength,
            double oldAdditionalFactorsEnergy)
    {
        double energyDelta = 0.0;
        energyDelta += getNodeDistribution(node, node_coord) * 2.0;
        energyDelta -= oldNodeDistribution * 2.0;

        energyDelta += getBorderline(node, node_coord);
        energyDelta -= oldBorderLine;

        energyDelta += getEdgeDistanceFromNode(node, node_coord, node_NotConnectedLinksCoord);
        energyDelta += getEdgeDistanceAffectedNodes(node, node_coord, node_ConnectedLinksCoord);
        energyDelta -= oldEdgeDistance;

        energyDelta -= oldEdgeLength;
        energyDelta += getEdgeLengthAffectedEdges(node, node_coord, node_ConnectedLinksCoord);

        energyDelta -= oldEdgeCrossing;
        energyDelta += getEdgeCrossingAffectedEdges(node_coord, links);

        energyDelta -= oldAdditionalFactorsEnergy;
        energyDelta += getAdditionFactorsEnergy();

        return energyDelta;
    }
    
    
}
