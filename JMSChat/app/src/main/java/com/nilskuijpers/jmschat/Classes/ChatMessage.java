package com.nilskuijpers.jmschat.Classes;

import java.util.Date;

/**
 * Created by surfa on 19-4-2016.
 */
public class ChatMessage {
    public String getUserName() {
        return userName;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public String getMessageContent() {
        return messageContent;
    }

    private String userName;
    private Date dateTime;
    private String messageContent;

    public ChatMessage(String UserName, Date DateTime, String MessageContent)
    {
        this.userName = UserName;
        this.dateTime = DateTime;
        this.messageContent = MessageContent;
    }
}
