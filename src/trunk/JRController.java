import java.io.*;
import javax.sound.sampled.*;

public class JRController extends JRNode {
	
	private static final float defaultAmplitude = 1.0F;
	
	// re: frequency, the range 0.5 to 8 is most useful
	// much below 0.5 and the oscillator buffer gets quite expensive
	private static final float defaultFrequency = 0.5F; 

	private JROscillator oscillator;
	private float frequency;
	private int waveform;

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
		much bigger than the audio synthesizer's line buffer. */
		/* Happily, we no longer need the readWholePeriods workaround anyway, now
		that setFrequency() restores oscillator buffer position after swapping
		oscillators. -Jared 11/24/09 */
		boolean readWholePeriods = false;
		
		// initialize oscillator
		this.frequency = frequency;
		this.waveform = waveform;
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

	public void setAngle ( float a ) throws JRInvalidAngleException {
		// new frequency in range from 0.5 to 8 Hz
		float newFrequency = 0.5F + (7.5F * a);
		this.setFrequency( newFrequency );		
	}

	public void setFrequency ( float newFrequency ) {
	
		/* TODO: There is a thread safety problem here. I want to replace
		the oscillator, but what if the syth is reading from it at the
		same time.  This is bound to happen sooner or later */
		
		// Frequency change must be big enough to be worth the effort
		if ( Math.abs( newFrequency - this.frequency ) > 0.5 ) {
			
			// remember the current oscillator buffer position
			float oscbufpos = this.oscillator.getBufferPosition();
			
			// unchanged waveform properties
			float	amplitude = JRController.defaultAmplitude;
			long lengthInFrames = AudioSystem.NOT_SPECIFIED;
	
			// initialize oscillator
			boolean readWholePeriods = false;
			this.oscillator = new JROscillator( 
				this.waveform, newFrequency, amplitude, this.audioFormat, lengthInFrames, readWholePeriods);
			
			// restore oscillator buffer position (to prevent clicking noise)
			this.oscillator.setBufferPosition(oscbufpos);
			
			// update freq
			this.frequency = newFrequency;
		}
	}

	public String toString ( ) {
		return "JRController";
	}

}