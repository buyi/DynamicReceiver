package com.buyi.dynamicreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.util.Log;

import com.buyi.dynamicreceiver.classloder_hook.CustomClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author weishu
 * @date 16/4/7
 */
public final class ReceiverHelper {

    private static final String TAG = "ReceiverHelper";

    public static Map<ActivityInfo, List<? extends IntentFilter>> sCache =
            new HashMap<ActivityInfo, List<? extends IntentFilter>>();

    /**
     * 解析Apk文件中的 <receiver>, 并存储起来
     *
     * @param apkFile
     * @param context
     * @throws Exception
     */
    private static void parserReceivers(File apkFile, Context context) throws Exception {
        Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");


        Class<?>[] arrayParseType = new Class[4];
        arrayParseType[0] = File.class;
        arrayParseType[1] = String.class;
        arrayParseType[2] = DisplayMetrics.class;
        arrayParseType[3] = Integer.TYPE;

        Object[] arrayParseParam = new Object[4];
        arrayParseParam[0] = apkFile;
        arrayParseParam[1] = apkFile.getAbsolutePath();
        arrayParseParam[2] = context.getResources().getDisplayMetrics();
        arrayParseParam[3] = Integer.valueOf(0);

        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", arrayParseType);

        //生成一个package parser 对象
        Constructor<?> packageParseConstructorMethod = packageParserClass.getConstructor(String.class);
        // 根据构造方法信息构造PackageParser类实例
        Object packageParserObject = packageParseConstructorMethod.newInstance(apkFile.getAbsolutePath());

        //获得Package对象
        Object packageObj = parsePackageMethod.invoke(packageParserObject, arrayParseParam);
//        Method parsePackageMethod = packageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
//
//        Object packageParser = packageParserClass.newInstance();
//
//        // 首先调用parsePackage获取到apk对象对应的Package对象
//        Object packageObj = parsePackageMethod.invoke(packageParser, apkFile, PackageManager.GET_RECEIVERS);

        // 读取Package对象里面的receivers字段,注意这是一个 List<Activity> (没错,底层把<receiver>当作<activity>处理)
        // 接下来要做的就是根据这个List<Activity> 获取到Receiver对应的 ActivityInfo (依然是把receiver信息用activity处理了)
        Field receiversField = packageObj.getClass().getDeclaredField("receivers");
        List receivers = (List) receiversField.get(packageObj);

        // 调用generateActivityInfo 方法, 把PackageParser.Activity 转换成
        Class<?> packageParser$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        Class<?> userHandler = Class.forName("android.os.UserHandle");
        Method getCallingUserIdMethod = userHandler.getDeclaredMethod("getCallingUserId");
        int userId = (Integer) getCallingUserIdMethod.invoke(null);
        Object defaultUserState = packageUserStateClass.newInstance();

        Class<?> componentClass = Class.forName("android.content.pm.PackageParser$Component");
        Field intentsField = componentClass.getDeclaredField("intents");

        // 需要调用 android.content.pm.PackageParser#generateActivityInfo(android.content.pm.ActivityInfo, int, android.content.pm.PackageUserState, int)
        Method generateReceiverInfo = packageParserClass.getDeclaredMethod("generateActivityInfo",
                packageParser$ActivityClass, int.class, packageUserStateClass, int.class);

        // 解析出 receiver以及对应的 intentFilter
        for (Object receiver : receivers) {
            ActivityInfo info = (ActivityInfo) generateReceiverInfo.invoke(packageParserObject, receiver, 0, defaultUserState, userId);
            List<? extends IntentFilter> filters = (List<? extends IntentFilter>) intentsField.get(receiver);
            sCache.put(info, filters);
        }

    }

    public static void preLoadReceiver(Context context, File apk) throws Exception {
        parserReceivers(apk, context);

        ClassLoader cl = null;
        for (ActivityInfo activityInfo : ReceiverHelper.sCache.keySet()) {
            Log.i(TAG, "preload receiver:" + activityInfo.name);
            List<? extends IntentFilter> intentFilters = ReceiverHelper.sCache.get(activityInfo);
            if (cl == null) {
                String odexPath = Utils.getPluginOptDexDir(activityInfo.packageName).getPath();
                String libDir = Utils.getPluginLibDir(activityInfo.packageName).getPath();
                cl = new CustomClassLoader(apk.getPath(), odexPath, libDir, ClassLoader.getSystemClassLoader());//CustomClassLoader.getPluginClassLoader(apk, activityInfo.packageName);
            }

            // 把解析出来的每一个静态Receiver都注册为动态的
            for (IntentFilter intentFilter : intentFilters) {
                BroadcastReceiver receiver = (BroadcastReceiver) cl.loadClass(activityInfo.name).newInstance();
                context.registerReceiver(receiver, intentFilter);
            }
        }
    }
}
