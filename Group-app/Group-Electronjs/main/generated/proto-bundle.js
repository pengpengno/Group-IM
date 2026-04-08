/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars*/
"use strict";

var $protobuf = require("protobufjs/minimal");

// Common aliases
var $Reader = $protobuf.Reader, $Writer = $protobuf.Writer, $util = $protobuf.util;

// Exported root namespace
var $root = $protobuf.roots["default"] || ($protobuf.roots["default"] = {});

$root.com = (function() {

    /**
     * Namespace com.
     * @exports com
     * @namespace
     */
    var com = {};

    com.github = (function() {

        /**
         * Namespace github.
         * @memberof com
         * @namespace
         */
        var github = {};

        github.im = (function() {

            /**
             * Namespace im.
             * @memberof com.github
             * @namespace
             */
            var im = {};

            im.common = (function() {

                /**
                 * Namespace common.
                 * @memberof com.github.im
                 * @namespace
                 */
                var common = {};

                common.connect = (function() {

                    /**
                     * Namespace connect.
                     * @memberof com.github.im.common
                     * @namespace
                     */
                    var connect = {};

                    connect.model = (function() {

                        /**
                         * Namespace model.
                         * @memberof com.github.im.common.connect
                         * @namespace
                         */
                        var model = {};

                        model.proto = (function() {

                            /**
                             * Namespace proto.
                             * @memberof com.github.im.common.connect.model
                             * @namespace
                             */
                            var proto = {};

                            proto.BaseMessagePkg = (function() {

                                /**
                                 * Properties of a BaseMessagePkg.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface IBaseMessagePkg
                                 * @property {com.github.im.common.connect.model.proto.IUserInfo|null} [userInfo] BaseMessagePkg userInfo
                                 * @property {com.github.im.common.connect.model.proto.IChatMessage|null} [message] BaseMessagePkg message
                                 * @property {com.github.im.common.connect.model.proto.INotificationInfo|null} [notification] BaseMessagePkg notification
                                 * @property {com.github.im.common.connect.model.proto.IAckMessage|null} [ack] BaseMessagePkg ack
                                 * @property {com.github.im.common.connect.model.proto.IHeartbeat|null} [heartbeat] BaseMessagePkg heartbeat
                                 */

                                /**
                                 * Constructs a new BaseMessagePkg.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc 基础消息包
                                 * @implements IBaseMessagePkg
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.IBaseMessagePkg=} [properties] Properties to set
                                 */
                                function BaseMessagePkg(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * BaseMessagePkg userInfo.
                                 * @member {com.github.im.common.connect.model.proto.IUserInfo|null|undefined} userInfo
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                BaseMessagePkg.prototype.userInfo = null;

                                /**
                                 * BaseMessagePkg message.
                                 * @member {com.github.im.common.connect.model.proto.IChatMessage|null|undefined} message
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                BaseMessagePkg.prototype.message = null;

                                /**
                                 * BaseMessagePkg notification.
                                 * @member {com.github.im.common.connect.model.proto.INotificationInfo|null|undefined} notification
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                BaseMessagePkg.prototype.notification = null;

                                /**
                                 * BaseMessagePkg ack.
                                 * @member {com.github.im.common.connect.model.proto.IAckMessage|null|undefined} ack
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                BaseMessagePkg.prototype.ack = null;

                                /**
                                 * BaseMessagePkg heartbeat.
                                 * @member {com.github.im.common.connect.model.proto.IHeartbeat|null|undefined} heartbeat
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                BaseMessagePkg.prototype.heartbeat = null;

                                // OneOf field names bound to virtual getters and setters
                                var $oneOfFields;

                                /**
                                 * BaseMessagePkg payload.
                                 * @member {"userInfo"|"message"|"notification"|"ack"|"heartbeat"|undefined} payload
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 */
                                Object.defineProperty(BaseMessagePkg.prototype, "payload", {
                                    get: $util.oneOfGetter($oneOfFields = ["userInfo", "message", "notification", "ack", "heartbeat"]),
                                    set: $util.oneOfSetter($oneOfFields)
                                });

                                /**
                                 * Creates a new BaseMessagePkg instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IBaseMessagePkg=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.BaseMessagePkg} BaseMessagePkg instance
                                 */
                                BaseMessagePkg.create = function create(properties) {
                                    return new BaseMessagePkg(properties);
                                };

                                /**
                                 * Encodes the specified BaseMessagePkg message. Does not implicitly {@link com.github.im.common.connect.model.proto.BaseMessagePkg.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IBaseMessagePkg} message BaseMessagePkg message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                BaseMessagePkg.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.userInfo != null && Object.hasOwnProperty.call(message, "userInfo"))
                                        $root.com.github.im.common.connect.model.proto.UserInfo.encode(message.userInfo, writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
                                    if (message.message != null && Object.hasOwnProperty.call(message, "message"))
                                        $root.com.github.im.common.connect.model.proto.ChatMessage.encode(message.message, writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
                                    if (message.notification != null && Object.hasOwnProperty.call(message, "notification"))
                                        $root.com.github.im.common.connect.model.proto.NotificationInfo.encode(message.notification, writer.uint32(/* id 3, wireType 2 =*/26).fork()).ldelim();
                                    if (message.ack != null && Object.hasOwnProperty.call(message, "ack"))
                                        $root.com.github.im.common.connect.model.proto.AckMessage.encode(message.ack, writer.uint32(/* id 4, wireType 2 =*/34).fork()).ldelim();
                                    if (message.heartbeat != null && Object.hasOwnProperty.call(message, "heartbeat"))
                                        $root.com.github.im.common.connect.model.proto.Heartbeat.encode(message.heartbeat, writer.uint32(/* id 5, wireType 2 =*/42).fork()).ldelim();
                                    return writer;
                                };

                                /**
                                 * Encodes the specified BaseMessagePkg message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.BaseMessagePkg.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IBaseMessagePkg} message BaseMessagePkg message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                BaseMessagePkg.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes a BaseMessagePkg message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.BaseMessagePkg} BaseMessagePkg
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                BaseMessagePkg.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.BaseMessagePkg();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.userInfo = $root.com.github.im.common.connect.model.proto.UserInfo.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 2: {
                                                message.message = $root.com.github.im.common.connect.model.proto.ChatMessage.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 3: {
                                                message.notification = $root.com.github.im.common.connect.model.proto.NotificationInfo.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 4: {
                                                message.ack = $root.com.github.im.common.connect.model.proto.AckMessage.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 5: {
                                                message.heartbeat = $root.com.github.im.common.connect.model.proto.Heartbeat.decode(reader, reader.uint32());
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes a BaseMessagePkg message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.BaseMessagePkg} BaseMessagePkg
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                BaseMessagePkg.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies a BaseMessagePkg message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                BaseMessagePkg.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    var properties = {};
                                    if (message.userInfo != null && message.hasOwnProperty("userInfo")) {
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.UserInfo.verify(message.userInfo);
                                            if (error)
                                                return "userInfo." + error;
                                        }
                                    }
                                    if (message.message != null && message.hasOwnProperty("message")) {
                                        if (properties.payload === 1)
                                            return "payload: multiple values";
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.ChatMessage.verify(message.message);
                                            if (error)
                                                return "message." + error;
                                        }
                                    }
                                    if (message.notification != null && message.hasOwnProperty("notification")) {
                                        if (properties.payload === 1)
                                            return "payload: multiple values";
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.NotificationInfo.verify(message.notification);
                                            if (error)
                                                return "notification." + error;
                                        }
                                    }
                                    if (message.ack != null && message.hasOwnProperty("ack")) {
                                        if (properties.payload === 1)
                                            return "payload: multiple values";
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.AckMessage.verify(message.ack);
                                            if (error)
                                                return "ack." + error;
                                        }
                                    }
                                    if (message.heartbeat != null && message.hasOwnProperty("heartbeat")) {
                                        if (properties.payload === 1)
                                            return "payload: multiple values";
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.Heartbeat.verify(message.heartbeat);
                                            if (error)
                                                return "heartbeat." + error;
                                        }
                                    }
                                    return null;
                                };

                                /**
                                 * Creates a BaseMessagePkg message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.BaseMessagePkg} BaseMessagePkg
                                 */
                                BaseMessagePkg.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.BaseMessagePkg)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.BaseMessagePkg();
                                    if (object.userInfo != null) {
                                        if (typeof object.userInfo !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.BaseMessagePkg.userInfo: object expected");
                                        message.userInfo = $root.com.github.im.common.connect.model.proto.UserInfo.fromObject(object.userInfo);
                                    }
                                    if (object.message != null) {
                                        if (typeof object.message !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.BaseMessagePkg.message: object expected");
                                        message.message = $root.com.github.im.common.connect.model.proto.ChatMessage.fromObject(object.message);
                                    }
                                    if (object.notification != null) {
                                        if (typeof object.notification !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.BaseMessagePkg.notification: object expected");
                                        message.notification = $root.com.github.im.common.connect.model.proto.NotificationInfo.fromObject(object.notification);
                                    }
                                    if (object.ack != null) {
                                        if (typeof object.ack !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.BaseMessagePkg.ack: object expected");
                                        message.ack = $root.com.github.im.common.connect.model.proto.AckMessage.fromObject(object.ack);
                                    }
                                    if (object.heartbeat != null) {
                                        if (typeof object.heartbeat !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.BaseMessagePkg.heartbeat: object expected");
                                        message.heartbeat = $root.com.github.im.common.connect.model.proto.Heartbeat.fromObject(object.heartbeat);
                                    }
                                    return message;
                                };

                                /**
                                 * Creates a plain object from a BaseMessagePkg message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.BaseMessagePkg} message BaseMessagePkg
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                BaseMessagePkg.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (message.userInfo != null && message.hasOwnProperty("userInfo")) {
                                        object.userInfo = $root.com.github.im.common.connect.model.proto.UserInfo.toObject(message.userInfo, options);
                                        if (options.oneofs)
                                            object.payload = "userInfo";
                                    }
                                    if (message.message != null && message.hasOwnProperty("message")) {
                                        object.message = $root.com.github.im.common.connect.model.proto.ChatMessage.toObject(message.message, options);
                                        if (options.oneofs)
                                            object.payload = "message";
                                    }
                                    if (message.notification != null && message.hasOwnProperty("notification")) {
                                        object.notification = $root.com.github.im.common.connect.model.proto.NotificationInfo.toObject(message.notification, options);
                                        if (options.oneofs)
                                            object.payload = "notification";
                                    }
                                    if (message.ack != null && message.hasOwnProperty("ack")) {
                                        object.ack = $root.com.github.im.common.connect.model.proto.AckMessage.toObject(message.ack, options);
                                        if (options.oneofs)
                                            object.payload = "ack";
                                    }
                                    if (message.heartbeat != null && message.hasOwnProperty("heartbeat")) {
                                        object.heartbeat = $root.com.github.im.common.connect.model.proto.Heartbeat.toObject(message.heartbeat, options);
                                        if (options.oneofs)
                                            object.payload = "heartbeat";
                                    }
                                    return object;
                                };

                                /**
                                 * Converts this BaseMessagePkg to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                BaseMessagePkg.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for BaseMessagePkg
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.BaseMessagePkg
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                BaseMessagePkg.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.BaseMessagePkg";
                                };

                                return BaseMessagePkg;
                            })();

                            proto.Heartbeat = (function() {

                                /**
                                 * Properties of a Heartbeat.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface IHeartbeat
                                 * @property {boolean|null} [ping] Heartbeat ping
                                 */

                                /**
                                 * Constructs a new Heartbeat.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc Represents a Heartbeat.
                                 * @implements IHeartbeat
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.IHeartbeat=} [properties] Properties to set
                                 */
                                function Heartbeat(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * Heartbeat ping.
                                 * @member {boolean} ping
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @instance
                                 */
                                Heartbeat.prototype.ping = false;

                                /**
                                 * Creates a new Heartbeat instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IHeartbeat=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.Heartbeat} Heartbeat instance
                                 */
                                Heartbeat.create = function create(properties) {
                                    return new Heartbeat(properties);
                                };

                                /**
                                 * Encodes the specified Heartbeat message. Does not implicitly {@link com.github.im.common.connect.model.proto.Heartbeat.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IHeartbeat} message Heartbeat message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                Heartbeat.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.ping != null && Object.hasOwnProperty.call(message, "ping"))
                                        writer.uint32(/* id 1, wireType 0 =*/8).bool(message.ping);
                                    return writer;
                                };

                                /**
                                 * Encodes the specified Heartbeat message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.Heartbeat.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IHeartbeat} message Heartbeat message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                Heartbeat.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes a Heartbeat message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.Heartbeat} Heartbeat
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                Heartbeat.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.Heartbeat();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.ping = reader.bool();
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes a Heartbeat message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.Heartbeat} Heartbeat
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                Heartbeat.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies a Heartbeat message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                Heartbeat.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    if (message.ping != null && message.hasOwnProperty("ping"))
                                        if (typeof message.ping !== "boolean")
                                            return "ping: boolean expected";
                                    return null;
                                };

                                /**
                                 * Creates a Heartbeat message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.Heartbeat} Heartbeat
                                 */
                                Heartbeat.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.Heartbeat)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.Heartbeat();
                                    if (object.ping != null)
                                        message.ping = Boolean(object.ping);
                                    return message;
                                };

                                /**
                                 * Creates a plain object from a Heartbeat message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.Heartbeat} message Heartbeat
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                Heartbeat.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (options.defaults)
                                        object.ping = false;
                                    if (message.ping != null && message.hasOwnProperty("ping"))
                                        object.ping = message.ping;
                                    return object;
                                };

                                /**
                                 * Converts this Heartbeat to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                Heartbeat.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for Heartbeat
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.Heartbeat
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                Heartbeat.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.Heartbeat";
                                };

                                return Heartbeat;
                            })();

                            proto.UserInfo = (function() {

                                /**
                                 * Properties of a UserInfo.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface IUserInfo
                                 * @property {string|null} [username] {@link UserInfo#getUsername}
                                 * @property {number|Long|null} [userId] UserInfo userId
                                 * @property {string|null} [eMail] UserInfo eMail
                                 * @property {string|null} [accessToken] UserInfo accessToken
                                 * @property {com.github.im.common.connect.model.proto.PlatformType|null} [platformType] UserInfo platformType
                                 */

                                /**
                                 * Constructs a new UserInfo.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc Represents a UserInfo.
                                 * @implements IUserInfo
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.IUserInfo=} [properties] Properties to set
                                 */
                                function UserInfo(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * {@link UserInfo#getUsername}
                                 * @member {string} username
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 */
                                UserInfo.prototype.username = "";

                                /**
                                 * UserInfo userId.
                                 * @member {number|Long} userId
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 */
                                UserInfo.prototype.userId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * UserInfo eMail.
                                 * @member {string} eMail
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 */
                                UserInfo.prototype.eMail = "";

                                /**
                                 * UserInfo accessToken.
                                 * @member {string} accessToken
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 */
                                UserInfo.prototype.accessToken = "";

                                /**
                                 * UserInfo platformType.
                                 * @member {com.github.im.common.connect.model.proto.PlatformType} platformType
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 */
                                UserInfo.prototype.platformType = 0;

                                /**
                                 * Creates a new UserInfo instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IUserInfo=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.UserInfo} UserInfo instance
                                 */
                                UserInfo.create = function create(properties) {
                                    return new UserInfo(properties);
                                };

                                /**
                                 * Encodes the specified UserInfo message. Does not implicitly {@link com.github.im.common.connect.model.proto.UserInfo.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IUserInfo} message UserInfo message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                UserInfo.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.username != null && Object.hasOwnProperty.call(message, "username"))
                                        writer.uint32(/* id 1, wireType 2 =*/10).string(message.username);
                                    if (message.userId != null && Object.hasOwnProperty.call(message, "userId"))
                                        writer.uint32(/* id 2, wireType 0 =*/16).int64(message.userId);
                                    if (message.eMail != null && Object.hasOwnProperty.call(message, "eMail"))
                                        writer.uint32(/* id 3, wireType 2 =*/26).string(message.eMail);
                                    if (message.accessToken != null && Object.hasOwnProperty.call(message, "accessToken"))
                                        writer.uint32(/* id 4, wireType 2 =*/34).string(message.accessToken);
                                    if (message.platformType != null && Object.hasOwnProperty.call(message, "platformType"))
                                        writer.uint32(/* id 5, wireType 0 =*/40).int32(message.platformType);
                                    return writer;
                                };

                                /**
                                 * Encodes the specified UserInfo message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.UserInfo.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IUserInfo} message UserInfo message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                UserInfo.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes a UserInfo message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.UserInfo} UserInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                UserInfo.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.UserInfo();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.username = reader.string();
                                                break;
                                            }
                                        case 2: {
                                                message.userId = reader.int64();
                                                break;
                                            }
                                        case 3: {
                                                message.eMail = reader.string();
                                                break;
                                            }
                                        case 4: {
                                                message.accessToken = reader.string();
                                                break;
                                            }
                                        case 5: {
                                                message.platformType = reader.int32();
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes a UserInfo message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.UserInfo} UserInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                UserInfo.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies a UserInfo message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                UserInfo.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    if (message.username != null && message.hasOwnProperty("username"))
                                        if (!$util.isString(message.username))
                                            return "username: string expected";
                                    if (message.userId != null && message.hasOwnProperty("userId"))
                                        if (!$util.isInteger(message.userId) && !(message.userId && $util.isInteger(message.userId.low) && $util.isInteger(message.userId.high)))
                                            return "userId: integer|Long expected";
                                    if (message.eMail != null && message.hasOwnProperty("eMail"))
                                        if (!$util.isString(message.eMail))
                                            return "eMail: string expected";
                                    if (message.accessToken != null && message.hasOwnProperty("accessToken"))
                                        if (!$util.isString(message.accessToken))
                                            return "accessToken: string expected";
                                    if (message.platformType != null && message.hasOwnProperty("platformType"))
                                        switch (message.platformType) {
                                        default:
                                            return "platformType: enum value expected";
                                        case 0:
                                        case 1:
                                        case 2:
                                        case 3:
                                        case 4:
                                        case 5:
                                            break;
                                        }
                                    return null;
                                };

                                /**
                                 * Creates a UserInfo message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.UserInfo} UserInfo
                                 */
                                UserInfo.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.UserInfo)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.UserInfo();
                                    if (object.username != null)
                                        message.username = String(object.username);
                                    if (object.userId != null)
                                        if ($util.Long)
                                            (message.userId = $util.Long.fromValue(object.userId)).unsigned = false;
                                        else if (typeof object.userId === "string")
                                            message.userId = parseInt(object.userId, 10);
                                        else if (typeof object.userId === "number")
                                            message.userId = object.userId;
                                        else if (typeof object.userId === "object")
                                            message.userId = new $util.LongBits(object.userId.low >>> 0, object.userId.high >>> 0).toNumber();
                                    if (object.eMail != null)
                                        message.eMail = String(object.eMail);
                                    if (object.accessToken != null)
                                        message.accessToken = String(object.accessToken);
                                    switch (object.platformType) {
                                    default:
                                        if (typeof object.platformType === "number") {
                                            message.platformType = object.platformType;
                                            break;
                                        }
                                        break;
                                    case "WEB":
                                    case 0:
                                        message.platformType = 0;
                                        break;
                                    case "ANDROID":
                                    case 1:
                                        message.platformType = 1;
                                        break;
                                    case "IOS":
                                    case 2:
                                        message.platformType = 2;
                                        break;
                                    case "WINDOWS":
                                    case 3:
                                        message.platformType = 3;
                                        break;
                                    case "MAC":
                                    case 4:
                                        message.platformType = 4;
                                        break;
                                    case "LINUX":
                                    case 5:
                                        message.platformType = 5;
                                        break;
                                    }
                                    return message;
                                };

                                /**
                                 * Creates a plain object from a UserInfo message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.UserInfo} message UserInfo
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                UserInfo.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (options.defaults) {
                                        object.username = "";
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.userId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.userId = options.longs === String ? "0" : 0;
                                        object.eMail = "";
                                        object.accessToken = "";
                                        object.platformType = options.enums === String ? "WEB" : 0;
                                    }
                                    if (message.username != null && message.hasOwnProperty("username"))
                                        object.username = message.username;
                                    if (message.userId != null && message.hasOwnProperty("userId"))
                                        if (typeof message.userId === "number")
                                            object.userId = options.longs === String ? String(message.userId) : message.userId;
                                        else
                                            object.userId = options.longs === String ? $util.Long.prototype.toString.call(message.userId) : options.longs === Number ? new $util.LongBits(message.userId.low >>> 0, message.userId.high >>> 0).toNumber() : message.userId;
                                    if (message.eMail != null && message.hasOwnProperty("eMail"))
                                        object.eMail = message.eMail;
                                    if (message.accessToken != null && message.hasOwnProperty("accessToken"))
                                        object.accessToken = message.accessToken;
                                    if (message.platformType != null && message.hasOwnProperty("platformType"))
                                        object.platformType = options.enums === String ? $root.com.github.im.common.connect.model.proto.PlatformType[message.platformType] === undefined ? message.platformType : $root.com.github.im.common.connect.model.proto.PlatformType[message.platformType] : message.platformType;
                                    return object;
                                };

                                /**
                                 * Converts this UserInfo to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                UserInfo.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for UserInfo
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.UserInfo
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                UserInfo.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.UserInfo";
                                };

                                return UserInfo;
                            })();

                            /**
                             * PlatformType enum.
                             * @name com.github.im.common.connect.model.proto.PlatformType
                             * @enum {number}
                             * @property {number} WEB=0 WEB value
                             * @property {number} ANDROID=1 ANDROID value
                             * @property {number} IOS=2 IOS value
                             * @property {number} WINDOWS=3 WINDOWS value
                             * @property {number} MAC=4 MAC value
                             * @property {number} LINUX=5 LINUX value
                             */
                            proto.PlatformType = (function() {
                                var valuesById = {}, values = Object.create(valuesById);
                                values[valuesById[0] = "WEB"] = 0;
                                values[valuesById[1] = "ANDROID"] = 1;
                                values[valuesById[2] = "IOS"] = 2;
                                values[valuesById[3] = "WINDOWS"] = 3;
                                values[valuesById[4] = "MAC"] = 4;
                                values[valuesById[5] = "LINUX"] = 5;
                                return values;
                            })();

                            proto.NotificationInfo = (function() {

                                /**
                                 * Properties of a NotificationInfo.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface INotificationInfo
                                 * @property {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null} [friendRequest] NotificationInfo friendRequest
                                 */

                                /**
                                 * Constructs a new NotificationInfo.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc Represents a NotificationInfo.
                                 * @implements INotificationInfo
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.INotificationInfo=} [properties] Properties to set
                                 */
                                function NotificationInfo(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * NotificationInfo friendRequest.
                                 * @member {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null|undefined} friendRequest
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @instance
                                 */
                                NotificationInfo.prototype.friendRequest = null;

                                // OneOf field names bound to virtual getters and setters
                                var $oneOfFields;

                                /**
                                 * NotificationInfo payload.
                                 * @member {"friendRequest"|undefined} payload
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @instance
                                 */
                                Object.defineProperty(NotificationInfo.prototype, "payload", {
                                    get: $util.oneOfGetter($oneOfFields = ["friendRequest"]),
                                    set: $util.oneOfSetter($oneOfFields)
                                });

                                /**
                                 * Creates a new NotificationInfo instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.INotificationInfo=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.NotificationInfo} NotificationInfo instance
                                 */
                                NotificationInfo.create = function create(properties) {
                                    return new NotificationInfo(properties);
                                };

                                /**
                                 * Encodes the specified NotificationInfo message. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.INotificationInfo} message NotificationInfo message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                NotificationInfo.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.friendRequest != null && Object.hasOwnProperty.call(message, "friendRequest"))
                                        $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.encode(message.friendRequest, writer.uint32(/* id 1, wireType 2 =*/10).fork()).ldelim();
                                    return writer;
                                };

                                /**
                                 * Encodes the specified NotificationInfo message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.INotificationInfo} message NotificationInfo message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                NotificationInfo.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes a NotificationInfo message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.NotificationInfo} NotificationInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                NotificationInfo.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.NotificationInfo();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.friendRequest = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.decode(reader, reader.uint32());
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes a NotificationInfo message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.NotificationInfo} NotificationInfo
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                NotificationInfo.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies a NotificationInfo message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                NotificationInfo.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    var properties = {};
                                    if (message.friendRequest != null && message.hasOwnProperty("friendRequest")) {
                                        properties.payload = 1;
                                        {
                                            var error = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify(message.friendRequest);
                                            if (error)
                                                return "friendRequest." + error;
                                        }
                                    }
                                    return null;
                                };

                                /**
                                 * Creates a NotificationInfo message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.NotificationInfo} NotificationInfo
                                 */
                                NotificationInfo.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.NotificationInfo)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.NotificationInfo();
                                    if (object.friendRequest != null) {
                                        if (typeof object.friendRequest !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.NotificationInfo.friendRequest: object expected");
                                        message.friendRequest = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.fromObject(object.friendRequest);
                                    }
                                    return message;
                                };

                                /**
                                 * Creates a plain object from a NotificationInfo message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.NotificationInfo} message NotificationInfo
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                NotificationInfo.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (message.friendRequest != null && message.hasOwnProperty("friendRequest")) {
                                        object.friendRequest = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.toObject(message.friendRequest, options);
                                        if (options.oneofs)
                                            object.payload = "friendRequest";
                                    }
                                    return object;
                                };

                                /**
                                 * Converts this NotificationInfo to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                NotificationInfo.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for NotificationInfo
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                NotificationInfo.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.NotificationInfo";
                                };

                                NotificationInfo.FriendRequest = (function() {

                                    /**
                                     * Properties of a FriendRequest.
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                     * @interface IFriendRequest
                                     * @property {number|null} [fromUserId] FriendRequest fromUserId
                                     * @property {number|null} [toUserId] FriendRequest toUserId
                                     * @property {string|null} [fromUserName] FriendRequest fromUserName
                                     * @property {string|null} [toUserName] FriendRequest toUserName
                                     * @property {string|null} [remark] FriendRequest remark
                                     * @property {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null} [type] FriendRequest type
                                     */

                                    /**
                                     * Constructs a new FriendRequest.
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo
                                     * @classdesc Represents a FriendRequest.
                                     * @implements IFriendRequest
                                     * @constructor
                                     * @param {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest=} [properties] Properties to set
                                     */
                                    function FriendRequest(properties) {
                                        if (properties)
                                            for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                                if (properties[keys[i]] != null)
                                                    this[keys[i]] = properties[keys[i]];
                                    }

                                    /**
                                     * FriendRequest fromUserId.
                                     * @member {number} fromUserId
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.fromUserId = 0;

                                    /**
                                     * FriendRequest toUserId.
                                     * @member {number} toUserId
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.toUserId = 0;

                                    /**
                                     * FriendRequest fromUserName.
                                     * @member {string} fromUserName
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.fromUserName = "";

                                    /**
                                     * FriendRequest toUserName.
                                     * @member {string} toUserName
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.toUserName = "";

                                    /**
                                     * FriendRequest remark.
                                     * @member {string} remark
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.remark = "";

                                    /**
                                     * FriendRequest type.
                                     * @member {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest|null|undefined} type
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     */
                                    FriendRequest.prototype.type = null;

                                    /**
                                     * Creates a new FriendRequest instance using the specified properties.
                                     * @function create
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest=} [properties] Properties to set
                                     * @returns {com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest} FriendRequest instance
                                     */
                                    FriendRequest.create = function create(properties) {
                                        return new FriendRequest(properties);
                                    };

                                    /**
                                     * Encodes the specified FriendRequest message. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify|verify} messages.
                                     * @function encode
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest} message FriendRequest message or plain object to encode
                                     * @param {$protobuf.Writer} [writer] Writer to encode to
                                     * @returns {$protobuf.Writer} Writer
                                     */
                                    FriendRequest.encode = function encode(message, writer) {
                                        if (!writer)
                                            writer = $Writer.create();
                                        if (message.fromUserId != null && Object.hasOwnProperty.call(message, "fromUserId"))
                                            writer.uint32(/* id 1, wireType 0 =*/8).int32(message.fromUserId);
                                        if (message.toUserId != null && Object.hasOwnProperty.call(message, "toUserId"))
                                            writer.uint32(/* id 2, wireType 0 =*/16).int32(message.toUserId);
                                        if (message.fromUserName != null && Object.hasOwnProperty.call(message, "fromUserName"))
                                            writer.uint32(/* id 3, wireType 2 =*/26).string(message.fromUserName);
                                        if (message.toUserName != null && Object.hasOwnProperty.call(message, "toUserName"))
                                            writer.uint32(/* id 4, wireType 2 =*/34).string(message.toUserName);
                                        if (message.remark != null && Object.hasOwnProperty.call(message, "remark"))
                                            writer.uint32(/* id 5, wireType 2 =*/42).string(message.remark);
                                        if (message.type != null && Object.hasOwnProperty.call(message, "type"))
                                            $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.encode(message.type, writer.uint32(/* id 6, wireType 2 =*/50).fork()).ldelim();
                                        return writer;
                                    };

                                    /**
                                     * Encodes the specified FriendRequest message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify|verify} messages.
                                     * @function encodeDelimited
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {com.github.im.common.connect.model.proto.NotificationInfo.IFriendRequest} message FriendRequest message or plain object to encode
                                     * @param {$protobuf.Writer} [writer] Writer to encode to
                                     * @returns {$protobuf.Writer} Writer
                                     */
                                    FriendRequest.encodeDelimited = function encodeDelimited(message, writer) {
                                        return this.encode(message, writer).ldelim();
                                    };

                                    /**
                                     * Decodes a FriendRequest message from the specified reader or buffer.
                                     * @function decode
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                     * @param {number} [length] Message length if known beforehand
                                     * @returns {com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest} FriendRequest
                                     * @throws {Error} If the payload is not a reader or valid buffer
                                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                     */
                                    FriendRequest.decode = function decode(reader, length, error) {
                                        if (!(reader instanceof $Reader))
                                            reader = $Reader.create(reader);
                                        var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest();
                                        while (reader.pos < end) {
                                            var tag = reader.uint32();
                                            if (tag === error)
                                                break;
                                            switch (tag >>> 3) {
                                            case 1: {
                                                    message.fromUserId = reader.int32();
                                                    break;
                                                }
                                            case 2: {
                                                    message.toUserId = reader.int32();
                                                    break;
                                                }
                                            case 3: {
                                                    message.fromUserName = reader.string();
                                                    break;
                                                }
                                            case 4: {
                                                    message.toUserName = reader.string();
                                                    break;
                                                }
                                            case 5: {
                                                    message.remark = reader.string();
                                                    break;
                                                }
                                            case 6: {
                                                    message.type = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.decode(reader, reader.uint32());
                                                    break;
                                                }
                                            default:
                                                reader.skipType(tag & 7);
                                                break;
                                            }
                                        }
                                        return message;
                                    };

                                    /**
                                     * Decodes a FriendRequest message from the specified reader or buffer, length delimited.
                                     * @function decodeDelimited
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                     * @returns {com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest} FriendRequest
                                     * @throws {Error} If the payload is not a reader or valid buffer
                                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                     */
                                    FriendRequest.decodeDelimited = function decodeDelimited(reader) {
                                        if (!(reader instanceof $Reader))
                                            reader = new $Reader(reader);
                                        return this.decode(reader, reader.uint32());
                                    };

                                    /**
                                     * Verifies a FriendRequest message.
                                     * @function verify
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {Object.<string,*>} message Plain object to verify
                                     * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                     */
                                    FriendRequest.verify = function verify(message) {
                                        if (typeof message !== "object" || message === null)
                                            return "object expected";
                                        if (message.fromUserId != null && message.hasOwnProperty("fromUserId"))
                                            if (!$util.isInteger(message.fromUserId))
                                                return "fromUserId: integer expected";
                                        if (message.toUserId != null && message.hasOwnProperty("toUserId"))
                                            if (!$util.isInteger(message.toUserId))
                                                return "toUserId: integer expected";
                                        if (message.fromUserName != null && message.hasOwnProperty("fromUserName"))
                                            if (!$util.isString(message.fromUserName))
                                                return "fromUserName: string expected";
                                        if (message.toUserName != null && message.hasOwnProperty("toUserName"))
                                            if (!$util.isString(message.toUserName))
                                                return "toUserName: string expected";
                                        if (message.remark != null && message.hasOwnProperty("remark"))
                                            if (!$util.isString(message.remark))
                                                return "remark: string expected";
                                        if (message.type != null && message.hasOwnProperty("type")) {
                                            var error = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.verify(message.type);
                                            if (error)
                                                return "type." + error;
                                        }
                                        return null;
                                    };

                                    /**
                                     * Creates a FriendRequest message from a plain object. Also converts values to their respective internal types.
                                     * @function fromObject
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {Object.<string,*>} object Plain object
                                     * @returns {com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest} FriendRequest
                                     */
                                    FriendRequest.fromObject = function fromObject(object) {
                                        if (object instanceof $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest)
                                            return object;
                                        var message = new $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest();
                                        if (object.fromUserId != null)
                                            message.fromUserId = object.fromUserId | 0;
                                        if (object.toUserId != null)
                                            message.toUserId = object.toUserId | 0;
                                        if (object.fromUserName != null)
                                            message.fromUserName = String(object.fromUserName);
                                        if (object.toUserName != null)
                                            message.toUserName = String(object.toUserName);
                                        if (object.remark != null)
                                            message.remark = String(object.remark);
                                        if (object.type != null) {
                                            if (typeof object.type !== "object")
                                                throw TypeError(".com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.type: object expected");
                                            message.type = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.fromObject(object.type);
                                        }
                                        return message;
                                    };

                                    /**
                                     * Creates a plain object from a FriendRequest message. Also converts values to other types if specified.
                                     * @function toObject
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest} message FriendRequest
                                     * @param {$protobuf.IConversionOptions} [options] Conversion options
                                     * @returns {Object.<string,*>} Plain object
                                     */
                                    FriendRequest.toObject = function toObject(message, options) {
                                        if (!options)
                                            options = {};
                                        var object = {};
                                        if (options.defaults) {
                                            object.fromUserId = 0;
                                            object.toUserId = 0;
                                            object.fromUserName = "";
                                            object.toUserName = "";
                                            object.remark = "";
                                            object.type = null;
                                        }
                                        if (message.fromUserId != null && message.hasOwnProperty("fromUserId"))
                                            object.fromUserId = message.fromUserId;
                                        if (message.toUserId != null && message.hasOwnProperty("toUserId"))
                                            object.toUserId = message.toUserId;
                                        if (message.fromUserName != null && message.hasOwnProperty("fromUserName"))
                                            object.fromUserName = message.fromUserName;
                                        if (message.toUserName != null && message.hasOwnProperty("toUserName"))
                                            object.toUserName = message.toUserName;
                                        if (message.remark != null && message.hasOwnProperty("remark"))
                                            object.remark = message.remark;
                                        if (message.type != null && message.hasOwnProperty("type"))
                                            object.type = $root.com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.toObject(message.type, options);
                                        return object;
                                    };

                                    /**
                                     * Converts this FriendRequest to JSON.
                                     * @function toJSON
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @instance
                                     * @returns {Object.<string,*>} JSON object
                                     */
                                    FriendRequest.prototype.toJSON = function toJSON() {
                                        return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                    };

                                    /**
                                     * Gets the default type url for FriendRequest
                                     * @function getTypeUrl
                                     * @memberof com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest
                                     * @static
                                     * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                     * @returns {string} The default type url
                                     */
                                    FriendRequest.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                        if (typeUrlPrefix === undefined) {
                                            typeUrlPrefix = "type.googleapis.com";
                                        }
                                        return typeUrlPrefix + "/com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest";
                                    };

                                    /**
                                     * FriendRequestType enum.
                                     * @name com.github.im.common.connect.model.proto.NotificationInfo.FriendRequest.FriendRequestType
                                     * @enum {number}
                                     * @property {number} ADD=0 ADD value
                                     * @property {number} DELETE=1 DELETE value
                                     * @property {number} ACCEPT=2 ACCEPT value
                                     * @property {number} REJECT=3 REJECT value
                                     */
                                    FriendRequest.FriendRequestType = (function() {
                                        var valuesById = {}, values = Object.create(valuesById);
                                        values[valuesById[0] = "ADD"] = 0;
                                        values[valuesById[1] = "DELETE"] = 1;
                                        values[valuesById[2] = "ACCEPT"] = 2;
                                        values[valuesById[3] = "REJECT"] = 3;
                                        return values;
                                    })();

                                    return FriendRequest;
                                })();

                                return NotificationInfo;
                            })();

                            proto.ChatMessage = (function() {

                                /**
                                 * Properties of a ChatMessage.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface IChatMessage
                                 * @property {number|Long|null} [msgId] ChatMessage msgId
                                 * @property {string|null} [clientMsgId] ChatMessage clientMsgId
                                 * @property {string|null} [content] ChatMessage content
                                 * @property {number|Long|null} [conversationId] ChatMessage conversationId
                                 * @property {string|null} [conversationName] ChatMessage conversationName
                                 * @property {com.github.im.common.connect.model.proto.IUserInfo|null} [fromUser] ChatMessage fromUser
                                 * @property {com.github.im.common.connect.model.proto.IUserInfo|null} [toUser] ChatMessage toUser
                                 * @property {number|Long|null} [clientTimeStamp] ChatMessage clientTimeStamp
                                 * @property {number|Long|null} [serverTimeStamp] ChatMessage serverTimeStamp
                                 * @property {com.github.im.common.connect.model.proto.MessageType|null} [type] ChatMessage type
                                 * @property {com.github.im.common.connect.model.proto.MessagesStatus|null} [messagesStatus] ChatMessage messagesStatus
                                 * @property {number|Long|null} [sequenceId] ChatMessage sequenceId
                                 */

                                /**
                                 * Constructs a new ChatMessage.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc Represents a ChatMessage.
                                 * @implements IChatMessage
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.IChatMessage=} [properties] Properties to set
                                 */
                                function ChatMessage(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * ChatMessage msgId.
                                 * @member {number|Long} msgId
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.msgId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * ChatMessage clientMsgId.
                                 * @member {string} clientMsgId
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.clientMsgId = "";

                                /**
                                 * ChatMessage content.
                                 * @member {string} content
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.content = "";

                                /**
                                 * ChatMessage conversationId.
                                 * @member {number|Long} conversationId
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.conversationId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * ChatMessage conversationName.
                                 * @member {string} conversationName
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.conversationName = "";

                                /**
                                 * ChatMessage fromUser.
                                 * @member {com.github.im.common.connect.model.proto.IUserInfo|null|undefined} fromUser
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.fromUser = null;

                                /**
                                 * ChatMessage toUser.
                                 * @member {com.github.im.common.connect.model.proto.IUserInfo|null|undefined} toUser
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.toUser = null;

                                /**
                                 * ChatMessage clientTimeStamp.
                                 * @member {number|Long} clientTimeStamp
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.clientTimeStamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * ChatMessage serverTimeStamp.
                                 * @member {number|Long} serverTimeStamp
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.serverTimeStamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * ChatMessage type.
                                 * @member {com.github.im.common.connect.model.proto.MessageType} type
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.type = 0;

                                /**
                                 * ChatMessage messagesStatus.
                                 * @member {com.github.im.common.connect.model.proto.MessagesStatus} messagesStatus
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.messagesStatus = 0;

                                /**
                                 * ChatMessage sequenceId.
                                 * @member {number|Long} sequenceId
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 */
                                ChatMessage.prototype.sequenceId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * Creates a new ChatMessage instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IChatMessage=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.ChatMessage} ChatMessage instance
                                 */
                                ChatMessage.create = function create(properties) {
                                    return new ChatMessage(properties);
                                };

                                /**
                                 * Encodes the specified ChatMessage message. Does not implicitly {@link com.github.im.common.connect.model.proto.ChatMessage.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IChatMessage} message ChatMessage message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                ChatMessage.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.msgId != null && Object.hasOwnProperty.call(message, "msgId"))
                                        writer.uint32(/* id 1, wireType 0 =*/8).int64(message.msgId);
                                    if (message.fromUser != null && Object.hasOwnProperty.call(message, "fromUser"))
                                        $root.com.github.im.common.connect.model.proto.UserInfo.encode(message.fromUser, writer.uint32(/* id 2, wireType 2 =*/18).fork()).ldelim();
                                    if (message.content != null && Object.hasOwnProperty.call(message, "content"))
                                        writer.uint32(/* id 3, wireType 2 =*/26).string(message.content);
                                    if (message.clientMsgId != null && Object.hasOwnProperty.call(message, "clientMsgId"))
                                        writer.uint32(/* id 4, wireType 2 =*/34).string(message.clientMsgId);
                                    if (message.sequenceId != null && Object.hasOwnProperty.call(message, "sequenceId"))
                                        writer.uint32(/* id 5, wireType 0 =*/40).int64(message.sequenceId);
                                    if (message.type != null && Object.hasOwnProperty.call(message, "type"))
                                        writer.uint32(/* id 6, wireType 0 =*/48).int32(message.type);
                                    if (message.clientTimeStamp != null && Object.hasOwnProperty.call(message, "clientTimeStamp"))
                                        writer.uint32(/* id 7, wireType 0 =*/56).int64(message.clientTimeStamp);
                                    if (message.toUser != null && Object.hasOwnProperty.call(message, "toUser"))
                                        $root.com.github.im.common.connect.model.proto.UserInfo.encode(message.toUser, writer.uint32(/* id 8, wireType 2 =*/66).fork()).ldelim();
                                    if (message.conversationId != null && Object.hasOwnProperty.call(message, "conversationId"))
                                        writer.uint32(/* id 9, wireType 0 =*/72).int64(message.conversationId);
                                    if (message.conversationName != null && Object.hasOwnProperty.call(message, "conversationName"))
                                        writer.uint32(/* id 10, wireType 2 =*/82).string(message.conversationName);
                                    if (message.messagesStatus != null && Object.hasOwnProperty.call(message, "messagesStatus"))
                                        writer.uint32(/* id 11, wireType 0 =*/88).int32(message.messagesStatus);
                                    if (message.serverTimeStamp != null && Object.hasOwnProperty.call(message, "serverTimeStamp"))
                                        writer.uint32(/* id 12, wireType 0 =*/96).int64(message.serverTimeStamp);
                                    return writer;
                                };

                                /**
                                 * Encodes the specified ChatMessage message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.ChatMessage.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IChatMessage} message ChatMessage message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                ChatMessage.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes a ChatMessage message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.ChatMessage} ChatMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                ChatMessage.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.ChatMessage();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.msgId = reader.int64();
                                                break;
                                            }
                                        case 4: {
                                                message.clientMsgId = reader.string();
                                                break;
                                            }
                                        case 3: {
                                                message.content = reader.string();
                                                break;
                                            }
                                        case 9: {
                                                message.conversationId = reader.int64();
                                                break;
                                            }
                                        case 10: {
                                                message.conversationName = reader.string();
                                                break;
                                            }
                                        case 2: {
                                                message.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 8: {
                                                message.toUser = $root.com.github.im.common.connect.model.proto.UserInfo.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 7: {
                                                message.clientTimeStamp = reader.int64();
                                                break;
                                            }
                                        case 12: {
                                                message.serverTimeStamp = reader.int64();
                                                break;
                                            }
                                        case 6: {
                                                message.type = reader.int32();
                                                break;
                                            }
                                        case 11: {
                                                message.messagesStatus = reader.int32();
                                                break;
                                            }
                                        case 5: {
                                                message.sequenceId = reader.int64();
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes a ChatMessage message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.ChatMessage} ChatMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                ChatMessage.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies a ChatMessage message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                ChatMessage.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    if (message.msgId != null && message.hasOwnProperty("msgId"))
                                        if (!$util.isInteger(message.msgId) && !(message.msgId && $util.isInteger(message.msgId.low) && $util.isInteger(message.msgId.high)))
                                            return "msgId: integer|Long expected";
                                    if (message.clientMsgId != null && message.hasOwnProperty("clientMsgId"))
                                        if (!$util.isString(message.clientMsgId))
                                            return "clientMsgId: string expected";
                                    if (message.content != null && message.hasOwnProperty("content"))
                                        if (!$util.isString(message.content))
                                            return "content: string expected";
                                    if (message.conversationId != null && message.hasOwnProperty("conversationId"))
                                        if (!$util.isInteger(message.conversationId) && !(message.conversationId && $util.isInteger(message.conversationId.low) && $util.isInteger(message.conversationId.high)))
                                            return "conversationId: integer|Long expected";
                                    if (message.conversationName != null && message.hasOwnProperty("conversationName"))
                                        if (!$util.isString(message.conversationName))
                                            return "conversationName: string expected";
                                    if (message.fromUser != null && message.hasOwnProperty("fromUser")) {
                                        var error = $root.com.github.im.common.connect.model.proto.UserInfo.verify(message.fromUser);
                                        if (error)
                                            return "fromUser." + error;
                                    }
                                    if (message.toUser != null && message.hasOwnProperty("toUser")) {
                                        var error = $root.com.github.im.common.connect.model.proto.UserInfo.verify(message.toUser);
                                        if (error)
                                            return "toUser." + error;
                                    }
                                    if (message.clientTimeStamp != null && message.hasOwnProperty("clientTimeStamp"))
                                        if (!$util.isInteger(message.clientTimeStamp) && !(message.clientTimeStamp && $util.isInteger(message.clientTimeStamp.low) && $util.isInteger(message.clientTimeStamp.high)))
                                            return "clientTimeStamp: integer|Long expected";
                                    if (message.serverTimeStamp != null && message.hasOwnProperty("serverTimeStamp"))
                                        if (!$util.isInteger(message.serverTimeStamp) && !(message.serverTimeStamp && $util.isInteger(message.serverTimeStamp.low) && $util.isInteger(message.serverTimeStamp.high)))
                                            return "serverTimeStamp: integer|Long expected";
                                    if (message.type != null && message.hasOwnProperty("type"))
                                        switch (message.type) {
                                        default:
                                            return "type: enum value expected";
                                        case 0:
                                        case 1:
                                        case 3:
                                        case 6:
                                        case 4:
                                        case 7:
                                            break;
                                        }
                                    if (message.messagesStatus != null && message.hasOwnProperty("messagesStatus"))
                                        switch (message.messagesStatus) {
                                        default:
                                            return "messagesStatus: enum value expected";
                                        case 0:
                                        case 1:
                                        case 2:
                                        case 3:
                                        case 4:
                                        case 5:
                                        case 6:
                                        case 7:
                                            break;
                                        }
                                    if (message.sequenceId != null && message.hasOwnProperty("sequenceId"))
                                        if (!$util.isInteger(message.sequenceId) && !(message.sequenceId && $util.isInteger(message.sequenceId.low) && $util.isInteger(message.sequenceId.high)))
                                            return "sequenceId: integer|Long expected";
                                    return null;
                                };

                                /**
                                 * Creates a ChatMessage message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.ChatMessage} ChatMessage
                                 */
                                ChatMessage.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.ChatMessage)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.ChatMessage();
                                    if (object.msgId != null)
                                        if ($util.Long)
                                            (message.msgId = $util.Long.fromValue(object.msgId)).unsigned = false;
                                        else if (typeof object.msgId === "string")
                                            message.msgId = parseInt(object.msgId, 10);
                                        else if (typeof object.msgId === "number")
                                            message.msgId = object.msgId;
                                        else if (typeof object.msgId === "object")
                                            message.msgId = new $util.LongBits(object.msgId.low >>> 0, object.msgId.high >>> 0).toNumber();
                                    if (object.clientMsgId != null)
                                        message.clientMsgId = String(object.clientMsgId);
                                    if (object.content != null)
                                        message.content = String(object.content);
                                    if (object.conversationId != null)
                                        if ($util.Long)
                                            (message.conversationId = $util.Long.fromValue(object.conversationId)).unsigned = false;
                                        else if (typeof object.conversationId === "string")
                                            message.conversationId = parseInt(object.conversationId, 10);
                                        else if (typeof object.conversationId === "number")
                                            message.conversationId = object.conversationId;
                                        else if (typeof object.conversationId === "object")
                                            message.conversationId = new $util.LongBits(object.conversationId.low >>> 0, object.conversationId.high >>> 0).toNumber();
                                    if (object.conversationName != null)
                                        message.conversationName = String(object.conversationName);
                                    if (object.fromUser != null) {
                                        if (typeof object.fromUser !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.ChatMessage.fromUser: object expected");
                                        message.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.fromObject(object.fromUser);
                                    }
                                    if (object.toUser != null) {
                                        if (typeof object.toUser !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.ChatMessage.toUser: object expected");
                                        message.toUser = $root.com.github.im.common.connect.model.proto.UserInfo.fromObject(object.toUser);
                                    }
                                    if (object.clientTimeStamp != null)
                                        if ($util.Long)
                                            (message.clientTimeStamp = $util.Long.fromValue(object.clientTimeStamp)).unsigned = false;
                                        else if (typeof object.clientTimeStamp === "string")
                                            message.clientTimeStamp = parseInt(object.clientTimeStamp, 10);
                                        else if (typeof object.clientTimeStamp === "number")
                                            message.clientTimeStamp = object.clientTimeStamp;
                                        else if (typeof object.clientTimeStamp === "object")
                                            message.clientTimeStamp = new $util.LongBits(object.clientTimeStamp.low >>> 0, object.clientTimeStamp.high >>> 0).toNumber();
                                    if (object.serverTimeStamp != null)
                                        if ($util.Long)
                                            (message.serverTimeStamp = $util.Long.fromValue(object.serverTimeStamp)).unsigned = false;
                                        else if (typeof object.serverTimeStamp === "string")
                                            message.serverTimeStamp = parseInt(object.serverTimeStamp, 10);
                                        else if (typeof object.serverTimeStamp === "number")
                                            message.serverTimeStamp = object.serverTimeStamp;
                                        else if (typeof object.serverTimeStamp === "object")
                                            message.serverTimeStamp = new $util.LongBits(object.serverTimeStamp.low >>> 0, object.serverTimeStamp.high >>> 0).toNumber();
                                    switch (object.type) {
                                    default:
                                        if (typeof object.type === "number") {
                                            message.type = object.type;
                                            break;
                                        }
                                        break;
                                    case "TEXT":
                                    case 0:
                                        message.type = 0;
                                        break;
                                    case "FILE":
                                    case 1:
                                        message.type = 1;
                                        break;
                                    case "VIDEO":
                                    case 3:
                                        message.type = 3;
                                        break;
                                    case "IMAGE":
                                    case 6:
                                        message.type = 6;
                                        break;
                                    case "VOICE":
                                    case 4:
                                        message.type = 4;
                                        break;
                                    case "MEETING":
                                    case 7:
                                        message.type = 7;
                                        break;
                                    }
                                    switch (object.messagesStatus) {
                                    default:
                                        if (typeof object.messagesStatus === "number") {
                                            message.messagesStatus = object.messagesStatus;
                                            break;
                                        }
                                        break;
                                    case "SENDING":
                                    case 0:
                                        message.messagesStatus = 0;
                                        break;
                                    case "SENT":
                                    case 1:
                                        message.messagesStatus = 1;
                                        break;
                                    case "FAILED":
                                    case 2:
                                        message.messagesStatus = 2;
                                        break;
                                    case "RECEIVED":
                                    case 3:
                                        message.messagesStatus = 3;
                                        break;
                                    case "READ":
                                    case 4:
                                        message.messagesStatus = 4;
                                        break;
                                    case "UNREAD":
                                    case 5:
                                        message.messagesStatus = 5;
                                        break;
                                    case "DELETED":
                                    case 6:
                                        message.messagesStatus = 6;
                                        break;
                                    case "REVOKE":
                                    case 7:
                                        message.messagesStatus = 7;
                                        break;
                                    }
                                    if (object.sequenceId != null)
                                        if ($util.Long)
                                            (message.sequenceId = $util.Long.fromValue(object.sequenceId)).unsigned = false;
                                        else if (typeof object.sequenceId === "string")
                                            message.sequenceId = parseInt(object.sequenceId, 10);
                                        else if (typeof object.sequenceId === "number")
                                            message.sequenceId = object.sequenceId;
                                        else if (typeof object.sequenceId === "object")
                                            message.sequenceId = new $util.LongBits(object.sequenceId.low >>> 0, object.sequenceId.high >>> 0).toNumber();
                                    return message;
                                };

                                /**
                                 * Creates a plain object from a ChatMessage message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.ChatMessage} message ChatMessage
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                ChatMessage.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (options.defaults) {
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.msgId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.msgId = options.longs === String ? "0" : 0;
                                        object.fromUser = null;
                                        object.content = "";
                                        object.clientMsgId = "";
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.sequenceId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.sequenceId = options.longs === String ? "0" : 0;
                                        object.type = options.enums === String ? "TEXT" : 0;
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.clientTimeStamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.clientTimeStamp = options.longs === String ? "0" : 0;
                                        object.toUser = null;
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.conversationId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.conversationId = options.longs === String ? "0" : 0;
                                        object.conversationName = "";
                                        object.messagesStatus = options.enums === String ? "SENDING" : 0;
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.serverTimeStamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.serverTimeStamp = options.longs === String ? "0" : 0;
                                    }
                                    if (message.msgId != null && message.hasOwnProperty("msgId"))
                                        if (typeof message.msgId === "number")
                                            object.msgId = options.longs === String ? String(message.msgId) : message.msgId;
                                        else
                                            object.msgId = options.longs === String ? $util.Long.prototype.toString.call(message.msgId) : options.longs === Number ? new $util.LongBits(message.msgId.low >>> 0, message.msgId.high >>> 0).toNumber() : message.msgId;
                                    if (message.fromUser != null && message.hasOwnProperty("fromUser"))
                                        object.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.toObject(message.fromUser, options);
                                    if (message.content != null && message.hasOwnProperty("content"))
                                        object.content = message.content;
                                    if (message.clientMsgId != null && message.hasOwnProperty("clientMsgId"))
                                        object.clientMsgId = message.clientMsgId;
                                    if (message.sequenceId != null && message.hasOwnProperty("sequenceId"))
                                        if (typeof message.sequenceId === "number")
                                            object.sequenceId = options.longs === String ? String(message.sequenceId) : message.sequenceId;
                                        else
                                            object.sequenceId = options.longs === String ? $util.Long.prototype.toString.call(message.sequenceId) : options.longs === Number ? new $util.LongBits(message.sequenceId.low >>> 0, message.sequenceId.high >>> 0).toNumber() : message.sequenceId;
                                    if (message.type != null && message.hasOwnProperty("type"))
                                        object.type = options.enums === String ? $root.com.github.im.common.connect.model.proto.MessageType[message.type] === undefined ? message.type : $root.com.github.im.common.connect.model.proto.MessageType[message.type] : message.type;
                                    if (message.clientTimeStamp != null && message.hasOwnProperty("clientTimeStamp"))
                                        if (typeof message.clientTimeStamp === "number")
                                            object.clientTimeStamp = options.longs === String ? String(message.clientTimeStamp) : message.clientTimeStamp;
                                        else
                                            object.clientTimeStamp = options.longs === String ? $util.Long.prototype.toString.call(message.clientTimeStamp) : options.longs === Number ? new $util.LongBits(message.clientTimeStamp.low >>> 0, message.clientTimeStamp.high >>> 0).toNumber() : message.clientTimeStamp;
                                    if (message.toUser != null && message.hasOwnProperty("toUser"))
                                        object.toUser = $root.com.github.im.common.connect.model.proto.UserInfo.toObject(message.toUser, options);
                                    if (message.conversationId != null && message.hasOwnProperty("conversationId"))
                                        if (typeof message.conversationId === "number")
                                            object.conversationId = options.longs === String ? String(message.conversationId) : message.conversationId;
                                        else
                                            object.conversationId = options.longs === String ? $util.Long.prototype.toString.call(message.conversationId) : options.longs === Number ? new $util.LongBits(message.conversationId.low >>> 0, message.conversationId.high >>> 0).toNumber() : message.conversationId;
                                    if (message.conversationName != null && message.hasOwnProperty("conversationName"))
                                        object.conversationName = message.conversationName;
                                    if (message.messagesStatus != null && message.hasOwnProperty("messagesStatus"))
                                        object.messagesStatus = options.enums === String ? $root.com.github.im.common.connect.model.proto.MessagesStatus[message.messagesStatus] === undefined ? message.messagesStatus : $root.com.github.im.common.connect.model.proto.MessagesStatus[message.messagesStatus] : message.messagesStatus;
                                    if (message.serverTimeStamp != null && message.hasOwnProperty("serverTimeStamp"))
                                        if (typeof message.serverTimeStamp === "number")
                                            object.serverTimeStamp = options.longs === String ? String(message.serverTimeStamp) : message.serverTimeStamp;
                                        else
                                            object.serverTimeStamp = options.longs === String ? $util.Long.prototype.toString.call(message.serverTimeStamp) : options.longs === Number ? new $util.LongBits(message.serverTimeStamp.low >>> 0, message.serverTimeStamp.high >>> 0).toNumber() : message.serverTimeStamp;
                                    return object;
                                };

                                /**
                                 * Converts this ChatMessage to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                ChatMessage.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for ChatMessage
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.ChatMessage
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                ChatMessage.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.ChatMessage";
                                };

                                return ChatMessage;
                            })();

                            /**
                             * 消息状态
                             * @name com.github.im.common.connect.model.proto.MessagesStatus
                             * @enum {number}
                             * @property {number} SENDING=0 SENDING value
                             * @property {number} SENT=1 SENT value
                             * @property {number} FAILED=2 FAILED value
                             * @property {number} RECEIVED=3 RECEIVED value
                             * @property {number} READ=4 READ value
                             * @property {number} UNREAD=5 UNREAD value
                             * @property {number} DELETED=6 DELETED value
                             * @property {number} REVOKE=7 REVOKE value
                             */
                            proto.MessagesStatus = (function() {
                                var valuesById = {}, values = Object.create(valuesById);
                                values[valuesById[0] = "SENDING"] = 0;
                                values[valuesById[1] = "SENT"] = 1;
                                values[valuesById[2] = "FAILED"] = 2;
                                values[valuesById[3] = "RECEIVED"] = 3;
                                values[valuesById[4] = "READ"] = 4;
                                values[valuesById[5] = "UNREAD"] = 5;
                                values[valuesById[6] = "DELETED"] = 6;
                                values[valuesById[7] = "REVOKE"] = 7;
                                return values;
                            })();

                            proto.AckMessage = (function() {

                                /**
                                 * Properties of an AckMessage.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @interface IAckMessage
                                 * @property {string|null} [clientMsgId] AckMessage clientMsgId
                                 * @property {number|Long|null} [serverMsgId] AckMessage serverMsgId
                                 * @property {number|Long|null} [conversationId] AckMessage conversationId
                                 * @property {com.github.im.common.connect.model.proto.IUserInfo|null} [fromUser] AckMessage fromUser
                                 * @property {number|Long|null} [ackTimestamp] AckMessage ackTimestamp
                                 * @property {com.github.im.common.connect.model.proto.MessagesStatus|null} [status] AckMessage status
                                 */

                                /**
                                 * Constructs a new AckMessage.
                                 * @memberof com.github.im.common.connect.model.proto
                                 * @classdesc ACK 应答消息
                                 * @implements IAckMessage
                                 * @constructor
                                 * @param {com.github.im.common.connect.model.proto.IAckMessage=} [properties] Properties to set
                                 */
                                function AckMessage(properties) {
                                    if (properties)
                                        for (var keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                            if (properties[keys[i]] != null)
                                                this[keys[i]] = properties[keys[i]];
                                }

                                /**
                                 * AckMessage clientMsgId.
                                 * @member {string} clientMsgId
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.clientMsgId = "";

                                /**
                                 * AckMessage serverMsgId.
                                 * @member {number|Long} serverMsgId
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.serverMsgId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * AckMessage conversationId.
                                 * @member {number|Long} conversationId
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.conversationId = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * AckMessage fromUser.
                                 * @member {com.github.im.common.connect.model.proto.IUserInfo|null|undefined} fromUser
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.fromUser = null;

                                /**
                                 * AckMessage ackTimestamp.
                                 * @member {number|Long} ackTimestamp
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.ackTimestamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                                /**
                                 * AckMessage status.
                                 * @member {com.github.im.common.connect.model.proto.MessagesStatus} status
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 */
                                AckMessage.prototype.status = 0;

                                /**
                                 * Creates a new AckMessage instance using the specified properties.
                                 * @function create
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IAckMessage=} [properties] Properties to set
                                 * @returns {com.github.im.common.connect.model.proto.AckMessage} AckMessage instance
                                 */
                                AckMessage.create = function create(properties) {
                                    return new AckMessage(properties);
                                };

                                /**
                                 * Encodes the specified AckMessage message. Does not implicitly {@link com.github.im.common.connect.model.proto.AckMessage.verify|verify} messages.
                                 * @function encode
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IAckMessage} message AckMessage message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                AckMessage.encode = function encode(message, writer) {
                                    if (!writer)
                                        writer = $Writer.create();
                                    if (message.clientMsgId != null && Object.hasOwnProperty.call(message, "clientMsgId"))
                                        writer.uint32(/* id 1, wireType 2 =*/10).string(message.clientMsgId);
                                    if (message.serverMsgId != null && Object.hasOwnProperty.call(message, "serverMsgId"))
                                        writer.uint32(/* id 2, wireType 0 =*/16).int64(message.serverMsgId);
                                    if (message.conversationId != null && Object.hasOwnProperty.call(message, "conversationId"))
                                        writer.uint32(/* id 3, wireType 0 =*/24).int64(message.conversationId);
                                    if (message.fromUser != null && Object.hasOwnProperty.call(message, "fromUser"))
                                        $root.com.github.im.common.connect.model.proto.UserInfo.encode(message.fromUser, writer.uint32(/* id 4, wireType 2 =*/34).fork()).ldelim();
                                    if (message.ackTimestamp != null && Object.hasOwnProperty.call(message, "ackTimestamp"))
                                        writer.uint32(/* id 5, wireType 0 =*/40).int64(message.ackTimestamp);
                                    if (message.status != null && Object.hasOwnProperty.call(message, "status"))
                                        writer.uint32(/* id 7, wireType 0 =*/56).int32(message.status);
                                    return writer;
                                };

                                /**
                                 * Encodes the specified AckMessage message, length delimited. Does not implicitly {@link com.github.im.common.connect.model.proto.AckMessage.verify|verify} messages.
                                 * @function encodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.IAckMessage} message AckMessage message or plain object to encode
                                 * @param {$protobuf.Writer} [writer] Writer to encode to
                                 * @returns {$protobuf.Writer} Writer
                                 */
                                AckMessage.encodeDelimited = function encodeDelimited(message, writer) {
                                    return this.encode(message, writer).ldelim();
                                };

                                /**
                                 * Decodes an AckMessage message from the specified reader or buffer.
                                 * @function decode
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @param {number} [length] Message length if known beforehand
                                 * @returns {com.github.im.common.connect.model.proto.AckMessage} AckMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                AckMessage.decode = function decode(reader, length, error) {
                                    if (!(reader instanceof $Reader))
                                        reader = $Reader.create(reader);
                                    var end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.github.im.common.connect.model.proto.AckMessage();
                                    while (reader.pos < end) {
                                        var tag = reader.uint32();
                                        if (tag === error)
                                            break;
                                        switch (tag >>> 3) {
                                        case 1: {
                                                message.clientMsgId = reader.string();
                                                break;
                                            }
                                        case 2: {
                                                message.serverMsgId = reader.int64();
                                                break;
                                            }
                                        case 3: {
                                                message.conversationId = reader.int64();
                                                break;
                                            }
                                        case 4: {
                                                message.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.decode(reader, reader.uint32());
                                                break;
                                            }
                                        case 5: {
                                                message.ackTimestamp = reader.int64();
                                                break;
                                            }
                                        case 7: {
                                                message.status = reader.int32();
                                                break;
                                            }
                                        default:
                                            reader.skipType(tag & 7);
                                            break;
                                        }
                                    }
                                    return message;
                                };

                                /**
                                 * Decodes an AckMessage message from the specified reader or buffer, length delimited.
                                 * @function decodeDelimited
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                                 * @returns {com.github.im.common.connect.model.proto.AckMessage} AckMessage
                                 * @throws {Error} If the payload is not a reader or valid buffer
                                 * @throws {$protobuf.util.ProtocolError} If required fields are missing
                                 */
                                AckMessage.decodeDelimited = function decodeDelimited(reader) {
                                    if (!(reader instanceof $Reader))
                                        reader = new $Reader(reader);
                                    return this.decode(reader, reader.uint32());
                                };

                                /**
                                 * Verifies an AckMessage message.
                                 * @function verify
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {Object.<string,*>} message Plain object to verify
                                 * @returns {string|null} `null` if valid, otherwise the reason why it is not
                                 */
                                AckMessage.verify = function verify(message) {
                                    if (typeof message !== "object" || message === null)
                                        return "object expected";
                                    if (message.clientMsgId != null && message.hasOwnProperty("clientMsgId"))
                                        if (!$util.isString(message.clientMsgId))
                                            return "clientMsgId: string expected";
                                    if (message.serverMsgId != null && message.hasOwnProperty("serverMsgId"))
                                        if (!$util.isInteger(message.serverMsgId) && !(message.serverMsgId && $util.isInteger(message.serverMsgId.low) && $util.isInteger(message.serverMsgId.high)))
                                            return "serverMsgId: integer|Long expected";
                                    if (message.conversationId != null && message.hasOwnProperty("conversationId"))
                                        if (!$util.isInteger(message.conversationId) && !(message.conversationId && $util.isInteger(message.conversationId.low) && $util.isInteger(message.conversationId.high)))
                                            return "conversationId: integer|Long expected";
                                    if (message.fromUser != null && message.hasOwnProperty("fromUser")) {
                                        var error = $root.com.github.im.common.connect.model.proto.UserInfo.verify(message.fromUser);
                                        if (error)
                                            return "fromUser." + error;
                                    }
                                    if (message.ackTimestamp != null && message.hasOwnProperty("ackTimestamp"))
                                        if (!$util.isInteger(message.ackTimestamp) && !(message.ackTimestamp && $util.isInteger(message.ackTimestamp.low) && $util.isInteger(message.ackTimestamp.high)))
                                            return "ackTimestamp: integer|Long expected";
                                    if (message.status != null && message.hasOwnProperty("status"))
                                        switch (message.status) {
                                        default:
                                            return "status: enum value expected";
                                        case 0:
                                        case 1:
                                        case 2:
                                        case 3:
                                        case 4:
                                        case 5:
                                        case 6:
                                        case 7:
                                            break;
                                        }
                                    return null;
                                };

                                /**
                                 * Creates an AckMessage message from a plain object. Also converts values to their respective internal types.
                                 * @function fromObject
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {Object.<string,*>} object Plain object
                                 * @returns {com.github.im.common.connect.model.proto.AckMessage} AckMessage
                                 */
                                AckMessage.fromObject = function fromObject(object) {
                                    if (object instanceof $root.com.github.im.common.connect.model.proto.AckMessage)
                                        return object;
                                    var message = new $root.com.github.im.common.connect.model.proto.AckMessage();
                                    if (object.clientMsgId != null)
                                        message.clientMsgId = String(object.clientMsgId);
                                    if (object.serverMsgId != null)
                                        if ($util.Long)
                                            (message.serverMsgId = $util.Long.fromValue(object.serverMsgId)).unsigned = false;
                                        else if (typeof object.serverMsgId === "string")
                                            message.serverMsgId = parseInt(object.serverMsgId, 10);
                                        else if (typeof object.serverMsgId === "number")
                                            message.serverMsgId = object.serverMsgId;
                                        else if (typeof object.serverMsgId === "object")
                                            message.serverMsgId = new $util.LongBits(object.serverMsgId.low >>> 0, object.serverMsgId.high >>> 0).toNumber();
                                    if (object.conversationId != null)
                                        if ($util.Long)
                                            (message.conversationId = $util.Long.fromValue(object.conversationId)).unsigned = false;
                                        else if (typeof object.conversationId === "string")
                                            message.conversationId = parseInt(object.conversationId, 10);
                                        else if (typeof object.conversationId === "number")
                                            message.conversationId = object.conversationId;
                                        else if (typeof object.conversationId === "object")
                                            message.conversationId = new $util.LongBits(object.conversationId.low >>> 0, object.conversationId.high >>> 0).toNumber();
                                    if (object.fromUser != null) {
                                        if (typeof object.fromUser !== "object")
                                            throw TypeError(".com.github.im.common.connect.model.proto.AckMessage.fromUser: object expected");
                                        message.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.fromObject(object.fromUser);
                                    }
                                    if (object.ackTimestamp != null)
                                        if ($util.Long)
                                            (message.ackTimestamp = $util.Long.fromValue(object.ackTimestamp)).unsigned = false;
                                        else if (typeof object.ackTimestamp === "string")
                                            message.ackTimestamp = parseInt(object.ackTimestamp, 10);
                                        else if (typeof object.ackTimestamp === "number")
                                            message.ackTimestamp = object.ackTimestamp;
                                        else if (typeof object.ackTimestamp === "object")
                                            message.ackTimestamp = new $util.LongBits(object.ackTimestamp.low >>> 0, object.ackTimestamp.high >>> 0).toNumber();
                                    switch (object.status) {
                                    default:
                                        if (typeof object.status === "number") {
                                            message.status = object.status;
                                            break;
                                        }
                                        break;
                                    case "SENDING":
                                    case 0:
                                        message.status = 0;
                                        break;
                                    case "SENT":
                                    case 1:
                                        message.status = 1;
                                        break;
                                    case "FAILED":
                                    case 2:
                                        message.status = 2;
                                        break;
                                    case "RECEIVED":
                                    case 3:
                                        message.status = 3;
                                        break;
                                    case "READ":
                                    case 4:
                                        message.status = 4;
                                        break;
                                    case "UNREAD":
                                    case 5:
                                        message.status = 5;
                                        break;
                                    case "DELETED":
                                    case 6:
                                        message.status = 6;
                                        break;
                                    case "REVOKE":
                                    case 7:
                                        message.status = 7;
                                        break;
                                    }
                                    return message;
                                };

                                /**
                                 * Creates a plain object from an AckMessage message. Also converts values to other types if specified.
                                 * @function toObject
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {com.github.im.common.connect.model.proto.AckMessage} message AckMessage
                                 * @param {$protobuf.IConversionOptions} [options] Conversion options
                                 * @returns {Object.<string,*>} Plain object
                                 */
                                AckMessage.toObject = function toObject(message, options) {
                                    if (!options)
                                        options = {};
                                    var object = {};
                                    if (options.defaults) {
                                        object.clientMsgId = "";
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.serverMsgId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.serverMsgId = options.longs === String ? "0" : 0;
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.conversationId = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.conversationId = options.longs === String ? "0" : 0;
                                        object.fromUser = null;
                                        if ($util.Long) {
                                            var long = new $util.Long(0, 0, false);
                                            object.ackTimestamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                                        } else
                                            object.ackTimestamp = options.longs === String ? "0" : 0;
                                        object.status = options.enums === String ? "SENDING" : 0;
                                    }
                                    if (message.clientMsgId != null && message.hasOwnProperty("clientMsgId"))
                                        object.clientMsgId = message.clientMsgId;
                                    if (message.serverMsgId != null && message.hasOwnProperty("serverMsgId"))
                                        if (typeof message.serverMsgId === "number")
                                            object.serverMsgId = options.longs === String ? String(message.serverMsgId) : message.serverMsgId;
                                        else
                                            object.serverMsgId = options.longs === String ? $util.Long.prototype.toString.call(message.serverMsgId) : options.longs === Number ? new $util.LongBits(message.serverMsgId.low >>> 0, message.serverMsgId.high >>> 0).toNumber() : message.serverMsgId;
                                    if (message.conversationId != null && message.hasOwnProperty("conversationId"))
                                        if (typeof message.conversationId === "number")
                                            object.conversationId = options.longs === String ? String(message.conversationId) : message.conversationId;
                                        else
                                            object.conversationId = options.longs === String ? $util.Long.prototype.toString.call(message.conversationId) : options.longs === Number ? new $util.LongBits(message.conversationId.low >>> 0, message.conversationId.high >>> 0).toNumber() : message.conversationId;
                                    if (message.fromUser != null && message.hasOwnProperty("fromUser"))
                                        object.fromUser = $root.com.github.im.common.connect.model.proto.UserInfo.toObject(message.fromUser, options);
                                    if (message.ackTimestamp != null && message.hasOwnProperty("ackTimestamp"))
                                        if (typeof message.ackTimestamp === "number")
                                            object.ackTimestamp = options.longs === String ? String(message.ackTimestamp) : message.ackTimestamp;
                                        else
                                            object.ackTimestamp = options.longs === String ? $util.Long.prototype.toString.call(message.ackTimestamp) : options.longs === Number ? new $util.LongBits(message.ackTimestamp.low >>> 0, message.ackTimestamp.high >>> 0).toNumber() : message.ackTimestamp;
                                    if (message.status != null && message.hasOwnProperty("status"))
                                        object.status = options.enums === String ? $root.com.github.im.common.connect.model.proto.MessagesStatus[message.status] === undefined ? message.status : $root.com.github.im.common.connect.model.proto.MessagesStatus[message.status] : message.status;
                                    return object;
                                };

                                /**
                                 * Converts this AckMessage to JSON.
                                 * @function toJSON
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @instance
                                 * @returns {Object.<string,*>} JSON object
                                 */
                                AckMessage.prototype.toJSON = function toJSON() {
                                    return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                                };

                                /**
                                 * Gets the default type url for AckMessage
                                 * @function getTypeUrl
                                 * @memberof com.github.im.common.connect.model.proto.AckMessage
                                 * @static
                                 * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                                 * @returns {string} The default type url
                                 */
                                AckMessage.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                                    if (typeUrlPrefix === undefined) {
                                        typeUrlPrefix = "type.googleapis.com";
                                    }
                                    return typeUrlPrefix + "/com.github.im.common.connect.model.proto.AckMessage";
                                };

                                return AckMessage;
                            })();

                            /**
                             * MessageType enum.
                             * @name com.github.im.common.connect.model.proto.MessageType
                             * @enum {number}
                             * @property {number} TEXT=0 TEXT value
                             * @property {number} FILE=1 FILE value
                             * @property {number} VIDEO=3 VIDEO value
                             * @property {number} IMAGE=6 IMAGE value
                             * @property {number} VOICE=4 VOICE value
                             * @property {number} MEETING=7 MEETING value
                             */
                            proto.MessageType = (function() {
                                var valuesById = {}, values = Object.create(valuesById);
                                values[valuesById[0] = "TEXT"] = 0;
                                values[valuesById[1] = "FILE"] = 1;
                                values[valuesById[3] = "VIDEO"] = 3;
                                values[valuesById[6] = "IMAGE"] = 6;
                                values[valuesById[4] = "VOICE"] = 4;
                                values[valuesById[7] = "MEETING"] = 7;
                                return values;
                            })();

                            return proto;
                        })();

                        return model;
                    })();

                    return connect;
                })();

                return common;
            })();

            return im;
        })();

        return github;
    })();

    return com;
})();

module.exports = $root;
