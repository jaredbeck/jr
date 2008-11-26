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
		JRGenerator sinewaveGenerator = new JRGenerator( JRGenerator.WAVEFORM_SINE );
		JRController controller = new JRController();
		
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
		//t.dump();
		
		// Test autio output
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

		int	BUFFER_SIZE = 128000;
		byte[] abData = new byte[BUFFER_SIZE];
		while (true)
		{
			int	nRead = -1;
			try { nRead = t.getHead().read(abData); }
			catch (Exception e) { 
				e.printStackTrace(); 
				System.exit(1);
			}			
			if (nRead == -1) { break; }
			else { int	nWritten = line.write(abData, 0, nRead); }
		}
	}

}