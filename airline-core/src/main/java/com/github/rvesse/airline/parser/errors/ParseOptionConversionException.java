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
package com.github.rvesse.airline.parser.errors;

/**
 * Exception that is thrown when the argument supplied as the value for an
 * option cannot be converted to the options Java type
 * 
 */
public class ParseOptionConversionException extends ParseException {
    private static final long serialVersionUID = -9105701233341582179L;

    private final String optionTitle;
    private final String value;
    private final String typeName;

    public ParseOptionConversionException(String optionTitle, String value, String typeName) {
        this(String.format("%s: can not convert \"%s\" to a %s", optionTitle, value, typeName), optionTitle, value,
                typeName);
    }

    public ParseOptionConversionException(String message, String optionTitle, String value, String typeName) {
        super(message);
        this.optionTitle = optionTitle;
        this.value = value;
        this.typeName = typeName;
    }

    public String getOptionTitle() {
        return optionTitle;
    }

    public String getValue() {
        return value;
    }

    public String getTypeName() {
        return typeName;
    }
}
