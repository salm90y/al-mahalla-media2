import React, { useState } from 'react';
import { NetplayLobbyRoom } from '../types';
import { netplayCoordinator } from '../services/netplayCoordinator';
import { soundFx } from '../services/audioSynthesizer';

interface NetplayLobbyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onRoomJoined?: (roomCode: string) => void;
}

const INITIAL_PUBLIC_LOBBIES: NetplayLobbyRoom[] = [
  {
    id: 'room-mena-1',
    code: 'CB94X2',
    name: 'حلبة بغداد الرياض - معركة النيترو السريعة',
    hostName: 'فارس_الصحراء',
    region: 'الشرق الأوسط (دبي/الرياض)',
    ping: 24,
    isPrivate: false,
    passwordRequired: false,
    players: 1,
    maxPlayers: 2,
    spectators: 3,
    rollbackFrames: 2,
    status: 'waiting',
  },
  {
    id: 'room-mena-2',
    code: 'CB77L9',
    name: 'تحدي كلاسيكي Combat 3 - جولات للمحترفين فقط',
    hostName: 'الأسطورة_PS1',
    region: 'الشرق الأوسط (دبي/الرياض)',
    ping: 32,
    isPrivate: false,
    passwordRequired: false,
    players: 1,
    maxPlayers: 2,
    spectators: 1,
    rollbackFrames: 2,
    status: 'waiting',
  },
  {
    id: 'room-eu-1',
    code: 'CBEU88',
    name: 'Euro-Championship Round 1 (Hardcore)',
    hostName: 'RetroRacer_EU',
    region: 'أوروبا (فرانكفورت)',
    ping: 68,
    isPrivate: false,
    passwordRequired: false,
    players: 2,
    maxPlayers: 2,
    spectators: 6,
    rollbackFrames: 3,
    status: 'in_match',
  },
  {
    id: 'room-mena-3',
    code: 'CB33K5',
    name: 'تدريب واختبار تزامن الإطارات 60FPS Netplay',
    hostName: 'المحاكي_العربي',
    region: 'الشرق الأوسط (دبي/الرياض)',
    ping: 18,
    isPrivate: true,
    passwordRequired: true,
    players: 1,
    maxPlayers: 2,
    spectators: 0,
    rollbackFrames: 1,
    status: 'waiting',
  }
];

export const NetplayLobbyModal: React.FC<NetplayLobbyModalProps> = ({
  isOpen,
  onClose,
  onRoomJoined,
}) => {
  const [lobbies, setLobbies] = useState<NetplayLobbyRoom[]>(INITIAL_PUBLIC_LOBBIES);
  const [activeTab, setActiveTab] = useState<'browser' | 'create' | 'direct_qr'>('browser');
  const [filterRegion, setFilterRegion] = useState<string>('all');
  const [quickCodeInput, setQuickCodeInput] = useState<string>('');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [copiedUrl, setCopiedUrl] = useState<boolean>(false);
  const [isJoining, setIsJoining] = useState<boolean>(false);
  const [selectedRoomForQr, setSelectedRoomForQr] = useState<NetplayLobbyRoom>(INITIAL_PUBLIC_LOBBIES[0]);

  // Create Room Form State
  const [newRoomTitle, setNewRoomTitle] = useState('معركة الردهة الجديدة - Combat 3');
  const [newRoomRegion, setNewRoomRegion] = useState('الشرق الأوسط (دبي/الرياض)');
  const [newRoomIsPrivate, setNewRoomIsPrivate] = useState(false);
  const [newRoomPassword, setNewRoomPassword] = useState('');
  const [newRoomRollback, setNewRoomRollback] = useState(2);

  if (!isOpen) return null;

  const handleQuickJoin = async (targetCode: string) => {
    const clean = targetCode.trim().toUpperCase();
    if (!clean) return;

    soundFx.playUiBlip(800);
    setIsJoining(true);

    try {
      await netplayCoordinator.joinRoom(clean);
      if (onRoomJoined) onRoomJoined(clean);
      onClose();
    } catch (e) {
      console.error('Join error:', e);
    } finally {
      setIsJoining(false);
    }
  };

  const handleCreateRoom = async () => {
    soundFx.playUiBlip(900);
    setIsJoining(true);

    try {
      const generatedCode = netplayCoordinator.generateRoomCode();
      await netplayCoordinator.createRoom(generatedCode);

      const created: NetplayLobbyRoom = {
        id: `room-${Date.now()}`,
        code: generatedCode,
        name: newRoomTitle,
        hostName: 'المضيف (أنت)',
        region: newRoomRegion,
        ping: 15,
        isPrivate: newRoomIsPrivate,
        passwordRequired: newRoomIsPrivate,
        players: 1,
        maxPlayers: 2,
        spectators: 0,
        rollbackFrames: newRoomRollback,
        status: 'waiting',
      };

      setLobbies(prev => [created, ...prev]);
      setSelectedRoomForQr(created);
      setActiveTab('direct_qr');

      if (onRoomJoined) onRoomJoined(generatedCode);
    } catch (e) {
      console.error('Create error:', e);
    } finally {
      setIsJoining(false);
    }
  };

  const handleCopyCode = (code: string) => {
    soundFx.playUiBlip(850);
    if (navigator.clipboard) {
      navigator.clipboard.writeText(code);
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(null), 1800);
    }
  };

  const handleCopyInviteUrl = (code: string) => {
    soundFx.playUiBlip(850);
    const inviteUrl = `${window.location.origin}${window.location.pathname}?room=${code}`;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(inviteUrl);
      setCopiedUrl(true);
      setTimeout(() => setCopiedUrl(false), 1800);
    }
  };

  const filteredLobbies = lobbies.filter(r => {
    if (filterRegion === 'mena') return r.region.includes('الشرق الأوسط');
    if (filterRegion === 'eu') return r.region.includes('أوروبا');
    if (filterRegion === 'open') return r.status === 'waiting' && !r.passwordRequired;
    return true;
  });

  return (
    <div 
      id="netplay-lobby-modal"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-900/60 backdrop-blur-sm animate-fade-in select-none"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-4xl max-h-[92vh] bg-white border border-slate-200 rounded-2xl shadow-xl flex flex-col overflow-hidden text-slate-800"
        onClick={e => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="px-6 py-4 border-b border-slate-200 bg-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-white shadow-sm text-sm shrink-0">
              P2P
            </div>
            <div>
              <h3 className="font-bold text-base sm:text-lg text-slate-900 flex items-center gap-2">
                <span>ردهات وغرف الألعاب المتزامنة (Combat 3 Netplay Lobbies)</span>
                <span className="text-xs bg-green-50 text-green-700 px-2.5 py-0.5 rounded-full border border-green-200 font-semibold">
                  Live Matchmaking
                </span>
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                تزامن فوري عبر WebRTC • دعم Rollback GGPO • مسح رمز QR ومشاركة الروابط
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

        {/* Navigation Tabs Bar */}
        <div className="flex border-b border-slate-200 bg-slate-50 px-6 py-2 gap-2 text-xs font-medium">
          <button
            onClick={() => { soundFx.playUiBlip(750); setActiveTab('browser'); }}
            className={`px-4 py-2 rounded-lg transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'browser'
                ? 'bg-blue-600 text-white shadow-xs font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white'
            }`}
          >
            <span>🌐</span>
            <span>تصفح الغرف العامة ({lobbies.length})</span>
          </button>

          <button
            onClick={() => { soundFx.playUiBlip(750); setActiveTab('create'); }}
            className={`px-4 py-2 rounded-lg transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'create'
                ? 'bg-blue-600 text-white shadow-xs font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white'
            }`}
          >
            <span>➕</span>
            <span>إنشاء غرفة مخصصة</span>
          </button>

          <button
            onClick={() => { soundFx.playUiBlip(750); setActiveTab('direct_qr'); }}
            className={`px-4 py-2 rounded-lg transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === 'direct_qr'
                ? 'bg-blue-600 text-white shadow-xs font-semibold'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white'
            }`}
          >
            <span>📱</span>
            <span>رمز QR & الدعوة المباشرة</span>
          </button>
        </div>

        {/* Modal Main Content */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6">
          
          {/* TAB 1: Browser */}
          {activeTab === 'browser' && (
            <div className="space-y-5">
              
              {/* Quick Join Bar & Filters */}
              <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                {/* Instant Code Entry */}
                <div className="flex items-center gap-2 flex-1">
                  <input
                    type="text"
                    maxLength={8}
                    placeholder="أدخل كود الغرفة (مثال: CB94X2)"
                    value={quickCodeInput}
                    onChange={(e) => setQuickCodeInput(e.target.value.toUpperCase())}
                    className="flex-1 px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs font-mono font-bold uppercase text-slate-800 placeholder:text-slate-400 focus:outline-hidden focus:border-blue-600"
                  />
                  <button
                    onClick={() => handleQuickJoin(quickCodeInput)}
                    disabled={!quickCodeInput.trim() || isJoining}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg text-xs font-semibold cursor-pointer shadow-xs transition-colors shrink-0"
                  >
                    {isJoining ? 'جارٍ الاتصال...' : 'انضمام فوري'}
                  </button>
                </div>

                {/* Filters */}
                <div className="flex items-center gap-1.5 shrink-0">
                  <span className="text-xs text-slate-500">تصفية:</span>
                  <button
                    onClick={() => setFilterRegion('all')}
                    className={`px-2.5 py-1.5 rounded-lg text-xs cursor-pointer ${filterRegion === 'all' ? 'bg-slate-900 text-white font-semibold' : 'bg-white border border-slate-200 text-slate-600'}`}
                  >
                    الكل
                  </button>
                  <button
                    onClick={() => setFilterRegion('mena')}
                    className={`px-2.5 py-1.5 rounded-lg text-xs cursor-pointer ${filterRegion === 'mena' ? 'bg-slate-900 text-white font-semibold' : 'bg-white border border-slate-200 text-slate-600'}`}
                  >
                    الشرق الأوسط
                  </button>
                  <button
                    onClick={() => setFilterRegion('open')}
                    className={`px-2.5 py-1.5 rounded-lg text-xs cursor-pointer ${filterRegion === 'open' ? 'bg-slate-900 text-white font-semibold' : 'bg-white border border-slate-200 text-slate-600'}`}
                  >
                    متاحة للعب
                  </button>
                </div>
              </div>

              {/* Lobbies List */}
              <div className="space-y-3">
                {filteredLobbies.map((room) => {
                  const pingColor = 
                    room.ping < 35 ? 'text-emerald-700 bg-emerald-50 border-emerald-200' :
                    room.ping < 80 ? 'text-amber-700 bg-amber-50 border-amber-200' : 'text-rose-700 bg-rose-50 border-rose-200';

                  return (
                    <div
                      key={room.id}
                      className="p-4 rounded-xl border border-slate-200 bg-white hover:border-blue-300 transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-xs"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                            {room.code}
                          </span>
                          <h4 className="font-bold text-sm text-slate-900">
                            {room.name}
                          </h4>
                          {room.passwordRequired && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold">
                              🔒 كلمة مرور
                            </span>
                          )}
                        </div>

                        <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500 font-mono">
                          <span>المضيف: <strong className="text-slate-700">{room.hostName}</strong></span>
                          <span>•</span>
                          <span>{room.region}</span>
                          <span>•</span>
                          <span>Rollback: <strong className="text-blue-600">{room.rollbackFrames}f</strong></span>
                          <span>•</span>
                          <span>المشاهدون: 👁️ {room.spectators}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 w-full md:w-auto justify-between md:justify-end shrink-0">
                        {/* Ping Badge */}
                        <div className={`px-2.5 py-1 rounded-full border text-xs font-bold font-mono flex items-center gap-1.5 ${pingColor}`}>
                          <span className="w-1.5 h-1.5 rounded-full bg-current" />
                          <span>{room.ping} ms</span>
                        </div>

                        {/* QR Code Quick View */}
                        <button
                          onClick={() => {
                            soundFx.playUiBlip(800);
                            setSelectedRoomForQr(room);
                            setActiveTab('direct_qr');
                          }}
                          title="عرض رمز QR ومشاركة الرابط"
                          className="p-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer transition-colors"
                        >
                          📱
                        </button>

                        {/* Join / Spectate Button */}
                        {room.status === 'waiting' ? (
                          <button
                            onClick={() => handleQuickJoin(room.code)}
                            disabled={isJoining}
                            className="px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold text-xs shadow-xs transition-colors cursor-pointer"
                          >
                            انضمام للقتال (P2)
                          </button>
                        ) : (
                          <button
                            onClick={() => handleQuickJoin(room.code)}
                            className="px-4 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-xs cursor-pointer"
                          >
                            مشاهدة البث (Spectate)
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

            </div>
          )}

          {/* TAB 2: Create Custom Room */}
          {activeTab === 'create' && (
            <div className="max-w-xl mx-auto space-y-5">
              <div className="text-center space-y-1">
                <h4 className="font-bold text-base text-slate-900">إنشاء غرفة نيت بلاي جديدة ومخصصة</h4>
                <p className="text-xs text-slate-500">ستكون أنت المضيف (Host P1) وسيتصل بك المنافس مباشرة عبر تقنية WebRTC</p>
              </div>

              <div className="space-y-4 p-5 rounded-2xl border border-slate-200 bg-slate-50">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-800">عنوان واسم الغرفة:</label>
                  <input
                    type="text"
                    value={newRoomTitle}
                    onChange={(e) => setNewRoomTitle(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs text-slate-800 focus:outline-hidden focus:border-blue-600"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-800">منطقة وسيرفر اللعب (Server Region):</label>
                  <select
                    value={newRoomRegion}
                    onChange={(e) => setNewRoomRegion(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs text-slate-800 focus:outline-hidden focus:border-blue-600"
                  >
                    <option value="الشرق الأوسط (دبي/الرياض)">الشرق الأوسط والخليج (دبي / الرياض) - أقل زمن استجابة</option>
                    <option value="أوروبا (فرانكفورت)">أوروبا (فرانكفورت)</option>
                    <option value="أمريكا الشمالية">أمريكا الشمالية (شرق)</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between items-center text-xs">
                    <span className="font-bold text-slate-800">تخزين الـ Rollback المؤقت (Frames Delay Buffer):</span>
                    <span className="font-mono text-blue-600 font-bold">{newRoomRollback} إطارات (~{newRoomRollback * 16}ms)</span>
                  </div>
                  <input
                    type="range"
                    min="1"
                    max="4"
                    step="1"
                    value={newRoomRollback}
                    onChange={(e) => setNewRoomRollback(parseInt(e.target.value))}
                    className="w-full accent-blue-600 cursor-pointer h-2 bg-slate-200 rounded-lg"
                  />
                </div>

                <div className="pt-2 border-t border-slate-200 flex items-center justify-between">
                  <div className="space-y-0.5">
                    <label className="text-xs font-bold text-slate-800">غرفة خاصة برقم سري (Private Match)</label>
                    <p className="text-[11px] text-slate-500">منع الدخول العشوائي وقصرها على المدعوين فقط</p>
                  </div>
                  <input
                    type="checkbox"
                    checked={newRoomIsPrivate}
                    onChange={(e) => setNewRoomIsPrivate(e.target.checked)}
                    className="w-4 h-4 accent-blue-600 rounded cursor-pointer"
                  />
                </div>

                {newRoomIsPrivate && (
                  <div className="space-y-1.5 pt-2">
                    <label className="text-xs font-bold text-slate-800">كلمة المرور / الرمز السري:</label>
                    <input
                      type="password"
                      placeholder="أدخل رمز المرور..."
                      value={newRoomPassword}
                      onChange={(e) => setNewRoomPassword(e.target.value)}
                      className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs text-slate-800 focus:outline-hidden focus:border-blue-600"
                    />
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setActiveTab('browser')}
                  className="px-4 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  onClick={handleCreateRoom}
                  disabled={isJoining}
                  className="px-6 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-xs font-bold shadow-sm shadow-blue-200 cursor-pointer transition-colors"
                >
                  {isJoining ? 'جارٍ تهيئة الغرفة...' : 'إنشاء الغرفة وبدء الاستضافة 🚀'}
                </button>
              </div>
            </div>
          )}

          {/* TAB 3: Direct QR & Invite Share */}
          {activeTab === 'direct_qr' && selectedRoomForQr && (
            <div className="max-w-xl mx-auto space-y-6 text-center">
              <div>
                <h4 className="font-bold text-base text-slate-900">مشاركة الغرفة عبر رمز QR ورابط الدعوة المباشر</h4>
                <p className="text-xs text-slate-500 mt-0.5">يمكن لصديقك مسح الكود بكاميرا هاتفه للدخول فورياً إلى المعركة</p>
              </div>

              {/* QR Code Container */}
              <div className="inline-flex flex-col items-center p-6 bg-white border border-slate-200 rounded-2xl shadow-sm space-y-3">
                
                {/* SVG Visual QR Matrix */}
                <div className="w-48 h-48 bg-white p-2 rounded-xl border border-slate-200 flex items-center justify-center relative">
                  <svg viewBox="0 0 100 100" className="w-full h-full text-slate-900">
                    {/* Corner Finders */}
                    <rect x="5" y="5" width="28" height="28" rx="4" fill="none" stroke="currentColor" strokeWidth="6" />
                    <rect x="13" y="13" width="12" height="12" rx="2" fill="currentColor" />

                    <rect x="67" y="5" width="28" height="28" rx="4" fill="none" stroke="currentColor" strokeWidth="6" />
                    <rect x="75" y="13" width="12" height="12" rx="2" fill="currentColor" />

                    <rect x="5" y="67" width="28" height="28" rx="4" fill="none" stroke="currentColor" strokeWidth="6" />
                    <rect x="13" y="75" width="12" height="12" rx="2" fill="currentColor" />

                    {/* QR Code Data Pattern Grid */}
                    <rect x="40" y="8" width="6" height="6" fill="currentColor" />
                    <rect x="52" y="8" width="6" height="6" fill="currentColor" />
                    <rect x="40" y="20" width="6" height="6" fill="currentColor" />
                    <rect x="48" y="26" width="6" height="6" fill="currentColor" />
                    <rect x="8" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="20" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="28" y="48" width="6" height="6" fill="currentColor" />
                    <rect x="40" y="40" width="8" height="8" rx="1" fill="#2563EB" />
                    <rect x="52" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="64" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="76" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="88" y="40" width="6" height="6" fill="currentColor" />
                    <rect x="40" y="52" width="6" height="6" fill="currentColor" />
                    <rect x="52" y="52" width="6" height="6" fill="currentColor" />
                    <rect x="64" y="52" width="6" height="6" fill="currentColor" />
                    <rect x="76" y="52" width="6" height="6" fill="currentColor" />
                    <rect x="40" y="64" width="6" height="6" fill="currentColor" />
                    <rect x="52" y="64" width="6" height="6" fill="currentColor" />
                    <rect x="64" y="76" width="6" height="6" fill="currentColor" />
                    <rect x="76" y="68" width="6" height="6" fill="currentColor" />
                    <rect x="88" y="76" width="6" height="6" fill="currentColor" />
                    <rect x="40" y="86" width="6" height="6" fill="currentColor" />
                    <rect x="52" y="80" width="6" height="6" fill="currentColor" />
                    <rect x="64" y="86" width="6" height="6" fill="currentColor" />
                  </svg>
                  {/* PS1 Center Emblem */}
                  <div className="absolute w-8 h-8 rounded-lg bg-blue-600 text-white font-bold text-[10px] flex items-center justify-center shadow-md">
                    PS1
                  </div>
                </div>

                <div className="space-y-1">
                  <div className="font-mono text-xl font-extrabold text-blue-600 tracking-wider">
                    {selectedRoomForQr.code}
                  </div>
                  <div className="text-xs text-slate-500">{selectedRoomForQr.name}</div>
                </div>

                {/* Copy Buttons */}
                <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
                  <button
                    onClick={() => handleCopyCode(selectedRoomForQr.code)}
                    className="px-4 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold cursor-pointer transition-colors flex items-center gap-1.5"
                  >
                    <span>📋</span>
                    <span>{copiedCode === selectedRoomForQr.code ? 'تم نسخ الكود!' : 'نسخ الكود'}</span>
                  </button>

                  <button
                    onClick={() => handleCopyInviteUrl(selectedRoomForQr.code)}
                    className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold cursor-pointer shadow-xs transition-colors flex items-center gap-1.5"
                  >
                    <span>🔗</span>
                    <span>{copiedUrl ? 'تم نسخ رابط الدعوة!' : 'نسخ رابط الدعوة المباشر'}</span>
                  </button>
                </div>
              </div>

              <div className="text-xs text-slate-400 font-mono">
                كود الغرفة صالح لجلسة اللعب الحالية ومُجهّز للاتصال المباشر بين الأجهزة
              </div>
            </div>
          )}

        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3.5 border-t border-slate-200 bg-slate-50 flex justify-between items-center text-xs text-slate-500 font-mono">
          <span>Netplay Engine v3.4 • P2P Mesh Network</span>
          <button
            onClick={onClose}
            className="px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white font-medium rounded-lg cursor-pointer transition-colors text-xs"
          >
            إغلاق
          </button>
        </div>

      </div>
    </div>
  );
};
