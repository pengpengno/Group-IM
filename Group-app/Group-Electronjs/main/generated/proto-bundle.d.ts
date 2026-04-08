import * as $protobuf from "protobufjs";
import Long = require("long");
/** Namespace com. */
export namespace com {

    /** Namespace github. */
    namespace github {

        /** Namespace im. */
        namespace im {

            /** Namespace common. */
            namespace common {

                /** Namespace connect. */
                namespace connect {

                    /** Namespace model. */
                    namespace model {

                        /** Namespace proto. */
                        namespace proto {

                            /** Properties of a BaseMessagePkg. */
                            interface IBaseMessagePkg {

                                /** BaseMessagePkg userInfo */
                                userInfo?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** BaseMessagePkg message */
                                message?: (com.github.im.common.connect.model.proto.IChatMessage|null);

                                /** BaseMessagePkg notification */
                                notification?: (com.github.im.common.connect.model.proto.INotificationInfo|null);

                                /** BaseMessagePkg ack */
                                ack?: (com.github.im.common.connect.model.proto.IAckMessage|null);

                                /** BaseMessagePkg heartbeat */
                                heartbeat?: (com.github.im.common.connect.model.proto.IHeartbeat|null);
                            }

                            /** 基础消息包 */
                            class BaseMessagePkg implements IBaseMessagePkg {

                                /**
                                 * Constructs a new BaseMessagePkg.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.IBaseMessagePkg);

                                /** BaseMessagePkg userInfo. */
                                public userInfo?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** BaseMessagePkg message. */
                                public message?: (com.github.im.common.connect.model.proto.IChatMessage|null);

                                /** BaseMessagePkg notification. */
                                public notification?: (com.github.im.common.connect.model.proto.INotificationInfo|null);

                                /** BaseMessagePkg ack. */
                                public ack?: (com.github.im.common.connect.model.proto.IAckMessage|null);

                                /** BaseMessagePkg heartbeat. */
                                public heartbeat?: (com.github.im.common.connect.model.proto.IHeartbeat|null);

                                /** BaseMessagePkg payload. */
                                public payload?: ("userInfo"|"message"|"notification"|"ack"|"heartbeat");

                                /**
                                 * Creates a new BaseMessagePkg instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns BaseMessagePkg instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.IBaseMessagePkg): com.github.im.common.connect.model.proto.BaseMessagePkg;

                                /**
                                 * Encodes the specified BaseMessagePkg message. Does not implicitly {@link com.github.im.common.connect.model.proto.BaseMessagePkg.verify|verify} messages.
                                 * @param message BaseMessagePkg message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.IBaseMessagePkg, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified BaseMessagePkg message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.BaseMessagePkg.verify|verify} messages.
                                 * @param message BaseMessagePkg message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.IBaseMessagePkg, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes a BaseMessagePkg message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns BaseMessagePkg
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.BaseMessagePkg;

                                /**
                                 * Decodes a BaseMessagePkg message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns BaseMessagePkg
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.BaseMessagePkg;

                                /**
                                 * Verifies a BaseMessagePkg message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates a BaseMessagePkg message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns BaseMessagePkg
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.BaseMessagePkg;

                                /**
                                 * Creates a plain object from a BaseMessagePkg message. Also converts values to other types if specified.
                                 * @param message BaseMessagePkg
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.BaseMessagePkg, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this BaseMessagePkg to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for BaseMessagePkg
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            /** Properties of a Heartbeat. */
                            interface IHeartbeat {

                                /** Heartbeat ping */
                                ping?: (boolean|null);
                            }

                            /** Represents a Heartbeat. */
                            class Heartbeat implements IHeartbeat {

                                /**
                                 * Constructs a new Heartbeat.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.IHeartbeat);

                                /** Heartbeat ping. */
                                public ping: boolean;

                                /**
                                 * Creates a new Heartbeat instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns Heartbeat instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.IHeartbeat): com.github.im.common.connect.model.proto.Heartbeat;

                                /**
                                 * Encodes the specified Heartbeat message. Does not implicitly {@link com.github.im.common.connect.model.proto.Heartbeat.verify|verify} messages.
                                 * @param message Heartbeat message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.IHeartbeat, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified Heartbeat message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.Heartbeat.verify|verify} messages.
                                 * @param message Heartbeat message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.IHeartbeat, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes a Heartbeat message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns Heartbeat
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.Heartbeat;

                                /**
                                 * Decodes a Heartbeat message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns Heartbeat
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.Heartbeat;

                                /**
                                 * Verifies a Heartbeat message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates a Heartbeat message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns Heartbeat
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.Heartbeat;

                                /**
                                 * Creates a plain object from a Heartbeat message. Also converts values to other types if specified.
                                 * @param message Heartbeat
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.Heartbeat, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this Heartbeat to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for Heartbeat
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            /** Properties of a UserInfo. */
                            interface IUserInfo {

                                /** {@link UserInfo#getUsername} */
                                username?: (string|null);

                                /** UserInfo userId */
                                userId?: (number|Long|null);

                                /** UserInfo eMail */
                                eMail?: (string|null);

                                /** UserInfo accessToken */
                                accessToken?: (string|null);

                                /** UserInfo platformType */
                                platformType?: (com.github.im.common.connect.model.proto.PlatformType|null);
                            }

                            /** Represents a UserInfo. */
                            class UserInfo implements IUserInfo {

                                /**
                                 * Constructs a new UserInfo.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.IUserInfo);

                                /** {@link UserInfo#getUsername} */
                                public username: string;

                                /** UserInfo userId. */
                                public userId: (number|Long);

                                /** UserInfo eMail. */
                                public eMail: string;

                                /** UserInfo accessToken. */
                                public accessToken: string;

                                /** UserInfo platformType. */
                                public platformType: com.github.im.common.connect.model.proto.PlatformType;

                                /**
                                 * Creates a new UserInfo instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns UserInfo instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.IUserInfo): com.github.im.common.connect.model.proto.UserInfo;

                                /**
                                 * Encodes the specified UserInfo message. Does not implicitly {@link com.github.im.common.connect.model.proto.UserInfo.verify|verify} messages.
                                 * @param message UserInfo message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.IUserInfo, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified UserInfo message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.UserInfo.verify|verify} messages.
                                 * @param message UserInfo message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.IUserInfo, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes a UserInfo message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns UserInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.UserInfo;

                                /**
                                 * Decodes a UserInfo message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns UserInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.UserInfo;

                                /**
                                 * Verifies a UserInfo message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates a UserInfo message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns UserInfo
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.UserInfo;

                                /**
                                 * Creates a plain object from a UserInfo message. Also converts values to other types if specified.
                                 * @param message UserInfo
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.UserInfo, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this UserInfo to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for UserInfo
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            /** PlatformType enum. */
                            enum PlatformType {
                                WEB = 0,
                                ANDROID = 1,
                                IOS = 2,
                                WINDOWS = 3,
                                MAC = 4,
                                LINUX = 5
                            }

                            /** Properties of a NotificationInfo. */
                            interface INotificationInfo {

                                /** NotificationInfo friendRequest */
                                friendRequest?: (com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null);
                            }

                            /** Represents a NotificationInfo. */
                            class NotificationInfo implements INotificationInfo {

                                /**
                                 * Constructs a new NotificationInfo.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.INotificationInfo);

                                /** NotificationInfo friendRequest. */
                                public friendRequest?: (com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null);

                                /** NotificationInfo payload. */
                                public payload?: "friendRequest";

                                /**
                                 * Creates a new NotificationInfo instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns NotificationInfo instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.INotificationInfo): com.github.im.common.connect.model.proto.NotificationInfo;

                                /**
                                 * Encodes the specified NotificationInfo message. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.verify|verify} messages.
                                 * @param message NotificationInfo message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.INotificationInfo, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified NotificationInfo message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.verify|verify} messages.
                                 * @param message NotificationInfo message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.INotificationInfo, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes a NotificationInfo message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns NotificationInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.NotificationInfo;

                                /**
                                 * Decodes a NotificationInfo message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns NotificationInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.NotificationInfo;

                                /**
                                 * Verifies a NotificationInfo message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates a NotificationInfo message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns NotificationInfo
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.NotificationInfo;

                                /**
                                 * Creates a plain object from a NotificationInfo message. Also converts values to other types if specified.
                                 * @param message NotificationInfo
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.NotificationInfo, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this NotificationInfo to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for NotificationInfo
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            namespace NotificationInfo {

                                /** Properties of a FriendRequest. */
                                interface IFriendRequest {

                                    /** FriendRequest fromUserId */
                                    fromUserId?: (number|null);

                                    /** FriendRequest toUserId */
                                    toUserId?: (number|null);

                                    /** FriendRequest fromUserName */
                                    fromUserName?: (string|null);

                                    /** FriendRequest toUserName */
                                    toUserName?: (string|null);

                                    /** FriendRequest remark */
                                    remark?: (string|null);

                                    /** FriendRequest type */
                                    type?: (com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null);
                                }

                                /** Represents a FriendRequest. */
                                class FriendRequest implements IFriendRequest {

                                    /**
                                     * Constructs a new FriendRequest.
                                     * @param [properties] Properties to set
                                     */
                                    constructor(properties?: com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest);

                                    /** FriendRequest fromUserId. */
                                    public fromUserId: number;

                                    /** FriendRequest toUserId. */
                                    public toUserId: number;

                                    /** FriendRequest fromUserName. */
                                    public fromUserName: string;

                                    /** FriendRequest toUserName. */
                                    public toUserName: string;

                                    /** FriendRequest remark. */
                                    public remark: string;

                                    /** FriendRequest type. */
                                    public type?: (com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null);

                                    /**
                                     * Creates a new FriendRequest instance using the specified properties.
                                     * @param [properties] Properties to set
                                     * @returns FriendRequest instance
                                     */
                                    public static create(properties?: com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest): com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest;

                                    /**
                                     * Encodes the specified FriendRequest message. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify|verify} messages.
                                     * @param message FriendRequest message or plain object to encode
                                     * @param [writer] Writer to encode to
                                     * @returns Writer
                                     */
                                    public static encode(message: com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest, writer?: $protobuf.Writer): $protobuf.Writer;

                                    /**
                                     * Encodes the specified FriendRequest message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify|verify} messages.
                                     * @param message FriendRequest message or plain object to encode
                                     * @param [writer] Writer to encode to
                                     * @returns Writer
                                     */
                                    public static encodeDelimited(message: com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest, writer?: $protobuf.Writer): $protobuf.Writer;

                                    /**
                                     * Decodes a FriendRequest message from the specified reader or buffer.
                                     * @param reader Reader or buffer to decode from
                                     * @param [length] Message length if known beforehand
                                     * @returns FriendRequest
                                     * @throws {Error} If the payload is not a reader or valid buffer
                                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                     */
                                    public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest;

                                    /**
                                     * Decodes a FriendRequest message from the specified reader or buffer, length delimited.
                                     * @param reader Reader or buffer to decode from
                                     * @returns FriendRequest
                                     * @throws {Error} If the payload is not a reader or valid buffer
                                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                     */
                                    public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest;

                                    /**
                                     * Verifies a FriendRequest message.
                                     * @param message Plain object to verify
                                     * @returns `null` if valid, otherwise the reason why it is not
                                     */
                                    public static verify(message: { [k: string]: any }): (string|null);

                                    /**
                                     * Creates a FriendRequest message from a plain object. Also converts values to their respective internal types.
                                     * @param object Plain object
                                     * @returns FriendRequest
                                     */
                                    public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest;

                                    /**
                                     * Creates a plain object from a FriendRequest message. Also converts values to other types if specified.
                                     * @param message FriendRequest
                                     * @param [options] Conversion options
                                     * @returns Plain object
                                     */
                                    public static toObject(message: com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                    /**
                                     * Converts this FriendRequest to JSON.
                                     * @returns JSON object
                                     */
                                    public toJSON(): { [k: string]: any };

                                    /**
                                     * Gets the default type url for FriendRequest
                                     * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                     * @returns The default type url
                                     */
                                    public static getTypeUrl(typeUrlPrefix?: string): string;
                                }

                                namespace FriendRequest {

                                    /** FriendRequestType enum. */
                                    enum FriendRequestType {
                                        ADD = 0,
                                        DELETE = 1,
                                        ACCEPT = 2,
                                        REJECT = 3
                                    }
                                }
                            }

                            /** Properties of a ChatMessage. */
                            interface IChatMessage {

                                /** ChatMessage msgId */
                                msgId?: (number|Long|null);

                                /** ChatMessage clientMsgId */
                                clientMsgId?: (string|null);

                                /** ChatMessage content */
                                content?: (string|null);

                                /** ChatMessage conversationId */
                                conversationId?: (number|Long|null);

                                /** ChatMessage conversationName */
                                conversationName?: (string|null);

                                /** ChatMessage fromUser */
                                fromUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** ChatMessage toUser */
                                toUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** ChatMessage clientTimeStamp */
                                clientTimeStamp?: (number|Long|null);

                                /** ChatMessage serverTimeStamp */
                                serverTimeStamp?: (number|Long|null);

                                /** ChatMessage type */
                                type?: (com.github.im.common.connect.model.proto.MessageType|null);

                                /** ChatMessage messagesStatus */
                                messagesStatus?: (com.github.im.common.connect.model.proto.MessagesStatus|null);

                                /** ChatMessage sequenceId */
                                sequenceId?: (number|Long|null);
                            }

                            /** Represents a ChatMessage. */
                            class ChatMessage implements IChatMessage {

                                /**
                                 * Constructs a new ChatMessage.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.IChatMessage);

                                /** ChatMessage msgId. */
                                public msgId: (number|Long);

                                /** ChatMessage clientMsgId. */
                                public clientMsgId: string;

                                /** ChatMessage content. */
                                public content: string;

                                /** ChatMessage conversationId. */
                                public conversationId: (number|Long);

                                /** ChatMessage conversationName. */
                                public conversationName: string;

                                /** ChatMessage fromUser. */
                                public fromUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** ChatMessage toUser. */
                                public toUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** ChatMessage clientTimeStamp. */
                                public clientTimeStamp: (number|Long);

                                /** ChatMessage serverTimeStamp. */
                                public serverTimeStamp: (number|Long);

                                /** ChatMessage type. */
                                public type: com.github.im.common.connect.model.proto.MessageType;

                                /** ChatMessage messagesStatus. */
                                public messagesStatus: com.github.im.common.connect.model.proto.MessagesStatus;

                                /** ChatMessage sequenceId. */
                                public sequenceId: (number|Long);

                                /**
                                 * Creates a new ChatMessage instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns ChatMessage instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.IChatMessage): com.github.im.common.connect.model.proto.ChatMessage;

                                /**
                                 * Encodes the specified ChatMessage message. Does not implicitly {@link com.github.im.common.connect.model.proto.ChatMessage.verify|verify} messages.
                                 * @param message ChatMessage message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.IChatMessage, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified ChatMessage message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.ChatMessage.verify|verify} messages.
                                 * @param message ChatMessage message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.IChatMessage, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes a ChatMessage message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns ChatMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.ChatMessage;

                                /**
                                 * Decodes a ChatMessage message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns ChatMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.ChatMessage;

                                /**
                                 * Verifies a ChatMessage message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates a ChatMessage message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns ChatMessage
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.ChatMessage;

                                /**
                                 * Creates a plain object from a ChatMessage message. Also converts values to other types if specified.
                                 * @param message ChatMessage
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.ChatMessage, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this ChatMessage to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for ChatMessage
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            /** 消息状态 */
                            enum MessagesStatus {
                                SENDING = 0,
                                SENT = 1,
                                FAILED = 2,
                                RECEIVED = 3,
                                READ = 4,
                                UNREAD = 5,
                                DELETED = 6,
                                REVOKE = 7
                            }

                            /** Properties of an AckMessage. */
                            interface IAckMessage {

                                /** AckMessage clientMsgId */
                                clientMsgId?: (string|null);

                                /** AckMessage serverMsgId */
                                serverMsgId?: (number|Long|null);

                                /** AckMessage conversationId */
                                conversationId?: (number|Long|null);

                                /** AckMessage fromUser */
                                fromUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** AckMessage ackTimestamp */
                                ackTimestamp?: (number|Long|null);

                                /** AckMessage status */
                                status?: (com.github.im.common.connect.model.proto.MessagesStatus|null);
                            }

                            /** ACK 应答消息 */
                            class AckMessage implements IAckMessage {

                                /**
                                 * Constructs a new AckMessage.
                                 * @param [properties] Properties to set
                                 */
                                constructor(properties?: com.github.im.common.connect.model.proto.IAckMessage);

                                /** AckMessage clientMsgId. */
                                public clientMsgId: string;

                                /** AckMessage serverMsgId. */
                                public serverMsgId: (number|Long);

                                /** AckMessage conversationId. */
                                public conversationId: (number|Long);

                                /** AckMessage fromUser. */
                                public fromUser?: (com.github.im.common.connect.model.proto.IUserInfo|null);

                                /** AckMessage ackTimestamp. */
                                public ackTimestamp: (number|Long);

                                /** AckMessage status. */
                                public status: com.github.im.common.connect.model.proto.MessagesStatus;

                                /**
                                 * Creates a new AckMessage instance using the specified properties.
                                 * @param [properties] Properties to set
                                 * @returns AckMessage instance
                                 */
                                public static create(properties?: com.github.im.common.connect.model.proto.IAckMessage): com.github.im.common.connect.model.proto.AckMessage;

                                /**
                                 * Encodes the specified AckMessage message. Does not implicitly {@link com.github.im.common.connect.model.proto.AckMessage.verify|verify} messages.
                                 * @param message AckMessage message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encode(message: com.github.im.common.connect.model.proto.IAckMessage, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Encodes the specified AckMessage message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.AckMessage.verify|verify} messages.
                                 * @param message AckMessage message or plain object to encode
                                 * @param [writer] Writer to encode to
                                 * @returns Writer
                                 */
                                public static encodeDelimited(message: com.github.im.common.connect.model.proto.IAckMessage, writer?: $protobuf.Writer): $protobuf.Writer;

                                /**
                                 * Decodes an AckMessage message from the specified reader or buffer.
                                 * @param reader Reader or buffer to decode from
                                 * @param [length] Message length if known beforehand
                                 * @returns AckMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.github.im.common.connect.model.proto.AckMessage;

                                /**
                                 * Decodes an AckMessage message from the specified reader or buffer, length delimited.
                                 * @param reader Reader or buffer to decode from
                                 * @returns AckMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.github.im.common.connect.model.proto.AckMessage;

                                /**
                                 * Verifies an AckMessage message.
                                 * @param message Plain object to verify
                                 * @returns `null` if valid, otherwise the reason why it is not
                                 */
                                public static verify(message: { [k: string]: any }): (string|null);

                                /**
                                 * Creates an AckMessage message from a plain object. Also converts values to their respective internal types.
                                 * @param object Plain object
                                 * @returns AckMessage
                                 */
                                public static fromObject(object: { [k: string]: any }): com.github.im.common.connect.model.proto.AckMessage;

                                /**
                                 * Creates a plain object from an AckMessage message. Also converts values to other types if specified.
                                 * @param message AckMessage
                                 * @param [options] Conversion options
                                 * @returns Plain object
                                 */
                                public static toObject(message: com.github.im.common.connect.model.proto.AckMessage, options?: $protobuf.IConversionOptions): { [k: string]: any };

                                /**
                                 * Converts this AckMessage to JSON.
                                 * @returns JSON object
                                 */
                                public toJSON(): { [k: string]: any };

                                /**
                                 * Gets the default type url for AckMessage
                                 * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns The default type url
                                 */
                                public static getTypeUrl(typeUrlPrefix?: string): string;
                            }

                            /** MessageType enum. */
                            enum MessageType {
                                TEXT = 0,
                                FILE = 1,
                                VIDEO = 3,
                                IMAGE = 6,
                                VOICE = 4,
                                MEETING = 7
                            }
                        }
                    }
                }
            }
        }
    }
}
