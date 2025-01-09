
package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.model.proto.BaseMessage;
import org.springframework.lang.Nullable;
import reactor.netty.Connection;

/***
 * connect  message process handler
 * {@link BaseMessage.BaseMessagePkg.PayloadCase }
 *
 */
@FunctionalInterface
public interface ProtoBufProcessHandler {

    /***
     * get corresponding message type ,
     * @return 返回 业务对应的类型
     */
    public default BaseMessage.BaseMessagePkg.PayloadCase type(){
        return BaseMessage.BaseMessagePkg.PayloadCase.PAYLOAD_NOT_SET;
    }


    /***
     * process client network IO
     * @param con connection within client
     * @param message  IO byte data
     * @throws IllegalArgumentException  when the connection  is invalid  , program will throw exception
     */
    public void process(@Nullable Connection con , BaseMessage.BaseMessagePkg message) throws IllegalArgumentException;

}
