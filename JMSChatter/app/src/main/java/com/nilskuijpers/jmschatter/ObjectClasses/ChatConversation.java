package com.nilskuijpers.jmschatter.ObjectClasses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.Destination;

/**
 * Created by surfa on 19-4-2016.
 */
public abstract class ChatConversation {

    private List<ChatMessage> chatMessages;
    private Date lastInteraction;
    private String singleAuthor;
    private Destination destinationQueue;


    public Destination getDestinationQueue()
    {
        return this.destinationQueue;
    }

    public List<ChatMessage> getMessageList()
    {
        return this.chatMessages;
    }

    public ChatConversation(Destination destinationQueue)
    {
        this.destinationQueue = destinationQueue;
        this.chatMessages = new ArrayList<>();
        this.lastInteraction = new Date();
    }

    public ChatConversation(ChatMessage initialMessage, Destination destinationQueue)
    {
        this.chatMessages = new ArrayList<>();
        this.chatMessages.add(initialMessage);
        this.lastInteraction = new Date();
        this.destinationQueue = destinationQueue;
    }

    public void addMessage(ChatMessage messageToAdd)
    {
        this.chatMessages.add(messageToAdd);
        this.lastInteraction = new Date();
    }

    public Date getLastInteraction()
    {
        return this.lastInteraction;
    }

    @Override
    public String toString()
    {
        if(this.chatMessages.size() > 0) {
            ChatMessage lastMessage = this.chatMessages.get(this.chatMessages.size() - 1);
            return lastMessage.getAuthor() + ": " + lastMessage.getMessage();
        }

        else
        {
            return "No messages";
        }
    }

    public int getNumberOfMessages()
    {
        return this.chatMessages.size();
    }

    public ChatMessage getMessageAtIndex(int index)
    {
        return this.chatMessages.get(index);
    }
}
