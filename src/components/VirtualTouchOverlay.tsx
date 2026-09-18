import React, { useState, useEffect } from 'react';
import { PS1Button, TouchControlsConfig } from '../types';
import { inputManager } from '../services/ps1InputManager';
import { touchConfigManager, DEFAULT_TOUCH_CONFIG } from '../services/touchConfigStorage';

interface VirtualTouchOverlayProps {
  isVisible: boolean;
  onOpenCustomizer?: () => void;
}

export const VirtualTouchOverlay: React.FC<VirtualTouchOverlayProps> = ({ isVisible, onOpenCustomizer }) => {
  const [activeButtons, setActiveButtons] = useState<Set<PS1Button>>(new Set());
  const [config, setConfig] = useState<TouchControlsConfig>(DEFAULT_TOUCH_CONFIG);

  useEffect(() => {
    const unsub = touchConfigManager.subscribe((newCfg) => {
      setConfig(newCfg);
    });
    return unsub;
  }, []);

  if (!isVisible) return null;

  const handleTouchStart = (btn: PS1Button) => {
    inputManager.setTouchButton(btn, true);
    setActiveButtons(prev => new Set(prev).add(btn));
    if (config.hapticEnabled && typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(config.hapticDuration || 15);
    }
  };

  const handleTouchEnd = (btn: PS1Button) => {
    inputManager.setTouchButton(btn, false);
    setActiveButtons(prev => {
      const next = new Set(prev);
      next.delete(btn);
      return next;
    });
  };

  return (
    <div 
      id="ps1-virtual-touch-overlay" 
      className="absolute inset-0 pointer-events-none z-30 flex justify-between items-end p-4 pb-6 select-none transition-opacity duration-200"
      style={{ opacity: config.opacity }}
    >
      {/* Left Cluster: D-Pad & L1/L2 */}
      <div 
        className="flex flex-col gap-3 pointer-events-auto origin-bottom-left transition-transform duration-150"
        style={{
          transform: `scale(${config.scale})`,
          marginLeft: `${config.dpadPos.x - 20}px`,
          marginBottom: `${config.dpadPos.y - 20}px`,
        }}
      >
        {/* Shoulder Buttons */}
        <div className="flex gap-2 mb-1">
          <button
            id="touch-btn-l1"
            onTouchStart={() => handleTouchStart(PS1Button.L1)}
            onTouchEnd={() => handleTouchEnd(PS1Button.L1)}
            onMouseDown={() => handleTouchStart(PS1Button.L1)}
            onMouseUp={() => handleTouchEnd(PS1Button.L1)}
            className={`w-14 h-8 rounded-lg bg-slate-900/70 border border-slate-700 text-xs font-bold font-mono-retro text-slate-300 active:bg-slate-700 ${
              activeButtons.has(PS1Button.L1) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            L1
          </button>
          <button
            id="touch-btn-l2"
            onTouchStart={() => handleTouchStart(PS1Button.L2)}
            onTouchEnd={() => handleTouchEnd(PS1Button.L2)}
            onMouseDown={() => handleTouchStart(PS1Button.L2)}
            onMouseUp={() => handleTouchEnd(PS1Button.L2)}
            className={`w-14 h-8 rounded-lg bg-slate-900/70 border border-slate-700 text-xs font-bold font-mono-retro text-slate-300 active:bg-slate-700 ${
              activeButtons.has(PS1Button.L2) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            L2
          </button>
        </div>

        {/* D-Pad Cross */}
        <div className="relative w-36 h-36 bg-slate-950/40 rounded-full border border-slate-800/80 p-2 backdrop-blur-xs flex items-center justify-center">
          {/* UP */}
          <button
            id="touch-dpad-up"
            onTouchStart={() => handleTouchStart(PS1Button.DPAD_UP)}
            onTouchEnd={() => handleTouchEnd(PS1Button.DPAD_UP)}
            onMouseDown={() => handleTouchStart(PS1Button.DPAD_UP)}
            onMouseUp={() => handleTouchEnd(PS1Button.DPAD_UP)}
            className={`absolute top-1.5 w-11 h-12 bg-slate-800/80 active:bg-slate-600 rounded-t-md flex items-center justify-center text-slate-300 shadow-md ${
              activeButtons.has(PS1Button.DPAD_UP) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            ▲
          </button>
          {/* DOWN */}
          <button
            id="touch-dpad-down"
            onTouchStart={() => handleTouchStart(PS1Button.DPAD_DOWN)}
            onTouchEnd={() => handleTouchEnd(PS1Button.DPAD_DOWN)}
            onMouseDown={() => handleTouchStart(PS1Button.DPAD_DOWN)}
            onMouseUp={() => handleTouchEnd(PS1Button.DPAD_DOWN)}
            className={`absolute bottom-1.5 w-11 h-12 bg-slate-800/80 active:bg-slate-600 rounded-b-md flex items-center justify-center text-slate-300 shadow-md ${
              activeButtons.has(PS1Button.DPAD_DOWN) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            ▼
          </button>
          {/* LEFT */}
          <button
            id="touch-dpad-left"
            onTouchStart={() => handleTouchStart(PS1Button.DPAD_LEFT)}
            onTouchEnd={() => handleTouchEnd(PS1Button.DPAD_LEFT)}
            onMouseDown={() => handleTouchStart(PS1Button.DPAD_LEFT)}
            onMouseUp={() => handleTouchEnd(PS1Button.DPAD_LEFT)}
            className={`absolute left-1.5 w-12 h-11 bg-slate-800/80 active:bg-slate-600 rounded-l-md flex items-center justify-center text-slate-300 shadow-md ${
              activeButtons.has(PS1Button.DPAD_LEFT) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            ◀
          </button>
          {/* RIGHT */}
          <button
            id="touch-dpad-right"
            onTouchStart={() => handleTouchStart(PS1Button.DPAD_RIGHT)}
            onTouchEnd={() => handleTouchEnd(PS1Button.DPAD_RIGHT)}
            onMouseDown={() => handleTouchStart(PS1Button.DPAD_RIGHT)}
            onMouseUp={() => handleTouchEnd(PS1Button.DPAD_RIGHT)}
            className={`absolute right-1.5 w-12 h-11 bg-slate-800/80 active:bg-slate-600 rounded-r-md flex items-center justify-center text-slate-300 shadow-md ${
              activeButtons.has(PS1Button.DPAD_RIGHT) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            ▶
          </button>
          {/* Center Hub */}
          <div className="w-9 h-9 bg-slate-900 rounded-full border border-slate-700" />
        </div>
      </div>

      {/* Center Select & Start */}
      <div 
        className="flex flex-col items-center gap-1.5 pointer-events-auto mb-2 origin-bottom transition-transform duration-150"
        style={{
          transform: `scale(${config.scale}) translate(${config.centerPos.x}px, ${-config.centerPos.y + 16}px)`,
        }}
      >
        <div className="flex gap-4">
          <button
            id="touch-btn-select"
            onTouchStart={() => handleTouchStart(PS1Button.SELECT)}
            onTouchEnd={() => handleTouchEnd(PS1Button.SELECT)}
            className="px-3 py-1.5 bg-slate-900/80 rounded-md border border-slate-700 text-[10px] font-mono-retro text-slate-400 active:bg-slate-700"
          >
            SELECT
          </button>
          <button
            id="touch-btn-start"
            onTouchStart={() => handleTouchStart(PS1Button.START)}
            onTouchEnd={() => handleTouchEnd(PS1Button.START)}
            className="px-3 py-1.5 bg-slate-900/80 rounded-md border border-slate-700 text-[10px] font-mono-retro text-slate-400 active:bg-slate-700"
          >
            START
          </button>
        </div>

        {onOpenCustomizer && (
          <button
            id="touch-btn-customizer-shortcut"
            onClick={onOpenCustomizer}
            title="تخصيص أزرار التحكم اللمسية (HUD Editor)"
            className="px-2.5 py-0.5 rounded-full bg-slate-900/80 hover:bg-slate-800 text-[10px] text-slate-300 hover:text-white border border-slate-700/60 flex items-center gap-1 cursor-pointer transition-colors shadow-xs"
          >
            <span>🎛️</span>
            <span>تخصيص اللمس</span>
          </button>
        )}
      </div>

      {/* Right Cluster: PS1 Action Buttons (△, □, ✕, ◯) + R1/R2 */}
      <div 
        className="flex flex-col items-end gap-3 pointer-events-auto origin-bottom-right transition-transform duration-150"
        style={{
          transform: `scale(${config.scale})`,
          marginRight: `${config.actionsPos.x - 20}px`,
          marginBottom: `${config.actionsPos.y - 20}px`,
        }}
      >
        {/* Shoulder Buttons */}
        <div className="flex gap-2 mb-1">
          <button
            id="touch-btn-r1"
            onTouchStart={() => handleTouchStart(PS1Button.R1)}
            onTouchEnd={() => handleTouchEnd(PS1Button.R1)}
            onMouseDown={() => handleTouchStart(PS1Button.R1)}
            onMouseUp={() => handleTouchEnd(PS1Button.R1)}
            className={`w-14 h-8 rounded-lg bg-slate-900/70 border border-slate-700 text-xs font-bold font-mono-retro text-slate-300 active:bg-slate-700 ${
              activeButtons.has(PS1Button.R1) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            R1
          </button>
          <button
            id="touch-btn-r2"
            onTouchStart={() => handleTouchStart(PS1Button.R2)}
            onTouchEnd={() => handleTouchEnd(PS1Button.R2)}
            onMouseDown={() => handleTouchStart(PS1Button.R2)}
            onMouseUp={() => handleTouchEnd(PS1Button.R2)}
            className={`w-14 h-8 rounded-lg bg-slate-900/70 border border-slate-700 text-xs font-bold font-mono-retro text-slate-300 active:bg-slate-700 ${
              activeButtons.has(PS1Button.R2) ? 'bg-slate-600 scale-95' : ''
            }`}
          >
            R2
          </button>
        </div>

        {/* Diamond PS1 Buttons */}
        <div className="relative w-36 h-36 bg-slate-950/40 rounded-full border border-slate-800/80 p-2 backdrop-blur-xs flex items-center justify-center">
          {/* TRIANGLE (Top - Green) */}
          <button
            id="touch-ps1-triangle"
            onTouchStart={() => handleTouchStart(PS1Button.TRIANGLE)}
            onTouchEnd={() => handleTouchEnd(PS1Button.TRIANGLE)}
            onMouseDown={() => handleTouchStart(PS1Button.TRIANGLE)}
            onMouseUp={() => handleTouchEnd(PS1Button.TRIANGLE)}
            className={`absolute top-1.5 w-11 h-11 bg-slate-800/90 active:bg-emerald-900 rounded-full border border-emerald-500/50 flex items-center justify-center text-emerald-400 font-bold text-base shadow-lg ${
              activeButtons.has(PS1Button.TRIANGLE) ? 'bg-emerald-800 scale-95' : ''
            }`}
          >
            △
          </button>

          {/* SQUARE (Left - Pink/Rose) */}
          <button
            id="touch-ps1-square"
            onTouchStart={() => handleTouchStart(PS1Button.SQUARE)}
            onTouchEnd={() => handleTouchEnd(PS1Button.SQUARE)}
            onMouseDown={() => handleTouchStart(PS1Button.SQUARE)}
            onMouseUp={() => handleTouchEnd(PS1Button.SQUARE)}
            className={`absolute left-1.5 w-11 h-11 bg-slate-800/90 active:bg-pink-900 rounded-full border border-pink-500/50 flex items-center justify-center text-pink-400 font-bold text-base shadow-lg ${
              activeButtons.has(PS1Button.SQUARE) ? 'bg-pink-800 scale-95' : ''
            }`}
          >
            □
          </button>

          {/* CIRCLE (Right - Red) */}
          <button
            id="touch-ps1-circle"
            onTouchStart={() => handleTouchStart(PS1Button.CIRCLE)}
            onTouchEnd={() => handleTouchEnd(PS1Button.CIRCLE)}
            onMouseDown={() => handleTouchStart(PS1Button.CIRCLE)}
            onMouseUp={() => handleTouchEnd(PS1Button.CIRCLE)}
            className={`absolute right-1.5 w-11 h-11 bg-slate-800/90 active:bg-red-900 rounded-full border border-red-500/50 flex items-center justify-center text-red-400 font-bold text-base shadow-lg ${
              activeButtons.has(PS1Button.CIRCLE) ? 'bg-red-800 scale-95' : ''
            }`}
          >
            ○
          </button>

          {/* CROSS (Bottom - Blue) */}
          <button
            id="touch-ps1-cross"
            onTouchStart={() => handleTouchStart(PS1Button.CROSS)}
            onTouchEnd={() => handleTouchEnd(PS1Button.CROSS)}
            onMouseDown={() => handleTouchStart(PS1Button.CROSS)}
            onMouseUp={() => handleTouchEnd(PS1Button.CROSS)}
            className={`absolute bottom-1.5 w-11 h-11 bg-slate-800/90 active:bg-sky-900 rounded-full border border-sky-500/50 flex items-center justify-center text-sky-400 font-bold text-base shadow-lg ${
              activeButtons.has(PS1Button.CROSS) ? 'bg-sky-800 scale-95' : ''
            }`}
          >
            ✕
          </button>
        </div>
      </div>
    </div>
  );
};
