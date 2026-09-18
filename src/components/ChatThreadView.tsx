import React, { useState } from 'react';
import { 
  ArrowLeft, Video, Phone, Flame, MoreVertical, Plus, X, Camera, Mic, 
  Image as ImageIcon, FileText, MapPin, Gamepad2, User, BarChart2, Play, Download
} from 'lucide-react';

interface ChatThreadViewProps {
  chatName: string;
  onBack: () => void;
}

export const ChatThreadView: React.FC<ChatThreadViewProps> = ({ chatName, onBack }) => {
  const [showAttachments, setShowAttachments] = useState(false);

  return (
    <div className="absolute inset-0 z-50 bg-[#F8FAFC] dark:bg-[#121212] flex flex-col">
      {/* 1. Top Bar (Chat Header) */}
      <header className="bg-gradient-to-r from-[#1C2A52] to-[#3B2B78] text-white px-3 py-3 shadow-md flex items-center justify-between z-20">
        <div className="flex items-center">
          <button onClick={onBack} className="p-1.5 mr-1 hover:bg-white/10 rounded-full transition">
            <ArrowLeft className="w-5 h-5 text-white" />
          </button>
          
          <div className="relative mr-3 flex-shrink-0">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#6C5CE7] to-[#00D2D3] flex items-center justify-center font-bold text-sm shadow overflow-hidden">
              <img src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop" alt="avatar" className="w-full h-full object-cover" />
            </div>
            <div className="absolute bottom-0 right-0 w-3 h-3 bg-[#10AC84] rounded-full border-2 border-[#3B2B78]"></div>
          </div>
          
          <div className="flex flex-col">
            <h2 className="text-[16px] font-bold text-white">{chatName}</h2>
            <span className="text-[11px] text-[#00D2D3] font-medium">online</span>
          </div>
        </div>

        <div className="flex items-center space-x-1">
          <button className="p-1.5 hover:bg-white/10 rounded-full transition text-white">
            <Video className="w-5 h-5" fill="white" />
          </button>
          <button className="p-1.5 hover:bg-white/10 rounded-full transition text-white">
            <Phone className="w-5 h-5" fill="white" />
          </button>
          <button className="p-1.5 hover:bg-white/10 rounded-full transition text-[#FFA502]">
            <Flame className="w-5 h-5" fill="currentColor" />
          </button>
          <button className="p-1.5 hover:bg-white/10 rounded-full transition text-white">
            <MoreVertical className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* Chat Stream (Scrollable) */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-white dark:bg-[#121212]" style={{ backgroundImage: 'radial-gradient(#e2e8f0 1px, transparent 1px)', backgroundSize: '20px 20px' }}>
        
        {/* A. Single Photo Bubble */}
        <div className="flex justify-end">
          <div className="relative rounded-[18px] overflow-hidden shadow-sm max-w-[85%] border border-slate-100">
            <img src="https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600&h=400&fit=crop" alt="Photo" className="w-full h-auto object-cover" />
            <div className="absolute bottom-0 left-0 right-0 h-16 bg-gradient-to-t from-black/70 to-transparent flex items-end justify-end p-3">
              <span className="text-[11px] text-white font-medium drop-shadow-md flex items-center">
                10:42 AM <span className="ml-1 text-[#00D2D3]">✓✓</span>
              </span>
            </div>
          </div>
        </div>

        {/* A. Multi-Photo Bubble */}
        <div className="flex justify-end">
          <div className="bg-[#6C5CE7] rounded-[18px] p-1.5 shadow-sm max-w-[85%] relative">
            <div className="grid grid-cols-2 gap-1 rounded-xl overflow-hidden">
              <img src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop" className="w-full h-24 object-cover" />
              <img src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop" className="w-full h-24 object-cover" />
              <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&h=150&fit=crop" className="w-full h-24 object-cover" />
              <img src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&h=150&fit=crop" className="w-full h-24 object-cover" />
            </div>
          </div>
        </div>

        {/* C. Document / File Card Bubble */}
        <div className="flex justify-end">
          <div className="bg-[#EAE6FC] dark:bg-[#2A244D] rounded-[18px] p-3 shadow-sm max-w-[85%] w-[85%] flex items-center space-x-3 relative">
            <div className="w-11 h-11 rounded-xl bg-[#2ED573] flex items-center justify-center text-white font-bold text-[11px] shadow-inner shrink-0">
              ZIP
            </div>
            <div className="flex-1 min-w-0 pr-4">
              <h4 className="text-[13px] font-bold text-[#1E272C] dark:text-white truncate">Project_Final_Assets.zip</h4>
              <p className="text-[11px] text-[#8395A7]">120.4 MB</p>
            </div>
            <div className="w-6 h-6 rounded-full border-[2.5px] border-[#6C5CE7]/30 border-t-[#6C5CE7] flex items-center justify-center shrink-0">
            </div>
          </div>
        </div>

        {/* D. Geolocation / Map Card Bubble */}
        <div className="flex justify-end">
          <div className="bg-[#3B2B78] rounded-[18px] overflow-hidden shadow-sm max-w-[85%] border border-[#3B2B78] flex flex-col w-[85%] relative">
            <div className="h-[140px] relative w-full overflow-hidden">
              <div className="absolute inset-0 opacity-70 bg-[url('https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=400&h=300&fit=crop')] bg-cover bg-center grayscale brightness-50 contrast-125"></div>
              <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
                <div className="w-8 h-8 bg-[#6C5CE7] rounded-full flex items-center justify-center shadow-lg border-2 border-white relative z-10">
                  <MapPin className="w-4 h-4 text-white" fill="white" />
                </div>
                <div className="w-2 h-2 bg-[#6C5CE7]/80 rounded-full mt-1 animate-ping absolute top-full"></div>
              </div>
              <div className="absolute bottom-3 right-3 text-white/90 text-[11px] font-bold text-right leading-tight">Basra<br/><span className="text-[9px] font-normal text-white/70">Iraq</span></div>
              <div className="absolute bottom-3 left-3 text-white/90 text-[11px] font-medium leading-tight">Al-Jaza'ir St, Basra</div>
            </div>
            <div className="py-2.5 text-center bg-[#2A1F5C]">
              <span className="text-white/90 text-[12px] font-medium">Open in Google Maps</span>
            </div>
            <div className="absolute -bottom-5 right-1">
              <span className="text-[10px] text-[#8395A7]">10:42 AM</span>
            </div>
          </div>
        </div>
        
        {/* Spacer for bottom input */}
        <div className="h-28"></div>
      </div>

      {/* Floating Attachment Drawer Overlay */}
      {showAttachments && (
        <div className="absolute inset-0 z-30 flex flex-col justify-end pointer-events-none pb-[76px] px-4">
          <div className="pointer-events-auto bg-white/60 dark:bg-slate-800/60 backdrop-blur-xl border border-white/40 dark:border-slate-700/40 rounded-[32px] p-6 shadow-2xl animate-in fade-in slide-in-from-bottom-10 duration-200">
            <div className="grid grid-cols-4 gap-y-6 gap-x-2">
              <AttachmentIcon icon={<ImageIcon className="w-6 h-6 text-white" />} label="Gallery" color="bg-[#FF4757]" />
              <AttachmentIcon icon={<Camera className="w-6 h-6 text-white" />} label="Snap Camera" color="bg-[#FFA502]" />
              <AttachmentIcon icon={<FileText className="w-6 h-6 text-white" />} label="Document" color="bg-[#2ED573]" />
              <AttachmentIcon icon={<MapPin className="w-6 h-6 text-white" />} label="Location" color="bg-[#1E90FF]" />
              <AttachmentIcon icon={<Gamepad2 className="w-6 h-6 text-white" />} label="Rooms" color="bg-[#70A1FF]" />
              <AttachmentIcon icon={<Mic className="w-6 h-6 text-white" />} label="Audio (Kuhayli)" color="bg-[#5352ED]" />
              <AttachmentIcon icon={<User className="w-6 h-6 text-white" />} label="Contact" color="bg-[#FF6B81]" />
              <AttachmentIcon icon={<BarChart2 className="w-6 h-6 text-white" />} label="Poll" color="bg-gray-500" />
            </div>
          </div>
        </div>
      )}

      {/* 2. Message Input Bar & Attachment Trigger (+ Button) */}
      <div className="absolute bottom-4 left-4 right-4 z-40 flex items-center space-x-2">
        {showAttachments ? (
          <button 
            onClick={() => setShowAttachments(false)}
            className="w-[44px] h-[44px] rounded-full flex items-center justify-center shadow-lg transition-transform duration-300 flex-shrink-0 bg-[#E2E8F0] dark:bg-slate-700 text-[#1E272C] dark:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        ) : (
          <button 
            onClick={() => setShowAttachments(true)}
            className="w-[44px] h-[44px] rounded-full flex items-center justify-center shadow-lg transition-transform duration-300 flex-shrink-0 bg-gradient-to-br from-[#6C5CE7] to-[#4834DF]"
          >
            <Plus className="w-6 h-6 text-white" />
          </button>
        )}

        <div className="flex-1 bg-white dark:bg-[#1E272C] rounded-[28px] shadow-lg flex items-center px-4 h-[48px] border border-slate-100 dark:border-slate-800">
          <input 
            type="text" 
            placeholder="Message..." 
            className="flex-1 bg-transparent border-none outline-none text-[15px] text-[#1E272C] dark:text-white placeholder-[#8395A7]"
          />
          <button className="p-1.5 text-[#8395A7] hover:text-[#1E272C] dark:hover:text-white transition">
            <Camera className="w-5 h-5" />
          </button>
          <button className="p-1.5 text-[#8395A7] hover:text-[#1E272C] dark:hover:text-white transition ml-1">
            <Mic className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};

const AttachmentIcon = ({ icon, label, color }: { icon: React.ReactNode, label: string, color: string }) => (
  <div className="flex flex-col items-center cursor-pointer hover:scale-105 transition-transform">
    <div className={`w-[54px] h-[54px] rounded-[18px] ${color} flex items-center justify-center shadow-md mb-2`}>
      {icon}
    </div>
    <span className="text-[11px] font-semibold text-[#1E272C] dark:text-white text-center max-w-[64px] leading-tight">{label}</span>
  </div>
);
