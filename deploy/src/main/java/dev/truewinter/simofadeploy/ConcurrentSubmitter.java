package dev.truewinter.simofadeploy;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


// Not the best name to describe this class, the submitters are run
// synchronously. But, the callback method will only be called if
// all the submitters have called their callback.
public class ConcurrentSubmitter {
    public static <T> void run(
            @NotNull ConcurrentSubmitterConfig<? extends Submitter<T>, T>[] config,
            @NotNull Submitter.SubmitterCallback submitterCallback) {
        Set<ConcurrentSubmitterConfig<? extends Submitter<T>, T>> completed = ConcurrentHashMap.newKeySet();
        AtomicBoolean errored = new AtomicBoolean(false);

        for (ConcurrentSubmitterConfig<? extends Submitter<T>, T> c : config) {
            if (errored.get()) return;

            c.getSubmitter().submit(c.getData(), new Submitter.SubmitterCallback() {
                @Override
                public synchronized void done() {
                    // Because all elements in a HashSet are unique, using it
                    // here ensures that no matter how many times done() is
                    // called by submitter, only the first call will succeed
                    // in furthering the success state of the ConcurrentSubmitter.
                    completed.add(c);

                    if (completed.size() == config.length) {
                        submitterCallback.done();
                    }
                }

                @Override
                public synchronized void error(Exception e) {
                    submitterCallback.error(e);
                    errored.set(true);
                }
            });
        }
    }
}
