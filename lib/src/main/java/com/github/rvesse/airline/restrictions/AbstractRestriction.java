/**
 * Copyright (C) 2010-15 the original author or authors.
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
package com.github.rvesse.airline.restrictions;

import com.github.rvesse.airline.model.ArgumentsMetadata;
import com.github.rvesse.airline.model.OptionMetadata;
import com.github.rvesse.airline.parser.ParseState;

public abstract class AbstractRestriction implements GlobalRestriction, OptionRestriction, ArgumentsRestriction {

    @Override
    public <T> void validate(ParseState<T> state) {
        // Does no validation
    }

    @Override
    public <T> void postValidate(ParseState<T> state, OptionMetadata option) {
        // Does no validation
    }
    
    @Override
    public <T> void preValidate(ParseState<T> state, OptionMetadata option, String value) {
        // Does no validation
    }

    @Override
    public <T> void preValidate(ParseState<T> state, ArgumentsMetadata arguments, String value) {
        // Does no validation
    }

    @Override
    public <T> void postValidate(ParseState<T> state, ArgumentsMetadata arguments) {
        // Does no validation
    }
}
