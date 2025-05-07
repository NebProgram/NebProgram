package ru.kos.neb_viewer;

import java.awt.*;
import java.awt.event.WindowEvent;

import javax.swing.*;

@SuppressWarnings({"serial", "unchecked"})
/*

  @author  kos
 */
public class UserPasswd extends javax.swing.JPanel
{
    static public JDialog frame=null;

    public UserPasswd()
    {
        
        initComponents();
    }
    
    public static void createUserPasswdDialog()
    {
        frame = new JDialog(Main.getFrames()[0], "Authorized", true);
        //Create and set up the content pane.
        JComponent newContentPane = new UserPasswd();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.setUndecorated(true);
        frame.setModal(true);
        frame.setAlwaysOnTop(true);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension ws = frame.getPreferredSize();
        frame.setBounds((int)d.getWidth()/2 - (int)ws.getWidth()/2, (int)d.getHeight()/2 - (int)ws.getHeight()/2, (int)ws.getWidth(), (int)ws.getHeight());
        frame.pack();
        frame.setVisible(true);        
        jTextField1.requestFocus();
    }
    
    
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(51, 153, 0), 3, true));

        jLabel2.setText("User:");

        jLabel3.setText("Passwd:");

        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextField1KeyTyped(evt);
            }
        });

        jPasswordField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jPasswordField1KeyTyped(evt);
            }
        });

        jButton1.setText("Login");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Out");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(105, 105, 105)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField1)
                            .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(27, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleParent(this);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Main.user = jTextField1.getText();
        Main.passwd = new String(jPasswordField1.getPassword());

        String access = Main.utils.GetAccess().split("\n")[0];
        switch (access) {
            case "write" -> {
                Main.read_write = "write";
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
            case "read" -> {
                Main.read_write = "read";
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
            case "not access" -> {
                jTextField1.setText("");
                jPasswordField1.setText("");
                jTextField1.requestFocus();
            }            
            default -> {
                JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyTyped
        if(String.valueOf(evt.getKeyChar()).equals("\n")) {
            Main.user = jTextField1.getText();
            Main.passwd = new String(jPasswordField1.getPassword());
            String access = Main.utils.GetAccess().split("\n")[0];
            switch (access) {
                case "write" -> {
                    Main.read_write = "write";
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
                case "read" -> {
                    Main.read_write = "read";
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
                case "not access" -> {
                    jTextField1.setText("");
                    jPasswordField1.setText("");
                    jTextField1.requestFocus();
                }            
                default -> {
                    JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }//GEN-LAST:event_jTextField1KeyTyped

    private void jPasswordField1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jPasswordField1KeyTyped
        if(String.valueOf(evt.getKeyChar()).equals("\n")) {
            Main.user = jTextField1.getText();
            Main.passwd = new String(jPasswordField1.getPassword());
            String access = Main.utils.GetAccess().split("\n")[0];
            switch (access) {
                case "write" -> {
                    Main.read_write = "write";
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
                case "read" -> {
                    Main.read_write = "read";
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }
                case "not access" -> {
                    jTextField1.setText("");
                    jPasswordField1.setText("");
                    jTextField1.requestFocus();
                }            
                default -> {
                    JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }//GEN-LAST:event_jPasswordField1KeyTyped

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        System.exit(1);
    }//GEN-LAST:event_jButton2ActionPerformed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private static javax.swing.JPasswordField jPasswordField1;
    private static javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
    
}

