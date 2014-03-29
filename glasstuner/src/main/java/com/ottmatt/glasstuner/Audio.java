package com.ottmatt.glasstuner;

import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by mott on 3/27/14.
 */

public class Audio  implements Runnable {

    private static final String TAG = "Audio";
    // Preferences

    protected int input;

    protected boolean zoom;
    protected boolean filter;
    protected boolean screen;
    protected boolean strobe;
    protected boolean multiple;
    protected boolean downsample;

    protected double reference;
    protected double sample;

    // Data

    protected Thread thread;
    protected double buffer[];
    protected short data[];

    // Output data

    protected double lower;
    protected double higher;
    protected double nearest;
    protected double frequency;
    protected double difference;
    protected double cents;
    protected double fps;

    protected int count;
    protected int note;

    // Private data

    private long timer;
    private int divisor = 1;

    private AudioRecord audioRecord;

    private static final int MAXIMA = 8;
    private static final int OVERSAMPLE = 16;
    private static final int SAMPLES = 16384;
    private static final int RANGE = SAMPLES * 3 / 8;
    private static final int STEP = SAMPLES / OVERSAMPLE;
    private static final int SIZE = 4096;

    private static final int OCTAVE = 12;
    private static final int C5_OFFSET = 57;
    private static final long TIMER_COUNT = 24;
    private static final double MIN = 0.5;

    private static final double G = 3.023332184e+01;
    private static final double K = 0.9338478249;

    private double xv[];
    private double yv[];

    private Complex x;

    protected float signal;

    protected Maxima maxima;

    protected double xa[];

    private double xp[];
    private double xf[];
    private double dx[];

    private double x2[];
    private double x3[];
    private double x4[];
    private double x5[];

    private Resources resources;

    private Spectrum spectrum;
    private Display display;

    // Constructor

    protected Audio(Resources resources, Spectrum spectrum, Display display) {
        this.resources = resources;
        this.display = display;
        this.spectrum = spectrum;

        buffer = new double[SAMPLES];
        data = new short[STEP];

        xv = new double[2];
        yv = new double[2];

        x = new Complex(SAMPLES);

        maxima = new Maxima(MAXIMA);

        xa = new double[RANGE];
        xp = new double[RANGE];
        xf = new double[RANGE];
        dx = new double[RANGE];

        x2 = new double[RANGE / 2];
        x3 = new double[RANGE / 3];
        x4 = new double[RANGE / 4];
        x5 = new double[RANGE / 5];
    }

    // Start audio

    protected void start() {
        // Start the thread

        thread = new Thread(this, "Audio");
        thread.start();
    }

    // Run

    @Override
    public void run() {
        processAudio();
    }

    // Stop

    protected void stop() {
        Thread t = thread;
        thread = null;

        // Wait for the thread to exit

        while (t != null && t.isAlive())
            Thread.yield();
    }

    // Process Audio

    protected void processAudio() {
        // Sample rates to try

        int rates[] = resources.getIntArray(org.billthefarmer.tuner.R.array.sample_rates);

        int size;
        for (int rate : rates) {
            sample = rate;
            size = AudioRecord.getMinBufferSize((int) sample,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
            if (size > 0) {
                break;
            } else if (size == AudioRecord.ERROR) {
                Log.d(TAG, "Error from audio recording! Cannot find a working input sample rate.");
                thread = null;
                return;
            }
        }

        // Set divisor according to sample rate

        // If you change the sample rates, make sure that this code
        // still works correctly, as both arrays get sorted as there
        // is no array.getIndexOf()

        Arrays.sort(rates);
        int index = Arrays.binarySearch(rates, (int) sample);
        int divisors[] = resources.getIntArray(org.billthefarmer.tuner.R.array.divisors);
        Arrays.sort(divisors);
        divisor = divisors[index];

        // Calculate fps

        fps = (sample / divisor) / SAMPLES;
        final double expect = 2.0 * Math.PI * STEP / SAMPLES;

        // Create the AudioRecord object

        audioRecord = new AudioRecord(input, (int) sample,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        SIZE * divisor);
        // Check state

        int state = audioRecord.getState();

        if (state != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "Audio recorder not initialised! Maybe the selected input source is not available, or a working input sample rate could not be found.");

            audioRecord.release();
            thread = null;
            return;
        }

        // Start recording

        audioRecord.startRecording();

        // Max data

        double dmax = 0.0;

        // Continue until the thread is stopped

        while (thread != null) {
            // Read a buffer of data

            size = audioRecord.read(data, 0, STEP * divisor);

            // Stop the thread if no data

            if (size == 0) {
                thread = null;
                break;
            }

            // Move the main data buffer up

            System.arraycopy(buffer, STEP, buffer, 0, SAMPLES - STEP);

            // Max signal

            double rm = 0;

            // Butterworth filter, 3dB/octave

            for (int i = 0; i < STEP; i++) {
                xv[0] = xv[1];
                xv[1] = data[i * divisor] / G;

                yv[0] = yv[1];
                yv[1] = (xv[0] + xv[1]) + (K * yv[0]);

                // Choose filtered/unfiltered data

                buffer[(SAMPLES - STEP) + i] =
                        filter ? yv[1] : data[i * divisor];

                // Find root mean signal

                double v = data[i * divisor] / 32768.0;
                rm += v * v;
            }

            // Signal value

            rm /= STEP;
            signal = (float) Math.sqrt(rm);

            // Maximum value

            if (dmax < 4096.0)
                dmax = 4096.0;

            // Calculate normalising value

            double norm = dmax;

            dmax = 0.0;

            // Copy data to FFT input arrays for tuner

            for (int i = 0; i < SAMPLES; i++) {
                // Find the magnitude

                if (dmax < Math.abs(buffer[i]))
                    dmax = Math.abs(buffer[i]);

                // Calculate the window

                double window = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / SAMPLES);

                // Normalise and window the input data

                x.r[i] = buffer[i] / norm * window;
            }

            // do FFT for tuner

            fftr(x);

            // Process FFT output for tuner

            for (int i = 1; i < RANGE; i++) {
                double real = x.r[i];
                double imag = x.i[i];

                xa[i] = Math.hypot(real, imag);

                // Do frequency calculation

                double p = Math.atan2(imag, real);
                double dp = xp[i] - p;

                xp[i] = p;

                // Calculate phase difference

                dp -= i * expect;

                int qpd = (int) (dp / Math.PI);

                if (qpd >= 0)
                    qpd += qpd & 1;

                else
                    qpd -= qpd & 1;

                dp -= Math.PI * qpd;

                // Calculate frequency difference

                double df = OVERSAMPLE * dp / (2.0 * Math.PI);

                // Calculate actual frequency from slot frequency plus
                // frequency difference and correction value

                xf[i] = i * fps + df * fps;

                // Calculate differences for finding maxima

                dx[i] = xa[i] - xa[i - 1];
            }

            // Downsample

            if (downsample) {
                // x2 = xa << 2

                for (int i = 0; i < RANGE / 2; i++) {
                    x2[i] = 0.0;

                    for (int j = 0; j < 2; j++)
                        x2[i] += xa[(i * 2) + j] / 2.0;
                }

                // x3 = xa << 3

                for (int i = 0; i < RANGE / 3; i++) {
                    x3[i] = 0.0;

                    for (int j = 0; j < 3; j++)
                        x3[i] += xa[(i * 3) + j] / 3.0;
                }

                // x4 = xa << 4

                for (int i = 0; i < RANGE / 4; i++) {
                    x4[i] = 0.0;

                    for (int j = 0; j < 4; j++)
                        x2[i] += xa[(i * 4) + j] / 4.0;
                }

                // x5 = xa << 5

                for (int i = 0; i < RANGE / 5; i++) {
                    x5[i] = 0.0;

                    for (int j = 0; j < 5; j++)
                        x5[i] += xa[(i * 5) + j] / 5.0;
                }

                // Add downsamples

                for (int i = 1; i < RANGE; i++) {
                    if (i < RANGE / 2)
                        xa[i] += x2[i];

                    if (i < RANGE / 3)
                        xa[i] += x3[i];

                    if (i < RANGE / 4)
                        xa[i] += x4[i];

                    if (i < RANGE / 5)
                        xa[i] += x5[i];

                    // Recalculate differences

                    dx[i] = xa[i] - xa[i - 1];
                }
            }

            // Maximum FFT output

            double max = 0.0;

            count = 0;
            int limit = RANGE - 1;

            // Find maximum value, and list of maxima

            for (int i = 1; i < limit; i++) {
                if (xa[i] > max) {
                    max = xa[i];
                    frequency = xf[i];
                }

                // If display not locked, find maxima and add to list

                if (count < MAXIMA &&
                        xa[i] > MIN && xa[i] > (max / 4.0) &&
                        dx[i] > 0.0 && dx[i + 1] < 0.0) {
                    maxima.f[count] = xf[i];

                    // Cents relative to reference

                    double cf =
                            -12.0 * log2(reference / xf[i]);

                    // Reference note

                    maxima.r[count] = reference *
                            Math.pow(2.0, Math.round(cf) / 12.0);

                    // Note number

                    maxima.n[count] = (int) (Math.round(cf) + C5_OFFSET);

                    // Don't use if negative

                    if (maxima.n[count] < 0) {
                        maxima.n[count] = 0;
                        continue;
                    }

                    // Set limit to octave above

                    if (!downsample && (limit > i * 2))
                        limit = i * 2 - 1;

                    count++;
                }
            }

            // Found flag

            boolean found = false;

            // Do the note and cents calculations

            if (max > MIN) {
                found = true;

                // Frequency

                if (!downsample)
                    frequency = maxima.f[0];

                // Cents relative to reference

                double cf =
                        -12.0 * log2(reference / frequency);

                // Don't count silly values

                if (Double.isNaN(cf))
                    continue;

                // Reference note

                nearest = reference *
                        Math.pow(2.0, Math.round(cf) / 12.0);

                // Lower and upper freq

                lower = reference *
                        Math.pow(2.0, (Math.round(cf) - 0.55) / 12.0);
                higher = reference *
                        Math.pow(2.0, (Math.round(cf) + 0.55) / 12.0);

                // Note number

                note = (int) Math.round(cf) + C5_OFFSET;

                if (note < 0) {
                    note = 0;
                    found = false;
                }
                // Find nearest maximum to reference note

                double df = 1000.0;

                for (int i = 0; i < count; i++) {
                    if (Math.abs(maxima.f[i] - nearest) < df) {
                        df = Math.abs(maxima.f[i] - nearest);
                        frequency = maxima.f[i];
                    }
                }

                // Cents relative to reference note

                cents = -12.0 * log2(nearest / frequency) * 100.0;

                // Ignore silly values

                if (Double.isNaN(cents)) {
                    cents = 0.0;
                    found = false;
                }

                // Ignore if not within 50 cents of reference note

                if (Math.abs(cents) > 50.0) {
                    cents = 0.0;
                    found = false;
                }

                // Difference

                difference = frequency - nearest;

                Log.d(TAG, "Found note: " + Integer.toString(note) + " but found was " + Boolean.toString(found));
            } else {
                Log.d(TAG, "Note wasn't found");
            }

            // Found

            if (found) {

                //Update spectrum

                if (spectrum != null)
                    spectrum.postInvalidate();

                // Update display

                if (display != null)
                    display.postInvalidate();
                // Reset count;

                timer = 0;
            } else {

                if (timer > TIMER_COUNT) {
                    difference = 0.0;
                    frequency = 0.0;
                    nearest = 0.0;
                    higher = 0.0;
                    lower = 0.0;
                    cents = 0.0;
                    count = 0;
                    note = 0;

                    // Update display

                    if (display != null)
                        display.postInvalidate();

                }
                // Update spectrum

                if (spectrum != null)
                    spectrum.postInvalidate();

                if (display != null)
                    display.postInvalidate();
            }

            timer++;
        }

        // Stop and release the audio recorder

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    // Real to complex FFT, ignores imaginary values in input array

    private void fftr(Complex complex) {
        final int n = complex.r.length;
        final double norm = Math.sqrt(1.0 / n);

        for (int i = 0, j = 0; i < n; i++) {
            if (j >= i) {
                double tr = complex.r[j] * norm;

                complex.r[j] = complex.r[i] * norm;
                complex.i[j] = 0.0;

                complex.r[i] = tr;
                complex.i[i] = 0.0;
            }

            int m = n / 2;
            while (m >= 1 && j >= m) {
                j -= m;
                m /= 2;
            }
            j += m;
        }

        for (int mmax = 1, istep = 2 * mmax; mmax < n;
             mmax = istep, istep = 2 * mmax) {
            double delta = (Math.PI / mmax);
            for (int m = 0; m < mmax; m++) {
                double w = m * delta;
                double wr = Math.cos(w);
                double wi = Math.sin(w);

                for (int i = m; i < n; i += istep) {
                    int j = i + mmax;
                    double tr = wr * complex.r[j] - wi * complex.i[j];
                    double ti = wr * complex.i[j] + wi * complex.r[j];
                    complex.r[j] = complex.r[i] - tr;
                    complex.i[j] = complex.i[i] - ti;
                    complex.r[i] += tr;
                    complex.i[i] += ti;
                }
            }
        }
    }

    // Log2

    double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }

    // These two objects replace arrays of structs in the C version
    // because initialising arrays of objects in Java is, IMHO, barmy

// Complex

    class Complex {
        double r[];
        double i[];

        private Complex(int l) {
            r = new double[l];
            i = new double[l];
        }
    }

    class Maxima {
        double f[];
        double r[];
        int n[];

        protected Maxima(int l) {
            f = new double[l];
            r = new double[l];
            n = new int[l];
        }
    }
}