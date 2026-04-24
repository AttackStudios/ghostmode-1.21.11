package net.attackstudioyt.afterlight.screen;

import net.attackstudioyt.afterlight.GhostClientManager;
import net.attackstudioyt.afterlight.item.RevivalBeaconItem;
import net.attackstudioyt.afterlight.network.RevivePlayerPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class RevivalBeaconScreen extends Screen {

    private record GhostEntry(UUID uuid, String name) {}

    private final List<GhostEntry> ghosts = new ArrayList<>();

    private static final int PANEL_W = 220;
    private static final int ROW_H = 22;
    private static final int PADDING = 10;
    private static final int TITLE_H = 28;
    private static final int CANCEL_H = 24;
    private static final int GAP = 6;

    public RevivalBeaconScreen() {
        super(Text.literal("Revival Beacon"));
        ClientPlayNetworkHandler net = MinecraftClient.getInstance().getNetworkHandler();
        for (UUID uuid : GhostClientManager.getAllGhosts()) {
            String name = uuid.toString().substring(0, 8);
            if (net != null) {
                PlayerListEntry entry = net.getPlayerListEntry(uuid);
                if (entry != null) name = entry.getProfile().name();
            }
            ghosts.add(new GhostEntry(uuid, name));
        }
    }

    private int panelH() {
        return TITLE_H + Math.max(1, ghosts.size()) * (ROW_H + GAP) + GAP + CANCEL_H + PADDING;
    }

    @Override
    protected void init() {
        int ph = panelH();
        int px = width / 2 - PANEL_W / 2;
        int py = height / 2 - ph / 2;

        for (int i = 0; i < ghosts.size(); i++) {
            GhostEntry g = ghosts.get(i);
            int y = py + TITLE_H + GAP + i * (ROW_H + GAP);
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("☽  " + g.name()),
                    btn -> revive(g.uuid())
            ).dimensions(px + PADDING, y, PANEL_W - PADDING * 2, ROW_H).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                btn -> close()
        ).dimensions(px + PADDING, py + ph - CANCEL_H - GAP, PANEL_W - PADDING * 2, CANCEL_H).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(ctx, mouseX, mouseY, tickDelta);

        int ph = panelH();
        int px = width / 2 - PANEL_W / 2;
        int py = height / 2 - ph / 2;

        ctx.fill(px, py, px + PANEL_W, py + ph, 0xF0101018);
        ctx.fill(px,               py,          px + PANEL_W, py + 1,  0xFF8888FF);
        ctx.fill(px,               py + ph - 1, px + PANEL_W, py + ph, 0xFF8888FF);
        ctx.fill(px,               py,          px + 1,       py + ph, 0xFF8888FF);
        ctx.fill(px + PANEL_W - 1, py,          px + PANEL_W, py + ph, 0xFF8888FF);
        ctx.fill(px + PADDING, py + TITLE_H - 1, px + PANEL_W - PADDING, py + TITLE_H, 0x558888FF);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        super.render(ctx, mouseX, mouseY, tickDelta);

        int ph = panelH();
        int py = height / 2 - ph / 2;

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Revival Beacon"),
                width / 2, py + 9, 0xFF55FFFF);

        if (ghosts.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No ghosts to revive"),
                    width / 2, py + TITLE_H + 10, 0xFFAAAAAA);
        }
    }

    private void revive(UUID uuid) {
        ClientPlayNetworking.send(new RevivePlayerPayload(uuid));
        close();
    }

    @Override
    public void close() {
        RevivalBeaconItem.activated = false;
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
