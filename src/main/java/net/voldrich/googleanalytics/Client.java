package net.voldrich.googleanalytics;

import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.model.ColumnHeader;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues;
import com.google.api.services.analyticsreporting.v4.model.Dimension;
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest;
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse;
import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.MetricHeaderEntry;
import com.google.api.services.analyticsreporting.v4.model.Report;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import com.google.api.services.analyticsreporting.v4.model.ReportRow;
import org.apache.commons.beanutils.FluentPropertyBeanIntrospector;
import org.apache.commons.beanutils.PropertyUtilsBean;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Client {

    private static final String GA_DATE = "ga:date";

    private final String viewId;

    private final int maxNumberOfDays;

    private final AnalyticsReporting analyticsReporting;

    private final PropertyUtilsBean propertyUtilsBean;

    public Client(AnalyticsReporting analyticsReporting, String viewId, int maxNumberOfDays) {
        this.analyticsReporting = analyticsReporting;
        this.viewId = viewId;
        this.maxNumberOfDays = maxNumberOfDays;

        this.propertyUtilsBean = new PropertyUtilsBean();
        this.propertyUtilsBean.addBeanIntrospector(new FluentPropertyBeanIntrospector());
    }

    /**
     * Fetches metrics with date as dimension and metrics loaded from statsClass field annotations.
     * @param statsClass class fields must be annotated with GoogleAnalyticsMetric.
     * @see GoogleAnalyticsMetric
     **/
    public <T extends BaseDailyStats> List<T> getDailyStats(Class<T> statsClass)
            throws IOException, ReflectiveOperationException {
        DateRange dateRange = new DateRange();
        dateRange.setStartDate(maxNumberOfDays + "daysAgo");
        dateRange.setEndDate("today");

        Dimension dateDim = new Dimension().setName(GA_DATE);

        Map<String,Field> fieldsAndMetrics = getFieldsAndMetrics(statsClass);

        // collect metrics definitions
        List<Metric> metricList = fieldsAndMetrics.keySet().stream()
                .map(metricStr -> new Metric().setExpression(metricStr))
                .collect(Collectors.toList());

        ReportRequest request = new ReportRequest()
                .setViewId(viewId)
                .setDateRanges(Arrays.asList(dateRange))
                .setDimensions(Arrays.asList(dateDim))
                .setMetrics(metricList);

        GetReportsRequest getReport = new GetReportsRequest().setReportRequests(Collections.singletonList(request));
        GetReportsResponse response = analyticsReporting.reports().batchGet(getReport).execute();

        List<T> ret = new ArrayList<>();
        for (Report report: response.getReports()) {
            ColumnHeader header = report.getColumnHeader();
            List<String> dimensionHeaders = header.getDimensions();
            List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
            List<ReportRow> rows = report.getData().getRows();

            if (rows == null) {
                return Collections.emptyList();
            }

            Long id = 1L;
            T lastStat = null;
            for (ReportRow row : rows) {
                LocalDate dateDimension = getDateDimension(dimensionHeaders, row.getDimensions());

                if (lastStat != null) {
                    // if there are some missing date values between this and last value, then fill them with empty values
                    LocalDate nextDay = lastStat.getDay().plus(1, ChronoUnit.DAYS);
                    while (nextDay.compareTo(dateDimension) < 0) {
                        T statEmpty = statsClass.newInstance();
                        statEmpty.setId(id);
                        statEmpty.setDay(nextDay);
                        nextDay = nextDay.plus(1, ChronoUnit.DAYS);
                        ret.add(statEmpty);
                        id++;
                    }
                }

                T stat = statsClass.newInstance();
                stat.setDay(dateDimension);
                stat.setId(id);

                for (DateRangeValues values : row.getMetrics()) {
                    for (int k = 0; k < values.getValues().size() && k < metricHeaders.size(); k++) {
                        // match values to fields based on metric name
                        Field field = fieldsAndMetrics.get(metricHeaders.get(k).getName());
                        if (field == null) {
                            throw new IllegalStateException("Unknown metric in result: " + metricHeaders.get(k).getName());
                        }

                        String value = values.getValues().get(k);
                        if (field.getType() == Long.class) {
                            propertyUtilsBean.setProperty(stat, field.getName(), Long.parseLong(value));
                        } else if (field.getType() == Double.class) {
                            propertyUtilsBean.setProperty(stat, field.getName(), Double.parseDouble(value));
                        } else {
                            throw new UnsupportedOperationException("Unsupported property type " + field.getType().getSimpleName());
                        }
                    }

                    stat.populateCalculatedFields();
                }

                ret.add(stat);
                id++;
                lastStat = stat;
            }
        }
        return ret;
    }

    /**
     * Loads Metric definitions from class annotated with GoogleAnalyticsMetric to map metric name -> field.
     **/
    private Map<String, Field> getFieldsAndMetrics(Class<?> clazz) {
        Map<String, Field> ret = new HashMap<>();
        for(Field field : clazz.getDeclaredFields()){
            GoogleAnalyticsMetric annotation = field.getAnnotation(GoogleAnalyticsMetric.class);
            if (annotation != null) {
                if (ret.containsKey(annotation.value())) {
                    throw new IllegalStateException("Duplicate metric definition " + annotation.value());
                }
                ret.put(annotation.value(), field);
            }
        }
        return ret;
    }

    private LocalDate getDateDimension(List<String> dimensionHeaders, List<String> dimensions) {
        for (int i = 0; i < dimensionHeaders.size() && i < dimensions.size(); i++) {
            if (dimensionHeaders.get(i).equals(GA_DATE)) {
                return LocalDate.parse(dimensions.get(i), DateTimeFormatter.BASIC_ISO_DATE);
            }
        }
        throw new IllegalStateException("Date dimension was not found");
    }
}
