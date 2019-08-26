/* Author: Noah Jett
Date: 12/14/2018
For CS 330: Computer Networking with Prof. Liffiton at IWU
This is a multithreaded chat application built on a client server model
It has a text based interface, supports a group message and private messaging */

import java.net.*;
import java.io.*;
import java.util.*;


//The Client that can be run as a console
public class Client  {
	
	// notification
	private static String notif = " --- ";

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;					// socket object
	
	private String server, username;	// server and username
	private int port;					//port

	public static ArrayList <String> groupUsers;
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/*
	 *  Constructor
	 *  server: the server address
	 *  port: the port number
	 *  username: I used to have users declaring usernames from the command line, left here because it still works
	 */
	
	Client(String server, int port, String username) { 
		this.server = server;
		this.port = port;
		this.username = username;
	}
	
	
	// To start the chat 
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// exception handler if it failed
		catch(Exception ec) {
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		display(msg);
	
		// Create input output streams
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		Listener listen = new Listener();
		Thread l = new Thread(listen);
		l.start();
		// Send username to console as String. All else sent as objects
		try
		{
			sOutput.writeObject(username);
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}

	// shorthand for system.out.println
	private void display(String msg) {

		System.out.println(msg);
		
	}
	
	// send message to server
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}

	// Manually close connections
	private void disconnect() {
		try { 
			if(sInput != null) {
				sInput.close(); }
		}
		catch(Exception e) {}
		try {
			if(sOutput != null) {
				sOutput.close();
			} 
		}
		catch(Exception e) {}
        try{
			if(socket != null) {
				socket.close(); }
		}
		catch(Exception e) {}
			
	}
	/*
	 * Run the client with:
	 * java Client
	 * java Client portNumber
	 * java Client portNumber serverAddress
	 * at the console prompt
	 */
	public static void main(String[] args) {
		// default values if not entered
		int portNumber = 5555;
		String serverAddress = "localhost";
		String userName = "Anonymous";
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Enter the username: ");
		userName = scan.nextLine();			
			
 		// different case according to the length of the arguments.
		switch(args.length) {
			case 2:
				// for javac Client portNumber serverAddr
				portNumber = Integer.parseInt(args[0]);
				serverAddress = args[1];
			case 1:
				// for javac Client portNumber
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
					return;
				}
			case 0: 
				// for > java Client
				break;
			// if number of arguments are invalid
			default:
				System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
			return;
		}  
		// create the Client object
		Client client = new Client(serverAddress, portNumber, userName);
		// try to connect to the server and return if not connected
		if(!client.start())
			return;
		
		System.out.println("\nYou have successfully joined the chatroom!.");
		System.out.println("Type the message to send broadcast to all active clients");
		System.out.println("Type '@username yourmessage' to send a private message");
		System.out.println("Type 'Active' to see list of active clients");
		System.out.println("Type 'Logout' to logoff from server");
		System.out.println("Type 'Group' to be prompted to create a new group - see groups with 'Active'");
		System.out.println("Type 'GroupMessage' to begin sending message to a group");
		
		// infinite loop to get the input from the user
		while(true) {
			System.out.print("> ");
			// read message from user
			String msg = scan.nextLine();
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("Logout")) { // "Java string ignore case"
				client.sendMessage(new ChatMessage(ChatMessage.Logout, ""));
				break;
			}
			// message to check who are present in chatroom
			else if(msg.equalsIgnoreCase("Active")) {
				client.sendMessage(new ChatMessage(ChatMessage.Active, ""));				
			}
			
			else if (msg.equalsIgnoreCase("Group")) {
				System.out.println("Enter the name of your group");
				String name = scan.nextLine();
				System.out.println("Enter the members of your group separated by a space");
				String members = scan.nextLine();
				
				client.sendMessage(new ChatMessage(ChatMessage.CreateGroup, name + "$" + members));
			}
			
			else if (msg.equalsIgnoreCase("GroupMessage")) {
				//System.out.println("*********************************\n");
				client.sendMessage(new ChatMessage(ChatMessage.Active, ""));
				System.out.println("\nWhich group do you want to message?\n\n");
				String toGroup = scan.nextLine();
				System.out.println("Your message: ");
				String gmsg = scan.nextLine();
				client.sendMessage(new ChatMessage(ChatMessage.GroupMsg, toGroup + "$" + gmsg));
			}
			
			
			// regular text message
			else {
				client.sendMessage(new ChatMessage(ChatMessage.Message, msg));
			}
		}
		// close resource
		scan.close();
		// client completed its job. disconnect client.
		client.disconnect();	
	}

	/*
	 * a class that waits for the message from the server
	 */
	class Listener implements Runnable { 

		public void run() {
			while(true) {
				try {
					// read the message form the input datastream
					String msg = sInput.readObject().toString(); // (String)
					// print the message
					System.out.println(msg);
					System.out.print("> ");
				}
				catch(IOException e) {
					display(notif + "Server has closed the connection: " + e + notif);
					break;
				}
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
}