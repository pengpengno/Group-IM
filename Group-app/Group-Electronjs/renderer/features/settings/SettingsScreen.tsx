import React, { useState } from 'react';
import './SettingsScreen.css';

const qualityOptions = ['AUTO', 'LOW', 'MEDIUM', 'HIGH'] as const;

const SettingsScreen: React.FC = () => {
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [messagePreviewEnabled, setMessagePreviewEnabled] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [videoQuality, setVideoQuality] = useState<(typeof qualityOptions)[number]>('AUTO');

  return (
    <div className="settings-screen">
      <section className="settings-hero">
        <div>
          <p className="settings-eyebrow">Unified Settings</p>
          <h2>One place for local, remote, and future server policy configuration.</h2>
          <p className="settings-hero-copy">
            This screen now mirrors the information architecture planned for every client:
            notifications, privacy, video, network, storage, and device behavior.
          </p>
        </div>
        <div className="settings-status-card">
          <span className="settings-status-label">Phase 1</span>
          <strong>Local settings first</strong>
          <p>Desktop is ready for local behavior now. Remote account sync will follow the server API work.</p>
        </div>
      </section>

      <div className="settings-grid">
        <article className="settings-card">
          <div className="settings-card-header">
            <div>
              <p className="settings-card-kicker">Local</p>
              <h3>Notifications</h3>
            </div>
            <span className="settings-pill">Live</span>
          </div>

          <label className="settings-toggle-row">
            <div>
              <strong>Enable desktop notifications</strong>
              <p>Allow system notifications for new chat and meeting activity on this device.</p>
            </div>
            <input
              type="checkbox"
              checked={notificationsEnabled}
              onChange={(event) => setNotificationsEnabled(event.target.checked)}
            />
          </label>

          <label className="settings-toggle-row">
            <div>
              <strong>Show message preview</strong>
              <p>Include message text in notification banners.</p>
            </div>
            <input
              type="checkbox"
              checked={messagePreviewEnabled}
              onChange={(event) => setMessagePreviewEnabled(event.target.checked)}
            />
          </label>

          <label className="settings-toggle-row">
            <div>
              <strong>Play alert sound</strong>
              <p>Use a sound cue when a new event arrives.</p>
            </div>
            <input
              type="checkbox"
              checked={soundEnabled}
              onChange={(event) => setSoundEnabled(event.target.checked)}
            />
          </label>
        </article>

        <article className="settings-card">
          <div className="settings-card-header">
            <div>
              <p className="settings-card-kicker">Remote next</p>
              <h3>Video And Meetings</h3>
            </div>
            <span className="settings-pill muted">Planned</span>
          </div>

          <div className="settings-field">
            <label htmlFor="videoQuality">Default video quality</label>
            <select
              id="videoQuality"
              value={videoQuality}
              onChange={(event) => setVideoQuality(event.target.value as (typeof qualityOptions)[number])}
            >
              {qualityOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
            <p>This models the future account-level preference. Runtime quality will still depend on device and network limits.</p>
          </div>

          <ul className="settings-checklist">
            <li>Auto join audio</li>
            <li>Auto join video</li>
            <li>Camera and microphone defaults</li>
          </ul>
        </article>

        <article className="settings-card">
          <div className="settings-card-header">
            <div>
              <p className="settings-card-kicker">Remote next</p>
              <h3>Privacy And Security</h3>
            </div>
            <span className="settings-pill muted">API pending</span>
          </div>

          <ul className="settings-checklist">
            <li>Online visibility</li>
            <li>Friend request policy</li>
            <li>Last seen visibility</li>
            <li>Notification mode</li>
          </ul>
        </article>

        <article className="settings-card">
          <div className="settings-card-header">
            <div>
              <p className="settings-card-kicker">Local</p>
              <h3>Network And Proxy</h3>
            </div>
            <span className="settings-pill">Next step</span>
          </div>

          <p className="settings-card-copy">
            Proxy host, API host, TCP host, and TLS belong to local settings. The KMP client is already being moved into this shape, and Electron should follow with the same data model.
          </p>

          <div className="settings-note">
            <strong>Planned desktop wiring</strong>
            <p>Expose local settings through preload plus IPC, then hydrate a dedicated settings store in the renderer.</p>
          </div>
        </article>
      </div>
    </div>
  );
};

export default SettingsScreen;
