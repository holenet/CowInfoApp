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
//    private final OnListFragmentInteractionListener mListener;

    public CowRecyclerAdapter(List<Cow> items) {
        cows = items;
//        mListener = listener;
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
        holder.tVid.setText(String.valueOf(cows.get(position).id));
        holder.tVcontent.setText(cows.get(position).number);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (null != mListener) {
//                     Notify the active callbacks interface (the activity, if the
//                     fragment is attached to one) that an item has been selected.
//                    mListener.onListFragmentInteraction(holder.mItem);
//                }
                Log.d("onClick", holder.toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cows.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final TextView tVid;
        public final TextView tVcontent;
        public Cow cow;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            tVid = view.findViewById(R.id.item_number);
            tVcontent = view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + tVcontent.getText() + "'";
        }
    }
}
