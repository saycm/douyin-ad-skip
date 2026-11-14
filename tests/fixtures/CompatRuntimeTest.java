import java.lang.reflect.*;
import java.util.*;
import org.json.JSONObject;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.inkdye.fuckminiappad.hook.Compat41Entry;

public class CompatRuntimeTest {
    static final String MINI = "com.tt.miniapphost.ad.manager.impl.MiniAppAdManagerServiceImpl";
    static final String GAME = "com.minigame.merge.miniapp.ad.GameAdManagerService";
    static int originals;
    static List<String> states = new ArrayList<>();
    public static class LiteEvent {
        public String type = "show";
        public void onAdStateChanged(String state, JSONObject data) {
            states.add(state);
            if (state.equals("close")) require(Boolean.TRUE.equals(data.get("isEnded")), "missing completed state");
        }
    }
    public static class StandardEvent {
        public String LIZIZ = "show";
        public void LIZ(String state, JSONObject data) { states.add(state); }
    }
    public static class CallbackBase { public void onSuccess() { states.add("success"); } }
    public static class LiteCallback extends CallbackBase {}
    public static class StandardCallbackBase { public void LJ(Object value) { states.add("success"); } }
    public static class StandardCallback extends StandardCallbackBase {}
    public static class MiniManager {
        public void operateVideoAd(LiteEvent event, LiteCallback callback) { originals++; }
        public void operateVideoAd(String unrelated, String callback) { originals++; }
        public void LJIIL(StandardEvent event, StandardCallback callback) { originals++; }
        public void LJII(StandardEvent event, StandardCallback callback) { originals++; }
    }
    public static class GameManager {
        public void operateVideoAd(LiteEvent event, LiteCallback callback) { originals++; }
        public void operateVideoAd(LiteEvent event, LiteCallback callback, Object intercept) { originals++; }
        public void operateVideoAdIntercept(LiteEvent event, LiteCallback callback, Object intercept) { originals++; }
        public void LJJJIL(StandardEvent event, StandardCallback callback, Object intercept) { originals++; }
    }
    public static class Mira {
        static ClassLoader plugin;
        public static ClassLoader getPluginClassLoader(String name) { return plugin; }
    }
    public static class PluginLoader {
        public boolean LJ(String name, android.content.pm.ComponentInfo info) { return true; }
    }
    public static class MappingLoader extends ClassLoader {
        final Map<String, Class<?>> classes = new HashMap<>();
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (classes.containsKey(name)) return classes.get(name);
            return super.loadClass(name);
        }
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Throwable {
        String scenario = args[0];
        boolean lite = scenario.startsWith("lite");
        MappingLoader host = new MappingLoader();
        MappingLoader plugin = new MappingLoader();
        plugin.classes.put(MINI, MiniManager.class); plugin.classes.put(GAME, GameManager.class);
        if (lite) {
            host.classes.put("com.bytedance.mira.Mira", Mira.class);
            host.classes.put("com.bytedance.mira.plugin.PluginLoader", PluginLoader.class);
            if (!scenario.equals("lite-delayed")) Mira.plugin = plugin;
        } else {
            host.classes.put(GAME, GameManager.class);
            if (!scenario.equals("standard-game-only")) host.classes.put(MINI, MiniManager.class);
        }
        XC_LoadPackage.LoadPackageParam param = new XC_LoadPackage.LoadPackageParam();
        param.packageName = lite ? "com.ss.android.ugc.aweme.lite" : "com.ss.android.ugc.aweme";
        param.processName = param.packageName + ":miniapp0"; param.classLoader = host;
        Compat41Entry module = new Compat41Entry(); module.handleLoadPackage(param);
        if (scenario.equals("lite-delayed")) {
            Mira.plugin = plugin;
            Method load = PluginLoader.class.getMethod("LJ", String.class, android.content.pm.ComponentInfo.class);
            XposedBridge.dispatch(load, new PluginLoader(), "m.l.miniapp", null);
            XposedBridge.dispatch(load, new PluginLoader(), "m.l.miniapp", null);
        }
        if (scenario.equals("lite-overload")) {
            Method unrelated = MiniManager.class.getMethod("operateVideoAd", String.class, String.class);
            require(!XposedBridge.hooks.containsKey(unrelated), "unrelated overload hooked");
            return;
        }
        if (lite) {
            LiteEvent event = new LiteEvent();
            if (scenario.equals("lite-load")) event.type = "load";
            boolean game = scenario.equals("lite-game");
            Method show = game ? GameManager.class.getMethod("operateVideoAd", LiteEvent.class, LiteCallback.class, Object.class)
                : MiniManager.class.getMethod("operateVideoAd", LiteEvent.class, LiteCallback.class);
            if (game) XposedBridge.dispatch(show, new GameManager(), event, new LiteCallback(), null);
            else XposedBridge.dispatch(show, new MiniManager(), event, new LiteCallback());
        } else {
            StandardEvent event = new StandardEvent();
            if (scenario.equals("standard-game-only")) {
                Method show = GameManager.class.getMethod("LJJJIL", StandardEvent.class, StandardCallback.class, Object.class);
                XposedBridge.dispatch(show, new GameManager(), event, new StandardCallback(), null);
            } else {
                String name = scenario.equals("standard-411") ? "LJIIL" : "LJII";
                XposedBridge.dispatch(MiniManager.class.getMethod(name, StandardEvent.class, StandardCallback.class),
                    new MiniManager(), event, new StandardCallback());
            }
        }
        android.os.Handler.drain();
        if (scenario.equals("lite-load")) require(originals == 1 && states.isEmpty(), "load must reach original unchanged");
        else {
            require(originals == 0, "show reached original video method");
            require(states.equals(Arrays.asList("show", "success", "close")), "callback sequence: " + states);
        }
    }
}
