package com.openshamba.watchdog.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.openshamba.watchdog.MainActivity;
import com.openshamba.watchdog.R;
import com.openshamba.watchdog.TrafficActivity;
import com.openshamba.watchdog.adapters.DataGridAdapter;
import com.openshamba.watchdog.adapters.Group;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.Notice;
import com.openshamba.watchdog.utils.Tools;


public class DataFragment extends Fragment {

    RecyclerView recyclerView;
    public DataGridAdapter mAdapter;
    private ProgressBar progressBar;
    private View view;
    private LinearLayout lyt_not_found;

    public DataFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_data, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        progressBar  = (ProgressBar) view.findViewById(R.id.progressBar);
        lyt_not_found   = (LinearLayout) view.findViewById(R.id.lyt_not_found);

        LinearLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), Tools.getGridSpanCount(getActivity()));
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);

        if (Constants.getGroupData(getActivity()).size() == 0) {
            lyt_not_found.setVisibility(View.VISIBLE);
        }else{
            lyt_not_found.setVisibility(View.GONE);
        }

        mAdapter = new DataGridAdapter(getContext(), Constants.getGroupData(getActivity()));
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener((v, obj, position) -> {
            if(obj.getId() == 0) {
                TrafficActivity.navigate((MainActivity) getActivity(),v.findViewById(R.id.lyt_parent),obj);
            }
            else {
                Notice.displayMessage(view,"You selected : "+ obj.getName(),"success");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        mAdapter.notifyDataSetChanged();
        super.onResume();
    }
}
