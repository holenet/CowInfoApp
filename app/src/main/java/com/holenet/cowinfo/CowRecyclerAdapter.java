package com.holenet.cowinfo;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.holenet.cowinfo.item.Cow;

import java.util.List;

public class CowRecyclerAdapter extends RecyclerView.Adapter<CowRecyclerAdapter.ViewHolder> {
    private final List<Cow> cows;
    private final OnCowSelectedListener listener;

    public CowRecyclerAdapter(List<Cow> items, OnCowSelectedListener listener) {
        cows = items;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.cow = cows.get(position);
        char sexSymbol = (holder.cow.sex.equals("female") ? '♀' : '♂');
        String summary = holder.cow.number.split("-")[2]+" "+sexSymbol;
        holder.tVsummary.setText(summary);
        holder.tVcount.setText("이력 "+String.valueOf(holder.cow.records.size()));
        holder.tVnumber.setText(cows.get(position).number);
        if (holder.cow.birthday != null) {
            holder.tVbirthday.setVisibility(View.VISIBLE);
            holder.tVbirthday.setText("출생 "+holder.cow.birthday);
        } else {
            holder.tVbirthday.setVisibility(View.GONE);
        }

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onCowSelected(holder.cow, holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cows.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        final TextView tVsummary, tVcount, tVnumber, tVbirthday;
        Cow cow;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            tVsummary = view.findViewById(R.id.tVsummary);
            tVcount = view.findViewById(R.id.tVcount);
            tVnumber = view.findViewById(R.id.tVnumber);
            tVbirthday = view.findViewById(R.id.tVbirthday);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + tVnumber.getText() + "'";
        }
    }

    public interface OnCowSelectedListener {
        void onCowSelected(Cow cow, int position);
    }
}
