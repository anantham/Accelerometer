package edu.nitt.spider.accelerometer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class Home extends AppCompatActivity  implements SensorEventListener {
    private static final String HOME = "Logging from HOME";

    private SensorManager mSensorManager;

    TextView a_X;
    TextView a_Y;
    TextView a_Z;
    TextView a_gX;
    TextView a_gY;
    TextView a_gZ;
    TextView errorText;

    // the gravity vector expressed in the device's coordinate
    //float[] mGravity = new float[3];
    float[] Rotation = new float[9];
    float[] Inclination = new float[9];

    float[] acceleration = new float[3];
    float[] rotationRate = new float[3];
    float[] magneticField = new float[3];
    private long currentTime;
    private long startTime;

    File myFile;
    FileOutputStream fOut;
    OutputStreamWriter myOutWriter;
    BufferedWriter myBufferedWriter;
    PrintWriter myPrintWriter;

    Button startButton;
    Button stopButton;

    boolean stopFlag = false;
    boolean startFlag = false;
    boolean isFirstSet = true;


    String fileName = "mySensorData";

    // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Checks if the app has permission to write to device storage
    // If the app does not has permission then the user will be prompted to grant permissions
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        a_X = (TextView) findViewById(R.id.textViewX);
        a_Y = (TextView) findViewById(R.id.textViewY);
        a_Z = (TextView) findViewById(R.id.textViewZ);

        a_gX = (TextView) findViewById(R.id.textViewgX);
        a_gY = (TextView) findViewById(R.id.textViewgY);
        a_gZ = (TextView) findViewById(R.id.textViewgZ);

        errorText = (TextView) findViewById(R.id.textViewError);

        startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                try {
                    File root = android.os.Environment.getExternalStorageDirectory();
                    Log.d(HOME,"\nExternal file system root: " + root.getAbsolutePath() + "/download/"+ fileName + ".txt");

                    myFile = new File(root.getAbsolutePath() + "/download/"+ fileName + ".txt");
                    if (!myFile.exists()) {
                        try {
                            Log.d(HOME,"The result of createNewFile is "+myFile.createNewFile());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    fOut = new FileOutputStream(myFile);
                    myOutWriter = new OutputStreamWriter(fOut);
                    myBufferedWriter = new BufferedWriter(myOutWriter);
                    myPrintWriter = new PrintWriter(myBufferedWriter);
                    myPrintWriter.write("TimeInMilliSeconds"
                                    + "," + "Acc X" + "," + "Acc Y" + "," + "Acc Z"
                                    + "," + "Rot X" + "," + "Rot Y" + "," + "Rot Z"
                                    + "," + "Mag X" + "," + "Mag Y" + "," + "Mag Z" + "\n");

                    Log.d(HOME,"Started recording the data set");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    startFlag = true;
                }
                Log.d(HOME,"all things needed to write to file is ready");
            }
        });

        stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // stop recording the sensor data
                try {
                    stopFlag = true;
                    Log.d(HOME,"Setting stopFlag, stop logging sensor data");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Home.verifyStoragePermissions(this);
        checkExternalMedia();
    }

    // To prevent battery loss, unregister listener when app isn't in the foreground
    protected void onResume() {
        super.onResume();
        // a sample every 0.2 secs
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(HOME,"register listener's - onResume");
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        Log.d(HOME," Unregister all listeners - onPause ");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acceleration[0] = event.values[0];
                acceleration[1] = event.values[1];
                acceleration[2] = event.values[2];

                // in acceleration the last value would be 0, just because we want to multiply with inv later
                //deviceRelativeAcceleration[3] = 0; //WHY?? TODO

                //a_X.setText(String.format(Locale.US, "%f", acceleration[0]));
                //a_Y.setText(String.format(Locale.US, "%f", acceleration[1]));
                //a_Z.setText(String.format(Locale.US, "%f", acceleration[2]));
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticField[0] = event.values[0];
                magneticField[1] = event.values[1];
                magneticField[2] = event.values[2];

                a_X.setText(String.format(Locale.US, "%f", magneticField[0]));
                a_Y.setText(String.format(Locale.US, "%f", magneticField[1]));
                a_Z.setText(String.format(Locale.US, "%f", magneticField[2]));
                break;

            case Sensor.TYPE_GYROSCOPE:
                rotationRate[0] = event.values[0];
                rotationRate[1] = event.values[1];
                rotationRate[2] = event.values[2];

                a_gX.setText(String.format(Locale.US, "%f", rotationRate[0]));
                a_gY.setText(String.format(Locale.US, "%f", rotationRate[1]));
                a_gZ.setText(String.format(Locale.US, "%f", rotationRate[2]));

            default:
                Log.d(HOME, "We saw a uncaught sensor Event, ==" + event.sensor.getType());
                return;
        }

        if(!startFlag){
            //Not ready to start recording, need to get file ready
            return;
        }

        // note down time - used when saving the samples
        if (isFirstSet) {
            startTime = System.currentTimeMillis();
            isFirstSet = false;
        }

        currentTime = System.currentTimeMillis();

        for (int i = 0; i < 1; i++) {
            // Save the sample to file else close the file pointers
            if (!stopFlag) {
                save();
            }
            else {
                try {
                    myOutWriter.close();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
                try {
                    fOut.close();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }

/*
        // Change the device relative acceleration values to earth relative values
        // X axis -> East
        // Y axis -> North Pole
        // Z axis -> Sky

        // Computes the inclination matrix as well as the rotation matrix.
        Boolean result = SensorManager.getRotationMatrix(Rotation, Inclination, deviceRelativeAcceleration, mGeoMagnetic);
        if(result){
            Log.i(HOME," Computed the inclination matrix as well as the rotation matrix successfully = "+result);
            a_gX.setText(String.format(Locale.US,"%f",Rotation[1]));
            a_gY.setText(String.format(Locale.US,"%f",Rotation[2]));
            a_gZ.setText(String.format(Locale.US,"%f",Rotation[3]));
            Log.d(HOME,Float.toString(Rotation[0]));

            float[] inv = new float[9];
            // We invert Rotation, store it in inv
            if(android.opengl.Matrix.invertM(inv, 0, Rotation, 0)){
                Log.i(HOME,"inverted Rotation matrix successfully");
                float[] earthAcc = new float[9];
                // earthAcc = (inv-Matrix)_{4*4}*(deviceRelativeAcceleration-Vector)_{4*1}
                try{
                    android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

                    errorText.setText(String.format(Locale.US,"%f",earthAcc[0]));
                    a_gX.setText(String.format(Locale.US,"%f",earthAcc[1]));
                    a_gY.setText(String.format(Locale.US,"%f",earthAcc[2]));
                    a_gZ.setText(String.format(Locale.US,"%f",earthAcc[3]));
                }
                catch(IllegalArgumentException e){
                    Log.d(HOME,e.toString());
                }
            }
            else{
                Log.i(HOME,"inversion of Rotation matrix Failed");
                return;
            }
        }
        else{
            Log.i(HOME,"SensorManager.getRotationMatrix - Failed");
            return;
        }

        float[] values = new float[3];
        getOrientation(Rotation, values);
*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(HOME,"The accuracy value has changed, new value is " + Integer.toString(accuracy));
    }

    // Checks and logs status of external storage
    private void checkExternalMedia() {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        Log.d(HOME, "\n\nExternal Media: status of readable = " + mExternalStorageAvailable + " AND status of writable = " + mExternalStorageWriteable);
    }

    // Time in ms | Acc X | Acc Y | Acc Z |
    private void save() {

        myPrintWriter.write(currentTime - startTime
                + "," + acceleration[0] + "," + acceleration[1] + "," + acceleration[2]
                + "," + rotationRate[0] + "," + rotationRate[1] + "," + rotationRate[2]
                + "," + magneticField[0] + "," + magneticField[1] + "," + magneticField[2] + "\n");
    }

}