package bean;
import Websocket.Chat;
import Websocket.Dashboard;
import Websocket.Tasks;
import dto.*;
import entities.UserEntity;
import entities.CategoryEntity;
import entities.TaskEntity;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import dao.TaskDao;
import dao.UserDao;
import bean.UserBean;
import entities.CategoryEntity;
import entities.TaskEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Singleton

public class TaskBean {
    public TaskBean() {
    }
    @EJB
    TaskDao taskDao;
    @EJB
    UserDao userDao;
    @EJB
    Chat chat;
    @Inject
    Dashboard dashboard;
    @Inject
    Tasks tasks;
    private static final Logger logger = LogManager.getLogger(TaskBean.class);
    public TaskBean(TaskDao taskDao) {
        this.taskDao = taskDao;
    }
    public boolean isTaskValid(Task task) {
        if (task.getTitle().isBlank() || task.getDescription().isBlank() || task.getStartDate() == null || task.getEndDate() == null || task.getCategory() == null) {
            logger.info("Task is invalid");
            return false;
        } else {
            logger.info("Task is valid");
            return task.getTitle() != null && task.getDescription() != null && task.getStartDate() != null && task.getEndDate() != null;
        }
    }

    public TaskEntity convertToEntity(dto.Task task) {
        logger.info("Converting task to entity");
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(task.getId());
        taskEntity.setTitle(task.getTitle());
        taskEntity.setDescription(task.getDescription());
        taskEntity.setStatus(task.getStatus());
        taskEntity.setCategory(taskDao.findCategoryByName(task.getCategory()));
        taskEntity.setStartDate(task.getStartDate());
        taskEntity.setPriority(task.getPriority());
        taskEntity.setEndDate(task.getEndDate());
        taskEntity.setUser(taskDao.findTaskById(task.getId()).getUser());
        taskEntity.setActive(true);
        taskEntity.setUser(taskDao.findTaskById(task.getId()).getUser());
        logger.info("Task converted to entity");
        return taskEntity;
    }

    public TaskEntity createTaskEntity(dto.Task task, UserEntity userEntity) {
        logger.info("Creating task entity");
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(task.getId());
        taskEntity.setTitle(task.getTitle());
        taskEntity.setDescription(task.getDescription());
        taskEntity.setStatus(task.getStatus());
        taskEntity.setCategory(taskDao.findCategoryByName(task.getCategory()));
        taskEntity.setStartDate(task.getStartDate());
        taskEntity.setPriority(task.getPriority());
        taskEntity.setEndDate(task.getEndDate());
        taskEntity.setUser(userEntity);
        taskEntity.setActive(true);
        logger.info("Task entity created");
        return taskEntity;
    }
    public TaskSocketDto convertEntityToSocketDto(TaskEntity taskEntity) {
        logger.info("Converting entity to socket dto");
        TaskSocketDto taskSocketDto = new TaskSocketDto();
        taskSocketDto.setTask(convertToDto(taskEntity));
        logger.info("Entity converted to socket dto");
        return taskSocketDto;
    }
    public boolean restoreTask(String id) {
        logger.info("Restoring task");
        TaskEntity a = taskDao.findTaskById(id);
        if (a != null) {
            a.setActive(true);
            taskDao.updateTask(a);
            dashboard.send("ping");
            TaskSocketDto taskSocketDto = convertEntityToSocketDto(a);
            taskSocketDto.setAction("restore");
            tasks.send(taskSocketDto);
            logger.info("Task restored");

            return true;
        }
        logger.info("Task not restored");
        return false;
    }

    public Category convertCatToDto(CategoryEntity categoryEntity) {
        logger.info("Converting category to dto");
        Category category = new Category();
        category.setId(categoryEntity.getId());
        category.setName(categoryEntity.getName());
        logger.info("Category converted to dto");
        return category;
    }
    public dto.Task convertToDto(TaskEntity taskEntity) {
        logger.info("Converting task to dto");
        dto.Task task = new dto.Task();
        task.setId(taskEntity.getId());
        task.setTitle(taskEntity.getTitle());
        task.setDescription(taskEntity.getDescription());
        task.setStatus(taskEntity.getStatus());
        task.setCategory(convertCatToDto(taskEntity.getCategory()).getName());
        task.setStartDate(taskEntity.getStartDate());
        task.setPriority(taskEntity.getPriority());
        task.setEndDate(taskEntity.getEndDate());
        task.setActive(taskEntity.isActive());
        task.setCreator(taskEntity.getUser().getUsername());
        logger.info("Task converted to dto");
        return task;
    }

    public void addTask(TaskEntity taskEntity) {
        logger.info("Adding task");
        taskDao.createTask(taskEntity);
        dashboard.send("ping");
        TaskSocketDto taskSocketDto = convertEntityToSocketDto(taskEntity);
        taskSocketDto.setAction("add");
        tasks.send(taskSocketDto);
        logger.info("Task added");
    }
    public List<TaskEntity> getTasks() {
        return taskDao.findAll();
    }
    public  List<TaskEntity> getTasksByUser(UserEntity userEntity) {
        logger.info("Getting tasks by user");
    return taskDao.findTasksByUser(userEntity);
    }
    public boolean deleteAllTasksByUser(UserEntity userEntity) {
        logger.info("Deleting all tasks by user");
        List<TaskEntity> tasks = taskDao.findTasksByUser(userEntity);
        for(TaskEntity task: tasks){
            task.setActive(false);
            taskDao.updateTask(task);
            dashboard.send("ping");
            TaskSocketDto taskSocketDto = convertEntityToSocketDto(task);
            taskSocketDto.setAction("block");
            this.tasks.send(taskSocketDto);
        }
        logger.info("All tasks by user deleted");
        return true;
    }
  public ArrayList<Task> getFilteredTasks( Boolean active,String category,String username) {
      logger.info("Getting filtered tasks");
      ArrayList<Task> tasks = new ArrayList<>();
      List<TaskEntity> activeTasks = taskDao.findAllActiveTasks();
      List<TaskEntity> inactiveTasks = taskDao.findDeletedTasks();
        logger.info("Filtering tasks");
        if(active && category == null && username==null) {
            for (TaskEntity taskEntity : activeTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(!active && category == null && username == null) {
            for (TaskEntity taskEntity : inactiveTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;


        } else if(active && category != null && username == null) {
            List<TaskEntity> allActiveTasks = taskDao.findTasksByCategory2(taskDao.findCategoryByName(category), active);
            for (TaskEntity taskEntity : allActiveTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(!active && category != null && username == null) {
            List<TaskEntity> allTasks = taskDao.findTasksByCategory2(taskDao.findCategoryByName(category), active);
            for (TaskEntity taskEntity : allTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(active && category == null && username != null )  {
            List<TaskEntity> allActiveTasks = taskDao.findTasksByUser2(userDao.findUserByUsername(username),active);
            for (TaskEntity taskEntity : allActiveTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(!active && category == null && username!=null){
            List<TaskEntity> allTasks = taskDao.findTasksByUser2(userDao.findUserByUsername(username),active);
            for (TaskEntity taskEntity : allTasks) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(active && category != null && username!=null) {
            List<TaskEntity> allActiveTasks = taskDao.findTasksByCategory2(taskDao.findCategoryByName(category), active);
            List<TaskEntity> allActiveTasksByUser = new ArrayList<>();
            for(TaskEntity task: allActiveTasks) {
                if(task.getUser().getUsername().equals(username)) {
                    allActiveTasksByUser.add(task);
                }
            }
            for (TaskEntity taskEntity : allActiveTasksByUser) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else if(!active && category != null && username != null) {
            List<TaskEntity> allTasks = taskDao.findTasksByCategory2(taskDao.findCategoryByName(category), active);
            List<TaskEntity> allTasksByUser = new ArrayList<>();
            for(TaskEntity task: allTasks) {
                if(task.getUser().getUsername().equals(username)) {
                    allTasksByUser.add(task);
                }
            }
            for (TaskEntity taskEntity : allTasksByUser) {
                tasks.add(convertToDto(taskEntity));
            }
            logger.info("Tasks filtered");
            return tasks;
        } else {
            return tasks;
        }

  }

    public Task findTaskById(String id) {
        return convertToDto(taskDao.findTaskById(id));
    }
    public ArrayList<Task> getAllActiveTasks() {
        logger.info("Getting all active tasks");
        List<TaskEntity> taskEntities = taskDao.findAll();
        ArrayList<Task> tasks = new ArrayList<>();
        for (TaskEntity taskEntity : taskEntities) {
            if (taskEntity.isActive()) {
                tasks.add(convertToDto(taskEntity));
            }
        }
        logger.info("All active tasks retrieved");
        return tasks;
    }
    public ArrayList<Task> getDeletedTasks() {
        logger.info("Getting all deleted tasks");
        List<TaskEntity> taskEntities = taskDao.findAll();
        ArrayList<Task> tasks = new ArrayList<>();
        for (TaskEntity taskEntity : taskEntities) {
            if (!taskEntity.isActive()) {
                tasks.add(convertToDto(taskEntity));
            }
        }
        logger.info("All deleted tasks retrieved");
        return tasks;
    }
    public TaskCreator findUserById(String id) {
        logger.info("Finding user by id");
    TaskEntity taskEntity = taskDao.findTaskById(id);
        TaskCreator taskCreator = new TaskCreator();
        taskCreator.setUsername(taskEntity.getUser().getUsername());
        taskCreator.setName(taskEntity.getUser().getName());
        logger.info("User found by id");
        return taskCreator;
    }
    public boolean categoryExists(String name) {
        if(taskDao.findCategoryByName(name) != null) {
            logger.info("Category exists");
            return true;
        }
        logger.info("Category does not exist");
        return false;
    }
    public boolean updateCategory(CategoryEntity categoryEntity) {
        logger.info("Updating category");
        CategoryEntity a = taskDao.findCategoryById(categoryEntity.getId());
        if (a != null) {
            a.setName(categoryEntity.getName());
            taskDao.updateCategory(a);
            dashboard.send("ping");
            logger.info("Category updated");
            return true;

        }
        logger.info("Category not updated");
        return false;
    }
    public void createCategory(String name, String creator) {
        logger.info("Creating category");
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setName(name);
        categoryEntity.setCreator(creator);
        taskDao.createCategory(categoryEntity);
        logger.info("Category created");
        logger.info("Sending ping to dashboard");
        dashboard.send("ping");
    }
    public boolean removeCategory(String name) {
        logger.info("Removing category");
        List<TaskEntity> tasks = taskDao.findAll();
        List<TaskEntity> tasksByCategory = new ArrayList<>();
        for(TaskEntity task : tasks) {
            if(task.getCategory().getName().equals(name)) {
                tasksByCategory.add(task);
            }
        }
        if(tasksByCategory.isEmpty()) {
            taskDao.removeCategory(taskDao.findCategoryByName(name));
            dashboard.send("ping");
            logger.info("Category removed");
            return true;
        }
        logger.info("Category not removed");
        return false;
    }
    public CategoryEntity findCategoryByName(String name) {
        return taskDao.findCategoryByName(name);
    }
    public CategoryEntity findCategoryById(int id) {
        return taskDao.findCategoryById(id);
    }
    public boolean blockTask(String id,String role) {
        logger.info("Blocking task");
        TaskEntity a = taskDao.findTaskById(id);
        if (a != null) {
            if(a.isActive() && !role.equals("developer")) {
                logger.info("Task is active");
                a.setActive(false);
                taskDao.updateTask(a);
                dashboard.send("ping");
                TaskSocketDto taskSocketDto = convertEntityToSocketDto(a);
                taskSocketDto.setAction("block");
                tasks.send(taskSocketDto);
                logger.info("Task blocked");
            }else if(!a.isActive()&& role.equals("Owner")) {
                logger.info("Task is not active");
                taskDao.remove(a);
                dashboard.send("ping");
                TaskSocketDto taskSocketDto = convertEntityToSocketDto(a);
                taskSocketDto.setAction("delete");
                tasks.send(taskSocketDto);
                logger.info("Task deleted");
            }
            logger.info("Task blocked/deleted");
            return true;
        }
        logger.info("Task not blocked nor deleted");
        return false;
    }



    public boolean updateTask(TaskEntity task) {
        logger.info("Updating task");
        TaskEntity a = taskDao.findTaskById(task.getId());
        if (a != null) {
            a.setTitle(task.getTitle());
            a.setDescription(task.getDescription());
            a.setPriority(task.getPriority());
            a.setStatus(task.getStatus());
            a.setStartDate(task.getStartDate());
            a.setEndDate(task.getEndDate());
            a.setCategory(task.getCategory());
            taskDao.updateTask(a);
            dashboard.send("ping");
            TaskSocketDto taskSocketDto = convertEntityToSocketDto(a);
            taskSocketDto.setAction("update");
            tasks.send(taskSocketDto);
            logger.info("Task updated");
            return true;
        }
        logger.info("Task not updated");
        return false;
    }
    public boolean changeStatus(String id, int status) {
        logger.info("Changing status");
        TaskEntity a = taskDao.findTaskById(id);
        if (a != null) {
            if(a.getStatus() == 30){
                a.setConclusionDate(null);
            }
            a.setStatus(status);
            if(status == 30){
                a.setConclusionDate(LocalDate.now());
            }
            if(status == 20 && a.getDoingDate() == null){
                a.setDoingDate(LocalDate.now());
            }

            taskDao.updateTask(a);
            dashboard.send("ping");
            TaskSocketDto taskSocketDto = convertEntityToSocketDto(a);
            taskSocketDto.setAction("status");
            tasks.send(taskSocketDto);
            logger.info("Status changed");
            return true;

        }
        logger.info("Status not changed");
        return false;
    }
    public List<CategoryEntity> getAllCategories() {
        logger.info("Getting all categories");
        return taskDao.findAllCategories();
    }

    public void setInitialId(Task task){
        logger.info("Setting initial id");
        task.setId("Task" + System.currentTimeMillis());}


public TaskStatisticsDto getTaskStatistics() {
        logger.info("Getting task statistics");
    TaskStatisticsDto taskStatisticsDto = new TaskStatisticsDto();
    taskStatisticsDto.setTotaDoneTasks(taskDao.findTasksByStatus(30).toArray().length);
    taskStatisticsDto.setTotalTasks(taskDao.findAll().size());
    taskStatisticsDto.setTotalDoingTasks(taskDao.findTasksByStatus(20).toArray().length);
    taskStatisticsDto.setTotalToDoTasks(taskDao.findTasksByStatus(10).toArray().length);
    taskStatisticsDto.setAverageTaskTime(getAverageTaskTime());
    taskStatisticsDto.setAverageTasksPerUser(averageTasksPerUser());
    taskStatisticsDto.setTasksByCategory(getTasksByCategory());
    taskStatisticsDto.setTasksCompletedByDate(taskDao.getTasksCompletedByDate());
    logger.info("Task statistics retrieved");
    return taskStatisticsDto;
}
public HashMap<String,Long> getTasksByCategory() {
        logger.info("Getting tasks by category");
     HashMap<String,Long> tasksByCategory = taskDao.getTaskCountPerCategory();

    return tasksByCategory;
}
public int getAverageTaskTime() {
        logger.info("Getting average task time");
    List<TaskEntity> tasks = taskDao.findAll();
    int total = 0;
    int count = 0;
    for (TaskEntity task : tasks) {
        if (task.getConclusionDate() != null) {
            if (task.getDoingDate() == null) {
                total += task.getStartDate().until(task.getConclusionDate()).getDays();
                count++;
            }else{
                total += task.getDoingDate().until(task.getConclusionDate()).getDays();
                count++;
            }
        }
    }
    if (count == 0) {
        logger.info("No tasks found");
        return 0;
    }
    logger.info("Average task time retrieved");
    return total / count;
}
public double averageTasksPerUser() {
        logger.info("Getting average tasks per user");
    List<UserEntity> users = userDao.findAll();
    List<TaskEntity> tasks = taskDao.findAll();
    double average = (double) tasks.size() / users.size();

    DecimalFormat df = new DecimalFormat("#.##");
    logger.info("Average tasks per user retrieved");
    return Double.parseDouble(df.format(average));
}
public void createDefaultCategories(){
        logger.info("Creating default categories");
        if(taskDao.findCategoryByName("Testing") == null){
            CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setName("Testing");
            categoryEntity.setCreator("System");
            taskDao.createCategory(categoryEntity);
        }
        if(taskDao.findCategoryByName("Backend") == null){
            CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setName("Backend");
            categoryEntity.setCreator("System");
            taskDao.createCategory(categoryEntity);
        }
        if(taskDao.findCategoryByName("Frontend") == null){
            CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setName("Frontend");
            categoryEntity.setCreator("System");
            taskDao.createCategory(categoryEntity);
        }
}
    public void createDefaultTasks() {
        logger.info("Creating default tasks");
    if (taskDao.findTaskById("Task1") == null) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task1");
        taskEntity.setTitle("KAIZEN");
        taskEntity.setDescription("Continuous improvement");
        taskEntity.setStatus(20);
        taskEntity.setCategory(taskDao.findCategoryByName("Testing"));
        taskEntity.setStartDate(LocalDate.now());
        taskEntity.setDoingDate(LocalDate.now());
        taskEntity.setPriority(100);
        taskEntity.setEndDate(LocalDate.of(2199, 12, 31));
        taskEntity.setUser(userDao.findUserByUsername("admin"));
        taskEntity.setActive(true);
        taskDao.createTask(taskEntity);
    }
    if (taskDao.findTaskById("Task2") == null) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task2");
        taskEntity.setTitle("Refactor");
        taskEntity.setDescription("Refactor the code");
        taskEntity.setStatus(10);
        taskEntity.setCategory(taskDao.findCategoryByName("Backend"));
        taskEntity.setStartDate(LocalDate.now());
        taskEntity.setPriority(200);
        taskEntity.setEndDate(LocalDate.of(2199, 12, 31));
        taskEntity.setUser(userDao.findUserByUsername("admin"));
        taskEntity.setActive(true);
        taskDao.createTask(taskEntity);
    }
    if (taskDao.findTaskById("Task3") == null) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task3");
        taskEntity.setTitle("Create new page");
        taskEntity.setDescription("Create a new page");
        taskEntity.setStatus(30);
        taskEntity.setCategory(taskDao.findCategoryByName("Frontend"));
        taskEntity.setStartDate(LocalDate.of(2024,4,2));
        taskEntity.setConclusionDate(LocalDate.now());
        taskEntity.setPriority(300);
        taskEntity.setEndDate(LocalDate.of(2199, 12, 31));
        taskEntity.setUser(userDao.findUserByUsername("admin"));
        taskEntity.setActive(true);
        taskDao.createTask(taskEntity);
    }
    if(taskDao.findTaskById("Task4") == null){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task4");
        taskEntity.setTitle("Render Page");
        taskEntity.setDescription("Update compoenents and render page");
        taskEntity.setStatus(10);
        taskEntity.setCategory(taskDao.findCategoryByName("Frontend"));
        taskEntity.setStartDate(LocalDate.of(2024,3,1));
        taskEntity.setPriority(300);
        taskEntity.setEndDate(LocalDate.of(2024, 5, 31));
        taskEntity.setUser(userDao.findUserByUsername("johndoe"));
        taskEntity.setActive(true);
        taskDao.createTask(taskEntity);
    }
    if(taskDao.findTaskById("Task5") == null){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task5");
        taskEntity.setTitle("Websockets");
        taskEntity.setDescription("Create a new Websockets feature");
        taskEntity.setStatus(20);
        taskEntity.setCategory(taskDao.findCategoryByName("Backend"));
        taskEntity.setStartDate(LocalDate.of(2024,3,13));
        taskEntity.setDoingDate(LocalDate.of(2024, 3, 13));
        taskEntity.setPriority(200);
        taskEntity.setEndDate(LocalDate.of(2024, 4, 29));
        taskEntity.setUser(userDao.findUserByUsername("johndoe"));
        taskEntity.setActive(false);
        taskDao.createTask(taskEntity);
    }
    if(taskDao.findTaskById("Task6") == null){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId("Task6");
        taskEntity.setTitle("Test Components");
        taskEntity.setDescription("Test all the new Components");
        taskEntity.setStatus(20);
        taskEntity.setCategory(taskDao.findCategoryByName("Testing"));
        taskEntity.setStartDate(LocalDate.of(2024,4,2));
        taskEntity.setDoingDate(LocalDate.of(2024,4,2));
        taskEntity.setPriority(100);
        taskEntity.setEndDate(LocalDate.of(2024, 4, 29));
        taskEntity.setUser(userDao.findUserByUsername("gabsmith"));
        taskEntity.setActive(true);
        taskDao.createTask(taskEntity);
    }
}
}

