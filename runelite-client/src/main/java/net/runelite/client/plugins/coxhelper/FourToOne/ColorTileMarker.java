package net.runelite.client.plugins.coxhelper.FourToOne;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.Units;

import java.awt.*;

@Value
public class ColorTileMarker
{
    private WorldPoint worldPoint;
    private Color color;
    private String label;
    private int ticks;
}
