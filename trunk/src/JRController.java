import java.io.*;
import javax.sound.sampled.*;

public class JRController extends JRNode {
	
	private static final float defaultAmplitude = 1.0F;
	private static final float defaultFrequency = 2.0F; // the range 0.5 to 8 is most useful

	private AudioInputStream oscillator;

	// Convenience constructors
	public JRController ( ) { 
		this( JROscillator.WAVEFORM_HALF_SQUARE, defaultFrequency, defaultAmplitude ); 
	}
	
	public JRController ( int waveform ) { 
		this( waveform, defaultFrequency, defaultAmplitude ); 
	}
	
	public JRController ( int waveform, float frequency ) { 
		this( waveform, frequency, defaultAmplitude ); 
	}

	// Main constructor
	public JRController ( int waveform, float frequency, float amplitude ) {
		super();
		
		/* Allow partial period oscillator read()s.  We can't require whole period
		read()s because the typical controller's period is around 80000 bytes long,
		much bigger than the audio synthesizer's line buffer. This is a temporary
		workaround.  When a rotation action or proximity effect is defined for
		controllers, we will probably experience the same clicking noises that used
		to come from generators.  */
		boolean readWholePeriods = false;
		
		// initialize oscillator
		long lengthInFrames = AudioSystem.NOT_SPECIFIED;
		this.oscillator = new JROscillator( 
			waveform, frequency, amplitude, this.audioFormat, lengthInFrames, readWholePeriods);
	}
	
	public int getNumAudioOutputs ( ) { return 0; }
	public int getNumAudioInputs ( ) { return 0; }
	public int getNumControlOutputs ( ) { return 1; }
	public int getNumControlInputs ( ) { return 0; }
	
	public boolean isInputSatisfied ( ) {
		// A controller is immediately input-satisfied upon creation.
		// i.e. A controller does not need any children in order to output.
		return true;
	}
	
	public int read(byte[] abData, int nOffset, int nLength) throws IOException {
		if ( this.getDegree() == 0 ) { 
			return oscillator.read(abData, nOffset, nLength); 
		}
		else {
			throw new IOException ( "JRController.read() not supported when degree > 0" );
		}
	}

	public String toString ( ) {
		return "JRController";
	}

}