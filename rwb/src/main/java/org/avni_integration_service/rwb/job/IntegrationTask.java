package org.avni_integration_service.rwb.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public enum IntegrationTask {
    // TODO: 24/12/24 Use IntegrationTask to filter if more than one rwb int task is present
    USER_NUDGE;
    public static List<IntegrationTask> getTasks(String taskNames) {
        if (taskNames.equals("all"))
            return Arrays.asList(IntegrationTask.values());

        List<IntegrationTask> tasks = new ArrayList<>();
        StringTokenizer stringTokenizer = new StringTokenizer(taskNames, ",");
        while (stringTokenizer.hasMoreTokens()) {
            tasks.add(IntegrationTask.valueOf(stringTokenizer.nextToken()));
        }
        return tasks;
    }
}
