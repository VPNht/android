package ht.vpn.android.content.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.Preferences;
import ht.vpn.android.utils.PixelUtils;
import ht.vpn.android.utils.PrefUtils;
import ht.vpn.android.utils.ThreadUtils;

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.ViewHolder> implements VpnStatus.LogListener {

    private static final Integer MAX_LOG_ENTRIES = 1000;

    private Context mContext;
    private int mLogLevel = VpnProfile.MAXLOGLEVEL;
    private RecyclerView mRecyclerView;

    private Vector<LogItem> mLogEntries = new Vector<LogItem>();
    private Vector<LogItem> mLevelLogEntries = new Vector<LogItem>();

    public LogListAdapter(Context context, RecyclerView recyclerView) {
        initLogBuffer();

        mContext = context;
        mRecyclerView = recyclerView;

        mLogLevel = PrefUtils.get(context, Preferences.LOG_VERBOSITY, 2);
        VpnStatus.addLogListener(this);
    }

    @Override
    public LogListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        TextView textView = new TextView(mContext);
        textView.setTextColor(Color.BLACK);
        int pixels = PixelUtils.getPixelsFromDp(mContext, 16);
        textView.setPadding(pixels, 0, pixels, 0);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(LogListAdapter.ViewHolder viewHolder, int position) {
        LogItem item = mLevelLogEntries.get(position);
        String text = getTime(item) + " " + item.getString(mContext);
        viewHolder.itemView.setText(text);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mLevelLogEntries.size();
    }

    @Override
    public void newLog(LogItem logItem) {
        if(mLogEntries.size() == MAX_LOG_ENTRIES) {
            mLevelLogEntries.remove(mLogEntries.get(0));
            mLogEntries.removeElementAt(0);
        }

        mLogEntries.add(logItem);
        mLevelLogEntries.add(logItem);

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                mRecyclerView.smoothScrollToPosition(getItemCount() - 1);
            }
        });
    }

    private void initLogBuffer() {
        mLogEntries.clear();
        Collections.addAll(mLogEntries, VpnStatus.getlogbuffer());
        initLevelEntries();
    }

    private void initLevelEntries() {
        mLevelLogEntries.clear();
        for(LogItem li: mLogEntries) {
            if (li.getVerbosityLevel() <= mLogLevel || mLogLevel == VpnProfile.MAXLOGLEVEL)
                mLevelLogEntries.add(li);
        }
    }

    private String getTime(LogItem le) {
        Date d = new Date(le.getLogtime());
        SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return timeformat.format(d);
    }

    public String getLogAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (LogItem entry : mLogEntries) {
            stringBuilder.append(getTime(entry));
            stringBuilder.append(' ');
            stringBuilder.append(entry.getString(mContext));
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = (TextView) itemView;
        }
    }
}