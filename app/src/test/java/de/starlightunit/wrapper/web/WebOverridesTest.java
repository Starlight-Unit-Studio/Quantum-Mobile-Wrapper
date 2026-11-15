package de.starlightunit.wrapper.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class WebOverridesTest {

    @Test
    public void cssInjectionEscapesContentAndUsesStableStyleElement() {
        String script = WebOverrides.cssInjectionScript("body { color: red; }\n.x { display: block; }");
        assertTrue(script.contains("quantum-profile-custom-css"));
        assertTrue(script.contains("body { color: red; }\\n.x { display: block; }"));
        assertTrue(script.contains("s.textContent="));
    }

    @Test
    public void themeInjectionExposesResolvedThemeContract() {
        String dark = WebOverrides.themeInjectionScript("dark");
        assertTrue(dark.contains("mode='dark'"));
        assertTrue(dark.contains("root.dataset.quantumTheme=resolved"));
        assertTrue(dark.contains("root.style.colorScheme=resolved"));

        String auto = WebOverrides.themeInjectionScript("auto");
        assertTrue(auto.contains("prefers-color-scheme: dark"));
    }

    @Test
    public void javascriptInjectionPreservesSourceAndAddsDebugSourceName() {
        String source = "window.quantumTest = (window.quantumTest || 0) + 1;";
        String script = WebOverrides.javascriptInjectionScript(source);
        assertTrue(script.startsWith(source));
        assertTrue(script.endsWith("//# sourceURL=quantum-profile-custom.js"));
    }

    @Test
    public void nullJavascriptStillProducesValidDebugSource() {
        assertEquals("\n//# sourceURL=quantum-profile-custom.js", WebOverrides.javascriptInjectionScript(null));
    }
}
