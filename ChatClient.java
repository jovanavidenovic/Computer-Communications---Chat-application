import java.io.*;
import java.net.*;
//import json.jar;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.util.*;


@SuppressWarnings("unchecked")

public class ChatClient extends Thread
{
	protected int serverPort = 1234;
	Scanner scan = new Scanner (System.in);

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public ChatClient() throws Exception {
	
		JSONObject poruka = new JSONObject();
		
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println();
		System.out.println();
		

		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			socket = new Socket("localhost", serverPort); // create socket connection
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");
				
			System.out.print("Please enter your username: ");
			String imeUporabnika = std_in.readLine();	
			
			poruka.put("tip", "login");	
			poruka.put("ime", imeUporabnika);
			
			this.sendMessage(poruka.toString(), out);
			
			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		System.out.println("\nPlease, enter the name of a recipient: \n" + "(If you want to send your message to all users write EVERYONE)");
		boolean prviPut = true;
	
		while (true) {
		
		if(!prviPut){
			System.out.println("If you want to send another message, write YES.");
			String odgovor = scan.nextLine();
			odgovor = odgovor.toUpperCase();
			if(!(odgovor.equals("YES"))){
				System.out.println("OK, bye!");
				break;
			}
			System.out.println("Please, enter the name of a recipient: \n" + "(If you want to send your message to all users write EVERYONE)");
		} else prviPut = false;
	
		
		String prejemnik = scan.nextLine();
		
		if(prejemnik.equals("EVERYONE")){
			poruka.put("tip", "javna");
			poruka.put("prejemnik", "ALL");
		} else {
			poruka.put("tip", "privatna");
			poruka.put("prejemnik", prejemnik);
			//System.out.println("Prejemnik je: " + poruka.get("prejemnik"));
		}
					
		System.out.println("\nPlease, enter your message:");
		// read from STDIN and send messages to the chat server
		
		
		String userInput = std_in.readLine();
		//while ((userInput = std_in.readLine()) != null) { // read a line from the console
		poruka.put("vsebina", userInput);
		this.sendMessage(poruka.toString(), out); // send the message to the chat server
		//}

		}
		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendMessage(String message, DataOutputStream out) {
		try {
			out.writeUTF(message); // writeUTF		send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
				System.out.println("[RKchat] " + message); // print the message to the console
				
				//////////////////////////////////////////////
				
				System.out.println("\nPlease, enter the name of a recipient: \n" 
				+ "(If you want to send your message to all users write EVERYONE)\nIf you have previously sent messages, firstly enter YES.\n");
			}
		} catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
