package com.vincentporath.commandcontrol.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.vincentporath.commandcontrol.CommandControl;
import com.vincentporath.commandcontrol.config.CommandControlConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.brigadier.ParseResults;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Server-side mixin to filter command suggestions and block unauthorized commands
 */
@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;

    /**
     * Filter command suggestions sent to players
     */
    @Inject(method = "sendCommandTree", at = @At("HEAD"), cancellable = true)
    private void commandcontrol$filterCommandTree(ServerPlayerEntity player, CallbackInfo ci) {
        // OP level 4 sees all commands
        if (player.hasPermissionLevel(4)) {
            return;
        }
        
        try {
            ServerCommandSource source = player.getCommandSource();
            
            // Build filtered command tree
            Map<CommandNode<ServerCommandSource>, CommandNode<CommandSource>> visitedNodes = new IdentityHashMap<>();
            RootCommandNode<CommandSource> resultRoot = new RootCommandNode<>();
            visitedNodes.put(this.dispatcher.getRoot(), resultRoot);
            
            // Process each top-level command
            for (CommandNode<ServerCommandSource> child : this.dispatcher.getRoot().getChildren()) {
                String commandName = child.getName().toLowerCase();
                
                // Check if command is allowed for this player
                if (CommandControlConfig.isCommandAllowed(player, commandName) && child.canUse(source)) {
                    buildFilteredTree(child, resultRoot, source, visitedNodes);
                }
            }
            
            // Send filtered packet
            player.networkHandler.sendPacket(new CommandTreeS2CPacket(resultRoot));
            ci.cancel();
            
        } catch (Exception e) {
            CommandControl.LOGGER.error("[CommandControl] Error filtering command tree", e);
            // Fall back to vanilla behavior on error
        }
    }
    
    /**
     * Block execution of unauthorized commands
     */
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void commandcontrol$blockUnauthorizedCommand(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfoReturnable<Integer> cir) {
        ServerCommandSource source = parseResults.getContext().getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // OP level 4 bypasses restrictions
            if (player.hasPermissionLevel(4)) {
                return;
            }
            
            // Extract base command
            String baseCommand = command.split(" ")[0].toLowerCase();
            if (baseCommand.startsWith("/")) {
                baseCommand = baseCommand.substring(1);
            }
            
            // Check if command is allowed
            if (!CommandControlConfig.isCommandAllowed(player, baseCommand)) {
                player.sendMessage(Text.literal("§c[CommandControl] You do not have permission for this command."), false);
                cir.setReturnValue(0);
            }
        }
    }
    
    /**
     * Recursively build filtered command tree
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildFilteredTree(
            CommandNode<ServerCommandSource> node,
            CommandNode<CommandSource> parent,
            ServerCommandSource source,
            Map<CommandNode<ServerCommandSource>, CommandNode<CommandSource>> visitedNodes
    ) {
        CommandNode<CommandSource> existingNode = visitedNodes.get(node);
        if (existingNode != null) {
            parent.addChild(existingNode);
            return;
        }
        
        CommandNode<CommandSource> newNode = createNodeCopy(node);
        if (newNode == null) {
            return;
        }
        
        visitedNodes.put(node, newNode);
        parent.addChild(newNode);
        
        // Process children
        for (CommandNode<ServerCommandSource> child : node.getChildren()) {
            if (child.canUse(source)) {
                buildFilteredTree(child, newNode, source, visitedNodes);
            }
        }
    }
    
    /**
     * Create a CommandSource copy of a ServerCommandSource node
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private CommandNode<CommandSource> createNodeCopy(CommandNode<ServerCommandSource> node) {
        if (node instanceof LiteralCommandNode literal) {
            return new LiteralCommandNode<>(
                    literal.getLiteral(),
                    null,
                    s -> true,
                    null,
                    null,
                    literal.isFork()
            );
        } else if (node instanceof ArgumentCommandNode argument) {
            ArgumentType<?> type = argument.getType();
            
            // Check if argument type is serializable
            if (ArgumentTypes.getArgumentTypeProperties(type) == null) {
                return null;
            }
            
            return new ArgumentCommandNode<>(
                    argument.getName(),
                    type,
                    null,
                    s -> true,
                    null,
                    null,
                    argument.isFork(),
                    argument.getCustomSuggestions()
            );
        }
        
        return null;
    }
}
