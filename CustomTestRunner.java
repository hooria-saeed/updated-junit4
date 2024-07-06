package junit.runner;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.framework.TestListener;
import junit.framework.AssertionFailedError;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TestRunner {
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Map<String, Long> testDurations = new ConcurrentHashMap<>();

    public void runTestsInParallel(TestSuite suite) {
        for (int i = 0; i < suite.testCount(); i++) {
            final Test test = suite.testAt(i);
            executorService.submit(() -> {
                TestResult result = new TestResult();
                result.addListener(new TestListener() {
                    @Override
                    public void testStarted(String testName) {
                        System.out.println("Test started: " + testName);
                        testDurations.put(testName, System.currentTimeMillis());
                    }

                    @Override
                    public void testEnded(String testName) {
                        long endTime = System.currentTimeMillis();
                        long startTime = testDurations.get(testName);
                        System.out.println("Test ended: " + testName + " Time: " + (endTime - startTime) + " ms");
                    }

                    @Override
                    public void testFailed(int status, Test test, Throwable t) {
                        System.err.println("Test failed: " + test + " with status: " + status + ", error: " + t.getMessage());
                    }

                    @Override
                    public void addError(Test test, Throwable t) {
                        result.addError((TestCase) test, t);
                    }

                    @Override
                    public void addFailure(Test test, AssertionFailedError t) {
                        result.addFailure((TestCase) test, t);
                    }
                });
                test.run(result);
                System.out.println("Test: " + test + " completed.");
            });
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public void generateSummary(TestSuite suite) {
        System.out.println("Overall Summary:");
        System.out.println("Number of test cases: " + suite.countTestCases());
    }
}
