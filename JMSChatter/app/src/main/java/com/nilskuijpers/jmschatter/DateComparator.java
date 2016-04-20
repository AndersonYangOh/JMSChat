package com.nilskuijpers.jmschatter;

import com.nilskuijpers.jmschatter.ObjectClasses.ChatConversation;

import java.util.Comparator;

/**
 * Created by surfa on 20-4-2016.
 */
public class DateComparator implements Comparator<ChatConversation> {

    @Override
    public int compare(ChatConversation lhs, ChatConversation rhs) {
        return rhs.getLastInteraction().compareTo(lhs.getLastInteraction());
    }
}
