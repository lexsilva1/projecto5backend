package bean;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Singleton
@Startup
public class StartupBean {
    @Inject
    UserBean userBean;
    @Inject
    TaskBean taskBean;
    @Inject
    TimerBean timerBean;
    private static final Logger logger = LogManager.getLogger(StartupBean.class);


    @PostConstruct
    public void init() {
        logger.info("Creating default users, categories and tasks...");
        userBean.createDefaultUsers();
        taskBean.createDefaultCategories();
        taskBean.createDefaultTasks();
        userBean.startRemovingExpiredUsers();
        timerBean.createTimeout(300);
    }
}