export interface IceCandidateData {
    candidate: string;
    sdpMid: string;
    sdpMLineIndex: number;
}

export const SIGNALING_MESSAGE_TYPES = {
    MEETING_REQUEST: 'meeting/request',
    MEETING_JOIN: 'meeting/join',
    MEETING_PARTICIPANTS: 'meeting/participants',
    MEETING_PARTICIPANT_JOINED: 'meeting/participant-joined',
    MEETING_PARTICIPANT_LEFT: 'meeting/participant-left',
    MEETING_REJECT: 'meeting/reject',
    MEETING_LEAVE: 'meeting/leave',
    MEETING_END: 'meeting/end',
    OFFER: 'offer',
    ANSWER: 'answer',
    CANDIDATE: 'candidate'
} as const;

export const SIGNALING_SDP_TYPES = {
    OFFER: 'offer',
    ANSWER: 'answer'
} as const;

export const SIGNALING_HEARTBEAT_TYPE = 'heartbeat' as const;

export type SignalingMessageType =
    typeof SIGNALING_MESSAGE_TYPES[keyof typeof SIGNALING_MESSAGE_TYPES];

export type SignalingSdpType =
    typeof SIGNALING_SDP_TYPES[keyof typeof SIGNALING_SDP_TYPES];

export interface WebrtcMessage {
    type: SignalingMessageType;
    fromUser: string;
    fromUserName?: string;
    fromAvatar?: string;
    toUser?: string;
    roomId?: string;
    participants?: Array<Record<string, any>>;
    sdp?: string;
    sdpType?: SignalingSdpType;
    candidate?: IceCandidateData;
    reason?: string;
    userId?: string;
    userName?: string;
    avatar?: string;
}
