package com.radiusnetworks.ibeaconreference;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

//import com.jessefarebro.mqtt.MqttService;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;
//import com.radiusnetworks.ibeacon.TimedBeaconSimulator;

import android.os.Bundle;
import android.os.RemoteException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * 
 * @author dyoung
 * @author Matt Tyler
 */
public class MonitoringActivity extends Activity implements IBeaconConsumer  {
	protected static final String TAG = "MonitoringActivity";
	
	String serverUri = "tcp://messaging.quickstart.internetofthings.ibmcloud.com";
	String clientId = "quickstart:fbb80f053444";
	MemoryPersistence persistence = new MemoryPersistence();
	String topic = "iot-1/d/fbb80f053444/evt/iotsensor/json";
	MqttAndroidClient androidClient;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitoring);
		verifyBluetooth();
	    iBeaconManager.bind(this);
	    try {
	    	MqttConnectOptions conOpt = new MqttConnectOptions();
	    	conOpt.setKeepAliveInterval(240000);
	    	Context context = this;
	    	
	    	androidClient = new MqttAndroidClient(context, serverUri, clientId, persistence);	
//	    	androidClient.setTraceEnabled(true);
//			conOpt.setWill(client.getTopic(topic),"Crash".getBytes(),1,true);
	    	androidClient.connect(conOpt);
	    	Log.v("onCreate", "We might have logged on with the Mqtt client!");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		//initializing simulated iBeacons
		//IBeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
		//((TimedBeaconSimulator) IBeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
	}
	
	public void onRangingClicked(View view) {
		Intent myIntent = new Intent(this, RangingActivity.class);
		this.startActivity(myIntent);
	}
	public void onBackgroundClicked(View view) {
		Intent myIntent = new Intent(this, BackgroundActivity.class);
		this.startActivity(myIntent);
	}

	private void verifyBluetooth() {

		try {
			if (!IBeaconManager.getInstanceForApplication(this).checkAvailability()) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Bluetooth not enabled");			
				builder.setMessage("Please enable bluetooth in settings and restart this application.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finish();
			            System.exit(0);					
					}					
				});
				builder.show();
			}			
		}
		catch (RuntimeException e) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Bluetooth LE not available");			
			builder.setMessage("Sorry, this device does not support Bluetooth LE.");
			builder.setPositiveButton(android.R.string.ok, null);
			builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					finish();
		            System.exit(0);					
				}
				
			});
			builder.show();
			
		}
		
	}	

    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    
    public void sendSignal(Region region){
    	try 
    	{
    		//first things first
    		if (region.getMajor()==null || region.getMinor()==null || region.getProximityUuid()==null)
    		{}
    		else{
    		
				//get stuff
				String myMajor = region.getMajor().toString();
				String myMinor = region.getMinor().toString();
				String myUuid = region.getProximityUuid();
				String rawMessage = "Discovered iBeacon: " + myUuid + "#" + myMajor + "#" + myMinor;
				//connect
				//parse message
				Log.v("sendSignal","Created raw message: " + rawMessage);
				MqttMessage message = new MqttMessage();	
				message.setQos(0);
				message.setRetained(false);
				message.setPayload(rawMessage.getBytes());
				Log.v("sendSignal","Attempting to publish message to broker");
				if(!androidClient.isConnected()) {
    				MqttConnectOptions conOpt = new MqttConnectOptions();
    		    	conOpt.setKeepAliveInterval(240000);
    		    	androidClient.connect(conOpt);
    			}
				androidClient.publish(topic, message);
				Log.v("sendSignal","Message (hopefully) published to broker");
    		} 
		}
    	catch (MqttSecurityException e) {
		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} 
    	catch (MqttException e) {
		// TODO Auto-generated catch block
    		e.printStackTrace();
		}
	}

    @Override 
    protected void onDestroy() {
        super.onDestroy();
        iBeaconManager.unBind(this);
        try {
        	if(androidClient != null && androidClient.isConnected()) {
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
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, true);    	
    	try {
    		if(androidClient != null && androidClient.isConnected())
    			androidClient.disconnect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    @Override 
    protected void onResume() {
    	super.onResume();
    	if (iBeaconManager.isBound(this)) iBeaconManager.setBackgroundMode(this, false);
    	try {
    		MqttConnectOptions conOpt = new MqttConnectOptions();
	    	conOpt.setKeepAliveInterval(240000);
	    	if(androidClient != null)
	    		androidClient.connect();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    	
    }    
    
    private void logToDisplay(final String line) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
    	    	EditText editText = (EditText)MonitoringActivity.this
    					.findViewById(R.id.monitoringText);
       	    	editText.append(line+"\n");            	    	    		
    	    }
    	});
    }
    @Override
    public void onIBeaconServiceConnect() {
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
          logToDisplay("I just saw an iBeacon named "+ region.getUniqueId() +" for the first time!" );
          //my added stuff
          logToDisplay("Sending MQTT message...");
          sendSignal(region);
          logToDisplay("MQTT message sent!");
        }

        @Override
        public void didExitRegion(Region region) {
        	logToDisplay("I no longer see an iBeacon named "+ region.getUniqueId());
        }

        @Override
        public void didDetermineStateForRegion(int state, Region region) {
        	logToDisplay("I have just switched from seeing/not seeing iBeacons: "+state);     
        }


        });

        try {
        	iBeaconManager.startMonitoringBeaconsInRegion(new Region("Utgang", "e2c56db5-dffb-48d2-b060-d0f5a71096e0", 3, 1));
        	//Sample Simulated iBeacons
        	//iBeaconManager.startMonitoringBeaconsInRegion(new Region("test1","DF7E1C79-43E9-44FF-886F-1D1F7DA6997A".toLowerCase(), 1, 1));
        	//iBeaconManager.startMonitoringBeaconsInRegion(new Region("test2","DF7E1C79-43E9-44FF-886F-1D1F7DA6997B".toLowerCase(), 1, 2));
        	//iBeaconManager.startMonitoringBeaconsInRegion(new Region("test3","DF7E1C79-43E9-44FF-886F-1D1F7DA6997C".toLowerCase(), 1, 3));
        	//iBeaconManager.startMonitoringBeaconsInRegion(new Region("test4","DF7E1C79-43E9-44FF-886F-1D1F7DA6997D".toLowerCase(), 1, 4));
        } catch (RemoteException e) {   }
    }
	
}
