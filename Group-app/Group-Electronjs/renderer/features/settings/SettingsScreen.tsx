import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { systemConfigAPI } from '../../services/api/apiClient';
import { RootState } from '../../store';
import './SettingsScreen.css';

type ConfigField = {
  key: string;
  label: string;
  description: string;
  valueType: 'BOOLEAN' | 'INTEGER' | string;
  defaultValue: string;
  publicReadable: boolean;
  value: string;
};

type ConfigGroup = {
  group: string;
  title: string;
  fields: ConfigField[];
};

const SettingsScreen: React.FC = () => {
  const { user } = useSelector((state: RootState) => state.auth);
  const isAdmin = user?.username === 'admin';
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [messagePreviewEnabled, setMessagePreviewEnabled] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [groups, setGroups] = useState<ConfigGroup[]>([]);
  const [formValues, setFormValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<string>('');

  useEffect(() => {
    const fetchConfig = async () => {
      if (!isAdmin) {
        return;
      }
      setLoading(true);
      try {
        const response = await systemConfigAPI.getAdminGroups();
        const data = response.data?.data ?? [];
        setGroups(data);
        const nextValues: Record<string, string> = {};
        data.forEach((group: ConfigGroup) => {
          group.fields.forEach((field) => {
            nextValues[field.key] = field.value;
          });
        });
        setFormValues(nextValues);
      } catch (error) {
        console.error('Failed to load system config', error);
        setStatus('System config load failed. Please refresh and try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchConfig();
  }, [isAdmin]);

  const updateField = (key: string, value: string) => {
    setFormValues((prev) => ({ ...prev, [key]: value }));
  };

  const saveMediaConfig = async () => {
    setSaving(true);
    setStatus('');
    try {
      const mediaValues = Object.fromEntries(
        Object.entries(formValues).filter(([key]) => key.startsWith('media.'))
      );
      const response = await systemConfigAPI.updateMediaConfig(mediaValues);
      const data = response.data?.data ?? [];
      setGroups(data);
      const nextValues: Record<string, string> = {};
      data.forEach((group: ConfigGroup) => {
        group.fields.forEach((field) => {
          nextValues[field.key] = field.value;
        });
      });
      setFormValues(nextValues);
      setStatus('Media delivery policy has been updated.');
    } catch (error: any) {
      setStatus(error.response?.data?.message || 'Media delivery policy update failed.');
    } finally {
      setSaving(false);
    }
  };

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
          <span className="settings-status-label">Phase 2</span>
          <strong>Hybrid control plane</strong>
          <p>Local switches stay on device. System-wide media policy now has a server-backed control path for admins.</p>
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
              <p className="settings-card-kicker">Server-backed</p>
              <h3>Media Delivery Policy</h3>
            </div>
            <span className={`settings-pill ${isAdmin ? '' : 'muted'}`}>{isAdmin ? 'Admin live' : 'Read only'}</span>
          </div>

          {!isAdmin && (
            <div className="settings-note">
              <strong>Admin-only control</strong>
              <p>This area is intended for system operators. It controls preview size, thumbnail generation, and transfer quality for the whole service.</p>
            </div>
          )}

          {isAdmin && (
            <>
              {loading && <p className="settings-card-copy">Loading current system config...</p>}
              {!loading && groups.map((group) => (
                <div key={group.group} className="settings-config-group">
                  {group.fields.map((field) => (
                    <label className="settings-field" key={field.key}>
                      <span className="settings-label-row">
                        <strong>{field.label}</strong>
                        <code>{field.key}</code>
                      </span>
                      {field.valueType === 'BOOLEAN' ? (
                        <select
                          value={formValues[field.key] ?? field.value}
                          onChange={(event) => updateField(field.key, event.target.value)}
                        >
                          <option value="true">true</option>
                          <option value="false">false</option>
                        </select>
                      ) : (
                        <input
                          type="number"
                          value={formValues[field.key] ?? field.value}
                          onChange={(event) => updateField(field.key, event.target.value)}
                        />
                      )}
                      <p>{field.description}</p>
                      <small>Default: {field.defaultValue}</small>
                    </label>
                  ))}
                </div>
              ))}

              <div className="settings-actions">
                <button className="settings-save-btn" onClick={saveMediaConfig} disabled={saving || loading}>
                  {saving ? 'Saving...' : 'Save media policy'}
                </button>
                {status && <span className="settings-status-text">{status}</span>}
              </div>
            </>
          )}
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
            Proxy host, API host, TCP host, and TLS still belong to local settings. The new split is intentional: device knobs stay local, while service policy moves into the database-backed control plane.
          </p>

          <div className="settings-note">
            <strong>What this unlocks</strong>
            <p>Operations can tune preview width, quality, and thumbnail generation without editing YAML or restarting how teams think about daily configuration work.</p>
          </div>
        </article>
      </div>
    </div>
  );
};

export default SettingsScreen;
