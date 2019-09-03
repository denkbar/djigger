/*******************************************************************************
 * (C) Copyright 2016 Jérôme Comte and Dorian Cransac
 *
 *  This file is part of djigger
 *
 *  djigger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  djigger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with djigger.  If not, see <http://www.gnu.org/licenses/>.
 *
 *******************************************************************************/
package io.djigger.ui.instrumentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.djigger.monitoring.java.instrumentation.InstrumentationEvent;
import io.djigger.monitoring.java.model.GlobalThreadId;


public class InstrumentationStatistics implements Serializable {

    private static final long serialVersionUID = -6506453302335513463L;

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationStatistics.class);

    private final DateFormat format = new SimpleDateFormat("hh:mm:ss");

    private long start;

    private long end;

    private final ArrayList<Sample> samples;

    private long totalTimeSpent;

    private int realCount;

    private Double averageResponseTime;

    private Double throughput;

    public InstrumentationStatistics() {
        super();
        start = Long.MAX_VALUE;
        samples = new ArrayList<Sample>();
    }

    public InstrumentationStatistics(List<Sample> samples) {
        this();
        this.samples.addAll(samples);

        long totalTimeSpent = 0;
        for (Sample s : samples) {
            realCount++;
            totalTimeSpent += s.getElapsed();
        }

        if (realCount > 0) {
            averageResponseTime = totalTimeSpent / (1000000.0 * realCount);
        }
    }

    public void update(InstrumentationEvent sample) {
        start = Math.min(start, sample.getStart());
        end = Math.max(end, sample.getEnd());
        realCount++;
        long duration = sample.getDuration();
        totalTimeSpent += duration;
        synchronized (samples) {
            samples.add(new Sample(sample.getGlobalThreadId(), sample.getStart(), duration));
        }

        averageResponseTime = null;
        throughput = null;
    }

    public Integer getRealCount() {
        return realCount;
    }

    public Double getAverageResponseTime() {
        if (averageResponseTime == null) {
            if (realCount > 0) {
                averageResponseTime = totalTimeSpent / (1000000.0 * realCount);
            } else {
                return 0.0;
            }
        }
        return averageResponseTime;
    }

    public Double getThroughput() {
        if (throughput == null) {
            if (realCount > 1 && end - start > 0) {
                throughput = 1000.0 * realCount / (end - start);
            } else {
                return 0.0;
            }
        }
        return throughput;
    }

    public Long getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public class Sample implements Serializable {

        private static final long serialVersionUID = 9177464584885284437L;


        private final GlobalThreadId threadId;

        private final long time;

        private final long elapsed;

        public Sample(GlobalThreadId threadId, long start, long duration) {
            super();
            this.threadId = threadId;
            this.time = start;
            this.elapsed = duration;
        }

        public long getTime() {
            return time;
        }

        public long getElapsed() {
            return elapsed;
        }

        public GlobalThreadId getThreadId() {
            return threadId;
        }

        @Override
        public String toString() {
            return "Sample [threadId=" + threadId + ", time=" + time
                + ", elapsed=" + elapsed + "]";
        }
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public void export(File file, String label) {
        PrintWriter writer = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss");
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            synchronized (samples) {
                for (Sample s : samples) {
                    writer.println(format.format(new Date(s.time)) + "\t" + label + "\t" + s.elapsed);
                }
            }
        } catch (IOException e) {
            logger.error("Error while exporting samples to file " + file, e);
        } finally {
            writer.close();
        }
    }

}
