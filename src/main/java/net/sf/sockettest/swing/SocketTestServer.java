package net.sf.sockettest.swing;

import net.sf.sockettest.SocketServer;
import net.sf.sockettest.Util;
import net.sf.sockettest.callbacks.DisconnectListener;
import net.sf.sockettest.callbacks.OnMessageRecieve;
import net.sf.sockettest.mode.ActiveEnv;
import net.sf.sockettest.mode.ApplicationMode;
import net.sf.sockettest.vo.RuleEntity;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import net.sf.sockettest.util.TextFormatter;


public class SocketTestServer extends JPanel /*JFrame*/ {
    private final String NEW_LINE = "\r\n";
    private ClassLoader cl = getClass().getClassLoader();
    public ImageIcon logo = new ImageIcon(
            cl.getResource("icons/logo.gif"));

    private ApplicationMode applicationMode;
    private JPanel topPanel;
    private JPanel toPanel;
    
    private JPanel centerPanel;
    private JPanel textPanel;
    private JPanel buttonPanel;
    private JPanel sendPanel;
    private JPanel modePanel; //select mode

    private ArrayList<Socket> clientSocketPool;

    private ButtonGroup group;
    private JRadioButton heartbeatMode = new JRadioButton("heartbeat");
    private JRadioButton normalMode = new JRadioButton("normal");

    private JLabel ipLabel = new JLabel("IP Address");
    private JLabel portLabel = new JLabel("Port");
    private JLabel logoLabel = new JLabel("Socket Test", logo,
            JLabel.CENTER);
    private JTextField ipField = new JTextField("0.0.0.0",20);
    private JTextField portField = new JTextField("21",10);
    private JButton portButton = new JButton("Port");
    private JButton connectButton = new JButton("Start Listening");
    
    private JLabel convLabel = new JLabel("Conversation with Client");
    private Border connectedBorder = BorderFactory.createTitledBorder(new EtchedBorder(),"Connected Client : < NONE >");
    private JTextArea messagesField = new JTextArea();
    
    private JLabel sendLabel = new JLabel("Message");
    private JTextArea sendField = new JTextArea();
    
    private JButton sendButton = new JButton("Send");
    private JButton disconnectButton = new JButton("Disconnect");
    private JButton saveButton = new JButton("Save");
    private JButton clearButton = new JButton("Clear");
    
    private GridBagConstraints gbc = new GridBagConstraints();
    
    private Socket socket;
    private ServerSocket server;
    private SocketServer socketServer;
    private PrintWriter out;
    
    protected final JFrame parent;

    private java.util.List<RuleEntity> entities;

    private Dimension textAreaDimen = new Dimension(20,70);
    
    public SocketTestServer(final JFrame parent) {
        //Container cp = getContentPane();
        this.parent = parent;
        Container cp = this;
        clientSocketPool = new ArrayList<Socket>(10);
        applicationMode = ApplicationMode.getInstance();

        topPanel = new JPanel();
        toPanel = new JPanel();
        toPanel.setLayout(new GridBagLayout());
        gbc.insets = new Insets( 2, 2, 2, 2 );
        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        toPanel.add(ipLabel, gbc);
        
        gbc.weightx = 1.0; //streach
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ActionListener ipListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                portField.requestFocus();
            }
        };
        ipField.addActionListener(ipListener);
        toPanel.add(ipField, gbc);
        
        gbc.weightx = 0.0;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        toPanel.add(portLabel, gbc);
        
        gbc.weightx = 1.0;
        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ActionListener connectListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        };
        portField.addActionListener(connectListener);
        toPanel.add(portField, gbc);
        
        gbc.weightx = 0.0;
        gbc.gridy = 1;
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        portButton.setMnemonic('P');
        portButton.setToolTipText("View Standard Ports");
        ActionListener portButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PortDialog dia = new PortDialog(parent, PortDialog.TCP);
                dia.show();
            }
        };
        portButton.addActionListener(portButtonListener);
        toPanel.add(portButton, gbc);
        
        gbc.weightx = 0.0;
        gbc.gridy = 1;
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        connectButton.setMnemonic('S');
        connectButton.setToolTipText("Start Listening");
        connectButton.addActionListener(connectListener);
        toPanel.add(connectButton, gbc);
        
        toPanel.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(),"Listen On"));
        topPanel.setLayout(new BorderLayout(10,0));
        topPanel.add(toPanel);
        logoLabel.setVerticalTextPosition(JLabel.BOTTOM);
        logoLabel.setHorizontalTextPosition(JLabel.CENTER);
        topPanel.add(logoLabel,BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10,10,5,10));
        
        
        textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout(0,5));
        textPanel.add(convLabel,BorderLayout.NORTH);
        messagesField.setEditable(false);
        JScrollPane jsp = new JScrollPane(messagesField);
        textPanel.add(jsp);
        textPanel.setBorder(BorderFactory.createEmptyBorder(3,3,0,3));
        
        sendPanel = new JPanel();
        sendPanel.setLayout(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        sendPanel.add(sendLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        //gbc.gridwidth = 20;
        gbc.gridheight = 60;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sendField.setEditable(false);
//        sendField.setPreferredSize(textAreaDimen);
        JScrollPane sendpane = new JScrollPane();
        sendpane.setPreferredSize(textAreaDimen);
        sendpane.setViewportView(sendField);
        sendPanel.setPreferredSize(new Dimension(150, 100));
        sendPanel.add(sendpane, gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sendButton.setEnabled(false);
        sendButton.setToolTipText("Send text to client");
        ActionListener sendListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String msg = sendField.getText();
                if(!msg.equals(""))
                    sendMessage(socket, msg);
                else {
                    int value = JOptionPane.showConfirmDialog(
                            SocketTestServer.this,  "Send Blank Line ?",
                            "Send Data To Client",
                            JOptionPane.YES_NO_OPTION);
                    if (value == JOptionPane.YES_OPTION)
                        sendMessage(socket, msg);
                }
            }
        };
        sendButton.addActionListener(sendListener);
//        sendField.addActionListener(sendListener);
        sendPanel.add(sendButton, gbc);
        ActionListener disconnectListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        };
        gbc.gridx = 3;
        disconnectButton.addActionListener(disconnectListener);
        disconnectButton.setEnabled(false);
        sendPanel.add(disconnectButton, gbc);
        
        sendPanel.setBorder(
                new CompoundBorder(
                BorderFactory.createEmptyBorder(0,0,0,3),
                BorderFactory.createTitledBorder("Send")));
        
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(sendPanel, gbc);
        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        saveButton.setToolTipText("Save conversation with client to a file");
        saveButton.setMnemonic('S');
        ActionListener saveListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = messagesField.getText();
                if(text.equals("")) {
                    error("Nothing to save","Save to file");
                    return;
                }
                String fileName="";
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File(""));
                int returnVal = chooser.showSaveDialog(SocketTestServer.this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    fileName=chooser.getSelectedFile().getAbsolutePath();
                    try {
                        Util.writeFile(fileName,text);
                    } catch (Exception ioe) {
                        JOptionPane.showMessageDialog(SocketTestServer.this,
                                ""+ioe.getMessage(),
                                "Error saving to file..",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        saveButton.addActionListener(saveListener);
        buttonPanel.add(saveButton, gbc);
        gbc.gridy = 1;
        clearButton.setToolTipText("Clear conversation with client");
        clearButton.setMnemonic('C');
        ActionListener clearListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                messagesField.setText("");
            }
        };
        clearButton.addActionListener(clearListener);
        buttonPanel.add(clearButton, gbc);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(3,0,0,3));
        
        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(0,10));
        centerPanel.add(buttonPanel,BorderLayout.SOUTH);
        centerPanel.add(textPanel,BorderLayout.CENTER);
        
        CompoundBorder cb=new CompoundBorder(
                BorderFactory.createEmptyBorder(5,10,10,10),
                connectedBorder);
        centerPanel.setBorder(cb);
        
        cp.setLayout(new BorderLayout(10,0));
        cp.add(topPanel,BorderLayout.NORTH);
        cp.add(centerPanel,BorderLayout.CENTER);
    }
    
    /////////////////////
    //action & helper methods
    /////////////////////
    private void connect() {
        if(server!=null) {
            stop();
            return;
        }
        String ip=ipField.getText();
        String port=portField.getText();
        if(ip==null || ip.equals("")) {
            JOptionPane.showMessageDialog(SocketTestServer.this,
                    "No IP Address. Please enter IP Address",
                    "Error connecting", JOptionPane.ERROR_MESSAGE);
            ipField.requestFocus();
            ipField.selectAll();
            return;
        }
        if(port==null || port.equals("")) {
            JOptionPane.showMessageDialog(SocketTestServer.this,
                    "No Port number. Please enter Port number",
                    "Error connecting", JOptionPane.ERROR_MESSAGE);
            portField.requestFocus();
            portField.selectAll();
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if(!Util.checkHost(ip)) {
            JOptionPane.showMessageDialog(SocketTestServer.this,
                    "Bad IP Address",
                    "Error connecting", JOptionPane.ERROR_MESSAGE);
            ipField.requestFocus();
            ipField.selectAll();
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        int portNo = 0;
        try	{
            portNo=Integer.parseInt(port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(SocketTestServer.this,
                    "Bad Port number. Please enter Port number",
                    "Error connecting", JOptionPane.ERROR_MESSAGE);
            portField.requestFocus();
            portField.selectAll();
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        try {
            InetAddress bindAddr=null;
            if(!ip.equals("0.0.0.0"))
                bindAddr = InetAddress.getByName(ip);
            else
                bindAddr = null;
            server = new ServerSocket(portNo,1,bindAddr);
            
            ipField.setEditable(false);
            portField.setEditable(false);
            
            connectButton.setText("Stop Listening");
            connectButton.setMnemonic('S');
            connectButton.setToolTipText("Stop Listening");
            //sendButton.setEnabled(true);
            //sendField.setEditable(true);
        } catch (Exception e) {
            error(e.getMessage(), "Starting Server at "+portNo);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        //changeBorder(" "+socket.getInetAddress().getHostName()+
        //	" ["+socket.getInetAddress().getHostAddress()+"] ");
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        messagesField.setText("> Server Started on Port: "+portNo+NEW_LINE);
        append("> ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        socketServer=SocketServer.handle(this,server);
        socketServer.addMessageListener(new OnMessageRecieve() {
            public void onRecieveMsg(Socket so, String message) {
                sendMessage(so, message);
            }
        });
        applicationMode.setEnv(ActiveEnv.SERVER);
        applicationMode.initTimer();
        //sendField.requestFocus();
    }

    private DisconnectListener disconnectListener;
    public void addDisconnectListener(DisconnectListener disconnectListener){
        this.disconnectListener = disconnectListener;
    }

    //disconnect a client
    public synchronized void disconnect() {
        try {
            socketServer.setDesonnected(true);
//            applicationMode.cancelAllTasks();
            if(disconnectListener!=null){
                disconnectListener.onDisconnect();
            }
//            applicationMode.setEnv(null);
        } catch (Exception e) {}
        applicationMode.setEnv(null);
    }
    
    public synchronized void stop() {
        try {
            disconnect(); //close any client
            socketServer.setStop(true);
        } catch (Exception e) {}
        server=null;
        ipField.setEditable(true);
        portField.setEditable(true);
        connectButton.setText("Start Listening");
        connectButton.setMnemonic('S');
        connectButton.setToolTipText("Start Listening");
        append("> Server stopped");
        append("> ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
    
    public synchronized void setClientSocket(Socket s) {
        
        if(s==null) {
            out=null;
            socket = null;
            changeBorder(null);
            sendButton.setEnabled(false);
            sendField.setEditable(false);
            disconnectButton.setEnabled(false);
        } else {
            socket = s;
            changeBorder(" "+socket.getInetAddress().getHostName()+
                    " ["+socket.getInetAddress().getHostAddress()+"] ");
            sendButton.setEnabled(true);
            sendField.setEditable(true);
            disconnectButton.setEnabled(true);
        }
    }
    
    public void error(String error) {
        if(error==null || error.equals(""))
            return;
        JOptionPane.showMessageDialog(SocketTestServer.this,
                error, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public void error(String error, String heading) {
        if(error==null || error.equals(""))
            return;
        JOptionPane.showMessageDialog(SocketTestServer.this,
                error, heading, JOptionPane.ERROR_MESSAGE);
    }
    
    public void append(String msg) {
        messagesField.append(msg+NEW_LINE);
        messagesField.setCaretPosition(messagesField.getText().length());
    }
    
    public void appendnoNewLine(String msg) {
        messagesField.append(msg);
        messagesField.setCaretPosition(messagesField.getText().length());
    }
    public void sendMessage(String s) {
        sendMessage(socket, s);
    }
    public void sendMessage(Socket so, String s) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try	{
            if(out==null) {
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(so.getOutputStream())), true);
            }
            append("Sent:: "+TextFormatter.transformMsg(s));
            out.print(s);
            out.flush();
            sendField.setText("");
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            JOptionPane.showMessageDialog(SocketTestServer.this,
                    e.getMessage(),"Error Sending Message",
                    JOptionPane.ERROR_MESSAGE);
            disconnect();
        }
    }
    
    private void changeBorder(String ip) {
        if(ip==null || ip.equals(""))
            connectedBorder = BorderFactory.createTitledBorder(
                    new EtchedBorder(), "Connected Client : < NONE >");
        else
            connectedBorder = BorderFactory.createTitledBorder(
                    new EtchedBorder(), "Connected Client : < "+ip+" >");
        CompoundBorder cb=new CompoundBorder(
                BorderFactory.createEmptyBorder(5,10,10,10),
                connectedBorder);
        centerPanel.setBorder(cb);
        invalidate();
        repaint();
    }    
}
