package de.starlightunit.wrapper.diagnostics;

import android.app.Application;

public final class QuantumDiagnosticApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StartupDiagnostics.install(this);
    }
}
