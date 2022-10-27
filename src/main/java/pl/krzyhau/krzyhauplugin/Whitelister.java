package pl.krzyhau.krzyhauplugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Whitelister {

    private static JavaPlugin plugin;
    private static File listFile;
    private static YamlConfiguration list;

    public static void initiate(JavaPlugin javaPlugin){
        plugin = javaPlugin;
        load();
    }

    public static void load(){
        listFile = new File(plugin.getDataFolder()+"/whitelist.yml");
        list = YamlConfiguration.loadConfiguration(listFile);
    }

    public static void save(){
        try{ list.save(listFile);
        }catch(IOException e){
            plugin.getLogger().log(Level.WARNING, "Couldn't save whitelist.yml.");
        }
    }

    private static boolean validateName(String name){
        return name.matches("^[a-zA-Z0-9_]+$") && name.length() >= 3 && name.length() <= 16;
    }

    // updates whitelisted minecraft username for given discord user ID
    // if name is "", given discord user is removed from record
    // if discordID <=0, discord user with given minecraft name is removed from record
    public static void updateUser(long discordID, String name){
        if(!validateName(name))throw new IllegalArgumentException("Given message is not a valid Minecraft username.");

        String discordIDStr = Long.toString(discordID);
        load();

        if(discordID > 0){
            list.set(discordIDStr, name.length() == 0 ? null : name);
        }else{
            for(String key:list.getKeys(false)){
                if(!name.equals(list.getString(key))) continue;
                list.set(key, null);
                break;
            }
        }

        save();
    }

    public static boolean isUserAllowed(String name){
        load();

        for(String key:list.getKeys(false)){
            if(name.equals(list.getString(key))) return true;
        }

        // check the actual whitelist as well
        for(OfflinePlayer player : Bukkit.getWhitelistedPlayers()){
            if(name.equals(player.getName())) return true;
        }

        return false;
    }
}
