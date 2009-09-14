import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JaredReactable implements IApp {

	private JRTuioClient 				jrTuioClient;
	private JRAudioSynthesizer	jrAudioSynthesizer;

	public void start() {
		/* The heart of the system is the model of the synthesizer,
		represented by an n-ary tree where nodes are the synthesizer
		modules, and edges are the patch cables. */
		JRTree jrTree = new JRTree();
		
		// JRTuioClient listens for OSC messages and creates
		// TUIO events, which JRConnectionManager listens for.
		// When the connection manager receives a TUIO event,
		// it will update the tree accordingly.
		this.jrTuioClient = new JRTuioClient();
		JRConnectionManager jrConnectionManager = new JRConnectionManager( jrTree );
		this.jrTuioClient.addTuioListener( jrConnectionManager );
		
		/* Synthesizer shares the tree, and listens for 
		connection manager events */
		this.jrAudioSynthesizer = new JRAudioSynthesizer( jrTree );
		jrConnectionManager.addListener( this.jrAudioSynthesizer );
		
		// Start listening for OSC messages and updating the tree.
		// This starts a thread (OSCPortIn)
		this.jrTuioClient.connect();
		
		// Start "playing" the tree.
		// This starts a thread.
		Thread snythThread = new Thread( this.jrAudioSynthesizer );
		snythThread.start();
		
		// Memory info
		//System.out.println("DEBUG: JVM Memory (free/total/max) " + Runtime.getRuntime().freeMemory() + " / " + Runtime.getRuntime().totalMemory()  + " / " + Runtime.getRuntime().maxMemory());
	}

	public void shutDown() {
		System.out.println("DEBUG: JaredReactable: Shutting down ..");
		this.jrTuioClient.disconnect();
		this.jrAudioSynthesizer.halt();
		
		// Give my threads time to finish sleeping and
		// schedule some time to finish their main loops
		try { Thread.sleep(100); }
		catch (InterruptedException e) { System.err.println("Unexpected interrupt:" + e.getMessage()); }
		
		System.out.println("DEBUG: JaredReactable: Exiting ..");
	}

	public static void main ( String[] args ) {
		IApp jr = new JaredReactable();
		ShutdownInterceptor si = new ShutdownInterceptor(jr);
		Runtime.getRuntime().addShutdownHook(si);
		jr.start();
	}

}