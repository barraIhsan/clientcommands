package net.earthcomputer.clientcommands.task;

import net.earthcomputer.clientcommands.features.Relogger;

import java.util.*;

public class TaskManager {

    private static final List<LongTask> newTasks = new ArrayList<>();
    private static final Map<String, LongTask> tasks = new LinkedHashMap<>();
    private static long nextTaskId = 1;

    public static void tick() {
        newTasks.forEach(LongTask::initialize);
        newTasks.clear();

        if (tasks.isEmpty())
            return;

        List<Map.Entry<String, LongTask>> iteratingTasks = new ArrayList<>(tasks.entrySet());
        while (!iteratingTasks.isEmpty()) {
            Iterator<Map.Entry<String, LongTask>> itr = iteratingTasks.iterator();
            while (itr.hasNext()) {
                Map.Entry<String, LongTask> taskEntry = itr.next();
                LongTask task = taskEntry.getValue();
                if (task.isCompleted()) {
                    task.onCompleted();
                    tasks.remove(taskEntry.getKey());
                    itr.remove();
                } else {
                    task.body();
                    if (!task.isCompleted())
                        task.increment();
                    if (task.isDelayScheduled()) {
                        task.unscheduleDelay();
                        itr.remove();
                    }
                }
            }
        }
    }

    public static void onWorldUnload(boolean isDisconnect) {
        List<Map.Entry<String, LongTask>> oldTasks = new ArrayList<>();
        {
            Iterator<Map.Entry<String, LongTask>> itr = tasks.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, LongTask> entry = itr.next();
                if (entry.getValue().stopOnWorldUnload(isDisconnect)) {
                    itr.remove();
                    oldTasks.add(entry);
                }
            }
        }
        List<LongTask> oldNewTasks = new ArrayList<>();
        {
            Iterator<LongTask> itr = newTasks.iterator();
            while (itr.hasNext()) {
                LongTask newTask = itr.next();
                if (newTask.stopOnWorldUnload(isDisconnect)) {
                    itr.remove();
                    oldNewTasks.add(newTask);
                }
            }
        }

        if (isDisconnect && Relogger.isRelogging) {
            Relogger.relogSuccessTasks.add(() -> {
                for (Map.Entry<String, LongTask> oldTask : oldTasks) {
                    tasks.put(oldTask.getKey(), oldTask.getValue());
                }
                newTasks.addAll(oldNewTasks);
            });
        }
    }

    public static String addTask(String name, LongTask task) {
        String actualName = (nextTaskId++) + "." + name;
        tasks.put(actualName, task);
        newTasks.add(task);
        return actualName;
    }

    public static int getTaskCount() {
        return tasks.size();
    }

    public static Iterable<String> getTaskNames() {
        return tasks.keySet();
    }

    public static void removeTask(String name) {
        LongTask task = tasks.get(name);
        if (task != null)
            task._break();
    }

}
