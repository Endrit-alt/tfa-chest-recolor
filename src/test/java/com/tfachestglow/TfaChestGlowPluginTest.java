package com.tfachestglow;

import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TfaChestGlowPluginTest
{
	@Test
	public void onlyOwnVisibleFormsAreHighlighted()
	{
		assertTrue(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE, false));
		assertTrue(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE, true));
		assertFalse(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE, false));
		assertFalse(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_NOTMINE, true));
		assertFalse(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_NOTMINE, true));
		assertFalse(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_REWARDS_CHEST_VAULT_1_CLOSED, true));
		assertFalse(TfaChestGlowPlugin.isOwnChest(NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED, true));
		assertFalse(TfaChestGlowPlugin.isOwnChest(-1, true));
	}

	@Test
	public void tracksAllSlotsAndUniqueChest()
	{
		int[] slots = {
			NpcID.TFA_REWARDS_CHEST_VAULT_1_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_1_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_2_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_2_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_3_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_3_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_4_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_4_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_5_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_5_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_6_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_6_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_7_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_7_OPEN,
			NpcID.TFA_REWARDS_CHEST_VAULT_8_CLOSED, NpcID.TFA_REWARDS_CHEST_VAULT_8_OPEN
		};
		for (int id : slots)
		{
			assertTrue(TfaChestGlowPlugin.isRewardChest(id));
		}
		assertTrue(TfaChestGlowPlugin.isRewardChest(NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED));
		assertTrue(TfaChestGlowPlugin.isRewardChest(NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN));
	}

	@Test
	public void uniqueOwnershipExcludesPersonalSlotsAndSpectators()
	{
		for (int slot = 0; slot <= 8; slot++)
		{
			assertFalse(TfaChestGlowPlugin.isUniqueOwner(slot, 0));
		}
		assertTrue(TfaChestGlowPlugin.isUniqueOwner(9, 0));
		assertFalse(TfaChestGlowPlugin.isUniqueOwner(9, 1));
		assertFalse(TfaChestGlowPlugin.isUniqueOwner(10, 0));
		assertFalse(TfaChestGlowPlugin.isUniqueOwner(-1, 0));
	}
}
