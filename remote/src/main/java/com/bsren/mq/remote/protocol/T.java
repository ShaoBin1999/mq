package com.bsren.mq.remote.protocol;

import com.bsren.mq.remote.utils.RemotingUtils;

import java.lang.reflect.Field;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class T {

    public static void main(String[] args) {
        String permissive = System.getProperty("tls.server.mode");
        System.out.println(permissive);
    }

    private String p;

    void say(){

    }

    public static String getLocalAddress(){
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<String> ipv4 = new ArrayList<>();
            List<String> ipv6 = new ArrayList<>();
            while (networkInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()){
                    InetAddress inetAddress = en.nextElement();
                    if(!inetAddress.isLoopbackAddress()){
                        if (inetAddress instanceof Inet6Address) {
                            ipv6.add(normalizeHostAddress(inetAddress));
                        } else {
                            ipv4.add(normalizeHostAddress(inetAddress));
                        }
                    }
                }
            }
            if(!ipv4.isEmpty()){
                for (String ip : ipv4) {
                    if(ip.startsWith("127.0") || ip.startsWith("192.168")){
                        continue;
                    }
                    return ip;
                }
                return ipv4.get(ipv4.size()-1);
            }else if(!ipv6.isEmpty()){
                return ipv6.get(0);
            }
            InetAddress localHost = InetAddress.getLocalHost();
            return normalizeHostAddress(localHost);
        } catch (SocketException | UnknownHostException e) {
//            log.error("Failed to obtain local address", e);
        }
        return null;
    }

    public static String normalizeHostAddress(final InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }

}
