package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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
 */
public class SignInputManager implements Listener {

    private final PlayerHeadShop plugin;
    private final Map<UUID, SignSession> activeSessions = new ConcurrentHashMap<>();

    public SignInputManager(PlayerHeadShop plugin) {
        this.plugin = plugin;
    }

    public static class SignSession {
        final Location location;
        final BlockData originalBlockData;
        final Consumer<String> callback;

        public SignSession(Location location, BlockData originalBlockData, Consumer<String> callback) {
            this.location = location;
            this.originalBlockData = originalBlockData;
            this.callback = callback;
        }
    }

    /**
     * 開啟告示牌讓玩家輸入文字
     */
    public void openSignInput(Player player, String[] defaultLines, Consumer<String> callback) {
        if (player == null || !player.isOnline()) return;

        Location playerLoc = player.getLocation();
        Location signLoc = playerLoc.clone();
        signLoc.setY(Math.min(319, Math.max(-60, signLoc.getBlockY())));

        Block block = signLoc.getBlock();
        BlockData original = block.getBlockData();

        // 放置告示牌
        block.setType(Material.OAK_SIGN, false);
        if (block.getState() instanceof Sign sign) {
            if (defaultLines != null) {
                for (int i = 0; i < defaultLines.length && i < 4; i++) {
                    if (defaultLines[i] != null) {
                        sign.getSide(Side.FRONT).setLine(i, defaultLines[i]);
                    }
                }
            }
            sign.update(true, false);

            activeSessions.put(player.getUniqueId(), new SignSession(signLoc, original, callback));

            // 開啟 Sign GUI 給玩家
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.openSign(sign, Side.FRONT);
                }
            }, 1L);
        } else {
            block.setBlockData(original, false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        SignSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            event.setCancelled(true);

            // 讀取所有行的文字並組合成完整字串
            StringBuilder sb = new StringBuilder();
            for (String line : event.getLines()) {
                if (line != null && !line.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(line.trim());
                }
            }
            String input = sb.toString().trim();

            // 還原方塊
            Block block = session.location.getBlock();
            block.setBlockData(session.originalBlockData, false);

            // 呼叫回呼
            Bukkit.getScheduler().runTask(plugin, () -> session.callback.accept(input));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SignSession session = activeSessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.location.getBlock().setBlockData(session.originalBlockData, false);
        }
    }
}
