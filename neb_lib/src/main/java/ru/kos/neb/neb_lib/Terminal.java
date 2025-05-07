package ru.kos.neb.neb_lib;

import com.jcraft.jsch.*;
import java.io.FileWriter;
import org.apache.commons.net.telnet.TelnetClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ru.kos.neb.neb_lib.Expect;
public class Terminal {

    public static boolean DEBUG = true;
    private int timeout_ssh_telnet=10;
    private int timeout_expect=40;    
    
    public Terminal(int timeout_ssh_telnet, int timeout_expect) {
        this.timeout_ssh_telnet = timeout_ssh_telnet;
        this.timeout_expect = timeout_expect;       
    }    

    public ArrayList ConnectTelnet(String node, String user, String passwd, String passwd_enable, int timeout, List<Pattern> stop_symbols) {
        ArrayList result = new ArrayList();
        TelnetClient channel = null;
        Expect expect = null;
        List<Pattern> new_stop_symbols = new ArrayList();
        try {
            channel = new TelnetClient();
            channel.setDefaultTimeout(timeout * 1000);
            channel.connect(node);
            expect = new Expect(channel.getInputStream(),
                    channel.getOutputStream());
            List<Pattern> s_s1 = new ArrayList();
            s_s1.add(Pattern.compile("[Uu]sername\\s*:\\s*$"));
            s_s1.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
            expect.expect(timeout, s_s1);
            expect.send(user + "\n");
            List<Pattern> s_s2 = new ArrayList();
            s_s2.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
            expect.expect(timeout, s_s2);
            expect.send(passwd + "\n");
            expect.expect(timeout, stop_symbols);

            if (expect.match != null) {
                Pattern p = Pattern.compile("[\n\r]\\S+>");
                Matcher m = p.matcher(expect.match);
                if (m.matches() && !passwd_enable.equals("")) {
                    expect.send("enable\n");
                    expect.expect(timeout, s_s2);
                    expect.send(passwd_enable + "\n");
                    expect.expect(timeout, stop_symbols);
                    new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("[\r\n]", "").replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                } else {
                    new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("[\r\n]", "").replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                }
                if (channel != null && expect != null) {
                    result.add(channel);
                    result.add(expect);
                    result.add(new_stop_symbols);
                }
            }

        } catch (IOException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public ArrayList ConnectSSH(String node, String user, String passwd, String passwd_enable, int timeout, List<Pattern> stop_symbols) {
        ArrayList result = new ArrayList();
        Channel channel = null;
        Expect expect = null;
        List<Pattern> new_stop_symbols = new ArrayList();
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, node);
            session.setPassword(passwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(timeout * 1000);
            channel = session.openChannel("shell");
            channel.connect();
            expect = new Expect(channel.getInputStream(),
                    channel.getOutputStream());
            expect.expect(timeout, stop_symbols);
            if (expect.match == null) {
                expect.send("\n");
                expect.expect(timeout, stop_symbols);
            }

            if (expect.match != null) {
                Pattern p = Pattern.compile("[\n\r]\\S+>");
                Matcher m = p.matcher(expect.match);
                if (m.matches() && !passwd_enable.equals("")) {
                    expect.send("enable\n");
                    List<Pattern> s_s2 = new ArrayList();
                    s_s2.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
                    expect.expect(timeout, s_s2);
                    expect.send(passwd_enable + "\n");
                    expect.expect(timeout, stop_symbols);
                    new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("([^a-zA-Z0-9_-])", "\\\\$1")));
                } else {
                    new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("([^a-zA-Z0-9_-])", "\\\\$1")));
                }
                if (channel != null && expect != null) {
                    result.add(channel);
                    result.add(expect);
                    result.add(new_stop_symbols);
                }

            }

        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public void DisconnectTelnet(ArrayList connection) {
        if (connection.size() == 3) {
            TelnetClient channel = (TelnetClient) connection.get(0);
            Expect expect = (Expect) connection.get(1);
            expect.send("exit\n");
            expect.send("quit\n");
            expect.close();
            try {
                channel.disconnect();
            } catch (Exception ex) {
                if (DEBUG) {
                    java.util.logging.Logger.getLogger(Terminal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void DisconnectSSH(ArrayList connection) {
        if (connection.size() == 3) {
            Channel channel = (Channel) connection.get(0);
            Expect expect = (Expect) connection.get(1);
            expect.send("exit\n");
            expect.send("quit\n");
            expect.close();
            channel.disconnect();
        }
    }

    public String RunCommand(Expect expect, String cmd, int timeout, List<Pattern> stop_symbols) {
        expect.send("terminal length 0\n");
        expect.expect(timeout, stop_symbols);
//        System.out.println("before="+expect.before);
        expect.send(cmd + "\n");
        expect.expect(timeout, stop_symbols);
        String out = "";
        if (expect.before != null) {
            String[] mas = expect.before.split("\n");
            if (mas.length > 1) {
                for (int i = 1; i < mas.length; i++) {
                    out = out + "\n" + mas[i];
                }
            }
        }
        return out;
    }

    /////////////////////////////////////////////////////////////////////////
//    private static class TimeoutSockectFactory implements SocketFactory {
//
//        public Socket createSocket(String hostname, int port) throws IOException {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress(hostname, port), 1000);
//            return socket;
//        }
//
//        public Socket createSocket(InetAddress hostAddress, int port) throws IOException {
//            Socket socket = new Socket();
//            socket.connect(new InetSocketAddress(hostAddress, port), 1000);
//            return socket;
//        }
//
//        public Socket createSocket(String remoteHost, int remotePort, InetAddress localAddress, int localPort) throws IOException {
//            return new Socket();
//        }
//
//        public Socket createSocket(InetAddress remoteAddress, int remotePort, InetAddress localAddress, int localPort) throws IOException {
//            return new Socket();
//        }
//
//        public ServerSocket createServerSocket(int port) throws IOException {
//            return new ServerSocket();
//        }
//
//        public ServerSocket createServerSocket(int port, int backlog) throws IOException {
//            return new ServerSocket();
//        }
//
//        public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddress) throws IOException {
//            return new ServerSocket();
//        }
//
//        @Override
//        public InputStream getInputStream(Socket socket) throws IOException {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//        @Override
//        public OutputStream getOutputStream(Socket socket) throws IOException {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//    }
}
