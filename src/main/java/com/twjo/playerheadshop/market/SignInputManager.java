package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 處理玩家告示牌 (Sign GUI) 文字輸入對話管理器
 *
 * <p>{@code Player#openSign} 必須指向世界中真實存在的告示牌，因此無法用純封包假方塊實作。為了徹底
 * 排除物品遺失風險，本類別「只」在確認為空氣的座標放置臨時告示牌 — 空氣不具 tile entity，不可能有
 * 內容被摧毀。舊版直接覆寫玩家腳下的方塊，若那格是箱子或潛影盒，裡面的物品會永久消失。</p>
 */
public class SignInputManager implements Listener {

    /** 相對玩家腳下位置的候選偏移量，依序尋找第一個確定為空氣的座標 */
    private static final int[][] CANDIDATE_OFFSETS = {
            {0, 2, 0}, {0, 3, 0}, {1, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {0, 2, -1}, {0, 1, 0}
    };

    private final PlayerHeadShop plugin;
    private final Map<UUID, SignSession> activeSessions = new ConcurrentHashMap<>();

    public SignInputManager(PlayerHeadShop plugin) {
        this.plugin = plugin;
    }

    public static class SignSession {
        final Location location;
        final Consumer<String> callback;

        public SignSession(Location location, Consumer<String> callback) {
            this.location = location;
            this.callback = callback;
        }
    }

    /**
     * 開啟告示牌讓玩家輸入文字。若附近找不到任何空氣格，會直接以空字串回呼，絕不覆寫既有方塊。
     */
    public void openSignInput(Player player, String[] defaultLines, Consumer<String> callback) {
        if (player == null || !player.isOnline() || callback == null) return;

        Location base = player.getLocation();

        // 於方塊所屬的區域執行緒操作（Folia 上跨區域寫方塊會拋異常）
        plugin.getSchedulerAdapter().runForRegion(base, () -> {
            if (!player.isOnline()) return;

            Location signLoc = findAirLocation(base);
            if (signLoc == null) {
                // 找不到安全位置：放棄輸入而非破壞方塊
                plugin.getSchedulerAdapter().runForEntity(player, () -> callback.accept(""));
                return;
            }

            Block block = signLoc.getBlock();
            block.setType(Material.OAK_SIGN, false);

            if (!(block.getState() instanceof Sign sign)) {
                block.setType(Material.AIR, false);
                plugin.getSchedulerAdapter().runForEntity(player, () -> callback.accept(""));
                return;
            }

            if (defaultLines != null) {
                for (int i = 0; i < defaultLines.length && i < 4; i++) {
                    if (defaultLines[i] != null) {
                        sign.getSide(Side.FRONT).line(i, Component.text(defaultLines[i]));
                    }
                }
            }
            sign.setWaxed(false);
            sign.update(true, false);

            activeSessions.put(player.getUniqueId(), new SignSession(signLoc, callback));

            plugin.getSchedulerAdapter().runForEntityLater(player, () -> {
                if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                    clearSign(signLoc);
                    return;
                }
                try {
                    player.openSign(sign, Side.FRONT);
                } catch (Throwable t) {
                    activeSessions.remove(player.getUniqueId());
                    clearSign(signLoc);
                    plugin.getLogger().warning("無法開啟告示牌輸入介面: " + t.getMessage());
                }
            }, 1L);
        });
    }

    /**
     * 尋找玩家附近第一個「確定為空氣」的座標。空氣沒有 tile entity，覆寫不會摧毀任何內容。
     */
    private Location findAirLocation(Location base) {
        for (int[] offset : CANDIDATE_OFFSETS) {
            Location candidate = base.clone().add(offset[0], offset[1], offset[2]);
            int y = candidate.getBlockY();
            if (y < candidate.getWorld().getMinHeight() || y >= candidate.getWorld().getMaxHeight()) {
                continue;
            }
            Material type = candidate.getBlock().getType();
            if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
                return candidate;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        SignSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        event.setCancelled(true);

        // 讀取所有行的文字並組合成完整字串
        StringBuilder sb = new StringBuilder();
        for (Component lineComponent : event.lines()) {
            String line = PlainTextComponentSerializer.plainText().serialize(lineComponent);
            if (!line.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(line.trim());
            }
        }
        String input = sb.toString().trim();

        clearSign(session.location);
        plugin.getSchedulerAdapter().runForEntity(player, () -> session.callback.accept(input));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SignSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            clearSign(session.location);
        }
    }

    /**
     * 移除臨時告示牌並還原為空氣（該座標放置前已確認是空氣）
     */
    private void clearSign(Location location) {
        if (location == null) return;
        plugin.getSchedulerAdapter().runForRegion(location, () -> {
            Block block = location.getBlock();
            if (Material.OAK_SIGN == block.getType()) {
                block.setType(Material.AIR, false);
            }
        });
    }
}
