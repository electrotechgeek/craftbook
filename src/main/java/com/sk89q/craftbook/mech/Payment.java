package com.sk89q.craftbook.mech;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import com.sk89q.craftbook.AbstractCraftBookMechanic;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.LocalPlayer;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.craftbook.util.ProtectionUtil;
import com.sk89q.craftbook.util.SignUtil;
import com.sk89q.craftbook.util.events.SignClickEvent;

/**
 * Payment Mech, takes payment. (Requires Vault.)
 *
 * @author Me4502
 */
public class Payment extends AbstractCraftBookMechanic {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRightClick(SignClickEvent event) {

        if(event.getClickedBlock().getType() != Material.WALL_SIGN) return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ChangedSign sign = event.getSign();

        if(!sign.getLine(1).equals("[Pay]")) return;

        LocalPlayer player = CraftBookPlugin.inst().wrapPlayer(event.getPlayer());

        if (!player.hasPermission("craftbook.mech.pay.use")) {
            if(CraftBookPlugin.inst().getConfiguration().showPermissionMessages)
                player.printError("mech.use-permission");
            return;
        }

        if(!ProtectionUtil.canUse(event.getPlayer(), event.getClickedBlock().getLocation(), event.getBlockFace(), event.getAction())) {
            if(CraftBookPlugin.inst().getConfiguration().showPermissionMessages)
                player.printError("area.use-permissions");
            return;
        }

        double money = Double.parseDouble(sign.getLine(2));
        String reciever = sign.getLine(3);

        if (CraftBookPlugin.plugins.getEconomy().withdrawPlayer(event.getPlayer().getName(), money).transactionSuccess())
            if (CraftBookPlugin.plugins.getEconomy().depositPlayer(reciever, money).transactionSuccess()) {
                Block back = SignUtil.getBackBlock(event.getClickedBlock());
                BlockFace bface = SignUtil.getBack(event.getClickedBlock());
                Block redstoneItem = back.getRelative(bface);
                player.print(player.translate("mech.pay.success") + money + " " + CraftBookPlugin.plugins.getEconomy().getName());
                if (ICUtil.setState(redstoneItem, true, back))
                    CraftBookPlugin.inst().getServer().getScheduler().runTaskLater(CraftBookPlugin.inst(), new TurnOff(redstoneItem, back), 20L);
            } else
                CraftBookPlugin.plugins.getEconomy().depositPlayer(event.getPlayer().getName(), money);

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {

        if(!event.getLine(1).equalsIgnoreCase("[pay]")) return;
        LocalPlayer lplayer = CraftBookPlugin.inst().wrapPlayer(event.getPlayer());
        if(!lplayer.hasPermission("craftbook.mech.pay")) {
            if(CraftBookPlugin.inst().getConfiguration().showPermissionMessages)
                lplayer.printError("mech.create-permission");
            SignUtil.cancelSign(event);
            return;
        }

        if(event.getLine(2).isEmpty())
            event.setLine(2, String.valueOf(5));
        if(event.getLine(3).isEmpty())
            event.setLine(3, lplayer.getName());

        event.setLine(1, "[Pay]");
        lplayer.print("mech.pay.create");
    }

    private static class TurnOff implements Runnable {

        final Block block;
        final Block source;

        public TurnOff(Block block, Block source) {

            this.block = block;
            this.source = source;
        }

        @Override
        public void run() {

            ICUtil.setState(block, false, source);
        }
    }
}