package ru.kos.neb_viewer;

import javax.swing.*;
import java.util.ArrayList;
import javax.swing.JPanel;
import java.net.URL;
import java.io.File;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;

@SuppressWarnings({"unchecked","serial"})

class SetNodePropertiesForm extends JPanel
{
    static private JFrame frame;
    
    static public JTextField ipField;
    static public JTextField nameField;
    static public JTextField imageField;
    JFileChooser fc;
    
    
    /** Creates a new instance of SetNodePropertiesForm */
    public SetNodePropertiesForm()
    {
        initComponents();
    }
    
    private void initComponents()
    {
        JPanel jPanel1 = new javax.swing.JPanel();
        JButton jButton1 = new javax.swing.JButton();
        JButton jButton2 = new javax.swing.JButton();
        JButton jButton3 = new javax.swing.JButton();
        JPanel jPanel2 = new javax.swing.JPanel();
        JLabel jLabel1 = new javax.swing.JLabel();
        JLabel jLabel2 = new javax.swing.JLabel();
        JLabel jLabel3 = new javax.swing.JLabel();
        ipField = new javax.swing.JTextField();
        nameField = new javax.swing.JTextField();
        imageField = new javax.swing.JTextField();

        setBackground(new java.awt.Color(220, 220, 190));
        jPanel1.setBackground(new java.awt.Color(220, 220, 190));
        jPanel2.setBackground(new java.awt.Color(220, 220, 190));
        jButton1.setBackground(new java.awt.Color(220, 220, 190));
        jButton2.setBackground(new java.awt.Color(220, 220, 190));
        jButton3.setBackground(new java.awt.Color(220, 220, 190));

        final PImage image = (PImage) Main.node_sel.getAttribute("pimage");
        final PText text_sel = (PText) Main.node_sel.getAttribute("text");
        final String name_node = text_sel.getText();
        final String tooltipNodeString = (String) Main.node_sel.getAttribute("tooltip");
        String path_image = (String) Main.node_sel.getAttribute("path_image");
        
        ipField.setText(name_node);
        nameField.setText(tooltipNodeString);
        imageField.setText(path_image);
        
        jButton1.setText("OK");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                if(!SetNodePropertiesForm.ipField.getText().equals(""))
                {
                    text_sel.setText(SetNodePropertiesForm.ipField.getText());
//                    ArrayList tmp = new ArrayList();
//                    tmp.add(text_sel);
                    image.addAttribute("tooltip", SetNodePropertiesForm.ipField.getText());
                    
                    
                    ArrayList edges = (ArrayList) Main.node_sel.getAttribute("edges");
                    for (int i = 0; i < edges.size(); i++)
                    {
                        String port1 = null, port2=null;
                        PPath edge = (PPath) edges.get(i);
                        String tooltipString = (String) edge.getAttribute("tooltip");
                        String[] nodesplit = tooltipString.split("-");
                        nodesplit[0] = nodesplit[0].trim(); nodesplit[1]=nodesplit[1].trim();
                        String[] host = nodesplit[0].split(" ");
                        String ip1 = host[0].trim();
                        if(host.length == 2) port1=host[1].trim();
                        host=nodesplit[1].split(" ");
                        String ip2 = host[0].trim();
                        if(host.length == 2) port2=host[1].trim();

                        if(ip1.equals(name_node))
                        {
                            if(port1 != null)
                            {
                                edge.addAttribute("tooltip", ipField.getText()+" "+port1+" - "+ip2+" "+port2);
                            }
                            else
                            {
                                edge.addAttribute("tooltip", ipField.getText()+" - "+ip2);
                            }

                        }
                        if(ip2.equals(name_node))
                        {
                            if(port2 != null)
                            {
                                edge.addAttribute("tooltip", ip1+" "+port1+" - "+ipField.getText()+" "+port2);
                            }
                            else
                            {
                                edge.addAttribute("tooltip", ip1+" - "+ipField.getText());
                            }

                        }

                    }
                    
                    text_sel.repaint();
                    Main.isChanged=true;
//                    ControlPanel.jButton1.setIcon(new ImageIcon(getClass().getResource("/Neb_viewer/images/save.png")));
                    ControlPanel.jButton1.setEnabled(true);
                }
                if(!SetNodePropertiesForm.imageField.getText().equals(""))
                {
                    double x = Main.node_sel.getFullBounds().getX();
                    double y = Main.node_sel.getFullBounds().getY();
                    image.setImage(SetNodePropertiesForm.imageField.getText());
                    Main.node_sel.setOffset(x, y);
                    ArrayList tmp = new ArrayList();
                    tmp.add(image);
                    image.addAttribute("pimage",tmp);
                    image.addAttribute("path_image", SetNodePropertiesForm.imageField.getText());
                    
                    image.repaint();
                    Main.isChanged=true;
//                    ControlPanel.jButton1.setIcon(new ImageIcon(getClass().getResource("/Neb_viewer/images/save.png")));
                    ControlPanel.jButton1.setEnabled(true);
                    Utils.RepaintEdge(Main.node_sel);
                }
                frame.dispose();
            }
        });

        jButton2.setText("Cancel");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                frame.dispose();
            }
        });

        jButton3.setText("...");
        jButton3.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt)
            {
                fc = new JFileChooser();
                //Add a custom file filter and disable the default
                //(Accept All) file filter.
                fc.addChoosableFileFilter(new ImageFilter());
                fc.setAcceptAllFileFilterUsed(false);
                
                //Add custom icons for file types.
                fc.setFileView(new ImageFileView());
                
                //Add the preview pane.
                fc.setAccessory(new ImagePreview(fc));
                
                File curdir = new File(Main.home+"image");
                fc.setCurrentDirectory(curdir);
//                fc.setOpaque(true);
//                fc.setBackground(new Color(220,220,190));
                int returnVal = fc.showDialog(SetNodePropertiesForm.this, "Get image");
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File file = fc.getSelectedFile();
                    imageField.setText(file.getAbsolutePath());
                }
            }
        });
        
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(139, Short.MAX_VALUE)
                .add(jButton1)
                .add(17, 17, 17)
                .add(jButton2)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton1)
                    .add(jButton2))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Set Node properties: "));
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setText("Ip: ");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel2.setText("Name:");

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel3.setText("Image:");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(31, 31, 31)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel3)
                    .add(jLabel2)
                    .add(jLabel1))
                .add(18, 18, 18)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(ipField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(nameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(imageField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(18, 18, 18)
                    .add(jButton3)
                .addContainerGap(63, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(22, 22, 22)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(ipField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(19, 19, 19)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(nameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(20, 20, 20)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel3)
//                    .add(imageField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
//                    .add(jButton3)
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(imageField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(jButton3))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

    }
    
    public void createSetNodePropertiesForm() 
    {
        //Create and set up the window.
        frame = new JFrame("Properties node");
        
        //Create and set up the content pane.
        JComponent newContentPane = new SetNodePropertiesForm();
        newContentPane.setOpaque(true); //content panes must be opaque
//        newContentPane.setBackground(new Color(220,220,190));
        frame.setContentPane(newContentPane);
//        ImageIcon m_ImageIcon = new ImageIcon();
        URL url = Main.class.getResource("/images/idle.gif");
        frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));
        //Display the window.
        frame.pack();
        frame.setVisible(true);

    }

    
}

