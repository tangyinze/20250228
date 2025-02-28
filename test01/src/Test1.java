import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @program: test01
 * @description: helloword
 * @author: tyz
 * @create: 2025-01-17
 */
public class Test1 {
    public static void main(String[] args){
        /*
        * 创建自定义线程池
        * cpu 密集型=cpu线程数+1
        * IO 密集型=cpu线程数*2
        * 队列选型：
        *  快速响应：SynchronousQueue 同步队列
        *  流量消峰：LinkedBlockingQueue
        *  延时任务: DelayedWorkQueue
        * */
        ThreadFactory factory = (Runnable r) -> {
            Thread t = new Thread(r);
            //t.setName("TEST-TYZ-POOL-");
            t.setUncaughtExceptionHandler((Thread thread1, Throwable e) -> {
                System.out.println(thread1.getName() + " setUncaughtExceptionHandler" + e.getMessage());
                //throw e;
            });
            return t;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                // 核心线程数
                2,
                // 最大线程数
                5,
                // 空闲线程存活时间
                60L,
                // 空闲线程存活时间单位
                TimeUnit.SECONDS,
                // 阻塞队列 10
                new LinkedBlockingQueue<>(10),
                // 线程工厂
                // Executors.defaultThreadFactory(),
                factory,
                // 拒绝策略
                new ThreadPoolExecutor.AbortPolicy()
        ){
            // 重写 afterExecute 方法
            /**
             * 接着重写afterexecutor方法，该方法每次任务完成后调用，
             * 在它内部也调用一遍monitor方法，
             * 每当有任务完成的时候，输出一次线程池的情况，
             * @param r0
             * @param t0
             */
            @Override
            protected void afterExecute(Runnable r0, Throwable t0) {
                monitor();
                // 这个execute 提交的时候的异常信息
                if (Objects.nonNull(t0)) {
                    System.out.println("afterExecute by execute 提交的时候的异常信息" + t0.getMessage());
                }
                // 如果 r0 的实际类型是FutureTask 那么是submit 提交的，所以可以在里边get到异常信息，
                if(r0 instanceof FutureTask){
                    Future<?> future = (Future<?>)r0;
                    // get 获取异常
                    try {
                        future.get();
                    } catch (Exception e) {
                        System.out.println("afterExecute by submit 提交的时候的异常信息" + e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
            }

            // 自定义监控方法
            public void monitor(){
                System.out.println(
                        " 当前存在的线程数 Pool Size: " + this.getPoolSize() +
                        " 正在工作的线程数 Active Count: " + this.getActiveCount() +
                        " 队列中的任务数 Queue Size: " + this.getQueue().size()
                );
            }
        };
        // 当线程池execute时
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getName() + "-taskID: " + taskId);
                try {
                    if (taskId % 3 == 0) {
                        int a = 1 / 0;
                    }
                    Thread.sleep(2000L);
                } catch (InterruptedException e0) {
                    // throw new RuntimeException(e);
                    e0.printStackTrace();
                }
            });
        }
        // 当线程池submit时
        executor.submit(() -> {
            System.out.println("execute by submit success task");
        });
        executor.submit(() -> {
            System.out.println("execute by submit error task");
            int ao = 1 / 0;
        });

        /**
           线程池中线程异常处理机制‌：
            execute方法‌：当使用execute方法提交任务时，如果任务抛出未捕获的异常，可以通过设置UncaughtExceptionHandler来捕获并处理这些异常。
            submit方法‌：对于submit方法，未捕获的异常会被封装在Future对象中，调用Future的get方法时会重新抛出ExecutionException，可以在此处理异常‌

         * 线程池异常处理的一般流程‌：
         * ‌设置UncaughtExceptionHandler‌：在线程池创建时设置UncaughtExceptionHandler，用于捕获线程中未捕获的异常。
         * ‌重写afterExecute方法‌：对于submit方法，可以通过重写ThreadPoolExecutor的afterExecute方法来处理异常。
         * Future的get方法‌：对于submit方法，可以通过Future对象的get方法重新抛出ExecutionException来处理异常。
        */

        // 关闭线程池
        executor.shutdown();
    }
}
