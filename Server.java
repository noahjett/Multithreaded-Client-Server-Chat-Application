/* Author: Noah Jett
Date: 12/14/2018
For CS 330: Computer Networking with Prof. Liffiton at IWU
This is a multithreaded chat application built on a client server model
It has a text based interface, supports a group message and private messaging */

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {

	private static int uniqueId;	
	private static ArrayList<Handler> users; // Stores active users, arraylist storing handler objects	
	//public static ArrayList <ArrayList<String>> groupList = new ArrayList<ArrayList<String>>();
	private static HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
	private SimpleDateFormat sdf; // For timestamp	
	private int port; // port number	
	private boolean Continue;	
	private String notif = " --- "; // notification
	
	// constructor
	public Server(int port) {
		this.port = port; // assign port
		sdf = new SimpleDateFormat("HH:mm:ss"); // to display time in "HH:mm:ss" format
		users = new ArrayList<Handler>(); // instantiate our array list
	}
	
	public void Start() {
		Continue = true;
		//create socket server and wait for connection requests 
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections ( till server is active )
			while(Continue) 
			{
				display("Server waiting for Clients on port " + port + ".");
				
				// accept connection if requested from client
				Socket socket = serverSocket.accept();
				// break if server stoped
				if(!Continue) {
					break; }
				// if client is connected, create its thread
				Handler h = new Handler(socket);
				//add this client to arraylist
				users.add(h);
				Thread t = new Thread(h);
				t.start();
			}
			// try to stop the server
			try {
				serverSocket.close();
				for(int i = 0; i < users.size(); ++i) {
					Handler h = users.get(i);
					try {
					// close all data streams and socket
					h.sInput.close();
					h.sOutput.close();
					h.socket.close();
					}
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}
	

	
	// Display an event to the console
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// To send a message to other connected clients
	private synchronized boolean broadcast(String message) {
		// add timestamp to the message
		String time = sdf.format(new Date());
		
		// to check if message is private i.e. client to client message
		String[] who = message.split(" ",3);
		boolean isPrivate =  false;
		

		if(who[1].charAt(0)=='@') 
			isPrivate = true;
		
		// if private message, send message to mentioned username only
		if(isPrivate==true)
		{
			String receiver=who[1].substring(1, who[1].length());
			message=who[0]+ " /private/ " + who[2];
			String messagef = time + " " + message + "\n";
			boolean found=false;
			// we loop in reverse order to find the mentioned username
			for(int y=users.size(); --y>= 0;) // pre decrement because post causes out of bounds error when only two users connected
			{
				Handler h1=users.get(y);
				String check=h1.getUsername();
				if(check.equals(receiver))
				{
					// try to write to the Client if it fails remove it from the list
					if(!h1.writeMsg(messagef)) {
						users.remove(y);
						display("Disconnected Client " + h1.username + " removed from list.");
					}
					// username found and delivered the message
					found=true;
					break;
				}				
				
			}

			// mentioned user not found, return false
			if(found!=true)
			{
				return false; 
			}
		}
		// if message is a broadcast message
		else
		{
			String messagef = time + " " + message + "\n";
			// display message
			System.out.print(messagef);
			
			// we loop in reverse order in case we would have to remove a Client
			// because it has disconnected
			for(int i = users.size(); --i >= 0;) {
				Handler h2 = users.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!h2.writeMsg(messagef)) {
					users.remove(i);
					display("Disconnected Client " + h2.username + " removed from list.");
				}
			}
		}
		return true;
		
		
	}


	
	// if client sent "logout" message to exit
	synchronized void remove(int id) {
		
		String disconnectedClient = "";
		// scan the array list until we found the Id
		for(int i = 0; i < users.size(); ++i) {
			Handler ct = users.get(i);
			// if found remove it
			if(ct.id == id) {
				disconnectedClient = ct.getUsername();
				users.remove(i);
				break;
			}
		}
		broadcast(notif + disconnectedClient + " has left the chat room." + notif);
	}
	
	/*
	 *  To run as a console application
	 *  java Server
	 *  java Server portNumber
	 *  If the port number is not specified 5555 is used
	 */ 
	public static void main(String[] args) {
		// start server on port 5555 unless a PortNumber is specified 
		int portNumber = 5555;
		if (args.length > 0) {
			try {
				portNumber = Integer.parseInt(args[0]);
			}
			catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is java Server [portNumber]");
				return;
			}
		}	
		
		// create a server object and start it
		Server server = new Server(portNumber);
		server.Start();
	}

	// One instance of this thread will run for each client
	class Handler implements Runnable { 
		Socket socket; // the socket to get messages from client
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		int id;
		String username;
		ChatMessage cm; // message object to recieve message and its type
		String date; // for timestamp

		// Constructor
		Handler(Socket socket) {
			id = ++uniqueId;
			this.socket = socket;
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// Creating both datastreams
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				
				// read the username
				username = sInput.readObject().toString(); // (String) sInput
				for(int i = 0; i < users.size(); ++i) {
					Handler ct = users.get(i);
					// if found remove it
					if(ct.username == username) {
						username += "1";
						
			}
		}
				
				broadcast(notif + username + " has joined the chat room." + notif);
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		// infinite loop to read and forward message
		public void run() {
			// to loop until LOGOUT
			boolean Continue = true;
			while(Continue) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject(); // Called class casting https://stackoverflow.com/questions/29017233/java-incompatible-types-object-cannot-be-converted-to-my-type
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// get the message from the ChatMessage object received
				String message = cm.getMessage();
		
				// different actions based on type message
				
				if (cm.getType() == 1) {
					broadcast(username + ": " + message);

				}
				if (cm.getType() == 2) {
					display(username + " disconnected.");
					Continue = false;
					break;					
				}
				if (cm.getType() == 3) {
					String[] parse = message.split("\\$",2);
					String groupName = parse[0];
					
					String inGroup = parse[1];
					ArrayList<String> members = new ArrayList<String>(Arrays.asList(inGroup.split(" "))); //https://stackoverflow.com/questions/7347856/how-to-convert-a-string-into-an-arraylist

					map.put(groupName, members);
					//System.out.println(map.get(groupName));
					//System.out.println(map.keySet());

					broadcast("Creating a group: " + groupName + " " + "-- Members: " + inGroup);

					
				}
				
				if (cm.getType() == 4) {
					String[] parse = message.split("\\$", 2);
					String receiver = parse[0];
					String msg = parse[1];
					//String gmsg = msg.substring(1,msg.length());
					//writeMsg("********" + receiver + " " + gmsg);
					
					for (Map.Entry<String, ArrayList<String>> entry: map.entrySet()) {
						String key = entry.getKey();
						if (receiver.equals(key)) {
							ArrayList<String> getter = entry.getValue();
							for (String s: getter) {
								//writeMsg(username + ": " + "@" +s + " " +  message);
								broadcast(username + "_" + receiver + ": " + "@" +s + " " +  msg);
							}
						}
					}
				}
				if (cm.getType() == 0) {
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					// send list of active clients
					for(int i = 0; i < users.size(); ++i) {
						Handler ct = users.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					int i = 0;
					for (Map.Entry<String, ArrayList<String>> entry: map.entrySet()) { // method for iterating maps taken directly from here https://stackoverflow.com/questions/5826384/java-iteration-through-a-hashmap-which-is-more-efficient
						
						String key = entry.getKey();
						ArrayList<String> getter = entry.getValue();
						//System.out.println(getter);
						writeMsg("Group: " + (i+1) + ") " + key);
						i++;
					}
				}
			}
			// if out of the loop then disconnected and remove from client list
			remove(id);
			close();
		}
		
		// close everything
		private void close() {
			try {
				if(sOutput != null) {
					sOutput.close();
				} 
			}
			catch(Exception e) {}
			try {
				if(sInput != null) {
					sInput.close();
				} 
			}
			catch(Exception e) {};
			try {
				if(socket != null) {
					socket.close();
				} 
			}
			catch (Exception e) {}
		}

		// write a String to the Client output stream
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display(notif + "Error sending message to " + username + notif);
				display(e.toString());
			}
			return true;
		}
		

	}
}

		// Class for defining the types of messages, necessary since transferring object instead of data stream
		class ChatMessage implements Serializable{ // Serializable
			// Active: Shows all connected users
			// Message: Sends message, either broadcast or private
			// Logout: disconnects client from server
			// Group: creates new group
			// GroupMsg: sends a message to group
			static final int Active = 0, Message = 1, Logout = 2, CreateGroup = 3, GroupMsg = 4;
			private int type;
			private String message;
			
			// constructor
			ChatMessage(int type, String message) {
				this.type = type;
				this.message = message;
			}
			

			int getType() {
				return type;
			}

			String getMessage() {
				return message;
			}
			
/* 			String getUsers() {
				for (int i = 0; i < groupUsers.size(); i++) {
					return groupUsers[i];
				}
			} */
	}
//}