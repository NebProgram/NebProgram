package ru.kos.neb_viewer;

import java.io.File;
import javax.swing.filechooser.*;

/* ImageFilter.java is used by FileChooserDemo2.java. */
public class ImageFilter extends FileFilter {

    //Accept all directories and all gif, jpg, tiff, or png files.
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = Utils.getExtension(f);
        if (extension != null) {
            return extension.equals(Utils.TIFF) ||
                    extension.equals(Utils.TIF) ||
                    extension.equals(Utils.GIF) ||
                    extension.equals(Utils.JPEG) ||
                    extension.equals(Utils.JPG) ||
                    extension.equals(Utils.PNG);
        }

        return false;
    }

    //The description of this filter
    @Override
    public String getDescription() {
        return "Just Images";
    }
}
