package com.idlefish.flutterboost;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;

/**
 * flutter 资源文件热更新
 */
@Keep
public class MonkeyPatcher {
    public static void monkeyPatchExistingResources(
            Context context, String externalResourceFile, Collection<Activity> activities) {

        if (externalResourceFile == null) {
            return;
        }

        try {
            // 反射创建新的AssetManager
            AssetManager newAssetManager = AssetManager.class.getConstructor(
                    new Class[0]).newInstance(new Object[0]);
            Method mAddAssetPath = AssetManager.class.getDeclaredMethod(
                    "addAssetPath", new Class[]{String.class});
            mAddAssetPath.setAccessible(true);
            // 反射调用addAssetPath方法加载外部资源
            if (((Integer) mAddAssetPath.invoke(
                    newAssetManager, new Object[]{externalResourceFile})).intValue() == 0) {
                throw new IllegalStateException("Could not create new AssetManager");
            }
            Method mEnsureStringBlocks = AssetManager.class.getDeclaredMethod(
                    "ensureStringBlocks", new Class[0]);
            mEnsureStringBlocks.setAccessible(true);
            mEnsureStringBlocks.invoke(newAssetManager, new Object[0]);
            if (activities != null) {
                for (Activity activity : activities) {
                    Resources resources = activity.getResources();
                    try {
                        // 把Resources中的mAssets替换为newAssetManager
                        Field mAssets = Resources.class.getDeclaredField("mAssets");
                        mAssets.setAccessible(true);
                        mAssets.set(resources, newAssetManager);
                    } catch (Throwable ignore) {
                        /* ... */
                    }
                    // 获取Activity的主题
                    Resources.Theme theme = activity.getTheme();
                    try {
                        try {
                            // 把Resources.Theme中的mAssets替换为newAssetManager
                            Field ma = Resources.Theme.class.getDeclaredField("mAssets");
                            ma.setAccessible(true);
                            ma.set(theme, newAssetManager);
                        } catch (NoSuchFieldException ignore) {
                            /* ... */
                        }
                        /* ... */
                    } catch (Throwable e) {
                        /* ... */
                    }
                }
                Collection<WeakReference<Resources>> references = null;
                /* ...根据不同SDK版本，用不同方式得到Resources的弱引用集合 */
                for (WeakReference<Resources> wr : references) {
                    Resources resources = wr.get();
                    if (resources != null) {
                        try {
                            // 把每个Resources中的mAssets替换为newAssetManager
                            Field mAssets = Resources.class.getDeclaredField("mAssets");
                            mAssets.setAccessible(true);
                            mAssets.set(resources, newAssetManager);
                        } catch (Throwable ignore) {
                            /* ... */
                        }
                        resources.updateConfiguration(
                                resources.getConfiguration(), resources.getDisplayMetrics());
                    }
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }


    public static void patchResource(FlutterEngine flutterEngine, String patchApkPath) {

        if (patchApkPath == null) return;

        if (!new File(patchApkPath).exists()) return;

        try {
            DartExecutor dartExecutor = flutterEngine.getDartExecutor();
//            AssetManager assetManager = activity.getAssets();
            AssetManager newAssetManager = AssetManager.class.getConstructor(
                    new Class[0]).newInstance(new Object[0]);
            Log.d("FlutterPatch","加载 flutter 资源文件-------->");
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(newAssetManager, patchApkPath);
            Field assetsManagerField = DartExecutor.class.getDeclaredField("assetManager");
            assetsManagerField.setAccessible(true);
            assetsManagerField.set(dartExecutor, newAssetManager);
        } catch (Exception e) {
            Log.e("FlutterPatch","e:"+e);
        }

    }
}
