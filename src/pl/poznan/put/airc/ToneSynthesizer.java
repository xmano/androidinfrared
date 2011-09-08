package pl.poznan.put.airc;

import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class ToneSynthesizer {
	// originally from
	// http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
	// and modified by Steve Pomeroy <steve@staticfree.info>
	// http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
	// and we are using it here for this http://www.lirc.org/html/audio.html

	private final int sampleRate = 44100;
	private final int channelConfig;
	private int i = 0;
	private byte[] raw = new byte[] {};

	/**
	 * 
	 * @param frequency
	 * @param channelConfig
	 *            for AudioFormat.CHANNEL_OUT_STEREO second channel has reversed
	 *            sin
	 */
	public ToneSynthesizer(final int channelConfig) {
		this.channelConfig = channelConfig;
	}

	/**
	 * 
	 * @param duration
	 *            duration of sound in seconds
	 * @param scale
	 *            max amplitude of tone 1.0f = 100%, 0.0f = 0%
	 * @return generated sound encoded in byte array
	 */
	protected byte[] genToneRaw(final double frequency, final double duration,
			final double scale) {
		int numSamples = (int) (duration * sampleRate);
		int i_stop = this.i + numSamples;
		double sample[] = new double[numSamples];

		byte generatedSnd[] = null;
		// fill out the array
		for (int j = 0; i < i_stop; ++i) {
			sample[j++] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.

		if (this.channelConfig == AudioFormat.CHANNEL_OUT_STEREO) {
			generatedSnd = new byte[2 * 2 * numSamples];
			int idx = 0;
			for (final double dVal : sample) {
				// scale to maximum amplitude
				final short val = (short) ((dVal * 32767 * scale));
				final short val_minus = (short) -val;
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
				generatedSnd[idx++] = (byte) (val_minus & 0x00ff);
				generatedSnd[idx++] = (byte) ((val_minus & 0xff00) >>> 8);
			}
		} else {
			generatedSnd = new byte[2 * numSamples];
			int idx = 0;
			for (final double dVal : sample) {
				// scale to maximum amplitude
				final short val = (short) ((dVal * 32767));
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
			}
		}

		return generatedSnd;
	}

	public byte[] genToneRaw(final double frequency,
			final ArrayList<Integer> SignalSpaceList) {
		int numSamples = 0;
		for (Integer d : SignalSpaceList) {
			numSamples += ((double) d) * sampleRate / 1000;
		}
		double sample[] = new double[numSamples];

		byte generatedSnd[] = null;
		// fill out the array
		int i = 0;
		boolean signal = true;
		for (Integer d : SignalSpaceList) {
			int stop = i + (int) ((((double) d) * sampleRate / 1000));
			if (signal)
				for (; i < stop; i++)
					sample[i] = Math.sin(2 * Math.PI * i
							/ (sampleRate / frequency));
			else
				for (; i < stop; i++)
					sample[i] = 0;
			signal = !signal;
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.

		if (this.channelConfig == AudioFormat.CHANNEL_OUT_STEREO) {
			generatedSnd = new byte[2 * 2 * numSamples];
			int idx = 0;
			for (final double dVal : sample) {
				// scale to maximum amplitude
				final short val = (short) ((dVal * 32767));
				final short val_minus = (short) -val;
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
				generatedSnd[idx++] = (byte) (val_minus & 0x00ff);
				generatedSnd[idx++] = (byte) ((val_minus & 0xff00) >>> 8);
			}
		} else {
			generatedSnd = new byte[2 * numSamples];
			int idx = 0;
			for (final double dVal : sample) {
				// scale to maximum amplitude
				final short val = (short) ((dVal * 32767));
				// in 16 bit wav PCM, first byte is the low order byte
				generatedSnd[idx++] = (byte) (val & 0x00ff);
				generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
			}
		}

		return generatedSnd;
	}

	public AudioTrack toneRawToAudioTrack(byte[] toneRaw) {
		final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				this.sampleRate, this.channelConfig,
				AudioFormat.ENCODING_PCM_16BIT, toneRaw.length,
				AudioTrack.MODE_STATIC);
		if (toneRaw.length > 0)
			audioTrack.write(toneRaw, 0, toneRaw.length);

		return audioTrack;
	}

	public void genTrackFragment(final double frequency, final double duration,
			final double scale) {
		byte[] newFragment = this.genToneRaw(frequency, duration, scale);
		byte[] tmp = new byte[this.raw.length + newFragment.length];
		System.arraycopy(this.raw, 0, tmp, 0, this.raw.length);
		System.arraycopy(newFragment, 0, tmp, this.raw.length,
				newFragment.length);
		this.raw = tmp;
	}

	public AudioTrack getTrack() {
		return this.toneRawToAudioTrack(this.raw);
	}

	public void playTone(final double frequency,
			final ArrayList<Integer> SignalSpaceList) {
		int buffSize = AudioTrack.getMinBufferSize(this.sampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT) * 4;
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				this.sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, buffSize,
				AudioTrack.MODE_STREAM);

		byte genSignal[] = new byte[buffSize];
		byte genSpace[] = new byte[buffSize];

		for (int j = 0; j < buffSize;) {
			double dVal = (256 * Math.sin(2 * Math.PI * j
					/ (sampleRate / frequency)));
			final short val = (short) ((dVal * 32767));
			final short val_minus = (short) -val;
			// in 16 bit wav PCM, first byte is the low order byte
			genSpace[j] = 0;
			genSignal[j++] = (byte) (val & 0x00ff);
			genSpace[j] = 0;
			genSignal[j++] = (byte) ((val & 0xff00) >>> 8);
			genSpace[j] = 0;
			genSignal[j++] = (byte) (val_minus & 0x00ff);
			genSpace[j] = 0;
			genSignal[j++] = (byte) ((val_minus & 0xff00) >>> 8);
		}

		audioTrack.play();

		boolean signal = true;
		for (Integer d : SignalSpaceList) {
			final int stop = (int) (((double)(d * sampleRate * 2 * 2))/100000.0);
			if (signal)
				//FIXME not exactly sinusoidal
				for (int i = 0; i < stop;)
					if (stop - i < buffSize)
						i += audioTrack.write(genSignal, 0, stop - i);
					else
						i += audioTrack.write(genSignal, 0, buffSize);
			else
				for (int i = 0; i < stop;)
					if (stop - i < buffSize)
						i += audioTrack.write(genSpace, 0, stop - i);
					else
						i += audioTrack.write(genSpace, 0, buffSize);
			signal = !signal;
		}
		audioTrack.stop();
	}
}
