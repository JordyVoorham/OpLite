/*
 * Copyright (c) 2020, dutta64 <https://github.com/dutta64>
 * Copyright (c) 2019, kThisIsCvpv <https://github.com/kThisIsCvpv>
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * Copyright (c) 2019, kyle <https://github.com/xKylee>
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

package net.runelite.client.plugins.gauntlet;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.gauntlet.entity.Demiboss;
import net.runelite.client.plugins.gauntlet.entity.Hunllef;
import net.runelite.client.plugins.gauntlet.entity.Missile;
import net.runelite.client.plugins.gauntlet.entity.Resource;
import net.runelite.client.plugins.gauntlet.entity.Tornado;
import net.runelite.client.plugins.gauntlet.overlay.Overlay;
import net.runelite.client.plugins.gauntlet.overlay.OverlayGauntlet;
import net.runelite.client.plugins.gauntlet.overlay.OverlayHunllef;
import net.runelite.client.plugins.gauntlet.overlay.OverlayPrayerBox;
import net.runelite.client.plugins.gauntlet.overlay.OverlayPrayerWidget;
import net.runelite.client.plugins.gauntlet.resource.ResourceManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.NPCCompositionHeadIcon;

@PluginDescriptor(
	name = "<html><font color=#6b8af6> \uD83D\uDC80 Gauntlet</font></html>",
	enabledByDefault = false,
	description = "All-in-one plugin for the Gauntlet.",
	tags = {"gauntlet"}
)
@Singleton
public class GauntletExtendedPlugin extends Plugin
{
	public static final int ONEHAND_SLASH_AXE_ANIMATION = 395;
	public static final int ONEHAND_CRUSH_PICKAXE_ANIMATION = 400;
	public static final int ONEHAND_CRUSH_AXE_ANIMATION = 401;
	public static final int UNARMED_PUNCH_ANIMATION = 422;
	public static final int UNARMED_KICK_ANIMATION = 423;
	public static final int BOW_ATTACK_ANIMATION = 426;
	public static final int ONEHAND_STAB_HALBERD_ANIMATION = 428;
	public static final int ONEHAND_SLASH_HALBERD_ANIMATION = 440;
	public static final int ONEHAND_SLASH_SWORD_ANIMATION = 390;
	public static final int ONEHAND_STAB_SWORD_ANIMATION = 386;
	public static final int HIGH_LEVEL_MAGIC_ATTACK = 1167;
	public static final int HUNLEFF_TORNADO = 8418;

	private static final Set<Integer> MELEE_ANIM_IDS = Set.of(
		ONEHAND_STAB_SWORD_ANIMATION, ONEHAND_SLASH_SWORD_ANIMATION,
		ONEHAND_SLASH_AXE_ANIMATION, ONEHAND_CRUSH_PICKAXE_ANIMATION,
		ONEHAND_CRUSH_AXE_ANIMATION, UNARMED_PUNCH_ANIMATION,
		UNARMED_KICK_ANIMATION, ONEHAND_STAB_HALBERD_ANIMATION,
		ONEHAND_SLASH_HALBERD_ANIMATION
	);

	private static final Set<Integer> ATTACK_ANIM_IDS = new HashSet<>();

	static
	{
		ATTACK_ANIM_IDS.addAll(MELEE_ANIM_IDS);
		ATTACK_ANIM_IDS.add(BOW_ATTACK_ANIMATION);
		ATTACK_ANIM_IDS.add(HIGH_LEVEL_MAGIC_ATTACK);
	}

	private static final Set<Integer> PROJECTILE_MAGIC_IDS = Set.of(
		ProjectileID.HUNLLEF_MAGE_ATTACK, ProjectileID.HUNLLEF_CORRUPTED_MAGE_ATTACK
	);

	private static final Set<Integer> PROJECTILE_RANGE_IDS = Set.of(
		ProjectileID.HUNLLEF_RANGE_ATTACK, ProjectileID.HUNLLEF_CORRUPTED_RANGE_ATTACK
	);

	private static final Set<Integer> PROJECTILE_PRAYER_IDS = Set.of(
		ProjectileID.HUNLLEF_PRAYER_ATTACK, ProjectileID.HUNLLEF_CORRUPTED_PRAYER_ATTACK
	);

	private static final Set<Integer> PROJECTILE_IDS = new HashSet<>();

	static
	{
		PROJECTILE_IDS.addAll(PROJECTILE_MAGIC_IDS);
		PROJECTILE_IDS.addAll(PROJECTILE_RANGE_IDS);
		PROJECTILE_IDS.addAll(PROJECTILE_PRAYER_IDS);
	}

	private static final Set<Integer> HUNLLEF_IDS = Set.of(
		NpcID.CRYSTALLINE_HUNLLEF, NpcID.CRYSTALLINE_HUNLLEF_9022,
		NpcID.CRYSTALLINE_HUNLLEF_9023, NpcID.CRYSTALLINE_HUNLLEF_9024,
		NpcID.CORRUPTED_HUNLLEF, NpcID.CORRUPTED_HUNLLEF_9036,
		NpcID.CORRUPTED_HUNLLEF_9037, NpcID.CORRUPTED_HUNLLEF_9038
	);

	private static final Set<Integer> TORNADO_IDS = Set.of(NullNpcID.NULL_9025, NullNpcID.NULL_9039);

	private static final Set<Integer> DEMIBOSS_IDS = Set.of(
		NpcID.CRYSTALLINE_BEAR, NpcID.CORRUPTED_BEAR,
		NpcID.CRYSTALLINE_DARK_BEAST, NpcID.CORRUPTED_DARK_BEAST,
		NpcID.CRYSTALLINE_DRAGON, NpcID.CORRUPTED_DRAGON
	);

	private static final Set<Integer> STRONG_NPC_IDS = Set.of(
		NpcID.CRYSTALLINE_SCORPION, NpcID.CORRUPTED_SCORPION,
		NpcID.CRYSTALLINE_UNICORN, NpcID.CORRUPTED_UNICORN,
		NpcID.CRYSTALLINE_WOLF, NpcID.CORRUPTED_WOLF
	);

	private static final Set<Integer> WEAK_NPC_IDS = Set.of(
		NpcID.CRYSTALLINE_BAT, NpcID.CORRUPTED_BAT,
		NpcID.CRYSTALLINE_RAT, NpcID.CORRUPTED_RAT,
		NpcID.CRYSTALLINE_SPIDER, NpcID.CORRUPTED_SPIDER
	);

	private static final Set<Integer> RESOURCE_IDS = new HashSet<>(Arrays.asList(
			ObjectID.CRYSTAL_DEPOSIT, ObjectID.CORRUPT_DEPOSIT,
			ObjectID.PHREN_ROOTS, ObjectID.CORRUPT_PHREN_ROOTS,
			ObjectID.FISHING_SPOT_36068, ObjectID.CORRUPT_FISHING_SPOT,
			ObjectID.GRYM_ROOT, ObjectID.CORRUPT_GRYM_ROOT,
			ObjectID.LINUM_TIRINUM, ObjectID.CORRUPT_LINUM_TIRINUM
	));

	private static final Set<Integer> UTILITY_IDS = Set.of(
		ObjectID.SINGING_BOWL_35966, ObjectID.SINGING_BOWL_36063,
		ObjectID.RANGE_35980, ObjectID.RANGE_36077,
		ObjectID.WATER_PUMP_35981, ObjectID.WATER_PUMP_36078
	);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GauntletExtendedConfig config;

	@Inject
	private ResourceManager resourceManager;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private OverlayGauntlet overlayGauntlet;

	@Inject
	private OverlayHunllef overlayHunllef;

	@Inject
	private OverlayPrayerWidget overlayPrayerWidget;

	@Inject
	private OverlayPrayerBox overlayPrayerBox;

	private Set<Overlay> overlays;

	@Getter
	private final Set<Resource> resources = new HashSet<>();

	@Getter
	private final Set<GameObject> utilities = new HashSet<>();

	@Getter
	private final Set<Tornado> tornadoes = new HashSet<>();

	@Getter
	private final Set<Demiboss> demibosses = new HashSet<>();

	@Getter
	private final Set<NPC> strongNpcs = new HashSet<>();

	@Getter
	private final Set<NPC> weakNpcs = new HashSet<>();

	private final List<Set<?>> entitySets = Arrays.asList(resources, utilities, tornadoes, demibosses, strongNpcs, weakNpcs);

	@Getter
	private Missile missile;

	@Getter
	private Hunllef hunllef;

	@Getter
	@Setter
	private boolean wrongAttackStyle;

	@Getter
	@Setter
	private boolean switchWeapon;

	private boolean inGauntlet;
	private boolean inHunllef;

	@Provides
	GauntletExtendedConfig getConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(GauntletExtendedConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (overlays == null)
		{
			overlays = Set.of(overlayGauntlet, overlayHunllef, overlayPrayerWidget, overlayPrayerBox);
		}

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(this::pluginEnabled);
		}
	}

	@Override
	protected void shutDown()
	{
		overlays.forEach(o -> overlayManager.remove(o));

		inGauntlet = false;
		inHunllef = false;

		hunllef = null;
		missile = null;
		wrongAttackStyle = false;
		switchWeapon = false;

		resourceManager.reset();

		entitySets.forEach(Set::clear);
	}

	@Subscribe
	private void onConfigChanged(final ConfigChanged event)
	{
		if (!event.getGroup().equals("gauntlet"))
		{
			return;
		}

		switch (event.getKey())
		{
			case "resourceIconSize":
				if (!resources.isEmpty())
				{
					resources.forEach(r -> r.setIconSize(config.resourceIconSize()));
				}
				break;
			case "resourceTracker":
				if (inGauntlet && !inHunllef)
				{
					resourceManager.reset();
					resourceManager.init();
				}
				break;
			case "projectileIconSize":
				if (missile != null)
				{
					missile.setIconSize(config.projectileIconSize());
				}
				break;
			case "hunllefAttackStyleIconSize":
				if (hunllef != null)
				{
					hunllef.setIconSize(config.hunllefAttackStyleIconSize());
				}
				break;
			case "mirrorMode":
				overlays.forEach(overlay -> {
					overlay.determineLayer();

					if (overlayManager.anyMatch(o -> o == overlay))
					{
						overlayManager.remove(overlay);
						overlayManager.add(overlay);
					}
				});
				break;
			default:
				break;
		}
	}

	@Subscribe
	private void onVarbitChanged(final VarbitChanged event)
	{
		if (isHunllefVarbitSet(client))
		{
			if (!inHunllef)
			{
				initHunllef();
			}
		}
		else if (isGauntletVarbitSet())
		{
			if (!inGauntlet)
			{
				initGauntlet();
			}
		}
		else
		{
			if (inGauntlet || inHunllef)
			{
				shutDown();
			}
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (hunllef == null)
		{
			return;
		}

		hunllef.decrementTicksUntilNextAttack();

		for(NPC npc : client.getNpcs()){
			if(HUNLLEF_IDS.contains(npc.getId())){
				hunleffAttack(npc);
			}
		}

		if (!tornadoes.isEmpty())
		{
			tornadoes.forEach(Tornado::updateTimeLeft);
		}
	}

	@Subscribe
	private void onGameStateChanged(final GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				resources.clear();
				utilities.clear();
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				shutDown();
				break;
		}
	}

	@Subscribe
	private void onWidgetLoaded(final WidgetLoaded event)
	{
		if (event.getGroupId() == WidgetID.GAUNTLET_TIMER_GROUP_ID)
		{
			resourceManager.init();
		}
	}

	@Subscribe
	private void onGameObjectSpawned(final GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (RESOURCE_IDS.contains(id))
		{
			resources.add(new Resource(gameObject, skillIconManager, config.resourceIconSize()));
		}
		else if (UTILITY_IDS.contains(id))
		{
			utilities.add(gameObject);
		}
	}

	@Subscribe
	private void onGameObjectDespawned(final GameObjectDespawned event)
	{
		final GameObject gameObject = event.getGameObject();

		final int id = gameObject.getId();

		if (RESOURCE_IDS.contains(gameObject.getId()))
		{
			resources.removeIf(o -> o.getGameObject() == gameObject);
		}
		else if (UTILITY_IDS.contains(id))
		{
			utilities.remove(gameObject);
		}
	}

	@Subscribe
	private void onNpcSpawned(final NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (HUNLLEF_IDS.contains(id))
		{
			hunllef = new Hunllef(npc, skillIconManager, config.hunllefAttackStyleIconSize());
		}
		else if (TORNADO_IDS.contains(id))
		{
			tornadoes.add(new Tornado(npc));
		}
		else if (DEMIBOSS_IDS.contains(id))
		{
			demibosses.add(new Demiboss(npc));
		}
		else if (STRONG_NPC_IDS.contains(id))
		{
			strongNpcs.add(npc);
		}
		else if (WEAK_NPC_IDS.contains(id))
		{
			weakNpcs.add(npc);
		}
	}

	@Subscribe
	private void onNpcDespawned(final NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		final int id = npc.getId();

		if (HUNLLEF_IDS.contains(id))
		{
			hunllef = null;
		}
		else if (TORNADO_IDS.contains(id))
		{
			tornadoes.removeIf(t -> t.getNpc() == npc);
		}
		else if (DEMIBOSS_IDS.contains(id))
		{
			demibosses.removeIf(d -> d.getNpc() == npc);
		}
		else if (STRONG_NPC_IDS.contains(id))
		{
			strongNpcs.remove(npc);
		}
		else if (WEAK_NPC_IDS.contains(id))
		{
			weakNpcs.remove(npc);
		}
	}

	@Subscribe
	private void onProjectileSpawned(final ProjectileSpawned event)
	{
		if (hunllef == null)
		{
			return;
		}

		final Projectile projectile = event.getProjectile();

		final int id = projectile.getId();

		if (!PROJECTILE_IDS.contains(id))
		{
			return;
		}

		missile = new Missile(projectile, skillIconManager, config.projectileIconSize());

		hunllef.updateAttackCount();

		if (PROJECTILE_PRAYER_IDS.contains(id) && config.hunllefPrayerAudio())
		{
			client.playSoundEffect(SoundEffectID.MAGIC_SPLASH_BOING);
		}
	}

	@Subscribe
	private void onChatMessage(final ChatMessage event)
	{
		final ChatMessageType type = event.getType();

		if (type == ChatMessageType.SPAM || type == ChatMessageType.GAMEMESSAGE)
		{
			resourceManager.parseChatMessage(event.getMessage());
		}
	}

	@Subscribe
	public void onAnimationChanged(final AnimationChanged event)
	{
		if (!isHunllefVarbitSet(client) || hunllef == null)
		{
			return;
		}

		final Actor actor = event.getActor();

		final int animationId = actor.getAnimation();

		if (actor instanceof Player)
		{
			if (!ATTACK_ANIM_IDS.contains(animationId))
			{
				return;
			}

			final boolean validAttack = isAttackAnimationValid(animationId);

			if (validAttack)
			{
				wrongAttackStyle = false;
				hunllef.updatePlayerAttackCount();

				if (hunllef.getPlayerAttackCount() == 1)
				{
					switchWeapon = true;
				}
			}
			else
			{
				wrongAttackStyle = true;
			}
		}
		else if (actor instanceof NPC)
		{
			if (animationId == HUNLEFF_TORNADO)
			{
				hunllef.updateAttackCount();
			}
		}
	}

	private boolean isAttackAnimationValid(final int animationId)
	{
		HeadIcon headIcon = null;

		NPCComposition hunllefComposition = hunllef.getNpc().getComposition();
		NPCCompositionHeadIcon npcCompositionHeadIcon = new NPCCompositionHeadIcon(client, hunllefComposition);
		headIcon = npcCompositionHeadIcon.getNPCHeadIcon();

		if (headIcon == null)
		{
			return true;
		}

		switch (headIcon)
		{
			case MELEE:
				if (MELEE_ANIM_IDS.contains(animationId))
				{
					return false;
				}
				break;
			case RANGED:
				if (animationId == BOW_ATTACK_ANIMATION)
				{
					return false;
				}
				break;
			case MAGIC:
				if (animationId == HIGH_LEVEL_MAGIC_ATTACK)
				{
					return false;
				}
				break;
		}

		return true;
	}

	private void pluginEnabled()
	{
		if (isGauntletVarbitSet())
		{
			resourceManager.init();
			initGauntlet();
		}

		if (isHunllefVarbitSet(client))
		{
			initHunllef();
		}
	}

	private void initGauntlet()
	{
		inGauntlet = true;

		overlayManager.add(overlayGauntlet);
	}

	private void initHunllef()
	{
		inHunllef = true;

		resourceManager.reset();

		overlayManager.remove(overlayGauntlet);
		overlayManager.add(overlayHunllef);
		overlayManager.add(overlayPrayerWidget);
		overlayManager.add(overlayPrayerBox);
	}

	private boolean isGauntletVarbitSet()
	{
		return client.getVarbitValue(9178) == 1;
	}

	private boolean isHunllefVarbitSet(Client client)
	{
		return client.getVarbitValue(9177) == 1;
	}

	private void hunleffAttack(NPC npc){
		int animation = npc.getAnimation();

		switch(animation) {
			case 8419:
				hunllef.updateAttackCount();
				break;
		}
	}
}
