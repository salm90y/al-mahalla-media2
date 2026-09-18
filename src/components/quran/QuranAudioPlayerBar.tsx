import React, { useState, useEffect, useRef } from 'react';
import { 
  Play, Pause, SkipBack, SkipForward, Repeat, RotateCcw, 
  Volume2, VolumeX, Sparkles, X, ChevronUp, ChevronDown 
} from 'lucide-react';
import { QuranReader, QuranSurah } from './types';
import { getReciterAudioUrl } from './recitersData';

interface QuranAudioPlayerBarProps {
  currentSurah: QuranSurah | null;
  currentReader: QuranReader;
  currentAyahNumber?: number;
  isPlaying: boolean;
  onTogglePlay: () => void;
  onNextSurah?: () => void;
  onPrevSurah?: () => void;
  onClose?: () => void;
  onAyahChange?: (ayahNum: number) => void;
}

export const QuranAudioPlayerBar: React.FC<QuranAudioPlayerBarProps> = ({
  currentSurah,
  currentReader,
  currentAyahNumber = 1,
  isPlaying,
  onTogglePlay,
  onNextSurah,
  onPrevSurah,
  onClose,
  onAyahChange
}) => {
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(180);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0);
  const [repeatMode, setRepeatMode] = useState<'none' | 'ayah' | 'surah'>('surah');
  const [isMuted, setIsMuted] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const audioSrc = currentSurah 
    ? getReciterAudioUrl(currentReader.id, currentSurah.id) 
    : '';

  useEffect(() => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.play().catch(() => {
        // Autoplay policy fallback: simulate progression
      });
    } else {
      audioRef.current.pause();
    }
  }, [isPlaying, audioSrc]);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.playbackRate = playbackSpeed;
    }
  }, [playbackSpeed]);

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
      if (audioRef.current.duration && !isNaN(audioRef.current.duration)) {
        setDuration(audioRef.current.duration);
      }
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setCurrentTime(val);
    if (audioRef.current) {
      audioRef.current.currentTime = val;
    }
  };

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const mins = Math.floor(secs / 60);
    const remainingSecs = Math.floor(secs % 60);
    return `${String(mins).padStart(2, '0')}:${String(remainingSecs).padStart(2, '0')}`;
  };

  const cycleSpeed = () => {
    const speeds = [0.75, 1.0, 1.25, 1.5];
    const nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.length;
    setPlaybackSpeed(speeds[nextIdx]);
  };

  const cycleRepeat = () => {
    const modes: ('none' | 'ayah' | 'surah')[] = ['none', 'ayah', 'surah'];
    const nextIdx = (modes.indexOf(repeatMode) + 1) % modes.length;
    setRepeatMode(modes[nextIdx]);
  };

  if (!currentSurah) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-[#0C1222]/95 backdrop-blur-md border-t border-[#1F2E4D] shadow-2xl z-40 text-white select-none transition-all duration-300" dir="rtl">
      {/* Hidden audio element */}
      <audio
        ref={audioRef}
        src={audioSrc}
        onTimeUpdate={handleTimeUpdate}
        onEnded={() => {
          if (repeatMode === 'surah') {
            if (audioRef.current) {
              audioRef.current.currentTime = 0;
              audioRef.current.play();
            }
          } else if (onNextSurah) {
            onNextSurah();
          }
        }}
        muted={isMuted}
      />

      {/* Progress Bar Top Edge */}
      <div className="relative w-full h-1 bg-[#1A2640] cursor-pointer group">
        <div 
          className="h-full bg-gradient-to-r from-[#10B981] to-[#F59E0B] transition-all"
          style={{ width: `${(currentTime / (duration || 1)) * 100}%` }}
        />
        <input 
          type="range" 
          min={0} 
          max={duration || 100} 
          value={currentTime} 
          onChange={handleSeek}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
        />
      </div>

      <div className="px-3 py-2 flex items-center justify-between gap-2 max-w-2xl mx-auto">
        {/* Reciter & Surah Info */}
        <div className="flex items-center gap-2.5 min-w-0">
          <img 
            src={currentReader.imageUrl} 
            alt={currentReader.name} 
            className="w-10 h-10 rounded-xl object-cover border border-[#10B981]/40 shadow flex-shrink-0"
          />
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <span className="text-[13px] font-bold text-white truncate">سورة {currentSurah.name}</span>
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#10B981]/20 text-[#10B981] font-medium border border-[#10B981]/30">
                الآية {currentAyahNumber}
              </span>
            </div>
            <div className="text-[11px] text-[#94A3B8] truncate flex items-center gap-1">
              <span>🎙️ {currentReader.name}</span>
              <span className="text-[9px] text-[#64748B]">({currentReader.style})</span>
            </div>
          </div>
        </div>

        {/* Center Controls */}
        <div className="flex items-center gap-1 sm:gap-2 flex-shrink-0">
          {onPrevSurah && (
            <button 
              onClick={onPrevSurah}
              className="p-1.5 hover:bg-[#1A2640] rounded-lg text-slate-300 hover:text-white transition"
              title="السورة السابقة"
            >
              <SkipForward className="w-4 h-4" />
            </button>
          )}

          <button 
            onClick={onTogglePlay}
            className="w-9 h-9 rounded-full bg-gradient-to-r from-[#10B981] to-[#059669] hover:from-[#059669] hover:to-[#047857] text-white flex items-center justify-center shadow-lg shadow-[#10B981]/25 transition transform active:scale-95"
            title={isPlaying ? 'إيقاف مؤقت' : 'تشغيل التلاوة'}
          >
            {isPlaying ? <Pause className="w-4 h-4 fill-white" /> : <Play className="w-4 h-4 fill-white ml-0.5" />}
          </button>

          {onNextSurah && (
            <button 
              onClick={onNextSurah}
              className="p-1.5 hover:bg-[#1A2640] rounded-lg text-slate-300 hover:text-white transition"
              title="السورة التالية"
            >
              <SkipBack className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Right Settings */}
        <div className="flex items-center gap-1 flex-shrink-0">
          <button 
            onClick={cycleSpeed}
            className="text-[10.5px] px-1.5 py-1 rounded-md bg-[#162238] border border-[#233554] text-[#F59E0B] font-bold hover:bg-[#1F2F4C] transition"
            title="سرعة التلاوة"
          >
            {playbackSpeed}x
          </button>

          <button 
            onClick={cycleRepeat}
            className={`p-1.5 rounded-md transition ${repeatMode !== 'none' ? 'text-[#10B981] bg-[#10B981]/15' : 'text-slate-400 hover:text-white'}`}
            title={`التكرار: ${repeatMode === 'none' ? 'معطل' : repeatMode === 'ayah' ? 'تكرار الآية' : 'تكرار السورة'}`}
          >
            <Repeat className="w-3.5 h-3.5" />
          </button>

          <button 
            onClick={() => setIsMuted(!isMuted)}
            className="p-1.5 text-slate-400 hover:text-white rounded-md transition"
            title={isMuted ? 'إلغاء الكتم' : 'كتم الصوت'}
          >
            {isMuted ? <VolumeX className="w-3.5 h-3.5 text-red-400" /> : <Volume2 className="w-3.5 h-3.5" />}
          </button>

          {onClose && (
            <button 
              onClick={onClose}
              className="p-1 text-slate-400 hover:text-white rounded-md transition"
              title="إغلاق المشغل"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
