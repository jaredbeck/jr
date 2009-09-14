import java.io.*;
import javax.sound.sampled.*;

public class JRAudioSynthesizer implements Runnable, JRConManListener {
	
	// not sure what is a good buffer size
	// so far, I have tried 64k and 128k with equal success
	private static final int BUFFER_SIZE = 64000;
	private byte[] abData;
	
	// Tree is a model of synth modules and patch cables
	private JRTree jrTree;
	
	// System audio line
	private SourceDataLine line;
	
	// Constructor
	public JRAudioSynthesizer ( JRTree jrTree ) { 
		this.jrTree = jrTree;
		this.abData = new byte[BUFFER_SIZE];
		line = null; // line will be acquired and opened in run()
	}
		
	/* refresh()
	Connection manager event. The tree has been rebuilt.  Data
	currently in the line buffer is now stale.  If we continue to
	play stale data, the reactable will appear laggy, or 
	unresponsive.  So, we must refresh the data in the line.  
	
	We could just flush the line, and wait for run() to refill it, but
	this causes a clicking noise, presumably because there are a few
	milliseconds with no data? From the javadocs:  "It is legal to flush
	a line that is not stopped, but doing so on an active line is likely
	to cause a discontinuity in the data, resulting in a perceptible
	click." */
	public void refresh ( ) {
		//System.out.println( "DEBUG: JRAudioSynthesizer.refresh() begin" );
		
		/* Prepare fresh data.  (See run() for more comments on this stuff) */
		JRNode head = this.jrTree.getHead();
		if ( head != null ) {
		
			int	nRead = readFromNode( head );
			if (nRead != -1) {
				//System.out.println("IO: " + nRead + " read from head");
				swapByteOrder( nRead );				

				/* Stop, Flush, Write, Start 
				Hopefully quickly enough to avoid a click. */
				line.stop();
				line.flush();
				writeToLine( nRead );
				line.start();
			}
			else {
				System.out.println( "DEBUG: JRAudioSynthesizer.refresh(): nRead == -1" );
			}
				
		}
		else {
			/* The tree is now empty. */
			//System.out.println( "DEBUG: JRAudioSynthesizer.refresh(): head == null" );
			line.stop();
			line.flush();
		}
		
		//System.out.println( "DEBUG: JRAudioSynthesizer.refresh() end" );
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
	
		// Acquire, open, and start a Line
		DataLine.Info	info = new DataLine.Info( SourceDataLine.class, audioFormat );
		try
		{
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
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
		line.start();
		//System.out.println( "DEBUG: JRAudioSynthesizer: Line started .." );
	
		while ( true ) {
			//System.out.println( "DEBUG: JRAudioSynthesizer: is running .." );
			
			// does the tree have a head?
			JRNode head = this.jrTree.getHead();
			
			// if so, try to read from the head node
			if ( head != null ) {
				int	nRead = readFromNode( head );
	
				// -1 indicates end of input.  This is unexpected and
				// is an error, so we exit the main loop, and shut down.
				if (nRead == -1) { break; }
				else {
					//System.out.println("IO: " + nRead + " read from head");
				
					// swap byte order to little endian, 
					// because Jared's hardware is little endian
					swapByteOrder( nRead );
					
					// write to the line
					writeToLine( nRead );
					
					// start the line if it is not active
					if ( ! line.isActive() ) { line.start(); }
				}
				
			} // end if ( head != null )
			
			// Because there is no head yet, we sleep() instead of yield()ing.  
			// This massively reduces CPU useage.
			try { Thread.sleep(33); }
			catch (InterruptedException e) { 
				System.err.println("JRAudioSynthesizer: Caught unexpected interrupt:" + e.getMessage());
			}
		}
		
		// cease I/O activity
		line.stop();
		
		// Flushes queued data from the line. The flushed data is discarded.
		line.flush();
		
		/* Close the line, releasing system resources */
		line.close();
	}

	
	private void swapByteOrder ( int bufferLength ) {
		for ( int i = 0; i < bufferLength; i += 2) {
			byte mostSigByte = abData[i];
			abData[i] = abData[i+1];
			abData[i+1] = mostSigByte;
		}
	}

	
	private int writeToLine ( int length ) {
		int	nWritten = line.write(abData, 0, length);
		//System.out.println("IO: " + nWritten + " written to line");
		//System.out.println("IO: Line buffer length " + line.available() + " / " + line.getBufferSize());		
		return nWritten;
	}
	
}