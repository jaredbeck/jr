import java.io.*;
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

	public String toString ( ) {
		return "JRMixer";
	}

}