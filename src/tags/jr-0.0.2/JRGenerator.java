import java.io.*;
import javax.sound.sampled.*;

public class JRGenerator extends JRNode {

	private int waveform;
	private AudioInputStream oscillator;
	
	public JRGenerator ( int waveform ) {
		super();
		
		// define waveform properties
		this.waveform = waveform;
		float	frequency = 600.0F;
		float	amplitude = 0.7F;
		long lengthInFrames = AudioSystem.NOT_SPECIFIED;
		
		// initialize oscillator
		this.oscillator = new JROscillator( 
			waveform, frequency, amplitude, this.audioFormat, lengthInFrames);
	}
	
	public void addChild ( JRNode child ) throws JRInvalidEdgeException {
		// Generators have only one input, a control input
		// So, if the child is not a Controller, we throw an exception
		JRController controller = null;
		try { controller = (JRController)child; }
		catch ( ClassCastException e ) { 
			throw new JRInvalidEdgeException( "JRGenerator only accepts JRController as children" ); 
		}
		
		// For now, only support one control input
		if ( this.getDegree() > 0 ) {
			throw new JRInvalidEdgeException( "So far, Generators only support one control input" ); 
		}
		
		// Add child
		child.setParent( this );
		children.add( child );
	}


	public int available ( ) throws IOException {
		if ( this.getDegree() == 0 ) { return oscillator.available(); }
		else { throw new IOException("JRGenerator.available() is unsupported when degree > 0"); }
	}


	/*
	  this method should throw an IOException if the frame size is not 1.
	  Since we currently always use 16 bit samples, the frame size is
	  always greater than 1. So we always throw an exception.
	*/
	public int read() throws IOException {
		throw new IOException("JRGenerator.read() is unsupported");
	}
	
	
	public int read(byte[] abData, int nOffset, int nLength) throws IOException {
	
		// Requested length must be multiple of frame size
		if (nLength % getFormat().getFrameSize() != 0) {
			throw new IOException("length must be an integer multiple of frame size");
		}
		
		// If there are no controller children
		if ( this.getDegree() == 0 ) { 
			return oscillator.read(abData, nOffset, nLength); 
		}
		
		// One controller
		else if ( this.getDegree() == 1 ) {
			//System.out.println("Trying to mix controller signal and generator signal ..");
			JRController c = (JRController)this.getFirstChild();
			byte[] b1 = new byte[nLength];
			byte[] b2 = new byte[nLength];
			int nRead = oscillator.read(b1, nOffset, nLength);
			int nReadCtrl = c.read(b2, nOffset, nRead); // nRead? nLength?

			// assertion: equal signal read length
			if (nRead != nReadCtrl) { 
				throw new IOException ( "Assertion failed: Controller signal read (" + nReadCtrl + ") and generator signal read (" + nRead + ") were not the same length" ); 
			}
			
			// assertion: frame size is four bytes
			if (nRead % 4 != 0) {
				throw new IOException ( "Assertion failed: invalid number of bytes read (" + nRead + ")" ); 
			}
			
			// itterate over signal one frame at a time
			for (int i = 0; i < nRead; i = i + 4) {

				// assume that data comes in 16 bit stereo, big endian
				// assume both channels are the same, so we only process one
				// bytes three and four are discarded
				int generatorSample = (b1[i] << 8) | (b1[i+1] & 0xFF);
				int controllerSample = (b2[i] << 8) | (b2[i+1] & 0xFF);
				
				// normalize the controller sample to a range from -1.0 to 1.0
				float controllerSampleNormalized = controllerSample / 32768.0F;
				
				// calculate result
				int resultSample = Math.round(generatorSample * controllerSampleNormalized);
				//System.out.println("g = " + generatorSample + " c = " + controllerSample + " cn = " + controllerSampleNormalized + " r = " + resultSample);
				
				// assign the result to the left channel of this frame 
				// in the provided big-endian result buffer
				abData[i+0] = (byte) ((resultSample >>> 8) & 0xFF);
				abData[i+1] = (byte) (resultSample & 0xFF);
			
				// assume the right channel is the same as the left
				abData[i+2] = abData[i];
				abData[i+3] = abData[i+1];
				
				}
				
			//System.out.println("Done mixing controller signal and generator signal");
			return nRead;
		}
		else { 
			throw new IOException("JRGenerator.read(byte[], int, int) is unsupported when degree > 1"); 
		}
	}
	

	public String toString ( ) {
		String r;
		switch ( this.waveform ) {
			case JROscillator.WAVEFORM_SINE : r = "Sinewave Generator"; break;
			case JROscillator.WAVEFORM_SQUARE : r = "Squarewave Generator"; break;
			case JROscillator.WAVEFORM_TRIANGLE : r = "Triangle Generator"; break;
			case JROscillator.WAVEFORM_SAWTOOTH : r = "Sawtooth Generator"; break;
			default : r = "Generator";
		}
		return r;
	}

}