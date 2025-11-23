package com.bmfalkye.client.effects;

import com.bmfalkye.cards.Card;
import com.bmfalkye.game.FalkyeGameSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayDeque;

/**
 * Улучшенный менеджер клиентских эффектов для GUI игры
 * Отображает частицы и звуки в области интерфейса игры, а не в мире
 * Оптимизирован с использованием пула объектов и батчинга
 */
public class GuiEffectManager {
    
    /**
     * Представляет один эффект частиц в GUI
     */
    public static class GuiParticle {
        public float x;
        public float y;
        public int color;
        public float size;
        public int lifetime;
        public int age;
        public float vx;
        public float vy;
        public float rotation;
        public float rotationSpeed;
        
        public GuiParticle() {
            // Пустой конструктор для пула объектов
        }
        
        public void init(float x, float y, int color, float size, int lifetime, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.size = size;
            this.lifetime = lifetime;
            this.age = 0;
            this.vx = vx;
            this.vy = vy;
            this.rotation = 0.0f;
            this.rotationSpeed = (float) (Math.random() * 0.1f - 0.05f);
        }
        
        public void update() {
            age++;
            x += vx;
            y += vy;
            rotation += rotationSpeed;
            
            // Замедление со временем
            vx *= 0.98f;
            vy *= 0.98f;
        }
        
        public boolean isDead() {
            return age >= lifetime;
        }
        
        public float getAlpha() {
            return Math.max(0.0f, 1.0f - (float) age / lifetime);
        }
        
        public void reset() {
            // Сброс для возврата в пул
            age = 0;
            x = 0;
            y = 0;
            color = 0;
            size = 0;
            lifetime = 0;
            vx = 0;
            vy = 0;
            rotation = 0;
            rotationSpeed = 0;
        }
    }
    
    // Пул объектов для оптимизации
    private final Queue<GuiParticle> particlePool = new ArrayDeque<>();
    public final List<GuiParticle> particles = new ArrayList<>();
    public final List<GuiSound> sounds = new ArrayList<>();
    private static final int MAX_POOL_SIZE = 200;
    private static final int MAX_PARTICLES = 50; // АГРЕССИВНО УМЕНЬШЕНО: Было 150, стало 50 для производительности
    
    /**
     * Получает частицу из пула или создает новую
     */
    public GuiParticle getParticleFromPool(float x, float y, int color, float size, 
                                           int lifetime, float vx, float vy) {
        GuiParticle particle = particlePool.poll();
        if (particle == null) {
            particle = new GuiParticle();
        }
        particle.init(x, y, color, size, lifetime, vx, vy);
        return particle;
    }
    
    /**
     * Возвращает частицу в пул
     */
    private void returnParticleToPool(GuiParticle particle) {
        if (particlePool.size() < MAX_POOL_SIZE) {
            particle.reset();
            particlePool.offer(particle);
        }
    }
    
    /**
     * Представляет звук для воспроизведения в GUI
     */
    public static class GuiSound {
        public final SoundEvent sound;
        public final float volume;
        public final float pitch;
        public int delay; // Задержка в тиках перед воспроизведением
        
        public GuiSound(SoundEvent sound, float volume, float pitch, int delay) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.delay = delay;
        }
    }
    
    /**
     * Создает эффект игры карты в области GUI с использованием продвинутых эффектов
     */
    public void playCardPlayEffect(int guiX, int guiY, int guiWidth, int guiHeight, 
                                   Card card, FalkyeGameSession.CardRow row) {
        // Определяем позицию эффекта в зависимости от ряда
        float effectX = guiX + guiWidth / 2.0f;
        float effectY = guiY + guiHeight / 2.0f;
        
        // Корректируем позицию в зависимости от ряда
        switch (row) {
            case MELEE:
                effectY = guiY + guiHeight * 0.3f; // Верхняя часть (противник)
                break;
            case RANGED:
                effectY = guiY + guiHeight * 0.5f; // Середина
                break;
            case SIEGE:
                effectY = guiY + guiHeight * 0.7f; // Нижняя часть (игрок)
                break;
        }
        
        // Используем продвинутые эффекты
        AdvancedVisualEffects.createCardPlayEffect(this, effectX, effectY, card, row);
    }
    
    /**
     * Создает эффект окончания раунда с использованием продвинутых эффектов
     */
    public void playRoundEndEffect(int guiX, int guiY, int guiWidth, int guiHeight, boolean won) {
        float effectX = guiX + guiWidth / 2.0f;
        float effectY = guiY + guiHeight / 2.0f;
        
        if (won) {
            AdvancedVisualEffects.createVictoryEffect(this, effectX, effectY);
        } else {
            AdvancedVisualEffects.createDefeatEffect(this, effectX, effectY);
        }
    }
    
    /**
     * Создает эффект окончания игры с использованием продвинутых эффектов
     */
    public void playGameEndEffect(int guiX, int guiY, int guiWidth, int guiHeight, boolean won) {
        float effectX = guiX + guiWidth / 2.0f;
        float effectY = guiY + guiHeight / 2.0f;
        
        if (won) {
            AdvancedVisualEffects.createVictoryEffect(this, effectX, effectY);
        } else {
            AdvancedVisualEffects.createDefeatEffect(this, effectX, effectY);
        }
    }
    
    /**
     * Создает волновой эффект
     * ОПТИМИЗИРОВАНО: Меньше частиц
     */
    public void playWaveEffect(float centerX, float centerY, int color, float speed) {
        // Проверяем лимит частиц
        if (particles.size() >= MAX_PARTICLES) {
            return; // Не создаём новые, если лимит достигнут
        }
        
        for (int wave = 0; wave < 2; wave++) { // Было 3, стало 2
            final int waveNum = wave;
            // Меньше частиц на волну
            for (int i = 0; i < 12; i++) { // Было 20, стало 12
                if (particles.size() >= MAX_PARTICLES) break;
                
                double angle = (i / 12.0) * Math.PI * 2;
                float radius = waveNum * 30.0f;
                float x = centerX + (float) (Math.cos(angle) * radius);
                float y = centerY + (float) (Math.sin(angle) * radius);
                
                particles.add(getParticleFromPool(
                    x, y, color, 2.0f, 30, 
                    (float) (Math.cos(angle) * speed), 
                    (float) (Math.sin(angle) * speed)
                ));
            }
        }
    }
    
    /**
     * Обновляет все эффекты
     * ОПТИМИЗИРОВАНО: Ограничивает количество частиц
     */
    public void update() {
        // Ограничиваем количество частиц
        if (particles.size() > MAX_PARTICLES) {
            // Удаляем самые старые частицы
            particles.sort((p1, p2) -> Integer.compare(p2.age, p1.age));
            while (particles.size() > MAX_PARTICLES) {
                GuiParticle removed = particles.remove(particles.size() - 1);
                returnParticleToPool(removed);
            }
        }
        
        // Обновляем частицы
        Iterator<GuiParticle> particleIter = particles.iterator();
        while (particleIter.hasNext()) {
            GuiParticle particle = particleIter.next();
            particle.update();
            if (particle.isDead()) {
                particleIter.remove();
                returnParticleToPool(particle);
            }
        }
        
        // Обрабатываем звуки
        Iterator<GuiSound> soundIter = sounds.iterator();
        while (soundIter.hasNext()) {
            GuiSound sound = soundIter.next();
            if (sound.delay <= 0) {
                // Воспроизводим звук через игрока (простой способ)
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.player != null && minecraft.level != null && sound.sound != null) {
                    minecraft.level.playLocalSound(
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        sound.sound,
                        SoundSource.PLAYERS,
                        sound.volume,
                        sound.pitch,
                        false
                    );
                }
                soundIter.remove();
            } else {
                sound.delay--;
            }
        }
    }
    
    /**
     * АГРЕССИВНО ОПТИМИЗИРОВАННЫЙ рендеринг для критической производительности
     */
    public void render(GuiGraphics guiGraphics) {
        // АГРЕССИВНОЕ ОГРАНИЧЕНИЕ: Максимум 30 частиц за кадр (было 100)
        int maxParticlesToRender = 30;
        int particlesToRender = Math.min(particles.size(), maxParticlesToRender);
        
        // Группируем частицы по цвету для батчинга
        Map<Integer, List<GuiParticle>> particlesByColor = new HashMap<>();
        
        for (int i = 0; i < particlesToRender; i++) {
            GuiParticle particle = particles.get(i);
            int colorKey = particle.color & 0x00FFFFFF; // Без альфа
            particlesByColor.computeIfAbsent(colorKey, k -> new ArrayList<>()).add(particle);
        }
        
        // Оптимизированный рендеринг: упрощённая визуализация
        for (Map.Entry<Integer, List<GuiParticle>> entry : particlesByColor.entrySet()) {
            for (GuiParticle particle : entry.getValue()) {
                float alpha = particle.getAlpha();
                if (alpha < 0.1f) continue; // Пропускаем почти невидимые частицы
                
                int alphaInt = (int) (alpha * 255);
                int color = (entry.getKey() & 0x00FFFFFF) | (alphaInt << 24);
                
                // Упрощённая визуализация: простой квадрат вместо градиента
                int size = Math.max(1, Math.min(3, (int) particle.size)); // Ограничиваем размер
                int centerX = (int) particle.x;
                int centerY = (int) particle.y;
                
                // Простой квадрат (быстрее, чем градиент)
                if (size == 1) {
                    guiGraphics.fill(centerX, centerY, centerX + 1, centerY + 1, color);
                } else {
                    guiGraphics.fill(centerX - size/2, centerY - size/2, 
                                   centerX + size/2 + 1, centerY + size/2 + 1, color);
                }
            }
        }
    }
    
    /**
     * Очищает все эффекты
     */
    public void clear() {
        // Возвращаем все частицы в пул
        for (GuiParticle particle : particles) {
            returnParticleToPool(particle);
        }
        particles.clear();
        sounds.clear();
    }
    
    /**
     * Создает эффект изменения силы карты с использованием продвинутых эффектов
     */
    public void playPowerChangeEffect(float x, float y, int oldPower, int newPower) {
        AdvancedVisualEffects.createPowerChangeEffect(this, x, y, oldPower, newPower);
    }
    
    /**
     * Создает эффект комбо
     */
    public void playComboEffect(float centerX, float centerY, String comboType, int comboLevel) {
        AdvancedVisualEffects.createComboEffect(this, centerX, centerY, comboType, comboLevel);
    }
}
