package net.attackstudioyt.ghostmode.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RespawnPayload() implements CustomPayload {
    public static final Id<RespawnPayload> ID = new Id<>(Identifier.of("ghostmode", "respawn_request"));

    public static final PacketCodec<RegistryByteBuf, RespawnPayload> CODEC =
            PacketCodec.unit(new RespawnPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
