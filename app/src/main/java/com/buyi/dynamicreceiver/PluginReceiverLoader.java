package com.buyi.dynamicreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by chan on 16/5/15.
 */
public class PluginReceiverLoader {

    private DexClassLoader m_dexClassLoader;
    private Map<String, List<String>> m_receivers;

    private static PluginReceiverLoader s_pluginReceiverLoader;

    public PluginReceiverLoader(DexClassLoader dexClassLoader, Map<String, List<String>> receivers) {
        m_dexClassLoader = dexClassLoader;
        m_receivers = receivers;
    }

    public List<BroadcastReceiver> registerReceivers(Context context)
            throws ClassNotFoundException,
            IllegalAccessException, InstantiationException {

        List<BroadcastReceiver> list = new ArrayList<>();
        if (m_dexClassLoader == null) {
            return list;
        }

        for (Map.Entry<String, List<String>> entry : m_receivers.entrySet()) {
            BroadcastReceiver broadcastReceiver = (BroadcastReceiver) m_dexClassLoader.loadClass(entry.getKey()).newInstance();
            IntentFilter intentFilter = new IntentFilter();
            for (String action : entry.getValue()) {
                intentFilter.addAction(action);
            }

            context.registerReceiver(broadcastReceiver, intentFilter);
            list.add(broadcastReceiver);
        }

        return list;
    }

    public void unregisterReceivers(Context context, List<BroadcastReceiver> broadcastReceiverList) {
        for (BroadcastReceiver broadcastReceiver : broadcastReceiverList) {
            context.unregisterReceiver(broadcastReceiver);
        }
    }

    public static void init(DexClassLoader dexClassLoader, Map<String, List<String>> receivers) {
        s_pluginReceiverLoader = new PluginReceiverLoader(dexClassLoader, receivers);
    }

    public static PluginReceiverLoader getInstance() {
        return s_pluginReceiverLoader;
    }
}
