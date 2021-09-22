package org.ifsp.domain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.StringTokenizer;

public class Server extends JFrame implements ActionListener {

    private DatagramSocket rtpSocket;
    private DatagramPacket videoFrame;

    private InetAddress clientIPAddr;
    private int rtpDestinationPort = 0;

    private JLabel label;

    private int imageNb = 0;
    private VideoStream video;
    private static int MJPEG_TYPE = 26;
    private static int FRAME_PERIOD = 100;
    private static int VIDEO_LENGTH = 500;
    private Timer timer;
    private byte[] buffer;

    private static final int INIT = 0;
    private static final int READY = 1;
    private static final int PLAYING = 2;
    private static final int SETUP = 3;
    private static final int PLAY = 4;
    private static final int PAUSE = 5;
    private static final int TEARDOWN = 6;

    private static int state;
    private Socket rtspSocket;
    private static BufferedReader rtspBufferedReader;
    private static BufferedWriter rtspBufferedWriter;
    private static String videoFileName;
    private final static int RTSP_ID = 123456;
    private int rtspSeqNb = 0;

    private final static String CRLF = "\r\n";

    public Server() {

        super("Server");

        timer = new Timer(FRAME_PERIOD, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        buffer = new byte[15000];

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                timer.stop();
                System.exit(0);
            }
        });

        label = new JLabel("Send frame #        #", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

    }

    public static void main(String argv[]) throws Exception {
        Server theServer = new Server();

        theServer.pack();
        theServer.setVisible(true);

        int rtspPort = Integer.parseInt(argv[0]);

        ServerSocket listenSocket = new ServerSocket(rtspPort);
        theServer.rtspSocket = listenSocket.accept();

        listenSocket.close();

        theServer.clientIPAddr = theServer.rtspSocket.getInetAddress();

        state = INIT;

        rtspBufferedReader = new BufferedReader(new InputStreamReader(theServer.rtspSocket.getInputStream()));
        rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.rtspSocket.getOutputStream()));

        int requestType;
        boolean done = false;

        while (!done) {
            requestType = theServer.parseRtspRequest();

            if (requestType == SETUP) {
                done = true;

                state = READY;
                System.out.println("New RTSP state: READY");

                theServer.sendRtspResponse();

                theServer.video = new VideoStream(videoFileName);

                theServer.rtpSocket = new DatagramSocket();
            }
        }

        //loop to handle RTSP requests
        while (true) {
            requestType = theServer.parseRtspRequest();

            if ((requestType == PLAY) && (state == READY)) {
                theServer.sendRtspResponse();
                theServer.timer.start();
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");
            } else if ((requestType == PAUSE) && (state == PLAYING)) {
                theServer.sendRtspResponse();
                theServer.timer.stop();
                state = READY;
                System.out.println("New RTSP state: READY");
            } else if (requestType == TEARDOWN) {
                theServer.sendRtspResponse();
                theServer.timer.stop();
                //close sockets
                theServer.rtspSocket.close();
                theServer.rtpSocket.close();
                System.exit(0);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {

        if (imageNb < VIDEO_LENGTH) {
            imageNb++;

            try {
                int imageLength = video.getNextFrame(buffer);

                RTPPacket rtpPacket = new RTPPacket(MJPEG_TYPE, imageNb, imageNb * FRAME_PERIOD, buffer, imageLength);

                int packetLength = rtpPacket.getLength();

                byte[] packetBits = new byte[packetLength];
                rtpPacket.getPacket(packetBits);

                videoFrame = new DatagramPacket(packetBits, packetLength, clientIPAddr, rtpDestinationPort);
                rtpSocket.send(videoFrame);

                //System.out.println("Send frame #"+imagenb);
                rtpPacket.printHeader();

                label.setText("Send frame #" + imageNb + "#");
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        } else {
            timer.stop();
        }
    }

    //Parse RTSP Request
    private int parseRtspRequest() {
        int requestType = -1;

        try {
            String requestLine = rtspBufferedReader.readLine();
            // System.out.println("RTSP Server - Received from Client:");
            // System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(requestLine);
            String requestTypeString = tokens.nextToken();

            //convert to request_type structure:
            if (new String(requestTypeString).compareTo("SETUP") == 0)
                requestType = SETUP;
            else if (new String(requestTypeString).compareTo("PLAY") == 0)
                requestType = PLAY;
            else if (new String(requestTypeString).compareTo("PAUSE") == 0)
                requestType = PAUSE;
            else if (new String(requestTypeString).compareTo("TEARDOWN") == 0)
                requestType = TEARDOWN;

            if (requestType == SETUP) {
                videoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String seqNumLine = rtspBufferedReader.readLine();
            // System.out.println(SeqNumLine);
            tokens = new StringTokenizer(seqNumLine);
            tokens.nextToken();
            rtspSeqNb = Integer.parseInt(tokens.nextToken());

            String lastLine = rtspBufferedReader.readLine();
            // System.out.println(LastLine);

            if (requestType == SETUP) {
                tokens = new StringTokenizer(lastLine);
                for (int i = 0; i < 3; i++)
                    tokens.nextToken(); //skip unused stuff
                rtpDestinationPort = Integer.parseInt(tokens.nextToken());
            }
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
        return (requestType);
    }

    //Send RTSP Response
    private void sendRtspResponse() {
        try {
            rtspBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSeqNb + CRLF);
            rtspBufferedWriter.write("Session: " + RTSP_ID + CRLF);
            rtspBufferedWriter.flush();
            // System.out.println("RTSP Server - Sent response to Client.");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}
