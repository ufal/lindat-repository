/**
 * Copyright 2012 Lyncode
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

package com.lyncode.xoai.dataprovider.exceptions;

/**
 * @author Development @ Lyncode <development@lyncode.com>
 * @version 3.1.0
 */
public class NoMetadataFormatsException extends HandlerException {

    /**
     *
     */
    private static final long serialVersionUID = 7091872607176190034L;

    /**
     * Creates a new instance of <code>NoMetadataFormatsException</code> without
     * detail message.
     */
    public NoMetadataFormatsException() {
    }

    /**
     * Constructs an instance of <code>NoMetadataFormatsException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public NoMetadataFormatsException(String msg) {
        super(msg);
    }
}
