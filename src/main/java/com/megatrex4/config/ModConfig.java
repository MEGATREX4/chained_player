package com.megatrex4.config;

import com.megatrex4.ChainedPlayers;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;

public class ModConfig {

    // Register and load config on startup
    public static Client BOTH = ConfigApiJava.registerAndLoadConfig(Client::new, RegisterType.BOTH);

    @Version(version = 1)
    public static class Client extends Config {

        public Client() {
            super(new Identifier(ChainedPlayers.MOD_ID, "config"));
        }

        @Comment("Whether confirmation is required before taking action")
        @Translation(prefix = "option.chained.requireConfirmation")
        public boolean requireConfirmation = true;

        @Comment("The pulling force multiplier, determines how strongly players pull each other.")
        @Translation(prefix = "option.chained.pullForce")
        @ValidatedInt.Restrict(min = 1, max = 10)
        public int pullForce = 5; // Default value


        @Comment("The length of the chain")
        @Translation(prefix = "option.chained.chainLength")
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int chainLength = 10;

        public void saveConfig() {
            this.save();
        }
    }

    public static void saveConfig() {
        BOTH.saveConfig();
    }
}
