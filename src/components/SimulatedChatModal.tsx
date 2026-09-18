import React, { useState } from 'react';
import { soundFx } from '../services/audioSynthesizer';

interface SimulatedChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  roomCode: string;
}

interface Message {
  id: string;
  sender: string;
  isHost: boolean;
  text: string;
  time: string;
}

export const SimulatedChatModal: React.FC<SimulatedChatModalProps> = ({
  isOpen,
  onClose,
  roomCode
}) => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      sender: 'النظام (System)',
      isHost: true,
      text: `تم الاتصال بغرفة Netplay [${roomCode}] بنجاح • مزامنة GGPO نشطة.`,
      time: '12:00'
    },
    {
      id: '2',
      sender: 'Player 2 (المنافس)',
      isHost: false,
      text: 'مرحباً! أنا جاهز للتحدي في لعبة Combat 3.',
      time: '12:01'
    }
  ]);
  const [inputText, setInputText] = useState('');

  if (!isOpen) return null;

  const handleSend = () => {
    if (!inputText.trim()) return;
    soundFx.playUiBlip(900);
    const newMsg: Message = {
      id: Date.now().toString(),
      sender: 'أنت (Player 1)',
      isHost: true,
      text: inputText.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setMessages(prev => [...prev, newMsg]);
    setInputText('');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div 
        id="simulated-chat-card"
        className="w-full max-w-md bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl flex flex-col overflow-hidden text-right"
        dir="rtl"
      >
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950">
          <div className="flex items-center gap-2">
            <span className="text-xl">💬</span>
            <div>
              <h3 className="text-sm font-bold text-white">المحادثة الفورية للغرفة</h3>
              <p className="text-[11px] text-slate-400">غرفة {roomCode} • مشفرة عبر WebRTC</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 rounded-full bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center text-xs cursor-pointer transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Message history */}
        <div className="flex-1 p-4 space-y-3 max-h-80 overflow-y-auto bg-slate-900/50">
          {messages.map(msg => (
            <div
              key={msg.id}
              className={`p-3 rounded-xl max-w-[85%] text-xs ${
                msg.sender.includes('النظام')
                  ? 'bg-slate-800/80 border border-slate-700 text-slate-300 mx-auto w-full text-center'
                  : msg.isHost
                  ? 'bg-blue-900/40 border border-blue-700/50 text-white mr-auto'
                  : 'bg-emerald-900/40 border border-emerald-700/50 text-white ml-auto'
              }`}
            >
              <div className="flex items-center justify-between text-[10px] text-slate-400 mb-1">
                <span className="font-bold text-slate-300">{msg.sender}</span>
                <span>{msg.time}</span>
              </div>
              <p className="leading-relaxed">{msg.text}</p>
            </div>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-3 bg-slate-950 border-t border-slate-800 flex items-center gap-2">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="اكتب رسالة للاعبين باللغة العربية..."
            className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white placeholder:text-slate-500 focus:outline-hidden focus:border-blue-500"
          />
          <button
            onClick={handleSend}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl text-xs transition-colors cursor-pointer"
          >
            إرسال ✈️
          </button>
        </div>
      </div>
    </div>
  );
};
