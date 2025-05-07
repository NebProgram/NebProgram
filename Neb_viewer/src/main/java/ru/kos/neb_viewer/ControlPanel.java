package ru.kos.neb_viewer;

import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import static ru.kos.neb_viewer.Utils.SaveChanged;

/**
 *
 * @author  kos
 */
@SuppressWarnings("serial")
public class ControlPanel extends javax.swing.JPanel
{
    public static String area_select = (String)Main.cfg.get("default_area");
    private final Map<String, String> description_area = new HashMap();
    private boolean ready = false;
    
    /** Creates new form ControlPanel */
    public ControlPanel()
    {
        initComponents();
        
        if(Main.read_write.equals("write"))
            ControlPanel.jToggleButton1.setVisible(true);
        else
            ControlPanel.jToggleButton1.setVisible(false);
        
        String url = "https://" + Main.neb_server + ":" + Main.neb_server_port + "/get?file=" + Main.nebserver_cfg + "&key=/areas";
        String result = Utils.HTTPSRequestGET(url);
        Map areas = new HashMap();
        JSONParser parser = new JSONParser();
        if (!result.equals("")) {
            try {
                JSONObject jsonObject = (JSONObject) parser.parse(result);
                areas = Utils.toMap(jsonObject);
            } catch (ParseException ex) {
                Frame frame = Main.getFrames()[0];
                JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            Frame frame = Main.getFrames()[0];
            JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
            
        if(!areas.isEmpty()) {
            boolean found = false;
            int index = 0;
            int i = 0;
            for(Map.Entry<String, Map> entry : ((Map<String, Map>)areas).entrySet()) {
                String area = entry.getKey();
                String description = (String)entry.getValue().get("description");
                if(description != null) { jComboBox1.addItem(description); description_area.put(description, area); }
                else { jComboBox1.addItem(area); description_area.put(area, area); }
                if(area.equals(Main.cfg.get("default_area"))) {
                    index=i;
                    found = true;
                }
                i=i+1;
            }
            if(found) jComboBox1.setSelectedIndex(index);
            else {
                jComboBox1.setSelectedIndex(0);
                String description = (String)jComboBox1.getSelectedItem();
                String area = description_area.get(description);
                if(area != null)
                    area_select = area;
            }
        }
        ready = true;
    }
    
    public void SetDisable() {
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        jButton3.setEnabled(false);
        jButton4.setEnabled(false);
        jButton5.setEnabled(false);
        jButton6.setEnabled(false);
        jToggleButton1.setEnabled(false);
        jComboBox1.setEnabled(false);
    }
    
    public void SetEnable() {
        jButton1.setEnabled(true);
        jButton2.setEnabled(true);
        jButton3.setEnabled(true);
        jButton4.setEnabled(true);
        jButton5.setEnabled(true);
        jButton6.setEnabled(true);
        jToggleButton1.setEnabled(true);
        jComboBox1.setEnabled(true);
    }    
    
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jToggleButton1 = new javax.swing.JToggleButton();
        jSeparator4 = new javax.swing.JSeparator();
        message = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jComboBox1 = new javax.swing.JComboBox<>();

        setBackground(new java.awt.Color(220, 220, 190));
        setMaximumSize(new java.awt.Dimension(99999, 35));
        setMinimumSize(new java.awt.Dimension(403, 35));
        setPreferredSize(new java.awt.Dimension(403, 35));
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        jButton1.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/save.png")))); // NOI18N
        jButton1.setToolTipText("Save");
        jButton1.setBorder(null);
        jButton1.setEnabled(false);
        jButton1.setOpaque(false);
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

        jButton2.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/print.png")))); // NOI18N
        jButton2.setToolTipText("Print");
        jButton2.setBorder(null);
        jButton2.setOpaque(false);
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

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        add(jSeparator1);

        jButton3.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/plus.png")))); // NOI18N
        jButton3.setToolTipText("Zoom In");
        jButton3.setBorder(null);
        jButton3.setOpaque(false);
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

        jButton4.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/minus.png")))); // NOI18N
        jButton4.setToolTipText("Zoom Out");
        jButton4.setBorder(null);
        jButton4.setOpaque(false);
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

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        add(jSeparator2);

        jButton5.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/center.png")))); // NOI18N
        jButton5.setToolTipText("Place to Screen");
        jButton5.setBorder(null);
        jButton5.setOpaque(false);
        jButton5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton5MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton5MouseExited(evt);
            }
        });
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });
        add(jButton5);

        jButton6.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/find.png")))); // NOI18N
        jButton6.setToolTipText("Find Ctrl-F");
        jButton6.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jButton6.setOpaque(false);
        jButton6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jButton6MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jButton6MouseExited(evt);
            }
        });
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        add(jButton6);

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
        add(jSeparator3);

        jToggleButton1.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/edit_off.png")))); // NOI18N
        jToggleButton1.setToolTipText("To edit mode");
        jToggleButton1.setBorder(null);
        jToggleButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jToggleButton1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jToggleButton1MouseExited(evt);
            }
        });
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });
        add(jToggleButton1);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);
        add(jSeparator4);

        message.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        message.setForeground(new java.awt.Color(0, 51, 0));
        message.setText("Neb Viewer is started.");
        add(message);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        add(jSeparator5);

        jComboBox1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jComboBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox1ItemStateChanged(evt);
            }
        });
        add(jComboBox1);
    }

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton6ActionPerformed
    {//GEN-HEADEREND:event_jButton6ActionPerformed
        Utils.hideFlashLinks();
        Main.toggle.doClick();
        Main.find_field.requestFocus();
//        FindNode.createFindNode();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton5ActionPerformed
    {//GEN-HEADEREND:event_jButton5ActionPerformed
//        Utils.hideFlashLinks();
        Utils.SetAllToScreen();
        Utils.repaintText();
        Utils.repaintInterfaceName();
        Utils.RepaintFlash(null);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
    {//GEN-HEADEREND:event_jButton4ActionPerformed
        Utils.ZoomIn();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
    {//GEN-HEADEREND:event_jButton3ActionPerformed
        Utils.ZoomOut();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
    {//GEN-HEADEREND:event_jButton2ActionPerformed
        javax.swing.SwingUtilities.invokeLater(() -> PrintUtilities.printComponent(Main.canvas));
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
    {//GEN-HEADEREND:event_jButton1ActionPerformed
        if(Main.isChanged) Utils.SaveChanged(Main.map_filename, area_select);        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jToggleButton1MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jToggleButton1MouseExited
    {//GEN-HEADEREND:event_jToggleButton1MouseExited
        jToggleButton1.setBorder(null);
    }//GEN-LAST:event_jToggleButton1MouseExited

    private void jToggleButton1MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jToggleButton1MouseEntered
    {//GEN-HEADEREND:event_jToggleButton1MouseEntered
        jToggleButton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jToggleButton1MouseEntered

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jToggleButton1ActionPerformed
    {//GEN-HEADEREND:event_jToggleButton1ActionPerformed
                if(jToggleButton1.isSelected())     // to edit mode
                {
                    Main.isModeEdit=true;
//                    Main m_Main = new Main();
                    CreateEvent(true);
                    jToggleButton1.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/edit.png"))));
                }
                else
                {                       // to view mode
                    if(Main.isChanged)       
                    {
                        Frame frame = Main.getFrames()[0];
                        int n = JOptionPane.showConfirmDialog(frame, "Would you like save changed ???","Save changed ???",JOptionPane.YES_NO_OPTION);
                        if(n == 0)
                        {
                            Utils.SaveChanged(Main.map_filename, area_select);
                        }
                        else Main.isChanged=false;
                    }
                    Main.isModeEdit=false; 
//                    Main m_Main = new Main();
                    CreateEvent(false);
                    jButton1.setEnabled(false);
                    jToggleButton1.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/edit_off.png"))));
                    jToggleButton1.setToolTipText("To edit mode");
//                    Main.contextCanvasMenu.removeAll();
                }
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton6MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton6MouseExited
    {//GEN-HEADEREND:event_jButton6MouseExited
        jButton6.setBorder(null);
//        jButton6.setBorder(javax.swing.BorderFactory..createEmptyBorder(3, 3, 3, 3));
    }//GEN-LAST:event_jButton6MouseExited

    private void jButton6MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton6MouseEntered
    {//GEN-HEADEREND:event_jButton6MouseEntered
        jButton6.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton6MouseEntered

    private void jButton5MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton5MouseExited
    {//GEN-HEADEREND:event_jButton5MouseExited
        jButton5.setBorder(null);
    }//GEN-LAST:event_jButton5MouseExited

    private void jButton5MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton5MouseEntered
    {//GEN-HEADEREND:event_jButton5MouseEntered
         jButton5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton5MouseEntered

    private void jButton4MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton4MouseExited
    {//GEN-HEADEREND:event_jButton4MouseExited
        jButton4.setBorder(null);
    }//GEN-LAST:event_jButton4MouseExited

    private void jButton4MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton4MouseEntered
    {//GEN-HEADEREND:event_jButton4MouseEntered
        jButton4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton4MouseEntered

    private void jButton3MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton3MouseExited
    {//GEN-HEADEREND:event_jButton3MouseExited
        jButton3.setBorder(null);
    }//GEN-LAST:event_jButton3MouseExited

    private void jButton3MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton3MouseEntered
    {//GEN-HEADEREND:event_jButton3MouseEntered
        jButton3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton3MouseEntered

    private void jButton2MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton2MouseExited
    {//GEN-HEADEREND:event_jButton2MouseExited
        jButton2.setBorder(null);
    }//GEN-LAST:event_jButton2MouseExited

    private void jButton2MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton2MouseEntered
    {//GEN-HEADEREND:event_jButton2MouseEntered
        jButton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton2MouseEntered

    private void jButton1MouseExited(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton1MouseExited
    {//GEN-HEADEREND:event_jButton1MouseExited
        jButton1.setBorder(null);
    }//GEN-LAST:event_jButton1MouseExited

    private void jButton1MouseEntered(java.awt.event.MouseEvent evt)//GEN-FIRST:event_jButton1MouseEntered
    {//GEN-HEADEREND:event_jButton1MouseEntered
        if(jButton1.isVisible()) jButton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    }//GEN-LAST:event_jButton1MouseEntered

    private void jComboBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBox1ItemStateChanged
        if (evt.getStateChange() == ItemEvent.DESELECTED) {
            Object item = evt.getItem();
            String area_description = item.toString();
            String area_name = description_area.get(area_description);
            if (Main.isChanged) {
                Frame frame = Main.getFrames()[0];
                int n = JOptionPane.showConfirmDialog(frame, "Would you like save changed ???", "Save changed ???", JOptionPane.YES_NO_OPTION);
                if (n == 0) {
                    SaveChanged(Main.map_filename, area_name);
                }
            }            
        }
        if (evt.getStateChange() == ItemEvent.SELECTED) {
          Object item = evt.getItem();
          String area_description = item.toString();
          if(ready) {
            String area_name = description_area.get(area_description);
            if(area_name != null) area_select = area_name;
//            System.out.println(area_select);           
            Utils.SetTimeMachine(Main.history_list, TimeMachineForm.selector, area_select);
          }
       }
//        System.out.println("jComboBox1ItemStateChanged");
    }//GEN-LAST:event_jComboBox1ItemStateChanged

    public void CreateEvent(boolean mode)
    {
        if(mode)
        {
            Main.nodeLayer.removeInputEventListener(new NodeEventHandlerView());
            Main.nodeLayer.addInputEventListener(new NodeEventHandler());
            Main.edgeLayer.removeInputEventListener(new EdgeEventHandlerView());
            Main.edgeLayer.addInputEventListener(new EdgeEventHandler());
            Main.textLayer.removeInputEventListener(new TextEventHandlerView());
            Main.textLayer.addInputEventListener(new TextEventHandler());
            Main.textCustomLayer.removeInputEventListener(new TextCustomEventHandlerView());
            Main.textCustomLayer.addInputEventListener(new TextCustomEventHandler());
            Main.canvas.removeInputEventListener(Main.canvas.getZoomEventHandler());
            Main.canvas.removeInputEventListener(new PBasicInputEventHandler());
            Main.canvas.grabFocus();
            Main.canvas.addInputEventListener(new PBasicInputEventHandler()
            {
                @Override
                public void mouseClicked(PInputEvent e)
                {
                    if ( e.isRightMouseButton() )
                    {
                        Point2D ePos;
                        ePos = e.getCanvasPosition();
                        new Utils().addCanvasContextMenu();
                        Main.contextCanvasMenu.show(Main.canvas, (int) ePos.getX(),
                                (int) ePos.getY());
                        
                    /* We don't want any other interpretations of this
                        mouse event. */
                        e.setHandled(true);
                    }
                }
            });
        }
        else
        {
            Main.nodeLayer.removeInputEventListener(new NodeEventHandler());
            Main.nodeLayer.addInputEventListener(new NodeEventHandlerView());
            Main.edgeLayer.removeInputEventListener(new EdgeEventHandler());
            Main.edgeLayer.addInputEventListener(new EdgeEventHandlerView());
            Main.textLayer.removeInputEventListener(new TextEventHandler());
            Main.textLayer.addInputEventListener(new TextEventHandlerView());
            Main.textCustomLayer.removeInputEventListener(new TextCustomEventHandler());
            Main.textCustomLayer.addInputEventListener(new TextCustomEventHandlerView());
            Main.canvas.removeInputEventListener(Main.canvas.getZoomEventHandler());
            Main.canvas.grabFocus();
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    public static javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    public static javax.swing.JToggleButton jToggleButton1;
    public static javax.swing.JLabel message;
    // End of variables declaration//GEN-END:variables
    
}

