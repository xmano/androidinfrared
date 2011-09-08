package pl.poznan.put.airc;

import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class ToneSynthesizer {
	// originally from
	// http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
	// and modified by Steve Pomeroy <steve@staticfree.info>
	// http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
	// and we are using it here for this http://www.lirc.org/html/audio.html

	private final int sampleRate = 44100;
	private final int channelConfig;
	private final int frequency;
	private byte[] raw = new byte[] {};
	private final int buffSize;
	final byte genSignal[];
	final byte genSpace[];

	/**
	 * 
	 * @param frequency
	 * @param channelConfig
	 *            for AudioFormat.CHANNEL_OUT_STEREO second channel has reversed
	 *            sin
	 */
	public ToneSynthesizer(final int channelConfig, final int frequency) {
		this.channelConfig = channelConfig;
		this.frequency = frequency;

		buffSize = AudioTrack.getMinBufferSize(this.sampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT) * 4;
		Log.i("AIRC", "buffsize " + Integer.toString(buffSize));

		genSignal = new byte[buffSize];
		genSpace = new byte[buffSize];

		for (int j = 0; j < buffSize;) {
			double dVal = Math.sin(2 * Math.PI * ((double)j)/4.0
					/ (((double)sampleRate) / ((double)frequency)));
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
	}

	public void playTone(final ArrayList<Integer> SignalSpaceList) {

		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				this.sampleRate, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, buffSize,
				AudioTrack.MODE_STREAM);
		audioTrack.play();
		for(int i=50;i>0;i--)
			audioTrack.write(genSpace, 0, buffSize);

		boolean signal = true;
		int count=0;
		for (Integer d : SignalSpaceList) {
			Log.i("AIRC", Integer.toString(d));
			final int stop = (int) (((double) (d * sampleRate)) / 1000000.0) *4 ;
			if (signal)
				// FIXME not exactly sinusoidal
				for (int i = 0; i < stop;)
				{
					if (stop - i < buffSize)
						count= audioTrack.write(genSignal, 0, stop - i);
						//count = audioTrack.write(genSignal, 0, buffSize/4);
					else
						count = audioTrack.write(genSignal, 0, buffSize);
					if(count>0)
						i+=count;
					Log.i("AIRC", "i " +  Integer.toString(i) + " count " + Integer.toString(i) + " stop " + Integer.toString(count));
				}
			else
				for (int i = 0; i < stop;)
				{
					if (stop - i < buffSize)
						//count = audioTrack.write(genSpace, 0, buffSize/4);
						count= audioTrack.write(genSpace, 0, stop - i);
					else
						count = audioTrack.write(genSpace, 0, buffSize);
					if(count>0)
						i+=count;
					Log.i("AIRC", "i " +  Integer.toString(i) + " count " + Integer.toString(i) + " stop " + Integer.toString(count));
				}
			
			signal = !signal;
		}
		audioTrack.stop();
	}
}
