package net.sprocketgames.mctelemetry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MCTelemetry.MOD_ID)
public class TelemetryCommand
{
    private static final Gson GSON = new Gson();

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(buildCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand()
    {
        return Commands.literal("telemetry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("json")
                        .then(Commands.argument("nonce", StringArgumentType.word())
                                .executes(context -> {
                                    String nonce = StringArgumentType.getString(context, "nonce");
                                    String json = buildJson(context.getSource());
                                    MCTelemetry.LOGGER.info("TELEMETRY {} {}", nonce, json);
                                    return 1;
                                })));
    }

    private static String buildJson(CommandSourceStack source)
    {
        JsonObject root = new JsonObject();
        root.addProperty("mc", MCTelemetry.MINECRAFT_VERSION);
        root.addProperty("loader", MCTelemetry.LOADER);

        JsonArray players = new JsonArray();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers())
        {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("name", player.getGameProfile().getName());
            playerObj.addProperty("uuid", player.getGameProfile().getId().toString().replace("-", ""));
            players.add(playerObj);
        }

        root.add("players", players);
        return GSON.toJson(root);
    }
}
