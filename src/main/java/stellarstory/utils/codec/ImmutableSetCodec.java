package stellarstory.utils.codec;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

public class ImmutableSetCodec {
    public static <T> Codec<ImmutableSet<T>> of(Codec<T> elementCodec) {
        return elementCodec.listOf().xmap(
                ImmutableSet::copyOf, // Преобразование из List в ImmutableSet
                ImmutableSet::asList  // Преобразование из ImmutableSet в List
        );
    }
}
