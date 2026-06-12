package com.rfizzle.meridian.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stability markers for Meridian's public API, per the
 * <a href="https://github.com/rfizzle/concord/blob/master/API-STANDARD.md">Concord API
 * Standard</a>.
 *
 * <p>The standard requires {@code api} classes to carry {@code @ApiStatus.Stable}, but
 * {@code org.jetbrains.annotations.ApiStatus} ships no {@code Stable} member (only
 * {@code Internal}, {@code Experimental}, …), so the marker is declared here under the
 * standard's name. Internal classes keep using
 * {@link org.jetbrains.annotations.ApiStatus.Internal}.
 */
public final class ApiStatus {

    private ApiStatus() {
    }

    /**
     * Marks an element as part of Meridian's stable public API: safe for sibling mods and
     * third parties to compile against, stable across patch and minor versions of Meridian,
     * with breaking changes only in a major version bump (and a changelog entry naming the
     * broken signature).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
    public @interface Stable {
    }
}
