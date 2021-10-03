/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.utils;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;

import java.util.concurrent.TimeUnit;

/**
 * @author Tomato
 * Created on 2021.10.03
 */
public class MetricHolder {

    private final MetricRegistry metricRegistry;
    private volatile boolean consoleReporterStarted;
    private volatile boolean jmxReporterStarted;
    private ConsoleReporter consoleReporter;
    private JmxReporter jmxReporter;

    public MetricHolder() {
        this.metricRegistry = new MetricRegistry();
        this.consoleReporterStarted = false;
        this.jmxReporterStarted = false;
    }

    public synchronized void registerMetric(final String name, final Metric metric) {
        metricRegistry.register(name, metric);
    }

    public synchronized void startConsoleReporter(long period, TimeUnit unit) {
        if (consoleReporterStarted) {
            return;
        }
        consoleReporter = ConsoleReporter.forRegistry(metricRegistry).build();
        consoleReporter.start(period, unit);
        consoleReporterStarted = true;
    }

    public synchronized void startJmxReporter() {
        if (jmxReporterStarted) {
            return;
        }
        jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
        jmxReporter.start();
        jmxReporterStarted = true;
    }

    public synchronized void stop() {
        if (consoleReporterStarted) {
            consoleReporter.stop();
            consoleReporterStarted = false;
        }
        if (jmxReporterStarted) {
            jmxReporter.stop();
            jmxReporterStarted = false;
        }
    }


}
