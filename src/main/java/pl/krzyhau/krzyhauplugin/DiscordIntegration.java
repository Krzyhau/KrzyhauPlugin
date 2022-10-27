package pl.krzyhau.krzyhauplugin;

import club.minnced.discord.webhook.WebhookClient;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DiscordIntegration extends ListenerAdapter {

    private final JavaPlugin plugin;
    private final JDA adapter;
    private final WebhookClient webhook;

    private Message whitelistBtnMessage;

    public DiscordIntegration(JavaPlugin javaPlugin)
    {
        super();
        plugin = javaPlugin;
        adapter = JDABuilder.createDefault(plugin.getConfig().getString("botToken"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(this)
                .setMemberCachePolicy(member -> true)
                .setActivity(Activity.playing("Minecraft"))
                .setStatus(OnlineStatus.ONLINE)
                .build();

        String webhookUrl = plugin.getConfig().getString("webhookUrl");
        if(webhookUrl == null) throw new IllegalArgumentException("Webhook URL is missing");
        webhook = WebhookClient.withUrl(webhookUrl);
    }

    public void cleanUp(){
        //whitelistBtnMessage.delete().timeout(10, TimeUnit.SECONDS).complete(); // wait for message to be deleted
        webhook.close();
        adapter.shutdownNow();
    }

    public JDA getAdapter(){
        return adapter;
    }

    public WebhookClient getWebhook(){
        return webhook;
    }

    public TextChannel getRelayChannel(){
        long relayChannelID = plugin.getConfig().getLong("relayChannelId");
        return adapter.getTextChannelById(relayChannelID);
    }

    public TextChannel getWhitelistRequestChannel(){
        long relayChannelID = plugin.getConfig().getLong("whitelistChannelId");
        return adapter.getTextChannelById(relayChannelID);
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        plugin.getLogger().log(Level.INFO, "Discord bot connected!");

        // verify if the text channels are valid
        if(getRelayChannel() == null){
            plugin.getLogger().log(Level.WARNING, "Invalid relay channel ID detected! Make sure to change it in config.yml");
        }

        if(getWhitelistRequestChannel() == null){
            plugin.getLogger().log(Level.WARNING, "Invalid whitelist button channel ID detected! Make sure to change it in config.yml");
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){

        // check if the message was sent in the relay channel
        TextChannel relayCh = getRelayChannel();
        if(relayCh != null && event.getChannel().getIdLong() == relayCh.getIdLong()){
            onRelayChatMessage(event);
            return;
        }

        // check if it was whitelist channel instead
        TextChannel whitelistCh = getWhitelistRequestChannel();
        if(whitelistCh != null && event.getChannel().getIdLong() == whitelistCh.getIdLong()){
            onWhitelistRequest(event);
        }
    }
    private void onRelayChatMessage(MessageReceivedEvent event){
        // don't relay the message if they're sent by a relay itself
        if(event.isWebhookMessage()) return;

        String name = event.getAuthor().getName();
        String nameTag = event.getAuthor().getAsTag();
        String message = event.getMessage().getContentStripped();
        plugin.getServer().broadcast(Component.text()
                .append(Component.text("<").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
                .append(Component.text(name).hoverEvent(HoverEvent.showText(Component.text(nameTag))))
                .append(Component.text(">").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD))
                .append(Component.text(" "+message.replace('§', '&')))
                .build()
        );
    }

    private void replyThenDeleteBoth(MessageReceivedEvent event, String messageString){
        event.getMessage().reply(messageString).queue(message->{
            try{
                event.getMessage().delete().and(message.delete()).queueAfter(10,TimeUnit.SECONDS);
            }catch(InsufficientPermissionException ex){
                event.getChannel().sendMessage("Cannot delete messages due to insufficient permissions!").queue();
            }
        });
    }

    private void onWhitelistRequest(MessageReceivedEvent event){
        // Ignore if it's bot's message (or ours... i know, redundant check, fck off)
        if(event.getAuthor().isBot() || event.getAuthor().getIdLong() == adapter.getSelfUser().getIdLong()) return;

        String newUsername = event.getMessage().getContentStripped();
        try{
            Whitelister.updateUser(event.getAuthor().getIdLong(),newUsername);
        }catch(Exception e){
            replyThenDeleteBoth(event, String.format("%s",e.getMessage()));
            return;
        }

        //set user's role as well, just to know who plays minecraft lol
        long roleId = plugin.getConfig().getLong("whitelistRoleId");
        Role mcRole = event.getGuild().getRoleById(roleId);
        if(mcRole == null) return;
        event.getGuild().addRoleToMember(event.getAuthor(), mcRole).queue();

        replyThenDeleteBoth(event, String.format("Your whitelisted nickname has been set to **%s**!", newUsername));

    }
}
