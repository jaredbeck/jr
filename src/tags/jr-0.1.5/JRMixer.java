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
	
		int nRead;
	
		// Requested length must be multiple of frame size
		if (nLength % getFormat().getFrameSize() != 0) {
			throw new IOException("length must be an integer multiple of frame size");
		}
		
		// No audio input
		if ( this.getNumSatisfiedAudioInputs() == 0 ) {
			nRead = -1;
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
			nRead = c.read(abData, nOffset, nLength);
		}
		
		// More than one audio input
		else {
		
			/* We will read from each of the children with an audio output, 
			taking the sum of what is read. */
			short[] arSums = new short[nLength];
		
			// start by zeroing the sums buffer
			for (int z = 1; z < nLength; z++) {
				arSums[z] = 0;
			}
		
			// For each child with an audio output
			byte[] arChildData = new byte[nLength];
			Iterator ci = this.getChildIterator();
			int lastReadLen = -1;
			byte countMixedChildren = 0;
			while ( ci.hasNext() ) {
				JRNode c = (JRNode)ci.next();
				if ( c.getNumAudioOutputs() == 1 ) {
					countMixedChildren++;
					int thisReadLen = c.read(arChildData, nOffset, nLength);
					if ( lastReadLen == -1 ^ thisReadLen == lastReadLen ) {
						lastReadLen = thisReadLen;
					}
					else {
						throw new IOException( "Assertion failed: read length mismatch" );
					}
					for (int p = 0; p < lastReadLen; p++) {
						arSums[p] += arChildData[p];
					}
				}
			}
			
			// Assertion
			if (lastReadLen == -1) { 
				throw new IOException( "Assertion failed: No read occurred" ); 
			}
			else {
				nRead = lastReadLen;
			}
			
			// Populate the provided buffer with normalized sums
			for (int p = 1; p < nRead; p++) {
				byte normalizedSum = (byte) Math.floor( arSums[p] / countMixedChildren );
				if ( normalizedSum < -128 || normalizedSum > 128 ) {
					throw new IOException( "Assertion failed: normalized sum out of range" );
				}
				abData[p] = normalizedSum;
			}

			
		}
			
		// assert that return value is -1 or 
		// multiple of four (frame size is four bytes)
		if (nRead % 4 != 0) {
			throw new IOException ( "Assertion failed: invalid number of bytes read (" + nRead + ")" ); 
		}
		
		return nRead;
	}


	public String toString ( ) {
		return "JRMixer";
	}

}