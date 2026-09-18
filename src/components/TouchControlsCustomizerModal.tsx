import React, { useState, useEffect } from 'react';
import { TouchControlsConfig } from '../types';
import { touchConfigManager, DEFAULT_TOUCH_CONFIG, PRESET_CONFIGS } from '../services/touchConfigStorage';
import { soundFx } from '../services/audioSynthesizer';

interface TouchControlsCustomizerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const TouchControlsCustomizerModal: React.FC<TouchControlsCustomizerModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [config, setConfig] = useState<TouchControlsConfig>(DEFAULT_TOUCH_CONFIG);
  const [selectedPreset, setSelectedPreset] = useState<string>('phone_standard');
  const [testHapticSuccess, setTestHapticSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setConfig(touchConfigManager.get());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleUpdate = (partial: Partial<TouchControlsConfig>) => {
    const updated = { ...config, ...partial };
    setConfig(updated);
    touchConfigManager.save(updated);
  };

  const handleApplyPreset = (presetKey: string) => {
    soundFx.playUiBlip(750);
    setSelectedPreset(presetKey);
    touchConfigManager.applyPreset(presetKey);
    setConfig(touchConfigManager.get());
  };

  const handleReset = () => {
    soundFx.playUiBlip(550);
    touchConfigManager.reset();
    setConfig(touchConfigManager.get());
    setSelectedPreset('phone_standard');
  };

  const handleTestHaptic = () => {
    soundFx.playUiBlip(880);
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(config.hapticDuration);
      setTestHapticSuccess(true);
      setTimeout(() => setTestHapticSuccess(false), 1200);
    } else {
      setTestHapticSuccess(true);
      setTimeout(() => setTestHapticSuccess(false), 1200);
    }
  };

  return (
    <div 
      id="touch-controls-customizer-modal"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-900/60 backdrop-blur-sm animate-fade-in select-none"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-3xl max-h-[92vh] bg-white border border-slate-200 rounded-2xl shadow-xl flex flex-col overflow-hidden text-slate-800"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 bg-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-white shadow-sm text-sm shrink-0">
              HUD
            </div>
            <div>
              <h3 className="font-bold text-base sm:text-lg text-slate-900 flex items-center gap-2">
                <span>تخصيص أزرار التحكم اللمسية (Virtual Controller HUD)</span>
                <span className="text-xs bg-blue-50 text-blue-700 px-2.5 py-0.5 rounded-full border border-blue-200 font-semibold">
                  Touch Layout
                </span>
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                تعديل الحجم، الشفافية، مواقع الأزرار، والاهتزاز اللمسي (Haptic Feedback)
              </p>
            </div>
          </div>

          <button
            onClick={() => {
              soundFx.playUiBlip(600);
              onClose();
            }}
            className="w-8 h-8 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-900 flex items-center justify-center font-bold text-sm cursor-pointer transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-6">
          
          {/* 1. Quick Presets */}
          <div>
            <div className="flex items-center justify-between mb-2.5">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
                ⚡ الأوضاع الجاهزة (Quick Presets)
              </label>
              <span className="text-xs text-slate-400">اختر قالباً جاهزاً بلمسة واحدة</span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {Object.entries(PRESET_CONFIGS).map(([key, item]) => {
                const isSelected = selectedPreset === key;
                return (
                  <button
                    key={key}
                    onClick={() => handleApplyPreset(key)}
                    className={`p-3 rounded-xl border text-left flex flex-col gap-1 transition-all cursor-pointer ${
                      isSelected
                        ? 'border-blue-600 bg-blue-50/70 text-blue-900 shadow-xs'
                        : 'border-slate-200 bg-slate-50 hover:bg-slate-100/70 text-slate-700'
                    }`}
                  >
                    <span className="text-xs font-bold">{item.label.split('(')[0]}</span>
                    <span className="text-[10px] text-slate-400 font-mono">
                      {key === 'tablet_wide' ? 'Scale 115% • Wide' :
                       key === 'compact' ? 'Scale 85% • Low Alpha' :
                       key === 'action_heavy' ? 'Scale 120% • Action' : 'Standard 100%'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 2. Interactive Preview Canvas */}
          <div className="rounded-2xl border border-slate-200 bg-[#0F172A] p-4 relative overflow-hidden h-44 flex flex-col justify-between select-none shadow-inner">
            <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono">
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>معاينة حية لشاشة اللعبة (Live Preview Canvas)</span>
              </div>
              <span>Scale: {Math.round(config.scale * 100)}% • Alpha: {Math.round(config.opacity * 100)}%</span>
            </div>

            {/* Simulated overlay buttons inside canvas */}
            <div 
              className="flex justify-between items-end w-full px-2 pointer-events-none transition-all duration-200"
              style={{ opacity: config.opacity }}
            >
              {/* Left Mini D-Pad */}
              <div 
                className="origin-bottom-left transition-transform duration-100"
                style={{
                  transform: `scale(${config.scale * 0.8})`,
                  marginLeft: `${config.dpadPos.x * 0.4}px`,
                  marginBottom: `${config.dpadPos.y * 0.4}px`,
                }}
              >
                <div className="w-16 h-16 rounded-full bg-slate-800/90 border border-slate-600 flex items-center justify-center relative">
                  <span className="absolute top-1 text-[10px] text-slate-300 font-bold">▲</span>
                  <span className="absolute bottom-1 text-[10px] text-slate-300 font-bold">▼</span>
                  <span className="absolute left-1 text-[10px] text-slate-300 font-bold">◀</span>
                  <span className="absolute right-1 text-[10px] text-slate-300 font-bold">▶</span>
                  <div className="w-4 h-4 rounded-full bg-slate-900 border border-slate-700" />
                </div>
              </div>

              {/* Center Mini Select/Start */}
              <div 
                className="flex flex-col items-center gap-1 origin-bottom transition-transform duration-100"
                style={{
                  transform: `scale(${config.scale * 0.8}) translate(${config.centerPos.x * 0.4}px, ${-config.centerPos.y * 0.4}px)`,
                }}
              >
                <div className="flex gap-2">
                  <div className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-[8px] font-mono text-slate-400">
                    SELECT
                  </div>
                  <div className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-[8px] font-mono text-slate-400">
                    START
                  </div>
                </div>
              </div>

              {/* Right Mini Action Buttons */}
              <div 
                className="origin-bottom-right transition-transform duration-100"
                style={{
                  transform: `scale(${config.scale * 0.8})`,
                  marginRight: `${config.actionsPos.x * 0.4}px`,
                  marginBottom: `${config.actionsPos.y * 0.4}px`,
                }}
              >
                <div className="w-16 h-16 rounded-full bg-slate-800/90 border border-slate-600 flex items-center justify-center relative">
                  <span className="absolute top-1 text-[11px] text-emerald-400 font-bold">△</span>
                  <span className="absolute bottom-1 text-[11px] text-blue-400 font-bold">✕</span>
                  <span className="absolute left-1 text-[11px] text-pink-400 font-bold">□</span>
                  <span className="absolute right-1 text-[11px] text-red-400 font-bold">○</span>
                </div>
              </div>
            </div>

            <div className="text-center text-[10px] text-slate-500 font-mono">
              تتحرك الأزرار وتتجاوب فورياً في شاشة اللعبة عند تحريك أشرطة التحكم أدناه
            </div>
          </div>

          {/* 3. Sliders Controls Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            
            {/* Scale Slider */}
            <div className="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-2">
              <div className="flex justify-between items-center text-xs font-semibold text-slate-800">
                <span>🔍 مقياس حجم الأزرار (Button Size Scale)</span>
                <span className="font-mono text-blue-600 font-bold">{Math.round(config.scale * 100)}%</span>
              </div>
              <input
                type="range"
                min="0.7"
                max="1.4"
                step="0.05"
                value={config.scale}
                onChange={(e) => handleUpdate({ scale: parseFloat(e.target.value) })}
                className="w-full accent-blue-600 cursor-pointer h-2 bg-slate-200 rounded-lg"
              />
              <div className="flex justify-between text-[10px] text-slate-400 font-mono">
                <span>70% أصغر</span>
                <span>100% قياسي</span>
                <span>140% أقصى حجم</span>
              </div>
            </div>

            {/* Opacity Slider */}
            <div className="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-2">
              <div className="flex justify-between items-center text-xs font-semibold text-slate-800">
                <span>👁️ مستوى الشفافية (Transparency / Opacity)</span>
                <span className="font-mono text-blue-600 font-bold">{Math.round(config.opacity * 100)}%</span>
              </div>
              <input
                type="range"
                min="0.2"
                max="1.0"
                step="0.05"
                value={config.opacity}
                onChange={(e) => handleUpdate({ opacity: parseFloat(e.target.value) })}
                className="w-full accent-blue-600 cursor-pointer h-2 bg-slate-200 rounded-lg"
              />
              <div className="flex justify-between text-[10px] text-slate-400 font-mono">
                <span>20% شفاف جداً</span>
                <span>60% متوازن</span>
                <span>100% معتم كلياً</span>
              </div>
            </div>

            {/* D-Pad Position Sliders */}
            <div className="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex justify-between items-center text-xs font-semibold text-slate-800">
                <span>🕹️ موضع أزرار الاتجاهات (D-Pad Placement)</span>
                <span className="font-mono text-blue-600 text-[11px]">X: {config.dpadPos.x}px | Y: {config.dpadPos.y}px</span>
              </div>
              <div className="space-y-1.5">
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>المسافة من الحافة اليسرى (X):</span>
                  <span className="font-mono font-bold">{config.dpadPos.x}px</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="80"
                  step="2"
                  value={config.dpadPos.x}
                  onChange={(e) => handleUpdate({ dpadPos: { ...config.dpadPos, x: parseInt(e.target.value) } })}
                  className="w-full accent-blue-600 cursor-pointer h-1.5 bg-slate-200 rounded-lg"
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>الارتفاع من الأسفل (Y):</span>
                  <span className="font-mono font-bold">{config.dpadPos.y}px</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="80"
                  step="2"
                  value={config.dpadPos.y}
                  onChange={(e) => handleUpdate({ dpadPos: { ...config.dpadPos, y: parseInt(e.target.value) } })}
                  className="w-full accent-blue-600 cursor-pointer h-1.5 bg-slate-200 rounded-lg"
                />
              </div>
            </div>

            {/* Action Buttons Position Sliders */}
            <div className="p-4 rounded-xl border border-slate-200 bg-slate-50 space-y-3">
              <div className="flex justify-between items-center text-xs font-semibold text-slate-800">
                <span>🎮 موضع أزرار الأكشن (Action Buttons Placement)</span>
                <span className="font-mono text-blue-600 text-[11px]">X: {config.actionsPos.x}px | Y: {config.actionsPos.y}px</span>
              </div>
              <div className="space-y-1.5">
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>المسافة من الحافة اليمنى (X):</span>
                  <span className="font-mono font-bold">{config.actionsPos.x}px</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="80"
                  step="2"
                  value={config.actionsPos.x}
                  onChange={(e) => handleUpdate({ actionsPos: { ...config.actionsPos, x: parseInt(e.target.value) } })}
                  className="w-full accent-blue-600 cursor-pointer h-1.5 bg-slate-200 rounded-lg"
                />
              </div>
              <div className="space-y-1.5">
                <div className="flex justify-between text-[11px] text-slate-500">
                  <span>الارتفاع من الأسفل (Y):</span>
                  <span className="font-mono font-bold">{config.actionsPos.y}px</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="80"
                  step="2"
                  value={config.actionsPos.y}
                  onChange={(e) => handleUpdate({ actionsPos: { ...config.actionsPos, y: parseInt(e.target.value) } })}
                  className="w-full accent-blue-600 cursor-pointer h-1.5 bg-slate-200 rounded-lg"
                />
              </div>
            </div>

          </div>

          {/* 4. Haptic Feedback & Vibrations */}
          <div className="p-4 rounded-xl border border-slate-200 bg-slate-50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="text-xs font-bold text-slate-900 flex items-center gap-2">
                <span>📳 التغذية الاهتزازية اللمسية (Haptic Vibration)</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 font-mono font-semibold">
                  {config.hapticEnabled ? 'مفعّل ON' : 'معطّل OFF'}
                </span>
              </div>
              <p className="text-xs text-slate-500">
                إرسال نبضة اهتزازية لمسية ناعمة للهاتف عند كل ضغطة زر لتجربة تحكم فيزيائية واقعية
              </p>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <button
                onClick={() => handleUpdate({ hapticEnabled: !config.hapticEnabled })}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold cursor-pointer transition-all ${
                  config.hapticEnabled 
                    ? 'bg-blue-600 text-white shadow-xs' 
                    : 'bg-slate-200 text-slate-700 hover:bg-slate-300'
                }`}
              >
                {config.hapticEnabled ? 'تعطيل الاهتزاز' : 'تفعيل الاهتزاز'}
              </button>

              <button
                onClick={handleTestHaptic}
                className="px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-white border border-slate-300 hover:bg-slate-100 text-slate-700 cursor-pointer transition-colors flex items-center gap-1.5"
              >
                <span>⚡</span>
                <span>{testHapticSuccess ? 'تم اختبار النبضة!' : 'اختبار النبضة'}</span>
              </button>
            </div>
          </div>

        </div>

        {/* Footer Actions */}
        <div className="px-6 py-4 border-t border-slate-200 bg-slate-50 flex flex-col sm:flex-row justify-between items-center gap-3">
          <button
            onClick={handleReset}
            className="text-xs text-slate-500 hover:text-rose-600 font-semibold cursor-pointer transition-colors flex items-center gap-1"
          >
            <span>🔄</span>
            <span>استعادة الإعدادات الافتراضية (Reset Default)</span>
          </button>

          <div className="flex items-center gap-2.5">
            <button
              onClick={() => {
                soundFx.playUiBlip(750);
                onClose();
              }}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg text-xs shadow-sm shadow-blue-200 cursor-pointer transition-colors"
            >
              حفظ وتطبيق على شاشة اللعبة ✓
            </button>
          </div>
        </div>

      </div>
    </div>
  );
};
