package dto;

public class TaskSocketDto  {
    private String action;
    private Task task;


    public TaskSocketDto(String action) {
        this.action = action;
    }

    public TaskSocketDto() {


    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
