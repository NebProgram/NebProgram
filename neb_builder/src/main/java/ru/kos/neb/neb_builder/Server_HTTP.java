package ru.kos.neb.neb_builder;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.parser.JSONParser;
import static ru.kos.neb.neb_builder.Neb.home_dir;
import static ru.kos.neb.neb_builder.Neb.logger_user;
import static ru.kos.neb.neb_builder.Neb.utils;
import ru.kos.neb.neb_lib.GetList;
import ru.kos.neb.neb_lib.PingPool;

import ru.kos.neb.neb_lib.WalkPool;

public class Server_HTTP extends Thread {
    private int port = 8080;
//    private String cert = "cert.jks";
    public static Map INFO = new HashMap();
//    public static Map INFO_sum = new HashMap();
    public static Map INFO_NAMES = new HashMap();
    public static ArrayList<String[]> file_key_val_list = new ArrayList();
    public static final int MAXPOOLTHREADS = 16;
    
//    private static Map<String, String> lastmodify = new HashMap();
    
    public static Map<String, Long> WHO_ONLINE = new HashMap();
    
    public Server_HTTP(int port) {
        this.port = port;
    }    
    
    @Override
    public void run() {
        try {
            // set up the socket address
            InetSocketAddress address = new InetSocketAddress(this.port);

            // initialise the HTTPS server
            HttpServer httpServer = HttpServer.create(address, 0);

            httpServer.createContext("/get", new GetInfo());
            httpServer.createContext("/set", new SetInfo());
            httpServer.createContext("/ipbyname", new IpByName());
            httpServer.createContext("/find", new FindInfo());
            httpServer.createContext("/find_full_text", new FindFullTextInfo());
            httpServer.createContext("/delete", new Delete());
            httpServer.createContext("/del_from_list", new Del_From_List());
            httpServer.createContext("/deletenode", new DeleteNode());
            httpServer.createContext("/deletelink", new DeleteLink());
            httpServer.createContext("/addnode", new Add_Node());
            httpServer.createContext("/addlink", new Add_Link());            
            httpServer.createContext("/add_to_list", new Add_To_List());
            httpServer.createContext("/list", new GetListKey());
            httpServer.createContext("/getfiles_list", new GetFilesList());
            httpServer.createContext("/getfile", new GetFile());
            httpServer.createContext("/getfile_attributes", new GetFileAttributes());
            httpServer.createContext("/commit", new Commit());
            httpServer.createContext("/cancel", new Cancel());
            httpServer.createContext("/setchunk", new SetChunk());
            httpServer.createContext("/delchunk", new DelChunk());
            httpServer.createContext("/snmpget", new SnmpGet());
            httpServer.createContext("/snmpwalk", new SnmpWalk());
            httpServer.createContext("/ping", new Ping());
            httpServer.createContext("/status", new Status());
            httpServer.createContext("/writefile", new WriteFile());
            httpServer.createContext("/pretty_json", new PrettyJSON());
            httpServer.createContext("/delfile", new DelFile());
            httpServer.createContext("/copyfile", new CopyFile());
            httpServer.createContext("/who", new Who());
            httpServer.createContext("/log", new WrileLog());
            httpServer.createContext("/getvar", new GetVar());
//            httpServer.createContext("/cluster_delete_node", new ClusterDeleteNode());
            httpServer.createContext("/set_image_nodes", new SetImageNodes());
            httpServer.createContext("/encryption", new Encryption());
            httpServer.createContext("/decryption", new Decryption());
            httpServer.createContext("/master_key", new MasterKey());
            httpServer.createContext("/is_input_master_key", new IsInputMasterKey());

            httpServer.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(MAXPOOLTHREADS));
            httpServer.start();

        } catch (IOException exception) {
            Neb.RELOAD_PROGRESS = false;
            System.out.println("Failed to create HTTPS server on port " + this.port + " of localhost");
//            exception.printStackTrace();

        }
    }
    
    public static void response(HttpExchange he, int cod_response, String msg) {
        try {
            String response = msg;
            he.sendResponseHeaders(cod_response, msg.getBytes().length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }    
        } catch(IOException ex) { 
            Neb.RELOAD_PROGRESS = false;
//            ex.printStackTrace(); 
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);
        }
    }
    
    public static boolean check_access_read(HttpExchange he) {
        String client_ip = he.getRemoteAddress().getAddress().getHostAddress();
        Headers headers=he.getRequestHeaders();
        String user = headers.getFirst("user");
        if(check_client_write(he) || check_client_read(he)) {
            logger_user.Println("client_ip: "+client_ip+"    user: " + user + " - OK.", logger_user.DEBUG);
            return true;
        } else {
            logger_user.Println("client_ip: "+client_ip+"    user: " + user + " - ERR!", logger_user.DEBUG);
            return false;
        }
    }
    
    public static boolean check_access_write(HttpExchange he) {
        String client_ip = he.getRemoteAddress().getAddress().getHostAddress();
        Headers headers=he.getRequestHeaders();
        String user = headers.getFirst("user");
        if(check_client_write(he)) {
            logger_user.Println("client_ip: "+client_ip+"    user: " + user + " - OK.", logger_user.DEBUG);
            return true;
        } else {
            logger_user.Println("client_ip: "+client_ip+"    user: " + user + " - ERR!", logger_user.DEBUG);
            return false;
        }
    }    
    
    public static boolean check_client_read(HttpExchange he) {
        boolean result = false;
        String client_ip = he.getRemoteAddress().getAddress().getHostAddress();
        Headers headers=he.getRequestHeaders();
        String user;
        if(headers.getFirst("user") != null && headers.getFirst("passwd") != null) {
            user = headers.getFirst("user").toLowerCase();

            String[] mas = user.split("/");
            if(mas.length == 2)
                user = mas[1];
            else {
                mas = user.split("\\\\");
                if(mas.length == 2)
                    user = mas[1];
                else {
                    mas = user.split("@");
                    if(mas.length == 2)
                        user = mas[0];
                }
            }        
            String passwd = headers.getFirst("passwd");

            if(user != null && passwd != null) {
                if(Neb.cfg.get("users") != null && ((Map)Neb.cfg.get("users")).get(user) != null) {
                    Map<String, String> user_info = (Map)((Map)Neb.cfg.get("users")).get(user);
                    String passwd_cfg = user_info.get("passwd");
                    String passwd_cfg_open = Neb.neb_lib_utils.decrypt(ru.kos.neb.neb_lib.Utils.master_key, passwd_cfg);

                    if(( (passwd_cfg != null && passwd_cfg.equals(passwd) ) || (passwd_cfg_open != null && passwd_cfg_open.equals(passwd)) ) 
                            && user_info.get("access").equals("read")) {
                        result = checkClientsIP(client_ip, user_info.get("clients"));
                    }
                    else {
                        if(Neb.cfg.get("AD_auth") != null) {
                            String serverName = (String)((Map) Neb.cfg.get("AD_auth")).get("serverName");
                            String domainName = (String)((Map) Neb.cfg.get("AD_auth")).get("domainName");
                            Map<String, Map> users_AD = (Map)((Map) Neb.cfg.get("AD_auth")).get("users_AD");
                            Map<String, Map> users_AD_lower = new HashMap();
                            if(users_AD != null) {
                                for(Map.Entry<String, Map> entry : users_AD.entrySet()) {
                                    users_AD_lower.put(entry.getKey().toLowerCase(), entry.getValue());
                                }
                                if(serverName != null && domainName != null) {
                                    if(users_AD_lower.get(user) != null && users_AD_lower.get(user).get("access").equals("read")) {
                                        if(Neb.utils.authDC(user, passwd, serverName, domainName)) {
                                            result = checkClientsIP(client_ip, (String)users_AD_lower.get(user).get("clients"));  
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if(Neb.cfg.get("AD_auth") != null) {
                        String serverName = (String)((Map) Neb.cfg.get("AD_auth")).get("serverName");
                        String domainName = (String)((Map) Neb.cfg.get("AD_auth")).get("domainName");
                        Map<String, Map> users_AD = (Map)((Map) Neb.cfg.get("AD_auth")).get("users_AD");
                        Map<String, Map> users_AD_lower = new HashMap();
                        if(users_AD != null) {
                            for(Map.Entry<String, Map> entry : users_AD.entrySet()) {
                                users_AD_lower.put(entry.getKey().toLowerCase(), entry.getValue());
                            }
                            if(serverName != null && domainName != null) {
                                if(users_AD_lower.get(user) != null && users_AD_lower.get(user).get("access").equals("read")) {
                                    if(Neb.utils.authDC(user, passwd, serverName, domainName)) {
                                        result = checkClientsIP(client_ip, (String)users_AD_lower.get(user).get("clients"));                                   
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public static boolean check_client_write(HttpExchange he) {
        boolean result = false;
        String client_ip = he.getRemoteAddress().getAddress().getHostAddress();

        Headers headers=he.getRequestHeaders();
        String user;
        if(headers.getFirst("user") != null && headers.getFirst("passwd") != null) {
            user = headers.getFirst("user").toLowerCase();

            String[] mas = user.split("/");
            if(mas.length == 2)
                user = mas[1];
            else {
                mas = user.split("\\\\");
                if(mas.length == 2)
                    user = mas[1];
                else {
                    mas = user.split("@");
                    if(mas.length == 2)
                        user = mas[0];
                }
            }
            String passwd = headers.getFirst("passwd");

            if(user != null && passwd != null && !user.isEmpty() && !passwd.isEmpty()) {
                if(Neb.cfg.get("users") != null && ((Map)Neb.cfg.get("users")).get(user) != null) {
                    Map<String, String> user_info = (Map)((Map)Neb.cfg.get("users")).get(user);
                    String passwd_cfg = user_info.get("passwd");
                    String passwd_cfg_open = Neb.neb_lib_utils.decrypt(ru.kos.neb.neb_lib.Utils.master_key, passwd_cfg);
                    if(( (passwd_cfg != null && passwd_cfg.equals(passwd) ) || ( passwd_cfg_open != null && passwd_cfg_open.equals(passwd)) ) 
                            && user_info.get("access").equals("write")) {
                        result = checkClientsIP(client_ip, user_info.get("clients"));
                    }
                    else {
                        if(Neb.cfg.get("AD_auth") != null) {
                            String serverName = (String)((Map) Neb.cfg.get("AD_auth")).get("serverName");
                            String domainName = (String)((Map) Neb.cfg.get("AD_auth")).get("domainName");
                            Map<String, Map> users_AD = (Map)((Map) Neb.cfg.get("AD_auth")).get("users_AD");
                            Map<String, Map> users_AD_lower = new HashMap();
                            if(users_AD != null) {
                                for(Map.Entry<String, Map> entry : users_AD.entrySet()) {
                                    users_AD_lower.put(entry.getKey().toLowerCase(), entry.getValue());
                                }                        
                                if(serverName != null && domainName != null) {
                                    if(users_AD_lower.get(user) != null && users_AD_lower.get(user).get("access").equals("write")) {
                                        if(Neb.utils.authDC(user, passwd, serverName, domainName)) {
                                            result = checkClientsIP(client_ip, (String)users_AD_lower.get(user).get("clients"));                                        
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if(Neb.cfg.get("AD_auth") != null) {
                        String serverName = (String)((Map) Neb.cfg.get("AD_auth")).get("serverName");
                        String domainName = (String)((Map) Neb.cfg.get("AD_auth")).get("domainName");
                        Map<String, Map> users_AD = (Map)((Map) Neb.cfg.get("AD_auth")).get("users_AD");
                        Map<String, Map> users_AD_lower = new HashMap();
                        if(users_AD != null) {
                            for(Map.Entry<String, Map> entry : users_AD.entrySet()) {
                                users_AD_lower.put(entry.getKey().toLowerCase(), entry.getValue());
                            }                        
                            if(serverName != null && domainName != null) {
                                if(users_AD_lower.get(user) != null && users_AD_lower.get(user).get("access").equals("write")) {
                                    if(Neb.utils.authDC(user, passwd, serverName, domainName)) {
                                        result = checkClientsIP(client_ip, (String)users_AD_lower.get(user).get("clients"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    } 
    
    private static boolean checkClientsIP(String client_ip, String clients) {
        if(clients != null) {
            if(client_ip.equals("0:0:0:0:0:0:0:1"))
                client_ip = "127.0.0.1";
//            logger.Println("client_ip="+client_ip+"    clients: " + clients, logger.INFO);
            String[] mas1 = clients.split(",");
            ArrayList<String> clients_list = new ArrayList();
            for(String ip : mas1) {
                if(!ip.trim().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    try {
                       ip = InetAddress.getByName(ip).getHostAddress();
                    } catch (UnknownHostException e) {
                       ip = "";
                    }
                }                            
                clients_list.add(ip);
            }
            return clients_list.contains(client_ip);//                logger.Println("true", logger.INFO);
//                logger.Println("false", logger.INFO);
        } else
            return true;
        
    }
        
    public static boolean WriteStrToFile(String filename, String str, boolean append) {
        try {
            try (Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), StandardCharsets.UTF_8))) {
                outFile.write(str);
            }
            return true;
        } catch (IOException ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);
            return false;
        }             
    }    
}

class Server_HTTPS extends Thread {
    private int port = 9090;
    private final String cert = "cert.jks";

    public Server_HTTPS(int port) {
        this.port = port;
    }    
    
    @Override
    public void run() {
        try {
            // set up the socket address
            InetSocketAddress address = new InetSocketAddress(this.port);

            // initialise the HTTPS server
            HttpsServer httpsServer = HttpsServer.create(address, 0);
            
            // ssl settings
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // initialise the keystore
            char[] password = "1qaz2wsx".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(Neb.home_dir+cert);
            ks.load(fis, password);

            // set up the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // set up the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // set up the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext context = getSSLContext();
                        SSLEngine engine = context.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // Set the SSL parameters
                        SSLParameters sslParameters = context.getSupportedSSLParameters();
                        params.setSSLParameters(sslParameters);

                    } catch (Exception ex) {
                        Neb.RELOAD_PROGRESS = false;
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);                        
                        System.out.println("Failed to create HTTPS port");
                    }
                }
            });
////////////////////////////            
//            httpServer.createContext("/token", new GetToken());
            httpsServer.createContext("/get", new GetInfo());
            httpsServer.createContext("/set", new SetInfo());
            httpsServer.createContext("/ipbyname", new IpByName());
            httpsServer.createContext("/find", new FindInfo());
            httpsServer.createContext("/find_full_text", new FindFullTextInfo());
            httpsServer.createContext("/delete", new Delete());
            httpsServer.createContext("/del_from_list", new Del_From_List());
            httpsServer.createContext("/deletenode", new DeleteNode());
            httpsServer.createContext("/deletelink", new DeleteLink());
            httpsServer.createContext("/addnode", new Add_Node());
            httpsServer.createContext("/addlink", new Add_Link());             
            httpsServer.createContext("/add_to_list", new Add_To_List());
            httpsServer.createContext("/list", new GetListKey());
            httpsServer.createContext("/getfiles_list", new GetFilesList());
            httpsServer.createContext("/getfiles_list_attribute", new GetFilesListAttributes());
            httpsServer.createContext("/getfile", new GetFile());
            httpsServer.createContext("/getfile_attributes", new GetFileAttributes());
            httpsServer.createContext("/commit", new Commit());
            httpsServer.createContext("/cancel", new Cancel());
            httpsServer.createContext("/setchunk", new SetChunk());
            httpsServer.createContext("/delchunk", new DelChunk());
            httpsServer.createContext("/snmpget", new SnmpGet());
            httpsServer.createContext("/snmpwalk", new SnmpWalk());
            httpsServer.createContext("/ping", new Ping());
            httpsServer.createContext("/status", new Status());
            httpsServer.createContext("/writefile", new WriteFile());
            httpsServer.createContext("/pretty_json", new PrettyJSON());
            httpsServer.createContext("/delfile", new DelFile());
            httpsServer.createContext("/copyfile", new CopyFile());
            httpsServer.createContext("/who", new Who());
//            httpsServer.createContext("/cluster_delete_node", new ClusterDeleteNode());
            httpsServer.createContext("/set_image_nodes", new SetImageNodes());
            httpsServer.createContext("/encryption", new Encryption());
            httpsServer.createContext("/decryption", new Decryption());
            httpsServer.createContext("/master_key", new MasterKey());
            httpsServer.createContext("/is_input_master_key", new IsInputMasterKey());
//            httpsServer.createContext("/get_secret", new GetSecret());
            
//            httpsServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            httpsServer.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(Server_HTTP.MAXPOOLTHREADS));
            httpsServer.start();

        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException exception) {
            System.out.println("Failed to create HTTPS server on port " + this.port + " of localhost");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(exception.toString(), Neb.logger.INFO);            

        }
    }
    
    public static void response(HttpExchange he, int cod_response, String msg) {
        try {
            String response = msg;
            he.sendResponseHeaders(cod_response, msg.getBytes().length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(response.getBytes());
            }    
        } catch(IOException ex) { 
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace(); 
//            Neb.logger.Println(ex.toString(), Neb.logger.INFO);
        }
    }
}

//class GetToken implements HttpHandler {
//    @Override
//    public void handle(HttpExchange he) {
//        if(he.getRequestMethod().equalsIgnoreCase("GET")) {
//            HttpExchange httpExchange = (HttpExchange) he;
//            he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
//            Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
//            String user = params.get("user");
//            String passwd = params.get("passwd");
//            if(user != null && passwd != null) {
//                Utils neb_lib_utils = new Utils();
//                String hash_str = neb_lib_utils.Hashing(user+passwd);
//                if(hash_str != null) Server_HTTP.response(he, 200, hash_str);
//                else Server_HTTP.response(he, 200, "\n");
//            } else {
//                Server_HTTP.response(he, 400, "Error query: not set user or passwd");
//            }
//        }
//    }
//}

class GetInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {         
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            if(file.equals(Neb.map_file)) {
                                Object value = Neb.utils.getKey(key, Server_HTTP.INFO);
                                if(value != null) {
                                    Gson gson = new Gson(); 
                                    String info_str = gson.toJson(value);                             
                                    Server_HTTP.response(he, 200, info_str);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");
                            } else {
                                Map info_map = Neb.utils.readJSONFile(file);
                                Object key_value = Neb.utils.getKey(key, info_map);
                                if(key_value != null) {
                                    Gson gson = new Gson(); 
                                    String info_str = gson.toJson(key_value);                             
                                    Server_HTTP.response(he, 200, info_str);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");                        
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }  else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }
    }
}

class Status implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                if(Server_HTTP.check_client_write(he)) {
                    Server_HTTP.response(he, 200, "write");
                } else if(Server_HTTP.check_client_read(he)) {
                    Server_HTTP.response(he, 200, "read");
                } else {
                    Server_HTTP.response(he, 200, "not access");
                }

            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class IpByName implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String name = params.get("name");
                    if(name != null) {
                        name = utils.shablon_to_colon(name);
                        String ip = "";
                        try {
                            if(Server_HTTP.INFO_NAMES.get(name) != null)
                                ip = (String)Server_HTTP.INFO_NAMES.get(name);                            
                            ip = InetAddress.getByName(name).getHostAddress();
                        } catch (UnknownHostException ex) {
                            if(Server_HTTP.INFO_NAMES.get(name) != null)
                                ip = (String)Server_HTTP.INFO_NAMES.get(name);
                        }    
                        Server_HTTP.response(he, 200, ip);
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set name.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class FindInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String key = params.get("key");
                    if(key != null) {
                        key = utils.shablon_to_colon(key);
                        ArrayList find_list = Neb.utils.findKey(key);
                        if(!find_list.isEmpty()) {
                            Gson gson = new Gson(); 
                            String info_str = gson.toJson(find_list);                             
                            Server_HTTP.response(he, 200, info_str);                    
                        } else Server_HTTP.response(he, 400, "Error query: Key not found.");

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class FindFullTextInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String key = params.get("key");
                    if(key != null) {
                        try {
                            key = utils.shablon_to_colon(key);
                            key = key.replaceAll("\\++", " ");
                            String query_str = key;
                            
//                            String[] mas = key.split(" ");
//                            for(int i = 0; i < mas.length; i++) {
//                                char[] mas1 = mas[i].toCharArray();
//                                for(char s : mas1) {
//                                    if((int)s > 128) {
//                                        if(!mas[i].substring(mas[i].length()-1).equals("*"))
//                                            mas[i] = mas[i]+"*";
//                                        break;
//                                    }
//                                }                                     
//                            }
//                            String query_str = mas[0];
//                            for(int i = 1; i < mas.length; i++) {
//                                query_str = query_str +" AND "+ mas[i];
//                            }
                            
                            StandardAnalyzer analyzer = new StandardAnalyzer();
                            final Directory index = FSDirectory.open(Paths.get(Neb.index_dir));

                            Query q = new QueryParser("text", analyzer).parse(query_str);

                            // 3. search
                            int hitsPerPage = 10000;
                            IndexReader reader = DirectoryReader.open(index);
                            IndexSearcher searcher = new IndexSearcher(reader);
                            TopDocs docs = searcher.search(q, hitsPerPage);
                            ScoreDoc[] hits = docs.scoreDocs;     

                            // 4. display results
//                            System.out.println("Found " + hits.length + " hits.");
                            Map<String, String> area_node_attribute = new HashMap();
                            for (ScoreDoc hit : hits) {
                                int docId = hit.doc;
                                Document d = searcher.doc(docId);

                                String text = d.get("text").replace("\n", " ").replace("\r", "");
                                String pattern = query_str.replace("*", ".+").replace("?", ".").toLowerCase();
                                Pattern p = Pattern.compile(pattern);
                                Matcher m = p.matcher(text.toLowerCase());
                                if (m.find()) {
                                    String area = d.get("area");
                                    String node = d.get("node");
                                    String sysname = "";
                                    if (Server_HTTP.INFO.get(area) != null && ((Map) Server_HTTP.INFO.get(area)).get("nodes_information") != null &&
                                            ((Map) ((Map) Server_HTTP.INFO.get(area)).get("nodes_information")).get(node) != null &&
                                            ((Map) ((Map) ((Map) Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general") != null &&
                                            ((Map) ((Map) ((Map) ((Map) Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general")).get("sysname") != null)
                                        sysname = (String) ((Map) ((Map) ((Map) ((Map) Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general")).get("sysname");

                                    if (area_node_attribute.get(area + "-" + node) == null) {
                                        String str = text + ";" + area + ";" + node + ";" + sysname + "\n";
                                        area_node_attribute.put(area + "-" + node, str);

                                    }

                                    //                                System.out.println((i + 1) + ". " + d.get("text") + "\t" + d.get("area") + "\t" + d.get("node"));
                                }
                            }
                            
                            StringBuilder out = new StringBuilder();
                            for(Map.Entry<String, String> entry : area_node_attribute.entrySet()) {
                                String str = entry.getValue();
                                out.append(str);
                            }
                            
                            Server_HTTP.response(he, 200, out.toString());
                        } catch(IOException | ParseException ex) {
                            Server_HTTP.response(he, 400, "Error query: Exception!");
                            Neb.RELOAD_PROGRESS = false;
                            Neb.logger.Println(ex.toString(), Neb.logger.INFO);                            
//                            ex.printStackTrace();
                        }

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}


class GetListKey implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            if(file.equals(Neb.map_file)) {
                                String key_list = Neb.utils.getKeyList(key, Server_HTTP.INFO);
                                if(!key_list.isEmpty()) {
                                    Server_HTTP.response(he, 200, key_list);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");
                            } else {
                                Map info_map = Neb.utils.readJSONFile(file);
                                String key_list = Neb.utils.getKeyList(key, info_map);
                                if(!key_list.isEmpty()) {
                                    Server_HTTP.response(he, 200, key_list);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");                        
                            } 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server."); 
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class GetFilesList implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String directory = params.get("directory");
                    directory = utils.getAbsolutePath(directory, home_dir);

                    if(directory != null) {
                        if((new File(directory)).exists()) {
                            Stream<Path> walk = Files.walk(Paths.get(directory));
                            List<String> files_list = walk.filter(Files::isRegularFile)
                                            .map(x -> x.toString()).toList();

                            StringBuilder out = new StringBuilder();
                            for (String f : files_list) {
                                f = f.replace("\\", "/");
                                String file_relative = "";
                                if (!home_dir.isEmpty()) {
                                    String[] mas = f.split(home_dir);
                                    if (mas.length > 1)
                                        file_relative = mas[1];
                                } else
                                    file_relative = f;
                                out.append("\n").append(file_relative);
                            }
                            if(!out.toString().isEmpty()) Server_HTTP.response(he, 200, out.toString().trim());
                            else Server_HTTP.response(he, 200, "\n");
                        } else Server_HTTP.response(he, 400, "Error query: directory "+directory+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set directory.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch (IOException e) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(e.toString(), Neb.logger.INFO);            
//            e.printStackTrace();
        }
    }
}

class GetFilesListAttributes implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String directory = params.get("directory");
                    directory = utils.getAbsolutePath(directory, home_dir);

                    if(directory != null) {
                        if((new File(directory)).exists()) {
                            Stream<Path> walk = Files.walk(Paths.get(directory));
                            List<String> files_list = walk.filter(Files::isRegularFile)
                                            .map(x -> x.toString()).toList();

                            StringBuilder out = new StringBuilder();
                            for (String f : files_list) {
                                f = f.replace("\\", "/");
                                String file_relative = "";
                                if (!home_dir.isEmpty()) {
                                    String[] mas = f.split(home_dir);
                                    if (mas.length > 1)
                                        file_relative = mas[1];
                                } else
                                    file_relative = f;
                                File newFile = new File(f);
                                long size = newFile.length();
                                long modify_time = newFile.lastModified();
                                String out_str = "{ \"file\": \"" + file_relative + "\", \"size\": \"" + size + "\", \"create_time\": \"" + utils.getFileCreateTime(f) + "\", \"modify_time\": \"" + modify_time + "\" }";
                                out.append("\n").append(out_str);
                            }
                            if(!out.toString().isEmpty()) Server_HTTP.response(he, 200, out.toString().trim());
                            else Server_HTTP.response(he, 200, "\n");
                        } else Server_HTTP.response(he, 400, "Error query: directory "+directory+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set directory.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch (IOException e) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(e.toString(), Neb.logger.INFO);            
//            e.printStackTrace();
        }
    }
}


class GetFile implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                Headers h = he.getResponseHeaders();
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
//                    Neb.logger.Println("file="+file, Neb.logger.INFO);

                    if(file != null) {
                        File newFile = new File(file);
//                        System.out.println("Get file: " + newFile.getName());
                        if(newFile.exists()) {
                            String str = Neb.utils.readFileToStringWithoutComments(file);
                            byte[] allBytes;
                            // check JSON format
                            try {
                                JSONParser parser = new JSONParser();
                                parser.parse(str);
                                allBytes = str.getBytes();
                            }  catch (org.json.simple.parser.ParseException ex) {
                                allBytes = Files.readAllBytes(Paths.get(file));
                            }
                            
//                            byte[] allBytes = Files.readAllBytes(Paths.get(file));
                            h.add("Content-Type", "application/json");
                            he.sendResponseHeaders(200, allBytes.length);
                            try (OutputStream os = he.getResponseBody()) {
                                os.write(allBytes);
                            } 
                        } else {
                            Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                        }
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file.");  
                    }
                } else 
                    Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else 
                Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch (IOException e) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(e.toString(), Neb.logger.INFO);            
//            e.printStackTrace();
        }
    }
}

class GetFileAttributes implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    
                    String ip = he.getRemoteAddress().getAddress().getHostAddress();
                    Headers headers=he.getRequestHeaders();
                    String user = headers.getFirst("user"); 
                    String user_ip = user+"/"+ip;
                    Server_HTTP.WHO_ONLINE.put(user_ip, System.currentTimeMillis());
                    long heartbit_user = 2 * 3 * 60 *1000L;
                    ArrayList<String> del_user_ip = new ArrayList();
                    for(Map.Entry<String, Long> entry : Server_HTTP.WHO_ONLINE.entrySet()) {
                        user_ip = entry.getKey();
                        Long lasttime = entry.getValue();
                        if(System.currentTimeMillis()-lasttime > heartbit_user)
                            del_user_ip.add(user_ip);
                    }
                    for(String item : del_user_ip) 
                        Server_HTTP.WHO_ONLINE.remove(item);
                    
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    
                    if(file != null) {
                        File newFile = new File(file);
    //                    System.out.println("Get file attributes: " + newFile.getName());
                        if(newFile.exists()) {
                            long size = newFile.length();
                            long modify_time = newFile.lastModified();
                            String out = "{ \"size\": \""+ size +"\", \"create_time\": \""+utils.getFileCreateTime(file)+"\", \"modify_time\": \""+ modify_time +"\" }";
                            Server_HTTP.response(he, 200, out);                         
                        } else {
                            Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                        }                    

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class SetInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            Headers requestHeaders = he.getRequestHeaders();//                                ex.printStackTrace();;
//                                ex.printStackTrace();;
                            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                            InputStream is = he.getRequestBody();
                            byte[] data = new byte[contentLength];
                            is.read(data);
                            String str = new String(data);

                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "SET";
                            mas1[2] = key;
                            mas1[3] = str;
                            Server_HTTP.file_key_val_list.add(mas1);
                            Server_HTTP.response(he, 200, "OK");
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class SetChunk implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    if(file != null) {
                        if((new File(file)).exists()) {
                            Headers requestHeaders = he.getRequestHeaders();//                                ex.printStackTrace();
                            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                            InputStream is = he.getRequestBody();
                            byte[] data = new byte[contentLength];
                            is.read(data);
                            String str = new String(data);
                            //                        System.out.println(str);
                            
                            for(String line : str.split("\n")) {
                                String[] mas = line.split(";");
                                if(mas.length == 2) {
                                    String key = mas[0];
                                    String val = mas[1];
                                    String[] mas1 = new String[4];
                                    mas1[0] = file;
                                    mas1[1] = "SET";
                                    mas1[2] = key;
                                    mas1[3] = val;
                                    Server_HTTP.file_key_val_list.add(mas1);
                                }
                            }
                            //                        synchronized(QueueWorker.queue) {
                            //                            if(QueueWorker.queue.get(file) != null) {
                            //                                ArrayList old_list = QueueWorker.queue.get(file);
                            //                                old_list.addAll(tmp_list);
                            //                            } else QueueWorker.queue.put(file, tmp_list);
                            //                        }
                            Server_HTTP.response(he, 200, "OK");
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();;
        }        
    }
}

class DelChunk implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    if(file != null) {
                        if((new File(file)).exists()) {
                            Headers requestHeaders = he.getRequestHeaders();//                                ex.printStackTrace();
                            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                            InputStream is = he.getRequestBody();
                            byte[] data = new byte[contentLength];
                            is.read(data);
                            String str = new String(data);
                            
                            for(String line : str.split("\n")) {
                                String[] mas1 = new String[4];
                                mas1[0] = file;
                                mas1[1] = "DELETE";
                                mas1[2] = line;
                                mas1[3] = null;
                                Server_HTTP.file_key_val_list.add(mas1);
                            }
                            Server_HTTP.response(he, 200, "OK");
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();;
        }        
    }
}

class Delete implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "DELETE";
                            mas1[2] = key;
                            mas1[3] = null;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Del_From_List implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            Headers requestHeaders = he.getRequestHeaders();//                                ex.printStackTrace();
                            int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                            InputStream is = he.getRequestBody();
                            byte[] data = new byte[contentLength];
                            is.read(data);
                            String str = new String(data);                             
                            
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "DEL_FROM_LIST";
                            mas1[2] = key;
                            mas1[3] = str;
                            Server_HTTP.file_key_val_list.add(mas1);
                            Server_HTTP.response(he, 200, "OK");
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();;
        }        
    }
}

class DeleteNode implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String area = params.get("area");
                    String node = params.get("node");
                    if(file != null && area != null && node != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "DELETE_NODE";
                            mas1[2] = area;
                            mas1[3] = node;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

//class ClusterDeleteNode implements HttpHandler {
//    @Override
//    public void handle(HttpExchange he) { 
//        try {
//            if(Server_HTTP.check_access_write(he)) {        
//                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
//                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
//                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
//                    String area = params.get("area");
//                    String node = params.get("node");
//                    if(area != null && node != null) {
//                        
//                        int pos = 0;
//                        boolean found = false;
//                        for(Map<String, String> it : Neb.clusters_nodes_delete) {
//                            for(Map.Entry<String, String> entry : it.entrySet()) {
//                                if(entry.getKey().equals(node)) {
//                                    found = true;
//                                    break;
//                                }
//                            }
//                            if(found) break;
//                            pos++;
//                        }
//                        ArrayList<String> result = new ArrayList();
//                        if(found) {
//                            for(Map.Entry<String, String> entry : Neb.clusters_nodes_delete.get(pos).entrySet()) {
//                                if(!entry.getKey().equals(node))
//                                    result.add(entry.getValue());
//                                else
//                                    entry.getValue();
//                            }
//                        }
//                        
//                        ArrayList<String> result_new = new ArrayList();
//                        result_new.add(utils.colon_to_shablon(area)+":"+utils.colon_to_shablon(node));
//
//                        for(String it : result) {
//                            String node1 = it.split(",\\s+")[1];
//                            String area1 = it.split(",\\s+")[3];
//
//                            result_new.add(utils.colon_to_shablon(area1)+":"+utils.colon_to_shablon(node1));
//                            Neb.logger.Println("Cluster node delete: "+area1+"/"+node1, Neb.logger.DEBUG);
//                        }
//                        result = result_new;
//
//                        String out = "";
//                        for(String item : result) {
//                            out=out+"\n"+item;
//                        }
//                        Server_HTTP.response(he, 200, out.trim());
////                        } else if(count > 1) {
////                            Server_HTTP.response(he, 201, "Error delete node "+area+"/"+node+" have sibling node.");
////                        } else if(count == -1) {
////                            Server_HTTP.response(he, 201, "Error delete node "+area+"/"+node+" not found!!!");
////                        }
//                    } else {
//                        Server_HTTP.response(he, 400, "Error query: not set area and node.");  
//                    }
//                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
//            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
//        } catch(Exception ex) {
//            Neb.RELOAD_PROGRESS = false;
//            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
////            ex.printStackTrace();
//        }        
//    }
//}

class DeleteLink implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String area = params.get("area");
                    String link = params.get("link");
                    link = URLDecoder.decode(link, StandardCharsets.UTF_8);
//                    System.out.println("del link="+link);
                    if(file != null && area != null && link != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "DELETE_LINK";
                            mas1[2] = area;
                            mas1[3] = link;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Add_Node implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {          
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String area = params.get("area");
                    String node = params.get("node");
                    if(file != null && area != null && node != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "ADD_NODE";
                            mas1[2] = area;
                            mas1[3] = node;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or area or node.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Add_Link implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {          
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String area = params.get("area");
                    String link = params.get("link");
                    link = URLDecoder.decode(link, StandardCharsets.UTF_8);
//                    System.out.println("add link="+link);
                    if(file != null && area != null && link != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "ADD_LINK";
                            mas1[2] = area;
                            mas1[3] = link;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or area or link.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class SetImageNodes implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String area = params.get("area");
                    String node = params.get("node");
                    String image = params.get("image");
                    if(area != null && node != null && image != null) {
                        StringBuilder out = new StringBuilder(utils.colon_to_shablon(area) + ":" + utils.colon_to_shablon(node) + ":" + utils.colon_to_shablon(image) + "\n");
                        Map node_info = (Map)utils.getKey("/"+area+"/nodes_information/"+node, Server_HTTP.INFO);
                        if(node_info != null) {
                            Map node_info_new = new HashMap(node_info);
                            utils.setKey("/image", image, node_info_new);
                            utils.setKey("/" + area + "/nodes_information/" + node, node_info_new, Neb.area_nodes_images_buffer);
                            Neb.clusters_nodes_image = utils.define_Images(Server_HTTP.INFO, Neb.area_nodes_images_buffer);

                            // get areas_nodes_images_auto
                            Map areas_nodes_images_auto = new HashMap();
                            for (Map.Entry<String, Map> area_it : ((Map<String, Map>) Server_HTTP.INFO).entrySet()) {
                                String area1 = area_it.getKey();
                                Map<String, Map> val = area_it.getValue();
                                if (val.get("nodes_information") != null) {
                                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) val.get("nodes_information")).entrySet()) {
                                        String node1 = node_it.getKey();
                                        String image_auto = (String) utils.getKey("/" + area1 + "/nodes_information/" + node1 + "/image_auto", Server_HTTP.INFO);
                                        String image1 = utils.getNodesImage(Server_HTTP.INFO, area1, node1);
                                        if (image1 == null)
                                            utils.setKey("/" + area1 + "/" + node1 + "/image_auto", Objects.requireNonNullElse(image_auto, ""), areas_nodes_images_auto);
                                    }
                                }
                            }

                            // chahges nodes images
                            for (String[] it : Neb.clusters_nodes_image) {
                                String node1 = it[0];
                                String image1 = it[1];
                                String area1 = it[3];
                                String image_auto_new = null;
                                if (areas_nodes_images_auto.get(area1) != null &&
                                        ((Map) areas_nodes_images_auto.get(area1)).get(node1) != null &&
                                        ((Map) ((Map) areas_nodes_images_auto.get(area1)).get(node1)).get("image_auto") != null) {
                                    String image_auto = (String) ((Map) ((Map) areas_nodes_images_auto.get(area1)).get(node1)).get("image_auto");
                                    if (!image_auto.equals(image1)) {
                                        image_auto_new = image1;
                                    }
                                }
                                if (image_auto_new != null) {
                                    out.append(utils.colon_to_shablon(area1)).append(":").append(utils.colon_to_shablon(node1)).append(":").append(utils.colon_to_shablon(image_auto_new)).append("\n");
                                    Neb.logger.Println("Cluster image node: " + area1 + "/" + node1 + " is changed image - " + image_auto_new, Neb.logger.DEBUG);
                                }
                            }

                            Server_HTTP.response(he, 200, out.toString().trim());
                        } else {
                            Server_HTTP.response(he, 200, out.toString().trim());
                        }
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set area and node and image.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Add_To_List implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {          
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            try {
                                Headers requestHeaders = he.getRequestHeaders();//                                ex.printStackTrace();
                                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                                //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                                InputStream is = he.getRequestBody();
                                byte[] data = new byte[contentLength];
                                is.read(data);
                                String str = new String(data);                            

                                String[] mas1 = new String[4];
                                mas1[0] = file;
                                mas1[1] = "ADD_TO_LIST";
                                mas1[2] = key;
                                mas1[3] = str;
                                Server_HTTP.file_key_val_list.add(mas1);
//                                Neb.logger.Println("file_key_val_list: "+mas1[0]+", "+mas1[1]+", "+mas1[2]+" - "+mas1[3], Neb.logger.INFO);
                            } catch (IOException ex) {
                                Server_HTTP.response(he, 400, "ERR");
                                Neb.RELOAD_PROGRESS = false;
                                Neb.logger.Println(ex.toString(), Neb.logger.INFO);                                
                            }
                            Server_HTTP.response(he, 200, "OK");
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Commit implements HttpHandler {
    private static final long TIMEOUT = 60*1000; // msec
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_write(he)) {  
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    synchronized(QueueWorker.queue) {
    //                    System.out.println("Start commit ...");
                        for(String[] key_val : Server_HTTP.file_key_val_list) {
                            String file = key_val[0];
                            String command = key_val[1];
                            String key = key_val[2];
                            String val = key_val[3];
                            if(QueueWorker.queue.get(file) != null) {
                                ArrayList old_list = QueueWorker.queue.get(file);
                                String[] mas = new String[3];
                                mas[0] = command;
                                mas[1] = key;
                                mas[2] = val;
                                old_list.add(mas);
                            } else {
                                ArrayList<String[]> list = new ArrayList();
                                String[] mas = new String[3];
                                mas[0] = command;
                                mas[1] = key;
                                mas[2] = val;
                                list.add(mas);
                                QueueWorker.queue.put(file, list);
                            }                                
                        }
                        Server_HTTP.file_key_val_list = new ArrayList();
    //                    System.out.println("Stop commit.");
                    }

                    try { Thread.sleep(QueueWorker.sleep_timeout+1000); } catch (InterruptedException e) { }
                    long start_time = System.currentTimeMillis();
                    while(true) {
                        long time_cur = System.currentTimeMillis();
                        if(!QueueWorker.busy || time_cur - start_time > TIMEOUT) {
                            break;
                        }
                        try { Thread.sleep(100); } catch (InterruptedException e) { }
                    }
                    // clear area_nodes_images_buffer
                    Neb.area_nodes_images_buffer = new HashMap();
                    
                    Server_HTTP.response(he, 200, "OK");
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Cancel implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_write(he)) {  
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    Server_HTTP.file_key_val_list = new ArrayList();
                    Server_HTTP.response(he, 200, "OK");
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}


class SnmpGet implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {  
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        is.read(data);
                        String str = new String(data);

                        ArrayList<String[]> list_node_community_ver_oid = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            String[] mas = line.split(";");
                            if(mas.length == 4) list_node_community_ver_oid.add(mas);
                        }

                        GetList getList = new GetList();
                        Map<String, ArrayList<String[]>> res = getList.get(list_node_community_ver_oid, Neb.timeout_thread);                    
                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (NumberFormatException ex) {
                        Server_HTTP.response(he, 400, "ERR");
                        Neb.RELOAD_PROGRESS = false;
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);                        
//                        ex.printStackTrace();
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class SnmpWalk implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));
                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        is.read(data);
                        String str = new String(data);

                        ArrayList<String[]> node_community_version_oid = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            String[] mas = line.split(";");
                            if(mas.length == 4) node_community_version_oid.add(mas);
                        }

                        WalkPool walkPool = new WalkPool();
                        Map<String, ArrayList<String[]>> res = walkPool.get(node_community_version_oid, Neb.timeout_thread);

                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (NumberFormatException ex) {
                        Server_HTTP.response(he, 400, "ERR");
                        Neb.RELOAD_PROGRESS = false;
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);                        
//                        ex.printStackTrace();
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Ping implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        is.read(data);
                        String str = new String(data);

                        ArrayList<String> node_list = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            if(line.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) node_list.add(line);
                        }

                        PingPool pingPool = new PingPool();
                        Map<String, String> res = pingPool.get(node_list, Neb.timeout_thread);

                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (NumberFormatException ex) {
                        Server_HTTP.response(he, 400, "ERR");
                            Neb.RELOAD_PROGRESS = false;
                            Neb.logger.Println(ex.toString(), Neb.logger.INFO);                        
//                        ex.printStackTrace();
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class WriteFile implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    file = utils.getAbsolutePath(file, home_dir);
                    String mode = params.get("mode");
                    if(file != null && mode != null) {
                        File f = new File(file);
                        String dir = f.getParent();
                        File directory = new File(dir);
                        if(!directory.exists()) {
                            directory.mkdirs();
                        }
                        
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                        
                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        is.read(data);
                        String str = new String(data);
                        
                        if(mode.equals("new")) {
                            if(!Server_HTTP.WriteStrToFile(file, str, false))
                                if(f.exists()) f.delete();
                        } else {
                            if(!Server_HTTP.WriteStrToFile(file, str, true))
                                if(f.exists()) f.delete();                            
                        }

                        Server_HTTP.response(he, 200, "OK");                                              

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or mode.");  
                    }
                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
            Server_HTTP.response(he, 300, "Error query.");
        }        
    }
}

class PrettyJSON implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        if(Server_HTTP.check_access_write(he)) {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                String file = params.get("file");
                file = utils.getAbsolutePath(file, home_dir);
                if(file != null) {
                    File f = new File(file);
                    if(f.exists()) {
                        String str = utils.readFileToString(file);
                        String str_pretty = utils.prettyJSONOut(str);
                        if(utils.writeStrToFile(file, str_pretty, Neb.DELAY_WRITE_FILE))
                            Server_HTTP.response(he, 200, "OK");
                        else
                            Server_HTTP.response(he, 400, "Error write file: "+file);
                    } else {
                        Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                    }
                } else {
                    Server_HTTP.response(he, 400, "Error query: not set file.");  
                }
            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
    }
}

class DelFile implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        if(Server_HTTP.check_access_write(he)) {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                String file = params.get("file");
                file = utils.getAbsolutePath(file, home_dir);
                if(file != null) {
                    File f = new File(file);
                    if(f.exists()) {
                        f.delete();
                        Server_HTTP.response(he, 200, "OK");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                    }
                } else {
                    Server_HTTP.response(he, 400, "Error query: not set file.");  
                }
            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
    }
}

class CopyFile implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        if(Server_HTTP.check_access_write(he)) {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                String file_from = params.get("file_from");
                file_from = utils.getAbsolutePath(file_from, home_dir);
                String file_to = params.get("file_to");
                file_to = utils.getAbsolutePath(file_to, home_dir);
                if(file_from != null && file_to != null) {
                    File f1 = new File(file_from);
                    if(f1.exists()) {
                        try {
                            Files.copy(Paths.get(file_from), Paths.get(file_to), REPLACE_EXISTING);
                            Server_HTTP.response(he, 200, "OK");
                        } catch (IOException ex) {
                            Server_HTTP.response(he, 400, "Error copy from: "+file_from+" to: "+file_to);
                            Neb.RELOAD_PROGRESS = false;
                            Neb.logger.Println(ex.toString(), Neb.logger.INFO);                            
//                            ex.printStackTrace();
                        }
                    } else {
                        Server_HTTP.response(he, 400, "File: "+file_from+" not found.");  
                    }
                } else {
                    Server_HTTP.response(he, 400, "Error query: not set file_from or file_to.");  
                }
            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
    }
}

class Who implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); 
                    
                    StringBuilder info_str = new StringBuilder();
                    for(Map.Entry<String, Long> entry : Server_HTTP.WHO_ONLINE.entrySet()) {
                        String user_ip = entry.getKey();
                        info_str.append(user_ip).append(";");
                    }
                    Server_HTTP.response(he, 200, info_str.toString());
                } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }
    }
}

class WrileLog implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Headers requestHeaders = he.getRequestHeaders();
                    int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));

                    InputStream is = he.getRequestBody();
                    byte[] data = new byte[contentLength];
                    is.read(data);
                    String msg = new String(data);               
                    
                    Neb.logger.Println(msg, Neb.logger.DEBUG);
                    Server_HTTP.response(he, 200, "OK");

                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class GetVar implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        if(Server_HTTP.check_access_read(he)) {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                String var = params.get("var");
                if(var != null) {
//                    String out = "";
                    try {
                        Field f = Class.forName("ru.kos.neb.neb_builder.Neb").getField(var);
                        Class<?> type = f.getType();

                        if (type.toString().equals("int")) {
                            Server_HTTP.response(he, 200, String.valueOf(f.getInt(Class.forName("ru.kos.neb.neb_builder.Neb"))));
//                            System.out.println(f.getInt(Class.forName("ru.kos.neb.neb_builder.Neb")));
                        } else if (type.toString().equals("long")) {
                            Server_HTTP.response(he, 200, String.valueOf(f.getLong(Class.forName("ru.kos.neb.neb_builder.Neb"))));
//                            System.out.println(f.getInt(Class.forName("ru.kos.neb.neb_builder.Neb")));
                        } else if (type.toString().equals("double")) {
                            Server_HTTP.response(he, 200, String.valueOf(f.getDouble(Class.forName("ru.kos.neb.neb_builder.Neb"))));
//                            System.out.println(f.getInt(Class.forName("ru.kos.neb.neb_builder.Neb")));
                        } else if (type.toString().equals("float")) {
                            Server_HTTP.response(he, 200, String.valueOf(f.getFloat(Class.forName("ru.kos.neb.neb_builder.Neb"))));
//                            System.out.println(f.getInt(Class.forName("ru.kos.neb.neb_builder.Neb")));
                        } else if (type.toString().matches(".*String")) {
                            Server_HTTP.response(he, 200, (String)f.get(Class.forName("ru.kos.neb.neb_builder.Neb")));
//                            System.out.println(f.get(Class.forName("ru.kos.neb.neb_builder.Neb")));
                        } else if (type.toString().matches(".*Map")) {
                            Map map = (HashMap) f.get(Class.forName("ru.kos.neb.neb_builder.Neb"));
                            Gson gson = new Gson(); 
                            String str = gson.toJson(map);
                            Server_HTTP.response(he, 200, str);
//                            System.out.println(str);
                        } else if (type.toString().matches(".*ArrayList")) {
                            ArrayList list = (ArrayList) f.get(Class.forName("ru.kos.neb.neb_builder.Neb"));
                            StringBuilder str = new StringBuilder();
                            for(Object item : list) {
                                if(item instanceof String[] strings) {
                                    str = new StringBuilder(strings[0]);
                                    for(int i=1; i<strings.length; i++) {
                                        str.append("\\;").append(strings[i]);
                                    }
                                    str.append("\n");
                                } else if(item instanceof String string)
                                    str = new StringBuilder(string);
                            }
                            Server_HTTP.response(he, 200, str.toString());
//                            System.out.println(str);
                        }
                    } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(GetVar.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    
                } else {
                    Server_HTTP.response(he, 400, "Error query: not set var parameter.");  
                }
            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
    }
}

class Encryption implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Headers requestHeaders = he.getRequestHeaders();
                    int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                    InputStream is = he.getRequestBody();
                    byte[] data = new byte[contentLength];
                    is.read(data);
                    String str = new String(data);
                    String encrypt = Neb.neb_lib_utils.encrypt(ru.kos.neb.neb_lib.Utils.master_key, str);
                    Server_HTTP.response(he, 200, encrypt);                                              

                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class Decryption implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Headers requestHeaders = he.getRequestHeaders();
                    int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                    InputStream is = he.getRequestBody();
                    byte[] data = new byte[contentLength];
                    is.read(data);
                    String str = new String(data);
                    
                    String[] mas = str.split("\n");
                    if(mas.length == 2 && mas[1].trim().replaceAll("\n","").equals(ru.kos.neb.neb_lib.Utils.master_key)) {
                        String decrypt = Neb.neb_lib_utils.decrypt(ru.kos.neb.neb_lib.Utils.master_key, mas[0].trim().replaceAll("\n",""));
                        Server_HTTP.response(he, 200, decrypt);
                    } else 
                        Server_HTTP.response(he, 301, "Error master_key: Not correct master_key.");

                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class MasterKey implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_access_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Headers requestHeaders = he.getRequestHeaders();
                    int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                    InputStream is = he.getRequestBody();
                    byte[] data = new byte[contentLength];
                    is.read(data);
                    String str = new String(data);
                    ru.kos.neb.neb_lib.Utils.master_key = str;
                    if(utils.check_master_key(Neb.cfg, ru.kos.neb.neb_lib.Utils.master_key)) {
                        Server_HTTP.response(he, 200, "OK");
                    } else {
                        Server_HTTP.response(he, 201, "Master key is error!!!");
                    }

                } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(NumberFormatException | IOException ex) {
            Server_HTTP.response(he, 400, "ERR");
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }        
    }
}

class IsInputMasterKey implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); 
                if(utils.is_need_master_key(Neb.cfg) && ru.kos.neb.neb_lib.Utils.master_key.isEmpty()) {
                    Server_HTTP.response(he, 200, "YES");
                } else {
                    Server_HTTP.response(he, 200, "NO");
                }
            } else Server_HTTP.response(he, 300, "Error query: This query is not GET.");
        } catch(Exception ex) {
            Neb.RELOAD_PROGRESS = false;
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
//            ex.printStackTrace();
        }
    }
}

//class GetSecret implements HttpHandler {
//    @Override
//    public void handle(HttpExchange he) { 
//        try {
//            if(he.getRequestMethod().equalsIgnoreCase("POST")) {
//                he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
//                
//                Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
//                String key = params.get("key");
//                if(key != null) {                
//                    Headers requestHeaders = he.getRequestHeaders();
//                    int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
//                    InputStream is = he.getRequestBody();
//                    byte[] data = new byte[contentLength];
//                    int length = is.read(data);
//                    String str = new String(data);
//                    if(ru.kos.neb.neb_lib.Utils.master_key.equals(str)) {
//                        if(Neb.secrets.get(key) != null) {
//                            Server_HTTP.response(he, 200, Neb.secrets.get(key));
//                        } else Server_HTTP.response(he, 300, "Error query: Key "+key+" is not secrets storage!");
//                    } else {
//                        Server_HTTP.response(he, 300, "Error query: Not correct master_key!");
//                    }
//                } else Server_HTTP.response(he, 300, "Error query: Not set key parameter!");
////                ru.kos.neb.neb_lib.Utils.master_key = str;
////                if(utils.check_master_key(Neb.cfg, ru.kos.neb.neb_lib.Utils.master_key)) {
////                    Server_HTTP.response(he, 200, "OK");
////                } else {
////                    Server_HTTP.response(he, 201, "Master key is error!!!");
////                }
//
//            } else Server_HTTP.response(he, 300, "Error query: This query is not POST.");
//        } catch(NumberFormatException | IOException ex) {
//            Server_HTTP.response(he, 400, "ERR");
//            Neb.RELOAD_PROGRESS = false;
//            Neb.logger.Println(ex.toString(), Neb.logger.INFO);            
////            ex.printStackTrace();
//        }        
//    }
//}
