import java.io.*;
import javax.sound.sampled.*;

public class JRAudioSynthesizer implements Runnable {
	
	// not sure what is a good buffer size
	// so far, I have tried 64k and 128k with equal success
	private static final int BUFFER_SIZE = 64000;
	
	private byte[] abData;
	private JRTree jrTree;
	
	public JRAudioSynthesizer ( JRTree jrTree ) { 
		this.jrTree = jrTree;
		this.abData = new byte[BUFFER_SIZE];
	}
	
	public void run ( ) {
	
		// define audio format
		float	sampleRate = 44100.0F;
		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				 sampleRate, 16, 2, 4, sampleRate, false);
	
		// Acquire, open, and start a Line
		SourceDataLine line = null;
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
		System.out.println( "DEBUG: JRAudioSynthesizer: Line started .." );
	
		while ( true ) {
			//System.out.println( "DEBUG: JRAudioSynthesizer: is running .." );
			
			// does the tree have a head?
			JRNode head = this.jrTree.getHead();
			
			// if so, try to read from the head node
			if ( head != null ) {
				int	nRead = -1;
				try { nRead = head.read(abData); }
				catch (Exception e) { 
					e.printStackTrace(); 
					System.exit(1);
				}			
	
				// -1 indicates end of input
				if (nRead == -1) { break; }
				
				// otherwise, try to process the input and write to the line
				else {
	
					// Assertion: assuming 16bit sample size (two bytes) we can assert an even length buffer
					if (nRead % 2 != 0) { 	
						System.err.println("ERROR: Assertion failed: odd length buffer"); 
						System.exit(1);
					}
				
					//System.out.println(nRead + " read from head");
				
					// swap byte order to little endian, because Jared's hardware is little endian
					for ( int i = 0; i < nRead; i += 2) {
						byte mostSigByte = abData[i];
						abData[i] = abData[i+1];
						abData[i+1] = mostSigByte;
					}
					
					// write to the line
					int	nWritten = line.write(abData, 0, nRead); 
					//System.out.println(nWritten + " written to line");
				}
				
			} // end if ( head != null )
			
			// Because there is no head yet, we sleep() instead of yield()ing.  
			// This massively reduces CPU useage.
			try { Thread.sleep(33); }
			catch (InterruptedException e) { 
				System.err.println("JRAudioSynthesizer: Caught unexpected interrupt:" + e.getMessage());
			}
		}
		
		// release resources
		line.flush();
		line.close();
	}
	
}