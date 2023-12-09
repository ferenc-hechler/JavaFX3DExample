package org.openjfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
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

	public static class DDDLineObject extends DDDObject {
		double x2;
		double y2;
		double z2;
		public DDDLineObject(double x, double y, double z, double x2, double y2, double z2, double size, int type) {
			super(x, y, z, size, type);
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
		}
	}
	
	private String title;
	
	private List<List<DDDObject>> dddObjects;
	private List<String> dddTitles;
	private int currentStep;
	Map<Integer, Node> nodeIdMap;

	double scale;
	double offsetX;
	double offsetY;
	double offsetZ;
	double radiusScale;

	public GUIOutput3D(String title) {
		this.title = title;
		this.dddObjects = new ArrayList<>();
		this.dddTitles = new ArrayList<>();
		this.currentStep = -1;
		this.scale = 1.0;
		this.offsetX = 0.0;
		this.offsetY = 0.0;
		this.offsetZ = 0.0;
		this.radiusScale = 2.0;
		this.nodeIdMap = new HashMap<>();
		open();
	}

	

	private Node createSphere(float size, PhongMaterial matRed) {
		Node child;
		Sphere sphere = new Sphere(0.5*size);
		sphere.setMaterial(matRed);
		child = sphere;
		return child;
	}

	private Node createBox(float size, PhongMaterial matRed) {
		Node child;
		Box box = new Box(size, size, size);
		box.setMaterial(matRed);
		child = box;
		return child;
	}
	
	



    static public Cylinder createCylinder(Point3D lineFrom, Point3D lineTo) {
        // x axis vector is <1,0,0>
        // y axis vector is <0,1,0>
        // z axis vector is <0,0,1>
        // angle = arccos((P*Q)/(|P|*|Q|))
        // define a point representing the Y axis
        Point3D yAxis = new Point3D(0,1,0);
        // define a point based on the difference of our end point from the start point of our segment
        Point3D seg = lineTo.subtract(lineFrom);
        // determine the length of our line or the height of our cylinder object
        double height = seg.magnitude();
        // get the midpoint of our line segment
        Point3D midpoint = lineTo.midpoint(lineFrom);
        // set up a translate transform to move to our cylinder to the midpoint
        Translate moveToMidpoint = new Translate(midpoint.getX(), midpoint.getY(), midpoint.getZ());
        // get the axis about which we want to rotate our object
        Point3D axisOfRotation = seg.crossProduct(yAxis);
        // get the angle we want to rotate our cylinder
        double angle = Math.acos(seg.normalize().dotProduct(yAxis));
        // create our rotating transform for our cylinder object
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
        // create our cylinder object representing our line
        Cylinder line = new Cylinder(1, height);
        // add our two transfroms to our cylinder object
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        // return our cylinder for use      
        return line;
    } // end of the createCylinder method

    static public Box createLineBox(Point3D lineFrom, Point3D lineTo, float size, PhongMaterial mat) {
        // x axis vector is <1,0,0>
        // y axis vector is <0,1,0>
        // z axis vector is <0,0,1>
        // angle = arccos((P*Q)/(|P|*|Q|))
        // define a point representing the Y axis
        Point3D yAxis = new Point3D(0,1,0);
        // define a point based on the difference of our end point from the start point of our segment
        Point3D seg = lineTo.subtract(lineFrom);
        // determine the length of our line or the height of our cylinder object
        double height = seg.magnitude();
        // get the midpoint of our line segment
        Point3D midpoint = lineTo.midpoint(lineFrom);
        // set up a translate transform to move to our cylinder to the midpoint
        Translate moveToMidpoint = new Translate(midpoint.getX(), midpoint.getY(), midpoint.getZ());
        // get the axis about which we want to rotate our object
        Point3D axisOfRotation = seg.crossProduct(yAxis);
        // get the angle we want to rotate our cylinder
        double angle = Math.acos(seg.normalize().dotProduct(yAxis));
        // create our rotating transform for our cylinder object
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
        // create our cylinder object representing our line
        Box line = new Box(size, height, size);
        // add our two transfroms to our cylinder object
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        // set material
		line.setMaterial(mat);
        // return our line for use
        return line;
    } // end of the createCylinder method
	
	
	/**
	 * from: http://www.java3d.org/position.html
	 * 
	 * @param universe
	 * @return
	 */
	public SmartGroup createScene(List<DDDObject> dddOs) {

        Color white  = new Color(1.0, 1.0, 1.0, 1.0); 
        Color red    = new Color(1.0, 0.1, 0.1, 1.0); 
        Color green  = new Color(0.1, 1.0, 0.1, 1.0); 
        Color yellow = new Color(1.0, 1.0, 0.1, 1.0); 
        Color blue   = new Color(0.1, 0.1, 1.0, 1.0); 
        Color black  = new Color(0.0, 0.0, 0.0, 1.0); 
        PhongMaterial matRed = new PhongMaterial();
        matRed.setDiffuseColor(red);
        matRed.setSpecularColor(red);
        PhongMaterial matGreen = new PhongMaterial();
        matGreen.setDiffuseColor(green);
        matGreen.setSpecularColor(green);
        PhongMaterial matYellow = new PhongMaterial();
        matYellow.setDiffuseColor(yellow);
        matYellow.setSpecularColor(yellow);
        PhongMaterial matBlue = new PhongMaterial();
        matBlue.setDiffuseColor(blue);
        matBlue.setSpecularColor(blue);
		
		SmartGroup result = new SmartGroup();
		nodeIdMap.clear();
		int id = -1;
		for (DDDObject dddo : dddOs) {
			id++;
			float size = (float) (radiusScale * scale * dddo.size);
			Node child;
			switch (dddo.type) {
			case 0: {
				child = createBox(size, matRed);
				break;
			}
			case 1: {
				child = createBox(size, matGreen);
				break;
			}
			case 2: {
				child = createBox(size, matBlue);
				break;
			}
			case 3: {
				child = createBox(size, matYellow);
				break;
			}
			case 10: {
				child = createSphere(size, matRed);
				break;
			}
			case 11: {
				child = createSphere(size, matGreen);
				break;			
			}
			case 12: {
				child = createSphere(size, matBlue);
				break;			
			}
			case 13: {
				child = createSphere(size, matYellow);
				break;
			}
//			case 1:
//				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apGreen)";
//				break;
//			case 2:
//				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apBlue)";
//				break;
//			case 3:
//				child = "new com.sun.j3d.utils.geometry.Box(size, size, size, apYellow)";
//				break;
//			case 10:
//				child = "new Sphere(size, apRed)";
//				break;
//			case 11:
//				child = "new Sphere(size, apGreen)";
//				break;
//			case 12:
//				child = "new Sphere(size, apBlue)";
//				break;
//			case 13:
//				child = "new Sphere(size, apYellow)";
//				break;
			case 30: {
				DDDLineObject line = (DDDLineObject)dddo;
				Point3D from = new Point3D(scale*(line.x-offsetX), scale*(line.y-offsetY), scale*(line.z-offsetZ));
				Point3D to = new Point3D(scale*(line.x2-offsetX), scale*(line.y2-offsetY), scale*(line.z2-offsetZ));
				child = createLineBox(from, to, (float)(size), matYellow);
				result.getChildren().add(child);
				continue;
			}
			default:
				throw new RuntimeException("invalid type " + dddo.type);
			}
//			child.translateXProperty().set(scale*(dddo.x-offsetX));
//			child.translateYProperty().set(scale*(dddo.y-offsetY));
//			child.translateZProperty().set(scale*(dddo.z-offsetZ));
			child.setTranslateX(scale*(dddo.x-offsetX));
			child.setTranslateY(scale*(dddo.y-offsetY));
			child.setTranslateZ(scale*(dddo.z-offsetZ));
			result.getChildren().add(child);
			nodeIdMap.put(id, child);
//    	      TransformGroup tg = new TransformGroup();
//    	      Transform3D transform = new Transform3D();
//    	      Vector3d vector = new Vector3d( scale*(dddo.x-offsetX), scale*(dddo.y-offsetY), scale*(dddo.z-offsetZ));
//    	      transform.setTranslation(vector);
//    	      tg.setTransform(transform);
//    	      tg.addChild(child);
//    	      group.addChild(tg);
		}
		return result;
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

    private int nextPage = -1;

    private synchronized void switchPage(int page) {
    	if (nextPage == -1) {
        	nextPage = page;
        	Platform.runLater(() -> asyncSwitchPage());
        	return;
    	}
    	nextPage = page;
    }

    
    private synchronized void asyncSwitchPage() {
    	int targetPage = Math.min(dddObjects.size()-1, Math.max(0, nextPage));
    	if (currentStep != targetPage) {
    		currentStep = targetPage;
    		refreshCanvas();
        	lbTextID.setText(Integer.toString(currentStep)+" "+dddTitles.get(currentStep));
    	}
    	nextPage = -1;
	}
    
    	
	private synchronized void refreshCanvas() {
		if ((currentStep < 0) || (currentStep >= dddObjects.size())) {
			return;
		}
		List<DDDObject> dddo = dddObjects.get(currentStep);
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

	private void updateScene(List<DDDObject> dddo) {
		Platform.runLater(()->updateSceneAsync(dddo));
	}

	private void updateSceneAsync(List<DDDObject> dddo) {
		
		SmartGroup newScene = createScene(dddo);
		rootGroup.getChildren().remove(currentScene);
		rootGroup.getChildren().add(newScene);
		currentScene = newScene;
	}

	private void previous() {
		switchPage(currentStep-1);
	}

	private void next() {
		switchPage(currentStep+1);
	}

	public void setScale(double scale) {
    	this.scale = scale;
    	refreshCanvas();
	}

	public void adjustScale() {
		if ((currentStep < 0) || (currentStep >= dddObjects.size())) {
			return;
		}
		adjustScale(dddObjects.get(currentStep));
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
		scale = scale * 60;
		refreshCanvas();
	}

	public void scaleUp() {
		scale = 1.5*scale;
		refreshCanvas();
	}

	public void scaleDown() {
		scale = scale/1.5;
		refreshCanvas();
	}

	public void addStep(String title, List<DDDObject> dddO) {
		dddObjects.add(new ArrayList<>(dddO));
		dddTitles.add(title);
		if (currentStep != -1) {
			return;
		}
		currentStep = dddObjects.size()-1;
		refreshCanvas();
		
	}

	private void smaller() {
		radiusScale = 0.5 * radiusScale;
		refreshCanvas();
	}

	private void bigger() {
		radiusScale = 2 * radiusScale;
		refreshCanvas();
	}

	class AnimationTask extends TimerTask {
		@Override public void run() { asyncAnimation(); }
	}
	
	private synchronized void animation() {
    	if (timer!= null) {
    		timer.cancel();
    		timer= null;
    		return;
    	}
    	timer = new Timer();
    	timer.scheduleAtFixedRate(new AnimationTask(), 250, 250);
	}

	private synchronized void asyncAnimation() {
		if (timer == null) {
			return;
		}
		int page = currentStep + 1;
		if (page >= dddObjects.size()) {
			timer.cancel();
			timer = null;
			return;
		}
		switchPage(page);
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
    SmartGroup rootGroup;
    SmartGroup currentScene;
    SubScene rootScene;
    Label lbTextID;
    Slider slider;
    Timer timer;
    TimerTask timerTask;
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		primary = primaryStage;

		primary.setTitle("3D Output");
		Button btPrevious = new Button("<");
		btPrevious.setOnAction(event -> {
			previous();
		});
		Button btNext = new Button(">");
		btNext.setOnAction(event -> {
			next();
		});
		
		Button btSmaller = new Button("v");
        btSmaller.setOnAction(ev -> {
        	smaller();
        });
        
        Button btBigger = new Button("^");
        btBigger.setOnAction(ev -> {
        	bigger();
        });
		
        Button btAdjustScale = new Button("Adjust Scale");
        btAdjustScale.setOnAction(ev -> {
        	adjustScale();
        });
        
        Button btScaleUp = new Button("+");
        btScaleUp.setOnAction(ev -> {
        	scaleUp();
        });
        
        Button btScaleDown = new Button("-");
        btScaleDown.setOnAction(ev -> {
        	scaleDown();
        });
        
        Button btAnimation = new Button("Animation");
        btAnimation.setOnAction(ev -> {
        	animation();
        });

        Button btBlue = new Button("blue");
        btBlue.setOnAction(ev -> {
        	buttonBlue();
        });

        Button btRed = new Button("red");
        btRed.setOnAction(ev -> {
        	buttonRed();
        });

		lbTextID = new Label("0");
		HBox buttons = new HBox(btPrevious, btNext, btSmaller, btBigger, btAdjustScale, btScaleUp, btScaleDown, btAnimation, btBlue, btRed, lbTextID);
		buttons.setSpacing(5);
//		buttons.setPadding(new Insets(5));
		
        slider = new Slider(0, 10000, 0);
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                    Number old_val, Number new_val) {
            	double percent = new_val.doubleValue() * 0.0001;
            	int page = (int) (percent * (dddObjects.size()-1));
            	switchPage(page);
                }
            });
        slider.setPrefWidth(WIDTH-50);
		
		VBox container2D = new VBox(buttons, slider);
		container2D.setSpacing(15);
		container2D.setPadding(new Insets(25));
		container2D.setAlignment(Pos.CENTER);
		
		Group group2D = new Group(container2D);
		SubScene subScene2D = new SubScene(group2D, WIDTH, 80);

		SmartGroup group3D = new SmartGroup();
		SubScene subScene3D = new SubScene(group3D, WIDTH, HEIGHT, true, SceneAntialiasing.DISABLED);
		
		VBox vbox = new VBox(subScene2D, subScene3D);
		Group groupALL = new Group(vbox);
		Scene scene = new Scene(groupALL);
		
		Box box = new Box(10, 2, 5);
		currentScene = new SmartGroup();
		currentScene.getChildren().add(box);
	    rootGroup = group3D;
	    rootGroup.getChildren().add(currentScene);
        Camera camera = new PerspectiveCamera();
        rootScene = subScene3D;
        rootScene.setFill(Color.SILVER);
        rootScene.setCamera(camera);

        rootGroup.translateXProperty().set(WIDTH / 2);
        rootGroup.translateYProperty().set(HEIGHT / 2);
        rootGroup.translateZProperty().set(-1500);

        initMouseControl(rootGroup, scene, primaryStage);

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case W:
                	rootGroup.translateZProperty().set(rootGroup.getTranslateZ() + 100);
                    break;
                case S:
                	rootGroup.translateZProperty().set(rootGroup.getTranslateZ() - 100);
                    break;
                case Q:
                	rootGroup.rotateByX(10);
                    break;
                case E:
                	rootGroup.rotateByX(-10);
                    break;
                case NUMPAD6:
                	rootGroup.rotateByY(10);
                    break;
                case NUMPAD4:
                	rootGroup.rotateByY(-10);
                    break;
            }
        });
		primary.setScene(scene);
		primaryStage.show();
	}
	
    private void buttonBlue() {
        setBoxColors(new Color(0.1, 0.1, 1.0, 1.0));
	}

    private void buttonRed() {
        setBoxColors(new Color(1.0, 0.1, 0.1, 1.0));
	}

    private void setBoxColors(Color col) {
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(col);
        mat.setSpecularColor(col);
    	for (Node node:nodeIdMap.values()) {
    		if (node instanceof Box) {
    			Box box = (Box)node;
    			box.setMaterial(mat);
    		}
    	}
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

    
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private synchronized void open() {
		instance = null;
		try {
			Platform.runLater(()->{
				Stage prim = new Stage();
				try {
					start(prim);
				} catch (Exception e) {
					throw new RuntimeException(e.toString(), e);
				}
				instance = this; 
			});
		}
		catch (IllegalStateException tkEx) {
			Platform.startup(()->{
				Stage prim = new Stage();
				try {
					start(prim);
				} catch (Exception e) {
					throw new RuntimeException(e.toString(), e);
				}
				instance = this; 
			});
		}
		while (instance == null) {
			System.out.println("WAIT");
			sleep(500);
		}
		System.out.println("FOUND");		
	}

	public static void main(String[] args) throws Exception {
		GUIOutput3D output = new GUIOutput3D("GUIOutput3D Test");
		output.smaller();
		
		List<DDDObject> state = new ArrayList<>();

		// F
		state.add(new DDDObject(-1.0, -1.0, -1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  0.0, -1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  1.0, -1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  2.0, -1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  3.0, -1.0, 1.0, 0));

		state.add(new DDDObject( 0.0,  1.0, -1.0, 1.0, 0));
		
		state.add(new DDDObject( 0.0, -1.0, -1.0, 1.0, 0));
		state.add(new DDDObject( 1.0, -1.0, -1.0, 1.0, 0));

		// E
		state.add(new DDDObject(-1.0, -1.0,  1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  0.0,  1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  1.0,  1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  2.0,  1.0, 1.0, 0));
		state.add(new DDDObject(-1.0,  3.0,  1.0, 1.0, 0));

		state.add(new DDDObject( 0.0, -1.0,  1.0, 1.0, 0));
		state.add(new DDDObject( 1.0, -1.0,  1.0, 1.0, 0));
		
		state.add(new DDDObject( 0.0,  1.0,  1.0, 1.0, 0));
		
		state.add(new DDDObject( 0.0,  3.0,  1.0, 1.0, 0));
		state.add(new DDDObject( 1.0,  3.0,  1.0, 1.0, 0));

		
		output.adjustScale(state);
		output.addStep("Initial Dummy", state);		
		
		
//		state = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			state.add(new DDDObject(rand(), rand(), rand(), rand(0.01, 0.1), irand(0, 2)));
//		}
//		output.adjustScale(state);
//		output.addStep("Erste Szene", state);

		for (int t = 0; t < 20; t++) {
			ArrayList<DDDObject> nextState = new ArrayList<>();
			for (DDDObject dddo : state) {
				nextState.add(new DDDObject(dddo.x + rand() * 0.05, dddo.y + rand() * 0.05, dddo.z + rand() * 0.05,
						dddo.size, dddo.type));
			}
			output.addStep("Testanimation", state);
			state = nextState;
		}
		
	}
    
}

