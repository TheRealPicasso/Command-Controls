package com.vincentporath.commandcontrol.client;

import com.vincentporath.commandcontrol.network.CommandSyncHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side initialization for Command Control
 * Receives allowed commands from server and filters client-side suggestions
 */
@Environment(EnvType.CLIENT)
public class CommandControlClient implements ClientModInitializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("commandcontrol-client");
    
    // Set of commands the player is allowed to use (synced from server)
    private static Set<String> allowedCommands = new HashSet<>();
    
    // Whether we've received sync from a CommandControl-enabled server
    private static boolean syncReceived = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("[CommandControl] Client initializing...");
        
        // Register to receive command sync from server
        ClientPlayNetworking.registerGlobalReceiver(CommandSyncHandler.SYNC_CHANNEL, (client, handler, buf, responseSender) -> {
            // Read the sync data on network thread
            Set<String> commands = CommandSyncHandler.readSyncPacket(buf);
            
            // Update state on client thread
            client.execute(() -> {
                allowedCommands = commands;
                syncReceived = true;
                LOGGER.info("[CommandControl] Received {} allowed commands from server", allowedCommands.size());
            });
        });
        
        // Clear state when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            allowedCommands.clear();
            syncReceived = false;
            LOGGER.debug("[CommandControl] Cleared command sync state");
        });
        
        LOGGER.info("[CommandControl] Client initialized!");
    }
    
    /**
     * Check if a command should be shown in suggestions
     * @param commandName The base command name (without /)
     * @return true if the command should be shown
     */
    public static boolean shouldShowCommand(String commandName) {
        // If we haven't received sync from server, show all commands (vanilla behavior)
        if (!syncReceived) {
            return true;
        }
        
        return allowedCommands.contains(commandName.toLowerCase());
    }
    
    /**
     * Check if command sync has been received from server
     */
    public static boolean isSyncReceived() {
        return syncReceived;
    }
    
    /**
     * Get the set of allowed commands
     */
    public static Set<String> getAllowedCommands() {
        return allowedCommands;
    }
}
