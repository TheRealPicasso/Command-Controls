package com.vincentporath.commandcontrol;

import com.vincentporath.commandcontrol.config.CommandControlConfig;
import com.vincentporath.commandcontrol.network.CommandSyncHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command Control - A Fabric mod for controlling command visibility and access
 * 
 * Features:
 * - Block commands per rank (integrates with LuckPerms)
 * - Hide command suggestions from tab-complete
 * - Sync allowed commands to client for client-side filtering
 * - Configurable via JSON config file
 */
public class CommandControl implements ModInitializer {
    
    public static final String MOD_ID = "commandcontrol";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("==========================================");
        LOGGER.info("Command Control v1.0.0 is initializing...");
        LOGGER.info("Control command visibility and access");
        LOGGER.info("==========================================");
        
        // Load configuration
        CommandControlConfig.initialize();
        
        // Register events
        registerEvents();
        
        LOGGER.info("Command Control initialization complete!");
    }
    
    private void registerEvents() {
        // When a player joins, send them the allowed commands list
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Small delay to ensure client is ready
            server.execute(() -> {
                try {
                    var player = handler.getPlayer();
                    var allowedCommands = CommandControlConfig.getAllowedCommandsForPlayer(player);
                    
                    // Check if client can receive our packets
                    if (ServerPlayNetworking.canSend(player, CommandSyncHandler.SYNC_CHANNEL)) {
                        // Send the allowed commands to the client
                        ServerPlayNetworking.send(player, CommandSyncHandler.SYNC_CHANNEL, 
                                CommandSyncHandler.createSyncPacket(allowedCommands));
                        
                        LOGGER.debug("[CommandControl] Sent {} allowed commands to {}", 
                                allowedCommands.size(), player.getName().getString());
                    } else {
                        LOGGER.debug("[CommandControl] Client {} doesn't have CommandControl installed, skipping sync",
                                player.getName().getString());
                    }
                } catch (Exception e) {
                    LOGGER.warn("[CommandControl] Failed to send command sync to player", e);
                }
            });
        });
        
        LOGGER.info("[CommandControl] Events registered");
    }
}
