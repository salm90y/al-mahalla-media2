import React, { useState } from 'react';
import {
  Users,
  UserPlus,
  Search,
  Edit2,
  Trash2,
  X,
  CheckCircle2,
  Lock,
  Mail,
  User,
  Shield,
  Eye,
  EyeOff
} from 'lucide-react';
import { soundFx } from '../services/audioSynthesizer';

interface UserAccount {
  id: string;
  name: string;
  username: string;
  email: string;
  role: string;
  status: 'active' | 'inactive';
  avatarEmoji: string;
}

interface SimulatedAdminAccountsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SimulatedAdminAccountsModal: React.FC<SimulatedAdminAccountsModalProps> = ({
  isOpen,
  onClose
}) => {
  const [accounts, setAccounts] = useState<UserAccount[]>([
    { id: '1', name: 'أحمد خالد', username: 'ahmed', email: 'ahmed1986y5@gmail.com', role: 'مشرف النظام', status: 'active', avatarEmoji: '🧔🏻' },
    { id: '2', name: 'سارة', username: 'sarah', email: 'sarah@example.com', role: 'عضو', status: 'active', avatarEmoji: '👩🏻' },
    { id: '3', name: 'فريق العمل', username: 'team', email: 'team@example.com', role: 'فريق دعم', status: 'active', avatarEmoji: '👥' },
    { id: '4', name: 'نورا', username: 'noura', email: 'noura@example.com', role: 'عضو', status: 'active', avatarEmoji: '🧕🏻' },
    { id: '5', name: 'خالد', username: 'khaled', email: 'khaled@example.com', role: 'عضو', status: 'active', avatarEmoji: '🤓' },
    { id: '6', name: 'ليلى', username: 'layla', email: 'layla@example.com', role: 'عضو', status: 'active', avatarEmoji: '👩🏻‍🏫' },
  ]);

  const [searchQuery, setSearchQuery] = useState('');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Create form state
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState('عضو');
  const [selectedAvatar, setSelectedAvatar] = useState('🧔🏻');
  const [showPassword, setShowPassword] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  if (!isOpen) return null;

  const showToast = (msg: string) => {
    soundFx.playUiBlip(850);
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 2800);
  };

  const filteredAccounts = accounts.filter(acc =>
    acc.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    acc.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
    acc.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
    acc.role.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSaveAccount = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || username.trim().length < 3) {
      showToast('اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل');
      return;
    }
    if (!password || password.length < 4) {
      showToast('كلمة المرور يجب أن تتكون من 4 أحرف/أرقام على الأقل');
      return;
    }
    if (password !== confirmPassword) {
      showToast('كلمة المرور وتأكيدها غير متطابقين');
      return;
    }

    setIsSaving(true);
    soundFx.playUiBlip(900);

    setTimeout(() => {
      const newAcc: UserAccount = {
        id: `u_${Date.now()}`,
        name: fullName.trim() || username.trim(),
        username: username.trim().toLowerCase(),
        email: email.trim(),
        role: role,
        status: 'active',
        avatarEmoji: selectedAvatar
      };

      setAccounts(prev => [newAcc, ...prev]);
      setIsSaving(false);
      setIsCreateModalOpen(false);
      
      // Reset form
      setFullName('');
      setUsername('');
      setEmail('');
      setPassword('');
      setConfirmPassword('');
      showToast('تم حفظ الحساب الجديد بنجاح في قاعدة البيانات!');
    }, 450);
  };

  const handleDeleteAccount = (id: string, name: string) => {
    soundFx.playUiBlip(500);
    setAccounts(prev => prev.filter(a => a.id !== id));
    showToast(`تم حذف حساب ${name}`);
  };

  const avatarsList = ['🧔🏻', '👩🏻', '👥', '🧕🏻', '🤓', '👩🏻‍🏫', '🎮', '⚡'];

  return (
    <div
      id="simulated-admin-accounts-modal"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 select-none"
      dir="rtl"
    >
      <div className="relative w-full max-w-2xl bg-slate-900 border border-cyan-500/30 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Top Header */}
        <div className="px-6 py-4 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-400/30 flex items-center justify-center text-cyan-400 shadow-sm">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <span>المَحَلَّة</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30">
                  لوحة التحكم
                </span>
              </h2>
              <p className="text-xs text-slate-400">إدارة الحسابات المسجلة في النظام</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Bar & Dedicated Elegant Create Button */}
        <div className="px-6 py-3 bg-slate-950/50 border-b border-slate-800 flex items-center justify-between gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="بحث عن مستخدم..."
              className="w-full bg-slate-900/90 border border-slate-700/60 rounded-xl pr-9 pl-4 py-2 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400 transition-colors"
            />
          </div>

          {/* زر صغير أنيق لفتح نافذة إنشاء الحساب */}
          <button
            id="btn-admin-open-create-modal"
            onClick={() => {
              soundFx.playUiBlip(800);
              setIsCreateModalOpen(true);
            }}
            className="px-4 py-2 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 border border-cyan-400/40 text-xs font-bold transition-all shadow-sm shadow-cyan-900/30 flex items-center gap-1.5 cursor-pointer shrink-0 active:scale-95"
          >
            <UserPlus className="w-4 h-4 text-cyan-400" />
            <span>+ حساب جديد</span>
          </button>
        </div>

        {/* Accounts Grid (3 Columns) */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-400 px-1">
            <span>قائمة الحسابات ({filteredAccounts.length})</span>
            <span>انقر على "+ حساب جديد" لفتح نموذج الإضافة</span>
          </div>

          {filteredAccounts.length === 0 ? (
            <div className="py-12 text-center text-slate-500 text-xs">
              لم يتم العثور على أي حساب يطابق البحث
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {filteredAccounts.map((account) => (
                <div
                  key={account.id}
                  className="bg-slate-800/40 border border-slate-700/60 hover:border-cyan-500/40 rounded-2xl p-4 flex flex-col items-center text-center transition-all duration-200 group relative"
                >
                  <div className="w-14 h-14 rounded-full bg-slate-800 border-2 border-cyan-400/40 flex items-center justify-center text-2xl mb-2 shadow-md shadow-cyan-500/10 group-hover:scale-105 transition-transform">
                    {account.avatarEmoji}
                  </div>

                  <h3 className="text-xs font-bold text-white truncate w-full mb-0.5">
                    {account.name}
                  </h3>
                  <p className="text-[11px] text-cyan-400/80 font-mono truncate w-full mb-1">
                    @{account.username}
                  </p>
                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-700/50 text-slate-300 border border-slate-600/40 mb-3">
                    {account.role}
                  </span>

                  {/* Actions (Edit | Delete) */}
                  <div className="w-full flex items-center gap-1.5 pt-2 border-t border-slate-700/50">
                    <button
                      onClick={() => showToast(`تعديل بيانات: ${account.name}`)}
                      className="flex-1 py-1 rounded-lg bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 text-[10px] font-semibold transition-colors cursor-pointer flex items-center justify-center gap-1"
                    >
                      <Edit2 className="w-3 h-3" />
                      <span>تعديل</span>
                    </button>
                    <button
                      onClick={() => handleDeleteAccount(account.id, account.name)}
                      className="flex-1 py-1 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30 text-[10px] font-semibold transition-colors cursor-pointer flex items-center justify-center gap-1"
                    >
                      <Trash2 className="w-3 h-3" />
                      <span>حذف</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer Note */}
        <div className="px-6 py-3 bg-slate-950/90 border-t border-slate-800 text-[11px] text-slate-400 flex items-center justify-between">
          <span className="flex items-center gap-1.5 text-emerald-400">
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>متصل بسحابة Cloudflare D1</span>
          </span>
          <span className="text-slate-500 font-mono">DB: d1-users-table</span>
        </div>
      </div>

      {/* ========================================================= */}
      {/* نافذة أو شاشة منبثقة مستقلة لإنشاء الحساب (Create Account Modal) */}
      {/* ========================================================= */}
      {isCreateModalOpen && (
        <div
          id="modal-create-account-subwindow"
          className="fixed inset-0 z-60 bg-black/75 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in zoom-in-95 duration-150"
        >
          <div className="w-full max-w-md bg-slate-900 border-2 border-cyan-400/50 rounded-3xl p-6 shadow-2xl relative max-h-[92vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between mb-5">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-300">
                  <UserPlus className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-white">إنشاء حساب جديد</h3>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="p-1.5 rounded-lg bg-slate-800 text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Avatar Emoji Selector */}
            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-2">
                اختر الصورة الرمزية:
              </label>
              <div className="flex items-center justify-center gap-2 bg-slate-950/60 p-2 rounded-2xl border border-slate-800">
                {avatarsList.map((emoji) => (
                  <button
                    key={emoji}
                    type="button"
                    onClick={() => setSelectedAvatar(emoji)}
                    className={`w-9 h-9 rounded-xl flex items-center justify-center text-lg transition-all cursor-pointer ${
                      selectedAvatar === emoji
                        ? 'bg-cyan-500/30 border-2 border-cyan-400 scale-110 shadow-md shadow-cyan-500/20'
                        : 'bg-slate-800/60 hover:bg-slate-700/60 border border-transparent'
                    }`}
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleSaveAccount} className="space-y-3">
              <div>
                <label className="block text-xs text-slate-300 mb-1">الاسم الكامل</label>
                <div className="relative">
                  <User className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="مثال: أحمد الحلفي"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-3 py-2.5 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">اسم المستخدم (اليوزر) *</label>
                <div className="relative">
                  <span className="text-cyan-400 absolute right-3.5 top-1/2 -translate-y-1/2 font-bold text-xs">@</span>
                  <input
                    type="text"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="مثال: ahmed1986"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-3 py-2.5 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">البريد الإلكتروني (اختياري)</label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="user@example.com"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-3 py-2.5 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400 font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">الدور / الصلاحية</label>
                <div className="relative">
                  <Shield className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  <select
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-3 py-2.5 text-xs text-white focus:outline-hidden focus:border-cyan-400"
                  >
                    <option value="عضو">عضو</option>
                    <option value="مشرف">مشرف</option>
                    <option value="فريق دعم">فريق دعم</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">كلمة المرور *</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-9 py-2.5 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-cyan-400 cursor-pointer"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">تأكيد كلمة المرور *</label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-cyan-400 absolute right-3 top-1/2 -translate-y-1/2" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl pr-9 pl-3 py-2.5 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-cyan-400"
                  />
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="flex-1 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
                >
                  إلغاء
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="flex-1 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 active:scale-95 text-slate-950 text-xs font-bold transition-all shadow-md shadow-cyan-500/20 cursor-pointer flex items-center justify-center gap-1.5"
                >
                  {isSaving ? (
                    <span>جاري الحفظ...</span>
                  ) : (
                    <>
                      <span>حفظ الحساب</span>
                      <span>💾</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-slate-900 border border-cyan-400/50 text-white text-xs px-4 py-2.5 rounded-xl shadow-2xl z-70 flex items-center gap-2 animate-in fade-in slide-in-from-bottom-3 duration-200">
          <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  );
};
