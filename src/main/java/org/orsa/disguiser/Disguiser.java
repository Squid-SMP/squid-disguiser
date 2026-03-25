package org.orsa.disguiser;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.Permissions;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Disguiser implements ModInitializer {

    @Override
    public void onInitialize() {
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, registrationEnvironment) ->
                dispatcher.register(literal("disguise")
                .requires(source -> source.getPermissions().hasPermission(new Permission.Level(PermissionLevel.MODERATORS)))
                .then(argument("name", StringArgumentType.string()))
                .executes(context -> {
                    var source = context.getSource();
                    source.sendMessage(Text.literal("Test"));
                    return 1;
                })
            )
        );
    }

    private void randomDisguise() {

    }
}
