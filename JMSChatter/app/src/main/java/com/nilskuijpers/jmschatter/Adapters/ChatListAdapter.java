package com.nilskuijpers.jmschatter.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nilskuijpers.jmschatter.ObjectClasses.ChatConversation;
import com.nilskuijpers.jmschatter.R;

import java.util.List;

/**
 * Created by surfa on 20-4-2016.
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder>
{
    private List<ChatConversation> conversations;
    OnItemClickListener mItemClickListener;

    @Override
    public ChatListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout ll = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_row, parent, false);

        ViewHolder vh = new ViewHolder(ll);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mLatestInteraction.setText(conversations.get(position).toString());
        holder.mConversation = conversations.get(position);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView mLatestInteraction;
        public ChatConversation mConversation;
        public ViewHolder(LinearLayout itemView) {
            super(itemView);
            this.mLatestInteraction = (TextView) itemView.findViewById(R.id.latestInteraction);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getPosition(), mConversation);
        }
    }

    public interface OnItemClickListener
    {
        public void onItemClick(View view, int position, ChatConversation mConversation);
    }

    public ChatListAdapter(List<ChatConversation> conversations)
    {
        this.conversations = conversations;
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }
}
