package com.mbientlab.metawear.impl.platform;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

public class AggregateException extends Exception {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "There were multiple errors.";

    private List<Throwable> innerThrowables;

    public AggregateException(String detailMessage, List<? extends Throwable> innerThrowables) {
        super(detailMessage,
                innerThrowables != null && innerThrowables.size() > 0 ? innerThrowables.get(0) : null);
        this.innerThrowables = Collections.unmodifiableList(innerThrowables);
    }

    @Override
    public void printStackTrace(PrintStream err) {
        super.printStackTrace(err);

        int currentIndex = -1;
        for (Throwable throwable : innerThrowables) {
            err.append("\n");
            err.append("  Inner throwable #");
            err.append(Integer.toString(++currentIndex));
            err.append(": ");
            throwable.printStackTrace(err);
            err.append("\n");
        }
    }

    @Override
    public void printStackTrace(PrintWriter err) {
        super.printStackTrace(err);

        int currentIndex = -1;
        for (Throwable throwable : innerThrowables) {
            err.append("\n");
            err.append("  Inner throwable #");
            err.append(Integer.toString(++currentIndex));
            err.append(": ");
            throwable.printStackTrace(err);
            err.append("\n");
        }
    }

}
