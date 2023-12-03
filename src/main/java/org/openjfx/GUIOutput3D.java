package org.openjfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Pair;


public class GUIOutput3D extends Application {

	static GUIOutput3D instance = null;
	
	public static class DDDObject {
		double x;
		double y;
		double z;
		double size;
		int type;

		public DDDObject(double x, double y, double z, double size, int type) {
			super();
			this.x = x;
			this.y = y;
			this.z = z;
			this.size = size;
			this.type = type;
		}

		@Override
		public String toString() {
			return "(" + x + "," + y + "," + z + "|" + size + ")";
		}
	}

	private List<List<DDDObject>> dddObjects;
	private List<String> dddTitles;
	private int currentText;

	boolean buffered;
	double scale;
	double offsetX;
	double offsetY;
	double offsetZ;
	double radiusScale;

	public GUIOutput3D() {
		this.dddObjects = new ArrayList<>();
		this.dddTitles = new ArrayList<>();
		this.currentText = -1;
		this.buffered = true;
		this.scale = 1.0;
		this.offsetX = 0.0;
		this.offsetY = 0.0;
		this.offsetZ = 0.0;
		this.radiusScale = 1.0;
		instance = this;
	}

	/**
	 * from: http://www.java3d.org/position.html
	 * 
	 * @param universe
	 * @return
	 */
	public Object createScene(List<DDDObject> dddOs) {

		for (DDDObject dddo : dddOs) {
			float size = (float) (radiusScale * scale * dddo.size);
			Object child;
			switch (dddo.type) {
			case 0:
				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apRed)";
				break;
			case 1:
				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apGreen)";
				break;
			case 2:
				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apBlue)";
				break;
			case 3:
				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apYellow)";
				break;
			case 10:
				child = "new Sphere(size, apRed)";
				break;
			case 11:
				child = "new Sphere(size, apGreen)";
				break;
			case 12:
				child = "new Sphere(size, apBlue)";
				break;
			case 13:
				child = "new Sphere(size, apYellow)";
				break;
			default:
				throw new RuntimeException("invalid type " + dddo.type);
			}
//    	      TransformGroup tg = new TransformGroup();
//    	      Transform3D transform = new Transform3D();
//    	      Vector3d vector = new Vector3d( scale*(dddo.x-offsetX), scale*(dddo.y-offsetY), scale*(dddo.z-offsetZ));
//    	      transform.setTranslation(vector);
//    	      tg.setTransform(transform);
//    	      tg.addChild(child);
//    	      group.addChild(tg);
		}
		return null;
	}

	public void create(String titleText) {
		
		// Create the custom dialog.
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Login Dialog");
		dialog.setHeaderText("Look, a Custom Login Dialog");
	
		// Set the icon (must be included in the project).
		dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));
	
		// Set the button types.
		ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
	
		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));
	
		TextField username = new TextField();
		username.setPromptText("Username");
		PasswordField password = new PasswordField();
		password.setPromptText("Password");
	
		grid.add(new Label("Username:"), 0, 0);
		grid.add(username, 1, 0);
		grid.add(new Label("Password:"), 0, 1);
		grid.add(password, 1, 1);
	
		// Enable/Disable login button depending on whether a username was entered.
		Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
		loginButton.setDisable(true);
	
		// Do some validation (using the Java 8 lambda syntax).
		username.textProperty().addListener((observable, oldValue, newValue) -> {
		    loginButton.setDisable(newValue.trim().isEmpty());
		});
	
		dialog.getDialogPane().setContent(grid);
	
		// Request focus on the username field by default.
		Platform.runLater(() -> username.requestFocus());
	
		// Convert the result to a username-password-pair when the login button is clicked.
		dialog.setResultConverter(dialogButton -> {
		    if (dialogButton == loginButtonType) {
		        return new Pair<>(username.getText(), password.getText());
		    }
		    return null;
		});
	
		Optional<Pair<String, String>> result = dialog.showAndWait();
	
		result.ifPresent(usernamePassword -> {
		    System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
		});
	}

	public static void createDialog(String titleText) {
		//Create Stage
		Stage newWindow = new Stage();
		newWindow.setTitle("New Scene");
		//Create view in Java
		Label title = new Label(titleText);
		TextField textField = new TextField("Enter your name here");
		Button button = new Button("OK");
		button.setOnAction(event -> {
		    //handle button press
		});
		VBox container = new VBox(title, textField, button);
		//Style container
		container.setSpacing(15);
		container.setPadding(new Insets(25));
		container.setAlignment(Pos.CENTER);
		//Set view in window
		newWindow.setScene(new Scene(container));
		//Launch
		newWindow.show();
	}

    	
	private synchronized void refreshCanvas() {
		if ((currentText < 0) || (currentText >= dddObjects.size())) {
			return;
		}
		List<DDDObject> dddo = dddObjects.get(currentText);
		updateScene(dddo);
////    		newCanvas.setDoubleBufferEnable(true);
////    		newCanvas.startRenderer();
//		canvasPanel.remove(canvas);
//		canvas = newCanvas;
//		canvasPanel.add(canvas);
//		canvasPanel.revalidate();
//		canvasPanel.repaint();
////		f.getContentPane().add(canvas);
////		f.invalidate();
////    		f.validate();
////    		f.repaint();
////    		System.out.println(dddo);
	}

	private void updateScene(List<GUIOutput3D.DDDObject> dddo) {
		Platform.runLater(()->updateSceneAsync(dddo));
	}

	private void updateSceneAsync(List<GUIOutput3D.DDDObject> dddo) {
		Box newBox = new Box(10, 2, 5);
		
		SmartGroup group2 = new SmartGroup();
		group2.getChildren().add(newBox);
		group2.translateXProperty().set(5);
		group2.translateYProperty().set(5);
		group2.translateZProperty().set(0);
		rootGroup.getChildren().add(group2);

	}

	private void previous() {
	}

	private void next() {
	}

	public void setScale(double scale) {
	}

	public void adjustScale() {
		if ((currentText < 0) || (currentText >= dddObjects.size())) {
			return;
		}
		adjustScale(dddObjects.get(currentText));
	}

	public void adjustScale(List<DDDObject> dddOs) {
		double minXValue = Double.POSITIVE_INFINITY;
		double minYValue = Double.POSITIVE_INFINITY;
		double minZValue = Double.POSITIVE_INFINITY;
		double maxXValue = Double.NEGATIVE_INFINITY;
		double maxYValue = Double.NEGATIVE_INFINITY;
		double maxZValue = Double.NEGATIVE_INFINITY;
		for (DDDObject dddo : dddOs) {
			minXValue = Math.min(minXValue, dddo.x);
			minYValue = Math.min(minYValue, dddo.y);
			minZValue = Math.min(minZValue, dddo.z);
			maxXValue = Math.max(maxXValue, dddo.x);
			maxYValue = Math.max(maxYValue, dddo.y);
			maxZValue = Math.max(maxZValue, dddo.z);
		}
		offsetX = (maxXValue + minXValue) / 2;
		offsetY = (maxYValue + minYValue) / 2;
		offsetZ = (maxZValue + minZValue) / 2;
		double maxDiff = Math.max(maxXValue - minXValue, Math.max(maxYValue - minYValue, maxZValue - minZValue));
		if (maxDiff == 0.0) {
			maxDiff = 1.0;
		}
		this.scale = 2.0 / maxDiff;
//    	SwingUtilities.invokeLater(() -> refreshCanvas());
	}

	public void setText(String title, List<DDDObject> dddO) {
		if (buffered) {
			dddObjects.add(new ArrayList<>(dddO));
			dddTitles.add(title);
			if (currentText != -1) {
				return;
			}
		}
		currentText = dddObjects.size();
		dddObjects.add(new ArrayList<>(dddO));
		dddTitles.add(title);
		refreshCanvas();
//    	lbTextID.setText(Integer.toString(currentText)+" "+title);
	}

	private void smaller() {
		radiusScale = 0.5 * radiusScale;
		refreshCanvas();
	}

	private void bigger() {
		radiusScale = 2 * radiusScale;
		refreshCanvas();
	}

	private synchronized void animation() {
	}

	private synchronized void asyncAnimation() {
	}

	private static Random random = new Random();

	public static double rand() {
		return rand(-1.0, 1.0);
	}

	public static double rand(double from, double to) {
		return from + random.nextDouble() * (to - from);
	}

	public static int irand(int from, int to) {
		return from + random.nextInt(to - from + 1);
	}

	
    private static final float WIDTH = 1400;
    private static final float HEIGHT = 1000;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final DoubleProperty angleX = new SimpleDoubleProperty(0);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);

    Stage primary;
    Group rootGroup;
    SmartGroup group;
    Scene scene; 
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		primary = primaryStage;
		Box box = new Box(10, 2, 5);

	    
        group = new SmartGroup();
        group.getChildren().add(box);

	    rootGroup = new Group();
	    rootGroup.getChildren().add(group);

        Camera camera = new PerspectiveCamera();
        scene = new Scene(rootGroup, WIDTH, HEIGHT);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);

        rootGroup.translateXProperty().set(WIDTH / 2);
        rootGroup.translateYProperty().set(HEIGHT / 2);
        rootGroup.translateZProperty().set(-1500);

        initMouseControl(rootGroup, scene, primaryStage);

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case W:
                    group.translateZProperty().set(rootGroup.getTranslateZ() + 100);
                    break;
                case S:
                    group.translateZProperty().set(rootGroup.getTranslateZ() - 100);
                    break;
                case Q:
                    group.rotateByX(10);
                    break;
                case E:
                    group.rotateByX(-10);
                    break;
                case NUMPAD6:
                    group.rotateByY(10);
                    break;
                case NUMPAD4:
                    group.rotateByY(-10);
                    break;
            }
        });

        primaryStage.setTitle("Genuine Coder");
        primaryStage.setScene(scene);
        primaryStage.show();
	}
	
    private void initMouseControl(Group group, Scene scene, Stage stage) {
        Rotate xRotate;
        Rotate yRotate;
        group.getTransforms().addAll(
                xRotate = new Rotate(0, Rotate.X_AXIS),
                yRotate = new Rotate(0, Rotate.Y_AXIS)
        );
        xRotate.angleProperty().bind(angleX);
        yRotate.angleProperty().bind(angleY);

        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX.get();
            anchorAngleY = angleY.get();
        });

        scene.setOnMouseDragged(event -> {
            angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
            angleY.set(anchorAngleY + anchorX - event.getSceneX());
        });

        stage.addEventHandler(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY();
            group.translateZProperty().set(group.getTranslateZ() + delta);
        });
    }


    class SmartGroup extends Group {
        Rotate r;
        Transform t = new Rotate();

        void rotateByX(int ang) {
            r = new Rotate(ang, Rotate.X_AXIS);
            t = t.createConcatenation(r);
            this.getTransforms().clear();
            this.getTransforms().addAll(t);
        }

        void rotateByY(int ang) {
            r = new Rotate(ang, Rotate.Y_AXIS);
            t = t.createConcatenation(r);
            this.getTransforms().clear();
            this.getTransforms().addAll(t);
        }
    }

    
	public static void main2(String[] args) throws Exception {
		System.out.println("A");
		new Thread(()->test()).start();
		System.out.println("B");
		launch(args);
		System.out.println("C");
	}
	
	public static GUIOutput3D test()  {
		System.out.println("TEST");
		try {
//			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			// UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		while (instance == null) {
			System.out.println("WAIT");
			sleep(50);
		}
		System.out.println("FOUND");
		GUIOutput3D output = instance;
		List<DDDObject> state = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			state.add(new DDDObject(rand(), rand(), rand(), rand(0.01, 0.1), irand(0, 2)));
		}
		Platform.runLater(() -> {
			output.updateScene(state);
		});
		return instance;
//		for (int t = 0; t < 20; t++) {
//			ArrayList<DDDObject> nextState = new ArrayList<>();
//			for (DDDObject dddo : state) {
//				nextState.add(new DDDObject(dddo.x + rand() * 0.05, dddo.y + rand() * 0.05, dddo.z + rand() * 0.05,
//						dddo.size, dddo.type));
//			}
//			output.setText("Test " + t, nextState);
//			state = nextState;
//		}
	}

	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	public static synchronized GUIOutput3D open(String string) {
		instance = null;
		try {
			Platform.runLater(()->{
				Stage prim = new Stage();
				GUIOutput3D go3d = new GUIOutput3D();
				try {
					go3d.start(prim);
				} catch (Exception e) {
					throw new RuntimeException(e.toString(), e);
				}
				instance = go3d; 
			});
		}
		catch (IllegalStateException tkEx) {
			Platform.startup(()->{
				Stage prim = new Stage();
				GUIOutput3D go3d = new GUIOutput3D();
				try {
					go3d.start(prim);
				} catch (Exception e) {
					throw new RuntimeException(e.toString(), e);
				}
				instance = go3d; 
			});
		}
		while (instance == null) {
			System.out.println("WAIT");
			sleep(50);
		}
		System.out.println("FOUND");		
		return instance;
	}

	public static synchronized GUIOutput3D startup(String string) {
		instance = null;
		Platform.startup(()->{
			Stage prim = new Stage();
			GUIOutput3D go3d = new GUIOutput3D();
			try {
				go3d.start(prim);
			} catch (Exception e) {
				throw new RuntimeException(e.toString(), e);
			}
			instance = go3d; 
		});
		while (instance == null) {
			System.out.println("WAIT");
			sleep(50);
		}
		System.out.println("FOUND");		
		return instance;
	}

	public static void main(String[] args) throws Exception {
		GUIOutput3D output = GUIOutput3D.open("title");
		List<DDDObject> state = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			state.add(new DDDObject(rand(), rand(), rand(), rand(0.01, 0.1), irand(0, 2)));
		}
		Platform.runLater(() -> {
			output.updateScene(state);
		});
//		for (int t = 0; t < 20; t++) {
//			ArrayList<DDDObject> nextState = new ArrayList<>();
//			for (DDDObject dddo : state) {
//				nextState.add(new DDDObject(dddo.x + rand() * 0.05, dddo.y + rand() * 0.05, dddo.z + rand() * 0.05,
//						dddo.size, dddo.type));
//			}
//			output.setText("Test " + t, nextState);
//			state = nextState;
//		}
		
	}
    
}

