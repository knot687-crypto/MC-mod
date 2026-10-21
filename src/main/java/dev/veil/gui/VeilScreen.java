package dev.veil.gui;

import dev.veil.Veil;
import dev.veil.core.Category;
import dev.veil.core.Module;
import dev.veil.core.NumberSetting;
import dev.veil.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Horizontal shell: category rail across the top, module board on the left, live settings on
 * the right. One frame delta drives every animation so the motion stays coherent.
 */
public final class VeilScreen extends Screen {
    private static final int PANEL_W = 624;
    private static final int PANEL_H = 348;
    private static final int HEADER_H = 52;
    private static final int TAB_H = 34;
    private static final int FOOTER_H = 32;
    private static final int LIST_W = 384;
    private static final int SETTINGS_W = 188;
    private static final int CARD_H = 26;
    private static final int CARD_GAP = 7;
    private static final int CONTENT_TOP = 44;
    private static final int BIND_ZONE = 32;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, Float> hover = new HashMap<>();

    private Category category = Category.COMBAT;
    private Module selected;
    private Module binding;
    private String query = "";
    private boolean searching;

    private float open;
    private float uiScale = 1.0f;
    private float tabIndicator = -1.0f;
    private double scroll;
    private double scrollTarget;
    private double settingsScroll;
    private double settingsTarget;
    private long lastFrame = System.nanoTime();
    private boolean draggingSlider;
    private Setting<?> dragSetting;

    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int settingsX;
    private int settingsY;
    private int listHeight;

    public VeilScreen() {
        super(Text.literal("Veil"));
    }

    @Override
    protected void init() {
        uiScale = Math.min(1.0f, Math.min((width - 20.0f) / PANEL_W, (height - 20.0f) / PANEL_H));
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        listX = panelX + 18;
        listY = panelY + HEADER_H + TAB_H + 12;
        settingsX = panelX + PANEL_W - SETTINGS_W - 18;
        settingsY = listY;
        listHeight = PANEL_H - HEADER_H - TAB_H - FOOTER_H - 20;
        if (selected == null) {
            List<Module> modules = Veil.modules().byCategory(category);
            selected = modules.isEmpty() ? null : modules.get(0);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float dt = Math.min(0.1f, (System.nanoTime() - lastFrame) / 1_000_000_000.0f);
        lastFrame = System.nanoTime();
        open = approach(open, 1.0f, dt * 9.0f);
        Theme.tick(dt);
        scroll = approach((float) scroll, (float) scrollTarget, dt * 12.0f);
        settingsScroll = approach((float) settingsScroll, (float) settingsTarget, dt * 12.0f);

        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);
        float scale = uiScale * (0.955f + 0.045f * ease(open));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(width / 2.0f, height / 2.0f);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-width / 2.0f, -height / 2.0f);

        drawPanel(ctx);
        drawHeader(ctx, localMouseX, localMouseY, dt);
        drawTabs(ctx, localMouseX, localMouseY);
        drawList(ctx, localMouseX, localMouseY);
        drawSettings(ctx, localMouseX, localMouseY);
        drawFooter(ctx);

        ctx.getMatrices().popMatrix();
    }

    private void drawPanel(DrawContext ctx) {
        Render2D.shadow(ctx, panelX, panelY, PANEL_W, PANEL_H, 12);
        Render2D.rounded(ctx, panelX - 1, panelY - 1, PANEL_W + 2, PANEL_H + 2, 13, Theme.alpha(Theme.accent(), 0.32f));
        Render2D.rounded(ctx, panelX, panelY, PANEL_W, PANEL_H, 12, 0xFF08080E);
        Render2D.rounded(ctx, panelX + 1, panelY + 1, PANEL_W - 2, PANEL_H - 2, 11, 0xFF0C0C14);
        ctx.fillGradient(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + 200, 0x0EFFFFFF, 0x00000000);

        Render2D.glow(ctx, panelX + PANEL_W - 168, panelY - 10, 150, 80, Theme.accent(), 5);
        Render2D.rounded(ctx, panelX + PANEL_W - 214, panelY + 1, 212, 116, 10, Theme.alpha(Theme.accent(), 0.05f));
        ctx.fill(panelX + 1, panelY + HEADER_H, panelX + PANEL_W - 1, panelY + HEADER_H + 1, Theme.STROKE);
        ctx.fill(panelX + 1, panelY + PANEL_H - FOOTER_H, panelX + PANEL_W - 1, panelY + PANEL_H - FOOTER_H + 1, Theme.STROKE);
        ctx.fill(settingsX - 10, settingsY, settingsX - 9, settingsY + listHeight, 0x12FFFFFF);
    }

    private void drawHeader(DrawContext ctx, int mouseX, int mouseY, float dt) {
        int x = panelX + 20;
        int y = panelY + 16;

        Render2D.rounded(ctx, x, y + 3, 5, 18, 2, Theme.accent());
        Render2D.rounded(ctx, x, y + 17, 5, 4, 2, Theme.accentDeep());
        Render2D.spacedText(ctx, textRenderer, "VEIL", x + 14, y + 6, Theme.TEXT, 2);
        Render2D.text(ctx, textRenderer, "GHOST SUITE", x + 16, y + 19, Theme.TEXT_FAINT);

        int chipX = x + 108;
        Render2D.rounded(ctx, chipX, y + 8, 46, 15, 4, Theme.alpha(Theme.accent(), 0.14f));
        Render2D.text(ctx, textRenderer, "1.21.11", chipX + 8, y + 12, Theme.accent());

        int searchW = 168;
        int searchX = panelX + PANEL_W - searchW - 20;
        int searchY = panelY + 15;
        boolean searchHover = inside(mouseX, mouseY, searchX, searchY, searchW, 22);
        float glow = hover("search", searchHover, dt);
        Render2D.rounded(ctx, searchX, searchY, searchW, 22, 6, Theme.mix(Theme.CARD, Theme.CARD_HOVER, glow));
        Render2D.rounded(ctx, searchX, searchY, searchW, 22, 6, Theme.alpha(0xFFFFFF, searching ? 0.10f : 0.04f));
        Render2D.rounded(ctx, searchX, searchY, searchW, 1, 0, Theme.alpha(Theme.accent(), searching ? 0.85f : 0.18f));

        String shown = query.isEmpty() ? "Search modules" : query;
        int textColor = query.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT;
        Render2D.text(ctx, textRenderer, shown, searchX + 9, searchY + 7, textColor);
        if (searching) {
            int caretX = searchX + 9 + Render2D.width(textRenderer, query) + 1;
            boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
            if (blink) {
                ctx.fill(caretX, searchY + 6, caretX + 1, searchY + 16, Theme.accent());
            }
        }
    }

    private void drawTabs(DrawContext ctx, int mouseX, int mouseY) {
        int tabY = panelY + HEADER_H + 4;
        int cursor = panelX + 20;
        int activeX = cursor;
        int activeW = 0;

        for (Category value : Category.values()) {
            String label = value.label();
            int tabW = Render2D.width(textRenderer, label) + 26;
            boolean active = value == category;
            boolean over = inside(mouseX, mouseY, cursor, tabY, tabW, 23);
            float glow = hover.getOrDefault("tab" + value.name(), 0.0f);
            hover.put("tab" + value.name(), approach(glow, over ? 1.0f : 0.0f, 0.16f));

            if (active) {
                Render2D.rounded(ctx, cursor, tabY, tabW, 23, 6, Theme.alpha(Theme.accent(), 0.13f));
                Render2D.rounded(ctx, cursor, tabY, tabW, 23, 6, Theme.alpha(0xFFFFFF, 0.04f));
                activeX = cursor;
                activeW = tabW;
            } else if (glow > 0.01f) {
                Render2D.rounded(ctx, cursor, tabY, tabW, 23, 6, Theme.alpha(0xFFFFFF, 0.035f * glow));
            }

            int color = active ? Theme.accent() : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, glow);
            Render2D.text(ctx, textRenderer, label, cursor + 13, tabY + 8, color);
            cursor += tabW + 6;
        }

        if (tabIndicator < 0.0f) {
            tabIndicator = activeX;
        } else {
            tabIndicator = approach(tabIndicator, activeX, 0.18f);
        }
        Render2D.rounded(ctx, Math.round(tabIndicator) + 10, tabY + 24, Math.max(0, activeW - 20), 2, 1, Theme.accent());

        int right = panelX + PANEL_W - 20;
        String bindHint = binding == null ? "Bind: click a key chip" : "Press any key";
        Render2D.text(ctx, textRenderer, bindHint, right - Render2D.width(textRenderer, bindHint), tabY + 8,
                binding == null ? Theme.TEXT_FAINT : Theme.accent());
    }

    private void drawList(DrawContext ctx, int mouseX, int mouseY) {
        List<Module> modules = filtered();
        int columns = 2;
        int cardW = (LIST_W - CARD_GAP) / columns;
        int rowHeight = CARD_H + CARD_GAP;
        int totalRows = (int) Math.ceil(modules.size() / (double) columns);
        int contentHeight = totalRows * rowHeight;
        double maxScroll = Math.max(0, contentHeight - listHeight);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));

        ctx.enableScissor(listX, listY, listX + LIST_W, listY + listHeight);
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int cardX = listX + (i % columns) * (cardW + CARD_GAP);
            int cardY = (int) Math.round(listY + (i / columns) * rowHeight - scroll);
            if (cardY + CARD_H < listY - 4 || cardY > listY + listHeight + 4) {
                continue;
            }
            drawCard(ctx, module, cardX, cardY, cardW, mouseX, mouseY);
        }

        if (maxScroll > 0) {
            int trackX = listX + LIST_W + 3;
            Render2D.rounded(ctx, trackX, listY, 2, listHeight, 1, 0x18FFFFFF);
            int thumbH = Math.max(24, (int) (listHeight * (listHeight / (double) contentHeight)));
            int thumbY = listY + (int) ((listHeight - thumbH) * (scroll / maxScroll));
            Render2D.rounded(ctx, trackX, thumbY, 2, thumbH, 1, Theme.alpha(Theme.accent(), 0.55f));
        }
        ctx.disableScissor();

        if (modules.isEmpty()) {
            String empty = query.isEmpty() ? "Nothing here yet." : "No module matches \"" + query + "\".";
            Render2D.text(ctx, textRenderer, empty, listX + 4, listY + 10, Theme.TEXT_FAINT);
        }
    }

    private void drawCard(DrawContext ctx, Module module, int x, int y, int w, int mouseX, int mouseY) {
        boolean over = inside(mouseX, mouseY, x, y, w, CARD_H);
        float glow = hover.getOrDefault("card" + module.name(), 0.0f);
        hover.put("card" + module.name(), approach(glow, over ? 1.0f : 0.0f, 0.16f));
        boolean active = module == selected;
        boolean enabled = module.isEnabled();

        int base = enabled ? Theme.mix(Theme.CARD, Theme.accentDeep(), 0.16f) : Theme.CARD;
        Render2D.rounded(ctx, x, y, w, CARD_H, 6, base);
        Render2D.rounded(ctx, x, y, w, CARD_H, 6, Theme.alpha(0xFFFFFF, 0.028f + 0.05f * hover.getOrDefault("card" + module.name(), 0.0f) + (active ? 0.03f : 0f)));
        if (enabled && !active) {
            Render2D.rounded(ctx, x, y + 7, 2, CARD_H - 14, 1, Theme.alpha(Theme.accent(), 0.55f));
        }
        if (active) {
            Render2D.rounded(ctx, x, y, w, CARD_H, 6, Theme.alpha(Theme.accent(), 0.10f));
            Render2D.rounded(ctx, x, y, 2, CARD_H, 1, Theme.accent());
        }

        Render2D.rounded(ctx, x + 8, y + CARD_H / 2 - 3, 6, 6, 3, enabled ? Theme.accent() : 0x33FFFFFF);
        int nameColor = enabled ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, hover.getOrDefault("card" + module.name(), 0.0f));
        Render2D.text(ctx, textRenderer, module.name(), x + 20, y + 9, nameColor);

        String key = keyName(module.getKey());
        if (!key.isEmpty()) {
            int keyW = Render2D.width(textRenderer, key) + 8;
            int keyX = x + w - keyW - 7;
            Render2D.rounded(ctx, keyX, y + 7, keyW, 12, 3, 0x22FFFFFF);
            Render2D.text(ctx, textRenderer, key, keyX + 4, y + 10, Theme.TEXT_DIM);
        } else if (enabled) {
            Render2D.rounded(ctx, x + w - 11, y + 10, 5, 5, 2, Theme.accent());
        }
    }

    private void drawSettings(DrawContext ctx, int mouseX, int mouseY) {
        Render2D.rounded(ctx, settingsX, settingsY, SETTINGS_W, listHeight, 8, Theme.PANEL_INNER);
        Render2D.rounded(ctx, settingsX, settingsY, SETTINGS_W, listHeight, 8, Theme.alpha(0xFFFFFF, 0.02f));
        Render2D.text(ctx, textRenderer, "SETTINGS", settingsX + 12, settingsY + 10, Theme.TEXT_FAINT);

        if (selected == null) {
            Render2D.text(ctx, textRenderer, "Pick a module", settingsX + 12, settingsY + 28, Theme.TEXT_DIM);
            return;
        }

        Render2D.text(ctx, textRenderer, selected.name(), settingsX + 12, settingsY + 24, Theme.TEXT);

        boolean enabled = selected.isEnabled();
        int pillX = settingsX + SETTINGS_W - 40;
        int pillY = settingsY + 22;
        Render2D.rounded(ctx, pillX, pillY, 26, 13, 6, enabled ? Theme.alpha(Theme.accent(), 0.85f) : 0x38FFFFFF);
        Render2D.circle(ctx, enabled ? pillX + 19 : pillX + 7, pillY + 6, 4, enabled ? 0xFF0B0B13 : Theme.TEXT_DIM);

        int contentTop = settingsY + CONTENT_TOP;
        int contentBottom = settingsY + listHeight - BIND_ZONE;
        int contentHeight = contentBottom - contentTop;
        double maxScroll = Math.max(0, settingsHeight() - contentHeight);
        settingsTarget = Math.max(0, Math.min(maxScroll, settingsTarget));

        ctx.enableScissor(settingsX + 2, contentTop, settingsX + SETTINGS_W - 2, contentBottom);
        int cursorY = contentTop - (int) Math.round(settingsScroll);

        for (String line : wrap(selected.description(), SETTINGS_W - 24)) {
            Render2D.text(ctx, textRenderer, line, settingsX + 12, cursorY, Theme.TEXT_FAINT);
            cursorY += 10;
        }
        cursorY += 6;
        ctx.fill(settingsX + 12, cursorY, settingsX + SETTINGS_W - 12, cursorY + 1, 0x14FFFFFF);
        cursorY += 9;

        for (Setting<?> setting : selected.settings()) {
            if (!setting.isVisible()) {
                continue;
            }
            if (setting.isNumber()) {
                NumberSetting number = (NumberSetting) setting;
                Render2D.text(ctx, textRenderer, setting.name(), settingsX + 12, cursorY, Theme.TEXT_DIM);
                String display = number.display();
                Render2D.text(ctx, textRenderer, display, settingsX + SETTINGS_W - 12 - Render2D.width(textRenderer, display), cursorY, Theme.TEXT);
                int trackY = cursorY + 15;
                int trackW = SETTINGS_W - 24;
                Render2D.rounded(ctx, settingsX + 12, trackY, trackW, 3, 1, 0x22FFFFFF);
                int fill = (int) Math.round(trackW * Math.max(0.0, Math.min(1.0, number.fraction())));
                Render2D.rounded(ctx, settingsX + 12, trackY, Math.max(2, fill), 3, 1, Theme.accent());
                Render2D.circle(ctx, settingsX + 12 + fill, trackY + 1, 3, Theme.TEXT);
                cursorY += 25;
            } else if (setting.isBoolean()) {
                boolean value = (Boolean) setting.value();
                Render2D.text(ctx, textRenderer, setting.name(), settingsX + 12, cursorY, Theme.TEXT_DIM);
                int boxX = settingsX + SETTINGS_W - 34;
                Render2D.rounded(ctx, boxX, cursorY - 1, 22, 11, 5, value ? Theme.alpha(Theme.accent(), 0.9f) : 0x28FFFFFF);
                Render2D.circle(ctx, value ? boxX + 16 : boxX + 6, cursorY + 4, 3, value ? 0xFF0B0B13 : Theme.TEXT_DIM);
                cursorY += 18;
            } else if (setting.isMode()) {
                Render2D.text(ctx, textRenderer, setting.name(), settingsX + 12, cursorY, Theme.TEXT_DIM);
                String display = setting.display();
                int chipW = Render2D.width(textRenderer, display) + 12;
                int chipX = settingsX + SETTINGS_W - 12 - chipW;
                Render2D.rounded(ctx, chipX, cursorY - 2, chipW, 13, 4, Theme.alpha(Theme.accent(), 0.16f));
                Render2D.text(ctx, textRenderer, display, chipX + 6, cursorY + 1, Theme.accent());
                cursorY += 18;
            }
        }
        ctx.disableScissor();

        int bindY = settingsY + listHeight - 24;
        ctx.fill(settingsX + 12, bindY - 9, settingsX + SETTINGS_W - 12, bindY - 8, 0x14FFFFFF);
        Render2D.text(ctx, textRenderer, "Bind", settingsX + 12, bindY, Theme.TEXT_DIM);
        String keyLabel = binding == selected ? "..." : keyName(selected.getKey());
        if (keyLabel.isEmpty()) {
            keyLabel = "None";
        }
        int chipW = Render2D.width(textRenderer, keyLabel) + 12;
        int chipX = settingsX + SETTINGS_W - 12 - chipW;
        boolean bindHover = inside(mouseX, mouseY, chipX, bindY - 3, chipW, 13);
        Render2D.rounded(ctx, chipX, bindY - 3, chipW, 13, 4, bindHover ? Theme.alpha(Theme.accent(), 0.28f) : 0x22FFFFFF);
        Render2D.text(ctx, textRenderer, keyLabel, chipX + 6, bindY, binding == selected ? Theme.accent() : Theme.TEXT);
    }

    private int settingsHeight() {
        int height = 0;
        for (String ignored : wrap(selected.description(), SETTINGS_W - 24)) {
            height += 10;
        }
        height += 15;
        for (Setting<?> setting : selected.settings()) {
            if (!setting.isVisible()) {
                continue;
            }
            height += setting.isNumber() ? 25 : 18;
        }
        return height + 4;
    }

    private void drawFooter(DrawContext ctx) {
        int y = panelY + PANEL_H - FOOTER_H + 10;
        int cursor = panelX + 20;
        cursor = drawHint(ctx, cursor, y, "R-SHIFT", "toggle");
        drawHint(ctx, cursor + 12, y, "drag", "sliders");

        String user = mc.getSession().getUsername();
        String right = user + "  \u2022  " + enabledCount() + " active";
        Render2D.text(ctx, textRenderer, right, panelX + PANEL_W - 20 - Render2D.width(textRenderer, right), y, Theme.TEXT_FAINT);
    }

    private int drawHint(DrawContext ctx, int x, int y, String key, String label) {
        int keyW = Render2D.width(textRenderer, key) + 10;
        Render2D.rounded(ctx, x, y - 2, keyW, 13, 4, 0x24FFFFFF);
        Render2D.text(ctx, textRenderer, key, x + 5, y + 1, Theme.TEXT);
        Render2D.text(ctx, textRenderer, label, x + keyW + 6, y + 1, Theme.TEXT_FAINT);
        return x + keyW + 6 + Render2D.width(textRenderer, label);
    }

    private int enabledCount() {
        int count = 0;
        for (Module module : Veil.modules().all()) {
            if (module.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private List<Module> filtered() {
        List<Module> result = new ArrayList<>();
        String needle = query.toLowerCase();
        for (Module module : Veil.modules().byCategory(category)) {
            if (needle.isEmpty() || module.name().toLowerCase().contains(needle)) {
                result.add(module);
            }
        }
        return result;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = toLocalX(click.x());
        double my = toLocalY(click.y());
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        int searchW = 168;
        int searchX = panelX + PANEL_W - searchW - 20;
        int searchY = panelY + 15;
        if (inside(mx, my, searchX, searchY, searchW, 22)) {
            searching = true;
            return true;
        }
        searching = false;

        int tabY = panelY + HEADER_H + 4;
        int cursor = panelX + 20;
        for (Category value : Category.values()) {
            int tabW = Render2D.width(textRenderer, value.label()) + 26;
            if (inside(mx, my, cursor, tabY, tabW, 23)) {
                category = value;
                List<Module> modules = Veil.modules().byCategory(value);
                selected = modules.isEmpty() ? null : modules.get(0);
                scroll = 0;
                scrollTarget = 0;
                settingsScroll = 0;
                settingsTarget = 0;
                tabIndicator = -1.0f;
                return true;
            }
            cursor += tabW + 6;
        }

        List<Module> modules = filtered();
        int columns = 2;
        int cardW = (LIST_W - CARD_GAP) / columns;
        int rowHeight = CARD_H + CARD_GAP;
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int cardX = listX + (i % columns) * (cardW + CARD_GAP);
            int cardY = (int) Math.round(listY + (i / columns) * rowHeight - scroll);
            if (!inside(mx, my, cardX, cardY, cardW, CARD_H)) {
                continue;
            }
            if (cardY + CARD_H < listY || cardY > listY + listHeight) {
                continue;
            }
            String key = keyName(module.getKey());
            if (!key.isEmpty()) {
                int keyW = Render2D.width(textRenderer, key) + 16;
                if (mx >= cardX + cardW - keyW) {
                    binding = module;
                    return true;
                }
            }
            selected = module;
            module.toggle();
            Veil.config().save(Veil.modules());
            return true;
        }

        if (selected != null && inside(mx, my, settingsX, settingsY, SETTINGS_W, listHeight)) {
            int pillX = settingsX + SETTINGS_W - 40;
            if (inside(mx, my, pillX, settingsY + 22, 26, 13)) {
                selected.toggle();
                Veil.config().save(Veil.modules());
                return true;
            }

            int bindY = settingsY + listHeight - 24;
            String keyLabel = keyName(selected.getKey());
            int chipW = Render2D.width(textRenderer, keyLabel.isEmpty() ? "None" : keyLabel) + 12;
            if (inside(mx, my, settingsX + SETTINGS_W - 12 - chipW, bindY - 3, chipW, 13)) {
                binding = selected;
                return true;
            }

            Setting<?> row = rowAt(mx, my);
            if (row != null) {
                if (row.isBoolean()) {
                    row.toggle();
                    Veil.config().save(Veil.modules());
                    return true;
                }
                if (row.isMode()) {
                    row.cycle();
                    Veil.config().save(Veil.modules());
                    return true;
                }
                if (row.isNumber()) {
                    draggingSlider = true;
                    dragSetting = row;
                    row.drag((mx - (settingsX + 12)) / (SETTINGS_W - 24));
                    Veil.config().save(Veil.modules());
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingSlider && dragSetting != null) {
            double mx = toLocalX(click.x());
            dragSetting.drag((mx - (settingsX + 12)) / (SETTINGS_W - 24));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSlider) {
            draggingSlider = false;
            dragSetting = null;
            Veil.config().save(Veil.modules());
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double mx = toLocalX(mouseX);
        double my = toLocalY(mouseY);
        if (inside(mx, my, listX, listY, LIST_W, listHeight)) {
            scrollTarget -= vertical * 22.0;
            return true;
        }
        if (selected != null && inside(mx, my, settingsX, settingsY, SETTINGS_W, listHeight)) {
            settingsTarget -= vertical * 18.0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (binding != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                binding.setKey(0);
            } else {
                binding.setKey(key);
            }
            binding = null;
            Veil.config().save(Veil.modules());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searching) {
                searching = false;
                query = "";
                return true;
            }
            close();
            return true;
        }
        if (searching && key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            }
            return true;
        }
        if (searching && key == GLFW.GLFW_KEY_ENTER) {
            searching = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searching && input.isValidChar()) {
            if (query.length() < 24) {
                query += input.asString();
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        Veil.config().save(Veil.modules());
        super.close();
    }

    private Setting<?> rowAt(double mouseX, double mouseY) {
        int contentTop = settingsY + CONTENT_TOP;
        int contentBottom = settingsY + listHeight - BIND_ZONE;
        if (mouseY < contentTop || mouseY > contentBottom) {
            return null;
        }
        int cursorY = contentTop - (int) Math.round(settingsScroll);
        for (String ignored : wrap(selected.description(), SETTINGS_W - 24)) {
            cursorY += 10;
        }
        cursorY += 15;
        for (Setting<?> setting : selected.settings()) {
            if (!setting.isVisible()) {
                continue;
            }
            int height = setting.isNumber() ? 25 : 18;
            if (mouseY >= cursorY - 2 && mouseY <= cursorY + height - 4) {
                return setting;
            }
            cursorY += height;
        }
        return null;
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (Render2D.width(textRenderer, candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.size() > 3) {
            return new ArrayList<>(lines.subList(0, 3));
        }
        return lines;
    }

    private String keyName(int key) {
        if (key == 0) {
            return "";
        }
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }

    private int toLocalX(double screenX) {
        double center = width / 2.0;
        return (int) Math.round((screenX - center) / uiScale + center);
    }

    private int toLocalY(double screenY) {
        double center = height / 2.0;
        return (int) Math.round((screenY - center) / uiScale + center);
    }

    private float hover(String key, boolean over, float delta) {
        float current = hover.getOrDefault(key, 0.0f);
        float next = approach(current, over ? 1.0f : 0.0f, delta * 8.0f);
        hover.put(key, next);
        return next;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float approach(float value, float target, float delta) {
        return value + (target - value) * Math.min(1.0f, delta);
    }

    private static float ease(float value) {
        return 1.0f - (1.0f - value) * (1.0f - value);
    }
}
