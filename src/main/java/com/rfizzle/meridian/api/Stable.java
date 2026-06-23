package com.rfizzle.meridian.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an element as part of Meridian's stable public API: safe for sibling mods and third
 * parties to compile against, stable across patch and minor versions of Meridian, with breaking
 * changes only in a major version bump (and a changelog entry naming the broken signature).
 *
 * <p>Local marker per the
 * <a href="https://github.com/rfizzle/concord/blob/master/API-STANDARD.md">Concord API
 * Standard</a> §2: each mod declares its own {@code com.rfizzle.<mod>.api.Stable} rather than
 * depending on a shared annotations jar. Internal classes that tooling might surface instead carry
 * {@link org.jetbrains.annotations.ApiStatus.Internal}.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Stable {
}
