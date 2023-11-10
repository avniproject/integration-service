package org.avni_integration_service.lahi.domain;

import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Students implements Iterator<Student> {
    private static final Logger logger = Logger.getLogger(Students.class);
    private final Iterator<FlowResult> results;
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
        if (!nextIsPicked()) return;

        while (results.hasNext()) {
            FlowResult flowResult = results.next();
            if (flowResult.isComplete()) {
                nextResult = flowResult;
                return;
            }
            logger.warn(String.format("Record: %s is in-complete: %s. skipping", flowResult.getFlowResultId(), flowResult.getRegistrationFlowCompleteValue()));
        }
    }

    private boolean nextIsPicked() {
        return nextResult == null;
    }
}
