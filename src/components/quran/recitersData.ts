import { QuranReader } from './types';

export const QURAN_RECITERS: QuranReader[] = [
  {
    id: 'abdulbaset',
    name: 'عبد الباسط عبد الصمد',
    style: 'مرتل ومجود',
    imageUrl: 'https://images.unsplash.com/photo-1590076215667-875d4ef2d7ee?w=200&h=200&fit=crop',
    serverSubdomain: 'AbdulBaset/Murattal',
    bitrate: '192kbps',
    description: 'صوت مكة الخالد وإمام المقرئين، ترتيل برواية حفص عن عاصم'
  },
  {
    id: 'minshawi',
    name: 'محمد صديق المنشاوي',
    style: 'مرتل خاشع',
    imageUrl: 'https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=200&h=200&fit=crop',
    serverSubdomain: 'Minshawy/Murattal',
    bitrate: '192kbps',
    description: 'الصوت الباكي ذو النبرة الخاشعة العميقة التي تأسر القلوب'
  },
  {
    id: 'husary',
    name: 'محمود خليل الحصري',
    style: 'متقن الأحكام',
    imageUrl: 'https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=200&h=200&fit=crop',
    serverSubdomain: 'Hussary',
    bitrate: '128kbps',
    description: 'شيخ عموم المقارئ المصرية، التلاوة النموذجية لضبط أحكام التجويد'
  },
  {
    id: 'afasy',
    name: 'مشاري بن راشد العفاسي',
    style: 'حدر وترتيل عذب',
    imageUrl: 'https://images.unsplash.com/photo-1564769625905-50e93615e769?w=200&h=200&fit=crop',
    serverSubdomain: 'Alafasy',
    bitrate: '128kbps',
    description: 'إمام المسجد الكبير بدولة الكويت، تلاوات عذبة شجية مؤثرة'
  },
  {
    id: 'muaiqly',
    name: 'ماهر المعيقلي',
    style: 'ترتيل الحرم المكي',
    imageUrl: 'https://images.unsplash.com/photo-1519817650390-64a93db51149?w=200&h=200&fit=crop',
    serverSubdomain: 'Maher',
    bitrate: '128kbps',
    description: 'إمام وخطيب المسجد الحرام بمكة المكرمة'
  },
  {
    id: 'ghamdi',
    name: 'سعد الغامدي',
    style: 'مرتل هادئ',
    imageUrl: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=200&h=200&fit=crop',
    serverSubdomain: 'Ghamadi',
    bitrate: '128kbps',
    description: 'قراءة محببة هادئة ومتقنة تميز بها في العالم الإسلامي'
  },
  {
    id: 'dossari',
    name: 'ياسر الدوسري',
    style: 'ترتيل الحرم المكي الشريف',
    imageUrl: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=200&h=200&fit=crop',
    serverSubdomain: 'Dosary',
    bitrate: '128kbps',
    description: 'إمام المسجد الحرام، صوت قوي وخاشع ذو نبرات حجازية فريدة'
  },
  {
    id: 'abbad',
    name: 'فارس عباد',
    style: 'ترتيل يماني شجي',
    imageUrl: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=200&h=200&fit=crop',
    serverSubdomain: 'Fares',
    bitrate: '128kbps',
    description: 'نبرة روحانية يمانية شهيرة تؤثر في النفوس'
  },
  {
    id: 'ajmy',
    name: 'أحمد بن علي العجمي',
    style: 'ترتيل جهوري',
    imageUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&h=200&fit=crop',
    serverSubdomain: 'Ajamy',
    bitrate: '128kbps',
    description: 'تلاوات خاشعة جهورية متقنة تحرك القلوب'
  }
];

export function getReciterAudioUrl(readerId: string, surahNumber: number): string {
  const padded = String(surahNumber).padStart(3, '0');
  switch (readerId) {
    case 'abdulbaset':
      return `https://server7.mp3quran.net/basit/${padded}.mp3`;
    case 'minshawi':
      return `https://server10.mp3quran.net/minsh/${padded}.mp3`;
    case 'husary':
      return `https://server13.mp3quran.net/husr/${padded}.mp3`;
    case 'afasy':
      return `https://server8.mp3quran.net/afs/${padded}.mp3`;
    case 'muaiqly':
      return `https://server12.mp3quran.net/maher/${padded}.mp3`;
    case 'ghamdi':
      return `https://server7.mp3quran.net/s_gmd/${padded}.mp3`;
    case 'dossari':
      return `https://server11.mp3quran.net/yasser/${padded}.mp3`;
    case 'abbad':
      return `https://server8.mp3quran.net/frs_a/${padded}.mp3`;
    case 'ajmy':
      return `https://server10.mp3quran.net/ajm/${padded}.mp3`;
    default:
      return `https://server8.mp3quran.net/afs/${padded}.mp3`;
  }
}
