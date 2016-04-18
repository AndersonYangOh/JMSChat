package com.nilskuijpers.jmschat.Classes;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nilskuijpers.jmschat.R;

import java.util.List;

/**
 * Created by surfa on 18-4-2016.
 */
public class MessagesAdapter extends  RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private List<String> receivedMessages;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout, parent, false);

        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(receivedMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return receivedMessages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mTextView;

        public ViewHolder(TextView v)
        {
            super(v);
            mTextView = v;
        }
    }

    public MessagesAdapter(List<String> receivedMessages)
    {
        this.receivedMessages = receivedMessages;
    }
}
