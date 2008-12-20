import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JaredReactable {

	public static void main ( String[] args ) {
	
		/* The heart of the system is the model of the synthesizer,
		represented by an n-ary tree where nodes are the synthesizer
		modules, and edges are the patch cables. */
		JRTree jrTree = new JRTree();
	
		// JRTuioClient listens for OSC messages and creates
		// TUIO events, which JRConnectionManager listens for.
		// When the connection manager receives a TUIO event,
		// it will update the tree accordingly.
		JRTuioClient jrTuioClient = new JRTuioClient();
		JRConnectionManager jrConnectionManager = new JRConnectionManager(jrTree);
		jrTuioClient.addTuioListener(jrConnectionManager);
		
		/* Synthesizer shares the tree, and listens for 
		connection manager events */
		JRAudioSynthesizer jrAudioSynthesizer = new JRAudioSynthesizer(jrTree);
		jrConnectionManager.addListener(jrAudioSynthesizer);
		
		// Start listening for OSC messages and updating the tree.
		// This starts a thread.
		jrTuioClient.connect();
		
		// Start "playing" the tree.
		// This starts a thread.
		Thread snythThread = new Thread( jrAudioSynthesizer );
		snythThread.start();
		
	}

}