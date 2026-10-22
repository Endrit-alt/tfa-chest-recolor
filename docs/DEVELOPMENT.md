# TFA Chest Recolor

Highlights your own reward chest in The Fractured Archive with a soft model outline and a tall, fading ray of light inspired by Visibility Enhancer. No floor tile glow or corner markers are drawn.

Enable **TFA Chest Recolor** in RuneLite's plugin list. Settings include color (with opacity), strength, pulse, ray height, model outline width, and whether to keep highlighting an opened chest. Defaults: strength 60, outline width 6, glow color `#DCAE98DB` (ARGB), and ray height 1000. Ray height supports 0-10000; 2000 or higher extends to the top of the viewport. Zero disables it. Existing saved settings are preserved, including older ray heights (the config key remains `pillarHeight`).

The plugin tracks reward chest NPC spawn/despawn events and checks their current transformed composition for the game's explicit `MINE` form. The central unique chest has its own glow and the purple brazier flames can use a custom color when that reward belongs to you. Use **Unique chest glow color** and **Unique light color** to pick those colors independently; both default to `#F05700FF` (ARGB). Flame recoloring uses the RGB channels; glow opacity also uses the alpha channel.

Unique ownership currently uses `TFA_VAULT_OWN_CHEST = 9`, excluding spectators and requiring a visible unique chest. The cache confirms personal slots 1-8; the unique-slot mapping is provisional and needs in-game confirmation for both your own and someone else's unique reward. Unknown ownership values leave the original visuals unchanged. No network requests, file I/O, or game actions are performed.

Flame recoloring uses private models and RuneLite visual objects, preserving the source animation. Original purple flames are hidden only while their replacements exist. Disabling the feature/plugin or leaving the room removes the replacements. Model IDs 62737 and 62738 come from the type-10/type-22 model entries in `TFA_BRAZIER_GUTHIXLOOT01_FIRE02`; RuneLite does not expose model-ID gameval constants. Third-party renderers' additional light sources (such as 117 HD's scene lighting) are not recolored by this plugin and require a compatibility check in-game.

## Development

Requires Java 11 and RuneLite with the TFA chest gamevals (validated against 1.13.3).

```powershell
.\gradlew.bat build
.\gradlew.bat run
```

For Jagex Accounts, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions to log in to the development client.

## Live development updates

Double-click **start-live.bat** in the project folder to compile the plugin, launch a client with live updates enabled, and start the automatic reload watcher. Keep the watcher window open. It reuses an existing debug listener on port 5005. Close the older client first if it was launched without debugging enabled.

Appearance settings already apply immediately in-game. For Java drawing/logic changes, `run` now enables the JDK debugger on `127.0.0.1:5005`. A client launched before this change needs one restart to enable it.

Leave the client running and apply edits from another terminal:

```powershell
.\gradlew.bat --no-daemon hotSwap
```

This compiles the plugin and replaces its loaded method bodies without logging you out. To rebuild and apply changes automatically after source saves:

```powershell
.\gradlew.bat --no-daemon hotSwap --continuous
```

Stop the continuous build with Ctrl+C in that terminal; the client stays open. Only one debugger may connect at a time. For a different port, pass the same `-PhotSwapPort=5006` to both `run` and `hotSwap`.

The watcher uses `--no-daemon` so it does not reuse a Gradle process started with restricted network access. After replacing classes, the development task asks RuneLite's existing plugin-list callback to rebuild its cached settings descriptors on Swing's event thread. This debugger-only refresh is validated against RuneLite 1.13.3 and is not shipped in the plugin JAR. When the task reports that refresh was queued, return to the plugin list and reopen the plugin settings panel. Saved values are preserved; select ray height 3000 to try the sky effect.

Java 11 HotSwap supports edits inside existing methods. Adding/removing fields or methods, changing signatures or inheritance, and adding classes require a restart. Existing objects, initializers, and saved config values are preserved; changing a config default does not overwrite a saved setting. Compile errors leave the running code unchanged. The debugger/reload tool is entirely in Gradle development tooling and is not included in the published plugin JAR.

To verify in-game after that initial restart, change a numeric drawing value inside an existing overlay method, run `hotSwap`, and confirm the glow changes while you remain logged in. Restore the value and apply it again.

## In-game verification

- Enter the reward room in a group: only your personal chest should glow before and after opening. Other players' personal chests should remain untouched.
- With your unique reward, verify the central chest glow and brazier flames use their selected colors. Change both color pickers, open the chest, then disable the feature/plugin and verify the original flames return.
- With someone else's unique reward, or as a spectator, verify the central chest and purple flames remain unchanged. This is required to validate the provisional unique ownership mapping.
- Disable/re-enable the plugin while already in the room; your chest should be detected immediately.
- Adjust all appearance settings. Zero strength/opacity hides the chest glow and ray; zero ray height removes the ray. Set ray height to 3000 to test the sky effect, rotate/zoom the camera, and confirm no floor marker appears.
- Turn off highlighting opened chests and open yours: the effect should disappear.
- Rotate/zoom the camera and test viewport edges. Leave, log out or hop, and complete another raid: no stale glow should remain and your new chest should be selected.

Automated tests cover ownership filtering, chest slot coverage, and recoloring while preserving shading and neutral details. Appearance, the unique ownership value, NPC transforms, instance transitions, and GPU/117 HD compatibility require user confirmation in-game.

## License

BSD-2-Clause. Glow styling is inspired by Endrit's BSD-2-Clause Visibility Enhancer.
