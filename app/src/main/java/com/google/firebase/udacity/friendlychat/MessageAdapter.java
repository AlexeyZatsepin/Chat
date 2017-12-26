package com.google.firebase.udacity.friendlychat;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{
    private List<Message> messages;
    private String name;
    private static String TAG = MessageAdapter.class.getName();

    private static final int MY_MESSAGE = 0;
    private static final int OTHER_MESSAGE = 1;


    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setCurrentUser(String name){
        this.name = name;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layout;
        if (viewType == MY_MESSAGE){
            layout = R.layout.item_my_message;
        } else if (viewType == OTHER_MESSAGE) {
            layout = R.layout.item_message;
        } else {
            layout = R.layout.item_message;
        }
        return new MessageViewHolder(inflater.inflate(layout, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getName().equals(name) ? MY_MESSAGE : OTHER_MESSAGE;
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        Message friendlyMessage = messages.get(position);
        if (friendlyMessage.getText() != null) {
            holder.messageTextView.setText(friendlyMessage.getText());
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.messageImageView.setVisibility(View.GONE);
        } else {
            String imageUrl = friendlyMessage.getImageUrl();
            if (imageUrl != null && imageUrl.startsWith("gs://")) {
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(holder.messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(holder.messageImageView);
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });
            } else {
                Glide.with(holder.messageImageView.getContext())
                        .load(friendlyMessage.getImageUrl())
                        .into(holder.messageImageView);
            }
            holder.messageImageView.setVisibility(View.VISIBLE);
            holder.messageTextView.setVisibility(View.GONE);
        }


        if (friendlyMessage.getName().contains("Firebase")){
            holder.messengerTextView.setVisibility(View.GONE);

        }
        holder.messengerTextView.setText(friendlyMessage.getName());
        if (friendlyMessage.getPhotoUrl() == null) {
            holder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(holder.messengerImageView.getContext(),
                    R.drawable.ic_account_circle_black_36dp));
        } else {
            Glide.with(holder.messengerImageView.getContext())
                    .load(friendlyMessage.getPhotoUrl())
                    .into(holder.messengerImageView);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }
}
