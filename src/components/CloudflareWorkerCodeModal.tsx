import React, { useState, useEffect } from 'react';

interface CloudflareWorkerCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CloudflareWorkerCodeModal: React.FC<CloudflareWorkerCodeModalProps> = ({ isOpen, onClose }) => {
  const [workerCode, setWorkerCode] = useState<string>('جاري تحميل كود السيرفر...');
  const [copied, setCopied] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'steps' | 'code'>('steps');

  useEffect(() => {
    if (isOpen) {
      fetch('/worker.js')
        .then(res => res.text())
        .then(text => setWorkerCode(text))
        .catch(() => {
          setWorkerCode('// تعذر تحميل الكود تلقائياً. يمكنك نسخ الكود من worker/src/index.ts');
        });
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(workerCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 3000);
  };

  const handleDownload = () => {
    const blob = new Blob([workerCode], { type: 'text/javascript' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'worker.js';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/60 backdrop-blur-xs select-text">
      <div className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="px-6 py-4 bg-gradient-to-r from-amber-500 to-orange-600 text-white flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-2xl">☁️</span>
            <div>
              <h2 className="text-base font-bold">تحديث سيرفر Cloudflare Worker فوراً</h2>
              <p className="text-xs text-amber-100">لحل مشكلة "3 days ago" وتفعيل تسجيل الدخول في 30 ثانية</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white transition-colors cursor-pointer text-sm font-bold"
          >
            ✕
          </button>
        </div>

        {/* Sub Navigation */}
        <div className="flex border-b border-slate-200 bg-slate-50 px-6 pt-3 gap-2">
          <button
            onClick={() => setActiveSubTab('steps')}
            className={`pb-2.5 px-3 text-xs font-bold transition-colors cursor-pointer border-b-2 ${
              activeSubTab === 'steps' ? 'border-amber-600 text-amber-700' : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            📋 خطوات اللصق والتفعيل (دقيقة واحدة)
          </button>
          <button
            onClick={() => setActiveSubTab('code')}
            className={`pb-2.5 px-3 text-xs font-bold transition-colors cursor-pointer border-b-2 ${
              activeSubTab === 'code' ? 'border-amber-600 text-amber-700' : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            💻 عرض كود الـ Worker المدمج
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1 text-slate-800 text-sm">
          {activeSubTab === 'steps' ? (
            <div className="space-y-4">
              <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-amber-900 text-xs leading-relaxed">
                <strong>لماذا ظهر لك في الصورة "3 days ago" وكلمة "Dashboard"؟</strong><br />
                لأن الـ Worker تم نشره يدوياً عبر محرر كلاودفلير منذ 3 أيام قبل إضافة كود تسجيل الدخول. بما أنه نُشر عبر Dashboard فهو لا يسحب من GitHub تلقائياً، ولتحديثه الآن كل ما تحتاجه هو استبدال الكود داخل محرر كلاودفلير بالنسخة الجديدة.
              </div>

              <div className="space-y-3">
                <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <div className="w-6 h-6 rounded-full bg-amber-600 text-white font-bold flex items-center justify-center shrink-0 text-xs">
                    1
                  </div>
                  <div>
                    <h4 className="font-bold text-slate-900">انسخ الكود الجديد بنقرة واحدة</h4>
                    <p className="text-xs text-slate-600 mt-1">
                      اضغط على الزر البرتقالي بالأسفل <strong>"نسخ الكود بالكامل"</strong>.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <div className="w-6 h-6 rounded-full bg-amber-600 text-white font-bold flex items-center justify-center shrink-0 text-xs">
                    2
                  </div>
                  <div>
                    <h4 className="font-bold text-slate-900">افتح محرر الكود في Cloudflare</h4>
                    <p className="text-xs text-slate-600 mt-1">
                      في نفس صفحة <strong>al-mahalla</strong> الموجودة في لقطة شاشتك: اضغط على التبويب <strong>Overview</strong> ثم اضغط على زر <strong>Edit code</strong> (أو اضغط زر القائمة ⋯ بأعلى اليمين).
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200">
                  <div className="w-6 h-6 rounded-full bg-amber-600 text-white font-bold flex items-center justify-center shrink-0 text-xs">
                    3
                  </div>
                  <div>
                    <h4 className="font-bold text-slate-900">الصق الكود واضغط Save and Deploy</h4>
                    <p className="text-xs text-slate-600 mt-1">
                      حدد كل النص القديم (Ctrl+A أو تحديد الكل)، احذفه، والصق الكود الجديد ثم اضغط على الزر الأزرق <strong>Save and deploy (حفظ ونشر)</strong>.
                    </p>
                  </div>
                </div>
              </div>

              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-900 text-xs">
                ✅ <strong>النتيجة الفورية:</strong> ستتغير النسخة من <strong>3d ago</strong> إلى <strong>Just now</strong>، وسيدخل التطبيق فوراً بحساب <strong>ahmed</strong> وكلمة المرور <strong>123456</strong>!
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500 font-mono">worker.js (24.4 KB - Standalone Bundle)</span>
                <span className="text-xs text-emerald-600 font-bold">جاهز للنشر مباشرة على كلاودفلير</span>
              </div>
              <pre className="p-3 bg-slate-900 text-slate-100 rounded-xl text-xs font-mono max-h-72 overflow-y-auto border border-slate-800 dir-ltr text-left">
                {workerCode}
              </pre>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="px-6 py-4 bg-slate-100 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3">
          <button
            onClick={handleDownload}
            className="px-4 py-2 text-xs font-bold text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded-xl transition-all cursor-pointer flex items-center gap-1.5"
          >
            <span>💾</span>
            <span>تحميل ملف worker.js</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopy}
              className={`px-5 py-2 text-xs font-bold rounded-xl transition-all cursor-pointer shadow-md flex items-center gap-1.5 ${
                copied
                  ? 'bg-emerald-600 text-white shadow-emerald-200'
                  : 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-200'
              }`}
            >
              <span>{copied ? '✅' : '📋'}</span>
              <span>{copied ? 'تم نسخ الكود بنجاح!' : 'نسخ الكود بالكامل'}</span>
            </button>

            <button
              onClick={onClose}
              className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 bg-transparent rounded-xl transition-all cursor-pointer"
            >
              إغلاق
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
