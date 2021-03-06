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
package io.djigger.db.client;

import io.djigger.collector.accessors.InstrumentationEventAccessor;
import io.djigger.collector.accessors.MetricAccessor;
import io.djigger.collector.accessors.MongoConnection;
import io.djigger.collector.accessors.ThreadInfoAccessor;
import io.djigger.collector.accessors.stackref.ThreadInfoAccessorImpl;

public class StoreClient {

    ThreadInfoAccessor threadInfoAccessor;

    InstrumentationEventAccessor instrumentationAccessor;

    MetricAccessor metricAccessor;

    public void connect(String host, int port, String user, String password) throws Exception {
        MongoConnection connection = new MongoConnection();
        connection.connect(host, port, user, password);
        threadInfoAccessor = new ThreadInfoAccessorImpl(connection.getDb());
        instrumentationAccessor = new InstrumentationEventAccessor(connection.getDb());
        metricAccessor = new MetricAccessor(connection);
    }

    public ThreadInfoAccessor getThreadInfoAccessor() {
        return threadInfoAccessor;
    }

    public InstrumentationEventAccessor getInstrumentationAccessor() {
        return instrumentationAccessor;
    }

    public MetricAccessor getMetricAccessor() {
        return metricAccessor;
    }
}
