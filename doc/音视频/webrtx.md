# WebRTC 中的 RTP、RTCP、RtpSender/Receiver 概览

> 来源：[MDN - Introduction to RTP](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Intro_to_RTP)

## 1. RTP（Real-time Transport Protocol）

- 定义于 [RFC 3550](https://datatracker.ietf.org/doc/html/rfc3550)。
- 设计用于**实时音视频传输**。
- 特点：
    - 高效低延迟
    - 不保证可靠到达（实时优先）
    - 支持拓展，如加密（WebRTC 中使用 SRTP）

## 2. RTCP（RTP Control Protocol）

- 与 RTP 配套使用。
- 提供传输质量反馈，如丢包率、网络延迟。
- 允许媒体会话中的参与者互相了解网络状态，有助于动态调整传输策略。

## 3. WebRTC 中 RTP 相关 API

### RTCRtpSender

- 通过 `RTCRtpSender` 发送音频或视频轨道。
- 支持动态调整参数（如分辨率、码率控制）。
- 可用于插入处理，例如端到端加密（E2EE）。

### RTCRtpReceiver

- 通过 `RTCRtpReceiver` 接收远端发送的媒体流。
- 允许访问底层媒体数据，进行处理或分析。

### RTCRtpTransceiver

- 组合了 `Sender` 和 `Receiver`。
- 支持同时双向音视频流操作。
- 可用于实现流的动态开启、关闭或方向改变（发送/接收）。

---

## 更多参考

- [MDN - RTP 简介](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Intro_to_RTP)
- [RFC 3550 - RTP](https://datatracker.ietf.org/doc/html/rfc3550)

