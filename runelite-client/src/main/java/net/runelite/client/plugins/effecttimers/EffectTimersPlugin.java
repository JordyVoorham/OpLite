/*
 * Copyright (c) 2020 ThatGamerBlue
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.effecttimers;

import com.google.inject.Provides;

import java.util.EnumSet;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
        name = "<html><font color=#6b8af6> \uD83D\uDC80 Effect Timers</font></html>",
        description = "Effect timer overlay on players",
        tags = {"freeze", "timers", "barrage", "teleblock", "pklite"}
)
public class EffectTimersPlugin extends Plugin {
    private static final int VORKATH_REGION = 9023;

    @Inject
    @Getter
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PrayerTracker prayerTracker;

    @Inject
    private TimerManager timerManager;

    @Inject
    private EffectTimersOverlay overlay;

    @Inject
    private EffectTimersConfig config;

    @Inject
    private KeyManager keyManager;

    private int fakeSpotAnim = -1;
    private HotkeyListener hotkeyListener = new HotkeyListener(() -> config.debugKeybind()) {
        public void hotkeyPressed() {
            fakeSpotAnim = config.debugInteger();
        }
    };

    public EffectTimersConfig getConfig() {
        return config;
    }

    @Provides
    public EffectTimersConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(EffectTimersConfig.class);
    }

    @Override
    public void startUp() {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    public void shutDown() {
        keyManager.unregisterKeyListener(hotkeyListener);
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        prayerTracker.gameTick();

        if (fakeSpotAnim != -1) {
            GraphicChanged event = new GraphicChanged();
            event.setActor(client.getLocalPlayer());
            onGraphicChanged(event);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        Actor actor = event.getActor();
        int spotAnim = fakeSpotAnim == -1 ? actor.getGraphic() : fakeSpotAnim;
        fakeSpotAnim = -1;
        if (spotAnim == prayerTracker.getSpotanimLastTick(event.getActor())) {
            return;
        }

        PlayerEffect effect = PlayerEffect.getFromSpotAnim(spotAnim);

        if (effect == null) {
            return;
        }

        if (timerManager.hasTimerActive(actor, effect.getType())) {
            return;
        }

        timerManager.setTimerFor(actor, effect.getType(), new Timer(this, effect, effect.isHalvable() && prayerTracker.getPrayerIconLastTick(actor) == HeadIcons.MAGIC));
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE
                || !event.getMessage().contains("Your Tele Block has been removed")) {
            return;
        }

        timerManager.jumpToCooldown(client.getLocalPlayer(), TimerType.TELEBLOCK);
    }

    private boolean isAtVorkath() {
        return ArrayUtils.contains(client.getMapRegions(), VORKATH_REGION);
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        if (!isAtVorkath()) {
            return;
        }

        final NPC npc = event.getNpc();

        if (npc.getName() == null) {
            return;
        }

        if (npc.getName().contains("Zombified Spawn")) {
            // TODO: not sure if we're meant to jump to cooldown here or just remove the timer completely, doesn't mechanically make a difference though
            timerManager.setTimerFor(client.getLocalPlayer(), TimerType.FREEZE, new Timer(this, null)); // empty timer
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        for (TimerType type : TimerType.values()) {
            timerManager.setTimerFor(event.getActor(), type, new Timer(this, null));
        }
    }
}
