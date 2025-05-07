package ru.kos.neb_viewer;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import static ru.kos.neb_viewer.Utils.HTTPSRequestGET;

@SuppressWarnings({"serial", "unchecked"})
/*

  @author  kos
 */
public class GetFileProgressBar extends javax.swing.JPanel
{
    static public JDialog frame=null;

    public GetFileProgressBar(String message, String directory, String dst_path)
    {
        initComponents();
    }
    
    public static void createGetFileProgressBar(String message, String directory, String dst_path)
    {
        frame = new JDialog(Main.getFrames()[0], "Authorized", true);
        //Create and set up the content pane.
        JComponent newContentPane = new GetFileProgressBar(message, directory, dst_path);
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
                
        Thread t = new Thread(() -> {
            try {
                jLabel2.setText(message);
                String url3 = "https://" + Main.neb_server + ":" + Main.neb_server_port + "/getfiles_list_attribute?directory=" + directory;
                String res3 = HTTPSRequestGET(url3);
                Map file_attribute_map = new HashMap();
                JSONParser parser = new JSONParser();
                String[] mas = res3.split("\n");
                jProgressBar1.setMaximum(mas.length);
                for(String line : mas) {
                    JSONObject jsonObject = (JSONObject) parser.parse(line);
                    Map file_attributes = Utils.toMap(jsonObject);
                    
                    if(file_attributes.get("file") != null && file_attributes.get("size") != null &&
                            file_attributes.get("create_time") != null && file_attributes.get("modify_time") != null) {
                        Map tmp_map = new HashMap();
                        tmp_map.put("size", file_attributes.get("size"));
                        tmp_map.put("create_time", file_attributes.get("create_time"));
                        tmp_map.put("modify_time", file_attributes.get("modify_time"));
                        file_attribute_map.put(file_attributes.get("file"), tmp_map);
                    }
                }
                
                String url = "https://" + Main.neb_server + ":" + Main.neb_server_port + "/getfiles_list?directory=" + directory;
                String res = HTTPSRequestGET(url);
                if (!res.equals("")) {
                    int i = 0;
                    for (String file : res.split("\n")) {
                        jLabel2.setText(message + " " + i);
                        jProgressBar1.setValue(i);
//                            System.out.println(jProgressBar1.getValue());
                        file = file.replace("\\", "/");
                        String url2 = "https://"+Main.neb_server+":"+Main.neb_server_port+"/getfile?file="+file;
                        File local_file = new File(dst_path+"/"+file);
                        if (!local_file.exists()) {
                            System.out.println(file);
                            Main.utils.CopyHTTPFile(url2, local_file.getPath());
                        } else {
                            long size_server = 0;
                            Map f_attr = (Map)file_attribute_map.get(file);
                            if(f_attr != null)
                                size_server = Long.parseLong((String)f_attr.get("size"));
                            long size_local = local_file.length();
                            if (size_server != size_local) {
                                System.out.println(file);
                                Main.utils.CopyHTTPFile(url2, local_file.getPath());
                            }
                        }
                        i = i + 1;
                    }
                    frame.setVisible(false);
                } else {
                    frame.setVisible(false);
                }
                
            } catch (Exception ex) {
                System.out.println(Utils.class.getName() + " - " + ex);
                frame.setVisible(false);
            }
        });
        t.start();
        

        frame.setVisible(true);
    }
    
    
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();

        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(51, 153, 0), 3, true));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("User:");

        jProgressBar1.setMaximum(500);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(125, 125, 125))
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(41, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(48, 48, 48))
        );

        getAccessibleContext().setAccessibleParent(this);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JLabel jLabel2;
    public static javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables
    
}

