package service;

import bean.TaskBean;
import bean.UserBean;
import dto.*;
import entities.TaskEntity;
import entities.UserEntity;
import entities.CategoryEntity;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import jakarta.ws.rs.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/tasks")
public class TaskService {
    @Inject
    TaskBean taskBean;
    @Inject
    UserBean userBean;
    private static final Logger logger = LogManager.getLogger(TaskService.class);


    @GET
    @Path("/allActive")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllActiveTasks(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            logger.info("Authorized access");
            ArrayList<Task> taskList = taskBean.getAllActiveTasks();
            taskList.sort(Comparator.comparing(Task::getPriority, Comparator.reverseOrder()).thenComparing(Comparator.comparing(Task::getStartDate).thenComparing(Task::getEndDate)));
           logger.info("Returning task list");
            return Response.status(200).entity(taskList).build();
        }
    }

    @GET
    @Path("/allDeleted")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDeletedTasks(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            ArrayList<Task> taskList = taskBean.getDeletedTasks();
            taskList.sort(Comparator.comparing(Task::getPriority, Comparator.reverseOrder()).thenComparing(Comparator.comparing(Task::getStartDate).thenComparing(Task::getEndDate)));
            logger.info("Returning task list");
            return Response.status(200).entity(taskList).build();
        }
    }



    @GET
    @Path("/byCategory/{category}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTasksByCategory(@HeaderParam("token") String token, @PathParam("category") String category, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            ArrayList<Task> taskList = new ArrayList<>();
            for (TaskEntity taskEntity : taskBean.getTasks()) {
                if (taskEntity.isActive()) {
                    if (taskEntity.getCategory().getName().equals(category)) {
                        taskList.add(taskBean.convertToDto(taskEntity));
                    }
                }
            }
            taskList.sort(Comparator.comparing(Task::getPriority, Comparator.reverseOrder()).thenComparing(Comparator.comparing(Task::getStartDate).thenComparing(Task::getEndDate)));
            logger.info("Returning task list");
            return Response.status(200).entity(taskList).build();
        }
    }

    @DELETE
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAllTasks(@HeaderParam("token") String token, @PathParam("username") String username, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            UserEntity user = userBean.getUserEntityByUsername(username);
            logger.info("Removing all tasks from user " + username);
            boolean removed = taskBean.deleteAllTasksByUser(user);
            if (!removed) {
                logger.info("Failed. Tasks not removed");
                return Response.status(400).entity("Failed. Tasks not removed").build();
            } else {
                logger.info("Tasks removed");
                return Response.status(200).entity("Tasks removed").build();
            }
        }
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTask(Task task, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean valid = taskBean.isTaskValid(task);
            boolean categoryExists = taskBean.categoryExists(task.getCategory());
            if (!valid) {
                logger.info("All elements are required");
                return Response.status(400).entity("All elements are required").build();
            } else if (!categoryExists) {
                logger.info("Category does not exist");
                return Response.status(400).entity("Category does not exist").build();
            }
            User user = userBean.getUser(token);
            taskBean.setInitialId(task);
            UserEntity userEntity = userBean.convertToEntity(user);
            TaskEntity taskEntity = taskBean.createTaskEntity(task, userEntity);
            taskBean.addTask(taskEntity);
            logger.info("Task added");
            return Response.status(201).entity(taskBean.convertToDto(taskEntity)).build();
        }
    }

    @PATCH
    @Path("/active/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreTask(@HeaderParam("token") String token, @PathParam("id") String id, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean restored = taskBean.restoreTask(id);
            if (!restored) {
                logger.info("Failed. Task not restored");
                return Response.status(400).entity("Failed. Task not restored").build();
            } else {
                logger.info("Task restored");
                return Response.status(200).entity("Task restored").build();
            }
        }
    }

    @POST
    @Path("/Categories")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCategory(Category category, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        User user = userBean.getUser(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {

            boolean available = taskBean.categoryExists(category.getName());
            if (available) {
                logger.info("Name not available");
                return Response.status(409).entity("Name not available").build();
            }
            taskBean.createCategory(category.getName(), user.getUsername());
            logger.info("Category created");
            return Response.status(201).entity("Category created").build();
        }
    }

    @PUT
    @Path("/Categories")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCategory(Category category, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean notavailable = taskBean.categoryExists(category.getName());
            if (notavailable) {
                logger.info("Category name is not available");
                return Response.status(409).entity("Category name is not available").build();
            }
        }
        CategoryEntity categoryEntity = taskBean.findCategoryById(category.getId());
        categoryEntity.setName(category.getName());
        if (taskBean.updateCategory(categoryEntity)) {
            logger.info("Category updated");
            return Response.status(200).entity("Category updated").build();
        } else {
            logger.info("Failed. Category not updated");
            return Response.status(400).entity("Failed. Category not updated").build();
        }
    }

    @DELETE
    @Path("/Categories/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeCategory(@HeaderParam("token") String token, @PathParam("name") String name, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        logger.info("User authorized: " + authorized);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean exists = taskBean.categoryExists(name);
            if (!exists) {
                logger.info("Category does not exist");
                return Response.status(404).entity("Category does not exist").build();
            }
            boolean removed = taskBean.removeCategory(name);
            if (!removed) {
                logger.info("Failed. Category not removed. update all tasks before deleting the category");
                return Response.status(409).entity("Failed. Category not removed. update all tasks before deleting the category").build();
            } else {
                logger.info("Category removed");
                return Response.status(200).entity("Category removed").build();
            }
        }
    }

    @PUT
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTask(Task task, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        User user = userBean.getUser(token);
        logger.info("User " + user.getUsername() + "authorized");
        if (task.getEndDate() == null) {
            task.setEndDate(LocalDate.of(2199, 31, 12));
        }

        TaskEntity taskEntity = taskBean.convertToEntity(task);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean valid = taskBean.isTaskValid(task);
            boolean categoryExists = taskBean.categoryExists(task.getCategory());
            if (!valid) {
                logger.info("All elements are required");
                return Response.status(406).entity("All elements are required").build();
            } else if (!categoryExists) {
                logger.info("Category does not exist");
                return Response.status(404).entity("Category does not exist").build();
            } else if (!user.getUsername().equals(taskEntity.getUser().getUsername()) && user.getRole().equals("Developer")) {
                logger.info("Forbidden");
                return Response.status(403).entity("Forbidden").build();
            }
            String category = task.getCategory();
            CategoryEntity categoryEntity = taskBean.findCategoryByName(category);
            taskEntity.setCategory(categoryEntity);
            logger.info("Task updated");
            boolean updated = taskBean.updateTask(taskEntity);
            if (!updated) {
                logger.info("Failed. Task not updated");
                return Response.status(400).entity("Failed. Task not updated").build();
            } else {
                logger.info("Task updated");
                return Response.status(200).entity(taskBean.convertToDto(taskEntity)).build();
            }
        }
    }

    @PATCH
    @Path("/Status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeStatus(@HeaderParam("token") String token, @PathParam("id") String id, String status, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            JsonObject jsonObject = Json.createReader(new StringReader(status)).readObject();
            int newActiveStatus = jsonObject.getInt("status");
            boolean changed = taskBean.changeStatus(id, newActiveStatus);
            if (!changed) {
                logger.info("Failed. Status not changed");
                return Response.status(400).entity("Failed. Status not changed").build();
            } else {
                logger.info("Status changed");
                return Response.status(200).entity("Status changed").build();
            }
        }
    }
    @GET
    @Path("/Statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            TaskStatisticsDto statistics = taskBean.getTaskStatistics();
            logger.info("Returning statistics");
            return Response.status(200).entity(statistics).build();
        }
    }


    @DELETE
    @Path("/active/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response blockTask(@HeaderParam("token") String token, @PathParam("id") String id, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        User user = userBean.getUser(token);
        String role = user.getRole();
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            boolean blocked = taskBean.blockTask(id, role);
            if (!blocked) {
                logger.info("Failed. Task not blocked");
                return Response.status(400).entity("Failed. Task not blocked").build();
            } else {
                logger.info("Task blocked");
                return Response.status(200).entity("Task blocked").build();
            }
        }
    }

    @GET
    @Path("/Categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCategories(@HeaderParam("token") String token,@Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            ArrayList<Category> categoryList = new ArrayList<>();
            for (CategoryEntity categoryEntity : taskBean.getAllCategories()) {
                categoryList.add(taskBean.convertCatToDto(categoryEntity));
            }
            logger.info("Returning category list");
            return Response.status(200).entity(categoryList).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTaskById(@HeaderParam("token") String token, @PathParam("id") String id, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            Task task = taskBean.findTaskById(id);
            logger.info("Returning task");
            return Response.status(200).entity(task).build();
        }
    }

    @GET
    @Path("/creator/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreatorByName(@HeaderParam("token") String token, @PathParam("id") String id, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        } else {
            TaskCreator creator = taskBean.findUserById(id);
            logger.info("Returning creator");
            return Response.status(200).entity(creator).build();
        }
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilteredTasks(@HeaderParam("token") String token, @QueryParam("active") Boolean active, @QueryParam("category") String category, @QueryParam("username") String username, @QueryParam("id") String taskId, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("IP Address: " + ipadress);

        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("Unauthorized access");
            return Response.status(401).entity("Unauthorized").build();
        }
            if (taskId == null) {
                if (active == null) {
                    active = true;
                    ArrayList<Task> taskList = taskBean.getFilteredTasks(active, category, username);
                    logger.info("Returning task list");
                    return Response.status(200).entity(taskList).build();

                } else {
                    ArrayList<Task> taskList = taskBean.getFilteredTasks(active, category, username);
                    //taskList.sort(Comparator.comparing(Task::getPriority, Comparator.reverseOrder()).thenComparing(Comparator.comparing(Task::getStartDate).thenComparing(Task::getEndDate)));
                   logger.info("Returning task list");
                    return Response.status(200).entity(taskList).build();
                }

            } else {
                Task task = taskBean.findTaskById(taskId);
                return Response.status(200).entity(task).build();
            }
        }
    }
