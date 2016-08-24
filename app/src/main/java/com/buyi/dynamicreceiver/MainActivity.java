package com.buyi.dynamicreceiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.buyi.dynamicreceiver.ams_hook.AMSHookHelper;
import com.buyi.dynamicreceiver.classloder_hook.LoadedApkClassLoaderHookHelper;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int PATCH_BASE_CLASS_LOADER = 1;

    private static final int CUSTOM_CLASS_LOADER = 2;

    private static final int HOOK_METHOD = CUSTOM_CLASS_LOADER;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new ContextWrapperSelf(newBase));
        try {
//            Utils.extractAssets(newBase, "dynamic-proxy-hook.apk");
//            Utils.extractAssets(newBase, "ams-pms-hook.apk");
            Utils.extractAssets(newBase, "plugin.apk");

//            if (HOOK_METHOD == PATCH_BASE_CLASS_LOADER) {
//                File dexFile = getFileStreamPath("test.apk");
//                File optDexFile = getFileStreamPath("test.dex");
//                BaseDexClassLoaderHookHelper.patchClassLoader(getClassLoader(), dexFile, optDexFile);
//            } else {
                LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(this, getFileStreamPath("plugin.apk"));
//            }

            AMSHookHelper.hookActivityManagerNative();
            AMSHookHelper.hookActivityThreadHandler();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        setContentView(R.layout.activity_main);

        try {
            PluginReceiverLoader.getInstance().registerReceivers(this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("wocao1");
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.buyi.dynamicreceiver.receiver");
        registerReceiver(receiver, f);

//        Utils.extractAssets(this, "test.jar");
        File testPlugin = getFileStreamPath("plugin.apk");
        try {
            ReceiverHelper.preLoadReceiver(this, testPlugin);
            Log.i(getClass().getSimpleName(), "hook success");
        } catch (Exception e) {
            throw new RuntimeException("receiver load failed", e);
        }


        findViewById(R.id.id_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.buyi.dynamicreceiver.receiver");
                sendBroadcast(intent);


                Intent intent1 = new Intent("com.buyi.dynamicreceiver.static");
                sendBroadcast(intent1);


            }
        });


        Log.d(TAG, "context classloader: " + getApplicationContext().getClassLoader());
        findViewById(R.id.id_button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent t = new Intent();
                    t.setComponent(new ComponentName("com.buyi.plugin", "com.buyi.plugin.TestActivity"));
                    startActivity(t);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
