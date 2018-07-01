package com.holenet.cowinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.holenet.cowinfo.item.Cow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CowListFragment extends Fragment {
    public static final int REQUEST_COW_DETAIL = 501;

    private static final String ARG_IS_DELETED_LIST = "is_deleted_list";
    private boolean isDeletedList = false;

    private List<Cow> cows = new ArrayList<>();
    private CowRecyclerAdapter adapter;

    private GetCowListTask getCowListTask;

    public static CowListFragment newInstance(boolean isDeletedList) {
        CowListFragment fragment = new CowListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DELETED_LIST, isDeletedList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle arguments = getArguments();
        if (arguments != null) {
            isDeletedList = arguments.getBoolean(ARG_IS_DELETED_LIST);
        }

        adapter = new CowRecyclerAdapter(cows, new CowRecyclerAdapter.OnCowSelectedListener() {
            @Override
            public void onCowSelected(Cow cow, int position) {
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
        rVcowList.setLayoutManager(new LinearLayoutManager(context));
        rVcowList.setAdapter(adapter);

        return view;
    }

    Menu menu;
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_cow_list, menu);
        menu.findItem(R.id.mIdeleted).setVisible(!isDeletedList);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mIrefresh) {
            attemptGetCowList();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void attemptGetCowList() {
        if (getCowListTask != null) {
            return;
        }

        getCowListTask = new GetCowListTask(this);
        getCowListTask.execute(isDeletedList);
    }

    private void setCowList(List<Cow> cows) {
        this.cows.clear();
        this.cows.addAll(cows);
        adapter.notifyDataSetChanged();
    }

    public void setIsDeletedList(boolean isDeletedList) {
        if (this.isDeletedList == isDeletedList)
            return;

        if (menu != null) {
            menu.findItem(R.id.mIdeleted).setVisible(!isDeletedList);
        }
        this.isDeletedList = isDeletedList;
        attemptGetCowList();
    }

    public boolean isDeletedList() {
        return isDeletedList;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COW_DETAIL) {
            attemptGetCowList();
        }
        super.onActivityResult(requestCode, resultCode, data);
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
