package com.example.celebrareproject;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.ViewHolder> {

    private final List<FAQItem> items;

    public FAQAdapter(List<FAQItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public FAQAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FAQAdapter.ViewHolder holder, int position) {
        FAQItem item = items.get(position);
        holder.tvQuestion.setText(item.getQuestion());
        holder.tvAnswer.setText(item.getAnswer());

        // Set visibility according to model
        holder.tvAnswer.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
        holder.divider.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);

        float targetRotation = item.isExpanded() ? 180f : 0f;


        // Rotate icon if expanded
        holder.ivExpand.setRotation(item.isExpanded() ? 180f : 0f);

        // Click listener on whole card (or on icon)
        holder.itemView.setOnClickListener(v -> {
            boolean expanded = item.isExpanded();
            item.setExpanded(!expanded);

            // animate
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView, new AutoTransition());
            notifyItemChanged(position);
        });

        // also allow tapping icon
        holder.ivExpand.setOnClickListener(v -> {
            boolean expanded = item.isExpanded();
            item.setExpanded(!expanded);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView, new AutoTransition());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;
        View divider;
        ViewGroup containerLayout;
        ImageView ivExpand;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            divider = itemView.findViewById(R.id.divider);
            containerLayout = itemView.findViewById(R.id.containerLayout);
        }
    }
    // inside FAQAdapter
    public void updateData(List<FAQItem> newList) {
        this.items.clear();
        this.items.addAll(newList);
        notifyDataSetChanged();
    }

}
