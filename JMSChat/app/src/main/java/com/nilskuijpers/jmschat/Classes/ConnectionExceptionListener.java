package com.nilskuijpers.jmschat.Classes;

import android.util.Log;

import com.kaazing.gateway.jms.client.ConnectionDisconnectedException;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Created by surfa on 18-4-2016.
 */
public class ConnectionExceptionListener implements ExceptionListener {

    public void onException(final JMSException exception) {
        Log.e("LISTENER ERROR", exception.getMessage());
        if (exception instanceof ConnectionDisconnectedException) {
            //updateButtonsForDisconnected();
        }
    }
}
