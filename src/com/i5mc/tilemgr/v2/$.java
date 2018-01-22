package com.i5mc.tilemgr.v2;

import com.google.common.collect.ArrayListMultimap;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class $ extends JavaPlugin {

    public void onEnable() {
        PluginHelper.addExecutor(this, "tilemgr", "tilemgr.admin", this::command);
    }

    public void command(CommandSender p, List<String> input) {
        if (input.isEmpty()) {
            p.sendMessage(new String[]{
                    "/tilemgr find <block> [limit]",
                    "/tilemgr next <block>",
                    ""
            });
            return;
        }

        val itr = input.iterator();
        Label.valueOf(itr.next().toUpperCase()).func.exec(p, itr);
    }

    static void next(CommandSender who, Iterator<String> itr) {
        Player p = (Player) who;
        Material material = Material.valueOf(itr.next().toUpperCase());
        ArrayListMultimap<String, BlockState> byLoc = allTileByLoc(p.getWorld(), material);
        val i = L2Pool.find(p.getWorld().getName() + ":loc:" + material.name() + ":itr", () -> {
            List<Pair<String, Integer>> all = new ArrayList<>();
            byLoc.asMap().forEach((k, v) -> all.add(new Pair<>(k, v.size())));
            all.sort((k, v) -> v.getValue() - k.getValue());
            return all.iterator();
        });
        if (!i.hasNext()) {
            p.sendMessage("请稍侯再试");
        } else {
            Location loc = byLoc.get(i.next().getKey()).iterator().next().getLocation();
            p.teleport(loc);
        }
    }

    static ArrayListMultimap<String, BlockState> allTileByLoc(World w, Material material) {
        return L2Pool.find(w.getName() + ":loc:" + material.name(), () -> {
            ArrayListMultimap<String, BlockState> multi = ArrayListMultimap.create();
            allTile(w, material).forEach(b -> multi.put(loc(b.getChunk()), b));
            return multi;
        });
    }

    static List<BlockState> allTile(World w, Material material) {
        return L2Pool.find(w.getName() + ":all:" + material.name(), () -> allTile(w).stream().filter(tile -> tile.getType() == material).collect(Collectors.toList()));
    }

    static String loc(Chunk chunk) {
        return (chunk.getX() << 4) + "," + (chunk.getZ() << 4);
    }

    static List<BlockState> allTile(World w) {
        return L2Pool.find(w.getName() + ":all", () -> {
            List<BlockState> out = new ArrayList<>();
            Arrays.stream(w.getLoadedChunks()).forEach(chunk -> Collections.addAll(out, chunk.getTileEntities()));
            return out;
        });
    }

    static void find(CommandSender p, Iterator<String> itr) {
        Material material = Material.valueOf(itr.next().toUpperCase());
        int limit = itr.hasNext() ? Integer.parseInt(itr.next()) : Integer.MIN_VALUE;
        List<World> all = p instanceof Player ? Collections.singletonList(((Player) p).getWorld()) : Bukkit.getWorlds();
        all.forEach(w -> {
            p.sendMessage("--- " + w.getName());
            ArrayListMultimap<String, BlockState> byLoc = allTileByLoc(w, material);
            List<Pair<String, Integer>> l = new ArrayList<>();
            byLoc.asMap().forEach((k, v) -> l.add(new Pair<>(k, v.size())));
            l.sort((i, r) -> r.getValue() - i.getValue());
            l.stream().filter(line -> line.getValue() > limit).forEach(line -> p.sendMessage("> " + line.getKey() + " " + line.getValue()));
        });
    }

    private static void all(CommandSender p, Iterator<String> input) {
        List<World> all = p instanceof Player ? Collections.singletonList(((Player) p).getWorld()) : Bukkit.getWorlds();
        all.forEach(w -> {
            List<BlockState> allTile = allTile(w);
            Map<String, Integer> map = new HashMap<>();
            allTile.forEach(tile -> {
                String key = tile.getType().name();
                Integer i = map.get(key);
                map.put(key, i == null ? 1 : 1 + i);
            });
            ArrayList<Map.Entry<String, Integer>> l = new ArrayList<>(map.entrySet());
            l.sort((i, r) -> r.getValue() - i.getValue());
            p.sendMessage("--- " + w.getName());
            l.forEach(line -> p.sendMessage("> " + line.getKey() + " " + line.getValue()));
        });
    }

    enum Label {

        ALL($::all),
        FIND($::find),
        NEXT($::next);

        private final IFunc func;

        Label(IFunc func) {
            this.func = func;
        }
    }

    interface IFunc {

        void exec(CommandSender p, Iterator<String> itr);
    }
}
