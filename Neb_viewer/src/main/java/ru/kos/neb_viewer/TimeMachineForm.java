package ru.kos.neb_viewer;

import javax.swing.ImageIcon;
import java.util.*;

/**
 *
 * @author  kos
 */
@SuppressWarnings("serial")
public class TimeMachineForm extends javax.swing.JPanel
{
    private boolean enabled_jButton1 = true;
    private boolean enabled_jButton2 = true;
    private boolean enabled_jButton3 = true;
    private boolean enabled_jButton4 = true;
    
    /** Creates new form TimeMachineForm */
    public TimeMachineForm()
    {
        initComponents();
    }
    
    public void SetDisable() {
        enabled_jButton1 = jButton1.isEnabled();
        enabled_jButton2 = jButton2.isEnabled();
        enabled_jButton3 = jButton3.isEnabled();
        enabled_jButton4 = jButton4.isEnabled();
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
        jButton4.setEnabled(false);
    }
    
    public void SetEnable() {
        if(enabled_jButton1) jButton1.setEnabled(true);
        if(enabled_jButton2) jButton2.setEnabled(true);
        if(enabled_jButton3) jButton3.setEnabled(true);
        if(enabled_jButton4) jButton4.setEnabled(true);
    } 
    
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();

        setBackground(new java.awt.Color(220, 220, 190));
        setMaximumSize(new java.awt.Dimension(270, 40));
        setMinimumSize(new java.awt.Dimension(270, 40));
        setPreferredSize(new java.awt.Dimension(270, 40));
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 0));

        jButton1.setBackground(new java.awt.Color(220, 220, 190));
        jButton1.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_left.png")))); // NOI18N
        jButton1.setToolTipText("Go previous time");
        jButton1.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jButton1.setMargin(new java.awt.Insets(2, 14, 2, 0));
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton1MouseExited(evt);
            }
        });
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        add(jButton1);

        jButton2.setBackground(new java.awt.Color(220, 220, 190));
        jButton2.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_down.png")))); // NOI18N
        jButton2.setToolTipText("Go previous time list");
        jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jButton2.setMargin(new java.awt.Insets(2, 0, 2, 14));
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton2MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton2MouseExited(evt);
            }
        });
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        add(jButton2);

        jTextField1.setEditable(false);
        jTextField1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        jTextField1.setText("hh:mm dd/mm/yy");
        jTextField1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        add(jTextField1);

        jButton3.setBackground(new java.awt.Color(220, 220, 190));
        jButton3.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_right.png")))); // NOI18N
        jButton3.setToolTipText("Go next time");
        jButton3.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jButton3.setMargin(new java.awt.Insets(2, 14, 2, 0));
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton3MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton3MouseExited(evt);
            }
        });
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });
        add(jButton3);

        jButton4.setBackground(new java.awt.Color(220, 220, 190));
        jButton4.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_down.png")))); // NOI18N
        jButton4.setToolTipText("Go next time list");
        jButton4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jButton4.setMargin(new java.awt.Insets(2, 0, 2, 2));
        jButton4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton4MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton4MouseExited(evt);
            }
        });
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });
        add(jButton4);
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
    {//GEN-HEADEREND:event_jButton3ActionPerformed
        if(selector < Main.history_list.size()-1) selector++;
        Utils.SetTimeMachine(Main.history_list, selector, ControlPanel.area_select);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        if(selector > 0) selector--;
        Utils.SetTimeMachine(Main.history_list, selector, ControlPanel.area_select);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton4MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton4MouseExited
    {//GEN-HEADEREND:event_jButton4MouseExited
        jButton4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    }//GEN-LAST:event_jButton4MouseExited

    private void jButton4MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton4MouseEntered
    {//GEN-HEADEREND:event_jButton4MouseEntered
        jButton4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton4MouseEntered

    private void jButton2MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton2MouseExited
    {//GEN-HEADEREND:event_jButton2MouseExited
        jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    }//GEN-LAST:event_jButton2MouseExited

    private void jButton2MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton2MouseEntered
    {//GEN-HEADEREND:event_jButton2MouseEntered
        jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton2MouseEntered

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
    {//GEN-HEADEREND:event_jButton4ActionPerformed
        Main.jPopupMenuNext.show(jButton4, jButton3.getX()-jButton4.getX(), jButton4.getHeight());
        jButton4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        Main.jPopupMenuPrev.show(jButton2, jButton1.getX()-jButton2.getX(), jButton2.getHeight());
        jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton3MouseExited
    {//GEN-HEADEREND:event_jButton3MouseExited
        if(jButton3.isVisible()) jButton3.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_right.png"))));
    }//GEN-LAST:event_jButton3MouseExited

    private void jButton3MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton3MouseEntered
    {//GEN-HEADEREND:event_jButton3MouseEntered
        if(jButton3.isVisible()) jButton3.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_right_select.png"))));
    }//GEN-LAST:event_jButton3MouseEntered

    private void jButton1MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton1MouseExited
    {//GEN-HEADEREND:event_jButton1MouseExited
        if(jButton1.isVisible()) jButton1.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_left.png"))));
    }//GEN-LAST:event_jButton1MouseExited

    private void jButton1MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton1MouseEntered
    {//GEN-HEADEREND:event_jButton1MouseEntered
        if(jButton1.isVisible()) jButton1.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/arrow_left_select.png"))));
    }//GEN-LAST:event_jButton1MouseEntered
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton jButton1;
    public static javax.swing.JButton jButton2;
    public static javax.swing.JButton jButton3;
    public static javax.swing.JButton jButton4;
    public static javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
    public static int selector;
    public static boolean last_map_selector=false;
    public static ArrayList<String[]> prev_list;
    public static ArrayList<String[]> next_list;
}
