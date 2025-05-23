package ru.kos.neb_viewer;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;
import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;

public class AddLink extends javax.swing.JPanel {
    static private JFrame frame;
    static private PImage from_node;
    static private PImage to_node;

    /**
     * Creates new form AddLink
     */
    public AddLink() {
        initComponents();
    }
    
    public void addingLink() {
        String node1 = (String)from_node.getAttribute("ip");
        String node2 = (String)to_node.getAttribute("ip");
        String iface1 = jTextField2.getText();
        String iface2 = jTextField4.getText();
        if(iface1.isEmpty())
            iface1 = "unknown";
        if(iface2.isEmpty())
            iface2 = "unknown";
        ArrayList<String> link = new ArrayList();
        link.add(node1);
        link.add("");
        link.add(iface1);
        link.add(node2);
        link.add("");
        link.add(iface2);
        
        if (node1 != null && node2 != null) {
            Point2D.Double bound1 = (Point2D.Double) from_node.getBounds().getCenter2D();
            Point2D.Double bound2 = (Point2D.Double) to_node.getBounds().getCenter2D();

            PPath edge = new PPath.Double();
            edge.moveTo((float) bound1.getX(), (float) bound1.getY());
            edge.lineTo((float) bound2.getX(), (float) bound2.getY());

            ArrayList<PImage> tmp1 = new ArrayList();
            tmp1.add(from_node);
            tmp1.add(to_node);
            edge.addAttribute("nodes", tmp1);
            ArrayList<Point2D.Double> tmp2 = new ArrayList();
            tmp2.add(bound1);
            tmp2.add(bound2);
            edge.addAttribute("coordinate", tmp2);

            edge.addAttribute("link", link);

            String tooltip = node1 + " " + iface1 + " - " + node2 + " " + iface2;
            edge.addAttribute("tooltip", tooltip);

            edge.addAttribute("ppath", edge);
            edge.setStroke(new BasicStroke(1));
            edge.addAttribute("width", 1);
            Main.edgeLayer.addChild(edge);
            Main.edgeLayer.repaint();
           
        }
        
        // Adding text
        for (int i = 0; i < Main.nodeLayer.getChildrenCount(); i++) {
            PNode node3 = Main.nodeLayer.getChild(i);

            PText text = new PText();
            String tooltipString = (String) node3.getAttribute("tooltip");
            text.setText(tooltipString);

            Font font = new Font("Arial", Font.PLAIN, 50);
            text.setFont(font);
            Point2D.Double pbound = (Point2D.Double) node3.getBounds().getCenter2D();
            double w = node3.getWidth();
            double h = node3.getHeight();
            double wt = text.getWidth();
            double ht = text.getHeight();
            double x = pbound.getX();
            double y = pbound.getY();
            text.setX(x - wt / 2.);

            text.setY(y - h / 2. - ht / 2. - 10);

            node3.addAttribute("text", text);
            text.addAttribute("text", text);
//            text.addAttribute("node3", node3);

            Main.textLayer.addChild(text);
        }        
    }

    public static void createAddLinkForm(PImage from_node) 
    {
        AddLink.from_node = from_node;
        //Create and set up the window.
        frame = new JFrame("Add Link");
        
        //Create and set up the content pane.
        JComponent newContentPane = new AddLink();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
//        ImageIcon m_ImageIcon = new ImageIcon();
        URL url = Main.class.getResource("/images/idle.gif");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));
        
        jTextField1.setText((String)from_node.getAttribute("ip"));
//        jTextField3.requestFocus();

        //Display the window.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension ws = frame.getPreferredSize();
        frame.setBounds((int)d.getWidth()/2 - (int)ws.getWidth()/2, (int)d.getHeight()/2 - (int)ws.getHeight()/2, (int)ws.getWidth(), (int)ws.getHeight());
        frame.pack();
        frame.setVisible(true);
        frame.getRootPane().setDefaultButton(jButton1);
        jTextField3.requestFocus();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("From"));

        jLabel1.setText("Node:");

        jTextField1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 0), 2));
        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField1KeyPressed(evt);
            }
        });

        jLabel2.setText("Iface:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "To", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 12), new java.awt.Color(51, 0, 204))); // NOI18N

        jLabel3.setText("Node:");

        jTextField3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 0), 2));
        jTextField3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextField3KeyPressed(evt);
            }
        });

        jLabel4.setText("Iface:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14))
        );

        jButton1.setText("Add Link");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(33, 33, 33))
        );
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Runnable task = () -> {
            Main.isBusy = true;
            WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
            waitCircleApplicationIcon.end = false;
            waitCircleApplicationIcon.start(); 
            Main.control_panel.SetDisable();
            Main.time_machine.SetDisable();
            addingLink();
            Main.isChanged = true;
            waitCircleApplicationIcon.end = true;
            Main.control_panel.SetEnable();
            Main.time_machine.SetEnable();
            Main.isBusy = false;                                
        };
        Thread thread = new Thread(task);
        thread.start(); 
        
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField3KeyPressed
        String s = String.valueOf(evt.getKeyChar());
        if(s.equals("\n")) {
            Runnable task = () -> {
                Main.isBusy = true;
                WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
                waitCircleApplicationIcon.end = false;
                waitCircleApplicationIcon.start(); 
                Main.control_panel.SetDisable();
                Main.time_machine.SetDisable();
                addingLink();
                Main.isChanged = true;
                waitCircleApplicationIcon.end = true;
                Main.control_panel.SetEnable();
                Main.time_machine.SetEnable();
                Main.isBusy = false;                                
            };
            Thread thread = new Thread(task);
            thread.start(); 

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        } else {
            from_node = Utils.findNode(jTextField1.getText());
            to_node = Utils.findNode(jTextField3.getText()+s);
            if(from_node != null && to_node != null) {
                jButton1.setEnabled(true);
            } else {
                jButton1.setEnabled(false);
            }
            if(to_node != null) {
                jTextField3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 0), 2));
            } else {
                jTextField3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 0), 2));
            }
            
        }
    }//GEN-LAST:event_jTextField3KeyPressed

    private void jTextField1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyPressed
        String s = String.valueOf(evt.getKeyChar());
        if(s.equals("\n")) {
            Runnable task = () -> {
                Main.isBusy = true;
                WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
                waitCircleApplicationIcon.end = false;
                waitCircleApplicationIcon.start(); 
                Main.control_panel.SetDisable();
                Main.time_machine.SetDisable();
                addingLink();
                Main.isChanged = true;
                waitCircleApplicationIcon.end = true;
                Main.control_panel.SetEnable();
                Main.time_machine.SetEnable();
                Main.isBusy = false;                                
            };
            Thread thread = new Thread(task);
            thread.start(); 

            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        } else {
            from_node = Utils.findNode(jTextField1.getText()+s);
            to_node = Utils.findNode(jTextField3.getText());
            if(from_node != null && to_node != null) {
                jButton1.setEnabled(true);
            } else {
                jButton1.setEnabled(false);
            }
            if(from_node != null) {
                jTextField1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 0), 2));
            } else {
                jTextField1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 0), 2));
            }
        }
    }//GEN-LAST:event_jTextField1KeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private static javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private static javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    // End of variables declaration//GEN-END:variables
}
