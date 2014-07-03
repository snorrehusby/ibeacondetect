package com.radiusnetworks.ibeaconreference;

import java.util.Collection;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.Region;
import com.radiusnetworks.ibeacon.RangeNotifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.EditText;

public class RangingActivity extends Activity implements IBeaconConsumer {
	protected static final String TAG = "RangingActivity";
	String serverUri = "tcp://messaging.quickstart.internetofthings.ibmcloud.com";
	String clientId = "quickstart:fbb80f053444";
	MemoryPersistence persistence = new MemoryPersistence();
	String topic = "iot-1/d/fbb80f053444/evt/iotsensor/json";
	MqttAndroidClient androidClient;

	private IBeaconManager iBeaconManager = IBeaconManager
			.getInstanceForApplication(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ranging);
		iBeaconManager.bind(this);
		connectToBroker();

	}

	void connectToBroker() {
		try {
			MqttConnectOptions conOpt = new MqttConnectOptions();
			conOpt.setKeepAliveInterval(240000);
			Context context = this;
			androidClient = new MqttAndroidClient(context, serverUri, clientId,
					persistence);
			androidClient.connect(conOpt);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		iBeaconManager.unBind(this);
		try {
			if (androidClient != null && androidClient.isConnected()) {
				androidClient.disconnect();
				androidClient.close();
			}
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(this, true);
		try {
			if (androidClient != null && androidClient.isConnected())
				androidClient.disconnect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(this, false);
		try {
			MqttConnectOptions conOpt = new MqttConnectOptions();
			conOpt.setKeepAliveInterval(240000);
			if (androidClient != null)
				androidClient.connect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void onIBeaconServiceConnect() {
		iBeaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons,
					Region region) {
				if (iBeacons.size() > 0) {
					EditText editText = (EditText) RangingActivity.this
							.findViewById(R.id.rangingText);
					logToDisplay("The first iBeacon I see is about "
							+ iBeacons.iterator().next().getAccuracy()
							+ " meters away.");
					if (!androidClient.isConnected()) {
						Log.e("MQTT", "Not connected to broker anymore");
						MqttConnectOptions conOpt = new MqttConnectOptions();
						conOpt.setKeepAliveInterval(240000);
						try {
							androidClient.connect(conOpt);
						} catch (MqttException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					String rawMessage = "The first iBeacon I see is about "
							+ iBeacons.iterator().next().getAccuracy()
							+ " meters away.";
					// connect
					// parse message
					Log.v("sendSignal", "Created raw message: " + rawMessage);
					MqttMessage message = new MqttMessage();
					message.setQos(0);
					message.setRetained(false);
					message.setPayload(rawMessage.getBytes());
					try {
						androidClient.publish(topic, message);						
					} catch (MqttPersistenceException e) {
						Log.e("MQTT", "MqttPersistenceException");
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (MqttException e) {
						Log.e("MQTT", "MqttException");
						e.printStackTrace();
					}
				}
			}

		});

		try {
			iBeaconManager.startRangingBeaconsInRegion(new Region(
					"myRangingUniqueId", null, null, null));
		} catch (RemoteException e) {
		}
	}

	private void logToDisplay(final String line) {
		runOnUiThread(new Runnable() {
			public void run() {
				EditText editText = (EditText) RangingActivity.this
						.findViewById(R.id.rangingText);
				editText.append(line + "\n");
			}
		});
	}
}
