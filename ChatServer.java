import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
//import java.json.*;

@SuppressWarnings("unchecked")


public class ChatServer {

	protected int serverPort = 1234;
	protected List<Socket> clients = new ArrayList<Socket>(); // list of clients
	public HashMap<Socket, String> uporabniki = new HashMap<Socket, String>(); //mapa kljuceva in uporabnika
	
	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		Iterator<Socket> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			Socket socket = (Socket) i.next(); // get the socket for communicating with this client
			//System.out.println(socket.toString() + " " + this.uporabniki.get(socket));
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
				out.writeUTF(message); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}
	
	//send a message to a specific client connected to the server
	public void sendToClient(String message, Socket prejemnik) throws Exception {
		try {
			DataOutputStream out = new DataOutputStream(prejemnik.getOutputStream()); // create output stream for sending messages to the client
			out.writeUTF(message); // send message to the client
		} catch (Exception e) {
			System.err.println("[system] could not send message to a client");
			e.printStackTrace(System.err);
		}			
	}

	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}
	
	public void addClient(Socket socket){
		synchronized(this){
		clients.add(socket);
		}
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
	//public HashMap<Socket, String> uporabniki = new HashMap<Socket, String>(); //mapa kljuceva in uporabnika
	//public List<Socket> klijenti = new ArrayList<Socket>(); // list of clients
	
	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	public void run() {
		System.out.println("[system] connected with client " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());
		
		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			JSONObject poruka;
			JSONParser razdeli = new JSONParser();
		
			try {
				msg_received = in.readUTF(); // read the message from the client
				if (msg_received.length() == 0) // invalid message
				continue;
				
				poruka = (JSONObject) razdeli.parse(msg_received);
				String tipPoruke = poruka.get("tip").toString();
				
				
				//PRIJAVA U SISTEM ///////////////////////////////////////////////////////////
				if(tipPoruke.equals("login")){
					System.out.println(poruka.get("ime").toString() + " (" + this.socket.toString() + ") has successfully signed in.");
					this.server.uporabniki.put(this.socket, poruka.get("ime").toString() ); //unesi u bazu, da se sa tog porta, prijavio sa tim imenom
					
					/*if(this.server.clients.indexOf(this.socket) != -1) //ce ze ne obstaja
						this.server.addClient(this.socket);*/
					
					//System.out.println(this.socket.toString() + " " + this.server.uporabniki.get(this.socket));
				}
				//////////////////////////////////////////////////////////////////////////////
				
				
				//JAVNA PORUKA///////////////////////////////////////////////////////////////
				if(tipPoruke.equals("javna")){
					
					//System.out.println("PUBLIC MESSAGE");
					
					System.out.println("[RKchat] [from: " + this.server.uporabniki.get(this.socket) + " (" + this.socket.getPort() + ")" + 
					" to: EVERYONE] : " + poruka.get("vsebina").toString().toUpperCase()); // print the incoming message in the console
					
					String msg_send = this.server.uporabniki.get(this.socket) + " said to everyone: " + poruka.get("vsebina").toString().toUpperCase(); // TODO		
				
					try {
					this.server.sendToAllClients(msg_send); // send message to all clients
					} catch (Exception e) {
					System.err.println("[system] there was a problem while sending the message to all clients");
					e.printStackTrace(System.err);
					continue;
					}
				}
				//////////////////////////////////////////////////////////////////////////////
			
			
			
				//PRIVATNA PORUKA/////////////////////////////////////////////////////////////
				if(tipPoruke.equals("privatna")){
					
					//System.out.println("PRIVATE MESSAGE");
					
					//pronadji socket prejemnika
					String prejemnik = poruka.get("prejemnik").toString();
					Socket sPrejemnika = null;
					
					Iterator<Socket> i = this.server.clients.iterator();
					//int brojac = 0;
					
					while (i.hasNext()) { // iterate through the client list
						//brojac++;
						//System.out.println("Iteracija: " + brojac);
						Socket socket = (Socket) i.next(); // get the socket for communicating with this client
						//System.out.println(socket.toString());
						if(this.server.uporabniki.get(socket)!=null){
							//System.out.println(this.server.uporabniki.get(socket));
							if(this.server.uporabniki.get(socket).equals(prejemnik)){
							sPrejemnika = socket;
							}
						}
						
					}
					
					
					if(sPrejemnika != null){
						
						System.out.println("[RKchat] [from: " + this.server.uporabniki.get(this.socket) + " (" + this.socket.getPort() + ")" + 
						" to: " + prejemnik  + " (" + sPrejemnika.getPort() + ")] : " + poruka.get("vsebina").toString().toUpperCase()); // print the incoming message in the console
					
						String msg_send = this.server.uporabniki.get(this.socket) + " said to you: " + poruka.get("vsebina").toString().toUpperCase(); // TODO		
						
						try {
						this.server.sendToClient(msg_send, sPrejemnika); // send message to all clients
						} catch (Exception e) {
						System.err.println("[system] there was a problem while sending the message to all clients");
						e.printStackTrace(System.err);
						continue;
						}
					} else {
						this.server.sendToClient("User " + prejemnik + " does not exist. Try again with different username.\n", this.socket); 
					}
				}
				//////////////////////////////////////////////////////////////////////////////
				
			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

		
		}
	}
}
