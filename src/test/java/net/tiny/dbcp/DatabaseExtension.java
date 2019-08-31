package net.tiny.dbcp;

import java.net.URL;
import java.util.NoSuchElementException;
import java.util.logging.LogManager;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class DatabaseExtension implements BeforeAllCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterAllCallback {

    private enum StoreKeyType {
        CLASS, TEST
    }

    private static final Namespace NAMESPACE = Namespace.create("net", "tiny", "DatabaseExtension");

    private static String lastLogging = "";
    private static boolean logged = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        logging(database.logging());
        H2Engine engine = H2Engine.getEngine(H2Engine.H2_DIR,
                database.port(), database.db(),
                database.startScripts(), database.stopScripts());
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.CLASS), engine);
        engine.start();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        H2Engine engine = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.CLASS), H2Engine.class);
        engine.clearDatabase(database.clear());
        engine.stop();
        if (database.report()) {
            report("Test Database", context, engine);
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        logging(database.logging());
        H2Engine engine = H2Engine.getEngine(H2Engine.H2_DIR,
                database.port(), database.db(),
                database.startScripts(), database.stopScripts());
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.TEST), engine);
        engine.start();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        Database database = getDatabaseAnnotation(context);
        if(null == database)
            return;
        H2Engine engine = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.TEST), H2Engine.class);
        engine.clearDatabase(database.clear());
        engine.stop();
        if (database.report()) {
            report("Test", context, engine);
        }
    }

    static void logging(String file) {
        if (logged && lastLogging.equals(file))
            return;
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL url = loader.getResource(file);
        if (null != url) {
            try {
                LogManager.getLogManager().readConfiguration(url.openStream());
                logged = true;
                lastLogging = file;
            } catch (Exception ignore) {
            }
        }
    }

    private static String getStoreKey(ExtensionContext context, StoreKeyType type) {
        String storedKey = type.name();
        switch(type) {
        case CLASS:
            storedKey = context.getRequiredTestClass().getName();
            break;
        case TEST:
            storedKey = context.getRequiredTestInstance().getClass().getSimpleName();
            storedKey = storedKey.concat(".")
                                 .concat(context.getRequiredTestMethod().getName());
            break;
        }
        return storedKey;
    }

    private static Database getDatabaseAnnotation(ExtensionContext context) {
        try {
            return context.getElement()
                    .filter(el -> el.isAnnotationPresent(Database.class))
                    .get()
                    .getAnnotation(Database.class);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private static void report(String unit, ExtensionContext context, H2Engine engine) {
        String message = "H2Engine report";
        context.publishReportEntry(unit, message);
    }
}
