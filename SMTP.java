import java.io.*;
import java.net.*;
import java.*;
import javax.swing.*;

import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.Properties;

public class SMTP extends Thread implements Runnable {
    
    public SmtpServerThread clients[];
    public ServerSocket server = null;
    public Thread thread = null; 
    public int clientCount = 0;
    public int port; 
    private GUI logger;
    public Properties prop = new Properties(); 
      
    public SMTP(GUI log) {
        clients = new SmtpServerThread[50];
        port = new Integer(getProp("port"));
        logger = log;
    	  try{  
    	      server = new ServerSocket(port);
    	      start(); 
        } catch(IOException ioe) { }
    }
    
    public void run() {  
  	    while (thread != null){  
            try {   
                addThread(server.accept()); 
            } catch(Exception ioe){ }
        }
      	if (thread == null){  
            thread = new Thread(this); 
  	        thread.start();
  	    }
    }		
    
    public String getProp(String p) {
    
        InputStream input = null;    
        try {    
            input = new FileInputStream("smtp.ini");
            prop.load(input);    
            return prop.getProperty(p);    
        } catch (IOException ex) {} 
        finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {}
            }
        }    
        return "";   
    }
    
    public void stopen() {
        try {
            this.server.close();
        } catch(IOException ioe) {}
    }
    
    private void addThread(Socket socket) {  
      	if (clientCount < clients.length) {  
      	    clients[clientCount] = new SmtpServerThread(this, socket);
      	    try{ 
      	      	clients[clientCount].open(); 
      	        clients[clientCount].start();  
      	        clientCount++; 
      	    } catch(IOException ioe) { } 
      	} else { }
    }
    
    public synchronized void handle(int ID, String line) {
    
        if(Boolean.parseBoolean(getProp("debug"))) {
            logger.log("<SMTP> [C] <- " + line + "\n");
        }
    
        String actual_state = getSession(ID, "state"); 
        String message_state = getSession(ID, "message_state");
        
        String relay = getSession(ID, "relay");
        String from = getSession(ID, "from");
        String to = getSession(ID, "to");                   
        String message = getSession(ID, "message");  
      
        switch(actual_state) {
                                   
            case "auth_requested":
                setSession(ID, "state", "auth_received");
                setSession(ID, "username", line);
                
                senden(ID, "334 OK 7hg7jfg85j6h=");
                break;
                
            case "auth_received":
                setSession(ID, "state", "auth_accepted");
                setSession(ID, "userpass", line);
                
                // TODO Authentication
                
                senden(ID, "235 2.7.0 Authentication successful");
                break; 
        
        }
        
        if (line.toLowerCase().startsWith("ehlo") || line.toLowerCase().startsWith("helo")) {
        
            String[] parts = line.split(" ");
            relay = parts[1];
            setSession(ID, "relay", relay);
            senden(ID, "250-" + relay);
            senden(ID, "250 AUTH LOGIN");
            
        }
        
        else if (line.startsWith("AUTH")) {                                       
                                        
            setSession(ID, "state", "auth_requested");
            senden(ID, "334 VXNlcm5hbW="); 
            String ar = getSession(ID, "state");                                       
            
        }   
          
        else if (line.toLowerCase().startsWith("ehlo") || line.toLowerCase().startsWith("helo")) {
        
            String[] parts = line.split(" ");
            relay = parts[1];
            setSession(ID, "relay", relay);
            senden(ID, "250-" + relay);
            senden(ID, "250 AUTH LOGIN");
            
        }
        
        else if (line.toLowerCase().startsWith("mail from:")) {


            String[] fromparts = line.split(":");
            String tmp_from = fromparts[1].trim();
            tmp_from = tmp_from.replace("<","");
            from = tmp_from.replace(">","");
            setSession(ID, "from", from);                         
            senden(ID, "250 OK");
            
        }  
        
        else if (line.toLowerCase().startsWith("rcpt to:")) {
            
            String[] toparts = line.split(":");
            String tmp_to = toparts[1].trim();                                        
            
            tmp_to = tmp_to.replace("<","");
            to = tmp_to.replace(">","");
            setSession(ID, "to", to);
            senden(ID, "250 OK " + to);                                        
            
        } 
        
        else if (line.toLowerCase().equals("data")) {
        
            set_messagestate(ID, true);
            senden(ID, "354 Start mail input; end with <CRLF>.<CRLF>");
            
        } 
        
        else if (line.equals(".")) {
            
            set_messagestate(ID, false);
            senden(ID, "250 OK Message queued for delivery");

            String tmp_to[] = getSession(ID, "to").split("@");
            String tomailbox = tmp_to[0];
            String tooffice = tmp_to[1];
            String content = getSession(ID, "message");            
            String logdate = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
            // MailToDisk
            mailToDisk(ID, tomailbox,tooffice,content,logdate);
            logger.log("<SMTP> Mail von <" + getSession(ID, "from") + "> an <" + getSession(ID, "to") + ">");
            
        } else if(line.toLowerCase().equals("quit")) {
        
            senden(ID, "221 OK Bye");  
        
        }
        
        else {
        
            if(get_messagestate(ID) == true) {
                
                String old_message = getSession(ID, "message");
                old_message += line + "\n";
                setSession(ID, "message", old_message);
                String new_message = getSession(ID, "message");
                
            }
            
        }
          
    }
  
    public synchronized void senden(int id, String msg) { 
    
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].send(msg);
                if(Boolean.parseBoolean(getProp("debug"))) {
                    logger.log("<SMTP> [S] -> " + msg + "\n");
                }
            }
        }
        
    }  
    
    public boolean get_messagestate(int id) {
        boolean r = false;
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                r = clients[i].getMessageState();
            }
        } 
        return r;       
    }  
      
    public void set_messagestate(int id, boolean state) {
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].setMessageState(state);
            }
        }        
    }
    
    public void setSession(int id, String prop, String value) { 
    
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].setSessionProp(prop, value);
            }
        }
        
    }
    
    public String getSession(int id, String prop) { 
        String p = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                p = clients[i].getSessionProp(prop);
            }
        }
        return p;
    } 
    
    public void mailToDisk(int id, String tomailbox, String tooffice, String message, String logdate) {
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].writeToDisk(tomailbox, tooffice, message, logdate);
            }
        }
    }        
    
    private int findClient(int ID) {  
    	for (int i = 0; i < clientCount; i++){
        	if (clients[i].getID() == ID){
                return i;
            }
	    }
	    return -1;
    }
        
    public SmtpServerThread findUserThread(String usr) {
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].username.equals(usr)) {
                return clients[i];
            }
        }
        return null;
    }
    	
    @SuppressWarnings("deprecation")
    public synchronized void remove(int ID) {  
        int pos = findClient(ID);
        if (pos >= 0){  
            SmtpServerThread toTerminate = clients[pos];
            if (pos < clientCount-1) {
                // alle darüber um eines zurücksetzen (Stack Prinzip)
                for (int i = pos+1; i < clientCount; i++) {
                    clients[i-1] = clients[i];
                }
            }
            clientCount--;
            try {  
                toTerminate.close(); 
            } catch(IOException ioe) {  
            }
            toTerminate.stop(); 
        }
    }    

}
                          
class SmtpServerThread extends Thread { 
	
    public SMTP server = null;
    public Socket socket = null;
    
    public int ID = -1;
    public String username = "";
    public String mailbox = "";
    public String userpass = "";
    public String full_username = "";
    public String state = "none"; // none, auth_requested, auth_received, auth_accepted
    public boolean message_state = false;
    
    // Mail variablen
    public String relay = "";
    public String from = "";
    public String to = "";                   
    public String message = ""; 

    public PrintWriter w = null;
    public BufferedReader r = null;

    public SmtpServerThread(SMTP _server, Socket _socket) {  
    	  super();
        server = _server;
        socket = _socket;
        ID = socket.getPort();
    }
          
    @SuppressWarnings("deprecation")
	  public void run() { 
    w.println("220 OK SimpleMail SMTP Server ready"); 
        while (true){  
    	      try {  
                String line = r.readLine();
                /*
                if(line.equals("QUIT")) { 
                    w.println("221 OK Bye"); 
                }
        	    	*/
                server.handle(ID, line); 
            }
            catch(Exception ioe){  
                server.remove(ID);
                stop();
            }
        }
    } 
       
    public void open() throws IOException {  
        w = new PrintWriter(socket.getOutputStream(), true);
        r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
       
    public void close() throws IOException {  
    	  if (socket != null) socket.close();
        if (r.readLine() != null) r.close();
        if (w != null) w.close();
    }
    
    // custom funcs
    
    public void send(String msg) {
        w.println(msg);
    }
        
    public int getID() {  
        return ID;
    }
    public String getUsername() {  
        return username;
    }
    
    public void setSessionProp(String prop, String value) {    
        switch(prop) {        
            case "username":
                username = value;
                break;
            case "userpass":
                userpass = value;
                break;
            case "mailbox":
                mailbox = value;
                break;
            case "state":
                state = value;
                break;
            case "relay":
                relay = value;
                break;
            case "from":
                from = value;
                break;
            case "to":
                to = value;
                break;
            case "message":
                message = value;

        }    
    } 
    
    public String getSessionProp(String prop) {    
        switch(prop) {        
            case "username":
                return username;
            case "userpass":
                return userpass;
            case "mailbox":
                return mailbox;
            case "state":
                return state;
            case "relay":
                return relay;
            case "from":
                return from;
            case "to":
                return to;
            case "message":
                return message;
        } 
        return "";  
    }
    public boolean getMessageState() {
        return message_state;
    }
    public void setMessageState(boolean state) {
        message_state = state;
    }
    public void writeToDisk(String to, String at, String message, String logdate) {
        String PATH = "./mailroot";
        String directoryName = PATH + "/" + at + "/" + to + "/inbox"; 
        String fileName = logdate + ".eml";
        File directory = new File(directoryName);
        if (! directory.exists()){
            directory.mkdirs();
        }
        File file = new File(directoryName + "/" + fileName);
        try{
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(message);
            bw.close();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }        
    }
    
}
