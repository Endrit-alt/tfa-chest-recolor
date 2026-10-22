package com.tfachestglow;

import net.runelite.api.JagexColor;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class UniqueChestLightsTest
{
	@Test
	public void recolorPreservesShadingAndNeutralDetails()
	{
		short purple = JagexColor.packHSL(48, 7, 80);
		short green = JagexColor.packHSL(21, 6, 100);
		short result = UniqueChestLights.recolor(purple, green);
		assertEquals(21, JagexColor.unpackHue(result));
		assertEquals(6, JagexColor.unpackSaturation(result));
		assertEquals(80, JagexColor.unpackLuminance(result));
		short grey = JagexColor.packHSL(0, 0, 30);
		assertEquals(grey, UniqueChestLights.recolor(grey, green));
	}
}
