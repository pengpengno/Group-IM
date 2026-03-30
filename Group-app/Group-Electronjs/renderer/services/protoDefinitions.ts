import protobuf from 'protobufjs';

/**
 * Minimal Proto Definitions for Renderer
 * Matches the .proto files used in the backend
 */
const protoSource = `
syntax = "proto3";

package com.github.im.common.connect.model.proto;

message BaseMessagePkg {
  oneof payload {
    UserInfo userInfo = 1;
    ChatMessage message = 2;
    NotificationInfo notification = 3;
    AckMessage ack = 4;
    Heartbeat heartbeat = 5;
  }
}

message UserInfo {
  string username = 1;
  int64 userId = 2;
  string eMail = 3;
  string accessToken = 4;
  int32 platformType = 5;
}

message ChatMessage {
  int64 msgId = 1;
  int64 conversationId = 2;
  string content = 3;
  int64 fromAccountId = 4;
  string type = 5;
  int64 clientTimeStamp = 6;
  int64 serverTimeStamp = 7;
  string clientMsgId = 8;
  int64 sequenceId = 9;
  UserInfo fromUser = 10;
}

message NotificationInfo {
  string type = 1;
  string content = 2;
  string title = 3;
}

message AckMessage {
  int64 msgId = 1;
  int64 conversationId = 2;
  string status = 3;
  int64 sequenceId = 4;
}

message Heartbeat {
  bool ping = 1;
}
`;

const root = protobuf.parse(protoSource).root;

export const BaseMessagePkg = root.lookupType("com.github.im.common.connect.model.proto.BaseMessagePkg");
export const UserInfo = root.lookupType("com.github.im.common.connect.model.proto.UserInfo");
export const ChatMessage = root.lookupType("com.github.im.common.connect.model.proto.ChatMessage");
export const Heartbeat = root.lookupType("com.github.im.common.connect.model.proto.Heartbeat");
export const AckMessage = root.lookupType("com.github.im.common.connect.model.proto.AckMessage");
