package com.buyi.plugin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.lang.reflect.Field;

/**
 * Created by buyi on 16/8/23.
 */
public class TestActivity extends Activity {


    /**
     * 两个反射工具类
     *
     * @param obj
     * @param fieldName
     * @return
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        Class<?> c = obj.getClass();
        Object value = null;
        while (c != null) {
            try {
                Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                value = field.get(obj);
                if (value != null) {
                    return value;
                }
            } catch (SecurityException e) {
            } catch (NoSuchFieldException e) {
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } finally {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setFieldValue(Object obj, String fieldName,
                                      Object fieldValue) {
        if (obj == null || fieldName == null) {
            return;
        }
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, fieldValue);
                return;
            } catch (SecurityException e) {
            } catch (NoSuchFieldException e) {
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } finally {
                clazz = clazz.getSuperclass();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
//        Context mBase = (Context) getFieldValue(this, "mBase");
        setFieldValue(newBase, "mBasePackageName", "com.buyi.dynamicreceiver");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                    System.out.println("wocao");
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.buyi.dynamicreceiver.receiver");
        registerReceiver(receiver, f);
    }
}
