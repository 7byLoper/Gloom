package ru.gloom.api.itemstack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
@SuppressWarnings("ALL")
public class SkullUtils {

    private static final Gson GSON = new Gson();
    private static final UUID skullUUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

    @NotNull
    public static String getEncoded(@NotNull final String url) {
        final byte[] encodedData = Base64.getEncoder()
                .encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", "https://textures.minecraft.net/texture/" + url)
                        .getBytes());
        return new String(encodedData);
    }

    @NotNull
    public static ItemStack getSkullByBase64EncodedTextureUrl(@NotNull final String base64Url) {
        final ItemStack head = ItemStackServices.skullHead();
        if (base64Url.isEmpty()) {
            return head;
        }

        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        final PlayerProfile profile = getPlayerProfile(base64Url);
        headMeta.setOwnerProfile(profile);
        head.setItemMeta(headMeta);
        return head;
    }

    public static String getTextureFromSkull(ItemStack item) {
        if (!(item.getItemMeta() instanceof SkullMeta meta)) return null;

        PlayerProfile profile = meta.getOwnerProfile();
        if (profile == null) return null;

        URL url = profile.getTextures().getSkin();
        if (url == null) return null;

        return url.toString().substring("https://textures.minecraft.net/texture/".length() - 1);
    }

    @NotNull
    public static ItemStack getSkullByName(@NotNull final String playerName) {
        final ItemStack head = ItemStackServices.skullHead();
        if (playerName.isEmpty()) {
            return head;
        }

        final SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta == null) {
            return head;
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

        if (offlinePlayer.getPlayerProfile().getTextures().isEmpty()) {
            headMeta.setOwnerProfile(offlinePlayer.getPlayerProfile().update().join());
        } else {
            headMeta.setOwningPlayer(offlinePlayer);
        }

        head.setItemMeta(headMeta);
        return head;
    }

    public static String getSkullOwner(ItemStack skull) {
        if (skull == null || !(skull.getItemMeta() instanceof SkullMeta meta)) return null;

        if (meta.getOwningPlayer() == null) return null;
        return meta.getOwningPlayer().getName();
    }

    @NotNull
    private static PlayerProfile getPlayerProfile(@NotNull final String base64Url) {
        final PlayerProfile profile = Bukkit.createPlayerProfile(skullUUID);

        final String decodedBase64 = decodeSkinUrl(base64Url);
        if (decodedBase64 == null) {
            return profile;
        }

        final PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(new URL(decodedBase64));
        } catch (final MalformedURLException exception) {
            Bukkit.getLogger()
                    .log(Level.SEVERE, "Something went horribly wrong trying to create basehead URL", exception);
        }

        profile.setTextures(textures);
        return profile;
    }

    @Nullable
    public static String decodeSkinUrl(@NotNull final String base64Texture) {
        final String decoded = new String(Base64.getDecoder().decode(base64Texture));
        final JsonObject object = GSON.fromJson(decoded, JsonObject.class);

        final JsonElement textures = object.get("textures");

        if (textures == null) {
            return null;
        }

        final JsonElement skin = textures.getAsJsonObject().get("SKIN");

        if (skin == null) {
            return null;
        }

        final JsonElement url = skin.getAsJsonObject().get("url");
        return url == null ? null : url.getAsString();
    }
}
