import java.io.*;
import java.net.*;

class eMail {

    public static void main(String args[]) {

        if(args.length != 5) {
            System.out.print("usage: java EMail2 <smtp-host> <fromName> <toAddress>");
            System.out.println(" <subject> <body>");
            System.exit(-1);
        }

        try {
            send(args[0], args[1], args[2], args[3], args[4]);
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    public static void send(String host, String from, String to, String subject, String message) {
        try {
            System.setProperty("mail.host", host);

            URL url = new URL("mailto:" + to);
            URLConnection conn = url.openConnection();
            conn.connect();

            PrintWriter out = new PrintWriter(new OutputStreamWriter(conn.getOutputStream() ) );

            out.println("From: \"" + from + "\" <" + from + ">");
            out.println("To: " + to);
            out.println("Subject: " + subject);
            out.println(); 
            out.println(message);
            out.close();
            
        } catch(Exception err) {
            System.err.println(err);
        }
        
    }
    
}
