package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

@Service
@AllArgsConstructor
public class GraphService {

    private final PaymentRepository paymentRepository;

    public List<String> generateChartsForFund(Long fundId, LocalDateTime startDate) {
        // Fetch raw daily data from the repository
        List<Object[]> dailyData = paymentRepository.findDailyTransactionsByFund(fundId, startDate);

        // Preprocess the raw data into the appropriate format for the bar and line charts
        Map<String, Double> barData = preprocessBarChartData(dailyData, startDate); // Preprocess bar chart data
        Map<String, Double> lineData = preprocessLineChartData(dailyData, startDate); // Preprocess line chart data

        // Extract the labels (dates) from the processed data
        List<String> labels = new ArrayList<>(barData.keySet()); // Labels are the dates (keys of the map)

        // Return the generated charts as a List
        List<String> charts = new ArrayList<>();
        charts.add(createBarChart(labels, barData));
        charts.add(createLineChart(labels, lineData));

        return charts;
    }

    private String createBarChart(List<String> labels, Map<String, Double> barData) {
        try {
            // Create the dataset
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (String date : labels) {
                dataset.addValue(barData.get(date), "Transactions", formatDate(date)); // Format date
            }

            // Create the bar chart
            JFreeChart chart = ChartFactory.createBarChart(
                    "Daily Transactions Overview",
                    "Date",
                    "Amount (€)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );

            // Customize the plot
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE); // Set plot background to white

            // Use a custom renderer to dynamically color bars
            BarRenderer renderer = new BarRenderer() {
                @Override
                public Paint getItemPaint(int row, int column) {
                    Number value = dataset.getValue(row, column);
                    if (value != null && value.doubleValue() < 0) {
                        return Color.RED; // Negative bars are red
                    }
                    return Color.GREEN; // Positive bars are green
                }
            };

            // Remove the 3D effect
            renderer.setBarPainter(new StandardBarPainter());
            renderer.setShadowVisible(false); // Disable shadows
            renderer.setDrawBarOutline(false); // Remove outlines

            // Apply the custom renderer
            plot.setRenderer(renderer);

            // Add vertical grid lines to separate each day
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

            // Customize the X-axis
            CategoryAxis xAxis = plot.getDomainAxis();
            xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45); // Rotate labels for readability
            xAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14)); // Increase font size
            xAxis.setTickLabelPaint(Color.BLACK);
            xAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16)); // Increase label font size

            // Customize the Y-axis
            ValueAxis yAxis = plot.getRangeAxis();
            yAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14));
            yAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16));

            // Add the black baseline at Y=0 with reduced thickness
            plot.setRangeZeroBaselineVisible(true);
            plot.setRangeZeroBaselinePaint(Color.BLACK);
            plot.setRangeZeroBaselineStroke(new BasicStroke(1.0f)); // Thinner black line at Y=0

            // Render the chart to a PNG image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 1600, 360); // Increased width, decreased height

            // Convert to Base64
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating bar chart", e);
        }
    }

    private String createLineChart(List<String> labels, Map<String, Double> lineData) {
        try {
            // Create the dataset
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (String date : labels) {
                dataset.addValue(lineData.get(date), "Balance", formatDate(date)); // Add current balance to the dataset
            }

            // Create the line chart
            JFreeChart chart = ChartFactory.createLineChart(
                    "Fund Balance Over Time",
                    "Date",
                    "Balance (€)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );

            // Customize the plot
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE); // Set plot background to white

            // Set a custom renderer for the line chart
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setSeriesPaint(0, Color.RED); // Use red for the line
            renderer.setSeriesStroke(0, new BasicStroke(2.0f)); // Thicker line for better visibility

            // Apply the custom renderer
            plot.setRenderer(renderer);

            // Add vertical grid lines to separate each day
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

            // Customize the X-axis
            CategoryAxis xAxis = plot.getDomainAxis();
            xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45); // Rotate labels for readability
            xAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14)); // Increase font size
            xAxis.setTickLabelPaint(Color.BLACK);
            xAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16)); // Increase label font size

            // Customize the Y-axis
            ValueAxis yAxis = plot.getRangeAxis();
            yAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14));
            yAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16));

            // Add the black baseline at Y=0 with reduced thickness
            plot.setRangeZeroBaselineVisible(true);
            plot.setRangeZeroBaselinePaint(Color.BLACK);
            plot.setRangeZeroBaselineStroke(new BasicStroke(1.0f)); // Thinner black line at Y=0

            // Render the chart to a PNG image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 1600, 360); // Increased width, decreased height

            // Convert to Base64
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating line chart", e);
        }
    }

    private String formatDate(String date) {
        LocalDate parsedDate = LocalDate.parse(date); // Assuming input is in ISO-8601 format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.");
        return parsedDate.format(formatter);
    }

    private Map<String, Double> preprocessBarChartData(List<Object[]> transactions, LocalDateTime startDate) {
        Map<String, Double> dailyNetAmounts = new LinkedHashMap<>();

        // Initialize with all days in the past 30 days
        for (int i = 0; i < 30; i++) {
            LocalDateTime date = startDate.plusDays(i);
            dailyNetAmounts.put(date.toLocalDate().toString(), 0.0);
        }

        // Populate net amounts for transaction dates
        for (Object[] transaction : transactions) {
            LocalDateTime date = ((LocalDateTime) transaction[1]).toLocalDate().atStartOfDay();
            double netAmount = (double) transaction[2];
            dailyNetAmounts.put(date.toLocalDate().toString(), netAmount);
        }

        return dailyNetAmounts;
    }

    private Map<String, Double> preprocessLineChartData(List<Object[]> transactions, LocalDateTime startDate) {
        Map<String, Double> dailyBalances = new LinkedHashMap<>();

        // Initialize with all days in the past 30 days
        for (int i = 0; i < 30; i++) {
            LocalDateTime date = startDate.plusDays(i);
            dailyBalances.put(date.toLocalDate().toString(), null);
        }

        double lastKnownBalance = paymentRepository.findBalanceByDate((Long) transactions.getFirst()[0], startDate).orElse(0.0);;
        double initialBalance = lastKnownBalance;
        System.out.println(lastKnownBalance + "id:" + transactions.getFirst()[0]);
        // Populate balance for available transaction dates
        for (Object[] transaction : transactions) {
            LocalDateTime date = ((LocalDateTime) transaction[1]).toLocalDate().atStartOfDay();
            double netAmount = (double) transaction[2];
            lastKnownBalance += netAmount;
            dailyBalances.put(date.toLocalDate().toString(), lastKnownBalance);
        }

        lastKnownBalance = initialBalance;
        // Fill missing days with the last known balance
        for (String date : dailyBalances.keySet()) {
            if (dailyBalances.get(date) == null) {
                dailyBalances.put(date, lastKnownBalance);
                System.out.println(lastKnownBalance);
            } else {
                lastKnownBalance = dailyBalances.get(date);
            }
        }

        return dailyBalances;
    }
}