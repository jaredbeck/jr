import java.io.*;
import javax.sound.sampled.*;

public class JRGenerator extends JRNode {

	public static final int WAVEFORM_SINE = 0;
	public static final int WAVEFORM_SQUARE = 1;
	public static final int WAVEFORM_TRIANGLE = 2;
	public static final int WAVEFORM_SAWTOOTH = 3;

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
		this.oscillator = new Oscillator( 
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

			// sanity check
			if (nRead != nReadCtrl) { 
				throw new IOException ( "Controller signal read (" + nReadCtrl + ") and generator signal read (" + nRead + ") were not the same length" ); 
			}
			
			// itterate over signal one frame at a time
			for (int i = 0; i < nRead; i = i + 4) {

				// data comes in 16 bit stereo, little endian
				// except we assume that both channels are the same,
				// so we only process one
				byte b1_channel1_lessSig = b1[i];
				byte b1_channel1_moreSig = b1[i+1];
				//byte b1_channel2_lessSig = b1[i+2];
				//byte b1_channel2_moreSig = b1[i+3];
				byte b2_channel1_lessSig = b2[i];
				byte b2_channel1_moreSig = b2[i+1];
				//byte b2_channel2_lessSig = b2[i+2];
				//byte b2_channel2_moreSig = b2[i+3];
				
				int b1_channel1_value = (b1_channel1_moreSig * 256) + b1_channel1_lessSig;
				//System.out.println(b1_channel1_moreSig + " * 256 + " + b1_channel1_lessSig + " = " + b1_channel1_value);
				//int b1_channel2_value = (b1_channel2_moreSig * 256) + b1_channel2_lessSig;
				int b2_channel1_value = (b2_channel1_moreSig * 256) + b2_channel1_lessSig;
				//System.out.println(b2_channel1_moreSig + " * 256 + " + b2_channel1_lessSig + " = " + b2_channel1_value);
				//int b2_channel2_value = (b2_channel2_moreSig * 256) + b2_channel2_lessSig;
				
				// all that so I can do big endian math
				int result_channel1 = (int)(b1_channel1_value * (float)(b2_channel1_value / 32768.0F));
				//System.out.println(b1_channel1_value + " * " + b2_channel1_value + " / 32768.0F = " + result_channel1);
				//int result_channel2 = b1_channel2_value * b2_channel2_value;
				
				// now return it to little endian!
				abData[i + 0] = (byte) (result_channel1 & 0xFF);
				abData[i + 1] = (byte) ((result_channel1 >>> 8) & 0xFF);
				abData[i + 2] = (byte) (result_channel1 & 0xFF);
				abData[i + 3] = (byte) ((result_channel1 >>> 8) & 0xFF);
				
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
			case JRGenerator.WAVEFORM_SINE : r = "Sinewave Generator"; break;
			case JRGenerator.WAVEFORM_SQUARE : r = "Squarewave Generator"; break;
			case JRGenerator.WAVEFORM_TRIANGLE : r = "Triangle Generator"; break;
			case JRGenerator.WAVEFORM_SAWTOOTH : r = "Sawtooth Generator"; break;
			default : r = "Generator";
		}
		return r;
	}

}