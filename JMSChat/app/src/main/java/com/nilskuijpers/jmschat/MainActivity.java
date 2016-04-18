package com.nilskuijpers.jmschat;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.kaazing.gateway.jms.client.JmsConnectionFactory;
import com.nilskuijpers.jmschat.Classes.ConnectionExceptionListener;
import com.nilskuijpers.jmschat.Classes.DispatchQueue;
import com.nilskuijpers.jmschat.Classes.MessagesAdapter;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class MainActivity extends AppCompatActivity implements MessageListener{

    private final static String GLOBAL_BROADCASTSUBSCRIPTION = "/topic/GLOBAL_BROADCAST";

    private DispatchQueue dispatchQueue;
    private JmsConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private HashMap<String, ArrayDeque<MessageConsumer>> consumers = new HashMap<String, ArrayDeque<MessageConsumer>>();
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private int mNotificationId = 001;

    private List<String> receivedMessages;
    public  RecyclerView.Adapter messagesAdapter;

    private Button sendMessageButton;
    private EditText messageText;

    private RecyclerView messagesRecycler;

    private RecyclerView.LayoutManager messagesLayoutManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        receivedMessages = new ArrayList<>();

        receivedMessages.add("hoi");
        receivedMessages.add("nils");

        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        messagesRecycler = (RecyclerView) findViewById(R.id.messagesRecycler);

        messagesRecycler.setHasFixedSize(true);

        messagesLayoutManager = new LinearLayoutManager(this);

        messagesRecycler.setLayoutManager(messagesLayoutManager);

        messagesAdapter = new MessagesAdapter(this.receivedMessages);

        messagesRecycler.setAdapter(messagesAdapter);

        /*sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        messageText = (EditText) findViewById(R.id.messageText);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(messageText.getText().toString());
            }
        });*/

        if (connectionFactory == null) {
            try {
                connectionFactory = JmsConnectionFactory.createConnectionFactory();
            } catch (JMSException e) {
                e.printStackTrace();
                Log.e("ERROR", e.getMessage());
            }
        }
        connect();
    }

    private void connect() {
        //closedExplicitly = false;
        Log.i("JMS","CONNECTING");
        // initialize dispatch queue that will be used to run
        // blocking calls asynchronously on a separate thread
        dispatchQueue = new DispatchQueue("Async Dispatch Queue");
        dispatchQueue.start();
        dispatchQueue.waitUntilReady();

        // Since WebSocket.connect() is a blocking method which will not return until
        // the connection is established or connection fails, it is a good practice to
        // establish connection on a separate thread so that UI is not blocked.
        dispatchQueue.dispatchAsync(new Runnable() {
            public void run() {
                try {
                    connectionFactory.setGatewayLocation(URI.create("ws://192.168.0.101:8001/jms"));
                    connection = connectionFactory.createConnection();
                    connection.start();
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Log.i("JMS", "CONNECTED");
                    connection.setExceptionListener(new ConnectionExceptionListener());
                    subscribeTo(MainActivity.GLOBAL_BROADCASTSUBSCRIPTION);
                } catch (Exception e) {
                    Log.e("JMS Connect Exception", e.getMessage());
                    dispatchQueue.quit();
                }
            }
        });
    }

    private void disconnect() {
        Log.i("JMS","DISCONNECTING");

        dispatchQueue.removePendingJobs();
        dispatchQueue.quit();
        new Thread(new Runnable() {
            public void run() {
                try {
                    connection.close();
                    Log.i("JMS","DISCONNECTED");
                } catch (JMSException e) {
                    e.printStackTrace();
                    Log.e("ERROR", e.getMessage());
                }
                finally {
                    connection = null;
                }
            }
        }).start();
    }

    private void subscribeTo(final String subscription)
    {
        Log.i("JMS", "Subscribing to " + subscription);

        dispatchQueue.dispatchAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Destination destination = getDestination(subscription);

                    if (destination == null)
                    {
                        return;
                    }

                    MessageConsumer consumer = session.createConsumer(destination);
                    ArrayDeque<MessageConsumer> consumersToDestionation = consumers.get(subscription);

                    if(consumersToDestionation == null)
                    {
                        consumersToDestionation = new ArrayDeque<MessageConsumer>();
                        consumers.put(subscription, consumersToDestionation);
                    }

                    consumersToDestionation.add(consumer);
                    consumer.setMessageListener(MainActivity.this);

                    if(subscription == MainActivity.GLOBAL_BROADCASTSUBSCRIPTION)
                    {
                        //sendMessage("Hello broadcast!");
                    }

                } catch (JMSException e) {
                    Log.e("JMS", e.getMessage());
                }
            }
        });
    }

    private Destination getDestination(String destinationName) throws JMSException {
        Destination destination;
        if (destinationName.startsWith("/topic/")) {
            destination = session.createTopic(destinationName);
        }
        else if (destinationName.startsWith("/queue/")) {
            destination = session.createQueue(destinationName);
        }
        else {
            Log.e("JMS", "Invalid destionation: " + destinationName);
            return null;
        }
        return destination;

    }

    private void sendMessage(final String messageContent)
    {
        dispatchQueue.dispatchAsync(new Runnable() {
            public void run() {
                try {
                    MessageProducer producer = session.createProducer(getDestination(MainActivity.GLOBAL_BROADCASTSUBSCRIPTION));
                    Message message;

                    message = session.createTextMessage(messageContent);

                    producer.send(message);

                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                    Log.e("ERROR", e.getMessage());
                }
            }
        });
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                Log.i("JMS / RECEIVED: ", ((TextMessage)message).getText());


                mBuilder = new NotificationCompat.Builder(MainActivity.this).setSmallIcon(R.drawable.ic_stat_name).setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_stat_name)).setContentTitle("Broadcast").setContentText(((TextMessage) message).getText().toString());

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mBuilder.setSound(alarmSound);

                mNotifyManager.notify(mNotificationId,mBuilder.build());

                mNotificationId = mNotificationId + 1;

                this.receivedMessages.add(((TextMessage) message).getText().toString());

                runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    messagesAdapter.notifyDataSetChanged();

                                }
                            });
                //
            }
            else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage)message;

                long len = bytesMessage.getBodyLength();
                byte b[] = new byte[(int)len];
                bytesMessage.readBytes(b);

                Log.i("JMS / RECEIVED: ", hexDump(b));
            }
            else if (message instanceof MapMessage) {
                MapMessage mapMessage = (MapMessage)message;
                Enumeration mapNames = mapMessage.getMapNames();
                while (mapNames.hasMoreElements()) {
                    String key = (String)mapNames.nextElement();
                    Object value = mapMessage.getObject(key);

                    if (value == null) {
                        Log.i("JMS / RECEIVED: ", key + ": null");
                    } else if (value instanceof byte[]) {
                        byte[] arr = (byte[])value;
                        StringBuilder s = new StringBuilder();
                        s.append("[");
                        for (int i = 0; i < arr.length; i++) {
                            if (i > 0) {
                                s.append(",");
                            }
                            s.append(arr[i]);
                        }
                        s.append("]");
                        Log.i("JMS / RECEIVED: ", key + ": "+ s.toString() + " (Byte[])");
                    } else {
                        Log.i("JMS / RECEIVED: ", key + ": " + value.toString() + " (" + value.getClass().getSimpleName() + ")");
                    }
                }
                Log.i("JMS / RECEIVED: ", "RECEIVED MapMessage: ");
            }
            else {
                Log.i("JMS / RECEIVED: ", "UNKNOWN MESSAGE TYPE: "+message.getClass().getSimpleName());
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.e("ERROR", ex.getMessage());
        }
    }

    private String hexDump(byte[] b) {
        if (b.length == 0) {
            return "empty";
        }

        StringBuilder out = new StringBuilder();
        for (int i=0; i < b.length; i++) {
            out.append(Integer.toHexString(b[i])).append(' ');
        }
        return out.toString();
    }
}
