package pl.krzyhau.krzyhauplugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class KrzyhauPlugin extends JavaPlugin{

    private DiscordIntegration discord = null;
    @Override
    public void onEnable() {

        this.getLogger().log(Level.INFO, "The most epic plugin on this planet!!!");



        if(!new File(this.getDataFolder(), "config.yml").exists()){
            this.saveDefaultConfig();
            this.getLogger().log(Level.WARNING, "Default config file created. Please configure the plugin before using it.");
        }else{
            this.getLogger().log(Level.INFO, "Setting up Discord integration...");
            try{
                discord = new DiscordIntegration(this);
            } catch (Exception e){
                this.getLogger().log(Level.WARNING, "Error occurred while setting up Discord integration:" + e.getMessage());
                this.getLogger().log(Level.WARNING, "Make sure to configure the plugin before using it.");
            }
        }

        if(discord != null){
            Whitelister.initiate(this);
            getServer().getPluginManager().registerEvents(new MinecraftListener(discord, this), this);
        }else{
            getServer().getPluginManager().disablePlugin(this);
        }

    }
    @Override
    public void onDisable() {
        discord.cleanUp();
        this.getLogger().log(Level.INFO, "Disabling... Bye!");
    }

}
