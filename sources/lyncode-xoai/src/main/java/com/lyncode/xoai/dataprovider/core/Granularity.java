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

package com.lyncode.xoai.dataprovider.core;

import com.lyncode.xoai.dataprovider.xml.oaipmh.GranularityType;

/**
 * @author Development @ Lyncode <development@lyncode.com>
 * @version 3.1.0
 */
public enum Granularity {
    Day, Second;

    public GranularityType toGranularityType() {
        if (this == Day) return GranularityType.YYYY_MM_DD;
        else return GranularityType.YYYY_MM_DD_THH_MM_SS_Z;
    }
}
