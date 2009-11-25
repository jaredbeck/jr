import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JaredReactable implements IApp {

	private JRTuioClient 				jrTuioClient;
	private JRAudioSynthesizer	jrAudioSynthesizer;
	private Thread 							snythThread;
	private JRTree 							jrTree;
	private JRConnectionManager jrConnectionManager;

	public void start() {
		/* The heart of the system is the model of the synthesizer,
		represented by an n-ary tree where nodes are the synthesizer
		modules, and edges are the patch cables. */
		this.jrTree = new JRTree();
		
		// JRTuioClient listens for OSC messages and creates
		// TUIO events, which JRConnectionManager listens for.
		// When the connection manager receives a TUIO event,
		// it will update the tree accordingly.
		this.jrTuioClient = new JRTuioClient();
		this.jrConnectionManager = new JRConnectionManager( this.jrTree );
		this.jrTuioClient.addTuioListener( jrConnectionManager );
		
		/* Synthesizer shares the tree, and listens for 
		connection manager events */
		this.jrAudioSynthesizer = new JRAudioSynthesizer( this.jrTree );
		this.jrConnectionManager.addListener( this.jrAudioSynthesizer );
		
		// Start listening for OSC messages and updating the tree.
		// This starts a thread (OSCPortIn)
		this.jrTuioClient.connect();
		
		// Start "playing" the tree.
		// This starts a thread.
		this.snythThread = new Thread( this.jrAudioSynthesizer, "JR-AudioSynthesizer" );
		this.snythThread.start();
		
		// Memory info
		//System.out.println("DEBUG: JVM Memory (free/total/max) " + Runtime.getRuntime().freeMemory() + " / " + Runtime.getRuntime().totalMemory()  + " / " + Runtime.getRuntime().maxMemory());
	}

	public void shutDown() {
		System.out.println("DEBUG: JaredReactable: Shutting down ..");
		
		this.jrAudioSynthesizer.stopSynth();
		//this.jrTuioClient.disconnect();
		
		try { Thread.sleep(100); }
		catch (InterruptedException e) { 
			System.err.println("JRAudioSynthesizer: Caught unexpected interrupt:" + e.getMessage());
		}
		
		System.out.println("DEBUG: JaredReactable: Thread.activeCount() = " + Thread.activeCount());
		//Thread.currentThread().getThreadGroup().list();
		
		// wait for synth to exit
		int safety = 10;
		System.out.println( this.snythThread == null );
		while (this.snythThread.isAlive() && safety > 0) {
			try { Thread.sleep(500); }
			catch (InterruptedException e) { 
				System.err.println("JRAudioSynthesizer: Caught unexpected interrupt:" + e.getMessage());
			}
			System.out.println("DEBUG: safety = " + safety);
			safety --;
		}
		
		System.out.println("EXIT: JaredReactable: Exiting ..");
	}

	public static void main ( String[] args ) {
		IApp jr = new JaredReactable();
		ShutdownInterceptor si = new ShutdownInterceptor(jr);
		Runtime.getRuntime().addShutdownHook(si);
		jr.start();
	}

}