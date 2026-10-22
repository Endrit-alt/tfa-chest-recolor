package com.tfachestglow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.LinearGradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class TfaChestGlowOverlay extends Overlay
{
	private final Client client;
	private final TfaChestGlowPlugin plugin;
	private final TfaChestGlowConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	TfaChestGlowOverlay(Client client, TfaChestGlowPlugin plugin, TfaChestGlowConfig config,
		ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN || plugin.getChests().isEmpty())
		{
			return null;
		}
		double pulse = config.pulse() ? 0.78 + 0.22 * Math.sin(client.getGameCycle() * 0.08) : 1;
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.clipRect(client.getViewportXOffset(), client.getViewportYOffset(),
				client.getViewportWidth(), client.getViewportHeight());
			for (NPC chest : plugin.getChests())
			{
				if (chest.getWorldView() != client.getTopLevelWorldView()
					|| chest.getWorldView().getPlane() != client.getPlane()
					|| !plugin.shouldHighlight(chest, config.highlightOpened()))
				{
					continue;
				}
				Color base = TfaChestGlowPlugin.isUniqueChest(chest.getTransformedComposition().getId())
					? config.uniqueGlowColor() : config.glowColor();
				int alpha = (int) (base.getAlpha() * clamp(config.strength(), 0, 100) / 100.0 * pulse);
				if (alpha <= 0)
				{
					continue;
				}
				if (config.pillarHeight() > 0)
				{
					drawTile(g, chest.getLocalLocation(), base, alpha);
				}
				if (config.modelOutline())
				{
					outlines.drawOutline(chest, clamp(config.outlineWidth(), 1, 12), tint(base, alpha), 4);
				}
			}
		}
		finally
		{
			g.dispose();
		}
		return null;
	}

	// Retain the original helper signature so this can replace the running code.
	// Only the ray above the chest is drawn; there is no floor tile highlight.
	private void drawTile(Graphics2D g, LocalPoint location, Color color, int alpha)
	{
		int height = clamp(config.pillarHeight(), 0, 10000);
		if (height == 0)
		{
			return;
		}
		// The old helper parameters remain for HotSwap compatibility; the ray
		// now projects directly with floating-point coordinates instead of tiles.
		drawPillar(g, location, null, null, height, color, alpha);
	}

	private void drawPillar(Graphics2D g, LocalPoint location, LocalPoint[] corners,
		Point[] bottom, int height, Color color, int alpha)
	{
		// The top-level scene uses camera coordinates directly in RuneLite's
		// Perspective utility; its canvas projection may legitimately be null.
		boolean gpu = client.isGpu();
		double pitch = gpu ? client.getCameraFpPitch() : client.getCameraPitch() * Math.PI / 1024.0;
		double yaw = gpu ? client.getCameraFpYaw() : client.getCameraYaw() * Math.PI / 1024.0;
		double cameraX = gpu ? client.getCameraFpX() : client.getCameraX();
		double cameraY = gpu ? client.getCameraFpY() : client.getCameraY();
		double cameraZ = gpu ? client.getCameraFpZ() : client.getCameraZ();
		double pitchSin = Math.sin(pitch);
		double pitchCos = Math.cos(pitch);
		double yawSin = Math.sin(yaw);
		double yawCos = Math.cos(yaw);
		int ground = client.getTopLevelWorldView().getTileHeight(location.getX(), location.getY(), client.getPlane());
		double localX = location.getX() - cameraX;
		double localY = location.getY() - cameraY;
		double localZ = ground - 160 - cameraZ;
		double horizontal = localX * yawCos + localY * yawSin;
		double forward = localY * yawCos - localX * yawSin;
		double vertical = localZ * pitchCos - forward * pitchSin;
		double depth = forward * pitchCos + localZ * pitchSin;
		double[] baseView = {horizontal, vertical, depth};
		double[] upperView = {horizontal, vertical - 128 * pitchCos, depth - 128 * pitchSin};
		double[] peakView = {horizontal, vertical - height * pitchCos, depth - height * pitchSin};
		if (baseView[2] < 50)
		{
			return;
		}
		// A vertical world ray can project almost sideways (or downwards) when
		// viewed from extreme angles. Fade before that degeneracy instead of
		// forcing its endpoint to the screen's top edge and stretching the beam.
		double directionX = upperView[0] * baseView[2] - baseView[0] * upperView[2];
		double directionY = upperView[1] * baseView[2] - baseView[1] * upperView[2];
		double directionLength = Math.hypot(directionX, directionY);
		if (directionLength < 0.000001)
		{
			return;
		}
		double upwardness = -directionY / directionLength;
		double visibility = Math.max(0, Math.min(1, (upwardness - 0.3) / 0.3));
		alpha = (int) (alpha * visibility * visibility * (3 - 2 * visibility));
		if (alpha <= 0)
		{
			return;
		}
		double viewportX = client.getViewportXOffset() + client.getViewportWidth() / 2.0;
		double viewportY = client.getViewportYOffset() + client.getViewportHeight() / 2.0;
		double scale = client.getScale();
		double centerX = viewportX + baseView[0] * scale / baseView[2];
		double centerY = viewportY + baseView[1] * scale / baseView[2];
		double peakX = peakView[2] >= 50 ? viewportX + peakView[0] * scale / peakView[2] : centerX;
		double peakY = peakView[2] >= 50 ? viewportY + peakView[1] * scale / peakView[2] : Double.NEGATIVE_INFINITY;
		double skyY = client.getViewportYOffset() - 64.0;
		if (height >= 2000 || peakView[2] < 50 || peakY < skyY)
		{
			// Derive direction in homogeneous coordinates. Rounded screen samples
			// magnify one-pixel differences into large sideways jumps at the sky.
			double slope = directionX / directionY;
			peakX = centerX + slope * (skyY - centerY);
			peakY = skyY;
		}
		if (Math.hypot(peakX - centerX, peakY - centerY) < 1)
		{
			return;
		}
		// Position the transparent foot above the actual projected chest and its
		// halo. A gradient, rather than a silhouette clip, keeps the end soft.
		for (NPC chest : plugin.getChests())
		{
			if (!location.equals(chest.getLocalLocation()))
			{
				continue;
			}
			Shape hull = chest.getConvexHull();
			if (hull == null || !hull.getBounds2D().intersects(g.getClipBounds()))
			{
				// Do not let the off-screen chest project a detached beam across
				// the viewport while walking away from the reward room.
				return;
			}
			if (peakY < centerY)
			{
				int padding = (config.modelOutline() ? clamp(config.outlineWidth(), 1, 12) : 0) + 4;
				double footY = Math.min(centerY, hull.getBounds2D().getMinY() - padding + 12);
				double fraction = (footY - centerY) / (peakY - centerY);
				fraction = Math.max(0, Math.min(0.9, fraction));
				centerX += (peakX - centerX) * fraction;
				centerY += (peakY - centerY) * fraction;
			}
			break;
		}
		double rayX = peakX - centerX;
		double rayY = peakY - centerY;
		float fadeEnd = (float) (65 / Math.max(1, Math.hypot(rayX, rayY)));
		fadeEnd = Math.max(0.08f, Math.min(0.4f, fadeEnd));
		float[] stops = {0f, fadeEnd, 1f};
		Color transparent = tint(color, 0);
		// A circular world-space radius keeps width steady when rotating the camera.
		double halfWidth = Math.max(8, Math.min(80, Perspective.LOCAL_TILE_SIZE / 4.0 * scale / baseView[2]));
		Point2D center = new Point2D.Double(centerX, centerY);
		Point2D peak = new Point2D.Double(peakX, peakY);
		g.setPaint(new LinearGradientPaint(center, peak,
			stops, new Color[]{transparent, tint(color, alpha / 4), transparent}));
		Path2D beam = new Path2D.Double();
		beam.moveTo(centerX - halfWidth, centerY);
		beam.lineTo(centerX + halfWidth, centerY);
		beam.lineTo(peakX + halfWidth * 2, peakY);
		beam.lineTo(peakX - halfWidth * 2, peakY);
		beam.closePath();
		g.fill(beam);
		// A broad halo and a pale center give the ray a bright core without a floor marker.
		for (int pass = 0; pass < 3; pass++)
		{
			int width = pass == 0 ? 30 : pass == 1 ? 12 : 3;
			Color start = pass == 2
				? new Color((color.getRed() + 255) / 2, (color.getGreen() + 255) / 2,
					(color.getBlue() + 255) / 2, alpha)
				: tint(color, pass == 0 ? alpha / 7 : alpha / 3);
			g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setPaint(new LinearGradientPaint(center, peak,
				stops, new Color[]{transparent, start, transparent}));
			g.draw(new Line2D.Double(center, peak));
		}
	}

	private static Color tint(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(alpha, 0, 255));
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
