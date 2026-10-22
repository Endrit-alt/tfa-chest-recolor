package com.tfachestglow;

import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/** Local visual replacements; original object definitions and shared models are untouched. */
class UniqueChestLights
{
	// Model IDs have no gameval constants. These are the type-10/type-22 models
	// of ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02 in the October 2026 cache.
	private static final int FIRE_MODEL = 62737;
	private static final int GROUND_FIRE_MODEL = 62738;

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private RenderCallbackManager renderCallbacks;
	@Inject private TfaChestGlowPlugin plugin;
	@Inject private TfaChestGlowConfig config;

	private final Set<TileObject> lights = Collections.newSetFromMap(new IdentityHashMap<>());
	private volatile Set<TileObject> hidden = Collections.emptySet();
	private final Map<TileObject, RuneLiteObjectController> replacements = new IdentityHashMap<>();
	private final RenderCallback listener = new RenderCallback()
	{
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			return !hidden.contains(object);
		}
	};
	private Color appliedColor;
	private volatile boolean active;

	void start()
	{
		active = true;
		renderCallbacks.register(listener);
		clientThread.invokeLater(() ->
		{
			if (!active || client.getGameState() != GameState.LOGGED_IN)
			{
				return;
			}
			// Bootstrap once if enabled inside the room. Later updates use spawn events.
			for (Tile[] row : client.getTopLevelWorldView().getScene().getTiles()[client.getPlane()])
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					track(tile.getGroundObject());
					for (GameObject object : tile.getGameObjects())
					{
						track(object);
					}
				}
			}
			update();
		});
	}

	void stop()
	{
		active = false;
		renderCallbacks.unregister(listener);
		clientThread.invoke(() ->
		{
			clearReplacements();
			lights.clear();
		});
	}

	@Subscribe public void onGameObjectSpawned(GameObjectSpawned event) { track(event.getGameObject()); }
	@Subscribe public void onGroundObjectSpawned(GroundObjectSpawned event) { track(event.getGroundObject()); }
	@Subscribe public void onGameObjectDespawned(GameObjectDespawned event) { remove(event.getGameObject()); }
	@Subscribe public void onGroundObjectDespawned(GroundObjectDespawned event) { remove(event.getGroundObject()); }
	@Subscribe public void onGameTick(GameTick event) { update(); }

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TfaChestGlowConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invoke(this::update);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			clearReplacements();
			lights.clear();
		}
	}

	private void track(TileObject object)
	{
		if (object != null && object.getId() == ObjectID.TFA_BRAZIER_GUTHIXLOOT01_FIRE02)
		{
			lights.add(object);
		}
	}

	private Renderable renderable(TileObject object)
	{
		return object instanceof GameObject ? ((GameObject) object).getRenderable()
			: ((GroundObject) object).getRenderable();
	}

	private void remove(TileObject object)
	{
		lights.remove(object);
		RuneLiteObjectController replacement = replacements.remove(object);
		if (replacement != null)
		{
			client.removeRuneLiteObject(replacement);
			publishHidden();
		}
	}

	private void update()
	{
		if (!active || !config.recolorUniqueLights() || !plugin.ownsUniqueChest())
		{
			clearReplacements();
			return;
		}
		Color color = config.uniqueLightColor();
		if (!color.equals(appliedColor))
		{
			clearReplacements();
			appliedColor = color;
		}
		for (TileObject object : lights)
		{
			if (replacements.containsKey(object) || object.getWorldView() != client.getTopLevelWorldView()
				|| object.getPlane() != client.getPlane())
			{
				continue;
			}
			ModelData data = client.loadModelData(object instanceof GroundObject ? GROUND_FIRE_MODEL : FIRE_MODEL);
			Renderable original = renderable(object);
			if (data == null || original == null)
			{
				continue;
			}
			data = data.cloneColors();
			short[] colors = data.getFaceColors();
			short target = JagexColor.rgbToHSL(color.getRGB(), 1.0);
			for (int i = 0; i < colors.length; i++)
			{
				colors[i] = recolor(colors[i], target);
			}
			Model model = data.light(94, 2518, -50, -10, -50);
			RuneLiteObjectController replacement = new RuneLiteObjectController()
			{
				@Override
				public Model getModel()
				{
					if (original instanceof DynamicObject)
					{
						DynamicObject animated = (DynamicObject) original;
						// Advance the original visual's animation, even while it is hidden.
						animated.getModel();
						Animation animation = animated.getAnimation();
						if (animation != null && animated.getAnimFrame() >= 0)
						{
							return client.applyTransformations(model, animation, animated.getAnimFrame(), null, 0);
						}
					}
					return model;
				}
			};
			replacement.setLocation(object.getLocalLocation(), object.getPlane());
			replacement.setZ(object.getZ());
			replacement.setOrientation(object instanceof GameObject ? ((GameObject) object).getOrientation() : 0);
			replacement.setRadius(128);
			client.registerRuneLiteObject(replacement);
			replacements.put(object, replacement);
		}
		publishHidden();
	}

	static short recolor(short original, short target)
	{
		// Preserve black/grey details and the flame's brightness gradient.
		return JagexColor.unpackSaturation(original) == 0 ? original
			: JagexColor.packHSL(JagexColor.unpackHue(target), JagexColor.unpackSaturation(target),
				JagexColor.unpackLuminance(original));
	}

	private void clearReplacements()
	{
		for (RuneLiteObjectController replacement : replacements.values())
		{
			client.removeRuneLiteObject(replacement);
		}
		replacements.clear();
		hidden = Collections.emptySet();
		appliedColor = null;
	}

	private void publishHidden()
	{
		Set<TileObject> snapshot = Collections.newSetFromMap(new IdentityHashMap<>());
		snapshot.addAll(replacements.keySet());
		hidden = Collections.unmodifiableSet(snapshot);
	}
}
