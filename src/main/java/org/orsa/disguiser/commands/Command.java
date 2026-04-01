package org.orsa.disguiser.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;

public class Command {
    protected CommandDispatcher<ServerCommandSource> dispatcher;

    public Command(CommandDispatcher<ServerCommandSource> dispatcher) {
        this.dispatcher = dispatcher;
        init();
    }

    protected void init() {}
}
