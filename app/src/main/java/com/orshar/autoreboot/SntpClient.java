package com.orshar.autoreboot;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SntpClient {
    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_VERSION = 3;
    private static final int NTP_PACKET_SIZE = 48;
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    private long ntpTime;

    public boolean requestTime(String host, int timeoutMs) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs);
            InetAddress address = InetAddress.getByName(host);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);
            socket.send(request);

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            long seconds = read32(buffer, 40);
            ntpTime = (seconds - OFFSET_1900_TO_1970) * 1000L;
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) socket.close();
        }
    }

    private long read32(byte[] buf, int offset) {
        long b0 = buf[offset] & 0xFF;
        long b1 = buf[offset + 1] & 0xFF;
        long b2 = buf[offset + 2] & 0xFF;
        long b3 = buf[offset + 3] & 0xFF;
        return (b0 << 24) + (b1 << 16) + (b2 << 8) + b3;
    }

    public long getNtpTime() { return ntpTime; }
}