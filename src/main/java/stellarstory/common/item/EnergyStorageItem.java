package stellarstory.common.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовый класс предмета-накопителя Forge Energy.
 *
 * Энергия хранится в NBT стака (ключ "Energy", конвенция FE), поэтому она
 * автоматически сохраняется, переносится между сторонами и видна в тултипе.
 * Доступ для чужих машин — через стандартный capability {@link ForgeCapabilities#ENERGY}:
 * любой FE-совместимый механизм (и наши трубы/машины) заряжает и разряжает
 * предмет без знания про этот класс.
 */
public class EnergyStorageItem extends Item {

    /** Ключ NBT стака с запасом энергии — общая конвенция Forge Energy. */
    public static final String TAG_ENERGY = "Energy";

    private final int capacity;
    private final int maxTransfer;

    public EnergyStorageItem(int capacity, int maxTransfer) {
        super(new Item.Properties().stacksTo(1));
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    /** Прямой доступ к энергии стака (для своей логики без capability-запроса). */
    public int getEnergy(ItemStack stack) {
        return readEnergy(stack);
    }

    private static int readEnergy(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getInt(TAG_ENERGY)) : 0;
    }

    private static void writeEnergy(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TAG_ENERGY, Math.max(0, value));
    }

    @Override
    public @NotNull ICapabilityProvider initCapabilities(@NotNull ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyItemCapabilities(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.stellarstory.energy_stored",
                readEnergy(stack), capacity));
    }

    /**
     * Capability энергии стака. Провайдер создаётся Forge лениво на каждый ItemStack
     * (см. CapabilityProvider/forgeInit у ItemStack) и читает NBT в момент обращения,
     * поэтому copy()/клонирование стака переносят энергию корректно.
     */
    private final class EnergyItemCapabilities implements ICapabilityProvider {

        private final ItemStack owner;
        private final LazyOptional<IEnergyStorage> lazy = LazyOptional.of(this::createStorage);

        private EnergyItemCapabilities(ItemStack owner) {
            this.owner = owner;
        }

        private IEnergyStorage createStorage() {
            return new ItemEnergy(owner, capacity, maxTransfer);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.ENERGY ? lazy.cast() : LazyOptional.empty();
        }
    }

    /**
     * IEnergyStorage поверх NBT стака. Количество энергии берётся из стака при
     * каждом вызове — внешних кэшей нет, рассинхрона тоже.
     */
    private static final class ItemEnergy implements IEnergyStorage {

        private final ItemStack stack;
        private final int capacity;
        private final int maxTransfer;

        private ItemEnergy(ItemStack stack, int capacity, int maxTransfer) {
            this.stack = stack;
            this.capacity = capacity;
            this.maxTransfer = maxTransfer;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored = readEnergy(stack);
            int accepted = Math.min(Math.min(maxReceive, maxTransfer), capacity - stored);
            if (accepted <= 0) {
                return 0;
            }
            if (!simulate) {
                writeEnergy(stack, stored + accepted);
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int stored = readEnergy(stack);
            int extracted = Math.min(Math.min(maxExtract, maxTransfer), stored);
            if (extracted <= 0) {
                return 0;
            }
            if (!simulate) {
                writeEnergy(stack, stored - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return readEnergy(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return maxTransfer > 0;
        }

        @Override
        public boolean canReceive() {
            return maxTransfer > 0;
        }
    }
}
