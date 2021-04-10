import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.System.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.sound.sampled.*;
import java.util.Iterator;
import java.nio.*;
import java.util.Scanner;

public class UserApplication {
    private static final int serverPort = 38013;
    private static final int clientPort = 48013; 
    private static final int copterPort = 48078;
    private static final String Name = "Session1";
    
    private static String echo_code = "E1461";
    private static final String image_code = "M6177";
    private static final String sound_code = "A6927";
    //private static final String copter_code = "Q1000";
    private static final String obd_code = "V0513";  
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String echoreq = "";

    public static void main(String[] args) throws IOException{
              
      try {
        File newSession = new File(Name);
        if(newSession.exists()){
            System.out.println("File created: " + newSession.getName());
        }else{
            newSession.mkdir();
            System.out.println("The file already exists!\n");
        }   
      } catch (Exception e) {
        System.out.println("An error has occured!");
        }
      
      while (true){
          Scanner scan = new Scanner(System.in);
          System.out.println();
          System.out.println("------------------------------------------------------");
          System.out.println("Type the request you want to make to the Ithaki server: "); 
          System.out.println("1) Type echo to make an echo request.");
          System.out.println("2) Type image to make an image request.");
          System.out.println("3) Type dpcm to make an audio request with DPCM.");
          System.out.println("4) Type aqdpcm to make an audio request with AQDPCM");
          System.out.println("5) Type ithakicopter to get copter telemetry.");
          System.out.println("6) Type obd to make an OBD request.");
          System.out.println();
          String user_input = scan.nextLine();
        
          if("echo".equalsIgnoreCase(user_input)){
            System.out.println("Do you want to remove the server delay? Please type yes / no.");
            String delay = scan.nextLine();
            if("yes".equalsIgnoreCase(delay)){
              echo_code = "E000";
            }
            System.out.println("Please type \"yes\" if you want the Temperature too." );
            String answer = scan.nextLine();
            if("yes".equalsIgnoreCase(answer)){
              echoreq = echo_code + "T00";
            }else{
              echoreq = echo_code; 
            }
            EchoRequest();
          }else if("image".equalsIgnoreCase(user_input)){
            ImageRequest();
          }else if("dpcm".equalsIgnoreCase(user_input)){
            Sound_DPCM();
          }else if("aqdpcm".equalsIgnoreCase(user_input)){
            Sound_AQDPCM();
          }else if("ithakicopter".equalsIgnoreCase(user_input)){
            IthakiCopter();
          }else if("obd".equalsIgnoreCase(user_input)){
            OBD();
          }else{
              System.out.println("Input not valid. Please choose one of the available options!");
          }
          //scan.close();
      }
    }


    public static void EchoRequest() throws IOException{
        File echo_out = new File(Name + "/echo_out.txt");
        File time_out = new File(Name + "/time_out.csv");
        FileOutputStream echo = new FileOutputStream(echo_out);
        FileOutputStream time = new FileOutputStream(time_out);
        byte[] rxbuffer = new byte[2048];
        byte[] txbuffer = echoreq.getBytes();
        String message;
        byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        ArrayList<String> MessageInfo = new ArrayList<String>();
        long startTime;
        String elapsed = "";

        try {
            DatagramSocket s = new DatagramSocket();
            InetAddress hostAddress = InetAddress.getByAddress(hostIP);
            DatagramPacket p = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);
            DatagramSocket r = new DatagramSocket(clientPort);
            DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
            r.setSoTimeout(8000);
            long start = System.currentTimeMillis();
      
            while (System.currentTimeMillis() - start < 24000) {
              try {
                s.send(p);
                startTime = System.currentTimeMillis();
                r.receive(q);
                elapsed = elapsed + (System.currentTimeMillis() - startTime) + ",";
                message = new String(rxbuffer,0,q.getLength());
                MessageInfo.add(message);
                System.out.println(message);
              } catch (Exception x) {
                System.out.println(x);
              }
            }
            try{
                for(String mes : MessageInfo){
                    echo.write(mes.getBytes());
                }
                echo.close();
                time.write(elapsed.getBytes());
                time.close();
            }catch(IOException l){
                System.out.println("The output could not be saved.");
            } 
            r.close();  
            s.close();  
        }catch (Exception e) {
          System.out.println(e);
        }
    }


    public static void ImageRequest() throws IOException{

      Scanner pickCam = new Scanner(System.in);
      System.out.println("Please choose the camera you want from the available below:");
      System.out.println("1. Type FIX if you want the camera with the fixed position.");
      System.out.println("2. Type PTZ if you want the camera that can change its position.");
      System.out.println();
      String camera = pickCam.nextLine();
      String cam = "CAM=";
      FileOutputStream image = null;
      pickCam.close();

      if("FIX".equalsIgnoreCase(camera)){
        File Fix_image = new File(Name + "/fix_image.jpeg");
        image = new FileOutputStream(Fix_image);
        cam = cam + "FIX";
      }else if("PTZ".equalsIgnoreCase(camera)){
        File PTZ_image = new File(Name + "/ptz_image.jpeg");
        image = new FileOutputStream(PTZ_image);
        cam = cam + "PTZ" + "DIR=L";
      }else{
        System.out.println("Input is not valid!");
      }

      DatagramSocket s = new DatagramSocket();
      DatagramSocket r = new DatagramSocket(clientPort);
      try{
        String request_code = image_code + cam;
        byte[] txbuffer = request_code.getBytes();
        byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
        s.send(p);
        byte[] rxbuffer = new byte[128];
        DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
        r.setSoTimeout(10000);
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
        image.close();
        r.close();
        s.close();
      } catch(Exception e) {
          System.out.println(e);
      }
    }


    public static void Sound_DPCM() throws IOException{

      Scanner SoundType = new Scanner(System.in);
      System.out.println("Please specify what you want to receive!");
      System.out.println("To receive audio clips please type audio.");
      System.out.println("To receive frequencies please type frequencies.");
      System.out.println();
      String answer = SoundType.nextLine();
      SoundType.close();
      String type = "";
      FileOutputStream samples = null;
      FileOutputStream diffs = null;
      if("audio".equalsIgnoreCase(answer)){
        File audio_samples = new File(Name + "/audio_samples.csv");
        File audio_diffs = new File(Name + "/audio_diffs.csv");
        samples = new FileOutputStream(audio_samples);
        diffs = new FileOutputStream(audio_diffs);
        type = "L17F999";
      }else if("frequencies".equalsIgnoreCase(answer)){
        File generator_samples = new File(Name + "/generator_samples.csv");
        File generator_diffs= new File(Name + "/generator_diffs.csv");
        samples = new FileOutputStream(generator_samples);
        diffs = new FileOutputStream(generator_diffs);
        type = "L17T999";
      }else{
        System.out.println("Invalid input!");
      }

      try {
        byte[][] udp_packets = new byte[999][128];
        byte[][] final_packets = new byte[999][256];
        String packetInfo = sound_code + type;
        byte[] rxbuffer = new byte[2048];
        byte[] txbuffer = packetInfo.getBytes();
        byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        DatagramSocket s = new DatagramSocket();
        DatagramSocket r = new DatagramSocket(clientPort);
        DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
        DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
        s.send(p);
        r.setSoTimeout(8000);

        ArrayList <Integer> Temp = new ArrayList<>();
        int lowNibble,highNibble;
        int di1, di2;
        String aud_diff = "";
        int total, mv;

        for(int i = 0; i < 999; i++){
          try{
            r.receive(q);
            udp_packets[i] = q.getData().clone();
            total = 0;
            di2 = 0; 
            int len = q.getLength();
            for(int j = 0; j < len; j++){
              highNibble = ((udp_packets[i][j] >> 4) & 0x0f) - 8;
              lowNibble = (udp_packets[i][j] & 0x0f) - 8 ;
              aud_diff += highNibble + ",";
              aud_diff += lowNibble + ",";
              di1 = highNibble + di2;
              di2 = lowNibble + di1;
              Temp.add(di1);
              Temp.add(di2);
              total = total + di1 + di2;
            }
            // each packet is 128 bytes and it contains 256 samples as 128 differencies.
            mv = total / (2 * len); 
            for(int k = 0; k < 256; k++){
              final_packets[i][k] = (byte)(Temp.get(k) - mv);
            }
            Temp.clear();
          }catch(Exception e){
            System.out.println(e);
          }
        }

        byte[] final_aud = new byte[255744];
        int y = 0;
        for(int i = 0; i < 999; i++) {
          for (int j = 0; j < 256; j++) {
            final_aud[y] = final_packets[i][j];
            y++;
          }
        }

        AudioFormat linearPCM = new AudioFormat(8000,8,1,true,false);
        SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
        lineOut.open(linearPCM,32000);
        lineOut.start();
        lineOut.write(final_aud, 0,255744); // 255744 = 256 * 999
        lineOut.stop();
        lineOut.close();
        r.close();
        s.close();

        try{
          samples.write(final_aud);
          samples.close();
          diffs.write(aud_diff.getBytes());
          diffs.close();
        }catch (IOException io){
          System.out.println("Results could not be saved.");       
        }
      }catch (Exception e) {
        System.out.println(e);
      }
    }


    public static void Sound_AQDPCM() throws IOException{
      
      File audio_samples = new File(Name + "/AQaudio_samples.csv");
      File audio_diffs = new File(Name + "/AQaudio_diffs.csv");   
      File mval = new File(Name + "/mv.csv");
      File b = new File(Name + "/quan_step.csv");
      FileOutputStream samples = new FileOutputStream(audio_samples);
      FileOutputStream diffs = new FileOutputStream(audio_diffs);
      FileOutputStream mean_val = new FileOutputStream(mval);
      FileOutputStream quan_step = new FileOutputStream(b);

      try {
        String packetInfo = sound_code + "L21AQF999";
        byte[] rxbuffer = new byte[2048];
        byte[] txbuffer = packetInfo.getBytes();
        byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        DatagramSocket s = new DatagramSocket();
        DatagramSocket r = new DatagramSocket(clientPort);
        DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
        s.send(p);
        r.setSoTimeout(8000);
        ArrayList <Byte> Sam = new ArrayList<>();
        ArrayList <Byte> Temp = new ArrayList<>();
        byte[] packet_header = new byte[4];
        int lowNibble, highNibble, di1, di2, sample1_high, sample1_low, sample2_high, sample2_low, mv, b_step, temporary, len;
        byte samp_header;
        String aud_diffs = "", mean_Value = "", quan = "";
        int packets = 0;
        byte[] info;
        // String S = "";

        for(int i = 0; i < 999; i++){
          DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
          try{
            temporary = 0;
            r.receive(q);
            di2 = 0;
            len = q.getLength();
            info = q.getData();

            // find mean value and the quantization step:
            samp_header = (byte)( ( info[1] & 0b10000000) !=0 ? 0xFF : 0x00);
            packet_header[0] = info[0];
            packet_header[1] = info[1];
            packet_header[2] = samp_header;
            packet_header[3] = samp_header;
            mv = ByteBuffer.wrap(packet_header).order(ByteOrder.LITTLE_ENDIAN).getInt();
            mean_Value += mv + ",";
            samp_header = (byte)( ( info[3] & 0b10000000) !=0 ? 0xFF : 0x00);
            packet_header[0] = info[2];
            packet_header[1] = info[3];
            packet_header[2] = samp_header;
            packet_header[3] = samp_header;
            b_step = ByteBuffer.wrap(packet_header).order(ByteOrder.LITTLE_ENDIAN).getInt();
            quan += b_step + ",";

            for(int j = 4; j < len; j++){
              highNibble = ((info[j] & 0x000000F0) >>4) - 8;
              lowNibble = (info[j] & 0x0000000F) - 8 ;
              aud_diffs += lowNibble + ",";
              aud_diffs += highNibble + ",";
              di1 = (highNibble * b_step) + temporary + mv;
              di2 = (lowNibble * b_step) + (highNibble * b_step) + mv;
              temporary = lowNibble * b_step;
              sample1_high = ((di1 & 0x0000FF00) >> 8);
              sample1_low = di1 & 0x000000FF;
              sample2_high = ((di2 & 0x0000FF00) >> 8);
              sample2_low = di2 & 0x000000FF;
              Temp.add((byte) sample1_low);Temp.add((byte) sample1_high);
              Temp.add((byte) sample2_low);Temp.add((byte) sample2_high);
            }
      
            for(int k = 0; k < Temp.size(); k++){
              Sam.add(Temp.get(k));
              // S += Temp.get(k) + ",";
            }
            Temp.clear();
            packets++;
          }catch(Exception e){
            System.out.println(e);
          }
      
        }
            
        AudioFormat AQPCM = new AudioFormat(8000,16,1,true,false);
        SourceDataLine lineOut = AudioSystem.getSourceDataLine(AQPCM);
        lineOut.open(AQPCM,32000);
        lineOut.start();
        lineOut.write(makeBytes(Sam), 0,256 * 2 * packets); // 255744 = 256 * 999
        lineOut.stop();
        lineOut.close();
  
        try{
          //samples.write(S.getBytes()); ///////// check if this is correct!
          samples.close();
          diffs.write(aud_diffs.getBytes());
          diffs.close();
          mean_val.write(mean_Value.getBytes());
          mean_val.close();
          quan_step.write(quan.getBytes());
          quan_step.close();
        }catch(IOException io){
          System.out.println("Results could not be saved.");       
        }
        r.close();
        s.close();
      }catch (Exception e) {
        System.out.println(e);
      }
    }


    /* Ithakicopter() is using the UDP protocol, so in order to get the telemetry, the user 
    must be running the ithakicopter.jar file at the same time. An interface will open from
    which they can choose flight level etc. 
    */
    public static void IthakiCopter() throws IOException{

      File IthakiCopterInfo = new File(Name + "/IthakiCopterInfo.csv");
      FileOutputStream copter_info = new FileOutputStream(IthakiCopterInfo);
      String info = "";

      DatagramSocket s = new DatagramSocket();
      DatagramSocket r = new DatagramSocket(copterPort);
      try {
        //byte[] udp_packets = new byte[128];
        String message;
        //String packetInfo = "Q9649";
        byte[] rxbuffer = new byte[129];
        //byte[] txbuffer = packetInfo.getBytes();
        //byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        //InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        //DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
        DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
        //s.send(p);
        r.setSoTimeout(8000);
      
        for (;;) {
          try {
            r.receive(q);
            message = new String(rxbuffer,0,q.getLength());
            System.out.println('\n');
            System.out.println(message);
            info += message + "\n";
          }catch (Exception x) {
            System.out.println(x);
          }
        }
      } catch (Exception e) {
          System.out.println(e);
      }
      r.close();
      s.close();
      copter_info.write(info.getBytes());
      copter_info.close();
    }


    public static void OBD() throws IOException{
      
      File v_diagnostics = null;
      try {
        String message;
        String opcode = "";
        int operation = 0;
        Scanner odb_operation = new Scanner( System.in );
        System.out.println();
        System.out.println("What operation do you want to check? ");
        System.out.println("Type engine for Engine run time.");
        System.out.println("Type temp for Intake air temperature.");
        System.out.println("Type throttle for Throttle position.");
        System.out.println("Type engine_rpm for Engine RPM.");
        System.out.println("Type speed for Vehicle speed.");
        System.out.println("Type coolant_temp for Coolant temperature.");
        System.out.println();
        String input = odb_operation.nextLine();
        odb_operation.close();

        if("engine".equalsIgnoreCase(input)){
          operation = 1;
          opcode = "01 1F";
          v_diagnostics = new File(Name + "/engine.csv");
        }else if ("temp".equalsIgnoreCase(input)){
          operation = 2;
          opcode = "01 0F";
          v_diagnostics = new File(Name + "/temp.csv");
        }else if ("throttle".equalsIgnoreCase(input)){
          operation = 3;
          opcode = "01 11";
          v_diagnostics = new File(Name + "/throttle.csv");
        }else if ("engine_rpm".equalsIgnoreCase(input)){
          operation = 4;
          opcode = "01 0C";
          v_diagnostics = new File(Name + "/engine_rpm.csv");
        }else if ("speed".equalsIgnoreCase(input)){
          operation = 5;
          opcode = "01 0D";
          v_diagnostics = new File(Name + "/speed.csv");
        }else if ("coolant_temp".equalsIgnoreCase(input)){
          operation = 6;
          opcode = "01 05";
          v_diagnostics = new File(Name + "/coolant_temp.csv");
        }else{
          System.out.print("Wrong input");
        }
        FileOutputStream obd_information = new FileOutputStream(v_diagnostics);
        byte[] rxbuffer = new byte[2048];
        String packetInfo = obd_code + "OBD=" + opcode;
        byte[] txbuffer = packetInfo.getBytes();
  
        byte[] hostIP = { (byte)155,(byte)207,18,(byte)208 };
        InetAddress hostAddress = InetAddress.getByAddress(hostIP);
        DatagramSocket s = new DatagramSocket();
        DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length,hostAddress,serverPort);
        DatagramSocket r = new DatagramSocket(clientPort);
        r.setSoTimeout(8000);
        String highNibble, lowNibble;
        String results = "";
        long start= System.currentTimeMillis();
        int XX = 0, YY = 0;
        ArrayList<Double> obd_info = new ArrayList<>();
        while (System.currentTimeMillis()- start < 24000){
          s.send(p);
          DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
          try{
            r.receive(q);
            message = new String(rxbuffer,0,q.getLength());
            System.out.println(message);
            int Datalen = q.getLength();
            byte[] data = q.getData();
            if (Datalen == 11) {                   
              highNibble = "" + (char) data[6] + (char) data[7];
              lowNibble = "" + (char) data[9] + (char) data[10];
              XX = Integer.parseInt(highNibble, 16);
              YY = Integer.parseInt(lowNibble, 16);
            }else if (Datalen == 8){              
              highNibble = "" + (char) data[6] + (char) data[7];
              XX = Integer.parseInt(highNibble, 16);
            }else {
              System.err.println("An unexpected error has occured!");
            }
            switch (operation){
              case 1:
                  obd_info.add((double) (256 * XX + YY));
                  break;
              case 2:
                  obd_info.add((double) (XX - 40));
                  break;
              case 3:
                  obd_info.add((double) (XX * 100 / 255));
                  break;
              case 4:
                  obd_info.add((double) (((XX * 256) + YY) / 4));
                  break;
              case 5:
                  obd_info.add((double) XX);
                  break;
              case 6:
                  obd_info.add((double) (XX - 40));
                  break;
            }
          }catch(Exception x){
            System.out.println(x);
          }
        }

        for (Double info : obd_info){
          results = results + info + ",";
        } 
        try {
          obd_information.write(results.getBytes());
          obd_information.close();
        } catch (Exception a) {
          System.out.println("Error while saving results!");
        }
        r.close();
        s.close();
      } catch (Exception e) {
        System.out.println(e);
      }
    }


    private static byte[] makeBytes(ArrayList<Byte> bytes) {
      byte[] a = new byte[bytes.size()];
      Iterator<Byte> iterator = bytes.iterator();
      for (int i = 0; i < a.length; i++) {
        a[i] = iterator.next().byteValue();
      }
      return a;
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

