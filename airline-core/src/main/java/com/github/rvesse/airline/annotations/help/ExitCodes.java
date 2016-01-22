/**
 * Copyright (C) 2010-16 the original author or authors.
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
package com.github.rvesse.airline.annotations.help;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that indicates the exit codes section for a commands help
 * 
 * @author rvesse
 *
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ TYPE })
public @interface ExitCodes {

    /**
     * The exit codes that this command may return, meanings of the exit codes
     * may be given using the {@link #descriptions()} property. The data in
     * these two properties is collated based on array indices.
     * 
     * @return Array of exit codes
     */
    int[]codes() default {};

    /**
     * Descriptions of the meanings of the exit codes this command may return,
     * the exit codes are given by the {@link #codes()} property. The data in
     * these two properties is collated based on array indices.
     * 
     * @return
     */
    String[]descriptions() default {};
}
