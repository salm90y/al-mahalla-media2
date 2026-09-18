export interface QuranSurah {
  id: number;
  name: string; // Arabic name e.g. الفاتحة
  nameEn: string; // English transliteration e.g. Al-Fatihah
  revelationType: 'مكية' | 'مدنية';
  versesCount: number;
  juzNumber: number;
  audioUrl?: string;
  revelationOrder: number;
}

export interface QuranAyah {
  number: number;
  surahId: number;
  textUthmani: string;
  textSimple: string;
  juz: number;
  page?: number;
  tafsir?: string;
  audioUrl?: string;
}

export interface QuranJuz {
  number: number;
  name: string;
  startSurahName: string;
  startAyah: number;
  endSurahName: string;
  endAyah: number;
  surahsIncluded: string[];
}

export interface QuranReader {
  id: string;
  name: string;
  style: string;
  imageUrl: string;
  serverSubdomain: string;
  bitrate: string;
  description: string;
}

export interface QuranBookmark {
  id: string;
  surahId: number;
  surahName: string;
  ayahNumber: number;
  juzNumber: number;
  timestamp: string;
  note?: string;
}

export interface QuranRoomMessage {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  timestamp: string;
  isHost?: boolean;
}

export interface QuranRoomParticipant {
  id: string;
  name: string;
  avatar: string;
  role: 'host' | 'speaker' | 'listener';
  isMuted: boolean;
  hasRaisedHand: boolean;
}

export interface QuranRoom {
  id: string;
  title: string;
  bannerUrl: string;
  hostUserId: string;
  hostUserName: string;
  readerId: string;
  readerName: string;
  surahId: number;
  surahName: string;
  ayahId: number;
  positionMs: number;
  isPlaying: boolean;
  serverTimestamp: number;
  listenersCount: number;
  isPublic: boolean;
  mode: 'listen_only' | 'voice_council';
  participants: QuranRoomParticipant[];
  messages: QuranRoomMessage[];
}
