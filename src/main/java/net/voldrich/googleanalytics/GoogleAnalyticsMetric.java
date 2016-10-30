package net.voldrich.googleanalytics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation value defines which metric (eg ga:users) is requested and set to the field value.
 * Take care which metrics you put together to the same class as google analytics does not allow certain combinations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface GoogleAnalyticsMetric {
    /**
     * Defines which metric (eg ga:users) is requested and set to the field value
     */
    String value();
}
