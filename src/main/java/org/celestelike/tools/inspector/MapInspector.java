package org.celestelike.tools.inspector;

import com.badlogic.gdx.ApplicationAdapter;
import org.celestelike.tools.editor.MapEditorApp;

/**
 * Thin wrapper that keeps the legacy "inspector" launcher alive while delegating to the
 * new {@link MapEditorApp} implementation.
 */
public final class MapInspector extends ApplicationAdapter {

    private final MapEditorApp delegate = new MapEditorApp("assets/b.tsx");

    @Override
    public void create() {
        delegate.create();
    }

    @Override
    public void render() {
        delegate.render();
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}

