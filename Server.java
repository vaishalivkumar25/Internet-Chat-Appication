/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Vaishali Vijaykumar
 */
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static int uniqueId;
    ArrayList<ClientThread> connection ;
	private final String portnum;
	private boolean keepGoing;
    
    
    public Server(String portnum) 
    {
		this.portnum = portnum;
		connection = new ArrayList<ClientThread>();
	}
    
	public void start() 
	{
		keepGoing = true;
		try 
		{
			ServerSocket serverSocket = new ServerSocket(Integer.parseInt(portnum)); 
			System.out.println("Server started.");
    	
		while(keepGoing) 
		{
			// format message saying we are waiting
			//System.out.println("Server waiting for Clients on port " + portnum + ".");
			Socket cliServSocket = serverSocket.accept();  	// accept connection
			if(!keepGoing)
				break;
			ClientThread newClient = new ClientThread(cliServSocket);  // make a thread of it
			connection.add(newClient);									// save it in the ArrayList
			newClient.start();
		}
		// I was asked to stop
		try 
		{
			serverSocket.close();
			for(int i = 0; i < connection.size(); ++i) 
			{
				ClientThread tc = connection.get(i);
				try {
				tc.clientInput.close();
				tc.sendToClient.close();
				tc.socket.close();
				}
				catch(IOException ioE) {}
			}
		}
		catch(Exception e) 
		{
			System.out.println("Exception closing the server and clients: " + e);
		}
		}
		// something went bad
		catch (IOException e) 
		{
        String msg = " Exception on new ServerSocket: " + e + "\n";
		System.out.println(msg);
		}
	}

	
	class ClientThread extends Thread 
	{
		Socket socket;  // the socket where to listen/talk
		Scanner clientInput;
		PrintStream sendToClient;
		int id;  // my unique id (easier for deconnection)
		String cName,command; // the Username of the Client
		String cType,clientMsg; // the only type of message a will receive
		InputStream is=null;
		OutputStream out=null;
		
		ClientThread(Socket socket) 
		{
			id = ++uniqueId;// a unique id
			this.socket = socket;
			try
			{
				// create output first
				sendToClient = new PrintStream(socket.getOutputStream());
				clientInput  = new Scanner(socket.getInputStream());
				// read the client name
				cName= clientInput.nextLine();
				System.out.println((cName + " is now connected."));
			}
			catch (IOException e) 
			{
				System.out.println("Exception creating new Input/output Streams: " + e);
			}
		}

		public void run() 
		{
			boolean keepGoing = true;
			while(keepGoing) 
			{
                command = clientInput.nextLine();
				// the message part of the ChatMessage
				cType = command.substring(0, command.indexOf('\"')-1) ;
				switch(cType)
				{
				case "broadcast message":
				{
					//Message from Client
					clientMsg=command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));					
					broadcast("@"+ cName+ " : " + clientMsg,this);
					System.out.println(cName + " has broadcasted a message");
					break;
				}
				case "broadcast file":
				{	
					String fileName = command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));
					
					//broadcastFile(fileName, connection.indexOf(cName));
					
					System.out.println(cName + " has broadcasted a file ");
					break;
				}
					
				case "unicast message":
				{
					//Message from Client
					clientMsg=command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));
					String otherClient = command.substring( command.lastIndexOf('\"')+2, command.length());
					unicast("@"+ cName+ " : " + clientMsg,otherClient);
					System.out.println(cName + " has sent a message to " + otherClient);
					break;
				}
				case "unicast file":
				{
					String fileName = command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));
					String otherClient = command.substring( command.lastIndexOf('\"')+2, command.length());
					InputStream in =null;
					OutputStream out = null;
					try 
					{
						in = socket.getInputStream();
					} 
					catch (IOException e) 
					{
						System.out.println("Can not get input stream , or socket error");
					}
					
					try 
					{
						out = new FileOutputStream(fileName);
					} 
					catch (FileNotFoundException e) 
					{
						System.out.println("File Not Found !");
					}
					
					byte[] b = new byte[20*1024];

					int i ;
					
					try {
						while((i = in.read(b))>0){
							out.write(b, 0, i);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						
					System.out.println(cName + " has sent a file to " + otherClient);
					break;
				}
				
					
				case "blockcast message":
				{
					//Message from Client
					clientMsg=command.substring(command.indexOf('\"')+1, command.lastIndexOf('\"'));
					String otherClient = command.substring(command.lastIndexOf('\"')+2, command.length());					
					blockcast("@"+ cName+ " : " + clientMsg,this,otherClient);
					System.out.println(cName + " has sent a message to all, except " + otherClient);
					break;
				}
				default:
					System.out.println("Invalid Input command");
					break;				
				}
				
			}
			// remove myself from the arrayList containing the list of the connected Clients
		remove(id);
		close();
	    }
		
		

		private void close() 
		{
			// try to close the connection
			try 
			{
				if(sendToClient != null) sendToClient.close();
			}
			catch(Exception e) {}
			try 
			{
				if(clientInput != null) clientInput.close();
			}
			catch(Exception e) {};
			try 
			{
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}
	
		
		private boolean writeMsg(String msg) 
		{
			// if Client is still connected send the message to it
			if(!socket.isConnected()) 
			{
				close();
				return false;
			}
			else
			{
				//System.out.println(msg);
                  sendToClient.println(msg);
			}
			return true;
		}
	

	private boolean writeFile(String fileName) 
	{
		// if Client is still connected send the message to it
		
		return true;
	}
}

	
	synchronized void remove(int id) 
	{
		// scan the array list until we found the Id
		for(int i = 0; i < connection.size(); ++i) 
		{
			ClientThread ct = connection.get(i);
			// found it
			if(ct.id == id) {
				connection.remove(i);
				return;
			}
		}
	}
		
    protected void stop() 
    {
		keepGoing = false;
		// connect to myself as Client to exit statement 
		try 
		{
			//new Socket("localhost", portnum);
		}
		catch(Exception e) {}
	}

    private synchronized void broadcast(String message,ClientThread ctr) 
	{		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = connection.size(); --i >= 0;) 
		{	
			ClientThread ct = connection.get(i);
			if(ctr.cName!=ct.cName)
			{
				// try to write to the Client if it fails remove it from the list
			    if(!ct.writeMsg(message)) 
			    {
			    	connection.remove(i);
			    	System.out.println("Disconnected Client " + ct.cName + " removed from list.");
			    }
			}
		}
	}
	
    private synchronized void broadcastFile(String fileName, int id) 
	{		
		for(int i = connection.size(); --i >= 0;) {
			if(id != i)	
			{
			ClientThread ct = connection.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeFile(fileName)) 
			{
				connection.remove(i);
				System.out.println("Disconnected Client " + ct.cName + " removed from list.");
			}
			}
		}
	}
    
    private synchronized void unicastFile(String fileName, String other) 
	{		
		for(int i = connection.size(); --i >= 0;) 
		{
			ClientThread ct = connection.get(i);
			if(other.equals(ct.cName))
			{
				if(!ct.writeFile(fileName)) 
				{
					connection.remove(i);
					System.out.println("Disconnected Client " + ct.cName + " removed from list.");
				}
			}
		}
	}
    
    private synchronized void blockcast(String message,ClientThread ctr,String other) 
	{
		for(int i = connection.size(); --i >= 0;) 
		{	
			ClientThread ct = connection.get(i);
			if(!ct.cName.equals(other))
			 {
				if(ctr.cName!=ct.cName)
				{
					if(!ct.writeMsg(message)) 
					{
						connection.remove(i);
						System.out.println("Disconnected Client " + ct.cName + " removed from list.");
					}
				}
		    }
		}
	}
    
    private synchronized void unicast(String message,String other) 
	{
		for(int i = connection.size(); --i >= 0;) 
		{	
			ClientThread ct = connection.get(i);
			if(other.equals(ct.cName))
			 {
					if(!ct.writeMsg(message)) 
					{
						connection.remove(i);
						System.out.println("Disconnected Client " + ct.cName + " removed from list.");
					}
		    }
		}
	}
	public static void main(String[] args) throws IOException {
		
		String serverReply,clientMsg,cName;
		
		String portnum =args[0];  
		Server server = new Server(portnum);
		server.start();
	}

}
