import React, { useState, useEffect } from 'react';
import { 
  BookOpen, Search, Radio, Volume2, Bookmark, Heart, Star, 
  ChevronLeft, ArrowRight, Play, Pause, Sparkles, Filter,
  Share2, Plus, Moon, Sun, Type, ZoomIn, ZoomOut, Check,
  X, Mic, Users, Headphones, Info, Shield
} from 'lucide-react';
import { 
  QuranSurah, QuranAyah, QuranJuz, QuranReader, 
  QuranBookmark, QuranRoom 
} from './types';
import { QURAN_SURAHS } from './quranSurahsData';
import { QURAN_AJZA } from './ajzaData';
import { QURAN_RECITERS, getReciterAudioUrl } from './recitersData';
import { getSurahVerses } from './quranVersesData';
import { INITIAL_QURAN_ROOMS } from './roomsData';
import { QuranAudioPlayerBar } from './QuranAudioPlayerBar';
import { QuranLiveRoomView } from './QuranLiveRoomView';

interface QuranHomeScreenProps {
  onBackToRooms?: () => void;
}

export const QuranHomeScreen: React.FC<QuranHomeScreenProps> = ({ onBackToRooms }) => {
  // Navigation & Sub-views
  const [activeTab, setActiveTab] = useState<'surahs' | 'ajza' | 'reciters' | 'rooms' | 'favorites'>('surahs');
  const [selectedSurah, setSelectedSurah] = useState<QuranSurah | null>(null);
  const [surahVerses, setSurahVerses] = useState<QuranAyah[]>([]);
  const [activeRoom, setActiveRoom] = useState<QuranRoom | null>(null);
  const [roomsList, setRoomsList] = useState<QuranRoom[]>(INITIAL_QURAN_ROOMS);

  // Audio Playback State
  const [currentReader, setCurrentReader] = useState<QuranReader>(QURAN_RECITERS[0]);
  const [audioPlayingSurah, setAudioPlayingSurah] = useState<QuranSurah | null>(null);
  const [audioPlayingAyah, setAudioPlayingAyah] = useState<number>(1);
  const [isPlayingAudio, setIsPlayingAudio] = useState<boolean>(false);

  // Reader Customization State
  const [readerTheme, setReaderTheme] = useState<'dark' | 'light' | 'sepia'>('dark');
  const [fontSize, setFontSize] = useState<number>(20);
  const [fontFamily, setFontFamily] = useState<'traditional' | 'amiri' | 'kufi'>('traditional');
  const [selectedTafsirAyah, setSelectedTafsirAyah] = useState<QuranAyah | null>(null);

  // Bookmarks & Favorites
  const [bookmarks, setBookmarks] = useState<QuranBookmark[]>([
    { id: 'b1', surahId: 2, surahName: 'البقرة', ayahNumber: 255, juzNumber: 3, timestamp: 'اليوم، 10:30 ص' }
  ]);
  const [favoriteSurahIds, setFavoriteSurahIds] = useState<number[]>([1, 2, 18, 36, 67, 112]);

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'مكية' | 'مدنية'>('all');

  // Create Room Modal
  const [showCreateRoomModal, setShowCreateRoomModal] = useState(false);
  const [newRoomTitle, setNewRoomTitle] = useState('');
  const [newRoomReaderId, setNewRoomReaderId] = useState(QURAN_RECITERS[0].id);
  const [newRoomSurahId, setNewRoomSurahId] = useState(1);
  const [newRoomMode, setNewRoomMode] = useState<'listen_only' | 'voice_council'>('listen_only');

  // Last read tracking for "متابعة القراءة"
  const [lastRead, setLastRead] = useState({
    surahId: 2,
    surahName: 'البقرة',
    ayahNumber: 255,
    juzNumber: 3,
    timeAgo: 'منذ ساعتين'
  });

  // Load verses when a Surah is opened
  useEffect(() => {
    if (selectedSurah) {
      getSurahVerses(selectedSurah.id).then(v => {
        setSurahVerses(v);
      });
    }
  }, [selectedSurah]);

  const handlePlaySurah = (surah: QuranSurah, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    if (audioPlayingSurah?.id === surah.id) {
      setIsPlayingAudio(!isPlayingAudio);
    } else {
      setAudioPlayingSurah(surah);
      setAudioPlayingAyah(1);
      setIsPlayingAudio(true);
    }
  };

  const handleToggleFavoriteSurah = (surahId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    setFavoriteSurahIds(prev => 
      prev.includes(surahId) ? prev.filter(id => id !== surahId) : [...prev, surahId]
    );
  };

  const handleCreateRoom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRoomTitle.trim()) return;
    const reader = QURAN_RECITERS.find(r => r.id === newRoomReaderId) || QURAN_RECITERS[0];
    const surah = QURAN_SURAHS.find(s => s.id === Number(newRoomSurahId)) || QURAN_SURAHS[0];

    const newRoom: QuranRoom = {
      id: `room_${Date.now()}`,
      title: newRoomTitle.trim(),
      bannerUrl: 'https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=500&h=200&fit=crop',
      hostUserId: 'host_101',
      hostUserName: 'أنت (مدير الغرفة)',
      readerId: reader.id,
      readerName: reader.name,
      surahId: surah.id,
      surahName: surah.name,
      ayahId: 1,
      positionMs: 0,
      isPlaying: true,
      serverTimestamp: Date.now(),
      listenersCount: 1,
      isPublic: true,
      mode: newRoomMode,
      participants: [
        { id: 'host_101', name: 'أنت', avatar: '👑', role: 'host', isMuted: false, hasRaisedHand: false }
      ],
      messages: [
        { id: 'm_init', userId: 'host_101', userName: 'أنت', userAvatar: '👑', text: 'مرحباً بالجميع في مجلس القرآن الكريم.', timestamp: 'الآن', isHost: true }
      ]
    };

    setRoomsList([newRoom, ...roomsList]);
    setShowCreateRoomModal(false);
    setActiveRoom(newRoom);
  };

  // Filtered Surahs
  const filteredSurahs = QURAN_SURAHS.filter(s => {
    const matchQuery = !searchQuery || 
      s.name.includes(searchQuery) || 
      s.nameEn.toLowerCase().includes(searchQuery.toLowerCase()) || 
      String(s.id) === searchQuery.trim();
    const matchType = filterType === 'all' || s.revelationType === filterType;
    return matchQuery && matchType;
  });

  // If in an active live room, display the synchronized room view
  if (activeRoom) {
    return (
      <QuranLiveRoomView 
        room={activeRoom}
        onLeaveRoom={() => setActiveRoom(null)}
        onUpdateRoom={(updated) => {
          setActiveRoom(updated);
          setRoomsList(roomsList.map(r => r.id === updated.id ? updated : r));
        }}
      />
    );
  }

  // Reader View for a specific Surah
  if (selectedSurah) {
    return (
      <div className={`flex flex-col h-full select-none ${
        readerTheme === 'light' 
          ? 'bg-[#F8F9FA] text-slate-900' 
          : readerTheme === 'sepia' 
            ? 'bg-[#F4ECD8] text-[#433422]' 
            : 'bg-[#090E1A] text-white'
      }`} dir="rtl">
        {/* Reader Top Bar */}
        <div className={`px-3 py-2.5 flex items-center justify-between border-b z-20 ${
          readerTheme === 'light' 
            ? 'bg-white border-slate-200' 
            : readerTheme === 'sepia' 
              ? 'bg-[#EAE0C8] border-[#D8CDAF]' 
              : 'bg-[#0F1626] border-[#1C2A48]'
        }`}>
          <div className="flex items-center gap-2">
            <button 
              onClick={() => setSelectedSurah(null)}
              className="p-1.5 rounded-xl hover:bg-black/10 transition"
              title="العودة للسور"
            >
              <ArrowRight className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-1.5">
                <h2 className="text-sm font-bold">سورة {selectedSurah.name}</h2>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 font-medium">
                  {selectedSurah.revelationType} • {selectedSurah.versesCount} آية
                </span>
              </div>
              <span className="text-[10px] text-slate-500 dark:text-slate-400">الجزء {selectedSurah.juzNumber}</span>
            </div>
          </div>

          <div className="flex items-center gap-1">
            {/* Quick Play Surah */}
            <button 
              onClick={(e) => handlePlaySurah(selectedSurah, e)}
              className="p-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1 text-[11px] font-bold shadow"
            >
              {audioPlayingSurah?.id === selectedSurah.id && isPlayingAudio ? (
                <Pause className="w-3.5 h-3.5 fill-white" />
              ) : (
                <Play className="w-3.5 h-3.5 fill-white" />
              )}
              <span className="hidden sm:inline">تلاوة</span>
            </button>

            {/* Font Size Adjusters */}
            <button 
              onClick={() => setFontSize(Math.min(32, fontSize + 2))}
              className="p-1.5 rounded-lg hover:bg-black/10 transition text-xs font-bold"
              title="تكبير الخط"
            >
              <ZoomIn className="w-4 h-4" />
            </button>
            <button 
              onClick={() => setFontSize(Math.max(16, fontSize - 2))}
              className="p-1.5 rounded-lg hover:bg-black/10 transition text-xs font-bold"
              title="تصغير الخط"
            >
              <ZoomOut className="w-4 h-4" />
            </button>

            {/* Theme Toggle */}
            <button 
              onClick={() => {
                const themes: ('dark' | 'light' | 'sepia')[] = ['dark', 'light', 'sepia'];
                const nextIdx = (themes.indexOf(readerTheme) + 1) % themes.length;
                setReaderTheme(themes[nextIdx]);
              }}
              className="p-1.5 rounded-lg hover:bg-black/10 transition"
              title="تغيير مظهر القراءة"
            >
              {readerTheme === 'dark' ? <Moon className="w-4 h-4 text-amber-400" /> : <Sun className="w-4 h-4 text-amber-600" />}
            </button>
          </div>
        </div>

        {/* Verses Reading Canvas */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 max-w-2xl mx-auto w-full pb-24">
          {/* Bismillah Decorative Header (unless At-Tawbah) */}
          {selectedSurah.id !== 9 && (
            <div className="py-4 text-center">
              <div className="inline-block p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 shadow-sm">
                <span 
                  className="text-lg sm:text-xl font-bold text-emerald-700 dark:text-emerald-400 tracking-wider block"
                  style={{ fontFamily: "'Traditional Arabic', 'Amiri', serif" }}
                >
                  بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ
                </span>
              </div>
            </div>
          )}

          {/* Verses Stream */}
          <div className="space-y-3">
            {surahVerses.map(ayah => {
              const isPlayingThisAyah = audioPlayingSurah?.id === selectedSurah.id && audioPlayingAyah === ayah.number;
              return (
                <div 
                  key={ayah.number}
                  className={`p-3.5 rounded-2xl border transition-all duration-300 ${
                    isPlayingThisAyah 
                      ? 'bg-emerald-500/15 border-emerald-500 shadow-md shadow-emerald-950/20' 
                      : readerTheme === 'light' 
                        ? 'bg-white border-slate-200 hover:border-slate-300 shadow-sm' 
                        : readerTheme === 'sepia' 
                          ? 'bg-[#EFE5CD] border-[#D8CDAF]' 
                          : 'bg-[#101728] border-[#1C2844] hover:border-[#2B3C64]'
                  }`}
                >
                  {/* Ayah Number Header & Actions */}
                  <div className="flex items-center justify-between mb-2">
                    <span className="w-7 h-7 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-600 dark:text-emerald-300 font-bold text-xs flex items-center justify-center">
                      {ayah.number}
                    </span>

                    <div className="flex items-center gap-1 text-slate-400">
                      <button 
                        onClick={() => setSelectedTafsirAyah(ayah)}
                        className="p-1 hover:text-emerald-500 rounded text-[11px] flex items-center gap-0.5"
                        title="التفسير الميسر"
                      >
                        <Info className="w-3.5 h-3.5" />
                        <span>تفسير</span>
                      </button>

                      <button 
                        onClick={() => {
                          const newBookmark: QuranBookmark = {
                            id: `bm_${Date.now()}`,
                            surahId: selectedSurah.id,
                            surahName: selectedSurah.name,
                            ayahNumber: ayah.number,
                            juzNumber: selectedSurah.juzNumber,
                            timestamp: 'الآن'
                          };
                          setBookmarks([newBookmark, ...bookmarks]);
                          setLastRead({
                            surahId: selectedSurah.id,
                            surahName: selectedSurah.name,
                            ayahNumber: ayah.number,
                            juzNumber: selectedSurah.juzNumber,
                            timeAgo: 'الآن'
                          });
                          alert(`تم حفظ العلامة المرجعية عند الآية ${ayah.number} من سورة ${selectedSurah.name}`);
                        }}
                        className="p-1 hover:text-amber-400 rounded"
                        title="إضافة علامة مرجعية"
                      >
                        <Bookmark className="w-3.5 h-3.5" />
                      </button>

                      <button 
                        onClick={() => {
                          navigator.clipboard.writeText(`﴿${ayah.textUthmani}﴾ [سورة ${selectedSurah.name}: ${ayah.number}]`);
                          alert('تم نسخ الآية الكريمة للمشاركة.');
                        }}
                        className="p-1 hover:text-blue-400 rounded"
                        title="مشاركة الآية"
                      >
                        <Share2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Ayah Calligraphy Text */}
                  <p 
                    className="leading-loose text-justify"
                    style={{ 
                      fontSize: `${fontSize}px`,
                      fontFamily: fontFamily === 'traditional' ? "'Traditional Arabic', serif" : "'Amiri', serif"
                    }}
                  >
                    {ayah.textUthmani}
                    <span className="mx-1 text-emerald-600 dark:text-emerald-400 font-bold text-sm">
                      ۝{ayah.number}
                    </span>
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Tafsir Modal */}
        {selectedTafsirAyah && (
          <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4" onClick={() => setSelectedTafsirAyah(null)}>
            <div className="bg-[#121B30] border border-[#233554] rounded-3xl p-5 max-w-md w-full shadow-2xl space-y-3 text-white" onClick={(e) => e.stopPropagation()}>
              <div className="flex items-center justify-between border-b border-[#1F2E4A] pb-2">
                <div className="flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-400" />
                  <h3 className="font-bold text-sm">التفسير الميسر</h3>
                </div>
                <button onClick={() => setSelectedTafsirAyah(null)} className="p-1 hover:bg-[#1C2844] rounded-lg">
                  <X className="w-4 h-4" />
                </button>
              </div>
              <p className="text-emerald-300 text-xs leading-relaxed font-serif">
                ﴿{selectedTafsirAyah.textUthmani}﴾
              </p>
              <div className="bg-[#0A101D] p-3 rounded-2xl border border-[#1A2640] text-xs text-slate-300 leading-relaxed">
                {selectedTafsirAyah.tafsir || 'تفسير وتدبر معاني الآية الكريمة والدروس المستفادة منها.'}
              </div>
              <button 
                onClick={() => setSelectedTafsirAyah(null)}
                className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 font-bold rounded-xl text-xs text-white"
              >
                إغلاق
              </button>
            </div>
          </div>
        )}

        {/* Persistent Bottom Audio Player */}
        <QuranAudioPlayerBar
          currentSurah={audioPlayingSurah}
          currentReader={currentReader}
          currentAyahNumber={audioPlayingAyah}
          isPlaying={isPlayingAudio}
          onTogglePlay={() => setIsPlayingAudio(!isPlayingAudio)}
          onNextSurah={() => {
            const nextId = (selectedSurah.id % 114) + 1;
            const nextSurah = QURAN_SURAHS.find(s => s.id === nextId);
            if (nextSurah) setSelectedSurah(nextSurah);
          }}
          onPrevSurah={() => {
            const prevId = selectedSurah.id === 1 ? 114 : selectedSurah.id - 1;
            const prevSurah = QURAN_SURAHS.find(s => s.id === prevId);
            if (prevSurah) setSelectedSurah(prevSurah);
          }}
          onClose={() => setIsPlayingAudio(false)}
        />
      </div>
    );
  }

  // Quran Home View
  return (
    <div className="flex flex-col h-full bg-[#080D1A] text-white select-none overflow-hidden" dir="rtl">
      {/* Top Header with Back to Rooms and Islamic Styling */}
      <div className="bg-gradient-to-r from-[#0F1628] via-[#142038] to-[#0F1628] border-b border-[#1E2E50] px-4 py-3 flex items-center justify-between z-20">
        <div className="flex items-center gap-2">
          {onBackToRooms && (
            <button 
              onClick={onBackToRooms}
              className="p-1.5 rounded-xl bg-[#162238] border border-[#233554] hover:bg-[#1F2F4C] transition text-slate-300"
              title="العودة لقائمة الغرف"
            >
              <ArrowRight className="w-5 h-5" />
            </button>
          )}
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center text-xl shadow-md shadow-emerald-950 border border-emerald-400/30">
              📖
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="text-sm font-black text-white tracking-wide">القرآن الكريم</h1>
                <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30">
                  ١١٤ سورة
                </span>
              </div>
              <span className="text-[10px] text-slate-400">تلاوة • تدبر • مجالس صوتية متزامنة</span>
            </div>
          </div>
        </div>

        {/* Selected Reciter Badge */}
        <div 
          onClick={() => setActiveTab('reciters')}
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-[#142036] border border-[#223554] cursor-pointer hover:border-emerald-500/50 transition"
          title="تغيير القارئ المفضل"
        >
          <img 
            src={currentReader.imageUrl} 
            alt={currentReader.name} 
            className="w-5 h-5 rounded-full object-cover border border-emerald-400/40"
          />
          <span className="text-[11px] font-bold text-emerald-400 truncate max-w-[90px]">{currentReader.name.split(' ')[0]}</span>
        </div>
      </div>

      {/* "متابعة القراءة" Banner (Continue Reading) */}
      <div className="px-3 pt-3">
        <div className="bg-gradient-to-l from-[#11243A] via-[#0F1E32] to-[#142940] border border-emerald-500/30 rounded-2xl p-3 shadow-lg shadow-emerald-950/20 flex items-center justify-between relative overflow-hidden">
          <div className="absolute top-0 left-0 w-24 h-24 bg-emerald-500/5 rounded-full blur-xl pointer-events-none"></div>
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-emerald-500/20 border border-emerald-400/40 flex items-center justify-center text-xl shadow-inner">
              🔖
            </div>
            <div>
              <div className="flex items-center gap-1.5 text-[11px] text-emerald-400 font-bold">
                <Bookmark className="w-3 h-3" />
                <span>متابعة القراءة</span>
                <span className="text-[10px] text-[#64748B]">• {lastRead.timeAgo}</span>
              </div>
              <div className="text-sm font-bold text-white mt-0.5">
                سورة {lastRead.surahName} - الآية {lastRead.ayahNumber}
              </div>
              <span className="text-[10.5px] text-slate-400">الجزء {lastRead.juzNumber}</span>
            </div>
          </div>

          <button 
            onClick={() => {
              const surah = QURAN_SURAHS.find(s => s.id === lastRead.surahId) || QURAN_SURAHS[0];
              setSelectedSurah(surah);
            }}
            className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-emerald-500 to-emerald-600 hover:from-emerald-600 hover:to-emerald-700 text-white text-xs font-bold shadow-md shadow-emerald-950/40 flex items-center gap-1 transition active:scale-95"
          >
            <span>متابعة</span>
            <ChevronLeft className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="px-3 pt-3">
        <div className="flex bg-[#0D1424] border border-[#1A2845] rounded-2xl p-1 gap-1 overflow-x-auto no-scrollbar">
          <button 
            onClick={() => setActiveTab('surahs')}
            className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11.5px] font-bold whitespace-nowrap transition flex items-center justify-center gap-1 ${
              activeTab === 'surahs' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>السور ({QURAN_SURAHS.length})</span>
          </button>

          <button 
            onClick={() => setActiveTab('ajza')}
            className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11.5px] font-bold whitespace-nowrap transition flex items-center justify-center gap-1 ${
              activeTab === 'ajza' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <span>الأجزاء (30)</span>
          </button>

          <button 
            onClick={() => setActiveTab('rooms')}
            className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11.5px] font-bold whitespace-nowrap transition flex items-center justify-center gap-1 ${
              activeTab === 'rooms' ? 'bg-gradient-to-r from-emerald-600 to-amber-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>غرف القرآن</span>
          </button>

          <button 
            onClick={() => setActiveTab('reciters')}
            className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11.5px] font-bold whitespace-nowrap transition flex items-center justify-center gap-1 ${
              activeTab === 'reciters' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Mic className="w-3.5 h-3.5" />
            <span>القراء</span>
          </button>

          <button 
            onClick={() => setActiveTab('favorites')}
            className={`flex-1 py-1.5 px-2.5 rounded-xl text-[11.5px] font-bold whitespace-nowrap transition flex items-center justify-center gap-1 ${
              activeTab === 'favorites' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Heart className="w-3.5 h-3.5" />
            <span>المفضلة</span>
          </button>
        </div>
      </div>

      {/* Main Tab Views */}
      <div className="flex-1 overflow-y-auto p-3 space-y-3 pb-24">
        {/* TAB 1: SURAHS */}
        {activeTab === 'surahs' && (
          <div className="space-y-3">
            {/* Search and Filters */}
            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute right-3 top-2.5 pointer-events-none" />
                <input 
                  type="text" 
                  placeholder="ابحث عن اسم السورة، رقمها، أو الآيات..." 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-[#10182C] border border-[#1E2D4C] rounded-xl pr-9 pl-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                />
                {searchQuery && (
                  <button onClick={() => setSearchQuery('')} className="absolute left-3 top-2.5 text-slate-400">
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>

              <div className="flex items-center gap-1 bg-[#10182C] border border-[#1E2D4C] p-0.5 rounded-xl">
                {(['all', 'مكية', 'مدنية'] as const).map(t => (
                  <button 
                    key={t}
                    onClick={() => setFilterType(t)}
                    className={`px-2 py-1 rounded-lg text-[10.5px] font-medium transition ${
                      filterType === t ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    {t === 'all' ? 'الكل' : t}
                  </button>
                ))}
              </div>
            </div>

            {/* Surahs Grid / Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {filteredSurahs.map(surah => {
                const isFav = favoriteSurahIds.includes(surah.id);
                const isPlaying = audioPlayingSurah?.id === surah.id && isPlayingAudio;
                return (
                  <div 
                    key={surah.id}
                    onClick={() => setSelectedSurah(surah)}
                    className="bg-[#11192D] border border-[#1E2D4C] hover:border-emerald-500/60 rounded-2xl p-2.5 flex items-center justify-between cursor-pointer transition group shadow-sm hover:shadow-md hover:shadow-emerald-950/20"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      {/* Number badge */}
                      <div className="w-9 h-9 rounded-xl bg-[#17233D] border border-[#26375A] flex items-center justify-center font-bold text-xs text-emerald-400 flex-shrink-0 group-hover:border-emerald-500">
                        {surah.id}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <h3 className="text-xs font-bold text-white group-hover:text-emerald-300 transition">{surah.name}</h3>
                          <span className="text-[9.5px] text-slate-400 font-mono">({surah.nameEn})</span>
                        </div>
                        <div className="text-[10px] text-slate-400 flex items-center gap-1 mt-0.5">
                          <span className={surah.revelationType === 'مكية' ? 'text-amber-400' : 'text-blue-400'}>
                            {surah.revelationType}
                          </span>
                          <span>•</span>
                          <span>{surah.versesCount} آية</span>
                          <span>•</span>
                          <span>الجزء {surah.juzNumber}</span>
                        </div>
                      </div>
                    </div>

                    {/* Quick Play & Favorite Buttons */}
                    <div className="flex items-center gap-1 flex-shrink-0">
                      <button 
                        onClick={(e) => handlePlaySurah(surah, e)}
                        className={`p-1.5 rounded-xl border transition ${
                          isPlaying 
                            ? 'bg-emerald-600 text-white border-emerald-400 shadow-md' 
                            : 'bg-[#18233C] text-slate-300 border-[#253556] hover:bg-[#223354] hover:text-white'
                        }`}
                        title={isPlaying ? 'إيقاف التلاوة' : 'تشغيل التلاوة'}
                      >
                        {isPlaying ? <Pause className="w-3.5 h-3.5 fill-white" /> : <Play className="w-3.5 h-3.5 fill-white ml-0.5" />}
                      </button>

                      <button 
                        onClick={(e) => handleToggleFavoriteSurah(surah.id, e)}
                        className={`p-1.5 rounded-xl border transition ${
                          isFav 
                            ? 'bg-rose-500/20 text-rose-400 border-rose-500/40' 
                            : 'bg-[#18233C] text-slate-400 border-[#253556] hover:text-rose-400'
                        }`}
                        title="إضافة للمفضلة"
                      >
                        <Heart className={`w-3.5 h-3.5 ${isFav ? 'fill-rose-400' : ''}`} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 2: AJZA */}
        {activeTab === 'ajza' && (
          <div className="space-y-2.5">
            <div className="bg-[#121B30] p-2.5 rounded-2xl border border-[#1E2C4A] text-xs text-slate-300 flex items-center gap-2">
              <Info className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              <span>القرآن الكريم مقسم إلى 30 جزءاً متساوياً لتسهيل الختمة والمراجعة اليومية.</span>
            </div>

            <div className="grid grid-cols-1 gap-2">
              {QURAN_AJZA.map(juz => (
                <div 
                  key={juz.number}
                  className="p-3 rounded-2xl bg-[#11192D] border border-[#1E2D4C] hover:border-emerald-500/60 transition flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-amber-500/20 to-emerald-500/20 border border-amber-500/30 flex items-center justify-center font-bold text-xs text-amber-300">
                      {juz.number}
                    </div>
                    <div>
                      <h3 className="text-xs font-bold text-white">{juz.name}</h3>
                      <p className="text-[10.5px] text-slate-400 mt-0.5">
                        من {juz.startSurahName} (آية {juz.startAyah}) إلى {juz.endSurahName} (آية {juz.endAyah})
                      </p>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {juz.surahsIncluded.map(name => (
                          <span key={name} className="text-[9px] px-1.5 py-0.2 rounded bg-[#18233C] text-emerald-400 border border-[#233352]">
                            {name}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  <button 
                    onClick={() => {
                      const surah = QURAN_SURAHS.find(s => s.name === juz.startSurahName) || QURAN_SURAHS[0];
                      setSelectedSurah(surah);
                    }}
                    className="px-3 py-1.5 rounded-xl bg-[#192642] hover:bg-emerald-600 hover:text-white text-emerald-400 border border-[#26375C] text-xs font-bold transition"
                  >
                    فتح
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 3: QURAN LIVE AUDIO ROOMS */}
        {activeTab === 'rooms' && (
          <div className="space-y-3">
            {/* Header & Create Room Button */}
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-xs font-bold text-white flex items-center gap-1.5">
                  <Radio className="w-4 h-4 text-emerald-400 animate-pulse" />
                  <span>غرف القرآن الصوتية المباشرة</span>
                </h3>
                <p className="text-[10px] text-slate-400">تلاوة واستماع جماعي متزامن لجميع المشاركين</p>
              </div>

              <button 
                onClick={() => setShowCreateRoomModal(true)}
                className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 text-white text-xs font-bold shadow-md shadow-emerald-950 flex items-center gap-1 transition"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>+ إنشاء غرفة قرآن</span>
              </button>
            </div>

            {/* Active Live Rooms List */}
            <div className="space-y-2.5">
              {roomsList.map(room => (
                <div 
                  key={room.id}
                  className="p-3.5 rounded-2xl bg-gradient-to-r from-[#11192E] to-[#0D1426] border border-[#202E4E] hover:border-emerald-500/60 shadow-md transition"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-1.5">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                      <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded-full border border-emerald-500/30">
                        مباشر الآن
                      </span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-[#18233C] text-slate-300 border border-[#253658]">
                        {room.mode === 'listen_only' ? 'استماع فقط' : 'مجلس صوتي'}
                      </span>
                    </div>

                    <div className="flex items-center gap-1 text-[11px] text-emerald-300 font-bold bg-emerald-950/40 px-2 py-0.5 rounded-lg border border-emerald-500/20">
                      <Users className="w-3 h-3" />
                      <span>{room.listenersCount} مستمعاً</span>
                    </div>
                  </div>

                  <h4 className="text-xs font-bold text-white mb-1">{room.title}</h4>

                  <div className="flex items-center justify-between text-[11px] text-slate-400 mb-3">
                    <div className="flex items-center gap-2">
                      <span>🎙️ {room.readerName}</span>
                      <span>•</span>
                      <span className="text-amber-300 font-medium">سورة {room.surahName} (الآية {room.ayahId})</span>
                    </div>
                  </div>

                  <button 
                    onClick={() => setActiveRoom(room)}
                    className="w-full py-2 bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-md shadow-emerald-950 transition active:scale-95"
                  >
                    <Headphones className="w-3.5 h-3.5" />
                    <span>دخول والاستماع المتزامن</span>
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 4: RECITERS */}
        {activeTab === 'reciters' && (
          <div className="space-y-3">
            <div className="bg-[#121B30] p-2.5 rounded-2xl border border-[#1E2C4A] text-xs text-slate-300 flex items-center gap-2">
              <Mic className="w-4 h-4 text-emerald-400 flex-shrink-0" />
              <span>اختر قارئك المفضل لتشغيل التلاوات وسماع المصحف المرتل والمجود بصوته العذب.</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {QURAN_RECITERS.map(reader => {
                const isCurrent = currentReader.id === reader.id;
                return (
                  <div 
                    key={reader.id}
                    onClick={() => {
                      setCurrentReader(reader);
                      alert(`تم اختيار القارئ: ${reader.name}`);
                    }}
                    className={`p-3 rounded-2xl border transition cursor-pointer flex items-center justify-between ${
                      isCurrent 
                        ? 'bg-gradient-to-r from-emerald-950/60 to-[#122432] border-emerald-500 shadow-md shadow-emerald-950/30' 
                        : 'bg-[#11192D] border-[#1E2D4C] hover:border-[#2B3E68]'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <img 
                        src={reader.imageUrl} 
                        alt={reader.name} 
                        className="w-12 h-12 rounded-2xl object-cover border border-emerald-500/30 shadow"
                      />
                      <div>
                        <div className="flex items-center gap-1.5">
                          <h3 className="text-xs font-bold text-white">{reader.name}</h3>
                          {isCurrent && (
                            <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold">
                              المحدد
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-emerald-400 font-medium">{reader.style}</span>
                        <p className="text-[9.5px] text-slate-400 line-clamp-1 mt-0.5">{reader.description}</p>
                      </div>
                    </div>

                    <button 
                      onClick={(e) => {
                        e.stopPropagation();
                        setCurrentReader(reader);
                        handlePlaySurah(QURAN_SURAHS[0]);
                      }}
                      className="p-2 rounded-xl bg-[#192642] hover:bg-emerald-600 text-emerald-400 hover:text-white transition"
                      title="استماع لتلاوة القارئ"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 5: FAVORITES & BOOKMARKS */}
        {activeTab === 'favorites' && (
          <div className="space-y-3">
            <div>
              <h3 className="text-xs font-bold text-white flex items-center gap-1 mb-2">
                <Bookmark className="w-3.5 h-3.5 text-amber-400" />
                <span>العلامات المرجعية المحفوظة</span>
              </h3>
              {bookmarks.length === 0 ? (
                <div className="p-4 rounded-2xl bg-[#10182C] border border-[#1E2D4C] text-center text-xs text-slate-400">
                  لا توجد علامات مرجعية حتى الآن. اضغط أيقونة الإشارة المرجعية عند أي آية لحفظها هنا.
                </div>
              ) : (
                <div className="space-y-2">
                  {bookmarks.map(b => (
                    <div 
                      key={b.id}
                      onClick={() => {
                        const surah = QURAN_SURAHS.find(s => s.id === b.surahId) || QURAN_SURAHS[0];
                        setSelectedSurah(surah);
                      }}
                      className="p-3 rounded-2xl bg-[#11192D] border border-[#1E2D4C] hover:border-emerald-500/60 transition cursor-pointer flex items-center justify-between"
                    >
                      <div>
                        <h4 className="text-xs font-bold text-white">سورة {b.surahName} - الآية {b.ayahNumber}</h4>
                        <span className="text-[10px] text-slate-400">الجزء {b.juzNumber} • {b.timestamp}</span>
                      </div>
                      <button className="text-xs font-bold text-emerald-400">الانتقال للآية</button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div>
              <h3 className="text-xs font-bold text-white flex items-center gap-1 mb-2 mt-4">
                <Heart className="w-3.5 h-3.5 text-rose-400" />
                <span>السور المفضلة</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {favoriteSurahIds.map(id => {
                  const surah = QURAN_SURAHS.find(s => s.id === id);
                  if (!surah) return null;
                  return (
                    <div 
                      key={surah.id}
                      onClick={() => setSelectedSurah(surah)}
                      className="p-2.5 rounded-2xl bg-[#11192D] border border-[#1E2D4C] hover:border-emerald-500/60 transition cursor-pointer flex items-center justify-between"
                    >
                      <div className="flex items-center gap-2">
                        <span className="w-7 h-7 rounded-lg bg-[#18233C] text-emerald-400 font-bold text-xs flex items-center justify-center">
                          {surah.id}
                        </span>
                        <div>
                          <h4 className="text-xs font-bold text-white">{surah.name}</h4>
                          <span className="text-[10px] text-slate-400">{surah.versesCount} آية • {surah.revelationType}</span>
                        </div>
                      </div>
                      <button 
                        onClick={(e) => handlePlaySurah(surah, e)}
                        className="p-1.5 rounded-xl bg-[#18233C] hover:bg-emerald-600 text-slate-300 hover:text-white transition"
                      >
                        <Play className="w-3 h-3 fill-current" />
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Create Quran Room Modal */}
      {showCreateRoomModal && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4" onClick={() => setShowCreateRoomModal(false)}>
          <div className="bg-[#121B32] border border-[#233558] rounded-3xl p-5 max-w-md w-full shadow-2xl space-y-4 text-white" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between border-b border-[#1E2E4E] pb-2.5">
              <div className="flex items-center gap-2">
                <Radio className="w-4 h-4 text-emerald-400" />
                <h3 className="font-bold text-sm">إنشاء غرفة قرآن صوتية</h3>
              </div>
              <button onClick={() => setShowCreateRoomModal(false)} className="p-1 hover:bg-[#1A2640] rounded-lg">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateRoom} className="space-y-3">
              <div>
                <label className="text-xs text-slate-300 font-medium block mb-1">اسم الغرفة أو المجلس</label>
                <input 
                  type="text" 
                  required
                  placeholder="مثال: مجلس تدبر وتلاوة سورة الرحمن" 
                  value={newRoomTitle}
                  onChange={(e) => setNewRoomTitle(e.target.value)}
                  className="w-full bg-[#0D1424] border border-[#202E4E] rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="text-xs text-slate-300 font-medium block mb-1">القارئ المعتمد للمجلس</label>
                <select 
                  value={newRoomReaderId}
                  onChange={(e) => setNewRoomReaderId(e.target.value)}
                  className="w-full bg-[#0D1424] border border-[#202E4E] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                >
                  {QURAN_RECITERS.map(r => (
                    <option key={r.id} value={r.id}>{r.name} ({r.style})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-300 font-medium block mb-1">السورة المقررة</label>
                <select 
                  value={newRoomSurahId}
                  onChange={(e) => setNewRoomSurahId(Number(e.target.value))}
                  className="w-full bg-[#0D1424] border border-[#202E4E] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-emerald-500"
                >
                  {QURAN_SURAHS.map(s => (
                    <option key={s.id} value={s.id}>{s.id}. {s.name} ({s.revelationType})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs text-slate-300 font-medium block mb-1">نوع المجلس الصوتي</label>
                <div className="grid grid-cols-2 gap-2">
                  <button 
                    type="button"
                    onClick={() => setNewRoomMode('listen_only')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition ${
                      newRoomMode === 'listen_only' 
                        ? 'bg-emerald-600 text-white border-emerald-400' 
                        : 'bg-[#0D1424] text-slate-400 border-[#202E4E]'
                    }`}
                  >
                    استماع فقط
                  </button>
                  <button 
                    type="button"
                    onClick={() => setNewRoomMode('voice_council')}
                    className={`py-2 px-3 rounded-xl border text-xs font-bold transition ${
                      newRoomMode === 'voice_council' 
                        ? 'bg-emerald-600 text-white border-emerald-400' 
                        : 'bg-[#0D1424] text-slate-400 border-[#202E4E]'
                    }`}
                  >
                    مجلس صوتي (مشاركة)
                  </button>
                </div>
              </div>

              <div className="pt-2 flex items-center gap-2">
                <button 
                  type="submit" 
                  className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-700 hover:from-emerald-500 hover:to-emerald-600 text-white text-xs font-bold shadow-lg shadow-emerald-950 transition"
                >
                  إنشاء الغرفة والدخول كمدير
                </button>
                <button 
                  type="button"
                  onClick={() => setShowCreateRoomModal(false)}
                  className="py-2.5 px-4 rounded-xl bg-[#162136] hover:bg-[#1E2D4A] text-slate-300 text-xs font-medium"
                >
                  إلغاء
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Persistent Bottom Audio Player Bar */}
      <QuranAudioPlayerBar
        currentSurah={audioPlayingSurah}
        currentReader={currentReader}
        currentAyahNumber={audioPlayingAyah}
        isPlaying={isPlayingAudio}
        onTogglePlay={() => setIsPlayingAudio(!isPlayingAudio)}
        onNextSurah={() => {
          if (!audioPlayingSurah) return;
          const nextId = (audioPlayingSurah.id % 114) + 1;
          const nextSurah = QURAN_SURAHS.find(s => s.id === nextId);
          if (nextSurah) setAudioPlayingSurah(nextSurah);
        }}
        onPrevSurah={() => {
          if (!audioPlayingSurah) return;
          const prevId = audioPlayingSurah.id === 1 ? 114 : audioPlayingSurah.id - 1;
          const prevSurah = QURAN_SURAHS.find(s => s.id === prevId);
          if (prevSurah) setAudioPlayingSurah(prevSurah);
        }}
        onClose={() => setIsPlayingAudio(false)}
      />
    </div>
  );
};
