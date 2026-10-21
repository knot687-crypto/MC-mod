package dev.vanta.gui;

import dev.vanta.Vanta;
import dev.vanta.core.Category;
import dev.vanta.core.Module;
import dev.vanta.core.ModuleManager;
import dev.vanta.core.Stats;
import dev.vanta.core.setting.BoolSetting;
import dev.vanta.core.setting.KeySetting;
import dev.vanta.core.setting.ModeSetting;
import dev.vanta.core.setting.NumberSetting;
import dev.vanta.core.setting.Setting;
import dev.vanta.render.Render2D;
import dev.vanta.render.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Horizontal type-kit menu: header, tab rail, module grid, live settings column.
 * Layout is fixed to the panel, so it holds together from 900 to 2560 wide.
 */
public final class ClickGui extends Screen {

    private static final float HEADER_H = 66.0f;
    private static final float TABS_H = 46.0f;
    private static final float FOOTER_H = 28.0f;
    private static final float PAD = 16.0f;
    private static final float GAP = 10.0f;
    private static final float CARD_H = 48.0f;

    private final Map<String, Float> toggles = new HashMap<>();
    private final Map<String, Float> hovers = new HashMap<>();
    private final Map<Category, Float> tabHover = new HashMap<>();

    private final List<CardHit> cards = new ArrayList<>();
    private final List<WidgetHit> widgets = new ArrayList<>();

    private Category category = Category.COMBAT;
    private Module preview;
    private String search = "";
    private boolean searchFocused;
    private Module binding;

    private float scroll;
    private float scrollTarget;
    private float settingsScroll;
    private float settingsTarget;
    private float tabX;
    private float tabW;
    private float open;
    private float panelDragX;
    private float panelDragY;
    private boolean draggingPanel;
    private NumberSetting draggingSlider;
    private Module draggingModule;

    private long lastFrame = System.nanoTime();
    private float deltaSeconds = 1.0f / 60.0f;

    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    private float searchX;
    private float searchY;
    private float searchW;
    private float searchH;
    private float closeX;
    private float closeY;

    public ClickGui() {
        super(MinecraftClient.getInstance(), MinecraftClient.getInstance().textRenderer, Text.literal("Vanta"));
    }

    // ---- screen lifecycle -------------------------------------------------

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The menu paints its own backdrop.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        deltaSeconds = Math.min(0.1f, (now - lastFrame) / 1_000_000_000.0f);
        lastFrame = now;

        open = approach(open, 1.0f, 14.0f);
        scroll = approach(scroll, scrollTarget, 16.0f);
        settingsScroll = approach(settingsScroll, settingsTarget, 16.0f);
        tabX = approach(tabX, tabXTarget(), 16.0f);
        tabW = approach(tabW, tabWTarget(), 16.0f);

        layout();
        cards.clear();
        widgets.clear();

        Render2D.rect(context, 0, 0, width, height, Render2D.alpha(0xFF05070A, (int) (168 * open)));
        drawWindow(context, mouseX, mouseY);
        drawHeader(context, mouseX, mouseY);
        drawTabs(context, mouseX, mouseY);
        drawGrid(context, mouseX, mouseY);
        drawSettings(context, mouseX, mouseY);
        drawFooter(context);
    }

    private void layout() {
        panelW = Math.min(900.0f, width - 40.0f);
        panelH = Math.min(512.0f, height - 40.0f);
        panelX = (width - panelW) / 2.0f + panelDragX;
        panelY = (height - panelH) / 2.0f + panelDragY + (1.0f - open) * 14.0f;

        searchW = Math.min(196.0f, panelW * 0.28f);
        searchH = 26.0f;
        closeX = panelX + panelW - PAD - 44.0f;
        closeY = panelY + 20.0f;
        searchX = closeX - 12.0f - searchW;
        searchY = panelY + 20.0f;
    }

    // ---- window -----------------------------------------------------------

    private void drawWindow(DrawContext context, int mouseX, int mouseY) {
        Render2D.shadow(context, panelX, panelY, panelW, panelH, 12.0f, 0xFF000000);
        Render2D.roundedV(context, panelX, panelY, panelW, panelH, 12.0f,
                Theme.WINDOW_TOP, Theme.WINDOW_BOTTOM);
        Render2D.roundedOutline(context, panelX, panelY, panelW, panelH, 12.0f, Theme.STROKE_STRONG);
        Render2D.hGradient(context, panelX + 12.0f, panelY + 1.0f, panelW - 24.0f, 1.0f,
                Render2D.alpha(Theme.accent(), 190), Render2D.alpha(Theme.accentAlt(), 40));

        Render2D.rounded(context, panelX, panelY, panelW, HEADER_H, 12.0f, Theme.HEADER);
        // Square off the plate's bottom corners so the tab rail sits flush.
        Render2D.rect(context, panelX + 1.0f, panelY + HEADER_H - 13.0f, panelW - 2.0f, 12.0f, Theme.HEADER);
        Render2D.rect(context, panelX, panelY + HEADER_H - 1.0f, panelW, 1.0f, Theme.STROKE);
    }

    private void drawHeader(DrawContext context, int mouseX, int mouseY) {
        float logo = panelY + 19.0f;
        float logoSize = 28.0f;
        Render2D.roundedV(context, panelX + PAD, logo, logoSize, logoSize, 8.0f, Theme.accent(), Theme.accentAlt());
        Render2D.roundedOutline(context, panelX + PAD, logo, logoSize, logoSize, 8.0f, 0x33FFFFFF);

        // Mark: two offset bars cut from the tile.
        Render2D.rounded(context, panelX + PAD + 8.0f, logo + 8.0f, 4.0f, 12.0f, 2.0f, 0xFFFFFFFF);
        Render2D.rounded(context, panelX + PAD + 15.0f, logo + 8.0f, 4.0f, 7.0f, 2.0f, 0xCCFFFFFF);

        float textX = panelX + PAD + logoSize + 12.0f;
        Render2D.caps(context, textRenderer, "VANTA", textX, logo + 2.0f, 1.55f, 1.5f, Theme.TEXT);
        Render2D.caps(context, textRenderer, "GHOST SUITE  ·  1.21.11", textX + 1.0f, logo + 18.0f, 0.72f, 0.9f,
                Theme.TEXT_FAINT);

        // Search pill.
        boolean hovered = inside(mouseX, mouseY, searchX, searchY, searchW, searchH);
        Render2D.rounded(context, searchX, searchY, searchW, searchH, 13.0f,
                searchFocused ? Theme.ELEVATED_HOVER : (hovered ? Theme.ELEVATED : Theme.PANEL));
        Render2D.roundedOutline(context, searchX, searchY, searchW, searchH, 13.0f,
                searchFocused ? Render2D.alpha(Theme.accent(), 150) : Theme.STROKE);

        float iconX = searchX + 10.0f;
        float iconY = searchY + 9.0f;
        Render2D.roundedOutline(context, iconX, iconY, 6.0f, 6.0f, 3.0f, searchFocused ? Theme.accent() : Theme.TEXT_FAINT);
        Render2D.rect(context, iconX + 5.5f, iconY + 5.5f, 4.0f, 1.0f, searchFocused ? Theme.accent() : Theme.TEXT_FAINT);

        String shown = search.isEmpty() ? "Search modules" : search;
        int colour = search.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT;
        Render2D.text(context, textRenderer, shown, searchX + 24.0f, searchY + 9.0f, 0.9f, colour, false);
        if (searchFocused && !search.isEmpty()) {
            float caret = searchX + 24.0f + Render2D.width(textRenderer, search) * 0.9f + 1.0f;
            Render2D.rect(context, caret, searchY + 8.0f, 1.0f, 9.0f, Theme.accent());
        }

        // Window dots.
        float dotY = closeY + 3.0f;
        int[] dots = {0xFFFF5F57, 0xFFFFBD2E, 0xFF28C840};
        for (int i = 0; i < 3; i++) {
            float dx = closeX + i * 14.0f;
            boolean dotHover = inside(mouseX, mouseY, dx - 2.0f, dotY - 2.0f, 9.0f, 9.0f);
            Render2D.rounded(context, dx, dotY, 7.0f, 7.0f, 3.5f,
                    Render2D.alpha(dots[i], dotHover ? 255 : 190));
        }
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        float y = panelY + HEADER_H + 11.0f;
        float x = panelX + PAD;

        Render2D.rounded(context, tabX, y, tabW, 24.0f, 8.0f, Render2D.alpha(Theme.accent(), 34));
        Render2D.roundedOutline(context, tabX, y, tabW, 24.0f, 8.0f, Render2D.alpha(Theme.accent(), 120));

        for (Category value : Category.values()) {
            float width = tabWidth(value);
            boolean active = value == category;
            boolean hovered = inside(mouseX, mouseY, x, y, width, 24.0f);
            float hover = approach(tabHover.getOrDefault(value, 0.0f), hovered && !active ? 1.0f : 0.0f, 12.0f);
            tabHover.put(value, hover);

            int base = active ? Theme.TEXT : Render2D.lerp(Theme.TEXT_FAINT, Theme.TEXT_DIM, hover);
            Render2D.icon(context, value.icon().name().toLowerCase(Locale.ROOT), x + 9.0f, y + 8.0f, 8.0f, base);
            Render2D.caps(context, textRenderer, value.label().toUpperCase(Locale.ROOT), x + 22.0f, y + 8.0f,
                    0.82f, 1.1f, base);

            String count = String.valueOf(ModuleManager.of(value).size());
            Render2D.text(context, textRenderer, count, x + width - 16.0f, y + 8.0f, 0.75f,
                    active ? Render2D.alpha(Theme.accentAlt(), 220) : Theme.TEXT_FAINT, false);

            x += width + 6.0f;
        }

        // Accent rail under the active tab keeps the horizontal rhythm.
        Render2D.rounded(context, tabX + 8.0f, y + 24.0f, Math.max(0.0f, tabW - 16.0f), 2.0f, 1.0f, Theme.accent());

        String summary = ModuleManager.enabledCount() + " active";
        Render2D.caps(context, textRenderer, summary.toUpperCase(Locale.ROOT),
                panelX + panelW - PAD - Render2D.capsWidth(textRenderer, summary.toUpperCase(Locale.ROOT), 0.72f, 0.9f),
                y + 9.0f, 0.72f, 0.9f, Theme.TEXT_FAINT);

        Render2D.rect(context, panelX + PAD, panelY + HEADER_H + TABS_H - 1.0f, panelW - PAD * 2.0f, 1.0f,
                Theme.STROKE);
    }

    // ---- module grid ------------------------------------------------------

    private void drawGrid(DrawContext context, int mouseX, int mouseY) {
        float contentX = panelX + PAD;
        float bodyTop = panelY + HEADER_H + TABS_H + 8.0f;
        float bodyBottom = panelY + panelH - FOOTER_H - 8.0f;
        float bodyH = bodyBottom - bodyTop;
        float leftW = (panelW - PAD * 2.0f) * 0.60f;

        List<Module> shown = filtered();
        float cardW = (leftW - GAP) / 2.0f;
        float totalRows = (float) Math.ceil(shown.size() / 2.0);
        float contentHeight = totalRows * (CARD_H + GAP);
        float maxScroll = Math.max(0.0f, contentHeight - bodyH + 6.0f);
        scrollTarget = Math.clamp(scrollTarget, 0.0f, maxScroll);
        scroll = Math.clamp(scroll, 0.0f, maxScroll);

        Render2D.scissor(context, contentX - 2.0f, bodyTop, leftW + 4.0f, bodyH);

        for (int index = 0; index < shown.size(); index++) {
            Module module = shown.get(index);
            int column = index % 2;
            int row = index / 2;
            float x = contentX + column * (cardW + GAP);
            float y = bodyTop + row * (CARD_H + GAP) - scroll;
            if (y + CARD_H < bodyTop - 4.0f || y > bodyBottom + 4.0f) {
                continue;
            }
            drawCard(context, module, x, y, cardW, CARD_H, mouseX, mouseY);
            cards.add(new CardHit(module, x, y, cardW, CARD_H));
        }

        if (shown.isEmpty()) {
            Render2D.textCentred(context, textRenderer, "nothing here", contentX + leftW / 2.0f, bodyTop + bodyH / 2.0f - 8.0f,
                    1.0f, Theme.TEXT_FAINT, false);
        }

        Render2D.unScissor(context);

        if (maxScroll > 0.0f) {
            float railH = 44.0f;
            float ratio = Math.clamp(scroll / maxScroll, 0.0f, 1.0f);
            float railY = bodyTop + (bodyH - railH) * ratio;
            Render2D.rounded(context, contentX + leftW + 3.0f, bodyTop, 2.0f, bodyH, 1.0f, 0x10FFFFFF);
            Render2D.roundedV(context, contentX + leftW + 3.0f, railY, 2.0f, railH, 1.0f,
                    Theme.accent(), Theme.accentAlt());
        }
    }

    private void drawCard(DrawContext context, Module module, float x, float y, float width, float height,
                          int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, height);
        float hover = approach(hovers.getOrDefault(module.name(), 0.0f), hovered ? 1.0f : 0.0f, 14.0f);
        hovers.put(module.name(), hover);
        float on = approach(toggles.getOrDefault(module.name(), module.enabled() ? 1.0f : 0.0f),
                module.enabled() ? 1.0f : 0.0f, 15.0f);
        toggles.put(module.name(), on);
        if (hovered) {
            preview = module;
        }

        int base = Render2D.lerp(Theme.CARD, Theme.CARD_HOVER, hover * 0.85f);
        base = Render2D.lerp(base, Theme.CARD_ACTIVE, on * 0.6f);
        Render2D.rounded(context, x, y, width, height, 9.0f, base);
        Render2D.roundedOutline(context, x, y, width, height, 9.0f,
                Render2D.lerp(Theme.STROKE, Render2D.alpha(Theme.accent(), 110), Math.max(hover * 0.55f, on * 0.7f)));

        float barH = height - 22.0f;
        if (on > 0.01f) {
            Render2D.roundedV(context, x + 9.0f, y + 11.0f, 3.0f, barH, 1.5f, Theme.accent(), Theme.accentAlt());
        } else {
            Render2D.rounded(context, x + 9.0f, y + 11.0f, 3.0f, barH, 1.5f, 0x22FFFFFF);
        }

        int titleColour = Render2D.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(hover, on));
        Render2D.text(context, textRenderer, module.name(), x + 20.0f, y + 13.0f, 1.0f, titleColour, false);

        // Trim instead of nesting another scissor: the grid clip is already active.
        Render2D.text(context, textRenderer, trim(module.description(), width - 40.0f, 0.72f), x + 20.0f, y + 27.0f,
                0.72f, Theme.TEXT_FAINT, false);

        // Mini switch.
        float switchW = 22.0f;
        float switchH = 12.0f;
        float switchX = x + width - switchW - 12.0f;
        float switchY = y + height / 2.0f - switchH / 2.0f;
        Render2D.rounded(context, switchX, switchY, switchW, switchH, 6.0f,
                Render2D.lerp(Theme.TRACK, Render2D.alpha(Theme.accent(), 235), on));
        float knob = 8.0f;
        float knobX = switchX + 2.0f + (switchW - knob - 4.0f) * on;
        Render2D.rounded(context, knobX, switchY + 2.0f, knob, knob, knob / 2.0f,
                Render2D.lerp(0xFF8C94A8, 0xFFFFFFFF, on));
    }

    // ---- settings ---------------------------------------------------------

    private void drawSettings(DrawContext context, int mouseX, int mouseY) {
        float contentX = panelX + PAD;
        float contentW = panelW - PAD * 2.0f;
        float leftW = contentW * 0.60f;
        float rightX = contentX + leftW + 12.0f;
        float rightW = contentW - leftW - 12.0f;
        float bodyTop = panelY + HEADER_H + TABS_H + 8.0f;
        float bodyBottom = panelY + panelH - FOOTER_H - 8.0f;
        float bodyH = bodyBottom - bodyTop;

        Render2D.roundedV(context, rightX, bodyTop, rightW, bodyH, 10.0f, 0xFF101620, 0xFF0C1119);
        Render2D.roundedOutline(context, rightX, bodyTop, rightW, bodyH, 10.0f, Theme.STROKE);
        Render2D.hGradient(context, rightX + 10.0f, bodyTop, rightW - 20.0f, 1.0f,
                Render2D.alpha(Theme.accent(), 120), 0x00FFFFFF);

        Module module = preview;
        if (module == null) {
            Render2D.caps(context, textRenderer, "SETTINGS", rightX + 16.0f, bodyTop + 16.0f, 0.75f, 1.2f, Theme.TEXT_FAINT);
            Render2D.textCentred(context, textRenderer, "hover a module", rightX + rightW / 2.0f, bodyTop + bodyH / 2.0f - 6.0f,
                    0.9f, Theme.TEXT_FAINT, false);
            return;
        }

        Render2D.caps(context, textRenderer, module.category().label().toUpperCase(Locale.ROOT) + "  /  SETTINGS",
                rightX + 16.0f, bodyTop + 16.0f, 0.72f, 1.1f, Theme.TEXT_FAINT);

        Render2D.text(context, textRenderer, module.name(), rightX + 16.0f, bodyTop + 30.0f, 1.25f, Theme.TEXT, false);

        // Toggle chip top right of the panel.
        float chipW = 40.0f;
        float chipH = 16.0f;
        float chipX = rightX + rightW - chipW - 14.0f;
        float chipY = bodyTop + 32.0f;
        boolean chipHover = inside(mouseX, mouseY, chipX, chipY, chipW, chipH);
        Render2D.rounded(context, chipX, chipY, chipW, chipH, 8.0f,
                module.enabled() ? Render2D.alpha(Theme.GOOD, 42) : Theme.PANEL);
        Render2D.roundedOutline(context, chipX, chipY, chipW, chipH, 8.0f,
                module.enabled() ? Render2D.alpha(Theme.GOOD, 150) : (chipHover ? Theme.STROKE_STRONG : Theme.STROKE));
        Render2D.caps(context, textRenderer, module.enabled() ? "ON" : "OFF",
                chipX + (chipW - Render2D.capsWidth(textRenderer, module.enabled() ? "ON" : "OFF", 0.7f, 1.0f)) / 2.0f,
                chipY + 5.0f, 0.7f, 1.0f, module.enabled() ? Theme.GOOD : Theme.TEXT_FAINT);
        widgets.add(new WidgetHit("module-toggle", module, null, chipX, chipY, chipW, chipH));

        float descTop = bodyTop + 48.0f;
        List<String> wrapped = wrap(module.description(), rightW - 32.0f, 0.72f);
        float descLine = descTop;
        for (int i = 0; i < Math.min(2, wrapped.size()); i++) {
            Render2D.text(context, textRenderer, wrapped.get(i), rightX + 16.0f, descLine, 0.72f, Theme.TEXT_DIM, false);
            descLine += 10.0f;
        }

        float listTop = descLine + 8.0f;
        Render2D.rect(context, rightX + 14.0f, listTop - 6.0f, rightW - 28.0f, 1.0f, Theme.STROKE);

        List<Setting<?>> settings = module.settings();
        float rowsHeight = 0.0f;
        for (Setting<?> setting : settings) {
            rowsHeight += rowHeight(setting);
        }
        float listH = bodyBottom - listTop - 34.0f;
        float maxScroll = Math.max(0.0f, rowsHeight - listH);
        settingsTarget = Math.clamp(settingsTarget, 0.0f, maxScroll);
        settingsScroll = Math.clamp(settingsScroll, 0.0f, maxScroll);

        Render2D.scissor(context, rightX + 2.0f, listTop, rightW - 4.0f, listH);
        float cursor = listTop - settingsScroll;
        for (Setting<?> setting : settings) {
            float h = rowHeight(setting);
            if (cursor + h >= listTop - 2.0f && cursor <= listTop + listH) {
                drawSettingRow(context, module, setting, rightX + 16.0f, cursor, rightW - 32.0f, mouseX, mouseY);
            }
            cursor += h;
        }
        Render2D.unScissor(context);

        if (maxScroll > 0.0f) {
            float ratio = Math.clamp(settingsScroll / maxScroll, 0.0f, 1.0f);
            float railH = listH * Math.max(0.18f, listH / rowsHeight);
            Render2D.rounded(context, rightX + rightW - 6.0f, listTop + (listH - railH) * ratio, 2.0f, railH, 1.0f,
                    Render2D.alpha(Theme.accent(), 170));
        }

        // Bind row pinned to the bottom of the column.
        float bindY = bodyBottom - 24.0f;
        Render2D.rect(context, rightX + 14.0f, bindY - 8.0f, rightW - 28.0f, 1.0f, Theme.STROKE);
        Render2D.caps(context, textRenderer, "BIND", rightX + 16.0f, bindY, 0.72f, 1.1f, Theme.TEXT_FAINT);
        String label = binding == module ? "press a key" : KeySetting.nameOf(module.key());
        float labelW = Render2D.capsWidth(textRenderer, label.toUpperCase(Locale.ROOT), 0.72f, 0.9f);
        float chipBW = labelW + 18.0f;
        float chipBX = rightX + rightW - chipBW - 16.0f;
        boolean bindHover = inside(mouseX, mouseY, chipBX, bindY - 5.0f, chipBW, 16.0f);
        Render2D.rounded(context, chipBX, bindY - 5.0f, chipBW, 16.0f, 8.0f,
                binding == module ? Render2D.alpha(Theme.accent(), 60) : (bindHover ? Theme.ELEVATED_HOVER : Theme.ELEVATED));
        Render2D.roundedOutline(context, chipBX, bindY - 5.0f, chipBW, 16.0f, 8.0f,
                binding == module ? Render2D.alpha(Theme.accent(), 160) : Theme.STROKE);
        Render2D.caps(context, textRenderer, label.toUpperCase(Locale.ROOT), chipBX + 9.0f, bindY - 1.0f, 0.72f, 0.9f,
                binding == module ? Theme.accent() : Theme.TEXT_DIM);
        widgets.add(new WidgetHit("bind", module, null, chipBX, bindY - 5.0f, chipBW, 16.0f));
    }

    private void drawSettingRow(DrawContext context, Module module, Setting<?> setting, float x, float y,
                                float width, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, width, rowHeight(setting));

        if (setting instanceof BoolSetting bool) {
            Render2D.text(context, textRenderer, setting.name(), x, y + 6.0f, 0.82f,
                    hovered ? Theme.TEXT : Theme.TEXT_DIM, false);
            float switchW = 22.0f;
            float switchH = 12.0f;
            float switchX = x + width - switchW;
            float switchY = y + 5.0f;
            float on = approach(toggles.getOrDefault(module.name() + "." + setting.name(), bool.get() ? 1.0f : 0.0f),
                    bool.get() ? 1.0f : 0.0f, 16.0f);
            toggles.put(module.name() + "." + setting.name(), on);
            Render2D.rounded(context, switchX, switchY, switchW, switchH, 6.0f,
                    Render2D.lerp(Theme.TRACK, Render2D.alpha(Theme.accent(), 235), on));
            Render2D.rounded(context, switchX + 2.0f + (switchW - 12.0f) * on, switchY + 2.0f, 8.0f, 8.0f, 4.0f,
                    Render2D.lerp(0xFF8C94A8, 0xFFFFFFFF, on));
            widgets.add(new WidgetHit("bool", module, setting, x, y, width, 22.0f));
            return;
        }

        if (setting instanceof NumberSetting number) {
            Render2D.text(context, textRenderer, setting.name(), x, y + 4.0f, 0.82f,
                    hovered ? Theme.TEXT : Theme.TEXT_DIM, false);
            String value = number.display();
            Render2D.text(context, textRenderer, value, x + width - Render2D.width(textRenderer, value), y + 3.0f,
                    0.82f, Render2D.alpha(Theme.accentAlt(), 235), false);

            float trackY = y + 20.0f;
            float trackH = 4.0f;
            Render2D.rounded(context, x, trackY, width, trackH, 2.0f, Theme.TRACK);
            float filled = Math.max(2.0f, width * (float) number.ratio());
            Render2D.roundedV(context, x, trackY, filled, trackH, 2.0f, Theme.accent(), Theme.accentAlt());
            float knobX = x + filled;
            Render2D.rounded(context, knobX - 4.0f, trackY - 3.0f, 8.0f, 10.0f, 4.0f, 0xFFF4F7FF);
            Render2D.roundedOutline(context, knobX - 5.5f, trackY - 4.5f, 11.0f, 13.0f, 5.0f,
                    Render2D.alpha(Theme.accent(), 120));
            widgets.add(new WidgetHit("slider", module, setting, x, trackY - 6.0f, width, 16.0f));
            return;
        }

        if (setting instanceof ModeSetting mode) {
            Render2D.text(context, textRenderer, setting.name(), x, y + 6.0f, 0.82f,
                    hovered ? Theme.TEXT : Theme.TEXT_DIM, false);
            String value = mode.get();
            float valueW = Render2D.width(textRenderer, value);
            float valueX = x + width - valueW - 1.0f;
            Render2D.text(context, textRenderer, value, valueX, y + 6.0f, 0.82f, Theme.TEXT, false);

            float arrowLeft = valueX - 14.0f;
            float arrowRight = x + width + 2.0f;
            chevron(context, arrowLeft, y + 10.0f, true, hovered ? Theme.TEXT_DIM : Theme.TEXT_FAINT);
            chevron(context, arrowRight - 6.0f, y + 10.0f, false, hovered ? Theme.TEXT_DIM : Theme.TEXT_FAINT);
            widgets.add(new WidgetHit("mode-left", module, setting, arrowLeft - 2.0f, y + 2.0f, 12.0f, 16.0f));
            widgets.add(new WidgetHit("mode-right", module, setting, arrowRight - 8.0f, y + 2.0f, 12.0f, 16.0f));
        }
    }

    private void chevron(DrawContext context, float x, float y, boolean left, int color) {
        for (int i = 0; i < 3; i++) {
            float offset = left ? i : 2 - i;
            Render2D.rect(context, x + offset, y + i, 1.0f, 1.0f, color);
            Render2D.rect(context, x + offset, y + 4 - i, 1.0f, 1.0f, color);
        }
    }

    private void drawFooter(DrawContext context) {
        float y = panelY + panelH - FOOTER_H;
        Render2D.rect(context, panelX, y, panelW, 1.0f, Theme.STROKE);
        Render2D.rounded(context, panelX, y, panelW, FOOTER_H, 12.0f, Render2D.alpha(Theme.HEADER, 140));
        Render2D.rect(context, panelX + 1.0f, y, panelW - 2.0f, 10.0f, Render2D.alpha(Theme.HEADER, 140));

        String left = "RSHIFT  toggle    ·    drag  move    ·    scroll  browse";
        Render2D.caps(context, textRenderer, left.toUpperCase(Locale.ROOT), panelX + PAD, y + 10.0f, 0.68f, 0.8f,
                Theme.TEXT_FAINT);

        String right = Stats.fps() + " fps   ·   " + Stats.ping() + " ms   ·   " + statsLine();
        Render2D.caps(context, textRenderer, right.toUpperCase(Locale.ROOT),
                panelX + panelW - PAD - Render2D.capsWidth(textRenderer, right.toUpperCase(Locale.ROOT), 0.68f, 0.8f),
                y + 10.0f, 0.68f, 0.8f, Theme.TEXT_FAINT);
    }

    private String statsLine() {
        return Theme.accentName() + " accent";
    }

    // ---- input ------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            for (CardHit card : cards) {
                if (inside((float) mouseX, (float) mouseY, card.x, card.y, card.w, card.h)) {
                    card.module.toggle();
                    preview = card.module;
                    return true;
                }
            }
            for (WidgetHit widget : widgets) {
                if (inside((float) mouseX, (float) mouseY, widget.x, widget.y, widget.w, widget.h)) {
                    handleWidget(widget, mouseX, mouseY);
                    return true;
                }
            }
            if (inside((float) mouseX, (float) mouseY, searchX, searchY, searchW, searchH)) {
                searchFocused = true;
                return true;
            }
            if (inside((float) mouseX, (float) mouseY, closeX, closeY, 42.0f, 14.0f)) {
                close();
                return true;
            }
            if (tryTabs((float) mouseX, (float) mouseY)) {
                return true;
            }
            if (inside((float) mouseX, (float) mouseY, panelX, panelY, panelW, HEADER_H)) {
                draggingPanel = true;
                return true;
            }
            if (!inside((float) mouseX, (float) mouseY, panelX, panelY, panelW, panelH)) {
                close();
            }
            searchFocused = inside((float) mouseX, (float) mouseY, searchX, searchY, searchW, searchH);
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean tryTabs(float mouseX, float mouseY) {
        float y = panelY + HEADER_H + 11.0f;
        float x = panelX + PAD;
        for (Category value : Category.values()) {
            float width = tabWidth(value);
            if (inside(mouseX, mouseY, x, y, width, 24.0f)) {
                category = value;
                scrollTarget = 0.0f;
                settingsTarget = 0.0f;
                return true;
            }
            x += width + 6.0f;
        }
        return false;
    }

    private void handleWidget(WidgetHit widget, double mouseX, double mouseY) {
        if (widget.setting instanceof BoolSetting bool && widget.kind.equals("bool")) {
            bool.toggle();
            return;
        }
        if (widget.setting instanceof NumberSetting number && widget.kind.equals("slider")) {
            float ratio = Math.clamp((float) ((mouseX - widget.x) / widget.w), 0.0f, 1.0f);
            number.set(number.min() + (number.max() - number.min()) * ratio);
            draggingSlider = number;
            draggingModule = widget.module;
            return;
        }
        if (widget.kind.equals("mode-left") && widget.setting instanceof ModeSetting mode) {
            mode.cycle(-1);
            return;
        }
        if (widget.kind.equals("mode-right") && widget.setting instanceof ModeSetting mode) {
            mode.cycle(1);
            return;
        }
        if (widget.kind.equals("module-toggle")) {
            widget.module.toggle();
            return;
        }
        if (widget.kind.equals("bind")) {
            binding = binding == widget.module ? null : widget.module;
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (draggingPanel) {
            panelDragX += (float) deltaX;
            panelDragY += (float) deltaY;
            return true;
        }
        if (draggingSlider != null) {
            for (WidgetHit widget : widgets) {
                if (widget.setting == draggingSlider && widget.kind.equals("slider")) {
                    float ratio = Math.clamp((float) ((mouseX - widget.x) / widget.w), 0.0f, 1.0f);
                    draggingSlider.set(draggingSlider.min() + (draggingSlider.max() - draggingSlider.min()) * ratio);
                    return true;
                }
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = false;
        draggingSlider = null;
        draggingModule = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float contentX = panelX + PAD;
        float leftW = (panelW - PAD * 2.0f) * 0.60f;
        float bodyTop = panelY + HEADER_H + TABS_H + 8.0f;
        if (inside((float) mouseX, (float) mouseY, contentX, bodyTop, leftW, panelH)) {
            scrollTarget -= (float) verticalAmount * 26.0f;
            return true;
        }
        if (inside((float) mouseX, (float) mouseY, contentX + leftW + 12.0f, bodyTop,
                panelW - leftW - PAD * 3.0f, panelH)) {
            settingsTarget -= (float) verticalAmount * 22.0f;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                binding.setKey(-1);
            } else if (keyCode != GLFW.GLFW_KEY_F3) {
                binding.setKey(keyCode);
            }
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (searchFocused && input.isValidChar() && search.length() < 24
                && (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '_')) {
            search += chr;
            return true;
        }
        return super.charTyped(input);
    }

    // ---- helpers ----------------------------------------------------------

    private List<Module> filtered() {
        List<Module> list = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            for (Module module : ModuleManager.all()) {
                if (module.name().toLowerCase(Locale.ROOT).contains(query)
                        || module.description().toLowerCase(Locale.ROOT).contains(query)) {
                    list.add(module);
                }
            }
            return list;
        }
        return ModuleManager.of(category);
    }

    private float tabWidth(Category value) {
        String label = value.label().toUpperCase(Locale.ROOT);
        return 34.0f + Render2D.capsWidth(textRenderer, label, 0.82f, 1.1f);
    }

    private float tabXTarget() {
        float x = panelX + PAD;
        for (Category value : Category.values()) {
            if (value == category) {
                return x;
            }
            x += tabWidth(value) + 6.0f;
        }
        return panelX + PAD;
    }

    private float tabWTarget() {
        return tabWidth(category);
    }

    private float rowHeight(Setting<?> setting) {
        if (setting instanceof NumberSetting) {
            return 32.0f;
        }
        return 24.0f;
    }

    private String trim(String text, float maxWidth, float scale) {
        if (Render2D.width(textRenderer, text) * scale <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 3 && Render2D.width(textRenderer, result + "..") * scale > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "..";
    }

    private List<String> wrap(String text, float maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Render2D.width(textRenderer, candidate) * scale > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private float approach(float current, float target, float speed) {
        return current + (target - current) * (1.0f - (float) Math.exp(-speed * deltaSeconds));
    }

    private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CardHit(Module module, float x, float y, float w, float h) {
    }

    private record WidgetHit(String kind, Module module, Setting<?> setting, float x, float y, float w, float h) {
    }
}
