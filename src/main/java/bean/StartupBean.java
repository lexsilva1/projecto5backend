package bean;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Singleton
@Startup
public class StartupBean {
    @Inject
    UserBean userBean;
    @Inject
    TaskBean taskBean;
    @Inject
    TimerBean timerBean;



    @PostConstruct
    public void init() {

        userBean.createDefaultUsers();
        taskBean.createDefaultCategories();
        taskBean.createDefaultTasks();
        userBean.startRemovingExpiredUsers();
        timerBean.createTimeout(300);
    }
}