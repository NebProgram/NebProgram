package ru.kos.neb.neb_lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author kos
 */
public class Utils {
    public static Logger logger;
    public static boolean DEBUG = true;
    public static boolean DEBUG_SCRIPT = false;
    public static long current_value = 0;
    public static long max_value = 0;
    public static final int MAXPOOLTHREADS = 256;
//    public static final int MAXPOOLTHREADS = 64;
    public static final int BULKSIZE = 10;
    private static final Map<String, String> encrypt_map = new HashMap();
    private static final Map<String, String> decrypt_map = new HashMap();
    
    public static String LOG_FILE = "neb_lib.log";
    public static String master_key = "";
    public static String salt = "tJHnN5b1i6wvXMwzYMRk";
    
    public Utils() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.setLevel(logger.DEBUG);
        else
            logger.setLevel(logger.INFO);
        
    }

    public long[] intervalNetworkAddress(String network) {
        String[] network_mask;
        int net;
        int mask;
        long[] interval = new long[2];

        try {
            if (network.indexOf('/') != -1) {
                network_mask = network.split("/");
                net = InetAddress.getByName(network_mask[0].trim()).hashCode();
                int num_mask = Integer.parseInt(network_mask[1].trim());
                mask = 0xffffffff;
                mask = mask << (32 - num_mask);
                interval[0] = Long.parseLong(Integer.toBinaryString((net & mask)), 2);
                interval[1] = Long.parseLong(Integer.toBinaryString((net | ~mask)), 2);                
            } else if (network.indexOf(' ') != -1) {
                network_mask = network.split(" ");
                net = InetAddress.getByName(network_mask[0].trim()).hashCode();
                mask = InetAddress.getByName(network_mask[1].trim()).hashCode();
                interval[0] = Long.parseLong(Integer.toBinaryString((net & mask)), 2);
                interval[1] = Long.parseLong(Integer.toBinaryString((net | ~mask)), 2);                   
            } else {
                interval[0] = InetAddress.getByName(network.trim()).hashCode();
                interval[1] = InetAddress.getByName(network.trim()).hashCode();
            }
            return interval;
        } catch (java.net.UnknownHostException | java.lang.NumberFormatException e) {
            interval[0] = 0;
            interval[1] = 0;
            return interval;
        }
    }

    public String networkToIPAddress(long addr) {
        long[] octets = new long[4];
        long tmp;
        String ip;

        octets[0] = addr / (256 * 256 * 256);
        tmp = addr % (256 * 256 * 256);
        octets[1] = tmp / (256 * 256);
        tmp = tmp % (256 * 256);
        octets[2] = tmp / 256;
        octets[3] = tmp % 256;
        ip = octets[0] + "."
                + octets[1] + "."
                + octets[2] + "."
                + octets[3];
        return ip;
    }

    public boolean insideInterval(String ip, String network)
    {
        try
        {
            int addr = java.net.InetAddress.getByName(ip).hashCode();
            
            if(network.indexOf('/') != -1)
            {
                String[] network_mask = network.split("/");
                int net = InetAddress.getByName(network_mask[0]).hashCode();
                int num_mask = Integer.parseInt(network_mask[1]);
                int mask = 0xffffffff;
                mask = mask << (32-num_mask);
                int net_start = net & mask;
                int net_stop = net | ~mask;
                return addr >= net_start && addr <= net_stop;
            }
            else if(network.indexOf(' ') != -1) {
                String[] network_mask = network.split(" ");
                int net = InetAddress.getByName(network_mask[0]).hashCode();
                int mask = InetAddress.getByName(network_mask[1]).hashCode();  
                int net_start = net & mask;
                int net_stop = net | ~mask;
                return addr >= net_start && addr <= net_stop;
            }
            else
            {
                return network.equals(ip);
            }
        }
        catch(java.net.UnknownHostException e)
        { return false; }
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public String encrypt(String master_key, String text) {
        if(!master_key.isEmpty() && encrypt_map.get(text) != null)
            return encrypt_map.get(text);
        else {
            try {
                if(!master_key.isEmpty()) {
                    Cipher cipher_encrypt = initCipher(master_key, salt, Cipher.ENCRYPT_MODE);
                    byte[] encrypted = cipher_encrypt.doFinal(text.getBytes());
                    String out = "AES:"+Base64.getEncoder().encodeToString(encrypted);
                    synchronized (encrypt_map) {
                        encrypt_map.put(text, out);
                    }
                    return out;
                } else return text;
            } catch (Exception ex) {
                if(!Utils.DEBUG) {
                    ex.printStackTrace();
                }
                logger.println(ex.getMessage(), logger.DEBUG);            
            }
        }
        return null;
    }      
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public String decrypt(String master_key, String text) {
        if(!master_key.isEmpty() && decrypt_map.get(text) != null)
            return decrypt_map.get(text);
        else {
            try {
                if(!master_key.isEmpty()) {
                    if(text.matches("^AES:.+")) {
                        Cipher cipher_decrypt = initCipher(master_key, salt, Cipher.DECRYPT_MODE);
                        String text1 = text.split("AES:")[1];
                        byte[] original = cipher_decrypt.doFinal(Base64.getDecoder().decode(text1));
                        String out = new String(original);
                        synchronized (decrypt_map) {
                            decrypt_map.put(text, out);
                        }
                        return out;
                    } else {
                        return text;
                    }
                } else {
                    return text;    
                }
            } catch (Exception ex) {
                if(!Utils.DEBUG) ex.printStackTrace();
                logger.println(ex.getMessage(), logger.DEBUG);            
            }
        }
        return null;                
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private static Cipher initCipher(String secretKey, String salt, int mode) throws Exception {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec skeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(mode, skeySpec, new IvParameterSpec(new byte[16]));
        return cipher;
    }
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

    public ArrayList runScripts(String script) {
        ArrayList result = new ArrayList();
        
        try {
            String[] param = splitCommandString(script);
            List list = new ArrayList(Arrays.asList(param));
//            String cmd = param[0];
            String[] paths=System.getenv("path").split(";");
            String command="";
            if(!param[0].contains(":") && !param[0].startsWith("/")) {
                for(String path : paths) {
                    String command_file = path+"/"+param[0];
                    if (new File(command_file).exists()) { list.set(0, command_file); break; }
                    else if (new File(command_file+".cmd").exists()) { list.set(0, command_file+".cmd"); break; }
                    else if (new File(command_file+".bat").exists()) { list.set(0, command_file+".bat"); break; }
                    else if (new File(command_file+".exe").exists()) { list.set(0, command_file+".exe"); break; }
                }
            }

            if(!list.get(0).equals("")) {
                Process process = new ProcessBuilder(list).start();
                OutputStream os = process.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
//                Writer w = new OutputStreamWriter(os);
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                result.add(process);
                result.add(bw);
                result.add(br);
            }
        } catch (IOException ex) {
            RunScriptsPool.logger.println("RunScripts - "+ex.getMessage(), RunScriptsPool.logger.DEBUG);
        }        
        
        return result;
    }
    
    private static String[] splitCommandString(String command) {
        Pattern p = Pattern.compile("(\"[^\"]+\"|\\S+)");
        String cmd = "";
        Matcher m1 = p.matcher(command);
        if(m1.find()) {        
            while(true) {
                Matcher m = p.matcher(command);
                if(m.find()) {
                    String find_str = m.group(1);
                    command = command.substring(m.start()+find_str.length());
                    find_str = find_str.replaceAll("\\s+", "%BLANK%");
                    cmd = cmd + find_str + " ";
    //                command = command.replace(find_str, find_str.replaceAll("\\s+", "%BLANK%"));
                } else
                    break;
            }
        } else
            cmd = command;
        cmd = cmd.trim();

        String[] mas = cmd.split("\\s+");
        int i=0;
        for(String s : mas) {
            mas[i] = s.replace("%BLANK%", " ");
            i++;
        }
        return mas;
    }    

    public String readFileToString(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
                sb.append(in.readLine());
                String s;
                while ((s = in.readLine()) != null) {
                    sb.append("\n").append(s);
                }
            }
        } catch (IOException e) {
            if (DEBUG) {
                System.out.println(e);
            }
//            throw new RuntimeException(e);
        }
        return sb.toString();
    }  
    
    public ArrayList addSNMPResult(String node, ArrayList<String[]> adding_list, Map<String, ArrayList<String[]>> collector, Map<String, String> uniqal_oid) {
        ArrayList result = new ArrayList();
        
        ArrayList<String[]> tmp_list = new ArrayList();
        boolean fail = false;
        for(String[] item : adding_list) {
            if(uniqal_oid.get(item[0]) == null) {
                uniqal_oid.put(item[0], item[0]);
                tmp_list.add(item);
            } else {
                fail = true;
                break;
            }
        }        
        
        if(collector.get(node) != null) {
            collector.get(node).addAll(tmp_list);
        } else {
            collector.put(node, tmp_list);
        }
        result.add(collector);
        result.add(fail);
        
        return result;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    ArrayList<String[]> snmpWalk(String node, String community, String versuon, String oid) {
        ArrayList<String[]> result = new ArrayList();
        
        
        return result;
    }
}
