package com.bsren.mq.remote.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class RemotingUtils {

    public static final String ROCKETMQ_REMOTING = "RocketmqRemoting";

    public static final String OS_NAME = System.getProperty("os.name");

    private static final Logger log = LoggerFactory.getLogger(RemotingUtils.class);
    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }
    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        return new InetSocketAddress(s[0], Integer.parseInt(s[1]));
    }

    public static Selector openSelector() throws IOException{
        Selector result = null;
        if(isLinuxPlatform){
            try {
                final Class<?> providerClass = Class.forName("sun.nio.ch.EPollSelectorProvider");
                final Method method = providerClass.getMethod("provider");
                final SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
                if(selectorProvider!=null){
                    result = selectorProvider.openSelector();
                }
            }catch (Exception e){
                log.warn("Open ePoll Selector for linux platform exception", e);
            }
        }
        if(result==null){
            result = Selector.open();
        }
        return result;
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
            log.error("Failed to obtain local address", e);
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

    public static SocketChannel connect(SocketAddress remote) {
        return connect(remote, 1000 * 5);
    }

    public static SocketChannel connect(SocketAddress remote, final int timeoutMillis) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);
            sc.socket().setSoLinger(false, -1);
            sc.socket().setTcpNoDelay(true);
            sc.socket().setReceiveBufferSize(1024 * 64);
            sc.socket().setSendBufferSize(1024 * 64);
            sc.socket().connect(remote, timeoutMillis);
            sc.configureBlocking(false);
            return sc;
        } catch (Exception e) {
            if (sc != null) {
                try {
                    sc.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return null;
    }

    public static void closeChannel(Channel channel) {
        final String addrRemote = parseChannelRemoteAddr(channel);
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                        future.isSuccess());
            }
        });
    }

    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }

    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();

            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }




}
