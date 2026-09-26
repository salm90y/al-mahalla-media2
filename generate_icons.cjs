const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const src = '/tmp/mahalla_icon.png';
if (!fs.existsSync(src)) {
  console.error('Source icon does not exist at ' + src);
  process.exit(1);
}

// Ensure public directory
if (!fs.existsSync('public')) fs.mkdirSync('public', { recursive: true });

// 1. Web & PWA assets with highest quality Lanczos resampling
execSync(`convert "${src}" -filter Lanczos -resize 512x512 -unsharp 0x0.6+0.8+0 "public/icon-512.png"`);
execSync(`convert "${src}" -filter Lanczos -resize 512x512 -unsharp 0x0.6+0.8+0 "public/mahalla_icon.png"`);
execSync(`convert "${src}" -filter Lanczos -resize 192x192 -unsharp 0x0.6+0.8+0 "public/icon-192.png"`);
execSync(`convert "${src}" -filter Lanczos -resize 64x64 -unsharp 0x0.5+0.9+0 "public/favicon.png"`);

// 2. Android Mipmaps
const densities = [
  { dir: 'mipmap-mdpi', size: 48, fg: 108 },
  { dir: 'mipmap-hdpi', size: 72, fg: 162 },
  { dir: 'mipmap-xhdpi', size: 96, fg: 216 },
  { dir: 'mipmap-xxhdpi', size: 144, fg: 324 },
  { dir: 'mipmap-xxxhdpi', size: 192, fg: 432 }
];

densities.forEach(({ dir, size, fg }) => {
  const targetDir = path.join('android/app/src/main/res', dir);
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }

  // Legacy square icon
  execSync(`convert "${src}" -filter Lanczos -resize ${size}x${size} -unsharp 0x0.6+0.8+0 "${path.join(targetDir, 'ic_launcher.png')}"`);
  
  // Legacy round icon
  execSync(`convert "${src}" -filter Lanczos -resize ${size}x${size} -unsharp 0x0.6+0.8+0 \\( +clone -alpha extract -draw "fill black polygon 0,0 0,${size} ${size},${size} ${size},0 fill white circle ${size/2},${size/2} ${size/2},0" \\) -alpha off -compose CopyOpacity -composite "${path.join(targetDir, 'ic_launcher_round.png')}"`);
  
  // Adaptive foreground (72/108 ratio = 66.6% safe zone centered in fg box)
  const fgIconSize = Math.round(fg * 0.72);
  execSync(`convert -size ${fg}x${fg} xc:none \\( "${src}" -filter Lanczos -resize ${fgIconSize}x${fgIconSize} -unsharp 0x0.6+0.8+0 \\) -gravity center -composite "${path.join(targetDir, 'ic_launcher_foreground.png')}"`);
});

// Also drawable/ic_launcher.png and drawable-nodpi/ic_launcher.png
execSync(`convert "${src}" -filter Lanczos -resize 512x512 -unsharp 0x0.6+0.8+0 "android/app/src/main/res/drawable/ic_launcher.png"`);
if (!fs.existsSync('android/app/src/main/res/drawable-nodpi')) {
  fs.mkdirSync('android/app/src/main/res/drawable-nodpi', { recursive: true });
}
execSync(`convert "${src}" -filter Lanczos -resize 512x512 -unsharp 0x0.6+0.8+0 "android/app/src/main/res/drawable-nodpi/ic_launcher.png"`);

console.log('All ultra high resolution icons generated successfully with pristine sharpness!');
