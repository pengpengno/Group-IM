
package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.enums.ProtocolMessageMapEnum;
import com.google.protobuf.Message;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.netty.Connection;

/***
 * connect  message process
 */
@FunctionalInterface
public interface ProtoBufProcess{

    /***
     * 获取类型
     * @return 返回 业务对应的类型
     */
    public default ProtocolMessageMapEnum type(){
        return null;
    }

    public default void decode(@Nullable Connection con , Message message){

        Assert.notNull(con,"connection is null");

        con.addHandlerLast(ProtobufDecoder.class.getName(),new ProtobufDecoder(message));

    }

    /***
     * process client network IO
     * @param con connection within client
     * @param message  IO byte data
     * @throws IllegalArgumentException  when the connection  is invalid  , program will throw exception
     */
    public void process(@Nullable Connection con , Message message) throws IllegalArgumentException;

}
