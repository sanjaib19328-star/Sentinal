package com.sentinel.api.dto;

import java.util.List;

public class TimeSeriesResponse {

    private Long applicationId;
    private String interval;
    private List<TimeSeriesPointDto> points;

    public TimeSeriesResponse() {}

    public TimeSeriesResponse(Long applicationId, String interval, List<TimeSeriesPointDto> points) {
        this.applicationId = applicationId;
        this.interval = interval;
        this.points = points;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public List<TimeSeriesPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<TimeSeriesPointDto> points) {
        this.points = points;
    }
}
