package net.attackstudioyt.ghostmode;

import net.attackstudioyt.astudiolib.command.StudioCommand;
import net.attackstudioyt.astudiolib.hud.Toast;
import net.attackstudioyt.ghostmode.network.GhostStatePayload;
import net.attackstudioyt.ghostmode.network.GhostVisibilityPayload;
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
        // Ghost state updates from server
        ClientPlayNetworking.registerGlobalReceiver(GhostStatePayload.ID, (payload, context) -> {
            GhostClientManager.setGhost(payload.playerUuid(), payload.isGhost());
            MinecraftClient client = context.client();
            if (client.player != null && client.player.getUuid().equals(payload.playerUuid())) {
                boolean wasGhost = GhostClientManager.isLocalPlayerGhost();
                GhostClientManager.setLocalGhost(payload.isGhost());
                if (!wasGhost && payload.isGhost()) {
                    Toast.show("§7Ghost Mode", "You are now a ghost. Press §fR§7 to respawn.");
                } else if (wasGhost && !payload.isGhost()) {
                    Toast.show("§aRespawned", "You have returned to life.");
                }
            }
        });

        // Visibility toggle updates from server
        ClientPlayNetworking.registerGlobalReceiver(GhostVisibilityPayload.ID, (payload, context) -> {
            GhostClientManager.setVisibility(payload.playerUuid(), payload.visibleToOthers());
            MinecraftClient client = context.client();
            if (client.player != null && client.player.getUuid().equals(payload.playerUuid())) {
                if (payload.visibleToOthers()) {
                    Toast.show("§7Visible", "Other players can see your ghost.");
                } else {
                    Toast.show("§7Hidden", "You are invisible to other players.");
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

        // /studiolib ghostmode toggle — force-exit ghost mode
        StudioCommand.register("ghostmode", "toggle", "Force-exit ghost mode (debug)", ctx -> {
            if (GhostClientManager.isLocalPlayerGhost()) {
                ClientPlayNetworking.send(new RespawnPayload());
                ctx.getSource().sendFeedback(Text.literal("§aGhost mode disabled."));
            } else {
                ctx.getSource().sendFeedback(Text.literal("§7You are not a ghost."));
            }
            return 1;
        });

        // /studiolib ghostmode visible — show visibility status (actual toggle is server-side via /ghostmode visible)
        StudioCommand.register("ghostmode", "visible", "Show your current ghost visibility status", ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return 0;
            if (!GhostClientManager.isLocalPlayerGhost()) {
                ctx.getSource().sendFeedback(Text.literal("§7You are not a ghost."));
                return 0;
            }
            boolean visible = GhostClientManager.isGhostVisible(client.player.getUuid());
            ctx.getSource().sendFeedback(Text.literal(
                "Ghost visibility: " + (visible ? "§aVISIBLE§r (translucent to others)" : "§7HIDDEN§r (invisible to others)") +
                "\n§8Use §f/ghostmode visible§8 to toggle."
            ));
            return 1;
        });

        // HUD: ghost mode text overlay
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!GhostClientManager.isLocalPlayerGhost()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.inGameHud.getDebugHud().shouldShowDebugHud()) return;

            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();
            int centerX = screenW / 2;
            int y = screenH / 2 + 30;

            drawContext.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("§7GHOST MODE"), centerX, y, 0xAAAAAA);
            drawContext.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("§8Press §f[R]§8 to respawn"), centerX, y + 11, 0x888888);
        });
    }
}
