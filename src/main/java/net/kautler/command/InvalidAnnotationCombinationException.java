/*
 * Copyright 2019-2020 Björn Kautler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kautler.command;

import net.kautler.command.api.annotation.RestrictedTo;
import net.kautler.command.api.annotation.RestrictionPolicy;

/**
 * An exception that is thrown if an invalid annotation combination is detected like for example multiple
 * {@link RestrictedTo @RestrictedTo} annotations without a {@link RestrictionPolicy @RestrictionPolicy} annotation.
 */
public class InvalidAnnotationCombinationException extends RuntimeException {
    /**
     * The serial version UID of this class.
     */
    private static final long serialVersionUID = 1;

    /**
     * Constructs a new invalid annotation combination exception with the given message.
     *
     * @param message the detail message
     */
    public InvalidAnnotationCombinationException(String message) {
        super(message);
    }
}
