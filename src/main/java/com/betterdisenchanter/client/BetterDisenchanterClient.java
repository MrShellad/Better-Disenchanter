package com.betterdisenchanter.client;

import com.betterdisenchanter.BetterDisenchanter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class BetterDisenchanterClient {
    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(BetterDisenchanterClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BetterDisenchanter.DISENCHANTER_BE.get(), BetterDisenchanterBlockEntityRenderer::new);
    }
}
