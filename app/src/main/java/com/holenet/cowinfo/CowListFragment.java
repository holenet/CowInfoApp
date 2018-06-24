package com.holenet.cowinfo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.holenet.cowinfo.item.Cow;

import java.util.ArrayList;
import java.util.List;

public class CowListFragment extends Fragment {
    private static final String ARG_COLUMN_COUNT = "column-count";
    private int columnCount = 1;
    private List<Cow> cows = new ArrayList<>();
//    private OnListFragmentInteractionListener listener;
    CowRecyclerAdapter adapter;

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

        /* Dummy items */
        Cow cow1 = new Cow();
        cow1.number = "number1";
        Cow cow2 = new Cow();
        cow2.number = "number2";
        cows.add(cow1);
        cows.add(cow2);

        adapter = new CowRecyclerAdapter(cows);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cow_list, container, false);

        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (columnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, columnCount));
            }
            recyclerView.setAdapter(adapter);
        }
        /* Just for testing */
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                Cow cow = new Cow();
                cow.number = "aoiwejfoawiefj";
                cows.add(cow);
                adapter.notifyDataSetChanged();
            }
        }, 2000);
        return view;
    }


/*
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            listener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }
*/

    @Override
    public void onDetach() {
        super.onDetach();
//        listener = null;
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
/*
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(DummyItem item);
    }
*/
}
