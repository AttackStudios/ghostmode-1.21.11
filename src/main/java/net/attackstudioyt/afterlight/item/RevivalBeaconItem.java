package net.attackstudioyt.afterlight.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;

public class RevivalBeaconItem extends Item {

    /** Client-side flag: true while the beacon screen is open. Drives the model predicate. */
    @Environment(EnvType.CLIENT)
    public static boolean activated = false;

    public RevivalBeaconItem(Settings settings) {
        super(settings);
    }
}
