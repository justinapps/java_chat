import java.io.PrintWriter;
import java.util.*;
import java.net.*;
import java.io.*;

/************************************************************************************
HOW TO COMPILE AND RUN:
	1.) compile applet -> javac ChatApplet.java
	2.) compile --------> javac ChatServer.java
	3.) run ------------> java ChatServer

	*** In separate terminal windows or on different machines: ***
			cat chat.html
			appletviewer -J"-Djava.security.policy" chat.html
************************************************************************************/

//The server
public class ChatServer{
	public static int numOfConnections = 0;
	public static void main(String [] args) throws IOException{
		

		Buffer buffer = new Buffer();
		ServerSocket serverSocket = null;
		Consumer consumerThread = new Consumer(buffer);
		final int port = 7777;

		try{
			serverSocket = new ServerSocket(port);
		}catch(IOException e){
			System.out.println("ServerSocket error.");
			System.exit(-1);
		}

	
		consumerThread.start(); //start a consumer thread
		
		//algorithm for this loop given in lab07
		while(true){ //loop infinitely
			Socket tmpSocket = serverSocket.accept(); //accept a connection
			consumerThread.addSocket(tmpSocket);
			new Producer(tmpSocket, buffer).start(); //create a new thread of type Producer
			ChatServer.numOfConnections++;
			System.out.println("Clients: " + ChatServer.numOfConnections); //prints number of clients
		}

	} //end of main
}

class Buffer{
	//uses a list to store messages
	private LinkedList<String> messages;
	private int messageCounter;
	
	Buffer(){
		messages = new LinkedList();
		messageCounter = 0;
	}

	//appends messages to end of list
	public synchronized void insertMessage(String message){
		messages.add(message);
		messageCounter++;
		notifyAll();
	}

	//removes messages from front of list
	public synchronized String removeMessage(){
		try{
			while(isEmpty()){
				wait();
			}

		}catch (InterruptedException e){
				System.out.println("Error removing item (In buffer class).");
		}

		messageCounter--;
		String temp = messages.removeFirst();
		return temp;
	}

	public boolean isEmpty(){
		return (messageCounter <= 0);
	}
}

class Producer extends Thread{
	private Socket socket = null;
	private Buffer buffer = null;
	private String nickname;
	private BufferedReader socketIn = null;
	private boolean newUser;

	Producer(Socket s0, Buffer b0){
		socket = s0;
		buffer = b0;
	}

	public void run(){
	
		newUser = true;
		
		while(true){ //runs infinitely
			try{
				String msg;
				socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				try{
					 //disallow sending of empty msg
					if( ((msg = socketIn.readLine()) != null) && (!msg.equals("")) ){
					
						if(!newUser){ //inserts messages to buffer
							buffer.insertMessage(nickname + " says: " + msg);
						}
						else{ //deals with newly connected users
							buffer.insertMessage(msg + " just joined the chatroom...");
							nickname = msg;
							newUser = false;
						}
					}
							
				}catch(SocketException e){
							ChatServer.numOfConnections--;
							System.out.println("Clients: " + ChatServer.numOfConnections);
							buffer.insertMessage(nickname + " just left the chatroom..");
							if(ChatServer.numOfConnections == 0){
								System.exit(1);
							}
							break;
				}
				
			}catch(IOException e){System.out.println("IOException in producer thread.");}
		}
		
	}

}

class Consumer extends Thread{
	private Buffer buffer;
	private ArrayList<Socket> sockets;
	private String msg;
	private PrintWriter socketOut = null;

	Consumer(Buffer b0){ //all-args constructor
		buffer = b0;
		sockets = new ArrayList();
	}

	public void run(){
		while(true){ //runs infinitely
			try{
				if( (msg = buffer.removeMessage()) == null){
					wait(); //if there are no messages to remove, tells thread to wait
				}
				else{
					sendMessage(msg); //otherwise, calls sendMessage
				}
			}catch(InterruptedException e){System.out.println("InterruptedException in consumer thread.");}
		}
	}

	private void sendMessage(String msgToSend){
		int i = 0;
		for(Socket socket : sockets){ //distribute msg to each socket in sockets ArrayList
			try{
				socketOut = new PrintWriter(sockets.get(i).getOutputStream(), true);
				socketOut.println(msgToSend);
				i++;
			}catch(IOException e){
				System.out.println("IOException in Consumer.sendMessage.");
			}
		}
	}

	//used it in main
	//appends socket to the end of sockets arraylist
	public void addSocket(Socket newSocket){
		if(newSocket != null){
			sockets.add(newSocket);
		}
	}
}
