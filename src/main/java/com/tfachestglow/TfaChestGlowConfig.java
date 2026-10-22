package com.tfachestglow;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(TfaChestGlowConfig.GROUP)
public interface TfaChestGlowConfig extends Config
{
	String GROUP = "tfa-chest-glow";

	@Alpha
	@ConfigItem(keyName = "glowColor", name = "Glow color", description = "Color and opacity of your chest highlight", position = 0)
	default Color glowColor()
	{
		return new Color(0xDCAE98DB, true);
	}

	@ConfigItem(keyName = "pulse", name = "Pulse", description = "Gently pulse the glow", position = 1)
	default boolean pulse()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(keyName = "strength", name = "Glow strength", description = "Overall highlight strength, as a percentage", position = 2)
	default int strength()
	{
		return 60;
	}

	// Keep the old method signature for live HotSwap compatibility. The tile
	// setting and all floor drawing are removed; this is no longer a config item.
	default boolean tileGlow()
	{
		return false;
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(keyName = "pillarHeight", name = "Ray height", description = "Height above your chest; 2000 or more extends to the sky, zero disables it", position = 4)
	default int pillarHeight()
	{
		return 1000;
	}

	@ConfigItem(keyName = "modelOutline", name = "Chest outline", description = "Draw a soft outline around your chest model", position = 5)
	default boolean modelOutline()
	{
		return true;
	}

	@Range(min = 1, max = 12)
	@ConfigItem(keyName = "outlineWidth", name = "Outline width", description = "Width of the chest model glow in pixels", position = 6)
	default int outlineWidth()
	{
		return 6;
	}

	@ConfigItem(keyName = "highlightOpened", name = "Highlight opened chest", description = "Keep highlighting your chest after it opens", position = 7)
	default boolean highlightOpened()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightUnique", name = "Highlight my unique chest", description = "Give the unique reward chest a glow only when it belongs to you", position = 8)
	default boolean highlightUnique()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "uniqueGlowColor", name = "Unique chest glow color", description = "Color and opacity of your unique chest glow", position = 9)
	default Color uniqueGlowColor()
	{
		return new Color(0xF05700FF, true);
	}

	@ConfigItem(keyName = "recolorUniqueLights", name = "Recolor my unique lights", description = "Replace the purple reward room brazier flames when the unique chest is yours", position = 10)
	default boolean recolorUniqueLights()
	{
		return true;
	}

	@ConfigItem(keyName = "uniqueLightColor", name = "Unique light color", description = "Color of the reward room flames when the unique chest is yours", position = 11)
	default Color uniqueLightColor()
	{
		return new Color(0xF05700FF, true);
	}
}
