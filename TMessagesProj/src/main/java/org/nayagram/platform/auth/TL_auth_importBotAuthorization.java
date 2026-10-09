package org.nayagram.platform.auth;

import org.telegram.tgnet.InputSerializedData;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

/**
 * auth.importBotAuthorization#67a3ff2c flags:# api_id:int api_hash:string bot_auth_token:string = auth.Authorization;
 * Telegram MTProto method allowing direct bot account authorization via bot token.
 */
public class TL_auth_importBotAuthorization extends TLObject {
    public static final int constructor = 0x67a3ff2c;

    public int flags;
    public int api_id;
    public String api_hash;
    public String bot_auth_token;

    @Override
    public TLObject deserializeResponse(InputSerializedData stream, int constructor, boolean exception) {
        if (constructor == 0xcd050916 || constructor == 0x2ea2c0d4 || constructor == 0x33fb7bb8) {
            TLRPC.TL_auth_authorization auth = new TLRPC.TL_auth_authorization();
            auth.readParams(stream, exception);
            return auth;
        }
        return TLRPC.auth_Authorization.TLdeserialize(stream, constructor, exception);
    }

    @Override
    public void serializeToStream(OutputSerializedData stream) {
        stream.writeInt32(constructor);
        stream.writeInt32(flags);
        stream.writeInt32(api_id);
        stream.writeString(api_hash);
        stream.writeString(bot_auth_token);
    }
}
