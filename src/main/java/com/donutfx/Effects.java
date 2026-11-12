package com.donutfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 8 tiers. Everything in a tier range plays the identical animation.
 *  1: 10M-99M   2: 100M-249M   3: 250M-499M   4: 500M-999M
 *  5: 1B-4B     6: 5B-9B       7: 10B-99B     8: 100B+
 */
public class Effects {
    static class Fx { String name; double amount; int tier; long start, dur; List<int[]> bolts = new ArrayList<>(); }
    static class P { double x, y, vx, vy; long born; int life; int rgb, w, h; boolean confetti; }
    static class Rocket { double x, ty; long launch; int rgb, size; }
    static class Snd { long at; SoundEvent ev; float pitch, vol; }

    private static final List<Fx> active = new ArrayList<>();
    private static final List<P> particles = new ArrayList<>();
    private static final List<Rocket> rockets = new ArrayList<>();
    private static final List<Snd> sounds = new ArrayList<>();
    private static final Random R = new Random();
    private static long lastFrame = System.currentTimeMillis();

    private static final int GOLD = 0xFFD700, ORANGE = 0xFF8800, WHITE = 0xFFFFFF, MAGENTA = 0xFF55FF;

    // per-tier tables, index 0 unused
    private static final String[] LABEL = {"", "DONATION", "BIG DONATION", "HUGE DONATION", "MASSIVE DONATION", "LEGENDARY DONATION", "MYTHIC DONATION", "GODLIKE DONATION", "ULTIMATE DONATION"};
    private static final String[] BADGE = {"", "10M", "100M", "250M", "500M", "1B", "5B", "10B", "100B"};
    private static final int[] ACCENT = {0, 0x55FF55, 0x55FFFF, GOLD, ORANGE, 0xFF5555, MAGENTA, 0, 0};
    private static final int[] ROCKETS = {0, 2, 4, 6, 8, 10, 12, 16, 22};
    private static final int[] RINGS = {0, 1, 2, 2, 3, 3, 4, 5, 6};
    private static final int[] RAYS = {0, 0, 10, 12, 14, 16, 18, 22, 28};
    private static final int[] BOLTS = {0, 0, 0, 1, 2, 3, 3, 3, 3};
    private static final long[] CONFETTI = {0, 2000, 3000, 4000, 5000, 6000, 7500, 9000, 12000};
    private static final long[] CENTER = {0, 3500, 4000, 4500, 5000, 6000, 7000, 8500, 11000};
    private static final double[] TEXTSCALE = {0, 3.0, 3.4, 3.8, 4.2, 4.6, 5.0, 5.4, 6.0};
    private static final double[] SHAKE = {0, 0.0, 1.0, 1.4, 1.8, 2.4, 3.0, 3.6, 4.5};

    // ------------------------------------------------------------ trigger
    public static void trigger(String name, double amount) {
        double[] th = DonutFxClient.config.tiers;
        int tier = 0;
        for (int i = 0; i < 8; i++) if (amount >= th[i]) tier = i + 1;
        if (tier == 0) return; // below 10M: no animation

        long now = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();

        Fx fx = new Fx();
        fx.name = name; fx.amount = amount; fx.tier = tier; fx.start = now;
        fx.dur = 10000 + 1500L * tier;
        for (int i = 0; i < BOLTS[tier]; i++) fx.bolts.add(bolt(w, h));
        active.add(fx);
        if (tier == 1) return; // 10M-99M: banner only, no effects or sounds

        burst(w - 90, 30, 40 + tier * 8, ACCENT[tier] != 0 ? ACCENT[tier] : GOLD, 1.3);
        burst(w / 2.0, h / 2.0, 180 + tier * 50, 0, 4.5 + tier * 0.8);
        if (tier >= 4) burst(w / 2.0, h / 2.0, 200 + tier * 25, 0, 8.0);
        if (tier >= 6) burst(w / 2.0, h / 2.0, 300, 0, 11.0);

        for (int i = 0; i < ROCKETS[tier]; i++) {
            Rocket r = new Rocket();
            r.x = w * (0.08 + R.nextDouble() * 0.84);
            r.ty = h * (0.12 + R.nextDouble() * 0.38);
            r.launch = now + 400 + i * Math.max(120L, 280L - tier * 20L);
            r.rgb = randColor();
            r.size = 55 + tier * 8;
            rockets.add(r);
            snd(r.launch - now, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.6f);
        }

        if (!DonutFxClient.config.sounds) return;
        snd(0, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1f);
        snd(250, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.9f, 1f);
        if (tier >= 2) snd(100, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1f);
        if (tier >= 3) snd(350, SoundEvents.ITEM_TOTEM_USE, 1.0f, 1f);
        if (tier >= 4) { snd(0, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1f); snd(900, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1f); }
        if (tier >= 5) snd(300, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1f);
        if (tier >= 6) { snd(0, SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 1f); snd(1000, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1f); }
        if (tier >= 7) { snd(600, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1f); snd(600, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1f); }
        if (tier >= 8) { snd(1200, SoundEvents.ITEM_TOTEM_USE, 0.7f, 1f); snd(1500, SoundEvents.ENTITY_WITHER_SPAWN, 0.7f, 1f); snd(1800, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f); }
    }

    private static void snd(long delay, SoundEvent ev, float pitch, float vol) {
        Snd s = new Snd(); s.at = System.currentTimeMillis() + Math.max(0, delay); s.ev = ev; s.pitch = pitch; s.vol = vol;
        sounds.add(s);
    }

    // ------------------------------------------------------------ helpers
    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }
    private static int rainbow(double t) { return Color.HSBtoRGB((float) (t - Math.floor(t)), 0.75f, 1f) & 0xFFFFFF; }
    private static int randColor() { return Color.HSBtoRGB(R.nextFloat(), 0.75f, 1f) & 0xFFFFFF; }
    private static double easeOutBack(double t) { double u = t - 1; return 1 + 2.70158 * u * u * u + 1.70158 * u * u; }
    private static int accent(int t, long age) { return ACCENT[t] != 0 ? ACCENT[t] : rainbow(age / 1500.0); }

    private static void line(DrawContext ctx, double x0, double y0, double x1, double y1, int th, int color) {
        double dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.max(1, Math.hypot(dx, dy) / th);
        for (int i = 0; i <= steps; i++) {
            int x = (int) (x0 + dx * i / steps), y = (int) (y0 + dy * i / steps);
            ctx.fill(x, y, x + th, y + th, color);
        }
    }

    private static int[] bolt(int w, int h) {
        int segs = 14;
        int[] pts = new int[(segs + 1) * 2];
        double x = R.nextInt(w), tx = w * 0.3 + R.nextDouble() * w * 0.4;
        int ty = (int) (h * 0.55);
        for (int i = 0; i <= segs; i++) {
            double t = i / (double) segs;
            double jitter = (i == 0 || i == segs) ? 0 : (R.nextDouble() - 0.5) * 70;
            pts[i * 2] = (int) (x + (tx - x) * t + jitter);
            pts[i * 2 + 1] = (int) (ty * t);
        }
        return pts;
    }

    private static void burst(double x, double y, int count, int rgb, double speed) {
        if (particles.size() > 3500) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            P p = new P();
            double a = R.nextDouble() * Math.PI * 2, s = (0.25 + R.nextDouble()) * speed;
            p.x = x; p.y = y; p.vx = Math.cos(a) * s; p.vy = Math.sin(a) * s;
            p.born = now; p.life = 900 + R.nextInt(1500);
            p.rgb = rgb != 0 ? (R.nextInt(4) == 0 ? WHITE : rgb)
                    : (R.nextInt(3) == 0 ? randColor() : (R.nextBoolean() ? GOLD : ORANGE));
            p.w = p.h = 1 + R.nextInt(3);
            particles.add(p);
        }
    }

    private static void outlinedText(DrawContext ctx, TextRenderer tr, String s, int x, int y, int rgb, int alpha) {
        if (alpha < 8) return;
        int dark = argb(alpha, 0x000000);
        ctx.drawText(tr, s, x - 1, y, dark, false);
        ctx.drawText(tr, s, x + 1, y, dark, false);
        ctx.drawText(tr, s, x, y - 1, dark, false);
        ctx.drawText(tr, s, x, y + 1, dark, false);
        ctx.drawText(tr, s, x, y, argb(alpha, rgb), false);
    }

    private static void waveText(DrawContext ctx, TextRenderer tr, String s, int cx, int y, long age, int tier, int alpha) {
        int x = cx - tr.getWidth(s) / 2;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            int yo = (int) Math.round(Math.sin(age / 140.0 + i * 0.7) * 2.5);
            int rgb = tier >= 5 ? rainbow(age / 1000.0 + i * 0.08)
                    : Color.HSBtoRGB(0.12f + 0.04f * (float) Math.sin(age / 200.0 + i), 0.85f, 1f) & 0xFFFFFF;
            outlinedText(ctx, tr, ch, x, y + yo, rgb, alpha);
            x += tr.getWidth(ch);
        }
    }

    // ------------------------------------------------------------ tick (camera shake + sound queue)
    public static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        Iterator<Snd> si = sounds.iterator();
        while (si.hasNext()) {
            Snd s = si.next();
            if (s.at <= now) {
                if (DonutFxClient.config.sounds)
                    mc.getSoundManager().play(PositionedSoundInstance.master(s.ev, s.pitch, s.vol));
                si.remove();
            }
        }
        if (mc.player == null || !DonutFxClient.config.cameraShake) return;
        for (Fx f : active) {
            long age = now - f.start;
            long shakeDur = 1500 + 250L * f.tier;
            if (age < shakeDur) {
                double mag = SHAKE[f.tier] * (1.0 - age / (double) shakeDur);
                mc.player.setYaw((float) (mc.player.getYaw() + (R.nextDouble() - 0.5) * mag));
                mc.player.setPitch((float) (mc.player.getPitch() + (R.nextDouble() - 0.5) * mag));
            }
        }
    }

    // ------------------------------------------------------------ render
    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        double dt = Math.min(50, now - lastFrame) / 16.0;
        lastFrame = now;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        TextRenderer tr = mc.textRenderer;
        int cx = w / 2, cy = h / 2;

        active.removeIf(f -> now - f.start > f.dur);

        // rockets
        Iterator<Rocket> ri = rockets.iterator();
        while (ri.hasNext()) {
            Rocket r = ri.next();
            if (now < r.launch) continue;
            double t = (now - r.launch) / 750.0;
            if (t >= 1) {
                burst(r.x, r.ty, r.size, r.rgb, 3.2);
                snd(0, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f + R.nextFloat() * 0.3f, 0.7f);
                snd(120, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 0.5f);
                ri.remove();
                continue;
            }
            double y = h + (r.ty - h) * (1 - (1 - t) * (1 - t));
            ctx.fill((int) r.x, (int) y, (int) r.x + 2, (int) y + 8, argb(255, WHITE));
            for (int k = 1; k <= 6; k++)
                ctx.fill((int) r.x, (int) y + 8 + k * 4, (int) r.x + 2, (int) y + 12 + k * 4, argb(200 - k * 30, GOLD));
        }

        // ---- pass 1: backgrounds
        for (Fx f : active) {
            long age = now - f.start;
            int t = f.tier;
            int acc = accent(t, age);
            if (t == 1) continue; // banner only

            // flash
            if (age < 700) ctx.fill(0, 0, w, h, argb((int) (235 * (1 - age / 700.0)), t >= 5 ? WHITE : 0xFFE9A0));
            if (t >= 6 && age > 900 && age < 1500) ctx.fill(0, 0, w, h, argb((int) (170 * (1 - (age - 900) / 600.0)), MAGENTA));
            if (t >= 6 && age < 5000) ctx.fill(0, 0, w, h, argb((int) (28 * (1 - age / 5000.0)), rainbow(age / 800.0)));

            // sunburst rays
            long sb = 2500 + 500L * t;
            if (RAYS[t] > 0 && age < sb) {
                double env = age < 300 ? age / 300.0 : Math.max(0, 1 - (age - 300) / (double) (sb - 300));
                int n = RAYS[t];
                double len = Math.hypot(w, h) / 2;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n + age * 0.0003;
                    int col = argb((int) (60 * env), i % 2 == 0 ? GOLD : (t >= 5 ? rainbow(i / (double) n + age / 3000.0) : WHITE));
                    line(ctx, cx, cy, cx + Math.cos(a) * len, cy + Math.sin(a) * len, 6, col);
                }
            }

            // shockwave rings
            for (int r = 0; r < RINGS[t]; r++) {
                double ra = age - r * 220.0;
                if (ra <= 0 || ra > 1900) continue;
                double radius = ra / 1900.0 * Math.max(w, h) * 0.8;
                int alpha = (int) (255 * (1 - ra / 1900.0));
                int steps = (int) Math.max(60, radius * 1.3);
                for (int i = 0; i < steps; i++) {
                    double a = i * Math.PI * 2 / steps;
                    int x = (int) (cx + Math.cos(a) * radius), y = (int) (cy + Math.sin(a) * radius * 0.9);
                    ctx.fill(x, y, x + 3, y + 3, argb(alpha, r % 3 == 0 ? GOLD : r % 3 == 1 ? acc : rainbow(i / (double) steps + age / 1500.0)));
                }
            }

            // lightning flashes
            long[] ft = {0, 220, 470};
            for (int b = 0; b < f.bolts.size(); b++) {
                if (age >= ft[b] && age < ft[b] + 110) {
                    ctx.fill(0, 0, w, h, argb(55, WHITE));
                    int[] p = f.bolts.get(b);
                    for (int i = 0; i < p.length / 2 - 1; i++)
                        line(ctx, p[i * 2], p[i * 2 + 1], p[i * 2 + 2], p[i * 2 + 3], 9, argb(80, 0x66AAFF));
                    for (int i = 0; i < p.length / 2 - 1; i++)
                        line(ctx, p[i * 2], p[i * 2 + 1], p[i * 2 + 2], p[i * 2 + 3], 3, argb(255, WHITE));
                }
            }

            // pulsing edge glow
            long vd = 2500 + 600L * t;
            if (age < vd) {
                double base = 120 * (1 - age / (double) vd) * (0.7 + 0.3 * Math.sin(age / 120.0));
                for (int i = 0; i < 10; i++) {
                    int a = (int) (base * (1 - i / 10.0)), o = i * 4;
                    ctx.fill(0, o, w, o + 4, argb(a, acc));
                    ctx.fill(0, h - o - 4, w, h - o, argb(a, acc));
                    ctx.fill(o, 0, o + 4, h, argb(a, acc));
                    ctx.fill(w - o - 4, 0, w - o, h, argb(a, acc));
                }
            }

            // confetti rain
            if (age < CONFETTI[t] && particles.size() < 3500) {
                int spawn = (int) Math.ceil(dt * (1 + t / 3.0));
                for (int i = 0; i < spawn; i++) {
                    P p = new P();
                    p.x = R.nextDouble() * w; p.y = -6;
                    p.vx = (R.nextDouble() - 0.5) * 1.2; p.vy = 1.4 + R.nextDouble() * 2.0;
                    p.born = now; p.life = 4200; p.rgb = randColor();
                    p.w = 2; p.h = 4; p.confetti = true;
                    particles.add(p);
                }
            }
        }

        // ---- particles
        Iterator<P> pi = particles.iterator();
        while (pi.hasNext()) {
            P p = pi.next();
            long age = now - p.born;
            if (age > p.life) { pi.remove(); continue; }
            if (p.confetti) {
                p.x += (p.vx + Math.sin(age / 200.0 + p.life) * 0.8) * dt;
                p.y += p.vy * dt;
            } else {
                p.x += p.vx * dt; p.y += p.vy * dt; p.vy += 0.04 * dt; p.vx *= 0.995;
            }
            if (p.y > h + 10) { pi.remove(); continue; }
            int a = (int) (255 * (1.0 - (double) age / p.life));
            ctx.fill((int) p.x, (int) p.y, (int) p.x + p.w, (int) p.y + p.h, argb(a, p.rgb));
        }

        // ---- pass 2: foreground
        int slotY = 0;
        for (Fx f : active) {
            long age = now - f.start;
            int t = f.tier;
            String amt = "$" + Parser.format(f.amount);
            int acc = accent(t, age);

            // cinematic letterbox (500M+)
            if (t >= 4 && age < 2800) {
                double e = age < 400 ? age / 400.0 : age < 2400 ? 1 : (2800 - age) / 400.0;
                int bar = (int) (h * 0.1 * e);
                ctx.fill(0, 0, w, bar, argb(255, 0));
                ctx.fill(0, h - bar, w, h, argb(255, 0));
            }

            // giant center text
            long cdur = CENTER[t];
            if (t > 1 && age < cdur) {
                int fade = age > cdur - 800 ? (int) (255 * (cdur - age) / 800.0) : 255;
                double base = TEXTSCALE[t];
                double popT = Math.min(1, age / 450.0);
                double s = base * easeOutBack(popT) + (age > 450 ? 0.12 * Math.sin(age / 110.0) : 0);
                int ccy = cy - 10;

                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((float) cx, (float) ccy);
                ctx.getMatrices().scale((float) s, (float) s);
                waveText(ctx, tr, amt, 0, -4, age, t, fade);
                ctx.getMatrices().popMatrix();

                if (age > 250) {
                    String label = "* " + LABEL[t] + " *";
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().translate((float) cx, (float) (ccy - base * 9 - 16));
                    ctx.getMatrices().scale(1.8f, 1.8f);
                    outlinedText(ctx, tr, label, -tr.getWidth(label) / 2, 0, t >= 5 ? rainbow(age / 800.0) : GOLD, fade);
                    ctx.getMatrices().popMatrix();

                    String nm = f.name + (t >= 5 ? " is a LEGEND!" : " donated!");
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().translate((float) cx, (float) (ccy + base * 9 + 8));
                    ctx.getMatrices().scale(2.4f, 2.4f);
                    outlinedText(ctx, tr, nm, -tr.getWidth(nm) / 2, 0, WHITE, fade);
                    ctx.getMatrices().popMatrix();
                }
            }

            // top-right banner with username (stays for the whole effect)
            int bh = 40;
            int bw = (int) Math.max(Math.max(170, tr.getWidth(f.name) + 70),
                    Math.max(tr.getWidth(amt) * 1.3 + 40, tr.getWidth(BADGE[t]) + 70));
            double slide = age < 400 ? 1 - easeOutBack(age / 400.0) : (age > f.dur - 400 ? 1 - (f.dur - age) / 400.0 : 0);
            int bx = (int) (w - bw - 8 + slide * (bw + 20));
            int by = 8 + slotY;
            slotY += bh + 6;

            ctx.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, argb(255, acc));
            ctx.fill(bx, by, bx + bw, by + bh, argb(240, 0x12121A));
            ctx.fill(bx, by, bx + bw, by + bh, argb((int) (40 + 30 * Math.sin(age / 150.0)), acc));
            double sx = bx + ((age / 900.0) % 1.6 - 0.3) * bw;
            int x0 = (int) Math.max(bx, sx), x1 = (int) Math.min(bx + bw, sx + 14);
            if (x1 > x0) ctx.fill(x0, by, x1, by + bh, argb(55, WHITE));

            ctx.drawText(tr, f.name, bx + 8, by + 5, argb(255, WHITE), true);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate((float) (bx + 8), (float) (by + 17));
            ctx.getMatrices().scale(1.3f, 1.3f);
            ctx.drawText(tr, amt, 0, 0, argb(255, acc), true);
            ctx.getMatrices().popMatrix();
            ctx.drawText(tr, BADGE[t], bx + bw - 8 - tr.getWidth(BADGE[t]), by + 5, argb(255, acc), true);
            int remain = (int) (bw * Math.max(0, 1 - age / (double) f.dur));
            ctx.fill(bx, by + bh - 3, bx + remain, by + bh, argb(255, acc));

            if (t > 1 && R.nextInt(2) == 0 && particles.size() < 3500)
                burst(bx + R.nextInt(bw), by + R.nextInt(bh), 1, ACCENT[t] != 0 ? ACCENT[t] : 0, 1.0);
        }
    }
}
