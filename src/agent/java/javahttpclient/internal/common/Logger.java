/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javahttpclient.internal.common;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * An internal {@code System.Logger} that is used for internal
 * debugging purposes in the {@link javahttpclient} module.
 * <p>
 * Though not enforced, this interface is designed for emitting
 * debug messages with Level.DEBUG.
 * <p>
 * It defines {@code log} methods that default to {@code Level.DEBUG},
 * so that they can be called in statements like:
 * <pre>{@code debug.log("some %s with %d %s", message(), one(), params());}</pre>
 *
 * @implSpec
 * This interface is implemented by loggers returned by
 * {@link Utils#getDebugLogger(Supplier)},
 * {@link Utils#getWebSocketLogger(Supplier)} and
 * {@link Utils#getHpackLogger(Supplier)}.
 * It is not designed to be implemented by any other
 * loggers. Do not use outside of this module.
 */
public interface Logger {
    public enum Level {

        // for convenience, we're reusing java.util.logging.Level int values
        // the mapping logic in sun.util.logging.PlatformLogger depends
        // on this.
        /**
         * A marker to indicate that all levels are enabled.
         * This level {@linkplain #getSeverity() severity} is
         * {@link Integer#MIN_VALUE}.
         */
        ALL(Integer.MIN_VALUE),  // typically mapped to/from j.u.l.Level.ALL
        /**
         * {@code TRACE} level: usually used to log diagnostic information.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 400}.
         */
        TRACE(400),   // typically mapped to/from j.u.l.Level.FINER
        /**
         * {@code DEBUG} level: usually used to log debug information traces.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 500}.
         */
        DEBUG(500),   // typically mapped to/from j.u.l.Level.FINEST/FINE/CONFIG
        /**
         * {@code INFO} level: usually used to log information messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 800}.
         */
        INFO(800),    // typically mapped to/from j.u.l.Level.INFO
        /**
         * {@code WARNING} level: usually used to log warning messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 900}.
         */
        WARNING(900), // typically mapped to/from j.u.l.Level.WARNING
        /**
         * {@code ERROR} level: usually used to log error messages.
         * This level {@linkplain #getSeverity() severity} is
         * {@code 1000}.
         */
        ERROR(1000),  // typically mapped to/from j.u.l.Level.SEVERE
        /**
         * A marker to indicate that all levels are disabled.
         * This level {@linkplain #getSeverity() severity} is
         * {@link Integer#MAX_VALUE}.
         */
        OFF(Integer.MAX_VALUE);  // typically mapped to/from j.u.l.Level.OFF

        private final int severity;

        Level(int severity) {
            this.severity = severity;
        }

        /**
         * Returns the name of this level.
         * @return this level {@linkplain #name()}.
         */
        public final String getName() {
            return name();
        }

        /**
         * Returns the severity of this level.
         * A higher severity means a more severe condition.
         * @return this level severity.
         */
        public final int getSeverity() {
            return severity;
        }
    }
	
    /**
     * Tells whether this logger is on.
     * @implSpec The default implementation for this method calls
     * {@code this.isLoggable(Level.DEBUG);}
     */
    default boolean on() {
        return isLoggable(Level.DEBUG);
    }

    /**
     * Logs a message.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, msg);}
     *
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * System.LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     */
    default void log(String msg) {
        log(Level.DEBUG, msg);
    }

    /**
     * Logs a lazily supplied message.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, msgSupplier);}
     *
     * @param msgSupplier a supplier function that produces a message.
     *
     * @throws NullPointerException if {@code msgSupplier} is {@code null}.
     */
    default void log(Supplier<String> msgSupplier) {
        log(Level.DEBUG, msgSupplier);
    }

    /**
     * Logs a message produced from the given object.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, obj);}
     *
     * @param obj the object to log.
     *
     * @throws NullPointerException if {@code obj} is {@code null}.
     */
    default void log(Object obj) {
        log(Level.DEBUG,  obj);
    }

    /**
     * Logs a message associated with a given throwable.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, msg, thrown);}
     *
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * System.LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param thrown a {@code Throwable} associated with the log message;
     *        can be {@code null}.
     */
    default void log(String msg, Throwable thrown) {
        this.log(Level.DEBUG, msg, thrown);
    }

    /**
     * Logs a lazily supplied message associated with a given throwable.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, msgSupplier, thrown);}
     *
     * @param msgSupplier a supplier function that produces a message.
     * @param thrown a {@code Throwable} associated with log message;
     *               can be {@code null}.
     *
     * @throws NullPointerException if {@code msgSupplier} is {@code null}.
     */
    default void log(Supplier<String> msgSupplier, Throwable thrown) {
        log(Level.DEBUG, msgSupplier, thrown);
    }

    /**
     * Logs a message with an optional list of parameters.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, format, params);}
     *
     * @param format the string message format in
     * {@link String#format(String, Object...)} or {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog, if this logger is a {@link
     * System.LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     */
    default void log(String format, Object... params) {
        log(Level.DEBUG, format, params);
    }
    

    /**
     * Returns the name of this logger.
     *
     * @return the logger name.
     */
    String getName();

    /**
     * Checks if a message of the given level would be logged by
     * this logger.
     *
     * @param level the log message level.
     * @return {@code true} if the given log message level is currently
     *         being logged.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    boolean isLoggable(Level level);

    /**
     * Logs a message.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msg, (Object[])null);}
     *
     * @param level the log message level.
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    default void log(Level level, String msg) {
        log(level, (ResourceBundle) null, msg, (Object[]) null);
    }

    /**
     * Logs a lazily supplied message.
     *
     * If the logger is currently enabled for the given log message level
     * then a message is logged that is the result produced by the
     * given supplier function.  Otherwise, the supplier is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msgSupplier.get(), (Object[])null);}
     *
     * @param level the log message level.
     * @param msgSupplier a supplier function that produces a message.
     *
     * @throws NullPointerException if {@code level} is {@code null},
     *         or {@code msgSupplier} is {@code null}.
     */
    default void log(Level level, Supplier<String> msgSupplier) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            log(level, (ResourceBundle) null, msgSupplier.get(), (Object[]) null);
        }
    }

    /**
     * Logs a message produced from the given object.
     *
     * If the logger is currently enabled for the given log message level then
     * a message is logged that, by default, is the result produced from
     * calling  toString on the given object.
     * Otherwise, the object is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, obj.toString(), (Object[])null);}
     *
     * @param level the log message level.
     * @param obj the object to log.
     *
     * @throws NullPointerException if {@code level} is {@code null}, or
     *         {@code obj} is {@code null}.
     */
    default void log(Level level, Object obj) {
        Objects.requireNonNull(obj);
        if (isLoggable(Objects.requireNonNull(level))) {
            this.log(level, (ResourceBundle) null, obj.toString(), (Object[]) null);
        }
    }

    /**
     * Logs a message associated with a given throwable.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msg, thrown);}
     *
     * @param level the log message level.
     * @param msg the string message (or a key in the message catalog, if
     * this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param thrown a {@code Throwable} associated with the log message;
     *        can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    default void log(Level level, String msg, Throwable thrown) {
        this.log(level, null, msg, thrown);
    }

    /**
     * Logs a lazily supplied message associated with a given throwable.
     *
     * If the logger is currently enabled for the given log message level
     * then a message is logged that is the result produced by the
     * given supplier function.  Otherwise, the supplier is not operated on.
     *
     * @implSpec When logging is enabled for the given level, the default
     * implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, msgSupplier.get(), thrown);}
     *
     * @param level one of the log message level identifiers.
     * @param msgSupplier a supplier function that produces a message.
     * @param thrown a {@code Throwable} associated with log message;
     *               can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}, or
     *                               {@code msgSupplier} is {@code null}.
     */
    default void log(Level level, Supplier<String> msgSupplier,
            Throwable thrown) {
        Objects.requireNonNull(msgSupplier);
        if (isLoggable(Objects.requireNonNull(level))) {
            this.log(level, null, msgSupplier.get(), thrown);
        }
    }

    /**
     * Logs a message with an optional list of parameters.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(level, (ResourceBundle)null, format, params);}
     *
     * @param level one of the log message level identifiers.
     * @param format the string message format in {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog, if this logger is a {@link
     * LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    default void log(Level level, String format, Object... params) {
        this.log(level, null, format, params);
    }

    /**
     * Logs a localized message associated with a given throwable.
     *
     * If the given resource bundle is non-{@code null},  the {@code msg}
     * string is localized using the given resource bundle.
     * Otherwise the {@code msg} string is not localized.
     *
     * @param level the log message level.
     * @param bundle a resource bundle to localize {@code msg}; can be
     * {@code null}.
     * @param msg the string message (or a key in the message catalog,
     *            if {@code bundle} is not {@code null}); can be {@code null}.
     * @param thrown a {@code Throwable} associated with the log message;
     *        can be {@code null}.
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    void log(Level level, ResourceBundle bundle, String msg,
            Throwable thrown);

    /**
     * Logs a message with resource bundle and an optional list of
     * parameters.
     *
     * If the given resource bundle is non-{@code null},  the {@code format}
     * string is localized using the given resource bundle.
     * Otherwise the {@code format} string is not localized.
     *
     * @param level the log message level.
     * @param bundle a resource bundle to localize {@code format}; can be
     * {@code null}.
     * @param format the string message format in {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog if {@code bundle} is not {@code null}); can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     *
     * @throws NullPointerException if {@code level} is {@code null}.
     */
    void log(Level level, ResourceBundle bundle, String format,
            Object... params);
}
