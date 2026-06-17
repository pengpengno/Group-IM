import { com } from '../../main/generated/proto-bundle';
import * as $protobuf from 'protobufjs/minimal';
import Long from 'long';

$protobuf.util.Long = Long;
$protobuf.configure();

export import proto = com.github.im.common.connect.model.proto;

export const BaseMessagePkg = proto.BaseMessagePkg;
export const UserInfo = proto.UserInfo;
export const ChatMessage = proto.ChatMessage;
export const Heartbeat = proto.Heartbeat;
export const AckMessage = proto.AckMessage;
export const MessageType = proto.MessageType;
export const MessagesStatus = proto.MessagesStatus;
