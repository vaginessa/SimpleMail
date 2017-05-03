import java.io.*;
import java.net.*;
import java.*;
import javax.swing.*;

import java.util.Properties;

public class IMAP extends Thread implements Runnable {
    
    public ImapServerThread clients[];
    public ServerSocket server = null;
    public Thread thread = null; 
    public int clientCount = 0;
    public int port; 
    public GUI logger;
    public Properties prop = new Properties();
      
    public IMAP(GUI log) {
        clients = new ImapServerThread[50];
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
            input = new FileInputStream("imap.ini");
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
            server.close();
        } catch(IOException ioe) {}
    }
    
    private void addThread(Socket socket) {  
      	if (clientCount < clients.length) {  
      	    clients[clientCount] = new ImapServerThread(this, socket);
      	    try{ 
      	      	clients[clientCount].open(); 
      	        clients[clientCount].start();  
      	        clientCount++; 
      	    } catch(IOException ioe) { } 
      	} else { }
    }
    
    public synchronized void handle(int ID, String line) {     
        
        // check tag
        String[] tmp_tag = line.split(" "); //line.charAt(0);
        String tag = tmp_tag[0];
        
        //debug("TAG=" + tag);
        
        if(Boolean.parseBoolean(getProp("debug"))) {
            logger.log("<IMAP> [C] <- " + line + "\n");
        }
        if (line.startsWith(tag + " LOGIN ")) {
        
            String[] userparts = line.split(" ");
            
            String tmp_username = userparts[2];
            String tmp_userpass = userparts[3];
            
            tmp_username = tmp_username.replaceAll("[\"]","");
            tmp_userpass = tmp_userpass.replaceAll("[\"]","");
            
            setSession(ID, "username", tmp_username);
            setSession(ID, "userpass", tmp_userpass);
            
            senden(ID, tag + " OK login sucessful");
            
        } 
        
        else if(line.startsWith(tag + " CAPABILITY")) {
        
            senden(ID, "* CAPABILITY IMAP4rev1 NAMESPACE QUOTA");
            senden(ID, tag + " OK");
            
        }
        
        else if (line.startsWith(tag + " GETQUOTAROOT ")) {
        
            String[] tmp_folder = line.split(" ");
            String folder = tmp_folder[2];
            String u = getSession(ID, "username");
            senden(ID, tag + "* QUOTAROOT " + folder + " \"\" " + u);
            
        
            String tmp_username = getSession(ID, "username");
            String[] tmp_u = tmp_username.split("@");
            
            File t_f;
            long dirsize = 0;
            try { 
                t_f = new File("mailroot/" + tmp_u[1] + "/" + tmp_u[0] + "/" + folder); 
                dirsize = folderSize(t_f);
            }catch(Exception e) { e.printStackTrace(); }
            
            
            senden(ID, tag + "* QUOTA \"\" (STORAGE " + dirsize + " 100000)");                                    
            //senden(ID, tag + "* QUOTA \"\" (STORAGE 0 10000000)");
            senden(ID, tag + " OK GETQUOTAROOT completed");
            
        }
        
        else if (line.startsWith(tag + " NOOP")) {
        
            senden(ID, tag + " OK");
            
        } 
        
        else if (line.startsWith(tag + " NAMESPACE")) {
            
            // TODO Namespace response
            senden(ID, tag + " * NAMESPACE ((\"INBOX.\" \".\")) ((\"user.\" \".\")) ((\"\" \".\"))");
            senden(ID, tag + " OK completed");
            
        }                                
        
        else if (line.startsWith(tag + " LIST")) {
        
            // TODO List response
            String[] listparts = line.split("LIST");
            String tmp_selected_folder = listparts[1];
            senden(ID, "* LIST (HasNoChildren) \"/\" " + tmp_selected_folder);
            senden(ID, tag + " OK Completed");
            
        }
        
        else if (line.startsWith(tag + " SELECT")) {
        
            // TODO Select response
            senden(ID, "* 24 Exists");
            senden(ID, "* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
            senden(ID, "* 2 Recent");
            senden(ID, "* OK [UNSEEN 17] Message 17 is the first unseen message"); //debug(tag + " OK " + tmp_selected_folder + " selected");
            senden(ID, tag + " OK [READ-WRITE] SELECT completed");
            
        }
        
        else if (line.startsWith(tag + " UID SEARCH")) {
             
            // TODO UID SEARCH Response                                
            senden(ID, "* 1 EXISTS");
            senden(ID, "* 1 RECENT");
            
            senden(ID, "* SEARCH ");
            senden(ID, tag + " OK - search completed");
            
        }
        
        else if (line.startsWith(tag + " FETCH")) {
        

            String[] msgnrparts = line.split("FETCH");
            System.out.println(msgnrparts[1].trim());

            // TODO Die echten Parameter parsen
            // https://www.skytale.net/blog/archives/23-Manual-IMAP.html
            senden(ID, "* 1 FETCH (FLAGS BODY[HEADER.FIELDS (To)] {28}");
            
            String trf = "mailroot/zarat.ml/manuel/inbox/20170427192354.eml";
            String rf = readMail(trf);
            
            // TODO Mail Felder Parsen
            System.out.println("<RECV> " + readMailMeta(trf, "Received "));
            System.out.println("<X-RECV> " + readMailMeta(trf, "X-Received: "));
            System.out.println("<FROM> " + readMailMeta(trf, "From: "));
            System.out.println("<TO> " + readMailMeta(trf, "To: "));

            // Hier wird einfach die ganze Mail gesendet, das darf nicht sein
            senden(ID, rf);
            senden(ID, ")");
            
            senden(ID, tag + " OK - fetch completed");
            
        }
                                       
        else {  
                                      
            //debug(line);
                                               
        }
        
    }
    
    public synchronized void senden(int id, String msg) { 
    
        for(int i = 0; i < clientCount; i++){
            if (clients[i].getID() == id){
                clients[i].send(msg);
                if(Boolean.parseBoolean(getProp("debug"))) {
                    logger.log("<IMAP> [S] -> " + msg + "\n");
                }
            }
        }
        
    }
    
    public synchronized void setSession(int id, String prop, String value) { 
    
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
    
    public static long folderSize(File directory) {
        
        long length = 0;
        try {
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        }catch(Exception e) {}
        return length;
        
    }
    
    public String readMail(final String file) {
    
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
    
    public String readMailMeta(final String file, String meta) {
    
        String zeile, ergebnis = "";    
        try {        
            FileReader fr = new FileReader(new File(file));
            BufferedReader br = new BufferedReader(fr);                        
            while((zeile = br.readLine()) != null ) {           
                if(zeile.startsWith(meta)) {
                    ergebnis = zeile;
                }            
            }            
            br.close();        
        } catch(Exception e) {}        
        return ergebnis;
        
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
    
    public void Announce(String msg) {
        for(int i = 0; i < clientCount; i++){
            clients[i].send(msg);
        }
    }
        
    public ImapServerThread findUserThread(String usr) {
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
            ImapServerThread toTerminate = clients[pos];
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
                          
class ImapServerThread extends Thread { 
	
    public IMAP server = null;
    public Socket socket = null;
    
    public int ID = -1;
    public String username = "";
    public String mailbox = "";
    public String userpass = "";
    public String full_username = "";
    public String state = "none"; // none, auth_requested, auth_received, auth_accepted, not_accepted ?!?

    public PrintWriter w = null;
    public BufferedReader r = null;

    public ImapServerThread(IMAP _server, Socket _socket) {  
    	  super();
        server = _server;
        socket = _socket;
        ID = socket.getPort();
    }
          
    @SuppressWarnings("deprecation")
	  public void run() { 
    w.println("* OK SimpleMail IMAP Server ready"); 
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

            case "userpass":
                userpass = value;
 
            case "mailbox":
                mailbox = value;
                
            case "state":
                state = value;

        }    
    } 
    
    public String getSessionProp(String prop) {    
        String p = "";
        switch(prop) {        
            case "username":
                p = username;
                break;
            case "userpass":
                p = userpass;
                break;
            case "mailbox":
                p = mailbox;
                break;
            case "state":
                p = state;
                break;
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
                            ergebnis += ic + " " + zeile.replace("Subject:","").trim();
                            if(ic<listOfFiles.length) { ergebnis+="\n"; } 
                            ic++; 
                        }                
                    }                
                    br.close();           
                } catch(Exception e) {}           
                content += ergebnis;           
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
