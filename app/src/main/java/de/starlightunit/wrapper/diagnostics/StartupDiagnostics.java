package de.starlightunit.wrapper.diagnostics;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class StartupDiagnostics {

    private static final String FILE_NAME = "quantum_startup_crash.txt";
    private static volatile boolean installed;

    private StartupDiagnostics() {
    }

    public static synchronized void install(Context context) {
        if (installed) {
            return;
        }
        installed = true;

        Context appContext = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                writeCrash(appContext, thread, throwable);
            } catch (RuntimeException ignored) {
                // Never mask the original process crash.
            }

            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    public static String read(Context context) {
        File file = file(context);
        if (!file.isFile()) {
            return "No persisted Java crash has been captured yet.";
        }
        try {
            return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "Could not read persisted crash log: " + exception;
        }
    }

    public static boolean hasCrash(Context context) {
        return file(context).isFile();
    }

    public static void clear(Context context) {
        File file = file(context);
        if (file.exists()) {
            file.delete();
        }
    }

    public static String deviceSummary() {
        return "Android " + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")\n"
                + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "ABI: " + String.join(", ", Build.SUPPORTED_ABIS);
    }

    public static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static void writeCrash(Context context, Thread thread, Throwable throwable) {
        File file = file(context);
        try (FileOutputStream output = new FileOutputStream(file, false);
             PrintWriter writer = new PrintWriter(output, true, StandardCharsets.UTF_8)) {
            writer.println("Quantum Mobile Wrapper startup diagnostics");
            writer.println("Captured: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date()));
            writer.println("Thread: " + thread.getName());
            writer.println(deviceSummary());
            writer.println();
            throwable.printStackTrace(writer);
        } catch (IOException ignored) {
            // The original exception remains authoritative.
        }
    }

    private static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }
}
