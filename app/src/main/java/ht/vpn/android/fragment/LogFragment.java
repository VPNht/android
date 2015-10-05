package ht.vpn.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import butterknife.Bind;
import ht.vpn.android.R;
import ht.vpn.android.content.adapters.LogListAdapter;

public class LogFragment extends BaseFragment {

    private LogListAdapter mAdapter;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    public LogFragment() {
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new LogListAdapter(getActivity(), mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
    }

    private void shareLog() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, mAdapter.getLogAsString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.ics_openvpn_log_file));
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.send_logfile)));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_log, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sendlog:
                shareLog();
                break;
            case android.R.id.home:
                getActivity().finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

