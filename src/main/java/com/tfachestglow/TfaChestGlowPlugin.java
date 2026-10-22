package com.tfachestglow;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "TFA Chest Recolor",
	internalName = "tfa-chest-recolor",
	description = "Custom chest outlines, fading light rays and unique reward light recoloring in The Fractured Archive",
	tags = {"tfa", "fractured", "archive", "the fractured archive", "fractured archive", "chest", "loot", "rewards",
		"glow", "recolor", "color", "outline", "highlight", "beam", "light"}
)
public class TfaChestGlowPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OverlayManager overlayManager;
	@Inject private TfaChestGlowOverlay overlay;
	@Inject private UniqueChestLights uniqueLights;
	@Inject private EventBus eventBus;
	@Inject private TfaChestGlowConfig config;

	private final Set<NPC> chests = Collections.newSetFromMap(new IdentityHashMap<>());
	private boolean active;

	@Override
	protected void startUp()
	{
		active = true;
		overlayManager.add(overlay);
		eventBus.register(uniqueLights);
		uniqueLights.start();
		clientThread.invokeLater(() ->
		{
			if (active && client.getGameState() == GameState.LOGGED_IN)
			{
				// One initial pass allows enabling the plugin inside the reward room.
				for (NPC npc : client.getTopLevelWorldView().npcs())
				{
					track(npc);
				}
			}
		});
	}

	@Override
	protected void shutDown()
	{
		active = false;
		eventBus.unregister(uniqueLights);
		uniqueLights.stop();
		overlayManager.remove(overlay);
		chests.clear();
	}

	@Provides
	TfaChestGlowConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TfaChestGlowConfig.class);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		chests.remove(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING || state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			chests.clear();
		}
	}

	private void track(NPC npc)
	{
		if (isRewardChest(npc.getId()))
		{
			chests.add(npc);
		}
	}

	Set<NPC> getChests()
	{
		return chests;
	}

	static boolean isRewardChest(int id)
	{
		switch (id)
		{
			case NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED:
			case NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_NOTMINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_NOTMINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE:
			case NpcID.TFA_REWARDS_CHEST_VAULT_1_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_1_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_2_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_2_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_3_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_3_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_4_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_4_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_5_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_5_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_6_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_6_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_7_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_7_OPEN:
			case NpcID.TFA_REWARDS_CHEST_VAULT_8_CLOSED:
			case NpcID.TFA_REWARDS_CHEST_VAULT_8_OPEN:
				return true;
			default:
				return false;
		}
	}

	static boolean isOwnChest(int transformedId, boolean highlightOpened)
	{
		return transformedId == NpcID.TFA_REWARDS_CHEST_VAULT_CLOSED_MINE
			|| (highlightOpened && transformedId == NpcID.TFA_REWARDS_CHEST_VAULT_OPEN_MINE);
	}

	boolean shouldHighlight(NPC npc, boolean highlightOpened)
	{
		// Resolve the game's current visible form rather than guessing from party order,
		// distance, or the base NPC id. A null/invisible form is never highlighted.
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			return false;
		}
		int id = composition.getId();
		if (isUniqueChest(id))
		{
			return config.highlightUnique() && ownsUniqueChest()
				&& (highlightOpened || id == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED);
		}
		return isOwnChest(id, highlightOpened);
	}

	static boolean isUniqueChest(int id)
	{
		return id == NpcID.TFA_UNIQUE_CHEST_VAULT_CLOSED || id == NpcID.TFA_UNIQUE_CHEST_VAULT_OPEN;
	}

	boolean ownsUniqueChest()
	{
		if (client.getGameState() != GameState.LOGGED_IN
			|| !isUniqueOwner(client.getVarbitValue(VarbitID.TFA_VAULT_OWN_CHEST),
				client.getVarbitValue(VarbitID.TFA_CLIENT_SPECTATE_MODE)))
		{
			return false;
		}
		for (NPC chest : chests)
		{
			NPCComposition composition = chest.getTransformedComposition();
			if (composition != null && isUniqueChest(composition.getId()))
			{
				return true;
			}
		}
		return false;
	}

	static boolean isUniqueOwner(int ownChest, int spectateMode)
	{
		// Cache transforms confirm personal slots 1-8. The ninth-slot mapping for
		// uniques is provisional pending in-game validation; reject unknown values.
		return ownChest == 9 && spectateMode == 0;
	}
}
