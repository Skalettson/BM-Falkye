package com.bmfalkye.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Партикл при поражении (использует стандартные текстуры)
 */
public class DefeatParticle extends TextureSheetParticle {
    protected DefeatParticle(ClientLevel level, double x, double y, double z, 
                            double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.lifetime = 40;
        this.quadSize = 0.3f;
        this.rCol = 0.5f;
        this.gCol = 0.0f;
        this.bCol = 0.0f; // Красный цвет
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
            return new DefeatParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}

