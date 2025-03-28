package net.runelite.client.plugins.coxhelper;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.CheatingConfig;
import net.runelite.client.plugins.CheatingPlugin;
import net.runelite.client.plugins.Util.AttackStyle;
import net.runelite.client.plugins.coxhelper.FourToOne.ColorTileMarker;
import net.runelite.client.plugins.coxhelper.FourToOne.FourToOneConstant;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
@Getter
@Setter
public class Olm
{
    public static final int HEAD_GAMEOBJECT_RISING = 29880;
    public static final int HEAD_GAMEOBJECT_READY = 29881;
    public static final int LEFT_HAND_GAMEOBJECT_RISING = 29883;
    public static final int LEFT_HAND_GAMEOBJECT_READY = 29884;
    public static final int RIGHT_HAND_GAMEOBJECT_RISING = 29886;
    public static final int RIGHT_HAND_GAMEOBJECT_READY = 29887;

    private final Client client;
    private final CheatingPlugin plugin;
    private final CheatingConfig config;

    private final List<WorldPoint> healPools = new ArrayList<>();
    private final List<WorldPoint> portals = new ArrayList<>();
    private final Set<Victim> victims = new HashSet<>();
    private int portalTicks = 10;

    private boolean active = false; // in fight
    private boolean firstPhase = false;
    private boolean finalPhase = false;
    private PhaseType phaseType = PhaseType.UNKNOWN;

    private GameObject hand = null;
    private OlmAnimation handAnimation = OlmAnimation.UNKNOWN;
    private GameObject head = null;
    private OlmAnimation headAnimation = OlmAnimation.UNKNOWN;

    private int ticksUntilNextAttack = -1;
    private int attackCycle = 1;
    private int specialCycle = 1;

    private boolean crippled = false;
    private int crippleTicks = 45;

    private Prayer prayer = null;
    private AttackStyle attackStyle = null;
    private long lastPrayTime = 0;

    private boolean isEast = true;

    @Inject
    private FourToOneConstant fourToOne;

    @Inject
    private Olm(final Client client, final CheatingPlugin plugin, final CheatingConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    public void startPhase()
    {
        firstPhase = !active;
        active = true;
        ticksUntilNextAttack = -1;
        attackCycle = 1;
        specialCycle = 1;
        crippled = false;
        crippleTicks = 45;
        prayer = null;
        attackStyle = null;
        isEast = true;
        lastPrayTime = 0;
        headAnimation = OlmAnimation.UNKNOWN;
        handAnimation = OlmAnimation.UNKNOWN;
    }

    public void hardRest()
    {
        active = false;
        firstPhase = false;
        finalPhase = false;
        phaseType = PhaseType.UNKNOWN;
        hand = null;
        head = null;
        headAnimation = OlmAnimation.UNKNOWN;
        handAnimation = OlmAnimation.UNKNOWN;
        isEast = true;
        ticksUntilNextAttack = -1;
        attackCycle = 1;
        specialCycle = 1;
        healPools.clear();
        portals.clear();
        portalTicks = 10;
        victims.clear();
        crippled = false;
        crippleTicks = 45;
        prayer = null;
        attackStyle = null;
        lastPrayTime = 0;
    }

    void setPrayer(Prayer pray)
    {
        prayer = pray;
        lastPrayTime = System.currentTimeMillis();
    }

    void cripple()
    {
        crippled = true;
        crippleTicks = 45;
    }

    void uncripple()
    {
        crippled = false;
        crippleTicks = 45;
    }

    public void update()
    {
        updateVictims();
        updateCrippleSticks();
        updateSpecials();
        incrementTickCycle();
        headAnimations();
        handAnimations();
    }

    public void incrementTickCycle()
    {
        if (ticksUntilNextAttack == 1)
        {
            ticksUntilNextAttack = 4;
            incrementAttackCycle();
        }
        else if (ticksUntilNextAttack != -1)
        {
            ticksUntilNextAttack--;
        }
    }

    public void incrementAttackCycle()
    {
        if (attackCycle == 4)
        {
            attackCycle = 1;
            incrementSpecialCycle();
        }
        else
        {
            attackCycle++;
        }
    }

    public void incrementSpecialCycle()
    {
        if ((specialCycle == 3 && !finalPhase) || specialCycle == 4)
        {
            specialCycle = 1;
        }
        else
        {
            specialCycle++;
        }
    }

    public void specialSync(OlmAnimation currentAnimation)
    {
        ticksUntilNextAttack = 4;
        attackCycle = 1;
        switch (currentAnimation)
        {
            case LEFT_HAND_CRYSTALS1:
            case LEFT_HAND_CRYSTALS2:
                specialCycle = 2;
                break;
            case LEFT_HAND_LIGHTNING1:
            case LEFT_HAND_LIGHTNING2:
                specialCycle = 3;
                break;
            case LEFT_HAND_PORTALS1:
            case LEFT_HAND_PORTALS2:
                specialCycle = finalPhase ? 4 : 1;
                break;
            case LEFT_HAND_HEAL1:
            case LEFT_HAND_HEAL2:
                specialCycle = 1;
                break;
        }
    }

    void updateCrippleSticks()
    {
        if (!crippled)
        {
            return;
        }

        crippleTicks--;
        if (crippleTicks <= 0)
        {
            crippled = false;
            crippleTicks = 45;
        }
    }

    void updateVictims()
    {
        if (victims.size() > 0)
        {
            victims.forEach(Victim::updateTicks);
            victims.removeIf(victim -> victim.getTicks() <= 0);
        }
    }

    void updateSpecials()
    {
        healPools.clear();
        portals.clear();
        client.clearHintArrow();

        for (GraphicsObject o : client.getGraphicsObjects())
        {
            if (o.getId() == CoxPlugin.OLM_TELEPORT)
            {
                portals.add(WorldPoint.fromLocal(client, o.getLocation()));
            }
            if (o.getId() == CoxPlugin.OLM_HEAL)
            {
                healPools.add(WorldPoint.fromLocal(client, o.getLocation()));
            }
            if (!portals.isEmpty())
            {
                portalTicks--;
                if (portalTicks <= 0)
                {
                    client.clearHintArrow();
                    portalTicks = 10;
                }
            }
        }
    }

    private void headAnimations()
    {
        if (head == null || head.getRenderable() == null)
        {
            return;
        }

        isEast = head.getWorldLocation().getRegionX() > 35;
        OlmAnimation currentAnimation = OlmAnimation.fromId(((DynamicObject) head.getRenderable()).getAnimation().getId());

        if (currentAnimation == headAnimation)
        {
            return;
        }

        switch (currentAnimation)
        {
            case HEAD_RISING_2:
            case HEAD_ENRAGED_RISING_2:
                ticksUntilNextAttack = firstPhase ? 6 : 8;
                attackCycle = 1;
                specialCycle = 1;
                break;
            case HEAD_ENRAGED_LEFT:
            case HEAD_ENRAGED_MIDDLE:
            case HEAD_ENRAGED_RIGHT:
                finalPhase = true;
                break;
        }

        headAnimation = currentAnimation;
    }

    private void handAnimations()
    {
        if (hand == null || hand.getRenderable() == null)
        {
            return;
        }

        OlmAnimation currentAnimation = OlmAnimation.fromId(((DynamicObject) hand.getRenderable()).getAnimation().getId());

        if (currentAnimation == handAnimation)
        {
            return;
        }

        switch (currentAnimation)
        {
            case LEFT_HAND_CRYSTALS1:
            case LEFT_HAND_CRYSTALS2:
            case LEFT_HAND_LIGHTNING1:
            case LEFT_HAND_LIGHTNING2:
            case LEFT_HAND_PORTALS1:
            case LEFT_HAND_PORTALS2:
            case LEFT_HAND_HEAL1:
            case LEFT_HAND_HEAL2:
                specialSync(currentAnimation);
                break;
            case LEFT_HAND_CRIPPLING:
                cripple();
                break;
            case LEFT_HAND_UNCRIPPLING1:
            case LEFT_HAND_UNCRIPPLING2:
                uncripple();
                break;
        }

        handAnimation = currentAnimation;
    }

    protected Collection<ColorTileMarker> getPoints(){
        return translateToColorTileMarker(fourToOne.getTiles(attackCycle, ticksUntilNextAttack, isEast));
    }

    private Collection<ColorTileMarker> translateToColorTileMarker(Collection<ColorTileMarker> points)
    {
        if (points.isEmpty())
        {
            return Collections.emptyList();
        }

        return points.stream()
                .map(point -> new ColorTileMarker(
                        WorldPoint.fromRegion(point.getWorldPoint().getRegionID(), point.getWorldPoint().getRegionX(), point.getWorldPoint().getRegionY(), point.getWorldPoint().getPlane()),
                        point.getColor(), point.getLabel(), point.getTicks()))
                .flatMap(colorTile ->
                {
                    final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, colorTile.getWorldPoint());
                    return localWorldPoints.stream().map(wp -> new ColorTileMarker(wp, colorTile.getColor(), colorTile.getLabel(), colorTile.getTicks()));
                })
                .collect(Collectors.toList());
    }

    public enum PhaseType
    {
        FLAME,
        ACID,
        CRYSTAL,
        UNKNOWN,
    }
}
