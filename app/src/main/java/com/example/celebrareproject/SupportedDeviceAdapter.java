package com.example.celebrareproject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SupportedDeviceAdapter extends RecyclerView.Adapter<SupportedDeviceAdapter.VH> {

    private static final String TAG = "SupportedDeviceAdapter";

    private final Context context;
    private final List<Device> items;

    // NO ACTION LISTENER NOW (because you are not using it)
    private final int layoutResId = R.layout.item_device_standard;

    public SupportedDeviceAdapter(Context context, List<Device> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Device d = items.get(position);

        // title
        holder.tvTitle.setText(d.title);

        // description
        holder.tvDesc.setText(d.description);

        // chips
        if (d.chipLeft != null) {
            holder.chip1.setVisibility(View.VISIBLE);
            holder.chip1.setText(d.chipLeft);
        } else {
            holder.chip1.setVisibility(View.GONE);
        }

        if (d.chipRight != null) {
            holder.chip2.setVisibility(View.VISIBLE);
            holder.chip2.setText(d.chipRight);
        } else {
            holder.chip2.setVisibility(View.GONE);
        }

        // image
        if (d.imageRes != 0) {
            holder.ivDevice.setImageResource(d.imageRes);
        } else {
            holder.ivDevice.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // button
        if (d.showButton) {
            holder.btnAction.setVisibility(View.VISIBLE);
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivDevice;
        TextView tvTitle, tvDesc, chip1, chip2;
        Button btnAction;

        VH(View itemView) {
            super(itemView);

            ivDevice = itemView.findViewById(R.id.ivDevice);
            tvTitle = itemView.findViewById(R.id.tvDeviceTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            chip1 = itemView.findViewById(R.id.chip1);
            chip2 = itemView.findViewById(R.id.chip2);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
