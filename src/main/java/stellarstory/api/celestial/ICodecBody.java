package stellarstory.api.celestial;

import com.mojang.serialization.Codec;

public interface ICodecBody {
    Codec<? extends ICodecBody> getCodec();

}