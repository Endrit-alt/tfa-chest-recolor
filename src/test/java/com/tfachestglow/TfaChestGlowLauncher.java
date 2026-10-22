package com.tfachestglow;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TfaChestGlowLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TfaChestGlowPlugin.class);
		RuneLite.main(args);
	}
}
