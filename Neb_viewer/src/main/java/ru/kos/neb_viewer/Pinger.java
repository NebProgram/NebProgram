package ru.kos.neb_viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Objects;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.border.Border;

/**
 *
 * @author kos
 */
public class Pinger extends javax.swing.JPanel {
    static public JFrame frame=null;


    public Pinger() {
//        System.out.println("Init");
        initComponents();
//        jLabel1.setVisible(false);
        jButton1.setVisible(false);
    }
  

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jImageLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 51)));
        setPreferredSize(new java.awt.Dimension(300, 100));

        jImageLabel.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/wait.gif")))); // NOI18N

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel1.setText("Waiting ...");

        jButton1.setText("Close");
        jButton1.setBorder(new RoundedBorder(10));
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
                .addGap(15, 15, 15)
                .addComponent(jImageLabel)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 97, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jLabel1))
                        .addGap(4, 4, 4))
                    .addComponent(jImageLabel))
                .addGap(29, 29, 29))
        );
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        frame = null;
    }//GEN-LAST:event_jButton1ActionPerformed

    public static void createPingerForm(String node) 
    {
        if(frame != null) {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            frame = null;
        }
        //Create and set up the window.
        frame = new JFrame();
        
        //Create and set up the content pane.
        JComponent newContentPane = new Pinger();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
            
        URL icon = Main.class.getResource("/images/idle.gif");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(icon));
        
        //Display the window.
        frame.setUndecorated(true);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension ws = frame.getPreferredSize();
        frame.setBounds((int)d.getWidth()/2 - (int)ws.getWidth()/2, (int)d.getHeight()/2 - (int)ws.getHeight()/2, (int)ws.getWidth(), (int)ws.getHeight());
        frame.pack();
//        frame.setVisible(true);
        
        Thread t = new Thread(() -> {
            try {        
                String url = "https://" + Main.neb_server + ":" + Main.neb_server_port + "/ping";
                String result = Utils.HTTPSRequestPOST(url, node);
                if (!result.equals("")) {
                    if(result.contains("ok")) {
                        jImageLabel.setIcon(new ImageIcon(Objects.requireNonNull(Main.class.getResource("/images/green_ball.png"))));
                        jLabel1.setForeground(new java.awt.Color(0, 153, 51));
                        jLabel1.setText(node+" - OK.");
                    } else {
                        jImageLabel.setIcon(new ImageIcon(Objects.requireNonNull(Main.class.getResource("/images/red_ball.png"))));
                        jLabel1.setForeground(new java.awt.Color(255, 0, 0));
                        jLabel1.setText(node+" - ERR!!!");
                    }
                } else {
                    System.out.println("Error post request: url - " + url + " val - " + node);
                    jLabel1.setText("Error post request: url - " + url + " val - " + node);
                }
                jButton1.setVisible(true);
            } catch (Exception ex) {
                System.out.println(Utils.class.getName() + " - " + ex);
                frame.setVisible(false);
            }
        });
        t.start();
        
        frame.setVisible(true);

    }    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static javax.swing.JButton jButton1;
    private static javax.swing.JLabel jImageLabel;
    private static javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}

class RoundedBorder implements Border {

    private final int radius;


    RoundedBorder(int radius) {
        this.radius = radius;
    }


    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }


    @Override
    public boolean isBorderOpaque() {
        return true;
    }


    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.drawRoundRect(x, y, width-1, height-1, radius, radius);
    }
}