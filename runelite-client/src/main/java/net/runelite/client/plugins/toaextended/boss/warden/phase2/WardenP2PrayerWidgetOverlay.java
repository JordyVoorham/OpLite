/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, rdutta <https://github.com/rdutta>
 * Copyright (c) 2022, LlemonDuck
 * Copyright (c) 2022, TheStonedTurtle
 * Copyright (c) 2019, Ron Young <https://github.com/raiyni>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.toaextended.boss.warden.phase2;

import net.runelite.client.plugins.toaextended.ToaExtendedConfig;
import net.runelite.client.plugins.toaextended.boss.AttackProjectile;
import net.runelite.client.plugins.toaextended.boss.PrayerWidgetOverlay;
import net.runelite.client.plugins.toaextended.module.PluginLifecycleComponent;
import net.runelite.client.plugins.toaextended.util.RaidState;
import java.util.Queue;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
public class WardenP2PrayerWidgetOverlay extends PrayerWidgetOverlay implements PluginLifecycleComponent
{

	private final OverlayManager overlayManager;
	private final WardenP2 wardenP2;

	@Inject
	public WardenP2PrayerWidgetOverlay(final Client client, final ToaExtendedConfig config, final OverlayManager overlayManager, final WardenP2 wardenP2)
	{
		super(client, config);
		this.overlayManager = overlayManager;
		this.wardenP2 = wardenP2;
	}

	@Override
	public boolean isEnabled(final ToaExtendedConfig config, final RaidState raidState)
	{
		return wardenP2.isEnabled(config, raidState);
	}

	@Override
	public void startUp()
	{
		overlayManager.add(this);
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(this);
	}

	@Override
	protected Queue<AttackProjectile> getAttackProjectileQueue()
	{
		return wardenP2.getAttackProjectiles();
	}

	@Override
	protected @Nullable Prayer getNextPrayer()
	{
		final Queue<AttackProjectile> queue = wardenP2.getAttackProjectiles();
		return queue.isEmpty() ? null : queue.peek().getPrayer();
	}

	@Override
	protected long getLastTickTime()
	{
		return wardenP2.getLastTickTime();
	}

	@Override
	protected boolean isPrayerWidgetEnabled()
	{
		return config.wardenP2PrayerIndicator();
	}

}
