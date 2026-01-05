package com.vincentporath.commandcontrol.mixin.client;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.vincentporath.commandcontrol.client.CommandControlClient;
import net.minecraft.client.network.ClientCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Client-side mixin to filter command suggestions for client-side mods
 * This catches commands that are registered only on the client (like Xaero's Minimap commands)
 */
@Mixin(ClientCommandSource.class)
public class ClientCommandSourceMixin {
    
    /**
     * Filter command suggestions on the client side
     * This is called when the client generates suggestions for commands
     */
    @Inject(method = "getCompletions", at = @At("RETURN"), cancellable = true)
    private void commandcontrol$filterClientSuggestions(CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        // Only filter if we've received sync from a CommandControl-enabled server
        if (!CommandControlClient.isSyncReceived()) {
            return;
        }
        
        CompletableFuture<Suggestions> originalFuture = cir.getReturnValue();
        
        CompletableFuture<Suggestions> filteredFuture = originalFuture.thenApply(suggestions -> {
            if (suggestions.isEmpty()) {
                return suggestions;
            }
            
            List<Suggestion> filtered = new ArrayList<>();
            
            for (Suggestion suggestion : suggestions.getList()) {
                String text = suggestion.getText();
                
                // Extract command name (handle both with and without /)
                String commandName = text;
                if (commandName.startsWith("/")) {
                    commandName = commandName.substring(1);
                }
                
                // Get base command (before any space or subcommand)
                int spaceIndex = commandName.indexOf(' ');
                if (spaceIndex > 0) {
                    commandName = commandName.substring(0, spaceIndex);
                }
                
                // Check if this command is allowed
                if (CommandControlClient.shouldShowCommand(commandName)) {
                    filtered.add(suggestion);
                }
            }
            
            return new Suggestions(suggestions.getRange(), filtered);
        });
        
        cir.setReturnValue(filteredFuture);
    }
}
