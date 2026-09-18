import React, { useEffect, useRef, useState } from 'react';
import { Volume2, VolumeX, Pause, Play, RefreshCw, Layers } from 'lucide-react';
import { playRetroSound } from '../utils/retroAudio';

interface PlayableEmulatorScreenProps {
  gameTitle: string;
  gameId: string;
  englishTitle: string;
  resolution: string;
  renderer: string;
  shader: string;
  isMuted: boolean;
  onToggleMute: () => void;
  activeButton: string | null;
  onOpenRomsModal: () => void;
}

export const PlayableEmulatorScreen: React.FC<PlayableEmulatorScreenProps> = ({
  gameTitle,
  gameId,
  englishTitle,
  resolution,
  renderer,
  shader,
  isMuted,
  onToggleMute,
  activeButton,
  onOpenRomsModal
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [fps, setFps] = useState(60);
  const [playerScore, setPlayerScore] = useState(1450);
  const [playerHp, setPlayerHp] = useState(88);
  const [enemyHp, setEnemyHp] = useState(64);
  const [comboCount, setComboCount] = useState(0);

  // Character internal position state
  const playerPosRef = useRef({ x: 70, y: 130, vx: 0, vy: 0, state: 'idle', facing: 1 });
  const enemyPosRef = useRef({ x: 230, y: 130, state: 'idle', facing: -1, timer: 0 });
  const particlesRef = useRef<Array<{ x: number; y: number; vx: number; vy: number; life: number; color: string }>>([]);

  // Trigger game response when physical/virtual button is pressed
  useEffect(() => {
    if (!activeButton) return;

    if (!isMuted) {
      if (activeButton.includes('TRIANGLE') || activeButton.includes('CIRCLE') || activeButton.includes('CROSS') || activeButton.includes('SQUARE')) {
        playRetroSound('action');
      } else if (activeButton.includes('UP') || activeButton.includes('DOWN') || activeButton.includes('LEFT') || activeButton.includes('RIGHT')) {
        playRetroSound('dpad');
      } else if (activeButton === 'START' || activeButton === 'SELECT') {
        playRetroSound('start');
      }
    }

    const p = playerPosRef.current;
    if (activeButton.includes('LEFT')) {
      p.x = Math.max(25, p.x - 12);
      p.facing = -1;
      p.state = 'walk';
    } else if (activeButton.includes('RIGHT')) {
      p.x = Math.min(270, p.x + 12);
      p.facing = 1;
      p.state = 'walk';
    } else if (activeButton.includes('UP')) {
      p.y = Math.max(90, p.y - 10);
      p.state = 'jump';
      if (!isMuted) playRetroSound('jump');
    } else if (activeButton.includes('DOWN')) {
      p.y = Math.min(145, p.y + 10);
      p.state = 'crouch';
    } else if (activeButton.includes('CROSS') || activeButton.includes('SQUARE')) {
      p.state = 'punch';
      // Attack hit check
      if (Math.abs(p.x - enemyPosRef.current.x) < 45) {
        setEnemyHp(prev => Math.max(0, prev - 8));
        setPlayerScore(prev => prev + 120);
        setComboCount(prev => prev + 1);
        if (!isMuted) playRetroSound('hit');
        // Spawn sparks
        for (let i = 0; i < 8; i++) {
          particlesRef.current.push({
            x: (p.x + enemyPosRef.current.x) / 2,
            y: p.y - 20,
            vx: (Math.random() - 0.5) * 6,
            vy: (Math.random() - 0.5) * 6 - 2,
            life: 1,
            color: '#F59E0B'
          });
        }
      }
    } else if (activeButton.includes('TRIANGLE') || activeButton.includes('CIRCLE')) {
      p.state = 'kick';
      if (Math.abs(p.x - enemyPosRef.current.x) < 55) {
        setEnemyHp(prev => Math.max(0, prev - 14));
        setPlayerScore(prev => prev + 250);
        setComboCount(prev => prev + 2);
        if (!isMuted) playRetroSound('hit');
        for (let i = 0; i < 12; i++) {
          particlesRef.current.push({
            x: (p.x + enemyPosRef.current.x) / 2,
            y: p.y - 25,
            vx: (Math.random() - 0.5) * 8,
            vy: (Math.random() - 0.5) * 8 - 3,
            life: 1,
            color: '#EC4899'
          });
        }
      }
    }

    const t = setTimeout(() => {
      p.state = 'idle';
    }, 260);

    return () => clearTimeout(t);
  }, [activeButton, isMuted]);

  // Main 60FPS Game Loop Rendering on Canvas
  useEffect(() => {
    let animId: number;
    let lastTime = performance.now();
    let frameCount = 0;
    let fpsTimer = 0;

    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const render = (time: number) => {
      const dt = (time - lastTime) / 1000;
      lastTime = time;
      frameCount++;
      fpsTimer += dt;
      if (fpsTimer >= 1) {
        setFps(frameCount);
        frameCount = 0;
        fpsTimer = 0;
      }

      if (!isPaused) {
        // Simple AI logic for enemy
        const enemy = enemyPosRef.current;
        const player = playerPosRef.current;
        enemy.timer += dt;
        if (enemy.timer > 1.2) {
          enemy.timer = 0;
          if (enemy.x > player.x + 35) {
            enemy.x -= 4;
            enemy.facing = -1;
          } else if (enemy.x < player.x - 35) {
            enemy.x += 4;
            enemy.facing = 1;
          }
        }

        // Draw Stage Background (PS1 3D Arena Look)
        const w = canvas.width;
        const h = canvas.height;

        // Sky / Arena Back wall
        const grad = ctx.createLinearGradient(0, 0, 0, h);
        grad.addColorStop(0, '#0F172A');
        grad.addColorStop(0.5, '#1E293B');
        grad.addColorStop(0.7, '#334155');
        grad.addColorStop(1, '#020617');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, w, h);

        // Perspective Floor Grid (Classic Tekken / PS1 3D floor)
        ctx.strokeStyle = '#38BDF8';
        ctx.lineWidth = 1;
        ctx.globalAlpha = 0.35;
        const horizon = 100;
        for (let x = -w; x <= w * 2; x += 30) {
          ctx.beginPath();
          ctx.moveTo(w / 2, horizon);
          ctx.lineTo(x, h);
          ctx.stroke();
        }
        for (let y = horizon + 5; y <= h; y += 15) {
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(w, y);
          ctx.stroke();
        }
        ctx.globalAlpha = 1.0;

        // Draw Shadows
        ctx.fillStyle = 'rgba(0, 0, 0, 0.45)';
        ctx.beginPath();
        ctx.ellipse(player.x, 158, 18, 5, 0, 0, Math.PI * 2);
        ctx.fill();

        ctx.beginPath();
        ctx.ellipse(enemy.x, 158, 18, 5, 0, 0, Math.PI * 2);
        ctx.fill();

        // Draw Player Sprite (Fighter P1 - Blue Martial Artist)
        ctx.save();
        ctx.translate(player.x, player.y);
        ctx.scale(player.facing, 1);

        // Torso
        ctx.fillStyle = '#2563EB';
        ctx.fillRect(-8, -25, 16, 20);
        // Head
        ctx.fillStyle = '#FED7AA';
        ctx.beginPath();
        ctx.arc(0, -32, 7, 0, Math.PI * 2);
        ctx.fill();
        // Headband (Red)
        ctx.fillStyle = '#EF4444';
        ctx.fillRect(-7, -35, 14, 3);

        // Arms based on state
        if (player.state === 'punch') {
          ctx.fillStyle = '#1D4ED8';
          ctx.fillRect(6, -22, 18, 6);
          ctx.fillStyle = '#FED7AA';
          ctx.beginPath();
          ctx.arc(24, -19, 4, 0, Math.PI * 2);
          ctx.fill();
        } else if (player.state === 'kick') {
          ctx.fillStyle = '#1D4ED8';
          ctx.fillRect(2, -10, 20, 7);
        } else {
          // Idle guard arms
          ctx.fillStyle = '#1D4ED8';
          ctx.fillRect(2, -22, 10, 6);
        }

        // Legs
        ctx.fillStyle = '#1E3A8A';
        ctx.fillRect(-7, -5, 6, 16);
        ctx.fillRect(1, -5, 6, 16);
        ctx.restore();

        // Draw Enemy Sprite (Fighter P2 - Red/Dark Cyber Ninja)
        ctx.save();
        ctx.translate(enemy.x, enemy.y);
        ctx.scale(enemy.facing, 1);

        // Torso
        ctx.fillStyle = '#DC2626';
        ctx.fillRect(-8, -25, 16, 20);
        // Head
        ctx.fillStyle = '#1E293B';
        ctx.beginPath();
        ctx.arc(0, -32, 7, 0, Math.PI * 2);
        ctx.fill();
        // Ninja Visor (Cyan)
        ctx.fillStyle = '#22D3EE';
        ctx.fillRect(-6, -33, 12, 3);
        // Arms
        ctx.fillStyle = '#991B1B';
        ctx.fillRect(2, -22, 10, 6);
        // Legs
        ctx.fillStyle = '#7F1D1D';
        ctx.fillRect(-7, -5, 6, 16);
        ctx.fillRect(1, -5, 6, 16);
        ctx.restore();

        // Update & Render Particles / Sparks
        const particles = particlesRef.current;
        for (let i = particles.length - 1; i >= 0; i--) {
          const pt = particles[i];
          pt.x += pt.vx;
          pt.y += pt.vy;
          pt.life -= 0.05;
          if (pt.life <= 0) {
            particles.splice(i, 1);
          } else {
            ctx.fillStyle = pt.color;
            ctx.globalAlpha = pt.life;
            ctx.fillRect(pt.x, pt.y, 3, 3);
          }
        }
        ctx.globalAlpha = 1.0;
      }

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [isPaused]);

  return (
    <div className="relative w-full h-full bg-[#090D16] rounded-2xl overflow-hidden border border-slate-700/80 flex flex-col justify-between select-none shadow-inner">
      {/* CRT Scanline Shader Filter */}
      {shader.includes('CRT') && (
        <div className="absolute inset-0 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.3)_50%)] bg-[length:100%_4px] pointer-events-none z-30 opacity-40"></div>
      )}
      {shader.includes('Bilinear') && (
        <div className="absolute inset-0 backdrop-blur-[0.4px] pointer-events-none z-30"></div>
      )}

      {/* Top HUD: Health Bars, Score & Game Engine Meta */}
      <div className="relative z-20 px-3 pt-2 flex items-center justify-between">
        {/* P1 Player HP Bar */}
        <div className="flex flex-col items-start w-28">
          <div className="flex items-center justify-between w-full text-[9px] font-mono font-bold text-sky-400 mb-0.5">
            <span>P1 (أنت)</span>
            <span>{playerHp}%</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-xs border border-sky-600/50 p-0.5">
            <div 
              className="h-full bg-gradient-to-r from-emerald-500 to-sky-400 transition-all duration-150"
              style={{ width: `${playerHp}%` }}
            ></div>
          </div>
        </div>

        {/* Center Round / Game Title */}
        <div className="flex flex-col items-center">
          <span className="text-[10px] font-bold text-amber-400 font-mono tracking-wider">ROUND 1</span>
          <span className="text-[8px] text-slate-400 font-mono">{fps} FPS • Cloudflare Edge</span>
        </div>

        {/* P2 Enemy HP Bar */}
        <div className="flex flex-col items-end w-28">
          <div className="flex items-center justify-between w-full text-[9px] font-mono font-bold text-rose-400 mb-0.5">
            <span>{enemyHp}%</span>
            <span>P2 (الخصم)</span>
          </div>
          <div className="w-full h-2.5 bg-slate-800 rounded-xs border border-rose-600/50 p-0.5">
            <div 
              className="h-full bg-gradient-to-l from-rose-500 to-amber-400 transition-all duration-150 ml-auto"
              style={{ width: `${enemyHp}%` }}
            ></div>
          </div>
        </div>
      </div>

      {/* Center 60FPS Canvas Viewport */}
      <div className="relative flex-1 flex items-center justify-center overflow-hidden">
        <canvas
          ref={canvasRef}
          width={320}
          height={180}
          className="w-full h-full object-contain image-rendering-pixelated"
        />

        {/* Combo Hits Overlay */}
        {comboCount > 0 && (
          <div className="absolute top-2 left-4 text-amber-400 font-black text-xs font-mono drop-shadow animate-bounce">
            {comboCount} HITS COMBO!
          </div>
        )}

        {/* Button Feedback Banner */}
        {activeButton && (
          <div className="absolute bottom-2 inset-x-0 mx-auto w-fit px-2.5 py-0.5 bg-[#2563EB]/90 text-white rounded-full text-[10px] font-mono font-bold shadow-md z-20">
            {activeButton}
          </div>
        )}
      </div>

      {/* Bottom Mini Control Bar */}
      <div className="relative z-20 px-3 py-1.5 bg-black/60 backdrop-blur-xs border-t border-slate-800/80 flex items-center justify-between text-[10px] text-slate-300">
        <div className="flex items-center space-x-1.5 space-x-reverse font-['Tajawal'] font-bold">
          <span className="text-sky-300">{gameTitle}</span>
          <span className="text-slate-500 font-mono text-[9px]">({resolution})</span>
        </div>

        <div className="flex items-center space-x-1.5 space-x-reverse">
          <button
            onClick={() => setIsPaused(!isPaused)}
            className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 transition"
            title={isPaused ? 'استئناف' : 'إيقاف مؤقت'}
          >
            {isPaused ? <Play className="w-3 h-3 text-emerald-400" /> : <Pause className="w-3 h-3 text-amber-400" />}
          </button>

          <button
            onClick={onToggleMute}
            className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 transition"
            title={isMuted ? 'تشغيل الصوت' : 'كتم الصوت'}
          >
            {isMuted ? <VolumeX className="w-3 h-3 text-rose-400" /> : <Volume2 className="w-3 h-3 text-sky-400" />}
          </button>

          <button
            onClick={onOpenRomsModal}
            className="px-2 py-0.5 bg-[#2563EB] hover:bg-blue-600 text-white rounded text-[10px] font-['Tajawal'] font-bold transition"
          >
            تبديل اللعبة
          </button>
        </div>
      </div>
    </div>
  );
};
