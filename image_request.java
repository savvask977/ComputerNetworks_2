import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.lang.System.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;


class imageFinal {

  static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static void main(String[] args) {

    final String path = "{path}/code/image.jpg";

    try {
      FileOutputStream image = new FileOutputStream(path);

      //general information about the communication
      int serverPort = 38016;
      int clientPort = 48016;
      String request_code = "M5549CAM=PTZ";
      byte[] txbuffer = request_code.getBytes();
      byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
      InetAddress hostAddress = InetAddress.getByAddress(hostIP);

      //create socket and send the request to the server
      DatagramSocket s = new DatagramSocket();
      DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
      s.send(p);

      //create socket for the response
      byte[] rxbuffer = new byte[128];
      DatagramSocket r = new DatagramSocket(clientPort);
      DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
      r.setSoTimeout(800);

      ArrayList<String> all = new ArrayList<String>();

      for(;;) {
        try {

          r.receive(q);
          byte[] buf = q.getData();
          image.write(buf);
          all.add(bytesToHex(buf));

        } catch (Exception e) {
          System.out.println(e);
          break;
        }
      }

      String sentence = String.join("", all);
      String key1  = "FFD8";
      String key2  = "FFD9";
      int index1 = sentence.indexOf(key1) / 2;
      int index2 = (sentence.indexOf(key2) + 4) / 2;

      String final_image_string = sentence.substring(index1, index2*2);
      System.out.println(final_image_string);

      byte[] byteForImg = hexStringToByteArray(final_image_string);
      image.write(byteForImg);

      s.close();
      r.close();
      image.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
        int v = bytes[j] & 0xFF;
        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }
}