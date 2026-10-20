package de.starlightunit.wrapper.navigation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import de.starlightunit.wrapper.ui.SystemBarStyle;

public final class NativeNavigationController {
    private static final int TOP_BAR_DP = 52;
    private static final int BOTTOM_TABS_DP = 58;
    private static final int CONTEXT_TOOLBAR_DP = 46;

    public interface Host {
        void navigate(String target);
        void home();
        void reload();
        void back();
        void forward();
        boolean canGoBack();
        boolean canGoForward();
    }

    private static final class BoundItem {
        final String resolvedUrl;
        final TextView view;
        final NativeNavigationConfig.Item item;
        final boolean iconAbove;
        final boolean reserveIconSlot;

        BoundItem(String resolvedUrl, TextView view, NativeNavigationConfig.Item item,
                  boolean iconAbove, boolean reserveIconSlot) {
            this.resolvedUrl = resolvedUrl;
            this.view = view;
            this.item = item;
            this.iconAbove = iconAbove;
            this.reserveIconSlot = reserveIconSlot;
        }
    }

    private final Context context;
    private final SwipeRefreshLayout content;
    private final LinearLayout topBar;
    private final TextView sidebarLauncher;
    private final HorizontalScrollView bottomTabs;
    private final LinearLayout bottomTabsContent;
    private final LinearLayout contextualToolbar;
    private final FrameLayout sidebarOverlay;
    private final ScrollView sidebarPanel;
    private final LinearLayout sidebarContent;
    private final Host host;
    private final NativeNavigationConfig config;
    private final String startUrl;
    private final boolean topEnabled;
    private final boolean sidebarEnabled;
    private final boolean bottomEnabled;
    private final boolean contextualEnabled;
    private final int backgroundColor;
    private final int foregroundColor;
    private final int accentColor;
    private final List<BoundItem> navigationBindings = new ArrayList<>();
    private final NativeNavigationIcons icons;
    private TextView backAction;
    private TextView forwardAction;

    public NativeNavigationController(
            Context context,
            SwipeRefreshLayout content,
            LinearLayout topBar,
            TextView sidebarLauncher,
            HorizontalScrollView bottomTabs,
            LinearLayout bottomTabsContent,
            LinearLayout contextualToolbar,
            FrameLayout sidebarOverlay,
            ScrollView sidebarPanel,
            LinearLayout sidebarContent,
            Host host,
            String startUrl,
            String title,
            String itemsJson,
            boolean topEnabled,
            boolean sidebarEnabled,
            boolean bottomEnabled,
            boolean contextualEnabled,
            String backgroundColor,
            String foregroundColor,
            String accentColor
    ) {
        this.context = context;
        this.icons = new NativeNavigationIcons(context);
        this.content = content;
        this.topBar = topBar;
        this.sidebarLauncher = sidebarLauncher;
        this.bottomTabs = bottomTabs;
        this.bottomTabsContent = bottomTabsContent;
        this.contextualToolbar = contextualToolbar;
        this.sidebarOverlay = sidebarOverlay;
        this.sidebarPanel = sidebarPanel;
        this.sidebarContent = sidebarContent;
        this.host = host;
        this.startUrl = startUrl;
        this.config = NativeNavigationConfig.parse(title, itemsJson);
        this.topEnabled = topEnabled;
        this.sidebarEnabled = sidebarEnabled;
        this.bottomEnabled = bottomEnabled;
        this.contextualEnabled = contextualEnabled;
        this.backgroundColor = SystemBarStyle.parseRgb(backgroundColor, Color.rgb(2, 6, 17));
        this.foregroundColor = SystemBarStyle.parseRgb(foregroundColor, Color.WHITE);
        this.accentColor = SystemBarStyle.parseRgb(accentColor, Color.rgb(111, 199, 255));
        bind();
    }

    private void bind() {
        topBar.removeAllViews();
        bottomTabsContent.removeAllViews();
        contextualToolbar.removeAllViews();
        sidebarContent.removeAllViews();
        navigationBindings.clear();

        configureSurface(topBar);
        configureSurface(bottomTabs);
        configureSurface(contextualToolbar);
        sidebarPanel.setBackgroundColor(backgroundColor);

        buildTopBar();
        buildSidebar();
        buildBottomTabs();
        buildContextualToolbar();
        applyContentInsets();

        sidebarOverlay.setOnClickListener(v -> closeSidebar());
        sidebarPanel.setClickable(true);
        sidebarPanel.setOnClickListener(v -> {
        });
        sidebarLauncher.setOnClickListener(v -> openSidebar());
        sidebarLauncher.setTextColor(foregroundColor);
        sidebarLauncher.setBackgroundColor(backgroundColor);
        updateHistoryState(startUrl);
    }

    private void buildTopBar() {
        topBar.setVisibility(topEnabled ? View.VISIBLE : View.GONE);
        sidebarLauncher.setVisibility(sidebarEnabled && !topEnabled ? View.VISIBLE : View.GONE);
        if (!topEnabled) {
            return;
        }

        if (sidebarEnabled) {
            TextView menu = actionText("☰", 48);
            menu.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
            menu.setOnClickListener(v -> openSidebar());
            topBar.addView(menu);
        }

        TextView title = new TextView(context);
        title.setText(config.title.isEmpty() ? "Quantum App" : config.title);
        title.setTextColor(foregroundColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setSingleLine(true);
        title.setPadding(dp(12), 0, dp(12), 0);
        topBar.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));

        TextView home = actionText("⌂", 48);
        home.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        home.setOnClickListener(v -> host.home());
        topBar.addView(home);
    }

    private void buildSidebar() {
        if (!sidebarEnabled) {
            sidebarOverlay.setVisibility(View.GONE);
            return;
        }

        TextView heading = new TextView(context);
        heading.setText(config.title.isEmpty() ? "Navigation" : config.title);
        heading.setTextColor(accentColor);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(dp(12), dp(14), dp(12), dp(18));
        sidebarContent.addView(heading);

        if (config.items.isEmpty()) {
            addSidebarItem(new NativeNavigationConfig.Item("Home", startUrl), startUrl, false);
            return;
        }
        boolean reserveIconSlot = hasIcons(config.items.size());
        for (NativeNavigationConfig.Item item : config.items) {
            String resolved = NativeNavigationConfig.resolveTarget(startUrl, item.target);
            if (!resolved.isEmpty()) {
                addSidebarItem(item, resolved, reserveIconSlot);
            }
        }
    }

    private void addSidebarItem(NativeNavigationConfig.Item configItem, String resolvedUrl,
                                boolean reserveIconSlot) {
        TextView item = new TextView(context);
        item.setText(configItem.label);
        item.setContentDescription(configItem.label);
        item.setTextColor(foregroundColor);
        item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(14), dp(14), dp(14));
        item.setCompoundDrawablePadding(reserveIconSlot ? dp(12) : 0);
        item.setMinimumHeight(dp(50));
        item.setFocusable(true);
        item.setOnClickListener(v -> {
            closeSidebar();
            host.navigate(resolvedUrl);
        });
        sidebarContent.addView(item, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        navigationBindings.add(new BoundItem(resolvedUrl, item, configItem, false, reserveIconSlot));
    }

    private void buildBottomTabs() {
        bottomTabs.setVisibility(bottomEnabled ? View.VISIBLE : View.GONE);
        if (!bottomEnabled) {
            return;
        }

        if (config.items.isEmpty()) {
            addBottomTab(new NativeNavigationConfig.Item("Home", startUrl), startUrl, false);
            return;
        }

        int count = Math.min(5, config.items.size());
        boolean reserveIconSlot = hasIcons(count);
        for (int i = 0; i < count; i++) {
            NativeNavigationConfig.Item item = config.items.get(i);
            String resolved = NativeNavigationConfig.resolveTarget(startUrl, item.target);
            if (!resolved.isEmpty()) {
                addBottomTab(item, resolved, reserveIconSlot);
            }
        }
    }

    private void addBottomTab(NativeNavigationConfig.Item configItem, String resolvedUrl,
                              boolean reserveIconSlot) {
        TextView tab = new TextView(context);
        tab.setText(configItem.label);
        tab.setContentDescription(configItem.label);
        tab.setTextColor(foregroundColor);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine(true);
        tab.setEllipsize(TextUtils.TruncateAt.END);
        tab.setCompoundDrawablePadding(reserveIconSlot ? dp(3) : 0);
        tab.setPadding(dp(reserveIconSlot ? 8 : 14), 0, dp(reserveIconSlot ? 8 : 14), 0);
        tab.setFocusable(true);
        tab.setOnClickListener(v -> host.navigate(resolvedUrl));
        bottomTabsContent.addView(tab, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));
        navigationBindings.add(new BoundItem(resolvedUrl, tab, configItem, true, reserveIconSlot));
    }

    private boolean hasIcons(int count) {
        for (int i = 0; i < count; i++) {
            NativeNavigationConfig.Item item = config.items.get(i);
            if (NativeNavigationIconPolicy.hasIcon(item.icon, item.activeIcon)) {
                return true;
            }
        }
        return false;
    }

    private void buildContextualToolbar() {
        contextualToolbar.setVisibility(contextualEnabled ? View.VISIBLE : View.GONE);
        if (!contextualEnabled) {
            return;
        }

        backAction = contextualAction("‹", v -> host.back());
        TextView home = contextualAction("⌂", v -> host.home());
        TextView reload = contextualAction("↻", v -> host.reload());
        forwardAction = contextualAction("›", v -> host.forward());

        contextualToolbar.addView(backAction);
        contextualToolbar.addView(home);
        contextualToolbar.addView(reload);
        contextualToolbar.addView(forwardAction);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) contextualToolbar.getLayoutParams();
        params.bottomMargin = bottomEnabled ? dp(BOTTOM_TABS_DP) : 0;
        contextualToolbar.setLayoutParams(params);
        updateHistoryState(null);
    }

    private TextView contextualAction(String label, View.OnClickListener listener) {
        TextView view = actionText(label, 0);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        view.setOnClickListener(listener);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        ));
        return view;
    }

    private TextView actionText(String label, int widthDp) {
        TextView view = new TextView(context);
        view.setText(label);
        view.setTextColor(foregroundColor);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                widthDp > 0 ? dp(widthDp) : LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        view.setLayoutParams(params);
        return view;
    }

    private void configureSurface(View view) {
        view.setBackgroundColor(backgroundColor);
    }

    private void applyContentInsets() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
        params.topMargin = topEnabled ? dp(TOP_BAR_DP) : 0;
        params.bottomMargin = (bottomEnabled ? dp(BOTTOM_TABS_DP) : 0)
                + (contextualEnabled ? dp(CONTEXT_TOOLBAR_DP) : 0);
        content.setLayoutParams(params);
    }

    public void updateHistoryState(String currentUrl) {
        if (backAction != null) {
            setActionEnabled(backAction, host.canGoBack());
        }
        if (forwardAction != null) {
            setActionEnabled(forwardAction, host.canGoForward());
        }

        if (currentUrl == null) {
            return;
        }
        for (BoundItem binding : navigationBindings) {
            boolean selected = sameLocation(binding.resolvedUrl, currentUrl);
            int color = selected ? accentColor : foregroundColor;
            binding.view.setSelected(selected);
            binding.view.setTextColor(color);
            binding.view.setTypeface(
                    Typeface.DEFAULT,
                    selected ? Typeface.BOLD : Typeface.NORMAL
            );
            Drawable icon = icons.forState(binding.item.icon, binding.item.activeIcon, selected,
                    color, dp(22), binding.reserveIconSlot);
            if (binding.iconAbove) {
                binding.view.setCompoundDrawablesRelative(null, icon, null, null);
            } else {
                binding.view.setCompoundDrawablesRelative(icon, null, null, null);
            }
        }
    }

    private void setActionEnabled(TextView view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.35f);
    }

    private static boolean sameLocation(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        int leftHash = left.indexOf('#');
        int rightHash = right.indexOf('#');
        String a = leftHash >= 0 ? left.substring(0, leftHash) : left;
        String b = rightHash >= 0 ? right.substring(0, rightHash) : right;
        return a.equals(b);
    }

    public boolean handleBackPressed() {
        if (sidebarOverlay.getVisibility() == View.VISIBLE) {
            closeSidebar();
            return true;
        }
        return false;
    }

    public void openSidebar() {
        if (sidebarEnabled) {
            sidebarOverlay.setVisibility(View.VISIBLE);
        }
    }

    public void closeSidebar() {
        sidebarOverlay.setVisibility(View.GONE);
    }

    public void destroy() {
        closeSidebar();
        sidebarOverlay.setOnClickListener(null);
        sidebarPanel.setOnClickListener(null);
        sidebarLauncher.setOnClickListener(null);
        topBar.removeAllViews();
        bottomTabsContent.removeAllViews();
        contextualToolbar.removeAllViews();
        sidebarContent.removeAllViews();
        navigationBindings.clear();
        icons.clear();
        backAction = null;
        forwardAction = null;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
