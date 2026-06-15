export interface IceCandidateData {
    candidate: string;
    sdpMid: string;
    sdpMLineIndex: number;
}

export interface WebrtcMessage {
    type:
        | 'meeting/request'
        | 'meeting/join'
        | 'meeting/participants'
        | 'meeting/participant-joined'
        | 'meeting/participant-left'
        | 'meeting/reject'
        | 'meeting/leave'
        | 'meeting/end'
        | 'offer'
        | 'answer'
        | 'candidate';
    fromUser: string;
    toUser?: string;
    roomId?: string;
    participants?: Array<Record<string, any>>;
    sdp?: string;
    sdpType?: 'offer' | 'answer';
    candidate?: IceCandidateData;
    reason?: string;
}
