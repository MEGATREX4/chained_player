package com.megatrex4.client;

import com.megatrex4.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.UUID;

public class ChainRenderer {
    private static final float SEGMENT_LENGTH      = 0.5f;
    private static final float MAX_RENDER_DISTANCE = 64f;
    private static final float CHAIN_ALPHA         = 0.85f;
    private static final float CHAIN_WIDTH         = 0.1f;
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

            Vec3d camPos = ctx.camera().getPos();
            float delta  = client.getTickDelta();

            Vec3d eyeA = local.getLerpedPos(delta)
                    .add(0, local.getEyeHeight(local.getPose()) - 0.3, 0)
                    .subtract(camPos);
            Vec3d eyeB = partner.getLerpedPos(delta)
                    .add(0, partner.getEyeHeight(partner.getPose()) - 0.3, 0)
                    .subtract(camPos);

            if (eyeA.distanceTo(eyeB) <= MAX_RENDER_DISTANCE) {
                renderThickChain(ctx.matrixStack(), eyeA, eyeB, world, camPos);
            }
        });
    }

    private static void renderThickChain(
            MatrixStack ms,
            Vec3d from, Vec3d to,
            World world,
            Vec3d camPos
    ) {
        double dist     = from.distanceTo(to);
        int    segments = Math.max(1, (int)Math.ceil(dist / SEGMENT_LENGTH));
        double h        = computeH(dist);

        Matrix4f mat    = ms.peek().getPositionMatrix();
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();

        // початкові налаштування рендеру
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();

        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float t0 = i       / (float)segments;
            float t1 = (i + 1) / (float)segments;

            Vec3d base0 = from.lerp(to, t0);
            Vec3d base1 = from.lerp(to, t1);

            float sag0 = computeParabolaSag(t0, h);
            float sag1 = computeParabolaSag(t1, h);

            // світові координати точок
            double wx0 = base0.x + camPos.x, wz0 = base0.z + camPos.z, wy0 = base0.y + camPos.y;
            double wx1 = base1.x + camPos.x, wz1 = base1.z + camPos.z, wy1 = base1.y + camPos.y;

            // обмежуємо провисання землею + 0.05
            double ground0 = sampleGroundHeight(world, wx0, wz0, wy0);
            double ground1 = sampleGroundHeight(world, wx1, wz1, wy1);
            sag0 = Math.max(sag0, (float)(ground0 - wy0));
            sag1 = Math.max(sag1, (float)(ground1 - wy1));

            Vec3d p0 = new Vec3d(base0.x, base0.y + sag0, base0.z);
            Vec3d p1 = new Vec3d(base1.x, base1.y + sag1, base1.z);

            // перевірка блоку всередині сегмента
            Vec3d mid = p0.add(p1).multiply(0.5).add(camPos);
            BlockPos midPos = new BlockPos(
                    MathHelper.floor(mid.x),
                    MathHelper.floor(mid.y),
                    MathHelper.floor(mid.z)
            );
            if (world.getBlockState(midPos).isOpaqueFullCube(world, midPos)) continue;

            // напрям і перпендикуляри
            Vec3d dir   = p1.subtract(p0).normalize();
            Vec3d perp1 = dir.crossProduct(new Vec3d(0,1,0)).normalize().multiply(CHAIN_WIDTH/2);
            Vec3d perp2 = dir.crossProduct(perp1).normalize().multiply(CHAIN_WIDTH/2);

            int baseCol = (i % 2 == 0) ? COLOR_LIGHT : COLOR_DARK;

            // освітлення та AO
            int blk = world.getLightLevel(LightType.BLOCK, midPos);
            int sky = world.getLightLevel(LightType.SKY,   midPos);
            float bright = MathHelper.clamp((blk + sky)/30f, 0.4f, 1f);
            int occ = 0;
            for (Vec3d off : new Vec3d[]{
                    new Vec3d(1,0,0), new Vec3d(-1,0,0),
                    new Vec3d(0,0,1), new Vec3d(0,0,-1)
            }) {
                BlockPos adj = midPos.add((int)off.x,(int)off.y,(int)off.z);
                if (world.getBlockState(adj).isOpaqueFullCube(world, adj)) occ++;
            }
            bright *= (1f - 0.2f * occ);

            int r = (int)(((baseCol>>16)&0xFF)*bright);
            int g = (int)(((baseCol>>8 )&0xFF)*bright);
            int b = (int)(( baseCol     &0xFF)*bright);
            int a = (int)(CHAIN_ALPHA*255);

            // малюємо квадрати
            buf.vertex(mat, (float)(p0.x+perp1.x),(float)(p0.y+perp1.y),(float)(p0.z+perp1.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p0.x-perp1.x),(float)(p0.y-perp1.y),(float)(p0.z-perp1.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p1.x-perp1.x),(float)(p1.y-perp1.y),(float)(p1.z-perp1.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p1.x+perp1.x),(float)(p1.y+perp1.y),(float)(p1.z+perp1.z))
                    .color(r,g,b,a).next();

            buf.vertex(mat, (float)(p0.x+perp2.x),(float)(p0.y+perp2.y),(float)(p0.z+perp2.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p0.x-perp2.x),(float)(p0.y-perp2.y),(float)(p0.z-perp2.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p1.x-perp2.x),(float)(p1.y-perp2.y),(float)(p1.z-perp2.z))
                    .color(r,g,b,a).next();
            buf.vertex(mat, (float)(p1.x+perp2.x),(float)(p1.y+perp2.y),(float)(p1.z+perp2.z))
                    .color(r,g,b,a).next();
        }
        tes.draw();

        // відновлюємо стани
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // обчислює “провисання” параболи
    private static double computeH(double dist) {
        double maxLen = ModConfig.BOTH.chainLength;
        double slack  = Math.max(0.0, maxLen - dist);
        return Math.sqrt(3 * dist * slack / 8.0);
    }

    // параболічна функція y = 4h·t·(t–1)
    private static float computeParabolaSag(float t, double h) {
        return (float)(4 * h * t * (t - 1));
    }

    // шукає поверхню (y верхньої границі блоку) + невеликий офсет
    private static double sampleGroundHeight(World world, double x, double z, double startY) {
        int ix = MathHelper.floor(x);
        int iz = MathHelper.floor(z);
        int iy = MathHelper.floor(startY);
        for (int y = iy; y >= 0; y--) {
            BlockPos pos = new BlockPos(ix, y, iz);
            if (world.getBlockState(pos).isOpaqueFullCube(world, pos)) {
                return y + 1.0 + 0.05;
            }
        }
        return 0.05;
    }
}
