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
package com.lyncode.xoai.dataprovider.services.api;

import com.lyncode.xoai.dataprovider.core.DeleteMethod;
import com.lyncode.xoai.dataprovider.core.Granularity;

import java.util.Date;
import java.util.List;

/**
 * Base class (required extension) to identify the OAI interface.
 *
 * @author Development @ Lyncode <development@lyncode.com>
 * @version 3.1.0
 */
public interface RepositoryConfiguration {
    /**
     * Gets the name of the repository NOTE: This is just for identifying
     * purposes.
     *
     * @return Repository Name
     */
    public String getRepositoryName();

    /**
     * Gets the administrator emails. NOTE: This is just for identifying
     * purposes.
     *
     * @return Administrator emails
     */
    public List<String> getAdminEmails();

    /**
     * Gets the base url. NOTE: This is just for identifying purposes.
     *
     * @return Base url
     */
    public String getBaseUrl();

    /**
     * Gets the earliest date on the system. Any item should have a date lower
     * than this one.
     *
     * @return The earliest date
     */
    public Date getEarliestDate();

    /**
     * Repositories must declare one of three levels of support for deleted
     * records in the deletedRecord element of the Identify response.
     *
     * @return The delete method
     */
    public DeleteMethod getDeleteMethod();

    /**
     * Repositories must support selective harvesting with the from and until
     * arguments expressed at day granularity. Optional support for seconds
     * granularity is indicated in the response to the Identify request.
     *
     * @return Granularity
     */
    public Granularity getGranularity();

    /**
     * Getting description
     *
     * @return String
     */
    public List<String> getDescription();
}
