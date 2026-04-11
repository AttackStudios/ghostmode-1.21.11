package net.attackstudioyt.ghostmode;

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
        // Register network payload receivers (S2C registered server-side)
        ClientPlayNetworking.registerGlobalReceiver(GhostStatePayload.ID, (payload, context) -> {
            GhostClientManager.setGhost(payload.playerUuid(), payload.isGhost());
            MinecraftClient client = context.client();
            if (client.player != null && client.player.getUuid().equals(payload.playerUuid())) {
                GhostClientManager.setLocalGhost(payload.isGhost());
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

        // HUD: show ghost mode message
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!GhostClientManager.isLocalPlayerGhost()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.inGameHud.getDebugHud().shouldShowDebugHud()) return;

            String line1 = "§7§oGHOST MODE";
            String line2 = "§8Press §f[R]§8 to respawn";

            int screenW = client.getWindow().getScaledWidth();
            int screenH = client.getWindow().getScaledHeight();
            int x1 = screenW / 2 - client.textRenderer.getWidth(line1) / 2;
            int x2 = screenW / 2 - client.textRenderer.getWidth(line2) / 2;
            int y = screenH / 2 + 30;

            drawContext.drawText(client.textRenderer, Text.literal(line1), x1, y, 0xAAAAAA, true);
            drawContext.drawText(client.textRenderer, Text.literal(line2), x2, y + 12, 0x888888, true);
        });
    }
}
