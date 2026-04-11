package net.attackstudioyt.ghostmode.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record GhostStatePayload(UUID playerUuid, boolean isGhost) implements CustomPayload {
    public static final Id<GhostStatePayload> ID = new Id<>(Identifier.of("ghostmode", "ghost_state"));

    public static final PacketCodec<RegistryByteBuf, GhostStatePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, GhostStatePayload::playerUuid,
            PacketCodecs.BOOLEAN, GhostStatePayload::isGhost,
            GhostStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
