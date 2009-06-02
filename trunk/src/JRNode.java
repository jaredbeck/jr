import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public abstract class JRNode extends AudioInputStream {
	
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
	protected float xpos, ypos;
	
	private long sessionID;
	
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
		if ( this.getNumAudioOutputs() + this.getNumControlOutputs() != 1 ) {
			System.err.println( "Invalid JRNode: A node may only have one output" );
			System.exit(1);
		}
	}
	
	public void addChild ( JRNode child ) throws JRInvalidEdgeException {
		if ( child.getNumAudioOutputs() + child.getNumControlOutputs() != 1 ) {
			System.err.println( "Invalid child: A node may only have one output" );
			System.exit(1);
		}
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
	
	public float getDistance(JRNode n) {
		float dx = this.xpos-n.getX();
		float dy = this.ypos-n.getY();
		return (float)Math.sqrt(dx*dx+dy*dy);
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
	
	public abstract int getNumAudioOutputs ( );
	public abstract int getNumAudioInputs ( );
	public abstract int getNumControlOutputs ( );
	public abstract int getNumControlInputs ( );
	
	public int getNumSatisfiedAudioInputs ( ) {
		int numSatisfied = 0;
		Iterator i = this.getChildIterator();
		while ( i.hasNext() ) {
			JRNode child = (JRNode)i.next();
			numSatisfied += child.getNumAudioOutputs();
		}
		return numSatisfied;
	}
	
	public String getOutputType ( ) throws JRException {
		if ( this.getNumAudioOutputs() > 0 ) { return "audio"; }
		else if ( this.getNumControlOutputs() > 0 ) { return "control"; }
		else { throw new JRException( "Unreckognized node output type" ); }
	}
	
	public long getSessionID ( ) {
		return this.sessionID;
	}

	public float getX() { return xpos; }
	public float getY() { return ypos; }	

	public boolean hasInputType ( String inputType ) {
		boolean result = false;
		if ( inputType.equals("audio") ) {
			result = this.getNumAudioInputs() > 0;
		}
		else if ( inputType.equals("control") ) {
			result = this.getNumControlInputs() > 0;
		}
		return result;
	}
	
	public boolean hasParent ( ) {
		return this.parent != null;
	}

	/* An object is input-satisfied when it can produce an output. In
	the case of a filter, which has one audio input and one audio
	output, input-satisfaction means a connected audio input. In the
	case of a controller, which has one control output, and no inputs,
	input-satisfaction is immediate. A generator has one audio output
	and N control outputs.*/
	public abstract boolean isInputSatisfied ( );

	public boolean isOutputSatisfied ( ) {
		return this.hasParent();
	}
	
	public boolean remove ( JRNode n ) {
		return children.remove( n );
	}
	
	public void setParent ( JRNode p ) {
		this.parent = p;
	}
	
	public void setSessionID ( long sid ) {
		this.sessionID = sid;
	}
	
	public void setX ( float x ) { this.xpos = x; }
	public void setY ( float y ) { this.ypos = y; }	
	
	public boolean setAngle ( float a ) throws JRInvalidAngleException { 
		/* Overwrite setAngle() in the child classes */ 
		return false;
		}	

}