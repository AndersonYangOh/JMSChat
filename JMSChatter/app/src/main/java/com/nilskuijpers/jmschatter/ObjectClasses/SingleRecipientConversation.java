package com.nilskuijpers.jmschatter.ObjectClasses;

import javax.jms.Destination;

/**
 * Created by surfa on 20-4-2016.
 */
public class SingleRecipientConversation extends ChatConversation {

    private String author;

    public SingleRecipientConversation(Destination destinationQueue, String recipient)
    {
        super(destinationQueue);
        this.author = recipient;
    }

    public SingleRecipientConversation(ChatMessage initialMessage, Destination destinationQueue) {
        super(initialMessage, destinationQueue);
        this.author = initialMessage.getAuthor();
    }

    public String getAuthor()
    {
        return this.author;
    }
}
