import { ipcMain, desktopCapturer } from 'electron';

export const registerVideoHandlers = () => {
    // Handle getting desktop sources for screen sharing
    ipcMain.handle('get-desktop-sources', async () => {
        try {
            const sources = await desktopCapturer.getSources({
                types: ['window', 'screen'],
                thumbnailSize: { width: 150, height: 150 },
                fetchWindowIcons: true
            });

            return sources.map(source => ({
                id: source.id,
                name: source.name,
                thumbnail: source.thumbnail.toDataURL(),
                appIcon: source.appIcon ? source.appIcon.toDataURL() : null
            }));
        } catch (error) {
            console.error('Error getting desktop sources:', error);
            throw error;
        }
    });
};
