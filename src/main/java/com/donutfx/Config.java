package com.donutfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class Config {
    // Named groups: name, amount, suffix (suffix optional: k/m/b/t)
    public String regex = "(?i)(?<name>\\w{3,16}) (?:paid|sent) you \\$?(?<amount>[\\d,]+(?:\\.\\d+)?)\\s*(?<suffix>[kmbt])?";
    // Minimum amount for each of the 8 tiers (anything below tiers[0] shows nothing)
    public double[] tiers = {10e6, 100e6, 250e6, 500e6, 1e9, 5e9, 10e9, 100e9};
    public boolean cameraShake = true;
    public boolean sounds = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("donutfx.json"); }

    public static Config load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c.tiers == null || c.tiers.length != 8) c.tiers = new Config().tiers;
                return c;
            }
            Config c = new Config();
            Files.writeString(p, GSON.toJson(c));
            return c;
        } catch (Exception e) {
            e.printStackTrace();
            return new Config();
        }
    }

    public Pattern pattern() { return Pattern.compile(regex); }
}
