import java.io.*;
import javax.sound.sampled.*;

public class JRController extends JRNode {
	
	private static final float defaultAmplitude = 0.9F;
	private static final float defaultFrequency = 2.0F; // the range 0.5 to 4 is most useful

	private AudioInputStream oscillator;

	// Convenience constructors
	public JRController ( ) { 
		this( JROscillator.WAVEFORM_HALF_SQUARE, defaultAmplitude, defaultFrequency ); 
	}
	
	public JRController ( int waveform ) { 
		this( waveform, defaultAmplitude, defaultFrequency ); 
	}
	
	public JRController ( int waveform, float frequency ) { 
		this( waveform, defaultAmplitude, frequency ); 
	}

	// Main constructor
	public JRController ( int waveform, float amplitude, float frequency ) {
		super();
		long lengthInFrames = AudioSystem.NOT_SPECIFIED;
		this.oscillator = new JROscillator( 
			waveform, frequency, amplitude, this.audioFormat, lengthInFrames);
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