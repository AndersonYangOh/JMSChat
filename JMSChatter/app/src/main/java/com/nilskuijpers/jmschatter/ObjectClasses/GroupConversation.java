package com.nilskuijpers.jmschatter.ObjectClasses;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;

/**
 * Created by surfa on 20-4-2016.
 */
public class GroupConversation extends ChatConversation  {

    private List<String> authors;
    private String groupName;

    public GroupConversation(ChatMessage initialMessage, Destination destinationQueue, String groupName) {
        super(initialMessage, destinationQueue);

        authors = new ArrayList<>();
        authors.add(initialMessage.getAuthor());

        this.groupName = groupName;
    }

    @Override
    public void addMessage(ChatMessage messageToAdd) {
        super.addMessage(messageToAdd);
        if(!authors.contains(messageToAdd.getAuthor()))
        {
            authors.add(messageToAdd.getAuthor());
        }
    }

    public List<String> getAllAuthors()
    {
        return this.authors;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
