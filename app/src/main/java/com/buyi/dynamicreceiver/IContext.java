package com.buyi.dynamicreceiver;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

/**
 * Created by buyi on 16/8/23.
 */
public interface IContext {
    Intent registerReceiver(@Nullable BroadcastReceiver receiver,
                            IntentFilter filter);
}
