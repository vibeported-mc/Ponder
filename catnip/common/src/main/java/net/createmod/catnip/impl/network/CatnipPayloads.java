package net.createmod.catnip.impl.network;

import net.createmod.catnip.api.Catnip;
import net.createmod.catnip.api.network.registry.CatnipPayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public final class CatnipPayloads {
	private static final CatnipPayloadRegistrar registrar = new CatnipPayloadRegistrar(Catnip.ID);

	// clientbound
	public static final Type<ClientboundConfigPacket> CLIENTBOUND_CONFIG = registrar.clientbound("config/clientbound", ClientboundConfigPacket.STREAM_CODEC);
	public static final Type<ClientboundSimpleActionPacket> SIMPLE_ACTION = registrar.clientbound("simple_action", ClientboundSimpleActionPacket.STREAM_CODEC);

	// serverbound
	// TODO: restore alongside the config UI. ServerboundConfigPacket is the client asking the server
	// to change a config value from the config screen, and it needs ConfigHelper, which cannot live
	// in the common module because Forge Config API Port does not supply FML's ModConfig.

	public static void init() {}
}
