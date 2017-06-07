package jobt.plugin.execution;

import java.io.Serializable;
import java.util.concurrent.Callable;

import jobt.plugin.execution.Dispatcher.ExecArgs;
import jobt.plugin.execution.Dispatcher.WorkerProtocol;

public class WorkerCallable<T extends ExecArgs> implements Callable<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Class<? extends WorkerProtocol<T>> workerImplementationClass;
    private final T args;

    public WorkerCallable(
        final Class<? extends WorkerProtocol<T>> workerImplementationClass, final T args) {
        this.workerImplementationClass = workerImplementationClass;
        this.args = args;
    }

    @Override
    public Object call() throws Exception {
        return workerImplementationClass.newInstance().execute(args);
    }
}
