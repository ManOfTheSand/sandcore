public class SandCorePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Casting system removed – no casting functionality registered.

        // Existing initialization for class and level systems...
        classManager = new ClassManager(this);
    }

    @Override
    public void onDisable() {
        // Any casting cleanup has been removed.
        // Existing shutdown code...
    }
} 