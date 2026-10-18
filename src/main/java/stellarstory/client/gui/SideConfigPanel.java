package stellarstory.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import stellarstory.api.transfer.SideConfig;
import stellarstory.api.transfer.SideMode;
import stellarstory.api.transfer.TransferKind;
import stellarstory.network.NetworkHandler;
import stellarstory.network.packet.server.SetSideConfigPacket;

/**
 * Панель настройки сторон: 6 кнопок граней + по кнопке режима на каждый вид переноса.
 *
 * Клик по грани выбирает её, клик по режиму ставит этот режим и отправляет
 * пакет на сервер. Сервер отвечает обновлённым BlockEntity-тегом, панель
 * перечитывает SideConfig из того же объекта — отдельного ответа не нужно.
 */
public final class SideConfigPanel {

    /** Порядок и подписи граней: UP, DOWN, NORTH, SOUTH, EAST, WEST. */
    private static final Direction[] FACES = {
            Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private static final TransferKind[] KINDS = TransferKind.values();

    /** Кнопки режимов идут в том же порядке, что и TransferKind. */
    private static final String[] KIND_ICONS = {"E", "I", "F"};

    public static final int HEIGHT = 40;
    public static final int WIDTH = 160;

    private static final int FACE_SIZE = 20;
    private static final int FACE_STEP = 26;
    private static final int FACE_Y = 2;

    private static final int MODE_W = 50;
    private static final int MODE_H = 12;
    private static final int MODE_STEP = 53;
    private static final int MODE_Y = 26;

    private static final int COLOR_PANEL = 0xCC0E0E14;
    private static final int COLOR_FACE = 0xFF2E2E38;
    private static final int COLOR_FACE_HOVER = 0xFF3E3E4C;
    private static final int COLOR_FACE_SELECTED = 0xFF51516A;
    private static final int COLOR_LABEL = 0xFF9A9AA8;

    private static final int COLOR_DISABLED = 0xFF6E6E6E;
    private static final int COLOR_INPUT = 0xFF4A87C7;
    private static final int COLOR_OUTPUT = 0xFFC7603F;
    private static final int COLOR_BOTH = 0xFF5FA85F;

    private final SideConfig sideConfig;
    private final BlockPos machinePos;
    private int x;
    private int y;

    /** Индекс грани из FACES, либо -1 если ничего не выбрано. */
    private int selectedFace = -1;

    /** Тултип под курсором — рисуется самим экраном поверх остальных элементов. */
    @Nullable
    private Component tooltip;

    public SideConfigPanel(SideConfig sideConfig, BlockPos machinePos, int x, int y) {
        this.sideConfig = sideConfig;
        this.machinePos = machinePos.immutable();
        this.x = x;
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Nullable
    public Component getTooltip() {
        return tooltip;
    }

    // ═══════════════════════════════════════════════════════════════
    // Отрисовка
    // ═══════════════════════════════════════════════════════════════

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        tooltip = null;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, COLOR_PANEL);

        renderFaces(graphics, mouseX, mouseY);

        if (selectedFace >= 0) {
            renderModes(graphics, mouseX, mouseY);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font,
                    Component.translatable("gui.stellarstory.side_config.hint"),
                    x + WIDTH / 2, y + MODE_Y + 2, COLOR_LABEL);
        }
    }

    private void renderFaces(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < FACES.length; i++) {
            int fx = x + i * FACE_STEP;
            boolean hovered = contains(mouseX, mouseY, fx, y + FACE_Y, FACE_SIZE, FACE_SIZE);
            boolean selected = selectedFace == i;

            int color = selected ? COLOR_FACE_SELECTED : (hovered ? COLOR_FACE_HOVER : COLOR_FACE);
            graphics.fill(fx, y + FACE_Y, fx + FACE_SIZE, y + FACE_Y + FACE_SIZE, color);

            String glyph = FACES[i].getSerializedName().substring(0, 1).toUpperCase();
            graphics.drawCenteredString(font, glyph, fx + FACE_SIZE / 2, y + FACE_Y + 6, 0xFFE6E6EC);

            if (hovered) {
                tooltip = Component.translatable("gui.stellarstory.side." + FACES[i].getSerializedName());
            }
        }
    }

    private void renderModes(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        Direction face = FACES[selectedFace];

        for (int k = 0; k < KINDS.length; k++) {
            TransferKind kind = KINDS[k];
            SideMode mode = sideConfig.get(kind, face);
            int mx = x + k * MODE_STEP;

            graphics.fill(mx, y + MODE_Y, mx + MODE_W, y + MODE_Y + MODE_H, modeColor(mode));
            graphics.drawString(font, KIND_ICONS[k] + " " + shortModeName(mode),
                    mx + 3, y + MODE_Y + 2, 0xFF101014);

            if (contains(mouseX, mouseY, mx, y + MODE_Y, MODE_W, MODE_H)) {
                tooltip = Component.translatable(
                                "gui.stellarstory.side_config.mode_tooltip",
                                Component.translatable("gui.stellarstory.transfer_kind." + kind.name().toLowerCase()),
                                Component.translatable("gui.stellarstory.side." + face.getSerializedName()),
                                Component.translatable(mode.translationKey()));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Ввод
    // ═══════════════════════════════════════════════════════════════

    /** @return true, если клик принадлежал панели. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < FACES.length; i++) {
            int fx = x + i * FACE_STEP;
            if (contains(mouseX, mouseY, fx, y + FACE_Y, FACE_SIZE, FACE_SIZE)) {
                selectedFace = (selectedFace == i) ? -1 : i;
                return true;
            }
        }
        if (selectedFace >= 0) {
            for (int k = 0; k < KINDS.length; k++) {
                int mx = x + k * MODE_STEP;
                if (contains(mouseX, mouseY, mx, y + MODE_Y, MODE_W, MODE_H)) {
                    sendMode(FACES[selectedFace], KINDS[k], button);
                    return true;
                }
            }
        }
        // Клик мимо панели снимает выбор — иначе панель «залипает» открытой.
        selectedFace = -1;
        return false;
    }

    /** ЛКМ — следующий режим, ПКМ — предыдущий. */
    private void sendMode(Direction face, TransferKind kind, int button) {
        SideMode current = sideConfig.get(kind, face);
        SideMode next = button == 1 ? previous(current) : current.next();
        // Локально применяем сразу: сервер подтвердит тем же значением через update-пакет.
        sideConfig.set(kind, face, next);
        NetworkHandler.PACKET_HANDLER.sendToServer(new SetSideConfigPacket(machinePos, face, kind, next));
    }

    private static SideMode previous(SideMode mode) {
        SideMode[] all = SideMode.values();
        return all[(mode.ordinal() + all.length - 1) % all.length];
    }

    // ═══════════════════════════════════════════════════════════════
    // Мелочи
    // ═══════════════════════════════════════════════════════════════

    private static boolean contains(double mx, double my, int rx, int ry, int w, int h) {
        return mx >= rx && mx < rx + w && my >= ry && my < ry + h;
    }

    private static int modeColor(SideMode mode) {
        return switch (mode) {
            case DISABLED -> COLOR_DISABLED;
            case INPUT -> COLOR_INPUT;
            case OUTPUT -> COLOR_OUTPUT;
            case BOTH -> COLOR_BOTH;
        };
    }

    /** Короткая подпись режима для тесной кнопки. */
    private static String shortModeName(SideMode mode) {
        return switch (mode) {
            case DISABLED -> "--";
            case INPUT -> "IN";
            case OUTPUT -> "OUT";
            case BOTH -> "I/O";
        };
    }
}
