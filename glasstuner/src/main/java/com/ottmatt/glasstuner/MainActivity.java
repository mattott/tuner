package com.ottmatt.glasstuner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private static final String PREF_INPUT = "pref_input";
    private static final String PREF_REFERENCE = "pref_reference";
    private static final String PREF_FILTER = "pref_filter";
    private static final String PREF_DOWNSAMPLE = "pref_downsample";
    private static final String PREF_MULTIPLE = "pref_multiple";
    private static final String PREF_SCREEN = "pref_screen";
    private static final String PREF_STROBE = "pref_strobe";
    private static final String PREF_ZOOM = "pref_zoom";

    private Audio audio;
    private Spectrum spectrum;
    private Display display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glass_activity_main);

        spectrum = (Spectrum) findViewById(org.billthefarmer.tuner.R.id.spectrum);
        display = (Display) findViewById(org.billthefarmer.tuner.R.id.display);

        // Create audio
        audio = new Audio(getResources(), spectrum, display);

        // Connect views to audio

        if (spectrum != null)
            spectrum.setAudio(audio);

        if (display != null)
            display.setAudio(audio);

    }

    @Override
    protected void onResume() {
        super.onResume();

        getPreferences();

        // Start the audio thread

        audio.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
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
        spectrum = null;
        display = null;

        // Hint that it might be a good idea
        System.runFinalization();
    }

    // Save preferences

    void savePreferences() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(PREF_FILTER, audio.filter);
        editor.putBoolean(PREF_DOWNSAMPLE, audio.downsample);
        editor.putBoolean(PREF_MULTIPLE, audio.multiple);
        editor.putBoolean(PREF_SCREEN, audio.screen);
        editor.putBoolean(PREF_STROBE, audio.strobe);
        editor.putBoolean(PREF_ZOOM, audio.zoom);

        editor.commit();
    }

    // Get preferences

    void getPreferences() {
        // Load preferences

        PreferenceManager.setDefaultValues(this, org.billthefarmer.tuner.R.xml.preferences, false);

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        // Set preferences

        if (audio != null) {
            audio.input = Integer.parseInt(preferences.getString(PREF_INPUT, "0"));
            audio.reference = preferences.getInt(PREF_REFERENCE, 440);
            audio.filter = preferences.getBoolean(PREF_FILTER, false);
            audio.downsample = preferences.getBoolean(PREF_DOWNSAMPLE, false);
            audio.multiple = preferences.getBoolean(PREF_MULTIPLE, false);
            audio.screen = preferences.getBoolean(PREF_SCREEN, false);
            audio.strobe = preferences.getBoolean(PREF_STROBE, false);
            audio.zoom = preferences.getBoolean(PREF_ZOOM, true);

            // Check screen

            if (audio.screen) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                Window window = getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }
}
