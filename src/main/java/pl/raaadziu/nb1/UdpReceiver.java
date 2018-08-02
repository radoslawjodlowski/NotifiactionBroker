package pl.raaadziu.nb1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

@Component
public class UdpReceiver extends Thread
{
    private MemBox memBox;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] buffer = new byte[4096];

    private static Logger log = LoggerFactory.getLogger("UDP");

    @Autowired
    UdpReceiver(MemBox memBox, Configuration configuration) throws SocketException
    {
        this.memBox = memBox;
        int port = configuration.getUdpPort();
        socket = new DatagramSocket(port);
        packet = new DatagramPacket(buffer, buffer.length);
        log.info("udp receiver port init ... ok");
        this.start();
    }

    @Override
    public void run()
    {
        while (true)
            receiving();
    }

    private void receiving()
    {
        log.info("udp receiver thread init ... ok");
        try {
            while (true)
            {
                // Wait to receive a datagram
                socket.receive(packet);
                String msg = new String(buffer, 0, packet.getLength());
                Notification notification = new Notification(msg);
                memBox.broadcastFromUtp(notification);
                // Reset the length of the packet before reusing it.
                packet.setLength(buffer.length);
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
