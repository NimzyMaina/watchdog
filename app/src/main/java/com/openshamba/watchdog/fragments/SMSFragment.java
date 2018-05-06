package com.openshamba.watchdog.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.openshamba.watchdog.R;
import com.openshamba.watchdog.adapters.SmsListAdapter;
import com.openshamba.watchdog.data.viewmodels.SmsViewModel;
import com.openshamba.watchdog.utils.EmptyRecyclerView;

import java.util.ArrayList;

public class SMSFragment extends Fragment {

    public EmptyRecyclerView recyclerView;
    private ProgressBar progressBar;
    public SmsListAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private SmsViewModel viewModel;
    private View view;
    private LinearLayout lyt_not_found;

    public SMSFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_sms, container, false);

        // activate fragment menu
        setHasOptionsMenu(true);

        recyclerView = (EmptyRecyclerView) view.findViewById(R.id.recyclerView2);
        lyt_not_found   = (LinearLayout) view.findViewById(R.id.lyt_not_found2);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar2);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        onRefreshLoading(true);

        mAdapter = new SmsListAdapter(getContext(),new ArrayList<>());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setEmptyView(lyt_not_found);

        viewModel = ViewModelProviders.of(this).get(SmsViewModel.class);

        viewModel.all().observe(SMSFragment.this,list ->{
            mAdapter.addItems(list);
            onRefreshLoading(false);
        });


        return view;
    }

    @Override
    public void onResume() {
        mAdapter.notifyDataSetChanged();
        super.onResume();
    }

    public void onRefreshLoading(boolean state) {
        if(state){
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
