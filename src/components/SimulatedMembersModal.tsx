import React, { useState } from 'react';
import { soundFx } from '../services/audioSynthesizer';

interface SimulatedMembersModalProps {
  isOpen: boolean;
  onClose: () => void;
  roomCode: string;
}

interface Member {
  id: string;
  name: string;
  role: string;
  isHost: boolean;
  isCoHost: boolean;
  isMuted: boolean;
  pingMs: number;
}

export const SimulatedMembersModal: React.FC<SimulatedMembersModalProps> = ({
  isOpen,
  onClose,
  roomCode
}) => {
  const [members, setMembers] = useState<Member[]>([
    { id: '1', name: 'المضيف الأساسي (أنت)', role: 'مضيف الغرفة (Host)', isHost: true, isCoHost: false, isMuted: false, pingMs: 14 },
    { id: '2', name: 'لاعب عبر الشبكة (Player 2)', role: 'لاعب منافس', isHost: false, isCoHost: false, isMuted: false, pingMs: 24 }
  ]);
  const [selectedMember, setSelectedMember] = useState<Member | null>(null);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div 
        id="simulated-members-card"
        className="w-full max-w-md bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl p-5 text-right overflow-hidden"
        dir="rtl"
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-3 mb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <span className="text-2xl">👥</span>
            <div>
              <h3 className="text-sm font-bold text-white">المتواجدون في الغرفة والصلاحيات</h3>
              <p className="text-[11px] text-slate-400">غرفة {roomCode} • 2 لاعبين متصلين</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center text-xs cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Member List */}
        <div className="space-y-3 mb-4 max-h-72 overflow-y-auto">
          {members.map(member => (
            <div
              key={member.id}
              className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 transition-colors"
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-blue-600/30 border border-blue-500/50 flex items-center justify-center text-sm font-bold text-white">
                    {member.name.charAt(0)}
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white flex items-center gap-1.5">
                      <span>{member.name}</span>
                      {member.isHost ? (
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800/40">
                          👑 مضيف
                        </span>
                      ) : member.isCoHost ? (
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800/40">
                          ⭐ مشرف
                        </span>
                      ) : null}
                    </h4>
                    <p className="text-[10px] text-slate-400">{member.role}</p>
                  </div>
                </div>

                <div className="text-left">
                  <span className="text-[11px] text-emerald-400 font-mono font-semibold">
                    ● {member.pingMs}ms
                  </span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2 pt-2 border-t border-slate-900 text-xs">
                <button
                  onClick={() => {
                    soundFx.playUiBlip(700);
                    setSelectedMember(member);
                  }}
                  className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] cursor-pointer"
                >
                  الملف الشخصي 👤
                </button>

                <button
                  onClick={() => {
                    soundFx.playUiBlip(600);
                    setMembers(prev => prev.map(m => m.id === member.id ? { ...m, isMuted: !m.isMuted } : m));
                  }}
                  className={`px-2.5 py-1 rounded text-[11px] cursor-pointer ${
                    member.isMuted
                      ? 'bg-rose-950 text-rose-300 border border-rose-800/60'
                      : 'bg-slate-800 hover:bg-slate-700 text-slate-300'
                  }`}
                >
                  {member.isMuted ? 'إلغاء الكتم 🔊' : 'كتم الصوت 🔇'}
                </button>

                {!member.isHost && (
                  <button
                    onClick={() => {
                      soundFx.playUiBlip(800);
                      setMembers(prev => prev.map(m => m.id === member.id ? { ...m, isCoHost: !m.isCoHost } : m));
                    }}
                    className="px-2.5 py-1 rounded bg-blue-900/40 hover:bg-blue-800 text-blue-300 border border-blue-700/50 text-[11px] cursor-pointer mr-auto"
                  >
                    {member.isCoHost ? 'إلغاء الإشراف' : 'منح إشراف ⭐'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Selected Member Profile Sub-Modal */}
        {selectedMember && (
          <div className="p-3 bg-slate-950 rounded-xl border border-blue-500/40 text-xs mb-3 space-y-1 text-slate-300">
            <h5 className="font-bold text-white text-sm mb-1 flex items-center justify-between">
              <span>بطاقة الملف الشخصي: {selectedMember.name}</span>
              <button
                onClick={() => setSelectedMember(null)}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </h5>
            <p>🎮 بروتوكول المزامنة: GGPO Rollback (زمن استجابة {selectedMember.pingMs}ms)</p>
            <p>🕹️ جهاز التحكم: سوني DualShock PS1 مع اهتزاز Haptics</p>
            <p>📶 جودة الاتصال: فائق الاستقرار • 0% فقدان حزم</p>
          </div>
        )}

        <button
          onClick={onClose}
          className="w-full py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl cursor-pointer"
        >
          إغلاق
        </button>
      </div>
    </div>
  );
};
