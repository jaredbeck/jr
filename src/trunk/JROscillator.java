import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;



public class JROscillator
	extends AudioInputStream
{
	private static final boolean	DEBUG = false;

	public static final int WAVEFORM_SINE = 0;
	public static final int WAVEFORM_SQUARE = 1;
	public static final int WAVEFORM_TRIANGLE = 2;
	public static final int WAVEFORM_SAWTOOTH = 3;
	public static final int WAVEFORM_HALF_SQUARE = 4;
	public static final int WAVEFORM_ENVELOPE = 5;

	private byte[]			m_abData;
	private int			m_nBufferPosition;
	private long			m_lRemainingFrames;
	
	private float attack_start_pos = 0.0F;
	private float attack_end_pos = 0.1F;
	private float attack_amplitude = 1.0F;
	private String attack_shape = "linear";
	
	private float sustain_start_pos = 0.1F;
	private float sustain_end_pos = 0.2F;
	private float sustain_amplitude = 0.2F;
	private String sustain_shape = "linear";
	
	private float decay_start_pos = 0.2F;
	private float decay_end_pos = 0.8F;
	private float decay_amplitude = 0.1F;
	private String decay_shape = "linear";
	
	private float release_start_pos = 0.8F;
	private float release_end_pos = 0.9F;
	private float release_amplitude = 0.0F; // always zero
	private String release_shape = "linear";
	
	/* readWholePeriods - read() normally copies a period at a time to the
	provided buffer. Normally, partial periods will be copied in order to
	completely fill the provided buffer. If readWholePeriods is true, then read()
	will only copy whole periods to the provided buffer. Currently, generators
	only copy whole periods, while controllers are allowed to copy partial periods. 
	This is sort of a workaround for now.  When a rotation action is defined for
	controllers, this workaround will no longer be enough of a solution.
	-Jared 4/9/2009 */
	private boolean readWholePeriods;


	public JROscillator(int nWaveformType,
			  float fSignalFrequency,
			  float fAmplitude,
			  AudioFormat audioFormat,
			  long lLength,
			  boolean arg_readWholePeriods)
	{
		super(new ByteArrayInputStream(new byte[0]),
		      new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				      audioFormat.getSampleRate(),
				      16,
				      2,
				      4,
				      audioFormat.getFrameRate(),
				      audioFormat.isBigEndian()),
		      lLength);
		      
		if (DEBUG) { out("DEBUG: Oscillator(): begin"); }
		
		this.readWholePeriods = arg_readWholePeriods;
		
		// envelope stuff
		float attack_length = attack_end_pos - attack_start_pos;
		float sustain_length = sustain_end_pos - sustain_start_pos;
		float decay_length = decay_end_pos - decay_start_pos;
		float release_length = release_end_pos - release_start_pos;
		
		// some oscillator attributes
		m_lRemainingFrames = lLength;
		fAmplitude = (float) (fAmplitude * Math.pow(2, getFormat().getSampleSizeInBits() - 1));
		
		// length of one period in frames
		int nPeriodLengthInFrames = Math.round( getFormat().getFrameRate() / fSignalFrequency );
		out("DEBUG: Oscillator(): getFormat().getFrameRate() = " + getFormat().getFrameRate());
		out("DEBUG: Oscillator(): fSignalFrequency = " + fSignalFrequency);
		out("DEBUG: Oscillator(): nPeriodLengthInFrames = " + nPeriodLengthInFrames);

		// buffer length will be one period
		int nBufferLength = nPeriodLengthInFrames * getFormat().getFrameSize();
		out("DEBUG: Oscillator(): period length in bytes = " + nBufferLength);
		
		// fill buffer with one period of PCM data
		m_abData = new byte[nBufferLength];
		for (int nFrame = 0; nFrame < nPeriodLengthInFrames; nFrame++)
		{
			// The relative position inside the period of the waveform: 0.0 to 1.0
			float	fPeriodPosition = (float) nFrame / (float) nPeriodLengthInFrames;
			
			// Calculate the sample value for this frame
			float	fValue = 0;
			switch (nWaveformType)
			{
			case WAVEFORM_ENVELOPE:
				// only support linear envelope shapes so far
				if (fPeriodPosition >= attack_start_pos && 
						fPeriodPosition <= attack_end_pos) {
					fValue = fPeriodPosition * attack_amplitude / attack_length;
					}
				else if (fPeriodPosition >= sustain_start_pos && 
						fPeriodPosition <= sustain_end_pos) {
					float slope = (sustain_amplitude - attack_amplitude) / sustain_length;
					float b = sustain_amplitude - (slope * sustain_end_pos);
					fValue = slope * fPeriodPosition + b;
					}
				else if (fPeriodPosition >= decay_start_pos && 
						fPeriodPosition <= decay_end_pos) {
					float slope = (decay_amplitude - sustain_amplitude) / decay_length;
					float b = decay_amplitude - (slope * decay_end_pos);
					fValue = slope * fPeriodPosition + b;
					}
				else if (fPeriodPosition >= release_start_pos && 
						fPeriodPosition <= release_end_pos) {
					float slope = (release_amplitude - decay_amplitude) / release_length;
					float b = release_amplitude - (slope * release_end_pos);
					fValue = slope * fPeriodPosition + b;
					}
				else {
					fValue = 0.0F;
					}
				//out("ENVELOPE: pos = " + fPeriodPosition + "\t value = " + fValue);
				break;
				
			case WAVEFORM_SINE:
				fValue = (float) Math.sin(fPeriodPosition * 2.0 * Math.PI);
				break;

			case WAVEFORM_SQUARE:
				fValue = (fPeriodPosition < 0.5F) ? 1.0F : -1.0F;
				break;
			
			case WAVEFORM_HALF_SQUARE:
				fValue = (fPeriodPosition < 0.5F) ? 1.0F : 0.0F;
				break;

			case WAVEFORM_TRIANGLE:
				if (fPeriodPosition < 0.25F)
				{
					fValue = 4.0F * fPeriodPosition;
				}
				else if (fPeriodPosition < 0.75F)
				{
					fValue = -4.0F * (fPeriodPosition - 0.5F);
				}
				else
				{
					fValue = 4.0F * (fPeriodPosition - 1.0F);
				}
				break;

			case WAVEFORM_SAWTOOTH:
				if (fPeriodPosition < 0.5F)
				{
					fValue = 2.0F * fPeriodPosition;
				}
				else
				{
					fValue = 2.0F * (fPeriodPosition - 1.0F);
				}
				break;
			}
			int	nValue = Math.round(fValue * fAmplitude);
			//System.out.println(fValue + " * " + fAmplitude + " = " + nValue);
			int nBaseAddr = (nFrame) * getFormat().getFrameSize();
			
			//out("DEBUG: Oscillator(): build period: " + nBaseAddr + " = " + nValue);
			
			// Write to buffer as:
			// 16 bit stereo, big endian
			if ( audioFormat.isBigEndian() ) {
				m_abData[nBaseAddr + 0] = (byte) ((nValue >>> 8) & 0xFF);
				m_abData[nBaseAddr + 1] = (byte) (nValue & 0xFF);
				m_abData[nBaseAddr + 2] = (byte) ((nValue >>> 8) & 0xFF);
				m_abData[nBaseAddr + 3] = (byte) (nValue & 0xFF);
			}
			// 16 bit stereo, little endian
			else {
				m_abData[nBaseAddr + 0] = (byte) (nValue & 0xFF);
				m_abData[nBaseAddr + 1] = (byte) ((nValue >>> 8) & 0xFF);
				m_abData[nBaseAddr + 2] = (byte) (nValue & 0xFF);
				m_abData[nBaseAddr + 3] = (byte) ((nValue >>> 8) & 0xFF);
			}
			
			//System.out.println(nValue + " = " + (byte) (nValue & 0xFF) + " " + (byte) ((nValue >>> 8) & 0xFF) );
		}
		m_nBufferPosition = 0;
		if (DEBUG) { out("DEBUG: Oscillator(): end"); }
	}


	/**	Returns the number of bytes that can be read without blocking.
		Since there is no blocking possible here, we simply try to
		return the number of bytes available at all. In case the
		length of the stream is indefinite, we return the highest
		number that can be represented in an integer. If the length
		if finite, this length is returned, clipped by the maximum
		that can be represented.
	*/
	public int available()
	{
		int	nAvailable = 0;
		if (m_lRemainingFrames == AudioSystem.NOT_SPECIFIED)
		{
			nAvailable = Integer.MAX_VALUE;
		}
		else
		{
			long	lBytesAvailable = m_lRemainingFrames * getFormat().getFrameSize();
			nAvailable = (int) Math.min(lBytesAvailable, (long) Integer.MAX_VALUE);
		}
		return nAvailable;
	}
	

	/* getAmplitudeAtPosition returns an integer value representing
	the amplitude at a given position in the period.  It only returns the 
	left channel, and it assumes 16-bit 2-channel frames.  It's only
	meant to be used for debugging.
	*/
	private int getAmplitudeAtPosition( int bufferPositionInBytes ) {
		return (m_abData[bufferPositionInBytes] << 8) | (m_abData[bufferPositionInBytes+1] & 0xFF);
		}
	
	
	public float getBufferPosition() {
		return (float)m_nBufferPosition / (float)m_abData.length;
	}


	public void setBufferPosition(float relativePosition) {
	
		// Given a relative position from 0.0 to 1.0
		if (relativePosition < 0.0 || relativePosition > 1.0) {
			System.err.println("ERROR: relative position out of bounds");
			System.exit(1);
		}
	
		/* Calculate new buffer position in bytes.  Find the frame which is 
		nearest to the relative position. */
		int approxNewBufPos = Math.round(m_abData.length * relativePosition);
		int approxOffsetFromNearestFrame = approxNewBufPos % getFormat().getFrameSize();
		int newBufferPosition = approxNewBufPos - approxOffsetFromNearestFrame;
		
		// Make sure the new position is valid
		if (newBufferPosition % getFormat().getFrameSize() != 0) {
			System.err.println("ERROR: newBufferPosition must be an integer multiple of frame size");
			System.exit(1);
		}
		
		// Set buffer position
		m_nBufferPosition = newBufferPosition;
		if (DEBUG) { out("DEBUG: Oscillator.setBufferPosition(): m_nBufferPosition = " + m_nBufferPosition); }
	}


	/*
	  this method should throw an IOException if the frame size is not 1.
	  Since we currently always use 16 bit samples, the frame size is
	  always greater than 1. So we always throw an exception.
	*/
	public int read()
		throws IOException
	{
		if (DEBUG) { out("DEBUG: Oscillator.read(): begin"); }
		throw new IOException("cannot use this method currently");
	}



	public int read(byte[] abData, int nOffset, int nLength)
		throws IOException
	{
		if (DEBUG) { out("DEBUG: Oscillator.read(): begin"); }
		
		// is the requested length valid?
		if (nLength % getFormat().getFrameSize() != 0)
		{
			throw new IOException("length must be an integer multiple of frame size");
		}
		
		// limit this read() to what is available()
		int	nConstrainedLength = Math.min(available(), nLength);
		int	nRemainingLength = nConstrainedLength;

		if (DEBUG) { 
			out("DEBUG: Oscillator.read(): available: " + available()); 
			out("DEBUG: Oscillator.read(): constrained: " + nConstrainedLength); 
			out("DEBUG: Oscillator.read(): nRemainingLength: " + nRemainingLength);
			out("DEBUG: Oscillator.read(): m_abData.length: " + m_abData.length);
			out("DEBUG: Oscillator.read(): m_nBufferPosition: " + m_nBufferPosition);
			int debugamp = getAmplitudeAtPosition(m_nBufferPosition);
			out("DEBUG: Oscillator.read(): first two bytes at m_nBufferPosition: " + debugamp);
			}
		
		// keep track of how many bytes are actually copied
		int nBytesActuallyRead = 0;
		
		// when to stop reading (is it OK to copy partial periods?)
		int minCopyPassLenInBytes = -1;
		if (this.readWholePeriods) { minCopyPassLenInBytes = m_abData.length; }
		else { minCopyPassLenInBytes = getFormat().getFrameSize(); }
		
		// Copy data in passes, until nLength is reached, or available() data is exhausted.
		// The data is copied one period at a time, or, in other words, 
		// one full Oscillator buffer at a time.
		while (nRemainingLength >= minCopyPassLenInBytes)
		{
		
			if (DEBUG) { 
				out("DEBUG: copy pass begin"); 
				out("\tnRemainingLength: " + nRemainingLength); 
				}
		
			// The size of a pass is usually a full period (the length of the Oscillator's buffer)
			// However, if we left off in the middle of the period at the end of the last read(),
			// then the size of this pass is the remaining length of the period, ie. the 
			// remainder of the Oscillator's buffer
			int	nNumBytesToCopyNow = m_abData.length - m_nBufferPosition;
				
			// Of course, if that's too much data (more than was requested) 
			// then just satisfy the request (fill up abData)
			nNumBytesToCopyNow = Math.min(nNumBytesToCopyNow, nRemainingLength);
			
			if (DEBUG) {
				out("\tnNumBytesToCopyNow: min( " + m_abData.length + " - " + m_nBufferPosition + " , " + nRemainingLength + " )  = " + nNumBytesToCopyNow); 
				}
			
			// the copy
			System.arraycopy(m_abData, m_nBufferPosition, abData, nOffset, nNumBytesToCopyNow);
			nBytesActuallyRead += nNumBytesToCopyNow;
			
			// update markers
			nRemainingLength -= nNumBytesToCopyNow;
			nOffset += nNumBytesToCopyNow;
			
			// This line below is quite clever.
			// Basically, if we didn't copy all of the Oscillator's buffer (all of a period)
			// then we save the buffer position, part-way through, for next time a read() happens.
			// That way, we pick up where we left off.
			m_nBufferPosition = (m_nBufferPosition + nNumBytesToCopyNow) % m_abData.length;
			
			if (DEBUG) { 
				out("\tm_nBufferPosition: " + m_nBufferPosition); 
				out("DEBUG: copy pass end"); 
				}
		}
		
		// calculate frames read
		int	nFramesRead = nConstrainedLength / getFormat().getFrameSize();
		
		// if this stream is of finite length, then decrement the 
		// number of remaining frames
		if (m_lRemainingFrames != AudioSystem.NOT_SPECIFIED)
		{
			m_lRemainingFrames -= nFramesRead;
		}
		
		// return number of bytes actually read
		int	nReturn = nBytesActuallyRead;
		
		if (m_lRemainingFrames == 0)
		{
			nReturn = -1;
		}
		if (DEBUG) { 
			out("DEBUG: Oscillator.read(): nBytesActuallyRead = " + nBytesActuallyRead);
			out("DEBUG: Oscillator.read(): m_nBufferPosition: " + m_nBufferPosition);
			if (m_nBufferPosition > 0) {
				int lastFramePos = m_nBufferPosition - getFormat().getFrameSize();
				int debugamp = getAmplitudeAtPosition(lastFramePos);
				out("DEBUG: Oscillator.read(): amplitude of last frame: " + debugamp);
				}
			out("DEBUG: Oscillator.read(): end"); 
			}
		return nReturn;
	}

	
	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}


