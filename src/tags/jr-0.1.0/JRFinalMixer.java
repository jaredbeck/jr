import java.io.*;
import javax.sound.sampled.*;

public class JRFinalMixer extends JRMixer {

	public JRFinalMixer ( ) {
		super();
	}
	
	/* The final mixer is a special case.  It is only used
	when constructing non-trivial trees, and is only placed
	at the head.  Because it is at the head, it is always
	output-satisfied. */
	public boolean isOutputSatisfied ( ) {
		return true;
	}

	public String toString ( ) {
		return "JRFinalMixer";
	}

}