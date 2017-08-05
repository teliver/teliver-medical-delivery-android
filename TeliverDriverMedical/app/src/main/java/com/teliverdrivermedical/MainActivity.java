package com.teliverdrivermedical;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.teliver.sdk.core.TLog;
import com.teliver.sdk.core.Teliver;
import com.teliver.sdk.core.TripListener;
import com.teliver.sdk.models.PushData;
import com.teliver.sdk.models.Trip;
import com.teliver.sdk.models.TripBuilder;
import com.teliver.sdk.models.UserBuilder;

public class MainActivity extends AppCompatActivity {

    private String username = "medical_driver",customerName = "medical_customer";

    private String trackingId = "TELIVERTRK_6600";

    private com.teliverdrivermedical.Application application;

    private Button btnDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        TLog.setVisible(true);
        Teliver.identifyUser(new UserBuilder(username).setUserType(UserBuilder.USER_TYPE.OPERATOR).registerPush().build());
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Drawable drawable = toolBar.getNavigationIcon();
        drawable.setColorFilter(ContextCompat.getColor(this,R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);

        application = (com.teliverdrivermedical.Application) getApplicationContext();
        btnDelivery = (Button) findViewById(R.id.btnDelivery);
        btnDelivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!application.getBooleanInPef("IN_CURRENT_TRIP")) {
                    PushData pushData = new PushData(customerName);
                    pushData.setMessage("your order is out for delivery track your order");
                    pushData.setPayload("1");
                    TripBuilder tripBuilder = new TripBuilder(trackingId).withUserPushObject(pushData);
                    Teliver.startTrip(tripBuilder.build());
                    Teliver.setTripListener(new TripListener() {
                        @Override
                        public void onTripStarted(Trip tripDetails) {
                            application.storeBoolenInPref("IN_CURRENT_TRIP", true);
                            btnDelivery.setText(getString(R.string.txtInDelivery));
                            Log.d("TELIVER::", "onTripStarted: " + tripDetails);
                        }

                        @Override
                        public void onLocationUpdate(Location location) {
                            Log.d("TELIVER::", "onLocationUpdate: LOCAION VALUES OF DRIVER == " + location.getLatitude()
                                    + location.getLongitude());
                        }

                        @Override
                        public void onTripEnded(String trackingID) {
                            Log.d("TELIVER::", "onTripEnded: " + trackingID);
                            btnDelivery.setText(getString(R.string.txtOutDelivery));
                            application.storeBoolenInPref("IN_CURRENT_TRIP", false);
                            application.deletePreference();

                        }

                        @Override
                        public void onTripError(String reason) {
                            Log.d("TELIVER::", "onTripError the trip error: " + reason);
                        }
                    });
                } else {
                    PushData pushData = new PushData(customerName);
                    pushData.setMessage("your order is successfully delivered");
                    pushData.setPayload("0");
                    Teliver.sendEventPush(trackingId,pushData,"Order Delivered");
                    Log.d("TELIVER::", "onClick: " + "THIS IS CALLING STOP TRIP");
                    Teliver.stopTrip(trackingId);
                }
            }
        });
    }


    @Override
    protected void onResume() {
        if (application.getBooleanInPef("IN_CURRENT_TRIP"))
            btnDelivery.setText(getString(R.string.txtInDelivery));
        else btnDelivery.setText(getString(R.string.txtOutDelivery));
        super.onResume();
    }
}
