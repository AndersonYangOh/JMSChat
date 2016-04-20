package com.nilskuijpers.jmschatter.ObjectClasses;

import java.util.Date;

/**
 * Created by surfa on 19-4-2016.
 */
public class ChatMessage {
    private String author;
    private Date timeSent;
    private String message;

    public ChatMessage(String author, Date timeSent, String message)
    {
        this.author = author;
        this.timeSent = timeSent;
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public Date getTimeSent() {
        return timeSent;
    }

    public String getMessage() {
        return message;
    }
}
