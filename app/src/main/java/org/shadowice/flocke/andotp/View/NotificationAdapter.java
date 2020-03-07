package org.shadowice.flocke.andotp.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.shadowice.flocke.andotp.R;
import org.shadowice.flocke.andotp.Services.API.Models.NotificationModel;
import org.shadowice.flocke.andotp.View.ItemTouchHelper.AdapterItemClickListener;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyViewHolder> {

    private List<NotificationModel> notifications;
    private AdapterItemClickListener<String> customCallback = null;

    public NotificationAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.notification_item, viewGroup, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {

        holder.txtMessage.setText(notifications.get(position).getMessage());
        holder.txtTitle.setText(notifications.get(position).getTitle());
        holder.txtTime.setText(notifications.get(position).getTime());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (customCallback != null) {
                    customCallback.onItemClickData(notifications.get(position).getMessage());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setCustomCallback(AdapterItemClickListener<String> customCallback) {
        this.customCallback = customCallback;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView txtMessage,txtTitle,txtTime;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
