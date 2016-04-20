package com.nilskuijpers.jmschatter.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nilskuijpers.jmschatter.MainActivity;
import com.nilskuijpers.jmschatter.ObjectClasses.ChatConversation;
import com.nilskuijpers.jmschatter.ObjectClasses.ChatMessage;
import com.nilskuijpers.jmschatter.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by surfa on 20-4-2016.
 */
public class ChatAdapter  extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> mChatMessages)
    {
        this.chatMessages = mChatMessages;
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout ll = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_row, parent, false);

        ViewHolder vh = new ViewHolder(ll);

        return vh;
    }

    @Override
    public void onBindViewHolder(ChatAdapter.ViewHolder holder, int position) {
        ChatMessage cm = this.chatMessages.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.GERMAN);
        holder.mMessageAuthor.setText(cm.getAuthor());
        holder.mMessageDate.setText(sdf.format(cm.getTimeSent()));
        holder.mMessageContent.setText(cm.getMessage());

        if(cm.getAuthor().equals(MainActivity.LOCAL_USER)){
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            spacerLp.weight = Float.valueOf("0.2");
            holder.spacer.setLayoutParams(spacerLp);

            LinearLayout.LayoutParams cardContainerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardContainerLp.weight = Float.valueOf("0.8");
            holder.container.setLayoutParams(cardContainerLp);
        }

        else
        {
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            spacerLp.weight = Float.valueOf("0");
            holder.spacer.setLayoutParams(spacerLp);

            LinearLayout.LayoutParams cardContainerLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardContainerLp.weight = Float.valueOf("0.8");
            holder.container.setLayoutParams(cardContainerLp);

        }


    }

    @Override
    public int getItemCount() {
       return this.chatMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public TextView mMessageAuthor;
        public TextView mMessageDate;
        public TextView mMessageContent;
        public LinearLayout spacer;
        public LinearLayout container;
        public ViewHolder(LinearLayout itemView) {
            super(itemView);
            this.mMessageAuthor = (TextView) itemView.findViewById(R.id.MessageAuthor);
            this.mMessageDate = (TextView) itemView.findViewById(R.id.MessageDate);
            this.mMessageContent = (TextView) itemView.findViewById(R.id.MessageContent);
            this.spacer = (LinearLayout) itemView.findViewById(R.id.messageSpacer);
            this.container = (LinearLayout) itemView.findViewById(R.id.cardContainer);
        }
    }
}
