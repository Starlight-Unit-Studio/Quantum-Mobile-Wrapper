package de.starlightunit.wrapper.navigation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
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
        final View selectionView;

        BoundItem(String resolvedUrl, TextView view, NativeNavigationConfig.Item item,
                  boolean iconAbove, boolean reserveIconSlot) {
            this(resolvedUrl, view, item, iconAbove, reserveIconSlot, view);
        }

        BoundItem(String resolvedUrl, TextView view, NativeNavigationConfig.Item item,
                  boolean iconAbove, boolean reserveIconSlot, View selectionView) {
            this.resolvedUrl = resolvedUrl;
            this.view = view;
            this.item = item;
            this.iconAbove = iconAbove;
            this.reserveIconSlot = reserveIconSlot;
            this.selectionView = selectionView;
        }
    }

    private static final class BoundGroup {
        final NativeNavigationConfig.Item item;
        final LinearLayout header;
        final TextView arrow;
        final LinearLayout children;
        Boolean expanded;

        BoundGroup(NativeNavigationConfig.Item item, LinearLayout header,
                   TextView arrow, LinearLayout children) {
            this.item = item;
            this.header = header;
            this.arrow = arrow;
            this.children = children;
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
    private final List<BoundGroup> groupBindings = new ArrayList<>();
    private final NativeNavigationIcons icons;
    private final NativeNavigationGroups groups;
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
        this.groups = new NativeNavigationGroups(config, startUrl);
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
        groupBindings.clear();

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

        boolean reserveIconSlot = NativeNavigationGroups.hasIcons(config.tree);
        addSidebarNodes(sidebarContent, config.tree, 0, reserveIconSlot);
        if (config.items.isEmpty()) {
            addSidebarItem(sidebarContent, new NativeNavigationConfig.Item("Home", startUrl),
                    startUrl, reserveIconSlot, 0);
        }
        updateGroupVisibility();
    }

    private void addSidebarNodes(LinearLayout parent, List<NativeNavigationConfig.Item> nodes,
                                 int depth, boolean reserveIconSlot) {
        for (NativeNavigationConfig.Item item : nodes) {
            if (item.isGroup) {
                addSidebarGroup(parent, item, depth, reserveIconSlot);
            } else {
                String resolved = NativeNavigationConfig.resolveTarget(startUrl, item.target);
                if (!resolved.isEmpty()) addSidebarItem(parent, item, resolved, reserveIconSlot, depth);
            }
        }
    }

    private void addSidebarGroup(LinearLayout parent, NativeNavigationConfig.Item configItem,
                                int depth, boolean reserveIconSlot) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        parent.addView(container, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setMinimumHeight(dp(50));
        header.setPadding(dp(14 + 12 * depth), dp(14), dp(14), dp(14));
        header.setFocusable(true);
        header.setClickable(true);
        header.setTag("quantum-nav-group:" + configItem.id);
        TextView label = new TextView(context);
        label.setText(configItem.label);
        label.setTextColor(foregroundColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setCompoundDrawablePadding(reserveIconSlot ? dp(12) : 0);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = new TextView(context);
        arrow.setTextColor(foregroundColor);
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        arrow.setGravity(Gravity.CENTER);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(22),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        arrowParams.setMarginStart(dp(8));
        header.addView(arrow, arrowParams);
        container.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout children = new LinearLayout(context);
        children.setOrientation(LinearLayout.VERTICAL);
        container.addView(children, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        BoundGroup binding = new BoundGroup(configItem, header, arrow, children);
        groupBindings.add(binding);
        navigationBindings.add(new BoundItem("", label, configItem, false, reserveIconSlot, header));
        header.setOnClickListener(v -> changeGroup(binding, !groups.isExpanded(configItem.id)));
        header.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View hostView, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(hostView, info);
                info.setClassName("android.widget.Button");
                info.addAction(groups.isExpanded(configItem.id)
                        ? AccessibilityNodeInfo.ACTION_COLLAPSE : AccessibilityNodeInfo.ACTION_EXPAND);
            }

            @Override
            public boolean performAccessibilityAction(View hostView, int action, Bundle arguments) {
                if (action == AccessibilityNodeInfo.ACTION_EXPAND || action == AccessibilityNodeInfo.ACTION_COLLAPSE) {
                    changeGroup(binding, action == AccessibilityNodeInfo.ACTION_EXPAND);
                    return true;
                }
                return super.performAccessibilityAction(hostView, action, arguments);
            }
        });
        addSidebarNodes(children, configItem.children, depth + 1, reserveIconSlot);
    }

    private void changeGroup(BoundGroup binding, boolean expanded) {
        groups.setExpanded(binding.item.id, expanded);
        updateGroupVisibility();
        binding.header.announceForAccessibility(groupDescription(binding));
    }

    private String groupDescription(BoundGroup binding) {
        return binding.item.label + (groups.isExpanded(binding.item.id) ? ", expanded group" : ", collapsed group");
    }

    private void updateGroupVisibility() {
        for (BoundGroup binding : groupBindings) {
            boolean expanded = groups.isExpanded(binding.item.id);
            if (binding.expanded != null && binding.expanded == expanded) continue;
            binding.expanded = expanded;
            binding.children.setVisibility(expanded ? View.VISIBLE : View.GONE);
            binding.arrow.setText(expanded ? "▾" : "▸");
            binding.header.setContentDescription(groupDescription(binding));
            binding.header.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        }
    }

    private void addSidebarItem(LinearLayout parent, NativeNavigationConfig.Item configItem,
                               String resolvedUrl, boolean reserveIconSlot, int depth) {
        TextView item = new TextView(context);
        item.setText(configItem.label);
        item.setContentDescription(configItem.label);
        item.setTextColor(foregroundColor);
        item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14 + 12 * depth), dp(14), dp(14), dp(14));
        item.setCompoundDrawablePadding(reserveIconSlot ? dp(12) : 0);
        item.setMinimumHeight(dp(50));
        item.setFocusable(true);
        item.setOnClickListener(v -> {
            closeSidebar();
            host.navigate(resolvedUrl);
        });
        parent.addView(item, new LinearLayout.LayoutParams(
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
        groups.navigate(currentUrl);
        updateGroupVisibility();
        for (BoundItem binding : navigationBindings) {
            boolean selected = binding.item.isGroup
                    ? groups.isActive(binding.item, currentUrl)
                    : NativeNavigationGroups.sameLocation(binding.resolvedUrl, currentUrl);
            int color = selected ? accentColor : foregroundColor;
            binding.view.setSelected(selected);
            binding.selectionView.setSelected(selected);
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
        for (BoundGroup binding : groupBindings) {
            binding.arrow.setTextColor(groups.isActive(binding.item, currentUrl) ? accentColor : foregroundColor);
        }
    }

    private void setActionEnabled(TextView view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.35f);
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
        groupBindings.clear();
        icons.clear();
        backAction = null;
        forwardAction = null;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
