package ru.kos.neb_viewer;

import java.awt.*;
import java.util.Objects;
import javax.swing.*;
import javax.swing.tree.*;
//import static ru.kos.neb.neb_viewer.GetNodePropertiesForm.interfInformation;

/**
 *
 * @author kos
 */
@SuppressWarnings("serial")
public class FindTreeCellRenderer extends DefaultTreeCellRenderer
{
    Icon icon_leaf=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/vlan.png")));
    Icon open_folder=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/FolderOpen.png")));
    Icon close_folder=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/FolderClose.png")));
    
    /** Creates a new instance of MyTreeCellRenderer */
    public FindTreeCellRenderer()
    {
    }
    @Override
    public Component getTreeCellRendererComponent(JTree tree,Object value,boolean selected,boolean expanded,boolean leaf,int row,boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
//        if(node != null && node.getParent() != null) System.out.println("value="+value.toString()+"\tparent="+node.getParent().toString());
        int level = node.getLevel();

        if (level == 1) {
            setToolTipText("");
            if(expanded) setIcon(open_folder);
            else setIcon(close_folder);
        }
        
        if (level == 2) {
            String tooltip = value.toString();

            setToolTipText(tooltip);            
            setIcon(icon_leaf);
        }        
        
        return this;
    }
    
}
