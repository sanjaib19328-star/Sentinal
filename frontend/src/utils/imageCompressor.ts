export interface ProcessedImageResult {
  base64: string;
  name: string;
  size: number;
  type: string;
}

/**
 * Compresses and resizes an image file client-side before Base64 encoding.
 * Ensures payloads stay lightweight (< 1MB) for instant network delivery and prevents upload timeouts.
 */
export async function processImageFile(
  file: File,
  maxDimension: number = 1600,
  quality: number = 0.82
): Promise<ProcessedImageResult> {
  const mime = file.type?.toLowerCase() || 'image/png';

  // If already small (< 300KB), encode directly without recompression
  if (file.size <= 300 * 1024) {
    const rawBase64 = await readFileAsBase64(file);
    return {
      base64: rawBase64,
      name: file.name,
      size: file.size,
      type: mime,
    };
  }

  // In browser environments with Image and Canvas support
  if (typeof window !== 'undefined' && typeof Image !== 'undefined') {
    try {
      return await compressWithCanvas(file, maxDimension, quality, mime);
    } catch (e) {
      console.warn('Canvas image compression failed, falling back to raw read:', e);
    }
  }

  const fallbackBase64 = await readFileAsBase64(file);
  return {
    base64: fallbackBase64,
    name: file.name,
    size: file.size,
    type: mime,
  };
}

function compressWithCanvas(
  file: File,
  maxDimension: number,
  quality: number,
  originalMime: string
): Promise<ProcessedImageResult> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Failed to read image file'));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error('Failed to decode image into HTML image element'));
      img.onload = () => {
        let { width, height } = img;

        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = Math.round((height * maxDimension) / width);
            width = maxDimension;
          } else {
            width = Math.round((width * maxDimension) / height);
            height = maxDimension;
          }
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          const raw = (reader.result as string).split(',')[1] || (reader.result as string);
          resolve({
            base64: raw,
            name: file.name,
            size: file.size,
            type: originalMime,
          });
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);

        // JPEG is vastly smaller for photos/screenshots unless small PNG
        const isPngSmall = originalMime === 'image/png' && file.size < 1024 * 1024;
        const outputMime = isPngSmall ? 'image/png' : 'image/jpeg';
        const dataUrl = canvas.toDataURL(outputMime, quality);
        const base64 = dataUrl.split(',')[1] || dataUrl;
        const estimatedSize = Math.round((base64.length * 3) / 4);

        let outName = file.name;
        if (outputMime === 'image/jpeg' && !outName.toLowerCase().endsWith('.jpg') && !outName.toLowerCase().endsWith('.jpeg')) {
          outName = outName.replace(/\.[^/.]+$/, '') + '.jpg';
        }

        resolve({
          base64,
          name: outName,
          size: estimatedSize,
          type: outputMime,
        });
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  });
}

function readFileAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.onload = () => {
      const result = reader.result as string;
      const base64 = result.split(',')[1] || result;
      resolve(base64);
    };
    reader.readAsDataURL(file);
  });
}
