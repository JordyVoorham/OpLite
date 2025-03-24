package net.runelite.client.plugins;

import net.runelite.client.plugins.Cheat;
import net.runelite.client.plugins.coxhelper.CoxPlugin;
import net.runelite.client.plugins.inferno.InfernoPlugin;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "<html><font color=#6b8af6>Cheating</font></html>",
        description = "Ayyyyy"
)
public class CheatingPlugin extends Plugin {


    @Inject
    private InfernoPlugin infernoPlugin;

    @Inject
    private CoxPlugin coxPlugin;

    //Enable plugins
    private Cheat[] plugins = null;

    @Provides
    CheatingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CheatingConfig.class);
    }

    @Override
    protected void startUp() {
        if (plugins == null){
            plugins = new Cheat[]{infernoPlugin, coxPlugin};
        }

        for(Cheat cheat : plugins){
            log.debug("Starting plugin: " + cheat.toString());
            cheat.startUp();
        }
    }

    @Override
    protected void shutDown() {
        for(Cheat cheat : plugins){
            cheat.shutDown();
        }
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (!event.getGroup().equals("Cheating")) {
            return;
        }

        for(Cheat cheat : plugins){
            cheat.onConfigChanged(event);
        }
    }

    @Subscribe
    private void onGameTick(final GameTick event) {
        for(Cheat cheat : plugins){
            cheat.onGameTick(event);
        }
    }

    @Subscribe
    private void onVarbitChanged(final VarbitChanged event){
        for(Cheat cheat : plugins){
            cheat.onVarbitChanged(event);
        }
    }

    @Subscribe
    private void onNpcSpawned(final NpcSpawned event){
        for(Cheat cheat : plugins){
            cheat.onNpcSpawned(event);
        }
    }

    @Subscribe
    private void onNpcDespawned(final NpcDespawned event) {
        for(Cheat cheat : plugins){
            cheat.onNpcDespawned(event);
        }
    }

    @Subscribe
    private void onAnimationChanged(final AnimationChanged event){
        for(Cheat cheat : plugins){
            cheat.onAnimationChanged(event);
        }
    }

    @Subscribe
    private void onGameStateChanged(final GameStateChanged event){
        for(Cheat cheat : plugins){
            cheat.onGameStateChanged(event);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        for(Cheat cheat : plugins){
            cheat.onGraphicsObjectCreated(event);
        }
    }

    @Subscribe
    public void onClientTick(ClientTick clientTick){
        for(Cheat cheat : plugins){
            cheat.onClientTick(clientTick);
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged npcChanged)
    {
        for(Cheat cheat : plugins){
            cheat.onNpcChanged(npcChanged);
        } 
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        for(Cheat cheat : plugins){
            cheat.onMenuEntryAdded(entry);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked option) {
        for(Cheat cheat : plugins){
            cheat.onMenuOptionClicked(option);
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened menu) {
        for(Cheat cheat : plugins){
            cheat.onMenuOpened(menu);
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        for(Cheat cheat : plugins){
            cheat.onGroundObjectSpawned(event);
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event){
        for(Cheat cheat : plugins){
            cheat.onProjectileMoved(event);
        }
    }
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event){
        for(Cheat cheat : plugins){
            cheat.onGameObjectSpawned(event);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event){
        for(Cheat cheat : plugins){
            cheat.onGameObjectDespawned(event);
        }
    }

    @Subscribe
    public void onChatMessage(final ChatMessage event){
        for(Cheat cheat : plugins){
            cheat.onChatMessage(event);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        for(Cheat cheat : plugins){
            cheat.onGraphicChanged(event);
        }
    }

    @Subscribe
    public void onFocusChanged(FocusChanged event) {
        for(Cheat cheat : plugins){
            cheat.onFocusChanged(event);
        }
    }
}