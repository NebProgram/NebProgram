package ru.kos.neb_viewer;

import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Objects;
import javax.swing.*;

/**
 *
 * @author kos
 */
@SuppressWarnings("serial")
public class MyTableCellRenderer extends DefaultTableCellRenderer
{
    Icon client=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/client.png")));
    Icon client_off=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/client_off.png")));
    
    /** Creates a new instance of MyTableCellRenderer */
    public MyTableCellRenderer()
    {
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        if(value != null)
        {
            if(value.equals("on")) setIcon(client);
            if(value.equals("off")) setIcon(client_off);
            if(value.equals("run")) setIcon(client_off);
        }
        else setIcon(null);
//        System.out.println("row="+row+"\tcolumn="+column+"\tvalue="+value);

        
        return this;
    }
    
}
