package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatisticsController {

    // Карточки метрик
    @FXML private VBox usersCard;
    @FXML private Label usersCountLabel;
    @FXML private VBox productsCard;
    @FXML private Label productsCountLabel;
    @FXML private VBox categoriesCard;
    @FXML private Label categoriesCountLabel;
    @FXML private VBox warehousesCard;
    @FXML private Label warehousesCountLabel;
    @FXML private VBox ordersCard;
    @FXML private Label ordersCountLabel;
    @FXML private VBox activeOrdersCard;
    @FXML private Label activeOrdersLabel;
    @FXML private VBox totalSumCard;
    @FXML private Label totalSumLabel;

    // Графики
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> stockBarChart;
    @FXML private CategoryAxis warehouseAxis;
    @FXML private NumberAxis quantityAxis;

    @FXML
    public void initialize() {
    }
}