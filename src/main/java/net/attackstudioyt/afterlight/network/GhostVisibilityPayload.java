package net.attackstudioyt.afterlight.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** Tells all clients whether a ghost player is visible (translucent) or fully hidden. */
public record GhostVisibilityPayload(UUID playerUuid, boolean visibleToOthers) implements CustomPayload {
    public static final Id<GhostVisibilityPayload> ID = new Id<>(Identifier.of("afterlight", "ghost_visibility"));

    public static final PacketCodec<RegistryByteBuf, GhostVisibilityPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, GhostVisibilityPayload::playerUuid,
            PacketCodecs.BOOLEAN, GhostVisibilityPayload::visibleToOthers,
            GhostVisibilityPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
