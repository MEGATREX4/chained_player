package com.megatrex4;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;

public class ChainRenderer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                double x = client.player.getX();
                double y = client.player.getY();
                double z = client.player.getZ();

                // Send movement updates
                new ChainMovementHandler().sendMovementUpdate(x, y, z);
            }
        });
    }


    private void renderChain(PlayerEntity player1, PlayerEntity player2, MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        double x1 = player1.prevX + (player1.getX() - player1.prevX) * tickDelta;
        double y1 = player1.prevY + (player1.getY() - player1.prevY) * tickDelta + player1.getStandingEyeHeight();
        double z1 = player1.prevZ + (player1.getZ() - player1.prevZ) * tickDelta;

        double x2 = player2.prevX + (player2.getX() - player2.prevX) * tickDelta;
        double y2 = player2.prevY + (player2.getY() - player2.prevY) * tickDelta + player2.getStandingEyeHeight();
        double z2 = player2.prevZ + (player2.getZ() - player2.prevZ) * tickDelta;

        renderLine(matrices.peek().getPositionMatrix(), x1, y1, z1, x2, y2, z2, 0xFFFFFF, 0.5F);
    }

    private void renderLine(Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, int color, float lineWidth) {
        // Set up the shader for rendering lines
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Begin drawing the line using the Tessellator and BufferBuilder
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        // Extract color components from the provided color
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = 1.0F; // Full opacity

        // Add the vertices for the line
        bufferBuilder.vertex(matrix, (float) x1, (float) y1, (float) z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, (float) x2, (float) y2, (float) z2).color(red, green, blue, alpha).next();

        // Draw the line
        Tessellator.getInstance().draw();

        // Clean up by re-enabling the texture
        RenderSystem.disableBlend();
    }

}
