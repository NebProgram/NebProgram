package ru.kos.neb_viewer;

import java.awt.Image;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

/* ImgFileView.java is used by FileChooserDemo2.java. */
public class ImgFileView extends FileView {
    public final static String JPEG = "jpeg";
    public final static String JPG = "jpg";
    public final static String GIF = "gif";
    public final static String TIFF = "tiff";
    public final static String TIF = "tif";
    public final static String PNG = "png";
    @Override
    public Boolean isTraversable(File f) {
//        System.out.println(Test2.parrent+" - "+f.getParent());
        if(f.getParent() != null && f.getParent().equals(ImageChooser.parrent)) {
            ImageChooser.enableUpFolderButton(ImageChooser.fileChooser, false);
        } else {
            ImageChooser.enableUpFolderButton(ImageChooser.fileChooser, true);
        }
        return true;
    }
    
    @Override
    public Icon getIcon(File f) {
        String extension = getExtension(f);
//        System.out.println(f.getAbsolutePath());
        Icon icon = null;

        if (extension != null) {
            switch (extension) {
                case JPEG, JPG, GIF, TIFF, TIF, PNG -> icon = loadImage(f);
                //                icon = pngIcon;
                default -> {
                }
            }
        }
        return icon;
    }
    
    public ImageIcon loadImage(File file) {
        if (file == null) {
            return null;
        }

        //Don't use createImageIcon (which is a wrapper for getResource)
        //because the image we're trying to load is probably not one
        //of this program's own resources.
        ImageIcon tmpIcon = new ImageIcon(file.getPath());
        if (tmpIcon.getIconWidth() > 22) {
            return new ImageIcon(tmpIcon.getImage().
                                      getScaledInstance(22, -1,
                                                  Image.SCALE_DEFAULT));
        } else if (tmpIcon.getIconHeight() > 12) {
            return new ImageIcon(tmpIcon.getImage().
                                      getScaledInstance(-1, 12,
                                                  Image.SCALE_DEFAULT));
        } else { //no need to miniaturize
            return tmpIcon;
        }
    }
    
    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }    
}
