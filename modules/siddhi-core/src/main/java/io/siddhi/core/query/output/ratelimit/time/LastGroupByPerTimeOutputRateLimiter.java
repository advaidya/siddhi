/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.core.query.output.ratelimit.time;


import io.siddhi.core.event.ComplexEvent;
import io.siddhi.core.event.ComplexEventChunk;
import io.siddhi.core.event.GroupedComplexEvent;
import io.siddhi.core.event.stream.StreamEventFactory;
import io.siddhi.core.query.output.ratelimit.OutputRateLimiter;
import io.siddhi.core.util.Schedulable;
import io.siddhi.core.util.Scheduler;
import io.siddhi.core.util.parser.SchedulerParser;
import io.siddhi.core.util.snapshot.state.State;
import io.siddhi.core.util.snapshot.state.StateFactory;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of {@link OutputRateLimiter} which will collect pre-defined time period and the emit only last
 * event. This implementation specifically represent GroupBy queries.
 */
public class LastGroupByPerTimeOutputRateLimiter
        extends OutputRateLimiter<LastGroupByPerTimeOutputRateLimiter.RateLimiterState> implements Schedulable {
    private static final Logger log = Logger.getLogger(LastGroupByPerTimeOutputRateLimiter.class);
    private final Long value;
    private String id;
    private ScheduledExecutorService scheduledExecutorService;
    private Scheduler scheduler;
    private long scheduledTime;

    public LastGroupByPerTimeOutputRateLimiter(String id, Long value, ScheduledExecutorService
            scheduledExecutorService) {
        this.id = id;
        this.value = value;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    protected StateFactory<RateLimiterState> init() {
        return () -> new RateLimiterState();
    }

    @Override
    public void process(ComplexEventChunk complexEventChunk) {
        ArrayList<ComplexEventChunk<ComplexEvent>> outputEventChunks = new ArrayList<>();
        complexEventChunk.reset();
        RateLimiterState state = stateHolder.getState();
        try {
            synchronized (state) {
                while (complexEventChunk.hasNext()) {
                    ComplexEvent event = complexEventChunk.next();
                    if (event.getType() == ComplexEvent.Type.TIMER) {
                        if (event.getTimestamp() >= scheduledTime) {
                            if (state.allGroupByKeyEvents.size() != 0) {
                                ComplexEventChunk<ComplexEvent> outputEventChunk = new ComplexEventChunk<ComplexEvent>
                                        (complexEventChunk.isBatch());
                                for (ComplexEvent complexEvent : state.allGroupByKeyEvents.values()) {
                                    outputEventChunk.add(complexEvent);
                                }
                                outputEventChunks.add(outputEventChunk);
                                state.allGroupByKeyEvents.clear();
                            }
                            scheduledTime = scheduledTime + value;
                            scheduler.notifyAt(scheduledTime);
                        }
                    } else if (event.getType() == ComplexEvent.Type.CURRENT || event.getType() == ComplexEvent.Type
                            .EXPIRED) {
                        complexEventChunk.remove();
                        GroupedComplexEvent groupedComplexEvent = ((GroupedComplexEvent) event);
                        state.allGroupByKeyEvents.put(groupedComplexEvent.getGroupKey(),
                                groupedComplexEvent.getComplexEvent());
                    }
                }
            }
        } finally {
            stateHolder.returnState(state);
        }
        for (ComplexEventChunk eventChunk : outputEventChunks) {
            sendToCallBacks(eventChunk);
        }

    }

    @Override
    public void start() {
        scheduler = SchedulerParser.parse(this, siddhiQueryContext);
        scheduler.setStreamEventFactory(new StreamEventFactory(0, 0, 0));
        scheduler.init(lockWrapper, siddhiQueryContext.getName());
        long currentTime = System.currentTimeMillis();
        scheduledTime = currentTime + value;
        scheduler.notifyAt(scheduledTime);
    }

    @Override
    public void stop() {
        //Nothing to stop
    }


    class RateLimiterState extends State {

        private Map<String, ComplexEvent> allGroupByKeyEvents = new LinkedHashMap<String, ComplexEvent>();

        @Override
        public boolean canDestroy() {
            return allGroupByKeyEvents.size() == 0;
        }

        @Override
        public Map<String, Object> snapshot() {
            Map<String, Object> state = new HashMap<>();
            state.put("AllGroupByKeyEvents", allGroupByKeyEvents);
            return state;
        }

        @Override
        public void restore(Map<String, Object> state) {
            allGroupByKeyEvents = (Map<String, ComplexEvent>) state.get("AllGroupByKeyEvents");
        }
    }

}
