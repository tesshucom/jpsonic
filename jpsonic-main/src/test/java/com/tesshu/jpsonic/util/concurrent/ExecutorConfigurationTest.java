/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2021 tesshucom
 */

package com.tesshu.jpsonic.util.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.tesshu.jpsonic.NeedsHome;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

@SpringBootTest
@ExtendWith(NeedsHome.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutorConfigurationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorConfigurationTest.class);

    private static final int PROCESS_DELAY_MILL_SECONDS = 2000;

    @Autowired
    private ThreadPoolTaskExecutor shortExecutor;
    @Autowired
    private ThreadPoolTaskExecutor jukeExecutor;
    @Autowired
    private ThreadPoolTaskExecutor podcastDownloadExecutor;
    @Autowired
    private ThreadPoolTaskExecutor podcastRefreshExecutor;
    @Autowired
    private ThreadPoolTaskExecutor scanExecutor;

    @Autowired
    private ShortTaskPoolConfiguration shortThreadPoolConf;

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class TestInstance {

        @Order(1)
        @Test
        public void testShortExecutor() {
            MatcherAssert.assertThat(shortExecutor, CoreMatchers.instanceOf(ThreadPoolTaskExecutor.class));
            assertEquals("short-task-pool-", shortExecutor.getThreadNamePrefix());
        }

        @Order(2)
        @Test
        public void testjukeExecutor() {
            MatcherAssert.assertThat(jukeExecutor, CoreMatchers.instanceOf(ThreadPoolTaskExecutor.class));
            assertEquals("juke-task-pool-", jukeExecutor.getThreadNamePrefix());
        }

        @Order(3)
        @Test
        public void testPodcastDownloadExecutor() {
            MatcherAssert.assertThat(podcastDownloadExecutor, CoreMatchers.instanceOf(ThreadPoolTaskExecutor.class));
            assertEquals("podcast-download-task-pool-", podcastDownloadExecutor.getThreadNamePrefix());
        }

        @Order(4)
        @Test
        public void testPodcastRefreshExecutor() {
            MatcherAssert.assertThat(podcastRefreshExecutor, CoreMatchers.instanceOf(ThreadPoolTaskExecutor.class));
            assertEquals("podcast-refresh-task-pool-", podcastRefreshExecutor.getThreadNamePrefix());
        }

        @Order(5)
        @Test
        public void testScanExecutor() {
            MatcherAssert.assertThat(scanExecutor, CoreMatchers.instanceOf(ThreadPoolTaskExecutor.class));
            assertEquals("scan-task-pool-", scanExecutor.getThreadNamePrefix());
        }
    }

    /*
     * Basically, we should use Future, but if use a derivative of Future, please check the operation in advance.
     */
    @Nested
    public class DeprecatedFuture {

        /*
         * We should be able to use ListenableFuture to represent success and failure cases, but it seems to require
         * some background knowledge. Internally, it seems that submitListenable is calling Runnable ... despite
         * "submit".
         */
        @Test
        @SuppressWarnings("PMD.PreserveStackTrace") // false positive
        public void testDeprecatedListenableFuture() {

            int len = shortThreadPoolConf.getCorePoolSize();

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();

            List<ListenableFuture<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                Callable<Integer> callable = () -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                    } catch (InterruptedException e) {
                        c2.addAndGet(1);
                    }
                    throw new OutOfMemoryError(); // OutOfMemoryError!
                };
                futures.add(shortExecutor.submitListenable(callable));
            }

            assertThrows(InternalError.class, () -> { // InternalError
                futures.stream().mapToInt(future -> {
                    future.addCallback((i) -> {
                        c1.addAndGet(1); // This route does not pass ...
                    }, (e) -> {
                        MatcherAssert.assertThat(e.getCause(),
                                CoreMatchers.is(CoreMatchers.instanceOf(OutOfMemoryError.class)));
                        c3.addAndGet(1); // This route does not pass ...

                        // should be logging ... ...

                    });

                    try {
                        future.get(); // OutOfMemoryError!
                    } catch (ExecutionException | InterruptedException e) {

                        MatcherAssert.assertThat(e.getCause(),
                                CoreMatchers.is(CoreMatchers.instanceOf(OutOfMemoryError.class)));

                        c4.addAndGet(1);

                        // should be logging ... ...

                        throw new InternalError(e);
                    }

                    return 1;
                }).sum();

            }, "Bubbling is incorrect");

            /*
             * In other words, there are 3 cases of success/failure/runtime(Yes, it depends on Spring and is different
             * from regular Future.). If we're investigating a bug and don't know this behavior, may be confusing. For
             * batches, coding like ShortExecutor#testThrowable() is less misleading.
             * 
             * It may be not bad to use on Contloller methods. (ExceptionHandler of Spring is used as the error handler
             * when Future#get. Processing may not be passed to Callback.)
             */
            assertEquals(0, c1.get()); // 0 ...
            assertEquals(0, c2.get());
            assertEquals(0, c3.get()); // 0 ...
            assertEquals(1, c4.get()); // 1 ...

        }
    }

    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class ShortExecutor {

        /*
         * Exceptions are caught and logged but not bubbling. The log is output using UncaughtExceptionHandler.
         */
        @Order(1)
        @Test
        public void testWithRunnableWithinCorePool() {
            if (LOG.isInfoEnabled()) {
                LOG.error("***** It is a test of logging (testWithRunnableWithinCorePool). *****");
            }
            int len = shortThreadPoolConf.getCorePoolSize();
            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            for (int i = 0; i < len; i++) {
                try {
                    shortExecutor.execute(() -> {
                        throw new IllegalArgumentException("Sample Exception: testWithRunnableWithinCorePool");
                    });
                    c1.addAndGet(1); // Simple but count here without waiting
                } catch (IllegalArgumentException e) {
                    c2.addAndGet(1);
                }
            }
            assertNotEquals(0, c1.get() + c2.get());
        }

        /*
         * Same as (1) except that the queue is used
         */
        @Order(2)
        @Test
        public void testWithRunnableOverPool() {
            if (LOG.isInfoEnabled()) {
                LOG.error("***** It is a test of logging (testWithRunnableOverPool). *****");
            }
            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            int len = shortThreadPoolConf.getMaxPoolSize() + 1;

            for (int i = 0; i < len; i++) {
                try {
                    shortExecutor.execute(() -> {
                        try {
                            Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                        } catch (InterruptedException e) {
                            c3.addAndGet(1);
                        }
                        throw new IllegalArgumentException("Sample Exception: testWithRunnableOverPool");
                    });
                    c1.addAndGet(1); // Simple but count here without waiting
                } catch (IllegalArgumentException e) {
                    c2.addAndGet(1);
                }
            }
            assertNotEquals(0, c1.get() + c2.get());
            assertEquals(0, c3.get());
        }

        /*
         * Same as (1)(2) except that the queue is used
         */
        @Order(3)
        @Test
        public void testWithRunnableWithinQueue() {
            try {
                Thread.sleep(PROCESS_DELAY_MILL_SECONDS * 2);
            } catch (InterruptedException e) {
                LOG.error("Unreachable", e);
            }
            if (LOG.isInfoEnabled()) {
                LOG.error("***** It is a test of logging (testWithRunnableWithinQueue). *****");
            }
            int len = shortThreadPoolConf.getQueueCapacity();
            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            for (int i = 0; i < len; i++) {
                try {
                    // Even if it is a little long time, there is no problem if it is in the pooling
                    shortExecutor.execute(() -> {
                        try {
                            Thread.sleep(PROCESS_DELAY_MILL_SECONDS * 2);
                        } catch (InterruptedException e) {
                            c3.addAndGet(1);
                        }
                        throw new IllegalArgumentException("Sample Exception: testWithRunnableWithinQueue");
                    });
                    c1.addAndGet(1); // Simple but count here without waiting
                } catch (IllegalArgumentException e) {
                    c2.addAndGet(1);
                }
            }
            assertNotEquals(0, c1.get() + c2.get());
            assertEquals(0, c3.get());
        }

        /*
         * In the case of CallerRunsPolicy, if the queue is exceeded, it will be transferred to the parent thread.
         * Therefore, the log does not use the subthread's UncaughtExceptionHandler and depends on the thread
         * specification of the parent thread.(Probably Spring's ExceptionHandler in most cases.)
         * 
         * This will not happen unless it is used by a very large number of people, but it suggests that tuning should
         * be done if this happens.
         */
        @Order(4)
        @Test
        public void testWithRunnableOverQueue() {
            if (LOG.isInfoEnabled()) {
                LOG.error("***** It is a test of logging (testWithRunnableOverQueue). *****");
            }
            int len = shortThreadPoolConf.getQueueCapacity() + 20;
            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            for (int i = 0; i < len; i++) {
                try {
                    shortExecutor.execute(() -> {
                        try {
                            Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                        } catch (InterruptedException e) {
                            c3.addAndGet(1);
                        }
                        throw new IllegalArgumentException("Sample Exception: testWithRunnableOverQueue");
                    });
                    c1.addAndGet(1);
                } catch (IllegalArgumentException e) {
                    c2.addAndGet(1);
                }
            }
            assertNotEquals(0, c1.get() + c2.get());
            assertEquals(0, c3.get());
        }

        /*
         * UncaughtExceptionHandler is not used. Logging is mandatory in Future.
         */
        @Order(5)
        @Test
        public void testWithFutureWithinCorePool() {

            int len = shortThreadPoolConf.getCorePoolSize();

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();
            AtomicInteger c5 = new AtomicInteger();

            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                futures.add(shortExecutor.submit(() -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                    } catch (InterruptedException e) {
                        c3.addAndGet(1);
                    }
                    c1.addAndGet(1);
                    throw new IllegalArgumentException();
                }));
            }

            double sum = futures.stream().mapToInt(future -> {
                try {
                    future.get();
                } catch (IllegalArgumentException e) {
                    c4.addAndGet(1);
                } catch (InterruptedException e) {
                    c5.addAndGet(1);
                } catch (ExecutionException e) {
                    c2.addAndGet(1);

                    // should be logging !

                }
                return 1;
            }).sum();

            assertEquals(len, sum);
            assertEquals(len, c1.get());
            assertEquals(len, c2.get());
            assertEquals(0, c3.get());
            assertEquals(0, c4.get());
            assertEquals(0, c5.get());
        }

        /*
         * Same as (5) except that the queue is used
         */
        @Order(6)
        @Test
        public void testWithFutureOverPool() {

            int len = shortThreadPoolConf.getMaxPoolSize() + 5;

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();
            AtomicInteger c5 = new AtomicInteger();

            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                futures.add(shortExecutor.submit(() -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                    } catch (InterruptedException e) {
                        c3.addAndGet(1);
                    }
                    c1.addAndGet(1);
                    throw new IllegalArgumentException();
                }));
            }

            double sum = futures.stream().mapToInt(future -> {
                try {
                    future.get();
                } catch (IllegalArgumentException e) {
                    c4.addAndGet(1);
                } catch (InterruptedException e) {
                    c5.addAndGet(1);
                } catch (ExecutionException e) {
                    c2.addAndGet(1);

                    // should be logging !

                }
                return 1;
            }).sum();

            assertEquals(len, sum);
            assertEquals(len, c1.get());
            assertEquals(len, c2.get());
            assertEquals(0, c3.get());
            assertEquals(0, c4.get());
            assertEquals(0, c5.get());
        }

        /*
         * Same as (5)(6) except that the queue is used
         */
        @Order(7)
        @Test
        public void testWithFutureWithinQueue() {

            int len = shortThreadPoolConf.getQueueCapacity();

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();
            AtomicInteger c5 = new AtomicInteger();

            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                futures.add(shortExecutor.submit(() -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS * 2);
                    } catch (InterruptedException e) {
                        c3.addAndGet(1);
                    }
                    c1.addAndGet(1);
                    throw new IllegalArgumentException();
                }));
            }

            double sum = futures.stream().mapToInt(future -> {
                try {
                    future.get();
                } catch (IllegalArgumentException e) {
                    c4.addAndGet(1);
                } catch (InterruptedException e) {
                    c5.addAndGet(1);
                } catch (ExecutionException e) {
                    c2.addAndGet(1);

                    // should be logging !

                }
                return 1;
            }).sum();

            assertEquals(len, sum);
            assertEquals(len, c1.get());
            assertEquals(len, c2.get());
            assertEquals(0, c3.get());
            assertEquals(0, c4.get());
            assertEquals(0, c5.get());

        }

        /*
         * For CallerRunsPolicy, the parent thread is used but bubbling is possible. However, there is a concern that
         * the speed will slow down in this situation.(As a result, it becomes difficult to create new threads)
         * ShortExecutor is not a critical task and is mainly used for task or subtask of showing the Web page. Long
         * processing and thread overcrowding are relatively unlikely to occur. Therefore, queueCapacity uses a
         * relatively small value. In other words, shortExecutor is not intended for best effort, but for relatively
         * restrictive parallelism.
         */
        @Order(8)
        @Test
        public void testWithFutureOverQueue() {

            int len = shortThreadPoolConf.getQueueCapacity() + 20;

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();
            AtomicInteger c5 = new AtomicInteger();

            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                futures.add(shortExecutor.submit(() -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                    } catch (InterruptedException e) {
                        c3.addAndGet(1);
                    }
                    c1.addAndGet(1);
                    throw new IllegalArgumentException();
                }));
            }

            int sum = futures.stream().mapToInt(future -> {
                /*
                 * This is validation code, but in real code, the two options, InterruptedException and
                 * ExecutionException, are the simplest.
                 */
                try {
                    future.get();
                } catch (IllegalArgumentException e) {
                    c4.addAndGet(1);
                } catch (InterruptedException e) {
                    c5.addAndGet(1);
                } catch (ExecutionException e) {
                    c2.addAndGet(1);

                    // should be logging !

                }
                return 1;
            }).sum();

            assertEquals(len, sum);
            assertEquals(len, c1.get());
            assertEquals(len, c2.get());
            assertEquals(0, c3.get());
            assertEquals(0, c4.get());
            assertEquals(0, c5.get());
        }

        /*
         * PMD: Empty Catch Block is ranked Medium (3), but basically EmptyCatchBlock should not be allowed. Especially
         * if we catch an ExecutionException, the throwable will be wrapped and cannot be tracked without bubbling or
         * logging. Subthreads need logging because bubbling notifications cannot be seen unless the parent thread is
         * certain to exist.
         */
        @Order(9)
        @Test
        @SuppressWarnings("PMD.PreserveStackTrace") // false positive
        public void testWithFutureThrowable() {

            int len = shortThreadPoolConf.getCorePoolSize();

            AtomicInteger c1 = new AtomicInteger();
            AtomicInteger c2 = new AtomicInteger();
            AtomicInteger c3 = new AtomicInteger();
            AtomicInteger c4 = new AtomicInteger();
            AtomicInteger c5 = new AtomicInteger();

            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                futures.add(shortExecutor.submit(() -> {
                    try {
                        Thread.sleep(PROCESS_DELAY_MILL_SECONDS);
                    } catch (InterruptedException e) {
                        c2.addAndGet(1);
                    }
                    c1.addAndGet(1);
                    throw new OutOfMemoryError(); // OutOfMemoryError !
                }));
            }

            int sum = futures.stream().mapToInt(future -> {
                try {
                    future.get();
                } catch (OutOfMemoryError e) {
                    c3.addAndGet(1);
                    return 0;
                } catch (InterruptedException e) {
                    c4.addAndGet(1);
                    return 0;
                } catch (ExecutionException e) {
                    c5.addAndGet(1);
                    MatcherAssert.assertThat(e.getCause(),
                            CoreMatchers.is(CoreMatchers.instanceOf(OutOfMemoryError.class)));
                    assertNotEquals(0, c1.get(), "Something will be running");
                    assertEquals(0, c2.get(), "OutOfMemoryError is thrown");
                    assertEquals(0, c3.get(), "OutOfMemoryError is thrown");
                    assertEquals(0, c4.get(), "OutOfMemoryError is thrown");

                    // should be logging !

                    return 0;
                }
                return 1;
            }).sum();

            assertNotEquals(len, sum);
            assertEquals(0, sum);
            assertEquals(len, c1.get());
            assertEquals(0, c2.get());
            assertEquals(0, c3.get());
            assertEquals(0, c4.get());
            assertEquals(len, c5.get());
        }
    }

}
