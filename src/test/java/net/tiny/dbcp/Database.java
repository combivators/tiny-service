package net.tiny.dbcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("Database")
@Test
@ExtendWith(DatabaseExtension.class)
public @interface Database {
    int port() default H2Engine.H2_PORT;
    String db()  default H2Engine.H2_DB;
    boolean clear() default true;
    boolean report() default false;
    String[] startScripts() default {};
    String[] stopScripts() default {};
    String logging() default "logging.properties";
}
