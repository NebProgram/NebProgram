package ru.kos.neb_viewer;

import java.awt.*;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;
import javax.swing.tree.*;
import static ru.kos.neb_viewer.GetNodePropertiesForm.item_icon;
//import static ru.kos.neb.neb_viewer.GetNodePropertiesForm.interfInformation;

/**
 *
 * @author kos
 */
@SuppressWarnings("serial")
public class MyTreeCellRenderer extends DefaultTreeCellRenderer
{
    Icon iconNode=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/node.png")));
    Icon iconGeneral=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/general.png")));
    Icon iconInterfaces=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/interfaces.png")));
    Icon iconInterf_up=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/interf_up.png")));
    Icon iconInterf_down=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/interf_down.png")));
    Icon iconRoutre=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/router.png")));
    Icon iconVlans=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/vlans.png")));
    Icon iconVlan=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/vlan.png")));
    Icon iconClient=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/client.png")));
    
    /** Creates a new instance of MyTreeCellRenderer */
    public MyTreeCellRenderer()
    {
    }
    @Override
    public Component getTreeCellRendererComponent(JTree tree,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

//        System.out.println("value="+value.toString());
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//        if(node != null && node.getParent() != null) System.out.println("value="+value.toString()+"\tparent="+node.getParent().toString());
        int level = node.getLevel();
//        System.out.println("level="+level);
        if (level == 0 && value.toString().equals(GetNodePropertiesForm.ip))
        {
            setIcon(iconNode);
        }
        if (level == 1 && value.toString().equals("General"))
        {
            setIcon(iconGeneral);
        }
        if (level == 1 && value.toString().equals("Interfaces"))
        {
            setIcon(iconInterfaces);
        }
        
//        if (level == 2 && GetNodePropertiesForm.interfInformation.get(value.toString()) != null) {
//            if(!GetNodePropertiesForm.run_task_iface) {
//                String iface_name = value.toString();
//                String mode_iface = GetNodePropertiesForm.response_http.get(iface_name);
//                if(mode_iface != null) {
//                    if(mode_iface.equals("1")) setIcon(iconInterf_up);
//                    else if(mode_iface.equals("2")) setIcon(iconInterf_down);
//                    else setIcon(iconInterf_error);
//                } else setIcon(iconInterf_error);
//            } else setIcon(iconInterf_up);
//        }
        
//        int i=0;
//        for (Map.Entry<String, Map<String, String>> entry : GetNodePropertiesForm.interfInformation.entrySet()) {
        if(!GetNodePropertiesForm.run_task_iface) {
            String status = GetNodePropertiesForm.ifacename_status.get(value.toString());
            if (level == 2 && status != null)
            {
                if(status.equals("1")) setIcon(iconInterf_up);
                else setIcon(iconInterf_down);
            }
        }
//            i++;
//        }

        if (level == 1 && value.toString().equals("Route information"))
        {
            setIcon(iconRoutre);
        }
        if (level == 1 && value.toString().equals("Vlans"))
        {
            setIcon(iconVlans);
        }
//        if(level == 2 && value.toString().equals("(default)"))
//        {
////            int kos=1;
//        }
        if(GetNodePropertiesForm.vlanInformation != null ) {
            for (Map.Entry<String, String> entry : GetNodePropertiesForm.vlanInformation.entrySet()) {
                 if (level == 2 && value.toString().equals(entry.getKey()+"  ("+entry.getValue()+")"))
                {
                    setIcon(iconVlan);
                    break;
                }
            }
        }
        if (level == 1 && value.toString().equals("Clients information"))
        {
            setIcon(iconClient);
        }

        if (level == 1 && item_icon.get(value.toString()) != null)
        {
            String path_image = Main.path+"/images/"+item_icon.get(value.toString());
            Icon icon=new ImageIcon(path_image);
            setIcon(icon);
        }

        return this;
    }
    
}
