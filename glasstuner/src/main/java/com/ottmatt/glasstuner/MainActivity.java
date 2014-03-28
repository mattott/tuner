package com.ottmatt.glasstuner;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private Audio audio;
    private Spectrum spectrum;
    private Display display;
    private Strobe strobe;
    private Status status;
    private Meter meter;
    private Scope scope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glass_activity_main);

        spectrum = (Spectrum) findViewById(org.billthefarmer.tuner.R.id.spectrum);
        display = (Display) findViewById(org.billthefarmer.tuner.R.id.display);
        scope = (Scope) findViewById(org.billthefarmer.tuner.R.id.scope);

        // Create audio
        audio = new Audio(getResources(), spectrum, display, scope);

        // Connect views to audio

        if (spectrum != null)
            spectrum.setAudio(audio);

        if (display != null)
            display.setAudio(audio);

        if (strobe != null)
            strobe.setAudio(audio);

        if (status != null)
            status.setAudio(audio);

        if (meter != null)
            meter.setAudio(audio);

        if (scope != null)
            scope.setAudio(audio);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update status

        if (status != null)
            status.invalidate();

        // Start the audio thread

        audio.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        audio.stop();
    }

    // On stop

    @Override
    protected void onStop() {
        super.onStop();
    }

    // On destroy

    @Override
    protected void onDestroy() {
        super.onDestroy();

        audio = null;
        scope = null;
        spectrum = null;
        display = null;
        strobe = null;
        meter = null;
        status = null;

        // Hint that it might be a good idea
        System.runFinalization();
    }
}
