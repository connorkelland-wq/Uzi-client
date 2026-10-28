package com.uzi.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class UziClientMenu extends Screen {

    // ARGB colors — alpha, red, green, blue
    private static final int COLOR_TOP = 0xE0FF1493;    // hot pink, slightly transparent
    private static final int COLOR_BOTTOM = 0xE06A0DAD; // deep purple, slightly transparent

    public UziClientMenu() {
        super(Text.literal("Uzi Client"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Suspicious Chunk Finder: " + (UziClient.finderEnabled ? "ON" : "OFF")),
                button -> {
                    UziClient.finderEnabled = !UziClient.finderEnabled;
                    button.setMessage(Text.literal(
                            "Suspicious Chunk Finder: " + (UziClient.finderEnabled ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Free Cam: " + (UziClient.freeCamEnabled ? "ON" : "OFF")),
                button -> {
                    UziClient.freeCamEnabled = !UziClient.freeCamEnabled;
                    button.setMessage(Text.literal(
                            "Free Cam: " + (UziClient.freeCamEnabled ? "ON" : "OFF")));
                }
        ).dimensions(centerX - 100, startY + 30, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                button -> this.close()
        ).dimensions(centerX - 100, startY + 60, 200, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Override the default dark background with our pink-to-purple gradient
        context.fillGradient(0, 0, this.width, this.height, COLOR_TOP, COLOR_BOTTOM);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF
        );
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
