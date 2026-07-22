import { systemConfigAPI } from './api/apiClient';

export type MediaPolicy = {
  previewDefaultWidth: number;
  previewMinWidth: number;
  previewMaxWidth: number;
  previewDefaultQuality: number;
  previewMinQuality: number;
  previewMaxQuality: number;
  thumbnailEnabled: boolean;
  thumbnailWidth: number;
  thumbnailHeight: number;
  thumbnailQuality: number;
  uploadCompressionEnabled: boolean;
  uploadCompressMinSizeKb: number;
  uploadMaxImageEdge: number;
  uploadJpegQuality: number;
};

const defaultPolicy: MediaPolicy = {
  previewDefaultWidth: 480,
  previewMinWidth: 160,
  previewMaxWidth: 1600,
  previewDefaultQuality: 75,
  previewMinQuality: 40,
  previewMaxQuality: 95,
  thumbnailEnabled: true,
  thumbnailWidth: 640,
  thumbnailHeight: 360,
  thumbnailQuality: 82,
  uploadCompressionEnabled: true,
  uploadCompressMinSizeKb: 350,
  uploadMaxImageEdge: 1600,
  uploadJpegQuality: 82
};

let currentPolicy: MediaPolicy = defaultPolicy;
let inflight: Promise<MediaPolicy> | null = null;

export const getMediaPolicy = () => currentPolicy;

export const loadMediaPolicy = async (): Promise<MediaPolicy> => {
  if (inflight) {
    return inflight;
  }

  inflight = systemConfigAPI.getMediaPolicy()
    .then((response) => {
      currentPolicy = { ...defaultPolicy, ...(response.data?.data || {}) };
      return currentPolicy;
    })
    .catch((error) => {
      console.error('Failed to load media policy:', error);
      return currentPolicy;
    })
    .finally(() => {
      inflight = null;
    });

  return inflight;
};

export const buildPreviewUrl = (baseUrl: string, fileId: string, width?: number, quality?: number) => {
  const resolvedWidth = width ?? currentPolicy.previewDefaultWidth;
  const resolvedQuality = quality ?? currentPolicy.previewDefaultQuality;
  return `${baseUrl}/api/files/preview/${fileId}?width=${resolvedWidth}&quality=${resolvedQuality}`;
};
