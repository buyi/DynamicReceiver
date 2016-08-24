package com.buyi.dynamicreceiver.classloder_hook;

import dalvik.system.DexClassLoader;

/**
 * 自定义的ClassLoader, 用于加载"插件"的资源和代码
 * @author weishu
 * @date 16/3/29
 */
public class CustomClassLoader extends DexClassLoader {

    public CustomClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return super.loadClass(className);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(className, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }
}
