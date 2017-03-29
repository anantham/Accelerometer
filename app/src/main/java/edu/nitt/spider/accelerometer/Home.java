package edu.nitt.spider.accelerometer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Locale;

import static android.hardware.SensorManager.getOrientation;


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

    float[] Rotation = new float[16];
    float[] Inclination = new float[16];

    float[] acceleration = new float[3];
    float[] gyroscope = new float[3];
    float[] magneticField = new float[3];
    float[] gravity = new float[3];

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
    String checkCache = "try2";

    float[] deviceGravity = new float[3];
    float[] deviceAcceleration = new float[3];
    float[] earthGravity = new float[3];
    float[] earthAcceleration = new float[3];

    //Dynamic Threshold and Dynamic Precision
    float[] dynThres = new float[3];
    float[] dynPres = new float[3];

    float[] runningMax = new float[3];
    float[] runningMin = new float[3];

    float[] actualMax = new float[3];
    float[] actualMin = new float[3];

    //  Overall Max/Min and Dynamic Max/Min
    // how dynamic these parameters are TODO adjust and try find optimal value
    public static final int frequency = 50;
    // Number of samples
    int noSamples;



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
        errorText.setText(checkCache);

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
                                    + "," + "earthGravity X" + "," + "earthGravity Y" + "," + "earthGravity Z"
                                    + "," + "earthAcceleration X" + "," + "earthAcceleration Y" + "," + "earthAcceleration Z"
                                    + "," + "Rot X" + "," + "Rot Y" + "," + "Rot Z"
                                    + "," + "Mag X" + "," + "Mag Y" + "," + "Mag Z" + "\n");

                    Log.d(HOME,"Started recording the data set");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    startFlag = true;
                }
                Log.d(HOME,"all things needed to write to file is ready");
                Toast.makeText(v.getContext(),"Start writing sensor values to file",Toast.LENGTH_SHORT).show();
                noSamples = 0; // Reset the count
                // Set the max min value
                runningMax = runningMin = actualMax = actualMin = new float[] {0,0,0};
            }
        });

        stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // stop recording the sensor data
                try {
                    stopFlag = true;
                    Log.d(HOME,"Setting stopFlag, stop logging sensor data");
                    Toast.makeText(v.getContext(),"STOP writing sensor values to file",Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Home.verifyStoragePermissions(this);
        checkExternalMedia();
    }

    // To prevent battery loss, unregister listener when app isn't in the foreground
    @Override
    protected void onResume() {
        super.onResume();
        // a sample every 0.2 secs
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
        Log.d(HOME,"register listener's - onResume");
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
        Log.d(HOME," Unregister all listeners - onPause ");
        Toast.makeText(this,"Unregister all Sensor listeners",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Activity")
                .setMessage("Are you sure you want to close this activity?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acceleration[0] = event.values[0];
                acceleration[1] = event.values[1];
                acceleration[2] = event.values[2];
                break;

            case Sensor.TYPE_GRAVITY:
                gravity[0] = event.values[0];
                gravity[1] = event.values[1];
                gravity[2] = event.values[2];
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticField[0] = event.values[0];
                magneticField[1] = event.values[1];
                magneticField[2] = event.values[2];
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyroscope[0] = event.values[0];
                gyroscope[1] = event.values[1];
                gyroscope[2] = event.values[2];
                break;

            default:
                Log.d(HOME, "We saw a uncaught sensor Event, ==" + event.sensor.getType());
                return;
        }

        // Change the device relative acceleration values to earth relative values
        // X axis -> East -- Right Swipe
        // Y axis -> North Pole -- Top of phone
        // Z axis -> Sky -- Out of Phone

        // Computes the inclination matrix as well as the rotation matrix.
        Boolean result = SensorManager.getRotationMatrix(Rotation, Inclination, gravity, magneticField);
        if (result) {
            //Log.i(HOME, " Computed the inclination matrix as well as the rotation matrix successfully = " + result);

            String RotationString = Float.toString(Rotation[0]) + " " + Float.toString(Rotation[1])
                    + " " + Float.toString(Rotation[2]) + " " + Float.toString(Rotation[3])
                    + " " + Float.toString(Rotation[4]) + " " + Float.toString(Rotation[5])
                    + Float.toString(Rotation[6]) + " " + Float.toString(Rotation[7])
                    + " " + Float.toString(Rotation[8]) + " " + Float.toString(Rotation[9])
                    + " " + Float.toString(Rotation[10]) + " " + Float.toString(Rotation[11])
                    + Float.toString(Rotation[12]) + " " + Float.toString(Rotation[13]) + " "
                    + Float.toString(Rotation[14]) + " " + Float.toString(Rotation[15]);

            //Log.d(HOME,"\n\nROTATION MATRIX = " +RotationString);

            String InclinationString = Float.toString(Inclination[0]) + " " + Float.toString(Inclination[1])
                    + " " + Float.toString(Inclination[2]) + " " + Float.toString(Inclination[3])
                    + " " + Float.toString(Inclination[4]) + " " + Float.toString(Inclination[5])
                    + Float.toString(Inclination[6]) + " " + Float.toString(Inclination[7])
                    + " " + Float.toString(Inclination[8]) + " " + Float.toString(Inclination[9])
                    + " " + Float.toString(Inclination[10]) + " " + Float.toString(Inclination[11])
                    + Float.toString(Inclination[12]) + " " + Float.toString(Inclination[13]) + " "
                    + Float.toString(Inclination[14]) + " " + Float.toString(Inclination[15]);

            //Log.d(HOME,"\n\nINCLINATION MATRIX = "+ InclinationString);

            deviceGravity[0] = gravity[0];
            deviceGravity[1] = gravity[1];
            deviceGravity[2] = gravity[2];

            earthGravity[0] = Rotation[0]*deviceGravity[0]+Rotation[1]*deviceGravity[1]+Rotation[2]*deviceGravity[2];
            earthGravity[1] = Rotation[4]*deviceGravity[0]+Rotation[5]*deviceGravity[1]+Rotation[6]*deviceGravity[2];
            earthGravity[2] = Rotation[8]*deviceGravity[0]+Rotation[9]*deviceGravity[1]+Rotation[10]*deviceGravity[2];

            deviceAcceleration[0] = acceleration[0];
            deviceAcceleration[1] = acceleration[1];
            deviceAcceleration[2] = acceleration[2];

            earthAcceleration[0] = Rotation[0]*deviceAcceleration[0]+Rotation[1]*deviceAcceleration[1]+Rotation[2]*deviceAcceleration[2];
            earthAcceleration[1] = Rotation[4]*deviceAcceleration[0]+Rotation[5]*deviceAcceleration[1]+Rotation[6]*deviceAcceleration[2];
            earthAcceleration[2] = Rotation[8]*deviceAcceleration[0]+Rotation[9]*deviceAcceleration[1]+Rotation[10]*deviceAcceleration[2];

            earthAcceleration[0] = earthAcceleration[0] - earthGravity[0];
            earthAcceleration[1] = earthAcceleration[1] - earthGravity[1];
            earthAcceleration[2] = earthAcceleration[2] - earthGravity[2];

            a_X.setText(String.format(Locale.US, "%f", acceleration[0]));
            a_Y.setText(String.format(Locale.US, "%f", acceleration[1]));
            a_Z.setText(String.format(Locale.US, "%f", acceleration[2]));

            a_gX.setText(String.format(Locale.US, "%f", earthAcceleration[0]));
            a_gY.setText(String.format(Locale.US, "%f", earthAcceleration[1]));
            a_gZ.setText(String.format(Locale.US, "%f", earthAcceleration[2]));

        }

        float[] values = new float[3];
        float[] check = new float[3];
        check = getOrientation(Rotation, values);
        // check is same as values
        //Log.d(HOME, "The getOrientation returns - "+(values[0]==check[0])+" "+(values[1]==check[1])+" "+(values[2]==check[2]));

        // Azimuth a.k.a Yaw, angle around earth Z axis (rotating both X and Y of device)
        // angle between device Y and earth Y in earth XY plane. Earth Y to Earth X is +ve angle
        float Yaw = (float) ((180/Math.PI)*values[0]);
        // Pitch, angle around X axis
        // angle between TODO Confirm this In YZ plane, earth Y to negative Z is positive
        float Pitch = (float) ((180/Math.PI)*values[1]);
        // Roll, angle around Y axis. Angle between the 2 YZ plane (device to group)
        // bending the plane towards (-ve) X axis of earth is positive
        float Roll = (float) ((180/Math.PI)*values[2]);

        // Show these angles on screen
        /*a_gX.setText(String.format(Locale.US, "%f", Pitch));
        a_gY.setText(String.format(Locale.US, "%f", Roll));
        a_gZ.setText(String.format(Locale.US, "%f", Yaw));*/

        //Log.d(HOME,"\nYaw (around Z) = "+Yaw+" Pitch (Around X) = "+Pitch+"Roll (Around Y) = "+Roll);

        if (!startFlag) {
                //Not ready to start recording, need to get file ready
                return;
        }

        // Count this sample
        noSamples++;

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
            } else {
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

        //With some experiments we note that Z axis has the largest (relatively) periodic acceleration changes

        // Filter values TODO Averaging the values?


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(HOME,"The accuracy value has changed, new value is " + Integer.toString(accuracy));
    }

    // Checks and logs status of external storage
    private void checkExternalMedia() {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWritable;

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWritable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWritable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWritable = false;
        }

        Log.d(HOME, "\n\nExternal Media: status of readable = " + mExternalStorageAvailable + " AND status of writable = " + mExternalStorageWritable);
    }

    // Time in ms | Acc X | Acc Y | Acc Z |
    private void save() {

        myPrintWriter.write(currentTime - startTime
                + "," + acceleration[0] + "," + acceleration[1] + "," + acceleration[2]
                + "," + earthGravity[0] + "," + earthGravity[1] + "," + earthGravity[2]
                + "," + earthAcceleration[0] + "," + earthAcceleration[1] + "," + earthAcceleration[2]
                + "," + gyroscope[0] + "," + gyroscope[1] + "," + gyroscope[2]
                + "," + magneticField[0] + "," + magneticField[1] + "," + magneticField[2] + "\n");
    }
}