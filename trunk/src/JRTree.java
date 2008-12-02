import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JRTree {
	
	private JRNode head;
	
	public JRTree ( ) {
	}
	
	public JRTree ( JRNode h ) {
		setHead( h );
	}
	
	public void dump ( ) {
		System.out.println( "Dumping tree with preorder traversal .." );
		this.dump( this.head, 0, "preorder" );
		System.out.println( "Dumping tree with postorder traversal .." );
		this.dump( this.head, 0, "postorder" );
	}
	
	public void dump ( JRNode n, int level, String order ) {
		// preorder output
		if ( order.equals("preorder") ) { 
			for ( int t = 0; t < level; t++ ) { System.out.print( "  " ); }
			System.out.println( n.toString() ); 
		}
		
		// recursion logic
		if ( n.getDegree() > 0 ) {
			Iterator i = n.getChildren();
			while ( i.hasNext() ) {
				dump( (JRNode)i.next(), level + 1, order );
			}
		}
		
		// postorder output
		if ( order.equals("postorder") ) { 
			for ( int l = 1; l < n.getLevel(); l++ ) { System.out.print( "  " ); }
			System.out.println( n.toString() ); 
		} 
	}
	
	public JRNode getHead ( ) {
		return this.head;
	}
	
	public void setHead ( JRNode h ) {
		this.head = h;
	}

	public static void main ( String[] args ) {
		
		// define audio format
		float	sampleRate = 44100.0F;
		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				 sampleRate, 16, 2, 4, sampleRate, false);
		
		// Define nodes
		JRGenerator sinewaveGenerator = new JRGenerator( JROscillator.WAVEFORM_SINE );
		JRController controller = new JRController( JROscillator.WAVEFORM_HALF_SQUARE, 2.0F );
		
		// Construct tree
		try {
			sinewaveGenerator.addChild( controller );
		}
		catch (JRInvalidEdgeException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		JRTree t = new JRTree( sinewaveGenerator );
		
		// Test traversals, writing to console
		// t.dump();
		
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
		//System.out.println( "Line started .." );

		// arbitrarily sized buffer
		int	BUFFER_SIZE = 128000;
		byte[] abData = new byte[BUFFER_SIZE];
		
		// try to read from the head node
		while (true)
		{
			int	nRead = -1;
			try { nRead = t.getHead().read(abData); }
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
					System.err.println("Assertion failed: odd length buffer"); 
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
		}
	}

}