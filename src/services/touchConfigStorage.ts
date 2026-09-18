import { TouchControlsConfig } from '../types';

const STORAGE_KEY = 'ps1_combat3_touch_config';

export const DEFAULT_TOUCH_CONFIG: TouchControlsConfig = {
  dpadPos: { x: 20, y: 20 },
  actionsPos: { x: 20, y: 20 },
  centerPos: { x: 0, y: 16 },
  scale: 1.0,
  opacity: 0.85,
  hapticEnabled: true,
  hapticDuration: 20,
};

export const PRESET_CONFIGS: Record<string, { label: string; config: TouchControlsConfig }> = {
  phone_standard: {
    label: 'هاتف ذكي قياسي (Standard Phone)',
    config: { ...DEFAULT_TOUCH_CONFIG }
  },
  tablet_wide: {
    label: 'شاشات عريضة وتابلت (Tablet Wide Grip)',
    config: {
      dpadPos: { x: 40, y: 40 },
      actionsPos: { x: 40, y: 40 },
      centerPos: { x: 0, y: 24 },
      scale: 1.15,
      opacity: 0.9,
      hapticEnabled: true,
      hapticDuration: 25,
    }
  },
  compact: {
    label: 'مدمج وشبه شفاف (Compact & Translucent)',
    config: {
      dpadPos: { x: 12, y: 12 },
      actionsPos: { x: 12, y: 12 },
      centerPos: { x: 0, y: 10 },
      scale: 0.85,
      opacity: 0.6,
      hapticEnabled: true,
      hapticDuration: 15,
    }
  },
  action_heavy: {
    label: 'أزرار أكشن مكبرة (Action Heavy)',
    config: {
      dpadPos: { x: 16, y: 16 },
      actionsPos: { x: 28, y: 28 },
      centerPos: { x: 0, y: 16 },
      scale: 1.2,
      opacity: 0.95,
      hapticEnabled: true,
      hapticDuration: 30,
    }
  }
};

class TouchConfigManager {
  private config: TouchControlsConfig;
  private listeners: Array<(cfg: TouchControlsConfig) => void> = [];

  constructor() {
    this.config = this.load();
  }

  public get(): TouchControlsConfig {
    return { ...this.config };
  }

  public save(newConfig: Partial<TouchControlsConfig>) {
    this.config = { ...this.config, ...newConfig };
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.config));
    } catch {
      // Ignore storage errors
    }
    this.notify();
  }

  public reset() {
    this.config = { ...DEFAULT_TOUCH_CONFIG };
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignore
    }
    this.notify();
  }

  public applyPreset(presetKey: string) {
    if (PRESET_CONFIGS[presetKey]) {
      this.save(PRESET_CONFIGS[presetKey].config);
    }
  }

  public subscribe(cb: (cfg: TouchControlsConfig) => void) {
    this.listeners.push(cb);
    cb(this.config);
    return () => {
      this.listeners = this.listeners.filter(l => l !== cb);
    };
  }

  private notify() {
    const copy = { ...this.config };
    this.listeners.forEach(l => l(copy));
  }

  private load(): TouchControlsConfig {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        return {
          ...DEFAULT_TOUCH_CONFIG,
          ...parsed,
          dpadPos: { ...DEFAULT_TOUCH_CONFIG.dpadPos, ...(parsed.dpadPos || {}) },
          actionsPos: { ...DEFAULT_TOUCH_CONFIG.actionsPos, ...(parsed.actionsPos || {}) },
          centerPos: { ...DEFAULT_TOUCH_CONFIG.centerPos, ...(parsed.centerPos || {}) },
        };
      }
    } catch {
      // fallback
    }
    return { ...DEFAULT_TOUCH_CONFIG };
  }
}

export const touchConfigManager = new TouchConfigManager();
