package stellarstory.api.utils.block.state.property;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Optional;

public class StringProperty extends Property<String> {
    private final Collection<String> values;

    protected StringProperty(String name, Collection<String> values) {
        super(name, String.class);
        this.values = values;
    }

    @Override
    public Collection<String> getPossibleValues() {
        return values;
    }

    @Override
    public String getName(String value) {
        return value;
    }

    @Override
    public Optional<String> getValue(String input) {
        return values.contains(input) ? Optional.of(input) : Optional.empty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StringProperty)) return false;
        StringProperty other = (StringProperty) obj;
        return this.getName().equals(other.getName()) && this.values.equals(other.values);
    }

    @Override
    public int generateHashCode() {
        return 31 * this.getName().hashCode() + this.values.hashCode();
    }

    public static StringProperty create(String name, Collection<String> values) {
        return new StringProperty(name, values);
    }
}
