package net.attackstudioyt.ghostmode.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/** C2S — client asks server to revive a specific ghost player. */
public record RevivePlayerPayload(UUID targetUuid) implements CustomPayload {
    public static final Id<RevivePlayerPayload> ID = new Id<>(Identifier.of("ghostmode", "revive_player"));

    public static final PacketCodec<RegistryByteBuf, RevivePlayerPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, RevivePlayerPayload::targetUuid,
            RevivePlayerPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
