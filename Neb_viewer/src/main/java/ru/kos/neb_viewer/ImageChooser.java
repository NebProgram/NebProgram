package ru.kos.neb_viewer;

import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImageChooser extends JFrame {
    public static JFileChooser fileChooser = null;
    public static String parrent = null;
    public static String result = "";


    public ImageChooser() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Создание экземпляра JFileChooser 
        fileChooser = new JFileChooser();
        //Add custom icons for file types.
        fileChooser.setFileView(new ImgFileView());
        //Add the preview pane.
        fileChooser.setAccessory(new ImgPreview(fileChooser));
        fileChooser.setCurrentDirectory(new File(Main.path+"/"+Main.image_path));
        parrent = fileChooser.getCurrentDirectory().getPath();
        disableNewHomeFolderButton(fileChooser);
        fileChooser.setDialogTitle("Select image");
        // Определяем фильтры типов файлов
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG images", "png");
        fileChooser.addChoosableFileFilter(filter);

        // Определение режима - только файл
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int res = fileChooser.showOpenDialog(null);
        // Если файл выбран, покажем его в сообщении
        if (res == JFileChooser.APPROVE_OPTION) {
            result = fileChooser.getSelectedFile().toString().replace("\\", "/");
            String[] mas = result.split("/");
            if(mas.length > 1) {
                if(mas[mas.length-1].equals(mas[mas.length-2])) {
                    result = "";
                    for(int i=0; i<mas.length-2; i++) {
                        result = result + mas[i] + "/";
                    }
                    result = result + mas[mas.length-1];
                }
            }

        }        
    }

    private static void disableNewHomeFolderButton(Container c) {
        int len = c.getComponentCount();
        for (int i = 0; i < len; i++) {
            Component comp = c.getComponent(i);
            if (comp instanceof JComboBox cb) {
                cb.setEnabled(false);
            }
            
            if(comp != null) {
                switch (comp) {
                    case JButton b -> {
                        Icon icon = b.getIcon();
                        if (icon != null
                                && icon == UIManager.getIcon("FileChooser.newFolderIcon")) {
                            b.setEnabled(false);
                            b.setVisible(false);
                        }
                        if (icon != null
                                && icon == UIManager.getIcon("FileChooser.homeFolderIcon")) {
                            b.setEnabled(false);
                            b.setVisible(false);
                        }
                    }
                    case Container container -> disableNewHomeFolderButton(container);
                    default -> {
                    }
                }
            }
        }
    }
    
    public static void enableUpFolderButton(Container c, boolean state) {
        int len = c.getComponentCount();
        for (int i = 0; i < len; i++) {
            Component comp = c.getComponent(i);
            if(comp != null) {
                switch (comp) {
                    case JButton b -> {
                        Icon icon = b.getIcon();
                        if (icon != null
                                && icon == UIManager.getIcon("FileChooser.upFolderIcon")) {
                            b.setEnabled(state);
                        }
                    }
                    case Container container -> enableUpFolderButton(container, state);
                    default -> {
                    }
                }
            }
        }
    }
    
    public static String createImageChooser() {
        @SuppressWarnings("unused")
        ImageChooser imageChooser = new ImageChooser();
        return result;
    }
}
