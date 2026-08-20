import React, { useCallback, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../../store';
import { MessageType } from '../../types';
import WorkbenchMessageCard from '../workbench/notification/WorkbenchMessageCard';
import LegacyChatRoom from './ChatRoomLegacy';
import './ChatRoomWorkbenchBridge.css';

type ChatRoomProps = React.ComponentProps<typeof LegacyChatRoom>;

type PortalTarget = {
  key: string;
  host: HTMLElement;
  content: string;
};

const ChatRoom: React.FC<ChatRoomProps> = (props) => {
  const rootRef = useRef<HTMLDivElement>(null);
  const targetsRef = useRef<PortalTarget[]>([]);
  const [targets, setTargets] = useState<PortalTarget[]>([]);
  const conversationId = Number((props as any).conversation?.conversationId || 0);
  const messages = useSelector((state: RootState) => state.chat.messages[conversationId] || []);

  const scanWorkbenchHosts = useCallback(() => {
    const root = rootRef.current;
    if (!root) return;

    const rows = Array.from(root.querySelectorAll<HTMLElement>('.messages-list-desktop .message-row'));
    const next: PortalTarget[] = [];

    messages.forEach((message, index) => {
      if (String(message.type).toUpperCase() !== MessageType.WORKBENCH) return;
      const host = rows[index]?.querySelector<HTMLElement>('.msg-text');
      if (!host) return;
      host.classList.add('workbench-portal-host');
      next.push({
        key: message.msgId > 0
          ? `workbench-${message.msgId}`
          : `workbench-${message.clientMsgId || `${message.timestamp}-${index}`}`,
        host,
        content: message.content,
      });
    });

    const previous = targetsRef.current;
    const unchanged = previous.length === next.length && previous.every((item, index) => (
      item.host === next[index]?.host
      && item.key === next[index]?.key
      && item.content === next[index]?.content
    ));
    if (!unchanged) {
      targetsRef.current = next;
      setTargets(next);
    }
  }, [messages]);

  useLayoutEffect(() => {
    scanWorkbenchHosts();
    const root = rootRef.current;
    if (!root) return undefined;

    let frame: number | null = null;
    const observer = new MutationObserver(() => {
      if (frame !== null) cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => {
        frame = null;
        scanWorkbenchHosts();
      });
    });
    observer.observe(root, { childList: true, subtree: true });

    return () => {
      observer.disconnect();
      if (frame !== null) cancelAnimationFrame(frame);
      targetsRef.current.forEach((item) => item.host.classList.remove('workbench-portal-host'));
      targetsRef.current = [];
    };
  }, [scanWorkbenchHosts]);

  return (
    <div ref={rootRef} className="chat-room-workbench-bridge">
      <LegacyChatRoom {...props} />
      {targets.map((target) => createPortal(
        <WorkbenchMessageCard content={target.content} />,
        target.host,
        target.key,
      ))}
    </div>
  );
};

export default ChatRoom;
