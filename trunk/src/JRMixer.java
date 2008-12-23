import java.io.*;
import java.util.*;
import javax.sound.sampled.*;

public class JRMixer extends JRNode {

	public JRMixer ( ) {
		super();
	}

	public int getNumAudioOutputs ( ) { return 1; }
	public int getNumAudioInputs ( ) { return Integer.MAX_VALUE; }
	public int getNumControlOutputs ( ) { return 0; }
	public int getNumControlInputs ( ) { return Integer.MAX_VALUE; }

	public boolean isInputSatisfied ( ) {
		/* In order to be considered input-satisfied, a mixer must have at
		least one child with an audio output. This satisfies the mixer's
		minimum audio input. */
		return getNumSatisfiedAudioInputs() > 0;
	}


	public int read(byte[] abData, int nOffset, int nLength) throws IOException {
	
		// Requested length must be multiple of frame size
		if (nLength % getFormat().getFrameSize() != 0) {
			throw new IOException("length must be an integer multiple of frame size");
		}
		
		// No audio input
		if ( this.getNumSatisfiedAudioInputs() == 0 ) {
			return -1;
			//throw new IOException("JRMixer.read() is unsupported when getNumSatisfiedAudioInputs() == 0"); 
		}
		
		// One child
		else if ( this.getNumSatisfiedAudioInputs() == 1 ) {

			// Which child is the audio input?
			Iterator ci = this.getChildIterator();
			JRNode c = null;
			while ( ci.hasNext() ) {
				c = (JRNode)ci.next();
				if ( c.getNumAudioOutputs() == 1 ) break;
				}

			// read from audio input
			int nRead = c.read(abData, nOffset, nLength);
			
			// assertion: frame size is four bytes
			if (nRead % 4 != 0) {
				throw new IOException ( "Assertion failed: invalid number of bytes read (" + nRead + ")" ); 
			}
			
			return nRead;
		}
		
		// More than one audio input
		else {
			throw new IOException("JRMixer.read(byte[], int, int) is unsupported when getNumSatisfiedAudioInputs() > 1");
		}
	}


	public String toString ( ) {
		return "JRMixer";
	}

}