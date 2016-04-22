package com.nilskuijpers.jmschatter.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.nilskuijpers.jmschatter.Adapters.ChatListAdapter;
import com.nilskuijpers.jmschatter.MainActivity;
import com.nilskuijpers.jmschatter.ObjectClasses.ChatConversation;
import com.nilskuijpers.jmschatter.ObjectClasses.GroupConversation;
import com.nilskuijpers.jmschatter.ObjectClasses.SingleRecipientConversation;
import com.nilskuijpers.jmschatter.R;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatListFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    //UI Properties
    private RecyclerView chatListRecycler;
    private ChatListAdapter chatListAdapter;
    private RecyclerView.LayoutManager chatListLayoutManager;
    private FloatingActionButton newChatFab;
    private FloatingActionButton newGroupFab;
    private EditText newChatRecipient;
    private EditText newGroupName;

    private List<ChatConversation> conversations;


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ChatListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatListFragment newInstance(String param1, String param2) {
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_chat_list, container, false);
        final MainActivity activity = (MainActivity) getActivity();
        this.chatListRecycler = (RecyclerView) v.findViewById(R.id.chatListRecycler);
        this.conversations = activity.getConversationList();
        this.chatListAdapter = new ChatListAdapter(conversations);
        //init ui properties
        this.newChatFab = (FloatingActionButton) v.findViewById(R.id.newChatFab);
        this.newGroupFab = (FloatingActionButton) v.findViewById(R.id.newGroupChatFab);
        this.newChatRecipient = (EditText) v.findViewById(R.id.recipientText);
        this.newGroupName = (EditText) v.findViewById(R.id.groupNameText);


        newChatFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    SingleRecipientConversation newCrp = activity.getSingleRecipientConversationByAuthor(newChatRecipient.getText().toString());

                    if(newCrp == null)
                    {
                        newCrp = new SingleRecipientConversation(activity.getDestination("/queue/" + newChatRecipient.getText().toString()),newChatRecipient.getText().toString());
                        conversations.add(newCrp);
                        chatListUpdated();
                    }

                    activity.swapListForChat(newCrp);

                    //sendMessageTo("Hello world!", getDestination("/queue/" + newChatRecipient.getText().toString()));
                } catch (Exception e) {
                    Log.e("ERROR", e.getMessage().toString());
                }
            }
        });

        newGroupFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                GroupConversation newGrp = activity.getGroupConversationByName(newGroupName.getText().toString());

                if(newGrp == null)
                {
                    newGrp = new GroupConversation(activity.getDestination("/topic/" + newGroupName.getText().toString()),newGroupName.getText().toString());
                    activity.subscribeTo("/topic/" + newGrp.getGroupName());
                    conversations.add(newGrp);
                    chatListUpdated();
                }

                }
                catch (JMSException e)
                {
                    e.printStackTrace();
                }
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(View view , Bundle savedInstanceState) {
        this.chatListRecycler.setAdapter(this.chatListAdapter);
        this.chatListRecycler.setHasFixedSize(true);
        this.chatListLayoutManager = new LinearLayoutManager(getContext());
        this.chatListRecycler.setLayoutManager(this.chatListLayoutManager);
        this.chatListAdapter.SetOnItemClickListener(new ChatListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, ChatConversation mConversation) {
                MainActivity activity = (MainActivity) getActivity();

                activity.swapListForChat(mConversation);
            }
        });
    }


        // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void chatListUpdated() {
        final MainActivity activity = (MainActivity) getActivity();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                conversations = activity.getConversationList();
                chatListAdapter.notifyDataSetChanged();
            }
        });
        //this.chatListAdapter.notifyItemInserted(this.chatListAdapter.getItemCount() + 1);
    }


}
