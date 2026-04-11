package net.attackstudioyt.ghostmode;

import net.attackstudioyt.astudiolib.command.StudioCommand;
import net.attackstudioyt.astudiolib.hud.HudRenderer;
import net.attackstudioyt.astudiolib.hud.Toast;
import net.attackstudioyt.ghostmode.network.GhostStatePayload;
import net.attackstudioyt.ghostmode.network.RespawnPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class GhostClient implements ClientModInitializer {

    private static KeyBinding respawnKey;

    @Override
    public void onInitializeClient() {
        // Network: receive ghost state updates from server
        ClientPlayNetworking.registerGlobalReceiver(GhostStatePayload.ID, (payload, context) -> {
            GhostClientManager.setGhost(payload.playerUuid(), payload.isGhost());
            MinecraftClient client = context.client();
            if (client.player != null && client.player.getUuid().equals(payload.playerUuid())) {
                boolean wasGhost = GhostClientManager.isLocalPlayerGhost();
                GhostClientManager.setLocalGhost(payload.isGhost());
                // Toast on state change
                if (!wasGhost && payload.isGhost()) {
                    Toast.show("§7Ghost Mode", "You are now a ghost. Press §fR§7 to respawn.");
                } else if (wasGhost && !payload.isGhost()) {
                    Toast.show("§aRespawned", "You have returned to life.");
                }
            }
        });

        // Clear ghost state on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                GhostClientManager.clear());

        // Respawn keybind: R
        respawnKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ghostmode.respawn",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                Category.MISC
        ));

        // Send respawn request when key pressed in ghost state
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (respawnKey.wasPressed() && GhostClientManager.isLocalPlayerGhost()) {
                ClientPlayNetworking.send(new RespawnPayload());
            }
        });

        // /studiolib ghostmode toggle — debug escape hatch
        StudioCommand.register("ghostmode", "toggle", "Force-exit ghost mode (debug)", ctx -> {
            if (GhostClientManager.isLocalPlayerGhost()) {
                ClientPlayNetworking.send(new RespawnPayload());
                ctx.getSource().sendFeedback(Text.literal("§aGhost mode disabled."));
            } else {
                ctx.getSource().sendFeedback(Text.literal("§7You are not a ghost."));
            }
            return 1;
        });

        // HUD: ghost mode overlay
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!GhostClientManager.isLocalPlayerGhost()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.inGameHud.getDebugHud().shouldShowDebugHud()) return;

            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();

            String line1 = "GHOST MODE";
            String line2 = "Press [R] to respawn";
            int tw1 = client.textRenderer.getWidth(line1);
            int tw2 = client.textRenderer.getWidth(line2);
            int panelW = Math.max(tw1, tw2) + 20;
            int panelH = 30;
            int px = screenW / 2 - panelW / 2;
            int py = screenH / 2 + 24;
            long now = System.currentTimeMillis();

            HudRenderer.drawPanel(drawContext, px, py, panelW, panelH, 180);
            HudRenderer.drawBorders(drawContext, px, py, panelW, panelH, 0x8888FF, 180, now);
            HudRenderer.drawTopGlow(drawContext, px, py, panelW, 0x8888FF, 180);

            drawContext.drawText(client.textRenderer, Text.literal("§7§o" + line1),
                    px + panelW / 2 - tw1 / 2, py + 7, 0xAAAAAA, true);
            drawContext.drawText(client.textRenderer, Text.literal("§8" + line2),
                    px + panelW / 2 - tw2 / 2, py + 18, 0x888888, true);
        });
    }
}
