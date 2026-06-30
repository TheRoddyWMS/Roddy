/*
 * Copyright (c) 2026 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or member as part of the public API — an element that Roddy
 * plugins (or other Roddy components) are meant to use. Anything NOT annotated
 * is internal and may change without notice.
 *
 * <p>Provided centrally by Roddy so plugins share one marker instead of each
 * redefining its own. Using it constrains a plugin to a Roddy version that
 * ships this annotation.
 *
 * <p>This is documentation only; it is not enforced. Groovy does not enforce
 * {@code private}, and all plugins share one class loader, so no in-JVM
 * boundary can be hard-enforced without a real module/artifact split. The
 * annotation declares the boundary; it does not lock it. See
 * {@code docs/developers/architecture/0002-provide-public-api-annotation-in-roddy.adoc}
 * for the rationale and the (deferred) enforcement options.
 *
 * <p>Applied at class level for types whose whole surface is public (value and
 * constant types), and at method/field level for classes that mix public API
 * with internal helpers.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface PublicApi {
}
