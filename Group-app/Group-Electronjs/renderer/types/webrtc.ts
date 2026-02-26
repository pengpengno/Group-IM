export interface IceCandidateData {
    candidate: string;
    sdpMid: string;
    sdpMLineIndex: number;
}

export interface WebrtcMessage {
    type: 'call/request' | 'call/accept' | 'call/end' | 'call/failed' | 'offer' | 'answer' | 'candidate';
    fromUser: string;
    toUser: string;
    sdp?: string;
    sdpType?: 'offer' | 'answer';
    candidate?: IceCandidateData;
    reason?: string;
}
