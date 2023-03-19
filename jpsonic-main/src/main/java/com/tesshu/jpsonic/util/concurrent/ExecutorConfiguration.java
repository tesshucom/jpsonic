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
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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

    private final ShortTaskPoolConfiguration poolConf;

    public ExecutorConfiguration(ShortTaskPoolConfiguration poolConf) {
        super();
        this.poolConf = poolConf;
    }

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

    /*
     * General-purpose executor for small processing. Executes a task that has a relatively short execution time and
     * does not cause a fatal problem even if it is forcibly shut down (Shutdown implementation method depends on
     * individual task).
     */
    @Bean
    @DependsOn({ "legacyDaoHelper", "cacheDisposer" })
    public ThreadPoolTaskExecutor shortExecutor() {

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setWaitForTasksToCompleteOnShutdown(true); // To handle Stream
        executor.setAwaitTerminationMillis(SHORT_AWAIT_TERMINATION);
        executor.setQueueCapacity(poolConf.getQueueCapacity());
        executor.setCorePoolSize(poolConf.getCorePoolSize());
        executor.setMaxPoolSize(poolConf.getMaxPoolSize());
        suppressIfLargePool(executor);

        String threadGroupName = "short-task";
        executor.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
        executor.setThreadFactory(createThreadFactory(threadGroupName, Thread.MIN_PRIORITY));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setDaemon(true);

        executor.initialize();
        return executor;
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

        String threadGroupName = "podcast-download-task";
        executor.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
        executor.setThreadFactory(createThreadFactory(threadGroupName, Thread.MIN_PRIORITY));

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

        String threadGroupName = "podcast-refresh-task";
        executor.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
        executor.setThreadFactory(createThreadFactory(threadGroupName, Thread.MIN_PRIORITY));

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

        String threadGroupName = "scan-task";
        executor.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
        executor.setThreadFactory(createThreadFactory(threadGroupName, Thread.MIN_PRIORITY));

        executor.initialize();
        return executor;
    }

    @Bean
    @DependsOn("legacyDaoHelper")
    public TaskScheduler taskScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.setPoolSize(2); // scan and podcast. See *ScheduleConfiguration.

        String threadGroupName = "task-scheduler";
        scheduler.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
        scheduler.setThreadFactory(createThreadFactory(threadGroupName, Thread.MIN_PRIORITY));

        return scheduler;
    }

    private String createThreadNamePrefix(String threadGroupName) {
        return threadGroupName + "-pool-";
    }

    private ThreadFactory createThreadFactory(String threadGroupName, int threadPriority) {
        JpsonicThreadFactory th = new JpsonicThreadFactory((thread, throwable) -> LoggerFactory
                .getLogger(thread.getName()).error("An error occurred in the pooling thread.", throwable));
        th.setThreadGroupName(threadGroupName);
        th.setThreadNamePrefix(createThreadNamePrefix(threadGroupName));
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
        @SuppressWarnings("PMD.DoNotUseThreads") // Required to set UncaughtExceptionHandler.
        public Thread newThread(Runnable run) {
            Thread thread = super.newThread(run);
            thread.setUncaughtExceptionHandler(handler);
            return thread;
        }
    }

}
