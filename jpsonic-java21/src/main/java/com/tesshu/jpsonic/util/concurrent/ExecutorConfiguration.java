/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2024 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.DefaultManagedAwareThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
public class ExecutorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorConfiguration.class);

    protected static final int SHORT_AWAIT_TERMINATION = 10_000;
    protected static final int PODCAST_DOWNLOAD_AWAIT_TERMINATION = 25_000;
    protected static final int PODCAST_REFRESH_AWAIT_TERMINATION = 25_000;
    protected static final int SCAN_AWAIT_TERMINATION = 25_000;

    public ThreadPoolTaskExecutor suppressIfLargePool(ThreadPoolTaskExecutor executor) {

        int poolLimit = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);

        if (poolLimit < executor.getMaxPoolSize()) {
            executor.setMaxPoolSize(poolLimit);
        } else {
            executor.setMaxPoolSize(Math.max(executor.getMaxPoolSize(), 1));
        }

        if (poolLimit < executor.getCorePoolSize()) {
            executor.setCorePoolSize(poolLimit);
        } else {
            executor.setCorePoolSize(Math.max(executor.getCorePoolSize(), 1));
        }

        return executor;
    }

    @Bean
    @DependsOn({ "legacyDaoHelper", "cacheDisposer" })
    public AsyncTaskExecutor shortExecutor() {
        // @see TaskExecutorConfigurations
        // shortExecutor(SimpleAsyncTaskExecutorBuilder builder)
        // return builder.threadNamePrefix("jps-").build();

        // ... Vanilla is enough for now.
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /*
     * Podcast download executor. All tasks must be successfully canceled at shutdown.
     */
    @Bean
    @DependsOn({ "legacyDaoHelper", "cacheDisposer" })
    public ThreadPoolTaskExecutor podcastDownloadExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(true); // To handle IO
        executor.setAwaitTerminationMillis(PODCAST_DOWNLOAD_AWAIT_TERMINATION);
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        suppressIfLargePool(executor);
        executor.setThreadFactory(createThreadFactory(true, "podcast-download-task", Thread.MIN_PRIORITY));
        executor.initialize();
        return executor;
    }

    /*
     * Podcast refresh executor. All tasks must be successfully canceled at shutdown.
     */
    @Bean
    @DependsOn({ "legacyDaoHelper", "cacheDisposer", "podcastDownloadExecutor" })
    public ThreadPoolTaskExecutor podcastRefreshExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(true); // To call download
        executor.setAwaitTerminationMillis(PODCAST_REFRESH_AWAIT_TERMINATION);
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        suppressIfLargePool(executor);
        executor.setThreadFactory(createThreadFactory(true, "podcast-refresh-task", Thread.MIN_PRIORITY));
        executor.initialize();
        return executor;
    }

    /*
     * Scan thread executor. All tasks must be successfully canceled at shutdown.
     */
    @Bean
    @DependsOn({ "legacyDaoHelper", "cacheDisposer", "podcastRefreshExecutor" })
    public ThreadPoolTaskExecutor scanExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(true); // To handle IO
        executor.setAwaitTerminationMillis(SCAN_AWAIT_TERMINATION);
        // In v110.0.0, run in single thread
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadFactory(createThreadFactory(true, "scan-task", Thread.MIN_PRIORITY));
        executor.initialize();
        return executor;
    }

    @Bean
    @DependsOn("legacyDaoHelper")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setPoolSize(2); // scan and podcast. See *ScheduleConfiguration.
        scheduler.setThreadFactory(createThreadFactory(true, "task-scheduler", Thread.MIN_PRIORITY));
        return scheduler;
    }

    /**
     * @see org.jupnp.DefaultUpnpServiceConfiguration.JUPnPExecutor
     * @see com.tesshu.jpsonic.service.upnp.transport.UpnpServiceConfigurationAdapter
     */
    @Lazy
    @Bean
    @DependsOn("legacyDaoHelper")
    public ExecutorService upnpExecutorService() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(
                (runnable, threadPoolExecutor) -> LoggerFactory.getLogger(runnable.getClass())
                        .info("Thread pool(%s) rejected execution.".formatted(threadPoolExecutor.getClass())));
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(200);
        executor.setKeepAliveSeconds(10);
        executor.setQueueCapacity(1_000);
        executor.setThreadFactory(createThreadFactory(true, "upnp-default", Thread.NORM_PRIORITY));
        executor.initialize();
        return new ExecutorServiceAdapter(executor);
    }

    @Configuration
    public class VirtualExecutorServiceConfiguration {
        @Lazy
        @Bean("virtualExecutorService")
        public ExecutorService createVirtualExecutorService(
                @Autowired @Qualifier("shortExecutor") AsyncTaskExecutor shortExecutor) {
            return new ExecutorServiceAdapter(shortExecutor);
        }

        @Bean("asyncProtocolExecutorService")
        public ExecutorService createUpnpServices(
                @Autowired @Qualifier("virtualExecutorService") ExecutorService executorService) {
            return executorService;
        }
    }

    @Bean
    public Executor registryMaintainerExecutor() {
        return Executors.newSingleThreadExecutor(
                createThreadFactory(false, "upnp-registry-maintainer", Thread.MIN_PRIORITY));
    }

    private ThreadFactory createThreadFactory(boolean isPool, String threadGroupName, int threadPriority) {
        JpsonicThreadFactory th = new JpsonicThreadFactory((thread, throwable) -> LoggerFactory
                .getLogger(thread.getName()).error("An error occurred in the pooling thread.", throwable));
        th.setThreadGroupName(threadGroupName);
        th.setThreadNamePrefix("jps-" + threadGroupName + (isPool ? "-pool-" : "-"));
        th.setThreadPriority(threadPriority);

        try {
            // Currently, beans are not registered, so call manually
            th.afterPropertiesSet();
        } catch (NamingException e) {
            if (LOG.isInfoEnabled()) {
                LOG.info(threadGroupName + ": java:comp/DefaultManagedThreadFactory cannot look up.", e);
            }
        }

        return th;
    }

    /*
     * ThreadFactory for overriding UncaughtExceptionHandler. Output to the standard log. However, keep in mind that if
     * the caller is CallerRunsPolicy, the expected log output depends on the caller's thread implementation.
     */
    @SuppressWarnings("serial")
    public static class JpsonicThreadFactory extends DefaultManagedAwareThreadFactory implements ThreadFactory {

        private final Thread.UncaughtExceptionHandler handler;

        public JpsonicThreadFactory(Thread.UncaughtExceptionHandler handler) {
            super();
            this.handler = handler;
        }

        @Override
        public Thread newThread(Runnable run) {
            Thread thread = super.newThread(run);
            thread.setUncaughtExceptionHandler(handler);
            return thread;
        }
    }
}
