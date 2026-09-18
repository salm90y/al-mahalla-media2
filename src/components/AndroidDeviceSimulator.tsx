import React, { useState, useEffect, useRef } from 'react';
import { 
  MessageSquare, Camera, Gamepad2, Phone, Users, Search, Settings,
  LayoutDashboard, BookOpen, Hand, Music, HelpCircle, Sparkles,
  Tv, Film, PlayCircle, ArrowLeft, ArrowRight, UserPlus, PhoneCall,
  CheckCheck, Video, ChevronDown, Palette, Bell, Lock, Database, 
  Shield, Globe, User, Edit, QrCode, Trash2, Eye, EyeOff, Check,
  LogOut, Loader2, FolderOpen, Volume2, VolumeX, Pause, Play, RotateCcw,
  Save, Cloud, Download, CloudDownload, CheckCircle, Plus, Wand2,
  RefreshCw, X, Disc, Radio, Sliders, ChevronUp, ChevronRight,
  Smartphone, Mic, Send, MessageCircle, MonitorPlay, Wifi, Layers,
  Copy, Folder, MoreHorizontal, ShieldCheck, UploadCloud
} from 'lucide-react';
import { PlayableEmulatorScreen } from './PlayableEmulatorScreen';
import { playRetroSound } from '../utils/retroAudio';

interface AndroidDeviceSimulatorProps {
  onOpenSettings?: () => void;
  onOpenAdmin?: () => void;
  onOpenNetplayLobby?: () => void;
  onOpenWalkieTalkie?: () => void;
  [key: string]: any;
}

export const AndroidDeviceSimulator: React.FC<AndroidDeviceSimulatorProps> = ({
  onOpenSettings,
  onOpenAdmin,
  onOpenNetplayLobby,
}) => {
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [loginUsername, setLoginUsername] = useState('أحمد المحلاوي');
  const [loginPassword, setLoginPassword] = useState('123456');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<'chats' | 'stories' | 'rooms' | 'calls' | 'friends'>('chats');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isAdminOpen, setIsAdminOpen] = useState(false);
  
  // Settings Expanded Accordion Sections (Empty by default: only names show initially)
  const [expandedSettings, setExpandedSettings] = useState<Record<string, boolean>>({});
  const [settingsSearch, setSettingsSearch] = useState('');

  // Settings State
  const [themeMode, setThemeMode] = useState('فاتح (افتراضي)');
  const [chatFontSize, setChatFontSize] = useState(15);
  const [sendOnEnter, setSendOnEnter] = useState(true);
  const [statusBarEdge, setStatusBarEdge] = useState(true);
  const [messageTone, setMessageTone] = useState('Apex');
  const [groupTone, setGroupTone] = useState('Chime');
  const [callRingtone, setCallRingtone] = useState('Default (افتراضي)');
  const [inAppSounds, setInAppSounds] = useState(true);
  const [messagePreview, setMessagePreview] = useState(true);
  const [readReceipts, setReadReceipts] = useState(true);
  const [hideOnline, setHideOnline] = useState(false);
  const [screenshotBlock, setScreenshotBlock] = useState(true);
  const [appLock, setAppLock] = useState(false);
  const [callDataSaver, setCallDataSaver] = useState(false);
  const [appLanguage, setAppLanguage] = useState('العربية (الافتراضية)');

  // Chat state
  const [chatCategory, setChatCategory] = useState<'الكل' | 'المجموعات' | 'القنوات'>('الكل');
  const [chatSearch, setChatSearch] = useState('');
  
  // Calls state
  const [callCategory, setCallCategory] = useState<'الكل' | 'الفائتة'>('الكل');
  const [callSearch, setCallSearch] = useState('');

  // Friends state
  const [friendCategory, setFriendCategory] = useState<'الأصدقاء' | 'طلبات الصداقة'>('الأصدقاء');
  const [friendSearch, setFriendSearch] = useState('');

  // Stories state
  const [storyCategory, setStoryCategory] = useState<'حالتي' | 'المشاهدة'>('حالتي');
  const [storySearch, setStorySearch] = useState('');

  // Rooms state
  const [roomSearch, setRoomSearch] = useState('');
  const [activeSubRoom, setActiveSubRoom] = useState<'none' | 'quran' | 'games'>('none');

  // Quran state
  const [isPlayingQuran, setIsPlayingQuran] = useState(false);
  const [currentSurah, setCurrentSurah] = useState('سورة الفاتحة');

  // Games Room Sub-Tab Navigation (matching the 6 icons from screenshot)
  const [gamesRoomSubTab, setGamesRoomSubTab] = useState<'chat' | 'camera' | 'intercom' | 'gamepad' | 'users' | 'settings'>('gamepad');
  const [isTopScreenEmulatorView, setIsTopScreenEmulatorView] = useState(false);

  // Chat Sub-Tab Messages
  const [gamesChatMessages, setGamesChatMessages] = useState([
    { id: 1, text: 'مرحباً! جاهز للعب؟', isMe: true, time: '10:24 ص' },
    { id: 2, text: 'أهلاً! نعم، لنبدأ الآن', isMe: false, time: '10:25 ص' },
    { id: 3, text: 'رائع! سوف أختار الشخصية', isMe: true, time: '10:26 ص' },
    { id: 4, text: 'ممتاز، أنا أيضاً جاهز', isMe: false, time: '10:27 ص' }
  ]);
  const [gamesChatInput, setGamesChatInput] = useState('');

  // Auto-scroll chat to bottom
  useEffect(() => {
    if (gamesRoomSubTab === 'chat') {
      chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [gamesChatMessages, gamesRoomSubTab]);

  // Walkie-Talkie Intercom state
  const [isIntercomTalking, setIsIntercomTalking] = useState(false);

  // Room & Comprehensive Emulator Settings State
  const [gamesRoomSound, setGamesRoomSound] = useState(true);
  const [gamesRoomVibration, setGamesRoomVibration] = useState(false);
  const [gamesRoomNotifications, setGamesRoomNotifications] = useState(true);
  const [gamesRoomPrivacy, setGamesRoomPrivacy] = useState(false);

  // Advanced PS1 NetPlay Emulator Settings
  const [emulatorResolution, setEmulatorResolution] = useState('2x (480p Enhanced)');
  const [emulatorRenderer, setEmulatorRenderer] = useState('Vulkan 1.3 (Hardware)');
  const [emulatorShader, setEmulatorShader] = useState('CRT Scanlines (Retro)');
  const [emulatorAudioSync, setEmulatorAudioSync] = useState('Ultra-Low (32ms DSP)');
  const [emulatorNetplayMode, setEmulatorNetplayMode] = useState('Cloudflare Global Turn Relay');
  const [emulatorFrameLimit, setEmulatorFrameLimit] = useState(true);
  const [isSettingsSavedToast, setIsSettingsSavedToast] = useState(false);
  const [copiedRoomCode, setCopiedRoomCode] = useState(false);
  const [isCompressionDetailsOpen, setIsCompressionDetailsOpen] = useState(false);
  const [isAddGameModalOpen, setIsAddGameModalOpen] = useState(false);
  const [customGameFileSelected, setCustomGameFileSelected] = useState<string | null>(null);
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  const [gamesLibrary, setGamesLibrary] = useState([
    {
      id: 'speed_race',
      title: 'سباق السرعة',
      englishTitle: 'Speed Racing GT',
      size: '1.8 غيغابايت',
      genre: 'سباق سيارات',
      rating: '9.9',
      players: '1 - 2 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-blue-600 to-indigo-900',
      tag: 'مثبتة',
      image: 'https://images.unsplash.com/photo-1617788138017-80ad40651399?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: true
    },
    {
      id: 'soccer',
      title: 'كرة القدم',
      englishTitle: 'Pro Soccer 2026',
      size: '2.4 غيغابايت',
      genre: 'رياضة',
      rating: '9.8',
      players: '1 - 4 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-emerald-600 to-teal-900',
      tag: 'مثبتة',
      image: 'https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: true
    },
    {
      id: 'hero_adventures',
      title: 'مغامرات الأبطال',
      englishTitle: 'Heroes Realm',
      size: '3.2 غيغابايت',
      genre: 'مغامرات • خيال',
      rating: '9.7',
      players: '1 - 2 لاعبين',
      isDownloaded: false,
      isDownloading: true,
      downloadProgress: 65,
      bannerColor: 'from-purple-600 to-slate-900',
      tag: 'جارٍ التحميل',
      image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: true
    },
    {
      id: 'empire_build',
      title: 'إمبراطورية البناء',
      englishTitle: 'Castle Empire',
      size: '1.6 غيغابايت',
      genre: 'استراتيجية • بناء',
      rating: '9.6',
      players: '1 لاعب',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-amber-600 to-orange-950',
      tag: 'مثبتة',
      image: 'https://images.unsplash.com/photo-1533158307587-828f0a76ef46?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: true
    },
    {
      id: 'space_odyssey',
      title: 'رحلة الفضاء',
      englishTitle: 'Cosmic Odyssey',
      size: '2.1 غيغابايت',
      genre: 'خيال علمي • فضاء',
      rating: '9.8',
      players: '1 - 2 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-cyan-600 to-blue-950',
      tag: 'مثبتة',
      image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: false
    },
    {
      id: 'open_world',
      title: 'عالم مفتوح',
      englishTitle: 'Horizon Wilderness',
      size: '2.9 غيغابايت',
      genre: 'عالم مفتوح • استكشاف',
      rating: '9.9',
      players: '1 - 2 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-teal-600 to-slate-900',
      tag: 'مثبتة',
      image: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80',
      isAddedByAdmin: false
    },
    {
      id: 'tekken3',
      title: 'تيكن 3',
      englishTitle: 'Tekken 3',
      size: '485 MB',
      genre: 'قتال • أكشن',
      rating: '9.8',
      players: '1 - 2 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-rose-600 to-slate-900',
      tag: 'مميزة'
    },
    {
      id: 'crash3',
      title: 'كراش بانديكوت 3: واربد',
      englishTitle: 'Crash Bandicoot 3',
      size: '340 MB',
      genre: 'مغامرات • منصات',
      rating: '9.7',
      players: '1 لاعب (NetPlay)',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-orange-500 to-amber-900',
      tag: 'كلاسيكية'
    },
    {
      id: 'we2002',
      title: 'وينينج إليفن 2002 (اليابانية)',
      englishTitle: 'Winning Eleven 2002',
      size: '512 MB',
      genre: 'رياضة • كرة قدم',
      rating: '9.9',
      players: '1 - 4 لاعبين',
      isDownloaded: false,
      isDownloading: false,
      downloadProgress: 0,
      bannerColor: 'from-emerald-600 to-slate-900',
      tag: 'الأكثر لعباً'
    },
    {
      id: 'pepsiman',
      title: 'بيبسي مان',
      englishTitle: 'Pepsiman',
      size: '120 MB',
      genre: 'ركض • مغامرات',
      rating: '9.4',
      players: '1 - 2 لاعبين',
      isDownloaded: false,
      isDownloading: false,
      downloadProgress: 0,
      bannerColor: 'from-blue-600 to-indigo-950',
      tag: 'خفيفة'
    },
    {
      id: 're2',
      title: 'ريزدنت إيفل 2',
      englishTitle: 'Resident Evil 2',
      size: '750 MB',
      genre: 'رعب • بقاء',
      rating: '9.8',
      players: '1 - 2 لاعبين',
      isDownloaded: false,
      isDownloading: false,
      downloadProgress: 0,
      bannerColor: 'from-slate-700 to-neutral-950',
      tag: 'مرعبة'
    },
    {
      id: 'jackiechan',
      title: 'جاكي شان: ستانت ماستر',
      englishTitle: 'Jackie Chan Stuntmaster',
      size: '410 MB',
      genre: 'قتال شوارع',
      rating: '9.6',
      players: '1 - 2 لاعبين',
      isDownloaded: false,
      isDownloading: false,
      downloadProgress: 0,
      bannerColor: 'from-amber-600 to-red-950',
      tag: 'قتال'
    }
  ]);

  const [activeGameId, setActiveGameId] = useState<string>('speed_race');
  const [isEmulatorInstalled, setIsEmulatorInstalled] = useState<boolean>(true);
  const [emulatorDownloadProgress, setEmulatorDownloadProgress] = useState<number>(100);
  const [isDownloadingEmulator, setIsDownloadingEmulator] = useState<boolean>(false);
  const [emulatorDownloadFinishedMessage, setEmulatorDownloadFinishedMessage] = useState<boolean>(false);

  // Gamepad states
  const [isAnalogOn, setIsAnalogOn] = useState(true);
  const [isGameMuted, setIsGameMuted] = useState(false);
  const [isGamePaused, setIsGamePaused] = useState(false);
  const [activeButtonPressed, setActiveButtonPressed] = useState<string | null>(null);

  // ROMs Manager Modal State (3 Tabs Matching Screenshots)
  const [isRomsModalOpen, setIsRomsModalOpen] = useState(false);
  const [romsModalTab, setRomsModalTab] = useState<'installed' | 'download' | 'added'>('installed');
  const [romSearchQuery, setRomSearchQuery] = useState('');

  // Admin Add Game State
  const [newGameName, setNewGameName] = useState('');
  const [newGameEnglish, setNewGameEnglish] = useState('');
  const [newGameGenre, setNewGameGenre] = useState('سباق سيارات');
  const [newGameSize, setNewGameSize] = useState('1.8 غيغابايت');
  const [newGamePosterUrl, setNewGamePosterUrl] = useState('');
  const [isAutoFetchingMetadata, setIsAutoFetchingMetadata] = useState(false);

  // First-time emulator download simulation when entering games room
  useEffect(() => {
    if (activeSubRoom === 'games' && !isEmulatorInstalled && !isDownloadingEmulator) {
      setIsDownloadingEmulator(true);
      setEmulatorDownloadProgress(0);
      let progress = 0;
      const interval = setInterval(() => {
        progress += 4;
        if (progress >= 100) {
          clearInterval(interval);
          setEmulatorDownloadProgress(100);
          setIsDownloadingEmulator(false);
          setIsEmulatorInstalled(true);
          setEmulatorDownloadFinishedMessage(true);
        } else {
          setEmulatorDownloadProgress(progress);
        }
      }, 40);
      return () => clearInterval(interval);
    }
  }, [activeSubRoom, isEmulatorInstalled, isDownloadingEmulator]);

  const activeGame = gamesLibrary.find(g => g.id === activeGameId) || gamesLibrary[0];

  const handleDownloadGame = (gameId: string) => {
    setGamesLibrary(prev => prev.map(g => {
      if (g.id === gameId) {
        return { ...g, isDownloading: true, downloadProgress: 0 };
      }
      return g;
    }));

    let progress = 0;
    const interval = setInterval(() => {
      progress += 10;
      if (progress >= 100) {
        clearInterval(interval);
        setGamesLibrary(prev => prev.map(g => {
          if (g.id === gameId) {
            return { ...g, isDownloading: false, isDownloaded: true, downloadProgress: 100 };
          }
          return g;
        }));
      } else {
        setGamesLibrary(prev => prev.map(g => {
          if (g.id === gameId) {
            return { ...g, downloadProgress: progress };
          }
          return g;
        }));
      }
    }, 150);
  };

  const handleAutoFetchMetadata = () => {
    setIsAutoFetchingMetadata(true);
    setTimeout(() => {
      if (!newGameName) {
        setNewGameName('جران توريزمو 2');
        setNewGameEnglish('Gran Turismo 2');
        setNewGameGenre('سباق • محاكاة');
        setNewGameSize('620 MB');
        setNewGamePosterUrl('https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500&q=80');
      }
      setIsAutoFetchingMetadata(false);
    }, 600);
  };

  const handleAddNewGame = () => {
    if (!newGameName.trim()) return;
    const newId = `custom_${Date.now()}`;
    const newGame = {
      id: newId,
      title: newGameName.trim(),
      englishTitle: newGameEnglish.trim() || newGameName.trim(),
      size: newGameSize.trim() || '450 MB',
      genre: newGameGenre,
      rating: '9.5',
      players: '1 - 2 لاعبين',
      isDownloaded: true,
      isDownloading: false,
      downloadProgress: 100,
      bannerColor: 'from-blue-600 to-indigo-900',
      tag: 'جديدة'
    };
    setGamesLibrary(prev => [newGame, ...prev]);
    setActiveGameId(newId);
    setNewGameName('');
    setNewGameEnglish('');
    setNewGamePosterUrl('');
    setIsRomsModalOpen(false);
  };

  const toggleAccordion = (id: string) => {
    setExpandedSettings(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const contentRooms = [
    { id: 'quran', title: 'قرآن كريم', icon: BookOpen },
    { id: 'duas', title: 'أدعية وازيارات', icon: Hand },
    { id: 'latmiyat', title: 'لطميات', icon: Music },
    { id: 'majalis', title: 'مجالس', icon: Users },
    { id: 'fatawa', title: 'فتاوى', icon: HelpCircle },
    { id: 'afrah', title: 'أفراح', icon: Sparkles },
    { id: 'tv', title: 'قنوات تلفزيونية', icon: Tv },
    { id: 'movies', title: 'أفلام ومسلسلات', icon: Film },
    { id: 'media', title: 'ميديا', icon: PlayCircle },
    { id: 'games', title: 'ألعاب', icon: Gamepad2 }
  ];

  const surahs = [
    { no: 1, name: 'الفاتحة', english: 'Al-Fatihah', verses: 7, type: 'مكية' },
    { no: 2, name: 'البقرة', english: 'Al-Baqarah', verses: 286, type: 'مدنية' },
    { no: 3, name: 'آل عمران', english: 'Ali Imran', verses: 200, type: 'مدنية' },
    { no: 4, name: 'النساء', english: 'An-Nisa', verses: 176, type: 'مدنية' },
    { no: 36, name: 'يس', english: 'Ya-Sin', verses: 83, type: 'مكية' },
    { no: 55, name: 'الرحمن', english: 'Ar-Rahman', verses: 78, type: 'مدنية' },
    { no: 67, name: 'الملك', english: 'Al-Mulk', verses: 30, type: 'مكية' },
    { no: 112, name: 'الإخلاص', english: 'Al-Ikhlas', verses: 4, type: 'مكية' },
  ];

  const filteredContentRooms = contentRooms.filter(r => 
    !roomSearch || r.title.includes(roomSearch)
  );

  return (
    <div id="android-device-simulator-root" className="flex items-center justify-center p-2 sm:p-6 min-h-screen bg-slate-900 font-sans">
      {/* Phone Frame */}
      <div className="w-full max-w-md h-[860px] max-h-[94vh] bg-[#F8FAFC] rounded-[42px] border-[8px] border-slate-800 shadow-2xl overflow-hidden flex flex-col relative" dir="rtl">
        
        {/* Device Top Status Bar */}
        <div className="w-full bg-[#F8FAFC] pt-3 pb-1 px-6 flex justify-between items-center text-xs text-slate-800 select-none z-30 font-['Tajawal']">
          <span className="font-bold">12:07</span>
          <div className="w-20 h-4 bg-slate-200 rounded-full flex items-center justify-center">
            <div className="w-2 h-2 rounded-full bg-slate-400"></div>
          </div>
          <div className="flex items-center space-x-1.5 space-x-reverse text-slate-600 font-semibold">
            <span>5G</span>
            <span>100%</span>
          </div>
        </div>

        {/* Dynamic Screen Content */}
        <main className="flex-1 overflow-y-auto pb-6 relative bg-[#F0F6FF]">
          
          {/* ================= 0. LOGIN SCREEN (WHEN NOT LOGGED IN - 100% MATCHING SCREENSHOT) ================= */}
          {!isLoggedIn ? (
            <div className="flex flex-col items-center justify-between min-h-full px-6 py-8 font-['Tajawal'] text-right bg-[#F8FAFC]">
              <div className="w-full flex flex-col items-center">
                {/* Top Illustrated Hero Badge Matching Screenshot */}
                <div className="relative mb-4 flex items-center justify-center">
                  <div className="w-24 h-24 rounded-full bg-[#EEF5FF] flex items-center justify-center relative shadow-sm border border-blue-100">
                    {/* Upper Left & Right Soft Bursts */}
                    <div className="absolute -top-3 -left-3 flex space-x-1">
                      <span className="w-1.5 h-4 bg-[#3B82F6] rounded-full rotate-[-45deg]"></span>
                      <span className="w-1 h-2.5 bg-[#3B82F6] rounded-full rotate-[-20deg] translate-y-1"></span>
                    </div>
                    <div className="absolute -top-3 -right-3 flex space-x-1">
                      <span className="w-1 h-2.5 bg-[#3B82F6] rounded-full rotate-[20deg] translate-y-1"></span>
                      <span className="w-1.5 h-4 bg-[#3B82F6] rounded-full rotate-[45deg]"></span>
                    </div>

                    {/* Integrated Icon (Controller + Chat Bubble + Sound Waves) */}
                    <div className="w-14 h-14 rounded-2xl bg-[#2563EB] flex items-center justify-center text-white shadow-md">
                      <Gamepad2 className="w-8 h-8" />
                    </div>
                  </div>
                </div>

                {/* Subtitle & Title Matching Screenshot */}
                <p className="text-xs font-bold text-slate-500 mb-1 font-['Tajawal']">
                  مرحباً بك في تطبيق المحلة
                </p>
                <h1 className="text-2xl font-black text-[#1E3A8A] tracking-wide mb-6 font-['Tajawal']">
                  تسجيل الدخول
                </h1>

                {/* Login Card Form */}
                <div className="w-full bg-white rounded-[2rem] border border-slate-100 shadow-[0_4px_20px_rgba(0,0,0,0.04)] p-6 space-y-4">
                  {/* Phone / Email Input Matching Screenshot */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1.5 font-['Tajawal']">
                      رقم الهاتف أو البريد الإلكتروني
                    </label>
                    <div className="flex items-center px-4 py-3 bg-[#EEF5FF] border border-[#DBEAFE] rounded-2xl focus-within:border-[#2563EB] transition">
                      <User className="w-5 h-5 text-[#2563EB] ml-3 flex-shrink-0" />
                      <input 
                        type="text"
                        placeholder="أدخل رقم الهاتف أو البريد الإلكتروني"
                        value={loginUsername}
                        onChange={(e) => {
                          setLoginUsername(e.target.value);
                          setLoginError(null);
                        }}
                        className="bg-transparent outline-none w-full text-xs font-bold text-slate-900 placeholder:text-slate-400 font-['Tajawal']"
                      />
                    </div>
                  </div>

                  {/* Password Input Matching Screenshot */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1.5 font-['Tajawal']">
                      كلمة المرور
                    </label>
                    <div className="flex items-center px-4 py-3 bg-[#EEF5FF] border border-[#DBEAFE] rounded-2xl focus-within:border-[#2563EB] transition">
                      <Lock className="w-5 h-5 text-[#2563EB] ml-3 flex-shrink-0" />
                      <input 
                        type={showPassword ? "text" : "password"}
                        placeholder="أدخل كلمة المرور"
                        value={loginPassword}
                        onChange={(e) => {
                          setLoginPassword(e.target.value);
                          setLoginError(null);
                        }}
                        className="bg-transparent outline-none w-full text-xs font-bold text-slate-900 placeholder:text-slate-400 font-['Tajawal']"
                      />
                      <button 
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="text-slate-400 hover:text-[#2563EB] mr-2 p-1 transition"
                        title={showPassword ? "إخفاء" : "إظهار"}
                      >
                        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>

                  {/* Row: Remember Me (Right) & Forgot Password (Left) Matching Screenshot */}
                  <div className="flex items-center justify-between pt-1 select-none">
                    <div 
                      onClick={() => setRememberMe(!rememberMe)}
                      className="flex items-center space-x-2 space-x-reverse cursor-pointer"
                    >
                      <div className={`w-4.5 h-4.5 rounded-md flex items-center justify-center transition ${rememberMe ? 'bg-[#2563EB] text-white' : 'bg-slate-200'}`}>
                        {rememberMe && <Check className="w-3.5 h-3.5" />}
                      </div>
                      <span className="text-xs font-bold text-slate-600 font-['Tajawal']">
                        تذكرني
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => alert('يمكنك إعادة تعيين كلمة المرور عبر الدعم')}
                      className="text-xs font-bold text-[#2563EB] hover:underline font-['Tajawal']"
                    >
                      نسيت كلمة المرور؟
                    </button>
                  </div>

                  {loginError && (
                    <p className="text-xs font-bold text-rose-500 font-['Tajawal'] pt-1">
                      {loginError}
                    </p>
                  )}

                  {/* Submit Full-Width Pill Button Matching Screenshot */}
                  <button
                    type="button"
                    disabled={isLoggingIn}
                    onClick={() => {
                      if (loginUsername.trim().length < 3) {
                        setLoginError("يرجى إدخال اسم المستخدم أو رقم الهاتف");
                        return;
                      }
                      if (loginPassword.trim().length < 4) {
                        setLoginError("كلمة المرور يجب أن تتكون من 4 خانات على الأقل");
                        return;
                      }
                      setIsLoggingIn(true);
                      setLoginError(null);
                      setTimeout(() => {
                        setIsLoggingIn(false);
                        setIsLoggedIn(true);
                      }, 400);
                    }}
                    className="w-full py-3.5 bg-[#2563EB] hover:bg-blue-600 active:scale-[0.98] text-white font-bold rounded-full text-sm transition shadow-md flex items-center justify-center font-['Tajawal'] mt-2"
                  >
                    {isLoggingIn ? (
                      <Loader2 className="w-5 h-5 animate-spin text-white" />
                    ) : (
                      "تسجيل الدخول"
                    )}
                  </button>
                </div>
              </div>

              {/* Footer Author Matching Screenshot */}
              <div className="pt-8 text-center">
                <p className="text-xs text-slate-400 font-medium font-sans">
                  تم الإنشاء بواسطة <span className="font-semibold text-slate-600">Ahmed Al Hilfi</span>
                </p>
              </div>
            </div>
          ) : isSettingsOpen ? (
            <div className="flex flex-col h-full bg-[#F8FAFC]">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">الإعدادات</h1>
                <button 
                  onClick={() => setIsSettingsOpen(false)}
                  className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-700 hover:bg-slate-50 shadow-sm transition"
                  title="رجوع"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
              </div>

              {/* Unified Search */}
              <div className="px-5 py-1 mb-2">
                <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                  <input 
                    type="text" 
                    placeholder="ابحث في الإعدادات" 
                    value={settingsSearch}
                    onChange={(e) => setSettingsSearch(e.target.value)}
                    className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                  />
                  <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                </div>
              </div>

              {/* Settings Accordion List */}
              <div className="px-5 space-y-3 pb-8">
                
                {/* 1. الملف الشخصي والحساب */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('profile')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <User className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">الملف الشخصي والحساب</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['profile'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>
                  
                  {(expandedSettings['profile'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center space-x-3 space-x-reverse py-2">
                        <div className="w-14 h-14 rounded-full bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center font-bold text-xl font-['Tajawal'] relative border border-blue-100">
                          أ
                          <div className="absolute bottom-0 right-0 w-5 h-5 bg-[#2563EB] text-white rounded-full flex items-center justify-center">
                            <Edit className="w-3 h-3" />
                          </div>
                        </div>
                        <div className="text-right flex-1">
                          <h3 className="font-bold text-slate-900 font-['Tajawal'] text-base">أحمد المحلاوي</h3>
                          <p className="text-xs text-slate-500 font-['Tajawal']">عضو نشط • متاح للتواصل</p>
                          <p className="text-xs text-[#2563EB] font-mono mt-0.5">+964 770 123 4567</p>
                        </div>
                        <button className="p-2 text-[#2563EB] hover:bg-blue-50 rounded-full transition" title="رمز QR">
                          <QrCode className="w-5 h-5" />
                        </button>
                      </div>

                      <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                        <button
                          onClick={() => {
                            setIsSettingsOpen(false);
                            setIsLoggedIn(false);
                          }}
                          className="flex items-center space-x-2 space-x-reverse text-xs text-red-600 hover:text-red-700 font-bold font-['Tajawal'] py-1.5 px-3 rounded-lg hover:bg-red-50 transition"
                        >
                          <LogOut className="w-4 h-4" />
                          <span>تسجيل الخروج من الحساب</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>

                {/* 2. المظهر والواجهة */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('appearance')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Palette className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">المظهر والواجهة</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['appearance'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['appearance'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-3 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">وضع الثيم</span>
                        <span className="text-xs text-[#2563EB] font-bold font-['Tajawal'] bg-blue-50 px-2.5 py-1 rounded-full">{themeMode}</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">لون التمييز الأساسي</span>
                        <div className="w-6 h-6 rounded-full bg-[#2563EB] shadow-xs"></div>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">شريط الحالة ممتد للحواف</span>
                        <input 
                          type="checkbox" 
                          checked={statusBarEdge} 
                          onChange={(e) => setStatusBarEdge(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">حجم خط الدردشة</span>
                        <span className="text-xs text-slate-500 font-['Tajawal']">{chatFontSize} نقطة</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">إرسال بزر الإدخال (Enter)</span>
                        <input 
                          type="checkbox" 
                          checked={sendOnEnter} 
                          onChange={(e) => setSendOnEnter(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* 3. الإشعارات والأصوات */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('notifications')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Bell className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">الإشعارات والأصوات</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['notifications'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['notifications'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-3 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">نغمة الرسائل</span>
                        <span className="text-xs text-slate-500 font-mono">{messageTone}</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">نغمة المجموعات</span>
                        <span className="text-xs text-slate-500 font-mono">{groupTone}</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">نغمة رنين المكالمات</span>
                        <span className="text-xs text-slate-500 font-mono">{callRingtone}</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">أصوات داخل التطبيق</span>
                        <input 
                          type="checkbox" 
                          checked={inAppSounds} 
                          onChange={(e) => setInAppSounds(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">إظهار معاينة الرسالة</span>
                        <input 
                          type="checkbox" 
                          checked={messagePreview} 
                          onChange={(e) => setMessagePreview(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* 4. الخصوصية والأمان */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('privacy')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Lock className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">الخصوصية والأمان</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['privacy'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['privacy'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-3 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">آخر ظهور ومتصل الآن</span>
                        <span className="text-xs text-slate-500 font-['Tajawal']">الجميع</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">مؤشرات قراءة الرسائل (الصحين)</span>
                        <input 
                          type="checkbox" 
                          checked={readReceipts} 
                          onChange={(e) => setReadReceipts(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">إخفاء حالة متصل الآن</span>
                        <input 
                          type="checkbox" 
                          checked={hideOnline} 
                          onChange={(e) => setHideOnline(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">حظر لقطات الشاشة</span>
                        <input 
                          type="checkbox" 
                          checked={screenshotBlock} 
                          onChange={(e) => setScreenshotBlock(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">قفل التطبيق برمز PIN</span>
                        <input 
                          type="checkbox" 
                          checked={appLock} 
                          onChange={(e) => setAppLock(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* 5. التخزين والبيانات */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('storage')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Database className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">التخزين والبيانات</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['storage'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['storage'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-3 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">مساحة التخزين المستخدمة</span>
                        <span className="text-xs text-slate-500 font-mono">124.5 MB</span>
                      </div>
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">توفير البيانات أثناء المكالمات</span>
                        <input 
                          type="checkbox" 
                          checked={callDataSaver} 
                          onChange={(e) => setCallDataSaver(e.target.checked)}
                          className="w-4 h-4 accent-[#2563EB]"
                        />
                      </div>
                      <button 
                        onClick={() => alert('تم مسح الذاكرة المؤقتة بنجاح!')}
                        className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl text-xs transition font-['Tajawal']"
                      >
                        مسح الذاكرة المؤقتة (Cache)
                      </button>
                    </div>
                  )}
                </div>

                {/* 6. أذونات التطبيق */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('permissions')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Shield className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">أذونات التطبيق</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['permissions'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['permissions'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-2 bg-[#FAFCFF] animate-in fade-in duration-200 text-xs text-slate-600 font-['Tajawal']">
                      <p>• الكاميرا: مسموح بها لالتقاط الصور ومكالمات الفيديو</p>
                      <p>• الميكروفون: مسموح به للملاحظات الصوتية والمكالمات</p>
                      <p>• الإشعارات: مسموح بها لتلقي التنبيهات الفورية</p>
                      <p>• جهات الاتصال: مسموح بها لمزامنة الأصدقاء</p>
                    </div>
                  )}
                </div>

                {/* 7. اللغة والإعدادات العامة */}
                <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('general')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-[#EEF4FB] text-[#2563EB] flex items-center justify-center">
                        <Globe className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-slate-900 font-['Tajawal'] text-base">اللغة والإعدادات العامة</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-slate-400 transition-transform duration-200 ${expandedSettings['general'] || settingsSearch ? 'rotate-180' : ''}`} />
                  </div>

                  {(expandedSettings['general'] || settingsSearch) && (
                    <div className="px-4 pb-4 pt-1 border-t border-slate-100 space-y-3 bg-[#FAFCFF] animate-in fade-in duration-200">
                      <div className="flex items-center justify-between py-1.5">
                        <span className="text-sm font-semibold text-slate-800 font-['Tajawal']">لغة التطبيق</span>
                        <span className="text-xs text-[#2563EB] font-bold font-['Tajawal']">{appLanguage}</span>
                      </div>
                      <button 
                        onClick={() => alert('تم أخذ نسخة احتياطية من كافة الدردشات بنجاح!')}
                        className="w-full py-2 bg-[#2563EB] text-white font-bold rounded-xl text-xs transition font-['Tajawal'] shadow-sm"
                      >
                        النسخ الاحتياطي للدردشات
                      </button>
                    </div>
                  )}
                </div>

                {/* 8. حذف الحساب */}
                <div className="bg-white border border-rose-100 rounded-2xl overflow-hidden shadow-xs">
                  <div 
                    onClick={() => toggleAccordion('delete_account')}
                    className="p-4 flex items-center justify-between cursor-pointer hover:bg-rose-50/50 transition select-none"
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-rose-50 text-rose-500 flex items-center justify-center">
                        <Trash2 className="w-5 h-5" />
                      </div>
                      <span className="font-bold text-rose-600 font-['Tajawal'] text-base">حذف الحساب نهائياً</span>
                    </div>
                    <ChevronDown className={`w-5 h-5 text-rose-400 transition-transform duration-200 ${expandedSettings['delete_account'] ? 'rotate-180' : ''}`} />
                  </div>

                  {expandedSettings['delete_account'] && (
                    <div className="px-4 pb-4 pt-1 border-t border-rose-100 space-y-3 bg-rose-50/30 animate-in fade-in duration-200">
                      <p className="text-xs text-slate-600 font-['Tajawal'] leading-relaxed">
                        تحذير: سيؤدي حذف الحساب إلى مسح جميع المحادثات والوسائط والمجموعات نهائياً ولن تتمكن من استعادتها.
                      </p>
                      <button 
                        onClick={() => alert('تم تقديم طلب حذف الحساب')}
                        className="w-full py-2 bg-rose-600 hover:bg-rose-700 text-white font-bold rounded-xl text-xs transition font-['Tajawal'] shadow-sm"
                      >
                        تأكيد حذف الحساب
                      </button>
                    </div>
                  )}
                </div>

              </div>
            </div>
          ) : isAdminOpen ? (
            /* ================= CONTROL PANEL / DASHBOARD SCREEN ================= */
            <div className="flex flex-col h-full">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">لوحة التحكم</h1>
                <button 
                  onClick={() => setIsAdminOpen(false)}
                  className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-700 hover:bg-slate-50 shadow-sm transition"
                  title="رجوع"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
              </div>

              <div className="p-5 space-y-4 font-['Tajawal']">
                <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-bold text-slate-900">إحصائيات المنظومة</span>
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  </div>
                  <div className="grid grid-cols-2 gap-3 text-center">
                    <div className="bg-[#EEF4FB] rounded-xl p-3">
                      <div className="text-xl font-bold text-[#2563EB]">1,420</div>
                      <div className="text-xs text-slate-500 mt-0.5">المستخدمون النشطون</div>
                    </div>
                    <div className="bg-[#EEF4FB] rounded-xl p-3">
                      <div className="text-xl font-bold text-[#2563EB]">99.9%</div>
                      <div className="text-xs text-slate-500 mt-0.5">استقرار السيرفر</div>
                    </div>
                  </div>
                </div>

                <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs space-y-3">
                  <h3 className="text-sm font-bold text-slate-900">الإدارة والتحكم</h3>
                  <button 
                    onClick={() => alert('تم إرسال إشعار عام لكافة المستخدمين!')}
                    className="w-full py-2.5 bg-[#2563EB] text-white rounded-xl font-bold text-xs shadow-xs hover:bg-blue-700 transition"
                  >
                    إرسال إشعار عام للمستخدمين
                  </button>
                  <button 
                    onClick={() => alert('سجلات النظام تعمل بكفاءة تامة.')}
                    className="w-full py-2.5 bg-slate-100 text-slate-700 rounded-xl font-bold text-xs hover:bg-slate-200 transition"
                  >
                    فحص سجلات النظام (Server Logs)
                  </button>
                </div>
              </div>
            </div>
          ) : activeTab === 'chats' ? (
            /* ================= 1. TAB: CHATS (الدردشات) ================= */
            <div className="flex flex-col h-full">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">الدردشات</h1>
                <div className="flex items-center space-x-2 space-x-reverse">
                  <button 
                    onClick={() => onOpenAdmin ? onOpenAdmin() : setIsAdminOpen(true)}
                    className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-[#2563EB] hover:bg-slate-50 shadow-sm transition"
                    title="لوحة التحكم"
                  >
                    <LayoutDashboard className="w-5 h-5" />
                  </button>
                  <button 
                    onClick={() => onOpenSettings ? onOpenSettings() : setIsSettingsOpen(true)}
                    className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-700 hover:bg-slate-50 shadow-sm transition"
                    title="إعدادات التطبيق"
                  >
                    <Settings className="w-5 h-5" />
                  </button>
                </div>
              </div>

              {/* Unified Search */}
              <div className="px-5 py-1">
                <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                  <input 
                    type="text" 
                    placeholder="ابحث في الدردشات" 
                    value={chatSearch}
                    onChange={(e) => setChatSearch(e.target.value)}
                    className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                  />
                  <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                </div>
              </div>

              {/* Tabs */}
              <div className="flex items-center justify-start px-5 pt-2 border-b border-slate-100">
                {(['الكل', 'المجموعات', 'القنوات'] as const).map(tab => (
                  <button
                    key={tab}
                    onClick={() => setChatCategory(tab)}
                    className={`px-4 py-2 text-[15px] font-bold font-['Tajawal'] transition relative ${
                      chatCategory === tab ? 'text-[#2563EB]' : 'text-slate-500'
                    }`}
                  >
                    {tab}
                    {chatCategory === tab && (
                      <div className="absolute bottom-0 right-4 left-4 h-0.5 bg-[#2563EB] rounded-full"></div>
                    )}
                  </button>
                ))}
              </div>

              {/* Chat List Items */}
              <div className="divide-y divide-slate-100 px-4 mt-2">
                {[
                  { name: 'حسين البصراوي', msg: 'وعليكم السلام، تم استلام ملفات التحديث.', time: '11:45 ص', unread: 2, online: true, avatar: 'ح' },
                  { name: 'مجموعة المطورين • العراق', msg: 'علي: قمنا بتطبيق ثيم Tajawal الموحد', time: '10:30 ص', unread: 5, online: false, avatar: 'م' },
                  { name: 'كرار المحلاوي', msg: 'شكراً جزيلاً لك أخي العزيز', time: 'أمس', unread: 0, online: true, avatar: 'ك' },
                  { name: 'قناة الأخبار والتقنية', msg: 'إطلاق الإصدار الجديد من التطبيق', time: 'أمس', unread: 0, online: false, avatar: 'ق' },
                  { name: 'مصطفى السلامي', msg: 'هل تتوفر غرفة القرآن الكريم بصوت نقي؟', time: 'الأحد', unread: 0, online: false, avatar: 'م' },
                ].map((chat, idx) => (
                  <div key={idx} className="py-3 flex items-center justify-between cursor-pointer hover:bg-slate-50/80 rounded-xl px-2 transition">
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="relative">
                        <div className="w-12 h-12 rounded-full bg-[#EEF4FB] text-[#2563EB] font-bold text-base flex items-center justify-center font-['Tajawal']">
                          {chat.avatar}
                        </div>
                        {chat.online && (
                          <div className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-emerald-500 border-2 border-white rounded-full"></div>
                        )}
                      </div>
                      <div className="text-right">
                        <h3 className="font-bold text-slate-900 text-[15px] font-['Tajawal']">{chat.name}</h3>
                        <p className="text-xs text-slate-500 font-['Tajawal'] truncate max-w-[180px] mt-0.5">{chat.msg}</p>
                      </div>
                    </div>
                    <div className="flex flex-col items-end space-y-1">
                      <span className="text-[11px] text-slate-400 font-['Tajawal']">{chat.time}</span>
                      {chat.unread > 0 ? (
                        <span className="w-5 h-5 rounded-full bg-[#2563EB] text-white font-bold text-[11px] flex items-center justify-center font-['Tajawal']">
                          {chat.unread}
                        </span>
                      ) : (
                        <CheckCheck className="w-4 h-4 text-blue-500" />
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : activeTab === 'friends' ? (
            /* ================= 2. TAB: FRIENDS (الأصدقاء) ================= */
            <div className="flex flex-col h-full">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">الأصدقاء</h1>
                <button 
                  onClick={() => alert('إضافة صديق جديد')}
                  className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-[#2563EB] hover:bg-slate-50 shadow-sm transition"
                  title="إضافة صديق"
                >
                  <UserPlus className="w-5 h-5" />
                </button>
              </div>

              {/* Unified Search */}
              <div className="px-5 py-1">
                <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                  <input 
                    type="text" 
                    placeholder="ابحث عن أصدقاء" 
                    value={friendSearch}
                    onChange={(e) => setFriendSearch(e.target.value)}
                    className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                  />
                  <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                </div>
              </div>

              {/* Tabs */}
              <div className="flex items-center justify-start px-5 pt-2 border-b border-slate-100">
                {(['الأصدقاء', 'طلبات الصداقة'] as const).map(tab => (
                  <button
                    key={tab}
                    onClick={() => setFriendCategory(tab)}
                    className={`px-4 py-2 text-[15px] font-bold font-['Tajawal'] transition relative ${
                      friendCategory === tab ? 'text-[#2563EB]' : 'text-slate-500'
                    }`}
                  >
                    {tab}
                    {friendCategory === tab && (
                      <div className="absolute bottom-0 right-4 left-4 h-0.5 bg-[#2563EB] rounded-full"></div>
                    )}
                  </button>
                ))}
              </div>

              {/* Friends List */}
              <div className="divide-y divide-slate-100 px-4 mt-2">
                {[
                  { name: 'كرار حيدر', status: 'متصل الآن', online: true, avatar: 'ك' },
                  { name: 'سجاد العراقي', status: 'آخر ظهور منذ 15 دقيقة', online: false, avatar: 'س' },
                  { name: 'محمد الموسوي', status: 'مشغول في العمل', online: true, avatar: 'م' },
                  { name: 'جعفر الصادق', status: 'في رحاب القرآن الكريم', online: false, avatar: 'ج' },
                ].map((friend, idx) => (
                  <div key={idx} className="py-3 flex items-center justify-between cursor-pointer hover:bg-slate-50/80 rounded-xl px-2 transition">
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="relative">
                        <div className="w-12 h-12 rounded-full bg-[#EEF4FB] text-[#2563EB] font-bold text-base flex items-center justify-center font-['Tajawal']">
                          {friend.avatar}
                        </div>
                        {friend.online && (
                          <div className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-emerald-500 border-2 border-white rounded-full"></div>
                        )}
                      </div>
                      <div className="text-right">
                        <h3 className="font-bold text-slate-900 text-[15px] font-['Tajawal']">{friend.name}</h3>
                        <p className="text-xs text-slate-500 font-['Tajawal'] mt-0.5">{friend.status}</p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 space-x-reverse">
                      <button className="p-2 text-slate-600 hover:text-[#2563EB] hover:bg-blue-50 rounded-full transition">
                        <MessageSquare className="w-4 h-4" />
                      </button>
                      <button className="p-2 text-slate-600 hover:text-emerald-600 hover:bg-emerald-50 rounded-full transition">
                        <PhoneCall className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : activeTab === 'calls' ? (
            /* ================= 3. TAB: CALLS (المكالمات) ================= */
            <div className="flex flex-col h-full">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">المكالمات</h1>
                <button 
                  onClick={() => alert('بدء مكالمة جديدة')}
                  className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-emerald-600 hover:bg-slate-50 shadow-sm transition"
                  title="بدء مكالمة"
                >
                  <PhoneCall className="w-5 h-5" />
                </button>
              </div>

              {/* Unified Search */}
              <div className="px-5 py-1">
                <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                  <input 
                    type="text" 
                    placeholder="ابحث في سجل المكالمات" 
                    value={callSearch}
                    onChange={(e) => setCallSearch(e.target.value)}
                    className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                  />
                  <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                </div>
              </div>

              {/* Tabs */}
              <div className="flex items-center justify-start px-5 pt-2 border-b border-slate-100">
                {(['الكل', 'الفائتة'] as const).map(tab => (
                  <button
                    key={tab}
                    onClick={() => setCallCategory(tab)}
                    className={`px-4 py-2 text-[15px] font-bold font-['Tajawal'] transition relative ${
                      callCategory === tab ? 'text-[#2563EB]' : 'text-slate-500'
                    }`}
                  >
                    {tab}
                    {callCategory === tab && (
                      <div className="absolute bottom-0 right-4 left-4 h-0.5 bg-[#2563EB] rounded-full"></div>
                    )}
                  </button>
                ))}
              </div>

              {/* Calls List */}
              <div className="divide-y divide-slate-100 px-4 mt-2">
                {[
                  { name: 'حسين البصراوي', time: 'اليوم، 11:20 ص', type: 'صوتية', missed: false, avatar: 'ح' },
                  { name: 'كرار حيدر', time: 'أمس، 09:15 م', type: 'فيديو', missed: true, avatar: 'ك' },
                  { name: 'مصطفى السلامي', time: '20 مارس، 04:30 م', type: 'صوتية', missed: false, avatar: 'م' },
                ].map((call, idx) => (
                  <div key={idx} className="py-3 flex items-center justify-between cursor-pointer hover:bg-slate-50/80 rounded-xl px-2 transition">
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-12 h-12 rounded-full bg-[#EEF4FB] text-[#2563EB] font-bold text-base flex items-center justify-center font-['Tajawal']">
                        {call.avatar}
                      </div>
                      <div className="text-right">
                        <h3 className={`font-bold text-[15px] font-['Tajawal'] ${call.missed ? 'text-rose-600' : 'text-slate-900'}`}>
                          {call.name}
                        </h3>
                        <p className="text-xs text-slate-500 font-['Tajawal'] mt-0.5">{call.time}</p>
                      </div>
                    </div>
                    <button className="p-2 text-slate-600 hover:text-[#2563EB] hover:bg-blue-50 rounded-full transition">
                      {call.type === 'فيديو' ? <Video className="w-4 h-4" /> : <PhoneCall className="w-4 h-4" />}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : activeTab === 'stories' ? (
            /* ================= 4. TAB: STORIES (الحالات) ================= */
            <div className="flex flex-col h-full">
              {/* Unified Header */}
              <div className="px-5 py-3.5 flex items-center justify-between">
                <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">الحالات</h1>
                <button 
                  onClick={() => alert('إضافة حالة')}
                  className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-[#2563EB] hover:bg-slate-50 shadow-sm transition"
                  title="إضافة حالة"
                >
                  <Camera className="w-5 h-5" />
                </button>
              </div>

              {/* Unified Search */}
              <div className="px-5 py-1">
                <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                  <input 
                    type="text" 
                    placeholder="ابحث في الحالات" 
                    value={storySearch}
                    onChange={(e) => setStorySearch(e.target.value)}
                    className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                  />
                  <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                </div>
              </div>

              {/* Tabs */}
              <div className="flex items-center justify-start px-5 pt-2 border-b border-slate-100">
                {(['حالتي', 'المشاهدة'] as const).map(tab => (
                  <button
                    key={tab}
                    onClick={() => setStoryCategory(tab)}
                    className={`px-4 py-2 text-[15px] font-bold font-['Tajawal'] transition relative ${
                      storyCategory === tab ? 'text-[#2563EB]' : 'text-slate-500'
                    }`}
                  >
                    {tab}
                    {storyCategory === tab && (
                      <div className="absolute bottom-0 right-4 left-4 h-0.5 bg-[#2563EB] rounded-full"></div>
                    )}
                  </button>
                ))}
              </div>

              {/* Empty State */}
              <div className="flex-1 flex flex-col items-center justify-center px-8 text-center py-16">
                <div className="w-28 h-28 rounded-full bg-[#EEF4FB] flex items-center justify-center mb-5">
                  <div className="w-16 h-16 rounded-2xl bg-[#2563EB] flex items-center justify-center shadow-lg text-white">
                    <Sparkles className="w-8 h-8" />
                  </div>
                </div>

                <h2 className="text-xl font-bold text-slate-900 mb-1 font-['Tajawal']">لا توجد حالات بعد</h2>
                <p className="text-sm text-slate-500 mb-6 font-['Tajawal']">شارك لحظاتك مع أصدقائك بضغطة زر</p>

                <button 
                  onClick={() => alert('إضافة حالة')}
                  className="px-6 py-2.5 bg-[#2563EB] text-white rounded-full font-bold text-sm shadow-md hover:bg-blue-700 transition flex items-center space-x-2 space-x-reverse font-['Tajawal']"
                >
                  <Camera className="w-4 h-4" />
                  <span>إضافة حالة جديدة</span>
                </button>
              </div>
            </div>
          ) : (
            /* ================= 5. TAB: ROOMS (الغرف) ================= */
            <div className="flex flex-col h-full">
              {activeSubRoom === 'quran' ? (
                <div className="px-5 py-3.5">
                  {/* Sub Header */}
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-2xl font-bold text-slate-900 font-['Tajawal']">غرفة القرآن الكريم</h2>
                    <button 
                      onClick={() => setActiveSubRoom('none')}
                      className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-700 hover:bg-slate-50 shadow-sm transition"
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>
                  </div>

                  {/* Audio Player Card */}
                  <div className="bg-[#EEF6FF] border border-[#BFDBFE] rounded-2xl p-4 flex items-center justify-between mb-4">
                    <button 
                      onClick={() => setIsPlayingQuran(!isPlayingQuran)}
                      className="w-12 h-12 rounded-full bg-[#2563EB] text-white flex items-center justify-center shadow-md text-base"
                    >
                      {isPlayingQuran ? '⏸' : '▶'}
                    </button>
                    <div className="text-right">
                      <h3 className="font-bold text-[#1E3A8A] font-['Tajawal'] text-base">{currentSurah}</h3>
                      <p className="text-xs text-[#3B82F6] font-['Tajawal']">بصوت الشيخ عبد الباسط عبد الصمد</p>
                    </div>
                  </div>

                  {/* Surahs List */}
                  <div className="space-y-2 pb-6">
                    {surahs.map(s => (
                      <div 
                        key={s.no}
                        onClick={() => {
                          setCurrentSurah(`سورة ${s.name}`);
                          setIsPlayingQuran(true);
                        }}
                        className="bg-white border border-slate-100 rounded-xl p-3 flex items-center justify-between cursor-pointer hover:border-blue-200 transition"
                      >
                        <span className="text-xs text-slate-500 font-['Tajawal']">{s.verses} آيات • {s.type}</span>
                        <div className="flex items-center space-x-3 space-x-reverse">
                          <div className="text-right">
                            <h4 className="font-bold text-slate-900 text-sm font-['Tajawal']">سورة {s.name}</h4>
                            <span className="text-[11px] text-slate-400">{s.english}</span>
                          </div>
                          <div className="w-8 h-8 rounded-full bg-[#EEF4FB] text-[#2563EB] font-bold text-xs flex items-center justify-center font-['Tajawal']">
                            {s.no}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : activeSubRoom === 'games' ? (
                /* ================= GAMES ROOM: EMULATOR & VIRTUAL GAMEPAD (100% MATCHING SCREENSHOTS) ================= */
                <div className="flex flex-col h-full bg-[#F4F8FE] select-none p-4 pb-24 overflow-y-auto">
                  {/* Top Bar Header with Back Button, Click-to-Copy Room Code, and ROMs Library Icon + Shrunken Title */}
                  <div className="flex items-center justify-between mb-2.5 px-1">
                    {/* Back Button */}
                    <button 
                      onClick={() => setActiveSubRoom('none')}
                      className="w-8 h-8 rounded-full bg-white border border-slate-200/80 shadow-xs flex items-center justify-center text-slate-700 hover:text-blue-600 hover:border-blue-200 transition"
                      title="العودة للغرف"
                    >
                      <ArrowRight className="w-4 h-4" />
                    </button>

                    {/* Room Code with Instant Click-to-Copy for Sharing with Friends */}
                    <button
                      onClick={() => {
                        navigator.clipboard?.writeText('#PS1-8842');
                        setCopiedRoomCode(true);
                        setTimeout(() => setCopiedRoomCode(false), 2000);
                      }}
                      className="px-2.5 py-1 bg-[#EEF5FF] hover:bg-blue-100 border border-[#DBEAFE] text-[#2563EB] rounded-xl flex items-center space-x-1.5 space-x-reverse transition active:scale-95 shadow-xs"
                      title="اضغط لنسخ كود الغرفة ومشاركته مع الأصدقاء"
                    >
                      {copiedRoomCode ? (
                        <>
                          <Check className="w-3 h-3 text-emerald-600" />
                          <span className="text-[11px] font-bold text-emerald-600 font-['Tajawal']">تم النسخ!</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3 h-3" />
                          <span className="text-[11px] font-bold font-mono tracking-tight">#PS1-8842</span>
                        </>
                      )}
                    </button>

                    {/* Shrunken Title + Small Icon Button Beside It for ROMs Library */}
                    <div className="flex items-center space-x-1.5 space-x-reverse">
                      {/* Small Icon Button Without Name for ROMs Library */}
                      <button
                        onClick={() => setIsRomsModalOpen(true)}
                        className="w-7 h-7 rounded-full bg-[#EEF5FF] hover:bg-blue-100 text-[#2563EB] border border-[#DBEAFE] shadow-xs flex items-center justify-center transition active:scale-95"
                        title="مكتبة الرومات والسيرفر السحابي Cloudflare R2"
                      >
                        <Folder className="w-3.5 h-3.5" />
                      </button>

                      {/* Shrunken Room Name */}
                      <h2 className="text-xs font-bold text-[#1E3A8A] font-['Tajawal']">غرفة الألعاب</h2>
                    </div>
                  </div>

                  {/* 1. Top Card: Enlarged downwards per user request (Illustrated Gamepad / Live 60 FPS PS1 Combat Engine) */}
                  <div className="relative w-full h-72 sm:h-76 bg-[#EEF5FF] border border-[#DBEAFE] rounded-[2.5rem] overflow-hidden shadow-xs flex flex-col items-center justify-center p-3">
                    {isTopScreenEmulatorView ? (
                      /* Live Playable PS1 Combat Engine (DuckStation/Tekken 3 Engine) */
                      <div className="w-full h-full rounded-3xl overflow-hidden relative">
                        <PlayableEmulatorScreen
                          gameTitle={activeGame.title}
                          gameId={activeGame.id}
                          englishTitle={activeGame.englishTitle}
                          resolution={emulatorResolution}
                          renderer={emulatorRenderer}
                          shader={emulatorShader}
                          isMuted={!gamesRoomSound}
                          onToggleMute={() => setGamesRoomSound(!gamesRoomSound)}
                          activeButton={activeButtonPressed}
                          onOpenRomsModal={() => setIsRomsModalOpen(true)}
                        />
                        {/* Switch Back to Illustration Badge */}
                        <button
                          onClick={() => setIsTopScreenEmulatorView(false)}
                          className="absolute top-2 left-2 z-20 px-2.5 py-1 bg-white/90 backdrop-blur-xs border border-blue-200 text-[#2563EB] rounded-full text-[10px] font-bold font-['Tajawal'] hover:bg-white shadow-xs transition"
                          title="عرض الشعار التوضيحي"
                        >
                          عرض الشعار
                        </button>
                      </div>
                    ) : (
                      /* Pure Illustrated Controller Card (100% Matching Screenshots) */
                      <div 
                        onClick={() => setIsTopScreenEmulatorView(true)}
                        className="relative flex flex-col items-center justify-center w-full h-full cursor-pointer group"
                        title="اضغط لتشغيل شاشة محاكي PS1 المباشرة (60 FPS)"
                      >
                        {/* Soft Background Circular Blobs */}
                        <div className="absolute w-44 h-44 rounded-full bg-blue-100/50 -top-8 -left-8 pointer-events-none blur-sm"></div>
                        <div className="absolute w-36 h-36 rounded-full bg-sky-100/60 -bottom-6 -right-6 pointer-events-none blur-sm"></div>

                        {/* Radiant Burst Accents & High-Fidelity SVG Controller */}
                        <div className="relative flex items-center justify-center">
                          {/* Upper Left Bursts (3 Blue Pills radiating towards top-left) */}
                          <div className="absolute -top-7 -left-10 flex space-x-1.5 pointer-events-none">
                            <span className="w-1.5 h-6 bg-[#3B82F6] rounded-full rotate-[-55deg]"></span>
                            <span className="w-1.5 h-4 bg-[#3B82F6] rounded-full rotate-[-30deg] translate-y-2"></span>
                            <span className="w-1.5 h-3 bg-[#3B82F6] rounded-full rotate-[-15deg] translate-y-3"></span>
                          </div>

                          {/* Upper Right Bursts (3 Blue Pills radiating towards top-right) */}
                          <div className="absolute -top-7 -right-10 flex space-x-1.5 pointer-events-none">
                            <span className="w-1.5 h-3 bg-[#3B82F6] rounded-full rotate-[15deg] translate-y-3"></span>
                            <span className="w-1.5 h-4 bg-[#3B82F6] rounded-full rotate-[30deg] translate-y-2"></span>
                            <span className="w-1.5 h-6 bg-[#3B82F6] rounded-full rotate-[55deg]"></span>
                          </div>

                          {/* High Precision Gamepad Controller SVG Matching Screenshots */}
                          <div className="relative w-44 h-32 flex items-center justify-center filter drop-shadow-md">
                            <svg viewBox="0 0 200 140" className="w-full h-full" fill="none" xmlns="http://www.w3.org/2000/svg">
                              {/* Soft Shadow Underneath */}
                              <ellipse cx="100" cy="132" rx="75" ry="7" fill="#BFDBFE" fillOpacity="0.7" />
                              
                              {/* Controller Main Body */}
                              <path
                                d="M38 120 C24 104 18 72 26 44 C32 24 50 16 72 18 C84 19 92 26 100 26 C108 26 116 19 128 18 C150 16 168 24 174 44 C182 72 176 104 162 120 C154 130 140 128 132 114 C124 100 114 96 100 96 C86 96 76 100 68 114 C60 128 46 130 38 120 Z"
                                fill="#2563EB"
                              />

                              {/* Inner Grip Contour Shading */}
                              <path
                                d="M48 108 C40 96 36 76 40 56 C44 42 54 36 68 36 C76 36 82 40 86 46 C76 56 68 76 72 98 C66 106 54 114 48 108 Z"
                                fill="#1D4ED8"
                                fillOpacity="0.25"
                              />

                              {/* D-Pad on Left Grip (+) in Pure White */}
                              <g transform="translate(48, 50)">
                                <rect x="10" y="0" width="8" height="28" rx="3" fill="white" />
                                <rect x="0" y="10" width="28" height="8" rx="3" fill="white" />
                              </g>

                              {/* Center Horizontal Dots (..) */}
                              <circle cx="92" cy="64" r="3.5" fill="white" fillOpacity="0.9" />
                              <circle cx="108" cy="64" r="3.5" fill="white" fillOpacity="0.9" />

                              {/* Action Buttons on Right Grip (::) in Pure White */}
                              <g transform="translate(132, 48)">
                                <circle cx="16" cy="4" r="4" fill="white" />
                                <circle cx="28" cy="16" r="4" fill="white" />
                                <circle cx="16" cy="28" r="4" fill="white" />
                                <circle cx="4" cy="16" r="4" fill="white" />
                              </g>
                            </svg>
                          </div>
                        </div>

                        {/* Interactive Engine Switcher Indicator */}
                        <div className="mt-3 flex items-center space-x-2 space-x-reverse bg-white/80 backdrop-blur-xs px-3 py-1 rounded-full border border-blue-100 shadow-2xs">
                          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                          <span className="text-[11px] font-bold text-[#1E3A8A] font-['Tajawal'] group-hover:text-[#2563EB] transition">
                            {activeGame ? `المحاكي جاهز: ${activeGame.title} (اضغط لتشغيل اللعبة)` : 'اضغط لتشغيل شاشة المحاكي'}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* 2. Middle Sub-Nav Dock (The 6 Icons Floating Pill Bar from Screenshots) */}
                  <div className="bg-white border border-slate-100/90 shadow-[0_2px_12px_rgba(37,99,235,0.06)] rounded-full py-2 px-3 flex justify-between items-center max-w-sm mx-auto my-3 w-full">
                    {/* 1. Chat */}
                    <button
                      onClick={() => setGamesRoomSubTab('chat')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'chat' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="الدردشة"
                    >
                      <MessageSquare className="w-5 h-5" />
                    </button>

                    {/* 2. Camera */}
                    <button
                      onClick={() => setGamesRoomSubTab('camera')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'camera' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="الكاميرات"
                    >
                      <Camera className="w-5 h-5" />
                    </button>

                    {/* 3. Walkie-Talkie */}
                    <button
                      onClick={() => setGamesRoomSubTab('intercom')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'intercom' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="اللاسلكي"
                    >
                      <Radio className="w-5 h-5" />
                    </button>

                    {/* 4. Gamepad & Emulator Controls */}
                    <button
                      onClick={() => setGamesRoomSubTab('gamepad')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'gamepad' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="يد التحكم"
                    >
                      <Gamepad2 className="w-5 h-5" />
                    </button>

                    {/* 5. Users */}
                    <button
                      onClick={() => setGamesRoomSubTab('users')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'users' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="المتواجدون"
                    >
                      <Users className="w-5 h-5" />
                    </button>

                    {/* 6. Settings */}
                    <button
                      onClick={() => setGamesRoomSubTab('settings')}
                      className={`p-2.5 rounded-full transition ${
                        gamesRoomSubTab === 'settings' ? 'bg-[#2563EB] text-white shadow-xs scale-105' : 'text-slate-400 hover:text-[#2563EB]'
                      }`}
                      title="الإعدادات الشاملة"
                    >
                      <Settings className="w-5 h-5" />
                    </button>
                  </div>

                  {/* 3. Dynamic Bottom Section (Switches between the 6 Sub-Tabs 100% Matching Screenshots) */}
                  <div className="flex-1 flex flex-col">
                    {/* ================= TAB 1: CHAT (Image 1 Exact Match) ================= */}
                    {gamesRoomSubTab === 'chat' && (
                      <div className="bg-white border border-slate-100 rounded-[2.5rem] p-5 pb-6 shadow-sm flex flex-col justify-between h-96 sm:h-[420px]">
                        {/* Messages List with auto-scroll */}
                        <div className="space-y-3.5 overflow-y-auto pr-1 flex-1 pb-2">
                          {gamesChatMessages.map((msg) => (
                            <div 
                              key={msg.id} 
                              className={`flex items-end space-x-2.5 space-x-reverse ${msg.isMe ? 'justify-start' : 'justify-end'}`}
                            >
                              {/* Avatar */}
                              <div className="w-8 h-8 rounded-full bg-[#EEF5FF] text-[#2563EB] flex items-center justify-center shrink-0 border border-blue-100">
                                <User className="w-4 h-4" />
                              </div>

                              {/* Message Bubble */}
                              <div className="flex flex-col">
                                <div 
                                  className={`px-4 py-2.5 rounded-2xl text-xs font-bold font-['Tajawal'] max-w-[230px] ${
                                    msg.isMe 
                                      ? 'bg-[#2563EB] text-white rounded-br-xs shadow-xs' 
                                      : 'bg-[#EEF5FF] text-[#1E3A8A] rounded-bl-xs border border-[#DBEAFE]'
                                  }`}
                                >
                                  {msg.text}
                                </div>
                                <span className={`text-[10px] text-slate-400 font-['Tajawal'] mt-0.5 ${msg.isMe ? 'text-right' : 'text-left'}`}>
                                  {msg.time}
                                </span>
                              </div>
                            </div>
                          ))}
                          <div ref={chatEndRef} />
                        </div>

                        {/* Chat Input Pill Bar Matching Screenshot - Elevated to clear phone bottom navigation */}
                        <div className="pt-3 pb-1 border-t border-slate-100 flex items-center space-x-2 space-x-reverse">
                          <input
                            type="text"
                            placeholder="اكتب رسالة..."
                            value={gamesChatInput}
                            onChange={(e) => setGamesChatInput(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === 'Enter' && gamesChatInput.trim()) {
                                setGamesChatMessages(prev => [
                                  ...prev,
                                  { id: Date.now(), text: gamesChatInput, isMe: true, time: 'الآن' }
                                ]);
                                setGamesChatInput('');
                              }
                            }}
                            className="flex-1 bg-[#EEF5FF] border border-[#DBEAFE] text-slate-800 rounded-full px-4 py-2.5 text-xs font-['Tajawal'] text-right outline-none focus:border-blue-400 placeholder:text-slate-400 shadow-inner"
                          />
                          <button
                            onClick={() => {
                              if (gamesChatInput.trim()) {
                                setGamesChatMessages(prev => [
                                  ...prev,
                                  { id: Date.now(), text: gamesChatInput, isMe: true, time: 'الآن' }
                                ]);
                                setGamesChatInput('');
                              }
                            }}
                            className="w-10 h-10 rounded-full bg-[#2563EB] hover:bg-blue-600 active:scale-95 text-white flex items-center justify-center shadow-xs transition"
                          >
                            <Send className="w-4 h-4 rotate-180" />
                          </button>
                        </div>
                      </div>
                    )}

                    {/* ================= TAB 2: CAMERAS (Image 5 Exact Match) ================= */}
                    {gamesRoomSubTab === 'camera' && (
                      <div className="bg-white border border-slate-100 rounded-[2.5rem] p-5 shadow-sm flex flex-col justify-between space-y-3">
                        {/* Top Row: Camera 3 (Living Room) and Camera 2 (Kitchen) */}
                        <div className="grid grid-cols-2 gap-3">
                          {/* Camera 3 */}
                          <div className="flex flex-col">
                            <div className="relative h-24 rounded-2xl overflow-hidden border border-slate-100 bg-slate-100 shadow-xs">
                              <img 
                                src="https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=500&auto=format&fit=crop&q=80" 
                                alt="Living Room"
                                className="w-full h-full object-cover"
                              />
                              <span className="absolute top-2 left-2 w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_6px_rgba(16,185,129,0.8)]"></span>
                            </div>
                            <span className="text-xs font-bold text-[#2563EB] text-center font-['Tajawal'] mt-1.5 bg-[#EEF5FF] py-1.5 rounded-xl">كاميرا 3</span>
                          </div>

                          {/* Camera 2 */}
                          <div className="flex flex-col">
                            <div className="relative h-24 rounded-2xl overflow-hidden border border-slate-100 bg-slate-100 shadow-xs">
                              <img 
                                src="https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=500&auto=format&fit=crop&q=80" 
                                alt="Kitchen"
                                className="w-full h-full object-cover"
                              />
                              <span className="absolute top-2 left-2 w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_6px_rgba(16,185,129,0.8)]"></span>
                            </div>
                            <span className="text-xs font-bold text-[#2563EB] text-center font-['Tajawal'] mt-1.5 bg-[#EEF5FF] py-1.5 rounded-xl">كاميرا 2</span>
                          </div>
                        </div>

                        {/* Bottom Row: Camera 1 (Bedroom) */}
                        <div className="flex flex-col">
                          <div className="relative h-28 rounded-2xl overflow-hidden border border-slate-100 bg-slate-100 shadow-xs">
                            <img 
                              src="https://images.unsplash.com/photo-1540518614846-7ede433c4550?w=600&auto=format&fit=crop&q=80" 
                              alt="Bedroom"
                              className="w-full h-full object-cover"
                            />
                            <span className="absolute top-2 left-2 w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_6px_rgba(16,185,129,0.8)]"></span>
                          </div>
                          <span className="text-xs font-bold text-[#2563EB] text-center font-['Tajawal'] mt-1.5 bg-[#EEF5FF] py-1.5 rounded-xl">كاميرا 1</span>
                        </div>

                        {/* Open Camera Action Button */}
                        <button
                          onClick={() => alert('تم تفعيل الاتصال المباشر بالكاميرات')}
                          className="w-full max-w-[220px] mx-auto py-3 bg-[#2563EB] hover:bg-blue-600 active:scale-95 text-white rounded-full font-bold text-sm font-['Tajawal'] flex items-center justify-center space-x-2 space-x-reverse shadow-md transition"
                        >
                          <Camera className="w-4 h-4" />
                          <span>فتح الكاميرا</span>
                        </button>
                      </div>
                    )}

                    {/* ================= TAB 3: WALKIE-TALKIE / INTERCOM (Image 4 Exact Match) ================= */}
                    {gamesRoomSubTab === 'intercom' && (
                      <div className="bg-white border border-slate-100 rounded-[2.5rem] p-6 shadow-sm flex flex-col items-center justify-center h-84 sm:h-96 space-y-5">
                        {/* Circular Walkie-Talkie Graphic with Wave Broadcast Rays */}
                        <div className="relative flex items-center justify-center">
                          <div className="w-40 h-40 rounded-full bg-[#EEF5FF] flex items-center justify-center shadow-inner">
                            <div className="relative flex items-center justify-center">
                              {/* Outer Wave Arcs */}
                              <div className={`absolute -inset-4 border-2 border-blue-200 rounded-full ${isIntercomTalking ? 'animate-ping' : ''}`}></div>
                              
                              {/* Stylized Walkie-Talkie Graphic */}
                              <div className="w-16 h-22 bg-[#2563EB] rounded-2xl relative flex flex-col items-center justify-between p-2.5 shadow-md">
                                {/* Antenna */}
                                <div className="w-2.5 h-4 bg-[#1D4ED8] rounded-t-full absolute -top-3 right-3"></div>
                                {/* Speaker Grille Lines */}
                                <div className="w-full space-y-1.5 mt-3">
                                  <div className="w-3/4 h-1 bg-white/90 rounded-full mx-auto"></div>
                                  <div className="w-3/4 h-1 bg-white/90 rounded-full mx-auto"></div>
                                  <div className="w-3/4 h-1 bg-white/90 rounded-full mx-auto"></div>
                                </div>
                                <div className="w-2.5 h-2.5 rounded-full bg-emerald-400"></div>
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Push-to-Talk Long Pill Button */}
                        <div className="w-full max-w-xs flex flex-col items-center space-y-2">
                          <button
                            onMouseDown={() => {
                              setIsIntercomTalking(true);
                              if (gamesRoomSound) playRetroSound('intercom_on');
                            }}
                            onMouseUp={() => {
                              setIsIntercomTalking(false);
                              if (gamesRoomSound) playRetroSound('intercom_off');
                            }}
                            onTouchStart={() => {
                              setIsIntercomTalking(true);
                              if (gamesRoomSound) playRetroSound('intercom_on');
                            }}
                            onTouchEnd={() => {
                              setIsIntercomTalking(false);
                              if (gamesRoomSound) playRetroSound('intercom_off');
                            }}
                            className={`w-full max-w-[260px] py-3.5 rounded-full font-bold text-sm font-['Tajawal'] flex items-center justify-center space-x-2 space-x-reverse shadow-md transition active:scale-95 ${
                              isIntercomTalking 
                                ? 'bg-rose-500 text-white scale-98 shadow-rose-200' 
                                : 'bg-[#2563EB] hover:bg-blue-600 text-white'
                            }`}
                          >
                            <Mic className="w-4 h-4" />
                            <span>{isIntercomTalking ? 'جارٍ التحدث...' : 'اضغط للتحدث'}</span>
                          </button>

                          {/* Status Indicator */}
                          <div className="flex items-center space-x-1.5 space-x-reverse pt-1">
                            <span className={`w-2 h-2 rounded-full ${isIntercomTalking ? 'bg-rose-500 animate-pulse' : 'bg-[#2563EB]'}`}></span>
                            <span className="text-xs font-bold text-[#2563EB] font-['Tajawal']">
                              {isIntercomTalking ? 'الميكروفون نشط' : 'جاهز للتحدث'}
                            </span>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* ================= TAB 4: GAMEPAD & EMULATOR (Image 6 Exact Match) ================= */}
                    {gamesRoomSubTab === 'gamepad' && (
                      <div className="bg-transparent flex flex-col justify-between space-y-3 py-1">
                        {/* 1. Shoulder Buttons Row: L2, L1, R1, R2 */}
                        <div className="flex items-center justify-between px-1">
                          {/* L2, L1 */}
                          <div className="flex items-center space-x-2 space-x-reverse">
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('L2');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('L2');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="px-6 py-2 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-2xl shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                            >
                              L2
                            </button>
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('L1');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('L1');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="px-6 py-2 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-2xl shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                            >
                              L1
                            </button>
                          </div>

                          {/* R1, R2 */}
                          <div className="flex items-center space-x-2 space-x-reverse">
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('R1');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('R1');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="px-6 py-2 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-2xl shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                            >
                              R1
                            </button>
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('R2');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('R2');
                                if (gamesRoomSound) playRetroSound('button');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="px-6 py-2 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-2xl shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                            >
                              R2
                            </button>
                          </div>
                        </div>

                        {/* 2. Middle Section: D-Pad (Golden Yellow Cross) & PS Action Buttons (Triangle, Circle, Cross, Square) */}
                        <div className="flex items-center justify-between px-3 py-1">
                          {/* D-Pad (Seamless Golden Outlined Cross matching screenshot) */}
                          <div className="relative w-34 h-34 flex items-center justify-center select-none">
                            {/* D-Pad Up */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('▲ UP');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('▲ UP');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute top-0 w-11 h-12 bg-white border-2 border-amber-400 rounded-t-xl border-b-0 flex items-center justify-center shadow-xs active:bg-amber-100 active:scale-95 transition z-10"
                            >
                              <span className="text-amber-500 font-black text-sm">▲</span>
                            </button>

                            {/* D-Pad Down */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('▼ DOWN');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('▼ DOWN');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute bottom-0 w-11 h-12 bg-white border-2 border-amber-400 rounded-b-xl border-t-0 flex items-center justify-center shadow-xs active:bg-amber-100 active:scale-95 transition z-10"
                            >
                              <span className="text-amber-500 font-black text-sm">▼</span>
                            </button>

                            {/* D-Pad Left */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('◀ LEFT');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('◀ LEFT');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute left-0 h-11 w-12 bg-white border-2 border-amber-400 rounded-l-xl border-r-0 flex items-center justify-center shadow-xs active:bg-amber-100 active:scale-95 transition z-10"
                            >
                              <span className="text-amber-500 font-black text-sm">◀</span>
                            </button>

                            {/* D-Pad Right */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('▶ RIGHT');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('▶ RIGHT');
                                if (gamesRoomSound) playRetroSound('dpad');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute right-0 h-11 w-12 bg-white border-2 border-amber-400 rounded-r-xl border-l-0 flex items-center justify-center shadow-xs active:bg-amber-100 active:scale-95 transition z-10"
                            >
                              <span className="text-amber-500 font-black text-sm">▶</span>
                            </button>
                            
                            {/* Center core */}
                            <div className="w-9 h-9 bg-white border border-amber-100/50"></div>
                          </div>

                          {/* PlayStation Action Buttons (Mint Triangle, Red Circle, Blue Cross, Pink Square) */}
                          <div className="relative w-34 h-34 flex items-center justify-center select-none">
                            {/* Triangle (Top - Mint/Emerald Green) */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('△ TRIANGLE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('△ TRIANGLE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute top-0 inset-x-0 mx-auto w-11 h-11 bg-white border-2 border-emerald-400 rounded-full flex items-center justify-center shadow-xs active:bg-emerald-50 active:scale-90 transition"
                            >
                              <span className="text-emerald-500 font-black text-base">△</span>
                            </button>

                            {/* Circle (Right - Red/Coral) */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('◯ CIRCLE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('◯ CIRCLE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute right-0 inset-y-0 my-auto w-11 h-11 bg-white border-2 border-rose-400 rounded-full flex items-center justify-center shadow-xs active:bg-rose-50 active:scale-90 transition"
                            >
                              <span className="text-rose-500 font-black text-base">◯</span>
                            </button>

                            {/* Cross (Bottom - Blue) */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('✕ CROSS');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('✕ CROSS');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute bottom-0 inset-x-0 mx-auto w-11 h-11 bg-white border-2 border-blue-500 rounded-full flex items-center justify-center shadow-xs active:bg-blue-50 active:scale-90 transition"
                            >
                              <span className="text-blue-600 font-black text-base">✕</span>
                            </button>

                            {/* Square (Left - Pink/Magenta) */}
                            <button
                              onMouseDown={() => {
                                setActiveButtonPressed('▢ SQUARE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onMouseUp={() => setActiveButtonPressed(null)}
                              onTouchStart={() => {
                                setActiveButtonPressed('▢ SQUARE');
                                if (gamesRoomSound) playRetroSound('action');
                              }}
                              onTouchEnd={() => setActiveButtonPressed(null)}
                              className="absolute left-0 inset-y-0 my-auto w-11 h-11 bg-white border-2 border-pink-400 rounded-full flex items-center justify-center shadow-xs active:bg-pink-50 active:scale-90 transition"
                            >
                              <span className="text-pink-500 font-black text-base">▢</span>
                            </button>
                          </div>
                        </div>

                        {/* 3. Bottom Row: SELECT and START (Pill Buttons) */}
                        <div className="flex items-center justify-center space-x-6 space-x-reverse pt-2">
                          <button
                            onMouseDown={() => {
                              setActiveButtonPressed('SELECT');
                              if (gamesRoomSound) playRetroSound('start');
                            }}
                            onMouseUp={() => setActiveButtonPressed(null)}
                            onTouchStart={() => {
                              setActiveButtonPressed('SELECT');
                              if (gamesRoomSound) playRetroSound('start');
                            }}
                            onTouchEnd={() => setActiveButtonPressed(null)}
                            className="px-8 py-2.5 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-full shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                          >
                            SELECT
                          </button>
                          <button
                            onMouseDown={() => {
                              setActiveButtonPressed('START');
                              if (gamesRoomSound) playRetroSound('start');
                            }}
                            onMouseUp={() => setActiveButtonPressed(null)}
                            onTouchStart={() => {
                              setActiveButtonPressed('START');
                              if (gamesRoomSound) playRetroSound('start');
                            }}
                            onTouchEnd={() => setActiveButtonPressed(null)}
                            className="px-8 py-2.5 bg-[#EEF6FF] text-[#2563EB] border border-[#DBEAFE] font-bold text-xs rounded-full shadow-xs active:bg-[#2563EB] active:text-white transition active:scale-95"
                          >
                            START
                          </button>
                        </div>
                      </div>
                    )}

                    {/* ================= TAB 5: USERS (Image 2 Exact Match) ================= */}
                    {gamesRoomSubTab === 'users' && (
                      <div className="bg-white border border-slate-100 rounded-[2.5rem] p-6 shadow-sm space-y-3">
                        {/* Header Badge */}
                        <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                          <span className="px-3.5 py-1 bg-[#2563EB] text-white rounded-full text-xs font-bold font-['Tajawal'] shadow-xs">
                            4 متواجدون
                          </span>
                          <span className="text-xs font-bold text-slate-500 font-['Tajawal']">أعضاء غرفة الألعاب</span>
                        </div>

                        {/* User List Matching Screenshot */}
                        <div className="space-y-1">
                          {[
                            { name: 'أحمد' },
                            { name: 'سارة' },
                            { name: 'محمد' },
                            { name: 'ليان' }
                          ].map((usr, i) => (
                            <div 
                              key={i} 
                              className="flex items-center justify-between py-2.5 border-b border-slate-50 last:border-0 hover:bg-slate-50/60 px-2 rounded-xl transition cursor-pointer"
                            >
                              <ChevronRight className="w-4 h-4 text-blue-300" />
                              <div className="flex items-center space-x-3 space-x-reverse">
                                <span className="font-bold text-[#1E3A8A] text-base font-['Tajawal']">{usr.name}</span>
                                <div className="relative">
                                  <div className="w-10 h-10 rounded-full bg-[#EEF5FF] text-[#2563EB] flex items-center justify-center border border-blue-100">
                                    <User className="w-5 h-5" />
                                  </div>
                                  <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-white"></span>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* ================= TAB 6: SETTINGS (Image 3 + Comprehensive Emulator Settings) ================= */}
                    {gamesRoomSubTab === 'settings' && (
                      <div className="bg-white border border-slate-100 rounded-[2.5rem] p-5 shadow-sm space-y-4 max-h-96 overflow-y-auto">
                        {/* 4 Core Room Toggles (Matching Screenshot) */}
                        <div className="space-y-3">
                          {/* الصوت */}
                          <div className="flex items-center justify-between py-1.5 border-b border-slate-50">
                            <button
                              onClick={() => setGamesRoomSound(!gamesRoomSound)}
                              className={`w-12 h-6 rounded-full transition-colors relative ${gamesRoomSound ? 'bg-[#2563EB]' : 'bg-slate-200'}`}
                            >
                              <span className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${gamesRoomSound ? 'left-0.5' : 'right-0.5'}`}></span>
                            </button>
                            <div className="flex items-center space-x-2 space-x-reverse">
                              <span className="font-bold text-[#1E3A8A] text-sm font-['Tajawal']">الصوت</span>
                              <Volume2 className="w-5 h-5 text-[#2563EB]" />
                            </div>
                          </div>

                          {/* الاهتزاز */}
                          <div className="flex items-center justify-between py-1.5 border-b border-slate-50">
                            <button
                              onClick={() => setGamesRoomVibration(!gamesRoomVibration)}
                              className={`w-12 h-6 rounded-full transition-colors relative ${gamesRoomVibration ? 'bg-[#2563EB]' : 'bg-slate-200'}`}
                            >
                              <span className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${gamesRoomVibration ? 'left-0.5' : 'right-0.5'}`}></span>
                            </button>
                            <div className="flex items-center space-x-2 space-x-reverse">
                              <span className="font-bold text-[#1E3A8A] text-sm font-['Tajawal']">الاهتزاز</span>
                              <Smartphone className="w-5 h-5 text-[#2563EB]" />
                            </div>
                          </div>

                          {/* إشعارات الغرفة */}
                          <div className="flex items-center justify-between py-1.5 border-b border-slate-50">
                            <button
                              onClick={() => setGamesRoomNotifications(!gamesRoomNotifications)}
                              className={`w-12 h-6 rounded-full transition-colors relative ${gamesRoomNotifications ? 'bg-[#2563EB]' : 'bg-slate-200'}`}
                            >
                              <span className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${gamesRoomNotifications ? 'left-0.5' : 'right-0.5'}`}></span>
                            </button>
                            <div className="flex items-center space-x-2 space-x-reverse">
                              <span className="font-bold text-[#1E3A8A] text-sm font-['Tajawal']">إشعارات الغرفة</span>
                              <Bell className="w-5 h-5 text-[#2563EB]" />
                            </div>
                          </div>

                          {/* خصوصية الغرفة */}
                          <div className="flex items-center justify-between py-1.5 border-b border-slate-50">
                            <button
                              onClick={() => setGamesRoomPrivacy(!gamesRoomPrivacy)}
                              className={`w-12 h-6 rounded-full transition-colors relative ${gamesRoomPrivacy ? 'bg-[#2563EB]' : 'bg-slate-200'}`}
                            >
                              <span className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${gamesRoomPrivacy ? 'left-0.5' : 'right-0.5'}`}></span>
                            </button>
                            <div className="flex items-center space-x-2 space-x-reverse">
                              <span className="font-bold text-[#1E3A8A] text-sm font-['Tajawal']">خصوصية الغرفة</span>
                              <Shield className="w-5 h-5 text-[#2563EB]" />
                            </div>
                          </div>
                        </div>

                        {/* Advanced Comprehensive Emulator & NetPlay Settings */}
                        <div className="pt-2 border-t border-slate-200 space-y-3">
                          <h4 className="text-xs font-bold text-slate-500 font-['Tajawal'] text-right">إعدادات محاكي PS1 وسيرفر NetPlay</h4>

                          {/* Internal Graphics Resolution */}
                          <div className="text-right">
                            <label className="block text-xs font-bold text-slate-700 font-['Tajawal'] mb-1">دقة العرض والجرافيكس</label>
                            <select
                              value={emulatorResolution}
                              onChange={(e) => setEmulatorResolution(e.target.value)}
                              className="w-full bg-[#F0F6FF] border border-[#DBEAFE] text-[#1E3A8A] font-['Tajawal'] text-xs rounded-xl px-3 py-2 outline-none"
                            >
                              <option value="1x (240p Native)">1x Native (240p الأصلي)</option>
                              <option value="2x (480p Enhanced)">2x HD (480p محسن)</option>
                              <option value="4x (1080p Ultra HD)">4x Full HD (1080p فائق الدقة)</option>
                            </select>
                          </div>

                          {/* Rendering Engine */}
                          <div className="text-right">
                            <label className="block text-xs font-bold text-slate-700 font-['Tajawal'] mb-1">محرك التصيير (Renderer)</label>
                            <select
                              value={emulatorRenderer}
                              onChange={(e) => setEmulatorRenderer(e.target.value)}
                              className="w-full bg-[#F0F6FF] border border-[#DBEAFE] text-[#1E3A8A] font-mono text-xs rounded-xl px-3 py-2 outline-none"
                            >
                              <option value="Vulkan 1.3 (Hardware)">Vulkan 1.3 (الأسرع والأعلى أداءً)</option>
                              <option value="OpenGL ES 3.2">OpenGL ES 3.2 (توافق شامل)</option>
                              <option value="Software (Pixel-Perfect)">Software (أصلي دقيق بالبكسل)</option>
                            </select>
                          </div>

                          {/* Screen Shaders */}
                          <div className="text-right">
                            <label className="block text-xs font-bold text-slate-700 font-['Tajawal'] mb-1">مرشح الشاشة (Shaders & Scanlines)</label>
                            <select
                              value={emulatorShader}
                              onChange={(e) => setEmulatorShader(e.target.value)}
                              className="w-full bg-[#F0F6FF] border border-[#DBEAFE] text-[#1E3A8A] font-['Tajawal'] text-xs rounded-xl px-3 py-2 outline-none"
                            >
                              <option value="CRT Scanlines (Retro)">خطوط كلاسيكية CRT Scanlines</option>
                              <option value="Bilinear Smooth">تنعيم بيكسل Bilinear Smooth</option>
                              <option value="Off">بدون مرشح (Direct Framebuffer)</option>
                            </select>
                          </div>

                          {/* Audio Latency */}
                          <div className="text-right">
                            <label className="block text-xs font-bold text-slate-700 font-['Tajawal'] mb-1">مزامنة وجودة الصوت (DSP Latency)</label>
                            <select
                              value={emulatorAudioSync}
                              onChange={(e) => setEmulatorAudioSync(e.target.value)}
                              className="w-full bg-[#F0F6FF] border border-[#DBEAFE] text-[#1E3A8A] font-mono text-xs rounded-xl px-3 py-2 outline-none"
                            >
                              <option value="Ultra-Low (32ms DSP)">استجابة فائقة السرعة Ultra-Low (32ms)</option>
                              <option value="Balanced (64ms DSP)">استجابة متوازنة Balanced (64ms)</option>
                            </select>
                          </div>

                          {/* NetPlay Sync Engine */}
                          <div className="text-right">
                            <label className="block text-xs font-bold text-slate-700 font-['Tajawal'] mb-1">سيرفر اللعب الجماعي والمزامنة</label>
                            <div className="p-2.5 bg-[#EEF5FF] rounded-xl border border-[#DBEAFE] flex items-center justify-between text-xs font-['Tajawal']">
                              <span className="text-emerald-600 font-bold">متصل بـ 12ms</span>
                              <span className="font-bold text-[#1E3A8A]">سيرفر Cloudflare Edge NetPlay</span>
                            </div>
                          </div>
                        </div>

                        {/* Save Changes Button */}
                        <button
                          onClick={() => {
                            setIsSettingsSavedToast(true);
                            setTimeout(() => setIsSettingsSavedToast(false), 2500);
                          }}
                          className="w-full py-3 bg-[#2563EB] hover:bg-blue-600 text-white rounded-2xl font-bold text-sm font-['Tajawal'] shadow-md transition"
                        >
                          {isSettingsSavedToast ? '✓ تم حفظ جميع التغييرات بنجاح' : 'حفظ التغييرات'}
                        </button>
                      </div>
                    )}
                  </div>

                  {/* 3. ROMs Library & Manager Modal Dialog (100% Matching Screenshots 1, 2, and 3) */}
                  {isRomsModalOpen && (
                    <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-xs flex items-center justify-center p-3 animate-in fade-in duration-150">
                      <div className="w-full max-w-md bg-white rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh] text-slate-900 border border-slate-100">
                        {/* Modal Header */}
                        <div className="px-5 py-3.5 border-b border-slate-100 flex items-center justify-between bg-white">
                          <div className="text-right">
                            <h3 className="font-bold text-slate-900 font-['Tajawal'] text-base flex items-center space-x-2 space-x-reverse">
                              <Folder className="w-4 h-4 text-[#2563EB]" />
                              <span>مكتبة الرومات</span>
                            </h3>
                            <p className="text-[11px] text-slate-500 font-['Tajawal']">سيرفر التخزين السحابي Cloudflare R2 وتقنية ضغط CHD</p>
                          </div>
                          <button
                            onClick={() => setIsRomsModalOpen(false)}
                            className="w-8 h-8 rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200 flex items-center justify-center transition"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </div>

                        {/* 3 Segmented Tabs Matching Screenshots */}
                        <div className="p-3 bg-slate-50 border-b border-slate-100">
                          <div className="flex bg-[#E2EBF8] p-1 rounded-2xl">
                            <button
                              onClick={() => setRomsModalTab('installed')}
                              className={`flex-1 py-2 text-xs font-bold font-['Tajawal'] rounded-xl transition ${
                                romsModalTab === 'installed' 
                                  ? 'bg-white text-[#2563EB] shadow-xs' 
                                  : 'text-slate-600 hover:text-slate-900'
                              }`}
                            >
                              الألعاب المحملة ({gamesLibrary.filter(g => g.isDownloaded).length})
                            </button>
                            <button
                              onClick={() => setRomsModalTab('download')}
                              className={`flex-1 py-2 text-xs font-bold font-['Tajawal'] rounded-xl transition ${
                                romsModalTab === 'download' 
                                  ? 'bg-white text-[#2563EB] shadow-xs' 
                                  : 'text-slate-600 hover:text-slate-900'
                              }`}
                            >
                              تحميل الألعاب
                            </button>
                            <button
                              onClick={() => setRomsModalTab('added')}
                              className={`flex-1 py-2 text-xs font-bold font-['Tajawal'] rounded-xl transition ${
                                romsModalTab === 'added' 
                                  ? 'bg-[#2563EB] text-white shadow-xs' 
                                  : 'text-blue-700 hover:bg-blue-100/50'
                              }`}
                            >
                              الألعاب المضافة ({gamesLibrary.filter(g => g.isAddedByAdmin).length})
                            </button>
                          </div>

                          {/* Search bar */}
                          <div className="mt-2.5 flex items-center px-3 py-2 bg-white border border-slate-200 rounded-xl shadow-xs">
                            <input
                              type="text"
                              placeholder={
                                romsModalTab === 'installed' ? "ابحث في الألعاب المحملة..." :
                                romsModalTab === 'download' ? "ابحث في ألعاب Cloudflare R2..." :
                                "ابحث في الألعاب المضافة..."
                              }
                              value={romSearchQuery}
                              onChange={(e) => setRomSearchQuery(e.target.value)}
                              className="bg-transparent outline-none w-full text-xs text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                            />
                            <Search className="w-4 h-4 text-slate-400 mr-2 shrink-0" />
                          </div>
                        </div>

                        {/* Modal Body Content */}
                        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[#F8FAFC]">
                          {/* ================= TAB 1: DOWNLOADED / INSTALLED GAMES (Image 1) ================= */}
                          {romsModalTab === 'installed' && (
                            <div className="space-y-3">
                              {/* Subtitle Counter Badge */}
                              <div className="flex items-center justify-between px-1">
                                <span className="text-xs font-bold text-slate-600 font-['Tajawal'] flex items-center space-x-1.5 space-x-reverse">
                                  <Cloud className="w-3.5 h-3.5 text-blue-600" />
                                  <span>{gamesLibrary.filter(g => g.isDownloaded).length} ألعاب محملة جاهزة للعب</span>
                                </span>
                                <button
                                  onClick={() => setIsCompressionDetailsOpen(!isCompressionDetailsOpen)}
                                  className="text-[11px] font-bold text-[#2563EB] hover:underline font-['Tajawal'] flex items-center space-x-1 space-x-reverse"
                                >
                                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                                  <span>تقنية الضغط CHD</span>
                                </button>
                              </div>

                              {/* Compression Info Banner if toggled */}
                              {isCompressionDetailsOpen && (
                                <div className="p-3 bg-blue-50 border border-blue-200 rounded-2xl text-right animate-in fade-in duration-150">
                                  <h5 className="text-xs font-bold text-blue-900 font-['Tajawal'] mb-1">تقنية ضغط ألعاب PS1 المعتمدة: CHD + ZSTD</h5>
                                  <p className="text-[11px] text-blue-800 font-['Tajawal'] leading-relaxed">
                                    تتم أرشفة الألعاب عبر Cloudflare R2 بصيغة <b>CHD (Compressed Hunks of Data)</b> مع خوارزمية Zstandard Level 19. تتيح هذه التقنية تقليص حجم أسطوانة الـ 700 MB إلى ما يقارب 210 MB فقط مع إمكانية القراءة الفورية المباشرة (Sub-Block Streaming) من السحابة دون الحاجة لفك الضغط مسبقاً.
                                  </p>
                                </div>
                              )}

                              {/* 2-Column Grid Matching Screenshot 1 */}
                              <div className="grid grid-cols-2 gap-3">
                                {gamesLibrary
                                  .filter(g => g.isDownloaded && (!romSearchQuery || g.title.includes(romSearchQuery) || g.englishTitle.toLowerCase().includes(romSearchQuery.toLowerCase())))
                                  .map((game) => (
                                    <div 
                                      key={game.id} 
                                      className="bg-white border border-slate-200/80 rounded-2xl p-2.5 shadow-xs flex flex-col justify-between hover:border-blue-300 transition"
                                    >
                                      {/* Artwork Thumbnail */}
                                      <div className="w-full h-24 rounded-xl overflow-hidden relative mb-2 bg-slate-100">
                                        <img 
                                          src={game.image} 
                                          alt={game.title} 
                                          className="w-full h-full object-cover" 
                                          referrerPolicy="no-referrer"
                                        />
                                        <div className="absolute top-1.5 right-1.5 bg-black/60 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-bold">
                                          {game.size}
                                        </div>
                                      </div>

                                      {/* Game Info */}
                                      <div className="text-right mb-2">
                                        <h4 className="font-bold text-slate-900 text-xs font-['Tajawal'] truncate">{game.title}</h4>
                                        <div className="flex items-center space-x-1 space-x-reverse mt-1">
                                          <Cloud className="w-3 h-3 text-blue-600 shrink-0" />
                                          <span className="text-[10px] font-bold text-blue-600 font-['Tajawal']">تم التثبيت</span>
                                        </div>
                                      </div>

                                      {/* Action Buttons Row */}
                                      <div className="flex items-center space-x-1.5 space-x-reverse pt-1 border-t border-slate-100">
                                        <button
                                          onClick={() => {
                                            setActiveGameId(game.id);
                                            setIsRomsModalOpen(false);
                                          }}
                                          className="flex-1 py-1.5 bg-[#2563EB] hover:bg-blue-700 text-white rounded-xl text-xs font-bold font-['Tajawal'] flex items-center justify-center space-x-1 space-x-reverse shadow-xs transition active:scale-95"
                                        >
                                          <Play className="w-3 h-3" />
                                          <span>تشغيل</span>
                                        </button>
                                        <button 
                                          className="w-8 h-8 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 flex items-center justify-center transition"
                                          title="خيارات إضافية"
                                        >
                                          <MoreHorizontal className="w-4 h-4" />
                                        </button>
                                      </div>
                                    </div>
                                  ))}
                              </div>
                            </div>
                          )}

                          {/* ================= TAB 2: DOWNLOAD GAMES (Image 2) ================= */}
                          {romsModalTab === 'download' && (
                            <div className="space-y-3">
                              {/* Cloudflare R2 Notice Banner */}
                              <div className="p-3 bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-2xl flex items-center justify-between text-right">
                                <div className="flex items-center space-x-2 space-x-reverse">
                                  <Cloud className="w-4 h-4 text-blue-600 shrink-0" />
                                  <span className="text-xs font-bold text-blue-900 font-['Tajawal']">خادم ألعاب Cloudflare R2 السحابي</span>
                                </div>
                                <span className="text-[10px] font-bold bg-blue-600 text-white px-2 py-0.5 rounded-full">سرعة 10Gbps</span>
                              </div>

                              {/* 2-Column Grid Matching Screenshot 2 */}
                              <div className="grid grid-cols-2 gap-3">
                                {gamesLibrary
                                  .filter(g => !romSearchQuery || g.title.includes(romSearchQuery) || g.englishTitle.toLowerCase().includes(romSearchQuery.toLowerCase()))
                                  .map((game) => (
                                    <div 
                                      key={game.id} 
                                      className="bg-white border border-slate-200/80 rounded-2xl p-2.5 shadow-xs flex flex-col justify-between hover:border-blue-300 transition"
                                    >
                                      {/* Artwork Thumbnail */}
                                      <div className="w-full h-24 rounded-xl overflow-hidden relative mb-2 bg-slate-100">
                                        <img 
                                          src={game.image} 
                                          alt={game.title} 
                                          className="w-full h-full object-cover" 
                                          referrerPolicy="no-referrer"
                                        />
                                        <div className="absolute top-1.5 right-1.5 bg-black/60 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-bold">
                                          {game.size}
                                        </div>
                                      </div>

                                      {/* Title & Size */}
                                      <div className="text-right mb-2">
                                        <h4 className="font-bold text-slate-900 text-xs font-['Tajawal'] truncate">{game.title}</h4>
                                        <div className="flex items-center space-x-1 space-x-reverse mt-1 text-[10px] text-slate-500 font-['Tajawal']">
                                          <Cloud className="w-3 h-3 text-slate-400" />
                                          <span>{game.size}</span>
                                        </div>
                                      </div>

                                      {/* Downloading Progress or Download Button */}
                                      {game.isDownloading ? (
                                        <div className="w-full bg-blue-50 border border-blue-200 rounded-xl p-2 flex flex-col items-center">
                                          <div className="flex items-center justify-between w-full text-[10px] font-bold text-blue-700 font-['Tajawal'] mb-1">
                                            <span>جارٍ التحميل...</span>
                                            <span>{game.downloadProgress}%</span>
                                          </div>
                                          <div className="w-full bg-blue-200 rounded-full h-1.5 overflow-hidden">
                                            <div 
                                              className="bg-[#2563EB] h-full rounded-full transition-all duration-300"
                                              style={{ width: `${game.downloadProgress}%` }}
                                            />
                                          </div>
                                        </div>
                                      ) : game.isDownloaded ? (
                                        <button
                                          onClick={() => {
                                            setActiveGameId(game.id);
                                            setIsRomsModalOpen(false);
                                          }}
                                          className="w-full py-1.5 bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 rounded-xl text-xs font-bold font-['Tajawal'] flex items-center justify-center space-x-1 space-x-reverse transition"
                                        >
                                          <Check className="w-3 h-3" />
                                          <span>مثبتة • تشغيل</span>
                                        </button>
                                      ) : (
                                        <button
                                          onClick={() => handleDownloadGame(game.id)}
                                          className="w-full py-1.5 bg-[#2563EB] hover:bg-blue-700 text-white rounded-xl text-xs font-bold font-['Tajawal'] flex items-center justify-center space-x-1 space-x-reverse shadow-xs transition active:scale-95"
                                        >
                                          <Download className="w-3.5 h-3.5" />
                                          <span>تحميل</span>
                                        </button>
                                      )}
                                    </div>
                                  ))}
                              </div>
                            </div>
                          )}

                          {/* ================= TAB 3: ADDED GAMES & ADMIN CLOUDFLARE R2 UPLOAD (Image 3) ================= */}
                          {romsModalTab === 'added' && (
                            <div className="space-y-3.5">
                              {/* Top Button Matching Screenshot 3: Add Game From Device */}
                              <button
                                onClick={() => setIsAddGameModalOpen(true)}
                                className="w-full py-3.5 border-2 border-dashed border-blue-300 bg-blue-50/50 hover:bg-blue-100/60 rounded-2xl flex items-center justify-center space-x-2 space-x-reverse text-[#2563EB] font-bold text-xs font-['Tajawal'] transition active:scale-98 shadow-xs"
                              >
                                <UploadCloud className="w-4 h-4" />
                                <span>إضافة لعبة من الجهاز (رفع إلى Cloudflare R2)</span>
                              </button>

                              {/* Title Section */}
                              <div className="flex items-center justify-between px-1">
                                <h4 className="text-xs font-bold text-slate-800 font-['Tajawal']">
                                  الألعاب المضافة ({gamesLibrary.filter(g => g.isAddedByAdmin).length})
                                </h4>
                                <span className="text-[10px] text-blue-600 font-bold font-['Tajawal']">إدارة المدير</span>
                              </div>

                              {/* Grid of Added Games Matching Screenshot 3 with Delete and Edit icons */}
                              <div className="grid grid-cols-2 gap-3">
                                {gamesLibrary
                                  .filter(g => g.isAddedByAdmin && (!romSearchQuery || g.title.includes(romSearchQuery)))
                                  .map((game) => (
                                    <div 
                                      key={game.id} 
                                      className="bg-white border border-slate-200 rounded-2xl p-2.5 shadow-xs flex flex-col justify-between relative"
                                    >
                                      {/* Artwork Thumbnail */}
                                      <div className="w-full h-24 rounded-xl overflow-hidden relative mb-2 bg-slate-100">
                                        <img 
                                          src={game.image} 
                                          alt={game.title} 
                                          className="w-full h-full object-cover" 
                                          referrerPolicy="no-referrer"
                                        />
                                        <div className="absolute top-1.5 right-1.5 bg-black/60 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-bold">
                                          {game.size}
                                        </div>
                                      </div>

                                      {/* Info */}
                                      <div className="text-right mb-2">
                                        <h4 className="font-bold text-slate-900 text-xs font-['Tajawal'] truncate">{game.title}</h4>
                                        <p className="text-[10px] text-slate-500 font-['Tajawal']">{game.genre}</p>
                                      </div>

                                      {/* Action Icons Row Matching Screenshot 3 */}
                                      <div className="flex items-center justify-between pt-1 border-t border-slate-100">
                                        {/* Red Trash Button (Delete from Cloudflare R2) */}
                                        <button
                                          onClick={() => {
                                            setGamesLibrary(prev => prev.filter(g => g.id !== game.id));
                                          }}
                                          className="w-8 h-8 rounded-full bg-red-50 text-red-600 hover:bg-red-100 flex items-center justify-center transition"
                                          title="حذف اللعبة من السيرفر السحابي Cloudflare R2"
                                        >
                                          <Trash2 className="w-4 h-4" />
                                        </button>

                                        {/* Blue Edit Button */}
                                        <button
                                          onClick={() => {
                                            setNewGameName(game.title);
                                            setNewGameEnglish(game.englishTitle);
                                            setIsAddGameModalOpen(true);
                                          }}
                                          className="w-8 h-8 rounded-full bg-blue-50 text-[#2563EB] hover:bg-blue-100 flex items-center justify-center transition"
                                          title="تعديل تفاصيل اللعبة والغلاف"
                                        >
                                          <Edit className="w-4 h-4" />
                                        </button>

                                        {/* Play Button */}
                                        <button
                                          onClick={() => {
                                            setActiveGameId(game.id);
                                            setIsRomsModalOpen(false);
                                          }}
                                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold font-['Tajawal'] flex items-center space-x-1 space-x-reverse"
                                        >
                                          <Play className="w-3 h-3" />
                                          <span>تشغيل</span>
                                        </button>
                                      </div>
                                    </div>
                                  ))}
                              </div>

                              {/* Guidance Box Matching Screenshot 3 */}
                              <div className="p-4 bg-[#EEF5FF] border border-[#DBEAFE] rounded-2xl flex flex-col items-center text-center">
                                <div className="w-12 h-12 rounded-full bg-white text-[#2563EB] flex items-center justify-center mb-2 shadow-xs border border-blue-100">
                                  <Gamepad2 className="w-6 h-6" />
                                </div>
                                <h5 className="font-bold text-[#1E3A8A] text-xs font-['Tajawal'] mb-1">
                                  رفع وإدارة الألعاب السحابية
                                </h5>
                                <p className="text-[11px] text-slate-600 font-['Tajawal'] leading-relaxed max-w-xs mb-2">
                                  يمكنك إضافة ألعاب من جهازك لتظهر هنا تلقائياً، ويتم حفظها سحابياً في Cloudflare R2 بدون تكلفة مساحة على هاتفك.
                                </p>
                                <div className="flex items-center space-x-1 space-x-reverse text-[10px] text-blue-700 font-bold font-['Tajawal'] bg-white px-3 py-1 rounded-full border border-blue-100">
                                  <Sparkles className="w-3 h-3 text-amber-500" />
                                  <span>تتعرف المنظومة تلقائياً على اسم اللعبة وصورتها</span>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 4. Sub-Modal: Add Game & Compress to Cloudflare R2 */}
                  {isAddGameModalOpen && (
                    <div className="fixed inset-0 z-60 bg-black/80 backdrop-blur-xs flex items-center justify-center p-3 animate-in fade-in duration-150">
                      <div className="w-full max-w-sm bg-white rounded-3xl overflow-hidden shadow-2xl p-5 text-right font-['Tajawal'] space-y-3.5 border border-slate-100">
                        <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                          <h4 className="font-bold text-slate-900 text-sm flex items-center space-x-1.5 space-x-reverse">
                            <UploadCloud className="w-4 h-4 text-blue-600" />
                            <span>رفع لعبة إلى Cloudflare R2 مع تقنية الضغط</span>
                          </h4>
                          <button 
                            onClick={() => setIsAddGameModalOpen(false)}
                            className="w-7 h-7 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center hover:bg-slate-200"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        </div>

                        {/* File Selector & CHD Compression Engine Banner */}
                        <div className="p-3 bg-blue-50 border border-blue-200 rounded-2xl text-[11px] text-blue-900 space-y-1.5">
                          <div className="flex items-center space-x-1.5 space-x-reverse font-bold text-blue-950">
                            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                            <span>تقنية ضغط متقدمة (CHD Zstandard 19)</span>
                          </div>
                          <p className="text-slate-600 leading-relaxed">
                            يتم ضغط ملفات ISO و BIN الكبيرة بنسبة تفوق <b>68%</b> تلقائياً قبل الرفع لتوفير مساحة التخزين في Cloudflare R2 وتسريع البث المباشر.
                          </p>
                        </div>

                        {/* Drag & Drop or Click File Upload */}
                        <label className="border-2 border-dashed border-blue-300 bg-blue-50/40 hover:bg-blue-100/50 rounded-2xl p-4 flex flex-col items-center justify-center cursor-pointer transition">
                          <UploadCloud className="w-8 h-8 text-blue-600 mb-1" />
                          <span className="text-xs font-bold text-slate-700">اضغط لاختيار ملف اللعبة (.iso, .bin, .chd, .cue)</span>
                          <span className="text-[10px] text-slate-400 mt-0.5">
                            {customGameFileSelected ? customGameFileSelected : 'أقصى حجم 1.5 GB لكل أسطوانة'}
                          </span>
                          <input 
                            type="file" 
                            className="hidden" 
                            accept=".iso,.bin,.chd,.cue,.img,.pbp"
                            onChange={(e) => {
                              if (e.target.files && e.target.files[0]) {
                                const file = e.target.files[0];
                                setCustomGameFileSelected(file.name);
                                setNewGameName(file.name.replace(/\.[^/.]+$/, ""));
                                setNewGameEnglish(file.name.replace(/\.[^/.]+$/, ""));
                              }
                            }}
                          />
                        </label>

                        {/* Smart Autofill Button */}
                        <button
                          onClick={handleAutoFetchMetadata}
                          disabled={isAutoFetchingMetadata}
                          className="w-full py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-xs flex items-center justify-center space-x-1.5 space-x-reverse shadow-xs transition"
                        >
                          {isAutoFetchingMetadata ? (
                            <Loader2 className="w-4 h-4 animate-spin" />
                          ) : (
                            <>
                              <Wand2 className="w-4 h-4" />
                              <span>التعرف التلقائي وجلب الغلاف (Smart Autofill)</span>
                            </>
                          )}
                        </button>

                        {/* Name & Title Inputs */}
                        <div>
                          <label className="block text-xs font-bold text-slate-700 mb-1">اسم اللعبة (عربي)</label>
                          <input 
                            type="text"
                            value={newGameName}
                            onChange={(e) => setNewGameName(e.target.value)}
                            placeholder="مثال: سباق السرعة توربو"
                            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 outline-none focus:border-blue-500"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-slate-700 mb-1">الاسم الإنجليزي</label>
                          <input 
                            type="text"
                            value={newGameEnglish}
                            onChange={(e) => setNewGameEnglish(e.target.value)}
                            placeholder="مثال: Need for Speed III"
                            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-900 outline-none focus:border-blue-500 text-left font-mono"
                          />
                        </div>

                        {/* Upload & Save Button */}
                        <button
                          onClick={() => {
                            handleAddNewGame();
                            setIsAddGameModalOpen(false);
                            setCustomGameFileSelected(null);
                          }}
                          className="w-full py-3 bg-[#2563EB] hover:bg-blue-700 text-white rounded-xl font-bold text-xs flex items-center justify-center space-x-2 space-x-reverse shadow-md transition"
                        >
                          <Cloud className="w-4 h-4" />
                          <span>ضغط الملف ورفعه إلى Cloudflare R2 وبدء التشغيل</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="flex flex-col h-full">
                  {/* Unified Header */}
                  <div className="px-5 py-3.5 flex items-center justify-between">
                    <h1 className="text-2xl font-bold text-slate-900 font-['Tajawal']">الغرف</h1>
                    <button 
                      onClick={() => setActiveTab('chats')}
                      className="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-slate-700 hover:bg-slate-50 shadow-sm transition"
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>
                  </div>

                  {/* Unified Search */}
                  <div className="px-5 py-1 mb-3">
                    <div className="flex items-center px-4 py-2.5 bg-[#EEF4FB] rounded-full">
                      <input 
                        type="text" 
                        placeholder="ابحث عن غرفة" 
                        value={roomSearch}
                        onChange={(e) => setRoomSearch(e.target.value)}
                        className="bg-transparent outline-none w-full text-sm text-slate-800 placeholder:text-slate-400 font-['Tajawal'] text-right"
                      />
                      <Search className="w-4 h-4 text-slate-400 mr-2 flex-shrink-0" />
                    </div>
                  </div>

                  {/* Section 1: الغرف النشطة */}
                  <div className="px-5 mb-5">
                    <h3 className="text-[15px] font-bold text-slate-900 font-['Tajawal'] text-right mb-2.5">الغرف النشطة</h3>
                    <div className="flex justify-start">
                      <div 
                        onClick={() => alert('غرفة أدعية وازيارات')}
                        className="bg-[#EEF4FB] border border-blue-100 rounded-full px-4 py-2 flex items-center space-x-2 space-x-reverse cursor-pointer hover:bg-blue-100 transition"
                      >
                        <span className="text-xs font-bold text-[#2563EB] font-['Tajawal']">أدعية وازيارات</span>
                        <div className="w-6 h-6 rounded-full bg-[#2563EB] text-white flex items-center justify-center">
                          <Hand className="w-3.5 h-3.5" />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Section 2: تصفح حسب المحتوى */}
                  <div className="px-5">
                    <h3 className="text-[15px] font-bold text-slate-900 font-['Tajawal'] text-right mb-3">تصفح حسب المحتوى</h3>
                    <div className="grid grid-cols-2 gap-2.5 pb-6">
                      {filteredContentRooms.map(room => {
                        const IconComponent = room.icon;
                        return (
                          <div 
                            key={room.id}
                            onClick={() => {
                              if (room.id === 'quran') setActiveSubRoom('quran');
                              else if (room.id === 'games') setActiveSubRoom('games');
                              else alert(`غرفة ${room.title}`);
                            }}
                            className="bg-[#EEF6FF] border border-[#DBEAFE] rounded-2xl p-3 flex items-center justify-between cursor-pointer hover:border-blue-400 hover:shadow-xs transition"
                          >
                            <span className="text-xs font-bold text-[#1E40AF] font-['Tajawal'] text-right flex-1 truncate pr-1">
                              {room.title}
                            </span>
                            <div className="w-8 h-8 rounded-full bg-[#2563EB] text-white flex items-center justify-center flex-shrink-0">
                              <IconComponent className="w-4 h-4" />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </main>

        {/* Bottom Navigation Dock matching Screenshot and RTL order */}
        {isLoggedIn && activeSubRoom !== 'games' && (
          <nav className="absolute bottom-0 inset-x-0 bg-white border-t border-slate-100 shadow-lg px-2 py-2 flex justify-around items-center z-40">
            {/* 1. الدردشات (Chat) - Far Right in RTL */}
            <button 
              onClick={() => {
                setIsSettingsOpen(false);
                setIsAdminOpen(false);
                setActiveTab('chats');
              }}
              className={`flex flex-col items-center justify-center px-3 py-1 rounded-xl transition ${
                activeTab === 'chats' && !isSettingsOpen && !isAdminOpen ? 'text-[#2563EB]' : 'text-slate-500'
              }`}
            >
              <MessageSquare className="w-5 h-5" />
              <span className="text-[11px] font-bold mt-1 font-['Tajawal']">الدردشات</span>
            </button>

            {/* 2. الحالات (Stories) - Next to Chats */}
            <button 
              onClick={() => {
                setIsSettingsOpen(false);
                setIsAdminOpen(false);
                setActiveTab('stories');
              }}
              className={`flex flex-col items-center justify-center px-3 py-1 rounded-xl transition ${
                activeTab === 'stories' && !isSettingsOpen && !isAdminOpen ? 'text-[#2563EB]' : 'text-slate-500'
              }`}
            >
              <Camera className="w-5 h-5" />
              <span className="text-[11px] font-bold mt-1 font-['Tajawal']">الحالات</span>
            </button>

            {/* 3. Center FAB: الغرف (Rooms) */}
            <div className="flex flex-col items-center -mt-6">
              <button 
                onClick={() => {
                  setIsSettingsOpen(false);
                  setIsAdminOpen(false);
                  setActiveSubRoom('none');
                  setActiveTab('rooms');
                }}
                className="w-14 h-14 rounded-full bg-[#2563EB] text-white flex items-center justify-center shadow-lg hover:scale-105 active:scale-95 transition"
              >
                <Gamepad2 className="w-6 h-6" />
              </button>
              <span className={`text-[11px] font-bold mt-1 font-['Tajawal'] ${activeTab === 'rooms' && !isSettingsOpen && !isAdminOpen ? 'text-[#2563EB]' : 'text-slate-500'}`}>
                الغرف
              </span>
            </div>

            {/* 4. المكالمات (Calls) */}
            <button 
              onClick={() => {
                setIsSettingsOpen(false);
                setIsAdminOpen(false);
                setActiveTab('calls');
              }}
              className={`flex flex-col items-center justify-center px-3 py-1 rounded-xl transition ${
                activeTab === 'calls' && !isSettingsOpen && !isAdminOpen ? 'text-[#2563EB]' : 'text-slate-500'
              }`}
            >
              <Phone className="w-5 h-5" />
              <span className="text-[11px] font-bold mt-1 font-['Tajawal']">المكالمات</span>
            </button>

            {/* 5. الأصدقاء (Friends) - Far Left in RTL */}
            <button 
              onClick={() => {
                setIsSettingsOpen(false);
                setIsAdminOpen(false);
                setActiveTab('friends');
              }}
              className={`flex flex-col items-center justify-center px-3 py-1 rounded-xl transition ${
                activeTab === 'friends' && !isSettingsOpen && !isAdminOpen ? 'text-[#2563EB]' : 'text-slate-500'
              }`}
            >
              <Users className="w-5 h-5" />
              <span className="text-[11px] font-bold mt-1 font-['Tajawal']">الأصدقاء</span>
            </button>
          </nav>
        )}
      </div>
    </div>
  );
};
