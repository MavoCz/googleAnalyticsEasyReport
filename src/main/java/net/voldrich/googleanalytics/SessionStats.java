package net.voldrich.googleanalytics;

public class SessionStats extends BaseDailyStats {
    @GoogleAnalyticsMetric("ga:sessions")
    private Long sessions = 0L;

    @GoogleAnalyticsMetric("ga:bounces")
    private Long bounces = 0L;

    @GoogleAnalyticsMetric("ga:bounceRate")
    private Double bounceRatePercent = 0d;

    @GoogleAnalyticsMetric("ga:avgSessionDuration")
    private Double avgSessionDuration = 0d;

    @GoogleAnalyticsMetric("ga:hits")
    private Long hits = 0L;

    public Long getSessions() {
        return sessions;
    }

    public void setSessions(Long sessions) {
        this.sessions = sessions;
    }

    public Long getBounces() {
        return bounces;
    }

    public void setBounces(Long bounces) {
        this.bounces = bounces;
    }

    public Double getBounceRatePercent() {
        return bounceRatePercent;
    }

    public void setBounceRatePercent(Double bounceRatePercent) {
        this.bounceRatePercent = bounceRatePercent;
    }

    public Double getAvgSessionDuration() {
        return avgSessionDuration;
    }

    public void setAvgSessionDuration(Double avgSessionDuration) {
        this.avgSessionDuration = avgSessionDuration;
    }

    public Long getHits() {
        return hits;
    }

    public void setHits(Long hits) {
        this.hits = hits;
    }

    @Override
    public String toString() {
        return "SessionStats{" +
                "id=" + getId() +
                ", day=" + getDay() +
                ", sessions=" + sessions +
                ", bounces=" + bounces +
                ", bounceRatePercent=" + bounceRatePercent +
                ", avgSessionDuration=" + avgSessionDuration +
                ", hits=" + hits +
                '}';
    }
}
