package ru.kos.neb_viewer;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import javax.swing.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.*;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import org.jdesktop.swingx.WrapLayout;
//import net.nebmap.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.kos.neb.neb_lib.PingPool;


/**
 *
 * @author  kos
 */
@SuppressWarnings("serial")
public class GetNodePropertiesForm extends javax.swing.JPanel
{
    static public JFrame frame=null;
    static public String ip;
    static private String version;
    static private String community;
//    static private String oid;
    static private String sysDescription;
    static private String sysLocation;
//    static private String uptime;
    static private String sysContact;
    static private String sysName;
    static private String defaultTTL;
    static public Map<String, Map<String, String>> interfInformation = new HashMap<>();
    static private ArrayList<String> routeInformation = new ArrayList();
    static public Map<String, String> vlanInformation = new HashMap<>();
    static public ArrayList<String> iface_name_list = new ArrayList();
static private Map<String, ArrayList<String[]>> clients;
public static Map<String, String> ifacename_status = new HashMap();
public static boolean run_task_iface = false;
    public static boolean run_task_clients = false;
    public static boolean run_task_chart = false;
public static ThreadPoolExecutor task_pool;
//    public static String[][] charts_tree;
    public static ArrayList settings = new ArrayList();
    public static long max_age;
public static String in_chart="";
    public static String out_chart="";
    
    public static Map<String, String> item_icon = new HashMap();
    private static final Map<String, Integer[]> item_numtabbed_numpannel = new HashMap();
    private static final ArrayList<JPanel> jPanel_external_list = new ArrayList();
    private static Map<String, Map> external = new HashMap();


    
    DefaultMutableTreeNode level0 =  new DefaultMutableTreeNode(ip);
    MyTreeCellRenderer treeCellRenderer=new MyTreeCellRenderer();
//    MyTableCellRenderer tableCellRenderer=new MyTableCellRenderer();
    /** Creates new form GetNodePropertiesForm */
    public GetNodePropertiesForm()
    {
        DefaultMutableTreeNode level1a = new DefaultMutableTreeNode("General");
        level0.add(level1a);
        DefaultMutableTreeNode level1b = new DefaultMutableTreeNode("Interfaces");
        
//        ArrayList<String> iface_name_list = new ArrayList();
        if(interfInformation != null) {
            iface_name_list.clear();
            for (Map.Entry<String, Map<String, String>> entry : interfInformation.entrySet()) {
                iface_name_list.add(entry.getKey());
            }
            Collections.sort(iface_name_list);
            for (String iface_name : iface_name_list) {
                level1b.add(new DefaultMutableTreeNode(iface_name));
            }
            level0.add(level1b);
        }
        DefaultMutableTreeNode level1c = new DefaultMutableTreeNode("Route information");
        if(routeInformation != null) level0.add(level1c);
        DefaultMutableTreeNode level1d = new DefaultMutableTreeNode("Vlans");

        ArrayList<String[]> vlan_list = new ArrayList();
        if(vlanInformation != null) {
            for (Map.Entry<String, String> entry : vlanInformation.entrySet()) {
                String[] mas = new String[2];
                mas[0]=entry.getKey(); mas[1]=entry.getValue();
                vlan_list.add(mas);
            }
            Collections.sort(vlan_list, (String[] o1, String[] o2) -> o1[0].compareTo(o2[0]));
            for (String[] item : vlan_list) {
                level1d.add(new DefaultMutableTreeNode(item[0]+"  ("+item[1]+")"));
            }
            level0.add(level1d);
        }

        if(clients != null && !clients.isEmpty()) {
            DefaultMutableTreeNode level1e = new DefaultMutableTreeNode("Clients information");
            level0.add(level1e);
        }
        
        // adding items from external information
        if(Main.INFORMATION.get("nodes_information") != null &&
                ((Map)(Main.INFORMATION).get("nodes_information")).get(ip) != null &&
                ((Map)((Map)(Main.INFORMATION).get("nodes_information")).get(ip)).get("external") != null)
        {
            external = (Map)((Map)((Map)(Main.INFORMATION).get("nodes_information")).get(ip)).get("external");
            item_icon.clear();
            for(Map.Entry<String, Map> entry : external.entrySet()) {
                String item = entry.getKey();
                Map<String, String> val = entry.getValue();
                Map key_val = new HashMap();
                for(Map.Entry<String, String> entry1 : val.entrySet()) {
                    String key = entry1.getKey();
                    String value = entry1.getValue();
                    if(key.equals("icon"))
                        item_icon.put(item, value);
                    else
                        key_val.put(key, value);
                }
                DefaultMutableTreeNode level_item = new DefaultMutableTreeNode(item);
                level0.add(level_item);
            }
        }

        initComponents();
        
        // adding items from external information
        if(Main.INFORMATION.get("nodes_information") != null &&
                ((Map)(Main.INFORMATION).get("nodes_information")).get(ip) != null &&
                ((Map)((Map)(Main.INFORMATION).get("nodes_information")).get(ip)).get("external") != null)
        {
//            external.clear();
            external = (Map)((Map)((Map)(Main.INFORMATION).get("nodes_information")).get(ip)).get("external");
            int i = 0;
            jPanel_external_list.clear();
            item_numtabbed_numpannel.clear();
            for(Map.Entry<String, Map> entry : external.entrySet()) {
                String item = entry.getKey();
                String path_image = Main.path+"/images/"+item_icon.get(item);
//                JScrollPane jPanel_external = new JScrollPane();
                JPanel jPanel_external = new JPanel();
                jPanel_external_list.add(jPanel_external);
                jPanel_external_list.get(i).setBackground(new java.awt.Color(220, 220, 190));
                jPanel_external_list.get(i).setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
                jPanel_external_list.get(i).setForeground(new java.awt.Color(236, 233, 216));
                JScrollPane jScrollPane = new JScrollPane(jPanel_external_list.get(i));
                jTabbedPane1.addTab(item, new javax.swing.ImageIcon(path_image), jScrollPane);
                Integer[] mas = new Integer[2];
                mas[0] = jTabbedPane1.getTabCount()-1;
                mas[1] = i;
                item_numtabbed_numpannel.put(item, mas);
                i = i + 1;
            }
        }
        
        for(int i=1; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);

        FillGeneral();

    }

    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new JTree(level0);
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea5 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jTextField10 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jTextField14 = new javax.swing.JTextField();
        jTextField15 = new javax.swing.JTextField();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextArea6 = new javax.swing.JTextArea();
        jLabel14 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jPanel13 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jTextField16 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jTextField18 = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jTextField21 = new javax.swing.JTextField();
        jScrollPane15 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jTextField26 = new javax.swing.JTextField();
        jTextField27 = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel21 = new javax.swing.JLabel();

        setBackground(new java.awt.Color(220, 220, 190));

        jScrollPane1.setBackground(new java.awt.Color(220, 220, 190));
        jScrollPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jTree1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTree1.setName("tree"); // NOI18N
        jTree1.setSelectionRow(1);
        jTree1.setCellRenderer(treeCellRenderer);
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                SelectEvent(evt);
            }
        });
        jTree1.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt) {
            }
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt) {
                jTree1TreeWillExpand(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        jTabbedPane1.setBorder(new javax.swing.border.MatteBorder(null));
        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);

        jPanel1.setBackground(new java.awt.Color(220, 220, 190));
        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel1.setForeground(new java.awt.Color(236, 233, 216));

        jLabel1.setText("Ip");

        jLabel2.setText("Description");

        jLabel3.setText("Location");

        jLabel4.setText("Name");

        jLabel5.setText("Contact");

        jLabel6.setText("TTL");

        jTextField1.setEditable(false);
        jTextField1.setText("jTextField1");
        jTextField1.setAutoscrolls(false);
        jTextField1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jTextField3.setEditable(false);
        jTextField3.setText("jTextField3");
        jTextField3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextField4.setEditable(false);
        jTextField4.setText("jTextField4");
        jTextField4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextField5.setEditable(false);
        jTextField5.setText("jTextField5");
        jTextField5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextField6.setEditable(false);
        jTextField6.setText("jTextField6");
        jTextField6.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextArea5.setEditable(false);
        jTextArea5.setColumns(20);
        jTextArea5.setLineWrap(true);
        jTextArea5.setRows(5);
        jTextArea5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jScrollPane6.setViewportView(jTextArea5);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(163, 163, 163)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel1)
                            .add(jLabel2))
                        .add(12, 12, 12)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 207, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 102, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(13, 13, 13)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel5)
                            .add(jLabel6)
                            .add(jLabel4)
                            .add(jLabel3))
                        .add(12, 12, 12)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(259, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(52, 52, 52)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addContainerGap(99, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General information", new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/general.png"))), jPanel1); // NOI18N

        jPanel2.setBackground(new java.awt.Color(220, 220, 190));
        jPanel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel7.setText("Interface name");

        jLabel8.setText("MTU");

        jLabel9.setText("Speed");

        jLabel10.setText("Mode");

        jLabel11.setText("Admin status");

        jLabel12.setText("Oper status");

        jLabel13.setText("Ip/Mask");

        jLabel20.setText("MAC");

        jTextField7.setEditable(false);
        jTextField7.setText("jTextField7");
        jTextField7.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextField10.setEditable(false);
        jTextField10.setText("jTextField10");
        jTextField10.setAutoscrolls(false);
        jTextField10.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField10.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField10.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextField11.setEditable(false);
        jTextField11.setText("jTextField11");
        jTextField11.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField11.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField11.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextField12.setEditable(false);
        jTextField12.setText("jTextField12");
        jTextField12.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField12.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField12.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextField13.setEditable(false);
        jTextField13.setText("jTextField13");
        jTextField13.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField13.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField13.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextField14.setEditable(false);
        jTextField14.setText("jTextField14");
        jTextField14.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField14.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField14.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextField15.setEditable(false);
        jTextField15.setText("jTextField15");
        jTextField15.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField15.setMaximumSize(new java.awt.Dimension(64, 18));
        jTextField15.setMinimumSize(new java.awt.Dimension(64, 18));

        jTextArea6.setEditable(false);
        jTextArea6.setColumns(20);
        jTextArea6.setRows(5);
        jTextArea6.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jScrollPane7.setViewportView(jTextArea6);

        jLabel14.setText("Access mode");

        jTextField8.setText("jTextField8");
        jTextField8.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jPanel13.setBackground(new java.awt.Color(220, 220, 190));
        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Vlans", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 12), new java.awt.Color(0, 0, 255))); // NOI18N

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jLabel15.setText("Mode");

        jTextField16.setEditable(false);
        jTextField16.setText("jTextField16");
        jTextField16.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabel16.setText("Trunk vlans");

        jTextField2.setEditable(false);
        jTextField2.setText("jTextField2");
        jTextField2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTextField2.setCaretPosition(1);

        jLabel17.setText("Native vlan");

        jTextField18.setEditable(false);
        jTextField18.setText("jTextField18");
        jTextField18.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabel24.setText("Access vlan");

        jTextField21.setEditable(false);
        jTextField21.setText("jTextField18");
        jTextField21.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        org.jdesktop.layout.GroupLayout jPanel13Layout = new org.jdesktop.layout.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel17)
                    .add(jLabel16)
                    .add(jLabel24))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel13Layout.createSequentialGroup()
                        .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jTextField18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 64, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jTextField21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 64, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jTextField2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE))
                .addContainerGap())
            .add(jPanel13Layout.createSequentialGroup()
                .add(54, 54, 54)
                .add(jLabel15)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jTextField16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel15)
                    .add(jTextField16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel16)
                    .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(jTextField18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel24)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jTextField21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jScrollPane15))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                .add(30, 30, 30)
                                .add(jLabel14)
                                .add(104, 104, 104)
                                .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 35, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jLabel13)
                                    .add(jLabel7)
                                    .add(jLabel20)
                                    .add(jLabel11)
                                    .add(jLabel9))
                                .add(5, 5, 5)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane7)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 225, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jTextField14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                            .add(jLabel8)
                                            .add(jPanel2Layout.createSequentialGroup()
                                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                    .add(jLabel12)
                                                    .add(jLabel10))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jTextField15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                .add(12, 12, 12)
                                .add(jPanel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jLabel20)
                                            .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jLabel11)
                                            .add(jTextField14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jLabel9)
                                            .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(27, 27, 27)
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jTextField15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jLabel12)))
                                    .add(jPanel2Layout.createSequentialGroup()
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                            .add(jLabel8))
                                        .add(34, 34, 34)
                                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                            .add(jLabel10)
                                            .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))))
                            .add(jLabel13)))
                    .add(jPanel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 129, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(213, 213, 213)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(22, 22, 22))
        );

        jTabbedPane1.addTab("Interfaces Information", new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/interfaces.png"))), jPanel2); // NOI18N

        jPanel3.setBackground(new java.awt.Color(220, 220, 190));
        jPanel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane3.setViewportView(jTextArea1);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Route Information", new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/router.png"))), jPanel3); // NOI18N

        jPanel4.setBackground(new java.awt.Color(220, 220, 190));
        jPanel4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel27.setText("Id vlan");

        jLabel28.setText("Name vlan");

        jTextField26.setEditable(false);
        jTextField26.setText("jTextField26");
        jTextField26.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jTextField27.setEditable(false);
        jTextField27.setText("jTextField27");
        jTextField27.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(170, 170, 170)
                .add(jLabel27)
                .add(12, 12, 12)
                .add(jTextField26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 43, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jLabel28)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jTextField27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 87, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(229, 229, 229))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(149, 149, 149)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel27)
                    .add(jLabel28)
                    .add(jTextField27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jTextField26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(180, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Vlans Information", new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/vlans.png"))), jPanel4); // NOI18N

        jPanel6.setBackground(new java.awt.Color(220, 220, 190));

        jScrollPane10.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Ip address", "Port", "Mac", "Last time"
            }
        ) {
            final boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane10.setViewportView(jTable2);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Set filter:", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));
        jPanel7.setOpaque(false);

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectItem2(evt);
            }
        });

        jLabel21.setText("Select port:");

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel21)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jComboBox2, 0, 176, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel21)
                    .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(71, 71, 71)
                        .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jScrollPane10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .add(jScrollPane10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 253, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(15, 15, 15)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Clients Information", new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/images/client.png"))), jPanel6); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 190, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(10, 10, 10)
                .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 390, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 390, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>
    ////////////////////////////////////////////////////
    
    private void SelectItem2(java.awt.event.ActionEvent evt)//GEN-FIRST:event_SelectItem2
    {//GEN-HEADEREND:event_SelectItem2
        String SelectedItem = (String)jComboBox2.getSelectedItem();
        if(Objects.requireNonNull(SelectedItem).equals("all ports")) FillClients("all");
        else 
            FillClients(SelectedItem);
        jComboBox2.setSelectedItem(SelectedItem);
    }//GEN-LAST:event_SelectItem2

    private void SelectEvent(javax.swing.event.TreeSelectionEvent evt)//GEN-FIRST:event_SelectEvent
    {//GEN-HEADEREND:event_SelectEvent


        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree1.getLastSelectedPathComponent();
        if (node !=null && node.isLeaf()) 
        {
            String str=node.toString();
            String parent=node.getParent().toString();
            if(str.equals("General")) 
            {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(0, true);
                jTabbedPane1.setSelectedIndex(0);
                FillGeneral();
            }
            else if(parent.equals("Interfaces"))
            {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(1, true);
                jTabbedPane1.setSelectedIndex(1);
                FillInterface(str);
            }
            else if(str.equals("Route information"))
            {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(2, true);
                jTabbedPane1.setSelectedIndex(2);
                FillRoutes();
            }
            else if(parent.equals("Vlans"))
            {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(3, true);
                jTabbedPane1.setSelectedIndex(3);
                FillVlan(str);
            }
            else if(str.equals("Clients information"))
            {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(4, true);
                jTabbedPane1.setSelectedIndex(4);
                FillClients("all");
            }
            else if(item_numtabbed_numpannel.get(str) != null) {
                for(int i=0; i<jTabbedPane1.getTabCount(); i++) jTabbedPane1.setEnabledAt(i, false);
                jTabbedPane1.setEnabledAt(item_numtabbed_numpannel.get(str)[0], true);
                jTabbedPane1.setSelectedIndex(item_numtabbed_numpannel.get(str)[0]);
                FillExternal(external.get(str), jPanel_external_list.get(item_numtabbed_numpannel.get(str)[1]));
            }
        }
    }//GEN-LAST:event_SelectEvent

    private void jTree1TreeWillExpand(javax.swing.event.TreeExpansionEvent evt) {
        //GEN-HEADEREND:event_jTree1TreeWillExpand
        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";
        String ifDescr = "1.3.6.1.2.1.2.2.1.2";
        if(evt.getPath().toString().contains("Interfaces") && !run_task_iface)
        {
            ifacename_status.clear();
            String url = "https://"+Main.neb_server+":"+Main.neb_server_port+"/snmpwalk";
            String request_status = ip+";"+community+";"+version+";"+ifOperStatus;
            String request_iface_name = ip+";"+community+";"+version+";"+ifDescr;
            
            GetNodePropertiesForm.run_task_iface=true;
            WaitCircle wait_circle = new WaitCircle();
            wait_circle.start();              
            
            Map<String, String> id_status = new HashMap<>();
            String result = Utils.HTTPSRequestPOST(url, request_status);
            if(!result.equals("")) {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject;
                try {
                    jsonObject = (JSONObject)parser.parse(result);

                    Map response_map = Main.utils.toMap(jsonObject);
                    if(response_map.get(ip) != null) {
                        ArrayList<ArrayList<String>> list = (ArrayList<ArrayList<String>>)response_map.get(ip);
                        for(ArrayList<String> item : list) {
                            String id = item.get(0).split("\\.")[item.get(0).split("\\.").length - 1];
                            String status = item.get(1);
                            id_status.put(id, status);
                        }
                    }
                } catch (ParseException ex) {
                    wait_circle.end=true;
                    GetNodePropertiesForm.run_task_iface=false;
                    GetNodePropertiesForm.jTree1.repaint();                    
                    Logger.getLogger(GetNodePropertiesForm.class.getName()).log(Level.SEVERE, null, ex);
                }                        
            } else {
                System.out.println("Error post request: url - "+url+" val - "+request_status);
            }    
            
            Map<String, String> id_iface_name = new HashMap<>();
            result = Utils.HTTPSRequestPOST(url, request_iface_name);
            if(!result.equals("")) {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject;
                try {
                    jsonObject = (JSONObject)parser.parse(result);

                    Map response_map = Main.utils.toMap(jsonObject);
                    if(response_map.get(ip) != null) {
                        ArrayList<ArrayList<String>> list = (ArrayList<ArrayList<String>>)response_map.get(ip);
                        for(ArrayList<String> item : list) {
                            String id = item.get(0).split("\\.")[item.get(0).split("\\.").length - 1];
                            String iface_name = item.get(1);
                            id_iface_name.put(id, iface_name);
                        }
                    }
                } catch (ParseException ex) {
                    wait_circle.end=true;
                    GetNodePropertiesForm.run_task_iface=false;
                    GetNodePropertiesForm.jTree1.repaint();                    
                    Logger.getLogger(GetNodePropertiesForm.class.getName()).log(Level.SEVERE, null, ex);
                }                        
            } else {
                System.out.println("Error post request: url - "+url+" val - "+request_iface_name);
            }
            
            // merging: ifacename->status 
//            Map<String, String> ifacename_status = new HashMap();
            if(!id_status.isEmpty() && !id_iface_name.isEmpty()) {
                for (Map.Entry<String, String> entry : id_iface_name.entrySet()) {
                    String id = entry.getKey();
                    String ifacename = entry.getValue();
                    if(id_status.get(id) != null) {
                        ifacename_status.put(ifacename, id_status.get(id));
                    }
                }
            }
            
            wait_circle.end=true;
            GetNodePropertiesForm.run_task_iface=false;
            GetNodePropertiesForm.jTree1.repaint();
        }
    }//GEN-LAST:event_jTree1TreeWillExpand

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void FillGeneral()
    {
        jTextField1.setText(ip);
        jTextArea5.setText(sysDescription);
        jTextArea5.setCaretPosition(0);
        jTextField3.setText(sysLocation);
        jTextField4.setText(sysName);
        jTextField5.setText(sysContact);
        jTextField6.setText(defaultTTL);
        jTextField1.setBorder(BorderFactory.createCompoundBorder(jTextField1.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        jTextArea5.setBorder(BorderFactory.createCompoundBorder(jTextArea5.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        jTextField3.setBorder(BorderFactory.createCompoundBorder(jTextField3.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        jTextField4.setBorder(BorderFactory.createCompoundBorder(jTextField4.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        jTextField5.setBorder(BorderFactory.createCompoundBorder(jTextField5.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        jTextField6.setBorder(BorderFactory.createCompoundBorder(jTextField6.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 5)));
    }

    private void FillInterface(String interfName)
    {
        jTextField7.setText("");
        jTextArea6.setText("");
        jTextField10.setText("");
        jTextField12.setText("");
        jTextField14.setText("");
        jTextField11.setText("");
        jTextField13.setText("");
        jTextField15.setText("");
        jTextField16.setText("");
        jTextField2.setText("");
        jTextField18.setText("");
        jTextField21.setText("");

        jTextField7.setText(interfName);
        jTextField7.setBorder(BorderFactory.createCompoundBorder(jTextField7.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        if(interfInformation != null && interfInformation.get(interfName) != null) {
//                jTextArea6.setText(entry.getValue()[6].replaceAll(":","\n "));
                if(interfInformation.get(interfName).get("mac") != null) { 
                    jTextField10.setText(interfInformation.get(interfName).get("mac"));
                    jTextField10.setBorder(BorderFactory.createCompoundBorder(jTextField10.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField10.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("mtu") != null) { 
                    jTextField11.setText(interfInformation.get(interfName).get("mtu"));
                    jTextField11.setBorder(BorderFactory.createCompoundBorder(jTextField11.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField11.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("speed") != null) { 
                    jTextField12.setText(interfInformation.get(interfName).get("speed"));
                    jTextField12.setBorder(BorderFactory.createCompoundBorder(jTextField12.getBorder(), BorderFactory.createEmptyBorder(0, 5, 0, 5)));
                    jTextField12.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("duplex") != null) { 
                    jTextField13.setText(interfInformation.get(interfName).get("duplex"));
                    jTextField13.setBorder(BorderFactory.createCompoundBorder(jTextField13.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField13.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("admin_status") != null) { 
                    jTextField14.setText(interfInformation.get(interfName).get("admin_status"));
                    jTextField14.setBorder(BorderFactory.createCompoundBorder(jTextField14.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField14.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("operation_status") != null) { 
                    jTextField15.setText(interfInformation.get(interfName).get("operation_status"));
                    jTextField15.setBorder(BorderFactory.createCompoundBorder(jTextField15.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField15.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("mode") != null) { 
                    jTextField16.setText(interfInformation.get(interfName).get("mode"));
                    jTextField16.setBorder(BorderFactory.createCompoundBorder(jTextField16.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField16.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("trunk_vlans") != null) { 
                    jTextField2.setText(interfInformation.get(interfName).get("trunk_vlans"));
                    jTextField2.setBorder(BorderFactory.createCompoundBorder(jTextField2.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField2.setCaretPosition(0); 
                }
                else if(interfInformation.get(interfName).get("trunk_vlan") != null) { 
                    jTextField2.setText(interfInformation.get(interfName).get("trunk_vlan"));
                    jTextField2.setBorder(BorderFactory.createCompoundBorder(jTextField2.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField2.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("native_vlan") != null) { 
                    jTextField18.setText(interfInformation.get(interfName).get("native_vlan"));
                    jTextField18.setBorder(BorderFactory.createCompoundBorder(jTextField18.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField18.setCaretPosition(0); 
                }
                if(interfInformation.get(interfName).get("access_vlan") != null) { 
                    jTextField21.setText(interfInformation.get(interfName).get("access_vlan"));
                    jTextField21.setBorder(BorderFactory.createCompoundBorder(jTextField21.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
                    jTextField21.setCaretPosition(0); 
                }
        }

    }

    private void FillRoutes()
    {
        for(String it : routeInformation)
        {
            jTextArea1.append(it+"\n");
        }
    }

    private void FillClients(String str)
    {
        if(clients != null)
        {
            String[] title = new String []
            {
                "", "Ip address", "Port", "Mac"
            };

            if(!clients.isEmpty())
            {
                ArrayList<String[]> list_tmp = new ArrayList();
                if(str.equals("all")) {
                    for (Map.Entry<String, ArrayList<String[]>> entry : clients.entrySet()) {
                        String iface = entry.getKey();
                        ArrayList<String[]> ip_mac_list = entry.getValue();
                        for(String[] ip_mac : ip_mac_list) {
                            String[] tmp = new String[4];
                            String ip_name = ip_mac[0];
                            if(ip_name.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                try {
                                    InetAddress address = InetAddress.getByName(ip_name);
                                    String hostname = address.getHostName();
                                    if(!ip_name.equals(hostname)) {
                                        hostname = hostname.split("\\.")[0];
                                        ip_name = ip_mac[0]+" ("+hostname+")";
                                    }
                                } catch (UnknownHostException _) {}
                            }
                            tmp[1]=ip_name;
                            tmp[2]=iface;
                            tmp[3]=ip_mac[1];
                            list_tmp.add(tmp);
                        }
                    }
                } else {
                    if(clients.get(str) != null) {
                        String iface = str;
                        for(String[] ip_mac : clients.get(str)) {
                            String[] tmp = new String[4];
                            String ip_name = ip_mac[0];
                            if(ip_name.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                try {
                                    InetAddress address = InetAddress.getByName(ip_mac[0]);
                                    String hostname = address.getHostName();
                                    if(!ip_name.equals(hostname)) {
                                        hostname = hostname.split("\\.")[0];
                                        ip_name = ip_mac[0]+" ("+hostname+")";
                                    }
                                } catch (UnknownHostException _) {}
                            }
                            tmp[1]=ip_name;
                            tmp[2]=iface;
                            tmp[3]=ip_mac[1];
                            list_tmp.add(tmp);
                        }                        
                    }
                }
                
                String[][] data = new String[list_tmp.size()][4];
                int num=0;
                for(String[] it : list_tmp) {
                    data[num][1]=it[1];
                    data[num][2]=it[2];
                    data[num][3]=it[3];                    
                    num++;
                }


                TableModel model = new javax.swing.table.DefaultTableModel(data, title);
                jTable2.setModel(model);
                RowSorter<TableModel> sorter = new TableRowSorter(model);
                jTable2.setRowSorter(sorter);
                DefaultTableCellRenderer renderer_left = new DefaultTableCellRenderer();
                renderer_left.setHorizontalAlignment(JLabel.LEFT);                
                DefaultTableCellRenderer renderer_right = new DefaultTableCellRenderer();
                renderer_right.setHorizontalAlignment(JLabel.RIGHT);
                DefaultTableCellRenderer renderer_center = new DefaultTableCellRenderer();
                renderer_center.setHorizontalAlignment(JLabel.CENTER);
                jTable2.getColumnModel().getColumn(0).setCellRenderer(new MyTableCellRenderer());
                jTable2.getColumnModel().getColumn(0).setPreferredWidth(18);
                jTable2.getColumnModel().getColumn(1).setPreferredWidth(200);
                jTable2.getColumnModel().getColumn(1).setCellRenderer(renderer_left);
                jTable2.getColumnModel().getColumn(2).setPreferredWidth(150);
                jTable2.getColumnModel().getColumn(2).setCellRenderer(renderer_left);
                jTable2.getColumnModel().getColumn(3).setPreferredWidth(150);
                jTable2.getColumnModel().getColumn(3).setCellRenderer(renderer_left);

                ArrayList<String> iface_list = new ArrayList();
                for (Map.Entry<String, ArrayList<String[]>> entry : clients.entrySet()) {
                    String iface = entry.getKey();
                    iface_list.add(iface);
                }
                
                    
                String[] masiv = new String[iface_list.size()+1];
                masiv[0]="all ports";
                int ii=1;
                for(String iface : iface_list) {
                    masiv[ii]=iface;
                    ii++;
                }
                Arrays.sort(masiv);
                jComboBox2.setModel( new DefaultComboBoxModel(masiv));
                jComboBox2.setSelectedItem("all ports");

                // get state client hosts
                if(!run_task_clients)
                {
                    String url = "https://"+Main.neb_server+":"+Main.neb_server_port+"/ping";
                    StringBuilder nodes = new StringBuilder();
                    for(String[] it : list_tmp) {
                        nodes.append(it[1]).append("\n");
                    }
            
                    GetNodePropertiesForm.run_task_clients=true;
                    WaitCircle wait_circle = new WaitCircle();
                    wait_circle.end=false;
                    wait_circle.start();                 
            
                    String result = Utils.HTTPSRequestPOST(url, nodes.toString());
                    if(!result.equals("")) {
                        JSONParser parser = new JSONParser();
                        JSONObject jsonObject;
                        try {
                            jsonObject = (JSONObject)parser.parse(result);

                            Map response_map = Main.utils.toMap(jsonObject);
                            
                            for(int i=0; i<GetNodePropertiesForm.jTable2.getRowCount(); i++)
                            {
                                String ip1 = (String)GetNodePropertiesForm.jTable2.getValueAt(i, 1);
                                String state = (String)response_map.get(ip1);
                                if(state != null) {
                                    if(state.equals("ok")) GetNodePropertiesForm.jTable2.setValueAt("on", i, 0);
                                    else GetNodePropertiesForm.jTable2.setValueAt("off", i, 0);
                                } else GetNodePropertiesForm.jTable2.setValueAt("off", i, 0);
                            }                            

                        } catch (ParseException ex) {
                            GetNodePropertiesForm.run_task_clients=false;
                            wait_circle.end=true;
                            GetNodePropertiesForm.jTable2.repaint();
                            Logger.getLogger(GetNodePropertiesForm.class.getName()).log(Level.SEVERE, null, ex);
                        }                        
                    } else {
                        System.out.println("Error post request: url - "+url+" val - "+nodes);
                    }                    

                    wait_circle.end=true;
                    GetNodePropertiesForm.jTable2.repaint();
                    GetNodePropertiesForm.run_task_clients=false;                    
                }
            }
        }
    }
    
    
    private void FillVlan(String vlanName)
    {
        String[] fields=vlanName.split("\\s+");
        String vlan_id = fields[0];
        jTextField26.setText(vlan_id);
        jTextField26.setBorder(BorderFactory.createCompoundBorder(jTextField26.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        String vlan_name = vlanInformation.get(vlan_id);
        if(vlan_name != null) {
            jTextField27.setText(vlan_name);
            jTextField27.setBorder(BorderFactory.createCompoundBorder(jTextField27.getBorder(), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        }
    }

    private void FillExternal(Map<String, String> external_info, JPanel pannel)
    {
        pannel.removeAll();
        for (Map.Entry<String, String> entry : external_info.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if(!key.equals("icon")) {
                key = key+" : ";
                JLabel jLabel = new javax.swing.JLabel();
                jLabel.setText(key);
                jLabel.setAlignmentX(LEFT_ALIGNMENT);
                JTextArea jTextArea = new javax.swing.JTextArea();
                jTextArea.setLineWrap(true);
                jTextArea.setWrapStyleWord(true);            
                jTextArea.setText(val);
                jTextArea.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                jTextArea.setAlignmentX(LEFT_ALIGNMENT);
                JPanel pannel_container = new JPanel();
                pannel_container.add(jLabel);
    //            pannel_container.add(new JScrollPane(jTextArea)); 
                pannel_container.add(jTextArea); 
                pannel_container.setBackground(new java.awt.Color(220, 220, 190));
                pannel_container.setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLUE, 1, true));

                JPanel pannel_group = new JPanel();
                Border margin = new EmptyBorder(10,10,10,10);
                pannel_group.setBorder(margin);
                pannel_group.add(pannel_container);
                pannel_group.setBackground(new java.awt.Color(220, 220, 190));
                pannel.setLayout(new WrapLayout(WrapLayout.LEFT));
                jTextArea.setBorder(margin);
                pannel.add(pannel_group);
            }
        }
    }
    
    public static void createGetNodePropertiesForm()
    {
            ip = (String) Main.node_sel.getAttribute("ip");
            ArrayList<ArrayList<String>> account = (ArrayList<ArrayList<String>>)Main.node_sel.getAttribute("snmp_account");
            if(account != null) {
                for(ArrayList<String> item : account) {
                    if(item.get(0).equals("snmp")) {
                        version = item.get(2);
                        community = item.get(1);
                        break;
                    }
                }
            }
            sysDescription = (String) Main.node_sel.getAttribute("sysDescription");
            sysLocation = (String) Main.node_sel.getAttribute("sysLocation");
            sysContact = (String) Main.node_sel.getAttribute("sysContact");
            sysName = (String) Main.node_sel.getAttribute("sysName");
            defaultTTL = (String) Main.node_sel.getAttribute("defaultTTL");
            interfInformation = (Map<String, Map<String, String>>)Main.node_sel.getAttribute("interfInformation");
//            if(out != null) interfInformation = (String[][]) out.get(0);
            routeInformation = (ArrayList)Main.node_sel.getAttribute("routeInformation");
//            if(out != null) routeInformation = (String[][]) out.get(0);
            vlanInformation = (Map<String, String>) Main.node_sel.getAttribute("vlanInformation");
        clients = (Map<String, ArrayList<String[]>>)Main.node_sel.getAttribute("clientsInformation");

            // get informations charts
//            charts_tree = Utils.GetInformationForCharts(ip);

            //Create and set up the window.
            frame = new JFrame(ip);

            //Create and set up the content pane.
            JComponent newContentPane = new GetNodePropertiesForm();
            newContentPane.setOpaque(true); //content panes must be opaque
            frame.setContentPane(newContentPane);
//            ImageIcon m_ImageIcon = new ImageIcon();
            URL url = Main.class.getResource("/images/idle.gif");
            frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));

            //Display the window.
            frame.pack();
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                }
            });

    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    public static javax.swing.JTable jTable2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea5;
    private javax.swing.JTextArea jTextArea6;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField21;
    private javax.swing.JTextField jTextField26;
    private javax.swing.JTextField jTextField27;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    public static javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
    
}

class WaitCircle implements Runnable
{
    Thread m_WaitCircle = null;
    public boolean end=false;

    @Override
    public void run()
    {
        // set icon wait ...
//        ImageIcon m_ImageIcon = new ImageIcon();
        URL[] url = new URL[12];
        url[0] = Main.class.getResource("/images/wait/wait_request_state1.png");
        url[1] = Main.class.getResource("/images/wait/wait_request_state2.png");
        url[2] = Main.class.getResource("/images/wait/wait_request_state3.png");
        url[3] = Main.class.getResource("/images/wait/wait_request_state4.png");
        url[4] = Main.class.getResource("/images/wait/wait_request_state5.png");
        url[5] = Main.class.getResource("/images/wait/wait_request_state6.png");
        url[6] = Main.class.getResource("/images/wait/wait_request_state7.png");
        url[7] = Main.class.getResource("/images/wait/wait_request_state8.png");
        url[8] = Main.class.getResource("/images/wait/wait_request_state9.png");
        url[9] = Main.class.getResource("/images/wait/wait_request_state10.png");
        url[10] = Main.class.getResource("/images/wait/wait_request_state11.png");
        url[11] = Main.class.getResource("/images/wait/wait_request_state12.png");

        while(!end)
        {
            for(int i=0; i<12; i++)
            {
                GetNodePropertiesForm.frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url[i]));
                try { Thread.sleep(100); } catch (InterruptedException _) {}
            }
        }
        // set default icon
        URL url_default = Main.class.getResource("/images/idle.gif");
        GetNodePropertiesForm.frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url_default));
    }

    public void start()
    {
        if (m_WaitCircle == null)
        {
            m_WaitCircle = new Thread(this);
            m_WaitCircle.start();
        }
    }

    public void stop()
    {
        end = true;
    }

}

class PingThread implements Runnable
{
    private final String host;

    public PingThread(String host)
    {
        this.host=host;
    }
    @Override
    public void run()
    {
        try
        {
            ArrayList list_ip = new ArrayList();
            list_ip.add(host);
            Map<String, String> out_ping = new PingPool().get(list_ip, Main.timeout_thread);
            String state;
            if(!out_ping.isEmpty()) {
               if(out_ping.get(host).equals("ok")) state="on"; else state="off";
            } else state="off";
            System.out.println("ip="+host+"\tstate="+state);

            for(int i=0; i<GetNodePropertiesForm.jTable2.getRowCount(); i++)
            {
                if(GetNodePropertiesForm.jTable2.getValueAt(i, 1).equals(host))
                {
//                    System.out.println(host+" - "+state);
                    GetNodePropertiesForm.jTable2.setValueAt(state, i, 0);
                    break;
                }
            }

        }
        catch(java.lang.OutOfMemoryError | java.lang.NullPointerException _) {}
    }
}

