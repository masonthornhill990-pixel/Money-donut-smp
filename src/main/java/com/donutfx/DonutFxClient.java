package com.donutfx;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonutFxClient implements ClientModInitializer {
    public static Config config;
    private static Pattern pattern;

    @Override
    public void onInitializeClient() {
        reload();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handle(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> handle(message.getString()));

        HudElementRegistry.addLast(Identifier.of("donutfx", "overlay"), (ctx, tick) -> Effects.render(ctx));
        ClientTickEvents.END_CLIENT_TICK.register(client -> Effects.tick(client));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
            dispatcher.register(ClientCommandManager.literal("donutfx")
                .then(ClientCommandManager.literal("reload").executes(c -> {
                    reload();
                    c.getSource().sendFeedback(Text.literal("DonutFX config reloaded"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("test")
                    .then(ClientCommandManager.argument("amount", StringArgumentType.word()).executes(c -> {
                        double amt = Parser.parseAmount(StringArgumentType.getString(c, "amount"));
                        Effects.trigger("TestPlayer", amt);
                        return 1;
                    }))));
        });
    }

    public static void reload() {
        config = Config.load();
        try { pattern = config.pattern(); } catch (Exception e) { pattern = Pattern.compile("a^"); }
    }

    private static void handle(String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return;
        try {
            String name = m.group("name");
            String amount = m.group("amount");
            String suffix;
            try { suffix = m.group("suffix"); } catch (Exception e) { suffix = null; }
            double value = Parser.parseAmount(amount + (suffix == null ? "" : suffix));
            if (value > 0) MinecraftClient.getInstance().execute(() -> Effects.trigger(name, value));
        } catch (Exception ignored) {}
    }
}
