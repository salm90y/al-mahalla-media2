import React, { useState, useEffect, useRef } from 'react';
import { 
  Users, Mic, MicOff, Volume2, Play, Pause, SkipForward, SkipBack, 
  Send, Share2, Crown, Sparkles, MessageSquare, List, Check,
  Radio, Hand, ShieldAlert, ArrowRight, BookOpen, Clock
} from 'lucide-react';
import { QuranRoom, QuranReader, QuranSurah, QuranAyah } from './types';
import { getReciterAudioUrl, QURAN_RECITERS } from './recitersData';
import { QURAN_SURAHS } from './quranSurahsData';
import { getSurahVerses } from './quranVersesData';

interface QuranLiveRoomViewProps {
  room: QuranRoom;
  currentUserId?: string;
  currentUserName?: string;
  onLeaveRoom: () => void;
  onUpdateRoom?: (updatedRoom: QuranRoom) => void;
}

export const QuranLiveRoomView: React.FC<QuranLiveRoomViewProps> = ({
  room: initialRoom,
  currentUserId = 'user_me',
  currentUserName = 'أنت (مستمع)',
  onLeaveRoom,
  onUpdateRoom
}) => {
  const [room, setRoom] = useState<QuranRoom>(initialRoom);
  const [activeTab, setActiveTab] = useState<'recitation' | 'chat' | 'participants'>('recitation');
  const [verses, setVerses] = useState<QuranAyah[]>([]);
  const [currentPositionSec, setCurrentPositionSec] = useState<number>(initialRoom.positionMs / 1000);
  const [durationSec, setDurationSec] = useState<number>(300);
  const [copiedLink, setCopiedLink] = useState(false);
  const [chatMessage, setChatMessage] = useState('');
  const [myMicEnabled, setMyMicEnabled] = useState(false);
  const [handRaised, setHandRaised] = useState(false);

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const isHost = room.hostUserId === currentUserId || initialRoom.hostUserId === currentUserId || currentUserId === 'host_101';

  // Load verses for current Surah
  useEffect(() => {
    let isMounted = true;
    getSurahVerses(room.surahId).then(v => {
      if (isMounted) setVerses(v);
    });
    return () => { isMounted = false; };
  }, [room.surahId]);

  // Synchronize audio on join or when room state changes
  useEffect(() => {
    // Calculate elapsed time for synchronized late-joiner
    const elapsedSeconds = room.isPlaying 
      ? Math.max(0, (Date.now() - room.serverTimestamp) / 1000) 
      : 0;
    const targetPosition = (room.positionMs / 1000) + elapsedSeconds;

    if (audioRef.current) {
      if (Math.abs(audioRef.current.currentTime - targetPosition) > 2) {
        audioRef.current.currentTime = targetPosition;
      }
      if (room.isPlaying) {
        audioRef.current.play().catch(() => {});
      } else {
        audioRef.current.pause();
      }
    }
    setCurrentPositionSec(targetPosition);
  }, [room.isPlaying, room.positionMs, room.serverTimestamp, room.surahId, room.readerId]);

  const audioUrl = getReciterAudioUrl(room.readerId, room.surahId);

  // Host Action: Toggle Play/Pause
  const handleHostTogglePlay = () => {
    if (!isHost) return;
    const newIsPlaying = !room.isPlaying;
    const currentMs = audioRef.current ? Math.floor(audioRef.current.currentTime * 1000) : room.positionMs;
    const updated: QuranRoom = {
      ...room,
      isPlaying: newIsPlaying,
      positionMs: currentMs,
      serverTimestamp: Date.now()
    };
    setRoom(updated);
    if (onUpdateRoom) onUpdateRoom(updated);
  };

  // Host Action: Seek
  const handleHostSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!isHost) return;
    const newSec = parseFloat(e.target.value);
    if (audioRef.current) {
      audioRef.current.currentTime = newSec;
    }
    setCurrentPositionSec(newSec);
    const updated: QuranRoom = {
      ...room,
      positionMs: Math.floor(newSec * 1000),
      serverTimestamp: Date.now()
    };
    setRoom(updated);
    if (onUpdateRoom) onUpdateRoom(updated);
  };

  // Host Action: Next/Prev Ayah
  const handleHostAyahJump = (newAyahId: number) => {
    if (!isHost) return;
    const updated: QuranRoom = {
      ...room,
      ayahId: newAyahId,
      serverTimestamp: Date.now()
    };
    setRoom(updated);
    if (onUpdateRoom) onUpdateRoom(updated);
  };

  // Send message in room chat
  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatMessage.trim()) return;
    const newMsg = {
      id: `msg_${Date.now()}`,
      userId: currentUserId,
      userName: currentUserName,
      userAvatar: isHost ? '🕌' : '👤',
      text: chatMessage.trim(),
      timestamp: new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' }),
      isHost: isHost
    };
    const updated = {
      ...room,
      messages: [...room.messages, newMsg]
    };
    setRoom(updated);
    if (onUpdateRoom) onUpdateRoom(updated);
    setChatMessage('');
  };

  const handleCopyInvite = () => {
    const inviteText = `انضم إلى مجلس القرآن الكريم: "${room.title}" - تلاوة القارئ: ${room.readerName} - سورة: ${room.surahName} عبر تطبيق Al-Mahalla (معرف الغرفة: ${room.id})`;
    navigator.clipboard.writeText(inviteText).catch(() => {});
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2500);
  };

  const formatSecs = (s: number) => {
    if (isNaN(s)) return '00:00';
    const m = Math.floor(s / 60);
    const sec = Math.floor(s % 60);
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  };

  return (
    <div className="flex flex-col h-full bg-[#080D1A] text-white select-none overflow-hidden" dir="rtl">
      {/* Hidden synchronized audio element */}
      <audio
        ref={audioRef}
        src={audioUrl}
        onTimeUpdate={() => {
          if (audioRef.current) {
            setCurrentPositionSec(audioRef.current.currentTime);
            if (audioRef.current.duration && !isNaN(audioRef.current.duration)) {
              setDurationSec(audioRef.current.duration);
            }
          }
        }}
        onEnded={() => {
          if (isHost) {
            const nextSurah = (room.surahId % 114) + 1;
            const updated: QuranRoom = {
              ...room,
              surahId: nextSurah,
              surahName: QURAN_SURAHS.find(s => s.id === nextSurah)?.name || '',
              positionMs: 0,
              serverTimestamp: Date.now()
            };
            setRoom(updated);
            if (onUpdateRoom) onUpdateRoom(updated);
          }
        }}
      />

      {/* Top Bar with Live Indicator & Exit */}
      <div className="bg-[#0F1629] border-b border-[#1F2C4C] px-3 py-2.5 flex items-center justify-between z-20">
        <div className="flex items-center gap-2">
          <button 
            onClick={onLeaveRoom}
            className="p-1.5 hover:bg-[#1C2640] rounded-xl text-slate-300 hover:text-white transition"
            title="مغادرة الغرفة"
          >
            <ArrowRight className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              <span className="text-[11px] font-bold text-emerald-400 tracking-wide">بث قرآني مباشر</span>
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#1A253D] text-[#94A3B8] border border-[#273859]">
                {room.mode === 'listen_only' ? 'استماع فقط' : 'مجلس صوتي'}
              </span>
            </div>
            <h2 className="text-[13px] font-bold text-white truncate max-w-[200px]">{room.title}</h2>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button 
            onClick={handleCopyInvite}
            className="flex items-center gap-1 px-2 py-1 rounded-xl bg-[#162238] border border-[#243554] text-[11px] font-medium text-amber-300 hover:bg-[#1E2E4B] transition"
            title="مشاركة رابط الغرفة"
          >
            {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Share2 className="w-3.5 h-3.5" />}
            <span>{copiedLink ? 'تم النسخ!' : 'دعوة'}</span>
          </button>

          <div className="flex items-center gap-1 px-2 py-1 rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 text-[11px] font-bold">
            <Users className="w-3.5 h-3.5" />
            <span>{room.listenersCount}</span>
          </div>
        </div>
      </div>

      {/* Recitation Info & Synchronized Playback Hub */}
      <div className="bg-gradient-to-b from-[#121A30] to-[#0A1020] p-3 border-b border-[#1E2C4A]">
        <div className="flex items-center justify-between gap-3 mb-2">
          <div className="flex items-center gap-2.5">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#10B981] to-[#047857] flex items-center justify-center shadow-lg shadow-emerald-950/40 text-2xl border border-emerald-400/30">
              📖
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-black text-white">سورة {room.surahName}</span>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
                  الآية {room.ayahId}
                </span>
              </div>
              <div className="text-[11.5px] text-[#A0AEC0] flex items-center gap-1 mt-0.5">
                <span>🎙️ بصوت القارئ:</span>
                <span className="font-bold text-emerald-400">{room.readerName}</span>
              </div>
            </div>
          </div>

          {/* Sync Status Badge */}
          <div className="flex flex-col items-end">
            <div className="flex items-center gap-1 text-[10.5px] text-emerald-400 font-bold bg-emerald-950/60 px-2 py-1 rounded-lg border border-emerald-500/20">
              <Radio className="w-3 h-3 animate-pulse" />
              <span>متزامن مع الجميع</span>
            </div>
            <span className="text-[10px] text-[#64748B] mt-0.5">
              {isHost ? 'أنت تدير التلاوة' : `المدير: ${room.hostUserName.split(' ')[0]}`}
            </span>
          </div>
        </div>

        {/* Audio Seek Timeline */}
        <div className="space-y-1">
          <div className="flex justify-between text-[10px] text-[#64748B] font-mono">
            <span>{formatSecs(currentPositionSec)}</span>
            <span>{formatSecs(durationSec)}</span>
          </div>
          <div className="relative w-full h-1.5 bg-[#172238] rounded-full overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-emerald-500 to-amber-400 transition-all duration-300"
              style={{ width: `${(currentPositionSec / (durationSec || 1)) * 100}%` }}
            />
            {isHost && (
              <input 
                type="range" 
                min={0} 
                max={durationSec || 100} 
                value={currentPositionSec} 
                onChange={handleHostSeek}
                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
            )}
          </div>
        </div>

        {/* Host Playback Controls (Disabled for listeners to prevent desync) */}
        <div className="mt-2.5 flex items-center justify-between pt-1">
          <div className="text-[11px] text-slate-400 flex items-center gap-1">
            {isHost ? (
              <span className="text-amber-400 font-medium">👑 تحكم المدير بالتلاوة متاح</span>
            ) : (
              <span className="text-slate-400">التلاوة تدار بواسطة مدير الغرفة</span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {isHost ? (
              <>
                <button 
                  onClick={() => {
                    const newAyah = Math.max(1, room.ayahId - 1);
                    handleHostAyahJump(newAyah);
                  }}
                  className="p-1.5 rounded-lg bg-[#18243C] hover:bg-[#223354] text-slate-300 text-xs transition"
                  title="الآية السابقة"
                >
                  <SkipForward className="w-4 h-4" />
                </button>
                <button 
                  onClick={handleHostTogglePlay}
                  className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 text-white font-bold text-xs flex items-center gap-1.5 shadow-md shadow-emerald-950/40 transition active:scale-95"
                >
                  {room.isPlaying ? <Pause className="w-3.5 h-3.5 fill-white" /> : <Play className="w-3.5 h-3.5 fill-white" />}
                  <span>{room.isPlaying ? 'إيقاف للكل' : 'تشغيل للكل'}</span>
                </button>
                <button 
                  onClick={() => {
                    const newAyah = room.ayahId + 1;
                    handleHostAyahJump(newAyah);
                  }}
                  className="p-1.5 rounded-lg bg-[#18243C] hover:bg-[#223354] text-slate-300 text-xs transition"
                  title="الآية التالية"
                >
                  <SkipBack className="w-4 h-4" />
                </button>
              </>
            ) : (
              <div className="flex items-center gap-1.5 text-xs text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-lg border border-emerald-500/20">
                <Volume2 className="w-3.5 h-3.5" />
                <span>استماع مباشر</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Tabs Switcher: Recitation Sync vs Chat vs Participants */}
      <div className="flex border-b border-[#1A2640] bg-[#0C1322] px-2">
        <button 
          onClick={() => setActiveTab('recitation')}
          className={`flex-1 py-2 text-[12px] font-bold flex items-center justify-center gap-1.5 border-b-2 transition ${activeTab === 'recitation' ? 'text-emerald-400 border-emerald-400 bg-[#141F36]/50' : 'text-slate-400 border-transparent hover:text-slate-200'}`}
        >
          <BookOpen className="w-3.5 h-3.5" />
          <span>الآيات المتزامنة</span>
        </button>

        <button 
          onClick={() => setActiveTab('chat')}
          className={`flex-1 py-2 text-[12px] font-bold flex items-center justify-center gap-1.5 border-b-2 transition ${activeTab === 'chat' ? 'text-emerald-400 border-emerald-400 bg-[#141F36]/50' : 'text-slate-400 border-transparent hover:text-slate-200'}`}
        >
          <MessageSquare className="w-3.5 h-3.5" />
          <span>دردشة المجلس ({room.messages.length})</span>
        </button>

        <button 
          onClick={() => setActiveTab('participants')}
          className={`flex-1 py-2 text-[12px] font-bold flex items-center justify-center gap-1.5 border-b-2 transition ${activeTab === 'participants' ? 'text-emerald-400 border-emerald-400 bg-[#141F36]/50' : 'text-slate-400 border-transparent hover:text-slate-200'}`}
        >
          <Users className="w-3.5 h-3.5" />
          <span>المتواجدون ({room.participants.length})</span>
        </button>
      </div>

      {/* Main Tab Content */}
      <div className="flex-1 overflow-y-auto p-3 space-y-3">
        {activeTab === 'recitation' && (
          <div className="space-y-3">
            <div className="bg-[#121B30] border border-[#202E50] rounded-2xl p-3 text-center">
              <span className="text-[11px] text-amber-400 font-bold block mb-1">بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ</span>
              <p className="text-[11.5px] text-slate-300">
                يتم تمييز وتحديد الآية الجاري الاستماع إليها تلقائياً لجميع الحاضرين في نفس اللحظة.
              </p>
            </div>

            {/* Verses List with Synchronized Highlight */}
            <div className="space-y-2.5">
              {verses.map(ayah => {
                const isActive = ayah.number === room.ayahId;
                return (
                  <div 
                    key={ayah.number}
                    id={`room-ayah-${ayah.number}`}
                    onClick={() => isHost && handleHostAyahJump(ayah.number)}
                    className={`p-3 rounded-2xl transition-all duration-300 border ${
                      isActive 
                        ? 'bg-gradient-to-r from-emerald-950/80 via-[#132B25] to-emerald-950/80 border-emerald-500 shadow-lg shadow-emerald-950/50 scale-[1.01]' 
                        : 'bg-[#0E1526] border-[#1C2947] hover:border-[#2C3E67]'
                    } ${isHost ? 'cursor-pointer' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-1.5">
                        <span className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold border ${
                          isActive 
                            ? 'bg-emerald-500 text-slate-950 border-emerald-300' 
                            : 'bg-[#18233C] text-emerald-400 border-[#2A3B5E]'
                        }`}>
                          {ayah.number}
                        </span>
                        {isActive && (
                          <span className="text-[10px] font-bold text-amber-300 px-1.5 py-0.5 rounded bg-amber-500/20 border border-amber-500/30 flex items-center gap-1">
                            <Sparkles className="w-2.5 h-2.5" />
                            <span>الآية الحالية</span>
                          </span>
                        )}
                      </div>
                      {isHost && (
                        <span className="text-[9.5px] text-slate-400">انقر لتزامن الجميع عندها</span>
                      )}
                    </div>

                    <p 
                      className={`font-serif text-[16px] sm:text-[18px] leading-loose text-justify ${
                        isActive ? 'text-white font-bold' : 'text-slate-200'
                      }`}
                      style={{ fontFamily: "'Traditional Arabic', 'Amiri', serif" }}
                    >
                      {ayah.textUthmani}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {activeTab === 'chat' && (
          <div className="flex flex-col h-full">
            <div className="flex-1 space-y-2.5 overflow-y-auto pb-2">
              {room.messages.map(m => (
                <div 
                  key={m.id}
                  className={`p-2.5 rounded-2xl text-[12px] border ${
                    m.isHost 
                      ? 'bg-amber-950/30 border-amber-500/30 text-amber-100' 
                      : m.userId === currentUserId 
                        ? 'bg-emerald-950/30 border-emerald-500/30 text-emerald-100 ml-auto' 
                        : 'bg-[#121A2E] border-[#1E2B48] text-slate-200'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <div className="flex items-center gap-1">
                      <span>{m.userAvatar}</span>
                      <span className="font-bold text-[11px]">{m.userName}</span>
                      {m.isHost && (
                        <span className="text-[9px] px-1 py-0.2 bg-amber-500/20 text-amber-300 rounded border border-amber-500/30">
                          مدير الغرفة
                        </span>
                      )}
                    </div>
                    <span className="text-[9px] text-[#64748B]">{m.timestamp}</span>
                  </div>
                  <p className="text-[12px] leading-relaxed">{m.text}</p>
                </div>
              ))}
            </div>

            <form onSubmit={handleSendMessage} className="pt-2 flex items-center gap-1.5 border-t border-[#1C2844]">
              <input 
                type="text" 
                placeholder="اكتب رسالة في مجلس القرآن..." 
                value={chatMessage} 
                onChange={(e) => setChatMessage(e.target.value)}
                className="flex-1 bg-[#10172A] border border-[#202E4E] rounded-xl px-3 py-2 text-[12px] text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
              />
              <button 
                type="submit" 
                className="p-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white transition flex-shrink-0"
              >
                <Send className="w-4 h-4 transform rotate-180" />
              </button>
            </form>
          </div>
        )}

        {activeTab === 'participants' && (
          <div className="space-y-2">
            {room.participants.map(p => (
              <div 
                key={p.id}
                className="flex items-center justify-between p-2.5 rounded-2xl bg-[#11192C] border border-[#1E2C4A]"
              >
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-xl bg-[#1A2640] border border-[#273859] flex items-center justify-center text-lg">
                    {p.avatar}
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <span className="text-[12px] font-bold text-white">{p.name}</span>
                      {p.role === 'host' && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30 flex items-center gap-1">
                          <Crown className="w-2.5 h-2.5" />
                          <span>المدير</span>
                        </span>
                      )}
                      {p.role === 'speaker' && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold">
                          متحدث
                        </span>
                      )}
                    </div>
                    <span className="text-[10px] text-slate-400">
                      {p.role === 'host' ? 'صاحب الغرفة' : p.role === 'speaker' ? 'مشارك صوتي' : 'مستمع هادئ'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5">
                  {p.role === 'speaker' && (
                    <div className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-300">
                      <Mic className="w-3.5 h-3.5" />
                    </div>
                  )}
                  {p.hasRaisedHand && (
                    <div className="p-1 rounded bg-amber-500/20 text-amber-300 text-[10px] flex items-center gap-0.5">
                      <Hand className="w-3 h-3" />
                      <span>طلب المايك</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Voice Council Mode Bottom Action Bar */}
      {room.mode === 'voice_council' && (
        <div className="bg-[#0C1222] border-t border-[#1C2A48] p-2.5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <button 
              onClick={() => setMyMicEnabled(!myMicEnabled)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                myMicEnabled 
                  ? 'bg-emerald-600 text-white shadow-md shadow-emerald-950' 
                  : 'bg-[#18233C] text-slate-300 hover:bg-[#202E4E]'
              }`}
            >
              {myMicEnabled ? <Mic className="w-3.5 h-3.5" /> : <MicOff className="w-3.5 h-3.5" />}
              <span>{myMicEnabled ? 'المايك مفتوح' : 'المايك مكتوم'}</span>
            </button>

            <button 
              onClick={() => setHandRaised(!handRaised)}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-medium border transition ${
                handRaised 
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40' 
                  : 'bg-[#162038] text-slate-300 border-[#243354] hover:bg-[#1E2B48]'
              }`}
            >
              <Hand className="w-3.5 h-3.5" />
              <span>{handRaised ? 'أنت رافع يدك' : 'طلب التحدث'}</span>
            </button>
          </div>

          <span className="text-[10px] text-[#64748B]">وضع المجلس الصوتي مفعل</span>
        </div>
      )}
    </div>
  );
};
