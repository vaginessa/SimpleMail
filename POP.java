import java.io.*;
import java.net.*;
import java.*;
import javax.swing.*;

import java.util.Properties;

public class POP extends Thread implements Runnable {
    
    public PopServerThread clients[];
    public ServerSocket server = null;
    public Thread thread = null; 
    public int clientCount = 0;
    public int port;
    public GUI logger; 
    public Properties prop = new Properties();

    public POP(GUI log) {
        clients = new PopServerThread[50];
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
            input = new FileInputStream("pop.ini");
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
      	    clients[clientCount] = new PopServerThread(this, socket);
      	    try{ 
      	      	clients[clientCount].open(); 
      	        clients[clientCount].start();  
      	        clientCount++; 
      	    } catch(IOException ioe) { } 
      	} else { }
    }
    
    public synchronized void handle(int ID, String line) {
    
        if(Boolean.parseBoolean(getProp("debug"))) {
            logger.log("<POP> [C] <- " + line + "\n"); 
        }
        
        if (line.toLowerCase().startsWith("apop ")) {
        
            String[] userparts = line.split(" ");
            
            String tmp_username = userparts[1];
            String tmp_userpass = userparts[2];
            
            String[] split_username = tmp_username.split("@");

            tmp_username = split_username[0];
            String tmp_mailbox = split_username[1]; 

            setUserprop(ID, "username", tmp_username);
            setUserprop(ID, "userpass", tmp_userpass);
            setUserprop(ID, "mailbox", tmp_mailbox);

            senden(ID, "+OK " + tmp_username);
            
        } 
        
        else if (line.toLowerCase().startsWith("user ")) {
                                
            String[] userparts = line.split(" ");
            String tmp_username = userparts[1];            
            tmp_username = tmp_username.replace("<","");
            tmp_username = tmp_username.replace(">","");
            setUserprop(ID, "username", tmp_username);            
            senden(ID, "+OK Password required for " + tmp_username); 
                                               
        } 
        
        else if (line.toLowerCase().startsWith("pass ")) {
        
            String[] userpassparts = line.split(" ");
            String tmp_userpass = userpassparts[1]; 
            // TODO check credentials
            senden(ID, "+OK logged in");
            
        } 
        
        else if (line.toLowerCase().equals("list")) {

            String mc = getMessageCount(ID, "inbox");
            String folder_content = getFolder(ID, "inbox");
            
            senden(ID, "+OK " + mc);
            senden(ID, folder_content);
            
        } 
        
        else if (line.toLowerCase().equals("stat")) {
        
            String mc = getMessageCount(ID, "inbox");
            senden(ID, "+OK " + mc);
            
        } 
        
        else if (line.toLowerCase().equals("top")) {
        
            String mc = getMessageCount(ID, "inbox");
            String folder_content = getFolderBriefly(ID, "inbox");
            
            senden(ID, "+OK " + mc);
            if(!folder_content.equals("")) {
                senden(ID, folder_content);
            }
            
        } 
        
        else if (line.toLowerCase().startsWith("get ") || line.toLowerCase().startsWith("retr ")) {
        
            String[] getparts = line.split(" ");
            int file_id = Integer.parseInt(getparts[1]);
            String content_of_file = getFile(ID, file_id);
            
            senden(ID, "+OK");
            senden(ID, content_of_file);
            
        } 
        
        else if (line.toLowerCase().equals("capa")) { 
                                       
            senden(ID, "+OK STAT TOP LIST GET");
                                                
        } else if(line.toLowerCase().equals("quit")) {
        
            senden(ID, "+OK Bye");
            remove(ID);
        
        }      
        
        else {  
                                      
            senden(ID, "-ERR unrecognized command");
                                               
        } 
        
    }
    
    public synchronized void senden(int id, String msg) { 
    
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].send(msg);
                if(Boolean.parseBoolean(getProp("debug"))) {
                    logger.log("<POP> [S] -> " + msg + "\n");
                }
            }
        }
        
    }
    
    public synchronized void setUserprop(int id, String prop, String value) { 
    
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].setProp(prop, value);
            }
        }
        
    }
    
    public String getUserprop(int id, String prop) { 
        String p = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                p = clients[i].getProp(prop);
            }
        }
        return p;
    }
    
    public synchronized String getMessageCount(int id, String folder) { 
    
        String ret = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                ret = clients[i].messageCount(folder);
            }
        }
        return ret;
    }
    
    public synchronized String getFolder(int id, String folder) { 
    
        String ret = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                ret = clients[i].listFolder(folder);
            }
        }
        return ret;
    }
    
    public synchronized String getFolderBriefly(int id, String folder) { 
    
        String ret = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                ret = clients[i].listFolderBriefly(folder);
            }
        }
        return ret;
    }
    
    public synchronized String getFile(int id, int file) { 
    
        String ret = "";
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                ret = clients[i].getSingleFile(file);
            }
        }
        return ret;
    }
    
    private int findClient(int ID) {  
    	for (int i = 0; i < clientCount; i++){
        	if (clients[i].getID() == ID){
                return i;
            }
	    }
	    return -1;
    }
        
    public PopServerThread findUserThread(String usr) {
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
            PopServerThread toTerminate = clients[pos];
            if (pos < clientCount-1) {
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
                          
class PopServerThread extends Thread { 
	
    public POP server = null;
    public Socket socket = null;
    
    public int ID = -1;
    public String username = "";
    public String mailbox = "";
    public String userpass = "";
    public String full_username = "";

    public PrintWriter w = null;
    public BufferedReader r = null;

    public PopServerThread(POP _server, Socket _socket) {  
    	  super();
        server = _server;
        socket = _socket;
        ID = socket.getPort();
    }
          
    @SuppressWarnings("deprecation")
	  public void run() {  
        while (true){  
    	      try {  
                String line = r.readLine();
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
    
    public void send(String msg) {
        w.println(msg);
    }
        
    public int getID() {  
        return ID;
    }
    public String getUsername() {  
        return username;
    }
    
    public void setProp(String prop, String value) {    
        switch(prop) {        
            case "username":
                username = value;

            case "userpass":
                userpass = value;
 
            case "mailbox":
                mailbox = value;

        }    
    } 
    
    public String getProp(String prop) {    
        String p = "";
        switch(prop) {        
            case "username":
                p = username;

            case "userpass":
                p = userpass;

            case "mailbox":
                p = mailbox;

        }
        return p;    
    } 
    
    public String messageCount(String the_folder) {   

        int mc = 0;
        double size = 0.00;
        try {
        File folder = new File("mailroot/" + mailbox + "/" + username + "/" + the_folder);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {      
            File file = listOfFiles[i];
            if (file.isFile()) {
                mc++;
                double bytes = file.length();
                size = size + bytes;          
            }         
        }

        } catch(Exception e) {}
        return mc + " messages (" + (int)size + " octets)";

    }
    
    public String listFolder(String the_folder) {   
        
        String content = "";
        try {
        File folder = new File("./mailroot/" + mailbox + "/" + username + "/" + the_folder);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {        
            File file = listOfFiles[i];
            if (file.isFile()) {
                content += "#"+i+"\n";
                content += readFile(""+file); /* read the file */            
            }           
        }
        }catch(Exception e) {}
        return content;
        
    }
    
    public String listFolderBriefly(String the_folder) {   

        String content = "";
        try {
        File folder = new File("./mailroot/" + mailbox + "/" + username + "/" + the_folder);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles.length < 1) { return ""; }
        int ic = 0;
        for (int i = 0; i < listOfFiles.length; i++) {      
            File file = listOfFiles[i];
            if (file.isFile()) {
                String zeile, ergebnis = "";        
                try {            
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);                            
                    while((zeile = br.readLine()) != null ) {                
                        if(zeile.startsWith("Subject:")) { 
                            content += ic + " " + zeile.replace("Subject:","").trim();
                            if(ic<listOfFiles.length-1) { 
                                content+="\n"; 
                            } 
                            ic++; 
                        }                
                    }                
                    br.close();           
                } catch(Exception e) {}           
                //content += ergebnis;           
            }         
        }
        }catch(Exception e) {}
        return content;
        
    }
    
    public String readFile(final String file) {
    
        String zeile, ergebnis = "";    
        try {        
            FileReader fr = new FileReader(new File(file));
            BufferedReader br = new BufferedReader(fr);                        
            while((zeile = br.readLine()) != null ) {           
                ergebnis += zeile+"\n";            
            }            
            br.close();        
        } catch(Exception e) {}        
        return ergebnis;
        
    }
    
    public String getSingleFile(int messagenr) {   

        File folder = new File("./mailroot/" + mailbox + "/" + username + "/inbox");
        File[] listOfFiles = folder.listFiles();
        String ergebnis = "";
        for (int i = 0; i < listOfFiles.length; i++) {      
            File file = listOfFiles[i];            
            if (file.isFile() && i==messagenr) {         
                String zeile = "";           
                try {               
                    FileReader fr = new FileReader(listOfFiles[messagenr]);
                    BufferedReader br = new BufferedReader(fr);                                
                    while((zeile = br.readLine()) != null ) {                    
                        ergebnis += zeile+"\n";                    
                    }                    
                    br.close();                
                } catch(Exception e) {}            
            }        
        }        
        return ergebnis;

    }
    
}
