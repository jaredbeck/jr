/*
 *	Oscillator.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;



public class Oscillator
	extends AudioInputStream
{
	private static final boolean	DEBUG = false;

	public static final int		WAVEFORM_SINE = 0;
	public static final int		WAVEFORM_SQUARE = 1;
	public static final int		WAVEFORM_TRIANGLE = 2;
	public static final int		WAVEFORM_SAWTOOTH = 3;

	private byte[]			m_abData;
	private int			m_nBufferPosition;
	private long			m_lRemainingFrames;


	public Oscillator(int nWaveformType,
			  float fSignalFrequency,
			  float fAmplitude,
			  AudioFormat audioFormat,
			  long lLength)
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
		if (DEBUG) { out("Oscillator.<init>(): begin"); }
		m_lRemainingFrames = lLength;
		fAmplitude = (float) (fAmplitude * Math.pow(2, getFormat().getSampleSizeInBits() - 1));
		
		// length of one period in frames
		int nPeriodLengthInFrames = Math.round(getFormat().getFrameRate() / fSignalFrequency);
		//System.out.println("Oscillator.init() nPeriodLengthInFrames = " + nPeriodLengthInFrames);
		int nBufferLength = nPeriodLengthInFrames * getFormat().getFrameSize();
		
		// fill buffer with one period of PCM data
		m_abData = new byte[nBufferLength];
		for (int nFrame = 0; nFrame < nPeriodLengthInFrames; nFrame++)
		{
			/**	The relative position inside the period
				of the waveform. 0.0 = beginning, 1.0 = end
			*/
			float	fPeriodPosition = (float) nFrame / (float) nPeriodLengthInFrames;
			float	fValue = 0;
			switch (nWaveformType)
			{
			case WAVEFORM_SINE:
				fValue = (float) Math.sin(fPeriodPosition * 2.0 * Math.PI);
				break;

			case WAVEFORM_SQUARE:
				fValue = (fPeriodPosition < 0.5F) ? 1.0F : -1.0F;
				//System.out.println(fPeriodPosition + " " + fValue);
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
			// this is for 16 bit stereo, little endian
			m_abData[nBaseAddr + 0] = (byte) (nValue & 0xFF);
			m_abData[nBaseAddr + 1] = (byte) ((nValue >>> 8) & 0xFF);
			m_abData[nBaseAddr + 2] = (byte) (nValue & 0xFF);
			m_abData[nBaseAddr + 3] = (byte) ((nValue >>> 8) & 0xFF);
			//System.out.println(nValue + " = " + (byte) (nValue & 0xFF) + " " + (byte) ((nValue >>> 8) & 0xFF) );
		}
		m_nBufferPosition = 0;
		if (DEBUG) { out("Oscillator.<init>(): end"); }
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



	/*
	  this method should throw an IOException if the frame size is not 1.
	  Since we currently always use 16 bit samples, the frame size is
	  always greater than 1. So we always throw an exception.
	*/
	public int read()
		throws IOException
	{
		if (DEBUG) { out("Oscillator.read(): begin"); }
		throw new IOException("cannot use this method currently");
	}



	public int read(byte[] abData, int nOffset, int nLength)
		throws IOException
	{
		if (DEBUG) { out("Oscillator.read(): begin"); }
		
		// is the requested length valid?
		if (nLength % getFormat().getFrameSize() != 0)
		{
			throw new IOException("length must be an integer multiple of frame size");
		}
		
		// limit this read() to what is available()
		int	nConstrainedLength = Math.min(available(), nLength);
		int	nRemainingLength = nConstrainedLength;
		
		// Copy data in passes, until nLength is reached, or available() data is exhausted.
		// The data is copied one period at a time, or, in other words, 
		// one full Oscillator buffer at a time.
		while (nRemainingLength > 0)
		{
			// The size of a pass is usually a full period (the length of the Oscillator's buffer)
			// However, if we left off in the middle of the period at the end of the last read(),
			// then the size of this pass is the remaining length of the period.
			int	nNumBytesToCopyNow = m_abData.length - m_nBufferPosition;
			
			// Of course, if that's too much data (more than was requested) 
			// then just satisfy the request (fill up abData)
			nNumBytesToCopyNow = Math.min(nNumBytesToCopyNow, nRemainingLength);
			
			// the copy
			System.arraycopy(m_abData, m_nBufferPosition, abData, nOffset, nNumBytesToCopyNow);
			
			// update markers
			nRemainingLength -= nNumBytesToCopyNow;
			nOffset += nNumBytesToCopyNow;
			
			// This line below has some serious wizardry in it!
			// Basically, if we didn't copy all of the Oscillator's buffer (all of a period)
			// then we save the buffer position, part-way through, for next time a read() happens.
			// That way, we pick up where we left off.
			m_nBufferPosition = (m_nBufferPosition + nNumBytesToCopyNow) % m_abData.length;
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
		int	nReturn = nConstrainedLength;
		if (m_lRemainingFrames == 0)
		{
			nReturn = -1;
		}
		if (DEBUG) { out("Oscillator.read(): end"); }
		return nReturn;
	}



	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}



/*** Oscillator.java ***/

