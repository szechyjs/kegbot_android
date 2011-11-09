package com.goliathonline.android.kegbot.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class C2DMReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            // we've received a registration response
            handleRegistration(context, intent);
        } else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
            // we've recieved a push notification w/ payload
            handleMessage(context, intent);
        }
    }

    private void handleRegistration(Context context, Intent intent) {
        String registration = intent.getStringExtra("registration_id");
        if (intent.getStringExtra("error") != null) {
            Toast.makeText(context, "Registration ERROR, Toast.LENGTH_SHORT).show();
        } else if (intent.getStringExtra("unregistered") != null) {
            Toast.makeText(context, "Registration not active", Toast.LENGTH_SHORT).show();
        } else if (registration != null) {
            Toast.makeText(context, "Registration Recieved: " + registration, Toast.LENGTH_SHORT).show();

            ///TODO: WebApp service registration -> django
        }
    }

    public void handleMessage(Context context, Intent intent)
    {
        String msg = "Message ID pushed: " + intent.getStringExtra("message");

        ///TODO: Webapp download message
    }
}
