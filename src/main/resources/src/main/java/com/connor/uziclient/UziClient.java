package com.connor.uziclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class UziClient implements ClientModInitializer {

    public static boolean freeCamEnabled = false;
    private KeyBinding toggleFreeCamKey;
    private KeyBinding chunkInspectorKey;

    @Override
    public void onInitializeClient() {
        toggleFreeCamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.uziclient.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.uziclient"
        ));

        chunkInspectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.uziclient.chunkinspect",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "category.uziclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleFreeCamKey.wasPressed()) {
                freeCamEnabled = !freeCamEnabled;
            }
            while (chunkInspectorKey.wasPressed()) {
                ChunkInspector.scanLoadedChunks(client);
            }
        });
    }
}
