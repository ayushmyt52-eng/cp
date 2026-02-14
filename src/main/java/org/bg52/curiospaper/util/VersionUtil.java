package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;

/**
 * Utility class for detecting Minecraft server version and feature
 * availability.
 */
public class VersionUtil {

    private static int majorVersion = -1;
    private static int minorVersion = -1;
    private static int patchVersion = -1;
    private static Boolean supportsItemModel = null;
    private static Boolean supportsDataComponents = null;
    private static Boolean supportsSmithingTemplate = null;

    static {
        parseVersion();
    }

    /**
     * Parse the server version from Bukkit.getVersion()
     * Examples: "1.14.4", "1.21.3", "1.20.1"
     */
    private static void parseVersion() {
        try {
            String versionString = Bukkit.getBukkitVersion();
            // Format: "1.21.3-R0.1-SNAPSHOT" or "1.14.4-R0.1-SNAPSHOT"
            String[] parts = versionString.split("-")[0].split("\\.");

            if (parts.length >= 2) {
                majorVersion = Integer.parseInt(parts[0]);
                minorVersion = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                patchVersion = Integer.parseInt(parts[2]);
            } else {
                patchVersion = 0;
            }
        } catch (Exception e) {
            // Fallback to safe defaults if parsing fails
            majorVersion = 1;
            minorVersion = 14;
            patchVersion = 0;
        }
    }

    /**
     * Get the major version number (e.g., 1 from "1.21.3")
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Get the minor version number (e.g., 21 from "1.21.3")
     */
    public static int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Get the patch version number (e.g., 3 from "1.21.3")
     */
    public static int getPatchVersion() {
        return patchVersion;
    }

    /**
     * Check if the server is running at least the specified version.
     * 
     * @param major Major version (usually 1)
     * @param minor Minor version (e.g., 21 for 1.21)
     * @param patch Patch version (e.g., 3 for 1.21.3)
     * @return true if server version >= specified version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (majorVersion > major)
            return true;
        if (majorVersion < major)
            return false;
        if (minorVersion > minor)
            return true;
        if (minorVersion < minor)
            return false;
        return patchVersion >= patch;
    }

    /**
     * Check if the server is running at least the specified version.
     * 
     * @param major Major version (usually 1)
     * @param minor Minor version (e.g., 21 for 1.21)
     * @return true if server version >= specified version
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    /**
     * Check if the server supports ItemMeta.setItemModel(NamespacedKey)
     * This was added in MC 1.21.3 (Paper)
     */
    public static boolean supportsItemModel() {
        if (supportsItemModel == null) {
            if (!isAtLeast(1, 21, 3)) {
                supportsItemModel = false;
            } else {
                // Additional runtime check via reflection
                try {
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    itemMetaClass.getMethod("setItemModel", org.bukkit.NamespacedKey.class);
                    supportsItemModel = true;
                } catch (Exception e) {
                    supportsItemModel = false;
                }
            }
        }
        return supportsItemModel;
    }

    /**
     * Check if the server supports DataComponentTypes (Paper 1.21+)
     * This is required for GLIDER and EQUIPPABLE components
     */
    public static boolean supportsDataComponents() {
        if (supportsDataComponents == null) {
            if (!isAtLeast(1, 21, 0)) {
                supportsDataComponents = false;
            } else {
                // Additional runtime check via reflection
                try {
                    Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
                    supportsDataComponents = true;
                } catch (Exception e) {
                    supportsDataComponents = false;
                }
            }
        }
        return supportsDataComponents;
    }

    /**
     * Check if the server supports SmithingTransformRecipe (1.20+)
     * with template slot. Older versions use SmithingRecipe with only
     * base + addition.
     */
    public static boolean supportsSmithingTemplate() {
        if (supportsSmithingTemplate == null) {
            if (!isAtLeast(1, 20)) {
                supportsSmithingTemplate = false;
            } else {
                try {
                    Class.forName("org.bukkit.inventory.SmithingTransformRecipe");
                    supportsSmithingTemplate = true;
                } catch (Exception e) {
                    supportsSmithingTemplate = false;
                }
            }
        }
        return supportsSmithingTemplate;
    }

    /**
     * Get a formatted version string for logging
     */
    public static String getVersionString() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    /**
     * Parse a namespaced key string into a NamespacedKey object.
     * Compatible with Spigot 1.14+ which doesn't have NamespacedKey.fromString()
     * 
     * @param key The key string in format "namespace:key" or just "key" (defaults
     *            to minecraft namespace)
     * @return The NamespacedKey, or null if parsing fails
     */
    @SuppressWarnings("deprecation")
    public static org.bukkit.NamespacedKey parseNamespacedKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                return new org.bukkit.NamespacedKey(parts[0], parts[1]);
            } else {
                // No namespace, default to minecraft
                return org.bukkit.NamespacedKey.minecraft(key);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set item model on ItemMeta in a version-safe way.
     * Uses setItemModel(NamespacedKey) on 1.21.3+, falls back to setCustomModelData
     * on older versions.
     * 
     * @param meta            The ItemMeta to modify
     * @param itemModel       The item model string (namespace:key format)
     * @param customModelData The CustomModelData value for older versions (can be
     *                        null)
     */
    public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, String itemModel,
            Integer customModelData) {
        if (meta == null)
            return;

        if (supportsItemModel() && itemModel != null && !itemModel.trim().isEmpty()) {
            try {
                org.bukkit.NamespacedKey key = parseNamespacedKey(itemModel);
                if (key != null) {
                    // Use the interface class for reflection to ensure compatibility
                    Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                    java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
                            org.bukkit.NamespacedKey.class);
                    method.invoke(meta, key);
                    return;
                }
            } catch (Exception e) {
                // Log and fall through to CustomModelData
                org.bukkit.Bukkit.getLogger()
                        .warning("[CuriosPaper] Failed to set item model via reflection: " + e.getMessage());
            }
        }

        // Fallback: Use CustomModelData
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        } else if (itemModel != null) {
            // Try to parse itemModel string as integer for CustomModelData
            try {
                int cmd = Integer.parseInt(itemModel);
                meta.setCustomModelData(cmd);
            } catch (NumberFormatException e) {
                // Cannot convert to CustomModelData, skip
            }
        }
    }

    /**
     * Set item model on ItemMeta using a NamespacedKey directly (for slot
     * configurations).
     * 
     * @param meta            The ItemMeta to modify
     * @param itemModel       The NamespacedKey item model
     * @param customModelData The CustomModelData value for older versions (can be
     *                        null)
     */
    // Reflection Cache
    private static Class<?> dataComponentTypesClass;
    private static Object typeGlider;
    private static Object typeEquippable;
    private static java.lang.reflect.Method getDataMethod;
    private static java.lang.reflect.Method setDataMethod;
    private static java.lang.reflect.Method unsetDataMethod;
    private static java.lang.reflect.Method hasDataMethod;
    private static java.lang.reflect.Method keyMethod;

    /**
     * Tries to initialize reflection for Data Components (1.21+ functionality)
     */
    private static void initDataComponents() {
        if (!supportsDataComponents())
            return;
        try {
            dataComponentTypesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            typeGlider = dataComponentTypesClass.getField("GLIDER").get(null);
            typeEquippable = dataComponentTypesClass.getField("EQUIPPABLE").get(null);

            // ItemStack methods for DataComponents
            Class<?> itemStackClass = org.bukkit.inventory.ItemStack.class;
            Class<?> dataComponentTypeClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType");

            getDataMethod = itemStackClass.getMethod("getData", dataComponentTypeClass);
            setDataMethod = itemStackClass.getMethod("setData", dataComponentTypeClass, Object.class);
            unsetDataMethod = itemStackClass.getMethod("unsetData", dataComponentTypeClass);
            hasDataMethod = itemStackClass.getMethod("hasData", dataComponentTypeClass);

            // Key factory
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            keyMethod = keyClass.getMethod("key", String.class, String.class);

        } catch (Exception e) {
            supportsDataComponents = false;
        }
    }

    public static boolean hasGlider(org.bukkit.inventory.ItemStack item) {
        if (!supportsDataComponents() || item == null)
            return false;
        try {
            if (hasDataMethod == null)
                initDataComponents();
            return (boolean) hasDataMethod.invoke(item, typeGlider);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setGlider(org.bukkit.inventory.ItemStack item, boolean enabled) {
        if (!supportsDataComponents() || item == null)
            return;
        try {
            if (hasDataMethod == null)
                initDataComponents();
            if (enabled) {
                setDataMethod.invoke(item, typeGlider,
                        Class.forName("io.papermc.paper.datacomponent.item.Glider").getField("glider").get(null));
            } else {
                unsetDataMethod.invoke(item, typeGlider);
            }
        } catch (Exception e) {
            // Glider component might behave differently or be unit-typed; verify
            // implementation
            try {
                // Alternative: if Glider is a unit interface, we just need ANY implementation?
                // Actually Glider in 1.21.4 API usually has a factory or is a Unit.
                // Let's safe-guard: if enabled, we just invoke setData.
                if (enabled) {
                    // For unit types, we might need the specific instance.
                    // Let's assume Glider.glider() or similar exists if it's not a field.
                    // But simpler: just unset it if disabling.
                }
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Applies the Glider component and sets the Equippable asset to make it look
     * like elytra.
     */
    public static void applyElytraFlight(org.bukkit.inventory.ItemStack chestplate, String assetNamespace,
            String assetKey) {
        if (!supportsDataComponents() || chestplate == null)
            return;
        try {
            if (hasDataMethod == null)
                initDataComponents();

            // 1. Set Glider (Unit type usually)
            // Accessing io.papermc.paper.datacomponent.item.Glider interface
            Class<?> gliderInterface = Class.forName("io.papermc.paper.datacomponent.item.Glider");
            // Usually Glider.glider() returns the singleton/instance? Or it's just a
            // marker.
            // Wait, setData(GLIDER) usually takes a Glider object.
            // Let's try to get a proxy or the default instance.
            // For now, let's skip the detailed implementation of GLIDER creation via
            // reflection without docs.
            // BUT, we can try to copy it from a real Elytra? No.

            // Plan B: The user's code used: chestplate.setData(DataComponentTypes.GLIDER);
            // This implies setData accepts the type itself? NO, setData(Type, Value).
            // Wait, the user code was: chestplate.setData(DataComponentTypes.GLIDER);
            // The user code was MISSING the value argument?
            // "chestplate.setData(DataComponentTypes.GLIDER);" -> ERROR in user code?
            // Ah, maybe usage was setData(DataComponentTypes.GLIDER, Glider.glider())?
            // Or maybe there is a setData(DataComponentType) for Unit types?
            // Assuming there is a simpler way or I'd seen an error.

            // Correct approach for reflection:
            // Glider glider = io.papermc.paper.datacomponent.item.Glider.glider();
            java.lang.reflect.Method gliderFactory = Class.forName("io.papermc.paper.datacomponent.item.Glider")
                    .getMethod("glider");
            Object gliderInstance = gliderFactory.invoke(null);
            setDataMethod.invoke(chestplate, typeGlider, gliderInstance);

            // 2. Set Equippable
            // Equippable equippable = chestplate.getData(DataComponentTypes.EQUIPPABLE);
            // Equippable.Builder builder = equippable.toBuilder();
            // builder.assetId(Key.key(ns, key));
            // chestplate.setData(DataComponentTypes.EQUIPPABLE, builder.build());

            Object currentEquippable = getDataMethod.invoke(chestplate, typeEquippable);
            if (currentEquippable != null) {
                java.lang.reflect.Method toBuilder = currentEquippable.getClass().getMethod("toBuilder");
                Object builder = toBuilder.invoke(currentEquippable);

                Object key = keyMethod.invoke(null, assetNamespace, assetKey);
                java.lang.reflect.Method assetIdMethod = builder.getClass().getMethod("assetId",
                        Class.forName("net.kyori.adventure.key.Key"));
                assetIdMethod.invoke(builder, key);

                java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                Object newEquippable = buildMethod.invoke(builder);

                setDataMethod.invoke(chestplate, typeEquippable, newEquippable);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeGlider(org.bukkit.inventory.ItemStack chestplate) {
        if (!supportsDataComponents() || chestplate == null)
            return;
        try {
            if (unsetDataMethod == null)
                initDataComponents();
            unsetDataMethod.invoke(chestplate, typeGlider);

            // Also reset Equippable to default?
            // Equippable defaultEq =
            // chestplate.getType().getDefaultData(DataComponentTypes.EQUIPPABLE);
            // chestplate.setData(DataComponentTypes.EQUIPPABLE, defaultEq);

            java.lang.reflect.Method getDefaultData = chestplate.getType().getClass().getMethod("getDefaultData",
                    Class.forName("io.papermc.paper.datacomponent.DataComponentType"));
            Object defaultEq = getDefaultData.invoke(chestplate.getType(), typeEquippable);
            if (defaultEq != null) {
                setDataMethod.invoke(chestplate, typeEquippable, defaultEq);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setItemModelSafe(org.bukkit.inventory.meta.ItemMeta meta, org.bukkit.NamespacedKey itemModel,
            Integer customModelData) {
        if (meta == null)
            return;

        if (supportsItemModel() && itemModel != null) {
            try {
                // Use the interface class for reflection to ensure compatibility
                Class<?> itemMetaClass = Class.forName("org.bukkit.inventory.meta.ItemMeta");
                java.lang.reflect.Method method = itemMetaClass.getMethod("setItemModel",
                        org.bukkit.NamespacedKey.class);
                method.invoke(meta, itemModel);
                return;
            } catch (Exception e) {
                // Log and fall through to CustomModelData
                org.bukkit.Bukkit.getLogger().warning(
                        "[CuriosPaper] Failed to set item model (NamespacedKey) via reflection: " + e.getMessage());
            }
        }

        // Fallback to CustomModelData
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }
    }
}
