package com.megatrex4.client;

import com.megatrex4.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.UUID;

public class ChainRenderer {
    private static final float SEGMENT_LENGTH      = 0.5f;    // довжина сегмента в блоках
    private static final float MAX_RENDER_DISTANCE = 64f;
    private static final float CHAIN_ALPHA         = 0.85f;
    private static final float CHAIN_WIDTH         = 0.1f;    // товщина ланцюга
    private static final int   COLOR_LIGHT         = 0xBBBBBB;
    private static final int   COLOR_DARK          = 0x666666;

    public static void register() {
        WorldRenderEvents.LAST.register(ctx -> {
            MinecraftClient client = MinecraftClient.getInstance();
            World world = client.world;
            PlayerEntity local = client.player;
            if (world == null || local == null) return;

            UUID partnerUuid = ClientChainData.chainedPlayers.get(local.getUuid());
            if (partnerUuid == null) return;
            PlayerEntity partner = world.getPlayerByUuid(partnerUuid);
            if (partner == null) return;

            MatrixStack ms    = ctx.matrixStack();
            Vec3d      camPos = ctx.camera().getPos();
            float      delta  = client.getTickDelta();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            // Анкери: від очей локального та очей партнера, зміщені -0.3
            Vec3d eyeA = local.getLerpedPos(delta)
                    .add(0, local.getEyeHeight(local.getPose()) - 0.3, 0)
                    .subtract(camPos);
            Vec3d eyeB = partner.getLerpedPos(delta)
                    .add(0, partner.getEyeHeight(partner.getPose()) - 0.3, 0)
                    .subtract(camPos);

            if (eyeA.distanceTo(eyeB) <= MAX_RENDER_DISTANCE) {
                renderThickChain(ms, eyeA, eyeB, client.world, camPos);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        });
    }

    private static void renderThickChain(
            MatrixStack ms,
            Vec3d from, Vec3d to,
            World world,
            Vec3d camPos) {

        // без параболи — просто пряма лінія
        double dist   = from.distanceTo(to);
        int segments  = Math.max(1, (int)Math.ceil(dist / SEGMENT_LENGTH));
        Matrix4f mat  = ms.peek().getPositionMatrix();
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();

        RenderSystem.disableCull();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float t0 = i       / (float)segments;
            float t1 = (i + 1) / (float)segments;

            // лінійна інтерполяція без провисання
            Vec3d p0 = from.lerp(to, t0);
            Vec3d p1 = from.lerp(to, t1);

            Vec3d dir   = p1.subtract(p0).normalize();
            Vec3d perp1 = dir.crossProduct(new Vec3d(0, 1, 0))
                    .normalize()
                    .multiply(CHAIN_WIDTH / 2);
            Vec3d perp2 = dir.crossProduct(perp1)
                    .normalize()
                    .multiply(CHAIN_WIDTH / 2);

            int base = (i % 2 == 0) ? COLOR_LIGHT : COLOR_DARK;

            // малюємо два перехрещених квадрати
            buf.vertex(mat, (float)(p0.x + perp1.x), (float)(p0.y + perp1.y), (float)(p0.z + perp1.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p0.x - perp1.x), (float)(p0.y - perp1.y), (float)(p0.z - perp1.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p1.x - perp1.x), (float)(p1.y - perp1.y), (float)(p1.z - perp1.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p1.x + perp1.x), (float)(p1.y + perp1.y), (float)(p1.z + perp1.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();

            buf.vertex(mat, (float)(p0.x + perp2.x), (float)(p0.y + perp2.y), (float)(p0.z + perp2.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p0.x - perp2.x), (float)(p0.y - perp2.y), (float)(p0.z - perp2.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p1.x - perp2.x), (float)(p1.y - perp2.y), (float)(p1.z - perp2.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
            buf.vertex(mat, (float)(p1.x + perp2.x), (float)(p1.y + perp2.y), (float)(p1.z + perp2.z))
                    .color(base, base, base, (int)(CHAIN_ALPHA * 255)).next();
        }

        tes.draw();
        RenderSystem.enableCull();
    }
}
