package com.nilskuijpers.jmschat.Classes;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nilskuijpers.jmschat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by surfa on 19-4-2016.
 */
public class ChatsAdapter extends  RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

    private LinkedHashMap<String, ArrayList<ChatMessage>> ChatDb;
    private List<ArrayList<ChatMessage>> list;


    @Override
    public ChatsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_layout, parent, false);

        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(ChatsAdapter.ViewHolder holder, int position) {
        Log.i("List size: ", String.valueOf(list.get(position).size()));
        ArrayList<ChatMessage> tempList = list.get(position);
        holder.mTextLastMessageNameAndDate.setText(tempList.get(tempList.size() -1).getUserName());
        holder.mTextLastMessageContent.setText(tempList.get(tempList.size() - 1).getMessageContent());

    }




    @Override
    public int getItemCount() {
        return this.list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextLastMessageNameAndDate;
        private TextView mTextLastMessageContent;
        public ViewHolder(View itemView) {
            super(itemView);

            mTextLastMessageNameAndDate = (TextView) itemView.findViewById(R.id.chatViewLastMessageNameAndDate);
            mTextLastMessageContent = (TextView) itemView.findViewById(R.id.chatViewLastMessageContent);
        }
    }

    public ChatsAdapter(LinkedHashMap<String, ArrayList<ChatMessage>> chatDb)
    {
        this.ChatDb = chatDb;
        this.list = new ArrayList<ArrayList<ChatMessage>>(chatDb.values());
    }
}
