package com.esri;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * mvn exec:java -Dexec.mainClass=com.esri.HZServer -Dexec.args="stop"
 */
public class HZServer implements MessageListener<String> {

    private HazelcastInstance m_hazelcastInstance;
    private InetAddress m_inetAddress;

    public void doMain(final String[] args) throws UnknownHostException, SocketException {
        final Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            final Enumeration ee = ((NetworkInterface) e.nextElement()).getInetAddresses();
            while (ee.hasMoreElements()) {
                final InetAddress inetAddress = (InetAddress) ee.nextElement();
                if (inetAddress.isLinkLocalAddress()) {
                    continue;
                }
                if (inetAddress.isLoopbackAddress()) {
                    continue;
                }
                m_inetAddress = inetAddress;
            }
        }
        if (args.length > 0) {
            final HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient();
            try {
                final String hostAddress = m_inetAddress == null ? "0.0.0.0" : m_inetAddress.getHostAddress();
                hazelcastInstance.<String>getTopic(args[0]).publish(hostAddress);
            } finally {
                hazelcastInstance.shutdown();
            }
        } else if (args.length == 0) {
            final Config config = new ClasspathXmlConfig("hazelcast.xml");
            m_hazelcastInstance = Hazelcast.newHazelcastInstance(config);
            m_hazelcastInstance.<String>getTopic("stop").addMessageListener(this);
            m_hazelcastInstance.<String>getTopic("shutdown").addMessageListener(new MessageListener<String>() {
                @Override
                public void onMessage(final Message<String> message) {
                    m_hazelcastInstance.shutdown();
                }
            });
        }
    }

    public static void main(final String[] args) throws IOException {
        final HZServer hzServer = new HZServer();
        hzServer.doMain(args);
    }

    @Override
    public void onMessage(final Message<String> message) {
        if (m_inetAddress.getHostAddress().equals(message.getMessageObject())) {
            m_hazelcastInstance.shutdown();
        }
    }
}
