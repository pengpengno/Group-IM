const roomInput = document.getElementById('roomInput');
const nameInput = document.getElementById('nameInput');
const joinButton = document.getElementById('joinButton');
const startAudioBtn = document.getElementById('startAudioBtn');
const startScreenBtn = document.getElementById('startScreenBtn');
const stopScreenBtn = document.getElementById('stopScreenBtn');
const leaveBtn = document.getElementById('leaveBtn');
const localVideo = document.getElementById('localVideo');
const remoteVideo = document.getElementById('remoteVideo');

let socket;
let peerConnection;
let localStream;
let screenStream;
let roomName;
let userName;

const config = {
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
};

joinButton.onclick = async () => {
    roomName = roomInput.value;
    userName = nameInput.value;
    if (!roomName || !userName) {
        alert('Room and Name are required.');
        return;
    }
    socket = new WebSocket(`ws://localhost:8080/webrtc?roomName=${roomName}&userName=${userName}`);
    socket.onopen = () => {
        socket.send(JSON.stringify({ type: 'join', room: roomName, name: userName }));
    };
    socket.onmessage = handleSignaling;
    socket.onclose = () => console.log('WebSocket closed');

    peerConnection = new RTCPeerConnection(config);
    peerConnection.onicecandidate = (e) => {
        if (e.candidate) {
            socket.send(JSON.stringify({ type: 'ice', candidate: e.candidate }));
        }
    };
    peerConnection.ontrack = (e) => {
        remoteVideo.srcObject = e.streams[0];
    };

    startAudioBtn.disabled = false;
    startScreenBtn.disabled = false;
    leaveBtn.disabled = false;
};

startAudioBtn.onclick = async () => {
    const audioStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    audioStream.getTracks().forEach(track => peerConnection.addTrack(track, audioStream));
};

startScreenBtn.onclick = async () => {
    screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
    screenStream.getTracks().forEach(track => peerConnection.addTrack(track, screenStream));
    localVideo.srcObject = screenStream;
    stopScreenBtn.disabled = false;
};

stopScreenBtn.onclick = () => {
    screenStream.getTracks().forEach(track => track.stop());
    stopScreenBtn.disabled = true;
};

leaveBtn.onclick = () => {
    socket.send(JSON.stringify({ type: 'leave' }));
    peerConnection.close();
    socket.close();
    window.location.reload();
};

async function handleSignaling(message) {
    const data = JSON.parse(message.data);
    switch (data.type) {
        case 'offer':
            await peerConnection.setRemoteDescription(new RTCSessionDescription(data.offer));
            const answer = await peerConnection.createAnswer();
            await peerConnection.setLocalDescription(answer);
            socket.send(JSON.stringify({ type: 'answer', answer: answer }));
            break;
        case 'answer':
            await peerConnection.setRemoteDescription(new RTCSessionDescription(data.answer));
            break;
        case 'ice':
            if (data.candidate) {
                await peerConnection.addIceCandidate(new RTCIceCandidate(data.candidate));
            }
            break;
    }
}
