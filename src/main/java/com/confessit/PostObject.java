package com.confessit;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Calendar;
import java.util.concurrent.Callable;

public class PostObject {
    /**
     * Stage is used to represent a window in a JavaFX desktop application
     */
    private Stage stage;

    /**
     * Scene is the container for all content in a scene graph
     */
    private Scene scene;

    /**
     * Root provides a solution to the issue of defining a reusable component with FXML
     */
    private Parent root;

    @FXML
    private Label postDate;

    @FXML
    private GridPane postGrid;

    @FXML
    private Button approveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private FlowPane adjustPane;

    @FXML
    public void setPost(Post pendingPost) throws FileNotFoundException {
        postGrid.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        postDate.setText("   Posted at " + pendingPost.getDatetime().toString());
        CustomTextArea contentField = new CustomTextArea(pendingPost.getContent());
        contentField.setStyle("-fx-focus-color: transparent; -fx-text-box-border: transparent;");
        postGrid.add(contentField, 0, 2);

        ImageView imagePane = new ImageView("com/fxml-resources/GreenTick.gif");
        imagePane.setFitWidth(150);
        imagePane.setPreserveRatio(true);
        postGrid.add(imagePane, 0, 3);

        approveButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            approve(pendingPost.getIndex());
        });
        deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            delete(pendingPost.getIndex());
        });

//        if (pictureFilePath != null) {
//            ImageView image = new ImageView(pictureFilePath);
//            postGrid.add(image, 0, 3);
//        }
    }

    public FlowPane getAdjustPane() {
        return this.adjustPane;
    }

    class CustomTextArea extends TextArea {
        CustomTextArea(String content) {
            setWrapText(true);
            setFocusTraversable(false);
            setMouseTransparent(true);
            setText(content);
            setFont(Font.font("Lato", 20));
            setBlendMode(BlendMode.DARKEN);

            sceneProperty().addListener((observableNewScene, oldScene, newScene) -> {
                if (newScene != null) {
                    applyCss();
                    layout();
                    Node text = lookup(".text");
                    prefHeightProperty().bind(Bindings.createDoubleBinding(() -> getFont().getSize() + text.getBoundsInLocal().getHeight(), text.boundsInLocalProperty()));
                    text.boundsInLocalProperty().addListener((observableBoundsAfter, boundsBefore, boundsAfter) -> {
                        Platform.runLater(() -> requestLayout());
                    });
                    adjustPane.setPrefHeight(prefHeightProperty().getValue() % 10);
                }
            });
        }
    }

    void approve(int index) {
        scene = approveButton.getScene();
        scene.setCursor(Cursor.WAIT);
        Connection connectDB = null;

        try {
            DatabaseConnection connection = new DatabaseConnection();
            connectDB = connection.getConnection();
            String sql = "UPDATE post SET approval = ?, likeNum = ?, dislikeNum = ?, approvalTime = ?, displayStatus = ? WHERE queryIndex = ?";
            PreparedStatement statement = connection.databaseLink.prepareStatement(sql);

            statement.setInt(1, 1);
            statement.setInt(2, 0);
            statement.setInt(3, 0);

            Calendar cal = Calendar.getInstance();
            java.sql.Timestamp timestamp = new java.sql.Timestamp(cal.getTimeInMillis());
            statement.setTimestamp(4, timestamp);

            statement.setInt(5, 0);
            statement.setInt(6, index);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            if (connectDB != null) {
                try {
                    connectDB.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        Parent loader = null;
        try {
            loader = FXMLLoader.load(getClass().getResource("admin-page.fxml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Stage window = (Stage)approveButton.getScene().getWindow();
        scene.setCursor(Cursor.DEFAULT);
        window.setScene(new Scene(loader));
    }

    void delete(int index) {
        scene = deleteButton.getScene();
        scene.setCursor(Cursor.WAIT);

        Connection connectDB = null;
        Statement statement = null;

        try {
            DatabaseConnection connection = new DatabaseConnection();
            connectDB = connection.getConnection();
            statement = connectDB.createStatement();
            String sql = "DELETE FROM post WHERE queryIndex='" + index + "'";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            if (statement != null) {
                try {
                    statement.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connectDB != null) {
                try {
                    connectDB.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        Parent loader = null;
        try {
            loader = FXMLLoader.load(getClass().getResource("admin-page.fxml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Stage window = (Stage)deleteButton.getScene().getWindow();
        scene.setCursor(Cursor.DEFAULT);
        window.setScene(new Scene(loader));
    }
}

