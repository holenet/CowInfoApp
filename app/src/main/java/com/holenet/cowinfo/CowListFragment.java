package com.holenet.cowinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.holenet.cowinfo.item.Cow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CowListFragment extends Fragment {
    public static final int REQUEST_COW_DETAIL = 501;

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int columnCount = 1;
    private List<Cow> cows = new ArrayList<>();
    private CowRecyclerAdapter adapter;

    private GetCowListTask getCowListTask;

    public static CowListFragment newInstance(int columnCount) {
        CowListFragment fragment = new CowListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            columnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        adapter = new CowRecyclerAdapter(cows, new CowRecyclerAdapter.OnCowSelectedListener() {
            @Override
            public void onCowSelected(Cow cow, int position) {
                Log.e("onCowSelected", position+": "+cow.toString());
                Intent intent = new Intent(CowListFragment.this.getContext(), CowDetailActivity.class);
                intent.putExtra("cow_list", (ArrayList<Cow>)cows);
                intent.putExtra("position", position);
                startActivityForResult(intent, REQUEST_COW_DETAIL);
            }
        });
        attemptGetCowList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cow_list, container, false);
        Context context = view.getContext();

        RecyclerView rVcowList = view.findViewById(R.id.rVcowList);
        if (columnCount <= 1) {
            rVcowList.setLayoutManager(new LinearLayoutManager(context));
        } else {
            rVcowList.setLayoutManager(new GridLayoutManager(context, columnCount));
        }
        rVcowList.setAdapter(adapter);

        return view;
    }

    public void attemptGetCowList() {
        if (getCowListTask != null) {
            return;
        }

        getCowListTask = new GetCowListTask(this);
        getCowListTask.execute(false);
    }

    private void setCowList(List<Cow> cows) {
        this.cows.clear();
        this.cows.addAll(cows);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COW_DETAIL) {
            attemptGetCowList();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private static class GetCowListTask extends NetworkService.Task<CowListFragment, Boolean, List<Cow>> {
        public GetCowListTask(CowListFragment holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<List<Cow>> request(Boolean deleted) {
            return NetworkService.getCowList(deleted);
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            getHolder().getCowListTask = null;
        }

        @Override
        protected void responseSuccess(List<Cow> cows) {
            getHolder().setCowList(cows);
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
            existErrors(errors, getHolder().getContext());
        }
    }

    static class CowRecyclerAdapter extends RecyclerView.Adapter<CowRecyclerAdapter.ViewHolder> {
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
            final Cow cow = cows.get(position);
            holder.tVsummary.setText(cow.getSummary());
            holder.tVcount.setText("이력 "+String.valueOf(cow.records.size()));
            holder.tVnumber.setText(cow.number);
            if (cow.birthday != null) {
                holder.tVbirthday.setVisibility(View.VISIBLE);
                holder.tVbirthday.setText("출생 "+cow.birthday);
            } else {
                holder.tVbirthday.setVisibility(View.GONE);
            }

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onCowSelected(cow, holder.getAdapterPosition());
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

            ViewHolder(View view) {
                super(view);
                this.view = view;
                tVsummary = view.findViewById(R.id.tVsummary);
                tVcount = view.findViewById(R.id.tVcount);
                tVnumber = view.findViewById(R.id.tVnumber);
                tVbirthday = view.findViewById(R.id.tVbirthday);
            }
        }

        public interface OnCowSelectedListener {
            void onCowSelected(Cow cow, int position);
        }
    }
}
