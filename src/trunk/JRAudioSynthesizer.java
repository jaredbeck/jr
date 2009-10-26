import java.io.*;
import javax.sound.sampled.*;

public class JRAudioSynthesizer implements Runnable, JRConManListener {

	// Buffer size
	// Must be multiple of frame size
	// 8k seems good
	private static final int BUFFER_SIZE = 8000;
	private byte[] abData;
	
	// Line buffer size
	// The smaller the better, so that less stale data accumulates
	private static final int LINE_BUFFER_SIZE = 16000;
	
	// Tree is a model of synth modules and patch cables
	private JRTree jrTree;
	
	// System audio line
	private SourceDataLine line;
	
	// Controls
	private boolean stopSynth;
	
	// Constructor
	public JRAudioSynthesizer ( JRTree jrTree ) { 
		this.jrTree = jrTree;
		this.abData = new byte[BUFFER_SIZE];
		line = null; // line will be acquired and opened in run()
		stopSynth = false;
	}
	
	// halt() - cease execution.  causes run() to shut down
	public void stopSynth() {
		synchronized(this) {
			System.out.println("DEBUG: JRAudioSynthesizer: stopSynth()");
			this.stopSynth = true;
		}
	}
	

	public void refresh ( ) {
		/* If we use a small line buffer, then the line does not accumulate too much
		stale data, and the refresh event becomes unnecessary. Besides, the javadocs
		say that flushing an active line can cause clicking noises.  
		-Jared 4/9/2009 */
	}
	
	
	private int readFromNode ( JRNode node ) {
		int	nRead = -1;
		try { nRead = node.read ( this.abData ); }
		catch (Exception e) { 
			e.printStackTrace(); 
			System.exit(1);
		}
		
		/* Sanity check: If data was read, the buffer should have
		an even length.  (assuming 16bit (two byte) sample size) */
		if ( nRead != -1 && nRead % 2 != 0) { 	
			System.err.println("ERROR: Assertion failed: odd length buffer"); 
			System.exit(1);
		}
		
		return nRead;
	}
	
	public void run ( ) {
	
		// define audio format
		float	sampleRate = 44100.0F;
		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				 sampleRate, 16, 2, 4, sampleRate, false);
	
		System.out.println("DEBUG: JRAudioSynthesizer: Thread.activeCount() = " + Thread.activeCount());
		Thread.currentThread().getThreadGroup().list();
	
		// Acquire, open, and start a Line
		DataLine.Info	info = new DataLine.Info( SourceDataLine.class, audioFormat, LINE_BUFFER_SIZE );
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat,LINE_BUFFER_SIZE);
		}
		catch (LineUnavailableException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		//System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		
		/*line.start();
		System.out.println( "DEBUG: JRAudioSynthesizer: Line started .." );*/
	
		while ( !stopSynth ) {
			System.out.println( "DEBUG: JRAudioSynthesizer: is running (stopSynth = " + this.stopSynth + ") .." );
			
			// does the tree have a head?
			JRNode head = this.jrTree.getHead();
			
			// if so, try to read from the head node
			if ( head != null ) {
				int	nRead = readFromNode( head );
	
				// -1 indicates end of input
				if (nRead == -1) {
					System.err.println("foobar");
					System.exit(1);
					/*try { Thread.sleep(33); }
					catch (InterruptedException e) { 
						System.err.println("JRAudioSynthesizer: Unexpected InterruptedException:" + e.getMessage());
						break;
					}	*/
				}
				else {
					System.out.println("IO: " + nRead + " read from head");
				
					// swap byte order to little endian, 
					// because Jared's hardware is little endian
					swapByteOrder( nRead );
					
					// write to the line
					writeToLine( nRead );
					
					// start the line if it is not active
					if ( ! line.isRunning() ) { 
						System.out.println( "DEBUG: JRAudioSynthesizer: Line started (was not running)" );
						line.start(); 
						}
				}
				
			} // end if ( head != null )
			
			// Because there is no head yet, we sleep() 
			// Note that sleep()ing instead of yield()ing massively reduces CPU useage
			try { Thread.sleep(33); }
			catch (InterruptedException e) { 
				System.err.println("JRAudioSynthesizer: Caught unexpected interrupt:" + e.getMessage());
				break;
			}
			
			synchronized(this) {
			} // lock
		} // while
		
	
		/*System.out.println("DEBUG: JRAudioSynthesizer: Releasing resources: Draining line .. ");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		System.out.flush();
		line.drain();*/

		System.out.println("DEBUG: JRAudioSynthesizer: Releasing resources: Stopping line .. ");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		line.stop();

		System.out.println("DEBUG: JRAudioSynthesizer: Releasing resources: Flushing line .. ");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		line.flush();

		/*System.out.println("DEBUG: JRAudioSynthesizer: Releasing resources: Closing line .. ");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		line.close();
		line = null;*/
		
		System.out.println("DEBUG: JRAudioSynthesizer: about to exit .. ");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		System.out.println("EXIT: JRAudioSynthesizer: Exiting ..");
	}

	
	private void swapByteOrder ( int bufferLength ) {
		for ( int i = 0; i < bufferLength; i += 2) {
			byte mostSigByte = abData[i];
			abData[i] = abData[i+1];
			abData[i+1] = mostSigByte;
		}
	}

	
	private int writeToLine ( int length ) {
		System.out.println("DEBUG: JRAudioSynthesizer: begin writeToLine(" + length + ")");
		System.out.println("LINE: open = " + line.isOpen() + ", active = " + line.isActive() + ", running = " + line.isRunning());
		System.out.println("IO: Line buffer available " + line.available() + " / " + line.getBufferSize());		
		int	nWritten = line.write(abData, 0, length);
		System.out.println("IO: " + nWritten + " written to line");
		System.out.flush();
		return nWritten;
	}
	
}