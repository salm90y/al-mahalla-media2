import { QuranRoom } from './types';

export const INITIAL_QURAN_ROOMS: QuranRoom[] = [
  {
    id: 'room_baqarah',
    title: '📖 مجلس تدبر سورة البقرة المباركة',
    bannerUrl: 'https://images.unsplash.com/photo-1609599006353-e629aaabfeae?w=500&h=200&fit=crop',
    hostUserId: 'host_101',
    hostUserName: 'الشيخ أحمد الموصلي (مدير الغرفة)',
    readerId: 'abdulbaset',
    readerName: 'عبد الباسط عبد الصمد',
    surahId: 2,
    surahName: 'البقرة',
    ayahId: 255,
    positionMs: 35000,
    isPlaying: true,
    serverTimestamp: Date.now(),
    listenersCount: 28,
    isPublic: true,
    mode: 'listen_only',
    participants: [
      { id: 'host_101', name: 'الشيخ أحمد الموصلي', avatar: '🕌', role: 'host', isMuted: false, hasRaisedHand: false },
      { id: 'user_1', name: 'سامر البصري', avatar: '🧔🏻', role: 'listener', isMuted: true, hasRaisedHand: false },
      { id: 'user_2', name: 'عبد الله الحيدري', avatar: '👨🏽', role: 'speaker', isMuted: true, hasRaisedHand: false },
      { id: 'user_3', name: 'أبو الحسن', avatar: '🧓🏼', role: 'listener', isMuted: true, hasRaisedHand: true }
    ],
    messages: [
      { id: 'm1', userId: 'host_101', userName: 'الشيخ أحمد', userAvatar: '🕌', text: 'أهلاً بكم في مجلس سورة البقرة. الآية الحالية هي آية الكرسي.', timestamp: '12:00', isHost: true },
      { id: 'm2', userId: 'user_1', userName: 'سامر', userAvatar: '🧔🏻', text: 'ما شاء الله تلاوة خاشعة تريح القلوب', timestamp: '12:02' },
      { id: 'm3', userId: 'user_2', userName: 'عبد الله', userAvatar: '👨🏽', text: 'جزاكم الله خير الجزاء', timestamp: '12:04' }
    ]
  },
  {
    id: 'room_khatmah',
    title: '📖 الختمة الجماعية المباركة (الجزء 30)',
    bannerUrl: 'https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=500&h=200&fit=crop',
    hostUserId: 'host_102',
    hostUserName: 'حيدر الكرخي (مدير الغرفة)',
    readerId: 'minshawi',
    readerName: 'محمد صديق المنشاوي',
    surahId: 112,
    surahName: 'الإخلاص',
    ayahId: 1,
    positionMs: 12000,
    isPlaying: true,
    serverTimestamp: Date.now(),
    listenersCount: 19,
    isPublic: true,
    mode: 'voice_council',
    participants: [
      { id: 'host_102', name: 'حيدر الكرخي', avatar: '🎙️', role: 'host', isMuted: false, hasRaisedHand: false },
      { id: 'user_4', name: 'محمد الموسوي', avatar: '👦🏻', role: 'speaker', isMuted: false, hasRaisedHand: false }
    ],
    messages: [
      { id: 'mk1', userId: 'host_102', userName: 'حيدر', userAvatar: '🎙️', text: 'وصلنا بحمد الله إلى جزء عمّ، نستمع معاً للمنشاوي.', timestamp: '12:05', isHost: true }
    ]
  },
  {
    id: 'room_kahf',
    title: '📖 تلاوة وتدبر سورة الكهف الشريفة',
    bannerUrl: 'https://images.unsplash.com/photo-1590076215667-875d4ef2d7ee?w=500&h=200&fit=crop',
    hostUserId: 'host_103',
    hostUserName: 'أبو فاطمة النجفي (مدير الغرفة)',
    readerId: 'afasy',
    readerName: 'مشاري العفاسي',
    surahId: 18,
    surahName: 'الكهف',
    ayahId: 10,
    positionMs: 48000,
    isPlaying: true,
    serverTimestamp: Date.now(),
    listenersCount: 42,
    isPublic: true,
    mode: 'listen_only',
    participants: [
      { id: 'host_103', name: 'أبو فاطمة النجفي', avatar: '🕋', role: 'host', isMuted: false, hasRaisedHand: false }
    ],
    messages: [
      { id: 'mh1', userId: 'host_103', userName: 'أبو فاطمة', userAvatar: '🕋', text: 'اللهم نور قلوبنا بالقرآن العظيم واجعله ربيع قلوبنا.', timestamp: '12:10', isHost: true }
    ]
  }
];
