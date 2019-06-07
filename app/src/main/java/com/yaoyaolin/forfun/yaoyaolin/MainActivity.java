package com.yaoyaolin.forfun.yaoyaolin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SteeringWheelView.SteeringWheelListener {

    static final String TAG = "yaoyaolin-debug";

    private LocationManager mLocationManager;
    private Location mLastLocation;
    private SteeringWheelView mSteeringWheel;
    private Button mSettings, mUp, mDown, mLeft, mRight, mNanYi, mOK, mPukou;
    private TextView mPosition;

    private EditText mEdLat, mEdLng;
    private double mEdlatPosition, mEdlngPosition;

    private float move_oldX = -1000;
    private float move_oldY = -1000;
    public float StartX;
    public float StartY;
    private float ProX;
    private float ProY;
    private float moveX;
    private float moveY;
    private float mTouchStartX;
    private float mTouchStartY;
    private float touchX;
    private float touchY;
    private float x;
    private float y;
//    private double tsetLat = 32.0654177600;
//    private double testLng = 118.7492251200;

    //    //南艺
//    private static double DEFAULT_LAT = 32.0654177600;
//    private static double DEFAULT_LNG = 118.7492251200;
//    //石头城
    private static double DEFAULT_LAT = 32.06107831960026;
    private static double DEFAULT_LNG = 118.74595957309987;

    private double lastLat = 0;
    private double lastLng = 0;

    private static final double STEP_GAP = 0.0001;
    private static final int STEP_SPEC_1 = 1;
    private static final int STEP_SPEC_10 = 10;
    private static final int STEP_SPEC_100 = 100;

    private static final int RO_UP = 1;
    private static final int RO_DOWM = 2;
    private static final int RO_LEFT = 3;
    private static final int RO_RIGHT = 4;
    private Handler handler = new Handler();

    private int mStepIndex = 1;
    private int mRotation = 1;


    /*1 = 100km
    0.1 = 10km
    0.01 = 1km
    0.00001 = 1m*/

    private static final int MSG_START_SHOW_TIME = 1;
    private View floatWindowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNanYi = (Button) findViewById(R.id.nanyi);
        mPukou = (Button) findViewById(R.id.pukou);
        mOK = (Button) findViewById(R.id.ok);
        mPosition = (TextView) findViewById(R.id.position);
        mEdLat = (EditText) findViewById(R.id.edlat);
        mEdLng = (EditText) findViewById(R.id.edlng);
        mSettings = (Button) findViewById(R.id.settings);
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStepIndex == 1) {
                    mStepIndex = 2;
                    mSettings.setText("x2");
                } else if (mStepIndex == 2) {
                    mStepIndex = 3;
                    mSettings.setText("x3");
                } else if (mStepIndex == 3) {
                    mStepIndex = 5;
                    mSettings.setText("x5");
                } else if (mStepIndex == 5) {
                    mStepIndex = 1;
                    mSettings.setText("x1");
                }
            }
        });
//        addFloatView1();
        addFloatView();
        mOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mEdlatPosition != 0 && mEdlngPosition != 0) {
                    lastLat = mEdlatPosition;
                    lastLng = mEdlngPosition;
                }
            }
        });

        mEdLat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString() != null && !"".equals(editable.toString())) {
                    mEdlatPosition = Double.valueOf(editable.toString());
                    Log.d(TAG, "input lat:" + mEdlatPosition);
                }
            }
        });

        mEdLng.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString() != null && !"".equals(editable.toString())) {
                    mEdlngPosition = Double.valueOf(editable.toString());
                    Log.d(TAG, "input lng:" + mEdlngPosition);
                }
            }
        });


        mNanYi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                lastLat = 32.1061839942;
//                lastLng = 118.8405750236;
                lastLat = DEFAULT_LAT;
                lastLng = DEFAULT_LNG;
            }
        });
        mPukou.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastLat = 32.1102023429;
                lastLng = 118.7000463853;
            }
        });

        mLocationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "请打开GPS开关", Toast.LENGTH_SHORT).show();
        } else {

            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


            } else {
                Toast.makeText(this, "需要权限", Toast.LENGTH_SHORT).setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            checkPermission(this);
                    mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            lastLat = DEFAULT_LAT;
//            lastLng = DEFAULT_LNG;
            if (mLastLocation != null) {
                Log.d(TAG, "last lat=" + mLastLocation.getLatitude() + ";last lng=" + mLastLocation.getLongitude());
                lastLat = mLastLocation.getLatitude();
                lastLng = mLastLocation.getLongitude();
            } else {
                Log.w(TAG, "last known location is null?");

            }


            //mLocationManager.addTestProvider();
            mLocationManager.addTestProvider(LocationManager.GPS_PROVIDER
                    , false, true, false, false
                    , true, true, true, 0, 5);
            mLocationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, mLocationListener);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    int idex = 0;
                    while (true) {
                        updateMockLocation(lastLng, lastLat);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        idex++;
                    }
                }
            }).start();

            mHandler.sendEmptyMessageDelayed(MSG_START_SHOW_TIME, 1000);

        }


    }
    public static boolean checkPermission(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(activity)) {
            Toast.makeText(activity, "当前无权限，请授权", Toast.LENGTH_SHORT).show();
            activity.startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName())), 0);
            return false;
        }
        return true;
    }

    private boolean move_event = false;
    private boolean up_event = false;
    public int state;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;

    private void addFloatView() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowLayoutParams.x = 0;
        mWindowLayoutParams.y = 0;
        mWindowLayoutParams.format = PixelFormat.RGBA_8888;
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        floatWindowView = LayoutInflater.from(this).inflate(R.layout.drag, null);
        mUp = (Button) floatWindowView.findViewById(R.id.up);
        mDown = (Button) floatWindowView.findViewById(R.id.down);
        mLeft = (Button) floatWindowView.findViewById(R.id.left);
        mRight = (Button) floatWindowView.findViewById(R.id.right);
        mSettings = (Button) floatWindowView.findViewById(R.id.settings);

        floatWindowView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                x = event.getRawX();
                y = event.getRawY() - 50;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        move_event = false;
                        up_event = false;
                        state = MotionEvent.ACTION_DOWN;
                        StartX = x;
                        StartY = y;
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY();
                        touchX = mTouchStartX;
                        touchY = mTouchStartY;
                        ProX = event.getRawX();
                        ProY = event.getRawY();

                        break;
                    case MotionEvent.ACTION_MOVE:
                        state = MotionEvent.ACTION_MOVE;
                        moveX = event.getRawX();
                        moveY = event.getRawY();
                        final ViewConfiguration configuration = ViewConfiguration
                                .get(getApplicationContext());
                        int mTouchSlop = configuration.getScaledTouchSlop();
                        // 第一次move
                        if (move_oldX == -1000 && move_oldY == -1000) {
                            move_oldX = moveX;
                            move_oldY = moveY;
                            if (Math.abs(moveX - ProX) < mTouchSlop * 2
                                    && Math.abs(moveY - ProY) < mTouchSlop * 2) {
                                move_event = false;
                            } else {
                                move_event = true;
                                updateViewPosition();
                            }
                        } else {
                            if (move_event == false) {
                                if (Math.abs(moveX - move_oldX) < mTouchSlop * 2
                                        && Math.abs(moveY - move_oldY) < mTouchSlop * 2) {
                                    move_event = false;
                                } else {
                                    move_event = true;
                                    updateViewPosition();
                                }
                            } else {
                                updateViewPosition();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        state = MotionEvent.ACTION_UP;
                        updateViewPositionEnd();
                        move_oldX = -1000;
                        move_oldY = -1000;
                        mTouchStartX = mTouchStartY = 0;
                        up_event = true;

                        break;
                }

                return true;
            }
        });


        mUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation = 1;
                lastLat += (double) (mStepIndex * STEP_GAP);
//                updateMockLocation(lastLng, lastLat);

            }
        });

        mDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation = 2;
                lastLat -= (double) (mStepIndex * STEP_GAP);
//                updateMockLocation(lastLng, lastLat);

            }
        });

        mLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation = 3;
                lastLng -= (double) (mStepIndex * STEP_GAP);
//                updateMockLocation(lastLng, lastLat);

            }
        });

        mRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRotation = 4;
                lastLng += (double) (mStepIndex * STEP_GAP);
//                updateMockLocation(lastLng, lastLat);

            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStepIndex == 1) {
                    mStepIndex = 3;
                    mSettings.setText("x3");
                } else if (mStepIndex == 3) {
                    mStepIndex = 5;
                    mSettings.setText("x5");
                } else if (mStepIndex == 5) {
                    mStepIndex = 10;
                    mSettings.setText("x10");
                } else if (mStepIndex == 10) {
                    mStepIndex = 1;
                    mSettings.setText("x1");
                }
            }
        });
        mWindowManager.addView(floatWindowView, mWindowLayoutParams);

    }

    private SparseArray<String> mSparseArray = new SparseArray<>();

    private void initData() {
        mSparseArray.put(SteeringWheelView.LEFT, getString(R.string.left));
        mSparseArray.put(SteeringWheelView.UP, getString(R.string.up));
        mSparseArray.put(SteeringWheelView.RIGHT, getString(R.string.right));
        mSparseArray.put(SteeringWheelView.DOWN, getString(R.string.down));
        mSparseArray.put(SteeringWheelView.INVALID, getString(R.string.idle));
        mSteeringWheel.notifyInterval(16).listener(this).interpolator(new OvershootInterpolator());

    }

    private void addFloatView1() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowLayoutParams.x = 0;
        mWindowLayoutParams.y = 0;
        mWindowLayoutParams.format = PixelFormat.RGBA_8888;
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        floatWindowView = LayoutInflater.from(this).inflate(R.layout.floatview, null);
        mSteeringWheel = floatWindowView.findViewById(R.id.steeringWheelView);
        floatWindowView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                x = event.getRawX();
                y = event.getRawY() - 50;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        move_event = false;
                        up_event = false;
                        state = MotionEvent.ACTION_DOWN;
                        StartX = x;
                        StartY = y;
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY();
                        touchX = mTouchStartX;
                        touchY = mTouchStartY;
                        ProX = event.getRawX();
                        ProY = event.getRawY();

                        break;
                    case MotionEvent.ACTION_MOVE:
                        state = MotionEvent.ACTION_MOVE;
                        moveX = event.getRawX();
                        moveY = event.getRawY();
                        final ViewConfiguration configuration = ViewConfiguration
                                .get(getApplicationContext());
                        int mTouchSlop = configuration.getScaledTouchSlop();
                        // 第一次move
                        if (move_oldX == -1000 && move_oldY == -1000) {
                            move_oldX = moveX;
                            move_oldY = moveY;
                            if (Math.abs(moveX - ProX) < mTouchSlop * 2
                                    && Math.abs(moveY - ProY) < mTouchSlop * 2) {
                                move_event = false;
                            } else {
                                move_event = true;
                                updateViewPosition();
                            }
                        } else {
                            if (move_event == false) {
                                if (Math.abs(moveX - move_oldX) < mTouchSlop * 2
                                        && Math.abs(moveY - move_oldY) < mTouchSlop * 2) {
                                    move_event = false;
                                } else {
                                    move_event = true;
                                    updateViewPosition();
                                }
                            } else {
                                updateViewPosition();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        state = MotionEvent.ACTION_UP;
                        updateViewPositionEnd();
                        move_oldX = -1000;
                        move_oldY = -1000;
                        mTouchStartX = mTouchStartY = 0;
                        up_event = true;

                        break;
                }

                return true;
            }
        });

        initData();
        mWindowManager.addView(floatWindowView, mWindowLayoutParams);

    }


    private void updateViewPosition() {

        // 更新浮动窗口位置参数
        mWindowLayoutParams.x = (int) (x - mTouchStartX);
        mWindowLayoutParams.y = (int) (y - mTouchStartY);

        mWindowManager.updateViewLayout(floatWindowView, mWindowLayoutParams);
    }

    private void updateViewPositionEnd() {
        mWindowLayoutParams.x = (int) (x - mTouchStartX);
        mWindowLayoutParams.y = (int) (y - mTouchStartY);

        mWindowManager.updateViewLayout(floatWindowView, mWindowLayoutParams);
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_SHOW_TIME:
                    mHandler.sendEmptyMessageDelayed(MSG_START_SHOW_TIME, 1000);
                    break;
                default:

                    break;
            }
        }
    };

    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged:lat=" + location.getLatitude() + ";lng=" + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void setStep(int step) {
        mStepIndex = step;
    }

    private void updateMockLocation(double longitude, double latitude) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(5.0f);
        location.setAccuracy(5.0f);
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        mLocationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    private void setLocation(double longitude, double latitude, int rotation) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(System.currentTimeMillis());
        double tmpLat = 0;
        double tmpLng = 0;
        if (rotation == RO_UP) {
            tmpLat = latitude + (double) (mStepIndex * STEP_GAP);
            tmpLng = longitude;
            location.setLatitude(tmpLat);
            location.setLongitude(tmpLng);
        } else if (rotation == RO_DOWM) {
            tmpLat = latitude - (double) (mStepIndex * STEP_GAP);
            tmpLng = longitude;
            location.setLatitude(tmpLat);
            location.setLongitude(tmpLng);
        } else if (rotation == RO_LEFT) {
            tmpLat = latitude;
            tmpLng = longitude - (double) (mStepIndex * STEP_GAP);
            location.setLatitude(tmpLat);
            location.setLongitude(tmpLng);
        } else if (rotation == RO_RIGHT) {
            tmpLat = latitude;
            tmpLng = longitude + (double) (mStepIndex * STEP_GAP);
            location.setLatitude(latitude);
            location.setLongitude(tmpLng);
        } else {

        }
        location.setAltitude(2.0f);
        location.setAccuracy(3.0f);
        lastLat = tmpLat;
        lastLng = tmpLng;
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        mLocationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    @Override
    public void onStatusChanged(SteeringWheelView view, int angle, int power, int direction) {
        String text = constructText(angle, power, direction);
        mPosition.setText(text);
        if (direction == -1) {
            return;
        }
        mRotation = direction;
        if (direction == RO_UP) {
            right((90 - angle) / 45);
            up(1);
        } else if (direction == RO_DOWM) {
            left((270 - angle) / 45);
            down(1);
        }
        if (direction == RO_LEFT) {
            up((180 - angle) / 45);
            left(1);
        }
        if (direction == RO_RIGHT) {
            if (angle <= 45) {
                up(angle / 45);
            } else if (angle > 315) {
                down((angle - 315) / 45);
            }
            right(1);

        }
//        updatePosition();
    }


    private String constructText(int angle, int power, int direction) {
        return String.format(Locale.CHINESE, "angle = %3d\npower = %3d\ndirection = %s",
                angle, power, direction2Text(direction));
    }

    private String direction2Text(int direction) {
        return mSparseArray.get(direction);
    }

    private boolean canUpdate = true;

    private void updatePosition() {
//        if (!canUpdate) {
//            return;
//        }
//        canUpdate = false;
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                canUpdate = true;
//            }
//        }, 100);
        updateMockLocation(lastLng, lastLat);
    }

    private void up(float f) {
        mRotation = 1;
        lastLat += (double) (mStepIndex * STEP_GAP) * f;
        updateMockLocation(lastLng, lastLat);

    }

    private void down(float f) {
        mRotation = 2;
        lastLat -= (double) (mStepIndex * STEP_GAP) * f;
        updateMockLocation(lastLng, lastLat);

    }

    private void left(float f) {
        mRotation = 3;
        lastLng -= (double) (mStepIndex * STEP_GAP) * f;
        updateMockLocation(lastLng, lastLat);

    }

    private void right(float f) {
        mRotation = 4;
        lastLng += (double) (mStepIndex * STEP_GAP) * f;
        updateMockLocation(lastLng, lastLat);

    }


}
