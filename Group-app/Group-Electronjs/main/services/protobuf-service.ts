import { com } from '../generated/proto-bundle';
import * as $protobuf from 'protobufjs/minimal';
import Long from 'long';

// 确保 protobufjs 在全局范围内正确配置 Long
$protobuf.util.Long = Long;
$protobuf.configure();

// 使用命名空间别名
import proto = com.github.im.common.connect.model.proto;

// 定义导出类型
export type IBaseMessagePkg = proto.IBaseMessagePkg;
export { proto };

class ProtobufService {
    private initialized = false;

    async initialize() {
        if (this.initialized) return;

        // 显式再次确保 Long 被正确关联
        $protobuf.util.Long = Long;
        $protobuf.configure();

        this.initialized = true;
        console.log('Protobuf service (static-bundle/minimal) initialized with Long support');
    }

    /**
     * 编码 BaseMessagePkg
     * @param payload 符合接口定义的 POJO 或 Message 实例
     */
    encode(payload: proto.IBaseMessagePkg): Buffer {
        // 开发调试：打印 payload 详情，特别是 userId 类型
        if (payload.userInfo) {
            const up = payload.userInfo;
            const t = typeof up.userId;
            const isLong = up.userId && (up.userId as any).low !== undefined;
            console.log(`[ProtobufService] Encoding UserInfo: userId=${up.userId}, type=${t}, isLong=${isLong}`);
        }

        // 1. 将字面量对象转换为类实例 (处理 string/number -> Long)
        const message = proto.BaseMessagePkg.fromObject(payload);

        // 2. 直接序列化
        return Buffer.from(proto.BaseMessagePkg.encode(message).finish());
    }

    /**
     * 解码 BaseMessagePkg
     */
    decode(buffer: Buffer): proto.BaseMessagePkg {
        // 使用静态类的解码能力，返回的是一个完整的 Message 实例
        return proto.BaseMessagePkg.decode(buffer);
    }

    /**
     * 将 Message 实例转换为普通的 POJO 对象（适合发送给前端渲染）
     */
    toObject(message: proto.BaseMessagePkg) {
        return proto.BaseMessagePkg.toObject(message, {
            enums: String,
            longs: String,
            bytes: String,
            defaults: true,
            oneofs: true
        });
    }

    /**
     * 创建心跳包
     */
    createHeartbeat(ping: boolean = true): Buffer {
        return this.encode({
            heartbeat: { ping }
        });
    }

    /**
     * 获取原型引用，如果需要做更复杂的操作
     */
    getProto() {
        return proto;
    }
}

export const protobufService = new ProtobufService();
