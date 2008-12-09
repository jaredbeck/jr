import java.io.*;
import javax.sound.sampled.*;

public class JRFilter extends JRNode {

	public JRFilter (  ) {
		super();
	}

	public int getNumAudioOutputs ( ) { return 1; }
	public int getNumAudioInputs ( ) { return 1; }
	public int getNumControlOutputs ( ) { return 0; }
	public int getNumControlInputs ( ) { return Integer.MAX_VALUE; }

	public boolean isInputSatisfied ( ) {
		/* In order to be considered input-satisfied, a filter must have
		one child with an audio output. This satisfies the filter's 
		audio input. */
		return getNumSatisfiedAudioInputs() == 1;
	}
	
	public String toString ( ) {
		return "JRFilter";
	}

}