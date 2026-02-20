package dbmeterplugin;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * DBMeterPlugin - Cordova plugin to get decibel (dB) values from the microphone.
 *
 * This plugin provides the decibel level from the microphone for sound meter applications.
 */
public class DBMeterPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "DBMeterPlugin";
    private static final int REQ_CODE = 0;
    private static final int SAMPLE_INTERVAL_MS = 100;

    private AudioRecord audioRecord;
    private short[] buffer;
    private Timer timer;
    private boolean isListening = false;
    private CallbackContext startCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission();
            return true;
        }

        switch (action) {
            case "start":
                this.start(callbackContext);
                return true;
            case "getDbLevel":
                this.getDbLevel(callbackContext);
                return true;
            case "stop":
                this.stop(callbackContext);
                return true;
            case "isListening":
                this.isListening(callbackContext);
                return true;
            case "destroy":
                this.destroy(callbackContext);
                return true;
            default:
                LOG.e(LOG_TAG, "Not a valid action: " + action);
                return false;
        }
    }

    /**
     * Check if RECORD_AUDIO permission is granted
     */
    private boolean hasRecordAudioPermission() {
        return cordova.hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    /**
     * Request RECORD_AUDIO permission
     */
    private void requestRecordAudioPermission() {
        cordova.requestPermission(this, REQ_CODE, Manifest.permission.RECORD_AUDIO);
    }

    /**
     * Permits to free the memory from the audioRecord instance
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     */
    public void destroy(CallbackContext callbackContext) {
        if (this.audioRecord != null) {
            this.isListening = false;
            this.audioRecord.stop();
            this.audioRecord.release();
            this.audioRecord = null;
            if (this.timer != null) {
                this.timer.cancel();
                this.timer = null;
            }
            callbackContext.success();
        } else {
            sendPluginError(callbackContext, PluginError.DBMETER_NOT_INITIALIZED, "DBMeter is not initialized");
        }
    }

    /**
     * Initialize the audio recorder if not already initialized
     */
    private void initAudioRecorder() {
        if (this.audioRecord == null) {
            int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            this.audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            this.buffer = new short[bufferSize];
        }
    }

    /**
     * Calculate the dB level from the audio buffer
     *
     * @return The calculated dB value
     */
    private double calculateDbLevel() {
        int readSize = this.audioRecord.read(this.buffer, 0, this.buffer.length);
        double maxAmplitude = 0;

        for (int i = 0; i < readSize; i++) {
            if (Math.abs(this.buffer[i]) > maxAmplitude) {
                maxAmplitude = Math.abs(this.buffer[i]);
            }
        }

        double db = 0;
        if (maxAmplitude != 0) {
            db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90;
        }

        return db;
    }

    /**
     * Starts listening the audio signal and sends dB values as a
     * {@link org.apache.cordova.PluginResult PluginResult} using a
     * {@link java.util.TimerTask TimerTask}.
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     */
    public void start(final CallbackContext callbackContext) {
        this.startCallbackContext = callbackContext;
        final DBMeterPlugin that = this;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                that.initAudioRecorder();

                if (!that.isListening) {
                    that.isListening = true;
                    that.audioRecord.startRecording();

                    that.timer = new Timer(LOG_TAG, true);

                    TimerTask timerTask = new TimerTask() {
                        public void run() {
                            if (that.isListening && that.audioRecord != null) {
                                double db = that.calculateDbLevel();
                                LOG.d(LOG_TAG, Double.toString(db));

                                PluginResult result = new PluginResult(PluginResult.Status.OK, (float) db);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                            }
                        }
                    };
                    that.timer.scheduleAtFixedRate(timerTask, 0, SAMPLE_INTERVAL_MS);
                }
            }
        });
    }

    /**
     * Get a single dB level reading (Promise-based)
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     */
    public void getDbLevel(final CallbackContext callbackContext) {
        final DBMeterPlugin that = this;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                that.initAudioRecorder();

                boolean wasListening = that.isListening;
                if (!that.isListening) {
                    that.audioRecord.startRecording();
                }

                // Give it a moment to get a reading
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                double db = that.calculateDbLevel();

                // Stop if it wasn't listening before
                if (!wasListening) {
                    that.audioRecord.stop();
                }

                PluginResult result = new PluginResult(PluginResult.Status.OK, (float) db);
                callbackContext.sendPluginResult(result);
            }
        });
    }

    /**
     * Stops listening the audio signal.
     * Even if stopped, the {@link android.media.AudioRecord AudioRecord} instance still exists.
     * To destroy this instance, please use the {@link #destroy(CallbackContext) destroy} method.
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     */
    public void stop(final CallbackContext callbackContext) {
        final DBMeterPlugin that = this;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (that.isListening) {
                    that.isListening = false;

                    if (that.timer != null) {
                        that.timer.cancel();
                        that.timer = null;
                    }
                    if (that.audioRecord != null) {
                        that.audioRecord.stop();
                    }
                    callbackContext.success();
                } else {
                    sendPluginError(callbackContext, PluginError.DBMETER_NOT_LISTENING, "DBMeter is not listening");
                }
            }
        });
    }

    /**
     * Returns whether the DBMeter is listening.
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     */
    public void isListening(final CallbackContext callbackContext) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.isListening);
        callbackContext.sendPluginResult(result);
    }

    /**
     * Convenient method to send plugin errors.
     *
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @param error           The error code to return
     * @param message         The error message to return
     */
    private void sendPluginError(CallbackContext callbackContext, PluginError error, String message) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("code", error.ordinal());
            jsonObject.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callbackContext.error(jsonObject);
    }

    public enum PluginError {
        DBMETER_NOT_INITIALIZED,
        DBMETER_NOT_LISTENING
    }
}
