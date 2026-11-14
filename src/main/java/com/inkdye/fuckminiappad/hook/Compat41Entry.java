package com.inkdye.fuckminiappad.hook;

import android.content.pm.ComponentInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Compat41Entry implements IXposedHookLoadPackage {
    private static final String TAG = "FuckMiniappAD";
    private static final String TARGET_PACKAGE = "com.ss.android.ugc.aweme";
    private static final String LITE_PACKAGE = "com.ss.android.ugc.aweme.lite";
    private static final String MINIAPP_PLUGIN = "m.l.miniapp";
    private static final String TARGET_CLASS =
            "com.tt.miniapphost.ad.manager.impl.MiniAppAdManagerServiceImpl";
    private static final String GAME_TARGET_CLASS =
            "com.minigame.merge.miniapp.ad.GameAdManagerService";
    private static final String REWARD_CLASS = "X.1Unc";
    private static final String GAME_REWARD_CLASS = "X.1UoY";

    private static Handler mainHandler;
    private final Set<Method> hookedMethods = new HashSet<>();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (param == null || (!TARGET_PACKAGE.equals(param.packageName)
                && !LITE_PACKAGE.equals(param.packageName))) {
            return;
        }
        Log.i(TAG, "T6 loaded in " + param.processName);
        if (LITE_PACKAGE.equals(param.packageName)) {
            watchMiniappPlugin(param.classLoader);
        } else {
            hookAvailable(param.classLoader, false);
        }
    }

    private void watchMiniappPlugin(ClassLoader hostLoader) {
        try {
            final Method getLoader = hostLoader.loadClass("com.bytedance.mira.Mira")
                    .getMethod("getPluginClassLoader", String.class);
            Method loadPlugin = hostLoader.loadClass("com.bytedance.mira.plugin.PluginLoader")
                    .getDeclaredMethod("LJ", String.class, ComponentInfo.class);
            synchronized (hookedMethods) {
                if (!hookedMethods.contains(loadPlugin)) {
                    XposedBridge.hookMethod(loadPlugin, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam hookParam) {
                            if (MINIAPP_PLUGIN.equals(hookParam.args[0])
                                    && Boolean.TRUE.equals(hookParam.getResult())) {
                                hookLoadedPlugin(getLoader);
                            }
                        }
                    });
                    hookedMethods.add(loadPlugin);
                    Log.i(TAG, "T6 watching m.l.miniapp plugin load");
                }
            }
            hookLoadedPlugin(getLoader);
        } catch (Throwable throwable) {
            Log.w(TAG, "T6 plugin observer failed", throwable);
        }
    }

    private void hookLoadedPlugin(Method getLoader) {
        try {
            ClassLoader pluginLoader = (ClassLoader) getLoader.invoke(null, MINIAPP_PLUGIN);
            if (pluginLoader != null) {
                hookAvailable(pluginLoader, true);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "T6 plugin lookup failed", throwable);
        }
    }

    private void hookAvailable(ClassLoader loader, boolean lite) {
        if (lite) {
            hookMethods(loader, TARGET_CLASS, 2, "operateVideoAd");
            hookMethods(loader, GAME_TARGET_CLASS, 2, "operateVideoAd");
            hookMethods(loader, GAME_TARGET_CLASS, 3, "operateVideoAd", "operateVideoAdIntercept");
        } else {
            hookMethods(loader, TARGET_CLASS, 2,
                    "LJII", "LJIIIIZZ", "LJIIIZ", "LJIIL", "LJIILL", "LJIILLIIL");
            hookMethods(loader, GAME_TARGET_CLASS, 3, "LJJIJLIJ", "LJJJIL");
            hookMethods(loader, REWARD_CLASS, 2, "LJIIIIZZ");
            hookMethods(loader, GAME_REWARD_CLASS, 3, "LJIIIZ");
        }
    }

    private void hookMethods(ClassLoader loader, String className, int count, String... names) {
        try {
            for (Method method : loader.loadClass(className).getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!Arrays.asList(names).contains(method.getName()) || parameters.length != count
                        || method.getReturnType() != Void.TYPE
                        || findTypeField(parameters[0]) == null || findStateMethod(parameters[0]) == null
                        || findSuccess(parameters[1]) == null) {
                    continue;
                }
                hookEntry(method);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            Log.w(TAG, "T6 entry lookup failed for " + className, throwable);
        }
    }

    private void hookEntry(Method entryPoint) {
        synchronized (hookedMethods) {
            if (hookedMethods.contains(entryPoint)) {
                return;
            }
            try {
                XposedBridge.hookMethod(entryPoint, new XC_MethodReplacement(10000) {
                @Override
                protected Object replaceHookedMethod(MethodHookParam hookParam) throws Throwable {
                    if (hookParam.args.length < 2 || hookParam.args[0] == null || hookParam.args[1] == null) {
                        return XposedBridge.invokeOriginalMethod(
                                hookParam.method, hookParam.thisObject, hookParam.args);
                    }
                    Object event = hookParam.args[0];
                    Method success = findSuccess(hookParam.args[1].getClass());
                    if (!"show".equals(readType(event)) || success == null) {
                        return XposedBridge.invokeOriginalMethod(
                                hookParam.method, hookParam.thisObject, hookParam.args);
                    }

                    JSONObject shown = new JSONObject();
                    shown.put("mode", "ad");
                    if (!emitState(event, "show", shown)) {
                        return XposedBridge.invokeOriginalMethod(
                                hookParam.method, hookParam.thisObject, hookParam.args);
                    }
                    try {
                        success.setAccessible(true);
                        if (success.getParameterCount() == 0) {
                            success.invoke(hookParam.args[1]);
                        } else {
                            success.invoke(hookParam.args[1], new Object[]{null});
                        }
                        Log.i(TAG, "T6 success callback completed");
                    } catch (Throwable throwable) {
                        Log.w(TAG, "T6 success callback failed", throwable);
                        return null;
                    }

                    final JSONObject closed = new JSONObject();
                    closed.put("mode", "ad");
                    closed.put("count", 1);
                    closed.put("isEnded", true);
                    getMainHandler().postDelayed(() -> emitState(event, "close", closed), 100L);
                    Log.i(TAG, "T6 skipped " + hookParam.method.getName()
                            + " show and returned reward state");
                    return null;
                }
            });
                hookedMethods.add(entryPoint);
                Log.i(TAG, "T6 hooked "
                        + entryPoint.getDeclaringClass().getName() + "."
                        + entryPoint.getName() + "/" + entryPoint.getParameterCount());
            } catch (Throwable throwable) {
                Log.w(TAG, "T6 hook failed for " + entryPoint, throwable);
            }
        }
    }

    private static Method findSuccess(Class<?> type) {
        String[] names = {"onSuccess", "LJ", "LJ", "LJFF"};
        Class<?>[][] signatures = {new Class<?>[0], {Object.class}, new Class<?>[0], new Class<?>[0]};
        for (int index = 0; index < names.length; index++) {
            try {
                Method method = type.getMethod(names[index], signatures[index]);
                if (method.getReturnType() == Void.TYPE) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static Field findTypeField(Class<?> type) {
        while (type != null) {
            for (String name : new String[]{"type", "LIZIZ"}) {
                try {
                    Field field = type.getDeclaredField(name);
                    if (field.getType() == String.class) {
                        return field;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Method findStateMethod(Class<?> type) {
        for (Method method : type.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getReturnType() == Void.TYPE && parameters.length == 2
                    && parameters[0] == String.class && parameters[1] == JSONObject.class) {
                return method;
            }
        }
        return null;
    }

    private static synchronized Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    private static String readType(Object event) {
        try {
            Field field = findTypeField(event.getClass());
            if (field != null) {
                field.setAccessible(true);
                return (String) field.get(event);
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "T6 event type lookup failed", throwable);
        }
        return null;
    }

    private static boolean emitState(Object event, String state, JSONObject data) {
        try {
            Method method = findStateMethod(event.getClass());
            if (method != null) {
                method.setAccessible(true);
                method.invoke(event, state, data);
                Log.i(TAG, "T6 emitted state=" + state);
                return true;
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "T6 state callback failed for " + state, throwable);
        }
        return false;
    }
}
