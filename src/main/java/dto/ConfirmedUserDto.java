package dto;

import java.time.LocalDateTime;

public class ConfirmedUserDto {
    private LocalDateTime confirmedDate;
    private Long count;

    public ConfirmedUserDto(LocalDateTime confirmedDate, Long count) {
        this.confirmedDate = confirmedDate;
        this.count = count;
    }

    public LocalDateTime getConfirmedDate() {
        return confirmedDate;
    }

    public void setConfirmedDate(LocalDateTime confirmedDate) {
        this.confirmedDate = confirmedDate;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
