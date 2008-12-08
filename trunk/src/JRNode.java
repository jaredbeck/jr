import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JRNode extends AudioInputStream {
	
	private static final int maximumDegree = 6;
	
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
		if ( this.getDegree() >= this.getMaximumDegree() ) {
			throw new JRInvalidEdgeException("Too many child nodes");
		}
		child.setParent( this );
		children.add(child);
	}

	public void clear ( ) {
		children.clear();
	}

	public Iterator getChildIterator ( ) {
		return children.iterator();
	}
	
	public int getDegree ( ) {
		return children.size();
	}

	public JRNode getFirstChild ( ) {
		return children.firstElement();
	}
	
	public static JRNode getInstance ( int fiducialID ) throws JRException {
		if ( fiducialID == 1 ) {
			return new JRGenerator( JROscillator.WAVEFORM_SINE );
		}
		else if ( fiducialID == 2 ) {
			return new JRGenerator( JROscillator.WAVEFORM_SQUARE );
		}
		else if ( fiducialID == 3 ) {
			return new JRGenerator( JROscillator.WAVEFORM_SAWTOOTH );
		}
		else if ( fiducialID == 4 ) {
			return new JRGenerator( JROscillator.WAVEFORM_TRIANGLE );
		}
		else if ( fiducialID == 5 ) {
			return new JRController( JROscillator.WAVEFORM_SINE );
		}
		else if ( fiducialID == 6 ) {
			return new JRController( JROscillator.WAVEFORM_HALF_SQUARE );
		}
		else if ( fiducialID == 7 ) {
			return new JRController( JROscillator.WAVEFORM_SAWTOOTH );
		}
		else if ( fiducialID == 8 ) {
			return new JRController( JROscillator.WAVEFORM_TRIANGLE );
		}
		else {
			throw new JRException( "Unknown fiducial ID in JRNode.getInstance()" );
		}
	}
	
	public int getLevel ( ) {
		if (parent == null) { return 1; }
		else { return parent.getLevel() + 1; }
	}
	
	public int getMaximumDegree ( ) {
		return JRNode.maximumDegree;
	}
	
	public boolean remove ( JRNode n ) {
		return children.remove( n );
	}
	
	public void setParent ( JRNode p ) {
		this.parent = p;
	}

}