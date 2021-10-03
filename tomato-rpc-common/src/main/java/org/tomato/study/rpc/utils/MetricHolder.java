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
