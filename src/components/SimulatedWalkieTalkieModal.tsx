import React, { useState, useEffect } from 'react';
import { soundFx } from '../services/audioSynthesizer';

interface SimulatedWalkieTalkieModalProps {
  isOpen: boolean;
  onClose: () => void;
  roomCode: string;
}

export const SimulatedWalkieTalkieModal: React.FC<SimulatedWalkieTalkieModalProps> = ({
  isOpen,
  onClose,
  roomCode
}) => {
  const [isTalking, setIsTalking] = useState(false);
  const [vuLevel, setVuLevel] = useState(25);
  const [volume, setVolume] = useState(85);
  const [micGain, setMicGain] = useState(70);

  // Animated VU level when talking
  useEffect(() => {
    if (!isTalking) {
      setVuLevel(10);
      return;
    }
    const interval = setInterval(() => {
      setVuLevel(Math.floor(40 + Math.random() * 55));
    }, 120);
    return () => clearInterval(interval);
  }, [isTalking]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div 
        id="simulated-walkie-talkie-card"
        className="w-full max-w-md bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl p-5 text-right overflow-hidden"
        dir="rtl"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <span className="text-2xl">📻</span>
            <div>
              <h3 className="text-sm font-bold text-white">اتصال هوكي توكي (Walkie-Talkie)</h3>
              <p className="text-[11px] text-slate-400">قناة صوتية فورية • غرفة {roomCode}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center text-xs cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Live Audio Spectrum Equalizer */}
        <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 mb-4 flex flex-col items-center justify-center">
          <div className="flex items-center gap-1.5 h-12 mb-2">
            {[35, 60, 85, 45, 90, 70, 40, 80, 50, 65].map((baseHeight, idx) => {
              const activeHeight = isTalking ? Math.min(100, (baseHeight * vuLevel) / 60) : 15;
              return (
                <div
                  key={idx}
                  style={{ height: `${activeHeight}%` }}
                  className={`w-2.5 rounded-full transition-all duration-100 ${
                    isTalking ? 'bg-emerald-400 shadow-sm shadow-emerald-400' : 'bg-slate-700'
                  }`}
                />
              );
            })}
          </div>

          <p className="text-[11px] font-semibold text-slate-300">
            {isTalking ? (
              <span className="text-rose-400 font-bold flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping inline-block" />
                الميكروفون يبث الصوت الآن إلى اللاعبين...
              </span>
            ) : (
              <span className="text-emerald-400">
                القناة الصوتية جاهزة • في وضع الاستماع
              </span>
            )}
          </p>
        </div>

        {/* Big Push-To-Talk Button */}
        <button
          onClick={() => {
            soundFx.playUiBlip(isTalking ? 400 : 700);
            setIsTalking(prev => !prev);
          }}
          className={`w-full py-4 rounded-2xl font-bold text-sm transition-all shadow-lg cursor-pointer flex items-center justify-center gap-2 mb-4 ${
            isTalking
              ? 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-900/50 ring-4 ring-rose-500/20'
              : 'bg-blue-600 hover:bg-blue-700 text-white shadow-blue-900/40 ring-2 ring-blue-400/20'
          }`}
        >
          <span className="text-lg">{isTalking ? '🔴' : '🎙️'}</span>
          <span>{isTalking ? 'انقر لإيقاف البث الصوتي' : 'اضغط للتحدث (Push-to-Talk)'}</span>
        </button>

        {/* Audio Sliders */}
        <div className="space-y-3 bg-slate-950/60 p-3 rounded-xl border border-slate-800 text-xs">
          <div>
            <div className="flex justify-between text-slate-300 mb-1">
              <span>مستوى صوت اللاعبين:</span>
              <span className="text-blue-400 font-bold">{volume}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={volume}
              onChange={(e) => setVolume(Number(e.target.value))}
              className="w-full accent-blue-500 cursor-pointer"
            />
          </div>

          <div>
            <div className="flex justify-between text-slate-300 mb-1">
              <span>حساسية الميكروفون وإلغاء الصدى:</span>
              <span className="text-emerald-400 font-bold">{micGain}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value={micGain}
              onChange={(e) => setMicGain(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
          </div>
        </div>

        <button
          onClick={onClose}
          className="mt-4 w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl cursor-pointer"
        >
          إغلاق النافذة
        </button>
      </div>
    </div>
  );
};
