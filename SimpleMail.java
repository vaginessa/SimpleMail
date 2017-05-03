import javax.swing.*;
import java.awt.*; 
import java.awt.event.*;
import java.*;
import java.io.*; 
import java.net.*;

import java.util.Date;
import java.text.SimpleDateFormat;

import javax.swing.text.DefaultCaret;
import java.io.BufferedWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;

public class SimpleMail { 

    public static void main(String[] args) {     
        JFrame gui = new GUI(); 
        gui.show();        
    } 
    
} 


class GUI extends JFrame { 
    
    public JButton POPButton;
    public JButton SMTPButton;
    public JButton IMAPButton;
    public JButton CLRButton;
    
    public JScrollPane scrollPanel;
    public JTextArea logger;
    public DefaultCaret caret;
    
    public IMAP imap;
    public POP pop;
    public SMTP smtp;
    
    public TrayIcon trayIcon;
    public SystemTray tray; 

    public GUI() { 
    
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
        }
    
        setTitle("SimpleMail 0.1"); 
        setPreferredSize(new Dimension(700,300));
        
        if(SystemTray.isSupported()){

            tray = SystemTray.getSystemTray();
            URL imageURL = GUI.class.getResource("simplemail.png");
            ImageIcon ico = new ImageIcon(imageURL);        
            Image image=Toolkit.getDefaultToolkit().getImage(imageURL);            
            ActionListener exitListener=new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            };
            PopupMenu popup=new PopupMenu();            
            MenuItem defaultItem = new MenuItem("anzeigen");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(true);
                    setExtendedState(JFrame.NORMAL);
                }
            });
            popup.add(defaultItem);
            defaultItem=new MenuItem("beenden");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeWindow();
                }
            });
            popup.add(defaultItem);
            trayIcon=new TrayIcon(image, "SimpleMail", popup);
            trayIcon.setImageAutoSize(true);
        }
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                if(e.getNewState()==ICONIFIED){
                    try {
                        tray.add(trayIcon);
                        setVisible(false);
                    } catch (AWTException ex) { }
                }
                if(e.getNewState()==7){
                    try{
                        tray.add(trayIcon);
                        setVisible(false);
                        }catch(AWTException ex){
                    }
                }
                if(e.getNewState()==MAXIMIZED_BOTH){
                    tray.remove(trayIcon);
                    setVisible(true);
                }
                if(e.getNewState()==NORMAL){
                    tray.remove(trayIcon);
                    setVisible(true);
                }
            }
        });
        URL imageURL = GUI.class.getResource("simplemail.png");
        setIconImage(Toolkit.getDefaultToolkit().getImage(imageURL));                
        
        setJMenuBar(createMenu(this));
                 
        SMTPButton = new JButton();
        IMAPButton = new JButton();
        POPButton = new JButton();
        SMTPButton = new JButton();
        CLRButton = new JButton();
        
        scrollPanel = new JScrollPane();
        logger = new JTextArea();
        
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) { 
                closeWindow();
            }
        });        
        
        logger.setColumns(20);
        logger.setFont(new java.awt.Font("Consolas", 0, 12)); 
        logger.setRows(5); 
        logger.setLineWrap(true);
        logger.setWrapStyleWord(true);
        logger.setEditable(false);
        logger.setFocusable(false);
        
        caret = (DefaultCaret)logger.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                                 
        scrollPanel.setViewportView(logger);
        
        IMAPButton.setText("IMAP Start");
        IMAPButton.setEnabled(true);
        IMAPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IMAPButtonAction(evt);
            }
        });
        
        POPButton.setText("POP Start");
        POPButton.setEnabled(true);
        POPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                POPButtonAction(evt);
            }
        }); 
        
        SMTPButton.setText("SMTP Start");
        SMTPButton.setEnabled(true);
        SMTPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SMTPButtonAction(evt);
            }
        });
        
        CLRButton.setText("CLEAR LOG");
        CLRButton.setEnabled(true);
        CLRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CLRButtonAction(evt);
            }
        });
       
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPanel) 
                    .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(IMAPButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED) 
                        .addComponent(POPButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SMTPButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CLRButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(IMAPButton)
                    .addComponent(POPButton)
                    .addComponent(SMTPButton)
                    .addComponent(CLRButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();        
        setLocationRelativeTo(null);

    } 
    
    public void closeWindow() {
        String ObjButtons[] = {"Beenden","abbrechen"};
        int PromptResult = JOptionPane.showOptionDialog(null,"Wollen Sie die SimpleMail wirklich beenden?","SimpleMail beenden",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,ObjButtons,ObjButtons[0]);
        if(PromptResult==JOptionPane.YES_OPTION) {
            System.exit(0);
        }    
    }
    
    public JMenuBar createMenu(JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Einstellungen");
                
        JMenuItem smtpoptions = new JMenuItem("SMTP");

        smtpoptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {                
                popup(event);                          
            }
        });
        menu.add(smtpoptions);
                
        JMenuItem popoptions = new JMenuItem("POP");                        
        popoptions.addActionListener(new ActionListener() {
            @Override
                public void actionPerformed(ActionEvent event) {                
                popup(event);         
            }
        });
        menu.add(popoptions);
        
        JMenuItem imapoptions = new JMenuItem("IMAP");                        
        imapoptions.addActionListener(new ActionListener() {
            @Override
                public void actionPerformed(ActionEvent event) {                
                popup(event);         
            }
        });
        menu.add(imapoptions);
         
        menuBar.add(menu);
        return menuBar;
        
    }  

    private void IMAPButtonAction(java.awt.event.ActionEvent evt) {
        String action = evt.getActionCommand();
        if (action.equals("IMAP Start")) {
            imap = new IMAP(this);
            IMAPButton.setText("IMAP Stop");
        }  
        if (action.equals("IMAP Stop")) {
           imap.stop();
           IMAPButton.setText("IMAP Start");
        } 
    } 
    
    private void POPButtonAction(java.awt.event.ActionEvent evt) {    
        String action = evt.getActionCommand();
        if (action.equals("POP Start")) {
            pop = new POP(this);
            POPButton.setText("POP Stop");
        }  
        if (action.equals("POP Stop")) {
           pop.stop();
           POPButton.setText("POP Start");
        }            
    }
    
    private void SMTPButtonAction(java.awt.event.ActionEvent evt) {    
        String action = evt.getActionCommand();
        if (action.equals("SMTP Start")) {
            smtp = new SMTP(this);
            SMTPButton.setText("SMTP Stop");
        }  
        if (action.equals("SMTP Stop")) {
           smtp.stop();
           SMTPButton.setText("SMTP Start");
        }            
    }
    
    private void CLRButtonAction(java.awt.event.ActionEvent evt) {    
        logger.setText("");            
    }
    
    public void popup(ActionEvent e) {
        String action = e.getActionCommand().toLowerCase();
        boolean i = editFile(new File(action+".ini"));
    }
    
    public boolean editFile(final File file) {
      if (!Desktop.isDesktopSupported()) {
        return false;
      }
    
      Desktop desktop = Desktop.getDesktop();
      if (!desktop.isSupported(Desktop.Action.EDIT)) {
        return false;
      }
    
      try {
        desktop.edit(file);
      } catch (IOException e) {
        // Log an error
        return false;
      }
    
      return true;
    }

    public void log(String s) {
    
        String logdate = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logger.append("[" + logdate + "] " + s + "\n");
    
    }
    
}
