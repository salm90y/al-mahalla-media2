import React, { useState } from 'react';
import {
  ArrowRight,
  MoreVertical,
  MessageSquare,
  Phone,
  Video,
  MicOff,
  Handshake,
  Folder,
  Image as ImageIcon,
  Link as LinkIcon,
  Fingerprint,
  Shield,
  Trash2,
  Share2,
  QrCode,
  Search,
  Bell,
  Clock,
  UserX,
  Flag
} from 'lucide-react';
import { soundFx } from '../services/audioSynthesizer';

interface SimulatedFriendProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SimulatedFriendProfileModal: React.FC<SimulatedFriendProfileModalProps> = ({
  isOpen,
  onClose
}) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [customNotifs, setCustomNotifs] = useState(true);
  const [mediaVisibility, setMediaVisibility] = useState(true);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const showToast = (msg: string) => {
    soundFx.playUiBlip(850);
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 2800);
  };

  const handleMenuItemClick = (title: string, actionMsg: string) => {
    setIsMenuOpen(false);
    showToast(actionMsg);
  };

  return (
    <div
      id="friend-profile-modal-root"
      className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-2 sm:p-4 select-none"
      dir="rtl"
    >
      {/* Phone container frame matching the screenshot */}
      <div
        id="friend-profile-phone-frame"
        className="relative w-full max-w-[390px] h-[92vh] max-h-[844px] bg-[#080C15] text-slate-100 rounded-[38px] border-[6px] border-slate-900 shadow-2xl flex flex-col overflow-hidden ring-1 ring-white/10"
        style={{
          backgroundImage:
            'radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.12) 0%, transparent 40%), radial-gradient(circle at 90% 80%, rgba(6, 182, 212, 0.15) 0%, transparent 45%), radial-gradient(circle at 50% 50%, rgba(139, 92, 246, 0.08) 0%, transparent 60%)'
        }}
      >
        {/* Android / Phone Status Bar */}
        <div className="h-8 px-6 flex items-center justify-between text-xs text-slate-300 shrink-0 select-none z-20">
          <span className="font-semibold text-slate-200 tracking-tight">9:41</span>
          <div className="flex items-center gap-1.5 text-[11px]">
            <span>📶</span>
            <span>📡</span>
            <span className="font-mono text-emerald-400 font-bold">78% 🔋</span>
          </div>
        </div>

        {/* Scrollable Screen Content (The First Design - Main Profile) */}
        <div className="flex-1 overflow-y-auto px-4 pb-6 space-y-4 relative scrollbar-none">
          
          {/* AppBar: زر رجوع يمين، عنوان الملف الشخصي وسط، زر ثلاث نقاط عمودية يسار بستايل نيون */}
          <div className="flex items-center justify-between pt-1 pb-2">
            {/* Right: زر الرجوع (يمين في RTL) */}
            <button
              id="profile-back-button"
              onClick={() => {
                soundFx.playUiBlip(700);
                onClose();
              }}
              title="رجوع"
              className="w-10 h-10 rounded-full bg-slate-900/60 border border-cyan-400/40 hover:border-cyan-400 text-cyan-400 flex items-center justify-center transition-colors cursor-pointer shadow-sm"
            >
              <ArrowRight className="w-5 h-5" />
            </button>

            {/* Middle: عنوان الملف الشخصي */}
            <h1 className="text-base font-bold text-white tracking-wide">
              الملف الشخصي
            </h1>

            {/* Left: زر ثلاث نقاط عمودية بستايل دائري نيون (يسار في RTL) */}
            <button
              id="profile-more-menu-button"
              onClick={() => {
                soundFx.playUiBlip(900);
                setIsMenuOpen(prev => !prev);
              }}
              title="المزيد من الخيارات"
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-all cursor-pointer shadow-sm ${
                isMenuOpen
                  ? 'bg-cyan-950 border-2 border-cyan-400 text-cyan-300 shadow-cyan-500/30'
                  : 'bg-slate-900/60 border border-cyan-400/50 hover:border-cyan-400 text-cyan-400 shadow-cyan-500/20'
              }`}
            >
              <MoreVertical className="w-5 h-5" />
            </button>
          </div>

          {/* صورة دائرية 110dp مع حلقة نيون ونقطة خضراء متصل الآن */}
          <div className="flex flex-col items-center justify-center pt-2">
            <div className="relative w-[110px] h-[110px] rounded-full p-[3px] bg-gradient-to-tr from-cyan-400 via-sky-400 to-indigo-500 shadow-lg shadow-cyan-500/25">
              <div className="w-full h-full rounded-full overflow-hidden bg-slate-900 border-2 border-[#080C15]">
                <img
                  src="/src/assets/images/avatar_ahmed_1788900731473.jpg"
                  alt="أحمد الحلفي"
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    // Fallback to stylized SVG avatar if image cannot load
                    (e.target as HTMLElement).style.display = 'none';
                  }}
                />
              </div>

              {/* نقطة خضراء متصل الآن */}
              <div className="absolute bottom-1 end-1 w-5 h-5 rounded-full bg-emerald-500 border-2 border-[#080C15] flex items-center justify-center shadow-md">
                <div className="w-2 h-2 rounded-full bg-white/70 animate-pulse" />
              </div>
            </div>

            {/* الاسم: أحمد الحلفي (bold) */}
            <h2 className="mt-3 text-lg font-black text-white tracking-tight">
              أحمد الحلفي
            </h2>

            {/* حالة متصل الآن أخضر */}
            <div className="flex items-center gap-1.5 mt-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span className="text-xs font-semibold text-emerald-400">
                متصل الآن
              </span>
            </div>

            {/* اليوزر: @ahmed */}
            <span className="text-xs text-slate-400 mt-0.5 font-mono">
              @ahmed
            </span>

            {/* نص BIO: عاشق ألعاب القتال، قائد فريق | المستوى: ماسي */}
            <p className="text-xs text-slate-300/90 text-center mt-2 px-4 leading-relaxed font-medium">
              عاشق ألعاب القتال، قائد فريق | المستوى: ماسي
            </p>
          </div>

          {/* صف 4 أزرار بنفس ستايل أزرار التطبيق: كتم - اتصال فيديو - اتصال صوتي - رسالة */}
          <div className="grid grid-cols-4 gap-2 pt-1">
            {/* 1. رسالة (Right in RTL) */}
            <button
              onClick={() => showToast('فتح المحادثة الفورية مع أحمد')}
              className="h-[74px] rounded-2xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800/80 hover:border-cyan-500/40 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer shadow-sm group"
            >
              <div className="w-8 h-8 rounded-full bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-cyan-400 group-hover:scale-110 transition-transform">
                <MessageSquare className="w-4 h-4" />
              </div>
              <span className="text-[11px] font-bold text-white">رسالة</span>
            </button>

            {/* 2. اتصال صوتي */}
            <button
              onClick={() => showToast('بدء اتصال صوتي مشفر...')}
              className="h-[74px] rounded-2xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800/80 hover:border-cyan-500/40 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer shadow-sm group"
            >
              <div className="w-8 h-8 rounded-full bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-cyan-400 group-hover:scale-110 transition-transform">
                <Phone className="w-4 h-4" />
              </div>
              <span className="text-[11px] font-bold text-white">اتصال صوتي</span>
            </button>

            {/* 3. اتصال فيديو */}
            <button
              onClick={() => showToast('بدء مكالمة فيديو عالية الدقة...')}
              className="h-[74px] rounded-2xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800/80 hover:border-cyan-500/40 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer shadow-sm group"
            >
              <div className="w-8 h-8 rounded-full bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-cyan-400 group-hover:scale-110 transition-transform">
                <Video className="w-4 h-4" />
              </div>
              <span className="text-[11px] font-bold text-white">اتصال فيديو</span>
            </button>

            {/* 4. كتم (Left in RTL) */}
            <button
              onClick={() => showToast('تم كتم إشعارات الصوت والميكروفون')}
              className="h-[74px] rounded-2xl bg-slate-900/80 hover:bg-slate-800/90 border border-slate-800/80 hover:border-cyan-500/40 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer shadow-sm group"
            >
              <div className="w-8 h-8 rounded-full bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-cyan-400 group-hover:scale-110 transition-transform">
                <MicOff className="w-4 h-4" />
              </div>
              <span className="text-[11px] font-bold text-white">كتم</span>
            </button>
          </div>

          {/* كاردات بنفس ستايل كاردات التطبيق الحالي (2 أعمدة × 3 صفوف) */}
          <div className="space-y-2.5">
            {/* الصف 1: تاريخ الصداقة (يمين) - الهاتف (يسار) */}
            <div className="grid grid-cols-2 gap-2.5">
              <div
                onClick={() => showToast('تاريخ بدء الصداقة: 12 مارس 2023')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">تاريخ الصداقة</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">صديق منذ 12 مارس 2023</p>
                </div>
                <Handshake className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>

              <div
                onClick={() => showToast('رقم الهاتف: 966 5X XXX 2087')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">الهاتف</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5 font-mono dir-ltr text-right">966 5X XXX 2087</p>
                </div>
                <Phone className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>
            </div>

            {/* الصف 2: الصور (يمين) - الملفات (يسار) */}
            <div className="grid grid-cols-2 gap-2.5">
              <div
                onClick={() => showToast('عرض 12 صورة وفيديو مشترك')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">الصور</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">عنصر 12</p>
                </div>
                <ImageIcon className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>

              <div
                onClick={() => showToast('عرض 5 ملفات مشتركة')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">الملفات</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">عناصر 5</p>
                </div>
                <Folder className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>
            </div>

            {/* الصف 3: البصمات (يمين) - الروابط (يسار) */}
            <div className="grid grid-cols-2 gap-2.5">
              <div
                onClick={() => showToast('عرض 8 بصمات ومقاطع صوتية')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">البصمات</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">مقاطع 8</p>
                </div>
                <Fingerprint className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>

              <div
                onClick={() => showToast('عرض 6 روابط مشتركة')}
                className="h-[68px] rounded-2xl bg-slate-900/85 border border-slate-800/90 hover:border-cyan-500/30 p-3 flex items-center justify-between cursor-pointer transition-colors shadow-xs"
              >
                <div>
                  <h4 className="text-xs font-bold text-white">الروابط</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5">روابط 6</p>
                </div>
                <LinkIcon className="w-5 h-5 text-cyan-400 shrink-0" />
              </div>
            </div>
          </div>

          {/* سويتشات: إشعارات مخصصة، ظهور الوسائط */}
          <div className="space-y-2.5">
            {/* سويتش 1: إشعارات مخصصة */}
            <div className="h-13 rounded-2xl bg-slate-900/85 border border-slate-800/90 px-4 flex items-center justify-between shadow-xs">
              <span className="text-xs font-bold text-white">إشعارات مخصصة</span>
              <button
                onClick={() => {
                  soundFx.playUiBlip(750);
                  setCustomNotifs(prev => !prev);
                  showToast(`الإشعارات المخصصة: ${!customNotifs ? 'مفعلة' : 'معطلة'}`);
                }}
                className={`w-12 h-6 rounded-full p-0.5 transition-colors cursor-pointer flex items-center ${
                  customNotifs ? 'bg-emerald-500 justify-start' : 'bg-slate-700 justify-end'
                }`}
              >
                <div className="w-5 h-5 rounded-full bg-white shadow-sm" />
              </button>
            </div>

            {/* سويتش 2: ظهور الوسائط */}
            <div className="h-13 rounded-2xl bg-slate-900/85 border border-slate-800/90 px-4 flex items-center justify-between shadow-xs">
              <span className="text-xs font-bold text-white">ظهور الوسائط</span>
              <button
                onClick={() => {
                  soundFx.playUiBlip(750);
                  setMediaVisibility(prev => !prev);
                  showToast(`ظهور الوسائط: ${!mediaVisibility ? 'مفعل' : 'معطل'}`);
                }}
                className={`w-12 h-6 rounded-full p-0.5 transition-colors cursor-pointer flex items-center ${
                  mediaVisibility ? 'bg-cyan-500 justify-start' : 'bg-slate-700 justify-end'
                }`}
              >
                <div className="w-5 h-5 rounded-full bg-white shadow-sm" />
              </button>
            </div>
          </div>

          {/* أزرار حمراء: حذف الصديق، حظر - بنفس ستايل أزرار الخطر في التطبيق */}
          <div className="grid grid-cols-2 gap-3 pt-1">
            {/* زر حظر (يمين في RTL) */}
            <button
              onClick={() => showToast('تم حظر المستخدم أحمد الحلفي')}
              className="h-12 rounded-2xl bg-[#EF4444] hover:bg-[#DC2626] text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md shadow-red-950/40 transition-colors cursor-pointer"
            >
              <Shield className="w-4 h-4" />
              <span>حظر</span>
            </button>

            {/* زر حذف الصديق (يسار في RTL) */}
            <button
              onClick={() => showToast('تم حذف الصديق بنجاح')}
              className="h-12 rounded-2xl bg-[#EF4444] hover:bg-[#DC2626] text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md shadow-red-950/40 transition-colors cursor-pointer"
            >
              <Trash2 className="w-4 h-4" />
              <span>حذف الصديق</span>
            </button>
          </div>
        </div>

        {/* ========================================================================= */}
        {/* التصميم الثاني - قائمة الثلاث نقاط - يظهر فوق التصميم الأول كـ Overlay     */}
        {/* زجاجية GlassCard بعرض 280dp تحت الزر مباشرة، مع تعتيم خفيف 30%           */}
        {/* ========================================================================= */}
        {isMenuOpen && (
          <div
            id="menu-overlay-backdrop"
            onClick={() => setIsMenuOpen(false)}
            className="absolute inset-0 z-40 bg-black/30 backdrop-blur-[1px] transition-opacity"
          >
            {/* The 280dp GlassCard Popup Menu positioned right under the 3-dots button (top left in RTL) */}
            <div
              id="menu-profile-more-popup"
              onClick={(e) => e.stopPropagation()}
              className="absolute top-14 start-4 w-[280px] rounded-2xl bg-[#0F172A]/95 backdrop-blur-md border border-purple-500/60 shadow-2xl p-2.5 space-y-0.5 z-50 text-right animate-in fade-in zoom-in-95 duration-150"
            >
              {/* 1. مشاركة الملف الشخصي */}
              <button
                onClick={() => handleMenuItemClick('مشاركة', 'تم نسخ رابط مشاركة الملف الشخصي')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>مشاركة الملف الشخصي</span>
                <Share2 className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 2. عرض رمز QR */}
              <button
                onClick={() => handleMenuItemClick('QR', 'عرض رمز QR الخاص بـ @ahmed')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>عرض رمز QR</span>
                <QrCode className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 3. بحث */}
              <button
                onClick={() => handleMenuItemClick('بحث', 'فتح البحث في الرسائل مع أحمد')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>بحث</span>
                <Search className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 4. كتم الإشعارات */}
              <button
                onClick={() => handleMenuItemClick('كتم', 'تم كتم إشعارات المحادثة')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>كتم الإشعارات</span>
                <Bell className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 5. إشعارات مخصصة */}
              <button
                onClick={() => handleMenuItemClick('إشعارات', 'فتح إعدادات النغمات والاهتزاز')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>إشعارات مخصصة</span>
                <Bell className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 6. ظهور الوسائط */}
              <button
                onClick={() => handleMenuItemClick('وسائط', 'إعدادات حفظ الصور في الهاتف')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>ظهور الوسائط</span>
                <ImageIcon className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 7. الرسائل ذاتية الاختفاء */}
              <button
                onClick={() => handleMenuItemClick('اختفاء', 'الرسائل ذاتية الاختفاء: معطلة')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>الرسائل ذاتية الاختفاء</span>
                <Clock className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 8. معلومات التشفير */}
              <button
                onClick={() => handleMenuItemClick('تشفير', 'المحادثة مشفرة تماماً End-to-End')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>معلومات التشفير</span>
                <Shield className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 9. حظر */}
              <button
                onClick={() => handleMenuItemClick('حظر', 'تم حظر أحمد الحلفي')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>حظر</span>
                <UserX className="w-4 h-4 text-white" />
              </button>
              <div className="h-[1px] bg-white/10 my-0.5" />

              {/* 10. إبلاغ */}
              <button
                onClick={() => handleMenuItemClick('إبلاغ', 'تم إرسال البلاغ بنجاح')}
                className="w-full h-9 px-2 rounded-lg hover:bg-white/10 flex items-center justify-between text-xs font-bold text-white transition-colors cursor-pointer"
              >
                <span>إبلاغ</span>
                <Flag className="w-4 h-4 text-rose-500" />
              </button>
            </div>
          </div>
        )}

        {/* Floating Toast Notification */}
        {toastMessage && (
          <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-full bg-slate-900/95 border border-cyan-500/50 text-white text-xs font-semibold shadow-xl flex items-center gap-2 animate-in fade-in slide-in-from-bottom-2">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
            <span>{toastMessage}</span>
          </div>
        )}
      </div>
    </div>
  );
};
