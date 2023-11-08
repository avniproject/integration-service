package org.avni_integration_service.lahi.domain;

import org.avni_integration_service.glific.bigQuery.domain.FlowResult;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Students implements Iterator<Student> {
    private Iterator<FlowResult> results;
    private FlowResult nextResult;

    public Students(Iterator<FlowResult> results) {
        this.results = results;
    }

    @Override
    public boolean hasNext() {
        moveToNext();
        return !nextIsPicked();
    }

    @Override
    public Student next() {
        moveToNext();
        if (nextResult == null) {
            throw new NoSuchElementException("No more students to process");
        }
        return new Student(pickNext());
    }

    private FlowResult pickNext() {
        FlowResult flowResult = nextResult;
        nextResult = null;
        return flowResult;
    }

    private void moveToNext() {
        if (nextIsNotYetPicked()) return;

        while (results.hasNext()) {
            FlowResult next = results.next();
            if (resultIsComplete(next)) {
                nextResult = next;
                return;
            }
        }
    }

    private boolean nextIsNotYetPicked() {
        return !nextIsPicked();
    }

    private boolean nextIsPicked() {
        return nextResult == null;
    }

    private boolean resultIsComplete(FlowResult next) {
        String registration_flow_complete = next.getInput("registration_flow_complete");
        return registration_flow_complete != null && registration_flow_complete.equalsIgnoreCase("Yes");
    }
}
