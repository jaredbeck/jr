import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JRNode extends AudioInputStream {
	
	// Define audio format
	private static final float sampleRate = 44100.0F;
	private static final int sampleSizeInBits = 16;
	private static final int channels = 2;
	private static final int frameSizeInBytes = 4;
	private static final float frameRate = 44100.0F;
	private static final boolean bigEndian = true;
	
	protected JRNode parent;
	protected Vector<JRNode> children;
	protected AudioFormat audioFormat;
	
	public JRNode ( ) {
		/* Convenience constructor uses default audio format defined above */
		super(new ByteArrayInputStream(new byte[0]),
			new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, 
				frameSizeInBytes, frameRate, bigEndian),
				AudioSystem.NOT_SPECIFIED);
		this.audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, 
				frameSizeInBytes, frameRate, bigEndian);
		children = new Vector<JRNode>();
	}
	
	public JRNode ( AudioFormat audioFormat ) {
		super(new ByteArrayInputStream(new byte[0]),
		      audioFormat,
		      AudioSystem.NOT_SPECIFIED);
		this.audioFormat = audioFormat;
		children = new Vector<JRNode>();
	}
	
	public void addChild ( JRNode child ) throws JRInvalidEdgeException {
		child.setParent( this );
		children.add(child);
	}

	public void clear ( ) {
		children.clear();
	}

	public Iterator getChildren ( ) {
		return children.iterator();
	}
	
	public int getDegree ( ) {
		return children.size();
	}

	public JRNode getFirstChild ( ) {
		return children.firstElement();
	}
	
	public int getLevel ( ) {
		if (parent == null) { return 1; }
		else { return parent.getLevel() + 1; }
	}
	
	public boolean remove ( JRNode n ) {
		return children.remove( n );
	}
	
	public void setParent ( JRNode p ) {
		this.parent = p;
	}

}