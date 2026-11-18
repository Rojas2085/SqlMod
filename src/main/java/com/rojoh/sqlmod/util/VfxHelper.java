package com.rojoh.sqlmod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Gestor de efectos visuales y auditivos (Estilo Magiteck / Resonancia) para sqlmod.
 */
public class VfxHelper {

    /**
     * Traza un haz de energía mística y armónicos entre un cofre origen y un cofre destino.
     */
    public static void spawnTransferBeam(ServerLevel level, BlockPos origin, BlockPos dest) {
        if (level == null || origin == null || dest == null) return;

        // Sonido de extracción en origen (Chime de amatista)
        level.m_5594_(null, origin, SoundEvents.f_144243_, SoundSource.BLOCKS, 0.8F, 1.2F);
        // Partículas en origen
        level.m_8767_(ParticleTypes.f_123760_, origin.m_123341_() + 0.5D, origin.m_123342_() + 0.8D, origin.m_123343_() + 0.5D, 12, 0.25D, 0.25D, 0.25D, 0.05D);

        // Trazado de partículas a lo largo de la trayectoria
        double dx = (dest.m_123341_() - origin.m_123341_());
        double dy = (dest.m_123342_() - origin.m_123342_());
        double dz = (dest.m_123343_() - origin.m_123343_());
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > 0.1D && dist <= 64.0D) {
            int steps = Math.min(30, Math.max(5, (int)(dist * 2)));
            double startX = origin.m_123341_() + 0.5D;
            double startY = origin.m_123342_() + 0.7D;
            double startZ = origin.m_123343_() + 0.5D;

            double stepX = dx / steps;
            double stepY = dy / steps;
            double stepZ = dz / steps;

            for (int i = 1; i < steps; i++) {
                double px = startX + stepX * i;
                double py = startY + stepY * i;
                double pz = startZ + stepZ * i;

                // Alternar entre runas de encantamiento y partículas de portal invertido
                if (i % 2 == 0) {
                    level.m_8767_(ParticleTypes.f_123809_, px, py, pz, 1, 0.05D, 0.05D, 0.05D, 0.02D);
                } else {
                    level.m_8767_(ParticleTypes.f_123789_, px, py, pz, 1, 0.02D, 0.02D, 0.02D, 0.01D);
                }
            }
        }

        // Sonido y partículas de recepción en destino (Resonancia de amatista)
        level.m_5594_(null, dest, SoundEvents.f_276532_, SoundSource.BLOCKS, 0.9F, 1.4F);
        level.m_8767_(ParticleTypes.f_123809_, dest.m_123341_() + 0.5D, dest.m_123342_() + 1.1D, dest.m_123343_() + 0.5D, 18, 0.3D, 0.3D, 0.3D, 0.1D);
        level.m_8767_(ParticleTypes.f_123760_, dest.m_123341_() + 0.5D, dest.m_123342_() + 0.9D, dest.m_123343_() + 0.5D, 10, 0.2D, 0.2D, 0.2D, 0.05D);
    }

    /**
     * Feedback de éxito al ejecutar una consulta en la terminal.
     */
    public static void playTerminalSuccess(ServerLevel level, BlockPos terminalPos) {
        if (level == null || terminalPos == null) return;
        // Bip armónico de amatista
        level.m_5594_(null, terminalPos, SoundEvents.f_144243_, SoundSource.BLOCKS, 1.0F, 1.8F);
        // Destello de runas místicas y brillo
        level.m_8767_(ParticleTypes.f_123749_, terminalPos.m_123341_() + 0.5D, terminalPos.m_123342_() + 1.1D, terminalPos.m_123343_() + 0.5D, 12, 0.2D, 0.2D, 0.2D, 0.05D);
        level.m_8767_(ParticleTypes.f_123809_, terminalPos.m_123341_() + 0.5D, terminalPos.m_123342_() + 1.2D, terminalPos.m_123343_() + 0.5D, 8, 0.2D, 0.2D, 0.2D, 0.08D);
    }

    /**
     * Feedback de error de sintaxis en la terminal.
     */
    public static void playTerminalError(ServerLevel level, BlockPos terminalPos) {
        if (level == null || terminalPos == null) return;
        // Sonido apagado grave de advertencia
        level.m_5594_(null, terminalPos, SoundEvents.f_11888_, SoundSource.BLOCKS, 0.8F, 0.6F);
        // Humo gris
        level.m_8767_(ParticleTypes.f_123762_, terminalPos.m_123341_() + 0.5D, terminalPos.m_123342_() + 1.1D, terminalPos.m_123343_() + 0.5D, 8, 0.15D, 0.15D, 0.15D, 0.02D);
    }

    /**
     * Feedback de activación de trigger.
     */
    public static void playTriggerFired(ServerLevel level, BlockPos terminalPos) {
        if (level == null || terminalPos == null) return;
        // Resonancia de amatista aguda
        level.m_5594_(null, terminalPos, SoundEvents.f_144243_, SoundSource.BLOCKS, 1.0F, 2.0F);
        // Partículas púrpuras
        level.m_8767_(ParticleTypes.f_123789_, terminalPos.m_123341_() + 0.5D, terminalPos.m_123342_() + 1.2D, terminalPos.m_123343_() + 0.5D, 15, 0.2D, 0.3D, 0.2D, 0.05D);
    }
}
