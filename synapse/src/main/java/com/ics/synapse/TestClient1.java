package com.ics.synapse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TestClient1 {
	
    public static void main(String[] args) throws IOException {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 1001));

            ByteBuffer readBuffer = ByteBuffer.allocate(1024*32);

            while (true) {
                readBuffer.clear();
                socketChannel.read(readBuffer);
                System.out.println(new String(readBuffer.array()));
            }
        } catch (IOException e) {
        }
	}

}
