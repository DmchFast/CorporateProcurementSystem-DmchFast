package com.team.corporate.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class ServicesController {

    @FXML private Label pageTitleLabel;
    @FXML private GridPane servicesGrid;

    // Каталог товаров
    @FXML private StackPane catalogCard;
    @FXML private Label catalogTitleLabel;
    @FXML private Label catalogSubtitleLabel;

    // Заказы
    @FXML private StackPane ordersCard;
    @FXML private Label ordersTitleLabel;
    @FXML private Label ordersSubtitleLabel;

    // Категории
    @FXML private StackPane categoriesCard;
    @FXML private Label categoriesTitleLabel;
    @FXML private Label categoriesSubtitleLabel;

    // Склады и остатки
    @FXML private StackPane warehousesCard;
    @FXML private Label warehousesTitleLabel;
    @FXML private Label warehousesSubtitleLabel;

    // Статистика
    @FXML private StackPane statisticsCard;
    @FXML private Label statisticsTitleLabel;
    @FXML private Label statisticsSubtitleLabel;

    // Экспорт данных
    @FXML private StackPane exportCard;
    @FXML private Label exportTitleLabel;
    @FXML private Label exportSubtitleLabel;

    // Пользователи
    @FXML private StackPane usersCard;
    @FXML private Label usersTitleLabel;
    @FXML private Label usersSubtitleLabel;

    @FXML
    public void initialize() {
        for (StackPane card : List.of(catalogCard, ordersCard, categoriesCard,
                warehousesCard, statisticsCard, exportCard, usersCard)) {
            applyRoundedClip(card);
        }
    }

    // Обрезает содержимое карточки по скруглённому прямоугольнику — как overflow:hidden в HTML
    private void applyRoundedClip(StackPane card) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);   // = 2 * background-radius (14)
        clip.setArcHeight(28);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);
    }
}