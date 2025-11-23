package com.bmfalkye.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Партикл природного эффекта (использует стандартные текстуры)
 */
public class NatureEffectParticle extends TextureSheetParticle {
    protected NatureEffectParticle(ClientLevel level, double x, double y, double z, 
                                   double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.lifetime = 30;
        this.quadSize = 0.2f;
        this.rCol = 0.2f;
        this.gCol = 0.8f;
        this.bCol = 0.3f;
        this.pickSprite(spriteSet); // Используем стандартные спрайты
    }
    
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        
        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }
        
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                      double x, double y, double z, 
                                      double xd, double yd, double zd) {
            return new NatureEffectParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}

