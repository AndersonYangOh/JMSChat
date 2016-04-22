package com.nilskuijpers.jmschatter;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.kaazing.gateway.jms.client.JmsConnectionFactory;
import com.nilskuijpers.jmschatter.Fragments.ChatFragment;
import com.nilskuijpers.jmschatter.Fragments.ChatListFragment;
import com.nilskuijpers.jmschatter.LibraryClasses.ConnectionExceptionListener;
import com.nilskuijpers.jmschatter.LibraryClasses.DispatchQueue;
import com.nilskuijpers.jmschatter.ObjectClasses.ChatConversation;
import com.nilskuijpers.jmschatter.ObjectClasses.ChatMessage;
import com.nilskuijpers.jmschatter.ObjectClasses.GroupConversation;
import com.nilskuijpers.jmschatter.ObjectClasses.SingleRecipientConversation;

import java.net.URI;
import java.security.acl.Group;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class MainActivity extends AppCompatActivity implements MessageListener, ChatListFragment.OnFragmentInteractionListener, ChatFragment.OnFragmentInteractionListener {

    //Static properties
    public static String LOCAL_USER;
    public static String PERSONAL_QUEUE;
    public final static String GLOBAL_BROADCASTSUBSCRIPTION = "/topic/GLOBAL_BROADCAST_NEW_V2";

    private final static String CHATLIST_TAG = "ChatListFragment";
    private final static String CHAT_TAG = "ChatFragment";

    //Date storage
    private List<ChatConversation> conversations = new ArrayList<>();
    private ChatConversation activeConversation;

    //Fragment properties
    private Fragment chatListFragment;
    private Fragment chatFragment;
    private FragmentTransaction ft;

    //JMS properties
    private JmsConnectionFactory connectionFactory;
    private DispatchQueue dispatchQueue;
    private Connection connection;
    private Session session;
    private HashMap<String, ArrayDeque<MessageConsumer>> consumers = new HashMap<String, ArrayDeque<MessageConsumer>>();

    //Notification shizzle
    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private int mNotificationId = 001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();

        this.LOCAL_USER = intent.getStringExtra("local_user");
        this.PERSONAL_QUEUE = "/queue/" + this.LOCAL_USER;

        getSupportActionBar().setTitle(LOCAL_USER);

        //initialise conversation list
        this.conversations = new ArrayList<>();

        //Notification stuff
        this.mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Set home chat overview
        setHomeFragment();

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

    private void setHomeFragment()
    {
        //Assign fragment, appoint variables and start transaction/animation
        chatListFragment = ChatListFragment.newInstance("dummy1","dummy2");
        ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        ft.replace(R.id.fragmentContainer,chatListFragment,MainActivity.CHATLIST_TAG);
        ft.commit();
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
                    subscribeTo(MainActivity.GLOBAL_BROADCASTSUBSCRIPTION);
                    subscribeTo(MainActivity.PERSONAL_QUEUE);
                } catch (Exception e) {
                    Log.e("JMS Connect Exception", e.getMessage());
                    dispatchQueue.quit();
                }
            }
        });
    }

    //This functions subscribes the consumer to a topic or queue
    public void subscribeTo(final String subscription)
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
                    ArrayDeque<MessageConsumer> consumersToDestination = consumers.get(subscription);

                    if(consumersToDestination == null)
                    {
                        consumersToDestination = new ArrayDeque<MessageConsumer>();
                        consumers.put(subscription, consumersToDestination);
                    }

                    consumersToDestination.add(consumer);
                    consumer.setMessageListener(MainActivity.this);

                } catch (JMSException e) {
                    Log.e("JMS", e.getMessage());
                }
            }
        });
    }

    public void sendMessageTo(final String messageContent, final Destination destination)
    {

        dispatchQueue.dispatchAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    MessageProducer producer = session.createProducer(destination);
                    Message message = session.createTextMessage(messageContent);



                    message.setStringProperty("author", MainActivity.LOCAL_USER);

                    if(destination.toString().contains("/topic/"))
                    {
                        GroupConversation grp = (GroupConversation) activeConversation;
                        message.setBooleanProperty("group", true);
                        message.setStringProperty("groupName", grp.getGroupName());
                    }

                    else
                    {
                        message.setBooleanProperty("group", false);
                    }


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
    public Destination getDestination(String destinationName) throws JMSException {
        Destination destination;
        if (destinationName.startsWith("/topic/")) {
            destination = session.createTopic(destinationName);
        }
        else if (destinationName.startsWith("/queue/")) {
            destination = session.createQueue(destinationName);
        }
        else {
            Log.e("JMS", "Invalid destination: " + destinationName);
            return null;
        }
        return destination;
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                Log.i("Recieved text message ", ((TextMessage) message).getText() + " from " + ((TextMessage) message).getStringProperty("author"));

                TextMessage textMessage = (TextMessage) message;

                if(textMessage.getBooleanProperty("group"))
                {
                    //its a group message
                    Log.i("Info", "It's a group message....");

                    if(!textMessage.getStringProperty("author").equals(LOCAL_USER))
                    {
                        for(int i = 0; i < this.conversations.size(); i++) {
                            if (this.conversations.get(i) instanceof GroupConversation) {
                                GroupConversation grp = (GroupConversation) this.conversations.get(i);

                                mNotificationBuilder = new Notification.Builder(getApplicationContext()).setContentTitle(textMessage.getStringProperty("groupName")).setContentText(textMessage.getStringProperty("author") + ": " + textMessage.getText()).setSmallIcon(R.drawable.ic_notify).setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

                                Log.i("Info", "Groupname is " + textMessage.getStringProperty("groupName"));

                                if(grp.getGroupName().equals(textMessage.getStringProperty("groupName")))
                                {
                                    //Group convo found
                                    Log.i("Info", "Group conversation found in datastore");
                                    ChatMessage cm = new ChatMessage(textMessage.getStringProperty("author"),new Date(),textMessage.getText());
                                    grp.addMessage(cm);

                                    if (this.activeConversation == null || !this.activeConversation.equals(grp))
                                    {
                                        mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
                                        this.mNotificationId = this.mNotificationId + 1;
                                    }

                                    notifyChatFragment(grp);
                                    return;
                                }
                            }
                        }
                    }
                }
                else {
                    //its a message for a single recipient
                    SingleRecipientConversation srp = getSingleRecipientConversationByAuthor(textMessage.getStringProperty("author"));

                    mNotificationBuilder = new Notification.Builder(getApplicationContext()).setContentTitle("Message from " + textMessage.getStringProperty("author")).setContentText(textMessage.getText()).setSmallIcon(R.drawable.ic_notify).setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

                    if (srp == null) {
                        Log.i("Info", "Adding new conversation for single recipient");
                        //No convo found
                        this.conversations.add(new SingleRecipientConversation(new ChatMessage(textMessage.getStringProperty("author"), new Date(), textMessage.getText()), getDestination("/queue/" + textMessage.getStringProperty("author"))));
                        this.mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
                        this.mNotificationId = this.mNotificationId + 1;
                        notifyChatFragment(null);
                    } else {
                        Log.i("Info", "Conversation found in datastore");

                        if (this.activeConversation == null || !this.activeConversation.equals(srp))
                            {
                            mNotificationManager.notify(mNotificationId, mNotificationBuilder.build());
                            this.mNotificationId = this.mNotificationId + 1;
                            }

                        ChatMessage cm = new ChatMessage(textMessage.getStringProperty("author"), new Date(), textMessage.getText());
                        srp.addMessage(cm);
                        notifyChatFragment(srp);
                    }
                }
            }
        }

        catch (JMSException exception)
        {
            Log.e("JMSException", exception.getMessage().toString());
        }

    }

    public SingleRecipientConversation getSingleRecipientConversationByAuthor(String author)
    {
        for(int i = 0; i < this.conversations.size(); i++)
        {
            if(this.conversations.get(i) instanceof SingleRecipientConversation)
            {
                SingleRecipientConversation srp = (SingleRecipientConversation) this.conversations.get(i);

                if(srp.getAuthor().equals(author))
                {
                    return srp;
                }
            }
        }

        return null;
    }

    public GroupConversation getGroupConversationByName(String groupName)
    {
        for(int i = 0; i < this.conversations.size(); i++)
        {
            if(this.conversations.get(i) instanceof GroupConversation)
            {
                GroupConversation grp = (GroupConversation) this.conversations.get(i);

                if(grp.getGroupName().equals(groupName))
                {
                    return grp;
                }
            }
        }

        return null;
    }

    public List<ChatConversation> getConversationList()
    {
        return this.conversations;
    }

    public ChatConversation getActiveConversation() { return this.activeConversation; };

    private void notifyChatFragment(ChatConversation cc)
    {
        ChatListFragment clf = (ChatListFragment) getSupportFragmentManager().findFragmentByTag(MainActivity.CHATLIST_TAG);

        Collections.sort(this.conversations, new DateComparator());

        clf.chatListUpdated();

        if(cc != null && cc.getDestinationQueue() == this.activeConversation.getDestinationQueue())
        {
            Log.i("Notify", "new message for active convo");
            ChatFragment cf = (ChatFragment) getSupportFragmentManager().findFragmentByTag(MainActivity.CHAT_TAG);

            cf.notifyMessageAdded();
        }
    }

    public void swapListForChat(ChatConversation chatConversation)
    {
        this.activeConversation = chatConversation;
        chatFragment = ChatFragment.newInstance("dummy1","dummy2");
        ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        ft.replace(R.id.fragmentContainer,chatFragment,MainActivity.CHAT_TAG);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
       this.activeConversation = null;
       super.onBackPressed();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

}
