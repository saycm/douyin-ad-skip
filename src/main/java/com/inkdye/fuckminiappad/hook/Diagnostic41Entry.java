package com.inkdye.fuckminiappad.hook;

import android.os.Process;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Diagnostic41Entry implements IXposedHookLoadPackage {
    private static final String TAG = "FuckMiniappAD-Diag";
    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        if (param == null || !TARGET_PACKAGE.equals(param.packageName)) {
            return;
        }

        Log.i(TAG, "loaded process=" + param.processName + " pid=" + Process.myPid());
        XposedBridge.log(TAG + " loaded process=" + param.processName);

        hookNames(param.classLoader,
                "com.tt.miniapphost.ad.api.AdApiServiceImpl",
                new String[]{"LJII", "LJIILLIIL", "LJIIZILJ"});
        hookNames(param.classLoader,
                "com.tt.miniapphost.ad.manager.impl.MiniAppAdManagerServiceImpl",
                new String[]{"LJII", "LJIILLIIL", "LJIIZILJ"});
        hookNames(param.classLoader, "X.16aU", new String[]{"LJII", "LJIIZILJ"});
        hookNames(param.classLoader,
                "com.bytedance.bdp.cpapi.impl.handler.ad.OperateVideoAdApiHandler",
                new String[]{"LJJIJL"});
        hookNames(param.classLoader,
                "com.minigame.merge.miniapp.ad.AdApiServiceImpl",
                new String[]{"LJI", "LJJIJ", "LJJIJL", "LJJJJLL", "LJJJJZ", "LJJJJZI"});
        hookNames(param.classLoader,
                "com.minigame.merge.miniapp.ad.GameAdManagerService",
                new String[]{"LJI", "LJJIFFI", "LJJIJ", "LJJIJL", "LJJIZ", "LJJJ", "LJJJI"});
        hookNames(param.classLoader,
                "X.16aD",
                new String[]{"LJI", "LJJIFFI", "LJJIJ", "LJJIJL", "LJJIZ", "LJJJ", "LJJJI"});
        hookNames(param.classLoader,
                "com.ss.android.excitingvideo.ExcitingVideoAd",
                new String[]{"requestExcitingVideo", "createRewardVideoAdFragment"});
        hookNames(param.classLoader,
                "com.ss.android.excitingvideo.interstitial.InterstitialAdFactory",
                new String[]{"create"});
        hookNames(param.classLoader,
                "com.ss.android.excitingvideo.sdk.ExcitingVideoFragment",
                new String[]{"onCreate", "onStart", "onResume"});
    }

    private static void hookNames(ClassLoader loader, String className, String[] names) {
        try {
            Class<?> type = loader.loadClass(className);
            int hooked = 0;
            for (Method method : type.getDeclaredMethods()) {
                if (!contains(names, method.getName())) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, new XC_MethodHook(10000) {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam hookParam) {
                            Log.i(TAG, "ENTER " + hookParam.method + "");
                            if (hookParam.args != null && hookParam.args.length > 0) {
                                Object first = hookParam.args[0];
                                Log.i(TAG, "  event=" + describe(first)
                                        + " type=" + readType(first)
                                        + " caller=" + caller());
                            } else {
                                Log.i(TAG, "  caller=" + caller());
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam hookParam) {
                            if (hookParam.getThrowable() != null) {
                                Log.w(TAG, "THROW " + hookParam.method,
                                        hookParam.getThrowable());
                            } else {
                                Log.i(TAG, "EXIT " + hookParam.method
                                        + " result=" + describe(hookParam.result));
                            }
                        }
                    });
                    hooked++;
                    Log.i(TAG, "hooked " + method);
                } catch (Throwable throwable) {
                    Log.w(TAG, "hook failed " + method, throwable);
                }
            }
            Log.i(TAG, "class " + className + " matches=" + hooked);
        } catch (Throwable throwable) {
            Log.w(TAG, "class not available " + className, throwable);
        }
    }

    private static boolean contains(String[] values, String value) {
        for (String candidate : values) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return value.getClass().getName() + "@"
                    + Integer.toHexString(System.identityHashCode(value));
        } catch (Throwable throwable) {
            return "<unprintable>";
        }
    }

    private static String readType(Object value) {
        if (value == null) {
            return "n/a";
        }
        Class<?> type = value.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("LIZIZ");
                field.setAccessible(true);
                Object result = field.get(value);
                return result == null ? "null" : result.toString();
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable throwable) {
                return "<error>";
            }
        }
        return "n/a";
    }

    private static String caller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (StackTraceElement item : trace) {
            String name = item.getClassName();
            if (name.equals(Diagnostic41Entry.class.getName())
                    || name.startsWith("de.robv.android.xposed.")) {
                continue;
            }
            if (count > 0) {
                result.append(" <- ");
            }
            result.append(name).append('.').append(item.getMethodName());
            if (++count == 6) {
                break;
            }
        }
        return result.toString();
    }
}
