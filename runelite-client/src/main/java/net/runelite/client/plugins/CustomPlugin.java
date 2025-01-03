package net.runelite.client.plugins;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "<html><font color=#6b8af6> \uD83D\uDC80 Gaming</font></html>"
)
public class CustomPlugin extends Plugin {

//    @Inject
//    private PlayerAttackPlugin playerAttackPlugin;
//
//    @Inject
//    private GauntletPlugin gauntletPlugin;
//    @Inject
//    private TheatrePlugin theatrePlugin;
//
//    @Inject
//    private HydraPlugin hydraPlugin;
//
//    @Inject
//    private GodWarsPlugin godWarsPlugin;
//
//    @Inject
//    private CoxPlugin coxPlugin;
//
//    @Inject
//    private ZulrahPlugin zulrahPlugin;
//
//    @Inject
//    private InfernoPlugin infernoPlugin;
//
//    @Inject
//    private CerberusPlugin cerberusPlugin;
//
//    @Inject
//    private ToaPlugin toaPlugin;


    //Enable plugins
    private Custom[] plugins = null;

    @Provides
    CustomConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CustomConfig.class);
    }

    @Override
    protected void startUp() {
        if (plugins == null) {
            plugins = new Custom[]{};

        }

        for (Custom custom : plugins) {
            log.debug("Starting plugin: " + custom.toString());
            custom.startUp();
        }
    }

    @Override
    protected void shutDown() {
        for (Custom custom : plugins) {
            custom.shutDown();
        }
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (!event.getGroup().equals("Custom")) {
            return;
        }

        for (Custom custom : plugins) {
            custom.onConfigChanged(event);
        }
    }

    @Subscribe
    private void onGameTick(final GameTick event) {
        for (Custom custom : plugins) {
            custom.onGameTick(event);
        }
    }

    @Subscribe
    private void onVarbitChanged(final VarbitChanged event) {
        for (Custom custom : plugins) {
            custom.onVarbitChanged(event);
        }
    }

    @Subscribe
    private void onNpcSpawned(final NpcSpawned event) {
        for (Custom custom : plugins) {
            custom.onNpcSpawned(event);
        }
    }

    @Subscribe
    private void onNpcDespawned(final NpcDespawned event) {
        for (Custom custom : plugins) {
            custom.onNpcDespawned(event);
        }
    }

    @Subscribe
    private void onAnimationChanged(final AnimationChanged event) {
        for (Custom custom : plugins) {
            custom.onAnimationChanged(event);
        }
    }

    @Subscribe
    private void onGameStateChanged(final GameStateChanged event) {
        for (Custom custom : plugins) {
            custom.onGameStateChanged(event);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        for (Custom custom : plugins) {
            custom.onGraphicsObjectCreated(event);
        }
    }

    @Subscribe
    public void onClientTick(ClientTick clientTick) {
        for (Custom custom : plugins) {
            custom.onClientTick(clientTick);
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged npcChanged) {
        for (Custom custom : plugins) {
            custom.onNpcChanged(npcChanged);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded entry) {
        for (Custom custom : plugins) {
            custom.onMenuEntryAdded(entry);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked option) {
        for (Custom custom : plugins) {
            custom.onMenuOptionClicked(option);
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened menu) {
        for (Custom custom : plugins) {
            custom.onMenuOpened(menu);
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        for (Custom custom : plugins) {
            custom.onGroundObjectSpawned(event);
        }
    }

    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        for (Custom custom : plugins) {
            custom.onProjectileMoved(event);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        for (Custom custom : plugins) {
            custom.onGameObjectSpawned(event);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        for (Custom custom : plugins) {
            custom.onGameObjectDespawned(event);
        }
    }

    @Subscribe
    public void onChatMessage(final ChatMessage event) {
        for (Custom custom : plugins) {
            custom.onChatMessage(event);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        for (Custom custom : plugins) {
            custom.onGraphicChanged(event);
        }
    }

    @Subscribe
    public void onFocusChanged(FocusChanged event) {
        for (Custom custom : plugins) {
            custom.onFocusChanged(event);
        }
    }
}