package com.loohp.globaltrackedmaps.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class EntityUtils {
    public static boolean isWearingMobHead(LivingEntity entity) {
        ItemStack helmet = entity.getEquipment().getHelmet();
        return (helmet != null && helmet.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta);
    }
}
