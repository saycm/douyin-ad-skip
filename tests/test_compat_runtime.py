from pathlib import Path
import shutil
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build" / "compat-runtime-tests"
SOURCES = {
    "android/os/Process.java": "package android.os; public class Process { public static int myPid() { return 1; } }",
    "android/os/Looper.java": "package android.os; public class Looper { public static Looper getMainLooper() { return new Looper(); } }",
    "android/os/Handler.java": """package android.os;
public class Handler {
    public static final java.util.List<Runnable> pending = new java.util.ArrayList<>();
    public Handler(Looper looper) {}
    public boolean postDelayed(Runnable task, long delay) { pending.add(task); return true; }
    public static void drain() { for (Runnable task : new java.util.ArrayList<>(pending)) task.run(); pending.clear(); }
}""",
    "android/content/pm/ComponentInfo.java": "package android.content.pm; public class ComponentInfo {}",
    "android/util/Log.java": """package android.util; public class Log {
    public static int i(String tag, String message) { return 0; }
    public static int w(String tag, String message) { return 0; }
    public static int w(String tag, String message, Throwable error) { return 0; }
}""",
    "org/json/JSONObject.java": """package org.json;
public class JSONObject extends java.util.HashMap<String, Object> {
    public JSONObject put(String key, Object value) { super.put(key, value); return this; }
}""",
    "de/robv/android/xposed/XC_MethodHook.java": """package de.robv.android.xposed;
public abstract class XC_MethodHook {
    protected XC_MethodHook() {} protected XC_MethodHook(int priority) {}
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    public static class MethodHookParam {
        public Object[] args; public java.lang.reflect.Member method; public Object thisObject; public Object result;
        public Object getResult() { return result; } public void setResult(Object value) { result = value; }
        public Throwable getThrowable() { return null; }
    }
    public static class Unhook {}
}""",
    "de/robv/android/xposed/XposedBridge.java": """package de.robv.android.xposed;
import java.lang.reflect.*; import java.util.*;
public class XposedBridge {
    public static final Map<Member, XC_MethodHook> hooks = new HashMap<>();
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook hook) {
        if (hooks.put(method, hook) != null) throw new AssertionError("duplicate hook: " + method);
        return null;
    }
    public static void log(String message) {} public static void log(Throwable error) {}
    public static Object invokeOriginalMethod(Member method, Object target, Object[] args) throws Throwable {
        return ((Method) method).invoke(target, args);
    }
    public static Object dispatch(Method method, Object target, Object... args) throws Throwable {
        XC_MethodHook hook = hooks.get(method);
        if (hook == null) return invokeOriginalMethod(method, target, args);
        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.method = method; param.thisObject = target; param.args = args;
        if (hook instanceof XC_MethodReplacement) return ((XC_MethodReplacement) hook).replaceHookedMethod(param);
        hook.beforeHookedMethod(param);
        param.result = invokeOriginalMethod(method, target, args);
        hook.afterHookedMethod(param);
        return param.result;
    }
}""",
}

class CompatRuntimeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source_dir = BUILD / "src"
        classes = BUILD / "classes"
        classes.mkdir(parents=True, exist_ok=True)
        sources = []
        for relative, content in SOURCES.items():
            target = source_dir / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding="utf-8")
            sources.append(str(target))
        stub_root = ROOT / "stubs/de/robv/android/xposed"
        sources += [str(stub_root / name) for name in (
            "IXposedHookLoadPackage.java", "XC_MethodReplacement.java", "callbacks/XC_LoadPackage.java")]
        sources += [str(path) for path in (ROOT / "src/main/java").rglob("*.java")]
        sources.append(str(ROOT / "tests/fixtures/CompatRuntimeTest.java"))
        subprocess.run([shutil.which("javac"), "-encoding", "UTF-8", "-d", str(classes), *sources], check=True)

    def test_callback_and_loading_scenarios(self):
        for scenario in ("lite-cached", "lite-delayed", "lite-game", "lite-load", "lite-overload",
                         "standard-411", "standard-412", "standard-game-only"):
            with self.subTest(scenario=scenario):
                result = subprocess.run([shutil.which("java"), "-cp", str(BUILD / "classes"),
                                         "CompatRuntimeTest", scenario], capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)

if __name__ == "__main__":
    unittest.main()
