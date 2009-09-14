import java.io.*;
import javax.sound.sampled.*;

public class JRController extends JRNode {

	private Oscillator oscillator;

	public JRController (  ) {
		super();
		
		// define waveform properties
		int waveform = JRGenerator.WAVEFORM_TRIANGLE;
		float	amplitude = 0.9F;
		long lengthInFrames = AudioSystem.NOT_SPECIFIED;
		float	frequency = 2.0F;  // the range 0.5 to 4 is most useful
		
		// initialize oscillator
		this.oscillator = new Oscillator( 
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