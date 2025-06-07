package com.minecraftmods.mcia;


import com.minecraftmods.mcia.commands.GeneratorCommand;
import com.minecraftmods.mcia.commands.SchematicsCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainMod implements ModInitializer {
    public static final String MOD_ID = "mcia";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            LOGGER.info("MCIA Mod initializing commands...");
            GeneratorCommand.register(dispatcher);
            SchematicsCommand.register(dispatcher);
            LOGGER.info("MCIA Mod commands initialized successfully!");
        });
    }
}
