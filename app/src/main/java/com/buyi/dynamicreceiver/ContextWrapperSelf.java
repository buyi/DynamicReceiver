package com.buyi.dynamicreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by buyi on 16/8/24.
 */
public class ContextWrapperSelf extends android.content.ContextWrapper {


    Context mBase;
    private List<BroadcastReceiver> receivers = new ArrayList<>();
    private List<IntentFilter> filters = new ArrayList<>();

    HashMap<IntentFilter, BroadcastReceiver> mapps = new HashMap<>();
    /**
     * Proxying implementation of Context that simply delegates all of its calls to
     * another Context.  Can be subclassed to modify behavior without changing
     * the original Context.
     */

        public ContextWrapperSelf(Context base) {
            super(base);
            mBase = base;
        }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        mapps.put(filter, receiver);
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        super.sendBroadcast(intent);

//        PackageManage
    }
}


