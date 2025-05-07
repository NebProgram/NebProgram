package ru.kos.neb_viewer;

import java.io.File;
import java.util.Objects;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
 *
 * @author kos
 */
public class ImageFileView extends FileView
{
    ImageIcon jpgIcon=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/jpgIcon.gif")));
    ImageIcon gifIcon=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/gifIcon.gif")));
    ImageIcon tiffIcon=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/tiffIcon.gif")));
    ImageIcon pngIcon=new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/pngIcon.png")));
    
    @Override
    public String getName(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public String getDescription(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public Boolean isTraversable(File f)
    {
        return null; //let the L&F FileView figure this out
    }
    
    @Override
    public String getTypeDescription(File f)
    {
        String extension = Utils.getExtension(f);
        String type = null;
        
        if (extension != null)
        {
            switch (extension) {
                case Utils.JPEG, Utils.JPG -> type = "JPEG Image";
                case Utils.GIF -> type = "GIF Image";
                case Utils.TIFF, Utils.TIF -> type = "TIFF Image";
                case Utils.PNG -> type = "PNG Image";
                default -> {
                }
            }
        }
        return type;
    }
    @Override
    public Icon getIcon(File f)
    {
        String extension = Utils.getExtension(f);
        Icon icon = null;
        
        if (extension != null)
        {
            switch (extension) {
                case Utils.JPEG, Utils.JPG -> icon = jpgIcon;
                case Utils.GIF -> icon = gifIcon;
                case Utils.TIFF, Utils.TIF -> icon = tiffIcon;
                case Utils.PNG -> icon = pngIcon;
                default -> {
                }
            }
        }
        return icon;
    }
}
