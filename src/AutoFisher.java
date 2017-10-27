import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import net.sourceforge.lept4j.util.LoadLibs;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class AutoFisher extends JFrame{

	private Robot robot;
	private Hashtable<String, int[]> start_coordinates;
	private int x;
	private int y;
	private int minutes_to_run = 0;
	private Dimension screen_size;
	private JTextArea text;
	private boolean shutdown = false;
	
	public static void main(String[] args){
		
		AutoFisher fisher = new AutoFisher();
		
	}
	
	public AutoFisher(){
		
		super("Terraria Autofisher");
		setSize(200,200);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		//TODO: use percentages to autodetect and calculate coordinates
		start_coordinates = new Hashtable<String, int[]>();
		start_coordinates.put("1920.0x1080.0", new int[]{958, 572});
		start_coordinates.put("1366.0x768.0", new int[]{679, 413});
		start_coordinates.put("1280.0x768.0", new int[]{636, 412});
		
		screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		x = start_coordinates.get(screen_size.getWidth() + "x" + screen_size.getHeight())[0];
		y = start_coordinates.get(screen_size.getWidth() + "x" + screen_size.getHeight())[1];
		
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		//let user click on the game window, pause the program 5 seconds
		robot.delay(5000);
		
		startFishing();
		
	}

	private void startFishing() {
		
		robot.mouseMove(x, y);
		
		int baitCount = getBaitCount();
		int counter = 1;
		
		while(baitCount > 0){
			
			robot.delay(500);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(150);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(2000);
			Color rodColor = robot.getPixelColor(x, y+3);
			Color backColor = robot.getPixelColor(x, y+3);
			while(similarColors(rodColor, backColor)){	
				robot.delay(35);
				backColor = robot.getPixelColor(x, y+3);
			}
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(150);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			
			/*
			 * recalculate bait every 5 times we pull back fishing rod
			 * as bait not used every time
			*/
			
			if(counter % 5 == 0){
				baitCount = getBaitCount();
			}
			
			counter++;
			
		}
		
		if(this.shutdown){
			//esc, go to menu, save and exit, then shutdown in 1 minute
			int delay = 600;
			robot.keyPress(KeyEvent.VK_ESCAPE);
			robot.delay(delay);
			robot.mouseMove(3720-1920, 1016);
			robot.delay(delay);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(delay);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(delay);
			robot.mouseMove(2700-1920, 715);
			robot.delay(delay);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(delay);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(delay);
			String shutdownCmd = "shutdown -s -t 60";
			try {
				Process child = Runtime.getRuntime().exec(shutdownCmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void screenshotBait(){
		BufferedImage img = null;
		BufferedImage alt = null;
		File f = null;
		try {
			img = robot.createScreenCapture(new Rectangle(1931,286,825,17));
			int width = img.getWidth();
			int height = img.getHeight();
			for(int i = 0; i < height; i++){
				for(int j = 0; j < width; j++){
					Color c = new Color(img.getRGB(j, i));
					int r = (int) (c.getRed() * 0.299);
					int g = (int) (c.getGreen() * 0.587);
					int b = (int) (c.getBlue() * 0.114);
					Color c2 = new Color(r + g + b, r + g + b, r + g + b);
					img.setRGB(j, i, c2.getRGB());
				}
			}
			File output = new File("grayscale.jpg");
			ImageIO.write(img, "jpg", output);
		} catch (Exception e){
			System.out.println(e);
		}
	}
	
	public void addContrast(){
		//double alpha = 2;
		//double beta = 50;
		try{
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			Mat source = Imgcodecs.imread("grayscale.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
			Mat destination = new Mat(source.rows(), source.cols(), source.type());
			Imgproc.equalizeHist(source, destination);
			Imgcodecs.imwrite("contrast.jpg", destination);
		} catch (Exception e){
			System.out.println(e);
		}
	}
		
	private int getBaitCount() {
		screenshotBait();
		addContrast();
		File f = new File("contrast.jpg");
		ITesseract instance = new Tesseract();
		instance.setDatapath(LoadLibs.extractNativeResources("tessdata").getParent());
		int baitCount = 0;
		try {
			baitCount = instance.doOCR(f);
		} catch (Exception e){
			System.out.println(e);
		}
		return baitCount;
	}

	private boolean similarColors(Color rodColor, Color backColor) {
		int r1 = rodColor.getRed(), r2 = backColor.getRed();
		int g1 = rodColor.getGreen(), g2 = backColor.getGreen();
		int b1 = rodColor.getBlue(), b2 = backColor.getBlue();
		double distance = Math.sqrt( (r2-r1)*(r2-r1)+(g2-g1)*(g2-g1)+(b2-b1)*(b2-b1));
		return distance < 25;
	}
	
}
