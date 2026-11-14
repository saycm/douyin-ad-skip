package de.robv.android.xposed;

public abstract class XC_MethodHook {
    protected XC_MethodHook() {}
    protected XC_MethodHook(int priority) {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Object[] args;
        public java.lang.reflect.Member method;
        public Object thisObject;
        public Object result;
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public Throwable getThrowable() { return null; }
    }

    public static class Unhook {}
}
