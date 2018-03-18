import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.lcd.Font;
import lejos.hardware.lcd.GraphicsLCD;

public class RoboSpy {

	private final static int PORT = 80;
	//private final static String IP = "192.168.1.104";
	
	private static GraphicsLCD gLCD; 
	private static DataOutputStream dout;
	private static RoboSpyEngine rse;
	private static Sender sender;

    public static void main(String[] args) throws Exception {

    	// set LCD font
    	gLCD = BrickFinder.getDefault().getGraphicsLCD();
    	gLCD.setFont(Font.getSmallFont());
    	
    	
    	// start engine
    	rse = new RoboSpyEngine();
    	
        Thread thread = new Thread("New Thread") {
            public void run(){
            	try {
	            	writeLCD("Starting network process");   	
	            	
	                ServerSocket server = new ServerSocket(80);

	                String IP = server.getInetAddress().getHostAddress(); // not working
	                writeLCD("Server has started on\n"+IP+":"+PORT+".\nWaiting for a connection...");
	
	                Socket client = server.accept();
	
	                writeLCD("A client connected.");
	                
	                InputStream in = client.getInputStream();
	                OutputStream out = client.getOutputStream();
	                        
	                String data = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();
	            	Matcher get = Pattern.compile("^GET").matcher(data);        
	                if (get.find()) {
	        	        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
	        	        match.find();
	        	        byte[] response;
						
							response = ("HTTP/1.1 101 Switching Protocols\r\n"
							        + "Connection: Upgrade\r\n"
							        + "Upgrade: websocket\r\n"
							        + "Sec-WebSocket-Accept: "
							        + DatatypeConverter
							        .printBase64Binary(
							                MessageDigest
							                .getInstance("SHA-1")
							                .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
							                        .getBytes("UTF-8")))
							        + "\r\n\r\n")
							        .getBytes("UTF-8");						
	        	
	        	        out.write(response, 0, response.length);
	        	        
	        	    } else {}
	        	    
	                dout = new DataOutputStream(client.getOutputStream());
	                sender = new Sender(dout);
	                rse.setSender(sender);	                
	                sendClient("welcome by EV3");
	                rse.radarStart();
	            	
	                int buffersize = client.getReceiveBufferSize();
	        	    byte[] b = new byte[buffersize];
	        	    int len;
	        	    
	        	    while(true) {
	        	    	len = in.read(b);
	                    if(len!=-1){
	
	                        byte rLength = 0;
	                        int rMaskIndex = 2;
	                        int rDataStart = 0;
	                        byte data2 = b[1];
	                        byte op = (byte) 127;
	                        rLength = (byte) (data2 & op);
	                        if(rLength==(byte)126) rMaskIndex=4;
	                        if(rLength==(byte)127) rMaskIndex=10;
	                        byte[] masks = new byte[4];
	                        int j=0;
	                        int i=0;
	                        for(i=rMaskIndex;i<(rMaskIndex+4);i++){
	                            masks[j] = b[i];
	                            j++;
	                        }
	                        rDataStart = rMaskIndex + 4;
	                        int messLen = len - rDataStart;
	                        byte[] message = new byte[messLen];
	                        for(i=rDataStart, j=0; i<len; i++, j++){
	                            message[j] = (byte) (b[i] ^ masks[j % 4]);
	                        }
	
	                        String msg = new String(message);
	                        writeLCD(msg); 
	                        rse.dispatchMsg(msg);
	                        
	                        // read next message
	                        b = new byte[buffersize];
	                        
	                    }	
	        	    }
            	} catch (NoSuchAlgorithmException | IOException e) {
					e.printStackTrace();
				}	
            };
         };
         thread.start();
               	
	    	
	    // video broadcasting
        if(Button.ESCAPE.isUp()) {
	        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	        Mat mat = new Mat();
	        VideoCapture vid = new VideoCapture(0);
	        vid.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, 160);
	        vid.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, 120);
	        vid.open(0);
	           
	        ServerSocket ss = new ServerSocket(8080);
	        Socket sock = ss.accept();
	        String boundary = "camera ON";
	        writeHeader(sock.getOutputStream(), boundary);
	         
	        long stime = System.currentTimeMillis();
	        int cnt = 0;
	        while (Button.ESCAPE.isUp()) {
	            vid.read(mat);
	            if (!mat.empty()) {
	                writeJpg(sock.getOutputStream(), mat, boundary);                
	                if (cnt++ >= 100)
	                {
	                    long stop = System.currentTimeMillis();
	                    //System.out.println("Frame rate: " + (cnt*1000/(stop - stime)));
	                    cnt = 0;
	                    stime = stop;
	                }
	                
	            } else  System.out.println("No picture");
	        }
	        sock.close();
	        ss.close();
	        System.exit(0);
        }
    }
     
    private static void writeHeader(OutputStream stream, String boundary) throws IOException {
        stream.write(("HTTP/1.0 200 OK\r\n" +
                "Connection: close\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                "Pragma: no-cache\r\n" + 
                "Content-Type: multipart/x-mixed-replace; " +
                "boundary=" + boundary + "\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n").getBytes());
    }
      
    private static void writeJpg(OutputStream stream, Mat img, String boundary) throws IOException {
        MatOfByte buf = new MatOfByte();
        Highgui.imencode(".jpg", img, buf);
        byte[] imageBytes = buf.toArray();
        stream.write(("Content-type: image/jpeg\r\n" +
                "Content-Length: " + imageBytes.length + "\r\n" +
                "\r\n").getBytes());
        stream.write(imageBytes);
        stream.write(("\r\n--" + boundary + "\r\n").getBytes());
    }
    
    
    private static void writeLCD(String msg) {
    	gLCD.clear();
    	gLCD.refresh();
    	String[] msgRows = msg.split("\n");
    	int r = 5;
    	for(int i=0; i<msgRows.length; i++) {
    		gLCD.drawString(msgRows[i], 0, r, GraphicsLCD.LEFT|GraphicsLCD.TOP);
    		r += 10;
    	}
    }
    
    private static void sendClient(String msg) {
    	sender.sendClient(msg);
    }
	
}
