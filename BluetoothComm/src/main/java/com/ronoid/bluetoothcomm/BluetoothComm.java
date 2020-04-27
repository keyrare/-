package com.ronoid.bluetoothcomm;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class BluetoothComm extends Activity {
    // Debugging
    private static final String TAG = "BluetoothComm";
    private static final boolean D = true;
    //������������requestCode
    static final int REQUEST_ENABLE_BT = 1;
    //�������ӵ�requestCode
    static final int REQUEST_CONNECT_DEVICE = 2;
    //bluetoothCommService ��������Ϣ״̬
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_DEVICE_NAME = 2;
    public static final int MESSAGE_TOAST = 3;
    //С��״̬
    public static final byte CAR_STOP = 0x01;
    public static final byte CAR_UP = 0x02;
    public static final byte CAR_DOWN = 0x03;
    public static final byte CAR_RIGHT = 0x04;
    public static final byte CAR_LEFT = 0x05;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    //�����豸
    private BluetoothDevice device = null;
    //���ӵ��豸
    private TextView connectDevices;
    private Button FWard, FRight, FLeft, Fback, FStop;
    //�Ͽ����Ӱ���
    private Button disconnectButton;
    private Button menu;
    //��������������
    private BluetoothAdapter bluetooth;
    //����һ���������ڷ������
    private BluetoothCommService mCommService = null;
    private String mConnectedDeviceName = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //��ÿؼ�
        //ǰ��
        FWard = (Button) findViewById(R.id.forWard);
        FWard.setOnClickListener(new btnClickedListener());
        //����
        Fback = (Button) findViewById(R.id.backWards);
        Fback.setOnClickListener(new btnClickedListener());
        //��ת
        FLeft = (Button) findViewById(R.id.towardsLeft);
        FLeft.setOnClickListener(new btnClickedListener());
        FLeft.setOnLongClickListener(new btnLongClickedListener());
        FLeft.setOnTouchListener(new btnTouchListener());
        //��ת
        FRight = (Button) findViewById(R.id.towardsRight);
        FRight.setOnClickListener(new btnClickedListener());
        FRight.setOnLongClickListener(new btnLongClickedListener());
        FRight.setOnTouchListener(new btnTouchListener());
        //ֹͣ
        FStop = (Button) findViewById(R.id.towardsStop);
        FStop.setOnClickListener(new btnClickedListener());
        //�����ؼ�
        connectDevices = (TextView) findViewById(R.id.connected_device);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new btnClickedListener());
        menu = (Button) findViewById(R.id.but_menu);
        menu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        //��ñ��������豸
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null) {//�豸û�������豸
            Toast.makeText(this, "û���ҵ�����������", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "- ON START -");
        if (!bluetooth.isEnabled()) {
            //����������豸
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mCommService == null) {
                mCommService = new BluetoothCommService(this, mHandler);
            }
        }
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "- ON RESUME -");
        if (mCommService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mCommService.getState() == BluetoothCommService.STATE_NONE) {
                // Start the Bluetooth services�����������߳�
                mCommService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        if (mCommService == null) {
            mCommService = new BluetoothCommService(this, mHandler);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (bluetooth != null) {
            bluetooth.disable();
        }
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mCommService != null) mCommService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * onActivityResult������������startActivityForResult����֮����ã�
     * �����û��Ĳ�����ִ����Ӧ�Ĳ���
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    if (D) Log.d(TAG, "�������豸");
                    Toast.makeText(this, "�ɹ�������", Toast.LENGTH_SHORT).show();
                } else {
                    if (D) Log.d(TAG, "������������豸");
                    Toast.makeText(this, "���ܴ�����,���򼴽��ر�", Toast.LENGTH_SHORT).show();
                    finish();//�û������豸���������
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {//�û�ѡ�����ӵ��豸
                    bluetooth = BluetoothAdapter.getDefaultAdapter();
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(ScanDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    device = bluetooth.getRemoteDevice(address);
                    //���������豸
                    if (device != null) {
                        mCommService.connect(device);
                    }
                }
                break;
        }
        return;
    }


    private class btnClickedListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            //ǰ�������ˣ���һ�´���
            if (device == null) {
                Toast.makeText(BluetoothComm.this, "�������豸��", Toast.LENGTH_LONG).show();
            } else {
                if (v.getId() == R.id.disconnectButton) {
                    if (mCommService != null) {
                        mCommService.stop();
                    }
                } else if (v.getId() == R.id.forWard) {
                    sendMessage(CAR_UP);
                } else if (v.getId() == R.id.backWards) {
                    sendMessage(CAR_STOP);
                    sendMessage(CAR_DOWN);
                    sendMessage(CAR_DOWN);
                } else {
                    sendMessage(CAR_STOP);
                }
            }
        }
    }


    private class btnLongClickedListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (device != null) {
                if (v.getId() == R.id.towardsLeft) {
                    sendMessage(CAR_LEFT);
                } else if (v.getId() == R.id.towardsRight) {
                    sendMessage(CAR_RIGHT);
                }
            }
            return false;
        }
    }

    private class btnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (device != null) {
                if (v.getId() == R.id.towardsLeft) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(CAR_LEFT);
                    }
                } else if (v.getId() == R.id.towardsRight) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        sendMessage(CAR_RIGHT);
                    }
                }
            }
            return false;
        }
    }

    private void ensureDiscoverable() {
        if (bluetooth.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //��ɼ�ʱ��Ϊ300s
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    //�����˵�ѡ��
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;

    }

    //�˵�����
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the ScanDeviceActivity to see devices and do scan
                Intent serverIntent = new Intent(this, ScanDeviceActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
            case R.id.about:
                Intent intent = new Intent(BluetoothComm.this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.exit:
                finish();
                return true;
        }
        return false;
    }


    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(byte message) {
        //û�������豸�����ܷ���
        if (mCommService.getState() != BluetoothCommService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.nodevice, Toast.LENGTH_SHORT).show();
        } else {
            byte send = message;
            mCommService.write(send);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothCommService.STATE_CONNECTED:
                            connectDevices.setText(R.string.title_connected_to);
                            connectDevices.append(mConnectedDeviceName);
                            //    mConversationArrayAdapter.clear();
                            break;
                        case BluetoothCommService.STATE_CONNECTING:
                            connectDevices.setText(R.string.title_connecting);
                            break;
                        case BluetoothCommService.STATE_LISTEN:
                        case BluetoothCommService.STATE_NONE:
                            connectDevices.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "�ɹ����ӣ�"
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}