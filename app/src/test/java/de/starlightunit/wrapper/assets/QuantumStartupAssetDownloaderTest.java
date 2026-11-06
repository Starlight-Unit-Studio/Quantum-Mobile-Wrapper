package de.starlightunit.wrapper.assets;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class QuantumStartupAssetDownloaderTest {

    @SuppressWarnings("unchecked")
    @Test
    public void rootsAreNormalizedAndUnsafeEntriesAreDropped() throws Exception {
        Method method = QuantumStartupAssetDownloader.class.getDeclaredMethod("parseRoots", String.class);
        method.setAccessible(true);
        List<String> roots = (List<String>) method.invoke(null, "assets/portraits; /assets/bg/\n../bad");
        assertEquals(2, roots.size());
        assertEquals("/assets/portraits/", roots.get(0));
        assertEquals("/assets/bg/", roots.get(1));
    }

    @Test
    public void emptyRootsAreAcceptedForGenericManifestUse() throws Exception {
        Method method = QuantumStartupAssetDownloader.class.getDeclaredMethod("parseRoots", String.class);
        method.setAccessible(true);
        List<?> roots = (List<?>) method.invoke(null, "");
        assertTrue(roots.isEmpty());
    }
}
