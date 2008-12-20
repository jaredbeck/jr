/**
 * @author cramakrishnan
 *
 * Copyright (C) 2004, C. Ramakrishnan / Illposed Software
 * All rights reserved.
 * 
 * See license.txt (or license.rtf) for license information.
 * 
 * 
 * OSCPortIn is the class that listens for OSC messages.
 *	 
 * To receive OSC, you need to construct the OSCPort with a 
 *
 * An example based on com.illposed.osc.test.OSCPortTest::testReceiving() :
 
		receiver = new OSCPort(OSCPort.defaultSCOSCPort());
		OSCListener listener = new OSCListener() {
			public void acceptMessage(java.util.Date time, OSCMessage message) {
				System.out.println("Message received!");
			}
		};
		receiver.addListener("/message/receiving", listener);
		receiver.startListening();
		
 * Then, using a program such as SuperCollider or sendOSC, send a message
 * to this computer, port 57110 (defaultSCOSCPort), with the address /message/receiving
 */

package com.illposed.osc;

import java.net.*;
import java.io.IOException;
import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCPacketDispatcher;

public class OSCPortIn extends OSCPort implements Runnable {

	// state for listening
	protected boolean isListening;
	protected OSCByteArrayToJavaConverter converter = new OSCByteArrayToJavaConverter();
	protected OSCPacketDispatcher dispatcher = new OSCPacketDispatcher();
	
	/**
	 * Create an OSCPort that listens on port
	 * @param port
	 * @throws SocketException
	 */
	public OSCPortIn(int port) throws SocketException {
		System.out.println("DEBUG: OSCPortIn()");
		socket = new DatagramSocket(port);
		this.port = port;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		System.out.println("DEBUG: OSCPortIn.run() begin [while isListening]");
		byte[] buffer = new byte[1536];
		DatagramPacket packet = new DatagramPacket(buffer, 1536);
		while (isListening) {
			//System.out.println("DEBUG: OSCPortIn: is running ..");
			try {
				packet.setLength(1536);
				socket.receive(packet);
				OSCPacket oscPacket = converter.convert(buffer, packet.getLength());
				dispatcher.dispatchPacket(oscPacket);
			} catch (java.net.SocketException e) {
				if (isListening) e.printStackTrace();
			} catch (IOException e) {
				if (isListening) e.printStackTrace();
			} 
		}
		System.out.println("DEBUG: OSCPortIn.run() end");
		System.out.flush();
	}
	
	/**
	 * Start listening for incoming OSCPackets
	 */
	public void startListening() {
		System.out.println("OSCPortIn.startListening() begin");
		isListening = true;
		Thread thread = new Thread(this);
		thread.start();
		System.out.println("OSCPortIn.startListening() end");
	}
	
	/**
	 * Stop listening for incoming OSCPackets
	 */
	public void stopListening() {
		System.out.println("OSCPortIn.stopListening() [sets isListening to false]");
		isListening = false;
	}
	
	/**
	 * Am I listening for packets?
	 */
	public boolean isListening() {
		return isListening;
	}
	
	/**
	 * Register the listener for incoming OSCPackets addressed to an Address
	 * @param anAddress  the address to listen for
	 * @param listener   the object to invoke when a message comes in
	 */
	public void addListener(String anAddress, OSCListener listener) {
		dispatcher.addListener(anAddress, listener);
	}
	
	/**
	 * Close the socket and free-up resources. It's recommended that clients call
	 * this when they are done with the port.
	 */
	public void close() {
		socket.close();
	}

}
