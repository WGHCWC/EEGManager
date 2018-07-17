package com.example.wghcwc.eeg_manager;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements OnNavigationItemSelectedListener, View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int BLUETOOTH_TURN_ON_ERROR = 99;
    private static final int BT_DATA_RECEIVE = 98;
    private static final int BT_DATA_TEST = 66;
    private static final int BT_DATA_BLINK = 77;
    private static final int BT_DATA_ATTENTION = 55;
    private static final int BT_DATA_MEDITATION = 44;
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> mBTDecices;
    private EditText ed_send_data;
    private ListView lv_dialog;
    private TextView tv_receive_data;
    private Button bt_cancel;
    private Button search;
    private Dialog mDialog;
    private BTDeviceAdapter mAdapter;
    private ConnectDevice connectDevice;
    private InputStream mInStream;
    private OutputStream mOutStream;
    private Button bt_send;
    private Button bt_next;
    private TextView tv_blink;
    private int Delta;
    private int Theta;
    private int LowAlpha;
    private int HighAlpha;
    private int LowBeta;
    private int HighBeta;
    private int LowGamma;
    private int MiddleGamma;
    private int Attention;
    private int Meditation;
    private int signal;
    private int rawdata;
    private int lastRawData;
    private StringBuffer stringBuffer;
    private ByteArrayOutputStream bos;
    private FileOutputStream outputStream;
    private FileInputStream inputStream;
    private DynamicLineChartManager dynamicLineChartManager2;
    private List<Integer> list = new ArrayList<>(); //数据集合
    private List<String> names = new ArrayList<>(); //折线名字集合
    private List<Integer> colour = new ArrayList<>();//折线颜色集合
    private DrawWaveView waveView = null;
    private LinearLayout wave_layout;
    private String[] p;
    int i = 0;
    private File file;
    private int size = 100;
    private int high = 500;
    private int low = -200;
    private boolean start;
    private int blink_times = 1;
    private int starts = 0;
    private int dec = 100;
    private int len = 1000;
    private int end = 0;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case BT_DATA_RECEIVE:
                    updateWaveView(msg.arg1);

                    break;
                case BT_DATA_TEST:
                    // updateWaveView(msg.arg1);

                    break;
                case BT_DATA_BLINK:
                    String ti = "眨眼:"+blink_times;
                    tv_receive_data.setText(ti);
                    break;
                case BT_DATA_MEDITATION:


                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        PermissionUtil.getLocationPermissions(this, 22);
        PermissionUtil.getExternalStoragePermissions(this, 4);
        initBTAdapter();
        wave_layout = (LinearLayout) findViewById(R.id.wave_layout);
        setUpDrawWaveView();
        initLinerChart();

    }


    private void initLinerChart() {

        LineChart mChart2 = (LineChart) findViewById(R.id.lin);
        //折线名字
        names.add("专注度");
        names.add("放松度");

        //折线颜色
        colour.add(Color.GREEN);
        colour.add(Color.BLUE);

        dynamicLineChartManager2 = new DynamicLineChartManager(mChart2, names, colour);

        dynamicLineChartManager2.setYAxis(100, 0, 10);
    }


    /*
    * 开启广播接收,开始搜索蓝牙设备
    */
    private void searchBTDevice() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceive, filter);
        mBluetoothAdapter.startDiscovery();
        mAdapter = new BTDeviceAdapter(this, mBTDecices);
        View v = View.inflate(this, R.layout.dialog_layout, null);
        lv_dialog = v.findViewById(R.id.lv_dialog);
        mBTDecices.clear();
        lv_dialog.setAdapter(mAdapter);
        mDialog = new Dialog(this);
        mDialog.setContentView(v);
        mDialog.show();


        lv_dialog.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int i, long l) {
                BluetoothDevice device = mBTDecices.get(i);
                connectDevice = new ConnectDevice(device);
                mBluetoothAdapter.cancelDiscovery();
                connectDevice.start();
                mDialog.dismiss();

            }
        });

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
        });

    }

    public void setUpDrawWaveView() {
        waveView = new DrawWaveView(getApplicationContext());
        wave_layout.addView(waveView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        waveView.setValue(2048, 1000, -1000);
    }

    public void updateWaveView(int data) {
        if (waveView != null) {
            waveView.updateData(data);
        }
    }

    private void initBTAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ed_send_data = findViewById(R.id.ed_send_data);
        tv_receive_data = findViewById(R.id.tv_receive_data);
        tv_blink = findViewById(R.id.tv_blink);
    /*    search = findViewById(R.id.bt_search);
        bt_send = findViewById(R.id.bt_send);

        bt_cancel = findViewById(R.id.bt_cancel);

        bt_next = findViewById(R.id.bt_next);
        bt_next.setOnClickListener(this);
        bt_cancel.setOnClickListener(this);
        search.setOnClickListener(this);
        bt_send.setOnClickListener(this);*/
        if (mBluetoothAdapter == null) {
            showToast("没有找到蓝牙设备");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, BLUETOOTH_TURN_ON_ERROR);
        } else {
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            mBTDecices = new ArrayList<>();
            mBTDecices.addAll(pairedDevices);


        }


    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLUETOOTH_TURN_ON_ERROR) {
            showToast("设备打开异常");
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    /*
    * 注册广播
    *
    * */

    private final BroadcastReceiver mReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                if (!mBTDecices.contains(device))
                    mBTDecices.add(device);

                mAdapter.notifyDataSetChanged();

            }
        }
    };

    /*
    * 创建socket链接
    *
    * */


    private class ConnectDevice extends Thread {
        private final BluetoothSocket mBluetoothSocket;
        private final BluetoothDevice mmBluetoothDevice;

        double times = 0;
        double err = 0;

        public ConnectDevice(BluetoothDevice mBluetoothDevice) {
            mmBluetoothDevice = mBluetoothDevice;
            InputStream inTemp = null;
            OutputStream outTemp = null;
            BluetoothSocket tmp = null;
            try {
                tmp = mmBluetoothDevice.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                inTemp = tmp.getInputStream();
                outTemp = tmp.getOutputStream();
                showToast("准备连接" + mBluetoothDevice.getName());
                Log.d(TAG, "ConnectDevice: csva");
            } catch (Exception e) {
                showToast("失败" + mBluetoothDevice.getName());
            }

            mBluetoothSocket = tmp;
            mInStream = inTemp;
            mOutStream = outTemp;
            try {
                mOutStream = mBluetoothSocket.getOutputStream();
                Log.d(TAG, "ConnectDevice: 输出流成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            Log.d(TAG, "run: 连接");
            mBluetoothAdapter.cancelDiscovery();
            byte[] buffer = new byte[1024];
            int bytes;
            int xxHigh, xxLow, xxchecksum;
            try {


                mBluetoothSocket.connect();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("连接成功");
                    }
                });

                bos = new ByteArrayOutputStream();
                stringBuffer = new StringBuffer();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                Date date = new Date(System.currentTimeMillis());
                File file1 = new File(Environment.getExternalStorageDirectory().getPath() + "/bluetooh");
                if (!file1.exists()) {
                    file1.mkdirs();
                }
                file = new File(file1.getPath(),
                        mmBluetoothDevice.getName() + simpleDateFormat.format(date) + ".txt");
                outputStream = new FileOutputStream(file, true);
                blink_times = 0;

            } catch (Exception e) {
                try {
                    mBluetoothSocket.close();
                    Log.d(TAG, "run: 失败");
                } catch (Exception eo) {
                }

                return;
            }
            Log.d(TAG, "run: 开始监听");
            while (true) {
                boolean SPackage = false;
                boolean BPackage = false;
                boolean isContent = false;


                try {

                    if ((bytes = mInStream.read(buffer)) > 0) {


                        for (int i = 0; i < bytes; i++) {
                            if (!SPackage && !BPackage) {
                                switch (buffer[i] & 0xff) {
                                    case 0xaa:
                                        if ((buffer[++i] & 0xff) == 0xaa)
                                            isContent = true;
                                        break;
                                    case 0x04:
                                        if (isContent) {
                                            if ((buffer[++i] & 0xff) == 0x80 && (buffer[++i] & 0xff) == 0x02) {
                                                SPackage = true;
                                            } else {
                                                isContent = false;
                                            }
                                        }
                                        break;
                                    case 0x20:
                                        if (isContent) {
                                            if ((buffer[++i] & 0xff) == 0x02) {
                                                BPackage = true;
                                            } else {
                                                isContent = false;
                                            }
                                        }
                                        break;
                                }

                            } else {
                                if (SPackage) {
                                    xxHigh = buffer[i];
                                    xxLow = buffer[++i];
                                    xxchecksum = buffer[++i] & 0xff;
                                    if (xxchecksum == (((0x80 + 0x02 + (xxHigh & 0xff) + xxLow & (0xff)) ^ 0xffffffff) & 0xff)) {
                                        rawdata = (xxHigh << 8) | xxLow;
                                        if (rawdata > 32768) {
                                            rawdata = 65536;
                                        }
                                        String val = rawdata + ",";
                                        bos.write(val.getBytes());
                                        if (bos.size() > 1024)

                                        {
                                            outputStream.write(bos.toString().getBytes());
                                            bos.reset();

                                        }

                                        if (rawdata > high && rawdata < (high + dec)) {
                                            start = true;
                                        }
                                        if (start) {
                                            starts++;
                                        }
                                        if (rawdata < low && rawdata > (low - dec) && start) {
                                            start = false;
                                            if (size > starts) {


                                                Message message1 = mHandler.obtainMessage();
                                                message1.what = BT_DATA_BLINK;
                                                message1.arg1 = blink_times++;
                                                mHandler.sendMessage(message1);

                                            }
                                            starts = 0;
                                        }
                                        Message message = mHandler.obtainMessage();
                                        message.what = BT_DATA_RECEIVE;
                                        message.arg1 = rawdata;
                                        mHandler.sendMessage(message);
                                        times++;

                                    }
                                    isContent = false;
                                    SPackage = false;
                                    //  Log.d(TAG, "run: 小包");


                                }
                                if (BPackage) {

                                    signal = buffer[i] & 0xff;
                                    if ((buffer[++i] & 0xff) == 0x83 && (buffer[++i] & 0xff) == 0x18) {
                                        Log.d(TAG, "run: EEG Power 开始");
                                    } else {
                                        isContent = false;
                                        BPackage = false;
                                        Log.e(TAG, "run: >>>>>>>>>>>>>>>>>>ERROR>>>>>>");
                                        continue;
                                    }
                                    Delta = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    Theta = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    LowAlpha = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    HighAlpha = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    LowBeta = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    HighBeta = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    LowGamma = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    MiddleGamma = ((buffer[++i] & 0xff) << 16) | ((buffer[++i] & 0xff) << 8) | (buffer[++i] & 0xff);
                                    if ((buffer[++i] & 0xff) == 0x04) {
                                        Attention = (buffer[++i]);
                                    }
                                    if ((buffer[++i] & 0xff) == 0x05) {
                                        Meditation = (buffer[++i]);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                list.add(Attention);
                                                list.add(Meditation);
                                                dynamicLineChartManager2.addEntry(list);
                                                list.clear();
                                            }
                                        });


                                    }

                                }
                                isContent = false;
                                BPackage = false;
                            }
                        }
                    }

                } catch (Exception e) {
                    try {
                        outputStream.write(bos.toString().getBytes());
                        bos.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);
                Log.d(TAG, "write: 发送成功");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {
            try {
                mBluetoothSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
           /* case R.id.bt_search:
                blink_times = 0;

                String[] va = ed_send_data.getText().toString().split("@");
                if (va.length > 1) {
                    size = Integer.parseInt(va[0]);
                    high = Integer.parseInt(va[1]);
                    low = Integer.parseInt(va[2]);
                    dec = Integer.parseInt(va[3]);
                }
                searchBTDevice();
                break;
            case R.id.bt_send:
                i = 1;
                try {
                    inputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                String con = StreamUtil.Stream2String(inputStream);
                p = con.split(",");


                waveView.clear();

                break;
            case R.id.bt_cancel:
                connectDevice.cancel();
                break;
            case R.id.bt_next:
                int data = Integer.parseInt(p[i]);
                i++;
                Message message = mHandler.obtainMessage();
                message.what = BT_DATA_TEST;
                message.arg1 = data;
                mHandler.sendMessage(message);

                break;*/

        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {

            blink_times = 0;

            String[] va = ed_send_data.getText().toString().split("@");
            if (va.length > 1) {
                size = Integer.parseInt(va[0]);
                high = Integer.parseInt(va[1]);
                low = Integer.parseInt(va[2]);
                dec = Integer.parseInt(va[3]);
            }
            searchBTDevice();
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            if (connectDevice != null)
                connectDevice.cancel();

        } else if (id == R.id.nav_slideshow) {
            i = 1;
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            String con = StreamUtil.Stream2String(inputStream);
            p = con.split(",");


            waveView.clear();

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
