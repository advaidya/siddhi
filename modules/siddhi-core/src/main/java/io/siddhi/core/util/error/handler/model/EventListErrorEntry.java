/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.core.util.error.handler.model;

import io.siddhi.core.event.Event;
import io.siddhi.core.util.error.handler.util.ErroneousEventType;
import io.siddhi.core.util.error.handler.util.ErrorOccurrence;
import io.siddhi.core.util.error.handler.util.ErrorType;

import java.util.List;

/**
 * Represents an error entry which contains a Siddhi Event List.
 */
public class EventListErrorEntry extends AbstractErrorEntry {
    private List<Event> event;

    public EventListErrorEntry(int id, long timestamp, String siddhiAppName, String streamName, String cause,
                               String stackTrace, String originalPayload, ErrorOccurrence errorOccurrence,
                               ErrorType errorType, List<Event> event) {
        super(id, timestamp, siddhiAppName, streamName, cause, stackTrace, originalPayload, errorOccurrence,
                ErroneousEventType.EVENT_LIST, errorType);
        this.event = event;
    }

    public List<Event> getEvent() {
        return event;
    }
}
