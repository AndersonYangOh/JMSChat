package com.nilskuijpers.jmschat;

import android.app.NotificationManager;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.kaazing.gateway.jms.client.JmsConnectionFactory;
import com.nilskuijpers.jmschat.Classes.ChatMessage;
import com.nilskuijpers.jmschat.Classes.ChatsAdapter;
import com.nilskuijpers.jmschat.Classes.ConnectionExceptionListener;
import com.nilskuijpers.jmschat.Classes.DispatchQueue;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class ListActivity extends AppCompatActivity implements MessageListener {

    private RecyclerView chatRecycler;
    private RecyclerView.LayoutManager chatsLayoutManager;
    private ChatsAdapter chatsAdapter;
    private LinkedHashMap<String, ArrayList<ChatMessage>> messageDb = new LinkedHashMap<>();

    private final static String DEBUG_LOCAL_USER = "nilsk123";
    private final static String DEBUG_PERSONAL_QUEUE = "/queue/" + DEBUG_LOCAL_USER;
    private final static String DEBUG_RECIPIENT_QUEUE = "/queue/jackster";
    private final static String GLOBAL_BROADCASTSUBSCRIPTION = "/topic/GLOBAL_BROADCAST_V2";

    //JMS Communications variables;
    private DispatchQueue dispatchQueue;
    private JmsConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private HashMap<String, ArrayDeque<MessageConsumer>> consumers = new HashMap<String, ArrayDeque<MessageConsumer>>();

    //Variables for notifications
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private int mNotificationId = 001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        chatRecycler = (RecyclerView) findViewById(R.id.chatRecycler);
        chatRecycler.setHasFixedSize(true);
        chatsLayoutManager = new LinearLayoutManager(this);
        chatRecycler.setLayoutManager(chatsLayoutManager);
        chatsAdapter = new ChatsAdapter(this.messageDb);
        chatRecycler.setAdapter(chatsAdapter);

        //Getting a reference to system services
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Setup JMS connection and connect to remote server
        if (connectionFactory == null)
        {
            try
            {
                connectionFactory = JmsConnectionFactory.createConnectionFactory();
            }
            catch (JMSException e)
            {
                e.printStackTrace();
                Log.e("ERROR", e.getMessage());
            }
        }
        connect();
    }

    //This function connects to the remote JMS server
    private void connect() {
        Log.i("JMS","CONNECTING");

        // initialize dispatch queue that will be used to run
        // blocking calls asynchronously on a separate thread
        dispatchQueue = new DispatchQueue("Async Dispatch Queue");
        dispatchQueue.start();
        dispatchQueue.waitUntilReady();

        dispatchQueue.dispatchAsync(new Runnable() {
            public void run() {
                try {
                    //setup connection
                    connectionFactory.setGatewayLocation(URI.create("ws://192.168.0.101:8001/jms"));
                    connection = connectionFactory.createConnection();
                    connection.start();

                    //create session
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    Log.i("JMS", "CONNECTED");
                    connection.setExceptionListener(new ConnectionExceptionListener());

                    //at this point connection succeeded so subscribe to global broadcast topic
                    subscribeTo(ListActivity.GLOBAL_BROADCASTSUBSCRIPTION);
                    subscribeTo(ListActivity.DEBUG_PERSONAL_QUEUE);
                } catch (Exception e) {
                    Log.e("JMS Connect Exception", e.getMessage());
                    dispatchQueue.quit();
                }
            }
        });
    }

    //This function disconnects from remote JMS server
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

    //This functions subscribes the consumer to a topic or queue
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
                    consumer.setMessageListener(ListActivity.this);

                    if(subscription == ListActivity.GLOBAL_BROADCASTSUBSCRIPTION)
                    {
                        sendMessage("Hello broadcast!", ListActivity.GLOBAL_BROADCASTSUBSCRIPTION);
                    }

                } catch (JMSException e) {
                    Log.e("JMS", e.getMessage());
                }
            }
        });
    }
    private void sendMessage(final String messageContent, final String groupName)
    {
        dispatchQueue.dispatchAsync(new Runnable() {
            public void run() {
                try {
                    MessageProducer producer = session.createProducer(getDestination(groupName));
                    Message message;

                    message = session.createTextMessage(messageContent);
                    message.setStringProperty("Sender", "nilsk123");
                    message.setBooleanProperty("Group", true);
                    message.setStringProperty("GroupName", groupName);

                    producer.send(message);

                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                    Log.e("ERROR", e.getMessage());
                }
            }
        });
    }


    //This function determines the subscription type
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

    //This function gets called whenever the messagelistener receives a new message
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                Log.i("JMS / RECEIVED Text: ", ((TextMessage)message).getText() + " FROM " + ((TextMessage)message).getStringProperty("Sender"));

                mBuilder = new NotificationCompat.Builder(ListActivity.this).setSmallIcon(R.drawable.ic_stat_name).setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_stat_name)).setContentTitle(((TextMessage)message).getStringProperty("Sender")).setContentText(((TextMessage) message).getText().toString());

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mBuilder.setSound(alarmSound);

                mNotifyManager.notify(mNotificationId,mBuilder.build());

                mNotificationId = mNotificationId + 1;

                if(!message.getBooleanProperty("Group"))
                {
                    if(!messageDb.containsKey(message.getStringProperty("Sender")))
                    {
                        Log.i("INFO","Create new local chat for " + message.getStringProperty("Sender"));
                        messageDb.put(message.getStringProperty("Sender"), new ArrayList<ChatMessage>());
                    }

                    else
                    {
                        Log.i("INFO","Local chat found for sender " +  message.getStringProperty("Sender"));
                    }

                    messageDb.get(message.getStringProperty("Sender")).add(new ChatMessage(message.getStringProperty("Sender"),new Date(), ((TextMessage) message).getText().toString()));
                }

                else
                {
                    Log.i("INFO","Broadcast received");
                    if(!messageDb.containsKey(message.getStringProperty("Broadcast")))
                    {
                        Log.i("INFO","Create new local chat for broadcast");
                        messageDb.put("Broadcast", new ArrayList<ChatMessage>());
                    }

                    else
                    {
                        Log.i("INFO","Local chat found for broadcast");
                    }

                    messageDb.get("Broadcast").add(new ChatMessage(message.getStringProperty("Sender"),new Date(), ((TextMessage) message).getText().toString()));
                }





                //receivedMessages = messageDb.get(message.getStringProperty("Sender"));



                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("INFO", messageDb.size() + " chats");
                        //messagesAdapter = new MessagesAdapter(receivedMessages);
                        chatRecycler.swapAdapter(new ChatsAdapter(messageDb),false);
                    }
                });
            }
            /*else if (message instanceof BytesMessage) {
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
            }*/

        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.e("ERROR", ex.getMessage());
        }
    }

    //Creates a hexdump from a byte array
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
