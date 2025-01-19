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

/**
 * Dieser Service ist zuständig für die Erstellung von Graphen
 * Diese Klasse bietet Methoden zum Erstellen von Graphen
 */
@Service
@AllArgsConstructor
public class GraphService {

    private final PaymentRepository paymentRepository;

    /**
     * Gibt eine Liste der Fund-Graphen in Form von base64-image-Strings zurück
     * @param fundId Die Id des Funds
     * @param startDate Der erste Tag des 30-Tage-Zeitraums
     * @return eine Liste von Benutzern
     */
    public List<String> generateFundCharts(Long fundId, LocalDateTime startDate) {

        List<Object[]> dailyData = paymentRepository.findDailyTransactionsByFund(fundId, startDate);

        // process the raw data into the appropriate format for the bar and line charts
        Map<String, Double> barData = processBarChartData(dailyData, startDate);
        Map<String, Double> lineData = processLineChartData(dailyData, startDate, fundId);

        // extract the date-labels from the processed data
        List<String> labels = new ArrayList<>(barData.keySet());

        List<String> charts = new ArrayList<>();
        charts.add(createBarChart(labels, barData));
        charts.add(createLineChart(labels, lineData));

        return charts;
    }

    /**
     * Verarbeitet die Daten für das Säulendiagramm
     * @param transactions Tägliche Transaktionen der letzten 30 Tage
     * @param startDate Der erste Tag des 30-Tage-Zeitraums
     * @return Eine Map mit den Netto-Transaktionen jedes Tages
     */
    private Map<String, Double> processBarChartData(List<Object[]> transactions, LocalDateTime startDate) {
        Map<String, Double> dailyNetAmounts = new LinkedHashMap<>();

        // initialize all 30 days with 0, so even days without transactions get a space in the chart
        for (int i = 0; i < 30; i++) {
            LocalDateTime date = startDate.plusDays(i);
            dailyNetAmounts.put(date.toLocalDate().toString(), 0.0);
        }

        // add the netAmounts for all existing transaction dates, transaction[1] is date, [2] is netAmount
        for (Object[] transaction : transactions) {
            LocalDateTime date = (LocalDateTime) transaction[1];
            double netAmount = (double) transaction[2];
            dailyNetAmounts.put(date.toLocalDate().toString(), netAmount);
        }
        return dailyNetAmounts;
    }

    /**
     * Verarbeitet die Daten für den Kontostand-Graph
     * @param transactions Tägliche Transaktionen der letzten 30 Tage
     * @param startDate Der erste Tag des 30-Tage-Zeitraums
     * @param fundId Die Id des Funds
     * @return Eine Map mit dem Kontostand jedes Tages
     */
    private Map<String, Double> processLineChartData(List<Object[]> transactions, LocalDateTime startDate, Long fundId) {
        Map<String, Double> dailyBalances = new LinkedHashMap<>();

        // initialize all 30 days, days without transactions will remain at a constant value
        for (int i = 0; i < 30; i++) {
            LocalDateTime date = startDate.plusDays(i);
            dailyBalances.put(date.toLocalDate().toString(), null);
        }

        Optional<Double> initialBalanceOpt = paymentRepository.findBalanceByDate(fundId, startDate);
        double initialBalance = initialBalanceOpt.orElse(0.0);
        double currentBalance = initialBalance;

        for (Object[] transaction : transactions) {
            LocalDateTime date = ((LocalDateTime) transaction[1]).toLocalDate().atStartOfDay();
            double netAmount = (double) transaction[2];
            currentBalance += netAmount;
            dailyBalances.put(date.toLocalDate().toString(), currentBalance);
        }

        currentBalance = initialBalance;
        // fill the days without transactions with yesterday's balance
        for (String date : dailyBalances.keySet()) {
            if (dailyBalances.get(date) == null) {
                dailyBalances.put(date, currentBalance);
            } else {
                currentBalance = dailyBalances.get(date);
            }
        }
        return dailyBalances;
    }

    /**
     * Erstellt das Säulendiagramm
     * @param labels Achsenbeschriftung
     * @param barData Die Daten für das Diagramm
     * @return base64-image-String des Diagramms
     */
    private String createBarChart(List<String> labels, Map<String, Double> barData) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (String date : labels) {
                dataset.addValue(barData.get(date), "Transactions", formatDate(date));
            }

            // create the bar chart
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

            // customize the plot
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);

            // use a custom renderer for differently coloured bars
            BarRenderer renderer = getBarRenderer(dataset);

            // apply the custom renderer
            plot.setRenderer(renderer);

            customizeChartLayout(plot);

            // render the chart to a PNG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(outputStream, chart, 1600, 360);

            // convert to Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating bar chart", e);
        }
    }

    /**
     * Erstellt einen custom Renderer für verschiedenfarbige Säulen
     * @param dataset Das Dataset
     * @return BarRenderer
     */
    private static BarRenderer getBarRenderer(DefaultCategoryDataset dataset) {
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                Number value = dataset.getValue(row, column);
                if (value != null && value.doubleValue() < 0) {
                    return Color.RED; // withdrawals
                }
                return Color.GREEN; // deposits
            }
        };

        // remove the 3D effect
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        return renderer;
    }

    /**
     * Erstellt den Kontostand-Graph
     * @param labels Achsenbeschriftung
     * @param lineData Die Daten für den Graph
     * @return base64-image-String des Graphen
     */
    private String createLineChart(List<String> labels, Map<String, Double> lineData) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (String date : labels) {
                dataset.addValue(lineData.get(date), "Balance", formatDate(date));
            }

            // create the line chart
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

            // customize the plot
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);

            // set and apply a custom renderer for the line chart
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));

            plot.setRenderer(renderer);

            customizeChartLayout(plot);

            // render the chart to a PNG image
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(outputStream, chart, 1600, 360);

            // convert to Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error generating line chart", e);
        }
    }

    /**
     * Modifiziert das Design des Graphen/Diagramms
     * @param plot Der Plot
     */
    private void customizeChartLayout(CategoryPlot plot){
        // add vertical grid lines to separate each day
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        // customize the x-axis
        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14));
        xAxis.setTickLabelPaint(Color.BLACK);
        xAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16));

        // customize the y-axis
        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setTickLabelFont(new Font("SansSerif", Font.BOLD, 14));
        yAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 16));

        // add a black baseline at y=0
        plot.setRangeZeroBaselineVisible(true);
        plot.setRangeZeroBaselinePaint(Color.BLACK);
        plot.setRangeZeroBaselineStroke(new BasicStroke(1.0f));
    }

    /**
     * Formatiert ein Datum in das deutsche "dd.mm."-Format
     * @param date Das Datum
     * @return String des formatierten Datums
     */
    private String formatDate(String date) {
        return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d.M."));
    }
}