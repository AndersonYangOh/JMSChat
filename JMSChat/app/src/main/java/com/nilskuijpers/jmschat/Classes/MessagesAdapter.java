package com.nilskuijpers.jmschat.Classes;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nilskuijpers.jmschat.R;

import java.util.List;

/**
 * Created by surfa on 18-4-2016.
 */
public class MessagesAdapter extends  RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private List<ChatMessage> receivedMessages;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout, parent, false);

        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextViewMessage.setText(receivedMessages.get(position).getMessageContent());
        holder.mTextViewSenderAndDate.setText(receivedMessages.get(position).getUserName() + " / " + receivedMessages.get(position).getDateTime().toString());
    }

    @Override
    public int getItemCount() {
        return receivedMessages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mTextViewMessage;
        public TextView mTextViewSenderAndDate;

        public ViewHolder(LinearLayout v)
        {
            super(v);
            mTextViewMessage = (TextView) v.findViewById(R.id.info_text);
            mTextViewSenderAndDate = (TextView) v.findViewById(R.id.senderAndDate);
        }
    }

    public MessagesAdapter(List<ChatMessage> receivedMessages)
    {
        this.receivedMessages = receivedMessages;
    }
}
