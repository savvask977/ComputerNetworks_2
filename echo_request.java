import java.io.*;
import java.net.*;

class userApplication {

  public static void main(String[] args) {
    //setup
    int serverPort = 38011;
    int clientPort = 48011;
    String packetInfo = "E7550";
    String message;
    byte[] rxbuffer = new byte[2048];
    byte[] txbuffer = packetInfo.getBytes();


    byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };


    try {
      DatagramSocket s = new DatagramSocket();
      InetAddress hostAddress = InetAddress.getByAddress(hostIP);
      DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
      DatagramSocket r = new DatagramSocket(clientPort);

      //the packet I receive
      DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);

      r.setSoTimeout(800);

      long start = System.currentTimeMillis();

      while (System.currentTimeMillis() - start < 240000) {
        try {
          s.send(p);
          r.receive(q);
          message = new String(rxbuffer,0,q.getLength());
          System.out.println(message);

        } catch (Exception x) {
          System.out.println(x);
        }
      }


    } catch (Exception e) {
      System.out.println(e);
    }
  }
}