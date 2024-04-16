package dto;

import java.util.List;

public class UserStatisticsDto {
    private int totalUsers;
    private int totalConfirmedusers;
    private int totalUnconfirmedUsers;
    private List<ConfirmedUserDto> confirmedUsersByDate;

    public UserStatisticsDto() {
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalConfirmedusers() {
        return totalConfirmedusers;
    }

    public void setTotalConfirmedusers(int totalConfirmedusers) {
        this.totalConfirmedusers = totalConfirmedusers;
    }

    public int getTotalUnconfirmedUsers() {
        return totalUnconfirmedUsers;
    }

    public void setTotalUnconfirmedUsers(int totalUnconfirmedUsers) {
        this.totalUnconfirmedUsers = totalUnconfirmedUsers;
    }

    public List<ConfirmedUserDto> getConfirmedUsersByDate() {
        return confirmedUsersByDate;
    }

    public void setConfirmedUsersByDate(List<ConfirmedUserDto> confirmedUsersByDate) {
        this.confirmedUsersByDate = confirmedUsersByDate;
    }
}
