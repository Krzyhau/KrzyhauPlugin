package pl.krzyhau.krzyhauplugin;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.dv8tion.jda.api.JDA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftListener implements Listener {

    private final DiscordIntegration discord;
    private final JavaPlugin plugin;

    public MinecraftListener(DiscordIntegration discord, JavaPlugin javaPlugin){
        super();
        this.plugin = javaPlugin;
        this.discord = discord;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event){

        if(!plugin.getConfig().getBoolean("whitelist")) event.allow();

        if(!Whitelister.isUserAllowed(event.getPlayer().getName())){
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Component.text()
                    .append(Component.text("You're not whitelisted. Join "))
                    .append(Component.text("Krzyhau's Discord Server")
                            .color(NamedTextColor.BLUE)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.openUrl("https://discord.gg/GF62QBuZc7")))
                    .append(Component.text(" to gain access."))
                    .build());
        }else event.allow();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();
        msgBuilder.setUsername(String.format("%s joined the game!", event.getPlayer().getName()));
        msgBuilder.setAvatarUrl(String.format("https://crafatar.com/renders/head/%s?default=MHF_Steve&overlay",event.getPlayer().getUniqueId()));
        msgBuilder.setContent("*<:krzyPepega:892473904482881566>*");
        discord.getWebhook().send(msgBuilder.build());

        discord.onMinecraftPlayerCountUpdated(plugin.getServer().getOnlinePlayers().size());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();
        msgBuilder.setUsername(String.format("%s left the game!", event.getPlayer().getName()));
        msgBuilder.setAvatarUrl(String.format("https://crafatar.com/renders/head/%s?default=MHF_Steve&overlay",event.getPlayer().getUniqueId()));
        msgBuilder.setContent("*<:krzyOhNoes:892507900491206717>*");
        discord.getWebhook().send(msgBuilder.build());

        discord.onMinecraftPlayerCountUpdated(plugin.getServer().getOnlinePlayers().size());
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();
        msgBuilder.setUsername(serializeForDiscord(event.deathMessage()));
        msgBuilder.setAvatarUrl(String.format("https://crafatar.com/renders/head/%s?default=MHF_Steve&overlay",event.getPlayer().getUniqueId()));
        msgBuilder.setContent("*<:krzyWarCrimes:834897863669973023>*");
        discord.getWebhook().send(msgBuilder.build());
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event)
    {
        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();
        msgBuilder.setUsername(serializeForDiscord(event.message()));
        msgBuilder.setAvatarUrl(String.format("https://crafatar.com/renders/head/%s?default=MHF_Steve&overlay",event.getPlayer().getUniqueId()));
        msgBuilder.setContent("*<:krzyUuu:892471216940671006>*");
        discord.getWebhook().send(msgBuilder.build());
    }

    @EventHandler
    public void onPlayerSendMessage(AsyncChatEvent event){
        if(discord.getAdapter().getStatus() != JDA.Status.CONNECTED) return;

        String name = event.getPlayer().getName();
        String message = serializeForDiscord(event.message());

        WebhookMessageBuilder msgBuilder = new WebhookMessageBuilder();
        msgBuilder.setUsername(name);
        msgBuilder.setAvatarUrl(String.format("https://crafatar.com/avatars/%s?default=MHF_Steve&overlay",event.getPlayer().getUniqueId()));
        msgBuilder.setContent(message);
        discord.getWebhook().send(msgBuilder.build());
    }

    private String serializeForDiscord(Component message){
        return PlainTextComponentSerializer.plainText().serialize(message)
                .replace("\\", "\\\\")
                .replace("@", "\\@\u200D")
                .replace("`", "\\`")
                .replace("§", "\\§");
    }
}
