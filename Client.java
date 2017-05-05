/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Vaishali Vijaykumar
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class Client {
	
	public String cName; 
	int portnum;
	Scanner userInput;
	Scanner serverReply;
	static PrintStream sendToServer;
	static Socket clientSocket;
	private String server = "localhost";
	
	
	Client(String server, int portnum, String cName) {
		this.server = server;
		this.portnum = portnum;
		this.cName = cName;
	}
	
	public boolean start() 
	{
		// try to connect to the server
		try {
			clientSocket = new Socket(server, portnum);
		   } 
		// if it failed not much I can so
		catch(Exception ec) {
			System.out.println("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + clientSocket.getInetAddress() + ":" + clientSocket.getPort();
		System.out.println(msg);
	
		/* Creating both Data Stream */
		try
		{
			serverReply = new Scanner(clientSocket.getInputStream());
			sendToServer = new PrintStream(clientSocket.getOutputStream());
			userInput = new Scanner(System.in);
			
		}
		catch (IOException eIO) {
			System.out.println("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
                sendToServer.println(cName);
		// success we inform the caller that it worked
		return true;
	}
	
	private void disconnect() 
	{
		try { 
			if(serverReply != null) serverReply.close();
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(sendToServer != null) sendToServer.close();
		}
		catch(Exception e) {} // not much else I can do
        try{
			if(clientSocket != null) clientSocket.close();
		}
		catch(Exception e) {}
			
	}
	
	class ListenFromServer extends Thread 
	{

		public void run() 
		{
			while(true) 
			{
                // can't happen with a String object but need the catch anyhow
                String msg = serverReply.nextLine();
                System.out.println(msg);
			}
		}
	}

	void sendMessage(String msg) {
            sendToServer.println(msg);
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		String userMsg,servMsg;
		InputStream in =null;
		OutputStream out = null;
		String cName=args[0], portString=args[1];
		FileInputStream fis = null;
	    BufferedInputStream bis = null;
	    OutputStream os = null;
		
		Client client = new Client("localhost", Integer.parseInt(portString), cName);
		if(!client.start())
			return;
		
		// wait for messages from user
		Scanner userInput = new Scanner(System.in);
		do
		{
			System.out.println("Please enter command as a string");
			String command = userInput.nextLine();
			String check = command.substring(0, command.indexOf('\"')-1);
//                        if(in != null)
//                            in.close();
//                        
//                        if(out != null)
//                            out.close();
                        
			if(check.equals("broadcast file")||check.equals("unicast file"))
			{ 
				String fileName = command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));
				File file = new File(fileName);
			    byte[] b = new byte[20*1024]; 
			    in = new FileInputStream(file);
                            //Path path = Paths.get("Journey Confirmation.pdf");
                            //byte[] data = Files.readAllBytes(path);
			    out = clientSocket.getOutputStream();
			    
			    int i ;
			    
			    while ((i = in.read(b))>0) 
				{
					out.write( b,0,i);
				}
			    break;
				
			}
			sendToServer.println(command);
		}while(!userInput.equals("^C~"));
		
		//out.close();
	    //in.close();
		clientSocket.close();
		client.disconnect();
	}

		
}
