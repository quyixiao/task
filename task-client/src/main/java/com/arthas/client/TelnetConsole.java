package com.arthas.client;

import jline.console.ConsoleReader;
import jline.console.KeyMap;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * @author ralf0131 2016-12-29 11:55.
 * @author hengyunabc 2018-11-01
 */

public class TelnetConsole {


    private TelnetClient telnet = new TelnetClient();
    private static InputStream in;
    private static PrintStream out;
    private static char prompt = '$';

    private static final String PROMPT = "$";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5000 ms
    private static final int DEFAULT_BUFFER_SIZE = 1024;


    private static final byte CTRL_C = 0x03;


    private Integer width = null;
    private Integer height = null;
    public static final int STATUS_OK = 0;

    public static final int DEFAULT_WIDTH = 80;

    public static final int DEFAULT_HEIGHT = 24;


    public static void main(String[] args) {
        try {
            int status = process(args);
            System.exit(status);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static int process(String[] args) {
        try {
            final TelnetClient telnet = new TelnetClient();
            telnet.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
            int width = DEFAULT_WIDTH;
            int height = DEFAULT_HEIGHT;
            final ConsoleReader consoleReader = new ConsoleReader(System.in, System.out);
            // send init terminal size
            TelnetOptionHandler sizeOpt = new WindowSizeOptionHandler(width, height, true, true, false, false);
            try {
                telnet.addOptionHandler(sizeOpt);
            } catch (InvalidTelnetOptionException e) {
                // ignore
            }

            // ctrl + c event callback
            consoleReader.getKeys().bind(new Character((char) CTRL_C).toString(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        consoleReader.getCursorBuffer().clear(); // clear current line
                        telnet.getOutputStream().write(CTRL_C);
                        telnet.getOutputStream().flush();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

            });

            // ctrl + d event call back
            consoleReader.getKeys().bind(new Character(KeyMap.CTRL_D).toString(), new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            String targetIp = "127.0.0.1";
            int port = 3658;
            try {
                telnet.connect(targetIp, port);
                in = telnet.getInputStream();
                out = new PrintStream(telnet.getOutputStream());
            } catch (IOException e) {
                System.out.println("Connect to telnet server error: " + targetIp + " " + 22);
                throw e;
            }
            IOUtil.readWrite(telnet.getInputStream(), telnet.getOutputStream(), System.in,
                    consoleReader.getOutput());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

}
