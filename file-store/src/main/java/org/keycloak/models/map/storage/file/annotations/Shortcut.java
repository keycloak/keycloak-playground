/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/AnnotationType.java to edit this template
 */

package org.keycloak.models.map.storage.file.annotations;

import org.keycloak.models.map.storage.file.entity.shortcut.ShortcutProcessor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author hmlnarik
 */
@Repeatable(Shortcuts.class)
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Shortcut {

    public String property() default "";

    /**
     * Name of the property in the file.
     * @return
     */
    public String name();

    /**
     * If the annotated method returns a {@code Map}, this denotes a key in that map that is represented by a shortcut
     * called as per {@link #name()} method. Only one of {@link key()} and {@link processor()} can be set.
     */
    public String key() default "";

    /**
     * Stateless shortcut processor class
     */
    public Class<? extends ShortcutProcessor> processor() default ShortcutProcessor.class;

}
