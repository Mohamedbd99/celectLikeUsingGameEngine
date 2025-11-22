package org.celestelike.tools.editor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small thread-safe bridge used by the map window and the detached palette window.
 */
final class PaletteSharedState {

    static final int NO_REQUEST = Integer.MIN_VALUE;

    private final AtomicInteger currentSelection = new AtomicInteger(-1);
    private final AtomicInteger pendingSelection = new AtomicInteger(NO_REQUEST);

    public void updateCurrentSelection(int tileIndex) {
        currentSelection.set(tileIndex);
    }

    public int currentSelection() {
        return currentSelection.get();
    }

    public void requestSelection(int tileIndex) {
        pendingSelection.set(tileIndex);
    }

    public int consumeRequestedSelection() {
        return pendingSelection.getAndSet(NO_REQUEST);
    }
}

